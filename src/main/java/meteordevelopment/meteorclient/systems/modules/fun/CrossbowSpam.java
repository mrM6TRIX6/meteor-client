/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.fun;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.settings.impl.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.ComponentMapReader;
import meteordevelopment.meteorclient.utils.world.RegistryUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class CrossbowSpam extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgExtra = settings.createGroup("Extra");
    
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in ticks.")
        .defaultValue(2)
        .min(0)
        .sliderMax(20)
        .build()
    );
    
    private final Setting<String> crossbowNBT = sgGeneral.add(new StringSetting.Builder()
        .name("crossbow-nbt")
        .description("NBT components applied to the crossbow.")
        .defaultValue("[minecraft:custom_name={bold:1b,color:\"#FF55FF\",italic:0b,text:\"§k\uD83C\uDDE8\uD83C\uDDF7\uD83C\uDDF4§r ᴄʀᴏꜱꜱʙᴏᴡ §k\uD83C\uDD32\uD83C\uDD41\uD83C\uDD3E§r\"}, minecraft:charged_projectiles=[{components:{\"minecraft:intangible_projectile\":{}},count:1,id:\"minecraft:arrow\"},{components:{\"minecraft:intangible_projectile\":{}},count:1,id:\"minecraft:arrow\"},{components:{\"minecraft:intangible_projectile\":{}},count:1,id:\"minecraft:arrow\"}], minecraft:enchantments={\"minecraft:mending\":1,\"minecraft:multishot\":1,\"minecraft:piercing\":4,\"minecraft:quick_charge\":3,\"minecraft:unbreaking\":3}]")
        .build()
    );
    
    // Extra
    private final Setting<Boolean> toggleOnLog = sgExtra.add(new BoolSetting.Builder()
        .name("toggle-on-log")
        .description("Disables when you disconnect from a server.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> blockSound = sgExtra.add(new BoolSetting.Builder()
        .name("block-sound")
        .description("Blocks crossbow shoot sound.")
        .defaultValue(true)
        .build()
    );
    
    private ComponentMapReader reader;
    private int timer;
    
    public CrossbowSpam() {
        super(Categories.Fun, "crossbow-spam", "Automatically shoots a crossbow very fast. Creative mode only.");
    }
    
    @Override
    public void onActivate() {
        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode only.");
            toggle();
        }
        
        reader = new ComponentMapReader(RegistryUtils.REGISTRY_ACCESS);
        timer = 0;
    }
    
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (toggleOnLog.get()) {
            toggle();
        }
    }
    
    @EventHandler
    private void onPlaySound(PlaySoundEvent event) {
        if (event.sound.getId().toString().startsWith("minecraft:item.crossbow.shoot") && blockSound.get()) {
            event.cancel();
        }
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (++timer > delay.get()) {
            if (!mc.player.getAbilities().creativeMode) {
                error("Creative mode only.");
                toggle();
                return;
            }
            
            ItemStack itemStack = new ItemStack(Items.CROSSBOW);
            
            try {
                ComponentMap componentsToApply = reader.consume(new StringReader(crossbowNBT.get()));
                if (!componentsToApply.contains(DataComponentTypes.CHARGED_PROJECTILES)) {
                    error("You did not specify CHARGED_PROJECTILES component in the crossbow NBT.");
                    toggle();
                }
                itemStack.applyComponentsFrom(componentsToApply);
            } catch (CommandSyntaxException exception) {
                error(exception.getMessage());
                toggle();
            }
            
            mc.player.getInventory().setStack(mc.player.getInventory().getSelectedSlot(), itemStack);
            mc.interactionManager.clickCreativeStack(itemStack, mc.player.getInventory().getSelectedSlot() + 36);
            
            mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND,
                0,
                mc.player.getYaw(),
                mc.player.getPitch()
            ));
            
            timer = 0;
        }
    }
    
}

