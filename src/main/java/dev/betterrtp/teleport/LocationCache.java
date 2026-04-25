package dev.betterrtp.teleport;

import dev.betterrtp.BetterRTP;
import dev.betterrtp.config.RtpConfig;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class LocationCache {

    private final BetterRTP plugin;
    private RtpConfig config;
    private LocationFinder finder;

    private final Map<World.Environment, Queue<Location>> pools =
            new EnumMap<>(World.Environment.class);

    private final Set<World.Environment> filling = Collections.synchronizedSet(EnumSet.noneOf(World.Environment.class));

    private int generation = 0;

    private ScheduledTask refillTask;

    private static final World.Environment[] DIMENSIONS = {
            World.Environment.NORMAL,
            World.Environment.NETHER,
            World.Environment.THE_END
    };

    public LocationCache(BetterRTP plugin, RtpConfig config, LocationFinder finder) {
        this.plugin = plugin;
        this.config  = config;
        this.finder  = finder;

        for (World.Environment env : DIMENSIONS) {
            pools.put(env, new ConcurrentLinkedQueue<>());
        }
    }

    public void start() {
        // 60 Ticks Wartezeit damit der Server hochfahren kann bevor wir Chunks anfassen
        // GlobalRegionScheduler: kein Location-Kontext nötig, da refillPool selbst async delegiert
        refillTask = plugin.getServer().getGlobalRegionScheduler()
                .runAtFixedRate(plugin, task -> checkAndRefill(), 60L, 40L);
    }

    public void stop() {
        if (refillTask != null) {
            refillTask.cancel();
            refillTask = null;
        }
        pools.values().forEach(Queue::clear);
    }

    public void reload(RtpConfig newConfig, LocationFinder newFinder) {
        this.config = newConfig;
        this.finder = newFinder;
        generation++;
        pools.values().forEach(Queue::clear);
        filling.clear();
    }

    public Location poll(World.Environment environment) {
        Location loc = pools.get(environment).poll();
        if (pools.get(environment).size() < config.getPoolSize()) {
            plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> checkAndRefill());
        }
        return loc;
    }

    private void checkAndRefill() {
        for (World.Environment env : DIMENSIONS) {
            Queue<Location> pool = pools.get(env);
            if (pool.size() < config.getPoolSize() && !filling.contains(env)) {
                World world = findWorld(env);
                if (world != null) {
                    filling.add(env);
                    refillPool(env, world);
                }
            }
        }
    }

    private void refillPool(World.Environment env, World world) {
        int needed = config.getPoolSize() - pools.get(env).size();
        List<Location> playerSnapshot = world.getPlayers().stream()
                .map(p -> p.getLocation().clone())
                .toList();
        fillNext(env, world, needed, generation, playerSnapshot);
    }

    private void fillNext(World.Environment env, World world, int remaining, int gen,
                           List<Location> playerSnapshot) {
        if (remaining <= 0) {
            filling.remove(env);
            return;
        }

        finder.findSafe(world, playerSnapshot).thenAccept(loc -> {
            if (gen != generation) {
                filling.remove(env);
                return;
            }
            pools.get(env).add(loc);
            fillNext(env, world, remaining - 1, gen, playerSnapshot);
        }).exceptionally(ex -> {
            Logger log = plugin.getLogger();
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            log.warning("Pool-Vorbefüllung für " + env.name()
                    + " fehlgeschlagen: " + cause.getMessage());
            filling.remove(env);
            return null;
        });
    }

    private World findWorld(World.Environment env) {
        return Bukkit.getWorlds().stream()
                .filter(w -> w.getEnvironment() == env)
                .findFirst()
                .orElse(null);
    }
}
