package checkers.igj;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.*;
import javax.lang.model.type.*;

import checkers.igj.quals.*;
import checkers.types.*;
import checkers.util.TypesUtils;

/**
 * This class encapsulate the relationships of immutability.
 */
public enum IGJImmutability {

    // ***************************************************
    // The Enum portion
    // ***************************************************
    READONLY(ReadOnly.class), IMMUTABLE(Immutable.class), MUTABLE(
            Mutable.class), ASSIGNSFIELDS(AssignsFields.class), PLACE_HOLDER(
            IGJPlaceHolder.class), WILD_CARD(I.class);

    private Class<? extends Annotation> annotation;

    private IGJImmutability(Class<? extends Annotation> annotation) {
        this.annotation = annotation;
    }

    /**
     * @return The {@code Annotation} that corresponds to this
     *         immutability.
     */
    public Class<? extends Annotation> getAnnotation() {
        return annotation;
    }

    /**
     * Finds the {@code IGJImmutability} type that corresponds to the
     * passed annotation and returns it, if one exists.
     * Otherwise, it throws an {@code IllegalArgumentException}.
     * 
     * @param annotation
     *            the name of the immutability {@code Annotation}
     * @return the {@code IGJImmutability} that corresponds to
     *         {@code annotation}
     */
    public static IGJImmutability valueOf(CharSequence annon) {
        String annotation = annon.toString();
        for (IGJImmutability value : values()) {
            if (value.getAnnotation().getCanonicalName().equals(annotation))
                return value;
        }

        throw new IllegalArgumentException(
                "Annotation is not an IGJ Annotation");
    }

    public static IGJImmutability valueOf(AnnotationData data) {
        Name qualifiedName = getAnnotationTypeName(data);
        return valueOf(qualifiedName);
    }

    /**
     * Compares this immutability type to otherType, and returns
     * {@code true} if this is a subtype of otherType.
     * 
     * @param otherType
     *            the immutability type to be compared to
     * @return true iff this <= othertype
     */
    public boolean isSubtypeOf(IGJImmutability otherType) {
        if (otherType == READONLY || otherType == PLACE_HOLDER
                || otherType == WILD_CARD || otherType == ASSIGNSFIELDS)
            return true;
        else if (this == PLACE_HOLDER)
            return true;
        else
            return (this == otherType);
    }

    /**
     * Returns the immutability type of the type in the given
     * {@code AnnotationLocation}.
     * 
     * @param type
     *            the class type to be checked
     * @param location
     *            the location to be checked for immutability
     * @return the immutability type in the given location or
     *         {@code null} if immutability is not specified
     */
    public static IGJImmutability getIGJImmutabilityAt(
            AnnotatedClassType type, AnnotationLocation location) {
        AnnotationData annotation =
            getIGJAnnotationDataAt(type, location);
        if (annotation == null)
            return null;
        return valueOf(annotation);
    }

    // **************************************************************
    // Immutability Annotations Method
    // **************************************************************

    /**
     * Returns the specified IGJ AnnotationData at the specified
     * location
     * 
     * @param type
     * @param location
     * @return the specified IGJ AnnotationData found in the given
     *         location
     */
    public static AnnotationData getIGJAnnotationDataAt(
            AnnotatedClassType type, AnnotationLocation location) {
        
        for (AnnotationData a : getIGJAnnotationData(type)) {
            if (a.getLocation().equals(location))
                return a;
        }
        
        return null;
    }

    public static Set<AnnotationData> getIGJAnnotationData(
            AnnotatedClassType type) {
        Set<AnnotationData> annotations = new HashSet<AnnotationData>();
        for (IGJImmutability annon : values()) {
            annotations.addAll(type.getAnnotationData(annon.getAnnotation()
                    .getCanonicalName(), true));
        }
        return annotations;
    }

    /**
     * Checks if type is mutable at the given location
     * 
     * @param type
     * @param location
     * @return true iff type is mutable at the given location
     */
    public static boolean isMutableAt(AnnotatedClassType type,
            AnnotationLocation location) {
        return (type.hasAnnotationAt(Mutable.class, location) || type
                .hasAnnotationAt(IGJPlaceHolder.class, location));
    }

    /**
     * Checks if the type is immutable at the given location
     * 
     * @param type
     * @param location
     * @return true iff type is immutable at the given location
     */
    public static boolean isImmutableAt(AnnotatedClassType type,
            AnnotationLocation location) {
        return (type.hasAnnotationAt(Immutable.class, location) || type
                .hasAnnotationAt(IGJPlaceHolder.class, location));
    }

    public static boolean isIGJAnnotation(CharSequence qualifiedName) {
        IGJImmutability[] values = IGJImmutability.values();
        for (int i = 0; i < values.length; ++i) {
            if (values[i].getAnnotation().getCanonicalName().contentEquals(
                    qualifiedName))
            return true;
        }
        return false;
    }

    public static boolean isIGJAnnotation(AnnotationData data) {        
        return isIGJAnnotation(getAnnotationTypeName(data));
    }

    /**
     * Utility method to extract the value of the wildcard Annotation
     * 
     * @param annotationData
     *            the IGJ annotation data
     * @return the id number of the annotationData
     */
    public static String getID(AnnotationData data) {
        // Defensive
        if (I.class.equals(data.getType().getClass()))
            throw new IllegalArgumentException("data is not a @I Annotation");

        // W has only one value
        // assert data.getValues().values().size() == 1; // assertion
        // is false due to a bug
        Map<? extends ExecutableElement, ? extends AnnotationValue> map =
            data.getValues();

        // Solve a bug
        if (map.isEmpty()) {
            return "I"; // To be fixed
        } else
            return (String) data.getValues().values().iterator().next()
                    .getValue();
    }

    public static boolean isWildcard(AnnotationData data) {
        return (getAnnotationTypeName(data).contentEquals(I.class.getCanonicalName()));
    }

    public static Name getAnnotationTypeName(AnnotationData data) {
        return TypesUtils.getQualifiedName((DeclaredType)data.getType());
    }

    public static boolean isSubtype(AnnotationData child,
            AnnotationData parent) {
        assert isIGJAnnotation(child) && isIGJAnnotation(parent);
        if (getAnnotationTypeName(child).contentEquals(
                IGJPlaceHolder.class.getCanonicalName()))
            return true;
        else if (isWildcard(parent)) {
            return (isWildcard(child) && getID(child).equals(getID(parent)));
        } else if (isWildcard(child)) {
            return READONLY.isSubtypeOf(valueOf(parent));
        }

        IGJImmutability childImmutabtility = valueOf(child);
        IGJImmutability parentImmutabtility = valueOf(parent);
        return childImmutabtility.isSubtypeOf(parentImmutabtility);
    }
    
    public static boolean isEqual(AnnotationData a1, AnnotationData a2) {
        return isSubtype(a1, a2) && isSubtype(a2, a1);
    }

}
