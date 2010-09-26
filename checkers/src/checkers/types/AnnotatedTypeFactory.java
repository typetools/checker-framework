package checkers.types;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

import checkers.basetype.BaseTypeChecker;
import checkers.javari.quals.*;
import checkers.nullness.quals.Nullable;
import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeMirror.*;
import checkers.types.visitors.AnnotatedTypeScanner;
import checkers.util.*;
import checkers.util.stub.StubParser;
import checkers.util.stub.StubUtil;
import static checkers.types.TypeFromTree.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.*;

/**
 * The methods of this class take an element or AST node, and return the
 * annotated type as an {@link AnnotatedTypeMirror}.  The methods are:
 *
 * <ul>
 * <li>{@link #getAnnotatedType(ClassTree)}</li>
 * <li>{@link #getAnnotatedType(MethodTree)}</li>
 * <li>{@link #getAnnotatedType(Tree)}</li>
 * <li>{@link #getAnnotatedTypeFromTypeTree(Tree)}</li>
 * <li>{@link #getAnnotatedType(TypeElement)}</li>
 * <li>{@link #getAnnotatedType(ExecutableElement)}</li>
 * <li>{@link #getAnnotatedType(Element)}</li>
 * </ul>
 *
 * This implementation only adds qualifiers explicitly specified by the
 * programmer.
 *
 * Type system checker writers may need to subclass this class, to add implicit
 * and default qualifiers according to the type system semantics. Subclasses
 * should especially override
 * {@link AnnotatedTypeFactory#annotateImplicit(Element, AnnotatedTypeMirror)}
 * and {@link #annotateImplicit(Tree, AnnotatedTypeMirror)}.
 *
 * @checker.framework.manual #writing-a-checker How to write a checker plug-in
 */
public class AnnotatedTypeFactory {

    /** The {@link Trees} instance to use for tree node path finding. */
    protected final Trees trees;

    /** optional! The AST of the source file being operated on */
    protected final /*@Nullable*/ CompilationUnitTree root;

    /** The processing environment to use for accessing compiler internals. */
    protected final ProcessingEnvironment env;

    /** The factory to use for creating annotations. */
    protected final AnnotationUtils annotations;

    /** Utility class for working with {@link Element}s. */
    protected final Elements elements;

    /** Utility class for working with {@link TypeMirror}s. */
    protected final Types types;

    /** Utility class for manipulating annotated types. */
    protected final AnnotatedTypes atypes;

    /** the state of the visitor **/
    final protected VisitorState visitorState;

    /** Represent the annotation relations **/
    protected final @Nullable QualifierHierarchy qualHierarchy;

    private final Map<Element, AnnotatedTypeMirror> indexTypes;

    private Class<? extends SourceChecker> checkerClass;

    private final boolean annotatedTypeParams;

    /**
     * Map from class name (canonical name) of an annotation, to the
     * annotation in the Checker Framework that will be used in its place.
     */
    private Map<String, AnnotationMirror> aliases = new HashMap<String, AnnotationMirror>();

    private static int uidCounter = 0;
    public final int uid;

    /**
     * Constructs a factory from the given {@link ProcessingEnvironment}
     * instance and syntax tree root. (These parameters are required so that
     * the factory may conduct the appropriate annotation-gathering analyses on
     * certain tree types.)
     *
     * Root can be {@code null} if the factory does not operate on trees.
     *
     * @param checker the {@link SourceChecker} to which this factory belongs
     * @param root the root of the syntax tree that this factory produces
     *            annotated types for
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public AnnotatedTypeFactory(SourceChecker checker, @Nullable CompilationUnitTree root) {
        this(checker.getProcessingEnvironment(),
                (checker instanceof BaseTypeChecker) ? ((BaseTypeChecker)checker).getQualifierHierarchy() : null,
                root,
                checker == null ? null : checker.getClass());
    }

    public AnnotatedTypeFactory(ProcessingEnvironment env,
            @Nullable QualifierHierarchy qualHierarchy, @Nullable CompilationUnitTree root,
            Class<? extends SourceChecker> checkerClass) {
        uid = ++uidCounter;
        this.env = env;
        this.root = root;
        this.checkerClass = checkerClass;
        this.trees = Trees.instance(env);
        this.annotations = AnnotationUtils.getInstance(env);
        this.elements = env.getElementUtils();
        this.types = env.getTypeUtils();
        this.atypes = new AnnotatedTypes(env, this);
        this.visitorState = new VisitorState();
        this.qualHierarchy = qualHierarchy;
        this.supportedQuals = getSupportedQualifiers();
        this.indexTypes = buildIndexTypes();
        this.annotatedTypeParams = true; // env.getOptions().containsKey("annotatedTypeParams");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "#" + uid;
    }        

    /**
     * For an annotated type parameter or wildcard (e.g.
     * {@code <@Nullable T>}, it returns
     * {@code true} if the annotation should target the type parameter itself,
     * otherwise the annotation should target the extends clause, i.e.
     * the declaration should be treated as {@code <T extends @Nullable Object>}
     */
    public boolean canHaveAnnotatedTypeParameters() {
        return this.annotatedTypeParams;
    }

