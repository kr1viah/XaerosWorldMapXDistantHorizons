package kr1v.xwmxdh.client.mixin.multithread;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.map.region.MapTileChunk;

@Mixin(MapTileChunk.class)
public class MapTileChunkMixin {
    @Definition(id = "isOnThread", method = "Lnet/minecraft/client/MinecraftClient;isOnThread()Z")
    @Expression("?.isOnThread()")
    @WrapOperation(method = "updateBuffers", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean isOnThread(MinecraftClient instance, Operation<Boolean> original) {
        return true;
    }
}
