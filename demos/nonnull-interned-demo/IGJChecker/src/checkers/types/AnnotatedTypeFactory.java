package checkers.types;

import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

import checkers.nullness.quals.*;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import checkers.types.AnnotatedTypeMirror.AnnotatedTypeVariable;
import checkers.util.AnnotatedTypes;
import checkers.util.*;
import static checkers.types.TypeFromTree.*;
import static checkers.types.AnnotatedTypeMirror.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.JCNewArray;

import static com.sun.tools.javac.tree.JCTree.*;

import checkers.igj.quals.*;

/**
 * Determines the annotated type (as an {@link AnnotatedTypeMirror}) of an
 * element or AST node.
 *
 * @manual #writing-a-checker How to write a checker plugin
 */
public class AnnotatedTypeFactory {

    /** The {@link Trees} instance to use for tree node pathfinding. */
    protected final Trees trees;

    /** The root of the syntax tree that this factory operates on. */
    protected final CompilationUnitTree root;

    /** The processing environment to use for accessing compiler internals. */
    protected final ProcessingEnvironment env;

    /** The factory to use for creating annotations. */
    protected final AnnotationFactory annotations;

    /** Utility class for working with {@link Element}s. */
    protected final Elements elements;

    /** Utility class for working with {@link TypeMirror}s. */
    protected final Types types;

    /** Utility class for manipulating annotated types. */
    protected final AnnotatedTypes atypes;

    /** the state of the visitor **/
    final protected VisitorState visitorState;

    /** Size of LRU cache **/
    private final static int CACHE_SIZE = 50;

    private final AnnotatedDeclaredType objectType;

    /** Various Caches **/
    private Map<Tree, @Immutable AnnotatedTypeMirror> treeCache = createLRUCache(CACHE_SIZE);
    private Map<Element, AnnotatedTypeMirror> elementCache = createLRUCache(CACHE_SIZE);
    private Map<Element, Tree> elementToTreeCache  = createLRUCache(CACHE_SIZE);

    /**
     * Constructs a factory from the given {@link ProcessingEnvironment}
     * instance and syntax tree root. (These parameters are required so that
     * the factory may conduct the appropriate annotation-gathering analyses on
     * certain tree types.)
     *
     * @param env the {@link ProcessingEnvironment} instance to use
     * @param root the root of the syntax tree that this factory produces
     *            annotated types for
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public AnnotatedTypeFactory(ProcessingEnvironment env, CompilationUnitTree
            root) {

        if (env == null)
            throw new IllegalArgumentException("null env");

        if (root == null)
            throw new IllegalArgumentException("null root");

        this.env = env;
        this.root = root;
        this.trees = Trees.instance(env);
        this.annotations = new AnnotationFactory(env);
        this.elements = env.getElementUtils();
        this.types = env.getTypeUtils();
        this.atypes = new AnnotatedTypes(env, this);
        this.visitorState = new VisitorState();
        this.objectType =
            (AnnotatedDeclaredType) AnnotatedTypeMirror.createType(
                    elements.getTypeElement("java.lang.Object").asType(),
                    env, this);
    }

    /**
     * Return the compilation unit that the visitor is visiting now.
     */
    public CompilationUnitTree getCurrentRoot() {
        return this.root;
    }

    /**
     * Ensures that a type has been constructed properly.
     *
     * @param type the type to check
     */
    private void checkRep(AnnotatedTypeMirror type) {
        new AnnotatedTypeScanner<Void, Void>() {
            @Override
            public Void visitDeclared(AnnotatedDeclaredType type, Void p) {
                //assert type.getElement() != null;
                return super.visitDeclared(type, p);
            }

            @Override
            public Void visitExecutable(AnnotatedExecutableType type, Void p) {
                assert type.getElement() != null;
                assert type.getElement() instanceof ExecutableElement;
                return super.visitExecutable(type, p);
            }

        }.visit(type);
    }

    /**
     * Adds implicit annotations to a type obtained from a {@link Tree}. By
     * default, this method does nothing. Subclasses should use this method to
     * implement implicit annotations specific to their type systems.
     *
     * @param tree an AST node
     * @param type the type obtained from {@code tree}
     */
    protected void annotateImplicit(Tree tree, @Mutable AnnotatedTypeMirror type) {
        // Pass.
    }

    /**
     * Adds implicit annotations to a type obtained from a {@link Element}. By
     * default, this method does nothing. Subclasses should use this method to
     * implement implicit annotations specific to their type systems.
     *
     * @param elt an element
     * @param type the type obtained from {@code elt}
     */
    protected void annotateImplicit(Element elt, @Mutable AnnotatedTypeMirror type) {
        // Pass.
    }

