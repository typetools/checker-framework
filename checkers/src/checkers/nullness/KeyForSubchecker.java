package checkers.nullness;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

import checkers.basetype.BaseTypeChecker;
import checkers.nullness.quals.Covariant;
import checkers.nullness.quals.KeyFor;
import checkers.nullness.quals.KeyForBottom;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.QualifierHierarchy;
import checkers.types.TypeHierarchy;
import checkers.util.AnnotationUtils;
import checkers.util.GraphQualifierHierarchy;

/**
 * TODO: doc
 */
@TypeQualifiers({ KeyFor.class, Unqualified.class, KeyForBottom.class})
public class KeyForSubchecker extends BaseTypeChecker {
    protected AnnotationMirror KEYFOR;

    @Override
    public void initChecker(ProcessingEnvironment env) {
        super.initChecker(env);

        AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);
        KEYFOR = annoFactory.fromClass(KeyFor.class);
    }

    @Override
    protected TypeHierarchy createTypeHierarchy() {
        return new KeyForTypeHierarchy(this, getQualifierHierarchy());
    }

    @Override
    public final boolean isSubtype(AnnotatedTypeMirror rhs, AnnotatedTypeMirror lhs) {
        if (lhs.getKind() == TypeKind.TYPEVAR &&
                rhs.getKind() == TypeKind.TYPEVAR) {
            // TODO: Investigate whether there is a nicer and more proper way to
            // get assignments between two type variables working.
            if (lhs.getAnnotations().isEmpty()) {
                return true;
            }
        }
        // Otherwise Covariant would cause trouble.
        if (rhs.getAnnotation(KeyForBottom.class) != null) {
            return true;
        }
        return super.isSubtype(rhs, lhs);
    }

    private class KeyForTypeHierarchy extends TypeHierarchy {

        public KeyForTypeHierarchy(KeyForSubchecker checker, QualifierHierarchy qualifierHierarchy) {
            super(checker, qualifierHierarchy);
        }

        @Override
        protected boolean isSubtypeTypeArguments(AnnotatedDeclaredType rhs, AnnotatedDeclaredType lhs) {
            List<AnnotatedTypeMirror> rhsTypeArgs = rhs.getTypeArguments();
            List<AnnotatedTypeMirror> lhsTypeArgs = lhs.getTypeArguments();

            if (rhsTypeArgs.isEmpty() || lhsTypeArgs.isEmpty())
                return true;

            TypeElement lhsElem = (TypeElement) lhs.getUnderlyingType().asElement();
            // TypeElement rhsElem = (TypeElement) lhs.getUnderlyingType().asElement();
            // the following would be needed if Covariant were per type parameter
            // AnnotatedDeclaredType lhsDecl = currentATF.fromElement(lhsElem);
            // AnnotatedDeclaredType rhsDecl = currentATF.fromElement(rhsElem);
            // List<AnnotatedTypeMirror> lhsTVs = lhsDecl.getTypeArguments();
            // List<AnnotatedTypeMirror> rhsTVs = rhsDecl.getTypeArguments();

            int[] covarVals = null;
            if (lhsElem.getAnnotation(Covariant.class) != null) {
                covarVals = lhsElem.getAnnotation(Covariant.class).value();
            }


            if (lhsTypeArgs.size() != rhsTypeArgs.size()) {
                // This test fails e.g. for casts from a type with one type
                // argument to a type with two type arguments.
                // See test case nullness/generics/GenericsCasts
                // TODO: shouldn't the type be brought to a common type before
                // this?
                return true;
            }

            for (int i = 0; i < lhsTypeArgs.size(); ++i) {
                boolean covar = false;
                if (covarVals != null) {
                    for (int cvv = 0; cvv < covarVals.length; ++cvv) {
                        if (covarVals[cvv] == i) {
                            covar = true;
                        }
                    }
                }

                if (covar) {
                    if (!KeyForSubchecker.this.isSubtype(rhsTypeArgs.get(i), lhsTypeArgs.get(i)))
                        return false;
                } else {
                    if (!isSubtypeAsTypeArgument(rhsTypeArgs.get(i), lhsTypeArgs.get(i)))
                        return false;
                }
            }

            return true;
        }
    }

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new KeyForQualifierHierarchy((GraphQualifierHierarchy) super.createQualifierHierarchy());
    }

    private final class KeyForQualifierHierarchy extends GraphQualifierHierarchy {

        public KeyForQualifierHierarchy(GraphQualifierHierarchy hierarchy) {
            super(hierarchy);
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(lhs, KEYFOR) &&
                    AnnotationUtils.areSameIgnoringValues(rhs, KEYFOR)) {
                // If they are both KeyFor annotations, they have to be equal.
                // TODO: or one a subset of the maps of the other? Ordering of maps?
                return AnnotationUtils.areSame(lhs, rhs);
            }
            // Ignore annotation values to ensure that annotation is in supertype map.
            if (AnnotationUtils.areSameIgnoringValues(lhs, KEYFOR)) {
                lhs = KEYFOR;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, KEYFOR)) {
                rhs = KEYFOR;
            }
            return super.isSubtype(rhs, lhs);
        }
    }

}
