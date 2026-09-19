/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.NametagUtils;
import meteordevelopment.meteorclient.renderer.engine.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.ColorSetting;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.settings.impl.EnumChoiceSetting;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import meteordevelopment.meteorclient.utils.render.ui.msdf.BuiltMsdf;
import meteordevelopment.meteorclient.utils.render.ui.msdf.MsdfFont;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.dimension.DimensionType;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LogoutSpots extends Module {
    
    private static final MsdfFont FONT = MsdfFont.MONTSERRAT_MEDIUM;
    private static final float TEXT_SIZE = 8.0f;
    
    private static final Color GREEN = new Color(25, 225, 25);
    private static final Color ORANGE = new Color(225, 105, 25);
    private static final Color RED = new Color(225, 25, 25);
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    
    // General
    
    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("Scale")
        .description("The scale.")
        .defaultValue(1)
        .min(0)
        .build()
    );
    
    private final Setting<Boolean> fullHeight = sgGeneral.add(new BoolSetting.Builder()
        .name("FullHeight")
        .description("Displays the height as the player's full height.")
        .defaultValue(true)
        .build()
    );
    
    // Render
    
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumChoiceSetting.Builder<ShapeMode>()
        .name("ShapeMode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.BOTH)
        .build()
    );
    
    private final Setting<Color> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("SideColor")
        .description("The side color.")
        .defaultValue(new Color(255, 0, 255, 55))
        .build()
    );
    
    private final Setting<Color> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("LineColor")
        .description("The line color.")
        .defaultValue(new Color(255, 0, 255))
        .build()
    );
    
    private final Setting<Color> nameColor = sgRender.add(new ColorSetting.Builder()
        .name("NameColor")
        .description("The name color.")
        .defaultValue(new Color(255, 255, 255))
        .build()
    );
    
    private final Setting<Color> nameBackgroundColor = sgRender.add(new ColorSetting.Builder()
        .name("NameBackgroundColor")
        .description("The name background color.")
        .defaultValue(new Color(0, 0, 0, 75))
        .build()
    );
    
    private final List<Entry> players = new ArrayList<>();
    
    private final List<PlayerListEntry> lastPlayerList = new ArrayList<>();
    private final List<PlayerEntity> lastPlayers = new ArrayList<>();
    
    private int timer;
    private DimensionType lastDimension;
    
    public LogoutSpots() {
        super(Category.RENDER, "LogoutSpots", "Displays a box where another player has logged out at.");
        lineColor.onChanged();
    }
    
    @Override
    public void onActivate() {
        lastPlayerList.addAll(mc.getNetworkHandler().getPlayerList());
        updateLastPlayers();
        
        timer = 10;
        lastDimension = mc.world.getDimension();
    }
    
    @Override
    public void onDeactivate() {
        players.clear();
        lastPlayerList.clear();
    }
    
    private void updateLastPlayers() {
        lastPlayers.clear();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity) {
                lastPlayers.add((PlayerEntity) entity);
            }
        }
    }
    
    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (event.entity instanceof PlayerEntity) {
            int toRemove = -1;
            
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).uuid.equals(event.entity.getUuid())) {
                    toRemove = i;
                    break;
                }
            }
            
            if (toRemove != -1) {
                players.remove(toRemove);
            }
        }
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.getNetworkHandler().getPlayerList().size() != lastPlayerList.size()) {
            for (PlayerListEntry entry : lastPlayerList) {
                if (mc.getNetworkHandler().getPlayerList().stream().anyMatch(playerListEntry -> playerListEntry.getProfile().equals(entry.getProfile()))) {
                    continue;
                }
                
                for (PlayerEntity player : lastPlayers) {
                    if (player.getUuid().equals(entry.getProfile().id())) {
                        add(new Entry(player));
                    }
                }
            }
            
            lastPlayerList.clear();
            lastPlayerList.addAll(mc.getNetworkHandler().getPlayerList());
            updateLastPlayers();
        }
        
        if (timer <= 0) {
            updateLastPlayers();
            timer = 10;
        } else {
            timer--;
        }
        
        DimensionType dimension = mc.world.getDimension();
        if (dimension != lastDimension) {
            players.clear();
        }
        lastDimension = dimension;
    }
    
    private void add(Entry entry) {
        players.removeIf(player -> player.uuid.equals(entry.uuid));
        players.add(entry);
    }
    
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        for (Entry player : players) {
            player.render3D(event);
        }
    }
    
    @EventHandler
    private void onRender2D(Render2DEvent event) {
        for (Entry player : players) {
            player.render2D(event.drawContext);
        }
    }
    
    @Override
    public String getInfoString() {
        return Integer.toString(players.size());
    }
    
    private static final Vector3d pos = new Vector3d();
    
    private class Entry {
        
        public final double x, y, z;
        public final double xWidth, zWidth, halfWidth, height;
        
        public final UUID uuid;
        public final String name;
        public final int health, maxHealth;
        public final String healthText;
        
        public Entry(PlayerEntity entity) {
            halfWidth = entity.getWidth() / 2;
            x = entity.getX() - halfWidth;
            y = entity.getY();
            z = entity.getZ() - halfWidth;
            
            xWidth = entity.getBoundingBox().getLengthX();
            zWidth = entity.getBoundingBox().getLengthZ();
            height = entity.getBoundingBox().getLengthY();
            
            uuid = entity.getUuid();
            name = entity.getName().getString();
            health = Math.round(entity.getHealth() + entity.getAbsorptionAmount());
            maxHealth = Math.round(entity.getMaxHealth() + entity.getAbsorptionAmount());
            
            healthText = " " + health;
        }
        
        public void render3D(Render3DEvent event) {
            if (fullHeight.get()) {
                event.renderer.box(x, y, z, x + xWidth, y + height, z + zWidth, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            } else {
                event.renderer.sideHorizontal(x, y, z, x + xWidth, z, sideColor.get(), lineColor.get(), shapeMode.get());
            }
        }
        
        public void render2D(DrawContext context) {
            if (!PlayerUtils.isWithinCamera(x, y, z, mc.options.getViewDistance().getValue() * 16)) {
                return;
            }
            
            pos.set(x + halfWidth, y + height + 0.5, z + halfWidth);
            
            if (!NametagUtils.to2D(pos, scale.get())) {
                return;
            }
            
            double healthPercentage = (double) health / maxHealth;
            
            Color healthColor;
            if (healthPercentage <= 0.333) {
                healthColor = RED;
            } else if (healthPercentage <= 0.666) {
                healthColor = ORANGE;
            } else {
                healthColor = GREEN;
            }
            
            Text text = Text.empty()
                .append(Text.literal(name).styled(s -> nameColor.get().styleWith(s)))
                .append(Text.literal(healthText).styled(healthColor::styleWith));
            
            float lineWidth = FONT.width(text, TEXT_SIZE);
            float lineHeight = FONT.height(TEXT_SIZE);
            
            NametagUtils.render(context, pos, () -> {
                Render2D.rect(-lineWidth / 2 - 2, -1, lineWidth + 4, lineHeight + 2, 1, nameBackgroundColor.get().getPacked());
                Render2D.msdf(new BuiltMsdf(FONT, text, (int) (-lineWidth / 2), 0, (int) TEXT_SIZE));
            });
        }
        
    }
    
}
