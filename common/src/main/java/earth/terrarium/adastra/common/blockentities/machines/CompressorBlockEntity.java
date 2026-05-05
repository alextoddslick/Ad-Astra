package earth.terrarium.adastra.common.blockentities.machines;

import earth.terrarium.adastra.common.blockentities.base.ContainerMachineBlockEntity;
import earth.terrarium.adastra.common.blockentities.base.sideconfig.Configuration;
import earth.terrarium.adastra.common.blockentities.base.sideconfig.ConfigurationEntry;
import earth.terrarium.adastra.common.blockentities.base.sideconfig.ConfigurationType;
import earth.terrarium.adastra.common.config.MachineConfig;
import earth.terrarium.adastra.common.constants.ConstantComponents;
import earth.terrarium.adastra.common.menus.machines.CompressorMenu;
import earth.terrarium.adastra.common.recipes.machines.CompressingRecipe;
import earth.terrarium.adastra.common.registry.ModRecipeTypes;
import earth.terrarium.adastra.common.utils.EnergyUtils;
import earth.terrarium.adastra.common.utils.ItemUtils;
import earth.terrarium.adastra.common.utils.TransferUtils;
import earth.terrarium.common_storage_lib.energy.EnergyProvider;
import earth.terrarium.common_storage_lib.energy.impl.SimpleValueStorage;
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * NOTE: Refactored to extend {@link ContainerMachineBlockEntity} directly (mirroring
 * {@code NasaWorkbenchBlockEntity}'s class hierarchy) so we can isolate whether item
 * extraction bugs originated in the {@code EnergyContainerMachineBlockEntity} /
 * {@code RecipeMachineBlockEntity} parent chain. Energy storage, recipe state,
 * cookTime, and battery-slot extraction were inlined as member fields/methods.
 */
public class CompressorBlockEntity extends ContainerMachineBlockEntity implements EnergyProvider.BlockEntity {

    public static final List<ConfigurationEntry> SIDE_CONFIG = List.of(
        new ConfigurationEntry(ConfigurationType.SLOT, Configuration.NONE, ConstantComponents.SIDE_CONFIG_INPUT_SLOTS),
        new ConfigurationEntry(ConfigurationType.SLOT, Configuration.NONE, ConstantComponents.SIDE_CONFIG_OUTPUT_SLOTS),
        new ConfigurationEntry(ConfigurationType.ENERGY, Configuration.NONE, ConstantComponents.SIDE_CONFIG_ENERGY)
    );

    protected SimpleValueStorage energyContainer;

    protected final RecipeManager.CachedCheck<RecipeInput, CompressingRecipe> quickCheck =
        RecipeManager.createCheck(ModRecipeTypes.COMPRESSING.get());

    @Nullable
    protected CompressingRecipe recipe;
    protected int cookTime;
    protected int cookTimeTotal;

    public CompressorBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, 3);
        this.energyContainer = EnergyUtils.machineInsertOnlyEnergy(MachineConfig.IRON);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CompressorMenu(id, inventory, this);
    }

    // ------------- Energy storage (inlined from EnergyContainerMachineBlockEntity) -------------

    public SimpleValueStorage getEnergyStorage() {
        if (energyContainer == null) energyContainer = EnergyUtils.machineInsertOnlyEnergy(MachineConfig.IRON);
        return energyContainer;
    }

    public ValueStorage getEnergyStorage(Direction direction) {
        return getEnergyStorage();
    }

    public ValueStorage getEnergyStorage(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        return getEnergyStorage();
    }

    @Override
    public ValueStorage getEnergy(@Nullable Direction direction) {
        return getEnergyStorage();
    }

    // ------------- Tick logic -------------

    @Override
    public void internalServerTick(ServerLevel level, long time, BlockState state, BlockPos pos) {
        super.internalServerTick(level, time, state, pos);
        // POWER_MACHINE behavior: slot 0 is a battery slot that gets drained into the machine.
        extractBatterySlot();
        if (time % 2 == 0) {
            setChanged();
            sync();
        }
        if (recipe != null && canFunction()) {
            recipeTick(level, getEnergyStorage());
        }
        if (time % 5 == 0) {
            setLit(cookTimeTotal > 0 && recipe != null && canFunction());
        }
    }

    @Override
    public boolean shouldUpdate() {
        return recipe == null;
    }

    @Override
    public void tickSideInteractions(BlockPos pos, Predicate<Direction> filter, List<ConfigurationEntry> sideConfig) {
        TransferUtils.pullItemsNearby(this, pos, new int[]{1}, sideConfig.get(0), filter);
        TransferUtils.pushItemsNearby(this, pos, new int[]{2}, sideConfig.get(1), filter);
        TransferUtils.pullEnergyNearby(this, pos, getEnergyStorage().getCapacity(), sideConfig.get(2), filter);
    }

    public void recipeTick(ServerLevel level, ValueStorage energyStorage) {
        if (recipe == null) return;
        if (!canCraft()) {
            clearRecipe();
            return;
        }

        energyStorage.extract(recipe.energy(), false);

        cookTime++;
        if (cookTime < cookTimeTotal) return;
        craft();
    }

    public boolean canCraft() {
        if (recipe == null) return false;
        if (!recipe.matches(toRecipeInput(), level())) return false;
        if (getEnergyStorage().extract(recipe.energy(), true) < recipe.energy()) return false;
        return ItemUtils.canAddItem(getItem(2), recipe.result());
    }

    public void craft() {
        if (recipe == null) return;

        getItem(1).shrink(1);
        ItemUtils.addItem(this, recipe.result(), 2);

        cookTime = 0;
        if (getItem(1).isEmpty()) clearRecipe();
    }

    public void clearRecipe() {
        recipe = null;
        cookTime = 0;
        cookTimeTotal = 0;
    }

    @Override
    public void update() {
        if (level().isClientSide()) return;
        RecipeHolder<CompressingRecipe> holder = quickCheck.getRecipeFor(toRecipeInput(), (ServerLevel) level()).orElse(null);
        if (holder != null) {
            recipe = holder.value();
            cookTimeTotal = holder.value().cookingTime();
        }
    }

    public int cookTime() {
        return cookTime;
    }

    public int cookTimeTotal() {
        return cookTimeTotal;
    }

    // ------------- Battery slot drain (inlined from EnergyContainerMachineBlockEntity) -------------

    public void extractBatterySlot() {
        ItemStack stack = this.getItem(0);
        if (stack.isEmpty()) return;
        if (!EnergyUtils.holdsEnergy(stack)) return;
        long maxTransfer = EnergyUtils.getMaxOutputPerTick(stack);
        EnergyUtils.transferFromItem(stack, energyContainer, maxTransfer);
    }

    // ------------- NBT -------------

    @Override
    public void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        if (energyContainer != null) {
            energyContainer.set(input.getLongOr("Energy", 0L));
        }
        cookTime = input.getIntOr("CookTime", 0);
        cookTimeTotal = input.getIntOr("CookTimeTotal", 0);
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        if (energyContainer != null) {
            output.putLong("Energy", energyContainer.getStoredAmount());
        }
        output.putInt("CookTime", cookTime);
        output.putInt("CookTimeTotal", cookTimeTotal);
    }

    @Override
    public List<ConfigurationEntry> getDefaultConfig() {
        return SIDE_CONFIG;
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction side) {
        return new int[]{1, 2};
    }
}
