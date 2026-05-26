package earth.terrarium.adastra.common.utils;

import earth.terrarium.adastra.common.constants.ConstantComponents;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class TooltipUtils {

    private static final long BUCKET = 81000L;

    public static String getFormattedAmount(long number) {
        if (com.mojang.blaze3d.platform.InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)) {
            return DecimalFormat.getNumberInstance().format(number);
        }
        NumberFormat compactFormat = NumberFormat.getCompactNumberInstance(Locale.ROOT, NumberFormat.Style.SHORT);
        compactFormat.setMaximumFractionDigits(2);
        return compactFormat.format(number);
    }

    public static Component getEnergyComponent(long energy, long capacity) {
        return Component.translatable("tooltip.ad_astra.energy",
            getFormattedAmount(energy),
            getFormattedAmount(capacity)).withStyle(ChatFormatting.GOLD);
    }

    public static Component getEnergyDifferenceComponent(long energy) {
        return Component.translatable("tooltip.ad_astra.energy_%s".formatted(energy < 0 ? "out" : "in"),
            getFormattedAmount(Math.abs(energy))).withStyle(ChatFormatting.GOLD);
    }

    public static Component getMaxEnergyInComponent(long maxIn) {
        return Component.translatable("tooltip.ad_astra.max_energy_in",
            getFormattedAmount(maxIn)).withStyle(ChatFormatting.GREEN);
    }

    public static Component getMaxEnergyOutComponent(long maxOut) {
        return Component.translatable("tooltip.ad_astra.max_energy_out",
            getFormattedAmount(maxOut)).withStyle(ChatFormatting.GREEN);
    }

    public static Component getEnergyUsePerTickComponent(long usePerTick) {
        return Component.translatable("tooltip.ad_astra.energy_use_per_tick",
            getFormattedAmount(Math.abs(usePerTick))).withStyle(ChatFormatting.AQUA);
    }

    public static Component getEnergyGenerationPerTickComponent(long generationPerTick) {
        return Component.translatable("tooltip.ad_astra.energy_generation_per_tick",
            getFormattedAmount(Math.abs(generationPerTick))).withStyle(ChatFormatting.AQUA);
    }

    public static Component getActiveInactiveComponent(boolean active) {
        return active ? ConstantComponents.ACTIVE.copy().withStyle(ChatFormatting.AQUA) : ConstantComponents.INACTIVE.copy().withStyle(ChatFormatting.AQUA);
    }

    public static Component getDistributionModeComponent(DistributionMode mode) {
        return switch (mode) {
            case SEQUENTIAL -> ConstantComponents.SEQUENTIAL.copy().withStyle(ChatFormatting.AQUA);
            case ROUND_ROBIN -> ConstantComponents.ROUND_ROBIN.copy().withStyle(ChatFormatting.AQUA);
        };
    }

    public static Component getFluidComponent(FluidResource resource, long amount, long capacity) {
        Component fluidName;
        if (!resource.isBlank()) {
            fluidName = resource.getType().getBucket().getName(ItemStack.EMPTY);
        } else {
            fluidName = Component.literal("Empty");
        }
        return Component.translatable("tooltip.ad_astra.fluid",
            getFormattedAmount(amount / 81L),
            getFormattedAmount(capacity / 81L),
            fluidName
        ).withStyle(ChatFormatting.GOLD);
    }

    public static Component getFluidComponent(long amount, long capacity, Fluid fluid) {
        Component fluidName = fluid != null ? fluid.getBucket().getName(ItemStack.EMPTY) : Component.literal("Empty");
        return Component.translatable("tooltip.ad_astra.fluid",
            getFormattedAmount(amount / 81L),
            getFormattedAmount(capacity / 81L),
            fluidName
        ).withStyle(ChatFormatting.GOLD);
    }

    public static Component getFluidComponent(Object fluid, long capacity, Fluid fallback) {
        Component fluidName;
        if (fallback != null) {
            fluidName = fallback.getBucket().getName(ItemStack.EMPTY);
        } else {
            fluidName = Component.literal("Empty");
        }
        return Component.translatable("tooltip.ad_astra.fluid",
            getFormattedAmount(0),
            getFormattedAmount(capacity / 81L),
            fluidName
        ).withStyle(ChatFormatting.GOLD);
    }

    public static Component getFluidComponent(Object fluid, long capacity) {
        return Component.translatable("tooltip.ad_astra.fluid",
            getFormattedAmount(0),
            getFormattedAmount(capacity / 81L),
            Component.literal("Empty")
        ).withStyle(ChatFormatting.GOLD);
    }

    public static Component getFluidDifferenceComponent(long fluid) {
        return Component.translatable("tooltip.ad_astra.fluid_%s".formatted(fluid < 0 ? "out" : "in"),
            getFormattedAmount(Math.abs(fluid) / 81L)).withStyle(ChatFormatting.GOLD);
    }

    public static Component getMaxFluidInComponent(long maxIn) {
        return Component.translatable("tooltip.ad_astra.max_fluid_in",
            getFormattedAmount(maxIn / 81L)).withStyle(ChatFormatting.GREEN);
    }

    public static Component getMaxFluidOutComponent(long maxOut) {
        return Component.translatable("tooltip.ad_astra.max_fluid_out",
            getFormattedAmount(maxOut / 81L)).withStyle(ChatFormatting.GREEN);
    }

    public static Component getFluidUsePerIterationComponent(long usePerTick) {
        return Component.translatable("tooltip.ad_astra.fluid_use_per_iteration",
            getFormattedAmount(Math.abs(usePerTick) / 81L)).withStyle(ChatFormatting.AQUA);
    }

    public static Component getFluidGenerationPerIterationComponent(long gainPerTick) {
        return Component.translatable("tooltip.ad_astra.fluid_generation_per_iteration",
            getFormattedAmount(Math.abs(gainPerTick) / 81L)).withStyle(ChatFormatting.AQUA);
    }

    public static Component getTicksPerIterationComponent(int time) {
        return Component.translatable("tooltip.ad_astra.ticks_per_iteration",
            getFormattedAmount(time)).withStyle(ChatFormatting.AQUA);
    }

    public static Component getDirectionComponent(Direction direction) {
        return Component.translatable("direction.ad_astra.%s".formatted(direction.getName()));
    }

    public static Component getRelativeDirectionComponent(Direction direction) {
        return Component.translatable("direction.ad_astra.relative.%s".formatted(direction.getName()));
    }

    public static void addDescriptionComponent(Consumer<Component> consumer, Component description) {
        if (!com.mojang.blaze3d.platform.InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)) {
            consumer.accept(ConstantComponents.SHIFT_DESCRIPTION);
            return;
        }

        // Split the description into multiple lines if it's too long
        for (FormattedCharSequence text : Minecraft.getInstance().font.split(description, 200)) {
            StringBuilder builder = new StringBuilder();
            text.accept((i, style, codePoint) -> {
                builder.appendCodePoint(codePoint);
                return true;
            });
            consumer.accept(Component.literal(builder.toString()).withStyle(description.getStyle()));
        }
    }

    public static void addDescriptionComponent(List<Component> tooltipComponents, Component description) {
        if (!com.mojang.blaze3d.platform.InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)) {
            tooltipComponents.add(ConstantComponents.SHIFT_DESCRIPTION);
            return;
        }

        // Split the description into multiple lines if it's too long
        for (FormattedCharSequence text : Minecraft.getInstance().font.split(description, 200)) {
            StringBuilder builder = new StringBuilder();
            text.accept((i, style, codePoint) -> {
                builder.appendCodePoint(codePoint);
                return true;
            });
            tooltipComponents.add(Component.literal(builder.toString()).withStyle(description.getStyle()));
        }
    }

    public static Component getProgressComponent(int progress, int maxProgress) {
        return Component.translatable("tooltip.ad_astra.progress",
            progress, maxProgress).withStyle(ChatFormatting.GOLD);
    }

    public static Component getEtaComponent(int progress, int maxProgress, boolean reverse) {
        int eta = reverse ? progress / 20 : (maxProgress - progress) / 20;
        return Component.translatable("tooltip.ad_astra.eta",
            eta).withStyle(ChatFormatting.GOLD);
    }

    /**
     * ETA to finish {@code totalItems} items (not just the current craft). Counts the
     * remainder of the current cycle plus full cycles for the rest of the items.
     */
    public static Component getTotalEtaComponent(int cookTime, int cookTimeTotal, int totalItems) {
        if (totalItems <= 0 || cookTimeTotal <= 0) {
            return Component.translatable("tooltip.ad_astra.eta", 0).withStyle(ChatFormatting.GOLD);
        }
        int remainingTicks = totalItems * cookTimeTotal - cookTime;
        if (remainingTicks < 0) remainingTicks = 0;
        return Component.translatable("tooltip.ad_astra.eta", remainingTicks / 20).withStyle(ChatFormatting.GOLD);
    }
}
