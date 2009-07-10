package checkers.nullness;

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;

import checkers.basetype.*;
import checkers.nullness.quals.LazyNonNull;
import checkers.nullness.quals.NonNull;
import checkers.nullness.quals.Nullable;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.*;
import checkers.util.*;

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
public class NullnessVisitor extends BaseTypeVisitor<Void, Void> {

    /** The {@link NonNull} annotation */
    private final AnnotationMirror NONNULL, NULLABLE;
    private final TypeMirror stringType;

    /**
     * Creates a new visitor for type-checking {@link NonNull}.
     *
     * @param checker the checker to use
     * @param root the root of the input program's AST to check
     */
    public NullnessVisitor(NullnessSubchecker checker, CompilationUnitTree root) {
        super(checker, root);
        NONNULL = this.annoFactory.fromClass(NonNull.class);
        NULLABLE = this.annoFactory.fromClass(Nullable.class);
        stringType = elements.getTypeElement("java.lang.String").asType();
    }

    /** Case 1: Check for null dereferecing */
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
        return super.visitThrow(node, p);
    }

    /** Case 5: Check for synchronizing locks */
    @Override
    public Void visitSynchronized(SynchronizedTree node, Void p) {
        
        //checkForNullability(node.getExpression(), "locking.nullable");
        // raw is suffecient
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(node.getExpression());
        if (type.hasAnnotation(NULLABLE))
            checker.report(Result.failure("locking.nullable", node), node);

        return super.visitSynchronized(node, p);
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

        // equality tests
        if ((node.getKind() == Tree.Kind.EQUAL_TO
                || node.getKind() == Tree.Kind.NOT_EQUAL_TO)
                && checker.getLintOption("nulltest", false)) {
            AnnotatedTypeMirror left = atypeFactory.getAnnotatedType(leftOp);
            AnnotatedTypeMirror right = atypeFactory.getAnnotatedType(rightOp);
            if (leftOp.getKind() == Tree.Kind.NULL_LITERAL
                    && right.hasAnnotation(NONNULL))
                checker.report(Result.warning("known.nonnull", rightOp.toString()), node);
            else if (rightOp.getKind() == Tree.Kind.NULL_LITERAL
                    && left.hasAnnotation(NONNULL))
                checker.report(Result.warning("known.nonnull", leftOp.toString()), node);
        }
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
        // ignore String concatination
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
    protected void commonAssignmentCheck(Tree varTree, ExpressionTree valueExp, String errorKey, Void p) {
        // allow LazyNonNull to be initalized to null at declaration
        if (varTree.getKind() == Tree.Kind.VARIABLE) {
            Element elem = TreeUtils.elementFromDeclaration((VariableTree)varTree);
            if (elem.getAnnotation(LazyNonNull.class) != null)
                return;
        }

        super.commonAssignmentCheck(varTree, valueExp, errorKey, p);
    }

    //////////////////////// Field Initializations //////////////////////////
    /** Case 8: field initialization */
    private Set<VariableElement> nonInitializedFields = null;

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        if (TreeUtils.isConstructor(node)
                && !TreeUtils.containsThisConstructorInvocation(node)) {
            Set<VariableElement> oldFields = nonInitializedFields;
            try {
                nonInitializedFields = getUninitializedFields(node);
                return super.visitMethod(node, p);
            } finally {
                if (!nonInitializedFields.isEmpty()) {
                    // TODO: warn against uninitialized fields
                }
                nonInitializedFields = oldFields;
            }
        }
        return super.visitMethod(node, p);
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        if (nonInitializedFields != null)
            nonInitializedFields.remove(InternalUtils.symbol(node.getVariable()));
        return super.visitAssignment(node, p);
    }

    private Set<VariableElement> getUninitializedFields(MethodTree node) {
        Set<VariableElement> fields = new HashSet<VariableElement>();

        ClassTree classTree = TreeUtils.enclosingClass(getCurrentPath());
        for (Tree member : classTree.getMembers()) {
            if (!(member instanceof VariableTree))
                continue;
            VariableTree var = (VariableTree)member;
            VariableElement varElt = TreeUtils.elementFromDeclaration(var);
            // only consider fields that are uninitialized at the declaration
            // and are qualified as nonnull
            if (var.getInitializer() == null
                    && atypeFactory.getAnnotatedType(var).hasAnnotation(NONNULL)
                    && !varElt.getModifiers().contains(Modifier.STATIC))
                fields.add(varElt);
        }
        return fields;
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
            if (method.getReceiverType().hasAnnotation(NONNULL))
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
    private void checkForNullability(ExpressionTree tree, String errMsg) {
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(tree);
        if (!type.hasAnnotation(NONNULL))
            checker.report(Result.failure(errMsg, tree), tree);
    }

    /////////////// Utilities methods //////////////////////////////
    /** @return true if binary operation could cause unboxing operation */
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
