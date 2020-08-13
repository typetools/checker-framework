package org.checkerframework.framework.type;

// The imports from com.sun are all @jdk.Exported and therefore somewhat safe to use.
// Try to avoid using non-@jdk.Exported classes.

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Type;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.reflection.DefaultReflectionResolver;
import org.checkerframework.common.reflection.MethodValAnnotatedTypeFactory;
import org.checkerframework.common.reflection.MethodValChecker;
import org.checkerframework.common.reflection.ReflectionResolver;
import org.checkerframework.common.wholeprograminference.WholeProgramInference;
import org.checkerframework.common.wholeprograminference.WholeProgramInferenceScenes;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.FieldInvariant;
import org.checkerframework.framework.qual.FromStubFile;
import org.checkerframework.framework.qual.HasQualifierParameter;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.NoQualifierParameter;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.stub.StubTypes;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeCombiner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.AnnotationFormatter;
import org.checkerframework.framework.util.CFContext;
import org.checkerframework.framework.util.DefaultAnnotationFormatter;
import org.checkerframework.framework.util.FieldInvariants;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.TreePathCacher;
import org.checkerframework.framework.util.typeinference.DefaultTypeArgumentInference;
import org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil;
import org.checkerframework.framework.util.typeinference.TypeArgumentInference;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.CollectionUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.javacutil.UserError;
import org.checkerframework.javacutil.trees.DetachedVarSymbol;

/**
 * The methods of this class take an element or AST node, and return the annotated type as an {@link
 * AnnotatedTypeMirror}. The methods are:
 *
 * <ul>
 *   <li>{@link #getAnnotatedType(ClassTree)}
 *   <li>{@link #getAnnotatedType(MethodTree)}
 *   <li>{@link #getAnnotatedType(Tree)}
 *   <li>{@link #getAnnotatedTypeFromTypeTree(Tree)}
 *   <li>{@link #getAnnotatedType(TypeElement)}
 *   <li>{@link #getAnnotatedType(ExecutableElement)}
 *   <li>{@link #getAnnotatedType(Element)}
 * </ul>
 *
 * This implementation only adds qualifiers explicitly specified by the programmer. Subclasses
 * override {@link #addComputedTypeAnnotations} to add defaults, flow-sensitive refinement, and
 * type-system-specific rules.
 *
 * <p>Unless otherwise indicated, each public method in this class returns a "fully annotated" type,
 * which is one that has an annotation in all positions.
 *
 * <p>Type system checker writers may need to subclass this class, to add default qualifiers
 * according to the type system semantics. Subclasses should especially override {@link
 * #addComputedTypeAnnotations(Element, AnnotatedTypeMirror)} and {@link
 * #addComputedTypeAnnotations(Tree, AnnotatedTypeMirror)} to handle default annotations. (Also,
 * {@link #addDefaultAnnotations(AnnotatedTypeMirror)} adds annotations, but that method is a
 * workaround for <a href="https://github.com/typetools/checker-framework/issues/979">Issue
 * 979</a>.)
 *
 * @checker_framework.manual #creating-a-checker How to write a checker plug-in
 */
public class AnnotatedTypeFactory implements AnnotationProvider {

    /** The {@link Trees} instance to use for tree node path finding. */
    protected final Trees trees;

    /** Optional! The AST of the source file being operated on. */
    // TODO: when should root be null? What are the use cases?
    // None of the existing test checkers has a null root.
    // Should not be modified between calls to "visit".
    protected @Nullable CompilationUnitTree root;

    /** The processing environment to use for accessing compiler internals. */
    protected final ProcessingEnvironment processingEnv;

    /** Utility class for working with {@link Element}s. */
    protected final Elements elements;

    /** Utility class for working with {@link TypeMirror}s. */
    public final Types types;

    /** The state of the visitor. */
    protected final VisitorState visitorState;

    /**
     * ===== postInit initialized fields ==== Note: qualHierarchy and typeHierarchy are both
     * initialized in the postInit.
     *
     * @see #postInit() This means, they cannot be final and cannot be referred to in any subclass
     *     constructor or method until after postInit is called
     */

    /** Represent the annotation relations. */
    protected QualifierHierarchy qualHierarchy;

    /** Represent the type relations. */
    protected TypeHierarchy typeHierarchy;

    /** Performs whole-program inference. If null, whole-program inference is disabled. */
    private final @Nullable WholeProgramInference wholeProgramInference;

    /**
     * This formatter is used for converting AnnotatedTypeMirrors to Strings. This formatter will be
     * used by all AnnotatedTypeMirrors created by this factory in their toString methods.
     */
    protected final AnnotatedTypeFormatter typeFormatter;

    /**
     * Annotation formatter is used to format AnnotationMirrors. It is primarily used by
     * SourceChecker when generating error messages.
     */
    private final AnnotationFormatter annotationFormatter;

    /** Holds the qualifier upper bounds for type uses. */
    protected QualifierUpperBounds qualifierUpperBounds;

    /**
     * Provides utility method to substitute arguments for their type variables. Field should be
     * final, but can only be set in postInit, because subtypes might need other state to be
     * initialized first.
     */
    protected TypeVariableSubstitutor typeVarSubstitutor;

    /** Provides utility method to infer type arguments. */
    protected TypeArgumentInference typeArgumentInference;

    /**
     * Caches the supported type qualifier classes. Call {@link #getSupportedTypeQualifiers()}
     * instead of using this field directly, as it may not have been initialized.
     */
    private final Set<Class<? extends Annotation>> supportedQuals;

    /**
     * Caches the fully-qualified names of the classes in {@link #supportedQuals}. Call {@link
     * #getSupportedTypeQualifierNames()} instead of using this field directly, as it may not have
     * been initialized.
     */
    private final Set<String> supportedQualNames;

    /** Parses stub files and stores annotations from stub files. */
    public final StubTypes stubTypes;

    /**
     * A cache used to store elements whose declaration annotations have already been stored by
     * calling the method {@link #getDeclAnnotations(Element)}.
     */
    private final Map<Element, Set<AnnotationMirror>> cacheDeclAnnos;

    /**
     * A set containing declaration annotations that should be inherited. A declaration annotation
     * will be inherited if it is in this set, or if it has the
     * meta-annotation @InheritedAnnotation.
     */
    private final Set<AnnotationMirror> inheritedAnnotations =
            AnnotationUtils.createAnnotationSet();

    /** The checker to use for option handling and resource management. */
    protected final BaseTypeChecker checker;

    /** Map keys are canonical names of aliased annotations. */
    private final Map<String, Alias> aliases = new HashMap<>();

    /**
     * Information about one annotation alias.
     *
     * <p>The information is either an AnotationMirror that can be used directly, or information for
     * a builder (name and fields not to copy); see checkRep.
     */
    private static class Alias {
        /** The canonical annotation (or null if copyElements == true). */
        AnnotationMirror canonical;
        /** Whether elements should be copied over when translating to the canonical annotation. */
        boolean copyElements;
        /** The canonical annotation name (or null if copyElements == false). */
        String canonicalName;
        /** Which elements should not be copied over (or null if copyElements == false). */
        String[] ignorableElements;

        /**
         * Create an Alias with the given components.
         *
         * @param aliasName the alias name; only used for debugging
         * @param canonical the canonical annotation
         * @param copyElements whether elements should be copied over when translating to the
         *     canonical annotation
         * @param ignorableElements elements that should not be copied over
         */
        Alias(
                String aliasName,
                AnnotationMirror canonical,
                boolean copyElements,
                String canonicalName,
                String[] ignorableElements) {
            this.canonical = canonical;
            this.copyElements = copyElements;
            this.canonicalName = canonicalName;
            this.ignorableElements = ignorableElements;
            checkRep(aliasName);
        }

        /**
         * Throw an exception if this object is malformed.
         *
         * @param aliasName the alias name; only used for diagnostic messages
         */
        void checkRep(String aliasName) {
            if (copyElements) {
                if (!(canonical == null && canonicalName != null && ignorableElements != null)) {
                    throw new BugInCF(
                            "Bad Alias for %s: [canonical=%s] copyElements=%s canonicalName=%s ignorableElements=%s",
                            aliasName, canonical, copyElements, canonicalName, ignorableElements);
                }
            } else {
                if (!(canonical != null && canonicalName == null && ignorableElements == null)) {
                    throw new BugInCF(
                            "Bad Alias for %s: canonical=%s copyElements=%s [canonicalName=%s ignorableElements=%s]",
                            aliasName, canonical, copyElements, canonicalName, ignorableElements);
                }
            }
        }
    }

    /**
     * A map from the class of an annotation to the set of classes for annotations with the same
     * meaning, as well as the annotation mirror that should be used.
     */
    private final Map<
                    Class<? extends Annotation>,
                    Pair<AnnotationMirror, Set<Class<? extends Annotation>>>>
            declAliases = new HashMap<>();

    /** Unique ID counter; for debugging purposes. */
    private static int uidCounter = 0;

    /** Unique ID of the current object; for debugging purposes. */
    public final int uid;

    /**
     * Object that is used to resolve reflective method calls, if reflection resolution is turned
     * on.
     */
    protected ReflectionResolver reflectionResolver;

    /** AnnotationClassLoader used to load type annotation classes via reflective lookup. */
    protected AnnotationClassLoader loader;

    /**
     * Which whole-program inference output format to use, if doing whole-program inference. This
     * variable would be final, but it is not set unless WPI is enabled.
     */
    private WholeProgramInference.OutputFormat wpiOutputFormat;

    /**
     * Should results be cached? This means that ATM.deepCopy() will be called. ATM.deepCopy() used
     * to (and perhaps still does) side effect the ATM being copied. So setting this to false is not
     * equivalent to setting shouldReadCache to false.
     */
    public boolean shouldCache;

    /** Size of LRU cache if one isn't specified using the atfCacheSize option. */
    private static final int DEFAULT_CACHE_SIZE = 300;

    /** Mapping from a Tree to its annotated type; defaults have been applied. */
    private final Map<Tree, AnnotatedTypeMirror> classAndMethodTreeCache;

    /**
     * Mapping from an expression tree to its annotated type; before defaults are applied, just what
     * the programmer wrote.
     */
    protected final Map<Tree, AnnotatedTypeMirror> fromExpressionTreeCache;

    /**
     * Mapping from a member tree to its annotated type; before defaults are applied, just what the
     * programmer wrote.
     */
    protected final Map<Tree, AnnotatedTypeMirror> fromMemberTreeCache;

    /**
     * Mapping from a type tree to its annotated type; before defaults are applied, just what the
     * programmer wrote.
     */
    protected final Map<Tree, AnnotatedTypeMirror> fromTypeTreeCache;

    /**
     * Mapping from an Element to its annotated type; before defaults are applied, just what the
     * programmer wrote.
     */
    private final Map<Element, AnnotatedTypeMirror> elementCache;

    /** Mapping from an Element to the source Tree of the declaration. */
    private final Map<Element, Tree> elementToTreeCache;

    /** Mapping from a Tree to its TreePath. Shared between all instances. */
    private final TreePathCacher treePathCache;

    /** Mapping from CFG generated trees to their enclosing elements. */
    private final Map<Tree, Element> artificialTreeToEnclosingElementMap;

    /**
     * Whether to ignore uninferred type arguments. This is a temporary flag to work around Issue
     * 979.
     */
    public final boolean ignoreUninferredTypeArguments;

    /** The Object.getClass method. */
    protected final ExecutableElement objectGetClass;

    /** Size of the annotationClassNames cache. */
    private static final int ANNOTATION_CACHE_SIZE = 500;

    /** Maps classes representing AnnotationMirrors to their canonical names. */
    private final Map<Class<? extends Annotation>, String> annotationClassNames;

    /**
     * Constructs a factory from the given {@link ProcessingEnvironment} instance and syntax tree
     * root. (These parameters are required so that the factory may conduct the appropriate
     * annotation-gathering analyses on certain tree types.)
     *
     * <p>Root can be {@code null} if the factory does not operate on trees.
     *
     * <p>A subclass must call postInit at the end of its constructor. postInit must be the last
     * call in the constructor or else types from stub files may not be created as expected.
     *
     * @param checker the {@link SourceChecker} to which this factory belongs
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public AnnotatedTypeFactory(BaseTypeChecker checker) {
        uid = ++uidCounter;
        this.processingEnv = checker.getProcessingEnvironment();
        // this.root = root;
        this.checker = checker;
        this.trees = Trees.instance(processingEnv);
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
        this.visitorState = new VisitorState();

        this.supportedQuals = new HashSet<>();
        this.supportedQualNames = new HashSet<>();
        this.stubTypes = new StubTypes(this);

        this.cacheDeclAnnos = new HashMap<>();

        this.artificialTreeToEnclosingElementMap = new HashMap<>();
        // get the shared instance from the checker
        this.treePathCache = checker.getTreePathCacher();

        this.shouldCache = !checker.hasOption("atfDoNotCache");
        if (shouldCache) {
            int cacheSize = getCacheSize();
            this.classAndMethodTreeCache = CollectionUtils.createLRUCache(cacheSize);
            this.fromExpressionTreeCache = CollectionUtils.createLRUCache(cacheSize);
            this.fromMemberTreeCache = CollectionUtils.createLRUCache(cacheSize);
            this.fromTypeTreeCache = CollectionUtils.createLRUCache(cacheSize);
            this.elementCache = CollectionUtils.createLRUCache(cacheSize);
            this.elementToTreeCache = CollectionUtils.createLRUCache(cacheSize);
            this.annotationClassNames =
                    Collections.synchronizedMap(
                            CollectionUtils.createLRUCache(ANNOTATION_CACHE_SIZE));
        } else {
            this.classAndMethodTreeCache = null;
            this.fromExpressionTreeCache = null;
            this.fromMemberTreeCache = null;
            this.fromTypeTreeCache = null;
            this.elementCache = null;
            this.elementToTreeCache = null;
            this.annotationClassNames = null;
        }

        this.typeFormatter = createAnnotatedTypeFormatter();
        this.annotationFormatter = createAnnotationFormatter();

        if (checker.hasOption("infer")) {
            checkInvalidOptionsInferSignatures();
            String inferArg = checker.getOption("infer");
            // No argument means "jaifs", for (temporary) backwards compatibility.
            if (inferArg == null) {
                inferArg = "jaifs";
            }
            switch (inferArg) {
                case "stubs":
                    wpiOutputFormat = WholeProgramInference.OutputFormat.STUB;
                    break;
                case "jaifs":
                    wpiOutputFormat = WholeProgramInference.OutputFormat.JAIF;
                    break;
                default:
                    throw new UserError(
                            "Unexpected option to -Ainfer: "
                                    + inferArg
                                    + System.lineSeparator()
                                    + "Available options: -Ainfer=jaifs, -Ainfer=stubs");
            }
            boolean isNullnessChecker =
                    "NullnessAnnotatedTypeFactory".equals(this.getClass().getSimpleName());
            wholeProgramInference = new WholeProgramInferenceScenes(!isNullnessChecker);
        } else {
            wholeProgramInference = null;
        }
        ignoreUninferredTypeArguments = !checker.hasOption("conservativeUninferredTypeArguments");

        objectGetClass = TreeUtils.getMethod("java.lang.Object", "getClass", 0, processingEnv);
    }

    /**
     * @throws BugInCF If supportedQuals is empty or if any of the support qualifiers has a @Target
     *     meta-annotation that contain something besides TYPE_USE or TYPE_PARAMETER. (@Target({})
     *     is allowed.)
     */
    private void checkSupportedQuals() {
        if (supportedQuals.isEmpty()) {
            throw new TypeSystemError("Found no supported qualifiers.");
        }
        for (Class<? extends Annotation> annotationClass : supportedQuals) {
            // Check @Target values
            ElementType[] elements = annotationClass.getAnnotation(Target.class).value();
            List<ElementType> otherElementTypes = new ArrayList<>();
            for (ElementType element : elements) {
                if (!(element == ElementType.TYPE_USE || element == ElementType.TYPE_PARAMETER)) {
                    // if there's an ElementType with an enumerated value of something other
                    // than TYPE_USE or TYPE_PARAMETER then it isn't a valid qualifier
                    otherElementTypes.add(element);
                }
            }
            if (!otherElementTypes.isEmpty()) {
                StringBuilder buf =
                        new StringBuilder("The @Target meta-annotation on type qualifier ");
                buf.append(annotationClass.toString());
                buf.append(" must not contain ");
                for (int i = 0; i < otherElementTypes.size(); i++) {
                    if (i == 1 && otherElementTypes.size() == 2) {
                        buf.append(" or ");
                    } else if (i == otherElementTypes.size() - 1) {
                        buf.append(", or ");
                    } else if (i != 0) {
                        buf.append(", ");
                    }
                    buf.append(otherElementTypes.get(i));
                }
                buf.append(".");
                throw new TypeSystemError(buf.toString());
            }
        }
    }

    /**
     * This method is called only when {@code -Ainfer} is passed as an option. It checks if another
     * option that should not occur simultaneously with the whole-program inference is also passed
     * as argument, and aborts the process if that is the case. For example, the whole-program
     * inference process was not designed to work with conservative defaults.
     *
     * <p>Subclasses may override this method to add more options.
     */
    protected void checkInvalidOptionsInferSignatures() {
        // See Issue 683
        // https://github.com/typetools/checker-framework/issues/683
        if (checker.useConservativeDefault("source")
                || checker.useConservativeDefault("bytecode")) {
            throw new UserError(
                    "The option -Ainfer=... cannot be used together with conservative defaults.");
        }
    }

    /**
     * Actions that logically belong in the constructor, but need to run after the subclass
     * constructor has completed. In particular, parseStubFiles() may try to do type resolution with
     * this AnnotatedTypeFactory.
     */
    protected void postInit() {
        this.qualHierarchy = createQualifierHierarchy();
        if (qualHierarchy == null) {
            throw new TypeSystemError(
                    "AnnotatedTypeFactory with null qualifier hierarchy not supported.");
        }
        this.typeHierarchy = createTypeHierarchy();
        this.typeVarSubstitutor = createTypeVariableSubstitutor();
        this.typeArgumentInference = createTypeArgumentInference();
        this.qualifierUpperBounds = createQualifierUpperBounds();

        // TODO: is this the best location for declaring this alias?
        addAliasedDeclAnnotation(
                org.jmlspecs.annotation.Pure.class,
                org.checkerframework.dataflow.qual.Pure.class,
                AnnotationBuilder.fromClass(
                        elements, org.checkerframework.dataflow.qual.Pure.class));

        // Accommodate the inability to write @InheritedAnnotation on these annotations.
        addInheritedAnnotation(
                AnnotationBuilder.fromClass(
                        elements, org.checkerframework.dataflow.qual.Pure.class));
        addInheritedAnnotation(
                AnnotationBuilder.fromClass(
                        elements, org.checkerframework.dataflow.qual.SideEffectFree.class));
        addInheritedAnnotation(
                AnnotationBuilder.fromClass(
                        elements, org.checkerframework.dataflow.qual.Deterministic.class));
        addInheritedAnnotation(
                AnnotationBuilder.fromClass(
                        elements, org.checkerframework.dataflow.qual.TerminatesExecution.class));

        initializeReflectionResolution();

        if (this.getClass() == AnnotatedTypeFactory.class) {
            this.parseStubFiles();
        }
    }

    /** Creates {@link QualifierUpperBounds} for this type factory. */
    protected QualifierUpperBounds createQualifierUpperBounds() {
        return new QualifierUpperBounds(this);
    }

    /**
     * Return {@link QualifierUpperBounds} for this type factory.
     *
     * @return {@link QualifierUpperBounds} for this type factory
     */
    public QualifierUpperBounds getQualifierUpperBounds() {
        return qualifierUpperBounds;
    }

    /**
     * Returns the WholeProgramInference instance (may be null).
     *
     * @return the WholeProgramInference instance, or null
     */
    public WholeProgramInference getWholeProgramInference() {
        return wholeProgramInference;
    }

    protected void initializeReflectionResolution() {
        if (checker.shouldResolveReflection()) {
            boolean debug = "debug".equals(checker.getOption("resolveReflection"));

            MethodValChecker methodValChecker = checker.getSubchecker(MethodValChecker.class);
            assert methodValChecker != null
                    : "AnnotatedTypeFactory: reflection resolution was requested, but MethodValChecker isn't a subchecker.";
            MethodValAnnotatedTypeFactory methodValATF =
                    (MethodValAnnotatedTypeFactory) methodValChecker.getAnnotationProvider();

            reflectionResolver = new DefaultReflectionResolver(checker, methodValATF, debug);
        }
    }

