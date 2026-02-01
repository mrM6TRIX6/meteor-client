/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.accounts;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.AccountType;
import meteordevelopment.meteorclient.systems.accounts.TokenAccount;
import meteordevelopment.meteorclient.utils.render.color.Color;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AccountInfoScreen extends WindowScreen {
    
    private final Account<?> account;
    
    public AccountInfoScreen(GuiTheme theme, Account<?> account) {
        super(theme, account.getUsername() + " details");
        this.account = account;
    }
    
    @Override
    public void initWidgets() {
        TokenAccount tokenAccount = (TokenAccount) account;
        WHorizontalList list = add(theme.horizontalList()).expandX().widget();
        
        String tokenLabel = account.getType() + " token:";
        if (account.getType() == AccountType.SESSION) {
            tokenLabel = "";
        }
        
        WButton copy = theme.button("Copy");
        copy.action = () -> mc.keyboard.setClipboard(tokenAccount.getToken());
        
        list.add(theme.label(tokenLabel));
        list.add(theme.label(account.getType() == AccountType.SESSION ? "Click to copy Token" : tokenAccount.getToken()).color(Color.GRAY)).pad(5);
        list.add(copy);
    }
    
}
