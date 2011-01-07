package checkers.types;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

import checkers.types.AnnotatedTypeMirror.*;
import checkers.util.AnnotationUtils;

/**
 * Class to test {@link AnnotatedTypeMirror} subtype relationships.
 * <p>
 *
 * This implementation uses the regular Java subtyping rules.
 * More specifically, let
 *   &lt;: be the subtyping relationship,
 *   {@code qual(A) = qualifier on A}, and
 *   {@code |A| = type without qualifier}:
 *
 * <ol>
 *
 * <li>A &lt;: B  iff |A| &lt;: |B| && qual(A) &lt;: qual(B)
 *
 * <li>A[] &lt;: B[] iff A &lt;: B and qual(A[]) &lt;: qual(B[])
 *
 * <li>A&lt;A1, ..., An&gt;  &lt;: B&lt;B1, ..., Bn&gt;
 *      if A &lt;: B && A1 = B1 && ... && An = Bn
 *
 * </ol>
 *
 * Subclasses may override this behavior.
 *
 * <p>
 *
 * Note that the array subtyping rules depends on a runtime check to
 * guarantee the soundness of the type system.  It is unsafe for static check
 * purposes.
 */
public class TypeHierarchy {
    /** The hierarchy of qualifiers */
    private final QualifierHierarchy qualifierHierarchy;
    /** Prevent infinite loops in cases of recursive type bound */
    protected final Set<Element> visited;

    /**
     * Constructs an instance of {@code TypeHierarchy} for the type system
     * whose qualifiers represented in qualifierHierarchy.
     * @param qualifierHierarchy
     */
    public TypeHierarchy(QualifierHierarchy qualifierHierarchy) {
        this.qualifierHierarchy = qualifierHierarchy;
        this.visited = new HashSet<Element>();
    }

    /**
     * Entry point for subtype checking:
     * Checks whether rhs is a subtype of lhs.
     *
     * @return  a true iff rhs a subtype of lhs
     */
    public final boolean isSubtype(AnnotatedTypeMirror rhs, AnnotatedTypeMirror lhs) {
        boolean result = isSubtypeImpl(rhs, lhs);
        this.visited.clear();
        return result;
    }

