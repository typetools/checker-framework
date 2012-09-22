package checkers.igj;

import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import checkers.basetype.BaseTypeChecker;
import checkers.igj.quals.*;
import checkers.quals.TypeQualifiers;
import checkers.source.SourceChecker;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.*;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;


/**
 * A type-checker plug-in for the IGJ immutability type system that finds (and
 * verifies the absence of) undesired side-effect errors.
 *
 * The IGJ language is a Java language extension that expresses immutability
 * constraints, using six annotations: {@link ReadOnly}, {@link Mutable},
 * {@link Immutable}, {@link I} -- a polymorphic qualifier, {@link Assignable},
 * and {@link AssignsFields}.  The language is specified by the FSE 2007 paper.
 *
 * @checker.framework.manual #igj-checker IGJ Checker
 *
 */
@TypeQualifiers({ ReadOnly.class, Mutable.class, Immutable.class, I.class,
    AssignsFields.class, IGJBottom.class })
public class IGJChecker extends BaseTypeChecker {
    //
    // IGJ tries to adhere to the various rules specified by the
    // type system and the conventions of the framework, except for two
    // things:
    // 1. overloading the meaning of BOTTOM_QUAL
    //    Review the javadoc of #createQualiferHierarchy
    //
    // 2. Having two qualifiers for a given type in one particular case
    //    which is that the self type (i.e. type of 'this' identifier) within
    //    a method with an AssignsFields receiver within I classes, then the self type is
    //    '@AssignsFields @I EnclosingClass' and they are treated as
    //    Incomparable.  This is useful in the following cases:
    //
    //    a. for method invocability tests, a method with an AssignsFields receiver from within
    //       a readonly context can be called only via AssignsFields reference
    //       of 'this'.  I cannot be a receiver type, so it doesn't interfere.
    //
    //    b. for assignment, 'this' can be assigned to '@I EnclosingClass'
    //       reference within such methods (assignment encompasses the escape
    //       of this when passed to method parameters).  Fields and variables
    //       cannot be AssignsFields, so it's safe.
    //
    //    The design of QualifierHierarchy.isSubtype(Collection, Collection)
    //    reflect this choice.
    //
    /** Supported annotations for IGJ.  Used for subtyping rules. **/
    protected AnnotationMirror READONLY, MUTABLE, IMMUTABLE, I, ASSIGNS_FIELDS, BOTTOM_QUAL;

    @Override
    public void initChecker() {
        Elements elements = processingEnv.getElementUtils();
        READONLY = AnnotationUtils.fromClass(elements, ReadOnly.class);
        MUTABLE = AnnotationUtils.fromClass(elements, Mutable.class);
        IMMUTABLE = AnnotationUtils.fromClass(elements, Immutable.class);
        I = AnnotationUtils.fromClass(elements, I.class);
        ASSIGNS_FIELDS = AnnotationUtils.fromClass(elements, AssignsFields.class);
        BOTTOM_QUAL = AnnotationUtils.fromClass(elements, IGJBottom.class);
        super.initChecker();
    }

