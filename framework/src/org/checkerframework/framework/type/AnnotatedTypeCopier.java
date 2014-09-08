package org.checkerframework.framework.type;

import org.checkerframework.framework.type.AnnotatedTypeMirror.*;
import org.checkerframework.framework.type.visitor.AnnotatedTypeVisitor;

import java.util.*;

public class AnnotatedTypeCopier implements AnnotatedTypeVisitor<AnnotatedTypeMirror, IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>> {
    private static AnnotatedTypeCopier typeCopier = new AnnotatedTypeCopier();

    @SuppressWarnings("unchecked")
    public static <T extends AnnotatedTypeMirror> T copy(final T type) {
        return (T) typeCopier.visit(type);
    }

    @Override
    public AnnotatedTypeMirror visit(AnnotatedTypeMirror type) {
        return type.accept(this, new IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>());
    }

    @Override
    public AnnotatedTypeMirror visit(AnnotatedTypeMirror type, IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
        return type.accept(this, originalToCopy);
    }

    @Override
    public AnnotatedTypeMirror visitDeclared(AnnotatedDeclaredType original, IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
        if(originalToCopy.containsKey(original)) {
            return originalToCopy.get(original);
        }

        final AnnotatedDeclaredType copy =  (AnnotatedDeclaredType) AnnotatedTypeMirror.createType(
                original.getUnderlyingType(), original.atypeFactory, original.isDeclaration());
        copy.addAnnotations(original.annotations);
        originalToCopy.put(original, copy);


        if(original.typeArgs != null) {
            final List<AnnotatedTypeMirror> copyTypeArgs = new ArrayList<AnnotatedTypeMirror>();
            for(final AnnotatedTypeMirror typeArg : original.typeArgs) {
                copyTypeArgs.add(visit(typeArg, originalToCopy));
            }
            copy.typeArgs = Collections.unmodifiableList( copyTypeArgs );
        }

        return copy;
    }

    @Override
    public AnnotatedTypeMirror visitIntersection(AnnotatedIntersectionType original, IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
        if(originalToCopy.containsKey(original)) {
            return originalToCopy.get(original);
        }

        final AnnotatedIntersectionType copy =  (AnnotatedIntersectionType) AnnotatedTypeMirror.createType(
                original.getUnderlyingType(), original.atypeFactory, original.isDeclaration());
        copy.addAnnotations(original.annotations);
        originalToCopy.put(original, copy);

        if(original.supertypes != null) {
            final List<AnnotatedDeclaredType> copySupertypes = new ArrayList<AnnotatedDeclaredType>();
            for(final AnnotatedDeclaredType supertype : original.supertypes) {
                copySupertypes.add((AnnotatedDeclaredType) visit(supertype, originalToCopy));
            }
            copy.supertypes = Collections.unmodifiableList( copySupertypes );
        }

        return copy;
    }

    @Override
    public AnnotatedTypeMirror visitUnion(AnnotatedUnionType original, IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
        if(originalToCopy.containsKey(original)) {
            return originalToCopy.get(original);
        }

        final AnnotatedUnionType copy =  (AnnotatedUnionType) AnnotatedTypeMirror.createType(
                original.getUnderlyingType(), original.atypeFactory, original.isDeclaration());
        copy.addAnnotations(original.annotations);
        originalToCopy.put(original, copy);

        if(original.alternatives != null) {
            final List<AnnotatedDeclaredType> copyAlternatives = new ArrayList<AnnotatedDeclaredType>();
            for(final AnnotatedDeclaredType supertype : original.alternatives) {
                copyAlternatives.add((AnnotatedDeclaredType) visit(supertype, originalToCopy));
            }
            copy.alternatives = Collections.unmodifiableList( copyAlternatives );
        }

        return copy;
    }

