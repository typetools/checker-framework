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
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.mustcall.CreatesObligationElementSupplier;
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

/**
 * An analyzer that checks consistency of {@code @MustCall} and {@code @CalledMethods} types within
 * a method, thereby detecting resource leaks. For any expression <emph>e</emph> in the method, the
 * analyzer ensures that at method exit, there exists a resource alias <emph>r</emph> of
 * <emph>e</emph> such that @MustCall(r) is contained in @CalledMethods(r). For any <emph>e</emph>
 * for which this property does not hold, the analyzer reports a {@code
 * "required.method.not.called"} error, indicating a possible resource leak.
 */
/* package-private */
class MustCallConsistencyAnalyzer {

  /** By default, should we transfer ownership to the caller when a variable is returned? */
  static final boolean TRANSFER_OWNERSHIP_AT_RETURN = true;

  /** {@code @MustCall} errors reported thus far, to avoid duplicates */
  private final Set<LocalVarWithTree> reportedMustCallErrors = new HashSet<>();

  private final ResourceLeakAnnotatedTypeFactory typeFactory;

  private final ResourceLeakChecker checker;

  private final CFAnalysis analysis;

  /* package-private */
  MustCallConsistencyAnalyzer(
      ResourceLeakAnnotatedTypeFactory typeFactory,
      ResourceLeakChecker checker,
      CFAnalysis analysis) {
    this.typeFactory = typeFactory;
    this.checker = checker;
    this.analysis = analysis;
  }

  /**
   * The main function of the consistency dataflow analysis. The analysis tracks dataflow facts of
   * type {@code ImmutableSet<LocalVarWithTree>}, each representing a set of resource aliases for
   * some value with a non-empty {@code @MustCall} obligation. The analysis is currently implemented
   * directly using a worklist; in the future, it should be transitioned to use the Checker dataflow
   * framework.
   *
   * @param cfg the control flow graph of the method to check
   */
  /* package-private */
  void analyze(ControlFlowGraph cfg) {
    Set<BlockWithFacts> visited = new LinkedHashSet<>();
    Deque<BlockWithFacts> worklist = new ArrayDeque<>();

    // add any owning parameters to initial set of variables to track
    BlockWithFacts entryBlockFacts =
        new BlockWithFacts(cfg.getEntryBlock(), computeOwningParameters(cfg));
    worklist.add(entryBlockFacts);
    visited.add(entryBlockFacts);

    while (!worklist.isEmpty()) {
      BlockWithFacts curBlockFacts = worklist.removeLast();
      List<Node> nodes = curBlockFacts.block.getNodes();
      // A *mutable* set that eventually holds the set of facts to be propagated to successor
      // blocks. The set is initialized to the current facts and updated by the methods invoked in
      // the for loop below.
      Set<ImmutableSet<LocalVarWithTree>> newFacts = new LinkedHashSet<>(curBlockFacts.facts);

      for (Node node : nodes) {
        if (node instanceof AssignmentNode) {
          handleAssignment((AssignmentNode) node, newFacts);
        } else if (node instanceof ReturnNode) {
          handleReturn((ReturnNode) node, cfg, newFacts);
        } else if (node instanceof MethodInvocationNode || node instanceof ObjectCreationNode) {
          handleInvocation(newFacts, node);
        }
      }

      handleSuccessorBlocks(visited, worklist, newFacts, curBlockFacts.block);
    }
  }

  /**
   * Update a set of facts to account for a method or constructor invocation
   *
   * @param facts the facts to update
   * @param node the method or constructor invocation
   */
  private void handleInvocation(Set<ImmutableSet<LocalVarWithTree>> facts, Node node) {
    doOwnershipTransferToParameters(facts, node);
    // Count calls to @CreatesObligation methods as creating new resources, for now.
    if (node instanceof MethodInvocationNode
        && typeFactory.canCreateObligations()
        && typeFactory.hasCreatesObligation((MethodInvocationNode) node)) {
      checkCreatesObligationInvocation(facts, (MethodInvocationNode) node);
      incrementNumMustCall(node);
    }

    if (skipTrackingInvocationResult(facts, node)) {
      return;
    }

    if (typeFactory.hasMustCall(node.getTree())) {
      incrementNumMustCall(node);
    }
    trackInvocationResult(facts, node);
  }

  /**
   * If node is an invocation of a this or super constructor that has a MCA return type and an MCA
   * parameter, check if any variable in defs is being passed to the other constructor. If so,
   * remove it from defs.
   *
   * @param defs current defs
   * @param node a super or this constructor invocation
   */
  private void handleThisOrSuperConstructorMustCallAlias(
      Set<ImmutableSet<LocalVarWithTree>> defs, Node node) {
    Node mcaParam = getMustCallAliasParamVar(node);
    // If the MCA param is also in the def set, then remove it -
    // its obligation has been fulfilled by being passed on to the MCA constructor (because we must
    // be in a constructor body if we've encountered a this/super constructor call).
    if (mcaParam instanceof LocalVariableNode) {
      removeFactContainingVar(defs, (LocalVariableNode) mcaParam);
    }
  }

