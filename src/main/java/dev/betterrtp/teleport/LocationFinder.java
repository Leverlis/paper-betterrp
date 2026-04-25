package dev.betterrtp.teleport;

import dev.betterrtp.config.RtpConfig;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class LocationFinder {

    private static final int NETHER_SCAN_MAX_Y = 100;
    private static final int NETHER_SCAN_MIN_Y = 10;

    private final RtpConfig config;

    public LocationFinder(RtpConfig config) {
        this.config = config;
    }

    public CompletableFuture<Location> findSafe(World world, List<Location> playerSnapshot) {
        return switch (world.getEnvironment()) {
            case NORMAL  -> searchSurface(world, config.getOverworld(), 0, playerSnapshot);
            case NETHER  -> searchNether(world, config.getNether(), 0, playerSnapshot);
            case THE_END -> searchSurface(world, config.getEnd(), 0, playerSnapshot);
            default      -> CompletableFuture.failedFuture(
                    new IllegalArgumentException("Unbekannte Dimension: " + world.getEnvironment())
            );
        };
    }

    // ── Overworld / End ─────────────────────────────────────────────────────

    private CompletableFuture<Location> searchSurface(World world,
                                                       RtpConfig.DimensionConfig dim,
                                                       int attempt,
                                                       List<Location> playerSnapshot) {
        if (attempt >= config.getMaxAttempts()) {
            return CompletableFuture.failedFuture(
                    new RuntimeException("Keine sichere Position gefunden nach "
                            + config.getMaxAttempts() + " Versuchen.")
            );
        }

        int[] xz = randomXZInRing(world, dim);
        int x = xz[0], z = xz[1];

        CompletableFuture<Location> result = new CompletableFuture<>();
        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            Block floor = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (isValidSurfaceFloor(world, floor, dim, playerSnapshot)) {
                result.complete(new Location(world, x + 0.5, floor.getY() + 1, z + 0.5));
            } else {
                chain(searchSurface(world, dim, attempt + 1, playerSnapshot), result);
            }
        }).exceptionally(ex -> {
            chain(searchSurface(world, dim, attempt + 1, playerSnapshot), result);
            return null;
        });

        return result;
    }

    private boolean isValidSurfaceFloor(World world, Block floor, RtpConfig.DimensionConfig dim,
                                         List<Location> playerSnapshot) {
        int x = floor.getX(), y = floor.getY(), z = floor.getZ();
        if (!isValidSpawnColumn(world, x, y, z)) return false;
        return checkPlayerDistance(new Location(world, x + 0.5, y + 1, z + 0.5), dim, playerSnapshot);
    }

    // ── Nether ──────────────────────────────────────────────────────────────

    private CompletableFuture<Location> searchNether(World world,
                                                      RtpConfig.DimensionConfig dim,
                                                      int attempt,
                                                      List<Location> playerSnapshot) {
        if (attempt >= config.getMaxAttempts()) {
            return CompletableFuture.failedFuture(
                    new RuntimeException("Keine sichere Nether-Position gefunden nach "
                            + config.getMaxAttempts() + " Versuchen.")
            );
        }

        int[] xz = randomXZInRing(world, dim);
        int x = xz[0], z = xz[1];

        CompletableFuture<Location> result = new CompletableFuture<>();
        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            Location found = scanNetherColumn(world, x, z, dim, playerSnapshot);
            if (found != null) {
                result.complete(found);
            } else {
                chain(searchNether(world, dim, attempt + 1, playerSnapshot), result);
            }
        }).exceptionally(ex -> {
            chain(searchNether(world, dim, attempt + 1, playerSnapshot), result);
            return null;
        });

        return result;
    }

    private Location scanNetherColumn(World world, int x, int z, RtpConfig.DimensionConfig dim,
                                       List<Location> playerSnapshot) {
        for (int y = NETHER_SCAN_MAX_Y; y >= NETHER_SCAN_MIN_Y; y--) {
            if (!isValidSpawnColumn(world, x, y, z)) continue;
            Location candidate = new Location(world, x + 0.5, y + 1, z + 0.5);
            if (checkPlayerDistance(candidate, dim, playerSnapshot)) return candidate;
        }
        return null;
    }

    private boolean isValidSpawnColumn(World world, int x, int y, int z) {
        Block floor = world.getBlockAt(x, y, z);
        if (floor.isPassable() || config.getBlockBlacklist().contains(floor.getType())) return false;
        Block body = world.getBlockAt(x, y + 1, z);
        Block head = world.getBlockAt(x, y + 2, z);
        if (!body.isPassable() || config.getBlockBlacklist().contains(body.getType())) return false;
        if (!head.isPassable() || config.getBlockBlacklist().contains(head.getType())) return false;
        return !isBiomeBlacklisted(world, x, y, z);
    }

    // Sqrt-Trick für gleichmäßige Flächenverteilung – ohne den dreht sich alles zu sehr zur Mitte
    private int[] randomXZInRing(World world, RtpConfig.DimensionConfig dim) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double minR = dim.getMinSpawnDistance();
        double maxR = dim.getMaxSpawnDistance();
        double angle = rng.nextDouble() * 2 * Math.PI;
        double r = Math.sqrt(rng.nextDouble() * (maxR * maxR - minR * minR) + minR * minR);
        Location spawn = world.getSpawnLocation();
        return new int[]{
            (int) (spawn.getX() + Math.cos(angle) * r),
            (int) (spawn.getZ() + Math.sin(angle) * r)
        };
    }

    private boolean checkPlayerDistance(Location candidate, RtpConfig.DimensionConfig dim,
                                         List<Location> playerSnapshot) {
        double minDistSq = (double) dim.getMinPlayerDistance() * dim.getMinPlayerDistance();
        for (Location playerLoc : playerSnapshot) {
            if (!playerLoc.getWorld().equals(candidate.getWorld())) continue;
            if (playerLoc.distanceSquared(candidate) < minDistSq) return false;
        }
        return true;
    }

    private boolean isBiomeBlacklisted(World world, int x, int y, int z) {
        Biome biome = world.getBiome(x, y, z);
        return config.getBiomeBlacklist().contains(biome);
    }

    private static void chain(CompletableFuture<Location> source, CompletableFuture<Location> target) {
        source.thenAccept(target::complete)
              .exceptionally(ex -> { target.completeExceptionally(ex); return null; });
    }
}
