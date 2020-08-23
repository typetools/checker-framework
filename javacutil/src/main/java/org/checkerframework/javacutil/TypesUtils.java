package org.checkerframework.javacutil;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.CapturedType;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiers;
import org.plumelib.util.ImmutableTypes;

/** A utility class that helps with {@link TypeMirror}s. */
// TODO: This class needs significant restructuring
public final class TypesUtils {

    // Class cannot be instantiated
    private TypesUtils() {
        throw new AssertionError("Class TypesUtils cannot be instantiated.");
    }

    /**
     * Gets the fully qualified name for a provided type. It returns an empty name if type is an
     * anonymous type.
     *
     * @param type the declared type
     * @return the name corresponding to that type
     */
    public static @DotSeparatedIdentifiers Name getQualifiedName(DeclaredType type) {
        TypeElement element = (TypeElement) type.asElement();
        return element.getQualifiedName();
    }

    /**
     * Checks if the type represents a java.lang.Object declared type.
     *
     * @param type the type
     * @return true iff type represents java.lang.Object
     */
    public static boolean isObject(TypeMirror type) {
        return isDeclaredOfName(type, "java.lang.Object");
    }

    /**
     * Checks if the type represents the java.lang.Class declared type.
     *
     * @param type the type
     * @return true iff type represents java.lang.Class
     */
    public static boolean isClass(TypeMirror type) {
        return isDeclaredOfName(type, "java.lang.Class");
    }

    /**
     * Checks if the type represents a java.lang.String declared type.
     *
     * @param type the type
     * @return true iff type represents java.lang.String
     */
    public static boolean isString(TypeMirror type) {
        return isDeclaredOfName(type, "java.lang.String");
    }

    /**
     * Checks if the type represents a boolean type, that is either boolean (primitive type) or
     * java.lang.Boolean.
     *
     * @param type the type to test
     * @return true iff type represents a boolean type
     */
    public static boolean isBooleanType(TypeMirror type) {
        return isDeclaredOfName(type, "java.lang.Boolean") || type.getKind() == TypeKind.BOOLEAN;
    }

    /**
     * Check if the type represents a declared type of the given qualified name.
     *
     * @param type the type
     * @return type iff type represents a declared type of the qualified name
     */
    public static boolean isDeclaredOfName(TypeMirror type, CharSequence qualifiedName) {
        return type.getKind() == TypeKind.DECLARED
                && getQualifiedName((DeclaredType) type).contentEquals(qualifiedName);
    }

    public static boolean isBoxedPrimitive(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }

        String qualifiedName = getQualifiedName((DeclaredType) type).toString();

