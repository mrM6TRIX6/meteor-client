/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.entity.player.FinishUsingItemEvent;
import meteordevelopment.meteorclient.events.entity.player.StoppedUsingItemEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.EnumChoiceSetting;
import meteordevelopment.meteorclient.settings.impl.StringSetting;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.IDisplayName;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InventoryUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;

public class MiddleClickExtra extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Mode> mode = sgGeneral.add(new EnumChoiceSetting.Builder<Mode>()
        .name("mode")
        .description("Which item to use when you middle click.")
        .defaultValue(Mode.PEARL)
        .build()
    );
    
    private final Setting<Boolean> message = sgGeneral.add(new BoolSetting.Builder()
        .name("send-message")
        .description("Sends a message when you add a player as a friend.")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.ADD_FRIEND)
        .build()
    );
    
    private final Setting<String> friendMessage = sgGeneral.add(new StringSetting.Builder()
        .name("message-to-send")
        .description("Message to send when you add a player as a friend (use %player for the player's name)")
        .defaultValue("/msg %player I just friended you on Meteor.")
        .visible(() -> mode.get() == Mode.ADD_FRIEND)
        .build()
    );
    
    private final Setting<Boolean> quickSwap = sgGeneral.add(new BoolSetting.Builder()
        .name("quick-swap")
        .description("Allows you to use items in your inventory by simulating hotbar key presses. May get flagged by anticheats.")
        .defaultValue(false)
        .visible(() -> mode.get() != Mode.ADD_FRIEND)
        .build()
    );
    
    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Swap back to your original slot when you finish using an item.")
        .defaultValue(false)
        .visible(() -> mode.get() != Mode.ADD_FRIEND && !quickSwap.get())
        .build()
    );
    
    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("notify")
        .description("Notifies you when you do not have the specified item in your hotbar.")
        .defaultValue(true)
        .visible(() -> mode.get() != Mode.ADD_FRIEND)
        .build()
    );
    
    private final Setting<Boolean> disableInCreative = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-in-creative")
        .description("Middle click action is disabled in Creative mode.")
        .defaultValue(true)
        .build()
    );
    
    private boolean isUsing;
    private boolean wasHeld;
    private int itemSlot;
    private int selectedSlot;
    
    public MiddleClickExtra() {
        super(Categories.PLAYER, "MiddleClickExtra", "Perform various actions when you middle click.");
    }
    
    @Override
    public void onDeactivate() {
        stopIfUsing(false);
    }
    
    @EventHandler
    private void onMouseClick(MouseClickEvent event) {
        if (event.action != KeyAction.PRESS || event.button() != GLFW_MOUSE_BUTTON_MIDDLE || mc.currentScreen != null) {
            return;
        }
        
        if (disabledByCreative()) {
            return;
        }
        
        if (mode.get() == Mode.ADD_FRIEND) {
            if (mc.targetedEntity == null) {
                return;
            }
            if (!(mc.targetedEntity instanceof PlayerEntity player)) {
                return;
            }
            
            if (!Friends.get().isFriend(player)) {
                Friends.get().add(new Friend(player));
                info("Added %s to friends", player.getName().getString());
                if (message.get()) {
                    String messageNotify = friendMessage.get().replace("%player", player.getName().getString());
                    ChatUtils.sendPlayerMsg(messageNotify);
                }
                
            } else {
                Friends.get().remove(Friends.get().get(player));
                info("Removed %s from friends", player.getName().getString());
            }
            
            return;
        }
        
        FindItemResult result = InventoryUtils.find(mode.get().item);
        if (!result.found() || !result.isHotbar() && !quickSwap.get()) {
            if (notify.get()) {
                warning("Unable to find specified item.");
            }
            return;
        }
        
        selectedSlot = mc.player.getInventory().getSelectedSlot();
        itemSlot = result.slot();
        wasHeld = result.isMainHand();
        
        if (!wasHeld) {
            if (!quickSwap.get()) {
                InventoryUtils.swap(result.slot(), swapBack.get());
            } else {
                InventoryUtils.quickSwap().fromId(selectedSlot).to(itemSlot);
            }
        }
        
        if (mode.get().immediate) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            swapBack(false);
        } else {
            mc.options.useKey.setPressed(true);
            isUsing = true;
        }
    }
    
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!isUsing) {
            return;
        }
        boolean pressed = true;
        
        if (mc.player.getMainHandStack().getItem() instanceof BowItem) {
            pressed = BowItem.getPullProgress(mc.player.getItemUseTime()) < 1;
        }
        
        mc.options.useKey.setPressed(pressed);
    }
    
    @EventHandler
    private void onPacketSendEvent(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            stopIfUsing(true);
        }
    }
    
    @EventHandler
    private void onStoppedUsingItem(StoppedUsingItemEvent event) {
        stopIfUsing(false);
    }
    
    @EventHandler
    private void onFinishUsingItem(FinishUsingItemEvent event) {
        stopIfUsing(false);
    }
    
    private void stopIfUsing(boolean wasCancelled) {
        if (isUsing) {
            swapBack(wasCancelled);
            mc.options.useKey.setPressed(false);
            isUsing = false;
        }
    }
    
    void swapBack(boolean wasCancelled) {
        if (wasHeld) {
            return;
        }
        
        if (quickSwap.get()) {
            InventoryUtils.quickSwap().fromId(selectedSlot).to(itemSlot);
        } else {
            if (!swapBack.get() || wasCancelled) {
                return;
            }
            InventoryUtils.swapBack();
        }
    }
    
    private boolean disabledByCreative() {
        if (mc.player == null) {
            return false;
        }
        
        return disableInCreative.get() && mc.player.getGameMode() == GameMode.CREATIVE;
    }
    
    private enum Mode implements IDisplayName {
        
        PEARL("Pearl", Items.ENDER_PEARL, true),
        XP("XP", Items.EXPERIENCE_BOTTLE, true),
        ROCKET("Rocket", Items.FIREWORK_ROCKET, true),
        WIND_CHARGE("Wind Charge", Items.WIND_CHARGE, true),
        
        BOW("Bow", Items.BOW, false),
        GAP("Gap", Items.GOLDEN_APPLE, false),
        EGAP("EGap", Items.ENCHANTED_GOLDEN_APPLE, false),
        CHORUS("Chorus", Items.CHORUS_FRUIT, false),
        
        ADD_FRIEND("Add Friend", null, true);
        
        private final String displayName;
        private final Item item;
        private final boolean immediate;
        
        Mode(String displayName, Item item, boolean immediate) {
            this.displayName = displayName;
            this.item = item;
            this.immediate = immediate;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
    }
    
}
