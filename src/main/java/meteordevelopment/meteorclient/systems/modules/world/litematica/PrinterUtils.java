/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world.litematica;

import meteordevelopment.meteorclient.IMinecraft;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.VerticallyAttachableBlockItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;

/**
 * Rotation aware block placement used by {@link LitematicaPrinter}.
 *
 * <p>Unlike {@link BlockUtils#place} this searches for a hit vector that makes the server place the block in the
 * exact state the schematic asks for (slab half, stair half, facing, axis) and it keeps the player sneaking so
 * clicking a container or a button places the block instead of opening it.
 */
public final class PrinterUtils implements IMinecraft {

    public static final double DEG_TO_RAD = Math.PI / 180.0;
    public static final float DEG_TO_RAD_F = (float) DEG_TO_RAD;
    public static final double RAD_TO_DEG = 180.0 / Math.PI;
    public static final float RAD_TO_DEG_F = (float) RAD_TO_DEG;

    private PrinterUtils() {}

    /**
     * @return whether a placement was attempted. When {@code rotate} is set the interaction is deferred to the
     * rotation manager, so this can only report that the attempt was queued, not that the server accepted it.
     */
    public static boolean place(BlockPos blockPos, Direction direction, SlabType slabType, BlockHalf blockHalf,
        Direction blockHorizontalOrientation, Direction.Axis wantedAxis, boolean airPlace, boolean swingHand,
        boolean rotate, boolean clientSide, int range, Hand hand) {
        if (mc.player == null || mc.world == null) {
            return false;
        }
        if (!BlockUtils.canPlace(blockPos)) {
            return false;
        }

        BlockPos neighbour;
        Vec3d hitPos;

        if (direction == null) {
            if (!airPlace) {
                return false;
            }

            if ((slabType != null && slabType != SlabType.DOUBLE || blockHalf != null
                || blockHorizontalOrientation != null || wantedAxis != null) && !mc.player.isCreative()) {
                return false;
            }

            direction = Direction.UP;
            neighbour = blockPos;
            hitPos = blockPos.toCenterPos();
        } else {
            // Even with air place available the found neighbour is clicked rather than the target itself: clicking a
            // replaceable block makes vanilla derive the placement from where the player is looking instead of from the
            // face, which produces the wrong block variant and the wrong half.
            neighbour = blockPos.offset(direction.getOpposite());
            hitPos = faceCenter(blockPos, direction);
        }

        Direction side = direction;
        Direction against = direction.getOpposite();
        BlockPos placeAgainst = neighbour;

        // Clicking a side face lets the hit height decide the slab / stair half, so aim at the half we actually want
        // instead of at the middle of the face.
        Boolean wantsBottom = wantsBottomHalf(slabType, blockHalf);
        if (wantsBottom != null && side.getAxis() != Direction.Axis.Y) {
            hitPos = new Vec3d(hitPos.x, blockPos.getY() + (wantsBottom ? 0.25 : 0.75), hitPos.z);
        }

        if (!rotate) {
            return interact(new BlockHitResult(hitPos, side, placeAgainst, false), swingHand, hand);
        }

        VoxelShape collisionShape = mc.world.getBlockState(placeAgainst).getCollisionShape(mc.world, placeAgainst);

        if (collisionShape.isEmpty()) {
            Vec3d target = hitPos;

            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), 50, clientSide,
                () -> interact(new BlockHitResult(target, side, placeAgainst, false), swingHand, hand));