    /**
     * Adds annotations to the type based on the annotations from its class
     * type if and only if no annotations are already present on the type.
     *
     * @param type the type for which class annotations will be inherited if
     * there are no annotations already present
     */
    protected void annotateInheritedFromClass(@Mutable AnnotatedTypeMirror type) {
        InheritedFromClassAnnotator.INSTANCE.scan(type, this);
    }

    /**
     * A singleton utility class for pulling annotations down from a class
     * type.
     *
     * @see #annotateInheritedFromClass
     */
    private static class InheritedFromClassAnnotator
            extends AnnotatedTypeScanner<Void, AnnotatedTypeFactory> {

        /** The singleton instance. */
        private static final InheritedFromClassAnnotator INSTANCE
            = new InheritedFromClassAnnotator();

        private InheritedFromClassAnnotator() {}

        @Override
        public Void visitExecutable(AnnotatedExecutableType type, AnnotatedTypeFactory p) {

            // When visiting an executable type, skip the receiver so we
            // never inherit class annotations there.

            scan(type.getReturnType(), p);
            scanAndReduce(type.getParameterTypes(), p, null);
            scanAndReduce(type.getThrownTypes(), p, null);
            scanAndReduce(type.getTypeVariables(), p, null);
            return null;
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, AnnotatedTypeFactory p) {
            Element classElt = type.getUnderlyingType().asElement();

            // Only add annotations from the class declaration if there
            // are no annotations already on the type.
            //
            // TODO: this conflicts with declaration annotations; the factory
            // currently can't tell e.g. @SuppressWarnings from a legit
            // type qualifier.

            if (classElt != null && type.getAnnotations().isEmpty()) {
                AnnotatedTypeMirror classType = p.fromElement(classElt);
                assert classType != null;
                type.addAnnotations(classType.getAnnotations());
            }

            return super.visitDeclared(type, p);
        }
    }

    /**
     * A convenience method that takes any visitor for converting trees to
     * annotated types, and applies the visitor to the tree, add implicit
     * annotations, etc.
     *
     * @param converter the tree-to-type-converting visitor
     * @param tree the tree to convert
     * @param type the converted annotated type
     */
    private AnnotatedTypeMirror fromTreeWithVisitor(TypeFromTree converter, Tree tree) {
        if (tree == null)
            throw new IllegalArgumentException("null tree");
        if (converter == null)
            throw new IllegalArgumentException("null visitor");
        AnnotatedTypeMirror result = converter.visit(tree, this);
        // annotateImplicit(tree, result);
        checkRep(result);
        return result;
    }


    /**
     * Determines the annotated type of an expression.
     *
     * @param tree an expression
     * @return the annotated type of the expression
     */
    public AnnotatedTypeMirror fromExpression(ExpressionTree tree) {
        AnnotatedTypeMirror result = fromTreeWithVisitor(
                TypeFromExpression.INSTANCE, tree);
        annotateInheritedFromClass(result);
        return result;
    }

    /**
     * Determines the annotated type of a variable or method declaration.
     *
     * @param tree the variable or method declaration
     * @return the annotated type of the variable or method being declared
     * @throws IllegalArgumentException if {@code tree} is not a method or
     * variable declaration
     */
    public AnnotatedTypeMirror fromMember(Tree tree) {
        if (!(tree instanceof MethodTree || tree instanceof VariableTree))
            throw new IllegalArgumentException("not a method or variable declaration");
        AnnotatedTypeMirror result = fromTreeWithVisitor(
                TypeFromMember.INSTANCE, tree);
        annotateInheritedFromClass(result);
        return result;
    }

    /**
     * Determines the annotated type of a class from its declaration.
     *
     * @param tree the class declaration
     * @return the annotated type of the class being declared
     */
    public AnnotatedTypeMirror fromClass(ClassTree tree) {
        return fromTreeWithVisitor(
                TypeFromClass.INSTANCE, tree);
    }

    /**
     * Determines the annotated type from a type in tree form.  This method
     * does not add implicit annotations
     *
     * @param tree the type tree
     * @return the annotated type of the type in the AST
     */
    public AnnotatedTypeMirror fromTypeTree(Tree tree) {
        AnnotatedTypeMirror result = fromTreeWithVisitor(
                TypeFromTypeTree.INSTANCE, tree);
        annotateInheritedFromClass(result);
        return result;
    }

