package kr1v.xwmxdh.client;

import kr1v.xwmxdh.WorldChunkWrapper;
import kr1v.xwmxdh.client.mixin.MapWriterAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.misc.Misc;
import xaero.map.region.*;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class XwmxdhClient implements ClientModInitializer {
    private static final BlockingQueue<Chunk> queue = new LinkedBlockingQueue<>();

    public static void submitForWrite(Chunk chunk) {
        try {
            queue.put(chunk);
        } catch (InterruptedException ignored) {}
    }

    private static boolean isRunning = true;

    @Override
    public void onInitializeClient() {
//        new Thread(() -> {
//                    while (isRunning) {
//                        if (!queue.isEmpty()) {
//                            try {
//                                Chunk chunk = queue.poll(200, TimeUnit.MILLISECONDS);
//                                if (chunk == null) continue;
//                                System.out.println(queue.size());
//                                writeChunkDirect(chunk);
//                            } catch (InterruptedException ignored) {}
//                        }
//                    }
//                }, "Chunk consumer thread").start();
//
//        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> isRunning = false);
    }

    // constants (match the ones we used inside MapWriter)
    public static final int TILE_SIZE = 4;                 // chunks per "tileChunk"
    public static final int TILE_CHUNKS_PER_REGION = 8;    // tileChunks per region (8)
    public static final int CHUNK_SIZE = 16;               // blocks per chunk

    /**
     * Write `chunk` into `writer`'s map structures.
     *
     * @param chunk2 the Chunk to write
     */
    public static void writeChunkDirect(Chunk chunk2) {
        WorldMapSession worldMapSession = WorldMapSession.getCurrentSession();
        MapProcessor mapProcessor = worldMapSession.getMapProcessor();
        MapWriter writer = mapProcessor.getMapWriter();
        MapWriterAccessor accessor = (MapWriterAccessor)writer;

        World world = mapProcessor.getWorld();
        WorldChunk chunk = new WorldChunkWrapper(world, chunk2);

        // map/world context via writer
        Registry<Block> blockRegistry = mapProcessor.getWorldBlockRegistry();
        Registry<Biome> biomeRegistry = mapProcessor.worldBiomeRegistry;
        boolean ignoreHeightmaps = mapProcessor.getMapWorld().isIgnoreHeightmaps();
        boolean flowers = WorldMap.settings.flowers;
        int caveDepth = WorldMap.settings.caveModeDepth;

        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        // compute tileChunk coordinates (tileChunk = group of TILE_SIZE chunks)
        int tileChunkX = Math.floorDiv(chunkX, TILE_SIZE);
        int tileChunkZ = Math.floorDiv(chunkZ, TILE_SIZE);

        // local within region and region indices (use floorMod for negatives)
        int tileChunkLocalX = Math.floorMod(tileChunkX, TILE_CHUNKS_PER_REGION);
        int tileChunkLocalZ = Math.floorMod(tileChunkZ, TILE_CHUNKS_PER_REGION);
        int regionX = Math.floorDiv(tileChunkX, TILE_CHUNKS_PER_REGION);
        int regionZ = Math.floorDiv(tileChunkZ, TILE_CHUNKS_PER_REGION);

        MapRegion region = mapProcessor.getLeafMapRegion(accessor.getWritingLayer(), regionX, regionZ, true);
        boolean wasSkipped = true;

        synchronized (region.writerThreadPauseSync) {
            if (region.isWritingPaused()) {
                // cannot write now
                return;
            }

            boolean isProperLoadState;
            boolean regionIsResting;
            boolean createdTileChunk = false;
            MapTileChunk tileChunk = null;

            // same synchronized(region) block pattern as original writeChunk
            synchronized (region) {
                isProperLoadState = region.getLoadState() == 2;
                if (isProperLoadState) {
                    region.registerVisit();
                }
                regionIsResting = region.isResting();
                if (regionIsResting) {
                    tileChunk = region.getChunk(tileChunkLocalX, tileChunkLocalZ);
                    if (isProperLoadState && tileChunk == null) {
                        region.setChunk(tileChunkLocalX, tileChunkLocalZ, tileChunk = new MapTileChunk(region, tileChunkX, tileChunkZ));
                        tileChunk.setLoadState((byte)2);
                        region.setAllCachePrepared(false);
                        createdTileChunk = true;
                    }
                }
            }

            if (!regionIsResting || !isProperLoadState || tileChunk == null) {
                // matches conservative behaviour of MapWriter.writeChunk
                return;
            }

            // compute which sub-tile inside tileChunk this chunk corresponds to (0..TILE_SIZE-1)
            int subTileX = Math.floorMod(chunkX, TILE_SIZE); // 0..3
            int subTileZ = Math.floorMod(chunkZ, TILE_SIZE); // 0..3

            // find or create MapTile for this chunk inside tileChunk
            MapTile mapTile = tileChunk.getTile(subTileX, subTileZ);
            if (mapTile == null) {
                mapTile = mapProcessor.getTilePool().get(mapProcessor.getCurrentDimension(), chunkX, chunkZ);
                tileChunk.setChanged(true);
            }

            // Height bounds
            int worldBottomY = chunk.getBottomY();
            int worldTopY = chunk.getTopYInclusive() + 1;
            boolean cave = accessor.getCaveStart() != Integer.MAX_VALUE;
            boolean fullCave = accessor.getCaveStart() == Integer.MIN_VALUE;
            int lowH = worldBottomY;
            if (cave && !fullCave) {
                lowH = accessor.getCaveStart() + 1 - caveDepth;
                if (lowH < worldBottomY) lowH = worldBottomY;
            }

            // neighbor helpers (mirroring original writeChunk)
            MapTileChunk bottomChunk = null;
            MapTileChunk rightChunk = null;
            MapTileChunk bottomRightChunk = null;
            MapTile bottomTile = null;
            MapTile rightTile = null;
            MapTile bottomRightTile = null;
            boolean triedFetchingBottomChunk = false;
            boolean triedFetchingRightChunk = false;

            // For each column in chunk (0..15), compute heights and call writer.loadPixel to populate accessor.getLoadingObject()
            for (int px = 0; px < CHUNK_SIZE; ++px) {
                for (int pz = 0; pz < CHUNK_SIZE; ++pz) {
                    int mappedHeight = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, px, pz);
                    int startHeight;
                    if (cave && !fullCave) {
                        startHeight = accessor.getCaveStart();
                    } else if (!ignoreHeightmaps && mappedHeight >= worldBottomY) {
                        startHeight = mappedHeight;
                    } else {
                        int sectionBasedHeight = writer.getSectionBasedHeight(chunk, 64);
                        startHeight = sectionBasedHeight;
                    }
                    if (startHeight >= worldTopY) startHeight = worldTopY - 1;

                    MapBlock currentPixel = mapTile.isLoaded() ? mapTile.getBlock(px, pz) : null;

                    // call the existing loadPixel helper of writer
                    writer.loadPixel(world, blockRegistry, accessor.getLoadingObject(), currentPixel,
                            chunk, px, pz, startHeight, lowH, cave, fullCave, mappedHeight,
                            mapTile.wasWrittenOnce(), ignoreHeightmaps, biomeRegistry, flowers, worldBottomY, accessor.getMutableBlockPos3());

                    // fix height, compare, commit
                    accessor.getLoadingObject().fixHeightType(px, pz, mapTile, tileChunk, null, null, null,
                            accessor.getLoadingObject().getEffectiveHeight(accessor.getBlockStateShortShapeCache()), true, accessor.getBlockStateShortShapeCache());

                    boolean equalsSlopesExcluded = accessor.getLoadingObject().equalsSlopesExcluded(currentPixel);
                    boolean fullyEqual = accessor.getLoadingObject().equals(currentPixel, equalsSlopesExcluded);
                    if (!fullyEqual) {
                        MapBlock loadedBlock = accessor.getLoadingObject();
                        mapTile.setBlock(px, pz, loadedBlock);
                        accessor.setLoadingObject(Objects.requireNonNullElseGet(currentPixel, MapBlock::new));
                        tileChunk.setChanged(true);

                        // neighbor-edge slope updates (attempt, matching original writeChunk behavior)
                        boolean zEdge = pz == 15;
                        boolean xEdge = px == 15;
                        if ((zEdge || xEdge) && (currentPixel == null || currentPixel.getEffectiveHeight(accessor.getBlockStateShortShapeCache()) != loadedBlock.getEffectiveHeight(accessor.getBlockStateShortShapeCache()))) {
                            if (zEdge) {
                                // fetch bottom chunk/tile once if we need it
                                if (!triedFetchingBottomChunk && bottomTile == null && subTileZ == 3 && tileChunkLocalZ < TILE_CHUNKS_PER_REGION - 1) {
                                    bottomChunk = region.getChunk(tileChunkLocalX, tileChunkLocalZ + 1);
                                    triedFetchingBottomChunk = true;
                                    bottomTile = bottomChunk != null ? bottomChunk.getTile(subTileX, 0) : null;
                                    if (bottomTile != null) {
                                        bottomChunk.setChanged(true);
                                    }
                                }

                                if (bottomTile != null && bottomTile.isLoaded()) {
                                    // set slope unknown on the matching bottom-tile column
                                    bottomTile.getBlock(px, 0).setSlopeUnknown(true);
                                    if (!xEdge) {
                                        bottomTile.getBlock(px + 1, 0).setSlopeUnknown(true);
                                    }
                                }

                                // if both edges, update bottom-right tile too (uses writer.rightChunk / writer.bottomRightChunk)
                                if (xEdge) {
                                    // ensure writer.rightChunk points to the correct chunk if not already set
                                    if (accessor.getRightChunk() == null && rightChunk != null) {
                                        accessor.setRightChunk(rightChunk);
                                    }
                                    writer.updateBottomRightTile(region, tileChunk, bottomChunk, tileChunkLocalX, tileChunkLocalZ);
                                    // update local bottomRightChunk/Tile references in case updateBottomRightTile populated them
                                    bottomRightChunk = accessor.getBottomRightChunk();
                                    bottomRightTile = bottomRightChunk != null ? bottomRightChunk.getTile(0, 0) : null;
                                    if (bottomRightTile != null) {
                                        bottomRightTile.getBlock(0, 0).setSlopeUnknown(true);
                                    }
                                }
                            } else {
                                // xEdge && !zEdge
                                if (!triedFetchingRightChunk && rightTile == null && subTileX == 3 && tileChunkLocalX < TILE_CHUNKS_PER_REGION - 1) {
                                    rightChunk = region.getChunk(tileChunkLocalX + 1, tileChunkLocalZ);
                                    triedFetchingRightChunk = true;
                                    rightTile = rightChunk != null ? rightChunk.getTile(0, subTileZ) : null;
                                    if (rightTile != null) {
                                        rightChunk.setChanged(true);
                                    }
                                    // store on writer for updateBottomRightTile compatibility
                                    accessor.setRightChunk(rightChunk);
                                }

                                if (rightTile != null && rightTile.isLoaded()) {
                                    // set slope unknown on the matching right-tile column
                                    rightTile.getBlock(0, pz + 1).setSlopeUnknown(true);
                                }
                            }
                        }
                    }
                }
            }

            // finalize tile/tileChunk
            mapTile.setWorldInterpretationVersion(1);
            mapTile.setWrittenCave(accessor.getCaveStart(), caveDepth);
            tileChunk.setTile(subTileX, subTileZ, mapTile, accessor.getBlockStateShortShapeCache());
            mapTile.setWrittenOnce(true);
            mapTile.setLoaded(true);

            Misc.setReflectFieldValue(chunk, XaeroWorldMapCore.chunkCleanField, true);
        } // end synchronized region.writerThreadPauseSync
    }
}
