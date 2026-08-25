/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.EnumChoiceSetting;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.settings.impl.MultiChoiceSetting;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.name.Namer;
import meteordevelopment.meteorclient.utils.player.InventoryUtils;
import meteordevelopment.orbit.EventHandler;

import java.util.List;
import java.util.Random;
import java.util.SequencedSet;

public class AutoHotbar extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(
        new IntSetting.Builder()
            .name("Delay")
            .description("The delay before switching slots again. In ticks.")
            .defaultValue(2)
            .min(0)
            .sliderMin(0)
            .sliderMax(20)
            .build()
    );

    private final Setting<SwitchMode> switchMode = sgGeneral.add(
        new EnumChoiceSetting.Builder<SwitchMode>()
            .name("SwitchingMode")
            .description("Mode of the slot switching.")
            .defaultValue(SwitchMode.NEXT)
            .build()
    );

    private final Setting<SequencedSet<Integer>> slots = sgGeneral.add(
        new MultiChoiceSetting.Builder<Integer>()
            .name("Slots")
            .description("Enabled slots to switch.")
            .choices(0, 1, 2, 3, 4, 5, 6, 7, 8)
            .namer(Namer.of(slot -> "Slot" + (slot + 1), slot -> "Slot " + (slot + 1)))
            .defaultValue(0, 1, 2, 3, 4, 5, 6, 7, 8)
            .build()
    );

    private final Setting<Boolean> ignoreEmptySlots = sgGeneral.add(
        new BoolSetting.Builder()
            .name("IgnoreEmptySlots")
            .description("Skips enabled slots that hold no item.")
            .defaultValue(false)
            .build()
    );

    private final Random random = new Random();

    private int timer;

    public AutoHotbar() {
        super(Category.PLAYER, "AutoHotbar", "Automatically swaps between slots in the hotbar.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (++timer <= delay.get()) {
            return;
        }

        timer = 0;

        List<Integer> available = availableSlots();

        if (available.isEmpty()) {
            return;
        }
        
        int currentSlot = mc.player.getInventory().getSelectedSlot();
        int currentIndex = available.indexOf(currentSlot);

        int slotToSwap = switch (switchMode.get()) {
            case NEXT -> available.get(currentIndex == -1 ? 0 : (currentIndex + 1) % available.size());
            case PREVIOUS -> available.get(currentIndex == -1 ? available.size() - 1 : Math.floorMod(currentIndex - 1, available.size()));
            case RANDOM -> randomSlot(available, currentIndex);
        };

        if (slotToSwap != currentSlot) {
            InventoryUtils.swap(slotToSwap);
        }
    }
    
    private List<Integer> availableSlots() {
        boolean skipEmpty = ignoreEmptySlots.get();

        return slots.get().stream()
            .filter(slot -> !skipEmpty || !mc.player.getInventory().getStack(slot).isEmpty())
            .sorted()
            .toList();
    }
    
    private int randomSlot(List<Integer> available, int currentIndex) {
        if (currentIndex == -1) {
            return available.get(random.nextInt(available.size()));
        }
        if (available.size() == 1) {
            return available.getFirst();
        }

        int index = random.nextInt(available.size() - 1);

        return available.get(index < currentIndex ? index : index + 1);
    }

    private enum SwitchMode {
        
        NEXT,
        PREVIOUS,
        RANDOM
        
    }

}
