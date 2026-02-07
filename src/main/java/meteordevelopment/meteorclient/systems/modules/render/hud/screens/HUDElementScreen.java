/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud.screens;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.EnumSetting;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDElement;
import meteordevelopment.meteorclient.systems.modules.render.hud.XAnchor;
import meteordevelopment.meteorclient.systems.modules.render.hud.YAnchor;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import net.minecraft.client.gui.DrawContext;

import static meteordevelopment.meteorclient.utils.render.RenderUtils.getWindowWidth;

public class HUDElementScreen extends WindowScreen {
    
    private final HUDElement element;
    
    private WContainer settingsC1, settingsC2;
    private final Settings settings;
    
    public HUDElementScreen(GuiTheme theme, HUDElement element) {
        super(theme, element.info.title);
        
        this.element = element;
        
        settings = new Settings();
        SettingGroup sg = settings.createGroup("Anchors");
        sg.add(new BoolSetting.Builder()
            .name("auto-anchors")
            .description("Automatically assigns anchors based on the position.")
            .defaultValue(true)
            .onModuleActivated(booleanSetting -> booleanSetting.set(element.autoAnchors))
            .onChanged(aBoolean -> {
                if (aBoolean) {
                    element.box.updateAnchors();
                }
                element.autoAnchors = aBoolean;
            })
            .build()
        );
        sg.add(new EnumSetting.Builder<XAnchor>()
            .name("x-anchor")
            .description("Horizontal anchor.")
            .defaultValue(XAnchor.LEFT)
            .visible(() -> !element.autoAnchors)
            .onModuleActivated(xAnchorSetting -> xAnchorSetting.set(element.box.xAnchor))
            .onChanged(element.box::setXAnchor)
            .build()
        );
        sg.add(new EnumSetting.Builder<YAnchor>()
            .name("y-anchor")
            .description("Vertical anchor.")
            .defaultValue(YAnchor.TOP)
            .visible(() -> !element.autoAnchors)
            .onModuleActivated(yAnchorSetting -> yAnchorSetting.set(element.box.yAnchor))
            .onChanged(element.box::setYAnchor)
            .build()
        );
    }
    
    @Override
    public void initWidgets() {
        // Description
        add(theme.label(element.info.description, getWindowWidth() / 2.0));
        
        // Settings
        if (element.settings.sizeGroups() > 0) {
            element.settings.onActivated();
            
            settingsC1 = add(theme.verticalList()).expandX().widget();
            settingsC1.add(theme.settings(element.settings)).expandX();
        }
        
        // Anchors
        settings.onActivated();
        
        settingsC2 = add(theme.verticalList()).expandX().widget();
        settingsC2.add(theme.settings(settings)).expandX();
        
        add(theme.horizontalSeparator()).expandX();
        
        // Custom widget
        WWidget widget = element.getWidget(theme);
        
        if (widget != null) {
            Cell<WWidget> cell = add(widget);
            if (widget instanceof WContainer) {
                cell.expandX();
            }
            add(theme.horizontalSeparator()).expandX();
        }
        
        // Bottom
        WHorizontalList bottomList = add(theme.horizontalList()).expandX().widget();
        
        // Active
        bottomList.add(theme.label("Active:"));
        WCheckbox active = bottomList.add(theme.checkbox(element.isActive())).widget();
        active.action = () -> {
            if (element.isActive() != active.checked) {
                element.toggle();
            }
        };
        
        // Remove
        WMinus remove = bottomList.add(theme.minus()).expandCellX().right().widget();
        remove.action = () -> {
            element.remove();
            close();
        };
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (settingsC1 != null) {
            element.settings.tick(settingsC1, theme);
        }
        
        settings.tick(settingsC2, theme);
    }
    
    @Override
    protected void onRenderBefore(DrawContext drawContext, float delta) {
        HUDEditorScreen.renderElements(drawContext);
    }
    
    @Override
    public boolean toClipboard() {
        return JsonUtils.toClipboard(element.info.title, element.toJson());
    }
    
    @Override
    public boolean fromClipboard() {
        JsonObject clipboard = JsonUtils.fromClipboard(element.toJson());
        
        if (clipboard != null) {
            element.fromJson(clipboard);
            return true;
        }
        
        return false;
    }
    
}
