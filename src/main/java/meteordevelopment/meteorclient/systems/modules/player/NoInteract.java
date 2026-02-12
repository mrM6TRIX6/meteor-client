/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.entity.player.EntityAttackEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractEntityEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BlockListSetting;
import meteordevelopment.meteorclient.settings.impl.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.impl.EnumChoiceSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.IDisplayName;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Set;

public class NoInteract extends Module {
    
    private final SettingGroup sgBlocks = settings.createGroup("Blocks");
    private final SettingGroup sgEntities = settings.createGroup("Entities");
    
    // Blocks
    
    private final Setting<List<Block>> blockMine = sgBlocks.add(new BlockListSetting.Builder()
        .name("block-mine")
        .description("Cancels block mining.")
        .build()
    );
    
    private final Setting<ListMode> blockMineMode = sgBlocks.add(new EnumChoiceSetting.Builder<ListMode>()
        .name("block-mine-mode")
        .description("List mode to use for block mine.")
        .defaultValue(ListMode.BLACKLIST)
        .build()
    );
    
    private final Setting<List<Block>> blockInteract = sgBlocks.add(new BlockListSetting.Builder()
        .name("block-interact")
        .description("Cancels block interaction.")
        .build()
    );
    
    private final Setting<ListMode> blockInteractMode = sgBlocks.add(new EnumChoiceSetting.Builder<ListMode>()
        .name("block-interact-mode")
        .description("List mode to use for block interact.")
        .defaultValue(ListMode.BLACKLIST)
        .build()
    );
    
    private final Setting<HandMode> blockInteractHand = sgBlocks.add(new EnumChoiceSetting.Builder<HandMode>()
        .name("block-interact-hand")
        .description("Cancels block interaction if performed by this hand.")
        .defaultValue(HandMode.NONE)
        .build()
    );
    
    // Entities
    
    private final Setting<Set<EntityType<?>>> entityHit = sgEntities.add(new EntityTypeListSetting.Builder()
        .name("entity-hit")
        .description("Cancel entity hitting.")
        .onlyAttackable()
        .build()
    );
    
    private final Setting<ListMode> entityHitMode = sgEntities.add(new EnumChoiceSetting.Builder<ListMode>()
        .name("entity-hit-mode")
        .description("List mode to use for entity hit.")
        .defaultValue(ListMode.BLACKLIST)
        .build()
    );
    
    private final Setting<Set<EntityType<?>>> entityInteract = sgEntities.add(new EntityTypeListSetting.Builder()
        .name("entity-interact")
        .description("Cancel entity interaction.")
        .onlyAttackable()
        .build()
    );
    
    private final Setting<ListMode> entityInteractMode = sgEntities.add(new EnumChoiceSetting.Builder<ListMode>()
        .name("entity-interact-mode")
        .description("List mode to use for entity interact.")
        .defaultValue(ListMode.BLACKLIST)
        .build()
    );
    
    private final Setting<HandMode> entityInteractHand = sgEntities.add(new EnumChoiceSetting.Builder<HandMode>()
        .name("entity-interact-hand")
        .description("Cancels entity interaction if performed by this hand.")
        .defaultValue(HandMode.NONE)
        .build()
    );
    
    private final Setting<InteractMode> friends = sgEntities.add(new EnumChoiceSetting.Builder<InteractMode>()
        .name("friends")
        .description("Friends cancel mode.")
        .defaultValue(InteractMode.NONE)
        .build()
    );
    
    private final Setting<InteractMode> babies = sgEntities.add(new EnumChoiceSetting.Builder<InteractMode>()
        .name("babies")
        .description("Baby entity cancel mode.")
        .defaultValue(InteractMode.NONE)
        .build()
    );
    
    private final Setting<InteractMode> nametagged = sgEntities.add(new EnumChoiceSetting.Builder<InteractMode>()
        .name("nametagged")
        .description("Nametagged entity cancel mode.")
        .defaultValue(InteractMode.NONE)
        .build()
    );
    
