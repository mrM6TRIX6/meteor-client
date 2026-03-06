/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(value = ChatScreen.class, priority = 1001)
public abstract class ChatScreenMixin {
    
    @Shadow
    protected TextFieldWidget chatField;
    
    @Inject(method = "init", at = @At(value = "RETURN"))
    private void onInit(CallbackInfo ci) {
        if (Modules.get().get(BetterChat.class).isInfiniteChatBox()) {
            chatField.setMaxLength(Integer.MAX_VALUE);
        }
    }
    
    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        BetterChat betterChat = Modules.get().get(BetterChat.class);
        if (!betterChat.copyingMessages()) {
            return;
        }
        
        Integer activeMessage = getActiveMessage(click);
        
        if (activeMessage == null) {
            return;
        }
        
        ChatHud chatHud = mc.inGameHud.getChatHud();
        ChatHudAccessor accessor = (ChatHudAccessor) chatHud;
        
        List<ChatHudLine.Visible> visibleMessages = accessor.meteor$getVisibleMessages();
        Deque<ChatHudLine.Visible> messageParts = new ArrayDeque<>();
        messageParts.add(visibleMessages.get(activeMessage));
        
        for (int index = activeMessage + 1; index < visibleMessages.size(); index++) {
            if (visibleMessages.get(index).endOfEntry()) {
                break;
            }
            
            messageParts.addFirst(visibleMessages.get(index));
        }
        
        if (messageParts.isEmpty()) {
            return;
        }
        
        betterChat.copyMessage(messageParts, click.button());
    }
    
    @Unique
    private @Nullable Integer getActiveMessage(Click click) {
        ChatHudAccessor chatHud = (ChatHudAccessor) mc.inGameHud.getChatHud();
        List<ChatHudLine.Visible> visibleMessages = chatHud.meteor$getVisibleMessages();
        
        if (visibleMessages.isEmpty()) {
            return null;
        }
        
        double chatScale = chatHud.meteor$invokeGetScale();
        
        if (chatScale <= 0.0) {
            return null;
        }
        
        int chatWidth = (int) Math.ceil(chatHud.meteor$invokeGetWidth() / chatScale);
        double localMouseX = click.x() / chatScale - 4.0;
        
        if (localMouseX < 0.0 || localMouseX > chatWidth) {
            return null;
        }
        
        int lineHeight = chatHud.meteor$invokeGetLineHeight();
        
        if (lineHeight <= 0) {
            return null;
        }
        
        int guiHeight = mc.getWindow().getScaledHeight();
        int chatBottom = (int) Math.floor((guiHeight - 40) / chatScale);
        int lineIndex = (int) Math.floor((chatBottom - click.y() / chatScale) / lineHeight);
        
        if (lineIndex < 0) {
            return null;
        }
        
        int messageIndex = lineIndex + chatHud.meteor$getScrolledLines();
        return messageIndex >= 0 && messageIndex < visibleMessages.size() ? messageIndex : null;
    }
    
}
