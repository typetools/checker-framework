package checkers.types;

import checkers.quals.*;

import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

/**
 * Creates new annotations as {@link AnnotationMirror}s that may be added to
 * {@link AnnotatedTypeMirror}s. 
 */ 
@DefaultQualifier("checkers.nullness.quals.NonNull")
public class AnnotationFactory {

    /** The environment to use. */
    private final ProcessingEnvironment env;

    /** Element utilities for acquiring {@link TypeMirror}s from strings. */
    private final Elements elements;

    /** Caching for annotation creation. */
    private final Map<CharSequence, AnnotationMirror> annotationsFromNames;
    
    /**
     * Creates an annotation factory.
     *
     * @param env the {@link ProcessingEnvironment} for this factory
     */
    public AnnotationFactory(ProcessingEnvironment env) {
        this.env = env;
        this.elements = env.getElementUtils();
        this.annotationsFromNames = new HashMap<CharSequence, AnnotationMirror>();
    }
    

    /**
     * Creates an {@link AnnotationMirror} given by a particular
     * fully-qualified name.
     *
     * @param name the name of the annotation to create
     * @return an {@link AnnotationMirror} of type {@code} name, or null if
     * {@code name} does not correspond to an annotation type
     */
    public AnnotationMirror fromName(CharSequence name) {
        if (annotationsFromNames.containsKey(name))
            return annotationsFromNames.get(name);
        final DeclaredType annoType = typeFromName(name);
        AnnotationMirror result = new AnnotationMirror() {
            public DeclaredType getAnnotationType() {
                return annoType;
            }
            public Map<? extends ExecutableElement, ? extends AnnotationValue>
                getElementValues() {
                return Collections.emptyMap();
            }
            @Override
            public String toString() {
                return "@" + annoType;
            }
        };
        annotationsFromNames.put(name, result);
        return result;
    }
    
    /** 
     * A utility method that converts a {@link CharSequence} (usually a {@link
     * String}) into a {@link TypeMirror} named thereby.
     *
     * @param name the name of a type
     * @return the {@link TypeMirror} corresponding to that name
     */
    protected DeclaredType typeFromName(CharSequence name) {

        /*@Nullable*/ TypeElement typeElt = elements.getTypeElement(name);
        if (typeElt == null)
            throw new IllegalArgumentException("invalid name: " + name);

        TypeMirror result = typeElt.asType();
        if (!(result instanceof DeclaredType))
            throw new AssertionError("not a declared type");

        return (DeclaredType)result;
    }
}
