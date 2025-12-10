package kr1v.xwmxdh.client.mixin.multithread;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;

@Mixin(WorldMapSession.class)
public class WorldMapSessionMixin {
    @Definition(id = "isFinished", method = "Lxaero/map/MapProcessor;isFinished()Z")
    @Expression("this.?.isFinished()")
    @WrapOperation(method = "cleanup", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean isFinished(MapProcessor instance, Operation<Boolean> original) {
        return true;
    }
}
