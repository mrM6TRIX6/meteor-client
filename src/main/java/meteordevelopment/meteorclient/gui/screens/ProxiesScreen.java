/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import meteordevelopment.meteorclient.systems.proxies.Proxy;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
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
    
    public ProxiesScreen() {
        super("Proxies");
    }
    
    @Override
    public void initWidgets() {
        WTable table = add(new WTable()).expandX().minWidth(400).widget();
        initTable(table);
        
        add(new WHorizontalSeparator()).expandX();
        
        WHorizontalList list = add(new WHorizontalList()).expandX().widget();
        
        // New
        WButton newBtn = list.add(new WButton("New")).expandX().widget();
        newBtn.action = () -> mc.setScreen(new EditProxyScreen(null, this::reload));
        
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
    }
    
    private void initTable(WTable table) {
        table.clear();
        if (Proxies.get().isEmpty()) {
            return;
        }
        
        for (Proxy proxy : Proxies.get()) {
            WCheckbox enabled = table.add(new WCheckbox(proxy.enabled.get())).widget();
            checkboxes.add(enabled);
            enabled.action = () -> {
                boolean checked = enabled.checked;
                Proxies.get().setEnabled(proxy, checked);
                
                for (WCheckbox checkbox : checkboxes) {
                    checkbox.checked = false;
                }
                enabled.checked = checked;
            };
            
            WLabel name = table.add(new WLabel(proxy.name.get())).widget();
            name.color = GuiConstants.TEXT;
            
            WHorizontalList ipList = table.add(new WHorizontalList()).expandCellX().widget();
            ipList.spacing = 0;
            
            ipList.add(new WLabel(proxy.address.get()));
            ipList.add(new WLabel(":")).widget().color = GuiConstants.TEXT_SECONDARY;
            ipList.add(new WLabel(Integer.toString(proxy.port.get())));
            
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
    public boolean toClipboard() {
        return JsonUtils.toClipboard(Proxies.get());
    }
    
    @Override
    public boolean fromClipboard() {
        return JsonUtils.fromClipboard(Proxies.get());
    }
    
    protected static class EditProxyScreen extends EditSystemScreen<Proxy> {
        
        private final boolean initialEnabled;
        
        public EditProxyScreen(Proxy value, Runnable reload) {
            super(value, reload);
            this.initialEnabled = value != null ? value.enabled.get() : false;
        }
        
        @Override
        public void onClosed() {
            if (value.enabled.get() && !initialEnabled) {
                Proxies.get().setEnabled(value, true);
            }
            super.onClosed();
        }
        
        @Override
        public Proxy create() {
            return new Proxy.Builder().build();
        }
        
        @Override
        public boolean save() {
            return value.resolveAddress() && (!isNew || Proxies.get().add(value));
        }
        
        @Override
        public Settings getSettings() {
            return value.settings;
        }
        
    }
    
}
