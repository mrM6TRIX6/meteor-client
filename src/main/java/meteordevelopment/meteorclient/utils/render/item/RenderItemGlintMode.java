package meteordevelopment.meteorclient.utils.render.item;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemStack;

public enum RenderItemGlintMode {
    
    AUTO,
    OFF,
    ON;
    
    boolean enabled(ItemStack stack, ItemRenderState.Glint foilType) {
        return enabled(stack, foilType != ItemRenderState.Glint.NONE);
    }
    
    boolean enabled(ItemStack stack, boolean foil) {
        return switch (this) {
            case ON -> true;
            case OFF -> false;
            case AUTO -> stack != null && (stack.hasGlint() || foil);
        };
    }
    
}
