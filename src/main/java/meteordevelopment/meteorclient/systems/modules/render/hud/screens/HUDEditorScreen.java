/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDElement;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUDRenderer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.other.Snapper;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class HUDEditorScreen extends WidgetScreen implements Snapper.Container {
    
    private static final Color SPLIT_LINES_COLOR = new Color(255, 255, 255, 75);
    
    private static final Color INACTIVE_BG_COLOR = new Color(200, 25, 25, 50);
    private static final Color INACTIVE_OL_COLOR = new Color(200, 25, 25, 200);
    
    private static final Color HOVER_BG_COLOR = new Color(200, 200, 200, 50);
    private static final Color HOVER_OL_COLOR = new Color(200, 200, 200, 200);
    
    private static final Color SELECTION_BG_COLOR = new Color(225, 225, 225, 25);
    private static final Color SELECTION_OL_COLOR = new Color(225, 225, 225, 100);
    
    private final HUD hud;
    
    private final Snapper snapper;
    private Snapper.Element selectionSnapBox;
    
    private int lastMouseX, lastMouseY;
    
    private boolean pressed;
    private int clickX, clickY;
    private final List<HUDElement> selection = new ArrayList<>();
    private boolean moved, dragging;
    private HUDElement addedHoveredToSelectionWhenClickedElement;
    
    private double splitLinesAnimation;
    
    public HUDEditorScreen(GuiTheme theme) {
        super(theme, "Hud Editor");
        
        hud = HUD.get();
        snapper = new Snapper(this);
    }
    
    @Override
    public void initWidgets() {}
    
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double s = mc.getWindow().getScaleFactor();
        
        double mouseX = click.x();
        double mouseY = click.y();
        
        mouseX *= s;
        mouseY *= s;
        
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            pressed = true;
            selectionSnapBox = null;
            
            HUDElement hovered = getHovered((int) mouseX, (int) mouseY);
            dragging = hovered != null;
            if (dragging) {
                if (!selection.contains(hovered)) {
                    selection.clear();
                    selection.add(hovered);
                    addedHoveredToSelectionWhenClickedElement = hovered;
                }
            } else {
                selection.clear();
            }
            
            clickX = (int) mouseX;
            clickY = (int) mouseY;
        }
        
        return false;
    }
    
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        double s = mc.getWindow().getScaleFactor();
        
        mouseX *= s;
        mouseY *= s;
        
        if (dragging && !selection.isEmpty()) {
            if (selectionSnapBox == null) {
                selectionSnapBox = new SelectionBox();
            }
            snapper.move(selectionSnapBox, (int) mouseX - lastMouseX, (int) mouseY - lastMouseY);
        }
        
        if (pressed) {
            moved = true;
        }
        
        lastMouseX = (int) mouseX;
        lastMouseY = (int) mouseY;
    }
    
    @Override
    public boolean mouseReleased(Click click) {
        double s = mc.getWindow().getScaleFactor();
        
        double mouseX = click.x();
        double mouseY = click.y();
        
        mouseX *= s;
        mouseY *= s;
        
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            pressed = false;
        }
        
        if (addedHoveredToSelectionWhenClickedElement != null) {
            selection.remove(addedHoveredToSelectionWhenClickedElement);
            addedHoveredToSelectionWhenClickedElement = null;
        }
        
        if (moved) {
            if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && !dragging) {
                fillSelection((int) mouseX, (int) mouseY);
            }
        } else {
            if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                HUDElement hovered = getHovered((int) mouseX, (int) mouseY);
                if (hovered != null) {
                    hovered.toggle();
                }
            } else if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                HUDElement hovered = getHovered((int) mouseX, (int) mouseY);
                
                if (hovered != null) {
                    mc.setScreen(new HUDElementScreen(theme, hovered));
                } else {
                    mc.setScreen(new AddHUDElementScreen(theme, lastMouseX, lastMouseY));
                }
            }
        }
        
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            snapper.unsnap();
            moved = dragging = false;
        }
        
        return false;
    }
    
    @Override
    public boolean keyPressed(KeyInput input) {
        if (!pressed) {
            if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
                HUDElement hovered = getHovered(lastMouseX, lastMouseY);
                if (hovered != null) {
                    hovered.toggle();
                }
            } else if (input.key() == GLFW.GLFW_KEY_DELETE) {
                HUDElement hovered = getHovered(lastMouseX, lastMouseY);
                
                if (hovered != null) {
                    hovered.remove();
                } else {
                    for (HUDElement element : selection) {
                        element.remove();
                    }
                    selection.clear();
                }
            } else if (!selection.isEmpty()) {
                int pixels = (Input.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) || Input.isKeyPressed(GLFW.GLFW_KEY_RIGHT_CONTROL)) ? 10 : 1;
                int dx = 0, dy = 0;
                
                switch (input.key()) {
                    case GLFW.GLFW_KEY_UP -> dy = -pixels;
                    case GLFW.GLFW_KEY_DOWN -> dy = pixels;
                    case GLFW.GLFW_KEY_RIGHT -> dx = pixels;
                    case GLFW.GLFW_KEY_LEFT -> dx = -pixels;
                }
                
                // Manually move selection to bypass snapping
                for (HUDElement element : selection) {
                    element.move(dx, dy);
                }
            }
        }
        
        return super.keyPressed(input);
    }
    
    private void fillSelection(int mouseX, int mouseY) {
        int x1 = Math.min(clickX, mouseX);
        int x2 = Math.max(clickX, mouseX);
        
        int y1 = Math.min(clickY, mouseY);
        int y2 = Math.max(clickY, mouseY);
        
        for (HUDElement element : hud) {
            if ((element.getX() <= x2 && element.getX2() >= x1) && (element.getY() <= y2 && element.getY2() >= y1)) {
                selection.add(element);
            }
        }
    }
    
    @Override
    public Iterable<Snapper.Element> getElements() {
        return () -> new Iterator<>() {
            private final Iterator<HUDElement> it = hud.iterator();
            
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }
            
            @Override
            public Snapper.Element next() {
                return it.next();
            }
        };
    }
    
    @Override
    public boolean shouldNotSnapTo(Snapper.Element element) {
        return selection.contains((HUDElement) element);
    }
    
    @Override
    public int getSnappingRange() {
        return hud.snappingRange.get();
    }
    
    private void onRender(int mouseX, int mouseY) {
        // Inactive
        for (HUDElement element : hud) {
            if (!element.isActive()) {
                renderElement(element, INACTIVE_BG_COLOR, INACTIVE_OL_COLOR);
            }
        }
        
        // Selected
        if (pressed && !dragging) {
            fillSelection(mouseX, mouseY);
        }
        for (HUDElement element : selection) {
            renderElement(element, HOVER_BG_COLOR, HOVER_OL_COLOR);
        }
        if (pressed && !dragging) {
            selection.clear();
        }
        
        // Selection
        if (pressed && !dragging) {
            int x1 = Math.min(clickX, mouseX);
            int x2 = Math.max(clickX, mouseX);
            
            int y1 = Math.min(clickY, mouseY);
            int y2 = Math.max(clickY, mouseY);
            
            renderQuad(x1, y1, x2 - x1, y2 - y1, SELECTION_BG_COLOR, SELECTION_OL_COLOR);
        }
        
        // Hovered
        if (!pressed) {
            HUDElement hovered = getHovered(mouseX, mouseY);
            if (hovered != null) {
                renderElement(hovered, HOVER_BG_COLOR, HOVER_OL_COLOR);
            }
        }
    }
    
    private HUDElement getHovered(int mouseX, int mouseY) {
        for (HUDElement element : hud) {
            if (mouseX >= element.x && mouseX <= element.x + element.getWidth() && mouseY >= element.y && mouseY <= element.y + element.getHeight()) {
                return element;
            }
        }
        
        return null;
    }
    
    private void renderQuad(double x, double y, double w, double h, Color bgColor, Color olColor) {
        Renderer2D.COLOR.quad(x + 1, y + 1, w - 2, h - 2, bgColor);
        
        Renderer2D.COLOR.quad(x, y, w, 1, olColor);
        Renderer2D.COLOR.quad(x, y + h - 1, w, 1, olColor);
        Renderer2D.COLOR.quad(x, y + 1, 1, h - 2, olColor);
        Renderer2D.COLOR.quad(x + w - 1, y + 1, 1, h - 2, olColor);
    }
    
    private void renderElement(HUDElement element, Color bgColor, Color olColor) {
        renderQuad(element.x, element.y, element.getWidth(), element.getHeight(), bgColor, olColor);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int s = mc.getWindow().getScaleFactor();
        
        mouseX *= s;
        mouseY *= s;
        
        RenderUtils.unscaledProjection();
        
        boolean renderSplitLines = pressed && !selection.isEmpty() && moved;
        if (renderSplitLines || splitLinesAnimation > 0) {
            renderSplitLines(renderSplitLines, delta / 20);
        }
        renderElements(context);
        
        Renderer2D.COLOR.begin();
        onRender(mouseX, mouseY);
        Renderer2D.COLOR.render();
        
        RenderUtils.scaledProjection();
        runAfterRenderTasks();
    }
    
    public static void renderElements(DrawContext drawContext) {
        HUD hud = HUD.get();
        boolean inactiveOnly = Utils.canUpdate() && hud.isActive();
        
        HUDRenderer.INSTANCE.begin(drawContext);
        
        for (HUDElement element : hud) {
            element.updatePos();
            
            if (inactiveOnly) {
                if (!element.isActive()) {
                    element.render(HUDRenderer.INSTANCE);
                }
            } else {
                element.render(HUDRenderer.INSTANCE);
            }
        }
        
        HUDRenderer.INSTANCE.end();
    }
    
    private void renderSplitLines(boolean increment, double delta) {
        if (increment) {
            splitLinesAnimation += delta * 6;
        } else {
            splitLinesAnimation -= delta * 6;
        }
        splitLinesAnimation = MathHelper.clamp(splitLinesAnimation, 0, 1);
        
        Renderer2D renderer = Renderer2D.COLOR;
        renderer.begin();
        
        double w = RenderUtils.getWindowWidth();
        double h = RenderUtils.getWindowHeight();
        double w3 = w / 3.0;
        double h3 = h / 3.0;
        
        int prevA = SPLIT_LINES_COLOR.a;
        SPLIT_LINES_COLOR.a *= splitLinesAnimation;
        
        renderSplitLine(renderer, w3, 0, w3, h);
        renderSplitLine(renderer, w3 * 2, 0, w3 * 2, h);
        
        renderSplitLine(renderer, 0, h3, w, h3);
        renderSplitLine(renderer, 0, h3 * 2, w, h3 * 2);
        
        SPLIT_LINES_COLOR.a = prevA;
        
        renderer.render();
    }
    
    private void renderSplitLine(Renderer2D renderer, double x, double y, double destX, double destY) {
        double incX = 0;
        double incY = 0;
        
        if (x == destX) {
            incY = RenderUtils.getWindowWidth() / 25.0;
        } else {
            incX = RenderUtils.getWindowWidth() / 25.0;
        }
        
        do {
            renderer.line(x, y, x + incX, y + incY, SPLIT_LINES_COLOR);
            
            x += incX * 2;
            y += incY * 2;
            
        } while (!(x >= destX) || !(y >= destY));
    }
    
    public static boolean isOpen() {
        Screen s = mc.currentScreen;
        return s instanceof HUDEditorScreen || s instanceof AddHUDElementScreen || s instanceof HUDElementPresetsScreen || s instanceof HUDElementScreen;
    }
    
    private class SelectionBox implements Snapper.Element {
        
        private int x, y;
        private final int width, height;
        
        public SelectionBox() {
            int x1 = Integer.MAX_VALUE;
            int y1 = Integer.MAX_VALUE;
            
            int x2 = 0;
            int y2 = 0;
            
            for (HUDElement element : selection) {
                if (element.getX() < x1) {
                    x1 = element.getX();
                } else if (element.getX() > x2) {
                    x2 = element.getX();
                }
                
                if (element.getX2() < x1) {
                    x1 = element.getX2();
                } else if (element.getX2() > x2) {
                    x2 = element.getX2();
                }
                
                if (element.getY() < y1) {
                    y1 = element.getY();
                } else if (element.getY() > y2) {
                    y2 = element.getY();
                }
                
                if (element.getY2() < y1) {
                    y1 = element.getY2();
                } else if (element.getY2() > y2) {
                    y2 = element.getY2();
                }
            }
            
            this.x = x1;
            this.y = y1;
            this.width = x2 - x1;
            this.height = y2 - y1;
        }
        
        @Override
        public int getX() {
            return x;
        }
        
        @Override
        public int getY() {
            return y;
        }
        
        @Override
        public int getWidth() {
            return width;
        }
        
        @Override
        public int getHeight() {
            return height;
        }
        
        @Override
        public void setPos(int x, int y) {
            for (HUDElement element : selection) {
                element.setPos(x + (element.x - this.x), y + (element.y - this.y));
            }
            
            this.x = x;
            this.y = y;
        }
        
        @Override
        public void move(int deltaX, int deltaY) {
            int lastX = x;
            int lastY = y;
            
            int border = HUD.get().border.get();
            x = MathHelper.clamp(x + deltaX, border, RenderUtils.getWindowWidth() - width - border);
            y = MathHelper.clamp(y + deltaY, border, RenderUtils.getWindowHeight() - height - border);
            
            for (HUDElement element : selection) {
                element.move(x - lastX, y - lastY);
            }
        }
        
    }
    
}
