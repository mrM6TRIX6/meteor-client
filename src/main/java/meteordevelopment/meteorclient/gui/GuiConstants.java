/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.utils.WindowConfig;
import meteordevelopment.meteorclient.renderer.color.Color;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import meteordevelopment.meteorclient.utils.render.ui.msdf.BuiltMsdf;
import meteordevelopment.meteorclient.utils.render.ui.msdf.MsdfFont;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Temporary replacement for the old theme system. Holds every value the gui needs: colors, metrics and icons.
 */
public class GuiConstants {

    private GuiConstants() {}

    // Icons

    public static final Identifier CIRCLE = MeteorClient.identifier("textures/icons/gui/circle.png");
    public static final Identifier TRIANGLE = MeteorClient.identifier("textures/icons/gui/triangle.png");
    public static final Identifier EDIT = MeteorClient.identifier("textures/icons/gui/edit.png");
    public static final Identifier RESET = MeteorClient.identifier("textures/icons/gui/reset.png");
    public static final Identifier FAVORITE_NO = MeteorClient.identifier("textures/icons/gui/favorite_no.png");
    public static final Identifier FAVORITE_YES = MeteorClient.identifier("textures/icons/gui/favorite_yes.png");

    // Metrics

    public static final MsdfFont FONT = MsdfFont.MONTSERRAT_MEDIUM;

    public static final double SCALE = 1.125;
    public static final double TEXT_SIZE = 12;
    public static final double TITLE_TEXT_SCALE = 1.25;

    // Layout

    public static final AlignmentX MODULE_ALIGNMENT = AlignmentX.CENTER;
    public static final boolean CATEGORY_ICONS = false;
    public static final boolean HIDE_HUD = false;

    // Colors

    public static final Color ACCENT = new Color(145, 61, 226);
    public static final Color CHECKBOX = new Color(145, 61, 226);
    public static final Color PLUS = new Color(50, 255, 50);
    public static final Color MINUS = new Color(255, 50, 50);
    public static final Color FAVORITE = new Color(250, 215, 0);

    public static final Color TEXT = new Color(255, 255, 255);
    public static final Color TEXT_SECONDARY = new Color(150, 150, 150);
    public static final Color TEXT_HIGHLIGHT = new Color(200, 200, 200, 50);
    public static final Color TITLE_TEXT = new Color(255, 255, 255);
    public static final Color LOGGED_IN = new Color(45, 225, 45);
    public static final Color PLACEHOLDER = new Color(255, 255, 255, 20);

    public static final Color MODULE_BACKGROUND = new Color(50, 50, 50);

    public static final Color SEPARATOR_TEXT = new Color(255, 255, 255);
    public static final Color SEPARATOR_CENTER = new Color(255, 255, 255);
    public static final Color SEPARATOR_EDGES = new Color(225, 225, 225, 150);

    public static final Color SLIDER_LEFT = new Color(100, 35, 170);
    public static final Color SLIDER_RIGHT = new Color(50, 50, 50);

    public static final ThreeStateColor BACKGROUND = new ThreeStateColor(new Color(20, 20, 20, 200), new Color(30, 30, 30, 200), new Color(40, 40, 40, 200));
    public static final ThreeStateColor OUTLINE = new ThreeStateColor(new Color(0, 0, 0), new Color(10, 10, 10), new Color(20, 20, 20));
    public static final ThreeStateColor SCROLLBAR = new ThreeStateColor(new Color(30, 30, 30, 200), new Color(40, 40, 40, 200), new Color(50, 50, 50, 200));
    public static final ThreeStateColor SLIDER_HANDLE = new ThreeStateColor(new Color(130, 0, 255), new Color(140, 30, 255), new Color(150, 60, 255));

    // Starscript

    public static final Color STARSCRIPT_TEXT = new Color(169, 183, 198);
    public static final Color STARSCRIPT_BRACES = new Color(150, 150, 150);
    public static final Color STARSCRIPT_PARENTHESIS = new Color(169, 183, 198);
    public static final Color STARSCRIPT_DOTS = new Color(169, 183, 198);
    public static final Color STARSCRIPT_COMMAS = new Color(169, 183, 198);
    public static final Color STARSCRIPT_OPERATORS = new Color(169, 183, 198);
    public static final Color STARSCRIPT_STRINGS = new Color(106, 135, 89);
    public static final Color STARSCRIPT_NUMBERS = new Color(104, 141, 187);
    public static final Color STARSCRIPT_KEYWORDS = new Color(204, 120, 50);
    public static final Color STARSCRIPT_ACCESSED_OBJECTS = new Color(152, 118, 170);

