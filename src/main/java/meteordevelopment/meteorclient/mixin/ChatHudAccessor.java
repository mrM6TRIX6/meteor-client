/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatHud.class)
public interface ChatHudAccessor {
    
    @Accessor("visibleMessages")
    List<ChatHudLine.Visible> meteor$getVisibleMessages();
    
    @Accessor("messages")
    List<ChatHudLine> meteor$getMessages();
    
    @Invoker("toChatLineY")
    double meteor$invokeToChatLineY(double y);
    
    @Invoker("getMessageIndex")
    int meteor$invokeGetMessageIndex(double chatLineX, double chatLineY);
    
    @Invoker("getLineHeight")
    int meteor$invokeGetLineHeight();
    
}
