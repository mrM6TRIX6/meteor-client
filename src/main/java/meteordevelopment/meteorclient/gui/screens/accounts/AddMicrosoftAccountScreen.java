/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.accounts;

import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.accounts.MicrosoftLogin;
import meteordevelopment.meteorclient.systems.accounts.types.MicrosoftAccount;

public class AddMicrosoftAccountScreen extends AddAccountScreen {
    
    public AddMicrosoftAccountScreen(AccountsScreen parent) {
        super("Add Microsoft Account", parent);
    }
    
    @Override
    public void initWidgets() {
        String url = MicrosoftLogin.getRefreshToken(refreshToken -> {
            if (refreshToken != null) {
                MicrosoftAccount account = new MicrosoftAccount(refreshToken);
                AccountsScreen.addAccount(null, parent, account);
            }
            close();
        });
        
        add(new WLabel("Please select the account to log into in your browser."));
        add(new WLabel("If the link does not automatically open in a few seconds, copy it into your browser."));
        
        WHorizontalList list = add(new WHorizontalList()).expandX().widget();
        
        WButton copy = list.add(new WButton("Copy link")).expandX().widget();
        copy.action = () -> mc.keyboard.setClipboard(url);
        
        WButton cancel = list.add(new WButton("Cancel")).expandX().widget();
        cancel.action = () -> {
            MicrosoftLogin.stopServer();
            close();
        };
    }
    
    @Override
    public void tick() {}
    
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
    
}
