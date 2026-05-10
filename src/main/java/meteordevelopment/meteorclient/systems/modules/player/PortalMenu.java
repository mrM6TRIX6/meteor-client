/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;

public class PortalMenu extends Module {
    
    public PortalMenu() {
        super(Category.PLAYER, "PortalMenu", "Allows you to use GUIs normally while in a Nether Portal.");
    }
    
}
