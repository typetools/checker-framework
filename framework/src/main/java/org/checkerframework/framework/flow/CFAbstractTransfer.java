package org.checkerframework.framework.flow;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.ForwardTransferFunction;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGLambda;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.dataflow.cfg.node.AbstractNodeVisitor;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.CaseNode;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.ConditionalNotNode;
import org.checkerframework.dataflow.cfg.node.EqualToNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.InstanceOfNode;
import org.checkerframework.dataflow.cfg.node.LambdaResultExpressionNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.NarrowingConversionNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NotEqualNode;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.cfg.node.StringConcatenateAssignmentNode;
import org.checkerframework.dataflow.cfg.node.StringConversionNode;
import org.checkerframework.dataflow.cfg.node.TernaryExpressionNode;
import org.checkerframework.dataflow.cfg.node.ThisNode;
import org.checkerframework.dataflow.cfg.node.VariableDeclarationNode;
import org.checkerframework.dataflow.cfg.node.WideningConversionNode;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.Contract;
import org.checkerframework.framework.util.Contract.ConditionalPostcondition;
import org.checkerframework.framework.util.Contract.Postcondition;
import org.checkerframework.framework.util.Contract.Precondition;
import org.checkerframework.framework.util.ContractsFromMethod;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The default analysis transfer function for the Checker Framework propagates information through
 * assignments and uses the {@link AnnotatedTypeFactory} to provide checker-specific logic how to
 * combine types (e.g., what is the type of a string concatenation, given the types of the two
 * operands) and as an abstraction function (e.g., determine the annotations on literals).
 *
 * <p>Design note: CFAbstractTransfer and its subclasses are supposed to act as transfer functions.
 * But, since the AnnotatedTypeFactory already existed and performed checker-independent type
 * propagation, CFAbstractTransfer delegates work to it instead of duplicating some logic in
 * CFAbstractTransfer. The checker-specific subclasses of CFAbstractTransfer do implement transfer
 * function logic themselves.
 */
