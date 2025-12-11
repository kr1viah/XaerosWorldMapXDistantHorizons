package kr1v.xwmxdh;

import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.event.listener.GameEventDispatcher;
import net.minecraft.world.tick.BasicTickScheduler;
import net.minecraft.world.tick.ChunkTickScheduler;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class WorldChunkWrapper extends WorldChunk {
    private final DhApiTerrainDataPoint[][][] data;

    public WorldChunkWrapper(int chunkX, int chunkZ, IDhApiLevelWrapper level, DhApiTerrainDataPoint[][][] data) {
        super((World) level.getWrappedMcObject(), new ChunkPos(chunkX, chunkZ), UpgradeData.NO_UPGRADE_DATA, new ChunkTickScheduler<>(), new ChunkTickScheduler<>(), 0L, null, null, null);
        this.data = data;
    }

    // bottomYBlockPos is the actual block position (aka inclusive)
    // topYBlockPos is the block above the actual block position (aka exclusive)

    private DhApiTerrainDataPoint getDataPointAt(int x, int y, int z) {
        int relX = Math.floorMod(x, 16);
        int relZ = Math.floorMod(z, 16);

        DhApiTerrainDataPoint[] column = data[relX][relZ];

        for (var pos : column) {
            if (pos.bottomYBlockPos <= y && y < pos.topYBlockPos) {
                return pos;
            }
        }
        throw new NoSuchElementException();
    }

    private BlockState blockStateAt(int x, int y, int z) {
        return (BlockState) getDataPointAt(x, y, z).blockStateWrapper.getWrappedMcObject();
    }

    private BlockState blockStateAt(BlockPos pos) {
        return blockStateAt(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return blockStateAt(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return blockStateAt(pos).getFluidState();
    }

    @Override
    public FluidState getFluidState(int x, int y, int z) {
        return blockStateAt(x, y, z).getFluidState();
    }

    @SuppressWarnings("unchecked")
    @Override
    public RegistryEntry<Biome> getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
        return (RegistryEntry<Biome>) getDataPointAt(biomeX, biomeY, biomeZ).biomeWrapper.getWrappedMcObject();
    }




    @Override
    public ChunkLevelType getLevelType() {
        return ChunkLevelType.FULL;
    }

    @Override
    public World getWorld() {
        return super.getWorld();
    }




    @Override
    public void setUnsavedListener(UnsavedListener unsavedListener) {
        throw new NotImplementedException();
//        super.setUnsavedListener(unsavedListener);
    }

    @Override
    public void markNeedsSaving() {
        throw new NotImplementedException();
//        super.markNeedsSaving();
    }

    @Override
    public BasicTickScheduler<Block> getBlockTickScheduler() {
        throw new NotImplementedException();
//        return super.getBlockTickScheduler();
    }

    @Override
    public BasicTickScheduler<Fluid> getFluidTickScheduler() {
        throw new NotImplementedException();
//        return super.getFluidTickScheduler();
    }

    @Override
    public TickSchedulers getTickSchedulers(long time) {
        throw new NotImplementedException();
//        return super.getTickSchedulers(time);
    }

    @Override
    public GameEventDispatcher getGameEventDispatcher(int ySectionCoord) {
        throw new NotImplementedException();
//        return super.getGameEventDispatcher(ySectionCoord);
    }

    @Override
    public @Nullable BlockState setBlockState(BlockPos pos, BlockState state, int flags) {
        throw new NotImplementedException();
//        return super.setBlockState(pos, state, flags);
    }

    @Override
    public void addEntity(Entity entity) {
        throw new NotImplementedException();
//        super.addEntity(entity);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        throw new NotImplementedException();
//        return super.getBlockEntity(pos);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos, CreationType creationType) {
        throw new NotImplementedException();
//        return super.getBlockEntity(pos, creationType);
    }

    @Override
    public void addBlockEntity(BlockEntity blockEntity) {
        throw new NotImplementedException();
//        super.addBlockEntity(blockEntity);
    }

    @Override
    public void setBlockEntity(BlockEntity blockEntity) {
        throw new NotImplementedException();
//        super.setBlockEntity(blockEntity);
    }

    @Override
    public @Nullable NbtCompound getPackedBlockEntityNbt(BlockPos pos, RegistryWrapper.WrapperLookup registries) {
        throw new NotImplementedException();
//        return super.getPackedBlockEntityNbt(pos, registries);
    }

    @Override
    public void removeBlockEntity(BlockPos pos) {
        throw new NotImplementedException();
//        super.removeBlockEntity(pos);
    }

    @Override
    public void loadEntities() {
        throw new NotImplementedException();
//        super.loadEntities();
    }

    @Override
    public boolean isEmpty() {
        throw new NotImplementedException();
//        return super.isEmpty();
    }

    @Override
    public void loadFromPacket(PacketByteBuf buf, Map<Heightmap.Type, long[]> heightmaps, Consumer<ChunkData.BlockEntityVisitor> blockEntityVisitorConsumer) {
        throw new NotImplementedException();
//        super.loadFromPacket(buf, heightmaps, blockEntityVisitorConsumer);
    }

    @Override
    public void loadBiomeFromPacket(PacketByteBuf buf) {
        throw new NotImplementedException();
//        super.loadBiomeFromPacket(buf);
    }

    @Override
    public void setLoadedToWorld(boolean loadedToWorld) {
        throw new NotImplementedException();
//        super.setLoadedToWorld(loadedToWorld);
    }

    @Override
    public Map<BlockPos, BlockEntity> getBlockEntities() {
        throw new NotImplementedException();
//        return super.getBlockEntities();
    }

    @Override
    public void runPostProcessing(ServerWorld world) {
        throw new NotImplementedException();
//        super.runPostProcessing(world);
    }

    @Override
    public void disableTickSchedulers(long time) {
        throw new NotImplementedException();
//        super.disableTickSchedulers(time);
    }

    @Override
    public void addChunkTickSchedulers(ServerWorld world) {
        throw new NotImplementedException();
//        super.addChunkTickSchedulers(world);
    }

    @Override
    public void removeChunkTickSchedulers(ServerWorld world) {
        throw new NotImplementedException();
//        super.removeChunkTickSchedulers(world);
    }

    @Override
    public ChunkStatus getStatus() {
        return ChunkStatus.FULL;
//        return super.getStatus();
    }

    @Override
    public void setLevelTypeProvider(Supplier<ChunkLevelType> levelTypeProvider) {
        throw new NotImplementedException();
//        super.setLevelTypeProvider(levelTypeProvider);
    }

    @Override
    public void clear() {
        throw new NotImplementedException();
//        super.clear();
    }

    @Override
    public void updateAllBlockEntities() {
        throw new NotImplementedException();
//        super.updateAllBlockEntities();
    }
}
