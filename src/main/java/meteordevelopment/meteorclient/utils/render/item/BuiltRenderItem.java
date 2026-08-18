package meteordevelopment.meteorclient.utils.render.item;

import net.minecraft.item.ItemStack;

public record BuiltRenderItem(
    ItemStack stack,
    float x,
    float y,
    float size,
    RenderItemOptions options,
    int seed
) {
    
    public BuiltRenderItem {
        stack = stack == null ? ItemStack.EMPTY : stack;
        options = options == null ? RenderItemOptions.defaults() : options;
    }
    
    public BuiltRenderItem(ItemStack stack, float x, float y, float size) {
        this(stack, x, y, size, RenderItemOptions.defaults(), 0);
    }
    
    public boolean visible() {
        return !stack.isEmpty()
            && size > 0.0f
            && options.alpha() > 0.0f
            && (options.color() >>> 24) != 0;
    }
    
}
