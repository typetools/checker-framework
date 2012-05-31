package checkers.oigj;

import java.util.Collections;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import checkers.basetype.BaseTypeChecker;
import checkers.oigj.quals.*;
import checkers.quals.TypeQualifiers;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.AnnotationUtils;
import checkers.util.InternalUtils;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

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

    //
    // OIGJ Rule 2. Field assignment
    // Field assignment o.f = ... is legal  iff
    //   (i) I(o) <= AssignsFields or f is annotated as @Assignable, and
    //   (ii) o = this or the type of f does not contain the owner Dominator
    //        or Modifier
    //
    @Override
    public boolean isAssignable(AnnotatedTypeMirror varType,
                                AnnotatedTypeMirror receiverType, Tree varTree,
                                AnnotatedTypeFactory factory) {
        if (!(varTree instanceof ExpressionTree))
            return true;

        Element varElement = InternalUtils.symbol(varTree);
        if (varElement != null && factory.getDeclAnnotation(varElement, Assignable.class) != null)
            return true;

        if (receiverType==null) {
            // Happens e.g. for local variable, which doesn't have a receiver.
            return true;
        }

        if (getQualifierHierarchy().isSubtype(
                receiverType.getAnnotations(),
                Collections.singleton(ASSIGNS_FIELDS)))
            return true;

        return false;
    }

}
