package org.checkerframework.common.wholeprograminference.scenelib;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiers;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import scenelib.annotations.el.AMethod;

/**
 * A wrapper for the AMethod class from scenelib. Keeps more information on the return type, and
 * wraps the parameter list so that more information can be kept on the parameters.
 */
public class AMethodWrapper {

    /** The wrapped AMethod. */
    private final AMethod theMethod;

    /** The return type of the method, as a fully qualified name. */
    private @FullyQualifiedName String returnType = "java.lang.Object";

    /**
     * A wrapper for the parameters field of AMethod, but using AFieldWrapper objects as the values.
     * Keys are parameter indices.
     */
    private Map<Integer, AFieldWrapper> parameters = new HashMap<>();

    /**
     * The return type, as a fully-qualified name.
     *
     * @return the return type as a fully-qualified name, or "java.lang.Object" if the return type
     *     is unknown.
     */
    public @FullyQualifiedName String getReturnType() {
        return returnType;
    }

    /**
     * Provide the AMethodWrapper with a return type
     *
     * @param type the return type as a TypeMirror
     */
    public void setReturnType(TypeMirror type) {
        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
        @DotSeparatedIdentifiers String typeAsString = type.toString();
        setReturnType(typeAsString);
    }

    /**
     * Provide the AMethodWrapper with a return type
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
     * Wrap an AMethod
     *
     * @param theMethod the method to wrap
     */
    public AMethodWrapper(AMethod theMethod) {
        this.theMethod = theMethod;
    }

    /**
     * Avoid if possible.
     *
     * @return the underlying AMethod object that has been wrapped
     */
    public AMethod getAMethod() {
        return theMethod;
    }

    /**
     * Interact with a parameter.
     *
     * @param i the parameter index (1st parameter is zero)
     * @param type the type of the parameter, as a TypeMirror
     * @param simpleName the name of the parameter
     * @return an AFieldWrapper representing the parameter
     */
    public AFieldWrapper vivifyParameter(int i, TypeMirror type, Name simpleName) {
        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
        @DotSeparatedIdentifiers String typeAsString = type.toString();
        return vivifyParameter(i, typeAsString, simpleName);
    }

    /**
     * Interact with a parameter.
     *
     * @param i the parameter index (1st parameter is zero)
     * @param type a fully-qualified name representing the type of the parameter
     * @param simpleName the name of the parameter
     * @return an AFieldWrapper representing the parameter
     */
    private AFieldWrapper vivifyParameter(int i, @FullyQualifiedName String type, Name simpleName) {
        if (parameters.containsKey(i)) {
            return parameters.get(i);
        } else {
            AFieldWrapper wrapper = new AFieldWrapper(theMethod.parameters.getVivify(i), type);
            parameters.put(i, wrapper);
            wrapper.setName(simpleName.toString());
            return wrapper;
        }
    }

    /**
     * Get the parameters, if they have been vivified
     *
     * @return an immutable copy of the vivified parameters, as a map from index to representation
     */
    public Map<Integer, AFieldWrapper> getParameters() {
        return ImmutableMap.copyOf(parameters);
    }
}
