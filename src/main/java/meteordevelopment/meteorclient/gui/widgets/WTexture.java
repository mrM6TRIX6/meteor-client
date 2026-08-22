/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.GuiConstants;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import meteordevelopment.meteorclient.utils.render.ui.image.BuiltImage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class WTexture extends WWidget {

    private final double textureWidth, textureHeight;
    private final double rotation;
    private final Identifier texture;

    public WTexture(double width, double height, double rotation, Identifier texture) {
        this.textureWidth = width;
        this.textureHeight = height;
        this.rotation = rotation;
        this.texture = texture;
    }

    @Override
    protected void onCalculateSize() {
        width = GuiConstants.scale(textureWidth);
        height = GuiConstants.scale(textureHeight);
    }

    @Override
    protected void onRender(DrawContext context, double mouseX, double mouseY, double delta) {
        if (texture == null) {
            return;
        }

        // Nearest filtering because these are tiny textures (player heads) drawn a lot bigger than they are.
        Render2D.image(new BuiltImage(texture, (float) x, (float) y, (float) width, (float) height, 0, GuiConstants.color(GuiConstants.TEXT))
            .withNearestFilter()
            .withRotation((float) rotation, (float) (x + width / 2), (float) (y + height / 2)));
    }

}
