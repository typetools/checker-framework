package checkers.types;

import checkers.quals.*;

import com.sun.source.tree.Tree;

import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

/**
 * Creates new annotations or converts existing annotations in the internal
 * annotation representation ({@link AnnotationData}) used by this framework.
 */ 
@DefaultQualifier("checkers.nullness.quals.NonNull")
public class AnnotationFactory {

    // TODO: move creation methods from InternalAnnotation to this class
    
    /** The environment to use. */
    private final ProcessingEnvironment env;

    /** Element utilities for acquiring {@link TypeMirror}s from strings. */
    private final Elements elements;

    /**
     * Creates an annotation factory.
     *
     * @param env the {@link ProcessingEnvironment} for this factory
     */
    public AnnotationFactory(ProcessingEnvironment env) {
        this.env = env;
        this.elements = env.getElementUtils();
    }
    
    /**
     * Creates an annotation with the given name and location.
     *
     * @param name the name of the annotation
     * @param location the annotation's location on a type
     * @return {@link AnnotationData} corresponding to the given name and location
     */
    public AnnotationData createAnnotation(CharSequence name, AnnotationLocation location) {
        return new SyntheticAnnotation(typeFromName(name), location, env);
    }
    
    /**
     * Creates an annotation with the given name and location, with the given values
     *
     * @param name the name of the annotation
     * @param location the annotation's location on a type
     * @param values    the arguments for the annotation type
     * @return {@link AnnotationData} corresponding to the given name, location, and values
     */
    public AnnotationData createAnnotation(CharSequence name, AnnotationLocation location, 
            Map<? extends ExecutableElement, ? extends AnnotationValue> values) {
        return new SyntheticAnnotation(typeFromName(name), location, values, env);
    }
    
    /**
     * Creates an annotation given an existing annotation (as represented by
     * the compiler).
     *
     * @param annotation an existing annotation
     * @return {@link AnnotationData} corresponding to the existing annotation
     */
    public InternalAnnotation createAnnotation(AnnotationMirror mirror) {
        return new InternalAnnotation(mirror, env);
    }

    /**
     * @param e an element containing annotations
     * @return the annotations contained in the given element
     */
    public List<InternalAnnotation> createAnnotations(@Nullable Element e) {
        if (e == null) /*nnbug*/
            return new LinkedList<InternalAnnotation>();
        return InternalAnnotation.fromElement(e, env);
    }

    /**
     * @param e an element containing annotations
     * @param tree a subtree of the element
     * @return the annotations on the given element that belong to the given
     *         tree
     */
    public List<InternalAnnotation> createAnnotations(Element e, Tree tree) {
        return InternalAnnotation.fromElement(e, tree, env);
    }
    
    /** 
     * A utility method that converts a {@link CharSequence} (usually a {@link
     * String}) into a {@link TypeMirror} named thereby.
     *
     * @param name the name of a type
     * @return the {@link TypeMirror} corresponding to that name
     */
    public TypeMirror typeFromName(CharSequence name) {

        @Nullable Element typeElt = elements.getTypeElement(name);
        if (typeElt == null)
            throw new IllegalArgumentException("invalid name: " + name);

        return typeElt.asType();
    }
}
