package earth.terrarium.adastra.common.items;

import earth.terrarium.adastra.common.constants.ConstantComponents;
import earth.terrarium.adastra.common.items.armor.SpaceSuitItem;
import earth.terrarium.adastra.common.utils.FluidUtils;
import earth.terrarium.adastra.common.utils.TooltipUtils;
import earth.terrarium.common_storage_lib.fluid.impl.SimpleFluidStorage;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.component.TooltipDisplay;

public class GasTankItem extends Item {

    private static final long BUCKET = 81000L;

    private final long tankSize;
    private final long distributionAmount;

    public GasTankItem(Properties properties, long tankSize, long distributionAmount) {
        super(properties);
        this.tankSize = tankSize;
        this.distributionAmount = distributionAmount;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (FluidUtils.hasFluid(stack)) {
            player.startUsingItem(usedHand);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;
        var container = getFluidContainer(stack);
        if (container == null || container.get(0).getAmount() == 0) return;
        if (entity.tickCount % 4 == 0) {
            level.playSound(player, player.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (distributeSequential(stack, container, player.getInventory())) {
            FluidUtils.saveItemFluidStorage(stack, container);
        }
    }

    public boolean distributeSequential(ItemStack from, SimpleFluidStorage container, Inventory inventory) {
        FluidResource resource = container.get(0).getResource();
        if (resource.isBlank() || container.get(0).getAmount() == 0) return false;
        long remaining = Math.min(container.get(0).getAmount(), distributionAmount * BUCKET / 1000L);
        boolean transferred = false;
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack target = inventory.getItem(i);
            if (target.getItem() instanceof SpaceSuitItem suit) {
                SimpleFluidStorage targetContainer = suit.getFluidContainer(target);
                long inserted = FluidUtils.insertFluid(targetContainer.get(0), resource, remaining, false);
                if (inserted > 0) {
                    FluidUtils.saveItemFluidStorage(target, targetContainer);
                    container.get(0).extract(resource, inserted, false);
                    remaining -= inserted;
                    transferred = true;
                }
            }
        }
        return transferred;
    }

    public SimpleFluidStorage getFluidContainer(ItemStack holder) {
        return FluidUtils.getItemFluidStorage(holder, 1, tankSize * BUCKET / 1000L);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> consumer, TooltipFlag isAdvanced) {
        var container = getFluidContainer(stack);
        FluidResource resource = container.get(0).getResource();
        long amount = container.get(0).getAmount();
        long capacity = container.get(0).getLimit(resource);
        consumer.accept(TooltipUtils.getFluidComponent(resource, amount, capacity));
        consumer.accept(TooltipUtils.getMaxFluidOutComponent(distributionAmount * BUCKET / 1000L));
        TooltipUtils.addDescriptionComponent(consumer, ConstantComponents.GAS_TANK_INFO);
    }

    public int getUseDuration(@NotNull ItemStack stack) {
        return 72_000;
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
}
