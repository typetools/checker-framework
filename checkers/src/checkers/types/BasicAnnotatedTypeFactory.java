package checkers.types;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.DefaultFlow;
import checkers.flow.DefaultFlowState;
import checkers.flow.Flow;
import checkers.flow.analysis.AnalysisResult;
import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFAbstractValue;
import checkers.flow.analysis.checkers.CFAnalysis;
import checkers.flow.analysis.checkers.CFValue;
import checkers.flow.cfg.CFGBuilder;
import checkers.flow.cfg.ControlFlowGraph;
import checkers.quals.DefaultLocation;
import checkers.quals.DefaultQualifier;
import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.ImplicitFor;
import checkers.quals.Unqualified;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.*;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

/**
 * A factory that extends {@link AnnotatedTypeFactory} to optionally use
 * flow-sensitive qualifier inference, qualifier polymorphism, implicit annotations
 * via {@link ImplicitFor}, and user-specified defaults via {@link DefaultQualifier}
 *
 * @see Flow
 */
public class BasicAnnotatedTypeFactory<Checker extends BaseTypeChecker> extends AnnotatedTypeFactory {

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

    //// Flow related fields
    /** Should use flow analysis? */
    protected boolean useFlow;
    /** Flow sensitive instance */
    protected final Flow flow;

    /**
     * Creates a type factory for checking the given compilation unit with
     * respect to the given annotation.
     *
     * @param checker the checker to which this type factory belongs
     * @param root the compilation unit to scan
     * @param useFlow whether flow analysis should be performed
     */
    public BasicAnnotatedTypeFactory(Checker checker, CompilationUnitTree root, boolean useFlow) {
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
        for (Class<? extends Annotation> qual : checker.getSupportedTypeQualifiers()) {
            if (qual.getAnnotation(DefaultQualifierInHierarchy.class) != null) {
                defaults.addAbsoluteDefault(this.annotations.fromClass(qual),
                        Collections.singleton(DefaultLocation.ALL));
                foundDefault = true;
            }
        }

        AnnotationMirror unqualified = this.annotations.fromClass(Unqualified.class);
        if (!foundDefault && this.isSupportedQualifier(unqualified)) {
        	defaults.addAbsoluteDefault(unqualified,
        			Collections.singleton(DefaultLocation.ALL));
        }

        // This also gets called by subclasses.  Is that a problem?
        postInit();
    }

    /**
     * Creates a type factory for checking the given compilation unit with
     * respect to the given annotation.
     *
     * @param checker the checker to which this type factory belongs
     * @param root the compilation unit to scan
     */
    public BasicAnnotatedTypeFactory(Checker checker, CompilationUnitTree root) {
        this(checker, root, FLOW_BY_DEFAULT);
    }

    // **********************************************************************
    // Factory Methods for the appropriate annotator classes
    // **********************************************************************

    /**
     * Returns a {@link TreeAnnotator} that adds annotations to a type based
     * on the contents of a tree.
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
     * Returns a {@link TypeAnnotator} that adds annotations to a type based
     * on the content of the type itself.
     *
     * @return a type annotator
     */
    protected TypeAnnotator createTypeAnnotator(Checker checker) {
        return new TypeAnnotator(checker);
    }

    /**
     * Returns a {@link Flow} instance that performs flow sensitive analysis
     * to infer qualifiers on unqualified types.
     *
     * @param checker   the checker
     * @param root      the compilation unit associated with this factory
     * @param flowQuals the qualifiers to infer
     * @return  the flow analysis class
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
                    defaults.annotate(((DeclaredType)supertype.getUnderlyingType()).asElement(), supertype);
                }
            }
    }

    /**
     * Track the state of dataflow analysis scanning for each class tree
     * in the compilation unit.
     */
    protected enum ScanState { IN_PROGRESS, FINISHED };

    protected Map<ClassTree, ScanState> scannedClasses = new HashMap<>();

    /**
     * The result of the flow analysis. Invariant:
     * <pre>
     *  scannedClasses.get(c) == FINISHED for some class c ==> flowResult != null
     * </pre>
     *
     * Note that flowResult contains analysis results for Trees from
     * multiple classes which are produced by multiple calls to
     * performFlowAnalysis.
     */
    protected AnalysisResult<CFValue> flowResult = null;
	
