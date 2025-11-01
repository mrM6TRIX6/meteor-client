/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.fun;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.ColorListSetting;
import meteordevelopment.meteorclient.settings.impl.EnumSetting;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static java.awt.Color.HSBtoRGB;
import static java.awt.Color.RGBtoHSB;

public class RainbowArmor extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSlots = settings.createGroup("Slots");
    private final SettingGroup sgExtra = settings.createGroup("Extra");
    
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("rainbow-mode")
        .description("RGB Method.")
        .defaultValue(Mode.Default)
        .build()
    );
    
    private final Setting<List<SettingColor>> colors = sgGeneral.add(new ColorListSetting.Builder()
        .name("colors")
        .description("The colors that will be used for interpolation.")
        .defaultValue(List.of(
            new SettingColor(255, 0, 0),
            new SettingColor(255, 255, 0),
            new SettingColor(0, 255, 0),
            new SettingColor(0, 255, 255),
            new SettingColor(0, 0, 255),
            new SettingColor(255, 0, 255)))
        .build()
    );
    
    private final Setting<Integer> speed = sgGeneral.add(new IntSetting.Builder()
        .name("speed")
        .description("WARNING: High speeds might crash the game!")
        .defaultValue(5)
        .min(1)
        .sliderMax(20)
        .build()
    );
    
    private final Setting<Boolean> glowing = sgGeneral.add(new BoolSetting.Builder()
        .name("glowing")
        .description("Add enchantment glow override component for armor to create a glowing effect.")
        .defaultValue(false)
        .build()
    );
    
    // Helmet
    private final Setting<Boolean> enableHelmet = sgSlots.add(new BoolSetting.Builder()
        .name("helmet")
        .description("Enable Helmet.")
        .defaultValue(true)
        .onChanged(v -> {
            if (isActive() && !v) {
                clearSlot(5);
            }
        })
        .build()
    );
    
    // Chestplate
    private final Setting<Boolean> enableChestplate = sgSlots.add(new BoolSetting.Builder()
        .name("chestplate")
        .description("Enable Chestplate.")
        .defaultValue(true)
        .onChanged(v -> {
            if (isActive() && !v) {
                clearSlot(6);
            }
        })
        .build()
    );
    
    // Leggings
    private final Setting<Boolean> enableLeggings = sgSlots.add(new BoolSetting.Builder()
        .name("leggings")
        .description("Enable Leggings.")
        .defaultValue(true)
        .onChanged(v -> {
            if (isActive() && !v) {
                clearSlot(7);
            }
        })
        .build()
    );
    
    // Boots
    private final Setting<Boolean> enableBoots = sgSlots.add(new BoolSetting.Builder()
        .name("boots")
        .description("Enable Boots.")
        .defaultValue(true)
        .onChanged(v -> {
            if (isActive() && !v) {
                clearSlot(8);
            }
        })
        .build()
    );
    
    // Extra
    private final Setting<Boolean> toggleOnLog = sgExtra.add(new BoolSetting.Builder()
        .name("toggle-on-log")
        .description("Disables when you disconnect from a server.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> blockSound = sgExtra.add(new BoolSetting.Builder()
        .name("block-sound")
        .description("Blocks armor equip sound.")
        .defaultValue(true)
        .build()
    );
    
    private final Timer timer = new Timer();
    
    private int linearI;
    
    public RainbowArmor() {
        super(Categories.Fun, "rainbow-armor", "Gives you Rainbow Leather Armor with various modes.");
    }
    
    @Override
    public void onActivate() {
        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode only.");
            toggle();
        }
        linearI = 0;
    }
    
    @Override
    public void onDeactivate() {
        if (!mc.player.getAbilities().creativeMode) {
            return;
        }
        
        if (enableHelmet.get()) {
            clearSlot(5);
        }
        if (enableChestplate.get()) {
            clearSlot(6);
        }
        if (enableLeggings.get()) {
            clearSlot(7);
        }
        if (enableBoots.get()) {
            clearSlot(8);
        }
    }
    
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (toggleOnLog.get()) {
            toggle();
        }
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode only.");
            toggle();
            return;
        }
        
        if (colors.get().isEmpty()) {
            error("List of colors cannot be empty.");
            toggle();
            return;
        }
        
        switch (mode.get()) {
            case Default -> methodDefault();
            case Linear -> methodLinear();
        }
    }
    
    @EventHandler
    private void onPlaySound(PlaySoundEvent event) {
        if (event.sound.getId().toString().equals("minecraft:item.armor.equip_leather") && blockSound.get()) {
            event.cancel();
        }
    }
    
    private void methodDefault() {
        DyedColorComponent dye = new DyedColorComponent(generateRGBMath());
        
        if (enableHelmet.get() && !Modules.get().isActive(CustomHead.class)) {
            mc.interactionManager.clickCreativeStack(getItemStack(Items.LEATHER_HELMET, dye), 5);
        }
        if (enableChestplate.get()) {
            mc.interactionManager.clickCreativeStack(getItemStack(Items.LEATHER_CHESTPLATE, dye), 6);
        }
        if (enableLeggings.get()) {
            mc.interactionManager.clickCreativeStack(getItemStack(Items.LEATHER_LEGGINGS, dye), 7);
        }
        if (enableBoots.get()) {
            mc.interactionManager.clickCreativeStack(getItemStack(Items.LEATHER_BOOTS, dye), 8);
        }
    }
    
    private void methodLinear() {
        DyedColorComponent dye = new DyedColorComponent(generateRGBMath());
        
        if (enableHelmet.get() && linearI == 0 && !Modules.get().isActive(CustomHead.class)) {
            mc.interactionManager.clickCreativeStack(getItemStack(Items.LEATHER_HELMET, dye), 5);
        }
        if (enableChestplate.get() && linearI == 1) {
            mc.interactionManager.clickCreativeStack(getItemStack(Items.LEATHER_CHESTPLATE, dye), 6);
        }
        if (enableLeggings.get() && linearI == 2) {
            mc.interactionManager.clickCreativeStack(getItemStack(Items.LEATHER_LEGGINGS, dye), 7);
        }
        if (enableBoots.get() && linearI == 3) {
            mc.interactionManager.clickCreativeStack(getItemStack(Items.LEATHER_BOOTS, dye), 8);
        }
        
        if (++linearI >= 4) {
            linearI = 0;
        }
    }
    
    private ItemStack getItemStack(Item item, DyedColorComponent dye) {
        ItemStack itemStack = new ItemStack(item);
        itemStack.set(DataComponentTypes.DYED_COLOR, dye);
        if (glowing.get()) {
            itemStack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return itemStack;
    }
    
    private void clearSlot(int slotId) {
        mc.interactionManager.clickCreativeStack(ItemStack.EMPTY, slotId);
        
        try {
            // In protocol version 1.20.2+ you need to manually update the slot on the client side
            // A delay of 65 ms is necessary, since immediately after the module is turned off, the armor is still being
            // updated for some time, and it becomes phantom.
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mc.player.getInventory().setStack(44 - slotId, ItemStack.EMPTY);
                }
            }, 65);
        } catch (Exception e) {
            error(e.getMessage());
        }
    }
    
    private int generateRGBMath() {
        long time = System.currentTimeMillis();
        
        if (colors.get().size() == 1) {
            return colors.get().getFirst().toTextColor().getRgb() & 0xFFFFFF;
        }
        
        return generateGradientHSL(time);
    }
    
    private int generateGradientHSL(long time) {
        double totalDuration = 10000.0 / speed.get();
        double progress = (time % totalDuration) / totalDuration;
        
        int colorCount = colors.get().size();
        double segment = progress * colorCount;
        int index1 = (int) segment % colorCount;
        int index2 = (index1 + 1) % colorCount;
        double segmentProgress = segment - (int) segment;
        
        return interpolateColorsHSL(colors.get().get(index1), colors.get().get(index2), segmentProgress);
    }
    
    private int interpolateColorsHSL(Color start, Color end, double progress) {
        float[] startHSL = new float[3];
        float[] endHSL = new float[3];
        
        
        RGBtoHSB(start.r, start.g, start.b, startHSL);
        RGBtoHSB(end.r, end.g, end.b, endHSL);
        
        float h = interpolateHue(startHSL[0], endHSL[0], progress);
        float s = (float) (startHSL[1] + (endHSL[1] - startHSL[1]) * progress);
        float b = (float) (startHSL[2] + (endHSL[2] - startHSL[2]) * progress);
        
        return HSBtoRGB(h, s, b) & 0xFFFFFF;
    }
    
    private float interpolateHue(float startHue, float endHue, double progress) {
        float diff = endHue - startHue;
        if (Math.abs(diff) > 0.5f) {
            if (diff > 0) {
                endHue -= 1.0f;
            } else {
                endHue += 1.0f;
            }
        }
        float result = (float) (startHue + (endHue - startHue) * progress);
        return (result + 1.0f) % 1.0f;
    }
    
    private enum Mode {
        Default,
        Linear
    }
    
}
