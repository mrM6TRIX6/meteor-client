/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.text.TextUtils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Formatting;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SaveSkinCommand extends Command {
    
    private final PointerBuffer filters;
    
    public SaveSkinCommand() {
        super("SaveSkin", "Saves the skin of the player.");
        
        filters = BufferUtils.createPointerBuffer(1);
        
        ByteBuffer pngFilter = MemoryUtil.memASCII("*.png");
        
        filters.put(pngFilter);
        filters.rewind();
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", PlayerListEntryArgumentType.create())
            .executes(context -> {
                PlayerListEntry player = PlayerListEntryArgumentType.get(context, "player");
                saveTexture(player.getProfile().name(), player.getSkinTextures().body());
                return SINGLE_SUCCESS;
            })
            .then(literal("body")
                .executes(context -> {
                    PlayerListEntry player = PlayerListEntryArgumentType.get(context, "player");
                    saveTexture(player.getProfile().name(), player.getSkinTextures().body());
                    return SINGLE_SUCCESS;
                })
            )
            .then(literal("cape")
                .executes(context -> {
                    PlayerListEntry player = PlayerListEntryArgumentType.get(context, "player");
                    saveTexture(player.getProfile().name(), player.getSkinTextures().cape());
                    return SINGLE_SUCCESS;
                })
            )
            .then(literal("elytra")
                .executes(context -> {
                    PlayerListEntry player = PlayerListEntryArgumentType.get(context, "player");
                    saveTexture(player.getProfile().name(), player.getSkinTextures().elytra());
                    return SINGLE_SUCCESS;
                })
            )
        );
    }
    
    private void saveTexture(String playerName, AssetInfo.TextureAsset asset) {
        if (asset == null) {
            error(playerName + " doesn't have this skin texture");
            return;
        }
        
        AbstractTexture texture = mc.getTextureManager().getTexture(asset.texturePath());
        if (!(texture instanceof NativeImageBackedTexture tex)) {
            error("Invalid skin texture");
            return;
        }
        
        NativeImage image = tex.getImage();
        if (image == null) {
            error("Invalid skin texture");
            return;
        }
        
        MeteorExecutor.execute(() -> {
            String path = TinyFileDialogs.tinyfd_saveFileDialog("Save image", null, filters, null);
            if (path == null) {
                error("Invalid path");
                return;
            }
            
            File file = new File(path);
            
            try {
                image.writeTo(file);
                info(
                    Text.literal(playerName + "'s skin saved to: ").formatted(Formatting.GRAY)
                        .append(TextUtils.copyable(file.getAbsolutePath()).formatted(Formatting.WHITE))
                );
            } catch (IOException e) {
                error("Failed to write %s", file.getName());
            }
        });
    }
    
}