    /**
     * Determines the annotated type of an element.
     *
     * @param elt the element
     * @return the annotated type of the element
     */
    public AnnotatedTypeMirror fromElement(Element elt) {
        Tree decl = declarationFromElement(elt);
        if (decl != null) {
            if (decl instanceof ClassTree)
                return fromClass((ClassTree)decl);
            if (decl instanceof VariableTree)
                return fromMember(decl);
            if (decl instanceof MethodTree)
                return fromMember(decl);
        }

        // TODO type parameters

        AnnotatedTypeMirror type = toAnnotatedType(elt.asType());
        type.addAnnotations(elt.getAnnotationMirrors());
        type.setElement(elt);

        if (elt instanceof ExecutableElement || elt instanceof VariableElement) {
            annotateInheritedFromClass(type);
            // IGJ rely on fromFOO not having implicit annotations
//            annotateImplicit(elt, type);
        }
        return type;
    }

    /**
     * A convenience method that converts a {@link TypeMirror} to an {@link
     * AnnotatedTypeMirror} using {@link AnnotatedTypeMirror#create}.
     *
     * @param t the {@link TypeMirror}
     * @return an {@link AnnotatedTypeMirror} that has {@code t} as its
     * underlying type
     */
    final AnnotatedTypeMirror toAnnotatedType(TypeMirror t) {
        return AnnotatedTypeMirror.createType(t, env, this);
    }

    /**
     * Determines an empty annotated type of the given tree. In other words,
     * finds the {@link TypeMirror} for the tree and converts that into an
     * {@link AnnotatedTypeMirror}, but does not add any annotations to the
     * result.
     *
     * @param node
     * @return the type of {@code node}, without any annotations
     */
    public AnnotatedTypeMirror type(Tree node) {

        // Attempt to obtain the type via JCTree.
        if (((JCTree)node).type != null) {
            AnnotatedTypeMirror result = toAnnotatedType(((JCTree)node).type);
            return result;
        }

        // Attempt to obtain the type via TreePath (slower).
//        TreePath path = TreePath.getPath(root, node);
        TreePath path = new TreePath(new TreePath(root), node);
        assert path != null : "no path or type in tree";

        TypeMirror t = trees.getTypeMirror(path);
        assert validType(t) : node + " --> " + t;

        return toAnnotatedType(t);
    }

    /**
     * Used for asserting that a type is valid for converting to an annotated
     * type.
     *
     * @param type
     * @return true if {@code type} can be converted to an annotated type, false
     *         otherwise
     */
    private static final boolean validType(TypeMirror type) {
        if (type == null) return false;
        switch (type.getKind()) {
            case ERROR:
            case OTHER:
            case PACKAGE:
                return false;
        }
        return true;
    }

    /**
     * Used for asserting that a type is a valid annotated type.
     *
     * @param type an annotated type
     * @return true if the type is a valid annotated type, false otherwise
     */
    static final boolean validAnnotatedType(AnnotatedTypeMirror type) {
        if (type == null) return false;
        if (type.getUnderlyingType() == null)
            return true; // e.g., for receiver types
        return validType(type.getUnderlyingType());
    }

    /**
     * Determines the annotated type from a type in tree form.
     *
     * @param tree the type tree
     * @return the annotated type of the type in the AST
     */
    public AnnotatedTypeMirror getAnnotatedTypeFromTypeTree(Tree tree) {
        if (tree == null)
            throw new IllegalArgumentException("null tree");
        AnnotatedTypeMirror type = fromTypeTree(tree);
        annotateImplicit(tree, type);
        return type;
    }

    /**
     * Determines the annotated type of an AST node.
     *
     * <p>
     *
     * The type is determined as follows:
     * <ul>
     *  <li>if {@code tree} is a class declaration, determine its type via
     *    {@link #fromClass}</li>
     *  <li>if {@code tree} is a method or variable declaration, determine its
     *    type via {@link #fromMember(Tree)}</li>
     *  <li>if {@code tree} is an {@link ExpressionTree}, determine its type
     *    via {@link #fromExpression(ExpressionTree)}</li>
     *  <li>othwerise, throw an {@link UnsupportedOperationException}</li>
     * </ul>
     *
     * @param tree the AST node
     * @return the annotated type of {@code tree}
     * @throws UnsupportedOperationException if an annotated type cannot be
     *         obtained from {@code tree}
     * @throws IllegalArgumentException if {@code tree} is null
     *
     * @see #fromClass(ClassTree)
     * @see #fromMember(Tree)
     * @see #fromExpression(ExpressionTree)
     */
    public AnnotatedTypeMirror getAnnotatedType(Tree tree) {

        if (tree == null)
            throw new IllegalArgumentException("null tree");
        if (treeCache.containsKey(tree))
            return atypes.deepCopy(treeCache.get(tree));
        AnnotatedTypeMirror type;
        switch (tree.getKind()) {
            case CLASS:
                type = fromClass((ClassTree)tree); break;
            case METHOD:
            case VARIABLE:
                type = fromMember(tree); break;
            default:
                if (tree instanceof ExpressionTree)
                    type = fromExpression((ExpressionTree)tree);
                else throw new UnsupportedOperationException(
                        "query of annotated type for tree " + tree.getKind());
        }
        annotateImplicit(TreeUtils.skipParens(tree), type);

        switch (tree.getKind()) {
        case CLASS:
        case METHOD:
	    treeCache.put(tree, atypes.deepCopy(type));
        }
        return type;
    }

