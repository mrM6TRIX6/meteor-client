/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.CharacterVisitor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
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
    private void hookMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        BetterChat betterChat = Modules.get().get(BetterChat.class);
        if (!(betterChat.isActive() && betterChat.copyingMessages())) {
            return;
        }
        
        int[] activeMessage = getActiveMessage((int) mouseX, (int) mouseY);
        
        if (activeMessage == null) {
            return;
        }
        
        ChatHud chatHud = mc.inGameHud.getChatHud();
        ChatHudAccessor accessor = (ChatHudAccessor) chatHud;
        
        List<ChatHudLine.Visible> visibleMessages = accessor.meteor$getVisibleMessages();
        Deque<ChatHudLine.Visible> messageParts = new ArrayDeque<>();
        
        messageParts.add(visibleMessages.get(activeMessage[3]));
        
        for (int index = activeMessage[3] + 1; index < visibleMessages.size(); index++) {
            if (visibleMessages.get(index).endOfEntry()) {
                break;
            }
            
            messageParts.addFirst(visibleMessages.get(index));
        }
        
        if (messageParts.isEmpty()) {
            return;
        }
        
        copyMessage(messageParts, button);
    }
    
    @Unique
    private void copyMessage(Deque<ChatHudLine.Visible> messageParts, int button) {
        final StringBuilder builder = new StringBuilder();
        
        CharacterVisitor visitor = (index, style, codePoint) -> {
            builder.append((char) codePoint);
            return true;
        };
        
        for (ChatHudLine.Visible line : messageParts) {
            line.content().accept(visitor);
        }
        
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            mc.keyboard.setClipboard(builder.toString());
        }
    }
    
    // [0] - y,
    // [1] - width,
    // [2] - height,
    // [3] - (message) index
    @Unique
    private int @Nullable [] getActiveMessage(int mouseX, int mouseY) {
        ChatHud chatHud = mc.inGameHud.getChatHud();
        ChatHudAccessor accessor = (ChatHudAccessor) chatHud;
        IChatHud addition = (IChatHud) chatHud;
        
        float chatScale = (float) chatHud.getChatScale();
        int chatLineY = (int) accessor.meteor$invokeToChatLineY(mouseY);
        int messageIndex = accessor.meteor$invokeGetMessageIndex(0, chatLineY);
        int buttonX = (int) (chatHud.getWidth() + 14 * chatScale);
        
        if (messageIndex == -1 || mouseX > buttonX + 14 * chatScale) {
            return null;
        }
        
        int chatY = addition.meteor$getChatY();
        
        int buttonSize = (int) (9 * chatScale);
        int lineHeight = accessor.meteor$invokeGetLineHeight();
        int scaledButtonY = chatY - (chatLineY + 1) * lineHeight + (int) Math.ceil((lineHeight - 9) / 2.0);
        float buttonY = scaledButtonY * chatScale;
        
        boolean hovering = mouseX >= 0 && mouseX <= buttonX && mouseY >= buttonY && mouseY <= buttonY + buttonSize;
        
        if (hovering) {
            return new int[] { (int) buttonY, buttonX, buttonSize, messageIndex };
        } else {
            return null;
        }
    }
    
}
