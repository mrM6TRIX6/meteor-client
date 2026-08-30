/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import net.minecraft.client.gui.DrawContext;

public class HUDElementScreen extends WindowScreen {

    private final HUDElement element;

    private WContainer settingsContainer;

    public HUDElementScreen(HUDElement element) {
        super(element.getName());
        this.element = element;
    }

    @Override
    public void initWidgets() {
        if (!element.settings.groups.isEmpty()) {
            element.settings.onActivated();
            settingsContainer = add(new WVerticalList()).expandX().widget();
            settingsContainer.add(DefaultSettingsWidgetFactory.settings(element.settings)).expandX();
        }

        WWidget widget = element.getWidget();
        if (widget != null) {
            add(new WHorizontalSeparator()).expandX();
            Cell<WWidget> cell = add(widget);
            if (widget instanceof WContainer) {
                cell.expandX();
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        element.settings.tick(settingsContainer);
    }

    @Override
    protected void onRenderBefore(DrawContext context, float delta) {
        // Keeps the elements on screen behind the settings window so changes are visible as they are made. Only draws
        // them without a world, for the same reason HUDEditorScreen does.
        HUD.get().renderPreview(context);
    }

    @Override
    public boolean toClipboard() {
        return JsonUtils.toClipboard(element.getName(), element.toJson());
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
