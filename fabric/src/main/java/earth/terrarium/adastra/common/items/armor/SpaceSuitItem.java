package earth.terrarium.adastra.common.items.armor;

import earth.terrarium.adastra.api.systems.OxygenApi;
import earth.terrarium.adastra.common.constants.ConstantComponents;
import earth.terrarium.adastra.common.items.armor.base.CustomDyeableArmorItem;
import earth.terrarium.adastra.common.registry.ModFluids;
import earth.terrarium.adastra.common.tags.ModFluidTags;
import earth.terrarium.adastra.common.tags.ModItemTags;
import earth.terrarium.adastra.common.utils.FluidUtils;
import earth.terrarium.adastra.common.utils.TooltipUtils;
import earth.terrarium.common_storage_lib.fluid.impl.SimpleFluidStorage;
import earth.terrarium.common_storage_lib.resources.fluid.FluidResource;
// TODO: Migrate to CSL
// import earth.terrarium.botarium.common.fluid.base.FluidContainer;
// import earth.terrarium.botarium.common.fluid.base.FluidHolder;
// import earth.terrarium.botarium.common.fluid.utils.ClientFluidHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class SpaceSuitItem extends CustomDyeableArmorItem {

    protected static final long BUCKET = 81000L;

    protected final long tankSize;

    public SpaceSuitItem(ArmorMaterial material, ArmorType type, long tankSize, Properties properties) {
        super(material, type, properties);
        this.tankSize = tankSize;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> consumer, @NotNull TooltipFlag isAdvanced) {
        var fluidContainer = getFluidContainer(stack);
        long fluidAmount = fluidContainer.get(0).getAmount();
        long fluidCapacity = fluidContainer.get(0).getLimit(fluidContainer.get(0).getResource());
        consumer.accept(TooltipUtils.getFluidComponent(fluidAmount, fluidCapacity, ModFluids.OXYGEN.get()));
        TooltipUtils.addDescriptionComponent(consumer, ConstantComponents.SPACE_SUIT_INFO);
    }

    public SimpleFluidStorage getFluidContainer(ItemStack holder) {
        return FluidUtils.getItemFluidStorage(holder, 1, tankSize * BUCKET / 1000L);
    }

    public static boolean hasFullSet(LivingEntity entity) {
        return hasFullSet(entity, ModItemTags.SPACE_SUITS);
    }

    public static boolean hasFullNetheriteSet(LivingEntity entity) {
        return hasFullSet(entity, ModItemTags.NETHERITE_SPACE_SUITS);
    }

    public static boolean hasFullJetSuitSet(LivingEntity entity) {
        return hasFullSet(entity, ModItemTags.JET_SUITS);
    }

    public static boolean hasFullSet(LivingEntity entity, TagKey<Item> spaceSuitTag) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!entity.getItemBySlot(slot).is(spaceSuitTag)) return false;
        }
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof LivingEntity livingEntity)) return;
        if (livingEntity instanceof Player player && (player.isCreative() || player.isSpectator())) return;
        if (livingEntity.getItemBySlot(EquipmentSlot.CHEST) != stack) return;
        livingEntity.setTicksFrozen(0);
        // Every 12 ticks = 10 minutes per 1,000 mB (1 bucket) oxygen
        if (livingEntity.tickCount % 12 == 0 && hasOxygen(entity)) {
            if (!OxygenApi.API.hasOxygen(entity)) {
                consumeOxygen(stack, 1);
            }
            // Allow the entity to breathe in water
            if (entity.isEyeInFluid(FluidTags.WATER)) {
                consumeOxygen(stack, 1);
                livingEntity.setAirSupply(Math.min(livingEntity.getMaxAirSupply(), livingEntity.getAirSupply() + 4 * 10));
            }
        }
    }

    public void consumeOxygen(ItemStack stack, long amount) {
        var container = getFluidContainer(stack);
        if (container == null) return;
        FluidResource resource = container.get(0).getResource();
        if (resource.isBlank()) return;
        container.get(0).extract(resource, amount * BUCKET / 1000L, false);
        FluidUtils.saveItemFluidStorage(stack, container);
    }

    public static long getOxygenAmount(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) return 0;
        var stack = livingEntity.getItemBySlot(EquipmentSlot.CHEST);
        if (!(stack.getItem() instanceof SpaceSuitItem suit)) return 0;
        return suit.getFluidContainer(stack).get(0).getAmount();
    }

    public static boolean hasOxygen(Entity entity) {
        return getOxygenAmount(entity) > BUCKET / 1000L;
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