    @Override
    public AnnotatedTypeMirror visitExecutable(AnnotatedExecutableType original, IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
        if(originalToCopy.containsKey(original)) {
            return originalToCopy.get(original);
        }

        final AnnotatedExecutableType copy =  (AnnotatedExecutableType) AnnotatedTypeMirror.createType(
                original.getUnderlyingType(), original.atypeFactory, original.isDeclaration());
        copy.addAnnotations(original.annotations);
        originalToCopy.put(original, copy);

        copy.setElement(original.getElement());
        if(original.receiverType != null) {
            copy.receiverType = (AnnotatedDeclaredType) visit(original.receiverType, originalToCopy);
        }

        for(final AnnotatedTypeMirror param : original.paramTypes) {
            copy.paramTypes.add(visit(param, originalToCopy));
        }

        for(final AnnotatedTypeMirror thrown : original.throwsTypes) {
            copy.throwsTypes.add(visit(thrown, originalToCopy));
        }

        if(original.returnType != null) {
            copy.returnType = visit(original.returnType, originalToCopy);
        }

        for(final AnnotatedTypeVariable typeVariable : original.typeVarTypes) {
            copy.typeVarTypes.add((AnnotatedTypeVariable) visit(typeVariable, originalToCopy));
        }

        return copy;
    }

    @Override
    public AnnotatedTypeMirror visitArray(AnnotatedArrayType original, IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
        if(originalToCopy.containsKey(original)) {
            return originalToCopy.get(original);
        }

        final AnnotatedArrayType copy =  (AnnotatedArrayType) AnnotatedTypeMirror.createType(
                original.getUnderlyingType(), original.atypeFactory, original.isDeclaration());
        copy.addAnnotations(original.annotations);
        originalToCopy.put(original, copy);

        copy.setComponentType(visit(original.getComponentType(), originalToCopy));

        return copy;
    }

    @Override
    public AnnotatedTypeMirror visitTypeVariable(AnnotatedTypeVariable original, IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
        if(originalToCopy.containsKey(original)) {
            return originalToCopy.get(original);
        }

        final AnnotatedTypeVariable copy =  (AnnotatedTypeVariable) AnnotatedTypeMirror.createType(
                original.getUnderlyingType(), original.atypeFactory, original.isDeclaration());
        copy.addAnnotations(original.annotations);
        originalToCopy.put(original, copy);

        if(original.getUpperBoundField() != null) {
            copy.setUpperBoundField(visit(original.getUpperBoundField(), originalToCopy));
        }

        if(original.getLowerBoundField() != null) {
            copy.setLowerBoundField(visit(original.getLowerBoundField(), originalToCopy));
        }

        return copy;
    }

    @Override
    public AnnotatedTypeMirror visitPrimitive(AnnotatedPrimitiveType original, IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
        return makeOrReturnCopy(original, originalToCopy);
    }

    @Override
    public AnnotatedTypeMirror visitNoType(AnnotatedNoType original, IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
        return makeOrReturnCopy(original, originalToCopy);
    }

    @Override
    public AnnotatedTypeMirror visitNull(AnnotatedNullType original, IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
        return makeOrReturnCopy(original, originalToCopy);
    }

    @Override
    public AnnotatedTypeMirror visitWildcard(AnnotatedWildcardType original, IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
        if(originalToCopy.containsKey(original)) {
            return originalToCopy.get(original);
        }

        final AnnotatedWildcardType copy =  (AnnotatedWildcardType) AnnotatedTypeMirror.createType(
                original.getUnderlyingType(), original.atypeFactory, original.isDeclaration());
        if(original.isTypeArgHack()) {
            copy.setTypeArgHack();
        }

        copy.addAnnotations(original.annotations);
        originalToCopy.put(original, copy);

        if(original.getExtendsBoundField() != null) {
            copy.setExtendsBound(visit(original.getExtendsBoundField(), originalToCopy));
        }

        if(original.getSuperBoundField() != null) {
            copy.setSuperBound(visit(original.getSuperBoundField(), originalToCopy));
        }

        return copy;
    }

    @SuppressWarnings("unchecked")
    public <T extends AnnotatedTypeMirror> AnnotatedTypeMirror makeOrReturnCopy(
            T original, IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
        if(originalToCopy.containsKey(original)) {
            return originalToCopy.get(original);
        }

        final T copy =  (T) AnnotatedTypeMirror.createType(
                original.getUnderlyingType(), original.atypeFactory, original.isDeclaration());
        copy.addAnnotations(original.annotations);
        originalToCopy.put(original, copy);

        return copy;
    }
}
