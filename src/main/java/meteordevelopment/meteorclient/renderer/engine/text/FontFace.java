package meteordevelopment.meteorclient.renderer.engine.text;

import meteordevelopment.meteorclient.renderer.FontUtils;

import java.io.InputStream;

public class FontFace {
    
    public final FontInfo info;
    private final String name;
    
    public FontFace(FontInfo info, String name) {
        this.info = info;
        this.name = name;
    }
    
    public InputStream toStream() {
        InputStream in = FontUtils.stream(name);
        if (in == null) {
            throw new RuntimeException("Failed to load font " + name + ".");
        }
        return in;
    }
    
    @Override
    public String toString() {
        return info.toString();
    }
    
}
