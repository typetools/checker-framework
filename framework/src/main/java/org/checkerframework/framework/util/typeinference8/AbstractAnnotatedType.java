package org.checkerframework.framework.util.typeinference8;

import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.javacutil.typeinference8.types.AbstractType;
import org.checkerframework.javacutil.typeinference8.types.ProperType;
import org.checkerframework.javacutil.typeinference8.util.Java8InferenceContext;

public abstract class AbstractAnnotatedType implements AbstractType {
    protected final Java8InferenceContext context;
    AnnotatedTypeFactory typeFactory;

    public AbstractAnnotatedType(Java8InferenceContext context) {
        this.context = context;
    }

    public abstract AbstractType create(AnnotatedTypeMirror type);

    /** @return the TypeKind of the underlying Java type */
    @Override
    public final TypeKind getTypeKind() {
        return getJavaType().getKind();
    }

    public abstract AnnotatedTypeMirror getAnnotatedType();

    /**
     * Assuming the type is a declared type, this method returns the upper bounds of its type
     * parameters. (A type parameter of a declared type, can't refer to any type being inferred, so
     * they are proper types.)
     */
    @Override
    public List<ProperType> getTypeParameterBounds() {
        List<ProperType> bounds = new ArrayList<>();
        TypeElement typeelem = (TypeElement) ((DeclaredType) getJavaType()).asElement();
        List<AnnotatedTypeParameterBounds> typeVars =
                typeFactory.typeVariablesFromUse(
                        (AnnotatedDeclaredType) getAnnotatedType(), typeelem);

        for (AnnotatedTypeParameterBounds bound : typeVars) {
            bounds.add(new ProperAnnotatedType(bound.getUpperBound(), context));
        }
        return bounds;
    }

    /** @return a new type that is the capture of this type. */
    @Override
    public AbstractType capture() {

        // TODO:
        throw new RuntimeException("not implemented");
        //        TypeMirror capture;
        //        if (getJavaType().getKind() == TypeKind.WILDCARD) {
        //            capture =
        //                    context.types.freshTypeVariables(
        //                            com.sun.tools.javac.util.List.of((Type) getJavaType()))
        //                            .head;
        //        } else {
        //            capture = context.env.getTypeUtils().capture(getJavaType());
        //        }
        //        return create(capture);
    }

    /**
     * If {@code superType} is a super type of this type, then this method returns the super type of
     * this type that is the same class as {@code superType}. Otherwise, it returns null
     *
     * @param superType a type, need not be a super type of this type
     * @return super type of this type that is the same class as {@code superType} or null if one
     *     doesn't exist
     */
    @Override
    public final AbstractType asSuper(AbstractType superType) {
        AnnotatedTypeMirror type = getAnnotatedType();
        AnnotatedTypeMirror superAnnotatedType =
                ((AbstractAnnotatedType) superType).getAnnotatedType();
        if (type.getKind() == TypeKind.WILDCARD) {
            type = ((AnnotatedWildcardType) type).getExtendsBound();
        }
        AnnotatedTypeMirror asSuper;
        if (TypesUtils.isErasedSubtype(
                type.getUnderlyingType(),
                superAnnotatedType.getUnderlyingType(),
                context.modelTypes)) {
            asSuper = AnnotatedTypes.asSuper(typeFactory, type, superAnnotatedType);
        } else {
            return null;
        }

        if (TypesUtils.isCaptured(type.getUnderlyingType())) {
            return create(asSuper).capture();
        }
        return create(asSuper);
    }

    /**
     * If this type is a functional interface, then this method returns the return type of the
     * function type of that functional interface. Otherwise, returns null.
     *
     * @return the return type of the function type of this type or null if one doesn't exist
     */
    @Override
    public final AbstractType getFunctionTypeReturnType() {
        if (TypesUtils.isFunctionalInterface(getJavaType(), context.env)) {
            ExecutableElement element = TypesUtils.findFunction(getJavaType(), context.env);
            AnnotatedExecutableType aet =
                    typeFactory.getFunctionType(
                            element, (AnnotatedDeclaredType) getAnnotatedType());
            AnnotatedTypeMirror returnType = aet.getReturnType();
            if (returnType.getKind() == TypeKind.VOID) {
                return null;
            }
            return create(returnType);
        } else {
            return null;
        }
    }

    /**
     * If this type is a functional interface, then this method returns the parameter types of the
     * function type of that functional interface. Otherwise, it returns null.
     *
     * @return the parameter types of the function type of this type or null if no function type
     *     exists.
     */
    @Override
    public final List<AbstractType> getFunctionTypeParameterTypes() {
        if (TypesUtils.isFunctionalInterface(getJavaType(), context.env)) {
            ExecutableElement element = TypesUtils.findFunction(getJavaType(), context.env);
            AnnotatedExecutableType aet =
                    typeFactory.getFunctionType(
                            element, (AnnotatedDeclaredType) getAnnotatedType());
            List<AbstractType> params = new ArrayList<>();
            for (AnnotatedTypeMirror param : aet.getParameterTypes()) {
                params.add(create(param));
            }
            return params;
        } else {
            return null;
        }
    }

    /** @return true if the type is a raw type */
    @Override
    public final boolean isRaw() {
        return getAnnotatedType().getKind() == TypeKind.DECLARED
                && ((AnnotatedDeclaredType) getAnnotatedType()).wasRaw();
    }

