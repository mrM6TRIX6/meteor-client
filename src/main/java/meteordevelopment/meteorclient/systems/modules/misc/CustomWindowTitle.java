/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import org.meteordev.starscript.Script;

public class CustomWindowTitle extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<String> title = sgGeneral.add(
        new StringSetting.Builder()
            .name("Title")
            .description("The text it displays in the window title. Starscript supported.")
            .defaultValue("Minecraft {mc_version} - {player}")
            .renderer(StarscriptTextBoxRenderer.class)
            .onModuleActivated(setting -> mc.updateWindowTitle())
            .onChanged(value -> mc.updateWindowTitle())
            .build()
    );
    
    public CustomWindowTitle() {
        super(Category.MISC, "CustomWindowTitle", "Show custom text in the window title.");
    }
    
    public String title(String original) {
        if (!isActive()) {
            return original;
        }
        
        String customTitle = title.get();
        Script script = MeteorStarscript.compile(customTitle);
        
        if (script != null) {
            String title = MeteorStarscript.run(script);
            if (title != null) {
                customTitle = title;
            }
        }
        
        return customTitle;
    }
    
}

