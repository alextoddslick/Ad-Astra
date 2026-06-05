package earth.terrarium.adastra.common.utils;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Reads the NASA-Workbench suit-upgrade flags stored on an item's {@code minecraft:custom_data}
 * (the same component-on-ItemStack pattern Ad Astra uses for energy in {@code EnergyUtils}). The
 * upgrade recipes stamp these flags into the result via the recipe JSON {@code components} block.
 *
 * <ul>
 *   <li>{@code analysis_visor} — on a jet-suit helmet (effects TBD).</li>
 *   <li>{@code boost_mode} — on jet-suit boots; grants extra thrust on high-gravity worlds.</li>
 * </ul>
 */
public final class UpgradeUtils {

    public static final String ANALYSIS_VISOR = "analysis_visor";
    public static final String BOOST_MODE = "boost_mode";

    private UpgradeUtils() {}

    public static boolean has(ItemStack stack, String key) {
        if (stack.isEmpty()) return false;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getBooleanOr(key, false);
    }

    /** Write an upgrade flag onto an item's CUSTOM_DATA (used by the recipe layer and {@code /adastra upgrade}). */
    public static void set(ItemStack stack, String key, boolean value) {
        if (stack.isEmpty()) return;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(key, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean hasAnalysisVisor(ItemStack helmet) {
        return has(helmet, ANALYSIS_VISOR);
    }

    public static boolean hasBoostMode(ItemStack boots) {
        return has(boots, BOOST_MODE);
    }
}
