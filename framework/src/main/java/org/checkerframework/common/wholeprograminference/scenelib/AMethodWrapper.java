package org.checkerframework.common.wholeprograminference.scenelib;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import scenelib.annotations.el.AMethod;

/**
 * A wrapper for the AMethod class from scenelib. Keeps more information about the return type and
 * formal parameters.
 */
public class AMethodWrapper {

    /** The wrapped AMethod. */
    private final AMethod theMethod;

    /** The return type of the method, or null if the method's return type is unknown or void. */
    private @Nullable TypeMirror returnType;

    /**
     * A mirror of the parameters field of AMethod, but using AFieldWrapper objects as the values.
     * Keys are parameter indices. Like the parameters field of AMethod, this map starts empty and
     * is vivified by calls to {@link #vivifyParameter(int, TypeMirror, Name)} or {@link
     * #vivifyParameter(int, TypeMirror, Name)}.
     */
    private Map<Integer, AFieldWrapper> parameters = new HashMap<>();

    /** The type parameters of this method. */
    private List<? extends TypeParameterElement> typeParameters;

    /**
     * Wrap an AMethod. Package-private, because it should only be called from AClassWrapper.
     *
     * @param theMethod the method to wrap
     * @param methodElt the method's declaration
     */
    AMethodWrapper(AMethod theMethod, ExecutableElement methodElt) {
        this.theMethod = theMethod;
        this.returnType = methodElt.getReturnType();
        this.vivifyParameters(methodElt);
        this.typeParameters = methodElt.getTypeParameters();
    }

    /**
     * Get the return type.
     *
     * @return the return type, or null if the return type is unknown or void
     */
    public @Nullable TypeMirror getReturnType() {
        return returnType;
    }

    /**
     * Get the type parameters of this method.
     *
     * @return the list of type parameters
     */
    public List<? extends TypeParameterElement> getTypeParameters() {
        return typeParameters;
    }

    /**
     * Populates the method parameter map for the method. This is called from the constructor, so
     * that the method parameter map always has an entry for each parameter.
     *
     * @param methodElt the method whose parameters should be vivified
     */
    private void vivifyParameters(ExecutableElement methodElt) {
        for (int i = 0; i < methodElt.getParameters().size(); i++) {
            VariableElement ve = methodElt.getParameters().get(i);
            TypeMirror type = ve.asType();
            Name name = ve.getSimpleName();
            this.vivifyParameter(i, type, name);
        }
    }

    /**
     * Obtain the parameter at the given index, which can be further operated on to e.g. add a type
     * annotation.
     *
     * @param i the parameter index (first parameter is zero)
     * @param type the type of the parameter
     * @param simpleName the name of the parameter
     * @return an AFieldWrapper representing the parameter
     */
    public AFieldWrapper vivifyParameter(int i, TypeMirror type, Name simpleName) {
        if (parameters.containsKey(i)) {
            return parameters.get(i);
        } else {
            AFieldWrapper wrapper =
                    new AFieldWrapper(
                            theMethod.parameters.getVivify(i), type, simpleName.toString());
            parameters.put(i, wrapper);
            return wrapper;
        }
    }

    /**
     * Get the parameters, as a map from parameter index (0-indexed) to representation.
     *
     * @return an immutable copy of the vivified parameters, as a map from index to representation
     */
    public Map<Integer, AFieldWrapper> getParameters() {
        return ImmutableMap.copyOf(parameters);
    }

    /**
     * Avoid calling this if possible; prefer the methods of this class.
     *
     * @return the underlying AMethod object that has been wrapped
     */
    public AMethod getAMethod() {
        return theMethod;
    }
}
