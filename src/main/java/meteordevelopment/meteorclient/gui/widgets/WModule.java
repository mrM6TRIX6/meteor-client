/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.gui.screens.ModuleScreen;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class WModule extends WPressable {

    private final Module module;
    private final String name;

    private double nameWidth;

    private double animationProgress1;
    private double animationProgress2;

    public WModule(Module module) {
        this(module, module.name);
    }

    public WModule(Module module, String name) {
        this.module = module;
        this.name = name;
        this.tooltip = module.description;

        if (module.isActive()) {
            animationProgress1 = 1;
            animationProgress2 = 1;
        } else {
            animationProgress1 = 0;
            animationProgress2 = 0;
        }
    }

    @Override
    public double pad() {
        return GuiConstants.scale(4);
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();

        if (nameWidth == 0) {
            nameWidth = GuiConstants.textWidth(name);
        }

        width = pad + nameWidth + pad;
        height = pad + GuiConstants.textHeight() + pad;
    }

    @Override
    protected void onPressed(int button) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            module.toggle();
        } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
            mc.setScreen(new ModuleScreen(module));
        }
    }

    @Override
    protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
        double pad = pad();

        animationProgress1 += delta * 4 * ((module.isActive() || mouseOver) ? 1 : -1);
        animationProgress1 = MathHelper.clamp(animationProgress1, 0, 1);

        animationProgress2 += delta * 6 * (module.isActive() ? 1 : -1);
        animationProgress2 = MathHelper.clamp(animationProgress2, 0, 1);

        if (animationProgress1 > 0) {
            rect(x, y, width * animationProgress1, height, GuiConstants.MODULE_BACKGROUND);
        }
        if (animationProgress2 > 0) {
            rect(x, y + height * (1 - animationProgress2), GuiConstants.scale(2), height * animationProgress2, GuiConstants.ACCENT);
        }

        double x = this.x + pad;
        double w = width - pad * 2;

        if (GuiConstants.MODULE_ALIGNMENT == AlignmentX.CENTER) {
            x += w / 2 - nameWidth / 2;
        } else if (GuiConstants.MODULE_ALIGNMENT == AlignmentX.RIGHT) {
            x += w - nameWidth;
        }

        text(name, x, y + pad, GuiConstants.TEXT);
    }

}
