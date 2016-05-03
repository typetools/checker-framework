package org.checkerframework.framework.util;

/*>>>
import org.checkerframework.checker.nullness.qual.*;
*/

import org.checkerframework.framework.flow.util.LubTypeVariableAnnotator;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.SyntheticArrays;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeVisitor;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TypesUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.WildcardType;

/**
 * Utility methods for operating on {@code AnnotatedTypeMirror}. This
 * class mimics the class {@link Types}.
 */
public class AnnotatedTypes {
    // Class cannot be instantiated.
    private AnnotatedTypes() { throw new AssertionError("Class AnnotatedTypes cannot be instantiated.");}

    /**
     * Returns the most specific base type of {@code t} whose erasure type
     * is {@code superType}.  It returns null if {@code t} is not a subtype
     * of {@code superType}.
     *
     * @param types the type utilities to use
     * @param atypeFactory the type factory to use
     * @param t      a type
     * @param superType   a type that is a supertype of {@code t}
     * @return the base type of t of the given element
     */
    public static AnnotatedTypeMirror asSuper(Types types, AnnotatedTypeFactory atypeFactory,
                                              AnnotatedTypeMirror t,
                                              AnnotatedTypeMirror superType) {
        if (asSuper == null ||
                asSuper.types != types ||
                asSuper.atypeFactory != atypeFactory) {
            asSuper = new AsSuperTypeVisitor(types, atypeFactory);
        }
        AnnotatedTypeMirror result = asSuper.visit(t, superType);
        return result;
    }

    private static AsSuperTypeVisitor asSuper;

    private static class AsSuperTypeVisitor extends SimpleAnnotatedTypeVisitor<AnnotatedTypeMirror, AnnotatedTypeMirror> {
        private final Types types;
        private final AnnotatedTypeFactory atypeFactory;

        AsSuperTypeVisitor(Types types, AnnotatedTypeFactory atypeFactory) {
            this.types = types;
            this.atypeFactory = atypeFactory;
        }

        @Override
        protected AnnotatedTypeMirror defaultAction(AnnotatedTypeMirror type, AnnotatedTypeMirror p) {
            return type;
        }

        @Override
        public AnnotatedTypeMirror visitPrimitive(AnnotatedPrimitiveType type, AnnotatedTypeMirror p) {
            if (!p.getKind().isPrimitive()) {
                return visit(atypeFactory.getBoxedType(type), p);
            }

            AnnotatedPrimitiveType pt = (AnnotatedPrimitiveType)p;
            AnnotatedPrimitiveType st = pt.shallowCopy(false);
            st.addAnnotations(type.getAnnotations());
            return st;
        }

        @Override
        public AnnotatedTypeMirror visitTypeVariable(AnnotatedTypeVariable type, AnnotatedTypeMirror p) {
            if (p.getKind() == TypeKind.TYPEVAR) {
                return type;
            }
            // Operate on the effective upper bound
            AnnotatedTypeMirror res = asSuper(types, atypeFactory, type.getUpperBound(), p);
            if (res != null) {
                res.addMissingAnnotations(atypeFactory.getQualifierHierarchy().getTopAnnotations());
                // TODO: or should it be the default?
                // Test MultiBoundTypeVar fails otherwise.
                // Is there a better place for this?
            }
            return res;
        }

        @Override
        public AnnotatedTypeMirror visitWildcard(AnnotatedWildcardType type, AnnotatedTypeMirror p) {
            if (p.getKind() == TypeKind.WILDCARD) {
                return type;
            }
            return asSuper(types, atypeFactory, type.getExtendsBound(), p);
        }


        @Override
        public AnnotatedTypeMirror visitArray(AnnotatedArrayType type, AnnotatedTypeMirror p) {
            // Check if array component is subtype of the element
            // first
            if (shouldStop(p, type)) {
                return type;
            }
            for (AnnotatedTypeMirror st : type.directSuperTypes()) {
                AnnotatedTypeMirror x = asSuper(types, atypeFactory, st, p);
                if (x != null) {
                    return isErased(types, x, p) ? x.getErased() : x;
                }
            }
            return null;
        }

        @Override
        public AnnotatedTypeMirror visitDeclared(AnnotatedDeclaredType type, AnnotatedTypeMirror p) {
            // If visited Element is the desired one, we are done
            if (p.getKind().isPrimitive()) {
                if (TypesUtils.isBoxedPrimitive(type.getUnderlyingType())) {
                    return visit(atypeFactory.getUnboxedType(type), p);
                } else {
                    // TODO: is there something better we could do?
                    // See tests/framework/Unboxing.java
                    return null;
                }
            }

            if (shouldStop(p, type)) {
                return type;
            }

            // Visit the superclass first!
            for (AnnotatedDeclaredType st : type.directSuperTypes()) {
                if (st.getKind() == TypeKind.DECLARED) {
                    AnnotatedDeclaredType x = (AnnotatedDeclaredType) asSuper(types, atypeFactory, st, p);
                    if (x != null) {
                        return x;
                    }
                }
            }

            if (p.getKind() == TypeKind.TYPEVAR) {
                return asSuper(types, atypeFactory, type, ((AnnotatedTypeVariable)p).getUpperBound());
            }
            if (p.getKind() == TypeKind.WILDCARD) {
                return asSuper(types, atypeFactory, type, ((AnnotatedWildcardType)p).getExtendsBound().deepCopy());
            }
            return null;
        }

        @Override
        public AnnotatedTypeMirror visitIntersection(AnnotatedIntersectionType type, AnnotatedTypeMirror p) {
            if (shouldStop(p, type)) {
                return type;
            }

            for (AnnotatedDeclaredType st : type.directSuperTypes()) {
                AnnotatedDeclaredType x = (AnnotatedDeclaredType) asSuper(types, atypeFactory, st, p);
                if (x != null) {
                    return x;
                }
            }

            return null;
        }
    };

    /**
     * This method identifies wildcard types that are unbound.
     */
    public static boolean hasNoExplicitBound(final AnnotatedTypeMirror wildcard) {
        return ((Type.WildcardType) wildcard.getUnderlyingType()).isUnbound();
    }

    /**
     * This method identifies wildcard types that have an explicit super bound.
     * NOTE: Type.WildcardType.isSuperBound will return true for BOTH unbound and super bound wildcards
     * which necessitates this method
     */
    public static boolean hasExplicitSuperBound(final AnnotatedTypeMirror wildcard) {
        final Type.WildcardType wildcardType = (Type.WildcardType) wildcard.getUnderlyingType();
        return wildcardType.isSuperBound() && !((WildcardType) wildcard.getUnderlyingType()).isUnbound();
    }

    /**
     * This method identifies wildcard types that have an explicit extends bound.
     * NOTE: Type.WildcardType.isExtendsBound will return true for BOTH unbound and extends bound wildcards
     * which necessitates this method
     */
    public static boolean hasExplicitExtendsBound(final AnnotatedTypeMirror wildcard) {
        final Type.WildcardType wildcardType = (Type.WildcardType) wildcard.getUnderlyingType();
        return wildcardType.isExtendsBound() && !((WildcardType) wildcard.getUnderlyingType()).isUnbound();
    }


    /**
     * Return the base type of t or any of its outer types that starts
     * with the given type. If none exists, return null.
     *
     * @param t     a type
     * @param elem   a type
     */
    private static AnnotatedTypeMirror asOuterSuper(Types types, AnnotatedTypeFactory atypeFactory, AnnotatedTypeMirror t,
                                                    AnnotatedTypeMirror elem) {
        switch (t.getKind()) {
            case DECLARED:
                AnnotatedDeclaredType dt = (AnnotatedDeclaredType) t;
                do {
                    // Search among supers for a desired supertype
                    AnnotatedTypeMirror s = asSuper(types, atypeFactory, dt, elem);
                    if (s != null) {
                        return s;
                    }
                    // if not found immediately, try enclosing type
                    // like A in A.B
                    dt = dt.getEnclosingType();
                } while (dt != null);
                return null;
            case ARRAY:     // intentional follow-through
            case TYPEVAR:   // intentional follow-through
            case WILDCARD:
                return asSuper(types, atypeFactory, t, elem);
            default:
                return null;
        }
    }

    /*
     * Returns true if sup and sub are the same type.
     * Returns false otherwise (including if sub cannot be a subtype of sup).
     */
    private static boolean shouldStop(AnnotatedTypeMirror sup, AnnotatedTypeMirror sub) {
        // Check if it's the same type
        // if sup is primitive, but not sub
        if (sup.getKind().isPrimitive() && !sub.getKind().isPrimitive()) {
            /// XXX shouldn't this be "return false"?
            return true;
        }
        if (sup.getKind().isPrimitive() && sub.getKind().isPrimitive()) {
            return sup.getKind() == sub.getKind();
        }
        // if both are declared
        if (sup.getKind() == TypeKind.DECLARED && sub.getKind() == TypeKind.DECLARED) {
            AnnotatedDeclaredType supdt = (AnnotatedDeclaredType) sup;
            AnnotatedDeclaredType subdt = (AnnotatedDeclaredType) sub;

            // Check if it's the same name
            if (!supdt.getUnderlyingType().asElement().equals(
                    subdt.getUnderlyingType().asElement()))
                return false;

            return true;
        }

        if (sup.getKind() == TypeKind.ARRAY && sub.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType supat = (AnnotatedArrayType) sup;
            AnnotatedArrayType subat = (AnnotatedArrayType) sub;
            return shouldStop(supat.getComponentType(), subat.getComponentType());
        }
        // horrible horrible hack
        // Types.isSameType() doesn't work for type variables or wildcards
        return sup.getUnderlyingType().toString().equals(sub.getUnderlyingType().toString());
    }

    /**
     * Tests that t2 is the erased type of t2
     *
     * @return true iff t2 is erased type of t1
     */
    private static boolean isErased(Types types, AnnotatedTypeMirror t1, AnnotatedTypeMirror t2) {
        return types.isSameType(
                types.erasure(t1.getUnderlyingType()), t2.getUnderlyingType());
    }

