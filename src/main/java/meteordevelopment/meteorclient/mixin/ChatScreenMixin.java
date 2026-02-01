/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.Component;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
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
    private void onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        BetterChat betterChat = Modules.get().get(BetterChat.class);
        if (!betterChat.copyingMessages()) {
            return;
        }
        
        int[] activeMessage = getActiveMessage((int) click.x(), (int) click.y());
        
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
        
        copyMessage(messageParts, click.button());
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
        
        float chatScale = (float) accessor.meteor$getChatScale();
        int chatLineY = (int) meteor$toChatLineY(mouseY);
        int messageIndex = meteor$getMessageIndex(0, chatLineY);
        int buttonX = (int) (accessor.meteor$getWidth() + 14 * chatScale);
        
        if (messageIndex == -1 || mouseX > buttonX + 14 * chatScale) {
            return null;
        }
        
        int chatY = (int) (9.0 * (mc.options.getChatLineSpacing().getValue() + 1.0));
        
        int buttonSize = (int) (9 * chatScale);
        int lineHeight = accessor.meteor$getLineHeight();
        int scaledButtonY = chatY - (chatLineY + 1) * lineHeight + (int) Math.ceil((lineHeight - 9) / 2.0);
        float buttonY = scaledButtonY * chatScale;
        
        boolean hovering = mouseX >= 0 && mouseX <= buttonX && mouseY >= buttonY && mouseY <= buttonY + buttonSize;
        
        if (hovering) {
            return new int[] {
                (int) buttonY,
                buttonX,
                buttonSize,
                messageIndex
            };
        } else {
            return null;
        }
    }
    
    // Functions removed in 1.21.11, so re-implementation
    
    @Unique
    private double meteor$toChatLineY(double y) {
        ChatHud chatHud = mc.inGameHud.getChatHud();
        ChatHudAccessor accessor = (ChatHudAccessor) chatHud;
        
        double d = mc.getWindow().getScaledHeight() - y - 40.0;
        return d / (accessor.meteor$getChatScale() * accessor.meteor$getLineHeight());
    }
    
    @Unique
    private int meteor$getMessageIndex(double chatLineX, double chatLineY) {
        ChatHud chatHud = mc.inGameHud.getChatHud();
        ChatHudAccessor accessor = (ChatHudAccessor) chatHud;
        
        int i = this.meteor$getMessageLineIndex(chatLineX, chatLineY);
        
        if (i == -1) {
            return -1;
        } else {
            while (i >= 0) {
                if (accessor.meteor$getVisibleMessages().get(i).endOfEntry()) {
                    return i;
                }
                --i;
            }
            
            return i;
        }
    }
    
    @Unique
    private int meteor$getMessageLineIndex(double chatLineX, double chatLineY) {
        ChatHud chatHud = mc.inGameHud.getChatHud();
        ChatHudAccessor accessor = (ChatHudAccessor) chatHud;
        
        if (chatHud.isChatFocused() && !accessor.meteor$isChatHidden()) {
            if (!(chatLineX < -4.0) && !(chatLineX > MathHelper.floor(accessor.meteor$getWidth() / accessor.meteor$getChatScale()))) {
                int i = Math.min(chatHud.getVisibleLineCount(), accessor.meteor$getVisibleMessages().size());
                if (chatLineY >= 0.0 && chatLineY < i) {
                    int j = MathHelper.floor(chatLineY + accessor.meteor$getScrolledLines());
                    if (j >= 0 && j < accessor.meteor$getVisibleMessages().size()) {
                        return j;
                    }
                }
                
                return -1;
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }
    
}
