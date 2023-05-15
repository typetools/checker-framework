package org.checkerframework.checker.resourceleak;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import java.util.*;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer.BlockWithObligations;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer.Obligation;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer.ResourceAlias;
import org.checkerframework.common.wholeprograminference.WholeProgramInference;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.*;

/**
 * This class contains the Resource Leak Checker's annotation inference algorithm. It contains
 * inference logic for owning annotations on final owning fields. It adds an @Owning annotation on a
 * field if it finds a method that satisfies the @MustCall obligation of the field along some path
 * to the regular exit point.
 */
public class MustCallInferenceLogic {

  /** The set of owning fields that are released within the enMethodElt element. */
  private Set<VariableElement> owningFieldToECM = new HashSet<>();

  /**
   * The type factory for the Resource Leak Checker, which is used to access the Must Call Checker.
   */
  private final ResourceLeakAnnotatedTypeFactory typeFactory;

  /** The {@link Owning} annotation. */
  protected final AnnotationMirror OWNING;

  /** The control flow graph. */
  private final ControlFlowGraph cfg;

  /** TODO */
  private final MustCallConsistencyAnalyzer mcca;

  /** The MethodTree of the cfg. */
  private MethodTree enMethodTree;

  /** The element for the enMethodTree. */
  private ExecutableElement enMethodElt;

  /**
   * Creates a MustCallInferenceLogic. If the type factory has whole program inference enabled, its
   * postAnalyze method should instantiate a new MustCallInferenceLogic using this constructor and
   * then call {@link #runInference()}.
   *
   * @param typeFactory the type factory
   * @param cfg the ControlFlowGraph
   */
  /*package-private*/ MustCallInferenceLogic(
      ResourceLeakAnnotatedTypeFactory typeFactory,
      ControlFlowGraph cfg,
      MustCallConsistencyAnalyzer mcca) {
    this.typeFactory = typeFactory;
    this.mcca = mcca;
    this.cfg = cfg;
    OWNING = AnnotationBuilder.fromClass(this.typeFactory.getElementUtils(), Owning.class);
    enMethodTree = ((UnderlyingAST.CFGMethod) cfg.getUnderlyingAST()).getMethod();
    enMethodElt = TreeUtils.elementFromDeclaration(enMethodTree);
  }

  /**
   * Runs the inference algorithm on the contents of the {@link #cfg} field.
   *
   * <p>Operationally, it checks method invocations for fields with non-empty @MustCall obligations
   * along all paths to the regular exit point in the method body of the method represented by
   * {@link #cfg}, and updates the {@link #owningFieldToECM} set if it discovers an owning field
   * whose must-call obligations were satisfied along one of the checked paths. //TODO update the
   * document
   */
  /*package-private*/ void runInference() {

    Set<BlockWithObligations> visited = new HashSet<>();

    Deque<BlockWithObligations> worklist = new ArrayDeque<>();

    BlockWithObligations entry =
        new BlockWithObligations(cfg.getEntryBlock(), getNonEmptyMCParams(cfg));

    worklist.add(entry);
    visited.add(entry);

    while (!worklist.isEmpty()) {
      BlockWithObligations current = worklist.remove();

      Set<Obligation> obligations = new LinkedHashSet<>(current.obligations);

      for (Node node : current.block.getNodes()) {
        if (node instanceof MethodInvocationNode || node instanceof ObjectCreationNode) {
          updateMethodInvocationOrObjectCreationNode(obligations, node);
          if (node instanceof MethodInvocationNode) {
            checkMethodInvocation(obligations, (MethodInvocationNode) node);
          }
        } else if (node instanceof AssignmentNode) {
          updateObligationsForAssignment(obligations, (AssignmentNode) node);
        }
      }

      propagateRegPaths(obligations, current.block, visited, worklist);
    }
  }

