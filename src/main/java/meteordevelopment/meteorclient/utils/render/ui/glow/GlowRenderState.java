package meteordevelopment.meteorclient.utils.render.ui.glow;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/**
 * The single quad that draws one {@link GlowGroup}'s finished halo on screen.
 * <p>
 * The quad is the content rect grown by the group's padding. The exact padding is only known once the atlas has been
 * laid out, which happens after this state was created, so {@code pad} here is a deliberate over-estimate used for
 * {@link #bounds()} and the real value is read back from {@link GlowCapture#effectivePad} at vertex time.
 */
final class GlowRenderState implements SimpleGuiElementRenderState {

    private final Matrix3x2f pose;
    private final GlowCapture capture;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;

    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final float pad;
    private final float alpha;

    GlowRenderState(
        Matrix3x2f pose,
        GlowCapture capture,
        ScreenRect scissor,
        float x,
        float y,
        float width,
        float height,
        float pad,
        float alpha
    ) {
        this.pose = new Matrix3x2f((Matrix3x2fc) pose);
        this.capture = capture;
        this.scissorArea = scissor;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.pad = pad;
        this.alpha = alpha;

        int left = (int) Math.floor(x - pad);
        int top = (int) Math.floor(y - pad);
        int boundsWidth = (int) Math.ceil(width + pad * 2.0f);
        int boundsHeight = (int) Math.ceil(height + pad * 2.0f);
        ScreenRect transformed = new ScreenRect(left, top, Math.max(1, boundsWidth), Math.max(1, boundsHeight))
            .transformEachVertex((Matrix3x2fc) (Object) this.pose);
        this.bounds = scissor == null ? transformed : scissor.intersection(transformed);
    }

    @Override
    public ScreenRect bounds() {
        return this.bounds;
    }

    @Override
    public RenderPipeline pipeline() {
        return GlowRenderer.GLOW_COMPOSITE_PIPELINE;
    }

    @Override
    public ScreenRect scissorArea() {
        return this.scissorArea;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.empty();
    }

    @Override
    public void setupVertices(VertexConsumer vertices) {
        int id = GlowRenderer.getInstance().reserve(this.capture, this.alpha);
        if (id < 0) {
            return;
        }

        float effective = this.capture.prepared ? this.capture.effectivePad : this.pad;
        float x1 = this.x - effective;
        float y1 = this.y - effective;
        float x2 = this.x + this.width + effective;
        float y2 = this.y + this.height + effective;

        this.vertex(vertices, x1, y1, 0, 0, id);
        this.vertex(vertices, x1, y2, 0, 255, id);
        this.vertex(vertices, x2, y2, 255, 255, id);
        this.vertex(vertices, x2, y1, 255, 0, id);
    }

    private void vertex(VertexConsumer vc, float x, float y, int u, int v, int id) {
        vc.vertex((Matrix3x2fc) (Object) this.pose, x, y).color(u, v, 255, 255).lineWidth((float) (id + 1));
    }

}
