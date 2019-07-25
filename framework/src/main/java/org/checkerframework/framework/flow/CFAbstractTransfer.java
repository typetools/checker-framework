package org.checkerframework.framework.flow;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.ClassName;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.LocalVariable;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.FlowExpressions.ThisReference;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGLambda;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.dataflow.cfg.UnderlyingAST.Kind;
import org.checkerframework.dataflow.cfg.node.AbstractNodeVisitor;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.CaseNode;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.ConditionalNotNode;
import org.checkerframework.dataflow.cfg.node.EqualToNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
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
import org.checkerframework.dataflow.cfg.node.ThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.VariableDeclarationNode;
import org.checkerframework.dataflow.cfg.node.WideningConversionNode;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.ContractsUtils;
import org.checkerframework.framework.util.ContractsUtils.ConditionalPostcondition;
import org.checkerframework.framework.util.ContractsUtils.Contract;
import org.checkerframework.framework.util.ContractsUtils.Postcondition;
import org.checkerframework.framework.util.ContractsUtils.Precondition;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
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
        implements TransferFunction<V, S> {

    /** The analysis class this store belongs to. */
    protected final CFAbstractAnalysis<V, S, T> analysis;

    /**
     * Should the analysis use sequential Java semantics (i.e., assume that only one thread is
     * running at all times)?
     */
    protected final boolean sequentialSemantics;

    /** Indicates that the whole-program inference is on. */
    private final boolean infer;

    public CFAbstractTransfer(CFAbstractAnalysis<V, S, T> analysis) {
        this.analysis = analysis;
        this.sequentialSemantics = !analysis.checker.hasOption("concurrentSemantics");
        this.infer = analysis.checker.hasOption("infer");
    }

    /**
     * Constructor that allows forcing concurrent semantics to be on for this instance of
     * CFAbstractTransfer.
     *
     * @param forceConcurrentSemantics whether concurrent semantics should be forced to be on. If
     *     false, concurrent semantics are turned off by default, but the user can still turn them
     *     on via {@code -AconcurrentSemantics}. If true, the user cannot turn off concurrent
     *     semantics.
     */
    public CFAbstractTransfer(
            CFAbstractAnalysis<V, S, T> analysis, boolean forceConcurrentSemantics) {
        this.analysis = analysis;
        this.sequentialSemantics =
                !(forceConcurrentSemantics || analysis.checker.hasOption("concurrentSemantics"));
        this.infer = analysis.checker.hasOption("infer");
    }

    /**
     * @return true if the transfer function uses sequential semantics, false if it uses concurrent
     *     semantics. Useful when creating an empty store, since a store makes different decisions
     *     depending on whether sequential or concurrent semantics are used.
     */
    public boolean usesSequentialSemantics() {
        return sequentialSemantics;
    }

    /**
     * This method is called before returning the abstract value {@code value} as the result of the
     * transfer function. By default, the value is not changed but subclasses might decide to
     * implement some functionality. The store at this position is also passed.
     */
    protected V finishValue(V value, S store) {
        return value;
    }

    /**
     * This method is called before returning the abstract value {@code value} as the result of the
     * transfer function. By default, the value is not changed but subclasses might decide to
     * implement some functionality. The store at this position is also passed (two stores, as the
     * result is a {@link ConditionalTransferResult}.
     */
    protected V finishValue(V value, S thenStore, S elseStore) {
        return value;
    }

    /**
     * @return the abstract value of a non-leaf tree {@code tree}, as computed by the {@link
     *     AnnotatedTypeFactory}.
     */
    protected V getValueFromFactory(Tree tree, Node node) {
        GenericAnnotatedTypeFactory<V, S, T, ? extends CFAbstractAnalysis<V, S, T>> factory =
                analysis.atypeFactory;
        Tree preTree = analysis.getCurrentTree();
        Pair<Tree, AnnotatedTypeMirror> preCtxt = factory.getVisitorState().getAssignmentContext();
        analysis.setCurrentTree(tree);
        // is there an assignment context node available?
        if (node != null && node.getAssignmentContext() != null) {
            // get the declared type of the assignment context by looking up the
            // assignment context tree's type in the factory while flow is
            // disabled.
            Tree contextTree = node.getAssignmentContext().getContextTree();
            AnnotatedTypeMirror assCtxt = null;
            if (contextTree != null) {
                assCtxt = factory.getAnnotatedTypeLhs(contextTree);
            } else {
                Element assCtxtElement = node.getAssignmentContext().getElementForType();
                if (assCtxtElement != null) {
                    // if contextTree is null, use the element to get the type
                    assCtxt = factory.getAnnotatedType(assCtxtElement);
                }
            }

            if (assCtxt != null) {
                if (assCtxt instanceof AnnotatedExecutableType) {
                    // For a MethodReturnContext, we get the full type of the
                    // method, but we only want the return type.
                    assCtxt = ((AnnotatedExecutableType) assCtxt).getReturnType();
                }
                factory.getVisitorState()
                        .setAssignmentContext(
                                Pair.of(node.getAssignmentContext().getContextTree(), assCtxt));
            }
        }
        AnnotatedTypeMirror at = factory.getAnnotatedType(tree);
        analysis.setCurrentTree(preTree);
        factory.getVisitorState().setAssignmentContext(preCtxt);
        return analysis.createAbstractValue(at);
    }

    /**
     * @return an abstract value with the given {@code type} and the annotations from {@code
     *     annotatedValue}.
     */
    protected V getValueWithSameAnnotations(TypeMirror type, V annotatedValue) {
        if (annotatedValue == null) {
            return null;
        }
        return analysis.createAbstractValue(annotatedValue.getAnnotations(), type);
    }

    private S fixedInitialStore = null;

    /** Set a fixed initial Store. */
    public void setFixedInitialStore(S s) {
        fixedInitialStore = s;
    }

    /** The initial store maps method formal parameters to their currently most refined type. */
    @Override
    public S initialStore(
            UnderlyingAST underlyingAST, @Nullable List<LocalVariableNode> parameters) {
        if (underlyingAST.getKind() != Kind.LAMBDA && underlyingAST.getKind() != Kind.METHOD) {
            if (fixedInitialStore != null) {
                return fixedInitialStore;
            } else {
                return analysis.createEmptyStore(sequentialSemantics);
            }
        }

        S info;

        if (underlyingAST.getKind() == Kind.METHOD) {

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
            MethodTree methodTree = method.getMethod();
            ExecutableElement methodElem = TreeUtils.elementFromDeclaration(methodTree);
            addInformationFromPreconditions(info, factory, method, methodTree, methodElem);

            final ClassTree classTree = method.getClassTree();
            addFieldValues(info, factory, classTree, methodTree);

            addFinalLocalValues(info, methodElem);

            if (shouldPerformWholeProgramInference(methodTree, methodElem)) {
                Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
                        AnnotatedTypes.overriddenMethods(
                                analysis.atypeFactory.getElementUtils(),
                                analysis.atypeFactory,
                                methodElem);
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
                    analysis.atypeFactory
                            .getWholeProgramInference()
                            .updateInferredMethodParameterTypes(
                                    methodTree,
                                    methodElem,
                                    overriddenMethod,
                                    analysis.getTypeFactory());
                    analysis.atypeFactory
                            .getWholeProgramInference()
                            .updateInferredMethodReceiverType(
                                    methodTree,
                                    methodElem,
                                    overriddenMethod,
                                    analysis.getTypeFactory());
                }
            }

        } else if (underlyingAST.getKind() == Kind.LAMBDA) {
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
            Tree enclosingTree =
                    TreeUtils.enclosingOfKind(
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

                // Try to find an enclosing initializer block
                // Would love to know if there was a better way
                // Find any enclosing element of the lambda (using trees)
                // Then go up the elements to find an initializer element (which can't be found with
                // the tree).
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
            for (Entry<FieldAccess, V> fieldValue : fieldValuesClone.entrySet()) {
                AnnotatedTypeMirror declaredType =
                        factory.getAnnotatedType(fieldValue.getKey().getField());
                V lubbedValue =
                        analysis.createAbstractValue(declaredType)
                                .leastUpperBound(fieldValue.getValue());
                info.fieldValues.put(fieldValue.getKey(), lubbedValue);
            }
        } else {
            assert false : "Unexpected tree: " + underlyingAST;
            info = null;
        }

        return info;
    }

    private void addFieldValues(
            S info, AnnotatedTypeFactory factory, ClassTree classTree, MethodTree methodTree) {

        // Add knowledge about final fields, or values of non-final fields
        // if we are inside a constructor (information about initializers)
        TypeMirror classType = TreeUtils.typeOf(classTree);
        List<Pair<VariableElement, V>> fieldValues = analysis.getFieldValues();
        for (Pair<VariableElement, V> p : fieldValues) {
            VariableElement element = p.first;
            V value = p.second;
            if (ElementUtils.isFinal(element) || TreeUtils.isConstructor(methodTree)) {
                Receiver receiver;
                if (ElementUtils.isStatic(element)) {
                    receiver = new ClassName(classType);
                } else {
                    receiver = new ThisReference(classType);
                }
                TypeMirror fieldType = ElementUtils.getType(element);
                Receiver field = new FieldAccess(receiver, fieldType, element);
                info.insertValue(field, value);
            }
        }

        // add properties about fields (static information from type)
        boolean isNotFullyInitializedReceiver = isNotFullyInitializedReceiver(methodTree);
        if (isNotFullyInitializedReceiver && !TreeUtils.isConstructor(methodTree)) {
            // cannot add information about fields if the receiver isn't initialized
            // and the method isn't a constructor
            return;
        }
        for (Tree member : classTree.getMembers()) {
            if (member instanceof VariableTree) {
                VariableTree vt = (VariableTree) member;
                final VariableElement element = TreeUtils.elementFromDeclaration(vt);
                AnnotatedTypeMirror type =
                        ((GenericAnnotatedTypeFactory<?, ?, ?, ?>) factory).getAnnotatedTypeLhs(vt);
                TypeMirror fieldType = ElementUtils.getType(element);
                Receiver receiver;
                if (ElementUtils.isStatic(element)) {
                    receiver = new ClassName(classType);
                } else {
                    receiver = new ThisReference(classType);
                }
                V value = analysis.createAbstractValue(type);
                if (value == null) {
                    continue;
                }
                if (TreeUtils.isConstructor(methodTree)) {
                    // if we are in a constructor,
                    // then we can still use the static type, but only
                    // if there is also an initializer that already does
                    // some initialization.
                    boolean found = false;
                    for (Pair<VariableElement, V> fieldValue : fieldValues) {
                        if (fieldValue.first.equals(element)) {
                            value = value.leastUpperBound(fieldValue.second);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // no initializer found, cannot use static type
                        continue;
                    }
                }
                Receiver field = new FieldAccess(receiver, fieldType, element);
                info.insertValue(field, value);
            }
        }
    }

    private void addFinalLocalValues(S info, Element enclosingElement) {
        // add information about effectively final variables (from outer scopes)
        for (Entry<Element, V> e : analysis.atypeFactory.getFinalLocalValues().entrySet()) {

            Element elem = e.getKey();

            // TODO: There is a design flaw where the values of final local values leaks
            // into other methods of the same class. For example, in
            // class a { void b() {...} void c() {...} }
            // final local values from b() would be visible in the store for c(),
            // even though they should only be visible in b() and in classes
            // defined inside the method body of b().
            // This is partly because GenericAnnotatedTypeFactory.performFlowAnalysis
            // does not call itself recursively to analyze inner classes, but instead
            // pops classes off of a queue, and the information about known final local
            // values is stored by GenericAnnotatedTypeFactory.analyze in
            // GenericAnnotatedTypeFactory.flowResult, which is visible to all classes
            // in the queue regardless of their level of recursion.

            // We work around this here by ensuring that we only add a final
            // local value to a method's store if that method is enclosed by
            // the method where the local variables were declared.

            // Find the enclosing method of the element
            Element enclosingMethodOfVariableDeclaration = elem.getEnclosingElement();

            if (enclosingMethodOfVariableDeclaration != null) {

                // Now find all the enclosing methods of the code we are analyzing. If any one of
                // them matches the above, then the final local variable value applies.
                Element enclosingMethodOfCurrentMethod = enclosingElement;

                while (enclosingMethodOfCurrentMethod != null) {
                    if (enclosingMethodOfVariableDeclaration.equals(
                            enclosingMethodOfCurrentMethod)) {
                        LocalVariable l = new LocalVariable(elem);
                        info.insertValue(l, e.getValue());
                        break;
                    }

                    enclosingMethodOfCurrentMethod =
                            enclosingMethodOfCurrentMethod.getEnclosingElement();
                }
            }
        }
    }

    /** Returns true if the receiver of a method might not yet be fully initialized. */
    protected boolean isNotFullyInitializedReceiver(MethodTree methodTree) {
        return TreeUtils.isConstructor(methodTree);
    }

    /**
     * Add the information from all the preconditions of the method {@code method} with
     * corresponding tree {@code methodTree} to the store {@code info}.
     */
    protected void addInformationFromPreconditions(
            S info,
            AnnotatedTypeFactory factory,
            CFGMethod method,
            MethodTree methodTree,
            ExecutableElement methodElement) {
        ContractsUtils contracts = ContractsUtils.getInstance(analysis.atypeFactory);
        FlowExpressionContext flowExprContext = null;
        Set<Precondition> preconditions = contracts.getPreconditions(methodElement);

        for (Precondition p : preconditions) {
            String expression = p.expression;
            AnnotationMirror annotation = p.annotation;

            if (flowExprContext == null) {
                flowExprContext =
                        FlowExpressionContext.buildContextForMethodDeclaration(
                                methodTree, method.getClassTree(), analysis.checker.getContext());
            }

            TreePath localScope = analysis.atypeFactory.getPath(methodTree);

            annotation = standardizeAnnotationFromContract(annotation, flowExprContext, localScope);

            try {
                // TODO: currently, these expressions are parsed at the
                // declaration (i.e. here) and for every use. this could
                // be optimized to store the result the first time.
                // (same for other annotations)
                FlowExpressions.Receiver expr =
                        FlowExpressionParseUtil.parse(
                                expression, flowExprContext, localScope, false);
                info.insertValue(expr, annotation);
            } catch (FlowExpressionParseException e) {
                // Errors are reported by BaseTypeVisitor.checkContractsAtMethodDeclaration()
            }
        }
    }

    /** Standardize a type qualifier annotation obtained from a contract. */
    private AnnotationMirror standardizeAnnotationFromContract(
            AnnotationMirror annoFromContract,
            FlowExpressionContext flowExprContext,
            TreePath path) {
        // TODO: common implementation with BaseTypeVisitor.standardizeAnnotationFromContract
        if (analysis.dependentTypesHelper != null) {
            return analysis.dependentTypesHelper.standardizeAnnotation(
                    flowExprContext, path, annoFromContract, false);
            // BaseTypeVisitor checks the validity of the annotaiton. Errors are reported there
            // when called from BaseTypeVisitor.checkContractsAtMethodDeclaration().
        } else {
            return annoFromContract;
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
                GenericAnnotatedTypeFactory<V, S, T, ? extends CFAbstractAnalysis<V, S, T>>
                        factory = analysis.atypeFactory;
                analysis.setCurrentTree(tree);
                AnnotatedTypeMirror at = factory.getAnnotatedTypeFromTypeTree(tree);
                analysis.setCurrentTree(null);
                value = analysis.createAbstractValue(at);
            }
        }

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
    public TransferResult<V, S> visitFieldAccess(FieldAccessNode n, TransferInput<V, S> p) {
        S store = p.getRegularStore();
        V storeValue = store.getValue(n);
        // look up value in factory, and take the more specific one
        // TODO: handle cases, where this is not allowed (e.g. constructors in
        // non-null type systems)
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
    public TransferResult<V, S> visitThisLiteral(ThisLiteralNode n, TransferInput<V, S> in) {
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

    /** The resulting abstract value is the merge of the 'then' and 'else' branch. */
    @Override
    public TransferResult<V, S> visitTernaryExpression(
            TernaryExpressionNode n, TransferInput<V, S> p) {
        TransferResult<V, S> result = super.visitTernaryExpression(n, p);
        S store = result.getRegularStore();
        V thenValue = p.getValueOfSubNode(n.getThenOperand());
        V elseValue = p.getValueOfSubNode(n.getElseOperand());
        V resultValue = null;
        if (thenValue != null && elseValue != null) {
            resultValue = thenValue.leastUpperBound(elseValue);
        }
        return new RegularTransferResult<>(finishValue(resultValue, store), store);
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

        // if annotations differ, use the one that is more precise for both
        // sides (and add it to the store if possible)
        res = strengthenAnnotationOfEqualTo(res, leftN, rightN, leftV, rightV, true);
        res = strengthenAnnotationOfEqualTo(res, rightN, leftN, rightV, leftV, true);

        return res;
    }

    /**
     * Refine the annotation of {@code secondNode} if the annotation {@code secondValue} is less
     * precise than {@code firstvalue}. This is possible, if {@code secondNode} is an expression
     * that is tracked by the store (e.g., a local variable or a field).
     *
     * <p>Note that when overriding this method, when a new type is inserted into the store,
     * splitAssignments should be called, and the new type should be inserted into the store for
     * each of the resulting nodes.
     *
     * @param res the previous result
     * @param notEqualTo if true, indicates that the logic is flipped (i.e., the information is
     *     added to the {@code elseStore} instead of the {@code thenStore}) for a not-equal
     *     comparison.
     * @return the conditional transfer result (if information has been added), or {@code null}.
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
                    Receiver secondInternal =
                            FlowExpressions.internalReprOf(analysis.getTypeFactory(), secondPart);
                    if (CFAbstractStore.canInsertReceiver(secondInternal)) {
                        S thenStore = res.getThenStore();
                        S elseStore = res.getElseStore();
                        if (notEqualTo) {
                            elseStore.insertValue(secondInternal, firstValue);
                        } else {
                            thenStore.insertValue(secondInternal, firstValue);
                        }
                        // To handle `(a = b = c) == x`, repeat for all insertable receivers of
                        // splitted assignments instead of returning.
                        res =
                                new ConditionalTransferResult<>(
                                        res.getResultValue(), thenStore, elseStore);
                    }
                }
            }
        }
        return res;
    }

    /**
     * Takes a node, and either returns the node itself again (as a singleton list), or if the node
     * is an assignment node, returns the lhs and rhs (where splitAssignments is applied recursively
     * to the rhs -- that is, the rhs may not appear in the result, but rather its lhs and rhs may).
     */
    protected List<Node> splitAssignments(Node node) {
        if (node instanceof AssignmentNode) {
            List<Node> result = new ArrayList<>();
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
            if (lhs instanceof FieldAccessNode) {
                // Updates inferred field type
                analysis.atypeFactory
                        .getWholeProgramInference()
                        .updateInferredFieldType(
                                (FieldAccessNode) lhs,
                                rhs,
                                analysis.getContainingClass(n.getTree()),
                                analysis.getTypeFactory());
            } else if (lhs instanceof LocalVariableNode
                    && ((LocalVariableNode) lhs).getElement().getKind() == ElementKind.PARAMETER) {
                analysis.atypeFactory
                        .getWholeProgramInference()
                        .updateInferredParameterType(
                                (LocalVariableNode) lhs,
                                rhs,
                                analysis.getContainingClass(n.getTree()),
                                analysis.getContainingMethod(n.getTree()),
                                analysis.getTypeFactory());
            }
        }

        processCommonAssignment(in, lhs, rhs, info, rhsValue);

        return new RegularTransferResult<>(finishValue(rhsValue, info), info);
    }

    @Override
    public TransferResult<V, S> visitReturn(ReturnNode n, TransferInput<V, S> p) {
        if (shouldPerformWholeProgramInference(n.getTree())) {
            // Retrieves class containing the method
            ClassTree classTree = analysis.getContainingClass(n.getTree());
            ClassSymbol classSymbol = (ClassSymbol) TreeUtils.elementFromTree(classTree);
            // Updates the inferred return type of the method
            analysis.atypeFactory
                    .getWholeProgramInference()
                    .updateInferredMethodReturnType(
                            n,
                            classSymbol,
                            analysis.getContainingMethod(n.getTree()),
                            analysis.getTypeFactory());
        }
        return super.visitReturn(n, p);
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

        // update the results store if the assignment target is something we can
        // process
        S info = result.getRegularStore();
        // ResultValue is the type of LHS + RHS
        V resultValue = result.getResultValue();

        if (lhs instanceof FieldAccessNode
                && shouldPerformWholeProgramInference(n.getTree(), lhs.getTree())) {
            // Updates inferred field type
            analysis.atypeFactory
                    .getWholeProgramInference()
                    .updateInferredFieldType(
                            (FieldAccessNode) lhs,
                            rhs,
                            analysis.getContainingClass(n.getTree()),
                            analysis.getTypeFactory());
        }

        processCommonAssignment(in, lhs, rhs, info, resultValue);

        return new RegularTransferResult<>(finishValue(resultValue, info), info);
    }

    /**
     * Determine abstract value of right-hand side and update the store accordingly to the
     * assignment.
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
                    analysis.getTypeFactory()
                            .constructorFromUse(n.getTree())
                            .executableType
                            .getElement();
            analysis.atypeFactory
                    .getWholeProgramInference()
                    .updateInferredConstructorParameterTypes(
                            n, constructorElt, analysis.getTypeFactory());
        }
        return super.visitObjectCreation(n, p);
    }

    @Override
    public TransferResult<V, S> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<V, S> in) {

        S store = in.getRegularStore();
        ExecutableElement method = n.getTarget().getMethod();

        V factoryValue = null;

        Tree tree = n.getTree();
        if (tree != null) {
            // look up the value from factory
            factoryValue = getValueFromFactory(tree, n);
        }
        // look up the value in the store (if possible)
        V storeValue = store.getValue(n);
        V resValue = moreSpecificValue(factoryValue, storeValue);

        store.updateForMethodCall(n, analysis.atypeFactory, resValue);

        // add new information based on postcondition
        processPostconditions(n, store, method, tree);

        S thenStore = store;
        S elseStore = thenStore.copy();

        // add new information based on conditional postcondition
        processConditionalPostconditions(n, method, tree, thenStore, elseStore);

        if (shouldPerformWholeProgramInference(n.getTree(), method)) {
            // Finds the receiver's type
            Tree receiverTree = n.getTarget().getReceiver().getTree();
            if (receiverTree == null) {
                // If there is no receiver, then get the class being visited.
                // This happens when the receiver corresponds to "this".
                receiverTree = analysis.getContainingClass(n.getTree());
                // receiverTree could still be null after the call above. That
                // happens when the method is called from a static context.
            }
            // Updates the inferred parameter type of the invoked method
            analysis.atypeFactory
                    .getWholeProgramInference()
                    .updateInferredMethodParameterTypes(
                            n, receiverTree, method, analysis.getTypeFactory());
        }

        return new ConditionalTransferResult<>(
                finishValue(resValue, thenStore, elseStore), thenStore, elseStore);
    }

    /**
     * Returns true if whole-program inference should be performed. If the tree is in the scope of
     * a @SuppressWarning, then this method returns false.
     */
    private boolean shouldPerformWholeProgramInference(Tree tree) {
        return infer && (tree == null || !analysis.checker.shouldSuppressWarnings(tree, ""));
    }

    /**
     * Returns true if whole-program inference should be performed. If the expressionTree or lhsTree
     * is in the scope of a @SuppressWarning, then this method returns false.
     */
    private boolean shouldPerformWholeProgramInference(Tree expressionTree, Tree lhsTree) {
        // Check that infer is true and the tree isn't in scope of a @SuppressWarning
        // before calling  InternalUtils.symbol(lhs)
        if (!shouldPerformWholeProgramInference(expressionTree)) {
            return false;
        }
        Element elt = TreeUtils.elementFromTree(lhsTree);
        return !analysis.checker.shouldSuppressWarnings(elt, "");
    }

    /**
     * Returns true if whole-program inference should be performed. If the tree or element is in the
     * scope of a @SuppressWarning, then this method returns false.
     */
    private boolean shouldPerformWholeProgramInference(Tree tree, Element elt) {
        return shouldPerformWholeProgramInference(tree)
                && !analysis.checker.shouldSuppressWarnings(elt, "");
    }

    /**
     * Add information based on all postconditions of method {@code n} with tree {@code tree} and
     * element {@code method} to the store {@code store}.
     */
    protected void processPostconditions(
            MethodInvocationNode n, S store, ExecutableElement methodElement, Tree tree) {
        ContractsUtils contracts = ContractsUtils.getInstance(analysis.atypeFactory);
        Set<Postcondition> postconditions = contracts.getPostconditions(methodElement);
        processPostconditionsAndConditionalPostconditions(n, tree, store, null, postconditions);
    }

    /**
     * Add information based on all conditional postconditions of method {@code n} with tree {@code
     * tree} and element {@code method} to the appropriate store.
     */
    protected void processConditionalPostconditions(
            MethodInvocationNode n,
            ExecutableElement methodElement,
            Tree tree,
            S thenStore,
            S elseStore) {
        ContractsUtils contracts = ContractsUtils.getInstance(analysis.atypeFactory);
        Set<ConditionalPostcondition> conditionalPostconditions =
                contracts.getConditionalPostconditions(methodElement);
        processPostconditionsAndConditionalPostconditions(
                n, tree, thenStore, elseStore, conditionalPostconditions);
    }

    private void processPostconditionsAndConditionalPostconditions(
            MethodInvocationNode n,
            Tree tree,
            S thenStore,
            S elseStore,
            Set<? extends Contract> postconditions) {
        FlowExpressionContext flowExprContext = null;

        for (Contract p : postconditions) {
            String expression = p.expression;
            AnnotationMirror anno = p.annotation;

            if (flowExprContext == null) {
                flowExprContext =
                        FlowExpressionContext.buildContextForMethodUse(
                                n, analysis.checker.getContext());
            }

            TreePath localScope = analysis.atypeFactory.getPath(tree);

            anno = standardizeAnnotationFromContract(anno, flowExprContext, localScope);

            try {
                FlowExpressions.Receiver r =
                        FlowExpressionParseUtil.parse(
                                expression, flowExprContext, localScope, false);
                if (p.kind == Contract.Kind.CONDITIONALPOSTCONDTION) {
                    if (((ConditionalPostcondition) p).annoResult) {
                        thenStore.insertValue(r, anno);
                    } else {
                        elseStore.insertValue(r, anno);
                    }
                } else {
                    thenStore.insertValue(r, anno);
                }
            } catch (FlowExpressionParseException e) {
                Result result;
                if (e.isFlowParseError()) {
                    Object[] args = new Object[e.args.length + 1];
                    args[0] = ElementUtils.getSimpleName(TreeUtils.elementFromUse(n.getTree()));
                    System.arraycopy(e.args, 0, args, 1, e.args.length);
                    result = Result.failure("flowexpr.parse.error.postcondition", args);
                } else {
                    result = e.getResult();
                }

                // report errors here
                analysis.checker.report(result, tree);
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

        V caseValue = in.getValueOfSubNode(n.getCaseOperand());
        AssignmentNode assign = (AssignmentNode) n.getSwitchOperand();
        V switchValue =
                store.getValue(
                        FlowExpressions.internalReprOf(
                                analysis.getTypeFactory(), assign.getTarget()));
        result =
                strengthenAnnotationOfEqualTo(
                        result,
                        n.getCaseOperand(),
                        assign.getExpression(),
                        caseValue,
                        switchValue,
                        false);

        // Update value of switch temporary variable
        result =
                strengthenAnnotationOfEqualTo(
                        result,
                        n.getCaseOperand(),
                        assign.getTarget(),
                        caseValue,
                        switchValue,
                        false);
        return result;
    }

    /**
     * In a cast {@code (@A C) e} of some expression {@code e} to a new type {@code @A C}, we
     * usually take the annotation of the type {@code C} (here {@code @A}). However, if the inferred
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
    public TransferResult<V, S> visitNarrowingConversion(
            NarrowingConversionNode n, TransferInput<V, S> p) {
        TransferResult<V, S> result = super.visitNarrowingConversion(n, p);
        // Combine annotations from the operand with the narrow type
        V operandValue = p.getValueOfSubNode(n.getOperand());
        V narrowedValue = getValueWithSameAnnotations(n.getType(), operandValue);
        result.setResultValue(narrowedValue);
        return result;
    }

    @Override
    public TransferResult<V, S> visitWideningConversion(
            WideningConversionNode n, TransferInput<V, S> p) {
        TransferResult<V, S> result = super.visitWideningConversion(n, p);
        // Combine annotations from the operand with the wide type
        V operandValue = p.getValueOfSubNode(n.getOperand());
        V widenedValue = getValueWithSameAnnotations(n.getType(), operandValue);
        result.setResultValue(widenedValue);
        return result;
    }

    @Override
    public TransferResult<V, S> visitStringConversion(
            StringConversionNode n, TransferInput<V, S> p) {
        TransferResult<V, S> result = super.visitStringConversion(n, p);
        result.setResultValue(p.getValueOfSubNode(n.getOperand()));
        return result;
    }
}