public abstract class CFAbstractTransfer<
        V extends CFAbstractValue<V>,
        S extends CFAbstractStore<V, S>,
        T extends CFAbstractTransfer<V, S, T>>
    extends AbstractNodeVisitor<TransferResult<V, S>, TransferInput<V, S>>
    implements ForwardTransferFunction<V, S> {

  /** The analysis used by this transfer function. */
  protected final CFAbstractAnalysis<V, S, T> analysis;

  /**
   * Should the analysis use sequential Java semantics (i.e., assume that only one thread is running
   * at all times)?
   */
  protected final boolean sequentialSemantics;

  /** Indicates that the whole-program inference is on. */
  private final boolean infer;

  /**
   * Create a CFAbstractTransfer.
   *
   * @param analysis the analysis used by this transfer function
   */
  protected CFAbstractTransfer(CFAbstractAnalysis<V, S, T> analysis) {
    this(analysis, false);
  }

  /**
   * Constructor that allows forcing concurrent semantics to be on for this instance of
   * CFAbstractTransfer.
   *
   * @param analysis the analysis used by this transfer function
   * @param forceConcurrentSemantics whether concurrent semantics should be forced to be on. If
   *     false, concurrent semantics are turned off by default, but the user can still turn them on
   *     via {@code -AconcurrentSemantics}. If true, the user cannot turn off concurrent semantics.
   */
  protected CFAbstractTransfer(
      CFAbstractAnalysis<V, S, T> analysis, boolean forceConcurrentSemantics) {
    this.analysis = analysis;
    this.sequentialSemantics =
        !(forceConcurrentSemantics || analysis.checker.hasOption("concurrentSemantics"));
    this.infer = analysis.checker.hasOption("infer");
  }

  /**
   * Returns true if the transfer function uses sequential semantics, false if it uses concurrent
   * semantics. Useful when creating an empty store, since a store makes different decisions
   * depending on whether sequential or concurrent semantics are used.
   *
   * @return true if the transfer function uses sequential semantics, false if it uses concurrent
   *     semantics
   */
  public boolean usesSequentialSemantics() {
    return sequentialSemantics;
  }

  /**
   * A hook for subclasses to modify the result of the transfer function. This method is called
   * before returning the abstract value {@code value} as the result of the transfer function.
   *
   * <p>If a subclass overrides this method, the subclass should also override {@link
   * #finishValue(CFAbstractValue,CFAbstractStore,CFAbstractStore)}.
   *
   * @param value a value to possibly modify
   * @param store the store
   * @return the possibly-modified value
   */
  protected @Nullable V finishValue(@Nullable V value, S store) {
    return value;
  }

  /**
   * A hook for subclasses to modify the result of the transfer function. This method is called
   * before returning the abstract value {@code value} as the result of the transfer function.
   *
   * <p>If a subclass overrides this method, the subclass should also override {@link
   * #finishValue(CFAbstractValue,CFAbstractStore)}.
   *
   * @param value the value to finish
   * @param thenStore the "then" store
   * @param elseStore the "else" store
   * @return the possibly-modified value
   */
  protected @Nullable V finishValue(@Nullable V value, S thenStore, S elseStore) {
    return value;
  }

  /**
   * Returns the abstract value of a non-leaf tree {@code tree}, as computed by the {@link
   * AnnotatedTypeFactory}.
   *
   * @return the abstract value of a non-leaf tree {@code tree}, as computed by the {@link
   *     AnnotatedTypeFactory}
   */
  protected V getValueFromFactory(Tree tree, Node node) {
    GenericAnnotatedTypeFactory<V, S, T, ? extends CFAbstractAnalysis<V, S, T>> factory =
        analysis.atypeFactory;
    Tree preTree = analysis.getCurrentTree();
    Pair<Tree, AnnotatedTypeMirror> preContext = factory.getVisitorState().getAssignmentContext();
    analysis.setCurrentTree(tree);
    // is there an assignment context node available?
    if (node != null && node.getAssignmentContext() != null) {
      // Get the declared type of the assignment context by looking up the assignment context tree's
      // type in the factory while flow is disabled.
      Tree contextTree = node.getAssignmentContext().getContextTree();
      AnnotatedTypeMirror assignmentContext = null;
      if (contextTree != null) {
        assignmentContext = factory.getAnnotatedTypeLhs(contextTree);
      } else {
        Element assignmentContextElement = node.getAssignmentContext().getElementForType();
        if (assignmentContextElement != null) {
          // if contextTree is null, use the element to get the type
          assignmentContext = factory.getAnnotatedType(assignmentContextElement);
        }
      }

      if (assignmentContext != null) {
        if (assignmentContext instanceof AnnotatedExecutableType) {
          // For a MethodReturnContext, we get the full type of the
          // method, but we only want the return type.
          assignmentContext = ((AnnotatedExecutableType) assignmentContext).getReturnType();
        }
        factory
            .getVisitorState()
            .setAssignmentContext(
                Pair.of(node.getAssignmentContext().getContextTree(), assignmentContext));
      }
    }
    AnnotatedTypeMirror at;
    if (node instanceof MethodInvocationNode
        && ((MethodInvocationNode) node).getIterableExpression() != null) {
      ExpressionTree iter = ((MethodInvocationNode) node).getIterableExpression();
      at = factory.getIterableElementType(iter);
    } else if (node instanceof ArrayAccessNode
        && ((ArrayAccessNode) node).getArrayExpression() != null) {
      ExpressionTree array = ((ArrayAccessNode) node).getArrayExpression();
      at = factory.getIterableElementType(array);
    } else {
      at = factory.getAnnotatedType(tree);
    }
    analysis.setCurrentTree(preTree);
    factory.getVisitorState().setAssignmentContext(preContext);
    return analysis.createAbstractValue(at);
  }

  /**
   * Returns an abstract value with the given {@code type} and the annotations from {@code
   * annotatedValue}.
   *
   * @param type the type to return
   * @param annotatedValue the annotations to return
   * @return an abstract value with the given {@code type} and the annotations from {@code
   *     annotatedValue}
   * @deprecated use {@link #getWidenedValue} or {@link #getNarrowedValue}
   */
  @Deprecated // 2020-10-02
  protected V getValueWithSameAnnotations(TypeMirror type, V annotatedValue) {
    if (annotatedValue == null) {
      return null;
    }
    return analysis.createAbstractValue(annotatedValue.getAnnotations(), type);
  }

  /** The fixed initial store. */
  private S fixedInitialStore = null;

  /** Set a fixed initial Store. */
  public void setFixedInitialStore(S s) {
    fixedInitialStore = s;
  }

  /** The initial store maps method formal parameters to their currently most refined type. */
  @Override
  public S initialStore(UnderlyingAST underlyingAST, @Nullable List<LocalVariableNode> parameters) {
    if (underlyingAST.getKind() != UnderlyingAST.Kind.LAMBDA
        && underlyingAST.getKind() != UnderlyingAST.Kind.METHOD) {
      if (fixedInitialStore != null) {
        return fixedInitialStore;
      } else {
        return analysis.createEmptyStore(sequentialSemantics);
      }
    }

    S info;

    if (underlyingAST.getKind() == UnderlyingAST.Kind.METHOD) {

      if (fixedInitialStore != null) {
        // copy knowledge
        info = analysis.createCopiedStore(fixedInitialStore);
      } else {
        info = analysis.createEmptyStore(sequentialSemantics);
      }

      AnnotatedTypeFactory factory = analysis.getTypeFactory();
      for (LocalVariableNode p : parameters) {
        AnnotatedTypeMirror anno = factory.getAnnotatedType(p.getElement());
        info.initializeMethodParameter(p, analysis.createAbstractValue(anno));
      }

      // add properties known through precondition
      CFGMethod method = (CFGMethod) underlyingAST;
      MethodTree methodDeclTree = method.getMethod();
      ExecutableElement methodElem = TreeUtils.elementFromDeclaration(methodDeclTree);
      addInformationFromPreconditions(info, factory, method, methodDeclTree, methodElem);

      addFieldValues(info, method.getClassTree(), methodDeclTree);

      addFinalLocalValues(info, methodElem);

      if (shouldPerformWholeProgramInference(methodDeclTree, methodElem)) {
        Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
            AnnotatedTypes.overriddenMethods(
                analysis.atypeFactory.getElementUtils(), analysis.atypeFactory, methodElem);
        for (Map.Entry<AnnotatedDeclaredType, ExecutableElement> pair :
            overriddenMethods.entrySet()) {
          AnnotatedExecutableType overriddenMethod =
              AnnotatedTypes.asMemberOf(
                  analysis.atypeFactory.getProcessingEnv().getTypeUtils(),
                  analysis.atypeFactory,
                  pair.getKey(),
                  pair.getValue());

          // Infers parameter and receiver types of the method based
          // on the overridden method.
          analysis
              .atypeFactory
              .getWholeProgramInference()
              .updateFromOverride(methodDeclTree, methodElem, overriddenMethod);
        }
      }

    } else if (underlyingAST.getKind() == UnderlyingAST.Kind.LAMBDA) {
      // Create a copy and keep only the field values (nothing else applies).
      info = analysis.createCopiedStore(fixedInitialStore);
      // Allow that local variables are retained; they are effectively final,
      // otherwise Java wouldn't allow access from within the lambda.
      // TODO: what about the other information? Can code further down be simplified?
      // info.localVariableValues.clear();
      info.classValues.clear();
      info.arrayValues.clear();
      info.methodValues.clear();

      AnnotatedTypeFactory factory = analysis.getTypeFactory();
      for (LocalVariableNode p : parameters) {
        AnnotatedTypeMirror anno = factory.getAnnotatedType(p.getElement());
        info.initializeMethodParameter(p, analysis.createAbstractValue(anno));
      }

      CFGLambda lambda = (CFGLambda) underlyingAST;
      @SuppressWarnings("interning:assignment") // used in == tests
      @InternedDistinct Tree enclosingTree =
          TreePathUtil.enclosingOfKind(
              factory.getPath(lambda.getLambdaTree()),
              new HashSet<>(
                  Arrays.asList(
                      Tree.Kind.METHOD,
                      // Tree.Kind for which TreeUtils.isClassTree is true
                      Tree.Kind.CLASS,
                      Tree.Kind.INTERFACE,
                      Tree.Kind.ANNOTATION_TYPE,
                      Tree.Kind.ENUM)));

      Element enclosingElement = null;
      if (enclosingTree.getKind() == Tree.Kind.METHOD) {
        // If it is in an initializer, we need to use locals from the initializer.
        enclosingElement = TreeUtils.elementFromTree(enclosingTree);

      } else if (TreeUtils.isClassTree(enclosingTree)) {

        // Try to find an enclosing initializer block.
        // Would love to know if there was a better way.
        // Find any enclosing element of the lambda (using trees).
        // Then go up the elements to find an initializer element (which can't be found with the
        // tree).
        TreePath loopTree = factory.getPath(lambda.getLambdaTree()).getParentPath();
        Element anEnclosingElement = null;
        while (loopTree.getLeaf() != enclosingTree) {
          Element sym = TreeUtils.elementFromTree(loopTree.getLeaf());
          if (sym != null) {
            anEnclosingElement = sym;
            break;
          }
          loopTree = loopTree.getParentPath();
        }
        while (anEnclosingElement != null
            && !anEnclosingElement.equals(TreeUtils.elementFromTree(enclosingTree))) {
          if (anEnclosingElement.getKind() == ElementKind.INSTANCE_INIT
              || anEnclosingElement.getKind() == ElementKind.STATIC_INIT) {
            enclosingElement = anEnclosingElement;
            break;
          }
          anEnclosingElement = anEnclosingElement.getEnclosingElement();
        }
      }
      if (enclosingElement != null) {
        addFinalLocalValues(info, enclosingElement);
      }

      // We want the initialization stuff, but need to throw out any refinements.
      Map<FieldAccess, V> fieldValuesClone = new HashMap<>(info.fieldValues);
      for (Map.Entry<FieldAccess, V> fieldValue : fieldValuesClone.entrySet()) {
        AnnotatedTypeMirror declaredType = factory.getAnnotatedType(fieldValue.getKey().getField());
        V lubbedValue =
            analysis.createAbstractValue(declaredType).leastUpperBound(fieldValue.getValue());
        info.fieldValues.put(fieldValue.getKey(), lubbedValue);
      }
    } else {
      assert false : "Unexpected tree: " + underlyingAST;
      info = null;
    }

    return info;
  }

  /**
   * Add field values to the initial store before {@code methodTree}.
   *
   * @param info initial store
   * @param methodTree the method or constructor tree
   */
  private void addFieldValues(S info, ClassTree classTree, MethodTree methodTree) {
    boolean constructor = TreeUtils.isConstructor(methodTree);
    List<Pair<FieldAccess, Pair<V, V>>> fields = analysis.getFieldValues();
    TypeElement classEle = TreeUtils.elementFromDeclaration(classTree);
    for (Pair<FieldAccess, Pair<V, V>> p : fields) {
      FieldAccess field = p.first;
      VariableElement varEle = p.first.getField();
      V declared = p.second.first;
      V init = p.second.second;
      if (init != null
          && varEle.getModifiers().contains(Modifier.PRIVATE)
          && ElementUtils.isFinal(varEle)
          && analysis.atypeFactory.isImmutable(ElementUtils.getType(varEle))) {
        // Insert the value from the initializer of private final fields.
        info.insertValue(field, init);
      }

      // Insert some of the declared types:
      // If it's not a constructor, use the declared type if the r.
      // If it is a constructor, then only use the declared type if the field has been
      // initialized.
      boolean isInitializedReceiver = !isNotFullyInitializedReceiver(methodTree);
      if (isInitializedReceiver && (!constructor || init != null)) {
        // Only insert declared types
        if (field.getField().getEnclosingElement().equals(classEle)) {
          info.insertValue(field, declared);
        }
      }
    }
  }

  private void addFinalLocalValues(S info, Element enclosingElement) {
    // add information about effectively final variables (from outer scopes)
    for (Map.Entry<Element, V> e : analysis.atypeFactory.getFinalLocalValues().entrySet()) {

      Element elem = e.getKey();

      // TODO: There is a design flaw where the values of final local values leaks
      // into other methods of the same class. For example, in
      // class a { void b() {...} void c() {...} }
      // final local values from b() would be visible in the store for c(),
      // even though they should only be visible in b() and in classes
      // defined inside the method body of b().
      // This is partly because GenericAnnotatedTypeFactory.performFlowAnalysis does not call itself
      // recursively to analyze inner classes, but instead pops classes off of a queue, and the
      // information about known final local values is stored by GenericAnnotatedTypeFactory.analyze
      // in GenericAnnotatedTypeFactory.flowResult, which is visible to all classes in the queue
      // regardless of their level of recursion.

      // We work around this here by ensuring that we only add a final local value to a method's
      // store if that method is enclosed by the method where the local variables were declared.

      // Find the enclosing method of the element
      Element enclosingMethodOfVariableDeclaration = elem.getEnclosingElement();

      if (enclosingMethodOfVariableDeclaration != null) {

        // Now find all the enclosing methods of the code we are analyzing. If any one of
        // them matches the above, then the final local variable value applies.
        Element enclosingMethodOfCurrentMethod = enclosingElement;

        while (enclosingMethodOfCurrentMethod != null) {
          if (enclosingMethodOfVariableDeclaration.equals(enclosingMethodOfCurrentMethod)) {
            LocalVariable l = new LocalVariable(elem);
            info.insertValue(l, e.getValue());
            break;
          }

          enclosingMethodOfCurrentMethod = enclosingMethodOfCurrentMethod.getEnclosingElement();
        }
      }
    }
  }

  /**
   * Returns true if the receiver of a method or constructor might not yet be fully initialized.
   *
   * @param methodDeclTree the declaration of the method or constructor
   * @return true if the receiver of a method or constructor might not yet be fully initialized
   */
  protected boolean isNotFullyInitializedReceiver(MethodTree methodDeclTree) {
    return TreeUtils.isConstructor(methodDeclTree);
  }

  /**
   * Add the information from all the preconditions of a method to the initial store in the method
   * body.
   *
   * @param initialStore the initial store for the method body
   * @param factory the type factory
   * @param methodAst the AST for a method declaration
   * @param methodDeclTree the declaration of the method; is a field of {@code methodAst}
   * @param methodElement the element for the method
   */
  protected void addInformationFromPreconditions(
      S initialStore,
      AnnotatedTypeFactory factory,
      CFGMethod methodAst,
      MethodTree methodDeclTree,
      ExecutableElement methodElement) {
    ContractsFromMethod contractsUtils = analysis.atypeFactory.getContractsFromMethod();
    Set<Precondition> preconditions = contractsUtils.getPreconditions(methodElement);
    StringToJavaExpression stringToJavaExpr =
        stringExpr ->
            StringToJavaExpression.atMethodBody(stringExpr, methodDeclTree, analysis.checker);
    for (Precondition p : preconditions) {
      String stringExpr = p.expressionString;
      AnnotationMirror annotation =
          p.viewpointAdaptDependentTypeAnnotation(
              analysis.atypeFactory, stringToJavaExpr, /*errorTree=*/ null);
      JavaExpression exprJe;
      try {
        // TODO: currently, these expressions are parsed at the declaration (i.e. here) and for
        // every use. this could be optimized to store the result the first time.
        // (same for other annotations)
        exprJe = StringToJavaExpression.atMethodBody(stringExpr, methodDeclTree, analysis.checker);
      } catch (JavaExpressionParseException e) {
        // Errors are reported by BaseTypeVisitor.checkContractsAtMethodDeclaration().
        continue;
      }
      initialStore.insertValuePermitNondeterministic(exprJe, annotation);
    }
  }

  /**
   * The default visitor returns the input information unchanged, or in the case of conditional
   * input information, merged.
   */
  @Override
  public TransferResult<V, S> visitNode(Node n, TransferInput<V, S> in) {
    V value = null;

    // TODO: handle implicit/explicit this and go to correct factory method
    Tree tree = n.getTree();
    if (tree != null) {
      if (TreeUtils.canHaveTypeAnnotation(tree)) {
        value = getValueFromFactory(tree, n);
      }
    }

    return createTransferResult(value, in);
  }

  /**
   * Creates a TransferResult.
   *
   * <p>This default implementation returns the input information unchanged, or in the case of
   * conditional input information, merged.
   *
   * @param value the value; possibly null
   * @param in the transfer input
   * @return the input information, as a TransferResult
   */
  protected TransferResult<V, S> createTransferResult(@Nullable V value, TransferInput<V, S> in) {
    if (in.containsTwoStores()) {
      S thenStore = in.getThenStore();
      S elseStore = in.getElseStore();
      return new ConditionalTransferResult<>(
          finishValue(value, thenStore, elseStore), thenStore, elseStore);
    } else {
      S info = in.getRegularStore();
      return new RegularTransferResult<>(finishValue(value, info), info);
    }
  }

  /**
   * Creates a TransferResult just like the given one, but with the given value.
   *
   * <p>This default implementation returns the input information unchanged, or in the case of
   * conditional input information, merged.
   *
   * @param value the value; possibly null
   * @param in the TransferResult to copy
   * @return the input informatio
   */
  protected TransferResult<V, S> recreateTransferResult(
      @Nullable V value, TransferResult<V, S> in) {
    if (in.containsTwoStores()) {
      S thenStore = in.getThenStore();
      S elseStore = in.getElseStore();
      return new ConditionalTransferResult<>(
          finishValue(value, thenStore, elseStore), thenStore, elseStore);
    } else {
      S info = in.getRegularStore();
      return new RegularTransferResult<>(finishValue(value, info), info);
    }
  }

  @Override
  public TransferResult<V, S> visitClassName(ClassNameNode n, TransferInput<V, S> in) {
    // The tree underlying a class name is a type tree.
    V value = null;

    Tree tree = n.getTree();
    if (tree != null) {
      if (TreeUtils.canHaveTypeAnnotation(tree)) {
        GenericAnnotatedTypeFactory<V, S, T, ? extends CFAbstractAnalysis<V, S, T>> factory =
            analysis.atypeFactory;
        analysis.setCurrentTree(tree);
        AnnotatedTypeMirror at = factory.getAnnotatedTypeFromTypeTree(tree);
        analysis.setCurrentTree(null);
        value = analysis.createAbstractValue(at);
      }
    }

    return createTransferResult(value, in);
  }

  @Override
  public TransferResult<V, S> visitFieldAccess(FieldAccessNode n, TransferInput<V, S> p) {
    S store = p.getRegularStore();
    V storeValue = store.getValue(n);
    // look up value in factory, and take the more specific one
    // TODO: handle cases, where this is not allowed (e.g. constructors in non-null type systems)
    V factoryValue = getValueFromFactory(n.getTree(), n);
    V value = moreSpecificValue(factoryValue, storeValue);
    return new RegularTransferResult<>(finishValue(value, store), store);
  }

  @Override
  public TransferResult<V, S> visitArrayAccess(ArrayAccessNode n, TransferInput<V, S> p) {
    S store = p.getRegularStore();
    V storeValue = store.getValue(n);
    // look up value in factory, and take the more specific one
    V factoryValue = getValueFromFactory(n.getTree(), n);
    V value = moreSpecificValue(factoryValue, storeValue);
    return new RegularTransferResult<>(finishValue(value, store), store);
  }

  /** Use the most specific type information available according to the store. */
  @Override
  public TransferResult<V, S> visitLocalVariable(LocalVariableNode n, TransferInput<V, S> in) {
    S store = in.getRegularStore();
    V valueFromStore = store.getValue(n);
    V valueFromFactory = getValueFromFactory(n.getTree(), n);
    V value = moreSpecificValue(valueFromFactory, valueFromStore);
    return new RegularTransferResult<>(finishValue(value, store), store);
  }

  @Override
  public TransferResult<V, S> visitThis(ThisNode n, TransferInput<V, S> in) {
    S store = in.getRegularStore();
    V valueFromStore = store.getValue(n);

    V valueFromFactory = null;
    V value = null;
    Tree tree = n.getTree();
    if (tree != null && TreeUtils.canHaveTypeAnnotation(tree)) {
      valueFromFactory = getValueFromFactory(tree, n);
    }

    if (valueFromFactory == null) {
      value = valueFromStore;
    } else {
      value = moreSpecificValue(valueFromFactory, valueFromStore);
    }

    return new RegularTransferResult<>(finishValue(value, store), store);
  }

  @Override
  public TransferResult<V, S> visitTernaryExpression(
      TernaryExpressionNode n, TransferInput<V, S> p) {
    TransferResult<V, S> result = super.visitTernaryExpression(n, p);
    S thenStore = result.getThenStore();
    S elseStore = result.getElseStore();
    V thenValue = p.getValueOfSubNode(n.getThenOperand());
    V elseValue = p.getValueOfSubNode(n.getElseOperand());
    V resultValue = null;
    if (thenValue != null && elseValue != null) {
      // The resulting abstract value is the merge of the 'then' and 'else' branch.
      resultValue = thenValue.leastUpperBound(elseValue);
    }
    V finishedValue = finishValue(resultValue, thenStore, elseStore);
    return new ConditionalTransferResult<>(finishedValue, thenStore, elseStore);
  }

  /** Reverse the role of the 'thenStore' and 'elseStore'. */
  @Override
  public TransferResult<V, S> visitConditionalNot(ConditionalNotNode n, TransferInput<V, S> p) {
    TransferResult<V, S> result = super.visitConditionalNot(n, p);
    S thenStore = result.getThenStore();
    S elseStore = result.getElseStore();
    return new ConditionalTransferResult<>(result.getResultValue(), elseStore, thenStore);
  }

  @Override
  public TransferResult<V, S> visitEqualTo(EqualToNode n, TransferInput<V, S> p) {
    TransferResult<V, S> res = super.visitEqualTo(n, p);

    Node leftN = n.getLeftOperand();
    Node rightN = n.getRightOperand();
    V leftV = p.getValueOfSubNode(leftN);
    V rightV = p.getValueOfSubNode(rightN);

    if (res.containsTwoStores()
        && (NodeUtils.isConstantBoolean(leftN, false)
            || NodeUtils.isConstantBoolean(rightN, false))) {
      S thenStore = res.getElseStore();
      S elseStore = res.getThenStore();
      res = new ConditionalTransferResult<>(res.getResultValue(), thenStore, elseStore);
    }

    // if annotations differ, use the one that is more precise for both
    // sides (and add it to the store if possible)
    res = strengthenAnnotationOfEqualTo(res, leftN, rightN, leftV, rightV, false);
    res = strengthenAnnotationOfEqualTo(res, rightN, leftN, rightV, leftV, false);
    return res;
  }

  @Override
  public TransferResult<V, S> visitNotEqual(NotEqualNode n, TransferInput<V, S> p) {
    TransferResult<V, S> res = super.visitNotEqual(n, p);

    Node leftN = n.getLeftOperand();
    Node rightN = n.getRightOperand();
    V leftV = p.getValueOfSubNode(leftN);
    V rightV = p.getValueOfSubNode(rightN);

    if (res.containsTwoStores()
        && (NodeUtils.isConstantBoolean(leftN, true)
            || NodeUtils.isConstantBoolean(rightN, true))) {
      S thenStore = res.getElseStore();
      S elseStore = res.getThenStore();
      res = new ConditionalTransferResult<>(res.getResultValue(), thenStore, elseStore);
    }

    // if annotations differ, use the one that is more precise for both
    // sides (and add it to the store if possible)
    res = strengthenAnnotationOfEqualTo(res, leftN, rightN, leftV, rightV, true);
    res = strengthenAnnotationOfEqualTo(res, rightN, leftN, rightV, leftV, true);

    return res;
  }

  /**
   * Refine the annotation of {@code secondNode} if the annotation {@code secondValue} is less
   * precise than {@code firstValue}. This is possible, if {@code secondNode} is an expression that
   * is tracked by the store (e.g., a local variable or a field). Clients usually call this twice
   * with {@code firstNode} and {@code secondNode} reversed, to refine each of them.
   *
   * <p>Note that when overriding this method, when a new type is inserted into the store, {@link
   * #splitAssignments} should be called, and the new type should be inserted into the store for
   * each of the resulting nodes.
   *
   * @param firstNode the node that might be more precise
   * @param secondNode the node whose type to possibly refine
   * @param firstValue the abstract value that might be more precise
   * @param secondValue the abstract value that might be less precise
   * @param res the previous result
   * @param notEqualTo if true, indicates that the logic is flipped (i.e., the information is added
   *     to the {@code elseStore} instead of the {@code thenStore}) for a not-equal comparison.
   * @return the conditional transfer result (if information has been added), or {@code null}
   */
  protected TransferResult<V, S> strengthenAnnotationOfEqualTo(
      TransferResult<V, S> res,
      Node firstNode,
      Node secondNode,
      V firstValue,
      V secondValue,
      boolean notEqualTo) {
    if (firstValue != null) {
      // Only need to insert if the second value is actually different.
      if (!firstValue.equals(secondValue)) {
        List<Node> secondParts = splitAssignments(secondNode);
        for (Node secondPart : secondParts) {
          JavaExpression secondInternal = JavaExpression.fromNode(secondPart);
          if (!secondInternal.isDeterministic(analysis.atypeFactory)) {
            continue;
          }
          if (CFAbstractStore.canInsertJavaExpression(secondInternal)) {
            S thenStore = res.getThenStore();
            S elseStore = res.getElseStore();
            if (notEqualTo) {
              elseStore.insertValue(secondInternal, firstValue);
            } else {
              thenStore.insertValue(secondInternal, firstValue);
            }
            // To handle `(a = b = c) == x`, repeat for all insertable receivers of
            // splitted assignments instead of returning.
            res = new ConditionalTransferResult<>(res.getResultValue(), thenStore, elseStore);
          }
        }
      }
    }
    return res;
  }

  /**
   * Takes a node, and either returns the node itself again (as a singleton list), or if the node is
   * an assignment node, returns the lhs and rhs (where splitAssignments is applied recursively to
   * the rhs -- that is, it is possible that the rhs does not appear in the result, but rather its
   * lhs and rhs do).
   *
   * @param node possibly an assignment node
   * @return a list containing all the right- and left-hand sides in the given assignment node; it
   *     contains just the node itself if it is not an assignment)
   */
  protected List<Node> splitAssignments(Node node) {
    if (node instanceof AssignmentNode) {
      List<Node> result = new ArrayList<>(2);
      AssignmentNode a = (AssignmentNode) node;
      result.add(a.getTarget());
      result.addAll(splitAssignments(a.getExpression()));
      return result;
    } else {
      return Collections.singletonList(node);
    }
  }

  @Override
  public TransferResult<V, S> visitAssignment(AssignmentNode n, TransferInput<V, S> in) {
    Node lhs = n.getTarget();
    Node rhs = n.getExpression();

    S info = in.getRegularStore();
    V rhsValue = in.getValueOfSubNode(rhs);

    if (shouldPerformWholeProgramInference(n.getTree(), lhs.getTree())) {
      // Fields defined in interfaces are LocalVariableNodes with ElementKind of FIELD.
      if (lhs instanceof FieldAccessNode
          || (lhs instanceof LocalVariableNode
              && ((LocalVariableNode) lhs).getElement().getKind() == ElementKind.FIELD)) {
        // Updates inferred field type
        analysis
            .atypeFactory
            .getWholeProgramInference()
            .updateFromFieldAssignment(lhs, rhs, analysis.getContainingClass(n.getTree()));
      } else if (lhs instanceof LocalVariableNode
          && ((LocalVariableNode) lhs).getElement().getKind() == ElementKind.PARAMETER) {
        // lhs is a formal parameter of some method
        VariableElement param = (VariableElement) ((LocalVariableNode) lhs).getElement();
        analysis
            .atypeFactory
            .getWholeProgramInference()
            .updateFromFormalParameterAssignment((LocalVariableNode) lhs, rhs, param);
      }
    }

    processCommonAssignment(in, lhs, rhs, info, rhsValue);

    return new RegularTransferResult<>(finishValue(rhsValue, info), info);
  }

  @Override
  public TransferResult<V, S> visitReturn(ReturnNode n, TransferInput<V, S> p) {
    TransferResult<V, S> result = super.visitReturn(n, p);

    if (shouldPerformWholeProgramInference(n.getTree())) {
      // Retrieves class containing the method
      ClassTree classTree = analysis.getContainingClass(n.getTree());
      // classTree is null e.g. if this is a return statement in a lambda.
      if (classTree == null) {
        return result;
      }
      ClassSymbol classSymbol = (ClassSymbol) TreeUtils.elementFromTree(classTree);

      ExecutableElement methodElem =
          TreeUtils.elementFromDeclaration(analysis.getContainingMethod(n.getTree()));

      Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
          AnnotatedTypes.overriddenMethods(
              analysis.atypeFactory.getElementUtils(), analysis.atypeFactory, methodElem);

      // Updates the inferred return type of the method
      analysis
          .atypeFactory
          .getWholeProgramInference()
          .updateFromReturn(
              n, classSymbol, analysis.getContainingMethod(n.getTree()), overriddenMethods);
    }

    return result;
  }

  @Override
  public TransferResult<V, S> visitLambdaResultExpression(
      LambdaResultExpressionNode n, TransferInput<V, S> in) {
    return n.getResult().accept(this, in);
  }

  @Override
  public TransferResult<V, S> visitStringConcatenateAssignment(
      StringConcatenateAssignmentNode n, TransferInput<V, S> in) {
    // This gets the type of LHS + RHS
    TransferResult<V, S> result = super.visitStringConcatenateAssignment(n, in);
    Node lhs = n.getLeftOperand();
    Node rhs = n.getRightOperand();

    // update the results store if the assignment target is something we can process
    S info = result.getRegularStore();
    // ResultValue is the type of LHS + RHS
    V resultValue = result.getResultValue();

    if (lhs instanceof FieldAccessNode
        && shouldPerformWholeProgramInference(n.getTree(), lhs.getTree())) {
      // Updates inferred field type
      analysis
          .atypeFactory
          .getWholeProgramInference()
          .updateFromFieldAssignment(
              (FieldAccessNode) lhs, rhs, analysis.getContainingClass(n.getTree()));
    }

    processCommonAssignment(in, lhs, rhs, info, resultValue);

    return new RegularTransferResult<>(finishValue(resultValue, info), info);
  }

  /**
   * Determine abstract value of right-hand side and update the store accordingly to the assignment.
   */
  protected void processCommonAssignment(
      TransferInput<V, S> in, Node lhs, Node rhs, S info, V rhsValue) {

    // update information in the store
    info.updateForAssignment(lhs, rhsValue);
  }

  @Override
  public TransferResult<V, S> visitObjectCreation(ObjectCreationNode n, TransferInput<V, S> p) {
    if (shouldPerformWholeProgramInference(n.getTree())) {
      ExecutableElement constructorElt =
          analysis.getTypeFactory().constructorFromUse(n.getTree()).executableType.getElement();
      analysis
          .atypeFactory
          .getWholeProgramInference()
          .updateFromObjectCreation(n, constructorElt, p.getRegularStore());
    }
    return super.visitObjectCreation(n, p);
  }

  @Override
  public TransferResult<V, S> visitMethodInvocation(
      MethodInvocationNode n, TransferInput<V, S> in) {

    S store = in.getRegularStore();
    ExecutableElement method = n.getTarget().getMethod();

    // Perform WPI before the store has been side-effected.
    if (shouldPerformWholeProgramInference(n.getTree(), method)) {
      // Updates the inferred parameter types of the invoked method.
      analysis.atypeFactory.getWholeProgramInference().updateFromMethodInvocation(n, method, store);
    }

    Tree invocationTree = n.getTree();

    // Determine the abstract value for the method call.
    // look up the call's value from factory
    V factoryValue = (invocationTree == null) ? null : getValueFromFactory(invocationTree, n);
    // look up the call's value in the store (if possible)
    V storeValue = store.getValue(n);
    V resValue = moreSpecificValue(factoryValue, storeValue);

    store.updateForMethodCall(n, analysis.atypeFactory, resValue);

    // add new information based on postcondition
    processPostconditions(n, store, method, invocationTree);

    S thenStore = store;
    S elseStore = thenStore.copy();

    // add new information based on conditional postcondition
    processConditionalPostconditions(n, method, invocationTree, thenStore, elseStore);

    return new ConditionalTransferResult<>(
        finishValue(resValue, thenStore, elseStore), thenStore, elseStore);
  }

  @Override
  public TransferResult<V, S> visitInstanceOf(InstanceOfNode node, TransferInput<V, S> in) {
    TransferResult<V, S> result = super.visitInstanceOf(node, in);
    // The "reference type" is the type after "instanceof".
    Tree refTypeTree = node.getTree().getType();
    if (refTypeTree.getKind() == Tree.Kind.ANNOTATED_TYPE) {
      AnnotatedTypeMirror refType = analysis.atypeFactory.getAnnotatedType(refTypeTree);
      AnnotatedTypeMirror expType =
          analysis.atypeFactory.getAnnotatedType(node.getTree().getExpression());
      if (analysis.atypeFactory.getTypeHierarchy().isSubtype(refType, expType)
          && !refType.getAnnotations().equals(expType.getAnnotations())
          && !expType.getAnnotations().isEmpty()) {
        JavaExpression expr = JavaExpression.fromTree(node.getTree().getExpression());
        for (AnnotationMirror anno : refType.getAnnotations()) {
          in.getRegularStore().insertOrRefine(expr, anno);
        }
        return new RegularTransferResult<>(result.getResultValue(), in.getRegularStore());
      }
    }
    return result;
  }

  /**
   * Returns true if whole-program inference should be performed. If the tree is in the scope of
   * a @SuppressWarnings, then this method returns false.
   *
   * @param tree a tree
   * @return whether to perform whole-program inference on the tree
   */
  private boolean shouldPerformWholeProgramInference(Tree tree) {
    return infer && (tree == null || !analysis.checker.shouldSuppressWarnings(tree, ""));
  }

  /**
   * Returns true if whole-program inference should be performed. If the expressionTree or lhsTree
   * is in the scope of a @SuppressWarnings, then this method returns false.
   *
   * @param expressionTree the right-hand side of an assignment
   * @param lhsTree the left-hand side of an assignment
   * @return whether to perform whole-program inference
   */
  private boolean shouldPerformWholeProgramInference(Tree expressionTree, Tree lhsTree) {
    // Check that infer is true and the tree isn't in scope of a @SuppressWarnings
    // before calling InternalUtils.symbol(lhs).
    if (!shouldPerformWholeProgramInference(expressionTree)) {
      return false;
    }
    Element elt = TreeUtils.elementFromTree(lhsTree);
    return !analysis.checker.shouldSuppressWarnings(elt, "");
  }

  /**
   * Returns true if whole-program inference should be performed. If the tree or element is in the
   * scope of a @SuppressWarnings, then this method returns false.
   *
   * @param tree a tree
   * @param elt its element
   * @return whether to perform whole-program inference
   */
  private boolean shouldPerformWholeProgramInference(Tree tree, Element elt) {
    return shouldPerformWholeProgramInference(tree)
        && !analysis.checker.shouldSuppressWarnings(elt, "");
  }

  /**
   * Add information from the postconditions of a method to the store after an invocation.
   *
   * @param invocationNode a method call
   * @param store a store; is side-effected by this method
   * @param methodElement the method being called
   * @param invocationTree the tree for the method call
   */
  protected void processPostconditions(
      MethodInvocationNode invocationNode,
      S store,
      ExecutableElement methodElement,
      Tree invocationTree) {
    ContractsFromMethod contractsUtils = analysis.atypeFactory.getContractsFromMethod();
    Set<Postcondition> postconditions = contractsUtils.getPostconditions(methodElement);
    processPostconditionsAndConditionalPostconditions(
        invocationNode, invocationTree, store, null, postconditions);
  }

  /**
   * Add information from the conditional postconditions of a method to the stores after an
   * invocation.
   *
   * @param invocationNode a method call
   * @param methodElement the method being called
   * @param invocationTree the tree for the method call
   * @param thenStore the "then" store; is side-effected by this method
   * @param elseStore the "else" store; is side-effected by this method
   */
  protected void processConditionalPostconditions(
      MethodInvocationNode invocationNode,
      ExecutableElement methodElement,
      Tree invocationTree,
      S thenStore,
      S elseStore) {
    ContractsFromMethod contractsUtils = analysis.atypeFactory.getContractsFromMethod();
    Set<ConditionalPostcondition> conditionalPostconditions =
        contractsUtils.getConditionalPostconditions(methodElement);
    processPostconditionsAndConditionalPostconditions(
        invocationNode, invocationTree, thenStore, elseStore, conditionalPostconditions);
  }

  /**
   * Add information from the postconditions and conditional postconditions of a method to the
   * stores after an invocation.
   *
   * @param invocationNode a method call
   * @param invocationTree the tree for the method call
   * @param thenStore the "then" store; is side-effected by this method
   * @param elseStore the "else" store; is side-effected by this method
   * @param postconditions the postconditions
   */
  private void processPostconditionsAndConditionalPostconditions(
      MethodInvocationNode invocationNode,
      Tree invocationTree,
      S thenStore,
      S elseStore,
      Set<? extends Contract> postconditions) {

    StringToJavaExpression stringToJavaExpr =
        stringExpr ->
            StringToJavaExpression.atMethodInvocation(stringExpr, invocationNode, analysis.checker);
    for (Contract p : postconditions) {
      // Viewpoint-adapt to the method use (the call site).
      AnnotationMirror anno =
          p.viewpointAdaptDependentTypeAnnotation(
              analysis.atypeFactory, stringToJavaExpr, /*errorTree=*/ null);

      String expressionString = p.expressionString;
      try {
        JavaExpression je = stringToJavaExpr.toJavaExpression(expressionString);

        // "insertOrRefine" is called so that the postcondition information is added to any
        // existing information rather than replacing it.  If the called method is not
        // side-effect-free, then the values that might have been changed by the method call
        // are removed from the store before this method is called.
        if (p.kind == Contract.Kind.CONDITIONALPOSTCONDITION) {
          if (((ConditionalPostcondition) p).resultValue) {
            thenStore.insertOrRefinePermitNondeterministic(je, anno);
          } else {
            elseStore.insertOrRefinePermitNondeterministic(je, anno);
          }
        } else {
          thenStore.insertOrRefinePermitNondeterministic(je, anno);
        }
      } catch (JavaExpressionParseException e) {
        // report errors here
        if (e.isFlowParseError()) {
          Object[] args = new Object[e.args.length + 1];
          args[0] =
              ElementUtils.getSimpleSignature(TreeUtils.elementFromUse(invocationNode.getTree()));
          System.arraycopy(e.args, 0, args, 1, e.args.length);
          analysis.checker.reportError(invocationTree, "flowexpr.parse.error.postcondition", args);
        } else {
          analysis.checker.report(invocationTree, e.getDiagMessage());
        }
      }
    }
  }

  /**
   * A case produces no value, but it may imply some facts about the argument to the switch
   * statement.
   */
  @Override
  public TransferResult<V, S> visitCase(CaseNode n, TransferInput<V, S> in) {
    S store = in.getRegularStore();
    TransferResult<V, S> result =
        new ConditionalTransferResult<>(
            finishValue(null, store), in.getThenStore(), in.getElseStore(), false);

    for (Node caseOperand : n.getCaseOperands()) {
      V caseValue = in.getValueOfSubNode(caseOperand);
      AssignmentNode assign = (AssignmentNode) n.getSwitchOperand();
      V switchValue = store.getValue(JavaExpression.fromNode(assign.getTarget()));
      result =
          strengthenAnnotationOfEqualTo(
              result, caseOperand, assign.getExpression(), caseValue, switchValue, false);
      // Update value of switch temporary variable
      result =
          strengthenAnnotationOfEqualTo(
              result, caseOperand, assign.getTarget(), caseValue, switchValue, false);
    }
    return result;
  }

  /**
   * In a cast {@code (@A C) e} of some expression {@code e} to a new type {@code @A C}, we usually
   * take the annotation of the type {@code C} (here {@code @A}). However, if the inferred
   * annotation of {@code e} is more precise, we keep that one.
   */
  // @Override
  // public TransferResult<V, S> visitTypeCast(TypeCastNode n,
  // TransferInput<V, S> p) {
  // TransferResult<V, S> result = super.visitTypeCast(n, p);
  // V value = result.getResultValue();
  // V operandValue = p.getValueOfSubNode(n.getOperand());
  // // Normally we take the value of the type cast node. However, if the old
  // // flow-refined value was more precise, we keep that value.
  // V resultValue = moreSpecificValue(value, operandValue);
  // result.setResultValue(resultValue);
  // return result;
  // }

  /**
   * Returns the abstract value of {@code (value1, value2)} that is more specific. If the two are
   * incomparable, then {@code value1} is returned.
   */
  public V moreSpecificValue(V value1, V value2) {
    if (value1 == null) {
      return value2;
    }
    if (value2 == null) {
      return value1;
    }
    return value1.mostSpecific(value2, value1);
  }

  @Override
  public TransferResult<V, S> visitVariableDeclaration(
      VariableDeclarationNode n, TransferInput<V, S> p) {
    S store = p.getRegularStore();
    return new RegularTransferResult<>(finishValue(null, store), store);
  }

  @Override
  public TransferResult<V, S> visitWideningConversion(
      WideningConversionNode n, TransferInput<V, S> p) {
    TransferResult<V, S> result = super.visitWideningConversion(n, p);
    // Combine annotations from the operand with the wide type
    V operandValue = p.getValueOfSubNode(n.getOperand());
    V widenedValue = getWidenedValue(n.getType(), operandValue);
    result.setResultValue(widenedValue);
    return result;
  }

  /**
   * Returns an abstract value with the given {@code type} and the annotations from {@code
   * annotatedValue}, adapted for narrowing. This is only called at a narrowing conversion.
   *
   * @param type the type to narrow to
   * @param annotatedValue the type to narrow from
   * @return an abstract value with the given {@code type} and the annotations from {@code
   *     annotatedValue}; returns null if {@code annotatedValue} is null
   */
  protected V getNarrowedValue(TypeMirror type, V annotatedValue) {
    if (annotatedValue == null) {
      return null;
    }
    Set<AnnotationMirror> narrowedAnnos =
        analysis.atypeFactory.getNarrowedAnnotations(
            annotatedValue.getAnnotations(),
            annotatedValue.getUnderlyingType().getKind(),
            type.getKind());

    return analysis.createAbstractValue(narrowedAnnos, type);
  }

  /**
   * Returns an abstract value with the given {@code type} and the annotations from {@code
   * annotatedValue}, adapted for widening. This is only called at a widening conversion.
   *
   * @param type the type to widen to
   * @param annotatedValue the type to widen from
   * @return an abstract value with the given {@code type} and the annotations from {@code
   *     annotatedValue}; returns null if {@code annotatedValue} is null
   */
  protected V getWidenedValue(TypeMirror type, V annotatedValue) {
    if (annotatedValue == null) {
      return null;
    }
    Set<AnnotationMirror> widenedAnnos =
        analysis.atypeFactory.getWidenedAnnotations(
            annotatedValue.getAnnotations(),
            annotatedValue.getUnderlyingType().getKind(),
            type.getKind());

    return analysis.createAbstractValue(widenedAnnos, type);
  }

  @Override
  public TransferResult<V, S> visitNarrowingConversion(
      NarrowingConversionNode n, TransferInput<V, S> p) {
    TransferResult<V, S> result = super.visitNarrowingConversion(n, p);
    // Combine annotations from the operand with the narrow type
    V operandValue = p.getValueOfSubNode(n.getOperand());
    V narrowedValue = getNarrowedValue(n.getType(), operandValue);
    result.setResultValue(narrowedValue);
    return result;
  }

  @Override
  public TransferResult<V, S> visitStringConversion(StringConversionNode n, TransferInput<V, S> p) {
    TransferResult<V, S> result = super.visitStringConversion(n, p);
    result.setResultValue(p.getValueOfSubNode(n.getOperand()));
    return result;
  }

  /**
   * Inserts newAnno as the value into all stores (conditional or not) in the result for node. This
   * is a utility method for subclasses.
   *
   * @param result the TransferResult holding the stores to modify
   * @param target the receiver whose value should be modified
   * @param newAnno the new value
   */
  public static void insertIntoStores(
      TransferResult<CFValue, CFStore> result, JavaExpression target, AnnotationMirror newAnno) {
    if (result.containsTwoStores()) {
      result.getThenStore().insertValue(target, newAnno);
      result.getElseStore().insertValue(target, newAnno);
    } else {
      result.getRegularStore().insertValue(target, newAnno);
    }
  }
}
