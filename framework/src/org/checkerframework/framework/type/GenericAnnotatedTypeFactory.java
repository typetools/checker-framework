package org.checkerframework.framework.type;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.AnalysisResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.CFGBuilder;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGLambda;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGStatement;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFCFGBuilder;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultForInUntyped;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.DefaultQualifierInUntyped;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.MonotonicQualifier;
import org.checkerframework.framework.qual.Unqualified;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ImplicitsTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.PropagationTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.QualifierPolymorphism;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;

/**
 * A factory that extends {@link AnnotatedTypeFactory} to optionally use
 * flow-sensitive qualifier inference, qualifier polymorphism, implicit
 * annotations via {@link ImplicitFor}, and user-specified defaults via
 * {@link DefaultQualifier}.
 */
public abstract class GenericAnnotatedTypeFactory<
        Value extends CFAbstractValue<Value>,
        Store extends CFAbstractStore<Value, Store>,
        TransferFunction extends CFAbstractTransfer<Value, Store, TransferFunction>,
        FlowAnalysis extends CFAbstractAnalysis<Value, Store, TransferFunction>>
    extends AnnotatedTypeFactory {

    /** should use flow by default */
    protected static boolean FLOW_BY_DEFAULT = true;

    /** To cache the supported monotonic type qualifiers. */
    private Set<Class<? extends Annotation>> supportedMonotonicQuals;

    /** to annotate types based on the given tree */
    protected TypeAnnotator typeAnnotator;

    /** for use in addTypeImplicits */
    private ImplicitsTypeAnnotator implicitsTypeAnnotator;

    /** to annotate types based on the given un-annotated types */
    protected TreeAnnotator treeAnnotator;

    /** to handle any polymorphic types */
    protected QualifierPolymorphism poly;

    /** to handle defaults specified by the user */
    protected QualifierDefaults defaults;

    // Flow related fields

    /** Should use flow analysis? */
    protected boolean useFlow;

    /** An empty store. */
    private Store emptyStore;

    /**
     * Creates a type factory for checking the given compilation unit with
     * respect to the given annotation.
     *
     * @param checker the checker to which this type factory belongs
     * @param useFlow whether flow analysis should be performed
     */
    public GenericAnnotatedTypeFactory(BaseTypeChecker checker, boolean useFlow) {
        super(checker);

        this.useFlow = useFlow;
        this.analyses = new LinkedList<>();
        this.scannedClasses = new HashMap<>();
        this.flowResult = null;
        this.regularExitStores = null;
        this.methodInvocationStores = null;
        this.returnStatementStores = null;

        this.initializationStore = null;
        this.initializationStaticStore = null;

        // Add common aliases.
        // addAliasedDeclAnnotation(checkers.nullness.quals.Pure.class,
        //         Pure.class, AnnotationUtils.fromClass(elements, Pure.class));

        // Every subclass must call postInit, but it must be called after
        // all other initialization is finished.
    }

    @Override
    protected void postInit() {
        super.postInit();

        this.defaults = createQualifierDefaults();
        this.treeAnnotator = createTreeAnnotator();
        this.typeAnnotator = createTypeAnnotator();

        this.poly = createQualifierPolymorphism();

        this.buildIndexTypes();
    }

    /**
     * Creates a type factory for checking the given compilation unit with
     * respect to the given annotation.
     *
     * @param checker the checker to which this type factory belongs
     */
    public GenericAnnotatedTypeFactory(BaseTypeChecker checker) {
        this(checker, FLOW_BY_DEFAULT);
    }


    @Override
    public void setRoot(/*@Nullable*/ CompilationUnitTree root) {
        super.setRoot(root);
        this.analyses.clear();
        this.scannedClasses.clear();
        this.flowResult = null;
        this.regularExitStores = null;
        this.methodInvocationStores = null;
        this.returnStatementStores = null;
        this.initializationStore = null;
        this.initializationStaticStore = null;
    }

    // **********************************************************************
    // Factory Methods for the appropriate annotator classes
    // **********************************************************************

    /**
     * Returns an immutable set of the <em>monotonic</em> type qualifiers supported by this
     * checker.
     *
     * @return the monotonic type qualifiers supported this processor, or an empty
     * set if none
     * @see MonotonicQualifier
     */
    public final Set<Class<? extends Annotation>> getSupportedMonotonicTypeQualifiers() {
        if (supportedMonotonicQuals == null) {
            supportedMonotonicQuals = new HashSet<>();
            for (Class<? extends Annotation> anno : getSupportedTypeQualifiers()) {
                MonotonicQualifier mono = anno.getAnnotation(MonotonicQualifier.class);
                if (mono != null) {
                    supportedMonotonicQuals.add(anno);
                }
            }
        }
        return supportedMonotonicQuals;
    }

    /**
     * Returns a {@link TreeAnnotator} that adds annotations to a type based
     * on the contents of a tree.
     *
     * Subclasses may override this method to specify more appropriate
     * {@link TreeAnnotator}
     *
     * @return a tree annotator
     */
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new PropagationTreeAnnotator(this),
                new ImplicitsTreeAnnotator(this)
        );
    }

    /**
     * Returns a {@link org.checkerframework.framework.type.typeannotator.ImplicitsTypeAnnotator} that adds annotations to a type based
     * on the content of the type itself.
     *
     * @return a type annotator
     */
    protected TypeAnnotator createTypeAnnotator() {
        implicitsTypeAnnotator = new ImplicitsTypeAnnotator(this);

        return new ListTypeAnnotator(
                new PropagationTypeAnnotator(this),
                implicitsTypeAnnotator
        );
    }

    protected void addTypeNameImplicit(Class<?> clazz, AnnotationMirror implicitAnno) {
        implicitsTypeAnnotator.addTypeName(clazz, implicitAnno);
    }

    /**
     * Returns the appropriate flow analysis class that is used for the org.checkerframework.dataflow
     * analysis.
     *
     * <p>
     * This implementation uses the checker naming convention to create the
     * appropriate analysis. If no transfer function is found, it returns an
     * instance of {@link CFAnalysis}.
     *
     * <p>
     * Subclasses have to override this method to create the appropriate
     * analysis if they do not follow the checker naming convention.
     */
    @SuppressWarnings({ "unchecked", "rawtypes"})
    protected FlowAnalysis createFlowAnalysis(List<Pair<VariableElement, Value>> fieldValues) {

        // Try to reflectively load the visitor.
        Class<?> checkerClass = checker.getClass();

        while (checkerClass != BaseTypeChecker.class) {
            final String classToLoad = checkerClass.getName()
                    .replace("Checker", "Analysis")
                    .replace("Subchecker", "Analysis");
            FlowAnalysis result = BaseTypeChecker.invokeConstructorFor(
                    classToLoad,
                    new Class<?>[] { BaseTypeChecker.class, this.getClass(), List.class },
                    new Object[] { checker, this, fieldValues });
            if (result != null)
                return result;
            checkerClass = checkerClass.getSuperclass();
        }

        // If an analysis couldn't be loaded reflectively, return the
        // default.
        List<Pair<VariableElement, CFValue>> tmp = new ArrayList<>();
        for (Pair<VariableElement, Value> fieldVal : fieldValues) {
            assert fieldVal.second instanceof CFValue;
            tmp.add(Pair.<VariableElement, CFValue> of(fieldVal.first,
                    (CFValue) fieldVal.second));
        }
        return (FlowAnalysis) new CFAnalysis(checker, (GenericAnnotatedTypeFactory) this, tmp);
    }

    /**
     * Returns the appropriate transfer function that is used for the org.checkerframework.dataflow
     * analysis.
     *
     * <p>
     * This implementation uses the checker naming convention to create the
     * appropriate transfer function. If no transfer function is found, it
     * returns an instance of {@link CFTransfer}.
     *
     * <p>
     * Subclasses have to override this method to create the appropriate
     * transfer function if they do not follow the checker naming convention.
     */
    // A more precise type for the parameter would be FlowAnalysis, which
    // is the type parameter bounded by the current parameter type CFAbstractAnalysis<Value, Store, TransferFunction>.
    // However, we ran into issues in callers of the method if we used that type.
    public TransferFunction createFlowTransferFunction(CFAbstractAnalysis<Value, Store, TransferFunction> analysis) {

        // Try to reflectively load the visitor.
        Class<?> checkerClass = checker.getClass();

        while (checkerClass != BaseTypeChecker.class) {
            final String classToLoad = checkerClass.getName()
                    .replace("Checker", "Transfer")
                    .replace("Subchecker", "Transfer");
            TransferFunction result = BaseTypeChecker.invokeConstructorFor(
                    classToLoad, new Class<?>[] { analysis.getClass() },
                    new Object[] { analysis });
            if (result != null)
                return result;
            checkerClass = checkerClass.getSuperclass();
        }

        // If a transfer function couldn't be loaded reflectively, return the
        // default.
        @SuppressWarnings("unchecked")
        TransferFunction ret = (TransferFunction) new CFTransfer(
                (CFAbstractAnalysis<CFValue, CFStore, CFTransfer>) analysis);
        return ret;
    }

    /**
     * Create {@link QualifierDefaults} which handles user specified defaults
     * @return the QualifierDefaults class
     *
     * TODO: should this be split in two methods to allow separate reuse?
     */
    protected QualifierDefaults createQualifierDefaults() {
        QualifierDefaults defs = new QualifierDefaults(elements, this);
        boolean foundDefaultOtherwise = false;

        // TODO: Verify that only one default per location type is present.
        for (Class<? extends Annotation> qual : getSupportedTypeQualifiers()) {
            DefaultFor defaultFor = qual.getAnnotation(DefaultFor.class);
            if (defaultFor != null) {
                defs.addAbsoluteDefaults(AnnotationUtils.fromClass(elements,qual),
                                         defaultFor.value());

                if (Arrays.asList(defaultFor.value()).contains(DefaultLocation.OTHERWISE)) {
                    foundDefaultOtherwise = true;
                }
            }

            if (qual.getAnnotation(DefaultQualifierInHierarchy.class) != null) {
                if (defaultFor != null) {
                    // A type qualifier should either have a DefaultFor or
                    // a DefaultQualifierInHierarchy annotation
                    ErrorReporter.errorAbort("GenericAnnotatedTypeFactory.createQualifierDefaults: " +
                            "qualifier has both @DefaultFor and @DefaultQualifierInHierarchy annotations: " +
                            qual.getCanonicalName());
                } else {
                    defs.addAbsoluteDefault(AnnotationUtils.fromClass(elements, qual),
                        DefaultLocation.OTHERWISE);
		    foundDefaultOtherwise = true;
                }
            }

	    // Add defaults for untyped code if conservative untyped flag is passed.
	    if (!checker.hasOption("unsafeUntyped")) {
		DefaultForInUntyped defaultForUntyped = qual.getAnnotation(DefaultForInUntyped.class);

		if (defaultForUntyped != null) {
		    defs.addUntypedDefaults(AnnotationUtils.fromClass(elements, qual),
			defaultForUntyped.value());
		}

		if (qual.getAnnotation(DefaultQualifierInUntyped.class) != null) {
		    if (defaultForUntyped != null) {
			// A type qualifier should either have a DefaultForInUntyped or
			// a DefaultQualifierInUntyped annotation.
			ErrorReporter.errorAbort("GenericAnnotatedTypeFactory.createQualifierDefaults: " +
			    "qualifier has both @DefaultForInUntyped and @DefaultQualifierInUntyped annotations: " +
   			    qual.getCanonicalName());
		    } else {
			for (DefaultLocation location : QualifierDefaults.validLocationsForUntyped()) {
  			    defs.addUntypedDefault(AnnotationUtils.fromClass(elements, qual),
			        location);
			}
		    }
	      	}
	    }
        }

        // If Unqualified is a supported qualifier, make it the default.
        // This is for convenience only. Maybe remove.
        AnnotationMirror unqualified = AnnotationUtils.fromClass(elements, Unqualified.class);
        if (!foundDefaultOtherwise &&
                this.isSupportedQualifier(unqualified)) {
            defs.addAbsoluteDefault(unqualified,
                    DefaultLocation.OTHERWISE);
        }

        return defs;
    }

    /**
     * Creates {@link QualifierPolymorphism} which supports
     * QualifierPolymorphism mechanism
     * @return the QualifierPolymorphism class
     */
    protected QualifierPolymorphism createQualifierPolymorphism() {
        return new QualifierPolymorphism(processingEnv, this);
    }

    // **********************************************************************
    // Factory Methods for the appropriate annotator classes
    // **********************************************************************

    @Override
    protected void postDirectSuperTypes(AnnotatedTypeMirror type,
            List<? extends AnnotatedTypeMirror> supertypes) {
        super.postDirectSuperTypes(type, supertypes);
        if (type.getKind() == TypeKind.DECLARED) {
            for (AnnotatedTypeMirror supertype : supertypes) {
                Element elt = ((DeclaredType) supertype.getUnderlyingType()).asElement();
                annotateImplicit(elt, supertype);
            }
        }
    }

    /**
     * Track the state of org.checkerframework.dataflow analysis scanning for each class tree in the
     * compilation unit.
     */
    protected enum ScanState {
        IN_PROGRESS, FINISHED
    };

    protected final Map<ClassTree, ScanState> scannedClasses;

    /**
     * The result of the flow analysis. Invariant:
     *
     * <pre>
     *  scannedClasses.get(c) == FINISHED for some class c &rArr; flowResult != null
     * </pre>
     *
     * Note that flowResult contains analysis results for Trees from multiple
     * classes which are produced by multiple calls to performFlowAnalysis.
     */
    protected AnalysisResult<Value, Store> flowResult;

    /**
     * A mapping from methods (or other code blocks) to their regular exit store (used to check
     * postconditions).
     */
    protected IdentityHashMap<Tree, Store> regularExitStores;

    /**
     * A mapping from methods to their a list with all return statements and the
     * corresponding store.
     */
    protected IdentityHashMap<MethodTree, List<Pair<ReturnNode, TransferResult<Value, Store>>>> returnStatementStores;

    /**
     * A mapping from methods to their a list with all return statements and the
     * corresponding store.
     */
    protected IdentityHashMap<MethodInvocationTree, Store> methodInvocationStores;

    /**
     * Returns the regular exit store for a method or another code block (such as static initializers).
     *
     * @return The regular exit store, or {@code null}, if there is no such
     *         store (because the method cannot exit through the regular exit
     *         block).
     */
    public /*@Nullable*/ Store getRegularExitStore(Tree t) {
        return regularExitStores.get(t);
    }

    /**
     * @return All return node and store pairs for a given method.
     */
    public List<Pair<ReturnNode, TransferResult<Value, Store>>> getReturnStatementStores(
            MethodTree methodTree) {
        assert returnStatementStores.containsKey(methodTree);
        return returnStatementStores.get(methodTree);
    }

    /**
     * @return The store immediately before a given {@link Tree}.
     */
    public Store getStoreBefore(Tree tree) {
        if (analyses == null || analyses.isEmpty()) {
            return flowResult.getStoreBefore(tree);
        }
        FlowAnalysis analysis = analyses.getFirst();
        Node node = analysis.getNodeForTree(tree);
        TransferInput<Value, Store> prevStore = analysis.getInput(node.getBlock());
        if (prevStore == null) {
            return null;
        }
        Store store = AnalysisResult.runAnalysisFor(node, true, prevStore);
        return store;
    }

    /**
     * @return The store immediately after a given {@link Tree}.
     */
    public Store getStoreAfter(Tree tree) {
        if (analyses == null || analyses.isEmpty()) {
            return flowResult.getStoreAfter(tree);
        }
        FlowAnalysis analysis = analyses.getFirst();
        Node node = analysis.getNodeForTree(tree);
        Store store = AnalysisResult.runAnalysisFor(node, false, analysis.getInput(node.getBlock()));
        return store;
    }

    /**
     * @return The {@link Node} for a given {@link Tree}.
     */
    public Node getNodeForTree(Tree tree) {
        return flowResult.getNodeForTree(tree);
    }

    /**
     * @return The value of effectively final local variables.
     */
    public HashMap<Element, Value> getFinalLocalValues() {
        return flowResult.getFinalLocalValues();
    }

    /**
     * Perform a org.checkerframework.dataflow analysis over a single class tree and its nested
     * classes.
     */
    protected void performFlowAnalysis(ClassTree classTree) {
        if (flowResult == null) {
            regularExitStores = new IdentityHashMap<>();
            returnStatementStores = new IdentityHashMap<>();
            flowResult = new AnalysisResult<>();
        }

        // no need to scan annotations
        if (classTree.getKind() == Kind.ANNOTATION_TYPE) {
            // Mark finished so that default annotations will be applied.
            scannedClasses.put(classTree, ScanState.FINISHED);
            return;
        }

        Queue<ClassTree> queue = new LinkedList<>();
        List<Pair<VariableElement, Value>> fieldValues = new ArrayList<>();
        queue.add(classTree);
        while (!queue.isEmpty()) {
            ClassTree ct = queue.remove();
            scannedClasses.put(ct, ScanState.IN_PROGRESS);

            AnnotatedDeclaredType preClassType = visitorState.getClassType();
            ClassTree preClassTree = visitorState.getClassTree();
            AnnotatedDeclaredType preAMT = visitorState.getMethodReceiver();
            MethodTree preMT = visitorState.getMethodTree();

            visitorState.setClassType(getAnnotatedType(ct));
            visitorState.setClassTree(ct);
            visitorState.setMethodReceiver(null);
            visitorState.setMethodTree(null);

            // start without a initialization store
            initializationStaticStore = null;
            initializationStore = null;

            Queue<Pair<LambdaExpressionTree, Store>> lambdaQueue = new LinkedList<>();

            try {
                List<MethodTree> methods = new ArrayList<>();
                for (Tree m : ct.getMembers()) {
                    switch (m.getKind()) {
                    case METHOD:
                        MethodTree mt = (MethodTree) m;

                        // Skip abstract and native methods because they have no body.
                        ModifiersTree modifiers = mt.getModifiers();
                        if (modifiers != null) {
                            Set<Modifier> flags = modifiers.getFlags();
                            if (flags.contains(Modifier.ABSTRACT) ||
                                flags.contains(Modifier.NATIVE)) {
                                break;
                            }
                        }
                        // Abstract methods in an interface have a null body but do not have an ABSTRACT flag.
                        if (mt.getBody() == null) {
                            break;
                        }

                        // Wait with scanning the method until all other members
                        // have been processed.
                        methods.add(mt);
                        break;
                    case VARIABLE:
                        VariableTree vt = (VariableTree) m;
                        ExpressionTree initializer = vt.getInitializer();
                        // analyze initializer if present
                        if (initializer != null) {
                            analyze(queue, lambdaQueue, new CFGStatement(vt),
                                    fieldValues, classTree, true, false);
                            Value value = flowResult.getValue(initializer);
                            if (value != null) {
                                // Store the abstract value for the field.
                                VariableElement element = TreeUtils.elementFromDeclaration(vt);
                                fieldValues.add(Pair.of(element, value));
                            }
                        }
                        break;
                    case CLASS:
                        // Visit inner and nested classes.
                        queue.add((ClassTree) m);
                        break;
                    case ANNOTATION_TYPE:
                    case INTERFACE:
                    case ENUM:
                        // not necessary to handle
                        break;
                    case BLOCK:
                        BlockTree b = (BlockTree) m;
                        analyze(queue, lambdaQueue, new CFGStatement(b), fieldValues, ct, true, b.isStatic());
                        break;
                    default:
                        assert false : "Unexpected member: " + m.getKind();
                        break;
                    }
                }

                // Now analyze all methods.
                // TODO: at this point, we don't have any information about
                // fields of superclasses.
                for (MethodTree mt : methods) {
                    analyze(queue, lambdaQueue,
                            new CFGMethod(mt, TreeUtils
                                    .enclosingClass(getPath(mt))), fieldValues, classTree, false, false);
                }

                while (lambdaQueue.size() > 0) {
                    Pair<LambdaExpressionTree, Store> lambdaPair = lambdaQueue.poll();
                    analyze(queue, lambdaQueue,
                            new CFGLambda(lambdaPair.first), fieldValues, classTree, false, false, lambdaPair.second);

                }

                // by convention we store the static initialization store as the regular exit
                // store of the class node, os that it can later be used to check
                // that all fields are initialized properly.
                // see InitializationVisitor.visitClass
                if (initializationStaticStore == null) {
                    regularExitStores.put(ct, emptyStore);
                } else {
                    regularExitStores.put(ct, initializationStaticStore);
                }
            } finally {
                visitorState.setClassType(preClassType);
                visitorState.setClassTree(preClassTree);
                visitorState.setMethodReceiver(preAMT);
                visitorState.setMethodTree(preMT);
            }

            scannedClasses.put(ct, ScanState.FINISHED);
        }
    }

    // Maintain a deque of analyses to accommodate nested classes.
    protected final Deque<FlowAnalysis> analyses;
    // Maintain for every class the store that is used when we analyze initialization code
    protected Store initializationStore;
    // Maintain for every class the store that is used when we analyze static initialization code
    protected Store initializationStaticStore;

    /**
     * Analyze the AST {@code ast} and store the result.
     *
     * @param queue
     *            The queue to add more things to scan.
     * @param fieldValues
     *            The abstract values for all fields of the same class.
     * @param ast
     *            The AST to analyze.
     * @param currentClass The class we are currently looking at.
     * @param isInitializationCode Are we analyzing a (non-static) initializer block of a class.
     */
    protected void analyze(Queue<ClassTree> queue, Queue<Pair<LambdaExpressionTree, Store>> lambdaQueue, UnderlyingAST ast,
            List<Pair<VariableElement, Value>> fieldValues, ClassTree currentClass,
            boolean isInitializationCode, boolean isStatic) {
        analyze(queue, lambdaQueue, ast, fieldValues, currentClass, isInitializationCode, isStatic, null);
    }

    protected void analyze(Queue<ClassTree> queue, Queue<Pair<LambdaExpressionTree, Store>> lambdaQueue, UnderlyingAST ast,
            List<Pair<VariableElement, Value>> fieldValues, ClassTree currentClass,
            boolean isInitializationCode, boolean isStatic,
            Store lambdaStore) {
        CFGBuilder builder = new CFCFGBuilder(checker, this);
        ControlFlowGraph cfg = builder.run(root, processingEnv, ast);
        FlowAnalysis newAnalysis = createFlowAnalysis(fieldValues);
        if (emptyStore == null) {
            emptyStore = newAnalysis.createEmptyStore(!checker.hasOption("concurrentSemantics"));
        }
        analyses.addFirst(newAnalysis);
        if (lambdaStore != null) {
            TransferFunction transfer = newAnalysis.getTransferFunction();
            transfer.setFixedInitialStore(lambdaStore);
        } else {
            Store initStore = isStatic ? initializationStore : initializationStaticStore;
            if (isInitializationCode) {
                if (initStore != null) {
                    // we have already seen initialization code and analyzed it, and
                    // the analysis ended with the store initStore.
                    // use it to start the next analysis.
                    TransferFunction transfer = newAnalysis.getTransferFunction();
                    transfer.setFixedInitialStore(initStore);
                }
            }
        }
        analyses.getFirst().performAnalysis(cfg);
        AnalysisResult<Value, Store> result = analyses.getFirst().getResult();

        // store result
        flowResult.combine(result);
        if (ast.getKind() == UnderlyingAST.Kind.METHOD) {
            // store exit store (for checking postconditions)
            CFGMethod mast = (CFGMethod) ast;
            MethodTree method = mast.getMethod();
            Store regularExitStore = analyses.getFirst().getRegularExitStore();
            if (regularExitStore != null) {
                regularExitStores.put(method, regularExitStore);
            }
            returnStatementStores.put(method, analyses.getFirst()
                    .getReturnStatementStores());
        } else if (ast.getKind() == UnderlyingAST.Kind.ARBITRARY_CODE) {
            CFGStatement block = (CFGStatement) ast;
            Store regularExitStore = analyses.getFirst().getRegularExitStore();
            if (regularExitStore != null) {
                regularExitStores.put(block.getCode(), regularExitStore);
            }
        } else if (ast.getKind() == UnderlyingAST.Kind.LAMBDA) {
            // TODO: Postconditions?

            CFGLambda block = (CFGLambda) ast;
            Store regularExitStore = analyses.getFirst().getRegularExitStore();
            if (regularExitStore != null) {
                regularExitStores.put(block.getCode(), regularExitStore);
            }
        }

        if (isInitializationCode) {
            Store newInitStore = analyses.getFirst().getRegularExitStore();
            if (isStatic) {
                initializationStore = newInitStore;
            } else {
                initializationStaticStore = newInitStore;
            }
        }

        if (checker.hasOption("flowdotdir")) {

            String checkerName = checker.getClass().getSimpleName();
            if (checkerName.endsWith("Checker") || checkerName.endsWith("checker")) {
                checkerName = checkerName.substring(0, checkerName.length() - "checker".length());
            }

            String dotfilename = checker.getOption("flowdotdir") + "/"
                    + dotOutputFileName(ast) + "_" + checkerName + ".dot";
            // make path safe for Windows
            dotfilename = dotfilename.replace("<", "_").replace(">", "");
            System.err.println("Output to DOT file: " + dotfilename);
            boolean verbose = checker.hasOption("verbosecfg");
            analyses.getFirst().outputToDotFile(dotfilename, verbose);
        }

        analyses.removeFirst();

        // add classes declared in method
        queue.addAll(builder.getDeclaredClasses());
        for (LambdaExpressionTree lambda : builder.getDeclaredLambdas()) {
            lambdaQueue.add(Pair.of(lambda, getStoreBefore(lambda)));
        }
    }

    /** @return The file name used for DOT output. */
    protected String dotOutputFileName(UnderlyingAST ast) {
        if (ast.getKind() == UnderlyingAST.Kind.ARBITRARY_CODE) {
            return "initializer-" + ast.hashCode();
        } else if (ast.getKind() == UnderlyingAST.Kind.METHOD) {
            return ((CFGMethod) ast).getMethod().getName().toString();
        }
        assert false;
        return null;
    }


    /**
     * Get the defaulted type of a variable, without considering
     * flow inference from the initializer expression.
     * This is needed to determine the type of the assignment context,
     * which should have the "default" meaning, without flow inference.
     * TODO: describe and generalize
     */
    public AnnotatedTypeMirror getDefaultedAnnotatedType(Tree tree, ExpressionTree valueTree) {
        AnnotatedTypeMirror res = null;
        if (tree instanceof VariableTree) {
            res = fromMember(tree);
            annotateImplicit(tree, res, false);
        } else if (tree instanceof AssignmentTree) {
            res = fromExpression(((AssignmentTree) tree).getVariable());
            annotateImplicit(tree, res, false);
        } else if (tree instanceof CompoundAssignmentTree) {
            res = fromExpression(((CompoundAssignmentTree) tree).getVariable());
            annotateImplicit(tree, res, false);
        } else if (TreeUtils.isExpressionTree(tree)) {
            res = fromExpression((ExpressionTree) tree);
            annotateImplicit(tree, res, false);
        } else {
            assert false;
        }
        return res;
    }

    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> constructorFromUse(
            NewClassTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = super
                .constructorFromUse(tree);
        AnnotatedExecutableType method = mfuPair.first;
        poly.annotate(tree, method);
        return mfuPair;
    }

    /**
     * This method is final. Override
     * {@link #annotateImplicit(Tree, AnnotatedTypeMirror, boolean)}
     * instead.
     */
    @Override
    public final void annotateImplicit(Tree tree, AnnotatedTypeMirror type) {
        annotateImplicit(tree, type, this.useFlow);
    }

    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
        assert root != null : "GenericAnnotatedTypeFactory.annotateImplicit: " +
                " root needs to be set when used on trees; factory: " + this.getClass();

        if (iUseFlow) {
             /**
             * We perform flow analysis on each {@link ClassTree} that is
             * passed to annotateImplicitWithFlow.  This works correctly when
             * a {@link ClassTree} is passed to this method before any of its
             * sub-trees.  It also helps to satisfy the requirement that a
             * {@link ClassTree} has been advanced to annotation before we
             * analyze it.
             */
            checkAndPerformFlowAnalysis(tree);
        }

        treeAnnotator.visit(tree, type);
        typeAnnotator.visit(type, null);
        defaults.annotate(tree, type);

        if (iUseFlow) {
            Value as = getInferredValueFor(tree);
            if (as != null) {
                applyInferredAnnotations(type, as);
            }
        }
    }

    /**
     * Flow analysis will be performed if:
     * <ul>
     *     <li>tree is a {@link ClassTree}</li>
     *     <li>Flow analysis has not already been performed on tree</li>
     * </ul>
     * @param tree the tree to check and possibly perform flow analysis on.
     */
    protected void checkAndPerformFlowAnalysis(Tree tree) {
        // For performance reasons, we require that getAnnotatedType is called
        // on the ClassTree before it's called on any code contained in the class,
        // so that we can perform flow analysis on the class.  Previously we
        // used TreePath.getPath to find enclosing classes, but that call
        // alone consumed more than 10% of execution time.  See BaseTypeVisitor
        // .visitClass for the call to getAnnotatedType that triggers analysis.
        if (tree instanceof ClassTree) {
            ClassTree classTree = (ClassTree) tree;
            if (!scannedClasses.containsKey(classTree)) {
                performFlowAnalysis(classTree);
            }
        }
    }

    /**
     * Returns the inferred value (by the org.checkerframework.dataflow analysis) for a given tree.
     */
    public Value getInferredValueFor(Tree tree) {
        if (tree == null) {
            ErrorReporter.errorAbort("GenericAnnotatedTypeFactory.getInferredValueFor called with null tree. Don't!");
            return null; // dead code
        }
        Value as = null;
        if (!analyses.isEmpty()) {
            as = analyses.getFirst().getValue(tree);
        }
        if (as == null &&
                // TODO: this comparison shouldn't be needed, but
                // Daikon check-nullness started failing without it.
                flowResult != null) {
            as = flowResult.getValue(tree);
        }
        return as;
    }

    /**
     * Applies the annotations inferred by the org.checkerframework.dataflow analysis to the type {@code type}.
     */
    protected void applyInferredAnnotations(AnnotatedTypeMirror type, Value as) {
        new DefaultInferredTypesApplier().applyInferredType(getQualifierHierarchy(), type, as.getType());
    }

    @Override
    public void annotateImplicit(Element elt, AnnotatedTypeMirror type) {
        typeAnnotator.visit(type, null);
        defaults.annotate(elt, type);
    }

    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(MethodInvocationTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = super.methodFromUse(tree);
        AnnotatedExecutableType method = mfuPair.first;
        poly.annotate(tree, method);
        return mfuPair;
    }

    public Store getEmptyStore() {
        return emptyStore;
    }

    public boolean getUseFlow() {
        return useFlow;
    }

    public void setUseFlow(boolean useFlow) {
        this.useFlow = useFlow;
    }

    /**
     * @see BaseTypeChecker#getTypeFactoryOfSubchecker(Class)
     */
    public <T extends GenericAnnotatedTypeFactory<?, ?, ?, ?>, U extends BaseTypeChecker> T getTypeFactoryOfSubchecker(Class<U> checkerClass) {
        return checker.getTypeFactoryOfSubchecker(checkerClass);
    }
}
