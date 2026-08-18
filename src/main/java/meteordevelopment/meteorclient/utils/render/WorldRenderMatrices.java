package meteordevelopment.meteorclient.utils.render;

import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class WorldRenderMatrices {
    
    private static final Matrix4f projection = new Matrix4f();
    private static final Matrix4f view = new Matrix4f();
    private static Vec3d cameraPosition = Vec3d.ZERO;
    
    private WorldRenderMatrices() {}
    
    public static void capture(Matrix4fc projectionMatrix, Matrix4fc viewMatrix, Vec3d cameraPos) {
        if (projectionMatrix != null) {
            projection.set(projectionMatrix);
        }
        if (viewMatrix != null) {
            view.set(viewMatrix);
        }
        cameraPosition = cameraPos == null ? Vec3d.ZERO : cameraPos;
    }
    
    public static Matrix4f projection() {
        return new Matrix4f(projection);
    }
    
    public static Matrix4f view() {
        return new Matrix4f(view);
    }
    
    public static Vec3d cameraPosition() {
        return cameraPosition;
    }
    
}
