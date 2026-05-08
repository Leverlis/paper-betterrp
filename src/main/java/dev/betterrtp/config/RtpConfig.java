package dev.betterrtp.config;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RtpConfig {

    private final int cooldown;
    private final int maxAttempts;
    private final int poolSize;
    private final int teleportDelay;

    private final Sound sound;
    private final float soundVolume;
    private final float soundPitch;
    private final Sound startSound;
    private final float startSoundVolume;
    private final float startSoundPitch;
    private final Sound cancelSound;
    private final float cancelSoundVolume;
    private final float cancelSoundPitch;
    private final Particle particle;
    private final int particleCount;
    private final double particleSpreadX;
    private final double particleSpreadY;
    private final double particleSpreadZ;
    private final double particleSpeed;

    /** Bis zu drei Partikel; werden im Wechsel auf die Helix-Stränge verteilt. */
    private final List<Particle> helixParticles;

    private final DimensionConfig overworld;
    private final DimensionConfig nether;
    private final DimensionConfig end;

    private final Set<Biome> biomeBlacklist;
    private final Set<Material> blockBlacklist;
    private final Map<String, String> messages;

    @SuppressWarnings("deprecation")
    public RtpConfig(FileConfiguration config) {
        this.cooldown    = config.getInt("cooldown", 300);
        this.maxAttempts = Math.max(1, config.getInt("max-attempts", 50));
        this.poolSize    = Math.max(1, config.getInt("pool-size", 5));
        this.teleportDelay     = Math.max(0, config.getInt("teleport-delay", 0)) * 20;

        String soundName = config.getString("sound", "ENTITY_ENDERMAN_TELEPORT");
        this.sound       = parseSound(soundName);
        this.soundVolume = (float) config.getDouble("sound-volume", 1.0);
        this.soundPitch  = (float) config.getDouble("sound-pitch", 1.0);

        String startSoundName = config.getString("start-sound", "BLOCK_NOTE_BLOCK_PLING");
        this.startSound       = parseSound(startSoundName);
        this.startSoundVolume = (float) config.getDouble("start-sound-volume", 0.6);
        this.startSoundPitch  = (float) config.getDouble("start-sound-pitch", 1.2);

        String cancelSoundName = config.getString("cancel-sound", "BLOCK_NOTE_BLOCK_BASS");
        this.cancelSound       = parseSound(cancelSoundName);
        this.cancelSoundVolume = (float) config.getDouble("cancel-sound-volume", 0.8);
        this.cancelSoundPitch  = (float) config.getDouble("cancel-sound-pitch", 0.5);

        this.particle        = parseParticle(config.getString("particle", "POOF"));
        this.particleCount   = config.getInt("particle-count", 20);
        this.particleSpreadX = config.getDouble("particle-spread-x", 0.3);
        this.particleSpreadY = config.getDouble("particle-spread-y", 0.5);
        this.particleSpreadZ = config.getDouble("particle-spread-z", 0.3);
        this.particleSpeed   = config.getDouble("particle-speed", 0.05);

        this.helixParticles = parseHelixParticles(config);

        this.overworld = new DimensionConfig(getSection(config, "overworld"));
        this.nether    = new DimensionConfig(getSection(config, "nether"));
        this.end       = new DimensionConfig(getSection(config, "end"));

        Set<Biome> biomes = new HashSet<>();
        for (String name : config.getStringList("biome-blacklist")) {
            Biome biome = org.bukkit.Bukkit.getRegistry(Biome.class).get(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
            if (biome != null) biomes.add(biome);
        }
        this.biomeBlacklist = Collections.unmodifiableSet(biomes);

        Set<Material> blocks = new HashSet<>();
        for (String name : config.getStringList("block-blacklist")) {
            Material mat = Material.matchMaterial(name);
            if (mat != null) {
                blocks.add(mat);
            }
        }
        this.blockBlacklist = Collections.unmodifiableSet(blocks);

        Map<String, String> msgs = new HashMap<>();
        ConfigurationSection msgSection = config.getConfigurationSection("messages");
        if (msgSection != null) {
            for (String key : msgSection.getKeys(false)) {
                msgs.put(key, msgSection.getString(key, ""));
            }
        }
        this.messages = Collections.unmodifiableMap(msgs);
    }

    private static ConfigurationSection getSection(FileConfiguration config, String key) {
        ConfigurationSection sec = config.getConfigurationSection(key);
        return sec != null ? sec : new MemoryConfiguration();
    }

    /** Parst einen Sound-Namen (Bukkit-Enum-Name oder NONE). */
    private static Sound parseSound(String name) {
        if ("NONE".equalsIgnoreCase(name) || name == null) return null;
        try {
            return Sound.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /** Parst einen Partikel-Namen (Bukkit-Enum-Name oder NONE). */
    private static Particle parseParticle(String name) {
        if (name == null || "NONE".equalsIgnoreCase(name)) return null;
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Liest bis zu drei Helix-Partikel aus der Config. Akzeptiert sowohl eine Liste
     * unter {@code helix-particles} als auch einen Single-String unter
     * {@code helix-particle} für Abwärtskompatibilität. Ungültige oder "NONE"-Einträge
     * werden verworfen; max. drei Partikel.
     */
    private static List<Particle> parseHelixParticles(FileConfiguration config) {
        List<String> raw;
        if (config.isList("helix-particles")) {
            raw = config.getStringList("helix-particles");
        } else {
            String single = config.getString("helix-particles",
                    config.getString("helix-particle", "END_ROD"));
            raw = single == null ? List.of() : List.of(single);
        }
        List<Particle> out = new ArrayList<>(3);
        for (String name : raw) {
            if (out.size() >= 3) break;
            Particle p = parseParticle(name);
            if (p != null) out.add(p);
        }
        return Collections.unmodifiableList(out);
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public int getCooldown()          { return cooldown; }
    public int getMaxAttempts()        { return maxAttempts; }
    public int getPoolSize()           { return poolSize; }
    public int getTeleportDelay()      { return teleportDelay; }
    public int getActionbarDuration()  { return 3; }
    public int getMovedDuration()       { return 3; }

    public Sound    getSound()           { return sound; }
    public float    getSoundVolume()     { return soundVolume; }
    public float    getSoundPitch()      { return soundPitch; }
    public Sound    getStartSound()           { return startSound; }
    public float    getStartSoundVolume()     { return startSoundVolume; }
    public float    getStartSoundPitch()      { return startSoundPitch; }
    public Sound    getCancelSound()           { return cancelSound; }
    public float    getCancelSoundVolume()     { return cancelSoundVolume; }
    public float    getCancelSoundPitch()      { return cancelSoundPitch; }
    public Particle getParticle()        { return particle; }
    public int      getParticleCount()   { return particleCount; }
    public double   getParticleSpreadX() { return particleSpreadX; }
    public double   getParticleSpreadY() { return particleSpreadY; }
    public double   getParticleSpreadZ() { return particleSpreadZ; }
    public double   getParticleSpeed()    { return particleSpeed; }

    public List<Particle> getHelixParticles() { return helixParticles; }

    public DimensionConfig getOverworld() { return overworld; }
    public DimensionConfig getNether()    { return nether; }
    public DimensionConfig getEnd()       { return end; }

    public Set<Biome>    getBiomeBlacklist() { return biomeBlacklist; }
    public Set<Material> getBlockBlacklist() { return blockBlacklist; }

    public String msg(String key) {
        return messages.getOrDefault(key, "<red>[Nachricht '" + key + "' fehlt in config.yml]");
    }

    // ── Inner class ─────────────────────────────────────────────────────────

    public static class DimensionConfig {

        private final int minSpawnDistance;
        private final int maxSpawnDistance;
        private final int minPlayerDistance;

        public DimensionConfig(ConfigurationSection section) {
            this.minSpawnDistance  = section.getInt("min-spawn-distance", 1000);
            this.maxSpawnDistance  = section.getInt("max-spawn-distance", 10000);
            this.minPlayerDistance = section.getInt("min-player-distance", 200);
        }

        public int getMinSpawnDistance()  { return minSpawnDistance; }
        public int getMaxSpawnDistance()  { return maxSpawnDistance; }
        public int getMinPlayerDistance() { return minPlayerDistance; }
    }
}