    /**
     * Determines the annotated type of an element using
     * {@link #fromElement(Element)}.
     *
     * @param elt the element
     * @return the annotated type of {@code elt}
     * @throws IllegalArgumentException if {@code elt} is null
     *
     * @see #fromElement(Element)
     */
    public AnnotatedTypeMirror getAnnotatedType(Element elt) {

        if (elt == null)
            throw new IllegalArgumentException("null element");
        if (elementCache.containsKey(elt))
            return elementCache.get(elt);
        AnnotatedTypeMirror type = fromElement(elt);
        annotateImplicit(elt, type);
        elementCache.put(elt, type);
        return type;
    }

    /**
     * Returns the type qualifiers to be least upper bound for c1 and c2
     * qualifiers.
     *
     * In most cases, this is simply the intersection of the collections.
     * However, if a type system specifies more than one type qualifier,
     * this needs to return the least restrictive type qualifiers.
     *
     * Examples:
     * For NonNull, unify('Nullable', 'NonNull') ==> Nullable
     * For IGJ, unify('Immutable', 'Mutable') ==> ReadOnly
     *
     * This method simply returns the intersection. Any subclasses may need to
     * override this method to adhere to the specification.
     *
     * @param c1    type qualifiers for the first type
     * @param c2    tyep qualifiers for the second type
     * @return  the least restrictive qualifiers for both types
     */
    public Collection<AnnotationMirror> unify(Collection<AnnotationMirror> c1,
            Collection<AnnotationMirror> c2) {
        Map<String, AnnotationMirror> first = new HashMap<String, AnnotationMirror>();
        for (AnnotationMirror anno : c1)
            first.put(AnnotationUtils.annotationName(anno).toString(), anno);
        Set<String> second = new HashSet<String>();
        for (AnnotationMirror anno : c2)
            second.add(AnnotationUtils.annotationName(anno).toString());
        first.keySet().retainAll(second);
        return first.values();
    }

    /**
     * Gets the element for a class corresponding to a declaration.
     *
     * @param node
     * @return the element for the given class
     */
    public final TypeElement elementFromDeclaration(ClassTree node) {
        assert node != null : "null node";
        TypeElement elt = (TypeElement) TreeInfo.symbolFor((JCTree) node);
        if (elt != null)
            return elt;
        TreePath path = trees.getPath(root, node);
        return (TypeElement)trees.getElement(path);
    }

    /**
     * Gets the element for a method corresponding to a declaration.
     *
     * @param node
     * @return the element for the given method
     */
    public final ExecutableElement elementFromDeclaration(MethodTree node) {
        assert node != null : "null node";
        ExecutableElement elt = (ExecutableElement)TreeInfo.symbolFor((JCTree)node);
        if (elt != null)
            return elt;
        TreePath path = trees.getPath(root, node);
        return (ExecutableElement)trees.getElement(path);
    }

    /**
     * Gets the element for a variable corresponding to its declaration.
     *
     * @param node
     * @return the element for the given variable
     */
    public final VariableElement elementFromDeclaration(VariableTree node) {
        assert node != null : "null node";
        VariableElement elt = (VariableElement)TreeInfo.symbolFor((JCTree)node);
        if (elt != null)
            return elt;
        TreePath path = trees.getPath(root, node);
        return (VariableElement)trees.getElement(path);
    }

    /**
     * Gets the element for the method corresponding to this invocation. To get
     * the element for a method declaration, use {@link
     * Trees#getElement(TreePath)} instead.
     *
     * @param node the method invocation
     * @return the element for the method that corresponds to this invocation
     */
    public final ExecutableElement elementFromUse(MethodInvocationTree node) {
        return (ExecutableElement)TreeInfo.symbol((JCTree)node.getMethodSelect());
    }

    /**
     * Gets the element for the declaration corresponding to this identifier.
     * To get the element for a declaration, use {@link
     * Trees#getElement(TreePath)} instead.
     *
     * @param node the identifier
     * @return the element for the declaration that corresponds to this
     * identifier
     */
    public final Element elementFromUse(IdentifierTree node) {
        return TreeInfo.symbol((JCTree)node);
    }

    /**
     * Gets the element for the declaration corresponding to this member
     * access.  To get the element for a declaration, use {@link
     * Trees#getElement(TreePath)} instead.
     *
     * @param node the member access
     * @return the element for the declaration that corresponds to this member
     * access
     */
    public final Element elementFromUse(MemberSelectTree node) {
        return TreeInfo.symbol((JCTree)node);
    }

