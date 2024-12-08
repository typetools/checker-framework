package org.checkerframework.framework.type;

// The imports from com.sun are all @jdk.Exported and therefore somewhat safe to use.
// Try to avoid using non-@jdk.Exported classes.

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
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
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Options;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import org.checkerframework.afu.scenelib.el.AMethod;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.reflection.DefaultReflectionResolver;
import org.checkerframework.common.reflection.MethodValAnnotatedTypeFactory;
import org.checkerframework.common.reflection.MethodValChecker;
import org.checkerframework.common.reflection.ReflectionResolver;
import org.checkerframework.common.reflection.qual.MethodVal;
import org.checkerframework.common.wholeprograminference.WholeProgramInference;
import org.checkerframework.common.wholeprograminference.WholeProgramInferenceImplementation;
import org.checkerframework.common.wholeprograminference.WholeProgramInferenceJavaParserStorage;
import org.checkerframework.common.wholeprograminference.WholeProgramInferenceScenesStorage;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.FieldInvariant;
import org.checkerframework.framework.qual.FromStubFile;
import org.checkerframework.framework.qual.HasQualifierParameter;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.NoQualifierParameter;
import org.checkerframework.framework.qual.RequiresQualifier;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.stub.AnnotationFileElementTypes;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeCombiner;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.AnnotatedTypes.TypeArguments;
import org.checkerframework.framework.util.CheckerMain;
import org.checkerframework.framework.util.FieldInvariants;
import org.checkerframework.framework.util.TreePathCacher;
import org.checkerframework.framework.util.TypeInformationPresenter;
import org.checkerframework.framework.util.typeinference8.DefaultTypeArgumentInference;
import org.checkerframework.framework.util.typeinference8.TypeArgumentInference;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationFormatter;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.DefaultAnnotationFormatter;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.SystemUtil;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.TypeKindUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.javacutil.UserError;
import org.checkerframework.javacutil.trees.DetachedVarSymbol;
import org.plumelib.util.CollectionsPlume;
import org.plumelib.util.IPair;
import org.plumelib.util.ImmutableTypes;
import org.plumelib.util.StringsPlume;
import org.plumelib.util.SystemPlume;

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

  /** Whether to output verbose, low-level debugging messages about {@link #getAnnotatedType}. */
  private static final boolean debugGat = false;

  /** Whether to print verbose debugging messages about stub files. */
  private final boolean debugStubParser;

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

  /**
   * A TreePath to the current tree that an external "visitor" is visiting. The visitor is either a
   * subclass of {@link BaseTypeVisitor} or {@link
   * org.checkerframework.framework.flow.CFAbstractTransfer}.
   */
  private @Nullable TreePath visitorTreePath;

  // These variables cannot be static because they depend on the ProcessingEnvironment.
  /** The AnnotatedFor.value argument/element. */
  protected final ExecutableElement annotatedForValueElement;

  /** The EnsuresQualifier.expression field/element. */
  protected final ExecutableElement ensuresQualifierExpressionElement;

  /** The EnsuresQualifier.List.value field/element. */
  protected final ExecutableElement ensuresQualifierListValueElement;

  /** The EnsuresQualifierIf.expression field/element. */
  protected final ExecutableElement ensuresQualifierIfExpressionElement;

  /** The EnsuresQualifierIf.result argument/element. */
  protected final ExecutableElement ensuresQualifierIfResultElement;

  /** The EnsuresQualifierIf.List.value field/element. */
  protected final ExecutableElement ensuresQualifierIfListValueElement;

  /** The FieldInvariant.field argument/element. */
  protected final ExecutableElement fieldInvariantFieldElement;

  /** The FieldInvariant.qualifier argument/element. */
  protected final ExecutableElement fieldInvariantQualifierElement;

  /** The HasQualifierParameter.value field/element. */
  protected final ExecutableElement hasQualifierParameterValueElement;

  /** The MethodVal.className argument/element. */
  public final ExecutableElement methodValClassNameElement;

  /** The MethodVal.methodName argument/element. */
  public final ExecutableElement methodValMethodNameElement;

  /** The MethodVal.params argument/element. */
  public final ExecutableElement methodValParamsElement;

  /** The NoQualifierParameter.value field/element. */
  protected final ExecutableElement noQualifierParameterValueElement;

  /** The RequiresQualifier.expression field/element. */
  protected final ExecutableElement requiresQualifierExpressionElement;

  /** The RequiresQualifier.List.value field/element. */
  protected final ExecutableElement requiresQualifierListValueElement;

  /** The RequiresQualifier type. */
  protected final TypeMirror requiresQualifierTM;

  /** The RequiresQualifier.List type. */
  protected final TypeMirror requiresQualifierListTM;

  /** The EnsuresQualifier type. */
  protected final TypeMirror ensuresQualifierTM;

  /** The EnsuresQualifier.List type. */
  protected final TypeMirror ensuresQualifierListTM;

  /** The EnsuresQualifierIf type. */
  protected final TypeMirror ensuresQualifierIfTM;

  /** The EnsuresQualifierIf.List type. */
  protected final TypeMirror ensuresQualifierIfListTM;

  // ===== postInit()-initialized fields ====
  // Note: qualHierarchy and typeHierarchy are both initialized in postInit().
  // This means, they cannot be final and cannot be referred to in any subclass
  // constructor or method until after postInit is called

  /** Represent the annotation relations. */
  // This field cannot be final because it is set in `postInit()`.
  protected QualifierHierarchy qualHierarchy;

  /** Represent the type relations. */
  // This field cannot be final because it is set in `postInit()`.
  protected TypeHierarchy typeHierarchy;

  /** Performs whole-program inference. If null, whole-program inference is disabled. */
  private final @Nullable WholeProgramInference wholeProgramInference;

  /**
   * This formatter is used for converting AnnotatedTypeMirrors to Strings. This formatter will be
   * used by all AnnotatedTypeMirrors created by this factory in their toString methods.
   */
  protected final AnnotatedTypeFormatter typeFormatter;

  /**
   * Annotation formatter is used to format AnnotationMirrors. It is primarily used by SourceChecker
   * when generating error messages.
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
   * Caches the supported type qualifier classes. Call {@link #getSupportedTypeQualifiers()} instead
   * of using this field directly, as it may not have been initialized.
   */
  private @MonotonicNonNull Set<Class<? extends Annotation>> supportedQuals = null;

  /**
   * Caches the fully-qualified names of the classes in {@link #supportedQuals}. Call {@link
   * #getSupportedTypeQualifierNames()} instead of using this field directly, as it may not have
   * been initialized.
   */
  private @MonotonicNonNull Set<@CanonicalName String> supportedQualNames = null;

  /** Parses stub files and stores annotations on public elements from stub files. */
  public final AnnotationFileElementTypes stubTypes;

  /** Parses ajava files and stores annotations on public elements from ajava files. */
  public final AnnotationFileElementTypes ajavaTypes;

  /**
   * If type checking a Java file, stores annotations read from an ajava file for that class if one
   * exists. Unlike {@link #ajavaTypes}, which only stores annotations on public elements, this
   * stores annotations on all element locations such as in anonymous class bodies.
   */
  protected @Nullable AnnotationFileElementTypes currentFileAjavaTypes;

  /**
   * A cache used to store elements whose declaration annotations have already been stored by
   * calling the method {@link #getDeclAnnotations(Element)}.
   */
  private final Map<Element, AnnotationMirrorSet> cacheDeclAnnos;

  /**
   * A set containing declaration annotations that should be inherited. A declaration annotation
   * will be inherited if it is in this set, or if it has the meta-annotation @InheritedAnnotation.
   */
  private final AnnotationMirrorSet inheritedAnnotations = new AnnotationMirrorSet();

  /** The checker to use for option handling and resource management. */
  protected final BaseTypeChecker checker;

  /** Map keys are canonical names of aliased annotations. */
  private final Map<@FullyQualifiedName String, Alias> aliases = new HashMap<>();

  /**
   * Scans all parts of the {@link AnnotatedTypeMirror} so that all of its fields are initialized.
   */
  private final SimpleAnnotatedTypeScanner<Void, Void> atmInitializer =
      new SimpleAnnotatedTypeScanner<>((type1, q) -> null);

  /**
   * True if all methods should be assumed to be @SideEffectFree, for the purposes of
   * org.checkerframework.dataflow analysis.
   */
  private final boolean assumeSideEffectFree;

  /**
   * True if all methods should be assumed to be @Deterministic, for the purposes of
   * org.checkerframework.dataflow analysis.
   */
  private final boolean assumeDeterministic;

  /**
   * True if all getter methods should be assumed to be @Pure, for the purposes of
   * org.checkerframework.dataflow analysis.
   */
  private final boolean assumePureGetters;

  /** True if -AmergeStubsWithSource was provided on the command line. */
  private final boolean mergeStubsWithSource;

  /**
   * Initializes all fields of {@code type}.
   *
   * @param type annotated type mirror
   */
  public void initializeAtm(AnnotatedTypeMirror type) {
    atmInitializer.visit(type);
  }

  /**
   * Information about one annotation alias.
   *
   * <p>The information is either an AnotationMirror that can be used directly, or information for a
   * builder (name and fields not to copy); see checkRep.
   */
  private static class Alias {
    /** The canonical annotation (or null if copyElements == true). */
    final AnnotationMirror canonical;

    /** Whether elements should be copied over when translating to the canonical annotation. */
    final boolean copyElements;

    /** The canonical annotation name (or null if copyElements == false). */
    final @CanonicalName String canonicalName;

    /** Which elements should not be copied over (or null if copyElements == false). */
    final String[] ignorableElements;

    /**
     * Create an Alias with the given components.
     *
     * @param aliasName the alias name; only used for debugging
     * @param canonical the canonical annotation
     * @param copyElements whether elements should be copied over when translating to the canonical
     *     annotation
     * @param canonicalName the canonical annotation name (or null if copyElements == false)
     * @param ignorableElements elements that should not be copied over
     */
    Alias(
        String aliasName,
        AnnotationMirror canonical,
        boolean copyElements,
        @Nullable @CanonicalName String canonicalName,
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
              "Bad Alias for %s: [canonical=%s] copyElements=%s canonicalName=%s"
                  + " ignorableElements=%s",
              aliasName, canonical, copyElements, canonicalName, ignorableElements);
        }
      } else {
        if (!(canonical != null && canonicalName == null && ignorableElements == null)) {
          throw new BugInCF(
              "Bad Alias for %s: canonical=%s copyElements=%s [canonicalName=%s"
                  + " ignorableElements=%s]",
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
          Class<? extends Annotation>, IPair<AnnotationMirror, Set<Class<? extends Annotation>>>>
      declAliases = new HashMap<>();

  /** Unique ID counter; for debugging purposes. */
  private static int uidCounter = 0;

  /** Unique ID of the current object; for debugging purposes. */
  public final int uid;

  /**
   * Object that is used to resolve reflective method calls, if reflection resolution is turned on.
   */
  protected ReflectionResolver reflectionResolver;

  /** This loads type annotation classes via reflective lookup. */
  protected AnnotationClassLoader loader;

  /**
   * Which whole-program inference output format to use, if doing whole-program inference. This
   * variable would be final, but it is not set unless WPI is enabled.
   */
  public WholeProgramInference.OutputFormat wpiOutputFormat;

  /**
   * Should results be cached? This means that ATM.deepCopy() will be called. ATM.deepCopy() used to
   * (and perhaps still does) side effect the ATM being copied. So setting this to false is not
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

  /** Mapping from CFG-generated trees to their enclosing elements. */
  protected final Map<Tree, Element> artificialTreeToEnclosingElementMap;

  /** Whether to ignore type arguments from raw types. */
  public final boolean ignoreRawTypeArguments;

  /** The Object.getClass method. */
  protected final ExecutableElement objectGetClass;

  /** Size of the annotationClassNames cache. */
  private static final int ANNOTATION_CACHE_SIZE = 500;

  /** Maps classes representing AnnotationMirrors to their canonical names. */
  private final Map<Class<? extends Annotation>, @CanonicalName String> annotationClassNames;

  /** An annotated type of the declaration of {@link Iterable} without any annotations. */
  private AnnotatedDeclaredType iterableDeclType;

  /**
   * If the option "lspTypeInfo" is defined, this presenter will report the type information of
   * every type-checked class. This information can be visualized by an editor/IDE that supports
   * LSP.
   */
  protected final TypeInformationPresenter typeInformationPresenter;

  /**
   * Constructs a factory from the given checker.
   *
   * <p>A subclass must call postInit at the end of its constructor. postInit must be the last call
   * in the constructor or else types from stub files may not be created as expected.
   *
   * @param checker the {@link SourceChecker} to which this factory belongs
   */
  public AnnotatedTypeFactory(BaseTypeChecker checker) {
    uid = ++uidCounter;
    this.processingEnv = checker.getProcessingEnvironment();
    this.checker = checker;
    this.assumeSideEffectFree =
        checker.hasOption("assumeSideEffectFree") || checker.hasOption("assumePure");
    this.assumeDeterministic =
        checker.hasOption("assumeDeterministic") || checker.hasOption("assumePure");
    this.assumePureGetters = checker.hasOption("assumePureGetters");

    this.trees = Trees.instance(processingEnv);
    this.elements = processingEnv.getElementUtils();
    this.types = processingEnv.getTypeUtils();

    this.stubTypes = new AnnotationFileElementTypes(this);
    this.ajavaTypes = new AnnotationFileElementTypes(this);
    this.currentFileAjavaTypes = null;

    this.cacheDeclAnnos = new HashMap<>();

    this.artificialTreeToEnclosingElementMap = new HashMap<>();
    // get the shared instance from the checker
    this.treePathCache = checker.getTreePathCacher();

    this.shouldCache = !checker.hasOption("atfDoNotCache");
    if (shouldCache) {
      int cacheSize = getCacheSize();
      this.classAndMethodTreeCache = CollectionsPlume.createLruCache(cacheSize);
      this.fromExpressionTreeCache = CollectionsPlume.createLruCache(cacheSize);
      this.fromMemberTreeCache = CollectionsPlume.createLruCache(cacheSize);
      this.fromTypeTreeCache = CollectionsPlume.createLruCache(cacheSize);
      this.elementCache = CollectionsPlume.createLruCache(cacheSize);
      this.elementToTreeCache = CollectionsPlume.createLruCache(cacheSize);
      this.annotationClassNames =
          Collections.synchronizedMap(CollectionsPlume.createLruCache(ANNOTATION_CACHE_SIZE));
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
    this.typeInformationPresenter = createTypeInformationPresenter();

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
        case "ajava":
          wpiOutputFormat = WholeProgramInference.OutputFormat.AJAVA;
          break;
        default:
          throw new UserError(
              "Bad argument -Ainfer="
                  + inferArg
                  + " should be one of: -Ainfer=jaifs, -Ainfer=stubs, -Ainfer=ajava");
      }
      boolean showWpiFailedInferences = checker.hasOption("showWpiFailedInferences");
      boolean inferOutputOriginal = checker.hasOption("inferOutputOriginal");
      if (inferOutputOriginal && wpiOutputFormat != WholeProgramInference.OutputFormat.AJAVA) {
        checker.message(
            Diagnostic.Kind.WARNING,
            "-AinferOutputOriginal only works with -Ainfer=ajava, so it is being ignored.");
      }
      if (wpiOutputFormat == WholeProgramInference.OutputFormat.AJAVA) {
        wholeProgramInference =
            new WholeProgramInferenceImplementation<AnnotatedTypeMirror>(
                this,
                new WholeProgramInferenceJavaParserStorage(this, inferOutputOriginal),
                showWpiFailedInferences);
      } else {
        wholeProgramInference =
            new WholeProgramInferenceImplementation<ATypeElement>(
                this, new WholeProgramInferenceScenesStorage(this), showWpiFailedInferences);
      }
      if (!checker.hasOption("warns")) {
        // Without -Awarns, the inference output may be incomplete, because javac halts
        // after issuing an error.
        checker.message(Diagnostic.Kind.ERROR, "Do not supply -Ainfer without -Awarns");
      }
    } else {
      wholeProgramInference = null;
    }
    ignoreRawTypeArguments = checker.getBooleanOption("ignoreRawTypeArguments", true);

    objectGetClass = TreeUtils.getMethod("java.lang.Object", "getClass", 0, processingEnv);

    this.debugStubParser = checker.hasOption("stubDebug");

    annotatedForValueElement = TreeUtils.getMethod(AnnotatedFor.class, "value", 0, processingEnv);
    ensuresQualifierExpressionElement =
        TreeUtils.getMethod(EnsuresQualifier.class, "expression", 0, processingEnv);
    ensuresQualifierListValueElement =
        TreeUtils.getMethod(EnsuresQualifier.List.class, "value", 0, processingEnv);
    ensuresQualifierIfExpressionElement =
        TreeUtils.getMethod(EnsuresQualifierIf.class, "expression", 0, processingEnv);
    ensuresQualifierIfResultElement =
        TreeUtils.getMethod(EnsuresQualifierIf.class, "result", 0, processingEnv);
    ensuresQualifierIfListValueElement =
        TreeUtils.getMethod(EnsuresQualifierIf.List.class, "value", 0, processingEnv);
    fieldInvariantFieldElement =
        TreeUtils.getMethod(FieldInvariant.class, "field", 0, processingEnv);
    fieldInvariantQualifierElement =
        TreeUtils.getMethod(FieldInvariant.class, "qualifier", 0, processingEnv);
    hasQualifierParameterValueElement =
        TreeUtils.getMethod(HasQualifierParameter.class, "value", 0, processingEnv);
    methodValClassNameElement = TreeUtils.getMethod(MethodVal.class, "className", 0, processingEnv);
    methodValMethodNameElement =
        TreeUtils.getMethod(MethodVal.class, "methodName", 0, processingEnv);
    methodValParamsElement = TreeUtils.getMethod(MethodVal.class, "params", 0, processingEnv);
    noQualifierParameterValueElement =
        TreeUtils.getMethod(NoQualifierParameter.class, "value", 0, processingEnv);
    requiresQualifierExpressionElement =
        TreeUtils.getMethod(RequiresQualifier.class, "expression", 0, processingEnv);
    requiresQualifierListValueElement =
        TreeUtils.getMethod(RequiresQualifier.List.class, "value", 0, processingEnv);

    requiresQualifierTM =
        ElementUtils.getTypeElement(processingEnv, RequiresQualifier.class).asType();
    requiresQualifierListTM =
        ElementUtils.getTypeElement(processingEnv, RequiresQualifier.List.class).asType();
    ensuresQualifierTM =
        ElementUtils.getTypeElement(processingEnv, EnsuresQualifier.class).asType();
    ensuresQualifierListTM =
        ElementUtils.getTypeElement(processingEnv, EnsuresQualifier.List.class).asType();
    ensuresQualifierIfTM =
        ElementUtils.getTypeElement(processingEnv, EnsuresQualifierIf.class).asType();
    ensuresQualifierIfListTM =
        ElementUtils.getTypeElement(processingEnv, EnsuresQualifierIf.List.class).asType();

    mergeStubsWithSource = checker.hasOption("mergeStubsWithSource");
  }

  /**
   * Requires that supportedQuals is non-null and non-empty and each element is a type qualifier.
   * That is, no element has a {@code @Target} meta-annotation that contains something besides
   * TYPE_USE or TYPE_PARAMETER. (@Target({}) is allowed.) @
   *
   * @throws BugInCF If supportedQuals is empty or contaions a non-type qualifier
   */
  private void checkSupportedQualsAreTypeQuals() {
    if (supportedQuals == null || supportedQuals.isEmpty()) {
      throw new TypeSystemError("Found no supported qualifiers.");
    }
    for (Class<? extends Annotation> annotationClass : supportedQuals) {
      // Check @Target values
      ElementType[] targetValues = annotationClass.getAnnotation(Target.class).value();
      List<ElementType> badTargetValues = new ArrayList<>(0);
      for (ElementType element : targetValues) {
        if (!(element == ElementType.TYPE_USE || element == ElementType.TYPE_PARAMETER)) {
          // if there's an ElementType with an enumerated value of something other
          // than TYPE_USE or TYPE_PARAMETER then it isn't a valid qualifier
          badTargetValues.add(element);
        }
      }
      if (!badTargetValues.isEmpty()) {
        String msg =
            "The @Target meta-annotation on type qualifier "
                + annotationClass.toString()
                + " must not contain "
                + StringsPlume.conjunction("or", badTargetValues)
                + ".";
        throw new TypeSystemError(msg);
      }
    }
  }

  /**
   * This method is called only when {@code -Ainfer} is passed as an option. It checks if another
   * option that should not occur simultaneously with the whole-program inference is also passed as
   * argument, and aborts the process if that is the case. For example, the whole-program inference
   * process was not designed to work with conservative defaults.
   *
   * <p>Subclasses may override this method to add more options.
   */
  protected void checkInvalidOptionsInferSignatures() {
    // See Issue 683
    // https://github.com/typetools/checker-framework/issues/683
    if (checker.useConservativeDefault("source") || checker.useConservativeDefault("bytecode")) {
      throw new UserError(
          "The option -Ainfer=... cannot be used together with conservative defaults.");
    }
  }

  /**
   * Actions that logically belong in the constructor, but need to run after the subclass
   * constructor has completed. In particular, {@link AnnotationFileElementTypes#parseStubFiles()}
   * may try to do type resolution with this AnnotatedTypeFactory.
   */
  protected void postInit(
      @UnderInitialization(AnnotatedTypeFactory.class) AnnotatedTypeFactory this) {
    this.qualHierarchy = createQualifierHierarchy();
    if (qualHierarchy == null) {
      throw new TypeSystemError(
          "AnnotatedTypeFactory with null qualifier hierarchy not supported.");
    } else if (!qualHierarchy.isValid()) {
      throw new TypeSystemError(
          "AnnotatedTypeFactory: invalid qualifier hierarchy: %s %s ",
          qualHierarchy.getClass(), qualHierarchy);
    }
    this.typeHierarchy = createTypeHierarchy();
    this.typeVarSubstitutor = createTypeVariableSubstitutor();
    this.typeArgumentInference = createTypeArgumentInference();
    this.qualifierUpperBounds = createQualifierUpperBounds();

    // TODO: is this the best location for declaring this alias?
    addAliasedDeclAnnotation(
        org.jmlspecs.annotation.Pure.class,
        org.checkerframework.dataflow.qual.Pure.class,
        AnnotationBuilder.fromClass(elements, org.checkerframework.dataflow.qual.Pure.class));

    // Accommodate the inability to write @InheritedAnnotation on these annotations.
    addInheritedAnnotation(
        AnnotationBuilder.fromClass(elements, org.checkerframework.dataflow.qual.Pure.class));
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
      this.parseAnnotationFiles();
    }
    TypeMirror iterableTypeMirror =
        ElementUtils.getTypeElement(processingEnv, Iterable.class).asType();
    this.iterableDeclType =
        (AnnotatedDeclaredType) AnnotatedTypeMirror.createType(iterableTypeMirror, this, true);
  }

  /**
   * Returns the checker associated with this factory.
   *
   * @return the checker associated with this factory
   */
  public BaseTypeChecker getChecker() {
    return checker;
  }

  /**
   * Returns the names of the annotation processors that are being run.
   *
   * @return the names of the annotation processors that are being run
   */
  @SuppressWarnings("JdkObsolete") // ClassLoader.getResources returns an Enumeration
  public List<String> getCheckerNames() {
    com.sun.tools.javac.util.Context context =
        ((JavacProcessingEnvironment) processingEnv).getContext();
    String processorArg = Options.instance(context).get("-processor");
    if (processorArg != null) {
      return SystemUtil.commaSplitter.splitToList(processorArg);
    }
    try {
      String filename = "META-INF/services/javax.annotation.processing.Processor";
      List<String> result = new ArrayList<>();
      Enumeration<URL> urls = getClass().getClassLoader().getResources(filename);
      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();
        try (BufferedReader in =
            new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
          result.addAll(in.lines().collect(Collectors.toList()));
        }
      }
      return result;
    } catch (IOException e) {
      throw new BugInCF(e);
    }
  }

  /**
   * Creates {@link QualifierUpperBounds} for this type factory.
   *
   * @return a new {@link QualifierUpperBounds} for this type factory
   */
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
  public @Nullable WholeProgramInference getWholeProgramInference() {
    return wholeProgramInference;
  }

  /** Initialize reflection resolution. */
  protected void initializeReflectionResolution() {
    if (checker.shouldResolveReflection()) {
      boolean debug = "debug".equals(checker.getOption("resolveReflection"));

      MethodValChecker methodValChecker = checker.getSubchecker(MethodValChecker.class);
      assert methodValChecker != null
          : "AnnotatedTypeFactory: reflection resolution was requested,"
              + " but MethodValChecker isn't a subchecker.";
      MethodValAnnotatedTypeFactory methodValATF =
          (MethodValAnnotatedTypeFactory) methodValChecker.getAnnotationProvider();

      reflectionResolver = new DefaultReflectionResolver(checker, methodValATF, debug);
    }
  }

  /**
   * Set the CompilationUnitTree that should be used.
   *
   * @param newRoot the new compilation unit to use
   */
  public void setRoot(@Nullable CompilationUnitTree newRoot) {
    if (newRoot != null && wholeProgramInference != null) {
      for (Tree typeDecl : newRoot.getTypeDecls()) {
        if (typeDecl.getKind() == Tree.Kind.CLASS) {
          ClassTree classTree = (ClassTree) typeDecl;
          wholeProgramInference.preprocessClassTree(classTree);
        }
      }
    }

    this.root = newRoot;
    // Do not clear here. Only the primary checker should clear this cache.
    // treePathCache.clear();

    // setRoot in a GenericAnnotatedTypeFactory will clear this;
    // if this isn't a GenericATF, then it must clear it itself.
    if (!(this instanceof GenericAnnotatedTypeFactory)) {
      artificialTreeToEnclosingElementMap.clear();
    }

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

    if (root != null && checker.hasOption("ajava")) {
      // Search for an ajava file with annotations for the current source file and the current
      // checker. It will be in a directory specified by the "ajava" option in a subdirectory
      // corresponding to this file's package. For example, a file in package a.b would be in
      // a subdirectory a/b. The filename is ClassName-checker.qualified.name.ajava. If such a
      // file exists, read its detailed annotation data, including annotations on private
      // elements.

      String packagePrefix =
          root.getPackageName() != null
              ? TreeUtils.nameExpressionToString(root.getPackageName()) + "."
              : "";

      // The method getName() returns a path.
      String rootFile = root.getSourceFile().getName();
      String className = rootFile;
      // Extract the basename.
      int lastSeparator = className.lastIndexOf(File.separator);
      if (lastSeparator != -1) {
        className = className.substring(lastSeparator + 1);
      }
      // Drop the ".java" extension.
      if (className.endsWith(".java")) {
        className = className.substring(0, className.length() - ".java".length());
      }

      String qualifiedName = packagePrefix + className;

      // If the set candidateAjavaFiles has exactly one element after the loop, a specific
      // .ajava file was supplied, with no ambiguity, and can be parsed. For an explanation,
      // see the comment below about possible ambiguity.
      Set<String> candidateAjavaFiles = new HashSet<>(1);
      // All .ajava files for this class + checker combo end in this string.
      String ajavaEnding =
          qualifiedName.replaceAll("\\.", "/")
              + "-"
              + checker.getClass().getCanonicalName()
              + ".ajava";
      for (String ajavaLocation : checker.getStringsOption("ajava", File.pathSeparator)) {
        // ajavaLocation might either be (1) a directory, or (2) the name of a specific
        // ajava file. This code must handle both possible cases.
        // Case (1): ajavaPath is a directory
        String ajavaPath = ajavaLocation + File.separator + ajavaEnding;
        File ajavaFileInDir = new File(ajavaPath);
        if (ajavaFileInDir.exists()) {
          // There is a candidate ajava file in one of the root directories.
          candidateAjavaFiles.add(ajavaPath);
        } else {
          // Check case (2): ajavaPath might be a specific .ajava file. The tricky thing
          // about this is that the "root" is not known, so the correct .ajava file might
          // be ambiguous. Consider the following: there are two ajava files:
          // ~/foo/foo/Bar-checker.ajava and ~/baz/foo/Bar-checker.ajava. Which is the
          // correct one for class foo.Bar? It depends on whether there is a foo.foo.Bar
          // or a baz.foo.Bar elsewhere in the project. For that reason, parsing using a
          // specific file is done at the **end** of the loop, and if there is more than
          // one match no file is parsed for this class and a warning is issued instead.
          // The user can disambiguate by supplying a root directory, instead of specific
          // files.
          if (ajavaLocation.endsWith(File.separator + ajavaEnding)) {
            // This is a candidate ajava file. If it is the only candidate, then it
            // might be unambiguous. If not, issue a warning.
            candidateAjavaFiles.add(ajavaLocation);
          }
        }
      }
      if (candidateAjavaFiles.size() == 1) {
        currentFileAjavaTypes = new AnnotationFileElementTypes(this);
        String ajavaPath = candidateAjavaFiles.toArray(new String[0])[0];
        try {
          currentFileAjavaTypes.parseAjavaFileWithTree(ajavaPath, root);
        } catch (Throwable e) {
          throw new Error(
              "Problem while parsing " + ajavaPath + " that corresponds to " + rootFile, e);
        }
      } else if (candidateAjavaFiles.size() > 1) {
        checker.reportWarning(root, "ambiguous.ajava", String.join(", ", candidateAjavaFiles));
      }
    } else {
      currentFileAjavaTypes = null;
    }
  }

  @SideEffectFree
  @Override
  public String toString() {
    return getClass().getSimpleName() + "#" + uid;
  }

  /**
   * Returns the {@link QualifierHierarchy} to be used by this checker.
   *
   * <p>The implementation builds the type qualifier hierarchy for the {@link
   * #getSupportedTypeQualifiers()} using the meta-annotations found in them. The current
   * implementation returns an instance of {@code NoElementQualifierHierarchy}.
   *
   * <p>Subclasses must override this method if their qualifiers have elements; the method must
   * return an implementation of {@link QualifierHierarchy}, such as {@link
   * ElementQualifierHierarchy}.
   *
   * @return a QualifierHierarchy for this type system
   */
  protected QualifierHierarchy createQualifierHierarchy() {
    return new NoElementQualifierHierarchy(
        this.getSupportedTypeQualifiers(),
        elements,
        (GenericAnnotatedTypeFactory<?, ?, ?, ?>) this);
  }

  /**
   * Returns the type qualifier hierarchy graph to be used by this processor.
   *
   * @see #createQualifierHierarchy()
   * @return the {@link QualifierHierarchy} for this checker
   */
  public final QualifierHierarchy getQualifierHierarchy() {
    return qualHierarchy;
  }

  /**
   * Returns true if the given qualifer is one of the top annotations for the qualifer hierarchy.
   *
   * @param qualifier a type qualifier
   * @return true if the given qualifer is one of the top annotations for the qualifer hierarchy
   */
  public final boolean isTop(AnnotationMirror qualifier) {
    return qualHierarchy.isTop(qualifier);
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
        ignoreRawTypeArguments,
        checker.hasOption("invariantArrays"));
  }

  public final TypeHierarchy getTypeHierarchy() {
    return typeHierarchy;
  }

  /** TypeVariableSubstitutor provides a method to replace type parameters with their arguments. */
  protected TypeVariableSubstitutor createTypeVariableSubstitutor() {
    return new TypeVariableSubstitutor();
  }

  public TypeVariableSubstitutor getTypeVarSubstitutor() {
    return typeVarSubstitutor;
  }

  /**
   * Creates the object that infers type arguments.
   *
   * @return the object that infers type arguments
   */
  protected TypeArgumentInference createTypeArgumentInference() {
    return new DefaultTypeArgumentInference();
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
   * <p>Subclasses should not call this method; they should call {@link #getSupportedTypeQualifiers}
   * instead.
   *
   * <p>By default, a checker supports all annotations located in a subdirectory called {@literal
   * qual} that's located in the same directory as the checker. Note that only annotations defined
   * with the {@code @Target({ElementType.TYPE_USE})} meta-annotation (and optionally with the
   * additional value of {@code ElementType.TYPE_PARAMETER}, but no other {@code ElementType}
   * values) are automatically considered as supported annotations.
   *
   * <p>To support a different set of annotations than those in the {@literal qual} subdirectory, or
   * that have other {@code ElementType} values, see examples below.
   *
   * <p>In total, there are 5 ways to indicate annotations that are supported by a checker:
   *
   * <ol>
   *   <li>Only support annotations located in a checker's {@literal qual} directory:
   *       <p>This is the default behavior. Simply place those annotations within the {@literal
   *       qual} directory.
   *   <li>Support annotations located in a checker's {@literal qual} directory and a list of other
   *       annotations:
   *       <p>Place those annotations within the {@literal qual} directory, and override {@link
   *       #createSupportedTypeQualifiers()} by calling {@link #getBundledTypeQualifiers(Class...)}
   *       with a varargs parameter list of the other annotations. Code example:
   *       <pre>
   * {@code @Override protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
   *      return getBundledTypeQualifiers(Regex.class, PartialRegex.class, RegexBottom.class, UnknownRegex.class);
   *  } }
   * </pre>
   *   <li>Supporting only annotations that are explicitly listed: Override {@link
   *       #createSupportedTypeQualifiers()} and return a mutable set of the supported annotations.
   *       Code example:
   *       <pre>
   * {@code @Override protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
   *      return new HashSet<Class<? extends Annotation>>(
   *              Arrays.asList(A.class, B.class));
   *  } }
   * </pre>
   *       The set of qualifiers returned by {@link #createSupportedTypeQualifiers()} must be a
   *       fresh, mutable set. The methods {@link #getBundledTypeQualifiers(Class...)} must return a
   *       fresh, mutable set
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
   *     be added to the returned set. For example, it is used frequently to add Bottom qualifiers.
   * @return a mutable set of the loaded and listed annotation classes
   */
  @SafeVarargs
  protected final Set<Class<? extends Annotation>> getBundledTypeQualifiers(
      Class<? extends Annotation>... explicitlyListedAnnotations) {
    return loadTypeAnnotationsFromQualDir(explicitlyListedAnnotations);
  }

  /**
   * Instantiates the AnnotationClassLoader and loads all annotations contained in the qual
   * directory of a checker via reflection, and has the option to include an explicitly stated list
   * of annotations (eg ones found in a different directory than qual).
   *
   * <p>The annotations that are automatically loaded must have the {@link
   * java.lang.annotation.Target Target} meta-annotation with the value of {@link
   * ElementType#TYPE_USE} (and optionally {@link ElementType#TYPE_PARAMETER}). If it has other
   * {@link ElementType} values, it won't be loaded. Other annotation classes must be explicitly
   * listed even if they are in the same directory as the checker's qual directory.
   *
   * @param explicitlyListedAnnotations a set of explicitly listed annotation classes to be added to
   *     the returned set, for example, it is used frequently to add Bottom qualifiers
   * @return a set of annotation class instances
   */
  @SafeVarargs
  @SuppressWarnings("varargs")
  private final Set<Class<? extends Annotation>> loadTypeAnnotationsFromQualDir(
      Class<? extends Annotation>... explicitlyListedAnnotations) {
    if (loader != null) {
      loader.close();
    }
    loader = createAnnotationClassLoader();

    Set<Class<? extends Annotation>> annotations = loader.getBundledAnnotationClasses();

    // add in all explicitly Listed qualifiers
    if (explicitlyListedAnnotations != null) {
      annotations.addAll(Arrays.asList(explicitlyListedAnnotations));
    }

    return annotations;
  }

  /**
   * Creates the {@link AnnotatedTypeFormatter} used by this type factory and all {@link
   * AnnotatedTypeMirror}s it creates. The {@link AnnotatedTypeFormatter} is used in {@link
   * AnnotatedTypeMirror#toString()} and will affect the error messages printed for checkers that
   * use this type factory.
   *
   * @return the {@link AnnotatedTypeFormatter} to pass to all {@link AnnotatedTypeMirror}s
   */
  protected AnnotatedTypeFormatter createAnnotatedTypeFormatter() {
    boolean printVerboseGenerics = checker.hasOption("printVerboseGenerics");
    return new DefaultAnnotatedTypeFormatter(
        printVerboseGenerics,
        // -AprintVerboseGenerics implies -AprintAllQualifiers
        printVerboseGenerics || checker.hasOption("printAllQualifiers"));
  }

  /**
   * Return the current {@link AnnotatedTypeFormatter}.
   *
   * @return the current {@link AnnotatedTypeFormatter}
   */
  public AnnotatedTypeFormatter getAnnotatedTypeFormatter() {
    return typeFormatter;
  }

  /**
   * Creates the {@link AnnotationFormatter} used by this type factory.
   *
   * @return the {@link AnnotationFormatter} used by this type factory
   */
  protected AnnotationFormatter createAnnotationFormatter() {
    return new DefaultAnnotationFormatter();
  }

  /**
   * Return the current {@link AnnotationFormatter}.
   *
   * @return the current {@link AnnotationFormatter}
   */
  public AnnotationFormatter getAnnotationFormatter() {
    return annotationFormatter;
  }

  /**
   * Creates the {@link TypeInformationPresenter} used in {@link #postProcessClassTree(ClassTree)}
   * to output type information about the current class.
   *
   * @return the {@link TypeInformationPresenter} used by this type factory, or null
   */
  protected @Nullable TypeInformationPresenter createTypeInformationPresenter() {
    // TODO: look into a similar mechanism as for CFG visualization.
    if (checker.hasOption("lspTypeInfo")) {
      return new TypeInformationPresenter(this);
    } else {
      return null;
    }
  }

  /**
   * Returns an immutable set of the classes corresponding to the type qualifiers supported by this
   * checker.
   *
   * <p>Subclasses cannot override this method; they should override {@link
   * #createSupportedTypeQualifiers createSupportedTypeQualifiers} instead.
   *
   * @see #createSupportedTypeQualifiers()
   * @return an immutable set of the supported type qualifiers, or an empty set if no qualifiers are
   *     supported
   */
  public final Set<Class<? extends Annotation>> getSupportedTypeQualifiers() {
    if (this.supportedQuals == null) {
      supportedQuals = createSupportedTypeQualifiers();
      checkSupportedQualsAreTypeQuals();
    }
    return supportedQuals;
  }

  /**
   * Returns an immutable set of the fully qualified names of the type qualifiers supported by this
   * checker.
   *
   * <p>Subclasses cannot override this method; they should override {@link
   * #createSupportedTypeQualifiers createSupportedTypeQualifiers} instead.
   *
   * @see #createSupportedTypeQualifiers()
   * @return an immutable set of the supported type qualifiers, or an empty set if no qualifiers are
   *     supported
   */
  public final Set<@CanonicalName String> getSupportedTypeQualifierNames() {
    if (this.supportedQualNames == null) {
      supportedQualNames = new HashSet<>();
      for (Class<?> clazz : getSupportedTypeQualifiers()) {
        supportedQualNames.add(clazz.getCanonicalName());
      }
      supportedQualNames = Collections.unmodifiableSet(supportedQualNames);
    }
    return supportedQualNames;
  }

  // **********************************************************************
  // Factories for annotated types that account for default qualifiers
  // **********************************************************************

  /**
   * Returns the size for LRU caches. It is either the value supplied via the {@code -AatfCacheSize}
   * option or the default cache size.
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

  /**
   * Returns an AnnotatedTypeMirror representing the annotated type of {@code clazz}.
   *
   * @param clazz a class
   * @return the annotated type of {@code clazz}
   */
  public AnnotatedTypeMirror getAnnotatedType(Class<?> clazz) {
    return getAnnotatedType(elements.getTypeElement(clazz.getCanonicalName()));
  }

  @Override
  public @Nullable AnnotationMirror getAnnotationMirror(
      Tree tree, Class<? extends Annotation> target) {
    if (isSupportedQualifier(target)) {
      AnnotatedTypeMirror atm = getAnnotatedType(tree);
      return atm.getPrimaryAnnotation(target);
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
    logGat("getAnnotatedType(%s)%n", tree);

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
      logGat("getAnnotatedType(%s): fromExpression=>%s%n", tree, type);
    } else {
      throw new BugInCF(
          "AnnotatedTypeFactory.getAnnotatedType: query of annotated type for tree "
              + tree.getKind());
    }

    logGat("getAnnotatedType(%s): before addComputedTypeAnnotations, type=%s%n", tree, type);
    addComputedTypeAnnotations(tree, type);
    if (tree.getKind() == Kind.TYPE_CAST) {
      type = applyCaptureConversion(type);
    }
    logGat("getAnnotatedType(%s): after addComputedTypeAnnotations, type=%s%n", tree, type);

    if (TreeUtils.isClassTree(tree) || tree.getKind() == Tree.Kind.METHOD) {
      // Don't cache VARIABLE
      if (shouldCache) {
        classAndMethodTreeCache.put(tree, type.deepCopy());
      }
    } else {
      // No caching otherwise
    }

    return type;
  }

  /**
   * Called by {@link BaseTypeVisitor#visitClass(ClassTree, Void)} before the classTree is type
   * checked.
   *
   * @param classTree the class on which to perform preprocessing
   */
  public void preProcessClassTree(ClassTree classTree) {}

  /**
   * Called by {@link BaseTypeVisitor#visitClass(ClassTree, Void)} after the ClassTree has been type
   * checked.
   *
   * <p>The default implementation uses this to store the defaulted AnnotatedTypeMirrors and
   * inherited declaration annotations back into the corresponding Elements. Subclasses might want
   * to override this method if storing defaulted types is not desirable.
   */
  public void postProcessClassTree(ClassTree tree) {
    TypesIntoElements.store(processingEnv, this, tree);
    DeclarationsIntoElements.store(processingEnv, this, tree);

    if (typeInformationPresenter != null) {
      typeInformationPresenter.process(tree);
    }

    if (wholeProgramInference != null) {
      // Write out the results of whole-program inference, just once for each class.  As soon
      // as any class is finished processing, all modified scenes are written to files, in
      // case this was the last class to be processed.  Post-processing of subsequent classes
      // might result in re-writing some of the scenes if new information has been written to
      // them.
      wholeProgramInference.writeResultsToFile(wpiOutputFormat, this.checker);
    }
  }

  /**
   * Determines the annotated type from a type in tree form.
   *
   * <p>Note that we cannot decide from a Tree whether it is a type use or an expression.
   * TreeUtils.isTypeTree is only an under-approximation. For example, an identifier can be either a
   * type or an expression.
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
   * @return the set of qualifiers that are the upper bounds for a use of the type
   */
  public AnnotationMirrorSet getTypeDeclarationBounds(TypeMirror type) {
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
  protected AnnotationMirrorSet getDefaultTypeDeclarationBounds() {
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
    AnnotationMirrorSet bound = getTypeDeclarationBounds(fromTypeTree.getUnderlyingType());
    fromTypeTree.addMissingAnnotations(bound);
    // Annotate any type variables in the type.
    addComputedTypeAnnotations(clause, fromTypeTree);
    return fromTypeTree;
  }

  // **********************************************************************
  // Factories for annotated types that do not account for default qualifiers.
  // They only include qualifiers explicitly inserted by the user.
  // **********************************************************************

  /**
   * Creates an AnnotatedTypeMirror for {@code elt} that includes: annotations explicitly written on
   * the element and annotations from stub files.
   *
   * <p>Does not include default qualifiers. To obtain them, use {@link #getAnnotatedType(Element)}.
   *
   * <p>Does not include fake overrides from the stub file.
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

    // Because of a bug in Java 8, annotations on type parameters are not stored in elements, so
    // get explicit annotations from the tree. (This bug has been fixed in Java 9.)  Also, since
    // annotations computed by the AnnotatedTypeFactory are stored in the element, the
    // annotations have to be retrieved from the tree so that only explicit annotations are
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

    type = mergeAnnotationFileAnnosIntoType(type, elt, ajavaTypes);
    if (currentFileAjavaTypes != null) {
      type = mergeAnnotationFileAnnosIntoType(type, elt, currentFileAjavaTypes);
    }

    if (mergeStubsWithSource) {
      if (debugStubParser) {
        System.out.printf("fromElement: mergeStubsIntoType(%s, %s)", type, elt);
      }
      type = mergeAnnotationFileAnnosIntoType(type, elt, stubTypes);
      if (debugStubParser) {
        System.out.printf(" => %s%n", type);
      }
    }
    // Caching is disabled if annotation files are being parsed, because calls to this
    // method before the annotation files are fully read can return incorrect results.
    if (shouldCache
        && !stubTypes.isParsing()
        && !ajavaTypes.isParsing()
        && (currentFileAjavaTypes == null || !currentFileAjavaTypes.isParsing())) {
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
   * AnnotatedTypeMirror contains annotations explicitly written on the tree, and possibly others as
   * described below.
   *
   * <p>If a VariableTree is a parameter to a lambda, this method also adds annotations from the
   * declared type of the functional interface and the executable type of its method.
   *
   * <p>The returned AnnotatedTypeMirror also contains explicitly written annotations from any ajava
   * file and if {@code -AmergeStubsWithSource} is passed, it also merges any explicitly written
   * annotations from stub files.
   *
   * @param tree a {@link MethodTree} or {@link VariableTree}
   * @return AnnotatedTypeMirror with explicit annotations from {@code tree}
   */
  private AnnotatedTypeMirror fromMember(Tree tree) {
    if (!(tree instanceof MethodTree || tree instanceof VariableTree)) {
      throw new BugInCF(
          "AnnotatedTypeFactory.fromMember: not a method or variable declaration: " + tree);
    }
    if (shouldCache && fromMemberTreeCache.containsKey(tree)) {
      return fromMemberTreeCache.get(tree).deepCopy();
    }
    AnnotatedTypeMirror result = TypeFromTree.fromMember(this, tree);

    result = mergeAnnotationFileAnnosIntoType(result, tree, ajavaTypes);
    if (currentFileAjavaTypes != null) {
      result = mergeAnnotationFileAnnosIntoType(result, tree, currentFileAjavaTypes);
    }

    if (mergeStubsWithSource) {
      if (debugStubParser) {
        System.out.printf("fromClass: mergeStubsIntoType(%s, %s)", result, tree);
      }
      result = mergeAnnotationFileAnnosIntoType(result, tree, stubTypes);
      if (debugStubParser) {
        System.out.printf(" => %s%n", result);
      }
    }

    if (shouldCache) {
      fromMemberTreeCache.put(tree, result.deepCopy());
    }

    return result;
  }

  /**
   * Merges types from annotation files for {@code tree} into {@code type} by taking the greatest
   * lower bound of the annotations in both.
   *
   * @param type the type to apply annotation file types to
   * @param tree the tree from which to read annotation file types
   * @param source storage for current annotation file annotations
   * @return the given type, side-effected to add the annotation file types
   */
  private AnnotatedTypeMirror mergeAnnotationFileAnnosIntoType(
      @Nullable AnnotatedTypeMirror type, Tree tree, AnnotationFileElementTypes source) {
    Element elt = TreeUtils.elementFromTree(tree);
    return mergeAnnotationFileAnnosIntoType(type, elt, source);
  }

  /**
   * A scanner used to combine annotations from two AnnotatedTypeMirrors. The scanner requires
   * {@link #qualHierarchy}, which is set in {@link #postInit()} rather than the construtor, so
   * lazily initialize this field before use.
   */
  private @MonotonicNonNull AnnotatedTypeCombiner annotatedTypeCombiner = null;

  /**
   * Merges types from annotation files for {@code elt} into {@code type} by taking the greatest
   * lower bound of the annotations in both.
   *
   * @param type the type to apply annotation file types to
   * @param elt the element from which to read annotation file types
   * @param source storage for current annotation file annotations
   * @return the type, side-effected to add the annotation file types
   */
  protected AnnotatedTypeMirror mergeAnnotationFileAnnosIntoType(
      @Nullable AnnotatedTypeMirror type, Element elt, AnnotationFileElementTypes source) {
    AnnotatedTypeMirror typeFromFile = source.getAnnotatedTypeMirror(elt);
    if (typeFromFile == null) {
      return type;
    }
    if (type == null) {
      return typeFromFile;
    }
    if (annotatedTypeCombiner == null) {
      annotatedTypeCombiner = new AnnotatedTypeCombiner(qualHierarchy);
    }
    // Must merge (rather than only take the annotation file type if it is a subtype) to support
    // WPI.
    annotatedTypeCombiner.visit(typeFromFile, type);
    return type;
  }

  /**
   * Creates an AnnotatedTypeMirror for an ExpressionTree. The AnnotatedTypeMirror contains explicit
   * annotations written on the expression and for some expressions, annotations from
   * sub-expressions that could have been explicitly written, defaulted, refined, or otherwise
   * computed. (Expressions whose type include annotations from sub-expressions are:
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
    logGat("fromExpression(%s) of kind %s%n", tree, tree.getKind());
    if (shouldCache && fromExpressionTreeCache.containsKey(tree)) {
      logGat("fromExpression(%s) => [cached] %s%n", tree, fromExpressionTreeCache.get(tree));
      return fromExpressionTreeCache.get(tree).deepCopy();
    }

    AnnotatedTypeMirror result = TypeFromTree.fromExpression(this, tree);

    if (shouldCache
        // Don't cache the type of some expressions, because incorrect annotations would be
        // cached during dataflow analysis. See Issue #602.
        && tree.getKind() != Tree.Kind.NEW_CLASS
        && tree.getKind() != Tree.Kind.NEW_ARRAY
        && tree.getKind() != Tree.Kind.CONDITIONAL_EXPRESSION) {
      fromExpressionTreeCache.put(tree, result.deepCopy());
    }
    logGat("fromExpression(%s) => %s%n", tree, result);
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
  /*package-private*/ final AnnotatedTypeMirror fromTypeTree(Tree tree) {
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
   * (flow-sensitive type refinement). Its subclasses usually override it only to customize default
   * annotations.
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
   * correct annotations cannot be computed because of type argument of raw types. (See {@link
   * AnnotatedWildcardType#isTypeArgOfRawType()}.)
   *
   * @param type annotated type to which default annotations are added
   */
  public void addDefaultAnnotations(AnnotatedTypeMirror type) {
    // Pass.
  }

  /**
   * A callback method for the AnnotatedTypeFactory subtypes to customize directSupertypes().
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
    AnnotationMirrorSet annotations = type.getEffectiveAnnotations();
    for (AnnotatedTypeMirror supertype : supertypes) {
      if (!annotations.equals(supertype.getEffectiveAnnotations())) {
        supertype.clearPrimaryAnnotations();
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
  public void postAsMemberOf(AnnotatedTypeMirror type, AnnotatedTypeMirror owner, Element element) {
    if (element.getKind() == ElementKind.FIELD) {
      addAnnotationFromFieldInvariant(type, owner, (VariableElement) element);
    }
    addComputedTypeAnnotations(element, type);
  }

  /**
   * Adds the qualifier specified by a field invariant for {@code field} to {@code type}.
   *
   * @param type annotated type to which the annotation is added
   * @param accessedVia the annotated type of the receiver of the accessing tree. (Only used to get
   *     the type element of the underling type.)
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
    if (ElementUtils.enclosingTypeElement(field).equals(typeElement)) {
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
   * @return field invariants for {@code element}
   */
  public @Nullable FieldInvariants getFieldInvariants(TypeElement element) {
    if (element == null) {
      return null;
    }
    AnnotationMirror fieldInvarAnno = getDeclAnnotation(element, FieldInvariant.class);
    if (fieldInvarAnno == null) {
      return null;
    }
    List<String> fields =
        AnnotationUtils.getElementValueArray(
            fieldInvarAnno, fieldInvariantFieldElement, String.class);
    List<@CanonicalName Name> classes =
        AnnotationUtils.getElementValueClassNames(fieldInvarAnno, fieldInvariantQualifierElement);
    List<AnnotationMirror> qualifiers =
        CollectionsPlume.mapList(
            name ->
                // Calling AnnotationBuilder.fromName (which ignores
                // elements/fields) is acceptable because @FieldInvariant
                // does not handle classes with elements/fields.
                AnnotationBuilder.fromName(elements, name),
            classes);
    if (qualifiers.size() == 1) {
      while (fields.size() > qualifiers.size()) {
        qualifiers.add(qualifiers.get(0));
      }
    }
    if (fields.size() != qualifiers.size()) {
      // The user wrote a malformed @FieldInvariant annotation, so just return a malformed
      // FieldInvariants object.  The BaseTypeVisitor will issue an error.
      return new FieldInvariants(fields, qualifiers, this);
    }

    // Only keep qualifiers that are supported by this checker.  (The other qualifiers cannot
    // be checked by this checker, so they must be ignored.)
    List<String> annotatedFields = new ArrayList<>();
    List<AnnotationMirror> supportedQualifiers = new ArrayList<>();
    for (int i = 0; i < fields.size(); i++) {
      if (isSupportedQualifier(qualifiers.get(i))) {
        annotatedFields.add(fields.get(i));
        supportedQualifiers.add(qualifiers.get(i));
      }
    }
    if (annotatedFields.isEmpty()) {
      return null;
    }

    return new FieldInvariants(annotatedFields, supportedQualifiers, this);
  }

  /**
   * Returns the element of {@code annoTrees} that is a use of one of the field invariant
   * annotations (as specified via {@link #getFieldInvariantDeclarationAnnotations()}. If one isn't
   * found, null is returned.
   *
   * @param annoTrees list of trees to search; the result is one of the list elements, or null
   * @return the AnnotationTree that is a use of one of the field invariant annotations, or null if
   *     one isn't found
   */
  public @Nullable AnnotationTree getFieldInvariantAnnotationTree(
      @Nullable List<? extends AnnotationTree> annoTrees) {
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

  /** The classes of field invariant annotations. */
  private Set<Class<? extends Annotation>> fieldInvariantDeclarationAnnotations =
      Collections.singleton(FieldInvariant.class);

  /**
   * Returns the set of classes of field invariant annotations.
   *
   * @return the set of classes of field invariant annotations
   */
  protected Set<Class<? extends Annotation>> getFieldInvariantDeclarationAnnotations() {
    return fieldInvariantDeclarationAnnotations;
  }

  /**
   * Adapt the upper bounds of the type variables of a class relative to the type instantiation. In
   * some type systems, the upper bounds depend on the instantiation of the class. For example, in
   * the Generic Universe Type system, consider a class declaration
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
   * <p>The upper bound of E adapted to the argument String, would be {@code List<@NonNull String>}
   * and the lower bound would be an AnnotatedNullType.
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

    Map<TypeVariable, AnnotatedTypeMirror> typeParamToTypeArg = new HashMap<>();

    AnnotatedDeclaredType enclosing = type;
    while (enclosing != null) {
      List<AnnotatedTypeMirror> enclosingTArgs = enclosing.getTypeArguments();
      AnnotatedDeclaredType declaredType =
          getAnnotatedType((TypeElement) enclosing.getUnderlyingType().asElement());
      List<AnnotatedTypeMirror> enclosingTVars = declaredType.getTypeArguments();
      for (int i = 0; i < enclosingTArgs.size(); i++) {
        AnnotatedTypeVariable enclosingTVar = (AnnotatedTypeVariable) enclosingTVars.get(i);
        typeParamToTypeArg.put(enclosingTVar.getUnderlyingType(), enclosingTArgs.get(i));
      }
      enclosing = enclosing.getEnclosingType();
    }

    List<AnnotatedTypeParameterBounds> res = new ArrayList<>(tvars.size());

    for (AnnotatedTypeMirror atm : tvars) {
      AnnotatedTypeVariable atv = (AnnotatedTypeVariable) atm;
      AnnotatedTypeMirror upper =
          typeVarSubstitutor.substitute(typeParamToTypeArg, atv.getUpperBound());
      AnnotatedTypeMirror lower =
          typeVarSubstitutor.substitute(typeParamToTypeArg, atv.getLowerBound());
      res.add(new AnnotatedTypeParameterBounds(upper, lower));
    }
    return res;
  }

  /**
   * Creates and returns an AnnotatedNullType qualified with {@code annotations}.
   *
   * @param annotations the set of AnnotationMirrors to qualify the returned type with
   * @return AnnotatedNullType qualified with {@code annotations}
   */
  public AnnotatedNullType getAnnotatedNullType(Set<? extends AnnotationMirror> annotations) {
    AnnotatedTypeMirror.AnnotatedNullType nullType =
        (AnnotatedNullType) toAnnotatedType(processingEnv.getTypeUtils().getNullType(), false);
    nullType.addAnnotations(annotations);
    return nullType;
  }

  // **********************************************************************
  // Utilities method for getting specific types from trees or elements
  // **********************************************************************

  /**
   * Return the implicit receiver type of an expression tree.
   *
   * <p>The result is null for expressions that don't have a receiver, e.g. for a local variable or
   * method parameter access. The result is also null for expressions that have an explicit
   * receiver.
   *
   * <p>Clients should generally call {@link #getReceiverType}.
   *
   * @param tree the expression that might have an implicit receiver
   * @return the type of the implicit receiver. Returns null if the expression has an explicit
   *     receiver or doesn't have a receiver.
   */
  protected @Nullable AnnotatedDeclaredType getImplicitReceiverType(ExpressionTree tree) {
    assert (tree.getKind() == Tree.Kind.IDENTIFIER
            || tree.getKind() == Tree.Kind.MEMBER_SELECT
            || tree.getKind() == Tree.Kind.METHOD_INVOCATION
            || tree.getKind() == Tree.Kind.NEW_CLASS)
        : "Unexpected tree kind: " + tree.getKind();

    // Return null if the element kind has no receiver.
    Element element = TreeUtils.elementFromUse(tree);
    assert element != null : "Unexpected null element for tree: " + tree;
    if (!ElementUtils.hasReceiver(element)) {
      return null;
    }

    // Return null if the receiver is explicit.
    if (TreeUtils.getReceiverTree(tree) != null) {
      return null;
    }

    TypeElement elementOfImplicitReceiver = ElementUtils.enclosingTypeElement(element);
    if (tree.getKind() == Tree.Kind.NEW_CLASS) {
      if (elementOfImplicitReceiver.getEnclosingElement() != null) {
        elementOfImplicitReceiver =
            ElementUtils.enclosingTypeElement(elementOfImplicitReceiver.getEnclosingElement());
      } else {
        elementOfImplicitReceiver = null;
      }
      if (elementOfImplicitReceiver == null) {
        // If the typeElt does not have an enclosing class, then the NewClassTree
        // does not have an implicit receiver.
        return null;
      }
    }

    TypeMirror typeOfImplicitReceiver = elementOfImplicitReceiver.asType();
    AnnotatedDeclaredType thisType = getSelfType(tree);
    if (thisType == null) {
      return null;
    }
    // An implicit receiver is the first enclosing type that is a subtype of the type where the
    // element is declared.
    while (thisType != null && !isSubtype(thisType.getUnderlyingType(), typeOfImplicitReceiver)) {
      thisType = thisType.getEnclosingType();
    }
    return thisType;
  }

  /**
   * Returns the type of {@code this} at the location of {@code tree}. Returns {@code null} if
   * {@code tree} is in a location where {@code this} has no meaning, such as the body of a static
   * method.
   *
   * <p>The parameter is an arbitrary tree and does not have to mention "this", neither explicitly
   * nor implicitly. This method can be overridden for type-system specific behavior.
   *
   * @param tree location used to decide the type of {@code this}
   * @return the type of {@code this} at the location of {@code tree}
   */
  public @Nullable AnnotatedDeclaredType getSelfType(Tree tree) {
    logGat("getSelfType(%s) of kind %s%n", tree, tree.getKind());
    if (TreeUtils.isClassTree(tree)) {
      return getAnnotatedType(TreeUtils.elementFromDeclaration((ClassTree) tree));
    }

    Tree enclosingTree = getEnclosingClassOrMethod(tree);
    if (enclosingTree == null) {
      // tree is inside an annotation, where "this" is not allowed. So, no self type exists.
      return null;
    } else if (enclosingTree.getKind() == Tree.Kind.METHOD) {
      MethodTree enclosingMethod = (MethodTree) enclosingTree;
      if (TreeUtils.isConstructor(enclosingMethod)) {
        return (AnnotatedDeclaredType) getAnnotatedType(enclosingMethod).getReturnType();
      } else {
        return getAnnotatedType(enclosingMethod).getReceiverType();
      }
    } else if (TreeUtils.isClassTree(enclosingTree)) {
      return (AnnotatedDeclaredType) getAnnotatedType(enclosingTree);
    }
    return null;
  }

  /** A set containing class, method, and annotation tree kinds. */
  private static final Set<Tree.Kind> classMethodAnnotationKinds =
      EnumSet.copyOf(TreeUtils.classTreeKinds());

  static {
    classMethodAnnotationKinds.add(Tree.Kind.METHOD);
    classMethodAnnotationKinds.add(Tree.Kind.TYPE_ANNOTATION);
    classMethodAnnotationKinds.add(Tree.Kind.ANNOTATION);
  }

  /**
   * Returns the innermost enclosing method or class tree of {@code tree}. If {@code tree} is
   * artificial (that is, created by dataflow), then {@link #artificialTreeToEnclosingElementMap} is
   * used to find the enclosing tree.
   *
   * <p>If the tree is inside an annotation, then {@code null} is returned.
   *
   * @param tree tree to whose innermost enclosing method or class to return
   * @return the innermost enclosing method or class tree of {@code tree}, or {@code null} if {@code
   *     tree} is inside an annotation
   */
  public @Nullable Tree getEnclosingClassOrMethod(Tree tree) {
    TreePath path = getPath(tree);
    Tree enclosing = TreePathUtil.enclosingOfKind(path, classMethodAnnotationKinds);
    if (enclosing != null) {
      if (enclosing.getKind() == Tree.Kind.ANNOTATION
          || enclosing.getKind() == Tree.Kind.TYPE_ANNOTATION) {
        return null;
      }
      return enclosing;
    }
    Element e = getEnclosingElementForArtificialTree(tree);
    if (e != null) {
      Element enclosingMethodOrClass = e;
      while (enclosingMethodOrClass != null
          && enclosingMethodOrClass.getKind() != ElementKind.METHOD
          && !enclosingMethodOrClass.getKind().isClass()
          && !enclosingMethodOrClass.getKind().isInterface()) {
        enclosingMethodOrClass = enclosingMethodOrClass.getEnclosingElement();
      }
      return declarationFromElement(enclosingMethodOrClass);
    }
    return TreePathUtil.enclosingClass(path);
  }

  /**
   * Returns the {@link AnnotatedTypeMirror} of the enclosing type at the location of {@code tree}
   * that is the same type as {@code typeElement}.
   *
   * @param typeElement type of the enclosing type to return
   * @param tree location to use
   * @return the enclosing type at the location of {@code tree} that is the same type as {@code
   *     typeElement}
   */
  public AnnotatedDeclaredType getEnclosingType(TypeElement typeElement, Tree tree) {
    AnnotatedDeclaredType thisType = getSelfType(tree);
    while (!isSameType(thisType.getUnderlyingType(), typeElement.asType())) {
      thisType = thisType.getEnclosingType();
    }
    return thisType;
  }

  /**
   * Returns the {@link AnnotatedTypeMirror} of the enclosing type at the location of {@code tree}
   * that is a subtype of {@code typeElement}.
   *
   * @param typeElement super type of the enclosing type to return
   * @param tree location to use
   * @return the enclosing type at the location of {@code tree} that is a subtype of {@code
   *     typeElement}
   */
  public AnnotatedDeclaredType getEnclosingSubType(TypeElement typeElement, Tree tree) {
    AnnotatedDeclaredType thisType = getSelfType(tree);
    while (!isSubtype(thisType.getUnderlyingType(), typeElement.asType())) {
      thisType = thisType.getEnclosingType();
    }
    return thisType;
  }

  /**
   * Returns true if the erasure of {@code type1} is a Java subtype of the erasure of {@code type2}.
   *
   * @param type1 a type
   * @param type2 a type
   * @return true if the erasure of {@code type1} is a Java subtype of the erasure of {@code type2}
   */
  private boolean isSubtype(TypeMirror type1, TypeMirror type2) {
    return types.isSubtype(types.erasure(type1), types.erasure(type2));
  }

  /**
   * Returns true if the erasure of {@code type1} is the same Java type as the erasure of {@code
   * type2}.
   *
   * @param type1 a type
   * @param type2 a type
   * @return true if the erasure of {@code type1} is the same Java type as the erasure of {@code
   *     type2}
   */
  private boolean isSameType(TypeMirror type1, TypeMirror type2) {
    return types.isSameType(types.erasure(type1), types.erasure(type2));
  }

  /**
   * Returns the receiver type of the expression tree, which might be the type of an implicit {@code
   * this}. Returns null if the expression has no explicit or implicit receiver.
   *
   * @param expression the expression for which to determine the receiver type
   * @return the type of the receiver of expression
   */
  public final @Nullable AnnotatedTypeMirror getReceiverType(ExpressionTree expression) {
    ExpressionTree receiver = TreeUtils.getReceiverTree(expression);
    if (receiver != null) {
      return getAnnotatedType(receiver);
    }

    Element element = TreeUtils.elementFromTree(expression);
    if (element != null && ElementUtils.hasReceiver(element)) {
      // The tree references an element that has a receiver, but the tree does not have an
      // explicit receiver. So, the tree must have an implicit receiver of "this" or
      // "Outer.this".
      return getImplicitReceiverType(expression);
    } else {
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

    @Override
    public String toString() {
      if (typeArgs.isEmpty()) {
        return executableType.toString();
      } else {
        StringJoiner typeArgsString = new StringJoiner(",", "<", ">");
        for (AnnotatedTypeMirror atm : typeArgs) {
          typeArgsString.add(atm.toString());
        }
        return typeArgsString + " " + executableType.toString();
      }
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
   * <p>As an implementation detail, this method depends on {@link AnnotatedTypes#asMemberOf(Types,
   * AnnotatedTypeFactory, AnnotatedTypeMirror, Element)}, and customization based on receiver type
   * should be in accordance to its specification.
   *
   * <p>The return type is a pair of the type of the invoked method and the (inferred) type
   * arguments. Note that neither the explicitly passed nor the inferred type arguments are
   * guaranteed to be subtypes of the corresponding upper bounds. See method {@link
   * org.checkerframework.common.basetype.BaseTypeVisitor#checkTypeArguments} for the checks of type
   * argument well-formedness.
   *
   * <p>Note that "this" and "super" constructor invocations are also handled by this method
   * (explicit or implicit ones, at the beginning of a constructor). Method {@link
   * #constructorFromUse(NewClassTree)} is only used for a constructor invocation in a "new"
   * expression.
   *
   * @param tree the method invocation tree
   * @return the type of the invoked method and any (explict or inferred) type arguments
   */
  public final ParameterizedExecutableType methodFromUse(MethodInvocationTree tree) {
    return methodFromUse(tree, true);
  }

  /**
   * Returns the same as {@link #methodFromUse(MethodInvocationTree)}, but without inferred type
   * arguments.
   *
   * @param tree a method invocation tree
   * @return the type of the invoked method and any explicit type arguments
   */
  public ParameterizedExecutableType methodFromUseWithoutTypeArgInference(
      MethodInvocationTree tree) {
    return methodFromUse(tree, false);
  }

  /**
   * The implementation of {@link #methodFromUse(MethodInvocationTree)} and {@link
   * #methodFromUseWithoutTypeArgInference(MethodInvocationTree)}.
   *
   * @param tree a method invocation tree
   * @param inferTypeArgs whether type arguments should be inferred
   * @return the type of the invoked method, any explicit type arguments, and if {@code
   *     inferTypeArgs} is true, any inferred type arguments
   */
  protected ParameterizedExecutableType methodFromUse(
      MethodInvocationTree tree, boolean inferTypeArgs) {
    ExecutableElement methodElt = TreeUtils.elementFromUse(tree);
    AnnotatedTypeMirror receiverType = getReceiverType(tree);
    if (receiverType == null && TreeUtils.isSuperConstructorCall(tree)) {
      // super() calls don't have a receiver, but they should be view-point adapted as if
      // "this" is the receiver.
      receiverType = getSelfType(tree);
    }
    if (receiverType != null && receiverType.getKind() == TypeKind.DECLARED) {
      receiverType = applyCaptureConversion(receiverType);
    }

    ParameterizedExecutableType result =
        methodFromUse(tree, methodElt, receiverType, inferTypeArgs);
    if (checker.shouldResolveReflection()
        && reflectionResolver.isReflectiveMethodInvocation(tree)) {
      result = reflectionResolver.resolveReflectiveCall(this, tree, result);
    }

    AnnotatedExecutableType method = result.executableType;
    if (AnnotatedTypes.isTypeArgOfRawType(method.getReturnType())) {
      // Get the correct Java type from the tree and use it as the upper bound of the
      // wildcard.
      TypeMirror tm = TreeUtils.typeOf(tree);
      AnnotatedTypeMirror t = toAnnotatedType(tm, false);

      AnnotatedWildcardType wildcard = (AnnotatedWildcardType) method.getReturnType();
      if (ignoreRawTypeArguments) {
        // Remove the annotations so that default annotations are used instead.
        // (See call to addDefaultAnnotations below.)
        t.clearPrimaryAnnotations();
      } else {
        t.replaceAnnotations(wildcard.getExtendsBound().getPrimaryAnnotations());
      }
      wildcard.setExtendsBound(t);
      addDefaultAnnotations(wildcard);
    }

    return result;
  }

  /**
   * Determines the type of the invoked method based on the passed expression tree, executable
   * element, and receiver type.
   *
   * @param tree either a MethodInvocationTree or a MemberReferenceTree
   * @param methodElt the element of the referenced method
   * @param receiverType the type of the receiver
   * @return the type of the method being invoked with tree and the (inferred) type arguments
   * @see #methodFromUse(MethodInvocationTree)
   */
  public final ParameterizedExecutableType methodFromUse(
      ExpressionTree tree, ExecutableElement methodElt, AnnotatedTypeMirror receiverType) {
    return methodFromUse(tree, methodElt, receiverType, true);
  }

  /**
   * Returns the same as {@link #methodFromUse(ExpressionTree, ExecutableElement,
   * AnnotatedTypeMirror)}, but without inferred type arguments.
   *
   * @param tree either a MethodInvocationTree or a MemberReferenceTree
   * @param methodElt the element of the referenced method
   * @param receiverType the type of the receiver
   * @return the type of the method being invoked with tree without inferring type arguments
   */
  public final ParameterizedExecutableType methodFromUseWithoutTypeArgInference(
      ExpressionTree tree, ExecutableElement methodElt, AnnotatedTypeMirror receiverType) {
    return methodFromUse(tree, methodElt, receiverType, false);
  }

  /**
   * The implementation of {@link #methodFromUse(ExpressionTree, ExecutableElement,
   * AnnotatedTypeMirror)} and {@link #methodFromUseWithoutTypeArgInference(ExpressionTree,
   * ExecutableElement, AnnotatedTypeMirror)}.
   *
   * @param tree either a MethodInvocationTree or a MemberReferenceTree
   * @param methodElt the element of the referenced method
   * @param receiverType the type of the receiver
   * @param inferTypeArgs whether type arguments should be inferred
   * @return the type of the invoked method
   */
  protected ParameterizedExecutableType methodFromUse(
      ExpressionTree tree,
      ExecutableElement methodElt,
      AnnotatedTypeMirror receiverType,
      boolean inferTypeArgs) {
    AnnotatedExecutableType memberTypeWithoutOverrides =
        getAnnotatedType(methodElt); // get unsubstituted type
    AnnotatedExecutableType memberTypeWithOverrides =
        applyFakeOverrides(receiverType, methodElt, memberTypeWithoutOverrides);
    memberTypeWithOverrides = applyRecordTypesToAccessors(methodElt, memberTypeWithOverrides);
    methodFromUsePreSubstitution(tree, memberTypeWithOverrides, inferTypeArgs);

    AnnotatedExecutableType methodType =
        AnnotatedTypes.asMemberOf(types, this, receiverType, methodElt, memberTypeWithOverrides);
    List<AnnotatedTypeMirror> typeargs = new ArrayList<>(methodElt.getTypeParameters().size());

    TypeArguments typeArguments =
        AnnotatedTypes.findTypeArguments(this, tree, methodElt, methodType, inferTypeArgs);
    Map<TypeVariable, AnnotatedTypeMirror> typeParamToTypeArg = typeArguments.typeArguments;
    if (!typeParamToTypeArg.isEmpty()) {
      for (AnnotatedTypeVariable tv : methodType.getTypeVariables()) {
        typeargs.add(typeParamToTypeArg.get(tv.getUnderlyingType()));
      }
      methodType =
          (AnnotatedExecutableType) typeVarSubstitutor.substitute(typeParamToTypeArg, methodType);
    }

    if (typeArguments.inferenceCrash && tree instanceof MethodInvocationTree) {
      // If inference crashed, then the return type will not be the correct Java type.  This
      // can cause crashes elsewhere in the framework.  To avoid those crashes, create an ATM
      // with the correct Java type and default annotations.  (If inference crashes an error
      // will be issued in the BaseTypeVisitor.)
      TypeMirror type = TreeUtils.typeOf(tree);
      AnnotatedTypeMirror returnType = AnnotatedTypeMirror.createType(type, this, false);
      addDefaultAnnotations(returnType);
      methodType.setReturnType(returnType);
    } else if (typeArguments.uncheckedConversion) {
      methodType.setReturnType(methodType.getReturnType().getErased());
    }

    if (tree.getKind() == Tree.Kind.METHOD_INVOCATION
        && TreeUtils.isMethodInvocation(tree, objectGetClass, processingEnv)) {
      adaptGetClassReturnTypeToReceiver(methodType, receiverType, tree);
    }

    return new ParameterizedExecutableType(methodType, typeargs);
  }

  /**
   * Given a member and its type, returns the type with fake overrides applied to it.
   *
   * @param receiverType the type of the class that contains member (or a subtype of it)
   * @param member a type member, such as a method or field
   * @param memberType the type of {@code member}
   * @return {@code memberType}, adjusted according to fake overrides
   */
  private AnnotatedExecutableType applyFakeOverrides(
      AnnotatedTypeMirror receiverType, Element member, AnnotatedExecutableType memberType) {
    // Currently, handle only methods, not fields.  TODO: Handle fields.
    if (memberType.getKind() != TypeKind.EXECUTABLE) {
      return memberType;
    }

    AnnotationFileElementTypes afet = stubTypes;
    AnnotatedExecutableType methodType = afet.getFakeOverride(member, receiverType);
    if (methodType == null) {
      methodType = memberType;
    }
    return methodType;
  }

  /**
   * Given a method, checks if there is: a record component with the same name AND the record
   * component has an annotation AND the method has no-arguments. If so, replaces the annotations on
   * the method return type with those from the record type in the same hierarchy.
   *
   * @param member a method or constructor
   * @param memberType the type of the method/constructor; side-effected by this method
   * @return {@code memberType} with annotations replaced if applicable
   */
  private AnnotatedExecutableType applyRecordTypesToAccessors(
      ExecutableElement member, AnnotatedExecutableType memberType) {
    if (memberType.getKind() != TypeKind.EXECUTABLE) {
      throw new BugInCF(
          "member %s has type %s of kind %s", member, memberType, memberType.getKind());
    }

    stubTypes.injectRecordComponentType(types, member, memberType);

    return memberType;
  }

  /**
   * A callback method for the AnnotatedTypeFactory subtypes to customize the handling of the
   * declared method type before type variable substitution.
   *
   * @param tree either a method invocation or a member reference tree
   * @param type declared method type before type variable substitution
   * @param resolvePolyQuals whether to resolve polymorphic qualifiers
   */
  protected void methodFromUsePreSubstitution(
      ExpressionTree tree, AnnotatedExecutableType type, boolean resolvePolyQuals) {
    assert tree instanceof MethodInvocationTree || tree instanceof MemberReferenceTree;
  }

  /**
   * Java special-cases the return type of {@link java.lang.Class#getClass() getClass()}. Though the
   * method has a return type of {@code Class<?>}, the compiler special cases this return-type and
   * changes the bound of the type argument to the erasure of the receiver type. For example:
   *
   * <ul>
   *   <li>{@code x.getClass()} has the type {@code Class< ? extends erasure_of_x >}
   *   <li>{@code someInteger.getClass()} has the type {@code Class< ? extends Integer >}
   * </ul>
   *
   * @param getClassType this must be a type representing a call to Object.getClass otherwise a
   *     runtime exception will be thrown. It is modified by side effect.
   * @param receiverType the receiver type of the method invocation (not the declared receiver type)
   * @param tree getClass method invocation tree
   */
  protected void adaptGetClassReturnTypeToReceiver(
      AnnotatedExecutableType getClassType, AnnotatedTypeMirror receiverType, ExpressionTree tree) {

    TypeMirror type = TreeUtils.typeOf(tree);
    AnnotatedTypeMirror returnType = AnnotatedTypeMirror.createType(type, this, false);

    if (returnType == null
        || !(returnType.getKind() == TypeKind.DECLARED)
        || ((AnnotatedDeclaredType) returnType).getTypeArguments().size() != 1) {
      throw new BugInCF(
          "Unexpected type passed to AnnotatedTypes.adaptGetClassReturnTypeToReceiver%n"
              + "getClassType=%s%nreceiverType=%s",
          getClassType, receiverType);
    }

    AnnotatedWildcardType classWildcardArg =
        (AnnotatedWildcardType)
            ((AnnotatedDeclaredType) getClassType.getReturnType()).getTypeArguments().get(0);
    getClassType.setReturnType(returnType);

    // Usually, the only locations that will add annotations to the return type are getClass in
    // stub files defaults and propagation tree annotator.  Since getClass is final they cannot
    // come from source code.  Also, since the newBound is an erased type we have no type
    // arguments.  So, we just copy the annotations from the bound of the declared type to the
    // new bound.
    AnnotationMirrorSet newAnnos = new AnnotationMirrorSet();
    AnnotationMirrorSet receiverTypeBoundAnnos =
        getTypeDeclarationBounds(receiverType.getErased().getUnderlyingType());
    AnnotationMirrorSet wildcardBoundAnnos = classWildcardArg.getEffectiveAnnotations();
    for (AnnotationMirror receiverTypeBoundAnno : receiverTypeBoundAnnos) {
      AnnotationMirror wildcardAnno =
          qualHierarchy.findAnnotationInSameHierarchy(wildcardBoundAnnos, receiverTypeBoundAnno);
      if (typeHierarchy.isSubtypeShallowEffective(receiverTypeBoundAnno, classWildcardArg)) {
        newAnnos.add(receiverTypeBoundAnno);
      } else {
        newAnnos.add(wildcardAnno);
      }
    }
    AnnotatedTypeMirror newTypeArg =
        ((AnnotatedDeclaredType) getClassType.getReturnType()).getTypeArguments().get(0);
    ((AnnotatedTypeVariable) newTypeArg).getUpperBound().replaceAnnotations(newAnnos);
  }

  /**
   * Return the element type of {@code expression}. This is usually the type of {@code
   * expression.itertor().next()}. If {@code expression} is an array, it is the component type of
   * the array.
   *
   * @param expression an expression whose type is an array or implements {@link Iterable}
   * @return the type of {@code expression.itertor().next()} or if {@code expression} is an array,
   *     the component type of the array.
   */
  public AnnotatedTypeMirror getIterableElementType(ExpressionTree expression) {
    return getIterableElementType(expression, getAnnotatedType(expression));
  }

  /**
   * Return the element type of {@code iterableType}. This is usually the type of {@code
   * expression.itertor().next()}. If {@code expression} is an array, it is the component type of
   * the array.
   *
   * @param expression an expression whose type is an array or implements {@link Iterable}
   * @param iterableType the type of the expression
   * @return the type of {@code expression.itertor().next()} or if {@code expression} is an array,
   *     the component type of the array.
   */
  protected AnnotatedTypeMirror getIterableElementType(
      ExpressionTree expression, AnnotatedTypeMirror iterableType) {
    switch (iterableType.getKind()) {
      case ARRAY:
        return ((AnnotatedArrayType) iterableType).getComponentType();
      case WILDCARD:
        return getIterableElementType(
            expression, ((AnnotatedWildcardType) iterableType).getExtendsBound().deepCopy());
      case TYPEVAR:
        return getIterableElementType(
            expression, ((AnnotatedTypeVariable) iterableType).getUpperBound());
      case DECLARED:
        AnnotatedDeclaredType dt =
            AnnotatedTypes.asSuper(this, iterableType, this.iterableDeclType);
        if (dt.getTypeArguments().isEmpty()) {
          TypeElement e = ElementUtils.getTypeElement(processingEnv, Object.class);
          return getAnnotatedType(e);
        } else {
          return dt.getTypeArguments().get(0);
        }

        // TODO: Properly desugar Iterator.next(), which is needed if an annotated JDK has
        // annotations on Iterator#next.
        // The below doesn't work because methodFromUse() assumes that the expression tree
        // matches the method element.
        // TypeElement iteratorElement =
        //         ElementUtils.getTypeElement(processingEnv, Iterator.class);
        // AnnotatedTypeMirror iteratorType =
        //         AnnotatedTypeMirror.createType(iteratorElement.asType(), this, false);
        // Map<TypeVariable, AnnotatedTypeMirror> mapping = new HashMap<>();
        // mapping.put(
        //         (TypeVariable) iteratorElement.getTypeParameters().get(0).asType(),
        //          typeArg);
        // iteratorType = typeVarSubstitutor.substitute(mapping, iteratorType);
        // ExecutableElement next =
        //         TreeUtils.getMethod("java.util.Iterator", "next", 0, processingEnv);
        // ParameterizedExecutableType m = methodFromUse(expression, next, iteratorType);
        // return m.executableType.getReturnType();
      default:
        throw new BugInCF(
            "AnnotatedTypeFactory.getIterableElementType: not iterable type: " + iterableType);
    }
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
   * <p>As an implementation detail, this method depends on {@link AnnotatedTypes#asMemberOf(Types,
   * AnnotatedTypeFactory, AnnotatedTypeMirror, Element)}, and customization based on receiver type
   * should be in accordance with its specification.
   *
   * <p>The return type is a pair of the type of the invoked constructor and the (inferred) type
   * arguments. Note that neither the explicitly passed nor the inferred type arguments are
   * guaranteed to be subtypes of the corresponding upper bounds. See method {@link
   * org.checkerframework.common.basetype.BaseTypeVisitor#checkTypeArguments} for the checks of type
   * argument well-formedness.
   *
   * <p>Note that "this" and "super" constructor invocations are handled by method {@link
   * #methodFromUse}. This method only handles constructor invocations in a "new" expression.
   *
   * @param tree the constructor invocation tree
   * @return the annotated type of the invoked constructor (as an executable type) and the
   *     (inferred) type arguments
   */
  public ParameterizedExecutableType constructorFromUse(NewClassTree tree) {
    return constructorFromUse(tree, true);
  }

  /**
   * The same as {@link #constructorFromUse(NewClassTree)}, but no type arguments are inferred.
   *
   * @param tree the constructor invocation tree
   * @return the annotated type of the invoked constructor (as an executable type) and the explicit
   *     type arguments
   */
  public ParameterizedExecutableType constructorFromUseWithoutTypeArgInference(NewClassTree tree) {
    return constructorFromUse(tree, false);
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
    // constructor. The code below recreates some of the logic from TypeFromTree.visitNewClass
    // to do this.

    // The return type of the constructor will be the type of the expression of the member
    // reference tree.
    AnnotatedTypeMirror constructorReturnType =
        fromTypeTree(memberReferenceTree.getQualifierExpression());
    if (TreeUtils.needsTypeArgInference(memberReferenceTree)) {
      // If the method reference is missing type arguments, e.g. LinkedHashMap::new, then the
      // constructorReturnType will be raw.  So, use the return type from the constructor
      // instead.
      AnnotatedTypeMirror re = constructorType.getReturnType().deepCopy(false);
      re.clearPrimaryAnnotations();
      re.addAnnotations(constructorReturnType.getPrimaryAnnotations());
      constructorReturnType = re;
    }

    if (constructorReturnType.getKind() == TypeKind.DECLARED) {
      // Keep only explicit annotations and those from @Poly
      AnnotatedTypes.copyOnlyExplicitConstructorAnnotations(
          this, (AnnotatedDeclaredType) constructorReturnType, constructorType);
    }

    // Now add back defaulting.
    addComputedTypeAnnotations(memberReferenceTree.getQualifierExpression(), constructorReturnType);
    return constructorReturnType;
  }

  /**
   * The implementation of {@link #constructorFromUse(NewClassTree)} and {@link
   * #constructorFromUseWithoutTypeArgInference(NewClassTree)}.
   *
   * @param tree the constructor invocation tree
   * @param inferTypeArgs whether the type arguments should be inferred
   * @return the annotated type of the invoked constructor (as an executable type) and the type
   *     arguments
   */
  protected ParameterizedExecutableType constructorFromUse(
      NewClassTree tree, boolean inferTypeArgs) {
    // Get the annotations written on the new class tree.
    AnnotatedDeclaredType type =
        (AnnotatedDeclaredType) toAnnotatedType(TreeUtils.typeOf(tree), false);
    if (!TreeUtils.isDiamondTree(tree)) {
      if (tree.getClassBody() == null) {
        type.setTypeArguments(getExplicitNewClassClassTypeArgs(tree));
      }
    } else {
      type = getAnnotatedType(TypesUtils.getTypeElement(type.underlyingType));
      // Add explicit annotations below.
      type.clearPrimaryAnnotations();
    }

    AnnotationMirrorSet explicitAnnos = getExplicitNewClassAnnos(tree);
    type.addAnnotations(explicitAnnos);

    // Get the enclosing type of the constructor, if one exists.
    // this.new InnerClass()
    AnnotatedDeclaredType enclosingType = (AnnotatedDeclaredType) getReceiverType(tree);
    type.setEnclosingType(enclosingType);

    // Add computed annotations to the type.
    addComputedTypeAnnotations(tree, type);

    ExecutableElement ctor = TreeUtils.elementFromUse(tree);
    AnnotatedExecutableType con = getAnnotatedType(ctor); // get unsubstituted type
    constructorFromUsePreSubstitution(tree, con, inferTypeArgs);

    if (tree.getClassBody() != null) {
      // Because the anonymous constructor can't have explicit annotations on its parameters,
      // they are copied from the super constructor invoked in the anonymous constructor. To
      // do this:
      // 1. get unsubstituted type of the super constructor.
      // 2. adapt it to this call site.
      // 3. copy the parameters to the anonymous constructor, `con`.
      // 4. copy annotations on the return type to `con`.
      AnnotatedExecutableType superCon = getAnnotatedType(TreeUtils.getSuperConstructor(tree));
      constructorFromUsePreSubstitution(tree, superCon, inferTypeArgs);
      // no viewpoint adaptation needed for super invocation
      superCon = AnnotatedTypes.asMemberOf(types, this, type, superCon.getElement(), superCon);
      if (superCon.getParameterTypes().size() == con.getParameterTypes().size()) {
        con.setParameterTypes(superCon.getParameterTypes());
      } else {
        // If the super class of the anonymous class has an enclosing type, then it is the
        // first parameter of the anonymous constructor. For example,
        // class Outer { class Inner {} }
        //  new Inner(){};
        // Then javac creates the following constructor:
        //  (.Outer x0) {
        //   x0.super();
        //   }
        // So the code below deals with this.
        List<AnnotatedTypeMirror> p = new ArrayList<>(superCon.getParameterTypes().size() + 1);
        if (TreeUtils.hasSyntheticArgument(tree)) {
          p.add(con.getParameterTypes().get(0));
          con.setReceiverType(superCon.getReceiverType());
        } else if (con.getReceiverType() != null) {
          // Because the anonymous constructor doesn't have annotated receiver type,
          // we copy the receiver type from the super constructor invoked in the anonymous
          // constructor and add it to the parameterTypes as the first element.
          con.setReceiverType(superCon.getReceiverType());
          p.add(con.getReceiverType());
        } else {
          p.add(con.getParameterTypes().get(0));
        }
        p.addAll(1, superCon.getParameterTypes());
        con.setParameterTypes(p);
      }
      Set<? extends AnnotationMirror> lub =
          qualHierarchy.leastUpperBoundsShallow(
              type.getPrimaryAnnotations(),
              type.getUnderlyingType(),
              superCon.getReturnType().getPrimaryAnnotations(),
              superCon.getReturnType().getUnderlyingType());
      con.getReturnType().replaceAnnotations(lub);
    } else {
      con = AnnotatedTypes.asMemberOf(types, this, type, ctor, con);
    }
    TypeArguments typeArguments =
        AnnotatedTypes.findTypeArguments(this, tree, ctor, con, inferTypeArgs);
    Map<TypeVariable, AnnotatedTypeMirror> typeParamToTypeArg =
        new HashMap<>(typeArguments.typeArguments);
    List<AnnotatedTypeMirror> typeargs;
    if (typeParamToTypeArg.isEmpty()) {
      typeargs = Collections.emptyList();
    } else {
      typeargs =
          CollectionsPlume.mapList(
              (AnnotatedTypeVariable tv) -> typeParamToTypeArg.get(tv.getUnderlyingType()),
              con.getTypeVariables());
    }

    con = (AnnotatedExecutableType) typeVarSubstitutor.substitute(typeParamToTypeArg, con);

    stubTypes.injectRecordComponentType(types, ctor, con);

    if (typeArguments.inferenceCrash) {
      // If inference crashed, then the return type will not be the correct Java type.  This
      // can cause crashes elsewhere in the framework.  To avoid those crashes, create an ATM
      // with the correct Java type and default annotations.  (If inference crashes an error
      // will be issued in the BaseTypeVisitor.)
      TypeMirror typeTM = TreeUtils.typeOf(tree);
      AnnotatedTypeMirror returnType = AnnotatedTypeMirror.createType(typeTM, this, false);
      addDefaultAnnotations(returnType);
      con.setReturnType(returnType);
    }
    if (enclosingType != null) {
      // Reset the enclosing type because it can be substituted incorrectly.
      ((AnnotatedDeclaredType) con.getReturnType()).setEnclosingType(enclosingType);
    }
    if (type.isUnderlyingTypeRaw() || TypesUtils.isRaw(TreeUtils.typeOf(tree))) {
      ((AnnotatedDeclaredType) con.getReturnType()).setIsUnderlyingTypeRaw();
    }
    if (ctor.getEnclosingElement().getKind() == ElementKind.ENUM) {
      Set<AnnotationMirror> enumAnnos = getEnumConstructorQualifiers();
      con.getReturnType().replaceAnnotations(enumAnnos);
    }
    return new ParameterizedExecutableType(con, typeargs);
  }

  /**
   * Returns the annotations that should be applied to enum constructors. This implementation
   * returns an empty set. Subclasses can override to return a different set.
   *
   * @return the annotations that should be applied to enum constructors
   */
  protected Set<AnnotationMirror> getEnumConstructorQualifiers() {
    return Collections.emptySet();
  }

  /**
   * Returns the annotations explicitly written on a NewClassTree.
   *
   * <p>{@code new @HERE Class()}
   *
   * @param newClassTree a constructor invocation
   * @return the annotations explicitly written on a NewClassTree
   */
  public AnnotationMirrorSet getExplicitNewClassAnnos(NewClassTree newClassTree) {
    if (newClassTree.getClassBody() != null) {
      // In Java 17+, the annotations are on the identifier, so copy them.
      AnnotatedTypeMirror identifierType = fromTypeTree(newClassTree.getIdentifier());
      // In Java 11 and lower, if newClassTree creates an anonymous class, then annotations in
      // this location:
      //   new @HERE Class() {}
      // are not on the identifier newClassTree, but rather on the modifier newClassTree.
      List<? extends AnnotationTree> annoTrees =
          newClassTree.getClassBody().getModifiers().getAnnotations();
      // Add the annotations to an AnnotatedTypeMirror removes the annotations that are not
      // supported by this type system.
      identifierType.addAnnotations(TreeUtils.annotationsFromTypeAnnotationTrees(annoTrees));
      return identifierType.getPrimaryAnnotations();
    } else {
      return fromTypeTree(newClassTree.getIdentifier()).getPrimaryAnnotations();
    }
  }

  /**
   * Returns the partially-annotated explicit class type arguments of the new class tree. The {@code
   * AnnotatedTypeMirror} only include the annotations explicitly written on the explict type
   * arguments. (If {@code newClass} use a diamond operator, this method returns the empty list.)
   * For example, when called with {@code new MyClass<@HERE String>()} this method would return a
   * list containing {@code @HERE String}.
   *
   * @param newClass a new class tree
   * @return the partially annotated {@code AnnotatedTypeMirror}s for the (explicit) class type
   *     arguments of the new class tree
   */
  protected List<AnnotatedTypeMirror> getExplicitNewClassClassTypeArgs(NewClassTree newClass) {
    if (!TreeUtils.isDiamondTree(newClass)) {
      return ((AnnotatedDeclaredType) fromTypeTree(newClass.getIdentifier())).getTypeArguments();
    }
    return Collections.emptyList();
  }

  /**
   * A callback method for the AnnotatedTypeFactory subtypes to customize the handling of the
   * declared constructor type before type variable substitution.
   *
   * @param tree a NewClassTree from constructorFromUse()
   * @param type declared method type before type variable substitution
   * @param resolvePolyQuals whether to resolve polymorphic qualifiers
   */
  protected void constructorFromUsePreSubstitution(
      NewClassTree tree, AnnotatedExecutableType type, boolean resolvePolyQuals) {}

  /**
   * Returns the return type of the method {@code m}.
   *
   * @param m tree of a method declaration
   * @return the return type of the method
   */
  public AnnotatedTypeMirror getMethodReturnType(MethodTree m) {
    AnnotatedExecutableType methodType = getAnnotatedType(m);
    AnnotatedTypeMirror ret = methodType.getReturnType();
    return ret;
  }

  /**
   * Returns the return type of the method {@code m} at the return statement {@code r}. This
   * implementation just calls {@link #getMethodReturnType(MethodTree)}, but subclasses may override
   * this method to change the type based on the return statement.
   *
   * @param m tree of a method declaration
   * @param r a return statement within method {@code m}
   * @return the return type of the method {@code m} at the return statement {@code r}
   */
  public AnnotatedTypeMirror getMethodReturnType(MethodTree m, ReturnTree r) {
    return getMethodReturnType(m);
  }

  /**
   * Returns the annotated boxed type of the given primitive type. The returned type would only have
   * the annotations on the given type.
   *
   * <p>Subclasses may override this method safely to override this behavior.
   *
   * @param type the primitive type
   * @return the boxed declared type of the passed primitive type
   */
  public AnnotatedDeclaredType getBoxedType(AnnotatedPrimitiveType type) {
    TypeElement typeElt = types.boxedClass(type.getUnderlyingType());
    AnnotatedDeclaredType dt = fromElement(typeElt).asUse();
    dt.addAnnotations(type.getPrimaryAnnotations());
    return dt;
  }

  /**
   * Return a primitive type: either the argument, or the result of unboxing it (which might affect
   * its annotations).
   *
   * <p>Subclasses should override {@link #getUnboxedType} rather than this method.
   *
   * @param type a type: a primitive or boxed primitive
   * @return the unboxed variant of the type
   */
  public final AnnotatedPrimitiveType applyUnboxing(AnnotatedTypeMirror type) {
    TypeMirror underlying = type.getUnderlyingType();
    if (TypesUtils.isPrimitive(underlying)) {
      return (AnnotatedPrimitiveType) type;
    } else if (TypesUtils.isBoxedPrimitive(underlying)) {
      return getUnboxedType((AnnotatedDeclaredType) type);
    } else {
      throw new BugInCF("Bad argument to applyUnboxing: " + type);
    }
  }

  /**
   * Returns the annotated primitive type of the given declared type if it is a boxed declared type.
   * Otherwise, it throws <i>IllegalArgumentException</i> exception.
   *
   * <p>In the {@code AnnotatedTypeFactory} implementation, the returned type has the same primary
   * annotations as the given type. Subclasses may override this behavior.
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
    pt.addAnnotations(type.getPrimaryAnnotations());
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
            AnnotatedTypeMirror.createType(stringTypeMirror, this, type.isDeclaration());
    stringATM.addAnnotations(type.getEffectiveAnnotations());
    return stringATM;
  }

  /**
   * Returns a widened type if applicable, otherwise returns its first argument.
   *
   * <p>Subclasses should override {@link #getWidenedAnnotations} rather than this method.
   *
   * @param exprType type to possibly widen
   * @param widenedType type to possibly widen to; its annotations are ignored
   * @return if widening is applicable, the result of converting {@code type} to the underlying type
   *     of {@code widenedType}; otherwise {@code type}
   */
  public final AnnotatedTypeMirror getWidenedType(
      AnnotatedTypeMirror exprType, AnnotatedTypeMirror widenedType) {
    TypeKind exprKind = exprType.getKind();
    TypeKind widenedKind = widenedType.getKind();

    if (!TypeKindUtils.isNumeric(widenedKind)) {
      // The target type is not a numeric primitive, so primitive widening is not applicable.
      return exprType;
    }

    AnnotatedPrimitiveType exprPrimitiveType;
    if (TypeKindUtils.isNumeric(exprKind)) {
      exprPrimitiveType = (AnnotatedPrimitiveType) exprType;
    } else if (TypesUtils.isNumericBoxed(exprType.getUnderlyingType())) {
      exprPrimitiveType = getUnboxedType((AnnotatedDeclaredType) exprType);
    } else {
      return exprType;
    }

    switch (TypeKindUtils.getPrimitiveConversionKind(
        exprPrimitiveType.getKind(), widenedType.getKind())) {
      case WIDENING:
        return getWidenedPrimitive(exprPrimitiveType, widenedType.getUnderlyingType());
      case NARROWING:
        return getNarrowedPrimitive(exprPrimitiveType, widenedType.getUnderlyingType());
      case SAME:
        return exprType;
      default:
        throw new BugInCF("unhandled PrimitiveConversionKind");
    }
  }

  /**
   * Applies widening if applicable, otherwise returns its first argument.
   *
   * <p>Subclasses should override {@link #getWidenedAnnotations} rather than this method.
   *
   * @param exprAnnos annotations to possibly widen
   * @param exprTypeMirror type to possibly widen
   * @param widenedType type to possibly widen to; its annotations are ignored
   * @return if widening is applicable, the result of converting {@code type} to the underlying type
   *     of {@code widenedType}; otherwise {@code type}
   */
  public final AnnotatedTypeMirror getWidenedType(
      AnnotationMirrorSet exprAnnos, TypeMirror exprTypeMirror, AnnotatedTypeMirror widenedType) {
    AnnotatedTypeMirror exprType = toAnnotatedType(exprTypeMirror, false);
    exprType.replaceAnnotations(exprAnnos);
    return getWidenedType(exprType, widenedType);
  }

  /**
   * Returns an AnnotatedPrimitiveType with underlying type {@code widenedTypeMirror} and with
   * annotations copied or adapted from {@code type}.
   *
   * @param type type to widen; a primitive or boxed primitive
   * @param widenedTypeMirror underlying type for the returned type mirror; a primitive or boxed
   *     primitive (same boxing as {@code type})
   * @return result of converting {@code type} to {@code widenedTypeMirror}
   */
  private AnnotatedPrimitiveType getWidenedPrimitive(
      AnnotatedPrimitiveType type, TypeMirror widenedTypeMirror) {
    AnnotatedPrimitiveType result =
        (AnnotatedPrimitiveType)
            AnnotatedTypeMirror.createType(widenedTypeMirror, this, type.isDeclaration());
    result.addAnnotations(
        getWidenedAnnotations(type.getPrimaryAnnotations(), type.getKind(), result.getKind()));
    return result;
  }

  /**
   * Returns annotations applicable to type {@code narrowedTypeKind}, that are copied or adapted
   * from {@code annos}.
   *
   * @param annos annotations to narrow, from a primitive or boxed primitive
   * @param typeKind primitive type to narrow
   * @param narrowedTypeKind target for the returned annotations; a primitive type that is narrower
   *     than {@code typeKind} (in the sense of JLS 5.1.3).
   * @return result of converting {@code annos} from {@code typeKind} to {@code narrowedTypeKind}
   */
  public AnnotationMirrorSet getNarrowedAnnotations(
      AnnotationMirrorSet annos, TypeKind typeKind, TypeKind narrowedTypeKind) {
    return annos;
  }

  /**
   * Returns annotations applicable to type {@code widenedTypeKind}, that are copied or adapted from
   * {@code annos}.
   *
   * @param annos annotations to widen, from a primitive or boxed primitive
   * @param typeKind primitive type to widen
   * @param widenedTypeKind target for the returned annotations; a primitive type that is wider than
   *     {@code typeKind} (in the sense of JLS 5.1.2)
   * @return result of converting {@code annos} from {@code typeKind} to {@code widenedTypeKind}
   */
  public AnnotationMirrorSet getWidenedAnnotations(
      AnnotationMirrorSet annos, TypeKind typeKind, TypeKind widenedTypeKind) {
    return annos;
  }

  /**
   * Returns the types of the two arguments to the BinaryTree, accounting for widening and unboxing
   * if applicable.
   *
   * @param tree a binary tree
   * @return the types of the two arguments
   */
  public IPair<AnnotatedTypeMirror, AnnotatedTypeMirror> binaryTreeArgTypes(BinaryTree tree) {
    return binaryTreeArgTypes(
        getAnnotatedType(tree.getLeftOperand()), getAnnotatedType(tree.getRightOperand()));
  }

  /**
   * Returns the types of the two arguments to the CompoundAssignmentTree, accounting for widening
   * and unboxing if applicable.
   *
   * @param tree a compound assignment tree
   * @return the types of the two arguments
   */
  public IPair<AnnotatedTypeMirror, AnnotatedTypeMirror> compoundAssignmentTreeArgTypes(
      CompoundAssignmentTree tree) {
    return binaryTreeArgTypes(
        getAnnotatedType(tree.getVariable()), getAnnotatedType(tree.getExpression()));
  }

  /**
   * Returns the types of the two arguments to a binary operation, accounting for widening and
   * unboxing if applicable.
   *
   * @param left the type of the left argument of a binary operation
   * @param right the type of the right argument of a binary operation
   * @return the types of the two arguments
   */
  public IPair<AnnotatedTypeMirror, AnnotatedTypeMirror> binaryTreeArgTypes(
      AnnotatedTypeMirror left, AnnotatedTypeMirror right) {
    TypeKind resultTypeKind =
        TypeKindUtils.widenedNumericType(left.getUnderlyingType(), right.getUnderlyingType());
    if (TypeKindUtils.isNumeric(resultTypeKind)) {
      TypeMirror resultTypeMirror = types.getPrimitiveType(resultTypeKind);
      AnnotatedPrimitiveType leftUnboxed = applyUnboxing(left);
      AnnotatedPrimitiveType rightUnboxed = applyUnboxing(right);
      AnnotatedPrimitiveType leftWidened =
          (leftUnboxed.getKind() == resultTypeKind
              ? leftUnboxed
              : getWidenedPrimitive(leftUnboxed, resultTypeMirror));
      AnnotatedPrimitiveType rightWidened =
          (rightUnboxed.getKind() == resultTypeKind
              ? rightUnboxed
              : getWidenedPrimitive(rightUnboxed, resultTypeMirror));
      return IPair.of(leftWidened, rightWidened);
    } else {
      return IPair.of(left, right);
    }
  }

  /**
   * Returns AnnotatedPrimitiveType with underlying type {@code narrowedTypeMirror} and with
   * annotations copied or adapted from {@code type}.
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
            AnnotatedTypeMirror.createType(narrowedTypeMirror, this, type.isDeclaration());
    narrowed.addAnnotations(type.getPrimaryAnnotations());
    return narrowed;
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
   * Returns true if the given annotation is a part of the type system under which this type factory
   * operates. Null is never a supported qualifier; the parameter is nullable to allow the result of
   * canonicalAnnotation to be passed in directly.
   *
   * @param a any annotation
   * @return true if that annotation is part of the type system under which this type factory
   *     operates, false otherwise
   */
  @EnsuresNonNullIf(expression = "#1", result = true)
  public boolean isSupportedQualifier(@Nullable AnnotationMirror a) {
    if (a == null) {
      return false;
    }
    return isSupportedQualifier(AnnotationUtils.annotationName(a));
  }

  /**
   * Returns true if the given class is a part of the type system under which this type factory
   * operates.
   *
   * @param clazz annotation class
   * @return true if that class is a type qualifier in the type system under which this type factory
   *     operates, false otherwise
   */
  public boolean isSupportedQualifier(Class<? extends Annotation> clazz) {
    return getSupportedTypeQualifiers().contains(clazz);
  }

  /**
   * Returns true if the given class name is a part of the type system under which this type factory
   * operates.
   *
   * @param className fully-qualified annotation class name
   * @return true if that class name is a type qualifier in the type system under which this type
   *     factory operates, false otherwise
   */
  public boolean isSupportedQualifier(String className) {
    return getSupportedTypeQualifierNames().contains(className);
  }

  /**
   * Adds the annotation {@code aliasClass} as an alias for the canonical annotation {@code
   * canonicalAnno} that will be used by the Checker Framework in the alias's place.
   *
   * <p>By specifying the alias/canonical relationship using this method, the elements of the alias
   * are not preserved when the canonical annotation to use is constructed from the alias. If you
   * want the elements to be copied over as well, use {@link #addAliasedTypeAnnotation(Class, Class,
   * boolean, String...)}.
   *
   * @param aliasClass the class of the aliased annotation
   * @param canonicalAnno the canonical annotation
   */
  protected void addAliasedTypeAnnotation(Class<?> aliasClass, AnnotationMirror canonicalAnno) {
    if (getSupportedTypeQualifiers().contains(aliasClass)) {
      throw new BugInCF(
          "AnnotatedTypeFactory: alias %s should not be in type hierarchy for %s",
          aliasClass, this.getClass().getSimpleName());
    }
    addAliasedTypeAnnotation(aliasClass.getCanonicalName(), canonicalAnno);
  }

  /**
   * Adds the annotation, whose fully-qualified name is given by {@code aliasName}, as an alias for
   * the canonical annotation {@code canonicalAnno} that will be used by the Checker Framework in
   * the alias's place.
   *
   * <p>Use this method if the alias class is not necessarily on the classpath at Checker Framework
   * compile and run time. Otherwise, use {@link #addAliasedTypeAnnotation(Class, AnnotationMirror)}
   * which prevents the possibility of a typo in the class name.
   *
   * @param aliasName the canonical name of the aliased annotation
   * @param canonicalAnno the canonical annotation
   */
  // aliasName is annotated as @FullyQualifiedName because there is no way to confirm that the
  // name of an external annotation is a canoncal name.
  protected void addAliasedTypeAnnotation(
      @FullyQualifiedName String aliasName, AnnotationMirror canonicalAnno) {

    aliases.put(aliasName, new Alias(aliasName, canonicalAnno, false, null, null));
  }

  /**
   * Adds the annotation {@code aliasClass} as an alias for the canonical annotation {@code
   * canonicalClass} that will be used by the Checker Framework in the alias's place.
   *
   * <p>You may specify the copyElements flag to indicate whether you want the elements of the alias
   * to be copied over when the canonical annotation is constructed as a copy of {@code
   * canonicalClass}. Be careful that the framework will try to copy the elements by name matching,
   * so make sure that names and types of the elements to be copied over are exactly the same as the
   * ones in the canonical annotation. Otherwise, an 'Couldn't find element in annotation' error is
   * raised.
   *
   * <p>To facilitate the cases where some of the elements are ignored on purpose when constructing
   * the canonical annotation, this method also provides a varargs {@code ignorableElements} for you
   * to explicitly specify the ignoring rules. For example, {@code
   * org.checkerframework.checker.index.qual.IndexFor} is an alias of {@code
   * org.checkerframework.checker.index.qual.NonNegative}, but the element "value" of
   * {@code @IndexFor} should be ignored when constructing {@code @NonNegative}. In the cases where
   * all elements are ignored, we can simply use {@link #addAliasedTypeAnnotation(Class,
   * AnnotationMirror)} instead.
   *
   * @param aliasClass the class of the aliased annotation
   * @param canonicalClass the class of the canonical annotation
   * @param copyElements a flag that indicates whether you want to copy the elements over when
   *     getting the alias from the canonical annotation
   * @param ignorableElements a list of elements that can be safely dropped when the elements are
   *     being copied over
   */
  protected void addAliasedTypeAnnotation(
      Class<?> aliasClass,
      Class<?> canonicalClass,
      boolean copyElements,
      String... ignorableElements) {
    if (getSupportedTypeQualifiers().contains(aliasClass)) {
      throw new BugInCF(
          "AnnotatedTypeFactory: alias %s should not be in type hierarchy for %s",
          aliasClass, this.getClass().getSimpleName());
    }
    addAliasedTypeAnnotation(
        aliasClass.getCanonicalName(), canonicalClass, copyElements, ignorableElements);
  }

  /**
   * Adds the annotation, whose fully-qualified name is given by {@code aliasName}, as an alias for
   * the canonical annotation {@code canonicalAnno} that will be used by the Checker Framework in
   * the alias's place.
   *
   * <p>Use this method if the alias class is not necessarily on the classpath at Checker Framework
   * compile and run time. Otherwise, use {@link #addAliasedTypeAnnotation(Class, Class, boolean,
   * String[])} which prevents the possibility of a typo in the class name.
   *
   * @param aliasName the canonical name of the aliased class
   * @param canonicalAnno the canonical annotation
   * @param copyElements a flag that indicates whether we want to copy the elements over when
   *     getting the alias from the canonical annotation
   * @param ignorableElements a list of elements that can be safely dropped when the elements are
   *     being copied over
   */
  // aliasName is annotated as @FullyQualifiedName because there is no way to confirm that the
  // name of an external annotation is a canoncal name.
  protected void addAliasedTypeAnnotation(
      @FullyQualifiedName String aliasName,
      Class<?> canonicalAnno,
      boolean copyElements,
      String... ignorableElements) {
    // The copyElements argument disambiguates overloading.
    if (!copyElements) {
      throw new BugInCF("Do not call with false");
    }
    aliases.put(
        aliasName,
        new Alias(
            aliasName, null, copyElements, canonicalAnno.getCanonicalName(), ignorableElements));
  }

  /**
   * Returns the canonical annotation for the passed annotation. Returns null if the passed
   * annotation is not an alias of a canonical one in the framework.
   *
   * <p>A canonical annotation is the internal annotation that will be used by the Checker Framework
   * in the aliased annotation's place.
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
   * Add the annotation {@code alias} as an alias for the declaration annotation {@code annotation},
   * where the annotation mirror {@code annotationToUse} will be used instead. If multiple calls are
   * made with the same {@code annotation}, then the {@code annotationToUse} must be the same.
   *
   * <p>The point of {@code annotationToUse} is that it may include elements/fields.
   *
   * @param alias the class of the alias annotation
   * @param annotationClass the class of the canonical annotation
   * @param annotationToUse the annotation mirror to use
   */
  protected void addAliasedDeclAnnotation(
      Class<? extends Annotation> alias,
      Class<? extends Annotation> annotationClass,
      AnnotationMirror annotationToUse) {
    IPair<AnnotationMirror, Set<Class<? extends Annotation>>> pair =
        declAliases.get(annotationClass);
    if (pair != null) {
      if (!AnnotationUtils.areSame(annotationToUse, pair.first)) {
        throw new BugInCF("annotationToUse should be the same: %s %s", pair.first, annotationToUse);
      }
    } else {
      pair = IPair.of(annotationToUse, new HashSet<>());
      declAliases.put(annotationClass, pair);
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
   * A convenience method that converts a {@link TypeMirror} to an empty {@link AnnotatedTypeMirror}
   * using {@link AnnotatedTypeMirror#createType}.
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
   * @param tree the tree to analyze
   * @return the type of {@code tree}, without any annotations
   */
  protected final AnnotatedTypeMirror type(Tree tree) {
    boolean isDeclaration = TreeUtils.isClassTree(tree);

    // Attempt to obtain the type via JCTree.
    if (TreeUtils.typeOf(tree) != null) {
      AnnotatedTypeMirror result = toAnnotatedType(TreeUtils.typeOf(tree), isDeclaration);
      return result;
    }

    // Attempt to obtain the type via TreePath (slower).
    TreePath path = this.getPath(tree);
    assert path != null
        : "No path or type in tree: " + tree + " [" + tree.getClass().getSimpleName() + "]";

    TypeMirror t = trees.getTypeMirror(path);
    assert validType(t) : "Invalid type " + t + " for tree " + t;

    AnnotatedTypeMirror result = toAnnotatedType(t, isDeclaration);
    return result;
  }

  /**
   * Gets the declaration tree for the element, if the source is available.
   *
   * <p>TODO: would be nice to move this to InternalUtils/TreeUtils.
   *
   * @param elt an element
   * @return the tree declaration of the element if found
   */
  public final @Nullable Tree declarationFromElement(Element elt) {
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
    // Prevent calling declarationFor on elements we know we don't have the tree for.

    switch (ElementUtils.getKindRecordAsClass(elt)) {
      case CLASS: // Including RECORD
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
                (com.sun.tools.javac.code.Symbol) elt, (com.sun.tools.javac.tree.JCTree) root);
        break;
    }
    if (shouldCache) {
      elementToTreeCache.put(elt, fromElt);
    }
    return fromElt;
  }

  /**
   * Returns true if {@code tree} is within a constructor.
   *
   * @param tree the tree that might be within a constructor
   * @return true if {@code tree} is within a constructor
   */
  protected final boolean isWithinConstructor(Tree tree) {
    MethodTree enclosingMethod = TreePathUtil.enclosingMethod(getPath(tree));
    return enclosingMethod != null && TreeUtils.isConstructor(enclosingMethod);
  }

  /**
   * Sets the path to the tree that an external "visitor" is visiting. The visitor is either a
   * subclass of {@link BaseTypeVisitor} or {@link
   * org.checkerframework.framework.flow.CFAbstractTransfer}.
   *
   * @param visitorTreePath path to the current tree that an external "visitor" is visiting
   */
  public void setVisitorTreePath(@Nullable TreePath visitorTreePath) {
    this.visitorTreePath = visitorTreePath;
  }

  /**
   * Returns the path to the tree that an external "visitor" is visiting. The type factory does not
   * update this value as it computes the types of any tree or element needed compute the type of
   * the tree being visited. Therefore this path may not be the path to the tree whose type is being
   * computed. This method should not be used directly. Use {@link #getPath(Tree)} instead.
   *
   * <p>This method is used to save the previous tree path and to give a hint to {@link
   * #getPath(Tree)} on where to look for a tree rather than searching starting at the root.
   *
   * @return the path to the tree that an external "visitor" is visiting
   */
  public @Nullable TreePath getVisitorTreePath() {
    return visitorTreePath;
  }

  /**
   * Gets the path for the given {@link Tree} under the current root by checking from the visitor's
   * current path, and using {@link Trees#getPath(CompilationUnitTree, Tree)} (which is much slower)
   * only if {@code tree} is not found on the current path.
   *
   * <p>Note that the given Tree has to be within the current compilation unit, otherwise null will
   * be returned.
   *
   * <p>Within a subclass of BaseTypeVisitor, use {@code getCurrentPath()} rather than this method.
   *
   * @param tree the {@link Tree} to get the path for
   * @return the path for {@code tree} under the current root. Returns null if {@code tree} is not
   *     within the current compilation unit.
   */
  public final @Nullable TreePath getPath(@FindDistinct Tree tree) {
    assert root != null
        : "AnnotatedTypeFactory.getPath("
            + tree.getKind()
            + "): root needs to be set when used on trees; factory: "
            + this.getClass().getSimpleName();

    if (tree == null) {
      return null;
    }

    if (artificialTreeToEnclosingElementMap.containsKey(tree)) {
      return null;
    }

    if (treePathCache.isCached(tree)) {
      return treePathCache.getPath(root, tree);
    }

    TreePath currentPath = visitorTreePath;
    if (currentPath == null) {
      TreePath path = TreePath.getPath(root, tree);
      treePathCache.addPath(tree, path);
      return path;
    }

    // This method uses multiple heuristics to avoid calling
    // TreePath.getPath()

    // If the current path you are visiting is for this tree we are done
    if (currentPath.getLeaf() == tree) {
      treePathCache.addPath(tree, currentPath);
      return currentPath;
    }

    // When running on Daikon, we noticed that a lot of calls happened
    // within a small subtree containing the tree we are currently visiting

    // When testing on Daikon, two steps resulted in the best performance
    if (currentPath.getParentPath() != null) {
      currentPath = currentPath.getParentPath();
      treePathCache.addPath(currentPath.getLeaf(), currentPath);
      if (currentPath.getLeaf() == tree) {
        return currentPath;
      }
      if (currentPath.getParentPath() != null) {
        currentPath = currentPath.getParentPath();
        treePathCache.addPath(currentPath.getLeaf(), currentPath);
        if (currentPath.getLeaf() == tree) {
          return currentPath;
        }
      }
    }

    TreePath pathWithinSubtree = TreePath.getPath(currentPath, tree);
    if (pathWithinSubtree != null) {
      treePathCache.addPath(tree, pathWithinSubtree);
      return pathWithinSubtree;
    }

    // climb the current path till we see that
    // Works when getPath called on the enclosing method, enclosing class.
    TreePath current = currentPath;
    while (current != null) {
      treePathCache.addPath(current.getLeaf(), current);
      if (current.getLeaf() == tree) {
        return current;
      }
      current = current.getParentPath();
    }

    // OK, we give up. Use the cache to look up.
    return treePathCache.getPath(root, tree);
  }

  /**
   * Gets the {@link Element} representing the declaration of the method enclosing a tree AST node.
   * This feature is used to record the enclosing methods of {@link Tree}s that are created
   * internally by the checker.
   *
   * <p>TODO: Find a better way to store information about enclosing Trees.
   *
   * @param tree the {@link Tree} to get the enclosing method for
   * @return the method {@link Element} enclosing the argument, or null if none has been recorded
   */
  public final @Nullable Element getEnclosingElementForArtificialTree(Tree tree) {
    return artificialTreeToEnclosingElementMap.get(tree);
  }

  /**
   * Adds the given mapping from a synthetic (generated) tree to its enclosing element.
   *
   * <p>See {@code
   * org.checkerframework.framework.flow.CFCFGBuilder.CFCFGTranslationPhaseOne.handleArtificialTree(Tree)}.
   *
   * @param tree artifical tree
   * @param enclosing element that encloses {@code tree}
   */
  public final void setEnclosingElementForArtificialTree(Tree tree, Element enclosing) {
    artificialTreeToEnclosingElementMap.put(tree, enclosing);
  }

  /**
   * Assert that the type is a type of valid type mirror, i.e. not an ERROR or OTHER type.
   *
   * @param type an annotated type
   * @return true if the type is a valid annotated type, false otherwise
   */
  static boolean validAnnotatedType(AnnotatedTypeMirror type) {
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
  private static boolean validType(TypeMirror type) {
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
   * Parses all annotation files in the following order:
   *
   * <ol>
   *   <li>jdk.astub in the same directory as the checker, if it exists and ignorejdkastub option is
   *       not supplied
   *   <li>jdkN.astub (where N is the Java version or any higher value) in the same directory as the
   *       checker, if it exists and ignorejdkastub option is not supplied
   *   <li>Stub files listed in @StubFiles annotation on the checker; must be in same directory as
   *       the checker
   *   <li>Stub files provided via {@code -Astubs} compiler option
   *   <li>Ajava files provided via {@code -Aajava} compiler option
   * </ol>
   *
   * <p>If a type is annotated with a qualifier from the same hierarchy in more than one stub file,
   * the qualifier in the last stub file is applied.
   *
   * <p>The annotations are stored by side-effecting {@link #stubTypes} and {@link #ajavaTypes}.
   */
  protected void parseAnnotationFiles() {
    stubTypes.parseStubFiles();
    ajavaTypes.parseAjavaFiles();
  }

  /**
   * Returns all of the declaration annotations whose name equals the passed annotation class (or is
   * an alias for it) including annotations:
   *
   * <ul>
   *   <li>on the element
   *   <li>written in stubfiles
   *   <li>inherited from overridden methods, (see {@link InheritedAnnotation})
   *   <li>inherited from superclasses or super interfaces (see {@link Inherited})
   * </ul>
   *
   * @see #getDeclAnnotationNoAliases
   * @param elt the element to retrieve the declaration annotation from
   * @param annoClass annotation class
   * @return the annotation mirror for annoClass
   */
  @Override
  public final AnnotationMirror getDeclAnnotation(
      Element elt, Class<? extends Annotation> annoClass) {
    logGat("entering getDeclAnnotation(%s [%s], %s)%n", elt, elt.getKind(), annoClass);
    if (debugGat) {
      if (elt.toString().equals("java.lang.CharSequence")) {
        new Error("stack trace").printStackTrace();
      }
    }
    AnnotationMirror result = getDeclAnnotation(elt, annoClass, true);
    logGat(
        "  exiting getDeclAnnotation(%s [%s], %s) => %s%n", elt, elt.getKind(), annoClass, result);
    return result;
  }

  /**
   * Returns the annotation mirror used to annotate this element, whose name equals the passed
   * annotation class. Looks in the same places specified by {@link #getDeclAnnotation(Element,
   * Class)}. Returns null if none exists. Does not check for aliases of the annotation class.
   *
   * <p>Call this method from a checker that needs to alias annotations for one purpose and not for
   * another. For example, in the Lock Checker, {@code @LockingFree} and {@code @ReleasesNoLocks}
   * are both aliases of {@code @SideEffectFree} since they are all considered side-effect-free with
   * regard to the set of locks held before and after the method call. However, a {@code
   * synchronized} block is permitted inside a {@code @ReleasesNoLocks} method but not inside a
   * {@code @LockingFree} or {@code @SideEffectFree} method.
   *
   * @see #getDeclAnnotation
   * @param elt the element to retrieve the declaration annotation from
   * @param annoClass annotation class
   * @return the annotation mirror for annoClass
   */
  public final @Nullable AnnotationMirror getDeclAnnotationNoAliases(
      Element elt, Class<? extends Annotation> annoClass) {
    return getDeclAnnotation(elt, annoClass, false);
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
   * redundancy in one of the type hierarchies can be ok. Such implementations should return false.
   *
   * @return whether to warn about redundancy between a stub file and bytecode
   */
  public boolean shouldWarnIfStubRedundantWithBytecode() {
    return true;
  }

  /**
   * Returns the actual annotation mirror used to annotate this element, whose name equals the
   * passed annotation class (or is an alias for it). Looks in the same places specified by {@link
   * #getDeclAnnotation(Element, Class)}. Returns null if none exists. May return the canonical
   * annotation that annotationName is an alias for.
   *
   * <p>This is the private implementation of the same-named, public method.
   *
   * <p>An option is provided to not check for aliases of annotations. For example, an annotated
   * type factory may use aliasing for a pair of annotations for convenience while needing in some
   * cases to determine a strict ordering between them, such as when determining whether the
   * annotations on an overrider method are more specific than the annotations of an overridden
   * method.
   *
   * @param elt the element to retrieve the annotation from
   * @param annoClass the class of the annotation to retrieve
   * @param checkAliases whether to return an annotation mirror for an alias of the requested
   *     annotation class name
   * @return the annotation mirror for the requested annotation, or null if not found
   */
  private @Nullable AnnotationMirror getDeclAnnotation(
      Element elt, Class<? extends Annotation> annoClass, boolean checkAliases) {
    AnnotationMirrorSet declAnnos = getDeclAnnotations(elt);

    for (AnnotationMirror am : declAnnos) {
      if (areSameByClass(am, annoClass)) {
        return am;
      }
    }
    if (!checkAliases) {
      return null;
    }
    // Look through aliases.
    IPair<AnnotationMirror, Set<Class<? extends Annotation>>> aliases = declAliases.get(annoClass);
    if (aliases == null) {
      return null;
    }
    for (Class<? extends Annotation> alias : aliases.second) {
      for (AnnotationMirror am : declAnnos) {
        if (areSameByClass(am, alias)) {
          // TODO: need to copy over elements/fields
          return aliases.first;
        }
      }
    }

    // Not found.
    return null;
  }

  /**
   * Returns all of the declaration annotations on this element including annotations:
   *
   * <ul>
   *   <li>on the element
   *   <li>written in stubfiles
   *   <li>inherited from overridden methods, (see {@link InheritedAnnotation})
   *   <li>inherited from superclasses or super interfaces (see {@link Inherited})
   * </ul>
   *
   * <p>This method returns the actual annotations not their aliases. {@link
   * #getDeclAnnotation(Element, Class)} returns aliases.
   *
   * @param elt the element for which to determine annotations
   * @return all of the declaration annotations on this element, written in stub files, or inherited
   */
  public AnnotationMirrorSet getDeclAnnotations(Element elt) {
    AnnotationMirrorSet cachedValue = cacheDeclAnnos.get(elt);
    if (cachedValue != null) {
      // Found in cache, return result.
      return cachedValue;
    }

    AnnotationMirrorSet results = new AnnotationMirrorSet();
    // Retrieving the annotations from the element.
    // This includes annotations inherited from superclasses, but not superinterfaces or
    // overridden methods.
    List<? extends AnnotationMirror> fromEle = elements.getAllAnnotationMirrors(elt);
    for (AnnotationMirror annotation : fromEle) {
      try {
        results.add(annotation);
      } catch (com.sun.tools.javac.code.Symbol.CompletionFailure cf) {
        // If a CompletionFailure occurs, issue a warning.
        checker.reportWarning(
            annotation.getAnnotationType().asElement(),
            "annotation.not.completed",
            ElementUtils.getQualifiedName(elt),
            annotation);
      }
    }

    // If parsing annotation files, return only the annotations in the element.
    if (stubTypes.isParsing()
        || ajavaTypes.isParsing()
        || (currentFileAjavaTypes != null && currentFileAjavaTypes.isParsing())) {
      return results;
    }

    // Add annotations from annotation files.
    results.addAll(stubTypes.getDeclAnnotations(elt));
    results.addAll(ajavaTypes.getDeclAnnotations(elt));
    if (currentFileAjavaTypes != null) {
      results.addAll(currentFileAjavaTypes.getDeclAnnotations(elt));
    }

    if (elt.getKind() == ElementKind.METHOD) {
      // Retrieve the annotations from the overridden method's element.
      inheritOverriddenDeclAnnos((ExecutableElement) elt, results);
    } else if (ElementUtils.isTypeDeclaration(elt)) {
      inheritOverriddenDeclAnnosFromTypeDecl(elt.asType(), results);
    }

    // Add the element and its annotations to the cache.
    cacheDeclAnnos.put(elt, results);
    return results;
  }

  /**
   * Adds into {@code results} the inherited declaration annotations found in all elements of the
   * super types of {@code typeMirror}. (Both superclasses and superinterfaces.)
   *
   * @param typeMirror type
   * @param results the set of AnnotationMirrors to which this method adds declarations annotations
   */
  private void inheritOverriddenDeclAnnosFromTypeDecl(
      TypeMirror typeMirror, AnnotationMirrorSet results) {
    List<? extends TypeMirror> superTypes = types.directSupertypes(typeMirror);
    for (TypeMirror superType : superTypes) {
      TypeElement elt = TypesUtils.getTypeElement(superType);
      if (elt == null) {
        continue;
      }
      AnnotationMirrorSet superAnnos = getDeclAnnotations(elt);
      for (AnnotationMirror annotation : superAnnos) {
        List<? extends AnnotationMirror> annotationsOnAnnotation;
        try {
          annotationsOnAnnotation =
              annotation.getAnnotationType().asElement().getAnnotationMirrors();
        } catch (com.sun.tools.javac.code.Symbol.CompletionFailure cf) {
          // Fix for Issue 348: If a CompletionFailure occurs, issue a warning.
          checker.reportWarning(
              annotation.getAnnotationType().asElement(),
              "annotation.not.completed",
              ElementUtils.getQualifiedName(elt),
              annotation);
          continue;
        }
        if (containsSameByClass(annotationsOnAnnotation, Inherited.class)
            || AnnotationUtils.containsSameByName(inheritedAnnotations, annotation)) {
          addOrMerge(results, annotation);
        }
      }
    }
  }

  /**
   * Adds into {@code results} the declaration annotations found in all elements that the method
   * element {@code elt} overrides.
   *
   * @param elt method element
   * @param results {@code elt} local declaration annotations. The ones found in stub files and in
   *     the element itself.
   */
  private void inheritOverriddenDeclAnnos(ExecutableElement elt, AnnotationMirrorSet results) {
    Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
        AnnotatedTypes.overriddenMethods(elements, this, elt);

    if (overriddenMethods != null) {
      for (ExecutableElement superElt : overriddenMethods.values()) {
        AnnotationMirrorSet superAnnos = getDeclAnnotations(superElt);

        for (AnnotationMirror annotation : superAnnos) {
          List<? extends AnnotationMirror> annotationsOnAnnotation;
          try {
            annotationsOnAnnotation =
                annotation.getAnnotationType().asElement().getAnnotationMirrors();
          } catch (com.sun.tools.javac.code.Symbol.CompletionFailure cf) {
            // Fix for Issue 348: If a CompletionFailure occurs, issue a warning.
            checker.reportWarning(
                annotation.getAnnotationType().asElement(),
                "annotation.not.completed",
                ElementUtils.getQualifiedName(elt),
                annotation);
            continue;
          }
          if (containsSameByClass(annotationsOnAnnotation, InheritedAnnotation.class)
              || AnnotationUtils.containsSameByName(inheritedAnnotations, annotation)) {
            addOrMerge(results, annotation);
          }
        }
      }
    }
  }

  private void addOrMerge(AnnotationMirrorSet results, AnnotationMirror annotation) {
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
   * Returns a list of all declaration annotations used to annotate the element, which have a
   * meta-annotation (i.e., an annotation on that annotation) with class {@code
   * metaAnnotationClass}.
   *
   * @param element the element for which to determine annotations
   * @param metaAnnotationClass the class of the meta-annotation that needs to be present
   * @return a list of pairs {@code (anno, metaAnno)} where {@code anno} is the annotation mirror at
   *     {@code element}, and {@code metaAnno} is the annotation mirror (of type {@code
   *     metaAnnotationClass}) used to meta-annotate the declaration of {@code anno}
   */
  public List<IPair<AnnotationMirror, AnnotationMirror>> getDeclAnnotationWithMetaAnnotation(
      Element element, Class<? extends Annotation> metaAnnotationClass) {
    List<IPair<AnnotationMirror, AnnotationMirror>> result = new ArrayList<>();
    AnnotationMirrorSet annotationMirrors = getDeclAnnotations(element);

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
            ElementUtils.getQualifiedName(element),
            candidate);
        continue;
      }
      // First call copier, if exception, continue normal modula laws.
      for (AnnotationMirror ma : metaAnnotationsOnAnnotation) {
        if (areSameByClass(ma, metaAnnotationClass)) {
          // This candidate has the right kind of meta-annotation.
          // It might be a real contract, or a list of contracts.
          if (isListForRepeatedAnnotation(candidate)) {
            @SuppressWarnings("deprecation") // concrete annotation class is not known
            List<AnnotationMirror> wrappedCandidates =
                AnnotationUtils.getElementValueArray(
                    candidate, "value", AnnotationMirror.class, false);
            for (AnnotationMirror wrappedCandidate : wrappedCandidates) {
              result.add(IPair.of(wrappedCandidate, ma));
            }
          } else {
            result.add(IPair.of(candidate, ma));
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
   * @return a list of pairs {@code (anno, metaAnno)} where {@code anno} is the annotation mirror at
   *     {@code element}, and {@code metaAnno} is the annotation mirror used to annotate {@code
   *     anno}.
   */
  public List<IPair<AnnotationMirror, AnnotationMirror>> getAnnotationWithMetaAnnotation(
      Element element, Class<? extends Annotation> metaAnnotationClass) {

    AnnotationMirrorSet annotationMirrors = new AnnotationMirrorSet();
    // Consider real annotations.
    annotationMirrors.addAll(getAnnotatedType(element).getPrimaryAnnotations());
    // Consider declaration annotations
    annotationMirrors.addAll(getDeclAnnotations(element));

    List<IPair<AnnotationMirror, AnnotationMirror>> result = new ArrayList<>();

    // Go through all annotations found.
    for (AnnotationMirror annotation : annotationMirrors) {
      List<? extends AnnotationMirror> annotationsOnAnnotation =
          annotation.getAnnotationType().asElement().getAnnotationMirrors();
      for (AnnotationMirror a : annotationsOnAnnotation) {
        if (areSameByClass(a, metaAnnotationClass)) {
          result.add(IPair.of(annotation, a));
        }
      }
    }
    return result;
  }

  /**
   * Whether or not the {@code annotatedTypeMirror} has a qualifier parameter.
   *
   * @param annotatedTypeMirror the type to check
   * @param top the top of the hierarchy to check
   * @return true if the type has a qualifier parameter
   */
  public boolean hasQualifierParameterInHierarchy(
      AnnotatedTypeMirror annotatedTypeMirror, AnnotationMirror top) {
    return AnnotationUtils.containsSame(getQualifierParameterHierarchies(annotatedTypeMirror), top);
  }

  /**
   * Whether or not the {@code element} has a qualifier parameter.
   *
   * @param element element to check
   * @param top the top of the hierarchy to check
   * @return true if the type has a qualifier parameter
   */
  public boolean hasQualifierParameterInHierarchy(@Nullable Element element, AnnotationMirror top) {
    if (element == null) {
      return false;
    }
    return AnnotationUtils.containsSame(getQualifierParameterHierarchies(element), top);
  }

  /**
   * Returns whether the {@code HasQualifierParameter} annotation was explicitly written on {@code
   * element} for the hierarchy given by {@code top}.
   *
   * @param element the Element to check
   * @param top the top qualifier for the hierarchy to check
   * @return whether the class given by {@code element} has been explicitly annotated with {@code
   *     HasQualifierParameter} for the given hierarchy
   */
  public boolean hasExplicitQualifierParameterInHierarchy(Element element, AnnotationMirror top) {
    return AnnotationUtils.containsSame(
        getSupportedAnnotationsInElementAnnotation(
            element, HasQualifierParameter.class, hasQualifierParameterValueElement),
        top);
  }

  /**
   * Returns whether the {@code NoQualifierParameter} annotation was explicitly written on {@code
   * element} for the hierarchy given by {@code top}.
   *
   * @param element the Element to check
   * @param top the top qualifier for the hierarchy to check
   * @return whether the class given by {@code element} has been explicitly annotated with {@code
   *     NoQualifierParameter} for the given hierarchy
   */
  public boolean hasExplicitNoQualifierParameterInHierarchy(Element element, AnnotationMirror top) {
    return AnnotationUtils.containsSame(
        getSupportedAnnotationsInElementAnnotation(
            element, NoQualifierParameter.class, noQualifierParameterValueElement),
        top);
  }

  /**
   * Returns the set of top annotations representing all the hierarchies for which this type has a
   * qualifier parameter.
   *
   * @param annotatedType the type to check
   * @return the set of top annotations representing all the hierarchies for which this type has a
   *     qualifier parameter
   */
  public AnnotationMirrorSet getQualifierParameterHierarchies(AnnotatedTypeMirror annotatedType) {
    while (annotatedType.getKind() == TypeKind.TYPEVAR
        || annotatedType.getKind() == TypeKind.WILDCARD) {
      if (annotatedType.getKind() == TypeKind.TYPEVAR) {
        annotatedType = ((AnnotatedTypeVariable) annotatedType).getUpperBound();
      } else if (annotatedType.getKind() == TypeKind.WILDCARD) {
        annotatedType = ((AnnotatedWildcardType) annotatedType).getSuperBound();
      }
    }

    if (annotatedType.getKind() != TypeKind.DECLARED) {
      return AnnotationMirrorSet.emptySet();
    }

    AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) annotatedType;
    Element element = declaredType.getUnderlyingType().asElement();
    if (element == null) {
      return AnnotationMirrorSet.emptySet();
    }
    return getQualifierParameterHierarchies(element);
  }

  /**
   * Returns the set of top annotations representing all the hierarchies for which this element has
   * a qualifier parameter.
   *
   * @param element the Element to check
   * @return the set of top annotations representing all the hierarchies for which this element has
   *     a qualifier parameter
   */
  public AnnotationMirrorSet getQualifierParameterHierarchies(Element element) {
    if (!ElementUtils.isTypeDeclaration(element)) {
      return AnnotationMirrorSet.emptySet();
    }

    AnnotationMirrorSet found = new AnnotationMirrorSet();
    found.addAll(
        getSupportedAnnotationsInElementAnnotation(
            element, HasQualifierParameter.class, hasQualifierParameterValueElement));
    AnnotationMirrorSet hasQualifierParameterTops = new AnnotationMirrorSet();
    PackageElement packageElement = ElementUtils.enclosingPackage(element);

    // Traverse all packages containing this element.
    while (packageElement != null) {
      AnnotationMirrorSet packageDefaultTops =
          getSupportedAnnotationsInElementAnnotation(
              packageElement, HasQualifierParameter.class, hasQualifierParameterValueElement);
      hasQualifierParameterTops.addAll(packageDefaultTops);

      packageElement = ElementUtils.parentPackage(packageElement, elements);
    }

    AnnotationMirrorSet noQualifierParamClasses =
        getSupportedAnnotationsInElementAnnotation(
            element, NoQualifierParameter.class, noQualifierParameterValueElement);
    for (AnnotationMirror anno : hasQualifierParameterTops) {
      if (!AnnotationUtils.containsSame(noQualifierParamClasses, anno)) {
        found.add(anno);
      }
    }

    return found;
  }

  /**
   * Returns a set of supported annotation mirrors corresponding to the annotation classes listed in
   * the value element of an annotation with class {@code annoClass} on {@code element}.
   *
   * @param element the Element to check
   * @param annoClass the class for an annotation that's written on elements, whose value element is
   *     a list of annotation classes. It is always HasQualifierParameter or NoQualifierParameter
   * @param valueElement the {@code value} field/element of an annotation with class {@code
   *     annoClass}
   * @return the set of supported annotations with classes listed in the value element of an
   *     annotation with class {@code annoClass} on the {@code element}. Returns an empty set if
   *     {@code annoClass} is not written on {@code element} or {@code element} is null.
   */
  private AnnotationMirrorSet getSupportedAnnotationsInElementAnnotation(
      @Nullable Element element,
      Class<? extends Annotation> annoClass,
      ExecutableElement valueElement) {
    if (element == null) {
      return AnnotationMirrorSet.emptySet();
    }
    // TODO: caching
    AnnotationMirror annotation = getDeclAnnotation(element, annoClass);
    if (annotation == null) {
      return AnnotationMirrorSet.emptySet();
    }

    AnnotationMirrorSet found = new AnnotationMirrorSet();
    List<@CanonicalName Name> qualClasses =
        AnnotationUtils.getElementValueClassNames(annotation, valueElement);
    for (Name qual : qualClasses) {
      AnnotationMirror annotationMirror = AnnotationBuilder.fromName(elements, qual);
      if (isSupportedQualifier(annotationMirror)) {
        found.add(annotationMirror);
      }
    }
    return found;
  }

  /**
   * A scanner that replaces annotations in one type with annotations from another. Used by {@link
   * #replaceAnnotations(AnnotatedTypeMirror, AnnotatedTypeMirror)} and {@link
   * #replaceAnnotations(AnnotatedTypeMirror, AnnotatedTypeMirror, AnnotationMirror)}.
   */
  private final AnnotatedTypeReplacer annotatedTypeReplacer = new AnnotatedTypeReplacer();

  /**
   * Replaces or adds all annotations from {@code from} to {@code to}. Annotations from {@code from}
   * will be used everywhere they exist, but annotations in {@code to} will be kept anywhere that
   * {@code from} is unannotated.
   *
   * @param from the annotated type mirror from which to take new annotations
   * @param to the annotated type mirror to which the annotations will be added
   */
  public void replaceAnnotations(AnnotatedTypeMirror from, AnnotatedTypeMirror to) {
    annotatedTypeReplacer.visit(from, to);
  }

  /**
   * Replaces or adds annotations in {@code top}'s hierarchy from {@code from} to {@code to}.
   * Annotations from {@code from} will be used everywhere they exist, but annotations in {@code to}
   * will be kept anywhere that {@code from} is unannotated.
   *
   * @param from the annotated type mirror from which to take new annotations
   * @param to the annotated type mirror to which the annotations will be added
   * @param top the top type of the hierarchy whose annotations will be added
   */
  public void replaceAnnotations(
      AnnotatedTypeMirror from, AnnotatedTypeMirror to, AnnotationMirror top) {
    annotatedTypeReplacer.setTop(top);
    annotatedTypeReplacer.visit(from, to);
    annotatedTypeReplacer.setTop(null);
  }

  /** The implementation of the visitor for #containsCapturedTypes. */
  private final SimpleAnnotatedTypeScanner<Boolean, Void> containsCapturedTypes =
      new SimpleAnnotatedTypeScanner<>(
          (type, p) -> TypesUtils.isCapturedTypeVariable(type.getUnderlyingType()),
          Boolean::logicalOr,
          false);

  /**
   * Returns true if {@code type} contains any captured type variables.
   *
   * @param type type to check
   * @return true if {@code type} contains any captured type variables
   */
  public boolean containsCapturedTypes(AnnotatedTypeMirror type) {
    return containsCapturedTypes.visit(type);
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
  public IPair<AnnotatedTypeMirror, AnnotatedExecutableType> getFnInterfaceFromTree(Tree tree) {

    // Functional interface
    // This is the target type of `tree`.
    AnnotatedTypeMirror functionalInterfaceType = getFunctionalInterfaceType(tree);
    if (functionalInterfaceType.getKind() == TypeKind.DECLARED) {
      functionalInterfaceType =
          makeGroundTargetType(
              (AnnotatedDeclaredType) functionalInterfaceType,
              (DeclaredType) TreeUtils.typeOf(tree));
    }

    // Functional method
    ExecutableElement fnElement = TreeUtils.findFunction(tree, processingEnv);

    // Function type
    AnnotatedExecutableType functionType =
        AnnotatedTypes.asMemberOf(types, this, functionalInterfaceType, fnElement);
    return IPair.of(functionalInterfaceType, functionType);
  }

  /**
   * Get the AnnotatedDeclaredType for the FunctionalInterface from assignment context of the method
   * reference or lambda expression which may be a variable assignment, a method call, or a cast.
   *
   * <p>The assignment context is not always correct, so we must search up the AST. It will
   * recursively search for lambdas nested in lambdas.
   *
   * @param tree the tree of the lambda or method reference
   * @return the functional interface type or a type argument from a raw type
   */
  private AnnotatedTypeMirror getFunctionalInterfaceType(Tree tree) {
    TreePath parentPath = getPath(tree).getParentPath();
    Tree parentTree = parentPath.getLeaf();
    switch (parentTree.getKind()) {
      case PARENTHESIZED:
        return getFunctionalInterfaceType(parentTree);

      case TYPE_CAST:
        TypeCastTree cast = (TypeCastTree) parentTree;
        assertIsFunctionalInterface(trees.getTypeMirror(getPath(cast.getType())), parentTree, tree);
        AnnotatedTypeMirror castATM = getAnnotatedType(cast.getType());
        if (castATM.getKind() == TypeKind.INTERSECTION) {
          AnnotatedIntersectionType itype = (AnnotatedIntersectionType) castATM;
          for (AnnotatedTypeMirror t : itype.directSupertypes()) {
            if (TypesUtils.isFunctionalInterface(t.getUnderlyingType(), getProcessingEnv())) {
              return t;
            }
          }
          // We should never reach here: isFunctionalInterface performs the same check
          // and would have raised an error already.
          throw new BugInCF(
              "Expected the type of a cast tree in an assignment context to contain"
                  + " a functional interface bound."
                  + " Found type: %s for tree: %s in lambda tree: %s",
              castATM, cast, tree);
        }
        return castATM;

      case NEW_CLASS:
        NewClassTree newClass = (NewClassTree) parentTree;
        int indexOfLambda = newClass.getArguments().indexOf(tree);
        ParameterizedExecutableType con = this.constructorFromUse(newClass);
        AnnotatedTypeMirror constructorParam =
            AnnotatedTypes.getAnnotatedTypeMirrorOfParameter(con.executableType, indexOfLambda);
        assertIsFunctionalInterface(constructorParam.getUnderlyingType(), parentTree, tree);
        return constructorParam;

      case NEW_ARRAY:
        NewArrayTree newArray = (NewArrayTree) parentTree;
        AnnotatedArrayType newArrayATM = getAnnotatedType(newArray);
        AnnotatedTypeMirror elementATM = newArrayATM.getComponentType();
        assertIsFunctionalInterface(elementATM.getUnderlyingType(), parentTree, tree);
        return elementATM;

      case METHOD_INVOCATION:
        MethodInvocationTree method = (MethodInvocationTree) parentTree;
        int index = method.getArguments().indexOf(tree);
        ParameterizedExecutableType exe = this.methodFromUse(method);
        AnnotatedTypeMirror param =
            AnnotatedTypes.getAnnotatedTypeMirrorOfParameter(exe.executableType, index);
        assertIsFunctionalInterface(param.getUnderlyingType(), parentTree, tree);
        return param;

      case VARIABLE:
        VariableTree varTree = (VariableTree) parentTree;
        assertIsFunctionalInterface(TreeUtils.typeOf(varTree), parentTree, tree);
        return getAnnotatedTypeFromTypeTree(varTree.getType());

      case ASSIGNMENT:
        AssignmentTree assignmentTree = (AssignmentTree) parentTree;
        assertIsFunctionalInterface(TreeUtils.typeOf(assignmentTree), parentTree, tree);
        return getAnnotatedType(assignmentTree.getVariable());

      case RETURN:
        Tree enclosing =
            TreePathUtil.enclosingOfKind(
                getPath(parentTree),
                new HashSet<>(Arrays.asList(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION)));

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
        AnnotatedTypeMirror trueType =
            getAnnotatedType(conditionalExpressionTree.getTrueExpression());
        AnnotatedTypeMirror falseType =
            getAnnotatedType(conditionalExpressionTree.getFalseExpression());

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
        assertIsFunctionalInterface(conditionalType.getUnderlyingType(), parentTree, tree);
        return conditionalType;
      case CASE:
        // Get the functional interface type of the whole switch expression.
        Tree switchTree = parentPath.getParentPath().getLeaf();
        return getFunctionalInterfaceType(switchTree);

      default:
        if (parentTree.getKind().toString().equals("YIELD")) {
          TreePath pathToCase = TreePathUtil.pathTillOfKind(parentPath, Kind.CASE);
          return getFunctionalInterfaceType(pathToCase.getParentPath().getLeaf());
        }
        throw new BugInCF(
            "Could not find functional interface from assignment context. "
                + "Unexpected tree type: "
                + parentTree.getKind()
                + " For lambda tree: "
                + tree);
    }
  }

  /**
   * Throws an exception if the type is not a funtional interface.
   *
   * @param typeMirror a type that must be a funtional interface
   * @param contextTree the tree that has the given type; used only for diagnostic messages
   * @param tree a labmba tree that encloses {@code contextTree}; used only for diagnostic messages
   */
  private void assertIsFunctionalInterface(TypeMirror typeMirror, Tree contextTree, Tree tree) {
    if (typeMirror.getKind() == TypeKind.WILDCARD) {
      // Ignore wildcards, because they are type arguments from raw types.
      return;
    }
    Type type = (Type) typeMirror;
    if (TypesUtils.isFunctionalInterface(type, processingEnv)) {
      return;
    }

    if (type.getKind() == TypeKind.INTERSECTION) {
      IntersectionType itype = (IntersectionType) type;
      for (TypeMirror t : itype.getBounds()) {
        if (TypesUtils.isFunctionalInterface(t, processingEnv)) {
          // As long as any of the bounds is a functional interface, we should be fine.
          return;
        }
      }
    }

    throw new BugInCF(
        "Expected the type of %s tree in assignment context to be a functional interface. "
            + "Found type: %s for tree: %s in lambda tree: %s",
        contextTree.getKind(), type, contextTree, tree);
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
   * @return the grounded functional type
   */
  private AnnotatedDeclaredType makeGroundTargetType(
      AnnotatedDeclaredType functionalType, DeclaredType groundTargetJavaType) {
    if (functionalType.getTypeArguments().isEmpty()) {
      return functionalType;
    }

    List<AnnotatedTypeParameterBounds> bounds =
        this.typeVariablesFromUse(
            functionalType, (TypeElement) functionalType.getUnderlyingType().asElement());

    boolean sizesDiffer =
        functionalType.getTypeArguments().size() != groundTargetJavaType.getTypeArguments().size();

    // This is the declared type of the functional type meaning that the type arguments are the
    // type parameters.
    DeclaredType declaredType =
        (DeclaredType) functionalType.getUnderlyingType().asElement().asType();
    Map<TypeVariable, AnnotatedTypeMirror> typeVarToTypeArg =
        new HashMap<>(functionalType.getTypeArguments().size());
    for (int i = 0; i < functionalType.getTypeArguments().size(); i++) {
      TypeVariable typeVariable = (TypeVariable) declaredType.getTypeArguments().get(i);
      AnnotatedTypeMirror argType = functionalType.getTypeArguments().get(i);
      if (argType.getKind() == TypeKind.WILDCARD) {
        AnnotatedWildcardType wildcardType = (AnnotatedWildcardType) argType;

        TypeMirror wildcardUbType = wildcardType.getExtendsBound().getUnderlyingType();

        if (wildcardType.isTypeArgOfRawType()) {
          // Keep the type arguments from raw types so that it is ignored by later
          // subtyping and containment checks.
          typeVarToTypeArg.put(typeVariable, wildcardType);
        } else if (isExtendsWildcard(wildcardType)) {
          TypeMirror correctArgType;
          if (sizesDiffer) {
            // The Java type is raw.
            TypeMirror typeParamUbType = bounds.get(i).getUpperBound().getUnderlyingType();
            correctArgType =
                TypesUtils.greatestLowerBound(
                    typeParamUbType, wildcardUbType, this.checker.getProcessingEnvironment());
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
                .replaceAnnotations(wildcardType.getExtendsBound().getPrimaryAnnotations());
            newArgAsTypeVar
                .getLowerBound()
                .replaceAnnotations(wildcardType.getSuperBound().getPrimaryAnnotations());
          } else {
            newArg = this.toAnnotatedType(correctArgType, false);
            newArg.replaceAnnotations(wildcardType.getExtendsBound().getPrimaryAnnotations());
          }

          typeVarToTypeArg.put(typeVariable, newArg);
        } else {
          typeVarToTypeArg.put(typeVariable, wildcardType.getSuperBound());
        }
      } else {
        typeVarToTypeArg.put(typeVariable, argType);
      }
    }

    // The ground functional type must be created using type variable substitution or else the
    // underlying type will not match the annotated type.
    AnnotatedDeclaredType groundFunctionalType =
        (AnnotatedDeclaredType)
            AnnotatedTypeMirror.createType(declaredType, this, functionalType.isDeclaration());
    initializeAtm(groundFunctionalType);
    groundFunctionalType =
        (AnnotatedDeclaredType)
            getTypeVarSubstitutor().substitute(typeVarToTypeArg, groundFunctionalType);
    groundFunctionalType.addAnnotations(functionalType.getPrimaryAnnotations());

    // When the groundTargetJavaType is different from the underlying type of functionalType,
    // only the main annotations are copied.  Add default annotations in places without
    // annotations.
    addDefaultAnnotations(groundFunctionalType);
    return groundFunctionalType;
  }

  /**
   * Return true if {@code type} should be captured.
   *
   * <p>{@code type} should be captured if all of the following are true:
   *
   * <ul>
   *   <li>{@code type} and {@code typeMirror} are both declared types.
   *   <li>{@code type} its underlying type is not raw.
   *   <li>{@code type} has a wildcard as a type argument and {@code typeMirror} has a captured type
   *       variable as the corresponding type argument.
   * </ul>
   *
   * @param type annotated type that might need to be captured
   * @param typeMirror the capture of the underlying type of {@code type}
   * @return true if {@code type} should be captured
   */
  private boolean shouldCapture(AnnotatedTypeMirror type, TypeMirror typeMirror) {
    if (type.getKind() != TypeKind.DECLARED || typeMirror.getKind() != TypeKind.DECLARED) {
      return false;
    }

    AnnotatedDeclaredType uncapturedType = (AnnotatedDeclaredType) type;
    DeclaredType capturedTypeMirror = (DeclaredType) typeMirror;
    if (capturedTypeMirror.getTypeArguments().isEmpty()) {
      return false;
    }

    if (uncapturedType.isUnderlyingTypeRaw()) {
      return false;
    }

    for (AnnotatedTypeMirror typeArg : uncapturedType.getTypeArguments()) {
      if (AnnotatedTypes.isTypeArgOfRawType(typeArg)) {
        return false;
      }
    }

    if (capturedTypeMirror.getTypeArguments().size() != uncapturedType.getTypeArguments().size()) {
      throw new BugInCF(
          "Not the same number of type arguments: capturedTypeMirror: %s uncapturedType: %s",
          capturedTypeMirror, uncapturedType);
    }

    for (int i = 0; i < capturedTypeMirror.getTypeArguments().size(); i++) {
      TypeMirror capturedTypeArgTM = capturedTypeMirror.getTypeArguments().get(i);
      AnnotatedTypeMirror uncapturedTypeArg = uncapturedType.getTypeArguments().get(i);
      if (uncapturedTypeArg.getKind() == TypeKind.WILDCARD
          && (TypesUtils.isCapturedTypeVariable(capturedTypeArgTM)
              || capturedTypeArgTM.getKind() != TypeKind.WILDCARD)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Apply capture conversion to {@code typeToCapture}.
   *
   * <p>Capture conversion is the process of converting wildcards in a parameterized type to fresh
   * type variables. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-5.html#jls-5.1.10">JLS 5.1.10</a>
   * for details.
   *
   * <p>If {@code type} is not a declared type or if it does not have any wildcard type arguments,
   * this method returns {@code type}.
   *
   * @param typeToCapture type to capture
   * @return the result of applying capture conversion to {@code typeToCapture}
   */
  public AnnotatedTypeMirror applyCaptureConversion(AnnotatedTypeMirror typeToCapture) {
    TypeMirror capturedTypeMirror = types.capture(typeToCapture.getUnderlyingType());
    return applyCaptureConversion(typeToCapture, capturedTypeMirror);
  }

  /**
   * Apply capture conversion to {@code type}.
   *
   * <p>Capture conversion is the process of converting wildcards in a parameterized type to fresh
   * type variables. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-5.html#jls-5.1.10">JLS 5.1.10</a>
   * for details.
   *
   * <p>If {@code type} is not a declared type or if it does not have any wildcard type arguments,
   * this method returns {@code type}.
   *
   * @param type type to capture
   * @param typeMirror the result of applying capture conversion to the underlying type of {@code
   *     type}; it is used as the underlying type of the returned type
   * @return the result of applying capture conversion to {@code type}
   */
  public AnnotatedTypeMirror applyCaptureConversion(
      AnnotatedTypeMirror type, TypeMirror typeMirror) {

    // If the type contains type arguments of raw types, don't capture, but mark all
    // wildcards that should have been captured as "raw" before it is returned.
    if (typeMirror.getKind() == TypeKind.DECLARED && type.getKind() == TypeKind.DECLARED) {
      boolean fromRawType = false;
      AnnotatedDeclaredType uncapturedType = (AnnotatedDeclaredType) type;
      for (AnnotatedTypeMirror typeArg : uncapturedType.getTypeArguments()) {
        if (AnnotatedTypes.isTypeArgOfRawType(typeArg)) {
          fromRawType = true;
          break;
        }
      }
      if (fromRawType) {
        DeclaredType capturedTypeMirror = (DeclaredType) typeMirror;
        for (int i = 0; i < capturedTypeMirror.getTypeArguments().size(); i++) {
          AnnotatedTypeMirror uncapturedTypeArg = uncapturedType.getTypeArguments().get(i);
          TypeMirror capturedTypeArgTM = capturedTypeMirror.getTypeArguments().get(i);
          if (uncapturedTypeArg.getKind() == TypeKind.WILDCARD
              && (TypesUtils.isCapturedTypeVariable(capturedTypeArgTM)
                  || capturedTypeArgTM.getKind() != TypeKind.WILDCARD)) {
            ((AnnotatedWildcardType) uncapturedTypeArg).setTypeArgOfRawType();
          }
        }
        return type;
      }
    }

    if (!shouldCapture(type, typeMirror)) {
      return type;
    }

    AnnotatedDeclaredType uncapturedType = (AnnotatedDeclaredType) type;
    DeclaredType capturedTypeMirror = (DeclaredType) typeMirror;
    // `capturedType` is the return value of this method.
    AnnotatedDeclaredType capturedType =
        (AnnotatedDeclaredType) AnnotatedTypeMirror.createType(capturedTypeMirror, this, false);

    nonWildcardTypeArgCopier.copy(uncapturedType, capturedType);

    AnnotatedDeclaredType typeDeclaration =
        (AnnotatedDeclaredType) getAnnotatedType(uncapturedType.getUnderlyingType().asElement());

    // A mapping from type variable to its type argument in the captured type.
    Map<TypeVariable, AnnotatedTypeMirror> typeVarToAnnotatedTypeArg = new HashMap<>();
    // A mapping from a captured type variable to the annotated captured type variable.
    Map<TypeVariable, AnnotatedTypeVariable> capturedTypeVarToAnnotatedTypeVar = new HashMap<>();
    // `newTypeArgs` will be the type arguments of the result of this method.
    List<AnnotatedTypeMirror> newTypeArgs = new ArrayList<>();
    for (int i = 0; i < typeDeclaration.getTypeArguments().size(); i++) {
      TypeVariable typeVarTypeMirror =
          (TypeVariable) typeDeclaration.getTypeArguments().get(i).getUnderlyingType();
      AnnotatedTypeMirror uncapturedTypeArg = uncapturedType.getTypeArguments().get(i);
      AnnotatedTypeMirror capturedTypeArg = capturedType.getTypeArguments().get(i);
      if (uncapturedTypeArg.getKind() == TypeKind.WILDCARD) {
        // The type argument is a captured type variable. Use the type argument from the
        // newly created and yet-to-be annotated capturedType. (The annotations are added
        // by #annotateCapturedTypeVar, which is called at the end of this method.)
        typeVarToAnnotatedTypeArg.put(typeVarTypeMirror, capturedTypeArg);
        newTypeArgs.add(capturedTypeArg);
        if (TypesUtils.isCapturedTypeVariable(capturedTypeArg.getUnderlyingType())) {
          // Also, add a mapping from the captured type variable to the annotated captured
          // type variable, so that if one captured type variable refers to another, the
          // same AnnotatedTypeVariable object is used.
          capturedTypeVarToAnnotatedTypeVar.put(
              ((AnnotatedTypeVariable) capturedTypeArg).getUnderlyingType(),
              (AnnotatedTypeVariable) capturedTypeArg);
        } else {
          // Javac used a declared type instead of a captured type variable.  This seems
          // to happen when the bounds of the captured type variable would have been
          // identical. This seems to be a violation of the JLS, but javac does this, so
          // the Checker Framework must handle that case. (See
          // https://bugs.openjdk.org/browse/JDK-8054309.)
          replaceAnnotations(
              ((AnnotatedWildcardType) uncapturedTypeArg).getSuperBound(), capturedTypeArg);
        }
      } else {
        // The type argument is not a wildcard.
        // typeVarTypeMirror is the type parameter for which uncapturedTypeArg is a type
        // argument.
        typeVarToAnnotatedTypeArg.put(typeVarTypeMirror, uncapturedTypeArg);
        if (uncapturedTypeArg.getKind() == TypeKind.TYPEVAR) {
          // If the type arg is a type variable also add it to the
          // typeVarToAnnotatedTypeArg map, so that references to the type variable are
          // substituted.
          AnnotatedTypeVariable typeVar = (AnnotatedTypeVariable) uncapturedTypeArg;
          typeVarToAnnotatedTypeArg.put(typeVar.getUnderlyingType(), typeVar);
        }
        newTypeArgs.add(uncapturedTypeArg);
      }
    }

    // Set the annotations of each captured type variable.
    List<AnnotatedTypeVariable> orderToCapture = order(capturedTypeVarToAnnotatedTypeVar.values());
    for (AnnotatedTypeVariable capturedTypeArg : orderToCapture) {
      int i = capturedTypeMirror.getTypeArguments().indexOf(capturedTypeArg.getUnderlyingType());
      AnnotatedTypeMirror uncapturedTypeArg = uncapturedType.getTypeArguments().get(i);
      AnnotatedTypeVariable typeVariable =
          (AnnotatedTypeVariable) typeDeclaration.getTypeArguments().get(i);
      annotateCapturedTypeVar(
          typeVarToAnnotatedTypeArg,
          capturedTypeVarToAnnotatedTypeVar,
          (AnnotatedWildcardType) uncapturedTypeArg,
          typeVariable,
          capturedTypeArg);
      newTypeArgs.set(i, capturedTypeArg);
    }

    capturedType.setTypeArguments(newTypeArgs);
    capturedType.addAnnotations(uncapturedType.getPrimaryAnnotations());
    return capturedType;
  }

  /**
   * Copy the non-wildcard type args from a uncapturedType to its capturedType. Also, ensure that
   * type variables in capturedType are the same object when they are refer to the same type
   * variable.
   *
   * <p>To use, call {@link NonWildcardTypeArgCopier#copy} rather than a visit method.
   */
  private final NonWildcardTypeArgCopier nonWildcardTypeArgCopier = new NonWildcardTypeArgCopier();

  /**
   * Copy the non-wildcard type args from {@code uncapturedType} to {@code capturedType}. Also,
   * ensure that type variables in {@code capturedType} are the same object when they refer to the
   * same type variable.
   *
   * <p>To use, call {@link NonWildcardTypeArgCopier#copy} rather than a visit method.
   */
  private class NonWildcardTypeArgCopier extends AnnotatedTypeCopier {

    /**
     * Copy the non-wildcard type args from {@code uncapturedType} to {@code capturedType}. Also,
     * ensure that type variables {@code capturedType} are the same object when they are refer to
     * the same type variable.
     *
     * @param uncapturedType a declared type that has not under gone capture conversion
     * @param capturedType the captured version of {@code uncapturedType} before it has been
     *     annotated
     */
    private void copy(AnnotatedDeclaredType uncapturedType, AnnotatedDeclaredType capturedType) {

      // The name "originalToCopy" means a mapping from the original to the copy, not an
      // original that needs to be copied.
      IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy =
          new IdentityHashMap<>();
      originalToCopy.put(uncapturedType, capturedType);
      int numTypeArgs = uncapturedType.getTypeArguments().size();

      AnnotatedTypeMirror[] newTypeArgs = new AnnotatedTypeMirror[numTypeArgs];
      // Mapping from type var to it's AnnotatedTypeVariable.  These are type variables
      // that are type arguments of the uncaptured type.
      Map<TypeVariable, AnnotatedTypeMirror> typeVarToAnnotatedTypeVar = new HashMap<>(numTypeArgs);
      // Copy the non-wildcard type args from uncapturedType to newTypeArgs.
      // If the non-wildcard type arg is a type var, add it to typeVarToAnnotatedTypeVar.
      for (int i = 0; i < numTypeArgs; i++) {
        AnnotatedTypeMirror uncapturedArg = uncapturedType.getTypeArguments().get(i);
        if (uncapturedArg.getKind() != TypeKind.WILDCARD) {
          AnnotatedTypeMirror copyOfArg = visit(uncapturedArg, originalToCopy);
          newTypeArgs[i] = copyOfArg;
          if (copyOfArg.getKind() == TypeKind.TYPEVAR) {
            typeVarToAnnotatedTypeVar.put(
                ((AnnotatedTypeVariable) copyOfArg).getUnderlyingType(), copyOfArg);
          }
        }
      }

      // Substitute the type variables in each type argument of capturedType using
      // typeVarToAnnotatedTypeVar.
      // This makes type variables in capturedType the same object when they are the same type
      // variable.
      for (int i = 0; i < numTypeArgs; i++) {
        AnnotatedTypeMirror uncapturedArg = uncapturedType.getTypeArguments().get(i);
        AnnotatedTypeMirror capturedArg = capturedType.getTypeArguments().get(i);
        // Note: This `if` statement can't be replaced with
        //   if (TypesUtils.isCapturedTypeVariable(capturedArg))
        // because if the bounds of the captured wildcard are equal, then instead of a
        // captured wildcard, the type of the bound is used.
        if (uncapturedArg.getKind() == TypeKind.WILDCARD) {
          AnnotatedTypeMirror newCapArg =
              typeVarSubstitutor.substituteWithoutCopyingTypeArguments(
                  typeVarToAnnotatedTypeVar, capturedArg);
          newTypeArgs[i] = newCapArg;
        }
      }
      // Set capturedType type args to newTypeArgs.
      capturedType.setTypeArguments(Arrays.asList(newTypeArgs));

      // Visit the enclosing type.
      if (uncapturedType.getEnclosingType() != null) {
        capturedType.setEnclosingType(
            (AnnotatedDeclaredType) visit(uncapturedType.getEnclosingType(), originalToCopy));
      }
    }
  }

  /**
   * Returns the list of type variables such that a type variable in the list only references type
   * variables at a lower index than itself.
   *
   * @param collection a collection of type variables
   * @return the type variables ordered so that each type variable only references earlier type
   *     variables
   */
  public List<AnnotatedTypeVariable> order(Collection<AnnotatedTypeVariable> collection) {
    List<AnnotatedTypeVariable> list = new ArrayList<>(collection);
    List<AnnotatedTypeVariable> ordered = new ArrayList<>();
    while (!list.isEmpty()) {
      AnnotatedTypeVariable free = doesNotContainOthers(list);
      list.remove(free);
      ordered.add(free);
    }
    return ordered;
  }

  /**
   * Returns the first TypeVariable in {@code collection} that does not lexically contain any other
   * type in the collection. Or if all the TypeVariables contain another, then it returns the first
   * TypeVariable in {@code collection}.
   *
   * @param collection a collection of type variables
   * @return the first TypeVariable in {@code collection} that does not contain any other type in
   *     the collection, except possibly itself
   */
  @SuppressWarnings("interning:not.interned") // must be the same object from collection
  private AnnotatedTypeVariable doesNotContainOthers(
      Collection<? extends AnnotatedTypeVariable> collection) {
    AnnotatedTypeVariable first = null;
    for (AnnotatedTypeVariable candidate : collection) {
      if (first == null) {
        first = candidate;
      }
      boolean doesNotContain = true;
      for (AnnotatedTypeVariable other : collection) {
        if (candidate != other && captureScanner.visit(candidate, other.getUnderlyingType())) {
          doesNotContain = false;
          break;
        }
      }
      if (doesNotContain) {
        return candidate;
      }
    }
    return first;
  }

  /**
   * Scanner that returns true if the underlying type of any part of an {@link AnnotatedTypeMirror}
   * is the passed captured type variable.
   *
   * <p>The second argument to visit must be a captured type variable.
   */
  @SuppressWarnings("interning:not.interned") // Captured type vars can be compared with ==.
  private final SimpleAnnotatedTypeScanner<Boolean, TypeVariable> captureScanner =
      new SimpleAnnotatedTypeScanner<>(
          (type, other) -> type.getUnderlyingType() == other, Boolean::logicalOr, false);

  /**
   * Set the annotated bounds for fresh type variable {@code capturedTypeVar}, so that it is the
   * capture of {@code wildcard}. Also, sets {@code capturedTypeVar} primary annotation if the
   * annotation on the bounds is identical.
   *
   * @param typeVarToAnnotatedTypeArg mapping from a (type mirror) type variable to its (annotated
   *     type mirror) type argument
   * @param capturedTypeVarToAnnotatedTypeVar mapping from a captured type variable to its {@link
   *     AnnotatedTypeMirror}
   * @param wildcard wildcard which is converted to {@code capturedTypeVar}
   * @param typeVariable type variable for which {@code wildcard} is a type argument
   * @param capturedTypeVar the fresh type variable which is side-effected by this method
   */
  private void annotateCapturedTypeVar(
      Map<TypeVariable, AnnotatedTypeMirror> typeVarToAnnotatedTypeArg,
      Map<TypeVariable, AnnotatedTypeVariable> capturedTypeVarToAnnotatedTypeVar,
      AnnotatedWildcardType wildcard,
      AnnotatedTypeVariable typeVariable,
      AnnotatedTypeVariable capturedTypeVar) {

    AnnotatedTypeMirror typeVarUpperBound =
        typeVarSubstitutor.substituteWithoutCopyingTypeArguments(
            typeVarToAnnotatedTypeArg, typeVariable.getUpperBound());
    AnnotatedTypeMirror upperBound =
        AnnotatedTypes.annotatedGLB(this, typeVarUpperBound, wildcard.getExtendsBound());
    if (upperBound.getKind() == TypeKind.INTERSECTION
        && capturedTypeVar.getUpperBound().getKind() != TypeKind.INTERSECTION) {
      // There is a bug in javac such that the upper bound of the captured type variable is
      // not the greatest lower bound. So the
      // captureTypeVar.getUnderlyingType().getUpperBound() may not
      // be the same type as upperbound.getUnderlyingType().  See
      // framework/tests/all-systems/Issue4890Interfaces.java,
      // framework/tests/all-systems/Issue4890.java and
      // framework/tests/all-systems/Issue4877.java.
      // (I think this is https://bugs.openjdk.org/browse/JDK-8039222.)
      for (AnnotatedTypeMirror bound : ((AnnotatedIntersectionType) upperBound).getBounds()) {
        if (types.isSameType(
            bound.underlyingType, capturedTypeVar.getUpperBound().getUnderlyingType())) {
          upperBound = bound;
        }
      }
    }

    capturedTypeVar.setUpperBound(upperBound);

    // typeVariable's lower bound is a NullType, so there's nothing to substitute.
    AnnotatedTypeMirror lowerBound =
        AnnotatedTypes.leastUpperBound(
            this, typeVariable.getLowerBound(), wildcard.getSuperBound());
    capturedTypeVar.setLowerBound(lowerBound);

    // Add as a primary annotation any qualifiers that are the same on the upper and lower
    // bound.
    AnnotationMirrorSet p =
        new AnnotationMirrorSet(capturedTypeVar.getUpperBound().getPrimaryAnnotations());
    p.retainAll(capturedTypeVar.getLowerBound().getPrimaryAnnotations());
    capturedTypeVar.replaceAnnotations(p);

    capturedTypeVarSubstitutor.substitute(capturedTypeVar, capturedTypeVarToAnnotatedTypeVar);
  }

  /**
   * Substitutes references to captured type variables.
   *
   * <p>Unlike {@link #typeVarSubstitutor}, this class does not copy the type. Call {@code
   * substitute} to use.
   */
  public final CapturedTypeVarSubstitutor capturedTypeVarSubstitutor =
      new CapturedTypeVarSubstitutor();

  /**
   * Substitutes references to captured types in {@code type} using {@code
   * capturedTypeVarToAnnotatedTypeVar}.
   *
   * <p>Unlike {@link #typeVarSubstitutor}, this class does not copy the type. Call {@code
   * substitute} to use.
   */
  public static class CapturedTypeVarSubstitutor extends AnnotatedTypeCopier {

    /** Creates a CapturedTypeVarSubstitutor. */
    public CapturedTypeVarSubstitutor() {}

    /** A mapping from a captured type variable to its AnnotatedTypeVariable. */
    private Map<TypeVariable, AnnotatedTypeVariable> capturedTypeVarToAnnotatedTypeVar;

    /**
     * Substitutes references to captured type variable in {@code type} using {@code
     * capturedTypeVarToAnnotatedTypeVar}.
     *
     * <p>Unlike {@link #typeVarSubstitutor}, this method does not copy the type.
     *
     * @param type the type whose captured type variables are substituted with those in {@code
     *     capturedTypeVarToAnnotatedTypeVar}
     * @param capturedTypeVarToAnnotatedTypeVar mapping from a TypeVariable (which is a captured
     *     type variable) to an AnnotatedTypeVariable
     */
    public void substitute(
        AnnotatedTypeVariable type,
        Map<TypeVariable, AnnotatedTypeVariable> capturedTypeVarToAnnotatedTypeVar) {
      this.capturedTypeVarToAnnotatedTypeVar = capturedTypeVarToAnnotatedTypeVar;
      IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> mapping = new IdentityHashMap<>();
      visit(type.getLowerBound(), mapping);
      visit(type.getUpperBound(), mapping);
      this.capturedTypeVarToAnnotatedTypeVar = null;
    }

    @Override
    public AnnotatedTypeMirror visitTypeVariable(
        AnnotatedTypeVariable original,
        IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
      AnnotatedTypeMirror cap = capturedTypeVarToAnnotatedTypeVar.get(original.getUnderlyingType());
      if (cap != null) {
        return cap;
      }
      return super.visitTypeVariable(original, originalToCopy);
    }

    @Override
    protected <T extends AnnotatedTypeMirror> T makeOrReturnCopy(
        T original, IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
      AnnotatedTypeMirror copy = originalToCopy.get(original);
      if (copy != null) {
        @SuppressWarnings("unchecked" // the key-value pairs in originalToCopy are always the same
        // kind of AnnotatedTypeMirror.
        )
        T copyCasted = (T) copy;
        return copyCasted;
      }

      if (original.getKind() == TypeKind.TYPEVAR) {
        AnnotatedTypeMirror captureType =
            capturedTypeVarToAnnotatedTypeVar.get(
                ((AnnotatedTypeVariable) original).getUnderlyingType());
        if (captureType != null) {
          originalToCopy.put(original, captureType);
          @SuppressWarnings("unchecked" // the key-value pairs in originalToCopy are always the same
          // kind of AnnotatedTypeMirror.
          )
          T captureTypeCasted = (T) captureType;
          return captureTypeCasted;
        }
      }
      originalToCopy.put(original, original);
      return original;
    }
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

  /**
   * Returns the utility class for working with {@link Element}s.
   *
   * @return the utility class for working with {@link Element}s
   */
  public final Elements getElementUtils() {
    return this.elements;
  }

  /** Accessor for the tree utilities. */
  public Trees getTreeUtils() {
    return this.trees;
  }

  /**
   * Accessor for the processing environment.
   *
   * @return the processing environment
   */
  public ProcessingEnvironment getProcessingEnv() {
    return this.processingEnv;
  }

  /** Matches addition of a constant. */
  private static final Pattern plusConstant = Pattern.compile(" *\\+ *(-?[0-9]+)$");

  /** Matches subtraction of a constant. */
  private static final Pattern minusConstant = Pattern.compile(" *- *(-?[0-9]+)$");

  /** Matches a string whose only parens are at the beginning and end of the string. */
  private static final Pattern surroundingParensPattern = Pattern.compile("^\\([^()]\\)");

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
   * There are methods that can only take as input an expression that represents a JavaExpression.
   * The purpose of this is to pre-process expressions to make those methods more likely to succeed.
   *
   * @param expression an expression to remove a constant offset from
   * @return a sub-expression and a constant offset. The offset is "0" if this routine is unable to
   *     splite the given expression
   */
  // TODO: generalize.  There is no reason this couldn't handle arbitrary addition and subtraction
  // expressions, given the Index Checker's support for OffsetEquation.  That might even make its
  // implementation simpler.
  public static IPair<String, String> getExpressionAndOffset(String expression) {
    String expr = expression;
    String offset = "0";

    // Is this normalization necessary?
    // Remove surrounding whitespace.
    expr = expr.trim();
    // Remove surrounding parentheses.
    if (surroundingParensPattern.matcher(expr).matches()) {
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

    return IPair.of(expr, offset);
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
   * <p>If a type is returned, then inference assumes that {@code expressionTree} was asigned to it.
   * This biases the inference algorithm toward the annotations in the returned type. In particular,
   * if the annotations on type variables in invariant positions are a super type of the annotations
   * inferred, the super type annotations are chosen.
   *
   * <p>This implementation returns null, but subclasses may override this method to return a type.
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
   * <p>In the end, all annotation comparisons are by name. This method is faster than {@link
   * AnnotationUtils#areSameByClass(AnnotationMirror, Class)} because it caches the name of {@code
   * annoClass} rather than computing it on each invocation of this method.
   *
   * @param am the AnnotationMirror whose class to compare
   * @param annoClass the class to compare
   * @return true if annoclass is the class of am
   */
  public boolean areSameByClass(AnnotationMirror am, Class<? extends Annotation> annoClass) {
    if (!shouldCache) {
      return AnnotationUtils.areSameByName(am, annoClass.getCanonicalName());
    }
    @SuppressWarnings("nullness") // assume getCanonicalName returns non-null
    String canonicalName = annotationClassNames.computeIfAbsent(annoClass, Class::getCanonicalName);
    return AnnotationUtils.areSameByName(am, canonicalName);
  }

  /**
   * Checks that the collection contains the annotation. Using {@code Collection.contains} does not
   * always work, because it does not use {@link AnnotationUtils#areSame} for comparison.
   *
   * <p>In the end, all annotation comparisons are by name. This method is faster than {@link
   * AnnotationUtils#containsSameByClass(Collection, Class)} because it (actually, its callee)
   * caches the name of {@code annoClass} rather than computing it each time.
   *
   * @param c a collection of AnnotationMirrors
   * @param annoClass the annotation class to search for in c
   * @return true iff c contains an annotation of class annoClass, according to {@link
   *     #areSameByClass}
   */
  public boolean containsSameByClass(
      Collection<? extends AnnotationMirror> c, Class<? extends Annotation> annoClass) {
    return getAnnotationByClass(c, annoClass) != null;
  }

  /**
   * Returns the AnnotationMirror in {@code c} that has class {@code annoClass}.
   *
   * <p>This method is faster than {@link AnnotationUtils#getAnnotationByClass(Collection, Class)}
   * because it (actually, its callee) caches the name of the class rather than computing it each
   * time.
   *
   * @param c a collection of AnnotationMirrors
   * @param annoClass the class to search for in c
   * @return an AnnotationMirror with class {@code annoClass} iff c contains one, according to
   *     areSameByClass; otherwise, {@code null}
   */
  public @Nullable AnnotationMirror getAnnotationByClass(
      Collection<? extends AnnotationMirror> c, Class<? extends Annotation> annoClass) {
    for (AnnotationMirror an : c) {
      if (areSameByClass(an, annoClass)) {
        return an;
      }
    }
    return null;
  }

  /**
   * Changes the type of {@code rhsATM} when being assigned to a field, for use by whole-program
   * inference. The default implementation does nothing.
   *
   * @param lhsTree the tree for the field whose type will be changed
   * @param element the element for the field whose type will be changed
   * @param fieldName the name of the field whose type will be changed
   * @param rhsATM the type of the expression being assigned to the field, which is side-effected by
   *     this method
   */
  public void wpiAdjustForUpdateField(
      Tree lhsTree, Element element, String fieldName, AnnotatedTypeMirror rhsATM) {}

  /**
   * Changes the type of {@code rhsATM} when being assigned to anything other than a field, for use
   * by whole-program inference. The default implementation does nothing.
   *
   * @param rhsATM the type of the rhs of the pseudo-assignment, which is side-effected by this
   *     method
   */
  public void wpiAdjustForUpdateNonField(AnnotatedTypeMirror rhsATM) {}

  /**
   * Returns whether whole-program inference should infer types for receiver expressions. For some
   * type systems, such as nullness, it doesn't make sense for WPI to do inference on receivers.
   *
   * @return true if WPI should infer types for method receiver parameters, false otherwise
   */
  public boolean wpiShouldInferTypesForReceivers() {
    return true;
  }

  /**
   * Side-effects the method or constructor annotations to make any desired changes before writing
   * to an annotation file.
   *
   * @param methodAnnos the method or constructor annotations to modify
   */
  public void wpiPrepareMethodForWriting(AMethod methodAnnos) {
    // This implementation does nothing.
  }

  /**
   * Side-effects the method or constructor annotations to make any desired changes before writing
   * to an ajava file.
   *
   * <p>Overriding implementations should call {@code super.wpiPrepareMethodForWriting()}.
   *
   * @param methodAnnos the method or constructor annotations to modify
   * @param inSupertypes the method or constructor annotations for all overridden methods; not
   *     side-effected
   * @param inSubtypes the method or constructor annotations for all overriding methods; not
   *     side-effected
   */
  public void wpiPrepareMethodForWriting(
      WholeProgramInferenceJavaParserStorage.CallableDeclarationAnnos methodAnnos,
      Collection<WholeProgramInferenceJavaParserStorage.CallableDeclarationAnnos> inSupertypes,
      Collection<WholeProgramInferenceJavaParserStorage.CallableDeclarationAnnos> inSubtypes) {
    Map<String, IPair<AnnotatedTypeMirror, AnnotatedTypeMirror>> precondMap =
        methodAnnos.getPreconditions();
    Map<String, IPair<AnnotatedTypeMirror, AnnotatedTypeMirror>> postcondMap =
        methodAnnos.getPostconditions();
    for (WholeProgramInferenceJavaParserStorage.CallableDeclarationAnnos inSupertype :
        inSupertypes) {
      makeConditionConsistentWithOtherMethod(precondMap, inSupertype, true, true);
      makeConditionConsistentWithOtherMethod(postcondMap, inSupertype, false, true);
    }
    for (WholeProgramInferenceJavaParserStorage.CallableDeclarationAnnos inSubtype : inSubtypes) {
      makeConditionConsistentWithOtherMethod(precondMap, inSubtype, true, false);
      makeConditionConsistentWithOtherMethod(postcondMap, inSubtype, false, false);
    }
  }

  /**
   * Performs side effects to make {@code conditionMap} obey behavioral subtyping constraints with
   * {@code otherDeclAnnos}, that is, postconditions must be at least as strong as the postcondition
   * on the superclass, and preconditions must be at most as strong as the condition on the
   * superclass.
   *
   * <p>Overriding implementations should call {@code
   * super.makeConditionConsistentWithOtherMethod()}.
   *
   * @param conditionMap pre- or post-condition annotations on a method M; may be side-effected
   * @param otherDeclAnnos annotations on a method that M overrides or that overrides M; that is, on
   *     a method in the same "method family" as M; may be side-effected
   * @param isPrecondition true if the annotations are pre-condition annotations, false if they are
   *     post-condition annotations
   * @param otherIsSupertype true if {@code otherDeclAnnos} are on a supertype; false if they are on
   *     a subtype
   */
  protected void makeConditionConsistentWithOtherMethod(
      Map<String, IPair<AnnotatedTypeMirror, AnnotatedTypeMirror>> conditionMap,
      WholeProgramInferenceJavaParserStorage.CallableDeclarationAnnos otherDeclAnnos,
      boolean isPrecondition,
      boolean otherIsSupertype) {
    for (Map.Entry<String, IPair<AnnotatedTypeMirror, AnnotatedTypeMirror>> entry :
        conditionMap.entrySet()) {
      String expr = entry.getKey();
      IPair<AnnotatedTypeMirror, AnnotatedTypeMirror> pair = entry.getValue();
      AnnotatedTypeMirror inferredType = pair.first;
      AnnotatedTypeMirror declaredType = pair.second;
      if (otherIsSupertype ? isPrecondition : !isPrecondition) {
        // other is a supertype & compare preconditions, or
        // other is a subtype & compare postconditions.
        Map<String, IPair<AnnotatedTypeMirror, AnnotatedTypeMirror>> otherConditionMap =
            isPrecondition ? otherDeclAnnos.getPreconditions() : otherDeclAnnos.getPostconditions();
        // TODO: Complete support for "every expression" conditions, then remove the
        // `!otherConditionMap.containsKey(expr)` test.
        // If a condition map contains the key "every expression", that means that inference
        // completed without inferring any conditions of that type.  For example, if no
        // @EnsuresCalledMethods was inferred for any expression, the map would contain the
        // key "every expression", which is not a legal Java expression.
        if (otherConditionMap.containsKey("every expression")
            || !otherConditionMap.containsKey(expr)) {
          // `otherInferredType` was inferred to be the top type.
          // Put the top type on `inferredType`.
          inferredType.replaceAnnotations(declaredType.getPrimaryAnnotations());
        } else {
          AnnotatedTypeMirror otherInferredType =
              isPrecondition
                  ? otherDeclAnnos.getPreconditionsForExpression(expr, declaredType, this)
                  : otherDeclAnnos.getPostconditionsForExpression(expr, declaredType, this);
          this.getWholeProgramInference().updateAtmWithLub(inferredType, otherInferredType);
        }
      }
    }
  }

  /**
   * Does {@code annotatedForAnno}, which is an {@link
   * org.checkerframework.framework.qual.AnnotatedFor} annotation, apply to this checker?
   *
   * @param annotatedForAnno an {@link AnnotatedFor} annotation
   * @return whether {@code annotatedForAnno} applies to this checker
   */
  public boolean doesAnnotatedForApplyToThisChecker(AnnotationMirror annotatedForAnno) {
    List<String> annotatedForCheckers =
        AnnotationUtils.getElementValueArray(
            annotatedForAnno, annotatedForValueElement, String.class);
    for (String annoForChecker : annotatedForCheckers) {
      if (checker.getUpstreamCheckerNames().contains(annoForChecker)
          || CheckerMain.matchesFullyQualifiedProcessor(
              annoForChecker, checker.getUpstreamCheckerNames(), true)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get the {@code expression} field/element of the given contract annotation.
   *
   * @param contractAnno a {@link RequiresQualifier}, {@link EnsuresQualifier}, or {@link
   *     EnsuresQualifier}
   * @return the {@code expression} field/element of the given annotation
   */
  public List<String> getContractExpressions(AnnotationMirror contractAnno) {
    DeclaredType annoType = contractAnno.getAnnotationType();
    if (types.isSameType(annoType, requiresQualifierTM)) {
      return AnnotationUtils.getElementValueArray(
          contractAnno, requiresQualifierExpressionElement, String.class);
    } else if (types.isSameType(annoType, ensuresQualifierTM)) {
      return AnnotationUtils.getElementValueArray(
          contractAnno, ensuresQualifierExpressionElement, String.class);
    } else if (types.isSameType(annoType, ensuresQualifierIfTM)) {
      return AnnotationUtils.getElementValueArray(
          contractAnno, ensuresQualifierIfExpressionElement, String.class);
    } else {
      throw new BugInCF("Not a contract annotation: " + contractAnno);
    }
  }

  /**
   * Get the {@code value} field/element of the given contract list annotation.
   *
   * @param contractListAnno a {@link org.checkerframework.framework.qual.RequiresQualifier.List
   *     RequiresQualifier.List}, {@link org.checkerframework.framework.qual.EnsuresQualifier.List
   *     EnsuresQualifier.List}, or {@link
   *     org.checkerframework.framework.qual.EnsuresQualifierIf.List EnsuresQualifierIf.List}
   * @return the {@code value} field/element of the given annotation
   */
  public List<AnnotationMirror> getContractListValues(AnnotationMirror contractListAnno) {
    DeclaredType annoType = contractListAnno.getAnnotationType();
    if (types.isSameType(annoType, requiresQualifierListTM)) {
      return AnnotationUtils.getElementValueArray(
          contractListAnno, requiresQualifierListValueElement, AnnotationMirror.class);
    } else if (types.isSameType(annoType, ensuresQualifierListTM)) {
      return AnnotationUtils.getElementValueArray(
          contractListAnno, ensuresQualifierListValueElement, AnnotationMirror.class);
    } else if (types.isSameType(annoType, ensuresQualifierIfListTM)) {
      return AnnotationUtils.getElementValueArray(
          contractListAnno, ensuresQualifierIfListValueElement, AnnotationMirror.class);
    } else {
      throw new BugInCF("Not a contract list annotation: " + contractListAnno);
    }
  }

  /**
   * Returns true if the type is immutable. Subclasses can override this method to add types that
   * are mutable, but the annotated type of an object is immutable.
   *
   * @param type type to test
   * @return true if the type is immutable
   */
  public boolean isImmutable(TypeMirror type) {
    if (type.getKind().isPrimitive()) {
      return true;
    }
    return ImmutableTypes.isImmutable(TypeAnnotationUtils.unannotatedType(type).toString());
  }

  @Override
  public boolean isSideEffectFree(ExecutableElement methodElement) {
    if (assumeSideEffectFree || (assumePureGetters && ElementUtils.isGetter(methodElement))) {
      return true;
    }
    if (ElementUtils.isRecordAccessor(methodElement)
        && ElementUtils.isAutoGeneratedRecordMember(methodElement)) {
      return true;
    }
    for (AnnotationMirror anno : getDeclAnnotations(methodElement)) {
      if (areSameByClass(anno, org.checkerframework.dataflow.qual.SideEffectFree.class)
          || areSameByClass(anno, org.checkerframework.dataflow.qual.Pure.class)
          || areSameByClass(anno, org.jmlspecs.annotation.Pure.class)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isDeterministic(ExecutableElement methodElement) {
    if (assumeDeterministic || (assumePureGetters && ElementUtils.isGetter(methodElement))) {
      return true;
    }
    if (ElementUtils.isRecordAccessor(methodElement)
        && ElementUtils.isAutoGeneratedRecordMember(methodElement)) {
      return true;
    }
    for (AnnotationMirror anno : getDeclAnnotations(methodElement)) {
      if (areSameByClass(anno, org.checkerframework.dataflow.qual.Deterministic.class)
          || areSameByClass(anno, org.checkerframework.dataflow.qual.Pure.class)
          || areSameByClass(anno, org.jmlspecs.annotation.Pure.class)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Output a message about {@link #getAnnotatedType}, if logging is on.
   *
   * <p>Set the value of {@link #debugGat} to {@literal true} to enable logging.
   *
   * <p>Any {@link Tree} arguments will be formatted using {@link TreeUtils#toStringTruncated(Tree,
   * int)} at a maximum length of 60.
   *
   * @param format a format string
   * @param args arguments to the format string
   */
  @FormatMethod
  public void logGat(String format, Object... args) {
    if (debugGat) {
      SystemPlume.sleep(1); // logging can interleave with typechecker output

      // Shorten tree arguments to keep the output readable.
      for (int i = 0; i < args.length; ++i) {
        Object arg = args[i];
        if (arg instanceof Tree) {
          args[i] = TreeUtils.toStringTruncated((Tree) arg, 60);
        }
      }

      System.out.printf(format, args);
    }
  }
}
