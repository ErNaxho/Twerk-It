package xyz.elnaxho.sneakgrow;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.function.Consumer;

/**
 * Shared "Area" resolution logic used by both AutoGrow and AutoPlant.
 *
 * Area = 1 -> only the block the player is standing on/at the given center.
 * Area = N -> a spherical region of radius (N - 1) around the center,
 *             always including the center block.
 * Area has no hardcoded maximum - server owners are responsible for picking
 * sane values in config.yml since huge areas scale O(n^3).
 */
public final class AreaUtil {

    private AreaUtil() {
    }

    public static void forEachInArea(Location center, int area, Consumer<Block> consumer) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        int radius = Math.max(0, area - 1);
        int radiusSquared = radius * radius;

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int dx = -radius; dx <= radius; dx++) {
            int dxSquared = dx * dx;
            for (int dy = -radius; dy <= radius; dy++) {
                int dxdySquared = dxSquared + dy * dy;
                if (dxdySquared > radiusSquared) {
                    continue;
                }
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dxdySquared + dz * dz > radiusSquared) {
                        continue;
                    }
                    consumer.accept(world.getBlockAt(cx + dx, cy + dy, cz + dz));
                }
            }
        }
    }
}
