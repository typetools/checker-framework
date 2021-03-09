package org.checkerframework.javacutil;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotatedType;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCLambda.ParameterKind;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.checker.interning.qual.PolyInterned;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.dataflow.qual.Pure;
import org.plumelib.util.UniqueIdMap;

/**
 * Utility methods for analyzing a javac {@code Tree}.
 *
 * @see TreePathUtil
 */
public final class TreeUtils {

    // Class cannot be instantiated.
    private TreeUtils() {
        throw new AssertionError("Class TreeUtils cannot be instantiated.");
    }

    /** Unique IDs for trees. */
    public static final UniqueIdMap<Tree> treeUids = new UniqueIdMap<>();

    /**
     * Checks if the provided method is a constructor method or no.
     *
     * @param tree a tree defining the method
     * @return true iff tree describes a constructor
     */
    public static boolean isConstructor(final MethodTree tree) {
        return tree.getName().contentEquals("<init>");
    }

    /**
     * Checks if the method invocation is a call to super.
     *
     * @param tree a tree defining a method invocation
     * @return true iff tree describes a call to super
     */
    public static boolean isSuperConstructorCall(MethodInvocationTree tree) {
        return isNamedMethodCall("super", tree);
    }

    /**
     * Checks if the method invocation is a call to "this".
     *
     * @param tree a tree defining a method invocation
     * @return true iff tree describes a call to this
     */
    public static boolean isThisConstructorCall(MethodInvocationTree tree) {
        return isNamedMethodCall("this", tree);
    }

    /**
     * Checks if the method call is a call to the given method name.
     *
     * @param name a method name
     * @param tree a tree defining a method invocation
     * @return true iff tree is a call to the given method
     */
    private static boolean isNamedMethodCall(String name, MethodInvocationTree tree) {
        return getMethodName(tree.getMethodSelect()).equals(name);
    }

    /**
     * Returns true if the tree is a tree that 'looks like' either an access of a field or an
     * invocation of a method that are owned by the same accessing instance.
     *
     * <p>It would only return true if the access tree is of the form:
     *
     * <pre>
     *   field
     *   this.field
     *
     *   method()
     *   this.method()
     * </pre>
     *
     * It does not perform any semantical check to differentiate between fields and local variables;
     * local methods or imported static methods.
     *
     * @param tree expression tree representing an access to object member
     * @return {@code true} iff the member is a member of {@code this} instance
     */
    public static boolean isSelfAccess(final ExpressionTree tree) {
        ExpressionTree tr = TreeUtils.withoutParens(tree);
        // If method invocation check the method select
        if (tr.getKind() == Tree.Kind.ARRAY_ACCESS) {
            return false;
        }

        if (tree.getKind() == Tree.Kind.METHOD_INVOCATION) {
            tr = ((MethodInvocationTree) tree).getMethodSelect();
        }
        tr = TreeUtils.withoutParens(tr);
        if (tr.getKind() == Tree.Kind.TYPE_CAST) {
            tr = ((TypeCastTree) tr).getExpression();
        }
        tr = TreeUtils.withoutParens(tr);

        if (tr.getKind() == Tree.Kind.IDENTIFIER) {
            return true;
        }

        if (tr.getKind() == Tree.Kind.MEMBER_SELECT) {
            tr = ((MemberSelectTree) tr).getExpression();
            if (tr.getKind() == Tree.Kind.IDENTIFIER) {
                Name ident = ((IdentifierTree) tr).getName();
                return ident.contentEquals("this") || ident.contentEquals("super");
            }
        }

        return false;
    }

    /**
     * If the given tree is a parenthesized tree, return the enclosed non-parenthesized tree.
     * Otherwise, return the same tree.
     *
     * @param tree an expression tree
     * @return the outermost non-parenthesized tree enclosed by the given tree
     */
    @SuppressWarnings("interning:return.type.incompatible") // polymorphism implementation
    public static @PolyInterned ExpressionTree withoutParens(
            final @PolyInterned ExpressionTree tree) {
        ExpressionTree t = tree;
        while (t.getKind() == Tree.Kind.PARENTHESIZED) {
            t = ((ParenthesizedTree) t).getExpression();
        }
        return t;
    }

    /**
     * Gets the {@link Element} for the given Tree API node. For an object instantiation returns the
     * value of the {@link JCNewClass#constructor} field. Note that this result might differ from
     * the result of {@link TreeUtils#constructor(NewClassTree)}.
     *
     * @param tree the {@link Tree} node to get the symbol for
     * @throws IllegalArgumentException if {@code tree} is null or is not a valid javac-internal
     *     tree (JCTree)
     * @return the {@link Symbol} for the given tree, or null if one could not be found
     */
    @Pure
    public static @Nullable Element elementFromTree(Tree tree) {
        if (tree == null) {
            throw new BugInCF("InternalUtils.symbol: tree is null");
        }

        if (!(tree instanceof JCTree)) {
            throw new BugInCF("InternalUtils.symbol: tree is not a valid Javac tree");
        }

        if (isExpressionTree(tree)) {
            tree = withoutParens((ExpressionTree) tree);
        }

        switch (tree.getKind()) {
                // symbol() only works on MethodSelects, so we need to get it manually
                // for method invocations.
            case METHOD_INVOCATION:
                return TreeInfo.symbol(((JCMethodInvocation) tree).getMethodSelect());

            case ASSIGNMENT:
                return TreeInfo.symbol((JCTree) ((AssignmentTree) tree).getVariable());

            case ARRAY_ACCESS:
                return elementFromTree(((ArrayAccessTree) tree).getExpression());

            case NEW_CLASS:
                return ((JCNewClass) tree).constructor;

            case MEMBER_REFERENCE:
                // TreeInfo.symbol, which is used in the default case, didn't handle
                // member references until JDK8u20. So handle it here.
                return ((JCMemberReference) tree).sym;

            default:
                if (isTypeDeclaration(tree)
                        || tree.getKind() == Tree.Kind.VARIABLE
                        || tree.getKind() == Tree.Kind.METHOD) {
                    return TreeInfo.symbolFor((JCTree) tree);
                }
                return TreeInfo.symbol((JCTree) tree);
        }
    }

    /**
     * Gets the element for a class corresponding to a declaration.
     *
     * @param node class declaration
     * @return the element for the given class
     */
    public static TypeElement elementFromDeclaration(ClassTree node) {
        TypeElement elt = (TypeElement) TreeUtils.elementFromTree(node);
        assert elt != null : "@AssumeAssertion(nullness): tree kind";
        return elt;
    }

