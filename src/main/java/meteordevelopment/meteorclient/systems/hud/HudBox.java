/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.render.RenderUtils;

public class HudBox implements ISerializable<HudBox> {
    
    private final HudElement element;
    
    public XAnchor xAnchor = XAnchor.LEFT;
    public YAnchor yAnchor = YAnchor.TOP;
    
    public int x, y;
    int width, height;
    
    public HudBox(HudElement element) {
        this.element = element;
    }
    
    public void setSize(double width, double height) {
        if (width >= 0) {
            this.width = (int) Math.ceil(width);
        }
        if (height >= 0) {
            this.height = (int) Math.ceil(height);
        }
    }
    
    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public void setXAnchor(XAnchor anchor) {
        if (xAnchor != anchor) {
            int renderX = getRenderX();
            
            switch (anchor) {
                case LEFT -> x = renderX;
                case CENTER -> x = renderX + width / 2 - RenderUtils.getWindowWidth() / 2;
                case RIGHT -> x = renderX + width - RenderUtils.getWindowWidth();
            }
            
            xAnchor = anchor;
        }
    }
    
    public void setYAnchor(YAnchor anchor) {
        if (yAnchor != anchor) {
            int renderY = getRenderY();
            
            switch (anchor) {
                case TOP -> y = renderY;
                case CENTER -> y = renderY + height / 2 - RenderUtils.getWindowHeight() / 2;
                case BOTTOM -> y = renderY + height - RenderUtils.getWindowHeight();
            }
            
            yAnchor = anchor;
        }
    }
    
    public void updateAnchors() {
        setXAnchor(getXAnchor(getRenderX()));
        setYAnchor(getYAnchor(getRenderY()));
    }
    
    public void move(int deltaX, int deltaY) {
        x += deltaX;
        y += deltaY;
        
        if (element.autoAnchors) {
            updateAnchors();
        }
        
        int border = Hud.get().border.get();
        
        // Clamp X
        if (xAnchor == XAnchor.LEFT && x < border) {
            x = border;
        } else if (xAnchor == XAnchor.RIGHT && x > border) {
            x = border;
        }
        
        // Clamp Y
        if (yAnchor == YAnchor.TOP && y < border) {
            y = border;
        } else if (yAnchor == YAnchor.BOTTOM && y > border) {
            y = border;
        }
    }
    
    public XAnchor getXAnchor(double x) {
        double splitLeft = RenderUtils.getWindowWidth() / 3.0;
        double splitRight = splitLeft * 2;
        
        boolean left = x <= splitLeft;
        boolean right = x + width >= splitRight;
        
        if ((left && right) || (!left && !right)) {
            return XAnchor.CENTER;
        }
        return left ? XAnchor.LEFT : XAnchor.RIGHT;
    }
    
    public YAnchor getYAnchor(double y) {
        double splitTop = RenderUtils.getWindowHeight() / 3.0;
        double splitBottom = splitTop * 2;
        
        boolean top = y <= splitTop;
        boolean bottom = y + height >= splitBottom;
        
        if ((top && bottom) || (!top && !bottom)) {
            return YAnchor.CENTER;
        }
        return top ? YAnchor.TOP : YAnchor.BOTTOM;
    }
    
    public int getRenderX() {
        return switch (xAnchor) {
            case LEFT -> x;
            case CENTER -> RenderUtils.getWindowWidth() / 2 - width / 2 + x;
            case RIGHT -> RenderUtils.getWindowWidth() - width + x;
        };
    }
    
    public int getRenderY() {
        return switch (yAnchor) {
            case TOP -> y;
            case CENTER -> RenderUtils.getWindowHeight() / 2 - height / 2 + y;
            case BOTTOM -> RenderUtils.getWindowHeight() - height + y;
        };
    }
    
    public double alignX(double selfWidth, double width, Alignment alignment) {
        XAnchor anchor = xAnchor;
        
        if (alignment == Alignment.LEFT) {
            anchor = XAnchor.LEFT;
        } else if (alignment == Alignment.CENTER) {
            anchor = XAnchor.CENTER;
        } else if (alignment == Alignment.RIGHT) {
            anchor = XAnchor.RIGHT;
        }
        
        return switch (anchor) {
            case LEFT -> 0;
            case CENTER -> selfWidth / 2.0 - width / 2.0;
            case RIGHT -> selfWidth - width;
        };
    }
    
    // Serialization
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.addProperty("x-anchor", xAnchor.name());
        jsonObject.addProperty("y-anchor", yAnchor.name());
        jsonObject.addProperty("x", x);
        jsonObject.addProperty("y", y);
        
        return jsonObject;
    }
    
    @Override
    public HudBox fromJson(JsonObject jsonObject) {
        xAnchor = XAnchor.valueOf(jsonObject.get("x-anchor").getAsString());
        yAnchor = YAnchor.valueOf(jsonObject.get("y-anchor").getAsString());
        x = jsonObject.get("x").getAsInt();
        y = jsonObject.get("y").getAsInt();
        
        return this;
    }
    
}
