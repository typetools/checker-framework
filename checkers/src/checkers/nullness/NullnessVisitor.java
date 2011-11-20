package checkers.nullness;

import java.util.*;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import checkers.basetype.BaseTypeVisitor;
import checkers.compilermsgs.quals.CompilerMessageKey;
import checkers.nullness.quals.*;
import checkers.quals.Unused;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.AnnotationUtils;
import checkers.util.ElementUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.*;

/**
 * A type-checking visitor for the Nullness type system.
 * This visitor reports errors ("dereference.of.nullable") or
 * warnings for violations for the following cases:
 *
 * <ol>
 * <li value="1">the receiver of a member dereference is not NonNull
 * <li value="2">the receiver of an implicit ".iterator()" dereference in an enhanced
 *    for loop is not NonNull
 * <li value="3">an accessed array is not NonNull
 * <li value="4">a thrown exception is not NonNull
 * <li value="5">the lock in a synchronized block is not NonNull
 * <li value="6">a NonNull reference is checked for nullness
 * <li value="7">a value in implicit unboxed operation is not NonNull
 * </ol>
 *
 * Self-accesses (via {@code this} or {@code super}) can always be dereferenced.
 *
 * @see NullnessSubchecker
 */
public class NullnessVisitor extends BaseTypeVisitor<NullnessSubchecker> {

    /** The {@link NonNull} annotation */
    private final AnnotationMirror NONNULL, NULLABLE, PRIMITIVE;
    private final TypeMirror stringType;

    /**
     * Creates a new visitor for type-checking {@link NonNull}.
     *
     * @param checker the checker to use
     * @param root the root of the input program's AST to check
     */
    public NullnessVisitor(NullnessSubchecker checker, CompilationUnitTree root) {
        super(checker, root);
        NONNULL = checker.NONNULL;
        NULLABLE = checker.NULLABLE;
        PRIMITIVE = checker.PRIMITIVE;
        stringType = elements.getTypeElement("java.lang.String").asType();
        checkForAnnotatedJdk();
    }

    /** Case 1: Check for null dereferencing */
    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {
        if (!TreeUtils.isSelfAccess(node))
            checkForNullability(node.getExpression(), "dereference.of.nullable");

        return super.visitMemberSelect(node, p);
    }

