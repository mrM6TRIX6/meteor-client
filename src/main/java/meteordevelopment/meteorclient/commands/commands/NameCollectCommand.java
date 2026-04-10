/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.text.TextUtils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

public class NameCollectCommand extends Command {
    
    private final PointerBuffer filters;
    
    public NameCollectCommand() {
        super("NameCollect", "Collects all player names to the file.");
        
        filters = BufferUtils.createPointerBuffer(1);
        
        ByteBuffer txtFilter = MemoryUtil.memASCII("*.txt");
        
        filters.put(txtFilter);
        filters.rewind();
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            MeteorExecutor.execute(() -> {
                String path = TinyFileDialogs.tinyfd_saveFileDialog("Save file", null, filters, null);
                
                if (path == null) {
                    error("Invalid path");
                    return;
                }
                
                File file = new File(path);
                
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                    for (PlayerListEntry player : mc.getNetworkHandler().getPlayerList()) {
                        writer.write(player.getProfile().name());
                        writer.newLine();
                    }
                    
                    info(Text.literal("Player names saved to: ").formatted(Formatting.GRAY)
                        .append(TextUtils.copyable(file.getAbsolutePath()).formatted(Formatting.WHITE)));
                } catch (IOException e) {
                    error("Failed write player names to the file");
                }
            });
            
            return SINGLE_SUCCESS;
        });
    }
    
}
