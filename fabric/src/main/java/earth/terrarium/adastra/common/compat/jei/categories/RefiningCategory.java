package earth.terrarium.adastra.common.compat.jei.categories;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.client.screens.machines.FuelRefineryScreen;
import earth.terrarium.adastra.common.compat.jei.drawables.EnergyBarDrawable;
import earth.terrarium.adastra.common.compat.jei.drawables.FluidBarDrawable;
import earth.terrarium.adastra.common.config.MachineConfig;
import earth.terrarium.adastra.common.recipes.machines.RefiningRecipe;
import earth.terrarium.adastra.common.registry.ModBlocks;
import earth.terrarium.adastra.common.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;

public record RefiningCategory(IGuiHelper guiHelper) implements IRecipeCategory<RefiningRecipe> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "refining");
    public static final RecipeType<RefiningRecipe> RECIPE = new RecipeType<>(ID, RefiningRecipe.class);

    @Override
    public RecipeType<RefiningRecipe> getRecipeType() {
        return RECIPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(ModBlocks.FUEL_REFINERY.get().getDescriptionId());
    }

    @Override
    public int getWidth() {
        return 180;
    }

    @Override
    public int getHeight() {
        return 105;
    }

    @Override
    public IDrawable getIcon() {
        return guiHelper.createDrawableItemStack(ModItems.FUEL_REFINERY.get().getDefaultInstance());
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RefiningRecipe recipe, IFocusGroup focuses) {
        // Catalyst role removed in JEI for 1.21.1; catalyst registration handled in registerRecipeCatalysts
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 14, 18);
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 14, 48);
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 129, 18);
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 129, 48);
    }

    @Override
    public void draw(RefiningRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, FuelRefineryScreen.TEXTURE, 2, -4, 0, 0, 177, 100, 177, 184);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FuelRefineryScreen.TEXTURE, 2, 96, 0, 177, 177, 7, 177, 184);

        new EnergyBarDrawable(mouseX, mouseY, -recipe.energy(), MachineConfig.STEEL.energyCapacity, MachineConfig.STEEL.maxEnergyInOut, 0).draw(graphics, 146, 50);

        int cookTime = recipe.cookingTime();
        long capacity = MachineConfig.STEEL.fluidCapacity * 81L;
        var inputFluids = recipe.input().ingredient().getMatchingFluids();
        if (!inputFluids.isEmpty()) {
            new FluidBarDrawable(mouseX, mouseY, false, capacity, cookTime, inputFluids.get(0).getType(), recipe.input().getAmount())
                .draw(graphics, 39, 49);
        }
        new FluidBarDrawable(mouseX, mouseY, true, capacity, cookTime, recipe.result().getType(), recipe.resultAmount())
            .draw(graphics, 96, 49);
    }
}
