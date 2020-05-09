package org.checkerframework.common.wholeprograminference.scenelib;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import scenelib.annotations.el.AField;
import scenelib.annotations.el.AMethod;

/**
 * A wrapper for the AMethod class from scenelib. Keeps more information about the return type and
 * formal parameters.
 */
public class AMethodWrapper {

    /** The wrapped AMethod. */
    private final AMethod theMethod;

    /** The type parameters of this method. */
    private final List<? extends TypeParameterElement> typeParameters;

    /**
     * Wrap an AMethod. Package-private, because it should only be called from AClassWrapper.
     *
     * @param theMethod the method to wrap
     * @param methodElt the method's declaration
     */
    AMethodWrapper(AMethod theMethod, ExecutableElement methodElt) {
        this.theMethod = theMethod;
        this.getAMethod().setReturnTypeMirror(methodElt.getReturnType());
        this.typeParameters = methodElt.getTypeParameters();
        vivifyAndAddTypeMirrorToParameters(methodElt);
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
    private void vivifyAndAddTypeMirrorToParameters(ExecutableElement methodElt) {
        for (int i = 0; i < methodElt.getParameters().size(); i++) {
            VariableElement ve = methodElt.getParameters().get(i);
            TypeMirror type = ve.asType();
            Name name = ve.getSimpleName();
            vivifyAndAddTypeMirrorToParameter(i, type, name);
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
    public AField vivifyAndAddTypeMirrorToParameter(int i, TypeMirror type, Name simpleName) {
        AField param = theMethod.parameters.getVivify(i);
        param.setName(simpleName.toString());
        if (param.getTypeMirror() == null) {
            param.setTypeMirror(type);
        }
        return param;
    }

    /**
     * Get the parameters, as a map from parameter index (0-indexed) to representation.
     *
     * @return an immutable copy of the vivified parameters, as a map from index to representation
     */
    public Map<Integer, AField> getParameters() {
        return ImmutableMap.copyOf(theMethod.parameters);
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
