/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world.litematica;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.RenderUtils;
import meteordevelopment.meteorclient.renderer.color.Color;
import meteordevelopment.meteorclient.renderer.color.SettingColor;
import meteordevelopment.meteorclient.renderer.engine.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.name.IDisplayName;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InventoryUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.*;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.*;
import java.util.function.BooleanSupplier;

public class LitematicaPrinter extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWorkMode = settings.createGroup("Work Mode");
    private final SettingGroup sgAntiStuck = settings.createGroup("Anti Stuck");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("printing-range")
        .description("The block place range.")
        .defaultValue(2)
        .min(1).sliderMin(1)
        .max(60).sliderMax(6)
        .build());

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("printing-delay")
        .description("Delay between printing blocks in ticks.")
        .defaultValue(2)
        .min(0).sliderMin(0)
        .max(100).sliderMax(40)
        .build());

    private final Setting<Integer> bpt = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("How many blocks to place per tick.")
        .defaultValue(1)
        .min(1).sliderMin(1)
        .max(100).sliderMax(100)
        .build());

    private final Setting<Boolean> advanced = sgGeneral.add(new BoolSetting.Builder()
        .name("advanced")
        .description("Respect block rotation (places blocks in weird places in singleplayer, multiplayer should work fine).")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> creativeBlockStates = sgGeneral.add(new BoolSetting.Builder()
        .name("creative-block-states")
        .description("Creative only. Gives yourself the block with a block state component so it is placed in the exact state the schematic wants, without having to aim at a specific face.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> headTextures = sgGeneral.add(new BoolSetting.Builder()
        .name("head-textures")
        .description("Creative only. Gives yourself player heads with the profile component of the head in the schematic, so they keep their skin.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("air-place")
        .description("Allow placing blocks without a neighbour to place against.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> placeThroughWall = sgGeneral.add(new BoolSetting.Builder()
        .name("place-through-wall")
        .description("Allow placing blocks you cannot see.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swing hand when placing.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> returnHand = sgGeneral.add(new BoolSetting.Builder()
        .name("return-slot")
        .description("Return to your old slot after placing.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotate to the blocks being placed.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> clientSide = sgGeneral.add(new BoolSetting.Builder()
        .name("client-side-rotation")
        .description("Only rotate on the client side.")
        .defaultValue(false)
        .visible(rotate::get)
        .build());

    private final Setting<Boolean> dirtAsGrass = sgGeneral.add(new BoolSetting.Builder()
        .name("dirt-as-grass")
        .description("Use dirt instead of grass blocks.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> useOffhand = sgGeneral.add(new BoolSetting.Builder()
        .name("use-offhand")
        .description("Automatically put block items in the offhand while printing.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> supportFallingBlocks = sgGeneral.add(new BoolSetting.Builder()
        .name("support-falling-blocks")
        .description("Wait for the block below to be placed before placing sand, gravel and other falling blocks.")
        .defaultValue(true)
        .build());

    private final Setting<SortAlgorithm> firstAlgorithm = sgGeneral.add(new EnumChoiceSetting.Builder<SortAlgorithm>()
        .name("first-sorting-mode")
        .description("The blocks you want to place first.")
        .defaultValue(SortAlgorithm.NONE)
        .build());

    private final Setting<SortingSecond> secondAlgorithm = sgGeneral.add(new EnumChoiceSetting.Builder<SortingSecond>()
        .name("second-sorting-mode")
        .description("Second pass of sorting, eg. place blocks higher up and closest to you first.")
        .defaultValue(SortingSecond.NONE)
        .visible(() -> firstAlgorithm.get().applySecondSorting)
        .build());

    private final Setting<ListMode> listMode = sgWorkMode.add(new EnumChoiceSetting.Builder<ListMode>()
        .name("list-mode")
        .description("Block list mode.")
        .defaultValue(ListMode.NONE)
        .build());

    private final Setting<List<Block>> filterBlocks = sgWorkMode.add(new BlockListSetting.Builder()
        .name("filter-blocks")
        .description("Blocks to whitelist or blacklist.")
        .visible(() -> listMode.get() != ListMode.NONE)
        .build());

    private final Setting<Boolean> antiStuck = sgAntiStuck.add(new BoolSetting.Builder()
        .name("anti-stuck")
        .description("Escalate blocks that keep failing instead of retrying them the same way forever.")
        .defaultValue(true)
        .build());

    private final Setting<Integer> ignoreStateAttempts = sgAntiStuck.add(new IntSetting.Builder()
        .name("attempts-before-ignoring-state")
        .description("Attempts on the same block before it is placed without matching the wanted block state.")
        .defaultValue(3)
        .min(1).sliderMin(1)
        .max(50).sliderMax(10)
        .visible(antiStuck::get)
        .build());

    private final Setting<Integer> delayAttempts = sgAntiStuck.add(new IntSetting.Builder()
        .name("attempts-before-delay")
        .description("Attempts on the same block before it gets a cooldown. Never lower than attempts-before-ignoring-state, so the stateless attempts come first.")
        .defaultValue(6)
        .min(1).sliderMin(1)
        .max(100).sliderMax(20)
        .visible(antiStuck::get)
        .build());

    private final Setting<Integer> stuckDelay = sgAntiStuck.add(new IntSetting.Builder()
        .name("stuck-delay")
        .description("Cooldown in ticks given to a stuck block, multiplied by every further attempt.")
        .defaultValue(5)
        .min(1).sliderMin(1)
        .max(200).sliderMax(40)
        .visible(antiStuck::get)
        .build());

    private final Setting<Integer> maxStuckDelay = sgAntiStuck.add(new IntSetting.Builder()
        .name("max-stuck-delay")
        .description("Upper bound for the cooldown in ticks.")
        .defaultValue(100)
        .min(1).sliderMin(1)
        .max(6000).sliderMax(600)
        .visible(antiStuck::get)
        .build());

    private final Setting<Boolean> cycleSlot = sgAntiStuck.add(new BoolSetting.Builder()
        .name("cycle-hotbar-slot")
        .description("Move to the next hotbar slot when a block keeps failing, to resync an inventory the server disagrees with.")
        .defaultValue(true)
        .visible(antiStuck::get)
        .build());

    private final Setting<Boolean> renderPlaced = sgRender.add(new BoolSetting.Builder()
        .name("render-placed-blocks")
        .description("Renders block placements.")
        .defaultValue(true)
        .build());

    private final Setting<Integer> fadeTime = sgRender.add(new IntSetting.Builder()
        .name("fade-time")
        .description("Time for the rendering to fade, in ticks.")
        .defaultValue(3)
        .min(1).sliderMin(1)
        .max(1000).sliderMax(20)
        .visible(renderPlaced::get)
        .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumChoiceSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.SIDES)
        .visible(renderPlaced::get)
        .build());

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("color")
        .description("The cubes color.")
        .defaultValue(new SettingColor(95, 190, 255, 100))
        .visible(renderPlaced::get)
        .build());

    /**
     * An attempt is dropped after this many ticks without being touched. Nothing touches the entry of a block that
     * finally got placed, so this both frees the map and resets the history of a position that stopped being a problem.
     */
    private static final int FORGET_TICKS = 100;

    /**
     * Every stuck block asking for a slot cycle at once would be a burst of held item packets, so they share one.
     */
    private static final int SLOT_CYCLE_INTERVAL = 10;

    private final List<BlockPos> toSort = new ArrayList<>();
    private final Map<BlockPos, Attempt> attempts = new HashMap<>();
    private int timer;
    private int ticks;
    private int nextCycleTick;
    private int usedSlot = -1;

    public LitematicaPrinter() {
        super(Category.WORLD, "LitematicaPrinter", "Automatically prints open Litematica schematics.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        ticks = 0;
        nextCycleTick = 0;
        usedSlot = -1;
        toSort.clear();
        attempts.clear();
    }

    @Override
    public void onDeactivate() {
        toSort.clear();
        attempts.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
        if (schematic == null) {
            error("No schematic is loaded, disabling.");
            toggle();
            return;
        }

        ticks++;

        if (ticks % FORGET_TICKS == 0) {
            forgetSettledBlocks();
        }

        if (timer > 0) {
            timer--;
            return;
        }

        toSort.clear();

        int r = range.get();

        BlockIterator.register(r + 1, r + 1, (pos, blockState) -> {
            BlockState required = schematic.getBlockState(pos);
            
            if (!mc.player.getBlockPos().isWithinDistance(pos, r)
                || !blockState.isReplaceable()
                || !required.getFluidState().isEmpty()
                || required.isAir()
                || blockState.getBlock() == required.getBlock()
                || !DataManager.getRenderLayerRange().isPositionWithinRange(pos)
                || mc.player.getBoundingBox().intersects(Vec3d.of(pos), Vec3d.of(pos).add(1, 1, 1))
                || !required.canPlaceAt(mc.world, pos)) {
                return;
            }

            if (isOnCooldown(pos)) {
                return;
            }

            if (!isAllowed(required.getBlock())) {
                return;
            }

            if (isMissingFallingSupport(pos, required)) {
                return;
            }

            // Having nothing to click for the state we want counts as an attempt of its own, otherwise a block that can
            // only ever be reached in the wrong state would never escalate to a stateless placement.
            if (!isReachable(pos, required, shouldIgnoreState(pos))) {
                trackAttempt(pos);
                return;
            }

            toSort.add(pos.toImmutable());
        });

        BlockIterator.after(() -> {
            if (firstAlgorithm.get() != SortAlgorithm.NONE) {
                if (firstAlgorithm.get().applySecondSorting && secondAlgorithm.get() != SortingSecond.NONE) {
                    toSort.sort(secondAlgorithm.get().algorithm);
                }

                toSort.sort(firstAlgorithm.get().algorithm);
            }

            int placed = 0;

            for (BlockPos pos : toSort) {
                BlockState state = schematic.getBlockState(pos);
                Item item = requiredItem(state);
                boolean ignoreState = shouldIgnoreState(pos);
                ItemStack forced = forcedStack(schematic, state, pos, item, ignoreState);

                boolean success;

                if (forced != null) {
                    success = forceStack(forced, () -> place(state, pos, ignoreState));
                } else {
                    success = useOffhand.get()
                        ? switchItemOffhand(item, () -> place(state, pos, ignoreState))
                        : switchItem(item, () -> place(state, pos, ignoreState));
                }

                // Counted either way: an accepted interaction is no proof the server placed anything, and the entry is
                // forgotten on its own once the block stops showing up here.
                trackAttempt(pos);

                if (!success) {
                    continue;
                }

                timer = delay.get();

                if (renderPlaced.get()) {
                    RenderUtils.renderTickingBlock(pos, new Color(color.get()), new Color(color.get()),
                        shapeMode.get(), 0, fadeTime.get(), true, false);
                }

                if (++placed >= bpt.get()) {
                    return;
                }
            }
        });
    }

    /**
     * A block that keeps failing is escalated instead of being retried the same way forever: first the wanted block
     * state is dropped, so any state of the right block ends the retries, then the position is put on a cooldown that
     * grows with every further attempt so the rest of the schematic keeps printing.
     */
    private void trackAttempt(BlockPos pos) {
        if (!antiStuck.get()) {
            return;
        }

        Attempt attempt = attempts.get(pos);

        if (attempt == null) {
            attempt = new Attempt();
            attempts.put(pos.toImmutable(), attempt);
        }

        attempt.count++;
        attempt.lastTick = ticks;

        // Dropping the block state comes first, so the cooldown only starts once those attempts failed as well.
        int threshold = Math.max(delayAttempts.get(), ignoreStateAttempts.get());
        if (attempt.count < threshold) {
            return;
        }

        cycleHotbarSlot();

        long cooldown = (long) stuckDelay.get() * (attempt.count - threshold + 1);
        attempt.nextTick = ticks + (int) Math.min(cooldown, maxStuckDelay.get());
    }

    /**
     * A placement the server drops without a word is often just it disagreeing about what we hold, and it only learns
     * otherwise once the selected slot is sent again. Walking one slot forward every time also cycles through the whole
     * hotbar over repeated stuck blocks, and it drops {@link #usedSlot} so the item is looked up again instead of
     * trusting a slot the server may no longer agree with.
     */
    private void cycleHotbarSlot() {
        if (!cycleSlot.get() || mc.player == null || ticks < nextCycleTick) {
            return;
        }

        nextCycleTick = ticks + SLOT_CYCLE_INTERVAL;
        usedSlot = -1;

        InventoryUtils.swap((mc.player.getInventory().getSelectedSlot() + 1) % 9, returnHand.get());
    }

    private boolean shouldIgnoreState(BlockPos pos) {
        if (!antiStuck.get()) {
            return false;
        }

        Attempt attempt = attempts.get(pos);

        return attempt != null && attempt.count >= ignoreStateAttempts.get();
    }

    private boolean isOnCooldown(BlockPos pos) {
        Attempt attempt = attempts.get(pos);

        return attempt != null && attempt.nextTick > ticks;
    }

    private void forgetSettledBlocks() {
        attempts.values().removeIf(attempt -> ticks - attempt.lastTick > FORGET_TICKS && attempt.nextTick <= ticks);
    }

    private boolean isAllowed(Block block) {
        return switch (listMode.get()) {
            case NONE -> true;
            case WHITELIST -> filterBlocks.get().contains(block);
            case BLACKLIST -> !filterBlocks.get().contains(block);
        };
    }

    private Item requiredItem(BlockState required) {
        Item item = required.getBlock().asItem();

        return dirtAsGrass.get() && item == Items.GRASS_BLOCK ? Items.DIRT : item;
    }

    /**
     * Sand, gravel and the like turn into a falling entity the moment they are placed with nothing underneath, and the
     * entity lands wherever it wants. So hold off until the block the schematic wants below is there - the printer
     * queues that block on its own, which resolves whole columns from the bottom up over the next ticks.
     */
    private boolean isMissingFallingSupport(BlockPos pos, BlockState required) {
        if (!supportFallingBlocks.get() || !(required.getBlock() instanceof FallingBlock) || mc.world == null) {
            return false;
        }

        return FallingBlock.canFallThrough(mc.world.getBlockState(pos.down()));
    }

    /**
     * Cheap pre-check so we don't queue up blocks we have no way of clicking.
     */
    private boolean isReachable(BlockPos pos, BlockState required, boolean ignoreState) {
        if (airPlace.get() && placeThroughWall.get()) {
            return true;
        }

        if (!airPlace.get() && placeThroughWall.get()) {
            return BlockUtils.getPlaceSide(pos) != null;
        }

        if (!PrinterUtils.isBlockInLineOfSight(pos)) {
            return false;
        }

        if (airPlace.get()) {
            return true;
        }

        return PrinterUtils.getVisiblePlaceSide(pos, required, wantedSlabType(required, ignoreState),
            wantedBlockHalf(required, ignoreState), wantedFacing(required, ignoreState),
            wantedAxis(required, ignoreState), range.get(), requiredItem(required)) != null;
    }

    /**
     * @param ignoreState place any state of the wanted block rather than the exact one, used to get a position that
     * keeps failing out of the way
     */
    private boolean place(BlockState required, BlockPos pos, boolean ignoreState) {
        if (mc.player == null || mc.world == null) {
            return false;
        }
        if (!mc.world.getBlockState(pos).isReplaceable()) {
            return false;
        }

        SlabType slabType = wantedSlabType(required, ignoreState);
        BlockHalf blockHalf = wantedBlockHalf(required, ignoreState);
        Direction facing = wantedFacing(required, ignoreState);
        Direction.Axis axis = wantedAxis(required, ignoreState);
        Item item = requiredItem(required);

        Direction placeSide = placeThroughWall.get()
            ? PrinterUtils.getPlaceSide(pos, required, slabType, blockHalf, facing, axis, item)
            : PrinterUtils.getVisiblePlaceSide(pos, required, slabType, blockHalf, facing, axis, range.get(), item);

        // Without a side to place against the only way left is an air place, which the user may have turned off. Signs,
        // torches and the like get their variant from the clicked face, so for those an air place is never an option
        // either - wait until a neighbour shows up instead of placing the wrong block.
        if (placeSide == null && (!airPlace.get() || item instanceof VerticallyAttachableBlockItem)) {
            return false;
        }

        return PrinterUtils.place(pos, placeSide, slabType, blockHalf, facing, axis, airPlace.get(), swing.get(),
            rotate.get(), clientSide.get(), range.get(), useOffhand.get() ? Hand.OFF_HAND : Hand.MAIN_HAND);
    }

    /**
     * In creative we can hand ourselves the block item with a block state component already attached, which makes the
     * server place the exact state instead of deriving it from where we clicked.
     */
    private boolean useBlockStateComponent(BlockState required) {
        return creativeBlockStates.get()
            && mc.player != null
            && mc.player.isCreative()
            && !required.getProperties().isEmpty();
    }

    /**
     * @return whether the wanted state still has to be produced by aiming the click, rather than being baked into the
     * item we are holding
     */
    private boolean needsAimedState(BlockState required, boolean ignoreState) {
        return !ignoreState && advanced.get() && !useBlockStateComponent(required);
    }

    private BlockStateComponent blockStateComponent(BlockState required) {
        BlockStateComponent component = BlockStateComponent.DEFAULT;

        // Every property is copied, not just the ones that differ from the default: the state we would get from a
        // plain placement depends on where we are standing, so a property matching the default still has to be pinned.
        for (Property<?> property : required.getProperties()) {
            component = component.with(property, required);
        }

        return component;
    }

    /**
     * Puts the exact stack we need into the hand we place from. The stack is resent whenever the held one differs,
     * otherwise the previous block's state would leak into this placement.
     */
    private boolean forceStack(ItemStack stack, BooleanSupplier action) {
        if (mc.player == null) {
            return false;
        }

        if (useOffhand.get()) {
            if (!isHolding(mc.player.getOffHandStack(), stack)) {
                InventoryUtils.clickCreativeStack(stack, SlotUtils.OFFHAND, true);
            }

            return action.getAsBoolean();
        }

        int selectedSlot = mc.player.getInventory().getSelectedSlot();

        if (!isHolding(mc.player.getMainHandStack(), stack)) {
            InventoryUtils.clickCreativeStack(stack, selectedSlot, false);
        }

        usedSlot = selectedSlot;

        return action.getAsBoolean();
    }

    /**
     * Everything the schematic asks for that an item out of the creative inventory does not carry on its own.
     *
     * @return the stack to hand ourselves, or null when any stack of the right item will do
     */
    private ItemStack forcedStack(WorldSchematic schematic, BlockState required, BlockPos pos, Item item,
        boolean ignoreState) {
        boolean wantsState = useBlockStateComponent(required);
        ProfileComponent profile = wantedProfile(schematic, pos, item);

        if (!wantsState && profile == null) {
            return null;
        }

        ItemStack stack = new ItemStack(item);

        if (wantsState) {
            // The default component clears the state of the previously held stack, which is what makes an escalated
            // attempt a stateless placement rather than one inheriting the last block's state.
            stack.set(DataComponentTypes.BLOCK_STATE,
                ignoreState ? BlockStateComponent.DEFAULT : blockStateComponent(required));
        }

        if (profile != null) {
            stack.set(DataComponentTypes.PROFILE, profile);
        }

        return stack;
    }

    /**
     * A head keeps its skin in a component on the item rather than in its block state, so the only way to print a
     * textured one is to read the profile off the block entity in the schematic and place a head already wearing it.
     *
     * <p>Looking a block entity up creates one when the schematic has none, so this only runs for the single item that
     * can make use of it.
     */
    private ProfileComponent wantedProfile(WorldSchematic schematic, BlockPos pos, Item item) {
        if (!headTextures.get() || !(item instanceof PlayerHeadItem) || mc.player == null || !mc.player.isCreative()) {
            return null;
        }

        return schematic.getBlockEntity(pos) instanceof SkullBlockEntity skull ? skull.getOwner() : null;
    }

    /**
     * Only the components we set ourselves are compared: the stack we get back carries a pile of defaults we never
     * asked for, and comparing those would resend it before every single placement.
     */
    private boolean isHolding(ItemStack held, ItemStack wanted) {
        return held.getItem() == wanted.getItem()
            && held.getOrDefault(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT)
                .equals(wanted.getOrDefault(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT))
            && Objects.equals(held.get(DataComponentTypes.PROFILE), wanted.get(DataComponentTypes.PROFILE));
    }

    private SlabType wantedSlabType(BlockState required, boolean ignoreState) {
        return needsAimedState(required, ignoreState) && required.contains(Properties.SLAB_TYPE) ? required.get(Properties.SLAB_TYPE) : null;
    }

    private BlockHalf wantedBlockHalf(BlockState required, boolean ignoreState) {
        return needsAimedState(required, ignoreState) && required.contains(Properties.BLOCK_HALF) ? required.get(Properties.BLOCK_HALF) : null;
    }

    private Direction.Axis wantedAxis(BlockState required, boolean ignoreState) {
        return needsAimedState(required, ignoreState) && required.contains(Properties.AXIS) ? required.get(Properties.AXIS) : null;
    }

    private Direction wantedFacing(BlockState required, boolean ignoreState) {
        if (!needsAimedState(required, ignoreState)) {
            return null;
        }
        // A wall sign, banner or head gets its facing from the face it is clicked onto, and which face that is has
        // already been settled by PrinterUtils#placesWantedBlock. Handing the facing to the rotation heuristics on top
        // of that only makes them ask the player to stand somewhere they do not have to stand.
        if (requiredItem(required) instanceof VerticallyAttachableBlockItem) {
            return null;
        }
        if (required.contains(Properties.HORIZONTAL_FACING)) {
            return required.get(Properties.HORIZONTAL_FACING);
        }
        if (required.contains(Properties.HOPPER_FACING)) {
            return required.get(Properties.HOPPER_FACING);
        }

        return null;
    }

    @SuppressWarnings("unused")
    private Direction wantedDirection(BlockState state) {
        if (state.contains(Properties.FACING)) {
            return state.get(Properties.FACING);
        } else if (state.contains(Properties.AXIS)) {
            return Direction.from(state.get(Properties.AXIS), Direction.AxisDirection.POSITIVE);
        } else if (state.contains(Properties.HORIZONTAL_AXIS)) {
            return Direction.from(state.get(Properties.HORIZONTAL_AXIS), Direction.AxisDirection.POSITIVE);
        } else {
            return Direction.UP;
        }
    }

    private boolean switchItem(Item item, BooleanSupplier action) {
        if (mc.player == null) {
            return false;
        }

        int selectedSlot = mc.player.getInventory().getSelectedSlot();

        if (mc.player.getMainHandStack().getItem() == item) {
            if (!action.getAsBoolean()) {
                return false;
            }

            usedSlot = selectedSlot;
            return true;
        }

        if (usedSlot != -1 && mc.player.getInventory().getStack(usedSlot).getItem() == item) {
            return swapAndPlace(usedSlot, selectedSlot, action);
        }

        FindItemResult hotbar = InventoryUtils.find(stack -> stack.getItem() == item,
            SlotUtils.HOTBAR_START, SlotUtils.HOTBAR_END);

        if (hotbar.found()) {
            return swapAndPlace(hotbar.slot(), selectedSlot, action);
        }

        if (mc.player.isCreative()) {
            // Put the item straight into the selected hotbar slot, clickCreativeStack keeps the client in sync.
            InventoryUtils.clickCreativeStack(new ItemStack(item), selectedSlot, false);
            usedSlot = selectedSlot;

            return action.getAsBoolean();
        }

        FindItemResult result = InventoryUtils.find(item);
        if (!result.found() || !result.isMain()) {
            return false;
        }

        FindItemResult empty = InventoryUtils.findEmpty();
        int target = empty.found() && empty.isHotbar() ? empty.slot() : usedSlot;
        if (target == -1) {
            return false;
        }

        InventoryUtils.move().from(result.slot()).toHotbar(target);

        return swapAndPlace(target, selectedSlot, action);
    }

    private boolean swapAndPlace(int slot, int selectedSlot, BooleanSupplier action) {
        InventoryUtils.swap(slot, returnHand.get());

        if (action.getAsBoolean()) {
            usedSlot = slot;
            return true;
        }

        InventoryUtils.swap(selectedSlot, returnHand.get());
        return false;
    }

    private boolean switchItemOffhand(Item item, BooleanSupplier action) {
        if (mc.player == null) {
            return false;
        }

        if (mc.player.getOffHandStack().getItem() == item) {
            return action.getAsBoolean();
        }

        if (mc.player.isCreative()) {
            InventoryUtils.clickCreativeStack(new ItemStack(item), SlotUtils.OFFHAND, true);
            return action.getAsBoolean();
        }

        FindItemResult result = InventoryUtils.find(item);
        if (!result.found() || result.isOffhand()) {
            return false;
        }

        InventoryUtils.move().from(result.slot()).toOffhand();

        // The swap only lands next tick, so let the next iteration do the placing.
        return false;
    }

    /**
     * Placement history of a single position. {@code count} is every attempt made since the position last settled,
     * {@code nextTick} the tick it may be retried on and {@code lastTick} the tick it was last attempted on.
     */
    private static final class Attempt {

        private int count;
        private int nextTick;
        private int lastTick;
    }

    public enum SortAlgorithm implements IDisplayName {

        NONE("None", false, (a, b) -> 0),
        TOP_DOWN("Top Down", true, Comparator.comparingInt(pos -> -pos.getY())),
        DOWN_TOP("Down Top", true, Comparator.comparingInt(Vec3i::getY)),
        NEAREST("Nearest", false, Comparator.comparingDouble(LitematicaPrinter::distanceToPlayer)),
        FURTHEST("Furthest", false, Comparator.comparingDouble(pos -> -distanceToPlayer(pos)));

        private final String displayName;
        final boolean applySecondSorting;
        final Comparator<BlockPos> algorithm;

        SortAlgorithm(String displayName, boolean applySecondSorting, Comparator<BlockPos> algorithm) {
            this.displayName = displayName;
            this.applySecondSorting = applySecondSorting;
            this.algorithm = algorithm;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }
    }

    public enum SortingSecond implements IDisplayName {

        NONE("None", SortAlgorithm.NONE.algorithm),
        NEAREST("Nearest", SortAlgorithm.NEAREST.algorithm),
        FURTHEST("Furthest", SortAlgorithm.FURTHEST.algorithm);

        private final String displayName;
        final Comparator<BlockPos> algorithm;

        SortingSecond(String displayName, Comparator<BlockPos> algorithm) {
            this.displayName = displayName;
            this.algorithm = algorithm;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }
    }

    public enum ListMode implements IDisplayName {

        NONE("None"),
        WHITELIST("Whitelist"),
        BLACKLIST("Blacklist");

        private final String displayName;

        ListMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }
    }

    private static double distanceToPlayer(BlockPos pos) {
        if (mc.player == null) {
            return 0;
        }

        return Utils.squaredDistance(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }
    
}
