package com.jvn.secondwind.item;

import com.jvn.secondwind.SecondWindMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public final class SecondWindItems {
    public static final Item SECOND_WIND_POP = new HiddenActivationItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    private SecondWindItems() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(SecondWindMod.MOD_ID, "second_wind_pop"), SECOND_WIND_POP);
    }

    private static final class HiddenActivationItem extends Item {
        private HiddenActivationItem(Properties properties) {
            super(properties);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
            if (!stack.isEmpty()) {
                stack.setCount(0);
            }
        }
    }
}
