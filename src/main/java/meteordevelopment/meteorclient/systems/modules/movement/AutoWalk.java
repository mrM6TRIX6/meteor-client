/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.pathing.NopPathManager;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.EnumSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;

public class AutoWalk extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Walking mode.")
        .defaultValue(Mode.SMART)
        .onChanged(mode1 -> {
            if (isActive() && Utils.canUpdate()) {
                if (mode1 == Mode.SIMPLE) {
                    PathManagers.get().stop();
                } else {
                    createGoal();
                }
                
                unpress();
            }
        })
        .build()
    );
    
    private final Setting<Direction> direction = sgGeneral.add(new EnumSetting.Builder<Direction>()
        .name("simple-direction")
        .description("The direction to walk in simple mode.")
        .defaultValue(Direction.FORWARDS)
        .onChanged(direction1 -> {
            if (isActive()) {
                unpress();
            }
        })
        .visible(() -> mode.get() == Mode.SIMPLE)
        .build()
    );
    private final Setting<Boolean> disableOnY = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-y-change")
        .description("Disable module if player moves vertically")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.SIMPLE)
        .build()
    );
    private final Setting<Boolean> waitForChunks = sgGeneral.add(new BoolSetting.Builder()
        .name("no-unloaded-chunks")
        .description("Do not allow movement into unloaded chunks")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.SIMPLE)
        .build()
    );
    private final Setting<Boolean> disableOnInput = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-input")
        .description("Disable module on manual movement input")
        .defaultValue(false)
        .build()
    );
    
    public AutoWalk() {
        super(Categories.MOVEMENT, "AutoWalk", "Automatically walks forward.");
    }
    
    @Override
    public void onActivate() {
        if (mode.get() == Mode.SMART) {
            createGoal();
        }
    }
    
    @Override
    public void onDeactivate() {
        if (mode.get() == Mode.SIMPLE) {
            unpress();
        } else {
            PathManagers.get().stop();
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Pre event) {
        if (mode.get() == Mode.SIMPLE) {
            if (disableOnY.get() && mc.player.lastY != mc.player.getY()) {
                toggle();
                return;
            }
            
            switch (direction.get()) {
                case FORWARDS -> mc.options.forwardKey.setPressed(true);
                case BACKWARDS -> mc.options.backKey.setPressed(true);
                case LEFT -> mc.options.leftKey.setPressed(true);
                case RIGHT -> mc.options.rightKey.setPressed(true);
            }
        } else {
            if (PathManagers.get() instanceof NopPathManager) {
                info("Smart mode requires Baritone");
                toggle();
            }
        }
    }
    
    private void onMovement() {
        if (!disableOnInput.get()) {
            return;
        }
        if (mc.currentScreen != null) {
            GUIMove guiMove = Modules.get().get(GUIMove.class);
            if (!guiMove.isActive()) {
                return;
            }
            if (guiMove.skip()) {
                return;
            }
        }
        toggle();
    }
    
    @EventHandler
    private void onKey(KeyEvent event) {
        if (isMovementKey(event.input) && event.action == KeyAction.PRESS) {
            onMovement();
        }
    }
    
    @EventHandler
    private void onMouseClick(MouseClickEvent event) {
        if (isMovementButton(event.click) && event.action == KeyAction.PRESS) {
            onMovement();
        }
    }
    
    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (mode.get() == Mode.SIMPLE && waitForChunks.get()) {
            int chunkX = (int) ((mc.player.getX() + event.movement.x * 2) / 16);
            int chunkZ = (int) ((mc.player.getZ() + event.movement.z * 2) / 16);
            if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
                ((IVec3d) event.movement).meteor$set(0, event.movement.y, 0);
            }
        }
    }
    
    private void unpress() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
    }
    
    private boolean isMovementKey(KeyInput input) {
        return mc.options.forwardKey.matchesKey(input)
            || mc.options.backKey.matchesKey(input)
            || mc.options.leftKey.matchesKey(input)
            || mc.options.rightKey.matchesKey(input)
            || mc.options.sneakKey.matchesKey(input)
            || mc.options.jumpKey.matchesKey(input);
    }
    
    private boolean isMovementButton(Click click) {
        return mc.options.forwardKey.matchesMouse(click)
            || mc.options.backKey.matchesMouse(click)
            || mc.options.leftKey.matchesMouse(click)
            || mc.options.rightKey.matchesMouse(click)
            || mc.options.sneakKey.matchesMouse(click)
            || mc.options.jumpKey.matchesMouse(click);
    }
    
    private void createGoal() {
        PathManagers.get().moveInDirection(mc.player.getYaw());
    }
    
    private enum Mode {
        
        SIMPLE,
        SMART
        
    }
    
    private enum Direction {
        
        FORWARDS,
        BACKWARDS,
        LEFT,
        RIGHT
        
    }
    
}