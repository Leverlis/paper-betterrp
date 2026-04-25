package dev.betterrtp.teleport;

import dev.betterrtp.BetterRTP;
import dev.betterrtp.config.RtpConfig;
import dev.betterrtp.util.Messages;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Teleporter {

    private final BetterRTP plugin;

    // Alle Spieler mit aktivem Teleport – verhindert Doppel-Teleports
    private final Set<UUID> pendingTeleports = ConcurrentHashMap.newKeySet();
    // Nur Spieler mit laufendem Delay-Task – zum Abbrechen bei Disconnect
    private final Map<UUID, ScheduledTask> delayTasks = new ConcurrentHashMap<>();
    // Laufende Actionbar-Tasks – werden bei Disconnect und nach Plugin-Reload abgebrochen
    private final Map<UUID, ScheduledTask> actionbarTasks = new ConcurrentHashMap<>();

    public Teleporter(BetterRTP plugin) {
        this.plugin = plugin;
    }

    public void cancelPending(UUID uuid) {
        pendingTeleports.remove(uuid);
        ScheduledTask delayTask = delayTasks.remove(uuid);
        if (delayTask != null) delayTask.cancel();
        ScheduledTask actionbarTask = actionbarTasks.remove(uuid);
        if (actionbarTask != null) actionbarTask.cancel();
    }

    public void teleport(Player player, World.Environment targetEnv) {
        if (!player.isOnline()) return;
        if (pendingTeleports.contains(player.getUniqueId())) return;

        RtpConfig cfg = plugin.getPluginConfig();

        World targetWorld = Bukkit.getWorlds().stream()
                .filter(w -> w.getEnvironment() == targetEnv)
                .findFirst()
                .orElse(null);

        if (targetWorld == null) {
            Messages.send(player, cfg.msg("dimension-unavailable"));
            return;
        }

        int delayTicks = cfg.getTeleportDelay();

        if (delayTicks <= 0) {
            pendingTeleports.add(player.getUniqueId());
            executeTransfer(player, targetWorld, targetEnv, cfg, null);
        } else {
            pendingTeleports.add(player.getUniqueId());
            ScheduledTask searchingTask = startSearchingBar(player, cfg);
            ScheduledTask delayTask = player.getScheduler().runDelayed(plugin,
                    st -> executeTransfer(player, targetWorld, targetEnv, cfg, searchingTask),
                    () -> {
                        if (searchingTask != null) searchingTask.cancel();
                        pendingTeleports.remove(player.getUniqueId());
                        delayTasks.remove(player.getUniqueId());
                    },
                    delayTicks);
            if (delayTask != null) {
                delayTasks.put(player.getUniqueId(), delayTask);
            } else {
                if (searchingTask != null) searchingTask.cancel();
                pendingTeleports.remove(player.getUniqueId());
            }
        }
    }

    private void executeTransfer(Player player, World targetWorld, World.Environment targetEnv,
                                  RtpConfig cfg, ScheduledTask searchingTask) {
        if (!player.isOnline()) {
            if (searchingTask != null) searchingTask.cancel();
            pendingTeleports.remove(player.getUniqueId());
            return;
        }

        spawnParticle(player.getLocation(), cfg);

        Location cached = plugin.getLocationCache().poll(targetEnv);
        if (cached != null) {
            doTeleport(player, cached, targetEnv, cfg, searchingTask);
            return;
        }

        List<Location> playerSnapshot = targetWorld.getPlayers().stream()
                .map(p -> p.getLocation().clone())
                .toList();

        ScheduledTask activeTask = (searchingTask != null) ? searchingTask : startSearchingBar(player, cfg);

        plugin.getLocationFinder().findSafe(targetWorld, playerSnapshot)
                .thenAccept(loc -> {
                    player.getScheduler().run(plugin,
                            st -> doTeleport(player, loc, targetEnv, cfg, activeTask),
                            () -> {
                                activeTask.cancel();
                                pendingTeleports.remove(player.getUniqueId());
                            });
                })
                .exceptionally(ex -> {
                    activeTask.cancel();
                    pendingTeleports.remove(player.getUniqueId());
                    player.getScheduler().run(plugin,
                            st -> Messages.send(player, cfg.msg("not-found")),
                            null);
                    return null;
                });
    }

    private void doTeleport(Player player, Location location, World.Environment env,
                             RtpConfig cfg, ScheduledTask searchingTask) {
        if (!player.isOnline()) {
            if (searchingTask != null) searchingTask.cancel();
            pendingTeleports.remove(player.getUniqueId());
            return;
        }

        player.teleportAsync(location).thenAccept(success -> {
            // teleportAsync-Callback läuft auf unbekanntem Thread -> zurück auf Entity-Region-Thread
            player.getScheduler().run(plugin, st -> {
                if (searchingTask != null) searchingTask.cancel();
                pendingTeleports.remove(player.getUniqueId());

                if (!player.isOnline()) return;

                if (!success) {
                    Messages.send(player, cfg.msg("teleport-failed"));
                    return;
                }

                plugin.getCooldownManager().setCooldown(player.getUniqueId());

                spawnParticle(player.getLocation(), cfg);
                playSound(player, cfg);

                String dimName = switch (env) {
                    case NORMAL  -> cfg.msg("dimension-overworld");
                    case NETHER  -> cfg.msg("dimension-nether");
                    case THE_END -> cfg.msg("dimension-end");
                    default      -> throw new IllegalStateException("Unbekannte Dimension: " + env);
                };

                int actionbarDuration = cfg.getActionbarDuration();
                if (actionbarDuration > 0) {
                    showActionbar(player, dimName, actionbarDuration);
                }
            }, () -> {
                // retired: Spieler ausgeloggt nach dem Teleport
                if (searchingTask != null) searchingTask.cancel();
                pendingTeleports.remove(player.getUniqueId());
            });
        }).exceptionally(ex -> {
            if (searchingTask != null) searchingTask.cancel();
            pendingTeleports.remove(player.getUniqueId());
            player.getScheduler().run(plugin,
                    st -> { if (player.isOnline()) Messages.send(player, cfg.msg("teleport-failed")); },
                    null);
            return null;
        });
    }


    private void showActionbar(Player player, String message, int durationSeconds) {
        Component component = Messages.parse(message);
        player.sendActionBar(component);

        UUID uuid = player.getUniqueId();
        int[] ticks = {0};
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            ticks[0]++;
            if (ticks[0] >= durationSeconds) {
                scheduledTask.cancel();
                actionbarTasks.remove(uuid);
                player.sendActionBar(Component.empty());
                return;
            }
            player.sendActionBar(component);
        }, () -> actionbarTasks.remove(uuid), 20L, 20L);
        if (task != null) actionbarTasks.put(uuid, task);
    }

    private ScheduledTask startSearchingBar(Player player, RtpConfig cfg) {
        Component component = Messages.parse(cfg.msg("searching"));
        player.sendActionBar(component);
        return player.getScheduler().runAtFixedRate(plugin,
                st -> { if (player.isOnline()) player.sendActionBar(component); },
                null, 20L, 20L);
    }

    private void spawnParticle(Location loc, RtpConfig cfg) {
        Particle p = cfg.getParticle();
        if (p == null) return;
        loc.getWorld().spawnParticle(p, loc,
                cfg.getParticleCount(),
                cfg.getParticleSpreadX(),
                cfg.getParticleSpreadY(),
                cfg.getParticleSpreadZ(),
                cfg.getParticleSpeed());
    }

    private void playSound(Player player, RtpConfig cfg) {
        Sound s = cfg.getSound();
        if (s == null) return;
        Messages.sound(player, s, cfg.getSoundVolume(), cfg.getSoundPitch());
    }
}
