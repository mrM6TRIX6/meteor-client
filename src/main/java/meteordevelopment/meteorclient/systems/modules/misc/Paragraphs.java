/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

/**
 * This is very simple module. See full implementation in {@link meteordevelopment.meteorclient.mixin.StringHelperMixin#isValidChar}
 */
public class Paragraphs extends Module {
    
    public Paragraphs() {
        super(Categories.MISC, "Paragraphs", "Makes you able to write §. (normally \"illegal\" character).");
    }
    
}
