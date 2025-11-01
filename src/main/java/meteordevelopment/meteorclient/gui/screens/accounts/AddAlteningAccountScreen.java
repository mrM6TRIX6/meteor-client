/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.accounts;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.systems.accounts.impl.TheAlteningAccount;

public class AddAlteningAccountScreen extends AddAccountScreen {
    
    public AddAlteningAccountScreen(GuiTheme theme, AccountsScreen parent) {
        super(theme, "Add The Altening Account", parent);
    }
    
    @Override
    public void initWidgets() {
        WTable table = add(theme.table()).expandX().minWidth(400).widget();
        
        // Token
        table.add(theme.label("Token: "));
        WTextBox token = table.add(theme.textBox("")).expandX().widget();
        token.setFocused(true);
        table.row();
        
        // Add
        add = table.add(theme.button("Add")).expandX().widget();
        add.action = () -> {
            if (!token.get().isEmpty()) {
                AccountsScreen.addAccount(this, parent, new TheAlteningAccount(token.get()));
            }
        };
        
        enterAction = add.action;
    }
    
}
