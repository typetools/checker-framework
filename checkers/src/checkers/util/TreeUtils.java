package checkers.util;

import java.util.EnumSet;
import java.util.Set;

import checkers.nullness.quals.*;
import checkers.types.AnnotatedTypeMirror;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

import javax.annotation.processing.ProcessingEnvironment;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.JCTree.JCNewClass;

/**
 * A utility class made for helping to analyze a given {@code Tree}.
 */
// TODO: This class needs significant restructuring
public final class TreeUtils {

    // Cannot be instantiated
    private TreeUtils() { throw new AssertionError("un-initializable class"); }

    /**
     * Checks if the provided method is a constructor method or no.
     *
     * @param tree
     *            a tree defining the method
     * @return true iff tree describes a constructor
     */
    public static boolean isConstructor(final MethodTree tree) {
        return (tree.getName().contentEquals("<init>"));
    }

    /**
     * Returns true if the tree is a tree that 'looks like' either an access
     * of a field or an invokation of a method that are owned by the same
     * accessing instance.
     *
     * It would only return true if the access tree is of the form:
     * <pre>
     *   field
     *   this.field
     *
     *   method()
     *   this.method()
     * </pre>
     *
     * It does not perform any semantical check to differentiate between
     * fields and local variables; local methods or imported static methods.
     *
     * @param tree  expression tree representing an access to object member
     * @return {@code true} iff the member is a member of {@code this} instance
     */
    public static boolean isSelfAccess(final ExpressionTree tree) {
        ExpressionTree tr = TreeUtils.skipParens(tree);
        // If method invocation check the method select
        if (tr.getKind() == Tree.Kind.ARRAY_ACCESS)
            return false;

        if (tree.getKind() == Tree.Kind.METHOD_INVOCATION) {
            tr = ((MethodInvocationTree)tree).getMethodSelect();
        }
        tr = TreeUtils.skipParens(tr);
        if (tr.getKind() == Tree.Kind.TYPE_CAST)
            tr = ((TypeCastTree)tr).getExpression();
        tr = TreeUtils.skipParens(tr);

        if (tr.getKind() == Tree.Kind.IDENTIFIER)
            return true;

        if (tr.getKind() == Tree.Kind.MEMBER_SELECT) {
            tr = ((MemberSelectTree)tr).getExpression();
            if (tr.getKind() == Tree.Kind.IDENTIFIER) {
                Name ident = ((IdentifierTree)tr).getName();
                return ident.contentEquals("this") ||
                        ident.contentEquals("super");
            }
        }

        return false;
    }

    /**
     * Checks if the method invocation is a call to super
     *
     * @param tree
     *            a tree defining a method invocation
     *
     * @return true iff tree describes a call to super
     */
    public static boolean isSuperCall(MethodInvocationTree tree) {
        /*@Nullable*/ ExpressionTree mst = tree.getMethodSelect();
        assert mst != null; /*nninvariant*/
        
        if (mst.getKind() == Tree.Kind.IDENTIFIER ) {
            return ((IdentifierTree)mst).getName().contentEquals("super");
        }
        
        if (mst.getKind() == Tree.Kind.MEMBER_SELECT) {
            MemberSelectTree selectTree = (MemberSelectTree)mst;

            if (selectTree.getExpression().getKind() != Tree.Kind.IDENTIFIER) {
                return false;
            }
            
            return ((IdentifierTree) selectTree.getExpression()).getName()
                .contentEquals("super");
        }
        
        return false;
    }


    /**
     * Gets the first enclosing tree in path, of the specified kind.
     *
     * @param path  the path defining the tree node
     * @param kind  the kind of the desired tree
     * @return the enclosing tree of the given type as given by the path
     */
    public static Tree enclosingOfKind(final TreePath path, final Tree.Kind kind) {
        return enclosingOfKind(path, EnumSet.of(kind));
    }

    public static Tree enclosingOfKind(final TreePath path, final Set<Tree.Kind> kinds) {

        TreePath p = path;

        while (p != null) {
            Tree leaf = p.getLeaf();
            assert leaf != null; /*nninvariant*/
            if (kinds.contains(leaf.getKind()))
                return leaf;
            p = p.getParentPath();
        }

        return null;
    }

