/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.accounts;

import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.accounts.types.SessionAccount;

public class AddSessionAccountScreen extends AddAccountScreen {
    
    public AddSessionAccountScreen(AccountsScreen parent) {
        super("Add Session Account", parent);
    }
    
    @Override
    public void initWidgets() {
        WTable t = add(new WTable()).widget();
        
        // Access token
        t.add(new WLabel("Access Token: "));
        WTextBox token = t.add(new WTextBox("")).minWidth(400).expandX().widget();
        token.setFocused(true);
        t.row();
        
        // Add
        add = t.add(new WButton("Add")).expandX().widget();
        add.action = () -> {
            if (!token.get().isEmpty()) {
                SessionAccount account = new SessionAccount(token.get());
                AccountsScreen.addAccount(this, parent, account);
            }
        };
        
        enterAction = add.action;
    }
    
}