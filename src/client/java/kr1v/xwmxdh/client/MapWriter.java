package kr1v.xwmxdh.client;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BushBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.TallFlowerBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.TransparentBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.resource.Resource;
import net.minecraft.state.State;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.logging.log4j.Logger;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.biome.BiomeGetter;
import xaero.map.biome.BlockTintProvider;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.exception.SilentException;
import xaero.map.gui.GuiMap;
import xaero.map.misc.CachedFunction;
import xaero.map.misc.Misc;
import xaero.map.mods.SupportMods;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapBlock;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTile;
import xaero.map.region.MapTileChunk;
import xaero.map.region.OverlayBuilder;
import xaero.map.region.OverlayManager;

@SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter", "StringConcatenationArgumentToLogCall"})
public abstract class MapWriter {
    private static final int TILE_SIZE = 4;
    private static final int TILE_CHUNKS_PER_REGION = 8;
    private static final int CHUNK_SIZE = 16;

    public static final String[] DEFAULT_RESOURCE = new String[]{"minecraft", ""};
    private int X;
    private int Z;
    private int playerChunkX;
    private int playerChunkZ;
    private int loadDistance;
    private int startTileChunkX;
    private int startTileChunkZ;
    private int endTileChunkX;
    private int endTileChunkZ;
    private int insideX;
    private int insideZ;
    private long updateCounter;
    private int caveStart;
    private int writingLayer = Integer.MAX_VALUE;
    private int writtenCaveStart = Integer.MAX_VALUE;
    private boolean clearCachedColours;
    private MapBlock loadingObject = new MapBlock();
    private final OverlayBuilder overlayBuilder;
    private final BlockPos.Mutable mutableLocalPos;
    private final BlockPos.Mutable mutableGlobalPos;
    private long lastWrite = -1L;
    private long lastWriteTry = -1L;
    private int workingFrameCount;
    private long framesFreedTime = -1L;
    public long writeFreeSinceLastWrite = -1L;
    private int writeFreeSizeTiles;
    private int writeFreeFullUpdateTargetTime;
    private MapProcessor mapProcessor;
    private final ArrayList<BlockState> buggedStates;
    private final BlockStateShortShapeCache blockStateShortShapeCache;
    private int topH;
    private final CachedFunction<State<?, ?>, Boolean> transparentCache;
    private int firstTransparentStateY;
    private final CachedFunction<FluidState, BlockState> fluidToBlock;
    private final BiomeGetter biomeGetter;
    private final ArrayList<MapRegion> regionBuffer = new ArrayList<>();
    private MapTileChunk rightChunk = null;
    private MapTileChunk bottomRightChunk = null;
    private final HashMap<String, Integer> textureColours = new HashMap<>();
    private final HashMap<BlockState, Integer> blockColours = new HashMap<>();
    private final Object2IntMap<BlockState> blockTintIndices;
    private long lastLayerSwitch;
    protected List<BlockModelPart> reusableBlockModelPartList;
    private BlockState lastBlockStateForTextureColor = null;
    private int lastBlockStateForTextureColorResult = -1;

    public MapWriter(OverlayManager overlayManager, BlockStateShortShapeCache blockStateShortShapeCache, BiomeGetter biomeGetter) {
        this.overlayBuilder = new OverlayBuilder(overlayManager);
        this.mutableLocalPos = new BlockPos.Mutable();
        this.mutableGlobalPos = new BlockPos.Mutable();
        this.buggedStates = new ArrayList<>();
        this.blockStateShortShapeCache = blockStateShortShapeCache;
        this.transparentCache = new CachedFunction<>(this::shouldOverlay);
        this.fluidToBlock = new CachedFunction<>(FluidState::getBlockState);
        this.biomeGetter = biomeGetter;
        this.blockTintIndices = new Object2IntOpenHashMap<>();
        this.reusableBlockModelPartList = new ArrayList<>();
    }

    protected abstract boolean blockStateHasTranslucentRenderType(BlockState var1);

