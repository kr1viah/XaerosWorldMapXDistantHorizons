package kr1v.xwmxdh.client.mixin.multithread;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;

@Mixin(GuiMap.class)
public class GuiMapMixin {
    @WrapOperation(method = "render", at = @At(value = "FIELD", target = "Lxaero/map/MapProcessor;renderThreadPauseSync:Ljava/lang/Object;"))
    private Object sync(MapProcessor instance, Operation<Object> original) {
        return new Object();
    }
}
