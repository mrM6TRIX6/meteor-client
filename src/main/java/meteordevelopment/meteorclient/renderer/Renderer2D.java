package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import meteordevelopment.meteorclient.gui.renderer.packer.TextureRegion;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.state.QuadColorState;
import meteordevelopment.meteorclient.utils.render.state.QuadRadiusState;
import net.minecraft.client.gl.GpuSampler;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Renderer2D {
    
    public static Renderer2D COLOR;
    public static Renderer2D TEXTURE;
    
    private final KawaseBlur blur = new KawaseBlur();
    private final boolean textured;
    
    public final MeshBuilder triangles;
    public final MeshBuilder lines;
    public final MeshBuilder rectangleMesh;
    
    public final List<BlurTask> blurTasks = new ArrayList<>();
    
    public Renderer2D(boolean textured) {
        this.textured = textured;
        
        triangles = new MeshBuilder(textured ? MeteorRenderPipelines.UI_TEXTURED : MeteorRenderPipelines.UI_COLORED);
        lines = new MeshBuilder(MeteorRenderPipelines.UI_COLORED_LINES);
        rectangleMesh = new MeshBuilder(MeteorRenderPipelines.UI_RECTANGLE);
    }
    
    @PreInit
    public static void init() {
        COLOR = new Renderer2D(false);
        TEXTURE = new Renderer2D(true);
    }
    
    public void setAlpha(double alpha) {
        triangles.alpha = alpha;
    }
    
    public void begin() {
        triangles.begin();
        lines.begin();
        rectangleMesh.begin();
    }
    
    public void end() {
        triangles.end();
        lines.end();
        rectangleMesh.end();
    }
    
    public void render() {
        render(null, null, null);
    }
    
    public void render(GpuTextureView textureView, GpuSampler sampler) {
        if (!textured) {
            throw new IllegalStateException("Tried to render with a texture with a non-textured Renderer2D");
        }
        
        render("u_Texture", textureView, sampler);
    }
    
    public void render(String samplerName, GpuTextureView samplerView, GpuSampler sampler) {
        if (lines.isBuilding()) {
            lines.end();
        }
        if (triangles.isBuilding()) {
            triangles.end();
        }
        if (rectangleMesh.isBuilding()) {
            rectangleMesh.end();
        }
        
        // Render blur tasks firstly
        
        for (BlurTask task : blurTasks) {
            blur.ensure(
                mc.getFramebuffer().textureWidth,
                mc.getFramebuffer().textureHeight
            );
            
            MeshRenderer.begin()
                .attachments(blur.getSource(), null)
                .pipeline(MeteorRenderPipelines.UI_RECTANGLE)
                .clearColor(Color.CLEAR)
                .fullscreen()
                .mesh(task.mesh)
                .end();
            
            GpuTextureView blurred = blur.blur(task.passes, (float) task.offset);
            
            MeshRenderer.begin()
                .attachments(mc.getFramebuffer())
                .pipeline(MeteorRenderPipelines.BLUR_PASSTHROUGH)
                .fullscreen()
                .sampler("u_Texture", blurred, RenderSystem.getSamplerCache().get(FilterMode.LINEAR))
                .end();
        }
        
        blurTasks.clear();
        
        MeshRenderer.begin()
            .attachments(mc.getFramebuffer())
            .pipeline(MeteorRenderPipelines.UI_COLORED_LINES)
            .mesh(lines)
            .end();
        
        MeshRenderer.begin()
            .attachments(mc.getFramebuffer())
            .pipeline(textured ? MeteorRenderPipelines.UI_TEXTURED : MeteorRenderPipelines.UI_COLORED)
            .mesh(triangles)
            .sampler(samplerName, samplerView, sampler)
            .end();
        
        MeshRenderer.begin()
            .attachments(mc.getFramebuffer())
            .pipeline(MeteorRenderPipelines.UI_RECTANGLE)
            .mesh(rectangleMesh)
            .end();
    }
    
    // Tris
    
    public void triangle(double x1, double y1, double x2, double y2, double x3, double y3, Color color) {
        triangles.ensureTriCapacity();
        
        triangles.triangle(
            triangles.vec2(x1, y1).color(color).next(),
            triangles.vec2(x2, y2).color(color).next(),
            triangles.vec2(x3, y3).color(color).next()
        );
    }
    
    // Lines
    
    public void line(double x1, double y1, double x2, double y2, Color color) {
        lines.ensureLineCapacity();
        
        lines.line(
            lines.vec2(x1, y1).color(color).next(),
            lines.vec2(x2, y2).color(color).next()
        );
    }
    
    public void boxLines(double x, double y, double width, double height, Color color) {
        lines.ensureCapacity(4, 8);
        
        int i1 = lines.vec2(x, y).color(color).next();
        int i2 = lines.vec2(x, y + height).color(color).next();
        int i3 = lines.vec2(x + width, y + height).color(color).next();
        int i4 = lines.vec2(x + width, y).color(color).next();
        
        lines.line(i1, i2);
        lines.line(i2, i3);
        lines.line(i3, i4);
        lines.line(i4, i1);
    }
    
    // Quads
    
    public void quad(double x, double y, double width, double height, QuadColorState color) {
        triangles.ensureQuadCapacity();
        
        triangles.quad(
            triangles.vec2(x, y).color(color.colorTopLeft()).next(),
            triangles.vec2(x, y + height).color(color.colorBottomLeft()).next(),
            triangles.vec2(x + width, y + height).color(color.colorBottomRight()).next(),
            triangles.vec2(x + width, y).color(color.colorTopRight()).next()
        );
    }
    
    public void quad(double x, double y, double width, double height, Color color) {
        quad(x, y, width, height, QuadColorState.of(color));
    }
    
    // Textured quads
    
    public void texQuad(double x, double y, double width, double height, Color color) {
        triangles.ensureQuadCapacity();
        
        triangles.quad(
            triangles.vec2(x, y).vec2(0, 0).color(color).next(),
            triangles.vec2(x, y + height).vec2(0, 1).color(color).next(),
            triangles.vec2(x + width, y + height).vec2(1, 1).color(color).next(),
            triangles.vec2(x + width, y).vec2(1, 0).color(color).next()
        );
    }
    
    public void texQuad(double x, double y, double width, double height, TextureRegion texture, Color color) {
        triangles.ensureQuadCapacity();
        
        triangles.quad(
            triangles.vec2(x, y).vec2(texture.x1, texture.y1).color(color).next(),
            triangles.vec2(x, y + height).vec2(texture.x1, texture.y2).color(color).next(),
            triangles.vec2(x + width, y + height).vec2(texture.x2, texture.y2).color(color).next(),
            triangles.vec2(x + width, y).vec2(texture.x2, texture.y1).color(color).next()
        );
    }
    
    public void texQuad(double x, double y, double width, double height, double rotation, double texX1, double texY1, double texX2, double texY2, Color color) {
        triangles.ensureQuadCapacity();
        
        double rad = Math.toRadians(rotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        
        double oX = x + width / 2;
        double oY = y + height / 2;
        
        double _x1 = ((x - oX) * cos) - ((y - oY) * sin) + oX;
        double _y1 = ((y - oY) * cos) + ((x - oX) * sin) + oY;
        int i1 = triangles.vec2(_x1, _y1).vec2(texX1, texY1).color(color).next();
        
        double _x2 = ((x - oX) * cos) - ((y + height - oY) * sin) + oX;
        double _y2 = ((y + height - oY) * cos) + ((x - oX) * sin) + oY;
        int i2 = triangles.vec2(_x2, _y2).vec2(texX1, texY2).color(color).next();
        
        double _x3 = ((x + width - oX) * cos) - ((y + height - oY) * sin) + oX;
        double _y3 = ((y + height - oY) * cos) + ((x + width - oX) * sin) + oY;
        int i3 = triangles.vec2(_x3, _y3).vec2(texX2, texY2).color(color).next();
        
        double _x4 = ((x + width - oX) * cos) - ((y - oY) * sin) + oX;
        double _y4 = ((y - oY) * cos) + ((x + width - oX) * sin) + oY;
        int i4 = triangles.vec2(_x4, _y4).vec2(texX2, texY1).color(color).next();
        
        triangles.quad(i1, i2, i3, i4);
    }
    
    public void texQuad(double x, double y, double width, double height, double rotation, TextureRegion region, Color color) {
        texQuad(x, y, width, height, rotation, region.x1, region.y1, region.x2, region.y2, color);
    }
    
    // Test
    
    public void rectangle(double x, double y, double width, double height, QuadColorState color, QuadRadiusState radius, double smoothness) {
        rectangleMesh.ensureQuadCapacity();
        
        rectangleMesh.quad(
            rectangleMesh.vec2(x, y)
                .color(color.colorTopLeft())
                .uv(0, 0)
                .size(width, height)
                .radius(
                    radius.radiusTopLeft(),
                    radius.radiusBottomLeft(),
                    radius.radiusBottomRight(),
                    radius.radiusTopRight()
                )
                .smoothness(smoothness)
                .next(),
            
            rectangleMesh.vec2(x + width, y)
                .color(color.colorBottomLeft())
                .uv(0, 1)
                .size(width, height)
                .radius(
                    radius.radiusTopLeft(),
                    radius.radiusBottomLeft(),
                    radius.radiusBottomRight(),
                    radius.radiusTopRight()
                )
                .smoothness(smoothness)
                .next(),
            
            rectangleMesh.vec2(x + width, y + height)
                .color(color.colorBottomRight())
                .uv(1, 1)
                .size(width, height)
                .radius(
                    radius.radiusTopLeft(),
                    radius.radiusBottomLeft(),
                    radius.radiusBottomRight(),
                    radius.radiusTopRight()
                )
                .smoothness(smoothness)
                .next(),
            
            rectangleMesh.vec2(x, y + height)
                .color(color.colorTopRight())
                .uv(1, 0)
                .size(width, height)
                .radius(
                    radius.radiusTopLeft(),
                    radius.radiusBottomLeft(),
                    radius.radiusBottomRight(),
                    radius.radiusTopRight()
                )
                .smoothness(smoothness)
                .next()
        );
    }
    
    public void blurredRectangle(double x, double y, double width, double height, QuadColorState color, QuadRadiusState radius, double smoothness, int passes, double offset) {
        BlurTask task = new BlurTask(x, y, width, height, passes, offset, new MeshBuilder(MeteorRenderPipelines.UI_RECTANGLE));
        
        task.mesh.begin();
        
        task.mesh.ensureQuadCapacity();
        
        task.mesh.quad(
            task.mesh.vec2(x, y)
                .color(color.colorTopLeft())
                .uv(0,0)
                .size(width, height)
                .radius(
                    radius.radiusTopLeft(),
                    radius.radiusBottomLeft(),
                    radius.radiusBottomRight(),
                    radius.radiusTopRight()
                )
                .smoothness(smoothness)
                .next(),
            
            task.mesh.vec2(x, y + height)
                .color(color.colorBottomLeft())
                .uv(0,1)
                .size(width,height)
                .radius(
                    radius.radiusTopLeft(),
                    radius.radiusBottomLeft(),
                    radius.radiusBottomRight(),
                    radius.radiusTopRight()
                )
                .smoothness(smoothness)
                .next(),
            
            task.mesh.vec2(x + width, y + height)
                .color(color.colorBottomRight())
                .uv(1,1)
                .size(width,height)
                .radius(
                    radius.radiusTopLeft(),
                    radius.radiusBottomLeft(),
                    radius.radiusBottomRight(),
                    radius.radiusTopRight()
                )
                .smoothness(smoothness)
                .next(),
            
            task.mesh.vec2(x + width, y)
                .color(color.colorTopRight())
                .uv(1,0)
                .size(width,height)
                .radius(
                    radius.radiusTopLeft(),
                    radius.radiusBottomLeft(),
                    radius.radiusBottomRight(),
                    radius.radiusTopRight()
                )
                .smoothness(smoothness)
                .next()
        );
        
        task.mesh.end();
        
        blurTasks.add(task);
    }
    
     private record BlurTask(double x, double y, double width, double height, int passes, double offset, MeshBuilder mesh) {}
    
}
