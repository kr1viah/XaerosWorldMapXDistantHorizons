package kr1v.xwmxdh.client.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.seibel.distanthorizons.core.config.Config;
import kr1v.xwmxdh.WorldChunkWrapper;
import kr1v.xwmxdh.Xwmxdh;
import net.minecraft.block.Block;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.biome.BiomeColorCalculator;
import xaero.map.biome.BiomeGetter;
import xaero.map.biome.BlockTintProvider;
import xaero.map.region.OverlayManager;

@Mixin(MapWriter.class)
public abstract class MapWriterMixin {
    // this does not work
    @WrapMethod(method = "writeChunk")
    private boolean preventChunkWrite(World world, Registry<Block> blockRegistry, int distance, boolean onlyLoad,
                                      Registry<Biome> biomeRegistry, OverlayManager overlayManager, boolean loadChunks,
                                      boolean updateChunks, boolean ignoreHeightmaps, boolean flowers, boolean detailedDebug,
                                      BlockPos.Mutable mutableBlockPos3, BlockTintProvider blockTintProvider, int caveDepth,
                                      int caveStart, int layerToWrite, int tileChunkX, int tileChunkZ, int tileChunkLocalX,
                                      int tileChunkLocalZ, int chunkX, int chunkZ, Operation<Boolean> original) {

        Chunk chunk = world.getChunk(chunkX, chunkZ);
        if (chunk == null || chunk instanceof EmptyChunk) {
            chunk = Xwmxdh.get(chunkX, chunkZ);
            if (chunk == null) {
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

                return false;
            }
        }
        return original.call(world, blockRegistry, distance, onlyLoad, biomeRegistry,
                overlayManager, loadChunks, updateChunks, ignoreHeightmaps, flowers,
                detailedDebug, mutableBlockPos3, blockTintProvider, caveDepth, caveStart,
                layerToWrite, tileChunkX, tileChunkZ, tileChunkLocalX, tileChunkLocalZ,
                chunkX, chunkZ);
    }

    @Shadow
    private int loadDistance;

    @Shadow
    private MapProcessor mapProcessor;

    @Shadow
    private int insideZ;

    @Shadow
    private int insideX;

    @Shadow
    private int Z;

    @Shadow
    private int X;

    @Shadow
    private int endTileChunkX;

    @Shadow
    private int startTileChunkZ;

    @Shadow
    private int startTileChunkX;

    @Shadow
    private int endTileChunkZ;

    @Shadow
    private long updateCounter;

    @Inject(method = "onRender", at = @At("HEAD"))
    private void sync(BiomeColorCalculator biomeColorCalculator, OverlayManager overlayManager, CallbackInfo ci) {
        synchronized (this.mapProcessor.renderThreadPauseSync) {
            if (this.mapProcessor.getCurrentWorldId() != null)
                Xwmxdh.switchh(this.mapProcessor.getCurrentWorldId());
        }
    }

    @WrapMethod(method = "writeMap")
    private boolean writeMap(World world, Registry<Block> blockRegistry, double playerX, double playerY, double playerZ, Registry<Biome> biomeRegistry, BiomeColorCalculator biomeColorCalculator, OverlayManager overlayManager, boolean loadChunks, boolean updateChunks, boolean ignoreHeightmaps, boolean flowers, boolean detailedDebug, BlockPos.Mutable mutableBlockPos3, BlockTintProvider blockTintProvider, int caveDepth, Operation<Boolean> original) {
        this.loadDistance = Config.Client.Advanced.Graphics.Quality.lodChunkRenderDistanceRadius.get();
        return original.call(world, blockRegistry, playerX, playerY, playerZ, biomeRegistry, biomeColorCalculator, overlayManager, loadChunks, updateChunks, ignoreHeightmaps, flowers, detailedDebug, mutableBlockPos3, blockTintProvider, caveDepth);
    }

    @WrapOperation(method = "writeMap", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    private int min(int a, int b, Operation<Integer> original) {
        return b;
    }

    @Definition(id = "playerChunkX", field = "Lxaero/map/MapWriter;playerChunkX:I")
    @Expression("? >= this.playerChunkX - ?")
    @ModifyExpressionValue(method = "writeChunk", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean injected(boolean original) {
        return true;
    }

    @Definition(id = "playerChunkX", field = "Lxaero/map/MapWriter;playerChunkX:I")
    @Expression("? <= this.playerChunkX + ?")
    @ModifyExpressionValue(method = "writeChunk", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean injected2(boolean original) {
        return true;
    }

    @Definition(id = "playerChunkZ", field = "Lxaero/map/MapWriter;playerChunkZ:I")
    @Expression("? <= this.playerChunkZ + ?")
    @ModifyExpressionValue(method = "writeChunk", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean injected3(boolean original) {
        return true;
    }

    @Definition(id = "playerChunkZ", field = "Lxaero/map/MapWriter;playerChunkZ:I")
    @Expression("? >= this.playerChunkZ - ?")
    @ModifyExpressionValue(method = "writeChunk", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean injected4(boolean original) {
        return true;
    }

    @WrapOperation(method = "writeChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;"))
    private Chunk wrap(World instance, int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create, Operation<Chunk> original) {
        Chunk original2 = original.call(instance, chunkX, chunkZ, leastStatus, create);
        if (original2 == null) {
            Chunk chunk2 = Xwmxdh.get(chunkX, chunkZ);
            if (chunk2 != null)
                original2 = new WorldChunkWrapper(instance, chunk2);
            else
                original2 = new EmptyChunk(instance, new ChunkPos(chunkX, chunkZ), null);
        }
        return original2;
    }

    @WrapOperation(method = "writeChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getChunk(II)Lnet/minecraft/world/chunk/WorldChunk;"))
    private WorldChunk wrap(World instance, int i, int j, Operation<WorldChunk> original) {
        WorldChunk original2 = original.call(instance, i, j);
        if (original2 == null || original2 instanceof EmptyChunk) {
            Chunk chunk2 = Xwmxdh.get(i, j);
            if (chunk2 != null)
                return new WorldChunkWrapper(instance, chunk2);
            else
                original2 = new EmptyChunk(instance, new ChunkPos(i, j), null);
        } else {
            Xwmxdh.put(i, j, original2);
        }
        return original2;
    }

    @WrapOperation(method = "loadPixel", at = @At(value = "INVOKE", target = "Lxaero/map/biome/BiomeGetter;getBiome(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/registry/Registry;)Lnet/minecraft/registry/RegistryKey;"))
    private RegistryKey<Biome> getBiome(BiomeGetter instance, World world, BlockPos pos, Registry<Biome> biomeRegistry, Operation<RegistryKey<Biome>> original, @Local(argsOnly = true) WorldChunk worldChunk) {
        RegistryKey<Biome> original2 = original.call(instance, world, pos, biomeRegistry);
        if (original2.equals(UNKNOWN)) {
            RegistryEntry<Biome> biomeHolder = worldChunk.getBiomeForNoiseGen(pos.getX(), pos.getY(), pos.getZ());
            original2 = biomeHolder == null ? null : biomeHolder.getKey().orElse(UNKNOWN);
        }

        return original2;
    }

    @WrapMethod(method = "getWriteDistance")
    private int getWriteDistance(Operation<Integer> original) {
        return Config.Client.Advanced.Graphics.Quality.lodChunkRenderDistanceRadius.get();
    }

    @Unique
    private static final RegistryKey<Biome> UNKNOWN = RegistryKey.of(RegistryKeys.BIOME, Identifier.of("xaeroworldmap:unknown_biome"));
}
