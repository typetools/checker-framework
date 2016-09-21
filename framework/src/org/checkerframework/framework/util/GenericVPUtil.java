package org.checkerframework.framework.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.AnnotatedTypeReplacer;
import org.checkerframework.javacutil.Pair;

/**
 * Generic version of VPUtil. It has two major clients: one is on framework side, the other is on inference side.
 * See <Link>FrameworkVPUtil</Link> and <Link>InferenceVPUtil</Link> for more information.
 * @author tamier
 *
 * @param <T> AnnotationMirror(framework side) or Slot(inference side)
 */
public abstract class GenericVPUtil<T> {

    // This prevents calling combineTypeWithType on type variable if it is a bound of another type variable.
    // We only process one level. TODO: why we need this mechanism?
    protected boolean isTypeVarExtends = false;

    /**
     * Viewpoint adapt declModifier to recvModifier. Modifier here is not equal to AnnotationMirror.
     * Rather, it can be AnnotationMirror or Slot.
     * @param recvModifier receiver modifier
     * @param declModifier declared modifier that is being adapted
     * @param f AnnotatedTypeFactory of concrete type system
     * @return result modifier after viewpoint adaptation
     */
    // side effect free! Need to use return value of this method to change annotation of others
    protected abstract T combineModifierWithModifier(
            T recvModifier, T declModifier, AnnotatedTypeFactory f);

    /**
     * Extract modifier from AnnotatedTypeMirror. On framework side, we extract AnnotationMiror;
     * On inference side, we extract slot.
     * @param atm AnnotatedTypeMirror from which modifier is going to be extracted
     * @param f AnnotatedTypeFactory of concrete type system
     * @return modifier extracted
     */
    protected abstract T getModifier(AnnotatedTypeMirror atm, AnnotatedTypeFactory f);

    /**
     * Get and return the AnnotationMirror of a modifier
     * @param t Source modifier from which AnnotationMirror is being extracted
     * @return AnnotationMirror extracted
     */
    protected abstract AnnotationMirror getAnnotationFromModifier(T t);

    /**
     * Viewpoint Adapt decl to recv, and return the result atm
     * @param recv receiver in the viewpoint adaptation
     * @param decl declared type in viewpoint adaptation, which needs to be adapted
     * @param f AnnotatedTypeFactory of concrete type system
     * @return AnnotatedTypeMirror after viewpoint adaptation
     */
    public AnnotatedTypeMirror combineTypeWithType(
            AnnotatedTypeMirror recv, AnnotatedTypeMirror decl, AnnotatedTypeFactory f) {
        AnnotatedTypeMirror result = null;
        if (recv.getKind() == TypeKind.TYPEVAR) {
            recv = ((AnnotatedTypeVariable) recv).getUpperBound();
        }
        T recvModifier = getModifier(recv, f);
        if (recvModifier != null) {
            result = combineModifierWithType(recvModifier, decl, f);
            result = substituteTVars(f, recv, result);
        }
        return result;
    }

