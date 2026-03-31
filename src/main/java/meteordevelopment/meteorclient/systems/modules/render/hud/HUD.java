/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.events.meteor.CustomFontChangedEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.ColorListSetting;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.hud.elements.*;
import meteordevelopment.meteorclient.systems.modules.render.hud.screens.HUDEditorScreen;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HUD extends Module implements Iterable<HUDElement> {
    
    public static final HUDGroup GROUP = new HUDGroup("Meteor");
    
    public final Map<String, HUDElementInfo<?>> infos = new TreeMap<>();
    private final List<HUDElement> elements = new ArrayList<>();
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgEditor = settings.createGroup("Editor");
    
    // General
    
    private final Setting<Boolean> customFont = sgGeneral.add(new BoolSetting.Builder()
        .name("custom-font")
        .description("Text will use custom font.")
        .defaultValue(true)
        .onChanged(aBoolean -> {
            for (HUDElement element : elements) {
                element.onFontChanged();
            }
        })
        .build()
    );
    
    private final Setting<Boolean> hideInMenus = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-in-menus")
        .description("Hides the meteor HUD when in inventory screens or game menus.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Double> textScale = sgGeneral.add(new DoubleSetting.Builder()
        .name("text-scale")
        .description("Scale of text if not overridden by the element.")
        .defaultValue(1)
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );
    
    public final Setting<List<SettingColor>> textColors = sgGeneral.add(new ColorListSetting.Builder()
        .name("text-colors")
        .description("Colors used for the Text element.")
        .defaultValue(List.of(new SettingColor(), new SettingColor(175, 175, 175), new SettingColor(25, 225, 25), new SettingColor(225, 25, 25)))
        .build()
    );
    
    // Editor
    
    public final Setting<Integer> border = sgEditor.add(new IntSetting.Builder()
        .name("border")
        .description("Space around the edges of the screen.")
        .defaultValue(4)
        .sliderMax(20)
        .build()
    );
    
    public final Setting<Integer> snappingRange = sgEditor.add(new IntSetting.Builder()
        .name("snapping-range")
        .description("Snapping range in editor.")
        .defaultValue(10)
        .sliderMax(20)
        .build()
    );
    
    public HUD() {
        super(Categories.RENDER, "HUD", "The client in-game dashboard.");
        
        settings.registerColorSettings(null);
        
        register(MeteorTextHud.INFO);
        register(ItemHUD.INFO);
        register(InventoryHUD.INFO);
        register(CompassHUD.INFO);
        register(ArmorHUD.INFO);
        register(HoleHUD.INFO);
        register(PlayerModelHUD.INFO);
        register(ActiveModulesHUD.INFO);
        register(LagNotifierHUD.INFO);
        register(PlayerRadarHUD.INFO);
        register(ModuleInfosHUD.INFO);
        register(PotionTimersHUD.INFO);
        register(CombatHUD.INFO);
        register(MapHUD.INFO);
        register(RectangleHUD.INFO);
        register(TextureHUD.INFO);
        register(BlurHUD.INFO);
    }
    
    public static HUD get() {
        return Modules.get().get(HUD.class);
    }
    
    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        
        WHorizontalList buttons = table.add(theme.horizontalList()).expandX().widget();
        
        // Edit
        WButton openEditor = buttons.add(theme.button("Edit")).expandX().widget();
        openEditor.action = () -> mc.setScreen(new HUDEditorScreen(theme));
        
        // Clear
        WButton clearBtn = buttons.add(theme.button("Clear")).expandX().widget();
        clearBtn.action = this::clear;
        
        return table;
    }
    
    public void register(HUDElementInfo<?> info) {
        infos.put(info.name, info);
    }
    
    private void add(HUDElement element, int x, int y, XAnchor xAnchor, YAnchor yAnchor) {
        element.box.setPos(x, y);
        
        if (xAnchor == null || yAnchor == null) {
            element.box.updateAnchors();
        } else {
            element.box.xAnchor = xAnchor;
            element.box.yAnchor = yAnchor;
        }
        
        element.settings.registerColorSettings(null);
        
        elements.add(element);
    }
    
    public void add(HUDElementInfo<?> info, int x, int y, XAnchor xAnchor, YAnchor yAnchor) {
        add(info.create(), x, y, xAnchor, yAnchor);
    }
    
    public void add(HUDElementInfo<?> info, int x, int y) {
        add(info, x, y, null, null);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void add(@NotNull HUDElementInfo.Preset preset, int x, int y, XAnchor xAnchor, YAnchor yAnchor) {
        HUDElement element = preset.info.create();
        preset.callback.accept(element);
        add(element, x, y, xAnchor, yAnchor);
    }
    
    public void add(@NotNull HUDElementInfo<?>.Preset preset, int x, int y) {
        add(preset, x, y, null, null);
    }
    
    void remove(HUDElement element) {
        element.settings.unregisterColorSettings();
        elements.remove(element);
    }
    
    public int getCount() {
        return elements.size();
    }
    
    public void clear() {
        elements.clear();
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (Utils.isLoading()) {
            return;
        }
        
        if (!(isActive() || HUDEditorScreen.isOpen())) {
            return;
        }
        
        for (HUDElement element : elements) {
            if (element.isActive() || element.isInEditor()) {
                element.tick(HUDRenderer.INSTANCE);
            }
        }
    }
    
    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (Utils.isLoading()) {
            return;
        }
        
        if (!isActive() || shouldHideHud()) {
            return;
        }
        if ((mc.options.hudHidden || mc.debugHudEntryList.isF3Enabled()) && !HUDEditorScreen.isOpen()) {
            return;
        }
        
        HUDRenderer.INSTANCE.begin(event.drawContext);
        
        for (HUDElement element : elements) {
            element.updatePos();
            
            if (element.isActive() || element.isInEditor()) {
                element.render(HUDRenderer.INSTANCE);
            }
        }
        
        HUDRenderer.INSTANCE.end();
    }
    
    private boolean shouldHideHud() {
        return hideInMenus.get() && mc.currentScreen != null && !(mc.currentScreen instanceof WidgetScreen);
    }
    
    @EventHandler
    private void onCustomFontChanged(CustomFontChangedEvent event) {
        if (customFont.get()) {
            for (HUDElement element : elements) {
                element.onFontChanged();
            }
        }
    }
    
    public boolean hasCustomFont() {
        return customFont.get();
    }
    
    public double getTextScale() {
        return textScale.get();
    }
    
    @NotNull
    @Override
    public Iterator<HUDElement> iterator() {
        return elements.iterator();
    }
    
    // Serialization
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = super.toJson();
        
        jsonObject.add("settings", settings.toJson());
        jsonObject.add("elements", JsonUtils.listToJson(elements));
        
        return jsonObject;
    }
    
    @Override
    public HUD fromJson(JsonObject jsonObject) {
        super.fromJson(jsonObject);
        
        // Elements
        elements.clear();
        
        for (JsonElement element : jsonObject.get("elements").getAsJsonArray()) {
            JsonObject jsonObject1 = (JsonObject) element;
            if (jsonObject1.get("name").getAsString().isEmpty()) {
                continue;
            }
            
            HUDElementInfo<?> info = infos.get(jsonObject1.get("name").getAsString());
            if (info != null) {
                HUDElement hudElement = info.create();
                hudElement.fromJson(jsonObject1);
                elements.add(hudElement);
            }
        }
        
        return this;
    }
    
}