    // ...

    protected final List<? extends AnnotationMirror> annotationsFromTree(AnnotatedTypeTree node) {
	return null;
    }

    protected final List<? extends AnnotationMirror> annotationsFromArrayCreation(NewArrayTree node, int level) {

        return new ArrayList<AnnotationMirror>();
    }

    protected final Tree declarationFromElement(Element elt) {
        if (elementToTreeCache.containsKey(elt))
            return elementToTreeCache.get(elt);
        // TODO: handle type parameter declarations?
        Tree fromElt = trees.getTree(elt);
        if (fromElt != null) {
            elementToTreeCache.put(elt, fromElt);
            return fromElt;
        }
        Tree tr = TreeInfo.declarationFor((Symbol)elt, (JCTree)root);
        elementToTreeCache.put(elt, tr);
        return tr;
    }

    /**
     * @return the type of the most enclosing visited class
     */
    protected AnnotatedDeclaredType getCurrentClassType(Tree tree) {
        if (visitorState.getClassType() != null) {
            return visitorState.getClassType();
        } else {
            ClassTree enclosingClass = TreeUtils.enclosingClass(TreePath.getPath(root, tree));
            return (AnnotatedDeclaredType)getAnnotatedType(enclosingClass);
        }
    }

    /**
     * @return the method receiver for the most enclosing method currently visited
     */
    protected AnnotatedDeclaredType getCurrentMethodReceiver(Tree tree) {
        if (visitorState.getClassType() != null) {
            return visitorState.getMethodReceiver();
        } else {
            MethodTree enclosingMethod = TreeUtils.enclosingMethod(TreePath.getPath(root, tree));
            AnnotatedExecutableType method = (AnnotatedExecutableType) getAnnotatedType(enclosingMethod);
            return method.getReceiverType();
        }
    }

    /**
     * Returns the type of {@code this} in the current location, which can
     * be used if {@code this} has a special semantics (e.g. {@code this}
     * is non-null)
     */
    public AnnotatedDeclaredType getSelfType(Tree tree) {
        AnnotatedDeclaredType type = getCurrentClassType(tree);
        AnnotatedDeclaredType methodReceiver = getCurrentMethodReceiver(tree);
        if (methodReceiver != null)
            type.addAnnotations(methodReceiver.getAnnotations());
        return type;
    }

    protected final List<AnnotatedDeclaredType>
        directSuperTypes(AnnotatedDeclaredType type) {
        List<AnnotatedDeclaredType> supertypes =
            superTypeFinder.visitDeclared(type, null);
        postDirectSuperTypes(type, supertypes);
        return supertypes;
    }

    protected final List<? extends AnnotatedTypeMirror> directSuperTypes(
            AnnotatedTypeMirror type) {
        List<? extends AnnotatedTypeMirror> supertypes =
            superTypeFinder.visit(type, null);
        postDirectSuperTypes(type, supertypes);
        return supertypes;
    }

