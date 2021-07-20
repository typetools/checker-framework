package org.checkerframework.framework.type;

import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

import java.util.IdentityHashMap;

/** Duplicates annotated types and replaces components according to a replacement map. */
public class AnnotatedTypeCopierWithReplacement {

    /**
     * Return a copy of type after making the specified replacements.
     *
     * @param type the type that will be copied with replaced components
     * @param replacementMap a mapping of {@literal referenceToReplace => referenceOfReplacement}
     * @return a duplicate of type in which every reference that was a key in replacementMap has
     *     been replaced by its corresponding value
     */
    public static AnnotatedTypeMirror replace(
            AnnotatedTypeMirror type,
            IdentityHashMap<? extends AnnotatedTypeMirror, ? extends AnnotatedTypeMirror>
                    replacementMap) {
        return new Visitor(replacementMap).visit(type);
    }

    /**
     * AnnotatedTypeCopier maintains a mapping of {@literal typeVisited => copyOfTypeVisited} When a
     * reference, typeVisited, is encountered again, it will use the recorded reference,
     * copyOfTypeVisited, instead of generating a new copy of typeVisited. Visitor pre-populates
     * this mapping so that references are replaced not by their copies but by those in the
     * replacementMap provided in the constructor.
     *
     * <p>All types NOT in the replacement map are duplicated as per AnnotatedTypeCopier.visit
     */
    protected static class Visitor extends AnnotatedTypeCopier {

        private final IdentityHashMap<? extends AnnotatedTypeMirror, ? extends AnnotatedTypeMirror>
                originalMappings;

        public Visitor(
                final IdentityHashMap<? extends AnnotatedTypeMirror, ? extends AnnotatedTypeMirror>
                        mappings) {
            originalMappings = new IdentityHashMap<>(mappings);
        }

        @Override
        public AnnotatedTypeMirror visit(AnnotatedTypeMirror type) {
            return type.accept(this, new IdentityHashMap<>(originalMappings));
        }

        @Override
        public AnnotatedTypeMirror visitTypeVariable(
                AnnotatedTypeVariable original,
                IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
            // AnnotatedTypeCopier will visit the type parameters of a method and copy them.
            // Without this flag, any mappings in originalToCopy would replace the type parameters.
            // However, we do not replace the type parameters in an AnnotatedExecutableType.  Also,
            // AnnotatedExecutableType.typeVarTypes is of type List<AnnotatedTypeVariable> so if the
            // mapping contained a type parameter -> (Non-type variable AnnotatedTypeMirror) then a
            // runtime exception would occur.
            if (visitingExecutableTypeParam) {
                visitingExecutableTypeParam = false;
                final AnnotatedTypeVariable copy =
                        (AnnotatedTypeVariable)
                                AnnotatedTypeMirror.createType(
                                        original.getUnderlyingType(),
                                        original.atypeFactory,
                                        original.isDeclaration());
                maybeCopyPrimaryAnnotations(original, copy);
                originalToCopy.put(original, copy);

                if (original.getUpperBoundField() != null) {
                    copy.setUpperBound(visit(original.getUpperBoundField(), originalToCopy));
                }

                if (original.getLowerBoundField() != null) {
                    copy.setLowerBound(visit(original.getLowerBoundField(), originalToCopy));
                }
                return copy;
            }

            return super.visitTypeVariable(original, originalToCopy);
        }
    }
}
