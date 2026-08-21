package meteordevelopment.meteorclient.utils.render.ui.glow;

import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import org.joml.Matrix3x2f;

import java.util.ArrayList;
import java.util.List;

final class GlowGroup {

    static final int PROCEDURAL = 0;
    static final int CAPTURED = 1;

    final List<SimpleGuiElementRenderState> states = new ArrayList<>(8);
    final Matrix3x2f pose = new Matrix3x2f();
    final GlowCapture capture = new GlowCapture();
    
    int kind;
    BuiltGlow glow;
    float contentX;
    float contentY;
    float contentWidth;
    float contentHeight;
    float pad;
    float radius;
    float expand;
    float resolution = 1.0f;
    float intensity = 1.0f;
    float alpha = 1.0f;
    boolean cutout = true;
    int tileX;
    int tileY;
    int tileWidth;
    int tileHeight;
    int bucket;
    int sourceQuad = -1;
    int patchQuad = -1;
    long shapeParamsOffset = -1L;
    long transformsOffset = -1L;
    int replayFrom;
    int replayTo;

    void reset() {
        states.clear();
        pose.identity();
        capture.reset();
        kind = PROCEDURAL;
        glow = null;
        contentX = 0.0f;
        contentY = 0.0f;
        contentWidth = 0.0f;
        contentHeight = 0.0f;
        pad = 0.0f;
        radius = 0.0f;
        expand = 0.0f;
        resolution = 1.0f;
        intensity = 1.0f;
        alpha = 1.0f;
        cutout = true;
        tileX = 0;
        tileY = 0;
        tileWidth = 0;
        tileHeight = 0;
        bucket = 0;
        sourceQuad = -1;
        patchQuad = -1;
        shapeParamsOffset = -1L;
        transformsOffset = -1L;
        replayFrom = 0;
        replayTo = 0;
    }

    boolean placed() {
        return tileWidth > 0 && tileHeight > 0;
    }

}
