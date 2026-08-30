/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.IMinecraft;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import net.minecraft.util.math.MathHelper;

public abstract class HUDElement implements ISerializable<HUDElement>, IMinecraft {

    public final Settings settings = new Settings();

    private final String name;

    private XAnchor anchorX = XAnchor.LEFT;
    private YAnchor anchorY = YAnchor.TOP;
    private int offsetX, offsetY;

    private int width, height;
    private int x, y;

    public HUDElement(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // Layout
    
    protected void updateSize() {}
    
    public void resolve() {
        updateSize();

        int screenWidth = Render2D.independentWidth();
        int screenHeight = Render2D.independentHeight();

        x = switch (anchorX) {
            case LEFT -> offsetX;
            case CENTER -> (screenWidth - width) / 2 + offsetX;
            case RIGHT -> screenWidth - width - offsetX;
        };

        y = switch (anchorY) {
            case TOP -> offsetY;
            case CENTER -> (screenHeight - height) / 2 + offsetY;
            case BOTTOM -> screenHeight - height - offsetY;
        };
    }
    
    public void setAbsolutePos(int newX, int newY) {
        int screenWidth = Render2D.independentWidth();
        int screenHeight = Render2D.independentHeight();

        clampTo(newX, newY);

        int centerX = x + width / 2;
        int centerY = y + height / 2;

        anchorX = centerX < screenWidth / 3 ? XAnchor.LEFT
            : centerX > screenWidth * 2 / 3 ? XAnchor.RIGHT
            : XAnchor.CENTER;

        anchorY = centerY < screenHeight / 3 ? YAnchor.TOP
            : centerY > screenHeight * 2 / 3 ? YAnchor.BOTTOM
            : YAnchor.CENTER;

        recomputeOffsets();
    }
    
    public void nudge(int deltaX, int deltaY) {
        clampTo(x + deltaX, y + deltaY);
        recomputeOffsets();
    }

    private void clampTo(int newX, int newY) {
        x = MathHelper.clamp(newX, 0, Math.max(0, Render2D.independentWidth() - width));
        y = MathHelper.clamp(newY, 0, Math.max(0, Render2D.independentHeight() - height));
    }
    
    private void recomputeOffsets() {
        int screenWidth = Render2D.independentWidth();
        int screenHeight = Render2D.independentHeight();

        offsetX = switch (anchorX) {
            case LEFT -> x;
            case CENTER -> x - (screenWidth - width) / 2;
            case RIGHT -> screenWidth - width - x;
        };

        offsetY = switch (anchorY) {
            case TOP -> y;
            case CENTER -> y - (screenHeight - height) / 2;
            case BOTTOM -> screenHeight - height - y;
        };
    }

    public XAnchor getAnchorX() {
        return anchorX;
    }

    public YAnchor getAnchorY() {
        return anchorY;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    
    protected int alignX(int contentWidth) {
        return switch (anchorX) {
            case LEFT -> x;
            case CENTER -> x + (width - contentWidth) / 2;
            case RIGHT -> x + width - contentWidth;
        };
    }

    // Lifecycle
    
    protected boolean isInEditor() {
        return HUDEditorScreen.isOpen() || !Utils.canUpdate();
    }

    public void tick() {}

    public void render() {};

    public WWidget getWidget() {
        return null;
    }
    
    public boolean hasSettings() {
        return !settings.groups.isEmpty() || getWidget() != null;
    }

    @Override
    public String toString() {
        return name;
    }

    // Serialization

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("anchorX", anchorX.name());
        json.addProperty("anchorY", anchorY.name());
        json.addProperty("offsetX", offsetX);
        json.addProperty("offsetY", offsetY);
        json.add("settings", settings.toJson());
        return json;
    }

    @Override
    public HUDElement fromJson(JsonObject json) {
        settings.reset();

        if (json.has("settings") && json.get("settings").isJsonObject()) {
            settings.fromJson(json.getAsJsonObject("settings"));
        }

        if (json.has("anchorX")) {
            anchorX = parseEnum(json.get("anchorX").getAsString(), XAnchor.values(), XAnchor.LEFT);
        }
        if (json.has("anchorY")) {
            anchorY = parseEnum(json.get("anchorY").getAsString(), YAnchor.values(), YAnchor.TOP);
        }
        if (json.has("offsetX")) {
            offsetX = json.get("offsetX").getAsInt();
        }
        if (json.has("offsetY")) {
            offsetY = json.get("offsetY").getAsInt();
        }

        return this;
    }

    private static <T extends Enum<T>> T parseEnum(String value, T[] values, T fallback) {
        for (T candidate : values) {
            if (candidate.name().equalsIgnoreCase(value)) {
                return candidate;
            }
        }
        return fallback;
    }

}
