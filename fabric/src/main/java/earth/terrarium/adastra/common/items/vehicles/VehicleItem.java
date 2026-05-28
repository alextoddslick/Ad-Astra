package earth.terrarium.adastra.common.items.vehicles;

import earth.terrarium.adastra.common.items.rendered.RenderedItem;
import earth.terrarium.adastra.common.registry.ModFluids;
import earth.terrarium.adastra.common.tags.ModFluidTags;
import earth.terrarium.adastra.common.utils.FluidUtils;
import earth.terrarium.adastra.common.utils.TooltipUtils;
import earth.terrarium.common_storage_lib.fluid.impl.SimpleFluidStorage;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class VehicleItem extends RenderedItem {

    private static final long BUCKET = 81000L;

    private final Supplier<EntityType<?>> type;

    public VehicleItem(Supplier<EntityType<?>> type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public EntityType<?> type() {
        return type.get();
    }

    public SimpleFluidStorage getFluidContainer(ItemStack holder) {
        return FluidUtils.getItemFluidStorage(holder, 1, 3000L * BUCKET / 1000L);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return FluidUtils.hasFluid(stack);
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        var fluidContainer = getFluidContainer(stack);
        return (int) (((double) fluidContainer.get(0).getAmount() / (double) fluidContainer.get(0).getLimit(fluidContainer.get(0).getResource())) * 13);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        var container = getFluidContainer(stack);
        FluidResource resource = container.get(0).getResource();
        if (resource.isBlank()) return 0xFFFFFF;
        return FluidUtils.getFluidBarColor(resource.getType());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> consumer, TooltipFlag isAdvanced) {
        var container = getFluidContainer(stack);
        FluidResource resource = container.get(0).getResource();
        long amount = container.get(0).getAmount();
        long capacity = container.get(0).getLimit(resource);
        consumer.accept(TooltipUtils.getFluidComponent(amount, capacity, resource.isBlank() ? ModFluids.FUEL.get() : resource.getType()));
    }
}
