/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud.elements;

import meteordevelopment.meteorclient.mixin.WorldRendererAccessor;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BlockListSetting;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.ColorSetting;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDElement;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDElementInfo;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class HoleHUD extends HUDElement {
    
    public static final HUDElementInfo<HoleHUD> INFO = new HUDElementInfo<>(HUD.GROUP, "hole", "Displays information about the hole you are standing in.", HoleHUD::new);
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");
    
    // General
    
    private final Setting<List<Block>> safe = sgGeneral.add(new BlockListSetting.Builder()
        .name("safe-blocks")
        .description("Which blocks to consider safe.")
        .defaultValue(Blocks.OBSIDIAN, Blocks.BEDROCK, Blocks.CRYING_OBSIDIAN, Blocks.NETHERITE_BLOCK)
        .build()
    );
    
    // Scale
    
    private final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies a custom scale to this hud element.")
        .defaultValue(false)
        .onChanged(aBoolean -> calculateSize())
        .build()
    );
    
    private final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .visible(customScale::get)
        .defaultValue(2)
        .onChanged(aDouble -> calculateSize())
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );
    
    // Background
    
    private final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays background.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color used for the background.")
        .visible(background::get)
        .defaultValue(new SettingColor(25, 25, 25, 50))
        .build()
    );
    
    private final Color BG_COLOR = new Color(255, 25, 25, 100);
    private final Color OL_COLOR = new Color(255, 25, 25, 255);
    
    public HoleHUD() {
        super(INFO);
        
        calculateSize();
    }
    
    private void calculateSize() {
        setSize(16 * 3 * getScale(), 16 * 3 * getScale());
    }
    
    @Override
    public void render(HUDRenderer renderer) {
        renderer.post(() -> {
            drawBlock(renderer, get(Facing.LEFT), x, y + 16 * getScale()); // Left
            drawBlock(renderer, get(Facing.FRONT), x + 16 * getScale(), y); // Front
            drawBlock(renderer, get(Facing.RIGHT), x + 32 * getScale(), y + 16 * getScale()); // Right
            drawBlock(renderer, get(Facing.BACK), x + 16 * getScale(), y + 32 * getScale()); // Back
        });
        
        if (background.get()) {
            renderer.quad(this.x, this.y, getWidth(), getHeight(), backgroundColor.get());
        }
    }
    
    private Direction get(Facing dir) {
        if (isInEditor()) {
            return Direction.DOWN;
        }
        return Direction.fromHorizontalDegrees(MathHelper.wrapDegrees(mc.player.getYaw() + dir.offset));
    }
    
    private void drawBlock(HUDRenderer renderer, Direction dir, double x, double y) {
        Block block = dir == Direction.DOWN ? Blocks.OBSIDIAN : mc.world.getBlockState(mc.player.getBlockPos().offset(dir)).getBlock();
        if (!safe.get().contains(block)) {
            return;
        }
        
        renderer.item(block.asItem().getDefaultStack(), (int) x, (int) y, getScale(), false);
        
        if (dir == Direction.DOWN) {
            return;
        }
        
        ((WorldRendererAccessor) mc.worldRenderer).meteor$getBlockBreakingInfos().values().forEach(info -> {
            if (info.getPos().equals(mc.player.getBlockPos().offset(dir))) {
                renderBreaking(renderer, x, y, info.getStage() / 9f);
            }
        });
    }
    
    private void renderBreaking(HUDRenderer renderer, double x, double y, double percent) {
        renderer.quad(x, y, (16 * percent) * getScale(), 16 * getScale(), BG_COLOR);
        renderer.quad(x, y, 16 * getScale(), 1 * getScale(), OL_COLOR);
        renderer.quad(x, y + 15 * getScale(), 16 * getScale(), 1 * getScale(), OL_COLOR);
        renderer.quad(x, y, 1 * getScale(), 16 * getScale(), OL_COLOR);
        renderer.quad(x + 15 * getScale(), y, 1 * getScale(), 16 * getScale(), OL_COLOR);
    }
    
    private float getScale() {
        return customScale.get() ? scale.get().floatValue() : scale.getDefaultValue().floatValue();
    }
    
    private enum Facing {
        
        LEFT(-90),
        RIGHT(90),
        FRONT(0),
        BACK(180);
        
        public final int offset;
        
        Facing(int offset) {
            this.offset = offset;
        }
        
    }
    
}