    public static TreePath pathTillClass(final TreePath path) {
        return pathTillOfKind(path, classTreeKinds());
    }

    /**
     * Gets path to the the first enclosing tree of the specified kind.
     *
     * @param path  the path defining the tree node
     * @param kind  the kind of the desired tree
     * @return the path to the enclosing tree of the given type
     */
    public static TreePath pathTillOfKind(final TreePath path, final Tree.Kind kind) {
        return pathTillOfKind(path, EnumSet.of(kind));
    }

    public static TreePath pathTillOfKind(final TreePath path, final Set<Tree.Kind> kinds) {
        TreePath p = path;

        while (p != null) {
            Tree leaf = p.getLeaf();
            assert leaf != null; /*nninvariant*/
            if (kinds.contains(leaf.getKind()))
                return p;
            p = p.getParentPath();
        }

        return null;
    }

    /**
     * Gets the first enclosing tree in path, of the specified class
     *
     * @param path  the path defining the tree node
     * @param treeClass the class of the desired tree
     * @return the enclosing tree of the given type as given by the path
     */
    public static <T extends Tree> T enclosingOfClass(final TreePath path, final Class<T> treeClass) {
        TreePath p = path;
        while (p != null) {
            Tree leaf = p.getLeaf();
            if (treeClass.isInstance(leaf))
                return treeClass.cast(leaf);
            p = p.getParentPath();
        }
        return null;
    }

    /**
     * Gets the enclosing class of the tree node defined by the given
     * {@code {@link TreePath}}. It returns a {@link Tree}, from which
     * {@link AnnotatedTypeMirror} or {@link Element} can be
     * obtained.
     *
     * @param path
     *            the path defining the tree node
     * @return the enclosing class (or interface) as given by the path, or null
     *         if one does not exist.
     */
    public static /*@Nullable*/ ClassTree enclosingClass(final /*@Nullable*/ TreePath path) {
        return (ClassTree) enclosingOfKind(path, classTreeKinds());
    }

    /**
     * Gets the enclosing variable of a tree node defined by the given
     * {@link TreePath}.
     *
     * @param path the path defining the tree node
     * @return the enclosing variable as given by the path, or null if one does not exist
     */
    public static VariableTree enclosingVariable(final TreePath path) {
        return (VariableTree) enclosingOfKind(path, Tree.Kind.VARIABLE);
    }

    /**
     * Gets the enclosing method of the tree node defined by the given
     * {@code {@link TreePath}}. It returns a {@link Tree}, from which an
     * {@link AnnotatedTypeMirror} or {@link Element} can be
     * obtained.
     *
     * @param path the path defining the tree node
     * @return the enclosing method as given by the path, or null if one does
     *         not exist
     */
    public static /*@Nullable*/ MethodTree enclosingMethod(final /*@Nullable*/ TreePath path) {
        return (MethodTree) enclosingOfKind(path, Tree.Kind.METHOD);
    }

    public static /*@Nullable*/ BlockTree enclosingTopLevelBlock(TreePath path) {
        TreePath parpath = path.getParentPath();
        while (parpath!=null && parpath.getLeaf().getKind() != Tree.Kind.CLASS) {
            path = parpath;
            parpath = parpath.getParentPath();
        }
        if (path.getLeaf().getKind() == Tree.Kind.BLOCK) {
            return (BlockTree) path.getLeaf();
        }
        return null;
    }

    /**
     * If the given tree is a parenthesized tree, it returns the enclosed
     * non-parenthesized tree. Otherwise, it returns the same tree.
     *
     * @param tree  a tree
     * @return  the outermost non-parenthesized tree enclosed by the given tree
     */
    public static Tree skipParens(final Tree tree) {
        Tree t = tree;
        while (t.getKind() == Tree.Kind.PARENTHESIZED)
            t = ((ParenthesizedTree)t).getExpression();
        return t;
    }


