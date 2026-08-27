/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world.litematica;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.color.SettingColor;
import meteordevelopment.meteorclient.renderer.engine.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.name.IDisplayName;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class LitematicaWalker extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPath = settings.createGroup("Path");
    private final SettingGroup sgMovement = settings.createGroup("Movement");
    private final SettingGroup sgPrinter = settings.createGroup("Printer");
    private final SettingGroup sgRender = settings.createGroup("Render");
    
    private final Setting<Area> area = sgGeneral.add(new EnumChoiceSetting.Builder<Area>()
        .name("Area")
        .description("Where the walked area comes from.")
        .defaultValue(Area.MANUAL)
        .onChanged(value -> invalidate())
        .build());
    
    private final Setting<BlockPos> corner1 = sgGeneral.add(new BlockPosSetting.Builder()
        .name("Corner1")
        .description("The first corner of the area.")
        .visible(() -> area.get() == Area.MANUAL)
        .onChanged(value -> invalidate())
        .build());
    
    private final Setting<BlockPos> corner2 = sgGeneral.add(new BlockPosSetting.Builder()
        .name("Corner2")
        .description("The second corner of the area.")
        .visible(() -> area.get() == Area.MANUAL)
        .onChanged(value -> invalidate())
        .build());
    
    private final Setting<Axis> axis = sgPath.add(new EnumChoiceSetting.Builder<Axis>()
        .name("Axis")
        .description("The axis the lanes run along. Auto picks the longer side of the area.")
        .defaultValue(Axis.AUTO)
        .onChanged(value -> invalidate())
        .build());
    
    private final Setting<Integer> spacing = sgPath.add(new IntSetting.Builder()
        .name("Spacing")
        .description("How many blocks to move sideways after turning around at the end of a lane.")
        .defaultValue(4)
        .min(1).sliderMin(1)
        .max(64).sliderMax(16)
        .onChanged(value -> invalidate())
        .build());
    
    private final Setting<Vertical> vertical = sgPath.add(new EnumChoiceSetting.Builder<Vertical>()
        .name("Vertical")
        .description("Whether to also walk the area layer by layer, which needs flight.")
        .defaultValue(Vertical.NONE)
        .onChanged(value -> invalidate())
        .build());
    
    private final Setting<Integer> layerSpacing = sgPath.add(new IntSetting.Builder()
        .name("LayerSpacing")
        .description("How many blocks to move up after a whole layer has been walked.")
        .defaultValue(3)
        .min(1).sliderMin(1)
        .max(64).sliderMax(16)
        .visible(() -> vertical.get() == Vertical.FLY)
        .onChanged(value -> invalidate())
        .build());
    
    private final Setting<Integer> heightOffset = sgPath.add(new IntSetting.Builder()
        .name("HeightOffset")
        .description("Blocks to add to the height of every layer, so you can fly above the layer you are printing.")
        .defaultValue(1)
        .min(-16).sliderMin(-8)
        .max(16).sliderMax(8)
        .visible(() -> vertical.get() == Vertical.FLY)
        .onChanged(value -> invalidate())
        .build());
    
    private final Setting<Boolean> loop = sgPath.add(new BoolSetting.Builder()
        .name("Loop")
        .description("Walk the path again backwards instead of disabling once the end is reached.")
        .defaultValue(false)
        .build());
    
    private final Setting<Double> speed = sgMovement.add(new DoubleSetting.Builder()
        .name("Speed")
        .description("Walking speed in blocks per second.")
        .defaultValue(4.3)
        .min(0.1).sliderMin(1)
        .max(20).sliderMax(10)
        .build());
    
    private final Setting<Boolean> autoJump = sgMovement.add(new BoolSetting.Builder()
        .name("AutoJump")
        .description("Jump when you walk into a block.")
        .defaultValue(true)
        .build());
    
    private final Setting<Boolean> pauseOnInput = sgMovement.add(new BoolSetting.Builder()
        .name("PauseOnInput")
        .description("Hand movement back to you while you press a movement key.")
        .defaultValue(true)
        .build());
    
    private final Setting<Integer> stuckTimeout = sgMovement.add(new IntSetting.Builder()
        .name("StuckTimeout")
        .description("Skip to the next point after this many ticks without getting closer to the current one. 0 to never skip.")
        .defaultValue(60)
        .min(0).sliderMin(0)
        .max(400).sliderMax(200)
        .build());
    
    private final Setting<Boolean> waitForPrinter = sgPrinter.add(new BoolSetting.Builder()
        .name("WaitForPrinter")
        .description("Stand still while blocks around you are still missing, so LitematicaPrinter can catch up.")
        .defaultValue(true)
        .build());
    
    private final Setting<Integer> waitRange = sgPrinter.add(new IntSetting.Builder()
        .name("WaitRange")
        .description("How far to look for missing blocks. Keep this at or below the printing range.")
        .defaultValue(3)
        .min(1).sliderMin(1)
        .max(16).sliderMax(8)
        .visible(waitForPrinter::get)
        .build());
    
    private final Setting<Boolean> waitInsideArea = sgPrinter.add(new BoolSetting.Builder()
        .name("WaitInsideArea")
        .description("Only wait for blocks inside the area. Turn this off to also wait for blocks around it, like the higher parts of a wall.")
        .defaultValue(true)
        .visible(waitForPrinter::get)
        .build());
    
    private final Setting<Integer> maxWait = sgPrinter.add(new IntSetting.Builder()
        .name("MaxWait")
        .description("Move on after waiting this many ticks in one spot, so blocks the printer cannot place don't stop the walk. 0 to wait forever.")
        .defaultValue(200)
        .min(0).sliderMin(0)
        .max(2000).sliderMax(600)
        .visible(waitForPrinter::get)
        .build());
    
    private final Setting<Boolean> renderArea = sgRender.add(new BoolSetting.Builder()
        .name("RenderArea")
        .description("Renders the walked area.")
        .defaultValue(true)
        .build());
    
    private final Setting<Boolean> renderTarget = sgRender.add(new BoolSetting.Builder()
        .name("Target")
        .description("Renders the point currently being walked to.")
        .defaultValue(true)
        .build());
    
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumChoiceSetting.Builder<ShapeMode>()
        .name("ShapeMode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.BOTH)
        .build());
    
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("SideColor")
        .description("The side color.")
        .defaultValue(new SettingColor(0, 255, 180, 25))
        .build());
    
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("LineColor")
        .description("The line color.")
        .defaultValue(new SettingColor(0, 255, 180, 255))
        .build());
    
    private static final double REACHED = 0.05;
    private static final int MAX_POINTS = 1 << 16;
    
    private final List<Vec3d> path = new ArrayList<>();
    private final BlockPos.Mutable scanPos = new BlockPos.Mutable();
    private BlockPos min;
    private BlockPos max;
    private int index;
    
    /**
     * Which way the path is walked, flipped instead of restarting from the first point when looping, so no time is
     * spent walking back to the corner the path starts in.
     */
    private int step = 1;
    
    private double lastDistance;
    private int stuckTicks;
    private int waitTicks;
    private boolean flightWarned;
    private boolean paused;
    private boolean waiting;
    
    public LitematicaWalker() {
        super(Category.WORLD, "LitematicaWalker", "Walks an area in a snake pattern, so LitematicaPrinter can print it without you steering.");
    }
    
    @Override
    public void onActivate() {
        reset();
        path.clear();
        min = null;
        max = null;
        flightWarned = false;
    }
    
    @Override
    public void onDeactivate() {
        path.clear();
        min = null;
        max = null;
    }
    
    @Override
    public String getInfoString() {
        return path.isEmpty() ? null : (index + 1) + "/" + path.size();
    }
    
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        
        if (path.isEmpty() && !buildPath()) {
            toggle();
            return;
        }
        
        paused = pauseOnInput.get() && (PlayerUtils.isMoving() || mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed());
        if (paused) {
            return;
        }
        
        if (vertical.get() == Vertical.FLY && !flightWarned && !canFly()) {
            flightWarned = true;
            warning("Vertical mode needs flight, only walking the current layer.");
        }
        
        if (isWaitingForPrinter()) {
            waiting = true;
            return;
        }
        
        waiting = false;
        
        Vec3d target = path.get(index);
        double distance = distanceTo(target);
        
        if (distance < REACHED) {
            advance();
            return;
        }
        
        // Anything that keeps us from getting closer - a wall, a fence, a block the printer put in the way - would
        // otherwise hold the whole walk at one point forever. What counts as progress follows the speed, so a slow
        // walk is not mistaken for a blocked one.
        if (distance < lastDistance - speed.get() / 80) {
            stuckTicks = 0;
        } else {
            stuckTicks++;
        }
        lastDistance = distance;
        
        if (autoJump.get() && mc.player.horizontalCollision && mc.player.isOnGround() && !flying()) {
            mc.player.jump();
        }
        
        if (stuckTimeout.get() > 0 && stuckTicks > stuckTimeout.get()) {
            warning("Could not reach a point, skipping it.");
            advance();
        }
    }
    
    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (event.type != MovementType.SELF || mc.player == null || paused || path.isEmpty()) {
            return;
        }
        
        if (waiting) {
            ((IVec3d) event.movement).meteor$setXZ(0, 0);
            return;
        }
        
        Vec3d target = path.get(index);
        double perTick = speed.get() / 20;
        
        double dx = target.x - mc.player.getX();
        double dz = target.z - mc.player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        
        double velX = 0;
        double velZ = 0;
        
        if (horizontal > 0) {
            // Never further than what is left, so a point is hit exactly instead of being circled around.
            double scale = Math.min(perTick, horizontal) / horizontal;
            velX = dx * scale;
            velZ = dz * scale;
        }
        
        double velY = event.movement.y;
        
        if (flying()) {
            double dy = target.y - mc.player.getY();
            velY = Math.abs(dy) < perTick ? dy : Math.copySign(perTick, dy);
        }
        
        ((IVec3d) event.movement).meteor$set(velX, velY, velZ);
    }
    
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null) {
            return;
        }
        
        if (renderArea.get() && min != null) {
            event.renderer.box(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1,
                sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
        
        if (renderTarget.get() && !path.isEmpty()) {
            Vec3d target = path.get(index);
            double y = flying() ? target.y : mc.player.getY();
            
            event.renderer.box(target.x - 0.25, y, target.z - 0.25, target.x + 0.25, y + 0.5, target.z + 0.25,
                sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            event.renderer.line(mc.player.getX(), mc.player.getY() + mc.player.getStandingEyeHeight(), mc.player.getZ(),
                target.x, y + 0.25, target.z, lineColor.get());
        }
    }
    
    private void reset() {
        index = 0;
        step = 1;
        stuckTicks = 0;
        waitTicks = 0;
        lastDistance = Double.MAX_VALUE;
        paused = false;
        waiting = false;
    }
    
    /**
     * Dropping the path is enough to have it built again on the next tick, which is also where the player the path is
     * laid out around is known to exist.
     */
    private void invalidate() {
        path.clear();
        reset();
    }
    
    private void advance() {
        stuckTicks = 0;
        waitTicks = 0;
        lastDistance = Double.MAX_VALUE;
        
        index += step;
        
        if (index >= 0 && index < path.size()) {
            return;
        }
        
        if (!loop.get()) {
            info("Walked the whole area, disabling.");
            toggle();
            return;
        }
        
        // Turning around lands on the point before the one we are standing on, since the one we just reached would be
        // reached again right away.
        step = -step;
        index = MathHelper.clamp(index + 2 * step, 0, path.size() - 1);
    }
    
    private boolean isWaitingForPrinter() {
        if (!waitForPrinter.get()) {
            return false;
        }
        
        WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
        if (schematic == null || !hasMissingBlocks(schematic)) {
            waitTicks = 0;
            return false;
        }
        
        waitTicks++;
        
        if (maxWait.get() == 0 || waitTicks <= maxWait.get()) {
            return true;
        }
        
        if (waitTicks == maxWait.get() + 1) {
            warning("Waited too long for the printer, moving on.");
        }
        
        return false;
    }
    
    private boolean hasMissingBlocks(WorldSchematic schematic) {
        int r = waitRange.get();
        BlockPos player = mc.player.getBlockPos();
        
        int minX = player.getX() - r;
        int minY = player.getY() - r;
        int minZ = player.getZ() - r;
        int maxX = player.getX() + r;
        int maxY = player.getY() + r;
        int maxZ = player.getZ() + r;
        
        if (waitInsideArea.get()) {
            minX = Math.max(minX, min.getX());
            minY = Math.max(minY, min.getY());
            minZ = Math.max(minZ, min.getZ());
            maxX = Math.min(maxX, max.getX());
            maxY = Math.min(maxY, max.getY());
            maxZ = Math.min(maxZ, max.getZ());
        }
        
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    scanPos.set(x, y, z);
                    
                    if (!player.isWithinDistance(scanPos, r)) {
                        continue;
                    }
                    
                    BlockState required = schematic.getBlockState(scanPos);
                    if (required.isAir() || !required.getFluidState().isEmpty()) {
                        continue;
                    }
                    
                    BlockState current = mc.world.getBlockState(scanPos);
                    if (!current.isReplaceable() || current.getBlock() == required.getBlock()) {
                        continue;
                    }
                    
                    if (!DataManager.getRenderLayerRange().isPositionWithinRange(scanPos)
                        || mc.player.getBoundingBox().intersects(Vec3d.of(scanPos), Vec3d.of(scanPos).add(1, 1, 1))
                        || !required.canPlaceAt(mc.world, scanPos)) {
                        continue;
                    }
                    
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean buildPath() {
        BlockPos corner1;
        BlockPos corner2;
        
        if (area.get() == Area.SCHEMATIC) {
            BlockPos[] bounds = placementBounds();
            
            if (bounds == null) {
                error("No enabled schematic placement to walk over.");
                return false;
            }
            
            corner1 = bounds[0];
            corner2 = bounds[1];
        } else {
            corner1 = this.corner1.get();
            corner2 = this.corner2.get();
        }
        
        min = new BlockPos(Math.min(corner1.getX(), corner2.getX()), Math.min(corner1.getY(), corner2.getY()), Math.min(corner1.getZ(), corner2.getZ()));
        max = new BlockPos(Math.max(corner1.getX(), corner2.getX()), Math.max(corner1.getY(), corner2.getY()), Math.max(corner1.getZ(), corner2.getZ()));
        
        boolean alongX = switch (axis.get()) {
            case X -> true;
            case Z -> false;
            case AUTO -> max.getX() - min.getX() >= max.getZ() - min.getZ();
        };
        
        int laneFrom = alongX ? min.getZ() : min.getX();
        int laneTo = alongX ? max.getZ() : max.getX();
        int walkFrom = alongX ? min.getX() : min.getZ();
        int walkTo = alongX ? max.getX() : max.getZ();
        
        List<Integer> lanes = steps(laneFrom, laneTo, spacing.get());
        List<Integer> layers = vertical.get() == Vertical.FLY
            ? steps(min.getY(), max.getY(), layerSpacing.get())
            : List.of(min.getY());
        
        long points = 2L * lanes.size() * layers.size();
        if (points > MAX_POINTS) {
            error("The area needs %d points, lower the spacing or pick a smaller area.", points);
            return false;
        }
        
        path.clear();
        
        boolean forward = true;
        boolean flipLanes = false;
        
        for (int layer : layers) {
            double y = layer + (vertical.get() == Vertical.FLY ? heightOffset.get() : 0);
            
            for (int i = 0; i < lanes.size(); i++) {
                // Every second layer runs its lanes the other way around, so a layer starts where the one below it
                // ended instead of crossing the whole area first.
                int lane = lanes.get(flipLanes ? lanes.size() - 1 - i : i);
                
                double lanePos = lane + 0.5;
                double from = (forward ? walkFrom : walkTo) + 0.5;
                double to = (forward ? walkTo : walkFrom) + 0.5;
                
                if (alongX) {
                    path.add(new Vec3d(from, y, lanePos));
                    path.add(new Vec3d(to, y, lanePos));
                } else {
                    path.add(new Vec3d(lanePos, y, from));
                    path.add(new Vec3d(lanePos, y, to));
                }
                
                forward = !forward;
            }
            
            flipLanes = !flipLanes;
        }
        
        reset();
        index = nearestPoint();
        
        return true;
    }
    
    private BlockPos[] placementBounds() {
        BlockPos min = null;
        BlockPos max = null;
        
        for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
            if (!placement.isEnabled()) {
                continue;
            }
            
            fi.dy.masa.litematica.selection.Box box = placement.getEclosingBox();
            if (box == null || box.getPos1() == null || box.getPos2() == null) {
                continue;
            }
            
            BlockPos pos1 = box.getPos1();
            BlockPos pos2 = box.getPos2();
            
            if (min == null) {
                min = new BlockPos(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()));
                max = new BlockPos(Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()));
                continue;
            }
            
            min = new BlockPos(Math.min(min.getX(), Math.min(pos1.getX(), pos2.getX())), Math.min(min.getY(), Math.min(pos1.getY(), pos2.getY())), Math.min(min.getZ(), Math.min(pos1.getZ(), pos2.getZ())));
            max = new BlockPos(Math.max(max.getX(), Math.max(pos1.getX(), pos2.getX())), Math.max(max.getY(), Math.max(pos1.getY(), pos2.getY())), Math.max(max.getZ(), Math.max(pos1.getZ(), pos2.getZ())));
        }
        
        return min == null ? null : new BlockPos[] { min, max };
    }
    
    private int nearestPoint() {
        int nearest = 0;
        double nearestDistance = Double.MAX_VALUE;
        
        for (int i = 0; i < path.size(); i++) {
            double distance = distanceTo(path.get(i));
            
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = i;
            }
        }
        
        return nearest;
    }
    
    private double distanceTo(Vec3d point) {
        double dx = point.x - mc.player.getX();
        double dz = point.z - mc.player.getZ();
        double dy = flying() ? point.y - mc.player.getY() : 0;
        
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    private boolean flying() {
        return vertical.get() == Vertical.FLY && canFly();
    }
    
    private boolean canFly() {
        return mc.player.getAbilities().flying || mc.player.isSpectator();
    }
    
    private static List<Integer> steps(int from, int to, int step) {
        List<Integer> values = new ArrayList<>();
        
        for (int value = from; value < to; value += step) {
            values.add(value);
        }
        
        if (values.isEmpty() || values.getLast() != to) {
            values.add(to);
        }
        
        return values;
    }
    
    public enum Area implements IDisplayName {
        
        MANUAL("Manual"),
        SCHEMATIC("Schematic");
        
        private final String displayName;
        
        Area(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum Axis implements IDisplayName {
        
        AUTO("Auto"),
        X("X"),
        Z("Z");
        
        private final String displayName;
        
        Axis(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum Vertical implements IDisplayName {
        
        NONE("None"),
        FLY("Fly");
        
        private final String displayName;
        
        Vertical(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
    }
    
}
