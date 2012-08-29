package checkers.oigj;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.basetype.BaseTypeChecker;
import checkers.oigj.quals.*;
import checkers.quals.TypeQualifiers;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.AnnotationUtils;

@TypeQualifiers({ ReadOnly.class, Mutable.class, Immutable.class, I.class,
    AssignsFields.class, OIGJMutabilityBottom.class })
public class ImmutabilitySubchecker extends BaseTypeChecker {

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
        BOTTOM_QUAL = annoFactory.fromClass(OIGJMutabilityBottom.class);
        super.initChecker(env);
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
        return new OIGJImmutabilityQualifierHierarchy(this, getQualifierHierarchy());
    }

    private final class OIGJImmutabilityQualifierHierarchy extends TypeHierarchy {

        public OIGJImmutabilityQualifierHierarchy(ImmutabilitySubchecker checker,
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

}
