package checkers.types;

import checkers.quals.*;

import java.util.*;

import javax.lang.model.element.Element;

import com.sun.source.util.TreeScanner;

/**
 * Represents a group of annotations and their associated element. Useful for
 * {@link TreeScanner}s.
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
final class InternalAnnotationGroup implements Iterable<InternalAnnotation> {

    /** An annotation group with no annotations. */
    public static final InternalAnnotationGroup EMPTY = new InternalAnnotationGroup(
            Collections.<InternalAnnotation> emptyList(), null);

    /** The annotations in the group. */
    private final List<InternalAnnotation> annotations;

    /** The element associated with the annotations in the group. */
    private final Element element;

    /**
     * Creates a new InternalAnnotationGroup.
     * 
     * @param annotations
     *            the annotations in the group
     * @param element
     *            the element associated with the group
     */
    public InternalAnnotationGroup(
            List<InternalAnnotation> annotations, Element element) {
        this.annotations = annotations;
        this.element = element;
    }
    
    /**
     * @return the elements in the annotation group
     */
    public List<InternalAnnotation> getAnnotations() {
        return Collections.<@NonNull InternalAnnotation>unmodifiableList(annotations);
    }

    /**
     * @return the element associated with the annotation group, if any
     */
    public Element getElement() {
        return this.element;
    }

    /**
     * Determines whether the annotation group has an associated element.
     * 
     * @return true if the annotation group has an associated element, false
     *         otherwise
     */
    public boolean hasElement() {
        return this.element != null;
    }

    /**
     * Returns an iterator for the {@link InternalAnnotation}s in this group.
     * 
     * @return an iterator for the annotations in this group
     */
    public Iterator<InternalAnnotation> iterator() {
        return annotations.iterator();
    }
}