            return true;
        }

        Box box = collisionShape.getBoundingBox();

        for (double z = 0.1; z < 0.9; z += 0.2) {
            for (double x = 0.1; x < 0.9; x += 0.2) {
                for (Vec3d multiplier : aabbSideMultipliers(against)) {
                    if (!isMultiplierDesired(multiplier, slabType, blockHalf, against)) {
                        continue;
                    }

                    Vec3d target = new Vec3d(
                        placeAgainst.getX() + box.minX * x + box.maxX * (1 - x),
                        placeAgainst.getY() + box.minY * multiplier.y + box.maxY * (1 - multiplier.y),
                        placeAgainst.getZ() + box.minZ * z + box.maxZ * (1 - z));

                    Rotation rotation = calcRotationFromVec3d(eyePos(), target,
                        new Rotation(mc.player.getYaw(), mc.player.getPitch()));

                    Direction testHorizontalDirection = getHorizontalDirectionFromYaw(rotation.normalize().getYaw());
                    if (blockHorizontalOrientation != null
                        && testHorizontalDirection.getAxis() != blockHorizontalOrientation.getAxis()) {
                        continue;
                    }

                    BlockHitResult result = rayTraceTowards(rotation, range);
                    if (result == null
                        || result.getType() != HitResult.Type.BLOCK
                        || !result.getBlockPos().equals(placeAgainst)
                        || result.getSide() != side) {
                        continue;
                    }

                    Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), 50, clientSide,
                        () -> interact(new BlockHitResult(target, side, placeAgainst, false), swingHand, hand));

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Same as {@link BlockUtils#interact} but forces sneaking instead of clearing it, so clickable blocks are
     * placed against rather than used.
     *
     * @return whether the interaction was accepted
     */
    private static boolean interact(BlockHitResult blockHitResult, boolean swing, Hand hand) {
        if (mc.player == null || mc.interactionManager == null || mc.getNetworkHandler() == null) {
            return false;
        }

        PlayerInput oldInput = mc.player.input.playerInput;
        mc.player.input.playerInput = new PlayerInput(
            oldInput.forward(),
            oldInput.backward(),
            oldInput.left(),
            oldInput.right(),
            oldInput.jump(),
            true,
            oldInput.sprint());

        try {
            ActionResult result = mc.interactionManager.interactBlock(mc.player, hand, blockHitResult);

            if (!result.isAccepted()) {
                return false;
            }

            if (swing) {
                mc.player.swingHand(hand);
            } else {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            }

            return true;
        } finally {
            mc.player.input.playerInput = oldInput;
        }
    }

    /**
     * The click has to land on the face {@code blockPos} shares with its neighbour on {@code side}, not on the center
     * of {@code blockPos}.
     */
    private static Vec3d faceCenter(BlockPos blockPos, Direction side) {
        return blockPos.toCenterPos().subtract(
            side.getOffsetX() * 0.5,
            side.getOffsetY() * 0.5,
            side.getOffsetZ() * 0.5);
    }

    /**
     * Vanilla derives the block variant from the clicked face - a sign becomes a standing or a wall sign, a torch a
     * floor or a wall torch - and the block state component cannot repair a wrong variant afterwards because the two
     * variants are two different blocks. So ask the item which state it would place and only accept faces that yield
     * the block the schematic asks for.
     *
     * @return true for items whose variant cannot change, so they keep using the cheaper heuristics
     */
    public static boolean placesWantedBlock(BlockPos blockPos, Direction side, BlockState placeAtState, Item item) {
        if (mc.player == null || mc.world == null) {
            return true;
        }
        if (!(item instanceof VerticallyAttachableBlockItem blockItem)) {
            return true;
        }

        // The variant is decided by the first placement direction, which is the opposite of the clicked face - except on
        // the face whose opposite is the direction the item skips, where the choice falls through to wherever the player
        // happens to be looking when the server handles the packet. That is not something the simulation below can
        // predict, and both variants are reachable from another face anyway, so leave this one alone.
        if (side == blockItem.verticalAttachmentDirection) {
            return false;
        }

        BlockPos placeAgainst = blockPos.offset(side.getOpposite());
        BlockHitResult hitResult = new BlockHitResult(faceCenter(blockPos, side), side, placeAgainst, false);
        BlockState state = blockItem.getPlacementState(new ItemPlacementContext(mc.world, mc.player, Hand.MAIN_HAND,
            new ItemStack(item), hitResult));

        if (state == null || state.getBlock() != placeAtState.getBlock()) {
            return false;
        }

        // A wall variant is held up by the block behind the face we clicked, so the face decides where its support is.
        // A block state component would happily point it at another wall afterwards, leaving it attached to nothing and
        // popping off on the next block update, so the face has to be the one the schematic hangs the block on.
        return !state.contains(Properties.HORIZONTAL_FACING)
            || !placeAtState.contains(Properties.HORIZONTAL_FACING)
            || state.get(Properties.HORIZONTAL_FACING) == placeAtState.get(Properties.HORIZONTAL_FACING);
    }

    public static boolean isBlockNormalCube(BlockState state) {
        Block block = state.getBlock();

        if (block instanceof ScaffoldingBlock
            || block instanceof ShulkerBoxBlock
            || block instanceof PointedDripstoneBlock
            || block instanceof AmethystClusterBlock) {
            return false;
        }

        try {
            return Block.isShapeFullCube(state.getCollisionShape(null, null)) || block instanceof StairsBlock;
        } catch (Exception ignored) {
            // if we can't get the collision shape, assume it's bad...
        }

        return false;
    }

    public static boolean canPlaceAgainst(BlockState placeAtState, BlockState placeAgainstState, Direction against) {
        // can we look at the center of a side face of this block and likely be able to place?
        // therefore dont include weird things that we technically could place against (like carpet) but practically can't

        return isBlockNormalCube(placeAgainstState) ||
            placeAgainstState.getBlock() == Blocks.GLASS ||
            placeAgainstState.getBlock() instanceof StainedGlassBlock ||
            placeAgainstState.getBlock() instanceof StairsBlock ||
            placeAgainstState.getBlock() instanceof SlabBlock &&
                (placeAgainstState.get(SlabBlock.TYPE) != SlabType.BOTTOM &&
                    placeAtState.getBlock() == placeAgainstState.getBlock() &&
                    against != Direction.DOWN ||
                    placeAtState.getBlock() != placeAgainstState.getBlock());
    }

    public static boolean isBlockInLineOfSight(BlockPos placeAt) {
        if (mc.player == null || mc.world == null) {
            return false;
        }

        RaycastContext context = new RaycastContext(eyePos(), placeAt.toCenterPos(),
            RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);

        return mc.world.raycast(context).getType() == HitResult.Type.MISS;
    }

    /**
     * @return whether a block will orient towards the block it is placed on
     */
    public static boolean isBlockSameAsPlaceDir(Block block) {
        return block instanceof HopperBlock;
    }

    /**
     * @return whether a block will orient opposite to the block it is placed on
     */
    public static boolean isBlockPlacementOppositeToPlacePos(Block block) {
        return block instanceof AmethystClusterBlock
            || block instanceof EndRodBlock
            || block instanceof LightningRodBlock
            || block instanceof TrapdoorBlock
            || block instanceof ChainBlock
            || block == Blocks.OAK_LOG
            || block == Blocks.SPRUCE_LOG
            || block == Blocks.BIRCH_LOG
            || block == Blocks.JUNGLE_LOG
            || block == Blocks.ACACIA_LOG
            || block == Blocks.DARK_OAK_LOG
            || block == Blocks.STRIPPED_SPRUCE_LOG
            || block == Blocks.STRIPPED_BIRCH_LOG
            || block == Blocks.STRIPPED_JUNGLE_LOG
            || block == Blocks.STRIPPED_ACACIA_LOG
            || block == Blocks.STRIPPED_DARK_OAK_LOG;
    }

    /**
     * Normal behaviour in this case is considered as when blocks are placed they take direction opposite to players direction.
     *
     * <p>Pitch between 45 (excluding) and -45 (excluding) means we are looking forward, below 45 (including) means we
     * are looking down, and below -45 (including) means we are looking up.
     *
     * <p>ObserverBlock faces same direction as player, ObserverBlock also checks pitch to place observer upwards or downwards.
     * AnvilBlock will face to direction clockwise of current look direction.
     * Buttons face same direction as player when on floor or ceiling but when on the wall it takes opposite to block it is placed on.
     * BellBlock acts same as Buttons.
     * GrindstoneBlock acts same as Buttons.
     * TrapdoorBlock normal facing when on floor or ceiling but when on the wall it takes opposite to block it is placed on.
     *
     * @return whether a block is a special case in terms of rotation
     */
    public static boolean isBlockSpecialCase(Block block) {
        return block instanceof ObserverBlock
            || block instanceof AnvilBlock
            || block instanceof GrindstoneBlock
            || block instanceof ButtonBlock;
    }

    /**
     * @return whether the block will face the same direction as the player when on floor or ceiling but takes the
     * opposite of the block it is placed on when on a wall
     */
    public static boolean isBlockLikeButton(Block block) {
        return block instanceof ButtonBlock
            || block instanceof BellBlock
            || block instanceof GrindstoneBlock
            || block instanceof TrapdoorBlock;
    }

    /**
     * Pitch between 45 (excluding) and -45 (excluding) means we are looking forward, below 45 (including) means we are
     * looking down, and below -45 (including) means we are looking up.
     *
     * @return whether the block checks pitch to orient upwards or downwards
     */
    public static boolean isBlockCheckingPitchForVerticalDir(Block block) {
        return block instanceof ObserverBlock
            || block instanceof PistonBlock;
    }

    public static boolean isFaceDesired(Block block, Direction blockHorizontalOrientation, Direction against) {
        return blockHorizontalOrientation == null
            || !(isBlockSameAsPlaceDir(block) || isBlockPlacementOppositeToPlacePos(block))
            || (isBlockSameAsPlaceDir(block) && blockHorizontalOrientation == against
            || block instanceof TrapdoorBlock && against.getOpposite() == blockHorizontalOrientation
            || !(block instanceof TrapdoorBlock) && (isBlockPlacementOppositeToPlacePos(block)
            && blockHorizontalOrientation == against.getOpposite()
            || isBlockLikeButton(block) && against != Direction.UP && against != Direction.DOWN
            && blockHorizontalOrientation == against));
    }

    public static boolean isPlayerOrientationDesired(Block block, Direction blockHorizontalOrientation,
        Direction playerOrientation) {
        return blockHorizontalOrientation == null
            || (block instanceof StairsBlock && playerOrientation == blockHorizontalOrientation
            || !(block instanceof StairsBlock)
            && !isBlockPlacementOppositeToPlacePos(block)
            && !isBlockSameAsPlaceDir(block)
            && playerOrientation == blockHorizontalOrientation.getOpposite());
    }

    /**
     * Finds a side of {@code placeAt} whose neighbour can actually be clicked from where the player is standing.
     *
     * @return the side to place against, or null when no visible neighbour produces the wanted block state
     */
    public static Direction getVisiblePlaceSide(BlockPos placeAt, BlockState placeAtState, SlabType slabType,
        BlockHalf blockHalf, Direction blockHorizontalOrientation, Direction.Axis wantedAxis, int range, Item item) {
        if (mc.player == null || mc.world == null) {
            return null;
        }

        for (Direction against : Direction.values()) {
            if (wantedAxis != null && against.getAxis() != wantedAxis
                || blockHalf != null && (against == Direction.UP && blockHalf == BlockHalf.BOTTOM
                || against == Direction.DOWN && blockHalf == BlockHalf.TOP)) {
                continue;
            }

            if ((slabType != null && slabType != SlabType.DOUBLE) && !mc.player.isCreative()) {
                if (slabType == SlabType.BOTTOM) {
                    if (against == Direction.UP) {
                        continue;
                    }
                } else {
                    if (against == Direction.DOWN) {
                        continue;
                    }
                }
            }

            if (wantedAxis == null && !isFaceDesired(placeAtState.getBlock(), blockHorizontalOrientation, against)
                || wantedAxis != null && wantedAxis != against.getAxis()) {
                continue;
            }

            BlockPos placeAgainst = placeAt.offset(against);
            BlockState placeAgainstState = mc.world.getBlockState(placeAgainst);

            if (!canPlaceAgainst(placeAtState, placeAgainstState, against)
                || BlockUtils.isClickable(placeAgainstState.getBlock())) {
                continue;
            }

            if (!placesWantedBlock(placeAt, against.getOpposite(), placeAtState, item)) {
                continue;
            }

            VoxelShape collisionShape = placeAgainstState.getCollisionShape(mc.world, placeAgainst);
            if (collisionShape.isEmpty()) {
                continue;
            }

            Box box = collisionShape.getBoundingBox();

            for (double z = 0.1; z < 0.9; z += 0.2) {
                for (double x = 0.1; x < 0.9; x += 0.2) {
                    for (Vec3d multiplier : aabbSideMultipliers(against)) {
                        if (!isMultiplierDesired(multiplier, slabType, blockHalf, against)) {
                            continue;
                        }

                        Vec3d target = new Vec3d(
                            placeAgainst.getX() + box.minX * x + box.maxX * (1 - x),
                            placeAgainst.getY() + box.minY * multiplier.y + box.maxY * (1 - multiplier.y),
                            placeAgainst.getZ() + box.minZ * z + box.maxZ * (1 - z));

                        Rotation rotation = calcRotationFromVec3d(eyePos(), target,
                            new Rotation(mc.player.getYaw(), mc.player.getPitch()));

                        Direction testHorizontalDirection = getHorizontalDirectionFromYaw(rotation.normalize().getYaw());
                        if (!isPlayerOrientationDesired(placeAtState.getBlock(), blockHorizontalOrientation,
                            testHorizontalDirection)) {
                            continue;
                        }

                        BlockHitResult result = rayTraceTowards(rotation, range);
                        if (result == null
                            || result.getType() != HitResult.Type.BLOCK
                            || !result.getBlockPos().equals(placeAgainst)
                            || result.getSide() != against.getOpposite()) {
                            continue;
                        }

                        return against.getOpposite();
                    }
                }
            }
        }

        return null;
    }

    /**
     * Same as {@link #getVisiblePlaceSide} but without the line of sight check, so it also works through walls.
     */
    public static Direction getPlaceSide(BlockPos blockPos, BlockState placeAtState, SlabType slabType,
        BlockHalf blockHalf, Direction blockHorizontalOrientation, Direction.Axis wantedAxis, Item item) {
        if (mc.player == null || mc.world == null) {
            return null;
        }

        for (Direction side : Direction.values()) {
            BlockPos neighbour = blockPos.offset(side);
            Direction placeSide = side.getOpposite();

            if (wantedAxis != null && side.getAxis() != wantedAxis
                || blockHalf != null && (side == Direction.UP && blockHalf == BlockHalf.BOTTOM
                || side == Direction.DOWN && blockHalf == BlockHalf.TOP)) {
                continue;
            }

            if ((slabType != null && slabType != SlabType.DOUBLE || blockHalf != null) && !mc.player.isCreative()) {
                if (slabType == SlabType.BOTTOM || blockHalf == BlockHalf.BOTTOM) {
                    if (placeSide == Direction.DOWN) {
                        continue;
                    }
                } else {
                    if (placeSide == Direction.UP) {
                        continue;
                    }
                }
            }

            if (wantedAxis == null && !isFaceDesired(placeAtState.getBlock(), blockHorizontalOrientation, side)
                || wantedAxis != null && wantedAxis != side.getAxis()) {
                continue;
            }

            BlockState state = mc.world.getBlockState(neighbour);

            // Check if neighbour isn't empty
            if (state.isAir()
                || BlockUtils.isClickable(state.getBlock())
                || state.contains(Properties.SLAB_TYPE)
                && (state.get(Properties.SLAB_TYPE) == SlabType.DOUBLE
                || side == Direction.UP && state.get(Properties.SLAB_TYPE) == SlabType.TOP
                || side == Direction.DOWN && state.get(Properties.SLAB_TYPE) == SlabType.BOTTOM)) {
                continue;
            }

            // Check if neighbour is a fluid
            if (!state.getFluidState().isEmpty()) {
                continue;
            }

            if (!placesWantedBlock(blockPos, placeSide, placeAtState, item)) {
                continue;
            }

            Rotation rotation = calcRotationFromVec3d(eyePos(), neighbour.toCenterPos(),
                new Rotation(mc.player.getYaw(), mc.player.getPitch()));

            Direction testHorizontalDirection = getHorizontalDirectionFromYaw(rotation.normalize().getYaw());
            if (!isPlayerOrientationDesired(placeAtState.getBlock(), blockHorizontalOrientation,
                testHorizontalDirection)) {
                continue;
            }

            return placeSide;
        }

        return null;
    }

    /**
     * Only the y component is used, the x and z of the returned vectors are placeholders. The interpolation done by
     * the callers is inverted, so a multiplier of 0 lands on the top of the box and 1 on the bottom - which is why
     * callers pass the opposite of the face they want to hit.
     */
    private static Vec3d[] aabbSideMultipliers(Direction side) {
        return switch (side) {
            case UP -> new Vec3d[] { new Vec3d(0.5, 1, 0.5), new Vec3d(0.1, 1, 0.5), new Vec3d(0.9, 1, 0.5),
                new Vec3d(0.5, 1, 0.1), new Vec3d(0.5, 1, 0.9) };
            case DOWN -> new Vec3d[] { new Vec3d(0.5, 0, 0.5), new Vec3d(0.1, 0, 0.5), new Vec3d(0.9, 0, 0.5),
                new Vec3d(0.5, 0, 0.1), new Vec3d(0.5, 0, 0.9) };
            case NORTH, SOUTH, EAST, WEST -> {
                double x = side.getOffsetX() == 0 ? 0.5 : (1 + side.getOffsetX()) / 2D;
                double z = side.getOffsetZ() == 0 ? 0.5 : (1 + side.getOffsetZ()) / 2D;
                yield new Vec3d[] { new Vec3d(x, 0.25, z), new Vec3d(x, 0.75, z) };
            }
        };
    }

    private static boolean isMultiplierDesired(Vec3d multiplier, SlabType slabType, BlockHalf blockHalf,
        Direction against) {
        Boolean wantsBottom = wantsBottomHalf(slabType, blockHalf);

        if (wantsBottom == null) {
            return true;
        }

        // Vanilla resolves the half from the clicked face before it looks at the hit position: clicking the top of the
        // block below always gives the bottom half and clicking the bottom of the block above always gives the top
        // half.
        if (against == Direction.DOWN) {
            return wantsBottom;
        }
        if (against == Direction.UP) {
            return !wantsBottom;
        }

        // Side faces do use the hit position, and the interpolation is inverted, so a high multiplier lands low.
        return wantsBottom == (multiplier.y > 0.5);
    }

    /**
     * @return whether the schematic wants the lower half of a slab or stair like block, or null when the wanted state
     * has no half to hit
     */
    private static Boolean wantsBottomHalf(SlabType slabType, BlockHalf blockHalf) {
        if (slabType != null && slabType != SlabType.DOUBLE) {
            return slabType == SlabType.BOTTOM;
        }
        if (blockHalf != null) {
            return blockHalf == BlockHalf.BOTTOM;
        }

        return null;
    }

    public static Direction getHorizontalDirectionFromYaw(float yaw) {
        yaw %= 360.0F;
        if (yaw < 0) {
            yaw += 360.0F;
        }

        if (yaw >= 45 && yaw < 135) {
            return Direction.WEST;
        } else if (yaw >= 135 && yaw < 225) {
            return Direction.NORTH;
        } else if (yaw >= 225 && yaw < 315) {
            return Direction.EAST;
        } else {
            return Direction.SOUTH;
        }
    }

    public static Direction getVerticalDirectionFromPitch(float pitch) {
        return pitch >= 0.0F ? Direction.UP : Direction.DOWN;
    }

    private static Vec3d eyePos() {
        return new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
    }

    public static Rotation calcRotationFromVec3d(Vec3d orig, Vec3d dest, Rotation current) {
        return wrapAnglesToRelative(current, calcRotationFromVec3d(orig, dest));
    }

    private static Rotation calcRotationFromVec3d(Vec3d orig, Vec3d dest) {
        double dx = orig.x - dest.x;
        double dy = orig.y - dest.y;
        double dz = orig.z - dest.z;

        double yaw = MathHelper.atan2(dx, -dz);
        double dist = Math.sqrt(dx * dx + dz * dz);
        double pitch = MathHelper.atan2(dy, dist);

        return new Rotation((float) (yaw * RAD_TO_DEG), (float) (pitch * RAD_TO_DEG));
    }

    public static Rotation wrapAnglesToRelative(Rotation current, Rotation target) {
        if (current.yawIsReallyClose(target)) {
            return new Rotation(current.getYaw(), target.getPitch());
        }

        return target.subtract(current).normalize().add(current);
    }

    public static Vec3d calcLookDirectionFromRotation(Rotation rotation) {
        float flatZ = MathHelper.cos((-rotation.getYaw() * DEG_TO_RAD_F) - (float) Math.PI);
        float flatX = MathHelper.sin((-rotation.getYaw() * DEG_TO_RAD_F) - (float) Math.PI);
        float pitchBase = -MathHelper.cos(-rotation.getPitch() * DEG_TO_RAD_F);
        float pitchHeight = MathHelper.sin(-rotation.getPitch() * DEG_TO_RAD_F);

        return new Vec3d(flatX * pitchBase, pitchHeight, flatZ * pitchBase);
    }

    public static BlockHitResult rayTraceTowards(Rotation rotation, double blockReachDistance) {
        if (mc.player == null || mc.world == null) {
            return null;
        }

        Vec3d start = mc.player.getCameraPosVec(1.0F);
        Vec3d direction = calcLookDirectionFromRotation(rotation);
        Vec3d end = start.add(
            direction.x * blockReachDistance,
            direction.y * blockReachDistance,
            direction.z * blockReachDistance);

        return mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE, mc.player));
    }

    public static final class Rotation {

        private final float yaw;
        private final float pitch;

        public Rotation(float yaw, float pitch) {
            if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
                throw new IllegalStateException(yaw + " " + pitch);
            }

            this.yaw = yaw;
            this.pitch = pitch;
        }

        public float getYaw() {
            return yaw;
        }

        public float getPitch() {
            return pitch;
        }

        public Rotation add(Rotation other) {
            return new Rotation(yaw + other.yaw, pitch + other.pitch);
        }

        public Rotation subtract(Rotation other) {
            return new Rotation(yaw - other.yaw, pitch - other.pitch);
        }

        public Rotation clamp() {
            return new Rotation(yaw, clampPitch(pitch));
        }

        public Rotation normalize() {
            return new Rotation(normalizeYaw(yaw), pitch);
        }

        public Rotation normalizeAndClamp() {
            return new Rotation(normalizeYaw(yaw), clampPitch(pitch));
        }

        public Rotation withPitch(float pitch) {
            return new Rotation(yaw, pitch);
        }

        public boolean isReallyCloseTo(Rotation other) {
            return yawIsReallyClose(other) && Math.abs(pitch - other.pitch) < 0.01;
        }

        public boolean yawIsReallyClose(Rotation other) {
            float yawDiff = Math.abs(normalizeYaw(yaw) - normalizeYaw(other.yaw));
            return yawDiff < 0.01 || yawDiff > 359.99;
        }

        public static float clampPitch(float pitch) {
            return Math.max(-90, Math.min(90, pitch));
        }

        public static float normalizeYaw(float yaw) {
            float newYaw = yaw % 360F;

            if (newYaw < -180F) {
                newYaw += 360F;
            }
            if (newYaw > 180F) {
                newYaw -= 360F;
            }

            return newYaw;
        }

        @Override
        public String toString() {
            return "Yaw: " + yaw + ", Pitch: " + pitch;
        }
    }
    
}
