/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud;

import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class HUDEditorScreen extends WidgetScreen {

    private static final Color HOVER_OUTLINE = new Color(200, 200, 200, 200);
    private static final Color SELECTION_FILL = new Color(225, 225, 225, 25);
    private static final Color SELECTION_OUTLINE = new Color(225, 225, 225, 255);
    private static final Color ANCHOR_MARKER = new Color(120, 200, 255, 200);

    private final HUD hud = HUD.get();

    private @Nullable HUDElement selected;
    private @Nullable HUDElement dragged;

    private int grabX, grabY;
    private boolean movedWhileDragging;

    public HUDEditorScreen() {
        super("HUD Editor");
    }

    @Override
    public void initWidgets() {}

    // Input
    
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mouseX = (int) Render2D.toIndependent(click.x());
        int mouseY = (int) Render2D.toIndependent(click.y());

        HUDElement hovered = getHovered(mouseX, mouseY);

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            selected = hovered;

            if (hovered != null) {
                dragged = hovered;
                grabX = mouseX - hovered.getX();
                grabY = mouseY - hovered.getY();
                movedWhileDragging = false;
            }

            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (dragged != null) {
            int x = (int) Render2D.toIndependent(mouseX);
            int y = (int) Render2D.toIndependent(mouseY);

            dragged.setAbsolutePos(x - grabX, y - grabY);
            movedWhileDragging = true;
        }

        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        int mouseX = (int) Render2D.toIndependent(click.x());
        int mouseY = (int) Render2D.toIndependent(click.y());

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragged = null;
            movedWhileDragging = false;
            return true;
        }

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            HUDElement hovered = getHovered(mouseX, mouseY);
            if (hovered != null) {
                selected = hovered;

                // Elements with nothing but a position are configured by dragging them, so there would be nothing to
                // put in the screen.
                if (hovered.hasSettings()) {
                    mc.setScreen(new HUDElementScreen(hovered));
                }

                return true;
            }
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (selected != null && dragged == null) {
            int step = Input.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) || Input.isKeyPressed(GLFW.GLFW_KEY_RIGHT_CONTROL) ? 10 : 1;

            int deltaX = 0, deltaY = 0;
            switch (input.key()) {
                case GLFW.GLFW_KEY_LEFT -> deltaX = -step;
                case GLFW.GLFW_KEY_RIGHT -> deltaX = step;
                case GLFW.GLFW_KEY_UP -> deltaY = -step;
                case GLFW.GLFW_KEY_DOWN -> deltaY = step;
            }

            if (deltaX != 0 || deltaY != 0) {
                selected.nudge(deltaX, deltaY);
                return true;
            }
        }

        return super.keyPressed(input);
    }

    private @Nullable HUDElement getHovered(int mouseX, int mouseY) {
        HUDElement hovered = null;

        // Later elements draw on top, so the last match is the one the user sees under the cursor.
        for (HUDElement element : hud.getEnabled()) {
            if (element.isHovered(mouseX, mouseY)) {
                hovered = element;
            }
        }

        return hovered;
    }

    // Rendering

    @Override
    protected void onRenderBefore(DrawContext context, float delta) {
        // Only draws the elements when there is no world. With one, HUD.onRender2D has already drawn them from the
        // same place it does in game, and drawing them again here would put them above the vanilla hud instead of
        // below it - exactly the difference the editor is meant to hide.
        hud.renderPreview(context);
    }

    @Override
    public void renderCustom(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderCustom(context, mouseX, mouseY, delta);

        int x = (int) Math.round(Render2D.toIndependent(mouseX));
        int y = (int) Math.round(Render2D.toIndependent(mouseY));

        Render2D.beginFrame(context);
        try {
            HUDElement hovered = getHovered(x, y);
            if (hovered != null && hovered != selected) {
                outline(hovered, HOVER_OUTLINE);
            }

            if (selected != null) {
                Render2D.rect(selected.getX(), selected.getY(), selected.getWidth(), selected.getHeight(), SELECTION_FILL.getPacked());
                outline(selected, SELECTION_OUTLINE);
                renderAnchorMarkers(selected);
            }

            Render2D.flush();
        } finally {
            Render2D.endFrame();
        }
    }

    private void outline(HUDElement element, Color color) {
        Render2D.outline(element.getX(), element.getY(), element.getWidth(), element.getHeight(), 0, 1, color.getPacked());
    }

    /**
     * Marks the screen edges the selected element is pinned to, so it is obvious which way it will move when the
     * resolution changes.
     */
    private void renderAnchorMarkers(HUDElement element) {
        int screenWidth = Render2D.independentWidth();
        int screenHeight = Render2D.independentHeight();
        int color = ANCHOR_MARKER.getPacked();
        int thickness = 2;
        int length = 24;

        int centerY = element.getY() + element.getHeight() / 2;
        switch (element.getAnchorX()) {
            case LEFT -> Render2D.rect(0, centerY - length / 2f, thickness, length, color);
            case RIGHT -> Render2D.rect(screenWidth - thickness, centerY - length / 2f, thickness, length, color);
            case CENTER -> Render2D.rect(screenWidth / 2f - thickness / 2f, centerY - length / 2f, thickness, length, color);
        }

        int centerX = element.getX() + element.getWidth() / 2;
        switch (element.getAnchorY()) {
            case TOP -> Render2D.rect(centerX - length / 2f, 0, length, thickness, color);
            case BOTTOM -> Render2D.rect(centerX - length / 2f, screenHeight - thickness, length, thickness, color);
            case CENTER -> Render2D.rect(centerX - length / 2f, screenHeight / 2f - thickness / 2f, length, thickness, color);
        }
    }

    public static boolean isOpen() {
        return mc.currentScreen instanceof HUDEditorScreen || mc.currentScreen instanceof HUDElementScreen;
    }

}
