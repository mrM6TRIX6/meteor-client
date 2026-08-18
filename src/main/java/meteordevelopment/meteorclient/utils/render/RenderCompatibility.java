package meteordevelopment.meteorclient.utils.render;

public final class RenderCompatibility {
    
    private static Boolean safeWorldEffects;
    private static String safeWorldEffectsReason = "unresolved";
    private static final String SAFE_WORLD_EFFECTS_PROPERTY = "meteor.safeWorldEffects";
    private static final String SAFE_WORLD_EFFECTS_ENV = "meteor_SAFE_WORLD_EFFECTS";
    
    private static Boolean disableFragEffectScanShader;
    private static String disableFragEffectScanShaderReason = "unresolved";
    private static final String DISABLE_FRAG_EFFECT_SCAN_PROPERTY = "meteor.disableFragEffectScanShader";
    private static final String DISABLE_FRAG_EFFECT_SCAN_ENV = "meteor_DISABLE_FRAG_EFFECT_SCAN_SHADER";
    
    private RenderCompatibility() {}
    
    public static boolean useSafeWorldEffects() {
        Boolean manualOverride = readManualOverride(System.getProperty(SAFE_WORLD_EFFECTS_PROPERTY));
        if (manualOverride == null) {
            manualOverride = readManualOverride(System.getenv(SAFE_WORLD_EFFECTS_ENV));
        }
        if (manualOverride != null) {
            safeWorldEffects = manualOverride;
            safeWorldEffectsReason = "manual override";
            return manualOverride;
        }
        
        if (safeWorldEffects == null) {
            safeWorldEffects = false;
            safeWorldEffectsReason = "default-path";
        }
        return safeWorldEffects;
    }
    
    public static void primeFromCurrentContext() {
        useSafeWorldEffects();
        shouldDisableFragEffectScanShader();
    }
    
    public static boolean shouldDisableFragEffectScanShader() {
        Boolean manualOverride = readManualOverride(System.getProperty(DISABLE_FRAG_EFFECT_SCAN_PROPERTY));
        if (manualOverride == null) {
            manualOverride = readManualOverride(System.getenv(DISABLE_FRAG_EFFECT_SCAN_ENV));
        }
        if (manualOverride != null) {
            disableFragEffectScanShader = manualOverride;
            disableFragEffectScanShaderReason = "manual override";
            return manualOverride;
        }
        
        if (disableFragEffectScanShader == null) {
            disableFragEffectScanShader = false;
            disableFragEffectScanShaderReason = "depth-copy shader path enabled";
        }
        return disableFragEffectScanShader;
    }
    
    public static String getSafeWorldEffectsReason() {
        return safeWorldEffectsReason;
    }
    
    public static String getDisableFragEffectScanShaderReason() {
        return disableFragEffectScanShaderReason;
    }
    
    private static Boolean readManualOverride(String value) {
        if (value == null) {
            return null;
        }
        
        String normalized = value.trim().toLowerCase();
        if (normalized.equals("1") || normalized.equals("true") || normalized.equals("yes") || normalized.equals("on")) {
            return true;
        }
        if (normalized.equals("0") || normalized.equals("false") || normalized.equals("no") || normalized.equals("off")) {
            return false;
        }
        return null;
    }
    
}
