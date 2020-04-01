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
     * May include generic type arguments.
     */
    private final String type;

    /**
     * The name of the method formal parameter; null if this AField does not represent a method
     * formal parameter.
     */
    private final @Nullable String parameterName;

    /**
     * Construct an AFieldWrapper. Package-private, because it should only be called by
     * AMethodWrapper or AClassWrapper.
     *
     * @param theField the wrapped AField
     * @param type a String representing the underlying type of the field in a form printable in
     *     Java source, which AField doesn't include; may include generic type arguments
     * @param parameterName the parameter name, if this AField object represents a formal parameter,
     *     or null if it does not
     */
    AFieldWrapper(AField theField, String type, @Nullable String parameterName) {
        this.theField = theField;
        // TypeMirror#toString prints multiple annotations on a single type
        // separated by commas rather than by whitespace, as is required in source code.
        this.type = type.replaceAll(",@", " @");
        this.parameterName = parameterName;
    }

    /**
     * Construct an AFieldWrapper. Package-private, because it should only be called by
     * AMethodWrapper or AClassWrapper.
     *
     * @param theField the wrapped AField
     * @param type a String representing the underlying type of the field in a form printable in
     *     Java source, which AField doesn't include; may include generic type arguments
     */
    AFieldWrapper(AField theField, String type) {
        this(theField, type, null);
    }

    /**
     * Create a new AField representing a receiver parameter. This constructor is only for use by
     * SceneToStubWriter.
     *
     * @param receiver the AField to wrap
     * @param type the type of the receiver parameter, as a Java source string
     * @return an AFieldWrapper with the name "this" representing the given receiver
     */
    public static AFieldWrapper createReceiverParameter(AField receiver, String basename) {
        return new AFieldWrapper(receiver, basename, "this");
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
     * Returns the type of the field or formal, formatted to be printable in Java source code. May
     * include generic type arguments.
     *
     * @return the type of the field or formal, formatted to be printable in Java source code
     */
    public String getType() {
        return type;
    }

    /**
     * The identifier name, if {@code this} is a method formal parameter, or null if not.
     *
     * @return the name of the formal parameter or null
     */
    public @Nullable String getParameterName() {
        return parameterName;
    }
}
