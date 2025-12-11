package kr1v.xwmxdh;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataCache;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataRepo;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.DhApiResult;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.concurrent.*;

public class ChunkInitializer {
    private final ConcurrentMap<String, ConcurrentMap<Long, WorldChunk>> worldCaches = new ConcurrentHashMap<>();
    private volatile ConcurrentMap<Long, WorldChunk> activeWorldCache = worldCaches.computeIfAbsent("default", k -> new ConcurrentHashMap<>());
    private final ConcurrentMap<Long, CompletableFuture<WorldChunk>> inProgress = new ConcurrentHashMap<>();

    public IDhApiTerrainDataRepo terrain;
    public IDhApiTerrainDataCache softCache;

    private final ThreadFactory threadFactory = (Runnable r) -> {
        Thread t = new Thread(r);
        t.setName("Xaero-world-mapper-" + t.getName());
        t.setDaemon(true);
        return t;
    };


    public void swap(String newActiveWorld) {
        activeWorldCache = worldCaches.computeIfAbsent(newActiveWorld, k -> new ConcurrentHashMap<>());
    }

    public WorldChunk get(int x, int z) {
        Long key = key(x, z);

        WorldChunk cached = activeWorldCache.get(key);
        if (cached != null || inProgress.size() > 5) {
            return cached;
        }

        CompletableFuture<WorldChunk> future = new CompletableFuture<>();
        CompletableFuture<WorldChunk> existing = inProgress.putIfAbsent(key, future);
        if (existing != null) {
            return null;
        }

        cached = activeWorldCache.get(key);
        if (cached != null) {
            inProgress.remove(key, future);
            return cached;
        }

        ConcurrentMap<Long, WorldChunk> currentCache = activeWorldCache;

        Thread t = threadFactory.newThread(() -> {
            try {
                WorldChunk v;
                try {
                    v = generate(x, z);
                } catch (Throwable ex) {
                    future.completeExceptionally(ex);
                    return;
                }

                if (v != null) {
                    currentCache.put(key, v);
                }
                future.complete(v);
            } finally {
                inProgress.remove(key, future);
            }
        });

        if (t.getName() == null || t.getName().isEmpty()) {
            String threadNamePrefix = "Xaero-world-mapper-";
            t.setName(threadNamePrefix + key);
        }

        t.start();
        return null;
    }

    private static long key(int x, int z) {
        return (((long) x) << 32) | (z & 0xffffffffL);
    }

    protected WorldChunk generate(int x, int z) {
        Long key = key(x, z);

        if (activeWorldCache.containsKey(key)) return activeWorldCache.get(key);
        checkFinals();

        IDhApiLevelWrapper level = DhApi.Delayed.worldProxy.getSinglePlayerLevel();
        DhApiResult<DhApiTerrainDataPoint[][][]> data = terrain.getAllTerrainDataAtChunkPos(level, x, z, softCache);
        if (data.success && data.payload != null && data.payload[0][0].length != 0) {
            WorldChunk chunk = new WorldChunkWrapper(x, z, level, data.payload);
            activeWorldCache.put(key, chunk);
            return chunk;
        } else {
            return null;
        }
    }

    private void checkFinals() {
        this.terrain = DhApi.Delayed.terrainRepo;
        this.softCache = terrain.getSoftCache();
    }

    public DhApiTerrainDataPoint getAt(IDhApiLevelWrapper level, BlockPos pos) {
        return getAt(level, pos.getX(), pos.getY(), pos.getZ());
    }

    private DhApiTerrainDataPoint getAt(IDhApiLevelWrapper level, int x, int y, int z) {
        return terrain.getSingleDataPointAtBlockPos(level, x, y, z, softCache).payload;
    }
}
