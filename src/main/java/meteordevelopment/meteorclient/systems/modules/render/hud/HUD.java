/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.MultiChoiceSetting;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.hud.elements.Test;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.name.Namer;
import meteordevelopment.meteorclient.utils.render.LoadingVisualGuard;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;

public class HUD extends Module {

    private final Map<Class<? extends HUDElement>, HUDElement> elements = new Reference2ReferenceOpenHashMap<>();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<SequencedSet<HUDElement>> enabledElements;

    public HUD() {
        super(Category.RENDER, "HUD", "The client in-game dashboard.");

        runInMainMenu = true;

        init();

        enabledElements = sgGeneral.add(
            new MultiChoiceSetting.Builder<HUDElement>()
                .name("Elements")
                .description("Enabled HUD elements.")
                .namer(Namer.of(HUDElement::getName))
                .choices(elements.values())
                .build()
        );
    }

    public static HUD get() {
        return Modules.get().get(HUD.class);
    }
    
    private void init() {
        add(new Test());
    }

    private void add(HUDElement element) {
        HUDElement existing = get(element.getName());
        if (existing != null) {
            throw new IllegalArgumentException("HUD element with name '%s' already exists".formatted(element.getName()));
        }

        elements.put(element.getClass(), element);
    }

    // Access

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends HUDElement> T get(Class<T> clazz) {
        return (T) elements.get(clazz);
    }

    public <T extends HUDElement> Optional<T> getOptional(Class<T> clazz) {
        return Optional.ofNullable(get(clazz));
    }

    @Nullable
    public HUDElement get(String name) {
        for (HUDElement element : elements.values()) {
            if (element.getName().equalsIgnoreCase(name)) {
                return element;
            }
        }
        return null;
    }

    public Collection<HUDElement> getAll() {
        return elements.values();
    }

    public Collection<HUDElement> getEnabled() {
        return enabledElements.get();
    }

    public boolean isElementEnabled(HUDElement element) {
        return enabledElements.get().contains(element);
    }

    public int getCount() {
        return elements.size();
    }

    @Override
    public WWidget getWidget() {
        WTable table = new WTable();
        WButton editBtn = table.add(new WButton("Open Editor")).expandX().widget();
        editBtn.action = () -> mc.setScreen(new HUDEditorScreen());
        return table;
    }

    // Lifecycle

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (Utils.isLoading()) {
            return;
        }
        if (!isActive() && !HUDEditorScreen.isOpen()) {
            return;
        }

        for (HUDElement element : getEnabled()) {
            element.tick();
        }
    }
    
    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (Utils.isLoading() || LoadingVisualGuard.shouldSuppressHud()) {
            return;
        }

        // The editor previews the elements even when the module is off or the hud is hidden, otherwise there would be
        // nothing to arrange. Drawing them from here instead of from the screen keeps them at the same point in the
        // pipeline as in game, so the editor shows the real draw order.
        if (HUDEditorScreen.isOpen()) {
            render(event.drawContext);
            return;
        }

        if (!isActive()) {
            return;
        }
        if (mc.options.hudHidden || mc.debugHudEntryList.isF3Enabled()) {
            return;
        }

        render(event.drawContext);
    }
    
    public void renderPreview(DrawContext context) {
        if (mc.world != null) {
            return;
        }

        render(context);
    }

    public void render(DrawContext context) {
        Render2D.beginFrame(context);
        try {
            for (HUDElement element : getEnabled()) {
                element.resolve();
                element.render();
            }
            Render2D.flush();
        } finally {
            Render2D.endFrame();
        }
    }

    // Serialization

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        if (json == null) {
            return null;
        }

        JsonArray elementsArray = new JsonArray();
        for (HUDElement element : getAll()) {
            JsonObject elementJson = element.toJson();
            if (elementJson != null) {
                elementsArray.add(elementJson);
            }
        }
        json.add("elements", elementsArray);

        return json;
    }

    @Override
    public HUD fromJson(JsonObject json) {
        super.fromJson(json);

        if (!json.has("elements") || !json.get("elements").isJsonArray()) {
            return this;
        }

        for (JsonElement entry : json.getAsJsonArray("elements")) {
            if (!entry.isJsonObject()) {
                continue;
            }

            JsonObject elementJson = entry.getAsJsonObject();
            if (!elementJson.has("name")) {
                continue;
            }

            HUDElement element = get(elementJson.get("name").getAsString());
            if (element != null) {
                element.fromJson(elementJson);
            }
        }

        return this;
    }

}
