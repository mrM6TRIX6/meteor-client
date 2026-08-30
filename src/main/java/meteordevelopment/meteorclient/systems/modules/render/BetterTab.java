/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.ColorSetting;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import meteordevelopment.meteorclient.utils.render.ui.msdf.BuiltMsdf;
import meteordevelopment.meteorclient.utils.render.ui.msdf.MsdfFont;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

public class BetterTab extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> autoTabSize = sgGeneral.add(new BoolSetting.Builder()
        .name("AutoTabSize")
        .description("Tab size will automatically adjust to the count of players.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Integer> tabSize = sgGeneral.add(new IntSetting.Builder()
        .name("TabSize")
        .description("How many players in total to display in the tab.")
        .defaultValue(80)
        .min(1)
        .sliderRange(1, 1000)
        .visible(() -> !autoTabSize.get())
        .build()
    );
    
    private final Setting<Integer> columnHeight = sgGeneral.add(new IntSetting.Builder()
        .name("ColumnHeight")
        .description("How many players to display in one column.")
        .defaultValue(20)
        .min(1)
        .sliderRange(1, 1000)
        .visible(() -> !autoTabSize.get())
        .build()
    );
    
    private final Setting<Boolean> highlightSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("HighlightSelf")
        .description("Highlights yourself in the tab.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Color> selfColor = sgGeneral.add(new ColorSetting.Builder()
        .name("SelfColor")
        .description("The color to highlight your name with.")
        .defaultValue(new Color(50, 193, 50, 100))
        .visible(highlightSelf::get)
        .build()
    );
    
    private final Setting<Boolean> highlightFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("HighlightFriends")
        .description("Highlights friends in the tab.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Color> friendsColor = sgGeneral.add(new ColorSetting.Builder()
        .name("FriendsColor")
        .description("The color to highlight friends with.")
        .defaultValue(new Color(16, 89, 203, 100))
        .visible(highlightFriends::get)
        .build()
    );
    
    private final Setting<Boolean> pingNumbers = sgGeneral.add(new BoolSetting.Builder()
        .name("PingNumbers")
        .description("Shows ping as a number in the tab.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> offlineHeads = sgGeneral.add(new BoolSetting.Builder()
        .name("OfflineHeads")
        .description("Render player heads on offline servers.")
        .defaultValue(true)
        .build()
    );
    
    public BetterTab() {
        super(Category.RENDER, "BetterTab", "Various improvements to the player list hud.");
    }
    
    public long modifyCount(long count) {
        if (isActive()) {
            return (!autoTabSize.get()) ? tabSize.get() : mc.getNetworkHandler().getListedPlayerListEntries().size();
        }
        return count;
    }
    
    public void modifyHeight(LocalIntRef o, LocalIntRef p, int size) {
        if (!isActive()) {
            return;
        }
        
        int newO;
        int newP = 1;
        int totalPlayers = newO = size;
        
        while (newO > (!autoTabSize.get() ? columnHeight.get() : (totalPlayers <= 100 ? 20 : 20 + totalPlayers / 10))) {
            newO = (totalPlayers + ++newP - 1) / newP;
        }
        
        o.set(newO);
        p.set(newP);
    }
    
    public boolean redirectIsEncrypted(boolean isEncrypted) {
        return (isActive() && offlineHeads.get()) || isEncrypted;
    }
    
    public void onRenderLatencyIcon(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        if (isActive() && pingNumbers.get()) {
            final int latency = entry.getLatency();
            final int color = latency < 150 ? 0xFF00E970 : latency < 300 ? 0xFFE7D020 : 0xFFD74238;
            
            Render2D.withVanilla(() -> {
                Render2D.beginFrame(context, Render2D.Space.VANILLA);
                try {
                    int size = 7;
                    String text = String.valueOf(latency);
                    int x2 = x + width - (int) MsdfFont.MONTSERRAT_SEMIBOLD.width(String.valueOf(latency), size);
                    
                    Render2D.glowShape(
                        Render2D.glowShapeOptions().radius(10).intensity(4).cutout(false),
                        () -> Render2D.msdf(
                            new BuiltMsdf(
                                MsdfFont.MONTSERRAT_SEMIBOLD,
                                text,
                                x2,
                                y,
                                size,
                                color
                            )
                        )
                    );
                    
                    Render2D.msdf(
                        new BuiltMsdf(
                            MsdfFont.MONTSERRAT_SEMIBOLD,
                            String.valueOf(latency),
                            x2,
                            y,
                            size,
                            color
                        )//.withOutline(
//                            0.05f,
//                            0xFF000000
//                        )
                    );
                    Render2D.flush();
                } finally {
                    Render2D.endFrame();
                }
            });
            
            ci.cancel();
        }
    }
    
    public void onRenderPlayerBackground(DrawContext context, int x1, int y1, int x2, int y2, int color, Operation<Void> original, int w, List<PlayerListEntry> entries) {
        int drawColor = color;
        
        if ((highlightSelf.get() || highlightFriends.get()) && w < entries.size()) {
            PlayerListEntry entry = entries.get(w);
            
            if (highlightSelf.get() && Objects.equals(entry.getProfile().name(), mc.player.getGameProfile().name())) {
                drawColor = selfColor.get().getPacked();
            } else if (highlightFriends.get() && Friends.get().isFriend(entry)) {
                drawColor = friendsColor.get().getPacked();
            }
        }
        
        original.call(context, x1, y1, x2, y2, drawColor);
    }
    
}