    private SuperTypeFinder superTypeFinder = new SuperTypeFinder();
    private class SuperTypeFinder extends
    SimpleAnnotatedTypeVisitor<List<? extends AnnotatedTypeMirror>, Void> {

        @Override
        public List<AnnotatedTypeMirror> defaultAction(AnnotatedTypeMirror t, Void p) {
            return new ArrayList<AnnotatedTypeMirror>();
        }


        /**
         * Primitive Rules:
         *
         * double >1 float
         * float >1 long
         * long >1 int
         * int >1 char
         * int >1 short
         * short >1 byte
         *
         * For easiness:
         * boxed(primitiveType) >: primitiveType
         */
        @Override
        public List<AnnotatedTypeMirror> visitPrimitive(AnnotatedPrimitiveType type, Void p) {
            List<AnnotatedTypeMirror> superTypes =
                new ArrayList<AnnotatedTypeMirror>();
            Set<AnnotationMirror> annotations = type.getAnnotations();

            // Find Boxed type
            TypeElement boxed = types.boxedClass(type.getUnderlyingType());
            AnnotatedDeclaredType boxedType = (AnnotatedDeclaredType) getAnnotatedType(boxed);
            boxedType.addAnnotations(annotations);
            superTypes.add(boxedType);

            TypeKind superPrimitiveType = null;

            if (type.getKind() == TypeKind.BOOLEAN) {
                // Nothing
            } else if (type.getKind() == TypeKind.BYTE) {
                superPrimitiveType = TypeKind.SHORT;
            } else if (type.getKind() == TypeKind.CHAR) {
                superPrimitiveType = TypeKind.INT;
            } else if (type.getKind() == TypeKind.DOUBLE) {
                // Nothing
            } else if (type.getKind() == TypeKind.FLOAT) {
                superPrimitiveType = TypeKind.DOUBLE;
            } else if (type.getKind() == TypeKind.INT) {
                superPrimitiveType = TypeKind.LONG;
            } else if (type.getKind() == TypeKind.LONG) {
                superPrimitiveType = TypeKind.FLOAT;
            } else if (type.getKind() == TypeKind.SHORT) {
                superPrimitiveType = TypeKind.INT;
            } else
                assert false: "Forgot the primitive " + type;

            if (superPrimitiveType != null) { @SuppressWarnings("igj")
                AnnotatedPrimitiveType superPrimitive = (AnnotatedPrimitiveType)
                    toAnnotatedType(types.getPrimitiveType(superPrimitiveType));
                superPrimitive.addAnnotations(annotations);
                superTypes.add(superPrimitive);
            }

            return superTypes;
        }

        @Override
        @SuppressWarnings("igj")
        public List<AnnotatedDeclaredType> visitDeclared(AnnotatedDeclaredType type, Void p) {
            List<AnnotatedDeclaredType> supertypes =
                new ArrayList<AnnotatedDeclaredType>();
            Set<AnnotationMirror> annotations = type.getAnnotations();

            TypeElement typeElement =
                (TypeElement) type.getUnderlyingType().asElement();
            // Mapping of type variable to actual types
            Map<TypeParameterElement, AnnotatedTypeMirror> mapping =
                new HashMap<TypeParameterElement, AnnotatedTypeMirror>();

            for (int i = 0; i < typeElement.getTypeParameters().size() &&
                            i < type.getTypeArguments().size(); ++i) {
                mapping.put(typeElement.getTypeParameters().get(i),
                        type.getTypeArguments().get(i));
            }

            ClassTree classTree = trees.getTree(typeElement);
            // Testing against enum. idealy we can simply use element!
            if (classTree != null && typeElement.getKind() != ElementKind.ENUM) {
                supertypes.addAll(supertypesFromTree(classTree));
            } else {
                supertypes.addAll(supertypesFromElement(typeElement));
            }

            for (AnnotatedTypeMirror dt : supertypes) {
                if (!mapping.isEmpty())
                    replacer.visit(dt, mapping);
                dt.addAnnotations(annotations);
            }

            return supertypes;
        }

        @SuppressWarnings("igj")
        private List<AnnotatedDeclaredType> supertypesFromElement(TypeElement typeElement) {
            List<AnnotatedDeclaredType> supertypes = new ArrayList<AnnotatedDeclaredType>();
            // Find the super types: Start with superclass
            if (typeElement.getSuperclass().getKind() != TypeKind.NONE) {
                DeclaredType superClass = (DeclaredType) typeElement.getSuperclass();
                AnnotatedDeclaredType dt =
                    (AnnotatedDeclaredType)toAnnotatedType(superClass);
                supertypes.add(dt);
            } else if (!ElementUtils.isObject(typeElement)) {
                supertypes.add(objectType.getCopy(true));
            }
            for (TypeMirror st : typeElement.getInterfaces()) {
                AnnotatedDeclaredType ast =
                    (AnnotatedDeclaredType)toAnnotatedType(st);
                supertypes.add(ast);
            }
            return supertypes;
        }

        @SuppressWarnings("igj")
        private List<AnnotatedDeclaredType> supertypesFromTree(ClassTree classTree) {
            List<AnnotatedDeclaredType> supertypes = new ArrayList<AnnotatedDeclaredType>();
            if (classTree.getExtendsClause() != null) {
                AnnotatedDeclaredType adt = (AnnotatedDeclaredType)
                    fromTypeTree(classTree.getExtendsClause());
                supertypes.add(adt);
            } else if (!(elementFromDeclaration(classTree)).getQualifiedName()
                    .contentEquals("java.lang.Object")) {
                supertypes.add(objectType.getCopy(true));
            }

            for (Tree implemented : classTree.getImplementsClause()) {
                AnnotatedDeclaredType adt = (AnnotatedDeclaredType)
                    fromTypeTree(implemented);
                supertypes.add(adt);
            }
            return supertypes;
        }

        /**
         * For type = A[ ] ==>
         *  Object >: A[ ]
         *  Clonable >: A[ ]
         *  java.io.Serializable >: A[ ]
         *
         * if A is reference type, then also
         *  B[ ] >: A[ ] for any B[ ] >: A[ ]
         */
        @Override
        @SuppressWarnings("igj")
        public List<AnnotatedTypeMirror> visitArray(AnnotatedArrayType type, Void p) {
            List<AnnotatedTypeMirror> superTypes = new ArrayList<AnnotatedTypeMirror>();
            Set<AnnotationMirror> annotations = type.getAnnotations();

            final AnnotatedTypeMirror objectType =
                getAnnotatedType(elements.getTypeElement("java.lang.Object"));
            objectType.addAnnotations(annotations);
            superTypes.add(objectType);

            final AnnotatedTypeMirror cloneableType =
                getAnnotatedType(elements.getTypeElement("java.lang.Cloneable"));
            cloneableType.addAnnotations(annotations);
            superTypes.add(cloneableType);

            final AnnotatedTypeMirror serializableType =
                getAnnotatedType(elements.getTypeElement("java.io.Serializable"));
            serializableType.addAnnotations(annotations);
            superTypes.add(serializableType);

            if (type.getComponentType() instanceof AnnotatedReferenceType) {
                for (AnnotatedTypeMirror sup : type.getComponentType().directSuperTypes()) {
                    ArrayType arrType = types.getArrayType(sup.getUnderlyingType());
                    AnnotatedArrayType aarrType = (AnnotatedArrayType)
                        toAnnotatedType(arrType);
                    aarrType.setComponentType(sup);
                    aarrType.addAnnotations(annotations);
                    superTypes.add(aarrType);
                }
            }

            return superTypes;
        }

        @Override
        public List<AnnotatedTypeMirror> visitTypeVariable(AnnotatedTypeVariable type, Void p) {
            List<AnnotatedTypeMirror> superTypes = new ArrayList<AnnotatedTypeMirror>();
            if (type.getUpperBound() != null)
                superTypes.add(type.getUpperBound());
            return superTypes;
        }

        @Override
        public List<AnnotatedTypeMirror> visitWildcard(AnnotatedWildcardType type, Void p) {
            List<AnnotatedTypeMirror> superTypes = new ArrayList<AnnotatedTypeMirror>();
            if (type.getExtendsBound() != null)
                superTypes.add(type.getExtendsBound());
            return superTypes;
        }
    };

