/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings.impl;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WQuad;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class ColorSettingScreen extends WindowScreen {
    
    private static final Color[] HUE_COLORS = {
        Color.RED,
        Color.YELLOW,
        Color.GREEN,
        Color.CYAN,
        Color.BLUE,
        Color.MAGENTA,
        Color.RED
    };
    
    public Runnable action;
    
    private final Setting<Color> setting;
    
    private WQuad displayQuad;
    
    private WBrightnessQuad brightnessQuad;
    private WHueQuad hueQuad;
    
    private WIntEdit rItb, gItb, bItb, aItb;
    private WCheckbox rainbow;
    
    public ColorSettingScreen(Setting<Color> setting) {
        super("Select Color");
        
        this.setting = setting;
    }
    
    @Override
    public boolean toClipboard() {
        String color = setting.get().toString().replace(" ", ",");
        mc.keyboard.setClipboard(color);
        return mc.keyboard.getClipboard().equals(color);
    }
    
    @Override
    public boolean fromClipboard() {
        String clipboard = mc.keyboard.getClipboard().trim();
        Color parsed;
        
        if ((parsed = parseRGBA(clipboard)) != null) {
            setting.set(parsed);
            setting.get().validate();
            return true;
        }
        
        if ((parsed = parseHex(clipboard)) != null) {
            setting.set(parsed);
            setting.get().validate();
            return true;
        }
        
        return false;
    }
    
    private Color parseRGBA(String string) {
        String[] rgba = string.replaceAll("[^0-9|,]", "").split(",");
        if (rgba.length < 3 || rgba.length > 4) {
            return null;
        }
        
        Color color;
        try {
            color = new Color(Integer.parseInt(rgba[0]), Integer.parseInt(rgba[1]), Integer.parseInt(rgba[2]));
            if (rgba.length == 4) {
                color.a = Integer.parseInt(rgba[3]);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        
        return color;
    }
    
    private Color parseHex(String string) {
        if (!string.startsWith("#")) {
            return null;
        }
        String hex = string.toLowerCase().replaceAll("[^0-9a-f]", "");
        if (hex.length() != 6 && hex.length() != 8) {
            return null;
        }
        
        Color color;
        try {
            color = new Color(
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16)
            );
            if (hex.length() == 8) {
                color.a = Integer.parseInt(hex.substring(6, 8), 16);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        
        return color;
    }
    
    @Override
    public void initWidgets() {
        // Top
        displayQuad = add(new WQuad(setting.get())).expandX().widget();
        
        brightnessQuad = add(new WBrightnessQuad()).expandX().widget();
        
        hueQuad = add(new WHueQuad()).expandX().widget();
        
        // RGBA
        WTable rgbaTable = add(new WTable()).expandX().widget();
        
        rgbaTable.add(new WLabel("R:"));
        rItb = rgbaTable.add(new WIntEdit(setting.get().r, 0, 255, 0, 255, false)).expandX().widget();
        rItb.action = this::rgbaChanged;
        rgbaTable.row();
        
        rgbaTable.add(new WLabel("G:"));
        gItb = rgbaTable.add(new WIntEdit(setting.get().g, 0, 255, 0, 255, false)).expandX().widget();
        gItb.action = this::rgbaChanged;
        rgbaTable.row();
        
        rgbaTable.add(new WLabel("B:"));
        bItb = rgbaTable.add(new WIntEdit(setting.get().b, 0, 255, 0, 255, false)).expandX().widget();
        bItb.action = this::rgbaChanged;
        rgbaTable.row();
        
        rgbaTable.add(new WLabel("A:"));
        aItb = rgbaTable.add(new WIntEdit(setting.get().a, 0, 255, 0, 255, false)).expandX().widget();
        aItb.action = this::rgbaChanged;
        
        // Bottom
        WHorizontalList bottomList = add(new WHorizontalList()).expandX().widget();
        
        WButton backButton = bottomList.add(new WButton("Back")).expandX().widget();
        backButton.action = this::close;
        
        WButton resetButton = bottomList.add(new WButton(GuiConstants.RESET)).widget();
        resetButton.action = () -> {
            setting.reset();
            setFromSetting();
            callAction();
        };
        resetButton.tooltip = "Reset";
        
        hueQuad.calculateFromSetting(false);
        brightnessQuad.calculateFromColor(setting.get(), false);
    }
    
    private void setFromSetting() {
        Color c = setting.get();
        
        if (c.r != rItb.get()) {
            rItb.set(c.r);
        }
        if (c.g != gItb.get()) {
            gItb.set(c.g);
        }
        if (c.b != bItb.get()) {
            bItb.set(c.b);
        }
        if (c.a != aItb.get()) {
            aItb.set(c.a);
        }
        
        displayQuad.color.set(setting.get());
        hueQuad.calculateFromSetting(true);
        brightnessQuad.calculateFromColor(setting.get(), true);
    }
    
    private void callAction() {
        if (action != null) {
            action.run();
        }
    }
    
    private void rgbaChanged() {
        Color c = setting.get();
        
        c.r = rItb.get();
        c.g = gItb.get();
        c.b = bItb.get();
        c.a = aItb.get();
        
        c.validate();
        
        if (c.r != rItb.get()) {
            rItb.set(c.r);
        }
        if (c.g != gItb.get()) {
            gItb.set(c.g);
        }
        if (c.b != bItb.get()) {
            bItb.set(c.b);
        }
        if (c.a != aItb.get()) {
            aItb.set(c.a);
        }
        
        displayQuad.color.set(c);
        hueQuad.calculateFromSetting(true);
        brightnessQuad.calculateFromColor(setting.get(), true);
        
        setting.onChanged();
        callAction();
    }
    
    private void hsvChanged() {
        double hh, p, q, t, ff;
        int i;
        
        double r = 0;
        double g = 0;
        double b = 0;
        boolean calculated = false;
        
        if (brightnessQuad.saturation <= 0.0) {
            r = brightnessQuad.value;
            g = brightnessQuad.value;
            b = brightnessQuad.value;
            calculated = true;
        }
        
        if (!calculated) {
            hh = hueQuad.hueAngle;
            if (hh >= 360.0) {
                hh = 0.0;
            }
            hh /= 60.0;
            i = (int) hh;
            ff = hh - i;
            p = brightnessQuad.value * (1.0 - brightnessQuad.saturation);
            q = brightnessQuad.value * (1.0 - (brightnessQuad.saturation * ff));
            t = brightnessQuad.value * (1.0 - (brightnessQuad.saturation * (1.0 - ff)));
            
            switch (i) {
                case 0 -> {
                    r = brightnessQuad.value;
                    g = t;
                    b = p;
                }
                case 1 -> {
                    r = q;
                    g = brightnessQuad.value;
                    b = p;
                }
                case 2 -> {
                    r = p;
                    g = brightnessQuad.value;
                    b = t;
                }
                case 3 -> {
                    r = p;
                    g = q;
                    b = brightnessQuad.value;
                }
                case 4 -> {
                    r = t;
                    g = p;
                    b = brightnessQuad.value;
                }
                default -> {
                    r = brightnessQuad.value;
                    g = p;
                    b = q;
                }
            }
        }
        
        Color c = setting.get();
        
        c.r = (int) (r * 255);
        c.g = (int) (g * 255);
        c.b = (int) (b * 255);
        c.validate();
        
        rItb.set(c.r);
        gItb.set(c.g);
        bItb.set(c.b);
        
        displayQuad.color.set(c);
        setting.onChanged();
        callAction();
    }
    
    private class WBrightnessQuad extends WWidget {
        
        double saturation, value;
        
        double handleX, handleY;
        
        boolean dragging;
        double lastMouseX, lastMouseY;
        
        double fixedHeight = -1;
        
        @Override
        protected void onCalculateSize() {
            double s = GuiConstants.scale(75);
            
            width = s;
            height = s;
            
            if (fixedHeight != -1) {
                height = fixedHeight;
                fixedHeight = -1;
            }
        }
        
        void calculateFromColor(Color c, boolean calculateNow) {
            double min = Math.min(Math.min(c.r, c.g), c.b);
            double max = Math.max(Math.max(c.r, c.g), c.b);
            double delta = max - min;
            
            value = max / 255;
            
            if (delta == 0) {
                saturation = 0;
            } else {
                saturation = delta / max;
            }
            
            if (calculateNow) {
                handleX = saturation * width;
                handleY = (1 - value) * height;
            }
        }
        
        @Override
        public boolean onMouseClicked(Click click, boolean doubled) {
            if (doubled) {
                return false;
            }
            
            if (mouseOver) {
                dragging = true;
                setFocused(true);
                
                handleX = lastMouseX - x;
                handleY = lastMouseY - y;
                handleMoved();
                
                return true;
            }
            
            return false;
        }
        
        @Override
        public boolean onMouseReleased(Click click) {
            if (dragging) {
                dragging = false;
                setFocused(false);
            }
            
            return false;
        }
        
        @Override
        public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
            if (dragging) {
                if (mouseX >= this.x && mouseX <= this.x + width) {
                    handleX += mouseX - lastMouseX;
                } else {
                    if (handleX > 0 && mouseX < this.x) {
                        handleX = 0;
                    } else if (handleX < width && mouseX > this.x + width) {
                        handleX = width;
                    }
                }
                
                if (mouseY >= this.y && mouseY <= this.y + height) {
                    handleY += mouseY - lastMouseY;
                } else {
                    if (handleY > 0 && mouseY < this.y) {
                        handleY = 0;
                    } else if (handleY < height && mouseY > this.y + height) {
                        handleY = height;
                    }
                }
                
                handleMoved();
            }
            
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
        }
        
        void handleMoved() {
            double handleXPercendisplayNamee = handleX / width;
            double handleYPercendisplayNamee = handleY / height;
            
            saturation = handleXPercendisplayNamee;
            value = 1 - handleYPercendisplayNamee;
            
            hsvChanged();
        }
        
        @Override
        protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
            if (height != width) {
                fixedHeight = width;
                invalidate();

                handleX = saturation * width;
                handleY = (1 - value) * fixedHeight;
            }

            hueQuad.calculateColor();

            // White in the top left corner fading to the hue in the top right one, black along the bottom edge.
            Render2D.rect((float) x, (float) y, (float) width, (float) height,
                GuiConstants.color(Color.WHITE), GuiConstants.color(hueQuad.color), GuiConstants.color(Color.BLACK), GuiConstants.color(Color.BLACK));

            double s = GuiConstants.scale(2);
            rect(x + handleX - s / 2, y + handleY - s / 2, s, s, Color.WHITE);
        }
        
    }
    
    private class WHueQuad extends WWidget {
        
        private double hueAngle;
        private double handleX;
        
        private final Color color = new Color();
        
        private boolean dragging;
        private double lastMouseX;
        
        private boolean calculateHandleXOnLayout;
        
        @Override
        protected void onCalculateSize() {
            width = GuiConstants.scale(75);
            height = GuiConstants.scale(10);
        }
        
        void calculateFromSetting(boolean calculateNow) {
            Color c = setting.get();
            boolean calculated = false;
            
            double min, max, delta;
            
            min = Math.min(c.r, c.g);
            min = min < c.b ? min : c.b;
            
            max = Math.max(c.r, c.g);
            max = max > c.b ? max : c.b;
            
            delta = max - min;
            if (delta < 0.00001) {
                hueAngle = 0;
                calculated = true;
            }
            
            if (!calculated) {
                if (max <= 0.0) { // NOTE: if Max is == 0, this divide would cause a crash
                    // if max is 0, then r = g = b = 0
                    // s = 0, h is undefined
                    hueAngle = 0;
                    calculated = true;
                }
                
                if (!calculated) {
                    if (c.r >= max) {
                        hueAngle = (c.g - c.b) / delta; // between yellow & magenta
                    } else if (c.g >= max) {
                        hueAngle = 2.0 + (c.b - c.r) / delta; // between cyan & yellow
                    } else {
                        hueAngle = 4.0 + (c.r - c.g) / delta; // between magenta & cyan
                    }
                    
                    hueAngle *= 60.0; // degrees
                    
                    if (hueAngle < 0.0) {
                        hueAngle += 360.0;
                    }
                }
            }
            
            if (calculateNow) {
                double huePercendisplayNamee = hueAngle / 360;
                handleX = huePercendisplayNamee * width;
            } else {
                calculateHandleXOnLayout = true;
            }
        }
        
        @Override
        protected void onCalculateWidgetPositions() {
            if (calculateHandleXOnLayout) {
                double huePercendisplayNamee = hueAngle / 360;
                handleX = huePercendisplayNamee * width;
                
                calculateHandleXOnLayout = false;
            }
            
            super.onCalculateWidgetPositions();
        }
        
        void calculateColor() {
            double hh, p, q, t, ff;
            int i;
            
            hh = hueAngle;
            if (hh >= 360.0) {
                hh = 0.0;
            }
            hh /= 60.0;
            i = (int) hh;
            ff = hh - i;
            p = 1 * (1.0 - 1);
            q = 1 * (1.0 - (1 * ff));
            t = 1 * (1.0 - (1 * (1.0 - ff)));
            
            double r;
            double g;
            double b;
            
            switch (i) {
                case 0 -> {
                    r = 1;
                    g = t;
                    b = p;
                }
                case 1 -> {
                    r = q;
                    g = 1;
                    b = p;
                }
                case 2 -> {
                    r = p;
                    g = 1;
                    b = t;
                }
                case 3 -> {
                    r = p;
                    g = q;
                    b = 1;
                }
                case 4 -> {
                    r = t;
                    g = p;
                    b = 1;
                }
                default -> {
                    r = 1;
                    g = p;
                    b = q;
                }
            }
            
            color.r = (int) (r * 255);
            color.g = (int) (g * 255);
            color.b = (int) (b * 255);
            color.validate();
        }
        
        @Override
        public boolean onMouseClicked(Click click, boolean doubled) {
            if (doubled) {
                return false;
            }
            
            if (mouseOver) {
                dragging = true;
                setFocused(true);
                
                handleX = lastMouseX - x;
                calculateHueAngleFromHandleX();
                hsvChanged();
                
                return true;
            }
            
            return false;
        }
        
        @Override
        public boolean onMouseReleased(Click click) {
            if (dragging) {
                dragging = false;
                setFocused(false);
            }
            
            return mouseOver;
        }
        
        @Override
        public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
            if (dragging) {
                if (mouseX >= this.x && mouseX <= this.x + width) {
                    handleX += mouseX - lastMouseX;
                    handleX = MathHelper.clamp(handleX, 0, width);
                } else {
                    if (handleX > 0 && mouseX < this.x) {
                        handleX = 0;
                    } else if (handleX < width && mouseX > this.x + width) {
                        handleX = width;
                    }
                }
                
                calculateHueAngleFromHandleX();
                hsvChanged();
            }
            
            this.lastMouseX = mouseX;
        }
        
        void calculateHueAngleFromHandleX() {
            double handleXPercendisplayNamee = handleX / (width - 4);
            hueAngle = handleXPercendisplayNamee * 360;
        }
        
        @Override
        protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
            double sectionWidth = (width) / (HUE_COLORS.length - 1);
            double sectionX = x;

            for (int i = 0; i < HUE_COLORS.length - 1; i++) {
                int left = GuiConstants.color(HUE_COLORS[i]);
                int right = GuiConstants.color(HUE_COLORS[i + 1]);

                Render2D.rect((float) sectionX, (float) y, (float) sectionWidth, (float) height, left, right, right, left);
                sectionX += sectionWidth;
            }

            double s = GuiConstants.scale(2);
            rect(x + handleX - s / 2, y, s, height, Color.WHITE);
        }
        
    }
    
}
