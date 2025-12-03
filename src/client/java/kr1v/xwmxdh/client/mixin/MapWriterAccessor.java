package kr1v.xwmxdh.client.mixin;

import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.MapWriter;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.region.MapBlock;
import xaero.map.region.MapTileChunk;

@Mixin(MapWriter.class)
public interface MapWriterAccessor {
    @Accessor int getCaveStart();
    @Accessor int getWritingLayer();
    @Accessor MapBlock getLoadingObject();
    @Accessor void setLoadingObject(MapBlock loadingObject);
    @Accessor BlockPos.Mutable getMutableBlockPos3();
    @Accessor BlockStateShortShapeCache getBlockStateShortShapeCache();
    @Accessor MapTileChunk getRightChunk();
    @Accessor void setRightChunk(MapTileChunk mapTileChunk);
    @Accessor MapTileChunk getBottomRightChunk();
    @Accessor void setBottomRightChunk(MapTileChunk mapTileChunk);
}
