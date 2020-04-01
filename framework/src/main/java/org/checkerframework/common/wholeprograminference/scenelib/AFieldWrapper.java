package org.checkerframework.common.wholeprograminference.scenelib;

import javax.lang.model.type.TypeMirror;
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
     * Javac's representation of the type of the parameter or field represented by {@code theField}.
     */
    private final TypeMirror type;

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
     * @param type javac's representation of the type of the wrapped field
     * @param parameterName the parameter name, if this AField object represents a formal parameter,
     *     or null if it does not
     */
    AFieldWrapper(AField theField, TypeMirror type, @Nullable String parameterName) {
        this.theField = theField;
        this.type = type;
        this.parameterName = parameterName;
    }

    /**
     * Construct an AFieldWrapper. Package-private, because it should only be called by
     * AMethodWrapper or AClassWrapper.
     *
     * @param theField the wrapped AField
     * @param type javac's representation of the type of the wrapped field
     */
    AFieldWrapper(AField theField, TypeMirror type) {
        this(theField, type, null);
    }

    /**
     * Create a new AField representing a receiver parameter. This constructor is only for use by
     * SceneToStubWriter.
     *
     * @param receiver the AField to wrap
     * @return an AFieldWrapper with the name "this" representing the given receiver
     */
    public static AFieldWrapper createReceiverParameter(AField receiver) {
        return new AFieldWrapper(receiver, null, "this");
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
     * Returns the type of the field or formal.
     *
     * @return the type of the field or formal
     */
    public TypeMirror getType() {
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
