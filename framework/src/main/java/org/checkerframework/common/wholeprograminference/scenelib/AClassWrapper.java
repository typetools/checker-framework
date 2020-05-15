package org.checkerframework.common.wholeprograminference.scenelib;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import scenelib.annotations.Annotation;
import scenelib.annotations.el.AClass;
import scenelib.annotations.el.AField;
import scenelib.annotations.el.AMethod;
import scenelib.annotations.util.JVMNames;

/**
 * A wrapper for the AClass class from scene-lib that carries additional name and type information
 * (referred to as "symbol information" elsewhere) that is useful during WPI.
 *
 * <p>This would be better as a subclass of AClass.
 */
public class AClassWrapper {

    /** The wrapped AClass object. */
    public final AClass theClass;

    /**
     * Wrap an AClass. Package-private, because it should only be called from ASceneWrapper.
     *
     * @param theClass the wrapped object
     */
    AClassWrapper(AClass theClass) {
        this.theClass = theClass;
    }

    /**
     * Obtain the given method, which can be further operated on to e.g. add information about a
     * parameter. WPI uses this to update the types in the method signature.
     *
     * <p>Results are interned.
     *
     * @param methodElt the method
     * @return an AMethod representing the method
     */
    public AMethod vivifyMethod(ExecutableElement methodElt) {
        String methodSignature = JVMNames.getJVMMethodSignature(methodElt);
        AMethod method = theClass.methods.getVivify(methodSignature);
        method.setFieldsFromMethodElement(methodElt);
        return method;
    }

    /**
     * Get all the methods that have been vivified (had their types updated by WPI) on a class.
     *
     * @return a map from method signature (in JVM format) to the object representing the method
     */
    public Map<String, AMethod> getMethods() {
        return ImmutableMap.copyOf(theClass.methods);
    }

    /**
     * Obtain the given field, which can be further operated on.
     *
     * <p>Results are interned.
     *
     * @param fieldName the name of the field
     * @param type the type of the field, which scenelib doesn't track
     * @return an AField object representing the field
     */
    public AField vivifyField(String fieldName, TypeMirror type) {
        AField field = theClass.fields.getVivify(fieldName);
        field.setTypeMirror(type);
        return field;
    }

    /**
     * Get all the fields that have been vivified on a class.
     *
     * @return a map from field name to the object representing the field
     */
    public Map<String, AField> getFields() {
        return ImmutableMap.copyOf(theClass.fields);
    }

    /**
     * Get the annotations on the class.
     *
     * @return the annotations, directly from scenelib
     */
    public Collection<? extends Annotation> getAnnotations() {
        return theClass.tlAnnotationsHere;
    }

    @Override
    public String toString() {
        return "AClassWrapper for " + theClass.toString();
    }
}
