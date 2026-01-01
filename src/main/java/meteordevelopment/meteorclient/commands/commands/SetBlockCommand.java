/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ClientPosArgumentType;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class SetBlockCommand extends Command {
    
    public SetBlockCommand() {
        super("SetBlock", "Sets client side blocks.");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("pos", ClientPosArgumentType.pos())
            .then(argument("block", BlockStateArgumentType.blockState(REGISTRY_ACCESS))
                .executes(context -> {
                    Vec3d pos = ClientPosArgumentType.getPos(context, "pos");
                    BlockState blockState = context.getArgument("block", BlockStateArgument.class).getBlockState();
                    
                    mc.world.setBlockState(new BlockPos((int) pos.getX(), (int) pos.getY(), (int) pos.getZ()), blockState);
                    
                    return SINGLE_SUCCESS;
                })
            )
        );
    }
    
}