    /**
     * Viewpoint adapt decl to recvModifier. Not side effect free. Modify the passed-in argument,
     * and return the reference.
     * @param recvModifier modifier of receiver in the viewpoint adaptation
     * @param decl declared type in viewpoint adaptation, which needs to be adapted
     * @param f AnnotatedTypeFactory of concrete type system
     * @return AnnotatedTypeMirror after viewpoint adaptation
     */
    protected AnnotatedTypeMirror combineModifierWithType(
            T recvModifier, AnnotatedTypeMirror decl, AnnotatedTypeFactory f) {
        if (decl.getKind().isPrimitive()) {
            T declModifier = getModifier(decl, f);
            decl.replaceAnnotation(getAnnotationFromModifier(declModifier));
            return decl;
        } else if (decl instanceof AnnotatedTypeVariable) {
            if (!isTypeVarExtends) {
                isTypeVarExtends = true;
                AnnotatedTypeVariable atv = (AnnotatedTypeVariable) decl;
                // For type variables, we recursively adapt upper and lower bounds
                combineModifierWithType(recvModifier, atv.getUpperBound(), f);
                combineModifierWithType(recvModifier, atv.getLowerBound(), f);
                isTypeVarExtends = false;
                return atv;
            }
            return decl;
        } else if (decl instanceof AnnotatedDeclaredType) {
            AnnotatedDeclaredType adt = (AnnotatedDeclaredType) decl;
            T declModifier = getModifier(adt, f);
            T resultModifier = combineModifierWithModifier(recvModifier, declModifier, f);
            // Replace the main modifier of declared type with the AnnotationMirror of adapted modifier
            adt.replaceAnnotation(getAnnotationFromModifier(resultModifier));
            for (AnnotatedTypeMirror typeArgument : adt.getTypeArguments()) {
                // Recursively adapt the type arguments of this adt
                combineModifierWithType(recvModifier, typeArgument, f);
            }
            return adt;
        } else if (decl instanceof AnnotatedArrayType) {
            AnnotatedArrayType aat = (AnnotatedArrayType) decl;
            AnnotatedTypeMirror compo = aat.getComponentType();
            // Recursively call itself first on the component type
            combineModifierWithType(recvModifier, compo, f);
            T declModifier = getModifier(aat, f);
            T result = combineModifierWithModifier(recvModifier, declModifier, f);
            // Replace the annotation of the main modifier of this aat
            aat.replaceAnnotation(getAnnotationFromModifier(result));
            return aat;
        } else if (decl instanceof AnnotatedWildcardType) {
            AnnotatedWildcardType awt = (AnnotatedWildcardType) decl;
            AnnotatedTypeMirror annotatedUpperBound = awt.getExtendsBound();
            // Recursively adapt the upper bound of this awt
            combineModifierWithType(recvModifier, annotatedUpperBound, f);
            AnnotatedTypeMirror annotatedLowerBound = awt.getSuperBound();
            // Recursively adapt the lower bound of this awt
            combineModifierWithType(recvModifier, annotatedLowerBound, f);
            return awt;
        } else if (decl instanceof AnnotatedNullType) {
            AnnotatedNullType ant = (AnnotatedNullType) decl;
            T declModifier = getModifier(ant, f);
            T result = combineModifierWithModifier(recvModifier, declModifier, f);
            ant.replaceAnnotation(getAnnotationFromModifier(result));
            return ant;
        } else {
            System.err.println("Error: Unknown result.getKind(): " + decl.getKind());
            assert false;
            return null;
        }
    }

    /**
     * If rhs is type variable use whose type arguments should be inferred from receiver - lhs, this method substitutes
     * that type argument into rhs, and return the reference to rhs. So, this method is not side effect free, i.e., lhs
     * will be modified after the method returns.
     * @param f AnnotatedTypeFactory of concrete type system
     * @param lhs type from which type arguments are extracted to replace formal type parameters of rhs.
     * @param rhs AnnotatedTypeMirror that might be a formal type parameter
     * @return rhs with its type parameter substituted
     */
    private AnnotatedTypeMirror substituteTVars(
            AnnotatedTypeFactory f, AnnotatedTypeMirror lhs, AnnotatedTypeMirror rhs) {
        if (rhs.getKind() == TypeKind.TYPEVAR) {
            AnnotatedTypeVariable atv = (AnnotatedTypeVariable) rhs;
            // Base case where actual type argument is extracted
            if (lhs.getKind() == TypeKind.DECLARED) {
                rhs = getTypeVariableSubstitution(f, (AnnotatedDeclaredType) lhs, atv);
            }
            // else TODO: the receiver might be another type variable... should we do something?
            // TODO: does that really happen?
        } else if (rhs.getKind() == TypeKind.DECLARED) {
            //System.out.println("before: " + rhs);
            AnnotatedDeclaredType adt = (AnnotatedDeclaredType) rhs;
            Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mapping =
                    new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>();
            for (AnnotatedTypeMirror formalTypeParameter : adt.getTypeArguments()) {
                AnnotatedTypeMirror actualTypeArgument =
                        substituteTVars(f, lhs, formalTypeParameter);
                //System.out.println("actual type argument: " + actualTypeArgument);
                mapping.put(formalTypeParameter, actualTypeArgument);
                // The following code does the wrong thing!
                /*T modifier = getModifier(actualTypeArgument, f);
                System.out.println("modifier: " + modifier);
                // Formally replace formal type parameter with actual type argument
                System.out.println("am: " + getAnnotationFromModifier(modifier));
                formalTypeArgument.replaceAnnotation(getAnnotationFromModifier(modifier));*/
            }
            // We must use AnnotatedTypeReplacer to replace the formal type parameters with actual type
            // arguments, but not replace with its main modifier
            rhs = AnnotatedTypeReplacer.replace(adt, mapping);
        } else if (rhs.getKind() == TypeKind.WILDCARD) {
            AnnotatedWildcardType awt = (AnnotatedWildcardType) rhs;
            AnnotatedTypeMirror upperBound = awt.getExtendsBound();
            if (upperBound != null) {
                substituteTVars(f, lhs, upperBound);
            }

            AnnotatedTypeMirror lowerBound = awt.getSuperBound();
            if (lowerBound != null) {
                substituteTVars(f, lhs, lowerBound);
            }
        } else if (rhs.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType aat = (AnnotatedArrayType) rhs;
            AnnotatedTypeMirror compnentType = aat.getComponentType();
            // Type variable of compnentType already gets substituted
            substituteTVars(f, lhs, compnentType);
        } else if (rhs.getKind().isPrimitive() || rhs.getKind() == TypeKind.NULL) {
            // nothing to do for primitive types and the null type
        } else {
            System.out.println(
                    "GUTQualifierUtils::substituteTVars: What should be done with: "
                            + rhs
                            + " of kind: "
                            + rhs.getKind());
            assert false;
        }

        return rhs;
    }

