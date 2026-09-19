/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.NametagUtils;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import meteordevelopment.meteorclient.utils.render.ui.msdf.BuiltMsdf;
import meteordevelopment.meteorclient.utils.render.ui.msdf.MsdfFont;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityOwner extends Module {
    
    private static final Color BACKGROUND = new Color(0, 0, 0, 75);
    private static final Color TEXT = new Color(255, 255, 255);
    private static final MsdfFont FONT = MsdfFont.MONTSERRAT_MEDIUM;
    private static final float TEXT_SIZE = 8.0f;
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("Scale")
        .description("The scale of the text.")
        .defaultValue(1)
        .min(0)
        .build()
    );
    
    private final Vector3d pos = new Vector3d();
    private final Map<UUID, String> uuidToName = new HashMap<>();
    
    public EntityOwner() {
        super(Category.RENDER, "EntityOwner", "Displays the name of the player who owns the entity you're looking at.");
    }
    
    @Override
    public void onDeactivate() {
        uuidToName.clear();
    }
    
    @EventHandler
    private void onRender2D(Render2DEvent event) {
        for (Entity entity : mc.world.getEntities()) {
            @Nullable
            LazyEntityReference<LivingEntity> owner;
            
            switch (entity) {
                case TameableEntity tameable -> owner = tameable.getOwnerReference();
                case EnderPearlEntity pearl -> owner = LazyEntityReference.of((LivingEntity) pearl.getOwner());
                default -> {
                    continue;
                }
            }
            
            if (owner != null) {
                Utils.set(pos, entity, event.tickDelta);
                pos.add(0, entity.getEyeHeight(entity.getPose()) + 0.75, 0);
                
                if (NametagUtils.to2D(pos, scale.get())) {
                    renderNametag(event.drawContext, getOwnerName(owner));
                }
            }
        }
    }
    
    private void renderNametag(DrawContext context, String name) {
        Text text = Text.literal(name).styled(TEXT::styleWith);
        float width = FONT.width(text, TEXT_SIZE);
        float height = FONT.height(TEXT_SIZE);
        
        float x = -width / 2;
        float y = -height;
        
        NametagUtils.render(context, pos, () -> {
            Render2D.rect(x - 2, y - 1, width + 4, height + 2, 1, BACKGROUND.getPacked());
            Render2D.msdf(new BuiltMsdf(FONT, text, (int) x, (int) y, (int) TEXT_SIZE));
        });
    }
    
    private String getOwnerName(LazyEntityReference<LivingEntity> owner) {
        // Check if the player is online
        @Nullable
        LivingEntity ownerEntity = LazyEntityReference.resolve(owner, mc.world, LivingEntity.class);
        if (ownerEntity instanceof PlayerEntity playerEntity) {
            return playerEntity.getName().getString();
        }
        
        UUID uuid = owner.getUuid();
        
        // Check cache
        String name = uuidToName.get(uuid);
        if (name != null) {
            return name;
        }
        
        // Makes an HTTP request to Mojang API
        MeteorExecutor.execute(() -> {
            if (isActive()) {
                ProfileResponse res = Http.get("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "")).sendJson(ProfileResponse.class);
                
                if (isActive()) {
                    if (res == null) {
                        uuidToName.put(uuid, "Failed to get name");
                    } else {
                        uuidToName.put(uuid, res.name);
                    }
                }
            }
        });
        
        name = "Retrieving";
        uuidToName.put(uuid, name);
        return name;
    }
    
    private static class ProfileResponse {
        
        public String name;
        
    }
    
}
