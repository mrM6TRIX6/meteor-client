/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.fun;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.settings.impl.ItemListSetting;
import meteordevelopment.meteorclient.settings.impl.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.ComponentMapReader;
import meteordevelopment.meteorclient.utils.player.InventoryUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtOps;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ItemGenerator extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgExtra = settings.createGroup("Extra");
    
    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
        .name("Items")
        .description("Select items to be shown on head.")
        .defaultValue(List.of())
        .build()
    );
    
    private final Setting<Boolean> randomise = sgGeneral.add(new BoolSetting.Builder()
        .name("Randomise")
        .description("Shuffle the items so that they don't go in order.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Integer> power = sgGeneral.add(new IntSetting.Builder()
        .name("Power")
        .description("How many slots filled and packets sent on tick.")
        .defaultValue(2)
        .range(1, 35)
        .sliderRange(1, 35)
        .build()
    );
    
    private final Setting<Integer> dropDelay = sgGeneral.add(new IntSetting.Builder()
        .name("DropDelay")
        .description("How often (in ticks) the generated item is dropped.")
        .defaultValue(2)
        .min(0)
        .sliderMax(20)
        .build()
    );
    
    private final Setting<Integer> updateDelay = sgGeneral.add(new IntSetting.Builder()
        .name("UpdateDelay")
        .description("How often (in ticks) the generated item is switched. Does NOT affect drop speed.")
        .defaultValue(2)
        .min(0)
        .sliderMax(20)
        .build()
    );
    
    private final Setting<Boolean> customNBT = sgGeneral.add(new BoolSetting.Builder()
        .name("CustomNBT")
        .description("Apply custom NBT to the items.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<List<String>> nbtList = sgGeneral.add(new StringListSetting.Builder()
        .name("ComponentsList")
        .description("List of strings of components. Example: [minecraft:item_name=\"Test\"]")
        .defaultValue(List.of("[minecraft:item_name=\"Test\",minecraft:enchantment_glint_override=1b]"))
        .visible(customNBT::get)
        .wide()
        .build()
    );
    
    private final Setting<Integer> stackSize = sgGeneral.add(new IntSetting.Builder()
        .name("StackSize")
        .description("How many items to place in each stack. Doesn't seem to affect performance.")
        .defaultValue(1)
        .range(1, 99)
        .sliderRange(1, 99)
        .build()
    );
    
    // Extra
    
    private final Setting<Boolean> toggleOnLog = sgExtra.add(new BoolSetting.Builder()
        .name("ToggleOnLog")
        .description("Disables when you disconnect from a server.")
        .defaultValue(true)
        .build()
    );
    
    private ComponentMapReader reader;
    private int currentItemIndex, currentComponentsIndex;
    private int dropTimer, updateTimer;
    private ItemStack currentItemStack;
    
    public ItemGenerator() {
        super(Category.FUN, "ItemGenerator", "Generates selected items and drops them from your inventory.");
    }
    
    @Override
    public void onActivate() {
        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode only.");
            toggle();
            return;
        }
        
        reader = new ComponentMapReader(mc.player.getRegistryManager().getOps(NbtOps.INSTANCE));
        currentItemIndex = 0;
        currentComponentsIndex = 0;
        dropTimer = 0;
        updateTimer = 0;
        
        updateItemStack();
    }
    
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (toggleOnLog.get()) {
            toggle();
        }
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode only.");
            toggle();
            return;
        }
        
        if (++updateTimer >= updateDelay.get()) {
            updateItemStack();
            updateTimer = 0;
        }
        
        if (++dropTimer >= dropDelay.get()) {
            if (currentItemStack != null && !currentItemStack.isEmpty()) {
                for (int i = 0; i < power.get(); i++) {
                    InventoryUtils.clickCreativeStack(currentItemStack.copy(), SlotUtils.MAIN_START + i, true);
                }
                for (int i = 0; i < power.get(); i++) {
                    InventoryUtils.drop().slotId(SlotUtils.MAIN_START + i);
                }
            }
            dropTimer = 0;
        }
    }
    
    private void updateItemStack() {
        List<Item> currentItems = items.get();
        
        if (currentItems.isEmpty()) {
            currentItemStack = null;
            return;
        }
        
        Item item;
        if (randomise.get()) {
            item = currentItems.get(ThreadLocalRandom.current().nextInt(currentItems.size()));
        } else {
            if (currentItemIndex >= currentItems.size()) currentItemIndex = 0;
            item = currentItems.get(currentItemIndex);
            currentItemIndex = (currentItemIndex + 1) % currentItems.size();
        }
        
        ItemStack itemStack = new ItemStack(item);
        itemStack.setCount(stackSize.get());
        
        List<String> currentComponents = customNBT.get() ? nbtList.get() : Collections.emptyList();
        
        if (customNBT.get() && !currentComponents.isEmpty()) {
            if (currentComponentsIndex >= currentComponents.size()) currentComponentsIndex = 0;
            
            String components = currentComponents.get(currentComponentsIndex);
            try {
                itemStack.applyComponentsFrom(ComponentMap.of(itemStack.getComponents(), reader.consume(new StringReader(components))));
            } catch (CommandSyntaxException exception) {
                error(exception.getMessage());
            }
            
            currentComponentsIndex = (currentComponentsIndex + 1) % currentComponents.size();
        }
        
        currentItemStack = itemStack;
    }
}