  /**
   * Checks that an invocation of a CreatesObligation method is valid. Such an invocation is valid
   * if one of the following conditions is true: 1) the target is an owning pointer 2) the target is
   * tracked in newdefs 3) the method in which the invocation occurs also has an @CreatesObligation
   * annotation, with the same target
   *
   * <p>If none of the above are true, this method issues a reset.not.owning error.
   *
   * <p>For soundness, this method also guarantees that if the target is tracked in newdefs, any
   * tracked aliases will be removed (lest the analysis conclude that it is already closed because
   * one of these aliases was closed before the reset method was invoked). Aliases created after the
   * reset method is invoked are still permitted.
   *
   * @param newDefs the local variables that have been defined in the current compilation unit (and
   *     are therefore going to be checked later). This value is side-effected if it contains the
   *     target of the reset method.
   * @param node a method invocation node, invoking a method with a CreatesObligation annotation
   */
  private void checkCreatesObligationInvocation(
      Set<ImmutableSet<LocalVarWithTree>> newDefs, MethodInvocationNode node) {

    TreePath currentPath = typeFactory.getPath(node.getTree());
    List<JavaExpression> targetExprs =
        CreatesObligationElementSupplier.getCreatesObligationExpressions(
            node, typeFactory, typeFactory);
    Set<JavaExpression> missing = new HashSet<>();
    for (JavaExpression target : targetExprs) {
      boolean validTarget = false;
      if (target instanceof LocalVariable) {
        ImmutableSet<LocalVarWithTree> toRemoveSet = null;
        ImmutableSet<LocalVarWithTree> toAddSet = null;
        for (ImmutableSet<LocalVarWithTree> defAliasSet : newDefs) {
          for (LocalVarWithTree localVarWithTree : defAliasSet) {
            if (target.equals(localVarWithTree.localVar)) {
              // satisfies case 2 above. Remove all its aliases, then return below.
              if (toRemoveSet != null) {
                throw new BugInCF(
                    "tried to remove multiple sets containing a reset target at once");
              }
              toRemoveSet = defAliasSet;
              toAddSet = ImmutableSet.of(localVarWithTree);
            }
          }
        }

        if (toRemoveSet != null) {
          newDefs.remove(toRemoveSet);
          newDefs.add(toAddSet);
          // satisfies case 2
          validTarget = true;
        }

        Element elt = ((LocalVariable) target).getElement();
        if (!validTarget
            && !checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)
            && typeFactory.getDeclAnnotation(elt, Owning.class) != null) {
          // if the target is an Owning param, this satisfies case 1
          validTarget = true;
        }
      }
      if (!validTarget && target instanceof FieldAccess) {
        Element elt = ((FieldAccess) target).getField();
        if (!checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)
            && typeFactory.getDeclAnnotation(elt, Owning.class) != null) {
          // if the target is an Owning field, this satisfies case 1
          validTarget = true;
        }
      }

