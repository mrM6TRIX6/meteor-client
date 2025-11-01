/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import meteordevelopment.meteorclient.systems.proxies.Proxy;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;

public class ProxiesImportScreen extends WindowScreen {
    
    private final File file;
    
    public ProxiesImportScreen(GuiTheme theme, File file) {
        super(theme, "Import Proxies");
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
            add(theme.label("Importing proxies from " + file.getName() + "...").color(Color.GREEN));
            WVerticalList list = add(theme.section("Log", false)).expandX().widget().add(theme.verticalList()).expandX().widget();
            Proxies proxies = Proxies.get();
            try {
                int success = 0, fail = 0;
                for (String line : Files.readAllLines(file.toPath())) {
                    Matcher matcher = Proxies.PROXY_PATTERN.matcher(line);
                    
                    if (matcher.matches()) {
                        String address = matcher.group(2).replaceAll("\\b0+\\B", "");
                        int port = Integer.parseInt(matcher.group(3));
                        String login = matcher.group(4);
                        String password = matcher.group(5);
                        
                        Proxy proxy = new Proxy.Builder()
                            .address(address)
                            .port(port)
                            .name("Proxy " + (proxies.getCount() + 1))
                            .username(login != null ? login : "")
                            .password(password != null ? password : "")
                            .build();
                        
                        if (proxies.add(proxy)) {
                            list.add(theme.label("Imported proxy: " + proxy.name.get()).color(Color.GREEN));
                            success++;
                        } else {
                            list.add(theme.label("Proxy already exists: " + proxy.name.get()).color(Color.ORANGE));
                            fail++;
                        }
                    } else {
                        list.add(theme.label("Invalid proxy: " + line).color(Color.RED));
                        fail++;
                    }
                }
                add(theme.label("Successfully imported " + success + "/" + (fail + success) + " proxies.")
                    .color(Utils.lerp(Color.RED, Color.GREEN, (float) success / (success + fail)))
                );
            } catch (IOException e) {
                MeteorClient.LOG.error("An error occurred while importing the proxy file", e);
            }
        } else {
            add(theme.label("Invalid File!"));
        }
        
        add(theme.horizontalSeparator()).expandX();
        WButton btnBack = add(theme.button("Back")).expandX().widget();
        btnBack.action = this::close;
    }
    
}
