package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import meteordevelopment.meteorclient.gui.renderer.packer.TextureRegion;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.state.QuadColorState;
import meteordevelopment.meteorclient.utils.render.state.QuadRadiusState;
import net.minecraft.client.gl.DynamicUniformStorage;
import net.minecraft.client.gl.GpuSampler;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.HashMap;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Renderer2D {
    
    public static Renderer2D COLOR;
    public static Renderer2D TEXTURE;
    
    private final KawaseBlur blur = new KawaseBlur();
    
    private final boolean textured;
    
    public final MeshBuilder triangles;
    public final MeshBuilder lines;
    
    public Renderer2D(boolean textured) {
        this.textured = textured;
        
        triangles = new MeshBuilder(textured ? MeteorRenderPipelines.UI_TEXTURED : MeteorRenderPipelines.UI_COLORED);
        lines = new MeshBuilder(MeteorRenderPipelines.UI_COLORED_LINES);
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
    }
    
    public void end() {
        triangles.end();
        lines.end();
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
    }
    
    // Tris
    
    public void triangle(double x1, double y1, double x2, double y2, double x3, double y3, Color color) {
        triangles.ensureTriCapacity();
        
        triangles.triangle(
            triangles.pos(x1, y1).color(color).next(),
            triangles.pos(x2, y2).color(color).next(),
            triangles.pos(x3, y3).color(color).next()
        );
    }
    
    // Lines
    
    public void line(double x1, double y1, double x2, double y2, Color color) {
        lines.ensureLineCapacity();
        
        lines.line(
            lines.pos(x1, y1).color(color).next(),
            lines.pos(x2, y2).color(color).next()
        );
    }
    
    public void boxLines(double x, double y, double width, double height, Color color) {
        lines.ensureCapacity(4, 8);
        
        int i1 = lines.pos(x, y).color(color).next();
        int i2 = lines.pos(x, y + height).color(color).next();
        int i3 = lines.pos(x + width, y + height).color(color).next();
        int i4 = lines.pos(x + width, y).color(color).next();
        
        lines.line(i1, i2);
        lines.line(i2, i3);
        lines.line(i3, i4);
        lines.line(i4, i1);
    }
    
    // Quads
    
    public void quad(double x, double y, double width, double height, QuadColorState color) {
        triangles.ensureQuadCapacity();
        
        triangles.quad(
            triangles.pos(x, y).color(color.colorTopLeft()).next(),
            triangles.pos(x, y + height).color(color.colorBottomLeft()).next(),
            triangles.pos(x + width, y + height).color(color.colorBottomRight()).next(),
            triangles.pos(x + width, y).color(color.colorTopRight()).next()
        );
    }
    
    public void quad(double x, double y, double width, double height, Color color) {
        quad(x, y, width, height, QuadColorState.of(color));
    }
    
    // Textured quads
    
    public void texQuad(double x, double y, double width, double height, Color color) {
        triangles.ensureQuadCapacity();
        
        triangles.quad(
            triangles.pos(x, y).pos(0, 0).color(color).next(),
            triangles.pos(x, y + height).pos(0, 1).color(color).next(),
            triangles.pos(x + width, y + height).pos(1, 1).color(color).next(),
            triangles.pos(x + width, y).pos(1, 0).color(color).next()
        );
    }
    
    public void texQuad(double x, double y, double width, double height, TextureRegion texture, Color color) {
        triangles.ensureQuadCapacity();
        
        triangles.quad(
            triangles.pos(x, y).pos(texture.x1, texture.y1).color(color).next(),
            triangles.pos(x, y + height).pos(texture.x1, texture.y2).color(color).next(),
            triangles.pos(x + width, y + height).pos(texture.x2, texture.y2).color(color).next(),
            triangles.pos(x + width, y).pos(texture.x2, texture.y1).color(color).next()
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
        int i1 = triangles.pos(_x1, _y1).pos(texX1, texY1).color(color).next();
        
        double _x2 = ((x - oX) * cos) - ((y + height - oY) * sin) + oX;
        double _y2 = ((y + height - oY) * cos) + ((x - oX) * sin) + oY;
        int i2 = triangles.pos(_x2, _y2).pos(texX1, texY2).color(color).next();
        
        double _x3 = ((x + width - oX) * cos) - ((y + height - oY) * sin) + oX;
        double _y3 = ((y + height - oY) * cos) + ((x + width - oX) * sin) + oY;
        int i3 = triangles.pos(_x3, _y3).pos(texX2, texY2).color(color).next();
        
        double _x4 = ((x + width - oX) * cos) - ((y - oY) * sin) + oX;
        double _y4 = ((y - oY) * cos) + ((x + width - oX) * sin) + oY;
        int i4 = triangles.pos(_x4, _y4).pos(texX2, texY1).color(color).next();
        
        triangles.quad(i1, i2, i3, i4);
    }
    
    public void texQuad(double x, double y, double width, double height, double rotation, TextureRegion region, Color color) {
        texQuad(x, y, width, height, rotation, region.x1, region.y1, region.x2, region.y2, color);
    }
    
    // Rectangle
    
    private static final FixedUniformStorage<RectangleUniformData> RECTANGLE_UNIFORM_STORAGE = new FixedUniformStorage<>(
        "RectangleUBO",
        new Std140SizeCalculator()
            .putVec2() // size
            .putVec4() // radius
            .putFloat() // smoothness
            .get(),
        16
    );
    
    private record RectangleUniformData(float width, float height, Vector4f radius, float smoothness) implements DynamicUniformStorage.Uploadable {
        
        @Override
        public void write(ByteBuffer buf) {
            Std140Builder.intoBuffer(buf)
                .putVec2(width, height)
                .putVec4(radius)
                .putFloat(smoothness);
        }
        
    }
    
    public void rectangle(double x, double y, double width, double height, QuadColorState color, QuadRadiusState radius, double smoothness) {
        MeshBuilder mesh = new MeshBuilder(MeteorRenderPipelines.UI_RECTANGLE);
        
        mesh.begin();
        
        mesh.ensureQuadCapacity();
        
        mesh.quad(
            mesh.pos(x, y)
                .texture(0, 0)
                .color(color.colorTopLeft())
                .next(),
            mesh.pos(x, y + height)
                .texture(0, 1)
                .color(color.colorBottomLeft())
                .next(),
            mesh.pos(x + width, y + height)
                .texture(1, 1)
                .color(color.colorBottomRight())
                .next(),
            mesh.pos(x + width, y)
                .texture(1, 0)
                .color(color.colorTopRight())
                .next()
        );
        
        mesh.end();
        
        RECTANGLE_UNIFORM_STORAGE.clear();
        
        MeshRenderer.begin()
            .attachments(mc.getFramebuffer())
            .pipeline(MeteorRenderPipelines.UI_RECTANGLE)
            .mesh(mesh)
            .uniform("RectangleData", RECTANGLE_UNIFORM_STORAGE.write(
                new RectangleUniformData(
                    (float) width,
                    (float) height,
                    radius.getVec4f(),
                    (float) smoothness
                )
            ))
            .end();
    }
    
    // Shadow
    
    public void shadow(double x, double y, double width, double height, QuadColorState color, QuadRadiusState radius, double smoothness, int passes, double offset) {
        MeshBuilder mesh = new MeshBuilder(MeteorRenderPipelines.UI_RECTANGLE);
        
        mesh.begin();
        
        mesh.ensureQuadCapacity();
        
        mesh.quad(
            mesh.pos(x, y)
                .texture(0, 0)
                .color(color.colorTopLeft())
                .next(),
            mesh.pos(x, y + height)
                .texture(0, 1)
                .color(color.colorBottomLeft())
                .next(),
            mesh.pos(x + width, y + height)
                .texture(1, 1)
                .color(color.colorBottomRight())
                .next(),
            mesh.pos(x + width, y)
                .texture(1, 0)
                .color(color.colorTopRight())
                .next()
        );
        
        mesh.end();
        
        blur.ensure(
            mc.getFramebuffer().textureWidth,
            mc.getFramebuffer().textureHeight
        );
        
        RECTANGLE_UNIFORM_STORAGE.clear();
        
        MeshRenderer.begin()
            .attachments(blur.getSource(), null)
            .pipeline(MeteorRenderPipelines.UI_RECTANGLE)
            .clearColor(Color.CLEAR)
            .fullscreen()
            .mesh(mesh)
            .uniform("RectangleData", RECTANGLE_UNIFORM_STORAGE.write(
                new RectangleUniformData(
                    (float) width,
                    (float) height,
                    radius.getVec4f(),
                    (float) smoothness
                )
            ))
            .end();
        
        GpuTextureView blurred = blur.blur(passes, (float) offset);
        
        MeshRenderer.begin()
            .attachments(mc.getFramebuffer())
            .pipeline(MeteorRenderPipelines.PASSTHROUGH)
            .fullscreen()
            .sampler("u_Texture", blurred, RenderSystem.getSamplerCache().get(FilterMode.LINEAR))
            .end();
    }
    
    private final HashMap<Integer, GpuTextureView> shadowCache = new HashMap<>();
    
//    public void shadow2(double x, double y, double width, double height, QuadColorState color, QuadRadiusState radius, double smoothness, int passes, double offset) {
//
//        // 👉 padding под блюр (очень важно)
//        double pad = passes * offset * 2.0;
//
//        int texW = (int) Math.ceil(width + pad * 2);
//        int texH = (int) Math.ceil(height + pad * 2);
//
//        int id = Objects.hash(texW, texH, radius, smoothness, passes, offset);
//
//        GpuTextureView blurred;
//
//        shadowCache.clear(); // TEST
//
//        if (shadowCache.containsKey(id)) {
//            blurred = shadowCache.get(id);
//        } else {
//            // 🔥 1. создаём меш В ЛОКАЛЬНЫХ координатах (0..size)
//            MeshBuilder mesh = new MeshBuilder(MeteorRenderPipelines.UI_RECTANGLE);
//
//            mesh.begin();
//            mesh.ensureQuadCapacity();
//
//            double x0 = pad + x;
//            double y0 = pad + y;
//            double x1 = pad + width + x;
//            double y1 = pad + height + y;
//
//            mesh.quad(
//                mesh.pos(x, y)
//                    .texture(0, 0)
//                    .color(Color.WHITE)
//                    .next(),
//                mesh.pos(x, y + height)
//                    .texture(0, 1)
//                    .color(Color.WHITE)
//                    .next(),
//                mesh.pos(x + width, y + height)
//                    .texture(1, 1)
//                    .color(Color.WHITE)
//                    .next(),
//                mesh.pos(x + width, y)
//                    .texture(1, 0)
//                    .color(Color.WHITE)
//                    .next()
//            );
//
//            mesh.end();
//
//            // 🔥 2. создаём blur под РАЗМЕР ТЕКСТУРЫ, а не экрана
////            blur.ensure(
////                (int) width,
////                (int) height
////            );
//
//            RECTANGLE_UNIFORM_STORAGE.clear();
//
//
////            blurred = RenderSystem.getDevice().createTexture(
////                "kawase",
////                15,
////                TextureFormat.RGBA8,
////                (int) width,
////                (int) height,
////                1,
////                1
////            );
//
//            // 🔥 3. рендерим БЕЗ fullscreen()
//            MeshRenderer.begin()
//                //.attachments(blurred, null)
//                .pipeline(MeteorRenderPipelines.UI_RECTANGLE)
//                .clearColor(Color.CLEAR)
//                .mesh(mesh)
//                .uniform("RectangleData", RECTANGLE_UNIFORM_STORAGE.write(
//                    new RectangleUniformData(
//                        (float) width,
//                        (float) height,
//                        radius.getVec4f(),
//                        (float) smoothness
//                    )
//                ))
//                .end();
//
//            // 🔥 4. блюрим
//
//            // blurred = blur.blur(passes, (float) offset);
//
//            //shadowCache.put(id, blurred);
//        }
//
////        MeshRenderer.begin()
////            .attachments(mc.getFramebuffer())
////            .pipeline(MeteorRenderPipelines.PASSTHROUGH)
////            .sampler("u_Texture", blurred, RenderSystem.getSamplerCache().get(FilterMode.LINEAR))
////            .end();
//
//        // 🔥 5. выводим с учётом padding
//        // Texture
//
//        MeshBuilder textureMesh = new MeshBuilder(MeteorRenderPipelines.UI_TEXTURE);
//
//        textureMesh.begin();
//
//        textureMesh.ensureQuadCapacity();
//
//        textureMesh.quad(
//            textureMesh.pos(x, y)
//                .texture(0, 0)
//                .color(color.colorTopLeft())
//                .next(),
//            textureMesh.pos(x, y + height)
//                .texture(0, 1)
//                .color(color.colorBottomLeft())
//                .next(),
//            textureMesh.pos(x + width, y + height)
//                .texture(1, 1)
//                .color(color.colorBottomRight())
//                .next(),
//            textureMesh.pos(x + width, y)
//                .texture(1, 0)
//                .color(color.colorTopRight())
//                .next()
//        );
//
//        textureMesh.end();
//
//        RECTANGLE_UNIFORM_STORAGE.clear();
//
//        MeshRenderer.begin()
//            .attachments(mc.getFramebuffer())
//            .pipeline(MeteorRenderPipelines.UI_TEXTURE)
//            .mesh(textureMesh)
//            //.sampler("u_Texture", blurred, RenderSystem.getSamplerCache().get(FilterMode.LINEAR))
//            .uniform("TextureData", RECTANGLE_UNIFORM_STORAGE.write(
//                new RectangleUniformData(
//                    (float) width,
//                    (float) height,
//                    radius.getVec4f(),
//                    (float) smoothness
//                )
//            ))
//            .end();
//    }
    
    public void blur(double x, double y, double width, double height, QuadColorState color, QuadRadiusState radius, double smoothness, int passes, double offset) {
        int fbWidth = mc.getFramebuffer().textureWidth;
        int fbHeight = mc.getFramebuffer().textureHeight;
        
        // Y inversion
        double invY = fbHeight - y - height;
        
        // UV
        float u0 = (float) (x / fbWidth);
        float v0 = (float) ((invY + height) / fbHeight);
        float u1 = (float) ((x + width) / fbWidth);
        float v1 = (float) (invY / fbHeight);
        
        // Mesh
        MeshBuilder mesh = new MeshBuilder(MeteorRenderPipelines.BLIT);

        mesh.begin();
        
        mesh.ensureQuadCapacity();
        
        mesh.quad(
            mesh.pos(-1, -1).texture(u0, v0).next(),
            mesh.pos(-1,  1).texture(u0, v1).next(),
            mesh.pos( 1,  1).texture(u1, v1).next(),
            mesh.pos( 1, -1).texture(u1, v0).next()
        );

        mesh.end();
        
        blur.ensure(
            (int) width,
            (int) height
        );
        
        MeshRenderer.begin()
            .attachments(blur.getSource(), null)
            .pipeline(MeteorRenderPipelines.BLIT)
            .clearColor(Color.BLACK)
            .mesh(mesh)
            .sampler(
                "u_Texture",
                mc.getFramebuffer().getColorAttachmentView(),
                RenderSystem.getSamplerCache().get(FilterMode.LINEAR)
            )
            .end();
        
        GpuTextureView blurred = blur.blur(passes, (float) offset);
        
        texture(
            x,
            y,
            width,
            height,
            color,
            radius,
            smoothness,
            blurred,
            RenderSystem.getSamplerCache().get(FilterMode.LINEAR)
        );
    }
    
    public void texture(double x, double y, double width, double height, QuadColorState color, QuadRadiusState radius, double smoothness, GpuTextureView textureView, GpuSampler sampler) {
        MeshBuilder mesh = new MeshBuilder(MeteorRenderPipelines.UI_TEXTURE);
        
        mesh.begin();
        
        mesh.ensureQuadCapacity();
        
        mesh.quad(
            mesh.pos(x, y)
                .texture(0, 0)
                .color(color.colorTopLeft())
                .next(),
            mesh.pos(x, y + height)
                .texture(0, 1)
                .color(color.colorBottomLeft())
                .next(),
            mesh.pos(x + width, y + height)
                .texture(1, 1)
                .color(color.colorBottomRight())
                .next(),
            mesh.pos(x + width, y)
                .texture(1, 0)
                .color(color.colorTopRight())
                .next()
        );
        
        mesh.end();
        
        RECTANGLE_UNIFORM_STORAGE.clear();
        
        MeshRenderer.begin()
            .attachments(mc.getFramebuffer())
            .pipeline(MeteorRenderPipelines.UI_TEXTURE)
            .mesh(mesh)
            .sampler("u_Texture", textureView, sampler)
            .uniform("TextureData", RECTANGLE_UNIFORM_STORAGE.write(
                new RectangleUniformData(
                    (float) width,
                    (float) height,
                    radius.getVec4f(),
                    (float) smoothness
                )
            ))
            .end();
    }
    
}
