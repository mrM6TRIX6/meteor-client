/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.EnumSetting;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InventoryUtils;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AutoHotbar extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSlots = settings.createGroup("Slots");
    
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay before switching slots again. In ticks.")
        .defaultValue(2)
        .min(0)
        .sliderMin(0)
        .sliderMax(20)
        .build()
    );
    
    private final Setting<SwitchMode> switchMode = sgGeneral.add(new EnumSetting.Builder<SwitchMode>()
        .name("switching-mode")
        .description("Mode of the slot switching.")
        .defaultValue(SwitchMode.Next)
        .build()
    );
    
    private final Setting<Boolean> slot1 = sgSlots.add(new BoolSetting.Builder()
        .name("slot-1")
        .description("Enable slot 1.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> slot2 = sgSlots.add(new BoolSetting.Builder()
        .name("slot-2")
        .description("Enable slot 2.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> slot3 = sgSlots.add(new BoolSetting.Builder()
        .name("slot-3")
        .description("Enable slot 3.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> slot4 = sgSlots.add(new BoolSetting.Builder()
        .name("slot-4")
        .description("Enable slot 4.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> slot5 = sgSlots.add(new BoolSetting.Builder()
        .name("slot-5")
        .description("Enable slot 5.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> slot6 = sgSlots.add(new BoolSetting.Builder()
        .name("slot-6")
        .description("Enable slot 6.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> slot7 = sgSlots.add(new BoolSetting.Builder()
        .name("slot-7")
        .description("Enable slot 7.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> slot8 = sgSlots.add(new BoolSetting.Builder()
        .name("slot-8")
        .description("Enable slot 8.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> slot9 = sgSlots.add(new BoolSetting.Builder()
        .name("slot-9")
        .description("Enable slot 9.")
        .defaultValue(true)
        .build()
    );
    
    private final Random random = new Random();
    
    private int timer;
    private int currentSlot;
    
    public AutoHotbar() {
        super(Categories.Player, "auto-hotbar", "Automatically swaps between slots in the hotbar.");
    }
    
    @Override
    public void onActivate() {
        timer = 0;
        currentSlot = mc.player.getInventory().getSelectedSlot();
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (++timer > delay.get()) {
            List<Integer> toggledSlots = new ArrayList<>();
            boolean[] slots = { slot1.get(), slot2.get(), slot3.get(), slot4.get(), slot5.get(), slot6.get(), slot7.get(), slot8.get(), slot9.get() };
            
            for (int i = 0; i < slots.length; i++) {
                if (slots[i]) {
                    toggledSlots.add(i);
                }
            }
            
            if (toggledSlots.isEmpty()) {
                return;
            }
            
            int slotToSwap = 0;
            
            switch (switchMode.get()) {
                
                case Next -> {
                    int currentIndex = toggledSlots.indexOf(currentSlot);
                    if (currentIndex == -1) {
                        currentIndex = 0;
                    }
                    slotToSwap = (currentIndex < toggledSlots.size() - 1) ? toggledSlots.get(currentIndex + 1) : toggledSlots.get(0);
                }
                case Previous -> {
                    int currentIndex = toggledSlots.indexOf(currentSlot);
                    if (currentIndex == -1) {
                        currentIndex = toggledSlots.size() - 1;
                    }
                    slotToSwap = (currentIndex > 0) ? toggledSlots.get(currentIndex - 1) : toggledSlots.get(toggledSlots.size() - 1);
                }
                case Random -> slotToSwap = toggledSlots.get(random.nextInt(toggledSlots.size()));
            }
            
            if (slotToSwap != currentSlot) {
                InventoryUtils.swap(slotToSwap);
                currentSlot = slotToSwap;
            }
            timer = 0;
        }
    }
    
    private enum SwitchMode {
        Next,
        Previous,
        Random
    }
    
}