  // TODO add documentation
  private Set<Obligation> getNonEmptyMCParams(ControlFlowGraph cfg) {
    // TODO what about lambdas?
    if (cfg.getUnderlyingAST().getKind() != UnderlyingAST.Kind.METHOD) {
      return Collections.emptySet();
    }
    Set<Obligation> result = new LinkedHashSet<>(1);
    for (VariableTree param : enMethodTree.getParameters()) {
      VariableElement paramElement = TreeUtils.elementFromDeclaration(param);
      if (typeFactory.declaredTypeHasMustCall(param)) {
        result.add(
            new Obligation(
                ImmutableSet.of(new ResourceAlias(new LocalVariable(paramElement), param))));
      }
    }
    return result;
  }

  /**
   * This function returns a set of all owning fields that have been inferred in the current or
   * previous iteration
   *
   * @return set of owning fields
   */
  private Set<VariableElement> getEnclosedOwningFields() {
    ClassTree classTree = TreePathUtil.enclosingClass(typeFactory.getPath(enMethodTree));
    TypeElement classElt = TreeUtils.elementFromDeclaration(classTree);
    Set<VariableElement> enOwningFields = new HashSet<>();
    for (Element memberElt : classElt.getEnclosedElements()) {
      if (memberElt.getKind().isField() && typeFactory.hasOwning(memberElt)) {
        enOwningFields.add((VariableElement) memberElt);
      }
    }
    for (Element memberElt : owningFieldToECM) {
      enOwningFields.add((VariableElement) memberElt);
    }
    return enOwningFields;
  }

  /**
   * Given an index, adds an owning annotation to the parameter at the specified index
   *
   * @param index index of enclosing method's parameter
   */
  private void addOwningOnParams(int index) {
    WholeProgramInference wpi = typeFactory.getWholeProgramInference();
    wpi.addDeclarationAnnotationToFormalParameter(enMethodElt, index, OWNING);
  }

  // TODO
  private void isOwningField(@Nullable Node node, MethodInvocationNode mNode) {
    if (node == null) {
      return;
    }
    Element nodeElt = TreeUtils.elementFromTree(node.getTree());
    if (nodeElt == null || !nodeElt.getKind().isField()) {
      return;
    }
    if (!getEnclosedOwningFields().contains(nodeElt)
        && typeFactory.isCandidateOwningField(nodeElt)) {
      node = NodeUtils.removeCasts(node);
      JavaExpression target = JavaExpression.fromNode(node);
      if (mustCallObligationSatisfied(mNode, nodeElt, target)) {
        // This assumes that any MustCall annotation has at most one element.
        // TODO: generalize this to MustCall annotations with more than one element.
        owningFieldToECM.add((VariableElement) nodeElt);
      }
    }
  }

  // TODO refactor this
  private void updateObligationsForAssignment(
      Set<Obligation> obligations, AssignmentNode assignmentNode) {
    Node lhs = assignmentNode.getTarget();
    Element lhsElement = TreeUtils.elementFromTree(lhs.getTree());
    // Use the temporary variable for the rhs if it exists.
    Node rhs = NodeUtils.removeCasts(assignmentNode.getExpression());
    rhs = mcca.getTempVarOrNode(rhs);

    if (!(rhs instanceof LocalVariableNode)) {
      return;
    }
    Obligation rhsObligation =
        MustCallConsistencyAnalyzer.getObligationForVar(obligations, (LocalVariableNode) rhs);
    if (rhsObligation == null) {
      return;
    }

    if (lhsElement.getKind() == ElementKind.FIELD) {
      if (!getEnclosedOwningFields().contains(lhsElement)) {
        return;
      }

      if (TreeUtils.isConstructor(enMethodTree)) {
        addOwningToParamsIfDisposedAtAssignment(obligations, rhsObligation, rhs);
      } else if (!(rhs instanceof NullLiteralNode)) {
        if (owningFieldToECM.contains((VariableElement) lhsElement)) {
          owningFieldToECM.remove((VariableElement) lhsElement);
        }
        addOwningToParamsIfDisposedAtAssignment(obligations, rhsObligation, rhs);
      }

    } else if (lhsElement.getKind() == ElementKind.RESOURCE_VARIABLE && mcca.isMustCallClose(rhs)) {
      if (rhs instanceof LocalVariableNode) {
        addOwningToParamsIfDisposedAtAssignment(obligations, rhsObligation, rhs);
      }
    } else if (lhs instanceof LocalVariableNode) {
      LocalVariableNode lhsVar = (LocalVariableNode) lhs;
      Obligation lhsObligation =
          MustCallConsistencyAnalyzer.getObligationForVar(obligations, lhsVar);
      if (lhsObligation == null) {
        Set<ResourceAlias> newResourceAliasesForObligation =
            new LinkedHashSet<>(rhsObligation.resourceAliases);
        newResourceAliasesForObligation.add(
            new ResourceAlias(new LocalVariable(lhsVar), lhs.getTree()));
        obligations.remove(rhsObligation);
        obligations.add(new Obligation(newResourceAliasesForObligation));
      } else {
        obligations.remove(rhsObligation);
      }
    }
  }

