package earth.terrarium.adastra.common.items.vehicles;

import earth.terrarium.adastra.common.blocks.LaunchPadBlock;
import earth.terrarium.adastra.common.blocks.properties.LaunchPadPartProperty;
import earth.terrarium.adastra.common.constants.ConstantComponents;
import earth.terrarium.adastra.common.entities.vehicles.Rocket;
import earth.terrarium.adastra.common.tags.ModBlockTags;
import earth.terrarium.adastra.common.utils.FluidUtils;
import earth.terrarium.adastra.common.utils.TooltipUtils;
import earth.terrarium.common_storage_lib.fluid.impl.SimpleFluidStorage;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class RocketItem extends VehicleItem {

    public RocketItem(Supplier<EntityType<?>> type, Properties properties) {
        super(type, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var pos = context.getClickedPos();
        var stack = context.getItemInHand();
        var state = level.getBlockState(pos);

        if (!state.is(ModBlockTags.LAUNCH_PADS)) {
            return InteractionResult.PASS;
        }
        if (state.hasProperty(LaunchPadBlock.PART) && state.getValue(LaunchPadBlock.PART) != LaunchPadPartProperty.CENTER) {
            return InteractionResult.PASS;
        }

        var vehicle = type().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (vehicle == null) {
            return InteractionResult.PASS;
        }
        level.playSound(context.getPlayer(), pos, SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.BLOCKS, 1, 1);
        vehicle.setPos(pos.getX() + 0.5, pos.getY() + 0.125f, pos.getZ() + 0.5);
        vehicle.setYRot(context.getHorizontalDirection().getOpposite().toYRot());
        level.addFreshEntity(vehicle);

        if (vehicle instanceof Rocket rocket) {
            SimpleFluidStorage itemFluid = getFluidContainer(stack);
            FluidResource resource = itemFluid.get(0).getResource();
            long amount = itemFluid.get(0).getAmount();
            if (!resource.isBlank() && amount > 0) {
                FluidUtils.insertFluid(rocket.fluidContainer().get(0), resource, amount, false);
            }
        }

        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> consumer, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, tooltipDisplay, consumer, isAdvanced);
        TooltipUtils.addDescriptionComponent(consumer, ConstantComponents.ROCKET_INFO);
    }
}
