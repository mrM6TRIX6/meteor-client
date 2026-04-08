/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.utils.text.TextUtils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Formatting;

import java.io.File;
import java.io.IOException;

public class SaveSkinCommand extends Command {
    
    private static final SimpleCommandExceptionType FOLDER_ERROR = new SimpleCommandExceptionType(Text.literal("Failed to create folder"));
    private static final SimpleCommandExceptionType NO_TEXTURES_SAVED = new SimpleCommandExceptionType(Text.literal("No textures were saved"));
    
    private static final File FOLDER = new File(MeteorClient.FOLDER, "skins");
    
    public SaveSkinCommand() {
        super("SaveSkin", "Saves the skin of the player.");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", PlayerListEntryArgumentType.create())
            .executes(context -> {
                PlayerListEntry player = PlayerListEntryArgumentType.get(context, "player");
                String playerName = player.getProfile().name();
                
                FOLDER.mkdirs();
                File playerFolder = new File(FOLDER, playerName + "-" + System.currentTimeMillis());
                
                if (!playerFolder.mkdirs()) {
                    throw FOLDER_ERROR.create();
                }
                
                AssetInfo.TextureAsset body = player.getSkinTextures().body();
                AssetInfo.TextureAsset cape = player.getSkinTextures().cape();
                AssetInfo.TextureAsset elytra = player.getSkinTextures().elytra();
                
                int saved = 0;
                
                saved += saveTexture(body, new File(playerFolder, "body.png"));
                saved += saveTexture(cape, new File(playerFolder, "cape.png"));
                saved += saveTexture(elytra, new File(playerFolder, "elytra.png"));
                
                if (saved == 0) {
                    throw NO_TEXTURES_SAVED.create();
                }
                
                info(
                    Text.literal(playerName + "'s skin saved to: ").formatted(Formatting.GRAY)
                        .append(TextUtils.copyable(playerFolder.getAbsolutePath()).formatted(Formatting.WHITE))
                        .append(Text.literal(". (" + saved + " files)").formatted(Formatting.GRAY))
                );
                
                return SINGLE_SUCCESS;
            })
        );
    }
    
    private int saveTexture(AssetInfo.TextureAsset asset, File file) {
        if (asset == null) {
            return 0;
        }
        
        AbstractTexture texture = mc.getTextureManager().getTexture(asset.texturePath());
        if (!(texture instanceof NativeImageBackedTexture tex)) {
            return 0;
        }
        
        NativeImage image = tex.getImage();
        if (image == null) {
            return 0;
        }
        
        try {
            image.writeTo(file);
            return 1;
        } catch (IOException e) {
            error("Failed to write %s.", file.getName());
            return 0;
        }
    }
    
}