  private void addOwningToParamsIfDisposedAtAssignment(
      Set<Obligation> obligations, Obligation rhsObligation, Node rhs) {
    Set<ResourceAlias> rhsAliases = rhsObligation.resourceAliases;
    for (ResourceAlias rl : rhsAliases) {
      Element rhdElt = rl.reference.getElement();
      List<? extends VariableTree> params = enMethodTree.getParameters();
      for (int i = 0; i < params.size(); i++) {
        VariableElement paramElt = TreeUtils.elementFromDeclaration(params.get(i));
        if (paramElt.equals(rhdElt)) {
          addOwningOnParams(i);
          mcca.removeObligationsContainingVar(obligations, (LocalVariableNode) rhs);
          break;
        }
      }
    }
  }

  /**
   * Adds an {@link EnsuresCalledMethods} annotation to the enclosing method for any owning field
   * whose must-call obligation is satisfied within the enclosing method.
   */
  private void addEnsuresCalledMethods() {
    Map<String, Set<String>> map = new LinkedHashMap<>();
    for (VariableElement owningField : owningFieldToECM) {
      List<String> mustCallValues = typeFactory.getMustCallValue(owningField);
      assert !mustCallValues.isEmpty() : "Must-call obligation of an owning field is deleted.";
      // Assume must-call set has one element
      String key = mustCallValues.get(0);
      String value = "this." + owningField.getSimpleName().toString();

      map.computeIfAbsent(key, k -> new HashSet<>()).add(value);
    }

    for (String mustCallValue : map.keySet()) {
      AnnotationBuilder builder =
          new AnnotationBuilder(typeFactory.getProcessingEnv(), EnsuresCalledMethods.class);
      builder.setValue("value", map.get(mustCallValue).toArray());
      builder.setValue("methods", new String[] {mustCallValue});
      AnnotationMirror am = builder.build();
      WholeProgramInference wpi = typeFactory.getWholeProgramInference();
      wpi.addMethodDeclarationAnnotation(enMethodElt, am);
    }
  }