    /**
     *
     */
    private AnnotatedTypeScanner<Void, Map<TypeParameterElement, AnnotatedTypeMirror>>
        replacer = new AnnotatedTypeScanner<Void, Map<TypeParameterElement, AnnotatedTypeMirror>>() {

        public Void visitDeclared(AnnotatedDeclaredType type, Map<TypeParameterElement, AnnotatedTypeMirror> mapping) {
            List<AnnotatedTypeMirror> args = new ArrayList<AnnotatedTypeMirror>();
            for (AnnotatedTypeMirror arg : type.getTypeArguments()) {
                Element elem = types.asElement(arg.getUnderlyingType());
                if ((elem != null) &&
                        (elem.getKind() == ElementKind.TYPE_PARAMETER) &&
                        (mapping.containsKey(elem)))
                    args.add(mapping.get(elem));
                else
                    args.add(arg);
            }
            type.setTypeArguments(args);
            return super.visitDeclared(type, mapping);
        }
    };

    /**
     * Returns the receiver type of the expression tree, or null if it does not exist.
     *
     * The only trees that could potentially have a receiver are:
     * <ul>
     *  <li> Array Access
     *  <li> Identifiers (whose receivers are usually self type)
     *  <li> Method Invocation Trees
     *  <li> Member Select Trees
     * </ul>
     *
     * @param expression
     * @return  the type of the receiver of this expression
     */
    public /*@Nullable*/ AnnotatedTypeMirror getReceiver(ExpressionTree expression) {

        if (!(expression.getKind() == Tree.Kind.METHOD_INVOCATION
                || expression.getKind() == Tree.Kind.MEMBER_SELECT
                || expression.getKind() == Tree.Kind.IDENTIFIER
                || expression.getKind() == Tree.Kind.ARRAY_ACCESS))
            // No receiver type for those
            return null;

        Element elem = InternalUtils.symbol(expression);
        if (elem == null || ElementUtils.isStatic(elem))
            return null;

        // Avoid int.class
        if (expression.getKind() == Tree.Kind.MEMBER_SELECT &&
                ((MemberSelectTree)expression).getExpression() instanceof PrimitiveTypeTree)
            return null;

        if (TreeUtils.isSelfAccess(expression)) {
            return getSelfType(expression);
        } else {
            Tree receiver = expression;
            while (receiver.getKind() == Tree.Kind.ARRAY_ACCESS)
                receiver = ((ArrayAccessTree)receiver).getExpression();
            if (receiver.getKind() == Tree.Kind.METHOD_INVOCATION)
                receiver = ((MethodInvocationTree)receiver).getMethodSelect();
            assert receiver.getKind() == Tree.Kind.MEMBER_SELECT;
            if (receiver.getKind() == Tree.Kind.MEMBER_SELECT)
                receiver = ((MemberSelectTree)receiver).getExpression();

            return getAnnotatedType(receiver);
        }
    }

