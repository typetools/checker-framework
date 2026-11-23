package org.checkerframework.checker.nullness;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreeScanner;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.initialization.InitializationTransfer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.InstanceOfNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NullLiteralNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.cfg.node.ThrowNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.MethodCall;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Transfer function for the non-null type system. Performs the following refinements:
 *
 * <ol>
 *   <li>After an expression is compared with the {@code null} literal, then that expression can
 *       safely be considered {@link NonNull} if the result of the comparison is false or {@link
 *       Nullable} if the result is true.
 *   <li>If an expression is dereferenced, then it can safely be assumed to be non-null in the
 *       future. If it would not be, then the dereference would have raised a {@link
 *       NullPointerException}.
 *   <li>Tracks whether {@link PolyNull} is known to be {@link NonNull} or {@link Nullable} (or not
 *       known to be either).
 * </ol>
 */
public class NullnessTransfer
    extends InitializationTransfer<NullnessValue, NullnessTransfer, NullnessStore> {

  /** The @{@link NonNull} annotation. */
  protected final AnnotationMirror NONNULL;

  /** The @{@link Nullable} annotation. */
  protected final AnnotationMirror NULLABLE;

  /** The @{@link PolyNull} annotation. */
  protected final AnnotationMirror POLYNULL;

  /**
   * Java's Map interface.
   *
   * <p>The qualifiers in this type don't matter -- it is not used as a fully-annotated
   * AnnotatedDeclaredType, but just passed to asSuper().
   */
  protected final AnnotatedDeclaredType MAP_TYPE;

  /** The type factory for the nullness analysis that was passed to the constructor. */
  protected final NullnessAnnotatedTypeFactory nullnessTypeFactory;

  /**
   * The type factory for the map key analysis, or null if the Map Key Checker should not be run.
   */
  protected final @Nullable KeyForAnnotatedTypeFactory keyForTypeFactory;

  /** True if -AassumeKeyFor was provided on the command line. */
  private final boolean assumeKeyFor;

  /**
   * True if the invocationPreservesArgumentNullness flag is turned on, meaning that after a method
   * call or constructor invocation, non-null arguments of the invocation (including the receiver)
   * are assumed to still be non-null.
   */
  private final boolean nonNullAssumptionAfterInvocation;

  /** The method {@code Object.toString()}. */
  private final ExecutableElement objectToString;

  /**
   * Create a new NullnessTransfer for the given analysis.
   *
   * @param analysis nullness analysis
   */
  public NullnessTransfer(NullnessAnalysis analysis) {
    super(analysis);
    this.nullnessTypeFactory = (NullnessAnnotatedTypeFactory) analysis.getTypeFactory();
    Elements elements = nullnessTypeFactory.getElementUtils();
    BaseTypeChecker checker = nullnessTypeFactory.getChecker();
    assumeKeyFor = checker.hasOption("assumeKeyFor");
    if (assumeKeyFor) {
      this.keyForTypeFactory = null;
    } else {
      // It is error-prone to put a type factory in a field.  It is OK here because
      // keyForTypeFactory is used only to call methods isMapGet() and isKeyForMap().
      this.keyForTypeFactory =
          nullnessTypeFactory.getTypeFactoryOfSubchecker(KeyForSubchecker.class);
    }

    NONNULL = AnnotationBuilder.fromClass(elements, NonNull.class);
    NULLABLE = AnnotationBuilder.fromClass(elements, Nullable.class);
    POLYNULL = AnnotationBuilder.fromClass(elements, PolyNull.class);

    MAP_TYPE =
        (AnnotatedDeclaredType)
            AnnotatedTypeMirror.createType(
                TypesUtils.typeFromClass(Map.class, analysis.getTypes(), elements),
                nullnessTypeFactory,
                false);

    // The assumption is on if the option is supplied without a value (as in
    // `-AinvocationPreservesArgumentNullness`) or with the value `true`.  It is off if the option
    // is absent or its value is `false`.
    nonNullAssumptionAfterInvocation =
        checker.hasOption("invocationPreservesArgumentNullness")
            && checker.getBooleanOption("invocationPreservesArgumentNullness", true);

    objectToString = TreeUtils.getMethod("java.lang.Object", "toString", 0, analysis.getEnv());
  }

  /**
   * Sets a given {@link Node} to non-null in the given {@code store}. Calls to this method
   * implement case 2.
   *
   * @param store the store to update
   * @param node the node that should be non-null
   */
  protected void makeNonNull(NullnessStore store, Node node) {
    JavaExpression internalRepr = JavaExpression.fromNode(node);
    store.insertValue(internalRepr, NONNULL);
  }

  /** Sets a given {@link Node} {@code node} to non-null in the given {@link TransferResult}. */
  protected void makeNonNull(TransferResult<NullnessValue, NullnessStore> result, Node node) {
    if (result.containsTwoStores()) {
      makeNonNull(result.getThenStore(), node);
      makeNonNull(result.getElseStore(), node);
    } else {
      makeNonNull(result.getRegularStore(), node);
    }
  }

  /** Refine the given result to @NonNull. */
  protected void refineToNonNull(TransferResult<NullnessValue, NullnessStore> result) {
    NullnessValue oldResultValue = result.getResultValue();
    NullnessValue refinedResultValue =
        analysis.createSingleAnnotationValue(NONNULL, oldResultValue.getUnderlyingType());
    NullnessValue newResultValue = refinedResultValue.mostSpecific(oldResultValue, null);
    result.setResultValue(newResultValue);
  }

  @Override
  protected @Nullable NullnessValue finishValue(
      @Nullable NullnessValue value, NullnessStore store) {
    value = super.finishValue(value, store);
    if (value != null) {
      value = withPolyNull(value, store.isPolyNullNonNull(), store.isPolyNullNull());
    }
    return value;
  }

  @Override
  protected @Nullable NullnessValue finishValue(
      @Nullable NullnessValue value, NullnessStore thenStore, NullnessStore elseStore) {
    value = super.finishValue(value, thenStore, elseStore);
    if (value != null) {
      value =
          withPolyNull(
              value,
              thenStore.isPolyNullNonNull() && elseStore.isPolyNullNonNull(),
              thenStore.isPolyNullNull() && elseStore.isPolyNullNull());
    }
    return value;
  }

  /**
   * Returns a value like {@code value}, but whose poly-null fields have the given values. Returns
   * {@code value} itself if its fields already have those values.
   *
   * <p>This method does not set the fields of {@code value} in place, because {@code value} might
   * be aliased: it might be the value that a store holds for some expression, or the value of some
   * other node.
   *
   * @param value a value
   * @param isPolyNullNonNull true if {@link PolyNull} is known to be {@link NonNull}
   * @param isPolyNullNull true if {@link PolyNull} is known to be {@link Nullable}
   * @return a value like {@code value}, but whose poly-null fields have the given values
   */
  private NullnessValue withPolyNull(
      NullnessValue value, boolean isPolyNullNonNull, boolean isPolyNullNull) {
    if (value.isPolyNullNonNull == isPolyNullNonNull && value.isPolyNullNull == isPolyNullNull) {
      return value;
    }
    NullnessValue result =
        new NullnessValue(
            analysis, new AnnotationMirrorSet(value.getAnnotations()), value.getUnderlyingType());
    result.isPolyNullNonNull = isPolyNullNonNull;
    result.isPolyNullNull = isPolyNullNull;
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Furthermore, this method refines the type to {@code NonNull} for the appropriate branch if
   * an expression is compared to the {@code null} literal (listed as case 1 in the class
   * description).
   */
  @Override
  protected TransferResult<NullnessValue, NullnessStore> strengthenAnnotationOfEqualTo(
      TransferResult<NullnessValue, NullnessStore> res,
      Node firstNode,
      Node secondNode,
      NullnessValue firstValue,
      NullnessValue secondValue,
      boolean notEqualTo) {
    res =
        super.strengthenAnnotationOfEqualTo(
            res, firstNode, secondNode, firstValue, secondValue, notEqualTo);
    if (firstNode instanceof NullLiteralNode) {
      NullnessStore thenStore = res.getThenStore();
      NullnessStore elseStore = res.getElseStore();

      List<Node> secondParts = splitAssignments(secondNode);
      for (Node secondPart : secondParts) {
        JavaExpression secondInternal = JavaExpression.fromNode(secondPart);
        if (CFAbstractStore.canInsertJavaExpression(secondInternal)) {
          thenStore = thenStore == null ? res.getThenStore() : thenStore;
          elseStore = elseStore == null ? res.getElseStore() : elseStore;
          if (notEqualTo) {
            thenStore.insertValue(secondInternal, NONNULL);
          } else {
            elseStore.insertValue(secondInternal, NONNULL);
          }
        }
      }

      if (secondValue != null
          && nullnessTypeFactory.containsSameByClass(
              secondValue.getAnnotations(), PolyNull.class)) {
        thenStore = thenStore == null ? res.getThenStore() : thenStore;
        elseStore = elseStore == null ? res.getElseStore() : elseStore;
        // TODO: methodTree is null for lambdas.  Handle that case.  See Issue3850.java.
        MethodTree methodTree = analysis.getEnclosingMethod(secondNode.getTree());
        ExecutableElement methodElem =
            methodTree == null ? null : TreeUtils.elementFromDeclaration(methodTree);
        if (notEqualTo) {
          elseStore.setPolyNullNull(true);
          if (methodElem != null && polyNullIsNonNull(methodElem, thenStore)) {
            thenStore.setPolyNullNonNull(true);
          }
        } else {
          thenStore.setPolyNullNull(true);
          if (methodElem != null && polyNullIsNonNull(methodElem, elseStore)) {
            elseStore.setPolyNullNonNull(true);
          }
        }
      }

      if (thenStore != null) {
        return new ConditionalTransferResult<>(res.getResultValue(), thenStore, elseStore);
      }
    }
    return res;
  }

  /**
   * Returns true if every formal parameter that is declared as @PolyNull is currently known to be
   * non-null.
   *
   * @param method a method
   * @param s a store
   * @return true if every formal parameter declared as @PolyNull is non-null
   */
  private boolean polyNullIsNonNull(ExecutableElement method, NullnessStore s) {
    // No need to check the receiver, which is always non-null.
    for (VariableElement var : method.getParameters()) {
      AnnotatedTypeMirror varType = atypeFactory.fromElement(var);

      if (containsPolyNullNotAtTopLevel(varType)) {
        return false;
      }

      if (varType.hasPrimaryAnnotation(POLYNULL)) {
        NullnessValue v = s.getValue(new LocalVariable(var));
        if (!AnnotationUtils.containsSameByName(v.getAnnotations(), NONNULL)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * A scanner that returns true if there is an occurrence of @PolyNull that is not at the top
   * level.
   */
  // Not static so it can access field POLYNULL.
  private class ContainsPolyNullNotAtTopLevelScanner
      extends SimpleAnnotatedTypeScanner<Boolean, Void> {
    /**
     * True if the top-level type has not yet been processed (by the first call to defaultAction).
     */
    private boolean isTopLevel = true;

    /** Create a ContainsPolyNullNotAtTopLevelScanner. */
    ContainsPolyNullNotAtTopLevelScanner() {
      super(Boolean::logicalOr, false);
    }

    @Override
    protected Boolean defaultAction(AnnotatedTypeMirror type, Void p) {
      if (isTopLevel) {
        isTopLevel = false;
        return false;
      } else {
        return type.hasPrimaryAnnotation(POLYNULL);
      }
    }
  }

  /**
   * Returns true if there is an occurrence of @PolyNull that is not at the top level.
   *
   * @param t a type
   * @return true if there is an occurrence of @PolyNull that is not at the top level
   */
  private boolean containsPolyNullNotAtTopLevel(AnnotatedTypeMirror t) {
    return new ContainsPolyNullNotAtTopLevelScanner().visit(t);
  }

  @Override
  public TransferResult<NullnessValue, NullnessStore> visitArrayAccess(
      ArrayAccessNode n, TransferInput<NullnessValue, NullnessStore> p) {
    TransferResult<NullnessValue, NullnessStore> result = super.visitArrayAccess(n, p);
    makeNonNull(result, n.getArray());
    return result;
  }

  @Override
  public TransferResult<NullnessValue, NullnessStore> visitInstanceOf(
      InstanceOfNode n, TransferInput<NullnessValue, NullnessStore> p) {
    TransferResult<NullnessValue, NullnessStore> result = super.visitInstanceOf(n, p);
    NullnessStore thenStore = result.getThenStore();
    NullnessStore elseStore = result.getElseStore();
    makeNonNull(thenStore, n.getOperand());
    return new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
  }

  @Override
  public TransferResult<NullnessValue, NullnessStore> visitFieldAccess(
      FieldAccessNode n, TransferInput<NullnessValue, NullnessStore> p) {
    TransferResult<NullnessValue, NullnessStore> result = super.visitFieldAccess(n, p);
    // The receiver of an instance field access is non-null. A static field access does not
    // ensure that the receiver is non-null.
    if (!n.isStatic()) {
      makeNonNull(result, n.getReceiver());
    }
    return result;
  }

  @Override
  public TransferResult<NullnessValue, NullnessStore> visitThrow(
      ThrowNode n, TransferInput<NullnessValue, NullnessStore> p) {
    TransferResult<NullnessValue, NullnessStore> result = super.visitThrow(n, p);
    makeNonNull(result, n.getExpression());
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * <p>When the invocationPreservesArgumentNullness flag is turned on, the receiver and arguments
   * that are passed to non-null parameters in a method call or constructor invocation are unsoundly
   * assumed to be non-null after the invocation.
   *
   * <p>When the flag is turned off, the analysis is sound: it checks that the method is
   * SideEffectFree or the receiver is unassignable. Only if either one of the two is true is the
   * receiver made non-null. The receiver is also made non-null if the method is annotated {@link
   * org.checkerframework.framework.qual.DoesNotUnrefineReceiver}. Similar logic is applied to the
   * arguments of the invocation.
   *
   * <p>The receiver of a static method invocation is never made non-null, because the invocation
   * does not dereference it.
   *
   * <p>Provided that m is of a type that implements interface java.util.Map:
   *
   * <ul>
   *   <li>Given a call m.get(k), if k is @KeyFor("m") and m's value type is @NonNull, then the
   *       result is @NonNull in the thenStore and elseStore of the transfer result.
   * </ul>
   */
  @Override
  public TransferResult<NullnessValue, NullnessStore> visitMethodInvocation(
      MethodInvocationNode n, TransferInput<NullnessValue, NullnessStore> in) {
    MethodInvocationTree tree = n.getTree();
    ExecutableElement method = n.getTarget().getMethod();

    boolean isMethodSideEffectFree = atypeFactory.isSideEffectFree(method);
    Node receiver = n.getTarget().getReceiver();
    // A static method invocation does not dereference its receiver.
    // The receiver is refined here, after the invocation completes normally.  A call tests its
    // receiver for nullness after evaluating its arguments (at the MethodAccessNode, which follows
    // the arguments), so neither the arguments nor a handler for the NullPointerException may
    // assume that the receiver is non-null.
    // The refinement applies to the receiver expression, not to the value that was evaluated
    // before the arguments, so it is skipped if evaluating the arguments might reassign the
    // receiver expression, as in `r.m(r = null)`.  It is also skipped for a synthetic invocation
    // that has no tree, whose arguments cannot be examined.
    // A method annotated @DoesNotUnrefineReceiver does not reassign its receiver expression.
    JavaExpression receiverExpr = JavaExpression.fromNode(receiver);
    boolean refineReceiver =
        !n.getTarget().isStatic()
            && (nonNullAssumptionAfterInvocation
                || isMethodSideEffectFree
                || atypeFactory.hasDoesNotUnrefineReceiver(method)
                || !receiverExpr.isAssignableByOtherCode())
            && tree != null
            && !argumentsMayReassign(tree.getArguments(), receiverExpr);

    // The exceptional successors of the invocation get the store before the invocation, which
    // super.visitMethodInvocation modifies in place.  So, copy it first.
    @Nullable NullnessStore storeBeforeInvocation =
        refineReceiver && n.getBlock() instanceof ExceptionBlock
            ? in.getRegularStore().copy()
            : null;

    TransferResult<NullnessValue, NullnessStore> result = super.visitMethodInvocation(n, in);

    if (refineReceiver) {
      // Make receiver non-null.
      makeNonNull(result, receiver);
      if (storeBeforeInvocation != null) {
        result = refineReceiverOnExceptionalSuccessors(n, result, storeBeforeInvocation);
      }
    }

    // For all formal parameters with a non-null annotation, make the actual argument non-null.
    // The point of this is to prevent cascaded errors -- the Nullness Checker will issue a
    // warning for the method invocation, but not for subsequent uses of the argument.  See test
    // case FlowNullness.java.
    // As for the receiver, the refinement is skipped if evaluating a later argument might reassign
    // the argument expression, as in `r.m(r, r = null)`.
    AnnotatedExecutableType methodType = nullnessTypeFactory.getAnnotatedType(method);
    List<AnnotatedTypeMirror> methodParams = methodType.getParameterTypes();
    List<? extends ExpressionTree> methodArgs = tree.getArguments();
    for (int i = 0; i < methodParams.size() && i < methodArgs.size(); ++i) {
      if (methodParams.get(i).hasPrimaryAnnotation(NONNULL)) {
        // Use the same expression that makeNonNull refines.
        JavaExpression argExpr = JavaExpression.fromNode(n.getArgument(i));
        if ((nonNullAssumptionAfterInvocation
                || isMethodSideEffectFree
                || !argExpr.isAssignableByOtherCode())
            && !argumentsMayReassign(methodArgs.subList(i + 1, methodArgs.size()), argExpr)) {
          makeNonNull(result, n.getArgument(i));
        }
      }
    }

    // Refine result to @NonNull if n is an invocation of Map.get, the argument is a key for
    // the map, and the map's value type is not @Nullable.
    if (nullnessTypeFactory.isMapGet(n)) {
      boolean isKeyFor;
      if (keyForTypeFactory != null) {
        String mapName = JavaExpression.fromNode(receiver).toString();
        isKeyFor = keyForTypeFactory.isKeyForMap(mapName, methodArgs.get(0));
      } else {
        isKeyFor = assumeKeyFor;
      }
      if (isKeyFor) {
        AnnotatedTypeMirror receiverType = nullnessTypeFactory.getReceiverType(n.getTree());
        if (!isValueTypeNullable(receiverType)) {
          makeNonNull(result, n);
          refineToNonNull(result);
        }
      }
    }

    return result;
  }

  /**
   * Returns a transfer result like {@code result}, in which the receiver of {@code n} is non-null
   * on each exceptional successor. Every exception that {@code n} throws comes from the invoked
   * method, which the invocation entered only because the receiver is non-null. The {@code
   * NullPointerException} for a null receiver comes from the target of {@code n} (a {@code
   * MethodAccessNode}), both for an invocation in source code and for a synthetic invocation such
   * as the {@code iterator()} call of an enhanced {@code for} loop. A handler that catches that
   * {@code NullPointerException} receives the unrefined store from the {@code MethodAccessNode}.
   *
   * @param n a method invocation whose receiver should be refined
   * @param result the transfer result for {@code n}
   * @param storeBefore the store before {@code n}; is not side-effected
   * @return a transfer result like {@code result}, with the receiver refined on the exceptional
   *     successors
   */
  private TransferResult<NullnessValue, NullnessStore> refineReceiverOnExceptionalSuccessors(
      MethodInvocationNode n,
      TransferResult<NullnessValue, NullnessStore> result,
      NullnessStore storeBefore) {
    if (!(n.getBlock() instanceof ExceptionBlock exceptionBlock)) {
      return result;
    }
    Node receiver = n.getTarget().getReceiver();
    Map<TypeMirror, NullnessStore> oldExceptionalStores = result.getExceptionalStores();
    Map<TypeMirror, NullnessStore> newExceptionalStores =
        oldExceptionalStores == null ? new HashMap<>() : new HashMap<>(oldExceptionalStores);
    boolean refined = false;
    for (TypeMirror exceptionType : exceptionBlock.getExceptionalSuccessors().keySet()) {
      NullnessStore exceptionalStore = newExceptionalStores.get(exceptionType);
      NullnessStore refinedStore =
          (exceptionalStore == null ? storeBefore : exceptionalStore).copy();
      makeNonNull(refinedStore, receiver);
      newExceptionalStores.put(exceptionType, refinedStore);
      refined = true;
    }
    return refined ? result.withExceptionalStores(newExceptionalStores) : result;
  }

  /**
   * Returns true if evaluating the given method arguments might reassign the given receiver
   * expression or a subexpression of it. The "receiver" may also be an earlier argument of the
   * invocation. This is conservative. It returns true if an argument assigns any part of the
   * receiver expression. If the receiver expression is assignable by other code or contains a
   * method call (whose value might depend on state that does not appear in the expression), it also
   * returns true if an argument assigns a non-local variable or (unless {@link
   * #nonNullAssumptionAfterInvocation} is set) invokes a method or constructor that is not
   * side-effect-free.
   *
   * <p>Matching a record pattern, as in {@code o instanceof R(int i)}, implicitly invokes the
   * record's component accessors. This method handles them as it handles explicit method
   * invocations.
   *
   * <p>This method does not examine the bodies of lambda expressions and anonymous classes in the
   * arguments. Evaluating the arguments executes such a body only through a method or constructor
   * invocation (an anonymous class's initializers run as part of its constructor), which this
   * method handles as described above.
   *
   * @param arguments the arguments of a method invocation
   * @param receiverExpr the receiver of the method invocation
   * @return true if evaluating {@code arguments} might reassign {@code receiverExpr}
   */
  private boolean argumentsMayReassign(
      List<? extends ExpressionTree> arguments, JavaExpression receiverExpr) {
    if (arguments.isEmpty()) {
      return false;
    }
    // A method call's value can depend on state that does not appear in the expression, such as a
    // static field that the method reads.  (MethodCall.isAssignableByOtherCode() examines only the
    // receiver and arguments.)  So, an assignment to a non-local variable or a call with side
    // effects might change the value of a receiver that contains a method call.
    boolean receiverMayChangeByOtherCode =
        receiverExpr.isAssignableByOtherCode() || receiverExpr.containsOfClass(MethodCall.class);
    TreeScanner<Boolean, Void> scanner =
        new TreeScanner<Boolean, Void>() {
          @Override
          public Boolean reduce(Boolean r1, Boolean r2) {
            return Boolean.TRUE.equals(r1) || Boolean.TRUE.equals(r2);
          }

          @Override
          public Boolean visitAssignment(AssignmentTree tree, Void p) {
            return assignsReceiver(tree.getVariable()) || super.visitAssignment(tree, p);
          }

          @Override
          public Boolean visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
            // `s += o`, for a String variable s, implicitly invokes o.toString().
            return assignsReceiver(tree.getVariable())
                || (tree.getKind() == Tree.Kind.PLUS_ASSIGNMENT
                    && TypesUtils.isString(TreeUtils.typeOf(tree.getVariable()))
                    && stringConversionMayHaveSideEffect(tree.getExpression()))
                || super.visitCompoundAssignment(tree, p);
          }

          // String concatenation implicitly invokes toString() on an operand that is not a String.
          @Override
          public Boolean visitBinary(BinaryTree tree, Void p) {
            return (tree.getKind() == Tree.Kind.PLUS
                    && TypesUtils.isString(TreeUtils.typeOf(tree))
                    && (stringConversionMayHaveSideEffect(tree.getLeftOperand())
                        || stringConversionMayHaveSideEffect(tree.getRightOperand())))
                || super.visitBinary(tree, p);
          }

          @Override
          public Boolean visitUnary(UnaryTree tree, Void p) {
            switch (tree.getKind()) {
              case PREFIX_INCREMENT, PREFIX_DECREMENT, POSTFIX_INCREMENT, POSTFIX_DECREMENT -> {
                if (assignsReceiver(tree.getExpression())) {
                  return true;
                }
              }
              default -> {}
            }
            return super.visitUnary(tree, p);
          }

          @Override
          public Boolean visitMethodInvocation(MethodInvocationTree tree, Void p) {
            return mayHaveSideEffect(TreeUtils.elementFromUse(tree))
                || super.visitMethodInvocation(tree, p);
          }

          @Override
          public Boolean visitNewClass(NewClassTree tree, Void p) {
            return mayHaveSideEffect(TreeUtils.elementFromUse(tree))
                || super.visitNewClass(tree, p);
          }

          // Matching a record pattern implicitly invokes the record's component accessors.
          // TreeScanner has no visitDeconstructionPattern method before Java 21, so this method
          // overrides scan instead.
          @Override
          public Boolean scan(@Nullable Tree tree, Void p) {
            if (tree != null
                && tree.getKind().name().equals("DECONSTRUCTION_PATTERN")
                && recordPatternMayHaveSideEffect(tree)) {
              return true;
            }
            return super.scan(tree, p);
          }

          // visitMethodInvocation and visitNewClass handle the invocations that execute the body
          // of a lambda expression or anonymous class.

          @Override
          public Boolean visitLambdaExpression(LambdaExpressionTree tree, Void p) {
            return false;
          }

          @Override
          public Boolean visitClass(ClassTree tree, Void p) {
            return false;
          }

          // An annotation is not evaluated at run time.  Its element-value pairs, such as
          // `value = 1`, are AssignmentTrees whose left-hand side is not a variable.
          @Override
          public Boolean visitAnnotation(AnnotationTree tree, Void p) {
            return false;
          }

          /*
           * Returns true if assigning the given variable might reassign the receiver.
           *
           * @param variable the left-hand side of an assignment
           * @return true if assigning {@code variable} might reassign the receiver
           */
          private boolean assignsReceiver(ExpressionTree variable) {
            JavaExpression assigned = JavaExpression.fromTree(variable);
            if (receiverExpr.containsSyntacticEqualJavaExpression(assigned)) {
              return true;
            }
            return receiverMayChangeByOtherCode && !(assigned instanceof LocalVariable);
          }

          /*
           * Returns true if matching the given record pattern might reassign the receiver, by
           * invoking one of the record's component accessors.  Does not examine nested patterns;
           * scan visits them.
           *
           * @param recordPattern a record pattern (a deconstruction pattern)
           * @return true if matching {@code recordPattern} might reassign the receiver
           */
          private boolean recordPatternMayHaveSideEffect(Tree recordPattern) {
            TypeElement recordElement = TypesUtils.getTypeElement(TreeUtils.typeOf(recordPattern));
            if (recordElement == null) {
              return receiverMayChangeByOtherCode && !nonNullAssumptionAfterInvocation;
            }
            for (RecordComponentElement component : recordElement.getRecordComponents()) {
              if (mayHaveSideEffect(component.getAccessor())) {
                return true;
              }
            }
            return false;
          }

          /*
           * Returns true if converting the given expression to a string, as string concatenation
           * does, might reassign the receiver by invoking a toString() method.  Does not examine
           * the subexpressions of the operand; scan visits them.
           *
           * @param operand an operand of string concatenation
           * @return true if converting {@code operand} to a string might reassign the receiver
           */
          private boolean stringConversionMayHaveSideEffect(ExpressionTree operand) {
            TypeMirror type = TreeUtils.typeOf(operand);
            if (type.getKind().isPrimitive()
                || type.getKind() == TypeKind.NULL
                || TypesUtils.isString(type)) {
              return false;
            }
            TypeElement typeElement = TypesUtils.getTypeElement(analysis.getTypes().erasure(type));
            if (typeElement == null) {
              // For example, an array type.
              return mayHaveSideEffect(objectToString);
            }
            // As for an explicit invocation, use the toString() methods of the static type.
            // getAllMethodsIn also returns overridden methods, which is conservative.
            boolean foundToString = false;
            for (ExecutableElement m :
                ElementUtils.getAllMethodsIn(typeElement, nullnessTypeFactory.getElementUtils())) {
              if (m.getSimpleName().contentEquals("toString") && m.getParameters().isEmpty()) {
                foundToString = true;
                if (mayHaveSideEffect(m)) {
                  return true;
                }
              }
            }
            // An interface need not declare or inherit a toString() method.
            return !foundToString && mayHaveSideEffect(objectToString);
          }

          /*
           * Returns true if invoking the given method or constructor might reassign the receiver.
           *
           * @param executable a method or constructor
           * @return true if invoking {@code executable} might reassign the receiver
           */
          private boolean mayHaveSideEffect(ExecutableElement executable) {
            return receiverMayChangeByOtherCode
                && !nonNullAssumptionAfterInvocation
                && !atypeFactory.isSideEffectFree(executable);
          }
        };
    for (ExpressionTree argument : arguments) {
      if (Boolean.TRUE.equals(scanner.scan(argument, null))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if {@code mapOrSubtype}'s value type (the V type argument to Map) is @Nullable.
   *
   * @param mapOrSubtype the Map type, or a subtype
   * @return true if {@code mapOrSubtype}'s value type is @Nullable
   */
  private boolean isValueTypeNullable(AnnotatedTypeMirror mapOrSubtype) {
    AnnotatedDeclaredType mapType =
        AnnotatedTypes.asSuper(nullnessTypeFactory, mapOrSubtype, MAP_TYPE);
    int numTypeArguments = mapType.getTypeArguments().size();
    if (numTypeArguments != 2) {
      throw new TypeSystemError("Wrong number %d of type arguments: %s", numTypeArguments, mapType);
    }
    AnnotatedTypeMirror valueType = mapType.getTypeArguments().get(1);
    return valueType.hasPrimaryAnnotation(NULLABLE);
  }

  @Override
  public TransferResult<NullnessValue, NullnessStore> visitReturn(
      ReturnNode n, TransferInput<NullnessValue, NullnessStore> in) {
    TransferResult<NullnessValue, NullnessStore> result = super.visitReturn(n, in);

    if (result.getResultValue() == null) {
      // Make sure there is a value for return statements, to record (at this return
      // statement) the values of isPolyNullNotNull and isPolyNullNull.
      return recreateTransferResult(createDummyValue(), result);
    } else {
      return result;
    }
  }

  /**
   * Creates a dummy abstract value (whose type is not supposed to be looked at).
   *
   * @return a dummy abstract value
   */
  private NullnessValue createDummyValue() {
    TypeMirror dummy = analysis.getEnv().getTypeUtils().getPrimitiveType(TypeKind.BOOLEAN);
    AnnotationMirrorSet annos = new AnnotationMirrorSet();
    annos.addAll(nullnessTypeFactory.getQualifierHierarchy().getBottomAnnotations());
    return new NullnessValue(analysis, annos, dummy);
  }
}