    /** Case 2: Check for implicit {@code .iterator} call */
    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void p) {
        checkForNullability(node.getExpression(), "dereference.of.nullable");
        return super.visitEnhancedForLoop(node, p);
    }

    /** Case 3: Check for array dereferencing */
    @Override
    public Void visitArrayAccess(ArrayAccessTree node, Void p) {
        checkForNullability(node.getExpression(), "accessing.nullable");
        return super.visitArrayAccess(node, p);
    }

    /** Case 4: Check for thrown exception nullness */
    @Override
    public Void visitThrow(ThrowTree node, Void p) {
        checkForNullability(node.getExpression(), "throwing.nullable");
        if (nonInitializedFields != null) {
            this.nonInitializedFields.clear();
        }
        return super.visitThrow(node, p);
    }

    /** Case 5: Check for synchronizing locks */
    @Override
    public Void visitSynchronized(SynchronizedTree node, Void p) {

        // checkForNullability(node.getExpression(), "locking.nullable");
        // raw is sufficient
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(node.getExpression());
        if (type.hasEffectiveAnnotation(NULLABLE))
            checker.report(Result.failure("locking.nullable", node), node);

        return super.visitSynchronized(node, p);
    }

    // Variable to skip redundant nullness tests when in assert
    private boolean isInAssert = false;

    @Override
    public Void visitAssert(AssertTree node, Void p) {
        boolean beforeAssert = isInAssert;
        try {
            isInAssert = true;
            return super.visitAssert(node, p);
        } finally {
            isInAssert = beforeAssert;
        }
    }

    @Override
    public Void visitIf(IfTree node, Void p) {
        boolean beforeAssert = isInAssert;
        try {
            isInAssert =
                TreeUtils.firstStatement(node.getThenStatement()).getKind() == Tree.Kind.THROW
                && node.getElseStatement() == null;
            return super.visitIf(node, p);
        } finally {
            isInAssert = beforeAssert;
        }
    }

    protected void checkForRedundantTests(BinaryTree node) {
        if (isInAssert) return;

        final ExpressionTree leftOp = node.getLeftOperand();
        final ExpressionTree rightOp = node.getRightOperand();

        // equality tests
        if ((node.getKind() == Tree.Kind.EQUAL_TO
                || node.getKind() == Tree.Kind.NOT_EQUAL_TO)
                && checker.getLintOption("nulltest", NullnessSubchecker.NULLTEST_DEFAULT)) {
            AnnotatedTypeMirror left = atypeFactory.getAnnotatedType(leftOp);
            AnnotatedTypeMirror right = atypeFactory.getAnnotatedType(rightOp);
            if (leftOp.getKind() == Tree.Kind.NULL_LITERAL
                    && right.hasEffectiveAnnotation(NONNULL))
                checker.report(Result.warning("known.nonnull", rightOp.toString()), node);
            else if (rightOp.getKind() == Tree.Kind.NULL_LITERAL
                    && left.hasEffectiveAnnotation(NONNULL))
                checker.report(Result.warning("known.nonnull", leftOp.toString()), node);
        }
    }

    /**
     * Case 6: Check for redundant nullness tests
     * Case 7: unboxing case: primitive operations
     */
    @Override
    public Void visitBinary(BinaryTree node, Void p) {
        final ExpressionTree leftOp = node.getLeftOperand();
        final ExpressionTree rightOp = node.getRightOperand();

        if (isUnboxingOperation(node)) {
            checkForNullability(leftOp, "unboxing.of.nullable");
            checkForNullability(rightOp, "unboxing.of.nullable");
        }

        checkForRedundantTests(node);

        return super.visitBinary(node, p);
    }

    /** Case 7: unboxing case: primitive operation */
    @Override
    public Void visitUnary(UnaryTree node, Void p) {
        checkForNullability(node.getExpression(), "unboxing.of.nullable");
        return super.visitUnary(node, p);
    }

    /** Case 7: unboxing case: primitive operation */
    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        // ignore String concatenation
        if (!isString(node)) {
            checkForNullability(node.getVariable(), "unboxing.of.nullable");
            checkForNullability(node.getExpression(), "unboxing.of.nullable");
        }
        return super.visitCompoundAssignment(node, p);
    }

    /** Case 7: unboxing case: casting to a primitive */
    @Override
    public Void visitTypeCast(TypeCastTree node, Void p) {
        if (isPrimitive(node) && !isPrimitive(node.getExpression()))
            checkForNullability(node.getExpression(), "unboxing.of.nullable");
        return super.visitTypeCast(node, p);
    }

    @Override
    protected void commonAssignmentCheck(Tree varTree, ExpressionTree valueExp, @CompilerMessageKey String errorKey) {
        // allow LazyNonNull to be initialized to null at declaration
        if (varTree.getKind() == Tree.Kind.VARIABLE) {
            Element elem = TreeUtils.elementFromDeclaration((VariableTree)varTree);
            if (atypeFactory.getDeclAnnotation(elem, LazyNonNull.class) != null)
                return;
        }

        super.commonAssignmentCheck(varTree, valueExp, errorKey);
    }

    //////////////////////// Field Initializations //////////////////////////

    // Case 8: field initialization 
    /**
     * non-null if currently processing a method (or constructor) declaration AST.
     * null if traversal is not currently within a method declaration AST.
     */
    private Set<VariableElement> nonInitializedFields = null;

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        // Check field initialization in constructors
        if (TreeUtils.isConstructor(node)
                && !TreeUtils.containsThisConstructorInvocation(node)) {
            Set<VariableElement> oldFields = nonInitializedFields;
            try {
                nonInitializedFields = getUninitializedFields(TreeUtils.enclosingClass(getCurrentPath()),
                                TreeUtils.elementFromDeclaration(node).getAnnotationMirrors());
                return super.visitMethod(node, p);
            } finally {
                nonInitializedFields.removeAll(
                                ((NullnessAnnotatedTypeFactory)atypeFactory).initializedAfter(node));
                if (!nonInitializedFields.isEmpty()) {
                    checker.report(Result.warning("fields.uninitialized", nonInitializedFields), node);
                }
                nonInitializedFields = oldFields;
            }
        }

        ExecutableElement elt = TreeUtils.elementFromDeclaration(node);
        if (atypeFactory.getDeclAnnotation(elt, AssertNonNullIfTrue.class) != null
            && elt.getReturnType().getKind() != TypeKind.BOOLEAN) {

            checker.report(Result.failure("assertiftrue.only.on.boolean"), node);
        }

        if (atypeFactory.getDeclAnnotation(elt, AssertNonNullIfFalse.class) != null
            && elt.getReturnType().getKind() != TypeKind.BOOLEAN) {

            checker.report(Result.failure("assertiffalse.only.on.boolean"), node);
        }

        return super.visitMethod(node, p);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        if (nonInitializedFields != null
            && TreeUtils.isSelfAccess(node)) {

            AnnotationMirror nnAfter =
                atypeFactory.getDeclAnnotation(TreeUtils.elementFromUse(node), AssertNonNullAfter.class);
            if (nnAfter != null) {
                List<String> nnAfterValue = AnnotationUtils.elementValueStringArray(nnAfter, "value");
                Set<VariableElement> elts =
                    ElementUtils.findFieldsInType(
                        TreeUtils.elementFromDeclaration(TreeUtils.enclosingClass(getCurrentPath())),
                        nnAfterValue);
                nonInitializedFields.removeAll(elts);
            }
        }
        return super.visitMethodInvocation(node, p);
    }

    @Override
    protected void checkDefaultConstructor(ClassTree node) {
        Set<VariableElement> fields = getUninitializedFields(node, Collections.<AnnotationMirror>emptyList());
        if (!fields.isEmpty()) {
            checker.report(Result.warning("fields.uninitialized", fields), node);
        }
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        if (nonInitializedFields != null)
            nonInitializedFields.remove(InternalUtils.symbol(node.getVariable()));
        return super.visitAssignment(node, p);
    }

    // Returns the uninitialized instance fields
    protected Set<VariableElement> getUninitializedFields(ClassTree classTree, List<? extends AnnotationMirror> annos) {
        Set<VariableElement> fields = new HashSet<VariableElement>();

        boolean check_all_fields
            = checker.getLintOption("uninitialized", NullnessSubchecker.UNINIT_DEFAULT);
        Set<Name> blockInitialized = getBlockInitializedFields(classTree);
        // System.out.printf("blockInitialized (length=%d) = %s%n", blockInitialized.size(), blockInitialized);

        for (Tree member : classTree.getMembers()) {
            if (!(member instanceof VariableTree))
                continue;
            VariableTree var = (VariableTree)member;
            VariableElement varElt = TreeUtils.elementFromDeclaration(var);
            if (
                // var has no initializer, nor does any initializer block set it
                (var.getInitializer() == null
                 && (! blockInitialized.contains(var.getName())))
                &&
                // var's type is @NonNull, or we are checking all vars
                (check_all_fields
                 || (atypeFactory.getAnnotatedType(var).hasEffectiveAnnotation(NONNULL)
                     // For now, primitives have an effecive @NonNull
                     // annotation.  (This is soon to change, at which
                     // point this clause is no longer necessary.)
                     && ! atypeFactory.getAnnotatedType(var).getKind().isPrimitive())
                 )
                // var is not @LazyNonNull -- don't check @LazyNonNull fields
                // even if checking all fields
                && atypeFactory.getDeclAnnotation(varElt, LazyNonNull.class) == null
                // var is not static -- need a check of initializer blocks,
                // not of constructor which is where this is used
                && !varElt.getModifiers().contains(Modifier.STATIC)
                // val is not @Unused
                && !isUnused(varElt, annos)) {
                // System.out.printf("var %s, hasEffectiveAnnotation = %s, check_all_fields=%s, %s%n", var, atypeFactory.getAnnotatedType(var).hasEffectiveAnnotation(NONNULL), check_all_fields, atypeFactory.getAnnotatedType(var));
                fields.add(varElt);
            }
        }
        return fields;
    }

    // List of all fields that are initialized in a block initializer.
    // This really ought to return a set of fields rather than of Names.
    // Also, perhaps handle assignments like "a = b = c = 1;".
    private Set<Name> getBlockInitializedFields(ClassTree classTree) {
        Set<Name> fields = new HashSet<Name>();

        for (Tree member : classTree.getMembers()) {
            if (member.getKind() == Tree.Kind.BLOCK) {
                BlockTree block = (BlockTree) member;
                for (StatementTree stmt : block.getStatements()) {
                    if (stmt.getKind() == Tree.Kind.EXPRESSION_STATEMENT) {
                        ExpressionTree expr = ((ExpressionStatementTree)stmt).getExpression();
                        if (expr.getKind() == Tree.Kind.ASSIGNMENT) {
                            ExpressionTree lhs = ((AssignmentTree)expr).getVariable();
                            Name field_name = null;
                            if (lhs.getKind() == Tree.Kind.IDENTIFIER) {
                                field_name = ((IdentifierTree) lhs).getName();
                            } else if (lhs.getKind() == Tree.Kind.MEMBER_SELECT) {
                                MemberSelectTree mst = (MemberSelectTree) lhs;
                                if ((mst.getExpression() instanceof IdentifierTree)
                                    && ((IdentifierTree)mst.getExpression()).getName().contentEquals("this")) {
                                    field_name = mst.getIdentifier();
                                }
                            }
                            if (field_name != null) {
                                fields.add(field_name);
                            }
                        }
                    }
                }
            }
        }

        return fields;
    }


    private boolean isUnused(VariableElement field, Collection<? extends AnnotationMirror> annos) {
        if (annos.isEmpty()) {
            return false;
        }

        AnnotationMirror unused = atypeFactory.getDeclAnnotation(field, Unused.class);
        if (unused == null)
            return false;

        String when = AnnotationUtils.elementValueClassName(unused, "when");
        for (AnnotationMirror anno : annos) {
            Name annoName = ((TypeElement)anno.getAnnotationType().asElement()).getQualifiedName();
            if (annoName.toString().equals(when)) {
                return true;
            }
        }

        return false;
    }

    /** Special casing NonNull and Raw method calls */
    @Override
    protected boolean checkMethodInvocability(AnnotatedExecutableType method,
            MethodInvocationTree node) {
        if (TreeUtils.isSelfAccess(node)) {
            // It's OK to call 'this' when all fields are initialized
            if (nonInitializedFields != null
                    && nonInitializedFields.isEmpty())
                return true;
        } else {
            // Claim that methods with a @NonNull receiver are invokable so that
            // visitMemberSelect issues dereference errors instead.
            if (method.getReceiverType().hasEffectiveAnnotation(NONNULL))
                return true;
        }
        return super.checkMethodInvocability(method, node);
    }

    /**
     * Issues a 'dereference.of.nullable' if the type is not of a
     * {@link NonNull} type.
     *
     * @param type  type to be checked nullability
     * @param tree  the tree where the error is to reported
     */
    private void checkForNullability(ExpressionTree tree, @CompilerMessageKey String errMsg) {
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(tree);
        Set<AnnotationMirror> annos = type.getEffectiveAnnotations();
        if (!(annos.contains(NONNULL) || annos.contains(PRIMITIVE))) {
            checker.report(Result.failure(errMsg, tree), tree);
        }
    }


    /////////////// Utility methods //////////////////////////////

    /** @return true if binary operation could cause an unboxing operation */
    private final boolean isUnboxingOperation(BinaryTree tree) {
        if (tree.getKind() == Tree.Kind.EQUAL_TO
                || tree.getKind() == Tree.Kind.NOT_EQUAL_TO)
            return isPrimitive(tree.getLeftOperand()) != isPrimitive(tree.getRightOperand());
        else
            return !isString(tree);
    }

    /**
     * @return true if the type of the tree is a super of String
     * */
    private final boolean isString(ExpressionTree tree) {
        TypeMirror type = InternalUtils.typeOf(tree);
        return types.isAssignable(stringType, type);
    }

    /**
     * @return true if the type of the tree is a primitive
     */
    private final boolean isPrimitive(ExpressionTree tree) {
        return InternalUtils.typeOf(tree).getKind().isPrimitive();
    }

}
