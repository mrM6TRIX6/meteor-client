/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.spider;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.settings.impl.ModeEnumChoiceSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.movement.spider.modes.Polar;
import meteordevelopment.meteorclient.systems.modules.movement.spider.modes.Vanilla;
import meteordevelopment.meteorclient.utils.misc.IDisplayName;

public class Spider extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Mode> mode = sgGeneral.add(new ModeEnumChoiceSetting.Builder<Mode, Spider>()
        .name("mode")
        .description("Spider mode.")
        .defaultValue(Mode.VANILLA)
        .build()
    );
    
    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("climb-speed")
        .description("The speed you go up blocks.")
        .defaultValue(0.2)
        .min(0.0)
        .visible(() -> mode.get() == Mode.VANILLA)
        .build()
    );
    
    private final Setting<Double> jumpHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("jump-height")
        .description("Jump height.")
        .defaultValue(0.55)
        .min(0.0)
        .sliderRange(0.42, 0.6)
        .visible(() -> mode.get() == Mode.POLAR)
        .build()
    );
    
    public Spider() {
        super(Categories.MOVEMENT, "Spider", "Allows you to climb walls like a spider.");
    }
    
    public double speed() {
        return speed.get();
    }
    
    public double jumpHeight() {
        return jumpHeight.get();
    }
    
    private enum Mode implements IDisplayName, ModeEnumChoiceSetting.IModeImpl<Spider> {
        
        VANILLA("Vanilla", new Vanilla()),
        POLAR("Polar", new Polar());
        
        private final String displayName;
        private final ModeEnumChoiceSetting.ModeImpl<Spider> modeImpl;
        
        Mode(String displayName, ModeEnumChoiceSetting.ModeImpl<Spider> modeImpl) {
            this.displayName = displayName;
            this.modeImpl = modeImpl;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
        @Override
        public ModeEnumChoiceSetting.ModeImpl<Spider> getModeImpl() {
            return modeImpl;
        }
        
    }
    
}
