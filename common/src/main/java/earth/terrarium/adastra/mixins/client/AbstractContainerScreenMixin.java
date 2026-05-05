package earth.terrarium.adastra.mixins.client;

import earth.terrarium.adastra.client.screens.base.AbstractContainerScreenExtension;
import earth.terrarium.adastra.client.screens.base.MachineScreen;
import earth.terrarium.adastra.common.blockentities.base.ContainerMachineBlockEntity;
import earth.terrarium.adastra.common.menus.base.BaseContainerMenu;
import earth.terrarium.adastra.common.menus.machines.CompressorMenu;
import earth.terrarium.adastra.common.menus.machines.CryoFreezerMenu;
import earth.terrarium.adastra.common.menus.machines.EtrionicBlastFurnaceMenu;
import earth.terrarium.adastra.common.menus.slots.BatterySlot;
import earth.terrarium.adastra.common.menus.slots.InventorySlot;
import earth.terrarium.adastra.common.network.NetworkHandler;
import earth.terrarium.adastra.common.network.packets.ServerboundExtractFromMachineSlotPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> implements AbstractContainerScreenExtension {

    @Shadow protected T menu;
    @Shadow protected Slot hoveredSlot;
    @Shadow protected abstract void slotClicked(Slot slot, int slotId, int button, ClickType clickType);

    @Inject(
        method = "renderSlot",
        at = @At("HEAD")
    )
    private void adastra$renderPreSlot(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        this.adastra$renderPreSlot(graphics, slot);
    }

    /**
     * Force-deliver empty-cursor left/right click on machine slots in our MachineScreen menus.
     * Some widget in the MachineScreen widget tree is consuming the click before vanilla can
     * route it to slotClicked() — bypass the widget delegation entirely for this specific case.
     */
    @Inject(
        method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void adastra$forceMachineSlotClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        if (!(self instanceof MachineScreen<?, ?>)) {
            return;
        }
        if (this.menu == null) {
            return;
        }
        if (!this.menu.getCarried().isEmpty()) {
            return;
        }
        int button = event.button();
        if (button != 0 && button != 1) {
            return;
        }
        Slot slot = this.hoveredSlot;
        if (slot == null) {
            return;
        }
        // Only act on machine slots: skip player inventory and battery slots.
        if (slot instanceof InventorySlot || slot instanceof BatterySlot) {
            return;
        }
        if (!slot.hasItem()) {
            return;
        }
        ClickType type = event.hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP;

        // For menus where the click is being eaten before reaching AbstractContainerMenu#clicked()
        // (Compressor, Etrionic Blast Furnace, Cryo Freezer), bypass vanilla's UI flow entirely
        // and send a bespoke serverbound extract packet. This sidesteps whatever is consuming the
        // click in the widget tree / menu pipeline.
        if (button == 0 && type == ClickType.PICKUP
            && (this.menu instanceof CompressorMenu
                || this.menu instanceof EtrionicBlastFurnaceMenu
                || this.menu instanceof CryoFreezerMenu)
            && this.menu instanceof BaseContainerMenu<?> baseMenu
            && baseMenu.getEntity() instanceof ContainerMachineBlockEntity machine) {
            NetworkHandler.CHANNEL.sendToServer(
                new ServerboundExtractFromMachineSlotPacket(machine.getBlockPos(), slot.index, true)
            );
            cir.setReturnValue(true);
            return;
        }

        this.slotClicked(slot, slot.index, button, type);
        cir.setReturnValue(true);
    }
}
