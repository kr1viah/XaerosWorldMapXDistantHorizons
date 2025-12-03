package kr1v.xwmxdh.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import xaero.map.MapProcessor;

@Mixin(MapProcessor.class)
public class MapProcessorMixin {
    @WrapMethod(method = "onRenderProcess")
    private void tryRenderProcess(MinecraftClient mc, Operation<Void> original) {
        try {
            original.call(mc);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
