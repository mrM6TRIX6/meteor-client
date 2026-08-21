/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.mixininterface.IGpuDevice;
import meteordevelopment.meteorclient.utils.render.ui.Render2D;

import java.util.ArrayList;
import java.util.List;

public class Scissor {
    
    public int x, y;
    public int width, height;
    
    public final List<Runnable> postTasks = new ArrayList<>();
    
    public Scissor set(double x, double y, double width, double height) {
        if (width < 0) {
            width = 0;
        }
        if (height < 0) {
            height = 0;
        }
        
        this.x = (int) Math.round(x);
        this.y = (int) Math.round(y);
        this.width = (int) Math.round(width);
        this.height = (int) Math.round(height);
        
        postTasks.clear();
        
        return this;
    }
    
    public void push() {
        // The gl scissor box is in physical framebuffer pixels with a bottom left origin,
        // so the independent units have to be converted back manually here.
        float s = Render2D.uiScale();
        
        int scissorX = Math.round(x * s);
        int scissorY = Math.round(y * s);
        int scissorWidth = Math.round(width * s);
        int scissorHeight = Math.round(height * s);
        
        ((IGpuDevice) RenderSystem.getDevice()).meteor$pushScissor(scissorX, Render2D.height() - scissorY - scissorHeight, scissorWidth, scissorHeight);
    }
    
    public void pop() {
        ((IGpuDevice) RenderSystem.getDevice()).meteor$popScissor();
    }
    
}
