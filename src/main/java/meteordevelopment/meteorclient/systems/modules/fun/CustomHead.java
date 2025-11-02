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
import meteordevelopment.meteorclient.config.Setting;
import meteordevelopment.meteorclient.config.SettingGroup;
import meteordevelopment.meteorclient.config.types.BoolSetting;
import meteordevelopment.meteorclient.config.types.IntSetting;
import meteordevelopment.meteorclient.config.types.ItemListSetting;
import meteordevelopment.meteorclient.config.types.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.ComponentMapReader;
import meteordevelopment.meteorclient.utils.world.RegistryUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

public class CustomHead extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgExtra = settings.createGroup("Extra");
    
    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("Select items to be shown on head.")
        .defaultValue(List.of())
        .build()
    );
    
    private final Setting<Boolean> randomise = sgGeneral.add(new BoolSetting.Builder()
        .name("randomise")
        .description("Shuffle the items so that they don't go in order.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in ticks.")
        .defaultValue(2)
        .min(0)
        .sliderMax(20)
        .build()
    );
    
    private final Setting<Boolean> customNBT = sgGeneral.add(new BoolSetting.Builder()
        .name("custom-nbt")
        .description("Apply custom NBT to the items.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<List<String>> NBTList = sgGeneral.add(new StringListSetting.Builder()
        .name("components-list")
        .description("List of strings of components. Example: [minecraft:item_name=\"Test\"]")
        .defaultValue(List.of("[minecraft:item_name=\"Test\",minecraft:enchantment_glint_override=1b]"))
        .visible(customNBT::get)
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
        .description("Blocks armor equip sound.")
        .defaultValue(true)
        .build()
    );
    
    private ComponentMapReader reader;
    private int currentItemIndex, currentComponentsIndex, timer;
    
    public CustomHead() {
        super(Categories.Fun, "custom-head", "Sets custom item in head slot.");
    }
    
    @Override
    public void onActivate() {
        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode only.");
            toggle();
        }
        
        reader = new ComponentMapReader(RegistryUtils.REGISTRY_ACCESS);
        currentItemIndex = 0;
        currentComponentsIndex = 0;
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
        if (event.sound.getId().toString().startsWith("minecraft:item.armor.equip") && blockSound.get()) {
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
            
            List<Item> currentItems = items.get();
            
            if (currentItems.isEmpty()) {
                return;
            }
            
            if (randomise.get()) {
                Collections.shuffle(currentItems);
            }
            
            if (currentItemIndex >= currentItems.size()) {
                currentItemIndex = 0;
            }
            
            List<String> currentComponents = customNBT.get() ? NBTList.get() : Collections.emptyList();
            
            if (customNBT.get() && !currentComponents.isEmpty()) {
                if (currentComponentsIndex >= currentComponents.size()) {
                    currentComponentsIndex = 0;
                }
            }
            
            ItemStack itemStack = new ItemStack(currentItems.get(currentItemIndex));
            
            if (customNBT.get() && !currentComponents.isEmpty()) {
                String components = currentComponents.get(currentComponentsIndex);
                try {
                    itemStack.applyComponentsFrom(ComponentMap.of(itemStack.getComponents(), reader.consume(new StringReader(components))));
                } catch (CommandSyntaxException exception) {
                    error(exception.getMessage());
                }
            }
            
            mc.player.getInventory().setStack(39, itemStack);
            mc.interactionManager.clickCreativeStack(itemStack, 5);
            
            currentItemIndex = (currentItemIndex + 1) % currentItems.size();
            if (customNBT.get() && !currentComponents.isEmpty()) {
                currentComponentsIndex = (currentComponentsIndex + 1) % currentComponents.size();
            }
            
            timer = 0;
        }
    }
    
}