    // Render state

    /**
     * Multiplied into every color returned by {@link #color(Color)}. Used for the gui open/close fade, tooltips and the text box cursor.
     */
    public static float alpha = 1;

    /**
     * Set by widgets which draw their own hovered state on top of children, so the children don't highlight themselves as well.
     */
    public static boolean disableHoverColor;

    public static void beforeRender() {
        disableHoverColor = false;
    }

    /**
     * Packs a color into ARGB, applying the current global {@link #alpha}.
     */
    public static int color(Color color) {
        if (alpha >= 1) {
            return color.getPacked();
        }
        return (color.r << 16) | (color.g << 8) | color.b | (Math.round(color.a * alpha) << 24);
    }

    public static int color(Color color, float alphaMultiplier) {
        return (color.r << 16) | (color.g << 8) | color.b | (Math.round(color.a * alpha * alphaMultiplier) << 24);
    }

    /**
     * Same color with its alpha bumped up by 50%, used for elements drawn on top of a background of the same color.
     */
    public static int brighter(Color color) {
        int a = Math.min(255, color.a + color.a / 2);
        return (color.r << 16) | (color.g << 8) | color.b | (Math.round(a * alpha) << 24);
    }

    // Metric helpers

    public static double scale(double value) {
        return value * SCALE;
    }
    public static double pad() {
        return scale(6);
    }

    /**
     * Rounded to a whole number so that {@link #textWidth(String, boolean)} measures exactly what
     * {@code Render2D.msdf} renders (the msdf builder takes an int size).
     */
    public static double textSize(boolean title) {
        return Math.round(scale(TEXT_SIZE * (title ? TITLE_TEXT_SCALE : 1)));
    }

    public static double textSize() {
        return textSize(false);
    }

    public static double textHeight(boolean title) {
        return textSize(title);
    }

    public static double textHeight() {
        return textHeight(false);
    }

    public static double textWidth(String text, int length, boolean title) {
        if (text.isEmpty() || length <= 0) {
            return 0;
        }
        if (length > text.length()) {
            length = text.length();
        }
        return MsdfFont.MONTSERRAT_REGULAR.width(text.substring(0, length), (float) textSize(title));
    }

    public static double textWidth(String text, boolean title) {
        return textWidth(text, text.length(), title);
    }

    public static double textWidth(String text) {
        return textWidth(text, text.length(), false);
    }

    /**
     * The only place text is turned into a draw call, {@link meteordevelopment.meteorclient.gui.widgets.WWidget#text}
     * and the text box renderers all go through here.
     */
    public static void text(String text, double x, double y, Color color, boolean title) {
        Render2D.msdf(new BuiltMsdf(MsdfFont.MONTSERRAT_REGULAR, text, (int) x, (int) y, (int) textSize(title), color(color)));
    }

    public static void text(String text, double x, double y, Color color) {
        text(text, x, y, color, false);
    }

    // Window configs

    private static final Map<String, WindowConfig> WINDOW_CONFIGS = new HashMap<>();

    public static WindowConfig getWindowConfig(String id) {
        return WINDOW_CONFIGS.computeIfAbsent(id, key -> new WindowConfig());
    }

    public static void clearWindowConfigs() {
        WINDOW_CONFIGS.clear();
    }

    /**
     * A color which changes depending on whether the widget is pressed or hovered.
     */
    public record ThreeStateColor(Color normal, Color hovered, Color pressed) {

        public Color get(boolean pressed, boolean hovered) {
            return get(pressed, hovered, false);
        }

        public Color get(boolean pressed, boolean hovered, boolean bypassDisableHoverColor) {
            if (pressed) {
                return this.pressed;
            }
            if (hovered && (bypassDisableHoverColor || !disableHoverColor)) {
                return this.hovered;
            }
            return normal;
        }

        public Color get() {
            return normal;
        }

    }

}