    /**
     * Return actual type argument for formal type parameter "var" from 'type"
     * @param f AnnotatedTypeFactory of concrete type system
     * @param type type from which type arguments are extracted to replace "var"
     * @param var formal type parameter that needs real type arguments
     * @return Real type argument
     */
    private AnnotatedTypeMirror getTypeVariableSubstitution(
            AnnotatedTypeFactory f, AnnotatedDeclaredType type, AnnotatedTypeVariable var) {
        Pair<AnnotatedDeclaredType, Integer> res = findDeclType(type, var);

        if (res == null) {
            return var;
        }

        AnnotatedDeclaredType decltype = res.first;
        int foundindex = res.second;

        if (!decltype.wasRaw()) {
            // Explicitly provide actual type arguments
            List<AnnotatedTypeMirror> tas = decltype.getTypeArguments();
            // CAREFUL: return a copy, as we want to modify the type later.
            // TODO what's the difference for AnnotatedTypeReplacer?
            return tas.get(foundindex).shallowCopy(true);
        } else {
            // Type arguments not explicitly provided => use upper bound of var
            // TODO why?
            return var.getUpperBound();
        }
    }

    /**
     * Find the index(position) of this type variable from type
     * @param type type from which we infer actual type arguments
     * @param var formal type parameter
     * @return index(position) of this type variable from type
     */
    private static Pair<AnnotatedDeclaredType, Integer> findDeclType(
            AnnotatedDeclaredType type, AnnotatedTypeVariable var) {
        Element varelem = var.getUnderlyingType().asElement();

        DeclaredType dtype = type.getUnderlyingType();
        TypeElement el = (TypeElement) dtype.asElement();
        List<? extends TypeParameterElement> tparams = el.getTypeParameters();
        int foundindex = 0;

        for (TypeParameterElement tparam : tparams) {
            if (tparam.equals(varelem)
                    ||
                    //TODO: comparing by name!!!???
                    // Sometimes "E" and "E extends Object" are compared, which do not match by "equals".
                    tparam.getSimpleName().equals(varelem.getSimpleName())) {
                // we found the right index!
                break;
            }
            ++foundindex;
        }

        if (foundindex >= tparams.size()) {
            // didn't find the desired type :-( => Head for super type of "type"!
            for (AnnotatedDeclaredType sup : type.directSuperTypes()) {
                Pair<AnnotatedDeclaredType, Integer> res = findDeclType(sup, var);
                if (res != null) {
                    return res;
                }
            }
            // we reach this point if the variable wasn't found in any recursive call on ALL direct supertypes.
            return null;
        }

        return Pair.of(type, foundindex);
    }
}