    /**
     * Set the CompilationUnitTree that should be used.
     *
     * @param root the new compilation unit to use
     */
    // What's a better name? Maybe "reset" or "restart"?
    public void setRoot(@Nullable CompilationUnitTree root) {
        this.root = root;
        // Do not clear here. Only the primary checker should clear this cache.
        // treePathCache.clear();
        artificialTreeToEnclosingElementMap.clear();

        if (shouldCache) {
            // Clear the caches with trees because once the compilation unit changes,
            // the trees may be modified and lose type arguments.
            elementToTreeCache.clear();
            fromExpressionTreeCache.clear();
            fromMemberTreeCache.clear();
            fromTypeTreeCache.clear();
            classAndMethodTreeCache.clear();

            // There is no need to clear the following cache, it is limited by cache size and it
            // contents won't change between compilation units.
            // elementCache.clear();
        }
    }

    @SideEffectFree
    @Override
    public String toString() {
        return getClass().getSimpleName() + "#" + uid;
    }

    /** Factory method to easily change what Factory is used to create a QualifierHierarchy. */
    protected MultiGraphQualifierHierarchy.MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    /**
     * Factory method to easily change what QualifierHierarchy is created. Needs to be public only
     * because the GraphFactory must be able to call this method. No external use of this method is
     * necessary.
     */
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new GraphQualifierHierarchy(factory, null);
    }

    /**
     * Returns the type qualifier hierarchy graph to be used by this processor.
     *
     * <p>The implementation builds the type qualifier hierarchy for the {@link
     * #getSupportedTypeQualifiers()} using the meta-annotations found in them. The current
     * implementation returns an instance of {@code GraphQualifierHierarchy}.
     *
     * <p>Subclasses may override this method to express any relationships that cannot be inferred
     * using meta-annotations (e.g. due to lack of meta-annotations).
     *
     * @return an annotation relation tree representing the supported qualifiers
     */
    protected QualifierHierarchy createQualifierHierarchy() {
        Set<Class<? extends Annotation>> supportedTypeQualifiers = getSupportedTypeQualifiers();
        MultiGraphQualifierHierarchy.MultiGraphFactory factory =
                this.createQualifierHierarchyFactory();

        return createQualifierHierarchy(elements, supportedTypeQualifiers, factory);
    }

    /**
     * Returns the type qualifier hierarchy graph for a given set of type qualifiers and a factory.
     *
     * <p>The implementation builds the type qualifier hierarchy for the {@code
     * supportedTypeQualifiers}. The current implementation returns an instance of {@code
     * GraphQualifierHierarchy}.
     *
     * @param elements the element utilities to use
     * @param supportedTypeQualifiers the type qualifiers for this type system
     * @param factory the type factory for this type system
     * @return an annotation relation tree representing the supported qualifiers
     */
    protected static QualifierHierarchy createQualifierHierarchy(
            Elements elements,
            Set<Class<? extends Annotation>> supportedTypeQualifiers,
            MultiGraphFactory factory) {

        for (Class<? extends Annotation> typeQualifier : supportedTypeQualifiers) {
            AnnotationMirror typeQualifierAnno =
                    AnnotationBuilder.fromClass(elements, typeQualifier);
            factory.addQualifier(typeQualifierAnno);
            // Polymorphic qualifiers can't declare their supertypes.
            // An error is raised if one is present.
            if (typeQualifier.getAnnotation(PolymorphicQualifier.class) != null) {
                if (typeQualifier.getAnnotation(SubtypeOf.class) != null) {
                    // This is currently not supported. At some point we might add
                    // polymorphic qualifiers with upper and lower bounds.
                    throw new TypeSystemError(
                            "AnnotatedTypeFactory: "
                                    + typeQualifier
                                    + " is polymorphic and specifies super qualifiers. "
                                    + "Remove the @org.checkerframework.framework.qual.SubtypeOf or @org.checkerframework.framework.qual.PolymorphicQualifier annotation from it.");
                }
                continue;
            }
            if (typeQualifier.getAnnotation(SubtypeOf.class) == null) {
                throw new TypeSystemError(
                        "AnnotatedTypeFactory: %s does not specify its super qualifiers.%n"
                                + "Add an @org.checkerframework.framework.qual.SubtypeOf annotation to it,%n"
                                + "or if it is an alias, exclude it from `createSupportedTypeQualifiers()`.%n",
                        typeQualifier);
            }
            Class<? extends Annotation>[] superQualifiers =
                    typeQualifier.getAnnotation(SubtypeOf.class).value();
            for (Class<? extends Annotation> superQualifier : superQualifiers) {
                if (!supportedTypeQualifiers.contains(superQualifier)) {
                    throw new TypeSystemError(
                            "Found unsupported qualifier in SubTypeOf: %s on qualifier: %s",
                            superQualifier.getCanonicalName(), typeQualifier.getCanonicalName());
                }
                if (superQualifier.getAnnotation(PolymorphicQualifier.class) != null) {
                    // This is currently not supported. No qualifier can have a polymorphic
                    // qualifier as super qualifier.
                    throw new TypeSystemError(
                            "Found polymorphic qualifier in SubTypeOf: %s on qualifier: %s",
                            superQualifier.getCanonicalName(), typeQualifier.getCanonicalName());
                }
                AnnotationMirror superAnno = AnnotationBuilder.fromClass(elements, superQualifier);
                factory.addSubtype(typeQualifierAnno, superAnno);
            }
        }

        QualifierHierarchy hierarchy = factory.build();

        if (!hierarchy.isValid()) {
            throw new TypeSystemError(
                    "AnnotatedTypeFactory: invalid qualifier hierarchy: "
                            + hierarchy.getClass()
                            + " "
                            + hierarchy);
        }

        return hierarchy;
    }

    /**
     * Returns the type qualifier hierarchy graph to be used by this processor.
     *
     * @see #createQualifierHierarchy()
     * @return the {@link QualifierHierarchy} for this checker
     */
    public final QualifierHierarchy getQualifierHierarchy() {
        // if (qualHierarchy == null)
        //    qualHierarchy = createQualifierHierarchy();
        return qualHierarchy;
    }

    /**
     * Creates the type hierarchy to be used by this factory.
     *
     * <p>Subclasses may override this method to specify new type-checking rules beyond the typical
     * Java subtyping rules.
     *
     * @return the type relations class to check type subtyping
     */
    protected TypeHierarchy createTypeHierarchy() {
        return new DefaultTypeHierarchy(
                checker,
                getQualifierHierarchy(),
                checker.getBooleanOption("ignoreRawTypeArguments", true),
                checker.hasOption("invariantArrays"));
    }

    public final TypeHierarchy getTypeHierarchy() {
        return typeHierarchy;
    }

    /**
     * TypeVariableSubstitutor provides a method to replace type parameters with their arguments.
     */
    protected TypeVariableSubstitutor createTypeVariableSubstitutor() {
        return new TypeVariableSubstitutor();
    }

    public TypeVariableSubstitutor getTypeVarSubstitutor() {
        return typeVarSubstitutor;
    }

    /**
     * TypeArgumentInference infers the method type arguments when they are not explicitly written.
     */
    protected TypeArgumentInference createTypeArgumentInference() {
        return new DefaultTypeArgumentInference(this);
    }

    public TypeArgumentInference getTypeArgumentInference() {
        return typeArgumentInference;
    }

    /**
     * Factory method to easily change what {@link AnnotationClassLoader} is created to load type
     * annotation classes. Subclasses can override this method and return a custom
     * AnnotationClassLoader subclass to customize loading logic.
     */
    protected AnnotationClassLoader createAnnotationClassLoader() {
        return new AnnotationClassLoader(checker);
    }

    /**
     * Returns a mutable set of annotation classes that are supported by a checker.
     *
     * <p>Subclasses may override this method to return a mutable set of their supported type
     * qualifiers through one of the 5 approaches shown below.
     *
     * <p>Subclasses should not call this method; they should call {@link
     * #getSupportedTypeQualifiers} instead.
     *
     * <p>By default, a checker supports all annotations located in a subdirectory called {@literal
     * qual} that's located in the same directory as the checker. Note that only annotations defined
     * with the {@code @Target({ElementType.TYPE_USE})} meta-annotation (and optionally with the
     * additional value of {@code ElementType.TYPE_PARAMETER}, but no other {@code ElementType}
     * values) are automatically considered as supported annotations.
     *
     * <p>To support a different set of annotations than those in the {@literal qual} subdirectory,
     * or that have other {@code ElementType} values, see examples below.
     *
     * <p>In total, there are 5 ways to indicate annotations that are supported by a checker:
     *
     * <ol>
     *   <li>Only support annotations located in a checker's {@literal qual} directory:
     *       <p>This is the default behavior. Simply place those annotations within the {@literal
     *       qual} directory.
     *   <li>Support annotations located in a checker's {@literal qual} directory and a list of
     *       other annotations:
     *       <p>Place those annotations within the {@literal qual} directory, and override {@link
     *       #createSupportedTypeQualifiers()} by calling {@link
     *       #getBundledTypeQualifiers(Class...)} with a varargs parameter list of the other
     *       annotations. Code example:
     *       <pre>
     * {@code @Override protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
     *      return getBundledTypeQualifiers(Regex.class, PartialRegex.class, RegexBottom.class, UnknownRegex.class);
     *  } }
     * </pre>
     *   <li>Supporting only annotations that are explicitly listed: Override {@link
     *       #createSupportedTypeQualifiers()} and return a mutable set of the supported
     *       annotations. Code example:
     *       <pre>
     * {@code @Override protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
     *      return new HashSet<Class<? extends Annotation>>(
     *              Arrays.asList(A.class, B.class));
     *  } }
     * </pre>
     *       The set of qualifiers returned by {@link #createSupportedTypeQualifiers()} must be a
     *       fresh, mutable set. The methods {@link #getBundledTypeQualifiers(Class...)} must return
     *       a fresh, mutable set
     * </ol>
     *
     * @return the type qualifiers supported this processor, or an empty set if none
     */
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiers();
    }

    /**
     * Loads all annotations contained in the qual directory of a checker via reflection; if a
     * polymorphic type qualifier exists, and an explicit array of annotations to the set of
     * annotation classes.
     *
     * <p>This method can be called in the overridden versions of {@link
     * #createSupportedTypeQualifiers()} in each checker.
     *
     * @param explicitlyListedAnnotations a varargs array of explicitly listed annotation classes to
     *     be added to the returned set. For example, it is used frequently to add Bottom
     *     qualifiers.
     * @return a mutable set of the loaded and listed annotation classes
     */
    @SafeVarargs
    protected final Set<Class<? extends Annotation>> getBundledTypeQualifiers(
            Class<? extends Annotation>... explicitlyListedAnnotations) {
        return loadTypeAnnotationsFromQualDir(explicitlyListedAnnotations);
    }

    /**
     * Instantiates the AnnotationClassLoader and loads all annotations contained in the qual
     * directory of a checker via reflection, and has the option to include an explicitly stated
     * list of annotations (eg ones found in a different directory than qual).
     *
     * <p>The annotations that are automatically loaded must have the {@link
     * java.lang.annotation.Target Target} meta-annotation with the value of {@link
     * ElementType#TYPE_USE} (and optionally {@link ElementType#TYPE_PARAMETER}). If it has other
     * {@link ElementType} values, it won't be loaded. Other annotation classes must be explicitly
     * listed even if they are in the same directory as the checker's qual directory.
     *
     * @param explicitlyListedAnnotations a set of explicitly listed annotation classes to be added
     *     to the returned set, for example, it is used frequently to add Bottom qualifiers
     * @return a set of annotation class instances
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    private final Set<Class<? extends Annotation>> loadTypeAnnotationsFromQualDir(
            Class<? extends Annotation>... explicitlyListedAnnotations) {
        loader = createAnnotationClassLoader();

        Set<Class<? extends Annotation>> annotations = loader.getBundledAnnotationClasses();

        // add in all explicitly Listed qualifiers
        if (explicitlyListedAnnotations != null) {
            annotations.addAll(Arrays.asList(explicitlyListedAnnotations));
        }

        return annotations;
    }

    /**
     * Creates the AnnotatedTypeFormatter used by this type factory and all AnnotatedTypeMirrors it
     * creates. The AnnotatedTypeFormatter is used in AnnotatedTypeMirror.toString and will affect
     * the error messages printed for checkers that use this type factory.
     *
     * @return the AnnotatedTypeFormatter to pass to all instantiated AnnotatedTypeMirrors
     */
    protected AnnotatedTypeFormatter createAnnotatedTypeFormatter() {
        boolean printVerboseGenerics = checker.hasOption("printVerboseGenerics");
        return new DefaultAnnotatedTypeFormatter(
                printVerboseGenerics,
                // -AprintVerboseGenerics implies -AprintAllQualifiers
                printVerboseGenerics || checker.hasOption("printAllQualifiers"));
    }

    public AnnotatedTypeFormatter getAnnotatedTypeFormatter() {
        return typeFormatter;
    }

    protected AnnotationFormatter createAnnotationFormatter() {
        return new DefaultAnnotationFormatter();
    }

    public AnnotationFormatter getAnnotationFormatter() {
        return annotationFormatter;
    }

    /**
     * Returns an immutable set of the classes corresponding to the type qualifiers supported by
     * this checker.
     *
     * <p>Subclasses cannot override this method; they should override {@link
     * #createSupportedTypeQualifiers createSupportedTypeQualifiers} instead.
     *
     * @see #createSupportedTypeQualifiers()
     * @return an immutable set of the supported type qualifiers, or an empty set if no qualifiers
     *     are supported
     */
    public final Set<Class<? extends Annotation>> getSupportedTypeQualifiers() {
        if (this.supportedQuals.isEmpty()) {
            supportedQuals.addAll(createSupportedTypeQualifiers());
            checkSupportedQuals();
        }
        return Collections.unmodifiableSet(supportedQuals);
    }

    /**
     * Returns an immutable set of the fully qualified names of the type qualifiers supported by
     * this checker.
     *
     * <p>Subclasses cannot override this method; they should override {@link
     * #createSupportedTypeQualifiers createSupportedTypeQualifiers} instead.
     *
     * @see #createSupportedTypeQualifiers()
     * @return an immutable set of the supported type qualifiers, or an empty set if no qualifiers
     *     are supported
     */
    public final Set<String> getSupportedTypeQualifierNames() {
        if (this.supportedQualNames.isEmpty()) {
            for (Class<?> clazz : getSupportedTypeQualifiers()) {
                supportedQualNames.add(clazz.getCanonicalName());
            }
        }
        return Collections.unmodifiableSet(supportedQualNames);
    }

    // **********************************************************************
    // Factories for annotated types that account for default qualifiers
    // **********************************************************************

    /**
     * Returns the int supplied to the checker via the atfCacheSize option or the default cache
     * size.
     *
     * @return cache size passed as argument to checker or DEFAULT_CACHE_SIZE
     */
    protected int getCacheSize() {
        String option = checker.getOption("atfCacheSize");
        if (option == null) {
            return DEFAULT_CACHE_SIZE;
        }
        try {
            return Integer.valueOf(option);
        } catch (NumberFormatException ex) {
            throw new UserError("atfCacheSize was not an integer: " + option);
        }
    }

    /**
     * Returns an AnnotatedTypeMirror representing the annotated type of {@code elt}.
     *
     * @param elt the element
     * @return the annotated type of {@code elt}
     */
    public AnnotatedTypeMirror getAnnotatedType(Element elt) {
        if (elt == null) {
            throw new BugInCF("AnnotatedTypeFactory.getAnnotatedType: null element");
        }
        // Annotations explicitly written in the source code,
        // or obtained from bytecode.
        AnnotatedTypeMirror type = fromElement(elt);
        addComputedTypeAnnotations(elt, type);
        return type;
    }

    @Override
    public @Nullable AnnotationMirror getAnnotationMirror(
            Tree tree, Class<? extends Annotation> target) {
        if (isSupportedQualifier(target)) {
            AnnotatedTypeMirror atm = getAnnotatedType(tree);
            return atm.getAnnotation(target);
        }
        return null;
    }

    /**
     * Returns an AnnotatedTypeMirror representing the annotated type of {@code tree}.
     *
     * @param tree the AST node
     * @return the annotated type of {@code tree}
     */
    public AnnotatedTypeMirror getAnnotatedType(Tree tree) {

        /// For debugging
        // String treeString = tree.toString();
        // if (treeString.length() > 63) {
        //     treeString = treeString.substring(0, 60) + "...";
        // }

        if (tree == null) {
            throw new BugInCF("AnnotatedTypeFactory.getAnnotatedType: null tree");
        }
        if (shouldCache && classAndMethodTreeCache.containsKey(tree)) {
            return classAndMethodTreeCache.get(tree).deepCopy();
        }

        AnnotatedTypeMirror type;
        if (TreeUtils.isClassTree(tree)) {
            type = fromClass((ClassTree) tree);
        } else if (tree.getKind() == Tree.Kind.METHOD || tree.getKind() == Tree.Kind.VARIABLE) {
            type = fromMember(tree);
        } else if (TreeUtils.isExpressionTree(tree)) {
            tree = TreeUtils.withoutParens((ExpressionTree) tree);
            type = fromExpression((ExpressionTree) tree);
        } else {
            throw new BugInCF(
                    "AnnotatedTypeFactory.getAnnotatedType: query of annotated type for tree "
                            + tree.getKind());
        }

        addComputedTypeAnnotations(tree, type);

        if (TreeUtils.isClassTree(tree) || tree.getKind() == Tree.Kind.METHOD) {
            // Don't cache VARIABLE
            if (shouldCache) {
                classAndMethodTreeCache.put(tree, type.deepCopy());
            }
        } else {
            // No caching otherwise
        }

        // System.out.println("AnnotatedTypeFactory::getAnnotatedType(Tree) result: " + type);
        return type;
    }

    /**
     * Called by {@link BaseTypeVisitor#visitClass(ClassTree, Void)} before the classTree is type
     * checked.
     *
     * @param classTree ClassTree on which to perform preprocessing
     */
    public void preProcessClassTree(ClassTree classTree) {}

    /**
     * Called by {@link BaseTypeVisitor#visitClass(ClassTree, Void)} after the ClassTree has been
     * type checked.
     *
     * <p>The default implementation uses this to store the defaulted AnnotatedTypeMirrors and
     * inherited declaration annotations back into the corresponding Elements. Subclasses might want
     * to override this method if storing defaulted types is not desirable.
     */
    public void postProcessClassTree(ClassTree tree) {
        TypesIntoElements.store(processingEnv, this, tree);
        DeclarationsIntoElements.store(processingEnv, this, tree);
        if (wholeProgramInference != null) {
            // Write out the results of whole-program inference, just once for each class.
            wholeProgramInference.writeResultsToFile(wpiOutputFormat, this.checker);
        }
    }

    /**
     * Determines the annotated type from a type in tree form.
     *
     * <p>Note that we cannot decide from a Tree whether it is a type use or an expression.
     * TreeUtils.isTypeTree is only an under-approximation. For example, an identifier can be either
     * a type or an expression.
     *
     * @param tree the type tree
     * @return the annotated type of the type in the AST
     */
    public AnnotatedTypeMirror getAnnotatedTypeFromTypeTree(Tree tree) {
        if (tree == null) {
            throw new BugInCF("AnnotatedTypeFactory.getAnnotatedTypeFromTypeTree: null tree");
        }
        AnnotatedTypeMirror type = fromTypeTree(tree);
        addComputedTypeAnnotations(tree, type);
        return type;
    }

    /**
     * Returns the set of qualifiers that are the upper bounds for a use of the type.
     *
     * @param type a type whose upper bounds to obtain
     */
    public Set<AnnotationMirror> getTypeDeclarationBounds(TypeMirror type) {
        return qualifierUpperBounds.getBoundQualifiers(type);
    }

    /**
     * Returns the set of qualifiers that are the upper bound for a type use if no other bound is
     * specified for the type.
     *
     * <p>This implementation returns the top qualifiers by default. Subclass may override to return
     * different qualifiers.
     *
     * @return the set of qualifiers that are the upper bound for a type use if no other bound is
     *     specified for the type
     */
    protected Set<? extends AnnotationMirror> getDefaultTypeDeclarationBounds() {
        return qualHierarchy.getTopAnnotations();
    }

    /**
     * Returns the type of the extends or implements clause.
     *
     * <p>The primary qualifier is either an explicit annotation on {@code clause}, or it is the
     * qualifier upper bounds for uses of the type of the clause.
     *
     * @param clause tree that represents an extends or implements clause
     * @return the type of the extends or implements clause
     */
    public AnnotatedTypeMirror getTypeOfExtendsImplements(Tree clause) {
        AnnotatedTypeMirror fromTypeTree = fromTypeTree(clause);
        Set<AnnotationMirror> bound = getTypeDeclarationBounds(fromTypeTree.getUnderlyingType());
        fromTypeTree.addMissingAnnotations(bound);
        return fromTypeTree;
    }

    // **********************************************************************
    // Factories for annotated types that do not account for default qualifiers.
    // They only include qualifiers explicitly inserted by the user.
    // **********************************************************************

    /**
     * Creates an AnnotatedTypeMirror for {@code elt} that includes: annotations explicitly written
     * on the element and annotations from stub files.
     *
     * @param elt the element
     * @return AnnotatedTypeMirror of the element with explicitly-written and stub file annotations
     */
    public AnnotatedTypeMirror fromElement(Element elt) {
        if (shouldCache && elementCache.containsKey(elt)) {
            return elementCache.get(elt).deepCopy();
        }
        if (elt.getKind() == ElementKind.PACKAGE) {
            return toAnnotatedType(elt.asType(), false);
        }
        AnnotatedTypeMirror type;

        // Because of a bug in Java 8, annotations on type parameters are not stored in elements,
        // so get explicit annotations from the tree. (This bug has been fixed in Java 9.)
        // Also, since annotations computed by the AnnotatedTypeFactory are stored in the element,
        // the annotations have to be retrieved from the tree so that only explicit annotations are
        // returned.
        Tree decl = declarationFromElement(elt);

        if (decl == null) {
            type = stubTypes.getAnnotatedTypeMirror(elt);
            if (type == null) {
                type = toAnnotatedType(elt.asType(), ElementUtils.isTypeDeclaration(elt));
                ElementAnnotationApplier.apply(type, elt, this);
            }
        } else if (decl instanceof ClassTree) {
            type = fromClass((ClassTree) decl);
        } else if (decl instanceof VariableTree) {
            type = fromMember(decl);
        } else if (decl instanceof MethodTree) {
            type = fromMember(decl);
        } else if (decl.getKind() == Tree.Kind.TYPE_PARAMETER) {
            type = fromTypeTree(decl);
        } else {
            throw new BugInCF(
                    "AnnotatedTypeFactory.fromElement: cannot be here. decl: "
                            + decl.getKind()
                            + " elt: "
                            + elt);
        }

        if (checker.hasOption("mergeStubsWithSource")) {
            type = mergeStubsIntoType(type, elt);
        }
        // Caching is disabled if stub files are being parsed, because calls to this
        // method before the stub files are fully read can return incorrect results.
        if (shouldCache && !stubTypes.isParsing()) {
            elementCache.put(elt, type.deepCopy());
        }
        return type;
    }

    /**
     * Returns an AnnotatedDeclaredType with explicit annotations from the ClassTree {@code tree}.
     *
     * @param tree the class declaration
     * @return AnnotatedDeclaredType with explicit annotations from {@code tree}
     */
    private AnnotatedDeclaredType fromClass(ClassTree tree) {
        return TypeFromTree.fromClassTree(this, tree);
    }

    /**
     * Creates an AnnotatedTypeMirror for a variable or method declaration tree. The
     * AnnotatedTypeMirror contains annotations explicitly written on the tree.
     *
     * <p>If a VariableTree is a parameter to a lambda, this method also adds annotations from the
     * declared type of the functional interface and the executable type of its method.
     *
     * @param tree MethodTree or VariableTree
     * @return AnnotatedTypeMirror with explicit annotations from {@code tree}.
     */
    private final AnnotatedTypeMirror fromMember(Tree tree) {
        if (!(tree instanceof MethodTree || tree instanceof VariableTree)) {
            throw new BugInCF(
                    "AnnotatedTypeFactory.fromMember: not a method or variable declaration: "
                            + tree);
        }
        if (shouldCache && fromMemberTreeCache.containsKey(tree)) {
            return fromMemberTreeCache.get(tree).deepCopy();
        }
        AnnotatedTypeMirror result = TypeFromTree.fromMember(this, tree);

        if (checker.hasOption("mergeStubsWithSource")) {
            result = mergeStubsIntoType(result, tree);
        }

        if (shouldCache) {
            fromMemberTreeCache.put(tree, result.deepCopy());
        }

        return result;
    }

    /**
     * Merges types from stub files for {@code tree} into {@code type} by taking the greatest lower
     * bound of the annotations in both.
     *
     * @param type the type to apply stub types to
     * @param tree the tree from which to read stub types
     * @return type, side-effected to add the stub types
     */
    private AnnotatedTypeMirror mergeStubsIntoType(@Nullable AnnotatedTypeMirror type, Tree tree) {
        Element elt = TreeUtils.elementFromTree(tree);
        return mergeStubsIntoType(type, elt);
    }

    /**
     * Merges types from stub files for {@code elt} into {@code type} by taking the greatest lower
     * bound of the annotations in both.
     *
     * @param type the type to apply stub types to
     * @param elt the element from which to read stub types
     * @return the type, side-effected to add the stub types
     */
    protected AnnotatedTypeMirror mergeStubsIntoType(
            @Nullable AnnotatedTypeMirror type, Element elt) {
        AnnotatedTypeMirror stubType = stubTypes.getAnnotatedTypeMirror(elt);
        if (stubType != null) {
            if (type == null) {
                type = stubType;
            } else {
                // Must merge (rather than only take the stub type if it is a subtype)
                // to support WPI.
                AnnotatedTypeCombiner.combine(stubType, type, this.getQualifierHierarchy());
            }
        }
        return type;
    }

    /**
     * Creates an AnnotatedTypeMirror for an ExpressionTree. The AnnotatedTypeMirror contains
     * explicit annotations written on the expression and for some expressions, annotations from
     * sub-expressions that could have been explicitly written, defaulted, refined, or otherwise
     * computed. (Expression whose type include annotations from sub-expressions are:
     * ArrayAccessTree, ConditionalExpressionTree, IdentifierTree, MemberSelectTree, and
     * MethodInvocationTree.)
     *
     * <p>For example, the AnnotatedTypeMirror returned for an array access expression is the fully
     * annotated type of the array component of the array being accessed.
     *
     * @param tree an expression
     * @return AnnotatedTypeMirror of the expressions either fully-annotated or partially annotated
     *     depending on the kind of expression
     * @see TypeFromExpressionVisitor
     */
    private AnnotatedTypeMirror fromExpression(ExpressionTree tree) {
        if (shouldCache && fromExpressionTreeCache.containsKey(tree)) {
            return fromExpressionTreeCache.get(tree).deepCopy();
        }

        AnnotatedTypeMirror result = TypeFromTree.fromExpression(this, tree);

        if (shouldCache && tree.getKind() != Tree.Kind.NEW_CLASS) {
            // Don't cache the type of object creations, because incorrect
            // annotations would be cached during dataflow analysis.
            // See Issue #602.
            fromExpressionTreeCache.put(tree, result.deepCopy());
        }
        return result;
    }

    /**
     * Creates an AnnotatedTypeMirror for the tree. The AnnotatedTypeMirror contains annotations
     * explicitly written on the tree. It also adds type arguments to raw types that include
     * annotations from the element declaration of the type {@link #fromElement(Element)}.
     *
     * <p>Called on the following trees: AnnotatedTypeTree, ArrayTypeTree, ParameterizedTypeTree,
     * PrimitiveTypeTree, TypeParameterTree, WildcardTree, UnionType, IntersectionTypeTree, and
     * IdentifierTree, MemberSelectTree.
     *
     * @param tree the type tree
     * @return the (partially) annotated type of the type in the AST
     */
    /*package private*/ final AnnotatedTypeMirror fromTypeTree(Tree tree) {
        if (shouldCache && fromTypeTreeCache.containsKey(tree)) {
            return fromTypeTreeCache.get(tree).deepCopy();
        }

        AnnotatedTypeMirror result = TypeFromTree.fromTypeTree(this, tree);

        if (shouldCache) {
            fromTypeTreeCache.put(tree, result.deepCopy());
        }
        return result;
    }

    // **********************************************************************
    // Customization methods meant to be overridden by subclasses to include
    // defaulted annotations
    // **********************************************************************

    /**
     * Changes annotations on a type obtained from a {@link Tree}. By default, this method does
     * nothing. GenericAnnotatedTypeFactory uses this method to implement defaulting and inference
     * (flow-sensitive type refinement). Its subclasses usually override it only to customize
     * default annotations.
     *
     * <p>Subclasses that override this method should also override {@link
     * #addComputedTypeAnnotations(Element, AnnotatedTypeMirror)}.
     *
     * <p>In classes that extend {@link GenericAnnotatedTypeFactory}, override {@link
     * GenericAnnotatedTypeFactory#addComputedTypeAnnotations(Tree, AnnotatedTypeMirror, boolean)}
     * instead of this method.
     *
     * @param tree an AST node
     * @param type the type obtained from {@code tree}
     */
    protected void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type) {
        // Pass.
    }

    /**
     * Changes annotations on a type obtained from an {@link Element}. By default, this method does
     * nothing. GenericAnnotatedTypeFactory uses this method to implement defaulting.
     *
     * <p>Subclasses that override this method should also override {@link
     * #addComputedTypeAnnotations(Tree, AnnotatedTypeMirror)}.
     *
     * @param elt an element
     * @param type the type obtained from {@code elt}
     */
    protected void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
        // Pass.
    }

    /**
     * Adds default annotations to {@code type}. This method should only be used in places where the
     * correct annotations cannot be computed because of uninferred type arguments. (See {@link
     * AnnotatedWildcardType#isUninferredTypeArgument()}.)
     *
     * @param type annotated type to which default annotations are added
     */
    protected void addDefaultAnnotations(AnnotatedTypeMirror type) {
        // Pass.
    }

    /**
     * A callback method for the AnnotatedTypeFactory subtypes to customize directSuperTypes().
     * Overriding methods should merely change the annotations on the supertypes, without adding or
     * removing new types.
     *
     * <p>The default provided implementation adds {@code type} annotations to {@code supertypes}.
     * This allows the {@code type} and its supertypes to have the qualifiers.
     *
     * @param type the type whose supertypes are desired
     * @param supertypes the supertypes as specified by the base AnnotatedTypeFactory
     */
    protected void postDirectSuperTypes(
            AnnotatedTypeMirror type, List<? extends AnnotatedTypeMirror> supertypes) {
        // Use the effective annotations here to get the correct annotations
        // for type variables and wildcards.
        Set<AnnotationMirror> annotations = type.getEffectiveAnnotations();
        for (AnnotatedTypeMirror supertype : supertypes) {
            if (!annotations.equals(supertype.getEffectiveAnnotations())) {
                supertype.clearAnnotations();
                // TODO: is this correct for type variables and wildcards?
                supertype.addAnnotations(annotations);
            }
        }
    }

    /**
     * A callback method for the AnnotatedTypeFactory subtypes to customize
     * AnnotatedTypes.asMemberOf(). Overriding methods should merely change the annotations on the
     * subtypes, without changing the types.
     *
     * @param type the annotated type of the element
     * @param owner the annotated type of the receiver of the accessing tree
     * @param element the element of the field or method
     */
    public void postAsMemberOf(
            AnnotatedTypeMirror type, AnnotatedTypeMirror owner, Element element) {
        if (element.getKind() == ElementKind.FIELD) {
            addAnnotationFromFieldInvariant(type, owner, (VariableElement) element);
        }
        addComputedTypeAnnotations(element, type);
    }

    /**
     * Adds the qualifier specified by a field invariant for {@code field} to {@code type}.
     *
     * @param type annotated type to which the annotation is added
     * @param accessedVia the annotated type of the receiver of the accessing tree. (Only used to
     *     get the type element of the underling type.)
     * @param field element representing the field
     */
    protected void addAnnotationFromFieldInvariant(
            AnnotatedTypeMirror type, AnnotatedTypeMirror accessedVia, VariableElement field) {
        TypeMirror declaringType = accessedVia.getUnderlyingType();
        // Find the first upper bound that isn't a wildcard or type variable
        while (declaringType.getKind() == TypeKind.WILDCARD
                || declaringType.getKind() == TypeKind.TYPEVAR) {
            if (declaringType.getKind() == TypeKind.WILDCARD) {
                declaringType = TypesUtils.wildUpperBound(declaringType, processingEnv);
            } else if (declaringType.getKind() == TypeKind.TYPEVAR) {
                declaringType = ((TypeVariable) declaringType).getUpperBound();
            }
        }
        TypeElement typeElement = TypesUtils.getTypeElement(declaringType);
        if (ElementUtils.enclosingClass(field).equals(typeElement)) {
            // If the field is declared in the accessedVia class, then the field in the invariant
            // cannot be this field, even if the field has the same name.
            return;
        }

        FieldInvariants invariants = getFieldInvariants(typeElement);
        if (invariants == null) {
            return;
        }
        List<AnnotationMirror> invariantAnnos = invariants.getQualifiersFor(field.getSimpleName());
        type.replaceAnnotations(invariantAnnos);
    }

    /**
     * Returns the field invariants for the given class, as expressed by the user in {@link
     * FieldInvariant @FieldInvariant} method annotations.
     *
     * <p>Subclasses may implement their own field invariant annotations if {@link
     * FieldInvariant @FieldInvariant} is not expressive enough. They must override this method to
     * properly create AnnotationMirror and also override {@link
     * #getFieldInvariantDeclarationAnnotations()} to return their field invariants.
     *
     * @param element class for which to get invariants
     * @return fields invariants for {@code element}
     */
    public FieldInvariants getFieldInvariants(TypeElement element) {
        if (element == null) {
            return null;
        }
        AnnotationMirror fieldInvarAnno = getDeclAnnotation(element, FieldInvariant.class);
        if (fieldInvarAnno == null) {
            return null;
        }
        List<String> fields =
                AnnotationUtils.getElementValueArray(fieldInvarAnno, "field", String.class, true);
        List<Name> classes =
                AnnotationUtils.getElementValueClassNames(fieldInvarAnno, "qualifier", true);
        List<AnnotationMirror> qualifiers = new ArrayList<>();
        for (Name name : classes) {
            // Calling AnnotationBuilder.fromName (which ignores elements/fields) is acceptable
            // because @FieldInvariant does not handle classes with elements/fields.
            qualifiers.add(AnnotationBuilder.fromName(elements, name));
        }
        if (qualifiers.size() == 1) {
            while (fields.size() > qualifiers.size()) {
                qualifiers.add(qualifiers.get(0));
            }
        }
        if (fields.size() != qualifiers.size()) {
            // The user wrote a malformed @FieldInvariant annotation, so just return a malformed
            // FieldInvariants object.  The BaseTypeVisitor will issue an error.
            return new FieldInvariants(fields, qualifiers);
        }

        // Only keep qualifiers that are supported by this checker.  (The other qualifiers cannot
        // be checked by this checker, so they must be ignored.)
        List<String> supportFields = new ArrayList<>();
        List<AnnotationMirror> supportedQualifiers = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            if (isSupportedQualifier(qualifiers.get(i))) {
                supportedQualifiers.add(qualifiers.get(i));
                supportFields.add(fields.get(i));
            }
        }
        if (supportFields.isEmpty() && qualifiers.isEmpty()) {
            return null;
        }

        return new FieldInvariants(supportFields, supportedQualifiers);
    }

    /**
     * Returns the AnnotationTree which is a use of one of the field invariant annotations (as
     * specified via {@link #getFieldInvariantDeclarationAnnotations()}. If one isn't found, null is
     * returned.
     *
     * @param annoTrees trees to look
     * @return returns the AnnotationTree which is a use of one of the field invariant annotations
     *     or null if one isn't found
     */
    public AnnotationTree getFieldInvariantAnnotationTree(
            List<? extends AnnotationTree> annoTrees) {
        List<AnnotationMirror> annos = TreeUtils.annotationsFromTypeAnnotationTrees(annoTrees);
        for (int i = 0; i < annos.size(); i++) {
            for (Class<? extends Annotation> clazz : getFieldInvariantDeclarationAnnotations()) {
                if (areSameByClass(annos.get(i), clazz)) {
                    return annoTrees.get(i);
                }
            }
        }
        return null;
    }

    /** Returns the set of classes of field invariant annotations. */
    protected Set<Class<? extends Annotation>> getFieldInvariantDeclarationAnnotations() {
        return Collections.singleton(FieldInvariant.class);
    }

    /**
     * A callback method for the AnnotatedTypeFactory subtypes to customize
     * AnnotatedTypeMirror.substitute().
     *
     * @param varDecl a declaration of a type variable
     * @param varUse a use of the same type variable
     * @param value the new type to substitute in for the type variable
     */
    public void postTypeVarSubstitution(
            AnnotatedTypeVariable varDecl,
            AnnotatedTypeVariable varUse,
            AnnotatedTypeMirror value) {
        if (!varUse.getAnnotationsField().isEmpty()
                && !AnnotationUtils.areSame(
                        varUse.getAnnotationsField(), varDecl.getAnnotationsField())) {
            value.replaceAnnotations(varUse.getAnnotationsField());
        }
    }

    /**
     * Adapt the upper bounds of the type variables of a class relative to the type instantiation.
     * In some type systems, the upper bounds depend on the instantiation of the class. For example,
     * in the Generic Universe Type system, consider a class declaration
     *
     * <pre>{@code   class C<X extends @Peer Object> }</pre>
     *
     * then the instantiation
     *
     * <pre>{@code   @Rep C<@Rep Object> }</pre>
     *
     * is legal. The upper bounds of class C have to be adapted by the main modifier.
     *
     * <p>An example of an adaptation follows. Suppose, I have a declaration:
     *
     * <pre>{@code  class MyClass<E extends List<E>>}</pre>
     *
     * And an instantiation:
     *
     * <pre>{@code  new MyClass<@NonNull String>()}</pre>
     *
     * <p>The upper bound of E adapted to the argument String, would be {@code List<@NonNull
     * String>} and the lower bound would be an AnnotatedNullType.
     *
     * <p>TODO: ensure that this method is consistently used instead of directly querying the type
     * variables.
     *
     * @param type the use of the type
     * @param element the corresponding element
     * @return the adapted bounds of the type parameters
     */
    public List<AnnotatedTypeParameterBounds> typeVariablesFromUse(
            AnnotatedDeclaredType type, TypeElement element) {

        AnnotatedDeclaredType generic = getAnnotatedType(element);
        List<AnnotatedTypeMirror> targs = type.getTypeArguments();
        List<AnnotatedTypeMirror> tvars = generic.getTypeArguments();

        assert targs.size() == tvars.size()
                : "Mismatch in type argument size between " + type + " and " + generic;

        // System.err.printf("TVFU%n  type: %s%n  generic: %s%n", type, generic);

        Map<TypeVariable, AnnotatedTypeMirror> mapping = new HashMap<>();

        AnnotatedDeclaredType enclosing = type;
        while (enclosing != null) {
            List<AnnotatedTypeMirror> enclosingTArgs = enclosing.getTypeArguments();
            AnnotatedDeclaredType declaredType =
                    getAnnotatedType((TypeElement) enclosing.getUnderlyingType().asElement());
            List<AnnotatedTypeMirror> enclosingTVars = declaredType.getTypeArguments();
            for (int i = 0; i < enclosingTArgs.size(); i++) {
                AnnotatedTypeVariable enclosingTVar = (AnnotatedTypeVariable) enclosingTVars.get(i);
                mapping.put(enclosingTVar.getUnderlyingType(), enclosingTArgs.get(i));
            }
            enclosing = enclosing.getEnclosingType();
        }

        List<AnnotatedTypeParameterBounds> res = new ArrayList<>(tvars.size());

        for (AnnotatedTypeMirror atm : tvars) {
            AnnotatedTypeVariable atv = (AnnotatedTypeVariable) atm;
            AnnotatedTypeMirror upper = typeVarSubstitutor.substitute(mapping, atv.getUpperBound());
            AnnotatedTypeMirror lower = typeVarSubstitutor.substitute(mapping, atv.getLowerBound());
            res.add(new AnnotatedTypeParameterBounds(upper, lower));
        }
        return res;
    }

    /**
     * Creates and returns an AnnotatedNullType qualified with {@code annotations}.
     *
     * @param annotations set of AnnotationMirrors to qualify the returned type with
     * @return AnnotatedNullType qualified with {@code annotations}
     */
    public AnnotatedNullType getAnnotatedNullType(Set<? extends AnnotationMirror> annotations) {
        final AnnotatedTypeMirror.AnnotatedNullType nullType =
                (AnnotatedNullType)
                        toAnnotatedType(processingEnv.getTypeUtils().getNullType(), false);
        nullType.addAnnotations(annotations);
        return nullType;
    }

    // **********************************************************************
    // Utilities method for getting specific types from trees or elements
    // **********************************************************************

    /**
     * Return the implicit receiver type of an expression tree.
     *
     * <p>The result is null for expressions that don't have a receiver, e.g. for a local variable
     * or method parameter access.
     *
     * <p>Clients should generally call {@link #getReceiverType}.
     *
     * @param tree the expression that might have an implicit receiver
     * @return the type of the receiver
     */
    /*
     * TODO: receiver annotations on outer this.
     * TODO: Better document the difference between getImplicitReceiverType and getSelfType?
     * TODO: this method assumes that the tree is within the current
     * Compilation Unit. This assumption fails in testcase Bug109_A/B, where
     * a chain of dependencies leads into a different compilation unit.
     * I didn't find a way how to handle this better and conservatively
     * return null. See TODO comment below.
     *
     */
    protected AnnotatedDeclaredType getImplicitReceiverType(ExpressionTree tree) {
        assert (tree.getKind() == Tree.Kind.IDENTIFIER
                        || tree.getKind() == Tree.Kind.MEMBER_SELECT
                        || tree.getKind() == Tree.Kind.METHOD_INVOCATION
                        || tree.getKind() == Tree.Kind.NEW_CLASS)
                : "Unexpected tree kind: " + tree.getKind();

        Element element = TreeUtils.elementFromTree(tree);
        assert element != null : "Unexpected null element for tree: " + tree;
        // Return null if the element kind has no receiver.
        if (!ElementUtils.hasReceiver(element)) {
            return null;
        }

        ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
        if (receiver == null) {
            if (isMostEnclosingThisDeref(tree)) {
                // TODO: problem with ambiguity with implicit receivers.
                // We need a way to find the correct class. We cannot use the
                // element, as generics might have to be substituted in a subclass.
                // See GenericsEnclosing test case.
                // TODO: is this fixed?
                return getSelfType(tree);
            } else {
                TreePath path = getPath(tree);
                if (path == null) {
                    // The path is null if the field is in a compilation unit we haven't
                    // processed yet. TODO: is there a better way?
                    return null;
                }
                TypeElement typeElt = ElementUtils.enclosingClass(element);
                if (typeElt == null) {
                    throw new BugInCF(
                            "AnnotatedTypeFactory.getImplicitReceiver: enclosingClass()==null for element: "
                                    + element);
                }
                if (tree.getKind() == Kind.NEW_CLASS) {
                    if (typeElt.getEnclosingElement() != null) {
                        typeElt = ElementUtils.enclosingClass(typeElt.getEnclosingElement());
                    } else {
                        typeElt = null;
                    }
                    if (typeElt == null) {
                        // If the typeElt does not have an enclosing class, then the NewClassTree
                        // does not have an implicit receiver.
                        return null;
                    }
                }
                // TODO: method receiver annotations on outer this
                return getEnclosingType(typeElt, tree);
            }
        }

        Element rcvelem = TreeUtils.elementFromTree(receiver);
        assert rcvelem != null : "Unexpected null element for receiver: " + receiver;

        if (!ElementUtils.hasReceiver(rcvelem)) {
            return null;
        }

        if (receiver.getKind() == Tree.Kind.IDENTIFIER
                && ((IdentifierTree) receiver).getName().contentEquals("this")) {
            // TODO: also "super"?
            return this.getSelfType(tree);
        }

        TypeElement typeElt = ElementUtils.enclosingClass(rcvelem);
        if (typeElt == null) {
            throw new BugInCF(
                    "AnnotatedTypeFactory.getImplicitReceiver: enclosingClass()==null for element: "
                            + rcvelem);
        }

        AnnotatedDeclaredType type = getAnnotatedType(typeElt);

        // TODO: go through _all_ enclosing methods to see whether any of them has a
        // receiver annotation of the correct type.
        // TODO: Can we reuse getSelfType for outer this accesses?

        AnnotatedDeclaredType methodReceiver = getCurrentMethodReceiver(tree);
        if (shouldTakeFromReceiver(methodReceiver)) {
            // TODO: this only takes the main annotations.
            // What about other annotations (annotations on the type argument, outer types, ...)
            type.clearAnnotations();
            type.addAnnotations(methodReceiver.getAnnotations());
        }

        return type;
    }

    // Determine whether we should take annotations from the given method receiver.
    private boolean shouldTakeFromReceiver(AnnotatedDeclaredType methodReceiver) {
        return methodReceiver != null;
    }

    /**
     * Determine whether the tree dereferences the most enclosing "this" object. That is, we have an
     * expression like "f.g" and want to know whether it is an access "this.f.g". Returns false if f
     * is a field of an outer class or f is a local variable.
     *
     * @param tree the tree to check
     * @return true, iff the tree is an explicit or implicit reference to the most enclosing "this"
     */
    public final boolean isMostEnclosingThisDeref(ExpressionTree tree) {
        if (!isAnyEnclosingThisDeref(tree)) {
            return false;
        }

        Element element = TreeUtils.elementFromUse(tree);
        TypeElement typeElt = ElementUtils.enclosingClass(element);

        ClassTree enclosingClass = getCurrentClassTree(tree);
        if (enclosingClass != null
                && isSubtype(TreeUtils.elementFromDeclaration(enclosingClass), typeElt)) {
            return true;
        }

        // ran out of options
        return false;
    }

    /**
     * Does this expression have (the innermost or an outer) "this" as receiver? Note that the
     * receiver can be either explicit or implicit.
     *
     * @param tree the tree to test
     * @return true, iff the expression uses (the innermost or an outer) "this" as receiver
     */
    public final boolean isAnyEnclosingThisDeref(ExpressionTree tree) {
        if (!TreeUtils.isUseOfElement(tree)) {
            return false;
        }
        ExpressionTree recv = TreeUtils.getReceiverTree(tree);

        if (recv == null) {
            Element element = TreeUtils.elementFromUse(tree);

            if (!ElementUtils.hasReceiver(element)) {
                return false;
            }

            tree = TreeUtils.withoutParens(tree);

            if (tree.getKind() == Tree.Kind.IDENTIFIER) {
                Name n = ((IdentifierTree) tree).getName();
                if ("this".contentEquals(n) || "super".contentEquals(n)) {
                    // An explicit reference to "this"/"super" has no receiver.
                    return false;
                }
            }
            // Must be some access through this.
            return true;
        } else if (!TreeUtils.isUseOfElement(recv)) {
            // The receiver is e.g. a String literal.
            return false;
            // TODO: I think this:
            //  (i==9 ? this : this).toString();
            // is not a use of an element, as the receiver is an
            // expression. How should this be handled?
        }

        Element element = TreeUtils.elementFromUse(recv);

        if (!ElementUtils.hasReceiver(element)) {
            return false;
        }

        return TreeUtils.isExplicitThisDereference(recv);
    }

    /**
     * Returns the type of {@code this} in the given location, which can be used if {@code this} has
     * a special semantics (e.g. {@code this} is non-null).
     *
     * <p>The parameter is an arbitrary tree and does not have to mention "this", neither explicitly
     * nor implicitly. This method should be overridden for type-system specific behavior.
     *
     * <p>TODO: in 1.8.2, handle all receiver type annotations. TODO: handle enclosing classes
     * correctly.
     */
    public AnnotatedDeclaredType getSelfType(Tree tree) {
        if (TreeUtils.isClassTree(tree)) {
            return getAnnotatedType(TreeUtils.elementFromDeclaration((ClassTree) tree));
        }
        TreePath path = getPath(tree);
        ClassTree enclosingClass = TreeUtils.enclosingClass(path);
        if (enclosingClass == null) {
            // I hope this only happens when tree is a fake tree that
            // we created, e.g. when desugaring enhanced-for-loops.
            enclosingClass = getCurrentClassTree(tree);
        }
        AnnotatedDeclaredType type = getAnnotatedType(enclosingClass);

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(path);
        if (enclosingClass.getSimpleName().length() != 0 && enclosingMethod != null) {
            AnnotatedDeclaredType methodReceiver;
            if (TreeUtils.isConstructor(enclosingMethod)) {
                // The type of `this` in a constructor is usually the constructor return type.
                // Certain type systems, in particular the Initialization Checker, need custom
                // logic.
                methodReceiver =
                        (AnnotatedDeclaredType) getAnnotatedType(enclosingMethod).getReturnType();
            } else {
                methodReceiver = getAnnotatedType(enclosingMethod).getReceiverType();
            }
            if (shouldTakeFromReceiver(methodReceiver)) {
                // TODO  what about all annotations on the receiver?
                // Code is also duplicated above.
                type.clearAnnotations();
                type.addAnnotations(methodReceiver.getAnnotations());
            }
        }
        return type;
    }

    /**
     * Determine the type of the most enclosing class of the given tree that is a subtype of the
     * given element. Receiver type annotations of an enclosing method are considered, similarly
     * return type annotations of an enclosing constructor.
     */
    public AnnotatedDeclaredType getEnclosingType(TypeElement element, Tree tree) {
        Element enclosingElt = getMostInnerClassOrMethod(tree);

        while (enclosingElt != null) {
            if (enclosingElt instanceof ExecutableElement) {
                ExecutableElement method = (ExecutableElement) enclosingElt;
                if (method.asType() != null // XXX: hack due to a compiler bug
                        && isSubtype((TypeElement) method.getEnclosingElement(), element)) {
                    if (ElementUtils.isStatic(method)) {
                        // Static methods should use type of enclosing class,
                        // by simply taking another turn in the loop.
                    } else if (method.getKind() == ElementKind.CONSTRUCTOR) {
                        // return getSelfType(this.declarationFromElement(method));
                        return (AnnotatedDeclaredType) getAnnotatedType(method).getReturnType();
                    } else {
                        return getAnnotatedType(method).getReceiverType();
                    }
                }
            } else if (enclosingElt instanceof TypeElement) {
                if (isSubtype((TypeElement) enclosingElt, element)) {
                    return (AnnotatedDeclaredType) getAnnotatedType(enclosingElt);
                }
            }
            enclosingElt = enclosingElt.getEnclosingElement();
        }
        return null;
    }

    private boolean isSubtype(TypeElement a1, TypeElement a2) {
        return (a1.equals(a2)
                || types.isSubtype(types.erasure(a1.asType()), types.erasure(a2.asType())));
    }

    /**
     * Returns the receiver type of the expression tree, or null if it does not exist.
     *
     * <p>The only trees that could potentially have a receiver are:
     *
     * <ul>
     *   <li>Array Access
     *   <li>Identifiers (whose receivers are usually self type)
     *   <li>Method Invocation Trees
     *   <li>Member Select Trees
     * </ul>
     *
     * @param expression the expression for which to determine the receiver type
     * @return the type of the receiver of this expression
     */
    public final AnnotatedTypeMirror getReceiverType(ExpressionTree expression) {
        if (this.isAnyEnclosingThisDeref(expression)) {
            return getImplicitReceiverType(expression);
        }

        ExpressionTree receiver = TreeUtils.getReceiverTree(expression);
        if (receiver != null) {
            return getAnnotatedType(receiver);
        } else {
            // E.g. local variables
            return null;
        }
    }

    /** The type for an instantiated generic method or constructor. */
    public static class ParameterizedExecutableType {
        /** The method's/constructor's type. */
        public final AnnotatedExecutableType executableType;
        /** The types of the generic type arguments. */
        public final List<AnnotatedTypeMirror> typeArgs;
        /** Create a ParameterizedExecutableType. */
        public ParameterizedExecutableType(
                AnnotatedExecutableType executableType, List<AnnotatedTypeMirror> typeArgs) {
            this.executableType = executableType;
            this.typeArgs = typeArgs;
        }
    }

    /**
     * Determines the type of the invoked method based on the passed method invocation tree.
     *
     * <p>The returned method type has all type variables resolved, whether based on receiver type,
     * passed type parameters if any, and method invocation parameter.
     *
     * <p>Subclasses may override this method to customize inference of types or qualifiers based on
     * method invocation parameters.
     *
     * <p>As an implementation detail, this method depends on {@link
     * AnnotatedTypes#asMemberOf(Types, AnnotatedTypeFactory, AnnotatedTypeMirror, Element)}, and
     * customization based on receiver type should be in accordance to its specification.
     *
     * <p>The return type is a pair of the type of the invoked method and the (inferred) type
     * arguments. Note that neither the explicitly passed nor the inferred type arguments are
     * guaranteed to be subtypes of the corresponding upper bounds. See method {@link
     * org.checkerframework.common.basetype.BaseTypeVisitor#checkTypeArguments} for the checks of
     * type argument well-formedness.
     *
     * <p>Note that "this" and "super" constructor invocations are also handled by this method
     * (explicit or implicit ones, at the beginning of a constructor). Method {@link
     * #constructorFromUse(NewClassTree)} is only used for a constructor invocation in a "new"
     * expression.
     *
     * @param tree the method invocation tree
     * @return the method type being invoked with tree and the (inferred) type arguments
     */
    public ParameterizedExecutableType methodFromUse(MethodInvocationTree tree) {
        ExecutableElement methodElt = TreeUtils.elementFromUse(tree);
        AnnotatedTypeMirror receiverType = getReceiverType(tree);

        ParameterizedExecutableType mType = methodFromUse(tree, methodElt, receiverType);
        if (checker.shouldResolveReflection()
                && reflectionResolver.isReflectiveMethodInvocation(tree)) {
            mType = reflectionResolver.resolveReflectiveCall(this, tree, mType);
        }

        AnnotatedExecutableType method = mType.executableType;
        if (method.getReturnType().getKind() == TypeKind.WILDCARD
                && ((AnnotatedWildcardType) method.getReturnType()).isUninferredTypeArgument()) {
            // Get the correct Java type from the tree and use it as the upper bound of the
            // wildcard.
            TypeMirror tm = TreeUtils.typeOf(tree);
            AnnotatedTypeMirror t = toAnnotatedType(tm, false);

            AnnotatedWildcardType wildcard = (AnnotatedWildcardType) method.getReturnType();
            if (ignoreUninferredTypeArguments) {
                // remove the annotations so that default annotations are used instead.
                // (See call to addDefaultAnnotations below.)
                t.clearAnnotations();
            } else {
                t.replaceAnnotations(wildcard.getExtendsBound().getAnnotations());
            }
            wildcard.setExtendsBound(t);
            addDefaultAnnotations(wildcard);
        }

        return mType;
    }

    /**
     * Determines the type of the invoked method based on the passed expression tree, executable
     * element, and receiver type.
     *
     * @param tree either a MethodInvocationTree or a MemberReferenceTree
     * @param methodElt the element of the referenced method
     * @param receiverType the type of the receiver
     * @return the method type being invoked with tree and the (inferred) type arguments
     * @see #methodFromUse(MethodInvocationTree)
     */
    public ParameterizedExecutableType methodFromUse(
            ExpressionTree tree, ExecutableElement methodElt, AnnotatedTypeMirror receiverType) {

        AnnotatedExecutableType memberType = getAnnotatedType(methodElt); // get unsubstituted type
        methodFromUsePreSubstitution(tree, memberType);

        AnnotatedExecutableType methodType =
                AnnotatedTypes.asMemberOf(types, this, receiverType, methodElt, memberType);
        List<AnnotatedTypeMirror> typeargs = new ArrayList<>(methodType.getTypeVariables().size());

        Map<TypeVariable, AnnotatedTypeMirror> typeVarMapping =
                AnnotatedTypes.findTypeArguments(processingEnv, this, tree, methodElt, methodType);

        if (!typeVarMapping.isEmpty()) {
            for (AnnotatedTypeVariable tv : methodType.getTypeVariables()) {
                if (typeVarMapping.get(tv.getUnderlyingType()) == null) {
                    throw new BugInCF(
                            "AnnotatedTypeFactory.methodFromUse:"
                                    + "mismatch between declared method type variables and the inferred method type arguments. "
                                    + "Method type variables: "
                                    + methodType.getTypeVariables()
                                    + "; "
                                    + "Inferred method type arguments: "
                                    + typeVarMapping);
                }
                typeargs.add(typeVarMapping.get(tv.getUnderlyingType()));
            }
            methodType =
                    (AnnotatedExecutableType)
                            typeVarSubstitutor.substitute(typeVarMapping, methodType);
        }

        if (tree.getKind() == Tree.Kind.METHOD_INVOCATION
                && TreeUtils.isMethodInvocation(tree, objectGetClass, processingEnv)) {
            adaptGetClassReturnTypeToReceiver(methodType, receiverType);
        }

        return new ParameterizedExecutableType(methodType, typeargs);
    }

    /**
     * A callback method for the AnnotatedTypeFactory subtypes to customize the handling of the
     * declared method type before type variable substitution.
     *
     * @param tree either a method invocation or a member reference tree
     * @param type declared method type before type variable substitution
     */
    protected void methodFromUsePreSubstitution(ExpressionTree tree, AnnotatedExecutableType type) {
        assert tree instanceof MethodInvocationTree || tree instanceof MemberReferenceTree;
    }

    /**
     * Java special-cases the return type of {@link java.lang.Class#getClass() getClass()}. Though
     * the method has a return type of {@code Class<?>}, the compiler special cases this return-type
     * and changes the bound of the type argument to the erasure of the receiver type. For example:
     *
     * <ul>
     *   <li>{@code x.getClass()} has the type {@code Class< ? extends erasure_of_x >}
     *   <li>{@code someInteger.getClass()} has the type {@code Class< ? extends Integer >}
     * </ul>
     *
     * @param getClassType this must be a type representing a call to Object.getClass otherwise a
     *     runtime exception will be thrown. It is modified by side effect.
     * @param receiverType the receiver type of the method invocation (not the declared receiver
     *     type)
     */
    protected void adaptGetClassReturnTypeToReceiver(
            final AnnotatedExecutableType getClassType, final AnnotatedTypeMirror receiverType) {
        // TODO: should the receiver type ever be a declaration??
        // Work on removing the asUse() call.
        final AnnotatedTypeMirror newBound = receiverType.getErased().asUse();

        final AnnotatedTypeMirror returnType = getClassType.getReturnType();
        if (returnType == null
                || !(returnType.getKind() == TypeKind.DECLARED)
                || ((AnnotatedDeclaredType) returnType).getTypeArguments().size() != 1) {
            throw new BugInCF(
                    "Unexpected type passed to AnnotatedTypes.adaptGetClassReturnTypeToReceiver%n"
                            + "getClassType=%s%nreceiverType=%s",
                    getClassType, receiverType);
        }

        final AnnotatedDeclaredType returnAdt =
                (AnnotatedDeclaredType) getClassType.getReturnType();
        final List<AnnotatedTypeMirror> typeArgs = returnAdt.getTypeArguments();

        // Usually, the only locations that will add annotations to the return type are getClass in
        // stub files defaults and propagation tree annotator.  Since getClass is final they cannot
        // come from source code.  Also, since the newBound is an erased type we have no type
        // arguments.  So, we just copy the annotations from the bound of the declared type to the
        // new bound.
        final AnnotatedWildcardType classWildcardArg = (AnnotatedWildcardType) typeArgs.get(0);
        Set<AnnotationMirror> newAnnos = AnnotationUtils.createAnnotationSet();
        Set<AnnotationMirror> typeBoundAnnos =
                getTypeDeclarationBounds(newBound.getUnderlyingType());
        Set<AnnotationMirror> wildcardBoundAnnos =
                classWildcardArg.getExtendsBound().getAnnotations();
        for (AnnotationMirror typeBoundAnno : typeBoundAnnos) {
            AnnotationMirror wildcardAnno =
                    qualHierarchy.findAnnotationInSameHierarchy(wildcardBoundAnnos, typeBoundAnno);
            if (qualHierarchy.isSubtype(typeBoundAnno, wildcardAnno)) {
                newAnnos.add(typeBoundAnno);
            } else {
                newAnnos.add(wildcardAnno);
            }
        }
        newBound.replaceAnnotations(newAnnos);

        classWildcardArg.setExtendsBound(newBound);
    }

    /**
     * Determines the type of the invoked constructor based on the passed new class tree.
     *
     * <p>The returned method type has all type variables resolved, whether based on receiver type,
     * passed type parameters if any, and constructor invocation parameter.
     *
     * <p>Subclasses may override this method to customize inference of types or qualifiers based on
     * constructor invocation parameters.
     *
     * <p>As an implementation detail, this method depends on {@link
     * AnnotatedTypes#asMemberOf(Types, AnnotatedTypeFactory, AnnotatedTypeMirror, Element)}, and
     * customization based on receiver type should be in accordance with its specification.
     *
     * <p>The return type is a pair of the type of the invoked constructor and the (inferred) type
     * arguments. Note that neither the explicitly passed nor the inferred type arguments are
     * guaranteed to be subtypes of the corresponding upper bounds. See method {@link
     * org.checkerframework.common.basetype.BaseTypeVisitor#checkTypeArguments} for the checks of
     * type argument well-formedness.
     *
     * <p>Note that "this" and "super" constructor invocations are handled by method {@link
     * #methodFromUse}. This method only handles constructor invocations in a "new" expression.
     *
     * @param tree the constructor invocation tree
     * @return the annotated type of the invoked constructor (as an executable type) and the
     *     (inferred) type arguments
     */
    public ParameterizedExecutableType constructorFromUse(NewClassTree tree) {
        ExecutableElement ctor = TreeUtils.constructor(tree);
        AnnotatedTypeMirror type = fromNewClass(tree);
        addComputedTypeAnnotations(tree, type);
        AnnotatedExecutableType con = getAnnotatedType(ctor); // get unsubstituted type
        constructorFromUsePreSubstitution(tree, con);

        con = AnnotatedTypes.asMemberOf(types, this, type, ctor, con);

        if (tree.getArguments().size() == con.getParameterTypes().size() + 1
                && isSyntheticArgument(tree.getArguments().get(0))) {
            // happens for anonymous constructors of inner classes
            List<AnnotatedTypeMirror> actualParams = new ArrayList<>();
            actualParams.add(getAnnotatedType(tree.getArguments().get(0)));
            actualParams.addAll(con.getParameterTypes());
            con.setParameterTypes(actualParams);
        }

        List<AnnotatedTypeMirror> typeargs = new ArrayList<>(con.getTypeVariables().size());

        Map<TypeVariable, AnnotatedTypeMirror> typeVarMapping =
                AnnotatedTypes.findTypeArguments(processingEnv, this, tree, ctor, con);

        if (!typeVarMapping.isEmpty()) {
            for (AnnotatedTypeVariable tv : con.getTypeVariables()) {
                typeargs.add(typeVarMapping.get(tv.getUnderlyingType()));
            }
            con = (AnnotatedExecutableType) typeVarSubstitutor.substitute(typeVarMapping, con);
        }

        return new ParameterizedExecutableType(con, typeargs);
    }

    /**
     * A callback method for the AnnotatedTypeFactory subtypes to customize the handling of the
     * declared constructor type before type variable substitution.
     *
     * @param tree a NewClassTree from constructorFromUse()
     * @param type declared method type before type variable substitution
     */
    protected void constructorFromUsePreSubstitution(
            NewClassTree tree, AnnotatedExecutableType type) {}

    /**
     * Returns the return type of the method {@code m}.
     *
     * @param m a tree of method
     * @return the return type of the method
     */
    public AnnotatedTypeMirror getMethodReturnType(MethodTree m) {
        AnnotatedExecutableType methodType = getAnnotatedType(m);
        AnnotatedTypeMirror ret = methodType.getReturnType();
        return ret;
    }

    /** Returns the return type of the method {@code m} at the return statement {@code r}. */
    public AnnotatedTypeMirror getMethodReturnType(MethodTree m, ReturnTree r) {
        AnnotatedExecutableType methodType = getAnnotatedType(m);
        AnnotatedTypeMirror ret = methodType.getReturnType();
        return ret;
    }

    private boolean isSyntheticArgument(Tree tree) {
        return tree.toString().contains("<*nullchk*>");
    }

    /**
     * Creates an AnnotatedDeclaredType for a NewClassTree. Only adds explicit annotations, unless
     * newClassTree has a diamond operator. In that case, the annotations on the type arguments are
     * inferred using the assignment context and contain defaults.
     *
     * <p>Also, fully annotates the enclosing type of the returned declared type.
     *
     * <p>(Subclass beside {@link GenericAnnotatedTypeFactory} should not override this method.)
     *
     * @param newClassTree NewClassTree
     * @return AnnotatedDeclaredType
     */
    public AnnotatedDeclaredType fromNewClass(NewClassTree newClassTree) {

        AnnotatedDeclaredType enclosingType;
        if (newClassTree.getEnclosingExpression() != null) {
            enclosingType = (AnnotatedDeclaredType) getReceiverType(newClassTree);
        } else {
            enclosingType = getImplicitReceiverType(newClassTree);
        }
        // Diamond trees that are not anonymous classes.
        if (TreeUtils.isDiamondTree(newClassTree) && newClassTree.getClassBody() == null) {
            AnnotatedDeclaredType type =
                    (AnnotatedDeclaredType) toAnnotatedType(TreeUtils.typeOf(newClassTree), false);
            if (((com.sun.tools.javac.code.Type) type.actualType)
                    .tsym
                    .getTypeParameters()
                    .nonEmpty()) {
                Pair<Tree, AnnotatedTypeMirror> ctx = this.visitorState.getAssignmentContext();
                if (ctx != null) {
                    AnnotatedTypeMirror ctxtype = ctx.second;
                    fromNewClassContextHelper(type, ctxtype);
                } else {
                    TreePath p = getPath(newClassTree);
                    AnnotatedTypeMirror ctxtype = TypeArgInferenceUtil.assignedTo(this, p);
                    if (ctxtype != null) {
                        fromNewClassContextHelper(type, ctxtype);
                    } else {
                        // give up trying and set to raw.
                        type.setWasRaw();
                    }
                }
            }
            AnnotatedDeclaredType fromTypeTree =
                    (AnnotatedDeclaredType)
                            TypeFromTree.fromTypeTree(this, newClassTree.getIdentifier());
            type.replaceAnnotations(fromTypeTree.getAnnotations());
            type.setEnclosingType(enclosingType);
            return type;
        } else if (newClassTree.getClassBody() != null) {
            AnnotatedDeclaredType type =
                    (AnnotatedDeclaredType) toAnnotatedType(TreeUtils.typeOf(newClassTree), false);
            // If newClassTree creates an anonymous class, then annotations in this location:
            //   new @HERE Class() {}
            // are on not on the identifier newClassTree, but rather on the modifier newClassTree.
            List<? extends AnnotationTree> annos =
                    newClassTree.getClassBody().getModifiers().getAnnotations();
            type.addAnnotations(TreeUtils.annotationsFromTypeAnnotationTrees(annos));
            type.setEnclosingType(enclosingType);
            return type;
        } else {
            // If newClassTree does not create anonymous class,
            // newClassTree.getIdentifier includes the explicit annotations in this location:
            //   new @HERE Class()
            AnnotatedDeclaredType type =
                    (AnnotatedDeclaredType)
                            TypeFromTree.fromTypeTree(this, newClassTree.getIdentifier());
            type.setEnclosingType(enclosingType);
            return type;
        }
    }

    // This method extracts the ugly hacky parts.
    // This method should be rewritten and in particular diamonds should be
    // implemented cleanly.
    // See Issue 289.
    private void fromNewClassContextHelper(
            AnnotatedDeclaredType type, AnnotatedTypeMirror ctxtype) {
        switch (ctxtype.getKind()) {
            case DECLARED:
                AnnotatedDeclaredType adctx = (AnnotatedDeclaredType) ctxtype;

                if (type.getTypeArguments().size() == adctx.getTypeArguments().size()) {
                    // Try to simply take the type arguments from LHS.
                    List<AnnotatedTypeMirror> oldArgs = type.getTypeArguments();
                    List<AnnotatedTypeMirror> newArgs = adctx.getTypeArguments();
                    for (int i = 0; i < type.getTypeArguments().size(); ++i) {
                        if (!types.isSubtype(
                                newArgs.get(i).actualType, oldArgs.get(i).actualType)) {
                            // One of the underlying types doesn't match. Give up.
                            return;
                        }
                    }

                    type.setTypeArguments(newArgs);

                    /* It would be nice to call isSubtype for a basic sanity check.
                     * However, the type might not have been completely initialized yet,
                     * so isSubtype might fail.
                     *
                    if (!typeHierarchy.isSubtype(type, ctxtype)) {
                        // Simply taking the newArgs didn't result in a valid subtype.
                        // Give up and simply use the inferred types.
                        type.setTypeArguments(oldArgs);
                    }
                    */
                } else {
                    // TODO: Find a way to determine annotated type arguments.
                    // Look at what Attr and Resolve are doing and rework this whole method.
                }
                break;

            case ARRAY:
                // This new class is in the initializer of an array.
                // The array being created can't have a generic component type,
                // so nothing to be done.
                break;
            case TYPEVAR:
                // TODO: this should NOT be necessary.
                // org.checkerframework.dataflow.cfg.node.MethodAccessNode.MethodAccessNode(ExpressionTree, Node)
                // Uses an ExecutableElement, which did not substitute type variables.
                break;
            case WILDCARD:
                // TODO: look at bounds of wildcard and see whether we can improve.
                break;
            default:
                if (ctxtype.getKind().isPrimitive()) {
                    // See Issue 438. Ignore primitive types for diamond inference - a primitive
                    // type is never a suitable context anyway.
                } else {
                    throw new BugInCF(
                            "AnnotatedTypeFactory.fromNewClassContextHelper: unexpected context: "
                                    + ctxtype
                                    + " ("
                                    + ctxtype.getKind()
                                    + ")");
                }
        }
    }

    /**
     * Returns the annotated boxed type of the given primitive type. The returned type would only
     * have the annotations on the given type.
     *
     * <p>Subclasses may override this method safely to override this behavior.
     *
     * @param type the primitive type
     * @return the boxed declared type of the passed primitive type
     */
    public AnnotatedDeclaredType getBoxedType(AnnotatedPrimitiveType type) {
        TypeElement typeElt = types.boxedClass(type.getUnderlyingType());
        AnnotatedDeclaredType dt = fromElement(typeElt);
        dt.addAnnotations(type.getAnnotations());
        return dt;
    }

    /**
     * Returns the annotated primitive type of the given declared type if it is a boxed declared
     * type. Otherwise, it throws <i>IllegalArgumentException</i> exception.
     *
     * <p>The returned type has the same primary annotations as the given type.
     *
     * @param type the declared type
     * @return the unboxed primitive type
     * @throws IllegalArgumentException if the type given has no unbox conversion
     */
    public AnnotatedPrimitiveType getUnboxedType(AnnotatedDeclaredType type)
            throws IllegalArgumentException {
        PrimitiveType primitiveType = types.unboxedType(type.getUnderlyingType());
        AnnotatedPrimitiveType pt =
                (AnnotatedPrimitiveType) AnnotatedTypeMirror.createType(primitiveType, this, false);
        pt.addAnnotations(type.getAnnotations());
        return pt;
    }

    /**
     * Returns AnnotatedDeclaredType with underlying type String and annotations copied from type.
     * Subclasses may change the annotations.
     *
     * @param type type to convert to String
     * @return AnnotatedTypeMirror that results from converting type to a String type
     */
    // TODO: Test that this is called in all the correct locations
    // See Issue #715
    // https://github.com/typetools/checker-framework/issues/715
    public AnnotatedDeclaredType getStringType(AnnotatedTypeMirror type) {
        TypeMirror stringTypeMirror = TypesUtils.typeFromClass(String.class, types, elements);
        AnnotatedDeclaredType stringATM =
                (AnnotatedDeclaredType)
                        AnnotatedTypeMirror.createType(
                                stringTypeMirror, this, type.isDeclaration());
        stringATM.addAnnotations(type.getEffectiveAnnotations());
        return stringATM;
    }

    /**
     * Returns AnnotatedPrimitiveType with underlying type {@code narrowedTypeMirror} and
     * annotations copied from {@code type}.
     *
     * <p>Currently this method is called only for primitives that are narrowed at assignments from
     * literal ints, for example, {@code byte b = 1;}. All other narrowing conversions happen at
     * typecasts.
     *
     * @param type type to narrow
     * @param narrowedTypeMirror underlying type for the returned type mirror
     * @return result of converting {@code type} to {@code narrowedTypeMirror}
     */
    public AnnotatedPrimitiveType getNarrowedPrimitive(
            AnnotatedPrimitiveType type, TypeMirror narrowedTypeMirror) {
        AnnotatedPrimitiveType narrowed =
                (AnnotatedPrimitiveType)
                        AnnotatedTypeMirror.createType(
                                narrowedTypeMirror, this, type.isDeclaration());
        narrowed.addAnnotations(type.getAnnotations());
        return narrowed;
    }

    /** Returns the VisitorState instance used by the factory to infer types. */
    public VisitorState getVisitorState() {
        return this.visitorState;
    }

    // **********************************************************************
    // random methods wrapping #getAnnotatedType(Tree) and #fromElement(Tree)
    // with appropriate casts to reduce casts on the client side
    // **********************************************************************

    /**
     * See {@link #getAnnotatedType(Tree)}.
     *
     * @see #getAnnotatedType(Tree)
     */
    public final AnnotatedDeclaredType getAnnotatedType(ClassTree tree) {
        return (AnnotatedDeclaredType) getAnnotatedType((Tree) tree);
    }

    /**
     * See {@link #getAnnotatedType(Tree)}.
     *
     * @see #getAnnotatedType(Tree)
     */
    public final AnnotatedDeclaredType getAnnotatedType(NewClassTree tree) {
        return (AnnotatedDeclaredType) getAnnotatedType((Tree) tree);
    }

    /**
     * See {@link #getAnnotatedType(Tree)}.
     *
     * @see #getAnnotatedType(Tree)
     */
    public final AnnotatedArrayType getAnnotatedType(NewArrayTree tree) {
        return (AnnotatedArrayType) getAnnotatedType((Tree) tree);
    }

    /**
     * See {@link #getAnnotatedType(Tree)}.
     *
     * @see #getAnnotatedType(Tree)
     */
    public final AnnotatedExecutableType getAnnotatedType(MethodTree tree) {
        return (AnnotatedExecutableType) getAnnotatedType((Tree) tree);
    }

    /**
     * See {@link #getAnnotatedType(Element)}.
     *
     * @see #getAnnotatedType(Element)
     */
    public final AnnotatedDeclaredType getAnnotatedType(TypeElement elt) {
        return (AnnotatedDeclaredType) getAnnotatedType((Element) elt);
    }

    /**
     * See {@link #getAnnotatedType(Element)}.
     *
     * @see #getAnnotatedType(Element)
     */
    public final AnnotatedExecutableType getAnnotatedType(ExecutableElement elt) {
        return (AnnotatedExecutableType) getAnnotatedType((Element) elt);
    }

    /**
     * See {@link #fromElement(Element)}.
     *
     * @see #fromElement(Element)
     */
    public final AnnotatedDeclaredType fromElement(TypeElement elt) {
        return (AnnotatedDeclaredType) fromElement((Element) elt);
    }

    /**
     * See {@link #fromElement(Element)}.
     *
     * @see #fromElement(Element)
     */
    public final AnnotatedExecutableType fromElement(ExecutableElement elt) {
        return (AnnotatedExecutableType) fromElement((Element) elt);
    }

    // **********************************************************************
    // Helper methods for this classes
    // **********************************************************************

    /**
     * Determines whether the given annotation is a part of the type system under which this type
     * factory operates. Null is never a supported qualifier; the parameter is nullable to allow the
     * result of canonicalAnnotation to be passed in directly.
     *
     * @param a any annotation
     * @return true if that annotation is part of the type system under which this type factory
     *     operates, false otherwise
     */
    public boolean isSupportedQualifier(@Nullable AnnotationMirror a) {
        if (a == null) {
            return false;
        }
        return isSupportedQualifier(AnnotationUtils.annotationName(a));
    }

    /**
     * Determines whether the given class is a part of the type system under which this type factory
     * operates.
     *
     * @param clazz annotation class
     * @return true if that class is a type qualifier in the type system under which this type
     *     factory operates, false otherwise
     */
    public boolean isSupportedQualifier(Class<? extends Annotation> clazz) {
        return getSupportedTypeQualifiers().contains(clazz);
    }

    /**
     * Determines whether the given class name is a part of the type system under which this type
     * factory operates.
     *
     * @param className fully-qualified annotation class name
     * @return true if that class name is a type qualifier in the type system under which this type
     *     factory operates, false otherwise
     */
    public boolean isSupportedQualifier(String className) {
        return getSupportedTypeQualifierNames().contains(className);
    }

    /**
     * Adds the annotation {@code aliasClass} as an alias for the canonical annotation {@code type}
     * that will be used by the Checker Framework in the alias's place.
     *
     * <p>By specifying the alias/canonical relationship using this method, the elements of the
     * alias are not preserved when the canonical annotation to use is constructed from the alias.
     * If you want the elements to be copied over as well, use {@link #addAliasedAnnotation(Class,
     * Class, boolean, String...)}.
     *
     * @param aliasClass the class of the aliased annotation
     * @param type the canonical annotation
     */
    protected void addAliasedAnnotation(Class<?> aliasClass, AnnotationMirror type) {
        if (getSupportedTypeQualifiers().contains(aliasClass)) {
            throw new BugInCF(
                    "AnnotatedTypeFactory: alias %s should not be in type hierarchy for %s",
                    aliasClass, this.getClass().getSimpleName());
        }
        addAliasedAnnotation(aliasClass.getCanonicalName(), type);
    }

    /**
     * Adds the annotation, whose fully-qualified name is given by {@code aliasName}, as an alias
     * for the canonical annotation {@code type} that will be used by the Checker Framework in the
     * alias's place.
     *
     * <p>Use this method if the alias class is not necessarily on the classpath at Checker
     * Framework compile and run time. Otherwise, use {@link #addAliasedAnnotation(Class,
     * AnnotationMirror)} which prevents the possibility of a typo in the class name.
     *
     * @param aliasName the fully-qualified name of the aliased annotation
     * @param type the canonical annotation
     */
    protected void addAliasedAnnotation(String aliasName, AnnotationMirror type) {
        aliases.put(aliasName, new Alias(aliasName, type, false, null, null));
    }

    /**
     * Adds the annotation {@code aliasClass} as an alias for the canonical annotation {@code type}
     * that will be used by the Checker Framework in the alias's place.
     *
     * <p>You may specify the copyElements flag to indicate whether you want the elements of the
     * alias to be copied over when the canonical annotation is constructed as a copy of {@code
     * type}. Be careful that the framework will try to copy the elements by name matching, so make
     * sure that names and types of the elements to be copied over are exactly the same as the ones
     * in the canonical annotation. Otherwise, an 'Couldn't find element in annotation' error is
     * raised.
     *
     * <p>To facilitate the cases where some of the elements is ignored on purpose when constructing
     * the canonical annotation, this method also provides a varargs {@code ignorableElements} for
     * you to explicitly specify the ignoring rules. For example, {@code
     * org.checkerframework.checker.index.qual.IndexFor} is an alias of {@code
     * org.checkerframework.checker.index.qual.NonNegative}, but the element "value" of
     * {@code @IndexFor} should be ignored when constructing {@code @NonNegative}. In the cases
     * where all elements are ignored, we can simply use {@link #addAliasedAnnotation(Class,
     * AnnotationMirror)} instead.
     *
     * @param aliasClass the class of the aliased annotation
     * @param canonical the canonical annotation
     * @param copyElements a flag that indicates whether you want to copy the elements over when
     *     getting the alias from the canonical annotation
     * @param ignorableElements a list of elements that can be safely dropped when the elements are
     *     being copied over
     */
    protected void addAliasedAnnotation(
            Class<?> aliasClass,
            Class<?> canonical,
            boolean copyElements,
            String... ignorableElements) {
        if (getSupportedTypeQualifiers().contains(aliasClass)) {
            throw new BugInCF(
                    "AnnotatedTypeFactory: alias %s should not be in type hierarchy for %s",
                    aliasClass, this.getClass().getSimpleName());
        }
        addAliasedAnnotation(
                aliasClass.getCanonicalName(), canonical, copyElements, ignorableElements);
    }

    /**
     * Adds the annotation, whose fully-qualified name is given by {@code aliasName}, as an alias
     * for the canonical annotation {@code type} that will be used by the Checker Framework in the
     * alias's place.
     *
     * <p>Use this method if the alias class is not necessarily on the classpath at Checker
     * Framework compile and run time. Otherwise, use {@link #addAliasedAnnotation(Class, Class,
     * boolean, String[])} which prevents the possibility of a typo in the class name.
     *
     * @param aliasName the fully-qualified name of the aliased class
     * @param canonicalName the canonical annotation name
     * @param copyElements a flag that indicates whether we want to copy the elements over when
     *     getting the alias from the canonical annotation
     * @param ignorableElements a list of elements that can be safely dropped when the elements are
     *     being copied over
     */
    protected void addAliasedAnnotation(
            String aliasName,
            Class<?> canonicalName,
            boolean copyElements,
            String... ignorableElements) {
        // The copyElements argument disambiguates overloading.
        if (!copyElements) {
            throw new BugInCF("Do not call with false");
        }
        aliases.put(
                aliasName,
                new Alias(
                        aliasName,
                        null,
                        copyElements,
                        canonicalName.getCanonicalName(),
                        ignorableElements));
    }

    /**
     * Returns the canonical annotation for the passed annotation. Returns null if the passed
     * annotation is not an alias of a canonical one in the framework.
     *
     * <p>A canonical annotation is the internal annotation that will be used by the Checker
     * Framework in the aliased annotation's place.
     *
     * @param a the qualifier to check for an alias
     * @return the canonical annotation, or null if none exists
     */
    public @Nullable AnnotationMirror canonicalAnnotation(AnnotationMirror a) {
        TypeElement elem = (TypeElement) a.getAnnotationType().asElement();
        String qualName = elem.getQualifiedName().toString();
        Alias alias = aliases.get(qualName);
        if (alias == null) {
            return null;
        }
        if (alias.copyElements) {
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, alias.canonicalName);
            builder.copyElementValuesFromAnnotation(a, alias.ignorableElements);
            return builder.build();
        } else {
            return alias.canonical;
        }
    }

    /**
     * Add the annotation {@code alias} as an alias for the declaration annotation {@code
     * annotation}, where the annotation mirror {@code annotationToUse} will be used instead. If
     * multiple calls are made with the same {@code annotation}, then the {@code annotationToUse}
     * must be the same.
     *
     * <p>The point of {@code annotationToUse} is that it may include elements/fields.
     */
    protected void addAliasedDeclAnnotation(
            Class<? extends Annotation> alias,
            Class<? extends Annotation> annotation,
            AnnotationMirror annotationToUse) {
        Pair<AnnotationMirror, Set<Class<? extends Annotation>>> pair = declAliases.get(annotation);
        if (pair != null) {
            if (!AnnotationUtils.areSame(annotationToUse, pair.first)) {
                throw new BugInCF(
                        "annotationToUse should be the same: %s %s", pair.first, annotationToUse);
            }
        } else {
            pair = Pair.of(annotationToUse, new HashSet<>());
            declAliases.put(annotation, pair);
        }
        Set<Class<? extends Annotation>> aliases = pair.second;
        aliases.add(alias);
    }

    /**
     * Adds the annotation {@code annotation} in the set of declaration annotations that should be
     * inherited. A declaration annotation will be inherited if it is in this list, or if it has the
     * meta-annotation @InheritedAnnotation. The meta-annotation @InheritedAnnotation should be used
     * instead of this method, if possible.
     */
    protected void addInheritedAnnotation(AnnotationMirror annotation) {
        inheritedAnnotations.add(annotation);
    }

    /**
     * A convenience method that converts a {@link TypeMirror} to an empty {@link
     * AnnotatedTypeMirror} using {@link AnnotatedTypeMirror#createType}.
     *
     * @param t the {@link TypeMirror}
     * @param declaration true if the result should be marked as a type declaration
     * @return an {@link AnnotatedTypeMirror} that has {@code t} as its underlying type
     */
    protected final AnnotatedTypeMirror toAnnotatedType(TypeMirror t, boolean declaration) {
        return AnnotatedTypeMirror.createType(t, this, declaration);
    }

    /**
     * Determines an empty annotated type of the given tree. In other words, finds the {@link
     * TypeMirror} for the tree and converts that into an {@link AnnotatedTypeMirror}, but does not
     * add any annotations to the result.
     *
     * <p>Most users will want to use {@link #getAnnotatedType(Tree)} instead; this method is mostly
     * for internal use.
     *
     * @param node the tree to analyze
     * @return the type of {@code node}, without any annotations
     */
    protected final AnnotatedTypeMirror type(Tree node) {
        boolean isDeclaration = TreeUtils.isTypeDeclaration(node);

        // Attempt to obtain the type via JCTree.
        if (TreeUtils.typeOf(node) != null) {
            AnnotatedTypeMirror result = toAnnotatedType(TreeUtils.typeOf(node), isDeclaration);
            return result;
        }

        // Attempt to obtain the type via TreePath (slower).
        TreePath path = this.getPath(node);
        assert path != null : "No path or type in tree: " + node;

        TypeMirror t = trees.getTypeMirror(path);
        assert validType(t) : "Invalid type " + t + " for node " + t;

        return toAnnotatedType(t, isDeclaration);
    }

    /**
     * Gets the declaration tree for the element, if the source is available.
     *
     * <p>TODO: would be nice to move this to InternalUtils/TreeUtils.
     *
     * @param elt an element
     * @return the tree declaration of the element if found
     */
    public final Tree declarationFromElement(Element elt) {
        // if root is null, we cannot find any declaration
        if (root == null) {
            return null;
        }
        if (shouldCache && elementToTreeCache.containsKey(elt)) {
            return elementToTreeCache.get(elt);
        }

        // Check for new declarations, outside of the AST.
        if (elt instanceof DetachedVarSymbol) {
            return ((DetachedVarSymbol) elt).getDeclaration();
        }

        // TODO: handle type parameter declarations?
        Tree fromElt;
        // Prevent calling declarationFor on elements we know we don't have
        // the tree for

        switch (elt.getKind()) {
            case CLASS:
            case ENUM:
            case INTERFACE:
            case ANNOTATION_TYPE:
            case FIELD:
            case ENUM_CONSTANT:
            case METHOD:
            case CONSTRUCTOR:
                fromElt = trees.getTree(elt);
                break;
            default:
                fromElt =
                        com.sun.tools.javac.tree.TreeInfo.declarationFor(
                                (com.sun.tools.javac.code.Symbol) elt,
                                (com.sun.tools.javac.tree.JCTree) root);
                break;
        }
        if (shouldCache) {
            elementToTreeCache.put(elt, fromElt);
        }
        return fromElt;
    }

    /**
     * Returns the current class type being visited by the visitor. The method uses the parameter
     * only if the most enclosing class cannot be found directly.
     *
     * @return type of the most enclosing class being visited
     */
    // This method is used to wrap access to visitorState
    protected final ClassTree getCurrentClassTree(Tree tree) {
        if (visitorState.getClassTree() != null) {
            return visitorState.getClassTree();
        }
        return TreeUtils.enclosingClass(getPath(tree));
    }

    protected final AnnotatedDeclaredType getCurrentClassType(Tree tree) {
        return getAnnotatedType(getCurrentClassTree(tree));
    }

    /**
     * Returns the receiver type of the current method being visited, and returns null if the
     * visited tree is not within a method or if that method has no receiver (e.g. a static method).
     *
     * <p>The method uses the parameter only if the most enclosing method cannot be found directly.
     *
     * @return receiver type of the most enclosing method being visited
     */
    protected final @Nullable AnnotatedDeclaredType getCurrentMethodReceiver(Tree tree) {
        AnnotatedDeclaredType res = visitorState.getMethodReceiver();
        if (res == null) {
            TreePath path = getPath(tree);
            if (path != null) {
                @SuppressWarnings("interning:assignment.type.incompatible") // used for == test
                @InternedDistinct MethodTree enclosingMethod = TreeUtils.enclosingMethod(path);
                ClassTree enclosingClass = TreeUtils.enclosingClass(path);

                boolean found = false;

                for (Tree member : enclosingClass.getMembers()) {
                    if (member.getKind() == Tree.Kind.METHOD) {
                        if (member == enclosingMethod) {
                            found = true;
                        }
                    }
                }

                if (found && enclosingMethod != null) {
                    AnnotatedExecutableType method = getAnnotatedType(enclosingMethod);
                    res = method.getReceiverType();
                    // TODO: three tests fail if one adds the following, which would make
                    // sense, or not?
                    // visitorState.setMethodReceiver(res);
                } else {
                    // We are within an anonymous class or field initializer
                    res = this.getAnnotatedType(enclosingClass);
                }
            }
        }
        return res;
    }

    protected final boolean isWithinConstructor(Tree tree) {
        if (visitorState.getClassType() != null) {
            return visitorState.getMethodTree() != null
                    && TreeUtils.isConstructor(visitorState.getMethodTree());
        }

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(getPath(tree));
        return enclosingMethod != null && TreeUtils.isConstructor(enclosingMethod);
    }

    private final Element getMostInnerClassOrMethod(Tree tree) {
        if (visitorState.getMethodTree() != null) {
            return TreeUtils.elementFromDeclaration(visitorState.getMethodTree());
        }
        if (visitorState.getClassTree() != null) {
            return TreeUtils.elementFromDeclaration(visitorState.getClassTree());
        }

        TreePath path = getPath(tree);
        if (path == null) {
            throw new BugInCF(
                    "AnnotatedTypeFactory.getMostInnerClassOrMethod: getPath(tree)=>null%n"
                            + "  TreePath.getPath(root, tree)=>%s%n  for tree (%s) = %s%n  root=%s",
                    TreePath.getPath(root, tree), tree.getClass(), tree, root);
        }
        for (Tree pathTree : path) {
            if (pathTree instanceof MethodTree) {
                return TreeUtils.elementFromDeclaration((MethodTree) pathTree);
            } else if (pathTree instanceof ClassTree) {
                return TreeUtils.elementFromDeclaration((ClassTree) pathTree);
            }
        }

        throw new BugInCF("AnnotatedTypeFactory.getMostInnerClassOrMethod: cannot be here");
    }

    /**
     * Gets the path for the given {@link Tree} under the current root by checking from the
     * visitor's current path, and only using {@link Trees#getPath(CompilationUnitTree, Tree)}
     * (which is much slower) only if {@code node} is not found on the current path.
     *
     * <p>Note that the given Tree has to be within the current compilation unit, otherwise null
     * will be returned.
     *
     * @param node the {@link Tree} to get the path for
     * @return the path for {@code node} under the current root
     */
    public final TreePath getPath(@FindDistinct Tree node) {
        assert root != null
                : "AnnotatedTypeFactory.getPath: root needs to be set when used on trees; factory: "
                        + this.getClass();

        if (node == null) {
            return null;
        }

        if (treePathCache.isCached(node)) {
            return treePathCache.getPath(root, node);
        }

        TreePath currentPath = visitorState.getPath();
        if (currentPath == null) {
            TreePath path = TreePath.getPath(root, node);
            treePathCache.addPath(node, path);
            return path;
        }

        // This method uses multiple heuristics to avoid calling
        // TreePath.getPath()

        // If the current path you are visiting is for this node we are done
        if (currentPath.getLeaf() == node) {
            treePathCache.addPath(node, currentPath);
            return currentPath;
        }

        // When running on Daikon, we noticed that a lot of calls happened
        // within a small subtree containing the node we are currently visiting

        // When testing on Daikon, two steps resulted in the best performance
        if (currentPath.getParentPath() != null) {
            currentPath = currentPath.getParentPath();
            treePathCache.addPath(currentPath.getLeaf(), currentPath);
            if (currentPath.getLeaf() == node) {
                return currentPath;
            }
            if (currentPath.getParentPath() != null) {
                currentPath = currentPath.getParentPath();
                treePathCache.addPath(currentPath.getLeaf(), currentPath);
                if (currentPath.getLeaf() == node) {
                    return currentPath;
                }
            }
        }

        final TreePath pathWithinSubtree = TreePath.getPath(currentPath, node);
        if (pathWithinSubtree != null) {
            treePathCache.addPath(node, pathWithinSubtree);
            return pathWithinSubtree;
        }

        // climb the current path till we see that
        // Works when getPath called on the enclosing method, enclosing
        // class
        TreePath current = currentPath;
        while (current != null) {
            treePathCache.addPath(current.getLeaf(), current);
            if (current.getLeaf() == node) {
                return current;
            }
            current = current.getParentPath();
        }

        // OK, we give up. Use the cache to look up.
        return treePathCache.getPath(root, node);
    }

    /**
     * Gets the {@link Element} representing the declaration of the method enclosing a tree node.
     * This feature is used to record the enclosing methods of {@link Tree}s that are created
     * internally by the checker.
     *
     * <p>TODO: Find a better way to store information about enclosing Trees.
     *
     * @param node the {@link Tree} to get the enclosing method for
     * @return the method {@link Element} enclosing the argument, or null if none has been recorded
     */
    public final Element getEnclosingElementForArtificialTree(Tree node) {
        return artificialTreeToEnclosingElementMap.get(node);
    }

    /**
     * Handle an artificial tree by mapping it to the enclosing element.
     *
     * <p>See {@code
     * org.checkerframework.framework.flow.CFCFGBuilder.CFCFGTranslationPhaseOne.handleArtificialTree(Tree)}.
     */
    public final void setEnclosingElementForArtificialTree(Tree node, Element enclosing) {
        artificialTreeToEnclosingElementMap.put(node, enclosing);
    }

    /**
     * Assert that the type is a type of valid type mirror, i.e. not an ERROR or OTHER type.
     *
     * @param type an annotated type
     * @return true if the type is a valid annotated type, false otherwise
     */
    static final boolean validAnnotatedType(AnnotatedTypeMirror type) {
        if (type == null) {
            return false;
        }
        return validType(type.getUnderlyingType());
    }

    /**
     * Used for asserting that a type is valid for converting to an annotated type.
     *
     * @return true if {@code type} can be converted to an annotated type, false otherwise
     */
    private static final boolean validType(TypeMirror type) {
        if (type == null) {
            return false;
        }
        switch (type.getKind()) {
            case ERROR:
            case OTHER:
            case PACKAGE:
                return false;
            default:
                return true;
        }
    }

    /**
     * Parses the stub files in the following order:
     *
     * <ol>
     *   <li>jdk.astub in the same directory as the checker, if it exists and ignorejdkastub option
     *       is not supplied <br>
     *   <li>jdkN.astub, where N is the Java version in the same directory as the checker, if it
     *       exists and ignorejdkastub option is not supplied <br>
     *   <li>Stub files listed in @StubFiles annotation on the checker; must be in same directory as
     *       the checker<br>
     *   <li>Stub files provide via stubs system property <br>
     *   <li>Stub files provide via stubs environment variable <br>
     *   <li>Stub files provide via stubs compiler option
     * </ol>
     *
     * <p>If a type is annotated with a qualifier from the same hierarchy in more than one stub
     * file, the qualifier in the last stub file is applied.
     *
     * <p>Sets typesFromStubFiles and declAnnosFromStubFiles by side effect, just before returning.
     */
    protected void parseStubFiles() {
        stubTypes.parseStubFiles();
    }

    /**
     * Returns the actual annotation mirror used to annotate this element, whose name equals the
     * passed annotation class (or is an alias for it). Returns null if none exists.
     *
     * @see #getDeclAnnotationNoAliases
     * @param elt the element to retrieve the declaration annotation from
     * @param anno annotation class
     * @return the annotation mirror for anno
     */
    @Override
    public final AnnotationMirror getDeclAnnotation(Element elt, Class<? extends Annotation> anno) {
        return getDeclAnnotation(elt, anno, true);
    }

    /**
     * Returns the actual annotation mirror used to annotate this element, whose name equals the
     * passed annotation class. Returns null if none exists. Does not check for aliases of the
     * annotation class.
     *
     * <p>Call this method from a checker that needs to alias annotations for one purpose and not
     * for another. For example, in the Lock Checker, {@code @LockingFree} and
     * {@code @ReleasesNoLocks} are both aliases of {@code @SideEffectFree} since they are all
     * considered side-effect-free with regard to the set of locks held before and after the method
     * call. However, a {@code synchronized} block is permitted inside a {@code @ReleasesNoLocks}
     * method but not inside a {@code @LockingFree} or {@code @SideEffectFree} method.
     *
     * @see #getDeclAnnotation
     * @param elt the element to retrieve the declaration annotation from
     * @param anno annotation class
     * @return the annotation mirror for anno
     */
    public final AnnotationMirror getDeclAnnotationNoAliases(
            Element elt, Class<? extends Annotation> anno) {
        return getDeclAnnotation(elt, anno, false);
    }

    /**
     * Returns true if the element appears in a stub file (Currently only works for methods,
     * constructors, and fields).
     */
    public boolean isFromStubFile(Element element) {
        return this.getDeclAnnotation(element, FromStubFile.class) != null;
    }

    /**
     * Returns true if the element is from bytecode and the if the element did not appear in a stub
     * file. Currently only works for methods, constructors, and fields.
     */
    public boolean isFromByteCode(Element element) {
        if (isFromStubFile(element)) {
            return false;
        }
        return ElementUtils.isElementFromByteCode(element);
    }

    /**
     * Returns true if redundancy between a stub file and bytecode should be reported.
     *
     * <p>For most type systems the default behavior of returning true is correct. For subcheckers,
     * redundancy in one of the type hierarchies can be ok. Such implementations should return
     * false.
     *
     * @return whether to warn about redundancy between a stub file and bytecode
     */
    public boolean shouldWarnIfStubRedundantWithBytecode() {
        return true;
    }

    /**
     * Returns the actual annotation mirror used to annotate this element, whose name equals the
     * passed annotation class (or is an alias for it). Returns null if none exists. May return the
     * canonical annotation that annotationName is an alias for.
     *
     * <p>This is the private implementation of the same-named, public method.
     *
     * <p>An option is provided to not to check for aliases of annotations. For example, an
     * annotated type factory may use aliasing for a pair of annotations for convenience while
     * needing in some cases to determine a strict ordering between them, such as when determining
     * whether the annotations on an overrider method are more specific than the annotations of an
     * overridden method.
     *
     * @param elt the element to retrieve the annotation from
     * @param annoClass the class the annotation to retrieve
     * @param checkAliases whether to return an annotation mirror for an alias of the requested
     *     annotation class name
     * @return the annotation mirror for the requested annotation, or null if not found
     */
    private AnnotationMirror getDeclAnnotation(
            Element elt, Class<? extends Annotation> annoClass, boolean checkAliases) {
        Set<AnnotationMirror> declAnnos = getDeclAnnotations(elt);

        for (AnnotationMirror am : declAnnos) {
            if (areSameByClass(am, annoClass)) {
                return am;
            }
        }
        // Look through aliases.
        if (checkAliases) {
            Pair<AnnotationMirror, Set<Class<? extends Annotation>>> aliases =
                    declAliases.get(annoClass);
            if (aliases != null) {
                for (Class<? extends Annotation> alias : aliases.second) {
                    for (AnnotationMirror am : declAnnos) {
                        if (areSameByClass(am, alias)) {
                            // TODO: need to copy over elements/fields
                            return aliases.first;
                        }
                    }
                }
            }
        }
        // Not found.
        return null;
    }

    /**
     * Returns all of the actual annotation mirrors used to annotate this element (includes stub
     * files and declaration annotations from overridden methods).
     *
     * @param elt the element for which to determine annotations
     * @return declaration annotations on this element
     */
    public Set<AnnotationMirror> getDeclAnnotations(Element elt) {
        Set<AnnotationMirror> cachedValue = cacheDeclAnnos.get(elt);
        if (cachedValue != null) {
            // Found in cache, return result.
            return cachedValue;
        }

        Set<AnnotationMirror> results = AnnotationUtils.createAnnotationSet();
        // Retrieving the annotations from the element.
        List<? extends AnnotationMirror> fromEle = elements.getAllAnnotationMirrors(elt);
        for (AnnotationMirror annotation : fromEle) {
            try {
                results.add(annotation);
            } catch (com.sun.tools.javac.code.Symbol.CompletionFailure cf) {
                // If a CompletionFailure occurs, issue a warning.
                checker.reportWarning(
                        annotation.getAnnotationType().asElement(),
                        "annotation.not.completed",
                        ElementUtils.getVerboseName(elt),
                        annotation);
            }
        }

        // If parsing stub files, return the annotations in the element.
        if (!stubTypes.isParsing()) {

            // Retrieving annotations from stub files.
            Set<AnnotationMirror> stubAnnos = stubTypes.getDeclAnnotation(elt);
            results.addAll(stubAnnos);

            if (elt.getKind() == ElementKind.METHOD) {
                // Retrieve the annotations from the overridden method's element.
                inheritOverriddenDeclAnnos((ExecutableElement) elt, results);
            }

            // Add the element and its annotations to the cache.
            cacheDeclAnnos.put(elt, results);
        }

        return results;
    }

    /**
     * Adds into {@code results} the declaration annotations found in all elements that the method
     * element {@code elt} overrides.
     *
     * @param elt method element
     * @param results {@code elt} local declaration annotations. The ones found in stub files and in
     *     the element itself.
     */
    private void inheritOverriddenDeclAnnos(ExecutableElement elt, Set<AnnotationMirror> results) {
        Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
                AnnotatedTypes.overriddenMethods(elements, this, elt);

        if (overriddenMethods != null) {
            for (ExecutableElement superElt : overriddenMethods.values()) {
                Set<AnnotationMirror> superAnnos = getDeclAnnotations(superElt);

                for (AnnotationMirror annotation : superAnnos) {
                    List<? extends AnnotationMirror> annotationsOnAnnotation;
                    try {
                        annotationsOnAnnotation =
                                annotation.getAnnotationType().asElement().getAnnotationMirrors();
                    } catch (com.sun.tools.javac.code.Symbol.CompletionFailure cf) {
                        // Fix for Issue 348: If a CompletionFailure occurs,
                        // issue a warning.
                        checker.reportWarning(
                                annotation.getAnnotationType().asElement(),
                                "annotation.not.completed",
                                ElementUtils.getVerboseName(elt),
                                annotation);
                        continue;
                    }
                    if (containsSameByClass(annotationsOnAnnotation, InheritedAnnotation.class)
                            || AnnotationUtils.containsSameByName(
                                    inheritedAnnotations, annotation)) {
                        addOrMerge(results, annotation);
                    }
                }
            }
        }
    }

    private void addOrMerge(Set<AnnotationMirror> results, AnnotationMirror annotation) {
        if (AnnotationUtils.containsSameByName(results, annotation)) {
            /*
             * TODO: feature request: figure out a way to merge multiple annotations
             * of the same kind. For some annotations this might mean merging some
             * arrays, for others it might mean converting a single annotation into a
             * container annotation. We should define a protected method for subclasses
             * to adapt the behavior.
             * For now, do nothing and just take the first, most concrete, annotation.
            AnnotationMirror prev = null;
            for (AnnotationMirror an : results) {
                if (AnnotationUtils.areSameByName(an, annotation)) {
                    prev = an;
                    break;
                }
            }
            results.remove(prev);
            AnnotationMirror merged = ...;
            results.add(merged);
            */
        } else {
            results.add(annotation);
        }
    }

    /**
     * Returns a list of all declaration annotations used to annotate this element, which have a
     * meta-annotation (i.e., an annotation on that annotation) with class {@code
     * metaAnnotationClass}.
     *
     * @param element the element for which to determine annotations
     * @param metaAnnotationClass the class of the meta-annotation that needs to be present
     * @return a list of pairs {@code (anno, metaAnno)} where {@code anno} is the annotation mirror
     *     at {@code element}, and {@code metaAnno} is the annotation mirror (of type {@code
     *     metaAnnotationClass}) used to meta-annotate the declaration of {@code anno}
     */
    public List<Pair<AnnotationMirror, AnnotationMirror>> getDeclAnnotationWithMetaAnnotation(
            Element element, Class<? extends Annotation> metaAnnotationClass) {
        List<Pair<AnnotationMirror, AnnotationMirror>> result = new ArrayList<>();
        Set<AnnotationMirror> annotationMirrors = getDeclAnnotations(element);

        for (AnnotationMirror candidate : annotationMirrors) {
            List<? extends AnnotationMirror> metaAnnotationsOnAnnotation;
            try {
                metaAnnotationsOnAnnotation =
                        candidate.getAnnotationType().asElement().getAnnotationMirrors();
            } catch (com.sun.tools.javac.code.Symbol.CompletionFailure cf) {
                // Fix for Issue 309: If a CompletionFailure occurs, issue a warning.
                // I didn't find a nicer alternative to check whether the Symbol can be completed.
                // The completer field of a Symbol might be non-null also in successful cases.
                // Issue a warning (exception only happens once) and continue.
                checker.reportWarning(
                        candidate.getAnnotationType().asElement(),
                        "annotation.not.completed",
                        ElementUtils.getVerboseName(element),
                        candidate);
                continue;
            }
            // First call copier, if exception, continue normal modula laws.
            for (AnnotationMirror ma : metaAnnotationsOnAnnotation) {
                if (areSameByClass(ma, metaAnnotationClass)) {
                    // This candidate has the right kind of meta-annotation.
                    // It might be a real contract, or a list of contracts.
                    if (isListForRepeatedAnnotation(candidate)) {
                        List<AnnotationMirror> wrappedCandidates =
                                AnnotationUtils.getElementValueArray(
                                        candidate, "value", AnnotationMirror.class, false);
                        for (AnnotationMirror wrappedCandidate : wrappedCandidates) {
                            result.add(Pair.of(wrappedCandidate, ma));
                        }
                    } else {
                        result.add(Pair.of(candidate, ma));
                    }
                }
            }
        }
        return result;
    }

    /** Cache for {@link #isListForRepeatedAnnotation}. */
    private final Map<DeclaredType, Boolean> isListForRepeatedAnnotationCache = new HashMap<>();

    /**
     * Returns true if the given annotation is a wrapper for multiple repeated annotations.
     *
     * @param a an annotation that might be a wrapper
     * @return true if the argument is a wrapper for multiple repeated annotations
     */
    private boolean isListForRepeatedAnnotation(AnnotationMirror a) {
        DeclaredType annotationType = a.getAnnotationType();
        Boolean resultObject = isListForRepeatedAnnotationCache.get(annotationType);
        if (resultObject != null) {
            return resultObject;
        }
        boolean result = isListForRepeatedAnnotationImplementation(annotationType);
        isListForRepeatedAnnotationCache.put(annotationType, result);
        return result;
    }

    /**
     * Returns true if the annotation is a wrapper for multiple repeated annotations.
     *
     * @param annotationType the declaration of the annotation to test
     * @return true if the annotation is a wrapper for multiple repeated annotations
     */
    private boolean isListForRepeatedAnnotationImplementation(DeclaredType annotationType) {
        TypeMirror enclosingType = annotationType.getEnclosingType();
        if (enclosingType == null) {
            return false;
        }
        if (!annotationType.asElement().getSimpleName().contentEquals("List")) {
            return false;
        }
        List<? extends Element> annoElements = annotationType.asElement().getEnclosedElements();
        if (annoElements.size() != 1) {
            return false;
        }
        // TODO: should check that the type of the single element is: "array of enclosingType".
        return true;
    }

    /**
     * Returns a list of all annotations used to annotate this element, which have a meta-annotation
     * (i.e., an annotation on that annotation) with class {@code metaAnnotationClass}.
     *
     * @param element the element at which to look for annotations
     * @param metaAnnotationClass the class of the meta-annotation that needs to be present
     * @return a list of pairs {@code (anno, metaAnno)} where {@code anno} is the annotation mirror
     *     at {@code element}, and {@code metaAnno} is the annotation mirror used to annotate {@code
     *     anno}.
     */
    public List<Pair<AnnotationMirror, AnnotationMirror>> getAnnotationWithMetaAnnotation(
            Element element, Class<? extends Annotation> metaAnnotationClass) {
        List<Pair<AnnotationMirror, AnnotationMirror>> result = new ArrayList<>();
        Set<AnnotationMirror> annotationMirrors = AnnotationUtils.createAnnotationSet();

        // Consider real annotations.
        annotationMirrors.addAll(getAnnotatedType(element).getAnnotations());

        // Consider declaration annotations
        annotationMirrors.addAll(getDeclAnnotations(element));

        // Go through all annotations found.
        for (AnnotationMirror annotation : annotationMirrors) {
            List<? extends AnnotationMirror> annotationsOnAnnotation =
                    annotation.getAnnotationType().asElement().getAnnotationMirrors();
            for (AnnotationMirror a : annotationsOnAnnotation) {
                if (areSameByClass(a, metaAnnotationClass)) {
                    result.add(Pair.of(annotation, a));
                }
            }
        }
        return result;
    }

    /**
     * Whether or not the {@code annotatedTypeMirror} has a qualifier parameter.
     *
     * @param annotatedTypeMirror AnnotatedTypeMirror to check
     * @param top the top of the hierarchy to check
     * @return true if the type has a qualifier parameter
     */
    public boolean hasQualifierParameterInHierarchy(
            AnnotatedTypeMirror annotatedTypeMirror, AnnotationMirror top) {
        return AnnotationUtils.containsSame(
                getQualifierParameterHierarchies(annotatedTypeMirror), top);
    }

    /**
     * Whether or not the {@code element} has a qualifier parameter.
     *
     * @param element element to check
     * @param top the top of the hierarchy to check
     * @return true if the type has a qualifier parameter
     */
    public boolean hasQualifierParameterInHierarchy(
            @Nullable Element element, AnnotationMirror top) {
        if (element == null) {
            return false;
        }
        return AnnotationUtils.containsSame(getQualifierParameterHierarchies(element), top);
    }

    /**
     * Returns whether the {@code HasQualifierParameter} annotation was explicitly written on {@code
     * element} for the hierarchy given by {@code top}.
     *
     * @param element Element to check
     * @param top the top qualifier for the hierarchy to check
     * @return whether the class given by {@code element} has been explicitly annotated with {@code
     *     HasQualifierParameter} for the given hierarchy
     */
    public boolean hasExplicitQualifierParameterInHierarchy(Element element, AnnotationMirror top) {
        return AnnotationUtils.containsSame(
                getSupportedAnnotationsInElementAnnotation(element, HasQualifierParameter.class),
                top);
    }

    /**
     * Returns whether the {@code NoQualifierParameter} annotation was explicitly written on {@code
     * element} for the hierarchy given by {@code top}.
     *
     * @param element Element to check
     * @param top the top qualifier for the hierarchy to check
     * @return whether the class given by {@code element} has been explicitly annotated with {@code
     *     NoQualifierParameter} for the given hierarchy
     */
    public boolean hasExplicitNoQualifierParameterInHierarchy(
            Element element, AnnotationMirror top) {
        return AnnotationUtils.containsSame(
                getSupportedAnnotationsInElementAnnotation(element, NoQualifierParameter.class),
                top);
    }

    /**
     * Returns the set of top annotations representing all the hierarchies for which this type has a
     * qualifier parameter.
     *
     * @param annotatedType AnnotatedTypeMirror to check
     * @return the set of top annotations representing all the hierarchies for which this type has a
     *     qualifier parameter
     */
    public Set<AnnotationMirror> getQualifierParameterHierarchies(
            AnnotatedTypeMirror annotatedType) {
        while (annotatedType.getKind() == TypeKind.TYPEVAR
                || annotatedType.getKind() == TypeKind.WILDCARD) {
            if (annotatedType.getKind() == TypeKind.TYPEVAR) {
                annotatedType = ((AnnotatedTypeVariable) annotatedType).getUpperBound();
            } else if (annotatedType.getKind() == TypeKind.WILDCARD) {
                annotatedType = ((AnnotatedWildcardType) annotatedType).getSuperBound();
            }
        }

        if (annotatedType.getKind() != TypeKind.DECLARED) {
            return Collections.emptySet();
        }

        AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) annotatedType;
        Element element = declaredType.getUnderlyingType().asElement();
        if (element == null) {
            return Collections.emptySet();
        }
        return getQualifierParameterHierarchies(element);
    }

    /**
     * Returns the set of top annotations representing all the hierarchies for which this element
     * has a qualifier parameter.
     *
     * @param element Element to check
     * @return the set of top annotations representing all the hierarchies for which this element
     *     has a qualifier parameter
     */
    public Set<AnnotationMirror> getQualifierParameterHierarchies(Element element) {
        if (!ElementUtils.isTypeDeclaration(element)) {
            return Collections.emptySet();
        }

        Set<AnnotationMirror> found = AnnotationUtils.createAnnotationSet();
        found.addAll(
                getSupportedAnnotationsInElementAnnotation(element, HasQualifierParameter.class));
        Set<AnnotationMirror> hasQualifierParameterTops = AnnotationUtils.createAnnotationSet();
        PackageElement packageElement = ElementUtils.enclosingPackage(element);

        // Traverse all packages containing this element.
        while (packageElement != null) {
            Set<AnnotationMirror> packageDefaultTops =
                    getSupportedAnnotationsInElementAnnotation(
                            packageElement, HasQualifierParameter.class);
            hasQualifierParameterTops.addAll(packageDefaultTops);

            packageElement = ElementUtils.parentPackage(packageElement, elements);
        }

        Set<AnnotationMirror> noQualifierParamClasses =
                getSupportedAnnotationsInElementAnnotation(element, NoQualifierParameter.class);
        for (AnnotationMirror anno : hasQualifierParameterTops) {
            if (!AnnotationUtils.containsSame(noQualifierParamClasses, anno)) {
                found.add(anno);
            }
        }

        return found;
    }

    /**
     * Returns a set of supported annotation mirrors corresponding to the annotation classes listed
     * in the value element of an annotation with class {@code annoClass} on {@code element}.
     *
     * @param element Element to check
     * @param annoClass the class for an annotation that's written on elements, whose value element
     *     is a list of annotation classes
     * @return the set of supported annotations with classes listed in the value element of an
     *     annotation with class {@code annoClass} on the {@code element}. Returns an empty set if
     *     {@code annoClass} is not written on {@code element} or {@code element} is null.
     */
    private Set<AnnotationMirror> getSupportedAnnotationsInElementAnnotation(
            @Nullable Element element, Class<? extends Annotation> annoClass) {
        if (element == null) {
            return Collections.emptySet();
        }
        // TODO: caching
        AnnotationMirror annotation = getDeclAnnotation(element, annoClass);
        if (annotation == null) {
            return Collections.emptySet();
        }

        Set<AnnotationMirror> found = AnnotationUtils.createAnnotationSet();
        List<Name> qualClasses =
                AnnotationUtils.getElementValueClassNames(annotation, "value", true);
        for (Name qual : qualClasses) {
            AnnotationMirror annotationMirror = AnnotationBuilder.fromName(elements, qual);
            if (isSupportedQualifier(annotationMirror)) {
                found.add(annotationMirror);
            }
        }
        return found;
    }

    /**
     * Returns a wildcard type to be used as a type argument when the correct type could not be
     * inferred. The wildcard will be marked as an uninferred wildcard so that {@link
     * AnnotatedWildcardType#isUninferredTypeArgument()} returns true.
     *
     * <p>This method should only be used by type argument inference.
     * org.checkerframework.framework.util.AnnotatedTypes.inferTypeArguments(ProcessingEnvironment,
     * AnnotatedTypeFactory, ExpressionTree, ExecutableElement)
     *
     * @param typeVar TypeVariable which could not be inferred
     * @return a wildcard that is marked as an uninferred type argument
     */
    public AnnotatedWildcardType getUninferredWildcardType(AnnotatedTypeVariable typeVar) {
        final boolean intersectionType;
        final TypeMirror boundType;
        if (typeVar.getUpperBound().getKind() == TypeKind.INTERSECTION) {
            boundType = typeVar.getUpperBound().directSuperTypes().get(0).getUnderlyingType();
            intersectionType = true;
        } else {
            boundType = typeVar.getUnderlyingType().getUpperBound();
            intersectionType = false;
        }

        WildcardType wc = types.getWildcardType(boundType, null);
        AnnotatedWildcardType wctype =
                (AnnotatedWildcardType) AnnotatedTypeMirror.createType(wc, this, false);
        wctype.setTypeVariable(typeVar.getUnderlyingType());
        if (!intersectionType) {
            wctype.setExtendsBound(typeVar.getUpperBound().deepCopy());
        } else {
            wctype.getExtendsBound().addAnnotations(typeVar.getUpperBound().getAnnotations());
        }
        wctype.setSuperBound(typeVar.getLowerBound().deepCopy());
        wctype.addAnnotations(typeVar.getAnnotations());
        addDefaultAnnotations(wctype);
        wctype.setUninferredTypeArgument();
        return wctype;
    }

    /**
     * If {@code wildcard}'s upper bound is a super type of {@code annotatedTypeMirror}, this method
     * returns an AnnotatedTypeMirror with the same qualifiers as {@code annotatedTypeMirror}, but
     * the underlying Java type is the most specific base type of {@code annotatedTypeMirror} whose
     * erasure type is equivalent to the upper bound of {@code wildcard}.
     *
     * <p>Otherwise, returns {@code annotatedTypeMirror} unmodified.
     *
     * <p>For example:
     *
     * <pre>
     * wildcard := @NonNull ? extends @NonNull Object
     * annotatedTypeMirror := @Nullable String
     *
     * widenToUpperBound(annotatedTypeMirror, wildcard) returns @Nullable Object
     * </pre>
     *
     * This method is needed because, the Java compiler allows wildcards to have upper bounds above
     * the type variable upper bounds for which they are type arguments. For example, given the
     * following parametric type:
     *
     * <pre>
     * {@code class MyClass<T extends Number>}
     * </pre>
     *
     * the following is legal:
     *
     * <pre>
     * {@code MyClass<? extends Object>}
     * </pre>
     *
     * This is sound because in Java the wildcard is capture converted to: {@code CAP#1 extends
     * Number from capture of ? extends Object}.
     *
     * <p>Because the Checker Framework does not implement capture conversion, wildcard upper bounds
     * may cause spurious errors in subtyping checks. This method prevents those errors by widening
     * the upper bound of the type parameter.
     *
     * <p>This method widens the underlying Java type of the upper bound of the type parameter
     * rather than narrowing the bound of the wildcard in order to avoid issuing an error with an
     * upper bound that is not in source code.
     *
     * <p>The widened type should only be used for typing checks that require it. Using the widened
     * type elsewhere would cause confusing error messages with types not in the source code.
     *
     * @param annotatedTypeMirror AnnotatedTypeMirror to widen
     * @param wildcard AnnotatedWildcardType whose upper bound is used to widen
     * @return {@code annotatedTypeMirror} widen to the upper bound of {@code wildcard}
     */
    public AnnotatedTypeMirror widenToUpperBound(
            final AnnotatedTypeMirror annotatedTypeMirror, final AnnotatedWildcardType wildcard) {
        final TypeMirror toModifyTypeMirror = annotatedTypeMirror.getUnderlyingType();
        final TypeMirror wildcardUBTypeMirror = wildcard.getExtendsBound().getUnderlyingType();
        if (TypesUtils.isErasedSubtype(wildcardUBTypeMirror, toModifyTypeMirror, types)) {
            return annotatedTypeMirror;
        } else if (TypesUtils.isErasedSubtype(toModifyTypeMirror, wildcardUBTypeMirror, types)) {
            return AnnotatedTypes.asSuper(this, annotatedTypeMirror, wildcard);
        } else if (wildcardUBTypeMirror.getKind() == TypeKind.DECLARED
                && TypesUtils.getTypeElement(wildcardUBTypeMirror).getKind().isInterface()) {
            // If the Checker Framework implemented capture conversion, then in this case, then
            // the upper bound of the capture converted wildcard would be an intersection type.
            // See JLS 15.1.10
            // (https://docs.oracle.com/javase/specs/jls/se11/html/jls-5.html#jls-5.1.10)

            // For example:
            // class MyClass<@A T extends @B Number> {}
            // MyClass<@C ? extends @D Serializable>
            // The upper bound of the captured wildcard:
            // glb(@B Number, @D Serializable) = @B Number & @D Serializable
            // The about upper bound must be a subtype of the declared upper bound:
            // @B Number & @D Serializable <: @B Number, which is always true.

            // So, replace the upper bound at the declaration with the wildcard's upper bound so
            // that the rest of the subtyping test pass.
            return wildcard.getExtendsBound();
        }

        return annotatedTypeMirror;
    }

    /**
     * Returns the function type that this member reference targets.
     *
     * <p>The function type is the type of the single method declared in the functional interface
     * adapted as if it were invoked using the functional interface as the receiver expression.
     *
     * <p>The target type of a member reference is the type to which it is assigned or casted.
     *
     * @param tree member reference tree
     * @return the function type that this method reference targets
     */
    public AnnotatedExecutableType getFunctionTypeFromTree(MemberReferenceTree tree) {
        return getFnInterfaceFromTree(tree).second;
    }

    /**
     * Returns the function type that this lambda targets.
     *
     * <p>The function type is the type of the single method declared in the functional interface
     * adapted as if it were invoked using the functional interface as the receiver expression.
     *
     * <p>The target type of a lambda is the type to which it is assigned or casted.
     *
     * @param tree lambda expression tree
     * @return the function type that this lambda targets
     */
    public AnnotatedExecutableType getFunctionTypeFromTree(LambdaExpressionTree tree) {
        return getFnInterfaceFromTree(tree).second;
    }

    /**
     * Returns the functional interface and the function type that this lambda or member references
     * targets.
     *
     * <p>The function type is the type of the single method declared in the functional interface
     * adapted as if it were invoked using the functional interface as the receiver expression.
     *
     * <p>The target type of a lambda or a method reference is the type to which it is assigned or
     * casted.
     *
     * @param tree lambda expression tree or member reference tree
     * @return the functional interface and the function type that this method reference or lambda
     *     targets
     */
    public Pair<AnnotatedTypeMirror, AnnotatedExecutableType> getFnInterfaceFromTree(Tree tree) {

        // Functional interface
        AnnotatedTypeMirror functionalInterfaceType = getFunctionalInterfaceType(tree);
        if (functionalInterfaceType.getKind() == TypeKind.DECLARED) {
            makeGroundTargetType(
                    (AnnotatedDeclaredType) functionalInterfaceType,
                    (DeclaredType) TreeUtils.typeOf(tree));
        }

        // Functional method
        Element fnElement = TreeUtils.findFunction(tree, processingEnv);

        // Function type
        AnnotatedExecutableType functionType =
                (AnnotatedExecutableType)
                        AnnotatedTypes.asMemberOf(types, this, functionalInterfaceType, fnElement);

        return Pair.of(functionalInterfaceType, functionType);
    }

    /**
     * Get the AnnotatedDeclaredType for the FunctionalInterface from assignment context of the
     * method reference or lambda expression which may be a variable assignment, a method call, or a
     * cast.
     *
     * <p>The assignment context is not always correct, so we must search up the AST. It will
     * recursively search for lambdas nested in lambdas.
     *
     * @param tree the tree of the lambda or method reference
     * @return the functional interface type or an uninferred type argument
     */
    private AnnotatedTypeMirror getFunctionalInterfaceType(Tree tree) {

        Tree parentTree = getPath(tree).getParentPath().getLeaf();
        switch (parentTree.getKind()) {
            case PARENTHESIZED:
                return getFunctionalInterfaceType(parentTree);

            case TYPE_CAST:
                TypeCastTree cast = (TypeCastTree) parentTree;
                assert isFunctionalInterface(
                        trees.getTypeMirror(getPath(cast.getType())), parentTree, tree);
                AnnotatedTypeMirror castATM = getAnnotatedType(cast.getType());
                if (castATM.getKind() == TypeKind.INTERSECTION) {
                    AnnotatedIntersectionType itype = (AnnotatedIntersectionType) castATM;
                    for (AnnotatedTypeMirror t : itype.directSuperTypes()) {
                        if (TypesUtils.isFunctionalInterface(
                                t.getUnderlyingType(), getProcessingEnv())) {
                            return t;
                        }
                    }
                    // We should never reach here: isFunctionalInterface performs the same check
                    // and would have raised an error already.
                    throw new BugInCF(
                            "Expected the type of a cast tree in an assignment context to contain a functional interface bound. "
                                    + "Found type: %s for tree: %s in lambda tree: %s",
                            castATM, cast, tree);
                }
                return castATM;

            case NEW_CLASS:
                NewClassTree newClass = (NewClassTree) parentTree;
                int indexOfLambda = newClass.getArguments().indexOf(tree);
                ParameterizedExecutableType con = this.constructorFromUse(newClass);
                AnnotatedTypeMirror constructorParam =
                        AnnotatedTypes.getAnnotatedTypeMirrorOfParameter(
                                con.executableType, indexOfLambda);
                assert isFunctionalInterface(
                        constructorParam.getUnderlyingType(), parentTree, tree);
                return constructorParam;

            case NEW_ARRAY:
                NewArrayTree newArray = (NewArrayTree) parentTree;
                AnnotatedArrayType newArrayATM = getAnnotatedType(newArray);
                AnnotatedTypeMirror elementATM = newArrayATM.getComponentType();
                assert isFunctionalInterface(elementATM.getUnderlyingType(), parentTree, tree);
                return elementATM;

            case METHOD_INVOCATION:
                MethodInvocationTree method = (MethodInvocationTree) parentTree;
                int index = method.getArguments().indexOf(tree);
                ParameterizedExecutableType exe = this.methodFromUse(method);
                AnnotatedTypeMirror param =
                        AnnotatedTypes.getAnnotatedTypeMirrorOfParameter(exe.executableType, index);
                if (param.getKind() == TypeKind.WILDCARD) {
                    // param is an uninferred wildcard.
                    TypeMirror typeMirror = TreeUtils.typeOf(tree);
                    param = AnnotatedTypeMirror.createType(typeMirror, this, false);
                    addDefaultAnnotations(param);
                }
                assert isFunctionalInterface(param.getUnderlyingType(), parentTree, tree);
                return param;

            case VARIABLE:
                VariableTree varTree = (VariableTree) parentTree;
                assert isFunctionalInterface(TreeUtils.typeOf(varTree), parentTree, tree);
                return getAnnotatedType(varTree.getType());

            case ASSIGNMENT:
                AssignmentTree assignmentTree = (AssignmentTree) parentTree;
                assert isFunctionalInterface(TreeUtils.typeOf(assignmentTree), parentTree, tree);
                return getAnnotatedType(assignmentTree.getVariable());

            case RETURN:
                Tree enclosing =
                        TreeUtils.enclosingOfKind(
                                getPath(parentTree),
                                new HashSet<>(
                                        Arrays.asList(
                                                Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION)));

                if (enclosing.getKind() == Tree.Kind.METHOD) {
                    MethodTree enclosingMethod = (MethodTree) enclosing;
                    return getAnnotatedType(enclosingMethod.getReturnType());
                } else {
                    LambdaExpressionTree enclosingLambda = (LambdaExpressionTree) enclosing;
                    AnnotatedExecutableType methodExe = getFunctionTypeFromTree(enclosingLambda);
                    return methodExe.getReturnType();
                }

            case LAMBDA_EXPRESSION:
                LambdaExpressionTree enclosingLambda = (LambdaExpressionTree) parentTree;
                AnnotatedExecutableType methodExe = getFunctionTypeFromTree(enclosingLambda);
                return methodExe.getReturnType();

            case CONDITIONAL_EXPRESSION:
                ConditionalExpressionTree conditionalExpressionTree =
                        (ConditionalExpressionTree) parentTree;
                final AnnotatedTypeMirror falseType =
                        getAnnotatedType(conditionalExpressionTree.getFalseExpression());
                final AnnotatedTypeMirror trueType =
                        getAnnotatedType(conditionalExpressionTree.getTrueExpression());

                // Known cases where we must use LUB because falseType/trueType will not be equal:
                // a) when one of the types is a type variable that extends a functional interface
                //    or extends a type variable that extends a functional interface
                // b) When one of the two sides of the expression is a reference to a sub-interface.
                //   e.g.   interface ConsumeStr {
                //              public void consume(String s)
                //          }
                //          interface SubConsumer extends ConsumeStr {
                //              default void someOtherMethod() { ... }
                //          }
                //   SubConsumer s = ...;
                //   ConsumeStr stringConsumer = (someCondition) ? s : System.out::println;
                AnnotatedTypeMirror conditionalType =
                        AnnotatedTypes.leastUpperBound(this, trueType, falseType);
                assert isFunctionalInterface(conditionalType.getUnderlyingType(), parentTree, tree);
                return conditionalType;

            default:
                throw new BugInCF(
                        "Could not find functional interface from assignment context. "
                                + "Unexpected tree type: "
                                + parentTree.getKind()
                                + " For lambda tree: "
                                + tree);
        }
    }

    private boolean isFunctionalInterface(TypeMirror typeMirror, Tree contextTree, Tree tree) {
        if (typeMirror.getKind() == TypeKind.WILDCARD) {
            // Ignore wildcards, because they are uninferred type arguments.
            return true;
        }
        Type type = (Type) typeMirror;

        if (!TypesUtils.isFunctionalInterface(type, processingEnv)) {
            if (type.getKind() == TypeKind.INTERSECTION) {
                IntersectionType itype = (IntersectionType) type;
                for (TypeMirror t : itype.getBounds()) {
                    if (TypesUtils.isFunctionalInterface(t, processingEnv)) {
                        // As long as any of the bounds is a functional interface
                        // we should be fine.
                        return true;
                    }
                }
            }
            throw new BugInCF(
                    "Expected the type of %s tree in assignment context to be a functional interface. "
                            + "Found type: %s for tree: %s in lambda tree: %s",
                    contextTree.getKind(), type, contextTree, tree);
        }
        return true;
    }

    /**
     * Create the ground target type of the functional interface.
     *
     * <p>Basically, it replaces the wildcards with their bounds doing a capture conversion like glb
     * for extends bounds.
     *
     * @see "JLS 9.9"
     * @param functionalType the functional interface type
     * @param groundTargetJavaType the Java type as found by javac
     */
    private void makeGroundTargetType(
            AnnotatedDeclaredType functionalType, DeclaredType groundTargetJavaType) {
        if (functionalType.getTypeArguments().isEmpty()) {
            return;
        }

        List<AnnotatedTypeParameterBounds> bounds =
                this.typeVariablesFromUse(
                        functionalType,
                        (TypeElement) functionalType.getUnderlyingType().asElement());

        List<AnnotatedTypeMirror> newTypeArguments =
                new ArrayList<>(functionalType.getTypeArguments());
        boolean sizesDiffer =
                functionalType.getTypeArguments().size()
                        != groundTargetJavaType.getTypeArguments().size();

        for (int i = 0; i < functionalType.getTypeArguments().size(); i++) {
            AnnotatedTypeMirror argType = functionalType.getTypeArguments().get(i);
            if (argType.getKind() == TypeKind.WILDCARD) {
                AnnotatedWildcardType wildcardType = (AnnotatedWildcardType) argType;

                TypeMirror wildcardUbType = wildcardType.getExtendsBound().getUnderlyingType();

                if (wildcardType.isUninferredTypeArgument()) {
                    // Keep the uninferred type so that it is ignored by later subtyping and
                    // containment checks.
                    newTypeArguments.set(i, wildcardType);
                } else if (isExtendsWildcard(wildcardType)) {
                    TypeMirror correctArgType;
                    if (sizesDiffer) {
                        // The java type is raw.
                        TypeMirror typeParamUbType =
                                bounds.get(i).getUpperBound().getUnderlyingType();
                        correctArgType =
                                TypesUtils.greatestLowerBound(
                                        typeParamUbType,
                                        wildcardUbType,
                                        this.checker.getProcessingEnvironment());
                    } else {
                        correctArgType = groundTargetJavaType.getTypeArguments().get(i);
                    }

                    final AnnotatedTypeMirror newArg;
                    if (types.isSameType(wildcardUbType, correctArgType)) {
                        newArg = wildcardType.getExtendsBound().deepCopy();
                    } else if (correctArgType.getKind() == TypeKind.TYPEVAR) {
                        newArg = this.toAnnotatedType(correctArgType, false);
                        AnnotatedTypeVariable newArgAsTypeVar = (AnnotatedTypeVariable) newArg;
                        newArgAsTypeVar
                                .getUpperBound()
                                .replaceAnnotations(
                                        wildcardType.getExtendsBound().getAnnotations());
                        newArgAsTypeVar
                                .getLowerBound()
                                .replaceAnnotations(wildcardType.getSuperBound().getAnnotations());
                    } else {
                        newArg = this.toAnnotatedType(correctArgType, false);
                        newArg.replaceAnnotations(wildcardType.getExtendsBound().getAnnotations());
                    }
                    newTypeArguments.set(i, newArg);
                } else {
                    newTypeArguments.set(i, wildcardType.getSuperBound());
                }
            }
        }
        functionalType.setTypeArguments(newTypeArguments);

        // When the groundTargetJavaType is different from the underlying type of functionalType,
        // only the main annotations are copied.  Add default annotations in places without
        // annotations.
        addDefaultAnnotations(functionalType);
    }

    /**
     * Check that a wildcard is an extends wildcard.
     *
     * @param awt the wildcard type
     * @return true if awt is an extends wildcard
     */
    private boolean isExtendsWildcard(AnnotatedWildcardType awt) {
        return awt.getUnderlyingType().getSuperBound() == null;
    }

    /** Accessor for the element utilities. */
    public Elements getElementUtils() {
        return this.elements;
    }

    /** Accessor for the tree utilities. */
    public Trees getTreeUtils() {
        return this.trees;
    }

    /** Accessor for the processing environment. */
    public ProcessingEnvironment getProcessingEnv() {
        return this.processingEnv;
    }

    /** Accessor for the {@link CFContext}. */
    public CFContext getContext() {
        return checker;
    }

    static final Pattern plusConstant = Pattern.compile(" *\\+ *(-?[0-9]+)$");
    static final Pattern minusConstant = Pattern.compile(" *- *(-?[0-9]+)$");

    /**
     * Given an expression, split it into a subexpression and a constant offset. For example:
     *
     * <pre>{@code
     * "a" => <"a", "0">
     * "a + 5" => <"a", "5">
     * "a + -5" => <"a", "-5">
     * "a - 5" => <"a", "-5">
     * }</pre>
     *
     * There are methods that can only take as input an expression that represents a Receiver. The
     * purpose of this is to pre-process expressions to make those methods more likely to succeed.
     *
     * @param expression an expression to remove a constant offset from
     * @return a sub-expression and a constant offset. The offset is "0" if this routine is unable
     *     to splite the given expression
     */
    // TODO: generalize.  There is no reason this couldn't handle arbitrary addition and subtraction
    // expressions, given the Index Checker's support for OffsetEquation.  That might even make its
    // implementation simpler.
    public static Pair<String, String> getExpressionAndOffset(String expression) {
        String expr = expression;
        String offset = "0";

        // Is this normalization necessary?
        // Remove surrrounding whitespace.
        expr = expr.trim();
        // Remove surrounding parentheses.
        if (expr.matches("^\\([^()]\\)")) {
            expr = expr.substring(1, expr.length() - 2).trim();
        }

        Matcher mPlus = plusConstant.matcher(expr);
        Matcher mMinus = minusConstant.matcher(expr);
        if (mPlus.find()) {
            expr = expr.substring(0, mPlus.start());
            offset = mPlus.group(1);
        } else if (mMinus.find()) {
            expr = expr.substring(0, mMinus.start());
            offset = negateConstant(mMinus.group(1));
        }

        if (offset.equals("-0")) {
            offset = "0";
        }

        expr = expr.intern();
        offset = offset.intern();

        return Pair.of(expr, offset);
    }

    /**
     * Given an expression string, returns its negation.
     *
     * @param constantExpression a string representing an integer constant
     * @return the negation of constantExpression
     */
    // Also see Subsequence.negateString which is similar but more sophisticated.
    public static String negateConstant(String constantExpression) {
        if (constantExpression.startsWith("-")) {
            return constantExpression.substring(1);
        } else {
            if (constantExpression.startsWith("+")) {
                constantExpression = constantExpression.substring(1);
            }
            return "-" + constantExpression;
        }
    }

    /**
     * Returns {@code null} or an annotated type mirror that type argument inference should assume
     * {@code expressionTree} is assigned to.
     *
     * <p>If {@code null} is returned, inference proceeds normally.
     *
     * <p>If a type is returned, then inference assumes that {@code expressionTree} was asigned to
     * it. This biases the inference algorithm toward the annotations in the returned type. In
     * particular, if the annotations on type variables in invariant positions are a super type of
     * the annotations inferred, the super type annotations are chosen.
     *
     * <p>This implementation returns null, but subclasses may override this method to return a
     * type.
     *
     * @param expressionTree an expression which has no assignment context and for which type
     *     arguments need to be inferred
     * @return {@code null} or an annotated type mirror that inferrence should pretend {@code
     *     expressionTree} is assigned to
     */
    public @Nullable AnnotatedTypeMirror getDummyAssignedTo(ExpressionTree expressionTree) {
        return null;
    }

    /**
     * Checks that the annotation {@code am} has the name of {@code annoClass}. Values are ignored.
     *
     * <p>This method is faster than {@link AnnotationUtils#areSameByClass(AnnotationMirror, Class)}
     * because is caches the name of the class rather than computing it each time.
     *
     * @param am the AnnotationMirror whose class to compare
     * @param annoClass the class to compare
     * @return true if annoclass is the class of am
     */
    public boolean areSameByClass(AnnotationMirror am, Class<? extends Annotation> annoClass) {
        if (!shouldCache) {
            return AnnotationUtils.areSameByName(am, annoClass.getCanonicalName());
        }
        String canonicalName = annotationClassNames.get(annoClass);
        if (canonicalName == null) {
            canonicalName = annoClass.getCanonicalName();
            assert canonicalName != null : "@AssumeAssertion(nullness): assumption";
            annotationClassNames.put(annoClass, canonicalName);
        }
        return AnnotationUtils.areSameByName(am, canonicalName);
    }

    /**
     * Checks that the collection contains the annotation. Using Collection.contains does not always
     * work, because it does not use areSame for comparison.
     *
     * <p>This method is faster than {@link AnnotationUtils#containsSameByClass(Collection, Class)}
     * because is caches the name of the class rather than computing it each time.
     *
     * @param c a collection of AnnotationMirrors
     * @param anno the annotation class to search for in c
     * @return true iff c contains anno, according to areSameByClass
     */
    public boolean containsSameByClass(
            Collection<? extends AnnotationMirror> c, Class<? extends Annotation> anno) {
        return getAnnotationByClass(c, anno) != null;
    }

    /**
     * Returns the AnnotationMirror in {@code c} that has the same class as {@code anno}.
     *
     * <p>This method is faster than {@link AnnotationUtils#getAnnotationByClass(Collection, Class)}
     * because is caches the name of the class rather than computing it each time.
     *
     * @param c a collection of AnnotationMirrors
     * @param anno the class to search for in c
     * @return AnnotationMirror with the same class as {@code anno} iff c contains anno, according
     *     to areSameByClass; otherwise, {@code null}
     */
    public @Nullable AnnotationMirror getAnnotationByClass(
            Collection<? extends AnnotationMirror> c, Class<? extends Annotation> anno) {
        for (AnnotationMirror an : c) {
            if (areSameByClass(an, anno)) {
                return an;
            }
        }
        return null;
    }
}
