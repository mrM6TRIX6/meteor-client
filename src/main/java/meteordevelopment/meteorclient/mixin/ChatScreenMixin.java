/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IChatHudLineVisible;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import meteordevelopment.meteorclient.utils.misc.Range;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.collection.ArrayListDeque;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        
        ChatHudAccessor accessor = (ChatHudAccessor) mc.inGameHud.getChatHud();
        
        List<ChatHudLine.Visible> visibleMessages = accessor.meteor$getVisibleMessages();
        Range messageBounds = resolveMessageBounds(visibleMessages, activeMessage);
        Deque<ChatHudLine.Visible> messageParts = new ArrayListDeque<>(messageBounds.to - messageBounds.from + 1);
        
        for (int index = messageBounds.to; index >= messageBounds.from; index--) {
            messageParts.addLast(visibleMessages.get(index));
        }
        
        if (messageParts.isEmpty()) {
            return;
        }
        
        betterChat.copyMessage(messageParts, click);
    }
    
    /**
     * Resolves the contiguous wrapped-line range for the message at [index].
     */
    @Unique
    private Range resolveMessageBounds(List<ChatHudLine.Visible> visibleMessages, int index) {
        IChatHudLineVisible line = (IChatHudLineVisible) (Object) visibleMessages.get(index);
        int id = line.meteor$getId();
        
        if (id != 0) {
            int start = index;
            while (start > 0) {
                IChatHudLineVisible previousLine = (IChatHudLineVisible) (Object) visibleMessages.get(start - 1);
                int previousId = previousLine.meteor$getId();
                
                if (id != previousId) {
                    break;
                }
                start--;
            }
            
            int end = index;
            int lastIndex = visibleMessages.size() - 1;
            
            while (end < lastIndex) {
                IChatHudLineVisible nextLine = (IChatHudLineVisible) (Object) visibleMessages.get(end + 1);
                int nextId = nextLine.meteor$getId();
                if (id != nextId) {
                    break;
                }
                end++;
            }
            
            return Range.of(start, end);
        }
        
        int start = index;
        
        while (start > 0 && !visibleMessages.get(start).endOfEntry()) {
            start--;
        }
        
        int end = index;
        int lastIndex = visibleMessages.size() - 1;
        while (end < lastIndex && !visibleMessages.get(end + 1).endOfEntry()) {
            end++;
        }
        
        return Range.of(start, end);
    }
    
    @Unique
    private @Nullable Integer getActiveMessage(Click click) {
        ChatHud chatHud = mc.inGameHud.getChatHud();
        ChatHudAccessor accessor = (ChatHudAccessor) chatHud;
        List<ChatHudLine.Visible> visibleMessages = accessor.meteor$getVisibleMessages();
        
        if (visibleMessages.isEmpty()) {
            return null;
        }
        
        double chatScale = accessor.meteor$invokeGetScale();
        
        if (chatScale <= 0.0) {
            return null;
        }
        
        int chatWidth = (int) Math.ceil(accessor.meteor$invokeGetWidth() / chatScale);
        double localMouseX = click.x() / chatScale - 4.0;
        
        if (localMouseX < 0.0 || localMouseX > chatWidth) {
            return null;
        }
        
        int lineHeight = accessor.meteor$invokeGetLineHeight();
        
        if (lineHeight <= 0) {
            return null;
        }
        
        int guiHeight = mc.getWindow().getScaledHeight();
        int chatBottom = (int) Math.floor((guiHeight - 40) / chatScale);
        double localMouseY = chatBottom - click.y() / chatScale;
        
        if (localMouseY < 0.0) {
            return null;
        }
        
        int lineIndex = (int) Math.floor(localMouseY / lineHeight);
        int visibleLineCount = Math.min(chatHud.getVisibleLineCount(), visibleMessages.size() - accessor.meteor$getScrolledLines());
        
        if (lineIndex < 0 || lineIndex >= visibleLineCount) {
            return null;
        }
        
        int messageIndex = lineIndex + accessor.meteor$getScrolledLines();
        
        return messageIndex >= 0 && messageIndex < visibleMessages.size() ? messageIndex : null;
    }
    
}
