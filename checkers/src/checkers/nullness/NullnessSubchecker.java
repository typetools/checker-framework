package checkers.nullness;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.basetype.BaseTypeChecker;
import checkers.nullness.quals.*;
import checkers.quals.TypeQualifiers;
import checkers.source.*;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import checkers.util.AnnotationUtils;
import checkers.util.GraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy;

/**
 * A typechecker plug-in for the Nullness type system qualifier that finds (and
 * verifies the absence of) null-pointer errors.
 *
 * @see NonNull
 * @see Nullable
 * @see Raw
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@TypeQualifiers({ Nullable.class, LazyNonNull.class, NonNull.class, PolyNull.class, Primitive.class })
@SupportedLintOptions({"nulltest", "uninitialized", "advancedchecks"})
public class NullnessSubchecker extends BaseTypeChecker {

    // warn about uninitialized primitive and nullable fields in the constructor
    public static final boolean UNINIT_DEFAULT = false;
    // warn about a null check performed against a value that is guaranteed
    // to be non-null, as in:  "m" == null.
    public static final boolean NULLTEST_DEFAULT = false;

    // TODO: This lint option should only be temporary, until all checks are implemented correctly.
    public static final boolean ADVANCEDCHECKS_DEFAULT = false;

    protected AnnotationMirror NONNULL, NULLABLE, PRIMITIVE;

    @Override
    public void initChecker(ProcessingEnvironment processingEnv) {
        super.initChecker(processingEnv);
        AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);
        NONNULL = annoFactory.fromClass(NonNull.class);
        NULLABLE = annoFactory.fromClass(Nullable.class);
        PRIMITIVE = annoFactory.fromClass(Primitive.class);
    }
    
    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
        // At most a single qualifier on a type
        if (useType.getAnnotations().size() > 1) {
            return false;
        }
        return super.isValidUse(declarationType, useType);
    }

    @Override
    public boolean isValidUse(AnnotatedPrimitiveType type) {
        // No explicit qualifiers on primitive types
        if (type.getAnnotations().size()>1 ||
             (type.getAnnotation(Primitive.class)==null &&
             // Flow inference might implicitly add a NonNull, therefore
             // check whether the element contained a type annotation.
             // Note that non-type annotations, e.g. SuppressWarnings, might
             // be present on the element.
             // The element is null if the primitive type is an array component ->
             // always a reason to warn.
             (type.getElement()==null ||
             AnnotatedTypes.containsTypeAnnotation(type.getElement().getAnnotationMirrors())))) {
            System.out.println("Looking at type: " + type);
            System.out.println("  the element: " + type.getElement());
            System.out.println("  the element annotations: " + (type.getElement()!=null ? type.getElement().getAnnotationMirrors() : "null"));
            new Throwable().printStackTrace();
            return false;
        }
        return super.isValidUse(type);
    }

    /*
    @Override
    protected MultiGraphQualifierHierarchy.MultiGraphFactory createQualifierHierarchyFactory() {
        // TODO: actually use the MultiGraphQH.
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }*/

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new NullnessQualifierHierarchy((/*Multi*/GraphQualifierHierarchy)super.createQualifierHierarchy());
    }
    
    private final class NullnessQualifierHierarchy extends MultiGraphQualifierHierarchy {
        public NullnessQualifierHierarchy(/*Multi*/GraphQualifierHierarchy hierarchy) {
            super(hierarchy);
        }

        @Override
        public boolean isSubtype(AnnotationMirror sub, AnnotationMirror sup) {
            if ( AnnotationUtils.areSame(sub, PRIMITIVE) &&
                    AnnotationUtils.areSame(sup, NONNULL) ) {
                return true;
            }
            return super.isSubtype(sub, sup);
        }
    }
}
