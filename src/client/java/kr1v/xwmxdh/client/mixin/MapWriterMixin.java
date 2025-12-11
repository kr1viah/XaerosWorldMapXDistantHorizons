package kr1v.xwmxdh.client.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import com.seibel.distanthorizons.core.config.Config;
import kr1v.xwmxdh.Xwmxdh;
import kr1v.xwmxdh.client.XwmxdhClient;
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
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.biome.BiomeColorCalculator;
import xaero.map.biome.BiomeGetter;
import xaero.map.biome.BlockTintProvider;
import xaero.map.region.OverlayManager;

import java.util.Objects;

@Mixin(MapWriter.class)
public abstract class MapWriterMixin {
    @Shadow
    private int loadDistance;

    @Shadow
    private MapProcessor mapProcessor;

    @WrapMethod(method = "onRender")
    private void onRender(BiomeColorCalculator biomeColorCalculator, OverlayManager overlayManager, Operation<Void> original) {
        if (this.mapProcessor.getCurrentWorldId() != null) {
            Xwmxdh.chunkManager.swap(this.mapProcessor.getCurrentWorldId());
        }

        original.call(biomeColorCalculator, overlayManager);
    }

    @WrapMethod(method = "writeMap")
    private boolean writeMap(World world, Registry<Block> blockRegistry, double playerX, double playerY, double playerZ, Registry<Biome> biomeRegistry, BiomeColorCalculator biomeColorCalculator, OverlayManager overlayManager, boolean loadChunks, boolean updateChunks, boolean ignoreHeightmaps, boolean flowers, boolean detailedDebug, BlockPos.Mutable mutableBlockPos3, BlockTintProvider blockTintProvider, int caveDepth, Operation<Boolean> original) {
        this.loadDistance = Config.Client.Advanced.Graphics.Quality.lodChunkRenderDistanceRadius.get();


        long start = System.nanoTime();
        var toRet = original.call(world, blockRegistry, playerX, playerY, playerZ, biomeRegistry, biomeColorCalculator, overlayManager, loadChunks, updateChunks, ignoreHeightmaps, flowers, detailedDebug, mutableBlockPos3, blockTintProvider, caveDepth);
        if (System.nanoTime() - start >= 1_000_000 * 100) {
            XwmxdhClient.printTimeSince(start);
        }
        return toRet;
    }

    @WrapMethod(method = "getSectionBasedHeight")
    private int getSectionBasedHeight(WorldChunk bchunk, int startY, Operation<Integer> original) {
        return bchunk.getHeight();
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
            WorldChunk chunk2 = Xwmxdh.chunkManager.get(chunkX, chunkZ);
            original2 = Objects.requireNonNullElseGet(chunk2, () -> new EmptyChunk(instance, new ChunkPos(chunkX, chunkZ), null));
        }
        return original2;
    }

    @WrapOperation(method = "writeChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getChunk(II)Lnet/minecraft/world/chunk/WorldChunk;"))
    private WorldChunk wrap(World instance, int i, int j, Operation<WorldChunk> original) {
        WorldChunk original2 = original.call(instance, i, j);
        if (original2 == null || original2 instanceof EmptyChunk) {
            WorldChunk chunk2 = Xwmxdh.chunkManager.get(i, j);
            return Objects.requireNonNullElseGet(chunk2, () -> new EmptyChunk(instance, new ChunkPos(i, j), null));
        }
        return original2;
    }

    @SuppressWarnings("unchecked")
    @WrapOperation(method = {"loadPixel", "loadPixelHelp"}, at = @At(value = "INVOKE", target = "Lxaero/map/biome/BiomeGetter;getBiome(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/registry/Registry;)Lnet/minecraft/registry/RegistryKey;"))
    private RegistryKey<Biome> getBiome(BiomeGetter instance, World world, BlockPos pos, Registry<Biome> biomeRegistry, Operation<RegistryKey<Biome>> original) {
        RegistryKey<Biome> original2 = original.call(instance, world, pos, biomeRegistry);
        if (original2.equals(UNKNOWN) || original2.equals(PLAINS)) {
            DhApiTerrainDataPoint data = Xwmxdh.chunkManager.getAt(DhApi.Delayed.worldProxy.getSinglePlayerLevel(), pos);
            RegistryEntry<Biome> origina = data == null ? null : (RegistryEntry<Biome>) data.biomeWrapper.getWrappedMcObject();
            original2 = origina == null || origina.getKey().isEmpty() ? UNKNOWN : origina.getKey().get();
        }

        return original2;
    }

    @WrapMethod(method = "getWriteDistance")
    private int getWriteDistance(Operation<Integer> original) {
        return Config.Client.Advanced.Graphics.Quality.lodChunkRenderDistanceRadius.get();
    }

    @Unique
    private static final RegistryKey<Biome> UNKNOWN = RegistryKey.of(RegistryKeys.BIOME, Identifier.of("xaeroworldmap:unknown_biome"));
    @Unique
    private static final RegistryKey<Biome> PLAINS = BiomeKeys.PLAINS;
}
