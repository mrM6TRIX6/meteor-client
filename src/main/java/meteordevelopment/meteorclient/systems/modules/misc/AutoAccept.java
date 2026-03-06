/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.game.MessageEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoAccept extends Module {
    
    private static final Pattern TP_PATTERN = Pattern.compile("\\|\\s+(.+?)\\s+просит телепортироваться к Вам\\.");
    private static final Pattern TPHERE_PATTERN = Pattern.compile("\\|\\s+(.+?)\\s+просит вас телепортироваться к нему\\.");
    private static final Pattern CLAN_PATTERN = Pattern.compile("§.§.\\|.*Вы приглашены в клан \".*\" игроком §.(.+)");
    private static final Pattern SITPLAYER_PATTERN = Pattern.compile("\\| §fИгрок §a([^§]+) §fхочет сесть вам на голову");
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> tp = sgGeneral.add(new BoolSetting.Builder()
        .name("tp")
        .description("Auto accepts for teleport requests (/tpa).")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> tphere = sgGeneral.add(new BoolSetting.Builder()
        .name("tphere")
        .description("Auto accepts for teleport here requests (/tpahere).")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> clan = sgGeneral.add(new BoolSetting.Builder()
        .name("clan")
        .description("Auto accepts for clan requests.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> sitplayer = sgGeneral.add(new BoolSetting.Builder()
        .name("sitplayer")
        .description("Auto accepts for sitplayer request.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> onlyFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("friends-only")
        .description("Automatically accepts requests from friends only.")
        .defaultValue(true)
        .build()
    );
    
    public AutoAccept() {
        super(Categories.MISC, "AutoAccept", "Automatically accepts various requests on MineBlaze.");
    }
    
    @EventHandler
    private void onMessageReceive(MessageEvent.Receive event) {
        String msg = event.getMessage().getString();
        
        if (tp.get()) {
            Matcher matcher = TP_PATTERN.matcher(msg);
            if (matcher.find() && (!onlyFriends.get() || Friends.get().isFriend(matcher.group(1)))) {
                ChatUtils.sendPlayerMsg("/tpaccept " + matcher.group(1));
            }
        }
        
        if (tphere.get()) {
            Matcher matcher = TPHERE_PATTERN.matcher(msg);
            if (matcher.find() && (!onlyFriends.get() || Friends.get().isFriend(matcher.group(1)))) {
                ChatUtils.sendPlayerMsg("/tpaccept " + matcher.group(1));
            }
        }
        
        if (clan.get()) {
            Matcher matcher = CLAN_PATTERN.matcher(msg);
            if (matcher.find() && (!onlyFriends.get() || Friends.get().isFriend(matcher.group(1)))) {
                ChatUtils.sendPlayerMsg("/c accept");
            }
        }
        
        if (sitplayer.get()) {
            Matcher matcher = SITPLAYER_PATTERN.matcher(msg);
            if (matcher.find() && (!onlyFriends.get() || Friends.get().isFriend(matcher.group(1)))) {
                ChatUtils.sendPlayerMsg("/sitplayer accept " + matcher.group(1));
            }
        }
    }
    
}
