/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WVerticalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.settings.Setting;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class CollectionListSettingScreen<T> extends WindowScreen {
    
    protected final Setting<?> setting;
    protected final Collection<T> collection;
    private final Iterable<T> registry;
    
    private WTable table;
    private String filterText = "";
    
    private boolean syncListSettingWidths = true; // todo
    
    public CollectionListSettingScreen(String title, Setting<?> setting, Collection<T> collection, Iterable<T> registry) {
        super(title);
        
        this.registry = registry;
        this.setting = setting;
        this.collection = collection;
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
        // Left (all)
        WTable left = abc(registry, true, t -> {
            addValue(t);
            
            T v = getAdditionalValue(t);
            if (v != null) {
                addValue(v);
            }
        });
        
        if (syncListSettingWidths || !left.cells.isEmpty()) {
            table.add(new WVerticalSeparator()).expandWidgetY();
        }
        
        // Right (selected)
        WTable right = abc(collection, false, t -> {
            removeValue(t);
            
            T v = getAdditionalValue(t);
            if (v != null) {
                removeValue(v);
            }
        });
        
        postWidgets(left, right);
    }
    
    private WTable abc(Iterable<T> iterable, boolean isLeft, Consumer<T> buttonAction) {
        // Create
        Cell<WTable> cell = this.table.add(new WTable()).top();
        
        if (syncListSettingWidths) {
            cell.group("sync-width");
        }
        
        WTable table = cell.widget();
        
        // Sort
        Predicate<T> predicate = isLeft
            ? value -> this.includeValue(value) && !collection.contains(value)
            : this::includeValue;
        
        Iterable<T> sorted = SortingHelper.sort(iterable, predicate, this::getValueNames, filterText);
        
        sorted.forEach(t -> {
            
            table.add(getValueWidget(t));
            
            WPressable button = table.add(isLeft ? new WPlus() : new WMinus()).expandCellX().right().widget();
            button.action = () -> buttonAction.accept(t);
            
            table.row();
        });
        
        if (!table.cells.isEmpty()) {
            cell.expandX();
        }
        
        return table;
    }
    
    protected void invalidateTable() {
        table.clear();
        initTable();
    }
    
    protected void addValue(T value) {
        if (!collection.contains(value)) {
            collection.add(value);
            setting.onChanged();
            invalidateTable();
        }
    }
    
    protected void removeValue(T value) {
        if (collection.remove(value)) {
            setting.onChanged();
            invalidateTable();
        }
    }
    
    protected void postWidgets(WTable left, WTable right) {}
    
    protected boolean includeValue(T value) {
        return true;
    }
    
    protected abstract WWidget getValueWidget(T value);
    
    protected abstract String[] getValueNames(T value);
    
    protected T getAdditionalValue(T value) {
        return null;
    }
    
}
