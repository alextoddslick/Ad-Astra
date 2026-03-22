package earth.terrarium.adastra.common.blockentities.machines;

import earth.terrarium.adastra.common.blockentities.base.RecipeMachineBlockEntity;
import earth.terrarium.adastra.common.blockentities.base.sideconfig.Configuration;
import earth.terrarium.adastra.common.blockentities.base.sideconfig.ConfigurationEntry;
import earth.terrarium.adastra.common.blockentities.base.sideconfig.ConfigurationType;
import earth.terrarium.adastra.common.config.MachineConfig;
import earth.terrarium.adastra.common.constants.ConstantComponents;
import earth.terrarium.adastra.common.menus.machines.FuelRefineryMenu;
import earth.terrarium.adastra.common.recipes.machines.RefiningRecipe;
import earth.terrarium.adastra.common.registry.ModFluids;
import earth.terrarium.adastra.common.registry.ModRecipeTypes;
import earth.terrarium.adastra.common.utils.EnergyUtils;
import earth.terrarium.adastra.common.utils.FluidUtils;
import earth.terrarium.adastra.common.utils.TransferUtils;
import earth.terrarium.common_storage_lib.fluid.util.FluidProvider;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import earth.terrarium.common_storage_lib.storage.base.CommonStorage;
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import earth.terrarium.common_storage_lib.fluid.impl.SimpleFluidStorage;
import earth.terrarium.common_storage_lib.fluid.util.FluidStorageData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class FuelRefineryBlockEntity extends RecipeMachineBlockEntity<RefiningRecipe> implements FluidProvider.BlockEntity {

    private static final long BUCKET = 81000L;

    public static final List<ConfigurationEntry> SIDE_CONFIG = List.of(
        new ConfigurationEntry(ConfigurationType.SLOT, Configuration.NONE, ConstantComponents.SIDE_CONFIG_INPUT_SLOTS),
        new ConfigurationEntry(ConfigurationType.SLOT, Configuration.NONE, ConstantComponents.SIDE_CONFIG_EXTRACTION_SLOTS),
        new ConfigurationEntry(ConfigurationType.SLOT, Configuration.NONE, ConstantComponents.SIDE_CONFIG_OUTPUT_SLOTS),
        new ConfigurationEntry(ConfigurationType.ENERGY, Configuration.NONE, ConstantComponents.SIDE_CONFIG_ENERGY),
        new ConfigurationEntry(ConfigurationType.FLUID, Configuration.NONE, ConstantComponents.SIDE_CONFIG_INPUT_FLUID),
        new ConfigurationEntry(ConfigurationType.FLUID, Configuration.NONE, ConstantComponents.SIDE_CONFIG_OUTPUT_FLUID)
    );

    private SimpleFluidStorage fluidContainer;

    public FuelRefineryBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, 5, ModRecipeTypes.REFINING);
        this.energyContainer = EnergyUtils.machineInsertOnlyEnergy(MachineConfig.STEEL);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FuelRefineryMenu(id, inventory, this);
    }

    public ValueStorage getEnergyStorage(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        if (energyContainer != null) return energyContainer;
        return energyContainer = EnergyUtils.machineInsertOnlyEnergy(MachineConfig.STEEL);
    }

    public @Nullable SimpleFluidStorage getFluidContainer(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        return getFluidContainer();
    }

    public SimpleFluidStorage getFluidContainer() {
        if (fluidContainer != null) return fluidContainer;
        // Filter tank 1 (output) to only accept fuel — prevents oil overflow
        return fluidContainer = new SimpleFluidStorage(2, MachineConfig.STEEL.fluidCapacity * 81L)
            .filter(1, resource -> resource.getType() == ModFluids.FUEL.get());
    }

    @Override
    public CommonStorage<FluidResource> getFluids(@Nullable Direction direction) {
        return getFluidContainer();
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("FluidData", FluidStorageData.CODEC)
            .ifPresent(data -> getFluidContainer().readSnapshot(data));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("FluidData", FluidStorageData.CODEC, getFluidContainer().createSnapshot());
    }

    @Override
    public void tickSideInteractions(BlockPos pos, Predicate<Direction> filter, List<ConfigurationEntry> sideConfig) {
        TransferUtils.pullItemsNearby(this, pos, new int[]{1}, sideConfig.get(0), filter);
        TransferUtils.pullItemsNearby(this, pos, new int[]{3}, sideConfig.get(1), filter);
        TransferUtils.pushItemsNearby(this, pos, new int[]{2, 4}, sideConfig.get(2), filter);
        TransferUtils.pullEnergyNearby(this, pos, getEnergyStorage().getCapacity(), sideConfig.get(3), filter);
        TransferUtils.pullFluidNearby(this, pos, getFluidContainer(), 200 * 81L, 0, sideConfig.get(4), filter);
        TransferUtils.pushFluidNearby(this, pos, getFluidContainer(), 200 * 81L, 1, sideConfig.get(5), filter);
    }

    @Override
    public void internalServerTick(ServerLevel level, long time, BlockState state, BlockPos pos) {
        super.internalServerTick(level, time, state, pos);
        updateSlots();
        if (time % 10 == 0 && recipe == null) {
            update();
        }
    }

    @Override
    public void recipeTick(ServerLevel level, ValueStorage energyStorage) {
        if (recipe == null) return;
        if (fluidContainer == null) getFluidContainer();
        if (!canCraft()) {
            clearRecipe();
            return;
        }

        energyStorage.extract(recipe.energy(), false);

        cookTime++;
        if (cookTime < cookTimeTotal) return;
        craft();
    }

    @Override
    public void craft() {
        if (recipe == null) return;

        // Extract input fluid from tank 0
        FluidResource inputResource = getFluidContainer().get(0).getResource();
        if (!inputResource.isBlank()) {
            getFluidContainer().get(0).extract(inputResource, recipe.input().getAmount(), false);
        }

        // Insert result fluid into tank 1
        FluidUtils.insertFluid(getFluidContainer().get(1), recipe.result(), recipe.resultAmount(), false);

        updateSlots();
        cookTime = 0;
    }

    @Override
    public boolean canCraft() {
        if (recipe == null) return false;
        FluidResource inputResource = getFluidContainer().get(0).getResource();
        if (inputResource.isBlank() || getFluidContainer().get(0).getAmount() < recipe.input().getAmount()) {
            return false;
        }
        if (!FluidUtils.ingredientMatches(recipe.input().ingredient(), inputResource)) {
            return false;
        }
        long insertable = FluidUtils.insertFluid(getFluidContainer().get(1), recipe.result(), recipe.resultAmount(), true);
        if (insertable < recipe.resultAmount()) {
            return false;
        }
        return true;
    }

    @Override
    public void update() {
        // Cannot use quickCheck here because RefiningRecipe.matches() always returns true
        // (fluid-based recipes can't check fluids via RecipeInput). Iterate all recipes instead.
        FluidResource inputResource = getFluidContainer().get(0).getResource();
        if (!inputResource.isBlank() && level().getServer() != null) {
            for (var holder : level().getServer().getRecipeManager().getRecipes()) {
                if (holder.value().getType() != ModRecipeTypes.REFINING.get()) continue;
                RefiningRecipe r = (RefiningRecipe) holder.value();
                if (FluidUtils.ingredientMatches(r.input().ingredient(), inputResource)
                    && getFluidContainer().get(0).getAmount() >= r.input().getAmount()) {
                    recipe = r;
                    cookTimeTotal = r.cookingTime();
                    updateSlots();
                    return;
                }
            }
        }
        clearRecipe();
        updateSlots();
    }

    @Override
    public void updateSlots() {
        // Use getFluidContainer() (not the raw field) to ensure lazy initialization
        FluidUtils.moveItemToContainer(this, getFluidContainer(), 1, 2, 0);
        FluidUtils.moveContainerToItem(this, getFluidContainer(), 3, 4, 1);
        // Don't call sync() here - parent EnergyContainerMachineBlockEntity already syncs every 2 ticks
    }

    @Override
    public List<ConfigurationEntry> getDefaultConfig() {
        return SIDE_CONFIG;
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction side) {
        return new int[]{1, 2, 3, 4};
    }
}
