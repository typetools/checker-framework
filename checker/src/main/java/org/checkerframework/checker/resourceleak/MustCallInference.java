package org.checkerframework.checker.resourceleak;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayDeque;
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
 * responsible for inferring annotations such as {@code @}{@link Owning} on owning fields and
 * parameters, @EnsuresCalledMethods on methods, and @InheritableMustCall on class declarations.
 *
 * <p>Each instance of this class corresponds to a single control flow graph (CFG), typically
 * representing a method.
 *
 * <p>The algorithm determines if the @MustCall obligation of a field is fulfilled along some path
 * leading to the regular exit point of the method. If the obligation is satisfied, it adds
 * an @Owning annotation on the field and an @EnsuresCalledMethods annotation on the method being
 * analyzed by this instance. Additionally, if the method being analyzed fulfills the must-call
 * obligation of all the enclosed owning fields, it adds a @InheritableMustCall annotation on the
 * enclosing class.
 *
 * <p>Note: This class makes the assumption that the must-call set has only one element. Must-call
 * sets with more than one element may be supported in the future.
 *
 * @see <a
 *     href="https://checkerframework.org/manual/#resource-leak-checker-inference-algo">Automatic
 *     Inference of Resource Leak Specifications</a>
 */
public class MustCallInference {

  /**
   * The fields that have been inferred to be released within the CFG currently under analysis. All
   * of these fields will be given an @Owning annotation.
   */
  private final Set<VariableElement> releasedFields = new HashSet<>();

  /**
   * The type factory for the Resource Leak Checker, which is used to access the Must Call Checker.
   */
  private final ResourceLeakAnnotatedTypeFactory typeFactory;

  /** The MustCallConsistencyAnalyzer. */
  private final MustCallConsistencyAnalyzer mcca;

  /** The {@link Owning} annotation. */
  protected final AnnotationMirror OWNING;

  /**
   * The control flow graph of the current method. There is a separate MustCallInference for each
   * method.
   */
  private final ControlFlowGraph cfg;

  /** The MethodTree of the current method. */
  private final MethodTree methodTree;

  /** The element for the current method. */
  private final ExecutableElement methodElt;

  /**
   * Creates a MustCallInference instance.
   *
   * @param typeFactory the type factory
   * @param cfg the control flow graph of the method to check
   * @param mcca the MustCallConsistencyAnalyzer
   */
  /*package-private*/ MustCallInference(
      ResourceLeakAnnotatedTypeFactory typeFactory,
      ControlFlowGraph cfg,
      MustCallConsistencyAnalyzer mcca) {
    this.typeFactory = typeFactory;
    this.mcca = mcca;
    this.cfg = cfg;
    OWNING = AnnotationBuilder.fromClass(this.typeFactory.getElementUtils(), Owning.class);
    methodTree = ((UnderlyingAST.CFGMethod) cfg.getUnderlyingAST()).getMethod();
    methodElt = TreeUtils.elementFromDeclaration(methodTree);
  }

  /**
   * Creates a MustCallInference instance and runs the inference algorithm. A type factory's
   * postAnalyze method calls this, if Whole Program Inference is enabled.
   *
   * @param typeFactory the type factory
   * @param cfg the control flow graph of the method to check
   * @param mcca the MustCallConsistencyAnalyzer
   */
  protected static void runMustCallInference(
      ResourceLeakAnnotatedTypeFactory typeFactory,
      ControlFlowGraph cfg,
      MustCallConsistencyAnalyzer mcca) {
    MustCallInference mustCallInferenceLogic = new MustCallInference(typeFactory, cfg, mcca);
    mustCallInferenceLogic.runInference();
  }