    /**
     * Checks if the rhs is a subtype of the lhs.
     *
     * Private method to be called internally only.
     * It populates the visited field.
     */
    protected final boolean isSubtypeImpl(AnnotatedTypeMirror rhs, AnnotatedTypeMirror lhs) {
        // System.out.printf("isSubtypeImpl(%s (%s, %s), %s (%s, %s))%n", rhs, rhs.getKind(), rhs.getClass(), lhs, lhs.getKind(), lhs.getClass());
        // If already checked this type (in case of recusive type bound)
        // return true.  If not subtype, we wouldn't have gotten here again.
        if (visited.contains(lhs.getElement()))
            return true;
        AnnotatedTypeMirror lhsBase = lhs;
        while (lhsBase.getKind() != rhs.getKind()
                && (lhsBase.getKind() == TypeKind.WILDCARD || lhsBase.getKind() == TypeKind.TYPEVAR)) {
            if (lhsBase.getKind() == TypeKind.WILDCARD && rhs.getKind() != TypeKind.WILDCARD) {
                AnnotatedWildcardType wildcard = (AnnotatedWildcardType)lhsBase;
                if (wildcard.getSuperBound() != null
                    && isSubtypeImpl(rhs, wildcard.getSuperBound()))
                    return true;
                lhsBase = ((AnnotatedWildcardType)lhsBase).getExtendsBound();
                if (lhsBase == null || !lhsBase.isAnnotated())
                    return true;
                visited.add(lhsBase.getElement());
            } else if (rhs.getKind() == TypeKind.WILDCARD) {
                rhs = ((AnnotatedWildcardType)rhs).getExtendsBound();
            } else if (lhsBase.getKind() == TypeKind.TYPEVAR && rhs.getKind() != TypeKind.TYPEVAR) {
                AnnotatedTypeVariable lhsb_atv = (AnnotatedTypeVariable)lhsBase;
                Set<AnnotationMirror> lAnnos = lhsb_atv.getLowerBoundAnnotations();
                if (!lAnnos.isEmpty())
                    return qualifierHierarchy.isSubtype(rhs.getAnnotations(), lAnnos);
                return qualifierHierarchy.getBottomQualifier() == null ?
                    rhs.getAnnotations().isEmpty() :
                    rhs.getAnnotations().contains(qualifierHierarchy.getBottomQualifier());
            }
        }

        if (lhsBase.getKind() == TypeKind.WILDCARD && rhs.getKind() == TypeKind.WILDCARD) {
            return isSubtype(((AnnotatedWildcardType)rhs).getExtendsBound(),
                    ((AnnotatedWildcardType)lhsBase).getExtendsBound());
        }

        AnnotatedTypeMirror rhsBase = rhs.typeFactory.atypes.asSuper(rhs, lhsBase);
        // FIXME: the following line should be removed, but erasure code is buggy
        // related to bug tests/framework/OverrideCrash
        if (rhsBase == null) rhsBase = rhs;

        // System.out.printf("lhsBase=%s (%s), rhsBase=%s (%s)%n", lhsBase, lhsBase.getClass(), rhsBase, rhsBase.getClass());

        // Is this test correct in the case of type variables?
        if (!qualifierHierarchy.isSubtype(rhsBase.getAnnotations(), lhsBase.getAnnotations()))
            return false;

        if (lhs.getKind() == TypeKind.ARRAY && rhsBase.getKind() == TypeKind.ARRAY) {
            AnnotatedTypeMirror rhsComponent = ((AnnotatedArrayType)rhsBase).getComponentType();
            AnnotatedTypeMirror lhsComponent = ((AnnotatedArrayType)lhsBase).getComponentType();
            return isSubtypeAsArrayComponent(rhsComponent, lhsComponent);
        } else if (lhsBase.getKind() == TypeKind.DECLARED && rhsBase.getKind() == TypeKind.DECLARED) {
            return isSubtypeTypeArguments((AnnotatedDeclaredType)rhsBase, (AnnotatedDeclaredType)lhsBase);
        } else if (lhsBase.getKind() == TypeKind.TYPEVAR && rhsBase.getKind() == TypeKind.TYPEVAR) {
            // System.out.printf("lhsBase (%s underlying=%s), rhsBase (%s underlying=%s), equals=%s%n", lhsBase.hashCode(), lhsBase.getUnderlyingType(), rhsBase.hashCode(), rhsBase.getUnderlyingType(), lhsBase.equals(rhsBase));
            // Should this logic also appear elsewhere?
            AnnotatedTypeMirror rhsSuperClass = rhsBase;
            while (rhsSuperClass.getKind() == TypeKind.TYPEVAR) {
                if (lhsBase.equals(rhsSuperClass))
                    return true;
                if (lhsBase.toString().equals(rhsSuperClass.toString())) // hack
                    return true;
                rhsSuperClass = ((AnnotatedTypeVariable) rhsSuperClass).getUpperBound();
            }
            // compare lower bound of lhs to upper bound of rhs
            Set<AnnotationMirror> las = ((AnnotatedTypeVariable) lhsBase).getLowerBoundAnnotations();
            Set<AnnotationMirror> ras = ((AnnotatedTypeVariable) rhsBase).getUpperBoundAnnotations();
            if (!las.isEmpty()) {
                return qualifierHierarchy.isSubtype(ras, las);
            }
            // No annotations on lhs lower bound
            return qualifierHierarchy.getBottomQualifier() == null ?
                rhs.getAnnotations().isEmpty() :
                rhs.getAnnotations().contains(qualifierHierarchy.getBottomQualifier());
        }

        // It's probably OK to reach this case, because of the isSubtype test above.
        // Still, the use of this default fallthrough is troubling.
        // System.out.printf("isSubtypeImpl(%s, %s):%n  return true via default fallthrough case%n", rhs, lhs);

        return true;
    }