    /**
     * If the given tree is a parenthesized tree, it returns the enclosed
     * non-parenthesized tree. Otherwise, it returns the same tree.
     *
     * @param tree  an expression tree
     * @return  the outermost non-parenthesized tree enclosed by the given tree
     */
    public static ExpressionTree skipParens(final ExpressionTree tree) {
        ExpressionTree t = tree;
        while (t.getKind() == Tree.Kind.PARENTHESIZED)
            t = ((ParenthesizedTree)t).getExpression();
        return t;
    }

    /**
     * Returns the tree with the assignment context for the treePath
     * leaf node.
     *
     * The assignment context for the treepath is the most enclosing
     * tree of type:
     * <ul>
     *   <li>AssignmentTree </li>
     *   <li>CompoundAssignmentTree </li>
     *   <li>MethodInvocationTree</li>
     *   <li>NewArrayTree</li>
     *   <li>NewClassTree</li>
     *   <li>ReturnTree</li>
     *   <li>VariableTree</li>
     * </ul>
     *
     * @param treePath
     * @return  the assignment context as described.
     */
    public static Tree getAssignmentContext(final TreePath treePath) {
        TreePath path = treePath.getParentPath();

        if (path == null)
            return null;
        Tree node = path.getLeaf();
        if ((node instanceof AssignmentTree) ||
                (node instanceof CompoundAssignmentTree) ||
                (node instanceof MethodInvocationTree) ||
                (node instanceof NewArrayTree) ||
                (node instanceof NewClassTree) ||
                (node instanceof ReturnTree) ||
                (node instanceof VariableTree))
            return node;
        return null;
    }

    /**
     * Gets the element for a class corresponding to a declaration.
     *
     * @param node
     * @return the element for the given class
     */
    public static final TypeElement elementFromDeclaration(ClassTree node) {
        assert node != null : "null node";
        TypeElement elt = (TypeElement) TreeInfo.symbolFor((JCTree) node);
        return elt;
//        if (elt != null)
//            return elt;
//        TreePath path = trees.getPath(root, node);
//        return (TypeElement)trees.getElement(path);
    }

    /**
     * Gets the element for a method corresponding to a declaration.
     *
     * @param node
     * @return the element for the given method
     */
    public static final ExecutableElement elementFromDeclaration(MethodTree node) {
        assert node != null : "null node";
        ExecutableElement elt = (ExecutableElement)TreeInfo.symbolFor((JCTree)node);
        return elt;
//        if (elt != null)
//            return elt;
//        TreePath path = trees.getPath(root, node);
//        return (ExecutableElement)trees.getElement(path);
    }

    /**
     * Gets the element for a variable corresponding to its declaration.
     *
     * @param node
     * @return the element for the given variable
     */
    public static final VariableElement elementFromDeclaration(VariableTree node) {
        assert node != null : "null node";
        VariableElement elt = (VariableElement)TreeInfo.symbolFor((JCTree)node);
        return elt;
//        if (elt != null)
//            return elt;
//        TreePath path = trees.getPath(root, node);
//        return (VariableElement)trees.getElement(path);
    }

