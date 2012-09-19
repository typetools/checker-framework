package checkers.javari;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import checkers.basetype.BaseTypeChecker;
import checkers.javari.quals.*;
import checkers.quals.PolyAll;
import checkers.quals.TypeQualifiers;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.QualifierHierarchy;
import checkers.types.TypeHierarchy;
import checkers.util.AnnotationUtils;
import checkers.util.GraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy;

/**
 * An annotation processor that checks a program's use of the Javari
 * type annotations ({@code @ReadOnly}, {@code @Mutable},
 * {@code @Assignable}, {@code @PolyRead} and {@code @QReadOnly}).
 *
 * @checker.framework.manual #javari-checker Javari Checker
 */
@TypeQualifiers( { ReadOnly.class, ThisMutable.class, Mutable.class,
    PolyRead.class, QReadOnly.class, PolyAll.class })
public class JavariChecker extends BaseTypeChecker {

    protected AnnotationMirror READONLY, THISMUTABLE, MUTABLE, POLYREAD, QREADONLY, ASSIGNABLE;

    /**
     * Initializes the checker: calls init method on super class,
     * creates a local AnnotationFactory based on the processing
     * environment, and uses it to create the protected
     * AnnotationMirrors used through this checker.
     */
    @Override
    public void initChecker() {
        Elements elements = processingEnv.getElementUtils();
        this.READONLY = AnnotationUtils.fromClass(elements, ReadOnly.class);
        this.THISMUTABLE = AnnotationUtils.fromClass(elements, ThisMutable.class);
        this.MUTABLE = AnnotationUtils.fromClass(elements, Mutable.class);
        this.POLYREAD = AnnotationUtils.fromClass(elements, PolyRead.class);
        this.QREADONLY = AnnotationUtils.fromClass(elements, QReadOnly.class);
        this.ASSIGNABLE = AnnotationUtils.fromClass(elements, Assignable.class);
        super.initChecker();
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
        return new JavariQualifierHierarchy(factory);
    }

    private final class JavariQualifierHierarchy extends GraphQualifierHierarchy {

        public JavariQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory, MUTABLE);
        }

        /**
         * Returns a singleton collection with the most restrictive immutability
         * annotation that is a supertype of the annotations on both collections.
         */
        @Override
        public Set<AnnotationMirror> leastUpperBounds(Collection<AnnotationMirror> c1,
                Collection<AnnotationMirror> c2) {
            Map<String, AnnotationMirror> ann =
                new HashMap<String, AnnotationMirror>();
            for (AnnotationMirror anno : c1)
                ann.put(AnnotationUtils.annotationName(anno).toString(), anno);
            for (AnnotationMirror anno : c2)
                ann.put(AnnotationUtils.annotationName(anno).toString(), anno);

            if (ann.containsKey(QReadOnly.class.getCanonicalName()))
                return Collections.singleton(QREADONLY);
            else if (ann.containsKey(ReadOnly.class.getCanonicalName()))
                return Collections.singleton(READONLY);
            else if (ann.containsKey(PolyRead.class.getCanonicalName()))
                return Collections.singleton(POLYREAD);
            else
                return Collections.singleton(MUTABLE);
        }
    }

    /**
     * Implements the {@code @QReadOnly} behavior on generic types,
     * creating a new {@link TypeHierarchy} class that allows a
     * comparison of type arguments to succeed if the left hand side
     * is annotated with {@code @QReadOnly} or if the regular
     * comparison succeeds.
     */
    @Override
    protected TypeHierarchy createTypeHierarchy() {
        return new TypeHierarchy(this, getQualifierHierarchy()) {
            /**
             * Checks if one the parameters is primitive, or if a type is
             * subtype of another. Primitive types always pass to avoid issues
             * with boxing.
             */
            @Override
            public boolean isSubtype(AnnotatedTypeMirror sub, AnnotatedTypeMirror sup) {
                return sub.getKind().isPrimitive() || sup.getKind().isPrimitive() || super.isSubtype(sub, sup);
            }

            @Override
            protected boolean isSubtypeAsTypeArgument(AnnotatedTypeMirror rhs, AnnotatedTypeMirror lhs) {
                return lhs.hasEffectiveAnnotation(QREADONLY) || super.isSubtypeAsTypeArgument(rhs, lhs);
            }
         };
    }

}