    /**
     * @see #asMemberOf(Types, AnnotatedTypeFactory, AnnotatedTypeMirror, Element)
     */
    public static AnnotatedExecutableType asMemberOf(Types types, AnnotatedTypeFactory atypeFactory, AnnotatedTypeMirror t,
                                                     ExecutableElement elem) {
        return (AnnotatedExecutableType) asMemberOf(types, atypeFactory, t, (Element) elem);
    }

    /**
     * Returns the type of an element when that element is viewed as a member
     * of, or otherwise directly contained by, a given type.
     *
     * For example, when viewed as a member of the parameterized type
     * {@code Set<@NonNull String>}, the {@code Set.add} method is an
     * {@code ExecutableType} whose parameter is of type
     * {@code @NonNull String}.
     *
     * The result is customized according to the type system semantics,
     * according to {@link AnnotatedTypeFactory#postAsMemberOf(
     * AnnotatedTypeMirror, AnnotatedTypeMirror, Element)}.
     *
     * Note that this method does not currently return (top level) captured types
     * for type parameters, parameters, and return types. Instead, the original
     * wildcard is returned, or sometimes inferring type arguments will create
     * a wildcard type which is returned. The bounds of an inferred wildcard
     * may itself have captures.
     *
     * To prevent unsoundness, the rest of the checker framework must expect
     * wildcard in places where captures should appear (like type arguments).
     * This should just involve the bounds of the wildcard where the bounds
     * of the capture would have been used.
     *
     * @param t    a type
     * @param elem  an element
     */
    public static AnnotatedTypeMirror asMemberOf(Types types, AnnotatedTypeFactory atypeFactory,
                                                 AnnotatedTypeMirror t, Element elem) {
        // asMemberOf is only for fields, variables, and methods!
        // Otherwise, simply use fromElement.
        switch (elem.getKind()) {
            case PACKAGE:
            case INSTANCE_INIT:
            case OTHER:
            case STATIC_INIT:
            case TYPE_PARAMETER:
                return atypeFactory.fromElement(elem);
            default:
                AnnotatedTypeMirror type = asMemberOfImpl(types, atypeFactory, t, elem);
                if (!ElementUtils.isStatic(elem)) {
                    atypeFactory.postAsMemberOf(type, t, elem);
                }
                return type;
        }
    }

    private static AnnotatedTypeMirror asMemberOfImpl(final Types types, final AnnotatedTypeFactory atypeFactory,
                                                      final AnnotatedTypeMirror t, final Element elem) {
        if (ElementUtils.isStatic(elem)) {
            return atypeFactory.getAnnotatedType(elem);
        }

        // For type variables and wildcards, operate on the upper bound
        if (t.getKind() == TypeKind.TYPEVAR) {
            return asMemberOf(types, atypeFactory, ((AnnotatedTypeVariable) t).getUpperBound(), elem);
        }
        if (t.getKind() == TypeKind.WILDCARD) {
            return asMemberOf(types, atypeFactory, ((AnnotatedWildcardType) t).getExtendsBound().deepCopy(), elem);
        }

        //Method references like String[]::clone should have a return type of String[] rather than Object
        if (SyntheticArrays.isArrayClone(t, elem)) {
            return SyntheticArrays.replaceReturnType(elem, (AnnotatedArrayType) t);
        }

        final AnnotatedTypeMirror elemType = atypeFactory.getAnnotatedType(elem);

        // t.getKind() may be a TypeKind.ARRAY for Array.length calls.
        // We don't _think_ there are any other cases where t.getKind() != TypeKind.DECLARED
        if (t.getKind() != TypeKind.DECLARED) {
            return elemType;
        }

        // Basic Algorithm:
        // 1. Find the owner of the element
        // 2. Find the base type of owner (e.g. type of owner as supertype
        //      of passed type)
        // 3. Substitute for type variables if any exist
        TypeElement owner = ElementUtils.enclosingClass(elem);
        // Is the owner or any enclosing class generic?
        boolean ownerGeneric = false;
        {
            TypeElement encl = owner;
            while (encl != null) {
                if (!encl.getTypeParameters().isEmpty()) {
                    ownerGeneric = true;
                    break;
                }
                encl = ElementUtils.enclosingClass(encl.getEnclosingElement());
            }
        }

        // TODO: Potential bug if Raw type is used
        if (!ownerGeneric) {
            return elemType;
        }

        AnnotatedDeclaredType ownerType = atypeFactory.getAnnotatedType(owner);
        AnnotatedDeclaredType base =
                (AnnotatedDeclaredType) asOuterSuper(types, atypeFactory, t, ownerType);

        if (base == null) {
            return elemType;
        }

        final List<AnnotatedTypeVariable> ownerParams = new ArrayList<>(ownerType.getTypeArguments().size());
        for (final AnnotatedTypeMirror typeParam : ownerType.getTypeArguments()) {
            if (typeParam.getKind() != TypeKind.TYPEVAR) {
                ErrorReporter.errorAbort("Type arguments of a declaration should be type variables\n"
                                       + "owner=" + owner + "\n"
                                       + "ownerType=" + ownerType + "\n"
                                       + "typeMirror=" + t + "\n"
                                       + "element=" + elem);
            }
            ownerParams.add((AnnotatedTypeVariable) typeParam);
        }

        final List<? extends AnnotatedTypeMirror> baseParams = base.getTypeArguments();
        if (!ownerParams.isEmpty()) {
            if (baseParams.isEmpty()) {
                List<AnnotatedTypeMirror> baseParamsEr = new ArrayList<>();
                for (AnnotatedTypeMirror arg : ownerParams) {
                    baseParamsEr.add(arg.getErased());
                }
                return subst(atypeFactory, elemType, ownerParams, baseParamsEr);
            }
            return subst(atypeFactory, elemType, ownerParams, baseParams);
        }

        return elemType;
    }

    /**
     * Returns a new type, a copy of the passed {@code t}, with all
     * instances of {@code from} type substituted with their correspondents
     * in {@code to}.
     *
     * @param t     the type
     * @param from  the from types
     * @param to    the to types
     * @return  the new type after substitutions
     */
    private static AnnotatedTypeMirror subst(AnnotatedTypeFactory atypeFactory,
                                             AnnotatedTypeMirror t,
                                             List<? extends AnnotatedTypeVariable> from,
                                             List<? extends AnnotatedTypeMirror> to) {
        final Map<TypeVariable, AnnotatedTypeMirror> mappings = new HashMap<>();

        for (int i = 0; i < from.size(); ++i) {
            mappings.put(from.get(i).getUnderlyingType(), to.get(i));
        }
        return atypeFactory.getTypeVarSubstitutor().substitute(mappings, t);
    }

    /**
     * Returns the iterated type of the passed iterable type, and throws
     * {@link IllegalArgumentException} if the passed type is not iterable.
     *
     * The iterated type is the component type of an array, and the type
     * argument of {@link Iterable} for declared types.
     *
     * @param iterableType  the iterable type (either array or declared)
     * @return the types of elements in the iterable type
     */
    public static AnnotatedTypeMirror getIteratedType(ProcessingEnvironment processingEnv,
                                                      AnnotatedTypeFactory atypeFactory,
                                                      AnnotatedTypeMirror iterableType) {
        if (iterableType.getKind() == TypeKind.ARRAY) {
            return ((AnnotatedArrayType) iterableType).getComponentType();
        }

        // For type variables and wildcards take the effective upper bound.
        if (iterableType.getKind() == TypeKind.WILDCARD) {
            return getIteratedType(processingEnv, atypeFactory,
                    ((AnnotatedWildcardType) iterableType).getExtendsBound().deepCopy());
        }
        if (iterableType.getKind() == TypeKind.TYPEVAR) {
            return getIteratedType(processingEnv, atypeFactory,
                    ((AnnotatedTypeVariable) iterableType).getUpperBound());
        }

        if (iterableType.getKind() != TypeKind.DECLARED) {
            ErrorReporter.errorAbort("AnnotatedTypes.getIteratedType: not iterable type: " + iterableType);
            return null; // dead code
        }

        TypeElement iterableElement = processingEnv.getElementUtils().getTypeElement("java.lang.Iterable");
        AnnotatedDeclaredType iterableElmType = atypeFactory.getAnnotatedType(iterableElement);
        AnnotatedDeclaredType dt = (AnnotatedDeclaredType) asSuper(processingEnv.getTypeUtils(), atypeFactory, iterableType, iterableElmType);
        if (dt == null) {
            ErrorReporter.errorAbort("AnnotatedTypes.getIteratedType: not an iterable type: " + iterableType);
            return null; // dead code
        } else if (dt.getTypeArguments().isEmpty()) {
            TypeElement e = processingEnv.getElementUtils().getTypeElement("java.lang.Object");
            AnnotatedDeclaredType t = atypeFactory.getAnnotatedType(e);
            return t;
        } else {
            return dt.getTypeArguments().get(0);
        }
    }

    /**
     * Returns all the super types of the given declared type.
     *
     * @param type a declared type
     * @return  all the supertypes of the given type
     */
    public static Set<AnnotatedDeclaredType> getSuperTypes(AnnotatedDeclaredType type) {

        Set<AnnotatedDeclaredType> supertypes = new LinkedHashSet<>();
        if (type == null) {
            return supertypes;
        }

        // Set up a stack containing the type mirror of subtype, which
        // is our starting point.
        Deque<AnnotatedDeclaredType> stack = new ArrayDeque<>();
        stack.push(type);

        while (!stack.isEmpty()) {
            AnnotatedDeclaredType current = stack.pop();

            // For each direct supertype of the current type, if it
            // hasn't already been visited, push it onto the stack and
            // add it to our supertypes set.
            for (AnnotatedDeclaredType supertype : current.directSuperTypes()) {
                if (!supertypes.contains(supertype)) {
                    stack.push(supertype);
                    supertypes.add(supertype);
                }
            }
        }

        return Collections.<AnnotatedDeclaredType>unmodifiableSet(supertypes);
    }

