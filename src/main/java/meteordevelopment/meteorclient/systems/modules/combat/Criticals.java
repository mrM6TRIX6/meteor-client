/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;


import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.settings.impl.EnumChoiceSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.IDisplayName;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class Criticals extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMace = settings.createGroup("Mace");
    
    private final Setting<Mode> mode = sgGeneral.add(new EnumChoiceSetting.Builder<Mode>()
        .name("mode")
        .description("The mode on how Criticals will function.")
        .defaultValue(Mode.PACKET)
        .build()
    );
    
    private final Setting<Boolean> ka = sgGeneral.add(new BoolSetting.Builder()
        .name("only-killaura")
        .description("Only performs crits when using killaura.")
        .defaultValue(false)
        .visible(() -> mode.get() != Mode.NONE)
        .build()
    );
    
    private final Setting<Boolean> mace = sgMace.add(new BoolSetting.Builder()
        .name("smash-attack")
        .description("Will always perform smash attacks when using a mace.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Double> extraHeight = sgMace.add(new DoubleSetting.Builder()
        .name("additional-height")
        .description("The amount of additional height to spoof. More height means more damage.")
        .defaultValue(0.0)
        .min(0)
        .sliderRange(0, 100)
        .visible(mace::get)
        .build()
    );
    
    private PlayerInteractEntityC2SPacket attackPacket;
    private HandSwingC2SPacket swingPacket;
    private boolean sendPackets;
    private int sendTimer;
    private double lastY;
    private boolean waitingForPeak;
    
    public Criticals() {
        super(Categories.COMBAT, "Criticals", "Performs critical attacks when you hit your target.");
    }
    
    @Override
    public void onActivate() {
        attackPacket = null;
        swingPacket = null;
        sendPackets = false;
        sendTimer = 0;
        lastY = 0;
        waitingForPeak = false;
    }
    
    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof IPlayerInteractEntityC2SPacket packet && packet.meteor$getType() == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {
            if (mace.get() && mc.player.getMainHandStack().getItem() instanceof MaceItem) {
                if (mc.player.isGliding()) {
                    return;
                }
                
                sendPacket(0);
                sendPacket(1.501 + extraHeight.get());
                sendPacket(0);
            } else {
                if (skipCrit()) {
                    return;
                }
                
                Entity entity = packet.meteor$getEntity();
                
                if (!(entity instanceof LivingEntity) || (entity != Modules.get().get(KillAura.class).getTarget() && ka.get())) {
                    return;
                }
                
                switch (mode.get()) {
                    case PACKET -> {
                        sendPacket(0.0625);
                        sendPacket(0);
                    }
                    case BYPASS -> {
                        sendPacket(0.11);
                        sendPacket(0.1100013579);
                        sendPacket(0.0000013579);
                    }
                    case JUMP, MINI_JUMP -> {
                        if (!sendPackets) {
                            sendPackets = true;
                            attackPacket = (PlayerInteractEntityC2SPacket) event.packet;
                            
                            if (mode.get() == Mode.JUMP) {
                                mc.player.jump();
                                waitingForPeak = true;
                                lastY = mc.player.getY();
                            } else {
                                ((IVec3d) mc.player.getVelocity()).meteor$setY(0.25);
                                sendTimer = 4;
                            }
                            event.cancel();
                        }
                    }
                }
            }
        } else if (event.packet instanceof HandSwingC2SPacket && mode.get() != Mode.PACKET) {
            if (skipCrit()) {
                return;
            }
            
            if (sendPackets && swingPacket == null) {
                swingPacket = (HandSwingC2SPacket) event.packet;
                event.cancel();
            }
        }
    }
    
    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (sendPackets) {
            if (mode.get() == Mode.JUMP && waitingForPeak) {
                double currentY = mc.player.getY();
                if (currentY <= lastY) {
                    waitingForPeak = false;
                    sendTimer = 0; // Attack on next tick after reaching peak
                }
                lastY = currentY;
                return;
            }
            
            if (sendTimer <= 0) {
                sendPackets = false;
                
                if (attackPacket == null || swingPacket == null) {
                    sendPackets = false;
                    return;
                }
                mc.getNetworkHandler().sendPacket(attackPacket);
                mc.getNetworkHandler().sendPacket(swingPacket);
                
                attackPacket = null;
                swingPacket = null;
                
                sendPackets = false;
            } else {
                sendTimer--;
            }
        }
    }
    
    private void sendPacket(double height) {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        
        PlayerMoveC2SPacket packet = new PlayerMoveC2SPacket.PositionAndOnGround(x, y + height, z, false, mc.player.horizontalCollision);
        ((IPlayerMoveC2SPacket) packet).meteor$setTag(1337);
        
        mc.player.networkHandler.sendPacket(packet);
    }
    
    private boolean skipCrit() {
        if (EntityUtils.isInCobweb(mc.player) && (mode.get() == Mode.JUMP || mode.get() == Mode.MINI_JUMP)) {
            return true;
        }
        return !mc.player.isOnGround() || mc.player.isSubmergedInWater() || mc.player.isInLava() || mc.player.isClimbing();
    }
    
    @Override
    public String getInfoString() {
        return mode.get().name();
    }
    
    private enum Mode implements IDisplayName {
        
        NONE("None"),
        PACKET("Packet"),
        BYPASS("Bypass"),
        JUMP("Jump"),
        MINI_JUMP("Mini Jump");
        
        private final String displayName;
        
        Mode(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
    }
    
}
