package org.checkerframework.checker.nullness;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CatchTree;
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
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.initialization.InitializationVisitor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.basetype.TypeValidator;
import org.checkerframework.framework.flow.CFCFGBuilder;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** The visitor for the nullness type-system. */
public class NullnessVisitor
        extends InitializationVisitor<NullnessAnnotatedTypeFactory, NullnessValue, NullnessStore> {
    // Error message keys
    // private static final @CompilerMessageKey String ASSIGNMENT_TYPE_INCOMPATIBLE =
    // "assignment.type.incompatible";
    private static final @CompilerMessageKey String UNBOXING_OF_NULLABLE = "unboxing.of.nullable";
    private static final @CompilerMessageKey String KNOWN_NONNULL = "known.nonnull";
    private static final @CompilerMessageKey String LOCKING_NULLABLE = "locking.nullable";
    private static final @CompilerMessageKey String THROWING_NULLABLE = "throwing.nullable";
    private static final @CompilerMessageKey String ACCESSING_NULLABLE = "accessing.nullable";
    private static final @CompilerMessageKey String CONDITION_NULLABLE = "condition.nullable";
    private static final @CompilerMessageKey String ITERATING_NULLABLE = "iterating.over.nullable";
    private static final @CompilerMessageKey String SWITCHING_NULLABLE = "switching.nullable";
    private static final @CompilerMessageKey String DEREFERENCE_OF_NULLABLE =
            "dereference.of.nullable";

    // Annotation and type constants
    private final AnnotationMirror NONNULL, NULLABLE, MONOTONIC_NONNULL;
    private final TypeMirror stringType;

    /** The element for java.util.Collection.size(). */
    private final ExecutableElement collectionSize;

    /** The element for java.util.Collection.toArray(T). */
    private final ExecutableElement collectionToArray;

    /** The System.clearProperty(String) method. */
    private final ExecutableElement systemClearProperty;

    /** The System.setProperties(String) method. */
    private final ExecutableElement systemSetProperties;

    /** True if checked code may clear system properties. */
    private final boolean permitClearProperty;

    /**
     * Create a new NullnessVisitor.
     *
     * @param checker the checker to which this visitor belongs
     */
    public NullnessVisitor(BaseTypeChecker checker) {
        super(checker);

        NONNULL = atypeFactory.NONNULL;
        NULLABLE = atypeFactory.NULLABLE;
        MONOTONIC_NONNULL = atypeFactory.MONOTONIC_NONNULL;
        stringType = elements.getTypeElement("java.lang.String").asType();

        ProcessingEnvironment env = checker.getProcessingEnvironment();
        this.collectionSize =
                TreeUtils.getMethod(java.util.Collection.class.getName(), "size", 0, env);
        this.collectionToArray =
                TreeUtils.getMethod(java.util.Collection.class.getName(), "toArray", env, "T[]");
        systemClearProperty =
                TreeUtils.getMethod(java.lang.System.class.getName(), "clearProperty", 1, env);
        systemSetProperties =
                TreeUtils.getMethod(java.lang.System.class.getName(), "setProperties", 1, env);

        this.permitClearProperty =
                checker.getLintOption(
                        NullnessChecker.LINT_PERMITCLEARPROPERTY,
                        NullnessChecker.LINT_DEFAULT_PERMITCLEARPROPERTY);
    }

    @Override
    public NullnessAnnotatedTypeFactory createTypeFactory() {
        return new NullnessAnnotatedTypeFactory(checker);
    }

    @Override
    public boolean isValidUse(
            AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
        // At most, a single qualifier on a type.
        boolean foundInit = false;
        boolean foundNonNull = false;
        Set<Class<? extends Annotation>> initQuals = atypeFactory.getInitializationAnnotations();
        Set<Class<? extends Annotation>> nonNullQuals = atypeFactory.getNullnessAnnotations();

        for (AnnotationMirror anno : useType.getAnnotations()) {
            if (containsSameByName(initQuals, anno)) {
                if (foundInit) {
                    return false;
                }
                foundInit = true;
            } else if (containsSameByName(nonNullQuals, anno)) {
                if (foundNonNull) {
                    return false;
                }
                foundNonNull = true;
            }
        }

        if (tree.getKind() == Tree.Kind.VARIABLE) {
            Element vs = TreeUtils.elementFromTree(tree);
            switch (vs.getKind()) {
                case EXCEPTION_PARAMETER:
                    if (useType.hasAnnotation(NULLABLE)) {
                        // Exception parameters cannot use Nullable
                        // annotations. They default to NonNull.
                        return false;
                    }
                    break;
                default:
                    // nothing to do
                    break;
            }
        }

        return super.isValidUse(declarationType, useType, tree);
    }

    private boolean containsSameByName(
            Set<Class<? extends Annotation>> quals, AnnotationMirror anno) {
        for (Class<? extends Annotation> q : quals) {
            if (atypeFactory.areSameByClass(anno, q)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void commonAssignmentCheck(
            Tree varTree,
            ExpressionTree valueExp,
            @CompilerMessageKey String errorKey,
            Object... extraArgs) {

        // allow MonotonicNonNull to be initialized to null at declaration
        if (varTree.getKind() == Tree.Kind.VARIABLE) {
            Element elem = TreeUtils.elementFromDeclaration((VariableTree) varTree);
            if (atypeFactory.fromElement(elem).hasEffectiveAnnotation(MONOTONIC_NONNULL)
                    && !checker.getLintOption(
                            NullnessChecker.LINT_NOINITFORMONOTONICNONNULL,
                            NullnessChecker.LINT_DEFAULT_NOINITFORMONOTONICNONNULL)) {
                return;
            }
        }
        super.commonAssignmentCheck(varTree, valueExp, errorKey, extraArgs);
    }

    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            ExpressionTree valueExp,
            @CompilerMessageKey String errorKey,
            Object... extraArgs) {
        // Use the valueExp as the context because data flow will have a value for that tree.
        // It might not have a value for the var tree.  This is sound because
        // if data flow has determined @PolyNull is @Nullable at the RHS, then
        // it is also @Nullable for the LHS.
        atypeFactory.replacePolyQualifier(varType, valueExp);
        super.commonAssignmentCheck(varType, valueExp, errorKey, extraArgs);
    }

    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType,
            Tree valueTree,
            @CompilerMessageKey String errorKey,
            Object... extraArgs) {
        if (TypesUtils.isPrimitive(varType.getUnderlyingType())
                && !TypesUtils.isPrimitive(valueType.getUnderlyingType())) {
            boolean succeed = checkForNullability(valueType, valueTree, UNBOXING_OF_NULLABLE);
            if (!succeed) {
                // Only issue the unboxing of nullable error.
                return;
            }
        }
        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, extraArgs);
    }

    /** Case 1: Check for null dereferencing. */
    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {
        Element e = TreeUtils.elementFromTree(node);
        if (!(TreeUtils.isSelfAccess(node)
                || node.getExpression().getKind() == Kind.PARAMETERIZED_TYPE
                // case 8. static member access
                || ElementUtils.isStatic(e))) {
            checkForNullability(node.getExpression(), DEREFERENCE_OF_NULLABLE);
        }
        return super.visitMemberSelect(node, p);
    }

    /** Case 2: Check for implicit {@code .iterator} call. */
    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void p) {
        checkForNullability(node.getExpression(), ITERATING_NULLABLE);
        return super.visitEnhancedForLoop(node, p);
    }

    /** Case 3: Check for array dereferencing. */
    @Override
    public Void visitArrayAccess(ArrayAccessTree node, Void p) {
        checkForNullability(node.getExpression(), ACCESSING_NULLABLE);
        return super.visitArrayAccess(node, p);
    }

    @Override
    public Void visitNewArray(NewArrayTree node, Void p) {
        AnnotatedArrayType type = atypeFactory.getAnnotatedType(node);
        AnnotatedTypeMirror componentType = type.getComponentType();
        if (componentType.hasEffectiveAnnotation(NONNULL)
                && !isNewArrayAllZeroDims(node)
                && !isNewArrayInToArray(node)
                && !TypesUtils.isPrimitive(componentType.getUnderlyingType())
                && (checker.getLintOption("soundArrayCreationNullness", false)
                        // temporary, for backward compatibility
                        || checker.getLintOption("forbidnonnullarraycomponents", false))) {
            checker.reportError(
                    node,
                    "new.array.type.invalid",
                    componentType.getAnnotations(),
                    type.toString());
        }

        return super.visitNewArray(node, p);
    }

    /**
     * Determine whether all dimensions given in a new array expression have zero as length. For
     * example "new Object[0][0];". Also true for empty dimensions, as in "new Object[] {...}".
     */
    private static boolean isNewArrayAllZeroDims(NewArrayTree node) {
        boolean isAllZeros = true;
        for (ExpressionTree dim : node.getDimensions()) {
            if (dim instanceof LiteralTree) {
                Object val = ((LiteralTree) dim).getValue();
                if (!(val instanceof Number) || !Integer.valueOf(0).equals(val)) {
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

    /**
     * Return true if the given node is "new X[]", in the context "toArray(new X[])".
     *
     * @param node a node to test
     * @return true if the node is a new array within acall to toArray()
     */
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

        ExpressionTree rcvtoarray = ((MethodInvocationTree) encl).getMethodSelect();
        if (!(rcvtoarray instanceof MemberSelectTree)) {
            return false;
        }
        rcvtoarray = ((MemberSelectTree) rcvtoarray).getExpression();
        if (!(rcvtoarray instanceof IdentifierTree)) {
            return false;
        }

        return ((IdentifierTree) rcvsize).getName() == ((IdentifierTree) rcvtoarray).getName();
    }

    /** Case 4: Check for thrown exception nullness. */
    @Override
    protected void checkThrownExpression(ThrowTree node) {
        checkForNullability(node.getExpression(), THROWING_NULLABLE);
    }

    /** Case 5: Check for synchronizing locks. */
    @Override
    public Void visitSynchronized(SynchronizedTree node, Void p) {
        checkForNullability(node.getExpression(), LOCKING_NULLABLE);
        return super.visitSynchronized(node, p);
    }

    @Override
    public Void visitAssert(AssertTree node, Void p) {
        // See also org.checkerframework.dataflow.cfg.CFGBuilder.CFGTranslationPhaseOne.visitAssert

        // In cases where neither assumeAssertionsAreEnabled nor assumeAssertionsAreDisabled are
        // turned on and @AssumeAssertions is not used, checkForNullability is still called since
        // the CFGBuilder will have generated one branch for which asserts are assumed to be
        // enabled.

        boolean doVisitAssert = true;

        if (checker.hasOption("assumeAssertionsAreEnabled")
                || CFCFGBuilder.assumeAssertionsActivatedForAssertTree(checker, node)) {
            doVisitAssert = true;
        } else if (checker.hasOption("assumeAssertionsAreDisabled")) {
            doVisitAssert = false;
        }

        if (doVisitAssert) {
            checkForNullability(node.getCondition(), CONDITION_NULLABLE);
            return super.visitAssert(node, p);
        }

        return null;
    }

    @Override
    public Void visitIf(IfTree node, Void p) {
        checkForNullability(node.getCondition(), CONDITION_NULLABLE);
        return super.visitIf(node, p);
    }

    /**
     * Reports an error if a comparison of a @NonNull expression with the null literal is performed.
     */
    protected void checkForRedundantTests(BinaryTree node) {

        final ExpressionTree leftOp = node.getLeftOperand();
        final ExpressionTree rightOp = node.getRightOperand();

        // respect command-line option
        if (!checker.getLintOption(
                NullnessChecker.LINT_REDUNDANTNULLCOMPARISON,
                NullnessChecker.LINT_DEFAULT_REDUNDANTNULLCOMPARISON)) {
            return;
        }

        // equality tests
        if ((node.getKind() == Tree.Kind.EQUAL_TO || node.getKind() == Tree.Kind.NOT_EQUAL_TO)) {
            AnnotatedTypeMirror left = atypeFactory.getAnnotatedType(leftOp);
            AnnotatedTypeMirror right = atypeFactory.getAnnotatedType(rightOp);
            if (leftOp.getKind() == Tree.Kind.NULL_LITERAL
                    && right.hasEffectiveAnnotation(NONNULL)) {
                checker.reportWarning(node, KNOWN_NONNULL, rightOp.toString());
            } else if (rightOp.getKind() == Tree.Kind.NULL_LITERAL
                    && left.hasEffectiveAnnotation(NONNULL)) {
                checker.reportWarning(node, KNOWN_NONNULL, leftOp.toString());
            }
        }
    }

    /** Case 6: Check for redundant nullness tests Case 7: unboxing case: primitive operations. */
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

    /** Case 7: unboxing case: primitive operation. */
    @Override
    public Void visitUnary(UnaryTree node, Void p) {
        checkForNullability(node.getExpression(), UNBOXING_OF_NULLABLE);
        return super.visitUnary(node, p);
    }

    /** Case 7: unboxing case: primitive operation. */
    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        // ignore String concatenation
        if (!isString(node)) {
            checkForNullability(node.getVariable(), UNBOXING_OF_NULLABLE);
            checkForNullability(node.getExpression(), UNBOXING_OF_NULLABLE);
        }
        return super.visitCompoundAssignment(node, p);
    }

    /** Case 7: unboxing case: casting to a primitive. */
    @Override
    public Void visitTypeCast(TypeCastTree node, Void p) {
        if (isPrimitive(node) && !isPrimitive(node.getExpression())) {
            if (!checkForNullability(node.getExpression(), UNBOXING_OF_NULLABLE)) {
                // If unboxing of nullable is issued, don't issue any other errors.
                return null;
            }
        }
        return super.visitTypeCast(node, p);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        if (!permitClearProperty) {
            ProcessingEnvironment env = checker.getProcessingEnvironment();
            if (TreeUtils.isMethodInvocation(node, systemClearProperty, env)) {
                String literal = literalFirstArgument(node);
                if (literal == null
                        || SystemGetPropertyHandler.predefinedSystemProperties.contains(literal)) {
                    checker.reportError(node, "clear.system.property");
                }
            }
            if (TreeUtils.isMethodInvocation(node, systemSetProperties, env)) {
                checker.reportError(node, "clear.system.property");
            }
        }
        return super.visitMethodInvocation(node, p);
    }

    /**
     * If the first argument of a method call is a literal, return it; otherwise return null.
     *
     * @param tree a method invocation whose first formal parameter is of String type
     * @return the first argument if it is a literal, otherwise null
     */
    /*package-private*/ static @Nullable String literalFirstArgument(MethodInvocationTree tree) {
        List<? extends ExpressionTree> args = tree.getArguments();
        assert args.size() > 0;
        ExpressionTree arg = args.get(0);
        if (arg.getKind() == Tree.Kind.STRING_LITERAL) {
            String literal = (String) ((LiteralTree) arg).getValue();
            return literal;
        }
        return null;
    }

    // ///////////// Utility methods //////////////////////////////

    /**
     * Issues the error message if the type of the tree is not of a {@link NonNull} type.
     *
     * @param tree the tree where the error is to reported
     * @param errMsg the error message (must be {@link CompilerMessageKey})
     * @return whether or not the check succeeded
     */
    private boolean checkForNullability(ExpressionTree tree, @CompilerMessageKey String errMsg) {
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(tree);
        return checkForNullability(type, tree, errMsg);
    }

    /**
     * Issues the error message if an expression with this type may be null.
     *
     * @param type annotated type
     * @param tree the tree where the error is to reported
     * @param errMsg the error message (must be {@link CompilerMessageKey})
     * @return whether or not the check succeeded
     */
    private boolean checkForNullability(
            AnnotatedTypeMirror type, Tree tree, @CompilerMessageKey String errMsg) {
        if (!type.hasEffectiveAnnotation(NONNULL)) {
            checker.reportError(tree, errMsg, tree);
            return false;
        }
        return true;
    }

    @Override
    protected void checkMethodInvocability(
            AnnotatedExecutableType method, MethodInvocationTree node) {
        if (!TreeUtils.isSelfAccess(node)
                &&
                // Static methods don't have a receiver
                method.getReceiverType() != null) {
            // TODO: should all or some constructors be excluded?
            // method.getElement().getKind() != ElementKind.CONSTRUCTOR) {
            Set<AnnotationMirror> recvAnnos = atypeFactory.getReceiverType(node).getAnnotations();
            AnnotatedTypeMirror methodReceiver = method.getReceiverType().getErased();
            AnnotatedTypeMirror treeReceiver = methodReceiver.shallowCopy(false);
            AnnotatedTypeMirror rcv = atypeFactory.getReceiverType(node);
            treeReceiver.addAnnotations(rcv.getEffectiveAnnotations());
            // If receiver is Nullable, then we don't want to issue a warning
            // about method invocability (we'd rather have only the
            // "dereference.of.nullable" message).
            if (treeReceiver.hasAnnotation(NULLABLE) || recvAnnos.contains(MONOTONIC_NONNULL)) {
                return;
            }
        }
        super.checkMethodInvocability(method, node);
    }

    /** @return true if binary operation could cause an unboxing operation */
    private final boolean isUnboxingOperation(BinaryTree tree) {
        if (tree.getKind() == Tree.Kind.EQUAL_TO || tree.getKind() == Tree.Kind.NOT_EQUAL_TO) {
            // it is valid to check equality between two reference types, even
            // if one (or both) of them is null
            return isPrimitive(tree.getLeftOperand()) != isPrimitive(tree.getRightOperand());
        } else {
            // All BinaryTree's are of type String, a primitive type or the
            // reference type equivalent of a primitive type. Furthermore,
            // Strings don't have a primitive type, and therefore only
            // BinaryTrees that aren't String can cause unboxing.
            return !isString(tree);
        }
    }

    /** @return true if the type of the tree is a super of String */
    private final boolean isString(ExpressionTree tree) {
        TypeMirror type = TreeUtils.typeOf(tree);
        return types.isAssignable(stringType, type);
    }

    /** @return true if the type of the tree is a primitive */
    private static final boolean isPrimitive(ExpressionTree tree) {
        return TreeUtils.typeOf(tree).getKind().isPrimitive();
    }

    @Override
    public Void visitSwitch(SwitchTree node, Void p) {
        checkForNullability(node.getExpression(), SWITCHING_NULLABLE);
        return super.visitSwitch(node, p);
    }

    @Override
    public Void visitForLoop(ForLoopTree node, Void p) {
        if (node.getCondition() != null) {
            // Condition is null e.g. in "for (;;) {...}"
            checkForNullability(node.getCondition(), CONDITION_NULLABLE);
        }
        return super.visitForLoop(node, p);
    }

    @Override
    public Void visitNewClass(NewClassTree node, Void p) {
        AnnotatedDeclaredType type = atypeFactory.getAnnotatedType(node);
        ExpressionTree identifier = node.getIdentifier();
        if (identifier instanceof AnnotatedTypeTree) {
            AnnotatedTypeTree t = (AnnotatedTypeTree) identifier;
            for (AnnotationMirror a : atypeFactory.getAnnotatedType(t).getAnnotations()) {
                // is this an annotation of the nullness checker?
                boolean nullnessCheckerAnno =
                        containsSameByName(atypeFactory.getNullnessAnnotations(), a);
                if (nullnessCheckerAnno && !AnnotationUtils.areSame(NONNULL, a)) {
                    // The type is not non-null => warning
                    checker.reportWarning(node, "new.class.type.invalid", type.getAnnotations());
                    // Note that other consistency checks are made by isValid.
                }
            }
            if (t.toString().contains("@PolyNull")) {
                // TODO: this is a hack, but PolyNull gets substituted
                // afterwards
                checker.reportWarning(node, "new.class.type.invalid", type.getAnnotations());
            }
        }
        // TODO: It might be nicer to introduce a framework-level
        // isValidNewClassType or some such.
        return super.visitNewClass(node, p);
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree node, Void p) {
        checkForNullability(node.getCondition(), CONDITION_NULLABLE);
        return super.visitWhileLoop(node, p);
    }

    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree node, Void p) {
        checkForNullability(node.getCondition(), CONDITION_NULLABLE);
        return super.visitDoWhileLoop(node, p);
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node, Void p) {
        checkForNullability(node.getCondition(), CONDITION_NULLABLE);
        return super.visitConditionalExpression(node, p);
    }

    @Override
    protected void checkExceptionParameter(CatchTree node) {
        // BasetypeVisitor forces annotations on exception parameters to be top,
        // but because exceptions can never be null, the Nullness Checker
        // does not require this check.
    }

    @Override
    public Void visitAnnotation(AnnotationTree node, Void p) {
        // All annotation arguments are non-null and initialized, so no need to check them.
        return null;
    }

    @Override
    protected TypeValidator createTypeValidator() {
        return new NullnessValidator(checker, this, atypeFactory);
    }

    /**
     * Check that primitive types are annotated with {@code @NonNull} even if they are the type of a
     * local variable.
     */
    private static class NullnessValidator extends BaseTypeValidator {

        /**
         * Create NullnessValidator.
         *
         * @param checker checker
         * @param visitor visitor
         * @param atypeFactory factory
         */
        public NullnessValidator(
                BaseTypeChecker checker,
                BaseTypeVisitor<?> visitor,
                AnnotatedTypeFactory atypeFactory) {
            super(checker, visitor, atypeFactory);
        }

        @Override
        protected boolean shouldCheckTopLevelDeclaredOrPrimitiveType(
                AnnotatedTypeMirror type, Tree tree) {
            if (type.getKind().isPrimitive()) {
                return true;
            }
            return super.shouldCheckTopLevelDeclaredOrPrimitiveType(type, tree);
        }
    }
}
