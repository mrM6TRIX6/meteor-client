/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.accounts;

import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WAccount;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WListView;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.Accounts;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import org.jetbrains.annotations.Nullable;

public class AccountsScreen extends WindowScreen {
    
    public AccountsScreen() {
        super("Accounts");
    }
    
    @Override
    public void initWidgets() {
        // Accounts, in a view of their own so the buttons below stay reachable with a long list
        WListView listView = add(new WListView()).expandX().minWidth(400).widget();

        for (Account<?> account : Accounts.get()) {
            WAccount wAccount = listView.add(new WAccount(this, account)).expandX().widget();
            wAccount.refreshScreenAction = this::reload;
        }

        add(new WHorizontalSeparator()).expandX();
        
        // Add account
        WHorizontalList list = add(new WHorizontalList()).expandX().minWidth(400).widget();
        
        addButton(list, "Cracked", () -> mc.setScreen(new AddCrackedAccountScreen(this)));
        addButton(list, "Altening", () -> mc.setScreen(new AddAlteningAccountScreen(this)));
        addButton(list, "Session", () -> mc.setScreen(new AddSessionAccountScreen(this)));
        addButton(list, "Microsoft", () -> mc.setScreen(new AddMicrosoftAccountScreen(this)));
        
        // Clear
        addButton(this.window, "Clear", () -> {
            Accounts.get().clear();
            reload();
        });
    }
    
    private void addButton(WContainer container, String text, Runnable action) {
        WButton button = container.add(new WButton(text)).expandX().widget();
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
