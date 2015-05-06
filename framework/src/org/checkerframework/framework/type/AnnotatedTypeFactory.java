package org.checkerframework.framework.type;

/*>>>
import org.checkerframework.checker.interning.qual.*;
import org.checkerframework.checker.javari.qual.Mutable;
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.reflection.DefaultReflectionResolver;
import org.checkerframework.common.reflection.MethodValAnnotatedTypeFactory;
import org.checkerframework.common.reflection.MethodValChecker;
import org.checkerframework.common.reflection.ReflectionResolver;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.FromByteCode;
import org.checkerframework.framework.qual.FromStubFile;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.stub.StubParser;
import org.checkerframework.framework.stub.StubResource;
import org.checkerframework.framework.stub.StubUtil;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
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
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.trees.DetachedVarSymbol;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

//The following imports are from com.sun, but they are all
//@jdk.Exported and therefore somewhat safe to use.
//Try to avoid using non-@jdk.Exported classes.
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
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

/**
 * The methods of this class take an element or AST node, and return the
 * annotated type as an {@link AnnotatedTypeMirror}.  The methods are:
 *
 * <ul>
 * <li>{@link #getAnnotatedType(ClassTree)}</li>
 * <li>{@link #getAnnotatedType(MethodTree)}</li>
 * <li>{@link #getAnnotatedType(Tree)}</li>
 * <li>{@link #getAnnotatedTypeFromTypeTree(Tree)}</li>
 * <li>{@link #getAnnotatedType(TypeElement)}</li>
 * <li>{@link #getAnnotatedType(ExecutableElement)}</li>
 * <li>{@link #getAnnotatedType(Element)}</li>
 * </ul>
 *
 * This implementation only adds qualifiers explicitly specified by the
 * programmer.
 *
 * Type system checker writers may need to subclass this class, to add implicit
 * and default qualifiers according to the type system semantics. Subclasses
 * should especially override
 * {@link AnnotatedTypeFactory#annotateImplicit(Element, AnnotatedTypeMirror)}
 * and {@link #annotateImplicit(Tree, AnnotatedTypeMirror)}.
 *
 * @checker_framework.manual #writing-a-checker How to write a checker plug-in
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

    /** The state of the visitor. **/
    protected final VisitorState visitorState;

    /** ===== postInit initialized fields ====
     * Note: qualHierarchy and typeHierarchy are both initialized in the postInit.
     * @see #postInit()
     * This means, they cannot be final and cannot be referred to in any subclass
     * constructor or method until after postInit is called.
     */

    /** Represent the annotation relations. **/
    protected QualifierHierarchy qualHierarchy;

    /** Represent the type relations. */
    protected TypeHierarchy typeHierarchy;

    /**
     * This formatter is used for converting AnnotatedTypeMirrors to Strings.
     * This formatter will be passed to all AnnotatedTypeMirrors created by this
     * factory and will be used in their toString methods.
     */
    protected final AnnotatedTypeFormatter typeFormatter;

    /**
     * Annotation formatter is used to format AnnotationMirrors. It is primarily
     * used by SourceChecker when generating error messages.
     */
    private final AnnotationFormatter annotationFormatter;

    /**
     * Provides utility method to substitute arguments for their type variables.
     * Field should be final, but can only be set in postInit, because subtypes
     * might need other state to be initialized first.
     */
    protected TypeVariableSubstitutor typeVarSubstitutor;

    /**
     * Provides utility method to infer type arguments
     */
    protected TypeArgumentInference typeArgumentInference;

    /** To cache the supported type qualifiers. */
    private final Set<Class<? extends Annotation>> supportedQuals;

    /** Types read from stub files (but not those from the annotated JDK jar file). */
    // Initially null, then assigned in postInit().  Caching is enabled as
    // soon as this is non-null, so it should be first set to its final
    // value, not initialized to an empty map that is incrementally filled.
    private Map<Element, AnnotatedTypeMirror> indexTypes;

    /**
     * Declaration annotations read from stub files (but not those from the annotated JDK jar file).
     * Map keys cannot be Element, because a different Element appears
     * in the stub files than in the real files.  So, map keys are the
     * verbose element name, as returned by ElementUtils.getVerboseName.
     */
    // Not final, because it is assigned in postInit().
    private Map<String, Set<AnnotationMirror>> indexDeclAnnos;

    /**
     * A cache used to store elements whose declaration annotations
     * have already been stored by calling the method getDeclAnnotations.
     */
    private final Map<Element, Set<AnnotationMirror>> cacheDeclAnnos;

    /**
     * A set containing declaration annotations that should be inherited.
     * A declaration annotation will be inherited if it is in this set,
     * or if it has the meta-annotation @InheritedAnnotation.
     */
    private final Set<AnnotationMirror> inheritedAnnotations = AnnotationUtils.
            createAnnotationSet();

    /**
     * The checker to use for option handling and resource management.
     */
    protected final BaseTypeChecker checker;

    /**
     * Map from class name (canonical name) of an annotation, to the
     * annotation in the Checker Framework that will be used in its place.
     */
    private final Map<String, AnnotationMirror> aliases = new HashMap<String, AnnotationMirror>();

    /**
     * A map from the class name (canonical name) of an annotation to the set of
     * class names (canonical names) for annotations with the same meaning
     * (i.e., aliases), as well as the annotation mirror that should be used.
     */
    private final Map<String, Pair<AnnotationMirror, Set</*@Interned*/ String>>> declAliases = new HashMap<>();

    /** Unique ID counter; for debugging purposes. */
    private static int uidCounter = 0;

    /** Unique ID of the current object; for debugging purposes. */
    public final int uid;

    /**Annotation added to every method defined in a class file
     * that is not in a stub file
     */
    private final AnnotationMirror fromByteCode;

    /**
     * Object that is used to resolve reflective method calls, if reflection
     * resolution is turned on.
     */
    protected ReflectionResolver reflectionResolver;

    /**
     * Constructs a factory from the given {@link ProcessingEnvironment}
     * instance and syntax tree root. (These parameters are required so that
     * the factory may conduct the appropriate annotation-gathering analyses on
     * certain tree types.)
     *
     * Root can be {@code null} if the factory does not operate on trees.
     *
     * A subclass must call postInit at the end of its constructor.
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

        this.supportedQuals = createSupportedTypeQualifiers();

        this.fromByteCode = AnnotationUtils.fromClass(elements, FromByteCode.class);

        this.cacheDeclAnnos = new HashMap<Element, Set<AnnotationMirror>>();

        this.typeFormatter = createAnnotatedTypeFormatter();
        this.annotationFormatter = createAnnotationFormatter();
    }

    /**
     * Actions that logically belong in the constructor, but need to run
     * after the subclass constructor has completed.  In particular,
     * buildIndexTypes may try to do type resolution with this
     * AnnotatedTypeFactory.
     */
    protected void postInit() {
        this.qualHierarchy = createQualifierHierarchy();
        if (qualHierarchy == null) {
            ErrorReporter.errorAbort("AnnotatedTypeFactory with null qualifier hierarchy not supported.");
        }
        this.typeHierarchy = createTypeHierarchy();
        this.typeVarSubstitutor = createTypeVariableSubstitutor();
        this.typeArgumentInference = createTypeArgumentInference();

        // TODO: is this the best location for declaring this alias?
        addAliasedDeclAnnotation(org.jmlspecs.annotation.Pure.class,
                org.checkerframework.dataflow.qual.Pure.class,
                AnnotationUtils.fromClass(elements, org.checkerframework.dataflow.qual.Pure.class));

        addInheritedAnnotation(AnnotationUtils.fromClass(elements,
                org.checkerframework.dataflow.qual.Pure.class));
        addInheritedAnnotation(AnnotationUtils.fromClass(elements,
                org.checkerframework.dataflow.qual.SideEffectFree.class));
        addInheritedAnnotation(AnnotationUtils.fromClass(elements,
                org.checkerframework.dataflow.qual.Deterministic.class));
        addInheritedAnnotation(AnnotationUtils.fromClass(elements,
                org.checkerframework.dataflow.qual.TerminatesExecution.class));
        addInheritedAnnotation(AnnotationUtils.fromClass(elements,
                org.checkerframework.dataflow.qual.LockingFree.class));

        initilizeReflectionResolution();

        if (this.getClass().equals(AnnotatedTypeFactory.class)) {
            this.buildIndexTypes();
        }
    }

    protected void initilizeReflectionResolution() {
        if (checker.shouldResolveReflection()) {
            boolean debug = "debug".equals(checker.getOption("resolveReflection"));

            MethodValChecker methodValChecker = checker
                    .getSubchecker(MethodValChecker.class);
            assert methodValChecker != null : "AnnotatedTypeFactory: reflection resolution was requested, but MethodValChecker isn't a subchecker.";
            MethodValAnnotatedTypeFactory methodValATF = (MethodValAnnotatedTypeFactory) methodValChecker
                    .getAnnotationProvider();

            reflectionResolver = new DefaultReflectionResolver(checker,
                    methodValATF, debug);
        }
    }

    // TODO: document
    // Set the CompilationUnitTree that should be used.
    // What's a better name? Maybe "reset" or "start"?
    public void setRoot(/*@Nullable*/ CompilationUnitTree root) {
        this.root = root;
        treePathCache.clear();
        pathHack.clear();

        // There is no need to clear the following caches, they
        // are all limited by CACHE_SIZE.
        /*
        treeCache.clear();
        fromTreeCache.clear();
        elementCache.clear();
        elementToTreeCache.clear();
        */
    }

    @SideEffectFree
    @Override
    public String toString() {
        return getClass().getSimpleName() + "#" + uid;
    }


    /** Factory method to easily change what Factory is used to
     * create a QualifierHierarchy.
     */
    protected MultiGraphQualifierHierarchy.MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    /** Factory method to easily change what QualifierHierarchy is
     * created.
     * Needs to be public only because the GraphFactory must be able to call this method.
     * No external use of this method is necessary.
     */
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new GraphQualifierHierarchy(factory, null);
    }

    /**
     * Returns the type qualifier hierarchy graph to be used by this processor.
     *
     * The implementation builds the type qualifier hierarchy for the
     * {@link #getSupportedTypeQualifiers()} using the
     * meta-annotations found in them.  The current implementation returns an
     * instance of {@code GraphQualifierHierarchy}.
     *
     * Subclasses may override this method to express any relationships that
     * cannot be inferred using meta-annotations (e.g. due to lack of
     * meta-annotations).
     *
     * @return an annotation relation tree representing the supported qualifiers
     */
    protected QualifierHierarchy createQualifierHierarchy() {
        Set<Class<? extends Annotation>> supportedTypeQualifiers = getSupportedTypeQualifiers();
        MultiGraphQualifierHierarchy.MultiGraphFactory factory = this.createQualifierHierarchyFactory();

        return createQualifierHierarchy(elements, supportedTypeQualifiers, factory);
    }

    /**
     * Returns the type qualifier hierarchy graph for a given set of type qualifiers and a factory.
     * <p>
     *
     * The implementation builds the type qualifier hierarchy for the
     * {@code supportedTypeQualifiers}.  The current implementation returns an
     * instance of {@code GraphQualifierHierarchy}.
     *
     * @return an annotation relation tree representing the supported qualifiers
     */
    protected static QualifierHierarchy createQualifierHierarchy(
            Elements elements,
            Set<Class<? extends Annotation>> supportedTypeQualifiers,
            MultiGraphFactory factory) {

        for (Class<? extends Annotation> typeQualifier : supportedTypeQualifiers) {
            AnnotationMirror typeQualifierAnno = AnnotationUtils.fromClass(elements, typeQualifier);
            assert typeQualifierAnno != null : "Loading annotation \"" + typeQualifier + "\" failed!";
            factory.addQualifier(typeQualifierAnno);
            // Polymorphic qualifiers can't declare their supertypes.
            // An error is raised if one is present.
            if (typeQualifier.getAnnotation(PolymorphicQualifier.class) != null) {
                if (typeQualifier.getAnnotation(SubtypeOf.class) != null) {
                    // This is currently not supported. At some point we might add
                    // polymorphic qualifiers with upper and lower bounds.
                    ErrorReporter.errorAbort("AnnotatedTypeFactory: " + typeQualifier + " is polymorphic and specifies super qualifiers. " +
                        "Remove the @org.checkerframework.framework.qual.SubtypeOf or @org.checkerframework.framework.qual.PolymorphicQualifier annotation from it.");
                }
                continue;
            }
            if (typeQualifier.getAnnotation(SubtypeOf.class) == null) {
                ErrorReporter.errorAbort("AnnotatedTypeFactory: " + typeQualifier + " does not specify its super qualifiers. " +
                    "Add an @org.checkerframework.framework.qual.SubtypeOf annotation to it.");
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
            ErrorReporter.errorAbort("AnnotatedTypeFactory: invalid qualifier hierarchy: " +
                    hierarchy.getClass() + " " + hierarchy);
        }

        return hierarchy;
    }

    /**
     * Returns the type qualifier hierarchy graph to be used by this processor.
     *
     * @see #createQualifierHierarchy()
     *
     * @return the {@link QualifierHierarchy} for this checker
     */
    public final QualifierHierarchy getQualifierHierarchy() {
        //if (qualHierarchy == null)
        //    qualHierarchy = createQualifierHierarchy();
        return qualHierarchy;
    }

    /**
     * Creates the type subtyping checker using the current type qualifier
     * hierarchy.
     *
     * Subclasses may override this method to specify new type-checking
     * rules beyond the typical java subtyping rules.
     *
     * @return  the type relations class to check type subtyping
     */
    protected TypeHierarchy createTypeHierarchy() {
        return new DefaultTypeHierarchy(checker, getQualifierHierarchy(),
                                        checker.hasOption("ignoreRawTypeArguments"),
                                        checker.hasOption("invariantArrays"));
    }

    public final TypeHierarchy getTypeHierarchy() {
        return typeHierarchy;
    }

    /**
     * TypeVariableSubstitutor provides a method to replace type parameters with
     * their arguments.
     */
    protected TypeVariableSubstitutor createTypeVariableSubstitutor() {
        return new TypeVariableSubstitutor();
    }

    public TypeVariableSubstitutor getTypeVarSubstitutor() {
        return typeVarSubstitutor;
    }

    /**
     * TypeArgumentInference infers the method type arguments when
     * they are not explicitly written.
     */
    protected TypeArgumentInference createTypeArgumentInference() {
        return new DefaultTypeArgumentInference();
    }

    public TypeArgumentInference getTypeArgumentInference() {
        return typeArgumentInference;
    }

    /**
     * If the checker class is annotated with {@link
     * TypeQualifiers}, return an immutable set with the same set
     * of classes as the annotation.  If the class is not so annotated,
     * return an empty set.
     *
     * Subclasses may override this method to return an immutable set
     * of their supported type qualifiers.
     *
     * @return the type qualifiers supported this processor, or an empty
     * set if none
     *
     * @see TypeQualifiers
     */
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        Class<?> classType;
        TypeQualifiers typeQualifiersAnnotation;

        // First see if the AnnotatedTypeFactory has @TypeQualifiers
        classType = this.getClass();
        typeQualifiersAnnotation = classType.getAnnotation(TypeQualifiers.class);

        if (typeQualifiersAnnotation == null) {
            // If not, try the Checker
            classType = checker.getClass();
            typeQualifiersAnnotation = classType.getAnnotation(TypeQualifiers.class);
        }

        if (typeQualifiersAnnotation != null) {
            Set<Class<? extends Annotation>> typeQualifiers = new HashSet<Class<? extends Annotation>>();
            for (Class<? extends Annotation> qualifier : typeQualifiersAnnotation.value()) {
                typeQualifiers.add(qualifier);
            }
            return Collections.unmodifiableSet(typeQualifiers);
        }

        return Collections.emptySet();
    }

    /**
     * Creates the AnnotatedTypeFormatter used by this type factory and all AnnotatedTypeMirrors
     * it creates.  The AnnotatedTypeFormatter is used in AnnotatedTypeMirror.toString and
     * will affect the error messages printed for checkers that use this type factory.
     * @return The AnnotatedTypeFormatter to pass to all instantiated AnnotatedTypeMirrors
     */
    protected AnnotatedTypeFormatter createAnnotatedTypeFormatter() {
        return new DefaultAnnotatedTypeFormatter(checker.hasOption("printVerboseGenerics"), checker.hasOption("printAllQualifiers"));
    }

    protected AnnotationFormatter createAnnotationFormatter() {
        return new DefaultAnnotationFormatter();
    }

    public AnnotationFormatter getAnnotationFormatter() {
        return annotationFormatter;
    }

    /**
     * Returns an immutable set of the type qualifiers supported by this
     * checker.
     *
     * @see #createSupportedTypeQualifiers()
     *
     * @return the type qualifiers supported this processor, or an empty
     * set if none
     */
    public final Set<Class<? extends Annotation>> getSupportedTypeQualifiers() {
        //if (supportedQuals == null)
        //    supportedQuals = createSupportedTypeQualifiers();
        return supportedQuals;
    }

    // **********************************************************************
    // Factories for annotated types that account for implicit qualifiers
    // **********************************************************************

    /** Should results be cached? Disable for better debugging. */
    protected static boolean SHOULD_CACHE = true;
    public boolean shouldCache = SHOULD_CACHE;

    /** Should the cached result be used, or should it be freshly computed? */
    protected static boolean SHOULD_READ_CACHE = true;
    public boolean shouldReadCache = SHOULD_READ_CACHE;

    /** Size of LRU cache. */
    private final static int CACHE_SIZE = 300;

    /** Mapping from a Tree to its annotated type; implicits have been applied. */
    private final Map<Tree, AnnotatedTypeMirror> treeCache = createLRUCache(CACHE_SIZE);

    /** Mapping from a Tree to its annotated type; before implicits are applied,
     * just what the programmer wrote. */
    protected final Map<Tree, AnnotatedTypeMirror> fromTreeCache = createLRUCache(CACHE_SIZE);

    /** Mapping from an Element to its annotated type; before implicits are applied,
     * just what the programmer wrote. */
    private final Map<Element, AnnotatedTypeMirror> elementCache = createLRUCache(CACHE_SIZE);

    /** Mapping from an Element to the source Tree of the declaration. */
    private final Map<Element, Tree> elementToTreeCache  = createLRUCache(CACHE_SIZE);

    /** Mapping from a Tree to its TreePath **/
    private final TreePathCacher treePathCache = new TreePathCacher();

    /**
     * Determines the annotated type of an element using
     * {@link #fromElement(Element)}.
     *
     * @param elt the element
     * @return the annotated type of {@code elt}
     * @throws IllegalArgumentException if {@code elt} is null
     *
     * @see #fromElement(Element)
     */
    public AnnotatedTypeMirror getAnnotatedType(Element elt) {
        if (elt == null) {
            ErrorReporter.errorAbort("AnnotatedTypeFactory.getAnnotatedType: null element");
            return null; // dead code
        }
        AnnotatedTypeMirror type = fromElement(elt);
        annotateInheritedFromClass(type);
        annotateImplicit(elt, type);
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
     * Determines the annotated type of an AST node.
     *
     * <p>
     *
     * The type is determined as follows:
     * <ul>
     *  <li>if {@code tree} is a class declaration, determine its type via
     *    {@link #fromClass}</li>
     *  <li>if {@code tree} is a method or variable declaration, determine its
     *    type via {@link #fromMember(Tree)}</li>
     *  <li>if {@code tree} is an {@link ExpressionTree}, determine its type
     *    via {@link #fromExpression(ExpressionTree)}</li>
     *  <li>otherwise, throw an {@link UnsupportedOperationException}</li>
     * </ul>
     *
     * @param tree the AST node
     * @return the annotated type of {@code tree}
     *
     * @see #fromClass(ClassTree)
     * @see #fromMember(Tree)
     * @see #fromExpression(ExpressionTree)
     */
    // I wish I could make this method protected
    public AnnotatedTypeMirror getAnnotatedType(Tree tree) {
        if (tree == null) {
            ErrorReporter.errorAbort("AnnotatedTypeFactory.getAnnotatedType: null tree");
            return null; // dead code
        }
        if (treeCache.containsKey(tree) && shouldReadCache) {
            return treeCache.get(tree).deepCopy();
        }

        AnnotatedTypeMirror type;
        if (TreeUtils.isClassTree(tree)) {
            type = fromClass((ClassTree)tree);
        } else if (tree.getKind() == Tree.Kind.METHOD ||
                tree.getKind() == Tree.Kind.VARIABLE) {
            type = fromMember(tree);
        } else if (TreeUtils.isExpressionTree(tree)) {
            tree = TreeUtils.skipParens((ExpressionTree)tree);
            type = fromExpression((ExpressionTree) tree);
        } else {
            ErrorReporter.errorAbort(
                    "AnnotatedTypeFactory.getAnnotatedType: query of annotated type for tree " + tree.getKind());
            type = null; // dead code
        }

        annotateImplicit(tree, type);

        if (TreeUtils.isClassTree(tree) ||
            tree.getKind() == Tree.Kind.METHOD) {
            // Don't cache VARIABLE
            if (shouldCache) {
                treeCache.put(tree, type.deepCopy());
            }
        } else {
            // No caching otherwise
        }

        if (tree.getKind() == Tree.Kind.CLASS) {
            postProcessClassTree((ClassTree) tree);
        }

        // System.out.println("AnnotatedTypeFactory::getAnnotatedType(Tree) result: " + type);
        return type;
    }

    /**
     * Called by getAnnotatedType(Tree) for each ClassTree after determining the type.
     * The default implementation uses this to store the defaulted AnnotatedTypeMirrors
     * and inherited declaration annotations back into the corresponding Elements.
     * Subclasses might want to override this method if storing defaulted types is
     * not desirable.
     */
    protected void postProcessClassTree(ClassTree tree) {
        TypesIntoElements.store(processingEnv, this, tree);
        DeclarationsIntoElements.store(processingEnv, this, tree);
    }

    /**
     * Determines the annotated type from a type in tree form.
     *
     * Note that we cannot decide from a Tree whether it is
     * a type use or an expression.
     * TreeUtils.isTypeTree is only an under-approximation.
     * For example, an identifier can be either a type or an expression.
     *
     * @param tree the type tree
     * @return the annotated type of the type in the AST
     */
    public AnnotatedTypeMirror getAnnotatedTypeFromTypeTree(Tree tree) {
        if (tree == null) {
            ErrorReporter.errorAbort("AnnotatedTypeFactory.getAnnotatedTypeFromTypeTree: null tree");
            return null; // dead code
        }
        AnnotatedTypeMirror type = fromTypeTree(tree);
        annotateImplicit(tree, type);
        return type;
    }


    // **********************************************************************
    // Factories for annotated types that do not account for implicit qualifiers.
    // They only include qualifiers explicitly inserted by the user.
    // **********************************************************************

    /**
     * Determines the annotated type of an element.
     *
     * @param elt the element
     * @return the annotated type of the element
     */
    public AnnotatedTypeMirror fromElement(Element elt) {
        if (elementCache.containsKey(elt) && shouldReadCache) {
            return elementCache.get(elt).deepCopy();
        }
        if (elt.getKind() == ElementKind.PACKAGE)
            return toAnnotatedType(elt.asType(), false);
        AnnotatedTypeMirror type;
        Tree decl = declarationFromElement(elt);

        if (decl == null && indexTypes != null && indexTypes.containsKey(elt)) {
            type = indexTypes.get(elt).deepCopy();
        } else if (decl == null && (indexTypes == null || !indexTypes.containsKey(elt))) {
            type = toAnnotatedType(elt.asType(), ElementUtils.isTypeDeclaration(elt));
            ElementAnnotationApplier.apply(type, elt, this);

            if (elt instanceof ExecutableElement
                    || elt instanceof VariableElement) {
                annotateInheritedFromClass(type);
            }
        } else if (decl instanceof ClassTree) {
            type = fromClass((ClassTree)decl);
        } else if (decl instanceof VariableTree) {
            type = fromMember(decl);
        } else if (decl instanceof MethodTree) {
            type = fromMember(decl);
        } else if (decl.getKind() == Tree.Kind.TYPE_PARAMETER) {
            type = fromTypeTree(decl);
        } else {
            ErrorReporter.errorAbort("AnnotatedTypeFactory.fromElement: cannot be here! decl: " + decl.getKind() +
                    " elt: " + elt, null);
            type = null; // dead code
        }

        // Caching is disabled if indexTypes == null, because calls to this
        // method before the stub files are fully read can return incorrect
        // results.
        if (shouldCache && indexTypes != null)
            elementCache.put(elt, type.deepCopy());
        return type;
    }

    /**
     * Adds @FromByteCode to methods, constructors, and fields declared in class files
     * that are not already annotated with @FromStubFile
     */
    private void addFromByteCode(Element elt) {
        if (indexDeclAnnos == null) { // || trees.getTree(elt) != null) {
            // Parsing stub files, don't add @FromByteCode
            return;
        }

        if (elt.getKind() == ElementKind.CONSTRUCTOR ||
                elt.getKind() == ElementKind.METHOD || elt.getKind() == ElementKind.FIELD) {
            // Only add @FromByteCode to methods, constructors, and fields
            if (ElementUtils.isElementFromByteCode(elt)) {
                Set<AnnotationMirror> annos = indexDeclAnnos.get(ElementUtils
                        .getVerboseName(elt));
                if (annos == null) {
                    annos = AnnotationUtils.createAnnotationSet();
                    indexDeclAnnos.put(ElementUtils.getVerboseName(elt), annos);
                }
                if (!annos.contains(AnnotationUtils.fromClass(elements,
                        FromStubFile.class))) {
                    annos.add(fromByteCode);
                }
            }
        }
    }


    /**
     * Determines the annotated type of a class from its declaration.
     *
     * @param tree the class declaration
     * @return the annotated type of the class being declared
     */
    public AnnotatedDeclaredType fromClass(ClassTree tree) {
        return TypeFromTree.fromClassTree(this, tree);
    }

    /**
     * Determines the annotated type of a variable or method declaration.
     *
     * @param tree the variable or method declaration
     * @return the annotated type of the variable or method being declared
     * @throws IllegalArgumentException if {@code tree} is not a method or
     * variable declaration
     */
    public AnnotatedTypeMirror fromMember(Tree tree) {
        if (!(tree instanceof MethodTree || tree instanceof VariableTree)) {
            ErrorReporter.errorAbort("AnnotatedTypeFactory.fromMember: not a method or variable declaration: " + tree);
            return null; // dead code
        }
        if (fromTreeCache.containsKey(tree) && shouldReadCache) {
            return fromTreeCache.get(tree).deepCopy();
        }
        AnnotatedTypeMirror result = TypeFromTree.fromMember(this, tree);
        annotateInheritedFromClass(result);
        if (shouldCache)
            fromTreeCache.put(tree, result.deepCopy());
        return result;
    }

    /**
     * Determines the annotated type of an expression.
     *
     * @param tree an expression
     * @return the annotated type of the expression
     */
    public AnnotatedTypeMirror fromExpression(ExpressionTree tree) {
        if (fromTreeCache.containsKey(tree) && shouldReadCache)
            return fromTreeCache.get(tree).deepCopy();

        AnnotatedTypeMirror result = TypeFromTree.fromExpression(this, tree);

        annotateInheritedFromClass(result);

        if (shouldCache)
            fromTreeCache.put(tree, result.deepCopy());
        return result;
    }

    /**
     * Determines the annotated type from a type in tree form.  This method
     * does not add implicit annotations.
     *
     * @param tree the type tree
     * @return the annotated type of the type in the AST
     */
    public AnnotatedTypeMirror fromTypeTree(Tree tree) {
        if (fromTreeCache.containsKey(tree) && shouldReadCache) {
            return fromTreeCache.get(tree).deepCopy();
        }

        AnnotatedTypeMirror result = TypeFromTree.fromTypeTree(this, tree);

        // treat Raw as generic!
        // TODO: This doesn't handle recursive type parameter
        // e.g. class Pair<Y extends List<Y>> { ... }
        if (result.getKind() == TypeKind.DECLARED) {
            AnnotatedDeclaredType dt = (AnnotatedDeclaredType)result;
            if (dt.wasRaw()) {
                List<AnnotatedTypeMirror> typeArgs;
                Pair<Tree, AnnotatedTypeMirror> ctx = this.visitorState.getAssignmentContext();
                if (ctx != null) {
                    if (ctx.second.getKind() == TypeKind.DECLARED &&
                            types.isSameType(types.erasure(ctx.second.actualType), types.erasure(dt.actualType))) {
                        typeArgs = ((AnnotatedDeclaredType) ctx.second).getTypeArguments();
                    } else {
                        // TODO: we want a way to go from the raw type to an instantiation of the raw type
                        // that is compatible with the context.
                        typeArgs = null;
                    }
                } else {
                    // TODO: the context is null, use uninstantiated wildcards instead.
                    typeArgs = new ArrayList<AnnotatedTypeMirror>();
                    AnnotatedDeclaredType declaration = fromElement((TypeElement)dt.getUnderlyingType().asElement());
                    for (AnnotatedTypeMirror typeParam : declaration.getTypeArguments()) {
                        AnnotatedWildcardType wct = getUninferredWildcardType((AnnotatedTypeVariable) typeParam);
                        typeArgs.add(wct);
                    }
                }
                dt.setTypeArguments(typeArgs);
            }
        }
        annotateInheritedFromClass(result);
        if (shouldCache)
            fromTreeCache.put(tree, result.deepCopy());
        return result;
    }

    // **********************************************************************
    // Customization methods meant to be overridden by subclasses to include
    // implicit annotations
    // **********************************************************************

    /**
     * Adds implicit annotations to a type obtained from a {@link Tree}. By
     * default, this method does nothing. Subclasses should use this method to
     * implement implicit annotations specific to their type systems.
     *
     * @param tree an AST node
     * @param type the type obtained from {@code tree}
     */
    // TODO: make this method protected. At the moment there is one use in
    // AnnotatedTypes that is actually not desirable.
    // TODO: rename the method; it's not just implicits, but also defaulting, etc.
    public void annotateImplicit(Tree tree, /*@Mutable*/ AnnotatedTypeMirror type) {
        // Pass.
    }

    /**
     * Adds implicit annotations to a type obtained from a {@link Element}. By
     * default, this method does nothing. Subclasses should use this method to
     * implement implicit annotations specific to their type systems.
     *
     * @param elt an element
     * @param type the type obtained from {@code elt}
     */
    protected void annotateImplicit(Element elt, /*@Mutable*/ AnnotatedTypeMirror type) {
        // Pass.
    }

    /**
     * A callback method for the AnnotatedTypeFactory subtypes to customize
     * directSuperTypes().  Overriding methods should merely change the
     * annotations on the supertypes, without adding or removing new types.
     *
     * The default provided implementation adds {@code type} annotations to
     * {@code supertypes}.  This allows the {@code type} and its supertypes
     * to have the qualifiers, e.g. the supertypes of an {@code Immutable}
     * type are also {@code Immutable}.
     *
     * @param type  the type whose supertypes are desired
     * @param supertypes
     *      the supertypes as specified by the base AnnotatedTypeFactory
     *
     */
    protected void postDirectSuperTypes(AnnotatedTypeMirror type,
            List<? extends AnnotatedTypeMirror> supertypes) {
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
     * AnnotatedTypes.asMemberOf().  Overriding methods should merely change
     * the annotations on the subtypes, without changing the types.
     *
     * @param type  the annotated type of the element
     * @param owner the annotated type of the receiver of the accessing tree
     * @param element   the element of the field or method
     */
    public void postAsMemberOf(AnnotatedTypeMirror type,
            AnnotatedTypeMirror owner, Element element) {
        annotateImplicit(element, type);
    }

    /**
     * A callback method for the AnnotatedTypeFactory subtypes to customize
     * AnnotatedTypeMirror.substitute().
     *
     * @param varDecl   a declaration of a type variable
     * @param varUse    a use of the same type variable
     * @param value     the new type to substitute in for the type variable
     */
    public void postTypeVarSubstitution(AnnotatedTypeVariable varDecl,
            AnnotatedTypeVariable varUse, AnnotatedTypeMirror value) {
        if (!varUse.annotations.isEmpty()
                && !AnnotationUtils.areSame(varUse.annotations, varDecl.annotations)) {
            value.replaceAnnotations(varUse.annotations);
        }
    }


    /**
     * Adapt the upper bounds of the type variables of a class relative
     * to the type instantiation.
     * In some type systems, the upper bounds depend on the instantiation
     * of the class. For example, in the Generic Universe Type system,
     * consider a class declaration
     * <pre>   class C&lt;X extends @Peer Object&gt; </pre>
     * then the instantiation
     * <pre>   @Rep C&lt;@Rep Object&gt; </pre>
     * is legal. The upper bounds of class C have to be adapted
     * by the main modifier.
     *
     * <p>
     *
     * An example of an adaptation follows.  Suppose, I have a declaration:
     * class MyClass&lt;E extends Listlt;E&gt;&gt;
     * And an instantiation:
     * new MyClass&lt;@NonNull String&gt;()
     *
     * The upper bound of E adapted to the argument String, would be List&lt;@NonNull String&gt;
     * and the lower bound would be an AnnotatedNullType.
     *
     * TODO: ensure that this method is consistently used instead
     * of directly querying the type variables.
     *
     * @param type The use of the type
     * @param element The corresponding element
     * @return The adapted bounds of the type parameters
     */
    public List<AnnotatedTypeParameterBounds> typeVariablesFromUse(
            AnnotatedDeclaredType type, TypeElement element) {

        AnnotatedDeclaredType generic = getAnnotatedType(element);
        List<AnnotatedTypeMirror> targs = type.getTypeArguments();
        List<AnnotatedTypeMirror> tvars = generic.getTypeArguments();

        assert targs.size() == tvars.size() : "Mismatch in type argument size between " + type + " and " + generic;

        //System.err.printf("TVFU\n  type: %s\n  generic: %s\n", type, generic);

        Map<TypeVariable, AnnotatedTypeMirror> mapping = new HashMap<>();

        for (int i = 0; i < targs.size(); ++i) {
            mapping.put(((AnnotatedTypeVariable)tvars.get(i)).getUnderlyingType(), targs.get(i));
        }

        List<AnnotatedTypeParameterBounds> res = new LinkedList<>();

        for (AnnotatedTypeMirror atm : tvars) {
            AnnotatedTypeVariable atv = (AnnotatedTypeVariable)atm;
            AnnotatedTypeMirror upper = typeVarSubstitutor.substitute(mapping, atv.getUpperBound());
            AnnotatedTypeMirror lower = typeVarSubstitutor.substitute(mapping, atv.getLowerBound());
            res.add(new AnnotatedTypeParameterBounds(upper, lower));
        }
        return res;
    }

    /**
     * Adds annotations to the type based on the annotations from its class
     * type if and only if no annotations are already present on the type.
     *
     * @param type the type for which class annotations will be inherited if
     * there are no annotations already present
     */
    protected void annotateInheritedFromClass(/*@Mutable*/ AnnotatedTypeMirror type) {
        InheritedFromClassAnnotator.INSTANCE.visit(type, this);
    }

    /**
     * Callback to determine what to do with the annotations from a class declaration.
     */
    protected void annotateInheritedFromClass(/*@Mutable*/ AnnotatedTypeMirror type,
            Set<AnnotationMirror> fromClass) {
        type.addMissingAnnotations(fromClass);
    }

    /**
     * A singleton utility class for pulling annotations down from a class
     * type.
     *
     * @see #annotateInheritedFromClass
     */
    protected static class InheritedFromClassAnnotator
            extends AnnotatedTypeScanner<Void, AnnotatedTypeFactory> {

        /** The singleton instance. */
        public static final InheritedFromClassAnnotator INSTANCE
            = new InheritedFromClassAnnotator();

        private InheritedFromClassAnnotator() {}

        @Override
        public Void visitExecutable(AnnotatedExecutableType type, AnnotatedTypeFactory p) {

            // When visiting an executable type, skip the receiver so we
            // never inherit class annotations there.

            // Also skip constructor return types (which somewhat act like
            // the receiver).
            MethodSymbol methodElt = (MethodSymbol)type.getElement();
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
            TypeParameterElement tpelt = (TypeParameterElement) type.getUnderlyingType().asElement();
            if (!visited.containsKey(tpelt)) {
                visited.put(tpelt, type);
                if (type.getAnnotations().isEmpty() &&
                        type.getUpperBound().getAnnotations().isEmpty() &&
                        tpelt.getEnclosingElement().getKind() != ElementKind.TYPE_PARAMETER) {
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
     * The result is null for expressions that don't have a receiver,
     * e.g. for a local variable or method parameter access.
     *
     * TODO: receiver annotations on outer this.
     * TODO: Better document the difference between getImplicitReceiverType and getSelfType?
     *
     * @param tree The expression that might have an implicit receiver.
     * @return The type of the receiver.
     */
    /* TODO: this method assumes that the tree is within the current
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
                || tree.getKind() == Tree.Kind.NEW_CLASS) : "Unexpected tree kind: " + tree.getKind();

        Element element = InternalUtils.symbol(tree);
        assert element != null : "Unexpected null element for tree: " + tree;
        // Return null if the element kind has no receiver.
        if (!ElementUtils.hasReceiver(element)) {
            return null;
        }

        ExpressionTree receiver = TreeUtils.getReceiverTree(tree);
        if (receiver==null) {
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
                    ErrorReporter.errorAbort("AnnotatedTypeFactory.getImplicitReceiver: enclosingClass()==null for element: " + element);
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
                && ((IdentifierTree)receiver).getName().contentEquals("this")) {
            // TODO: also "super"?
            return this.getSelfType(tree);
        }

        TypeElement typeElt = ElementUtils.enclosingClass(rcvelem);
        if (typeElt == null) {
            ErrorReporter.errorAbort("AnnotatedTypeFactory.getImplicitReceiver: enclosingClass()==null for element: " + rcvelem);
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
     * Determine whether the tree dereferences the most enclosing "this" object.
     * That is, we have an expression like "f.g" and want to know whether it is
     * an access "this.f.g" or whether e.g. f is a field of an outer class or
     * e.g. f is a local variable.
     *
     * @param tree The tree to check.
     * @return True, iff the tree is an explicit or implicit reference to the
     *         most enclosing "this".
     */
    public final boolean isMostEnclosingThisDeref(ExpressionTree tree) {
        if (!isAnyEnclosingThisDeref(tree)) {
            return false;
        }

        Element element = TreeUtils.elementFromUse(tree);
        TypeElement typeElt = ElementUtils.enclosingClass(element);

        ClassTree enclosingClass = getCurrentClassTree(tree);
        if (enclosingClass != null && isSubtype(TreeUtils.elementFromDeclaration(enclosingClass), typeElt)) {
            return true;
        }

        // ran out of options
        return false;
    }

    /**
     * Does this expression have (the innermost or an outer) "this" as receiver?
     * Note that the receiver can be either explicit or implicit.
     *
     * @param tree The tree to test.
     * @return True, iff the expression uses (the innermost or an outer) "this" as receiver.
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
                Name n = ((IdentifierTree)tree).getName();
                if ("this".contentEquals(n) ||
                        "super".contentEquals(n)) {
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
     * Returns the type of {@code this} in the current location, which can
     * be used if {@code this} has a special semantics (e.g. {@code this}
     * is non-null).
     *
     * The parameter is an arbitrary tree and does not have to mention "this",
     * neither explicitly nor implicitly.
     * This method should be overridden for type-system specific behavior.
     *
     * TODO: in 1.8.2, handle all receiver type annotations.
     * TODO: handle enclosing classes correctly.
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
        if (enclosingClass.getSimpleName().length() != 0 &&
                enclosingMethod != null) {
            AnnotatedDeclaredType methodReceiver;
            if (TreeUtils.isConstructor(enclosingMethod)) {
                methodReceiver = (AnnotatedDeclaredType) getAnnotatedType(enclosingMethod).getReturnType();
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
     * Determine the type of the most enclosing class of the given tree that
     * is a subtype of the given element. Receiver type annotations of an
     * enclosing method are considered, similarly return type annotations of an
     * enclosing constructor.
     */
    public AnnotatedDeclaredType getEnclosingType(TypeElement element, Tree tree) {
        Element enclosingElt = getMostInnerClassOrMethod(tree);

        while (enclosingElt != null) {
            if (enclosingElt instanceof ExecutableElement) {
                ExecutableElement method = (ExecutableElement)enclosingElt;
                if (method.asType() != null // XXX: hack due to a compiler bug
                        && isSubtype((TypeElement)method.getEnclosingElement(), element)) {
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
                if (isSubtype((TypeElement)enclosingElt, element)) {
                    return (AnnotatedDeclaredType) getAnnotatedType(enclosingElt);
                }
            }
            enclosingElt = enclosingElt.getEnclosingElement();
        }
        return null;
    }

    private boolean isSubtype(TypeElement a1, TypeElement a2) {
        return (a1.equals(a2)
                || types.isSubtype(types.erasure(a1.asType()),
                        types.erasure(a2.asType())));
    }

    /**
     * Returns the receiver type of the expression tree, or null if it does not exist.
     *
     * The only trees that could potentially have a receiver are:
     * <ul>
     *  <li> Array Access
     *  <li> Identifiers (whose receivers are usually self type)
     *  <li> Method Invocation Trees
     *  <li> Member Select Trees
     * </ul>
     *
     * @param expression The expression for which to determine the receiver type
     * @return  the type of the receiver of this expression
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
     * Determines the type of the invoked method based on the passed method
     * invocation tree.
     *
     * The returned method type has all type variables resolved, whether based
     * on receiver type, passed type parameters if any, and method invocation
     * parameter.
     *
     * Subclasses may override this method to customize inference of types
     * or qualifiers based on method invocation parameters.
     *
     * As an implementation detail, this method depends on
     * {@link AnnotatedTypes#asMemberOf(Types, AnnotatedTypeFactory, AnnotatedTypeMirror, Element)},
     * and customization based on receiver type should be in accordance to its
     * specification.
     *
     * The return type is a pair of the type of the invoked method and
     * the (inferred) type arguments.
     * Note that neither the explicitly passed nor the inferred type arguments
     * are guaranteed to be subtypes of the corresponding upper bounds.
     * See method
     * {@link org.checkerframework.common.basetype.BaseTypeVisitor#checkTypeArguments(Tree, List, List, List)}
     * for the checks of type argument well-formedness.
     *
     * Note that "this" and "super" constructor invocations are also handled by this
     * method. Method {@link #constructorFromUse(NewClassTree)} is only used for a constructor
     * invocation in a "new" expression.
     *
     * @param tree the method invocation tree
     * @return the method type being invoked with tree and the (inferred) type arguments
     */
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(MethodInvocationTree tree) {
        ExecutableElement methodElt = TreeUtils.elementFromUse(tree);
        AnnotatedTypeMirror receiverType = getReceiverType(tree);

        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair =  methodFromUse(tree, methodElt, receiverType);
        if (checker.shouldResolveReflection() && reflectionResolver.isReflectiveMethodInvocation(tree)) {
            mfuPair = reflectionResolver.resolveReflectiveCall(this, tree, mfuPair);
        }
        return mfuPair;
    }

    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(ExpressionTree tree,
            ExecutableElement methodElt, AnnotatedTypeMirror receiverType) {

        AnnotatedExecutableType methodType = AnnotatedTypes.asMemberOf(types, this, receiverType, methodElt);
        List<AnnotatedTypeMirror> typeargs = new LinkedList<AnnotatedTypeMirror>();

        Map<TypeVariable, AnnotatedTypeMirror> typeVarMapping =
            AnnotatedTypes.findTypeArguments(processingEnv, this, tree, methodElt, methodType);

        if (!typeVarMapping.isEmpty()) {
            for (AnnotatedTypeVariable tv : methodType.getTypeVariables()) {
                if (typeVarMapping.get(tv.getUnderlyingType()) == null) {
                    ErrorReporter.errorAbort("AnnotatedTypeFactory.methodFromUse:" +
                            "mismatch between declared method type variables and the inferred method type arguments! " +
                            "Method type variables: " + methodType.getTypeVariables() + "; " +
                            "Inferred method type arguments: " + typeVarMapping);
                }
                typeargs.add(typeVarMapping.get(tv.getUnderlyingType()));
            }
            methodType = (AnnotatedExecutableType) typeVarSubstitutor.substitute(typeVarMapping, methodType);
        }

        if (tree.getKind() == Tree.Kind.METHOD_INVOCATION &&
                TreeUtils.isGetClassInvocation((MethodInvocationTree) tree, processingEnv)) {
            adaptGetClassReturnTypeToReceiver(methodType, receiverType);
        }

        return Pair.of(methodType, typeargs);
    }

    /**
     * Java special cases the return type of getClass.  Though the method has a return type of {@code Class<?>},
     * the compiler special cases this return type and changes the bound of the type argument to the
     * erasure of the receiver type.  e.g.,
     *
     * x.getClass() has the type {@code Class< ? extends erasure_of_x >}
     * someInteger.getClass() has the type {@code Class< ? extends Integer >}
     *
     * @param getClassType This must be a type representing a call to Object.getClass otherwise
     *                     a runtime exception will be thrown
     * @param receiverType The receiver type of the method invocation (not the declared receiver type)
     */
    protected static void adaptGetClassReturnTypeToReceiver(final AnnotatedExecutableType getClassType,
                                                            final AnnotatedTypeMirror receiverType) {
        final AnnotatedTypeMirror newBound = receiverType.getErased();

        final AnnotatedTypeMirror returnType = getClassType.getReturnType();
        if ( returnType == null || !(returnType.getKind() == TypeKind.DECLARED)
          || ((AnnotatedDeclaredType) returnType).getTypeArguments().size() != 1 ) {
            ErrorReporter.errorAbort(
                    "Unexpected type passed to AnnotatedTypes.adaptGetClassReturnTypeToReceiver\n"
                            + "getClassType=" + getClassType + "\n"
                            + "receiverType=" + receiverType);
        }

        final AnnotatedDeclaredType returnAdt = (AnnotatedDeclaredType) getClassType.getReturnType();
        final List<AnnotatedTypeMirror> typeArgs = returnAdt.getTypeArguments();

        //usually, the only locations that will add annotations to the return type are getClass in stub files
        //defaults and propagation tree annotator.  Since getClass is final they cannot come from source code.
        //Also, since the newBound is an erased type we have no type arguments.  So, we just copy the annotations
        //from the bound of the declared type to the new bound.
        final AnnotatedWildcardType classWildcardArg = (AnnotatedWildcardType) typeArgs.get(0);
        newBound.replaceAnnotations(classWildcardArg.getExtendsBound().getAnnotations());

        classWildcardArg.setExtendsBound(newBound);
    }

    /**
     * Determines the {@link AnnotatedExecutableType} of a constructor
     * invocation. Note that this is different than calling
     * {@link #getAnnotatedType(Tree)} or
     * {@link #fromExpression(ExpressionTree)} on the constructor invocation;
     * those determine the type of the <i>result</i> of invoking the
     * constructor, which is probably an {@link AnnotatedDeclaredType}.
     * TODO: Should the result of getAnnotatedType be the return type
     *   from the AnnotatedExecutableType computed here?
     *
     * Note that "this" and "super" constructor invocations are handled by
     * method {@link #methodFromUse}. This method only handles constructor invocations
     * in a "new" expression.
     *
     * @param tree the constructor invocation tree
     * @return the annotated type of the invoked constructor (as an executable
     *         type) and the (inferred) type arguments
     */
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> constructorFromUse(NewClassTree tree) {
        ExecutableElement ctor = InternalUtils.constructor(tree);
        AnnotatedTypeMirror type = fromNewClass(tree);
        annotateImplicit(tree.getIdentifier(), type);
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

    /**
     * Returns the return type of the method {@code m}.
     */
    public AnnotatedTypeMirror getMethodReturnType(MethodTree m) {
        AnnotatedExecutableType methodType = getAnnotatedType(m);
        AnnotatedTypeMirror ret = methodType.getReturnType();
        return ret;
    }

    /**
     * Returns the return type of the method {@code m} at the return statement {@code r}.
     */
    public AnnotatedTypeMirror getMethodReturnType(MethodTree m, ReturnTree r) {
        AnnotatedExecutableType methodType = getAnnotatedType(m);
        AnnotatedTypeMirror ret = methodType.getReturnType();
        return ret;
    }

    private boolean isSyntheticArgument(Tree tree) {
        return tree.toString().contains("<*nullchk*>");
    }

    public AnnotatedDeclaredType fromNewClass(NewClassTree tree) {
        AnnotatedDeclaredType type;
        if (!TreeUtils.isDiamondTree(tree)) {
            type = (AnnotatedDeclaredType) fromTypeTree(tree.getIdentifier());
        } else {
            type = (AnnotatedDeclaredType) toAnnotatedType(InternalUtils.typeOf(tree), false);
        }

        if (tree.getClassBody() != null) {
            // TODO: try to remove this - javac should add the annotations to the type already.
            List<? extends AnnotationTree> annos = tree.getClassBody().getModifiers().getAnnotations();
            type.replaceAnnotations(InternalUtils.annotationsFromTypeAnnotationTrees(annos));
        }

        if (TreeUtils.isDiamondTree(tree)) {
            if (((com.sun.tools.javac.code.Type)type.actualType).tsym.getTypeParameters().nonEmpty()) {
                Pair<Tree, AnnotatedTypeMirror> ctx = this.visitorState.getAssignmentContext();
                if (ctx != null) {
                    AnnotatedTypeMirror ctxtype = ctx.second;
                    fromNewClassContextHelper(type, ctxtype);
                }
            }
        }

        return type;
    }

    // This method extracts the ugly hacky parts.
    // This method should be rewritten and in particular diamonds should be
    // implemented cleanly.
    // See Issue 289.
    private void fromNewClassContextHelper(AnnotatedDeclaredType type, AnnotatedTypeMirror ctxtype) {
        switch (ctxtype.getKind()) {
        case DECLARED:
            AnnotatedDeclaredType adctx = (AnnotatedDeclaredType) ctxtype;

            if (type.getTypeArguments().size() == adctx.getTypeArguments().size()) {
                // Try to simply take the type arguments from LHS.
                List<AnnotatedTypeMirror> oldArgs = type.getTypeArguments();
                List<AnnotatedTypeMirror> newArgs = adctx.getTypeArguments();
                for (int i = 0; i < type.getTypeArguments().size(); ++i) {
                    if (!types.isSameType(oldArgs.get(i).actualType, newArgs.get(i).actualType)) {
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
        default:
            ErrorReporter.errorAbort("AnnotatedTypeFactory.fromNewClassContextHelper: unexpected context: " +
                    ctxtype + " (" + ctxtype.getKind() + ")");
        }
    }

    /**
     * Returns the annotated boxed type of the given primitive type.
     * The returned type would only have the annotations on the given type.
     *
     * Subclasses may override this method safely to override this behavior.
     *
     * @param type  the primitive type
     * @return the boxed declared type of the passed primitive type
     */
    public AnnotatedDeclaredType getBoxedType(AnnotatedPrimitiveType type) {
        TypeElement typeElt = types.boxedClass(type.getUnderlyingType());
        AnnotatedDeclaredType dt = fromElement(typeElt);
        dt.addAnnotations(type.getAnnotations());
        return dt;
    }

    /**
     * returns the annotated primitive type of the given declared type
     * if it is a boxed declared type.  Otherwise, it throws
     * <i>IllegalArgumentException</i> exception.
     *
     * The returned type would have the annotations on the given type and
     * nothing else.
     *
     * @param type  the declared type
     * @return the unboxed primitive type
     * @throws IllegalArgumentException if the type given has no unbox conversion
     */
    public AnnotatedPrimitiveType getUnboxedType(AnnotatedDeclaredType type)
            throws IllegalArgumentException {
        PrimitiveType primitiveType =
            types.unboxedType(type.getUnderlyingType());
        AnnotatedPrimitiveType pt = (AnnotatedPrimitiveType)
            AnnotatedTypeMirror.createType(primitiveType, this, false);
        pt.addAnnotations(type.getAnnotations());
        return pt;
    }

    /**
     * Returns the VisitorState instance used by the factory to infer types
     */
    public VisitorState getVisitorState() {
        return this.visitorState;
    }

    // **********************************************************************
    // random methods wrapping #getAnnotatedType(Tree) and #fromElement(Tree)
    // with appropriate casts to reduce casts on the client side
    // **********************************************************************

    /**
     * @see #getAnnotatedType(Tree)
     */
    public final AnnotatedDeclaredType getAnnotatedType(ClassTree tree) {
        return (AnnotatedDeclaredType)getAnnotatedType((Tree)tree);
    }

    /**
     * @see #getAnnotatedType(Tree)
     */
    public final AnnotatedDeclaredType getAnnotatedType(NewClassTree tree) {
        return (AnnotatedDeclaredType)getAnnotatedType((Tree)tree);
    }

    /**
     * @see #getAnnotatedType(Tree)
     */
    public final AnnotatedArrayType getAnnotatedType(NewArrayTree tree) {
        return (AnnotatedArrayType)getAnnotatedType((Tree)tree);
    }

    /**
     * @see #getAnnotatedType(Tree)
     */
    public final AnnotatedExecutableType getAnnotatedType(MethodTree tree) {
        return (AnnotatedExecutableType)getAnnotatedType((Tree)tree);
    }


    /**
     * @see #getAnnotatedType(Element)
     */
    public final AnnotatedDeclaredType getAnnotatedType(TypeElement elt) {
        return (AnnotatedDeclaredType)getAnnotatedType((Element)elt);
    }

    /**
     * @see #getAnnotatedType(Element)
     */
    public final AnnotatedExecutableType getAnnotatedType(ExecutableElement elt) {
        return (AnnotatedExecutableType)getAnnotatedType((Element)elt);
    }

    /**
     * @see #getAnnotatedType(Element)
     */
    public final AnnotatedDeclaredType fromElement(TypeElement elt) {
        return (AnnotatedDeclaredType)fromElement((Element)elt);
    }

    /**
     * @see #getAnnotatedType(Element)
     */
    public final AnnotatedExecutableType fromElement(ExecutableElement elt) {
        return (AnnotatedExecutableType)fromElement((Element)elt);
    }

    // **********************************************************************
    // Helper methods for this classes
    // **********************************************************************

    /**
     * Determines whether the given annotation is a part of the type system
     * under which this type factory operates.
     * Null is never a supported qualifier; the parameter is nullable to
     * allow the result of aliasedAnnotation to be passed in directly.
     *
     * @param a any annotation
     * @return true if that annotation is part of the type system under which
     *         this type factory operates, false otherwise
     */
    public boolean isSupportedQualifier(/*@Nullable*/ AnnotationMirror a) {
        if (a == null) return false;
        return AnnotationUtils.containsSameIgnoringValues(this.getQualifierHierarchy().getTypeQualifiers(), a);
    }

    /** Add the annotation clazz as an alias for the annotation type. */
    protected void addAliasedAnnotation(Class<?> alias, AnnotationMirror type) {
        aliases.put(alias.getCanonicalName(), type);
    }

    /**
     * Returns the canonical annotation for the passed annotation if it is
     * an alias of a canonical one in the framework.  If it is not an alias,
     * the method returns null.
     *
     * Returns an aliased type of the current one
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
     * Add the annotation {@code alias} as an alias for the declaration
     * annotation {@code annotation}, where the annotation mirror
     * {@code annoationToUse} will be used instead. If multiple calls are made
     * with the same {@code annotation}, then the {@code anontationToUse} must
     * be the same.
     */
    protected void addAliasedDeclAnnotation(Class<? extends Annotation> alias,
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
     * Adds the annotation {@code annotation} in the set of declaration
     * annotations that should be inherited. A declaration annotation
     * will be inherited if it is in this list,  or if it has the
     * meta-annotation @InheritedAnnotation.
     */
    protected void addInheritedAnnotation(AnnotationMirror annotation) {
        inheritedAnnotations.add(annotation);
    }

    /**
     * A convenience method that converts a {@link TypeMirror} to an {@link
     * AnnotatedTypeMirror} using {@link AnnotatedTypeMirror#createType}.
     *
     * @param t the {@link TypeMirror}
     * @param declaration   true if the result should be marked as a type declaration
     * @return an {@link AnnotatedTypeMirror} that has {@code t} as its
     * underlying type
     */
    public final AnnotatedTypeMirror toAnnotatedType(TypeMirror t, boolean declaration) {
        return AnnotatedTypeMirror.createType(t, this, declaration);
    }

    /**
     * Determines an empty annotated type of the given tree. In other words,
     * finds the {@link TypeMirror} for the tree and converts that into an
     * {@link AnnotatedTypeMirror}, but does not add any annotations to the
     * result.
     *
     * Most users will want to use getAnnotatedType instead; this method
     * is mostly for internal use.
     *
     * @param node the tree to analyze
     * @return the type of {@code node}, without any annotations
     */
    public AnnotatedTypeMirror type(Tree node) {
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
     * TODO: would be nice to move this to InternalUtils/TreeUtils.
     *
     * @param elt   an element
     * @return the tree declaration of the element if found
     */
    public final Tree declarationFromElement(Element elt) {
        // if root is null, we cannot find any declaration
        if (root == null)
            return null;
        if (elementToTreeCache.containsKey(elt) && shouldReadCache) {
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
            fromElt = com.sun.tools.javac.tree.TreeInfo.declarationFor((com.sun.tools.javac.code.Symbol) elt,
                    (com.sun.tools.javac.tree.JCTree) root);
            break;
        }
        if (shouldCache)
            elementToTreeCache.put(elt, fromElt);
        return fromElt;
    }

    /**
     * Returns the current class type being visited by the visitor.  The method
     * uses the parameter only if the most enclosing class cannot be found
     * directly.
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
     * Returns the receiver type of the current method being visited, and
     * returns null if the visited tree is not within a method or if that
     * method has no receiver (e.g. a static method).
     *
     * The method uses the parameter only if the most enclosing method cannot
     * be found directly.
     *
     * @return receiver type of the most enclosing method being visited.
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
        if (visitorState.getClassType() != null)
            return visitorState.getMethodTree() != null
                && TreeUtils.isConstructor(visitorState.getMethodTree());

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(getPath(tree));
        return enclosingMethod != null && TreeUtils.isConstructor(enclosingMethod);
    }

    private final Element getMostInnerClassOrMethod(Tree tree) {
        if (visitorState.getMethodTree() != null)
            return TreeUtils.elementFromDeclaration(visitorState.getMethodTree());
        if (visitorState.getClassTree() != null)
            return TreeUtils.elementFromDeclaration(visitorState.getClassTree());

        TreePath path = getPath(tree);
        if (path == null) {
            ErrorReporter.errorAbort(String.format("AnnotatedTypeFactory.getMostInnerClassOrMethod: getPath(tree)=>null%n  TreePath.getPath(root, tree)=>%s\n  for tree (%s) = %s%n  root=%s",
                                                   TreePath.getPath(root, tree), tree.getClass(), tree, root));
            return null; // dead code
        }
        for (Tree pathTree : path) {
            if (pathTree instanceof MethodTree)
                return TreeUtils.elementFromDeclaration((MethodTree)pathTree);
            else if (pathTree instanceof ClassTree)
                return TreeUtils.elementFromDeclaration((ClassTree)pathTree);
        }

        ErrorReporter.errorAbort("AnnotatedTypeFactory.getMostInnerClassOrMethod: cannot be here!");
        return null; // dead code
    }

    private final Map<Tree, Element> pathHack = new HashMap<>();
    public final void setPathHack(Tree node, Element enclosing) {
        pathHack.put(node, enclosing);
    }

    /**
     * Gets the path for the given {@link Tree} under the current root by
     * checking from the visitor's current path, and only using
     * {@link Trees#getPath(CompilationUnitTree, Tree)} (which is much slower)
     * only if {@code node} is not found on the current path.
     *
     * Note that the given Tree has to be within the current compilation unit,
     * otherwise null will be returned.
     *
     * @param node the {@link Tree} to get the path for
     * @return the path for {@code node} under the current root
     */
    public final TreePath getPath(Tree node) {
        assert root != null : "AnnotatedTypeFactory.getPath: root needs to be set when used on trees; factory: " + this.getClass();

        if (node == null) return null;

        if (treePathCache.isCached(node)) {
            return treePathCache.getPath(root, node);
        };

        TreePath currentPath = visitorState.getPath();
        if (currentPath == null)
            return TreePath.getPath(root, node);

        // This method uses multiple heuristics to avoid calling
        // TreePath.getPath()

        // If the current path you are visiting is for this node we are done
        if (currentPath.getLeaf() == node) {
            return currentPath;
        }

        // When running on Daikon, we noticed that a lot of calls happened
        // within a small subtree containing the node we are currently visiting

        // When testing on Daikon, two steps resulted in the best performance
        if (currentPath.getParentPath() != null)
            currentPath = currentPath.getParentPath();
        if (currentPath.getLeaf() == node) {
            return currentPath;
        }
        if (currentPath.getParentPath() != null)
            currentPath = currentPath.getParentPath();
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
            if (current.getLeaf() == node)
                return current;
            current = current.getParentPath();
        }

        // OK, we give up. Use the cache to look up.
        return treePathCache.getPath(root, node);
    }

    /**
     * Gets the {@link Element} representing the declaration of the
     * method enclosing a tree node. This feature is used to record
     * the enclosing methods of {@link Tree}s that are created
     * internally by the checker.
     *
     * TODO: Find a better way to store information about enclosing
     * Trees.
     *
     * @param node the {@link Tree} to get the enclosing method for
     * @return the method {@link Element} enclosing the argument, or
     * null if none has been recorded
     */
    public final Element getEnclosingMethod(Tree node) {
        return pathHack.get(node);
    }

    /**
     * Assert that the type is a type of valid type mirror, i.e. not an ERROR
     * or OTHER type.
     *
     * @param type an annotated type
     * @return true if the type is a valid annotated type, false otherwise
     */
    static final boolean validAnnotatedType(AnnotatedTypeMirror type) {
        if (type == null)
            return false;
        if (type.getUnderlyingType() == null)
            return true; // e.g., for receiver types
        return validType(type.getUnderlyingType());
    }

    /**
     * Used for asserting that a type is valid for converting to an annotated
     * type.
     *
     * @return true if {@code type} can be converted to an annotated type, false
     *         otherwise
     */
    private static final boolean validType(TypeMirror type) {
        if (type == null)
            return false;
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
     * A Utility method for creating LRU cache
     * @param size  size of the cache
     * @return  a new cache with the provided size
     */
    public static <K, V> Map<K, V> createLRUCache(final int size) {
        return new LinkedHashMap<K, V>() {

            private static final long serialVersionUID = 5261489276168775084L;
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
                return size() > size;
            }
        };
    }

    /** Sets indexTypes and indexDeclAnnos by side effect, just before returning. */
    protected void buildIndexTypes() {
        if (this.indexTypes != null || this.indexDeclAnnos != null) {
            ErrorReporter.errorAbort("AnnotatedTypeFactory.buildIndexTypes called more than once");
        }

        Map<Element, AnnotatedTypeMirror> indexTypes
            = new HashMap<Element, AnnotatedTypeMirror>();
        Map<String, Set<AnnotationMirror>> indexDeclAnnos
            = new HashMap<String, Set<AnnotationMirror>>();

        if (!checker.hasOption("ignorejdkastub")) {
            InputStream in = null;
            if (checker != null)
                in = checker.getClass().getResourceAsStream("jdk.astub");
            if (in != null) {
                StubParser stubParser = new StubParser("jdk.astub", in, this, processingEnv);
                stubParser.parse(indexTypes, indexDeclAnnos);
            }
        }

        // stub file for type-system independent annotations
        InputStream input = BaseTypeChecker.class.getResourceAsStream("flow.astub");
        if (input != null) {
            StubParser stubParser = new StubParser("flow.astub", input, this, processingEnv);
            stubParser.parse(indexTypes, indexDeclAnnos);
        }

        String allstubFiles = "";
        String stubFiles;

        stubFiles = checker.getOption("stubs");
        if (stubFiles != null)
            allstubFiles += File.pathSeparator + stubFiles;

        stubFiles = System.getProperty("stubs");
        if (stubFiles != null)
            allstubFiles += File.pathSeparator + stubFiles;

        stubFiles = System.getenv("stubs");
        if (stubFiles != null)
            allstubFiles += File.pathSeparator + stubFiles;

        {
            StubFiles sfanno = checker.getClass().getAnnotation(StubFiles.class);
            if (sfanno != null) {
                String[] sfarr = sfanno.value();
                stubFiles = "";
                for (String sf : sfarr) {
                    stubFiles += File.pathSeparator + sf;
                }
                allstubFiles += stubFiles;
            }
        }

        if (allstubFiles.isEmpty()) {
            this.indexTypes = indexTypes;
            this.indexDeclAnnos = indexDeclAnnos;
            return;
        }

        String[] stubArray = allstubFiles.split(File.pathSeparator);
        for (String stubPath : stubArray) {
            if (stubPath == null || stubPath.isEmpty()) continue;
            // Handle case when running in jtreg
            String base = System.getProperty("test.src");
            String stubPathFull = stubPath;
            if (base != null)
                stubPathFull = base + "/" + stubPath;
            List<StubResource> stubs = StubUtil.allStubFiles(stubPathFull);
            if (stubs.size() == 0) {
                InputStream in = null;
                if (checker != null)
                    in = checker.getClass().getResourceAsStream(stubPath);
                if (in != null) {
                    StubParser stubParser = new StubParser(stubPath, in, this, processingEnv);
                    stubParser.parse(indexTypes, indexDeclAnnos);
                    // We could handle the stubPath -> continue.
                    continue;
                }
                // We couldn't handle the stubPath -> error message.
                checker.message(Kind.NOTE,
                        "Did not find stub file or files within directory: " + stubPath + " " + new File(stubPath).getAbsolutePath());
            }
            for (StubResource resource : stubs) {
                InputStream stubStream;
                try {
                    stubStream = resource.getInputStream();
                } catch (IOException e) {
                    checker.message(Kind.NOTE,
                            "Could not read stub resource: " + resource.getDescription());
                    continue;
                }
                StubParser stubParser = new StubParser(resource.getDescription(), stubStream, this, processingEnv);
                stubParser.parse(indexTypes, indexDeclAnnos);
            }
        }

        this.indexTypes = indexTypes;
        this.indexDeclAnnos = indexDeclAnnos;
        return;
    }

    /**
     * Returns the actual annotation mirror used to annotate this element,
     * whose name equals the passed annotation class, if one exists, or null otherwise.
     *
     * @param anno annotation class
     * @return the annotation mirror for anno
     */
    @Override
    public AnnotationMirror getDeclAnnotation(Element elt,
            Class<? extends Annotation> anno) {
        String annoName = anno.getCanonicalName().intern();
        return getDeclAnnotation(elt, annoName, true);
    }

    /**
     * Returns true if the element appears in a stub file
     * (Currently only works for methods, constructors, and fields)
     */
    public boolean isFromStubFile(Element element) {
        return this.getDeclAnnotation(element, FromStubFile.class) != null;
    }

    /**
     * Returns true if the element is from byte code
     * and the if the element did not appear in a stub file
     * (Currently only works for methods, constructors, and fields)
     */
    public boolean isFromByteCode(Element element) {
        if (isFromStubFile(element)) return false;
        return this.getDeclAnnotation(element, FromByteCode.class) != null;
    }

    /**
     * Returns the actual annotation mirror used to annotate this type, whose
     * name equals the passed annotationName if one exists, null otherwise. This
     * is the private implementation of the same-named, public method.
     */
    private AnnotationMirror getDeclAnnotation(Element elt,
            /*@Interned*/ String annoName, boolean checkAliases) {
        Set<AnnotationMirror> declAnnos = getDeclAnnotations(elt);

        for (AnnotationMirror am : declAnnos) {
            if (AnnotationUtils.areSameByName(am, annoName)) {
                return am;
            }
        }
        // Look through aliases.
        if (checkAliases) {
            Pair<AnnotationMirror, Set</*@Interned*/ String>> aliases =
                    declAliases.get(annoName);
            if (aliases != null) {
                for (String alias : aliases.second) {
                    AnnotationMirror declAnnotation = getDeclAnnotation(elt,
                            alias, false);
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
     * Returns all of the actual annotation mirrors used to annotate this element
     * (includes stub files and declaration annotations from overridden methods).
     *
     * @param elt
     *            The element for which to determine annotations.
     */
    public Set<AnnotationMirror> getDeclAnnotations(Element elt) {
        if (cacheDeclAnnos.containsKey(elt)) {
            //Found in cache, return result.
            return cacheDeclAnnos.get(elt);
        }

        Set<AnnotationMirror> results = AnnotationUtils.createAnnotationSet();
        // Retrieving the annotations from the element.
        results.addAll(elt.getAnnotationMirrors());
        // If indexDeclAnnos == null, return the annotations in the element.
        if (indexDeclAnnos != null) {
            // Adding @FromByteCode annotation to indexDeclAnnos entry with key
            // elt, if elt is from bytecode.
            addFromByteCode(elt);

            // Retrieving annotations from stub files.
            String eltName = ElementUtils.getVerboseName(elt);
            Set<AnnotationMirror> stubAnnos = indexDeclAnnos.get(eltName);
            if (stubAnnos != null) {
                results.addAll(stubAnnos);
            }

            // Retrieve the annotations from the overridden method's element.
            inheritOverriddenDeclAnnos(elt, results);

            // Add the element and its annotations to the cache.
            cacheDeclAnnos.put(elt, results);
        }

        return results;
    }

    /**
     * Adds into {@code results} the declaration annotations found in all
     * elements that the method element {@code elt} overrides.
     *
     * @param elt
     *          Method element.
     * @param results
     *          {@code elt} local declaration annotations. The ones found
     *          in stub files and in the element itself.
     */

    private void inheritOverriddenDeclAnnos(Element elt,
            Set<AnnotationMirror> results) {
        Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods = null;
        if (elt instanceof ExecutableElement) {
            overriddenMethods = AnnotatedTypes.overriddenMethods(elements,
                    this, (ExecutableElement) elt);
        }

        if (overriddenMethods != null) {
            for (Map.Entry<AnnotatedDeclaredType, ExecutableElement>
                    pair : overriddenMethods.entrySet()) {
                // Getting annotations from super implementation.
                AnnotatedDeclaredType overriddenType = pair.getKey();
                AnnotatedExecutableType overriddenMethod = AnnotatedTypes
                        .asMemberOf(types, this, overriddenType,pair.getValue());
                ExecutableElement superElt = overriddenMethod.getElement();
                Set<AnnotationMirror> superAnnos = getDeclAnnotations(superElt);

                for (AnnotationMirror annotation : superAnnos) {
                    List<? extends AnnotationMirror> annotationsOnAnnotation;
                    try {
                        annotationsOnAnnotation = annotation
                                .getAnnotationType().asElement()
                                .getAnnotationMirrors();
                    } catch (com.sun.tools.javac.code.Symbol.CompletionFailure cf) {
                        // Fix for Issue 348: If a CompletionFailure occurs,
                        // issue a warning.
                        checker.message(Kind.WARNING, annotation
                                .getAnnotationType().asElement(),
                                "annotation.not.completed", ElementUtils
                                        .getVerboseName(elt), annotation);
                        continue;
                    }
                    if (AnnotationUtils.containsSameByClass(
                            annotationsOnAnnotation, InheritedAnnotation.class)
                            || AnnotationUtils.containsSameIgnoringValues(
                                    inheritedAnnotations, annotation)) {
                        results.add(annotation);
                    }
                }
            }
        }
    }

    /**
     * Returns a list of all declaration annotations used to annotate this element,
     * which have a meta-annotation (i.e., an annotation on that annotation)
     * with class {@code metaAnnotation}.
     *
     * @param element
     *            The element for which to determine annotations.
     * @param metaAnnotation
     *            The meta-annotation that needs to be present.
     * @return A list of pairs {@code (anno, metaAnno)} where {@code anno} is
     *         the annotation mirror at {@code element}, and {@code metaAnno} is
     *         the annotation mirror used to annotate {@code anno}.
     */
    public List<Pair<AnnotationMirror, AnnotationMirror>> getDeclAnnotationWithMetaAnnotation(
            Element element, Class<? extends Annotation> metaAnnotation) {
        List<Pair<AnnotationMirror, AnnotationMirror>> result = new ArrayList<>();
        Set<AnnotationMirror> annotationMirrors = getDeclAnnotations(element);

        // Go through all annotations found.
        for (AnnotationMirror annotation : annotationMirrors) {
            List<? extends AnnotationMirror> annotationsOnAnnotation;
            try {
                annotationsOnAnnotation = annotation.getAnnotationType().asElement().getAnnotationMirrors();
            } catch (com.sun.tools.javac.code.Symbol.CompletionFailure cf) {
                // Fix for Issue 309: If a CompletionFailure occurs, issue a warning.
                // I didn't find a nicer alternative to check whether the Symbol can be completed.
                // The completer field of a Symbol might be non-null also in successful cases.
                // Issue a warning (exception only happens once) and continue.
                checker.message(Kind.WARNING, annotation.getAnnotationType().asElement(),
                        "annotation.not.completed", ElementUtils.getVerboseName(element), annotation);
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
     * Returns a list of all annotations used to annotate this element,
     * which have a meta-annotation (i.e., an annotation on that annotation)
     * with class {@code metaAnnotation}.
     *
     * @param element
     *            The element at which to look for annotations.
     * @param metaAnnotation
     *            The meta-annotation that needs to be present.
     * @return A list of pairs {@code (anno, metaAnno)} where {@code anno} is
     *         the annotation mirror at {@code element}, and {@code metaAnno} is
     *         the annotation mirror used to annotate {@code anno}.
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
            List<? extends AnnotationMirror> annotationsOnAnnotation = annotation
                    .getAnnotationType().asElement().getAnnotationMirrors();
            for (AnnotationMirror a : annotationsOnAnnotation) {
                if (AnnotationUtils.areSameByClass(a, metaAnnotation)) {
                    result.add(Pair.of(annotation, a));
                }
            }
        }
        return result;
    }

    /**
     * This method is a hack to use when a method type argument
     * could not be inferred automatically or if a raw type is used.
     * The only use should be:
     * org.checkerframework.framework.util.AnnotatedTypes.inferTypeArguments(ProcessingEnvironment, AnnotatedTypeFactory, ExpressionTree, ExecutableElement)
     * org.checkerframework.framework.type.AnnotatedTypeFactory.fromTypeTree(Tree)
     */
    public AnnotatedWildcardType getUninferredWildcardType(AnnotatedTypeVariable typeVar) {
        final boolean intersectionType;
        final TypeMirror boundType;
        if (typeVar.getUpperBound().getKind() == TypeKind.INTERSECTION) {
            boundType =
                    typeVar.getUpperBound()
                           .directSuperTypes()
                           .get(0)
                           .getUnderlyingType();
            intersectionType = true;
        } else {
            boundType = typeVar.getUnderlyingType().getUpperBound();
            intersectionType = false;
        }

        WildcardType wc = types.getWildcardType(boundType, null);
        AnnotatedWildcardType wctype = (AnnotatedWildcardType) AnnotatedTypeMirror.createType(wc, this, false);
        if (!intersectionType) {
            wctype.setExtendsBound(typeVar.getUpperBound().deepCopy());
        } else {
            //TODO: This probably doesn't work if the type has a type argument
            wctype.getExtendsBound().addAnnotations(typeVar.getUpperBound().getAnnotations());
        }
        wctype.setSuperBound(typeVar.getLowerBound().deepCopy());
        wctype.addAnnotations(typeVar.getAnnotations());
        wctype.setTypeArgHack();
        return wctype;
    }

    public Pair<AnnotatedDeclaredType, AnnotatedExecutableType> getFnInterfaceFromTree(MemberReferenceTree tree) {
        return getFnInterfaceFromTree((Tree)tree);
    }
    public Pair<AnnotatedDeclaredType, AnnotatedExecutableType> getFnInterfaceFromTree(LambdaExpressionTree tree) {
        return getFnInterfaceFromTree((Tree)tree);
    }

    /**
     * Find the declared type of the functional interface and the executable type for its method for a given
     * MemberReferenceTree or LambdaExpressionTree.
     *
     * @param tree the MemberReferenceTree or LambdaExpressionTree
     * @return the declared type of the functional interface and the executable type
     */
    private Pair<AnnotatedDeclaredType, AnnotatedExecutableType> getFnInterfaceFromTree(Tree tree) {

        Context ctx = ((JavacProcessingEnvironment) getProcessingEnv()).getContext();
        com.sun.tools.javac.code.Types javacTypes = com.sun.tools.javac.code.Types.instance(ctx);

        // ========= Overridden Type =========
        AnnotatedDeclaredType functionalInterfaceType = getFunctionalInterfaceType(tree, javacTypes);
        makeGroundTargetType(functionalInterfaceType);

        // ========= Overridden Executable =========
        Element fnElement = javacTypes.findDescriptorSymbol(
                ((Type) functionalInterfaceType.getUnderlyingType()).asElement());

        // The method viewed from the declared type
        AnnotatedExecutableType methodExe = (AnnotatedExecutableType)AnnotatedTypes.asMemberOf(
                types, this, functionalInterfaceType, fnElement);

        return Pair.of(functionalInterfaceType, methodExe);
    }

    /**
     * Get the AnnotatedDeclaredType for the FunctionalInterface from assignment context of the method reference
     * which may be a variable assignment, a method call, or a cast.
     *
     * The assignment context is not always correct, so we must search up the AST. It will recursively search
     * for lambdas nested in lambdas.
     *
     * @param lambdaTree the tree of the lambda or method reference.
     * @return the functional interface type
     */
    private AnnotatedDeclaredType getFunctionalInterfaceType(Tree lambdaTree,
            com.sun.tools.javac.code.Types javacTypes) {

        Tree parentTree = TreePath.getPath(this.root, lambdaTree).getParentPath().getLeaf();
        switch (parentTree.getKind()) {
            case TYPE_CAST:
                TypeCastTree cast = (TypeCastTree) parentTree;
                assertFunctionalInterface(javacTypes, (Type) trees.getTypeMirror(getPath(cast.getType())), parentTree, lambdaTree);
                return (AnnotatedDeclaredType)getAnnotatedType(cast.getType());

            case METHOD_INVOCATION:
                MethodInvocationTree method = (MethodInvocationTree) parentTree;
                int index = method.getArguments().indexOf(lambdaTree);
                Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> exe = this.methodFromUse(method);
                AnnotatedTypeMirror param = exe.first.getParameterTypes().get(index);
                assertFunctionalInterface(javacTypes, (Type)param.getUnderlyingType(), parentTree, lambdaTree);
                return (AnnotatedDeclaredType) param;

            case VARIABLE:
                VariableTree varTree = (VariableTree) parentTree;
                assertFunctionalInterface(javacTypes, (Type)InternalUtils.typeOf(varTree), parentTree, lambdaTree);
                return (AnnotatedDeclaredType) getAnnotatedType(varTree.getType());

            case ASSIGNMENT:
                AssignmentTree assignmentTree = (AssignmentTree) parentTree;
                assertFunctionalInterface(javacTypes, (Type)InternalUtils.typeOf(assignmentTree), parentTree, lambdaTree);
                return (AnnotatedDeclaredType) getAnnotatedType(assignmentTree.getVariable());

            case RETURN:
                Tree enclosing = TreeUtils.enclosingOfKind(TreePath.getPath(this.root, parentTree),
                        new HashSet<>(Arrays.asList(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION)));

                if (enclosing.getKind() == Tree.Kind.METHOD) {
                    MethodTree enclosingMethod = (MethodTree) enclosing;
                    return (AnnotatedDeclaredType) getAnnotatedType(enclosingMethod.getReturnType());
                } else {
                    LambdaExpressionTree enclosingLambda = (LambdaExpressionTree) enclosing;
                    Pair<AnnotatedDeclaredType, AnnotatedExecutableType> result = getFnInterfaceFromTree(enclosingLambda);
                    AnnotatedExecutableType methodExe = result.second;
                    return (AnnotatedDeclaredType) methodExe.getReturnType();
                }
            case LAMBDA_EXPRESSION:
                LambdaExpressionTree enclosingLambda = (LambdaExpressionTree) parentTree;
                Pair<AnnotatedDeclaredType, AnnotatedExecutableType> result = getFnInterfaceFromTree(enclosingLambda);
                AnnotatedExecutableType methodExe = result.second;
                return (AnnotatedDeclaredType) methodExe.getReturnType();

            default:
                ErrorReporter.errorAbort("Could not find functional interface from assignment context. " +
                        "Unexpected tree type: " + parentTree.getKind() +
                        " For lambda tree: " + lambdaTree);
                return null;
        }
    }

    private void assertFunctionalInterface(com.sun.tools.javac.code.Types javacTypes,
            Type type, Tree contextTree, Tree lambdaTree) {

        if (!javacTypes.isFunctionalInterface(type)) {
            ErrorReporter.errorAbort(String.format(
                    "Expected the type of %s tree in assignment context to be a functional interface. " +
                    "Found type: %s for tree: %s in lambda tree: %s",
                    contextTree.getKind(), type, contextTree, lambdaTree));
        }
    }

    /**
     * Create the ground target type of the functional interface.
     *
     * Basically, it replaces the wildcards with their bounds
     * doing a capture conversion like glb for extends bounds.
     *
     * @see "JLS 9.9"
     * @param overriddenType the functional interface type
     */
    private void makeGroundTargetType(AnnotatedDeclaredType overriddenType) {
        if (overriddenType.getTypeArguments().size() > 0) {
            List<AnnotatedTypeParameterBounds> bounds = this.typeVariablesFromUse(overriddenType, (TypeElement)overriddenType.getUnderlyingType().asElement());
            List<AnnotatedTypeMirror> newTypeArguments = new ArrayList<>(overriddenType.getTypeArguments());
            for (int i = 0 ; i < overriddenType.getTypeArguments().size() ; i ++) {
                AnnotatedTypeMirror argType = overriddenType.getTypeArguments().get(i);
                if (argType.getKind() == TypeKind.WILDCARD) {
                    AnnotatedWildcardType wildcardType = (AnnotatedWildcardType) argType;

                    final TypeMirror wilcardUbType = wildcardType.getExtendsBound().getUnderlyingType();
                    final TypeMirror typeParamUbType =  bounds.get(i).getUpperBound().getUnderlyingType();
                    if (isExtendsWildcard(wildcardType)) {
                        TypeMirror glbType =
                            InternalUtils.greatestLowerBound(this.checker.getProcessingEnvironment(),
                                                             typeParamUbType, wilcardUbType);

                        //checkTypeArgs now enforces that wildcard annotation bounds MUST be within
                        //the bounds of the type parameter.  Therefore, the wildcard's upper bound
                        //should ALWAYS be more specific than the upper bound of the type parameter
                        //That said, the Java type does NOT have to be.
                        //Add the annotations from the wildcard to the lub type.
                        final AnnotatedTypeMirror newArg;
                        if (types.isSameType(wilcardUbType, glbType)) {
                            newArg = wildcardType.getExtendsBound().deepCopy();

                        } else {
                            newArg = this.toAnnotatedType(glbType, false);
                            newArg.replaceAnnotations(wildcardType.getExtendsBound().getAnnotations());
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

    /** Accessor for the element utilities.
     */
    public Elements getElementUtils() {
        return this.elements;
    }

    /** Accessor for the tree utilities.
     */
    public Trees getTreeUtils() {
        return this.trees;
    }

    /** Accessor for the processing environment.
     */
    public ProcessingEnvironment getProcessingEnv() {
        return this.processingEnv;
    }

    /** Accessor for the {@link CFContext}.
     */
    public CFContext getContext() {
        return checker;
    }
}
