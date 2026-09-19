/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.NametagUtils;
import meteordevelopment.meteorclient.renderer.RenderUtils;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.name.IDisplayName;
import meteordevelopment.meteorclient.utils.name.Names;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import meteordevelopment.meteorclient.utils.render.ui.msdf.BuiltMsdf;
import meteordevelopment.meteorclient.utils.render.ui.msdf.MsdfFont;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.joml.Vector3d;

import java.util.*;

public class Nametags extends Module {

    private static final MsdfFont FONT = MsdfFont.MONTSERRAT_MEDIUM;
    private static final float TEXT_SIZE = 8.0f;

    /** Side of one rendered item, matching the scale of 2 passed to {@link RenderUtils#drawItem}. */
    private static final float ITEM_SIZE = 32.0f;
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgItems = settings.createGroup("Items");
    private final SettingGroup sgRender = settings.createGroup("Render");
    
    // General
    
    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("Entities")
        .description("Select entities to draw nametags on.")
        .defaultValue(EntityType.PLAYER, EntityType.ITEM)
        .build()
    );
    
    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("Scale")
        .description("The scale of the nametag.")
        .defaultValue(1.1)
        .min(0.1)
        .build()
    );
    
    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("IgnoreSelf")
        .description("Ignore yourself when in third person or freecam.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("IgnoreFriends")
        .description("Ignore rendering nametags for friends.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Boolean> ignoreBots = sgGeneral.add(new BoolSetting.Builder()
        .name("IgnoreBots")
        .description("Only render non-bot nametags.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> culling = sgGeneral.add(new BoolSetting.Builder()
        .name("Culling")
        .description("Only render a certain number of nametags at a certain distance.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Double> maxCullRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("CullingRange")
        .description("Only render nametags within this distance of your player.")
        .defaultValue(20)
        .min(0)
        .sliderMax(200)
        .visible(culling::get)
        .build()
    );
    
    private final Setting<Integer> maxCullCount = sgGeneral.add(new IntSetting.Builder()
        .name("CullingCount")
        .description("Only render this many nametags.")
        .defaultValue(50)
        .min(1)
        .sliderRange(1, 100)
        .visible(culling::get)
        .build()
    );
    
    //Players
    
    private final Setting<Boolean> displayHealth = sgPlayers.add(new BoolSetting.Builder()
        .name("Health")
        .description("Shows the player's health.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> displayGameMode = sgPlayers.add(new BoolSetting.Builder()
        .name("Gamemode")
        .description("Shows the player's GameMode.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Boolean> displayDistance = sgPlayers.add(new BoolSetting.Builder()
        .name("Distance")
        .description("Shows the distance between you and the player.")
        .defaultValue(false)
        .build()
    );
    
    private final Setting<Boolean> displayPing = sgPlayers.add(new BoolSetting.Builder()
        .name("Ping")
        .description("Shows the player's ping.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> displayItems = sgPlayers.add(new BoolSetting.Builder()
        .name("Items")
        .description("Displays armor and hand items above the name tags.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Double> itemSpacing = sgPlayers.add(new DoubleSetting.Builder()
        .name("ItemSpacing")
        .description("The spacing between items.")
        .defaultValue(2)
        .range(0, 10)
        .visible(displayItems::get)
        .build()
    );
    
    private final Setting<Boolean> ignoreEmpty = sgPlayers.add(new BoolSetting.Builder()
        .name("IgnoreEmptySlots")
        .description("Doesn't add spacing where an empty item stack would be.")
        .defaultValue(true)
        .visible(displayItems::get)
        .build()
    );
    
    private final Setting<Durability> itemDurability = sgPlayers.add(new EnumChoiceSetting.Builder<Durability>()
        .name("Durability")
        .description("Displays item durability as either a total, percentage, or neither.")
        .defaultValue(Durability.NONE)
        .visible(displayItems::get)
        .build()
    );
    
    private final Setting<Boolean> displayEnchants = sgPlayers.add(new BoolSetting.Builder()
        .name("DisplayEnchants")
        .description("Displays item enchantments on the items.")
        .defaultValue(false)
        .visible(displayItems::get)
        .build()
    );
    
    private final Setting<Set<RegistryKey<Enchantment>>> shownEnchantments = sgPlayers.add(new EnchantmentListSetting.Builder()
        .name("ShownEnchantments")
        .description("The enchantments that are shown on nametags.")
        .visible(() -> displayItems.get() && displayEnchants.get())
        .defaultValue(
            Enchantments.PROTECTION,
            Enchantments.BLAST_PROTECTION,
            Enchantments.FIRE_PROTECTION,
            Enchantments.PROJECTILE_PROTECTION
        )
        .build()
    );
    
    private final Setting<Position> enchantPos = sgPlayers.add(new EnumChoiceSetting.Builder<Position>()
        .name("EnchantmentPosition")
        .description("Where the enchantments are rendered.")
        .defaultValue(Position.ABOVE)
        .visible(() -> displayItems.get() && displayEnchants.get())
        .build()
    );
    
    private final Setting<Integer> enchantLength = sgPlayers.add(new IntSetting.Builder()
        .name("EnchantNameLength")
        .description("The length enchantment names are trimmed to.")
        .defaultValue(3)
        .range(1, 5)
        .sliderRange(1, 5)
        .visible(() -> displayItems.get() && displayEnchants.get())
        .build()
    );
    
    private final Setting<Double> enchantTextScale = sgPlayers.add(new DoubleSetting.Builder()
        .name("EnchantTextScale")
        .description("The scale of the enchantment text.")
        .defaultValue(1)
        .range(0.1, 2)
        .sliderRange(0.1, 2)
        .visible(() -> displayItems.get() && displayEnchants.get())
        .build()
    );
    
    //Items
    
    private final Setting<Boolean> itemCount = sgItems.add(new BoolSetting.Builder()
        .name("ShowCount")
        .description("Displays the number of items in the stack.")
        .defaultValue(true)
        .build()
    );
    
    // Render
    
    private final Setting<Color> background = sgRender.add(new ColorSetting.Builder()
        .name("BackgroundColor")
        .description("The color of the nametag background.")
        .defaultValue(new Color(0, 0, 0, 75))
        .build()
    );
    
    private final Setting<Color> nameColor = sgRender.add(new ColorSetting.Builder()
        .name("NameColor")
        .description("The color of the nametag names.")
        .defaultValue(new Color())
        .build()
    );
    
    private final Setting<Color> pingColor = sgRender.add(new ColorSetting.Builder()
        .name("PingColor")
        .description("The color of the nametag ping.")
        .defaultValue(new Color(20, 170, 170))
        .visible(displayPing::get)
        .build()
    );
    
    private final Setting<Color> gamemodeColor = sgRender.add(new ColorSetting.Builder()
        .name("GamemodeColor")
        .description("The color of the nametag gamemode.")
        .defaultValue(new Color(232, 185, 35))
        .visible(displayGameMode::get)
        .build()
    );
    
    private final Setting<DistanceColorMode> distanceColorMode = sgRender.add(new EnumChoiceSetting.Builder<DistanceColorMode>()
        .name("DistanceColorMode")
        .description("The mode to color the nametag distance with.")
        .defaultValue(DistanceColorMode.GRADIENT)
        .visible(displayDistance::get)
        .build()
    );
    
    private final Setting<Color> distanceColor = sgRender.add(new ColorSetting.Builder()
        .name("DistanceColor")
        .description("The color of the nametag distance.")
        .defaultValue(new Color(150, 150, 150))
        .visible(() -> displayDistance.get() && distanceColorMode.get() == DistanceColorMode.MODE)
        .build()
    );
    
    private final Color WHITE = new Color(255, 255, 255);
    private final Color RED = new Color(255, 25, 25);
    private final Color AMBER = new Color(255, 105, 25);
    private final Color GREEN = new Color(25, 252, 25);
    private final Color GOLD = new Color(232, 185, 35);
    
    private final Vector3d pos = new Vector3d();
    private final double[] itemWidths = new double[6];
    
    private final List<Entity> entityList = new ArrayList<>();
    
    public Nametags() {
        super(Category.RENDER, "Nametags", "Displays customizable nametags above players, items and other entities.");
    }
    
    private static String ticksToTime(int ticks) {
        if (ticks > 20 * 3600) {
            int h = ticks / 20 / 3600;
            return h + " h";
        } else if (ticks > 20 * 60) {
            int m = ticks / 20 / 60;
            return m + " m";
        } else {
            int s = ticks / 20;
            int ms = (ticks % 20) / 2;
            return s + "." + ms + " s";
        }
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        entityList.clear();
        
        boolean freecamNotActive = !Modules.get().isActive(Freecam.class);
        boolean notThirdPerson = mc.options.getPerspective().isFirstPerson();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
        
        for (Entity entity : mc.world.getEntities()) {
            EntityType<?> type = entity.getType();
            if (!entities.get().contains(type)) {
                continue;
            }
            
            if (type == EntityType.PLAYER) {
                if ((ignoreSelf.get() || (freecamNotActive && notThirdPerson)) && entity == mc.player) {
                    continue;
                }
                if (EntityUtils.getGameMode((PlayerEntity) entity) == null && ignoreBots.get()) {
                    continue;
                }
                if (Friends.get().isFriend((PlayerEntity) entity) && ignoreFriends.get()) {
                    continue;
                }
            }
            
            if (!culling.get() || PlayerUtils.isWithinCamera(entity, maxCullRange.get())) {
                entityList.add(entity);
            }
        }
        
        entityList.sort(Comparator.comparing(e -> e.squaredDistanceTo(cameraPos)));
    }
    
    @EventHandler
    private void onRender2D(Render2DEvent event) {
        int count = getRenderCount();
        
        for (int i = count - 1; i > -1; i--) {
            Entity entity = entityList.get(i);
            
            Utils.set(pos, entity, event.tickDelta);
            pos.add(0, getHeight(entity), 0);
            
            EntityType<?> type = entity.getType();
            
            if (!NametagUtils.to2D(pos, scale.get())) {
                continue;
            }
            
            if (type == EntityType.PLAYER) {
                renderNametagPlayer(event, (PlayerEntity) entity);
            } else if (type == EntityType.ITEM) {
                renderNametagItem(event.drawContext, ((ItemEntity) entity).getStack());
            } else if (type == EntityType.ITEM_FRAME || type == EntityType.GLOW_ITEM_FRAME) {
                renderNametagItem(event.drawContext, ((ItemFrameEntity) entity).getHeldItemStack());
            } else if (type == EntityType.TNT) {
                renderTntNametag(event.drawContext, ticksToTime(((TntEntity) entity).getFuse()));
            } else if (type == EntityType.TNT_MINECART && ((TntMinecartEntity) entity).isPrimed()) {
                renderTntNametag(event.drawContext, ticksToTime(((TntMinecartEntity) entity).getFuseTicks()));
            } else if (entity instanceof LivingEntity livingEntity) {
                renderGenericLivingNametag(event.drawContext, livingEntity);
            } else {
                renderGenericNametag(event.drawContext, entity);
            }
        }
    }
    
    private int getRenderCount() {
        int count = culling.get() ? maxCullCount.get() : entityList.size();
        count = MathHelper.clamp(count, 0, entityList.size());
        
        return count;
    }
    
    @Override
    public String getInfoString() {
        return Integer.toString(getRenderCount());
    }
    
    private double getHeight(Entity entity) {
        double height = entity.getEyeHeight(entity.getPose());
        
        if (entity.getType() == EntityType.ITEM || entity.getType() == EntityType.ITEM_FRAME || entity.getType() == EntityType.GLOW_ITEM_FRAME) {
            height += 0.2;
        } else {
            height += 0.5;
        }
        
        return height;
    }
    
    private void renderNametagPlayer(Render2DEvent event, PlayerEntity player) {
        // Gamemode
        GameMode gm = EntityUtils.getGameMode(player);
        String gmText = "BOT";
        if (gm != null) {
            gmText = switch (gm) {
                case SPECTATOR -> "Sp";
                case SURVIVAL -> "S";
                case CREATIVE -> "C";
                case ADVENTURE -> "A";
            };
        }

        gmText = "[" + gmText + "] ";

        // Name
        String name;
        Color nameColor = PlayerUtils.getPlayerColor(player, this.nameColor.get());

        if (player == mc.player) {
            name = Modules.get().get(NameProtect.class).getName(player.getName().getString());
        } else {
            name = player.getName().getString();
        }

        // Health
        float absorption = player.getAbsorptionAmount();
        int health = Math.round(player.getHealth() + absorption);
        double healthPercentage = health / (player.getMaxHealth() + absorption);

        Color healthColor;
        if (healthPercentage <= 0.333) {
            healthColor = RED;
        } else if (healthPercentage <= 0.666) {
            healthColor = AMBER;
        } else {
            healthColor = GREEN;
        }

        boolean renderPlayerDistance = player != mc.getCameraEntity() || Modules.get().isActive(Freecam.class);

        MutableText text = Text.empty();

        if (displayGameMode.get()) {
            text.append(Text.literal(gmText).styled(s -> gamemodeColor.get().styleWith(s)));
        }
        text.append(Text.literal(name).styled(s -> nameColor.styleWith(s)));

        if (displayHealth.get()) {
            text.append(Text.literal(" " + health).styled(s -> healthColor.styleWith(s)));
        }
        if (displayPing.get()) {
            text.append(Text.literal(" [" + EntityUtils.getPing(player) + "ms]").styled(s -> pingColor.get().styleWith(s)));
        }
        if (displayDistance.get() && renderPlayerDistance) {
            double dist = Math.round(PlayerUtils.distanceToCamera(player) * 10.0) / 10.0;
            Color color = switch (distanceColorMode.get()) {
                case MODE -> distanceColor.get();
                case GRADIENT -> EntityUtils.getColorFromDistance(player);
            };
            text.append(Text.literal(" " + dist + "m").styled(s -> color.styleWith(s)));
        }

        float width = FONT.width(text, TEXT_SIZE);
        float height = FONT.height(TEXT_SIZE);
        float widthHalf = width / 2;

        MutableText finalText = text;
        NametagUtils.render(event.drawContext, pos, () -> {
            drawBg(-widthHalf, -height, width, height);
            Render2D.msdf(new BuiltMsdf(FONT, finalText, (int) (-widthHalf), (int) (-height), (int) TEXT_SIZE));

            if (displayItems.get()) {
                renderItems(event.drawContext, player, height);
            } else if (displayEnchants.get()) {
                displayEnchants.set(false);
            }
        });
    }

    /**
     * Items and their enchantments sit above the name line, so this runs inside the same
     * {@link NametagUtils#render} block and takes {@code nameHeight} to know where that line ended.
     */
    private void renderItems(DrawContext context, PlayerEntity player, float nameHeight) {
        Arrays.fill(itemWidths, 0);
        boolean hasItems = false;
        int maxEnchantCount = 0;

        float enchantSize = TEXT_SIZE * enchantTextScale.get().floatValue();
        float enchantHeight = FONT.height(enchantSize);

        for (int i = 0; i < 6; i++) {
            ItemStack itemStack = getItem(player, i);

            if (itemWidths[i] == 0 && (!ignoreEmpty.get() || !itemStack.isEmpty())) {
                itemWidths[i] = ITEM_SIZE + itemSpacing.get();
            }

            if (!itemStack.isEmpty()) {
                hasItems = true;
            }

            if (displayEnchants.get()) {
                ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(itemStack);

                int size = 0;
                for (RegistryEntry<Enchantment> enchantment : enchantments.getEnchantments()) {
                    if (enchantment.getKey().isEmpty() || !shownEnchantments.get().contains(enchantment.getKey().get())) {
                        continue;
                    }
                    String enchantName = Utils.getEnchantSimpleName(enchantment, enchantLength.get()) + " " + enchantments.getLevel(enchantment);
                    itemWidths[i] = Math.max(itemWidths[i], FONT.width(enchantName, enchantSize));
                    size++;
                }

                maxEnchantCount = Math.max(maxEnchantCount, size);
            }
        }

        double itemsHeight = hasItems ? ITEM_SIZE : 0;
        double itemWidthTotal = 0;
        for (double w : itemWidths) {
            itemWidthTotal += w;
        }

        double y = -nameHeight - 7 - itemsHeight;
        double x = -itemWidthTotal / 2;

        for (int i = 0; i < 6; i++) {
            ItemStack stack = getItem(player, i);

            RenderUtils.drawItem(context, stack, (int) x, (int) y, 2, true, null, false);

            if (stack.isDamageable() && itemDurability.get() != Durability.NONE) {
                String damageText = switch (itemDurability.get()) {
                    case PERCENTAGE ->
                        String.format("%.0f%%", ((stack.getMaxDamage() - stack.getDamage()) * 100f) / (float) stack.getMaxDamage());
                    case TOTAL -> Integer.toString(stack.getMaxDamage() - stack.getDamage());
                    default -> "err";
                };

                Render2D.msdf(new BuiltMsdf(
                    FONT,
                    damageText,
                    (int) x,
                    (int) y,
                    (int) (TEXT_SIZE * 0.75f),
                    new Color(stack.getItemBarColor()).a(255).getPacked()
                ));
            }

            if (maxEnchantCount > 0 && displayEnchants.get()) {
                ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);
                Object2IntMap<RegistryEntry<Enchantment>> enchantmentsToShow = new Object2IntOpenHashMap<>();

                for (RegistryEntry<Enchantment> enchantment : enchantments.getEnchantments()) {
                    if (enchantment.matches(shownEnchantments.get()::contains)) {
                        enchantmentsToShow.put(enchantment, enchantments.getLevel(enchantment));
                    }
                }

                double aW = itemWidths[i];
                double enchantY = 0;

                double addY = switch (enchantPos.get()) {
                    case ABOVE -> -((enchantmentsToShow.size() + 1) * enchantHeight);
                    case ON_TOP -> (itemsHeight - enchantmentsToShow.size() * enchantHeight) / 2;
                };

                for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : Object2IntMaps.fastIterable(enchantmentsToShow)) {
                    String enchantName = Utils.getEnchantSimpleName(entry.getKey(), enchantLength.get()) + " " + entry.getIntValue();

                    Color enchantColor = entry.getKey().isIn(EnchantmentTags.CURSE) ? RED : WHITE;
                    double enchantX = x + (aW - FONT.width(enchantName, enchantSize)) / 2;

                    Render2D.msdf(new BuiltMsdf(
                        FONT,
                        enchantName,
                        (int) enchantX,
                        (int) (y + addY + enchantY),
                        (int) enchantSize,
                        enchantColor.getPacked()
                    ));

                    enchantY += enchantHeight;
                }
            }

            x += itemWidths[i];
        }
    }




    private void renderNametagItem(DrawContext context, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        MutableText text = Text.literal(Names.get(stack)).styled(s -> nameColor.get().styleWith(s));

        if (itemCount.get()) {
            text.append(Text.literal(" x" + stack.getCount()).styled(s -> GOLD.styleWith(s)));
        }

        renderLine(context, text);
    }

    private void renderGenericLivingNametag(DrawContext context, LivingEntity entity) {
        float absorption = entity.getAbsorptionAmount();
        int health = Math.round(entity.getHealth() + absorption);
        double healthPercentage = health / (entity.getMaxHealth() + absorption);

        Color healthColor;
        if (healthPercentage <= 0.333) {
            healthColor = RED;
        } else if (healthPercentage <= 0.666) {
            healthColor = AMBER;
        } else {
            healthColor = GREEN;
        }

        Text text = Text.empty()
            .append(Text.literal(entity.getType().getName().getString() + " ").styled(s -> nameColor.get().styleWith(s)))
            .append(Text.literal(String.valueOf(health)).styled(s -> healthColor.styleWith(s)));
        
        renderLine(context, text);
    }

    private void renderGenericNametag(DrawContext context, Entity entity) {
        Text text = Text.literal(entity.getType().getName().getString())
            .styled(s -> nameColor.get().styleWith(s));
        renderLine(context, text);
    }

    private void renderTntNametag(DrawContext context, String fuseText) {
        Text text = Text.literal(fuseText).styled(s -> nameColor.get().styleWith(s));
        renderLine(context, text);
    }

    /** Draws a single-line nametag, centred above {@link #pos} and sitting on its background. */
    private void renderLine(DrawContext context, Text text) {
        float width = FONT.width(text, TEXT_SIZE);
        float height = FONT.height(TEXT_SIZE);
        float widthHalf = width / 2;

        NametagUtils.render(context, pos, () -> {
            drawBg(-widthHalf, -height, width, height);
            Render2D.msdf(new BuiltMsdf(FONT, text, (int) (-widthHalf), (int) (-height), (int) TEXT_SIZE));
        });
    }

    private ItemStack getItem(PlayerEntity entity, int index) {
        return switch (index) {
            case 0 -> entity.getMainHandStack();
            case 1 -> entity.getEquippedStack(EquipmentSlot.HEAD);
            case 2 -> entity.getEquippedStack(EquipmentSlot.CHEST);
            case 3 -> entity.getEquippedStack(EquipmentSlot.LEGS);
            case 4 -> entity.getEquippedStack(EquipmentSlot.FEET);
            case 5 -> entity.getOffHandStack();
            default -> ItemStack.EMPTY;
        };
    }

    private void drawBg(double x, double y, double width, double height) {
        Render2D.rect((float) x - 2, (float) y - 1, (float) width + 4, (float) height + 2, 1, background.get().getPacked());
    }

    
    public boolean excludeBots() {
        return ignoreBots.get();
    }
    
    public boolean playerNametags() {
        return isActive() && entities.get().contains(EntityType.PLAYER);
    }
    
    private enum Position implements IDisplayName {
        
        ABOVE("Above"),
        ON_TOP("On Top");
        
        private final String displayName;
        
        Position(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
    }
    
    private enum Durability implements IDisplayName {
        
        NONE("None"),
        TOTAL("Total"),
        PERCENTAGE("Percentage");
        
        private final String displayName;
        
        Durability(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
    }
    
    private enum DistanceColorMode implements IDisplayName {
        
        GRADIENT("Gradient"),
        MODE("Mode");
        
        private final String displayName;
        
        DistanceColorMode(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String getDisplayName() {
            return displayName;
        }
        
    }
    
}
