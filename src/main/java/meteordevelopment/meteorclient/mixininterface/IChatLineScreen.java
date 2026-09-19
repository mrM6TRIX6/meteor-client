/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import org.jetbrains.annotations.Nullable;

public interface IChatLineScreen {
    
    @Nullable
    TextFieldWidget meteor$getChatLineField();
    
    boolean meteor$handleChatLineKey(KeyInput input);
    
    default int meteor$chatLineBottomInset() {
        return 0;
    }

}
