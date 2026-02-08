/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.EnumChoiceSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.ITagged;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InventoryUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.attribute.EnvironmentAttributes;

import java.util.function.Predicate;

public class NoFall extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Mode> mode = sgGeneral.add(new EnumChoiceSetting.Builder<Mode>()
        .name("mode")
        .description("The way you are saved from fall damage.")
        .defaultValue(Mode.PACKET)
        .build()
    );
    
    private final Setting<PlacedItem> placedItem = sgGeneral.add(new EnumChoiceSetting.Builder<PlacedItem>()
        .name("placed-item")
        .description("Which block to place.")
        .defaultValue(PlacedItem.BUCKET)
        .visible(() -> mode.get() == Mode.PLACE)
        .build()
    );
    
    private final Setting<PlaceMode> airPlaceMode = sgGeneral.add(new EnumChoiceSetting.Builder<PlaceMode>()
        .name("air-place-mode")
        .description("Whether place mode places before you die or before you take damage.")
        .defaultValue(PlaceMode.BEFORE_DEATH)
        .visible(() -> mode.get() == Mode.AIR_PLACE)
        .build()
    );
    
    private final Setting<Boolean> anchor = sgGeneral.add(new BoolSetting.Builder()
        .name("anchor")
        .description("Centers the player and reduces movement when using bucket or air place mode.")
        .defaultValue(true)
        .visible(() -> mode.get() != Mode.PACKET)
        .build()
    );
    
    private final Setting<Boolean> antiBounce = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-bounce")
        .description("Disables bouncing on slime-block and bed upon landing.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> pauseOnMace = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-mace")
        .description("Pauses NoFall when using a mace.")
        .defaultValue(true)
        .build()
    );
    
    private boolean placedWater;
    private BlockPos targetPos;
    private int timer;
    private boolean prePathManagerNoFall;
    
    public NoFall() {
        super(Categories.MOVEMENT, "NoFall", "Attempts to prevent you from taking fall damage.");
    }
    
    @Override
    public void onActivate() {
        prePathManagerNoFall = PathManagers.get().getSettings().getNoFall().get();
        if (mode.get() == Mode.PACKET) {
            PathManagers.get().getSettings().getNoFall().set(true);
        }
        
        placedWater = false;
    }
    
    @Override
    public void onDeactivate() {
        PathManagers.get().getSettings().getNoFall().set(prePathManagerNoFall);
    }
    
    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (pauseOnMace.get() && mc.player.getMainHandStack().getItem() instanceof MaceItem) {
            return;
        }
        if (mc.player.getAbilities().creativeMode
            || !(event.packet instanceof PlayerMoveC2SPacket)
            || mode.get() != Mode.PACKET
            || ((IPlayerMoveC2SPacket) event.packet).meteor$getTag() == 1337) {
            return;
        }
        
        
        if (!Modules.get().isActive(Flight.class)) {
            if (mc.player.isGliding()) {
                return;
            }
            if (mc.player.getVelocity().y > -0.5) {
                return;
            }
            ((PlayerMoveC2SPacketAccessor) event.packet).meteor$setOnGround(true);
        } else {
            ((PlayerMoveC2SPacketAccessor) event.packet).meteor$setOnGround(true);
        }
    }
    
    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (!Utils.canUpdate()) {
            return;
        }
        
        if (timer > 20) {
            placedWater = false;
            timer = 0;
        }
        
        if (mc.player.getAbilities().creativeMode) {
            return;
        }
        if (pauseOnMace.get() && mc.player.getMainHandStack().getItem() instanceof MaceItem) {
            return;
        }
        
        // AirPlace mode
        if (mode.get() == Mode.AIR_PLACE) {
            // Test if fall damage setting is valid
            if (!airPlaceMode.get().test((float) mc.player.fallDistance)) {
                return;
            }
            
            // Center and place block
            if (anchor.get()) {
                PlayerUtils.centerPlayer(true);
            }
            
            Rotations.rotate(mc.player.getYaw(), 90, Integer.MAX_VALUE, () -> {
                double preY = mc.player.getVelocity().y;
                ((IVec3d) mc.player.getVelocity()).meteor$setY(0);
                
                BlockUtils.place(mc.player.getBlockPos().down(), InventoryUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem), false, 0, true);
                
                ((IVec3d) mc.player.getVelocity()).meteor$setY(preY);
            });
        }
        
        // Bucket mode
        else if (mode.get() == Mode.PLACE) {
            PlacedItem placedItem1 = mc.world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.WATER_EVAPORATES_GAMEPLAY)
                && placedItem.get() == PlacedItem.BUCKET ? PlacedItem.POWDER_SNOW : placedItem.get();
            
            if (mc.player.fallDistance > 3 && !EntityUtils.isAboveWater(mc.player)) {
                Item item = placedItem1.item;
                
                // Place
                FindItemResult findItemResult = InventoryUtils.findInHotbar(item);
                if (!findItemResult.found()) {
                    return;
                }
                
                // Center player
                if (anchor.get()) {
                    PlayerUtils.centerPlayer(true);
                }
                
                // Check if there is a block within 5 blocks
                BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getEntityPos(), mc.player.getEntityPos().subtract(0, 5, 0), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
                
                // Place
                if (result != null && result.getType() == HitResult.Type.BLOCK) {
                    targetPos = result.getBlockPos().up();
                    if (placedItem1 == PlacedItem.BUCKET) {
                        useItem(findItemResult, true, targetPos, true);
                    } else {
                        useItem(findItemResult, placedItem1 == PlacedItem.POWDER_SNOW, targetPos, false);
                    }
                }
            }
            
            // Remove placed
            if (placedWater) {
                timer++;
                if (mc.player.getBlockStateAtPos().getBlock() == placedItem1.block) {
                    useItem(InventoryUtils.findInHotbar(Items.BUCKET), false, targetPos, true);
                } else if (mc.world.getBlockState(mc.player.getBlockPos().down()).getBlock() == Blocks.POWDER_SNOW && mc.player.fallDistance == 0 && placedItem1.block == Blocks.POWDER_SNOW) { //check if the powder snow block is still there and the player is on the ground
                    useItem(InventoryUtils.findInHotbar(Items.BUCKET), false, targetPos.down(), true);
                }
            }
        }
    }
    
    public boolean cancelBounce() {
        return isActive() && antiBounce.get();
    }
    
    private void useItem(FindItemResult item, boolean placedWater, BlockPos blockPos, boolean interactItem) {
        if (!item.found()) {
            return;
        }
        
        if (interactItem) {
            Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 10, true, () -> {
                if (item.isOffhand()) {
                    mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
                } else {
                    InventoryUtils.swap(item.slot(), true);
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    InventoryUtils.swapBack();
                }
            });
        } else {
            BlockUtils.place(blockPos, item, true, 10, true);
        }
        
        this.placedWater = placedWater;
    }
    
    @Override
    public String getInfoString() {
        return mode.get().toString();
    }
    
    private enum Mode implements ITagged {
        
        PACKET("Packet"),
        AIR_PLACE("Air Place"),
        PLACE("Place");
        
        private final String tag;
        
        Mode(String tag) {
            this.tag = tag;
        }
        
        @Override
        public String getTag() {
            return tag;
        }
        
    }
    
    private enum PlacedItem implements ITagged {
        
        BUCKET("Bucket", Items.WATER_BUCKET, Blocks.WATER),
        POWDER_SNOW("Powder Snow", Items.POWDER_SNOW_BUCKET, Blocks.POWDER_SNOW),
        HAY_BALE("Hay Bale", Items.HAY_BLOCK, Blocks.HAY_BLOCK),
        COBWEB("Cobweb", Items.COBWEB, Blocks.COBWEB),
        SLIME_BLOCK("Slime Block", Items.SLIME_BLOCK, Blocks.SLIME_BLOCK);
        
        private final String tag;
        private final Item item;
        private final Block block;
        
        PlacedItem(String tag, Item item, Block block) {
            this.tag = tag;
            this.item = item;
            this.block = block;
        }
        
        @Override
        public String getTag() {
            return tag;
        }
        
    }
    
    private enum PlaceMode implements ITagged {
        
        BEFORE_DAMAGE("Before Damage", height -> height > 2),
        BEFORE_DEATH("Before Death", height -> height > Math.max(PlayerUtils.getTotalHealth(), 2));
        
        private final String tag;
        private final Predicate<Float> fallHeight;
        
        PlaceMode(String tag, Predicate<Float> fallHeight) {
            this.tag = tag;
            this.fallHeight = fallHeight;
        }
        
        @Override
        public String getTag() {
            return tag;
        }
        
        public boolean test(float fallheight) {
            return fallHeight.test(fallheight);
        }
        
    }
    
}
