package kr1v.xwmxdh;

import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
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
import net.minecraft.structure.StructureStart;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.light.ChunkSkyLight;
import net.minecraft.world.event.listener.GameEventDispatcher;
import net.minecraft.world.gen.chunk.BlendingData;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.tick.BasicTickScheduler;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Stream;

@SuppressWarnings({"EqualsDoesntCheckParameterClass", "deprecation"})
public class WorldChunkWrapper extends WorldChunk {
    public Chunk wrappedChunk;
    public static ThreadLocal<Chunk> wrappedChunkLocal = new ThreadLocal<>();

    public WorldChunkWrapper(World world, Chunk toWrap) {
        super(world, setLocalChunk(toWrap).getPos());
        this.wrappedChunk = toWrap;
        wrappedChunkLocal.remove();
    }

    private static Chunk setLocalChunk(Chunk toWrap) {
        wrappedChunkLocal.set(toWrap);
        return toWrap;
    }


    @Override
    public void updateAllBlockEntities() {
        throw new NotImplementedException();
    }

    @Override
    public void clear() {
        throw new NotImplementedException();
    }

    @Override
    public void setLevelTypeProvider(Supplier<ChunkLevelType> levelTypeProvider) {
        throw new NotImplementedException();
    }

    @Override
    public ChunkLevelType getLevelType() {
        throw new NotImplementedException();
    }

    @Override
    public void removeChunkTickSchedulers(ServerWorld world) {
        throw new NotImplementedException();
    }

    @Override
    public void addChunkTickSchedulers(ServerWorld world) {
        throw new NotImplementedException();
    }

    @Override
    public void disableTickSchedulers(long time) {
        throw new NotImplementedException();
    }

    @Override
    public void runPostProcessing(ServerWorld world) {
        throw new NotImplementedException();
    }

    @Override
    public Map<BlockPos, BlockEntity> getBlockEntities() {
        throw new NotImplementedException();
    }

    @Override
    public World getWorld() {
        throw new NotImplementedException();
    }

    @Override
    public void setLoadedToWorld(boolean loadedToWorld) {
        throw new NotImplementedException();
    }

    @Override
    public void loadBiomeFromPacket(PacketByteBuf buf) {
        throw new NotImplementedException();
    }

    @Override
    public void loadFromPacket(PacketByteBuf buf, Map<Heightmap.Type, long[]> heightmaps, Consumer<ChunkData.BlockEntityVisitor> blockEntityVisitorConsumer) {
        throw new NotImplementedException();
    }

    @Override
    public boolean isEmpty() {
        throw new NotImplementedException();
    }

    @Override
    public void loadEntities() {
        throw new NotImplementedException();
    }

    @Override
    public void addBlockEntity(BlockEntity blockEntity) {
        throw new NotImplementedException();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos, CreationType creationType) {
        throw new NotImplementedException();
    }

    @Override
    public void setUnsavedListener(UnsavedListener unsavedListener) {
        throw new NotImplementedException();
    }



    @Override
    public @Nullable BlockState setBlockState(BlockPos pos, BlockState state, int flags) {
        return this.getWrappedChunk().setBlockState(pos, state, flags);
    }

    @Override
    public void setBlockEntity(BlockEntity blockEntity) {
        this.getWrappedChunk().setBlockEntity(blockEntity);
    }

    @Override
    public void addEntity(Entity entity) {
        this.getWrappedChunk().addEntity(entity);
    }

    @Override
    public ChunkStatus getStatus() {
        return this.getWrappedChunk().getStatus();
    }

    @Override
    public void removeBlockEntity(BlockPos pos) {
        this.getWrappedChunk().removeBlockEntity(pos);
    }

    @Override
    public @Nullable NbtCompound getPackedBlockEntityNbt(BlockPos pos, RegistryWrapper.WrapperLookup registries) {
        return this.getWrappedChunk().getPackedBlockEntityNbt(pos, registries);
    }

