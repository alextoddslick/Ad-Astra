package earth.terrarium.adastra.common.blockentities.machines;

import earth.terrarium.adastra.common.blockentities.base.EnergyContainerMachineBlockEntity;
import earth.terrarium.adastra.common.blockentities.base.sideconfig.Configuration;
import earth.terrarium.adastra.common.blockentities.base.sideconfig.ConfigurationEntry;
import earth.terrarium.adastra.common.blockentities.base.sideconfig.ConfigurationType;
import earth.terrarium.adastra.common.config.MachineConfig;
import earth.terrarium.adastra.common.constants.ConstantComponents;
import earth.terrarium.adastra.common.menus.machines.EtrionicBlastFurnaceMenu;
import earth.terrarium.adastra.common.recipes.machines.AlloyingRecipe;
import earth.terrarium.adastra.common.registry.ModRecipeTypes;
import earth.terrarium.adastra.common.utils.EnergyUtils;
import earth.terrarium.adastra.common.utils.ItemUtils;
import earth.terrarium.adastra.common.utils.TransferUtils;
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public class EtrionicBlastFurnaceBlockEntity extends EnergyContainerMachineBlockEntity {

    public static final List<ConfigurationEntry> SIDE_CONFIG = List.of(
        new ConfigurationEntry(ConfigurationType.SLOT, Configuration.NONE, ConstantComponents.SIDE_CONFIG_INPUT_SLOTS),
        new ConfigurationEntry(ConfigurationType.SLOT, Configuration.NONE, ConstantComponents.SIDE_CONFIG_OUTPUT_SLOTS),
        new ConfigurationEntry(ConfigurationType.ENERGY, Configuration.NONE, ConstantComponents.SIDE_CONFIG_ENERGY)
    );

    @Nullable
    private AlloyingRecipe alloyingRecipe;
    protected final RecipeManager.CachedCheck<RecipeInput, AlloyingRecipe> alloyingQuickCheck = RecipeManager.createCheck(ModRecipeTypes.ALLOYING.get());

    private final BlastingRecipe[] recipes = new BlastingRecipe[4];

    private Mode mode = Mode.ALLOYING;
    protected int cookTime;
    protected int cookTimeTotal;

    public EtrionicBlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, 9);
        this.energyContainer = EnergyUtils.machineInsertOnlyEnergy(MachineConfig.STEEL);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EtrionicBlastFurnaceMenu(id, inventory, this);
    }


    public ValueStorage getEnergyStorage(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        if (energyContainer != null) return energyContainer;
        return energyContainer = EnergyUtils.machineInsertOnlyEnergy(MachineConfig.STEEL);
    }

    @Override
    public void serverTick(ServerLevel level, long time, BlockState state, BlockPos pos) {
        if (canFunction()) {
            recipeTick(getEnergyStorage());
        }
        if (time % 5 == 0) {
            setLit(cookTimeTotal > 0 && canFunction());
        }
    }

    @Override
    public boolean shouldUpdate() {
        for (int i = 0; i < 4; i++) {
            if (recipes[i] == null) return true;
        }
        return false;
    }

    @Override
    public void tickSideInteractions(BlockPos pos, Predicate<Direction> filter, List<ConfigurationEntry> sideConfig) {
        TransferUtils.pullItemsNearby(this, pos, new int[]{1, 2, 3, 4}, sideConfig.get(0), filter);
        TransferUtils.pushItemsNearby(this, pos, new int[]{5, 6, 7, 8}, sideConfig.get(1), filter);
        TransferUtils.pullEnergyNearby(this, pos, getEnergyStorage().getCapacity(), sideConfig.get(2), filter);
    }

    public void recipeTick(ValueStorage energyStorage) {
        if (mode == Mode.ALLOYING) {
            alloyingRecipeTick(energyStorage);
            return;
        }

        boolean isCooking = false;
        boolean shouldClear = true;
        for (int i = 0; i < 4; i++) {
            if (recipes[i] == null) continue;
            if (canCraft(energyStorage, recipes[i], i + 1)) {
                shouldClear = false;
            }
            energyStorage.extract(MachineConfig.etrionicBlastFurnaceBlastingEnergyPerItem, false);
            isCooking = true;
        }

        if (isCooking && cookTime >= cookTimeTotal) {
            // 2x throughput when ALL FOUR input slots have a valid recipe and >=2 items.
            // Decided once at craft time so it can't flip mid-cycle.
            int multiplier = 1;
            boolean allReady = true;
            for (int j = 0; j < 4; j++) {
                if (recipes[j] == null || getItem(j + 1).getCount() < 2) {
                    allReady = false;
                    break;
                }
            }
            if (allReady) {
                long extraEnergy = (long) MachineConfig.etrionicBlastFurnaceBlastingEnergyPerItem * 4L;
                if (energyStorage.extract(extraEnergy, true) >= extraEnergy) {
                    energyStorage.extract(extraEnergy, false);
                    multiplier = 2;
                }
            }
            for (int j = 0; j < 4; j++) {
                craft(recipes[j], j + 1, multiplier);
            }
        }

        if (isCooking) cookTime++;
        if (shouldClear) {
            for (int i = 0; i < 4; i++) {
                clearRecipe(i);
            }
        }
    }

    protected boolean canCraft(ValueStorage energyStorage, BlastingRecipe recipe, int slot) {
        if (recipe == null) return false;
        if (energyStorage.extract(MachineConfig.etrionicBlastFurnaceBlastingEnergyPerItem, true) < MachineConfig.etrionicBlastFurnaceBlastingEnergyPerItem)
            return false;
        if (!recipe.input().test(getItem(slot))) return false;
        return ItemUtils.canAddItem(this, recipe.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(getItem(slot))), 5, 6, 7, 8);
    }

    protected void craft(BlastingRecipe recipe, int slot, int multiplier) {
        if (recipe == null) return;
        int actual = Math.min(multiplier, getItem(slot).getCount());
        if (actual <= 0) return;

        var result = recipe.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(getItem(slot))).copy();
        result.setCount(result.getCount() * actual);

        if (actual > 1 && !ItemUtils.canAddItem(this, result, 5, 6, 7, 8)) {
            actual = 1;
            result = recipe.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(getItem(slot))).copy();
        }

        getItem(slot).shrink(actual);
        ItemUtils.addItem(this, result, 5, 6, 7, 8);

        cookTime = 0;
    }

    public void alloyingRecipeTick(ValueStorage energyStorage) {
        if (alloyingRecipe == null) return;
        if (!canCraftAlloying()) {
            clearAlloyingRecipe();
            return;
        }

        energyStorage.extract(alloyingRecipe.energy(), false);

        cookTime++;
        if (cookTime < cookTimeTotal) return;
        craftAlloying();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canCraftAlloying() {
        if (alloyingRecipe == null) return false;
        if (!alloyingRecipe.matches(toRecipeInput(), level())) return false;
        if (getEnergyStorage().extract(alloyingRecipe.energy(), true) < alloyingRecipe.energy()) return false;
        return ItemUtils.canAddItem(this, alloyingRecipe.result(), 5, 6, 7, 8);
    }

    public void craftAlloying() {
        if (alloyingRecipe == null) return;

        // Multiplier = how many full ingredient sets are present across the 4 input slots.
        // For "1 coal + 1 iron" with 2 coal and 2 iron loaded (any slot configuration),
        // multiplier = 2 → produce 2 steel and consume 2+2 inputs in one craft cycle.
        // Capped at 2 for now (user-requested behavior); higher tiers could extend.
        int multiplier = computeAlloyingMultiplier();

        for (var ingredient : alloyingRecipe.ingredients()) {
            int remaining = multiplier;
            for (int i = 0; i < 4 && remaining > 0; i++) {
                ItemStack slotItem = getItem(i + 1);
                if (ingredient.test(slotItem)) {
                    int take = Math.min(remaining, slotItem.getCount());
                    slotItem.shrink(take);
                    remaining -= take;
                }
            }
        }

        ItemStack result = alloyingRecipe.result().copy();
        result.setCount(result.getCount() * multiplier);
        ItemUtils.addItem(this, result, 5, 6, 7, 8);

        cookTime = 0;
        if (!canCraftAlloying()) clearAlloyingRecipe();
    }

    private int computeAlloyingMultiplier() {
        if (alloyingRecipe == null) return 1;
        // Each ingredient needs >= 2 items available across all 4 slots combined.
        for (var ingredient : alloyingRecipe.ingredients()) {
            int total = 0;
            for (int i = 0; i < 4; i++) {
                ItemStack slotItem = getItem(i + 1);
                if (ingredient.test(slotItem)) total += slotItem.getCount();
            }
            if (total < 2) return 1;
        }
        // Output must accommodate 2× result.
        ItemStack result2x = alloyingRecipe.result().copy();
        result2x.setCount(result2x.getCount() * 2);
        if (!ItemUtils.canAddItem(this, result2x, 5, 6, 7, 8)) return 1;
        return 2;
    }

    @Override
    public void update() {
        if (level().isClientSide()) return;

        if (mode == Mode.BLASTING) {
            for (int i = 0; i < 4; i++) {
                createRecipe(i, i + 1);
            }
        } else {
            alloyingQuickCheck.getRecipeFor(toRecipeInput(), (ServerLevel) level()).ifPresent(r -> {
                alloyingRecipe = r.value();
                cookTimeTotal = r.value().cookingTime();
            });
        }
    }

    protected void createRecipe(int recipe, int slot) {
        if (getItem(slot).isEmpty()) return;
        if (level().getServer() == null) return;
        level().getServer().getRecipeManager()
            .getRecipeFor(RecipeType.BLASTING, new net.minecraft.world.item.crafting.SingleRecipeInput(getItem(slot)), level())
            .ifPresent(r -> {
                recipes[recipe] = r.value();
                cookTimeTotal = r.value().cookingTime();
            });
    }

    @Override
    public void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        cookTime = input.getIntOr("CookTime", 0);
        cookTimeTotal = input.getIntOr("CookTimeTotal", 0);
        mode = Mode.values()[input.getByteOr("Mode", (byte) 0)];
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("CookTime", cookTime);
        output.putInt("CookTimeTotal", cookTimeTotal);
        output.putByte("Mode", (byte) mode.ordinal());
    }

    public void clearRecipe(int recipe) {
        recipes[recipe] = null;
        cookTime = 0;
        cookTimeTotal = 0;
    }

    public void clearAlloyingRecipe() {
        alloyingRecipe = null;
        cookTime = 0;
        cookTimeTotal = 0;
    }

    public int cookTime() {
        return cookTime;
    }

    public int cookTimeTotal() {
        return cookTimeTotal;
    }

    public Mode mode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public List<ConfigurationEntry> getDefaultConfig() {
        return SIDE_CONFIG;
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction side) {
        return new int[]{1, 2, 3, 4, 5, 6, 7, 8};
    }

    public enum Mode {
        ALLOYING,
        BLASTING;

        public Component translation() {
            return Component.translatable("tooltip.ad_astra.etrionic_blast_furnace.mode.%s".formatted(name().toLowerCase(Locale.ROOT)));
        }

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public Mode previous() {
            return values()[(ordinal() - 1 + values().length) % values().length];
        }
    }
}
