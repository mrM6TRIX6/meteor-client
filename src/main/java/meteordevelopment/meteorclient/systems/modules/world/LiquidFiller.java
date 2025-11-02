/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.config.Setting;
import meteordevelopment.meteorclient.config.SettingGroup;
import meteordevelopment.meteorclient.config.types.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InventoryUtils;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LiquidFiller extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");
    
    private final Setting<PlaceIn> placeInLiquids = sgGeneral.add(new EnumSetting.Builder<PlaceIn>()
        .name("place-in")
        .description("What type of liquids to place in.")
        .defaultValue(PlaceIn.BOTH)
        .build()
    );
    
    private final Setting<Shape> shape = sgGeneral.add(new EnumSetting.Builder<Shape>()
        .name("shape")
        .description("The shape of placing algorithm.")
        .defaultValue(Shape.SPHERE)
        .build()
    );
    
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The place range.")
        .defaultValue(4)
        .min(0)
        .build()
    );
    
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between actions in ticks.")
        .defaultValue(0)
        .min(0)
        .build()
    );
    
    private final Setting<Integer> maxBlocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("max-blocks-per-tick")
        .description("Maximum blocks to try to place per tick.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 10)
        .build()
    );
    
    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
        .name("sort-mode")
        .description("The blocks you want to place first.")
        .defaultValue(SortMode.CLOSEST)
        .build()
    );
    
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically rotates towards the space targeted for filling.")
        .defaultValue(true)
        .build()
    );
    
    // Whitelist and blacklist
    
    private final Setting<ListMode> listMode = sgWhitelist.add(new EnumSetting.Builder<ListMode>()
        .name("list-mode")
        .description("Selection mode.")
        .defaultValue(ListMode.WHITELIST)
        .build()
    );
    
    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("whitelist")
        .description("The allowed blocks that it will use to fill up the liquid.")
        .defaultValue(
            Blocks.DIRT,
            Blocks.COBBLESTONE,
            Blocks.STONE,
            Blocks.NETHERRACK,
            Blocks.DIORITE,
            Blocks.GRANITE,
            Blocks.ANDESITE
        )
        .visible(() -> listMode.get() == ListMode.WHITELIST)
        .build()
    );
    
    private final Setting<List<Block>> blacklist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("blacklist")
        .description("The denied blocks that it not will use to fill up the liquid.")
        .visible(() -> listMode.get() == ListMode.BLACKLIST)
        .build()
    );
    
    private final List<BlockPos.Mutable> blocks = new ArrayList<>();
    
    private int timer;
    
    public LiquidFiller() {
        super(Categories.World, "liquid-filler", "Places blocks inside of liquid source blocks within range of you.");
    }
    
    @Override
    public void onActivate() {
        timer = 0;
    }
    
    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        // Update timer according to delay
        if (timer < delay.get()) {
            timer++;
            return;
        } else {
            timer = 0;
        }
        
        // Calculate some stuff
        double pX = mc.player.getX();
        double pY = mc.player.getY();
        double pZ = mc.player.getZ();
        
        double rangeSq = Math.pow(range.get(), 2);
        
        if (shape.get() == Shape.UNIFORM_CUBE) {
            range.set((double) Math.round(range.get()));
        }
        
        // Find slot with a block
        FindItemResult item;
        if (listMode.get() == ListMode.WHITELIST) {
            item = InventoryUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem && whitelist.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        } else {
            item = InventoryUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem && !blacklist.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        }
        if (!item.found()) {
            return;
        }
        
        // Loop blocks around the player
        BlockIterator.register((int) Math.ceil(range.get() + 1), (int) Math.ceil(range.get()), (blockPos, blockState) -> {
            boolean toofarSphere = Utils.squaredDistance(pX, pY, pZ, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) > rangeSq;
            boolean toofarUniformCube = maxDist(Math.floor(pX), Math.floor(pY), Math.floor(pZ), blockPos.getX(), blockPos.getY(), blockPos.getZ()) >= range.get();
            
            // Check distance
            if ((toofarSphere && shape.get() == Shape.SPHERE) || (toofarUniformCube && shape.get() == Shape.UNIFORM_CUBE)) {
                return;
            }
            
            // Check if the block is a source block and set to be filled
            Fluid fluid = blockState.getFluidState().getFluid();
            if ((placeInLiquids.get() == PlaceIn.BOTH && (fluid != Fluids.WATER && fluid != Fluids.LAVA))
                || (placeInLiquids.get() == PlaceIn.WATER && fluid != Fluids.WATER)
                || (placeInLiquids.get() == PlaceIn.LAVA && fluid != Fluids.LAVA)) {
                return;
            }
            
            // Check if the player can place at pos
            if (!BlockUtils.canPlace(blockPos)) {
                return;
            }
            
            // Add block
            blocks.add(blockPos.mutableCopy());
        });
        
        BlockIterator.after(() -> {
            // Sort blocks
            if (sortMode.get() == SortMode.TOP_DOWN || sortMode.get() == SortMode.BOTTOM_UP) {
                blocks.sort(Comparator.comparingDouble(value -> value.getY() * (sortMode.get() == SortMode.BOTTOM_UP ? 1 : -1)));
            } else if (sortMode.get() != SortMode.NONE) {
                blocks.sort(Comparator.comparingDouble(value -> Utils.squaredDistance(pX, pY, pZ, value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5) * (sortMode.get() == SortMode.CLOSEST ? 1 : -1)));
            }
            
            // Place and clear place positions
            int count = 0;
            for (BlockPos pos : blocks) {
                if (count >= maxBlocksPerTick.get()) {
                    break;
                }
                BlockUtils.place(pos, item, rotate.get(), 0, true);
                count++;
            }
            blocks.clear();
        });
    }
    
    private static double maxDist(double x1, double y1, double z1, double x2, double y2, double z2) {
        // Gets the largest X, Y or Z difference, manhattan style
        double dX = Math.ceil(Math.abs(x2 - x1));
        double dY = Math.ceil(Math.abs(y2 - y1));
        double dZ = Math.ceil(Math.abs(z2 - z1));
        return Math.max(Math.max(dX, dY), dZ);
    }
    
    private enum ListMode {
        
        WHITELIST,
        BLACKLIST
        
    }
    
    private enum PlaceIn {
        
        BOTH,
        WATER,
        LAVA
        
    }
    
    private enum SortMode {
        
        NONE,
        CLOSEST,
        FURTHEST,
        TOP_DOWN,
        BOTTOM_UP
        
    }
    
    private enum Shape {
        
        SPHERE,
        UNIFORM_CUBE
        
    }
    
}