  /**
   * Adds the InheritableMustCall annotation on the enclosing class of the current method. If the
   * class already has a non-empty MustCall type, if it's inherited from one its superclass, this
   * method does nothing. Otherwise, adds the current InheritableMustCall annotation to avoid
   * infinite iteration. If the class does not have a MustCall annotation, and the current method is
   * not private and satisfies must-call obligation of all the enclosing owning fields, this method
   * adds an InheritableMustCall annotation with the current method name to the enclosing class.
   */
  private void addOrUpdateMustCall() {
    ClassTree classTree = TreePathUtil.enclosingClass(typeFactory.getPath(enMethodTree));
    TypeElement typeElement = TreeUtils.elementFromDeclaration(classTree);
    if (typeElement == null) {
      return;
    }

    WholeProgramInference wpi = typeFactory.getWholeProgramInference();
    List<String> currentMustCallValues = typeFactory.getMustCallValue(typeElement);
    if (!currentMustCallValues.isEmpty()) {
      // If the class already has a MustCall annotation which is inherited one from a superclass,
      // do nothing
      if (typeElement.getSuperclass() != null) {
        TypeMirror superType = typeElement.getSuperclass();
        TypeElement superTypeElement = TypesUtils.getTypeElement(superType);
        if (superTypeElement != null && !typeFactory.getMustCallValue(superTypeElement).isEmpty()) {
          return;
        }
      }
      // add the current @MustCall annotation to guarantee the termination property. This is
      // necessary when there are multiple candidates for the must-call obligation.
      AnnotationBuilder builder =
          new AnnotationBuilder(typeFactory.getProcessingEnv(), InheritableMustCall.class);
      String[] methodArray = new String[] {currentMustCallValues.get(0).toString()};
      Arrays.sort(methodArray);
      builder.setValue("value", methodArray);
      wpi.addClassDeclarationAnnotation(typeElement, builder.build());
      return;
    }

    // if the enclosing method is not private and satisfies the must-call obligation of all owning
    // fields, add an InheritableMustCall annotation with the current method name
    if (!enMethodTree.getModifiers().getFlags().contains(Modifier.PRIVATE)) {
      if (!owningFieldToECM.isEmpty()
          && owningFieldToECM.size() == getEnclosedOwningFields().size()) {
        AnnotationBuilder builder =
            new AnnotationBuilder(typeFactory.getProcessingEnv(), InheritableMustCall.class);
        String[] methodArray = new String[] {enMethodTree.getName().toString()};
        Arrays.sort(methodArray);
        builder.setValue("value", methodArray);
        wpi.addClassDeclarationAnnotation(typeElement, builder.build());
      }
    }
  }

  private void checkArgsOfMethodCall(
      Set<Obligation> obligations,
      MethodInvocationNode mNode,
      List<? extends VariableTree> paramsOfEnclosingMethod) {
    List<Node> arguments = mcca.getArgumentsOfInvocation(mNode);
    List<? extends VariableElement> parameters = mcca.getParametersOfInvocation(mNode);
    if (parameters.isEmpty()) {
      return;
    }

    // TODO check this again
    for (int i = 0; i < arguments.size(); i++) {
      Node arg = NodeUtils.removeCasts(arguments.get(i));

      if (!typeFactory.hasOwning(parameters.get(i))) {
        continue;
      }

      Set<ResourceAlias> argAliases = getResourceAliasOfArgument(obligations, arg);
      for (int j = 0; j < paramsOfEnclosingMethod.size(); j++) {
        VariableElement paramElt = TreeUtils.elementFromDeclaration(paramsOfEnclosingMethod.get(j));
        for (ResourceAlias rl : argAliases) {
          Element argAliasElt = rl.reference.getElement();
          if (!argAliasElt.equals(paramElt)) {
            continue;
          }
          if (typeFactory.hasOwning(parameters.get(i))) {
            addOwningOnParams(j);
            break;
          }
        }
      }
    }
  }

  private void checkReceiverOfMethodCall(
      Set<Obligation> obligations,
      MethodInvocationNode mNode,
      List<? extends VariableTree> paramsOfEnclosingMethod) {

    Node receiver = mNode.getTarget().getReceiver();
    if (receiver.getTree() == null) {
      return;
    }

    Element receiverEl = TreeUtils.elementFromTree(receiver.getTree());
    if (receiverEl != null) {
      if (receiverEl.getKind().isField()) {
        isOwningField(receiver, mNode);
        return;
      }
    }

    Node receiverTmpVar = mcca.getTempVarOrNode(receiver);
    if (!(receiverTmpVar instanceof LocalVariableNode)) {
      return;
    }

    Obligation receiverObligation =
        MustCallConsistencyAnalyzer.getObligationForVar(
            obligations, (LocalVariableNode) receiverTmpVar);
    if (receiverObligation == null) {
      return;
    }

    Set<ResourceAlias> receiverAliases = receiverObligation.resourceAliases;
    for (int i = 0; i < paramsOfEnclosingMethod.size(); i++) {
      VariableElement paramElt = TreeUtils.elementFromDeclaration(paramsOfEnclosingMethod.get(i));

      for (ResourceAlias resourceAlias : receiverAliases) {
        Element resourceElt = resourceAlias.reference.getElement();
        if (!resourceElt.equals(paramElt)) {
          continue;
        }

        JavaExpression target = JavaExpression.fromVariableTree(paramsOfEnclosingMethod.get(i));
        if (mustCallObligationSatisfied(mNode, paramElt, target)) {
          addOwningOnParams(i);
          break;
        }
      }
    }
  }