    /**
     * Gets the element for the method corresponding to this invocation. To get
     * the element for a method declaration, use {@link
     * Trees#getElement(TreePath)} instead.
     *
     * @param node the method invocation
     * @return the element for the method that corresponds to this invocation
     */
    public static final ExecutableElement elementFromUse(MethodInvocationTree node) {
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
    public static final Element elementFromUse(IdentifierTree node) {
        return TreeInfo.symbol((JCTree)node);
    }

    public static final boolean isUseOfElement(Tree node) {
        node = TreeUtils.skipParens(node);
        switch (node.getKind()) {
            case IDENTIFIER:
            case MEMBER_SELECT:
            case METHOD_INVOCATION:
                return true;
            default:
                return false;
        }
    }

    public static final Element elementFromUse(ExpressionTree node) {
        node = TreeUtils.skipParens(node);
        switch (node.getKind()) {
        case IDENTIFIER:
        case MEMBER_SELECT:
        case METHOD_INVOCATION:
            return TreeInfo.symbol((JCTree)node);
        default:
            throw new IllegalArgumentException("Tree not use: " + node.getKind());
        }
    }

    public static final ExecutableElement elementFromUse(NewClassTree node) {
        return (ExecutableElement)((JCNewClass)node).constructor;
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
    public static final Element elementFromUse(MemberSelectTree node) {
        return TreeInfo.symbol((JCTree)node);
    }

    /**
     * @return the name of the invoked method
     */
    public static final Name methodName(MethodInvocationTree node) {
        ExpressionTree expr = node.getMethodSelect();
        if (expr.getKind() == Tree.Kind.IDENTIFIER)
            return ((IdentifierTree)expr).getName();
        else if (expr.getKind() == Tree.Kind.MEMBER_SELECT)
            return ((MemberSelectTree)expr).getIdentifier();
        throw new AssertionError("cannot be here: " + node);
    }

    /**
     * @return true if the first statement in the body is a self constructor
     *  invocation within a constructor
     */
    public static final boolean containsThisConstructorInvocation(MethodTree node) {
        if (!TreeUtils.isConstructor(node)
                || node.getBody().getStatements().isEmpty())
            return false;

        StatementTree st = node.getBody().getStatements().get(0);
        if (!(st instanceof ExpressionStatementTree)
                || !(((ExpressionStatementTree)st).getExpression() instanceof MethodInvocationTree))
            return false;

        MethodInvocationTree invocation = (MethodInvocationTree)
            ((ExpressionStatementTree)st).getExpression();

        return "this".contentEquals(TreeUtils.methodName(invocation));
    }

    public static final Tree firstStatement(Tree tree) {
        Tree first;
        if (tree.getKind() == Tree.Kind.BLOCK) {
            BlockTree block = (BlockTree)tree;
            if (block.getStatements().isEmpty())
                first = block;
            else
                first = block.getStatements().iterator().next();
        } else {
            first = tree;
        }
        return first;
    }

    /**
     * Determine whether the given class contains an explicit constructor.
     * 
     * @param node A class tree.
     * @return True, iff there is an explicit constructor.
     */
    public static boolean hasExplicitConstructor(ClassTree node) {
        TypeElement elem = TreeUtils.elementFromDeclaration(node);

        for ( ExecutableElement ee : ElementFilter.constructorsIn(elem.getEnclosedElements())) {
            MethodSymbol ms = (MethodSymbol) ee;
            long mod = ms.flags();

            if ((mod & Flags.SYNTHETIC) == 0) {
                return true;
            }
        }
        return false;
        // WMD Old impl
        // return !ElementFilter.constructorsIn(elem.getEnclosedElements()).isEmpty();
    }

    /**
     * Returns true if the tree is of a diamond type
     */
    public static final boolean isDiamondTree(Tree tree) {
        switch (tree.getKind()) {
        case ANNOTATED_TYPE: return isDiamondTree(((AnnotatedTypeTree)tree).getUnderlyingType());
        case PARAMETERIZED_TYPE: return ((ParameterizedTypeTree)tree).getTypeArguments().isEmpty();
        case NEW_CLASS: return isDiamondTree(((NewClassTree)tree).getIdentifier());
        default: return false;
        }
    }

    /**
     * Returns true if the tree represents a {@code String} concatenation
     * operation
     */
    public static final boolean isStringConcatenation(Tree tree) {
        return (tree.getKind() == Tree.Kind.PLUS
                && TypesUtils.isString(InternalUtils.typeOf(tree)));
    }

    /**
     * Returns true if the compound assignment tree is a string concatenation
     */
    public static final boolean isStringCompoundConcatenation(CompoundAssignmentTree tree) {
        return (tree.getKind() == Tree.Kind.PLUS_ASSIGNMENT
                && TypesUtils.isString(InternalUtils.typeOf(tree)));
    }

    /**
     * Returns true if the node is a constant-time expression.
     *
     * A tree is a constant-time expression if it is:
     * <ol>
     * <li>a literal tree
     * <li>a reference to a final variable initialized with a compile time
     *  constant
     * <li>a String concatination of two compile time constants
     * </ol>
     */
    public static boolean isCompileTimeString(ExpressionTree node) {
        ExpressionTree tree = TreeUtils.skipParens(node);
        if (tree instanceof LiteralTree)
            return true;

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
     * Returns the receiver tree of a field access or a method invocation
     */
    public static ExpressionTree getReceiverTree(ExpressionTree expression) {
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
            return ((ArrayAccessTree)receiver).getExpression();

        // Avoid int.class
        if (expression.getKind() == Tree.Kind.MEMBER_SELECT &&
                ((MemberSelectTree)expression).getExpression() instanceof PrimitiveTypeTree)
            return null;

        if (isSelfAccess(expression)) {
            return null;
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

        return receiver;
    }

    // TODO: What about anonymous classes?
    // Adding Tree.Kind.NEW_CLASS here doesn't work, because then a 
    // tree gets cast to ClassTree when it is actually a NewClassTree,
    // for example in enclosingClass above.
    private final static Set<Tree.Kind> classTreeKinds = EnumSet.of(
            Tree.Kind.CLASS,
            Tree.Kind.ENUM,
            Tree.Kind.INTERFACE,
            Tree.Kind.ANNOTATION_TYPE
    );

    public static Set<Tree.Kind> classTreeKinds() {
        return classTreeKinds;
    }

    public static boolean isClassTree(Tree.Kind kind) {
        return classTreeKinds().contains(kind);
    }


    /**
     * Returns true if the given element is an invocation of the method, or
     * of any method that overrides that one.
     */
    public static boolean isMethodInvocation(Tree tree, ExecutableElement method, ProcessingEnvironment env) {
        if (!(tree instanceof MethodInvocationTree))
            return false;
        MethodInvocationTree methInvok = (MethodInvocationTree)tree;
        ExecutableElement invoked = TreeUtils.elementFromUse(methInvok);
        return isMethod(invoked, method, env);
    }

    /** Returns true if the given element is, or overrides, method. */
    private static boolean isMethod(ExecutableElement questioned, ExecutableElement method, ProcessingEnvironment env) {
        return (questioned.equals(method)
                || env.getElementUtils().overrides(questioned, method,
                        (TypeElement)questioned.getEnclosingElement()));
    }
    
    /**
     * Returns the ExecutableElement for a method declaration of
     * methodName, in class typeName, with params parameters.
     */
    public static ExecutableElement getMethod(String typeName, String methodName, int params, ProcessingEnvironment env) {
        TypeElement mapElt = env.getElementUtils().getTypeElement(typeName);
        for (ExecutableElement exec : ElementFilter.methodsIn(mapElt.getEnclosedElements())) {
            if (exec.getSimpleName().contentEquals(methodName)
                    && exec.getParameters().size() == params)
                return exec;
        }
        throw new RuntimeException("Shouldn't be here!");
    }
    
    /**
     * Returns true if the given tree represents an access of the given VariableElement.
     */
    public static boolean isFieldAccess(Tree tree, VariableElement var, ProcessingEnvironment env) {
        if (!(tree instanceof MemberSelectTree)) {
            return false;
        }
        MemberSelectTree memSel = (MemberSelectTree) tree;
        Element field = TreeUtils.elementFromUse(memSel);
        return field.equals(var);
    }
    
    /**
     * Returns the VariableElement for a field declaration.
     *
     * @param typeName the class where the field is declared.
     * @param fieldName the name of the field.
     * @param env the processing environment.
     * @return the VariableElement for typeName.fieldName
     */
    public static VariableElement getField(String typeName, String fieldName, ProcessingEnvironment env) {
        TypeElement mapElt = env.getElementUtils().getTypeElement(typeName);
        for (VariableElement var : ElementFilter.fieldsIn(mapElt.getEnclosedElements())) {
            if (var.getSimpleName().contentEquals(fieldName)) {
                return var;
            }
        }
        throw new RuntimeException("Shouldn't be here!");
    }
}
