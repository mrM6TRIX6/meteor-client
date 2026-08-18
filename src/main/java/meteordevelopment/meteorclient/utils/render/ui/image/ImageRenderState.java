package meteordevelopment.meteorclient.utils.render.ui.image;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

import java.util.ArrayList;
import java.util.List;

final class ImageRenderState implements SimpleGuiElementRenderState {
    
    private final Matrix3x2f pose;
    private final ImageTexture texture;
    private final ScreenRect scissorArea;
    private final List<ImageQuad> images = new ArrayList<>(16);
    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxX = -Float.MAX_VALUE;
    private float maxY = -Float.MAX_VALUE;
    
    ImageRenderState(Matrix3x2f pose, ImageTexture texture, ScreenRect scissorArea) {
        this.pose = pose;
        this.texture = texture;
        this.scissorArea = scissorArea;
    }
    
    void add(ImageQuad image) {
        images.add(image);
        includeBounds(image, image.x(), image.y());
        includeBounds(image, image.x(), image.y() + image.height());
        includeBounds(image, image.x() + image.width(), image.y() + image.height());
        includeBounds(image, image.x() + image.width(), image.y());
    }
    
    @Override
    public void setupVertices(VertexConsumer consumer) {
        for (ImageQuad image : images) {
            int batchIndex = ImageRenderer.getInstance().reserve(image);
            if (batchIndex < 0) {
                continue;
            }
            
            float x0 = image.x();
            float y0 = image.y();
            float x1 = image.x() + image.width();
            float y1 = image.y() + image.height();
            
            vertex(consumer, image, x0, y0, 0.0f, 0.0f, image.colorTopLeft(), batchIndex);
            vertex(consumer, image, x0, y1, 0.0f, 1.0f, image.colorBottomLeft(), batchIndex);
            vertex(consumer, image, x1, y1, 1.0f, 1.0f, image.colorBottomRight(), batchIndex);
            vertex(consumer, image, x1, y0, 1.0f, 0.0f, image.colorTopRight(), batchIndex);
        }
    }
    
    private void vertex(VertexConsumer consumer, ImageQuad image, float x, float y, float u, float v, int color, int batchIndex) {
        consumer.vertex(pose, rotateX(image, x, y), rotateY(image, x, y))
            .texture(u, v)
            .color(color)
            .lineWidth((float) (batchIndex + 1));
    }
    
    private void includeBounds(ImageQuad image, float x, float y) {
        float rotatedX = rotateX(image, x, y);
        float rotatedY = rotateY(image, x, y);
        minX = Math.min(minX, rotatedX);
        minY = Math.min(minY, rotatedY);
        maxX = Math.max(maxX, rotatedX);
        maxY = Math.max(maxY, rotatedY);
    }
    
    private static float rotateX(ImageQuad image, float x, float y) {
        float rotation = image.rotationDegrees();
        if (rotation == 0.0f) {
            return x;
        }
        float dx = x - image.rotationOriginX();
        float dy = y - image.rotationOriginY();
        return image.rotationOriginX() + dx * image.rotationCos() - dy * image.rotationSin();
    }
    
    private static float rotateY(ImageQuad image, float x, float y) {
        float rotation = image.rotationDegrees();
        if (rotation == 0.0f) {
            return y;
        }
        float dx = x - image.rotationOriginX();
        float dy = y - image.rotationOriginY();
        return image.rotationOriginY() + dx * image.rotationSin() + dy * image.rotationCos();
    }
    
    @Override
    public RenderPipeline pipeline() {
        return ImageRenderer.IMAGE_PIPELINE;
    }
    
    @Override
    public TextureSetup textureSetup() {
        return texture.setup();
    }
    
    @Override
    public ScreenRect scissorArea() {
        return scissorArea;
    }
    
    @Override
    public ScreenRect bounds() {
        if (images.isEmpty()) {
            return new ScreenRect(0, 0, 1, 1);
        }
        
        int x = (int) Math.floor(minX);
        int y = (int) Math.floor(minY);
        int width = Math.max(1, (int) Math.ceil(maxX - minX));
        int height = Math.max(1, (int) Math.ceil(maxY - minY));
        ScreenRect transformedBounds = new ScreenRect(x, y, width, height).transformEachVertex(pose);
        return scissorArea == null ? transformedBounds : scissorArea.intersection(transformedBounds);
    }
    
}
