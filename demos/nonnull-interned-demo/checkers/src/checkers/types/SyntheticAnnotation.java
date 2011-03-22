package checkers.types;

import checkers.quals.*;

import java.util.*;

import javax.annotation.processing.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

/**
 * Represents an annotation that was not written as part of a source or class
 * file, but rather was generated. This is primarily useful for annotations
 * that are implicitly present on types (e.g., for adding a {@code @NonNull}
 * annotation to a {@link String} literal in a pre-processing step before
 * typechecking).
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
class SyntheticAnnotation implements AnnotationData {
    
    /** The type of the annotation. */
    private final TypeMirror type;

    /** The location of the annotation. */
    private final AnnotationLocation location;

    /** The values of the annotation's arguments. */
    private final Map<? extends ExecutableElement, ? extends AnnotationValue> values;

    /** Types utilities, for comparing types. */
    private final Types types;

    /**
     * Creates an annotation from the given type and location with no argument
     * values.
     *
     * @param the annotation's type
     * @param the annotation's location
     * @param env the current processing environment
     */
    SyntheticAnnotation(TypeMirror type, AnnotationLocation location,
            ProcessingEnvironment env) {
        this(type, location, new HashMap<ExecutableElement, AnnotationValue>(), env);
    }

    /**
     * Creates an annotation from the given type and location with no argument
     * values.
     *
     * @param type  the annotation's type
     * @param location  the annotation's location
     * @param values    the annotation's argument values
     * @param env the current processing environment
     */
    SyntheticAnnotation(TypeMirror type, AnnotationLocation location, Map<? extends ExecutableElement, ? extends AnnotationValue> values, ProcessingEnvironment env) {
        this.type = type;
        this.location = location;
        this.values = new HashMap<ExecutableElement, AnnotationValue>(values);
        this.types = env.getTypeUtils();
    }
    
    public TypeMirror getType() {
        return this.type;
    }

    public AnnotationLocation getLocation() {
        return this.location;
    }

    public Map<? extends ExecutableElement, ? extends AnnotationValue>
            getValues() {
        return this.values;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof AnnotationData) {
            AnnotationData s = (AnnotationData)o;
            return types.isSameType(getType(), s.getType())
                    && getLocation().equals(s.getLocation())
                    && getValues().equals(s.getValues());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 17 * type.toString().hashCode() + 19 * location.hashCode() 
                + 31 * values.hashCode();        
    }

    @Override
    public String toString() {
        return String.format("[%s@%s %s]",
                this.getType(),
                this.getLocation(),
                this.getValues());
    }
}
