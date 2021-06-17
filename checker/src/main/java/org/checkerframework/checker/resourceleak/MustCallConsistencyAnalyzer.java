package org.checkerframework.checker.resourceleak;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.mustcall.CreatesMustCallForElementSupplier;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlockImpl;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NullLiteralNode;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.cfg.node.TernaryExpressionNode;
import org.checkerframework.dataflow.cfg.node.ThisNode;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.StringsPlume;

/**
 * An analyzer that checks consistency of {@code @MustCall} and {@code @CalledMethods} types,
 * thereby detecting resource leaks. For any expression <em>e</em>, the analyzer ensures that when
 * <em>e</em> goes out of scope, there exists a resource alias <em>r</em> of <em>e</em> (which might
 * be <em>e</em> itself) such that MustCall(r) is contained in CalledMethods(r). For any <em>e</em>
 * for which this property does not hold, the analyzer reports a {@code
 * "required.method.not.called"} error, indicating a possible resource leak.
 *
 * <p>Mechanically, the analysis tracks dataflow facts about the obligations of sets of
 * resource-aliases that refer to the same resource, and checks their must-call and called-methods
 * types when the last reference to those sets goes out of scope. That is, this class implements a
 * lightweight alias analysis that tracks must-alias sets for resources.
 *
 * <p>Throughout this class, variables named "obligation" or "obligations" are dataflow facts of
 * type {@code ImmutableSet<LocalVarWithTree>}, each representing a set of resource aliases for some
 * value with a non-empty {@code @MustCall} obligation. These obligations can be resolved either via
 * ownership transfer (e.g. by being assigned into an owning field) or via their must-call
 * obligations being contained in their called-methods type when the last reference in a set goes
 * out of scope.
 *
 * <p>The algorithm here adds, modifies, or removes obligations from those it is tracking when
 * certain code patterns are encountered. Here are non-exhaustive examples:
 *
 * <ul>
 *   <li>A new obligation is added to the tracked set when a constructor or a method with an owning
 *       return is invoked.
 *   <li>An obligation is modified when an expression with a tracked obligation is assigned to a
 *       local variable or a resource-alias method or constructor is called (the new local or the
 *       result of the resource-alias method/constructor is added to the existing resource alias
 *       set).
 *   <li>An obligation can be removed when a member of a resource-alias set is assigned to an owning
 *       field or passed to a method in a parameter location that is annotated as {@code @Owning}.
 * </ul>
 *
 * <p>Throughout, this class uses the temporary-variable facilities provided by the Must Call and
 * Resource Leak type factories to permit expressions to have their types refined in their
 * respective checkers' stores. These temporary variables can be members of resource-alias sets.
 * Without temporary variables, the checker wouldn't be able to verify code such as {@code new
 * Socket(host, port).close()}, which would cause false positives. Temporaries are created for
 * {@code new} expressions, method calls (for the return value), and ternary expressions. Other
 * types of expressions may also be supported in the future.
 */
/* package-private */
class MustCallConsistencyAnalyzer {

  /** {@code @MustCall} errors reported thus far, to avoid duplicate reports. */
  private final Set<LocalVarWithTree> reportedMustCallErrors = new HashSet<>();

  /**
   * The type factory for the Resource Leak Checker, which is used to get called methods types and
   * to access the Must Call Checker.
   */
  private final ResourceLeakAnnotatedTypeFactory typeFactory;

  /** The Resource Leak Checker, used to issue errors. */
  private final ResourceLeakChecker checker;

  /** The analysis from the Resource Leak Checker, used to get input stores based on CFG blocks. */
  private final CFAnalysis analysis;

  /**
   * Creates a consistency analyzer. Typically, the type factory's postAnalyze method would
   * instantiate a new consistency analyzer using this constructor and then call {@link
   * #analyze(ControlFlowGraph)}.
   *
   * @param typeFactory the type factory
   * @param analysis the analysis from the type factory. Usually this would have protected access,
   *     so this constructor cannot get it directly.
   */
  /* package-private */
  MustCallConsistencyAnalyzer(ResourceLeakAnnotatedTypeFactory typeFactory, CFAnalysis analysis) {
    this.typeFactory = typeFactory;
    this.checker = (ResourceLeakChecker) typeFactory.getChecker();
    this.analysis = analysis;
  }

  /**
   * The main function of the consistency dataflow analysis. The analysis tracks dataflow facts
   * ("obligations") of type {@code ImmutableSet<LocalVarWithTree>}, each representing a set of
   * resource aliases for some value with a non-empty {@code @MustCall} obligation. (It is not
   * necessary to track expressions with empty {@code @MustCall} obligations, because they are
   * trivially fulfilled.)
   *
   * @param cfg the control flow graph of the method to check
   */
  // TODO: This analysis is currently implemented directly using a worklist; in the future, it
  // should be rewritten to use the dataflow framework of the Checker Framework.
  /* package-private */
  void analyze(ControlFlowGraph cfg) {
    // The `visited` set contains everything that has been added to the worklist, even if it has not
    // yet been removed and analyzed.
    Set<BlockWithObligations> visited = new LinkedHashSet<>();
    Deque<BlockWithObligations> worklist = new ArrayDeque<>();

    // Add any owning parameters to the initial set of variables to track.
    BlockWithObligations entry =
        new BlockWithObligations(cfg.getEntryBlock(), computeOwningParameters(cfg));
    worklist.add(entry);
    visited.add(entry);

    while (!worklist.isEmpty()) {
      BlockWithObligations current = worklist.remove();
      List<Node> nodes = current.block.getNodes();
      // A *mutable* set that eventually holds the set of obligations to be propagated to successor
      // blocks. The set is initialized to the current obligations and updated by the methods
      // invoked in the for loop below.
      Set<ImmutableSet<LocalVarWithTree>> obligations = new LinkedHashSet<>(current.obligations);

      for (Node node : nodes) {
        if (node instanceof AssignmentNode) {
          handleAssignment((AssignmentNode) node, obligations);
        } else if (node instanceof ReturnNode) {
          handleReturn((ReturnNode) node, cfg, obligations);
        } else if (node instanceof MethodInvocationNode || node instanceof ObjectCreationNode) {
          handleInvocation(obligations, node);
        }
        // All other types of nodes are ignored. This is safe, because other kinds of
        // nodes cannot create or modify the resource-alias sets that the algorithm is tracking.
      }

      handleSuccessorBlocks(visited, worklist, obligations, current.block);
    }
  }

  /**
   * Update a set of obligations to account for a method or constructor invocation.
   *
   * @param obligations the obligations to update
   * @param node the method or constructor invocation
   */
  private void handleInvocation(Set<ImmutableSet<LocalVarWithTree>> obligations, Node node) {
    doOwnershipTransferToParameters(obligations, node);
    if (node instanceof MethodInvocationNode
        && typeFactory.canCreateObligations()
        && typeFactory.hasCreatesMustCallFor((MethodInvocationNode) node)) {
      checkCreatesMustCallForInvocation(obligations, (MethodInvocationNode) node);
      // Count calls to @CreatesMustCallFor methods as creating new resources. Doing so could
      // result in slightly over-counting, because @CreatesMustCallFor doesn't guarantee that a
      // new resource is created: it just means that a new resource might have been created.
      incrementNumMustCall(node);
    }

    if (!shouldTrackInvocationResult(obligations, node)) {
      return;
    }

    if (typeFactory.hasDeclaredMustCall(node.getTree())) {
      // The incrementNumMustCall call above increments the count for the target of the
      // @CreatesMustCallFor annotation.  By contrast, this call increments the count for the return
      // value of the method (which can't be the target of the annotation, because our syntax
      // doesn't support that).
      incrementNumMustCall(node);
    }
    trackInvocationResult(obligations, node);
  }

  /**
   * If node is an invocation of a this or super constructor that has a MustCallAlias return type
   * and a MustCallAlias parameter, check if any variable in the current set of obligations is being
   * passed to the other constructor. If so, remove it from the obligations.
   *
   * @param obligations current obligations
   * @param node a super or this constructor invocation
   */
  private void handleThisOrSuperConstructorMustCallAlias(
      Set<ImmutableSet<LocalVarWithTree>> obligations, Node node) {
    Node mcaParam = getMustCallAliasParamVar(node);
    // If the MustCallAlias param is also in the set of obligations, then remove it -- its
    // obligation has been fulfilled by being passed on to the MustCallAlias constructor (because we
    // must be in a constructor body if we've encountered a this/super constructor call).
    if (mcaParam instanceof LocalVariableNode) {
      removeObligationContainingVar(obligations, (LocalVariableNode) mcaParam);
    }
  }

