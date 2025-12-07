package kr1v.xwmxdh;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.chunk.Chunk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Xwmxdh implements ModInitializer {
    private static Long2ObjectMap<Chunk> activeChunks;
    private static final Map<String, Long2ObjectMap<Chunk>> chunks = new ConcurrentHashMap<>();
    private static final Object lock = new Object();

    private static long key(int x, int z) {
        return (((long) x) << 32) | (z & 0xffffffffL);
    }

    public static void put(int x, int z, Chunk chunk) {
        synchronized (lock) {
            activeChunks.put(key(x, z), chunk);
        }
    }

    public static Chunk get(int x, int z) {
        synchronized (lock) {
            return activeChunks.get(key(x, z));
        }
    }

    public static void switchh(String newWorldAndDimension) {
        synchronized (lock) {
            chunks.computeIfAbsent(newWorldAndDimension, (k) -> new Long2ObjectOpenHashMap<>());
            activeChunks = chunks.get(newWorldAndDimension);
        }
    }

    @Override
    public void onInitialize() {}
}