package kr1v.xwmxdh.client.mixin.multithread;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.region.LeveledRegion;
import xaero.map.region.texture.RegionTexture;

import java.io.DataOutputStream;
import java.io.File;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

@Mixin(LeveledRegion.class)
public abstract class LeveledRegionMixin<T extends RegionTexture<T>> {
    @Shadow
    public abstract T getTexture(int var1, int var2);
    @Inject(method = "writeCacheMetaData", at = @At("HEAD"))
    private void checkReady(DataOutputStream output, byte[] usableBuffer, byte[] integerByteBuffer, CallbackInfo ci) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                T texture = this.getTexture(i, j);
                if (texture != null && texture.shouldIncludeInCache()) {
                    waitUntilTrue(texture::isCachePrepared);
//                    if (!texture.isCachePrepared()) {
//                        texture.setCachePrepared(true);
//                        ci.cancel();
//                        output.write(255);
//                        return;
//                    }
                }
            }
        }
    }

    @Inject(method = "saveCacheTextures", at = @At("HEAD"))
    private void checkReady(File tempFile, int extraAttempts, CallbackInfoReturnable<Boolean> cir) {
//        synchronized ()
        for (int i = 0; i < 8; i++) {
             for (int j = 0; j < 8; j++) {
                T texture = this.getTexture(i, j);
                if (texture != null && texture.shouldIncludeInCache()) {
                    waitUntilTrue(texture::isCachePrepared);
//                    if (!texture.isCachePrepared()) {
//                        cir.setReturnValue(false);
//                        return;
//                    }
                }
            }
        }
    }

    @Unique
    private static void waitUntilTrue(BooleanSupplier condition) {
        while (!condition.getAsBoolean()) {
            LockSupport.parkNanos(1_000_000);
        }
    }
}