    /**
     * Checks that rhs and lhs are subtypes with respect to type arguments only.
     * Returns true if any of the provided types is not a parameterized type.
     *
     * A parameterized type, rhs, is a subtype of another, lhs, only if their
     * actual type parameters are invariant.
     *
     * <p>
     *
     * As an implementation detail, this method uses
     * {@link #isSubtypeAsTypeArgument(AnnotatedTypeMirror, AnnotatedTypeMirror)}
     * to compare each type argument individually.  Subclasses may override
     * either methods to allow type argument to change covariantly.
     *
     * @return  true iff the type arguments of lhs and rhs are invariant.
     */
    protected boolean isSubtypeTypeArguments(AnnotatedDeclaredType rhs, AnnotatedDeclaredType lhs) {
        List<AnnotatedTypeMirror> rhsTypeArgs = rhs.getTypeArguments();
        List<AnnotatedTypeMirror> lhsTypeArgs = lhs.getTypeArguments();

        if (rhsTypeArgs.isEmpty() || lhsTypeArgs.isEmpty())
            return true;

        assert lhsTypeArgs.size() == rhsTypeArgs.size();
        for (int i = 0; i < lhsTypeArgs.size(); ++i) {
            if (!isSubtypeAsTypeArgument(rhsTypeArgs.get(i), lhsTypeArgs.get(i)))
                return false;
        }

        return true;
    }

    /**
     * Checks that rhs is a subtype of lhs, as actual type arguments.  In JLS
     * terms, rhs needs to be invariant of lhs.
     *
     * @return  true if the types have the same annotations
     */
    protected boolean isSubtypeAsTypeArgument(AnnotatedTypeMirror rhs, AnnotatedTypeMirror lhs) {
        if (lhs.getKind() == TypeKind.WILDCARD && rhs.getKind() != TypeKind.WILDCARD) {
            if (visited.contains(lhs.getElement()))
                return true;

            visited.add(lhs.getElement());
            lhs = ((AnnotatedWildcardType)lhs).getExtendsBound();
            if (lhs == null) return true;
            return isSubtypeImpl(rhs, lhs);
        }

        if (lhs.getKind() == TypeKind.WILDCARD && rhs.getKind() == TypeKind.WILDCARD) {
            return isSubtype(((AnnotatedWildcardType)rhs).getExtendsBound(),
                    ((AnnotatedWildcardType)lhs).getExtendsBound());
        }

        if (lhs.getKind() == TypeKind.TYPEVAR && rhs.getKind() != TypeKind.TYPEVAR) {
            if (visited.contains(lhs.getElement())) return true;
            visited.add(lhs.getElement());
            return isSubtype(rhs, ((AnnotatedTypeVariable)lhs).getUpperBound());
        }

        Set<AnnotationMirror> las = lhs.getAnnotations();
        Set<AnnotationMirror> ras = rhs.getAnnotations();

        if (!AnnotationUtils.areSame(las, ras))
            return false;

        if (lhs.getKind() == TypeKind.DECLARED && rhs.getKind() == TypeKind.DECLARED)
            return isSubtypeTypeArguments((AnnotatedDeclaredType)rhs, (AnnotatedDeclaredType)lhs);
        else if (lhs.getKind() == TypeKind.ARRAY && rhs.getKind() == TypeKind.ARRAY) {
            // arrays components within type arguments are invariants too
            // List<String[]> is not a subtype of List<Object[]>
            AnnotatedTypeMirror rhsComponent = ((AnnotatedArrayType)rhs).getComponentType();
            AnnotatedTypeMirror lhsComponent = ((AnnotatedArrayType)lhs).getComponentType();
            return isSubtypeAsTypeArgument(rhsComponent, lhsComponent);
        }
        return true;
    }

    /**
     * Checks that rhs is a subtype of lhs, as an array component type.
     * (Unfortunately) Java specifies array components are co-variant while
     * maintaining subtype relations.
     *
     * <p>
     *
     * This property of arrays makes the code unsafe at run-time.  Subclasses
     * may override this method to enforce a stricter relationship.
     *
     *
     * @return true iff rhs is a subtype of lhs
     */
    protected boolean isSubtypeAsArrayComponent(AnnotatedTypeMirror rhs, AnnotatedTypeMirror lhs) {
        return isSubtypeImpl(rhs, lhs);
    }
}
