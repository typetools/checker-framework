package checkers.javari;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.basetype.BaseTypeChecker;
import checkers.javari.quals.*;
import checkers.quals.PolyAll;
import checkers.quals.TypeQualifiers;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.TypeHierarchy;
import checkers.util.AnnotationUtils;

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
     * @param processingEnv the processing environment to use in the local AnnotationFactory
     */
    @Override
    public void initChecker(ProcessingEnvironment processingEnv) {
        AnnotationUtils annoFactory = AnnotationUtils.getInstance(processingEnv);
        this.READONLY = annoFactory.fromClass(ReadOnly.class);
        this.THISMUTABLE = annoFactory.fromClass(ThisMutable.class);
        this.MUTABLE = annoFactory.fromClass(Mutable.class);
        this.POLYREAD = annoFactory.fromClass(PolyRead.class);
        this.QREADONLY = annoFactory.fromClass(QReadOnly.class);
        this.ASSIGNABLE = annoFactory.fromClass(Assignable.class);
        super.initChecker(processingEnv);
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
            @Override
            protected boolean isSubtypeAsTypeArgument(AnnotatedTypeMirror rhs, AnnotatedTypeMirror lhs) {
                return lhs.hasEffectiveAnnotation(QREADONLY) || super.isSubtypeAsTypeArgument(rhs, lhs);
            }
         };
    }

    /**
     * Checks if one the parameters is primitive, or if a type is
     * subtype of another. Primitive types always pass to avoid issues
     * with boxing.
     */
    @Override
    public boolean isSubtype(AnnotatedTypeMirror sub, AnnotatedTypeMirror sup) {
        return sub.getKind().isPrimitive() || sup.getKind().isPrimitive() || super.isSubtype(sub, sup);
    }

}