    public void onRender(OverlayManager overlayManager) {
        try {
            if (WorldMap.crashHandler.getCrashedBy() == null) {
                synchronized(this.mapProcessor.renderThreadPauseSync) {
                    if (!this.mapProcessor.isWritingPaused() && !this.mapProcessor.isWaitingForWorldUpdate() && this.mapProcessor.getMapSaveLoad().isRegionDetectionComplete() && this.mapProcessor.isCurrentMultiworldWritable()) {
                        if (this.mapProcessor.getWorld() == null || this.mapProcessor.isCurrentMapLocked() || this.mapProcessor.getMapWorld().isCacheOnlyMode()) {
                            return;
                        }

                        if (this.mapProcessor.getCurrentWorldId() != null && !this.mapProcessor.ignoreWorld(this.mapProcessor.getWorld()) && (WorldMap.settings.updateChunks || WorldMap.settings.loadChunks || this.mapProcessor.getMapWorld().getCurrentDimension().isUsingWorldSave())) {
                            double playerX;
                            double playerZ;
                            synchronized(this.mapProcessor.mainStuffSync) {
                                if (this.mapProcessor.mainWorld != this.mapProcessor.getWorld()) {
                                    return;
                                }

                                if (this.mapProcessor.getWorld().getRegistryKey() != this.mapProcessor.getMapWorld().getCurrentDimensionId()) {
                                    return;
                                }

                                playerX = this.mapProcessor.mainPlayerX;
                                playerZ = this.mapProcessor.mainPlayerZ;
                            }

                            XaeroWorldMapCore.ensureField();
                            int lengthX = this.endTileChunkX - this.startTileChunkX + 1;
                            int lengthZ = this.endTileChunkZ - this.startTileChunkZ + 1;
                            if (this.lastWriteTry == -1L) {
                                lengthX = 3;
                                lengthZ = 3;
                            }

                            int sizeTileChunks = lengthX * lengthZ;
                            int sizeTiles = sizeTileChunks * 4 * 4;
                            int sizeBasedTargetTime = sizeTiles * 1000 / 1500;
                            int fullUpdateTargetTime = Math.max(100, sizeBasedTargetTime);
                            long time = System.currentTimeMillis();
                            long passed = this.lastWrite == -1L ? 0L : time - this.lastWrite;
                            if (this.lastWriteTry == -1L || this.writeFreeSizeTiles != sizeTiles || this.writeFreeFullUpdateTargetTime != fullUpdateTargetTime || this.workingFrameCount > 30) {
                                this.framesFreedTime = time;
                                this.writeFreeSizeTiles = sizeTiles;
                                this.writeFreeFullUpdateTargetTime = fullUpdateTargetTime;
                                this.workingFrameCount = 0;
                            }

                            long sinceLastWrite = Math.min(passed, this.writeFreeSinceLastWrite);
                            if (this.framesFreedTime != -1L) {
                                sinceLastWrite = time - this.framesFreedTime;
                            }

                            long tilesToUpdate = Math.min(sinceLastWrite * (long)sizeTiles / (long)fullUpdateTargetTime, 100L);
                            if (this.lastWrite == -1L || tilesToUpdate != 0L) {
                                this.lastWrite = time;
                            }

                            if (tilesToUpdate != 0L) {
                                if (this.framesFreedTime != -1L) {
                                    this.writeFreeSinceLastWrite = sinceLastWrite;
                                    this.framesFreedTime = -1L;
                                } else {
                                    int timeLimit = (int)(Math.min(sinceLastWrite, 50L) * 86960L);
                                    long writeStartNano = System.nanoTime();
                                    Registry<Biome> biomeRegistry = this.mapProcessor.worldBiomeRegistry;
                                    boolean loadChunks = WorldMap.settings.loadChunks || this.mapProcessor.getMapWorld().getCurrentDimension().isUsingWorldSave();
                                    boolean updateChunks = WorldMap.settings.updateChunks || this.mapProcessor.getMapWorld().getCurrentDimension().isUsingWorldSave();
                                    boolean ignoreHeightmaps = this.mapProcessor.getMapWorld().isIgnoreHeightmaps();
                                    boolean flowers = WorldMap.settings.flowers;
                                    boolean detailedDebug = WorldMap.settings.detailed_debug;
                                    int caveDepth = WorldMap.settings.caveModeDepth;
                                    BlockTintProvider blockTintProvider = this.mapProcessor.getWorldBlockTintProvider();
                                    ClientWorld world = this.mapProcessor.getWorld();
                                    Registry<Block> blockRegistry = this.mapProcessor.getWorldBlockRegistry();

                                    for(int i = 0; (long)i < tilesToUpdate; ++i) {
                                        if (this.writeMap(world, blockRegistry, playerX, playerZ, biomeRegistry, overlayManager, loadChunks, updateChunks, ignoreHeightmaps, flowers, detailedDebug, blockTintProvider, caveDepth)) {
                                            --i;
                                        }

                                        if (System.nanoTime() - writeStartNano >= (long)timeLimit) {
                                            break;
                                        }
                                    }

                                    ++this.workingFrameCount;
                                }
                            }

                            this.lastWriteTry = time;
                            int startRegionX = this.startTileChunkX >> 3;
                            int startRegionZ = this.startTileChunkZ >> 3;
                            int endRegionX = this.endTileChunkX >> 3;
                            int endRegionZ = this.endTileChunkZ >> 3;
                            boolean shouldRequestLoading;
                            LeveledRegion<?> nextToLoad = this.mapProcessor.getMapSaveLoad().getNextToLoadByViewing();
                            if (nextToLoad != null) {
                                shouldRequestLoading = nextToLoad.shouldAllowAnotherRegionToLoad();
                            } else {
                                shouldRequestLoading = true;
                            }

                            this.regionBuffer.clear();
                            int comparisonChunkX = this.playerChunkX - 16;
                            int comparisonChunkZ = this.playerChunkZ - 16;
                            LeveledRegion.setComparison(comparisonChunkX, comparisonChunkZ, 0, comparisonChunkX, comparisonChunkZ);

                            for(int visitRegionX = startRegionX; visitRegionX <= endRegionX; ++visitRegionX) {
                                for(int visitRegionZ = startRegionZ; visitRegionZ <= endRegionZ; ++visitRegionZ) {
                                    MapRegion visitRegion = this.mapProcessor.getLeafMapRegion(this.writingLayer, visitRegionX, visitRegionZ, true);
                                    if (/*visitRegion != null && */visitRegion.getLoadState() == 2) {
                                        visitRegion.registerVisit();
                                    }

                                    synchronized(visitRegion) {
                                        if (visitRegion.isResting() && shouldRequestLoading && visitRegion.canRequestReload_unsynced() && visitRegion.getLoadState() != 2) {
                                            visitRegion.calculateSortingChunkDistance();
                                            Misc.addToListOfSmallest(10, this.regionBuffer, visitRegion);
                                        }
                                    }
                                }
                            }

                            int toRequest = 1;
                            int counter = 0;

                            for(int i = 0; i < this.regionBuffer.size() && counter < toRequest; ++i) {
                                MapRegion region = this.regionBuffer.get(i);
                                if (region != nextToLoad || this.regionBuffer.size() <= 1) {
                                    synchronized(region) {
                                        if (region.canRequestReload_unsynced() && region.getLoadState() != 2) {
                                            region.setBeingWritten(true);
                                            this.mapProcessor.getMapSaveLoad().requestLoad(region, "writing");
                                            if (counter == 0) {
                                                this.mapProcessor.getMapSaveLoad().setNextToLoadByViewing(region);
                                            }

                                            ++counter;
                                            if (region.getLoadState() == 4) {
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            WorldMap.crashHandler.setCrashedBy(e);
        }
    }

    private int getWriteDistance() {
        int limit = this.mapProcessor.getMapWorld().getCurrentDimension().isUsingWorldSave() ? Integer.MAX_VALUE : WorldMap.settings.mapWritingDistance;
        if (limit < 0) {
            limit = Integer.MAX_VALUE;
        }

        return Math.min(limit, Math.min(32, MinecraftClient.getInstance().options.getViewDistance().getValue()));
    }

    public boolean writeMap(World world, Registry<Block> blockRegistry, double playerX, double playerZ, Registry<Biome> biomeRegistry, OverlayManager overlayManager, boolean loadChunks, boolean updateChunks, boolean ignoreHeightmaps, boolean flowers, boolean detailedDebug, BlockTintProvider blockTintProvider, int caveDepth) {
        boolean onlyLoad = loadChunks && (!updateChunks || this.updateCounter % 5L != 0L);
        synchronized(world) {
            if (this.insideX == 0 && this.insideZ == 0) {
                if (this.X == 0 && this.Z == 0) {
                    this.writtenCaveStart = this.caveStart;
                }

                this.mapProcessor.updateCaveStart();
                int newWritingLayer = this.mapProcessor.getCurrentCaveLayer();
                if (this.writingLayer != newWritingLayer && System.currentTimeMillis() - this.lastLayerSwitch > 300L) {
                    this.writingLayer = newWritingLayer;
                    this.lastLayerSwitch = System.currentTimeMillis();
                }

                this.loadDistance = this.getWriteDistance();
                if (this.writingLayer != Integer.MAX_VALUE && !(MinecraftClient.getInstance().currentScreen instanceof GuiMap)) {
                    this.loadDistance = Math.min(16, this.loadDistance);
                }

                this.caveStart = this.mapProcessor.getMapWorld().getCurrentDimension().getLayeredMapRegions().getLayer(this.writingLayer).getCaveStart();
                if (this.caveStart != this.writtenCaveStart) {
                    this.loadDistance = Math.min(4, this.loadDistance);
                }

                this.playerChunkX = (int)Math.floor(playerX) >> 4;
                this.playerChunkZ = (int)Math.floor(playerZ) >> 4;
                this.startTileChunkX = this.playerChunkX - this.loadDistance >> 2;
                this.startTileChunkZ = this.playerChunkZ - this.loadDistance >> 2;
                this.endTileChunkX = this.playerChunkX + this.loadDistance >> 2;
                this.endTileChunkZ = this.playerChunkZ + this.loadDistance >> 2;
            }

            int tileChunkX = this.startTileChunkX + this.X;
            int tileChunkZ = this.startTileChunkZ + this.Z;
            int tileChunkLocalX = tileChunkX & 7;
            int tileChunkLocalZ = tileChunkZ & 7;
            int chunkX = tileChunkX * 4 + this.insideX;
            int chunkZ = tileChunkZ * 4 + this.insideZ;
            boolean wasSkipped = this.writeChunk(world, blockRegistry, this.loadDistance, onlyLoad, biomeRegistry, overlayManager, loadChunks, updateChunks, ignoreHeightmaps, flowers, detailedDebug, blockTintProvider, caveDepth, this.caveStart, this.writingLayer, tileChunkX, tileChunkZ, tileChunkLocalX, tileChunkLocalZ, chunkX, chunkZ);
            return wasSkipped && (Math.abs(chunkX - this.playerChunkX) > 8 || Math.abs(chunkZ - this.playerChunkZ) > 8);
        }
    }

    public boolean writeChunk(World world, Registry<Block> blockRegistry, int distance, boolean onlyLoad, Registry<Biome> biomeRegistry, OverlayManager overlayManager, boolean loadChunks, boolean updateChunks, boolean ignoreHeightmaps, boolean flowers, boolean detailedDebug, BlockTintProvider blockTintProvider, int caveDepth, int caveStart, int layerToWrite, int tileChunkX, int tileChunkZ, int tileChunkLocalX, int tileChunkLocalZ, int chunkX, int chunkZ) {
        int regionX = tileChunkX >> 3;
        int regionZ = tileChunkZ >> 3;
        MapTileChunk tileChunk = null;
        this.rightChunk = null;
        MapTileChunk bottomChunk = null;
        this.bottomRightChunk = null;
        int worldBottomY = world.getBottomY();
        int worldTopY = world.getTopYInclusive() + 1;
        MapRegion region = this.mapProcessor.getLeafMapRegion(layerToWrite, regionX, regionZ, true);
        boolean wasSkipped = true;
        synchronized(region.writerThreadPauseSync) {
            if (!region.isWritingPaused()) {
                boolean createdTileChunk = false;
                boolean regionIsResting;
                boolean isProperLoadState;
                synchronized(region) {
                    isProperLoadState = region.getLoadState() == 2;
                    if (isProperLoadState) {
                        region.registerVisit();
                    }

                    regionIsResting = region.isResting();
                    if (regionIsResting) {
                        region.setBeingWritten(true);
                        tileChunk = region.getChunk(tileChunkLocalX, tileChunkLocalZ);
                        if (isProperLoadState && tileChunk == null) {
                            region.setChunk(tileChunkLocalX, tileChunkLocalZ, tileChunk = new MapTileChunk(region, tileChunkX, tileChunkZ));
                            tileChunk.setLoadState((byte)2);
                            region.setAllCachePrepared(false);
                            createdTileChunk = true;
                        }

                        if (!region.isNormalMapData()) {
                            region.getDim().getLayeredMapRegions().applyToEachLoadedLayer((ix, layer) -> {
                                if (ix != region.getCaveLayer()) {
                                    MapRegion sameRegionAnotherLayer = this.mapProcessor.getLeafMapRegion(ix, regionX, regionZ, true);
                                    sameRegionAnotherLayer.setOutdatedWithOtherLayers(true);
                                    sameRegionAnotherLayer.setHasHadTerrain();
                                }

                            });
                        }
                    }
                }

                if (regionIsResting && isProperLoadState) {
                    if (tileChunk.getLoadState() == 2) {
                        if (!tileChunk.getLeafTexture().shouldUpload()) {
                            boolean cave = caveStart != Integer.MAX_VALUE;
                            boolean fullCave = caveStart == Integer.MIN_VALUE;
                            int lowH = worldBottomY;
                            if (cave && !fullCave) {
                                lowH = caveStart + 1 - caveDepth;
                                if (lowH < worldBottomY) {
                                    lowH = worldBottomY;
                                }
                            }

                            if (chunkX >= this.playerChunkX - distance && chunkX <= this.playerChunkX + distance && chunkZ >= this.playerChunkZ - distance && chunkZ <= this.playerChunkZ + distance) {
                                WorldChunk chunk = (WorldChunk)world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                                MapTile mapTile = tileChunk.getTile(this.insideX, this.insideZ);
                                boolean chunkUpdated;

                                try {
                                    chunkUpdated = chunk != null && (mapTile == null || mapTile.getWrittenCaveStart() != caveStart || mapTile.getWrittenCaveDepth() != caveDepth || !(Boolean)XaeroWorldMapCore.chunkCleanField.get(chunk));
                                } catch (IllegalAccessException | IllegalArgumentException e) {
                                    throw new RuntimeException(e);
                                }

                                if (chunkUpdated && !(chunk instanceof EmptyChunk)) {
                                    boolean edgeChunk = false;

                                    label365:
                                    for(int i = -1; i < 2; ++i) {
                                        for(int j = -1; j < 2; ++j) {
                                            if (i != 0 || j != 0) {
                                                WorldChunk neighbor = world.getChunk(chunkX + i, chunkZ + j);
                                                if (neighbor == null || neighbor instanceof EmptyChunk) {
                                                    edgeChunk = true;
                                                    break label365;
                                                }
                                            }
                                        }
                                    }

                                    if (!edgeChunk && (mapTile == null && loadChunks || mapTile != null && updateChunks && (!onlyLoad || mapTile.getWrittenCaveStart() != caveStart || mapTile.getWrittenCaveDepth() != caveDepth))) {
                                        wasSkipped = false;
                                        if (mapTile == null) {
                                            mapTile = this.mapProcessor.getTilePool().get(this.mapProcessor.getCurrentDimension(), chunkX, chunkZ);
                                            tileChunk.setChanged(true);
                                        }

                                        MapTileChunk prevTileChunk = tileChunk.getNeighbourTileChunk(0, -1, this.mapProcessor, false);
                                        MapTileChunk prevTileChunkDiagonal = tileChunk.getNeighbourTileChunk(-1, -1, this.mapProcessor, false);
                                        MapTileChunk prevTileChunkHorisontal = tileChunk.getNeighbourTileChunk(-1, 0, this.mapProcessor, false);
                                        int sectionBasedHeight = this.getSectionBasedHeight(chunk, 64);
                                        Heightmap.Type typeWorldSurface = Type.WORLD_SURFACE;
                                        MapTile bottomTile = this.insideZ < 3 ? tileChunk.getTile(this.insideX, this.insideZ + 1) : null;
                                        MapTile rightTile = this.insideX < 3 ? tileChunk.getTile(this.insideX + 1, this.insideZ) : null;
                                        boolean triedFetchingBottomChunk = false;
                                        boolean triedFetchingRightChunk = false;

                                        for(int x = 0; x < 16; ++x) {
                                            for(int z = 0; z < 16; ++z) {
                                                int mappedHeight = chunk.sampleHeightmap(typeWorldSurface, x, z);
                                                int startHeight;
                                                if (cave && !fullCave) {
                                                    startHeight = caveStart;
                                                } else if (!ignoreHeightmaps && mappedHeight >= worldBottomY) {
                                                    startHeight = mappedHeight;
                                                } else {
                                                    startHeight = sectionBasedHeight;
                                                }

                                                if (startHeight >= worldTopY) {
                                                    startHeight = worldTopY - 1;
                                                }

                                                MapBlock currentPixel = mapTile.isLoaded() ? mapTile.getBlock(x, z) : null;
                                                this.loadPixel(world, blockRegistry, this.loadingObject, currentPixel, chunk, x, z, startHeight, lowH, cave, fullCave, mappedHeight, mapTile.wasWrittenOnce(), ignoreHeightmaps, biomeRegistry, flowers, worldBottomY);
                                                this.loadingObject.fixHeightType(x, z, mapTile, tileChunk, prevTileChunk, prevTileChunkDiagonal, prevTileChunkHorisontal, this.loadingObject.getEffectiveHeight(this.blockStateShortShapeCache), true, this.blockStateShortShapeCache);
                                                boolean equalsSlopesExcluded = this.loadingObject.equalsSlopesExcluded(currentPixel);
                                                boolean fullyEqual = this.loadingObject.equals(currentPixel, equalsSlopesExcluded);
                                                if (!fullyEqual) {
                                                    MapBlock loadedBlock = this.loadingObject;
                                                    mapTile.setBlock(x, z, loadedBlock);
                                                    this.loadingObject = Objects.requireNonNullElseGet(currentPixel, MapBlock::new);

                                                    if (!equalsSlopesExcluded) {
                                                        tileChunk.setChanged(true);
                                                        boolean zEdge = z == 15;
                                                        boolean xEdge = x == 15;
                                                        if ((zEdge || xEdge) && (currentPixel == null || currentPixel.getEffectiveHeight(this.blockStateShortShapeCache) != loadedBlock.getEffectiveHeight(this.blockStateShortShapeCache))) {
                                                            if (zEdge) {
                                                                if (!triedFetchingBottomChunk && bottomTile == null && this.insideZ == 3 && tileChunkLocalZ < 7) {
                                                                    bottomChunk = region.getChunk(tileChunkLocalX, tileChunkLocalZ + 1);
                                                                    triedFetchingBottomChunk = true;
                                                                    bottomTile = bottomChunk != null ? bottomChunk.getTile(this.insideX, 0) : null;
                                                                    if (bottomTile != null) {
                                                                        bottomChunk.setChanged(true);
                                                                    }
                                                                }

                                                                if (bottomTile != null && bottomTile.isLoaded()) {
                                                                    bottomTile.getBlock(x, 0).setSlopeUnknown(true);
                                                                    if (!xEdge) {
                                                                        bottomTile.getBlock(x + 1, 0).setSlopeUnknown(true);
                                                                    }
                                                                }

                                                                if (xEdge) {
                                                                    this.updateBottomRightTile(region, tileChunk, bottomChunk, tileChunkLocalX, tileChunkLocalZ);
                                                                }
                                                            } else {
                                                                if (!triedFetchingRightChunk && rightTile == null && this.insideX == 3 && tileChunkLocalX < 7) {
                                                                    this.rightChunk = region.getChunk(tileChunkLocalX + 1, tileChunkLocalZ);
                                                                    triedFetchingRightChunk = true;
                                                                    rightTile = this.rightChunk != null ? this.rightChunk.getTile(0, this.insideZ) : null;
                                                                    if (rightTile != null) {
                                                                        this.rightChunk.setChanged(true);
                                                                    }
                                                                }

                                                                if (rightTile != null && rightTile.isLoaded()) {
                                                                    rightTile.getBlock(0, z + 1).setSlopeUnknown(true);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        mapTile.setWorldInterpretationVersion(1);
                                        if (mapTile.getWrittenCaveStart() != caveStart) {
                                            tileChunk.setChanged(true);
                                        }

                                        mapTile.setWrittenCave(caveStart, caveDepth);
                                        tileChunk.setTile(this.insideX, this.insideZ, mapTile, this.blockStateShortShapeCache);
                                        mapTile.setWrittenOnce(true);
                                        mapTile.setLoaded(true);
                                        Misc.setReflectFieldValue(chunk, XaeroWorldMapCore.chunkCleanField, true);
                                    }
                                }
                            }
                        }

                        if (createdTileChunk) {
                            if (tileChunk.includeInSave()) {
                                tileChunk.setHasHadTerrain();
                            }

                            this.mapProcessor.getMapRegionHighlightsPreparer().prepare(region, tileChunkLocalX, tileChunkLocalZ, false);
                            if (!tileChunk.includeInSave() && !tileChunk.hasHighlightsIfUndiscovered()) {
                                region.setChunk(tileChunkLocalX, tileChunkLocalZ, null);
                                tileChunk = null;
                            }
                        }
                    }

                    if (tileChunk != null && this.insideX == 3 && this.insideZ == 3 && tileChunk.wasChanged()) {
                        tileChunk.updateBuffers(this.mapProcessor, blockTintProvider, overlayManager, detailedDebug, this.blockStateShortShapeCache);
                        if (bottomChunk == null && tileChunkLocalZ < 7) {
                            bottomChunk = region.getChunk(tileChunkLocalX, tileChunkLocalZ + 1);
                        }

                        if (this.rightChunk == null && tileChunkLocalX < 7) {
                            this.rightChunk = region.getChunk(tileChunkLocalX + 1, tileChunkLocalZ);
                        }

                        if (this.bottomRightChunk == null && tileChunkLocalX < 7 && tileChunkLocalZ < 7) {
                            this.bottomRightChunk = region.getChunk(tileChunkLocalX + 1, tileChunkLocalZ + 1);
                        }

                        if (bottomChunk != null && bottomChunk.wasChanged()) {
                            bottomChunk.updateBuffers(this.mapProcessor, blockTintProvider, overlayManager, detailedDebug, this.blockStateShortShapeCache);
                            bottomChunk.setChanged(false);
                        }

                        if (this.rightChunk != null && this.rightChunk.wasChanged()) {
                            this.rightChunk.setToUpdateBuffers(true);
                            this.rightChunk.setChanged(false);
                        }

                        if (this.bottomRightChunk != null && this.bottomRightChunk.wasChanged()) {
                            this.bottomRightChunk.setToUpdateBuffers(true);
                            this.bottomRightChunk.setChanged(false);
                        }

                        tileChunk.setChanged(false);
                    }
                }
            } else {
                this.insideX = 3;
                this.insideZ = 3;
            }
        }

        ++this.insideZ;
        if (this.insideZ > 3) {
            this.insideZ = 0;
            ++this.insideX;
            if (this.insideX > 3) {
                this.insideX = 0;
                ++this.Z;
                if (this.Z > this.endTileChunkZ - this.startTileChunkZ) {
                    this.Z = 0;
                    ++this.X;
                    if (this.X > this.endTileChunkX - this.startTileChunkX) {
                        this.X = 0;
                        ++this.updateCounter;
                    }
                }
            }
        }

        return wasSkipped;
    }

    public void updateBottomRightTile(MapRegion region, MapTileChunk tileChunk, MapTileChunk bottomChunk, int tileChunkLocalX, int tileChunkLocalZ) {
        MapTile bottomRightTile = this.insideX < 3 && this.insideZ < 3 ? tileChunk.getTile(this.insideX + 1, this.insideZ + 1) : null;
        if (bottomRightTile == null) {
            if (this.insideX == 3 && tileChunkLocalX < 7) {
                if (this.insideZ == 3) {
                    if (tileChunkLocalZ < 7) {
                        this.bottomRightChunk = region.getChunk(tileChunkLocalX + 1, tileChunkLocalZ + 1);
                    }

                    bottomRightTile = this.bottomRightChunk != null ? this.bottomRightChunk.getTile(0, 0) : null;
                    if (bottomRightTile != null) {
                        this.bottomRightChunk.setChanged(true);
                    }
                } else {
                    if (this.rightChunk == null) {
                        this.rightChunk = region.getChunk(tileChunkLocalX + 1, tileChunkLocalZ);
                    }

                    bottomRightTile = this.rightChunk != null ? this.rightChunk.getTile(0, this.insideZ + 1) : null;
                    if (bottomRightTile != null) {
                        this.rightChunk.setChanged(true);
                    }
                }
            } else if (this.insideX != 3 && this.insideZ == 3 && tileChunkLocalZ < 7) {
                bottomRightTile = bottomChunk != null ? bottomChunk.getTile(this.insideX + 1, 0) : null;
                if (bottomRightTile != null) {
                    bottomChunk.setChanged(true);
                }
            }
        }

        if (bottomRightTile != null && bottomRightTile.isLoaded()) {
            bottomRightTile.getBlock(0, 0).setSlopeUnknown(true);
        }

    }

    public int getSectionBasedHeight(WorldChunk bchunk, int startY) {
        ChunkSection[] sections = bchunk.getSectionArray();
        if (sections.length == 0) {
            return 0;
        } else {
            int chunkBottomY = bchunk.getBottomY();
            int playerSection = Math.min(startY - chunkBottomY >> 4, sections.length - 1);
            if (playerSection < 0) {
                playerSection = 0;
            }

            int result = 0;

            for(int i = playerSection; i < sections.length; ++i) {
                ChunkSection searchedSection = sections[i];
                if (!searchedSection.isEmpty()) {
                    result = chunkBottomY + (i << 4) + 15;
                }
            }

            if (playerSection > 0 && result == 0) {
                for(int i = playerSection - 1; i >= 0; --i) {
                    ChunkSection searchedSection = sections[i];
                    if (!searchedSection.isEmpty()) {
                        result = chunkBottomY + (i << 4) + 15;
                        break;
                    }
                }
            }

            return result;
        }
    }

    public boolean isGlowing(BlockState state) {
        return (double)state.getLuminance() >= (double)0.5F;
    }

    private boolean shouldOverlayCached(State<?, ?> state) {
        return this.transparentCache.apply(state);
    }

    public boolean shouldOverlay(State<?, ?> state) {
        if (state instanceof BlockState blockState) {
            return blockState.getBlock() instanceof AirBlock || blockState.getBlock() instanceof TransparentBlock || this.blockStateHasTranslucentRenderType(blockState);
        } else {
            FluidState fluidState = (FluidState)state;
            return RenderLayers.getFluidLayer(fluidState) == RenderLayer.getTranslucent();
        }
    }

    public boolean isInvisible(BlockState state, Block b, boolean flowers) {
        if (!(b instanceof FluidBlock) && state.getRenderType() == BlockRenderType.INVISIBLE) {
            return true;
        } else if (b == Blocks.TORCH) {
            return true;
        } else if (b == Blocks.SHORT_GRASS) {
            return true;
        } else if (b != Blocks.GLASS && b != Blocks.GLASS_PANE) {
            boolean isFlower = b instanceof TallFlowerBlock || b instanceof FlowerBlock || b instanceof BushBlock && state.isIn(BlockTags.FLOWERS);
            if (b instanceof TallPlantBlock && !isFlower) {
                return true;
            } else if (isFlower && !flowers) {
                return true;
            } else {
                synchronized(this.buggedStates) {
                    return this.buggedStates.contains(state);
                }
            }
        } else {
            return true;
        }
    }

    public boolean hasVanillaColor(BlockState state, World world, Registry<Block> blockRegistry, BlockPos pos) {
        MapColor materialColor = null;

        try {
            materialColor = state.getMapColor(world, pos);
        } catch (Throwable var10) {
            synchronized(this.buggedStates) {
                this.buggedStates.add(state);
            }

            Logger var10000 = WorldMap.LOGGER;
            Identifier var10001 = blockRegistry.getId(state.getBlock());
            var10000.info("Broken vanilla map color definition found: " + var10001);
        }

        return materialColor != null && materialColor.color != 0;
    }

    private BlockState unpackFramedBlocks(BlockState original, World world, BlockPos globalPos) {
        if (original.getBlock() instanceof AirBlock) {
            return original;
        } else {
            BlockState result = original;
            if (SupportMods.framedBlocks() && SupportMods.supportFramedBlocks.isFrameBlock(world, null, original)) {
                BlockEntity tileEntity = world.getBlockEntity(globalPos);
                if (tileEntity != null) {
                    result = SupportMods.supportFramedBlocks.unpackFramedBlock(world, null, original, tileEntity);
                    if (result == null || result.getBlock() instanceof AirBlock) {
                        result = original;
                    }
                }
            }

            return result;
        }
    }

    public void loadPixel(World world, Registry<Block> blockRegistry, MapBlock pixel, MapBlock currentPixel, WorldChunk bchunk, int insideX, int insideZ, int highY, int lowY, boolean cave, boolean fullCave, int mappedHeight, boolean canReuseBiomeColours, boolean ignoreHeightmaps, Registry<Biome> biomeRegistry, boolean flowers, int worldBottomY) {
        pixel.prepareForWriting(worldBottomY);
        this.overlayBuilder.startBuilding();
        boolean underair = !cave || fullCave;
        boolean shouldEnterGround = fullCave;
        BlockState opaqueState = null;
        byte workingLight = -1;
        boolean worldHasSkyLight = world.getDimension().hasSkyLight();
        byte workingSkyLight = (byte)(worldHasSkyLight ? 15 : 0);
        this.topH = lowY;
        this.mutableGlobalPos.set((bchunk.getPos().x << 4) + insideX, lowY - 1, (bchunk.getPos().z << 4) + insideZ);
        boolean shouldExtendTillTheBottom = false;
        int transparentSkipY = 0;

        int h;
        for(h = highY; h >= lowY; h = shouldExtendTillTheBottom ? transparentSkipY : h - 1) {
            this.mutableLocalPos.set(insideX, h, insideZ);
            BlockState state = bchunk.getBlockState(this.mutableLocalPos);
            if (state == null) {
                state = Blocks.AIR.getDefaultState();
            }

            this.mutableGlobalPos.setY(h);
            state = this.unpackFramedBlocks(state, world, this.mutableGlobalPos);
            FluidState fluidFluidState = state.getFluidState();
            shouldExtendTillTheBottom = !shouldExtendTillTheBottom && !this.overlayBuilder.isEmpty() && this.firstTransparentStateY - h >= 5;
            if (shouldExtendTillTheBottom) {
                for(transparentSkipY = h - 1; transparentSkipY >= lowY; --transparentSkipY) {
                    BlockState traceState = bchunk.getBlockState(new BlockPos(insideX, transparentSkipY, insideZ));
                    if (traceState == null) {
                        traceState = Blocks.AIR.getDefaultState();
                    }

                    FluidState traceFluidState = traceState.getFluidState();
                    if (!traceFluidState.isEmpty()) {
                        if (!this.shouldOverlayCached(traceFluidState)) {
                            break;
                        }

                        if (!(traceState.getBlock() instanceof AirBlock) && traceState.getBlock() == this.fluidToBlock.apply(traceFluidState).getBlock()) {
                            continue;
                        }
                    }

                    if (!this.shouldOverlayCached(traceState)) {
                        break;
                    }
                }
            }

            this.mutableGlobalPos.setY(h + 1);
            workingLight = (byte)world.getLightLevel(LightType.BLOCK, this.mutableGlobalPos);
            if (cave && workingLight < 15 && worldHasSkyLight) {
                if (!ignoreHeightmaps && !fullCave && highY >= mappedHeight) {
                    workingSkyLight = 15;
                } else {
                    workingSkyLight = (byte)world.getLightLevel(LightType.SKY, this.mutableGlobalPos);
                }
            }

            this.mutableGlobalPos.setY(h);
            if (!fluidFluidState.isEmpty() && (!cave || !shouldEnterGround)) {
                underair = true;
                BlockState fluidState = this.fluidToBlock.apply(fluidFluidState);
                if (this.loadPixelHelp(currentPixel, world, blockRegistry, fluidState, workingLight, workingSkyLight, h, canReuseBiomeColours, cave, fluidFluidState, biomeRegistry, transparentSkipY, shouldExtendTillTheBottom, flowers)) {
                    opaqueState = state;
                    break;
                }
            }

            Block b = state.getBlock();
            if (b instanceof AirBlock) {
                underair = true;
            } else if (underair && state.getBlock() != this.fluidToBlock.apply(fluidFluidState).getBlock()) {
                if (cave && shouldEnterGround) {
                    if (!state.isBurnable() && !state.isReplaceable() && state.getPistonBehavior() != PistonBehavior.DESTROY && !this.shouldOverlayCached(state)) {
                        underair = false;
                        shouldEnterGround = false;
                    }
                } else if (this.loadPixelHelp(currentPixel, world, blockRegistry, state, workingLight, workingSkyLight, h, canReuseBiomeColours, cave, null, biomeRegistry, transparentSkipY, shouldExtendTillTheBottom, flowers)) {
                    opaqueState = state;
                    break;
                }
            }
        }

        if (h < lowY) {
            h = lowY;
        }

        RegistryKey<Biome> blockBiome;
        BlockState state = opaqueState == null ? Blocks.AIR.getDefaultState() : opaqueState;
        this.overlayBuilder.finishBuilding(pixel);
        byte light = 0;
        if (opaqueState != null) {
            light = workingLight;
            if (cave && workingLight < 15 && pixel.getNumberOfOverlays() == 0 && workingSkyLight > workingLight) {
                light = workingSkyLight;
            }
        } else {
            h = worldBottomY;
        }

        if (canReuseBiomeColours && currentPixel != null && currentPixel.getState() == state && currentPixel.getTopHeight() == this.topH) {
            blockBiome = currentPixel.getBiome();
        } else {
            this.mutableGlobalPos.setY(this.topH);
            blockBiome = this.biomeGetter.getBiome(world, this.mutableGlobalPos, biomeRegistry);
            this.mutableGlobalPos.setY(h);
        }

        if (this.overlayBuilder.getOverlayBiome() != null) {
            blockBiome = this.overlayBuilder.getOverlayBiome();
        }

        boolean glowing = this.isGlowing(state);
        pixel.write(state, h, this.topH, blockBiome, light, glowing, cave);
    }

    private boolean loadPixelHelp(MapBlock currentPixel,
                                  World world, Registry<Block> blockRegistry,
                                  BlockState state,
                                  byte light,
                                  byte skyLight,
                                  int h,
                                  boolean canReuseBiomeColours,
                                  boolean cave,
                                  FluidState fluidFluidState,
                                  Registry<Biome> biomeRegistry,
                                  int transparentSkipY,
                                  boolean shouldExtendTillTheBottom,
                                  boolean flowers) {
        Block b = state.getBlock();
        if (this.isInvisible(state, b, flowers)) {
            return false;
        } else if (this.shouldOverlayCached(fluidFluidState == null ? state : fluidFluidState)) {
            if (h > this.topH) {
                this.topH = h;
            }

            byte overlayLight = light;
            if (this.overlayBuilder.isEmpty()) {
                this.firstTransparentStateY = h;
                if (cave && skyLight > light) {
                    overlayLight = skyLight;
                }
            }

            if (shouldExtendTillTheBottom) {
                this.overlayBuilder.getCurrentOverlay().increaseOpacity(this.overlayBuilder.getCurrentOverlay().getState().getOpacity() * (h - transparentSkipY));
            } else {
                RegistryKey<Biome> overlayBiome = this.overlayBuilder.getOverlayBiome();
                if (overlayBiome == null) {
                    if (canReuseBiomeColours && currentPixel != null && currentPixel.getNumberOfOverlays() > 0 && currentPixel.getOverlays().getFirst().getState() == state) {
                        overlayBiome = currentPixel.getBiome();
                    } else {
                        overlayBiome = this.biomeGetter.getBiome(world, this.mutableGlobalPos, biomeRegistry);
                    }
                }

                this.overlayBuilder.build(state, state.getOpacity(), overlayLight, this.mapProcessor, overlayBiome);
            }

            return false;
        } else if (!this.hasVanillaColor(state, world, blockRegistry, this.mutableGlobalPos)) {
            return false;
        } else {
            if (h > this.topH) {
                this.topH = h;
            }

            return true;
        }
    }

    protected abstract List<BakedQuad> getQuads(BlockStateModel var1, World var2, BlockPos var3, BlockState var4, Direction var5);

    protected abstract Sprite getParticleIcon(BlockModels var1, BlockStateModel var2, World var3, BlockPos var4, BlockState var5);

    public int loadBlockColourFromTexture(BlockState state, boolean convert, World world, Registry<Block> blockRegistry, BlockPos globalPos) {
        if (this.clearCachedColours) {
            this.textureColours.clear();
            this.blockColours.clear();
            this.blockTintIndices.clear();
            this.lastBlockStateForTextureColor = null;
            this.lastBlockStateForTextureColorResult = -1;
            this.clearCachedColours = false;
            if (WorldMap.settings.debug) {
                WorldMap.LOGGER.info("Xaero's World Map cache cleared!");
            }
        }

        if (state == this.lastBlockStateForTextureColor) {
            return this.lastBlockStateForTextureColorResult;
        } else {
            Integer c = this.blockColours.get(state);
            int red;
            int green;
            int blue;
            int alpha = 0;
            Block b = state.getBlock();
            if (c == null) {
                String name = null;
                int tintIndex = -1;

                try {
                    List<BakedQuad> upQuads = null;
                    BlockModels bms = MinecraftClient.getInstance().getBlockRenderManager().getModels();
                    BlockStateModel model = bms.getModel(state);
                    if (convert) {
                        upQuads = this.getQuads(model, world, globalPos, state, Direction.UP);
                    }

                    Sprite missingTexture = MinecraftClient.getInstance().getBakedModelManager().getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).getSprite(MissingSprite.getMissingSpriteId());
                    Sprite texture;
                    if (upQuads != null && !upQuads.isEmpty() && upQuads.getFirst().sprite() != missingTexture) {
                        texture = upQuads.getFirst().sprite();
                        tintIndex = upQuads.getFirst().tintIndex();
                    } else {
                        texture = this.getParticleIcon(bms, model, world, globalPos, state);
                        tintIndex = 0;
                    }

                    if (texture == null) {
                        throw new SilentException("No texture for " + state);
                    }

                    name = texture.getContents().getId() + ".png";
                    String[] args = name.split(":");
                    if (args.length < 2) {
                        DEFAULT_RESOURCE[1] = args[0];
                        args = DEFAULT_RESOURCE;
                    }

                    Integer cachedColour = this.textureColours.get(name);
                    if (cachedColour == null) {
                        Identifier location = Identifier.of(args[0], "textures/" + args[1]);
                        Resource resource = MinecraftClient.getInstance().getResourceManager().getResource(location).orElse(null);
                        if (resource == null) {
                            throw new SilentException("No texture " + location);
                        }

                        InputStream input = resource.getInputStream();
                        BufferedImage img = ImageIO.read(input);
                        red = 0;
                        green = 0;
                        blue = 0;
                        int total = 0;
                        int ts = Math.min(img.getWidth(), img.getHeight());
                        if (ts > 0) {
                            int diff = Math.max(1, Math.min(4, ts / 8));
                            int parts = ts / diff;
                            Raster raster = img.getData();
                            int[] colorHolder = null;

                            for(int i = 0; i < parts; ++i) {
                                for(int j = 0; j < parts; ++j) {
                                    int rgb;
                                    if (img.getColorModel().getNumComponents() < 3) {
                                        colorHolder = raster.getPixel(i * diff, j * diff, colorHolder);
                                        int sample = colorHolder[0] & 255;
                                        int a = 255;
                                        if (colorHolder.length > 1) {
                                            a = colorHolder[1];
                                        }

                                        rgb = a << 24 | sample << 16 | sample << 8 | sample;
                                    } else {
                                        rgb = img.getRGB(i * diff, j * diff);
                                    }

                                    int a = rgb >> 24 & 255;
                                    if (rgb != 0 && a != 0) {
                                        red += rgb >> 16 & 255;
                                        green += rgb >> 8 & 255;
                                        blue += rgb & 255;
                                        alpha += a;
                                        ++total;
                                    }
                                }
                            }
                        }

                        input.close();
                        if (total == 0) {
                            total = 1;
                        }

                        red /= total;
                        green /= total;
                        blue /= total;
                        alpha /= total;
                        if (convert && red == 0 && green == 0 && blue == 0) {
                            throw new SilentException("Black texture " + ts);
                        }

                        c = alpha << 24 | red << 16 | green << 8 | blue;
                        this.textureColours.put(name, c);
                    } else {
                        c = cachedColour;
                    }
                } catch (FileNotFoundException var36) {
                    if (convert) {
                        return this.loadBlockColourFromTexture(state, false, world, blockRegistry, globalPos);
                    }

                    Logger var48 = WorldMap.LOGGER;
                    Identifier var49 = blockRegistry.getId(b);
                    var48.info("Block file not found: " + var49);
                    c = 0;
                    if (state.getMapColor(world, globalPos) != null) {
                        c = state.getMapColor(world, globalPos).color;
                    }

                    this.textureColours.put(name, c);
                } catch (Exception e) {
                    Logger var10000 = WorldMap.LOGGER;
                    Identifier var10001 = blockRegistry.getId(b);
                    var10000.info("Exception when loading " + var10001 + " texture, using material colour.");
                    c = 0;
                    if (state.getMapColor(world, globalPos) != null) {
                        c = state.getMapColor(world, globalPos).color;
                    }

                    if (name != null) {
                        this.textureColours.put(name, c);
                    }

                    if (e instanceof SilentException) {
                        WorldMap.LOGGER.info(e.getMessage());
                    } else {
                        WorldMap.LOGGER.error("suppressed exception", e);
                    }
                }

                this.blockColours.put(state, c);
                this.blockTintIndices.put(state, tintIndex);
            }

            this.lastBlockStateForTextureColor = state;
            this.lastBlockStateForTextureColorResult = c;
            return c;
        }
    }

    public long getUpdateCounter() {
        return this.updateCounter;
    }

    public void resetPosition() {
        this.X = 0;
        this.Z = 0;
        this.insideX = 0;
        this.insideZ = 0;
    }

    public void requestCachedColoursClear() {
        this.clearCachedColours = true;
    }

    public void setMapProcessor(MapProcessor mapProcessor) {
        this.mapProcessor = mapProcessor;
    }

    public void setDirtyInWriteDistance(PlayerEntity player, World level) {
        int writeDistance = this.getWriteDistance();
        int playerChunkX = player.getBlockPos().getX() >> 4;
        int playerChunkZ = player.getBlockPos().getZ() >> 4;
        int startChunkX = playerChunkX - writeDistance;
        int startChunkZ = playerChunkZ - writeDistance;
        int endChunkX = playerChunkX + writeDistance;
        int endChunkZ = playerChunkZ + writeDistance;

        for(int x = startChunkX; x < endChunkX; ++x) {
            for(int z = startChunkZ; z < endChunkZ; ++z) {
                WorldChunk chunk = level.getChunk(x, z);
                if (chunk != null) {
                    try {
                        XaeroWorldMapCore.chunkCleanField.set(chunk, false);
                    } catch (IllegalAccessException | IllegalArgumentException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

    }

    public int getBlockTintIndex(BlockState state) {
        return this.blockTintIndices.getInt(state);
    }

    public boolean writeWorldChunkDirect(WorldChunk chunk) {
        if (chunk == null) return true;

        // MapWriter / MapProcessor world context (same world the writer works with)
        World world = this.mapProcessor.getWorld();
        Registry<Block> blockRegistry = this.mapProcessor.getWorldBlockRegistry();
        Registry<Biome> biomeRegistry = this.mapProcessor.worldBiomeRegistry;
        boolean ignoreHeightmaps = this.mapProcessor.getMapWorld().isIgnoreHeightmaps();
        boolean flowers = WorldMap.settings.flowers;
        int caveDepth = WorldMap.settings.caveModeDepth;

        // chunk coordinates
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

        MapRegion region = this.mapProcessor.getLeafMapRegion(this.writingLayer, regionX, regionZ, true);
        boolean wasSkipped;

        synchronized (region.writerThreadPauseSync) {
            if (region.isWritingPaused()) {
                // cannot write now
                return true;
            }

            boolean isProperLoadState;
            boolean regionIsResting;
            boolean createdTileChunk = false;
            MapTileChunk tileChunk = null;

            // same synchronized(region) block pattern as writeChunk
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
                    }
                }
            }

            if (!regionIsResting || !isProperLoadState) {
                // same conservative behavior as writeChunk
                return true;
            }

            // compute the indices of this chunk inside its tileChunk (0..TILE_SIZE-1)
            int subTileX = Math.floorMod(chunkX, TILE_SIZE); // 0..3
            int subTileZ = Math.floorMod(chunkZ, TILE_SIZE); // 0..3

            // find or create MapTile for this chunk inside tileChunk
            MapTile mapTile = tileChunk.getTile(subTileX, subTileZ);
            if (mapTile == null) {
                mapTile = this.mapProcessor.getTilePool().get(this.mapProcessor.getCurrentDimension(), chunkX, chunkZ);
                tileChunk.setChanged(true);
            }

            // Height bounds for this chunk
            int worldBottomY = chunk.getBottomY();
            int worldTopY = chunk.getTopYInclusive() + 1;
            boolean cave = this.caveStart != Integer.MAX_VALUE;
            boolean fullCave = this.caveStart == Integer.MIN_VALUE;
            int lowH = worldBottomY;
            if (cave && !fullCave) {
                lowH = this.caveStart + 1 - caveDepth;
                if (lowH < worldBottomY) lowH = worldBottomY;
            }

            // We'll iterate block-columns (0..15) as writeChunk does and call loadPixel for each (px,pz)
            for (int px = 0; px < CHUNK_SIZE; ++px) {
                for (int pz = 0; pz < CHUNK_SIZE; ++pz) {
                    // determine startHeight using same logic as writeChunk
                    int mappedHeight = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, px, pz);
                    int startHeight;
                    if (cave && !fullCave) {
                        startHeight = this.caveStart;
                    } else if (!ignoreHeightmaps && mappedHeight >= worldBottomY) {
                        startHeight = mappedHeight;
                    } else {
                        startHeight = this.getSectionBasedHeight(chunk, 64);
                    }
                    if (startHeight >= worldTopY) startHeight = worldTopY - 1;

                    // existing pixel if loaded
                    MapBlock currentPixel = mapTile.isLoaded() ? mapTile.getBlock(px, pz) : null;

                    // call the existing loadPixel helper (chunk is used as WorldChunk param)
                    this.loadPixel(world, blockRegistry, this.loadingObject, currentPixel,
                            chunk, px, pz, startHeight, lowH, cave, fullCave, mappedHeight,
                            mapTile.wasWrittenOnce(), ignoreHeightmaps, biomeRegistry, flowers, worldBottomY);

                    // mirror the fixHeightType + equality/commit logic used in writeChunk
                    this.loadingObject.fixHeightType(px, pz, mapTile, tileChunk, null, null, null,
                            this.loadingObject.getEffectiveHeight(this.blockStateShortShapeCache), true, this.blockStateShortShapeCache);

                    boolean equalsSlopesExcluded = this.loadingObject.equalsSlopesExcluded(currentPixel);
                    boolean fullyEqual = this.loadingObject.equals(currentPixel, equalsSlopesExcluded);
                    if (!fullyEqual) {
                        MapBlock loadedBlock = this.loadingObject;
                        mapTile.setBlock(px, pz, loadedBlock);
                        this.loadingObject = Objects.requireNonNullElseGet(currentPixel, MapBlock::new);
//                        tileChunk.setChanged(true);
                        // for simplicity I don't attempt the neighbor-edge slope updates here;
                        // that's a best-effort omission but tileChunk is marked as changed.
                    }
                }
            }

            // finalize tile and tileChunk state (mirrors writeChunk)
            mapTile.setWorldInterpretationVersion(1);
            mapTile.setWrittenCave(this.caveStart, caveDepth);
            tileChunk.setTile(subTileX, subTileZ, mapTile, this.blockStateShortShapeCache);
            mapTile.setWrittenOnce(true);
            mapTile.setLoaded(true);

            Misc.setReflectFieldValue(chunk, XaeroWorldMapCore.chunkCleanField, true);

            return false;
        }
    }
}
