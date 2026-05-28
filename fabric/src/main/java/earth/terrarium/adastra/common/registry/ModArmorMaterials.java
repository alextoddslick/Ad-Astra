package earth.terrarium.adastra.common.registry;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.common.tags.ModItemTags;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumMap;
import java.util.Map;

public class ModArmorMaterials {

    public static final ArmorMaterial SPACE_SUIT = new ArmorMaterial(
        15, // durability
        Util.make(new EnumMap<>(ArmorType.class), map -> {
            map.put(ArmorType.BOOTS, 2);
            map.put(ArmorType.LEGGINGS, 5);
            map.put(ArmorType.CHESTPLATE, 6);
            map.put(ArmorType.HELMET, 2);
            map.put(ArmorType.BODY, 5);
        }),
        14,
        SoundEvents.ARMOR_EQUIP_LEATHER,
        0.0f,
        0.0f,
        ModItemTags.STEEL_INGOTS,
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "space_suit"))
    );

    public static final ArmorMaterial NETHERITE_SPACE_SUIT = new ArmorMaterial(
        37, // durability (netherite-tier)
        Util.make(new EnumMap<>(ArmorType.class), map -> {
            map.put(ArmorType.BOOTS, 3);
            map.put(ArmorType.LEGGINGS, 6);
            map.put(ArmorType.CHESTPLATE, 8);
            map.put(ArmorType.HELMET, 3);
            map.put(ArmorType.BODY, 5);
        }),
        15,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        3.0f,
        0.1f,
        net.minecraft.tags.ItemTags.REPAIRS_NETHERITE_ARMOR,
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "netherite_space_suit"))
    );

    public static final ArmorMaterial JET_SUIT = new ArmorMaterial(
        41, // durability (beyond netherite)
        Util.make(new EnumMap<>(ArmorType.class), map -> {
            map.put(ArmorType.BOOTS, 4);
            map.put(ArmorType.LEGGINGS, 7);
            map.put(ArmorType.CHESTPLATE, 9);
            map.put(ArmorType.HELMET, 4);
            map.put(ArmorType.BODY, 5);
        }),
        15,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        5.0f,
        0.1f,
        ModItemTags.CALORITE_INGOTS,
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "jet_suit"))
    );

    public static void init() {
        // Force static initialization
    }
}
