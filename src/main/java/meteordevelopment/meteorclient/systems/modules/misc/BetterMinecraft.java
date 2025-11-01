/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class BetterMinecraft extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> paragraphs = sgGeneral.add(new BoolSetting.Builder()
        .name("paragraphs")
        .description("Makes you able to write §. (normally \"illegal\" character).")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> unicodeArguments = sgGeneral.add(new BoolSetting.Builder()
        .name("unicode-arguments")
        .description("Allows you to use non-English characters in command arguments.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> noResourcePacksWarnings = sgGeneral.add(new BoolSetting.Builder()
        .name("no-resource-packs-warnings")
        .description("Disable warnings for outdated resource packs.")
        .defaultValue(true)
        .build()
    );
    
    public BetterMinecraft() {
        super(Categories.Misc, "better-minecraft", "Various simple improvements to enhance your gaming experience.");
    }
    
    public boolean paragraphs() {
        return isActive() && paragraphs.get();
    }
    
    public boolean unicodeArguments() {
        return isActive() && unicodeArguments.get();
    }
    
    public boolean noResourcePacksWarnings() {
        return isActive() && noResourcePacksWarnings.get();
    }
    
}
