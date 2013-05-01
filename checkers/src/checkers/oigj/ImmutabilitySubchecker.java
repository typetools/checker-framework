package checkers.oigj;

import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import checkers.basetype.BaseTypeChecker;
import checkers.oigj.quals.*;
import checkers.quals.TypeQualifiers;
import checkers.source.SourceChecker;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.AnnotationUtils;
import checkers.util.GraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

@TypeQualifiers({ ReadOnly.class, Mutable.class, Immutable.class, I.class,
    AssignsFields.class, OIGJMutabilityBottom.class })
public class ImmutabilitySubchecker extends BaseTypeChecker {

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
        BOTTOM_QUAL = AnnotationUtils.fromClass(elements, OIGJMutabilityBottom.class);
        super.initChecker();
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new ImmutabilityQualifierHierarchy(factory);
    }

    //
    // OIGJ Rule 6. Same-class subtype definition
    // Let C<I, X_1, ..., X_n> be a class.  Type S = C<J, S_1, ..., S_n>
    // is a subtype of T = C<J', T_1, ... T_n> witten as S<= T, iff J <= J' and
    // for i = 1, ..., n, either S_i = T_i or
    // (Immutable <= J' and S_i <= T_i and CoVariant(X_i, C))
    //
    @Override
    protected TypeHierarchy createTypeHierarchy() {
        return new OIGJImmutabilityTypeHierarchy(this, getQualifierHierarchy());
    }

    private final class OIGJImmutabilityTypeHierarchy extends TypeHierarchy {

        public OIGJImmutabilityTypeHierarchy(ImmutabilitySubchecker checker,
                QualifierHierarchy qualifierHierarchy) {
            super(checker, qualifierHierarchy);
        }

        /**
         * OIGJ Rule 6. <b>Same-class subtype definition</b>
         */
        // TODO: Handle CoVariant(X_i, C)
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

    private final class ImmutabilityQualifierHierarchy extends GraphQualifierHierarchy {
        public ImmutabilityQualifierHierarchy(MultiGraphFactory factory) {
            super(factory, BOTTOM_QUAL);
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

}
