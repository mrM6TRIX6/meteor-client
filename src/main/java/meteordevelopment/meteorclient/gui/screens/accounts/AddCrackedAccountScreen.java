/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.accounts;

import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.accounts.Accounts;
import meteordevelopment.meteorclient.systems.accounts.types.CrackedAccount;
import org.apache.commons.lang3.RandomStringUtils;

public class AddCrackedAccountScreen extends AddAccountScreen {
    
    public AddCrackedAccountScreen(AccountsScreen parent) {
        super("Add Cracked Account", parent);
    }
    
    @Override
    public void initWidgets() {
        WTable table = add(new WTable()).expandX().minWidth(400).widget();
        
        // Name
        table.add(new WLabel("Name: "));
        WTextBox name = table.add(new WTextBox("", "nickname", (text, c) ->
            // Username can't contain spaces
            c != ' '
        )).expandX().widget();
        name.setFocused(true);
        table.row();
        
        WHorizontalList list = add(new WHorizontalList()).expandX().widget();
        
        // Add
        add = list.add(new WButton("Add")).expandX().widget();
        add.action = () -> {
            if (!name.get().isEmpty() && name.get().length() < 17) {
                CrackedAccount account = new CrackedAccount(name.get());
                if (!(Accounts.get().exists(account))) {
                    AccountsScreen.addAccount(this, parent, account);
                }
            }
        };
        
        // Random
        WButton random = list.add(new WButton("Random")).expandX().widget();
        random.action = () -> name.set(
            RandomStringUtils.insecure().nextAlphanumeric(7, 15)
        );
        
        enterAction = add.action;
    }
    
}
