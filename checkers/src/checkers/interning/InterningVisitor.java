package checkers.interning;

import static javax.lang.model.util.ElementFilter.methodsIn;

import java.util.Comparator;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;

import checkers.basetype.BaseTypeVisitor;
import checkers.interning.quals.Interned;
import checkers.interning.quals.UsesObjectEquals;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror;
import checkers.util.Heuristics;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.*;

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
public final class InterningVisitor extends BaseTypeVisitor<InterningChecker> {

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

    // Handles the -Acheckclass command-line argument
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

        ExpressionTree leftOp = node.getLeftOperand();
        ExpressionTree rightOp = node.getRightOperand();

        // Check passes if either arg is null.
        if (leftOp.getKind() == Tree.Kind.NULL_LITERAL ||
            rightOp.getKind() == Tree.Kind.NULL_LITERAL)
            return super.visitBinary(node, p);

        AnnotatedTypeMirror left = atypeFactory.getAnnotatedType(leftOp);
        AnnotatedTypeMirror right = atypeFactory.getAnnotatedType(rightOp);

        // If either argument is a primitive, check passes due to auto-unboxing
        if (left.getKind().isPrimitive() || right.getKind().isPrimitive())
            return super.visitBinary(node, p);

        if (!(shouldCheckFor(leftOp) && shouldCheckFor(rightOp)))
            return super.visitBinary(node, p);

        // Syntactic checks for legal uses of ==
        if (suppressInsideComparison(node))
            return super.visitBinary(node, p);
        if (suppressEarlyEquals(node))
            return super.visitBinary(node, p);
        if (suppressEarlyCompareTo(node))
            return super.visitBinary(node, p);

        if (suppressClassAnnotation(left, right)) {
            return super.visitBinary(node, p);
        }

        Element leftElt = null;
        Element rightElt = null;
        if(left instanceof checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType){
        	leftElt = ((DeclaredType)left.getUnderlyingType()).asElement();
        }
        if(right instanceof checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType){
        	rightElt = ((DeclaredType)right.getUnderlyingType()).asElement();
        }

