package org.checkerframework.framework.type;

import org.checkerframework.framework.type.AnnotatedTypeMirror.*;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeParameterElement;
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
     * @see #substituteTypeVariable(org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable, AnnotatedTypeMirror, org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable)
     * @param typeParamToArg
     * @param typeMirror
     * @return A copy of typeMirror with its type variables substituted
     */
    public AnnotatedTypeMirror subtitute(final Map<AnnotatedTypeVariable, AnnotatedTypeMirror> typeParamToArg,
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
     * @param declaration The declaration of a type variable that is being substituted
     * @param argument    The argument to declaration (this will be a value in typeParamToArg)
     * @param use  The use that is being replaced
     * @return a shallow copy of argument with the appropriate annotations applied
     */
    protected AnnotatedTypeMirror substituteTypeVariable(final AnnotatedTypeVariable declaration,
                                                         final AnnotatedTypeMirror argument,
                                                         final AnnotatedTypeVariable use) {
        final AnnotatedTypeMirror substitute = argument.shallowCopy(false);
        substitute.addAnnotations(argument.annotations);

        if(declaration.annotations.size() > 0) {
            ErrorReporter.errorAbort("Type parameter declarations should NOT have primary annotations!"
                  + "declaration=" + declaration + "\n"
                  + "argument=" + argument + "\n"
                  + "use=" + use);
        }

        if (!use.annotations.isEmpty()) {
            substitute.replaceAnnotations(use.getAnnotations());
        }

        return substitute;
    }

    protected class Visitor extends AnnotatedTypeCopier {
        private Map<TypeParameterElement, Pair<AnnotatedTypeVariable, AnnotatedTypeMirror>> elementToArgMap;

        public Visitor(final Map<AnnotatedTypeVariable, AnnotatedTypeMirror> typeParamToArg) {
            elementToArgMap = new HashMap<>();

            for (Entry<AnnotatedTypeVariable, AnnotatedTypeMirror> paramToArg : typeParamToArg.entrySet()) {
                elementToArgMap.put((TypeParameterElement) paramToArg.getKey().getUnderlyingType().asElement(),
                                    Pair.of(paramToArg.getKey(), paramToArg.getValue()));
            }
        }

        @Override
        public AnnotatedTypeMirror visitTypeVariable(AnnotatedTypeVariable original, IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
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
                    final Pair<AnnotatedTypeVariable, AnnotatedTypeMirror> paramToArg = elementToArgMap.get(typeVarElem);
                    final AnnotatedTypeVariable declaration = paramToArg.first;
                    final AnnotatedTypeMirror argument = paramToArg.second;
                    return substituteTypeVariable(declaration, argument, original);
                }
            }

            return super.visitTypeVariable(original, originalToCopy);
        }
    }
}
