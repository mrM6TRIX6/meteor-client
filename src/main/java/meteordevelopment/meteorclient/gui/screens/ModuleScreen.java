/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.ActiveModulesChangedEvent;
import meteordevelopment.meteorclient.events.meteor.ModuleBindChangedEvent;
import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WKeybind;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WFavorite;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import meteordevelopment.orbit.EventHandler;

import static meteordevelopment.meteorclient.renderer.RenderUtils.getWindowWidth;

public class ModuleScreen extends WindowScreen {
    
    private final Module module;
    
    private WContainer settingsContainer;
    private WKeybind keybind;
    private WCheckbox active;
    
    public ModuleScreen(Module module) {
        super(new WFavorite(module.favorite), module.name);
        ((WFavorite) window.icon).action = () -> module.favorite = ((WFavorite) window.icon).checked;
        
        this.module = module;
    }
    
    @Override
    public void initWidgets() {
        // Description
        add(new WLabel(module.description, getWindowWidth() / 2.0));
        
        // Settings
        if (!module.settings.groups.isEmpty()) {
            settingsContainer = add(new WVerticalList()).expandX().widget();
            settingsContainer.add(DefaultSettingsWidgetFactory.settings(module.settings)).expandX();
        }
        
        // Custom widget
        WWidget widget = module.getWidget();
        
        if (widget != null) {
            add(new WHorizontalSeparator()).expandX();
            Cell<WWidget> cell = add(widget);
            if (widget instanceof WContainer) {
                cell.expandX();
            }
        }
        
        // Bind
        WSection section = add(new WSection("Bind", true)).expandX().widget();
        
        // Keybind
        WHorizontalList bind = section.add(new WHorizontalList()).expandX().widget();
        
        bind.add(new WLabel("Bind: "));
        keybind = bind.add(new WKeybind(module.keybind)).expandX().widget();
        keybind.actionOnSet = () -> Modules.get().setModuleToBind(module);
        
        WButton reset = bind.add(new WButton(GuiConstants.RESET)).expandCellX().right().widget();
        reset.action = keybind::resetBind;
        reset.tooltip = "Reset";
        
        // Toggle on bind release
        WHorizontalList tobr = section.add(new WHorizontalList()).widget();
        
        tobr.add(new WLabel("Toggle on bind release: "));
        WCheckbox tobrC = tobr.add(new WCheckbox(module.toggleOnBindRelease)).widget();
        tobrC.action = () -> module.toggleOnBindRelease = tobrC.checked;
        
        // Chat feedback
        WHorizontalList cf = section.add(new WHorizontalList()).widget();
        
        cf.add(new WLabel("Chat Feedback: "));
        WCheckbox cfC = cf.add(new WCheckbox(module.chatFeedback)).widget();
        cfC.action = () -> module.chatFeedback = cfC.checked;
        
        add(new WHorizontalSeparator()).expandX();
        
        // Bottom
        WHorizontalList bottom = add(new WHorizontalList()).expandX().widget();
        
        // Active
        bottom.add(new WLabel("Active: "));
        active = bottom.add(new WCheckbox(module.isActive())).expandCellX().widget();
        active.action = () -> {
            if (module.isActive() != active.checked) {
                module.toggle();
            }
        };
        
        if (module.addon != null && module.addon != MeteorClient.ADDON) {
            bottom.add(new WLabel("From: ")).right().widget();
            bottom.add(new WLabel(module.addon.name).color(GuiConstants.TEXT_SECONDARY)).right().widget();
        }
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return !Modules.get().isBinding();
    }
    
    @Override
    public void tick() {
        super.tick();
        
        module.settings.tick(settingsContainer);
    }
    
    @EventHandler
    private void onModuleBindChanged(ModuleBindChangedEvent event) {
        keybind.reset();
    }
    
    @EventHandler
    private void onActiveModulesChanged(ActiveModulesChangedEvent event) {
        this.active.checked = module.isActive();
    }
    
    @Override
    public boolean toClipboard() {
        return JsonUtils.toClipboard(module.name, module.toJson());
    }
    
    @Override
    public boolean fromClipboard() {
        JsonObject clipboard = JsonUtils.fromClipboard(module.toJson());
        
        if (clipboard != null) {
            module.fromJson(clipboard);
            return true;
        }
        
        return false;
    }
    
}
