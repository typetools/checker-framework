package org.checkerframework.common.wholeprograminference.scenelib;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.wholeprograminference.SceneToStubWriter;
import org.checkerframework.javacutil.BugInCF;
import scenelib.annotations.Annotation;
import scenelib.annotations.el.AClass;
import scenelib.annotations.el.AField;
import scenelib.annotations.util.JVMNames;

/**
 * A wrapper for the AClass class from scene-lib that carries additional name and type information
 * (referred to as "symbol information" elsewhere) that is useful during WPI.
 *
 * <p>This would be better as a subclass of AClass.
 */
public class AClassWrapper {

    /** The wrapped AClass object. */
    private final AClass theClass;

    /**
     * This HashSet contains the simple class names any of this class' outer classes (or this class)
     * that are enums.
     */
    private final HashSet<String> enums = new HashSet<>();

    /**
     * The methods of the class. Keys are the signatures of methods, entries are AMethodWrapper
     * objects. Mirrors the "methods" field of AClass.
     */
    private final Map<String, AMethodWrapper> methods = new HashMap<>();

    /** The enum constants of the class, or null if this class is not an enum. */
    private @MonotonicNonNull List<VariableElement> enumConstants = null;

    /**
     * The type element representing the class. Clients must call {@link
     * #setTypeElement(TypeElement)} before accessing this field.
     */
    private @MonotonicNonNull TypeElement typeElement = null;

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
     * @return an AMethodWrapper representing the method
     */
    public AMethodWrapper vivifyMethod(ExecutableElement methodElt) {
        String methodSignature = JVMNames.getJVMMethodSignature(methodElt);
        if (methods.containsKey(methodSignature)) {
            return methods.get(methodSignature);
        } else {
            AMethodWrapper wrapper =
                    new AMethodWrapper(theClass.methods.getVivify(methodSignature));
            wrapper.getAMethod().setFieldsFromMethodElement(methodElt);

            methods.put(methodSignature, wrapper);
            return wrapper;
        }
    }

    /**
     * Get all the methods that have been vivified (had their types updated by WPI) on a class.
     *
     * @return a map from method signature (in JVM format) to the object representing the method
     */
    public Map<String, AMethodWrapper> getMethods() {
        return ImmutableMap.copyOf(methods);
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

    /**
     * Get the type of the class, or null if it is unknown. Callers should ensure that either:
     *
     * <ul>
     *   <li>{@link #setTypeElement(TypeElement)} has been called, or
     *   <li>the return value is checked against null.
     * </ul>
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
                    "setTypeElement(%s): type is already %s", typeElement, this.typeElement);
        }
    }

    /**
     * Checks if the given class is an enum or not.
     *
     * @param className the simple class name of this class or one of its outer classes
     * @return true if the given class is an enum
     */
    public boolean isEnum(String className) {
        return enums.contains(className);
    }

    /**
     * Checks if this class is an enum.
     *
     * @return true if this class is an enum
     */
    public boolean isEnum() {
        return enums.contains(this.theClass.className);
    }

    /**
     * Marks the given simple class name as an enum. This method is used to mark outer classes of
     * this class that have not been vivified, meaning that only their names are available.
     *
     * <p>Note that this code will misbehave if a class has the same name as its inner enum, or
     * vice-versa, because this uses simple names.
     *
     * @param className the simple class name of this class or one of its outer classes
     */
    public void markAsEnum(String className) {
        enums.add(className);
    }

    /**
     * Returns the set of enum constants for this class, or null if this is not an enum.
     *
     * @return the enum constants, or null if this is not an enum
     */
    public @Nullable List<VariableElement> getEnumConstants() {
        if (enumConstants != null) {
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
    public void setEnumConstants(List<VariableElement> enumConstants) {
        if (this.enumConstants != null) {
            throw new BugInCF(
                    "setEnumConstants was called multiple times with arguments %s and %s",
                    this.enumConstants, enumConstants);
        }
        this.enumConstants = new ArrayList<>(enumConstants);
    }

    /**
     * Can {@link SceneToStubWriter} print this class? If so, do nothing. If not, throw an error.
     */
    public void checkIfPrintable() {
        if (typeElement == null) {
            throw new BugInCF(
                    "Tried printing an unprintable class to a stub file during WPI: "
                            + theClass.className);
        }
    }

    @Override
    public String toString() {
        return "AClassWrapper for " + theClass.toString();
    }
}
