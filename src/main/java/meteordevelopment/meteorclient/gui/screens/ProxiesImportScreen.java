/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import meteordevelopment.meteorclient.systems.proxies.Proxy;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ProxiesImportScreen extends WindowScreen {

    private final File file;

    public ProxiesImportScreen(File file) {
        super("Import Proxies");
        this.file = file;
        this.onClosed(() -> {
            if (parent instanceof ProxiesScreen screen) {
                screen.reload();
            }
        });
    }

    @Override
    public void initWidgets() {
        if (file.exists() && file.isFile()) {
            add(new WLabel("Importing proxies from " + file.getName() + "...").color(Color.GREEN));
            add(new WLabel("One per line, " + Proxy.FORMAT).color(GuiConstants.TEXT_SECONDARY));
            WVerticalList list = add(new WSection("Log", false)).expandX().widget().add(new WVerticalList()).expandX().widget();
            Proxies proxies = Proxies.get();
            try {
                int success = 0, fail = 0;
                for (String line : Files.readAllLines(file.toPath())) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }

                    Proxy proxy = Proxy.parse(line);

                    if (proxy == null) {
                        list.add(new WLabel("Invalid proxy: " + line).color(Color.RED));
                        fail++;
                    } else if (proxies.add(proxy)) {
                        list.add(new WLabel("Imported proxy: " + proxy).color(Color.GREEN));
                        success++;
                    } else {
                        list.add(new WLabel("Proxy already exists: " + proxy).color(Color.ORANGE));
                        fail++;
                    }
                }

                int total = success + fail;
                add(new WLabel("Successfully imported " + success + "/" + total + " proxies.")
                    .color(total == 0 ? Color.ORANGE : Utils.lerp(Color.RED, Color.GREEN, (float) success / total))
                );
            } catch (IOException e) {
                MeteorClient.LOGGER.error("An error occurred while importing the proxy file", e);
            }
        } else {
            add(new WLabel("Invalid File!"));
        }

        add(new WHorizontalSeparator()).expandX();
        WButton btnBack = add(new WButton("Back")).expandX().widget();
        btnBack.action = this::close;
    }

}
