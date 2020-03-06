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
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiers;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import scenelib.annotations.el.AMethod;

/**
 * A wrapper for the AMethod class from scenelib. Keeps more information about the return type and
 * formal parameters.
 */
public class AMethodWrapper {

    /** The wrapped AMethod. */
    private final AMethod theMethod;

    /** The return type of the method, as a fully qualified name. */
    private @FullyQualifiedName String returnType = "java.lang.Object";

    /**
     * A mirror of the parameters field of AMethod, but using AFieldWrapper objects as the values.
     * Keys are parameter indices. Like the parameters field of AMethod, this map starts empty and
     * is vivified by calls to {@link #vivifyParameter(int, String, Name)} or {@link
     * #vivifyParameter(int, TypeMirror, Name)}.
     */
    private Map<Integer, AFieldWrapper> parameters = new HashMap<>();

    /** The type parameters of this method. */
    private List<? extends TypeParameterElement> typeParameters;

    /**
     * The return type, as a fully-qualified name.
     *
     * @return the return type as a fully-qualified name, or "java.lang.Object" if the return type
     *     is unknown
     */
    public @FullyQualifiedName String getReturnType() {
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
     * Provide the AMethodWrapper with a return type.
     *
     * @param returnType a fully-qualified name
     */
    private void setReturnType(@FullyQualifiedName String returnType) {
        // do not keep return types that start with a ?, because that's not valid Java
        if ("java.lang.Object".equals(this.returnType) && !returnType.startsWith("?")) {
            this.returnType = returnType;
        }
    }

    /**
     * Wrap an AMethod. Package-private, because it should only be called from AClassWrapper.
     *
     * @param theMethod the method to wrap
     */
    AMethodWrapper(AMethod theMethod, ExecutableElement methodElt) {
        this.theMethod = theMethod;
        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
        @DotSeparatedIdentifiers String typeAsString = methodElt.getReturnType().toString();
        setReturnType(typeAsString);
        vivifyParameters(methodElt);
        this.typeParameters = methodElt.getTypeParameters();
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
     * Avoid calling this if possible; prefer the methods of this class.
     *
     * @return the underlying AMethod object that has been wrapped
     */
    public AMethod getAMethod() {
        return theMethod;
    }

    /**
     * Add the given parameter to the scene-lib representation.
     *
     * @param i the parameter index (zero-indexed)
     * @param type the type of the parameter, as a TypeMirror
     * @param simpleName the name of the parameter
     * @return an AFieldWrapper representing the parameter
     */
    public AFieldWrapper vivifyParameter(int i, TypeMirror type, Name simpleName) {
        String typeAsString = type.toString();
        return vivifyParameter(i, typeAsString, simpleName);
    }

    /**
     * Add the given parameter to the scene-lib representation.
     *
     * @param i the parameter index (first parameter is zero)
     * @param type the type of the parameter, printable in Java source code
     * @param simpleName the name of the parameter
     * @return an AFieldWrapper representing the parameter
     */
    private AFieldWrapper vivifyParameter(int i, String type, Name simpleName) {
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
     * Get the parameters, as a map from parameter index (0 -> first parameter) to representation.
     *
     * @return an immutable copy of the vivified parameters, as a map from index to representation
     */
    public Map<Integer, AFieldWrapper> getParameters() {
        return ImmutableMap.copyOf(parameters);
    }
}
