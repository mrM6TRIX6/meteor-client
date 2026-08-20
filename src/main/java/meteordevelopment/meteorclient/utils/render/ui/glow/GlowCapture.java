package meteordevelopment.meteorclient.utils.render.ui.glow;

import net.minecraft.client.texture.TextureSetup;

public final class GlowCapture {
    
    public float regionU0 = 0.0f;
    public float regionV0 = 0.0f;
    public float regionUW = 1.0f;
    public float regionVH = 1.0f;
    public int index = 0;
    public TextureSetup setup = TextureSetup.empty();
    
    public float effectivePad = 0.0f;
    public boolean prepared = false;
    
    GlowCapture() {}
    
    public void reset() {
        this.regionU0 = 0.0f;
        this.regionV0 = 0.0f;
        this.regionUW = 1.0f;
        this.regionVH = 1.0f;
        this.effectivePad = 0.0f;
        this.prepared = false;
        this.setup = TextureSetup.empty();
    }
    
}
