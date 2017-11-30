package org.checkerframework.checker.interning;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.Comparator;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.checker.interning.qual.UsesObjectEquals;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.Heuristics;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Typechecks source code for interning violations. A type is considered interned if its primary
 * annotation is {@link Interned} or {@link InternedDistinct}. This visitor reports errors or
 * warnings for violations for the following cases:
 *
 * <ol>
 *   <li value="1">either argument to a "==" or "!=" comparison is not Interned (error
 *       "not.interned"). As a special case, the comparison is permitted if either arugment is
 *       InternedDistinct.
 *   <li value="2">the receiver and argument for a call to an equals method are both Interned
 *       (optional warning "unnecessary.equals")
 * </ol>
 *
 * @see BaseTypeVisitor
 */
public final class InterningVisitor extends BaseTypeVisitor<InterningAnnotatedTypeFactory> {

    /** The @Interned annotation. */
    private final AnnotationMirror INTERNED;
    /** The @InternedDistinct annotation. */
    private final AnnotationMirror INTERNED_DISTINCT;
    /** See method typeToCheck() */
    private final DeclaredType typeToCheck;

    public InterningVisitor(BaseTypeChecker checker) {
        super(checker);
        this.INTERNED = AnnotationBuilder.fromClass(elements, Interned.class);
        this.INTERNED_DISTINCT = AnnotationBuilder.fromClass(elements, InternedDistinct.class);
        typeToCheck = typeToCheck();
    }

    /**
     * @return true if interning should be verified for the input expression. By default, all
     *     classes are checked for interning unless {@code -Acheckclass} is specified.
     * @see <a href="https://checkerframework.org/manual/#interning-checks">What the Interning
     *     Checker checks</a>
     */
    private boolean shouldCheckExpression(ExpressionTree tree) {
        if (typeToCheck == null) return true;

        TypeMirror type = TreeUtils.typeOf(tree);
        return types.isSubtype(type, typeToCheck) || types.isSubtype(typeToCheck, type);
    }

    /** Checks comparison operators, == and !=, for INTERNING violations. */
    @Override
    public Void visitBinary(BinaryTree node, Void p) {

        // No checking unless the operator is "==" or "!=".
        if (!(node.getKind() == Tree.Kind.EQUAL_TO || node.getKind() == Tree.Kind.NOT_EQUAL_TO)) {
            return super.visitBinary(node, p);
        }

        ExpressionTree leftOp = node.getLeftOperand();
        ExpressionTree rightOp = node.getRightOperand();

        // Check passes if either arg is null.
        if (leftOp.getKind() == Tree.Kind.NULL_LITERAL
                || rightOp.getKind() == Tree.Kind.NULL_LITERAL) {
            return super.visitBinary(node, p);
        }

        AnnotatedTypeMirror left = atypeFactory.getAnnotatedType(leftOp);
        AnnotatedTypeMirror right = atypeFactory.getAnnotatedType(rightOp);

        // If either argument is a primitive, check passes due to auto-unboxing
        if (left.getKind().isPrimitive() || right.getKind().isPrimitive()) {
            return super.visitBinary(node, p);
        }

        if (left.hasEffectiveAnnotation(INTERNED_DISTINCT)
                || right.hasEffectiveAnnotation(INTERNED_DISTINCT)) {
            return super.visitBinary(node, p);
        }

        // If shouldCheckExpression returns true for either the LHS or RHS,
        // this method proceeds with the interning check.

        // Justification: Consider the following scenario:

        // interface I { ... }
        // class A { ... }
        // class B extends A implements I { ... }
        // ...
        // I i;
        // A a;
        // ...
        // if (a == i) { ... }

        // The Java compiler does not issue a compilation error for the (a == i) comparison because,
        // even though A does not implement I, 'a' could be assigned an instance of B, and B does
        // implement I (note that the compiler does not need to know about the existence of B
        // in order to assume this).

        // Now suppose the user passes -AcheckClass=A on the command-line.
        // I is not a subtype or supertype of A, so shouldCheckExpression will not return true for
        // I.
        // But the interning check must be performed, given the argument above.  Therefore if
        // shouldCheckExpression returns true for either the LHS or the RHS, this method proceeds
        // with the interning check.

        if (!shouldCheckExpression(leftOp) && !shouldCheckExpression(rightOp)) {
            return super.visitBinary(node, p);
        }

        // Syntactic checks for legal uses of ==
        if (suppressInsideComparison(node)) {
            return super.visitBinary(node, p);
        }
        if (suppressEarlyEquals(node)) {
            return super.visitBinary(node, p);
        }
        if (suppressEarlyCompareTo(node)) {
            return super.visitBinary(node, p);
        }

        if (suppressEqualsIfClassIsAnnotated(left, right)) {
            return super.visitBinary(node, p);
        }

        Element leftElt = null;
        Element rightElt = null;
        if (left instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
            leftElt = ((DeclaredType) left.getUnderlyingType()).asElement();
        }
        if (right instanceof AnnotatedTypeMirror.AnnotatedDeclaredType) {
            rightElt = ((DeclaredType) right.getUnderlyingType()).asElement();
        }

        // TODO: CODE REVIEW
        // TODO: WOULD IT BE CLEARER TO USE A METHOD usesReferenceEquality(AnnotatedTypeMirror type)
        // TODO: RATHER THAN leftElt.getAnnotation(UsesObjectEquals.class) != null)
        // if neither @Interned or @UsesObjectEquals, report error
        if (!(left.hasEffectiveAnnotation(INTERNED)
                || (leftElt != null && leftElt.getAnnotation(UsesObjectEquals.class) != null))) {
            checker.report(Result.failure("not.interned", left), leftOp);
        }
        if (!(right.hasEffectiveAnnotation(INTERNED)
                || (rightElt != null && rightElt.getAnnotation(UsesObjectEquals.class) != null))) {
            checker.report(Result.failure("not.interned", right), rightOp);
        }
        return super.visitBinary(node, p);
    }