    /**
     * A utility method that takes a Method element and returns a set
     * of all elements that this method overrides (as
     * {@link ExecutableElement}s)
     *
     * @param method
     *            the overriding method
     * @return an unmodifiable set of {@link ExecutableElement}s
     *         representing the elements that method overrides
     */
    public static Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods(
            Elements elements,
            AnnotatedTypeFactory atypeFactory,
            ExecutableElement method) {
        final TypeElement elem = (TypeElement) method.getEnclosingElement();
        final AnnotatedDeclaredType type = atypeFactory.getAnnotatedType(elem);
        final Collection<AnnotatedDeclaredType> supertypes = getSuperTypes(type);
        return overriddenMethods(elements, method, supertypes);
    }

    /**
     * A utility method that takes the element for a method and the
     * set of all supertypes of the method's containing class and
     * returns the set of all elements that method overrides (as
     * {@link ExecutableElement}s).
     *
     * @param method
     *            the overriding method
     * @param supertypes
     *            the set of supertypes to check for methods that are
     *            overridden by {@code method}
     * @return an unmodified set of {@link ExecutableElement}s
     *         representing the elements that {@code method} overrides
     *         among {@code supertypes}
     */
    public static Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods(
            Elements elements,
            ExecutableElement method, Collection<AnnotatedDeclaredType> supertypes) {

        Map<AnnotatedDeclaredType, ExecutableElement> overrides = new LinkedHashMap<>();

        for (AnnotatedDeclaredType supertype : supertypes) {
            /*@Nullable*/ TypeElement superElement =
                    (TypeElement) supertype.getUnderlyingType().asElement();
            assert superElement != null; /*nninvariant*/
            // For all method in the supertype, add it to the set if
            // it overrides the given method.
            for (ExecutableElement supermethod : ElementFilter.methodsIn(superElement.getEnclosedElements())) {
                if (elements.overrides(method, supermethod,
                        superElement)) {
                    overrides.put(supertype, supermethod);
                    break;
                }
            }
        }

        return Collections.</*@NonNull*/ AnnotatedDeclaredType,
            /*@NonNull*/ ExecutableElement>unmodifiableMap(overrides);
    }

    /**
     * Given a method or constructor invocation, return a mapping
     * of the type variables to their type arguments, if any exist.
     *
     * It uses the method or constructor invocation type arguments if they
     * were specified and otherwise it infers them based on the passed arguments
     * or the return type context, according to JLS 15.12.2.
     *
     * @param atypeFactory the annotated type factory
     * @param expr the method or constructor invocation tree; the passed argument
     *   has to be a subtype of MethodInvocationTree or NewClassTree.
     * @param elt the element corresponding to the tree.
     * @param preType the (partially annotated) type corresponding to the tree -
     *   the result of AnnotatedTypes.asMemberOf with the receiver and elt.
     *
     * @return the mapping of the type variables to type arguments for
     *   this method or constructor invocation.
     */
    public static Map<TypeVariable, AnnotatedTypeMirror>
    findTypeArguments(final ProcessingEnvironment processingEnv,
                      final AnnotatedTypeFactory atypeFactory,
                      final ExpressionTree expr,
                      final ExecutableElement elt,
                      final AnnotatedExecutableType preType) {
        //TODO: TEMPORARY KLUDGE
        atypeFactory.getTypeArgumentInference().adaptMethodType(atypeFactory, expr, preType);

        // Is the method a generic method?
        if (elt.getTypeParameters().isEmpty()) {
            return Collections.emptyMap();
        }

        List<? extends Tree> targs;
        if (expr instanceof MethodInvocationTree) {
            targs = ((MethodInvocationTree) expr).getTypeArguments();
        } else if (expr instanceof NewClassTree) {
            targs = ((NewClassTree) expr).getTypeArguments();
        } else if (expr instanceof MemberReferenceTree) {
            targs = ((MemberReferenceTree) expr).getTypeArguments();
            if (targs == null) {
                return new HashMap<>();
            }
        } else {
            // This case should never happen.
            ErrorReporter.errorAbort("AnnotatedTypes.findTypeArguments: unexpected tree: " + expr);
            return null; // dead code
        }

        // Has the user supplied type arguments?
        if (!targs.isEmpty()) {
            List<? extends AnnotatedTypeVariable> tvars = preType.getTypeVariables();

            Map<TypeVariable, AnnotatedTypeMirror> typeArguments = new HashMap<>();
            for (int i = 0; i < elt.getTypeParameters().size(); ++i) {
                AnnotatedTypeVariable typeVar = tvars.get(i);
                AnnotatedTypeMirror typeArg = atypeFactory.getAnnotatedTypeFromTypeTree(targs.get(i));
                // TODO: the call to getTypeParameterDeclaration shouldn't be necessary - typeVar already
                // should be a declaration.
                typeArguments.put(typeVar.getUnderlyingType(), typeArg);
            }
            return typeArguments;
        } else {
            return atypeFactory.getTypeArgumentInference().inferTypeArgs(atypeFactory, expr, elt, preType);
        }
    }

    private static Set<TypeMirror> wildcards = Collections.newSetFromMap(new IdentityHashMap<TypeMirror, Boolean>());
    // TODO: compare to leastUpperBound method that is in comments further
    // below and see how to incorporate the logic.
    // Also see CFAbstractValue for other methods that should be in
    // a better location.
    public static AnnotatedTypeMirror leastUpperBound(ProcessingEnvironment processingEnv, AnnotatedTypeFactory atypeFactory,
                                                      AnnotatedTypeMirror a, AnnotatedTypeMirror b) {
        List<AnnotatedTypeMirror> list = new ArrayList<>(2);
        list.add(a);
        list.add(b);
        // It would be nice to use the following
        // return leastUpperBound(processingEnv, atypeFactory, list);
        // see commented-out version below.
        TypeMirror lubType = InternalUtils.leastUpperBound(processingEnv, a.getUnderlyingType(), b.getUnderlyingType());
        AnnotatedTypeMirror res = AnnotatedTypeMirror.createType(lubType, atypeFactory, false);

        wildcards.clear();
        annotateAsLub(processingEnv, atypeFactory, res, list);
        wildcards.clear();
        return res;
    }

    /* TODO: Add version that takes arbitrary number of arguments.
     * At the moment, InternalUtils.leastUpperBound only exists for two arguments.
    public static AnnotatedTypeMirror leastUpperBound(ProcessingEnvironment processingEnv, AnnotatedTypeFactory atypeFactory,
            Collection<AnnotatedTypeMirror> types) {
        com.sun.tools.javac.util.List<Type> utypes = com.sun.tools.javac.util.List.nil();
        for (AnnotatedTypeMirror atm : types) {
            utypes = utypes.append((Type) atm.getUnderlyingType());
        }
        JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) processingEnv;
        com.sun.tools.javac.code.Types jctypes = com.sun.tools.javac.code.Types.instance(javacEnv.getContext());
        // jctypes.lub doesn't do the special handling of NULL that we have in InternalUtils.leastUpperBound
        // Add that tu InternalUtils and use it here. Using the jctypes version crashes.
        TypeMirror lubType = jctypes.lub(utypes);
        AnnotatedTypeMirror res = AnnotatedTypeMirror.createType(lubType, atypeFactory, false);
        annotateAsLub(processingEnv, atypeFactory, res, types);
        return res;
    }
    */

    /**
     * Annotate the lub type as if it is the least upper bound of the rest of
     * the types.  This is a useful method for finding conditional expression
     * types.
     *
     * All the types need to be subtypes of lub.
     *
     * @param lub   the type to be the least upper bound
     * @param types the type arguments
     */
    public static void annotateAsLub(ProcessingEnvironment processingEnv, AnnotatedTypeFactory atypeFactory,
                                     AnnotatedTypeMirror lub, Collection<AnnotatedTypeMirror> types) {
        Types typeutils = processingEnv.getTypeUtils();
        Elements elements = processingEnv.getElementUtils();

        // Is it anonymous?
        if (lub.getKind() == TypeKind.INTERSECTION) {
            // Find the intersect types
            AnnotatedIntersectionType adt = (AnnotatedIntersectionType) lub;

            for (AnnotatedDeclaredType adts : adt.directSuperTypes()) {
                ArrayList<AnnotatedTypeMirror> subtypes = new ArrayList<>(types.size());
                for (AnnotatedTypeMirror type : types) {
                    AnnotatedTypeMirror sup = asSuper(typeutils, atypeFactory, type, adts);
                    if (sup != null) {
                        subtypes.add(sup);
                    }
                }
                if (subtypes.size() > 0) {
                    adts.clearAnnotations();
                }

                addAnnotations(elements, atypeFactory, adts, subtypes);
                ArrayList<AnnotatedTypeMirror> adtslist = new ArrayList<AnnotatedTypeMirror>();
                adtslist.add(adts);
                addAnnotations(elements, atypeFactory, lub, adtslist);
            }
        } else {
            ArrayList<AnnotatedTypeMirror> subtypes = new ArrayList<>(types.size());

            // TODO: This code needs some more serious thought.
            if (lub.getKind() == TypeKind.WILDCARD) {
                subtypes.add(lub.deepCopy());
            } else {
                for (AnnotatedTypeMirror type : types) {
                    if (type == null) {
                        continue;
                    }
                    AnnotatedTypeMirror ass = asSuper(typeutils, atypeFactory, type, lub);
                    if (ass == null) {
                        subtypes.add(type.deepCopy());
                    } else {
                        subtypes.add(ass);
                    }
                }
            }
            if (subtypes.size() > 0) {
                if (!findEffectiveAnnotations(atypeFactory.getQualifierHierarchy(), lub).isEmpty()) {
                    //I believe the only place this can happen is within recursive types and
                    //if we already have annotations than the type has been visited
                    return;
                }
            }

            if (lub.getKind() == TypeKind.TYPEVAR) {
                //TODO: TERRIBLE HACK UNTIL WE FIX LUB
                final AnnotatedTypeVariable lubAtv = (AnnotatedTypeVariable) lub;
                final List<AnnotatedTypeVariable> subtypesAsTvs =
                    LubTypeVariableAnnotator.getSubtypesAsTypevars(lubAtv, subtypes);

                if (subtypesAsTvs != null) {
                    LubTypeVariableAnnotator.annotateTypeVarAsLub(lubAtv, subtypesAsTvs, atypeFactory);
                } else {
                    addAnnotations(elements, atypeFactory, lub, subtypes);
                }

            } else {
                addAnnotations(elements, atypeFactory, lub, subtypes);
            }
        }
    }

