package org.checkerframework.framework.type;

/*>>>
import org.checkerframework.checker.interning.qual.*;
import org.checkerframework.checker.nullness.qual.Nullable;
*/

// The imports from com.sun, but they are all
// @jdk.Exported and therefore somewhat safe to use.
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
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.reflection.DefaultReflectionResolver;
import org.checkerframework.common.reflection.MethodValAnnotatedTypeFactory;
import org.checkerframework.common.reflection.MethodValChecker;
import org.checkerframework.common.reflection.ReflectionResolver;
import org.checkerframework.common.wholeprograminference.WholeProgramInference;
import org.checkerframework.common.wholeprograminference.WholeProgramInferenceScenes;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.FromByteCode;
import org.checkerframework.framework.qual.FromStubFile;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.stub.StubParser;
import org.checkerframework.framework.stub.StubResource;
import org.checkerframework.framework.stub.StubUtil;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.AnnotationFormatter;
import org.checkerframework.framework.util.CFContext;
import org.checkerframework.framework.util.DefaultAnnotationFormatter;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.TreePathCacher;
import org.checkerframework.framework.util.typeinference.DefaultTypeArgumentInference;
import org.checkerframework.framework.util.typeinference.TypeArgumentInference;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.CollectionUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
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
 * This implementation only adds qualifiers explicitly specified by the programmer.
 *
 * <p>Type system checker writers may need to subclass this class, to add implicit and default
 * qualifiers according to the type system semantics. Subclasses should especially override {@link
 * AnnotatedTypeFactory#addComputedTypeAnnotations(Element, AnnotatedTypeMirror)} and {@link
 * #addComputedTypeAnnotations(Tree, AnnotatedTypeMirror)}.
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
    protected /*@Nullable*/ CompilationUnitTree root;

    /** The processing environment to use for accessing compiler internals. */
    protected final ProcessingEnvironment processingEnv;

    /** Utility class for working with {@link Element}s. */
    protected final Elements elements;

    /** Utility class for working with {@link TypeMirror}s. */
    protected final Types types;

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

    /** performs whole program inference */
    private WholeProgramInference wholeProgramInference;

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

    /**
     * Provides utility method to substitute arguments for their type variables. Field should be
     * final, but can only be set in postInit, because subtypes might need other state to be
     * initialized first.
     */
    protected TypeVariableSubstitutor typeVarSubstitutor;

    /** Provides utility method to infer type arguments */
    protected TypeArgumentInference typeArgumentInference;

    /**
     * To cache the supported type qualifiers. call {@link #getSupportedTypeQualifiers()} instead of
     * using this field directly, as it may not have been initialized.
     */
    private final Set<Class<? extends Annotation>> supportedQuals;

    /** Types read from stub files (but not those from the annotated JDK jar file). */
    // Initially null, then assigned in postInit().  Caching is enabled as
    // soon as this is non-null, so it should be first set to its final
    // value, not initialized to an empty map that is incrementally filled.
    private Map<Element, AnnotatedTypeMirror> typesFromStubFiles;

    /**
     * Declaration annotations read from stub files (but not those from the annotated JDK jar file).
     * Map keys cannot be Element, because a different Element appears in the stub files than in the
     * real files. So, map keys are the verbose element name, as returned by
     * ElementUtils.getVerboseName.
     */
    // Not final, because it is assigned in postInit().
    private Map<String, Set<AnnotationMirror>> declAnnosFromStubFiles;

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

    /**
     * Map from class name (canonical name) of an annotation, to the annotation in the Checker
     * Framework that will be used in its place.
     */
    private final Map<String, AnnotationMirror> aliases = new HashMap<String, AnnotationMirror>();

    /**
     * A map from the class name (canonical name) of an annotation to the set of class names
     * (canonical names) for annotations with the same meaning (i.e., aliases), as well as the
     * annotation mirror that should be used.
     */
    private final Map<String, Pair<AnnotationMirror, Set</*@Interned*/ String>>> declAliases =
            new HashMap<>();

    /** Unique ID counter; for debugging purposes. */
    private static int uidCounter = 0;

    /** Unique ID of the current object; for debugging purposes. */
    public final int uid;

    /** Annotation added to every method defined in a class file that is not in a stub file. */
    private final AnnotationMirror fromByteCode;

    /** Annotation added to every method defined in a stub file. */
    private final AnnotationMirror fromStubFile;

    /**
     * Object that is used to resolve reflective method calls, if reflection resolution is turned
     * on.
     */
    protected ReflectionResolver reflectionResolver;

    /** Annotated Type Loader used to load annotation classes via reflective lookup */
    protected AnnotationClassLoader loader;

    /** Indicates that the whole-program inference is on. */
    private final boolean infer;

    /**
     * Should results be cached? This means that ATM.deepCopy() will be called. ATM.deepCopy() used
     * to (and perhaps still does) side effect the ATM being copied. So setting this to false is not
     * equivalent to setting shouldReadCache to false.
     */
    public boolean shouldCache;

    /** Size of LRU cache if one isn't specified using the atfCacheSize option. */
    private static final int DEFAULT_CACHE_SIZE = 300;

    /** Mapping from a Tree to its annotated type; implicits have been applied. */
    private final Map<Tree, AnnotatedTypeMirror> classAndMethodTreeCache;

    /**
     * Mapping from a Tree to its annotated type; before implicits are applied, just what the
     * programmer wrote.
     */
    protected final Map<Tree, AnnotatedTypeMirror> fromTreeCache;

    /**
     * Mapping from an Element to its annotated type; before implicits are applied, just what the
     * programmer wrote.
     */
    private final Map<Element, AnnotatedTypeMirror> elementCache;

    /** Mapping from an Element to the source Tree of the declaration. */
    private final Map<Element, Tree> elementToTreeCache;

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

        this.loader = new AnnotationClassLoader(checker);
        this.supportedQuals = new HashSet<>();

        this.fromByteCode = AnnotationUtils.fromClass(elements, FromByteCode.class);
        this.fromStubFile = AnnotationUtils.fromClass(elements, FromStubFile.class);

        this.cacheDeclAnnos = new HashMap<Element, Set<AnnotationMirror>>();

        this.shouldCache = !checker.hasOption("atfDoNotCache");
        if (shouldCache) {
            int cacheSize = getCacheSize();
            this.classAndMethodTreeCache = CollectionUtils.createLRUCache(cacheSize);
            this.fromTreeCache = CollectionUtils.createLRUCache(cacheSize);
            this.elementCache = CollectionUtils.createLRUCache(cacheSize);
            this.elementToTreeCache = CollectionUtils.createLRUCache(cacheSize);
        } else {
            this.classAndMethodTreeCache = null;
            this.fromTreeCache = null;
            this.elementCache = null;
            this.elementToTreeCache = null;
        }

        this.typeFormatter = createAnnotatedTypeFormatter();
        this.annotationFormatter = createAnnotationFormatter();

        infer = checker.hasOption("infer");
        if (infer) {
            checkInvalidOptionsInferSignatures();
            wholeProgramInference =
                    new WholeProgramInferenceScenes(
                            !"NullnessAnnotatedTypeFactory"
                                    .equals(this.getClass().getSimpleName()));
        }
    }

    /**
     * Issue an error and abort if any of the support qualifiers has a @Target meta-annotation that
     * contain something besides TYPE_USE or TYPE_PARAMETER. (@Target({}) is allowed)
     */
    private void checkSupportedQuals() {
        boolean hasPolyAll = false;
        boolean hasPolymorphicQualifier = false;
        for (Class<? extends Annotation> annotationClass : supportedQuals) {
            // Check @Target values
            ElementType[] elements = annotationClass.getAnnotation(Target.class).value();
            List<ElementType> otherElementTypes = new ArrayList<>();
            for (ElementType element : elements) {
                if (!(element.equals(ElementType.TYPE_USE)
                        || element.equals(ElementType.TYPE_PARAMETER))) {
                    // if there's an ElementType with an enumerated value of something other
                    // than TYPE_USE or TYPE_PARAMETER then it isn't a valid qualifier
                    otherElementTypes.add(element);
                }
            }
            if (!otherElementTypes.isEmpty()) {
                StringBuffer buf =
                        new StringBuffer("The @Target meta-annotation on type qualifier ");
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
                ErrorReporter.errorAbort(buf.toString());
            }
            // Check for PolyAll
            if (annotationClass.equals(PolyAll.class)) {
                hasPolyAll = true;
            } else if (annotationClass.getAnnotation(PolymorphicQualifier.class) != null) {
                hasPolymorphicQualifier = true;
            }
        }

        if (hasPolyAll && !hasPolymorphicQualifier) {
            ErrorReporter.errorAbort(
                    "Checker added @PolyAll to list of supported qualifiers, but "
                            + "the checker does not have a polymorphic qualifier.  Either remove "
                            + "@PolyAll from the list of supported qualifiers or add a polymorphic "
                            + "qualifier.");
        }
    }

    /**
     * This method is called only when {@code -Ainfer} is passed as an option. It checks if another
     * option that should not occur simultaneously with the whole-program inference is also passed
     * as argument, and aborts the process if that is the case. For example, the whole-program
     * inference process was not designed to work with unchecked code defaults. (Subclasses may
     * override this method to add more options.)
     */
    protected void checkInvalidOptionsInferSignatures() {
        // See Issue 683
        // https://github.com/typetools/checker-framework/issues/683
        if (checker.useUncheckedCodeDefault("source")
                || checker.useUncheckedCodeDefault("bytecode")) {
            ErrorReporter.errorAbort(
                    "The option -Ainfer cannot be"
                            + " used together with unchecked code defaults.");
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
            ErrorReporter.errorAbort(
                    "AnnotatedTypeFactory with null qualifier hierarchy not supported.");
        }
        this.typeHierarchy = createTypeHierarchy();
        this.typeVarSubstitutor = createTypeVariableSubstitutor();
        this.typeArgumentInference = createTypeArgumentInference();

        // TODO: is this the best location for declaring this alias?
        addAliasedDeclAnnotation(
                org.jmlspecs.annotation.Pure.class,
                org.checkerframework.dataflow.qual.Pure.class,
                AnnotationUtils.fromClass(elements, org.checkerframework.dataflow.qual.Pure.class));

        addInheritedAnnotation(
                AnnotationUtils.fromClass(elements, org.checkerframework.dataflow.qual.Pure.class));
        addInheritedAnnotation(
                AnnotationUtils.fromClass(
                        elements, org.checkerframework.dataflow.qual.SideEffectFree.class));
        addInheritedAnnotation(
                AnnotationUtils.fromClass(
                        elements, org.checkerframework.dataflow.qual.Deterministic.class));
        addInheritedAnnotation(
                AnnotationUtils.fromClass(
                        elements, org.checkerframework.dataflow.qual.TerminatesExecution.class));

        initializeReflectionResolution();

        if (this.getClass().equals(AnnotatedTypeFactory.class)) {
            this.parseStubFiles();
        }
    }

    /** Returns the WholeProgramInference instance. */
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

    // TODO: document
    // Set the CompilationUnitTree that should be used.
    // What's a better name? Maybe "reset" or "start"?
    public void setRoot(/*@Nullable*/ CompilationUnitTree root) {
        this.root = root;
        treePathCache.clear();
        pathHack.clear();

        if (shouldCache) {
            // Clear the caches with trees because once the compilation unit changes,
            // the trees may be modified and lose type arguments.
            elementToTreeCache.clear();
            fromTreeCache.clear();
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
     * @return an annotation relation tree representing the supported qualifiers
     */
    protected static QualifierHierarchy createQualifierHierarchy(
            Elements elements,
            Set<Class<? extends Annotation>> supportedTypeQualifiers,
            MultiGraphFactory factory) {

        for (Class<? extends Annotation> typeQualifier : supportedTypeQualifiers) {
            AnnotationMirror typeQualifierAnno = AnnotationUtils.fromClass(elements, typeQualifier);
            assert typeQualifierAnno != null
                    : "Loading annotation \"" + typeQualifier + "\" failed!";
            factory.addQualifier(typeQualifierAnno);
            // Polymorphic qualifiers can't declare their supertypes.
            // An error is raised if one is present.
            if (typeQualifier.getAnnotation(PolymorphicQualifier.class) != null) {
                if (typeQualifier.getAnnotation(SubtypeOf.class) != null) {
                    // This is currently not supported. At some point we might add
                    // polymorphic qualifiers with upper and lower bounds.
                    ErrorReporter.errorAbort(
                            "AnnotatedTypeFactory: "
                                    + typeQualifier
                                    + " is polymorphic and specifies super qualifiers. "
                                    + "Remove the @org.checkerframework.framework.qual.SubtypeOf or @org.checkerframework.framework.qual.PolymorphicQualifier annotation from it.");
                }
                continue;
            }
            if (typeQualifier.getAnnotation(SubtypeOf.class) == null) {
                ErrorReporter.errorAbort(
                        "AnnotatedTypeFactory: "
                                + typeQualifier
                                + " does not specify its super qualifiers. "
                                + "Add an @org.checkerframework.framework.qual.SubtypeOf annotation to it.");
            }
            Class<? extends Annotation>[] superQualifiers =
                    typeQualifier.getAnnotation(SubtypeOf.class).value();
            for (Class<? extends Annotation> superQualifier : superQualifiers) {
                if (!supportedTypeQualifiers.contains(superQualifier)) {
                    continue;
                }
                AnnotationMirror superAnno = null;
                superAnno = AnnotationUtils.fromClass(elements, superQualifier);
                factory.addSubtype(typeQualifierAnno, superAnno);
            }
        }

        QualifierHierarchy hierarchy = factory.build();

        if (!hierarchy.isValid()) {
            ErrorReporter.errorAbort(
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
     * Creates the type subtyping checker using the current type qualifier hierarchy.
     *
     * <p>Subclasses may override this method to specify new type-checking rules beyond the typical
     * java subtyping rules.
     *
     * @return the type relations class to check type subtyping
     */
    protected TypeHierarchy createTypeHierarchy() {
        return new DefaultTypeHierarchy(
                checker,
                getQualifierHierarchy(),
                checker.getOption("ignoreRawTypeArguments", "true").equals("true"),
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
        return new DefaultTypeArgumentInference();
    }

    public TypeArgumentInference getTypeArgumentInference() {
        return typeArgumentInference;
    }

    /**
     * Returns a mutable set of annotation classes that are supported by a checker
     *
     * <p>Subclasses may override this method and to return a mutable set of their supported type
     * qualifiers through one of the 5 approaches shown below.
     *
     * <p>Subclasses should not call this method; they should call {@link
     * #getSupportedTypeQualifiers} instead.
     *
     * <p>By default, a checker supports {@link PolyAll}, and all annotations located in a
     * subdirectory called {@literal qual} that's located in the same directory as the checker. Note
     * that only annotations defined with the {@code @Target({ElementType.TYPE_USE})}
     * meta-annotation (and optionally with the additional value of {@code
     * ElementType.TYPE_PARAMETER}, but no other {@code ElementType} values) are automatically
     * considered as supported annotations.
     *
     * <p>Annotations located outside the {@literal qual} subdirectory, or has other {@code
     * ElementType} values must be explicitly listed in code by overriding the {@link
     * #createSupportedTypeQualifiers()} method, as shown below.
     *
     * <p>Lastly, for checkers that do not want to support {@link PolyAll}, it must also be
     * explicitly written in code, as shown below.
     *
     * <p>In total, there are 5 ways to indicate annotations that are supported by a checker:
     *
     * <ol>
     *   <li>Only support annotations located in a checker's {@literal qual} directory, and {@link
     *       PolyAll}:
     *       <p>This is the default behavior. Simply place those annotations within the {@literal
     *       qual} directory.
     *   <li>Support annotations located in a checker's {@literal qual} directory, but without
     *       {@link PolyAll}:
     *       <p>Place those annotations within the {@literal qual} directory, and override {@link
     *       #createSupportedTypeQualifiers()} by calling {@link
     *       #getBundledTypeQualifiersWithPolyAll(Class...)} with no parameters passed in. Code
     *       example:
     *       <pre>
     * {@code @Override protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
     *      return getBundledTypeQualifiersWithoutPolyAll();
     *  } }
     * </pre>
     *   <li>Support annotations located in a checker's {@literal qual} directory, {@link PolyAll},
     *       and a list of other annotations:
     *       <p>Place those annotations within the {@literal qual} directory, and override {@link
     *       #createSupportedTypeQualifiers()} by calling {@link
     *       #getBundledTypeQualifiersWithPolyAll(Class...)} with a varargs parameter list of the
     *       other annotations. Code example:
     *       <pre>
     * {@code @Override protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
     *      return getBundledTypeQualifiersWithPolyAll(Regex.class, PartialRegex.class, RegexBottom.class, UnknownRegex.class);
     *  } }
     * </pre>
     *   <li>Support annotations located in a checker's {@literal qual} directory and a list of
     *       other annotations, but without supporting {@link PolyAll}:
     *       <p>Place those annotations within the {@literal qual} directory, and override {@link
     *       #createSupportedTypeQualifiers()} by calling {@link
     *       #getBundledTypeQualifiersWithoutPolyAll(Class...)} with a varargs parameter list of the
     *       other annotations. Code example:
     *       <pre>
     * {@code @Override protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
     *      return getBundledTypeQualifiersWithoutPolyAll(UnknownFormat.class, FormatBottom.class);
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
     *       fresh, mutable set. The methods {@link
     *       #getBundledTypeQualifiersWithoutPolyAll(Class...)} and {@link
     *       #getBundledTypeQualifiersWithPolyAll(Class...)} each must return a fresh, mutable set
     * </ol>
     *
     * @return the type qualifiers supported this processor, or an empty set if none
     */
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiersWithPolyAll();
    }

    /**
     * Loads all annotations contained in the qual directory of a checker via reflection, and adds
     * {@link PolyAll}, if a polymorphic type qualifier exists, and an explicit array of annotations
     * to the set of annotation classes.
     *
     * <p>This method can be called in the overridden versions of {@link
     * #createSupportedTypeQualifiers()} in each checker.
     *
     * @param explicitlyListedAnnotations a varargs array of explicitly listed annotation classes to
     *     be added to the returned set. For example, it is used frequently to add Bottom
     *     qualifiers.
     * @return a mutable set of the loaded and listed annotation classes, as well as {@link
     *     PolyAll}.
     */
    @SafeVarargs
    protected final Set<Class<? extends Annotation>> getBundledTypeQualifiersWithPolyAll(
            Class<? extends Annotation>... explicitlyListedAnnotations) {
        Set<Class<? extends Annotation>> annotations =
                loadTypeAnnotationsFromQualDir(explicitlyListedAnnotations);
        boolean addPolyAll = false;
        for (Class<? extends Annotation> annotationClass : annotations) {
            if (annotationClass.getAnnotation(PolymorphicQualifier.class) != null) {
                addPolyAll = true;
                break;
            }
        }
        if (addPolyAll) {
            annotations.add(PolyAll.class);
        }
        return annotations;
    }

    /**
     * Loads all annotations contained in the qual directory of a checker via reflection, and an
     * explicit list of annotations to the set of annotation classes.
     *
     * <p>This method can be called in the overridden versions of {@link
     * #createSupportedTypeQualifiers()} in each checker.
     *
     * @param explicitlyListedAnnotations a varargs array of explicitly listed annotation classes to
     *     be added to the returned set. For example, it is used frequently to add Bottom
     *     qualifiers.
     * @return a mutable set of the loaded, and listed annotation classes
     */
    @SafeVarargs
    protected final Set<Class<? extends Annotation>> getBundledTypeQualifiersWithoutPolyAll(
            Class<? extends Annotation>... explicitlyListedAnnotations) {
        return loadTypeAnnotationsFromQualDir(explicitlyListedAnnotations);
    }

    /**
     * Loads all annotations contained in the qual directory of a checker via reflection, and has
     * the option to include an explicitly stated list of annotations (eg ones found in a different
     * directory than qual).
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
        // add the loaded annotations to the annotation set
        Set<Class<? extends Annotation>> annotations = loader.getLoadedAnnotationClasses();

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
        return new DefaultAnnotatedTypeFormatter(
                checker.hasOption("printVerboseGenerics"), checker.hasOption("printAllQualifiers"));
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
     * Returns an immutable set of the type qualifiers supported by this checker.
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

    // **********************************************************************
    // Factories for annotated types that account for implicit qualifiers
    // **********************************************************************

    /** Mapping from a Tree to its TreePath */
    private final TreePathCacher treePathCache = new TreePathCacher();

    /**
     * Returns the int supplied to the checker via the atfCacheSize option or the default cache
     * size.
     *
     * @return cache size passed as argument to checker or DEFAULT_CACHE_SIZE
     */
    private int getCacheSize() {
        String option = checker.getOption("atfCacheSize");
        if (option == null) {
            return DEFAULT_CACHE_SIZE;
        }
        try {
            return Integer.valueOf(option);
        } catch (NumberFormatException ex) {
            ErrorReporter.errorAbort("atfCacheSize was not an integer: " + option);
            return 0; // dead code
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
            ErrorReporter.errorAbort("AnnotatedTypeFactory.getAnnotatedType: null element");
            return null; // dead code
        }
        // Annotations explicitly written in the source code,
        // or obtained from bytecode.
        AnnotatedTypeMirror type = fromElement(elt);
        // Implicits due to writing annotation on the class declaration.
        annotateInheritedFromClass(type);
        addComputedTypeAnnotations(elt, type);
        return type;
    }

    @Override
    public AnnotationMirror getAnnotationMirror(Tree tree, Class<? extends Annotation> target) {
        AnnotationMirror mirror = AnnotationUtils.fromClass(elements, target);
        if (isSupportedQualifier(mirror)) {
            AnnotatedTypeMirror atm = getAnnotatedType(tree);
            return atm.getAnnotation(target);
        }
        return null;
    }

    /**
     * Returns an AnnotatedTypeMirror representing the annotated type of {@code tree}.
     *
     * <p>
     *
     * @param tree the AST node
     * @return the annotated type of {@code tree}
     */
    public AnnotatedTypeMirror getAnnotatedType(Tree tree) {
        if (tree == null) {
            ErrorReporter.errorAbort("AnnotatedTypeFactory.getAnnotatedType: null tree");
            return null; // dead code
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
            tree = TreeUtils.skipParens((ExpressionTree) tree);
            type = fromExpression((ExpressionTree) tree);
        } else {
            ErrorReporter.errorAbort(
                    "AnnotatedTypeFactory.getAnnotatedType: query of annotated type for tree "
                            + tree.getKind());
            type = null; // dead code
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
     * @param classTree ClassTree on which to perfrom preprocessing
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
        if (checker.hasOption("infer") && wholeProgramInference != null) {
            // Write scenes into .jaif files. In order to perform the write
            // operation only once for each .jaif file, the best location to
            // do so is here.
            wholeProgramInference.saveResults();
        }
    }

    /**
     * Determines the annotated type from a type in tree form.
     *
     * <p>Note that we cannot decide from a Tree whether it is a type use or an expression.
     * TreeUtils.isTypeTree is only an under-approximation. For example, an identifier can be either
     * a type or an expression.
     *
     * <p>
     *
     * @param tree the type tree
     * @return the annotated type of the type in the AST
     */
    public AnnotatedTypeMirror getAnnotatedTypeFromTypeTree(Tree tree) {
        if (tree == null) {
            ErrorReporter.errorAbort(
                    "AnnotatedTypeFactory.getAnnotatedTypeFromTypeTree: null tree");
            return null; // dead code
        }
        AnnotatedTypeMirror type = fromTypeTree(tree);
        addComputedTypeAnnotations(tree, type);
        return type;
    }

    // **********************************************************************
    // Factories for annotated types that do not account for implicit qualifiers.
    // They only include qualifiers explicitly inserted by the user.
    // **********************************************************************

    /**
     * Creates an AnnotatedTypeMirror for {@code elt} that includes: annotations explicitly written
     * on the element and annotations from stub files
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
        // the annotations have to be retrived from the tree so that only explicit annotations are returned.
        Tree decl = declarationFromElement(elt);

        if (decl == null && typesFromStubFiles != null && typesFromStubFiles.containsKey(elt)) {
            type = typesFromStubFiles.get(elt).deepCopy();
        } else if (decl == null
                && (typesFromStubFiles == null || !typesFromStubFiles.containsKey(elt))) {
            type = toAnnotatedType(elt.asType(), ElementUtils.isTypeDeclaration(elt));
            ElementAnnotationApplier.apply(type, elt, this);

            if (elt instanceof ExecutableElement || elt instanceof VariableElement) {
                annotateInheritedFromClass(type);
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
            ErrorReporter.errorAbort(
                    "AnnotatedTypeFactory.fromElement: cannot be here! decl: "
                            + decl.getKind()
                            + " elt: "
                            + elt);
            type = null; // dead code
        }

        // Caching is disabled if typesFromStubFiles == null, because calls to this
        // method before the stub files are fully read can return incorrect
        // results.
        if (shouldCache && typesFromStubFiles != null) {
            elementCache.put(elt, type.deepCopy());
        }
        return type;
    }

    /**
     * Adds @FromByteCode to methods, constructors, and fields declared in class files that are not
     * already annotated with @FromStubFile
     */
    private void addFromByteCode(Element elt) {
        if (declAnnosFromStubFiles == null) { // || trees.getTree(elt) != null) {
            // Parsing stub files, don't add @FromByteCode
            return;
        }

        if (elt.getKind() == ElementKind.CONSTRUCTOR
                || elt.getKind() == ElementKind.METHOD
                || elt.getKind() == ElementKind.FIELD) {
            // Only add @FromByteCode to methods, constructors, and fields
            if (ElementUtils.isElementFromByteCode(elt)) {
                Set<AnnotationMirror> annos =
                        declAnnosFromStubFiles.get(ElementUtils.getVerboseName(elt));
                if (annos == null) {
                    annos = AnnotationUtils.createAnnotationSet();
                    declAnnosFromStubFiles.put(ElementUtils.getVerboseName(elt), annos);
                }
                if (!AnnotationUtils.containsSameIgnoringValues(annos, fromStubFile)) {
                    annos.add(fromByteCode);
                }
            }
        }
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
     * AnnotatedTypeMirror contains annotations explicitly written on the tree and annotations
     * inherited from the class declarations {@link
     * #annotateInheritedFromClass(AnnotatedTypeMirror)}.
     *
     * <p>If a VariableTree is a parameter to a lambda, this method also adds annotations from the
     * declared type of the functional interface and the executable type of its method.
     *
     * @param tree MethodTree or VariableTree
     * @return AnnotatedTypeMirror with explicit annotations from {@code tree} and annotations
     *     inherited from class declarations
     */
    private final AnnotatedTypeMirror fromMember(Tree tree) {
        if (!(tree instanceof MethodTree || tree instanceof VariableTree)) {
            ErrorReporter.errorAbort(
                    "AnnotatedTypeFactory.fromMember: not a method or variable declaration: "
                            + tree);
            return null; // dead code
        }
        if (shouldCache && fromTreeCache.containsKey(tree)) {
            return fromTreeCache.get(tree).deepCopy();
        }
        AnnotatedTypeMirror result = TypeFromTree.fromMember(this, tree);
        annotateInheritedFromClass(result);
        if (shouldCache) {
            fromTreeCache.put(tree, result.deepCopy());
        }
        return result;
    }

    /**
     * Creates an AnnotatedTypeMirror for an ExpressionTree. The AnnotatedTypeMirror contains
     * explicit annotations written with on the expression, annotations inherited from class
     * declarations, and for some expressions, annotations from sub-expressions that could have been
     * explicitly written, implicited, defaulted, refined, or otherwise computed. (Expression whose
     * type include annotations from sub- expressions are: ArrayAccessTree,
     * ConditionalExpressionTree, IdentifierTree, MemberSelectTree, and MethodInvocationTree.)
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
        if (shouldCache && fromTreeCache.containsKey(tree)) {
            return fromTreeCache.get(tree).deepCopy();
        }

        AnnotatedTypeMirror result = TypeFromTree.fromExpression(this, tree);

        annotateInheritedFromClass(result);

        if (shouldCache) {
            fromTreeCache.put(tree, result.deepCopy());
        }
        return result;
    }

    /**
     * Creates an AnnotatedTypeMirror for the tree. The AnnotatedTypeMirror contains annotations
     * explicitly written on the tree and annotations inherited from class declarations {@link
     * #annotateInheritedFromClass(AnnotatedTypeMirror)}. It also adds type arguments to raw types
     * that include annotations from the element declaration of the type {@link
     * #fromElement(Element)}.
     *
     * <p>Called on the following trees: AnnotatedTypeTree, ArrayTypeTree, ParameterizedTypeTree,
     * PrimitiveTypeTree, TypeParameterTree, WildcardTree, UnionType, IntersectionTypeTree, and
     * IdentifierTree, MemberSelectTree.
     *
     * @param tree the type tree
     * @return the (partially) annotated type of the type in the AST
     */
    /*package private*/ final AnnotatedTypeMirror fromTypeTree(Tree tree) {
        if (shouldCache && fromTreeCache.containsKey(tree)) {
            return fromTreeCache.get(tree).deepCopy();
        }

        AnnotatedTypeMirror result = TypeFromTree.fromTypeTree(this, tree);

        annotateInheritedFromClass(result);
        if (shouldCache) {
            fromTreeCache.put(tree, result.deepCopy());
        }
        return result;
    }

    // **********************************************************************
    // Customization methods meant to be overridden by subclasses to include
    // implicit annotations
    // **********************************************************************

    /**
     * Adds implicit annotations to a type obtained from a {@link Tree}. By default, this method
     * does nothing. Subclasses should use this method to implement implicit annotations specific to
     * their type systems.
     *
     * @param tree an AST node
     * @param type the type obtained from {@code tree}
     */
    protected void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type) {
        // Pass.
    }

    /**
     * Adds implicit annotations to a type obtained from a {@link Element}. By default, this method
     * does nothing. Subclasses should use this method to implement implicit annotations specific to
     * their type systems.
     *
     * @param elt an element
     * @param type the type obtained from {@code elt}
     */
    protected void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
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
        addComputedTypeAnnotations(element, type);
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

        // System.err.printf("TVFU\n  type: %s\n  generic: %s\n", type, generic);

        Map<TypeVariable, AnnotatedTypeMirror> mapping = new HashMap<>();

        for (int i = 0; i < targs.size(); ++i) {
            mapping.put(((AnnotatedTypeVariable) tvars.get(i)).getUnderlyingType(), targs.get(i));
        }

        List<AnnotatedTypeParameterBounds> res = new LinkedList<>();

        for (AnnotatedTypeMirror atm : tvars) {
            AnnotatedTypeVariable atv = (AnnotatedTypeVariable) atm;
            AnnotatedTypeMirror upper = typeVarSubstitutor.substitute(mapping, atv.getUpperBound());
            AnnotatedTypeMirror lower = typeVarSubstitutor.substitute(mapping, atv.getLowerBound());
            res.add(new AnnotatedTypeParameterBounds(upper, lower));
        }
        return res;
    }

    /**
     * Adds annotations to the type based on the annotations from its class type if and only if no
     * annotations are already present on the type.
     *
     * <p>The class type is found using {@link #fromElement(Element)}
     *
     * @param type the type for which class annotations will be inherited if there are no
     *     annotations already present
     */
    protected void annotateInheritedFromClass(AnnotatedTypeMirror type) {
        InheritedFromClassAnnotator.INSTANCE.visit(type, this);
    }

    /** Callback to determine what to do with the annotations from a class declaration. */
    protected void annotateInheritedFromClass(
            AnnotatedTypeMirror type, Set<AnnotationMirror> fromClass) {
        type.addMissingAnnotations(fromClass);
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

    /**
     * A singleton utility class for pulling annotations down from a class type.
     *
     * @see #annotateInheritedFromClass
     */
    protected static class InheritedFromClassAnnotator
            extends AnnotatedTypeScanner<Void, AnnotatedTypeFactory> {

        /** The singleton instance. */
        public static final InheritedFromClassAnnotator INSTANCE =
                new InheritedFromClassAnnotator();

        private InheritedFromClassAnnotator() {}

        @Override
        public Void visitExecutable(AnnotatedExecutableType type, AnnotatedTypeFactory p) {

            // When visiting an executable type, skip the receiver so we
            // never inherit class annotations there.

            // Also skip constructor return types (which somewhat act like
            // the receiver).
            MethodSymbol methodElt = (MethodSymbol) type.getElement();
            if (methodElt == null || !methodElt.isConstructor()) {
                scan(type.getReturnType(), p);
            }

            scanAndReduce(type.getParameterTypes(), p, null);
            scanAndReduce(type.getThrownTypes(), p, null);
            scanAndReduce(type.getTypeVariables(), p, null);
            return null;
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, AnnotatedTypeFactory p) {
            Element classElt = type.getUnderlyingType().asElement();

            // Only add annotations from the class declaration if there
            // are no annotations from that hierarchy already on the type.

            if (classElt != null) {
                AnnotatedTypeMirror classType = p.fromElement(classElt);
                assert classType != null : "Unexpected null type for class element: " + classElt;

                p.annotateInheritedFromClass(type, classType.getAnnotations());
            }

            return super.visitDeclared(type, p);
        }

        private final Map<TypeParameterElement, AnnotatedTypeVariable> visited =
                new HashMap<TypeParameterElement, AnnotatedTypeVariable>();

        @Override
        public Void visitTypeVariable(AnnotatedTypeVariable type, AnnotatedTypeFactory p) {
            TypeParameterElement tpelt =
                    (TypeParameterElement) type.getUnderlyingType().asElement();
            if (!visited.containsKey(tpelt)) {
                visited.put(tpelt, type);
                if (type.getAnnotations().isEmpty()
                        && type.getUpperBound().getAnnotations().isEmpty()
                        && tpelt.getEnclosingElement().getKind() != ElementKind.TYPE_PARAMETER) {
                    ElementAnnotationApplier.apply(type, tpelt, p);
                }
                super.visitTypeVariable(type, p);
                visited.remove(tpelt);
            }
            return null;
        }

        @Override
        public void reset() {
            visited.clear();
            super.reset();
        }
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

        Element element = InternalUtils.symbol(tree);
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
                    // This only arises in the Nullness Checker when substituting rawness.
                    return null;
                }
                TypeElement typeElt = ElementUtils.enclosingClass(element);
                if (typeElt == null) {
                    ErrorReporter.errorAbort(
                            "AnnotatedTypeFactory.getImplicitReceiver: enclosingClass()==null for element: "
                                    + element);
                }
                // TODO: method receiver annotations on outer this
                return getEnclosingType(typeElt, tree);
            }
        }

        Element rcvelem = InternalUtils.symbol(receiver);
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
            ErrorReporter.errorAbort(
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
     * expression like "f.g" and want to know whether it is an access "this.f.g" or whether e.g. f
     * is a field of an outer class or e.g. f is a local variable.
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

            tree = TreeUtils.skipParens(tree);

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
     * Returns the type of {@code this} in the current location, which can be used if {@code this}
     * has a special semantics (e.g. {@code this} is non-null).
     *
     * <p>The parameter is an arbitrary tree and does not have to mention "this", neither explicitly
     * nor implicitly. This method should be overridden for type-system specific behavior.
     *
     * <p>TODO: in 1.8.2, handle all receiver type annotations. TODO: handle enclosing classes
     * correctly.
     */
    public AnnotatedDeclaredType getSelfType(Tree tree) {
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
     * org.checkerframework.common.basetype.BaseTypeVisitor#checkTypeArguments(Tree, List, List,
     * List)} for the checks of type argument well-formedness.
     *
     * <p>Note that "this" and "super" constructor invocations are also handled by this method.
     * Method {@link #constructorFromUse(NewClassTree)} is only used for a constructor invocation in
     * a "new" expression.
     *
     * @param tree the method invocation tree
     * @return the method type being invoked with tree and the (inferred) type arguments
     */
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(
            MethodInvocationTree tree) {
        ExecutableElement methodElt = TreeUtils.elementFromUse(tree);
        AnnotatedTypeMirror receiverType = getReceiverType(tree);

        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair =
                methodFromUse(tree, methodElt, receiverType);
        if (checker.shouldResolveReflection()
                && reflectionResolver.isReflectiveMethodInvocation(tree)) {
            mfuPair = reflectionResolver.resolveReflectiveCall(this, tree, mfuPair);
        }
        return mfuPair;
    }

    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(
            ExpressionTree tree, ExecutableElement methodElt, AnnotatedTypeMirror receiverType) {

        AnnotatedExecutableType methodType =
                AnnotatedTypes.asMemberOf(types, this, receiverType, methodElt);
        List<AnnotatedTypeMirror> typeargs = new LinkedList<AnnotatedTypeMirror>();

        Map<TypeVariable, AnnotatedTypeMirror> typeVarMapping =
                AnnotatedTypes.findTypeArguments(processingEnv, this, tree, methodElt, methodType);

        if (!typeVarMapping.isEmpty()) {
            for (AnnotatedTypeVariable tv : methodType.getTypeVariables()) {
                if (typeVarMapping.get(tv.getUnderlyingType()) == null) {
                    ErrorReporter.errorAbort(
                            "AnnotatedTypeFactory.methodFromUse:"
                                    + "mismatch between declared method type variables and the inferred method type arguments! "
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
                && TreeUtils.isGetClassInvocation((MethodInvocationTree) tree)) {
            adaptGetClassReturnTypeToReceiver(methodType, receiverType);
        }

        return Pair.of(methodType, typeargs);
    }

    /**
     * Java special cases the return type of getClass. Though the method has a return type of {@code
     * Class<?>}, the compiler special cases this return type and changes the bound of the type
     * argument to the erasure of the receiver type. e.g.,
     *
     * <ul>
     *   <li>x.getClass() has the type {@code Class< ? extends erasure_of_x >}
     *   <li>someInteger.getClass() has the type {@code Class< ? extends Integer >}
     * </ul>
     *
     * @param getClassType this must be a type representing a call to Object.getClass otherwise a
     *     runtime exception will be thrown
     * @param receiverType the receiver type of the method invocation (not the declared receiver
     *     type)
     */
    protected static void adaptGetClassReturnTypeToReceiver(
            final AnnotatedExecutableType getClassType, final AnnotatedTypeMirror receiverType) {
        final AnnotatedTypeMirror newBound = receiverType.getErased();

        final AnnotatedTypeMirror returnType = getClassType.getReturnType();
        if (returnType == null
                || !(returnType.getKind() == TypeKind.DECLARED)
                || ((AnnotatedDeclaredType) returnType).getTypeArguments().size() != 1) {
            ErrorReporter.errorAbort(
                    "Unexpected type passed to AnnotatedTypes.adaptGetClassReturnTypeToReceiver\n"
                            + "getClassType="
                            + getClassType
                            + "\n"
                            + "receiverType="
                            + receiverType);
        }

        final AnnotatedDeclaredType returnAdt =
                (AnnotatedDeclaredType) getClassType.getReturnType();
        final List<AnnotatedTypeMirror> typeArgs = returnAdt.getTypeArguments();

        // usually, the only locations that will add annotations to the return type are getClass in stub files
        // defaults and propagation tree annotator.  Since getClass is final they cannot come from source code.
        // Also, since the newBound is an erased type we have no type arguments.  So, we just copy the annotations
        // from the bound of the declared type to the new bound.
        final AnnotatedWildcardType classWildcardArg = (AnnotatedWildcardType) typeArgs.get(0);
        newBound.replaceAnnotations(classWildcardArg.getExtendsBound().getAnnotations());

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
     * org.checkerframework.common.basetype.BaseTypeVisitor#checkTypeArguments(Tree, List, List,
     * List)} for the checks of type argument well-formedness.
     *
     * <p>Note that "this" and "super" constructor invocations are handled by method {@link
     * #methodFromUse}. This method only handles constructor invocations in a "new" expression.
     *
     * @param tree the constructor invocation tree
     * @return the annotated type of the invoked constructor (as an executable type) and the
     *     (inferred) type arguments
     */
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> constructorFromUse(
            NewClassTree tree) {
        ExecutableElement ctor = InternalUtils.constructor(tree);
        AnnotatedTypeMirror type = fromNewClass(tree);
        addComputedTypeAnnotations(tree.getIdentifier(), type);
        AnnotatedExecutableType con = AnnotatedTypes.asMemberOf(types, this, type, ctor);

        if (tree.getArguments().size() == con.getParameterTypes().size() + 1
                && isSyntheticArgument(tree.getArguments().get(0))) {
            // happens for anonymous constructors of inner classes
            List<AnnotatedTypeMirror> actualParams = new ArrayList<AnnotatedTypeMirror>();
            actualParams.add(getAnnotatedType(tree.getArguments().get(0)));
            actualParams.addAll(con.getParameterTypes());
            con.setParameterTypes(actualParams);
        }

        List<AnnotatedTypeMirror> typeargs = new LinkedList<AnnotatedTypeMirror>();

        Map<TypeVariable, AnnotatedTypeMirror> typeVarMapping =
                AnnotatedTypes.findTypeArguments(processingEnv, this, tree, ctor, con);

        if (!typeVarMapping.isEmpty()) {
            for (AnnotatedTypeVariable tv : con.getTypeVariables()) {
                typeargs.add(typeVarMapping.get(tv.getUnderlyingType()));
            }
            con = (AnnotatedExecutableType) typeVarSubstitutor.substitute(typeVarMapping, con);
        }

        return Pair.of(con, typeargs);
    }

    /** Returns the return type of the method {@code m}. */
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
     * Creates an AnnotatedDeclaredType for a NewClassTree. Adds explicit annotations and
     * annotations inherited from class declarations {@link
     * #annotateInheritedFromClass(AnnotatedTypeMirror)}.
     *
     * <p>If the NewClassTree has type arguments, then any explicit (or inherited from class)
     * annotations on those type arguments are included. If the NewClassTree has a diamond operator,
     * then the annotations on the type arguments are inferred using the assignment context.
     *
     * <p>(Subclass beside {@link GenericAnnotatedTypeFactory} should not override this method.)
     *
     * @param newClassTree NewClassTree
     * @return AnnotatedDeclaredType
     */
    public AnnotatedDeclaredType fromNewClass(NewClassTree newClassTree) {
        if (TreeUtils.isDiamondTree(newClassTree)) {
            AnnotatedDeclaredType type =
                    (AnnotatedDeclaredType)
                            toAnnotatedType(InternalUtils.typeOf(newClassTree), false);
            if (((com.sun.tools.javac.code.Type) type.actualType)
                    .tsym
                    .getTypeParameters()
                    .nonEmpty()) {
                Pair<Tree, AnnotatedTypeMirror> ctx = this.visitorState.getAssignmentContext();
                if (ctx != null) {
                    AnnotatedTypeMirror ctxtype = ctx.second;
                    fromNewClassContextHelper(type, ctxtype);
                }
            }
            return type;
        } else if (newClassTree.getClassBody() != null) {
            AnnotatedDeclaredType type =
                    (AnnotatedDeclaredType)
                            toAnnotatedType(InternalUtils.typeOf(newClassTree), false);
            // If newClassTree creates an anonymous class, then annotations in this location:
            //   new @HERE Class() {}
            // are on not on the identifier newClassTree, but rather on the modifier newClassTree.
            List<? extends AnnotationTree> annos =
                    newClassTree.getClassBody().getModifiers().getAnnotations();
            type.addAnnotations(InternalUtils.annotationsFromTypeAnnotationTrees(annos));
            return type;
        } else {
            // If newClassTree does not create anonymous class,
            // newClassTree.getIdentifier includes the explicit annotations in this location:
            //   new @HERE Class()
            return (AnnotatedDeclaredType) fromTypeTree(newClassTree.getIdentifier());
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
                        if (!types.isSameType(
                                oldArgs.get(i).actualType, newArgs.get(i).actualType)) {
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
                    // See Issue 438. Ignore primitive types for diamond inference - a primitive type
                    // is never a suitable context anyways.
                } else {
                    ErrorReporter.errorAbort(
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
     * returns the annotated primitive type of the given declared type if it is a boxed declared
     * type. Otherwise, it throws <i>IllegalArgumentException</i> exception.
     *
     * <p>The returned type would have the annotations on the given type and nothing else.
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
        TypeMirror stringTypeMirror = TypesUtils.typeFromClass(types, elements, String.class);
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

    /** Returns the VisitorState instance used by the factory to infer types */
    public VisitorState getVisitorState() {
        return this.visitorState;
    }

    // **********************************************************************
    // random methods wrapping #getAnnotatedType(Tree) and #fromElement(Tree)
    // with appropriate casts to reduce casts on the client side
    // **********************************************************************

    /** @see #getAnnotatedType(Tree) */
    public final AnnotatedDeclaredType getAnnotatedType(ClassTree tree) {
        return (AnnotatedDeclaredType) getAnnotatedType((Tree) tree);
    }

    /** @see #getAnnotatedType(Tree) */
    public final AnnotatedDeclaredType getAnnotatedType(NewClassTree tree) {
        return (AnnotatedDeclaredType) getAnnotatedType((Tree) tree);
    }

    /** @see #getAnnotatedType(Tree) */
    public final AnnotatedArrayType getAnnotatedType(NewArrayTree tree) {
        return (AnnotatedArrayType) getAnnotatedType((Tree) tree);
    }

    /** @see #getAnnotatedType(Tree) */
    public final AnnotatedExecutableType getAnnotatedType(MethodTree tree) {
        return (AnnotatedExecutableType) getAnnotatedType((Tree) tree);
    }

    /** @see #getAnnotatedType(Element) */
    public final AnnotatedDeclaredType getAnnotatedType(TypeElement elt) {
        return (AnnotatedDeclaredType) getAnnotatedType((Element) elt);
    }

    /** @see #getAnnotatedType(Element) */
    public final AnnotatedExecutableType getAnnotatedType(ExecutableElement elt) {
        return (AnnotatedExecutableType) getAnnotatedType((Element) elt);
    }

    /** @see #fromElement(Element) */
    public final AnnotatedDeclaredType fromElement(TypeElement elt) {
        return (AnnotatedDeclaredType) fromElement((Element) elt);
    }

    /** @see #fromElement(Element) */
    public final AnnotatedExecutableType fromElement(ExecutableElement elt) {
        return (AnnotatedExecutableType) fromElement((Element) elt);
    }

    // **********************************************************************
    // Helper methods for this classes
    // **********************************************************************

    /**
     * Determines whether the given annotation is a part of the type system under which this type
     * factory operates. Null is never a supported qualifier; the parameter is nullable to allow the
     * result of aliasedAnnotation to be passed in directly.
     *
     * @param a any annotation
     * @return true if that annotation is part of the type system under which this type factory
     *     operates, false otherwise
     */
    public boolean isSupportedQualifier(/*@Nullable*/ AnnotationMirror a) {
        if (a == null) return false;
        return AnnotationUtils.containsSameIgnoringValues(
                this.getQualifierHierarchy().getTypeQualifiers(), a);
    }

    /** Add the annotation clazz as an alias for the annotation type. */
    protected void addAliasedAnnotation(Class<?> alias, AnnotationMirror type) {
        aliases.put(alias.getCanonicalName(), type);
    }

    /**
     * Returns the canonical annotation for the passed annotation if it is an alias of a canonical
     * one in the framework. If it is not an alias, the method returns null.
     *
     * <p>Returns an aliased type of the current one
     *
     * @param a the qualifier to check for an alias
     * @return the alias or null if none exists
     */
    public /*@Nullable*/ AnnotationMirror aliasedAnnotation(AnnotationMirror a) {
        TypeElement elem = (TypeElement) a.getAnnotationType().asElement();
        String qualName = elem.getQualifiedName().toString();
        return aliases.get(qualName);
    }

    /**
     * Add the annotation {@code alias} as an alias for the declaration annotation {@code
     * annotation}, where the annotation mirror {@code annoationToUse} will be used instead. If
     * multiple calls are made with the same {@code annotation}, then the {@code anontationToUse}
     * must be the same.
     */
    protected void addAliasedDeclAnnotation(
            Class<? extends Annotation> alias,
            Class<? extends Annotation> annotation,
            AnnotationMirror annotationToUse) {
        String aliasName = alias.getCanonicalName();
        /*@Interned*/ String annotationName = annotation.getCanonicalName();
        Set</*@Interned*/ String> set = new HashSet<>();
        if (declAliases.containsKey(annotationName)) {
            set.addAll(declAliases.get(annotationName).second);
        }
        set.add(aliasName.intern());
        declAliases.put(annotationName, Pair.of(annotationToUse, set));
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
     * <p>Most users will want to use getAnnotatedType instead; this method is mostly for internal
     * use.
     *
     * @param node the tree to analyze
     * @return the type of {@code node}, without any annotations
     */
    protected final AnnotatedTypeMirror type(Tree node) {
        boolean isDeclaration = TreeUtils.isTypeDeclaration(node);

        // Attempt to obtain the type via JCTree.
        if (InternalUtils.typeOf(node) != null) {
            AnnotatedTypeMirror result = toAnnotatedType(InternalUtils.typeOf(node), isDeclaration);
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
    protected final /*@Nullable*/ AnnotatedDeclaredType getCurrentMethodReceiver(Tree tree) {
        AnnotatedDeclaredType res = visitorState.getMethodReceiver();
        if (res == null) {
            TreePath path = getPath(tree);
            if (path != null) {
                MethodTree enclosingMethod = TreeUtils.enclosingMethod(path);
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
            ErrorReporter.errorAbort(
                    String.format(
                            "AnnotatedTypeFactory.getMostInnerClassOrMethod: getPath(tree)=>null%n  TreePath.getPath(root, tree)=>%s\n  for tree (%s) = %s%n  root=%s",
                            TreePath.getPath(root, tree), tree.getClass(), tree, root));
            return null; // dead code
        }
        for (Tree pathTree : path) {
            if (pathTree instanceof MethodTree) {
                return TreeUtils.elementFromDeclaration((MethodTree) pathTree);
            } else if (pathTree instanceof ClassTree) {
                return TreeUtils.elementFromDeclaration((ClassTree) pathTree);
            }
        }

        ErrorReporter.errorAbort("AnnotatedTypeFactory.getMostInnerClassOrMethod: cannot be here!");
        return null; // dead code
    }

    private final Map<Tree, Element> pathHack = new HashMap<>();

    public final void setPathHack(Tree node, Element enclosing) {
        pathHack.put(node, enclosing);
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
    public final TreePath getPath(Tree node) {
        assert root != null
                : "AnnotatedTypeFactory.getPath: root needs to be set when used on trees; factory: "
                        + this.getClass();

        if (node == null) return null;

        if (treePathCache.isCached(node)) {
            return treePathCache.getPath(root, node);
        }
        ;

        TreePath currentPath = visitorState.getPath();
        if (currentPath == null) {
            return TreePath.getPath(root, node);
        }

        // This method uses multiple heuristics to avoid calling
        // TreePath.getPath()

        // If the current path you are visiting is for this node we are done
        if (currentPath.getLeaf() == node) {
            return currentPath;
        }

        // When running on Daikon, we noticed that a lot of calls happened
        // within a small subtree containing the node we are currently visiting

        // When testing on Daikon, two steps resulted in the best performance
        if (currentPath.getParentPath() != null) {
            currentPath = currentPath.getParentPath();
        }
        if (currentPath.getLeaf() == node) {
            return currentPath;
        }
        if (currentPath.getParentPath() != null) {
            currentPath = currentPath.getParentPath();
        }
        if (currentPath.getLeaf() == node) {
            return currentPath;
        }

        final TreePath pathWithinSubtree = TreePath.getPath(currentPath, node);
        if (pathWithinSubtree != null) {
            return pathWithinSubtree;
        }

        // climb the current path till we see that
        // Works when getPath called on the enclosing method, enclosing
        // class
        TreePath current = currentPath;
        while (current != null) {
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
    public final Element getEnclosingMethod(Tree node) {
        return pathHack.get(node);
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
        if (type.getUnderlyingType() == null) {
            return true; // e.g., for receiver types
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
     * Parses the stub files in the following order: <br>
     *
     * <ol>
     *   <li>jdk.astub in the same directory as the checker, if it exists and ignorejdkastub option
     *       is not supplied <br>
     *   <li>flow.astub in the same directory as BaseTypeChecker <br>
     *   <li>Stub files listed in @Stubfiles annotation on the checker; must be in same directory as
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
        if (this.typesFromStubFiles != null || this.declAnnosFromStubFiles != null) {
            ErrorReporter.errorAbort("AnnotatedTypeFactory.parseStubFiles called more than once");
        }

        Map<Element, AnnotatedTypeMirror> typesFromStubFiles =
                new HashMap<Element, AnnotatedTypeMirror>();
        Map<String, Set<AnnotationMirror>> declAnnosFromStubFiles =
                new HashMap<String, Set<AnnotationMirror>>();

        // 1. jdk.astub
        if (!checker.hasOption("ignorejdkastub")) {
            InputStream in = null;
            in = checker.getClass().getResourceAsStream("jdk.astub");
            if (in != null) {
                StubParser stubParser = new StubParser("jdk.astub", in, this, processingEnv);
                stubParser.parse(typesFromStubFiles, declAnnosFromStubFiles);
            }
        }

        // 2. flow.astub
        // stub file for type-system independent annotations
        InputStream input = BaseTypeChecker.class.getResourceAsStream("flow.astub");
        if (input != null) {
            StubParser stubParser = new StubParser("flow.astub", input, this, processingEnv);
            stubParser.parse(typesFromStubFiles, declAnnosFromStubFiles);
        }

        // Stub files specified via stubs compiler option, stubs system property,
        // stubs env. variable, or @Stubfiles
        List<String> allStubFiles = new ArrayList<>();

        // 3. Stub files listed in @Stubfiles annotation on the checker
        StubFiles stubFilesAnnotation = checker.getClass().getAnnotation(StubFiles.class);
        if (stubFilesAnnotation != null) {
            Collections.addAll(allStubFiles, stubFilesAnnotation.value());
        }

        // 4. Stub files provide via stubs system property
        String stubsProperty = System.getProperty("stubs");
        if (stubsProperty != null) {
            Collections.addAll(allStubFiles, stubsProperty.split(File.pathSeparator));
        }

        // 5. Stub files provide via stubs environment variable
        String stubEnvVar = System.getenv("stubs");
        if (stubEnvVar != null) {
            Collections.addAll(allStubFiles, stubEnvVar.split(File.pathSeparator));
        }

        // 6. Stub files provide via stubs option
        String stubsOption = checker.getOption("stubs");
        if (stubsOption != null) {
            Collections.addAll(allStubFiles, stubsOption.split(File.pathSeparator));
        }

        if (allStubFiles.isEmpty()) {
            this.typesFromStubFiles = typesFromStubFiles;
            this.declAnnosFromStubFiles = declAnnosFromStubFiles;
            return;
        }

        // Parse stub files specified via stubs compiler option, stubs system property,
        // stubs env. variable, or @Stubfiles
        for (String stubPath : allStubFiles) {
            if (stubPath == null || stubPath.isEmpty()) {
                continue;
            }
            // Handle case when running in jtreg
            String base = System.getProperty("test.src");
            String stubPathFull = stubPath;
            if (base != null) {
                stubPathFull = base + "/" + stubPath;
            }
            List<StubResource> stubs = StubUtil.allStubFiles(stubPathFull);
            if (stubs.size() == 0) {
                InputStream in = null;
                in = checker.getClass().getResourceAsStream(stubPath);
                if (in != null) {
                    StubParser stubParser = new StubParser(stubPath, in, this, processingEnv);
                    stubParser.parse(typesFromStubFiles, declAnnosFromStubFiles);
                    // We could handle the stubPath -> continue.
                    continue;
                }
                // We couldn't handle the stubPath -> error message.
                checker.message(
                        Kind.NOTE,
                        "Did not find stub file or files within directory: "
                                + stubPath
                                + " "
                                + new File(stubPath).getAbsolutePath());
            }
            for (StubResource resource : stubs) {
                InputStream stubStream;
                try {
                    stubStream = resource.getInputStream();
                } catch (IOException e) {
                    checker.message(
                            Kind.NOTE,
                            "Could not read stub resource: " + resource.getDescription());
                    continue;
                }
                StubParser stubParser =
                        new StubParser(resource.getDescription(), stubStream, this, processingEnv);
                stubParser.parse(typesFromStubFiles, declAnnosFromStubFiles);
            }
        }

        this.typesFromStubFiles = typesFromStubFiles;
        this.declAnnosFromStubFiles = declAnnosFromStubFiles;
    }

    /**
     * Returns the actual annotation mirror used to annotate this element, whose name equals the
     * passed annotation class, if one exists, or null otherwise.
     *
     * @see #getDeclAnnotationNoAliases
     * @param elt the element to retrieve the declaration annotation from
     * @param anno annotation class
     * @return the annotation mirror for anno
     */
    @Override
    public final AnnotationMirror getDeclAnnotation(Element elt, Class<? extends Annotation> anno) {
        String annoName = anno.getCanonicalName().intern();
        return getDeclAnnotation(elt, annoName, true);
    }

    /**
     * Returns the actual annotation mirror used to annotate this element, whose name equals the
     * passed annotation class, if one exists, or null otherwise. Does not check for aliases of the
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
        String annoName = anno.getCanonicalName().intern();
        return getDeclAnnotation(elt, annoName, false);
    }

    /**
     * Returns true if the element appears in a stub file (Currently only works for methods,
     * constructors, and fields)
     */
    public boolean isFromStubFile(Element element) {
        return this.getDeclAnnotation(element, FromStubFile.class) != null;
    }

    /**
     * Returns true if the element is from bytecode and the if the element did not appear in a stub
     * file (Currently only works for methods, constructors, and fields)
     */
    public boolean isFromByteCode(Element element) {
        if (isFromStubFile(element)) return false;
        return this.getDeclAnnotation(element, FromByteCode.class) != null;
    }

    /**
     * Returns the actual annotation mirror used to annotate this type, whose name equals the passed
     * annotationName if one exists, null otherwise. This is the private implementation of the
     * same-named, public method.
     *
     * <p>An option is provided to not to check for aliases of annotations. For example, an
     * annotated type factory may use aliasing for a pair of annotations for convenience while
     * needing in some cases to determine a strict ordering between them, such as when determining
     * whether the annotations on an overrider method are more specific than the annotations of an
     * overridden method.
     *
     * @param elt the element to retrieve the annotation from
     * @param annoName the class name of the annotation to retrieve
     * @param checkAliases whether to return an annotation mirror for an alias of the requested
     *     annotation class name
     * @return the annotation mirror for the requested annotation or null if not found
     */
    private AnnotationMirror getDeclAnnotation(
            Element elt, /*@Interned*/ String annoName, boolean checkAliases) {
        Set<AnnotationMirror> declAnnos = getDeclAnnotations(elt);

        for (AnnotationMirror am : declAnnos) {
            if (AnnotationUtils.areSameByName(am, annoName)) {
                return am;
            }
        }
        // Look through aliases.
        if (checkAliases) {
            Pair<AnnotationMirror, Set</*@Interned*/ String>> aliases = declAliases.get(annoName);
            if (aliases != null) {
                for (String alias : aliases.second) {
                    AnnotationMirror declAnnotation = getDeclAnnotation(elt, alias, false);
                    if (declAnnotation != null) {
                        return aliases.first;
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
     * <p>
     *
     * @param elt the element for which to determine annotations
     */
    public Set<AnnotationMirror> getDeclAnnotations(Element elt) {
        if (cacheDeclAnnos.containsKey(elt)) {
            // Found in cache, return result.
            return cacheDeclAnnos.get(elt);
        }

        Set<AnnotationMirror> results = AnnotationUtils.createAnnotationSet();
        // Retrieving the annotations from the element.
        results.addAll(elt.getAnnotationMirrors());
        // If declAnnosFromStubFiles == null, return the annotations in the element.
        if (declAnnosFromStubFiles != null) {
            // Adding @FromByteCode annotation to declAnnosFromStubFiles entry with key
            // elt, if elt is from bytecode.
            addFromByteCode(elt);

            // Retrieving annotations from stub files.
            String eltName = ElementUtils.getVerboseName(elt);
            Set<AnnotationMirror> stubAnnos = declAnnosFromStubFiles.get(eltName);
            if (stubAnnos != null) {
                results.addAll(stubAnnos);
            }

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
                        checker.message(
                                Kind.WARNING,
                                annotation.getAnnotationType().asElement(),
                                "annotation.not.completed",
                                ElementUtils.getVerboseName(elt),
                                annotation);
                        continue;
                    }
                    if (AnnotationUtils.containsSameByClass(
                                    annotationsOnAnnotation, InheritedAnnotation.class)
                            || AnnotationUtils.containsSameIgnoringValues(
                                    inheritedAnnotations, annotation)) {
                        addOrMerge(results, annotation);
                    }
                }
            }
        }
    }

    private void addOrMerge(Set<AnnotationMirror> results, AnnotationMirror annotation) {
        if (AnnotationUtils.containsSameIgnoringValues(results, annotation)) {
            /*
             * TODO: feature request: figure out a way to merge multiple annotations
             * of the same kind. For some annotations this might mean merging some
             * arrays, for others it might mean converting a single annotation into a
             * container annotation. We should define a protected method for subclasses
             * to adapt the behavior.
             * For now, do nothing and just take the first, most concrete, annotation.
            AnnotationMirror prev = null;
            for (AnnotationMirror an : results) {
                if (AnnotationUtils.areSameIgnoringValues(an, annotation)) {
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
     * meta-annotation (i.e., an annotation on that annotation) with class {@code metaAnnotation}.
     *
     * @param element the element for which to determine annotations
     * @param metaAnnotation the meta-annotation that needs to be present
     * @return a list of pairs {@code (anno, metaAnno)} where {@code anno} is the annotation mirror
     *     at {@code element}, and {@code metaAnno} is the annotation mirror used to annotate {@code
     *     anno}.
     */
    public List<Pair<AnnotationMirror, AnnotationMirror>> getDeclAnnotationWithMetaAnnotation(
            Element element, Class<? extends Annotation> metaAnnotation) {
        List<Pair<AnnotationMirror, AnnotationMirror>> result = new ArrayList<>();
        Set<AnnotationMirror> annotationMirrors = getDeclAnnotations(element);

        // Go through all annotations found.
        for (AnnotationMirror annotation : annotationMirrors) {
            List<? extends AnnotationMirror> annotationsOnAnnotation;
            try {
                annotationsOnAnnotation =
                        annotation.getAnnotationType().asElement().getAnnotationMirrors();
            } catch (com.sun.tools.javac.code.Symbol.CompletionFailure cf) {
                // Fix for Issue 309: If a CompletionFailure occurs, issue a warning.
                // I didn't find a nicer alternative to check whether the Symbol can be completed.
                // The completer field of a Symbol might be non-null also in successful cases.
                // Issue a warning (exception only happens once) and continue.
                checker.message(
                        Kind.WARNING,
                        annotation.getAnnotationType().asElement(),
                        "annotation.not.completed",
                        ElementUtils.getVerboseName(element),
                        annotation);
                continue;
            }
            // First call copier, if exception, continue normal modula laws.
            for (AnnotationMirror a : annotationsOnAnnotation) {
                if (AnnotationUtils.areSameByClass(a, metaAnnotation)) {
                    result.add(Pair.of(annotation, a));
                }
            }
        }
        return result;
    }

    /**
     * Returns a list of all annotations used to annotate this element, which have a meta-annotation
     * (i.e., an annotation on that annotation) with class {@code metaAnnotation}.
     *
     * @param element the element at which to look for annotations
     * @param metaAnnotation the meta-annotation that needs to be present
     * @return a list of pairs {@code (anno, metaAnno)} where {@code anno} is the annotation mirror
     *     at {@code element}, and {@code metaAnno} is the annotation mirror used to annotate {@code
     *     anno}.
     */
    public List<Pair<AnnotationMirror, AnnotationMirror>> getAnnotationWithMetaAnnotation(
            Element element, Class<? extends Annotation> metaAnnotation) {
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
                if (AnnotationUtils.areSameByClass(a, metaAnnotation)) {
                    result.add(Pair.of(annotation, a));
                }
            }
        }
        return result;
    }

    /**
     * Returns a wildcard type to be used as a type argument when the correct type could not be
     * inferred. The wildcard will be marked as an uninferred wildcard so that {@link
     * AnnotatedWildcardType#isUninferredTypeArgument()} returns true.
     *
     * <p>This method should only be used by type argument inference or for type arguments to raw
     * types:
     * org.checkerframework.framework.util.AnnotatedTypes.inferTypeArguments(ProcessingEnvironment,
     * AnnotatedTypeFactory, ExpressionTree, ExecutableElement)
     * org.checkerframework.framework.type.AnnotatedTypeFactory.fromTypeTree(Tree)
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
        if (!intersectionType) {
            wctype.setExtendsBound(typeVar.getUpperBound().deepCopy());
        } else {
            //TODO: This probably doesn't work if the type has a type argument
            wctype.getExtendsBound().addAnnotations(typeVar.getUpperBound().getAnnotations());
        }
        wctype.setSuperBound(typeVar.getLowerBound().deepCopy());
        wctype.addAnnotations(typeVar.getAnnotations());
        wctype.setUninferredTypeArgument();
        return wctype;
    }

    /**
     * If {@code wildcard}'s upper bound is a super type of {@code annotatedTypeMirror}, this method
     * returns an AnnotatedTypeMirror with the same qualifiers as {@code annotatedTypeMirror}, but
     * the underlying Java type is the the most specific base type of {@code annotatedTypeMirror}
     * whose erasure type is equivalent to the upper bound of {@code wildcard}.
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
        if (types.isSubtype(wildcardUBTypeMirror, toModifyTypeMirror)) {
            return annotatedTypeMirror;
        } else if (types.isSubtype(toModifyTypeMirror, wildcardUBTypeMirror)) {
            return AnnotatedTypes.asSuper(this, annotatedTypeMirror, wildcard);
        } else if (wildcardUBTypeMirror.getKind() == TypeKind.DECLARED
                && InternalUtils.getTypeElement(wildcardUBTypeMirror).getKind().isInterface()) {
            // If the Checker Framework implemented capture conversion, then in this case, then
            // the upper bound of the capture converted wildcard would be an intersection type.
            // See JLS 15.1.10
            // (https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.10)

            // For example:
            // class MyClass<@A T extends @B Number> { }
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

    public Pair<AnnotatedDeclaredType, AnnotatedExecutableType> getFnInterfaceFromTree(
            MemberReferenceTree tree) {
        return getFnInterfaceFromTree((Tree) tree);
    }

    public Pair<AnnotatedDeclaredType, AnnotatedExecutableType> getFnInterfaceFromTree(
            LambdaExpressionTree tree) {
        return getFnInterfaceFromTree((Tree) tree);
    }

    /**
     * Find the declared type of the functional interface and the executable type for its method for
     * a given MemberReferenceTree or LambdaExpressionTree.
     *
     * @param tree the MemberReferenceTree or LambdaExpressionTree
     * @return the declared type of the functional interface and the executable type
     */
    private Pair<AnnotatedDeclaredType, AnnotatedExecutableType> getFnInterfaceFromTree(Tree tree) {

        Context ctx = ((JavacProcessingEnvironment) getProcessingEnv()).getContext();
        com.sun.tools.javac.code.Types javacTypes = com.sun.tools.javac.code.Types.instance(ctx);

        // ========= Overridden Type =========
        AnnotatedDeclaredType functionalInterfaceType =
                getFunctionalInterfaceType(tree, javacTypes);
        makeGroundTargetType(functionalInterfaceType);

        // ========= Overridden Executable =========
        Element fnElement =
                javacTypes.findDescriptorSymbol(
                        ((Type) functionalInterfaceType.getUnderlyingType()).asElement());

        // The method viewed from the declared type
        AnnotatedExecutableType methodExe =
                (AnnotatedExecutableType)
                        AnnotatedTypes.asMemberOf(types, this, functionalInterfaceType, fnElement);

        return Pair.of(functionalInterfaceType, methodExe);
    }

    /**
     * Get the AnnotatedDeclaredType for the FunctionalInterface from assignment context of the
     * method reference which may be a variable assignment, a method call, or a cast.
     *
     * <p>The assignment context is not always correct, so we must search up the AST. It will
     * recursively search for lambdas nested in lambdas.
     *
     * @param lambdaTree the tree of the lambda or method reference
     * @return the functional interface type
     */
    private AnnotatedDeclaredType getFunctionalInterfaceType(
            Tree lambdaTree, com.sun.tools.javac.code.Types javacTypes) {

        Tree parentTree = TreePath.getPath(this.root, lambdaTree).getParentPath().getLeaf();
        switch (parentTree.getKind()) {
            case PARENTHESIZED:
                return getFunctionalInterfaceType(parentTree, javacTypes);

            case TYPE_CAST:
                TypeCastTree cast = (TypeCastTree) parentTree;
                assertFunctionalInterface(
                        javacTypes,
                        (Type) trees.getTypeMirror(getPath(cast.getType())),
                        parentTree,
                        lambdaTree);
                AnnotatedTypeMirror castATM = getAnnotatedType(cast.getType());
                if (castATM.getKind() == TypeKind.INTERSECTION) {
                    AnnotatedIntersectionType itype = (AnnotatedIntersectionType) castATM;
                    for (AnnotatedTypeMirror t : itype.directSuperTypes()) {
                        if (javacTypes.isFunctionalInterface((Type) t.getUnderlyingType())) {
                            return (AnnotatedDeclaredType) t;
                        }
                    }
                    // We should never reach here: assertFunctionalInterface performs the same check and
                    // would have raised an error already.
                    ErrorReporter.errorAbort(
                            String.format(
                                    "Expected the type of a cast tree in an assignment context to contain a functional interface bound. "
                                            + "Found type: %s for tree: %s in lambda tree: %s",
                                    castATM, cast, lambdaTree));
                }
                return (AnnotatedDeclaredType) castATM;

            case NEW_CLASS:
                NewClassTree newClass = (NewClassTree) parentTree;
                int indexOfLambda = newClass.getArguments().indexOf(lambdaTree);
                Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> con =
                        this.constructorFromUse(newClass);
                AnnotatedTypeMirror constructorParam =
                        AnnotatedTypes.getAnnotatedTypeMirrorOfParameter(con.first, indexOfLambda);
                assertFunctionalInterface(
                        javacTypes,
                        (Type) constructorParam.getUnderlyingType(),
                        parentTree,
                        lambdaTree);
                return (AnnotatedDeclaredType) constructorParam;

            case METHOD_INVOCATION:
                MethodInvocationTree method = (MethodInvocationTree) parentTree;
                int index = method.getArguments().indexOf(lambdaTree);
                Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> exe =
                        this.methodFromUse(method);
                AnnotatedTypeMirror param =
                        AnnotatedTypes.getAnnotatedTypeMirrorOfParameter(exe.first, index);
                assertFunctionalInterface(
                        javacTypes, (Type) param.getUnderlyingType(), parentTree, lambdaTree);
                return (AnnotatedDeclaredType) param;

            case VARIABLE:
                VariableTree varTree = (VariableTree) parentTree;
                assertFunctionalInterface(
                        javacTypes, (Type) InternalUtils.typeOf(varTree), parentTree, lambdaTree);
                return (AnnotatedDeclaredType) getAnnotatedType(varTree.getType());

            case ASSIGNMENT:
                AssignmentTree assignmentTree = (AssignmentTree) parentTree;
                assertFunctionalInterface(
                        javacTypes,
                        (Type) InternalUtils.typeOf(assignmentTree),
                        parentTree,
                        lambdaTree);
                return (AnnotatedDeclaredType) getAnnotatedType(assignmentTree.getVariable());

            case RETURN:
                Tree enclosing =
                        TreeUtils.enclosingOfKind(
                                TreePath.getPath(this.root, parentTree),
                                new HashSet<>(
                                        Arrays.asList(
                                                Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION)));

                if (enclosing.getKind() == Tree.Kind.METHOD) {
                    MethodTree enclosingMethod = (MethodTree) enclosing;
                    return (AnnotatedDeclaredType)
                            getAnnotatedType(enclosingMethod.getReturnType());
                } else {
                    LambdaExpressionTree enclosingLambda = (LambdaExpressionTree) enclosing;
                    Pair<AnnotatedDeclaredType, AnnotatedExecutableType> result =
                            getFnInterfaceFromTree(enclosingLambda);
                    AnnotatedExecutableType methodExe = result.second;
                    return (AnnotatedDeclaredType) methodExe.getReturnType();
                }
            case LAMBDA_EXPRESSION:
                LambdaExpressionTree enclosingLambda = (LambdaExpressionTree) parentTree;
                Pair<AnnotatedDeclaredType, AnnotatedExecutableType> result =
                        getFnInterfaceFromTree(enclosingLambda);
                AnnotatedExecutableType methodExe = result.second;
                return (AnnotatedDeclaredType) methodExe.getReturnType();

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
                assertFunctionalInterface(
                        javacTypes,
                        (Type) conditionalType.getUnderlyingType(),
                        parentTree,
                        lambdaTree);
                return (AnnotatedDeclaredType) conditionalType;

            default:
                ErrorReporter.errorAbort(
                        "Could not find functional interface from assignment context. "
                                + "Unexpected tree type: "
                                + parentTree.getKind()
                                + " For lambda tree: "
                                + lambdaTree);
                return null;
        }
    }

    private void assertFunctionalInterface(
            com.sun.tools.javac.code.Types javacTypes,
            Type type,
            Tree contextTree,
            Tree lambdaTree) {

        if (!javacTypes.isFunctionalInterface(type)) {
            if (type.getKind() == TypeKind.INTERSECTION) {
                IntersectionType itype = (IntersectionType) type;
                for (TypeMirror t : itype.getBounds()) {
                    if (javacTypes.isFunctionalInterface((Type) t)) {
                        // As long as any of the bounds is a functional interface
                        // we should be fine.
                        return;
                    }
                }
            }
            ErrorReporter.errorAbort(
                    String.format(
                            "Expected the type of %s tree in assignment context to be a functional interface. "
                                    + "Found type: %s for tree: %s in lambda tree: %s",
                            contextTree.getKind(), type, contextTree, lambdaTree));
        }
    }

    /**
     * Create the ground target type of the functional interface.
     *
     * <p>Basically, it replaces the wildcards with their bounds doing a capture conversion like glb
     * for extends bounds.
     *
     * @see "JLS 9.9"
     * @param overriddenType the functional interface type
     */
    private void makeGroundTargetType(AnnotatedDeclaredType overriddenType) {
        if (overriddenType.getTypeArguments().size() > 0) {
            List<AnnotatedTypeParameterBounds> bounds =
                    this.typeVariablesFromUse(
                            overriddenType,
                            (TypeElement) overriddenType.getUnderlyingType().asElement());
            List<AnnotatedTypeMirror> newTypeArguments =
                    new ArrayList<>(overriddenType.getTypeArguments());
            for (int i = 0; i < overriddenType.getTypeArguments().size(); i++) {
                AnnotatedTypeMirror argType = overriddenType.getTypeArguments().get(i);
                if (argType.getKind() == TypeKind.WILDCARD) {
                    AnnotatedWildcardType wildcardType = (AnnotatedWildcardType) argType;

                    final TypeMirror wilcardUbType =
                            wildcardType.getExtendsBound().getUnderlyingType();
                    final TypeMirror typeParamUbType =
                            bounds.get(i).getUpperBound().getUnderlyingType();
                    if (isExtendsWildcard(wildcardType)) {
                        TypeMirror glbType =
                                InternalUtils.greatestLowerBound(
                                        this.checker.getProcessingEnvironment(),
                                        typeParamUbType,
                                        wilcardUbType);

                        // checkTypeArgs now enforces that wildcard annotation bounds MUST be within
                        // the bounds of the type parameter.  Therefore, the wildcard's upper bound
                        // should ALWAYS be more specific than the upper bound of the type parameter
                        // That said, the Java type does NOT have to be.
                        // Add the annotations from the wildcard to the lub type.
                        final AnnotatedTypeMirror newArg;
                        if (types.isSameType(wilcardUbType, glbType)) {
                            newArg = wildcardType.getExtendsBound().deepCopy();

                        } else {
                            newArg = this.toAnnotatedType(glbType, false);
                            newArg.replaceAnnotations(
                                    wildcardType.getExtendsBound().getAnnotations());
                        }
                        newTypeArguments.set(i, newArg);

                    } else {
                        newTypeArguments.set(i, wildcardType.getSuperBound());
                    }
                }
            }
            overriddenType.setTypeArguments(newTypeArguments);
        }
    }

    /**
     * Check that a wildcard is an extends wildcard
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
}
