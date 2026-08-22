package meteordevelopment.meteorclient.gui.prompts;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.systems.clientsettings.ClientSettings;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@SuppressWarnings("unchecked") // Cant instantiate a Prompt directly so this is fine
public abstract class Prompt<T> {
    protected final Screen parent;
    
    protected String title = "";
    protected final List<String> messages = new ArrayList<>();
    protected boolean dontShowAgainCheckboxVisible = true;
    protected String id = null;
    
    protected Prompt(Screen parent) {
        this.parent = parent;
    }
    
    public T title(String title) {
        this.title = title;
        return (T) this;
    }
    
    public T message(String message) {
        this.messages.add(message);
        return (T) this;
    }
    
    public T message(String message, Object... args) {
        this.messages.add(String.format(message, args));
        return (T) this;
    }
    
    public T dontShowAgainCheckboxVisible(boolean visible) {
        this.dontShowAgainCheckboxVisible = visible;
        return (T) this;
    }
    
    public T id(String from) {
        this.id = from;
        return (T) this;
    }
    
    public boolean show() {
        if (id != null && ClientSettings.get().dontShowAgainPrompts.contains(id)) {
            return false;
        }
        
        if (!RenderSystem.isOnRenderThread()) {
            mc.execute(() -> mc.setScreen(new PromptScreen()));
        } else {
            mc.setScreen(new PromptScreen());
        }
        
        return true;
    }
    
    protected void dontShowAgain(PromptScreen screen) {
        if (screen.dontShowAgainCheckbox != null && screen.dontShowAgainCheckbox.checked && id != null) {
            ClientSettings.get().dontShowAgainPrompts.add(id);
        }
    }
    
    protected abstract void initialiseWidgets(PromptScreen screen);
    
    protected class PromptScreen extends WindowScreen {
        
        protected WCheckbox dontShowAgainCheckbox;
        protected WHorizontalList list;
        
        public PromptScreen() {
            super(Prompt.this.title);
            
            this.parent = Prompt.this.parent;
        }
        
        @Override
        public void initWidgets() {
            for (String line : messages) {
                add(new WLabel(line)).expandX();
            }
            add(new WHorizontalSeparator()).expandX();
            
            if (dontShowAgainCheckboxVisible) {
                WHorizontalList checkboxContainer = add(new WHorizontalList()).expandX().widget();
                dontShowAgainCheckbox = checkboxContainer.add(new WCheckbox(false)).widget();
                checkboxContainer.add(new WLabel("Don't show this again.")).expandX();
            } else {
                dontShowAgainCheckbox = null;
            }
            
            list = add(new WHorizontalList()).expandX().widget();
            
            initialiseWidgets(this);
        }
        
    }
    
}
