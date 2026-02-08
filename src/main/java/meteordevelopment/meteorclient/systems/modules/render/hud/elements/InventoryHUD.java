/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud.elements;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.ColorSetting;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.settings.impl.EnumChoiceSetting;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDElement;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDElementInfo;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDRenderer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ITagged;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class InventoryHUD extends HUDElement {
    
    public static final HUDElementInfo<InventoryHUD> INFO = new HUDElementInfo<>(HUD.GROUP, "inventory", "Displays your inventory.", InventoryHUD::new);
    
    private static final Identifier TEXTURE = MeteorClient.identifier("textures/container.png");
    private static final Identifier TEXTURE_TRANSPARENT = MeteorClient.identifier("textures/container-transparent.png");
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");
    
    private final Setting<Boolean> containers = sgGeneral.add(new BoolSetting.Builder()
        .name("containers")
        .description("Shows the contents of a container when holding them.")
        .defaultValue(false)
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
    
    private final Setting<Background> background = sgBackground.add(new EnumChoiceSetting.Builder<Background>()
        .name("background")
        .description("Background of inventory viewer.")
        .defaultValue(Background.TEXTURE)
        .onChanged(bg -> calculateSize())
        .build()
    );
    
    private final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color of the background.")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(() -> background.get() != Background.NONE)
        .defaultValue(new SettingColor(25, 25, 25, 50))
        .build()
    );
    
    private final ItemStack[] containerItems = new ItemStack[9 * 3];
    private final Color WHITE = new Color(255, 255, 255);
    
    private InventoryHUD() {
        super(INFO);
        
        calculateSize();
    }
    
    @Override
    public void render(HUDRenderer renderer) {
        double x = this.x, y = this.y;
        
        ItemStack container = getContainer();
        boolean hasContainer = containers.get() && container != null;
        if (hasContainer) {
            Utils.getItemsInContainerItem(container, containerItems);
        }
        Color drawColor = hasContainer ? Utils.getShulkerColor(container) : background.get() == Background.FLAT ? backgroundColor.get() : WHITE;
        
        if (background.get() != Background.NONE) {
            drawBackground(renderer, (int) x, (int) y, drawColor);
        }
        
        if (mc.player == null) {
            return;
        }
        
        renderer.post(() -> {
            for (int row = 0; row < 3; row++) {
                for (int i = 0; i < 9; i++) {
                    int index = row * 9 + i;
                    ItemStack stack = hasContainer ? containerItems[index] : mc.player.getInventory().getStack(index + 9);
                    if (stack == null) {
                        continue;
                    }
                    
                    int itemX = background.get() == Background.TEXTURE ? (int) (x + (8 + i * 18) * getScale()) : (int) (x + (1 + i * 18) * getScale());
                    int itemY = background.get() == Background.TEXTURE ? (int) (y + (7 + row * 18) * getScale()) : (int) (y + (1 + row * 18) * getScale());
                    
                    renderer.item(stack, itemX, itemY, (float) getScale(), true);
                }
            }
        });
    }
    
    private void calculateSize() {
        setSize(background.get().width * scale.get(), background.get().height * scale.get());
    }
    
    private void drawBackground(HUDRenderer renderer, int x, int y, Color color) {
        int w = getWidth();
        int h = getHeight();
        
        switch (background.get()) {
            case TEXTURE, OUTLINE ->
                renderer.texture(background.get() == Background.TEXTURE ? TEXTURE : TEXTURE_TRANSPARENT, x, y, w, h, color);
            case FLAT -> renderer.quad(x, y, w, h, color);
        }
    }
    
    private ItemStack getContainer() {
        if (isInEditor() || mc.player == null) {
            return null;
        }
        
        ItemStack stack = mc.player.getOffHandStack();
        if (Utils.hasItems(stack) || stack.getItem() == Items.ENDER_CHEST) {
            return stack;
        }
        
        stack = mc.player.getMainHandStack();
        if (Utils.hasItems(stack) || stack.getItem() == Items.ENDER_CHEST) {
            return stack;
        }
        
        return null;
    }
    
    private double getScale() {
        return customScale.get() ? scale.get() : scale.getDefaultValue();
    }
    
    private enum Background implements ITagged {
        
        NONE("None", 162, 54),
        TEXTURE("Texture", 176, 67),
        OUTLINE("Outline", 162, 54),
        FLAT("Flat", 162, 54);
        
        private final String tag;
        private final int width, height;
        
        Background(String tag, int width, int height) {
            this.tag = tag;
            this.width = width;
            this.height = height;
        }
        
        @Override
        public String getTag() {
            return tag;
        }
        
    }
    
}