    /**
     * Add the 'intersection' of the types provided to alub.  This is a similar
     * method to the one provided
     * TODO: the above sentence should be finished somehow...
     */
    private static void addAnnotations(Elements elements, AnnotatedTypeFactory atypeFactory,
                                       AnnotatedTypeMirror alub,
                                       ArrayList<AnnotatedTypeMirror> types) {
        Set<AnnotatedTypeMirror> visited = Collections.newSetFromMap(new IdentityHashMap<AnnotatedTypeMirror,Boolean>());
        addAnnotationsImpl(elements, atypeFactory, alub, visited, types);
    }

    private static void addAnnotationsImpl(Elements elements, AnnotatedTypeFactory atypeFactory,
                                           AnnotatedTypeMirror alub,
                                           Set<AnnotatedTypeMirror> visited,
                                           ArrayList<AnnotatedTypeMirror> types) {
        // System.out.println("AnnotatedTypes.addAnnotationsImpl: alub: " + alub +
        //        "\n   visited: " + visited +
        //        "\n   types: " + types);

        final AnnotatedTypeMirror origalub = alub;
        boolean shouldAnnoOrig = false;
        Set<AnnotationMirror> putOnOrig = AnnotationUtils.createAnnotationSet();

        // get rid of wildcards and type variables
        if (alub.getKind() == TypeKind.WILDCARD) {
            final QualifierHierarchy qualifierHierarchy = atypeFactory.getQualifierHierarchy();
            //TODO: TYPEVAR handling of LUB doesn't make much sense, as a stop gap for unannotated
            //TODO: LOWER/UPPER bounds we'll glb the annotations and add them to the lower bound
            //TODO: this will not handle component annotations if we have compound types as the
            //TODO: lower bound (which for type variables would only happen on a capture)
            Set<? extends AnnotationMirror> lowerBounds = new HashSet<>(qualifierHierarchy.getTopAnnotations().size());
            for (AnnotatedTypeMirror type : types) {

                final Set<? extends AnnotationMirror>  annos = findEffectiveLowerBoundAnnotations(qualifierHierarchy, type);
                if (lowerBounds.isEmpty()) {
                    lowerBounds = annos;
                } else if (!annos.isEmpty()) { //for some reason this algorithm some times adds the lub itself
                                               //into the list of type which may lead to an empty annos set
                    lowerBounds = qualifierHierarchy.greatestLowerBounds(lowerBounds, annos);
                }
            }

            ((AnnotatedWildcardType)alub).getSuperBound().replaceAnnotations(lowerBounds);


            //TODO: AGAIN, ALL LUB CODE SHOULD BE EXTRACTED OUT TO IT'S OWN CLASS OR THE TYPE HIERARCHY
            //TODO: AND REWRITTEN
            boolean allWildcards = true;
            for (int i = 0; i < types.size() && allWildcards; i++) {
                if (types.get(i).getKind() != TypeKind.WILDCARD) {
                    allWildcards = false;
                }
            }

            if (allWildcards) {
                if (wildcards.contains(alub.getUnderlyingType())) {
                    return;
                }
                wildcards.add(alub.getUnderlyingType());
                final List<AnnotatedTypeMirror> upperBounds = new ArrayList<>(types.size());
                for (final AnnotatedTypeMirror type : types) {
                    upperBounds.add(((AnnotatedWildcardType)type).getExtendsBound());
                }

                alub = ((AnnotatedWildcardType) alub).getExtendsBound();

                annotateAsLub(atypeFactory.getProcessingEnv(), atypeFactory, alub, upperBounds);
                return;

            } else {
                //old behavior
                alub = ((AnnotatedWildcardType) alub).getExtendsBound();
            }


            // TODO using the getEffective versions copies objects, losing side-effects.
        }
        while (alub.getKind() == TypeKind.TYPEVAR) {
            //TODO: TYPEVAR handling of LUB doesn't make much sense, as a stop gap for unannotated
            //TODO: LOWER/UPPER bounds we'll glb the annotations and add them to the lower bound
            //TODO: this will not handle component annotations if we have compound types as the
            //TODO: lower bound (which for type variables would only happen on a capture)
            Set<? extends AnnotationMirror> glb = glbAll(atypeFactory.getQualifierHierarchy(), types);
            ((AnnotatedTypeVariable)alub).getLowerBound().replaceAnnotations(glb);
            alub = ((AnnotatedTypeVariable)alub).getUpperBound();
        }

        if (visited.contains(alub)) {
            return;
        }
        visited.add(alub);

        for (int i = 0; i < types.size(); ++i) {
            final AnnotatedTypeMirror typei = types.get(i);

            if (!(typei.getAnnotations().isEmpty() ||
                    bottomsOnly(elements, atypeFactory, typei.getAnnotations()))) {
                shouldAnnoOrig = true;
            }

            if (typei.getKind() == TypeKind.WILDCARD) {
                putOnOrig.addAll(typei.getAnnotations());
                AnnotatedWildcardType wildcard = (AnnotatedWildcardType) typei;
                if (wildcard.getExtendsBound() != null) {
                    types.set(i, wildcard.getExtendsBound().deepCopy());
                } else if (wildcard.getSuperBound() != null) {
                    types.set(i, wildcard.getSuperBound().deepCopy());
                }
            }
            if (typei.getKind() == TypeKind.TYPEVAR) {
                putOnOrig.addAll(typei.getAnnotations());
                AnnotatedTypeVariable typevar = (AnnotatedTypeVariable) types.get(i);
                if (typevar.getUpperBound() != null) {
                    types.set(i, typevar.getUpperBound());
                } else if (typevar.getLowerBound() != null) {
                    types.set(i, typevar.getLowerBound());
                }
            }
        }

        Collection<? extends AnnotationMirror> unification = Collections.emptySet();

        boolean isFirst = true;
        for (AnnotatedTypeMirror type : types) {
            if (type.getAnnotations().isEmpty()) {
                continue;
            }
            // TODO: unification fails with an empty set of annotations.
            // Why are they sometimes empty, e.g. in the FlowNegation test case.

            if (isFirst) {
                unification = type.getAnnotations();
            } else {
                unification = atypeFactory.getQualifierHierarchy().leastUpperBounds(unification, type.getAnnotations());
            }
            isFirst = false;
        }

        // Remove a previously existing unqualified annotation on the type.
        alub.replaceAnnotations(unification);

        if (alub.getKind() == TypeKind.DECLARED) {
            AnnotatedDeclaredType adt = (AnnotatedDeclaredType) alub;

            for (int i = 0; i < adt.getTypeArguments().size(); ++i) {
                AnnotatedTypeMirror adtArg = adt.getTypeArguments().get(i);
                ArrayList<AnnotatedTypeMirror> dTypesArg = new ArrayList<>();
                for (int j = 0; j < types.size(); ++j) {
                    if (types.get(j).getKind() == TypeKind.DECLARED) {
                        AnnotatedDeclaredType adtypej = (AnnotatedDeclaredType) types.get(j);
                        if (adtypej.getTypeArguments().size() == adt.getTypeArguments().size()) {
                            dTypesArg.add(adtypej.getTypeArguments().get(i));
                        } else {
                            // TODO: actually not just the number of type arguments should match, but
                            // the base types should be equal. See test case framework/GenericTest1
                            // for when this test fails.
                        }
                    }
                }

                //TODO: If we have a list of raw types, or null/raw types this code can create an empty set of
                //TODO: type arguments (dTypesArg) below.  This will then cause an exception.
                //TODO: To test this: make the conditional always true and run the nullness checker on
                //TODO: jdk/nullness/src/java/lang/ref/ReferenceQueue.java
                //TODO: I think this will only happen when we "fix-up" the lub type with a wildcard
                //TODO: in which case, the type annotator will add the annotation from the bound of
                //TODO: the type parameter for which the wildcard is an argument and we will NOT have an
                //TODO: unannotated type.  That said, we might want to just call that here to ensure
                //TODO: that when this is called in places that are not followed by annotate implicit
                //TODO: the type is fully annotated
                if (!dTypesArg.isEmpty()) {
                    addAnnotationsImpl(elements, atypeFactory, adtArg, visited, dTypesArg);
                }
            }
        } else if (alub.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType aat = (AnnotatedArrayType) alub;

            ArrayList<AnnotatedTypeMirror> compTypes = new ArrayList<>();
            for (AnnotatedTypeMirror atype : types)  {
                if (atype.getKind() == TypeKind.ARRAY) {
                    compTypes.add(((AnnotatedArrayType)atype).getComponentType());
                }
            }

            if (aat.getComponentType().getKind() == TypeKind.TYPEVAR) {
                //TODO: TERRIBLE HACK UNTIL WE FIX LUB
                final AnnotatedTypeVariable lubAtv = (AnnotatedTypeVariable) aat.getComponentType();
                final List<AnnotatedTypeVariable> subtypesAsTvs =
                        LubTypeVariableAnnotator.getSubtypesAsTypevars(lubAtv, compTypes);

                if (subtypesAsTvs != null) {
                    LubTypeVariableAnnotator.annotateTypeVarAsLub(lubAtv, subtypesAsTvs, atypeFactory);
                } else {
                    addAnnotationsImpl(elements, atypeFactory, aat.getComponentType(), visited, compTypes);
                }
            } else {
                addAnnotationsImpl(elements, atypeFactory, aat.getComponentType(), visited, compTypes);
            }
        }
        if (alub != origalub && shouldAnnoOrig) {
            // These two are not the same if origalub is a wildcard or type variable.
            // In that case, add the found annotations to the type variable also.
            // Do not put the annotations inferred for the declared type
            // on a type variable/wildcard.
            // origalub.replaceAnnotations(alub.getAnnotations());
            // Instead, keep track of the annotations that originally
            // existed on the type variable, stored in putOnOrig, and
            // put them back on now.
            origalub.replaceAnnotations(putOnOrig);
        }
    }

