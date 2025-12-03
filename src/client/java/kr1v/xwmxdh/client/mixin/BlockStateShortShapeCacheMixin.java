package kr1v.xwmxdh.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.map.cache.BlockStateShortShapeCache;

@Mixin(BlockStateShortShapeCache.class)
public class BlockStateShortShapeCacheMixin {
    @WrapOperation(method = "isShort", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;isOnThread()Z"))
    private boolean yes(MinecraftClient instance, Operation<Boolean> original) {
        return true;
    }
}
