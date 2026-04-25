package dev.betterrtp.command;

import dev.betterrtp.BetterRTP;
import dev.betterrtp.gui.DimensionGui;
import dev.betterrtp.util.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RtpCommand implements CommandExecutor {

    private final BetterRTP plugin;

    public RtpCommand(BetterRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(
                    "Dieser Befehl kann nur von Spielern verwendet werden."));
            return true;
        }

        // Sub-Befehl: /rtp reload
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("rtp.admin")) {
                Messages.send(player, plugin.getPluginConfig().msg("no-permission-admin"));
                return true;
            }
            plugin.reload();
            Messages.send(player, plugin.getPluginConfig().msg("reload-success"));
            return true;
        }

        // Berechtigungsprüfung
        if (!player.hasPermission("rtp.use")) {
            Messages.send(player, plugin.getPluginConfig().msg("no-permission"));
            return true;
        }

        // Cooldown prüfen
        int cooldownSeconds = plugin.getPluginConfig().getCooldown();
        if (cooldownSeconds > 0 && !player.hasPermission("rtp.bypass.cooldown")) {
            long remaining = plugin.getCooldownManager()
                    .getRemaining(player.getUniqueId(), cooldownSeconds);
            if (remaining > 0) {
                String msg = plugin.getPluginConfig().msg("cooldown")
                        .replace("{remaining}", String.valueOf(remaining));
                Messages.send(player, msg);
                return true;
            }
        }

        player.openInventory(DimensionGui.build());
        return true;
    }
}