    /**
     * Gets the element for a method corresponding to a declaration.
     *
     * @return the element for the given method
     */
    public static ExecutableElement elementFromDeclaration(MethodTree node) {
        ExecutableElement elt = (ExecutableElement) TreeUtils.elementFromTree(node);
        assert elt != null : "@AssumeAssertion(nullness): tree kind";
        return elt;
    }

    /**
     * Gets the element for a variable corresponding to its declaration.
     *
     * @return the element for the given variable
     */
    public static VariableElement elementFromDeclaration(VariableTree node) {
        VariableElement elt = (VariableElement) TreeUtils.elementFromTree(node);
        assert elt != null : "@AssumeAssertion(nullness): tree kind";
        return elt;
    }

    /**
     * Gets the element for the declaration corresponding to this use of an element. To get the
     * element for a declaration, use {@link #elementFromDeclaration(ClassTree)}, {@link
     * #elementFromDeclaration(MethodTree)}, or {@link #elementFromDeclaration(VariableTree)}
     * instead.
     *
     * <p>This method is just a wrapper around {@link TreeUtils#elementFromTree(Tree)}, but this
     * class might be the first place someone looks for this functionality.
     *
     * @param node the tree corresponding to a use of an element
     * @return the element for the corresponding declaration, {@code null} otherwise
     */
    @Pure
    public static @Nullable Element elementFromUse(ExpressionTree node) {
        return TreeUtils.elementFromTree(node);
    }

    /**
     * Returns the ExecutableElement for the called method, from a call.
     *
     * @param node a method call
     * @return the ExecutableElement for the called method
     */
    @Pure
    public static ExecutableElement elementFromUse(MethodInvocationTree node) {
        Element el = TreeUtils.elementFromTree(node);
        if (!(el instanceof ExecutableElement)) {
            throw new BugInCF("Method elements should be ExecutableElement. Found: %s", el);
        }
        return (ExecutableElement) el;
    }

    /**
     * Gets the ExecutableElement for the called constructor, from a constructor invocation.
     *
     * @param node a constructor invocation
     * @return the ExecutableElement for the called constructor
     * @see #constructor(NewClassTree)
     */
    @Pure
    public static ExecutableElement elementFromUse(NewClassTree node) {
        Element el = TreeUtils.elementFromTree(node);
        if (!(el instanceof ExecutableElement)) {
            throw new BugInCF("Constructor elements should  be ExecutableElement. Found: %s", el);
        }
        return (ExecutableElement) el;
    }

    /**
     * Determines the symbol for a constructor given an invocation via {@code new}.
     *
     * <p>If the tree is a declaration of an anonymous class, then method returns constructor that
     * gets invoked in the extended class, rather than the anonymous constructor implicitly added by
     * the constructor (JLS 15.9.5.1)
     *
     * @see #elementFromUse(NewClassTree)
     * @param tree the constructor invocation
     * @return the {@link ExecutableElement} corresponding to the constructor call in {@code tree}
     */
    public static ExecutableElement constructor(NewClassTree tree) {

        if (!(tree instanceof JCTree.JCNewClass)) {
            throw new BugInCF("InternalUtils.constructor: not a javac internal tree");
        }

        JCNewClass newClassTree = (JCNewClass) tree;

        if (tree.getClassBody() != null) {
            // anonymous constructor bodies should contain exactly one statement
            // in the form:
            //    super(arg1, ...)
            // or
            //    o.super(arg1, ...)
            //
            // which is a method invocation (!) to the actual constructor

            // the method call is guaranteed to return nonnull
            JCMethodDecl anonConstructor =
                    (JCMethodDecl) TreeInfo.declarationFor(newClassTree.constructor, newClassTree);
            assert anonConstructor != null;
            assert anonConstructor.body.stats.size() == 1;
            JCExpressionStatement stmt = (JCExpressionStatement) anonConstructor.body.stats.head;
            JCTree.JCMethodInvocation superInvok = (JCMethodInvocation) stmt.expr;
            return (ExecutableElement) TreeInfo.symbol(superInvok.meth);
        } else {
            Element e = newClassTree.constructor;
            return (ExecutableElement) e;
        }
    }

    /**
     * Determine whether the given ExpressionTree has an underlying element.
     *
     * @param node the ExpressionTree to test
     * @return whether the tree refers to an identifier, member select, or method invocation
     */
    @EnsuresNonNullIf(result = true, expression = "elementFromUse(#1)")
    @Pure
    public static boolean isUseOfElement(ExpressionTree node) {
        ExpressionTree realnode = TreeUtils.withoutParens(node);
        switch (realnode.getKind()) {
            case IDENTIFIER:
            case MEMBER_SELECT:
            case METHOD_INVOCATION:
            case NEW_CLASS:
                assert elementFromUse(node) != null : "@AssumeAssertion(nullness): inspection";
                return true;
            default:
                return false;
        }
    }

    /**
     * Determine whether the given MethodInvocationTree has an underlying element.
     *
     * @param node the MethodInvocationTree to test
     * @return whether the tree refers to an identifier, member select, or method invocation
     */
    @EnsuresNonNullIf(result = true, expression = "elementFromUse(#1)")
    @Pure
    @SuppressWarnings("nullness:contracts.conditional.postcondition.not.satisfied")
    // TODO: remove this method
    public static boolean isUseOfElement(MethodInvocationTree node) {
        return isUseOfElement((ExpressionTree) node);
    }

    /**
     * Determine whether the given NewClassTree has an underlying element.
     *
     * @param node the NewClassTree to test
     * @return whether the tree refers to an identifier, member select, or method invocation
     */
    @EnsuresNonNullIf(result = true, expression = "elementFromUse(#1)")
    @Pure
    @SuppressWarnings("nullness:contracts.conditional.postcondition.not.satisfied")
    // TODO: remove this method
    public static boolean isUseOfElement(NewClassTree node) {
        return isUseOfElement((ExpressionTree) node);
    }

