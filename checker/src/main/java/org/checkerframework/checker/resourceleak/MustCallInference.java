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
 * <p>See {@link ResourceLeakChecker#ENABLE_WPI_FOR_RLC} for an explanation of the meaning of the
 * flags {@code -Ainfer} and {@code -AenableWpiForRlc}.
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
   * The fields with written or inferred {@code @Owning} annotations at the entry point of the CFG
   * currently under analysis.
   */
  private final Set<VariableElement> owningFields = new HashSet<>();

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

  /** The ClassTree referring to the enclosing class of the current method. */
  private final ClassTree classTree;

  /** The element referring to the enclosing class of the current method. */
  private final @Nullable TypeElement classElt;

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
    classTree = TreePathUtil.enclosingClass(resourceLeakAtf.getPath(methodTree));
    // elementFromDeclaration() returns null when no element exists for the class tree, which can
    // happen for certain kinds of anonymous classes, such as PolyCollectorTypeVar.java in the
    // all-systems test suite.
    classElt = TreeUtils.elementFromDeclaration(classTree);

    if (classElt != null) {
      for (Element memberElt : classElt.getEnclosedElements()) {
        if (memberElt.getKind().isField() && resourceLeakAtf.hasOwning(memberElt)) {
          owningFields.add((VariableElement) memberElt);
        }
      }
    }
  }

  /**
   * Creates a MustCallInference instance and runs the inference algorithm. A type factory's
   * postAnalyze method calls this, if Whole Program Inference is enabled.
   *
   * @param resourceLeakAtf the type factory
   * @param cfg the control flow graph of the method to check
   * @param mcca the MustCallConsistencyAnalyzer
   */
  /*package-private*/ static void runMustCallInference(
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
        // The obligation set calculated for RLC differs from the Inference process. In the
        // Inference process, it exclusively tracks parameters with non-empty must-call types,
        // whether they have the @Owning annotation or not. However, there are some shared
        // computations, such as 'updateObligationsWithInvocationResult,' which is used during
        // inference and could potentially affect the RLC result if it were called before the
        // checking phase. However, calling 'updateObligationsWithInvocationResult()' will not have
        // any side effects on the outcome of the Resource Leak Checker. This is because the
        // inference occurs within the postAnalyze method of the ResourceLeakAnnotatedTypeFactory,
        // once the consistency analyzer has completed its process
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
    owningFields.addAll(disposedFields);
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
  private void inferOwningField(Node node, MethodInvocationNode invocation) {
    Element nodeElt = TreeUtils.elementFromTree(node.getTree());
    if (nodeElt == null || !nodeElt.getKind().isField()) {
      return;
    }
    if (resourceLeakAtf.isFieldWithNonemptyMustCallValue(nodeElt)) {
      node = NodeUtils.removeCasts(node);
      JavaExpression nodeJe = JavaExpression.fromNode(node);
      AnnotationMirror cmAnno = getCalledMethodsAnno(invocation, nodeJe);
      List<String> mustCallValues = resourceLeakAtf.getMustCallValues(nodeElt);
      if (mcca.calledMethodsSatisfyMustCall(mustCallValues, cmAnno)) {
        // This assumes that any MustCall annotation has at most one element.
        // TODO: generalize this to MustCall annotations with more than one element.
        assert mustCallValues.size() <= 1 : "TODO: Handle larger must-call values sets";
        disposedFields.add((VariableElement) nodeElt);
      }
    }
  }

  /**
   * Analyzes an assignment statement and performs three computations:
   *
   * <ul>
   *   <li>If the left-hand side of the assignment is an owning field, and the rhs is an alias of a
   *       formal parameter, it adds an {@code @Owning} annotation to the formal parameter.
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

      // If the owning field is present in the disposedFields set and there is an assignment to the
      // field, it must be removed from the set. This is essential since the disposedFields set is
      // used for adding @EnsuresCalledMethods annotations to the current method later.
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
   * If a must-call obligation of some alias of method parameter p is satisfied during the
   * assignment, add an @Owning annotation to p, and remove the rhs node from the obligations set,
   * since it no longer needs to be tracked.
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
        break;
      }
    }
  }

  /**
   * Adds an {@link EnsuresCalledMethods} annotation to the current method for any owning field
   * whose must-call obligation is satisfied within the current method, i.e., the fields in {@link
   * #disposedFields}.
   */
  private void addEnsuresCalledMethods() {
    // The keys are the must-call method names, and the values are the set of fields on which those
    // methods should be called. This map is used to create a single @EnsuresCalledMethods
    // annotation for
    // fields that share the same must-call obligation.
    Map<String, Set<String>> methodToFields = new LinkedHashMap<>();
    for (VariableElement disposedField : disposedFields) {
      List<String> mustCallValues = resourceLeakAtf.getMustCallValues(disposedField);
      assert !mustCallValues.isEmpty()
          : "Must-call obligation of owning field " + disposedField + " is empty.";
      // Currently, this code assumes that the must-call set has only one element.
      assert mustCallValues.size() == 1
          : "The must-call set of " + disposedField + "should be a singleton: " + mustCallValues;
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
   * Possibly adds an InheritableMustCall annotation on the enclosing class.
   *
   * <p>If the class already has a non-empty MustCall type (that is inherited from one of its
   * superclasses), this method does nothing, in order to avoid infinite iteration. Otherwise, if
   * the current method is not private and satisfies the must-call obligations of all the owning
   * fields, it adds (or updates) an InheritableMustCall annotation to the enclosing class.
   */
  private void addOrUpdateClassMustCall() {
    if (classElt == null) {
      return;
    }

    WholeProgramInference wpi = resourceLeakAtf.getWholeProgramInference();
    List<String> currentMustCallValues = resourceLeakAtf.getMustCallValues(classElt);
    if (!currentMustCallValues.isEmpty()) {
      // The class already has a MustCall annotation.

      // If it is inherited from a superclass, do nothing.
      if (classElt.getSuperclass() != null) {
        TypeMirror superType = classElt.getSuperclass();
        TypeElement superClassElt = TypesUtils.getTypeElement(superType);
        if (superClassElt != null && !resourceLeakAtf.getMustCallValues(superClassElt).isEmpty()) {
          return;
        }
      }

      // If the enclosing class already has a non-empty @MustCall type, either added by programmers
      // or inferred in previous iterations (not-inherited), we do not change it in the current
      // analysis round to prevent potential inconsistencies and guarantee the termination of the
      // inference algorithm. This becomes particularly important when multiple methods could
      // satisfy
      // the must-call obligation of the enclosing class. To ensure the existing
      // @MustCall annotation is included in the inference result for this iteration, we re-add it.
      assert currentMustCallValues.size() == 1 : "TODO: Handle multiple must-call values";
      AnnotationMirror am = createInheritableMustCall(new String[] {currentMustCallValues.get(0)});
      wpi.addClassDeclarationAnnotation(classElt, am);
      return;
    }

    // If the current method is not private and satisfies the must-call obligation of all owning
    // fields, then add (to the class) an InheritableMustCall annotation with the name of this
    // method.
    if (!methodTree.getModifiers().getFlags().contains(Modifier.PRIVATE)) {
      // Since the result of getOwningFields() is a superset of disposedFields, it is sufficient to
      // check the equality of their sizes to determine if both sets are equal.
      if (!disposedFields.isEmpty() && disposedFields.size() == getOwningFields().size()) {
        AnnotationMirror am =
            createInheritableMustCall(new String[] {methodTree.getName().toString()});
        wpi.addClassDeclarationAnnotation(classElt, am);
      }
    }
  }

  /**
   * Computes an {@code @Owning} annotation for the receiver of the method call, which can be either
   * a field or a formal parameter of the current method.
   *
   * @param obligations the obligations associated with the current block
   * @param invocation the method invocation node to check
   */
  private void computeOwningForReceiver(
      Set<Obligation> obligations, MethodInvocationNode invocation) {
    Node receiver = invocation.getTarget().getReceiver();
    if (receiver.getTree() == null) {
      // There is no receiver e.g. in static methods or when the receiver is implicit "this".
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

    List<? extends VariableTree> paramsOfCurrentMethod = methodTree.getParameters();

    computeOwningForParamOfCurrentMethod(obligations, paramsOfCurrentMethod, invocation, receiver);
  }

  /**
   * Computes ownership transfer at the method call to infer @Owning annotation for the arguments
   * passed into the call.
   *
   * @param obligations the obligations associated with the current block
   * @param invocation the method invocation node to check
   */
  private void inferOwningParamsViaOwnershipTransfer(
      Set<Obligation> obligations, MethodInvocationNode invocation) {
    List<? extends VariableTree> paramsOfCurrentMethod = methodTree.getParameters();
    if (paramsOfCurrentMethod.isEmpty()) {
      return;
    }
    List<? extends VariableElement> calleeParams = mcca.getParametersOfInvocation(invocation);
    if (calleeParams.isEmpty()) {
      return;
    }
    List<Node> arguments = mcca.getArgumentsOfInvocation(invocation);

    for (int i = 0; i < arguments.size(); i++) {
      if (!resourceLeakAtf.hasOwning(calleeParams.get(i))) {
        continue;
      }
      for (int j = 0; j < paramsOfCurrentMethod.size(); j++) {
        VariableTree paramOfCurrMethod = paramsOfCurrentMethod.get(j);
        if (resourceLeakAtf.hasEmptyMustCallValue(paramOfCurrMethod)) {
          continue;
        }

        Node arg = NodeUtils.removeCasts(arguments.get(i));
        VariableElement paramElt = TreeUtils.elementFromDeclaration(paramOfCurrMethod);
        if (nodeAndElementResourceAliased(obligations, arg, paramElt)) {
          addOwningToParam(j + 1);
          break;
        }
      }
    }
  }

  /**
   * Checks whether the given element is a resource alias of the given node in the provided set of
   * obligations.
   *
   * @param obligations the obligations associated with the current block
   * @param node the node
   * @param element the element
   * @return true if {@code element} is a resource alias of {@code node}
   */
  private boolean nodeAndElementResourceAliased(
      Set<Obligation> obligations, Node node, VariableElement element) {
    Set<ResourceAlias> nodeAliases = getResourceAliasOfNode(obligations, node);
    for (ResourceAlias nodeAlias : nodeAliases) {
      Element nodeAliasElt = nodeAlias.reference.getElement();
      if (nodeAliasElt.equals(element)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Computes @Owning annotations for arguments of a call.
   *
   * @param obligations set of obligations associated with the current block
   * @param invocation a method invocation node to check
   */
  private void computeOwningForArgsOfCall(
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
        computeOwningParamsForVarArgs(obligations, paramsOfCurrentMethod, invocation, varArgsNode);
      } else {
        Element varArgElt = TreeUtils.elementFromTree(arg.getTree());
        if (varArgElt != null && varArgElt.getKind().isField()) {
          inferOwningField(arg, invocation);
          continue;
        }
        computeOwningForParamOfCurrentMethod(obligations, paramsOfCurrentMethod, invocation, arg);
      }
    }
  }

  /**
   * Computes an @Owning annotation for a field or parameter passed in the varargs argument
   * position.
   *
   * @param obligations set of obligations associated with the current block
   * @param paramsOfCurrentMethod the parameters of the current method
   * @param invocation the method invocation node to check
   * @param varArgsNode the VarArg node of the given method invocation node
   */
  private void computeOwningParamsForVarArgs(
      Set<Obligation> obligations,
      List<? extends VariableTree> paramsOfCurrentMethod,
      MethodInvocationNode invocation,
      ArrayCreationNode varArgsNode) {
    for (Node varArgNode : varArgsNode.getInitializers()) {
      Element varArgElt = TreeUtils.elementFromTree(varArgNode.getTree());

      if (varArgElt == null) {
        continue;
      }

      if (varArgElt.getKind().isField()) {
        inferOwningField(varArgNode, invocation);
      } else {
        computeOwningForParamOfCurrentMethod(
            obligations, paramsOfCurrentMethod, invocation, varArgNode);
      }
    }
  }

  /**
   * Computes an @Owning annotation for any parameter of the current method that is aliased with the
   * argument passed to the method call.
   *
   * @param obligations set of obligations associated with the current block
   * @param paramsOfCurrentMethod the parameters of the current method
   * @param invocation the method invocation node to check
   * @param arg the argument of a method invocation node
   */
  private void computeOwningForParamOfCurrentMethod(
      Set<Obligation> obligations,
      List<? extends VariableTree> paramsOfCurrentMethod,
      MethodInvocationNode invocation,
      Node arg) {

    outerLoop:
    for (int i = 0; i < paramsOfCurrentMethod.size(); i++) {
      VariableTree currentMethodParamTree = paramsOfCurrentMethod.get(i);
      if (resourceLeakAtf.hasEmptyMustCallValue(currentMethodParamTree)) {
        continue;
      }

      VariableElement paramElt = TreeUtils.elementFromDeclaration(currentMethodParamTree);

      if (nodeAndElementResourceAliased(obligations, arg, paramElt)) {
        List<String> mustCallValues = resourceLeakAtf.getMustCallValues(paramElt);
        // TODO: generalize this method to MustCall annotations with more than one element.
        assert mustCallValues.size() <= 1 : "TODO: Handle larger must-call values sets";
        Set<ResourceAlias> nodeAliases = getResourceAliasOfNode(obligations, arg);
        for (ResourceAlias resourceAlias : nodeAliases) {
          AnnotationMirror cmAnno = getCalledMethodsAnno(invocation, resourceAlias.reference);
          if (mcca.calledMethodsSatisfyMustCall(mustCallValues, cmAnno)) {
            addOwningToParam(i + 1);
            break outerLoop;
          }
        }
      }
    }
  }

  /**
   * Returns the set of resource aliases associated with the given node, by looking up the
   * corresponding obligation in the given set of obligations.
   *
   * @param obligations the set of obligations to search in
   * @param node the node whose resource aliases are to be returned
   * @return the resource aliases associated with the given node, or an empty set if the node has
   *     none
   */
  private Set<ResourceAlias> getResourceAliasOfNode(Set<Obligation> obligations, Node node) {
    Node tempVar = mcca.getTempVarOrNode(node);
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
   * Computes @Owning annotations for the enclosing formal parameter or fields:
   *
   * <ul>
   *   <li>If a formal parameter is passed as an owning parameter, it adds the @Owning annotation to
   *       that formal parameter (see {@link #inferOwningParamsViaOwnershipTransfer}).
   *   <li>It calls {@link #computeOwningForReceiver} to verify if the receiver of the method
   *       represented by {@code invocation} qualifies as a candidate owning field, and if the
   *       method invocation satisfies the field's must-call obligation. If these conditions are
   *       met, the field is added to the {@link #disposedFields} set.
   *   <li>It calls {@link #computeOwningForArgsOfCall} to inspect the method represented by the
   *       given MethodInvocationNode for any indirect calls within it. The method analyzes the
   *       called-methods set of each argument after the call and computes the @Owning annotation to
   *       the field or parameter passed as an argument to this call.
   * </ul>
   *
   * @param obligations the set of obligations to search in
   * @param invocation the MethodInvocationNode
   */
  private void computeOwningFromInvocation(
      Set<Obligation> obligations, MethodInvocationNode invocation) {
    if (methodElt != null) {
      inferOwningParamsViaOwnershipTransfer(obligations, invocation);
      computeOwningForReceiver(obligations, invocation);
      computeOwningForArgsOfCall(obligations, invocation);
    }
  }

  /**
   * Returns the called methods annotation for the given Java expression after the invocation node.
   *
   * @param invocation the MethodInvocationNode
   * @param varJe a Java expression
   * @return the called methods annotation for the {@code varJe} after the {@code invocation} node.
   */
  private AnnotationMirror getCalledMethodsAnno(
      MethodInvocationNode invocation, JavaExpression varJe) {
    AccumulationStore cmStoreAfter = resourceLeakAtf.getStoreAfter(invocation);
    AccumulationValue cmValue = cmStoreAfter == null ? null : cmStoreAfter.getValue(varJe);

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

    return cmAnno;
  }

  /**
   * Adds all non-exceptional successors to {@code worklist}. If a successor is a non-exceptional
   * exit point, adds an {@literal @Owning} annotation for fields in {@link #disposedFields}.
   *
   * @param obligations the obligations for the current block
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

        addOrUpdateClassMustCall();
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
