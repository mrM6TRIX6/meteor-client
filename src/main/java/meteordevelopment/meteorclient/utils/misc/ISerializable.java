/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.google.gson.JsonObject;

public interface ISerializable<T> {
    
    JsonObject toJson();
    
    T fromJson(JsonObject jsonObject);
    
}