  private void checkIndirectCalls(
      Set<Obligation> obligations,
      MethodInvocationNode mNode,
      List<? extends VariableTree> paramsOfEnclosingMethod) {

    List<Node> arguments = mcca.getArgumentsOfInvocation(mNode);
    List<? extends VariableElement> parameters = mcca.getParametersOfInvocation(mNode);
    if (parameters.isEmpty()) {
      return;
    }

    // TODO check this again
    for (int i = 0; i < arguments.size(); i++) {
      Node arg = NodeUtils.removeCasts(arguments.get(i));
      Element argElt = TreeUtils.elementFromTree(arg.getTree());
      if (argElt == null && arg instanceof ArrayCreationNode) {
        ArrayCreationNode arrayCreationNode = (ArrayCreationNode) arg;
        List<Node> arrays = arrayCreationNode.getInitializers();
        for (Node n : arrays) {
          Element nElt = TreeUtils.elementFromTree(n.getTree());
          if (nElt == null) {
            continue;
          }
          if (nElt.getKind().isField()) {
            isOwningField(n, mNode);
          }
        }
      } else if (argElt != null && argElt.getKind().isField()) {
        isOwningField(arg, mNode);
      }

      Set<ResourceAlias> argAliases = getResourceAliasOfArgument(obligations, arg);
      for (int j = 0; j < paramsOfEnclosingMethod.size(); j++) {
        VariableElement paramElt = TreeUtils.elementFromDeclaration(paramsOfEnclosingMethod.get(i));
        for (ResourceAlias rl : argAliases) {
          Element argAliasElt = rl.reference.getElement();
          if (!argAliasElt.equals(paramElt)) {
            continue;
          }
          JavaExpression target = JavaExpression.fromVariableTree(paramsOfEnclosingMethod.get(i));
          if (mustCallObligationSatisfied(mNode, paramElt, target)) {
            addOwningOnParams(i);
          }
        }
      }
    }
  }

  private Set<ResourceAlias> getResourceAliasOfArgument(Set<Obligation> obligations, Node arg) {
    Node tempVar = mcca.getTempVarOrNode(arg);
    if (!(tempVar instanceof LocalVariableNode)) {
      return Collections.emptySet();
    }

    Obligation argumentObligation =
        MustCallConsistencyAnalyzer.getObligationForVar(obligations, (LocalVariableNode) tempVar);
    if (argumentObligation == null) {
      return Collections.emptySet();
    }
    return argumentObligation.resourceAliases;
  }

  /**
   * If the receiver of {@code mNode} is a candidate owning field and the method invocation
   * satisfies the field's must-call obligation, then adds that field to the {@link
   * #owningFieldToECM} set.
   *
   * @param mNode the MethodInvocationNode
   */
  private void checkMethodInvocation(Set<Obligation> obligations, MethodInvocationNode mNode) {
    if (enMethodElt == null) {
      return;
    }

    List<? extends VariableTree> paramsOfEnclosingMethod = enMethodTree.getParameters();

    checkArgsOfMethodCall(obligations, mNode, paramsOfEnclosingMethod);
    checkReceiverOfMethodCall(obligations, mNode, paramsOfEnclosingMethod);
    checkIndirectCalls(obligations, mNode, paramsOfEnclosingMethod);
  }

