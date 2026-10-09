package net.minecraftforge.common.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.EnumSkyBlock;

/**
 * Forge's EnumHelper creates enum constants through sun.reflect.ReflectionFactory and patches Field.modifiers, neither
 * of which exist here. This version has the same public API and does it with the constructor and the $VALUES field.
 */
@SuppressWarnings("unchecked")
public class EnumHelper {
    private static Class<?> named(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Class<?>[][] commonTypes = {
        {EnumAction.class},
        {ItemArmor.ArmorMaterial.class, int.class, int[].class, int.class},
        {named("net.minecraft.entity.item.EntityPainting$EnumArt"), String.class, int.class, int.class, int.class, int.class},
        {EnumCreatureAttribute.class},
        {EnumCreatureType.class, Class.class, int.class, Material.class, boolean.class, boolean.class},
        {EnumEnchantmentType.class},
        {Entity.EnumEntitySize.class},
        {named("net.minecraft.block.BlockPressurePlate$Sensitivity")},
        {MovingObjectPosition.MovingObjectType.class},
        {EnumSkyBlock.class, int.class},
        {EntityPlayer.EnumStatus.class},
        {Item.ToolMaterial.class, int.class, int.class, float.class, float.class, int.class},
        {EnumRarity.class, EnumChatFormatting.class, String.class},
    };

    public static EnumAction addAction(String name) {
        return addEnum(EnumAction.class, name);
    }

    public static ItemArmor.ArmorMaterial addArmorMaterial(String name, int durability, int[] reductionAmounts,
            int enchantability) {
        return addEnum(ItemArmor.ArmorMaterial.class, name, durability, reductionAmounts, enchantability);
    }

    /** (The enum type is package-private, so this is declared with the common supertype.) */
    public static Enum<?> addArt(String name, String tile, int sizeX, int sizeY, int offsetX, int offsetY) {
        return addEnum((Class<Enum<?>>) named("net.minecraft.entity.item.EntityPainting$EnumArt"), name, tile, sizeX,
                sizeY, offsetX, offsetY);
    }

    public static EnumCreatureAttribute addCreatureAttribute(String name) {
        return addEnum(EnumCreatureAttribute.class, name);
    }

    public static EnumCreatureType addCreatureType(String name, Class typeClass, int maxNumber, Material material,
            boolean peaceful, boolean animal) {
        return addEnum(EnumCreatureType.class, name, typeClass, maxNumber, material, peaceful, animal);
    }

    public static EnumEnchantmentType addEnchantmentType(String name) {
        return addEnum(EnumEnchantmentType.class, name);
    }

    public static Entity.EnumEntitySize addEntitySize(String name) {
        return addEnum(Entity.EnumEntitySize.class, name);
    }

    public static Enum<?> addSensitivity(String name) {
        return addEnum((Class<Enum<?>>) named("net.minecraft.block.BlockPressurePlate$Sensitivity"), name);
    }

    public static MovingObjectPosition.MovingObjectType addMovingObjectType(String name) {
        return addEnum(MovingObjectPosition.MovingObjectType.class, name);
    }

    public static EnumSkyBlock addSkyBlock(String name, int lightValue) {
        return addEnum(EnumSkyBlock.class, name, lightValue);
    }

    public static EntityPlayer.EnumStatus addStatus(String name) {
        return addEnum(EntityPlayer.EnumStatus.class, name);
    }

    public static Item.ToolMaterial addToolMaterial(String name, int harvestLevel, int maxUses, float efficiency,
            float damage, int enchantability) {
        return addEnum(Item.ToolMaterial.class, name, harvestLevel, maxUses, efficiency, damage, enchantability);
    }

    public static EnumRarity addRarity(String name, EnumChatFormatting color, String displayName) {
        return addEnum(EnumRarity.class, name, color, displayName);
    }

    public static void setFailsafeFieldValue(Field field, Object target, Object value) throws Exception {
        field.setAccessible(true);
        field.set(target, value);
    }

    public static <T extends Enum<?>> T addEnum(Class<T> enumType, String enumName, Object... paramValues) {
        return addEnum(commonTypes, enumType, enumName, paramValues);
    }

    public static <T extends Enum<?>> T addEnum(Class<?>[][] map, Class<T> enumType, String enumName,
            Object... paramValues) {
        for (Class<?>[] lookup : map) {
            if (lookup[0] != enumType) {
                continue;
            }
            Class<?>[] paramTypes = new Class<?>[lookup.length - 1];
            if (paramTypes.length > 0) {
                System.arraycopy(lookup, 1, paramTypes, 0, paramTypes.length);
            }
            return addEnum(enumType, enumName, paramTypes, paramValues);
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Enum<?>> T addEnum(Class<T> enumType, String enumName, Class<?>[] paramTypes,
            Object[] paramValues) {
        Field valuesField = null;
        for (Field field : enumType.getDeclaredFields()) {
            String name = field.getName();
            if (name.equals("$VALUES") || name.equals("ENUM$VALUES")) {
                valuesField = field;
                break;
            }
        }
        if (valuesField == null) {
            System.err.println("Could not find $VALUES field for enum: " + enumType.getName());
            return null;
        }
        valuesField.setAccessible(true);
        try {
            Enum[] previous = (Enum[]) valuesField.get(enumType);
            Class<?>[] ctorTypes = new Class<?>[paramTypes.length + 2];
            ctorTypes[0] = String.class;
            ctorTypes[1] = int.class;
            System.arraycopy(paramTypes, 0, ctorTypes, 2, paramTypes.length);
            Object[] args = new Object[paramValues.length + 2];
            args[0] = enumName;
            args[1] = previous.length;
            System.arraycopy(paramValues, 0, args, 2, paramValues.length);
            Constructor<T> constructor = enumType.getDeclaredConstructor(ctorTypes);
            constructor.setAccessible(true);
            T created = constructor.newInstance(args);
            ArrayList<Enum> values = new ArrayList<Enum>(Arrays.asList(previous));
            values.add(created);
            valuesField.set(null, values.toArray((Enum[]) Array.newInstance(enumType, 0)));
            return created;
        } catch (Exception e) {
            System.err.println("EnumHelper could not add " + enumName + " to " + enumType.getName() + ": " + e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
