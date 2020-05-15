package org.checkerframework.common.wholeprograminference.scenelib;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
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
    public AMethod setFieldsFromMethod(ExecutableElement methodElt) {
        String methodSignature = JVMNames.getJVMMethodSignature(methodElt);
        AMethod method = theClass.methods.getVivify(methodSignature);
        method.setFieldsFromMethodElement(methodElt);
        return method;
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
    public AField addTypeMirrorToField(String fieldName, TypeMirror type) {
        AField field = theClass.fields.getVivify(fieldName);
        field.setTypeMirror(type);
        return field;
    }

    @Override
    public String toString() {
        return "AClassWrapper for " + theClass.toString();
    }
}
