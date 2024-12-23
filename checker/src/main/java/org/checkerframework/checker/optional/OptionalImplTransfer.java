package org.checkerframework.checker.optional;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.BinaryOperator;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.nonempty.qual.NonEmpty;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.optional.qual.Present;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGLambda;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;

/** The transfer function for the Optional Checker. */
public class OptionalImplTransfer extends CFTransfer {

  /** The {@link OptionalImplAnnotatedTypeFactory} instance for this transfer class. */
  private final OptionalImplAnnotatedTypeFactory optionalTypeFactory;

  /** True if "-AassumePure" or "-AassumeDeterministic" was passed. */
  boolean assumeDeterministic;

  /** The @{@link Present} annotation. */
  private final AnnotationMirror PRESENT;

  /** The @{@link NonEmpty} annotation. */
  private final AnnotationMirror NON_EMPTY;

  /** The element for java.util.Optional.ifPresent(). */
  private final ExecutableElement optionalIfPresent;

  /** The element for java.util.Optional.ifPresentOrElse(), or null. */
  private final @Nullable ExecutableElement optionalIfPresentOrElse;

  /** The element for java.util.stream.Stream.max(), or null. */
  private final @Nullable ExecutableElement streamMax;

  /** The element for java.util.stream.Stream.min(), or null. */
  private final @Nullable ExecutableElement streamMin;

  /** The element for java.util.stream.Stream.reduce(BinaryOperator&lt;T&gt;), or null. */
  private final @Nullable ExecutableElement streamReduceNoIdentity;

  /** The element for java.util.stream.Stream.findFirst(), or null. */
  private final @Nullable ExecutableElement streamFindFirst;

  /** The element for java.util.stream.Stream.findAny(), or null. */
  private final @Nullable ExecutableElement streamFindAny;

  /** Stream methods such that if the input is @NonEmpty, the result is @Present. */
  private final List<ExecutableElement> nonEmptyToPresentStreamMethods;

  /**
   * Create an OptionalImplTransfer.
   *
   * @param analysis the OptionalImpl Checker instance
   */
  public OptionalImplTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    super(analysis);
    optionalTypeFactory = (OptionalImplAnnotatedTypeFactory) analysis.getTypeFactory();
    BaseTypeChecker checker = optionalTypeFactory.getChecker();
    assumeDeterministic =
        checker.hasOption("assumePure") || checker.hasOption("assumeDeterministic");

