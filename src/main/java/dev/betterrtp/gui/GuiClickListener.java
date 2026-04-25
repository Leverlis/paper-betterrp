package dev.betterrtp.gui;

import dev.betterrtp.BetterRTP;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;

public class GuiClickListener implements Listener {

    private final BetterRTP plugin;

    public GuiClickListener(BetterRTP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DimensionGui.RtpHolder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();

        if (event.getClickedInventory() == null
                || event.getClickedInventory().getType() == InventoryType.PLAYER) return;

        World.Environment targetEnv = DimensionGui.SLOT_ENV_MAP.get(slot);
        if (targetEnv == null) return;

        player.closeInventory();
        plugin.getTeleporter().teleport(player, targetEnv);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getTeleporter().cancelPending(event.getPlayer().getUniqueId());
    }
}
