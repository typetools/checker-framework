package checkers.interning;

import java.util.*;

import checkers.source.*;
import checkers.basetype.*;
import checkers.interning.quals.Interned;
import checkers.types.*;
import checkers.util.*;
import com.sun.source.tree.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import static javax.lang.model.util.ElementFilter.*;

/**
 * A type-checking visitor for the {@link Interned} type
 * qualifier that uses the {@link BaseTypeVisitor} implementation. This visitor
 * reports errors or warnings for violations for the following cases:
 *
 * <ol>
 * <li value="1">either argument to a "==" or "!=" comparison is not Interned (error
 *    "not.interned")
 * <li value="2">the receiver and argument for a call to an equals method are both
 *    Interned (optional warning "unnecessary.equals")
 * </ol>
 *
 * @see BaseTypeVisitor
 */
public final class InterningVisitor extends BaseTypeVisitor<Void, Void> {

    /** The interned annotation. */
    private final AnnotationMirror INTERNED;
    private final DeclaredType typeToCheck;

    /**
     * Creates a new visitor for type-checking {@link Interned}.
     *
     * @param checker the checker to use
     * @param root the root of the input program's AST to check
     */
    public InterningVisitor(InterningChecker checker, CompilationUnitTree root) {
        super(checker, root);
        this.INTERNED = annoFactory.fromClass(Interned.class);
        typeToCheck = checker.typeToCheck();
    }

    private boolean shouldCheckFor(ExpressionTree tree) {
        if (typeToCheck == null) return true;

        TypeMirror type = InternalUtils.typeOf(tree);
        return types.isSubtype(type, typeToCheck) || types.isSubtype(typeToCheck, type);
    }

    @Override
    public Void visitBinary(BinaryTree node, Void p) {

        // No checking unless the operator is "==" or "!=".
        if (!(node.getKind() == Tree.Kind.EQUAL_TO ||
                node.getKind() == Tree.Kind.NOT_EQUAL_TO))
            return super.visitBinary(node, p);

        ExpressionTree leftOp = node.getLeftOperand(),
                       rightOp = node.getRightOperand();

        // Check passes if either arg is null.
        if (leftOp.getKind() == Tree.Kind.NULL_LITERAL ||
                rightOp.getKind() == Tree.Kind.NULL_LITERAL)
            return super.visitBinary(node, p);

        if (suppressByHeuristic(node))
            return super.visitBinary(node, p);

        if (!shouldCheckFor(leftOp) && !shouldCheckFor(rightOp))
            return super.visitBinary(node, p);

        AnnotatedTypeMirror left = atypeFactory.getAnnotatedType(leftOp);
        AnnotatedTypeMirror right = atypeFactory.getAnnotatedType(rightOp);

        // If either argument is a primitive, check passes due to auto-unboxing
        if (left.getKind().isPrimitive() || right.getKind().isPrimitive())
            return super.visitBinary(node, p);

        if (!left.hasAnnotation(INTERNED))
            checker.report(Result.failure("not.interned", left), leftOp);
        if (!right.hasAnnotation(INTERNED))
            checker.report(Result.failure("not.interned", right), rightOp);

        return super.visitBinary(node, p);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        if (isInvocationOfEquals(node)) {
            AnnotatedTypeMirror recv = atypeFactory.getReceiver(node);
            AnnotatedTypeMirror comp = atypeFactory.getAnnotatedType(node.getArguments().get(0));

            if (this.checker.getLintOption("dotequals", true)
                    && recv.hasAnnotation(INTERNED)
                    && comp.hasAnnotation(INTERNED))
                checker.report(Result.warning("unnecessary.equals"), node);
        }

        return super.visitMethodInvocation(node, p);
    }

    // **********************************************************************
    // Helper methods
    // **********************************************************************

    /**
     * Tests whether a method invocation is an invocation of
     * {@link #equals} that overrides or hides {@link Object#equals(Object)}.
     * <p>
     *
     * Returns true even if a method does not override {@link Object.equals},
     * because of the common idiom of writing an equals method with a non-Object
     * parameter, in addition to the equals method that overrides
     * {@link Object.equals}.
     *
     * @param node a method invocation node
     * @return true iff {@code node} is a invocation of {@code equals()}
     */
    private boolean isInvocationOfEquals(MethodInvocationTree node) {
        ExecutableElement method = TreeUtils.elementFromUse(node);
        return (method.getParameters().size() == 1
                && method.getReturnType().getKind() == TypeKind.BOOLEAN
                // method symbols only have simple names
                && method.getSimpleName().contentEquals("equals"));
    }

