package com.jvn.secondwind.item;

import com.jvn.secondwind.SecondWindMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SecondWindItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SecondWindMod.MOD_ID);

    public static final DeferredItem<Item> SECOND_WIND_POP = ITEMS.register(
            "second_wind_pop",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    private SecondWindItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}