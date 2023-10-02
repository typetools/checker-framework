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
import org.plumelib.util.CollectionsPlume;

/**
 * This class implements the annotation inference algorithm for the Resource Leak Checker. It infers
 * annotations such as {@code @}{@link Owning} on owning fields and
 * parameters, @EnsuresCalledMethods on methods, and @InheritableMustCall on class declarations.
 *
 * <p>Each instance of this class corresponds to a single control flow graph (CFG), typically
 * representing a method.
 *
 * <p>The algorithm determines if the @MustCall obligation of a field is satisfied along some path
 * leading to the regular exit point of the method. If the obligation is satisfied, it adds
 * an @Owning annotation on the field and an @EnsuresCalledMethods annotation on the method being
 * analyzed. Additionally, if the method being analyzed satisfies the must-call obligation of all
 * the enclosed owning fields, it adds a @InheritableMustCall annotation on the enclosing class.
 *
 * <p>Note: This class makes the assumption that the must-call set has only one element. Must-call
 * sets with more than one element may be supported in the future.
 *
 * <p>Note: When the -Ainfer flag is used by default, whole-program inference is disabled for the
 * Resource Leak Checker, and instead, this special mechanism as the best inference mechanism for
 * the Resource Leak Checker inference is executed. However, for testing and future experimental
 * purposes, we defined the -AenableWpiForRlc flag to enable whole-program inference (WPI) when
 * running the Resource Leak Checker inference.
 *
 * @see <a
 *     href="https://checkerframework.org/manual/#resource-leak-checker-inference-algo">Automatic
 *     Inference of Resource Leak Specifications</a>
 */
public class MustCallInference {

  /**
   * The fields that have been inferred to be disposed within the CFG currently under analysis. All
   * of these fields will be given an @Owning annotation.
   */
  private final Set<VariableElement> disposedFields = new HashSet<>();

  /**
   * The type factory for the Resource Leak Checker, which is used to access the Must Call Checker.
   */
  private final ResourceLeakAnnotatedTypeFactory resourceLeakAtf;

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
   * @param resourceLeakAtf the type factory
   * @param cfg the control flow graph of the method to check
   * @param mcca the MustCallConsistencyAnalyzer
   */
  /*package-private*/ MustCallInference(
      ResourceLeakAnnotatedTypeFactory resourceLeakAtf,
      ControlFlowGraph cfg,
      MustCallConsistencyAnalyzer mcca) {
    this.resourceLeakAtf = resourceLeakAtf;
    this.mcca = mcca;
    this.cfg = cfg;
    OWNING = AnnotationBuilder.fromClass(this.resourceLeakAtf.getElementUtils(), Owning.class);
    methodTree = ((UnderlyingAST.CFGMethod) cfg.getUnderlyingAST()).getMethod();
    methodElt = TreeUtils.elementFromDeclaration(methodTree);
  }

  /**
   * Creates a MustCallInference instance and runs the inference algorithm. A type factory's
   * postAnalyze method calls this, if Whole Program Inference is enabled.
   *
   * @param resourceLeakAtf the type factory
   * @param cfg the control flow graph of the method to check
   * @param mcca the MustCallConsistencyAnalyzer
   */
  protected static void runMustCallInference(
      ResourceLeakAnnotatedTypeFactory resourceLeakAtf,
      ControlFlowGraph cfg,
      MustCallConsistencyAnalyzer mcca) {
    MustCallInference mustCallInferenceLogic = new MustCallInference(resourceLeakAtf, cfg, mcca);
    mustCallInferenceLogic.runInference();
  }

