package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.IMinecraft;

import java.util.Locale;

public final class LoadingVisualGuard implements IMinecraft {
    
    private LoadingVisualGuard() {}
    
    public static boolean shouldSuppressHud() {
        return isLoadingVisual(mc.currentScreen) || isLoadingVisual(mc.getOverlay());
    }
    
    private static boolean isLoadingVisual(Object visual) {
        if (visual == null) {
            return false;
        }
        
        String className = visual.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        String fullName = visual.getClass().getName().toLowerCase(Locale.ROOT);
        
        return className.contains("loading")
            || className.contains("progress")
            || className.contains("connecting")
            || className.contains("downloading")
            || className.contains("terrain")
            || className.contains("generating")
            || className.contains("saving")
            || className.contains("reload")
            || className.contains("resource")
            || className.contains("pack")
            || className.contains("receiving")
            || className.contains("level")
            || className.contains("message")
            || fullName.contains("mojang");
    }
    
}
