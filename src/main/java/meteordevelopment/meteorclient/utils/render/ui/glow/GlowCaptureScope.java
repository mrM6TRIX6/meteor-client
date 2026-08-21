package meteordevelopment.meteorclient.utils.render.ui.glow;

import meteordevelopment.meteorclient.utils.render.ui.Render2D;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;

import java.util.List;

/**
 * Redirects gui render states away from the vanilla {@code GuiRenderState} and into a glow group while a
 * {@link GlowRenderer#addShape} lambda is running.
 * <p>
 * Every meteor ui renderer already funnels its work through {@code GuiRenderState.addSimpleElement} /
 * {@code addPreparedTextElement}, so intercepting those two entry points is enough to reroute any element -
 * arc, rotating gradient rect, msdf text - into the glow atlas without duplicating a single shader or pipeline.
 * <p>
 * Nested scopes merge into the outermost one, so a shape lambda may freely call helpers that themselves use
 * {@code addShape} without producing extra textures.
 */
public final class GlowCaptureScope {

    private static List<SimpleGuiElementRenderState> sink;
    private static int depth;

    private GlowCaptureScope() {}

    public static boolean active() {
        return depth > 0;
    }

    static void begin(List<SimpleGuiElementRenderState> target) {
        if (depth++ == 0) {
            // Cross-submit batchers (msdf text, images) keep their render state open between submits, so an
            // already open batch would happily absorb captured glyphs and end up in the atlas instead of on screen.
            Render2D.batchBarrier();
            sink = target;
        }
    }

    static void end() {
        if (depth > 0 && --depth == 0) {
            sink = null;
            Render2D.batchBarrier();
        }
    }

    /**
     * @return true when the state was swallowed by the active scope and must not reach the vanilla gui state.
     */
    public static boolean intercept(SimpleGuiElementRenderState state) {
        List<SimpleGuiElementRenderState> target = sink;
        if (target == null || state == null) {
            return false;
        }
        target.add(state);
        return true;
    }

}
