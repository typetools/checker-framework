package org.checkerframework.framework.type;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.AnalysisResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.LocalVariable;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.CFGVisualizer;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.DOTCFGVisualizer;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGLambda;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGStatement;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
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
import org.checkerframework.framework.qual.DefaultInUncheckedCodeFor;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchyInUncheckedCode;
import org.checkerframework.framework.qual.MonotonicQualifier;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.poly.DefaultQualifierPolymorphism;
import org.checkerframework.framework.type.poly.QualifierPolymorphism;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.DefaultForTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.DefaultQualifierForUseTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.IrrelevantTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.PropagationTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;
import org.checkerframework.framework.util.dependenttypes.DependentTypesTreeAnnotator;
import org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.CollectionUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.UserError;

/**
 * A factory that extends {@link AnnotatedTypeFactory} to optionally use flow-sensitive qualifier
 * inference, qualifier polymorphism, default annotations via {@link DefaultFor}, and user-specified
 * defaults via {@link DefaultQualifier}.
 */
public abstract class GenericAnnotatedTypeFactory<
                Value extends CFAbstractValue<Value>,
                Store extends CFAbstractStore<Value, Store>,
                TransferFunction extends CFAbstractTransfer<Value, Store, TransferFunction>,
                FlowAnalysis extends CFAbstractAnalysis<Value, Store, TransferFunction>>
        extends AnnotatedTypeFactory {

    /** Should use flow by default. */
    protected static boolean flowByDefault = true;

    /** To cache the supported monotonic type qualifiers. */
    private Set<Class<? extends Annotation>> supportedMonotonicQuals;

    /** to annotate types based on the given tree */
    protected TypeAnnotator typeAnnotator;

    /** for use in addAnnotationsFromDefaultQualifierForUse */
    private DefaultQualifierForUseTypeAnnotator defaultQualifierForUseTypeAnnotator;

    /** to annotate types based on the given un-annotated types */
    protected TreeAnnotator treeAnnotator;

    /** to handle any polymorphic types */
    protected QualifierPolymorphism poly;

    /** to handle defaults specified by the user */
    protected QualifierDefaults defaults;

    /** to handle dependent type annotations */
    protected DependentTypesHelper dependentTypesHelper;

    // Flow related fields

    /**
     * Should use flow-sensitive type refinement analysis? This value can be changed when an
     * AnnotatedTypeMirror without annotations from data flow is required.
     *
     * @see #getAnnotatedTypeLhs(Tree)
     */
    private boolean useFlow;

    /** Is this type factory configured to use flow-sensitive type refinement? */
    private final boolean everUseFlow;

    /**
     * Should the local variable default annotation be applied to type variables?
     *
     * <p>It is initialized to true if data flow is used by the checker. It is set to false when
     * getting the assignment context for type argument inference.
     *
     * @see GenericAnnotatedTypeFactory#getAnnotatedTypeLhsNoTypeVarDefault
     */
    private boolean shouldDefaultTypeVarLocals;

    /** An empty store. */
    // Set in postInit only
    protected Store emptyStore;

    // Set in postInit only
    protected FlowAnalysis analysis;

    // Set in postInit only
    protected TransferFunction transfer;

    // Maintain for every class the store that is used when we analyze initialization code
    protected Store initializationStore;

    // Maintain for every class the store that is used when we analyze static initialization code
    protected Store initializationStaticStore;

    /**
     * Caches for {@link AnalysisResult#runAnalysisFor(Node, boolean, TransferInput,
     * IdentityHashMap, Map)}. This cache is enabled if {@link #shouldCache} is true. The cache size
     * is derived from {@link #getCacheSize()}.
     *
     * @see AnalysisResult#runAnalysisFor(Node, boolean, TransferInput, IdentityHashMap, Map)
     */
    protected final Map<
                    TransferInput<Value, Store>,
                    IdentityHashMap<Node, TransferResult<Value, Store>>>
            flowResultAnalysisCaches;

    /**
     * Creates a type factory for checking the given compilation unit with respect to the given
     * annotation.
     *
     * @param checker the checker to which this type factory belongs
     * @param useFlow whether flow analysis should be performed
     */
    public GenericAnnotatedTypeFactory(BaseTypeChecker checker, boolean useFlow) {
        super(checker);

        this.everUseFlow = useFlow;
        this.shouldDefaultTypeVarLocals = useFlow;
        this.useFlow = useFlow;

        this.scannedClasses = new HashMap<>();
        this.flowResult = null;
        this.regularExitStores = null;
        this.methodInvocationStores = null;
        this.returnStatementStores = null;

        this.initializationStore = null;
        this.initializationStaticStore = null;

        this.cfgVisualizer = createCFGVisualizer();

        if (shouldCache) {
            int cacheSize = getCacheSize();
            flowResultAnalysisCaches = CollectionUtils.createLRUCache(cacheSize);
        } else {
            flowResultAnalysisCaches = null;
        }

        // Every subclass must call postInit, but it must be called after
        // all other initialization is finished.
    }

    @Override
    protected void postInit() {
        super.postInit();

        this.dependentTypesHelper = createDependentTypesHelper();
        this.defaults = createAndInitQualifierDefaults();
        this.treeAnnotator = createTreeAnnotator();
        this.typeAnnotator = createTypeAnnotator();
        this.defaultQualifierForUseTypeAnnotator = createDefaultForUseTypeAnnotator();

        this.poly = createQualifierPolymorphism();

        this.analysis = createFlowAnalysis(new ArrayList<>());
        this.transfer = analysis.getTransferFunction();
        this.emptyStore = analysis.createEmptyStore(transfer.usesSequentialSemantics());

        this.parseStubFiles();
    }

    /**
     * Performs flow-sensitive type refinement on {@code classTree} if this type factory is
     * configured to do so.
     *
     * @param classTree tree on which to perform flow-sensitive type refinement
     */
    @Override
    public void preProcessClassTree(ClassTree classTree) {
        if (this.everUseFlow) {
            checkAndPerformFlowAnalysis(classTree);
        }
    }

    /**
     * Creates a type factory for checking the given compilation unit with respect to the given
     * annotation.
     *
     * @param checker the checker to which this type factory belongs
     */
    public GenericAnnotatedTypeFactory(BaseTypeChecker checker) {
        this(checker, flowByDefault);
    }

    @Override
    public void setRoot(@Nullable CompilationUnitTree root) {
        super.setRoot(root);
        this.scannedClasses.clear();
        this.flowResult = null;
        this.regularExitStores = null;
        this.methodInvocationStores = null;
        this.returnStatementStores = null;
        this.initializationStore = null;
        this.initializationStaticStore = null;

        if (shouldCache) {
            this.flowResultAnalysisCaches.clear();
            this.defaultQualifierForUseTypeAnnotator.clearCache();
        }
    }

    // **********************************************************************
    // Factory Methods for the appropriate annotator classes
    // **********************************************************************

    /**
     * Returns an immutable set of the <em>monotonic</em> type qualifiers supported by this checker.
     *
     * @return the monotonic type qualifiers supported this processor, or an empty set if none
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
     * Returns a {@link TreeAnnotator} that adds annotations to a type based on the contents of a
     * tree.
     *
     * <p>The default tree annotator is a {@link ListTreeAnnotator} of the following:
     *
     * <ol>
     *   <li>{@link PropagationTreeAnnotator}: Propagates annotations from subtrees
     *   <li>{@link LiteralTreeAnnotator}: Adds annotations based on {@link QualifierForLiterals}
     *       meta-annotations
     *   <li>{@link DependentTypesTreeAnnotator}: Adapts dependent annotations based on context
     * </ol>
     *
     * <p>Subclasses may override this method to specify additional tree annotators, for example:
     *
     * <pre>
     * new ListTreeAnnotator(super.createTreeAnnotator(), new KeyLookupTreeAnnotator(this));
     * </pre>
     *
     * @return a tree annotator
     */
    protected TreeAnnotator createTreeAnnotator() {
        List<TreeAnnotator> treeAnnotators = new ArrayList<>();
        treeAnnotators.add(new PropagationTreeAnnotator(this));
        treeAnnotators.add(new LiteralTreeAnnotator(this).addStandardLiteralQualifiers());
        if (dependentTypesHelper != null) {
            treeAnnotators.add(dependentTypesHelper.createDependentTypesTreeAnnotator(this));
        }
        return new ListTreeAnnotator(treeAnnotators);
    }

    /**
     * Returns a {@link DefaultForTypeAnnotator} that adds annotations to a type based on the
     * content of the type itself.
     *
     * <p>Subclass may override this method. The default type annotator is a {@link
     * ListTypeAnnotator} of the following:
     *
     * <ol>
     *   <li>{@link IrrelevantTypeAnnotator}: Adds top to types not listed in the {@link
     *       RelevantJavaTypes} annotation on the checker.
     *   <li>{@link PropagationTypeAnnotator}: Propagates annotation onto wildcards.
     *   <li>{@link DefaultForTypeAnnotator}: Adds annotations based on {@link DefaultFor}
     *       meta-annotations.
     * </ol>
     *
     * @return a type annotator
     */
    protected TypeAnnotator createTypeAnnotator() {
        List<TypeAnnotator> typeAnnotators = new ArrayList<>();
        RelevantJavaTypes relevantJavaTypes =
                checker.getClass().getAnnotation(RelevantJavaTypes.class);
        if (relevantJavaTypes != null) {
            Class<?>[] relevantClasses = relevantJavaTypes.value();
            // Must be first in order to annotate all irrelevant types.
            typeAnnotators.add(
                    new IrrelevantTypeAnnotator(
                            this, getQualifierHierarchy().getTopAnnotations(), relevantClasses));
        }
        typeAnnotators.add(new PropagationTypeAnnotator(this));
        typeAnnotators.add(new DefaultForTypeAnnotator(this));
        return new ListTypeAnnotator(typeAnnotators);
    }

    /** Creates an {@link DefaultQualifierForUseTypeAnnotator}. */
    protected DefaultQualifierForUseTypeAnnotator createDefaultForUseTypeAnnotator() {
        return new DefaultQualifierForUseTypeAnnotator(this);
    }

    /**
     * Returns the appropriate flow analysis class that is used for the
     * org.checkerframework.dataflow analysis.
     *
     * <p>This implementation uses the checker naming convention to create the appropriate analysis.
     * If no transfer function is found, it returns an instance of {@link CFAnalysis}.
     *
     * <p>Subclasses have to override this method to create the appropriate analysis if they do not
     * follow the checker naming convention.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected FlowAnalysis createFlowAnalysis(List<Pair<VariableElement, Value>> fieldValues) {

        // Try to reflectively load the visitor.
        Class<?> checkerClass = checker.getClass();

        while (checkerClass != BaseTypeChecker.class) {
            final String classToLoad =
                    checkerClass
                            .getName()
                            .replace("Checker", "Analysis")
                            .replace("Subchecker", "Analysis");
            FlowAnalysis result =
                    BaseTypeChecker.invokeConstructorFor(
                            classToLoad,
                            new Class<?>[] {BaseTypeChecker.class, this.getClass(), List.class},
                            new Object[] {checker, this, fieldValues});
            if (result != null) {
                return result;
            }
            checkerClass = checkerClass.getSuperclass();
        }

        // If an analysis couldn't be loaded reflectively, return the
        // default.
        List<Pair<VariableElement, CFValue>> tmp = new ArrayList<>();
        for (Pair<VariableElement, Value> fieldVal : fieldValues) {
            assert fieldVal.second instanceof CFValue;
            tmp.add(Pair.of(fieldVal.first, (CFValue) fieldVal.second));
        }
        return (FlowAnalysis) new CFAnalysis(checker, (GenericAnnotatedTypeFactory) this, tmp);
    }

    /**
     * Returns the appropriate transfer function that is used for the org.checkerframework.dataflow
     * analysis.
     *
     * <p>This implementation uses the checker naming convention to create the appropriate transfer
     * function. If no transfer function is found, it returns an instance of {@link CFTransfer}.
     *
     * <p>Subclasses have to override this method to create the appropriate transfer function if
     * they do not follow the checker naming convention.
     */
    // A more precise type for the parameter would be FlowAnalysis, which
    // is the type parameter bounded by the current parameter type CFAbstractAnalysis<Value, Store,
    // TransferFunction>.
    // However, we ran into issues in callers of the method if we used that type.
    public TransferFunction createFlowTransferFunction(
            CFAbstractAnalysis<Value, Store, TransferFunction> analysis) {

        // Try to reflectively load the visitor.
        Class<?> checkerClass = checker.getClass();

        while (checkerClass != BaseTypeChecker.class) {
            final String classToLoad =
                    checkerClass
                            .getName()
                            .replace("Checker", "Transfer")
                            .replace("Subchecker", "Transfer");
            TransferFunction result =
                    BaseTypeChecker.invokeConstructorFor(
                            classToLoad,
                            new Class<?>[] {analysis.getClass()},
                            new Object[] {analysis});
            if (result != null) {
                return result;
            }
            checkerClass = checkerClass.getSuperclass();
        }

        // If a transfer function couldn't be loaded reflectively, return the
        // default.
        @SuppressWarnings("unchecked")
        TransferFunction ret =
                (TransferFunction)
                        new CFTransfer((CFAbstractAnalysis<CFValue, CFStore, CFTransfer>) analysis);
        return ret;
    }

    /**
     * Creates an {@link DependentTypesHelper} and returns it.
     *
     * @return a new {@link DependentTypesHelper}
     */
    protected DependentTypesHelper createDependentTypesHelper() {
        DependentTypesHelper helper = new DependentTypesHelper(this);
        if (helper.hasDependentAnnotations()) {
            return helper;
        }
        return null;
    }

    public DependentTypesHelper getDependentTypesHelper() {
        return dependentTypesHelper;
    }

    @Override
    public AnnotatedDeclaredType fromNewClass(NewClassTree newClassTree) {
        AnnotatedDeclaredType superResult = super.fromNewClass(newClassTree);
        if (dependentTypesHelper != null) {
            dependentTypesHelper.standardizeNewClassTree(newClassTree, superResult);
        }
        return superResult;
    }

    /**
     * Create {@link QualifierDefaults} which handles checker specified defaults, and initialize the
     * created {@link QualifierDefaults}. Subclasses should override {@link
     * GenericAnnotatedTypeFactory#addCheckedCodeDefaults(QualifierDefaults defs)} or {@link
     * GenericAnnotatedTypeFactory#addUncheckedCodeDefaults(QualifierDefaults defs)} to add more
     * defaults or use different defaults.
     *
     * @return the QualifierDefaults object
     */
    // TODO: When changing this method, also look into
    // {@link
    // org.checkerframework.common.wholeprograminference.WholeProgramInferenceScenesHelper#shouldIgnore}.
    // Both methods should have some functionality merged into a single location.
    // See Issue 683
    // https://github.com/typetools/checker-framework/issues/683
    protected final QualifierDefaults createAndInitQualifierDefaults() {
        QualifierDefaults defs = createQualifierDefaults();
        addCheckedCodeDefaults(defs);
        addCheckedStandardDefaults(defs);
        addUncheckedCodeDefaults(defs);
        addUncheckedStandardDefaults(defs);
        checkForDefaultQualifierInHierarchy(defs);

        return defs;
    }

    /**
     * Create {@link QualifierDefaults} which handles checker specified defaults. Sub-classes
     * override this method to provide a different {@code QualifierDefault} implementation.
     */
    protected QualifierDefaults createQualifierDefaults() {
        return new QualifierDefaults(elements, this);
    }

    /** Defines alphabetical sort ordering for qualifiers. */
    private static final Comparator<Class<? extends Annotation>> QUALIFIER_SORT_ORDERING =
            new Comparator<Class<? extends Annotation>>() {
                @Override
                public int compare(Class<? extends Annotation> a1, Class<? extends Annotation> a2) {
                    return a1.getCanonicalName().compareTo(a2.getCanonicalName());
                }
            };

    /**
     * Creates and returns a string containing the number of qualifiers and the canonical class
     * names of each qualifier that has been added to this checker's supported qualifier set. The
     * names are alphabetically sorted.
     *
     * @return a string containing the number of qualifiers and canonical names of each qualifier
     */
    protected final String getSortedQualifierNames() {
        Set<Class<? extends Annotation>> stq = getSupportedTypeQualifiers();
        if (stq.isEmpty()) {
            return "No qualifiers examined";
        }
        if (stq.size() == 1) {
            return "1 qualifier examined: " + stq.iterator().next().getCanonicalName();
        }

        // Create a list of the supported qualifiers and sort the list
        // alphabetically
        List<Class<? extends Annotation>> sortedSupportedQuals = new ArrayList<>();
        sortedSupportedQuals.addAll(stq);
        Collections.sort(sortedSupportedQuals, QUALIFIER_SORT_ORDERING);

        // display the number of qualifiers as well as the names of each
        // qualifier.
        StringBuilder sb = new StringBuilder();
        sb.append(sortedSupportedQuals.size());
        sb.append(" qualifiers examined");

        sb.append(": ");
        // for each qualifier, add its canonical name, a comma and a space
        // to the string.
        for (Class<? extends Annotation> qual : sortedSupportedQuals) {
            sb.append(qual.getCanonicalName());
            sb.append(", ");
        }
        // remove last comma and space
        return sb.substring(0, sb.length() - 2);
    }

    /**
     * Adds default qualifiers for type-checked code by reading {@link DefaultFor} and {@link
     * DefaultQualifierInHierarchy} meta-annotations. Subclasses may override this method to add
     * defaults that cannot be specified with a {@link DefaultFor} or {@link
     * DefaultQualifierInHierarchy} meta-annotations.
     *
     * @param defs QualifierDefault object to which defaults are added
     */
    protected void addCheckedCodeDefaults(QualifierDefaults defs) {
        // Add defaults from @DefaultFor and @DefaultQualifierInHierarchy
        for (Class<? extends Annotation> qual : getSupportedTypeQualifiers()) {
            DefaultFor defaultFor = qual.getAnnotation(DefaultFor.class);
            if (defaultFor != null) {
                final TypeUseLocation[] locations = defaultFor.value();
                defs.addCheckedCodeDefaults(AnnotationBuilder.fromClass(elements, qual), locations);
            }

            if (qual.getAnnotation(DefaultQualifierInHierarchy.class) != null) {
                defs.addCheckedCodeDefault(
                        AnnotationBuilder.fromClass(elements, qual), TypeUseLocation.OTHERWISE);
            }
        }
    }

    /**
     * Adds the standard CLIMB defaults that do not conflict with previously added defaults.
     *
     * @param defs {@link QualifierDefaults} object to which defaults are added
     */
    protected void addCheckedStandardDefaults(QualifierDefaults defs) {
        if (this.everUseFlow) {
            defs.addClimbStandardDefaults();
        }
    }

    /**
     * Adds default qualifiers for code that is not type-checked by reading
     * {@code @DefaultInUncheckedCodeFor} and {@code @DefaultQualifierInHierarchyInUncheckedCode}
     * meta-annotations. Then it applies the standard unchecked code defaults, if a default was not
     * specified for a particular location.
     *
     * <p>Standard unchecked code default are: <br>
     * top: {@code TypeUseLocation.RETURN,TypeUseLocation.FIELD,TypeUseLocation.UPPER_BOUND}<br>
     * bottom: {@code TypeUseLocation.PARAMETER, TypeUseLocation.LOWER_BOUND}<br>
     *
     * <p>If {@code @DefaultQualifierInHierarchyInUncheckedCode} code is not found or a default for
     * {@code TypeUseLocation.Otherwise} is not used, the defaults for checked code will be applied
     * to locations without a default for unchecked code.
     *
     * <p>Subclasses may override this method to add defaults that cannot be specified with a
     * {@code @DefaultInUncheckedCodeFor} or {@code @DefaultQualifierInHierarchyInUncheckedCode}
     * meta-annotations or to change the standard defaults.
     *
     * @param defs {@link QualifierDefaults} object to which defaults are added
     */
    protected void addUncheckedCodeDefaults(QualifierDefaults defs) {
        for (Class<? extends Annotation> annotation : getSupportedTypeQualifiers()) {
            DefaultInUncheckedCodeFor defaultInUncheckedCodeFor =
                    annotation.getAnnotation(DefaultInUncheckedCodeFor.class);

            if (defaultInUncheckedCodeFor != null) {
                final TypeUseLocation[] locations = defaultInUncheckedCodeFor.value();
                defs.addUncheckedCodeDefaults(
                        AnnotationBuilder.fromClass(elements, annotation), locations);
            }

            if (annotation.getAnnotation(DefaultQualifierInHierarchyInUncheckedCode.class)
                    != null) {
                defs.addUncheckedCodeDefault(
                        AnnotationBuilder.fromClass(elements, annotation),
                        TypeUseLocation.OTHERWISE);
            }
        }
    }

    /**
     * Adds standard unchecked defaults that do not conflict with previously added defaults.
     *
     * @param defs {@link QualifierDefaults} object to which defaults are added
     */
    protected void addUncheckedStandardDefaults(QualifierDefaults defs) {
        defs.addUncheckedStandardDefaults();
    }

    /**
     * Check that a default qualifier (in at least one hierarchy) has been set and issue an error if
     * not.
     *
     * @param defs {@link QualifierDefaults} object to which defaults are added
     */
    protected void checkForDefaultQualifierInHierarchy(QualifierDefaults defs) {
        if (!defs.hasDefaultsForCheckedCode()) {
            throw new BugInCF(
                    "GenericAnnotatedTypeFactory.createQualifierDefaults: "
                            + "@DefaultQualifierInHierarchy or @DefaultFor(TypeUseLocation.OTHERWISE) not found. "
                            + "Every checker must specify a default qualifier. "
                            + getSortedQualifierNames());
        }

        // Don't require @DefaultQualifierInHierarchyInUncheckedCode or an
        // unchecked default for TypeUseLocation.OTHERWISE.
        // If a default unchecked code qualifier isn't specified, the defaults
        // for checked code will be used.
    }

    /**
     * Creates the {@link QualifierPolymorphism} instance which supports the QualifierPolymorphism
     * mechanism.
     *
     * @return the QualifierPolymorphism instance to use
     */
    protected QualifierPolymorphism createQualifierPolymorphism() {
        return new DefaultQualifierPolymorphism(processingEnv, this);
    }

    /**
     * Gives the current {@link QualifierPolymorphism} instance which supports the
     * QualifierPolymorphism mechanism.
     *
     * @return the QualifierPolymorphism instance to use
     */
    public QualifierPolymorphism getQualifierPolymorphism() {
        return this.poly;
    }

    // **********************************************************************
    // Factory Methods for the appropriate annotator classes
    // **********************************************************************

    @Override
    protected void postDirectSuperTypes(
            AnnotatedTypeMirror type, List<? extends AnnotatedTypeMirror> supertypes) {
        super.postDirectSuperTypes(type, supertypes);
        if (type.getKind() == TypeKind.DECLARED) {
            for (AnnotatedTypeMirror supertype : supertypes) {
                Element elt = ((DeclaredType) supertype.getUnderlyingType()).asElement();
                addComputedTypeAnnotations(elt, supertype);
            }
        }
    }

    /**
     * Gets the type of the resulting constructor call of a MemberReferenceTree.
     *
     * @param memberReferenceTree MemberReferenceTree where the member is a constructor
     * @param constructorType AnnotatedExecutableType of the declaration of the constructor
     * @return AnnotatedTypeMirror of the resulting type of the constructor
     */
    public AnnotatedTypeMirror getResultingTypeOfConstructorMemberReference(
            MemberReferenceTree memberReferenceTree, AnnotatedExecutableType constructorType) {
        assert memberReferenceTree.getMode() == MemberReferenceTree.ReferenceMode.NEW;

        // The return type for constructors should only have explicit annotations from the
        // constructor.  Recreate some of the logic from TypeFromTree.visitNewClass here.

        // The return type of the constructor will be the type of the expression of the member
        // reference tree.
        AnnotatedDeclaredType constructorReturnType =
                (AnnotatedDeclaredType) fromTypeTree(memberReferenceTree.getQualifierExpression());

        // Keep only explicit annotations and those from @Poly
        AnnotatedTypes.copyOnlyExplicitConstructorAnnotations(
                this, constructorReturnType, constructorType);

        // Now add back defaulting.
        addComputedTypeAnnotations(
                memberReferenceTree.getQualifierExpression(), constructorReturnType);
        return constructorReturnType;
    }

    /**
     * Returns the primary annotation on expression if it were evaluated at path.
     *
     * @param expression a Java expression
     * @param tree current tree
     * @param path location at which expression is evaluated
     * @param clazz class of the annotation
     * @return the annotation on expression or null if one does not exist
     * @throws FlowExpressionParseException thrown if the expression cannot be parsed
     */
    public AnnotationMirror getAnnotationFromJavaExpressionString(
            String expression, Tree tree, TreePath path, Class<? extends Annotation> clazz)
            throws FlowExpressionParseException {

        Receiver expressionObj = getReceiverFromJavaExpressionString(expression, path);
        return getAnnotationFromReceiver(expressionObj, tree, clazz);
    }
    /**
     * Returns the primary annotation on a receiver.
     *
     * @param receiver the receiver for which the annotation is returned
     * @param tree current tree
     * @param clazz the Class of the annotation
     * @return the annotation on expression or null if one does not exist
     */
    public AnnotationMirror getAnnotationFromReceiver(
            Receiver receiver, Tree tree, Class<? extends Annotation> clazz) {

        AnnotationMirror annotationMirror = null;
        if (CFAbstractStore.canInsertReceiver(receiver)) {
            Store store = getStoreBefore(tree);
            Value value = store.getValue(receiver);
            if (value != null) {
                annotationMirror =
                        AnnotationUtils.getAnnotationByClass(value.getAnnotations(), clazz);
            }
        }
        // If the specific annotation wasn't in the store, look in the type factory.
        if (annotationMirror == null) {
            if (receiver instanceof LocalVariable) {
                Element ele = ((LocalVariable) receiver).getElement();
                annotationMirror = getAnnotatedType(ele).getAnnotation(clazz);
            } else if (receiver instanceof FieldAccess) {
                Element ele = ((FieldAccess) receiver).getField();
                annotationMirror = getAnnotatedType(ele).getAnnotation(clazz);
            }
        }
        return annotationMirror;
    }

    /**
     * Produces the receiver associated with expression on currentPath.
     *
     * @param expression a Java expression
     * @param currentPath location at which expression is evaluated
     * @throws FlowExpressionParseException thrown if the expression cannot be parsed
     */
    public Receiver getReceiverFromJavaExpressionString(String expression, TreePath currentPath)
            throws FlowExpressionParseException {
        TypeMirror enclosingClass = TreeUtils.typeOf(TreeUtils.enclosingClass(currentPath));

        Receiver r = FlowExpressions.internalReprOfPseudoReceiver(currentPath, enclosingClass);
        FlowExpressionParseUtil.FlowExpressionContext context =
                new FlowExpressionParseUtil.FlowExpressionContext(
                        r,
                        FlowExpressions.getParametersOfEnclosingMethod(this, currentPath),
                        this.getContext());

        return FlowExpressionParseUtil.parse(expression, context, currentPath, true);
    }

    /**
     * Produces the receiver and offset associated with an expression. For instance, "n+1" has no
     * associated Receiver, but this method produces a pair of a Receiver (for "n") and an offset
     * ("1").
     *
     * @param expression a Java expression, possibly with a constant offset
     * @param currentPath location at which expression is evaluated
     * @return receiver and offset for the given expression
     * @throws FlowExpressionParseException thrown if the expression cannot be parsed
     */
    public Pair<Receiver, String> getReceiverAndOffsetFromJavaExpressionString(
            String expression, TreePath currentPath) throws FlowExpressionParseException {
        Pair<String, String> p = getExpressionAndOffset(expression);
        Receiver r = getReceiverFromJavaExpressionString(p.first, currentPath);
        return Pair.of(r, p.second);
    }

    /**
     * Returns the annotation mirror from dataflow for {@code expression}.
     *
     * <p>This will output a different annotation than {@link
     * #getAnnotationFromJavaExpressionString(String, Tree, TreePath, Class)}, because if the
     * specified annotation isn't found in the store, the type from the factory is used.
     *
     * @param expression a Java expression
     * @param tree the tree at the location to parse the expression
     * @param currentPath location at which expression is evaluated
     * @throws FlowExpressionParseException thrown if the expression cannot be parsed
     * @return an AnnotationMirror representing the type in the store at the given location from
     *     this type factory's type system, or null if one is not available
     */
    public AnnotationMirror getAnnotationMirrorFromJavaExpressionString(
            String expression, Tree tree, TreePath currentPath)
            throws FlowExpressionParseException {
        Receiver rec = getReceiverFromJavaExpressionString(expression, currentPath);
        if (rec == null || !CFAbstractStore.canInsertReceiver(rec)) {
            return null;
        }
        Store store = getStoreBefore(tree);
        Value value = store.getValue(rec);
        return value != null ? value.getAnnotations().iterator().next() : null;
    }

    /**
     * Track the state of org.checkerframework.dataflow analysis scanning for each class tree in the
     * compilation unit.
     */
    protected enum ScanState {
        /** Dataflow analysis in progress. */
        IN_PROGRESS,
        /** Dataflow analysis finished. */
        FINISHED
    }

    /** Map from ClassTree to their dataflow analysis state. */
    protected final Map<ClassTree, ScanState> scannedClasses;

    /**
     * The result of the flow analysis. Invariant:
     *
     * <pre>
     *  scannedClasses.get(c) == FINISHED for some class c &rArr; flowResult != null
     * </pre>
     *
     * Note that flowResult contains analysis results for Trees from multiple classes which are
     * produced by multiple calls to performFlowAnalysis.
     */
    protected AnalysisResult<Value, Store> flowResult;

    /**
     * A mapping from methods (or other code blocks) to their regular exit store (used to check
     * postconditions).
     */
    protected IdentityHashMap<Tree, Store> regularExitStores;

    /** A mapping from methods to a list with all return statements and the corresponding store. */
    protected IdentityHashMap<MethodTree, List<Pair<ReturnNode, TransferResult<Value, Store>>>>
            returnStatementStores;

    /**
     * A mapping from methods to their a list with all return statements and the corresponding
     * store.
     */
    protected IdentityHashMap<MethodInvocationTree, Store> methodInvocationStores;

    /**
     * Returns the regular exit store for a method or another code block (such as static
     * initializers).
     *
     * @return the regular exit store, or {@code null}, if there is no such store (because the
     *     method cannot exit through the regular exit block).
     */
    public @Nullable Store getRegularExitStore(Tree t) {
        return regularExitStores.get(t);
    }

    /** @return all return node and store pairs for a given method */
    public List<Pair<ReturnNode, TransferResult<Value, Store>>> getReturnStatementStores(
            MethodTree methodTree) {
        assert returnStatementStores.containsKey(methodTree);
        return returnStatementStores.get(methodTree);
    }

    /** @return the store immediately before a given {@link Tree}. */
    public Store getStoreBefore(Tree tree) {
        if (!analysis.isRunning()) {
            return flowResult.getStoreBefore(tree);
        }
        Set<Node> nodes = analysis.getNodesForTree(tree);
        if (nodes != null) {
            return getStoreBefore(nodes);
        } else {
            return flowResult.getStoreBefore(tree);
        }
    }

    /** @return the store immediately before a given Set of {@link Node}s. */
    public Store getStoreBefore(Set<Node> nodes) {
        Store merge = null;
        for (Node aNode : nodes) {
            Store s = getStoreBefore(aNode);
            if (merge == null) {
                merge = s;
            } else if (s != null) {
                merge = merge.leastUpperBound(s);
            }
        }
        return merge;
    }

    /** @return the store immediately before a given {@link Node}. */
    public Store getStoreBefore(Node node) {
        if (!analysis.isRunning()) {
            return flowResult.getStoreBefore(node);
        }
        TransferInput<Value, Store> prevStore = analysis.getInput(node.getBlock());
        if (prevStore == null) {
            return null;
        }
        Store store =
                AnalysisResult.runAnalysisFor(
                        node, true, prevStore, analysis.getNodeValues(), flowResultAnalysisCaches);
        return store;
    }

    /** @return the store immediately after a given {@link Tree}. */
    public Store getStoreAfter(Tree tree) {
        if (!analysis.isRunning()) {
            return flowResult.getStoreAfter(tree);
        }
        Set<Node> nodes = analysis.getNodesForTree(tree);
        return getStoreAfter(nodes);
    }

    /** @return the store immediately after a given set of {@link Node}s. */
    public Store getStoreAfter(Set<Node> nodes) {
        Store merge = null;
        for (Node node : nodes) {
            Store s = getStoreAfter(node);
            if (merge == null) {
                merge = s;
            } else if (s != null) {
                merge = merge.leastUpperBound(s);
            }
        }
        return merge;
    }

    /** @return the store immediately after a given {@link Node}. */
    public Store getStoreAfter(Node node) {
        Store res =
                AnalysisResult.runAnalysisFor(
                        node,
                        false,
                        analysis.getInput(node.getBlock()),
                        analysis.getNodeValues(),
                        flowResultAnalysisCaches);
        return res;
    }

    /**
     * @see org.checkerframework.dataflow.analysis.AnalysisResult#getNodesForTree(Tree)
     * @return the {@link Node}s for a given {@link Tree}
     */
    public Set<Node> getNodesForTree(Tree tree) {
        return flowResult.getNodesForTree(tree);
    }

    /**
     * Return the first {@link Node} for a given {@link Tree} that has class {@code kind}.
     *
     * <p>You probably don't want to use this function: iterate over the result of {@link
     * #getNodesForTree(Tree)} yourself or ask for a conservative approximation of the store using
     * {@link #getStoreBefore(Tree)} or {@link #getStoreAfter(Tree)}. This method is for code that
     * uses a {@link Node} in a rather unusual way. Callers should probably be rewritten to not use
     * a {@link Node} at all.
     *
     * @see #getNodesForTree(Tree)
     * @see #getStoreBefore(Tree)
     * @see #getStoreAfter(Tree)
     * @return the first {@link Node} for a given {@link Tree} that of class {@code kind}.
     */
    public <T extends Node> T getFirstNodeOfKindForTree(Tree tree, Class<T> kind) {
        Set<Node> nodes = getNodesForTree(tree);
        for (Node node : nodes) {
            if (node.getClass().equals(kind)) {
                return kind.cast(node);
            }
        }
        return null;
    }

    /** @return the value of effectively final local variables */
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
            flowResult = new AnalysisResult<>(flowResultAnalysisCaches);
        }

        // no need to scan annotations
        if (classTree.getKind() == Kind.ANNOTATION_TYPE) {
            // Mark finished so that default annotations will be applied.
            scannedClasses.put(classTree, ScanState.FINISHED);
            return;
        }

        Queue<Pair<ClassTree, Store>> queue = new ArrayDeque<>();
        List<Pair<VariableElement, Value>> fieldValues = new ArrayList<>();

        // No captured store for top-level classes.
        queue.add(Pair.of(classTree, null));

        while (!queue.isEmpty()) {
            final Pair<ClassTree, Store> qel = queue.remove();
            final ClassTree ct = qel.first;
            final Store capturedStore = qel.second;
            scannedClasses.put(ct, ScanState.IN_PROGRESS);

            TreePath preTreePath = visitorState.getPath();
            AnnotatedDeclaredType preClassType = visitorState.getClassType();
            ClassTree preClassTree = visitorState.getClassTree();
            AnnotatedDeclaredType preAMT = visitorState.getMethodReceiver();
            MethodTree preMT = visitorState.getMethodTree();

            // Don't use getPath, b/c that depends on the visitorState path.
            visitorState.setPath(TreePath.getPath(this.root, ct));
            visitorState.setClassType(getAnnotatedType(TreeUtils.elementFromDeclaration(ct)));
            visitorState.setClassTree(ct);
            visitorState.setMethodReceiver(null);
            visitorState.setMethodTree(null);

            // start with the captured store as initialization store
            initializationStaticStore = capturedStore;
            initializationStore = capturedStore;

            Queue<Pair<LambdaExpressionTree, Store>> lambdaQueue = new ArrayDeque<>();

            try {
                List<CFGMethod> methods = new ArrayList<>();
                for (Tree m : ct.getMembers()) {
                    switch (m.getKind()) {
                        case METHOD:
                            MethodTree mt = (MethodTree) m;

                            // Skip abstract and native methods because they have no body.
                            Set<Modifier> flags = mt.getModifiers().getFlags();
                            if (flags.contains(Modifier.ABSTRACT)
                                    || flags.contains(Modifier.NATIVE)) {
                                break;
                            }
                            // Abstract methods in an interface have a null body but do not have an
                            // ABSTRACT flag.
                            if (mt.getBody() == null) {
                                break;
                            }

                            // Wait with scanning the method until all other members
                            // have been processed.
                            CFGMethod met = new CFGMethod(mt, ct);
                            methods.add(met);
                            break;
                        case VARIABLE:
                            VariableTree vt = (VariableTree) m;
                            ExpressionTree initializer = vt.getInitializer();
                            // analyze initializer if present
                            if (initializer != null) {
                                boolean isStatic =
                                        vt.getModifiers().getFlags().contains(Modifier.STATIC);
                                analyze(
                                        queue,
                                        lambdaQueue,
                                        new CFGStatement(vt, ct),
                                        fieldValues,
                                        classTree,
                                        true,
                                        true,
                                        isStatic,
                                        capturedStore);
                                Value value = flowResult.getValue(initializer);
                                if (vt.getModifiers().getFlags().contains(Modifier.FINAL)
                                        && value != null) {
                                    // Store the abstract value for the field.
                                    VariableElement element = TreeUtils.elementFromDeclaration(vt);
                                    fieldValues.add(Pair.of(element, value));
                                }
                            }
                            break;
                        case CLASS:
                        case ANNOTATION_TYPE:
                        case INTERFACE:
                        case ENUM:
                            // Visit inner and nested class trees.
                            // TODO: Use no store for them? What can be captured?
                            queue.add(Pair.of((ClassTree) m, capturedStore));
                            break;
                        case BLOCK:
                            BlockTree b = (BlockTree) m;
                            analyze(
                                    queue,
                                    lambdaQueue,
                                    new CFGStatement(b, ct),
                                    fieldValues,
                                    ct,
                                    true,
                                    true,
                                    b.isStatic(),
                                    capturedStore);
                            break;
                        default:
                            assert false : "Unexpected member: " + m.getKind();
                            break;
                    }
                }

                // Now analyze all methods.
                // TODO: at this point, we don't have any information about
                // fields of superclasses.
                for (CFGMethod met : methods) {
                    analyze(
                            queue,
                            lambdaQueue,
                            met,
                            fieldValues,
                            classTree,
                            TreeUtils.isConstructor(met.getMethod()),
                            false,
                            false,
                            capturedStore);
                }

                while (!lambdaQueue.isEmpty()) {
                    Pair<LambdaExpressionTree, Store> lambdaPair = lambdaQueue.poll();
                    analyze(
                            queue,
                            lambdaQueue,
                            new CFGLambda(lambdaPair.first),
                            fieldValues,
                            classTree,
                            false,
                            false,
                            false,
                            lambdaPair.second);
                }

                // by convention we store the static initialization store as the regular exit
                // store of the class node, so that it can later be used to check
                // that all fields are initialized properly.
                // see InitializationVisitor.visitClass
                if (initializationStaticStore == null) {
                    regularExitStores.put(ct, emptyStore);
                } else {
                    regularExitStores.put(ct, initializationStaticStore);
                }
            } finally {
                visitorState.setPath(preTreePath);
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
     * @param queue the queue to add more things to scan
     * @param fieldValues the abstract values for all fields of the same class
     * @param ast the AST to analyze
     * @param currentClass the class we are currently looking at
     * @param isInitializationCode are we analyzing a (non-static) initializer block of a class
     * @param capturedStore the input Store to use for captured variables, e.g. in a lambda
     */
    protected void analyze(
            Queue<Pair<ClassTree, Store>> queue,
            Queue<Pair<LambdaExpressionTree, Store>> lambdaQueue,
            UnderlyingAST ast,
            List<Pair<VariableElement, Value>> fieldValues,
            ClassTree currentClass,
            boolean isInitializationCode,
            boolean updateInitializationStore,
            boolean isStatic,
            Store capturedStore) {
        ControlFlowGraph cfg = CFCFGBuilder.build(root, ast, checker, this, processingEnv);

        if (isInitializationCode) {
            Store initStore = !isStatic ? initializationStore : initializationStaticStore;
            if (initStore != null) {
                // we have already seen initialization code and analyzed it, and
                // the analysis ended with the store initStore.
                // use it to start the next analysis.
                transfer.setFixedInitialStore(initStore);
            } else {
                transfer.setFixedInitialStore(capturedStore);
            }
        } else {
            transfer.setFixedInitialStore(capturedStore);
        }
        analysis.performAnalysis(cfg, fieldValues);
        AnalysisResult<Value, Store> result = analysis.getResult();

        // store result
        flowResult.combine(result);
        if (ast.getKind() == UnderlyingAST.Kind.METHOD) {
            // store exit store (for checking postconditions)
            CFGMethod mast = (CFGMethod) ast;
            MethodTree method = mast.getMethod();
            Store regularExitStore = analysis.getRegularExitStore();
            if (regularExitStore != null) {
                regularExitStores.put(method, regularExitStore);
            }
            returnStatementStores.put(method, analysis.getReturnStatementStores());
        } else if (ast.getKind() == UnderlyingAST.Kind.ARBITRARY_CODE) {
            CFGStatement block = (CFGStatement) ast;
            Store regularExitStore = analysis.getRegularExitStore();
            if (regularExitStore != null) {
                regularExitStores.put(block.getCode(), regularExitStore);
            }
        } else if (ast.getKind() == UnderlyingAST.Kind.LAMBDA) {
            // TODO: Postconditions?

            CFGLambda block = (CFGLambda) ast;
            Store regularExitStore = analysis.getRegularExitStore();
            if (regularExitStore != null) {
                regularExitStores.put(block.getCode(), regularExitStore);
            }
        } else {
            assert false : "Unexpected AST kind: " + ast.getKind();
        }

        if (isInitializationCode && updateInitializationStore) {
            Store newInitStore = analysis.getRegularExitStore();
            if (!isStatic) {
                initializationStore = newInitStore;
            } else {
                initializationStaticStore = newInitStore;
            }
        }

        if (checker.hasOption("flowdotdir") || checker.hasOption("cfgviz")) {
            handleCFGViz();
        }

        // add classes declared in method
        for (ClassTree cls : cfg.getDeclaredClasses()) {
            queue.add(Pair.of(cls, getStoreBefore(cls)));
        }
        for (LambdaExpressionTree lambda : cfg.getDeclaredLambdas()) {
            lambdaQueue.add(Pair.of(lambda, getStoreBefore(lambda)));
        }
    }

    /**
     * Handle the visualization of the CFG, by calling {@code visualizeCFG} on the analysis. This
     * method gets invoked in {@code analyze} if one of the visualization options is provided.
     */
    protected void handleCFGViz() {
        analysis.visualizeCFG();
    }

    /**
     * Returns the type of the left-hand side of an assignment without applying local variable
     * defaults to type variables.
     *
     * <p>The type variables that are types of local variables are defaulted to top so that they can
     * be refined by dataflow. When these types are used as context during type argument inference,
     * this default is too conservative. So this method is used instead of {@link
     * GenericAnnotatedTypeFactory#getAnnotatedTypeLhs(Tree)}.
     *
     * <p>{@link TypeArgInferenceUtil#assignedToVariable(AnnotatedTypeFactory, Tree)} explains why a
     * different type is used.
     *
     * @param lhsTree left-hand side of an assignment
     * @return AnnotatedTypeMirror of {@code lhsTree}
     */
    public AnnotatedTypeMirror getAnnotatedTypeLhsNoTypeVarDefault(Tree lhsTree) {
        boolean old = this.shouldDefaultTypeVarLocals;
        shouldDefaultTypeVarLocals = false;
        AnnotatedTypeMirror type = getAnnotatedTypeLhs(lhsTree);
        this.shouldDefaultTypeVarLocals = old;
        return type;
    }

    /**
     * Returns the type of a left-hand side of an assignment.
     *
     * <p>The default implementation returns the type without considering dataflow type refinement.
     * Subclass can override this method and add additional logic for computing the type of a LHS.
     *
     * @param lhsTree left-hand side of an assignment
     * @return AnnotatedTypeMirror of {@code lhsTree}
     */
    public AnnotatedTypeMirror getAnnotatedTypeLhs(Tree lhsTree) {
        AnnotatedTypeMirror res = null;
        boolean oldUseFlow = useFlow;
        boolean oldShouldCache = shouldCache;
        useFlow = false;
        // Don't cache the result because getAnnotatedType(lhsTree) could
        // be called from elsewhere and would expect flow-sensitive type refinements.
        shouldCache = false;
        switch (lhsTree.getKind()) {
            case VARIABLE:
            case IDENTIFIER:
            case MEMBER_SELECT:
            case ARRAY_ACCESS:
                res = getAnnotatedType(lhsTree);
                break;
            default:
                if (TreeUtils.isTypeTree(lhsTree)) {
                    // lhsTree is a type tree at the pseudo assignment of a returned expression to
                    // declared return type.
                    res = getAnnotatedType(lhsTree);
                } else {
                    throw new BugInCF(
                            "GenericAnnotatedTypeFactory: Unexpected tree passed to getAnnotatedTypeLhs. "
                                    + "lhsTree: "
                                    + lhsTree
                                    + " Tree.Kind: "
                                    + lhsTree.getKind());
                }
        }
        useFlow = oldUseFlow;
        shouldCache = oldShouldCache;
        return res;
    }

    /**
     * Returns the type of a varargs array of a method invocation or a constructor invocation.
     *
     * @param tree a method invocation or a constructor invocation
     * @return AnnotatedTypeMirror of varargs array for a method or constructor invocation {@code
     *     tree}
     */
    public AnnotatedTypeMirror getAnnotatedTypeVarargsArray(Tree tree) {
        if (!useFlow) {
            return null;
        }

        List<Node> args;
        switch (tree.getKind()) {
            case METHOD_INVOCATION:
                args = getFirstNodeOfKindForTree(tree, MethodInvocationNode.class).getArguments();
                break;
            case NEW_CLASS:
                args = getFirstNodeOfKindForTree(tree, ObjectCreationNode.class).getArguments();
                break;
            default:
                throw new BugInCF("Unexpected kind of tree: " + tree);
        }

        assert !args.isEmpty() : "Arguments are empty";
        Node varargsArray = args.get(args.size() - 1);
        AnnotatedTypeMirror varargtype = getAnnotatedType(varargsArray.getTree());
        return varargtype;
    }

    /* Returns the type of a right-hand side of an assignment for unary operation like prefix or
     * postfix increment or decrement.
     *
     * @param tree unary operation tree for compound assignment
     * @return AnnotatedTypeMirror of a right-hand side of an assignment for unary operation
     */
    public AnnotatedTypeMirror getAnnotatedTypeRhsUnaryAssign(UnaryTree tree) {
        if (!useFlow) {
            return getAnnotatedType(tree);
        }
        AssignmentNode n = flowResult.getAssignForUnaryTree(tree);
        return getAnnotatedType(n.getExpression().getTree());
    }

    @Override
    public ParameterizedExecutableType constructorFromUse(NewClassTree tree) {
        ParameterizedExecutableType mType = super.constructorFromUse(tree);
        AnnotatedExecutableType method = mType.executableType;
        if (dependentTypesHelper != null) {
            dependentTypesHelper.viewpointAdaptConstructor(tree, method);
        }
        poly.resolve(tree, method);
        return mType;
    }

    @Override
    public AnnotatedTypeMirror getMethodReturnType(MethodTree m) {
        AnnotatedTypeMirror returnType = super.getMethodReturnType(m);
        if (dependentTypesHelper != null) {
            dependentTypesHelper.standardizeReturnType(m, returnType);
        }
        return returnType;
    }

    @Override
    public AnnotatedTypeMirror getMethodReturnType(MethodTree m, ReturnTree r) {
        AnnotatedTypeMirror returnType = super.getMethodReturnType(m, r);
        if (dependentTypesHelper != null) {
            dependentTypesHelper.standardizeReturnType(m, returnType);
        }
        return returnType;
    }

    @Override
    public void addDefaultAnnotations(AnnotatedTypeMirror type) {
        addAnnotationsFromDefaultQualifierForUse(null, type);
        typeAnnotator.visit(type, null);
        defaults.annotate((Element) null, type);
    }

    /**
     * This method is final; override {@link #addComputedTypeAnnotations(Tree, AnnotatedTypeMirror,
     * boolean)} instead.
     *
     * <p>{@inheritDoc}
     */
    @Override
    protected final void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type) {
        addComputedTypeAnnotations(tree, type, this.useFlow);
    }

    /**
     * Like {#addComputedTypeAnnotations(Tree, AnnotatedTypeMirror)}. Overriding implementations
     * typically simply pass the boolean to calls to super.
     */
    protected void addComputedTypeAnnotations(
            Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
        assert root != null
                : "GenericAnnotatedTypeFactory.addComputedTypeAnnotations: "
                        + " root needs to be set when used on trees; factory: "
                        + this.getClass();

        addAnnotationsFromDefaultQualifierForUse(TreeUtils.elementFromTree(tree), type);
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
     * Flow analysis will be performed if all of the following are true.
     *
     * <ul>
     *   <li>tree is a {@link ClassTree}
     *   <li>Flow analysis has not already been performed on tree
     * </ul>
     *
     * @param tree the tree to check and possibly perform flow analysis on
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
            throw new BugInCF(
                    "GenericAnnotatedTypeFactory.getInferredValueFor called with null tree");
        }
        Value as = null;
        if (analysis.isRunning()) {
            as = analysis.getValue(tree);
        }
        if (as == null
                &&
                // TODO: this comparison shouldn't be needed, but
                // checker-framework-inference fails without it.
                flowResult != null) {
            as = flowResult.getValue(tree);
        }
        return as;
    }

    /**
     * Applies the annotations inferred by the org.checkerframework.dataflow analysis to the type
     * {@code type}.
     */
    protected void applyInferredAnnotations(AnnotatedTypeMirror type, Value as) {
        DefaultInferredTypesApplier applier =
                new DefaultInferredTypesApplier(getQualifierHierarchy(), this);
        applier.applyInferredType(type, as.getAnnotations(), as.getUnderlyingType());
    }

    @Override
    public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
        addAnnotationsFromDefaultQualifierForUse(elt, type);
        typeAnnotator.visit(type, null);
        defaults.annotate(elt, type);
        if (dependentTypesHelper != null) {
            dependentTypesHelper.standardizeVariable(type, elt);
        }
    }

    @Override
    public ParameterizedExecutableType methodFromUse(MethodInvocationTree tree) {
        ParameterizedExecutableType mType = super.methodFromUse(tree);
        AnnotatedExecutableType method = mType.executableType;
        if (dependentTypesHelper != null) {
            dependentTypesHelper.viewpointAdaptMethod(tree, method);
        }
        poly.resolve(tree, method);
        return mType;
    }

    @Override
    public List<AnnotatedTypeParameterBounds> typeVariablesFromUse(
            AnnotatedDeclaredType type, TypeElement element) {
        List<AnnotatedTypeParameterBounds> f = super.typeVariablesFromUse(type, element);
        if (dependentTypesHelper != null) {
            dependentTypesHelper.viewpointAdaptTypeVariableBounds(
                    element, f, visitorState.getPath());
        }
        return f;
    }

    public Store getEmptyStore() {
        return emptyStore;
    }

    /**
     * Returns the AnnotatedTypeFactory of the subchecker and copies the current visitor state to
     * the sub-factory so that the types are computed properly. Because the visitor state is copied,
     * call this method each time a subfactory is needed rather than store the returned subfactory
     * in a field.
     *
     * @see BaseTypeChecker#getTypeFactoryOfSubchecker(Class)
     */
    @SuppressWarnings("TypeParameterUnusedInFormals") // Intentional abuse
    public <T extends GenericAnnotatedTypeFactory<?, ?, ?, ?>, U extends BaseTypeChecker>
            T getTypeFactoryOfSubchecker(Class<U> checkerClass) {
        T subFactory = checker.getTypeFactoryOfSubchecker(checkerClass);
        if (subFactory != null && subFactory.getVisitorState() != null) {
            // Copy the visitor state so that the types are computed properly.
            VisitorState subFactoryVisitorState = subFactory.getVisitorState();
            subFactoryVisitorState.setPath(visitorState.getPath());
            subFactoryVisitorState.setClassTree(visitorState.getClassTree());
            subFactoryVisitorState.setClassType(visitorState.getClassType());
            subFactoryVisitorState.setMethodTree(visitorState.getMethodTree());
            subFactoryVisitorState.setMethodReceiver(visitorState.getMethodReceiver());
        }
        return subFactory;
    }

    /**
     * Should the local variable default annotation be applied to type variables?
     *
     * <p>It is initialized to true if data flow is used by the checker. It is set to false when
     * getting the assignment context for type argument inference.
     *
     * @see GenericAnnotatedTypeFactory#getAnnotatedTypeLhsNoTypeVarDefault
     * @return shouldDefaultTypeVarLocals
     */
    public boolean getShouldDefaultTypeVarLocals() {
        return shouldDefaultTypeVarLocals;
    }

    /** The CFGVisualizer to be used by all CFAbstractAnalysis instances. */
    protected final CFGVisualizer<Value, Store, TransferFunction> cfgVisualizer;

    protected CFGVisualizer<Value, Store, TransferFunction> createCFGVisualizer() {
        if (checker.hasOption("flowdotdir")) {
            String flowdotdir = checker.getOption("flowdotdir");
            boolean verbose = checker.hasOption("verbosecfg");

            Map<String, Object> args = new HashMap<>(2);
            args.put("outdir", flowdotdir);
            args.put("verbose", verbose);
            args.put("checkerName", getCheckerName());

            CFGVisualizer<Value, Store, TransferFunction> res = new DOTCFGVisualizer<>();
            res.init(args);
            return res;
        } else if (checker.hasOption("cfgviz")) {
            String cfgviz = checker.getOption("cfgviz");
            if (cfgviz == null) {
                throw new UserError(
                        "-Acfgviz specified without arguments, should be -Acfgviz=VizClassName[,opts,...]");
            }
            String[] opts = cfgviz.split(",");

            Map<String, Object> args = processCFGVisualizerOption(opts);
            if (!args.containsKey("verbose")) {
                boolean verbose = checker.hasOption("verbosecfg");
                args.put("verbose", verbose);
            }
            args.put("checkerName", getCheckerName());

            CFGVisualizer<Value, Store, TransferFunction> res =
                    BaseTypeChecker.invokeConstructorFor(opts[0], null, null);
            res.init(args);
            return res;
        }
        // Nobody expected to use cfgVisualizer if neither option given.
        return null;
    }

    /* A simple utility method to determine a short checker name to be
     * used by CFG visualizations.
     */
    private String getCheckerName() {
        String checkerName = checker.getClass().getSimpleName();
        if (checkerName.endsWith("Checker") || checkerName.endsWith("checker")) {
            checkerName = checkerName.substring(0, checkerName.length() - "checker".length());
        }
        return checkerName;
    }

    /* Parse values or key-value pairs into a map from value to true, respectively,
     * from the value to the key.
     */
    private Map<String, Object> processCFGVisualizerOption(String[] opts) {
        Map<String, Object> res = new HashMap<>(opts.length - 1);
        // Index 0 is the visualizer class name and can be ignored.
        for (int i = 1; i < opts.length; ++i) {
            String opt = opts[i];
            String[] split = opt.split("=");
            switch (split.length) {
                case 1:
                    res.put(split[0], true);
                    break;
                case 2:
                    res.put(split[0], split[1]);
                    break;
                default:
                    throw new UserError("Too many `=` in cfgviz option: " + opt);
            }
        }
        return res;
    }

    /** The CFGVisualizer to be used by all CFAbstractAnalysis instances. */
    public CFGVisualizer<Value, Store, TransferFunction> getCFGVisualizer() {
        return cfgVisualizer;
    }

    /**
     * Adds default qualifiers bases on the underlying type of {@code type} to {@code type}. If
     * {@code element} is a local variable, then the defaults are not added.
     *
     * @param element possibly null element whose type is {@code type}
     * @param type the type to which defaults are added
     */
    protected void addAnnotationsFromDefaultQualifierForUse(
            @Nullable Element element, AnnotatedTypeMirror type) {
        if (element != null
                && element.getKind() == ElementKind.LOCAL_VARIABLE
                && type.getKind() == TypeKind.DECLARED) {
            // If this is a type for a local variable, don't apply the default to the primary
            // location.
            AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) type;
            if (declaredType.getEnclosingType() != null) {
                defaultQualifierForUseTypeAnnotator.visit(declaredType.getEnclosingType());
            }
            for (AnnotatedTypeMirror typeArg : declaredType.getTypeArguments()) {
                defaultQualifierForUseTypeAnnotator.visit(typeArg);
            }
        } else {
            defaultQualifierForUseTypeAnnotator.visit(type);
        }
    }
}
