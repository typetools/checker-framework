package checkers.nonnull;

import java.lang.annotation.Annotation;
import java.util.Set;

import javacutils.AnnotationUtils;
import javacutils.InternalUtils;
import javacutils.TreeUtils;
import javacutils.TypesUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

import checkers.compilermsgs.quals.CompilerMessageKey;
import checkers.initialization.InitializationVisitor;
import checkers.nonnull.quals.NonNull;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import checkers.util.QualifierPolymorphism;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;

// TODO/later: documentation
// Note: this code is originally based on NullnessVisitor
public class NonNullVisitor extends
        InitializationVisitor<AbstractNonNullChecker> {

    // Error message keys
    private static final /*@CompilerMessageKey*/ String ASSIGNMENT_TYPE_INCOMPATIBLE = "assignment.type.incompatible";
    private static final /*@CompilerMessageKey*/ String UNBOXING_OF_NULLABLE = "unboxing.of.nullable";
    private static final /*@CompilerMessageKey*/ String KNOWN_NONNULL = "known.nonnull";
    private static final /*@CompilerMessageKey*/ String LOCKING_NULLABLE = "locking.nullable";
    private static final /*@CompilerMessageKey*/ String THROWING_NULLABLE = "throwing.nullable";
    private static final /*@CompilerMessageKey*/ String ACCESSING_NULLABLE = "accessing.nullable";
    private static final /*@CompilerMessageKey*/ String DEREFERENCE_OF_NULLABLE = "dereference.of.nullable";

    // Annotation and type constants
    private final AnnotationMirror NONNULL, NULLABLE, MONOTONICNONNULL;
    private final TypeMirror stringType;

    /**
     * The element for java.util.Collection.size().
     */
    private final ExecutableElement collectionSize;

    /**
     * The element for java.util.Collection.toArray(T).
     */
    private final ExecutableElement collectionToArray;

    public NonNullVisitor(AbstractNonNullChecker checker,
            CompilationUnitTree root) {
        super(checker, root);

        NONNULL = checker.NONNULL;
        NULLABLE = checker.NULLABLE;
        MONOTONICNONNULL = checker.MONOTONICNONNULL;
        stringType = elements.getTypeElement("java.lang.String").asType();

        ProcessingEnvironment env = checker.getProcessingEnvironment();
        this.collectionSize = TreeUtils.getMethod("java.util.Collection",
                "size", 0, env);
        this.collectionToArray = TreeUtils.getMethod("java.util.Collection",
                "toArray", 1, env);

        checkForAnnotatedJdk();
    }

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
        // At most a single qualifier on a type, ignoring a possible PolyAll
        // annotation.
        boolean foundInit = false;
        boolean foundNonNull = false;
        Set<Class<? extends Annotation>> initQuals = checker
                .getInitializationAnnotations();
        Set<AnnotationMirror> nonNullQuals = checker.getNonNullAnnotations();
        for (AnnotationMirror anno : useType.getAnnotations()) {
            if (QualifierPolymorphism.isPolyAll(anno)) {
                // ok.
            } else if (containsSameIgnoringValues(initQuals, anno)) {
                if (foundInit) {
                    return false;
                }
                foundInit = true;
            } else if (AnnotationUtils.containsSameIgnoringValues(nonNullQuals,
                    anno)) {
                if (foundNonNull) {
                    return false;
                }
                foundNonNull = true;
            }
        }
        // The super implementation checks that useType is a subtype
        // of declarationType. However, declarationType by default
        // is NonNull, which would then forbid Nullable uses.
        // Therefore, don't perform this check.
        return true;
    }

    @Override
    public boolean isValidUse(AnnotatedPrimitiveType type) {
        boolean nonNull = AnnotationUtils.containsSame(type.getAnnotations(),
                checker.NONNULL);
        if (!nonNull) {
            return false;
        }
        return super.isValidUse(type);
    }

    private boolean containsSameIgnoringValues(
            Set<Class<? extends Annotation>> quals, AnnotationMirror anno) {
        for (Class<? extends Annotation> q : quals) {
            if (AnnotationUtils.areSameByClass(anno, q)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void commonAssignmentCheck(Tree varTree, ExpressionTree valueExp,
            String errorKey) {

        // allow MonotonicNonNull to be initialized to null at declaration
        if (varTree.getKind() == Tree.Kind.VARIABLE) {
            Element elem = TreeUtils
                    .elementFromDeclaration((VariableTree) varTree);
            if (atypeFactory.fromElement(elem).hasAnnotation(MONOTONICNONNULL)
                    && !checker
                            .getLintOption(
                                    "strictMonotonicNonNullInit",
                                    AbstractNonNullChecker.LINT_DEFAULT_STRICTMONOTONICNONNULLINIT)) {
                return;
            }
        }

        if (TreeUtils.isFieldAccess(varTree)) {
            AnnotatedTypeMirror valueType = atypeFactory
                    .getAnnotatedType(valueExp);
            // special case writing to NonNull field for free/unc receivers
            // cast is safe, because varTree is a field
            AnnotatedTypeMirror annos = getNonNullFactory()
                    .getDeclaredAndDefaultedAnnotatedType(varTree);
            // receiverType is null for static field accesses
            AnnotatedTypeMirror receiverType = atypeFactory
                    .getReceiverType((ExpressionTree) varTree);
            if (receiverType != null
                    && (checker.isFree(receiverType) || checker
                            .isUnclassified(receiverType))) {
                if (annos.hasAnnotation(NONNULL)
                        && !valueType.hasAnnotation(NONNULL)) {
                    checker.report(Result.failure(ASSIGNMENT_TYPE_INCOMPATIBLE,
                            atypeFactory.getAnnotatedType(valueExp).toString(),
                            annos.toString()), varTree);
                }
            }
        }
        super.commonAssignmentCheck(varTree, valueExp, errorKey);
    }

    private NonNullAnnotatedTypeFactory getNonNullFactory() {
        return (NonNullAnnotatedTypeFactory) atypeFactory;
    }

    /** Case 1: Check for null dereferencing */
    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {
        if (!TreeUtils.isSelfAccess(node))
            checkForNullability(node.getExpression(), DEREFERENCE_OF_NULLABLE);

        return super.visitMemberSelect(node, p);
    }

    /** Case 2: Check for implicit {@code .iterator} call */
    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void p) {
        checkForNullability(node.getExpression(), DEREFERENCE_OF_NULLABLE);
        return super.visitEnhancedForLoop(node, p);
    }

    /** Case 3: Check for array dereferencing */
    @Override
    public Void visitArrayAccess(ArrayAccessTree node, Void p) {
        checkForNullability(node.getExpression(), ACCESSING_NULLABLE);
        return super.visitArrayAccess(node, p);
    }

    @Override
    public Void visitNewArray(NewArrayTree node, Void p) {
        AnnotatedArrayType type = atypeFactory.getAnnotatedType(node);
        AnnotatedTypeMirror componentType = type.getComponentType();
        if (componentType.hasAnnotation(NONNULL)
                && !isNewArrayAllZeroDims(node)
                && !isNewArrayInToArray(node)
                && !TypesUtils.isPrimitive(componentType.getUnderlyingType())
                && checker.getLintOption("arrays:forbidnonnullcomponents",
                        false)) {
            checker.report(
                    Result.failure("new.array.type.invalid",
                            componentType.getAnnotations(), type.toString()),
                    node);
        }

        return super.visitNewArray(node, p);
    }

    /**
     * Determine whether all dimensions given in a new array expression have
     * zero as length. For example "new Object[0][0];". Also true for empty
     * dimensions, as in "new Object[] {...}".
     */
    private static boolean isNewArrayAllZeroDims(NewArrayTree node) {
        boolean isAllZeros = true;
        for (ExpressionTree dim : node.getDimensions()) {
            if (dim instanceof LiteralTree) {
                Object val = ((LiteralTree) dim).getValue();
                if (!(val instanceof Number) || !(new Integer(0).equals(val))) {
                    isAllZeros = false;
                    break;
                }
            } else {
                isAllZeros = false;
                break;
            }
        }
        return isAllZeros;
    }

    private boolean isNewArrayInToArray(NewArrayTree node) {
        if (node.getDimensions().size() != 1) {
            return false;
        }

        ExpressionTree dim = node.getDimensions().get(0);
        ProcessingEnvironment env = checker.getProcessingEnvironment();

        if (!TreeUtils.isMethodInvocation(dim, collectionSize, env)) {
            return false;
        }

        ExpressionTree rcvsize = ((MethodInvocationTree) dim).getMethodSelect();
        if (!(rcvsize instanceof MemberSelectTree)) {
            return false;
        }
        rcvsize = ((MemberSelectTree) rcvsize).getExpression();
        if (!(rcvsize instanceof IdentifierTree)) {
            return false;
        }

        Tree encl = getCurrentPath().getParentPath().getLeaf();

        if (!TreeUtils.isMethodInvocation(encl, collectionToArray, env)) {
            return false;
        }

        ExpressionTree rcvtoarray = ((MethodInvocationTree) encl)
                .getMethodSelect();
        if (!(rcvtoarray instanceof MemberSelectTree)) {
            return false;
        }
        rcvtoarray = ((MemberSelectTree) rcvtoarray).getExpression();
        if (!(rcvtoarray instanceof IdentifierTree)) {
            return false;
        }

        return ((IdentifierTree) rcvsize).getName() == ((IdentifierTree) rcvtoarray)
                .getName();
    }

    /** Case 4: Check for thrown exception nullness */
    @Override
    public Void visitThrow(ThrowTree node, Void p) {
        checkForNullability(node.getExpression(), THROWING_NULLABLE);
        return super.visitThrow(node, p);
    }

    /** Case 5: Check for synchronizing locks */
    @Override
    public Void visitSynchronized(SynchronizedTree node, Void p) {
        checkForNullability(node.getExpression(), LOCKING_NULLABLE);
        return super.visitSynchronized(node, p);
    }

    @Override
    public Void visitAssert(AssertTree node, Void p) {
        return super.visitAssert(node, p);
    }

    @Override
    public Void visitIf(IfTree node, Void p) {
        checkForNullability(node.getCondition(), "condition.nullable");
        return super.visitIf(node, p);
    }

    /**
     * Reports an error if a comparison of a @NonNull expression with the null
     * literal is performed.
     */
    protected void checkForRedundantTests(BinaryTree node) {

        final ExpressionTree leftOp = node.getLeftOperand();
        final ExpressionTree rightOp = node.getRightOperand();

        // equality tests
        if ((node.getKind() == Tree.Kind.EQUAL_TO || node.getKind() == Tree.Kind.NOT_EQUAL_TO)) {
            AnnotatedTypeMirror left = atypeFactory.getAnnotatedType(leftOp);
            AnnotatedTypeMirror right = atypeFactory.getAnnotatedType(rightOp);
            if (leftOp.getKind() == Tree.Kind.NULL_LITERAL
                    && right.hasAnnotation(NONNULL))
                checker.report(
                        Result.warning(KNOWN_NONNULL, rightOp.toString()), node);
            else if (rightOp.getKind() == Tree.Kind.NULL_LITERAL
                    && left.hasAnnotation(NONNULL))
                checker.report(
                        Result.warning(KNOWN_NONNULL, leftOp.toString()), node);
        }
    }

    /**
     * Case 6: Check for redundant nullness tests Case 7: unboxing case:
     * primitive operations
     */
    @Override
    public Void visitBinary(BinaryTree node, Void p) {
        final ExpressionTree leftOp = node.getLeftOperand();
        final ExpressionTree rightOp = node.getRightOperand();

        if (isUnboxingOperation(node)) {
            checkForNullability(leftOp, UNBOXING_OF_NULLABLE);
            checkForNullability(rightOp, UNBOXING_OF_NULLABLE);
        }

        checkForRedundantTests(node);

        return super.visitBinary(node, p);
    }

    /** Case 7: unboxing case: primitive operation */
    @Override
    public Void visitUnary(UnaryTree node, Void p) {
        checkForNullability(node.getExpression(), UNBOXING_OF_NULLABLE);
        return super.visitUnary(node, p);
    }

    /** Case 7: unboxing case: primitive operation */
    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        // ignore String concatenation
        if (!isString(node)) {
            checkForNullability(node.getVariable(), UNBOXING_OF_NULLABLE);
            checkForNullability(node.getExpression(), UNBOXING_OF_NULLABLE);
        }
        return super.visitCompoundAssignment(node, p);
    }

    /** Case 7: unboxing case: casting to a primitive */
    @Override
    public Void visitTypeCast(TypeCastTree node, Void p) {
        if (isPrimitive(node) && !isPrimitive(node.getExpression()))
            checkForNullability(node.getExpression(), UNBOXING_OF_NULLABLE);
        return super.visitTypeCast(node, p);
    }

    // ///////////// Utility methods //////////////////////////////

    /**
     * Issues a 'dereference.of.nullable' if the type is not of a
     * {@link NonNull} type.
     *
     * @param type
     *            type to be checked nullability
     * @param errMsg
     *            the error message (must be {@link CompilerMessageKey})
     * @param tree
     *            the tree where the error is to reported
     */
    private void checkForNullability(ExpressionTree tree,
    /* @CompilerMessageKey */String errMsg) {
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(tree);
        if (!type.hasEffectiveAnnotation(NONNULL))
            checker.report(Result.failure(errMsg, tree), tree);
    }

    @Override
    protected void checkMethodInvocability(AnnotatedExecutableType method,
            MethodInvocationTree node) {
        if (!TreeUtils.isSelfAccess(node)) {
            Set<AnnotationMirror> recvAnnos = atypeFactory
                    .getReceiverType(node).getAnnotations();
            AnnotatedTypeMirror methodReceiver = method.getReceiverType()
                    .getErased();
            AnnotatedTypeMirror treeReceiver = methodReceiver.getCopy(false);
            AnnotatedTypeMirror rcv = atypeFactory.getReceiverType(node);
            treeReceiver.addAnnotations(rcv.getEffectiveAnnotations());
            // If receiver is Nullable, then we don't want to issue a warning
            // about method invocability (we'd rather have only the
            // "dereference.of.nullable" message).
            if (AnnotationUtils.containsSame(treeReceiver.getAnnotations(), NULLABLE)
                    || recvAnnos.contains(MONOTONICNONNULL)) {
                return;
            }
        }
        super.checkMethodInvocability(method, node);
    }

    /** @return true if binary operation could cause an unboxing operation */
    private final boolean isUnboxingOperation(BinaryTree tree) {
        if (tree.getKind() == Tree.Kind.EQUAL_TO
                || tree.getKind() == Tree.Kind.NOT_EQUAL_TO) {
            // it is valid to check equality between two reference types, even
            // if one (or both) of them is null
            return isPrimitive(tree.getLeftOperand()) != isPrimitive(tree
                    .getRightOperand());
        } else {
            // All BinaryTree's are of type String, a primitive type or the
            // reference type equivalent of a primitive type. Furthermore,
            // Strings don't have a primitive type, and therefore only
            // BinaryTrees that aren't String can cause unboxing.
            return !isString(tree);
        }
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
    private static final boolean isPrimitive(ExpressionTree tree) {
        return InternalUtils.typeOf(tree).getKind().isPrimitive();
    }

    @Override
    public Void visitSwitch(SwitchTree node, Void p) {
        checkForNullability(node.getExpression(), "switching.nullable");
        return super.visitSwitch(node, p);
    }

    @Override
    public Void visitForLoop(ForLoopTree node, Void p) {
        if (node.getCondition() != null) {
            // Condition is null e.g. in "for (;;) {...}"
            checkForNullability(node.getCondition(), "condition.nullable");
        }
        return super.visitForLoop(node, p);
    }

    @Override
    public Void visitNewClass(NewClassTree node, Void p) {
        AnnotatedDeclaredType type = atypeFactory.getAnnotatedType(node);
        ExpressionTree identifier = node.getIdentifier();
        if (identifier instanceof AnnotatedTypeTree) {
            AnnotatedTypeTree t = (AnnotatedTypeTree) identifier;
            for (AnnotationMirror a : atypeFactory.getAnnotatedType(t)
                    .getAnnotations()) {
                // is this an annotation of the nonnull checker?
                boolean nonnullCheckerAnno = AnnotationUtils
                        .containsSameIgnoringValues(
                                checker.getNonNullAnnotations(), a);
                if (nonnullCheckerAnno && !AnnotationUtils.areSame(NONNULL, a)) {
                    // The type is not non-null => warning
                    checker.report(
                            Result.warning("new.class.type.invalid",
                                    type.getAnnotations()), node);
                    // Note that other consistency checks are made by isValid.
                }
            }
            if (t.toString().contains("@PolyNull")) {
                // TODO: this is a hack, but PolyNull gets substituted
                // afterwards
                checker.report(
                        Result.warning("new.class.type.invalid",
                                type.getAnnotations()), node);
            }
        }
        // TODO: It might be nicer to introduce a framework-level
        // isValidNewClassType or some such.
        return super.visitNewClass(node, p);
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree node, Void p) {
        checkForNullability(node.getCondition(), "condition.nullable");
        return super.visitWhileLoop(node, p);
    }

    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree node, Void p) {
        checkForNullability(node.getCondition(), "condition.nullable");
        return super.visitDoWhileLoop(node, p);
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node,
            Void p) {
        checkForNullability(node.getCondition(), "condition.nullable");
        return super.visitConditionalExpression(node, p);
    }
}