    // **********************************************************************
    // Factories for annotated types that account for implicit qualifiers
    // **********************************************************************

    /** Should cache? disable for better debugging */
    private final static boolean SHOULD_CACHE = true;

    /** Various Caches **/
    /** Size of LRU cache **/
    private final static int CACHE_SIZE = 50;
    private Map<Tree, AnnotatedTypeMirror> treeCache = createLRUCache(CACHE_SIZE);
    private Map<Element, AnnotatedTypeMirror> elementCache = createLRUCache(CACHE_SIZE);
    private Map<Element, Tree> elementToTreeCache  = createLRUCache(CACHE_SIZE);

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
        AnnotatedTypeMirror type = fromElement(elt);
        annotateImplicit(elt, type);
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
     *  <li>otherwise, throw an {@link UnsupportedOperationException}</li>
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
    // I wish I could make this method protected
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
        // case VARIABLE:
            if (SHOULD_CACHE)
                treeCache.put(tree, atypes.deepCopy(type));
        }
        return type;
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

    // **********************************************************************
    // Factories for annotated types that do not account for implicit qualifiers.
    // They only include qualifiers explicitly inserted by the user.
    // **********************************************************************

    /**
     * Determines the annotated type of an element.
     *
     * @param elt the element
     * @return the annotated type of the element
     */
    public AnnotatedTypeMirror fromElement(Element elt) {
        if (elementCache.containsKey(elt))
            return atypes.deepCopy(elementCache.get(elt));
        if (elt.getKind() == ElementKind.PACKAGE)
            return toAnnotatedType(elt.asType());
        AnnotatedTypeMirror type;
        Tree decl = declarationFromElement(elt);

        if (decl == null && indexTypes != null && indexTypes.containsKey(elt)) {
            type = indexTypes.get(elt);
        } else if (decl == null && (indexTypes == null || !indexTypes.containsKey(elt))) {
            type = toAnnotatedType(elt.asType());
            type.setElement(elt);
            TypeFromElement.annotate(type, elt);

            if (elt instanceof ExecutableElement
                    || elt instanceof VariableElement) {
                annotateInheritedFromClass(type);
            }
        } else if (decl instanceof ClassTree) {
            type = fromClass((ClassTree)decl);
        } else if (decl instanceof VariableTree) {
            type = fromMember(decl);
        } else if (decl instanceof MethodTree) {
            type = fromMember(decl);
        } else
            throw new AssertionError("Cannot be here " + decl.getKind() +
                    " " + elt);

        if (SHOULD_CACHE && indexTypes != null)
            elementCache.put(elt, atypes.deepCopy(type));
        return type;
    }

    /**
     * Determines the annotated type of a class from its declaration.
     *
     * @param tree the class declaration
     * @return the annotated type of the class being declared
     */
    public AnnotatedDeclaredType fromClass(ClassTree tree) {
        AnnotatedDeclaredType result = (AnnotatedDeclaredType)
            fromTreeWithVisitor(TypeFromClass.INSTANCE, tree);
        return result;
    }

    protected Map<Tree, AnnotatedTypeMirror> fromTreeCache = createLRUCache(CACHE_SIZE);

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
        if (fromTreeCache.containsKey(tree))
            return atypes.deepCopy(fromTreeCache.get(tree));
        AnnotatedTypeMirror result = fromTreeWithVisitor(
                TypeFromMember.INSTANCE, tree);
        annotateInheritedFromClass(result);
        if (SHOULD_CACHE)
            fromTreeCache.put(tree, atypes.deepCopy(result));
        return result;
    }

    /**
     * Determines the annotated type of an expression.
     *
     * @param tree an expression
     * @return the annotated type of the expression
     */
    public AnnotatedTypeMirror fromExpression(ExpressionTree tree) {
        if (fromTreeCache.containsKey(tree))
            return atypes.deepCopy(fromTreeCache.get(tree));
        AnnotatedTypeMirror result = fromTreeWithVisitor(
                TypeFromExpression.INSTANCE, tree);
        annotateInheritedFromClass(result);
        if (SHOULD_CACHE)
            fromTreeCache.put(tree, atypes.deepCopy(result));
        return result;
    }

    /**
     * Determines the annotated type from a type in tree form.  This method
     * does not add implicit annotations.
     *
     * @param tree the type tree
     * @return the annotated type of the type in the AST
     */
    public AnnotatedTypeMirror fromTypeTree(Tree tree) {
        if (fromTreeCache.containsKey(tree))
            return atypes.deepCopy(fromTreeCache.get(tree));

        AnnotatedTypeMirror result = fromTreeWithVisitor(
                TypeFromTypeTree.INSTANCE, tree);

        // treat Raw as generic!
        // TODO: This doesn't handle recursive type parameter
        // e.g. class Pair<Y extends List<Y>> { ... }
        if (result.getKind() == TypeKind.DECLARED) {
            AnnotatedDeclaredType dt = (AnnotatedDeclaredType)result;
            if (dt.getTypeArguments().isEmpty()
                    && !((TypeElement)dt.getUnderlyingType().asElement()).getTypeParameters().isEmpty()) {
                List<AnnotatedTypeMirror> typeArgs = new ArrayList<AnnotatedTypeMirror>();
                AnnotatedDeclaredType declaration = fromElement((TypeElement)dt.getUnderlyingType().asElement());
                for (AnnotatedTypeMirror typeParam : declaration.getTypeArguments()) {
                    AnnotatedTypeVariable typeParamVar = (AnnotatedTypeVariable)typeParam;
                    AnnotatedTypeMirror upperBound = typeParamVar.getUpperBound();
                    while (upperBound.getKind() == TypeKind.TYPEVAR)
                        upperBound = ((AnnotatedTypeVariable)upperBound).getUpperBound();
                    typeArgs.add(upperBound.getCopy(false));
                }
                dt.setTypeArguments(typeArgs);
            }
        }
        annotateInheritedFromClass(result);
        if (SHOULD_CACHE)
            fromTreeCache.put(tree, atypes.deepCopy(result));
        return result;
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
        checkRep(result);
        return result;
    }

    // **********************************************************************
    // Customization methods meant to be overridden by subclasses to include
    // implicit annotations
    // **********************************************************************

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
     * A callback method for the AnnotatedTypeFactory subtypes to customize
     * directSuperTypes().  Overriding methods should merely change the
     * annotations on the supertypes, without adding or removing new types
     *
     * The default provided implementation adds {@code type} annotations to
     * {@code supertypes}.  This allows the {@code type} and its supertypes
     * to have the qualifiers, e.g. the supertypes of an {@code Immutable}
     * type are also {@code Immutable}.
     *
     * @param type  the type whose supertypes are desired
     * @param supertypes
     *      the supertypes as specified by the base AnnotatedTypeFactory
     *
     */
    protected void postDirectSuperTypes(AnnotatedTypeMirror type,
            List<? extends AnnotatedTypeMirror> supertypes) {
        Set<AnnotationMirror> annotations = type.getAnnotations();
        for (AnnotatedTypeMirror supertype : supertypes) {
            if (!annotations.equals(supertype.getAnnotations())) {
                supertype.clearAnnotations();
                supertype.addAnnotations(annotations);
            }
        }
    }

    /**
     * A callback method for the AnnotatedTypeFactory subtypes to customize
     * AnnotatedTypes.asMemberOf().  Overriding methods should merely change
     * the annotations on the subtypes, without changing the types.
     *
     * @param type  the annotated type of the element
     * @param owner the annotated type of the receiver of the accessing tree
     * @param element   the element of the field or method
     */
    protected void postAsMemberOf(AnnotatedTypeMirror type,
            AnnotatedTypeMirror owner, Element element) {
        annotateImplicit(element, type);
    }

    /**
     * Adds annotations to the type based on the annotations from its class
     * type if and only if no annotations are already present on the type.
     *
     * @param type the type for which class annotations will be inherited if
     * there are no annotations already present
     */
    protected void annotateInheritedFromClass(@Mutable AnnotatedTypeMirror type) {
        InheritedFromClassAnnotator.INSTANCE.visit(type, this);
    }

    /**
     * A singleton utility class for pulling annotations down from a class
     * type.
     *
     * @see #annotateInheritedFromClass
     */
    protected static class InheritedFromClassAnnotator
            extends AnnotatedTypeScanner<Void, AnnotatedTypeFactory> {

        /** The singleton instance. */
        public static final InheritedFromClassAnnotator INSTANCE
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

            if (classElt != null && !type.isAnnotated()) {
                AnnotatedTypeMirror classType = p.fromElement(classElt);
                assert classType != null;
                for (AnnotationMirror anno : classType.getAnnotations()) {
                    if (AnnotationUtils.hasInheritiedMeta(anno)) {
                        type.addAnnotation(anno);
                    }
                }
            }

            return super.visitDeclared(type, p);
        }
    }

    // **********************************************************************
    // Utilities method for getting specific types from trees or elements
    // **********************************************************************

    protected AnnotatedDeclaredType getImplicitReceiverType(Tree tree) {
        assert (tree.getKind() == Tree.Kind.IDENTIFIER
                || tree.getKind() == Tree.Kind.MEMBER_SELECT
                || tree.getKind() == Tree.Kind.METHOD_INVOCATION
                || tree.getKind() == Tree.Kind.NEW_CLASS);

        Element element = InternalUtils.symbol(tree);
        assert element != null;
                if (ElementUtils.isStatic(element)
                        || element.getKind() == ElementKind.PACKAGE)
            return null;

        if (isMostEnclosingThisDeref(tree))
            return getSelfType(tree);

        TypeElement typeElt = ElementUtils.enclosingClass(element);
        if (typeElt == null) {
            throw new AssertionError("enclosingClass()=>null for element=" + element);
        }
        return getEnclosingType(typeElt, tree);
    }

    protected final boolean isMostEnclosingThisDeref(Tree tree) {
        // check local variables or expressions
        Element element = InternalUtils.symbol(tree);
        if (element == null)
            return true;
        if (ElementUtils.isStatic(element))
            return false;
        if (element.getKind() == ElementKind.LOCAL_VARIABLE
            || element.getKind() == ElementKind.PARAMETER)
            return !ElementUtils.isStatic(element.getEnclosingElement());
        if (isThisDereference(tree))
            return true;
        TypeElement typeElt = ElementUtils.enclosingClass(element);
        ClassTree enclosingClass = visitorState.getClassTree();
        if (enclosingClass == null)
            enclosingClass = TreeUtils.enclosingClass(getPath(tree));
        if (enclosingClass != null && isSubtype(TreeUtils.elementFromDeclaration(enclosingClass), typeElt))
            return true;

        // ran out of options
        return false;
    }
    private final boolean isThisDereference(Tree tree) {
        if (tree.getKind() != Tree.Kind.MEMBER_SELECT)
            return false;
        MemberSelectTree memSelTree = (MemberSelectTree)tree;
        return (memSelTree.getExpression().getKind() == Tree.Kind.IDENTIFIER
                && ((IdentifierTree)memSelTree.getExpression()).getName().contentEquals("this"));
    }

    /**
     * Returns the type of {@code this} in the current location, which can
     * be used if {@code this} has a special semantics (e.g. {@code this}
     * is non-null)
     */
    public AnnotatedDeclaredType getSelfType(Tree tree) {
        AnnotatedDeclaredType type = getCurrentClassType(tree);
        AnnotatedDeclaredType methodReceiver = getCurrentMethodReceiver(tree);
        if (methodReceiver != null) {
            type.clearAnnotations();
            type.addAnnotations(methodReceiver.getAnnotations());
        }
        return type;
    }

    public AnnotatedDeclaredType getEnclosingType(TypeElement element, Tree tree) {

        Element enclosingElt = getMostInnerClassOrMethod(tree);

        while (enclosingElt != null) {
            if (enclosingElt instanceof ExecutableElement) {
                ExecutableElement method = (ExecutableElement)enclosingElt;
                if (method.asType() != null // XXX: hack due to a compiler bug
                        && isSubtype((TypeElement)method.getEnclosingElement(), element))
                    if (ElementUtils.isStatic(method))
                        return null;
                    else
                        return getAnnotatedType(method).getReceiverType();
            } else
            if (enclosingElt instanceof TypeElement) {
                if (isSubtype((TypeElement)enclosingElt, element))
                    return getAnnotatedType(element);
            }
            enclosingElt = enclosingElt.getEnclosingElement();
        }
        return null;
    }

    private boolean isSubtype(TypeElement a1, TypeElement a2) {
        return (a1.equals(a2)
                || types.isSubtype(types.erasure(a1.asType()),
                        types.erasure(a2.asType())));
    }

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
    public final AnnotatedTypeMirror getReceiver(ExpressionTree expression) {

        if (!(expression.getKind() == Tree.Kind.METHOD_INVOCATION
                || expression.getKind() == Tree.Kind.MEMBER_SELECT
                || expression.getKind() == Tree.Kind.IDENTIFIER
                || expression.getKind() == Tree.Kind.ARRAY_ACCESS))
            // No receiver type for those
            return null;

        if (expression.getKind() == Tree.Kind.IDENTIFIER
            && "this".equals(expression.toString()))
            return null;

        ExpressionTree receiver = TreeUtils.skipParens(expression);
        if (receiver.getKind() == Tree.Kind.ARRAY_ACCESS)
            return getAnnotatedType(((ArrayAccessTree)receiver).getExpression());

        Element elem = InternalUtils.symbol(expression);
        if (elem == null || ElementUtils.isStatic(elem))
            return null;

        // Avoid int.class
        if (expression.getKind() == Tree.Kind.MEMBER_SELECT &&
                ((MemberSelectTree)expression).getExpression() instanceof PrimitiveTypeTree)
            return null;

        if (TreeUtils.isSelfAccess(expression)) {
            return getImplicitReceiverType(expression);
        }
        //
        // Trying to handle receiver calls to trees of the form
        // ((m).getArray())
        // returns the type of 'm' in this case

        if (receiver.getKind() == Tree.Kind.METHOD_INVOCATION)
            receiver = ((MethodInvocationTree)receiver).getMethodSelect();
        receiver = TreeUtils.skipParens(receiver);
        assert receiver.getKind() == Tree.Kind.MEMBER_SELECT;
        if (receiver.getKind() == Tree.Kind.MEMBER_SELECT)
            receiver = ((MemberSelectTree)receiver).getExpression();

        return getAnnotatedType(receiver);
    }

    /**
     * Determines the type of the invoked method based on the passed method
     * invocation tree.
     *
     * The returned method type has all type variables resolved, whether based
     * on receiver type, passed type parameters if any, and method invocation
     * parameter.
     *
     * Subclasses may override this method to customize inference of types
     * or qualifiers based on method invocation parameters.
     *
     * As an implementation detail, this method depends on
     * {@link AnnotatedTypes#asMemberOf(AnnotatedTypeMirror, Element)}, and
     * customization based on receiver type should be in accordance to its
     * specification.
     *
     * @param tree  the method invocation tree
     * @return the method type being invoked with tree
     */
    public AnnotatedExecutableType methodFromUse(MethodInvocationTree tree) {
        ExecutableElement methodElt = TreeUtils.elementFromUse(tree);
        AnnotatedTypeMirror type = getReceiver(tree);
        AnnotatedExecutableType methodType =
            atypes.asMemberOf(type, methodElt);

        Map<AnnotatedTypeVariable, AnnotatedTypeMirror> typeVarMapping =
            atypes.findTypeArguments(tree);

        if (!typeVarMapping.isEmpty()) {
            methodType = methodType.substitute(typeVarMapping);
        }

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
        AnnotatedTypeMirror type = fromNewClass(tree);
        annotateImplicit(tree.getIdentifier(), type);
        AnnotatedExecutableType con = atypes.asMemberOf(type, ctor);
        if (tree.getArguments().size() == con.getParameterTypes().size() + 1
            && isSyntheticArgument(tree.getArguments().get(0))) {
            // happens for anonymous constructors of inner classes
            List<AnnotatedTypeMirror> actualParams = new ArrayList<AnnotatedTypeMirror>();
            actualParams.add(getAnnotatedType(tree.getArguments().get(0)));
            actualParams.addAll(con.getParameterTypes());
            con.setParameterTypes(actualParams);
        }
        return con;
    }

    private boolean isSyntheticArgument(Tree tree) {
        return tree.toString().contains("<*nullchk*>");
    }

    public AnnotatedDeclaredType fromNewClass(NewClassTree tree) {
        if (!TreeUtils.isDiamondTree(tree))
            return (AnnotatedDeclaredType)fromTypeTree(tree.getIdentifier());

        AnnotatedDeclaredType type = (AnnotatedDeclaredType)toAnnotatedType(((JCTree)tree).type);
        if (tree.getIdentifier().getKind() == Tree.Kind.ANNOTATED_TYPE)
            type.addAnnotations(InternalUtils.annotationsFromTree((AnnotatedTypeTree)tree));
        return type;
    }

    /**
     * returns the annotated boxed type of the given primitive type.
     * The returned type would only have the annotations on the given type.
     *
     * Subclasses may override this method safely to override this behavior.
     *
     * @param type  the primitive type
     * @return the boxed declared type of the passed primitive type
     */
    public AnnotatedDeclaredType getBoxedType(AnnotatedPrimitiveType type) {
        TypeElement typeElt = types.boxedClass(type.getUnderlyingType());
        AnnotatedDeclaredType dt = fromElement(typeElt);
        dt.addAnnotations(type.getAnnotations());
        return dt;
    }

    /**
     * returns the annotated primitive type of the given declared type
     * if it is a boxed declared type.  Otherwise, it throws
     * <i>IllegalArgumentException</i> exception.
     *
     * The returned type would have the annotations on the given type and
     * nothing else.
     *
     * @param type  the declared type
     * @return the unboxed primitive type
     * @throws IllegalArgumentException if the type given has no unbox conversion
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

    /**
     * Returns the VisitorState instance used by the factory to infer types
     */
    public VisitorState getVisitorState() {
        return this.visitorState;
    }

    // **********************************************************************
    // random methods wrapping #getAnnotatedType(Tree) and #fromElement(Tree)
    // with appropriate casts to reduce casts on the client side
    // **********************************************************************

    /**
     * @see #getAnnotatedType(Tree)
     */
    public final AnnotatedDeclaredType getAnnotatedType(ClassTree tree) {
        return (AnnotatedDeclaredType)getAnnotatedType((Tree)tree);
    }

    /**
     * @see #getAnnotatedType(Tree)
     */
    public final AnnotatedDeclaredType getAnnotatedType(NewClassTree tree) {
        return (AnnotatedDeclaredType)getAnnotatedType((Tree)tree);
    }

    /**
     * @see #getAnnotatedType(Tree)
     */
    public final AnnotatedArrayType getAnnotatedType(NewArrayTree tree) {
        return (AnnotatedArrayType)getAnnotatedType((Tree)tree);
    }

    /**
     * @see #getAnnotatedType(Tree)
     */
    public final AnnotatedExecutableType getAnnotatedType(MethodTree tree) {
        return (AnnotatedExecutableType)getAnnotatedType((Tree)tree);
    }

    public final AnnotatedTypeMirror getAnnotatedType(VariableTree tree) {
        return getAnnotatedType((Tree)tree);
    }

    public final AnnotatedTypeMirror getAnnotatedType(ExpressionTree tree) {
        return getAnnotatedType((Tree)tree);
    }

    /**
     * @see #getAnnotatedType(Element)
     */
    public final AnnotatedDeclaredType getAnnotatedType(TypeElement elt) {
        return (AnnotatedDeclaredType)getAnnotatedType((Element)elt);
    }

    /**
     * @see #getAnnotatedType(Element)
     */
    public final AnnotatedExecutableType getAnnotatedType(ExecutableElement elt) {
        return (AnnotatedExecutableType)getAnnotatedType((Element)elt);
    }

    /**
     * @see #getAnnotatedType(Element)
     */
    public final AnnotatedDeclaredType fromElement(TypeElement elt) {
        return (AnnotatedDeclaredType)fromElement((Element)elt);
    }

    /**
     * @see #getAnnotatedType(Element)
     */
    public final AnnotatedExecutableType fromElement(ExecutableElement elt) {
        return (AnnotatedExecutableType)fromElement((Element)elt);
    }

    // **********************************************************************
    // Helper methods for this classes
    // **********************************************************************

    /** Memoization for isRecognizedAnnotation(). set by the constructor */
    private final Set<Name> supportedQuals;


    /**
     * Creates the set of the names of the recognized type qualifiers in the
     * current checker.
     *
     * @return a set of the name of the recognized qualifier.
     */
    private Set<Name> getSupportedQualifiers() {
        if (qualHierarchy != null)
            return qualHierarchy.getTypeQualifiers();
        return Collections.emptySet();
    }

    /**
     * Determines whether the given annotation is a part of the type system
     * under which this type factory operates.
     *
     * @param a any annotation
     * @return true if that annotation is part of the type system under which
     *         this type factory operates, false otherwise
     */
    /*package-scope*/ boolean isSupportedQualifier(AnnotationMirror a) {
        if (supportedQuals.isEmpty()) {
            // Only include with retention
            TypeElement elt = (TypeElement)a.getAnnotationType().asElement();
            Retention retention = elt.getAnnotation(Retention.class);
            return (retention == null) || retention.value() != RetentionPolicy.SOURCE;
        }
        Name name = AnnotationUtils.annotationName(a);
        return supportedQuals.contains(name);
    }

    /** Add the annotation clazz as an alias for the annotation type. */
    protected void addAliasedAnnotation(Class<?> clazz, AnnotationMirror type) {
        aliases.put(clazz.getCanonicalName(), type);
    }

    /**
     * Returns the canonical annotation for the passed annotation if it is
     * an alias of a canonical one in the framework.  If it is not an alias,
     * the method returns null.
     *
     * Returns an aliased type of the current one
     */
    protected AnnotationMirror aliasedAnnotation(AnnotationMirror a) {
        TypeElement elem = (TypeElement)a.getAnnotationType().asElement();
        String qualName = elem.getQualifiedName().toString();
        return aliases.get(qualName);
    }

    /**
     * A convenience method that converts a {@link TypeMirror} to an {@link
     * AnnotatedTypeMirror} using {@link AnnotatedTypeMirror#create}.
     *
     * @param t the {@link TypeMirror}
     * @return an {@link AnnotatedTypeMirror} that has {@code t} as its
     * underlying type
     */
    /*package-scope*/ final AnnotatedTypeMirror toAnnotatedType(TypeMirror t) {
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
    /*package-scope*/ AnnotatedTypeMirror type(Tree node) {

        // Attempt to obtain the type via JCTree.
        if (((JCTree)node).type != null) {
            AnnotatedTypeMirror result = toAnnotatedType(((JCTree)node).type);
            return result;
        }

        // Attempt to obtain the type via TreePath (slower).
        TreePath path = this.getPath(node);
        assert path != null : "no path or type in tree";

        TypeMirror t = trees.getTypeMirror(path);
        assert validType(t) : node + " --> " + t;

        return toAnnotatedType(t);
    }

    /**
     * Returns the type qualifiers that are least upper bound for c1 and c2
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
     * Delegates the call to
     * {@link QualifierHierarchy#leastUpperBound(Collection, Collection)}.
     *
     * @param c1    type qualifiers for the first type
     * @param c2    type qualifiers for the second type
     * @return  the least restrictive qualifiers for both types
     */
    protected Collection<AnnotationMirror> unify(Collection<AnnotationMirror> c1,
            Collection<AnnotationMirror> c2) {
        if (qualHierarchy == null) {
            // return the intersection
            Set<AnnotationMirror> intersection = AnnotationUtils.createAnnotationSet();
            intersection.addAll(c1);
            intersection.retainAll(c2);
            return intersection;
        }
        return qualHierarchy.leastUpperBound(c1, c2);
    }

    public QualifierHierarchy getQualifierHierarchy() {
        return this.qualHierarchy;
    }

    /**
     * Gets the declaration tree for the element, if the source is available.
     *
     * @param elt   an element
     * @return the tree declaration of the element if found
     */
    protected final Tree declarationFromElement(Element elt) {
        // if root is null, we cannot find any declaration
        if (root == null)
            return null;

        if (elementToTreeCache.containsKey(elt))
            return elementToTreeCache.get(elt);
        // TODO: handle type parameter declarations?
        Tree fromElt;
        // Prevent calling declarationFor on elements we know we don't have
        // the tree for

        switch (elt.getKind()) {
        case CLASS:
        case ENUM:
        case INTERFACE:
        case ANNOTATION_TYPE:
        case FIELD:
        case ENUM_CONSTANT:
        case METHOD:
        case CONSTRUCTOR:
            fromElt = trees.getTree(elt);
            break;
        default:
            fromElt = TreeInfo.declarationFor((Symbol)elt, (JCTree)root);
            break;
        }
        if (SHOULD_CACHE)
            elementToTreeCache.put(elt, fromElt);
        return fromElt;
    }

    /**
     * Returns the current class type being visited by the visitor.  The method
     * uses the parameter only if the most enclosing class cannot be found
     * directly.
     *
     * @return type of the most enclosing class being visited
     */
    // This method is used to wrap access to visitorState
    protected final AnnotatedDeclaredType getCurrentClassType(Tree tree) {
        if (visitorState.getClassType() != null) {
            return visitorState.getClassType();
        }
        ClassTree enclosingClass = TreeUtils.enclosingClass(getPath(tree));
        return getAnnotatedType(enclosingClass);
    }

    /**
     * Returns the receiver type of the current method being visited, and
     * returns null if the visited tree is not within a method.
     *
     * The method uses the parameter only if the most enclosing method cannot
     * be found directly.
     *
     * @return receiver type of the most enclosing method being visited.
     */
    protected final AnnotatedDeclaredType getCurrentMethodReceiver(Tree tree) {
        if (visitorState.getClassType() != null) {
            return visitorState.getMethodReceiver();
        }

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(getPath(tree));
        if (enclosingMethod == null)
            return null;
        AnnotatedExecutableType method = getAnnotatedType(enclosingMethod);
        return method.getReceiverType();
    }

    protected final boolean isWithinConstructor(Tree tree) {
        if (visitorState.getClassType() != null)
            return visitorState.getMethodTree() != null
                && TreeUtils.isConstructor(visitorState.getMethodTree());

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(getPath(tree));
        return enclosingMethod != null && TreeUtils.isConstructor(enclosingMethod);
    }

    private final Element getMostInnerClassOrMethod(Tree tree) {
        if (visitorState.getMethodTree() != null)
            return TreeUtils.elementFromDeclaration(visitorState.getMethodTree());
        if (visitorState.getClassTree() != null)
            return TreeUtils.elementFromDeclaration(visitorState.getClassTree());

        TreePath path = getPath(tree);
        if (path == null) {
            throw new AssertionError(String.format("getPath(tree)=>null%n  TreePath.getPath(root, tree)=>%s\n  for tree (%s) = %s%n  root=%s",
                                                   TreePath.getPath(root, tree), tree.getClass(), tree, root));
        }
        for (Tree pathTree : path) {
            if (pathTree instanceof MethodTree)
                return TreeUtils.elementFromDeclaration((MethodTree)pathTree);
            else if (pathTree instanceof ClassTree)
                return TreeUtils.elementFromDeclaration((ClassTree)pathTree);
        }

        throw new AssertionError("Cannot be here!");
    }

    /**
     * Gets the path for the given {@link Tree} under the current root by
     * checking from the visitor's current path, and only using
     * {@link Trees#getPath(CompilationUnitTree, Tree)} (which is much slower)
     * only if {@code node} is not found on the current path.
     *
     * @param node the {@link Tree} to get the path for
     * @return the path for {@code node} under the current root
     */
    public final TreePath getPath(Tree node) {
        assert root != null : "root needs to be set when used on trees";

        if (node == null) return null;
        TreePath currentPath = visitorState.getPath();
        if (currentPath == null)
            return TreePath.getPath(root, node);

        // This method uses multiple heuristics to avoid calling
        // TreePath.getPath()

        // If the current path you are visiting is for this node we are done
        if (currentPath.getLeaf() == node) {
            return currentPath;
        }

        //
        // When running on Daikon, we noticed that a lot of calls happened
        // within a small subtree containing the node we are currently visiting

        // When testing on Daikon, two steps resulted in the best performance
        if (currentPath.getParentPath() != null)
            currentPath = currentPath.getParentPath();
        if (currentPath.getLeaf() == node) {
            return currentPath;
        }
        if (currentPath.getParentPath() != null)
            currentPath = currentPath.getParentPath();
        if (currentPath.getLeaf() == node) {
            return currentPath;
        }

        final TreePath pathWithinSubtree = TreePath.getPath(currentPath, node);
        if (pathWithinSubtree != null) {
            return pathWithinSubtree;
        }

        // climb the current path till we see that
        // Works when getPath called on the enclosing method, enclosing
        // class
        TreePath current = currentPath;
        while (current != null) {
            if (current.getLeaf() == node)
                return current;
            current = current.getParentPath();
        }

        // OK, we give up. do a full scan
        return TreePath.getPath(root, node);
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
                return super.visitExecutable(type, p);
            }

        }.visit(type);
    }

    /**
     * Assert that the type is a type of valid type mirror, i.e. not an ERROR
     * or OTHER type.
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

    private Map<Element, AnnotatedTypeMirror> buildIndexTypes() {
        Map<Element, AnnotatedTypeMirror> result =
            new HashMap<Element, AnnotatedTypeMirror>();

        InputStream in = null;
        if (checkerClass != null)
            in = checkerClass.getResourceAsStream("jdk.astub");
        if (in != null) {
            StubParser stubParser = new StubParser(in, this, env);
            stubParser.parse(result);
        }

        String stubFiles = env.getOptions().get("stubs");
        if (stubFiles == null)
            stubFiles = System.getProperty("stubs");
        if (stubFiles == null)
            stubFiles = System.getenv("stubs");

        if (stubFiles == null)
            return result;

        String[] stubArray = stubFiles.split(File.pathSeparator);
        for (String stubPath : stubArray) {
            try {
                // Handle case when running in jtreg
                String base = System.getProperty("test.src");
                if (base != null)
                    stubPath = base + "/" + stubPath;
                List<File> stubs = StubUtil.allStubFiles(stubPath);
                for (File f : stubs) {
                    InputStream stubStream = new FileInputStream(f);
                    StubParser stubParser = new StubParser(stubStream, this, env);
                    stubParser.parse(result);
                }
            } catch (FileNotFoundException e) {
                System.err.println("Couldn't find stub file named: " + stubPath);
            }
        }

        return result;
    }
}