    /**
     * Returns true if {@code tree} has a synthetic argument.
     *
     * <p>For some anonymous classes with an explicit enclosing expression, javac creates a
     * synthetic argument to the constructor that is the enclosing expression of the NewClassTree.
     * Suppose a programmer writes:
     *
     * <pre><code>
     *     class Outer {
     *         class Inner { }
     *         void method() {
     *             this.new Inner(){};
     *         }
     *     }
     * </code></pre>
     *
     * Java 9 javac creates the following synthetic tree for {@code this.new Inner(){}}:
     *
     * <pre><code>
     *    new Inner(this) {
     *         (.Outer x0) {
     *             x0.super();
     *         }
     *    }
     * </code></pre>
     *
     * Java 11 javac creates a different tree without the synthetic argument for {@code this.new
     * Inner(){}}:
     *
     * <pre><code>
     *    this.new Inner() {
     *         (.Outer x0) {
     *             x0.super();
     *         }
     *    }
     * </code></pre>
     *
     * @param tree a new class tree
     * @return true if {@code tree} has a synthetic argument
     */
    public static boolean hasSyntheticArgument(NewClassTree tree) {
        if (tree.getClassBody() == null || tree.getEnclosingExpression() != null) {
            return false;
        }
        for (Tree member : tree.getClassBody().getMembers()) {
            if (member.getKind() == Kind.METHOD && isConstructor((MethodTree) member)) {
                MethodTree methodTree = (MethodTree) member;
                StatementTree f = methodTree.getBody().getStatements().get(0);
                return TreeUtils.getReceiverTree(((ExpressionStatementTree) f).getExpression())
                        != null;
            }
        }
        return false;
    }
    /**
     * Returns the name of the invoked method.
     *
     * @return the name of the invoked method
     */
    public static Name methodName(MethodInvocationTree node) {
        ExpressionTree expr = node.getMethodSelect();
        if (expr.getKind() == Tree.Kind.IDENTIFIER) {
            return ((IdentifierTree) expr).getName();
        } else if (expr.getKind() == Tree.Kind.MEMBER_SELECT) {
            return ((MemberSelectTree) expr).getIdentifier();
        }
        throw new BugInCF("TreeUtils.methodName: cannot be here: " + node);
    }

    /**
     * Returns true if the first statement in the body is a self constructor invocation within a
     * constructor.
     *
     * @return true if the first statement in the body is a self constructor invocation within a
     *     constructor
     */
    public static boolean containsThisConstructorInvocation(MethodTree node) {
        if (!TreeUtils.isConstructor(node) || node.getBody().getStatements().isEmpty()) {
            return false;
        }

        StatementTree st = node.getBody().getStatements().get(0);
        if (!(st instanceof ExpressionStatementTree)
                || !(((ExpressionStatementTree) st).getExpression()
                        instanceof MethodInvocationTree)) {
            return false;
        }

        MethodInvocationTree invocation =
                (MethodInvocationTree) ((ExpressionStatementTree) st).getExpression();

        return "this".contentEquals(TreeUtils.methodName(invocation));
    }

    /**
     * Returns the first statement of the tree if it is a block. If it is not a block or an empty
     * block, tree is returned.
     *
     * @param tree any kind of tree
     * @return the first statement of the tree if it is a block. If it is not a block or an empty
     *     block, tree is returned.
     */
    public static Tree firstStatement(Tree tree) {
        Tree first;
        if (tree.getKind() == Tree.Kind.BLOCK) {
            BlockTree block = (BlockTree) tree;
            if (block.getStatements().isEmpty()) {
                first = block;
            } else {
                first = block.getStatements().iterator().next();
            }
        } else {
            first = tree;
        }
        return first;
    }

