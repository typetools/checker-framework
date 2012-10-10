package checkers.types;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;

import javacutils.AnnotationUtils;
import javacutils.InternalUtils;
import javacutils.Pair;
import javacutils.TreeUtils;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.analysis.AnalysisResult;
import checkers.flow.analysis.TransferResult;
import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFAbstractStore;
import checkers.flow.analysis.checkers.CFAbstractTransfer;
import checkers.flow.analysis.checkers.CFAbstractValue;
import checkers.flow.analysis.checkers.CFAnalysis;
import checkers.flow.analysis.checkers.CFCFGBuilder;
import checkers.flow.analysis.checkers.CFStore;
import checkers.flow.analysis.checkers.CFTransfer;
import checkers.flow.analysis.checkers.CFValue;
import checkers.flow.cfg.CFGBuilder;
import checkers.flow.cfg.ControlFlowGraph;
import checkers.flow.cfg.UnderlyingAST;
import checkers.flow.cfg.UnderlyingAST.CFGMethod;
import checkers.flow.cfg.UnderlyingAST.CFGStatement;
import checkers.flow.cfg.node.Node;
import checkers.flow.cfg.node.ReturnNode;
import checkers.quals.DefaultLocation;
import checkers.quals.DefaultQualifier;
import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.ImplicitFor;
import checkers.quals.Pure;
import checkers.quals.Unqualified;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.QualifierDefaults;
import checkers.util.QualifierPolymorphism;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

/**
 * A factory that extends {@link AnnotatedTypeFactory} to optionally use
 * flow-sensitive qualifier inference, qualifier polymorphism, implicit
 * annotations via {@link ImplicitFor}, and user-specified defaults via
 * {@link DefaultQualifier}.
 */