    /**
     * @return a new type that is the same type as this one, but whose type arguments are {@code
     *     args}
     */
    @Override
    public final AbstractType replaceTypeArgs(List<AbstractType> args) {
        DeclaredType declaredType = (DeclaredType) getJavaType();
        TypeMirror[] newArgs = new TypeMirror[args.size()];
        int i = 0;
        for (AbstractType t : args) {
            newArgs[i++] = t.getJavaType();
        }
        TypeMirror newTypeJava =
                context.env
                        .getTypeUtils()
                        .getDeclaredType((TypeElement) declaredType.asElement(), newArgs);
        AnnotatedDeclaredType newType =
                (AnnotatedDeclaredType)
                        AnnotatedTypeMirror.createType(
                                newTypeJava, typeFactory, getAnnotatedType().isDeclaration());
        List<AnnotatedTypeMirror> argTypes = new ArrayList<>();
        for (AbstractType arg : args) {
            argTypes.add(((AbstractAnnotatedType) arg).getAnnotatedType());
        }
        newType.setTypeArguments(argTypes);
        newType.replaceAnnotations(getAnnotatedType().getAnnotations());
        return create(newType);
    }

    /**
     * Whether the proper type is a parameterized class or interface type, or an inner class type of
     * a parameterized class or interface type (directly or indirectly)
     *
     * @return whether T is a parameterized type.
     */
    @Override
    public final boolean isParameterizedType() {
        // TODO this isn't matching the JavaDoc.
        return ((Type) getJavaType()).isParameterized();
    }

    /**
     * @return the most specific array type that is a super type of this type or null if one doesn't
     *     exist
     */
    @Override
    public final AbstractType getMostSpecificArrayType() {
        if (getTypeKind() == TypeKind.ARRAY) {
            return this;
        } else {
            for (AnnotatedTypeMirror superType : this.getAnnotatedType().directSuperTypes()) {
                AbstractType arrayType = create(superType).getMostSpecificArrayType();
                if (arrayType != null) {
                    return arrayType;
                }
            }
            return null;
        }
    }

    /** @return true if this type is a primitive array. */
    @Override
    public final boolean isPrimitiveArray() {
        return getJavaType().getKind() == TypeKind.ARRAY
                && ((ArrayType) getJavaType()).getComponentType().getKind().isPrimitive();
    }

    /**
     * @return assuming type is an intersection type, this method returns the bounds in this type
     */
    @Override
    public final List<AbstractType> getIntersectionBounds() {
        List<AbstractType> bounds = new ArrayList<>();
        for (AnnotatedTypeMirror bound :
                ((AnnotatedIntersectionType) getAnnotatedType()).directSuperTypes()) {
            bounds.add(create(bound));
        }
        return bounds;
    }

    /**
     * @return assuming this type is a type variable, this method returns the upper bound of this
     *     type.
     */
    @Override
    public final AbstractType getTypeVarUpperBound() {
        return create(((AnnotatedTypeVariable) getAnnotatedType()).getUpperBound());
    }

    /**
     * @return assuming this type is a type variable that has a lower bound, this method returns the
     *     lower bound of this type.
     */
    @Override
    public final AbstractType getTypeVarLowerBound() {
        return create(((AnnotatedTypeVariable) getAnnotatedType()).getLowerBound());
    }

    /** @return true if this type is a type variable with a lower bound */
    @Override
    public final boolean isLowerBoundTypeVariable() {
        return ((AnnotatedTypeVariable) getAnnotatedType()).getLowerBound().getKind()
                != TypeKind.NULL;
    }

    /**
     * @return true if this type is a parameterized type whose has at least one wildcard as a type
     *     argument.
     */
    @Override
    public final boolean isWildcardParameterizedType() {
        return TypesUtils.isWildcardParameterized(getJavaType());
    }

    /** @return this type's type arguments or null this type isn't a declared type */
    @Override
    public final List<AbstractType> getTypeArguments() {
        if (getJavaType().getKind() != TypeKind.DECLARED) {
            return null;
        }
        List<AbstractType> list = new ArrayList<>();
        for (AnnotatedTypeMirror typeArg :
                ((AnnotatedDeclaredType) getAnnotatedType()).getTypeArguments()) {
            list.add(create(typeArg));
        }
        return list;
    }

    /** @return true if the type is an unbound wildcard */
    @Override
    public final boolean isUnboundWildcard() {
        return TypesUtils.isUnboundWildcard(getJavaType());
    }

    /** @return true if the type is a wildcard with an upper bound */
    @Override
    public final boolean isUpperBoundedWildcard() {
        return TypesUtils.isExtendsBoundWildcard(getJavaType());
    }

    /** @return true if the type is a wildcard with a lower bound */
    @Override
    public final boolean isLowerBoundedWildcard() {
        return TypesUtils.isSuperBoundWildcard(getJavaType());
    }

    /** @return if this type is a wildcard return its lower bound; otherwise, return null. */
    @Override
    public final AbstractType getWildcardLowerBound() {
        if (getJavaType().getKind() == TypeKind.WILDCARD) {
            return create(((AnnotatedWildcardType) getAnnotatedType()).getSuperBound());
        }
        return null;
    }

    /** @return if this type is a wildcard return its upper bound; otherwise, return null. */
    @Override
    public final AbstractType getWildcardUpperBound() {
        if (getJavaType().getKind() != TypeKind.WILDCARD) {
            return null;
        } else if (((Type.WildcardType) getJavaType()).isExtendsBound()) {
            return create(((AnnotatedWildcardType) getAnnotatedType()).getExtendsBound());
        } else {
            return null;
        }
    }

    /** @return a new type whose Java type is the erasure of this type */
    @Override
    public AbstractType getErased() {
        return create(getAnnotatedType().getErased());
    }

    /** @return the array component type of this type or null if one does not exist. */
    @Override
    public final AbstractType getComponentType() {
        if (getJavaType().getKind() == TypeKind.ARRAY) {
            return create(((AnnotatedArrayType) getAnnotatedType()).getComponentType());
        } else {
            return null;
        }
    }
}
