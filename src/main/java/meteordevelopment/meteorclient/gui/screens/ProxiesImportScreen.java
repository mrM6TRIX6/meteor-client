/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.color.Color;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import meteordevelopment.meteorclient.systems.proxies.Proxy;
import meteordevelopment.meteorclient.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;

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
            WVerticalList list = add(new WSection("Log", false)).expandX().widget().add(new WVerticalList()).expandX().widget();
            Proxies proxies = Proxies.get();
            try {
                int success = 0, fail = 0;
                for (String line : Files.readAllLines(file.toPath())) {
                    Matcher matcher = Proxies.PROXY_PATTERN.matcher(line);
                    
                    if (matcher.matches()) {
                        String address = matcher.group("host").replaceAll("\\b0+\\B", "");
                        int port = Integer.parseInt(matcher.group("port"));
                        String login = matcher.group("user");
                        String password = matcher.group("pass");
                        
                        Proxy proxy = new Proxy.Builder()
                            .address(address)
                            .port(port)
                            .name("Proxy " + (proxies.getCount() + 1))
                            .username(login != null ? login : "")
                            .password(password != null ? password : "")
                            .build();
                        
                        if (proxies.add(proxy)) {
                            list.add(new WLabel("Imported proxy: " + proxy.name.get()).color(Color.GREEN));
                            success++;
                        } else {
                            list.add(new WLabel("Proxy already exists: " + proxy.name.get()).color(Color.ORANGE));
                            fail++;
                        }
                    } else {
                        list.add(new WLabel("Invalid proxy: " + line).color(Color.RED));
                        fail++;
                    }
                }
                add(new WLabel("Successfully imported " + success + "/" + (fail + success) + " proxies.")
                    .color(Utils.lerp(Color.RED, Color.GREEN, (float) success / (success + fail)))
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