    /*
     * Return true if all the qualifiers are bottom qualifiers. Allow fewer
     * qualifiers to be present, which can happen for type variables and
     * wildcards.
     */
    private static boolean bottomsOnly(Elements elements, AnnotatedTypeFactory atypeFactory,
                                       Set<AnnotationMirror> annotations) {
        Set<AnnotationMirror> bots = AnnotationUtils.createAnnotationSet();
        bots.addAll(atypeFactory.getQualifierHierarchy().getBottomAnnotations());

        for (AnnotationMirror am : annotations) {
            if (!bots.remove(am)) {
                return false;
            }
        }
        return true;
    }

    /* TODO: This least upper bound computation was originally
     * in org.checkerframework.framework.flow.CFAbstractValue<V>.
     * It should be checked to make sure the implementation here is consistent.
     * Afterwards it can be removed.
     *
     * Computes and returns the least upper bound of two
     * {@link AnnotatedTypeMirror}.
     *
     * <p>
     * TODO: The code in this method is rather similar to
     * {@link CFAbstractValue#mostSpecific(CFAbstractValue, CFAbstractValue)}.
     * Can code be reused?
    public AnnotatedTypeMirror leastUpperBound(AnnotatedTypeMirror type,
            AnnotatedTypeMirror otherType) {
        GenericAnnotatedTypeFactory<V, ?, ?, ?> factory = analysis.getTypeFactory();
        ProcessingEnvironment processingEnv = factory.getProcessingEnv();
        QualifierHierarchy qualifierHierarchy = factory.getQualifierHierarchy();

        AnnotatedTypeMirror lubAnnotatedType;

        if (type.getKind() == TypeKind.ARRAY
                && otherType.getKind() == TypeKind.ARRAY) {
            // for arrays, we have:
            // lub(@A1 A @A2[],@B1 B @B2[]) = lub(@A1 A, @B1 B) lub(@A2,@B2) []
            AnnotatedArrayType a = (AnnotatedArrayType) type;
            AnnotatedArrayType b = (AnnotatedArrayType) otherType;
            AnnotatedTypeMirror componentLub = leastUpperBound(
                    a.getComponentType(), b.getComponentType());
            if (componentLub.getUnderlyingType().getKind() == TypeKind.NONE) {
                // If the components do not have an upper bound, then Object
                // is still an upper bound of the array types.
                Elements elements = analysis.getEnv().getElementUtils();
                TypeMirror underlyingType = elements.getTypeElement(
                        "java.lang.Object").asType();
                lubAnnotatedType = AnnotatedTypeMirror.createType(
                        underlyingType, factory, false);
            } else {
                TypeMirror underlyingType = TypesUtils.createArrayType(
                        analysis.getTypes(), componentLub.getUnderlyingType());
                lubAnnotatedType = AnnotatedTypeMirror.createType(
                        underlyingType, factory, false);
                AnnotatedArrayType aLubAnnotatedType = (AnnotatedArrayType) lubAnnotatedType;
                aLubAnnotatedType.setComponentType(componentLub);
            }
        } else {
            TypeMirror lubType = InternalUtils.leastUpperBound(processingEnv,
                    type.getUnderlyingType(), otherType.getUnderlyingType());
            lubAnnotatedType = AnnotatedTypeMirror.createType(lubType, factory, false);
        }

        Set<AnnotationMirror> annos1;
        Set<AnnotationMirror> annos2;
        if (QualifierHierarchy.canHaveEmptyAnnotationSet(lubAnnotatedType)) {
            annos1 = type.getAnnotations();
            annos2 = otherType.getAnnotations();
        } else {
            annos1 = type.getEffectiveAnnotations();
            annos2 = otherType.getEffectiveAnnotations();
        }

        lubAnnotatedType.addAnnotations(qualifierHierarchy.leastUpperBounds(
                type, otherType, annos1, annos2));

        TypeKind kind = lubAnnotatedType.getKind();
        if (kind == TypeKind.WILDCARD) {
            AnnotatedWildcardType wLubAnnotatedType = (AnnotatedWildcardType) lubAnnotatedType;
            AnnotatedTypeMirror extendsBound = wLubAnnotatedType
                    .getExtendsBound();
            extendsBound.clearAnnotations();
            Collection<AnnotationMirror> extendsBound1 = getUpperBound(type);
            Collection<AnnotationMirror> extendsBound2 = getUpperBound(otherType);
            extendsBound.addAnnotations(qualifierHierarchy.leastUpperBounds(
                    extendsBound1, extendsBound2));
        } else if (kind == TypeKind.TYPEVAR) {
            AnnotatedTypeVariable tLubAnnotatedType = (AnnotatedTypeVariable) lubAnnotatedType;
            AnnotatedTypeMirror upperBound = tLubAnnotatedType.getUpperBound();
            Collection<AnnotationMirror> upperBound1 = getUpperBound(type);
            Collection<AnnotationMirror> upperBound2 = getUpperBound(otherType);

            // TODO: how is it possible that uppBound1 or 2 does not have any
            // annotations?
            if (upperBound1.size() != 0 && upperBound2.size() != 0) {
                upperBound.clearAnnotations();
                upperBound.addAnnotations(qualifierHierarchy.leastUpperBounds(
                        upperBound1, upperBound2));
            }

            // if only one of the input types were type variables, then we want
            // the effective annotations and take the lub of them
            if (type.getKind() != TypeKind.TYPEVAR || otherType.getKind() != TypeKind.TYPEVAR) {
                // TODO Why the special treatment for NULL?
                if (otherType.getKind() == TypeKind.NULL) {
                    // TODO Why the flipping between the two?
                    if (type.getKind() != TypeKind.TYPEVAR) {
                        AnnotatedTypeMirror tmp = otherType;
                        otherType = type;
                        type = tmp;
                    }
                    // Do these hold?
                    // assert type.getKind() == TypeKind.TYPEVAR ||
                    //        type.getKind() == TypeKind.WILDCARD : "Unexpected type: " + type;
                    // assert otherType.getKind() != TypeKind.TYPEVAR : "Unexpected type variable: " + otherType;

                    lubAnnotatedType.clearAnnotations();
                    lubAnnotatedType.addAnnotations(type.getAnnotations());
                    for (AnnotationMirror top : qualifierHierarchy.getTopAnnotations()) {
                        AnnotationMirror o = otherType.getAnnotationInHierarchy(top);
                        assert o != null : "null should have all annotations.";
                        if (AnnotationUtils.areSame(o,
                                qualifierHierarchy.getBottomAnnotation(top))) {
                            // if the annotation on 'null' is the bottom
                            // annotation, take whatever is present on the type
                            // variable (even if it is nothing)...
                            // (already done)
                        } else {
                            // ... otherwise, take the LUB of the effective
                            // annotations.
                            lubAnnotatedType.replaceAnnotation(
                                    qualifierHierarchy.leastUpperBound(o,
                                            type.getEffectiveAnnotationInHierarchy(top)));
                        }
                    }
                }
            }
        } else if (kind == TypeKind.ARRAY
                && !(type.getKind() == TypeKind.ARRAY && otherType.getKind() == TypeKind.ARRAY)) {
            AnnotatedArrayType aLubAnnotatedType = (AnnotatedArrayType) lubAnnotatedType;
            // lub(a,b) is an array, but not both a and b are arrays -> either a
            // or b must be the null type.
            AnnotatedArrayType array;
            if (type.getKind() == TypeKind.ARRAY) {
                assert otherType.getKind() == TypeKind.NULL;
                array = (AnnotatedArrayType) type;
            } else {
                assert otherType.getKind() == TypeKind.ARRAY;
                assert type.getKind() == TypeKind.NULL;
                array = (AnnotatedArrayType) otherType;
            }
            // copy over annotations
            copyArrayComponentAnnotations(array, aLubAnnotatedType);
        }
        return lubAnnotatedType;
    }
    */


    /**
     * Returns the method parameters for the invoked method, with the same number
     * of arguments passed in the methodInvocation tree.
     *
     * If the invoked method is not a vararg method or it is a vararg method
     * but the invocation passes an array to the vararg parameter, it would simply
     * return the method parameters.
     *
     * Otherwise, it would return the list of parameters as if the vararg is expanded
     * to match the size of the passed arguments.
     *
     * @param method the method's type
     * @param args the arguments to the method invocation
     * @return  the types that the method invocation arguments need to be subtype of
     */
    public static List<AnnotatedTypeMirror> expandVarArgs(AnnotatedTypeFactory atypeFactory,
                                                          AnnotatedExecutableType method,
                                                          List<? extends ExpressionTree> args) {
        List<AnnotatedTypeMirror> parameters = method.getParameterTypes();
        if (!method.getElement().isVarArgs()) {
            return parameters;
        }

        AnnotatedArrayType varargs = (AnnotatedArrayType)parameters.get(parameters.size() - 1);

        if (parameters.size() == args.size()) {
            // Check if one sent an element or an array
            AnnotatedTypeMirror lastArg = atypeFactory.getAnnotatedType(args.get(args.size() - 1));
            if (lastArg.getKind() == TypeKind.ARRAY &&
                    getArrayDepth(varargs) == getArrayDepth((AnnotatedArrayType)lastArg)) {
                return parameters;
            }
        }

        parameters = new ArrayList<>(parameters.subList(0, parameters.size() - 1));
        for (int i = args.size() - parameters.size(); i > 0; --i) {
            parameters.add(varargs.getComponentType());
        }

        return parameters;
    }

