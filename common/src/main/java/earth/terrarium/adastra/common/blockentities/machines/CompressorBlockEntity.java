package earth.terrarium.adastra.common.blockentities.machines;

import earth.terrarium.adastra.common.blockentities.base.RecipeMachineBlockEntity;
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
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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

public class CompressorBlockEntity extends RecipeMachineBlockEntity<CompressingRecipe> {

    public static final List<ConfigurationEntry> SIDE_CONFIG = List.of(
        new ConfigurationEntry(ConfigurationType.SLOT, Configuration.NONE, ConstantComponents.SIDE_CONFIG_INPUT_SLOTS),
        new ConfigurationEntry(ConfigurationType.SLOT, Configuration.NONE, ConstantComponents.SIDE_CONFIG_OUTPUT_SLOTS),
        new ConfigurationEntry(ConfigurationType.ENERGY, Configuration.NONE, ConstantComponents.SIDE_CONFIG_ENERGY)
    );

    public CompressorBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, 3, ModRecipeTypes.COMPRESSING);
        this.energyContainer = EnergyUtils.machineInsertOnlyEnergy(MachineConfig.IRON);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CompressorMenu(id, inventory, this);
    }

    public ValueStorage getEnergyStorage(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        if (energyContainer != null) return energyContainer;
        return energyContainer = EnergyUtils.machineInsertOnlyEnergy(MachineConfig.IRON);
    }

    @Override
    public void tickSideInteractions(BlockPos pos, Predicate<Direction> filter, List<ConfigurationEntry> sideConfig) {
        TransferUtils.pullItemsNearby(this, pos, new int[]{1}, sideConfig.get(0), filter);
        TransferUtils.pushItemsNearby(this, pos, new int[]{2}, sideConfig.get(1), filter);
        TransferUtils.pullEnergyNearby(this, pos, getEnergyStorage().getCapacity(), sideConfig.get(2), filter);
    }

    @Override
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

    @Override
    public boolean canCraft() {
        if (!super.canCraft()) return false;
        if (getEnergyStorage().extract(recipe.energy(), true) < recipe.energy()) return false;
        return ItemUtils.canAddItem(getItem(2), recipe.result());
    }

    @Override
    public void craft() {
        if (recipe == null) return;

        getItem(1).shrink(1);
        ItemUtils.addItem(this, recipe.result(), 2);

        cookTime = 0;
        if (getItem(1).isEmpty()) clearRecipe();
    }

    @Override
    public void update() {
        if (level().isClientSide()) return;
        quickCheck.getRecipeFor(toRecipeInput(), (ServerLevel) level()).ifPresent(r -> {
            recipe = r.value();
            cookTimeTotal = r.value().cookingTime();
        });
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