    /**
     * If lint option "dotequals" is specified, warn if the .equals method is used where reference
     * equality is safe.
     */
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        if (isInvocationOfEquals(node)) {
            AnnotatedTypeMirror recv = atypeFactory.getReceiverType(node);
            AnnotatedTypeMirror comp = atypeFactory.getAnnotatedType(node.getArguments().get(0));

            if (this.checker.getLintOption("dotequals", true)
                    && recv.hasEffectiveAnnotation(INTERNED)
                    && comp.hasEffectiveAnnotation(INTERNED))
                checker.report(Result.warning("unnecessary.equals"), node);
        }

        return super.visitMethodInvocation(node, p);
    }

    /**
     * Method to implement the @UsesObjectEquals functionality. If a class is annotated
     * with @UsesObjectEquals, it must:
     *
     * <ul>
     *   <li>not override .equals(Object)
     *   <li>be a subclass of Object or another class annotated with @UsesObjectEquals
     * </ul>
     *
     * If a class is not annotated with @UsesObjectEquals, it must:
     *
     * <ul>
     *   <li>not have a superclass annotated with @UsesObjectEquals
     * </ul>
     *
     * @see
     *     org.checkerframework.common.basetype.BaseTypeVisitor#visitClass(com.sun.source.tree.ClassTree,
     *     java.lang.Object)
     */
    @Override
    public void processClassTree(ClassTree node) {
        // TODO: Should this method use the Javac types or some other utility to get
        // all direct supertypes instead, and should it verify that each does not
        // override .equals and that at least one of them is annotated with @UsesObjectEquals?

        // Looking for an @UsesObjectEquals class declaration

        TypeElement elt = TreeUtils.elementFromDeclaration(node);
        UsesObjectEquals annotation = elt.getAnnotation(UsesObjectEquals.class);

        Tree superClass = node.getExtendsClause();
        Element elmt = null;
        if (superClass != null
                && (superClass instanceof IdentifierTree
                        || superClass instanceof MemberSelectTree)) {
            elmt = TreeUtils.elementFromUse((ExpressionTree) superClass);
        }

        // If @UsesObjectEquals is present, check to make sure the class does not override equals
        // and its supertype is Object or is annotated with @UsesObjectEquals.
        if (annotation != null) {
            // Check methods to ensure no .equals
            if (overridesEquals(node)) {
                checker.report(Result.failure("overrides.equals"), node);
            }

            if (!(superClass == null
                    || (elmt != null && elmt.getAnnotation(UsesObjectEquals.class) != null))) {
                checker.report(Result.failure("superclass.notannotated"), node);
            }
        } else {
            // The class is not annotated with @UsesObjectEquals -> make sure its superclass isn't
            // either.
            // TODO: is this impossible after the design change making @UsesObjectEquals inherited?
            // This check is left behind in case of a future design change back to non-inherited.
            if (superClass != null
                    && (elmt != null && elmt.getAnnotation(UsesObjectEquals.class) != null)) {
                checker.report(Result.failure("superclass.annotated"), node);
            }
        }

        super.processClassTree(node);
    }

    // **********************************************************************
    // Helper methods
    // **********************************************************************

    /** Returns true if a class overrides Object.equals */
    private boolean overridesEquals(ClassTree node) {
        List<? extends Tree> members = node.getMembers();
        for (Tree member : members) {
            if (member instanceof MethodTree) {
                MethodTree mTree = (MethodTree) member;
                ExecutableElement enclosing = TreeUtils.elementFromDeclaration(mTree);
                if (overrides(enclosing, Object.class, "equals")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tests whether a method invocation is an invocation of {@link #equals} that overrides or hides
     * {@link Object#equals(Object)}.
     *
     * <p>Returns true even if a method does not override {@link Object#equals(Object)}, because of
     * the common idiom of writing an equals method with a non-Object parameter, in addition to the
     * equals method that overrides {@link Object#equals(Object)}.
     *
     * @param node a method invocation node
     * @return true iff {@code node} is a invocation of {@code equals()}
     */
    private boolean isInvocationOfEquals(MethodInvocationTree node) {
        ExecutableElement method = TreeUtils.elementFromUse(node);
        // TODO: CODE REVIEW NEITHER OF THE TWO
        return (method.getParameters().size() == 1
                && method.getReturnType().getKind() == TypeKind.BOOLEAN
                // method symbols only have simple names
                && method.getSimpleName().contentEquals("equals"));
    }

    /**
     * Tests whether a method invocation is an invocation of {@link Comparable#compareTo}.
     *
     * @param node a method invocation node
     * @return true iff {@code node} is a invocation of {@code compareTo()}
     */
    private boolean isInvocationOfCompareTo(MethodInvocationTree node) {
        ExecutableElement method = TreeUtils.elementFromUse(node);
        return (method.getParameters().size() == 1
                && method.getReturnType().getKind() == TypeKind.INT
                // method symbols only have simple names
                && method.getSimpleName().contentEquals("compareTo"));
    }

    /**
     * Pattern matches particular comparisons to avoid common false positives in the {@link
     * Comparable#compareTo(Object)} and {@link Object#equals(Object)}.
     *
     * <p>Specifically, this method tests if: the comparison is a == comparison, it is the test of
     * an if statement that's the first statement in the method, and one of the following is true:
     *
     * <ol>
     *   <li>the method overrides {@link Comparator#compare}, the "then" branch of the if statement
     *       returns zero, and the comparison tests equality of the method's two parameters
     *   <li>the method overrides {@link Object#equals(Object)} and the comparison tests "this"
     *       against the method's parameter
     *   <li>the method overrides {@link Comparable#compareTo(Object)}, the "then" branch of the if
     *       statement returns zero, and the comparison tests "this" against the method's parameter
     * </ol>
     *
     * @param node the comparison to check
     * @return true if one of the supported heuristics is matched, false otherwise
     */
    // TODO: handle != comparisons too!
    // TODO: handle more methods, such as early return from addAll when this == arg
    private boolean suppressInsideComparison(final BinaryTree node) {
        // Only handle == binary trees
        if (node.getKind() != Tree.Kind.EQUAL_TO) {
            return false;
        }

        Tree left = node.getLeftOperand();
        Tree right = node.getRightOperand();

        // Only valid if we're comparing identifiers.
        if (!(left.getKind() == Tree.Kind.IDENTIFIER && right.getKind() == Tree.Kind.IDENTIFIER))
            return false;

        // If we're not directly in an if statement in a method (ignoring
        // parens and blocks), terminate.
        if (!Heuristics.matchParents(getCurrentPath(), Tree.Kind.IF, Tree.Kind.METHOD)) {
            return false;
        }

        // Ensure the if statement is the first statement in the method

        TreePath parentPath = getCurrentPath().getParentPath();

        // Retrieve the enclosing if statement tree and method tree
        Tree tree, ifStatementTree = null;
        MethodTree methodTree = null;
        while ((tree = parentPath.getLeaf()) != null) {
            if (tree.getKind() == Tree.Kind.IF) {
                ifStatementTree = tree;
            } else if (tree.getKind() == Tree.Kind.METHOD) {
                methodTree = (MethodTree) tree;
                break;
            }

            parentPath = parentPath.getParentPath();
        }

        // The call to Heuristics.matchParents already ensured there is an enclosing if statement
        assert ifStatementTree != null;
        // The call to Heuristics.matchParents already ensured there is an enclosing method
        assert methodTree != null;

        StatementTree stmnt = methodTree.getBody().getStatements().get(0);
        // The call to Heuristics.matchParents already ensured the enclosing method has at least one statement (an if statement) in the body
        assert stmnt != null;

        if (stmnt != ifStatementTree) {
            return false; // The if statement is not the first statement in the method.
        }

        ExecutableElement enclosing =
                TreeUtils.elementFromDeclaration(visitorState.getMethodTree());
        assert enclosing != null;

        final Element lhs = TreeUtils.elementFromUse((IdentifierTree) left);
        final Element rhs = TreeUtils.elementFromUse((IdentifierTree) right);

        // Matcher to check for if statement that returns zero
        Heuristics.Matcher matcherIfReturnsZero =
                new Heuristics.Matcher() {

                    @Override
                    public Boolean visitIf(IfTree tree, Void p) {
                        return visit(tree.getThenStatement(), p);
                    }

                    @Override
                    public Boolean visitBlock(BlockTree tree, Void p) {
                        if (tree.getStatements().size() > 0) {
                            return visit(tree.getStatements().get(0), p);
                        }
                        return false;
                    }

                    @Override
                    public Boolean visitReturn(ReturnTree tree, Void p) {
                        ExpressionTree expr = tree.getExpression();
                        return (expr != null
                                && expr.getKind() == Tree.Kind.INT_LITERAL
                                && ((LiteralTree) expr).getValue().equals(0));
                    }
                };

        // Determine whether or not the "then" statement of the if has a single
        // "return 0" statement (for the Comparator.compare heuristic).
        if (overrides(enclosing, Comparator.class, "compare")) {
            final boolean returnsZero =
                    Heuristics.Matchers.withIn(
                                    Heuristics.Matchers.ofKind(Tree.Kind.IF, matcherIfReturnsZero))
                            .match(getCurrentPath());

            if (!returnsZero) {
                return false;
            }

            assert enclosing.getParameters().size() == 2;
            Element p1 = enclosing.getParameters().get(0);
            Element p2 = enclosing.getParameters().get(1);
            return (p1.equals(lhs) && p2.equals(rhs)) || (p2.equals(lhs) && p1.equals(rhs));

        } else if (overrides(enclosing, Object.class, "equals")) {
            assert enclosing.getParameters().size() == 1;
            Element param = enclosing.getParameters().get(0);
            Element thisElt = getThis(trees.getScope(getCurrentPath()));
            assert thisElt != null;
            return (thisElt.equals(lhs) && param.equals(rhs))
                    || (param.equals(lhs) && thisElt.equals(rhs));

        } else if (overrides(enclosing, Comparable.class, "compareTo")) {

            final boolean returnsZero =
                    Heuristics.Matchers.withIn(
                                    Heuristics.Matchers.ofKind(Tree.Kind.IF, matcherIfReturnsZero))
                            .match(getCurrentPath());

            if (!returnsZero) {
                return false;
            }

            assert enclosing.getParameters().size() == 1;
            Element param = enclosing.getParameters().get(0);
            Element thisElt = getThis(trees.getScope(getCurrentPath()));
            assert thisElt != null;
            return (thisElt.equals(lhs) && param.equals(rhs))
                    || (param.equals(lhs) && thisElt.equals(rhs));
        }
        return false;
    }

    /**
     * Returns true if two expressions originating from the same scope are identical, i.e. they are
     * syntactically represented in the same way (modulo parentheses) and represent the same value.
     *
     * <p>For example, given an expression (a == b) || a.equals(b) sameTree can be called to
     * determine that the first 'a' and second 'a' refer to the same variable, which is the case
     * since both expressions 'a' originate from the same scope.
     *
     * <p>If the expression includes one or more method calls, assumes the method calls are
     * deterministic.
     *
     * @param expr1 the first expression to compare
     * @param expr2 the second expression to compare - expr2 must originate from the same scope as
     *     expr1
     * @return true if the expressions expr1 and expr2 are identical
     */
    private static boolean sameTree(ExpressionTree expr1, ExpressionTree expr2) {
        return TreeUtils.skipParens(expr1)
                .toString()
                .equals(TreeUtils.skipParens(expr2).toString());
    }

    /**
     * Pattern matches to prevent false positives of the forms:
     *
     * <pre>
     *   (a == b) || a.equals(b)
     *   (a == b) || (a != null ? a.equals(b) : false)
     *   (a == b) || (a != null &amp;&amp; a.equals(b))
     * </pre>
     *
     * Returns true iff the given node fits this pattern.
     *
     * @return true iff the node fits a pattern such as (a == b || a.equals(b))
     */
    private boolean suppressEarlyEquals(final BinaryTree node) {
        // Only handle == binary trees
        if (node.getKind() != Tree.Kind.EQUAL_TO) {
            return false;
        }

        // should strip parens
        final ExpressionTree left = TreeUtils.skipParens(node.getLeftOperand());
        final ExpressionTree right = TreeUtils.skipParens(node.getRightOperand());

        // looking for ((a == b || a.equals(b))
        Heuristics.Matcher matcherEqOrEquals =
                new Heuristics.Matcher() {

                    /** Returns true if e is either "e1 != null" or "e2 != null". */
                    private boolean isNeqNull(
                            ExpressionTree e, ExpressionTree e1, ExpressionTree e2) {
                        e = TreeUtils.skipParens(e);
                        if (e.getKind() != Tree.Kind.NOT_EQUAL_TO) {
                            return false;
                        }
                        ExpressionTree neqLeft = ((BinaryTree) e).getLeftOperand();
                        ExpressionTree neqRight = ((BinaryTree) e).getRightOperand();
                        return (((sameTree(neqLeft, e1) || sameTree(neqLeft, e2))
                                        && neqRight.getKind() == Tree.Kind.NULL_LITERAL)
                                // also check for "null != e1" and "null != e2"
                                || ((sameTree(neqRight, e1) || sameTree(neqRight, e2))
                                        && neqLeft.getKind() == Tree.Kind.NULL_LITERAL));
                    }

                    @Override
                    public Boolean visitBinary(BinaryTree tree, Void p) {
                        ExpressionTree leftTree = tree.getLeftOperand();
                        ExpressionTree rightTree = tree.getRightOperand();

                        if (tree.getKind() == Tree.Kind.CONDITIONAL_OR) {
                            if (sameTree(leftTree, node)) {
                                // left is "a==b"
                                // check right, which should be a.equals(b) or b.equals(a) or
                                // similar
                                return visit(rightTree, p);
                            } else {
                                return false;
                            }
                        }

                        if (tree.getKind() == Tree.Kind.CONDITIONAL_AND) {
                            // looking for: (a != null && a.equals(b)))
                            if (isNeqNull(leftTree, left, right)) {
                                return visit(rightTree, p);
                            }
                            return false;
                        }

                        return false;
                    }

                    @Override
                    public Boolean visitConditionalExpression(
                            ConditionalExpressionTree tree, Void p) {
                        // looking for: (a != null ? a.equals(b) : false)
                        ExpressionTree cond = tree.getCondition();
                        ExpressionTree trueExp = tree.getTrueExpression();
                        ExpressionTree falseExp = tree.getFalseExpression();
                        if (isNeqNull(cond, left, right)
                                && (falseExp.getKind() == Tree.Kind.BOOLEAN_LITERAL)
                                && ((LiteralTree) falseExp).getValue().equals(false)) {
                            return visit(trueExp, p);
                        }
                        return false;
                    }

                    @Override
                    public Boolean visitMethodInvocation(MethodInvocationTree tree, Void p) {
                        if (!isInvocationOfEquals(tree)) {
                            return false;
                        }

                        List<? extends ExpressionTree> args = tree.getArguments();
                        if (args.size() != 1) {
                            return false;
                        }
                        ExpressionTree arg = args.get(0);
                        // if (arg.getKind() != Tree.Kind.IDENTIFIER) {
                        //     return false;
                        // }
                        // Element argElt = TreeUtils.elementFromUse((IdentifierTree) arg);

                        ExpressionTree exp = tree.getMethodSelect();
                        if (exp.getKind() != Tree.Kind.MEMBER_SELECT) {
                            return false;
                        }
                        MemberSelectTree member = (MemberSelectTree) exp;
                        ExpressionTree receiver = member.getExpression();
                        // Element refElt = TreeUtils.elementFromUse(receiver);

                        // if (!((refElt.equals(lhs) && argElt.equals(rhs)) ||
                        //       ((refElt.equals(rhs) && argElt.equals(lhs))))) {
                        //     return false;
                        // }

                        if (sameTree(receiver, left) && sameTree(arg, right)) {
                            return true;
                        }
                        if (sameTree(receiver, right) && sameTree(arg, left)) {
                            return true;
                        }

                        return false;
                    }
                };

        boolean okay =
                Heuristics.Matchers.withIn(
                                Heuristics.Matchers.ofKind(
                                        Tree.Kind.CONDITIONAL_OR, matcherEqOrEquals))
                        .match(getCurrentPath());
        return okay;
    }

    /**
     * Pattern matches to prevent false positives of the form {@code (a == b || a.compareTo(b) ==
     * 0)}. Returns true iff the given node fits this pattern.
     *
     * @return true iff the node fits the pattern (a == b || a.compareTo(b) == 0)
     */
    private boolean suppressEarlyCompareTo(final BinaryTree node) {
        // Only handle == binary trees
        if (node.getKind() != Tree.Kind.EQUAL_TO) {
            return false;
        }

        Tree left = TreeUtils.skipParens(node.getLeftOperand());
        Tree right = TreeUtils.skipParens(node.getRightOperand());

        // Only valid if we're comparing identifiers.
        if (!(left.getKind() == Tree.Kind.IDENTIFIER && right.getKind() == Tree.Kind.IDENTIFIER)) {
            return false;
        }

        final Element lhs = TreeUtils.elementFromUse((IdentifierTree) left);
        final Element rhs = TreeUtils.elementFromUse((IdentifierTree) right);

        // looking for ((a == b || a.compareTo(b) == 0)
        Heuristics.Matcher matcherEqOrCompareTo =
                new Heuristics.Matcher() {

                    @Override
                    public Boolean visitBinary(BinaryTree tree, Void p) {
                        if (tree.getKind() == Tree.Kind.EQUAL_TO) { // a.compareTo(b) == 0
                            ExpressionTree leftTree =
                                    tree.getLeftOperand(); // looking for a.compareTo(b) or
                            // b.compareTo(a)
                            ExpressionTree rightTree = tree.getRightOperand(); // looking for 0

                            if (rightTree.getKind() != Tree.Kind.INT_LITERAL) {
                                return false;
                            }
                            LiteralTree rightLiteral = (LiteralTree) rightTree;
                            if (!rightLiteral.getValue().equals(0)) {
                                return false;
                            }

                            return visit(leftTree, p);
                        } else {
                            // a == b || a.compareTo(b) == 0
                            ExpressionTree leftTree = tree.getLeftOperand(); // looking for a==b
                            ExpressionTree rightTree =
                                    tree.getRightOperand(); // looking for a.compareTo(b) == 0
                            // or b.compareTo(a) == 0
                            if (leftTree != node) {
                                return false;
                            }
                            if (rightTree.getKind() != Tree.Kind.EQUAL_TO) {
                                return false;
                            }
                            return visit(rightTree, p);
                        }
                    }

                    @Override
                    public Boolean visitMethodInvocation(MethodInvocationTree tree, Void p) {
                        if (!isInvocationOfCompareTo(tree)) {
                            return false;
                        }

                        List<? extends ExpressionTree> args = tree.getArguments();
                        if (args.size() != 1) {
                            return false;
                        }
                        ExpressionTree arg = args.get(0);
                        if (arg.getKind() != Tree.Kind.IDENTIFIER) {
                            return false;
                        }
                        Element argElt = TreeUtils.elementFromUse(arg);

                        ExpressionTree exp = tree.getMethodSelect();
                        if (exp.getKind() != Tree.Kind.MEMBER_SELECT) {
                            return false;
                        }
                        MemberSelectTree member = (MemberSelectTree) exp;
                        if (member.getExpression().getKind() != Tree.Kind.IDENTIFIER) {
                            return false;
                        }

                        Element refElt = TreeUtils.elementFromUse(member.getExpression());

                        if (!((refElt.equals(lhs) && argElt.equals(rhs))
                                || ((refElt.equals(rhs) && argElt.equals(lhs))))) {
                            return false;
                        }
                        return true;
                    }
                };

        boolean okay =
                Heuristics.Matchers.withIn(
                                Heuristics.Matchers.ofKind(
                                        Tree.Kind.CONDITIONAL_OR, matcherEqOrCompareTo))
                        .match(getCurrentPath());
        return okay;
    }

    /**
     * Given {@code a == b}, where a has type A and b has type B, don't issue a warning when either
     * the declaration of A or that of B is annotated with @Interned because {@code a == b} will be
     * true only if a's run-time type is B (or lower), in which case a is actually interned.
     */
    private boolean suppressEqualsIfClassIsAnnotated(
            AnnotatedTypeMirror left, AnnotatedTypeMirror right) {
        // It would be better to just test their greatest lower bound.
        // That could permit some comparisons that this forbids.
        return classIsAnnotated(left) || classIsAnnotated(right);
    }

    /** Returns true if the type's declaration has an @Interned annotation. */
    private boolean classIsAnnotated(AnnotatedTypeMirror type) {

        TypeMirror tm = type.getUnderlyingType();
        if (tm == null) {
            // Maybe a type variable or wildcard had no upper bound
            return false;
        }

        tm = TypesUtils.findConcreteUpperBound(tm);
        if (tm == null || tm.getKind() == TypeKind.ARRAY) {
            // Bound of a wildcard might be null
            return false;
        }

        if (tm.getKind() != TypeKind.DECLARED) {
            checker.message(
                    Kind.WARNING,
                    "InterningVisitor.classIsAnnotated: tm = %s (%s)%n",
                    tm,
                    tm.getClass());
        }
        Element classElt = ((DeclaredType) tm).asElement();
        if (classElt == null) {
            checker.message(
                    Kind.WARNING,
                    "InterningVisitor.classIsAnnotated: classElt = null for tm = %s (%s)%n",
                    tm,
                    tm.getClass());
        }
        if (classElt != null) {
            AnnotatedTypeMirror classType = atypeFactory.fromElement(classElt);
            assert classType != null;
            for (AnnotationMirror anno : classType.getAnnotations()) {
                if (INTERNED.equals(anno)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines the element corresponding to "this" inside a scope. Returns null within static
     * methods.
     *
     * @param scope the scope to search for the element corresponding to "this" in
     * @return the element corresponding to "this" in the given scope, or null if not found
     */
    private Element getThis(Scope scope) {
        for (Element e : scope.getLocalElements()) {
            if (e.getSimpleName().contentEquals("this")) {
                return e;
            }
        }
        return null;
    }

    /**
     * Determines whether or not the given element overrides the named method in the named class.
     *
     * @param e an element for a method
     * @param clazz the class
     * @param method the name of a method
     * @return true if the method given by {@code e} overrides the named method in the named class;
     *     false otherwise
     */
    private boolean overrides(ExecutableElement e, Class<?> clazz, String method) {

        // Get the element named by "clazz".
        TypeElement clazzElt = elements.getTypeElement(clazz.getCanonicalName());
        assert clazzElt != null;

        // Check all of the methods in the class for name matches and overriding.
        for (ExecutableElement elt : ElementFilter.methodsIn(clazzElt.getEnclosedElements())) {
            if (elt.getSimpleName().contentEquals(method) && elements.overrides(e, elt, clazzElt)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the declared type of which the equality tests should be tested, if the user
     * explicitly passed one. The user can pass the class name via the {@code -Acheckclass=...}
     * option.
     *
     * <p>If no class is specified, or the class specified isn't in the classpath, it returns null.
     */
    DeclaredType typeToCheck() {
        String className = checker.getOption("checkclass");
        if (className == null) {
            return null;
        }

        TypeElement classElt = elements.getTypeElement(className);
        if (classElt == null) {
            return null;
        }

        return types.getDeclaredType(classElt);
    }
}
