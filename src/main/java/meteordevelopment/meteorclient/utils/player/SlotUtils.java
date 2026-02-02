/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.mixin.ItemGroupsAccessor;
import meteordevelopment.meteorclient.mixin.MountScreenHandlerAccessor;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.mob.ZombieHorseEntity;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.registry.Registries;
import net.minecraft.screen.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class SlotUtils {
    
    public static final int HOTBAR_START = 0;
    public static final int HOTBAR_END = 8;
    
    public static final int OFFHAND = 45;
    
    public static final int MAIN_START = 9;
    public static final int MAIN_END = 35;
    
    public static final int ARMOR_START = 36;
    public static final int ARMOR_END = 39;
    
    public static final int HEAD = 36;
    public static final int CHEST = 37;
    public static final int LEGS = 38;
    public static final int FEET = 39;
    
    private SlotUtils() {}
    
    public static int indexToIdCurrentHandler(int i) {
        if (mc.player == null) {
            return -1;
        }
        
        return switch(mc.player.currentScreenHandler) {
            case PlayerScreenHandler ignored -> survivalInventory(i);
            case CreativeInventoryScreen.CreativeScreenHandler ignored -> creativeInventory(i);
            case GenericContainerScreenHandler handler -> genericContainer(i, handler.getRows());
            case CraftingScreenHandler ignored -> craftingTable(i);
            case FurnaceScreenHandler ignored -> furnace(i);
            case BlastFurnaceScreenHandler ignored -> furnace(i);
            case SmokerScreenHandler ignored -> furnace(i);
            case Generic3x3ContainerScreenHandler ignored -> generic3x3(i);
            case EnchantmentScreenHandler ignored -> enchantmentTable(i);
            case BrewingStandScreenHandler ignored -> brewingStand(i);
            case MerchantScreenHandler ignored -> villager(i);
            case BeaconScreenHandler ignored -> beacon(i);
            case AnvilScreenHandler ignored -> anvil(i);
            case HopperScreenHandler ignored -> hopper(i);
            case ShulkerBoxScreenHandler ignored -> genericContainer(i, 3);
            case HorseScreenHandler handler -> horse(handler, i);
            case CartographyTableScreenHandler ignored -> cartographyTable(i);
            case GrindstoneScreenHandler ignored -> grindstone(i);
            case LecternScreenHandler ignored -> lectern();
            case LoomScreenHandler ignored -> loom(i);
            case StonecutterScreenHandler ignored -> stonecutter(i);
            
            default -> -1;
        };
    }
    
    public static int survivalInventory(int i) {
        if (isHotbar(i)) {
            return 36 + i;
        }
        if (isArmor(i)) {
            return 5 + (i - 36);
        }
        return i;
    }
    
    public static int creativeInventory(int i) {
        if (mc.currentScreen instanceof CreativeInventoryScreen) {
            Registries.ITEM_GROUP.get(ItemGroupsAccessor.meteor$getInventory());
        }
        return survivalInventory(i);
    }
    
    public static int genericContainer(int i, int rows) {
        if (isHotbar(i)) {
            return (rows + 3) * 9 + i;
        }
        if (isMain(i)) {
            return rows * 9 + (i - 9);
        }
        return -1;
    }
    
    public static int craftingTable(int i) {
        if (isHotbar(i)) {
            return 37 + i;
        }
        if (isMain(i)) {
            return i + 1;
        }
        return -1;
    }
    
    public static int furnace(int i) {
        if (isHotbar(i)) {
            return 30 + i;
        }
        if (isMain(i)) {
            return 3 + (i - 9);
        }
        return -1;
    }
    
    public static int generic3x3(int i) {
        if (isHotbar(i)) {
            return 36 + i;
        }
        if (isMain(i)) {
            return i;
        }
        return -1;
    }
    
    public static int enchantmentTable(int i) {
        if (isHotbar(i)) {
            return 29 + i;
        }
        if (isMain(i)) {
            return 2 + (i - 9);
        }
        return -1;
    }
    
    public static int brewingStand(int i) {
        if (isHotbar(i)) {
            return 32 + i;
        }
        if (isMain(i)) {
            return 5 + (i - 9);
        }
        return -1;
    }
    
    public static int villager(int i) {
        if (isHotbar(i)) {
            return 30 + i;
        }
        if (isMain(i)) {
            return 3 + (i - 9);
        }
        return -1;
    }
    
    public static int beacon(int i) {
        if (isHotbar(i)) {
            return 28 + i;
        }
        if (isMain(i)) {
            return 1 + (i - 9);
        }
        return -1;
    }
    
    public static int anvil(int i) {
        if (isHotbar(i)) {
            return 30 + i;
        }
        if (isMain(i)) {
            return 3 + (i - 9);
        }
        return -1;
    }
    
    public static int hopper(int i) {
        if (isHotbar(i)) {
            return 32 + i;
        }
        if (isMain(i)) {
            return 5 + (i - 9);
        }
        return -1;
    }
    
    public static int horse(ScreenHandler handler, int i) {
        LivingEntity entity = ((MountScreenHandlerAccessor) handler).meteor$getMount();
        
        if (entity instanceof LlamaEntity llamaEntity) {
            int strength = llamaEntity.getStrength();
            if (isHotbar(i)) {
                return (2 + 3 * strength) + 28 + i;
            }
            if (isMain(i)) {
                return (2 + 3 * strength) + 1 + (i - 9);
            }
        } else if (entity instanceof HorseEntity || entity instanceof SkeletonHorseEntity || entity instanceof ZombieHorseEntity) {
            if (isHotbar(i)) {
                return 29 + i;
            }
            if (isMain(i)) {
                return 2 + (i - 9);
            }
        } else if (entity instanceof AbstractDonkeyEntity abstractDonkeyEntity) {
            boolean chest = abstractDonkeyEntity.hasChest();
            if (isHotbar(i)) {
                return (chest ? 44 : 29) + i;
            }
            if (isMain(i)) {
                return (chest ? 17 : 2) + (i - 9);
            }
        }
        
        return -1;
    }
    
    public static int cartographyTable(int i) {
        if (isHotbar(i)) {
            return 30 + i;
        }
        if (isMain(i)) {
            return 3 + (i - 9);
        }
        return -1;
    }
    
    public static int grindstone(int i) {
        if (isHotbar(i)) {
            return 30 + i;
        }
        if (isMain(i)) {
            return 3 + (i - 9);
        }
        return -1;
    }
    
    public static int lectern() {
        return -1;
    }
    
    public static int loom(int i) {
        if (isHotbar(i)) {
            return 31 + i;
        }
        if (isMain(i)) {
            return 4 + (i - 9);
        }
        return -1;
    }
    
    public static int stonecutter(int i) {
        if (isHotbar(i)) {
            return 29 + i;
        }
        if (isMain(i)) {
            return 2 + (i - 9);
        }
        return -1;
    }
    
    // Utils
    
    public static boolean isHotbar(int i) {
        return i >= HOTBAR_START && i <= HOTBAR_END;
    }
    
    public static boolean isMain(int i) {
        return i >= MAIN_START && i <= MAIN_END;
    }
    
    public static boolean isArmor(int i) {
        return i >= ARMOR_START && i <= ARMOR_END;
    }
    
}