  /**
   * Runs the inference algorithm on the current method (the {@link #cfg} field). It updates the
   * {@link #releasedFields} set or adds @Owning to the formal parameter if it discovers their
   * must-call obligations were satisfied along one of the checked paths.
   *
   * <p>Operationally, it checks method invocations for fields and parameters with
   * non-empty @MustCall obligations along all paths to the regular exit point in the method body of
   * the method represented by {@link #cfg}.
   */
  private void runInference() {

    Set<BlockWithObligations> visited = new HashSet<>();
    Deque<BlockWithObligations> worklist = new ArrayDeque<>();

    {
      BlockWithObligations entry =
          new BlockWithObligations(cfg.getEntryBlock(), getNonEmptyMCParams(cfg));
      worklist.add(entry);
      visited.add(entry);
    }

    while (!worklist.isEmpty()) {
      BlockWithObligations current = worklist.remove();

      Set<Obligation> obligations = new LinkedHashSet<>(current.obligations);

      for (Node node : current.block.getNodes()) {
        // Calling updateObligationsWithInvocationResult() will not induce any side effects in the
        // result of RLC, as the inference takes place within the postAnalyze method of the
        // ResourceLeakAnnotatedTypeFactory, once the consistency analyzer is finished.
        if (node instanceof MethodInvocationNode) {
          mcca.updateObligationsWithInvocationResult(obligations, node);
          checkMethodInvocation(obligations, (MethodInvocationNode) node);
        } else if (node instanceof ObjectCreationNode) {
          mcca.updateObligationsWithInvocationResult(obligations, node);
        } else if (node instanceof AssignmentNode) {
          checkAssignment(obligations, (AssignmentNode) node);
        }
      }

      discoverNonExceptionalSuccessors(obligations, current.block, visited, worklist);
    }
  }

  /**
   * Returns a set of obligations representing the non-empty MustCall parameters of the current
   * method. Returns an empty set if the given CFG doesn't correspond to a method body.
   *
   * @param cfg the control flow graph of the method to check
   * @return a set of obligations representing the non-empty MustCall parameters of the method
   *     corresponding to {@code cfg}
   */
  private Set<Obligation> getNonEmptyMCParams(ControlFlowGraph cfg) {
    // TODO what about lambdas?
    if (cfg.getUnderlyingAST().getKind() != UnderlyingAST.Kind.METHOD) {
      return Collections.emptySet();
    }
    Set<Obligation> result = new LinkedHashSet<>(1);
    for (VariableTree param : methodTree.getParameters()) {
      if (typeFactory.declaredTypeHasMustCall(param)) {
        VariableElement paramElement = TreeUtils.elementFromDeclaration(param);
        result.add(
            new Obligation(
                ImmutableSet.of(new ResourceAlias(new LocalVariable(paramElement), param))));
      }
    }
    return result;
  }

  /**
   * Returns all fields within the enclosing class that have been either inferred as owning or
   * annotated with the {@code @Owning} annotation.
   *
   * @return the owning fields
   */
  private Set<VariableElement> getOwningFields() {
    ClassTree classTree = TreePathUtil.enclosingClass(typeFactory.getPath(methodTree));
    TypeElement classElt = TreeUtils.elementFromDeclaration(classTree);
    Set<VariableElement> enOwningFields = new HashSet<>(releasedFields);
    for (Element memberElt : classElt.getEnclosedElements()) {
      if (memberElt.getKind().isField() && typeFactory.hasOwning(memberElt)) {
        enOwningFields.add((VariableElement) memberElt);
      }
    }
    return enOwningFields;
  }

  /**
   * Given an index, adds an owning annotation to the parameter at the specified index.
   *
   * @param index index of the current method's parameter (0-indexed)
   */
  private void addOwningToParam(int index) {
    WholeProgramInference wpi = typeFactory.getWholeProgramInference();
    wpi.addDeclarationAnnotationToFormalParameter(methodElt, index, OWNING);
  }

