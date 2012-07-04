package checkers.igj;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import checkers.basetype.BaseTypeChecker;
import checkers.igj.quals.*;
import checkers.quals.TypeQualifiers;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.*;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

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
    public void initChecker(ProcessingEnvironment env) {
        AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);
        READONLY = annoFactory.fromClass(ReadOnly.class);
        MUTABLE = annoFactory.fromClass(Mutable.class);
        IMMUTABLE = annoFactory.fromClass(Immutable.class);
        I = annoFactory.fromClass(I.class);
        ASSIGNS_FIELDS = annoFactory.fromClass(AssignsFields.class);
        BOTTOM_QUAL = annoFactory.fromClass(IGJBottom.class);
        super.initChecker(env);
    }

    // **********************************************************************
    // IGJ specific Type Relationship
    // **********************************************************************

    /**
     * Return true if the assignment variable is an assignable field or
     * variable, and returns false otherwise.
     *
     * A field is assignable if it is
     *
     * 1. a static field
     * 2. marked {@link Assignable}
     * 3. accessed through a mutable reference
     * 4. reassigned with an {@link AssignsFields} method and owned by 'this'
     *
     */
    @Override
    public boolean isAssignable(AnnotatedTypeMirror varType,
            AnnotatedTypeMirror receiverType, Tree varTree,
            AnnotatedTypeFactory factory) {
        if (!(varTree instanceof ExpressionTree))
            return true;

        Element varElement = InternalUtils.symbol(varTree);
        if (varTree.getKind() != Tree.Kind.ARRAY_ACCESS
                && (varElement == null // a variable element should never be null
                        || !varElement.getKind().isField()
                        || ElementUtils.isStatic(varElement)
                        || factory.getDeclAnnotation(varElement, Assignable.class) != null))
            return true;

        assert receiverType != null;

        final boolean isAssignable =
            receiverType.hasEffectiveAnnotation(MUTABLE)
             || receiverType.hasEffectiveAnnotation(BOTTOM_QUAL)
             || (receiverType.hasEffectiveAnnotation(ASSIGNS_FIELDS)
                     && factory.isMostEnclosingThisDeref((ExpressionTree)varTree));

        return isAssignable;
    }

    // **********************************************************************
    // Factory methods
    // **********************************************************************

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new IGJQualifierHierarchy((GraphQualifierHierarchy)super.createQualifierHierarchy());
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
        public IGJQualifierHierarchy(GraphQualifierHierarchy hierarchy) {
            super(hierarchy);
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
