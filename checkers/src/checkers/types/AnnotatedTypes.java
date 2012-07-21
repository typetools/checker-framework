package checkers.types;

import static javax.lang.model.util.ElementFilter.methodsIn;

import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;

import checkers.quals.TypeQualifier;
import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import checkers.types.AnnotatedTypeMirror.AnnotatedTypeVariable;
import checkers.types.AnnotatedTypeMirror.AnnotatedWildcardType;
import checkers.types.visitors.SimpleAnnotatedTypeVisitor;
import checkers.util.ElementUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;
import checkers.util.TypesUtils;
import checkers.nullness.quals.*;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;

/**
 * Utility methods for operating on {@code AnnotatedTypeMirror}. This
 * class mimics the class {@link Types}.
 */
public class AnnotatedTypes {

    private final ProcessingEnvironment env;
    private final AnnotatedTypeFactory factory;

    static int uidCounter = 0;
    int uid;

    /**
     * Constructor for {@code AnnotatedTypes}
     *
     * @param env  the processing environment for this round
     */
    public AnnotatedTypes(ProcessingEnvironment env, AnnotatedTypeFactory factory) {
        this.env = env;
        this.factory = factory;
        uid = ++uidCounter;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "#" + uid;
    }

    /**
     * Returns the most specific base type of {@code t} whose erasure type
     * is {@code superType}.  It returns null if {@code t} is not a subtype
     * of {@code superType}.
     *
     * @param t      a type
     * @param superType   a type that is a supertype of {@code t}
     * @return the base type of t of the given element
     */
    public AnnotatedTypeMirror asSuper(AnnotatedTypeMirror t,
            AnnotatedTypeMirror superType) {
        return asSuper.visit(t, superType);
    }

    private final SimpleAnnotatedTypeVisitor<AnnotatedTypeMirror, AnnotatedTypeMirror> asSuper =
        new SimpleAnnotatedTypeVisitor<AnnotatedTypeMirror, AnnotatedTypeMirror>() {

        @Override
        protected AnnotatedTypeMirror defaultAction(AnnotatedTypeMirror type, AnnotatedTypeMirror p) {
            return type;
        }

        @Override
        public AnnotatedTypeMirror visitPrimitive(AnnotatedPrimitiveType type, AnnotatedTypeMirror p) {
            if (!p.getKind().isPrimitive())
                return visit(factory.getBoxedType(type), p);

            AnnotatedPrimitiveType pt = (AnnotatedPrimitiveType)p;
            AnnotatedPrimitiveType st = pt.getCopy(false);
            st.addAnnotations(type.getAnnotations());
            return st;
        }

        @Override
        public AnnotatedTypeMirror visitTypeVariable(AnnotatedTypeVariable type, AnnotatedTypeMirror p) {
            if (p.getKind() == TypeKind.TYPEVAR)
                return type;
            // Operate on the effective upper bound
            AnnotatedTypeMirror res = asSuper(type.getEffectiveUpperBound(), p);
            if (res!=null && !res.isAnnotated()) {
                // TODO: or should it be the default?
                // Test MultiBoundTypeVar fails otherwise.
                // Is there a better place for this?
                res.addAnnotations(factory.getQualifierHierarchy().getTopAnnotations());
            }
            return res;
        }

        @Override
        public AnnotatedTypeMirror visitWildcard(AnnotatedWildcardType type, AnnotatedTypeMirror p) {
            if (p.getKind() == TypeKind.WILDCARD)
                return type;
            // Operate on the effective extends bound
            return asSuper(type.getEffectiveExtendsBound(), p);
        }


        @Override
        public AnnotatedTypeMirror visitArray(AnnotatedArrayType type, AnnotatedTypeMirror p) {
            // Check if array component is subtype of the element
            // first
            if (shouldStop(p, type))
                return type;
            for (AnnotatedTypeMirror st : type.directSuperTypes()) {
                AnnotatedTypeMirror x = asSuper(st, p);
                if (x != null) {
                    return isErased(x, p) ? x.getErased() : x;
                }
            }
            return null;
        }

        @Override
        public AnnotatedTypeMirror visitDeclared(AnnotatedDeclaredType type, AnnotatedTypeMirror p) {
            // If visited Element is the desired one, we are done
            if (p.getKind().isPrimitive())
                return visit(factory.getUnboxedType(type), p);

            /* Something like the following seemed sensible for intersection types,
             * which came up in the Ternary test case with classes MethodSymbol and ClassSymbol.
             * However, it results in an infinite recursion with the IGJ checker.
             * For now, let's handle the null result in the caller, TypeFromTree.visitConditionalExpression.
            if (p.getKind() == TypeKind.DECLARED &&
                    ((AnnotatedDeclaredType)p).getUnderlyingType().asElement().getSimpleName().length()==0) {
                p = ((AnnotatedDeclaredType)p).directSuperTypes().get(0);
            }
            */

            if (shouldStop(p, type))
                return type;

            // Visit the superclass first!
            for (AnnotatedDeclaredType st : type.directSuperTypes()) {
                if (st.getKind() == TypeKind.DECLARED) {
                    AnnotatedDeclaredType x = (AnnotatedDeclaredType) asSuper(st, p);
                    if (x != null)
                        return x;
                }
            }

            return null;
        }
    };