    /**
     * A callback method for the AnnotatedTypeFactory subtypes to customize
     * directSuperTypes().  Overriding methods should merely change the
     * annotations on the supertypes, without adding or removing new types
     *
     * @param type  the type whose supertypes are desired
     * @param supertypes
     *      the supertypes as specified by the base AnnotatedTypeFactory
     *
     */
    protected void postDirectSuperTypes(AnnotatedTypeMirror type,
            List<? extends AnnotatedTypeMirror> supertypes) {
        // Nothing
    }

    /**
     * A callback method for the AnnotatedTypeFactory subtypes to customize
     * AnnotatedTypes.asMemberOf().  Overriding methods should merely change the
     * annotations on the subtypes, without changing the types.
     *
     * @param type  the annotated type of the element
     * @param owner the annotated type of the receiver of the accessing tree
     * @param element   the element of the field or method
     */
    public void postAsMemberOf(AnnotatedTypeMirror type,
            AnnotatedTypeMirror owner, Element element) {
        annotateImplicit(element, type);
    }

    /**
     * Returns the VisitorState instance used by the factory to infer types
     */
    public VisitorState getVisitorState() {
        return this.visitorState;
    }

    /**
     * A Utility method for creating LRU cache
     * @param size  size of the cache
     * @return  a new cache with the provided size
     */
    protected static <K, V> Map<K, V> createLRUCache(final int size) {
        return new LinkedHashMap<K, V>() {

            private static final long serialVersionUID = 5261489276168775084L;
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
                return size() > size;
            }
        };
    }

    /**
     * Returns that invoked method executable type through the method
     * invocation tree
     */
    public AnnotatedExecutableType methodFromUse(MethodInvocationTree tree) {
        ExecutableElement methodElt = elementFromUse(tree);
        AnnotatedTypeMirror type = getReceiver(tree);
        AnnotatedExecutableType methodType =
            (AnnotatedExecutableType) atypes.asMemberOf(type, methodElt);

        Map<AnnotatedTypeVariable, AnnotatedTypeMirror> typeVarMapping =
            atypes.findTypeParameters(tree);





        return methodType;
    }

    /**
     * Determines the {@link AnnotatedExecutableType} of a constructor
     * invocation. Note that this is different than calling
     * {@link #getAnnotatedType(Tree)} or
     * {@link #fromExpression(ExpressionTree)} on the constructor invocation;
     * those determine the type of the <i>result</i> of invoking the
     * constructor, which is probably an {@link AnnotatedDeclaredType}.
     *
     * @param tree a constructor invocation
     * @return the annotated type of the invoked constructor (as an executable
     *         type)
     */
    public AnnotatedExecutableType constructorFromUse(NewClassTree tree) {
        ExecutableElement ctor = InternalUtils.constructor(tree);
        AnnotatedTypeMirror type = fromTypeTree(tree.getIdentifier());
        return (AnnotatedExecutableType) atypes.asMemberOf(type, ctor);
    }

    /**
     * returns the annotated boxed type of the given primitive type.
     *
     * The returned type would only have the annotations on the given type.
     *
     * @param type  the primitivate type
     * @return the boxed declared type of the passed primitive type
     */
    public AnnotatedDeclaredType getBoxedType(AnnotatedPrimitiveType type) {
        TypeElement typeElt = env.getTypeUtils().boxedClass(type.getUnderlyingType());
        AnnotatedDeclaredType dt = (AnnotatedDeclaredType)fromElement(typeElt);
        dt.addAnnotations(type.getAnnotations());
        return dt;
    }

    /**
     * returns the annotated primitive type of the given declared type
     * if it is a boxed declared type.  Otherwise, it throws
     * <i>IllegalArgumentException</i> exception.
     *
     * The returned type would have the annotations on the given type and nothing
     * else.
     *
     * @param type  the declared type
     * @return the unboxed primitive type
     * @throws IllegalArgmentException   if the type given has no unbox conversion
     */
    public AnnotatedPrimitiveType getUnboxedType(AnnotatedDeclaredType type)
    throws IllegalArgumentException {
        PrimitiveType primitiveType =
            env.getTypeUtils().unboxedType(type.getUnderlyingType());
        AnnotatedPrimitiveType pt = (AnnotatedPrimitiveType)
            AnnotatedTypeMirror.createType(primitiveType, env, this);
        pt.addAnnotations(type.getAnnotations());
        return pt;
    }
}

/*
  Fix the error by deep-copying the type before insertion and after retrieval.

  Edit getAnnotatedType(Element) as follows:

  -     return elementCache.get(elt);
  +     return atypes.deepCopy(elementCache.get(elt));

  -     elementCache.put(elt, type);
  +     elementCache.put(elt, atypes.deepCopy(type));
*/
