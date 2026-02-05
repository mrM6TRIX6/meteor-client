/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.state.QuadColorState;
import meteordevelopment.meteorclient.utils.render.state.QuadRadiusState;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;

public class Test extends Module {
    
    public Test() {
        super(Categories.RENDER, "Test", "");
        runInMainMenu = true;
    }
    
    @EventHandler
    private void onRender2D(Render2DEvent event) {
        double width = 300;
        double height = 150;
        QuadColorState color = QuadColorState.of(Color.RED);
        QuadRadiusState radius = QuadRadiusState.of(10);
        double smoothness = 10;
        
        Renderer2D.COLOR.begin();
        
        Renderer2D.COLOR.rectangle(
            50,
            50,
            width,
            height,
            color,
            radius,
            smoothness
        );
        
        Renderer2D.COLOR.end();
        
        Renderer2D.COLOR.render();
    }
    
}
