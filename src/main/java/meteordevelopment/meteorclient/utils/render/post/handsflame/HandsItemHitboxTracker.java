package meteordevelopment.meteorclient.utils.render.post.handsflame;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.joml.Vector4f;

public final class HandsItemHitboxTracker {
    
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Hitbox LEFT = new Hitbox();
    private static final Hitbox RIGHT = new Hitbox();
    private static boolean hasProjection;
    
    private HandsItemHitboxTracker() {}
    
    public static boolean contains(boolean bl, double d, double d2) {
        return (bl ? LEFT : RIGHT).contains(d, d2);
    }
    
    private static void projectPoint(Vector3fc vector3fc, Matrix4fc matrix4fc, Matrix4fc matrix4fc2, float f, float f2, PointCloud pointCloud) {
        Vector4f vector4f = new Vector4f(vector3fc.x(), vector3fc.y(), vector3fc.z(), 1.0f);
        vector4f.mul(matrix4fc);
        vector4f.mul(matrix4fc2);
        if (Math.abs(vector4f.w()) <= 1.0E-5f) {
            return;
        }
        float f3 = vector4f.x() / vector4f.w();
        float f4 = vector4f.y() / vector4f.w();
        if (!Float.isFinite(f3) || !Float.isFinite(f4)) {
            return;
        }
        pointCloud.add((f3 * 0.5f + 0.5f) * f, (0.5f - f4 * 0.5f) * f2);
    }
    
    private static boolean isFirstPerson(ItemDisplayContext itemDisplayContext) {
        return itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || itemDisplayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }
    
    public static float centerX(boolean bl, float f) {
        Hitbox hitbox = bl ? LEFT : RIGHT;
        return hitbox.isFresh() ? hitbox.centerX : f;
    }
    
    public static float centerY(boolean bl, float f) {
        Hitbox hitbox = bl ? LEFT : RIGHT;
        return hitbox.isFresh() ? hitbox.centerY : f;
    }
    
    public static void captureProjection(Matrix4fc matrix4fc) {
        if (matrix4fc == null) {
            hasProjection = false;
            return;
        }
        PROJECTION.set(matrix4fc);
        hasProjection = true;
    }
    
    public static void capture(ItemDisplayContext itemDisplayContext, MatrixStack matrixStack, ItemRenderState itemRenderState) {
        if (!hasProjection || matrixStack == null || itemRenderState == null || !HandsItemHitboxTracker.isFirstPerson(itemDisplayContext)) {
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.getWindow() == null) {
            return;
        }
        PointCloud pointCloud = new PointCloud();
        Matrix4f matrix4f = new Matrix4f((Matrix4fc) matrixStack.peek().getPositionMatrix());
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc) PROJECTION);
        float f = minecraftClient.getWindow().getScaledWidth();
        float f2 = minecraftClient.getWindow().getScaledHeight();
        itemRenderState.load(vector3fc -> HandsItemHitboxTracker.projectPoint(vector3fc, (Matrix4fc) matrix4f, (Matrix4fc) matrix4f2, f, f2, pointCloud));
        if (pointCloud.count < 2) {
            return;
        }
        (itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? LEFT : RIGHT).update(pointCloud);
    }
    
    public static boolean isFresh(boolean bl) {
        return (bl ? LEFT : RIGHT).isFresh();
    }
    
    public static final class Hitbox {
        
        private float centerX;
        private float centerY;
        private float axisX = 1.0f;
        private float axisY;
        private float halfA;
        private float halfB;
        private long updatedNanos;
        
        private Hitbox() {
        }
        
        private void update(PointCloud pointCloud) {
            float f;
            float f2;
            float f3 = 0.0f;
            float f4 = 0.0f;
            
            for (int i = 0; i < pointCloud.count; ++i) {
                f3 += pointCloud.x[i];
                f4 += pointCloud.y[i];
            }
            
            f3 /= (float) pointCloud.count;
            f4 /= (float) pointCloud.count;
            
            float f5 = 0.0f;
            float f6 = 0.0f;
            float f7 = 0.0f;
            
            for (int i = 0; i < pointCloud.count; ++i) {
                f2 = pointCloud.x[i] - f3;
                f = pointCloud.y[i] - f4;
                f5 += f2 * f2;
                f6 += f2 * f;
                f7 += f * f;
            }
            
            float f8 = (float) (0.5 * Math.atan2(2.0 * (double) f6, f5 - f7));
            this.axisX = (float) Math.cos(f8);
            this.axisY = (float) Math.sin(f8);
            
            f2 = Float.MAX_VALUE;
            f = -3.4028235E38f;
            
            float f9 = Float.MAX_VALUE;
            float f10 = -3.4028235E38f;
            
            for (int i = 0; i < pointCloud.count; ++i) {
                float f11 = pointCloud.x[i] - f3;
                float f12 = pointCloud.y[i] - f4;
                float f13 = f11 * this.axisX + f12 * this.axisY;
                float f14 = f11 * -this.axisY + f12 * this.axisX;
                f2 = Math.min(f2, f13);
                f = Math.max(f, f13);
                f9 = Math.min(f9, f14);
                f10 = Math.max(f10, f14);
            }
            
            this.centerX = f3 + this.axisX * ((f2 + f) * 0.5f) - this.axisY * ((f9 + f10) * 0.5f);
            this.centerY = f4 + this.axisY * ((f2 + f) * 0.5f) + this.axisX * ((f9 + f10) * 0.5f);
            this.halfA = Math.max(5.0f, (f - f2) * 0.5f + 2.0f);
            this.halfB = Math.max(5.0f, (f10 - f9) * 0.5f + 2.0f);
            this.updatedNanos = System.nanoTime();
        }
        
        private boolean contains(double d, double d2) {
            if (!this.isFresh()) {
                return false;
            }
            
            float f = (float) d - this.centerX;
            float f2 = (float) d2 - this.centerY;
            float f3 = f * this.axisX + f2 * this.axisY;
            float f4 = f * -this.axisY + f2 * this.axisX;
            float f5 = 1.5f;
            
            return Math.abs(f3) <= this.halfA + f5 && Math.abs(f4) <= this.halfB + f5;
        }
        
        private boolean isFresh() {
            return this.updatedNanos != 0L && System.nanoTime() - this.updatedNanos <= 250000000L;
        }
        
    }
    
    public static class PointCloud {
        
        public final float[] x = new float[256];
        public final float[] y = new float[256];
        public int count = 0;
        
        public void add(float px, float py) {
            if (count < 256) {
                x[count] = px;
                y[count] = py;
                count++;
            }
        }
        
        public void clear() {
            count = 0;
        }
        
    }
    
}

