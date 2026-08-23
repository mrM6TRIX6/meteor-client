/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.name;

/**
 * A value that carries a human readable label for the gui.
 *
 * <p>A label is never parsed and never written to a config - that is what an id is for, see {@link IName}.
 * Implement this alone when a type only ever needs to be shown, or when the label cannot be derived from the
 * id and has to be spelled out.
 */
public interface IDisplayName {

    String getDisplayName();

}
