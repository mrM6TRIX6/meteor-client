/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

public interface IName extends IDisplayName {
    
    String getName();
    
    @Override
    default String getDisplayName() {
        return NameFormat.display(getName());
    }

}
