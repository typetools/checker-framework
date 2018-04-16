package org.checkerframework.framework.util.typeinference8.types;

import java.util.Collection;
import java.util.List;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.util.typeinference8.types.typemirror.InferenceTypeMirror;
import org.checkerframework.framework.util.typeinference8.types.typemirror.ProperTypeMirror;
import org.checkerframework.framework.util.typeinference8.types.typemirror.VariableTypeMirror;

public interface AbstractType {

    /** Creates an {@link AbstractType} with underlying type {@code type}. */
    AbstractType create(TypeMirror type);

    /** Returns the kind of {@link AbstractType}. */
    Kind getKind();

    /** @return true if this type is a proper type. */
    default boolean isProper() {
        return getKind() == Kind.PROPER;
    }

    /** @return true if this type is an inference variable. */
    default boolean isVariable() {
        return getKind() == Kind.VARIABLE;
    }

    /** @return true if this type contains inference variables, but is not an inference variable */
    default boolean isInferenceType() {
        return getKind() == Kind.INFERENCE_TYPE;
    }

    /** @return the TypeKind of the underlying Java type */
    TypeKind getTypeKind();

    /** @return the underlying Java type without inference variables. */
    TypeMirror getJavaType();

    /** @return a collection of all inference variables referenced by this type. */
    Collection<Variable> getInferenceVariables();

    /**
     * @return a new type that is the same as this one except the variables in {@code
     *     instantiations} have been replaced by their instantiation.
     */
    AbstractType applyInstantiations(List<Variable> instantiations);

    /** @return true if this type is java.lang.Object. */
    boolean isObject();

    /**
     * Assuming the type is a declared type, this method returns the upper bounds of its type
     * parameters. (A type parameter of a declared type, can't refer to any type being inferred, so
     * they are proper types.)
     */
    List<ProperType> getTypeParameterBounds();

    /** @return a new type that is the capture of this type. */
    AbstractType capture();

    /**
     * If {@code superType} is a super type of this type, then this method returns the super type of
     * this type that is the same class as {@code superType}. Otherwise, it returns null
     *
     * @param superType a type, need not be a super type of this type
     * @return super type of this type that is the same class as {@code superType} or null if one
     *     doesn't exist
     */
    AbstractType asSuper(AbstractType superType);

    /**
     * If this type is a functional interface, then this method returns the return type of the
     * function type of that functional interface. Otherwise, returns null.
     *
     * @return the return type of the function type of this type or null if one doesn't exist
     */
    AbstractType getFunctionTypeReturnType();

    /**
     * If this type is a functional interface, then this method returns the parameter types of the
     * function type of that functional interface. Otherwise, it returns null.
     *
     * @return the parameter types of the function type of this type or null if no function type
     *     exists.
     */
    List<AbstractType> getFunctionTypeParameterTypes();

    /** @return true if the type is a raw type */
    boolean isRaw();

    /**
     * @return a new type that is the same type as this one, but whose type arguments are {@code
     *     args}
     */
    AbstractType replaceTypeArgs(List<AbstractType> args);

    /**
     * Whether the proper type is a parameterized class or interface type, or an inner class type of
     * a parameterized class or interface type (directly or indirectly)
     *
     * @return whether T is a parameterized type.
     */
    boolean isParameterizedType();

    /**
     * @return the most specific array type that is a super type of this type or null if one doesn't
     *     exist
     */
    AbstractType getMostSpecificArrayType();

    /** @return true if this type is a primitive array. */
    boolean isPrimitiveArray();

    /**
     * @return assuming type is an intersection type, this method returns the bounds in this type
     */
    List<AbstractType> getIntersectionBounds();

    /**
     * @return assuming this type is a type variable, this method returns the upper bound of this
     *     type.
     */
    AbstractType getTypeVarUpperBound();

    /**
     * @return assuming this type is a type variable that has a lower bound, this method returns the
     *     lower bound of this type.
     */
    AbstractType getTypeVarLowerBound();

    /** @return true if this type is a type variable with a lower bound */
    boolean isLowerBoundTypeVariable();

    /**
     * @return true if this type is a parameterized type whose has at least one wildcard as a type
     *     argument.
     */
    boolean isWildcardParameterizedType();

    /** @return this type's type arguments or null this type isn't a declared type */
    List<AbstractType> getTypeArguments();

    /** @return true if the type is an unbound wildcard */
    boolean isUnboundWildcard();

    /** @return true if the type is a wildcard with an upper bound */
    boolean isUpperBoundedWildcard();

    /** @return true if the type is a wildcard with a lower bound */
    boolean isLowerBoundedWildcard();

    /** @return if this type is a wildcard return its lower bound; otherwise, return null. */
    AbstractType getWildcardLowerBound();

    /** @return if this type is a wildcard return its upper bound; otherwise, return null. */
    AbstractType getWildcardUpperBound();

    /** @return a new type whose Java type is the erasure of this type */
    AbstractType getErased();

    /** @return the array component type of this type or null if one does not exist. */
    AbstractType getComponentType();

    public enum Kind {
        /** {@link ProperTypeMirror},a type that contains no inference variables* */
        PROPER,
        /** {@link VariableTypeMirror}, an inference variable. */
        VARIABLE,
        /**
         * {@link InferenceTypeMirror}, a type that contains inference variables, but is not an
         * inference variable.
         */
        INFERENCE_TYPE
    }
}
