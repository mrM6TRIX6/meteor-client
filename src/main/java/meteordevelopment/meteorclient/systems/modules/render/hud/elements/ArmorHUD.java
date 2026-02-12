/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud.elements;

import meteordevelopment.meteorclient.renderer.text.TextRenderer;
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
import meteordevelopment.meteorclient.utils.misc.IDisplayName;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ArmorHUD extends HUDElement {
    
    public static final HUDElementInfo<ArmorHUD> INFO = new HUDElementInfo<>(HUD.GROUP, "armor", "Displays your armor.", ArmorHUD::new);
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDurability = settings.createGroup("Durability");
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");
    
    // General
    
    private final Setting<Orientation> orientation = sgGeneral.add(new EnumChoiceSetting.Builder<Orientation>()
        .name("orientation")
        .description("How to display armor.")
        .defaultValue(Orientation.HORIZONTAL)
        .onChanged(val -> calculateSize())
        .build()
    );
    
    private final Setting<Boolean> flipOrder = sgGeneral.add(new BoolSetting.Builder()
        .name("flip-order")
        .description("Flips the order of armor items.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> showEmpty = sgGeneral.add(new BoolSetting.Builder()
        .name("show-empty")
        .description("Renders barrier icons for empty slots.")
        .defaultValue(false)
        .build()
    );
    
    // Durability
    
    private final Setting<Durability> durability = sgDurability.add(new EnumChoiceSetting.Builder<Durability>()
        .name("durability")
        .description("How to display armor durability.")
        .defaultValue(Durability.BAR)
        .onChanged(durability1 -> calculateSize())
        .build()
    );
    
    private final Setting<SettingColor> durabilityColor = sgDurability.add(new ColorSetting.Builder()
        .name("durability-color")
        .description("Color of the text.")
        .visible(() -> durability.get() == Durability.TOTAL || durability.get() == Durability.PERCENTAGE)
        .defaultValue(new SettingColor())
        .build()
    );
    
    private final Setting<Boolean> durabilityShadow = sgDurability.add(new BoolSetting.Builder()
        .name("durability-shadow")
        .description("Text shadow.")
        .visible(() -> durability.get() == Durability.TOTAL || durability.get() == Durability.PERCENTAGE)
        .defaultValue(true)
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
    
    public ArmorHUD() {
        super(INFO);
        
        calculateSize();
    }
    
    private void calculateSize() {
        switch (orientation.get()) {
            // Four item stacks plus
            case HORIZONTAL -> setSize((16 * 4 + 2 * 4) * getScale(), 16 * getScale());
            case VERTICAL -> setSize(16 * getScale(), (16 * 4 + 2 * 4) * getScale());
        }
    }
    
    @Override
    public void render(HUDRenderer renderer) {
        int emptySlots = 0;
        
        // default order is from boots to helmet
        ItemStack[] armor = flipOrder.get() ?
            new ItemStack[] { getItem(EquipmentSlot.HEAD), getItem(EquipmentSlot.CHEST), getItem(EquipmentSlot.LEGS), getItem(EquipmentSlot.FEET) } :
            new ItemStack[] { getItem(EquipmentSlot.FEET), getItem(EquipmentSlot.LEGS), getItem(EquipmentSlot.CHEST), getItem(EquipmentSlot.HEAD) };
        
        for (ItemStack stack : armor) {
            if (stack.isEmpty()) {
                emptySlots++;
            }
        }
        
        if (background.get() && emptySlots < 4) {
            renderer.quad(this.x, this.y, getWidth(), getHeight(), backgroundColor.get());
        }
        
        renderer.post(() -> {
            double x = this.x;
            double y = this.y;
            
            double armorX, armorY;
            
            for (int position = 0; position < 4; position++) {
                ItemStack itemStack = armor[position];
                
                if (orientation.get() == Orientation.VERTICAL) {
                    armorX = x;
                    armorY = y + position * 18 * getScale();
                } else {
                    armorX = x + position * 18 * getScale();
                    armorY = y;
                }
                
                renderer.item(itemStack, (int) armorX, (int) armorY, getScale(), (itemStack.isDamageable() && durability.get() == Durability.BAR));
                
                if (itemStack.isDamageable() && durability.get() != Durability.BAR && durability.get() != Durability.NONE) {
                    String message = switch (durability.get()) {
                        case TOTAL -> Integer.toString(itemStack.getMaxDamage() - itemStack.getDamage());
                        case PERCENTAGE ->
                            Integer.toString(Math.round(((itemStack.getMaxDamage() - itemStack.getDamage()) * 100f) / (float) itemStack.getMaxDamage()));
                        default -> "err";
                    };
                    
                    double messageWidth = renderer.textWidth(message);
                    
                    if (orientation.get() == Orientation.VERTICAL) {
                        armorX = x + 8 * getScale() - messageWidth / 2.0;
                        armorY = y + (18 * position * getScale()) + (18 * getScale() - renderer.textHeight());
                    } else {
                        armorX = x + 18 * position * getScale() + 8 * getScale() - messageWidth / 2.0;
                        armorY = y + (getHeight() - renderer.textHeight());
                    }
                    
                    TextRenderer.get().render(message, armorX, armorY, durabilityColor.get(), durabilityShadow.get());
                }
            }
        });
    }
    
    private ItemStack getItem(EquipmentSlot slot) {
        if (isInEditor()) {
            return switch (slot.getEntitySlotId()) {
                case 3 -> Items.NETHERITE_HELMET.getDefaultStack();
                case 2 -> Items.NETHERITE_CHESTPLATE.getDefaultStack();
                case 1 -> Items.NETHERITE_LEGGINGS.getDefaultStack();
                default -> Items.NETHERITE_BOOTS.getDefaultStack();
            };
        }
        
        ItemStack stack = mc.player.getEquippedStack(slot);
        return stack.isEmpty() && showEmpty.get() ? Items.BARRIER.getDefaultStack() : stack;
    }
    
    private float getScale() {
        return customScale.get() ? scale.get().floatValue() : scale.getDefaultValue().floatValue();
    }
    
    private enum Durability implements IDisplayName {
        
        NONE("None"),
        BAR("Bar"),
        TOTAL("Total"),
        PERCENTAGE("PercendisplayNamee");
        
        private final String displayName;
        
        Durability(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
    }
    
    private enum Orientation implements IDisplayName {
        
        HORIZONTAL("Horizontal"),
        VERTICAL("Vertical");
        
        private final String displayName;
        
        Orientation(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
    }
    
}
