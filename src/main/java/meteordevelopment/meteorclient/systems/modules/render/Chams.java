/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.name.IDisplayName;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;

import java.util.Set;

public class Chams extends Module {
    
    private final SettingGroup sgThroughWalls = settings.createGroup("Through Walls");
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgCrystals = settings.createGroup("Crystals");
    private final SettingGroup sgHand = settings.createGroup("Hand");
    
    // Through walls
    
    public final Setting<Set<EntityType<?>>> entities = sgThroughWalls.add(new EntityTypeListSetting.Builder()
        .name("Entities")
        .description("Select entities to show through walls.")
        .onlyAttackable()
        .build()
    );
    
    public final Setting<Shader> shader = sgThroughWalls.add(new EnumChoiceSetting.Builder<Shader>()
        .name("Shader")
        .description("Renders a shader over of the entities.")
        .defaultValue(Shader.IMAGE)
        .build()
    );
    
    public final Setting<Color> shaderColor = sgThroughWalls.add(new ColorSetting.Builder()
        .name("ShaderColor")
        .description("The color that the shader is drawn with.")
        .defaultValue(new Color(255, 255, 255, 150))
        .visible(() -> shader.get() != Shader.NONE)
        .build()
    );
    
    public final Setting<Boolean> ignoreSelfDepth = sgThroughWalls.add(new BoolSetting.Builder()
        .name("IgnoreSelfDepth")
        .description("Ignores yourself drawing the player.")
        .defaultValue(true)
        .build()
    );
    
    // Players
    
    public final Setting<Boolean> players = sgPlayers.add(new BoolSetting.Builder()
        .name("Players")
        .description("Enables model tweaks for players.")
        .defaultValue(false)
        .build()
    );
    
    public final Setting<Boolean> ignoreSelf = sgPlayers.add(new BoolSetting.Builder()
        .name("IgnoreSelf")
        .description("Ignores yourself when tweaking player models.")
        .defaultValue(false)
        .visible(players::get)
        .build()
    );
    
    public final Setting<Boolean> playersTexture = sgPlayers.add(new BoolSetting.Builder()
        .name("PlayersTexture")
        .description("Enables player model textures.")
        .defaultValue(false)
        .visible(players::get)
        .build()
    );
    
    public final Setting<Color> playersColor = sgPlayers.add(new ColorSetting.Builder()
        .name("PlayersColor")
        .description("The color of player models.")
        .defaultValue(new Color(198, 135, 254, 150))
        .visible(players::get)
        .build()
    );
    
    public final Setting<Double> playersScale = sgPlayers.add(new DoubleSetting.Builder()
        .name("PlayersScale")
        .description("Players scale.")
        .defaultValue(1.0)
        .min(0.0)
        .visible(players::get)
        .build()
    );
    
    // Crystals
    
    public final Setting<Boolean> crystals = sgCrystals.add(new BoolSetting.Builder()
        .name("Crystals")
        .description("Enables model tweaks for end crystals.")
        .defaultValue(false)
        .build()
    );
    
    public final Setting<Double> crystalsScale = sgCrystals.add(new DoubleSetting.Builder()
        .name("CrystalsScale")
        .description("Crystal scale.")
        .defaultValue(0.6)
        .min(0)
        .visible(crystals::get)
        .build()
    );
    
    public final Setting<Double> crystalsBounce = sgCrystals.add(new DoubleSetting.Builder()
        .name("Bounce")
        .description("How high crystals bounce.")
        .defaultValue(0.6)
        .min(0.0)
        .visible(crystals::get)
        .build()
    );
    
    public final Setting<Double> crystalsRotationSpeed = sgCrystals.add(new DoubleSetting.Builder()
        .name("RotationSpeed")
        .description("Multiplies the rotation speed of the crystal.")
        .defaultValue(0.3)
        .min(0)
        .visible(crystals::get)
        .build()
    );
    
    public final Setting<Boolean> crystalsTexture = sgCrystals.add(new BoolSetting.Builder()
        .name("CrystalTexture")
        .description("Whether to render crystal model textures.")
        .defaultValue(true)
        .visible(crystals::get)
        .build()
    );
    
    public final Setting<Color> crystalsColor = sgCrystals.add(new ColorSetting.Builder()
        .name("CrystalColor")
        .description("The color of the of the crystal.")
        .defaultValue(new Color(198, 135, 254, 255))
        .visible(crystals::get)
        .build()
    );
    
    // Hand
    
    public final Setting<Boolean> hand = sgHand.add(new BoolSetting.Builder()
        .name("Enabled")
        .description("Enables tweaks of hand rendering.")
        .defaultValue(false)
        .build()
    );
    
    public final Setting<Boolean> handTexture = sgHand.add(new BoolSetting.Builder()
        .name("HandTexture")
        .description("Whether to render hand textures.")
        .defaultValue(false)
        .visible(hand::get)
        .build()
    );
    
    public final Setting<Color> handColor = sgHand.add(new ColorSetting.Builder()
        .name("HandColor")
        .description("The color of your hand.")
        .defaultValue(new Color(198, 135, 254, 150))
        .visible(hand::get)
        .build()
    );
    
    public static final Identifier BLANK = MeteorClient.identifier("textures/blank.png");
    
    public Chams() {
        super(Category.RENDER, "Chams", "Tweaks rendering of entities.");
    }
    
    public boolean shouldRender(Entity entity) {
        return isActive() && !isShader() && entities.get().contains(entity.getType()) && (entity != mc.player || !ignoreSelfDepth.get());
    }
    
    public boolean isShader() {
        return isActive() && shader.get() != Shader.NONE;
    }
    
    public enum Shader implements IDisplayName {
        
        IMAGE("Image"),
        NONE("None");
        
        private final String displayName;
        
        Shader(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
    }
    
}
