/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.network.Http;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * A player's head cut out of their skin, registered with the vanilla texture manager so it can be drawn by
 * {@link meteordevelopment.meteorclient.utils.render.ui.Render2D}.
 */
public class PlayerHeadTexture {

    private static int COUNT;

    private @Nullable Identifier identifier;

    public PlayerHeadTexture(String url) {
        BufferedImage skin;
        try {
            skin = ImageIO.read(Http.get(url).sendInputStream());
        } catch (IOException e) {
            MeteorClient.LOGGER.error("Failed to download player head from '{}'", url, e);
            return;
        }

        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 8, 8, false);

        // The head, then the hat layer on top of it.
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                image.setColorArgb(x, y, skin.getRGB(8 + x, 8 + y));

                int hat = skin.getRGB(40 + x, 8 + y);
                if ((hat >>> 24) != 0) {
                    image.setColorArgb(x, y, hat);
                }
            }
        }

        register(image);
    }

    public PlayerHeadTexture() {
        try (InputStream inputStream = mc.getResourceManager().getResource(MeteorClient.identifier("textures/steve.png")).get().getInputStream()) {
            register(NativeImage.read(inputStream));
        } catch (IOException e) {
            MeteorClient.LOGGER.error("Failed to read the default player head", e);
        }
    }

    private void register(NativeImage image) {
        identifier = MeteorClient.identifier("heads/" + COUNT++);
        mc.getTextureManager().registerTexture(identifier, new NativeImageBackedTexture(null, image));
    }

    /**
     * {@code null} when the skin could not be read, in which case there is nothing to draw.
     */
    public @Nullable Identifier identifier() {
        return identifier;
    }

}