    @Override
    public BasicTickScheduler<Fluid> getFluidTickScheduler() {
        return this.getWrappedChunk().getFluidTickScheduler();
    }

    @Override
    public BasicTickScheduler<Block> getBlockTickScheduler() {
        return this.getWrappedChunk().getBlockTickScheduler();
    }

    @Override
    public TickSchedulers getTickSchedulers(long time) {
        return this.getWrappedChunk().getTickSchedulers(time);
    }

    @Override
    public int getBottomY() {
        return this.getWrappedChunk().getBottomY();
    }

    @Override
    public ChunkSection[] getSectionArray() {
        return this.getWrappedChunk().getSectionArray();
    }

    @Override
    public void markNeedsSaving() {
        this.getWrappedChunk().markNeedsSaving();
    }

    @Override
    public GameEventDispatcher getGameEventDispatcher(int ySectionCoord) {
        return this.getWrappedChunk().getGameEventDispatcher(ySectionCoord);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.getWrappedChunk().getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getWrappedChunk().getFluidState(pos);
    }

    @Override
    public FluidState getFluidState(int x, int y, int z) {
        return this.getWrappedChunk().getFluidState(new BlockPos(x, y, z));
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return this.getWrappedChunk().getBlockEntity(pos);
    }

    @Override
    public @Nullable BlockState setBlockState(BlockPos pos, BlockState state) {
        return this.getWrappedChunk().setBlockState(pos, state);
    }

    @Override
    public int getHighestNonEmptySection() {
        return this.getWrappedChunk().getHighestNonEmptySection();
    }

    @Override
    public Set<BlockPos> getBlockEntityPositions() {
        return this.getWrappedChunk().getBlockEntityPositions();
    }

    @Override
    public ChunkSection getSection(int yIndex) {
        return this.getWrappedChunk().getSection(yIndex);
    }

    @Override
    public Collection<Map.Entry<Heightmap.Type, Heightmap>> getHeightmaps() {
        return this.getWrappedChunk().getHeightmaps();
    }

    @Override
    public void setHeightmap(Heightmap.Type type, long[] heightmap) {
        this.getWrappedChunk().setHeightmap(type, heightmap);
    }

    @Override
    public Heightmap getHeightmap(Heightmap.Type type) {
        return this.getWrappedChunk().getHeightmap(type);
    }

    @Override
    public boolean hasHeightmap(Heightmap.Type type) {
        return this.getWrappedChunk().hasHeightmap(type);
    }

    @Override
    public int sampleHeightmap(Heightmap.Type type, int x, int z) {
        return this.getWrappedChunk().sampleHeightmap(type, x, z);
    }

    @Override
    public ChunkPos getPos() {
        return this.getWrappedChunk().getPos();
    }

    @Override
    public @Nullable StructureStart getStructureStart(Structure structure) {
        return this.getWrappedChunk().getStructureStart(structure);
    }

    @Override
    public void setStructureStart(Structure structure, StructureStart start) {
        this.getWrappedChunk().setStructureStart(structure, start);
    }

    @Override
    public Map<Structure, StructureStart> getStructureStarts() {
        return this.getWrappedChunk().getStructureStarts();
    }

    @Override
    public void setStructureStarts(Map<Structure, StructureStart> structureStarts) {
        this.getWrappedChunk().setStructureStarts(structureStarts);
    }

    @Override
    public LongSet getStructureReferences(Structure structure) {
        return this.getWrappedChunk().getStructureReferences(structure);
    }

    @Override
    public void addStructureReference(Structure structure, long reference) {
        this.getWrappedChunk().addStructureReference(structure, reference);
    }

    @Override
    public Map<Structure, LongSet> getStructureReferences() {
        return this.getWrappedChunk().getStructureReferences();
    }

    @Override
    public void setStructureReferences(Map<Structure, LongSet> structureReferences) {
        this.getWrappedChunk().setStructureReferences(structureReferences);
    }

