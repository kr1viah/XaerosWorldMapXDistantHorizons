package kr1v.xwmxdh.client.mixin.multithread;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.map.MapProcessor;

@Mixin(MapProcessor.class)
public class MapProcessorMixin {
    // sigh...
    @WrapOperation(method = "onRenderProcess", at = @At(value = "FIELD", target = "Lxaero/map/MapProcessor;renderThreadPauseSync:Ljava/lang/Object;"))
    private Object change(MapProcessor instance, Operation<Object> original) {
        return new Object();
    }

    @WrapOperation(method = "pushWriterPause", at = @At(value = "FIELD", target = "Lxaero/map/MapProcessor;renderThreadPauseSync:Ljava/lang/Object;"))
    private Object change2(MapProcessor instance, Operation<Object> original) {
        return new Object();
    }

    @WrapOperation(method = "popWriterPause", at = @At(value = "FIELD", target = "Lxaero/map/MapProcessor;renderThreadPauseSync:Ljava/lang/Object;"))
    private Object change3(MapProcessor instance, Operation<Object> original) {
        return new Object();
    }


    @Definition(id = "isOnThread", method = "Lnet/minecraft/client/MinecraftClient;isOnThread()Z")
    @Expression("?.isOnThread()")
    @WrapOperation(method = "getLeafMapRegion", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
    private boolean isOnThread(MinecraftClient instance, Operation<Boolean> original) {
        return true;
    }
}
