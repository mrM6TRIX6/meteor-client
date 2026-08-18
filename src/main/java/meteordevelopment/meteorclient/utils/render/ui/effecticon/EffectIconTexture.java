package meteordevelopment.meteorclient.utils.render.ui.effecticon;

import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;

record EffectIconTexture(
    Identifier id,
    TextureSetup setup
) {}