    @Override
    public boolean areSectionsEmptyBetween(int lowerHeight, int upperHeight) {
        return this.getWrappedChunk().areSectionsEmptyBetween(lowerHeight, upperHeight);
    }

    @Override
    public boolean isSectionEmpty(int sectionCoord) {
        return this.getWrappedChunk().isSectionEmpty(sectionCoord);
    }

    @Override
    public boolean tryMarkSaved() {
        return this.getWrappedChunk().tryMarkSaved();
    }

    @Override
    public boolean needsSaving() {
        return this.getWrappedChunk().needsSaving();
    }

    @Override
    public ChunkStatus getMaxStatus() {
        return this.getWrappedChunk().getMaxStatus();
    }

    @Override
    public void markBlockForPostProcessing(BlockPos pos) {
        this.getWrappedChunk().markBlockForPostProcessing(pos);
    }

    @Override
    public ShortList[] getPostProcessingLists() {
        return this.getWrappedChunk().getPostProcessingLists();
    }

    @Override
    public void markBlocksForPostProcessing(ShortList packedPositions, int index) {
        this.getWrappedChunk().markBlocksForPostProcessing(packedPositions, index);
    }

    @Override
    public void addPendingBlockEntityNbt(NbtCompound nbt) {
        this.getWrappedChunk().addPendingBlockEntityNbt(nbt);
    }

    @Override
    public @Nullable NbtCompound getBlockEntityNbt(BlockPos pos) {
        return this.getWrappedChunk().getBlockEntityNbt(pos);
    }

    @Override
    public void forEachBlockMatchingPredicate(Predicate<BlockState> predicate, BiConsumer<BlockPos, BlockState> consumer) {
        this.getWrappedChunk().forEachBlockMatchingPredicate(predicate, consumer);
    }

    @Override
    public boolean isSerializable() {
        return this.getWrappedChunk().isSerializable();
    }

    @Override
    public UpgradeData getUpgradeData() {
        return this.getWrappedChunk().getUpgradeData();
    }

    @Override
    public boolean usesOldNoise() {
        return this.getWrappedChunk().usesOldNoise();
    }

    @Override
    public @Nullable BlendingData getBlendingData() {
        return this.getWrappedChunk().getBlendingData();
    }

    @Override
    public long getInhabitedTime() {
        return this.getWrappedChunk().getInhabitedTime();
    }

    @Override
    public void increaseInhabitedTime(long timeDelta) {
        this.getWrappedChunk().increaseInhabitedTime(timeDelta);
    }

    @Override
    public void setInhabitedTime(long inhabitedTime) {
        this.getWrappedChunk().setInhabitedTime(inhabitedTime);
    }

    @Override
    public boolean isLightOn() {
        return this.getWrappedChunk().isLightOn();
    }

    @Override
    public void setLightOn(boolean lightOn) {
        this.getWrappedChunk().setLightOn(lightOn);
    }

    @Override
    public int getHeight() {
        return this.getWrappedChunk().getHeight();
    }

    @Override
    public ChunkNoiseSampler getOrCreateChunkNoiseSampler(Function<Chunk, ChunkNoiseSampler> chunkNoiseSamplerCreator) {
        return this.getWrappedChunk().getOrCreateChunkNoiseSampler(chunkNoiseSamplerCreator);
    }

    @Override
    public GenerationSettings getOrCreateGenerationSettings(Supplier<GenerationSettings> generationSettingsCreator) {
        return this.getWrappedChunk().getOrCreateGenerationSettings(generationSettingsCreator);
    }