  /**
   * Adds the node to the releasedFields set if it is a field and its must-call obligation is
   * fulfilled via the given method call. If so, it will be given an @Owning annotation later.
   *
   * @param node possibly an owning field
   * @param invocation method invoked on the possible owning field
   */
  private void inferOwningField(@Nullable Node node, MethodInvocationNode invocation) {
    if (node == null) {
      return;
    }
    Element nodeElt = TreeUtils.elementFromTree(node.getTree());
    if (nodeElt == null || !nodeElt.getKind().isField()) {
      return;
    }
    if (typeFactory.isCandidateOwningField(nodeElt)) {
      node = NodeUtils.removeCasts(node);
      JavaExpression nodeJe = JavaExpression.fromNode(node);
      if (mustCallObligationSatisfied(invocation, nodeElt, nodeJe)) {
        // This assumes that any MustCall annotation has at most one element.
        // TODO: generalize this to MustCall annotations with more than one element.
        releasedFields.add((VariableElement) nodeElt);
      }
    }
  }

  /**
   * Analyzes an assignment statement node and performs three computations:
   *
   * <ul>
   *   <li>If the left-hand side of the assignment is an owning field, and the rhs is an alias of a
   *       formal parameter, it adds the {@code @Owning} annotation to the formal parameter.
   *   <li>If the left-hand side of the assignment is a resource variable, and the right-hand side
   *       is a must-call-close method call, and an alias of a formal parameter, it adds the
   *       {@code @Owning} annotation to the formal parameter.
   *   <li>Otherwise, updates the set of tracked obligations to account for the (pseudo-)assignment
   *       to some variable, as in a gen-kill dataflow analysis problem.
   * </ul>
   *
   * @param obligations the set of obligations to update
   * @param assignmentNode the assignment statement node
   */
  private void checkAssignment(Set<Obligation> obligations, AssignmentNode assignmentNode) {
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
      if (!getOwningFields().contains(lhsElement)) {
        return;
      }

      if (!TreeUtils.isConstructor(methodTree)) {
        releasedFields.remove((VariableElement) lhsElement);
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
   * Adds @Owning to method parameter p, if the must-call obligation of some alias of p is fulfilled
   * during the assignment.
   *
   * @param obligations the set of obligations to update
   * @param rhsObligation the obligation associated with the right-hand side of the assignment
   * @param rhs the right-hand side of the assignment
   */
  private void addOwningToParamsIfDisposedAtAssignment(
      Set<Obligation> obligations, Obligation rhsObligation, Node rhs) {
    Set<ResourceAlias> rhsAliases = rhsObligation.resourceAliases;
    for (ResourceAlias rhsAlias : rhsAliases) {
      Element rhsElt = rhsAlias.reference.getElement();
      List<? extends VariableTree> params = methodTree.getParameters();
      for (int i = 0; i < params.size(); i++) {
        VariableElement paramElt = TreeUtils.elementFromDeclaration(params.get(i));
        if (paramElt.equals(rhsElt)) {
          addOwningToParam(i);
          mcca.removeObligationsContainingVar(obligations, (LocalVariableNode) rhs);
          break;
        }
      }
    }
  }

  /**
   * Adds an {@link EnsuresCalledMethods} annotation to the current method for any owning field
   * whose must-call obligation is satisfied within the current method.
   */
  private void addEnsuresCalledMethods() {
    // This map is used to create @EnsuresCalledMethods annotation for fields with the same
    // must-call obligation on the method boundary. The keys are the must-call method names, and
    // the values are the set fields on which those methods are called
    Map<String, Set<String>> methodToFields = new LinkedHashMap<>();
    for (VariableElement releasedField : releasedFields) {
      List<String> mustCallValues = typeFactory.getMustCallValue(releasedField);
      assert !mustCallValues.isEmpty()
          : "Must-call obligation of owning field " + releasedField + " is deleted.";
      assert mustCallValues.size() == 1
          : "The must-call set ("
              + mustCallValues
              + ") of "
              + releasedField
              + "should be a singleton";
      // The assumption is that the must-call set has only one element
      String mustCallValue = mustCallValues.get(0);
      String fieldName = "this." + releasedField.getSimpleName().toString();

      methodToFields.computeIfAbsent(mustCallValue, k -> new HashSet<>()).add(fieldName);
    }

    for (String mustCallValue : methodToFields.keySet()) {
      AnnotationMirror am =
          createEnsuresCalledMethods(
              methodToFields.get(mustCallValue).toArray(new String[0]),
              new String[] {mustCallValue});
      WholeProgramInference wpi = typeFactory.getWholeProgramInference();
      wpi.addMethodDeclarationAnnotation(methodElt, am);
    }
  }

  /**
   * Possibly adds an InheritableMustCall annotation on the enclosing class. If the class already
   * has a non-empty MustCall type (which is inherited from one of its superclasses), this method
   * does nothing, in order to avoid infinite iteration. Otherwise, if the current method is not
   * private and satisfies the must-call obligations of all the owning fields, it adds an
   * InheritableMustCall annotation to the enclosing class.
   */
  private void addOrUpdateMustCall() {
    ClassTree classTree = TreePathUtil.enclosingClass(typeFactory.getPath(methodTree));
    TypeElement typeElement = TreeUtils.elementFromDeclaration(classTree);
    if (typeElement == null) {
      return;
    }

    WholeProgramInference wpi = typeFactory.getWholeProgramInference();
    List<String> currentMustCallValues = typeFactory.getMustCallValue(typeElement);
    if (!currentMustCallValues.isEmpty()) {
      // If the class already has a MustCall annotation which is inherited from a superclass,
      // do nothing.
      if (typeElement.getSuperclass() != null) {
        TypeMirror superType = typeElement.getSuperclass();
        TypeElement superTypeElement = TypesUtils.getTypeElement(superType);
        if (superTypeElement != null && !typeFactory.getMustCallValue(superTypeElement).isEmpty()) {
          return;
        }
      }
      // Add the current @MustCall annotation to guarantee the termination property. This is
      // necessary to avoid overwriting @MustCall annotations on the class declaration when there
      // are multiple methods that could fulfill the must-call obligation.
      AnnotationMirror am = createInheritableMustCall(new String[] {currentMustCallValues.get(0)});
      wpi.addClassDeclarationAnnotation(typeElement, am);
      return;
    }

    // If the current method is not private and satisfies the must-call obligation of all owning
    // fields, add an InheritableMustCall annotation with the name of this method.
    if (!methodTree.getModifiers().getFlags().contains(Modifier.PRIVATE)) {
      // Since the result of getOwningFields() is a superset of releasedFields, it is sufficient to
      // check the equality of their sizes to determine if both sets are equal.
      if (!releasedFields.isEmpty() && releasedFields.size() == getOwningFields().size()) {
        AnnotationMirror am =
            createInheritableMustCall(new String[] {methodTree.getName().toString()});
        wpi.addClassDeclarationAnnotation(typeElement, am);
      }
    }
  }

  /**
   * Checks whether a method invocation node satisfies the receiver's obligation. If the receiver is
   * a field, check if it is an owning field, if the receiver is resource alias with any parameter
   * of the current method, add owning to that parameter.
   *
   * @param obligations the obligations associated with the current code block
   * @param invocation the method invocation node to check
   */
  private void addOwningReceiverFromMethodCall(
      Set<Obligation> obligations, MethodInvocationNode invocation) {
    List<? extends VariableTree> paramsOfCurrentMethod = methodTree.getParameters();

    Node receiver = invocation.getTarget().getReceiver();
    if (receiver.getTree() == null) {
      return;
    }

    Element receiverEl = TreeUtils.elementFromTree(receiver.getTree());
    if (receiverEl != null) {
      if (receiverEl.getKind().isField()) {
        inferOwningField(receiver, invocation);
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
    for (int i = 0; i < paramsOfCurrentMethod.size(); i++) {
      VariableTree paramVarTree = paramsOfCurrentMethod.get(i);
      VariableElement paramElt = TreeUtils.elementFromDeclaration(paramVarTree);

      for (ResourceAlias resourceAlias : receiverAliases) {
        Element resourceElt = resourceAlias.reference.getElement();
        if (!resourceElt.equals(paramElt)) {
          continue;
        }

        JavaExpression paramJe = JavaExpression.fromVariableTree(paramVarTree);
        if (mustCallObligationSatisfied(invocation, paramElt, paramJe)) {
          addOwningToParam(i);
          break;
        }
      }
    }
  }

  /**
   * Checks the arguments of a method invocation to see if any of them is passed as an owning
   * parameter. If so, it adds owning to the corresponding parameters of the current method.
   *
   * @param obligations the obligations associated with the current code block
   * @param invocation a method invocation node that appears in the current method
   */
  private void addOwningParamsFromMethodCall(
      Set<Obligation> obligations, MethodInvocationNode invocation) {
    List<? extends VariableTree> paramsOfCurrentMethod = methodTree.getParameters();
    if (paramsOfCurrentMethod.isEmpty()) {
      return;
    }
    List<Node> arguments = mcca.getArgumentsOfInvocation(invocation);
    List<? extends VariableElement> invocationParams = mcca.getParametersOfInvocation(invocation);
    if (invocationParams.isEmpty()) {
      return;
    }

    for (int i = 0; i < arguments.size(); i++) {
      if (!typeFactory.hasOwning(invocationParams.get(i))) {
        continue;
      }

      Node arg = NodeUtils.removeCasts(arguments.get(i));

      Set<ResourceAlias> argAliases = getResourceAliasOfArgument(obligations, arg);
      for (int j = 0; j < paramsOfCurrentMethod.size(); j++) {
        VariableElement paramElt = TreeUtils.elementFromDeclaration(paramsOfCurrentMethod.get(j));
        for (ResourceAlias argAlias : argAliases) {
          Element argAliasElt = argAlias.reference.getElement();
          if (argAliasElt.equals(paramElt)) {
            addOwningToParam(j);
            break;
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
   * @param obligations Set of obligations associated with the current code block
   * @param invocation a method invocation node to check
   */
  private void checkIndirectCalls(Set<Obligation> obligations, MethodInvocationNode invocation) {
    List<? extends VariableTree> paramsOfCurrentMethod = methodTree.getParameters();

    List<Node> arguments = mcca.getArgumentsOfInvocation(invocation);
    List<? extends VariableElement> paramsOfInvocation = mcca.getParametersOfInvocation(invocation);

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
        checkCalledMethodsSetForVarArgs(paramsOfCurrentMethod, invocation, varArgsNode, argAliases);
      } else {
        Element varArgElt = TreeUtils.elementFromTree(arg.getTree());
        if (varArgElt != null && varArgElt.getKind().isField()) {
          inferOwningField(arg, invocation);
          continue;
        }
        checkCalledMethodsSetForArgAliases(paramsOfCurrentMethod, invocation, argAliases);
      }
    }
  }

  /**
   * Checks each node passed in the varargs argument position. It checks the called-methods set of
   * each argument after the call to infer owning annotation for the field or parameter passed as an
   * argument to this call.
   *
   * @param paramsOfCurrentMethod the parameters of the current method
   * @param invocation the method invocation node to check
   * @param varArgsNode the VarArg node of the given method invocation node
   * @param argAliases the resource aliases associated with the argument passed in the given {@code
   *     invocation}
   */
  private void checkCalledMethodsSetForVarArgs(
      List<? extends VariableTree> paramsOfCurrentMethod,
      MethodInvocationNode invocation,
      ArrayCreationNode varArgsNode,
      Set<ResourceAlias> argAliases) {

    for (Node varArgNode : varArgsNode.getInitializers()) {
      Element varArgElt = TreeUtils.elementFromTree(varArgNode.getTree());

      if (varArgElt == null) {
        continue;
      }

      if (varArgElt.getKind().isField()) {
        inferOwningField(varArgNode, invocation);
      } else {
        checkCalledMethodsSetForArgAliases(paramsOfCurrentMethod, invocation, argAliases);
      }
    }
  }

  /**
   * It checks if any of the parameters of the current method are aliased with the argument passed
   * to the method invocation. It so, it checks the set of called methods for the parameter after
   * the call, in order to infer the owning annotation for that parameter.
   *
   * @param paramsOfCurrentMethod the parameters of the current method
   * @param invocation a method invocation within the current method
   * @param argAliases the set of resource aliases associated with the argument passed in the given
   *     {@code invocation}
   */
  private void checkCalledMethodsSetForArgAliases(
      List<? extends VariableTree> paramsOfCurrentMethod,
      MethodInvocationNode invocation,
      Set<ResourceAlias> argAliases) {

    for (int i = 0; i < paramsOfCurrentMethod.size(); i++) {

      VariableTree currentMethodParamTree = paramsOfCurrentMethod.get(i);
      for (ResourceAlias argAlias : argAliases) {
        Element argAliasElt = argAlias.reference.getElement();
        VariableElement currentMethodParamElt =
            TreeUtils.elementFromDeclaration(currentMethodParamTree);
        if (!argAliasElt.equals(currentMethodParamElt)) {
          continue;
        }

        JavaExpression paramJe = JavaExpression.fromVariableTree(currentMethodParamTree);
        if (mustCallObligationSatisfied(invocation, currentMethodParamElt, paramJe)) {
          addOwningToParam(i);
          break;
        }
      }
    }
  }

  /**
   * Returns the set of resource aliases associated with the given argument node, by looking up the
   * corresponding obligation in the given set of obligations.
   *
   * @param obligations the set of obligations to search in
   * @param arg the argument node whose resource aliases are to be returned
   * @return the resource aliases associated with the given argument node, or an empty set if the
   *     node has none
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
   * This method performs three checks related to method invocation node. It computes @Owning
   * annotations to the enclosing formal parameter or fields:
   *
   * <ul>
   *   <li>If a formal method is passed as an owning parameter, it adds the @Owning annotation to
   *       that formal parameter (see {@link #addOwningParamsFromMethodCall}).
   *   <li>It calls {@link #addOwningReceiverFromMethodCall} to verify if the receiver of the method
   *       represented by {@code invocation} qualifies as a candidate owning field, and if the
   *       method invocation satisfies the field's must-call obligation. If these conditions are
   *       met, the field is added to the {@link #releasedFields} set.
   *   <li>It calls {@link #checkIndirectCalls} to inspect the method represented by the given
   *       MethodInvocationNode for any indirect calls within it. The method analyzes the
   *       called-methods set of each argument after the call and computes the @Owning annotation to
   *       the field or parameter passed as an argument to this call.
   * </ul>
   *
   * @param obligations the set of obligations to search in
   * @param invocation the MethodInvocationNode
   */
  private void checkMethodInvocation(Set<Obligation> obligations, MethodInvocationNode invocation) {
    if (methodElt != null) {
      addOwningParamsFromMethodCall(obligations, invocation);
      addOwningReceiverFromMethodCall(obligations, invocation);
      checkIndirectCalls(obligations, invocation);
    }
  }

  /**
   * Checks if a MustCall obligation is satisfied via the given method call. A MustCall obligation
   * of an element is satisfied if the called-methods set contains the target of its must-call
   * obligation.
   *
   * @param invocation the method invocation node being checked for satisfaction of the MustCall
   *     obligation
   * @param varElt a variable whose must-call obligation is being evaluated
   * @param varExpr a Java expression whose its must-call obligation is being evaluated
   * @return {@code true} if the MustCall obligation is satisfied
   */
  private boolean mustCallObligationSatisfied(
      MethodInvocationNode invocation, Element varElt, JavaExpression varExpr) {

    List<String> mustCallValues = typeFactory.getMustCallValue(varElt);
    if (mustCallValues.size() != 1) {
      // TODO: generalize this to MustCall annotations with more than one element.
      return false;
    }

    AccumulationStore cmStoreAfter = typeFactory.getStoreAfter(invocation);
    @Nullable AccumulationValue cmValue =
        cmStoreAfter == null ? null : cmStoreAfter.getValue(varExpr);
    AnnotationMirror cmAnno = null;

    if (cmValue != null) {
      // The store contains the lhs.
      Set<String> accumulatedValues = cmValue.getAccumulatedValues();
      if (accumulatedValues != null) {
        // accumulatedValues is not null if the underlying type is a type variable or a wildcard
        cmAnno = typeFactory.createCalledMethods(accumulatedValues.toArray(new String[0]));
      } else {
        for (AnnotationMirror anno : cmValue.getAnnotations()) {
          if (AnnotationUtils.areSameByName(
              anno, "org.checkerframework.checker.calledmethods.qual.CalledMethods")) {
            cmAnno = anno;
            break;
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
   * Adds all non-exceptional successors to {@code worklist}. If a successor is a non-exceptional
   * exit point, adds an {@literal @}Owning annotation for fields in {@link #releasedFields}.
   *
   * @param obligations the set of obligations to update
   * @param curBlock the block whose successors to add to the worklist
   * @param visited set of blocks that have already been added worklist
   * @param worklist current worklist
   */
  private void discoverNonExceptionalSuccessors(
      Set<Obligation> obligations,
      Block curBlock,
      Set<BlockWithObligations> visited,
      Deque<BlockWithObligations> worklist) {

    for (Block successor : getNonExceptionalSuccessors(curBlock)) {
      // If successor is a special block, it must be the regular exit.
      if (successor.getType() == Block.BlockType.SPECIAL_BLOCK) {
        WholeProgramInference wpi = typeFactory.getWholeProgramInference();
        assert wpi != null : "MustCallInference is running without WPI.";
        for (VariableElement fieldElt : getOwningFields()) {
          wpi.addFieldDeclarationAnnotation(fieldElt, OWNING);
        }
        if (!releasedFields.isEmpty()) {
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
   * Returns the non-exceptional successors of a block.
   *
   * @param cur a block
   * @return the successors of this current block
   */
  private List<Block> getNonExceptionalSuccessors(Block cur) {
    if (cur.getType() == Block.BlockType.CONDITIONAL_BLOCK) {
      ConditionalBlock ccur = (ConditionalBlock) cur;
      return List.of(ccur.getThenSuccessor(), ccur.getElseSuccessor());
    }
    if (!(cur instanceof SingleSuccessorBlock)) {
      throw new BugInCF("Not a conditional block nor a SingleSuccessorBlock: " + cur);
    }

    Block successor = ((SingleSuccessorBlock) cur).getSuccessor();
    if (successor != null) {
      return Collections.singletonList(successor);
    }
    return Collections.emptyList();
  }

  /**
   * Creates an {@code @EnsuresCalledMethods} annotation with the given arguments.
   *
   * @param value the expressions that will have methods called on them
   * @param methods the methods guaranteed to be invoked on the expressions
   * @return an {@code @EnsuresCalledMethods} annotation with the given arguments
   */
  private AnnotationMirror createEnsuresCalledMethods(String[] value, String[] methods) {
    AnnotationBuilder builder =
        new AnnotationBuilder(typeFactory.getProcessingEnv(), EnsuresCalledMethods.class);
    builder.setValue("value", value);
    builder.setValue("methods", methods);
    AnnotationMirror am = builder.build();
    return am;
  }

  /**
   * Creates an {@code @InheritableMustCall} annotation with the given arguments.
   *
   * @param methods methods that might need to be called on the expression whose type is annotated
   * @return an {@code @InheritableMustCall} annotation with the given arguments
   */
  private AnnotationMirror createInheritableMustCall(String[] methods) {
    AnnotationBuilder builder =
        new AnnotationBuilder(typeFactory.getProcessingEnv(), InheritableMustCall.class);
    Arrays.sort(methods);
    builder.setValue("value", methods);
    return builder.build();
  }
}