  /**
   * Runs the inference algorithm on the current method (the {@link #cfg} field). It updates the
   * {@link #disposedFields} set or adds @Owning to the formal parameter if it discovers their
   * must-call obligations were satisfied along one of the checked paths.
   *
   * <p>Operationally, it checks method invocations for fields and parameters with
   * non-empty @MustCall obligations along all paths to the regular exit point.
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
          computeOwningFromInvocation(obligations, (MethodInvocationNode) node);
        } else if (node instanceof ObjectCreationNode) {
          mcca.updateObligationsWithInvocationResult(obligations, node);
        } else if (node instanceof AssignmentNode) {
          analyzeOwnershipTransferAtAssignment(obligations, (AssignmentNode) node);
        }
      }

      addNonExceptionalSuccessorsToWorklist(obligations, current.block, visited, worklist);
    }
  }

  /**
   * Returns a set of obligations representing the formal parameters of the current method that have
   * non-empty MustCall annotations. Returns an empty set if the given CFG doesn't correspond to a
   * method body.
   *
   * @param cfg the control flow graph of the method to check
   * @return a set of obligations representing the parameters with non-empty MustCall
   */
  private Set<Obligation> getNonEmptyMCParams(ControlFlowGraph cfg) {
    // TODO what about lambdas?
    if (cfg.getUnderlyingAST().getKind() != UnderlyingAST.Kind.METHOD) {
      return Collections.emptySet();
    }
    Set<Obligation> result = null;
    for (VariableTree param : methodTree.getParameters()) {
      if (resourceLeakAtf.declaredTypeHasMustCall(param)) {
        VariableElement paramElement = TreeUtils.elementFromDeclaration(param);
        if (result == null) {
          result = new HashSet<>(2);
        }
        result.add(
            new Obligation(
                ImmutableSet.of(new ResourceAlias(new LocalVariable(paramElement), param))));
      }
    }
    return result != null ? result : Collections.emptySet();
  }

  /**
   * Returns all owning fields within the enclosing class. These are the fields that have been
   * annotated with the {@code @Owning} annotation or inferred as owning.
   *
   * @return the owning fields
   */
  private Set<VariableElement> getOwningFields() {
    ClassTree classTree = TreePathUtil.enclosingClass(resourceLeakAtf.getPath(methodTree));
    TypeElement classElt = TreeUtils.elementFromDeclaration(classTree);
    Set<VariableElement> owningFields = new HashSet<>(disposedFields);
    for (Element memberElt : classElt.getEnclosedElements()) {
      if (memberElt.getKind().isField() && resourceLeakAtf.hasOwning(memberElt)) {
        owningFields.add((VariableElement) memberElt);
      }
    }
    return owningFields;
  }

  /**
   * Adds an owning annotation to the formal parameter at the given index.
   *
   * @param index the index of a formal parameter of the current method (1-based)
   */
  private void addOwningToParam(int index) {
    WholeProgramInference wpi = resourceLeakAtf.getWholeProgramInference();
    wpi.addDeclarationAnnotationToFormalParameter(methodElt, index, OWNING);
  }

