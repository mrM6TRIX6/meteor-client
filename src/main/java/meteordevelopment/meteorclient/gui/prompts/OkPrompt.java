/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.prompts;

import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.client.gui.screen.Screen;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class OkPrompt extends Prompt<OkPrompt> {
    
    private Runnable onOk = () -> {};
    
    private OkPrompt(Screen parent) {
        super(parent);
    }
    
    public static OkPrompt create() {
        return new OkPrompt(mc.currentScreen);
    }
    
    public static OkPrompt create(Screen parent) {
        return new OkPrompt(parent);
    }
    
    public OkPrompt onOk(Runnable action) {
        this.onOk = action;
        return this;
    }
    
    @Override
    protected void initialiseWidgets(PromptScreen screen) {
        WButton okButton = screen.list.add(new WButton("Ok")).expandX().widget();
        okButton.action = () -> {
            dontShowAgain(screen);
            onOk.run();
            screen.close();
        };
    }
    
}
