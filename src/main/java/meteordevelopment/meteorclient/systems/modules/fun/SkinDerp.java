/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.fun;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.settings.impl.MultiChoiceSetting;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerModelPart;

import java.util.HashSet;
import java.util.Random;
import java.util.SequencedSet;
import java.util.Set;

public class SkinDerp extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(
        new IntSetting.Builder()
            .name("Delay")
            .description("Delay in ticks.")
            .defaultValue(0)
            .min(0)
            .sliderRange(0, 20)
            .build()
    );

    private final Setting<Boolean> sync = sgGeneral.add(
        new BoolSetting.Builder()
            .name("Sync")
            .description("All model parts will be blinking synchronously.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SequencedSet<PlayerModelPart>> modelParts = sgGeneral.add(
        new MultiChoiceSetting.Builder<PlayerModelPart>()
            .name("ModelParts")
            .description("Which model parts to blink.")
            .choices(PlayerModelPart.values())
            .defaultValue(PlayerModelPart.values())
            .build()
    );

    private final Random random = new Random();

    private Set<PlayerModelPart> originalParts;
    private int timer;

    public SkinDerp() {
        super(Category.FUN, "SkinDerp", "Makes your skin blink (Requires multi-layer skin).");
    }

    @Override
    public void onActivate() {
        timer = 0;
        originalParts = new HashSet<>(mc.options.enabledPlayerModelParts);
    }

    @Override
    public void onDeactivate() {
        for (PlayerModelPart playerModelPart : PlayerModelPart.values()) {
            mc.options.setPlayerModelPart(playerModelPart, false);
        }
        for (PlayerModelPart playerModelPart : originalParts) {
            mc.options.setPlayerModelPart(playerModelPart, true);
        }
        mc.options.sendClientSettings();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (++timer > delay.get()) {
            for (PlayerModelPart part : modelParts.get()) {
                if (sync.get()) {
                    mc.options.setPlayerModelPart(part, !mc.options.isPlayerModelPartEnabled(part));
                } else {
                    mc.options.setPlayerModelPart(part, random.nextBoolean());
                }
            }

            mc.options.sendClientSettings();
            timer = 0;
        }
    }

}