    /**
     * Determine whether the given class contains an explicit constructor.
     *
     * @param node a class tree
     * @return true iff there is an explicit constructor
     */
    public static boolean hasExplicitConstructor(ClassTree node) {
        TypeElement elem = TreeUtils.elementFromDeclaration(node);
        for (ExecutableElement constructorElt :
                ElementFilter.constructorsIn(elem.getEnclosedElements())) {
            if (!isSynthetic(constructorElt)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the given method is synthetic.
     *
     * @param ee a method or constructor element
     * @return true iff the given method is synthetic
     */
    public static boolean isSynthetic(ExecutableElement ee) {
        MethodSymbol ms = (MethodSymbol) ee;
        long mod = ms.flags();
        // GENERATEDCONSTR is for generated constructors, which seem not to have SYNTHETIC set.
        return (mod & (Flags.SYNTHETIC | Flags.GENERATEDCONSTR)) != 0;
    }

    /**
     * Returns true if the given method is synthetic.
     *
     * @param node a method declaration tree
     * @return true iff the given method is synthetic
     */
    public static boolean isSynthetic(MethodTree node) {
        ExecutableElement ee = TreeUtils.elementFromDeclaration(node);
        return isSynthetic(ee);
    }

    /**
     * Returns true if the tree is of a diamond type. In contrast to the implementation in TreeInfo,
     * this version works on Trees.
     *
     * @see com.sun.tools.javac.tree.TreeInfo#isDiamond(JCTree)
     */
    public static boolean isDiamondTree(Tree tree) {
        switch (tree.getKind()) {
            case ANNOTATED_TYPE:
                return isDiamondTree(((AnnotatedTypeTree) tree).getUnderlyingType());
            case PARAMETERIZED_TYPE:
                return ((ParameterizedTypeTree) tree).getTypeArguments().isEmpty();
            case NEW_CLASS:
                return isDiamondTree(((NewClassTree) tree).getIdentifier());
            default:
                return false;
        }
    }

    /** Returns true if the tree represents a {@code String} concatenation operation. */
    public static boolean isStringConcatenation(Tree tree) {
        return (tree.getKind() == Tree.Kind.PLUS && TypesUtils.isString(TreeUtils.typeOf(tree)));
    }

    /** Returns true if the compound assignment tree is a string concatenation. */
    public static boolean isStringCompoundConcatenation(CompoundAssignmentTree tree) {
        return (tree.getKind() == Tree.Kind.PLUS_ASSIGNMENT
                && TypesUtils.isString(TreeUtils.typeOf(tree)));
    }

    /**
     * Returns true if the node is a constant-time expression.
     *
     * <p>A tree is a constant-time expression if it is:
     *
     * <ol>
     *   <li>a literal tree
     *   <li>a reference to a final variable initialized with a compile time constant
     *   <li>a String concatenation of two compile time constants
     * </ol>
     */
    public static boolean isCompileTimeString(ExpressionTree node) {
        ExpressionTree tree = TreeUtils.withoutParens(node);
        if (tree instanceof LiteralTree) {
            return true;
        }

        if (TreeUtils.isUseOfElement(tree)) {
            Element elt = TreeUtils.elementFromUse(tree);
            return ElementUtils.isCompileTimeConstant(elt);
        } else if (TreeUtils.isStringConcatenation(tree)) {
            BinaryTree binOp = (BinaryTree) tree;
            return isCompileTimeString(binOp.getLeftOperand())
                    && isCompileTimeString(binOp.getRightOperand());
        } else {
            return false;
        }
    }

    /**
     * Returns the receiver tree of a field access or a method invocation.
     *
     * @param expression a field access or a method invocation
     * @return the expression's receiver tree, or null if it does not have an explicit receiver
     */
    public static @Nullable ExpressionTree getReceiverTree(ExpressionTree expression) {
        ExpressionTree receiver;
        switch (expression.getKind()) {
            case METHOD_INVOCATION:
                // Trying to handle receiver calls to trees of the form
                //     ((m).getArray())
                // returns the type of 'm' in this case
                receiver = ((MethodInvocationTree) expression).getMethodSelect();

                if (receiver.getKind() == Tree.Kind.MEMBER_SELECT) {
                    receiver = ((MemberSelectTree) receiver).getExpression();
                } else {
                    // It's a method call "m(foo)" without an explicit receiver
                    return null;
                }
                break;
            case NEW_CLASS:
                receiver = ((NewClassTree) expression).getEnclosingExpression();
                break;
            case ARRAY_ACCESS:
                receiver = ((ArrayAccessTree) expression).getExpression();
                break;
            case MEMBER_SELECT:
                receiver = ((MemberSelectTree) expression).getExpression();
                // Avoid int.class
                if (receiver instanceof PrimitiveTypeTree) {
                    return null;
                }
                break;
            case IDENTIFIER:
                // It's a field access on implicit this or a local variable/parameter.
                return null;
            default:
                return null;
        }
        if (receiver == null) {
            return null;
        }

        return TreeUtils.withoutParens(receiver);
    }

    // TODO: What about anonymous classes?
    // Adding Tree.Kind.NEW_CLASS here doesn't work, because then a
    // tree gets cast to ClassTree when it is actually a NewClassTree,
    // for example in enclosingClass above.
    /** The set of kinds that represent classes. */
    private static final Set<Tree.Kind> classTreeKinds;

    static {
        classTreeKinds = EnumSet.noneOf(Tree.Kind.class);
        for (Tree.Kind kind : Tree.Kind.values()) {
            if (kind.asInterface() == ClassTree.class) {
                classTreeKinds.add(kind);
            }
        }
    }

    /**
     * Return the set of kinds that represent classes.
     *
     * @return the set of kinds that represent classes
     */
    public static Set<Tree.Kind> classTreeKinds() {
        return classTreeKinds;
    }

    /**
     * Is the given tree kind a class, i.e. a class, enum, interface, or annotation type.
     *
     * @param tree the tree to test
     * @return true, iff the given kind is a class kind
     */
    public static boolean isClassTree(Tree tree) {
        return classTreeKinds().contains(tree.getKind());
    }

    private static final Set<Tree.Kind> typeTreeKinds =
            EnumSet.of(
                    Tree.Kind.PRIMITIVE_TYPE,
                    Tree.Kind.PARAMETERIZED_TYPE,
                    Tree.Kind.TYPE_PARAMETER,
                    Tree.Kind.ARRAY_TYPE,
                    Tree.Kind.UNBOUNDED_WILDCARD,
                    Tree.Kind.EXTENDS_WILDCARD,
                    Tree.Kind.SUPER_WILDCARD,
                    Tree.Kind.ANNOTATED_TYPE);

    public static Set<Tree.Kind> typeTreeKinds() {
        return typeTreeKinds;
    }

    /**
     * Is the given tree a type instantiation?
     *
     * <p>TODO: this is an under-approximation: e.g. an identifier could be either a type use or an
     * expression. How can we distinguish.
     *
     * @param tree the tree to test
     * @return true, iff the given tree is a type
     */
    public static boolean isTypeTree(Tree tree) {
        return typeTreeKinds().contains(tree.getKind());
    }

    /**
     * Returns true if the given element is an invocation of the method, or of any method that
     * overrides that one.
     */
    public static boolean isMethodInvocation(
            Tree tree, ExecutableElement method, ProcessingEnvironment env) {
        if (!(tree instanceof MethodInvocationTree)) {
            return false;
        }
        MethodInvocationTree methInvok = (MethodInvocationTree) tree;
        ExecutableElement invoked = TreeUtils.elementFromUse(methInvok);
        if (invoked == null) {
            return false;
        }
        return ElementUtils.isMethod(invoked, method, env);
    }

    /**
     * Returns true if the argument is an invocation of one of the given methods, or of any method
     * that overrides them.
     */
    public static boolean isMethodInvocation(
            Tree methodTree, List<ExecutableElement> methods, ProcessingEnvironment processingEnv) {
        if (!(methodTree instanceof MethodInvocationTree)) {
            return false;
        }
        for (ExecutableElement Method : methods) {
            if (isMethodInvocation(methodTree, Method, processingEnv)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the ExecutableElement for a method declaration. Errs if there is not exactly one
     * matching method. If more than one method takes the same number of formal parameters, then use
     * {@link #getMethod(String, String, ProcessingEnvironment, String...)}.
     *
     * @param typeName the class that contains the method
     * @param methodName the name of the method
     * @param params the number of formal parameters
     * @param env the processing environment
     * @return the ExecutableElement for the specified method
     */
    public static ExecutableElement getMethod(
            @FullyQualifiedName String typeName,
            String methodName,
            int params,
            ProcessingEnvironment env) {
        List<ExecutableElement> methods = getMethods(typeName, methodName, params, env);
        if (methods.size() == 1) {
            return methods.get(0);
        }
        throw new BugInCF(
                "TreeUtils.getMethod(%s, %s, %d): expected 1 match, found %d",
                typeName, methodName, params, methods.size());
    }

    /**
     * Returns the ExecutableElement for a method declaration. Returns null there is no matching
     * method. Errs if there is more than one matching method. If more than one method takes the
     * same number of formal parameters, then use {@link #getMethod(String, String,
     * ProcessingEnvironment, String...)}.
     *
     * @param typeName the class that contains the method
     * @param methodName the name of the method
     * @param params the number of formal parameters
     * @param env the processing environment
     * @return the ExecutableElement for the specified method, or null
     */
    public static @Nullable ExecutableElement getMethodOrNull(
            @FullyQualifiedName String typeName,
            String methodName,
            int params,
            ProcessingEnvironment env) {
        List<ExecutableElement> methods = getMethods(typeName, methodName, params, env);
        if (methods.size() == 0) {
            return null;
        } else if (methods.size() == 1) {
            return methods.get(0);
        } else {
            throw new BugInCF(
                    "TreeUtils.getMethod(%s, %s, %d): expected 0 or 1 match, found %d",
                    typeName, methodName, params, methods.size());
        }
    }

    /**
     * Returns all ExecutableElements for method declarations of methodName, in class typeName, with
     * params formal parameters.
     *
     * @param typeName the class that contains the method
     * @param methodName the name of the method
     * @param params the number of formal parameters
     * @param env the processing environment
     * @return the ExecutableElements for all matching methods
     */
    public static List<ExecutableElement> getMethods(
            @FullyQualifiedName String typeName,
            String methodName,
            int params,
            ProcessingEnvironment env) {
        List<ExecutableElement> methods = new ArrayList<>(1);
        TypeElement typeElt = env.getElementUtils().getTypeElement(typeName);
        if (typeElt == null) {
            throw new UserError("Configuration problem! Could not load type: " + typeName);
        }
        for (ExecutableElement exec : ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
            if (exec.getSimpleName().contentEquals(methodName)
                    && exec.getParameters().size() == params) {
                methods.add(exec);
            }
        }
        return methods;
    }

    /**
     * Returns the ExecutableElement for a method declaration. Errs if there is no matching method.
     *
     * @param typeName the class that contains the method
     * @param methodName the name of the method
     * @param env the processing environment
     * @param paramTypes the method's formal parameter types
     * @return the ExecutableElement for the specified method
     */
    public static ExecutableElement getMethod(
            @FullyQualifiedName String typeName,
            String methodName,
            ProcessingEnvironment env,
            String... paramTypes) {
        TypeElement typeElt = env.getElementUtils().getTypeElement(typeName);
        for (ExecutableElement exec : ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
            if (exec.getSimpleName().contentEquals(methodName)
                    && exec.getParameters().size() == paramTypes.length) {
                boolean typesMatch = true;
                List<? extends VariableElement> params = exec.getParameters();
                for (int i = 0; i < paramTypes.length; i++) {
                    VariableElement ve = params.get(i);
                    TypeMirror tm = TypeAnnotationUtils.unannotatedType(ve.asType());
                    if (!tm.toString().equals(paramTypes[i])) {
                        typesMatch = false;
                        break;
                    }
                }
                if (typesMatch) {
                    return exec;
                }
            }
        }
        throw new BugInCF(
                "TreeUtils.getMethod: found no match for "
                        + typeName
                        + "."
                        + methodName
                        + "("
                        + Arrays.toString(paramTypes)
                        + ")");
    }

    /**
     * Determine whether the given expression is either "this" or an outer "C.this".
     *
     * <p>TODO: Should this also handle "super"?
     */
    public static boolean isExplicitThisDereference(ExpressionTree tree) {
        if (tree.getKind() == Tree.Kind.IDENTIFIER
                && ((IdentifierTree) tree).getName().contentEquals("this")) {
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
     * Determine whether {@code tree} is a class literal, such as.
     *
     * <pre>
     *   <em>Object</em> . <em>class</em>
     * </pre>
     *
     * @return true iff if tree is a class literal
     */
    public static boolean isClassLiteral(Tree tree) {
        if (tree.getKind() != Tree.Kind.MEMBER_SELECT) {
            return false;
        }
        return "class".equals(((MemberSelectTree) tree).getIdentifier().toString());
    }

    /**
     * Determine whether {@code tree} is a field access expressions, such as.
     *
     * <pre>
     *   <em>f</em>
     *   <em>obj</em> . <em>f</em>
     * </pre>
     *
     * @return true iff if tree is a field access expression (implicit or explicit)
     */
    public static boolean isFieldAccess(Tree tree) {
        if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
            // explicit field access
            MemberSelectTree memberSelect = (MemberSelectTree) tree;
            assert isUseOfElement(memberSelect) : "@AssumeAssertion(nullness): tree kind";
            Element el = TreeUtils.elementFromUse(memberSelect);
            return el.getKind().isField();
        } else if (tree.getKind() == Tree.Kind.IDENTIFIER) {
            // implicit field access
            IdentifierTree ident = (IdentifierTree) tree;
            assert isUseOfElement(ident) : "@AssumeAssertion(nullness): tree kind";
            Element el = TreeUtils.elementFromUse(ident);
            return el.getKind().isField()
                    && !ident.getName().contentEquals("this")
                    && !ident.getName().contentEquals("super");
        }
        return false;
    }

    /**
     * Compute the name of the field that the field access {@code tree} accesses. Requires {@code
     * tree} to be a field access, as determined by {@code isFieldAccess}.
     *
     * @param tree a field access tree
     * @return the name of the field accessed by {@code tree}
     */
    public static String getFieldName(Tree tree) {
        assert isFieldAccess(tree);
        if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
            MemberSelectTree mtree = (MemberSelectTree) tree;
            return mtree.getIdentifier().toString();
        } else {
            IdentifierTree itree = (IdentifierTree) tree;
            return itree.getName().toString();
        }
    }

    /**
     * Determine whether {@code tree} refers to a method element, such as.
     *
     * <pre>
     *   <em>m</em>(...)
     *   <em>obj</em> . <em>m</em>(...)
     * </pre>
     *
     * @return true iff if tree is a method access expression (implicit or explicit)
     */
    public static boolean isMethodAccess(Tree tree) {
        if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
            // explicit method access
            MemberSelectTree memberSelect = (MemberSelectTree) tree;
            assert isUseOfElement(memberSelect) : "@AssumeAssertion(nullness): tree kind";
            Element el = TreeUtils.elementFromUse(memberSelect);
            return el.getKind() == ElementKind.METHOD || el.getKind() == ElementKind.CONSTRUCTOR;
        } else if (tree.getKind() == Tree.Kind.IDENTIFIER) {
            // implicit method access
            IdentifierTree ident = (IdentifierTree) tree;
            // The field "super" and "this" are also legal methods
            if (ident.getName().contentEquals("super") || ident.getName().contentEquals("this")) {
                return true;
            }
            assert isUseOfElement(ident) : "@AssumeAssertion(nullness): tree kind";
            Element el = TreeUtils.elementFromUse(ident);
            return el.getKind() == ElementKind.METHOD || el.getKind() == ElementKind.CONSTRUCTOR;
        }
        return false;
    }

    /**
     * Compute the name of the method that the method access {@code tree} accesses. Requires {@code
     * tree} to be a method access, as determined by {@code isMethodAccess}.
     *
     * @param tree a method access tree
     * @return the name of the method accessed by {@code tree}
     */
    public static String getMethodName(Tree tree) {
        assert isMethodAccess(tree);
        if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
            MemberSelectTree mtree = (MemberSelectTree) tree;
            return mtree.getIdentifier().toString();
        } else {
            IdentifierTree itree = (IdentifierTree) tree;
            return itree.getName().toString();
        }
    }

    /**
     * Return {@code true} if and only if {@code tree} can have a type annotation.
     *
     * @return {@code true} if and only if {@code tree} can have a type annotation
     */
    // TODO: is this implementation precise enough? E.g. does a .class literal work correctly?
    public static boolean canHaveTypeAnnotation(Tree tree) {
        return ((JCTree) tree).type != null;
    }

    /**
     * Returns true if and only if the given {@code tree} represents a field access of the given
     * {@link VariableElement}.
     */
    public static boolean isSpecificFieldAccess(Tree tree, VariableElement var) {
        if (tree instanceof MemberSelectTree) {
            MemberSelectTree memSel = (MemberSelectTree) tree;
            assert isUseOfElement(memSel) : "@AssumeAssertion(nullness): tree kind";
            Element field = TreeUtils.elementFromUse(memSel);
            return field.equals(var);
        } else if (tree instanceof IdentifierTree) {
            IdentifierTree idTree = (IdentifierTree) tree;
            assert isUseOfElement(idTree) : "@AssumeAssertion(nullness): tree kind";
            Element field = TreeUtils.elementFromUse(idTree);
            return field.equals(var);
        } else {
            return false;
        }
    }

    /**
     * Returns the VariableElement for a field declaration.
     *
     * @param typeName the class where the field is declared
     * @param fieldName the name of the field
     * @param env the processing environment
     * @return the VariableElement for typeName.fieldName
     */
    public static VariableElement getField(
            @FullyQualifiedName String typeName, String fieldName, ProcessingEnvironment env) {
        TypeElement mapElt = env.getElementUtils().getTypeElement(typeName);
        for (VariableElement var : ElementFilter.fieldsIn(mapElt.getEnclosedElements())) {
            if (var.getSimpleName().contentEquals(fieldName)) {
                return var;
            }
        }
        throw new BugInCF("TreeUtils.getField: shouldn't be here");
    }

    /**
     * Determine whether the given tree represents an ExpressionTree.
     *
     * @param tree the Tree to test
     * @return whether the tree is an ExpressionTree
     */
    public static boolean isExpressionTree(Tree tree) {
        return tree instanceof ExpressionTree;
    }

    /**
     * Returns true if this is a super call to the {@link Enum} constructor.
     *
     * @param node the method invocation to check
     * @return true if this is a super call to the {@link Enum} constructor
     */
    public static boolean isEnumSuper(MethodInvocationTree node) {
        ExecutableElement ex = TreeUtils.elementFromUse(node);
        assert ex != null : "@AssumeAssertion(nullness): tree kind";
        Name name = ElementUtils.getQualifiedClassName(ex);
        assert name != null : "@AssumeAssertion(nullness): assumption";
        boolean correctClass = "java.lang.Enum".contentEquals(name);
        boolean correctMethod = "<init>".contentEquals(ex.getSimpleName());
        return correctClass && correctMethod;
    }

    /**
     * Determine whether the given tree represents a declaration of a type (including type
     * parameters).
     *
     * @param node the Tree to test
     * @return true if the tree is a type declaration
     */
    public static boolean isTypeDeclaration(Tree node) {
        return isClassTree(node) || node.getKind() == Tree.Kind.TYPE_PARAMETER;
    }

    /**
     * Returns whether or not tree is an access of array length.
     *
     * @param tree tree to check
     * @return true if tree is an access of array length
     */
    public static boolean isArrayLengthAccess(Tree tree) {
        if (tree.getKind() == Kind.MEMBER_SELECT
                && isFieldAccess(tree)
                && getFieldName(tree).equals("length")) {
            ExpressionTree expressionTree = ((MemberSelectTree) tree).getExpression();
            if (TreeUtils.typeOf(expressionTree).getKind() == TypeKind.ARRAY) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether or not the node referred to by the given {@link Tree} is an anonymous
     * constructor (the constructor for an anonymous class.
     *
     * @param method the {@link Tree} for a node that may be an anonymous constructor
     * @return true if the given path points to an anonymous constructor, false if it does not
     */
    public static boolean isAnonymousConstructor(final MethodTree method) {
        @Nullable Element e = elementFromTree(method);
        if (!(e instanceof Symbol)) {
            return false;
        }

        if ((((@NonNull Symbol) e).flags() & Flags.ANONCONSTR) != 0) {
            return true;
        }

        return false;
    }

    /**
     * Converts the given AnnotationTrees to AnnotationMirrors.
     *
     * @param annoTreess list of annotation trees to convert to annotation mirrors
     * @return list of annotation mirrors that represent the given annotation trees
     */
    public static List<AnnotationMirror> annotationsFromTypeAnnotationTrees(
            List<? extends AnnotationTree> annoTreess) {
        List<AnnotationMirror> annotations = new ArrayList<>(annoTreess.size());
        for (AnnotationTree anno : annoTreess) {
            annotations.add(TreeUtils.annotationFromAnnotationTree(anno));
        }
        return annotations;
    }

    /**
     * Converts the given AnnotationTree to an AnnotationMirror.
     *
     * @param tree annotation tree to convert to an annotation mirror
     * @return annotation mirror that represent the given annotation tree
     */
    public static AnnotationMirror annotationFromAnnotationTree(AnnotationTree tree) {
        return ((JCAnnotation) tree).attribute;
    }

    /**
     * Converts the given AnnotatedTypeTree to a list of AnnotationMirrors.
     *
     * @param tree annotated type tree to convert
     * @return list of AnnotationMirrors from the tree
     */
    public static List<? extends AnnotationMirror> annotationsFromTree(AnnotatedTypeTree tree) {
        return annotationsFromTypeAnnotationTrees(((JCAnnotatedType) tree).annotations);
    }

    /**
     * Converts the given TypeParameterTree to a list of AnnotationMirrors.
     *
     * @param tree type parameter tree to convert
     * @return list of AnnotationMirrors from the tree
     */
    public static List<? extends AnnotationMirror> annotationsFromTree(TypeParameterTree tree) {
        return annotationsFromTypeAnnotationTrees(((JCTypeParameter) tree).annotations);
    }

    /**
     * Converts the given NewArrayTree to a list of AnnotationMirrors.
     *
     * @param tree new array tree
     * @return list of AnnotationMirrors from the tree
     */
    public static List<? extends AnnotationMirror> annotationsFromArrayCreation(
            NewArrayTree tree, int level) {

        assert tree instanceof JCNewArray;
        final JCNewArray newArray = ((JCNewArray) tree);

        if (level == -1) {
            return annotationsFromTypeAnnotationTrees(newArray.annotations);
        }

        if (newArray.dimAnnotations.length() > 0
                && (level >= 0)
                && (level < newArray.dimAnnotations.size())) {
            return annotationsFromTypeAnnotationTrees(newArray.dimAnnotations.get(level));
        }

        return Collections.emptyList();
    }

    /**
     * Returns true if the tree is the declaration or use of a local variable.
     *
     * @return true if the tree is the declaration or use of a local variable
     */
    public static boolean isLocalVariable(Tree tree) {
        if (tree.getKind() == Kind.VARIABLE) {
            return elementFromDeclaration((VariableTree) tree).getKind()
                    == ElementKind.LOCAL_VARIABLE;
        } else if (tree.getKind() == Kind.IDENTIFIER) {
            ExpressionTree etree = (ExpressionTree) tree;
            assert isUseOfElement(etree) : "@AssumeAssertion(nullness): tree kind";
            return elementFromUse(etree).getKind() == ElementKind.LOCAL_VARIABLE;
        }
        return false;
    }

    /**
     * Returns the type as a TypeMirror of {@code tree}. To obtain {@code tree}'s
     * AnnotatedTypeMirror, call {@code AnnotatedTypeFactory.getAnnotatedType()}.
     *
     * @return the type as a TypeMirror of {@code tree}
     */
    public static TypeMirror typeOf(Tree tree) {
        return ((JCTree) tree).type;
    }

    /**
     * The type of the lambda or method reference tree is a functional interface type. This method
     * returns the single abstract method declared by that functional interface. (The type of this
     * method is referred to as the function type.)
     *
     * @param tree lambda or member reference tree
     * @param env ProcessingEnvironment
     * @return the single abstract method declared by the type of the tree
     */
    public static Symbol findFunction(Tree tree, ProcessingEnvironment env) {
        Context ctx = ((JavacProcessingEnvironment) env).getContext();
        Types javacTypes = Types.instance(ctx);
        return javacTypes.findDescriptorSymbol(((Type) typeOf(tree)).asElement());
    }

    /**
     * Returns true if {@code tree} is an implicitly typed lambda.
     *
     * <p>A lambda expression whose formal type parameters have inferred types is an implicitly
     * typed lambda. (See JLS 15.27.1)
     *
     * @param tree any kind of tree
     * @return true iff {@code tree} is an implicitly typed lambda
     */
    public static boolean isImplicitlyTypedLambda(Tree tree) {
        return tree.getKind() == Kind.LAMBDA_EXPRESSION
                && ((JCLambda) tree).paramKind == ParameterKind.IMPLICIT;
    }

    /**
     * Determine whether an expression {@link ExpressionTree} has the constant value true, according
     * to the compiler logic.
     *
     * @param node the expression to be checked
     * @return true if {@code node} has the constant value true
     */
    public static boolean isExprConstTrue(final ExpressionTree node) {
        assert node instanceof JCExpression;
        if (((JCExpression) node).type.isTrue()) {
            return true;
        }
        ExpressionTree tree = TreeUtils.withoutParens(node);
        if (tree instanceof JCTree.JCBinary) {
            JCBinary binTree = (JCBinary) tree;
            JCExpression ltree = binTree.lhs;
            JCExpression rtree = binTree.rhs;
            switch (binTree.getTag()) {
                case AND:
                    return isExprConstTrue(ltree) && isExprConstTrue(rtree);
                case OR:
                    return isExprConstTrue(ltree) || isExprConstTrue(rtree);
                default:
                    break;
            }
        }
        return false;
    }

    /**
     * Return toString(), but without line separators.
     *
     * @param tree a tree
     * @return a one-line string representation of the tree
     */
    public static String toStringOneLine(Tree tree) {
        return tree.toString().trim().replaceAll("\\s+", " ");
    }

    /**
     * Return either {@link #toStringOneLine} if it is no more than {@code length} characters, or
     * {@link #toStringOneLine} quoted and truncated.
     *
     * @param tree a tree
     * @param length the maximum length for the result; must be at least 6
     * @return a one-line string representation of the tree that is no longer than {@code length}
     *     characters long
     */
    public static String toStringTruncated(Tree tree, int length) {
        if (length < 6) {
            throw new IllegalArgumentException("bad length " + length);
        }
        String result = toStringOneLine(tree);
        if (result.length() > length) {
            // The quoting increases the likelihood that all delimiters are balanced in the result.
            // That makes it easier to manipulate the result (such as skipping over it) in an
            // editor.  The quoting also makes clear that the value is truncated.
            result = "\"" + result.substring(0, length - 5) + "...\"";
        }
        return result;
    }

    /**
     * Given a javac ExpressionTree representing a fully qualified name such as "java.lang.Object",
     * creates a String containing the name.
     *
     * @param nameExpr an ExpressionTree representing a fully qualified name
     * @return a String representation of the fully qualified name
     */
    public static String nameExpressionToString(ExpressionTree nameExpr) {
        TreeVisitor<String, Void> visitor =
                new SimpleTreeVisitor<String, Void>() {
                    @Override
                    public String visitIdentifier(IdentifierTree node, Void p) {
                        return node.toString();
                    }

                    @Override
                    public String visitMemberSelect(MemberSelectTree node, Void p) {
                        return node.getExpression().accept(this, null)
                                + "."
                                + node.getIdentifier().toString();
                    }
                };
        return nameExpr.accept(visitor, null);
    }

    /**
     * Returns true if the binary operator may do a widening primitive conversion. See <a
     * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-5.html">JLS chapter 5</a>.
     *
     * @param node a binary tree
     * @return true if the tree's operator does numeric promotion on its arguments
     */
    public static boolean isWideningBinary(BinaryTree node) {
        switch (node.getKind()) {
            case LEFT_SHIFT:
            case LEFT_SHIFT_ASSIGNMENT:
            case RIGHT_SHIFT:
            case RIGHT_SHIFT_ASSIGNMENT:
            case UNSIGNED_RIGHT_SHIFT:
            case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
                // Strictly speaking,  these operators do unary promotion on each argument
                // separately.
                return true;

            case MULTIPLY:
            case MULTIPLY_ASSIGNMENT:
            case DIVIDE:
            case DIVIDE_ASSIGNMENT:
            case REMAINDER:
            case REMAINDER_ASSIGNMENT:
            case PLUS:
            case PLUS_ASSIGNMENT:
            case MINUS:
            case MINUS_ASSIGNMENT:

            case LESS_THAN:
            case LESS_THAN_EQUAL:
            case GREATER_THAN:
            case GREATER_THAN_EQUAL:
            case EQUAL_TO:
            case NOT_EQUAL_TO:

            case AND:
            case XOR:
            case OR:
                // These operators do binary promotion on the two arguments together.
                return true;

                // TODO: CONDITIONAL_EXPRESSION (?:) sometimes does numeric promotion.

            default:
                return false;
        }
    }

    /**
     * Returns the annotations explicitly written on the given type.
     *
     * @param annoTrees annotations written before a variable/method declaration; null if this type
     *     is not from such a location. This might contain type annotations that the Java parser
     *     attached to the declaration rather than to the type.
     * @param typeTree the type whose annotations to return
     * @return the annotations explicitly written on the given type
     */
    public static List<? extends AnnotationTree> getExplicitAnnotationTrees(
            @Nullable List<? extends AnnotationTree> annoTrees, Tree typeTree) {
        while (true) {
            switch (typeTree.getKind()) {
                case IDENTIFIER:
                case PRIMITIVE_TYPE:
                    if (annoTrees == null) {
                        return Collections.emptyList();
                    }
                    return annoTrees;
                case ANNOTATED_TYPE:
                    return ((AnnotatedTypeTree) typeTree).getAnnotations();
                case ARRAY_TYPE:
                case TYPE_PARAMETER:
                case UNBOUNDED_WILDCARD:
                case EXTENDS_WILDCARD:
                case SUPER_WILDCARD:
                    return Collections.emptyList();
                case MEMBER_SELECT:
                    if (annoTrees == null) {
                        return Collections.emptyList();
                    }
                    typeTree = ((MemberSelectTree) typeTree).getExpression();
                    break;
                case PARAMETERIZED_TYPE:
                    typeTree = ((ParameterizedTypeTree) typeTree).getType();
                    break;
                case UNION_TYPE:
                    List<AnnotationTree> result = new ArrayList<>();
                    for (Tree alternative : ((UnionTypeTree) typeTree).getTypeAlternatives()) {
                        result.addAll(getExplicitAnnotationTrees(null, alternative));
                    }
                    return result;
                default:
                    throw new BugInCF(
                            "what typeTree? %s %s %s",
                            typeTree.getKind(), typeTree.getClass(), typeTree);
            }
        }
    }

    /**
     * Return a tree for the default value of the given type. The default value is 0, false, or
     * null.
     *
     * @param typeMirror a type
     * @param processingEnv the processing environment
     * @return a tree for {@code type}'s default value
     */
    public static LiteralTree getDefaultValueTree(
            TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
        switch (typeMirror.getKind()) {
            case BYTE:
                return TreeUtils.createLiteral(TypeTag.BYTE, (byte) 0, typeMirror, processingEnv);
            case CHAR:
                return TreeUtils.createLiteral(TypeTag.CHAR, '\u0000', typeMirror, processingEnv);
            case SHORT:
                return TreeUtils.createLiteral(TypeTag.SHORT, (short) 0, typeMirror, processingEnv);
            case LONG:
                return TreeUtils.createLiteral(TypeTag.LONG, 0L, typeMirror, processingEnv);
            case FLOAT:
                return TreeUtils.createLiteral(TypeTag.FLOAT, 0.0f, typeMirror, processingEnv);
            case INT:
                return TreeUtils.createLiteral(TypeTag.INT, 0, typeMirror, processingEnv);
            case DOUBLE:
                return TreeUtils.createLiteral(TypeTag.DOUBLE, 0.0d, typeMirror, processingEnv);
            case BOOLEAN:
                return TreeUtils.createLiteral(TypeTag.BOOLEAN, false, typeMirror, processingEnv);
            default:
                return TreeUtils.createLiteral(TypeTag.BOT, null, typeMirror, processingEnv);
        }
    }

    /**
     * Creates a LiteralTree for the given value.
     *
     * @param typeTag the literal's type tag
     * @param value a wrapped primitive, null, or a String
     * @param typeMirror the typeMirror for the literal
     * @param processingEnv the processing environment
     * @return a LiteralTree for the given type tag and value
     */
    public static LiteralTree createLiteral(
            TypeTag typeTag,
            @Nullable Object value,
            TypeMirror typeMirror,
            ProcessingEnvironment processingEnv) {
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        TreeMaker maker = TreeMaker.instance(context);
        LiteralTree result = maker.Literal(typeTag, value);
        ((JCLiteral) result).type = (Type) typeMirror;
        return result;
    }

    /**
     * Returns true if the given tree evaluates to {@code null}.
     *
     * @param t a tree
     * @return true if the given tree evaluates to {@code null}
     */
    public static boolean isNullExpression(Tree t) {
        while (true) {
            switch (t.getKind()) {
                case PARENTHESIZED:
                    t = ((ParenthesizedTree) t).getExpression();
                    break;
                case TYPE_CAST:
                    t = ((TypeCastTree) t).getExpression();
                    break;
                case NULL_LITERAL:
                    return true;
                default:
                    return false;
            }
        }
    }
}
