package org.checkerframework.framework.type;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

/** TypeVariableSusbtitutor replaces type variables from a declaration with arguments to its use. */
public class TypeVariableSubstitutor {

    /**
     * Given a mapping between type variable's to typeArgument, replace each instance of type
     * variable with a copy of type argument.
     *
     * @see #substituteTypeVariable(AnnotatedTypeMirror,
     *     org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable)
     * @return a copy of typeMirror with its type variables substituted
     */
    public AnnotatedTypeMirror substitute(
            final Map<TypeVariable, AnnotatedTypeMirror> typeParamToArg,
            final AnnotatedTypeMirror typeMirror) {

        return new Visitor(typeParamToArg).visit(typeMirror);
    }

    /**
     * Given the types of a type parameter declaration, the argument to that type parameter
     * declaration, and a given use of that declaration, return a substitute for the use with the
     * correct annotations.
     *
     * <p>To determine what primary annotations are correct for the substitute the following rules
     * are used: If the type variable use has a primary annotation then apply that primary
     * annotation to the substitute. Otherwise, use the annotations of the argument.
     *
     * @param argument the argument to declaration (this will be a value in typeParamToArg)
     * @param use the use that is being replaced
     * @return a deep copy of argument with the appropriate annotations applied
     */
    protected AnnotatedTypeMirror substituteTypeVariable(
            final AnnotatedTypeMirror argument, final AnnotatedTypeVariable use) {
        final AnnotatedTypeMirror substitute = argument.deepCopy(true);
        substitute.addAnnotations(argument.getAnnotationsField());

        if (!use.getAnnotationsField().isEmpty()) {
            substitute.replaceAnnotations(use.getAnnotations());
        }

        return substitute;
    }

    protected class Visitor extends AnnotatedTypeCopier {
        private final Map<TypeParameterElement, AnnotatedTypeMirror> elementToArgMap;

        public Visitor(final Map<TypeVariable, AnnotatedTypeMirror> typeParamToArg) {
            elementToArgMap = new HashMap<>();

            for (Entry<TypeVariable, AnnotatedTypeMirror> paramToArg : typeParamToArg.entrySet()) {
                elementToArgMap.put(
                        (TypeParameterElement) paramToArg.getKey().asElement(),
                        paramToArg.getValue());
            }
        }

        @Override
        public AnnotatedTypeMirror visitArray(
                AnnotatedArrayType original,
                IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
            if (originalToCopy.containsKey(original)) {
                return originalToCopy.get(original);
            }

            final AnnotatedArrayType copy =
                    (AnnotatedArrayType)
                            AnnotatedTypeMirror.createType(
                                    original.getUnderlyingType(),
                                    original.atypeFactory,
                                    original.isDeclaration());
            maybeCopyPrimaryAnnotations(original, copy);
            originalToCopy.put(original, copy);

            // Substitution (along with any other operation that changes the component types of an
            // AnnotatedTypeMirror) may change the underlying Java type of components without
            // updating the underlying Java type of the parent type.  We use the underlying type for
            // various purposes (including equals/hashcode) so this can lead to unpredictable
            // behavior.  Currently, we update the underlying type when substituting on arrays in
            // order to avoid an error in LubTypeVariableAnnotator.
            // TODO: Presumably there are more cases in which we want to do this
            final AnnotatedTypeMirror componentType =
                    visit(original.getComponentType(), originalToCopy);
            final Types types = componentType.atypeFactory.types;

            final AnnotatedArrayType correctedCopy;
            if (!types.isSameType(componentType.getUnderlyingType(), copy.getUnderlyingType())
                    && componentType.getKind()
                            != TypeKind.WILDCARD) { // TODO: THIS SHOULD BE CAPTURE CONVERTED
                final TypeMirror underlyingType =
                        types.getArrayType(componentType.getUnderlyingType());
                correctedCopy =
                        (AnnotatedArrayType)
                                AnnotatedTypeMirror.createType(
                                        underlyingType, copy.atypeFactory, false);
                correctedCopy.addAnnotations(copy.getAnnotations());

            } else {
                correctedCopy = copy;
            }

            correctedCopy.setComponentType(componentType);

            return correctedCopy;
        }

        @Override
        public AnnotatedTypeMirror visitTypeVariable(
                AnnotatedTypeVariable original,
                IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {

            if (visitingExecutableTypeParam) {
                // AnnotatedExecutableType differs from AnnotatedDeclaredType in that its list of
                // type parameters cannot be adapted in place since the
                // AnnotatedExecutable.typeVarTypes field is of type AnnotatedTypeVariable and not
                // AnnotatedTypeMirror.  When substituting, all component types that contain a use
                // of the executable's type parameters will be substituted.  The executable's type
                // parameters will have their bounds substituted but the top-level
                // AnnotatedTypeVariable's will remain
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