public abstract class AbstractBasicAnnotatedTypeFactory<Checker extends BaseTypeChecker, Value extends CFAbstractValue<Value>, Store extends CFAbstractStore<Value, Store>, TransferFunction extends CFAbstractTransfer<Value, Store, TransferFunction>, FlowAnalysis extends CFAbstractAnalysis<Value, Store, TransferFunction>>
        extends AnnotatedTypeFactory {

    /** The type checker to use. */
    protected Checker checker;

    /** should use flow by default */
    protected static boolean FLOW_BY_DEFAULT = true;

    /** to annotate types based on the given tree */
    protected final TypeAnnotator typeAnnotator;
    /** to annotate types based on the given un-annotated types */
    protected final TreeAnnotator treeAnnotator;

    /** to handle any polymorphic types */
    protected final QualifierPolymorphism poly;

    /** to handle defaults specified by the user */
    protected final QualifierDefaults defaults;

    // // Flow related fields
    /** Should use flow analysis? */
    protected boolean useFlow;

    /** An empty store. */
    private Store emptyStore;

    /**
     * Creates a type factory for checking the given compilation unit with
     * respect to the given annotation.
     *
     * @param checker
     *            the checker to which this type factory belongs
     * @param root
     *            the compilation unit to scan
     * @param useFlow
     *            whether flow analysis should be performed
     */
    @SuppressWarnings("deprecation")
    // we alias a deprecated annotation to its replacement
    public AbstractBasicAnnotatedTypeFactory(Checker checker,
            CompilationUnitTree root, boolean useFlow) {
        super(checker, checker.getQualifierHierarchy(), root);
        this.checker = checker;
        this.treeAnnotator = createTreeAnnotator(checker);
        this.typeAnnotator = createTypeAnnotator(checker);
        this.useFlow = useFlow;
        this.poly = new QualifierPolymorphism(checker, this);

        this.defaults = new QualifierDefaults(elements, this);
        for (AnnotationMirror a : checker.getQualifierHierarchy().getTopAnnotations()) {
            defaults.addAbsoluteDefault(a, DefaultLocation.LOCALS);
        }
        boolean foundDefault = false;
        // TODO: should look for a default qualifier per qualifier hierarchy.
        for (Class<? extends Annotation> qual : checker
                .getSupportedTypeQualifiers()) {
            if (qual.getAnnotation(DefaultQualifierInHierarchy.class) != null) {
                defaults.addAbsoluteDefault(AnnotationUtils.fromClass(elements, qual),
                        DefaultLocation.OTHERWISE);
                foundDefault = true;
            }
        }

        AnnotationMirror unqualified = AnnotationUtils.fromClass(elements, Unqualified.class);
        if (!foundDefault && this.isSupportedQualifier(unqualified)) {
            defaults.addAbsoluteDefault(unqualified,
                    DefaultLocation.OTHERWISE);
        }

        // Add common aliases.
        addAliasedDeclAnnotation(Pure.class,
                checkers.nullness.quals.Pure.class,
                AnnotationUtils.fromClass(elements, Pure.class));

        // every subclass must call postInit!
        if (this.getClass().equals(BasicAnnotatedTypeFactory.class)) {
            this.postInit();
        }
    }

    /**
     * Creates a type factory for checking the given compilation unit with
     * respect to the given annotation.
     *
     * @param checker
     *            the checker to which this type factory belongs
     * @param root
     *            the compilation unit to scan
     */
    public AbstractBasicAnnotatedTypeFactory(Checker checker,
            CompilationUnitTree root) {
        this(checker, root, FLOW_BY_DEFAULT);
    }

    // **********************************************************************
    // Factory Methods for the appropriate annotator classes
    // **********************************************************************

    /**
     * Returns a {@link TreeAnnotator} that adds annotations to a type based on
     * the contents of a tree.
     *
     * Subclasses may override this method to specify more appriopriate
     * {@link TreeAnnotator}
     *
     * @return a tree annotator
     */
    protected TreeAnnotator createTreeAnnotator(Checker checker) {
        return new TreeAnnotator(checker, this);
    }

    /**
     * Returns a {@link TypeAnnotator} that adds annotations to a type based on
     * the content of the type itself.
     *
     * @return a type annotator
     */
    protected TypeAnnotator createTypeAnnotator(Checker checker) {
        return new TypeAnnotator(checker, this);
    }

    /**
     * Returns the appropriate flow analysis class that is used for the dataflow
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
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected FlowAnalysis createFlowAnalysis(Checker checker,
            List<Pair<VariableElement, Value>> fieldValues) {

        // Try to reflectively load the visitor.
        Class<?> checkerClass = checker.getClass();

        while (checkerClass != BaseTypeChecker.class) {
            final String classToLoad = checkerClass.getName()
                    .replace("Checker", "Analysis")
                    .replace("Subchecker", "Analysis");
            FlowAnalysis result = BaseTypeChecker.invokeConstructorFor(
                    classToLoad, new Class<?>[] { this.getClass(),
                            ProcessingEnvironment.class, checkerClass,
                            List.class }, new Object[] { this, processingEnv, checker,
                            fieldValues });
            if (result != null)
                return result;
            checkerClass = checkerClass.getSuperclass();
        }

        // If an analysis couldn't be loaded reflectively, return the
        // default.
        return (FlowAnalysis) new CFAnalysis(
                (AbstractBasicAnnotatedTypeFactory) this, processingEnv, checker,
                (List) fieldValues);
    }

    /**
     * Returns the appropriate transfer function that is used for the dataflow
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
    @SuppressWarnings("unchecked")
    public TransferFunction createFlowTransferFunction(FlowAnalysis analysis) {

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
        return (TransferFunction) new CFTransfer(
                (CFAbstractAnalysis<CFValue, CFStore, CFTransfer>) analysis);
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
     * Track the state of dataflow analysis scanning for each class tree in the
     * compilation unit.
     */
    protected enum ScanState {
        IN_PROGRESS, FINISHED
    };

    protected Map<ClassTree, ScanState> scannedClasses = new HashMap<>();

    /**
     * The result of the flow analysis. Invariant:
     *
     * <pre>
     *  scannedClasses.get(c) == FINISHED for some class c ==> flowResult != null
     * </pre>
     *
     * Note that flowResult contains analysis results for Trees from multiple
     * classes which are produced by multiple calls to performFlowAnalysis.
     */
    protected AnalysisResult<Value, Store> flowResult = null;

    /**
     * A mapping from methods (or other code blocks) to their regular exit store (used to check
     * postconditions).
     */
    protected IdentityHashMap<Tree, Store> regularExitStores = null;

    /**
     * A mapping from methods to their a list with all return statements and the
     * corresponding store.
     */
    protected IdentityHashMap<MethodTree, List<Pair<ReturnNode, TransferResult<Value, Store>>>> returnStatementStores = null;

    /**
     * A mapping from methods to their a list with all return statements and the
     * corresponding store.
     */
    protected IdentityHashMap<MethodInvocationTree, Store> methodInvocationStores = null;

    /**
     * Returns the regular exit store for a method or another code block (such as static initializers).
     *
     * @return The regular exit store, or {@code null}, if there is no such
     *         store (because the method cannot exit through the regular exit
     *         block).
     */
    public/* @Nullable */Store getRegularExitStore(Tree t) {
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
        Store store = AnalysisResult.runAnalysisFor(node, true, analysis.getStore(node.getBlock()));
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
        Store store = AnalysisResult.runAnalysisFor(node, false, analysis.getStore(node.getBlock()));
        return store;
    }

    /**
     * @return The {@link Node} for a given {@link Tree}.
     */
    public Node getNodeForTree(Tree tree) {
        return flowResult.getNodeForTree(tree);
    }

    /**
     * Perform a dataflow analysis over a single class tree and its nested
     * classes.
     */
    protected void performFlowAnalysis(ClassTree classTree) {
        if (flowResult == null) {
            regularExitStores = new IdentityHashMap<>();
            returnStatementStores = new IdentityHashMap<>();
            flowResult = new AnalysisResult<>();
        }
        // no need to scan interfaces or enums
        if (classTree.getKind() == Kind.INTERFACE
                || classTree.getKind() == Kind.ENUM
                || classTree.getKind() == Kind.ANNOTATION_TYPE) {
            // Mark finished so that default annotations will be applied.
            scannedClasses.put(classTree, ScanState.FINISHED);
            return;
        }

        Queue<ClassTree> queue = new LinkedList<>();
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

            try {
                List<MethodTree> methods = new ArrayList<>();
                List<Pair<VariableElement, Value>> fieldValues = new ArrayList<>();
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

                        // Wait with scanning the method until all other members
                        // have been processed.
                        methods.add(mt);
                        break;
                    case VARIABLE:
                        VariableTree vt = (VariableTree) m;
                        ExpressionTree initializer = vt.getInitializer();
                        // analyze initializer if present
                        if (initializer != null) {
                            analyze(queue, new CFGStatement(initializer),
                                    fieldValues);
                            Value value = flowResult.getValue(initializer);
                            if (value != null) {
                                // Store the abstract value for the field.
                                VariableElement element = TreeUtils
                                        .elementFromDeclaration(vt);
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
                        analyze(queue, new CFGStatement(b), fieldValues);
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
                    analyze(queue,
                            new CFGMethod(mt, TreeUtils
                                    .enclosingClass(getPath(mt))), fieldValues);
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

    // Maintain a deque of analyses to accomodate nested classes.
    Deque<FlowAnalysis> analyses = new LinkedList<>();

    /**
     * Analyze the AST {@code ast} and store the result.
     *
     * @param queue
     *            The queue to add more things to scan.
     * @param fieldValues
     *            The abstract values for all fields of the same class.
     * @param ast
     *            The AST to analyze.
     */
    protected void analyze(Queue<ClassTree> queue, UnderlyingAST ast,
            List<Pair<VariableElement, Value>> fieldValues) {
        CFGBuilder builder = new CFCFGBuilder(checker);
        ControlFlowGraph cfg = builder.run(this, root, processingEnv, ast);
        FlowAnalysis newAnalysis = createFlowAnalysis(getChecker(), fieldValues);
        if (emptyStore == null) {
            emptyStore = newAnalysis.createEmptyStore(!getProcessingEnv()
                    .getOptions().containsKey("concurrentSemantics"));
        }
        analyses.addFirst(newAnalysis);
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
        }

        if (processingEnv.getOptions().containsKey("flowdotdir")) {
            String dotfilename = processingEnv.getOptions().get("flowdotdir") + "/"
                    + dotOutputFileName(ast) + ".dot";
            // make path safe for Windows
            dotfilename = dotfilename.replace("<", "_").replace(">", "");
            System.err.println("Output to DOT file: " + dotfilename);
            analyses.getFirst().outputToDotFile(dotfilename);
        }

        analyses.removeFirst();

        // add classes declared in method
        queue.addAll(builder.getDeclaredClasses());
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

    @Override
    public AnnotatedTypeMirror getDefaultedAnnotatedType(Tree tree) {
        AnnotatedTypeMirror res = null;
        if (tree instanceof VariableTree) {
            res = fromMember(tree);
            annotateImplicit(((VariableTree) tree).getType(), res, false);
        } else if (tree instanceof AssignmentTree) {
            res = fromExpression(((AssignmentTree) tree).getVariable());
            annotateImplicit((AssignmentTree) tree, res, false);
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

    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type,
            boolean iUseFlow) {
        assert root != null : "root needs to be set when used on trees";
        if (iUseFlow) {
            annotateImplicitWithFlow(tree, type);
        } else {
            treeAnnotator.visit(tree, type);
            Element elt = InternalUtils.symbol(tree);
            typeAnnotator.visit(type, elt != null ? elt.getKind()
                    : ElementKind.OTHER);
            defaults.annotate(tree, type);
        }
    }

    @Override
    public void annotateImplicit(Tree tree, AnnotatedTypeMirror type) {
        annotateImplicit(tree, type, this.useFlow);
    }

    protected void annotateImplicitWithFlow(Tree tree, AnnotatedTypeMirror type) {
        assert useFlow : "useFlow must be true to use flow analysis";

        // This function can be called on Trees outside of the current
        // compilation unit root.
        TreePath path = trees.getPath(root, tree);
        ClassTree enclosingClass = null;
        if (path != null) {
            enclosingClass = TreeUtils.enclosingClass(path);
            if (!scannedClasses.containsKey(enclosingClass)) {
                performFlowAnalysis(enclosingClass);
            }
        }

        treeAnnotator.visit(tree, type);

        // TODO: This is quite ugly
        boolean finishedScanning = enclosingClass == null
                || scannedClasses.get(enclosingClass) == ScanState.FINISHED;
        if (finishedScanning || type.getKind() != TypeKind.TYPEVAR) {
            Element elt = InternalUtils.symbol(tree);
            typeAnnotator.visit(type, elt != null ? elt.getKind()
                    : ElementKind.OTHER);
            defaults.annotate(tree, type);
        }

        Value as = null;
        if (!analyses.isEmpty() && tree != null) {
            as = analyses.getFirst().getValue(tree);
        }
        if (as == null && tree != null) {
            as = flowResult.getValue(tree);
        }
        if (as != null) {
            AnnotatedTypeMirror inferred = as.getType();
            for (AnnotationMirror top : getQualifierHierarchy()
                    .getTopAnnotations()) {
                AnnotationMirror inferredAnnotation = inferred
                        .getAnnotationInHierarchy(top);
                if (inferredAnnotation == null) {
                    // We inferred "no annotation" for this hierarchy.
                    type.removeAnnotationInHierarchy(top);
                } else {
                    // We inferred an annotation.
                    AnnotationMirror present = type
                            .getAnnotationInHierarchy(top);
                    if (present != null) {
                        if (getQualifierHierarchy().isSubtype(
                                inferredAnnotation, present)) {
                            // TODO: why is the above check needed?
                            // Shouldn't inferred qualifiers always be
                            // subtypes?
                            type.replaceAnnotation(inferredAnnotation);
                        }
                    } else {
                        type.addAnnotation(inferredAnnotation);
                    }
                }
            }
        }
    }

    @Override
    public void annotateImplicit(Element elt, AnnotatedTypeMirror type) {
        typeAnnotator.visit(type, elt.getKind());
        defaults.annotate(elt, type);
    }

    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(
            MethodInvocationTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = super
                .methodFromUse(tree);
        AnnotatedExecutableType method = mfuPair.first;
        poly.annotate(tree, method);
        return mfuPair;
    }

    public Checker getChecker() {
        return checker;
    }

    public Store getEmptyStore() {
        return emptyStore;
    }

    public void setEmptyStore(Store emptyStore) {
        this.emptyStore = emptyStore;
    }
}
