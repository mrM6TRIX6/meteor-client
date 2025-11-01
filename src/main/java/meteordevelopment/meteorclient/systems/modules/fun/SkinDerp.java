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
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerModelPart;

import java.util.*;

public class SkinDerp extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgParts = settings.createGroup("Parts");
    
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in ticks.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );
    
    private final Setting<Boolean> sync = sgGeneral.add(new BoolSetting.Builder()
        .name("sync")
        .description("All model parts will be blinking synchronously.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> hat = sgParts.add(new BoolSetting.Builder()
        .name("hat")
        .description("Enable hat.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> jacket = sgParts.add(new BoolSetting.Builder()
        .name("jacket")
        .description("Enable jacket.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> leftPants = sgParts.add(new BoolSetting.Builder()
        .name("left-pants")
        .description("Enable left pants.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> rightPants = sgParts.add(new BoolSetting.Builder()
        .name("right-pants")
        .description("Enable right pants.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> leftSleeve = sgParts.add(new BoolSetting.Builder()
        .name("left-sleeve")
        .description("Enable left sleeve.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> rightSleeve = sgParts.add(new BoolSetting.Builder()
        .name("right-sleeve")
        .description("Enable right sleeve.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> cape = sgParts.add(new BoolSetting.Builder()
        .name("cape")
        .description("Enable cape.")
        .defaultValue(true)
        .build()
    );
    
    private final Random random = new Random();
    
    private Set<PlayerModelPart> originalParts;
    private int timer;
    
    public SkinDerp() {
        super(Categories.Fun, "skin-derp", "Makes your skin blink (Requires multi-layer skin).");
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
            Map<PlayerModelPart, Boolean> partsMap = new HashMap<>();
            
            partsMap.put(PlayerModelPart.HAT, hat.get());
            partsMap.put(PlayerModelPart.JACKET, jacket.get());
            partsMap.put(PlayerModelPart.LEFT_PANTS_LEG, leftPants.get());
            partsMap.put(PlayerModelPart.RIGHT_PANTS_LEG, rightPants.get());
            partsMap.put(PlayerModelPart.LEFT_SLEEVE, leftSleeve.get());
            partsMap.put(PlayerModelPart.RIGHT_SLEEVE, rightSleeve.get());
            partsMap.put(PlayerModelPart.CAPE, cape.get());
            
            for (Map.Entry<PlayerModelPart, Boolean> entry : partsMap.entrySet()) {
                PlayerModelPart part = entry.getKey();
                boolean isEnabled = entry.getValue();
                
                if (isEnabled) {
                    if (sync.get()) {
                        mc.options.setPlayerModelPart(part, !mc.options.isPlayerModelPartEnabled(part));
                    } else {
                        mc.options.setPlayerModelPart(part, random.nextBoolean());
                    }
                }
            }
            mc.options.sendClientSettings();
            timer = 0;
        }
    }
    
}
