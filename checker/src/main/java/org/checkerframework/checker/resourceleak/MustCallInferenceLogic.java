package org.checkerframework.checker.resourceleak;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer.BlockWithObligations;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer.Obligation;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer.ResourceAlias;
import org.checkerframework.common.accumulation.AccumulationStore;
import org.checkerframework.common.accumulation.AccumulationValue;
import org.checkerframework.common.wholeprograminference.WholeProgramInference;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * This class implements the annotation inference algorithm for the Resource Leak Checker. It is
 * responsible for inferring annotations such as @Owning on owning fields and
 * parameters, @EnsuresCalledMethods on methods, and @InheritableMustCall on class declarations.
 * Each instance of this class corresponds to a single control flow graph (CFG), typically
 * representing a method. The algorithm determines if the @MustCall obligation of a field is
 * fulfilled along some path leading to the regular exit point of the method. If the obligation is
 * satisfied, it adds an @Owning annotation on the field and an @EnsuresCalledMethods annotation on
 * the method being analyzed by this instance. Additionally, if the method being analyzed fulfills
 * the must-call obligation of all the enclosed owning fields, it adds a @InheritableMustCall
 * annotation on the enclosing class.
 *
 * <p>Note: This class makes the assumption that the must-call set has only one element. This
 * limitation should be taken into account while using the class. Must-call sets with more than one
 * element may be supported in the future.
 *
 * @see <a
 *     href="https://checkerframework.org/manual/#resource-leak-checker-inference-algo">Automatic
 *     Inference of Resource Leak Specifications</a>
 */
public class MustCallInferenceLogic {

  /**
   * The set of owning fields that have been inferred to be released within the CFG currently under
   * analysis.
   */
  private final Set<VariableElement> owningFieldToECM = new HashSet<>();

  /**
   * The type factory for the Resource Leak Checker, which is used to access the Must Call Checker.
   */
  private final ResourceLeakAnnotatedTypeFactory typeFactory;

  /** The {@link Owning} annotation. */
  protected final AnnotationMirror OWNING;

  /**
   * The control flow graph of the current method. There is a separate MustCallInferenceLogic for
   * each method.
   */
  private final ControlFlowGraph cfg;

  /** The MustCallConsistencyAnalyzer. */
  private final MustCallConsistencyAnalyzer mcca;

  /** The MethodTree of the current method. */
  private final MethodTree enclosingMethodTree;

  /** The element for the current method. */
  private final ExecutableElement enclosingMethodElt;

  /**
   * Creates a MustCallInferenceLogic instance.
   *
   * @param typeFactory the type factory
   * @param cfg the control flow graph of the method to check
   * @param mcca the MustCallConsistencyAnalyzer
   */
  /*package-private*/ MustCallInferenceLogic(
      ResourceLeakAnnotatedTypeFactory typeFactory,
      ControlFlowGraph cfg,
      MustCallConsistencyAnalyzer mcca) {
    this.typeFactory = typeFactory;
    this.mcca = mcca;
    this.cfg = cfg;
    OWNING = AnnotationBuilder.fromClass(this.typeFactory.getElementUtils(), Owning.class);
    enclosingMethodTree = ((UnderlyingAST.CFGMethod) cfg.getUnderlyingAST()).getMethod();
    enclosingMethodElt = TreeUtils.elementFromDeclaration(enclosingMethodTree);
  }

  /**
   * Creates a MustCallInferenceLogic instance, runs the inference algorithm. If the type factory
   * has whole program inference enabled, its postAnalyze method should execute the inference
   * algorithm using this method.
   *
   * @param typeFactory the type factory
   * @param cfg the control flow graph of the method to check
   * @param mcca the MustCallConsistencyAnalyzer
   */
  protected static void runMustCallInferenceLogic(
      ResourceLeakAnnotatedTypeFactory typeFactory,
      ControlFlowGraph cfg,
      MustCallConsistencyAnalyzer mcca) {
    MustCallInferenceLogic mustCallInferenceLogic =
        new MustCallInferenceLogic(typeFactory, cfg, mcca);
    mustCallInferenceLogic.runInference();
  }

