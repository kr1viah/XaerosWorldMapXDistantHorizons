package kr1v.xwmxdh;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataCache;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataRepo;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.DhApiResult;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import net.minecraft.world.chunk.WorldChunk;

import java.util.concurrent.*;

public class ChunkManager {
    private final ConcurrentMap<String, ConcurrentMap<Long, WorldChunk>> worldCaches = new ConcurrentHashMap<>();
    private volatile ConcurrentMap<Long, WorldChunk> cache = worldCaches.computeIfAbsent("default", k -> new ConcurrentHashMap<>());
    public IDhApiTerrainDataRepo terrain;
    public IDhApiTerrainDataCache softCache;

    private static long key(int x, int z) {
        return (((long) x) << 32) | (z & 0xffffffffL);
    }

    public void switchh(String newActiveWorld) {
        cache = worldCaches.computeIfAbsent(newActiveWorld, k -> new ConcurrentHashMap<>());
    }

    public WorldChunk get(int x, int z) {
        Long key = key(x, z);
        if (cache.containsKey(key)) return cache.get(key);
        checkFinals();

        IDhApiLevelWrapper level = DhApi.Delayed.worldProxy.getSinglePlayerLevel();

        DhApiResult<DhApiTerrainDataPoint> dummyTest = terrain.getSingleDataPointAtBlockPos(level, x * 16, 0, z * 16, softCache);

        if (dummyTest.success && dummyTest.payload != null) {
            WorldChunk chunk = new DhTerrainDataWorldChunkWrapper(x, z, level);
            cache.put(key, chunk);
            return chunk;
        } else {
            return null;
        }
    }

    private void checkFinals() {
        this.terrain = DhApi.Delayed.terrainRepo;
        this.softCache = terrain.getSoftCache();
    }
}