    /**
     * Perform a dataflow analysis over a single class tree and its
     * nested classes.
     */
    protected void performFlowAnalysis(ClassTree classTree) {
        scannedClasses.put(classTree, ScanState.IN_PROGRESS);
        if (flowResult == null) {
            flowResult = new AnalysisResult<>();
        }
        Queue<ClassTree> queue = new LinkedList<>();
        queue.add(classTree);
        while (!queue.isEmpty()) {
            ClassTree ct = queue.remove();
            for (Tree m : ct.getMembers()) {
                switch (m.getKind()) {
                case METHOD:
                    MethodTree mt = (MethodTree) m;
                    ControlFlowGraph cfg = CFGBuilder.build(root, env, mt);
                    CFAnalysis analysis = new CFAnalysis(this, checker.getProcessingEnvironment());
                    analysis.performAnalysis(cfg);
                    AnalysisResult<CFValue> result = analysis.getResult();
                    flowResult.combine(result);

                    if (env.getOptions().containsKey("flowdotdir")) {
                        String dotfilename =
                            env.getOptions().get("flowdotdir") + "/" +
                            mt.getName() + ".dot";
                        // make path safe for Windows
                        dotfilename = dotfilename.replace("<", ".").replace(">", ".");
                        System.err.println("Output to DOT file: " + dotfilename);
                        analysis.outputToDotFile(dotfilename);
                    }

                    break;
                case VARIABLE:
                    // TODO: handle initializers
                    break;
                case CLASS:
                    // Visit inner and nested classes.
                    queue.add((ClassTree) m);
                    break;
                default:
                    System.err.println("Unexpected member: "+m.getKind());
                    assert false;
                    break;
                }
            }
        }

        /*ControlFlowGraph cfg = CFGBuilder.build(env, node);
        analysis = new CFAnalysis(this, checker.getProcessingEnvironment());
        analysis.performAnalysis(cfg);

        super.fromTreeCache.clear();*/
        scannedClasses.put(classTree, ScanState.FINISHED);
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

        TreePath path = trees.getPath(root, tree);
        ClassTree enclosingClass = TreeUtils.enclosingClass(path);
        if (!scannedClasses.containsKey(enclosingClass)) {
            performFlowAnalysis(enclosingClass);
        }

        treeAnnotator.visit(tree, type);

        CFValue as = flowResult.getValue(tree);
        // TODO: handle inference of more than one qualifier
        final AnnotationMirror inferred = as != null ? as.getAnnotations().iterator().next() : null;
        if (inferred != null) {
            if (!type.isAnnotated() || this.qualHierarchy.isSubtype(inferred, type.getAnnotations().iterator().next())) {
                /* TODO:
                 * The above check should NOT be necessary. However, for the InterningChecker test case Arrays fails
                 * without it. It only fails if Unqualified is one of the supported type qualifiers, which it should.
                 * Flow inference should always just return subtypes of the declared type, so something is going wrong!
                 * TODO!
                 */
                type.removeAnnotationInHierarchy(inferred);
                type.addAnnotation(inferred);
            }
        }

        // TODO: This is quite ugly
        boolean finishedScanning = scannedClasses.get(enclosingClass) == ScanState.FINISHED;
        if (finishedScanning || type.getKind() != TypeKind.TYPEVAR) {
            Element elt = InternalUtils.symbol(tree);
            typeAnnotator.visit(type, elt != null ? elt.getKind() : ElementKind.OTHER);
            defaults.annotate(tree, type);
        }
    }

    @Override
    protected void annotateImplicit(Element elt, AnnotatedTypeMirror type) {
        typeAnnotator.visit(type, elt.getKind());
        defaults.annotate(elt, type);
    }

    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(MethodInvocationTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = super.methodFromUse(tree);
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
        Set<AnnotationMirror> flowQuals = new HashSet<AnnotationMirror>();
        for (Class<? extends Annotation> cl : checker.getSupportedTypeQualifiers()) {
            flowQuals.add(annotations.fromClass(cl));
        }
        return flowQuals;
    }

}
