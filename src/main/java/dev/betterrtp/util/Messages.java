package dev.betterrtp.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class Messages {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Messages() {}

    public static Component parse(String message) {
        return MM.deserialize(message);
    }

    public static void send(Player player, String message) {
        player.sendMessage(parse(message));
    }

    public static void actionbar(Player player, String message) {
        player.sendActionBar(parse(message));
    }

    public static void sound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