  /**
   * Runs the inference algorithm on the contents of the {@link #cfg} field and parameters.
   *
   * <p>Operationally, it checks method invocations for fields and parameters with
   * non-empty @MustCall obligations along all paths to the regular exit point in the method body of
   * the method represented by {@link #cfg}, and updates the {@link #owningFieldToECM} set or
   * adds @Owning to the formal parameter if it discovers their must-call obligations were satisfied
   * along one of the checked paths.
   */
  private void runInference() {

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
          // This call will not induce any side effects in the result of RLC, as the inference takes
          // place within the postAnalyze method of the ResourceLeakAnnotatedTypeFactory, once the
          // consistency analyzer is finished.
          mcca.updateObligationsWithInvocationResult(obligations, node);
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

  /**
   * Returns a set of obligations representing the non-empty MustCall parameters of the method that
   * corresponds to the given cfg, or an empty set if the given CFG doesn't correspond to a method
   * body.
   *
   * @param cfg the control flow graph of the method to check
   * @return a set of obligations representing the non-empty MustCall parameters of the method
   *     corresponding to a CFG.
   */
  private Set<Obligation> getNonEmptyMCParams(ControlFlowGraph cfg) {
    // TODO what about lambdas?
    if (cfg.getUnderlyingAST().getKind() != UnderlyingAST.Kind.METHOD) {
      return Collections.emptySet();
    }
    Set<Obligation> result = new LinkedHashSet<>(1);
    for (VariableTree param : enclosingMethodTree.getParameters()) {
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
   * This function returns a set of all owning fields that have been inferred in the current or any
   * previous iteration
   *
   * @return set of owning fields
   */
  private Set<VariableElement> getEnclosedOwningFields() {
    ClassTree classTree = TreePathUtil.enclosingClass(typeFactory.getPath(enclosingMethodTree));
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
    wpi.addDeclarationAnnotationToFormalParameter(enclosingMethodElt, index, OWNING);
  }

  /**
   * Infers @Owning annotation for the given node if it is a field and its must-call obligation is
   * fulfilled via the given method call. If so, it adds the node to the owningFieldToECM set.
   *
   * @param node the possible owning field
   * @param mNode method invoked on the possible owning field
   */
  private void inferOwningField(@Nullable Node node, MethodInvocationNode mNode) {
    if (node == null) {
      return;
    }
    Element nodeElt = TreeUtils.elementFromTree(node.getTree());
    if (nodeElt == null || !nodeElt.getKind().isField()) {
      return;
    }
    if (typeFactory.isCandidateOwningField(nodeElt)) {
      node = NodeUtils.removeCasts(node);
      JavaExpression target = JavaExpression.fromNode(node);
      if (mustCallObligationSatisfied(mNode, nodeElt, target)) {
        // This assumes that any MustCall annotation has at most one element.
        // TODO: generalize this to MustCall annotations with more than one element.
        owningFieldToECM.add((VariableElement) nodeElt);
      }
    }
  }

  /**
   * Updates a set of obligations based on an assignment statement node. If the left-hand side of
   * the assignment is a field that is an "enclosed owning field", adds the owning field to the
   * method's parameters if its alias is assigned to the field. If the left-hand side of the
   * assignment is a resource variable and the right-hand side is a must-call-close method call,
   * adds the owning resource to the method's parameters if its alias is assigned to the resource
   * variable. Otherwise, updates the obligations based on the assignment.
   *
   * @param obligations The set of obligations to update.
   * @param assignmentNode The assignment statement node.
   */
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

      if (!TreeUtils.isConstructor(enclosingMethodTree)) {
        owningFieldToECM.remove((VariableElement) lhsElement);
      }
      addOwningToParamsIfDisposedAtAssignment(obligations, rhsObligation, rhs);

    } else if (lhsElement.getKind() == ElementKind.RESOURCE_VARIABLE && mcca.isMustCallClose(rhs)) {
      addOwningToParamsIfDisposedAtAssignment(obligations, rhsObligation, rhs);
    } else if (lhs instanceof LocalVariableNode) {
      LocalVariableNode lhsVar = (LocalVariableNode) lhs;
      mcca.updateObligationsForPseudoAssignment(obligations, assignmentNode, lhsVar, rhs);
    }
  }

  /**
   * Adds owning fields to the method's parameters, if the must-call obligation of its alias
   * referred to by the right-hand side of the assignment is fulfilled during the assignment.
   *
   * @param obligations The set of obligations to update.
   * @param rhsObligation The obligation associated with the right-hand side of the assignment.
   * @param rhs The right-hand side of the assignment.
   */
  private void addOwningToParamsIfDisposedAtAssignment(
      Set<Obligation> obligations, Obligation rhsObligation, Node rhs) {
    Set<ResourceAlias> rhsAliases = rhsObligation.resourceAliases;
    for (ResourceAlias rl : rhsAliases) {
      Element rhsElt = rl.reference.getElement();
      List<? extends VariableTree> params = enclosingMethodTree.getParameters();
      for (int i = 0; i < params.size(); i++) {
        VariableElement paramElt = TreeUtils.elementFromDeclaration(params.get(i));
        if (paramElt.equals(rhsElt)) {
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
    Map<String, Set<String>> methodToFields = new LinkedHashMap<>();
    for (VariableElement owningField : owningFieldToECM) {
      List<String> mustCallValues = typeFactory.getMustCallValue(owningField);
      assert !mustCallValues.isEmpty() : "Must-call obligation of an owning field is deleted.";
      assert mustCallValues.size() == 1 : "The size of the must-call set is greater than one.";
      // Assume must-call set has one element
      String key = mustCallValues.get(0);
      String value = "this." + owningField.getSimpleName().toString();

      methodToFields.computeIfAbsent(key, k -> new HashSet<>()).add(value);
    }

    for (String mustCallValue : methodToFields.keySet()) {
      AnnotationBuilder builder =
          new AnnotationBuilder(typeFactory.getProcessingEnv(), EnsuresCalledMethods.class);
      builder.setValue("value", methodToFields.get(mustCallValue).toArray());
      builder.setValue("methods", new String[] {mustCallValue});
      AnnotationMirror am = builder.build();
      WholeProgramInference wpi = typeFactory.getWholeProgramInference();
      wpi.addMethodDeclarationAnnotation(enclosingMethodElt, am);
    }
  }

  /**
   * Adds an InheritableMustCall annotation on the enclosing class. If the class already has a
   * non-empty MustCall type, if it's inherited from one of its superclasses, this method does
   * nothing to avoid infinite iteration. Otherwise, if the method being analyzed by {@code this} is
   * not private and satisfies must-call obligation of all the enclosed owning fields, it adds an
   * InheritableMustCall annotation to the enclosing class.
   */
  private void addOrUpdateMustCall() {
    ClassTree classTree = TreePathUtil.enclosingClass(typeFactory.getPath(enclosingMethodTree));
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
      String[] methodArray = new String[] {currentMustCallValues.get(0)};
      Arrays.sort(methodArray);
      builder.setValue("value", methodArray);
      wpi.addClassDeclarationAnnotation(typeElement, builder.build());
      return;
    }

    // if the enclosing method is not private and satisfies the must-call obligation of all owning
    // fields, add an InheritableMustCall annotation with the name of this method
    if (!enclosingMethodTree.getModifiers().getFlags().contains(Modifier.PRIVATE)) {
      if (!owningFieldToECM.isEmpty()
          && owningFieldToECM.size() == getEnclosedOwningFields().size()) {
        AnnotationBuilder builder =
            new AnnotationBuilder(typeFactory.getProcessingEnv(), InheritableMustCall.class);
        String[] methodArray = new String[] {enclosingMethodTree.getName().toString()};
        Arrays.sort(methodArray);
        builder.setValue("value", methodArray);
        wpi.addClassDeclarationAnnotation(typeElement, builder.build());
      }
    }
  }

  /**
   * Checks if the receiver of a method call has an obligation that is satisfied by the method
   * invocation node. If the receiver is a field, check if it is an owning field, if the receiver is
   * resource alias with any parameter of the enclosing method, add owning to that parameter.
   *
   * @param obligations Set of obligations associated with the current code block.
   * @param mNode Method invocation node to check.
   * @param paramsOfEnclosingMethod List of parameters of the enclosing method.
   */
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
        inferOwningField(receiver, mNode);
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

  /**
   * Checks the arguments of a method invocation to see if any of them is passed as an owning
   * parameter. If so, it adds owning to the corresponding parameters of the enclosing method.
   *
   * @param obligations Set of obligations associated with the current code block.
   * @param mNode The method invocation node to check.
   * @param paramsOfEnclosingMethod List of parameters of the enclosing method.
   */
  private void checkArgsOfMethodCall(
      Set<Obligation> obligations,
      MethodInvocationNode mNode,
      List<? extends VariableTree> paramsOfEnclosingMethod) {
    List<Node> arguments = mcca.getArgumentsOfInvocation(mNode);
    List<? extends VariableElement> parameters = mcca.getParametersOfInvocation(mNode);
    if (parameters.isEmpty() || paramsOfEnclosingMethod.isEmpty()) {
      return;
    }

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
          if (argAliasElt.equals(paramElt)) {
            addOwningOnParams(j);
          }
        }
      }
    }
  }

