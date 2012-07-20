package checkers.types;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import checkers.basetype.BaseTypeChecker;
import checkers.javari.quals.Mutable;
import checkers.nullness.quals.Nullable;
import checkers.quals.StubFiles;
import checkers.quals.Unqualified;
import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeMirror.*;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.TypeFromTree;
import checkers.types.visitors.AnnotatedTypeScanner;
import checkers.util.*;
import checkers.util.stub.StubParser;
import checkers.util.stub.StubUtil;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;

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

    /** The state of the visitor. **/
    final protected VisitorState visitorState;

    /** Represent the annotation relations. **/
    protected final /*@Nullable*/ QualifierHierarchy qualHierarchy;

    /** Types read from stub files (but not those from the annotated JDK jar file). */
    // Initially null, then assigned in postInit().  Caching is enabled as
    // soon as this is non-null, so it should be first set to its final
    // value, not initialized to an empty map that is incrementally filled.
    private Map<Element, AnnotatedTypeMirror> indexTypes;

    /**
     * Declaration annotations read from stub files (but not those from the annotated JDK jar file).
     * Map keys cannot be Element, because a different Element appears
     * in the stub files than in the real files.  So, map keys are the
     * verbose element name, as returned by ElementUtils.getVerboseName.
     */
    // Not final, because it is assigned in postInit().
    private Map<String, Set<AnnotationMirror>> indexDeclAnnos;

    private final Class<? extends SourceChecker> checkerClass;

    /** @see #canHaveAnnotatedTypeParameters() */
    private final boolean annotatedTypeParams;

    /**
     * Map from class name (canonical name) of an annotation, to the
     * annotation in the Checker Framework that will be used in its place.
     */
    private final Map<String, AnnotationMirror> aliases = new HashMap<String, AnnotationMirror>();

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
    public AnnotatedTypeFactory(SourceChecker checker, /*@Nullable*/ CompilationUnitTree root) {
        this(checker.getProcessingEnvironment(),
                (checker instanceof BaseTypeChecker) ? ((BaseTypeChecker)checker).getQualifierHierarchy() : null,
                root,
                checker == null ? null : checker.getClass());
    }

    /** A subclass must call postInit at the end of its constructor. */
    public AnnotatedTypeFactory(ProcessingEnvironment env,
                                /*@Nullable*/ QualifierHierarchy qualHierarchy,
                                /*@Nullable*/ CompilationUnitTree root,
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
        this.indexTypes = null; // will be set by postInit()
        this.indexDeclAnnos = null; // will be set by postInit()
        // TODO: why is the option not used?
        this.annotatedTypeParams = true; // env.getOptions().containsKey("annotatedTypeParams");
    }

    /**
     * Actions that logically belong in the constructor, but need to run
     * after the subclass constructor has completed.  In particular,
     * buildIndexTypes may try to do type resolution with this
     * AnnotatedTypeFactory.
     */
    protected void postInit() {
        buildIndexTypes();
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

    /** Should results be cached? Disable for better debugging. */
    private final static boolean SHOULD_CACHE = true;

    /** Size of LRU cache. */
    private final static int CACHE_SIZE = 300;

    /** Mapping from a Tree to its annotated type; implicits have been applied. */
    private final Map<Tree, AnnotatedTypeMirror> treeCache = createLRUCache(CACHE_SIZE);

    /** Mapping from a Tree to its annotated type; before implicits are applied,
     * just what the programmer wrote. */
    protected final Map<Tree, AnnotatedTypeMirror> fromTreeCache = createLRUCache(CACHE_SIZE);

    /** Mapping from an Element to its annotated type; before implicits are applied,
     * just what the programmer wrote. */
    private final Map<Element, AnnotatedTypeMirror> elementCache = createLRUCache(CACHE_SIZE);

    /** Mapping from an Element to the source Tree of the declaration. */
    private final Map<Element, Tree> elementToTreeCache  = createLRUCache(CACHE_SIZE);

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
        if (elt == null) {
            SourceChecker.errorAbort("AnnotatedTypeFactory.getAnnotatedType: null element");
            return null; // dead code
        }
        AnnotatedTypeMirror type = fromElement(elt);
        annotateInheritedFromClass(type);
        annotateImplicit(elt, type);
        // System.out.println("AnnotatedTypeFactory::getAnnotatedType(Element) result: " + type);
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
     *
     * @see #fromClass(ClassTree)
     * @see #fromMember(Tree)
     * @see #fromExpression(ExpressionTree)
     */
    // I wish I could make this method protected
    public AnnotatedTypeMirror getAnnotatedType(Tree tree) {
        if (tree == null) {
            SourceChecker.errorAbort("AnnotatedTypeFactory.getAnnotatedType: null tree");
            return null; // dead code
        }
        if (treeCache.containsKey(tree))
            return AnnotatedTypes.deepCopy(treeCache.get(tree));

        AnnotatedTypeMirror type;
        switch (tree.getKind()) {
            case CLASS:
            case ENUM:
            case INTERFACE:
            case ANNOTATION_TYPE:
                type = fromClass((ClassTree)tree);
                break;
            case METHOD:
            case VARIABLE:
                type = fromMember(tree);
                break;
            default:
                if (tree instanceof ExpressionTree) {
                    type = fromExpression((ExpressionTree)tree);
                } else {
                    SourceChecker.errorAbort(
                        "AnnotatedTypeFactory.getAnnotatedType: query of annotated type for tree " + tree.getKind());
                    type = null; // dead code
                }
        }

        if (TreeUtils.isExpressionTree(tree)) {
            tree = TreeUtils.skipParens((ExpressionTree)tree);
        }

        annotateImplicit(tree, type);

        switch (tree.getKind()) {
        case CLASS:
        case ENUM:
        case INTERFACE:
        case ANNOTATION_TYPE:
        case METHOD:
        // case VARIABLE:
            if (SHOULD_CACHE)
                treeCache.put(tree, AnnotatedTypes.deepCopy(type));
        }
        // System.out.println("AnnotatedTypeFactory::getAnnotatedType(Tree) result: " + type);
        return type;
    }

    /**
     * Get the defaulted type of a variable, without considering
     * flow inference from the initializer expression.
     * This is needed to determine the type of the assignment context,
     * which should have the "default" meaning, without flow inference.
     * TODO: describe and generalize
     */
    public AnnotatedTypeMirror getDefaultedAnnotatedType(VariableTree tree) {
        return null;
    }

    /**
     * Determines the annotated type from a type in tree form.
     *
     * @param tree the type tree
     * @return the annotated type of the type in the AST
     */
    public AnnotatedTypeMirror getAnnotatedTypeFromTypeTree(Tree tree) {
        if (tree == null) {
            SourceChecker.errorAbort("AnnotatedTypeFactory.getAnnotatedTypeFromTypeTree: null tree");
            return null; // dead code
        }
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
        if (elementCache.containsKey(elt)) {
            return AnnotatedTypes.deepCopy(elementCache.get(elt));
        }
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
        } else if (decl.getKind() == Tree.Kind.TYPE_PARAMETER) {
            type = fromTypeTree(decl);
        } else {
            SourceChecker.errorAbort("AnnotatedTypeFactory.fromElement: cannot be here! decl: " + decl.getKind() +
                    " elt: " + elt, null);
            type = null; // dead code
        }

        // Caching is disabled if indexTypes == null, because calls to this
        // method before the stub files are fully read can return incorrect
        // results.
        if (SHOULD_CACHE && indexTypes != null)
            elementCache.put(elt, AnnotatedTypes.deepCopy(type));
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
            fromTreeWithVisitor(TypeFromTree.TypeFromClassINSTANCE, tree);
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
        if (!(tree instanceof MethodTree || tree instanceof VariableTree)) {
            SourceChecker.errorAbort("AnnotatedTypeFactory.fromMember: not a method or variable declaration");
            return null; // dead code
        }
        if (fromTreeCache.containsKey(tree)) {
            return AnnotatedTypes.deepCopy(fromTreeCache.get(tree));
        }
        AnnotatedTypeMirror result = fromTreeWithVisitor(
                TypeFromTree.TypeFromMemberINSTANCE, tree);
        annotateInheritedFromClass(result);
        if (SHOULD_CACHE)
            fromTreeCache.put(tree, AnnotatedTypes.deepCopy(result));
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
            return AnnotatedTypes.deepCopy(fromTreeCache.get(tree));
        AnnotatedTypeMirror result = fromTreeWithVisitor(
                TypeFromTree.TypeFromExpressionINSTANCE, tree);
        annotateInheritedFromClass(result);
        if (SHOULD_CACHE)
            fromTreeCache.put(tree, AnnotatedTypes.deepCopy(result));
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
            return AnnotatedTypes.deepCopy(fromTreeCache.get(tree));

        AnnotatedTypeMirror result = fromTreeWithVisitor(
                TypeFromTree.TypeFromTypeTreeINSTANCE, tree);

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
                    AnnotatedTypeMirror upperBound = typeParamVar.getEffectiveUpperBound();
                    while (upperBound.getKind() == TypeKind.TYPEVAR)
                        upperBound = ((AnnotatedTypeVariable)upperBound).getEffectiveUpperBound();

                    WildcardType wc = env.getTypeUtils().getWildcardType(upperBound.getUnderlyingType(), null);
                    AnnotatedWildcardType wctype = (AnnotatedWildcardType) AnnotatedTypeMirror.createType(wc, env, this);
                    wctype.setElement(typeParam.getElement());
                    wctype.setExtendsBound(upperBound);
                    wctype.addAnnotations(typeParam.getAnnotations());
                    // This hack allows top-level wildcards to be supertypes of non-wildcards
                    // wctype.setMethodTypeArgHack();

                    typeArgs.add(wctype);
                }
                dt.setTypeArguments(typeArgs);
            }
        }
        annotateInheritedFromClass(result);
        if (SHOULD_CACHE)
            fromTreeCache.put(tree, AnnotatedTypes.deepCopy(result));
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
            SourceChecker.errorAbort("AnnotatedTypeFactory.fromTreeWithVisitor: null tree");
        if (converter == null)
            SourceChecker.errorAbort("AnnotatedTypeFactory.fromTreeWithVisitor: null visitor");
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
    protected void annotateImplicit(Tree tree, /*@Mutable*/ AnnotatedTypeMirror type) {
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
    protected void annotateImplicit(Element elt, /*@Mutable*/ AnnotatedTypeMirror type) {
        // Pass.
    }

    /**
     * A callback method for the AnnotatedTypeFactory subtypes to customize
     * directSuperTypes().  Overriding methods should merely change the
     * annotations on the supertypes, without adding or removing new types.
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
        // Use the effective annotations here to get the correct annotations
        // for type variables and wildcards.
        Set<AnnotationMirror> annotations = type.getEffectiveAnnotations();
        for (AnnotatedTypeMirror supertype : supertypes) {
            if (!annotations.equals(supertype.getEffectiveAnnotations())) {
                supertype.clearAnnotations();
                // TODO: is this correct for type variables and wildcards?
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
     * Adapt the upper bounds of the type variables of a class relative
     * to the type instantiation.
     * In some type systems, the upper bounds depend on the instantiation
     * of the class. For example, in the Generic Universe Type system,
     * consider a class declaration
     * <pre>   class C&lt;X extends @Peer Object&gt; </pre>
     * then the instantiation
     * <pre>   @Rep C&lt;@Rep Object&gt; </pre>
     * is legal. The upper bounds of class C have to be adapted
     * by the main modifier.
     *
     * <p>
     * TODO: ensure that this method is consistently used instead
     * of directly querying the type variables.
     *
     * @param type The use of the type
     * @param element The corresponding element
     * @return The adapted type variables
     */
    public List<AnnotatedTypeVariable> typeVariablesFromUse(
            AnnotatedDeclaredType type, TypeElement element) {

        AnnotatedDeclaredType generic = getAnnotatedType(element);
        List<AnnotatedTypeMirror> targs = type.getTypeArguments();
        List<AnnotatedTypeMirror> tvars = generic.getTypeArguments();
        Map<AnnotatedTypeVariable, AnnotatedTypeMirror> mapping =
                new HashMap<AnnotatedTypeVariable, AnnotatedTypeMirror>();

        List<AnnotatedTypeVariable> res = new LinkedList<AnnotatedTypeVariable>();

        assert targs.size() == tvars.size() : "Mismatch in type argument size between " + type + " and " + generic;
        for(int i=0; i<targs.size(); ++i) {
            mapping.put((AnnotatedTypeVariable)tvars.get(i), targs.get(i));
        }

        for (AnnotatedTypeMirror atm : tvars) {
            AnnotatedTypeVariable atv = (AnnotatedTypeVariable)atm;
            atv.setUpperBound(atv.getUpperBound().substitute(mapping));
            atv.setLowerBound(atv.getLowerBound().substitute(mapping));
            res.add(atv);
        }
        return res;
    }

    /**
     * Adds annotations to the type based on the annotations from its class
     * type if and only if no annotations are already present on the type.
     *
     * @param type the type for which class annotations will be inherited if
     * there are no annotations already present
     */
    protected void annotateInheritedFromClass(/*@Mutable*/ AnnotatedTypeMirror type) {
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
                    if (AnnotationUtils.hasInheritedMeta(anno)) {
                        type.addAnnotation(anno);
                    }
                }
            }

            return super.visitDeclared(type, p);
        }

        private final Map<TypeParameterElement, AnnotatedTypeVariable> visited =
                new HashMap<TypeParameterElement, AnnotatedTypeVariable>();

        @Override
        public Void visitTypeVariable(AnnotatedTypeVariable type, AnnotatedTypeFactory p) {
            TypeParameterElement tpelt = (TypeParameterElement) type.getUnderlyingType().asElement();
            if (!visited.containsKey(tpelt)) {
                visited.put(tpelt, type);
                if (!type.isAnnotated() &&
                        !type.getUpperBound().isAnnotated() &&
                        tpelt.getEnclosingElement().getKind()!=ElementKind.TYPE_PARAMETER) {
                        TypeFromElement.annotate(type, tpelt);
                }
                super.visitTypeVariable(type, p);
                visited.remove(tpelt);
            }
            return null;
        }
    }

    // **********************************************************************
    // Utilities method for getting specific types from trees or elements
    // **********************************************************************

    /**
     * Return the implicit receiver type of an expression tree.
     *
     * The result is null for expressions that don't have a receiver,
     * e.g. for a local variable or method parameter access.
     *
     * TODO: receiver annotations on outer this.
     * TODO: Better document the difference between getImplicitReceiverType and getSelfType?
     *
     * @param tree The expression that might have an implicit receiver.
     * @return The type of the receiver.
     */
    /* TODO: this method assumes that the tree is within the current
     * Compilation Unit. This assumption fails in testcase Bug109_A/B, where
     * a chain of dependencies leads into a different compilation unit.
     * I didn't find a way how to handle this better and conservatively
     * return null. See TODO comment below.
     *
     */
    protected AnnotatedDeclaredType getImplicitReceiverType(ExpressionTree tree) {
        assert (tree.getKind() == Tree.Kind.IDENTIFIER
                || tree.getKind() == Tree.Kind.MEMBER_SELECT
                || tree.getKind() == Tree.Kind.METHOD_INVOCATION
                || tree.getKind() == Tree.Kind.NEW_CLASS);

        Element element = InternalUtils.symbol(tree);
        assert element != null;
        // Return null if the element kind has no receiver.
        if (!ElementUtils.hasReceiver(element)) {
            return null;
        }

        ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
        if (receiver==null) {
            if (isMostEnclosingThisDeref(tree)) {
                // TODO: problem with ambiguity with implicit receivers.
                // We need a way to find the correct class. We cannot use the
                // element, as generics might have to be substituted in a subclass.
                // See GenericsEnclosing test case.
                // TODO: is this fixed?
                return getSelfType(tree);
            } else {
                TreePath path = getPath(tree);
                if (path == null) {
                    // The path is null if the field is in a compilation unit we haven't
                    // processed yet. TODO: is there a better way?
                    // This only arises in the Nullness Checker when substituting rawness.
                    return null;
                }
                TypeElement typeElt = ElementUtils.enclosingClass(element);
                if (typeElt == null) {
                    SourceChecker.errorAbort("AnnotatedTypeFactory.getImplicitReceiver: enclosingClass()==null for element: " + element);
                }
                // TODO: method receiver annotations on outer this
                return getEnclosingType(typeElt, tree);
            }
        }

        Element rcvelem = InternalUtils.symbol(receiver);
        assert rcvelem != null;

        if (!ElementUtils.hasReceiver(rcvelem)) {
            return null;
        }

        if ("this".contentEquals(receiver.toString())) {
            // TODO: also "super"?
            return this.getSelfType(tree);
        }

        TypeElement typeElt = ElementUtils.enclosingClass(rcvelem);
        if (typeElt == null) {
            SourceChecker.errorAbort("AnnotatedTypeFactory.getImplicitReceiver: enclosingClass()==null for element: " + rcvelem);
        }

        AnnotatedDeclaredType type = getAnnotatedType(typeElt);

        // TODO: go through _all_ enclosing methods to see whether any of them has a
        // receiver annotation of the correct type.
        // TODO: Can we reuse getSelfType for outer this accesses?

        AnnotatedDeclaredType methodReceiver = getCurrentMethodReceiver(tree);
        if (methodReceiver != null &&
                !(methodReceiver.getAnnotations().size()==1 && methodReceiver.getAnnotation(Unqualified.class)!=null)) {
            // TODO: this only takes the main annotations. What about other annotations?
            type.clearAnnotations();
            type.addAnnotations(methodReceiver.getAnnotations());
        }

        return type;
    }

    /**
     * Determine whether the tree dereferences the most enclosing "this" object.
     * That is, we have an expression like "f.g" and want to know whether it is
     * an access "this.f.g" or whether e.g. f is a field of an outer class or
     * e.g. f is a local variable.
     *
     * @param tree The tree to check.
     * @return True, iff the tree is an explicit or implicit reference to the
     *         most enclosing "this".
     */
    public final boolean isMostEnclosingThisDeref(ExpressionTree tree) {
        if (!isAnyEnclosingThisDeref(tree)) {
            return false;
        }

        Element element = TreeUtils.elementFromUse(tree);
        TypeElement typeElt = ElementUtils.enclosingClass(element);

        ClassTree enclosingClass = getCurrentClassTree(tree);
        if (enclosingClass != null && isSubtype(TreeUtils.elementFromDeclaration(enclosingClass), typeElt)) {
            return true;
        }

        // ran out of options
        return false;
    }

    /**
     * Determine whether the given expression is either "this" or an outer
     * "C.this".
     *
     * TODO: Should this also handle "super"?
     *
     * @param tree
     * @return
     */
    private final boolean isExplicitThisDereference(ExpressionTree tree) {
        if (tree.getKind() == Tree.Kind.IDENTIFIER
                && ((IdentifierTree)tree).getName().contentEquals("this")) {
            // Explicit this reference "this"
            return true;
        }

        if (tree.getKind() != Tree.Kind.MEMBER_SELECT) {
            return false;
        }

        MemberSelectTree memSelTree = (MemberSelectTree) tree;
        if (memSelTree.getIdentifier().contentEquals("this")) {
            // Outer this reference "C.this"
            return true;
        }
        return false;
    }

    /**
     * Does this expression have (the innermost or an outer) "this" as receiver?
     * Note that the receiver can be either explicit or implicit.
     *
     * @param tree The tree to test.
     * @return True, iff the expression uses (the innermost or an outer) "this" as receiver.
     */
    public final boolean isAnyEnclosingThisDeref(ExpressionTree tree) {
        if (!TreeUtils.isUseOfElement(tree)) {
            return false;
        }
        ExpressionTree recv = TreeUtils.getReceiverTree(tree);

        if (recv==null) {
            Element element = TreeUtils.elementFromUse(tree);

            if (!ElementUtils.hasReceiver(element)) {
                return false;
            }

            tree = TreeUtils.skipParens(tree);

            if (tree.getKind() == Tree.Kind.IDENTIFIER) {
                Name n = ((IdentifierTree)tree).getName();
                if ("this".contentEquals(n) ||
                        "super".contentEquals(n)) {
                    // An explicit reference to "this"/"super" has no receiver.
                    return false;
                }
            }
            // Must be some access through this.
            return true;
        } else if (!TreeUtils.isUseOfElement(recv)) {
            // The receiver is e.g. a String literal.
            return false;
            // TODO: I think this:
            //  (i==9 ? this : this).toString();
            // is not a use of an element, as the receiver is an
            // expression. How should this be handled?
        }

        Element element = TreeUtils.elementFromUse(recv);

        if (!ElementUtils.hasReceiver(element)) {
            return false;
        }

        return isExplicitThisDereference(recv);
    }

    /**
     * Returns the type of {@code this} in the current location, which can
     * be used if {@code this} has a special semantics (e.g. {@code this}
     * is non-null).
     *
     * The parameter is an arbitrary tree and does not have to mention "this",
     * neither explicitly nor implicitly.
     * This method should be overridden for type-system specific behavior.
     *
     * TODO: in 1.3, handle all receiver type annotations.
     * TODO: handle enclosing classes correctly.
     */
    public AnnotatedDeclaredType getSelfType(Tree tree) {
        AnnotatedDeclaredType type = getCurrentClassType(tree);
        AnnotatedDeclaredType methodReceiver = getCurrentMethodReceiver(tree);
        if (methodReceiver != null &&
                !(methodReceiver.getAnnotations().size()==1 && methodReceiver.getAnnotation(Unqualified.class)!=null)) {
            type.clearAnnotations();
            type.addAnnotations(methodReceiver.getAnnotations());
        }
        return type;
    }

    /**
     * Determine the type of the most enclosing class of the given tree that
     * is a subtype of the given element. Receiver type annotations of an
     * enclosing method are considered.
     */
    public AnnotatedDeclaredType getEnclosingType(TypeElement element, Tree tree) {
        Element enclosingElt = getMostInnerClassOrMethod(tree);

        while (enclosingElt != null) {
            if (enclosingElt instanceof ExecutableElement) {
                ExecutableElement method = (ExecutableElement)enclosingElt;
                if (method.asType() != null // XXX: hack due to a compiler bug
                        && isSubtype((TypeElement)method.getEnclosingElement(), element)) {
                    if (ElementUtils.isStatic(method)) {
                        // TODO: why not the type of the class?
                        return null;
                    } else {
                        return getAnnotatedType(method).getReceiverType();
                    }
                }
            } else if (enclosingElt instanceof TypeElement) {
                if (isSubtype((TypeElement)enclosingElt, element)) {
                    return (AnnotatedDeclaredType) getAnnotatedType(enclosingElt);
                }
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
     * @param expression The expression for which to determine the receiver type
     * @return  the type of the receiver of this expression
     */
    public final AnnotatedTypeMirror getReceiverType(ExpressionTree expression) {
        ExpressionTree receiver = TreeUtils.getReceiverTree(expression);

        if (this.isAnyEnclosingThisDeref(expression)) {
            return getImplicitReceiverType(expression);
        }

        if (receiver!=null) {
            return getAnnotatedType(receiver);
        } else {
            // E.g. local variables
            return null;
        }
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
     * The return type is a pair of the type of the invoked method and
     * the (inferred) type arguments.
     * Note that neither the explicitly passed nor the inferred type arguments
     * are guaranteed to be subtypes of the corresponding upper bounds.
     * See method
     * {@link checkers.basetype.BaseTypeVisitor#checkTypeArguments(Tree, List, List, List)}
     * for the checks of type argument well-formedness.
     *
     * @param tree the method invocation tree
     * @return the method type being invoked with tree and the (inferred) type arguments
     */
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(MethodInvocationTree tree) {
        ExecutableElement methodElt = TreeUtils.elementFromUse(tree);
        AnnotatedTypeMirror receiverType = getReceiverType(tree);
        AnnotatedExecutableType methodType = atypes.asMemberOf(receiverType, methodElt);
        List<AnnotatedTypeMirror> typeargs = new LinkedList<AnnotatedTypeMirror>();

        Map<AnnotatedTypeVariable, AnnotatedTypeMirror> typeVarMapping =
            atypes.findTypeArguments(tree);

        if (!typeVarMapping.isEmpty()) {
            for ( AnnotatedTypeVariable tv : methodType.getTypeVariables()) {
                if (typeVarMapping.get(tv)==null) {
                    System.err.println("Detected a mismatch between the declared method" +
                            " type variables and the inferred method type arguments. Something is going wrong!");
                    System.err.println("Method type variables: " + methodType.getTypeVariables());
                    System.err.println("Inferred method type arguments: " + typeVarMapping);
                    SourceChecker.errorAbort("AnnotatedTypeFactory.methodFromUse: mismatch between declared method type variables and the inferred method type arguments!");
                }
                typeargs.add(typeVarMapping.get(tv));
            }
            methodType = methodType.substitute(typeVarMapping);
        }

        return Pair.of(methodType, typeargs);
    }

    /**
     * Determines the {@link AnnotatedExecutableType} of a constructor
     * invocation. Note that this is different than calling
     * {@link #getAnnotatedType(Tree)} or
     * {@link #fromExpression(ExpressionTree)} on the constructor invocation;
     * those determine the type of the <i>result</i> of invoking the
     * constructor, which is probably an {@link AnnotatedDeclaredType}.
     *
     * @param tree the constructor invocation tree
     * @return the annotated type of the invoked constructor (as an executable
     *         type) and the (inferred) type arguments
     */
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> constructorFromUse(NewClassTree tree) {
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

        List<AnnotatedTypeMirror> typeargs = new LinkedList<AnnotatedTypeMirror>();

        Map<AnnotatedTypeVariable, AnnotatedTypeMirror> typeVarMapping =
            atypes.findTypeArguments(tree);

        if (!typeVarMapping.isEmpty()) {
            for ( AnnotatedTypeVariable tv : con.getTypeVariables()) {
                typeargs.add(typeVarMapping.get(tv));
            }
            con = con.substitute(typeVarMapping);
        }

        return Pair.of(con, typeargs);
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
            types.unboxedType(type.getUnderlyingType());
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
    public boolean isSupportedQualifier(AnnotationMirror a) {
        if (a!=null && supportedQuals.isEmpty()) {
            // Only include with retention
            // TODO: what is the logic behind this?? Why does Retention matter?
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
        if (a==null) return null;
        TypeElement elem = (TypeElement)a.getAnnotationType().asElement();
        String qualName = elem.getQualifiedName().toString();
        return aliases.get(qualName);
    }

    /**
     * A convenience method that converts a {@link TypeMirror} to an {@link
     * AnnotatedTypeMirror} using {@link AnnotatedTypeMirror#createType}.
     *
     * @param t the {@link TypeMirror}
     * @return an {@link AnnotatedTypeMirror} that has {@code t} as its
     * underlying type
     */
    public final AnnotatedTypeMirror toAnnotatedType(TypeMirror t) {
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
     * {@link QualifierHierarchy#leastUpperBounds(Collection, Collection)}.
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
        return qualHierarchy.leastUpperBounds(c1, c2);
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
        if (elementToTreeCache.containsKey(elt)) {
            return elementToTreeCache.get(elt);
        }
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
    protected final ClassTree getCurrentClassTree(Tree tree) {
        if (visitorState.getClassTree() != null) {
            return visitorState.getClassTree();
        }
        return TreeUtils.enclosingClass(getPath(tree));
    }

    protected final AnnotatedDeclaredType getCurrentClassType(Tree tree) {
        return getAnnotatedType(getCurrentClassTree(tree));
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
        AnnotatedDeclaredType res = visitorState.getMethodReceiver();
        if (res == null) {
            MethodTree enclosingMethod = TreeUtils.enclosingMethod(getPath(tree));
            if (enclosingMethod != null) {
                AnnotatedExecutableType method = getAnnotatedType(enclosingMethod);
                res = method.getReceiverType();
                // TODO: three tests fail if one adds the following, which would make
                // sense, or not?
                // visitorState.setMethodReceiver(res);
            }
        }
        return res;
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
            SourceChecker.errorAbort(String.format("AnnotatedTypeFactory.getMostInnerClassOrMethod: getPath(tree)=>null%n  TreePath.getPath(root, tree)=>%s\n  for tree (%s) = %s%n  root=%s",
                                                   TreePath.getPath(root, tree), tree.getClass(), tree, root));
        }
        for (Tree pathTree : path) {
            if (pathTree instanceof MethodTree)
                return TreeUtils.elementFromDeclaration((MethodTree)pathTree);
            else if (pathTree instanceof ClassTree)
                return TreeUtils.elementFromDeclaration((ClassTree)pathTree);
        }

        SourceChecker.errorAbort("AnnotatedTypeFactory.getMostInnerClassOrMethod: cannot be here!");
        return null; // dead code
    }

    /**
     * Gets the path for the given {@link Tree} under the current root by
     * checking from the visitor's current path, and only using
     * {@link Trees#getPath(CompilationUnitTree, Tree)} (which is much slower)
     * only if {@code node} is not found on the current path.
     *
     * Note that the given Tree has to be within the current compilation unit,
     * otherwise null will be returned.
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

        // OK, we give up. Do a full scan.
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

    /** Sets indexTypes and indexDeclAnnos by side effect, just before returning. */
    private void buildIndexTypes() {
        if (this.indexTypes != null || this.indexDeclAnnos != null) {
            SourceChecker.errorAbort("AnnotatedTypeFactory.buildIndexTypes called more than once");
        }

        Map<Element, AnnotatedTypeMirror> indexTypes
            = new HashMap<Element, AnnotatedTypeMirror>();
        Map<String, Set<AnnotationMirror>> indexDeclAnnos
            = new HashMap<String, Set<AnnotationMirror>>();

        if (!env.getOptions().containsKey("ignorejdkastub")) {
            InputStream in = null;
            if (checkerClass != null)
                in = checkerClass.getResourceAsStream("jdk.astub");
            if (in != null) {
                StubParser stubParser = new StubParser("jdk.astub", in, this, env);
                stubParser.parse(indexTypes, indexDeclAnnos);
            }
        }

        String allstubFiles = "";
        String stubFiles;

        stubFiles = env.getOptions().get("stubs");
        if (stubFiles != null)
            allstubFiles += File.pathSeparator + stubFiles;

        stubFiles = System.getProperty("stubs");
        if (stubFiles != null)
            allstubFiles += File.pathSeparator + stubFiles;

        stubFiles = System.getenv("stubs");
        if (stubFiles != null)
            allstubFiles += File.pathSeparator + stubFiles;

        {
            StubFiles sfanno = checkerClass.getAnnotation(StubFiles.class);
            if (sfanno!=null) {
                String[] sfarr = sfanno.value();
                stubFiles = "";
                for (String sf : sfarr) {
                    stubFiles += File.pathSeparator + sf;
                }
                allstubFiles += stubFiles;
            }
        }

        if (allstubFiles.isEmpty()) {
            this.indexTypes = indexTypes;
            this.indexDeclAnnos = indexDeclAnnos;
            return;
        }

        String[] stubArray = allstubFiles.split(File.pathSeparator);
        for (String stubPath : stubArray) {
            if (stubPath==null || stubPath.isEmpty()) continue;
            try {
                // Handle case when running in jtreg
                String base = System.getProperty("test.src");
                if (base != null)
                    stubPath = base + "/" + stubPath;
                List<File> stubs = StubUtil.allStubFiles(stubPath);
                if (stubs.size()==0) {
                    InputStream in = null;
                    if (checkerClass != null)
                        in = checkerClass.getResourceAsStream(stubPath);
                    if (in != null) {
                        StubParser stubParser = new StubParser(stubPath, in, this, env);
                        stubParser.parse(indexTypes, indexDeclAnnos);
                        // We could handle the stubPath -> continue.
                        continue;
                    }
                    // We couldn't handle the stubPath -> error message.
                    System.err.println("Did not find stub file or files within directory: " + stubPath);
                }

                for (File f : stubs) {
                    InputStream stubStream = new FileInputStream(f);
                    StubParser stubParser = new StubParser(f.getAbsolutePath(), stubStream, this, env);
                    stubParser.parse(indexTypes, indexDeclAnnos);
                }
            } catch (FileNotFoundException e) {
                System.err.println("Couldn't find stub file named: " + stubPath);
            }
        }

        this.indexTypes = indexTypes;
        this.indexDeclAnnos = indexDeclAnnos;
        return;
    }

    /**
     * Returns the actual annotation mirror used to annotate this type,
     * whose name equals the passed annotationName if one exists, null otherwise.
     *
     * @param anno annotation class
     * @return the annotation mirror for anno
     */
    public AnnotationMirror getDeclAnnotation(Element elt, Class<? extends Annotation> anno) {
        String aname = anno.getCanonicalName();

        // First look in the stub files.
        String eltName = ElementUtils.getVerboseName(elt);
        Set<AnnotationMirror> stubAnnos = indexDeclAnnos.get(eltName);

        if (stubAnnos != null) {
            for (AnnotationMirror am : stubAnnos) {
                if (sameAnnotation(am, aname)) {
                    return am;
                }
            }
        }

        // Then look at the real annotations.
        for (AnnotationMirror am : elt.getAnnotationMirrors()) {
            if (sameAnnotation(am, aname)) {
                return am;
            }
        }

        // Not found in either location
        return null;
    }

    // Checks the annotation name, but not its arguments
    private boolean sameAnnotation(AnnotationMirror am, String aname) {
        Name amname = AnnotationUtils.annotationName(am);
        return amname.toString().equals(aname);
    }

    // Checks the annotation name, but not its arguments
    private boolean sameAnnotation(AnnotationMirror am, Class<? extends Annotation> anno) {
        return sameAnnotation(am, anno.getCanonicalName());
    }

}
