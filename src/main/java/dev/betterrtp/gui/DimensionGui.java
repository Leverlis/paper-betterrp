package dev.betterrtp.gui;

import dev.betterrtp.util.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DimensionGui {

    public static final String GUI_TITLE = "ʀᴀɴᴅᴏᴍ ᴛᴇʟᴇᴘᴏʀᴛ";

    // Einmalig geparster GUI-Titel – spart wiederholtes MiniMessage-Parsing bei jedem Öffnen
    private static final Component GUI_TITLE_COMPONENT = Messages.parse(GUI_TITLE);

    // Slot-Gruppen pro Dimension (0-basiert)
    public static final int[] SLOTS_OVERWORLD = {10};
    public static final int[] SLOTS_NETHER    = {13};
    public static final int[] SLOTS_END       = {16};

    /** Jeden Slot-Index O(1) auf seine Dimension mappen für den Click-Listener. */
    public static final Map<Integer, World.Environment> SLOT_ENV_MAP;
    static {
        Map<Integer, World.Environment> map = new HashMap<>();
        for (int s : SLOTS_OVERWORLD) map.put(s, World.Environment.NORMAL);
        for (int s : SLOTS_NETHER)    map.put(s, World.Environment.NETHER);
        for (int s : SLOTS_END)       map.put(s, World.Environment.THE_END);
        SLOT_ENV_MAP = Collections.unmodifiableMap(map);
    }

    // Repräsentative Blöcke pro Dimension (Slots 11 / 14 / 17)
    private static final ItemStack ITEM_GRASS = buildItem(
            Material.GRASS_BLOCK,
            "<gradient:#7dff8a:#2e8b57>ᴏᴠᴇʀᴡᴏʀʟᴅ</gradient>",
            List.of());
    private static final ItemStack ITEM_NETHERRACK = buildItem(
            Material.NETHERRACK,
            "<gradient:#ff4d4d:#7a0000>ɴᴇᴛʜᴇʀ</gradient>",
            List.of());
    private static final ItemStack ITEM_END_STONE = buildItem(
            Material.END_STONE,
            "<gradient:#9d50bb:#ff4fd8>ᴛʜᴇ ᴇɴᴅ</gradient>",
            List.of());

    private DimensionGui() {}

    public static Inventory build() {
        RtpHolder holder = new RtpHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, GUI_TITLE_COMPONENT);
        holder.setInventory(inv);

        // Nur die repr\u00e4sentativen Bl\u00f6cke in die Mittel-Slots setzen
        inv.setItem(10, ITEM_GRASS.clone());
        inv.setItem(13, ITEM_NETHERRACK.clone());
        inv.setItem(16, ITEM_END_STONE.clone());

        return inv;
    }

    private static ItemStack buildItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Messages.parse(name));
        meta.lore(lore.stream().map(Messages::parse).toList());
        item.setItemMeta(meta);
        return item;
    }

    public static final class RtpHolder implements InventoryHolder {

        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }
}
