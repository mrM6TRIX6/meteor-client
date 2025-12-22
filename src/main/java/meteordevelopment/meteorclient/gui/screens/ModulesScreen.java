/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.systems.clientsettings.ClientSettings;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public class ModulesScreen extends TabScreen {
    
    private WCategoryController controller;
    
    public ModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
    }
    
    @Override
    public void initWidgets() {
        controller = add(new WCategoryController()).widget();
    }
    
    @Override
    protected void init() {
        super.init();
        controller.refresh();
    }
    
    // Category
    
    protected WWindow createCategory(WContainer container, Category category, List<Module> moduleList) {
        WWindow window = theme.window(category.name);
        window.id = category.name;
        window.minWidth = 175;
        window.padding = 0;
        window.spacing = 0;
        
        if (theme.categoryIcons()) {
            window.beforeHeaderInit = wContainer -> wContainer.add(theme.item(category.icon)).pad(2);
        }
        
        container.add(window);
        window.view.scrollOnlyWhenMouseOver = true;
        window.view.hasScrollBar = false;
        window.view.spacing = 0;
        
        for (Module module : moduleList) {
            window.add(theme.module(module)).expandX();
        }
        
        return window;
    }
    
    // Search
    
    protected void createSearchW(WContainer container, String text) {
        if (!text.isEmpty()) {
            // Titles
            Set<Module> modules = Modules.get().searchNames(text);
            
            if (!modules.isEmpty()) {
                WSection section = container.add(theme.section("Modules")).expandX().widget();
                section.spacing = 0;
                
                int count = 0;
                for (Module module : modules) {
                    if (count >= ClientSettings.get().moduleSearchCount.get() || count >= modules.size()) {
                        break;
                    }
                    section.add(theme.module(module)).expandX();
                    count++;
                }
            }
            
            // Settings
            modules = Modules.get().searchSettingTitles(text);
            
            if (!modules.isEmpty()) {
                WSection section = container.add(theme.section("Settings")).expandX().widget();
                section.spacing = 0;
                
                int count = 0;
                for (Module module : modules) {
                    if (count >= ClientSettings.get().moduleSearchCount.get() || count >= modules.size()) {
                        break;
                    }
                    section.add(theme.module(module)).expandX();
                    count++;
                }
            }
        }
    }
    
    protected WWindow createSearch(WContainer container) {
        WWindow search = theme.window("Search");
        search.id = "search";
        search.minWidth = 175;
        
        if (theme.categoryIcons()) {
            search.beforeHeaderInit = wContainer -> wContainer.add(theme.item(Items.COMPASS.getDefaultStack())).pad(2);
        }
        
        container.add(search);
        search.view.scrollOnlyWhenMouseOver = true;
        search.view.hasScrollBar = false;
        search.view.maxHeight -= 20;
        
        WVerticalList list = theme.verticalList();
        
        WTextBox text = search.add(theme.textBox("")).minWidth(140).expandX().widget();
        text.setFocused(true);
        text.action = () -> {
            list.clear();
            createSearchW(list, text.get());
        };
        
        search.add(list).expandX();
        createSearchW(list, text.get());
        
        return search;
    }
    
    // Favorites
    
    protected Cell<WWindow> createFavorites(WContainer container) {
        boolean hasFavorites = Modules.get().getAll().stream().anyMatch(module -> module.favorite);
        if (!hasFavorites) {
            return null;
        }
        
        WWindow favorites = theme.window("Favorites");
        favorites.id = "favorites";
        favorites.minWidth = 175;
        favorites.padding = 0;
        favorites.spacing = 0;
        
        if (theme.categoryIcons()) {
            favorites.beforeHeaderInit = wContainer -> wContainer.add(theme.item(Items.NETHER_STAR.getDefaultStack())).pad(2);
        }
        
        Cell<WWindow> cell = container.add(favorites);
        favorites.view.scrollOnlyWhenMouseOver = true;
        favorites.view.hasScrollBar = false;
        favorites.view.spacing = 0;
        
        createFavoritesW(favorites);
        return cell;
    }
    
    protected boolean createFavoritesW(WWindow window) {
        List<Module> modules = new ArrayList<>();
        
        for (Module module : Modules.get().getAll()) {
            if (module.favorite) {
                modules.add(module);
            }
        }
        
        modules.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.name, o2.name));
        
        for (Module module : modules) {
            window.add(theme.module(module)).expandX();
        }
        
        return !modules.isEmpty();
    }
    
    @Override
    public boolean toClipboard() {
        return JsonUtils.toClipboard(Modules.get());
    }
    
    @Override
    public boolean fromClipboard() {
        return JsonUtils.fromClipboard(Modules.get());
    }
    
    @Override
    public void reload() {}
    
    // Stuff
    
    protected class WCategoryController extends WContainer {
        
        public final List<WWindow> windows = new ArrayList<>();
        private Cell<WWindow> favorites;
        
        @Override
        public void init() {
            List<Module> moduleList = new ArrayList<>();
            for (Category category : Modules.loopCategories()) {
                for (Module module : Modules.get().getGroup(category)) {
                    if (!ClientSettings.get().hiddenModules.get().contains(module)) {
                        moduleList.add(module);
                    }
                }
                
                // Ensure empty categories are not shown
                if (!moduleList.isEmpty()) {
                    windows.add(createCategory(this, category, moduleList));
                    moduleList.clear();
                }
            }
            windows.add(createSearch(this));
            refresh();
        }
        
        protected void refresh() {
            if (favorites == null) {
                favorites = createFavorites(this);
                if (favorites != null) {
                    windows.add(favorites.widget());
                }
            } else {
                favorites.widget().clear();
                
                if (!createFavoritesW(favorites.widget())) {
                    remove(favorites);
                    windows.remove(favorites.widget());
                    favorites = null;
                }
            }
        }
        
        @Override
        protected void onCalculateWidgetPositions() {
            double pad = theme.scale(4);
            double h = theme.scale(40);
            
            double x = this.x + pad;
            double y = this.y;
            
            for (Cell<?> cell : cells) {
                double windowWidth = getWindowWidth();
                double windowHeight = getWindowHeight();
                
                if (x + cell.width > windowWidth) {
                    x = x + pad;
                    y += h;
                }
                
                if (x > windowWidth) {
                    x = windowWidth / 2.0 - cell.width / 2.0;
                    if (x < 0) {
                        x = 0;
                    }
                }
                if (y > windowHeight) {
                    y = windowHeight / 2.0 - cell.height / 2.0;
                    if (y < 0) {
                        y = 0;
                    }
                }
                
                cell.x = x;
                cell.y = y;
                
                cell.width = cell.widget().width;
                cell.height = cell.widget().height;
                
                cell.alignWidget();
                
                x += cell.width + pad;
            }
        }
        
    }
    
}