    public static List<AnnotatedTypeMirror> expandVarArgsFromTypes(AnnotatedExecutableType method,
                                                                   List<AnnotatedTypeMirror> args) {
        List<AnnotatedTypeMirror> parameters = method.getParameterTypes();
        if (!method.getElement().isVarArgs()) {
            return parameters;
        }

        AnnotatedArrayType varargs = (AnnotatedArrayType)parameters.get(parameters.size() - 1);

        if (parameters.size() == args.size()) {
            // Check if one sent an element or an array
            AnnotatedTypeMirror lastArg = args.get(args.size() - 1);
            if (lastArg.getKind() == TypeKind.ARRAY &&
                    getArrayDepth(varargs) == getArrayDepth((AnnotatedArrayType)lastArg)) {
                return parameters;
            }
        }

        parameters = new ArrayList<>(parameters.subList(0, parameters.size() - 1));
        for (int i = args.size() - parameters.size(); i > 0; --i) {
            parameters.add(varargs.getComponentType());
        }

        return parameters;
    }

    /**
     * Given an AnnotatedExecutableType of a method or constructor declaration, get the parameter type
     * expect at the indexth position (unwrapping var args if necessary).
     *
     * @param methodType AnnotatedExecutableType of method or constructor containing parameter to return
     * @param index position of parameter type to return
     * @return If that parameter is a varArgs, return the component of the var args and NOT the array type.
     *         Otherwise, return the exact type of the parameter in the index position
     */
    public static AnnotatedTypeMirror getAnnotatedTypeMirrorOfParameter(AnnotatedExecutableType methodType, int index) {
        List<AnnotatedTypeMirror> parameterTypes = methodType.getParameterTypes();
        boolean hasVarArg = methodType.getElement().isVarArgs();

        final int lastIndex = parameterTypes.size() - 1;
        final AnnotatedTypeMirror lastType = parameterTypes.get(lastIndex);
        final boolean parameterBeforeVarargs = index < lastIndex;
        if (!parameterBeforeVarargs && lastType instanceof AnnotatedArrayType) {
            final AnnotatedArrayType arrayType = (AnnotatedArrayType) lastType;
            if (hasVarArg) {
                return arrayType.getComponentType();
            }
        }
        return parameterTypes.get(index);
    }

    /**
     * Return a list of the AnnotatedTypeMirror of the passed
     * expression trees, in the same order as the trees.
     *
     * @param paramTypes The parameter types to use as assignment context
     * @param trees the AST nodes
     * @return  a list with the AnnotatedTypeMirror of each tree in trees.
     */
    public static List<AnnotatedTypeMirror> getAnnotatedTypes(AnnotatedTypeFactory atypeFactory,
                                                              List<AnnotatedTypeMirror> paramTypes, List<? extends ExpressionTree> trees) {
        assert paramTypes.size() == trees.size() : "AnnotatedTypes.getAnnotatedTypes: size mismatch! " +
                "Parameter types: " + paramTypes + " Arguments: " + trees;
        List<AnnotatedTypeMirror> types = new ArrayList<>();
        Pair<Tree, AnnotatedTypeMirror> preAssCtxt = atypeFactory.getVisitorState().getAssignmentContext();

        try {
            for (int i = 0; i < trees.size(); ++i) {
                AnnotatedTypeMirror param = paramTypes.get(i);
                atypeFactory.getVisitorState().setAssignmentContext(Pair.<Tree, AnnotatedTypeMirror>of((Tree) null, param));
                ExpressionTree arg = trees.get(i);
                types.add(atypeFactory.getAnnotatedType(arg));
            }
        } finally {
            atypeFactory.getVisitorState().setAssignmentContext(preAssCtxt);
        }
        return types;
    }

    // TODO: can't we do better than comparing the strings?
    public static boolean areSame(AnnotatedTypeMirror t1, AnnotatedTypeMirror t2) {
        return t1.toString().equals(t2.toString());
    }

    /**
     * Returns the depth of the array type of the provided array.
     *
     * @param array the type of the array
     * @return  the depth of the provided array
     */
    public static int getArrayDepth(AnnotatedArrayType array) {
        int counter = 0;
        AnnotatedTypeMirror type = array;
        while (type.getKind() == TypeKind.ARRAY) {
            counter++;
            type = ((AnnotatedArrayType)type).getComponentType();
        }
        return counter;
    }

    // The innermost *array* type.
    public static AnnotatedTypeMirror innerMostType(AnnotatedTypeMirror t) {
        AnnotatedTypeMirror inner = t;
        while (inner.getKind() == TypeKind.ARRAY)
            inner = ((AnnotatedArrayType)inner).getComponentType();
        return inner;
    }


    /**
     * Checks whether type contains the given modifier, also recursively in type arguments and arrays.
     * This method might be easier to implement directly as instance method in AnnotatedTypeMirror;
     * it corresponds to a "deep" version of
     * {@link AnnotatedTypeMirror#hasAnnotation(AnnotationMirror)}.
     *
     * @param type the type to search.
     * @param modifier the modifier to search for.
     * @return whether the type contains the modifier.
     */
    public static boolean containsModifier(AnnotatedTypeMirror type, AnnotationMirror modifier) {
        return containsModifierImpl(type, modifier, new LinkedList<AnnotatedTypeMirror>());
    }

    /*
     * For type variables we might hit the same type again. We keep a list of visited types.
     */
    private static boolean containsModifierImpl(AnnotatedTypeMirror type, AnnotationMirror modifier,
                                                List<AnnotatedTypeMirror> visited) {
        boolean found = type.hasAnnotation(modifier);
        boolean vis = visited.contains(type);
        visited.add(type);

        if (!found && !vis) {
            if (type.getKind() == TypeKind.DECLARED) {
                AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) type;
                for (AnnotatedTypeMirror typeMirror : declaredType.getTypeArguments()) {
                    found |= containsModifierImpl(typeMirror, modifier, visited);
                    if (found) {
                        break;
                    }
                }
            } else if (type.getKind() == TypeKind.ARRAY) {
                AnnotatedArrayType arrayType = (AnnotatedArrayType) type;
                found = containsModifierImpl(arrayType.getComponentType(), modifier, visited);
            } else if (type.getKind() == TypeKind.TYPEVAR) {
                AnnotatedTypeVariable atv = (AnnotatedTypeVariable) type;
                if (atv.getUpperBound() != null) {
                    found = containsModifierImpl(atv.getUpperBound(), modifier, visited);
                }
                if (!found && atv.getLowerBound() != null) {
                    found = containsModifierImpl(atv.getLowerBound(), modifier, visited);
                }
            } else if (type.getKind() == TypeKind.WILDCARD) {
                AnnotatedWildcardType awc = (AnnotatedWildcardType) type;
                if (awc.getExtendsBound() != null) {
                    found = containsModifierImpl(awc.getExtendsBound(), modifier, visited);
                }
                if (!found && awc.getSuperBound() != null) {
                    found = containsModifierImpl(awc.getSuperBound(), modifier, visited);
                }
            }
        }

