package checkers.types;

import static javax.lang.model.util.ElementFilter.methodsIn;

import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;

import checkers.types.AnnotatedTypeMirror.*;
import checkers.types.visitors.SimpleAnnotatedTypeVisitor;
import checkers.util.ElementUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;
import checkers.util.TypesUtils;
import checkers.nullness.quals.*;

/**
 * Utility methods for operating on {@code AnnotatedTypeMirror}. This
 * class mimics the class {@link Types}.
 */
public class AnnotatedTypes {

    private ProcessingEnvironment env;
    private AnnotatedTypeFactory factory;

    /**
     * Constructor for {@code AnnotatedTypeUtils}
     *
     * @param env  the processing environment for this round
     */
    public AnnotatedTypes(ProcessingEnvironment env, AnnotatedTypeFactory factory) {
        this.env = env;
        this.factory = factory;
    }

    /**
     * Returns the most specific base type of {@code t} whose erasure type
     * is {@code superType}.  It returns null if {@code t} is not a subtype
     * of {@code superType}.
     *
     * @param t      a type
     * @param superType   a type that is a supertype of {@code type}
     * @return the base type of t of the given element
     */
    public AnnotatedTypeMirror asSuper(AnnotatedTypeMirror t,
            AnnotatedTypeMirror superType) {
        return asSuper.visit(t, superType);
    }

