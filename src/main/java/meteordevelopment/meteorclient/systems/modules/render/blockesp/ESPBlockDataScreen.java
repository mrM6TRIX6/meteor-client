/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.blockesp;

import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.renderer.engine.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.impl.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.block.Block;
import org.jetbrains.annotations.Nullable;

public class ESPBlockDataScreen extends WindowScreen {
    
    private final ESPBlockData blockData;
    private final Setting<?> setting;
    private final @Nullable Runnable firstChangeConsumer;
    
    public ESPBlockDataScreen(ESPBlockData blockData, Block block, BlockDataSetting<ESPBlockData> setting) {
        this(blockData, setting, () -> setting.get().put(block, blockData));
    }
    
    public ESPBlockDataScreen(ESPBlockData blockData, GenericSetting<ESPBlockData> setting) {
        this(blockData, setting, null);
    }
    
    private ESPBlockDataScreen(ESPBlockData blockData, Setting<?> setting, @Nullable Runnable firstChangeConsumer) {
        super("Configure Block");
        
        this.blockData = blockData;
        this.setting = setting;
        this.firstChangeConsumer = firstChangeConsumer;
    }
    
    @Override
    public void initWidgets() {
        Settings settings = new Settings();
        SettingGroup sgGeneral = settings.getDefaultGroup();
        SettingGroup sgTracer = settings.createGroup("Tracer");
        
        sgGeneral.add(new EnumChoiceSetting.Builder<ShapeMode>()
            .name("ShapeMode")
            .description("How the shape is rendered.")
            .defaultValue(ShapeMode.LINES)
            .onModuleActivated(shapeModeSetting -> shapeModeSetting.set(blockData.shapeMode))
            .onChanged(shapeMode -> {
                if (blockData.shapeMode != shapeMode) {
                    blockData.shapeMode = shapeMode;
                    onChanged();
                }
            })
            .build()
        );
        
        sgGeneral.add(new ColorSetting.Builder()
            .name("LineColor")
            .description("Color of lines.")
            .defaultValue(new Color(0, 255, 200))
            .onModuleActivated(settingColorSetting -> settingColorSetting.get().set(blockData.lineColor))
            .onChanged(settingColor -> {
                if (!blockData.lineColor.equals(settingColor)) {
                    blockData.lineColor.set(settingColor);
                    onChanged();
                }
            })
            .build()
        );
        
        sgGeneral.add(new ColorSetting.Builder()
            .name("SideColor")
            .description("Color of sides.")
            .defaultValue(new Color(0, 255, 200, 25))
            .onModuleActivated(settingColorSetting -> settingColorSetting.get().set(blockData.sideColor))
            .onChanged(settingColor -> {
                if (!blockData.sideColor.equals(settingColor)) {
                    blockData.sideColor.set(settingColor);
                    onChanged();
                }
            })
            .build()
        );
        
        sgTracer.add(new BoolSetting.Builder()
            .name("Tracer")
            .description("If tracer line is allowed to this block.")
            .defaultValue(true)
            .onModuleActivated(booleanSetting -> booleanSetting.set(blockData.tracer))
            .onChanged(aBoolean -> {
                if (blockData.tracer != aBoolean) {
                    blockData.tracer = aBoolean;
                    onChanged();
                }
            })
            .build()
        );
        
        sgTracer.add(new ColorSetting.Builder()
            .name("TracerColor")
            .description("Color of tracer line.")
            .defaultValue(new Color(0, 255, 200, 125))
            .onModuleActivated(settingColorSetting -> settingColorSetting.get().set(blockData.tracerColor))
            .onChanged(settingColor -> {
                if (!blockData.tracerColor.equals(settingColor)) {
                    blockData.tracerColor.set(settingColor);
                    onChanged();
                }
            })
            .build()
        );
        
        settings.onActivated();
        add(DefaultSettingsWidgetFactory.settings(settings)).expandX();
    }
    
    private void onChanged() {
        if (!blockData.isChanged() && firstChangeConsumer != null) {
            firstChangeConsumer.run();
        }
        
        setting.onChanged();
        blockData.changed();
    }
    
}