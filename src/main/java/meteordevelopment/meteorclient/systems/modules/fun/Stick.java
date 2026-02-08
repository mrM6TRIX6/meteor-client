/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.fun;

import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.settings.impl.EnumChoiceSetting;
import meteordevelopment.meteorclient.settings.impl.Vector3dSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.ITagged;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Position;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;

public class Stick extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Mode> targetMode = sgGeneral.add(new EnumChoiceSetting.Builder<Mode>()
        .name("target-mode")
        .description("The mode at which to follow the player.")
        .defaultValue(Mode.AUTOMATIC)
        .onChanged(onChanged -> target = null)
        .build()
    );
    
    private final Setting<Follow> followMode = sgGeneral.add(new EnumChoiceSetting.Builder<Follow>()
        .name("follow")
        .description("Which parts rotation to follow.")
        .defaultValue(Follow.HEAD)
        .build()
    );
    
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The maximum range to set target.")
        .defaultValue(6)
        .min(0)
        .sliderMax(6)
        .build()
    );
    
    private final Setting<Vector3d> offset = sgGeneral.add(new Vector3dSetting.Builder()
        .name("offset")
        .description("Offset from target.")
        .defaultValue(0, 0, 0)
        .sliderRange(-3, 3)
        .decimalPlaces(1)
        .build()
    );
    
    private final List<Entity> targets = new ArrayList<>();
    private Entity target;
    
    public Stick() {
        super(Categories.FUN, "Stick", "Stick to a player.");
    }
    
    @Override
    public void onActivate() {
        if (targetMode.get() == Mode.AUTOMATIC) {
            setTarget();
        }
    }
    
    @Override
    public void onDeactivate() {
        target = null;
        mc.player.getAbilities().flying = false;
    }
    
    // Middle click mode
    @EventHandler
    private void onMouseClick(MouseClickEvent event) {
        if (targetMode.get() == Mode.MIDDLE_CLICK) {
            if (event.action == KeyAction.PRESS && event.button() == GLFW_MOUSE_BUTTON_MIDDLE && mc.currentScreen == null) {
                if (mc.targetedEntity instanceof PlayerEntity) {
                    target = mc.targetedEntity;
                } else {
                    target = null;
                    mc.player.getAbilities().flying = false;
                }
            }
        }
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (target == null) {
            return;
        }
        
        checkEntity();
        mc.player.getAbilities().flying = true;
        
        switch (followMode.get()) {
            
            case HEAD -> {
                Rotations.rotate(Rotations.getYaw(target), 0);
                Position head = target.raycast(-1 + offset.get().z, 1f / 20f, false).getPos();
                mc.player.setPosition(head.getX() + offset.get().x, head.getY() + offset.get().y, head.getZ());
            }
            case BODY ->
                mc.player.setPosition(target.getX() + offset.get().x, target.getY() + offset.get().y, target.getZ() + offset.get().z);
        }
    }
    
    private boolean entityCheck(Entity entity) {
        if (entity.equals(mc.player) || entity.equals(mc.getCameraEntity())) {
            return false;
        }
        if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) || !entity.isAlive()) {
            return false;
        }
        if (!PlayerUtils.isWithin(entity, range.get())) {
            return false;
        }
        if (!PlayerUtils.canSeeEntity(entity) && !PlayerUtils.isWithin(entity, range.get())) {
            return false;
        }
        
        return entity.isPlayer();
    }
    
    private void checkEntity() {
        List<String> playerNamesList = mc.player.networkHandler.getPlayerList().stream()
            .map(PlayerListEntry::getProfile)
            .map(GameProfile::name)
            .toList();
        
        if (!playerNamesList.contains(EntityUtils.getName(target)) && targetMode.get() == Mode.AUTOMATIC) {
            target = null;
        }
        if (target == null && targetMode.get() == Mode.AUTOMATIC) {
            setTarget();
        }
    }
    
    public void setTarget() {
        TargetUtils.getList(targets, this::entityCheck, SortPriority.LOWEST_DISTANCE, 1);
        if (targets.isEmpty()) {
            return;
        }
        target = targets.get(0);
    }
    
    public enum Mode implements ITagged {
        
        MIDDLE_CLICK("Middle Click"),
        AUTOMATIC("Automatic");
        
        private final String tag;
        
        Mode(String tag) {
            this.tag = tag;
        }
        
        @Override
        public String getTag() {
            return tag;
        }
        
    }
    
    public enum Follow implements ITagged {
        
        HEAD("Head"),
        BODY("Body");
        
        private final String tag;
        
        Follow(String tag) {
            this.tag = tag;
        }
        
        @Override
        public String getTag() {
            return tag;
        }
        
    }
    
}