  /**
   * Checks for indirect calls within the method represented by the given MethodInvocationNode. It
   * checks the called-methods set of each argument after the call to infer owning annotation for
   * the field or parameter passed as an argument to this call.
   *
   * @param obligations Set of obligations associated with the current code block.
   * @param mNode Method invocation node to check.
   * @param paramsOfEnclosingMethod a list of the parameters of the enclosing method
   */
  private void checkIndirectCalls(
      Set<Obligation> obligations,
      MethodInvocationNode mNode,
      List<? extends VariableTree> paramsOfEnclosingMethod) {

    List<Node> arguments = mcca.getArgumentsOfInvocation(mNode);
    List<? extends VariableElement> paramsOfInvocation = mcca.getParametersOfInvocation(mNode);

    if (paramsOfInvocation.isEmpty()) {
      return;
    }

    for (Node argument : arguments) {
      Node arg = NodeUtils.removeCasts(argument);

      Set<ResourceAlias> argAliases = getResourceAliasOfArgument(obligations, arg);
      // In the CFG, explicit passing of multiple arguments in the varargs position is represented
      // via an ArrayCreationNode. In this case, it checks the called methods set of each argument
      // passed in this position.
      if (arg instanceof ArrayCreationNode) {
        ArrayCreationNode varArgsNode = (ArrayCreationNode) arg;
        checkCalledMethodsSetForVarArgs(paramsOfEnclosingMethod, mNode, varArgsNode, argAliases);
      } else {
        Element varArgElt = TreeUtils.elementFromTree(arg.getTree());
        if (varArgElt != null && varArgElt.getKind().isField()) {
          inferOwningField(arg, mNode);
          continue;
        }
        checkCalledMethodsSetForArgAliases(paramsOfEnclosingMethod, mNode, argAliases);
      }
    }
  }

