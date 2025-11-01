/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;

public enum MyPotion {
    
    SWIFTNESS(Potions.SWIFTNESS, Items.NETHER_WART, Items.SUGAR),
    SWIFTNESS_LONG(Potions.LONG_SWIFTNESS, Items.NETHER_WART, Items.SUGAR, Items.REDSTONE),
    SWIFTNESS_STRONG(Potions.STRONG_SWIFTNESS, Items.NETHER_WART, Items.SUGAR, Items.GLOWSTONE_DUST),
    
    SLOWNESS(Potions.SLOWNESS, Items.NETHER_WART, Items.SUGAR, Items.FERMENTED_SPIDER_EYE),
    SLOWNESS_LONG(Potions.LONG_SLOWNESS, Items.NETHER_WART, Items.SUGAR, Items.FERMENTED_SPIDER_EYE, Items.REDSTONE),
    SLOWNESS_STRONG(Potions.STRONG_SLOWNESS, Items.NETHER_WART, Items.SUGAR, Items.FERMENTED_SPIDER_EYE, Items.GLOWSTONE_DUST),
    
    JUMP_BOOST(Potions.LEAPING, Items.NETHER_WART, Items.RABBIT_FOOT),
    JUMP_BOOST_LONG(Potions.LONG_LEAPING, Items.NETHER_WART, Items.RABBIT_FOOT, Items.REDSTONE),
    JUMP_BOOST_STRONG(Potions.STRONG_LEAPING, Items.NETHER_WART, Items.RABBIT_FOOT, Items.GLOWSTONE_DUST),
    
    STRENGTH(Potions.STRENGTH, Items.NETHER_WART, Items.BLAZE_POWDER),
    STRENGTH_LONG(Potions.LONG_STRENGTH, Items.NETHER_WART, Items.BLAZE_POWDER, Items.REDSTONE),
    STRENGTH_STRONG(Potions.STRONG_STRENGTH, Items.NETHER_WART, Items.BLAZE_POWDER, Items.GLOWSTONE_DUST),
    
    HEALING(Potions.HEALING, Items.NETHER_WART, Items.GLISTERING_MELON_SLICE),
    HEALING_STRONG(Potions.STRONG_HEALING, Items.NETHER_WART, Items.GLISTERING_MELON_SLICE, Items.GLOWSTONE_DUST),
    
    HARMING(Potions.HARMING, Items.NETHER_WART, Items.GLISTERING_MELON_SLICE, Items.FERMENTED_SPIDER_EYE),
    HARMING_STRONG(Potions.STRONG_HARMING, Items.NETHER_WART, Items.GLISTERING_MELON_SLICE, Items.FERMENTED_SPIDER_EYE, Items.GLOWSTONE_DUST),
    
    POISON(Potions.POISON, Items.NETHER_WART, Items.SPIDER_EYE),
    POISON_LONG(Potions.LONG_POISON, Items.NETHER_WART, Items.SPIDER_EYE, Items.REDSTONE),
    POISON_STRONG(Potions.STRONG_POISON, Items.NETHER_WART, Items.SPIDER_EYE, Items.GLOWSTONE_DUST),
    
    REGENERATION(Potions.REGENERATION, Items.NETHER_WART, Items.GHAST_TEAR),
    REGENERATION_LONG(Potions.LONG_REGENERATION, Items.NETHER_WART, Items.GHAST_TEAR, Items.REDSTONE),
    REGENERATION_STRONG(Potions.STRONG_REGENERATION, Items.NETHER_WART, Items.GHAST_TEAR, Items.GLOWSTONE_DUST),
    
    FIRE_RESISTANCE(Potions.FIRE_RESISTANCE, Items.NETHER_WART, Items.MAGMA_CREAM),
    FIRE_RESISTANCE_LONG(Potions.LONG_FIRE_RESISTANCE, Items.NETHER_WART, Items.MAGMA_CREAM, Items.REDSTONE),
    
    WATER_BREATHING(Potions.WATER_BREATHING, Items.NETHER_WART, Items.PUFFERFISH),
    WATER_BREATHING_LONG(Potions.LONG_WATER_BREATHING, Items.NETHER_WART, Items.PUFFERFISH, Items.REDSTONE),
    
    NIGHT_VISION(Potions.NIGHT_VISION, Items.NETHER_WART, Items.GOLDEN_CARROT),
    NIGHT_VISION_LONG(Potions.LONG_NIGHT_VISION, Items.NETHER_WART, Items.GOLDEN_CARROT, Items.REDSTONE),
    
    INVISIBILITY(Potions.INVISIBILITY, Items.NETHER_WART, Items.GOLDEN_CARROT, Items.FERMENTED_SPIDER_EYE),
    INVISIBILITY_LONG(Potions.LONG_INVISIBILITY, Items.NETHER_WART, Items.GOLDEN_CARROT, Items.FERMENTED_SPIDER_EYE, Items.REDSTONE),
    
    TURTLE_MASTER(Potions.TURTLE_MASTER, Items.NETHER_WART, Items.TURTLE_HELMET),
    TURTLE_MASTER_LONG(Potions.LONG_TURTLE_MASTER, Items.NETHER_WART, Items.TURTLE_HELMET, Items.REDSTONE),
    TURTLE_MASTER_STRONG(Potions.STRONG_TURTLE_MASTER, Items.NETHER_WART, Items.TURTLE_HELMET, Items.GLOWSTONE_DUST),
    
    SLOW_FALLING(Potions.SLOW_FALLING, Items.NETHER_WART, Items.PHANTOM_MEMBRANE),
    SLOW_FALLING_LONG(Potions.LONG_SLOW_FALLING, Items.NETHER_WART, Items.PHANTOM_MEMBRANE, Items.REDSTONE),
    
    WEAKNESS(Potions.WEAKNESS, Items.FERMENTED_SPIDER_EYE),
    WEAKNESS_LONG(Potions.LONG_WEAKNESS, Items.FERMENTED_SPIDER_EYE, Items.REDSTONE);
    
    public final ItemStack potion;
    public final Item[] ingredients;
    
    MyPotion(RegistryEntry<Potion> potion, Item... ingredients) {
        this.potion = PotionContentsComponent.createStack(Items.POTION, potion);
        this.ingredients = ingredients;
    }
    
}