  /**
   * Checks that an invocation of a CreatesMustCallFor method is valid. Such an invocation is valid
   * if one of the following conditions is true: 1) the target is an owning pointer, 2) the target
   * already has a tracked obligation, or 3) the method in which the invocation occurs also has
   * an @CreatesMustCallFor annotation, with the same target.
   *
   * <p>If none of the above are true, this method issues a reset.not.owning error.
   *
   * <p>For soundness, this method also guarantees that if the target has a tracked obligation, any
   * tracked aliases will be removed (lest the analysis conclude that it is already closed because
   * one of these aliases was closed before the method was invoked). Aliases created after the
   * CreatesMustCallFor method is invoked are still permitted.
   *
   * @param obligations the currently-tracked obligations. This value is side-effected if it
   *     contains the target of the reset method.
   * @param node a method invocation node, invoking a method with a CreatesMustCallFor annotation
   */
  private void checkCreatesMustCallForInvocation(
      Set<ImmutableSet<LocalVarWithTree>> obligations, MethodInvocationNode node) {

    TreePath currentPath = typeFactory.getPath(node.getTree());
    List<JavaExpression> targetExprs =
        CreatesMustCallForElementSupplier.getCreatesMustCallForExpressions(
            node, typeFactory, typeFactory);
    Set<JavaExpression> missing = new HashSet<>();
    for (JavaExpression target : targetExprs) {
      boolean validInvocation = false;
      if (target instanceof FieldAccess) {
        Element elt = ((FieldAccess) target).getField();
        if (!checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)
            && typeFactory.getDeclAnnotation(elt, Owning.class) != null) {
          // The target is an Owning field.  This satisfies case 1.
          validInvocation = true;
        }
      } else if (target instanceof LocalVariable) {
        Element elt = ((LocalVariable) target).getElement();
        if (!checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)
            && typeFactory.getDeclAnnotation(elt, Owning.class) != null) {
          // The target is an Owning param.  This satisfies case 1.
          validInvocation = true;
        } else {
          ImmutableSet<LocalVarWithTree> toRemoveSet = null;
          ImmutableSet<LocalVarWithTree> toAddSet = null;
          for (ImmutableSet<LocalVarWithTree> resourceAliasSet : obligations) {
            for (LocalVarWithTree alias : resourceAliasSet) {
              if (target.equals(alias.localVar)) {
                // This satisfies case 2 above. Remove all its aliases, then return below.
                if (toRemoveSet != null) {
                  throw new BugInCF(
                      "tried to remove multiple sets containing a reset target at once");
                }
                toRemoveSet = resourceAliasSet;
                toAddSet = ImmutableSet.of(alias);
              }
            }
          }

          if (toRemoveSet != null) {
            obligations.remove(toRemoveSet);
            obligations.add(toAddSet);
            // This satisfies case 2.
            validInvocation = true;
          }
        }
      }