  /**
   * Checks each node passed in the var argument position. It checks the called-methods set of each
   * argument after the call to infer owning annotation for the field or parameter passed as an
   * argument to this call.
   *
   * @param paramsOfEnclosingMethod a list of the parameters of the enclosing method.
   * @param mNode the method invocation node to check.
   * @param varArgsNode the VarArg node of the given method invocation node.
   * @param argAliases the set of resource aliases associated with the argument node.
   */
  private void checkCalledMethodsSetForVarArgs(
      List<? extends VariableTree> paramsOfEnclosingMethod,
      MethodInvocationNode mNode,
      ArrayCreationNode varArgsNode,
      Set<ResourceAlias> argAliases) {

    for (Node argNode : varArgsNode.getInitializers()) {
      Element varArgElt = TreeUtils.elementFromTree(argNode.getTree());

      if (varArgElt == null) {
        continue;
      }

      if (varArgElt.getKind().isField()) {
        inferOwningField(argNode, mNode);
      } else {
        checkCalledMethodsSetForArgAliases(paramsOfEnclosingMethod, mNode, argAliases);
      }
    }
  }

  /**
   * It checks if any of the parameters of the enclosing method are aliased with the argument passed
   * to the method invocation. It so, it checks the set of called methods for the parameter after
   * the call, in order to infer the owning annotation for that parameter.
   *
   * @param paramsOfEnclosingMethod a list of the parameters of the enclosing method.
   * @param mNode the method invocation node to check.
   * @param argAliases the set of resource aliases associated with the argument node.
   */
  private void checkCalledMethodsSetForArgAliases(
      List<? extends VariableTree> paramsOfEnclosingMethod,
      MethodInvocationNode mNode,
      Set<ResourceAlias> argAliases) {

    for (int i = 0; i < paramsOfEnclosingMethod.size(); i++) {

      VariableTree encParamTree = paramsOfEnclosingMethod.get(i);
      for (ResourceAlias rl : argAliases) {
        Element argAliasElt = rl.reference.getElement();
        VariableElement encParamElt = TreeUtils.elementFromDeclaration(encParamTree);
        if (!argAliasElt.equals(encParamElt)) {
          continue;
        }

        JavaExpression target = JavaExpression.fromVariableTree(encParamTree);
        if (mustCallObligationSatisfied(mNode, encParamElt, target)) {
          addOwningOnParams(i);
          break;
        }
      }
    }
  }

