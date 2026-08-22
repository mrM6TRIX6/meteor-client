/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.IChangeable;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;

public abstract class CollectionMapSettingScreen<K, V> extends WindowScreen {
    
    private final Setting<?> setting;
    protected final Map<K, V> map;
    private final Iterable<K> registry;
    
    private WTable table;
    private String filterText = "";
    
    public CollectionMapSettingScreen(String title, Setting<?> setting, Map<K, V> map, Iterable<K> registry) {
        super(title);
        
        this.setting = setting;
        this.map = map;
        this.registry = registry;
    }
    
    @Override
    public void initWidgets() {
        // Filter
        WTextBox filter = add(new WTextBox("")).minWidth(400).expandX().widget();
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.get().trim();
            
            table.clear();
            initTable();
        };
        
        table = add(new WTable()).expandX().widget();
        
        initTable();
    }
    
    private void initTable() {
        Comparator<K> prioritizeChanged = Comparator.comparing(key -> !(map.get(key) instanceof IChangeable changeable && changeable.isChanged()));
        Iterable<K> sorted = SortingHelper.sortWithPriority(registry, this::includeValue, this::getValueNames, filterText, prioritizeChanged);
        
        sorted.forEach(t -> {
            @Nullable V data = map.get(t);
            boolean isChanged = data instanceof IChangeable changeable && changeable.isChanged();
            
            table.add(getValueWidget(t)).expandCellX();
            table.add(new WLabel(isChanged ? "*" : " "));
            table.add(getDataWidget(t, data));
            
            WButton reset = table.add(new WButton(GuiConstants.RESET)).widget();
            reset.action = () -> removeValue(t);
            reset.tooltip = "Reset";
            
            table.row();
        });
    }
    
    protected void invalidateTable() {
        table.clear();
        initTable();
    }
    
    protected void removeValue(K value) {
        if (map.remove(value) != null) {
            setting.onChanged();
            invalidateTable();
        }
    }
    
    protected boolean includeValue(K value) {
        return true;
    }
    
    protected abstract WWidget getValueWidget(K value);
    
    protected abstract WWidget getDataWidget(K value, @Nullable V data);
    
    protected abstract String[] getValueNames(K value);
    
}