        //if neither @Interned or @UsesObjectEquals, report error
        if (!(left.hasEffectiveAnnotation(INTERNED) || (leftElt != null && leftElt.getAnnotation(UsesObjectEquals.class) != null)))
            checker.report(Result.failure("not.interned", left), leftOp);
        if (!(right.hasEffectiveAnnotation(INTERNED) || (rightElt != null && rightElt.getAnnotation(UsesObjectEquals.class) != null)))
            checker.report(Result.failure("not.interned", right), rightOp);
        return super.visitBinary(node, p);
    }

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


    /*
     * Method to implement the @UsesObjectEquals functionality.
     * If a class is marked @UsesObjectEquals, it must:
     *
     *    -not override .equals(Object)
     *    -be a subclass of Object or another class marked @UsesObjectEquals
     *
     * If a class is not marked @UsesObjectEquals, it must:
     *
     * 	  -not have a superclass marked @UsesObjectEquals
     *
     *
     * @see checkers.basetype.BaseTypeVisitor#visitClass(com.sun.source.tree.ClassTree, java.lang.Object)
     */
    @Override
    public Void visitClass(ClassTree node, Void p){
        //Looking for an @UsesObjectEquals class declaration

        TypeElement elt = TreeUtils.elementFromDeclaration(node);
        UsesObjectEquals annotation = elt.getAnnotation(UsesObjectEquals.class);

        Tree superClass = node.getExtendsClause();
        Element elmt = null;
        if (superClass!= null && (superClass instanceof IdentifierTree || superClass instanceof MemberSelectTree)){
            elmt = TreeUtils.elementFromUse((ExpressionTree)superClass);
        }


        //if it's there, check to make sure does not override equals
        //and supertype is Object or @UsesObjectEquals
        if (annotation != null){
            //check methods to ensure no .equals
            if(overridesEquals(node)){
                checker.report(Result.failure("overrides.equals"), node);
            }


            if(!(superClass == null || (elmt != null && elmt.getAnnotation(UsesObjectEquals.class) != null))){
                checker.report(Result.failure("superclass.unmarked"), node);
            }
        } else {
            //the class is not marked @UsesObjectEquals -> make sure its superclass isn't either.
            //this is impossible after design change making @UsesObjectEquals inherited?
            //check left in case of future design change back to non-inherited. 
            if(superClass != null && (elmt != null && elmt.getAnnotation(UsesObjectEquals.class) != null)){
                checker.report(Result.failure("superclass.marked"), node);
            }
        }

        return super.visitClass(node, p);
    }

    // **********************************************************************
    // Helper methods
    // **********************************************************************

    /**
     * Returns true if a class overrides Object.equals
     */
    private boolean overridesEquals(ClassTree node){
        List<? extends Tree> members = node.getMembers();
        for(Tree member : members){
            if(member instanceof MethodTree){
                MethodTree mTree = (MethodTree) member;
                ExecutableElement enclosing = TreeUtils.elementFromDeclaration(mTree);
                if(overrides(enclosing, Object.class, "equals")){
                    return true;
                }
            }
        }
        return false;
    }

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
     * Tests whether a method invocation is an invocation of
     * {@link Comparable#compareTo}.
     * <p>
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
     * Pattern matches particular comparisons to avoid common false positives
     * in the {@link Comparable#compareTo(Object)} and
     * {@link Object#equals(Object)}.
     *
     * Specifically, this method tests if:  the comparison is a == comparison,
     * it is the test of an if statement that's the first statement in the
     * method, and one of the following is true:
     *<ol>
     * <li> the method overrides {@link Comparator#compare}, the "then" branch
     *    of the if statement returns zero, and the comparison tests equality
     *    of the method's two parameters</li>
     *
     * <li> the method overrides {@link Object#equals(Object)} and the
     *    comparison tests "this" against the method's parameter </li>
     *
     * <li> the method overrides {@link Comparable#compareTo(Object)}, the
     *    "then" branch of the if statement returns zero, and the comparison
     *    tests "this" against the method's parameter </li>
     * </ol>
     *
     * @param node the comparison to check
     * @return true if one of the supported heuristics is matched, false
     *         otherwise
     */
    // TODO: handle != comparisons too!
    private boolean suppressInsideComparison(final BinaryTree node) {
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

        final Element lhs = TreeUtils.elementFromUse((IdentifierTree)left);
        final Element rhs = TreeUtils.elementFromUse((IdentifierTree)right);

        //Matcher to check for if statement that returns zero
        Heuristics.Matcher matcher = new Heuristics.Matcher() {

                @Override
                public Boolean visitIf (IfTree tree, Void p) {
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
            };

        // Determine whether or not the "then" statement of the if has a single
        // "return 0" statement (for the Comparator.compare heuristic).
        if (overrides(enclosing, Comparator.class, "compare")) {
            final boolean returnsZero =
                Heuristics.Matchers.withIn(
                        Heuristics.Matchers.ofKind(Tree.Kind.IF, matcher)).match(getCurrentPath());

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

        } else if (overrides(enclosing, Comparable.class, "compareTo")) {

            final boolean returnsZero =
                Heuristics.Matchers.withIn(
                        Heuristics.Matchers.ofKind(Tree.Kind.IF, matcher)).match(getCurrentPath());

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

    private static ExpressionTree unparenthesize(ExpressionTree t) {
        while (t.getKind() == Tree.Kind.PARENTHESIZED) {
            t = ((ParenthesizedTree) t).getExpression();
        }
        return t;
    }

    // This string comparison seems wrong.  Fix later.
    private static boolean sameTree(ExpressionTree a, ExpressionTree b) {
        return unparenthesize(a).toString().equals(unparenthesize(b).toString());
    }

    /**
     * Pattern matches to prevent false positives of the forms:
     * <pre>
     *   (a == b) || a.equals(b)
     *   (a == b) || (a != null ? a.equals(b) : false)
     *   (a == b) || (a != null && a.equals(b))
     * </pre>
     * Returns true iff the given node fits this pattern.
     *
     * @param node
     * @return true iff the node fits the pattern (a == b || a.equals(b))
     */
    private boolean suppressEarlyEquals(final BinaryTree node) {
        // Only handle == binary trees
        if (node.getKind() != Tree.Kind.EQUAL_TO)
            return false;

        // should strip parens
        final ExpressionTree left = unparenthesize(node.getLeftOperand());
        final ExpressionTree right = unparenthesize(node.getRightOperand());

        // looking for ((a == b || a.equals(b))
        Heuristics.Matcher matcher = new Heuristics.Matcher() {

                // Returns true if e is either "e1 != null" or "e2 != null"
                private boolean isNeqNull(ExpressionTree e, ExpressionTree e1, ExpressionTree e2) {
                    e = unparenthesize(e);
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
                            // check right, which should be a.equals(b) or b.equals(a) or similar
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
                public Boolean visitConditionalExpression(ConditionalExpressionTree tree, Void p) {
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

        boolean okay = Heuristics.Matchers.withIn(
                Heuristics.Matchers.ofKind(Tree.Kind.CONDITIONAL_OR, matcher)).match(getCurrentPath());
        return okay;
    }

    /**
     * Pattern matches to prevent false positives of the form
     * {@code (a == b || a.compareTo(b) == 0)}. Returns true iff
     * the given node fits this pattern.
     *
     * @param node
     * @return true iff the node fits the pattern (a == b || a.compareTo(b) == 0)
     */
    private boolean suppressEarlyCompareTo(final BinaryTree node) {
        // Only handle == binary trees
        if (node.getKind() != Tree.Kind.EQUAL_TO)
            return false;

        Tree left = node.getLeftOperand();
        Tree right = node.getRightOperand();

        // Only valid if we're comparing identifiers.
        if (!(left.getKind() == Tree.Kind.IDENTIFIER
              && right.getKind() == Tree.Kind.IDENTIFIER)) {
            return false;
        }

        final Element lhs = TreeUtils.elementFromUse((IdentifierTree)left);
        final Element rhs = TreeUtils.elementFromUse((IdentifierTree)right);

        // looking for ((a == b || a.compareTo(b) == 0)
        Heuristics.Matcher matcher = new Heuristics.Matcher() {

                @Override
                public Boolean visitBinary(BinaryTree tree, Void p) {
                    if (tree.getKind() == Tree.Kind.EQUAL_TO) {                          // a.compareTo(b) == 0
                        ExpressionTree leftTree = tree.getLeftOperand();        //looking for a.compareTo(b) or b.compareTo(a)
                        ExpressionTree rightTree = tree.getRightOperand();      //looking for 0

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
                        ExpressionTree leftTree = tree.getLeftOperand();        //looking for a==b
                        ExpressionTree rightTree = tree.getRightOperand();      //looking for a.compareTo(b) == 0 or b.compareTo(a) == 0
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
                    Element argElt = TreeUtils.elementFromUse((IdentifierTree) arg);

                    ExpressionTree exp = tree.getMethodSelect();
                    if (exp.getKind() != Tree.Kind.MEMBER_SELECT) {
                        return false;
                    }
                    MemberSelectTree member = (MemberSelectTree) exp;
                    if (member.getExpression().getKind() != Tree.Kind.IDENTIFIER) {
                        return false;
                    }

                    Element refElt = TreeUtils.elementFromUse((IdentifierTree)member.getExpression());

                    if (!((refElt.equals(lhs) && argElt.equals(rhs)) ||
                          ((refElt.equals(rhs) && argElt.equals(lhs))))) {
                        return false;
                    }
                    return true;
                }
            };

        boolean okay = Heuristics.Matchers.withIn(
                Heuristics.Matchers.ofKind(Tree.Kind.CONDITIONAL_OR, matcher)).match(getCurrentPath());
        return okay;
    }

    /**
     * Suppose that we have:
     * <pre>
     *   class A { ... }
     *   @Interned class B { ... }
     * </pre>
     * then <code>a == b</code>, where a has type A and b has type B, is
     * fine because it will be true only if a's run-time type is B (or
     * lower), in which case a is actually interned.
     * <p>
     * More generally, if either of the classes is of a type whose
     * declaration is annotated with @Interned, then the comparison is OK.
     */
    private boolean suppressClassAnnotation(AnnotatedTypeMirror left, AnnotatedTypeMirror right) {
        // It would be better to just test their greatest lower bound.
        // That could permit some comparisons that this forbids.
        return classIsAnnotated(left) || classIsAnnotated(right);
    }

    /** Returns true if the type's declaration has an @Interned annotation. */
    private boolean classIsAnnotated(AnnotatedTypeMirror type) {

        TypeMirror tm = type.getUnderlyingType();
        if (tm instanceof TypeVariable) {
            tm = ((TypeVariable) tm).getUpperBound();
        }
        if (tm instanceof WildcardType) {
            tm = ((WildcardType) tm).getExtendsBound();
        }
        if (tm == null) {
            // Maybe a type variable or wildcard had no upper bound
            return false;
        }
        if (tm instanceof ArrayType) {
            return false;
        }
        if (! (tm instanceof DeclaredType)) {
            System.out.printf("InterningVisitor.classIsAnnotated: tm = %s (%s)%n", tm, tm.getClass());
        }
        Element classElt = ((DeclaredType) tm).asElement();
        if (classElt == null) {
            System.out.printf("InterningVisitor.classIsAnnotated: classElt = null for tm = %s (%s)%n", tm, tm.getClass());
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
