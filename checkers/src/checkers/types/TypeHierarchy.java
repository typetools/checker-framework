package checkers.types;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

import checkers.basetype.BaseTypeChecker;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedTypeVariable;
import checkers.types.AnnotatedTypeMirror.AnnotatedWildcardType;
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

    /** The type checker to use. */
    protected final BaseTypeChecker checker;

    /**
     * Constructs an instance of {@code TypeHierarchy} for the type system
     * whose qualifiers represented in qualifierHierarchy.
     * 
     * @param checker The type checker to use
     * @param qualifierHierarchy The qualifier hierarchy to use
     */
    public TypeHierarchy(BaseTypeChecker checker, QualifierHierarchy qualifierHierarchy) {
        this.qualifierHierarchy = qualifierHierarchy;
        this.visited = new HashSet<Element>();
        this.checker = checker;
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
        /*
        System.out.printf("isSubtypeImpl(rhs: %s (%s, %s), lhs: %s (%s, %s))%n",
                rhs, rhs.getKind(), rhs.getClass(),
                lhs, lhs.getKind(), lhs.getClass());
        */
        // If we already checked this type (in case of a recursive type bound)
        // return true.  If it's not a subtype, we wouldn't have gotten here again.
        if (visited.contains(lhs.getElement()))
            return true;
        AnnotatedTypeMirror lhsBase = lhs;
        while (lhsBase.getKind() != rhs.getKind()
                && (lhsBase.getKind() == TypeKind.WILDCARD || lhsBase.getKind() == TypeKind.TYPEVAR)) {
            if (lhsBase.getKind() == TypeKind.WILDCARD && rhs.getKind() != TypeKind.WILDCARD) {
                AnnotatedWildcardType wildcard = (AnnotatedWildcardType)lhsBase;
                if (wildcard.getSuperBound() != null
                        && isSubtypeImpl(rhs, wildcard.getEffectiveSuperBound())) {
                    return true;
                }
                if (wildcard.isAnnotated()
                        && qualifierHierarchy.isSubtype(rhs.getEffectiveAnnotations(), wildcard.getAnnotations())) {
                    return true;
                } else {
                    Set<AnnotationMirror> bnd = wildcard.getEffectiveAnnotations();
                    Set<AnnotationMirror> bot = Collections.singleton(qualifierHierarchy.getBottomAnnotation(bnd.iterator().next()));
                    if (!wildcard.isMethodTypeArgHack() &&
                            (!qualifierHierarchy.isSubtype(bnd, bot) ||
                            !qualifierHierarchy.isSubtype(rhs.getEffectiveAnnotations(), bot))) {
                        return false;
                    }
                }
                lhsBase = ((AnnotatedWildcardType)lhsBase).getExtendsBound();
                visited.add(lhsBase.getElement());
            } else if (rhs.getKind() == TypeKind.WILDCARD) {
                rhs = ((AnnotatedWildcardType)rhs).getExtendsBound();
            } else if (lhsBase.getKind() == TypeKind.TYPEVAR && rhs.getKind() != TypeKind.TYPEVAR) {
                AnnotatedTypeVariable lhsb_atv = (AnnotatedTypeVariable)lhsBase;
                Set<AnnotationMirror> lAnnos = lhsb_atv.getEffectiveLowerBoundAnnotations();
                return qualifierHierarchy.isSubtype(rhs.getAnnotations(), lAnnos);
            }
        }

        if (lhsBase.getKind() == TypeKind.WILDCARD && rhs.getKind() == TypeKind.WILDCARD) {
            return isSubtype(((AnnotatedWildcardType)rhs).getEffectiveExtendsBound(),
                    ((AnnotatedWildcardType)lhsBase).getEffectiveExtendsBound());
        }

        AnnotatedTypeMirror rhsBase = rhs.typeFactory.atypes.asSuper(rhs, lhsBase);

        // FIXME: the following line should be removed, but erasure code is buggy
        // related to bug tests/framework/OverrideCrash
        if (rhsBase == null) rhsBase = rhs;

        // System.out.printf("lhsBase=%s (%s), rhsBase=%s (%s)%n",
        //        lhsBase, lhsBase.getClass(), rhsBase, rhsBase.getClass());

        {
            Set<AnnotationMirror> lhsAnnos = lhsBase.getEffectiveAnnotations();
            Set<AnnotationMirror> rhsAnnos = rhsBase.getEffectiveAnnotations();

            if (lhsAnnos.isEmpty() || rhsAnnos.isEmpty() && lhsBase.getKind()==TypeKind.TYPEVAR) {
                // TODO: allow type variables without annotations for now. Better solution?
                // System.out.println("TypeHierarchy: empty annotations in lhs: " +
                //        lhs + " " + lhsAnnos + " or rhs: " + rhs + " " + rhsAnnos);
                return true;
            }

            if (!qualifierHierarchy.isSubtype(rhsAnnos, lhsAnnos)) {
                return false;
            }
        }

        if (lhs.getKind() == TypeKind.ARRAY && rhsBase.getKind() == TypeKind.ARRAY) {
            AnnotatedTypeMirror rhsComponent = ((AnnotatedArrayType)rhsBase).getComponentType();
            AnnotatedTypeMirror lhsComponent = ((AnnotatedArrayType)lhsBase).getComponentType();
            return isSubtypeAsArrayComponent(rhsComponent, lhsComponent);
        } else if (lhsBase.getKind() == TypeKind.DECLARED && rhsBase.getKind() == TypeKind.DECLARED) {
            return isSubtypeTypeArguments((AnnotatedDeclaredType)rhsBase, (AnnotatedDeclaredType)lhsBase);
        } else if (lhsBase.getKind() == TypeKind.TYPEVAR && rhsBase.getKind() == TypeKind.TYPEVAR) {
            // System.out.printf("lhsBase (%s underlying=%s), rhsBase (%s underlying=%s), equals=%s%n", lhsBase.hashCode(), lhsBase.getUnderlyingType(), rhsBase.hashCode(), rhsBase.getUnderlyingType(), lhsBase.equals(rhsBase));

            if (areCorrespondingTypeVariables(lhsBase, rhsBase)) {
                // We have corresponding type variables 
                if(!lhsBase.getAnnotations().isEmpty() && !rhsBase.getAnnotations().isEmpty() &&
                    qualifierHierarchy.isSubtype(rhsBase.getAnnotations(), lhsBase.getAnnotations())) {
                    // Both sides have annotations and the rhs is a subtype of the lhs -> good
                    return true;
                }
                if(!rhsBase.getAnnotations().isEmpty() &&
                        lhsBase.getAnnotations().isEmpty()) {
                    for (AnnotationMirror bot : qualifierHierarchy.getBottomAnnotations()) {
                        for(AnnotationMirror rhsAnno : rhsBase.getAnnotations()) {
                            if (!AnnotationUtils.areSame(bot, rhsAnno)) {
                                return false;
                            }
                        }
                    }
                    // Only the rhs is annotated and it's only bottom annotations -> good
                    return true;
                }
                if(!lhsBase.getAnnotations().isEmpty() &&
                        rhsBase.getAnnotations().isEmpty()) {
                    if (qualifierHierarchy.isSubtype(((AnnotatedTypeVariable)rhsBase).getEffectiveUpperBound().getAnnotations(),
                            lhsBase.getAnnotations())) {
                        // The annotations on the upper bound of the RHS are below the annotation on the LHS -> good
                        return true;
                    } else {
                        // LHS has annotation that is not a top annotation -> bad
                        return false;
                    }
                }
                if (lhsBase.getAnnotations().isEmpty() && rhsBase.getAnnotations().isEmpty()) {
                    // Neither type variable has an annotation and they correspond -> good
                    return true;
                } else {
                    // Go away.
                    return false;
                }
            }

            AnnotatedTypeMirror rhsSuperClass = rhsBase;
            while (rhsSuperClass.getKind() == TypeKind.TYPEVAR) {
                if (lhsBase.equals(rhsSuperClass))
                    return true;
                rhsSuperClass = ((AnnotatedTypeVariable) rhsSuperClass).getUpperBound();
            }
            // compare lower bound of lhs to upper bound of rhs
            Set<AnnotationMirror> las = ((AnnotatedTypeVariable) lhsBase).getEffectiveLowerBoundAnnotations();
            Set<AnnotationMirror> ras = ((AnnotatedTypeVariable) rhsBase).getEffectiveUpperBoundAnnotations();
            return qualifierHierarchy.isSubtype(ras, las);
        } else if ((lhsBase.getKind().isPrimitive() || lhsBase.getKind() == TypeKind.DECLARED)
                && rhsBase.getKind().isPrimitive()) {
            // There are only the main qualifiers and they were compared above.
            // Allow declared on LHS for boxing.
            return true;
        } else if (lhsBase.getKind() == TypeKind.NULL || rhsBase.getKind() == TypeKind.NULL) {
            // There are only the main qualifiers and they were compared above.
            return true;
        } else if (lhsBase.getKind() == TypeKind.ARRAY) {
            // The qualifiers on the LHS and RHS have been compared already. Done?
            // Test case IdentityArrayList triggered this.
            // TODO: Maybe something is wrong with method type argument inference?
            return true;
        }

        /* This point is not reached in the test suite.
        System.out.printf("isSubtypeImpl(rhs: %s [%s]; lhs: %s [%s]):%n" +
                "  rhsBase: %s; lhsBase: %s%n" +
                "  return false via default fallthrough case%n",
                rhs, rhs.getKind(), lhs, lhs.getKind(),
                rhsBase, lhsBase);
         */
        return false;
    }

    private boolean areCorrespondingTypeVariables(AnnotatedTypeMirror lhs,
            AnnotatedTypeMirror rhs) {
        com.sun.tools.javac.code.Symbol.TypeSymbol lhstsym = ((com.sun.tools.javac.code.Type)lhs.actualType).tsym;
        com.sun.tools.javac.code.Symbol.TypeSymbol rhstsym = ((com.sun.tools.javac.code.Type)rhs.actualType).tsym;

        List<com.sun.tools.javac.code.Symbol.TypeSymbol> lhsTPs =
                lhstsym.getEnclosingElement().getTypeParameters();
        List<com.sun.tools.javac.code.Symbol.TypeSymbol> rhsTPs =
                rhstsym.getEnclosingElement().getTypeParameters();

        if (lhsTPs.size() != rhsTPs.size()) {
            // different lenghts -> definitely not the same
            return false;
        }

        int i;
        for (i = 0; i < lhsTPs.size(); ++i) {
            com.sun.tools.javac.code.Symbol.TypeSymbol lTS = lhsTPs.get(i);
            if(lTS.equals(lhstsym)) {
                break;
            }
        }
        if (i < lhsTPs.size()) {
            return rhsTPs.get(i).equals(rhstsym);
        }
        return false;
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

        if (lhsTypeArgs.size() != rhsTypeArgs.size()) {
            // System.out.println("Mismatch between " + lhsTypeArgs + " and " + rhsTypeArgs
            //         + " in types " + lhs + " and " + rhs);
            // This happened in javari/RandomTests, where we have:
            //         List<String> l = (List<String>) new HashMap<String, String>();
            // Shouldn't rhs and lhs be first brought to the same base type, before comparing the
            // type arguments?
            // When compiling that line with javac, one gets an unchecked
            // warning.
            // TODO
            return true;
        }

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
            if(!lhs.getAnnotations().isEmpty()) {
                if (!lhs.getAnnotations().equals(rhs.getEffectiveAnnotations())) {
                    return false;
                }
            }
            lhs = ((AnnotatedWildcardType)lhs).getEffectiveExtendsBound();
            if (lhs == null) return true;
            return isSubtypeImpl(rhs, lhs);
        }

        if (lhs.getKind() == TypeKind.WILDCARD && rhs.getKind() == TypeKind.WILDCARD) {
            AnnotatedTypeMirror rhsbnd = ((AnnotatedWildcardType)rhs).getEffectiveExtendsBound();
            AnnotatedTypeMirror lhsbnd = ((AnnotatedWildcardType)lhs).getEffectiveExtendsBound();
            return isSubtype(rhsbnd, lhsbnd);
        }

        if (lhs.getKind() == TypeKind.TYPEVAR && rhs.getKind() != TypeKind.TYPEVAR) {
            if (visited.contains(lhs.getElement())) return true;
            visited.add(lhs.getElement());
            // TODO: the following two lines were added to make tests/nullness/MethodTypeVars2 pass.
            // Is this correct or just a quick fix?
            if (visited.contains(((AnnotatedTypeVariable)lhs).getUpperBound().getElement())) return true;
            visited.add(((AnnotatedTypeVariable)lhs).getUpperBound().getElement());
            return isSubtype(rhs, ((AnnotatedTypeVariable)lhs).getUpperBound());
        }

        // Do not ask for the effective annotations here, as that would confuse
        // annotations on the type variable with upper bound annotations.
        // See nullness testcase GenericsBounds1.
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
        // TODO: I think this can only happen from the method type variable introduction
        // of wildcards. If we change that (in AnnotatedTypes.inferTypeArguments)
        if (lhs.getKind() == TypeKind.WILDCARD && rhs.getKind() != TypeKind.WILDCARD) {
            if (visited.contains(lhs.getElement()))
                return true;

            visited.add(lhs.getElement());
            if(!lhs.getAnnotations().isEmpty()) {
                if (!lhs.getAnnotations().equals(rhs.getEffectiveAnnotations())) {
                    return false;
                }
            }
            lhs = ((AnnotatedWildcardType)lhs).getEffectiveExtendsBound();
            if (lhs == null) return true;
            return checker.isSubtype(rhs, lhs);
        }
        // End of copied code.

        // The main array component annotations must be equal.
        if (checker.getLintOption("arrays:invariant", false) &&
                !AnnotationUtils.areSame(lhs.getAnnotations(), rhs.getAnnotations())) {
            return false;
        }

        // In addition, check that the full types are subtypes, to ensure that the
        // remaining qualifiers are correct.
        // TODO: go back to the type checker and invoke isSubtype from there.
        // Should we do this more consistently, e.g. also for type arguments?
        // The problem is that I was overriding isSubtype in the checker, but then
        // the behavior for arrays didn't adapt.
        return checker.isSubtype(rhs, lhs);
    }
}
