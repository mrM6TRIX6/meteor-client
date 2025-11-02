/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.config.Setting;
import meteordevelopment.meteorclient.config.SettingGroup;
import meteordevelopment.meteorclient.config.types.BoolSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class MultiActions extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> attackingEntities = sgGeneral.add(new BoolSetting.Builder()
        .name("attacking-entities")
        .description("Lets you attack entities while using an item.")
        .defaultValue(true)
        .build()
    );
    
    public MultiActions() {
        super(Categories.Player, "multi-actions", "Lets you use items and attack at the same time.");
    }
    
    public boolean attackingEntities() {
        return isActive() && attackingEntities.get();
    }
    
}
