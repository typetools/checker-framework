package checkers.nullness;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import checkers.basetype.BaseTypeVisitor;
/*>>>
import checkers.compilermsgs.quals.CompilerMessageKey;
*/
import checkers.nullness.quals.*;
import checkers.quals.Unused;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import checkers.util.AnnotationUtils;
import checkers.util.ElementUtils;
import checkers.util.InternalUtils;
import checkers.util.QualifierPolymorphism;
import checkers.util.TreeUtils;
import checkers.util.Pair;

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
    private final AnnotationMirror NONNULL, /*NULLABLE, LAZYNONNULL,*/ PRIMITIVE, RAW;
    private final TypeMirror stringType;

    /**
     * The element for java.util.Collection.size().
     */
    private final ExecutableElement collectionSize;

    /**
     * The element for java.util.Collection.toArray(T).
     */
    private final ExecutableElement collectionToArray;

    /**
     * Creates a new visitor for type-checking {@link NonNull}.
     *
     * @param checker the checker to use
     * @param root the root of the input program's AST to check
     */
    public NullnessVisitor(NullnessSubchecker checker, CompilationUnitTree root) {
        super(checker, root);
        NONNULL = checker.NONNULL;
        // NULLABLE = checker.NULLABLE;
        // LAZYNONNULL = checker.LAZYNONNULL;
        PRIMITIVE = checker.PRIMITIVE;
        RAW = ((NullnessAnnotatedTypeFactory)atypeFactory).RAW;
        stringType = elements.getTypeElement("java.lang.String").asType();

        ProcessingEnvironment env = checker.getProcessingEnvironment();
        this.collectionSize = TreeUtils.getMethod("java.util.Collection", "size", 0, env);
        this.collectionToArray = TreeUtils.getMethod("java.util.Collection", "toArray", 1, env);

        checkForAnnotatedJdk();
    }

    /** Case 1: Check for null dereferencing */
    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {
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

    @Override
    public Void visitNewArray(NewArrayTree node, Void p) {
        AnnotatedArrayType type = atypeFactory.getAnnotatedType(node);
        AnnotatedTypeMirror componentType = type.getComponentType();
        if (componentType.hasAnnotation(NONNULL) &&
                !isNewArrayAllZeroDims(node) &&
                !isNewArrayInToArray(node) &&
                checker.getLintOption("arrays:forbidnonnullcomponents", false)) {
            checker.report(Result.failure("new.array.type.invalid",
                    componentType.getAnnotations(), type.toString()), node);
        }

        return super.visitNewArray(node, p);
    }

    /** Determine whether all dimensions given in a new array expression
     * have zero as length. For example "new Object[0][0];".
     * Also true for empty dimensions, as in "new Object[] {...}".
     */
    private static boolean isNewArrayAllZeroDims(NewArrayTree node) {
        boolean isAllZeros = true;
        for (ExpressionTree dim : node.getDimensions()) {
            if (dim instanceof LiteralTree) {
                Object val = ((LiteralTree)dim).getValue();
                if (!(val instanceof Number) ||
                        !(new Integer(0).equals(((Number)val)))) {
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
        rcvsize = ((MemberSelectTree)rcvsize).getExpression();
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
        rcvtoarray = ((MemberSelectTree)rcvtoarray).getExpression();
        if (!(rcvtoarray instanceof IdentifierTree)) {
            return false;
        }

        return ((IdentifierTree)rcvsize).getName() == ((IdentifierTree)rcvtoarray).getName();
    }

    /** Case 4: Check for thrown exception nullness */
    @Override
    public Void visitThrow(ThrowTree node, Void p) {
        checkForNullability(node.getExpression(), "throwing.nullable");
        if (nonInitializedFields != null) {
            nonInitializedFields.first.clear();
            nonInitializedFields.second.clear();
        }
        return super.visitThrow(node, p);
    }

    /** Case 5: Check for synchronizing locks */
    @Override
    public Void visitSynchronized(SynchronizedTree node, Void p) {
        checkForNullability(node.getExpression(), "locking.nullable");
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
    public Void visitConditionalExpression(ConditionalExpressionTree node, Void p) {
        checkForNullability(node.getCondition(), "condition.nullable");
        return super.visitConditionalExpression(node, p);
    }

    @Override
    public Void visitIf(IfTree node, Void p) {
        checkForNullability(node.getCondition(), "condition.nullable");
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

    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree node, Void p) {
        checkForNullability(node.getCondition(), "condition.nullable");
        return super.visitDoWhileLoop(node, p);
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree node, Void p) {
        checkForNullability(node.getCondition(), "condition.nullable");
        return super.visitWhileLoop(node, p);
    }

    // Nothing needed for EnhancedForLoop, no boolean get's unboxed there.
    @Override
    public Void visitForLoop(ForLoopTree node, Void p) {
        if (node.getCondition() != null) {
            // Condition is null e.g. in "for (;;) {...}"
            checkForNullability(node.getCondition(), "condition.nullable");
        }
        return super.visitForLoop(node, p);
    }

    @Override
    public Void visitSwitch(SwitchTree node, Void p) {
        checkForNullability(node.getExpression(), "switching.nullable");
        return super.visitSwitch(node, p);
    }

    @Override
    public Void visitNewClass(NewClassTree node, Void p) {
        AnnotatedDeclaredType type = atypeFactory.getAnnotatedType(node);
        if (!type.hasAnnotation(NONNULL)) {
            // The type is not non-null => error
            checker.report(Result.failure("new.class.type.invalid", type.getAnnotations()), node);
            // Note that other consistency checks are made by isValid.
        }
        // TODO: It might be nicer to introduce a framework-level
        // isValidNewClassType or some such.
        return super.visitNewClass(node, p);
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

        if (isUnboxingOperation(types, stringType, node)) {
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
        if (!isString(types, stringType, node)) {
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
    protected void commonAssignmentCheck(Tree varTree, ExpressionTree valueExp, /*@CompilerMessageKey*/ String errorKey) {
        // allow LazyNonNull to be initialized to null at declaration
        if (varTree.getKind() == Tree.Kind.VARIABLE) {
            Element elem = TreeUtils.elementFromDeclaration((VariableTree)varTree);
            if (atypeFactory.fromElement(elem).hasAnnotation(LazyNonNull.class))
                return;
        }

        super.commonAssignmentCheck(varTree, valueExp, errorKey);
    }

    //////////////////////// Field Initializations //////////////////////////

    // Case 8: field initialization
    /**
     * non-null iff currently processing a method (or constructor) declaration AST.
     * In that case, it is a pair of:
     *  (list of non-initialized fields of NonNull type,
     *   list of non-initialized fields of Nullable or primitive type).
     * The second list is empty unless lint option "uninitialized" is set.
     */
    private Pair<Set<VariableElement>,Set<VariableElement>> nonInitializedFields = null;

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        // Check field initialization in constructors
        if (TreeUtils.isConstructor(node)
                && !TreeUtils.containsThisConstructorInvocation(node)) {
            Pair<Set<VariableElement>, Set<VariableElement>> oldFields = nonInitializedFields;
            try {
                // TODO: get access to a Types instance and use it to get receiver type
                // Or, extend ExecutableElement with such a method.
                // Note that we cannot use the receiver type from AnnotatedExecutableType,
                // because that would only have the nullness annotations; here we want to
                // see all annotations on the receiver.
                // TODO: can we clean up constructor vs. method distinction?
                List<? extends AnnotationMirror> rcvannos = null;
                if (TreeUtils.isConstructor(node)) {
                    com.sun.tools.javac.code.Symbol meth =
                            (com.sun.tools.javac.code.Symbol)TreeUtils.elementFromDeclaration(node);
                    rcvannos = meth.getRawTypeAttributes();
                } else {
                    ExecutableElement meth = TreeUtils.elementFromDeclaration(node);
                    com.sun.tools.javac.code.Type rcv = (com.sun.tools.javac.code.Type) ((ExecutableType)meth.asType()).getReceiverType();
                    if (rcv != null && rcv.isAnnotated()) {
                        rcvannos = ((com.sun.tools.javac.code.Type.AnnotatedType)rcv).typeAnnotations;
                    }
                }
                if (rcvannos == null){
                    rcvannos = Collections.<AnnotationMirror>emptyList();
                }
                nonInitializedFields
                    = getUninitializedFields(TreeUtils.enclosingClass(getCurrentPath()), rcvannos);
                return super.visitMethod(node, p);
            } finally {
                Set<VariableElement> initAfter
                    = ((NullnessAnnotatedTypeFactory)atypeFactory).initializedAfter(node);
                nonInitializedFields.first.removeAll(initAfter);
                nonInitializedFields.second.removeAll(initAfter);
                reportUninitializedFields(nonInitializedFields, node);
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
            && atypeFactory.isMostEnclosingThisDeref(node)) {

            AnnotationMirror nnAfter =
                atypeFactory.getDeclAnnotation(TreeUtils.elementFromUse(node), AssertNonNullAfter.class);
            if (nnAfter != null) {
                List<String> nnAfterValue = AnnotationUtils.getElementValueArray(nnAfter, "value", String.class, false);
                Set<VariableElement> elts =
                    ElementUtils.findFieldsInType(
                        TreeUtils.elementFromDeclaration(TreeUtils.enclosingClass(getCurrentPath())),
                        nnAfterValue);
                nonInitializedFields.first.removeAll(elts);
                nonInitializedFields.second.removeAll(elts);
            }
        }
        return super.visitMethodInvocation(node, p);
    }

    @Override
    protected void checkDefaultConstructor(ClassTree node) {
        reportUninitializedFields(getUninitializedFields(node, Collections.<AnnotationMirror>emptyList()), node);
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        if (nonInitializedFields != null) {
            Element assigned = InternalUtils.symbol(node.getVariable());
            nonInitializedFields.first.remove(assigned);
            nonInitializedFields.second.remove(assigned);
        }
        return super.visitAssignment(node, p);
    }

    // Report "fields.uninitialized" errors at the given node
    private void reportUninitializedFields(Pair<Set<VariableElement>,Set<VariableElement>> uninitFields, Tree node) {
        if (!uninitFields.first.isEmpty()) {
            checker.report(Result.failure("fields.uninitialized", uninitFields.first), node);
        }
        if (!uninitFields.second.isEmpty()) {
            checker.report(Result.warning("fields.uninitialized", uninitFields.second), node);
        }
    }

    // Returns the uninitialized instance fields.  The first element in the
    // returned pair is the NonNull fields, and the second element is the
    // primitive and Nullable fields.
    protected Pair<Set<VariableElement>,Set<VariableElement>> getUninitializedFields(ClassTree classTree, List<? extends AnnotationMirror> annos) {
        Set<VariableElement> hs1 = new HashSet<VariableElement>();
        Set<VariableElement> hs2 = new HashSet<VariableElement>();
        Pair<Set<VariableElement>,Set<VariableElement>> fields = Pair.of(hs1, hs2);

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
                // var is not @LazyNonNull -- don't check @LazyNonNull fields
                // even if checking all fields
                && !atypeFactory.fromElement(varElt).hasAnnotation(LazyNonNull.class)
                // var is not static -- need a check of initializer blocks,
                // not of constructor which is where this is used
                && !varElt.getModifiers().contains(Modifier.STATIC)
                // val is not @Unused
                && !isUnused(varElt, annos)) {
                // System.out.printf("var %s, hasEffectiveAnnotation = %s, check_all_fields=%s, %s%n", var, atypeFactory.getAnnotatedType(var).hasEffectiveAnnotation(NONNULL), check_all_fields, atypeFactory.getAnnotatedType(var));
                if ((atypeFactory.getAnnotatedType(var).hasEffectiveAnnotation(NONNULL)
                     // For now, primitives have an effecive @NonNull
                     // annotation.  (This is soon to change, at which
                     // point this clause is no longer necessary.)
                     && ! atypeFactory.getAnnotatedType(var).getKind().isPrimitive())
                    ) {
                    // var's type is @NonNull
                    fields.first.add(varElt);
                } else if (check_all_fields) {
                    // we are checking all vars
                    fields.second.add(varElt);
                }
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

        Name when = AnnotationUtils.getElementValueClassName(unused, "when", false);
        for (AnnotationMirror anno : annos) {
            Name annoName = ((TypeElement)anno.getAnnotationType().asElement()).getQualifiedName();
            if (annoName.contentEquals(when)) {
                return true;
            }
        }

        return false;
    }

    /** Special casing NonNull and Raw method calls */
    @Override
    protected void checkMethodInvocability(AnnotatedExecutableType method,
            MethodInvocationTree node) {
        if (atypeFactory.isMostEnclosingThisDeref(node)) {
            // An alternate approach would be to let the rawness checker
            // issue the warning, but the approach taken here gives, in the
            // error message, an explicit list of the fields that have been
            // initialized so far.

            Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = ((NullnessAnnotatedTypeFactory)atypeFactory).rawnessFactory.methodFromUse(node);
            AnnotatedExecutableType invokedMethod = mfuPair.first;
            // List<AnnotatedTypeMirror> typeargs = mfuPair.second;
            if (! invokedMethod.getReceiverType().hasAnnotation(RAW)) {
                if (nonInitializedFields != null) {
                    if (! nonInitializedFields.first.isEmpty()) {
                        checker.report(Result.failure("method.invocation.invalid.rawness",
                                                      TreeUtils.elementFromUse(node),
                                                      nonInitializedFields.first), node);
                    } else if (! nonInitializedFields.second.isEmpty()) {
                        checker.report(Result.warning("method.invocation.invalid.rawness",
                                                      TreeUtils.elementFromUse(node),
                                                      nonInitializedFields.second), node);
                    }
                }
            }
        }

        // Claim that methods with a @NonNull receiver are invokable so that
        // visitMemberSelect issues dereference errors instead.
        // That is, don't call the super implementation for them.
        if (!method.getReceiverType().hasEffectiveAnnotation(NONNULL)) {
            super.checkMethodInvocability(method, node);
        }
    }

    /**
     * Issues a 'dereference.of.nullable' if the type is not of a
     * {@link NonNull} type.
     *
     * @param type  type to be checked nullability
     * @param tree  the tree where the error is to reported
     */
    private void checkForNullability(ExpressionTree tree, /*@CompilerMessageKey*/ String errMsg) {
        AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(tree);
        Set<AnnotationMirror> annos = type.getEffectiveAnnotations();
        if (!(annos.contains(NONNULL) || annos.contains(PRIMITIVE))) {
            checker.report(Result.failure(errMsg, tree), tree);
        }
    }

    /**
     * Ensure that also the method post-condition annotations (like @AssertNonNullAfter) are
     * overridden consistently: at least the same fields have to be listed in the overriding method.
     */
    @Override
    protected boolean checkOverride(MethodTree overriderTree,
            AnnotatedDeclaredType enclosingType,
            AnnotatedExecutableType overridden,
            AnnotatedDeclaredType overriddenType,
            Void p) {

        if (!super.checkOverride(overriderTree, enclosingType, overridden, overriddenType, p)) {
            return false;
        }
        if (checker.shouldSkipUses(overriddenType.getElement())) {
            return true;
        }

        // Get the type of the overriding method.
        AnnotatedExecutableType overrider = atypeFactory.getAnnotatedType(overriderTree);

        boolean result = true;

        if (overrider.getTypeVariables().isEmpty() && !overridden.getTypeVariables().isEmpty()) {
            overridden = overridden.getErased();
        }
        String overriderMeth = overrider.getElement().toString();
        String overriderTyp = enclosingType.getUnderlyingType().asElement().toString();
        String overriddenMeth = overridden.getElement().toString();
        String overriddenTyp = overriddenType.getUnderlyingType().asElement().toString();

        @SuppressWarnings("unchecked")
        Class<? extends Annotation>[] postMethodAnnos = new Class[] {
            AssertNonNullAfter.class,
            AssertNonNullIfTrue.class,
            AssertNonNullIfFalse.class,
            AssertNonNullIfNonNull.class,
        };

        for (Class<? extends Annotation> methodAnno : postMethodAnnos) {
            AnnotationMirror overriddenAnno = atypeFactory.getDeclAnnotation(overridden.getElement(), methodAnno);

            // nothing to do if the overridden method has no annotation
            if (overriddenAnno == null) continue;

            AnnotationMirror overriderAnno = atypeFactory.getDeclAnnotation(overrider.getElement(), methodAnno);

            if (overriderAnno == null) {
                checker.report(Result.failure("override.post.method.annotation.invalid",
                        overriderMeth, overriderTyp, overriddenMeth, overriddenTyp,
                        overriderAnno,
                        overriddenAnno),
                        overriderTree);
                result = false;
            } else {
                List<String> overriddenValue = AnnotationUtils.getElementValueArray(overriddenAnno, "value", String.class, false);
                List<String> overriderValue = AnnotationUtils.getElementValueArray(overriderAnno, "value", String.class, false);

                for (String f : overriddenValue) {
                    // The overrider may have additional fields, but all fields from the
                    // overridden method must be mentioned again.
                    // -> You cannot weaken postconditions in an overriding method.
                    if (!overriderValue.contains(f)) {
                        checker.report(Result.failure("override.post.method.annotation.part.invalid",
                                overriderMeth, overriderTyp, overriddenMeth, overriddenTyp,
                                overriderAnno,
                                overriddenAnno,
                                f),
                                overriderTree);
                        result = false;
                        break;
                    }
                    // TODO: This purely syntactic comparison is not sufficient in general.
                    // See test case tests/nullness/OverrideANNA2.
                }
            }
        }

        @SuppressWarnings("unchecked")
        Class<? extends Annotation>[] preMethodAnnos = new Class[] {
            NonNullOnEntry.class
        };

        for (Class<? extends Annotation> methodAnno : preMethodAnnos) {
            AnnotationMirror overriderAnno = atypeFactory.getDeclAnnotation(overrider.getElement(), methodAnno);

            // nothing to do if the overrider method has no annotation
            if (overriderAnno == null) continue;

            AnnotationMirror overriddenAnno = atypeFactory.getDeclAnnotation(overridden.getElement(), methodAnno);

            if (overriddenAnno == null) {
                checker.report(Result.failure("override.pre.method.annotation.invalid",
                        overriderMeth, overriderTyp, overriddenMeth, overriddenTyp,
                        overriderAnno,
                        overriddenAnno),
                        overriderTree);
                result = false;
            } else {
                List<String> overriddenValue = AnnotationUtils.getElementValueArray(overriddenAnno, "value", String.class, false);
                List<String> overriderValue = AnnotationUtils.getElementValueArray(overriderAnno, "value", String.class, false);

                for (String f : overriderValue) {
                    // The overridden method may have additional fields, but all fields from the
                    // overrider must be mentioned again.
                    // -> You cannot strengthen preconditions in an overriding method.
                    if (!overriddenValue.contains(f)) {
                        checker.report(Result.failure("override.pre.method.annotation.part.invalid",
                                overriderMeth, overriderTyp, overriddenMeth, overriddenTyp,
                                overriderAnno,
                                overriddenAnno,
                                f),
                                overriderTree);
                        result = false;
                        break;
                    }
                    // TODO: This purely syntactic comparison is not sufficient in general.
                    // See test case tests/nullness/OverrideANNA2.
                }
            }
        }

        return result;
    }

    /////////////// Utility methods //////////////////////////////

    /** @return true if binary operation could cause an unboxing operation */
    public static final boolean isUnboxingOperation(Types types, TypeMirror stringType, BinaryTree tree) {
        if (tree.getKind() == Tree.Kind.EQUAL_TO
                || tree.getKind() == Tree.Kind.NOT_EQUAL_TO)
            return isPrimitive(tree.getLeftOperand()) != isPrimitive(tree.getRightOperand());
        else
            return !isString(types, stringType, tree);
    }

    /**
     * @return true if the type of the tree is a super of String
     * */
    public static final boolean isString(Types types, TypeMirror stringType, ExpressionTree tree) {
        TypeMirror type = InternalUtils.typeOf(tree);
        return types.isAssignable(stringType, type);
    }

    /**
     * @return true if the type of the tree is a primitive
     */
    public static final boolean isPrimitive(ExpressionTree tree) {
        return InternalUtils.typeOf(tree).getKind().isPrimitive();
    }


    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
                             AnnotatedDeclaredType useType) {
        // At most a single qualifier on a type, ignoring a possible PolyAll annotation.
        boolean found = false;
        for (AnnotationMirror anno : useType.getAnnotations()) {
            if (!QualifierPolymorphism.isPolyAll(anno)) {
                if (found) {
                    return false;
                }
                found = true;
            }
        }
        // The super implementation checks that useType is a subtype
        // of declarationType. However, declarationType by default
        // is NonNull, which would then forbid Nullable uses.
        // Therefore, don't perform this check.
        return true;
        // return super.isValidUse(declarationType, useType);
    }

    @Override
    public boolean isValidUse(AnnotatedPrimitiveType type) {
        // No explicit qualifiers on primitive types
        if (type.getAnnotations().size()>1 ||
             (!type.hasAnnotation(Primitive.class) &&
             // The element is null if the primitive type is an array component ->
             // always a reason to warn.
             (type.getElement() == null ||
                     !type.getExplicitAnnotations().isEmpty()))) {
            return false;
        }
        return super.isValidUse(type);
    }

    /* In isValidUse we cannot distinguish between the type in an array creation,
     * which has to be Nullable, and other array types. Therefore, we override
     * visitNewArray.
    @Override
    public boolean isValidUse(AnnotatedArrayType type) {
        return true;
    }
    */
}
