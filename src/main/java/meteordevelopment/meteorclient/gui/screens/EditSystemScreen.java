package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.Settings;

public abstract class EditSystemScreen<T> extends WindowScreen {
    
    private WContainer settingsContainer;
    protected final T value;
    protected final boolean isNew;
    private final Runnable reload;
    
    public EditSystemScreen(T value, Runnable reload) {
        super(value == null ? "New" : "Edit");
        
        this.isNew = value == null;
        this.value = isNew ? create() : value;
        this.reload = reload;
    }
    
    @Override
    public void initWidgets() {
        settingsContainer = add(new WVerticalList()).expandX().minWidth(400).widget();
        settingsContainer.add(DefaultSettingsWidgetFactory.settings(getSettings())).expandX();
        
        add(new WHorizontalSeparator()).expandX();
        
        WButton done = add(new WButton(isNew ? "Create" : "Save")).expandX().widget();
        done.action = () -> {
            if (save()) {
                close();
            }
        };
        
        enterAction = done.action;
    }
    
    @Override
    public void tick() {
        getSettings().tick(settingsContainer);
    }
    
    @Override
    protected void onClosed() {
        if (reload != null) {
            reload.run();
        }
    }
    
    public abstract T create();
    
    public abstract boolean save();
    
    public abstract Settings getSettings();
    
}
