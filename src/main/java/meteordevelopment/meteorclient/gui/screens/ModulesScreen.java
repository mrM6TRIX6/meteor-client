/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.tabs.impl.ModulesTab;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WItem;
import meteordevelopment.meteorclient.gui.widgets.WModule;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import net.minecraft.item.Items;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static meteordevelopment.meteorclient.renderer.RenderUtils.getWindowHeight;
import static meteordevelopment.meteorclient.renderer.RenderUtils.getWindowWidth;

public class ModulesScreen extends TabScreen {
    
    private static final int MODULE_SEARCH_COUNT = 8;
    
    private WCategoryController controller;
    
    public ModulesScreen() {
        super(Tabs.get(ModulesTab.class));
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
        WWindow window = new WWindow(category.name);
        window.id = category.name;
        window.minWidth = 175;
        window.padding = 0;
        window.spacing = 0;
        
        if (GuiConstants.CATEGORY_ICONS) {
            window.beforeHeaderInit = wContainer -> wContainer.add(new WItem(category.icon)).pad(2);
        }
        
        container.add(window);
        window.view.scrollOnlyWhenMouseOver = true;
        window.view.hasScrollBar = false;
        window.view.spacing = 0;
        
        for (Module module : moduleList) {
            window.add(new WModule(module)).expandX();
        }
        
        return window;
    }
    
    // Search
    
    protected void createSearchW(WContainer container, String text) {
        if (!text.isEmpty()) {
            // Names
            List<Pair<Module, String>> modules = Modules.get().searchNames(text);
            
            if (!modules.isEmpty()) {
                WSection section = container.add(new WSection("Modules")).expandX().widget();
                section.spacing = 0;
                
                int count = 0;
                for (Pair<Module, String> p : modules) {
                    if (count >= MODULE_SEARCH_COUNT || count >= modules.size()) {
                        break;
                    }
                    section.add(new WModule(p.getLeft(), p.getRight())).expandX();
                    count++;
                }
            }
            
            // Settings
            Set<Module> settings = Modules.get().searchSettingNames(text);
            
            if (!settings.isEmpty()) {
                WSection section = container.add(new WSection("Settings")).expandX().widget();
                section.spacing = 0;
                
                int count = 0;
                for (Module module : settings) {
                    if (count >= MODULE_SEARCH_COUNT || count >= settings.size()) {
                        break;
                    }
                    section.add(new WModule(module)).expandX();
                    count++;
                }
            }
        }
    }
    
    protected WWindow createSearch(WContainer container) {
        WWindow search = new WWindow("Search");
        search.id = "search";
        search.minWidth = 175;
        
        if (GuiConstants.CATEGORY_ICONS) {
            search.beforeHeaderInit = wContainer -> wContainer.add(new WItem(Items.COMPASS.getDefaultStack())).pad(2);
        }
        
        container.add(search);
        search.view.scrollOnlyWhenMouseOver = true;
        search.view.hasScrollBar = false;
        search.view.maxHeight -= 20;
        
        WVerticalList list = new WVerticalList();
        
        WTextBox text = search.add(new WTextBox("")).minWidth(140).expandX().widget();
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
        
        WWindow favorites = new WWindow("Favorites");
        favorites.id = "favorites";
        favorites.minWidth = 175;
        favorites.padding = 0;
        favorites.spacing = 0;
        
        if (GuiConstants.CATEGORY_ICONS) {
            favorites.beforeHeaderInit = wContainer -> wContainer.add(new WItem(Items.NETHER_STAR.getDefaultStack())).pad(2);
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
            window.add(new WModule(module)).expandX();
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
            for (Category category : Category.values()) {
                moduleList.addAll(Modules.get().getGroup(category));
                
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
            double pad = GuiConstants.scale(4);
            double h = GuiConstants.scale(40);
            
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
