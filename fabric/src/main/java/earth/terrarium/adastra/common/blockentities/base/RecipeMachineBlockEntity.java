package earth.terrarium.adastra.common.blockentities.base;

import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class RecipeMachineBlockEntity<T extends Recipe<RecipeInput>> extends EnergyContainerMachineBlockEntity {

    @Nullable
    protected T recipe;
    protected int cookTime;
    protected int cookTimeTotal;
    protected final RecipeManager.CachedCheck<RecipeInput, T> quickCheck;

    public RecipeMachineBlockEntity(BlockPos pos, BlockState state, int containerSize, Supplier<RecipeType<T>> recipeType) {
        super(pos, state, containerSize);
        this.quickCheck = RecipeManager.createCheck(recipeType.get());
    }

    @Override
    public void internalServerTick(ServerLevel level, long time, BlockState state, BlockPos pos) {
        super.internalServerTick(level, time, state, pos);
        if (recipe != null && canFunction()) {
            recipeTick(level, getEnergyStorage());
        }
        if (time % 5 == 0 && shouldAutomaticallyUpdateLitState()) {
            setLit(cookTimeTotal > 0 && recipe != null && canFunction());
        }
    }

    public boolean shouldAutomaticallyUpdateLitState() {
        return true;
    }

    @Override
    public boolean shouldUpdate() {
        return recipe == null;
    }

    public abstract void recipeTick(ServerLevel level, ValueStorage energyStorage);

    public boolean canCraft() {
        return recipe != null && recipe.matches(toRecipeInput(), level());
    }

    public abstract void craft();

    public void updateSlots() {}

    @Override
    public void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        cookTime = input.getIntOr("CookTime", 0);
        cookTimeTotal = input.getIntOr("CookTimeTotal", 0);
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("CookTime", cookTime);
        output.putInt("CookTimeTotal", cookTimeTotal);
    }

    public void clearRecipe() {
        recipe = null;
        cookTime = 0;
        cookTimeTotal = 0;
    }

    public int cookTime() {
        return cookTime;
    }

    public int cookTimeTotal() {
        return cookTimeTotal;
    }
}
