package meteordevelopment.meteorclient.utils.render.post.handsflame;

import net.fabricmc.loader.api.FabricLoader;

public final class IrisShaderCompat {
    
    private static Boolean irisLoaded;
    private static boolean lastPackInUse;
    private static long lastPackInUseCheck;
    
    private IrisShaderCompat() {}
    
    public static void renderHandsFlameAfterIrisFinalPass() {
        if (!IrisShaderCompat.isShaderPackInUse()) {
            return;
        }
        HandsFlameRenderer.renderIrisCapturedHandsFlame();
    }
    
    public static boolean isShaderPackInUse() {
        if (!IrisShaderCompat.isIrisLoaded()) {
            return false;
        }
        
        long l = System.currentTimeMillis();
        if (l - lastPackInUseCheck < 1000L) {
            return lastPackInUse;
        }
        
        lastPackInUseCheck = l;
        lastPackInUse = IrisApiHolder.isShaderPackInUse();
        return lastPackInUse;
    }
    
    public static void beginHandDepthCapture() {
        if (IrisShaderCompat.isShaderPackInUse()) {
            HandsFlameRenderer.beginIrisHandDepthCapture();
        }
    }
    
    private static boolean isIrisLoaded() {
        if (irisLoaded == null) {
            irisLoaded = FabricLoader.getInstance().isModLoaded("iris");
        }
        return irisLoaded;
    }
    
    public static void endHandDepthCapture() {
        if (IrisShaderCompat.isShaderPackInUse()) {
            HandsFlameRenderer.endIrisHandDepthCapture();
        }
    }
    
    private static class IrisApiHolder {
        
        static boolean isShaderPackInUse() {
            try {
                Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object apiInstance = apiClass.getMethod("getInstance").invoke(null);
                return (boolean) apiClass.getMethod("isShaderPackInUse").invoke(apiInstance);
            } catch (Throwable t) {
                return false;
            }
        }
        
    }
    
}

