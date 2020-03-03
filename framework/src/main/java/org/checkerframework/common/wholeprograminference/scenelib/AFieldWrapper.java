package org.checkerframework.common.wholeprograminference.scenelib;

import org.checkerframework.checker.nullness.qual.Nullable;
import scenelib.annotations.el.AField;

/** Wraps an AField from scenelib, and keeps additional information. */
public class AFieldWrapper {
    /** The wrapped object. */
    private final AField theField;

    /** A String representing the type of the field. */
    private final String type;

    /**
     * The name of the field. Used only for method formal parameters, which AScene fails to save.
     */
    private String parameterName;

    /**
     * Construct an AFieldWrapper.
     *
     * @param theField the wrapped AField
     * @param type a String representing the underlying type of the field, which AField doesn't
     *     include
     */
    public AFieldWrapper(AField theField, String type) {
        this.theField = theField;
        this.type = type;
    }

    /**
     * Avoid using this method if possible.
     *
     * @return the wrapped AField object.
     */
    public AField getTheField() {
        return theField;
    }

    /**
     * Returns the type of the field, as a String.
     *
     * @return the type of the field, as a String
     */
    public String getType() {
        return type;
    }

    /**
     * The name of the field, if it is a method formal parameter, or null if not.
     *
     * @return the name of the field or null
     */
    public @Nullable String getParameterName() {
        return parameterName;
    }

    /**
     * Provide a name.
     *
     * @param name the name of the field
     */
    public void setName(String name) {
        this.parameterName = name;
    }
}
