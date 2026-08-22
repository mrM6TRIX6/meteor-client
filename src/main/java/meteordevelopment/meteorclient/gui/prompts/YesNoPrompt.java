/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.prompts;

import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.client.gui.screen.Screen;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class YesNoPrompt extends Prompt<YesNoPrompt> {
    
    private Runnable onYes = () -> {};
    private Runnable onNo = () -> {};
    
    private YesNoPrompt(Screen parent) {
        super(parent);
    }
    
    public static YesNoPrompt create() {
        return new YesNoPrompt(mc.currentScreen);
    }
    
    public static YesNoPrompt create(Screen parent) {
        return new YesNoPrompt(parent);
    }
    
    public YesNoPrompt onYes(Runnable action) {
        this.onYes = action;
        return this;
    }
    
    public YesNoPrompt onNo(Runnable action) {
        this.onNo = action;
        return this;
    }
    
    @Override
    protected void initialiseWidgets(PromptScreen screen) {
        WButton yesButton = screen.list.add(new WButton("Yes")).expandX().widget();
        yesButton.action = () -> {
            dontShowAgain(screen);
            onYes.run();
            screen.close();
        };
        
        WButton noButton = screen.list.add(new WButton("No")).expandX().widget();
        noButton.action = () -> {
            dontShowAgain(screen);
            onNo.run();
            screen.close();
        };
    }
    
}