      if (!validTarget) {
        // TODO: getting this every time is inefficient if a method has many @CreatesObligation
        // annotations,
        //  but that should be a rare path
        MethodTree enclosingMethod = TreePathUtil.enclosingMethod(currentPath);
        if (enclosingMethod != null) {
          ExecutableElement enclosingElt = TreeUtils.elementFromDeclaration(enclosingMethod);
          MustCallAnnotatedTypeFactory mcAtf =
              typeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
          List<String> enclosingCoValues =
              ResourceLeakVisitor.getCOValues(enclosingElt, mcAtf, typeFactory);
          if (!enclosingCoValues.isEmpty()) {
            for (String enclosingCoValue : enclosingCoValues) {
              JavaExpression enclosingTarget;
              try {
                enclosingTarget =
                    StringToJavaExpression.atMethodBody(enclosingCoValue, enclosingMethod, checker);
              } catch (JavaExpressionParseException e) {
                // TODO: or issue an unparseable error?
                enclosingTarget = null;
              }

              if (representSame(target, enclosingTarget)) {
                // this satisifies case 3
                validTarget = true;
              }
            }
          }
        }
      }
      if (!validTarget) {
        missing.add(target);
      }
    }

    if (missing.isEmpty()) {
      // all targets were valid
      return;
    }

    String missingStrs =
        missing.stream().map(JavaExpression::toString).collect(Collectors.joining(", "));
    checker.reportError(node.getTree(), "reset.not.owning", missingStrs);
  }

  /**
   * Checks whether the two JavaExpressions are the same. This is identical to calling equals() on
   * one of them, with two exceptions: the second expression can be null, and this references are
   * compared using their underlying type (ThisReference#equals always returns true, which isn't
   * accurate in the case of nested classes).
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
   * ownership of the result. Searches for the set of same resources in defs and add the new
   * LocalVarWithTree to it if one exists. Otherwise creates a new set.
   */
  private void trackInvocationResult(Set<ImmutableSet<LocalVarWithTree>> facts, Node node) {
    Tree tree = node.getTree();
    // we need to track the result of the call iff there is a temporary variable for the node
    LocalVariableNode tmpVar = typeFactory.getTempVarForTree(node);
    if (tmpVar == null) {
      return;
    }
    LocalVarWithTree tmpVarWithTree = new LocalVarWithTree(new LocalVariable(tmpVar), tree);

    // Set mustCallAlias to the MCA parameter if any exists, otherwise it remains null
    Node mustCallAlias = getMustCallAliasParamVar(node);

    // If mustCallAlias is still null and call returns @This, set mustCallAlias to the receiver
    if (mustCallAlias == null
        && node instanceof MethodInvocationNode
        && typeFactory.returnsThis((MethodInvocationTree) tree)) {
      mustCallAlias =
          removeCastsAndGetTmpVarIfPresent(((MethodInvocationNode) node).getTarget().getReceiver());
    }

    // If mustCallAlias is local variable already tracked by some fact, add tmpVarWithTree
    // to the set containing mustCallAlias. Otherwise, add it to a new set
    if (mustCallAlias instanceof LocalVariableNode
        && varInFacts(facts, (LocalVariableNode) mustCallAlias)) {
      ImmutableSet<LocalVarWithTree> factContainingMustCallAlias =
          getFactContainingVar(facts, (LocalVariableNode) mustCallAlias);
      ImmutableSet<LocalVarWithTree> newFact =
          FluentIterable.from(factContainingMustCallAlias).append(tmpVarWithTree).toSet();
      facts.remove(factContainingMustCallAlias);
      facts.add(newFact);
    } else if (mustCallAlias instanceof LocalVariableNode
        || mustCallAlias instanceof FieldAccessNode) {
      // we do not track the call result if the MustCallAlias parameter is a local (that
      // case is handled above; the local must already be in the facts) or a field (handling of
      // @Owning fields is a completely separate check, and we never need to track an alias of
      // non-@Owning fields)
      return;
    } else {
      facts.add(ImmutableSet.of(tmpVarWithTree));
    }
  }

  /**
   * Checks for cases where we do not need to track the result of a method call. We can skip the
   * check when the method invocation is a call to "this" or a super constructor call, when the
   * method's return type is annotated with MustCallAlias and the argument in the corresponding
   * position is an owning field, or when the method's return type is non-owning, which can either
   * be because the method has no return type or because it is annotated with {@link NotOwning}.
   */
  private boolean skipTrackingInvocationResult(
      Set<ImmutableSet<LocalVarWithTree>> defs, Node node) {
    Tree callTree = node.getTree();
    if (callTree.getKind() == Tree.Kind.METHOD_INVOCATION) {
      MethodInvocationTree methodInvokeTree = (MethodInvocationTree) callTree;

      if (TreeUtils.isSuperConstructorCall(methodInvokeTree)
          || TreeUtils.isThisConstructorCall(methodInvokeTree)) {
        handleThisOrSuperConstructorMustCallAlias(defs, node);
        return true;
      }
      return returnTypeIsMustCallAliasWithIgnorable((MethodInvocationNode) node)
          || hasNotOwningReturnType((MethodInvocationNode) node);
    }
    return false;
  }

  /**
   * Returns true if this node represents a method invocation of a must-call alias method, where the
   * other must call alias is some ignorable pointer, such as an owning field or a pointer that is
   * guaranteed to be non-owning, such as this or a non-owning field.
   *
   * @param node a method invocation node
   * @return if this is the invocation of a method whose return type is MCA with an owning field or
   *     a non-owning pointer
   */
  private boolean returnTypeIsMustCallAliasWithIgnorable(MethodInvocationNode node) {
    Node mcaParam = getMustCallAliasParamVar(node);
    return mcaParam instanceof FieldAccessNode || mcaParam instanceof ThisNode;
  }

  /**
   * Checks if {@code node} is nested inside a {@link TypeCastNode} or a {@link
   * TernaryExpressionNode}, by looking at the successor block in the CFG.
   *
   * @param node the CFG node
   * @return {@code true} if {@code node} is in a {@link SingleSuccessorBlock} {@code b}, the first
   *     {@link Node} in {@code b}'s successor block is a {@link TypeCastNode} or a {@link
   *     TernaryExpressionNode}, and {@code node} is an operand of the successor node; {@code false}
   *     otherwise
   */
  private boolean nestedInCastOrTernary(Node node) {
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
   * logic to transfer ownership of locals to {@code @Owning} parameters at a method or constructor
   * call
   */
  private void doOwnershipTransferToParameters(
      Set<ImmutableSet<LocalVarWithTree>> facts, Node node) {

    if (checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)) {
      // never transfer ownership to parameters, matching ECJ's default
      return;
    }

    List<Node> actualParams = getArgumentsOfMethodOrConstructor(node);
    List<? extends VariableElement> formalParams = getFormalsOfMethodOrConstructor(node);

    if (actualParams.size() != formalParams.size()) {
      // this could happen, e.g., with varargs, or with strange cases like generated Enum
      // constructors
      // for now, just skip this case
      // TODO allow for ownership transfer here if needed in future
      return;
    }
    for (int i = 0; i < actualParams.size(); i++) {
      Node n = removeCastsAndGetTmpVarIfPresent(actualParams.get(i));
      if (n instanceof LocalVariableNode) {
        LocalVariableNode local = (LocalVariableNode) n;
        if (varInFacts(facts, local)) {

          // check if formal has an @Owning annotation
          VariableElement formal = formalParams.get(i);
          Set<AnnotationMirror> annotationMirrors = typeFactory.getDeclAnnotations(formal);

          if (annotationMirrors.stream()
              .anyMatch(
                  anno ->
                      AnnotationUtils.areSameByName(
                          anno, "org.checkerframework.checker.mustcall.qual.Owning"))) {
            // transfer ownership!
            facts.remove(getFactContainingVar(facts, local));
          }
        }
      }
    }
  }

  /**
   * If the return type of the enclosing method is {@code @Owning}, transfer ownership of the return
   * value and stop tracking it in the facts
   */
  private void handleReturn(
      ReturnNode node, ControlFlowGraph cfg, Set<ImmutableSet<LocalVarWithTree>> newFacts) {
    if (isTransferOwnershipAtReturn(cfg)) {
      Node result = node.getResult();
      Node temp = typeFactory.getTempVarForTree(result);
      if (temp != null) {
        result = temp;
      }
      if (result instanceof LocalVariableNode) {
        removeFactContainingVar(newFacts, (LocalVariableNode) result);
      }
    }
  }

  /**
   * Should we transfer ownership to the return type of the method corresponding to a CFG? Returns
   * true when either (1) there is an explicit {@link Owning} annotation on the return type or (2)
   * the policy is to transfer ownership by default, and there is no {@link NotOwning} annotation on
   * the return type
   */
  private boolean isTransferOwnershipAtReturn(ControlFlowGraph cfg) {
    if (checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)) {
      // Default to always transferring at return if not using LO, just like ECJ does.
      return true;
    }

    UnderlyingAST underlyingAST = cfg.getUnderlyingAST();
    if (underlyingAST instanceof UnderlyingAST.CFGMethod) {
      // TODO: lambdas?
      MethodTree method = ((UnderlyingAST.CFGMethod) underlyingAST).getMethod();
      ExecutableElement executableElement = TreeUtils.elementFromDeclaration(method);
      return (typeFactory.getDeclAnnotation(executableElement, Owning.class) != null)
          || (TRANSFER_OWNERSHIP_AT_RETURN
              && typeFactory.getDeclAnnotation(executableElement, NotOwning.class) == null);
    }
    return false;
  }

  /**
   * Updates a set of facts to account for an assignment
   *
   * @param node the assignment
   * @param newFacts the set of facts to update
   */
  private void handleAssignment(AssignmentNode node, Set<ImmutableSet<LocalVarWithTree>> newFacts) {
    // use the temporary variable for the rhs if it exists
    Node rhs = removeCasts(node.getExpression());
    if (typeFactory.getTempVarForTree(rhs) != null) {
      rhs = typeFactory.getTempVarForTree(rhs);
    }
    Node lhs = node.getTarget();
    Element lhsElement = TreeUtils.elementFromTree(lhs.getTree());

    // Ownership transfer to @Owning field
    if (lhsElement.getKind().equals(ElementKind.FIELD)) {
      boolean isOwningField =
          !checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)
              && typeFactory.getDeclAnnotation(lhsElement, Owning.class) != null;
      // Check that there is no obligation on the lhs, if the field is non-final and owning.
      if (isOwningField
          && typeFactory.canCreateObligations()
          && !ElementUtils.isFinal(lhsElement)) {
        checkReassignmentToField(node, newFacts);
      }
      // Remove obligations from local variables, now that the owning field is responsible.
      // (When obligation creation is turned off, non-final fields cannot take ownership).
      if (isOwningField
          && rhs instanceof LocalVariableNode
          && (typeFactory.canCreateObligations() || ElementUtils.isFinal(lhsElement))) {
        removeFactContainingVar(newFacts, (LocalVariableNode) rhs);
      }
    } else if (lhs instanceof LocalVariableNode) {
      LocalVariableNode lhsVar = (LocalVariableNode) lhs;
      if (isTryWithResourcesVariable(lhsVar)) {
        // don't track try-with-resources variables.  Also, we know that whatever value gets
        // assigned to the variable will be closed.  So, if the RHS is a tracked variable, remove
        // its set from the defs
        if (rhs instanceof LocalVariableNode) {
          removeFactContainingVar(newFacts, (LocalVariableNode) rhs);
        }
      } else {
        doGenKillForPseudoAssignment(node, newFacts, lhsVar, rhs);
      }
    }
  }

  /**
   * Remove any facts from a fact set that contain a {@link LocalVarWithTree} with a particular
   * variable
   *
   * @param facts the set of facts
   * @param var the variable
   */
  private void removeFactContainingVar(
      Set<ImmutableSet<LocalVarWithTree>> facts, LocalVariableNode var) {
    Set<LocalVarWithTree> setContainingRhs = getFactContainingVar(facts, var);
    facts.remove(setContainingRhs);
  }

  /**
   * remove any {@link TypeCastNode}s wrapping a node, returning the operand nested within the type
   * casts
   */
  private Node removeCasts(Node node) {
    while (node instanceof TypeCastNode) {
      node = ((TypeCastNode) node).getOperand();
    }
    return node;
  }

  /**
   * Update a set of tracked definitions to account for a (pseudo-)assignment to some variable, as
   * in a gen-kill dataflow analysis problem. Pseudo-assignments may include operations that
   * "assign" to a temporary variable. E.g., for an expression {@code b ? x : y}, this method may
   * process an "assignment" from {@code x} or {@code y} to the temporary variable representing the
   * ternary expression.
   *
   * @param node the node performing the assignment.
   * @param facts the tracked facts
   * @param lhsVar the left-hand side variable for the pseudo-assignment
   * @param rhs the right-hand side for the pseudo-assignment
   */
  private void doGenKillForPseudoAssignment(
      Node node, Set<ImmutableSet<LocalVarWithTree>> facts, LocalVariableNode lhsVar, Node rhs) {
    // Replacements to eventually perform in the facts.  We keep this map to avoid a
    // ConcurrentModificationException in the loop below
    Map<ImmutableSet<LocalVarWithTree>, ImmutableSet<LocalVarWithTree>> replacements =
        new LinkedHashMap<>();
    // construct lhsVarWithTreeToGen once outside the loop for efficiency
    LocalVarWithTree lhsVarWithTreeToGen =
        new LocalVarWithTree(new LocalVariable(lhsVar), node.getTree());
    for (ImmutableSet<LocalVarWithTree> fact : facts) {
      Set<LocalVarWithTree> kill = new LinkedHashSet<>();
      // always kill the lhs var if present
      addLocalVarWithTreeToSetIfPresent(fact, lhsVar.getElement(), kill);
      LocalVarWithTree gen = null;
      // if rhs is a variable tracked in the fact, gen the lhs
      if (rhs instanceof LocalVariableNode) {
        LocalVariableNode rhsVar = (LocalVariableNode) rhs;
        if (fact.stream()
            .anyMatch(lvwt -> lvwt.localVar.getElement().equals(rhsVar.getElement()))) {
          gen = lhsVarWithTreeToGen;
          // we remove temp vars from tracking once they are assigned to another location
          if (typeFactory.isTempVar(rhsVar)) {
            addLocalVarWithTreeToSetIfPresent(fact, rhsVar.getElement(), kill);
          }
        }
      }
      // check if there is something to do before creating a new fact, for efficiency
      if (kill.isEmpty() && gen == null) {
        continue;
      }
      Set<LocalVarWithTree> newFact = new LinkedHashSet<>(fact);
      newFact.removeAll(kill);
      if (gen != null) {
        newFact.add(gen);
      }
      if (newFact.size() == 0) {
        // we have killed the last reference to the resource; check the must-call obligation
        MustCallAnnotatedTypeFactory mcAtf =
            typeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
        checkMustCall(
            fact,
            typeFactory.getStoreBefore(node),
            mcAtf.getStoreBefore(node),
            "variable overwritten by assignment " + node.getTree());
      }
      replacements.put(fact, ImmutableSet.copyOf(newFact));
    }
    // finally, update facts according to the replacements
    for (Map.Entry<ImmutableSet<LocalVarWithTree>, ImmutableSet<LocalVarWithTree>> entry :
        replacements.entrySet()) {
      facts.remove(entry.getKey());
      if (!entry.getValue().isEmpty()) {
        facts.add(entry.getValue());
      }
    }
  }

  /**
   * If a {@link LocalVarWithTree} is present in {@code varWithTreeSet} whose variable element is
   * {@code element}, add it to {@code lvwtSet}
   */
  private void addLocalVarWithTreeToSetIfPresent(
      ImmutableSet<LocalVarWithTree> varWithTreeSet,
      Element element,
      Set<LocalVarWithTree> lvwtSet) {
    varWithTreeSet.stream()
        .filter(lvwt -> lvwt.localVar.getElement().equals(element))
        .findFirst()
        .ifPresent(lvwtSet::add);
  }

  /**
   * Checks that the given re-assignment to a non-final, owning field is valid. Issues an error if
   * not. A re-assignment is valid if the called methods type of the lhs before the assignment
   * satisfies the must-call obligations.
   *
   * @param node an assignment to a non-final, owning field
   * @param newDefs currently in-scope variables that are being tracked
   */
  private void checkReassignmentToField(
      AssignmentNode node, Set<ImmutableSet<LocalVarWithTree>> newDefs) {

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
    // for a CreatesObligation annotation, because getting the path can be expensive.
    // It might be possible to exploit the CFG structure to find the containing
    // method (rather than using the path, as below), because if a method is being
    // analyzed then it should be the root of the CFG (I think).
    TreePath currentPath = typeFactory.getPath(node.getTree());
    MethodTree enclosingMethod = TreePathUtil.enclosingMethod(currentPath);

    if (enclosingMethod == null) {
      // Assignments outside of methods must be field initializers, which
      // are always safe.
      return;
    }

    // Check that there is a corresponding createsObligation annotation, unless this is
    // 1) an assignment to a field of a newly-declared local variable that can't be in scope
    // for the containing method, or 2) the rhs is a null literal (so there's nothing to reset).
    if (!(receiver instanceof LocalVariableNode
            && varInFacts(newDefs, (LocalVariableNode) receiver))
        && !(node.getExpression() instanceof NullLiteralNode)) {
      checkEnclosingMethodIsCreatesObligation(node, enclosingMethod);
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
    AnnotationMirror cmAnno =
        cmValue == null
            ? typeFactory.top
            : cmValue.getAnnotations().stream()
                .filter(
                    anno ->
                        AnnotationUtils.areSameByName(
                            anno, "org.checkerframework.checker.calledmethods.qual.CalledMethods"))
                .findAny()
                .orElse(typeFactory.top);

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
   * Checks that the method that encloses an assignment is marked with @CreatesObligation annotation
   * whose target is the object whose field is being re-assigned.
   *
   * @param node an assignment node whose lhs is a non-final, owning field
   * @param enclosingMethod the MethodTree in which the re-assignment takes place
   */
  private void checkEnclosingMethodIsCreatesObligation(
      AssignmentNode node, MethodTree enclosingMethod) {
    Node lhs = node.getTarget();
    if (!(lhs instanceof FieldAccessNode)) {
      return;
    }

    String receiverString = receiverAsString((FieldAccessNode) lhs);
    if (TreeUtils.isConstructor(enclosingMethod)) {
      // Resetting a constructor doesn't make sense.
      return;
    }
    ExecutableElement enclosingElt = TreeUtils.elementFromDeclaration(enclosingMethod);
    MustCallAnnotatedTypeFactory mcAtf =
        typeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);

    List<String> coValues = ResourceLeakVisitor.getCOValues(enclosingElt, mcAtf, typeFactory);

    if (coValues.isEmpty()) {
      checker.reportError(
          enclosingMethod,
          "missing.creates.obligation",
          receiverString,
          ((FieldAccessNode) lhs).getFieldName());
      return;
    }

    String checked = "";
    for (String targetStrWithoutAdaptation : coValues) {
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
        // This create obligation annotation matches.
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
        "incompatible.creates.obligation",
        receiverString,
        ((FieldAccessNode) lhs).getFieldName(),
        checked);
  }

  /** Gets a standardized name for an object whose field is being re-assigned. */
  private String receiverAsString(FieldAccessNode lhs) {
    Node receiver = lhs.getReceiver();
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
   * @param node node representing the call
   * @return if {@code node} invokes a method with a {@code @MustCallAlias} annotation on some
   *     formal parameter, returns the result of calling {@link
   *     #removeCastsAndGetTmpVarIfPresent(Node)} on the actual parameter passed in that position.
   *     Otherwise, returns {@code null}.
   */
  private @Nullable Node getMustCallAliasParamVar(Node node) {
    Preconditions.checkArgument(
        node instanceof MethodInvocationNode || node instanceof ObjectCreationNode);
    if (!typeFactory.hasMustCallAlias(node.getTree())) {
      return null;
    }

    Node result = null;
    List<Node> actualParams = getArgumentsOfMethodOrConstructor(node);
    List<? extends VariableElement> formalParams = getFormalsOfMethodOrConstructor(node);
    for (int i = 0; i < actualParams.size(); i++) {
      if (typeFactory.hasMustCallAlias(formalParams.get(i))) {
        result = actualParams.get(i);
        break;
      }
    }

    // If none of the parameters were @MustCallAlias, it must be the receiver
    if (result == null && node instanceof MethodInvocationNode) {
      result = ((MethodInvocationNode) node).getTarget().getReceiver();
    }

    result = removeCastsAndGetTmpVarIfPresent(result);
    return result;
  }

  private Node removeCastsAndGetTmpVarIfPresent(Node node) {
    // TODO create temp vars for TypeCastNodes as well, so we don't need to explicitly remove casts
    // here
    node = removeCasts(node);
    LocalVariableNode tmpVar = typeFactory.getTempVarForTree(node);
    return tmpVar != null ? tmpVar : node;
  }

  private List<Node> getArgumentsOfMethodOrConstructor(Node node) {
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

  private List<? extends VariableElement> getFormalsOfMethodOrConstructor(Node node) {
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
   * get all successor blocks for some block, except for those corresponding to ignored exception
   * types
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
      return block.getSuccessors().stream()
          .map(b -> Pair.<Block, TypeMirror>of(b, null))
          .collect(Collectors.toSet());
    }
  }

  /**
   * Propagates a set of facts to relevant successors, and performs consistency checks when
   * variables are going out of scope
   *
   * @param visited block-facts pairs already handled
   * @param worklist current worklist
   * @param facts facts to propagate to successors
   * @param curBlock the current block
   */
  private void handleSuccessorBlocks(
      Set<BlockWithFacts> visited,
      Deque<BlockWithFacts> worklist,
      Set<ImmutableSet<LocalVarWithTree>> facts,
      Block curBlock) {
    List<Node> curBlockNodes = curBlock.getNodes();
    for (Pair<Block, @Nullable TypeMirror> succAndExcType : getRelevantSuccessors(curBlock)) {
      Block succ = succAndExcType.first;
      TypeMirror exceptionType = succAndExcType.second;
      Set<ImmutableSet<LocalVarWithTree>> curFacts =
          handleTernarySuccIfNeeded(curBlock, succ, facts);
      // factsForSucc eventually contains the facts to propagate to succ.  It may be mutated in the
      // loop below.
      Set<ImmutableSet<LocalVarWithTree>> factsForSucc = new LinkedHashSet<>();
      // Set<ImmutableSet<LocalVarWithTree>> toRemove = new LinkedHashSet<>();
      // a detailed reason to give in the case that a relevant variable goes out of scope with an
      // unsatisfied obligation along the current control-flow edge
      String reasonForSucc =
          exceptionType == null
              ?
              // technically the variable may be going out of scope before the method exit, but that
              // doesn't seem to provide additional helpful information
              "regular method exit"
              : "possible exceptional exit due to "
                  + ((ExceptionBlock) curBlock).getNode().getTree()
                  + " with exception type "
                  + exceptionType;
      CFStore succRegularStore = analysis.getInput(succ).getRegularStore();
      for (ImmutableSet<LocalVarWithTree> fact : curFacts) {
        boolean noInfoInSuccStoreForVars =
            fact.stream()
                .allMatch(assign -> varNotPresentInStoreAndNotForTernary(succRegularStore, assign));
        if (succ instanceof SpecialBlockImpl /* exit block */ || noInfoInSuccStoreForVars) {
          MustCallAnnotatedTypeFactory mcAtf =
              typeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);

          // Don't propagate the fact if the curBlock is an ExceptionBlock and the fact represents
          // the temporary variable for curBlock's node
          if (exceptionType != null) {
            Node exceptionalNode = removeCasts(((ExceptionBlock) curBlock).getNode());
            LocalVariableNode tmpVarForExcNode = typeFactory.getTempVarForTree(exceptionalNode);
            if (tmpVarForExcNode != null
                && fact.size() == 1
                && Iterables.getOnlyElement(fact)
                    .localVar
                    .getElement()
                    .equals(tmpVarForExcNode.getElement())) {
              break;
            }
          }

          // always propagate fact to successor if current block represents code nested in a cast or
          // ternary expression.  TODO why???
          if (curBlockNodes.size() == 1 && nestedInCastOrTernary(curBlockNodes.get(0))) {
            factsForSucc.add(fact);
            break;
          }

          if (curBlockNodes.size() == 0 /* curBlock is special or conditional */) {
            // Use the store from the block actually being analyzed, rather than succRegularStore,
            // if succRegularStore contains no information about the variables of interest.
            // In the case where none of the local variables in fact appear in
            // succRegularStore, the variable is going out of scope, and it doesn't make
            // sense to pass succRegularStore to checkMustCall - the successor store will
            // not have any information about it, by construction, and
            // any information in the previous store remains true. If any locals do appear
            // in succRegularStore, we will always use that store.
            CFStore cmStore =
                noInfoInSuccStoreForVars
                    ? analysis.getInput(curBlock).getRegularStore()
                    : succRegularStore;
            CFStore mcStore = mcAtf.getStoreForBlock(noInfoInSuccStoreForVars, curBlock, succ);
            checkMustCall(fact, cmStore, mcStore, reasonForSucc);
          } else { // current block has at least one node
            // use the called-methods store immediately after the last node in curBlock
            Node last = curBlockNodes.get(curBlockNodes.size() - 1);
            CFStore cmStoreAfter = typeFactory.getStoreAfter(last);
            // If this is an exceptional block, check the MC store beforehand to avoid
            // issuing an error about a call to a CreatesObligation method that might throw
            // an exception. Otherwise, use the store after.
            CFStore mcStore;
            if (exceptionType != null && isInvocationOfCOMethod(last)) {
              mcStore = mcAtf.getStoreBefore(last);
            } else {
              mcStore = mcAtf.getStoreAfter(last);
            }
            checkMustCall(fact, cmStoreAfter, mcStore, reasonForSucc);
          }

        } else { // info in successor store about some var in fact
          // handling the possibility that some vars in the fact go out of scope
          Set<LocalVarWithTree> factCopy = new LinkedHashSet<>(fact);
          factCopy.removeIf(
              assign -> varNotPresentInStoreAndNotForTernary(succRegularStore, assign));
          factsForSucc.add(ImmutableSet.copyOf(factCopy));
        }
      }

      propagate(new BlockWithFacts(succ, factsForSucc), visited, worklist);
    }
  }

  /**
   * Returns true if {@code assign.localVar} has no value in {@code store} and {@code assign.tree}
   * is not a {@link ConditionalExpressionTree}. The check for a {@link ConditionalExpressionTree}
   * is to accommodate our handling of ternary expressions, where we track the temporary variable
   * for the expression at the program point before that expression; see {@link
   * #handleTernarySuccIfNeeded(Block, Block, Set)}.
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
   * handling, the facts reaching the successor node of <em>t</em> will properly account for the
   * execution of case <em>c</em>.
   *
   * <p>If the successor block does not begin with a {@link TernaryExpressionNode} that needs to be
   * handled, this method simply returns {@code facts}.
   *
   * @param pred the predecessor block, potentially corresponding to the ternary expression case
   * @param succ the successor block, potentially starting with a {@link TernaryExpressionNode}
   * @param facts the facts before the control-flow transition
   * @return a new set of facts to account for the {@link TernaryExpressionNode}, or just {@code
   *     facts} if no handling is required.
   */
  private Set<ImmutableSet<LocalVarWithTree>> handleTernarySuccIfNeeded(
      Block pred, Block succ, Set<ImmutableSet<LocalVarWithTree>> facts) {
    List<Node> succNodes = succ.getNodes();
    if (succNodes.isEmpty() || !(succNodes.get(0) instanceof TernaryExpressionNode)) {
      return facts;
    }
    TernaryExpressionNode ternaryNode = (TernaryExpressionNode) succNodes.get(0);
    LocalVariableNode ternaryTempVar = typeFactory.getTempVarForTree(ternaryNode);
    if (ternaryTempVar == null) {
      return facts;
    }
    List<Node> predNodes = pred.getNodes();
    // right-hand side of the pseudo-assignment to the ternary expression temporary variable
    Node rhs = removeCasts(predNodes.get(predNodes.size() - 1));
    if (!(rhs instanceof LocalVariableNode)) {
      rhs = typeFactory.getTempVarForTree(rhs);
      if (rhs == null) {
        return facts;
      }
    }
    Set<ImmutableSet<LocalVarWithTree>> newDefs = new LinkedHashSet<>(facts);
    doGenKillForPseudoAssignment(ternaryNode, newDefs, ternaryTempVar, rhs);
    return newDefs;
  }

  /**
   * returns true if node is a MethodInvocationNode of a method with a CreatesObligation annotation.
   *
   * @param node a node
   * @return true if node is a MethodInvocationNode of a method with a CreatesObligation annotation
   */
  private boolean isInvocationOfCOMethod(Node node) {
    if (!(node instanceof MethodInvocationNode)) {
      return false;
    }
    MethodInvocationNode miNode = (MethodInvocationNode) node;
    return typeFactory.hasCreatesObligation(miNode);
  }

  /**
   * Finds {@link Owning} formal parameters for the method corresponding to a CFG
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
            || (typeFactory.hasMustCall(param)
                && !checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP)
                && paramElement.getAnnotation(Owning.class) != null)) {
          result.add(ImmutableSet.of(new LocalVarWithTree(new LocalVariable(paramElement), param)));
          // Increment numMustCall for each @Owning parameter tracked by the enclosing method
          incrementNumMustCall(paramElement);
        }
      }
    }
    return result;
  }

  /**
   * Checks whether there is some fact {@code f} in {@code facts} such that {@code f} contains a
   * {@link LocalVarWithTree} whose local variable is {@code node}
   */
  private static boolean varInFacts(
      Set<ImmutableSet<LocalVarWithTree>> facts, LocalVariableNode node) {
    return facts.stream()
        .flatMap(Set::stream)
        .map(assign -> assign.localVar.getElement())
        .anyMatch(elem -> elem.equals(node.getElement()));
  }

  /**
   * gets the single fact in a set of facts containing some variable
   *
   * @param facts set of facts
   * @param node variable of interest
   * @return the fact in {@code facts} containing {@code node}, or {@code null} if there is no such
   *     fact
   */
  private static ImmutableSet<LocalVarWithTree> getFactContainingVar(
      Set<ImmutableSet<LocalVarWithTree>> facts, LocalVariableNode node) {
    return facts.stream()
        .filter(
            set ->
                set.stream()
                    .anyMatch(assign -> assign.localVar.getElement().equals(node.getElement())))
        .findAny()
        .orElse(null);
  }

  /** checks if the variable has been declared in a try-with-resources header */
  private static boolean isTryWithResourcesVariable(LocalVariableNode lhs) {
    Tree tree = lhs.getTree();
    return tree != null
        && TreeUtils.elementFromTree(tree).getKind().equals(ElementKind.RESOURCE_VARIABLE);
  }

  /**
   * For the given fact, checks that at least one of its variables has its {@code @MustCall}
   * obligation satisfied, based on {@code @CalledMethods} and {@code @MustCall} types in the given
   * stores
   *
   * @param fact the fact
   * @param cmStore the called-methods store
   * @param mcStore the must-call store
   * @param outOfScopeReason if the {@code @MustCall} obligation is not satisfied, a useful
   *     explanation to include in the error message
   */
  private void checkMustCall(
      ImmutableSet<LocalVarWithTree> fact,
      CFStore cmStore,
      CFStore mcStore,
      String outOfScopeReason) {

    List<String> mustCallValue = typeFactory.getMustCallValue(fact, mcStore);
    // optimization: if there are no must-call methods, we do not need to perform the check
    if (mustCallValue == null || mustCallValue.isEmpty()) {
      return;
    }

    boolean mustCallSatisfied = false;
    for (LocalVarWithTree localVarWithTree : fact) {

      // sometimes the store is null!  this looks like a bug in checker dataflow.
      // TODO track down and report the root-cause bug
      CFValue lhsCFValue = cmStore != null ? cmStore.getValue(localVarWithTree.localVar) : null;
      AnnotationMirror cmAnno;

      if (lhsCFValue != null) { // When store contains the lhs
        cmAnno =
            lhsCFValue.getAnnotations().stream()
                .filter(
                    anno ->
                        AnnotationUtils.areSameByName(
                            anno, "org.checkerframework.checker.calledmethods.qual.CalledMethods"))
                .findAny()
                .orElse(typeFactory.top);
      } else {
        cmAnno =
            typeFactory
                .getAnnotatedType(localVarWithTree.localVar.getElement())
                .getAnnotationInHierarchy(typeFactory.top);
      }

      if (calledMethodsSatisfyMustCall(mustCallValue, cmAnno)) {
        mustCallSatisfied = true;
        break;
      }
    }

    if (!mustCallSatisfied) {
      if (reportedMustCallErrors.stream().noneMatch(fact::contains)) {
        LocalVarWithTree firstlocalVarWithTree = fact.iterator().next();
        if (!checker.shouldSkipUses(TreeUtils.elementFromTree(firstlocalVarWithTree.tree))) {
          reportedMustCallErrors.add(firstlocalVarWithTree);
          checker.reportError(
              firstlocalVarWithTree.tree,
              "required.method.not.called",
              formatMissingMustCallMethods(mustCallValue),
              firstlocalVarWithTree.localVar.getType().toString(),
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
    String qualifiedName = TypesUtils.getTypeElement(type).getQualifiedName().toString();
    if (!qualifiedName.startsWith("java")) {
      return;
    }
    checker.numMustCall++;
  }

  /**
   * Do the called methods represented by the {@link CalledMethods} type {@code cmAnno} include all
   * the methods in {@code mustCallValue}?
   */
  private boolean calledMethodsSatisfyMustCall(
      List<String> mustCallValue, AnnotationMirror cmAnno) {
    AnnotationMirror cmAnnoForMustCallMethods =
        typeFactory.createCalledMethods(mustCallValue.toArray(new String[0]));
    return typeFactory.getQualifierHierarchy().isSubtype(cmAnno, cmAnnoForMustCallMethods);
  }

  /**
   * Is {@code exceptionClassName} an exception type we are ignoring, to avoid excessive false
   * positives? For now we ignore {@code java.lang.Throwable}, {@code NullPointerException}, and the
   * runtime exceptions that can occur at any point during the program due to something going wrong
   * in the JVM, like OutOfMemoryErrors or ClassCircularityErrors.
   */
  private static boolean isIgnoredExceptionType(@FullyQualifiedName Name exceptionClassName) {
    // any method call has a CFG edge for Throwable/RuntimeException/Error to represent run-time
    // misbehavior. Ignore it.
    return exceptionClassName.contentEquals(Throwable.class.getCanonicalName())
        || exceptionClassName.contentEquals(RuntimeException.class.getCanonicalName())
        || exceptionClassName.contentEquals(Error.class.getCanonicalName())
        // use the Nullness Checker to prove this won't happen
        || exceptionClassName.contentEquals(NullPointerException.class.getCanonicalName())
        // these errors can't be predicted statically, so we'll ignore them and assume they won't
        // happen
        || exceptionClassName.contentEquals(ClassCircularityError.class.getCanonicalName())
        || exceptionClassName.contentEquals(ClassFormatError.class.getCanonicalName())
        || exceptionClassName.contentEquals(NoClassDefFoundError.class.getCanonicalName())
        || exceptionClassName.contentEquals(OutOfMemoryError.class.getCanonicalName())
        // it's not our problem if the Java type system is wrong
        || exceptionClassName.contentEquals(ClassCastException.class.getCanonicalName())
        // it's not our problem if the code is going to divide by zero.
        || exceptionClassName.contentEquals(ArithmeticException.class.getCanonicalName())
        // use the Index Checker to catch the next two cases
        || exceptionClassName.contentEquals(ArrayIndexOutOfBoundsException.class.getCanonicalName())
        || exceptionClassName.contentEquals(NegativeArraySizeException.class.getCanonicalName())
        // Most of the time, this exception is infeasible, as the charset used
        // is guaranteed to be present by the Java spec (e.g., "UTF-8"). Eventually,
        // we could refine this exclusion by looking at the charset being requested
        || exceptionClassName.contentEquals(UnsupportedEncodingException.class.getCanonicalName());
  }

  /**
   * Updates {@code visited} and {@code worklist} if the input {@code state} has not been visited
   * yet.
   */
  private static void propagate(
      BlockWithFacts state, Set<BlockWithFacts> visited, Deque<BlockWithFacts> worklist) {

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
    if (mustCallVal.size() == 1) {
      return "method " + mustCallVal.get(0);
    } else {
      return "methods " + String.join(", ", mustCallVal);
    }
  }

  /**
   * A pair of a {@link Block} and a set of dataflow facts for our analysis. Each fact is an {@code
   * ImmutableSet<LocalVarWithTree>}, representing a set of resource aliases for some tracked
   * resource. The analyzer's worklist consists of BlockWithFacts objects, each representing the
   * need to handle the set of facts reaching the block during analysis.
   */
  private static class BlockWithFacts {
    public final Block block;
    public final ImmutableSet<ImmutableSet<LocalVarWithTree>> facts;

    public BlockWithFacts(Block b, Set<ImmutableSet<LocalVarWithTree>> facts) {
      this.block = b;
      this.facts = ImmutableSet.copyOf(facts);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      BlockWithFacts that = (BlockWithFacts) o;
      return block.equals(that.block) && facts.equals(that.facts);
    }

    @Override
    public int hashCode() {
      return Objects.hash(block, facts);
    }
  }

  /**
   * A pair of a local variable along with a tree in the corresponding method that "assigns" the
   * variable. Besides a normal assignment, the tree may be a {@link VariableTree} in the case of a
   * formal parameter. We keep the tree for error-reporting purposes (so we can report an error per
   * assignment to a local, pinpointing the expression whose MustCall may not be satisfied).
   */
  /* package-private */ static class LocalVarWithTree {
    public final LocalVariable localVar;
    public final Tree tree;

    public LocalVarWithTree(LocalVariable localVarNode, Tree tree) {
      this.localVar = localVarNode;
      this.tree = tree;
    }

    @Override
    public String toString() {
      return "(LocalVarWithAssignTree: localVar: " + localVar + " |||| tree: " + tree + ")";
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
