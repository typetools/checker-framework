package org.checkerframework.common.wholeprograminference.scenelib;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.BugInCF;
import scenelib.annotations.Annotation;
import scenelib.annotations.el.AClass;

/**
 * A wrapper for the AClass class from scene-lib that carries additional information that is useful
 * during WPI.
 *
 * <p>This might be better as a subclass of AClass, but AClass is final.
 */
public class AClassWrapper {

    /** The wrapped AClass object. */
    private AClass theClass;

    /**
     * The methods of the class. Keys are the names of methods, entries are AMethodWrapper objects.
     * Mirrors the "methods" field of AClass.
     */
    private final Map<String, AMethodWrapper> methods = new HashMap<>();

    /**
     * The fields of the class. Keys are the names of the fields, entries are AFieldWrapper objects.
     * Wraps the "fields" field of AClass.
     */
    private final Map<String, AFieldWrapper> fields = new HashMap<>();

    /** The enum constants of the class, or null if this class is not an enum. */
    private @MonotonicNonNull List<VariableElement> enumConstants = null;

    /**
     * The type element representing the class. Clients must call {@link
     * #setTypeElement(TypeElement)} before interacting with this field.
     */
    private @MonotonicNonNull TypeElement typeElement = null;

    /**
     * Wrap an AClass.
     *
     * @param theClass the wrapped object
     */
    public AClassWrapper(AClass theClass) {
        this.theClass = theClass;
    }

    /**
     * Call before doing anything with a method. Interacts with scenelib and fetches or creates an
     * appropriate AMethodWrapper object.
     *
     * <p>Results are interned.
     *
     * @param methodName the name of the method
     * @return an AMethodWrapper representing the method
     */
    public AMethodWrapper vivifyMethod(String methodName) {
        if (methods.containsKey(methodName)) {
            return methods.get(methodName);
        } else {
            AMethodWrapper wrapper = new AMethodWrapper(theClass.methods.getVivify(methodName));
            methods.put(methodName, wrapper);
            return wrapper;
        }
    }

    /**
     * Get all the methods that have been vivified (that is, interacted with) on a class.
     *
     * @return a map from method name to the object representing the method
     */
    public Map<String, AMethodWrapper> getMethods() {
        return ImmutableMap.copyOf(methods);
    }

    /**
     * Call before doing anything with a field. Interacts with scenelib and fetches or creates an
     * appropriate AFieldWrapper object.
     *
     * <p>Results are interned.
     *
     * @param fieldName the name of the field
     * @param type the type of the field, which scenelib doesn't track
     * @return an AField object representing the field
     */
    public AFieldWrapper vivifyField(String fieldName, TypeMirror type) {
        if (fields.containsKey(fieldName)) {
            return fields.get(fieldName);
        } else {
            AFieldWrapper wrapper =
                    new AFieldWrapper(theClass.fields.getVivify(fieldName), type.toString());
            fields.put(fieldName, wrapper);
            return wrapper;
        }
    }

    /**
     * Get all the fields that have been vivified (that is, interacted with) on a class.
     *
     * @return a map from field name to the object representing the field
     */
    public Map<String, AFieldWrapper> getFields() {
        return ImmutableMap.copyOf(fields);
    }

    /**
     * Get the annotations on the class.
     *
     * @return the annotations, directly from scenelib
     */
    public Collection<? extends Annotation> getAnnotations() {
        return theClass.tlAnnotationsHere;
    }

    /**
     * Get the type of the class, or null if it is unknown. Callers should ensure that either: -
     * {@link #setTypeElement(TypeElement)} has been called, or - the return value is checked
     * against null
     *
     * @return a type element representing this class
     */
    public @Nullable TypeElement getTypeElement() {
        return typeElement;
    }

    /**
     * Set the type element representing the class.
     *
     * @param typeElement the type element representing the class
     */
    public void setTypeElement(TypeElement typeElement) {
        if (this.typeElement == null) {
            this.typeElement = typeElement;
        } else if (!this.typeElement.equals(typeElement)) {
            throw new BugInCF(
                    "Set the base type of an AClassWrapper to a different type than the previous"
                            + "base type.");
        }
    }

    /**
     * Checks if any enum constants have been provided to this class/
     *
     * @return true if this class has been marked as an enum
     */
    public boolean isEnum() {
        return enumConstants != null;
    }

    /**
     * Returns the set of enum constants for this class, or null if this is not an enum.
     *
     * @return the enum constants, or null if this is not an enum
     */
    public @Nullable List<VariableElement> getEnumConstants() {
        if (this.isEnum()) {
            return ImmutableList.copyOf(enumConstants);
        } else {
            return null;
        }
    }

    /**
     * Marks this class as an enum.
     *
     * @param enumConstants the list of enum constants for the class
     */
    public void markAsEnum(List<VariableElement> enumConstants) {
        if (this.enumConstants != null) {
            throw new BugInCF("WPI marked the same class as an enum multiple times");
        }
        this.enumConstants = new ArrayList<>(enumConstants);
    }
}
