package meteordevelopment.meteorclient.utils.render.ui.effecticon;

import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;

public record BuiltEffectIcon(
    RegistryEntry<StatusEffect> effect,
    float x,
    float y,
    float size,
    int color
) {
    
    public static final int DEFAULT_COLOR = ColorUtil.WHITE;
    
    public BuiltEffectIcon(RegistryEntry<StatusEffect> effect, float x, float y, float size) {
        this(effect, x, y, size, DEFAULT_COLOR);
    }
    
    public BuiltEffectIcon(StatusEffectInstance instance, float x, float y, float size) {
        this(instance == null ? null : instance.getEffectType(), x, y, size, DEFAULT_COLOR);
    }
    
    public BuiltEffectIcon(StatusEffectInstance instance, float x, float y, float size, int color) {
        this(instance == null ? null : instance.getEffectType(), x, y, size, color);
    }
    
    public boolean visible() {
        return effect != null
            && effect.hasKeyAndValue()
            && size > 0.0f
            && effectiveAlpha(color) > 0;
    }
    
    private static int effectiveAlpha(int color) {
        int alpha = (color >>> 24) & 0xFF;
        return alpha == 0 && (color & 0x00FFFFFF) != 0 ? 255 : alpha;
    }
    
}
