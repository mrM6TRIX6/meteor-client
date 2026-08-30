/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.*;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import meteordevelopment.meteorclient.systems.proxies.Proxy;
import meteordevelopment.meteorclient.systems.proxies.ProxyPinger;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ProxiesScreen extends WindowScreen {

    private final List<WCheckbox> checkboxes = new ArrayList<>();
    private final List<PingLabel> pingLabels = new ArrayList<>();

    private WContainer settingsContainer;

    public ProxiesScreen() {
        super("Proxies");
    }

    @Override
    public void initWidgets() {
        WLabel format = add(new WLabel("SOCKS5 only, " + Proxy.FORMAT)).widget();
        format.color = GuiConstants.TEXT_SECONDARY;

        WHorizontalList newList = add(new WHorizontalList()).expandX().widget();

        WTextBox input = newList.add(new WTextBox("", Proxy.FORMAT)).expandX().widget();
        input.setFocused(true);
        input.action = () -> format.color = GuiConstants.TEXT_SECONDARY;

        WButton addBtn = newList.add(new WButton("Add")).widget();
        addBtn.action = () -> {
            Proxy proxy = Proxy.parse(input.get());

            // Unparsable or already in the list
            if (proxy == null || !Proxies.get().add(proxy)) {
                format.color = Color.RED;
                return;
            }

            reload();
        };

        enterAction = addBtn.action;

        add(new WHorizontalSeparator()).expandX();

        // The list scrolls on its own so the buttons and settings below stay put no matter how many proxies there are
        WListView listView = add(new WListView()).expandX().minWidth(400).widget();
        WTable table = listView.add(new WTable()).expandX().widget();
        initTable(table);

        add(new WHorizontalSeparator()).expandX();

        WHorizontalList list = add(new WHorizontalList()).expandX().widget();

        // Ping all
        WButton pingAllBtn = list.add(new WButton("Ping All")).expandX().widget();
        pingAllBtn.action = ProxyPinger::pingAll;

        // Sort
        WButton sortBtn = list.add(new WButton("Sort")).expandX().widget();
        sortBtn.tooltip = "Sort by ping, fastest first and failed last.";
        sortBtn.action = () -> {
            Proxies.get().sortByPing();
            reload();
        };

        // Import
        PointerBuffer filters = BufferUtils.createPointerBuffer(1);

        ByteBuffer txtFilter = MemoryUtil.memASCII("*.txt");

        filters.put(txtFilter);
        filters.rewind();

        WButton importBtn = list.add(new WButton("Import")).expandX().widget();
        importBtn.action = () -> {
            String selectedFile = TinyFileDialogs.tinyfd_openFileDialog("Import Proxies", null, filters, null, false);
            if (selectedFile != null) {
                File file = new File(selectedFile);
                mc.setScreen(new ProxiesImportScreen(file));
            }
        };

        // Clear
        WButton clearBtn = list.add(new WButton("Clear")).expandX().widget();
        clearBtn.action = () -> {
            Proxies.get().clear();
            reload();
        };

        // Ping settings
        settingsContainer = add(new WVerticalList()).expandX().widget();
        settingsContainer.add(DefaultSettingsWidgetFactory.settings(Proxies.get().settings)).expandX();
    }

    private void initTable(WTable table) {
        table.clear();
        checkboxes.clear();
        pingLabels.clear();

        if (Proxies.get().isEmpty()) {
            return;
        }

        for (Proxy proxy : Proxies.get()) {
            WCheckbox current = table.add(new WCheckbox(Proxies.get().isCurrent(proxy))).widget();
            checkboxes.add(current);
            current.action = () -> {
                Proxies.get().setCurrent(current.checked ? proxy : null);

                // There can only ever be one current proxy
                for (WCheckbox checkbox : checkboxes) {
                    checkbox.checked = false;
                }
                current.checked = Proxies.get().isCurrent(proxy);
            };

            table.add(new WLabel(proxy.toString())).expandCellX();

            WLabel ping = table.add(new WLabel(pingText(proxy))).widget();
            ping.color = pingColor(proxy);
            pingLabels.add(new PingLabel(proxy, ping));

            WButton pingBtn = table.add(new WButton("Ping")).widget();
            pingBtn.action = () -> ProxyPinger.ping(proxy);

            WButton edit = table.add(new WButton(GuiConstants.EDIT)).widget();
            edit.action = () -> mc.setScreen(new EditProxyScreen(proxy, this::reload));

            WMinus remove = table.add(new WMinus()).widget();
            remove.action = () -> {
                Proxies.get().remove(proxy);
                reload();
            };

            table.row();
        }
    }

    @Override
    public void tick() {
        for (PingLabel pingLabel : pingLabels) {
            pingLabel.label().set(pingText(pingLabel.proxy()));
            pingLabel.label().color = pingColor(pingLabel.proxy());
        }

        Proxies.get().settings.tick(settingsContainer);
    }

    @Override
    protected void onClosed() {
        Proxies.get().save();
    }

    private static String pingText(Proxy proxy) {
        return switch (proxy.ping) {
            case Proxy.NOT_PINGED -> "-";
            case Proxy.PINGING -> "...";
            case Proxy.FAILED -> "failed";
            default -> proxy.ping + " ms";
        };
    }

    private static Color pingColor(Proxy proxy) {
        return switch (proxy.ping) {
            case Proxy.NOT_PINGED, Proxy.PINGING -> GuiConstants.TEXT_SECONDARY;
            case Proxy.FAILED -> Color.RED;
            default -> proxy.ping < 150 ? Color.GREEN : proxy.ping < 350 ? Color.ORANGE : Color.RED;
        };
    }

    @Override
    public boolean toClipboard() {
        return JsonUtils.toClipboard(Proxies.get());
    }

    @Override
    public boolean fromClipboard() {
        return JsonUtils.fromClipboard(Proxies.get());
    }

    private record PingLabel(Proxy proxy, WLabel label) {}

}
