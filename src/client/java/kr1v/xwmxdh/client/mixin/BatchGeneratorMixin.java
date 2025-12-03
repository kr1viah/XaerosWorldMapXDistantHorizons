package kr1v.xwmxdh.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.core.generation.BatchGenerator;
import kr1v.xwmxdh.client.XwmxdhClient;
import loaderCommon.fabric.com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

@Mixin(BatchGenerator.class)
public class BatchGeneratorMixin {
    @WrapMethod(method = "generateChunks")
    private CompletableFuture<Void> wrap(int chunkPosMinX, int chunkPosMinZ, int generationRequestChunkWidthCount, byte targetDataDetail, EDhApiDistantGeneratorMode generatorMode, ExecutorService worldGeneratorThreadPool, Consumer<Object[]> resultConsumer, Operation<CompletableFuture<Void>> original) {

        Consumer<Object[]> consumerWrapper = (chunkWrapper) -> {
            ChunkWrapper chunk1 = (ChunkWrapper) chunkWrapper[0];

            Chunk chunk = chunk1.getChunk();

//            XwmxdhClient.submitForWrite(chunk);
            XwmxdhClient.writeChunkDirect(chunk);

            resultConsumer.accept(chunkWrapper);
        };

        return original.call(chunkPosMinX, chunkPosMinZ, generationRequestChunkWidthCount, targetDataDetail, generatorMode, worldGeneratorThreadPool, consumerWrapper);
    }
}
