package kr1v.xwmxdh.client.mixin.multithread;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.map.region.MapRegion;
import xaero.map.region.texture.LeafRegionTexture;

@Mixin(LeafRegionTexture.class)
public class LeafRegionMixin {
    @WrapOperation(method = "preUpload", at = @At(value = "FIELD", target = "Lxaero/map/region/MapRegion;writerThreadPauseSync:Ljava/lang/Object;"))
    private Object sync(MapRegion instance, Operation<Object> original) {
        return new Object();
    }
}
