/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.screens.accounts.AccountInfoScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.renderer.PlayerHeadTexture;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.Accounts;
import meteordevelopment.meteorclient.systems.accounts.TokenAccount;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WAccount extends WHorizontalList {

    public Runnable refreshScreenAction;

    private final WidgetScreen screen;
    private final Account<?> account;

    public WAccount(WidgetScreen screen, Account<?> account) {
        this.screen = screen;
        this.account = account;
    }

    @Override
    public void init() {
        // Head
        PlayerHeadTexture head = account.getCache().getHeadTexture();
        add(new WTexture(32, 32, 0, head.identifier()));

        // Name
        WLabel name = add(new WLabel(account.getUsername())).widget();
        if (mc.getSession().getUsername().equalsIgnoreCase(account.getUsername())) {
            name.color = GuiConstants.LOGGED_IN;
        }

        // Type
        WLabel label = add(new WLabel("(" + account.getType() + ")")).expandCellX().right().widget();
        label.color = GuiConstants.TEXT_SECONDARY;

        // Info
        if (account instanceof TokenAccount) {
            WButton info = add(new WButton("Info")).widget();
            info.action = () -> mc.setScreen(new AccountInfoScreen(account));
        }

        // Login
        WButton login = add(new WButton("Login")).widget();
        login.action = () -> {
            login.minWidth = login.width;
            login.set("...");
            screen.locked = true;

            MeteorExecutor.execute(() -> {
                if (account.fetchInfo() && account.login()) {
                    name.set(account.getUsername());

                    Accounts.get().save();

                    screen.taskAfterRender = refreshScreenAction;
                }

                login.minWidth = 0;
                login.set("Login");
                screen.locked = false;
            });
        };

        // Remove
        WMinus remove = add(new WMinus()).widget();
        remove.action = () -> {
            Accounts.get().remove(account);
            if (refreshScreenAction != null) {
                refreshScreenAction.run();
            }
        };
    }

}
