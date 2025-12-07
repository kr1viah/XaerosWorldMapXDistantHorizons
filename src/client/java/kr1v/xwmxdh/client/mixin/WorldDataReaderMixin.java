package kr1v.xwmxdh.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.file.worldsave.WorldDataReader;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTile;
import xaero.map.region.MapTileChunk;

@Mixin(WorldDataReader.class)
public class WorldDataReaderMixin {
//    @WrapMethod(method = "buildRegion")
//    private boolean mreowww(MapRegion region, ServerWorld serverWorld, RegistryWrapper<Block> blockLookup, Registry<Block> blockRegistry, Registry<Fluid> fluidRegistry, boolean loading, int[] chunkCountDest, Executor renderExecutor, Operation<Boolean> original) {
//        return false;
//    }

//    @WrapMethod(method = "buildTile")
//    private boolean preventBuildTileIfEmptyChunk(
//            NbtCompound nbttagcompound,
//            MapTile tile,
//            MapTileChunk tileChunk,
//            int chunkX,
//            int chunkZ,
//            int insideRegionX,
//            int insideRegionZ,
//            int caveStart,
//            int caveDepth,
//            boolean worldHasSkylight,
//            boolean ignoreHeightmaps,
//            ServerWorld serverWorld,
//            RegistryWrapper<Block> blockLookup,
//            Registry<Block> blockRegistry,
//            Registry<Fluid> fluidRegistry,
//            Registry<Biome> biomeRegistry,
//            boolean flowers,
//            int worldBottomY,
//            int worldTopY,
//            Operation<Boolean> original,
//            @Share("didSomething")LocalBooleanRef booleanRef) {
//
////        Chunk chunk = serverWorld.getChunk(chunkX, chunkZ);
////        if (chunk == null || chunk instanceof EmptyChunk) {
////            booleanRef.set(false);
////            return false;
////        }
////        booleanRef.set(true);
//
//        return original.call(nbttagcompound, tile, tileChunk, chunkX, chunkZ, insideRegionX, insideRegionZ, caveStart, caveDepth, worldHasSkylight, ignoreHeightmaps, serverWorld, blockLookup, blockRegistry, fluidRegistry, biomeRegistry, flowers, worldBottomY, worldTopY);
//    }

//    @Definition(id = "tile", local = @Local(type = MapTile.class, name = "tile"))
//    @Expression("tile != null")
//    @ModifyExpressionValue(method = "buildTileChunk", at = @At("MIXINEXTRAS:EXPRESSION"))
//    private boolean mrep(boolean original) {
//        return false;
//    }

//    @WrapMethod(method = "buildTileChunk")
//    private void no(MapTileChunk tileChunk, int caveStart, int caveDepth, boolean worldHasSkylight, boolean ignoreHeightmaps, MapRegion prevRegion, ServerWorld serverWorld, RegistryWrapper<Block> blockLookup, Registry<Block> blockRegistry, Registry<Fluid> fluidRegistry, Registry<Biome> biomeRegistry, boolean flowers, int worldBottomY, int worldTopY, Operation<Void> original) {
//    }
//
//    @WrapOperation(method = "buildTileChunk", at = @At(value = "INVOKE", target = "Lxaero/map/region/MapTileChunk;setTile(IILxaero/map/region/MapTile;Lxaero/map/cache/BlockStateShortShapeCache;)V", ordinal = 2))
//    private void preventTileWrite(MapTileChunk instance, int destZ, int mapBlock, MapTile subtractOneFromHeight, BlockStateShortShapeCache j, Operation<Void> original, @Share("didSomething")LocalBooleanRef booleanRef) {
////        if (booleanRef.get()) {
//            original.call(instance, destZ, mapBlock, subtractOneFromHeight, j);
////        }
//    }
}
