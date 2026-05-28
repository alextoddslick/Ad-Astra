package earth.terrarium.adastra.mixins.common;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * In 1.21.11, Inventory.tick() only iterates the main inventory (items list),
 * not the armor slots. This means Item.inventoryTick() is never called for
 * equipped armor. This mixin adds armor slot ticking so that SpaceSuitItem
 * and JetSuitItem inventoryTick() methods are called properly.
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Shadow
    @Final
    public Player player;

    @Inject(method = "tick", at = @At("TAIL"))
    private void adastra$tickArmorSlots(CallbackInfo ci) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                stack.inventoryTick(player.level(), player, slot);
            }
        }
    }
}
