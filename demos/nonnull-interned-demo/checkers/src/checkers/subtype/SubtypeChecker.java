package checkers.subtype;

import java.io.*;
import java.lang.annotation.Annotation;
import java.util.*;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;

import checkers.quals.*;
import checkers.source.*;
import checkers.types.*;

import com.sun.source.tree.*;

/**
 * An annotation processor for typechecking qualifiers for which the qualified
 * type is the subtype of the unqualified type.
 *
 * <p>
 *
 * This class is intended for subclassing. It provides two primary services:
 * <ul>
 *  <li>{@link SubtypeVisitor}, an implementation of {@link SourceVisitor} that
 * scans programs for assignability violations with respect to the subtype
 * qualifier (i.e., if the qualified type of @A is the subtype of the
 * qualified type, the visitor checks that data with the unqualified type is
 * never assigned to variables with the qualified type)</li>
 *  <li>the {@link SubtypeChecker#isSubtype}
 * helper method for {@link SubtypeVisitor} and its potential subclasses</li>
 * </ul>
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({"*"})
@SupportedLintOptions({"cast", "cast:redundant"})
@DefaultQualifier("checkers.nullness.quals.NonNull")
public abstract class SubtypeChecker extends SourceChecker {

    /** The annotation to check (in isSubtype). */
    protected final Class<? extends Annotation> annotation;

    public Class<? extends Annotation> getAnnotation() {
        return this.annotation;
    }
    
    /**
     * Creates an annotation processor for checking subtype qualifiers. The
     * processor will perform checking using the annotation class named by the
     * value of the "annotation" JVM property, which may be set using the
     * "-Dannotation=[value]" command-line switch.
     */
    @SuppressWarnings("unchecked")
    public SubtypeChecker() {
        @Nullable String annotationName = System.getProperty("annotation");

        try {
            if (annotationName != null) /*nnbug*/
                this.annotation = (Class<? extends @NonNull Annotation>)Class.forName(annotationName);
            else throw new RuntimeException("invalid annotation name");
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("invalid annotation: " + annotationName);
        }
    }

    /**
     * Creates an annotation processor for checking subtype qualifiers.
     *
     * @param annotation the class of the annotation that this annotation
     * processor will check
     */
    public SubtypeChecker(Class<? extends Annotation> annotation) {
        this.annotation = annotation;
    }

    @Override
    protected Properties getMessages(String fileName) throws IOException {
        // Read messages from a file (in the working directory, by default, or
        // else in a directory indicated by the system "messages" property.
        @Nullable String path = System.getProperty("messages", ".");
        Properties p = new Properties();
        if (path != null) /*nnbug*/
            p.load(new FileInputStream(path + File.separator + fileName));
        return p;
    }

    /**
     * Determines whether the first type is a subtype of the second type
     * with respect to the annotations on raw types.  It ignores the actual
     * type and only checks annotations, and is only concerned with annotations
     * on raw types (it ignores all generic and array annotations).
     *
     * @param child the type that may or may not be a subtype
     * @param parent the parent type
     * @return true if {@code child} is a subtype of {@code parent} with respect
     *         to its type qualifier annotations, false otherwise
     */
    @Deprecated
    protected boolean isSubtypeIgnoringTypeParameters(AnnotatedClassType child, AnnotatedClassType parent) {
        if (!child.hasAnnotationAt(this.annotation, AnnotationLocation.RAW) &&
                parent.hasAnnotationAt(this.annotation, AnnotationLocation.RAW))
            return false;
        return true;
    }

    /**
     * Determines whether the first type is a subtype of the second type.
     * It ignores the actual type and only checks annotations.
     *
     * @param child the type that may or may not be a subtype
     * @param parent the parent type
     * @return true if {@code child} is a subtype of {@code parent} with respect
     *         to its type qualifier annotations, false otherwise
     */
    public boolean isSubtype(AnnotatedClassType child, AnnotatedClassType parent) {

        if (child.getTree() != null && child.getTree().getKind() == Tree.Kind.NULL_LITERAL &&
                !parent.hasAnnotationAt(this.annotation, AnnotationLocation.RAW))
            return true;
        
        if (child.getElement() != null) {
            Elements elts = env.getElementUtils();
            Element childElt = child.getElement();

            String className = InternalUtils.getQualifiedName(childElt);
            if (this.shouldSkip(className))
                return true;
        }

        if (!child.hasAnnotationAt(this.annotation, AnnotationLocation.RAW)
              && parent.hasAnnotationAt(this.annotation, AnnotationLocation.RAW))
            return false;

        for (AnnotationLocation location : 
                parent.getAnnotatedTypeArgumentLocations()) {
            if (!child.hasAnnotationAt(this.annotation, location)
                    && !child.hasWildcardAt(location) 
                    && parent.hasAnnotationAt(this.annotation, location)) 
                return false;
        }

        if (child.getUnderlyingType() != null 
                && (child.getUnderlyingType().getKind() == TypeKind.ARRAY))
            return true;

        boolean skipInner = false; 
        if (parent.getUnderlyingType() instanceof DeclaredType) {
            DeclaredType dt = (DeclaredType)parent.getUnderlyingType(); 
            if (dt.getTypeArguments().isEmpty())
                skipInner = true;
        }


        for (AnnotationLocation location : 
                child.getAnnotatedTypeArgumentLocations()) {
            if (!location.equals(AnnotationLocation.RAW) && skipInner)
                continue;
            if (!parent.hasAnnotationAt(this.annotation, location)
                    && !parent.hasWildcardAt(location)
                    && child.hasAnnotationAt(this.annotation, location))
                return false;
        }

        return true;
    }
}
