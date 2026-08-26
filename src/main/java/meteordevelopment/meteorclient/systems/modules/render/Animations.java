/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.game.ScreenOpenEvent;
import meteordevelopment.meteorclient.events.render.ChangeTabVisibleEvent;
import meteordevelopment.meteorclient.events.render.RenderInventoryEvent;
import meteordevelopment.meteorclient.events.render.RenderTabEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.MultiChoiceSetting;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.animation.Animation;
import meteordevelopment.meteorclient.utils.render.animation.Animator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

import java.util.SequencedSet;

public class Animations extends Module {
    
    private static final float TAB_TOP = 9;
    
    private static final Animation OPEN_ANIMATION = Animation.EASE_OUT_BACK;
    private static final Animation CLOSE_ANIMATION = Animation.EASE_IN_BACK;
    private static final int DURATION = 200;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SequencedSet<Element>> elements = sgGeneral.add(
        new MultiChoiceSetting.Builder<Element>()
            .name("Elements")
            .description("Enabled gui elements to animate.")
            .choices(Element.values())
            .defaultValue(Element.values())
            .build()
    );

    private final Animator tabAnimator = new Animator();
    private final Animator inventoryAnimator = new Animator();
    
    private @Nullable HandledScreen<?> closingInventory;
    
    private int closingMouseX;
    private int closingMouseY;

    private boolean renderingClosingInventory;

    public Animations() {
        super(Category.RENDER, "Animations", "Animates vanilla gui elements.");
    }

    @Override
    public void onActivate() {
        tabAnimator.set(mc.options.playerListKey.isPressed() ? 1 : 0);
        inventoryAnimator.set(mc.currentScreen instanceof HandledScreen ? 1 : 0);
    }

    @Override
    public void onDeactivate() {
        closingInventory = null;
    }

    // Tab

    @EventHandler
    private void onChangeTabVisible(ChangeTabVisibleEvent event) {
        if (!elements.get().contains(Element.TAB)) {
            return;
        }

        tabAnimator.run(
            event.visible ? OPEN_ANIMATION : CLOSE_ANIMATION,
            event.visible ? 1 : 0,
            DURATION
        );
    }

    @EventHandler
    private void onRenderTab(RenderTabEvent event) {
        if (!elements.get().contains(Element.TAB)) {
            return;
        }

        float openness = tabAnimator.getValueF();
        if (openness == 1) {
            return;
        }

        float pivotX = event.width / 2f;

        Matrix3x2fStack matrices = event.context.getMatrices();
        matrices.translate(pivotX, TAB_TOP);
        matrices.scale(scale(openness));
        matrices.translate(-pivotX, -TAB_TOP);
    }

    public boolean isClosingTab() {
        return isActive()
            && elements.get().contains(Element.TAB)
            && tabAnimator.isRunning()
            && tabAnimator.getToValue() == 0;
    }

    // Inventory

    @EventHandler
    private void onScreenOpen(ScreenOpenEvent event) {
        if (!elements.get().contains(Element.INVENTORY)) {
            return;
        }

        // Posted before the swap happens, so this is still the screen going away. Closing a container
        // goes through setScreen(null) twice, once from the screen handler and once from the screen
        // itself, and the second one has nothing left to close - so it must leave all of this alone.
        HandledScreen<?> closing = mc.currentScreen instanceof HandledScreen<?> screen ? screen : null;

        if (event.screen instanceof HandledScreen) {
            closingInventory = null;
            inventoryAnimator.run(OPEN_ANIMATION, 1, DURATION);
        } else if (closing != null) {
            // Anything but going back to the world draws over the container anyway, so a closing
            // animation is only worth playing when the hud is all that is left.
            if (event.screen == null) {
                closingInventory = closing;
                closingMouseX = (int) mc.mouse.getScaledX(mc.getWindow());
                closingMouseY = (int) mc.mouse.getScaledY(mc.getWindow());

                inventoryAnimator.run(CLOSE_ANIMATION, 0, DURATION);
            } else {
                // Nothing is going to draw the old container, so there is nothing to animate.
                closingInventory = null;
                inventoryAnimator.set(0);
            }
        }
    }

    @EventHandler
    private void onRenderInventory(RenderInventoryEvent event) {
        if (!elements.get().contains(Element.INVENTORY)) {
            return;
        }

        float openness = inventoryAnimator.getValueF();
        if (openness == 1) {
            return;
        }

        // A container is one centered block of gui, so the middle is the only pivot that does not make
        // it slide across the screen.
        float pivotX = event.x + event.width / 2f;
        float pivotY = event.y + event.height / 2f;

        Matrix3x2fStack matrices = event.context.getMatrices();
        matrices.translate(pivotX, pivotY);
        matrices.scale(scale(openness));
        matrices.translate(-pivotX, -pivotY);
    }
    
    public void renderClosingInventory(DrawContext context, float deltaTicks) {
        if (closingInventory == null) {
            return;
        }
        
        boolean done = mc.currentScreen != null
            || !elements.get().contains(Element.INVENTORY)
            || !inventoryAnimator.isRunning();

        if (done) {
            closingInventory = null;

            // Whatever took the screen over is not going to advance the animation, so it would sit
            // half closed and swallow the next opening one.
            if (inventoryAnimator.getToValue() == 0) {
                inventoryAnimator.set(0);
            }

            return;
        }

        renderingClosingInventory = true;

        context.createNewRootLayer();
        closingInventory.renderWithTooltip(context, closingMouseX, closingMouseY, deltaTicks);

        renderingClosingInventory = false;
    }
    
    public boolean isRenderingClosingInventory() {
        return renderingClosingInventory;
    }

    // The cursor stack and the tooltips are drawn outside of the two passes a container is transformed in, so they
    // would keep their full size while everything else animates. None of them are worth a transform of their own -
    // a cursor stack does not even exist anymore once the container is on its way out - so they are left out entirely
    // for as long as the animation runs.
    public boolean skipInventoryOverlays() {
        return isActive()
            && elements.get().contains(Element.INVENTORY)
            && (renderingClosingInventory || inventoryAnimator.isRunning());
    }
    
    private float scale(float openness) {
        return Math.max(0, openness);
    }

    private enum Element {

        TAB,
        INVENTORY

    }

}