    @Override
    public RegistryEntry<Biome> getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
        return this.getWrappedChunk().getBiomeForNoiseGen(biomeX, biomeY, biomeZ);
    }

    @Override
    public void populateBiomes(BiomeSupplier biomeSupplier, MultiNoiseUtil.MultiNoiseSampler sampler) {
        this.getWrappedChunk().populateBiomes(biomeSupplier, sampler);
    }

    @Override
    public boolean hasStructureReferences() {
        return this.getWrappedChunk().hasStructureReferences();
    }

    @Override
    public @Nullable BelowZeroRetrogen getBelowZeroRetrogen() {
        return this.getWrappedChunk().getBelowZeroRetrogen();
    }

    @Override
    public boolean hasBelowZeroRetrogen() {
        return this.getWrappedChunk().hasBelowZeroRetrogen();
    }

    @Override
    public HeightLimitView getHeightLimitView() {
        return this.getWrappedChunk().getHeightLimitView();
    }

    @Override
    public void refreshSurfaceY() {
        this.getWrappedChunk().refreshSurfaceY();
    }

    @Override
    public ChunkSkyLight getChunkSkyLight() {
        return this.getWrappedChunk().getChunkSkyLight();
    }

    @Override
    public <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) {
        return this.getWrappedChunk().getBlockEntity(pos, type);
    }

    @Override
    public int getLuminance(BlockPos pos) {
        return this.getWrappedChunk().getLuminance(pos);
    }

    @Override
    public Stream<BlockState> getStatesInBox(Box box) {
        return this.getWrappedChunk().getStatesInBox(box);
    }

    @Override
    public BlockHitResult raycast(BlockStateRaycastContext context) {
        return this.getWrappedChunk().raycast(context);
    }

    @Override
    public BlockHitResult raycast(RaycastContext context) {
        return this.getWrappedChunk().raycast(context);
    }

    @Override
    public @Nullable BlockHitResult raycastBlock(Vec3d start, Vec3d end, BlockPos pos, VoxelShape shape, BlockState state) {
        return this.getWrappedChunk().raycastBlock(start, end, pos, shape, state);
    }

    @Override
    public double getDismountHeight(VoxelShape blockCollisionShape, Supplier<VoxelShape> belowBlockCollisionShapeGetter) {
        return this.getWrappedChunk().getDismountHeight(blockCollisionShape, belowBlockCollisionShapeGetter);
    }

    @Override
    public double getDismountHeight(BlockPos pos) {
        return this.getWrappedChunk().getDismountHeight(pos);
    }

    @Override
    public int getTopYInclusive() {
        return this.getWrappedChunk().getTopYInclusive();
    }

    @Override
    public int countVerticalSections() {
        return this.getWrappedChunk().countVerticalSections();
    }

    @Override
    public int getBottomSectionCoord() {
        return this.getWrappedChunk().getBottomSectionCoord();
    }

    @Override
    public int getTopSectionCoord() {
        return this.getWrappedChunk().getTopSectionCoord();
    }

    @Override
    public boolean isInHeightLimit(int y) {
        return this.getWrappedChunk().isInHeightLimit(y);
    }

    @Override
    public boolean isOutOfHeightLimit(BlockPos pos) {
        return this.getWrappedChunk().isOutOfHeightLimit(pos);
    }

    @Override
    public boolean isOutOfHeightLimit(int y) {
        return this.getWrappedChunk().isOutOfHeightLimit(y);
    }

    @Override
    public int getSectionIndex(int y) {
        return this.getWrappedChunk().getSectionIndex(y);
    }

    @Override
    public int sectionCoordToIndex(int coord) {
        return this.getWrappedChunk().sectionCoordToIndex(coord);
    }

    @Override
    public int sectionIndexToCoord(int index) {
        return this.getWrappedChunk().sectionIndexToCoord(index);
    }

    @Override
    public int hashCode() {
        return this.getWrappedChunk().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.getWrappedChunk().equals(obj);
    }

    @Override
    public String toString() {
        return getWrappedChunk().toString();
    }

    private Chunk getWrappedChunk() {
        return (wrappedChunk != null) ? wrappedChunk : wrappedChunkLocal.get();
    }
}
