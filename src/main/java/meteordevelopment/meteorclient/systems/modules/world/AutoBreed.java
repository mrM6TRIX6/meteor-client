/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.settings.impl.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.impl.EnumSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AutoBreed extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Entities to breed.")
        .defaultValue(EntityType.HORSE, EntityType.DONKEY, EntityType.COW,
            EntityType.MOOSHROOM, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN, EntityType.WOLF,
            EntityType.CAT, EntityType.OCELOT, EntityType.RABBIT, EntityType.LLAMA, EntityType.TURTLE,
            EntityType.PANDA, EntityType.FOX, EntityType.BEE, EntityType.STRIDER, EntityType.HOGLIN)
        .onlyAttackable()
        .build()
    );
    
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("How far away the animals can be to be bred.")
        .min(0)
        .defaultValue(4.5)
        .build()
    );
    
    private final Setting<Hand> hand = sgGeneral.add(new EnumSetting.Builder<Hand>()
        .name("hand-for-breeding")
        .description("The hand to use for breeding.")
        .defaultValue(Hand.MAIN_HAND)
        .build()
    );
    
    private final Setting<EntityAge> mobAgeFilter = sgGeneral.add(new EnumSetting.Builder<EntityAge>()
        .name("mob-age-filter")
        .description("Determines the age of the mobs to target (baby, adult, or both).")
        .defaultValue(EntityAge.ADULT)
        .build()
    );
    
    private final List<Entity> animalsFed = new ArrayList<>();
    
    public AutoBreed() {
        super(Categories.World, "AutoBreed", "Automatically breeds specified animals.");
    }
    
    @Override
    public void onActivate() {
        animalsFed.clear();
    }
    
    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof AnimalEntity animal)) {
                continue;
            }
            
            boolean isAllowedType = entities.get().contains(animal.getType());
            boolean isRightAge = checkAgeRequirement(animal, mobAgeFilter.get());
            boolean isNotFedYet = !animalsFed.contains(animal);
            boolean isInRange = PlayerUtils.isWithin(animal, range.get());
            boolean hasCorrectFood = checkHeldFood(animal, hand.get());
            
            boolean shouldSkipAnimal = !isAllowedType
                || !isRightAge
                || !isNotFedYet
                || !isInRange
                || !hasCorrectFood;
            
            if (shouldSkipAnimal) {
                continue;
            }
            
            Rotations.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity), -100, () -> {
                EntityHitResult location = new EntityHitResult(animal, animal.getBoundingBox().getCenter());
                mc.interactionManager.interactEntityAtLocation(mc.player, animal, location, hand.get());
                animalsFed.add(animal);
            });
            
            return;
        }
    }
    
    private boolean checkAgeRequirement(AnimalEntity animal, EntityAge filter) {
        return switch (filter) {
            case BABY -> animal.isBaby();
            case ADULT -> !animal.isBaby();
            case BOTH -> true;
        };
    }
    
    private boolean checkHeldFood(AnimalEntity animal, Hand hand) {
        ItemStack itemStack = hand == Hand.MAIN_HAND
            ? mc.player.getMainHandStack()
            : mc.player.getOffHandStack();
        return animal.isBreedingItem(itemStack);
    }
    
    private enum EntityAge {
        
        BABY,
        ADULT,
        BOTH
        
    }
    
}
