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

    // Titel: eigenes Hintergrundbild über Resourcepack-Font (better_rtp:rtp_gui).
    // \uF808 verschiebt den Cursor -8px (Inventar-Text startet bei x=8 → x=0),
    // \uE100 rendert rtp_gui.png über das gesamte GUI.
    public static final String GUI_TITLE = "<white><font:better_rtp:rtp_gui>\uF808\uE100</font:better_rtp:rtp_gui></white>";

    // Einmalig geparster GUI-Titel – spart wiederholtes MiniMessage-Parsing bei jedem Öffnen
    private static final Component GUI_TITLE_COMPONENT = Messages.parse(GUI_TITLE);

    // Slot-Gruppen pro Dimension (0-basiert), je 3×3 Block
    public static final int[] SLOTS_OVERWORLD = {0,  1,  2,  9, 10, 11, 18, 19, 20};
    public static final int[] SLOTS_NETHER    = {3,  4,  5, 12, 13, 14, 21, 22, 23};
    public static final int[] SLOTS_END       = {6,  7,  8, 15, 16, 17, 24, 25, 26};

    /** Jeden Slot-Index O(1) auf seine Dimension mappen für den Click-Listener. */
    public static final Map<Integer, World.Environment> SLOT_ENV_MAP;
    static {
        Map<Integer, World.Environment> map = new HashMap<>();
        for (int s : SLOTS_OVERWORLD) map.put(s, World.Environment.NORMAL);
        for (int s : SLOTS_NETHER)    map.put(s, World.Environment.NETHER);
        for (int s : SLOTS_END)       map.put(s, World.Environment.THE_END);
        SLOT_ENV_MAP = Collections.unmodifiableMap(map);
    }

    // Items einmalig beim Klassenload erstellen – STRUCTURE_VOID wird vollständig vom Resourcepack überschrieben
    private static final ItemStack ITEM_OVERWORLD = buildItem(
            Material.STRUCTURE_VOID,
            "<white>» ᴛᴇʟᴇᴘᴏʀᴛɪᴇʀᴇ ᴅɪᴄʜ ɪɴ ᴅɪᴇ ᴏᴠᴇʀᴡᴏʀʟᴅ</white>",
            List.of());
    private static final ItemStack ITEM_NETHER = buildItem(
            Material.STRUCTURE_VOID,
            "<white>» ᴛᴇʟᴇᴘᴏʀᴛɪᴇʀᴇ ᴅɪᴄʜ ɪɴ ᴅᴇɴ ɴᴇᴛʜᴇʀ</white>",
            List.of());
    private static final ItemStack ITEM_END = buildItem(
            Material.STRUCTURE_VOID,
            "<white>» ᴛᴇʟᴇᴘᴏʀᴛɪᴇʀᴇ ᴅɪᴄʜ ɪɴꜱ ᴛʜᴇ ᴇɴᴅ</white>",
            List.of());

    private DimensionGui() {}

    public static Inventory build() {
        RtpHolder holder = new RtpHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, GUI_TITLE_COMPONENT);
        holder.setInventory(inv);

        for (int s : SLOTS_OVERWORLD) inv.setItem(s, ITEM_OVERWORLD.clone());
        for (int s : SLOTS_NETHER)    inv.setItem(s, ITEM_NETHER.clone());
        for (int s : SLOTS_END)       inv.setItem(s, ITEM_END.clone());

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
