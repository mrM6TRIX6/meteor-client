/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.accounts;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.accounts.Accounts;
import meteordevelopment.meteorclient.systems.accounts.impl.CrackedAccount;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AddCrackedAccountScreen extends AddAccountScreen {
    
    public AddCrackedAccountScreen(GuiTheme theme, AccountsScreen parent) {
        super(theme, "Add Cracked Account", parent);
    }
    
    @Override
    public void initWidgets() {
        WTable table = add(theme.table()).expandX().minWidth(400).widget();
        
        // Name
        table.add(theme.label("Name: "));
        WTextBox name = table.add(theme.textBox("", "seasnail8169", (text, c) ->
            // Username can't contain spaces
            c != ' '
        )).expandX().widget();
        name.setFocused(true);
        table.row();
        
        WHorizontalList list = add(theme.horizontalList()).expandX().widget();
        
        // Add
        add = list.add(theme.button("Add")).expandX().widget();
        add.action = () -> {
            if (!name.get().isEmpty() && name.get().length() < 17) {
                CrackedAccount account = new CrackedAccount(name.get());
                if (!(Accounts.get().exists(account))) {
                    AccountsScreen.addAccount(this, parent, account);
                }
            }
        };
        
        // Random
        WButton random = list.add(theme.button("Random")).expandX().widget();
        random.action = () -> name.set("Random_name_123");
        
        WButton edit = list.add(theme.button(GuiRenderer.EDIT)).widget();
        edit.action = () -> mc.setScreen(new RandomAccountSettingsScreen(theme));
        
        enterAction = add.action;
    }
    
}
