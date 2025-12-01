/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.accounts;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WAccount;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.Accounts;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import org.jetbrains.annotations.Nullable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AccountsScreen extends WindowScreen {
    
    public AccountsScreen(GuiTheme theme) {
        super(theme, "Accounts");
    }
    
    @Override
    public void initWidgets() {
        // Accounts
        for (Account<?> account : Accounts.get()) {
            WAccount wAccount = add(theme.account(this, account)).expandX().minWidth(400).widget();
            wAccount.refreshScreenAction = this::reload;
        }
        
        add(theme.horizontalSeparator()).expandX();
        
        // Add account
        WHorizontalList list = add(theme.horizontalList()).expandX().minWidth(400).widget();
        
        addButton(list, "Cracked", () -> mc.setScreen(new AddCrackedAccountScreen(theme, this)));
        addButton(list, "Altening", () -> mc.setScreen(new AddAlteningAccountScreen(theme, this)));
        addButton(list, "Microsoft", () -> mc.setScreen(new AddMicrosoftAccountScreen(theme, this)));
        
        // Clear
        addButton(this.window, "Clear", () -> {
            Accounts.get().clear();
            reload();
        });
    }
    
    private void addButton(WContainer container, String text, Runnable action) {
        WButton button = container.add(theme.button(text)).expandX().widget();
        button.action = action;
    }
    
    public static void addAccount(@Nullable AddAccountScreen screen, AccountsScreen parent, Account<?> account) {
        if (screen != null) {
            screen.locked = true;
        }
        
        MeteorExecutor.execute(() -> {
            if (account.fetchInfo()) {
                account.getCache().loadHead();
                
                Accounts.get().add(account);
                if (account.login()) {
                    Accounts.get().save();
                }
                
                if (screen != null) {
                    screen.locked = false;
                    screen.close();
                }
                
                parent.reload();
                
                return;
            }
            if (screen != null) {
                screen.locked = false;
            }
        });
    }
    
    @Override
    public boolean toClipboard() {
        return JsonUtils.toClipboard(Accounts.get());
    }
    
    @Override
    public boolean fromClipboard() {
        return JsonUtils.fromClipboard(Accounts.get());
    }
    
}
