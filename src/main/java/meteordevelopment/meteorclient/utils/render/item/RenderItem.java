package meteordevelopment.meteorclient.utils.render.item;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import meteordevelopment.meteorclient.utils.render.ui.msdf.BuiltMsdf;
import meteordevelopment.meteorclient.utils.render.ui.msdf.MsdfFont;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleResourceReloader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public final class RenderItem {
    
    private static boolean reloadListenerRegistered;
    
    private RenderItem() {}
    
    public static void init() {
        registerReloadListener();
    }
    
    public static void beginFrame(DrawContext context) {
        renderer().beginFrame(context);
    }
    
    public static void flush() {
        renderer().flush();
    }
    
    public static void item(String id, float x, float y, float size) {
        item(stackOf(id), x, y, size, RenderItemOptions.defaults());
    }
    
    public static void item(String id, float x, float y, float size, RenderItemOptions options) {
        item(stackOf(id), x, y, size, options);
    }
    
    public static void item(ItemStack stack, float x, float y, float size) {
        item(stack, x, y, size, RenderItemOptions.defaults());
    }
    
    public static void item(ItemStack stack, float x, float y, float size, float alpha) {
        item(stack, x, y, size, RenderItemOptions.decorated(alpha));
    }
    
    public static void item(ItemStack stack, float x, float y, float size, RenderItemOptions options) {
        BuiltRenderItem item = new BuiltRenderItem(stack, x, y, size, options, 0);
        renderer().enqueue(item);
        drawDecorations(item);
    }
    
    public static void close() {
        CustomItemRenderer.closeInstance();
    }
    
    public static void beginGuiFrame() {
        renderer().beginGuiFrame();
    }
    
    public static void prepareBuffers() {
        renderer().prepareBuffers();
    }
    
    public static boolean isItemPipeline(com.mojang.blaze3d.pipeline.RenderPipeline pipeline) {
        return renderer().isItemPipeline(pipeline);
    }
    
    public static void bindParams(com.mojang.blaze3d.systems.RenderPass renderPass) {
        renderer().bindParams(renderPass);
    }
    
    public static void clearCaches() {
        renderer().clearCaches();
    }
    
    private static void drawDecorations(BuiltRenderItem item) {
        if (item == null || !item.visible()) {
            return;
        }
        
        ItemStack stack = item.stack();
        RenderItemOptions options = item.options();
        if (options.showDurability() && stack.isItemBarVisible()) {
            drawDurabilityBar(stack, item.x(), item.y(), item.size(), options.alpha());
        }
        if (options.showCount() && stack.getCount() > 1) {
            drawCount(stack.getCount(), item.x(), item.y(), item.size(), options.alpha());
        }
    }
    
    private static void drawDurabilityBar(ItemStack stack, float x, float y, float size, float alpha) {
        float barWidth = Math.max(8.0f, size * 0.78f);
        float barHeight = Math.max(1.5f, size * 0.07f);
        float barX = x + (size - barWidth) * 0.5f;
        float barY = y + size - barHeight - Math.max(1.0f, size * 0.08f);
        float fill = Math.max(0.0f, Math.min(1.0f, stack.getItemBarStep() / 13.0f));
        int barColor = stack.getItemBarColor();
        
        Render2D.rect(barX, barY, barWidth, barHeight, barHeight * 0.5f, ColorUtil.rgba(0, 0, 0, Math.round(150.0f * alpha)));
        Render2D.rect(
            barX,
            barY,
            Math.max(1.0f, barWidth * fill),
            barHeight,
            barHeight * 0.5f,
            ColorUtil.rgba((barColor >>> 16) & 0xFF, (barColor >>> 8) & 0xFF, barColor & 0xFF, Math.round(240.0f * alpha))
        );
    }
    
    private static void drawCount(int count, float x, float y, float size, float alpha) {
        String text = Integer.toString(count);
        float textSize = Math.clamp(size * 0.5f, 4.0f, 6.0f);
        float textWidth = MsdfFont.MONTSERRAT_SEMIBOLD.width(text, textSize);
        float textX = x + size - textWidth + size * 0.08f;
        float textY = y + size - textSize + size * 0.04f;
        float shadowOffset = Math.clamp(size * 0.07f, 0.35f, 0.75f);
        int shadowAlpha = Math.round(170.0f * alpha);
        int textAlpha = Math.round(255.0f * alpha);
        
        Render2D.msdf(new BuiltMsdf(MsdfFont.MONTSERRAT_SEMIBOLD, text, (int) (textX + shadowOffset), (int) (textY + shadowOffset), (int) textSize, ColorUtil.rgba(0, 0, 0, shadowAlpha)));
        Render2D.msdf(new BuiltMsdf(MsdfFont.MONTSERRAT_SEMIBOLD, text, (int) textX, (int) textY, (int) textSize, ColorUtil.rgba(255, 255, 255, textAlpha)));
    }
    
    private static ItemStack stackOf(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return ItemStack.EMPTY;
        }
        
        Identifier id = rawId.indexOf(':') >= 0
            ? Identifier.tryParse(rawId.trim())
            : Identifier.ofVanilla(rawId.trim());
        if (id == null) {
            return ItemStack.EMPTY;
        }
        
        Item item = Registries.ITEM.getOptionalValue(id).orElse(null);
        return item == null ? ItemStack.EMPTY : item.getDefaultStack();
    }
    
    private static CustomItemRenderer renderer() {
        return CustomItemRenderer.getInstance();
    }
    
    private static void registerReloadListener() {
        if (reloadListenerRegistered) {
            return;
        }
        
        Identifier id = MeteorClient.identifier("render_item_cache");
        ResourceLoader.get(ResourceType.CLIENT_RESOURCES).registerReloader(id, new SimpleResourceReloader<Void>() {
            @Override
            protected Void prepare(Store state) {
                return null;
            }
            
            @Override
            protected void apply(Void prepared, Store state) {
                RenderItem.clearCaches();
            }
        });
        reloadListenerRegistered = true;
    }
    
}