  /**
   * Adds the node to the disposedFields set if it is a field and its must-call obligation is
   * satisfied by the given method call. If so, it will be given an @Owning annotation later.
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
    if (resourceLeakAtf.isFieldWithNonemptyMustCallValue(nodeElt)) {
      node = NodeUtils.removeCasts(node);
      JavaExpression nodeJe = JavaExpression.fromNode(node);
      if (mustCallObligationSatisfied(invocation, nodeElt, nodeJe)) {
        // This assumes that any MustCall annotation has at most one element.
        // TODO: generalize this to MustCall annotations with more than one element.
        disposedFields.add((VariableElement) nodeElt);
      }
    }
  }

  /**
   * Analyzes an assignment statement and performs three computations:
   *
   * <ul>
   *   <li>If the left-hand side of the assignment is an owning field, and the rhs is an alias of a
   *       formal parameter, it adds the {@code @Owning} annotation to the formal parameter.
   *   <li>If the left-hand side of the assignment is a resource variable, and the right-hand side
   *       is an alias of a formal parameter that has a must-call-close type, it adds the
   *       {@code @Owning} annotation to the formal parameter.
   *   <li>Otherwise, updates the set of tracked obligations to account for the (pseudo-)assignment
   *       to some variable, as in a gen-kill dataflow analysis problem.
   * </ul>
   *
   * @param obligations the set of obligations to update
   * @param assignmentNode the assignment statement
   */
  private void analyzeOwnershipTransferAtAssignment(
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
      if (!getOwningFields().contains(lhsElement)) {
        return;
      }
      if (!TreeUtils.isConstructor(methodTree)) {
        disposedFields.remove((VariableElement) lhsElement);
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
   * Adds @Owning to method parameter p, if the must-call obligation of some alias of p is satisfied
   * during the assignment.
   *
   * @param obligations the set of obligations to update
   * @param rhsObligation the obligation associated with the right-hand side of the assignment
   * @param rhs the right-hand side of the assignment
   */
  private void addOwningToParamsIfDisposedAtAssignment(
      Set<Obligation> obligations, Obligation rhsObligation, Node rhs) {
    Set<ResourceAlias> rhsAliases = rhsObligation.resourceAliases;
    if (rhsAliases.isEmpty()) {
      return;
    }
    List<VariableElement> paramElts =
        CollectionsPlume.mapList(TreeUtils::elementFromDeclaration, methodTree.getParameters());
    for (ResourceAlias rhsAlias : rhsAliases) {
      Element rhsElt = rhsAlias.reference.getElement();
      int i = paramElts.indexOf(rhsElt);
      if (i != -1) {
        addOwningToParam(i + 1);
        mcca.removeObligationsContainingVar(obligations, (LocalVariableNode) rhs);
      }
    }
  }

  /**
   * Adds an {@link EnsuresCalledMethods} annotation to the current method for any owning field
   * whose must-call obligation is satisfied within the current method.
   */
  private void addEnsuresCalledMethods() {
    // This map is used to create @EnsuresCalledMethods annotation for fields that share the same
    // must-call obligation on the method boundary. The keys are the must-call method names, and
    // the values are the set fields on which those methods are called
    Map<String, Set<String>> methodToFields = new LinkedHashMap<>();
    for (VariableElement disposedField : disposedFields) {
      List<String> mustCallValues = resourceLeakAtf.getMustCallValue(disposedField);
      assert !mustCallValues.isEmpty()
          : "Must-call obligation of owning field " + disposedField + " is empty.";
      // Currently, this code assumes that the must-call set has only one element.
      assert mustCallValues.size() == 1
          : "The must-call set ("
              + mustCallValues
              + ") of "
              + disposedField
              + "should be a singleton";
      String mustCallValue = mustCallValues.get(0);
      String fieldName = "this." + disposedField.getSimpleName().toString();

      methodToFields.computeIfAbsent(mustCallValue, k -> new HashSet<>()).add(fieldName);
    }

    for (String mustCallValue : methodToFields.keySet()) {
      Set<String> fields = methodToFields.get(mustCallValue);
      AnnotationMirror am =
          createEnsuresCalledMethods(
              fields.toArray(new String[fields.size()]), new String[] {mustCallValue});
      WholeProgramInference wpi = resourceLeakAtf.getWholeProgramInference();
      wpi.addMethodDeclarationAnnotation(methodElt, am);
    }
  }

  /**
   * Possibly adds an InheritableMustCall annotation on the enclosing class. If the class already
   * has a non-empty MustCall type (that is inherited from one of its superclasses), this method
   * does nothing, in order to avoid infinite iteration. Otherwise, if the current method is not
   * private and satisfies the must-call obligations of all the owning fields, it adds (or updates)
   * an InheritableMustCall annotation to the enclosing class.
   */
  private void addOrUpdateMustCall() {
    ClassTree classTree = TreePathUtil.enclosingClass(resourceLeakAtf.getPath(methodTree));

    // elementFromDeclaration returns null instead of crashing when no element exists for
    // the class tree, which can happen for certain kinds of anonymous classes, such as
    // PolyCollectorTypeVar.java in the all-systems test suite.
    TypeElement typeElement = TreeUtils.elementFromDeclaration(classTree);
    if (typeElement == null) {
      return;
    }

    WholeProgramInference wpi = resourceLeakAtf.getWholeProgramInference();
    List<String> currentMustCallValues = resourceLeakAtf.getMustCallValue(typeElement);
    if (!currentMustCallValues.isEmpty()) {
      // The class already has a MustCall annotation.

      // If it is inherited from a superclass, do nothing.
      if (typeElement.getSuperclass() != null) {
        TypeMirror superType = typeElement.getSuperclass();
        TypeElement superTypeElement = TypesUtils.getTypeElement(superType);
        if (superTypeElement != null
            && !resourceLeakAtf.getMustCallValue(superTypeElement).isEmpty()) {
          return;
        }
      }
      // Add the existing @MustCall annotation to guarantee the termination of inference. This is
      // necessary to prevent the re-computation of @MustCall annotations on the class declaration.
      // It simply adds the existing `@MustCall` annotation from the class declaration, helping to
      // maintain consistency and termination of the fixed-point algorithm. It becomes particularly
      // important when multiple methods could satisfy the must-call obligation, potentially
      // resulting in different @MustCall annotations between different iterations of the
      // fixed-point algorithm.
      AnnotationMirror am = createInheritableMustCall(new String[] {currentMustCallValues.get(0)});
      wpi.addClassDeclarationAnnotation(typeElement, am);
      return;
    }

    // If the current method is not private and satisfies the must-call obligation of all owning
    // fields, add an InheritableMustCall annotation with the name of this method.
    if (!methodTree.getModifiers().getFlags().contains(Modifier.PRIVATE)) {
      // Since the result of getOwningFields() is a superset of disposedFields, it is sufficient to
      // check the equality of their sizes to determine if both sets are equal.
      if (!disposedFields.isEmpty() && disposedFields.size() == getOwningFields().size()) {
        AnnotationMirror am =
            createInheritableMustCall(new String[] {methodTree.getName().toString()});
        wpi.addClassDeclarationAnnotation(typeElement, am);
      }
    }
  }

  /**
   * Computes {@code @Owning} annotation for the receiver of the method call. If the receiver is a
   * field, compute {@code @Owning} annotation for the field. If the receiver is a resource alias
   * with a parameter of the current method, and the method invocation satisfies its must-call
   * obligation, it adds the {@code @Owning} annotation to that parameter.
   *
   * @param obligations the obligations associated with the current block
   * @param invocation the method invocation node to check
   */
  private void analyzeOwnershipOfReceiverFromMethodInvocation(
      Set<Obligation> obligations, MethodInvocationNode invocation) {
    List<? extends VariableTree> paramsOfCurrentMethod = methodTree.getParameters();

    Node receiver = invocation.getTarget().getReceiver();
    if (receiver.getTree() == null) {
      // It's a static method.
      return;
    }

    Element receiverElt = TreeUtils.elementFromTree(receiver.getTree());
    if (receiverElt != null) {
      if (receiverElt.getKind().isField()) {
        inferOwningField(receiver, invocation);
        return;
      }
    }

    Node receiverTempVar = mcca.getTempVarOrNode(receiver);
    if (!(receiverTempVar instanceof LocalVariableNode)) {
      return;
    }

    Obligation receiverObligation =
        MustCallConsistencyAnalyzer.getObligationForVar(
            obligations, (LocalVariableNode) receiverTempVar);
    if (receiverObligation == null) {
      return;
    }

    Set<ResourceAlias> receiverAliases = receiverObligation.resourceAliases;
    if (receiverAliases.isEmpty()) {
      return;
    }

    for (int i = 1; i < paramsOfCurrentMethod.size() + 1; i++) {
      VariableTree paramOfCurrMethod = paramsOfCurrentMethod.get(i - 1);
      if (resourceLeakAtf.hasEmptyMustCallValue(paramOfCurrMethod)) {
        continue;
      }
      VariableElement paramElt = TreeUtils.elementFromDeclaration(paramOfCurrMethod);

      for (ResourceAlias resourceAlias : receiverAliases) {
        Element resourceElt = resourceAlias.reference.getElement();
        if (!resourceElt.equals(paramElt)) {
          continue;
        }

        JavaExpression paramJe = JavaExpression.fromVariableTree(paramOfCurrMethod);
        if (mustCallObligationSatisfied(invocation, paramElt, paramJe)) {
          addOwningToParam(i);
          break;
        }
      }
    }
  }

  /**
   * Analyze the arguments of the method invocation node. If any of them is passed as an owning
   * parameter to the callee, and is an alias with any parameter of the current method, this method
   * adds the {@code @Owning} annotation to the corresponding parameter of the current method.
   *
   * @param obligations the obligations associated with the current block
   * @param invocation the method invocation node to check
   */
  private void analyzeOwnershipTransferAtMethodCall(
      Set<Obligation> obligations, MethodInvocationNode invocation) {
    List<? extends VariableTree> paramsOfCurrentMethod = methodTree.getParameters();
    if (paramsOfCurrentMethod.isEmpty()) {
      return;
    }
    List<? extends VariableElement> invocationParams = mcca.getParametersOfInvocation(invocation);
    if (invocationParams.isEmpty()) {
      return;
    }
    List<Node> arguments = mcca.getArgumentsOfInvocation(invocation);

    for (int i = 1; i < arguments.size() + 1; i++) {
      if (!resourceLeakAtf.hasOwning(invocationParams.get(i - 1))) {
        continue;
      }
      for (int j = 1; j < paramsOfCurrentMethod.size() + 1; j++) {
        VariableTree paramOfCurrMethod = paramsOfCurrentMethod.get(j - 1);
        if (resourceLeakAtf.hasEmptyMustCallValue(paramOfCurrMethod)) {
          continue;
        }

        Node arg = NodeUtils.removeCasts(arguments.get(i - 1));
        VariableElement paramElt = TreeUtils.elementFromDeclaration(paramOfCurrMethod);
        if (isParamAndArgAliased(obligations, arg, paramElt)) {
          addOwningToParam(j);
          break;
        }
      }
    }
  }

  /**
   * Checks whether the given argument which is passed in the invocation node and the given
   * parameter of the current method are resource aliases.
   *
   * @param obligations the obligations associated with the current block
   * @param argument the argument
   * @param paramElt the parameter
   * @return true if the {@code paramElt} is in the resource alias set of the given {@code arg}
   */
  private boolean isParamAndArgAliased(
      Set<Obligation> obligations, Node argument, VariableElement paramElt) {
    Set<ResourceAlias> argAliases = getResourceAliasOfArgument(obligations, argument);
    for (ResourceAlias argAlias : argAliases) {
      Element argAliasElt = argAlias.reference.getElement();
      if (argAliasElt.equals(paramElt)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Analyzes the called-methods set of each argument passed into the method call to compute
   * the @Owning annotation for the field or parameter used as an argument in the call.
   *
   * @param obligations set of obligations associated with the current block
   * @param invocation a method invocation node to check
   */
  private void analyzeCalledMethodsOfInvocationArgs(
      Set<Obligation> obligations, MethodInvocationNode invocation) {
    List<? extends VariableTree> paramsOfCurrentMethod = methodTree.getParameters();

    List<? extends VariableElement> paramsOfInvocation = mcca.getParametersOfInvocation(invocation);
    if (paramsOfInvocation.isEmpty()) {
      return;
    }

    for (Node argument : mcca.getArgumentsOfInvocation(invocation)) {
      Node arg = NodeUtils.removeCasts(argument);

      // In the CFG, explicit passing of multiple arguments in the varargs position is represented
      // via an ArrayCreationNode. In this case, it checks the called methods set of each argument
      // passed in this position.
      if (arg instanceof ArrayCreationNode) {
        ArrayCreationNode varArgsNode = (ArrayCreationNode) arg;
        analyzeCalledMethodsSetForVarArgsToComputeOwningParams(
            obligations, paramsOfCurrentMethod, invocation, varArgsNode, arg);
      } else {
        Element varArgElt = TreeUtils.elementFromTree(arg.getTree());
        if (varArgElt != null && varArgElt.getKind().isField()) {
          inferOwningField(arg, invocation);
          continue;
        }
        analyzeCalledMethodsSetForArgAliasesToInferOwningParams(
            obligations, paramsOfCurrentMethod, invocation, arg);
      }
    }
  }

  /**
   * Analyze each node passed in the varargs argument position. This method checks the
   * called-methods set of each argument after the call to compute an {@code @Owning} annotation for
   * the field or parameter passed as an argument to a method invocation.
   *
   * @param obligations set of obligations associated with the current block
   * @param paramsOfCurrentMethod the parameters of the current method
   * @param invocation the method invocation node to check
   * @param varArgsNode the VarArg node of the given method invocation node
   * @param arg the argument of a method invocation node
   */
  private void analyzeCalledMethodsSetForVarArgsToComputeOwningParams(
      Set<Obligation> obligations,
      List<? extends VariableTree> paramsOfCurrentMethod,
      MethodInvocationNode invocation,
      ArrayCreationNode varArgsNode,
      Node arg) {
    for (Node varArgNode : varArgsNode.getInitializers()) {
      Element varArgElt = TreeUtils.elementFromTree(varArgNode.getTree());

      if (varArgElt == null) {
        continue;
      }

      if (varArgElt.getKind().isField()) {
        inferOwningField(varArgNode, invocation);
      } else {
        analyzeCalledMethodsSetForArgAliasesToInferOwningParams(
            obligations, paramsOfCurrentMethod, invocation, arg);
      }
    }
  }

  /**
   * For any parameter of the current method that is aliased with an argument passed to the method
   * invocation, check the set of called methods for the parameter after the call, in order to infer
   * the owning annotation for that parameter.
   *
   * @param obligations set of obligations associated with the current block
   * @param paramsOfCurrentMethod the parameters of the current method
   * @param invocation the method invocation node to check
   * @param arg the argument of a method invocation node
   */
  private void analyzeCalledMethodsSetForArgAliasesToInferOwningParams(
      Set<Obligation> obligations,
      List<? extends VariableTree> paramsOfCurrentMethod,
      MethodInvocationNode invocation,
      Node arg) {

    for (int i = 1; i < paramsOfCurrentMethod.size() + 1; i++) {
      if (resourceLeakAtf.hasEmptyMustCallValue(paramsOfCurrentMethod.get(i - 1))) {
        continue;
      }
      VariableTree currentMethodParamTree = paramsOfCurrentMethod.get(i - 1);
      VariableElement currentMethodParamElt =
          TreeUtils.elementFromDeclaration(currentMethodParamTree);
      if (isParamAndArgAliased(obligations, arg, currentMethodParamElt)) {
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
   * This method performs three computations related to method invocation nodes. It computes @Owning
   * annotations for the enclosing formal parameter or fields:
   *
   * <ul>
   *   <li>If a formal method is passed as an owning parameter, it adds the @Owning annotation to
   *       that formal parameter (see {@link #analyzeOwnershipTransferAtMethodCall}).
   *   <li>It calls {@link #analyzeOwnershipOfReceiverFromMethodInvocation} to verify if the
   *       receiver of the method represented by {@code invocation} qualifies as a candidate owning
   *       field, and if the method invocation satisfies the field's must-call obligation. If these
   *       conditions are met, the field is added to the {@link #disposedFields} set.
   *   <li>It calls {@link #analyzeCalledMethodsOfInvocationArgs} to inspect the method represented
   *       by the given MethodInvocationNode for any indirect calls within it. The method analyzes
   *       the called-methods set of each argument after the call and computes the @Owning
   *       annotation to the field or parameter passed as an argument to this call.
   * </ul>
   *
   * @param obligations the set of obligations to search in
   * @param invocation the MethodInvocationNode
   */
  private void computeOwningFromInvocation(
      Set<Obligation> obligations, MethodInvocationNode invocation) {
    if (methodElt != null) {
      analyzeOwnershipTransferAtMethodCall(obligations, invocation);
      analyzeOwnershipOfReceiverFromMethodInvocation(obligations, invocation);
      analyzeCalledMethodsOfInvocationArgs(obligations, invocation);
    }
  }

  /**
   * Checks if a MustCall obligation is satisfied via the given method call. A MustCall obligation
   * of an element is satisfied if the called-methods set contains the target of its must-call
   * obligation.
   *
   * @param invocation the method invocation node being checked for satisfaction of the MustCall
   *     obligation
   * @param varElt the variable annotated with the MustCall annotation
   * @param varJe the java expression corresponding to the {@code varElt}
   * @return {@code true} if the MustCall obligation is satisfied
   */
  private boolean mustCallObligationSatisfied(
      MethodInvocationNode invocation, Element varElt, JavaExpression varJe) {

    List<String> mustCallValues = resourceLeakAtf.getMustCallValue(varElt);
    if (mustCallValues.size() != 1) {
      // TODO: generalize this to MustCall annotations with more than one element.
      return false;
    }

    AccumulationStore cmStoreAfter = resourceLeakAtf.getStoreAfter(invocation);
    @Nullable AccumulationValue cmValue =
        cmStoreAfter == null ? null : cmStoreAfter.getValue(varJe);
    AnnotationMirror cmAnno = null;

    if (cmValue != null) {
      // The store contains the lhs.
      Set<String> accumulatedValues = cmValue.getAccumulatedValues();
      if (accumulatedValues != null) { // type variable or wildcard type
        cmAnno = resourceLeakAtf.createCalledMethods(accumulatedValues.toArray(new String[0]));
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
      cmAnno = resourceLeakAtf.top;
    }

    return mcca.calledMethodsSatisfyMustCall(mustCallValues, cmAnno);
  }

  /**
   * Adds all non-exceptional successors to {@code worklist}. If a successor is a non-exceptional
   * exit point, adds an {@literal @Owning} annotation for fields in {@link #disposedFields}.
   *
   * @param obligations the obligationsx for the current block
   * @param curBlock the block whose successors to add to the worklist
   * @param visited block-Obligations pairs already analyzed or already on the worklist
   * @param worklist the worklist, which is side-effected by this method
   */
  private void addNonExceptionalSuccessorsToWorklist(
      Set<Obligation> obligations,
      Block curBlock,
      Set<BlockWithObligations> visited,
      Deque<BlockWithObligations> worklist) {

    for (Block successor : getNonExceptionalSuccessors(curBlock)) {
      // If successor is a special block, it must be the regular exit.
      if (successor.getType() == Block.BlockType.SPECIAL_BLOCK) {
        WholeProgramInference wpi = resourceLeakAtf.getWholeProgramInference();
        assert wpi != null : "MustCallInference is running without WPI.";
        for (VariableElement fieldElt : getOwningFields()) {
          wpi.addFieldDeclarationAnnotation(fieldElt, OWNING);
        }
        if (!disposedFields.isEmpty()) {
          addEnsuresCalledMethods();
        }

        addOrUpdateMustCall();
      } else {
        BlockWithObligations state = new BlockWithObligations(successor, obligations);
        if (visited.add(state)) {
          worklist.add(state);
        }
      }
    }
  }

  /**
   * Returns the non-exceptional successors of a block.
   *
   * @param cur a block
   * @return the successors of the given block
   */
  private List<Block> getNonExceptionalSuccessors(Block cur) {
    if (cur.getType() == Block.BlockType.CONDITIONAL_BLOCK) {
      ConditionalBlock ccur = (ConditionalBlock) cur;
      return Arrays.asList(ccur.getThenSuccessor(), ccur.getElseSuccessor());
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
        new AnnotationBuilder(resourceLeakAtf.getProcessingEnv(), EnsuresCalledMethods.class);
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
        new AnnotationBuilder(resourceLeakAtf.getProcessingEnv(), InheritableMustCall.class);
    Arrays.sort(methods);
    builder.setValue("value", methods);
    return builder.build();
  }
}