  // TODO replace with checkMustCall in mcca
  private boolean mustCallObligationSatisfied(
      MethodInvocationNode mNode, Element varElt, JavaExpression target) {

    List<String> mustCallValues = typeFactory.getMustCallValue(varElt);
    if (mustCallValues.size() != 1) {
      // TODO: generalize this to MustCall annotations with more than one element.
      return false;
    }

    CFStore cmStoreAfter = typeFactory.getStoreAfter(mNode);
    CFValue cmValue = cmStoreAfter == null ? null : cmStoreAfter.getValue(target);
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

    AnnotationMirror cmAnnoForMustCallMethods =
        typeFactory.createCalledMethods(mustCallValues.toArray(new String[0]));
    if (!mustCallValues.isEmpty()
        && typeFactory.getQualifierHierarchy().isSubtype(cmAnno, cmAnnoForMustCallMethods)) {
      return true;
    }
    return false;
  }

  // probably replace with updateObligationsWithInvocationResult in mcca TODO
  private void updateMethodInvocationOrObjectCreationNode(Set<Obligation> obligations, Node node) {
    List<Node> arguments = mcca.getArgumentsOfInvocation(node);
    List<? extends VariableElement> parameters = mcca.getParametersOfInvocation(node);

    for (int i = 0; i < arguments.size(); i++) {
      Node tempVar = mcca.getTempVarOrNode(arguments.get(i));
      if (!(tempVar instanceof LocalVariableNode)) {
        continue;
      }

      Obligation obligation =
          MustCallConsistencyAnalyzer.getObligationForVar(obligations, (LocalVariableNode) tempVar);
      if (obligation == null) {
        continue;
      }

      Node method = mcca.getTempVarOrNode(node);
      if (typeFactory.hasMustCallAlias(parameters.get(i))) {
        Set<ResourceAlias> newResourceAliasesForObligation =
            new LinkedHashSet<>(obligation.resourceAliases);
        newResourceAliasesForObligation.add(
            new ResourceAlias(new LocalVariable((LocalVariableNode) method), node.getTree()));
        obligations.remove(obligation);
        obligations.add(new Obligation(newResourceAliasesForObligation));
      }
    }
  }

  /**
   * Updates {@code worklist} with the next block along all paths to the regular exit point. If the
   * next block is a regular exit point, adds an {@literal @}Owning annotation for fields in {@link
   * #owningFieldToECM}.
   *
   * @param curBlock the current block
   * @param visited set of blocks already on the worklist
   * @param worklist current worklist
   */
  private void propagateRegPaths(
      Set<Obligation> obligations,
      Block curBlock,
      Set<BlockWithObligations> visited,
      Deque<BlockWithObligations> worklist) {

    List<Block> successors = getNormalSuccessors(curBlock);

    for (Block successor : successors) {
      // If b is a special block, it must be the regular exit, since we do not propagate to
      // exceptional successors.
      if (successor.getType() == Block.BlockType.SPECIAL_BLOCK) {
        WholeProgramInference wpi = typeFactory.getWholeProgramInference();

        assert wpi != null : "MustCallInference is running without WPI.";
        for (VariableElement fieldElt : getEnclosedOwningFields()) {
          wpi.addFieldDeclarationAnnotation(fieldElt, OWNING);
        }
        if (!owningFieldToECM.isEmpty()) {
          addEnsuresCalledMethods();
        }

        addOrUpdateMustCall();
      }
      BlockWithObligations state = new BlockWithObligations(successor, obligations);
      if (visited.add(state)) {
        worklist.add(state);
      }
    }
  }

  /**
   * Returns the non-exceptional successors of the current block.
   *
   * @param cur the current block
   * @return the successors of this current block
   */
  private List<Block> getNormalSuccessors(Block cur) {
    List<Block> successorBlock = new ArrayList<>();

    if (cur.getType() == Block.BlockType.CONDITIONAL_BLOCK) {

      ConditionalBlock ccur = (ConditionalBlock) cur;

      successorBlock.add(ccur.getThenSuccessor());
      successorBlock.add(ccur.getElseSuccessor());

    } else {
      if (!(cur instanceof SingleSuccessorBlock)) {
        throw new BugInCF("BlockImpl is neither a conditional block nor a SingleSuccessorBlock");
      }

      Block b = ((SingleSuccessorBlock) cur).getSuccessor();
      if (b != null) {
        successorBlock.add(b);
      }
    }
    return successorBlock;
  }
}
