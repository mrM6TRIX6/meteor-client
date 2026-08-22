/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.impl;

import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WTexture;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.misc.JsonUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.gui.screen.Screen;

public class FriendsTab extends Tab {
    
    public FriendsTab() {
        super("Friends");
    }
    
    @Override
    public TabScreen createScreen() {
        return new FriendsScreen(this);
    }
    
    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof FriendsScreen;
    }
    
    private static class FriendsScreen extends WindowTabScreen {
        
        public FriendsScreen(Tab tab) {
            super(tab);
        }
        
        @Override
        public void initWidgets() {
            WTable table = add(new WTable()).expandX().minWidth(400).widget();
            initTable(table);
            
            add(new WHorizontalSeparator()).expandX();
            
            // New
            WHorizontalList list = add(new WHorizontalList()).expandX().widget();
            
            WTextBox nameW = list.add(new WTextBox("", (text, c) -> c != ' ')).expandX().widget();
            nameW.setFocused(true);
            
            WPlus add = list.add(new WPlus()).widget();
            add.action = () -> {
                String name = nameW.get().trim();
                Friend friend = new Friend(name);
                
                if (Friends.get().add(friend)) {
                    nameW.set("");
                    reload();
                    
                    MeteorExecutor.execute(() -> {
                        friend.updateInfo();
                        mc.execute(this::reload);
                    });
                }
            };
            enterAction = add.action;
            
            // Clear
            WButton clearBtn = add(new WButton("Clear")).expandX().widget();
            clearBtn.action = () -> {
                Friends.get().clear();
                reload();
            };
        }
        
        private void initTable(WTable table) {
            table.clear();
            if (Friends.get().isEmpty()) {
                return;
            }
            
            Friends.get().forEach(friend ->
                MeteorExecutor.execute(() -> {
                    if (friend.headTextureNeedsUpdate()) {
                        friend.updateInfo();
                    }
                })
            );
            
            for (Friend friend : Friends.get()) {
                table.add(new WTexture(32, 32, 0, friend.getHead().identifier()));
                table.add(new WLabel(friend.getName()));
                
                WMinus remove = table.add(new WMinus()).expandCellX().right().widget();
                remove.action = () -> {
                    Friends.get().remove(friend);
                    reload();
                };
                
                table.row();
            }
        }
        
        @Override
        public boolean toClipboard() {
            return JsonUtils.toClipboard(Friends.get());
        }
        
        @Override
        public boolean fromClipboard() {
            return JsonUtils.fromClipboard(Friends.get());
        }
        
    }
    
}
