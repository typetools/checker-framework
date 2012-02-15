package checkers.types;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.*;

import checkers.quals.*;
import checkers.util.ElementUtils;

import com.sun.source.tree.Tree;

/**
 * {@link AnnotatedClassType} represents a type along with all its annotations.
 * Given a use of a type, clients can obtain an {@link AnnotatedClassType} and
 * query which annotations the type has.
 * <p>
 *
 * This class abstracts the sources of the annotations: written
 * directly on the type, inherited from the supertype, specified via a
 * "default" annotation, or implicitly present (e.g., {@code @NonNull} for
 * {@link String} literals).
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
public class AnnotatedClassType {

    /** The element node related to this annotated type. */
    protected Element element;

    /** The tree node related to this annotated type. */
    protected Tree tree;
    
    protected TypeMirror type;

    /**
     * Annotation types that were written directly on the class type.
     * @see #annotate(AnnotationData)
     */
    protected final AnnotationSet<AnnotationData> annos;

    /**
     * Annotation types treated as though they were written on the class type.
     * @see #include(Class&lt;? extends Annotation&gt; annotation)
     */
    protected final AnnotationSet<AnnotationData> includes;

    /**
     * Annotation types that should be ignored if present.
     * @see #exclude(Class&lt;? extends Annotation&gt; annotation)
     */
    protected final AnnotationSet<AnnotationData> excludes;

    /** The processing environment (for utilities and context). */
    protected final ProcessingEnvironment env;

    /** The factory used for including/excluding annotations. */
    protected final AnnotationFactory annotations;

    /**
     * A class for storing annotations (in this frameworks internal
     * representation, {@link AnnotationData}). It provides membership testing
     * by annotation type and location.
     */
    protected static class AnnotationSet<A extends AnnotationData>
        extends HashSet<A> {

        protected ProcessingEnvironment env;
        
        /**
         * Determines whether or not this {@link AnnotationSet} contains an annotation
         * with the given type at the given location.
         *
         * @param annotation the type of the annotation to test for
         * @param location the location of the annotation to test for
         * @return true if there exists at least one annotation in this set
         *         with the given type and location, false otherwise
         */
        public boolean contains(TypeMirror annotation, AnnotationLocation location) {
            for (A ad : this) {
                if (env.getTypeUtils().isSameType(ad.getType(), annotation)
                        && ad.getLocation().equals(location))
                    return true;
            }
            return false;
        }
    }

    /**
     * Creates an annotated class type.
     *
     * @param env the {@link ProcessingEnvironment} for the checker creating
     *            this type
     */
    public AnnotatedClassType(ProcessingEnvironment env) {

        // Initialize annotation storage.
        this.annos = new AnnotationSet<AnnotationData>();
        annos.env = env;
        this.includes = new AnnotationSet<AnnotationData>();
        includes.env = env;
        this.excludes = new AnnotationSet<AnnotationData>();
        excludes.env = env;
        
        this.env = env;
        this.annotations = new AnnotationFactory(env);
    }

    /**
     * Determines whether the annotated type has an annotation with a certain name
     * at a certain location.
     *
     * @param annotation
     *            the name of the annotation to check for
     * @param location
     *            the location to check
     * @param implicit whether or not to check included/excluded annotations
     * @return true if the annotation with the given name is present at the
     *         given location, false otherwise
     */
    public boolean hasAnnotationAt(CharSequence annotation,
            AnnotationLocation location, boolean implicit) {

        TypeMirror type = this.annotations.typeFromName(annotation);

        if (implicit) {
            // If this type explicitly excludes this annotation type, regard it
            // as not annotated with the annotation.
            if (excludes.contains(type, location))
                return false;

            if (includes.contains(type, location))
                return true;
        }

        if (annos.contains(type, location))
            return true;

        return false;
    }

    /**
     * Determines whether the annotated type has an annotation with a certain name
     * at a certain location, checking included/excluded annotations by default.
     *
     * @param annotation
     *            the name of the annotation to check for
     * @param location
     *            the location to check
     * @return true if the annotation with the given name is present at the
     *         given location, false otherwise
     */
    public boolean hasAnnotationAt(CharSequence annotation,
            AnnotationLocation location) {
        return hasAnnotationAt(annotation, location, true);
    }

    /**
     * Determines whether the annotated type has an annotation of a certain
     * class at a certain location.
     *
     * @param annotation the {@link Class} of the annotation to check for
     * @param location the location to check
     * @param implicit whether or not to check included/excluded annotations
     * @return true if the annotation with the given class is present at the
     *         given location, false otherwise
     */
    public boolean hasAnnotationAt(Class<? extends Annotation> annotation,
            AnnotationLocation location, boolean implicit) {
        return hasAnnotationAt(annotation.getName(), location, implicit);
    }

    /**
     * Determines whether the annotated type has an annotation of a certain
     * class at a certain location, checking included/excluded annotations by
     * default.
     *
     * @param annotation the {@link Class} of the annotation to check for
     * @param location the location to check
     * @return true if the annotation with the given class is present at the
     *         given location, false otherwise
     */
    public boolean hasAnnotationAt(Class<? extends Annotation> annotation,
            AnnotationLocation location) {
        return hasAnnotationAt(annotation.getName(), location);
    }

    /**
     * Determines whether the type backing this annotated type has a wildcard
     * ("?") at location specified by the given integer list.
     *
     * @param location
     *            location at which to check for a wildcard
     * @return true if there is a wildcard at the given location, false
     *         otherwise
     */
    public boolean hasWildcardAt(AnnotationLocation location) {

        if (element == null)
            return false;

        TypeMirror type = element.asType();

        if (!(type instanceof DeclaredType))
            return false;

        @Nullable TypeMirror typeAtLocation = location.getTypeFrom(type);

        return (typeAtLocation instanceof WildcardType);
    }

    /**
     * Adds the given annotation, represented by an {@link AnnotationData},
     * to this class type at the location provided by the
     * {@link AnnotationData}.
     *
     * @param annotation the annotation to add
     */
    public void annotate(AnnotationData annotation) {
        this.annos.add(annotation);
    }

    public void annotate(AnnotationData annotation, AnnotationData override) {
        this.annos.remove(override);
        this.annos.add(annotation);
    }

    /**
     * Forces the given annotation to be included among the annotations checked
     * by {@link AnnotatedClassType#hasAnnotationAt}. This is useful for
     * treating class types as having an annotation even though they don't
     * (e.g., a primitive type that is implicitly {@code @NonNull}.
     *
     * @param annotation the annotation to include among this type's annotations
     * @see AnnotationFactory
     */
    public void include(AnnotationData annotation) {

        if (annotation == null)
            throw new IllegalArgumentException("can't include null");

        this.excludes.remove(annotation);
        this.includes.add(annotation);
    }

    /**
     * Forces an annotation with the given name to be included among the
     * annotations checked by {@link AnnotatedClassType#hasAnnotationAt}. This is useful for
     * treating class types as having an annotation even though they don't
     * (e.g., a primitive type that is implicitly {@code @NonNull}.
     *
     * @param annotation
     *            the name of the annotation to include among this type's
     *            annotations
     */
    public void include(CharSequence annotation) {

        if (annotation == null)
            throw new IllegalArgumentException("can't include null");

        AnnotationData syn = annotations.createAnnotation(annotation,
                AnnotationLocation.RAW);

        include(syn);
    }

    /**
     * Forces an annotation of the given type to be included among the
     * annotations checked by {@link AnnotatedClassType#hasAnnotationAt}. This is useful for
     * treating class types as having an annotation even though they don't
     * (e.g., a primitive type that is implicitly {@code @NonNull}.
     *
     * @param annotation
     *            the {@link Class} of the annotation to include among this
     *            type's annotations
     */
    public void include(Class<? extends Annotation> annotation) {
        include(annotation.getName());
    }

    /**
     * Forces an annotation of the given type to be included at the
     * specified position among the annotations checked by {@link
     * AnnotatedClassType#hasAnnotationAt}. This is useful for
     * treating class types as having an annotation even though they
     * don't (e.g., a primitive type that is implicitly {@code
     * @NonNull}.
     *
     * @param annotation
     *            the {@link Class} of the annotation to include among this
     *            type's annotations
     * @param location
     *            the location at which the annotation should be included
     *
     */
    public void includeAt(Class<? extends Annotation> annotation,
                          AnnotationLocation location) {
        AnnotationData annotationData =
            annotations.createAnnotation(annotation.getName(), location);
        include(annotationData);
    }


    /**
     * Forces the given annotation to be skipped by {@code hasAnnotationAt}.
     * Useful in conjunction with annotation defaults (e.g., if variables are
     * {@code @NonNull} by default and the checker finds a {@code @Nullable}
     * variable, it can simply exclude {@code @NonNull} rather than handle
     * {@code @Nullable} as a special case).
     *
     * @param annotation the annotation to exclude from this type's annotations
     * @see AnnotationFactory
     */
    public void exclude(AnnotationData annotation) {

        if (annotation == null)
            throw new IllegalArgumentException("can't exclude null");

        this.includes.remove(annotation);
        this.excludes.add(annotation);
    }

    /**
     * Forces an annotation with the given name to be skipped by
     * {@code hasAnnotationAt}. Useful in conjunction with annotation defaults
     * (e.g., if variables are {@code @NonNull} by default and the checker finds
     * a {@code @Nullable} variable, it can simply exclude {@code @NonNull}
     * rather than handle {@code @Nullable} as a special case).
     *
     * @param annotation
     *            the name of the annotation to exclude from this type's
     *            annotations
     */
    public void exclude(CharSequence annotation) {

        if (annotation == null)
            throw new IllegalArgumentException("can't exclude null");

        AnnotationData syn = annotations.createAnnotation(annotation,
                AnnotationLocation.RAW);

        exclude(syn);
    }

    /**
     * Forces an annotation of the given type to be skipped by
     * {@code hasAnnotationAt}. Useful in conjunction with annotation defaults
     * (e.g., if variables are {@code @NonNull} by default and the checker finds
     * a {@code @Nullable} variable, it can simply exclude {@code @NonNull}
     * rather than handle {@code @Nullable} as a special case).
     *
     * @param annotation
     *            the {@link Class} of the annotation to exclude from this
     *            type's annotations
     */
    public void exclude(Class<? extends Annotation> annotation) {
        exclude(annotation.getName());
    }

    /**
     * Forces the given annotation to be skipped by {@code
     * hasAnnotationAt} at the given location.  Useful in conjunction
     * with annotation defaults (e.g., if variables are {@code
     * @NonNull} by default and the checker finds a {@code @Nullable}
     * variable, it can simply exclude {@code @NonNull} rather than
     * handle {@code @Nullable} as a special case).
     *
     * @param annotation the annotation to exclude from this type's annotations
     * @param location the location at which the annotation should be excluded
     */
    public void excludeAt(Class<? extends Annotation> annotation,
                          AnnotationLocation location) {
        AnnotationData annotationData
            = annotations.createAnnotation(annotation.getName(), location);
        exclude(annotationData);
    }

    /**
     * Sets this type's element reference to the given element.
     *
     * @param e
     *            the element to associate with this type
     */
    public void setElement(Element e) {
        this.element = e;
    }

    /**
     * @return the element associated with this type
     */
    public Element getElement() {
        return this.element;
    }

    /**
     * Sets this type's tree reference to the given tree.
     *
     * @param t the tree to associate with this type
     */
    public void setTree(Tree t) {
        this.tree = t;
    }

    /**
     * @return the tree associated with this type
     */
    public Tree getTree() {
        return tree;
    }

    /**
     * @return the underlying TypeMirror associated with this type
     */
    public TypeMirror getUnderlyingType() {
        // defensive
        if (type == null)
            return ElementUtils.getType(element);
        return type;
    }
    
    /**
     * sets the underlying TypeMirror associated with this type
     * 
     * @param type  underlying TypeMirror
     */
    public void setUnderlyingType(TypeMirror type) {
        this.type = type;
    }
    
    /**
     * @return the locations of all annotations in this class type
     *
     * @todo should take an annotation argument
     */
    public Set<AnnotationLocation> getAnnotatedLocations() {
        return getAnnotatedLocations(true);
    }

    /**
     * @param implicit
     *     indicates whether included and execluded annotations are considered
     * @return the location of all annotations in the class type
     */
    public Set<AnnotationLocation> getAnnotatedLocations(boolean implicit) {
        Set<AnnotationLocation> locs = new HashSet<AnnotationLocation>();
        for (AnnotationData a : getAnnotationData(implicit))
            locs.add(a.getLocation());

        return Collections.<@NonNull AnnotationLocation>unmodifiableSet(locs);
    }

    /**
     * @return the locations of all annotations in this class types,
     * excluding the location of annotations on raw types ({@link
     * AnnotationLocation#RAW}).  "Type argument" annotations includes both
     * generics and array levels.
     */
    public Set<AnnotationLocation> getAnnotatedTypeArgumentLocations() {
        Set<AnnotationLocation> locs = this.getAnnotatedLocations();
        if (!locs.contains(AnnotationLocation.RAW))
            return locs;

        Set<AnnotationLocation> typeargs = new HashSet<AnnotationLocation>();
        typeargs.addAll(locs);
        typeargs.remove(AnnotationLocation.RAW);
        return Collections.<@NonNull AnnotationLocation>unmodifiableSet(typeargs);
    }

    /**
     * Return the annotations of this
     * @param implicit  whether included annotations are to be considered
     *
     * @return a {@code Collection} of annotations annotating this type
     */
    public Set<AnnotationData> getAnnotationData(boolean implicit) {
        Set<AnnotationData> annotations = new HashSet<AnnotationData>();
        annotations.addAll(annos);
        if (implicit) {
            annotations.addAll(includes);
            annotations.removeAll(excludes);
        }
        return Collections.<@NonNull AnnotationData>unmodifiableSet(annotations);
    }

    /**
     * Retrieves {@link AnnotationData} for a certain annotation type on the
     * given annotation type.
     *
     * @param name the type of the annotation to retrieve
     * @param implicit true if annotations added via {@link
     *        AnnotatedClassType#include} should be added to the result
     * @return the set of annotations with the given name on this type
     */
    public Set<AnnotationData> getAnnotationData(
            CharSequence name, boolean implicit) {

        TypeMirror type = annotations.typeFromName(name);

        Set<AnnotationData> annos = new HashSet<AnnotationData>();

        for (AnnotationData a : getAnnotationData(implicit))
            if (env.getTypeUtils().isSameType(a.getType(), type))
                annos.add(a);

        return annos;
    }

    /**
     * Retrieves {@link AnnotationData} for a certain annotation type on the
     * given annotation type.
     *
     * @param annotation the type of the annotation to retrieve
     * @param implicit true if annotations added via {@link
     *        AnnotatedClassType#include} should be added to the result
     * @return the set of annotations with the given class on this type
     */
    public Set<AnnotationData> getAnnotationData(
            Class<? extends Annotation> annotation, boolean implicit) {
        if (annotation == null) return Collections.<@NonNull AnnotationData>emptySet();
        return this.getAnnotationData(annotation.getName(), implicit);
    }

    @Override
    public String toString() {
        return this.annos + " / " + this.includes + " / " + this.excludes;
    }

    public String toCondensedString() {
        return this.toString() + " (" + this.getAnnotationData(true).toString() + ")";
    }

    @Override
    public int hashCode() {
        int hashCode = 37;

        if (element != null)
            hashCode += 13 * element.hashCode();

        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AnnotatedClassType))
            return false;
        AnnotatedClassType other = (AnnotatedClassType) obj;

        return ((this.element != null)
        		&& (other.element != null)
            	&& (this.element.equals(other.element)));
    }
}
