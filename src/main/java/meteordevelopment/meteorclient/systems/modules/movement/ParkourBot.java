/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.game.MessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class ParkourBot extends Module {
    
    private static final Set<Block> PARKOUR_BLOCKS = Set.of(
        Blocks.QUARTZ_BLOCK,
        Blocks.RED_STAINED_GLASS,
        Blocks.BIRCH_LOG,
        Blocks.CHERRY_LOG,
        Blocks.YELLOW_WOOL,
        Blocks.YELLOW_STAINED_GLASS,
        Blocks.OAK_LOG,
        Blocks.RED_WOOL
    );
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between moving to the next block.")
        .defaultValue(2)
        .min(0)
        .sliderRange(0, 50)
        .build()
    );
    
    private final Setting<Boolean> sendStartCommand = sgGeneral.add(new BoolSetting.Builder()
        .name("send-start-command")
        .description("Send /startp command to start parkour on MineBlaze.")
        .defaultValue(true)
        .build()
    );
    
    public ParkourBot() {
        super(Categories.MOVEMENT, "ParkourBot", "The powerful bot for MineBlaze parkour.");
    }
    
    private final ArrayDeque<BlockPos> visitedBlockPoses = new ArrayDeque<>();
    
    private int ticks = 0;
    private BlockPos prevBlockPos;
    
    @Override
    public void onActivate() {
        warning("This bot is not perfect yet, so in order not to fall, also enable the Flight or AirWalk module.");
        
        ticks = 0;
        prevBlockPos = mc.player.getBlockPos().down(1);
        visitedBlockPoses.clear();
        
        if (sendStartCommand.get()) {
            mc.getNetworkHandler().sendChatCommand("startp");
        }
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (++ticks > delay.get()) {
            Set<BlockPos> possibleBlockPoses = new HashSet<>();
            
            BlockIterator.register(5, 5, (
                (blockPos, blockState) -> {
                    if (!isCurrentBlockPos(blockPos)
                        && !isPrevBlockPos(blockPos)
                        && PARKOUR_BLOCKS.contains(blockState.getBlock())
                        && !visitedBlockPoses.contains(blockPos)
                        && !isLogo(blockPos)
                    ) {
                        possibleBlockPoses.add(blockPos.toImmutable());
                    }
                })
            );
            
            BlockIterator.after(() -> {
                prevBlockPos = mc.player.getBlockPos().down(1);
                BlockPos blockPos = findClosestBlockPos(possibleBlockPoses, prevBlockPos);
                
                mc.player.updatePosition(
                    blockPos.getX() + 0.5,
                    blockPos.getY() + 1,
                    blockPos.getZ() + 0.5
                );
                
                addVisitedBlockPos(blockPos);
            });
            
            ticks = 0;
        }
    }
    
    @EventHandler
    private void onMessageReceive(MessageEvent.Receive event) {
        if (event.getMessage().getString().startsWith("| Вы проиграли со счетом")) {
            info("The bot has fallen. Disabling...");
            toggle();
        }
    }
    
    public BlockPos findClosestBlockPos(Set<BlockPos> possibleBlockPoses, BlockPos targetPos) {
        if (possibleBlockPoses == null || possibleBlockPoses.isEmpty() || targetPos == null) {
            return targetPos;
        }
        
        BlockPos closest = targetPos;
        double minDistance = Double.MAX_VALUE;
        
        for (BlockPos pos : possibleBlockPoses) {
            if (pos == null) {
                continue;
            }
            
            double distanceSquared = pos.getSquaredDistance(targetPos);
            
            if (distanceSquared < minDistance) {
                minDistance = distanceSquared;
                closest = pos;
            }
        }
        
        return closest;
    }
    
    private void addVisitedBlockPos(BlockPos blockPos) {
        if (visitedBlockPoses.size() >= 5) {
            visitedBlockPoses.pollFirst();
        }
        visitedBlockPoses.addLast(blockPos);
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
    
    private boolean isLogo(BlockPos blockPos) {
        Block blockWest = mc.world.getBlockState(blockPos.west(1)).getBlock();
        Block blockEast = mc.world.getBlockState(blockPos.east(1)).getBlock();
        
        return blockWest == Blocks.SEA_LANTERN || blockEast == Blocks.SEA_LANTERN;
    }
    
}