        return (qualifiedName.equals("java.lang.Boolean")
                || qualifiedName.equals("java.lang.Byte")
                || qualifiedName.equals("java.lang.Character")
                || qualifiedName.equals("java.lang.Short")
                || qualifiedName.equals("java.lang.Integer")
                || qualifiedName.equals("java.lang.Long")
                || qualifiedName.equals("java.lang.Double")
                || qualifiedName.equals("java.lang.Float"));
    }

    /**
     * Return true if this is an immutable type in the JDK.
     *
     * <p>This does not use immutability annotations and always returns false for user-defined
     * classes.
     */
    public static boolean isImmutableTypeInJdk(TypeMirror type) {
        return isPrimitive(type)
                || (type.getKind() == TypeKind.DECLARED
                        && ImmutableTypes.isImmutable(
                                getQualifiedName((DeclaredType) type).toString()));
    }

    /**
     * Returns true if type represents a Throwable type (e.g. Exception, Error).
     *
     * @return true if type represents a Throwable type (e.g. Exception, Error)
     */
    public static boolean isThrowable(TypeMirror type) {
        while (type != null && type.getKind() == TypeKind.DECLARED) {
            DeclaredType dt = (DeclaredType) type;
            TypeElement elem = (TypeElement) dt.asElement();
            Name name = elem.getQualifiedName();
            if ("java.lang.Throwable".contentEquals(name)) {
                return true;
            }
            type = elem.getSuperclass();
        }
        return false;
    }

    /**
     * Returns true iff the argument is an anonymous type.
     *
     * @return whether the argument is an anonymous type
     */
    public static boolean isAnonymous(TypeMirror type) {
        return (type instanceof DeclaredType)
                && ((TypeElement) ((DeclaredType) type).asElement()).getNestingKind()
                        == NestingKind.ANONYMOUS;
    }

    /**
     * Returns true iff the argument is a primitive type.
     *
     * @return whether the argument is a primitive type
     */
    public static boolean isPrimitive(TypeMirror type) {
        switch (type.getKind()) {
            case BOOLEAN:
            case BYTE:
            case CHAR:
            case DOUBLE:
            case FLOAT:
            case INT:
            case LONG:
            case SHORT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns true iff the arguments are both the same declared types.
     *
     * <p>This is needed because class {@code Type.ClassType} does not override equals.
     *
     * @param t1 the first type to test
     * @param t2 the second type to test
     * @return whether the arguments are the same declared types
     */
    public static boolean areSameDeclaredTypes(Type.ClassType t1, Type.ClassType t2) {
        // Do a cheaper test first
        if (t1.tsym.name != t2.tsym.name) {
            return false;
        }
        return t1.toString().equals(t1.toString());
    }

    /**
     * Returns true iff the arguments are both the same primitive types.
     *
     * @return whether the arguments are the same primitive types
     */
    public static boolean areSamePrimitiveTypes(TypeMirror left, TypeMirror right) {
        if (!isPrimitive(left) || !isPrimitive(right)) {
            return false;
        }

        return (left.getKind() == right.getKind());
    }

    /**
     * Returns true iff the argument is a primitive numeric type.
     *
     * @return whether the argument is a primitive numeric type
     */
    public static boolean isNumeric(TypeMirror type) {
        switch (type.getKind()) {
            case BYTE:
            case CHAR:
            case DOUBLE:
            case FLOAT:
            case INT:
            case LONG:
            case SHORT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns true iff the argument is an integral type.
     *
     * @return whether the argument is an integral type
     */
    public static boolean isIntegral(TypeMirror type) {
        switch (type.getKind()) {
            case BYTE:
            case CHAR:
            case INT:
            case LONG:
            case SHORT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns true iff the argument is a boxed floating point type.
     *
     * @param type type to test
     * @return whether the argument is a boxed floating point type
     */
    public static boolean isBoxedFloating(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }

        String qualifiedName = getQualifiedName((DeclaredType) type).toString();
        return qualifiedName.equals("java.lang.Double") || qualifiedName.equals("java.lang.Float");
    }

    /**
     * Returns true iff the argument is a floating point type.
     *
     * @param type type mirror
     * @return whether the argument is a floating point type
     */
    public static boolean isFloating(TypeMirror type) {
        switch (type.getKind()) {
            case DOUBLE:
            case FLOAT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns the widened numeric type for an arithmetic operation performed on a value of the left
     * type and the right type. Defined in JLS 5.6.2. We return a {@link TypeKind} because creating
     * a {@link TypeMirror} requires a {@link Types} object from the {@link
     * javax.annotation.processing.ProcessingEnvironment}.
     *
     * @return the result of widening numeric conversion, or NONE when the conversion cannot be
     *     performed
     */
    public static TypeKind widenedNumericType(TypeMirror left, TypeMirror right) {
        if (!isNumeric(left) || !isNumeric(right)) {
            return TypeKind.NONE;
        }

        TypeKind leftKind = left.getKind();
        TypeKind rightKind = right.getKind();

        if (leftKind == TypeKind.DOUBLE || rightKind == TypeKind.DOUBLE) {
            return TypeKind.DOUBLE;
        }

        if (leftKind == TypeKind.FLOAT || rightKind == TypeKind.FLOAT) {
            return TypeKind.FLOAT;
        }

        if (leftKind == TypeKind.LONG || rightKind == TypeKind.LONG) {
            return TypeKind.LONG;
        }

        return TypeKind.INT;
    }

    /**
     * If the argument is a bounded TypeVariable or WildcardType, return its non-variable,
     * non-wildcard upper bound. Otherwise, return the type itself.
     *
     * @param type a type
     * @return the non-variable, non-wildcard upper bound of a type, if it has one, or itself if it
     *     has no bounds
     */
    public static TypeMirror upperBound(TypeMirror type) {
        do {
            if (type instanceof TypeVariable) {
                TypeVariable tvar = (TypeVariable) type;
                if (tvar.getUpperBound() != null) {
                    type = tvar.getUpperBound();
                } else {
                    break;
                }
            } else if (type instanceof WildcardType) {
                WildcardType wc = (WildcardType) type;
                if (wc.getExtendsBound() != null) {
                    type = wc.getExtendsBound();
                } else {
                    break;
                }
            } else {
                break;
            }
        } while (true);
        return type;
    }

    /**
     * Get the type parameter for this wildcard from the underlying type's bound field This field is
     * sometimes null, in that case this method will return null.
     *
     * @return the TypeParameterElement the wildcard is an argument to, {@code null} otherwise
     */
    public static @Nullable TypeParameterElement wildcardToTypeParam(
            final Type.WildcardType wildcard) {

        final Element typeParamElement;
        if (wildcard.bound != null) {
            typeParamElement = wildcard.bound.asElement();
        } else {
            typeParamElement = null;
        }

        return (TypeParameterElement) typeParamElement;
    }

    /**
     * Version of com.sun.tools.javac.code.Types.wildUpperBound(Type) that works with both jdk8
     * (called upperBound there) and jdk8u.
     */
    // TODO: contrast to upperBound.
    public static Type wildUpperBound(TypeMirror tm, ProcessingEnvironment env) {
        Type t = (Type) tm;
        if (t.hasTag(TypeTag.WILDCARD)) {
            Context context = ((JavacProcessingEnvironment) env).getContext();
            Type.WildcardType w = (Type.WildcardType) TypeAnnotationUtils.unannotatedType(t);
            if (w.isSuperBound()) { // returns true if w is unbound
                Symtab syms = Symtab.instance(context);
                // w.bound is null if the wildcard is from bytecode.
                return w.bound == null ? syms.objectType : w.bound.getUpperBound();
            } else {
                return wildUpperBound(w.type, env);
            }
        } else {
            return TypeAnnotationUtils.unannotatedType(t);
        }
    }

    /**
     * Version of com.sun.tools.javac.code.Types.wildLowerBound(Type) that works with both jdk8
     * (called upperBound there) and jdk8u.
     */
    public static Type wildLowerBound(TypeMirror tm, ProcessingEnvironment env) {
        Type t = (Type) tm;
        if (t.hasTag(TypeTag.WILDCARD)) {
            Context context = ((JavacProcessingEnvironment) env).getContext();
            Symtab syms = Symtab.instance(context);
            Type.WildcardType w = (Type.WildcardType) TypeAnnotationUtils.unannotatedType(t);
            return w.isExtendsBound() ? syms.botType : wildLowerBound(w.type, env);
        } else {
            return TypeAnnotationUtils.unannotatedType(t);
        }
    }

    /** Returns the {@link TypeMirror} for a given {@link Class}. */
    public static TypeMirror typeFromClass(Class<?> clazz, Types types, Elements elements) {
        if (clazz == void.class) {
            return types.getNoType(TypeKind.VOID);
        } else if (clazz.isPrimitive()) {
            String primitiveName = clazz.getName().toUpperCase();
            TypeKind primitiveKind = TypeKind.valueOf(primitiveName);
            return types.getPrimitiveType(primitiveKind);
        } else if (clazz.isArray()) {
            TypeMirror componentType = typeFromClass(clazz.getComponentType(), types, elements);
            return types.getArrayType(componentType);
        } else {
            String name = clazz.getCanonicalName();
            assert name != null : "@AssumeAssertion(nullness): assumption";
            TypeElement element = elements.getTypeElement(name);
            if (element == null) {
                throw new BugInCF("Unrecognized class: " + clazz);
            }
            return element.asType();
        }
    }

    /** Returns an {@link ArrayType} with elements of type {@code componentType}. */
    public static ArrayType createArrayType(TypeMirror componentType, Types types) {
        JavacTypes t = (JavacTypes) types;
        return t.getArrayType(componentType);
    }

    /**
     * Returns true if declaredType is a Class that is used to box primitive type (e.g.
     * declaredType=java.lang.Double and primitiveType=22.5d )
     */
    public static boolean isBoxOf(TypeMirror declaredType, TypeMirror primitiveType) {
        if (declaredType.getKind() != TypeKind.DECLARED) {
            return false;
        }

        final String qualifiedName = getQualifiedName((DeclaredType) declaredType).toString();
        switch (primitiveType.getKind()) {
            case BOOLEAN:
                return qualifiedName.equals("java.lang.Boolean");
            case BYTE:
                return qualifiedName.equals("java.lang.Byte");
            case CHAR:
                return qualifiedName.equals("java.lang.Character");
            case DOUBLE:
                return qualifiedName.equals("java.lang.Double");
            case FLOAT:
                return qualifiedName.equals("java.lang.Float");
            case INT:
                return qualifiedName.equals("java.lang.Integer");
            case LONG:
                return qualifiedName.equals("java.lang.Long");
            case SHORT:
                return qualifiedName.equals("java.lang.Short");

            default:
                return false;
        }
    }

    /**
     * Given a bounded type (wildcard or typevar) get the concrete type of its upper bound. If the
     * bounded type extends other bounded types, this method will iterate through their bounds until
     * a class, interface, or intersection is found.
     *
     * @return a type that is not a wildcard or typevar, or {@code null} if this type is an
     *     unbounded wildcard
     */
    public static @Nullable TypeMirror findConcreteUpperBound(final TypeMirror boundedType) {
        TypeMirror effectiveUpper = boundedType;
        outerLoop:
        while (true) {
            switch (effectiveUpper.getKind()) {
                case WILDCARD:
                    effectiveUpper =
                            ((javax.lang.model.type.WildcardType) effectiveUpper).getExtendsBound();
                    if (effectiveUpper == null) {
                        return null;
                    }
                    break;

                case TYPEVAR:
                    effectiveUpper = ((TypeVariable) effectiveUpper).getUpperBound();
                    break;

                default:
                    break outerLoop;
            }
        }
        return effectiveUpper;
    }

    /**
     * Returns true if the erased type of subtype is a subtype of the erased type of supertype.
     *
     * @param subtype possible subtype
     * @param supertype possible supertype
     * @param types a Types object
     * @return true if the erased type of subtype is a subtype of the erased type of supertype
     */
    public static boolean isErasedSubtype(TypeMirror subtype, TypeMirror supertype, Types types) {
        return types.isSubtype(types.erasure(subtype), types.erasure(supertype));
    }

    /** Returns whether a TypeVariable represents a captured type. */
    public static boolean isCaptured(TypeMirror typeVar) {
        if (typeVar.getKind() != TypeKind.TYPEVAR) {
            return false;
        }
        return ((Type.TypeVar) TypeAnnotationUtils.unannotatedType(typeVar)).isCaptured();
    }

    /** If typeVar is a captured wildcard, returns that wildcard; otherwise returns {@code null}. */
    public static @Nullable WildcardType getCapturedWildcard(TypeVariable typeVar) {
        if (isCaptured(typeVar)) {
            return ((CapturedType) TypeAnnotationUtils.unannotatedType(typeVar)).wildcard;
        }
        return null;
    }

    /** Returns whether a TypeMirror represents a class type. */
    public static boolean isClassType(TypeMirror type) {
        return (type instanceof Type.ClassType);
    }

    /**
     * Returns the least upper bound of two {@link TypeMirror}s, ignoring any annotations on the
     * types.
     *
     * <p>Wrapper around Types.lub to add special handling for null types, primitives, and
     * wildcards.
     *
     * @param tm1 a {@link TypeMirror}
     * @param tm2 a {@link TypeMirror}
     * @param processingEnv the {@link ProcessingEnvironment} to use
     * @return the least upper bound of {@code tm1} and {@code tm2}.
     */
    public static TypeMirror leastUpperBound(
            TypeMirror tm1, TypeMirror tm2, ProcessingEnvironment processingEnv) {
        Type t1 = TypeAnnotationUtils.unannotatedType(tm1);
        Type t2 = TypeAnnotationUtils.unannotatedType(tm2);
        // Handle the 'null' type manually (not done by types.lub).
        if (t1.getKind() == TypeKind.NULL) {
            return t2;
        }
        if (t2.getKind() == TypeKind.NULL) {
            return t1;
        }
        if (t1.getKind() == TypeKind.WILDCARD) {
            WildcardType wc1 = (WildcardType) t1;
            t1 = (Type) wc1.getExtendsBound();
            if (t1 == null) {
                // Implicit upper bound of java.lang.Object
                Elements elements = processingEnv.getElementUtils();
                return elements.getTypeElement("java.lang.Object").asType();
            }
        }
        if (t2.getKind() == TypeKind.WILDCARD) {
            WildcardType wc2 = (WildcardType) t2;
            t2 = (Type) wc2.getExtendsBound();
            if (t2 == null) {
                // Implicit upper bound of java.lang.Object
                Elements elements = processingEnv.getElementUtils();
                return elements.getTypeElement("java.lang.Object").asType();
            }
        }
        JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) processingEnv;
        com.sun.tools.javac.code.Types types =
                com.sun.tools.javac.code.Types.instance(javacEnv.getContext());
        if (types.isSameType(t1, t2)) {
            // Special case if the two types are equal.
            return t1;
        }
        // Special case for primitives.
        if (isPrimitive(t1) || isPrimitive(t2)) {
            if (types.isAssignable(t1, t2)) {
                return t2;
            } else if (types.isAssignable(t2, t1)) {
                return t1;
            } else {
                Elements elements = processingEnv.getElementUtils();
                return elements.getTypeElement("java.lang.Object").asType();
            }
        }
        return types.lub(t1, t2);
    }

    /**
     * Returns the greatest lower bound of two {@link TypeMirror}s, ignoring any annotations on the
     * types.
     *
     * <p>Wrapper around Types.glb to add special handling for null types, primitives, and
     * wildcards.
     *
     * @param tm1 a {@link TypeMirror}
     * @param tm2 a {@link TypeMirror}
     * @param processingEnv the {@link ProcessingEnvironment} to use
     * @return the greatest lower bound of {@code tm1} and {@code tm2}.
     */
    public static TypeMirror greatestLowerBound(
            TypeMirror tm1, TypeMirror tm2, ProcessingEnvironment processingEnv) {
        Type t1 = TypeAnnotationUtils.unannotatedType(tm1);
        Type t2 = TypeAnnotationUtils.unannotatedType(tm2);
        JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) processingEnv;
        com.sun.tools.javac.code.Types types =
                com.sun.tools.javac.code.Types.instance(javacEnv.getContext());
        if (types.isSameType(t1, t2)) {
            // Special case if the two types are equal.
            return t1;
        }
        // Handle the 'null' type manually.
        if (t1.getKind() == TypeKind.NULL) {
            return t1;
        }
        if (t2.getKind() == TypeKind.NULL) {
            return t2;
        }
        // Special case for primitives.
        if (isPrimitive(t1) || isPrimitive(t2)) {
            if (types.isAssignable(t1, t2)) {
                return t1;
            } else if (types.isAssignable(t2, t1)) {
                return t2;
            } else {
                // Javac types.glb returns TypeKind.Error when the GLB does
                // not exist, but we can't create one.  Use TypeKind.NONE
                // instead.
                return processingEnv.getTypeUtils().getNoType(TypeKind.NONE);
            }
        }
        if (t1.getKind() == TypeKind.WILDCARD) {
            return t2;
        }
        if (t2.getKind() == TypeKind.WILDCARD) {
            return t1;
        }

        // If neither type is a primitive type, null type, or wildcard
        // and if the types are not the same, use javac types.glb
        return types.glb(t1, t2);
    }

    /** Returns the return type of a method, given the receiver of the method call. */
    public static TypeMirror substituteMethodReturnType(
            Element methodElement, TypeMirror substitutedReceiverType, ProcessingEnvironment env) {

        com.sun.tools.javac.code.Types types =
                com.sun.tools.javac.code.Types.instance(InternalUtils.getJavacContext(env));

        Type substitutedMethodType =
                types.memberType((Type) substitutedReceiverType, (Symbol) methodElement);
        return substitutedMethodType.getReturnType();
    }

    /**
     * Returns the type element for {@code type} if {@code type} is a class, interface, annotation
     * type, or enum. Otherwise, returns null.
     *
     * @param type whose element is returned
     * @return the type element for {@code type} if {@code type} is a class, interface, annotation
     *     type, or enum; otherwise, returns {@code null}
     */
    public static @Nullable TypeElement getTypeElement(TypeMirror type) {
        Element element = ((Type) type).asElement();
        if (element == null) {
            return null;
        }
        if (ElementUtils.isClassElement(element)) {
            return (TypeElement) element;
        }
        return null;
    }

    /**
     * Returns {@code type} as {@code superType} if {@code superType} is a super type of {@code
     * type}; otherwise, null.
     *
     * @return {@code type} as {@code superType} if {@code superType} is a super type of {@code
     *     type}; otherwise, null
     */
    public static TypeMirror asSuper(
            TypeMirror type, TypeMirror superType, ProcessingEnvironment env) {
        Context ctx = ((JavacProcessingEnvironment) env).getContext();
        com.sun.tools.javac.code.Types javacTypes = com.sun.tools.javac.code.Types.instance(ctx);
        return javacTypes.asSuper((Type) type, ((Type) superType).tsym);
    }

    /**
     * Returns whether or not {@code type} is a functional interface type (as defined in JLS 9.8).
     *
     * @param type possible functional interface type
     * @param env ProcessingEnvironment
     * @return whether or not {@code type} is a functional interface type (as defined in JLS 9.8)
     */
    public static boolean isFunctionalInterface(TypeMirror type, ProcessingEnvironment env) {
        Context ctx = ((JavacProcessingEnvironment) env).getContext();
        com.sun.tools.javac.code.Types javacTypes = com.sun.tools.javac.code.Types.instance(ctx);
        return javacTypes.isFunctionalInterface((Type) type);
    }

    /**
     * Returns the simple type name, without annotations.
     *
     * @param type a type
     * @return the simple type name, without annotations
     */
    public static String simpleTypeName(TypeMirror type) {
        switch (type.getKind()) {
            case ARRAY:
                return simpleTypeName(((ArrayType) type).getComponentType()) + "[]";
            case TYPEVAR:
                return ((TypeVariable) type).asElement().getSimpleName().toString();
            case DECLARED:
                return ((DeclaredType) type).asElement().getSimpleName().toString();
            case NULL:
                return "<nulltype>";
            case VOID:
                return "void";
            default:
                if (type.getKind().isPrimitive()) {
                    return TypeAnnotationUtils.unannotatedType(type).toString();
                } else {
                    throw new BugInCF(
                            "simpleTypeName: unhandled type kind: %s, type: %s",
                            type.getKind(), type);
                }
        }
    }
}
