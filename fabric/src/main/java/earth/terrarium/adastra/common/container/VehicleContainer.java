package earth.terrarium.adastra.common.container;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Simple container that retains the order stacks were inserted in.
 */
public class VehicleContainer extends SimpleContainer {

    public VehicleContainer(int size) {
        super(size);
    }

    public void fromTag(ValueInput input) {
        var list = input.listOrEmpty("Inventory", ItemStack.CODEC);
        for (int i = 0; !list.isEmpty() && i < getContainerSize(); i++) {
            // Read from typed input list
        }
        fromItemList(list);
    }

    public void toTag(ValueOutput output) {
        var list = output.list("Inventory", ItemStack.CODEC);
        storeAsItemList(list);
    }
}
