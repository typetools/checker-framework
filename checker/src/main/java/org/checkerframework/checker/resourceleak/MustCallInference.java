package org.checkerframework.checker.resourceleak;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
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
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer.BlockWithObligations;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer.MethodExitKind;
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
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
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
 * annotations such as {@code @}{@link Owning} on owning fields and parameters, {@code @}{@link
 * NotOwning} on return types, {@code @}{@link EnsuresCalledMethods} on methods, {@code @}{@link
 * MustCallAlias} on parameters and return types, and {@code @}{@link InheritableMustCall} on class
 * declarations.
 *
 * <p>Each instance of this class corresponds to a single control flow graph (CFG), typically
 * representing a method. The entry method of this class is {@link
 * #runMustCallInference(ResourceLeakAnnotatedTypeFactory, ControlFlowGraph,
 * MustCallConsistencyAnalyzer)}, invoked from the {@link
 * ResourceLeakAnnotatedTypeFactory#postAnalyze} method when Whole Program Inference is enabled.
 *
 * <p>The algorithm determines if the @MustCall obligation of a field is satisfied along some path
 * leading to the regular exit point of the method. If the obligation is satisfied, the algorithm
 * adds an @Owning annotation on the field and an @EnsuresCalledMethods annotation on the method
 * being analyzed. Additionally, if the method being analyzed satisfies the must-call obligation of
 * all the enclosed owning fields, the algorithm adds a @InheritableMustCall annotation on the
 * enclosing class.
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
   * This map keeps the fields that have been inferred to be disposed within the current method.
   * Keys represent inferred owning fields, and values contain the must-call method names (Note:
   * currently this code assumes that the must-call set only contains one element). When inference
   * finishes, all of the fields in this map will be given an @Owning annotation. Note that this map
   * is not monotonically-increasing: fields may be added to this map and then removed during
   * inference. For example, if a field's must-call method is called, it is added to this map. If,
   * in a later statement in the same method, the same field is re-assigned, it will be removed from
   * this map (since the field assignment invalidated the previously-inferred disposing of the
   * obligation).
   */
  private final Map<VariableElement, String> disposedFields = new LinkedHashMap<>();

  /**
   * The owned fields. This includes:
   *
   * <ul>
   *   <li>fields with written {@code @Owning} annotations, and
   *   <li>the inferred owning fields in this analysis.
   * </ul>
   *
   * This set is a superset of the key set in the {@code disposedFields} map.
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

  /** The {@link NotOwning} annotation. */
  protected final AnnotationMirror NOTOWNING;

  /** The {@link MustCallAlias} annotation. */
  protected final AnnotationMirror MUSTCALLALIAS;

  /**
   * The control flow graph of the current method. There is a separate MustCallInference for each
   * method.
   */
  private final ControlFlowGraph cfg;

  /** The MethodTree of the current method. */
  private final MethodTree methodTree;

  /** The element for the current method. */
  private final ExecutableElement methodElt;

  /** The tree for the enclosing class of the current method. */
  private final ClassTree classTree;

  /**
   * The element for the enclosing class of the current method. It can be null for certain kinds of
   * anonymous classes, such as PolyCollectorTypeVar.java in the all-systems test suite.
   */
  private final @Nullable TypeElement classElt;

  /**
   * This map is used to track must-alias relationships between the obligations that are
   * resource-aliased to the return nodes and method parameters. The keys are the obligation of
   * return nodes, and the values are the index of current method formal parameter (1-based) that is
   * aliased with the return node. This map will be used to infer the {@link MustCallAlias}
   * annotation for method parameters.
   */
  private final Map<Obligation, Integer> returnObligationToParameter = new HashMap<>();

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
    this.OWNING = AnnotationBuilder.fromClass(this.resourceLeakAtf.getElementUtils(), Owning.class);
    this.NOTOWNING =
        AnnotationBuilder.fromClass(this.resourceLeakAtf.getElementUtils(), NotOwning.class);
    this.MUSTCALLALIAS =
        AnnotationBuilder.fromClass(this.resourceLeakAtf.getElementUtils(), MustCallAlias.class);
    this.methodTree = ((UnderlyingAST.CFGMethod) cfg.getUnderlyingAST()).getMethod();
    this.methodElt = TreeUtils.elementFromDeclaration(methodTree);
    this.classTree = TreePathUtil.enclosingClass(resourceLeakAtf.getPath(methodTree));
    this.classElt = TreeUtils.elementFromDeclaration(classTree);
    if (classElt != null) {
      for (Element memberElt : classElt.getEnclosedElements()) {
        if (memberElt.getKind().isField() && resourceLeakAtf.hasOwning(memberElt)) {
          owningFields.add((VariableElement) memberElt);
        }
      }
    }
  }

  /**
   * Creates a MustCallInference instance and runs the inference algorithm. This method is called by
   * the {@link ResourceLeakAnnotatedTypeFactory#postAnalyze} method if Whole Program Inference is
   * enabled.
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
   * Runs the inference algorithm on the current method (the {@link #cfg} field). It discovers
   * annotations such as {@code @}{@link Owning} on owning fields and parameters, {@code @}{@link
   * NotOwning} on return types, {@code @}{@link EnsuresCalledMethods} on methods, {@code @}{@link
   * MustCallAlias} on parameters and return types, and {@code @}{@link InheritableMustCall} on
   * class declarations.
   *
   * <p>Operationally, it checks method invocations for fields and parameters (with
   * non-empty @MustCall obligations) along all paths to the regular exit point.
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

      // Use a LinkedHashSet for determinism.
      Set<Obligation> obligations = new LinkedHashSet<>(current.obligations);

      for (Node node : current.block.getNodes()) {
        // The obligation set calculated for RLC differs from the Inference process. In the
        // Inference process, it exclusively tracks parameters with non-empty must-call
        // types, whether they have the @Owning annotation or not. However, there are some
        // shared computations, such as updateObligationsWithInvocationResult, which is used
        // during inference and could potentially affect the RLC result if it were called
        // before the checking phase. However, calling
        // updateObligationsWithInvocationResult() will not have any side effects on the
        // outcome of the Resource Leak Checker. This is because the inference occurs within
        // the postAnalyze method of the ResourceLeakAnnotatedTypeFactory, once the
        // consistency analyzer has completed its process.
        if (node instanceof MethodInvocationNode || node instanceof ObjectCreationNode) {
          if (mcca.shouldTrackInvocationResult(obligations, node, true)) {
            mcca.updateObligationsWithInvocationResult(obligations, node);
          }
          inferOwningFromInvocation(obligations, node);
        } else if (node instanceof AssignmentNode) {
          analyzeAssignmentNode(obligations, (AssignmentNode) node);
        } else if (node instanceof ReturnNode) {
          analyzeReturnNode(obligations, (ReturnNode) node);
        }
      }

      addNonExceptionalSuccessorsToWorklist(obligations, current.block, visited, worklist);
    }

    addMemberAndClassAnnotations();
  }

  /**
   * Analyzes a return statement and performs two computations. Does nothing if the return
   * expression's type has an empty must-call obligation.
   *
   * <ul>
   *   <li>If the returned expression is a field with non-empty must-call obligations, adds a {@link
   *       NotOwning} annotation to the return type of the current method. Note the implication: if
   *       a method returns a field of this class at <i>any</i> return site, the return type is
   *       inferred to be non-owning.
   *   <li>Compute the index of the parameter that is an alias of the return node and add it the
   *       {@link #returnObligationToParameter} map.
   * </ul>
   *
   * @param obligations the current set of tracked Obligations
   * @param node the return node
   */
  private void analyzeReturnNode(Set<Obligation> obligations, ReturnNode node) {
    Node returnNode = node.getResult();
    returnNode = mcca.removeCastsAndGetTmpVarIfPresent(returnNode);
    if (resourceLeakAtf.hasEmptyMustCallValue(returnNode.getTree())) {
      return;
    }

    if (returnNode instanceof FieldAccessNode) {
      addNotOwningToMethodDecl();
    } else if (returnNode instanceof LocalVariableNode) {
      Obligation returnNodeObligation =
          MustCallConsistencyAnalyzer.getObligationForVar(
              obligations, (LocalVariableNode) returnNode);
      if (returnNodeObligation != null) {
        returnObligationToParameter.put(
            returnNodeObligation, getIndexOfParam(returnNodeObligation));
      }
    }
  }

  /**
   * Adds inferred {@literal @Owning} annotations to fields, {@literal @EnsuresCalledMethods}
   * annotations to the current method, {@literal @MustCallAlias} annotations to the parameter, and
   * {@literal @InheritableMustCall} annotations to the enclosing class.
   */
  private void addMemberAndClassAnnotations() {
    WholeProgramInference wpi = resourceLeakAtf.getWholeProgramInference();
    assert wpi != null : "MustCallInference is running without WPI.";
    for (VariableElement fieldElt : getOwningFields()) {
      wpi.addFieldDeclarationAnnotation(fieldElt, OWNING);
    }
    if (!disposedFields.isEmpty()) {
      addEnsuresCalledMethodsForDisposedFields();
    }

    // If all return statements alias the same parameter index, then add the @MustCallAlias
    // annotation to that parameter and the return type.
    if (!returnObligationToParameter.isEmpty()) {
      if (returnObligationToParameter.values().stream().distinct().count() == 1) {
        int indexOfParam = returnObligationToParameter.values().iterator().next();
        if (indexOfParam > 0) {
          addMustCallAliasToFormalParameter(indexOfParam);
        }
      }
    }

    addInheritableMustCallToClass();
  }

  /**
   * Returns a set of obligations representing the formal parameters of the current method that have
   * non-empty MustCall annotations. Returns an empty set if the given CFG doesn't correspond to a
   * method body.
   *
   * @param cfg the control flow graph of the method to check
   * @return a set of obligations representing the parameters with non-empty MustCall obligations
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
                ImmutableSet.of(
                    new ResourceAlias(new LocalVariable(paramElement), paramElement, param)),
                Collections.singleton(MethodExitKind.NORMAL_RETURN)));
      }
    }
    return result != null ? result : Collections.emptySet();
  }

  /**
   * Retrieves the owning fields, including fields inferred as owning from the current iteration.
   *
   * @return the owning fields
   */
  private Set<VariableElement> getOwningFields() {
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
   * This method checks if a field is an owning candidate. A field is an owning candidate if it has
   * a non-empty must-call obligation, unless it is {code @MustCallUnknown}. For a
   * {code @MustCallUnknown} field, we don't want to infer anything. So, we conservatively treat it
   * as a non-owning candidate.
   *
   * @param resourceLeakAtf the type factory
   * @param field the field to check
   * @return true if the field is an owning candidate, false otherwise
   */
  private boolean isFieldOwningCandidate(
      ResourceLeakAnnotatedTypeFactory resourceLeakAtf, Element field) {
    AnnotationMirror mustCallAnnotation = resourceLeakAtf.getMustCallAnnotation(field);
    if (mustCallAnnotation == null) {
      // Indicates @MustCallUnknown. We want to  conservatively avoid inferring an @Owning
      // annotation for @MustCallUnknown.
      return false;
    }
    // Otherwise, the field is an @Owning candidate if it has a non-empty @MustCall obligation
    return !resourceLeakAtf.getMustCallValues(mustCallAnnotation).isEmpty();
  }

  /**
   * Adds the node to the disposedFields map and the owningFields set if it is a field and its
   * must-call obligation is satisfied by the given method call. If so, it will be given an @Owning
   * annotation later.
   *
   * @param node possibly an owning field
   * @param invocation method invoked on the possible owning field
   */
  private void inferOwningField(Node node, MethodInvocationNode invocation) {
    Element nodeElt = TreeUtils.elementFromTree(node.getTree());
    if (nodeElt == null || !nodeElt.getKind().isField()) {
      return;
    }
    if (isFieldOwningCandidate(resourceLeakAtf, nodeElt)) {
      node = NodeUtils.removeCasts(node);
      JavaExpression nodeJe = JavaExpression.fromNode(node);
      AnnotationMirror cmAnno = getCalledMethodsAnno(invocation, nodeJe);
      List<String> mustCallValues = resourceLeakAtf.getMustCallValues(nodeElt);
      if (mcca.calledMethodsSatisfyMustCall(mustCallValues, cmAnno)) {
        assert !mustCallValues.isEmpty()
            : "Must-call obligation of owning field " + nodeElt + " is empty.";
        // TODO: generalize this to MustCall annotations with more than one element.
        // Currently, this code assumes that the must-call set has only one element.
        assert mustCallValues.size() == 1
            : "The must-call set of " + nodeElt + "should be a singleton: " + mustCallValues;
        disposedFields.put((VariableElement) nodeElt, mustCallValues.get(0));
        owningFields.add((VariableElement) nodeElt);
      }
    }
  }

  /**
   * Analyzes an assignment statement and performs the following computations. Does nothing if the
   * rhs is not a local variable or parameter, or if the rhs has no must-call obligation.
   *
   * <ul>
   *   <li>1) If the current method under analysis is a constructor, the left-hand side of the
   *       assignment is the only owning field of the enclosing class, and the rhs is an alias of a
   *       formal parameter, it adds an {@code @MustCallAlias} annotation to the formal parameter
   *       and the result type of the constructor.
   *   <li>2) If the left-hand side of the assignment is an owning field, and the rhs is an alias of
   *       a formal parameter, it adds an {@code @Owning} annotation to the formal parameter.
   *   <li>3) Otherwise, updates the set of tracked obligations to account for the
   *       (pseudo-)assignment, as in a gen-kill dataflow analysis problem.
   * </ul>
   *
   * @param obligations the set of obligations to update
   * @param assignmentNode the assignment statement
   */
  private void analyzeAssignmentNode(Set<Obligation> obligations, AssignmentNode assignmentNode) {
    Node lhs = assignmentNode.getTarget();
    Element lhsElement = TreeUtils.elementFromTree(lhs.getTree());
    // Use the temporary variable for the rhs if it exists.
    Node rhs = mcca.removeCastsAndGetTmpVarIfPresent(assignmentNode.getExpression());

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

      // If the owning field is present in the disposedFields map and there is an assignment
      // to the field, it must be removed from the set. This is essential since the
      // disposedFields map is used for adding @EnsuresCalledMethods annotations to the
      // current method later. Note that this removal doesn't affect the owning annotation
      // we inferred for the field, as the owningField set is updated with the inferred
      // owning field in the 'inferOwningField' method.
      if (!TreeUtils.isConstructor(methodTree)) {
        disposedFields.remove((VariableElement) lhsElement);
      }

      int paramIndex = getIndexOfParam(rhsObligation);
      if (paramIndex == -1) {
        // We are only tracking formal parameter aliases. If the rhsObligation is not an
        // alias of any of the formal parameters, it won't be present in the obligations
        // set. Thus, skipping the rest of this method is fine.
        return;
      }

      if (TreeUtils.isConstructor(methodTree) && getOwningFields().size() == 1) {
        // case 1 is satisfied.
        addMustCallAliasToFormalParameter(paramIndex);
        mcca.removeObligationsContainingVar(obligations, (LocalVariableNode) rhs);
      } else {
        // case 2 is satisfied.
        addOwningToParam(paramIndex);
        mcca.removeObligationsContainingVar(obligations, (LocalVariableNode) rhs);
      }

    } else if (lhs instanceof LocalVariableNode) {
      // Updates the set of tracked obligations. (case 4)
      LocalVariableNode lhsVar = (LocalVariableNode) lhs;
      mcca.updateObligationsForPseudoAssignment(obligations, assignmentNode, lhsVar, rhs);
    }
  }

  /**
   * Return the (1-based) index of the method parameter that exist in the set of aliases of the
   * given {@code obligation}, if one exists; otherwise, return -1.
   *
   * @param obligation the obligation
   * @return the index of the current method parameter that exist in the set of aliases of the given
   *     obligation, if one exists; otherwise, return -1.
   */
  private int getIndexOfParam(Obligation obligation) {
    Set<ResourceAlias> resourceAliases = obligation.resourceAliases;
    List<VariableElement> paramElts =
        CollectionsPlume.mapList(TreeUtils::elementFromDeclaration, methodTree.getParameters());
    for (ResourceAlias resourceAlias : resourceAliases) {
      int paramIndex = paramElts.indexOf(resourceAlias.element);
      if (paramIndex != -1) {
        return paramIndex + 1;
      }
    }

    return -1;
  }

  /** Adds a {@link NotOwning} annotation to the current method. */
  private void addNotOwningToMethodDecl() {
    WholeProgramInference wpi = resourceLeakAtf.getWholeProgramInference();
    wpi.addMethodDeclarationAnnotation(methodElt, NOTOWNING);
  }

  /**
   * Adds a {@link MustCallAlias} annotation to the formal parameter at the given index.
   *
   * @param index the index of a formal parameter of the current method (1-based)
   */
  private void addMustCallAliasToFormalParameter(int index) {
    WholeProgramInference wpi = resourceLeakAtf.getWholeProgramInference();
    wpi.addMethodDeclarationAnnotation(methodElt, MUSTCALLALIAS);
    wpi.addDeclarationAnnotationToFormalParameter(methodElt, index, MUSTCALLALIAS);
  }

  /**
   * Adds an {@link EnsuresCalledMethods} annotation to the current method for any owning field
   * whose must-call obligation is satisfied within the current method, i.e., the fields in {@link
   * #disposedFields}.
   */
  private void addEnsuresCalledMethodsForDisposedFields() {
    // The keys are the must-call method names, and the values are the set of fields on which
    // those methods are called. This map is used to create a @EnsuresCalledMethods annotation
    // for each set of fields that share the same must-call obligation.
    Map<String, Set<String>> methodToFields = new LinkedHashMap<>();
    for (VariableElement disposedField : disposedFields.keySet()) {
      String mustCallValue = disposedFields.get(disposedField);
      String fieldName = "this." + disposedField.getSimpleName().toString();
      methodToFields.computeIfAbsent(mustCallValue, k -> new LinkedHashSet<>()).add(fieldName);
    }

    for (String mustCallValue : methodToFields.keySet()) {
      Set<String> fields = methodToFields.get(mustCallValue);
      AnnotationMirror am =
          createEnsuresCalledMethods(fields.toArray(new String[0]), new String[] {mustCallValue});
      WholeProgramInference wpi = resourceLeakAtf.getWholeProgramInference();
      wpi.addMethodDeclarationAnnotation(methodElt, am);
    }
  }

  /**
   * Possibly adds an InheritableMustCall annotation on the enclosing class.
   *
   * <p>Let the enclosing class be C. If C already has a non-empty MustCall type (that is written or
   * inherited from one of its superclasses), this method preserves the exising must-call type to
   * avoid infinite iteration. Otherwise, if the current method is not private and satisfies the
   * must-call obligations of all the owning fields in C, it adds an InheritableMustCall annotation
   * to C.
   */
  private void addInheritableMustCallToClass() {
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

      // If the enclosing class already has a non-empty @MustCall type, either added by
      // programmers or inferred in previous iterations (not-inherited), we do not change it
      // in the current analysis round to prevent potential inconsistencies and guarantee
      // the termination of the inference algorithm. This becomes particularly important
      // when multiple methods could satisfy the must-call obligation of the enclosing
      // class. To ensure the existing @MustCall annotation is included in the inference
      // result for this iteration, we re-add it.
      assert currentMustCallValues.size() == 1 : "TODO: Handle multiple must-call values";
      AnnotationMirror am = createInheritableMustCall(new String[] {currentMustCallValues.get(0)});
      wpi.addClassDeclarationAnnotation(classElt, am);
      return;
    }

    // If the current method is not private and satisfies the must-call obligation of all owning
    // fields, then add (to the class) an InheritableMustCall annotation with the name of this
    // method.
    if (!methodTree.getModifiers().getFlags().contains(Modifier.PRIVATE)) {
      // Since the result of getOwningFields() is a superset of the key set in
      // disposedFields map, it is sufficient to check the equality of their sizes to
      // determine if both sets are equal.
      if (!disposedFields.isEmpty() && disposedFields.size() == getOwningFields().size()) {
        AnnotationMirror am =
            createInheritableMustCall(new String[] {methodTree.getName().toString()});
        wpi.addClassDeclarationAnnotation(classElt, am);
      }
    }
  }

  /**
   * Infers ownership transfer at the method call to infer @owning annotations for formal parameters
   * of the current method, if the parameter is passed into the call and the corresponding formal
   * parameter of the callee is @owning.
   *
   * @param obligations the current set of tracked Obligations
   * @param invocation the method or constructor invocation
   */
  private void inferOwningParamsViaOwnershipTransfer(Set<Obligation> obligations, Node invocation) {
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
   * @param obligations the current set of tracked Obligations
   * @param node the node
   * @param element the element
   * @return true if {@code element} is a resource alias of {@code node}
   */
  private boolean nodeAndElementResourceAliased(
      Set<Obligation> obligations, Node node, VariableElement element) {
    Set<ResourceAlias> nodeAliases = getResourceAliasOfNode(obligations, node);
    for (ResourceAlias nodeAlias : nodeAliases) {
      Element nodeAliasElt = nodeAlias.element;
      if (nodeAliasElt.equals(element)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Infers @Owning annotations for fields or the parameters of the current method that are passed
   * in the receiver or arguments position of a call if their must-call obligation is satisfied via
   * the {@code invocation}.
   *
   * @param obligations the current set of tracked Obligations
   * @param invocation a method invocation node to check
   */
  private void inferOwningForRecieverOrFormalParamPassedToCall(
      Set<Obligation> obligations, MethodInvocationNode invocation) {
    Node receiver = invocation.getTarget().getReceiver();
    receiver = NodeUtils.removeCasts(receiver);
    if (receiver.getTree() != null) {
      inferOwningForParamOrField(obligations, invocation, receiver);
    }

    for (Node argument : mcca.getArgumentsOfInvocation(invocation)) {
      Node arg = mcca.removeCastsAndGetTmpVarIfPresent(argument);
      // In the CFG, explicit passing of multiple arguments in the varargs position is
      // represented via an ArrayCreationNode. In this case, it checks the called methods
      // set of each argument passed in this position.
      if (arg instanceof ArrayCreationNode) {
        ArrayCreationNode varArgsNode = (ArrayCreationNode) arg;
        for (Node varArgNode : varArgsNode.getInitializers()) {
          inferOwningForParamOrField(obligations, invocation, varArgNode);
        }
      } else {
        inferOwningForParamOrField(obligations, invocation, arg);
      }
    }
  }

  /**
   * Infers an @Owning annotation for the {@code arg} that can be a receiver or an argument passed
   * into a method call if the must-call obligation of the {@code arg} is satisfied via the {@code
   * invocation}.
   *
   * @param obligations the current set of tracked Obligations
   * @param invocation the method invocation node to check
   * @param arg a receiver or an argument passed to the method call
   */
  private void inferOwningForParamOrField(
      Set<Obligation> obligations, MethodInvocationNode invocation, Node arg) {
    Element argElt = TreeUtils.elementFromTree(arg.getTree());
    // The must-call obligation of a field can be satisfied either through a call where it
    // serves as a receiver or within the callee method when it is passed as an argument.
    if (argElt != null && argElt.getKind().isField()) {
      inferOwningField(arg, invocation);
      return;
    }

    List<? extends VariableTree> paramsOfCurrentMethod = methodTree.getParameters();
    outerLoop:
    for (int i = 0; i < paramsOfCurrentMethod.size(); i++) {
      VariableTree currentMethodParamTree = paramsOfCurrentMethod.get(i);
      if (resourceLeakAtf.hasEmptyMustCallValue(currentMethodParamTree)) {
        continue;
      }

      VariableElement paramElt = TreeUtils.elementFromDeclaration(currentMethodParamTree);
      if (!nodeAndElementResourceAliased(obligations, arg, paramElt)) {
        continue;
      }

      List<String> mustCallValues = resourceLeakAtf.getMustCallValues(paramElt);
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
   * Infers @Owning or @MustCallAlias annotations for formal parameters of the enclosing method
   * and @Owning annotations for fields of the enclosing class, as follows:
   *
   * <ul>
   *   <li>If a formal parameter is passed as an owning parameter, add an @Owning annotation to that
   *       formal parameter (see {@link #inferOwningParamsViaOwnershipTransfer}).
   *   <li>It calls {@link #inferOwningForRecieverOrFormalParamPassedToCall} to infer @Owning
   *       annotations for the receiver or arguments of a call by analyzing the called-methods set
   *       after the call.
   *   <li>It calls {@link #inferMustCallAliasFromThisOrSuperCall} to infer @MustCallAlias
   *       annotation for formal parameters and the result of the constructor.
   * </ul>
   *
   * @param obligations the set of obligations to search in
   * @param invocation the method or constructor invocation
   */
  private void inferOwningFromInvocation(Set<Obligation> obligations, Node invocation) {
    if (invocation instanceof ObjectCreationNode) {
      // If the invocation corresponds to an object creation node, only ownership transfer
      // checking is required, as constructor parameters may have an @Owning annotation.  We
      // do not handle @EnsuresCalledMethods annotations on constructors as we have not
      // observed them in practice.
      inferOwningParamsViaOwnershipTransfer(obligations, invocation);
    } else if (invocation instanceof MethodInvocationNode) {
      inferMustCallAliasFromThisOrSuperCall(obligations, (MethodInvocationNode) invocation);
      inferOwningParamsViaOwnershipTransfer(obligations, invocation);
      inferOwningForRecieverOrFormalParamPassedToCall(
          obligations, (MethodInvocationNode) invocation);
    }
  }

  /**
   * Adds the @MustCallAlias annotation to a method parameter when it is passed in a @MustCallAlias
   * position during a constructor call using {@literal this} or {@literal super}.
   *
   * @param obligations the current set of tracked Obligations
   * @param node a method invocation node
   */
  private void inferMustCallAliasFromThisOrSuperCall(
      Set<Obligation> obligations, MethodInvocationNode node) {
    if (!TreeUtils.isSuperConstructorCall(node.getTree())
        && !TreeUtils.isThisConstructorCall(node.getTree())) {
      return;
    }
    List<? extends VariableElement> calleeParams = mcca.getParametersOfInvocation(node);
    List<Node> arguments = mcca.getArgumentsOfInvocation(node);
    for (int i = 0; i < arguments.size(); i++) {
      if (!resourceLeakAtf.hasMustCallAlias(calleeParams.get(i))) {
        continue;
      }

      Node arg = mcca.removeCastsAndGetTmpVarIfPresent(arguments.get(i));
      if (!(arg instanceof LocalVariableNode)) {
        continue;
      }
      Obligation argObligation =
          MustCallConsistencyAnalyzer.getObligationForVar(obligations, (LocalVariableNode) arg);
      if (argObligation == null) {
        continue;
      }
      int index = getIndexOfParam(argObligation);
      if (index != -1) {
        addMustCallAliasToFormalParameter(index);
        break;
      }
    }
  }

  /**
   * Returns the called methods annotation for the given Java expression after the invocation node.
   *
   * @param invocation the MethodInvocationNode
   * @param varJe a Java expression
   * @return the called methods annotation for the {@code varJe} after the {@code invocation} node
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
   * Adds all non-exceptional successors to {@code worklist}.
   *
   * @param obligations the current set of tracked Obligations
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
      if (successor.getType() != Block.BlockType.SPECIAL_BLOCK) {
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