      if (!validInvocation) {
        // TODO: Getting this every time is inefficient if a method has many @CreatesMustCallFor
        // annotations, but that should be a rare path.
        MethodTree enclosingMethodTree = TreePathUtil.enclosingMethod(currentPath);
        if (enclosingMethodTree != null) {
          ExecutableElement enclosingMethodElt =
              TreeUtils.elementFromDeclaration(enclosingMethodTree);
          MustCallAnnotatedTypeFactory mcAtf =
              typeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
          List<String> enclosingCmcfValues =
              ResourceLeakVisitor.getCreatesMustCallForValues(
                  enclosingMethodElt, mcAtf, typeFactory);
          if (!enclosingCmcfValues.isEmpty()) {
            for (String enclosingCmcfValue : enclosingCmcfValues) {
              JavaExpression enclosingTarget;
              try {
                enclosingTarget =
                    StringToJavaExpression.atMethodBody(
                        enclosingCmcfValue, enclosingMethodTree, checker);
              } catch (JavaExpressionParseException e) {
                // Do not issue an error here, because it would be a duplicate.
                // The error will be issued by the Transfer class of the checker,
                // via the CreatesMustCallForElementSupplier interface.
                enclosingTarget = null;
              }

              if (representSame(target, enclosingTarget)) {
                // This satisifies case 3.
                validInvocation = true;
              }
            }
          }
        }
      }
      if (!validInvocation) {
        missing.add(target);
      }
    }

    if (missing.isEmpty()) {
      // all targets were valid
      return;
    }

    String missingStrs = StringsPlume.join(", ", missing);
    checker.reportError(node.getTree(), "reset.not.owning", missingStrs);
  }

  /**
   * Checks whether the two JavaExpressions are the same. This is identical to calling equals() on
   * one of them, with two exceptions: the second expression can be null, and "this" references are
   * compared using their underlying type. (ThisReference#equals always returns true, which is
   * probably a bug and isn't accurate in the case of nested classes.)
   *
   * @param target a JavaExpression
   * @param enclosingTarget another, possibly null, JavaExpression
   * @return true iff they represent the same program element
   */
  private boolean representSame(JavaExpression target, @Nullable JavaExpression enclosingTarget) {
    if (enclosingTarget == null) {
      return false;
    }
    if (enclosingTarget instanceof ThisReference && target instanceof ThisReference) {
      return enclosingTarget.getType().toString().equals(target.getType().toString());
    } else {
      return enclosingTarget.equals(target);
    }
  }

  /**
   * Given a node representing a method or constructor call, checks that if the call has a non-empty
   * {@code @MustCall} type, then its result is pseudo-assigned to some location that can take
   * ownership of the result. Searches for the set of same resources in {@code obligations} and adds
   * the new LocalVarWithTree to it if one exists. Otherwise creates a new set.
   *
   * @param obligations the currently-tracked obligations. This is always side-effected: an
   *     obligation is either modified to include a new resource alias (the result of the invocation
   *     being tracked) or a new resource alias set (i.e. obligation) is created and added.
   * @param node the node whose result is to be tracked
   */
  private void trackInvocationResult(Set<ImmutableSet<LocalVarWithTree>> obligations, Node node) {
    Tree tree = node.getTree();
    // We need to track the result of the call iff there is a temporary variable for the call node
    // (because we only create temporaries for expressions that could actually have must-call
    // values).
    LocalVariableNode tmpVar = typeFactory.getTempVarForNode(node);
    if (tmpVar == null) {
      return;
    }
    LocalVarWithTree tmpVarWithTree = new LocalVarWithTree(new LocalVariable(tmpVar), tree);

    // `mustCallAlias` is the MCA parameter if any exists, otherwise null.
    Node mustCallAlias = getMustCallAliasParamVar(node);

    // If mustCallAlias is null and call returns @This, set mustCallAlias to the receiver.
    if (mustCallAlias == null
        && node instanceof MethodInvocationNode
        && typeFactory.returnsThis((MethodInvocationTree) tree)) {
      mustCallAlias =
          removeCastsAndGetTmpVarIfPresent(((MethodInvocationNode) node).getTarget().getReceiver());
    }

    if (mustCallAlias instanceof FieldAccessNode) {
      // We do not track the call result if the MustCallAlias parameter is a field (handling of
      // @Owning fields is a completely separate check, and we never need to track an alias of
      // non-@Owning fields).
    } else if (mustCallAlias instanceof LocalVariableNode) {
      ImmutableSet<LocalVarWithTree> resourceAliasSetContainingMustCallAlias =
          getResourceAliasSetForVar(obligations, (LocalVariableNode) mustCallAlias);
      // If mustCallAlias is a local variable already being tracked, add tmpVarWithTree
      // to the set containing mustCallAlias.
      if (resourceAliasSetContainingMustCallAlias != null) {
        ImmutableSet<LocalVarWithTree> newResourceAliasSet =
            FluentIterable.from(resourceAliasSetContainingMustCallAlias)
                .append(tmpVarWithTree)
                .toSet();
        obligations.remove(resourceAliasSetContainingMustCallAlias);
        obligations.add(newResourceAliasSet);
      }
    } else {
      // If mustCallAlias is neither a field nor a local already in the set of obligations,
      // add it to a new set.
      obligations.add(ImmutableSet.of(tmpVarWithTree));
    }
  }

  /**
   * Checks for cases where we do not need to track the result of a method call. An invocation
   * result does not need to be checked if the method invocation is a call to a constructor `this()`
   * or `super()`, if the method's return type is annotated with MustCallAlias and the argument in
   * the corresponding position is an owning field, or if the method's return type is non-owning,
   * which can either be because the method has no return type or because it is annotated with
   * {@link NotOwning}.
   *
   * <p>This method can also side-effect obligations, if node is a super or this constructor call
   * with MustCallAlias annotations, by removing that obligation.
   *
   * @param obligations the current set of obligations
   * @param node the invocation node to check
   * @return true iff the result of node should be tracked in obligations
   */
  private boolean shouldTrackInvocationResult(
      Set<ImmutableSet<LocalVarWithTree>> obligations, Node node) {
    Tree callTree = node.getTree();
    if (callTree.getKind() == Tree.Kind.METHOD_INVOCATION) {
      MethodInvocationTree methodInvokeTree = (MethodInvocationTree) callTree;

      if (TreeUtils.isSuperConstructorCall(methodInvokeTree)
          || TreeUtils.isThisConstructorCall(methodInvokeTree)) {
        handleThisOrSuperConstructorMustCallAlias(obligations, node);
        return false;
      }
      return !returnTypeIsMustCallAliasWithUntrackable((MethodInvocationNode) node)
          && !hasNotOwningReturnType((MethodInvocationNode) node);
    }
    return true;
  }

  /**
   * Returns true if this node represents a method invocation of a must-call-alias method, where the
   * other must call alias is untrackable: an owning field or a pointer that is guaranteed to be
   * non-owning, such as "`this`" or a non-owning field. Owning fields are handled by the rest of
   * the checker, not by this algorithm, so they are "untrackable". Non-owning fields and this nodes
   * are guaranteed to be non-owning, and therefore do not need to be tracked, either.
   *
   * @param node a method invocation node
   * @return true if this is the invocation of a method whose return type is MCA with an owning
   *     field or a definitely non-owning pointer
   */
  private boolean returnTypeIsMustCallAliasWithUntrackable(MethodInvocationNode node) {
    Node mcaParam = getMustCallAliasParamVar(node);
    return mcaParam instanceof FieldAccessNode || mcaParam instanceof ThisNode;
  }

  /**
   * Checks if {@code node} is either directly enclosed by a {@link TypeCastNode} or is the then or
   * else operand of a {@link TernaryExpressionNode}, by looking at the successor block in the CFG.
   * This method is only used within {@link #handleSuccessorBlocks(Set, Deque, Set, Block)} to
   * ensure obligations are propagated to cast / ternary nodes properly. It relies on the assumption
   * that a {@link TypeCastNode} or {@link TernaryExpressionNode} will only appear in a CFG as the
   * first node in a block.
   *
   * @param node the CFG node
   * @return {@code true} if {@code node} is in a {@link SingleSuccessorBlock} {@code b}, the first
   *     {@link Node} in {@code b}'s successor block is a {@link TypeCastNode} or a {@link
   *     TernaryExpressionNode}, and {@code node} is an operand of the successor node; {@code false}
   *     otherwise
   */
  private boolean inCastOrTernary(Node node) {
    if (!(node.getBlock() instanceof SingleSuccessorBlock)) {
      return false;
    }
    Block successorBlock = ((SingleSuccessorBlock) node.getBlock()).getSuccessor();
    if (successorBlock != null) {
      List<Node> succNodes = successorBlock.getNodes();
      if (succNodes.size() > 0) {
        Node succNode = succNodes.get(0);
        if (succNode instanceof TypeCastNode) {
          return ((TypeCastNode) succNode).getOperand().equals(node);
        } else if (succNode instanceof TernaryExpressionNode) {
          TernaryExpressionNode ternaryExpressionNode = (TernaryExpressionNode) succNode;
          return ternaryExpressionNode.getThenOperand().equals(node)
              || ternaryExpressionNode.getElseOperand().equals(node);
        }
      }
    }
    return false;
  }

  /**
   * Transfers ownership of locals to {@code @Owning} parameters at a method or constructor call.
   *
   * @param obligations the current set of obligations, which is side-effected to remove obligations
   *     for locals that are passed as owning parameters to the method or constructor
   * @param node a method or constructor invocation node
   */
  private void doOwnershipTransferToParameters(
      Set<ImmutableSet<LocalVarWithTree>> obligations, Node node) {

    if (checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)) {
      // Never transfer ownership to parameters, matching ECJ's default.
      return;
    }

    List<Node> actualParams = getArgumentsOfInvocation(node);
    List<? extends VariableElement> formalParams = getFormalsOfInvocation(node);

    if (actualParams.size() != formalParams.size()) {
      // This could happen, e.g., with varargs, or with strange cases like generated Enum
      // constructors. In the varargs case (i.e. if the varargs parameter is owning),
      // only the first of the varargs arguments will actually get transferred: the second
      // and later varargs arguments will continue to be tracked at the call-site.
      // For now, just skip this case - the worst that will happen is a false positive in
      // cases like the varargs one described above.
      // TODO allow for ownership transfer here if needed in future
      return;
    }
    for (int i = 0; i < actualParams.size(); i++) {
      Node n = removeCastsAndGetTmpVarIfPresent(actualParams.get(i));
      if (n instanceof LocalVariableNode) {
        LocalVariableNode local = (LocalVariableNode) n;
        if (varTrackedInObligations(local, obligations)) {

          // check if formal has an @Owning annotation
          VariableElement formal = formalParams.get(i);
          Set<AnnotationMirror> annotationMirrors = typeFactory.getDeclAnnotations(formal);

          for (AnnotationMirror anno : annotationMirrors) {
            if (AnnotationUtils.areSameByName(
                anno, "org.checkerframework.checker.mustcall.qual.Owning")) {
              // transfer ownership!
              obligations.remove(getResourceAliasSetForVar(obligations, local));
              break;
            }
          }
        }
      }
    }
  }

  /**
   * If the return type of the enclosing method is {@code @Owning}, transfer ownership of the return
   * value and treat its obligations as satisfied by removing it from obligations.
   *
   * @param node a return node
   * @param cfg the CFG of the enclosing method
   * @param obligations the current set of tracked obligations, side-effected to remove the
   *     obligations of the returned value if ownership is transferred
   */
  private void handleReturn(
      ReturnNode node, ControlFlowGraph cfg, Set<ImmutableSet<LocalVarWithTree>> obligations) {
    if (isTransferOwnershipAtReturn(cfg)) {
      Node result = node.getResult();
      Node temp = typeFactory.getTempVarForNode(result);
      if (temp != null) {
        result = temp;
      }
      if (result instanceof LocalVariableNode) {
        removeObligationContainingVar(obligations, (LocalVariableNode) result);
      }
    }
  }

  /**
   * Should we transfer ownership to the return type of the method corresponding to a CFG? Returns
   * true when there is no {@link NotOwning} annotation on the return type.
   *
   * @param cfg the CFG of the method
   * @return true iff we should transfer ownership to the return type of the method corresponding to
   *     a CFG
   */
  private boolean isTransferOwnershipAtReturn(ControlFlowGraph cfg) {
    if (checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)) {
      // Default to always transferring at return if not using LO, just like ECJ does.
      return true;
    }

    UnderlyingAST underlyingAST = cfg.getUnderlyingAST();
    if (underlyingAST instanceof UnderlyingAST.CFGMethod) {
      // TODO: lambdas? I think in that case we'll return false, below.
      MethodTree method = ((UnderlyingAST.CFGMethod) underlyingAST).getMethod();
      ExecutableElement executableElement = TreeUtils.elementFromDeclaration(method);
      return typeFactory.getDeclAnnotation(executableElement, NotOwning.class) == null;
    }
    return false;
  }

  /**
   * Updates a set of obligations to account for an assignment.
   *
   * @param node the assignment
   * @param obligations the set of obligations to update, which may be side-effected depending on
   *     the assignment: assigning to an owning field might remove obligations, assigning to a new
   *     local variable might modify an obligation (by increasing the size of its resource alias
   *     set), etc.
   */
  private void handleAssignment(
      AssignmentNode node, Set<ImmutableSet<LocalVarWithTree>> obligations) {
    Node lhs = node.getTarget();
    Element lhsElement = TreeUtils.elementFromTree(lhs.getTree());
    // Use the temporary variable for the rhs if it exists.
    Node rhs = removeCasts(node.getExpression());
    LocalVariableNode tempVarForRhs = typeFactory.getTempVarForNode(rhs);
    if (tempVarForRhs != null) {
      rhs = tempVarForRhs;
    }

    // Ownership transfer to @Owning field.
    if (lhsElement.getKind() == ElementKind.FIELD) {
      boolean isOwningField =
          !checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)
              && typeFactory.getDeclAnnotation(lhsElement, Owning.class) != null;
      // Check that there is no obligation on the lhs, if the field is non-final and owning.
      if (isOwningField
          && typeFactory.canCreateObligations()
          && !ElementUtils.isFinal(lhsElement)) {
        checkReassignmentToField(node, obligations);
      }
      // Remove obligations from local variables, now that the owning field is responsible.
      // (When obligation creation is turned off, non-final fields cannot take ownership).
      if (isOwningField
          && rhs instanceof LocalVariableNode
          && (typeFactory.canCreateObligations() || ElementUtils.isFinal(lhsElement))) {
        removeObligationContainingVar(obligations, (LocalVariableNode) rhs);
      }
    } else if (lhs instanceof LocalVariableNode) {
      LocalVariableNode lhsVar = (LocalVariableNode) lhs;
      doGenKillForPseudoAssignment(node, obligations, lhsVar, rhs);
    }
  }

  /**
   * Remove any obligations that contain {@link LocalVarWithTree} {@code var} in their
   * resource-alias set.
   *
   * @param obligations the set of obligations
   * @param var a variable
   */
  private void removeObligationContainingVar(
      Set<ImmutableSet<LocalVarWithTree>> obligations, LocalVariableNode var) {
    Set<LocalVarWithTree> setContainingRhs = getResourceAliasSetForVar(obligations, var);
    obligations.remove(setContainingRhs);
  }

  /**
   * Remove any {@link TypeCastNode}s wrapping a node, returning the operand nested within the type
   * casts.
   *
   * @param node a node
   * @return node, but with any surrounding typecasts removed
   */
  private Node removeCasts(Node node) {
    while (node instanceof TypeCastNode) {
      node = ((TypeCastNode) node).getOperand();
    }
    return node;
  }

  /**
   * Update a set of tracked obligations to account for a (pseudo-)assignment to some variable, as
   * in a gen-kill dataflow analysis problem. Pseudo-assignments may include operations that
   * "assign" to a temporary variable. E.g., for an expression {@code b ? x : y}, this method may
   * process an "assignment" from {@code x} or {@code y} to the temporary variable representing the
   * ternary expression.
   *
   * @param node the node performing the pseudo-assignment; it is not necessarily an assignment node
   * @param obligations the tracked obligations, which will be side-effected
   * @param lhsVar the left-hand side variable for the pseudo-assignment
   * @param rhs the right-hand side for the pseudo-assignment, which must have been converted to a
   *     temporary variable (via a call to {@link
   *     ResourceLeakAnnotatedTypeFactory#getTempVarForNode(Node)})
   */
  private void doGenKillForPseudoAssignment(
      Node node,
      Set<ImmutableSet<LocalVarWithTree>> obligations,
      LocalVariableNode lhsVar,
      Node rhs) {
    // Replacements to eventually perform in obligations.  We keep this map to avoid a
    // ConcurrentModificationException in the loop below.
    Map<ImmutableSet<LocalVarWithTree>, ImmutableSet<LocalVarWithTree>> replacements =
        new LinkedHashMap<>();
    // construct lhsVarWithTreeToGen once outside the loop for efficiency
    LocalVarWithTree lhsVarWithTreeToGen =
        new LocalVarWithTree(new LocalVariable(lhsVar), node.getTree());
    for (ImmutableSet<LocalVarWithTree> obligation : obligations) {
      Set<LocalVarWithTree> kill = new LinkedHashSet<>();
      // always kill the lhs var if present
      addLocalVarWithTreeToSetIfPresent(obligation, lhsVar.getElement(), kill);
      LocalVarWithTree gen = null;
      // if rhs is a variable tracked in the obligation's resource alias set, gen the lhs
      if (rhs instanceof LocalVariableNode) {
        LocalVariableNode rhsVar = (LocalVariableNode) rhs;
        for (LocalVarWithTree lvwt : obligation) {
          if (lvwt.localVar.getElement().equals(rhsVar.getElement())) {
            gen = lhsVarWithTreeToGen;
            // We remove temp vars from tracking once they are assigned to another location.
            if (typeFactory.isTempVar(rhsVar)) {
              addLocalVarWithTreeToSetIfPresent(obligation, rhsVar.getElement(), kill);
            }
            break;
          }
        }
      }
      // Check if there is something to do before creating a new obligation, for efficiency.
      if (kill.isEmpty() && gen == null) {
        continue;
      }
      Set<LocalVarWithTree> newObligation = new LinkedHashSet<>(obligation);
      newObligation.removeAll(kill);
      if (gen != null) {
        newObligation.add(gen);
      }
      if (newObligation.size() == 0) {
        // We have killed the last reference to the resource; check the must-call obligation.
        MustCallAnnotatedTypeFactory mcAtf =
            typeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
        checkMustCall(
            obligation,
            typeFactory.getStoreBefore(node),
            mcAtf.getStoreBefore(node),
            "variable overwritten by assignment " + node.getTree());
      }
      replacements.put(obligation, ImmutableSet.copyOf(newObligation));
    }
    // Finally, update obligations according to the replacements.
    for (Map.Entry<ImmutableSet<LocalVarWithTree>, ImmutableSet<LocalVarWithTree>> entry :
        replacements.entrySet()) {
      obligations.remove(entry.getKey());
      if (!entry.getValue().isEmpty()) {
        obligations.add(entry.getValue());
      }
    }
  }

  /**
   * If a {@link LocalVarWithTree} is present in {@code resourceAliasSet} whose variable element is
   * {@code element}, add it to {@code lvwtSet}.
   *
   * @param resourceAliasSet the resource-alias set of an obligation
   * @param element the element to search for
   * @param lvwtSet the set to add to
   */
  private void addLocalVarWithTreeToSetIfPresent(
      ImmutableSet<LocalVarWithTree> resourceAliasSet,
      Element element,
      Set<LocalVarWithTree> lvwtSet) {
    for (LocalVarWithTree lvwt : resourceAliasSet) {
      if (lvwt.localVar.getElement().equals(element)) {
        lvwtSet.add(lvwt);
      }
    }
  }

  /**
   * Issues an error if the given re-assignment to a non-final, owning field is not valid. A
   * re-assignment is valid if the called methods type of the lhs before the assignment satisfies
   * the must-call obligations of the field.
   *
   * @param node an assignment to a non-final, owning field
   * @param obligations current tracked obligations
   */
  private void checkReassignmentToField(
      AssignmentNode node, Set<ImmutableSet<LocalVarWithTree>> obligations) {

    Node lhsNode = node.getTarget();

    if (!(lhsNode instanceof FieldAccessNode)) {
      throw new BugInCF(
          "tried to check reassignment to a field for a non-field node: "
              + node
              + " of type: "
              + node.getClass());
    }

    FieldAccessNode lhs = (FieldAccessNode) lhsNode;
    Node receiver = lhs.getReceiver();

    // TODO: it would be better to defer getting the path until after we check
    // for a CreatesMustCallFor annotation, because getting the path can be expensive.
    // It might be possible to exploit the CFG structure to find the containing
    // method (rather than using the path, as below), because if a method is being
    // analyzed then it should be the root of the CFG (I think).
    TreePath currentPath = typeFactory.getPath(node.getTree());
    MethodTree enclosingMethodTree = TreePathUtil.enclosingMethod(currentPath);

    if (enclosingMethodTree == null) {
      // Assignments outside of methods must be field initializers, which
      // are always safe. (Note that we don't support static owning fields,
      // so static initializers don't need to be considered here.)
      return;
    }

    // Check that there is a corresponding CreatesMustCallFor annotation, unless this is
    // 1) an assignment to a field of a newly-declared local variable that can't be in scope
    // for the containing method, or 2) the rhs is a null literal (so there's nothing to reset).
    if (!(receiver instanceof LocalVariableNode
            && varTrackedInObligations((LocalVariableNode) receiver, obligations))
        && !(node.getExpression() instanceof NullLiteralNode)) {
      checkEnclosingMethodIsCreatesMustCallFor(node, enclosingMethodTree);
    }

    MustCallAnnotatedTypeFactory mcTypeFactory =
        typeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
    AnnotationMirror mcAnno =
        mcTypeFactory.getAnnotationFromJavaExpression(
            JavaExpression.fromNode(lhs), node.getTree(), MustCall.class);
    List<String> mcValues =
        AnnotationUtils.getElementValueArray(
            mcAnno, mcTypeFactory.getMustCallValueElement(), String.class);

    if (mcValues.isEmpty()) {
      return;
    }

    // Get the store before the RHS rather than the assignment node, because the CFG always has
    // the RHS first. If the RHS has side-effects, then the assignment node's store will have
    // had its inferred types erased.
    Node rhs = node.getExpression();
    CFStore cmStoreBefore = typeFactory.getStoreBefore(rhs);
    CFValue cmValue = cmStoreBefore == null ? null : cmStoreBefore.getValue(lhs);
    AnnotationMirror cmAnno = null;
    if (cmValue != null) {
      for (AnnotationMirror anno : cmValue.getAnnotations()) {
        if (AnnotationUtils.areSameByName(
            anno, "org.checkerframework.checker.calledmethods.qual.CalledMethods")) {
          cmAnno = anno;
          break;
        }
      }
    }
    if (cmAnno == null) {
      cmAnno = typeFactory.top;
    }
    if (!calledMethodsSatisfyMustCall(mcValues, cmAnno)) {
      Element lhsElement = TreeUtils.elementFromTree(lhs.getTree());
      if (!checker.shouldSkipUses(lhsElement)) {
        checker.reportError(
            node.getTree(),
            "required.method.not.called",
            formatMissingMustCallMethods(mcValues),
            lhsElement.asType().toString(),
            " Non-final owning field might be overwritten");
      }
    }
  }

  /**
   * Checks that the method that encloses an assignment is marked with @CreatesMustCallFor
   * annotation whose target is the object whose field is being re-assigned.
   *
   * @param node an assignment node whose lhs is a non-final, owning field
   * @param enclosingMethod the MethodTree in which the re-assignment takes place
   */
  private void checkEnclosingMethodIsCreatesMustCallFor(
      AssignmentNode node, MethodTree enclosingMethod) {
    Node lhs = node.getTarget();
    if (!(lhs instanceof FieldAccessNode)) {
      return;
    }

    String receiverString = receiverAsString((FieldAccessNode) lhs);
    if (TreeUtils.isConstructor(enclosingMethod)) {
      // Constructors always create must-call obligations, so there is no need for them to
      // be annotated.
      return;
    }
    ExecutableElement enclosingMethodElt = TreeUtils.elementFromDeclaration(enclosingMethod);
    MustCallAnnotatedTypeFactory mcAtf =
        typeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);

    List<String> cmcfValues =
        ResourceLeakVisitor.getCreatesMustCallForValues(enclosingMethodElt, mcAtf, typeFactory);

    if (cmcfValues.isEmpty()) {
      checker.reportError(
          enclosingMethod,
          "missing.creates.mustcall.for",
          receiverString,
          ((FieldAccessNode) lhs).getFieldName());
      return;
    }

    String checked = "";
    for (String targetStrWithoutAdaptation : cmcfValues) {
      String targetStr = null;
      try {
        targetStr =
            StringToJavaExpression.atMethodBody(
                    targetStrWithoutAdaptation, enclosingMethod, checker)
                .toString();
      } catch (JavaExpressionParseException e) {
        targetStr = targetStrWithoutAdaptation;
      }
      if (targetStr.equals(receiverString)) {
        // This @CreatesMustCallFor annotation matches.
        return;
      }
      if ("".equals(checked)) {
        checked += targetStr;
      } else {
        checked += ", " + targetStr;
      }
    }
    checker.reportError(
        enclosingMethod,
        "incompatible.creates.mustcall.for",
        receiverString,
        ((FieldAccessNode) lhs).getFieldName(),
        checked);
  }

  /**
   * Gets a standardized name for an object whose field is being re-assigned.
   *
   * @param fieldAccessNode a field access node
   * @return the name of the object whose field is being accessed (the receiver), as a string
   */
  private String receiverAsString(FieldAccessNode fieldAccessNode) {
    Node receiver = fieldAccessNode.getReceiver();
    if (receiver instanceof ThisNode) {
      return "this";
    }
    if (receiver instanceof LocalVariableNode) {

      return ((LocalVariableNode) receiver).getName();
    }
    throw new BugInCF(
        "unexpected receiver of field assignment: " + receiver + " of type " + receiver.getClass());
  }

  /**
   * Finds the actual parameter passed in the {@code @MustCallAlias} position for a call.
   *
   * @param callNode callNode representing the call
   * @return if {@code callNode} invokes a method with a {@code @MustCallAlias} annotation on some
   *     formal parameter (or the receiver), returns the result of calling {@link
   *     #removeCastsAndGetTmpVarIfPresent(Node)} on the actual parameter passed in that position.
   *     Otherwise, returns {@code null}.
   */
  private @Nullable Node getMustCallAliasParamVar(Node callNode) {
    Preconditions.checkArgument(
        callNode instanceof MethodInvocationNode || callNode instanceof ObjectCreationNode);
    if (!typeFactory.hasMustCallAlias(callNode.getTree())) {
      return null;
    }

    Node result = null;
    List<Node> actualParams = getArgumentsOfInvocation(callNode);
    List<? extends VariableElement> formalParams = getFormalsOfInvocation(callNode);
    for (int i = 0; i < actualParams.size(); i++) {
      if (typeFactory.hasMustCallAlias(formalParams.get(i))) {
        result = actualParams.get(i);
        break;
      }
    }

    // If none of the parameters were @MustCallAlias, it must be the receiver
    if (result == null && callNode instanceof MethodInvocationNode) {
      result = ((MethodInvocationNode) callNode).getTarget().getReceiver();
    }

    result = removeCastsAndGetTmpVarIfPresent(result);
    return result;
  }

  /**
   * If a temporary variable exists for node after typecasts have been removed, return it.
   * Otherwise, return node.
   *
   * @param node a node
   * @return either a tempvar for node's content sans typecasts, or node
   */
  private Node removeCastsAndGetTmpVarIfPresent(Node node) {
    // TODO create temp vars for TypeCastNodes as well, so we don't need to explicitly remove casts
    // here
    node = removeCasts(node);
    LocalVariableNode tmpVar = typeFactory.getTempVarForNode(node);
    return tmpVar != null ? tmpVar : node;
  }

  /**
   * Get the nodes representing the arguments of a method or constructor invocation from the
   * invocation node.
   *
   * @param node a MethodInvocation or ObjectCreation node
   * @return a list of the arguments, in order
   */
  private List<Node> getArgumentsOfInvocation(Node node) {
    List<Node> arguments;
    if (node instanceof MethodInvocationNode) {
      MethodInvocationNode invocationNode = (MethodInvocationNode) node;
      arguments = invocationNode.getArguments();
    } else {
      if (!(node instanceof ObjectCreationNode)) {
        throw new BugInCF("unexpected node type " + node.getClass());
      }
      arguments = ((ObjectCreationNode) node).getArguments();
    }
    return arguments;
  }

  /**
   * Get the elements representing the formal parameters of a method or constructor, from an
   * invocation of that method or constructor.
   *
   * @param node a method invocation or object creation node
   * @return a list of the declarations of the formal parameters of the method or constructor being
   *     invoked
   */
  private List<? extends VariableElement> getFormalsOfInvocation(Node node) {
    ExecutableElement executableElement;
    if (node instanceof MethodInvocationNode) {
      MethodInvocationNode invocationNode = (MethodInvocationNode) node;
      executableElement = TreeUtils.elementFromUse(invocationNode.getTree());
    } else {
      if (!(node instanceof ObjectCreationNode)) {
        throw new BugInCF("unexpected node type " + node.getClass());
      }
      executableElement = TreeUtils.elementFromUse(((ObjectCreationNode) node).getTree());
    }

    List<? extends VariableElement> formals = executableElement.getParameters();
    return formals;
  }

  /**
   * Does the method being invoked have a not-owning return type?
   *
   * @param node a method invocation
   * @return true iff (1) the checker is not in no-lightweight-ownership mode, (2) the method has a
   *     non-void return type, and (3) a NotOwning annotation is present on the method declaration
   */
  private boolean hasNotOwningReturnType(MethodInvocationNode node) {
    if (checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)) {
      // Default to always transferring at return if not using LO, just like ECJ does.
      return false;
    }
    MethodInvocationTree methodInvocationTree = node.getTree();
    ExecutableElement executableElement = TreeUtils.elementFromUse(methodInvocationTree);
    // void methods are "not owning" by construction
    return (ElementUtils.getType(executableElement).getKind() == TypeKind.VOID)
        || (typeFactory.getDeclAnnotation(executableElement, NotOwning.class) != null);
  }

  /**
   * Get all successor blocks for some block, except for those corresponding to ignored exception
   * types.
   *
   * @param block input block
   * @return set of pairs (b, t), where b is a relevant successor block, and t is the type of
   *     exception for the CFG edge from block to b, or {@code null} if b is a non-exceptional
   *     successor
   */
  private Set<Pair<Block, @Nullable TypeMirror>> getRelevantSuccessors(Block block) {
    if (block.getType() == Block.BlockType.EXCEPTION_BLOCK) {
      ExceptionBlock excBlock = (ExceptionBlock) block;
      Set<Pair<Block, @Nullable TypeMirror>> result = new LinkedHashSet<>();
      // regular successor
      Block regularSucc = excBlock.getSuccessor();
      if (regularSucc != null) {
        result.add(Pair.of(regularSucc, null));
      }
      // relevant exception successors
      Map<TypeMirror, Set<Block>> exceptionalSuccessors = excBlock.getExceptionalSuccessors();
      for (Map.Entry<TypeMirror, Set<Block>> entry : exceptionalSuccessors.entrySet()) {
        TypeMirror exceptionType = entry.getKey();
        if (!isIgnoredExceptionType(((Type) exceptionType).tsym.getQualifiedName())) {
          for (Block exSucc : entry.getValue()) {
            result.add(Pair.of(exSucc, exceptionType));
          }
        }
      }
      return result;
    } else {
      Set<Pair<Block, @Nullable TypeMirror>> result = new LinkedHashSet<>();
      for (Block b : block.getSuccessors()) {
        result.add(Pair.of(b, null));
      }
      return result;
    }
  }

  /**
   * Propagates a set of obligations to relevant successors, and performs consistency checks when
   * variables are going out of scope.
   *
   * @param visited block-obligations pairs already analyzed or already on the worklist
   * @param worklist current worklist
   * @param obligations obligations to propagate to successors
   * @param curBlock the current block
   */
  private void handleSuccessorBlocks(
      Set<BlockWithObligations> visited,
      Deque<BlockWithObligations> worklist,
      Set<ImmutableSet<LocalVarWithTree>> obligations,
      Block curBlock) {
    List<Node> curBlockNodes = curBlock.getNodes();
    for (Pair<Block, @Nullable TypeMirror> succAndExcType : getRelevantSuccessors(curBlock)) {
      Block succ = succAndExcType.first;
      TypeMirror exceptionType = succAndExcType.second;
      Set<ImmutableSet<LocalVarWithTree>> curObligations =
          handleTernarySuccIfNeeded(curBlock, succ, obligations);
      // obligationsForSucc eventually contains the obligations to propagate to succ.  It may be
      // mutated in the
      // loop below.
      Set<ImmutableSet<LocalVarWithTree>> obligationsForSucc = new LinkedHashSet<>();
      // A detailed reason to give in the case that a relevant variable goes out of scope with an
      // unsatisfied obligation along the current control-flow edge.
      String reasonForSucc =
          exceptionType == null
              ?
              // Technically the variable may be going out of scope before the method exit, but that
              // doesn't seem to provide additional helpful information.
              "regular method exit"
              : "possible exceptional exit due to "
                  + ((ExceptionBlock) curBlock).getNode().getTree()
                  + " with exception type "
                  + exceptionType;
      CFStore succRegularStore = analysis.getInput(succ).getRegularStore();
      for (ImmutableSet<LocalVarWithTree> resourceAliasSet : curObligations) {
        boolean noInfoInSuccStoreForVars = true;
        for (LocalVarWithTree resourceAlias : resourceAliasSet) {
          if (!varNotPresentInStoreAndNotForTernary(succRegularStore, resourceAlias)) {
            noInfoInSuccStoreForVars = false;
            break;
          }
        }
        if (succ instanceof SpecialBlockImpl /* exit block */ || noInfoInSuccStoreForVars) {
          MustCallAnnotatedTypeFactory mcAtf =
              typeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);

          // If succ is an exceptional successor, and resourceAliasSet represents the temporary
          // variable for
          // curBlock's node, do not propagate, as in the exceptional case the "assignment" to
          // the temporary variable does not succeed.
          if (exceptionType != null) {
            Node exceptionalNode = removeCasts(((ExceptionBlock) curBlock).getNode());
            LocalVariableNode tmpVarForExcNode = typeFactory.getTempVarForNode(exceptionalNode);
            if (tmpVarForExcNode != null
                && resourceAliasSet.size() == 1
                && Iterables.getOnlyElement(resourceAliasSet)
                    .localVar
                    .getElement()
                    .equals(tmpVarForExcNode.getElement())) {
              break;
            }
          }

          // Always propagate resourceAliasSet to successor if current block represents code nested
          // in a cast or
          // ternary expression.  Without this logic, the analysis may report a false positive in
          // when the resourceAliasSet represents a temporary variable for a nested expression, as
          // the temporary
          // may not appear in the successor store and hence seems to be going out of scope.  The
          // temporary will be handled with special logic; casts are unwrapped at various points in
          // the analysis, and ternary expressions are handled by handleTernarySuccIfNeeded.
          if (curBlockNodes.size() == 1 && inCastOrTernary(curBlockNodes.get(0))) {
            obligationsForSucc.add(resourceAliasSet);
            break;
          }

          if (curBlockNodes.size() == 0 /* curBlock is special or conditional */) {
            // Use the store from the block actually being analyzed, rather than succRegularStore,
            // if succRegularStore contains no information about the variables of interest.
            // In the case where none of the aliases in resourceAliasSet appear in
            // succRegularStore, the resource is going out of scope, and it doesn't make
            // sense to pass succRegularStore to checkMustCall - the successor store will
            // not have any information about it, by construction, and
            // any information in the previous store remains true. If any locals from the resource
            // alias set do appear in succRegularStore, we will always use that store.
            CFStore cmStore =
                noInfoInSuccStoreForVars
                    ? analysis.getInput(curBlock).getRegularStore()
                    : succRegularStore;
            CFStore mcStore = mcAtf.getStoreForBlock(noInfoInSuccStoreForVars, curBlock, succ);
            checkMustCall(resourceAliasSet, cmStore, mcStore, reasonForSucc);
          } else { // In this case, current block has at least one node.
            // Use the called-methods store immediately after the last node in curBlock.
            Node last = curBlockNodes.get(curBlockNodes.size() - 1);
            CFStore cmStoreAfter = typeFactory.getStoreAfter(last);
            // If this is an exceptional block, check the MC store beforehand to avoid
            // issuing an error about a call to a CreatesMustCallFor method that might throw
            // an exception. Otherwise, use the store after.
            CFStore mcStore;
            if (exceptionType != null && isInvocationOfCreatesMustCallForMethod(last)) {
              mcStore = mcAtf.getStoreBefore(last);
            } else {
              mcStore = mcAtf.getStoreAfter(last);
            }
            checkMustCall(resourceAliasSet, cmStoreAfter, mcStore, reasonForSucc);
          }

        } else { // In this case, there is info in the successor store about some alias in
          // resourceAliasSet.
          // Handles the possibility that some resource in the resourceAliasSet may go out of scope.
          Set<LocalVarWithTree> obligationCopy = new LinkedHashSet<>(resourceAliasSet);
          obligationCopy.removeIf(
              assign -> varNotPresentInStoreAndNotForTernary(succRegularStore, assign));
          obligationsForSucc.add(ImmutableSet.copyOf(obligationCopy));
        }
      }

      propagate(new BlockWithObligations(succ, obligationsForSucc), visited, worklist);
    }
  }

  /**
   * Returns true if {@code assign.localVar} has no value in {@code store} and {@code assign.tree}
   * is not a {@link ConditionalExpressionTree}. The check for a {@link ConditionalExpressionTree}
   * is to accommodate our handling of ternary expressions, where we track the temporary variable
   * for the expression at the program point before that expression; see {@link
   * #handleTernarySuccIfNeeded(Block, Block, Set)}.
   *
   * @param store the store to check
   * @param assign the lvt to check
   * @return true if the variable is not present in store and is not for a ternary
   */
  private boolean varNotPresentInStoreAndNotForTernary(CFStore store, LocalVarWithTree assign) {
    return store.getValue(assign.localVar) == null
        && !(assign.tree instanceof ConditionalExpressionTree);
  }

  /**
   * Handles control-flow to a block starting with a {@link TernaryExpressionNode}.
   *
   * <p>In the Checker Framework's CFG, a ternary expression is represented as a {@link
   * org.checkerframework.dataflow.cfg.block.ConditionalBlock}, whose successor blocks are the two
   * cases of the ternary expression. The {@link TernaryExpressionNode} is the first node in the
   * successor block of the two cases.
   *
   * <p>To handle this representation, we treat the control-flow transition from a node for a
   * ternary expression case <em>c</em> to the successor {@link TernaryExpressionNode} <em>t</em> as
   * a pseudo-assignment from <em>c</em> to the temporary variable for <em>t</em>. With this
   * handling, the obligations reaching the successor node of <em>t</em> will properly account for
   * the execution of case <em>c</em>.
   *
   * <p>If the successor block does not begin with a {@link TernaryExpressionNode} that needs to be
   * handled, this method simply returns {@code obligations}.
   *
   * @param pred the predecessor block, potentially corresponding to the ternary expression case
   * @param succ the successor block, potentially starting with a {@link TernaryExpressionNode}
   * @param obligations the obligations before the control-flow transition
   * @return a new set of obligations to account for the {@link TernaryExpressionNode}, or just
   *     {@code obligations} if no handling is required.
   */
  private Set<ImmutableSet<LocalVarWithTree>> handleTernarySuccIfNeeded(
      Block pred, Block succ, Set<ImmutableSet<LocalVarWithTree>> obligations) {
    List<Node> succNodes = succ.getNodes();
    if (succNodes.isEmpty() || !(succNodes.get(0) instanceof TernaryExpressionNode)) {
      return obligations;
    }
    TernaryExpressionNode ternaryNode = (TernaryExpressionNode) succNodes.get(0);
    LocalVariableNode ternaryTempVar = typeFactory.getTempVarForNode(ternaryNode);
    if (ternaryTempVar == null) {
      return obligations;
    }
    List<Node> predNodes = pred.getNodes();
    // Right-hand side of the pseudo-assignment to the ternary expression temporary variable.
    Node rhs = removeCasts(predNodes.get(predNodes.size() - 1));
    if (!(rhs instanceof LocalVariableNode)) {
      rhs = typeFactory.getTempVarForNode(rhs);
      if (rhs == null) {
        return obligations;
      }
    }
    Set<ImmutableSet<LocalVarWithTree>> newDefs = new LinkedHashSet<>(obligations);
    doGenKillForPseudoAssignment(ternaryNode, newDefs, ternaryTempVar, rhs);
    return newDefs;
  }

  /**
   * Returns true if node is a MethodInvocationNode of a method with a CreatesMustCallFor
   * annotation.
   *
   * @param node a node
   * @return true if node is a MethodInvocationNode of a method with a CreatesMustCallFor annotation
   */
  private boolean isInvocationOfCreatesMustCallForMethod(Node node) {
    if (!(node instanceof MethodInvocationNode)) {
      return false;
    }
    MethodInvocationNode miNode = (MethodInvocationNode) node;
    return typeFactory.hasCreatesMustCallFor(miNode);
  }

  /**
   * Finds {@link Owning} formal parameters for the method corresponding to a CFG.
   *
   * @param cfg the CFG
   * @return the owning formal parameters of the method that corresponds to the given cfg
   */
  private Set<ImmutableSet<LocalVarWithTree>> computeOwningParameters(ControlFlowGraph cfg) {
    Set<ImmutableSet<LocalVarWithTree>> result = new LinkedHashSet<>();
    UnderlyingAST underlyingAST = cfg.getUnderlyingAST();
    if (underlyingAST instanceof UnderlyingAST.CFGMethod) {
      // TODO what about lambdas?
      MethodTree method = ((UnderlyingAST.CFGMethod) underlyingAST).getMethod();
      for (VariableTree param : method.getParameters()) {
        Element paramElement = TreeUtils.elementFromDeclaration(param);
        boolean isMustCallAlias = typeFactory.hasMustCallAlias(paramElement);
        if (isMustCallAlias
            || (typeFactory.hasDeclaredMustCall(param)
                && !checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)
                && paramElement.getAnnotation(Owning.class) != null)) {
          result.add(ImmutableSet.of(new LocalVarWithTree(new LocalVariable(paramElement), param)));
          // Increment numMustCall for each @Owning parameter tracked by the enclosing method.
          incrementNumMustCall(paramElement);
        }
      }
    }
    return result;
  }

  /**
   * Checks whether there is some resource alias set <em>R</em> in {@code obligations} such that
   * <em>R</em> contains a {@link LocalVarWithTree} whose local variable is {@code node}.
   *
   * @param var the local variable to look for
   * @param obligations the set of obligations to search
   * @return true iff there is a resource alias set in obligations that contains node
   */
  private static boolean varTrackedInObligations(
      LocalVariableNode var, Set<ImmutableSet<LocalVarWithTree>> obligations) {
    Element nodeElement = var.getElement();
    for (Set<LocalVarWithTree> resourceAliasSet : obligations) {
      for (LocalVarWithTree alias : resourceAliasSet) {
        if (alias.localVar.getElement().equals(nodeElement)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Gets the resource alias set that contains the given local variable, if one exists in
   * obligations.
   *
   * @param obligations set of obligations
   * @param node variable of interest
   * @return the resource alias set in {@code obligations} containing {@code node}, or {@code null}
   *     if there is no such set
   */
  private static @Nullable ImmutableSet<LocalVarWithTree> getResourceAliasSetForVar(
      Set<ImmutableSet<LocalVarWithTree>> obligations, LocalVariableNode node) {
    Element nodeElement = node.getElement();
    for (ImmutableSet<LocalVarWithTree> resourceAliasSet : obligations) {
      for (LocalVarWithTree lvwt : resourceAliasSet) {
        if (lvwt.localVar.getElement().equals(nodeElement)) {
          return resourceAliasSet;
        }
      }
    }
    return null;
  }

  /**
   * For the given resourceAliasSet, checks that at least one of its variables has its
   * {@code @MustCall} obligation satisfied, based on {@code @CalledMethods} and {@code @MustCall}
   * types in the given stores.
   *
   * @param resourceAliasSet the resourceAliasSet
   * @param cmStore the called-methods store
   * @param mcStore the must-call store
   * @param outOfScopeReason if the {@code @MustCall} obligation is not satisfied, a useful
   *     explanation to include in the error message
   */
  private void checkMustCall(
      ImmutableSet<LocalVarWithTree> resourceAliasSet,
      CFStore cmStore,
      CFStore mcStore,
      String outOfScopeReason) {

    List<String> mustCallValue = typeFactory.getMustCallValue(resourceAliasSet, mcStore);
    // optimization: if there are no must-call methods, we do not need to perform the check
    if (mustCallValue == null || mustCallValue.isEmpty()) {
      return;
    }

    boolean mustCallSatisfied = false;
    for (LocalVarWithTree alias : resourceAliasSet) {

      // sometimes the store is null!  this looks like a bug in checker dataflow.
      // TODO track down and report the root-cause bug
      CFValue lhsCFValue = cmStore != null ? cmStore.getValue(alias.localVar) : null;
      AnnotationMirror cmAnno = null;

      if (lhsCFValue != null) { // When store contains the lhs
        for (AnnotationMirror anno : lhsCFValue.getAnnotations()) {
          if (AnnotationUtils.areSameByName(
              anno, "org.checkerframework.checker.calledmethods.qual.CalledMethods")) {
            cmAnno = anno;
          }
        }
      }
      if (cmAnno == null) {
        cmAnno =
            typeFactory
                .getAnnotatedType(alias.localVar.getElement())
                .getAnnotationInHierarchy(typeFactory.top);
      }

      if (calledMethodsSatisfyMustCall(mustCallValue, cmAnno)) {
        mustCallSatisfied = true;
        break;
      }
    }

    if (!mustCallSatisfied) {
      // Report the error at the first alias' definition. This choice is arbitrary but consistent.
      LocalVarWithTree firstAlias = resourceAliasSet.iterator().next();
      if (!reportedMustCallErrors.contains(firstAlias)) {
        if (!checker.shouldSkipUses(TreeUtils.elementFromTree(firstAlias.tree))) {
          reportedMustCallErrors.add(firstAlias);
          checker.reportError(
              firstAlias.tree,
              "required.method.not.called",
              formatMissingMustCallMethods(mustCallValue),
              firstAlias.localVar.getType().toString(),
              outOfScopeReason);
        }
      }
    }
  }

  /**
   * Increment the -AcountMustCall counter.
   *
   * @param node the node being counted, to extract the type
   */
  private void incrementNumMustCall(Node node) {
    if (checker.hasOption(ResourceLeakChecker.COUNT_MUST_CALL)) {
      TypeMirror type = node.getType();
      incrementMustCallImpl(type);
    }
  }

  /**
   * Increment the -AcountMustCall counter.
   *
   * @param elt the elt being counted, to extract the type
   */
  private void incrementNumMustCall(Element elt) {
    if (checker.hasOption(ResourceLeakChecker.COUNT_MUST_CALL)) {
      TypeMirror type = elt.asType();
      incrementMustCallImpl(type);
    }
  }

  /**
   * Shared implementation for the two version of countMustCall. Don't call this directly.
   *
   * @param type the type of the object that has a must call obligation
   */
  private void incrementMustCallImpl(TypeMirror type) {
    // only count uses of JDK classes, since that's what we report on in the paper
    if (!isJdkClass(TypesUtils.getTypeElement(type).getQualifiedName().toString())) {
      return;
    }
    checker.numMustCall++;
  }

  /**
   * Is the given class a java* class? This is a heuristic for whether the class was defined in the
   * JDK.
   *
   * @param qualifiedName a fully qualified name of a class
   * @return true iff the type's fully-qualified name starts with "java", indicating that it is from
   *     a java.* or javax.* package (probably)
   */
  /* package-private */ static boolean isJdkClass(String qualifiedName) {
    return qualifiedName.startsWith("java");
  }

  /**
   * Do the called methods represented by the {@link CalledMethods} type {@code cmAnno} include all
   * the methods in {@code mustCallValues}?
   *
   * @param mustCallValues the strings representing the must-call obligations
   * @param cmAnno an annotation from the called-methods type hierarchy
   * @return true iff cmAnno is a subtype of a called-methods annotation with the same values as
   *     mustCallValues
   */
  private boolean calledMethodsSatisfyMustCall(
      List<String> mustCallValues, AnnotationMirror cmAnno) {
    // Create this annotation and use a subtype test because there's no guarantee that
    // cmAnno is actually an instance of CalledMethods: it could be CMBottom or CMPredicate.
    AnnotationMirror cmAnnoForMustCallMethods =
        typeFactory.createCalledMethods(mustCallValues.toArray(new String[0]));
    return typeFactory.getQualifierHierarchy().isSubtype(cmAnno, cmAnnoForMustCallMethods);
  }

  /**
   * The exception types in this set are ignored in the CFG when determining if a resource leaks
   * along an exceptional path. These kinds of errors fall into a few categories: runtime errors,
   * errors that the JVM can issue on any statement, and errors that can be prevented by running
   * some other CF checker.
   */
  private static Set<String> ignoredExceptionTypes =
      new HashSet<>(
          ImmutableSet.of(
              // Any method call has a CFG edge for Throwable/RuntimeException/Error to represent
              // run-time
              // misbehavior. Ignore it.
              Throwable.class.getCanonicalName(),
              Error.class.getCanonicalName(),
              RuntimeException.class.getCanonicalName(),
              // Use the Nullness Checker to prove this won't happen.
              NullPointerException.class.getCanonicalName(),
              // These errors can't be predicted statically, so we'll ignore them and assume they
              // won't
              // happen.
              ClassCircularityError.class.getCanonicalName(),
              ClassFormatError.class.getCanonicalName(),
              NoClassDefFoundError.class.getCanonicalName(),
              OutOfMemoryError.class.getCanonicalName(),
              // It's not our problem if the Java type system is wrong.
              ClassCastException.class.getCanonicalName(),
              // It's not our problem if the code is going to divide by zero.
              ArithmeticException.class.getCanonicalName(),
              // Use the Index Checker to prevent these errors.
              ArrayIndexOutOfBoundsException.class.getCanonicalName(),
              NegativeArraySizeException.class.getCanonicalName(),
              // Most of the time, this exception is infeasible, as the charset used
              // is guaranteed to be present by the Java spec (e.g., "UTF-8"). Eventually,
              // we could refine this exclusion by looking at the charset being requested.
              UnsupportedEncodingException.class.getCanonicalName()));

  /**
   * Is {@code exceptionClassName} an exception type we are ignoring, to avoid excessive false
   * positives? For now we ignore {@code java.lang.Throwable}, {@code NullPointerException}, and the
   * runtime exceptions that can occur at any point during the program due to something going wrong
   * in the JVM, like OutOfMemoryError and ClassCircularityError.
   *
   * @param exceptionClassName the fully-qualified name of the exception
   * @return true if the given exception class should be ignored
   */
  private static boolean isIgnoredExceptionType(@FullyQualifiedName Name exceptionClassName) {
    return ignoredExceptionTypes.contains(exceptionClassName.toString());
  }

  /**
   * If the input {@code state} has not been visited yet, add it to {@code visited} and {@code
   * worklist}.
   *
   * @param state the current state
   * @param visited the states that have been analyzed or are already on the worklist
   * @param worklist the states that will be analyzed
   */
  private static void propagate(
      BlockWithObligations state,
      Set<BlockWithObligations> visited,
      Deque<BlockWithObligations> worklist) {

    if (visited.add(state)) {
      worklist.add(state);
    }
  }

  /**
   * Formats a list of must-call method names to be printed in an error message.
   *
   * @param mustCallVal the list of must-call strings
   * @return a formatted string
   */
  /* package-private */
  static String formatMissingMustCallMethods(List<String> mustCallVal) {
    int size = mustCallVal.size();
    if (size == 0) {
      throw new BugInCF("empty mustCallVal " + mustCallVal);
    } else if (size == 1) {
      return "method " + mustCallVal.get(0);
    } else {
      return "methods " + String.join(", ", mustCallVal);
    }
  }

  /**
   * A pair of a {@link Block} and a set of obligations (i.e. dataflow facts) on entry to the block.
   * Each obligation is an {@code ImmutableSet<LocalVarWithTree>}, representing a set of resource
   * aliases for some tracked resource. The analyzer's worklist consists of BlockWithObligations
   * objects, each representing the need to handle the set of obligations reaching the block during
   * analysis.
   */
  private static class BlockWithObligations {

    /** The block. */
    public final Block block;

    /** The facts. */
    public final ImmutableSet<ImmutableSet<LocalVarWithTree>> obligations;

    /**
     * Create a new BlockWithObligations from a block and a set of obligations.
     *
     * @param b the block
     * @param obligations the set of incoming obligations at the start of the block (may be the
     *     empty set)
     */
    public BlockWithObligations(Block b, Set<ImmutableSet<LocalVarWithTree>> obligations) {
      this.block = b;
      this.obligations = ImmutableSet.copyOf(obligations);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      BlockWithObligations that = (BlockWithObligations) o;
      return block.equals(that.block) && obligations.equals(that.obligations);
    }

    @Override
    public int hashCode() {
      return Objects.hash(block, obligations);
    }
  }

  /**
   * A pair of a local variable along with a tree in the corresponding method that "assigns" the
   * variable. Besides a normal assignment, the tree may be a {@link VariableTree} in the case of a
   * formal parameter. We keep the tree for error-reporting purposes (so we can report an error per
   * assignment to a local, pinpointing the expression whose MustCall may not be satisfied).
   *
   * <p>This class is used to represent a single resource alias in a resource alias set.
   */
  /* package-private */ static class LocalVarWithTree {

    /** The variable. */
    public final LocalVariable localVar;

    /** The tree at which it was assigned, for error reporting. */
    public final Tree tree;

    /**
     * Create a new LocalVarWithTree.
     *
     * @param localVar the local variable
     * @param tree the tree
     */
    public LocalVarWithTree(LocalVariable localVar, Tree tree) {
      this.localVar = localVar;
      this.tree = tree;
    }

    @Override
    public String toString() {
      return "(LocalVarWithTree: localVar: " + localVar + " |||| tree: " + tree + ")";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LocalVarWithTree that = (LocalVarWithTree) o;
      return localVar.equals(that.localVar) && tree.equals(that.tree);
    }

    @Override
    public int hashCode() {
      return Objects.hash(localVar, tree);
    }
  }
}
