/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

public class GodbridgeAngleCommand extends Command {
    
    public GodbridgeAngleCommand() {
        super("GodbridgeAngle", "Rotates your camera to the perfect angle for godbridge.", "gb");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            float yaw = mc.player.getYaw();
            
            float angle = yaw % 360;
            yaw = angle > 180 ? angle - 360 : angle < -180 ? angle + 360 : angle;
            
            float targetYaw = findClosestGodbridgeAngle(yaw);
            
            mc.player.setYaw(targetYaw);
            mc.player.setPitch(76.0F);
            
            return SINGLE_SUCCESS;
        });
    }
    
    private float findClosestGodbridgeAngle(float yaw) {
        float[] godbridgeAngles = { -135, -45, 45, 135 };
        
        float closestAngle = godbridgeAngles[0];
        float smallestDiff = Math.abs(yaw - closestAngle);
        
        for (float angle : godbridgeAngles) {
            float diff = Math.abs(yaw - angle);
            if (diff < smallestDiff) {
                smallestDiff = diff;
                closestAngle = angle;
            }
        }
        return closestAngle;
    }
    
}