    /**
     * Pattern matches particular comparisons to avoid common false positives
     * in the {@link Comparable#compareTo(Object)} and
     * {@link Object#equals(Object)}.
     *
     * Specifically, this method tests if:  the comparison is a == comparison,
     * it is the test of an if statement that's the first statement in the
     * method, and one of the following is true:
     *
     * 1. the method overrides {@link Comparator#compare}, the "then" branch
     *    of the if statement returns zero, and the comparison tests equality
     *    of the method's two parameters
     *
     * 2. the method overrides {@link Object#equals(Object)} and the
     *    comparison tests "this" against the method's parameter
     *
     * @param node the comparison to check
     * @return true if one of the supported heuristics is matched, false
     *         otherwise
     */
    // TODO: handle != comparisons too!
    private boolean suppressByHeuristic(final BinaryTree node) {

        // Only handle == binary trees
        if (node.getKind() != Tree.Kind.EQUAL_TO)
            return false;

        Tree left = node.getLeftOperand();
        Tree right = node.getRightOperand();

        // Only valid if we're comparing identifiers.
        if (!(left.getKind() == Tree.Kind.IDENTIFIER
                && right.getKind() == Tree.Kind.IDENTIFIER))
            return false;

        // If we're not directly in an if statement in a method (ignoring
        // parens and blocks), terminate.
        // TODO: only if it's the first statement in the block
        if (!Heuristics.matchParents(getCurrentPath(), Tree.Kind.IF, Tree.Kind.METHOD))
            return false;

        ExecutableElement enclosing =
            TreeUtils.elementFromDeclaration(visitorState.getMethodTree());
        assert enclosing != null;

        Element lhs = TreeUtils.elementFromUse((IdentifierTree)left);
        Element rhs = TreeUtils.elementFromUse((IdentifierTree)right);

        // Determine whether or not the "then" statement of the if has a single
        // "return 0" statement (for the Comparator.compare heuristic).
        if (overrides(enclosing, Comparator.class, "compare")) {
            final boolean returnsZero =
                Heuristics.applyAt(getCurrentPath(), Tree.Kind.IF, new Heuristics.Matcher() {

                    @Override
                    public Boolean visitIf(IfTree tree, Void p) {
                        return visit(tree.getThenStatement(), p);
                    }

                    @Override
                    public Boolean visitBlock(BlockTree tree, Void p) {
                        if (tree.getStatements().size() > 0)
                            return visit(tree.getStatements().get(0), p);
                        return false;
                    }

                    @Override
                    public Boolean visitReturn(ReturnTree tree, Void p) {
                        ExpressionTree expr = tree.getExpression();
                        return (expr != null &&
                                expr.getKind() == Tree.Kind.INT_LITERAL &&
                                ((LiteralTree)expr).getValue().equals(0));
                    }
                });

            if (!returnsZero)
                return false;

            assert enclosing.getParameters().size() == 2;
            Element p1 = enclosing.getParameters().get(0);
            Element p2 = enclosing.getParameters().get(1);
            return (p1.equals(lhs) && p2.equals(rhs))
                || (p2.equals(lhs) && p1.equals(rhs));

        } else if (overrides(enclosing, Object.class, "equals")) {
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
     * Determines the element corresponding to "this" inside a scope.  Returns
     * null within static methods.
     *
     * @param scope the scope with the
     * @return the element corresponding to "this" in the given scope, or null
     *      if not found
     */
    private Element getThis(Scope scope) {
        for (Element e : scope.getLocalElements())
            if (e.getSimpleName().contentEquals("this"))
                return e;
        return null;
    }

    /**
     * Determines whether or not the given element overrides the named method in
     * the named class.
     *
     * @param e an element for a method
     * @param clazz the class
     * @param method the name of a method
     * @return true if the method given by {@code e} overrides the named method
     *         in the named class; false otherwise
     */
    private boolean overrides(ExecutableElement e, Class<?> clazz, String method) {

        // Get the element named by "clazz".
        TypeElement clazzElt = elements.getTypeElement(clazz.getCanonicalName());
        assert clazzElt != null;

        // Check all of the methods in the class for name matches and overriding.
        for (ExecutableElement elt : methodsIn(clazzElt.getEnclosedElements()))
            if (elt.getSimpleName().contentEquals(method)
                    && elements.overrides(e, elt, clazzElt))
                return true;

        return false;
    }
}