        return found;
    }


    private static Map<TypeElement, Boolean> isTypeAnnotationCache = new IdentityHashMap<>();

    public static boolean isTypeAnnotation(AnnotationMirror anno) {
        TypeElement elem = (TypeElement)anno.getAnnotationType().asElement();
        if (isTypeAnnotationCache.containsKey(elem)) {
            return isTypeAnnotationCache.get(elem);
        }

        // the annotation is a type annotation if it has the proper ElementTypes in the @Target meta-annotation
        boolean result = hasTypeQualifierElementTypes(elem.getAnnotation(Target.class).value());
        isTypeAnnotationCache.put(elem, result);
        return result;
    }

    /**
     * Sees if the passed in array of {@link ElementType} values have the correct set of
     * values which defines a type qualifier
     *
     * @param elements
     *            an array of {@link ElementType} values
     * @return true if the array only has {@link ElementType#TYPE_USE} (and
     *         optionally {@link ElementType#TYPE_PARAMETER}), false if it
     *         contains anything else
     */
    public static boolean hasTypeQualifierElementTypes(ElementType[] elements) {
        boolean hasTypeUse = false;
        boolean hasOtherElementTypes = false;

        for (ElementType element : elements) {
            if (element.equals(ElementType.TYPE_USE)) {
                // valid annotations have to have TYPE_USE
                hasTypeUse = true;
            } else if (!element.equals(ElementType.TYPE_PARAMETER)) {
                // if there's an ElementType with a enumerated value of something other than
                // TYPE_USE or TYPE_PARAMETER then it isn't a valid annotation
                hasOtherElementTypes = true;
            }
        }

        return (hasTypeUse && !hasOtherElementTypes);
    }

    public static boolean containsTypeAnnotation(Collection<? extends AnnotationMirror> annos) {
        for (AnnotationMirror am : annos) {
            if (isTypeAnnotation(am)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the given {@link AnnotatedTypeMirror} passed a set of
     * well-formedness checks. The method will never return false for valid
     * types, but might not catch all invalid types.
     *
     * <p>
     * Currently, the following is checked:
     * <ol>
     * <li>There should not be multiple annotations from the same hierarchy.
     * <li>There should not be more annotations than the width of the qualifier
     * hierarchy.
     * <li>If the type is not a type variable, then the number of annotations
     * should be the same as the width of the qualifier hierarchy.
     * <li>These properties should also hold recursively for component types of
     * arrays, as wells as bounds of type variables and wildcards.
     * </ol>
     */
    public static boolean isValidType(QualifierHierarchy qualifierHierarchy,
                                      AnnotatedTypeMirror type) {
        boolean res = isValidType(qualifierHierarchy, type,
                Collections.<AnnotatedTypeMirror>emptySet());
        return res;
    }

    private static boolean isValidType(QualifierHierarchy qualifierHierarchy,
                                       AnnotatedTypeMirror type, Set<AnnotatedTypeMirror> v) {
        if (type == null) {
            return false;
        }

        Set<AnnotatedTypeMirror> visited = new HashSet<>(v);
        if (visited.contains(type)) {
            return true; // prevent infinite recursion
        }
        visited.add(type);

        // multiple annotations from the same hierarchy
        Set<AnnotationMirror> annotations = type.getAnnotations();
        Set<AnnotationMirror> seenTops = AnnotationUtils.createAnnotationSet();
        int n = 0;
        for (AnnotationMirror anno : annotations) {
            if (QualifierPolymorphism.isPolyAll(anno)) {
                // ignore PolyAll when counting annotations
                continue;
            }
            n++;
            AnnotationMirror top = qualifierHierarchy.getTopAnnotation(anno);
            if (seenTops.contains(top)) {
                return false;
            }
            seenTops.add(top);
        }

        // too many annotations
        int expectedN = qualifierHierarchy.getWidth();
        if (n > expectedN) {
            return false;
        }

        // treat types that have polyall like type variables
        boolean hasPolyAll = type.hasAnnotation(PolyAll.class);
        boolean canHaveEmptyAnnotationSet =
                QualifierHierarchy.canHaveEmptyAnnotationSet(type) ||
                        hasPolyAll;

        // wrong number of annotations
        if (!canHaveEmptyAnnotationSet && n != expectedN) {
            return false;
        }

        // recurse for composite types
        if (type instanceof AnnotatedArrayType) {
            AnnotatedArrayType at = (AnnotatedArrayType) type;
            if (!isValidType(qualifierHierarchy, at.getComponentType(), visited)) {
                return false;
            }
        } else if (type instanceof AnnotatedTypeVariable) {
            AnnotatedTypeVariable at = (AnnotatedTypeVariable) type;
            AnnotatedTypeMirror lowerBound = at.getLowerBound();
            AnnotatedTypeMirror upperBound = at.getUpperBound();
            if (lowerBound != null
                    && !isValidType(qualifierHierarchy, lowerBound, visited)) {
                return false;
            }
            if (upperBound != null
                    && !isValidType(qualifierHierarchy, upperBound, visited)) {
                return false;
            }
        } else if (type instanceof AnnotatedWildcardType) {
            AnnotatedWildcardType at = (AnnotatedWildcardType) type;
            AnnotatedTypeMirror extendsBound = at.getExtendsBound();
            AnnotatedTypeMirror superBound = at.getSuperBound();
            if (extendsBound != null
                    && !isValidType(qualifierHierarchy, extendsBound, visited)) {
                return false;
            }
            if (superBound != null
                    && !isValidType(qualifierHierarchy, superBound, visited)) {
                return false;
            }
        }
        // TODO: the recursive checks on type arguments are currently skipped, because
        // this breaks various tests.  it seems that checking the validity changes the
        // annotations on some types.
//        } else if (type instanceof AnnotatedDeclaredType) {
//            AnnotatedDeclaredType at = (AnnotatedDeclaredType) type;
//            for (AnnotatedTypeMirror typeArgument : at.getTypeArguments()) {
//                if (!isValidType(qualifierHierarchy, typeArgument, visited)) {
//                    return false;
//                }
//            }
//        }
        return true;
    }

    private static String annotationClassName = java.lang.annotation.Annotation.class.getCanonicalName();

    /**
     * @return true if the underlying type of this atm is a java.lang.annotation.Annotation
     */
    public static boolean isJavaLangAnnotation(final AnnotatedTypeMirror atm) {
        return TypesUtils.isDeclaredOfName(atm.getUnderlyingType(), annotationClassName);
    }

    /**
     * @return true if atm is an Annotation interface, i.e. an implementation of java.lang.annotation.Annotation
     * e.g. @interface MyAnno - implementsAnnotation would return true when called on an
     * AnnotatedDeclaredType representing a use of MyAnno
     */
    public static boolean implementsAnnotation(final AnnotatedTypeMirror atm) {
        if (atm.getKind() != TypeKind.DECLARED) {
            return false;
        }
        final AnnotatedTypeMirror.AnnotatedDeclaredType declaredType = (AnnotatedTypeMirror.AnnotatedDeclaredType) atm;

        Symbol.ClassSymbol classSymbol = (Symbol.ClassSymbol) declaredType.getUnderlyingType().asElement();
        for (final Type iface : classSymbol.getInterfaces() ) {
            if (TypesUtils.isDeclaredOfName(iface, annotationClassName)) {
                return true;
            }
        }

        return false;
    }


    public static boolean isEnum(final AnnotatedTypeMirror typeMirror) {
        if (typeMirror.getKind() == TypeKind.DECLARED) {
            final AnnotatedDeclaredType adt = (AnnotatedDeclaredType) typeMirror;
            return TypesUtils.isDeclaredOfName(adt.getUnderlyingType(), java.lang.Enum.class.getName());
        }

        return false;
    }

    public static boolean isDeclarationOfJavaLangEnum(final Types types, final Elements elements,
                                                      final AnnotatedTypeMirror typeMirror) {
        if (isEnum(typeMirror)) {
            return elements.getTypeElement("java.lang.Enum").equals(
                   ((AnnotatedDeclaredType) typeMirror).getUnderlyingType().asElement());
        }

        return false;
    }


    /**
     * @return true if the typeVar1 and typeVar2 are two uses of the same type variable
     */
    public static boolean haveSameDeclaration(Types types, final AnnotatedTypeVariable typeVar1, final AnnotatedTypeVariable typeVar2) {
        return types.isSameType(typeVar1.getUnderlyingType(), typeVar2.getUnderlyingType());
    }

    /**
     * When overriding a method, you must include the same number of type parameters as the base method.  By index,
     * these parameters are considered equivalent to the type parameters of the overridden method.
     * Necessary conditions:
     *    Both type variables are defined in methods
     *    One of the two methods overrides the other
     *    Within their method declaration, both types have the same type parameter index
     *
     * @return  returns true if type1 and type2 are corresponding type variables (that is, either one "overrides" the other).
     */
    public static boolean areCorrespondingTypeVariables(Elements elements, AnnotatedTypeVariable type1, AnnotatedTypeVariable type2) {
        final TypeParameterElement type1ParamElem   = (TypeParameterElement) type1.getUnderlyingType().asElement();
        final TypeParameterElement type2ParamElem = (TypeParameterElement) type2.getUnderlyingType().asElement();


        if (type1ParamElem.getGenericElement() instanceof ExecutableElement
         && type2ParamElem.getGenericElement() instanceof ExecutableElement) {
            final ExecutableElement type1Executable   = (ExecutableElement) type1ParamElem.getGenericElement();
            final ExecutableElement type2Executable = (ExecutableElement) type2ParamElem.getGenericElement();

            final TypeElement type1Class = (TypeElement) type1Executable.getEnclosingElement();
            final TypeElement type2Class = (TypeElement) type2Executable.getEnclosingElement();

            boolean methodIsOverriden = elements.overrides(type1Executable, type2Executable, type1Class)
                                     || elements.overrides(type2Executable, type1Executable, type2Class);
            if (methodIsOverriden) {
                boolean haveSameIndex = type1Executable.getTypeParameters().indexOf(type1ParamElem) ==
                                        type2Executable.getTypeParameters().indexOf(type2ParamElem);
                return haveSameIndex;
            }
        }

        return false;
    }

    /**
     * When comparing types against the bounds of a type variable, we may encounter other
     * type variables, wildcards, and intersections in those bounds.  This method traverses
     * the bounds until it finds a concrete type from which it can pull an annotation.
     * @param top The top of the hierarchy for which you are searching.
     * @return The AnnotationMirror that represents the type of toSearch in the hierarchy of top
     */
    public static AnnotationMirror findEffectiveAnnotationInHierarchy(final QualifierHierarchy qualifierHierarchy,
                                                                      final AnnotatedTypeMirror toSearch,
                                                                      final AnnotationMirror top) {
        return findEffectiveAnnotationInHierarchy(qualifierHierarchy, toSearch, top, false);
    }
    /**
     * When comparing types against the bounds of a type variable, we may encounter other
     * type variables, wildcards, and intersections in those bounds.  This method traverses
     * the bounds until it finds a concrete type from which it can pull an annotation.
     * @param top The top of the hierarchy for which you are searching.
     * @param canBeEmpty Whether or not the effective type can have NO annotation in the hierarchy specified by top
     *                   If this param is false, an exception will be thrown if no annotation is found
     *                   Otherwise the result is null
     * @return The AnnotationMirror that represents the type of toSearch in the hierarchy of top
     */
    public static AnnotationMirror findEffectiveAnnotationInHierarchy(final QualifierHierarchy qualifierHierarchy,
                                                                      final AnnotatedTypeMirror toSearch,
                                                                      final AnnotationMirror top,
                                                                      final boolean canBeEmpty) {
        AnnotatedTypeMirror source = toSearch;
        while (source.getAnnotationInHierarchy(top) == null) {

            switch (source.getKind()) {
                case TYPEVAR:
                    source = ((AnnotatedTypeVariable) source).getUpperBound();
                    break;

                case WILDCARD:
                    source = ((AnnotatedWildcardType) source).getExtendsBound();
                    break;

                case INTERSECTION:
                    //if there are multiple conflicting annotations, choose the lowest
                    final AnnotationMirror glb = glbOfBoundsInHierarchy((AnnotatedIntersectionType) source, top, qualifierHierarchy);

                    if (glb == null) {
                        ErrorReporter.errorAbort("AnnotatedIntersectionType has no annotation in hierarchy "
                                + "on any of its supertypes!\n"
                                + "intersectionType=" + source);
                    }
                    return glb;

                default:
                    if (canBeEmpty) {
                        return null;
                    }

                    ErrorReporter.errorAbort("Unexpected AnnotatedTypeMirror with no primary annotation!\n"
                            + "toSearch=" + toSearch + "\n"
                            + "top="      + top      + "\n"
                            + "source=" + source);
                    return null;
            }
        }

        return source.getAnnotationInHierarchy(top);
    }

    /**
     * When comparing types against the bounds of a type variable, we may encounter other
     * type variables, wildcards, and intersections in those bounds.  This method traverses
     * the lower bounds until it finds a concrete type from which it can pull an annotation.
     * This occurs for every hierarchy in QualifierHierarchy
     * @return The set of effective annotation mirrors in all hierarchies
     */
    public static Set<AnnotationMirror> findEffectiveLowerBoundAnnotations(final QualifierHierarchy qualifierHierarchy,
                                                                           final AnnotatedTypeMirror toSearch) {
        AnnotatedTypeMirror source = toSearch;
        TypeKind kind = source.getKind();
        while (kind == TypeKind.TYPEVAR
                || kind == TypeKind.WILDCARD
                || kind == TypeKind.INTERSECTION) {

            switch (source.getKind()) {
                case TYPEVAR:
                    source = ((AnnotatedTypeVariable) source).getLowerBound();
                    break;

                case WILDCARD:
                    source = ((AnnotatedWildcardType) source).getSuperBound();
                    break;

                case INTERSECTION:
                    //if there are multiple conflicting annotations, choose the lowest
                    final Set<AnnotationMirror> glb = glbOfBounds((AnnotatedIntersectionType) source, qualifierHierarchy);
                    return glb;

                default:
                    ErrorReporter.errorAbort("Unexpected AnnotatedTypeMirror with no primary annotation!"
                            + "toSearch=" + toSearch
                            + "source=" + source);
            }

            kind = source.getKind();
        }

        return source.getAnnotations();
    }

    /**
     * When comparing types against the bounds of a type variable, we may encounter other
     * type variables, wildcards, and intersections in those bounds.  This method traverses
     * the bounds until it finds a concrete type from which it can pull an annotation.
     * This occurs for every hierarchy in QualifierHierarchy
     * @return The set of effective annotation mirrors in all hierarchies
     */
    public static Set<AnnotationMirror> findEffectiveAnnotations(final QualifierHierarchy qualifierHierarchy,
                                                                 final AnnotatedTypeMirror toSearch) {
        AnnotatedTypeMirror source = toSearch;
        TypeKind kind = source.getKind();
        while (kind == TypeKind.TYPEVAR
                || kind == TypeKind.WILDCARD
                || kind == TypeKind.INTERSECTION) {

            switch (source.getKind()) {
                case TYPEVAR:
                    source = ((AnnotatedTypeVariable) source).getUpperBound();
                    break;

                case WILDCARD:
                    source = ((AnnotatedWildcardType) source).getExtendsBound();
                    break;

                case INTERSECTION:
                    //if there are multiple conflicting annotations, choose the lowest
                    final Set<AnnotationMirror> glb = glbOfBounds((AnnotatedIntersectionType) source, qualifierHierarchy);
                    return glb;

                default:
                    ErrorReporter.errorAbort("Unexpected AnnotatedTypeMirror with no primary annotation!"
                            + "toSearch=" + toSearch
                            + "source=" + source);
            }

            kind = source.getKind();
        }

        return source.getAnnotations();
    }

    /**
     * @return The greatest lower bound of the primary annotations on the input types
     */
    private static Set<? extends AnnotationMirror> glbAll(final QualifierHierarchy qualifierHierarchy,
                                                          final Collection<AnnotatedTypeMirror> types) {
        final Set<AnnotationMirror> result = AnnotationUtils.createAnnotationSet();
        Map<AnnotationMirror, AnnotationMirror> intermediate = AnnotationUtils.createAnnotationMap();

        if (types.size() == 0) {
            return result;
        }

        final Set<? extends AnnotationMirror> tops = qualifierHierarchy.getTopAnnotations();

        for (AnnotatedTypeMirror type : types) {
            for (AnnotationMirror top : tops) {
                final AnnotationMirror newAnno = type.getAnnotationInHierarchy(top);
                final AnnotationMirror prevGlb = intermediate.get(top);
                if (newAnno == null) {
                    continue;
                } //else
                if (prevGlb == null) {
                    intermediate.put(top, newAnno);
                } else {
                    intermediate.put(top, qualifierHierarchy.greatestLowerBound(newAnno, prevGlb));
                }
            }
        }

        result.addAll(intermediate.values());

        return result;
    }

    private static AnnotationMirror glbOfBoundsInHierarchy(final AnnotatedIntersectionType isect, final AnnotationMirror top,
                                                           final QualifierHierarchy qualifierHierarchy) {
        AnnotationMirror anno = isect.getAnnotationInHierarchy(top);
        for (final AnnotatedTypeMirror supertype : isect.directSuperTypes()) {
            final AnnotationMirror superAnno = supertype.getAnnotationInHierarchy(top);
            if (superAnno != null && (anno == null || qualifierHierarchy.isSubtype(superAnno, anno))) {
                anno = superAnno;
            }
        }

        return anno;
    }

    /**
     * Get's the lowest primary annotation of all bounds in the intersection
     * @param isect The intersection for which we are glbing bounds
     * @param qualifierHierarchy The qualifier used to get the hierarchies in which to glb
     * @return A set of annotations representing the glb of the intersection's bounds
     */
    public static Set<AnnotationMirror> glbOfBounds(final AnnotatedIntersectionType isect,
                                                     final QualifierHierarchy qualifierHierarchy) {
        Set<AnnotationMirror> result = AnnotationUtils.createAnnotationSet();
        for (final AnnotationMirror top : qualifierHierarchy.getTopAnnotations()) {
            final AnnotationMirror glbAnno = glbOfBoundsInHierarchy(isect, top, qualifierHierarchy);
            if (glbAnno != null) {
                result.add(glbAnno);
            }
        }

        return result;
    }

    //For Wildcards, isSuperBound and isExtendsBound will return true if isUnbound does.

    public static boolean isExplicitlySuperBounded(final AnnotatedWildcardType wildcardType) {
        return ((Type.WildcardType) wildcardType.getUnderlyingType()).isSuperBound() &&
              !((Type.WildcardType) wildcardType.getUnderlyingType()).isUnbound();
    }

    /**
     * Returns true if wildcard type was explicitly unbounded.
     */
    public static boolean isExplicitlyExtendsBounded(final AnnotatedWildcardType wildcardType) {
        return ((Type.WildcardType) wildcardType.getUnderlyingType()).isExtendsBound() &&
              !((Type.WildcardType) wildcardType.getUnderlyingType()).isUnbound();
    }

    /**
     * Returns true if this type is super bounded or unbounded.
     */
    public static boolean isUnboundedOrSuperBounded(final AnnotatedWildcardType wildcardType) {
        return ((Type.WildcardType) wildcardType.getUnderlyingType()).isSuperBound();
    }

    /**
     * Returns true if this type is extends bounded or unbounded.
     */
    public static boolean isUnboundedOrExtendsBounded(final AnnotatedWildcardType wildcardType) {
        return ((Type.WildcardType) wildcardType.getUnderlyingType()).isExtendsBound();
    }


    /**
     * Copies explicit annotations and annotations resulting from resolution of polymorphic qualifiers
     * from {@code constructor} to {@code returnType}. If {@code returnType} has an annotation in the
     * same hierarchy of an annotation to be copied, that annotation is not copied.
     *
     * @param atypeFactory type factory
     * @param returnType return type to copy annotations to
     * @param constructor The ATM for the constructor.
     */
    public static void copyOnlyExplicitConstructorAnnotations(AnnotatedTypeFactory atypeFactory,
                                                              AnnotatedDeclaredType returnType,
                                                              AnnotatedExecutableType constructor) {

        // TODO: There will be a nicer way to access this in 308 soon.
        List<Attribute.TypeCompound> decall = ((Symbol)constructor.getElement()).getRawTypeAttributes();
        Set<AnnotationMirror> decret = AnnotationUtils.createAnnotationSet();
        for (Attribute.TypeCompound da : decall) {
            if (da.position.type == com.sun.tools.javac.code.TargetType.METHOD_RETURN) {
                decret.add(da);
            }
        }

        // Collect all polymorphic qualifiers; we should substitute them.
        Set<AnnotationMirror> polys = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror anno : returnType.getAnnotations()) {
            if (QualifierPolymorphism.isPolymorphicQualified(anno)) {
                polys.add(anno);
            }
        }

        for (AnnotationMirror cta : constructor.getReturnType().getAnnotations()) {
            AnnotationMirror ctatop = atypeFactory.getQualifierHierarchy().getTopAnnotation(cta);
            if (atypeFactory.isSupportedQualifier(cta) &&
                !returnType.isAnnotatedInHierarchy(cta)) {
                for (AnnotationMirror fromDecl : decret) {
                    if (atypeFactory.isSupportedQualifier(fromDecl) &&
                        AnnotationUtils.areSame(ctatop,
                                                atypeFactory.getQualifierHierarchy().getTopAnnotation(fromDecl))) {
                        returnType.addAnnotation(cta);
                        break;
                    }
                }
            }

            // Go through the polymorphic qualifiers and see whether
            // there is anything left to replace.
            for (AnnotationMirror pa : polys) {
                if (AnnotationUtils.areSame(ctatop,
                                            atypeFactory.getQualifierHierarchy().getTopAnnotation(pa))) {
                    returnType.replaceAnnotation(cta);
                    break;
                }
            }
        }

    }

}
