/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.config.Setting;
import meteordevelopment.meteorclient.config.SettingGroup;
import meteordevelopment.meteorclient.config.types.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.player.InventoryUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ItemHud extends HudElement {
    
    public static final HudElementInfo<ItemHud> INFO = new HudElementInfo<>(Hud.GROUP, "item", "Displays the item count.", ItemHud::new);
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");
    
    // General
    
    private final Setting<Item> item = sgGeneral.add(new ItemSetting.Builder()
        .name("item")
        .description("Item to display")
        .defaultValue(Items.TOTEM_OF_UNDYING)
        .build()
    );
    
    private final Setting<NoneMode> noneMode = sgGeneral.add(new EnumSetting.Builder<NoneMode>()
        .name("none-mode")
        .description("How to render the item when you don't have the specified item in your inventory.")
        .defaultValue(NoneMode.HIDE_COUNT)
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
    
    private ItemHud() {
        super(INFO);
        
        calculateSize();
    }
    
    private void calculateSize() {
        setSize(17 * getScale(), 17 * getScale());
    }
    
    @Override
    public void render(HudRenderer renderer) {
        ItemStack itemStack = new ItemStack(item.get(), InventoryUtils.find(item.get()).count());
        
        if (noneMode.get() == NoneMode.HIDE_ITEM && itemStack.isEmpty()) {
            if (isInEditor()) {
                renderer.line(x, y, x + getWidth(), y + getHeight(), Color.GRAY);
                renderer.line(x, y + getHeight(), x + getWidth(), y, Color.GRAY);
            }
        } else {
            renderer.post(() -> render(renderer, itemStack, x, y));
        }
        
        if (background.get()) {
            renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());
        }
    }
    
    private void render(HudRenderer renderer, ItemStack itemStack, int x, int y) {
        if (noneMode.get() == NoneMode.HIDE_ITEM) {
            renderer.item(itemStack, x, y, getScale(), true);
            return;
        }
        
        String countOverride = null;
        boolean resetToZero = false;
        
        if (itemStack.isEmpty()) {
            if (noneMode.get() == NoneMode.SHOW_COUNT) {
                countOverride = "0";
            }
            
            itemStack.setCount(1);
            resetToZero = true;
        }
        
        renderer.item(itemStack, x, y, getScale(), true, countOverride);
        
        if (resetToZero) {
            itemStack.setCount(0);
        }
    }
    
    private float getScale() {
        return customScale.get() ? scale.get().floatValue() : scale.getDefaultValue().floatValue();
    }
    
    private enum NoneMode {
        
        HIDE_ITEM,
        HIDE_COUNT,
        SHOW_COUNT;
        
        @Override
        public String toString() {
            return switch (this) {
                case HIDE_ITEM -> "Hide Item";
                case HIDE_COUNT -> "Hide Count";
                case SHOW_COUNT -> "Show Count";
            };
        }
        
    }
    
}
