/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.accounts;

import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.color.Color;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.AccountType;
import meteordevelopment.meteorclient.systems.accounts.TokenAccount;

public class AccountInfoScreen extends WindowScreen {
    
    private final Account<?> account;
    
    public AccountInfoScreen(Account<?> account) {
        super(account.getUsername() + " details");
        this.account = account;
    }
    
    @Override
    public void initWidgets() {
        TokenAccount tokenAccount = (TokenAccount) account;
        WHorizontalList list = add(new WHorizontalList()).expandX().widget();
        
        String tokenLabel = account.getType() + " token:";
        if (account.getType() == AccountType.SESSION) {
            tokenLabel = "";
        }
        
        WButton copy = new WButton("Copy");
        copy.action = () -> mc.keyboard.setClipboard(tokenAccount.getToken());
        
        list.add(new WLabel(tokenLabel));
        list.add(new WLabel(account.getType() == AccountType.SESSION ? "Click to copy Token" : tokenAccount.getToken()).color(Color.GRAY)).pad(5);
        list.add(copy);
    }
    
}