    Elements elements = optionalTypeFactory.getElementUtils();
    PRESENT = AnnotationBuilder.fromClass(elements, Present.class);
    NON_EMPTY = AnnotationBuilder.fromClass(elements, NonEmpty.class);
    ProcessingEnvironment env = optionalTypeFactory.getProcessingEnv();
    optionalIfPresent = TreeUtils.getMethod("java.util.Optional", "ifPresent", 1, env);
    optionalIfPresentOrElse =
        TreeUtils.getMethodOrNull("java.util.Optional", "ifPresentOrElse", 2, env);
    streamMax = TreeUtils.getMethodOrNull("java.util.stream.Stream", "max", 1, env);
    streamMin = TreeUtils.getMethodOrNull("java.util.stream.Stream", "min", 1, env);
    streamReduceNoIdentity = TreeUtils.getMethodOrNull("java.util.stream.Stream", "reduce", 1, env);
    streamFindFirst = TreeUtils.getMethodOrNull("java.util.stream.Stream", "findFirst", 0, env);
    streamFindAny = TreeUtils.getMethodOrNull("java.util.stream.Stream", "findAny", 0, env);
    nonEmptyToPresentStreamMethods =
        Arrays.asList(streamMax, streamMin, streamReduceNoIdentity, streamFindFirst, streamFindAny);
  }

  @Override
  public CFStore initialStore(UnderlyingAST underlyingAST, List<LocalVariableNode> parameters) {

    CFStore result = super.initialStore(underlyingAST, parameters);

    if (underlyingAST.getKind() == UnderlyingAST.Kind.LAMBDA) {
      // Check whether this lambda is an argument to `Optional.ifPresent()` or
      // `Optional.ifPresentOrElse()`.  If so, then within the lambda, the receiver of the
      // `ifPresent*` method is @Present.
      CFGLambda cfgLambda = (CFGLambda) underlyingAST;
      LambdaExpressionTree lambdaTree = cfgLambda.getLambdaTree();
      List<? extends VariableTree> lambdaParams = lambdaTree.getParameters();
      if (lambdaParams.size() == 1) {
        TreePath lambdaPath = optionalTypeFactory.getPath(lambdaTree);
        Tree lambdaParent = lambdaPath.getParentPath().getLeaf();
        if (lambdaParent.getKind() == Tree.Kind.METHOD_INVOCATION) {
          MethodInvocationTree invok = (MethodInvocationTree) lambdaParent;
          ExecutableElement methodElt = TreeUtils.elementFromUse(invok);
          if (methodElt.equals(optionalIfPresent) || methodElt.equals(optionalIfPresentOrElse)) {
            // `underlyingAST` is an invocation of `Optional.ifPresent()` or
            // `Optional.ifPresentOrElse()`.  In the lambda, the receiver is @Present.
            ExpressionTree methodSelectTree = TreeUtils.withoutParens(invok.getMethodSelect());
            ExpressionTree receiverTree = ((MemberSelectTree) methodSelectTree).getExpression();
            JavaExpression receiverJe = JavaExpression.fromTree(receiverTree);
            result.insertValue(receiverJe, PRESENT);
          }
        }
      }
    }

    // TODO: Similar logic to the above can be applied in the Nullness Checker.
    // Some methods take a function as an argument, guaranteeing that, if the function is
    // called:
    //  * the value passed to the function is non-null
    //  * some other argument to the method is non-null
    // Examples:
    //  * Jodd's `StringUtil.ifNotNull()`
    //  * `Opt.ifPresent()`
    //  * `Opt.map()`

    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);
    if (n.getTree() == null) {
      return result;
    }
    return refineNonEmptyToPresentStreamResult(n, result);
  }

  /**
   * Refines the result of a call to a method in {@link #nonEmptyToPresentStreamMethods}. Examples
   * are {@link java.util.stream.Stream#max(Comparator)} and {@link
   * java.util.stream.Stream#reduce(BinaryOperator)}.
   *
   * <p>When one of those methods is invoked on an empty stream, the result is an empty/absent
   * Optional. When one of those methods is invoked on a non-empty stream, the result is a present
   * Optional.
   *
   * @param methodInvok the method invocation node
   * @param result the transfer result to side effect
   * @return the refined transfer result
   */
  private TransferResult<CFValue, CFStore> refineNonEmptyToPresentStreamResult(
      MethodInvocationNode methodInvok, TransferResult<CFValue, CFStore> result) {
    if (NodeUtils.isMethodInvocation(
        methodInvok, nonEmptyToPresentStreamMethods, optionalTypeFactory.getProcessingEnv())) {
      if (isReceiverParameterNonEmpty(methodInvok)) {
        // The receiver is @Non-Empty, therefore the result is @Present.
        JavaExpression methodInvokJE = JavaExpression.fromNode(methodInvok);
        if (assumeDeterministic) {
          insertIntoStoresPermitNonDeterministic(result, methodInvokJE, PRESENT);
        } else {
          insertIntoStores(result, methodInvokJE, PRESENT);
        }
        CFValue value = analysis.createSingleAnnotationValue(PRESENT, methodInvok.getType());

        return new ConditionalTransferResult<>(
            finishValue(value, result.getThenStore(), result.getElseStore()),
            result.getThenStore(),
            result.getElseStore(),
            result.getExceptionalStores());
      }
    }
    return result;
  }

  /**
   * Returns true if the receiver parameter of the method being invoked is explicitly annotated
   * with @{@link NonEmpty}.
   *
   * @param methodInvok a method invocation node
   * @return true if the receiver parameter of the method being invoked is explicitly annotated
   *     with @{@link NonEmpty}
   */
  private boolean isReceiverParameterNonEmpty(MethodInvocationNode methodInvok) {
    ExpressionTree receiverTree = TreeUtils.getReceiverTree(methodInvok.getTree());
    JavaExpression receiver;
    if (receiverTree instanceof MethodInvocationTree) {
      // TODO(https://github.com/typetools/checker-framework/issues/6848): this logic needs further
      // refinement to eliminate a source of false positives in the Optional Checker.
      // Also see the discussion in:
      // https://github.com/typetools/checker-framework/pull/6685#discussion_r1788632663 for
      // additional context.
      receiver = JavaExpression.getLeftmostReceiverOfMethodInvocation(receiverTree);
    } else {
      receiver = JavaExpression.fromTree(receiverTree);
    }
    VariableTree receiverDeclaration = getReceiverDeclaration(methodInvok, receiver);
    if (receiverDeclaration == null) {
      return false;
    }
    List<? extends AnnotationTree> receiverAnnotationTrees =
        receiverDeclaration.getModifiers().getAnnotations();
    List<AnnotationMirror> receiverAnnotationMirrors =
        TreeUtils.annotationsFromTypeAnnotationTrees(receiverAnnotationTrees);
    return AnnotationUtils.containsSame(receiverAnnotationMirrors, NON_EMPTY);
  }

  /**
   * Returns the declaration of the receiver of the given method invocation node.
   *
   * <p>Finds the declaration of {@code receiver} in the method that immediately encloses {@code
   * methodInvok}. If unsuccessful, look up the declaration in the fields of the class(s) that
   * enclose {@code methodInvok}.
   *
   * @param methodInvok a method invocation node
   * @param receiver the receiver argument in the method invocation node
   * @return the declaration of the receiver if found, else null
   */
  private @Nullable VariableTree getReceiverDeclaration(
      MethodInvocationNode methodInvok, @Nullable JavaExpression receiver) {
    if (receiver == null) {
      return null;
    }
    // Look in the method, first.
    MethodTree methodTree = TreePathUtil.enclosingMethod(methodInvok.getTreePath());
    VariableTree declarationInMethod =
        JavaExpression.getReceiverDeclarationInMethod(methodTree, receiver);
    if (declarationInMethod != null) {
      return declarationInMethod;
    }
    // If the declaration can't be found in the method, look in the class.
    return getReceiverDeclaredInClassField(methodInvok, receiver);
  }

  /**
   * Returns the declaration of the receiver of the given method invocation node.
   *
   * <p>Finds the declaration of {@code receiver} in the classes that enclose {@code methodInvok}.
   *
   * @param methodInvok a method invocation node
   * @param receiver the receiver argument in the method invocation node
   * @return the declaration of the receiver if found, else null
   */
  private @Nullable VariableTree getReceiverDeclaredInClassField(
      MethodInvocationNode methodInvok, JavaExpression receiver) {
    TreePath classTreePath = methodInvok.getTreePath();
    ClassTree classTree = TreePathUtil.enclosingClass(classTreePath);
    while (classTree != null) {
      VariableTree receiverAsField =
          JavaExpression.getReceiverDeclarationAsField(classTree, receiver);
      if (receiverAsField != null) {
        return receiverAsField;
      }
      // The receiver was not declared in the class immediately enclosing the method invocation, but
      // it might be declared
      // as a field in an outer class
      if (classTreePath.getParentPath() != null) {
        classTreePath = classTreePath.getParentPath();
        classTree = TreePathUtil.enclosingClass(classTreePath);
      } else {
        // There is no outer class in which to check for the declaration of the receiver
        return null;
      }
    }
    return null;
  }
}
