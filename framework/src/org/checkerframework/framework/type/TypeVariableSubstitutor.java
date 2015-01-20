package org.checkerframework.framework.type;

import org.checkerframework.framework.type.AnnotatedTypeMirror.*;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeVariable;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * TypeVariableSusbtitutor replaces type variables from a declaration with arguments to its use.
 */
public class TypeVariableSubstitutor {

    /**
     * Given a mapping between type variable's to typeArgument, replace each instance of
     * type variable with a copy of type argument.
     * @see #substituteTypeVariable(AnnotatedTypeMirror, org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable)
     * @param typeParamToArg
     * @param typeMirror
     * @return A copy of typeMirror with its type variables substituted
     */
    public AnnotatedTypeMirror substitute(final Map<TypeVariable, AnnotatedTypeMirror> typeParamToArg,
            final AnnotatedTypeMirror typeMirror) {

        return new Visitor(typeParamToArg).visit(typeMirror);
    }

    /**
     * Given the types of a type parameter declaration, the argument to that type parameter declaration,
     * and a given use of that declaration, return a substitute for the use with the correct annotations.
     *
     * To determine what primary annotations are correct for the substitute the following rules are used:
     * If the type variable use has a primary annotation then apply that primary annotation to the substitute.
     * Otherwise, use the annotations of the argument.
     *
     * @param argument    The argument to declaration (this will be a value in typeParamToArg)
     * @param use  The use that is being replaced
     * @return a shallow copy of argument with the appropriate annotations applied
     */
    protected AnnotatedTypeMirror substituteTypeVariable(final AnnotatedTypeMirror argument,
                                                         final AnnotatedTypeVariable use) {
        final AnnotatedTypeMirror substitute = argument.shallowCopy(false);
        substitute.addAnnotations(argument.annotations);

        if (!use.annotations.isEmpty()) {
            substitute.replaceAnnotations(use.getAnnotations());
        }

        return substitute;
    }

    protected class Visitor extends AnnotatedTypeCopier {
        private final Map<TypeParameterElement,AnnotatedTypeMirror> elementToArgMap;

        public Visitor(final Map<TypeVariable, AnnotatedTypeMirror> typeParamToArg) {
            elementToArgMap = new HashMap<>();

            for (Entry<TypeVariable, AnnotatedTypeMirror> paramToArg : typeParamToArg.entrySet()) {
                elementToArgMap.put((TypeParameterElement) paramToArg.getKey().asElement(), paramToArg.getValue());
            }
        }

        @Override
        public AnnotatedTypeMirror visitTypeVariable(
                AnnotatedTypeVariable original,
                IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {

            if (visitingExecutableTypeParam) {
                //AnnotatedExecutableType differs from AnnotatedDeclaredType in that its list of
                //type parameters cannot be adapted in place since the AnnotatedExecutable.typeVarTypes
                //field is of type AnnotatedTypeVariable and not AnnotatedTypeMirror.
                //When substituting, all component types that contain a use of the executable's type parameters
                //will be substituted.  The executable's type parameters will have their bounds substituted
                //but the top-level AnnotatedTypeVariable's will remain
                visitingExecutableTypeParam = false;
                return super.visitTypeVariable(original, originalToCopy);

            } else {
                final Element typeVarElem = original.getUnderlyingType().asElement();
                if (elementToArgMap.containsKey(typeVarElem)) {
                    final AnnotatedTypeMirror argument = elementToArgMap.get(typeVarElem);
                    return substituteTypeVariable(argument, original);
                }
            }

            return super.visitTypeVariable(original, originalToCopy);
        }
    }
}
