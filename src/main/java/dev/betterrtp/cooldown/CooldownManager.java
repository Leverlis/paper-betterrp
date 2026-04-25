package dev.betterrtp.cooldown;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final Map<UUID, Instant> cooldowns = new ConcurrentHashMap<>();

    public long getRemaining(UUID uuid, int cooldownSeconds) {
        Instant last = cooldowns.get(uuid);
        if (last == null) return 0;
        long elapsed   = Instant.now().getEpochSecond() - last.getEpochSecond();
        long remaining = cooldownSeconds - elapsed;
        if (remaining <= 0) {
            cooldowns.remove(uuid);
            return 0;
        }
        return remaining;
    }

    public void setCooldown(UUID uuid) {
        cooldowns.put(uuid, Instant.now());
    }


    public void remove(UUID uuid) {
        cooldowns.remove(uuid);
    }
}
