/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import meteordevelopment.meteorclient.systems.proxies.Proxy;
import meteordevelopment.meteorclient.utils.render.color.Color;

/**
 * Lets a proxy which is already in the list be retyped, so a wrong port does not mean removing it and adding a new one.
 */
public class EditProxyScreen extends WindowScreen {

    private final Proxy proxy;
    private final Runnable action;

    public EditProxyScreen(Proxy proxy, Runnable action) {
        super("Edit Proxy");

        this.proxy = proxy;
        this.action = action;
    }

    @Override
    public void initWidgets() {
        WLabel format = add(new WLabel("SOCKS5 only, " + Proxy.FORMAT)).widget();
        format.color = GuiConstants.TEXT_SECONDARY;

        WTextBox input = add(new WTextBox(proxy.toFullString(), Proxy.FORMAT)).expandX().minWidth(300).widget();
        input.setFocused(true);
        input.setCursorMax();
        input.action = () -> format.color = GuiConstants.TEXT_SECONDARY;

        add(new WHorizontalSeparator()).expandX();

        WButton save = add(new WButton("Save")).expandX().widget();
        save.action = () -> {
            Proxy edited = Proxy.parse(input.get());

            // Unparsable or another entry in the list already is this proxy
            if (edited == null || !Proxies.get().replace(proxy, edited)) {
                format.color = Color.RED;
                return;
            }

            close();
        };

        enterAction = save.action;
    }

    @Override
    protected void onClosed() {
        if (action != null) {
            action.run();
        }
    }

}