    /**
     * Return the base type of t or any of its outer types that starts
     * with the given type. If none exists, return null.
     *
     * @param t     a type
     * @param elem   a type
     */
    private AnnotatedTypeMirror asOuterSuper(AnnotatedTypeMirror t,
            AnnotatedTypeMirror elem) {
        switch (t.getKind()) {
        case DECLARED:
            do {
                // Search among supers for a desired supertype
                AnnotatedTypeMirror s = asSuper(t, elem);
                if (s != null)
                    return s;
                // if not found immediately, try enclosing type
                // like A in A.B
                t = t.getEnclosingType();
            } while (t != null && t.getKind() == TypeKind.DECLARED);
            return null;
        case ARRAY:     // intentional follow-through
        case TYPEVAR:   // intentional follow-through
        case WILDCARD:
            return asSuper(t, elem);
        default:
            return null;
        }
    }

    /*
     * Returns true if sup and sub are the same type.
     * Returns false otherwise (including if sub cannot be a subtype of sup).
     */
    private boolean shouldStop(AnnotatedTypeMirror sup, AnnotatedTypeMirror sub) {
        // Check if it's the same type
        // if sup is primitive, but not sub
        if (sup.getKind().isPrimitive() && !sub.getKind().isPrimitive())
            /// XXX shouldn't this be "return false"?
            return true;
        if (sup.getKind().isPrimitive() && sub.getKind().isPrimitive())
            return sup.getKind() == sub.getKind();
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
    private boolean isErased(AnnotatedTypeMirror t1, AnnotatedTypeMirror t2) {
        Types types = env.getTypeUtils();
        return types.isSameType(
                types.erasure(t1.getUnderlyingType()), t2.getUnderlyingType());
    }

    /**
     * @see #asMemberOf(AnnotatedTypeMirror, Element)
     */
    public AnnotatedExecutableType asMemberOf(AnnotatedTypeMirror t,
            ExecutableElement elem) {
        return (AnnotatedExecutableType) asMemberOf(t, (Element) elem);
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
     * @param t    a type
     * @param elem  an element
     */
    public AnnotatedTypeMirror asMemberOf(AnnotatedTypeMirror t, Element elem) {
        // asMemberOf is only for fields, variables, and methods!
        // Otherwise, simply use fromElement.
        switch (elem.getKind()) {
        case PACKAGE:
        case INSTANCE_INIT:
        case OTHER:
        case STATIC_INIT:
        case TYPE_PARAMETER:
            return factory.fromElement(elem);
        }
        AnnotatedTypeMirror type = asMemberOfImpl(t, elem);
        if (!ElementUtils.isStatic(elem))
            factory.postAsMemberOf(type, t, elem);
        return type;
    }

    private AnnotatedTypeMirror asMemberOfImpl(final AnnotatedTypeMirror t, final Element elem) {
        if (ElementUtils.isStatic(elem))
            return factory.getAnnotatedType(elem);

        // For Type Variable, operate on the upper
        if (t.getKind() == TypeKind.TYPEVAR &&
                ((AnnotatedTypeVariable)t).getUpperBound() != null)
            return asMemberOf(((AnnotatedTypeVariable) t).getUpperBound(),
                    elem);

        if (t.getKind() == TypeKind.ARRAY
                && elem.getKind() == ElementKind.METHOD
                && elem.getSimpleName().contentEquals("clone")) {
                AnnotatedExecutableType method = (AnnotatedExecutableType)factory.getAnnotatedType(elem);
                return method.substitute(Collections.singletonMap(method.getReturnType(), t));
        }

        final AnnotatedTypeMirror elemType = factory.getAnnotatedType(elem);

        // I cannot think of why it wouldn't be a declared type!
        // Defensive Programming
        if (t.getKind() != TypeKind.DECLARED) {
            return elemType;
        }

        //
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
            while (encl!=null) {
                if (!encl.getTypeParameters().isEmpty()) {
                    ownerGeneric = true;
                    break;
                }
                encl = ElementUtils.enclosingClass(encl.getEnclosingElement());
            }
        }

        // TODO: Potential bug if Raw type is used
        if (ElementUtils.isStatic(elem) || !ownerGeneric)
            return elemType;

        AnnotatedDeclaredType ownerType = factory.getAnnotatedType(owner);
        AnnotatedDeclaredType base =
            (AnnotatedDeclaredType) asOuterSuper(t, ownerType);

        if (base == null)
            return elemType;

        List<? extends AnnotatedTypeMirror> ownerParams =
            ownerType.getTypeArguments();
        List<? extends AnnotatedTypeMirror> baseParams =
            base.getTypeArguments();
        if (!ownerParams.isEmpty()) {
            if (baseParams.isEmpty()) {
                List<AnnotatedTypeMirror> baseParamsEr = new ArrayList<AnnotatedTypeMirror>();
                for (AnnotatedTypeMirror arg : ownerParams)
                    baseParamsEr.add(arg.getErased());
                return subst(elemType, ownerParams, baseParamsEr);
            }
            return subst(elemType, ownerParams, baseParams);
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
    public AnnotatedTypeMirror subst(AnnotatedTypeMirror t,
            List<? extends AnnotatedTypeMirror> from,
            List<? extends AnnotatedTypeMirror> to) {
        Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mappings =
            new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>();

        for (int i = 0; i < from.size(); ++i) {
            mappings.put(from.get(i), to.get(i));
        }
        return t.substitute(mappings);
    }


    /**
     * Returns a deep copy of the passed type.
     *
     * @param type  the annotated type to be copied
     * @return a deep copy of the passed type
     */
    @SuppressWarnings("unchecked")
    public static <ATM extends AnnotatedTypeMirror> ATM deepCopy(ATM type) {
        // TODO: Test this, specify behavior, merge/compare to ATM.copy
        ATM result = (ATM) type.substitute(Collections.<AnnotatedTypeMirror,
                AnnotatedTypeMirror>emptyMap());
        return result;
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
    public AnnotatedTypeMirror getIteratedType(AnnotatedTypeMirror iterableType) {
        if (iterableType.getKind() == TypeKind.ARRAY) {
            return ((AnnotatedArrayType) iterableType).getComponentType();
        }

        // For type variables and wildcards take the effective upper bound.
        if (iterableType.getKind() == TypeKind.WILDCARD)
            return getIteratedType(((AnnotatedWildcardType) iterableType).getEffectiveExtendsBound());
        if (iterableType.getKind() == TypeKind.TYPEVAR)
            return getIteratedType(((AnnotatedTypeVariable) iterableType).getEffectiveUpperBound());

        if (iterableType.getKind() != TypeKind.DECLARED) {
            SourceChecker.errorAbort("AnnotatedTypes.getIteratedType: not iterable type: " + iterableType);
            return null; // dead code
        }

        TypeElement iterableElement = env.getElementUtils().getTypeElement("java.lang.Iterable");
        AnnotatedDeclaredType iterableElmType = factory.getAnnotatedType(iterableElement);
        AnnotatedDeclaredType dt = (AnnotatedDeclaredType) asSuper(iterableType, iterableElmType);
        if (dt == null) {
            SourceChecker.errorAbort("AnnotatedTypes.getIteratedType: not iterable type: " + iterableType);
            return null; // dead code
        } else if (dt.getTypeArguments().isEmpty()) {
            TypeElement e = env.getElementUtils().getTypeElement("java.lang.Object");
            AnnotatedDeclaredType t = factory.fromElement(e);
            t.clearAnnotations();
            factory.annotateImplicit(e, t);
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
    public Set<AnnotatedDeclaredType> getSuperTypes(AnnotatedDeclaredType type) {

        Set<AnnotatedDeclaredType> supertypes = new HashSet<AnnotatedDeclaredType>();
        if (type == null)
            return supertypes;

        // Set up a stack containing the type mirror of subtype, which
        // is our starting point.
        Deque<AnnotatedDeclaredType> stack = new ArrayDeque<AnnotatedDeclaredType>();
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
    public Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods(
            ExecutableElement method) {
        final TypeElement elem = (TypeElement) method.getEnclosingElement();
        final AnnotatedDeclaredType type = factory.getAnnotatedType(elem);
        final Collection<AnnotatedDeclaredType> supertypes = getSuperTypes(type);
        return overriddenMethods(method, supertypes);
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
    public Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods(
            ExecutableElement method, Collection<AnnotatedDeclaredType> supertypes) {

        Map<AnnotatedDeclaredType, ExecutableElement> overrides =
            new HashMap<AnnotatedDeclaredType, ExecutableElement>();

        for (AnnotatedDeclaredType supertype : supertypes) {
            /*@Nullable*/ TypeElement superElement =
                (TypeElement) supertype.getUnderlyingType().asElement();
            assert superElement != null; /*nninvariant*/
            // For all method in the supertype, add it to the set if
            // it overrides the given method.
            for (ExecutableElement supermethod : methodsIn(superElement.getEnclosedElements())) {
                if (env.getElementUtils().overrides(method, supermethod,
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
     * @param expr the method or constructor invocation tree; the passed argument
     *   has to be a subtype of MethodInvocationTree or NewClassTree.
     * @return the mapping of the type variables to type arguments for
     *   this method or constructor invocation.
     */
    public Map<AnnotatedTypeVariable, AnnotatedTypeMirror>
    findTypeArguments(ExpressionTree expr) {
        Map<AnnotatedTypeVariable, AnnotatedTypeMirror> typeArguments =
            new HashMap<AnnotatedTypeVariable, AnnotatedTypeMirror>();

        ExecutableElement elt;
        if (expr instanceof MethodInvocationTree ||
                expr instanceof NewClassTree) {
            elt = (ExecutableElement) TreeUtils.elementFromUse(expr);
        } else {
            // This case should never happen.
            System.err.println("AnnotatedTypes.findTypeArguments: unexpected tree: " + expr);
            elt = null;
        }

        // Is the method a generic method?
        if (elt.getTypeParameters().isEmpty())
            return typeArguments;

        List<? extends Tree> targs;
        if (expr instanceof MethodInvocationTree) {
            targs = ((MethodInvocationTree) expr).getTypeArguments();
        } else if (expr instanceof NewClassTree) {
            targs = ((NewClassTree) expr).getTypeArguments();
        } else {
            targs = null;
        }

        // Has the user supplied type arguments?
        if (!targs.isEmpty()) {
            List<? extends TypeParameterElement> tvars = elt.getTypeParameters();

            for (int i = 0; i < elt.getTypeParameters().size(); ++i) {
                AnnotatedTypeVariable typeVar = (AnnotatedTypeVariable) factory.getAnnotatedType(tvars.get(i));
                AnnotatedTypeMirror typeArg = factory.getAnnotatedTypeFromTypeTree(targs.get(i));
                typeArguments.put(typeVar, typeArg);
            }
            return typeArguments;
        } else {
            return inferTypeArguments(expr);
        }
    }


    /**
     * Return the method or constructor invocation type arguments if specified, otherwise
     * infer them based on the passed arguments or the return type context,
     * according to JLS 15.12.2.
     *
     * @param expr the method or constructor invocation tree; the passed argument
     *   has to be a subtype of MethodInvocationTree or NewClassTree.
     * @return the mapping of the type variables to type arguments for
     *   this method or constructor invocation.
     */

    // TODO: Note that this implementation is buggy as it only infers arguments
    // that make it to the return type.  So it would fail for invocations to
    // <T> void test(T arg1, T arg2)
    // in such cases, T is inferred to be '? extends T.upperBound'
    private Map<AnnotatedTypeVariable, AnnotatedTypeMirror>
    inferTypeArguments(ExpressionTree expr) {
        //
        // The basic algorithm used here, for each type variable:
        // 1. Find the un-annotated  least upper bound for the type variable
        //    by finding the type bound to variable within the return type
        // 2. Infer the type argument annotations using the passed arguments
        // 3. If the type variable is not used within a parameter, find
        //    the assignment context and infer the type argument using it
        // 4. if not within an assignment context, then bind it to the extend bound.
        Map<AnnotatedTypeVariable, AnnotatedTypeMirror> typeArguments =
            new HashMap<AnnotatedTypeVariable, AnnotatedTypeMirror>();

        ExecutableElement elt;
        if (expr instanceof MethodInvocationTree ||
                expr instanceof NewClassTree) {
            elt = (ExecutableElement) TreeUtils.elementFromUse(expr);
        } else {
            // This case should never happen.
            System.err.println("AnnotatedTypes.findTypeArguments: unexpected tree: " + expr);
            elt = null;
        }

        // Find the un-annotated type
        // TODO: WMD thinks it would be better to (also?) determine the assignment context,
        // instead of just the defaulted return type. For an example, see
        // nullness/generics/MethodTypeVars6.java where an annotation on a type variable
        // gets ignored.
        AnnotatedTypeMirror returnType = factory.type(expr);
        factory.annotateImplicit(expr, returnType);

        AnnotatedExecutableType methodType;

        if (expr instanceof MethodInvocationTree) {
            methodType = asMemberOf(factory.getReceiverType(expr), elt);
        } else if (expr instanceof NewClassTree) {
            // consider the constructor type itself as the viewpoint
            methodType = asMemberOf(returnType, elt);
        } else {
            methodType = null;
        }

        for (TypeParameterElement var : elt.getTypeParameters()) {
            // Find the un-annotated binding for the type variable
            AnnotatedTypeVariable typeVar = (AnnotatedTypeVariable) factory.getAnnotatedType(var);
            AnnotatedTypeMirror returnTypeBase;

            AnnotatedTypeMirror argument =
                inferTypeArgsUsingArgs(typeVar, returnType, methodType, expr);

            if (argument == null) {
                // Using assignment context
                assert factory.root != null : "root needs to be set when used on trees";
                AnnotatedTypeMirror assigned =
                    assignedTo(TreePath.getPath(factory.root, expr));
                if (assigned != null) {
                    AnnotatedTypeMirror rettype = methodType.getReturnType();
                    returnTypeBase = asSuper(rettype, assigned);
                    List<AnnotatedTypeMirror> lst =
                        new TypeResolutionFinder(typeVar).visit(returnTypeBase, assigned);

                    if (lst != null && !lst.isEmpty()) {
                        argument = lst.get(0);
                    } else {
                        if (rettype instanceof AnnotatedTypeVariable) {
                            AnnotatedTypeVariable atvrettype = (AnnotatedTypeVariable) rettype;
                            if (atvrettype.getUnderlyingType().asElement() == var) {
                                // Special case if the return type is the type variable we are looking at
                                if (!factory.qualHierarchy.isSubtype(assigned.getAnnotations(),
                                        rettype.getEffectiveAnnotations())) {
                                    // If the assignment context is not a subtype of the upper bound of the
                                    // return type, take the type qualifiers from the upper bound.
                                    // If the assignment type and bound type are incompatible, we'll get an
                                    // error later. Most likely the assignment type is simply a supertype of
                                    // the bound, e.g. because of the non-null except locals default.
                                    assigned = deepCopy(assigned);
                                    assigned.clearAnnotations();
                                    assigned.addAnnotations(rettype.getEffectiveAnnotations());
                                }
                                argument = assigned;
                            }
                        }
                    }
                }
            }

            if (argument == null) {
                AnnotatedTypeMirror upperBound = typeVar.getEffectiveUpperBound();
                while (upperBound.getKind() == TypeKind.TYPEVAR)
                    upperBound = ((AnnotatedTypeVariable)upperBound).getEffectiveUpperBound();
                WildcardType wc = env.getTypeUtils().getWildcardType(upperBound.getUnderlyingType(), null);
                AnnotatedWildcardType wctype = (AnnotatedWildcardType) AnnotatedTypeMirror.createType(wc, env, factory);
                wctype.setElement(typeVar.getElement());
                wctype.setExtendsBound(upperBound);
                wctype.addAnnotations(typeVar.getAnnotations());
                wctype.setMethodTypeArgHack();

                argument = wctype;
            }

            if (argument != null)
                typeArguments.put(typeVar, argument);
        }

        return typeArguments;
    }

    /**
     * Infer the type argument for a single type variable.
     *
     * @param typeVar the method or constructor type variable to infer
     * @param returnType the return type
     * @param exeType the executable type of the method or constructor
     * @param expr the method or constructor invocation tree; the passed argument
     *   has to be a subtype of MethodInvocationTree or NewClassTree.
     * @return the type argument
     */
    private AnnotatedTypeMirror inferTypeArgsUsingArgs(AnnotatedTypeVariable typeVar,
            AnnotatedTypeMirror returnType, AnnotatedExecutableType exeType,
            ExpressionTree expr) {
        TypeResolutionFinder finder = new TypeResolutionFinder(typeVar);
        List<AnnotatedTypeMirror> lubForVar = finder.visit(exeType.getReturnType(), returnType);

        // TODO: This may introduce a bug, but I don't want to deal with it right now
        if (lubForVar.isEmpty()) {
            return null;
        }

        List<? extends ExpressionTree> args;
        if (expr instanceof MethodInvocationTree) {
            args = ((MethodInvocationTree) expr).getArguments();
        } else if (expr instanceof NewClassTree) {
            args = ((NewClassTree) expr).getArguments();
        } else {
            // TODO
            args = null;
        }

        // find parameter arguments beneficial for inference
        List<AnnotatedTypeMirror> requiredParams = expandVarArgs(exeType, args);
        List<AnnotatedTypeMirror> passedArgs = new ArrayList<AnnotatedTypeMirror>();

        for (int i = 0; i < requiredParams.size(); ++i) {
            AnnotatedTypeMirror passedArg = factory.getAnnotatedType(args.get(i));
            AnnotatedTypeMirror requiredArg = requiredParams.get(i);
            if (asSuper(passedArg, requiredArg) != null)
                passedArg = asSuper(passedArg, requiredArg);
            passedArgs.addAll(finder.visit(requiredArg, passedArg));
        }
        if (passedArgs.isEmpty())
            return null;

        // Found arguments! Great!
        annotateAsLub(lubForVar.get(0), passedArgs);
        return lubForVar.get(0);
    }

    private class TypeResolutionFinder
    extends SimpleAnnotatedTypeVisitor<List<AnnotatedTypeMirror>, AnnotatedTypeMirror> {
        private final AnnotatedTypeVariable typeToFind;

        public TypeResolutionFinder(AnnotatedTypeVariable typeToFind) {
            this.typeToFind = typeToFind;
        }

        List<AnnotatedTypeMirror> visit(List<AnnotatedTypeMirror> types,
                List<AnnotatedTypeMirror> other) {
            List<AnnotatedTypeMirror> found = new ArrayList<AnnotatedTypeMirror>();
            assert types.size() == other.size();
            for (int i = 0; i < types.size(); ++i) {
                List<AnnotatedTypeMirror> foundHere = visit(types.get(i), other.get(i));
                found.addAll(foundHere);
            }
            return found;
        }

        @Override
        public List<AnnotatedTypeMirror>
        visitArray(AnnotatedArrayType type, AnnotatedTypeMirror p) {
            if (p.getKind() == TypeKind.NULL) {
                return Collections.emptyList();
            } else if (p.getKind() == TypeKind.WILDCARD) {
                // WMD was inspired for this test by visitDeclared below.
                // For an array type, the only legal upper bound is java.lang.Object.
                AnnotatedTypeMirror bound = ((AnnotatedWildcardType)p).getExtendsBound();
                if (bound != null) {
                    assert bound.getUnderlyingType().toString().equals("java.lang.Object");
                }
                return Collections.emptyList();
            }
            assert type.getKind() == p.getKind();
            AnnotatedArrayType pArray = (AnnotatedArrayType) p;

            AnnotatedTypeMirror typeToLookIn;
            if (pArray.getComponentType().getKind().isPrimitive())
                typeToLookIn = pArray;
            else
                typeToLookIn = pArray.getComponentType();

            return visit(type.getComponentType(), typeToLookIn);
        }

        @Override
        public List<AnnotatedTypeMirror>
        visitDeclared(AnnotatedDeclaredType type, AnnotatedTypeMirror p) {
            if (p.getKind() == TypeKind.NULL) {
                if (type.getKind() == TypeKind.TYPEVAR) {
                    Element elem = type.getUnderlyingType().asElement();
                    if (elem.equals(typeToFind.getUnderlyingType().asElement())) {
                        // If we pass null as argument, it only has an influence, if the declared
                        // parameter type is the typeToFind type variable.
                        // E.g. the declared type might not contain the type variable at all.
                        // Also, if we have the parameter type "List<T>" and pass null, this should
                        // not have an effect on the type of T.
                        return Collections.singletonList(p);
                    }
                }
                return Collections.emptyList();
            } else if (p.getKind() == TypeKind.WILDCARD) {
                AnnotatedTypeMirror bound = ((AnnotatedWildcardType)p).getExtendsBound();
                if (bound == null) return Collections.emptyList();
                else return visitDeclared(type, bound);
            }
            AnnotatedDeclaredType pDeclared = (AnnotatedDeclaredType) asSuper(p, type);
            // if one of them is erased do nothing
            if (type.getTypeArguments().isEmpty()
                || pDeclared == null || pDeclared.getTypeArguments().isEmpty())
                return Collections.emptyList();
            return visit(type.getTypeArguments(), pDeclared.getTypeArguments());
        }

        @Override
        public List<AnnotatedTypeMirror>
        visitExecutable(AnnotatedExecutableType type, AnnotatedTypeMirror p) {
            assert type.getKind() == p.getKind();
            AnnotatedExecutableType pExecutable = (AnnotatedExecutableType)p;
            // Do do return nor type variables
            return visit(type.getParameterTypes(), pExecutable.getParameterTypes());
        }

        @Override
        public List<AnnotatedTypeMirror>
        visitTypeVariable(AnnotatedTypeVariable type, AnnotatedTypeMirror p) {
            Element elem = type.getUnderlyingType().asElement();
            if (elem.equals(typeToFind.getUnderlyingType().asElement())) {
                return Collections.singletonList(p);
            }
            return Collections.emptyList();
        }

        @Override
        public List<AnnotatedTypeMirror>
        visitWildcard(AnnotatedWildcardType type, AnnotatedTypeMirror p) {
            List<AnnotatedTypeMirror> types = new ArrayList<AnnotatedTypeMirror>();
            if (type.getExtendsBound() != null)
                types.addAll(visit(type.getExtendsBound(), p));
            if (type.getSuperBound() != null)
                types.addAll(visit(type.getSuperBound(), p));
            return types;
        }

        @Override
        public List<AnnotatedTypeMirror>
        defaultAction(AnnotatedTypeMirror type, AnnotatedTypeMirror p) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns the annotated type that the leaf of path is assigned to, if it
     * is within an assignment context
     * Returns the annotated type that the method invocation at the leaf
     * is assigned to.
     *
     * @param path
     * @return type that it path leaf is assigned to
     */
    public AnnotatedTypeMirror assignedTo(TreePath path) {
        Tree assignmentContext = TreeUtils.getAssignmentContext(path);
        if (assignmentContext == null) {
            return null;
        } else if (assignmentContext instanceof AssignmentTree) {
            ExpressionTree variable = ((AssignmentTree)assignmentContext).getVariable();
            return factory.getAnnotatedType(variable);
        } else if (assignmentContext instanceof CompoundAssignmentTree) {
            ExpressionTree variable =
                ((CompoundAssignmentTree)assignmentContext).getVariable();
            return factory.getAnnotatedType(variable);
        } else if (assignmentContext instanceof MethodInvocationTree) {
            MethodInvocationTree methodInvocation = (MethodInvocationTree)assignmentContext;
            // TODO move to getAssignmentContext
            if (methodInvocation.getMethodSelect() instanceof MemberSelectTree
                    && ((MemberSelectTree)methodInvocation.getMethodSelect()).getExpression() == path.getLeaf())
                return null;
            ExecutableElement methodElt = TreeUtils.elementFromUse(methodInvocation);
            AnnotatedTypeMirror receiver = factory.getReceiverType(methodInvocation);
            AnnotatedExecutableType method = asMemberOf(receiver, methodElt);
            int treeIndex = -1;
            for (int i = 0; i < method.getParameterTypes().size(); ++i) {
                if (TreeUtils.skipParens(methodInvocation.getArguments().get(i)) == path.getLeaf()) {
                    treeIndex = i;
                    break;
                }
            }
            if (treeIndex == -1) return null;
            return method.getParameterTypes().get(treeIndex);
        } else if (assignmentContext instanceof NewArrayTree) {
            // FIXME: This may cause infinite loop
            AnnotatedTypeMirror type =
                factory.getAnnotatedType((NewArrayTree)assignmentContext);
            type = AnnotatedTypes.innerMostType(type);
            return type;
        } else if (assignmentContext instanceof NewClassTree) {
            // This need to be basically like MethodTree
            NewClassTree newClassTree = (NewClassTree) assignmentContext;
            ExecutableElement constructorElt = InternalUtils.constructor(newClassTree);
            AnnotatedExecutableType constructor =
                factory.getAnnotatedType(constructorElt);
            int treeIndex = -1;
            for (int i = 0; i < constructor.getParameterTypes().size(); ++i) {
                if (TreeUtils.skipParens(newClassTree.getArguments().get(i)) == path.getLeaf()) {
                    treeIndex = i;
                    break;
                }
            }
            if (treeIndex == -1) return null;
            return constructor.getParameterTypes().get(treeIndex);
        } else if (assignmentContext instanceof ReturnTree) {
            MethodTree method = TreeUtils.enclosingMethod(path);
            return (factory.getAnnotatedType(method)).getReturnType();
        } else if (assignmentContext instanceof VariableTree) {
            return factory.getAnnotatedType((VariableTree)assignmentContext);
        }

        SourceChecker.errorAbort("AnnotatedTypes.assignedTo: shouldn't be here!");
        return null; // dead code
    }

    /**
     * Determines if the type is for an anonymous type or not
     *
     * @param type  type to be checked
     * @return  true iff type is an anonymous type
     */
    public boolean isAnonymousType(AnnotatedTypeMirror type) {
        return TypesUtils.isAnonymousType(type.getUnderlyingType());
    }

    /**
     * Determines if the type is for an intersect type or not
     *
     * @param type  type to be checked
     * @return  true iff type is an intersect type
     */
    public boolean isIntersectType(AnnotatedTypeMirror type) {
        return isAnonymousType(type) &&
                type.getUnderlyingType().toString().contains("&");
    }

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
    public void annotateAsLub(AnnotatedTypeMirror lub, Collection<AnnotatedTypeMirror> types) {
        // Is it anonymous?
        if (isAnonymousType(lub)) {
            // Find the intersect types
            AnnotatedDeclaredType adt = (AnnotatedDeclaredType)lub;

            for (AnnotatedDeclaredType adts : adt.directSuperTypes()) {
                List<AnnotatedTypeMirror> subtypes = new ArrayList<AnnotatedTypeMirror>(types.size());
                for (AnnotatedTypeMirror type : types) {
                    AnnotatedTypeMirror sup = asSuper(type, adts);
                    if (sup!=null) {
                        subtypes.add(sup);
                    }
                }
                if (subtypes.size() > 0) {
                    adts.clearAnnotations();
                }

                addAnnotations(adts, subtypes.toArray(new AnnotatedTypeMirror[0]));
                addAnnotations(lub, adts);
            }
        } else {
            List<AnnotatedTypeMirror> subtypes = new ArrayList<AnnotatedTypeMirror>(types.size());

            for (AnnotatedTypeMirror type : types) {
                if (type == null) {
                    continue;
                }
                if (type.getKind() == TypeKind.WILDCARD &&
                        ((AnnotatedWildcardType)type).getSuperBound() != null) {
                    type = ((AnnotatedWildcardType)type).getSuperBound();
                }
                if (type.getKind() == TypeKind.WILDCARD) {
                    subtypes.add(deepCopy(lub));
                } else if (asSuper(type, lub) == null) {
                    subtypes.add(deepCopy(lub));
                } else {
                    subtypes.add(asSuper(type, lub));
                }
            }
            if (subtypes.size() > 0) {
                lub.clearAnnotations();
            }

            addAnnotations(lub, subtypes.toArray(new AnnotatedTypeMirror[0]));
        }
    }

    /**
     * Add the 'intersection' of the types provided to alub.  This is a similar
     * method to the one provided
     */
    private void addAnnotations(AnnotatedTypeMirror alub,
            AnnotatedTypeMirror ...types) {
        Set<TypeMirror> visited = new HashSet<TypeMirror>();
        addAnnotationsImpl(alub, visited, types);
    }

    private void addAnnotationsImpl(AnnotatedTypeMirror alub,
            Set<TypeMirror> visited,
            AnnotatedTypeMirror ...types) {
        // System.out.println("AnnotatedTypes.addAnnotationsImpl: alub: " + alub +
        //        "\n   visited: " + visited +
        //        "\n   types: " + Arrays.toString(types));

        // types may contain a null in the context of unchecked cast
        // TODO: fix this
        boolean isFirst = true;
        // get rid of wildcards and type variables
        if (alub.getKind() == TypeKind.WILDCARD) {
            alub = ((AnnotatedWildcardType)alub).getExtendsBound();
        }
        if (alub.getKind() == TypeKind.TYPEVAR) {
            alub = ((AnnotatedTypeVariable)alub).getUpperBound();
        }

        if (visited.contains(alub.actualType)) {
            return;
        }
        visited.add(alub.actualType);

        for (int i = 0; i < types.length; ++i) {
            if (types[i].getKind() == TypeKind.WILDCARD) {
                AnnotatedWildcardType wildcard = (AnnotatedWildcardType) types[i];
                if (wildcard.getExtendsBound() != null)
                    types[i] = wildcard.getEffectiveExtendsBound();
                else if (wildcard.getSuperBound() != null)
                    types[i] = wildcard.getEffectiveSuperBound();
            }
            if (types[i].getKind() == TypeKind.TYPEVAR) {
                AnnotatedTypeVariable typevar = (AnnotatedTypeVariable) types[i];
                if (typevar.getUpperBound() != null)
                    types[i] = typevar.getEffectiveUpperBound();
                else if (typevar.getLowerBound() != null)
                    types[i] = typevar.getEffectiveLowerBound();
            }
        }

        Collection<AnnotationMirror> unification = Collections.emptySet();

        for (AnnotatedTypeMirror type : types) {
            if (type.getKind() == TypeKind.NULL && !type.isAnnotated()) continue;
            if (type.getAnnotations().isEmpty()) continue;
            // TODO: unification fails with an empty set of annotations.
            // Why are they sometimes empty, e.g. in the FlowNegation test case.

            if (isFirst) {
                unification = type.getAnnotations();
            } else {
                unification = factory.unify(unification, type.getAnnotations());
            }
            isFirst = false;
        }

        // Remove a previously existing unqualified annotation on the type.
        alub.removeUnqualified();
        alub.addAnnotations(unification);

        if (alub.getKind() == TypeKind.DECLARED) {
            AnnotatedDeclaredType adt = (AnnotatedDeclaredType) alub;

            for (int i = 0; i < adt.getTypeArguments().size(); ++i) {
                AnnotatedTypeMirror adtArg = adt.getTypeArguments().get(i);
                List<AnnotatedTypeMirror> dTypesArg = new ArrayList<AnnotatedTypeMirror>();
                for (int j = 0; j < types.length; ++j) {
                    if (types[j].getKind() == TypeKind.DECLARED) {
                        AnnotatedDeclaredType adtypej = (AnnotatedDeclaredType) types[j];
                        if (adtypej.getTypeArguments().size() == adt.getTypeArguments().size()) {
                            dTypesArg.add(adtypej.getTypeArguments().get(i));
                        } else {
                            // TODO: actually not just the number of type arguments should match, but
                            // the base types should be equal. See test case framework/GenericTest1
                            // for when this test fails.
                        }
                    }
                }
                addAnnotationsImpl(adtArg, visited, dTypesArg.toArray(new AnnotatedTypeMirror[0]));
            }
        } else if (alub.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType aat = (AnnotatedArrayType) alub;
            List<AnnotatedTypeMirror> compTypes = new ArrayList<AnnotatedTypeMirror>();
            for (int i = 0; i < types.length; ++i)  {
                if (types[i].getKind() != TypeKind.NULL) {
                    compTypes.add(((AnnotatedArrayType)types[i]).getComponentType());
                }
            }
            addAnnotationsImpl(aat.getComponentType(), visited, compTypes.toArray(new AnnotatedTypeMirror[0]));
        }
    }

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
    public List<AnnotatedTypeMirror> expandVarArgs(AnnotatedExecutableType method,
            List<? extends ExpressionTree> args) {
        List<AnnotatedTypeMirror> parameters = method.getParameterTypes();
        if (!method.getElement().isVarArgs())
            return parameters;

        AnnotatedArrayType varargs = (AnnotatedArrayType)parameters.get(parameters.size() - 1);

        if (parameters.size() == args.size()) {
            // Check if one sent an element or an array
            AnnotatedTypeMirror lastArg = factory.getAnnotatedType(args.get(args.size() - 1));
            if (lastArg.getKind() == TypeKind.ARRAY &&
                    getArrayDepth(varargs) == getArrayDepth((AnnotatedArrayType)lastArg))
                return parameters;
        }

        parameters = new ArrayList<AnnotatedTypeMirror>(parameters.subList(0, parameters.size() - 1));
        for (int i = args.size() - parameters.size(); i > 0; --i)
            parameters.add(varargs.getComponentType());

        return parameters;
    }

    /**
     * Return a list of the AnnotatedTypeMirror of the passed
     * expression trees, in the same order as the trees.
     *
     * @param trees the AST nodes
     * @return  a list with the AnnotatedTypeMirror of each tree in trees.
     */
    public List<AnnotatedTypeMirror> getAnnotatedTypes(
            Iterable<? extends ExpressionTree> trees) {
        List<AnnotatedTypeMirror> types =
            new ArrayList<AnnotatedTypeMirror>();

        for (ExpressionTree tree : trees)
            types.add(factory.getAnnotatedType(tree));

        return types;
    }

    public boolean areSame(AnnotatedTypeMirror t1, AnnotatedTypeMirror t2) {
        return t1.toString().equals(t2.toString());
    }

    /**
     * Returns the depth of the array type of the provided array.
     *
     * @param array the type of the array
     * @return  the depth of the provided array
     */
    public int getArrayDepth(AnnotatedArrayType array) {
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
                if (atv.getUpperBound()!=null) {
                    found = containsModifierImpl(atv.getUpperBound(), modifier, visited);
                }
                if (!found && atv.getLowerBound()!=null) {
                    found = containsModifierImpl(atv.getLowerBound(), modifier, visited);
                }
            } else if (type.getKind() == TypeKind.WILDCARD) {
                AnnotatedWildcardType awc = (AnnotatedWildcardType) type;
                if (awc.getExtendsBound()!=null) {
                    found = containsModifierImpl(awc.getExtendsBound(), modifier, visited);
                }
                if (!found && awc.getSuperBound()!=null) {
                    found = containsModifierImpl(awc.getSuperBound(), modifier, visited);
                }
            }
        }

        return found;
    }


    private static Map<TypeElement, Boolean> isTypeAnnotationCache = new IdentityHashMap<TypeElement, Boolean>();

    public static boolean isTypeAnnotation(AnnotationMirror anno) {
        TypeElement elem = (TypeElement)anno.getAnnotationType().asElement();
        if (isTypeAnnotationCache.containsKey(elem))
            return isTypeAnnotationCache.get(elem);

        boolean result = isTypeAnnotationImpl(elem);
        isTypeAnnotationCache.put(elem, result);
        return result;
    }

    private static boolean isTypeAnnotationImpl(TypeElement type) {
        return type.getAnnotation(TypeQualifier.class)!=null;
    }

    public static boolean containsTypeAnnotation(Collection<? extends AnnotationMirror> annos) {
        for(AnnotationMirror am : annos) {
            if(isTypeAnnotation(am)) return true;
        }
        return false;
    }

}