    // **********************************************************************
    // Factory methods
    // **********************************************************************

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new IGJQualifierHierarchy(factory);
    }

    @Override
    protected TypeHierarchy createTypeHierarchy() {
        return new IGJTypeHierarchy(this, getQualifierHierarchy());
    }

    //
    // IGJ makes an interesting use of BOTTOM_QUAL (IGJBottom).  It gets used
    // in two ways:
    //
    // __ AS BOTTOM QUALIFIER __
    //
    // This is the intended use and design for it
    //
    // A bottom qualifier is needed to annotate some expressions, like 'null'.
    // Otherwise, null would need to be '@Mutable @Immutable <nulltype>' so
    // it could be assigned to everything.
    //
    // __ AS SUPER QUALIFIER __
    //
    // As I used IGJ, I realized that it's useful to have an annotation
    // that acts as a place holder qualifier that is a supertype of
    // everything.  The semantics of such qualifier is a bit
    // different from ReadOnly.
    //
    // It's only because the existence of un-annotated code that we need this
    // annotation, and IGJ's promise that un-annotated code should type check.
    //
    // TODO: Explain these cases more
    //
    private final class IGJQualifierHierarchy extends GraphQualifierHierarchy {
        public IGJQualifierHierarchy(MultiGraphFactory factory) {
            super(factory, BOTTOM_QUAL);
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(lhs, I) &&
                    AnnotationUtils.areSameIgnoringValues(rhs, I)) {
                return AnnotationUtils.areSame(lhs, rhs);
            }
            // Ignore annotation values to ensure that annotation is in supertype map.
            if (AnnotationUtils.areSameIgnoringValues(lhs, I)) {
                lhs = I;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, I)) {
                rhs = I;
            }
            return (AnnotationUtils.areSame(rhs, BOTTOM_QUAL)
                    || AnnotationUtils.areSame(lhs, BOTTOM_QUAL)
                    || super.isSubtype(rhs, lhs));
        }

        @Override
        public boolean isSubtype(Collection<AnnotationMirror> rhs, Collection<AnnotationMirror> lhs) {
            if (lhs.isEmpty() || rhs.isEmpty()) {
                SourceChecker.errorAbort("GraphQualifierHierarchy: Empty annotations in lhs: " + lhs + " or rhs: " + rhs);
            }
            // TODO: sometimes there are multiple mutability annotations in a type and
            // the check in the superclass that the sets contain exactly one annotation
            // fails. I replaced "addAnnotation" calls with "replaceAnnotation" calls,
            // but then other test cases fail. Some love needed here.
            for (AnnotationMirror lhsAnno : lhs) {
                for (AnnotationMirror rhsAnno : rhs) {
                    if (isSubtype(rhsAnno, lhsAnno)) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    /**
     * Represents the annotated type hierarchy of the IGJ type system.
     *
     * The IGJ type system diverges from the JLS in two ways:
     * 1. Type arguments are always co-variant with respect to
     *    {@link IGJBottom}
     *
     * 2. If the type is a read-only or an immutable type, then type arguments
     *    may change co-variantly in a safe manner
     */
    private final class IGJTypeHierarchy extends TypeHierarchy {
        public IGJTypeHierarchy(IGJChecker checker, QualifierHierarchy qualifierHierarchy) {
            super(checker, qualifierHierarchy);
        }

        /**
         * Returns true if either of the provided types is a
         * {@link IGJBottom}, otherwise uses the JLS specification
         * implemented by the abstract {@link typeHierarchy}.
         *
         */
        // Note: This cannot be expressed with the QualifierHierarchy alone,
        // as TypeHierarchy requires type arguments to be equivalent
        @Override
        protected boolean isSubtypeAsTypeArgument(AnnotatedTypeMirror rhs, AnnotatedTypeMirror lhs) {
            return (lhs.hasEffectiveAnnotation(BOTTOM_QUAL)
                    || rhs.hasEffectiveAnnotation(BOTTOM_QUAL)
                    || super.isSubtypeAsTypeArgument(rhs, lhs));
        }


        /**
         * Uses the JLS specification (as implemented in {@link TypeHierarchy},
         * if the variable type, lhs, is mutable; otherwise, allows the type
         * arguments to change while maintaining subtype relationship.
         *
         * This allows for subtyping relationships of the kind:
         * <pre>  @Mutable List&lt;@Mutable Date&gt; &lt;: @ReadOnly List&lt;@ReadOnly Date&gt;<\pre>
         */
        @Override
        protected boolean isSubtypeTypeArguments(AnnotatedDeclaredType rhs, AnnotatedDeclaredType lhs) {
            if (lhs.hasEffectiveAnnotation(MUTABLE))
                return super.isSubtypeTypeArguments(rhs, lhs);

            if (!lhs.getTypeArguments().isEmpty()
                    && !rhs.getTypeArguments().isEmpty()) {
                assert lhs.getTypeArguments().size() == rhs.getTypeArguments().size();
                for (int i = 0; i < lhs.getTypeArguments().size(); ++i) {
                    if (!isSubtype(rhs.getTypeArguments().get(i), lhs.getTypeArguments().get(i)))
                        return false;
                }
            }
            return true;
        }
    }
}