    public NoInteract() {
        super(Categories.PLAYER, "NoInteract", "Blocks interactions with certain types of inputs.");
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlockEvent(StartBreakingBlockEvent event) {
        if (!shouldAttackBlock(event.blockPos)) {
            event.cancel();
        }
    }
    
    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        if (!shouldInteractBlock(event.result, event.hand)) {
            event.cancel();
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    private void onAttackEntity(EntityAttackEvent event) {
        if (!shouldAttackEntity(event.entity)) {
            event.cancel();
        }
    }
    
    @EventHandler
    private void onInteractEntity(InteractEntityEvent event) {
        if (!shouldInteractEntity(event.entity, event.hand)) {
            event.cancel();
        }
    }
    
    private boolean shouldAttackBlock(BlockPos blockPos) {
        if (blockMineMode.get() == ListMode.WHITELIST &&
            blockMine.get().contains(mc.world.getBlockState(blockPos).getBlock())) {
            return false;
        }
        
        return blockMineMode.get() != ListMode.BLACKLIST ||
            !blockMine.get().contains(mc.world.getBlockState(blockPos).getBlock());
    }
    
    private boolean shouldInteractBlock(BlockHitResult hitResult, Hand hand) {
        // Hand Interactions
        if (blockInteractHand.get() == HandMode.BOTH ||
            (blockInteractHand.get() == HandMode.MAINHAND && hand == Hand.MAIN_HAND) ||
            (blockInteractHand.get() == HandMode.OFFHAND && hand == Hand.OFF_HAND)) {
            return false;
        }
        
        // Blocks
        if (blockInteractMode.get() == ListMode.BLACKLIST &&
            blockInteract.get().contains(mc.world.getBlockState(hitResult.getBlockPos()).getBlock())) {
            return false;
        }
        
        return blockInteractMode.get() != ListMode.WHITELIST ||
            blockInteract.get().contains(mc.world.getBlockState(hitResult.getBlockPos()).getBlock());
    }
    
    private boolean shouldAttackEntity(Entity entity) {
        // Friends
        if ((friends.get() == InteractMode.BOTH || friends.get() == InteractMode.HIT) &&
            entity instanceof PlayerEntity && !Friends.get().shouldAttack((PlayerEntity) entity)) {
            return false;
        }
        
        // Babies
        if ((babies.get() == InteractMode.BOTH || babies.get() == InteractMode.HIT) &&
            entity instanceof AnimalEntity && ((AnimalEntity) entity).isBaby()) {
            return false;
        }
        
        // NameTagged
        if ((nametagged.get() == InteractMode.BOTH || nametagged.get() == InteractMode.HIT) && entity.hasCustomName()) {
            return false;
        }
        
        // Entities
        if (entityHitMode.get() == ListMode.BLACKLIST &&
            entityHit.get().contains(entity.getType())) {
            return false;
        } else {
            return entityHitMode.get() != ListMode.WHITELIST ||
                entityHit.get().contains(entity.getType());
        }
    }
    
    private boolean shouldInteractEntity(Entity entity, Hand hand) {
        // Hand Interactions
        if (entityInteractHand.get() == HandMode.BOTH ||
            (entityInteractHand.get() == HandMode.MAINHAND && hand == Hand.MAIN_HAND) ||
            (entityInteractHand.get() == HandMode.OFFHAND && hand == Hand.OFF_HAND)) {
            return false;
        }
        
        // Friends
        if ((friends.get() == InteractMode.BOTH || friends.get() == InteractMode.INTERACT) &&
            entity instanceof PlayerEntity && !Friends.get().shouldAttack((PlayerEntity) entity)) {
            return false;
        }
        
        // Babies
        if ((babies.get() == InteractMode.BOTH || babies.get() == InteractMode.INTERACT) &&
            entity instanceof AnimalEntity && ((AnimalEntity) entity).isBaby()) {
            return false;
        }
        
        // NameTagged
        if ((nametagged.get() == InteractMode.BOTH || nametagged.get() == InteractMode.INTERACT) && entity.hasCustomName()) {
            return false;
        }
        
        // Entities
        if (entityInteractMode.get() == ListMode.BLACKLIST &&
            entityInteract.get().contains(entity.getType())) {
            return false;
        } else {
            return entityInteractMode.get() != ListMode.WHITELIST ||
                entityInteract.get().contains(entity.getType());
        }
    }
    
    private enum HandMode implements IDisplayName {
        
        MAINHAND("Mainhand"),
        OFFHAND("Offhand"),
        BOTH("Both"),
        NONE("None");
        
        private final String displayName;
        
        HandMode(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
    }
    
    private enum ListMode implements IDisplayName {
        
        WHITELIST("Whitelist"),
        BLACKLIST("Blacklist");
        
        private final String displayName;
        
        ListMode(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
    }
    
    private enum InteractMode implements IDisplayName {
        
        HIT("Hit"),
        INTERACT("Interact"),
        BOTH("Both"),
        NONE("None");
        
        private final String displayName;
        
        InteractMode(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
    }
    
}
