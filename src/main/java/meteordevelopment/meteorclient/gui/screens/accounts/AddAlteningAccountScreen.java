/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.accounts;

import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.accounts.types.TheAlteningAccount;

public class AddAlteningAccountScreen extends AddAccountScreen {
    
    public AddAlteningAccountScreen(AccountsScreen parent) {
        super("Add The Altening Account", parent);
    }
    
    @Override
    public void initWidgets() {
        WTable table = add(new WTable()).expandX().minWidth(400).widget();
        
        // Token
        table.add(new WLabel("Token: "));
        WTextBox token = table.add(new WTextBox("")).expandX().widget();
        token.setFocused(true);
        table.row();
        
        // Add
        add = table.add(new WButton("Add")).expandX().widget();
        add.action = () -> {
            if (!token.get().isEmpty()) {
                AccountsScreen.addAccount(this, parent, new TheAlteningAccount(token.get()));
            }
        };
        
        enterAction = add.action;
    }
    
}
