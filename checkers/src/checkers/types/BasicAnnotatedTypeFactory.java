package checkers.types;

import java.lang.annotation.Annotation;
import java.util.Collections;
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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.DefaultFlow;
import checkers.flow.DefaultFlowState;
import checkers.flow.Flow;
import checkers.flow.analysis.AnalysisResult;
import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFAnalysis;
import checkers.flow.analysis.checkers.CFCFGBuilder;
import checkers.flow.analysis.checkers.CFStore;
import checkers.flow.analysis.checkers.CFValue;
import checkers.flow.analysis.checkers.RegexAnalysis;
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
import checkers.quals.Unqualified;
import checkers.regex.RegexAnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.AnnotationUtils;
import checkers.util.InternalUtils;
import checkers.util.Pair;
import checkers.util.QualifierDefaults;
import checkers.util.QualifierPolymorphism;
import checkers.util.TreeUtils;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

/**
 * A factory that extends {@link AnnotatedTypeFactory} to optionally use
 * flow-sensitive qualifier inference, qualifier polymorphism, implicit
 * annotations via {@link ImplicitFor}, and user-specified defaults via
 * {@link DefaultQualifier}
 * 
 * @see Flow
 */
public class BasicAnnotatedTypeFactory<Checker extends BaseTypeChecker> extends
        AnnotatedTypeFactory {

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
    /** Flow sensitive instance */
    protected final Flow flow;

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
    public BasicAnnotatedTypeFactory(Checker checker, CompilationUnitTree root,
            boolean useFlow) {
        super(checker, root);
        this.checker = checker;
        this.treeAnnotator = createTreeAnnotator(checker);
        this.typeAnnotator = createTypeAnnotator(checker);
        this.useFlow = useFlow;
        this.poly = new QualifierPolymorphism(checker, this);
        Set<AnnotationMirror> flowQuals = createFlowQualifiers(checker);
        this.flow = useFlow ? createFlow(checker, root, flowQuals) : null;

        this.defaults = new QualifierDefaults(this, this.annotations);
        boolean foundDefault = false;
        for (Class<? extends Annotation> qual : checker
                .getSupportedTypeQualifiers()) {
            if (qual.getAnnotation(DefaultQualifierInHierarchy.class) != null) {
                defaults.addAbsoluteDefault(this.annotations.fromClass(qual),
                        Collections.singleton(DefaultLocation.ALL));
                foundDefault = true;
            }
        }

        AnnotationMirror unqualified = this.annotations
                .fromClass(Unqualified.class);
        if (!foundDefault && this.isSupportedQualifier(unqualified)) {
            defaults.addAbsoluteDefault(unqualified,
                    Collections.singleton(DefaultLocation.ALL));
        }

        // This also gets called by subclasses. Is that a problem?
        postInit();
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
    public BasicAnnotatedTypeFactory(Checker checker, CompilationUnitTree root) {
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
        return new TypeAnnotator(checker);
    }

    /**
     * Returns a {@link Flow} instance that performs flow sensitive analysis to
     * infer qualifiers on unqualified types.
     * 
     * @param checker
     *            the checker
     * @param root
     *            the compilation unit associated with this factory
     * @param flowQuals
     *            the qualifiers to infer
     * @return the flow analysis class
     */
    protected Flow createFlow(Checker checker, CompilationUnitTree root,
            Set<AnnotationMirror> flowQuals) {
        return new DefaultFlow<DefaultFlowState>(checker, root, flowQuals, this);
    }

    // **********************************************************************
    // Factory Methods for the appropriate annotator classes
    // **********************************************************************

    @Override
    protected void postDirectSuperTypes(AnnotatedTypeMirror type,
            List<? extends AnnotatedTypeMirror> supertypes) {
        super.postDirectSuperTypes(type, supertypes);
        if (type.getKind() == TypeKind.DECLARED)
            for (AnnotatedTypeMirror supertype : supertypes) {
                // FIXME: Recursive initialization for defaults fields
                if (defaults != null) {
                    defaults.annotate(((DeclaredType) supertype
                            .getUnderlyingType()).asElement(), supertype);
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
    protected AnalysisResult<CFValue, CFStore> flowResult = null;

    /**
     * A mapping from methods to their regular exit store (used to check
     * postconditions).
     */
    protected IdentityHashMap<MethodTree, CFStore> regularExitStores = null;

    /**
     * A mapping from methods to their a list with all return statements and the
     * corresponding store.
     */
    protected IdentityHashMap<MethodTree, List<Pair<ReturnNode, CFStore>>> returnStatementStores = null;

    /**
     * A mapping from methods to their a list with all return statements and the
     * corresponding store.
     */
    protected IdentityHashMap<MethodInvocationTree, CFStore> methodInvocationStores = null;

    /**
     * @return The regular exit store, or {@code null}, if there is no such
     *         store (because the method cannot exit through the regular exit
     *         block).
     */
    public/* @Nullable */CFStore getRegularExitStore(MethodTree methodTree) {
        return regularExitStores.get(methodTree);
    }

    /**
     * @return All return node and store pairs for a given method.
     */
    public List<Pair<ReturnNode, CFStore>> getReturnStatementStores(
            MethodTree methodTree) {
        assert returnStatementStores.containsKey(methodTree);
        return returnStatementStores.get(methodTree);
    }

    /**
     * @return The store immediately before a method invocation.
     */
    public CFStore getStoreBefore(MethodInvocationTree tree) {
        return flowResult.getStoreBeforeMethodInvocation(tree);
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
                for (Tree m : ct.getMembers()) {
                    switch (m.getKind()) {
                    case METHOD:
                        MethodTree mt = (MethodTree) m;
                        // Skip abstract methods because they have no body.
                        ModifiersTree modifiers = mt.getModifiers();
                        if (modifiers != null) {
                            Set<Modifier> flags = modifiers.getFlags();
                            if (flags.contains(Modifier.ABSTRACT)) {
                                break;
                            }
                        }

                        analyze(queue,
                                new CFGMethod(mt, TreeUtils
                                        .enclosingClass(getPath(mt))));
                        break;
                    case VARIABLE:
                        VariableTree vt = (VariableTree) m;
                        ExpressionTree initializer = vt.getInitializer();
                        // analyze initializer if present
                        if (initializer != null) {
                            analyze(queue, new CFGStatement(initializer));
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
                        analyze(queue, new CFGStatement(b));
                        break;
                    default:
                        assert false : "Unexpected member: " + m.getKind();
                        break;
                    }
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

    /**
     * Analyze the AST {@code ast} and store the result.
     * 
     * @param queue
     *            The queue to add more things to scan.
     * @param ast
     *            The AST to analyze.
     */

    // Maintain a deque of analyses to accomodate nested classes.
    Deque<CFAbstractAnalysis<CFValue, CFStore, ?>> analyses = new LinkedList<>();

    protected void analyze(Queue<ClassTree> queue, UnderlyingAST ast) {
        CFGBuilder builder = new CFCFGBuilder(this);
        ControlFlowGraph cfg = builder.run(root, env, ast);
        CFAbstractAnalysis<CFValue, CFStore, ?> newAnalysis =
            new CFAnalysis(this, checker.getProcessingEnvironment(),
                checker);
        // TODO: remove this hack
        if (this instanceof RegexAnnotatedTypeFactory) {
            newAnalysis = new RegexAnalysis((RegexAnnotatedTypeFactory) this,
                    checker.getProcessingEnvironment(), checker);
        }
        analyses.addFirst(newAnalysis);
        analyses.getFirst().performAnalysis(cfg);
        AnalysisResult<CFValue, CFStore> result = analyses.getFirst().getResult();

        // store result
        flowResult.combine(result);
        if (ast.getKind() == UnderlyingAST.Kind.METHOD) {
            // store exit store (for checking postconditions)
            CFGMethod mast = (CFGMethod) ast;
            MethodTree method = mast.getMethod();
            CFStore regularExitStore = analyses.getFirst().getRegularExitStore();
            if (regularExitStore != null) {
                regularExitStores.put(method, regularExitStore);
            }
            returnStatementStores.put(method,
                    analyses.getFirst().getReturnStatementStores());
        }

        if (env.getOptions().containsKey("flowdotdir")) {
            String dotfilename = env.getOptions().get("flowdotdir") + "/"
                    + dotOutputFileName(ast) + ".dot";
            // make path safe for Windows
            dotfilename = dotfilename.replace("<", ".").replace(">", ".");
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
    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type) {
        assert root != null : "root needs to be set when used on trees";
        if (useFlow) {
            annotateImplicitWithFlow(tree, type);
        } else {
            treeAnnotator.visit(tree, type);
        }
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

        CFValue as = null;
        if (!analyses.isEmpty() && tree != null) {
            as = analyses.getFirst().getValue(tree);
        }
        if (as == null && tree != null) {
            as = flowResult.getValue(tree);
        }
        final Set<AnnotationMirror> inferred = as != null ? as.getAnnotations()
                : null;
        if (inferred != null) {
            if (!type.isAnnotated()
                    || this.qualHierarchy.isSubtype(inferred,
                            type.getAnnotations())) {
                /*
                 * TODO: The above check should NOT be necessary. However, for
                 * the InterningChecker test case Arrays fails without it. It
                 * only fails if Unqualified is one of the supported type
                 * qualifiers, which it should. Flow inference should always
                 * just return subtypes of the declared type, so something is
                 * going wrong! TODO!
                 */
                for (AnnotationMirror inf : inferred) {
                    type.removeAnnotationInHierarchy(inf);
                }
                type.addAnnotations(inferred);
            }

        }
        // TODO: This is quite ugly
        boolean finishedScanning = enclosingClass == null
                || scannedClasses.get(enclosingClass) == ScanState.FINISHED;
        if (finishedScanning || type.getKind() != TypeKind.TYPEVAR) {
            Element elt = InternalUtils.symbol(tree);
            typeAnnotator.visit(type, elt != null ? elt.getKind()
                    : ElementKind.OTHER);
            defaults.annotate(tree, type);
        }
    }

    @Override
    protected void annotateImplicit(Element elt, AnnotatedTypeMirror type) {
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
        poly.annotate(method.getElement(), method);
        return mfuPair;
    }

    // **********************************************************************
    // Helper method
    // **********************************************************************

    /**
     * Returns the set of annotations to be inferred in flow analysis
     */
    protected Set<AnnotationMirror> createFlowQualifiers(Checker checker) {
        Set<AnnotationMirror> flowQuals = AnnotationUtils.createAnnotationSet();
        for (Class<? extends Annotation> cl : checker
                .getSupportedTypeQualifiers()) {
            flowQuals.add(annotations.fromClass(cl));
        }
        return flowQuals;
    }
}
