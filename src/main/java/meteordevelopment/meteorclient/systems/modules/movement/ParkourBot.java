/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;

public class ParkourBot extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between moving to the next block.")
        .defaultValue(2)
        .min(0)
        .sliderRange(0, 50)
        .build()
    );
    
    public ParkourBot() {
        super(Categories.MOVEMENT, "ParkourBot", "Powerful parkour bot for MineBlaze parkour.");
    }
    
    private int ticks = 0;
    private BlockPos prevBlockPos;
    
    @Override
    public void onActivate() {
        ticks = 0;
        prevBlockPos = mc.player.getBlockPos().down(1);
        mc.getNetworkHandler().sendChatCommand("startp");
    }
    
    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (++ticks > delay.get()) {
            BlockIterator.register(4, 3, (
                (blockPos, blockState) -> {
                    if (!isCurrentBlockPos(blockPos) && !isPrevBlockPos(blockPos) && !blockState.isAir()) {
                        prevBlockPos = mc.player.getBlockPos().down(1);
                        mc.player.updatePosition(
                            blockPos.getX() + 0.5,
                            blockPos.getY() + 1,
                            blockPos.getZ() + 0.5
                        );
                        BlockIterator.disableCurrent();
                    }
                })
            );
            
            ticks = 0;
        }
    }
    
    private boolean isCurrentBlockPos(BlockPos blockPos) {
        BlockPos currentBlockPos = mc.player.getBlockPos().down(1);
        return currentBlockPos.getX() == blockPos.getX()
            && currentBlockPos.getY() == blockPos.getY()
            && currentBlockPos.getZ() == blockPos.getZ();
    }
    
    private boolean isPrevBlockPos(BlockPos blockPos) {
        return prevBlockPos.getX() == blockPos.getX()
            && prevBlockPos.getY() == blockPos.getY()
            && prevBlockPos.getZ() == blockPos.getZ();
    }
    
}
