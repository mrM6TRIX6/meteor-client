package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.BoolSetting;
import meteordevelopment.meteorclient.settings.impl.IntSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class MaceKill extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> maxPower = sgGeneral.add(new BoolSetting.Builder()
        .name("max-power")
        .title("Maximum Mace Power (Paper/Spigot servers only)")
        .description("Simulates a fall from the highest air gap within 170 blocks")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Integer> fallHeight = sgGeneral.add(new IntSetting.Builder()
        .name("fall-height")
        .description("Simulates a fall from this distance.")
        .defaultValue(22)
        .sliderRange(1, 170)
        .min(1)
        .max(170)
        .visible(() -> !maxPower.get())
        .build()
    );
    
    private final Setting<Boolean> preventDeath = sgGeneral.add(new BoolSetting.Builder()
        .name("prevent-fall-damage")
        .description("Attempts to prevent fall damage even on packet hiccups.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> packetDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-when-blocked")
        .description("Does not send movement packets if the attack was blocked. (prevents death)")
        .defaultValue(true)
        .build()
    );
    
    private Vec3d previousPos;
    
    public MaceKill() {
        super(Categories.COMBAT, "MaceKill", "Makes the mace powerful by faking fall height.");
    }
    
    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null) {
            return;
        }
        if (mc.player.getMainHandStack().getItem() != Items.MACE) {
            return;
        }
        if (!(event.packet instanceof IPlayerInteractEntityC2SPacket packet)) {
            return;
        }
        if (packet.meteor$getType() != PlayerInteractEntityC2SPacket.InteractType.ATTACK) {
            return;
        }
        
        LivingEntity targetEntity;
        try {
            targetEntity = (LivingEntity) packet.meteor$getEntity();
        } catch (Exception e) {
            return;
        }
        
        if (packetDisable.get() && (targetEntity.isBlocking() || targetEntity.isInvulnerable() || targetEntity.isInCreativeMode())) {
            return;
        }
        
        previousPos = mc.player.getEntityPos();
        int blocks = getMaxHeightAbovePlayer();
        int packetsRequired = (int) Math.ceil(Math.abs(blocks / 10.0));
        if (packetsRequired > 20) {
            packetsRequired = 1;
        }
        
        BlockPos isOpenAir1 = mc.player.getBlockPos().add(0, blocks, 0);
        BlockPos isOpenAir2 = mc.player.getBlockPos().add(0, blocks + 1, 0);
        if (!isSafeBlock(isOpenAir1) || !isSafeBlock(isOpenAir2)) {
            return;
        }
        
        if (blocks <= 22) {
            if (mc.player.hasVehicle()) {
                for (int i = 0; i < 4; i++) {
                    mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
                }
                double maxHeight = Math.min(mc.player.getVehicle().getY() + 22, mc.player.getVehicle().getY() + blocks);
                doVehicleTeleports(maxHeight, blocks);
            } else {
                for (int i = 0; i < 4; i++) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
                }
                double heightY = Math.min(mc.player.getY() + 22, mc.player.getY() + blocks);
                doPlayerTeleports(heightY);
            }
        } else {
            if (mc.player.hasVehicle()) {
                for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                    mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
                }
                double maxHeight = mc.player.getVehicle().getY() + blocks;
                doVehicleTeleports(maxHeight, blocks);
            } else {
                for (int i = 0; i < packetsRequired - 1; i++) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
                }
                double heightY = mc.player.getY() + blocks;
                doPlayerTeleports(heightY);
            }
        }
    }
    
    private void doPlayerTeleports(double height) {
        PlayerMoveC2SPacket movePacket = new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), height, mc.player.getZ(), false, mc.player.horizontalCollision);
        
        PlayerMoveC2SPacket homePacket = new PlayerMoveC2SPacket.PositionAndOnGround(
            previousPos.getX(), previousPos.getY(), previousPos.getZ(),
            false, mc.player.horizontalCollision
        );
        
        if (preventDeath.get()) {
            homePacket = new PlayerMoveC2SPacket.PositionAndOnGround(
                previousPos.getX(), previousPos.getY() + 0.25, previousPos.getZ(),
                false, mc.player.horizontalCollision
            );
        }
        
        ((IPlayerMoveC2SPacket) homePacket).meteor$setTag(1337);
        ((IPlayerMoveC2SPacket) movePacket).meteor$setTag(1337);
        
        mc.player.networkHandler.sendPacket(movePacket);
        mc.player.networkHandler.sendPacket(homePacket);
        
        if (preventDeath.get()) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.1, mc.player.getVelocity().z);
            mc.player.fallDistance = 0;
        }
    }
    
    private void doVehicleTeleports(double height, int blocks) {
        mc.player.getVehicle().setPosition(mc.player.getVehicle().getX(), height + blocks, mc.player.getVehicle().getZ());
        mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
        mc.player.getVehicle().setPosition(previousPos);
        mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle()));
    }
    
    private int getMaxHeightAbovePlayer() {
        BlockPos playerPos = mc.player.getBlockPos();
        int maxHeight = playerPos.getY() + (maxPower.get() ? 170 : fallHeight.get());
        
        for (int i = maxHeight; i > playerPos.getY(); i--) {
            BlockPos up1 = new BlockPos(playerPos.getX(), i, playerPos.getZ());
            BlockPos up2 = up1.up(1);
            if (isSafeBlock(up1) && isSafeBlock(up2)) {
                return i - playerPos.getY();
            }
        }
        return 0; // Return 0 if no suitable position is found
    }
    
    private boolean isSafeBlock(BlockPos pos) {
        return mc.world.getBlockState(pos).isReplaceable() && mc.world.getFluidState(pos).isEmpty() && !mc.world.getBlockState(pos).isOf(Blocks.POWDER_SNOW);
    }
    
}