    private SimpleAnnotatedTypeVisitor<AnnotatedTypeMirror, AnnotatedTypeMirror> asSuper =
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
            // Operate on the upper bound
            return asSuper(type.getUpperBound(), p);
        }

        @Override
        public AnnotatedTypeMirror visitWildcard(AnnotatedWildcardType type, AnnotatedTypeMirror p) {
            if (p.getKind() == TypeKind.WILDCARD)
                return type;
            // Operate on the upper bound
            return asSuper(type.getExtendsBound(), p);
        }


        @Override
        public AnnotatedTypeMirror visitArray(AnnotatedArrayType type, AnnotatedTypeMirror p) {
            // Check if array component is subtype of the element
            // first
            if (shouldStop(p, type))
                return type;
            for (AnnotatedTypeMirror st : type.directSuperTypes()) {
                AnnotatedTypeMirror x = asSuper(st, p);
                if (x != null)
                    return isErased(x, p) ? x.getErased() : x;
            }
            return null;
        }

        @Override
        public AnnotatedTypeMirror visitDeclared(AnnotatedDeclaredType type, AnnotatedTypeMirror p) {
            // If visited Element is the desired one, we are done
            if (p.getKind().isPrimitive())
                return visit(factory.getUnboxedType(type), p);

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
     * Helper method that decides whether sup and sub are the same type or that
     * sub cannot be a subtype of sup.
     */
    private boolean shouldStop(AnnotatedTypeMirror sup, AnnotatedTypeMirror sub) {
        // Check if it's the same type
        // if sup is primitive, but not sub
        if (sup.getKind().isPrimitive() && !sub.getKind().isPrimitive())
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
        // horroble horroble hack
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
        return (AnnotatedExecutableType)asMemberOf(t,(Element)elem);
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
        // as Member of is only for fields, variables and methods!
        // otherwise simply use fromElement
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

    private AnnotatedTypeMirror asMemberOfImpl(AnnotatedTypeMirror t, Element elem) {
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

        // I cannot think of why it wouldn't be a declared type!
        // Defensive Programming
        if (t.getKind() != TypeKind.DECLARED) {
            return factory.getAnnotatedType(elem);
        }

        //
        // Basic Algorithm:
        // 1. Find the owner of the element
        // 2. Find the base type of owner (e.g. type of owner as supertype
        //      of passed type)
        // 3. Subsitute for type variables if any exist
        TypeElement owner = ElementUtils.enclosingClass(elem);

        // TODO: Potential bug if Raw type is used
        if (ElementUtils.isStatic(elem) || owner.getTypeParameters().isEmpty())
            return factory.getAnnotatedType(elem);

        AnnotatedDeclaredType ownerType = factory.getAnnotatedType(owner);
        AnnotatedDeclaredType base =
            (AnnotatedDeclaredType) asOuterSuper(t, ownerType);

        if (base == null)
            return factory.getAnnotatedType(elem);

        List<? extends AnnotatedTypeMirror> ownerParams =
            ownerType.getTypeArguments();
        List<? extends AnnotatedTypeMirror> baseParams =
            base.getTypeArguments();
        if (!ownerParams.isEmpty()) {
            if (baseParams.isEmpty()) {
                List<AnnotatedTypeMirror> baseParamsEr = new ArrayList<AnnotatedTypeMirror>();
                for (AnnotatedTypeMirror arg : ownerParams)
                    baseParamsEr.add(arg.getErased());
                return subst(factory.getAnnotatedType(elem), ownerParams, baseParamsEr);
            }
            return subst(factory.getAnnotatedType(elem), ownerParams, baseParams);
        }

        return factory.getAnnotatedType(elem);
    }

    /**
     * Returns a new type, a copy of the passed {@code t}, with all
     * instances of {@code from} type substituted with their correspondents
     * in {@code to} and return the substituted in type.
     *
     * @param t     the type
     * @param from  the from types
     * @param to    the to types
     * @return  the new type after substitutions
     */
    public AnnotatedTypeMirror subst(AnnotatedTypeMirror t,
            List<? extends AnnotatedTypeMirror> from,
            List<? extends AnnotatedTypeMirror> to) {
        assert from.size() == to.size();
        Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mappings =
            new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>();

        for (int i = 0; i < from.size(); ++i) {
            mappings.put(from.get(i), to.get(i));
        }
        return t.substitute(mappings);
    }


    /**
     * Returns a deep copy of the passed type
     *
     * @param type  the annotated type to be copied
     * @return a deep copy of the passed type
     */
    public AnnotatedTypeMirror deepCopy(AnnotatedTypeMirror type) {
        // TODO: Test this, specify behaviour
        return type.substitute(Collections.<AnnotatedTypeMirror,
                AnnotatedTypeMirror>emptyMap());
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

        if (iterableType.getKind() != TypeKind.DECLARED)
            throw new IllegalArgumentException("Not iterable type: " + iterableType);

        TypeElement iterableElement = env.getElementUtils().getTypeElement("java.lang.Iterable");
        AnnotatedDeclaredType iterableElmType = factory.getAnnotatedType(iterableElement);
        AnnotatedDeclaredType dt = (AnnotatedDeclaredType) asSuper(iterableType, iterableElmType);
        if (dt == null)
            throw new IllegalArgumentException("Not iterable type: " + iterableType);
        else if (dt.getTypeArguments().isEmpty()) {
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
     *            overriden by {@code method}
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
            for (ExecutableElement supermethod : methodsIn(superElement
                    .getEnclosedElements())) {
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
     * Given a method method invocation, it returns a mapping of the type variables
     * to their parameters if any exist.
     *
     * It uses the method invocation type parameters if specified, otherwise
     * it infers them based the passed arguments or the return type context,
     * according to JLS 15.12.2.
     *
     * @param methodInvocation  the method invocation tree
     * @return  the mapping of the type variables for this method invocation
     */
    public Map<AnnotatedTypeVariable, AnnotatedTypeMirror>
    findTypeArguments(MethodInvocationTree methodInvocation) {
        Map<AnnotatedTypeVariable, AnnotatedTypeMirror> typeArguments =
            new HashMap<AnnotatedTypeVariable, AnnotatedTypeMirror>();

        ExecutableElement methodElt = TreeUtils.elementFromUse(methodInvocation);
        // Is the method a generic method?
        if (methodElt.getTypeParameters().isEmpty())
            return typeArguments;

        // Has the user supplied type arguments?
        if (!methodInvocation.getTypeArguments().isEmpty()) {
            for (int i = 0; i < methodElt.getTypeParameters().size(); ++i) {
                AnnotatedTypeVariable typeVar = (AnnotatedTypeVariable)
                factory.getAnnotatedType(methodElt.getTypeParameters().get(i));
                AnnotatedTypeMirror typeArg =
                    factory.getAnnotatedTypeFromTypeTree(methodInvocation.getTypeArguments().get(i));
                typeArguments.put(typeVar, typeArg);
            }
            return typeArguments;
        }

        return inferTypeArguments(methodInvocation);
    }

    /**
     * It uses the method invocation type parameters if specified, otherwise
     * it infers them based the passed arguments or the return type context,
     * according to JLS 15.12.2.
     */
    // Note that this implementation is buggy as it only infers arguments
    // that make it to the return type.  So it would fail for invocations to
    // <T> void test(T arg1, T arg2)
    // in such cases, T is infered to be '? extends T.upperBound'
    private Map<AnnotatedTypeVariable, AnnotatedTypeMirror>
    inferTypeArguments(MethodInvocationTree methodInvocation) {
        //
        // The basic algorithm used here, for each type variable:
        // 1. Find the un-annotated  least upper bound for the type variable
        //  by finding the type bound to variable within the return type
        // 2. Infer the type argument annotations using the passed parameters
        // 3. If the type variable is not used within a parameter, find
        //    the assignment context and infer the type argument using it
        // 4. if not within an assignment context, then bind it to the extend bound.
        Map<AnnotatedTypeVariable, AnnotatedTypeMirror> typeArguments =
            new HashMap<AnnotatedTypeVariable, AnnotatedTypeMirror>();

        ExecutableElement methodElt = TreeUtils.elementFromUse(methodInvocation);

        // Find the un-annotated type
        AnnotatedTypeMirror returnType = factory.type(methodInvocation);
        AnnotatedExecutableType methodType =
            asMemberOf(factory.getReceiver(methodInvocation), methodElt);

        for (TypeParameterElement var : methodElt.getTypeParameters()) {
            // Find the un-annotated binding for the type variable
            AnnotatedTypeVariable typeVar = (AnnotatedTypeVariable) factory.getAnnotatedType(var);

            AnnotatedTypeMirror argument =
                inferTypeArgUsingParams(typeVar, returnType, methodType, methodInvocation);

            if (argument == null) {
                // Using assignment context
                assert factory.root != null : "root needs to be set when used on trees";
                AnnotatedTypeMirror assigned =
                    assignedTo(TreePath.getPath(factory.root, methodInvocation));
                if (assigned != null) {
                    AnnotatedTypeMirror returnTypeBase = asSuper(methodType.getReturnType(), assigned);
                    List<AnnotatedTypeMirror> lst =
                        new TypeResolutionFinder(typeVar).visit(returnTypeBase, assigned);
                    if (lst != null && !lst.isEmpty())
                        argument = lst.get(0);
                }
            }

            if (argument == null) {
                // should really be '? extends typeVar.getUpperBound()'
                AnnotatedTypeMirror upperBound = typeVar.getUpperBound();
                while (upperBound.getKind() == TypeKind.TYPEVAR)
                    upperBound = ((AnnotatedTypeVariable)upperBound).getUpperBound();
                WildcardType wc = env.getTypeUtils().getWildcardType(upperBound.getUnderlyingType(), null);
                @SuppressWarnings("deprecation")
                AnnotatedWildcardType wctype = new AnnotatedWildcardType(wc, env, factory);
                wctype.setElement(typeVar.getElement());
                wctype.setExtendsBound(upperBound);
                argument = wctype;
            }

            if (argument != null)
                typeArguments.put(typeVar, argument);
        }
        return typeArguments;
    }

    private AnnotatedTypeMirror inferTypeArgUsingParams(AnnotatedTypeVariable typeVar,
            AnnotatedTypeMirror returnType, AnnotatedExecutableType methodType,
            MethodInvocationTree methodInvocation) {
        TypeResolutionFinder finder = new TypeResolutionFinder(typeVar);
        List<AnnotatedTypeMirror> lubForVar = finder.visit(methodType.getReturnType(), returnType);

        // This may introduce a bug, but I don't want to deal with it right now
        if (lubForVar.isEmpty())
            return null;

        // find parameter arguments beneficial for inference
        List<AnnotatedTypeMirror> requiredParams =
            expandVarArgs(methodType, methodInvocation.getArguments());
        List<AnnotatedTypeMirror> passedArgs = new ArrayList<AnnotatedTypeMirror>();

        for (int i = 0; i < requiredParams.size(); ++i) {
            AnnotatedTypeMirror passedArg = factory.getAnnotatedType(
                    methodInvocation.getArguments().get(i));
            AnnotatedTypeMirror requiredArg = requiredParams.get(i);
            if (asSuper(passedArg, requiredArg) != null)
                passedArg = asSuper(passedArg, requiredArg);
            passedArgs.addAll(finder.visit(requiredArg, passedArg));
        }
        if (passedArgs.isEmpty())
            return null;
        // Found arguments! Great!
        AnnotatedTypeMirror[] argsArray = passedArgs.toArray(new AnnotatedTypeMirror[0]);
        annotateAsLub(lubForVar.get(0), argsArray);
        return lubForVar.get(0);
    }

    private class TypeResolutionFinder
    extends SimpleAnnotatedTypeVisitor<List<AnnotatedTypeMirror>, AnnotatedTypeMirror> {
        private AnnotatedTypeVariable typeToFind;

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
            if (p.getKind() == TypeKind.NULL)
                return Collections.emptyList();
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
                return Collections.singletonList(p);
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
                if (p.getKind() == TypeKind.TYPEVAR
                    && !p.isAnnotated()) {
                    return Collections.singletonList((AnnotatedTypeMirror)type);
                } else {
                    return Collections.singletonList(p);
                }
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
                ((CompoundAssignmentTree)assignmentContext).getExpression();
            return factory.getAnnotatedType(variable);
        } else if (assignmentContext instanceof MethodInvocationTree) {
            MethodInvocationTree methodInvocation = (MethodInvocationTree)assignmentContext;
            // TODO move to getAssignmentContext
            if (methodInvocation.getMethodSelect() instanceof MemberSelectTree
                    && ((MemberSelectTree)methodInvocation.getMethodSelect()).getExpression() == path.getLeaf())
                return null;
            ExecutableElement methodElt = TreeUtils.elementFromUse(methodInvocation);
            AnnotatedTypeMirror receiver = factory.getReceiver(methodInvocation);
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
            while (type.getKind() == TypeKind.ARRAY)
                type = ((AnnotatedArrayType)type).getComponentType();
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

        throw new AssertionError("Shouldn't be here!");
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
     * the types.  This is a useful method for the finding conditional expression
     * types.
     *
     * All the types need to be subtypes of lub.
     *
     * @param lub   the type to be the least upper bound
     * @param types the type arguments
     */
    public void annotateAsLub(AnnotatedTypeMirror lub,
            AnnotatedTypeMirror ...types) {
        // It it anonoymous
        if (isAnonymousType(lub)) {
            // Find the intersect types
            AnnotatedDeclaredType adt = (AnnotatedDeclaredType)lub;

            for (AnnotatedDeclaredType adts : adt.directSuperTypes()) {
                AnnotatedTypeMirror[] subtypes = new AnnotatedTypeMirror[types.length];
                for (int i = 0; i < types.length; ++i) {
                    subtypes[i] = asSuper(types[i], adts);
                }

                addAnnotations(adts, subtypes);
                this.addAnnotations(lub, adts);
            }
        } else {
            AnnotatedTypeMirror[] subtypes = new AnnotatedTypeMirror[types.length];
            for (int i = 0; i < types.length; ++i) {
                AnnotatedTypeMirror type = types[i];
                if (type.getKind() == TypeKind.WILDCARD &&
                        ((AnnotatedWildcardType)type).getSuperBound() != null)
                    type = ((AnnotatedWildcardType)type).getSuperBound();
                if (type == null)
                    return;
                if (type.getKind() == TypeKind.WILDCARD)
                    subtypes[i] = lub;
                else
                    subtypes[i] = asSuper(type, lub);
            }
            if (subtypes.length > 0)
                lub.clearAnnotations();
            addAnnotations(lub, subtypes);
        }
    }

    /**
     * Add the 'intersection' of the types provided to alub.  This is a similar
     * method to the one provided
     */
    private void addAnnotations(AnnotatedTypeMirror alub,
            AnnotatedTypeMirror ...types) {

        // types may contain a null in the context of unchecked cast
        // TODO: fix this
        boolean isFirst = true;
        // get rid of wildcards
        if (alub.getKind() == TypeKind.WILDCARD)
            alub = ((AnnotatedWildcardType)alub).getExtendsBound();
        for (int i = 0; i < types.length; ++i) {
            if (types[i] == null)
                continue;     // TODO: fix this
            if (types[i].getKind() == TypeKind.WILDCARD) {
                AnnotatedWildcardType wildcard = (AnnotatedWildcardType)types[i];
                if (wildcard.getExtendsBound() != null)
                    types[i] = wildcard.getExtendsBound();
                else if (wildcard.getSuperBound() != null)
                    types[i] = wildcard.getSuperBound();
            }
        }

        Collection<AnnotationMirror> unification = Collections.emptySet();
        for (AnnotatedTypeMirror type : types) {
            if (type == null)
                continue;    // TODO: fix this
            if (type.getKind() == TypeKind.NULL && !type.isAnnotated()) continue;
            if (isFirst)
                unification = type.getAnnotations();
            else
                unification = factory.unify(unification, type.getAnnotations());
            isFirst = false;
        }

        alub.addAnnotations(unification);

        if (alub.getKind() == TypeKind.DECLARED) {
            AnnotatedDeclaredType adt = (AnnotatedDeclaredType) alub;

            for (int i = 0; i < adt.getTypeArguments().size(); ++i) {
                AnnotatedTypeMirror adtArg = adt.getTypeArguments().get(i);
                AnnotatedTypeMirror[] dTypesArg = new AnnotatedTypeMirror[types.length];
                for (int j = 0; j < types.length; ++j) {
                    if (types[j].getKind() == TypeKind.NULL)
                        dTypesArg[j] = types[j];
                    else
                        dTypesArg[j] = ((AnnotatedDeclaredType)types[j]).getTypeArguments().get(i);
                }
                addAnnotations(adtArg, dTypesArg);
            }
        } else if (alub.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType aat = (AnnotatedArrayType) alub;
            AnnotatedTypeMirror[] compTypes = new AnnotatedTypeMirror[types.length];
            for (int i = 0; i < types.length; ++i)  {
                if (types[i].getKind() == TypeKind.NULL)
                    compTypes[i] = types[i];
                else
                    compTypes[i] = ((AnnotatedArrayType)types[i]).getComponentType();
            }
            addAnnotations(aat.getComponentType(), compTypes);
        }
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
}
