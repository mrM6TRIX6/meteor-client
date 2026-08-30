/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world.litematica;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.RenderUtils;
import meteordevelopment.meteorclient.renderer.engine.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.name.IDisplayName;
import meteordevelopment.meteorclient.utils.player.InventoryUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.state.property.Property;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class LitematicaShredder extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWorkMode = settings.createGroup("Work Mode");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("Range")
        .description("How far away blocks can be broken.")
        .defaultValue(5.0)
        .min(1).sliderMin(1)
        .max(20).sliderMax(6)
        .build());

    private final Setting<Double> wallsRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("WallsRange")
        .description("How far away blocks you cannot see can be broken.")
        .defaultValue(6.0)
        .min(0).sliderMin(0)
        .max(6)
        .build());

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("Delay")
        .description("Delay between breaks in ticks.")
        .defaultValue(0)
        .min(0).sliderMin(0)
        .max(100).sliderMax(20)
        .build());

    private final Setting<Integer> bpt = sgGeneral.add(new IntSetting.Builder()
        .name("BlocksPerTick")
        .description("Maximum amount of blocks to break per tick.")
        .defaultValue(100)
        .min(1).sliderMin(1)
        .max(100)
        .build());

    private final Setting<Mode> mode = sgGeneral.add(new EnumChoiceSetting.Builder<Mode>()
        .name("Mode")
        .description("Which blocks to break.")
        .defaultValue(Mode.ALL)
        .build());

    private final Setting<Boolean> headTextures = sgGeneral.add(new BoolSetting.Builder()
        .name("HeadTextures")
        .description("Also break player heads whose skin doesn't match the head in the schematic.")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.WRONG_STATE || mode.get() == Mode.ALL)
        .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("Rotate")
        .description("Rotate towards the block being broken.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("Swing")
        .description("Swing hand when breaking.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> silkTouch = sgGeneral.add(new BoolSetting.Builder()
        .name("SilkTouch")
        .description("Swap to a Silk Touch tool before breaking.")
        .defaultValue(false)
        .build());

    private final Setting<ListMode> listMode = sgWorkMode.add(new EnumChoiceSetting.Builder<ListMode>()
        .name("ListMode")
        .description("Block list mode.")
        .defaultValue(ListMode.NONE)
        .build());

    private final Setting<List<Block>> filterBlocks = sgWorkMode.add(new BlockListSetting.Builder()
        .name("FilterBlocks")
        .description("Blocks to whitelist or blacklist.")
        .visible(() -> listMode.get() != ListMode.NONE)
        .build());

    private final Setting<Boolean> showBroken = sgRender.add(new BoolSetting.Builder()
        .name("BrokenBlocks")
        .description("Renders recently broken blocks.")
        .defaultValue(true)
        .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumChoiceSetting.Builder<ShapeMode>()
        .name("ShapeMode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.BOTH)
        .visible(showBroken::get)
        .build());

    private final Setting<Color> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("SideColor")
        .description("The side color of broken blocks.")
        .defaultValue(new Color(255, 0, 0, 80))
        .visible(showBroken::get)
        .build());

    private final Setting<Color> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("LineColor")
        .description("The line color of broken blocks.")
        .defaultValue(new Color(255, 0, 0, 255))
        .visible(showBroken::get)
        .build());

    private final List<BlockPos> toBreak = new ArrayList<>();
    private int timer;

    public LitematicaShredder() {
        super(Category.WORLD, "LitematicaShredder", "Breaks blocks that don't match the loaded Litematica schematic.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        toBreak.clear();
    }

    @Override
    public void onDeactivate() {
        toBreak.clear();
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

        if (timer > 0) {
            timer--;
            return;
        }

        if (silkTouch.get()) {
            equipSilkTouch();
        }

        toBreak.clear();

        Mode m = mode.get();
        double r = range.get();
        double wr = wallsRange.get();
        int radius = (int) Math.ceil(r) + 1;

        BlockIterator.register(radius, radius, (pos, worldState) -> {
            if (worldState.isAir()) {
                return;
            }

            BlockState schematicState = schematic.getBlockState(pos);
            if (schematicState.isAir() && !isInsideAnyPlacement(pos)) {
                return;
            }

            if (!shouldBreak(m, schematic, pos, schematicState, worldState)) {
                return;
            }

            if (!isAllowed(worldState.getBlock())) {
                return;
            }

            if (mc.player.getBoundingBox().intersects(Vec3d.of(pos), Vec3d.of(pos).add(1, 1, 1))) {
                return;
            }

            double distance = mc.player.getEyePos().distanceTo(pos.toCenterPos());
            if (distance > r) {
                return;
            }
            if (distance > wr && !isBlockVisible(pos)) {
                return;
            }

            toBreak.add(pos.toImmutable());
        });

        BlockIterator.after(() -> {
            int broken = 0;

            for (BlockPos pos : toBreak) {
                boolean instaBreak = mc.player.isCreative() || BlockUtils.canInstaBreak(pos);

                if (rotate.get()) {
                    Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos),
                        () -> BlockUtils.breakBlock(pos, swing.get()));
                } else {
                    BlockUtils.breakBlock(pos, swing.get());
                }

                if (showBroken.get()) {
                    RenderUtils.renderTickingBlock(pos, sideColor.get(), lineColor.get(), shapeMode.get(),
                        0, 8, true, false);
                }

                // Anything that isn't broken in a single tick keeps the mining progress, so stop here.
                if (!instaBreak || ++broken >= bpt.get()) {
                    break;
                }
            }

            timer = delay.get();
        });
    }

    private boolean isAllowed(Block block) {
        return switch (listMode.get()) {
            case NONE -> true;
            case WHITELIST -> filterBlocks.get().contains(block);
            case BLACKLIST -> !filterBlocks.get().contains(block);
        };
    }

    private boolean isBlockVisible(BlockPos pos) {
        RaycastContext context = new RaycastContext(mc.player.getEyePos(), pos.toCenterPos(),
            RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);

        HitResult result = mc.world.raycast(context);

        return result.getType() != HitResult.Type.BLOCK || pos.equals(((net.minecraft.util.hit.BlockHitResult) result).getBlockPos());
    }

    private void equipSilkTouch() {
        if (Utils.getEnchantmentLevel(mc.player.getMainHandStack(), Enchantments.SILK_TOUCH) > 0) {
            return;
        }

        for (int i = 0; i < 9; i++) {
            if (Utils.getEnchantmentLevel(mc.player.getInventory().getStack(i), Enchantments.SILK_TOUCH) > 0) {
                InventoryUtils.swap(i, false);
                return;
            }
        }
    }
    
    private boolean isInsideAnyPlacement(BlockPos pos) {
        for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
            if (!placement.isEnabled()) {
                continue;
            }

            fi.dy.masa.litematica.selection.Box box = placement.getEclosingBox();
            if (box == null || box.getPos1() == null || box.getPos2() == null) {
                continue;
            }

            BlockBox blockBox = box.toVanilla();
            if (blockBox != null && blockBox.contains(pos)) {
                return true;
            }
        }

        return false;
    }

    private boolean shouldBreak(Mode m, WorldSchematic schematicWorld, BlockPos pos, BlockState schematic, BlockState world) {
        if (schematic.isAir()) {
            return m == Mode.EXTRA || m == Mode.ALL;
        }

        if (world.getBlock() != schematic.getBlock()) {
            return m == Mode.WRONG_BLOCK || m == Mode.ALL;
        }

        if (m != Mode.WRONG_STATE && m != Mode.ALL) {
            return false;
        }

        for (Property<?> property : schematic.getProperties()) {
            if (!world.contains(property)) {
                return true;
            }
            if (!schematic.get(property).equals(world.get(property))) {
                return true;
            }
        }

        return hasWrongHeadTexture(schematicWorld, pos, world);
    }
    
    private boolean hasWrongHeadTexture(WorldSchematic schematicWorld, BlockPos pos, BlockState state) {
        if (!headTextures.get()
            || !(state.getBlock() instanceof AbstractSkullBlock skull)
            || skull.getSkullType() != SkullBlock.Type.PLAYER) {
            return false;
        }

        // The iterated position is reused for every block, and looking a head up can hand it to a block entity we
        // create along the way.
        BlockPos headPos = pos.toImmutable();

        return !sameSkin(owner(schematicWorld.getBlockEntity(headPos)), owner(mc.world.getBlockEntity(headPos)));
    }

    private ProfileComponent owner(BlockEntity blockEntity) {
        return blockEntity instanceof SkullBlockEntity skull ? skull.getOwner() : null;
    }
    
    private boolean sameSkin(ProfileComponent schematic, ProfileComponent world) {
        if (schematic == null || world == null) {
            return schematic == world;
        }

        if (!schematic.getOverride().equals(world.getOverride())) {
            return false;
        }

        String schematicTextures = textures(schematic);
        String worldTextures = textures(world);

        if (schematicTextures != null && worldTextures != null) {
            if (schematicTextures.equals(worldTextures)) {
                return true;
            }

            // The property is a signed blob with the time it was signed inside it, so the same skin looked up twice
            // does not come out as the same string. Only the url it lives at is worth comparing.
            String url = skinUrl(schematicTextures);
            return url != null && url.equals(skinUrl(worldTextures));
        }

        // Heads carrying only a name or an id have their skin looked up when they are rendered, so that is all there is
        // to go by.
        String schematicName = schematic.getName().filter(name -> !name.isBlank()).orElse(null);
        String worldName = world.getName().filter(name -> !name.isBlank()).orElse(null);

        if (schematicName != null && worldName != null) {
            return schematicName.equalsIgnoreCase(worldName);
        }

        return schematic.getGameProfile().id().equals(world.getGameProfile().id());
    }

    private String textures(ProfileComponent profile) {
        var properties = profile.getGameProfile().properties().get("textures");
        return properties.isEmpty() ? null : properties.iterator().next().value();
    }

    private String skinUrl(String textures) {
        try {
            JsonObject json = JsonParser.parseString(new String(Base64.getDecoder().decode(textures), StandardCharsets.UTF_8)).getAsJsonObject();
            return json.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    public enum Mode implements IDisplayName {

        ALL("All"),
        WRONG_BLOCK("Wrong Block"),
        WRONG_STATE("Wrong State"),
        EXTRA("Extra");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
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
}
