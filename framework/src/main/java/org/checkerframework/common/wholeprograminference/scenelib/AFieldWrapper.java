package org.checkerframework.common.wholeprograminference.scenelib;

import org.checkerframework.checker.nullness.qual.Nullable;
import scenelib.annotations.el.AField;

/**
 * Wraps an AField from scenelib, and keeps additional information: the base type, and the name of
 * the parameter (if this AField represents a formal parameter).
 */
public class AFieldWrapper {
    /** The wrapped object. */
    private final AField theField;

    /**
     * A String representing the type of the field, formatted to be printable in Java source code.
     */
    private final String type;

    /**
     * The name of the method formal parameter; null if this AField does not represent a method
     * formal parameter.
     */
    private final @Nullable String parameterName;

    /**
     * Construct an AFieldWrapper.
     *
     * @param theField the wrapped AField
     * @param type a String representing the underlying type of the field in a form printable in
     *     Java source, which AField doesn't include
     * @param parameterName the name, if this AField object represents a formal parameter, or null
     *     if it does not
     */
    public AFieldWrapper(AField theField, String type, @Nullable String parameterName) {
        this.theField = theField;
        this.type = type;
        this.parameterName = parameterName;
    }

    /**
     * Construct an AFieldWrapper.
     *
     * @param theField the wrapped AField
     * @param type a String representing the underlying type of the field in a form printable in
     *     Java source, which AField doesn't include
     */
    public AFieldWrapper(AField theField, String type) {
        this(theField, type, null);
    }

    /**
     * Returns a reference to the underlying AField object. It is preferable to use the methods of
     * this class to using the underlying object, if possible.
     *
     * @return the wrapped AField object.
     */
    public AField getTheField() {
        return theField;
    }

    /**
     * Returns the type of the field, formatted to be printable in Java source code
     *
     * @return the type of the field, formatted to be printable in Java source code
     */
    public String getType() {
        return type;
    }

    /**
     * The identifier name, if it is a method formal parameter, or null if not.
     *
     * @return the name of the field or null
     */
    public @Nullable String getParameterName() {
        return parameterName;
    }
}
