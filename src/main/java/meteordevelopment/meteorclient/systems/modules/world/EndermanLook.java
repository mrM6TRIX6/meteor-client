/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.EnumChoiceSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.misc.ITagged;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class EndermanLook extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Mode> lookMode = sgGeneral.add(new EnumChoiceSetting.Builder<Mode>()
        .name("look-mode")
        .description("How this module behaves.")
        .defaultValue(Mode.AWAY)
        .build()
    );
    
    private final Setting<Boolean> stun = sgGeneral.add(new BoolSetting.Builder()
        .name("stun-hostiles")
        .description("Automatically stares at hostile endermen to stun them in place.")
        .defaultValue(true)
        .visible(() -> lookMode.get() == Mode.AWAY)
        .build()
    );
    
    public EndermanLook() {
        super(Categories.WORLD, "EndermanLook", "Either looks at all Endermen or prevents you from looking at Endermen.");
    }
    
    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        // if either are true nothing happens when you look at an enderman
        if (mc.player.getEquippedStack(EquipmentSlot.HEAD).isOf(Blocks.CARVED_PUMPKIN.asItem()) || mc.player.getAbilities().creativeMode) {
            return;
        }
        
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndermanEntity enderman) || !enderman.isAlive() || !mc.player.canSee(enderman)) {
                continue;
            }
            
            switch (lookMode.get()) {
                case AWAY -> {
                    if (enderman.isAngry() && stun.get()) {
                        Rotations.rotate(Rotations.getYaw(enderman), Rotations.getPitch(enderman, Target.HEAD), -75, null);
                    } else if (angleCheck(enderman)) {
                        Rotations.rotate(mc.player.getYaw(), 90, -75, null);
                    }
                }
                case AT -> {
                    if (!enderman.isAngry()) {
                        Rotations.rotate(Rotations.getYaw(enderman), Rotations.getPitch(enderman, Target.HEAD), -75, null);
                    }
                }
            }
        }
    }
    
    /**
     * @see EndermanEntity#isPlayerStaring(PlayerEntity)
     */
    private boolean angleCheck(EndermanEntity entity) {
        Vec3d vec3d = mc.player.getRotationVec(1.0F).normalize();
        Vec3d vec3d2 = new Vec3d(entity.getX() - mc.player.getX(), entity.getEyeY() - mc.player.getEyeY(), entity.getZ() - mc.player.getZ());
        
        double d = vec3d2.length();
        vec3d2 = vec3d2.normalize();
        double e = vec3d.dotProduct(vec3d2);
        
        return e > 1.0D - 0.025D / d;
    }
    
    private enum Mode implements ITagged {
        
        AT("At"),
        AWAY("Away");
        
        private final String tag;
        
        Mode(String tag) {
            this.tag = tag;
        }
        
        @Override
        public String getTag() {
            return tag;
        }
        
    }
    
}