  /**
   * Returns the set of resource aliases associated with the given argument node, by looking up the
   * corresponding obligation in the set of obligations passed as an argument.
   *
   * @param obligations the set of obligations to search in
   * @param arg the argument node whose corresponding resource aliases are to be returned
   * @return the set of resource aliases associated with the given argument node, or an empty set if
   *     the node does not
   */
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
   * This method performs three checks related to method invocation node and compute @Owning
   * annotations to the enclosing formal parameter or fields:
   *
   * <ul>
   *   <li>It calls {@link #checkArgsOfMethodCall} to inspect the arguments of a method invocation
   *       and identify if any of them are passed as an owning parameter. If found, it adds the
   *       “owning” annotation to the corresponding parameters of the enclosing method.
   *   <li>It calls {@link #checkReceiverOfMethodCall} to verify if the receiver of the method
   *       represented by {@code mNode} qualifies as a candidate owning field, and if the method
   *       invocation satisfies the field's must-call obligation. If these conditions are met, the
   *       field is added to the {@link #owningFieldToECM} set.
   *   <li>It calls {@link #checkIndirectCalls} to inspect the method represented by the given
   *       MethodInvocationNode for any indirect calls within it. The method analyzes the
   *       called-methods set of each argument after the call and computes the @Owning annotation to
   *       the field or parameter passed as an argument to this call.
   * </ul>
   *
   * @param obligations the set of obligations to search in
   * @param mNode the MethodInvocationNode
   */
  private void checkMethodInvocation(Set<Obligation> obligations, MethodInvocationNode mNode) {
    if (enclosingMethodElt != null) {
      List<? extends VariableTree> paramsOfEnclosingMethod = enclosingMethodTree.getParameters();
      checkArgsOfMethodCall(obligations, mNode, paramsOfEnclosingMethod);
      checkReceiverOfMethodCall(obligations, mNode, paramsOfEnclosingMethod);
      checkIndirectCalls(obligations, mNode, paramsOfEnclosingMethod);
    }
  }

  /**
   * Checks if a MustCall obligation of the element is satisfied via the given method call. A
   * MustCall obligation of an element is satisfied if the called-methods set contains the target of
   * its must-call obligation.
   *
   * @param mNode The method invocation node being checked for satisfaction of the MustCall
   *     obligation.
   * @param varElt The element representing the variable annotated with the MustCall annotation.
   * @param target The target of the MustCall obligation, represented as a JavaExpression.
   * @return {@code true} if the MustCall obligation is satisfied, {@code false} otherwise.
   */
  private boolean mustCallObligationSatisfied(
      MethodInvocationNode mNode, Element varElt, JavaExpression target) {

    List<String> mustCallValues = typeFactory.getMustCallValue(varElt);
    if (mustCallValues.size() != 1) {
      // TODO: generalize this to MustCall annotations with more than one element.
      return false;
    }

    AccumulationStore cmStoreAfter = typeFactory.getStoreAfter(mNode);
    @Nullable AccumulationValue cmValue =
        cmStoreAfter == null ? null : cmStoreAfter.getValue(target);
    AnnotationMirror cmAnno = null;

    if (cmValue != null) { // When store contains the lhs
      Set<String> accumulatedValues = cmValue.getAccumulatedValues();
      if (accumulatedValues != null) { // type variable or wildcard type
        cmAnno = typeFactory.createCalledMethods(accumulatedValues.toArray(new String[0]));
      } else {
        for (AnnotationMirror anno : cmValue.getAnnotations()) {
          if (AnnotationUtils.areSameByName(
              anno, "org.checkerframework.checker.calledmethods.qual.CalledMethods")) {
            cmAnno = anno;
          }
        }
      }
    }

    if (cmAnno == null) {
      cmAnno = typeFactory.top;
    }

    return mcca.calledMethodsSatisfyMustCall(mustCallValues, cmAnno);
  }

  /**
   * Updates {@code worklist} with the next block along all paths to the regular exit point. If the
   * next block is a regular exit point, adds an {@literal @}Owning annotation for fields in {@link
   * #owningFieldToECM}.
   *
   * @param obligations the set of obligations to update
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

    for (Block b : successors) {
      // If b is a special block, it must be the regular exit, since it does not propagate to
      // exceptional successors.
      if (b.getType() == Block.BlockType.SPECIAL_BLOCK) {
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
      BlockWithObligations state = new BlockWithObligations(b, obligations);
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
