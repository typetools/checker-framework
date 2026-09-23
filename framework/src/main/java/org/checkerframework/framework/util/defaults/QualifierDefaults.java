package org.checkerframework.framework.util.defaults;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNoType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.MapsP;
import org.plumelib.util.StringsP;

/**
 * Determines the default qualifiers on a type. Default qualifiers are specified via the {@link
 * org.checkerframework.framework.qual.DefaultQualifier} annotation.
 *
 * @see org.checkerframework.framework.qual.DefaultQualifier
 */
public class QualifierDefaults {

  // TODO add visitor state to get the default annotations from the top down?
  // TODO apply from package elements also
  // TODO try to remove some dependencies (e.g. on factory)

  /**
   * True if a default should be applied to type vars located in the type being defaulted. This
   * should only ever be true when the type variable is a local variable, non-component use, i.e.
   *
   * <pre>{@code
   * <T> void method(@NOT_HERE T tIn) {
   *     T t = tIn;
   * }
   * }</pre>
   *
   * The local variable T will be defaulted in order to allow dataflow to refine T. This variable
   * will be false if dataflow is not in use.
   */
  private boolean applyToTypeVar = false;

  /** Element utilities to use. */
  private final Elements elements;

  /** The value() element/field of a @DefaultQualifier annotation. */
  protected final ExecutableElement defaultQualifierValueElement;

  /** The locations() element/field of a @DefaultQualifier annotation. */
  protected final ExecutableElement defaultQualifierLocationsElement;

  /** The value() element/field of a @DefaultQualifier.List annotation. */
  protected final ExecutableElement defaultQualifierListValueElement;

  /** AnnotatedTypeFactory to use. */
  private final AnnotatedTypeFactory atypeFactory;

  /** Defaults for checked code. */
  private final DefaultSet checkedCodeDefaults = new DefaultSet();

  /** Defaults for unchecked code. */
  private final DefaultSet uncheckedCodeDefaults = new DefaultSet();

  /** Size for caches. */
  private static final int CACHE_SIZE = 300;

  /** Mapping from an Element to the bound type. */
  protected final Map<Element, BoundType> elementToBoundType = MapsP.createLruCache(CACHE_SIZE);

  /**
   * Defaults that a type system has explicitly declared for an Element, via {@link
   * #addElementDefault}. These compose with the defaults written as {@code @DefaultQualifier} on
   * the element and with the defaults of the element's enclosing scopes, and they take precedence
   * over both; see {@link #defaultsAt} and {@link #precedenceList}.
   */
  private final IdentityHashMap<Element, DefaultSet> elementDeclaredDefaults =
      new IdentityHashMap<>();

  /**
   * Memoizes {@link #defaultsAt}: a mapping from an Element to all the defaults that apply to that
   * Element, including the defaults contributed by the Element's enclosing scopes.
   *
   * <p>This is an LRU cache, because most Elements map to {@link ScopeDefaults#EMPTY} and an
   * unbounded map would retain an entry for every scope in the compilation.
   */
  private final Map<Element, ScopeDefaults> defaultsAtCache = MapsP.createLruCache(CACHE_SIZE);

  /**
   * Memoizes {@link #precedenceList} for the scopes that have defaults of their own. A scope
   * without such defaults needs no entry, because its precedence list is one of the two shared
   * arrays {@link #checkedCodeDefaultsArray} and {@link #uncheckedThenCheckedArray}. Like {@link
   * #defaultsAtCache}, this is an LRU cache.
   */
  private final Map<Element, PrecedenceList> precedenceListCache = MapsP.createLruCache(CACHE_SIZE);

  /**
   * The precedence list for a scope that has no element defaults and to which conservative defaults
   * do not apply. Computed on demand by {@link #getCheckedCodeDefaultsArray}.
   */
  private @Nullable PrecedenceList checkedCodeDefaultsArray = null;

  /**
   * The precedence list for a scope that has no element defaults and to which conservative defaults
   * apply. Computed on demand by {@link #getUncheckedThenCheckedArray}.
   */
  private @Nullable PrecedenceList uncheckedThenCheckedArray = null;

  /** A mapping of Element &rarr; Whether or not that element is AnnotatedFor this type system. */
  private final IdentityHashMap<Element, Boolean> elementAnnotatedFors = new IdentityHashMap<>();

  /** CLIMB locations whose standard default is top for a given type system. */
  public static final List<TypeUseLocation> STANDARD_CLIMB_DEFAULTS_TOP =
      List.of(
          TypeUseLocation.LOCAL_VARIABLE,
          TypeUseLocation.RESOURCE_VARIABLE,
          TypeUseLocation.EXCEPTION_PARAMETER,
          TypeUseLocation.IMPLICIT_UPPER_BOUND);

  /** CLIMB locations whose standard default is bottom for a given type system. */
  public static final List<TypeUseLocation> STANDARD_CLIMB_DEFAULTS_BOTTOM =
      List.of(TypeUseLocation.IMPLICIT_LOWER_BOUND);

  /**
   * Locations whose default, when applied at the top level of a type, annotates some node other
   * than the node being visited: the alternatives of a union type, or the parameter, receiver, or
   * return types of an executable type.
   */
  private static final Set<TypeUseLocation> CROSS_NODE_LOCATIONS =
      Collections.unmodifiableSet(
          EnumSet.of(
              TypeUseLocation.EXCEPTION_PARAMETER,
              TypeUseLocation.RECEIVER,
              TypeUseLocation.PARAMETER,
              TypeUseLocation.RETURN,
              TypeUseLocation.CONSTRUCTOR_RESULT));

  /** Locations whose default can annotate a node below the top level of a type. */
  private static final Set<TypeUseLocation> DESCENDANT_LOCATIONS =
      Collections.unmodifiableSet(
          EnumSet.of(
              TypeUseLocation.LOWER_BOUND,
              TypeUseLocation.EXPLICIT_LOWER_BOUND,
              TypeUseLocation.IMPLICIT_LOWER_BOUND,
              TypeUseLocation.UPPER_BOUND,
              TypeUseLocation.EXPLICIT_UPPER_BOUND,
              TypeUseLocation.IMPLICIT_UPPER_BOUND,
              TypeUseLocation.OTHERWISE,
              TypeUseLocation.ALL));

  /** List of TypeUseLocations that are valid for unchecked code defaults. */
  private static final List<TypeUseLocation> validUncheckedCodeDefaultLocations =
      List.of(
          TypeUseLocation.FIELD,
          TypeUseLocation.PARAMETER,
          TypeUseLocation.RETURN,
          TypeUseLocation.RECEIVER,
          TypeUseLocation.UPPER_BOUND,
          TypeUseLocation.LOWER_BOUND,
          TypeUseLocation.OTHERWISE,
          TypeUseLocation.ALL);

  /** Standard unchecked default locations that should be top. */
  // Fields are defaulted to top so that warnings are issued at field reads, which we believe are
  // more common than field writes. Future work is to specify different defaults for field reads
  // and field writes.  (When a field is written to, its type should be bottom.)
  public static final List<TypeUseLocation> STANDARD_UNCHECKED_DEFAULTS_TOP =
      List.of(TypeUseLocation.RETURN, TypeUseLocation.FIELD, TypeUseLocation.UPPER_BOUND);

  /** Standard unchecked default locations that should be bottom. */
  public static final List<TypeUseLocation> STANDARD_UNCHECKED_DEFAULTS_BOTTOM =
      List.of(TypeUseLocation.PARAMETER, TypeUseLocation.LOWER_BOUND);

  /** True if conservative defaults should be used in unannotated source code. */
  private final boolean useConservativeDefaultsSource;

  /** True if conservative defaults should be used for bytecode. */
  private final boolean useConservativeDefaultsBytecode;

  /**
   * Returns an array of locations that are valid for the unchecked value defaults. These are simply
   * by syntax, since an entire file is typechecked, it is not possible for local variables to be
   * unchecked.
   */
  public static List<TypeUseLocation> validLocationsForUncheckedCodeDefaults() {
    return validUncheckedCodeDefaultLocations;
  }

  /**
   * @param elements interface to Element data in the current processing environment
   * @param atypeFactory an annotation factory, used to get annotations by name
   */
  public QualifierDefaults(Elements elements, AnnotatedTypeFactory atypeFactory) {
    this.elements = elements;
    this.atypeFactory = atypeFactory;
    this.useConservativeDefaultsBytecode =
        atypeFactory.getChecker().useConservativeDefault("bytecode");
    this.useConservativeDefaultsSource = atypeFactory.getChecker().useConservativeDefault("source");
    ProcessingEnvironment processingEnv = atypeFactory.getProcessingEnv();
    this.defaultQualifierValueElement =
        TreeUtils.getMethod(DefaultQualifier.class, "value", 0, processingEnv);
    this.defaultQualifierLocationsElement =
        TreeUtils.getMethod(DefaultQualifier.class, "locations", 0, processingEnv);
    this.defaultQualifierListValueElement =
        TreeUtils.getMethod(DefaultQualifier.List.class, "value", 0, processingEnv);
  }

  @Override
  public String toString() {
    // displays the checked and unchecked code defaults
    return StringsP.joinLines(
        "Checked code defaults: ",
        StringsP.joinLines(checkedCodeDefaults),
        "Unchecked code defaults: ",
        StringsP.joinLines(uncheckedCodeDefaults),
        "useConservativeDefaultsSource: " + useConservativeDefaultsSource,
        "useConservativeDefaultsBytecode: " + useConservativeDefaultsBytecode);
  }

  /**
   * Check that a default with TypeUseLocation OTHERWISE or ALL is specified.
   *
   * @return true if we found a Default with location OTHERWISE or ALL
   */
  public boolean hasDefaultsForCheckedCode() {
    for (Default def : checkedCodeDefaults) {
      if (def.location == TypeUseLocation.OTHERWISE || def.location == TypeUseLocation.ALL) {
        return true;
      }
    }
    return false;
  }

  /** Add standard unchecked defaults that do not conflict with previously added defaults. */
  public void addUncheckedStandardDefaults() {
    QualifierHierarchy qualHierarchy = this.atypeFactory.getQualifierHierarchy();
    AnnotationMirrorSet tops = qualHierarchy.getTopAnnotations();
    AnnotationMirrorSet bottoms = qualHierarchy.getBottomAnnotations();

    for (TypeUseLocation loc : STANDARD_UNCHECKED_DEFAULTS_TOP) {
      // Only add standard defaults in locations where a default has not be specified
      for (AnnotationMirror top : tops) {
        if (!conflictsWithExistingDefaults(uncheckedCodeDefaults, top, loc)) {
          addUncheckedCodeDefault(top, loc);
        }
      }
    }

    for (TypeUseLocation loc : STANDARD_UNCHECKED_DEFAULTS_BOTTOM) {
      for (AnnotationMirror bottom : bottoms) {
        // Only add standard defaults in locations where a default has not be specified
        if (!conflictsWithExistingDefaults(uncheckedCodeDefaults, bottom, loc)) {
          addUncheckedCodeDefault(bottom, loc);
        }
      }
    }
  }

  /** Add standard CLIMB defaults that do not conflict with previously added defaults. */
  public void addClimbStandardDefaults() {
    QualifierHierarchy qualHierarchy = this.atypeFactory.getQualifierHierarchy();
    AnnotationMirrorSet tops = qualHierarchy.getTopAnnotations();
    AnnotationMirrorSet bottoms = qualHierarchy.getBottomAnnotations();

    for (TypeUseLocation loc : STANDARD_CLIMB_DEFAULTS_TOP) {
      for (AnnotationMirror top : tops) {
        if (!conflictsWithExistingDefaults(checkedCodeDefaults, top, loc)) {
          // Only add standard defaults in locations where a default has not been
          // specified
          addCheckedCodeDefault(top, loc);
        }
      }
    }

    for (TypeUseLocation loc : STANDARD_CLIMB_DEFAULTS_BOTTOM) {
      for (AnnotationMirror bottom : bottoms) {
        if (!conflictsWithExistingDefaults(checkedCodeDefaults, bottom, loc)) {
          // Only add standard defaults in locations where a default has not been
          // specified
          addCheckedCodeDefault(bottom, loc);
        }
      }
    }
  }

  /**
   * Adds a default annotation. A programmer may override this by writing the @DefaultQualifier
   * annotation on an element.
   *
   * @param absoluteDefaultAnno the default annotation mirror
   * @param location the type use location
   */
  public void addCheckedCodeDefault(
      AnnotationMirror absoluteDefaultAnno, TypeUseLocation location) {
    clearDefaultsCaches();
    checkDuplicates(checkedCodeDefaults, absoluteDefaultAnno, location);
    checkedCodeDefaults.add(new Default(absoluteDefaultAnno, location));
  }

  /**
   * Add a default annotation for unchecked elements.
   *
   * @param uncheckedDefaultAnno the default annotation mirror
   * @param location the type use location
   */
  public void addUncheckedCodeDefault(
      AnnotationMirror uncheckedDefaultAnno, TypeUseLocation location) {
    clearDefaultsCaches();
    checkDuplicates(uncheckedCodeDefaults, uncheckedDefaultAnno, location);
    checkIsValidUncheckedCodeLocation(uncheckedDefaultAnno, location);

    uncheckedCodeDefaults.add(new Default(uncheckedDefaultAnno, location));
  }

  /** Sets the default annotation for unchecked elements, with specific locations. */
  public void addUncheckedCodeDefaults(
      AnnotationMirror absoluteDefaultAnno, TypeUseLocation[] locations) {
    for (TypeUseLocation location : locations) {
      addUncheckedCodeDefault(absoluteDefaultAnno, location);
    }
  }

  public void addCheckedCodeDefaults(
      AnnotationMirror absoluteDefaultAnno, TypeUseLocation[] locations) {
    for (TypeUseLocation location : locations) {
      addCheckedCodeDefault(absoluteDefaultAnno, location);
    }
  }

  /**
   * Sets the default annotations for a certain Element.
   *
   * <p>The default applies within {@code elem} and within the elements that {@code elem} encloses.
   * It composes with the {@code @DefaultQualifier} annotations on those elements, and it takes
   * precedence over them and over a default registered for an element that encloses {@code elem}.
   *
   * @param elem the scope to set the default within
   * @param elementDefaultAnno the default to set
   * @param location the location to apply the default to
   */
  public void addElementDefault(
      Element elem, AnnotationMirror elementDefaultAnno, TypeUseLocation location) {
    DefaultSet prevset = elementDeclaredDefaults.get(elem);
    if (prevset != null) {
      checkDuplicates(prevset, elementDefaultAnno, location);
    } else {
      prevset = new DefaultSet();
      elementDeclaredDefaults.put(elem, prevset);
    }
    prevset.add(new Default(elementDefaultAnno, location));
    clearDefaultsCaches();
  }

  /**
   * Discards every memoized answer that a newly-added default could invalidate. The standard
   * defaults methods {@code addClimbStandardDefaults}, {@code addUncheckedStandardDefaults}, and
   * the plural {@code add*CodeDefaults} all funnel through {@link #addCheckedCodeDefault} or {@link
   * #addUncheckedCodeDefault}, so those two methods plus {@link #addElementDefault} are the only
   * callers this method needs.
   */
  private void clearDefaultsCaches() {
    defaultsAtCache.clear();
    precedenceListCache.clear();
    checkedCodeDefaultsArray = null;
    uncheckedThenCheckedArray = null;
  }

  /**
   * Throws an exception if the given location is not a valid location for an unchecked code
   * default.
   *
   * @param uncheckedDefaultAnno the unchecked code default annotation; used only for the error
   *     message
   * @param location the location to check
   */
  private void checkIsValidUncheckedCodeLocation(
      AnnotationMirror uncheckedDefaultAnno, TypeUseLocation location) {
    if (!validLocationsForUncheckedCodeDefaults().contains(location)) {
      throw new BugInCF(
          "Invalid unchecked code default location: " + location + " -> " + uncheckedDefaultAnno);
    }
  }

  private void checkDuplicates(
      DefaultSet previousDefaults, AnnotationMirror newAnno, TypeUseLocation newLoc) {
    if (conflictsWithExistingDefaults(previousDefaults, newAnno, newLoc)) {
      throw new BugInCF(
          "Only one qualifier from a hierarchy can be the default. Existing: "
              + previousDefaults
              + " and new: "
              + new Default(newAnno, newLoc));
    }
  }

  /**
   * Returns true if there are conflicts with existing defaults.
   *
   * @param previousDefaults the previous defaults
   * @param newAnno the new annotation
   * @param newLoc the location of the type use
   * @return true if there are conflicts with existing defaults
   */
  private boolean conflictsWithExistingDefaults(
      DefaultSet previousDefaults, AnnotationMirror newAnno, TypeUseLocation newLoc) {
    QualifierHierarchy qualHierarchy = atypeFactory.getQualifierHierarchy();

    for (Default previous : previousDefaults) {
      if (!AnnotationUtils.areSame(newAnno, previous.anno) && previous.location == newLoc) {
        AnnotationMirror previousTop = qualHierarchy.getTopAnnotation(previous.anno);
        if (qualHierarchy.isSubtypeQualifiersOnly(newAnno, previousTop)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Applies default annotations to a type obtained from an {@link
   * javax.lang.model.element.Element}.
   *
   * @param elt the element from which the type was obtained
   * @param type the type to annotate
   */
  public void annotate(Element elt, AnnotatedTypeMirror type) {
    if (elt != null) {
      switch (elt.getKind()) {
        case FIELD,
            LOCAL_VARIABLE,
            PARAMETER,
            RESOURCE_VARIABLE,
            EXCEPTION_PARAMETER,
            ENUM_CONSTANT -> {
          String varName = elt.getSimpleName().toString();
          ((GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory)
              .getDefaultForTypeAnnotator()
              .defaultTypeFromName(type, varName);
        }
        case METHOD -> {
          String methodName = elt.getSimpleName().toString();
          AnnotatedTypeMirror returnType = ((AnnotatedExecutableType) type).getReturnType();
          ((GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory)
              .getDefaultForTypeAnnotator()
              .defaultTypeFromName(returnType, methodName);
        }
        default -> {} // do nothing
      }
    }

    applyDefaultsElement(elt, type);
  }

  /**
   * Applies default annotations to a type given a {@link com.sun.source.tree.Tree}.
   *
   * @param tree the tree from which the type was obtained
   * @param type the type to annotate
   */
  public void annotate(Tree tree, AnnotatedTypeMirror type) {
    applyDefaults(tree, type);
  }

  /**
   * Determines the nearest enclosing element for a tree by climbing the tree toward the root and
   * obtaining the element for the first declaration (variable, method, or class) that encloses the
   * tree. Initializers of local variables are handled in a special way: within an initializer we
   * look for the DefaultQualifier(s) annotation and keep track of the previously visited tree.
   * TODO: explain the behavior better.
   *
   * @param tree the tree
   * @return the nearest enclosing element for a tree
   */
  private @Nullable Element nearestEnclosingExceptLocal(Tree tree) {
    TreePath path = atypeFactory.getPath(tree);
    if (path == null) {
      Element element = atypeFactory.getEnclosingElementForArtificialTree(tree);
      if (element != null) {
        return element;
      } else {
        return TreeUtils.elementFromTree(tree);
      }
    }

    Tree prev = null;

    for (Tree t : path) {
      switch (t.getKind()) {
        case ANNOTATED_TYPE, ANNOTATION -> {
          // If the tree is in an annotation, then there is no relevant scope.
          return null;
        }
        case VARIABLE -> {
          VariableTree vtree = (VariableTree) t;
          ExpressionTree vtreeInit = vtree.getInitializer();
          @SuppressWarnings("interning:not.interned") // check cached value
          boolean sameAsPrev = (vtreeInit != null && prev == vtreeInit);
          if (sameAsPrev) {
            Element elt = TreeUtils.elementFromDeclaration((VariableTree) t);
            AnnotationMirror d = atypeFactory.getDeclAnnotation(elt, DefaultQualifier.class);
            AnnotationMirror ds = atypeFactory.getDeclAnnotation(elt, DefaultQualifier.List.class);

            if (d == null && ds == null) {
              break;
            }
          }
          if (prev != null && prev instanceof ModifiersTree) {
            // Annotations are modifiers. We do not want to apply the local variable
            // default to annotations. Without this, test fenum/TestSwitch failed,
            // because the default for an argument became incompatible with the declared
            // type.
            break;
          }
          return TreeUtils.elementFromDeclaration((VariableTree) t);
        }
        case METHOD -> {
          return TreeUtils.elementFromDeclaration((MethodTree) t);
        }
        case CLASS, RECORD, ENUM, INTERFACE, ANNOTATION_TYPE -> {
          return TreeUtils.elementFromDeclaration((ClassTree) t);
        }
        default -> {} // Do nothing.
      }
      prev = t;
    }

    return null;
  }

  /**
   * Applies default annotations to a type. A {@link com.sun.source.tree.Tree} determines the
   * appropriate scope for defaults.
   *
   * <p>For instance, if the tree is associated with a declaration (e.g., it's the use of a field,
   * or a method invocation), defaults in the scope of the <i>declaration</i> are used; if the tree
   * is not associated with a declaration (e.g., a typecast), defaults in the scope of the tree are
   * used.
   *
   * @param tree the tree associated with the type
   * @param type the type to which defaults will be applied
   * @see #applyDefaultsElement(javax.lang.model.element.Element,
   *     org.checkerframework.framework.type.AnnotatedTypeMirror)
   */
  private void applyDefaults(Tree tree, AnnotatedTypeMirror type) {

    // The location to take defaults from.
    Element elt;
    switch (tree.getKind()) {
      case MEMBER_SELECT -> elt = TreeUtils.elementFromUse((MemberSelectTree) tree);
      case IDENTIFIER -> {
        elt = TreeUtils.elementFromUse((IdentifierTree) tree);
        if (ElementUtils.isTypeDeclaration(elt)) {
          // If the identifier is a type, then use the scope of the tree.
          elt = nearestEnclosingExceptLocal(tree);
        }
      }
      case METHOD_INVOCATION -> elt = TreeUtils.elementFromUse((MethodInvocationTree) tree);
      // TODO cases for array access, etc. -- every expression tree
      // (The above probably means that we should use defaults in the
      // scope of the declaration of the array.  Is that right?  -MDE)
      default ->
          // If no associated symbol was found, use the tree's (lexical) scope.
          elt = nearestEnclosingExceptLocal(tree);
        // elt = nearestEnclosing(tree);
    }
    // System.out.println("applyDefaults on tree " + tree +
    //        " gives elt: " + elt + "(" + elt.getKind() + ")");

    boolean defaultTypeVarLocals =
        (atypeFactory instanceof GenericAnnotatedTypeFactory<?, ?, ?, ?>)
            && ((GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory)
                .getShouldDefaultTypeVarLocals();
    applyToTypeVar =
        defaultTypeVarLocals
            && elt != null
            && ElementUtils.isLocalVariable(elt)
            && type.getKind() == TypeKind.TYPEVAR;
    applyDefaultsElement(elt, type);
    applyToTypeVar = false;
  }

  /** The default {@code value} element for a @DefaultQualifier annotation. */
  private static final TypeUseLocation[] defaultQualifierValueDefault =
      new TypeUseLocation[] {org.checkerframework.framework.qual.TypeUseLocation.ALL};

  /**
   * Create a DefaultSet from a @DefaultQualifier annotation.
   *
   * @param dq a @DefaultQualifier annotation
   * @return a DefaultSet corresponding to the @DefaultQualifier annotation
   */
  private @Nullable DefaultSet fromDefaultQualifier(AnnotationMirror dq) {
    @SuppressWarnings("unchecked")
    Name cls = AnnotationUtils.getElementValueClassName(dq, defaultQualifierValueElement);
    AnnotationMirror anno = AnnotationBuilder.fromName(elements, cls);

    if (anno == null) {
      return null;
    }

    anno = atypeFactory.canonicalAnnotation(anno);

    if (atypeFactory.isSupportedQualifier(anno)) {
      TypeUseLocation[] locations =
          AnnotationUtils.getElementValueEnumArray(
              dq,
              defaultQualifierLocationsElement,
              TypeUseLocation.class,
              defaultQualifierValueDefault);
      DefaultSet ret = new DefaultSet();
      for (TypeUseLocation loc : locations) {
        ret.add(new Default(anno, loc));
      }
      return ret;
    } else {
      return null;
    }
  }

  private boolean isElementAnnotatedForThisChecker(Element elt) {
    boolean elementAnnotatedForThisChecker = false;

    if (elt == null) {
      throw new BugInCF("Call of QualifierDefaults.isElementAnnotatedForThisChecker with null");
    }

    if (elementAnnotatedFors.containsKey(elt)) {
      return elementAnnotatedFors.get(elt);
    }

    AnnotationMirror annotatedFor = atypeFactory.getDeclAnnotation(elt, AnnotatedFor.class);

    if (annotatedFor != null) {
      elementAnnotatedForThisChecker =
          atypeFactory.doesAnnotatedForApplyToThisChecker(annotatedFor);
    }

    if (!elementAnnotatedForThisChecker) {
      Element parent;
      if (elt.getKind() == ElementKind.PACKAGE) {
        // elt.getEnclosingElement() on a package is null; therefore,
        // use the dedicated method.
        parent = ElementUtils.parentPackage((PackageElement) elt, elements);
      } else {
        parent = elt.getEnclosingElement();
      }

      if (parent != null && isElementAnnotatedForThisChecker(parent)) {
        elementAnnotatedForThisChecker = true;
      }
    }

    if (atypeFactory.shouldCache && !atypeFactory.isParsingAnnotationFiles()) {
      elementAnnotatedFors.put(elt, elementAnnotatedForThisChecker);
    }

    return elementAnnotatedForThisChecker;
  }

  /**
   * Returns the defaults that apply to the given Element, considering defaults from enclosing
   * Elements.
   *
   * @param elt the element
   * @return the defaults
   */
  private ScopeDefaults defaultsAt(@Nullable Element elt) {
    if (elt == null) {
      return ScopeDefaults.EMPTY;
    }

    ScopeDefaults cached = defaultsAtCache.get(elt);
    if (cached != null) {
      return cached;
    }

    DefaultSet elementDefaults = null;
    DefaultSet qualifierDefaults = null;

    DefaultSet declared = elementDeclaredDefaults.get(elt);
    if (declared != null) {
      // Copy, because addElementDefault may add to the stored DefaultSet later.
      elementDefaults = new DefaultSet();
      elementDefaults.addAll(declared);
    }

    {
      AnnotationMirror dqAnno = atypeFactory.getDeclAnnotation(elt, DefaultQualifier.class);

      if (dqAnno != null) {
        qualifierDefaults = new DefaultSet();
        Set<Default> p = fromDefaultQualifier(dqAnno);

        if (p != null) {
          qualifierDefaults.addAll(p);
        }
      }
    }

    {
      AnnotationMirror dqListAnno =
          atypeFactory.getDeclAnnotation(elt, DefaultQualifier.List.class);
      if (dqListAnno != null) {
        if (qualifierDefaults == null) {
          qualifierDefaults = new DefaultSet();
        }

        List<AnnotationMirror> values =
            AnnotationUtils.getElementValueArray(
                dqListAnno, defaultQualifierListValueElement, AnnotationMirror.class);
        for (AnnotationMirror dqAnno : values) {
          Set<Default> p = fromDefaultQualifier(dqAnno);
          if (p != null) {
            qualifierDefaults.addAll(p);
          }
        }
      }
    }

    Element parent;
    if (elt.getKind() == ElementKind.PACKAGE) {
      parent = ElementUtils.parentPackage((PackageElement) elt, elements);
    } else {
      parent = elt.getEnclosingElement();
    }

    ScopeDefaults parentDefaults = defaultsAt(parent);
    ScopeDefaults result =
        ScopeDefaults.of(
            concat(elementDefaults, parentDefaults.elementDefaults),
            concat(qualifierDefaults, parentDefaults.qualifierDefaults));

    // Memoize the empty answer as well as a non-empty one: most elements have no applicable
    // default, and recomputing that walks the whole chain of enclosing scopes every time.
    // Do not memoize while an annotation file is being parsed, because getDeclAnnotation can
    // return null for an element whose annotation file has not been read yet.
    if (atypeFactory.shouldCache && !atypeFactory.isParsingAnnotationFiles()) {
      defaultsAtCache.put(elt, result);
    }

    return result;
  }

  /**
   * Returns the defaults of a scope, followed by the defaults that the scope inherits from its
   * enclosing scopes. A default of the scope itself comes first, so that it takes precedence over a
   * default of an enclosing scope.
   *
   * @param scopeDefaults the defaults of the scope itself, or null if the scope has none
   * @param enclosingDefaults the defaults of the enclosing scopes, nearest scope first
   * @return the defaults that apply to the scope, nearest scope first
   */
  private static List<Default> concat(
      @Nullable DefaultSet scopeDefaults, List<Default> enclosingDefaults) {
    if (scopeDefaults == null || scopeDefaults.isEmpty()) {
      return enclosingDefaults;
    }
    List<Default> result = new ArrayList<>(scopeDefaults.size() + enclosingDefaults.size());
    result.addAll(scopeDefaults);
    result.addAll(enclosingDefaults);
    return result;
  }

  /**
   * Given an element, returns true if the conservative default should be applied for it. Handles
   * elements from bytecode or source code.
   *
   * @param annotationScope the element that the conservative default might apply to
   * @return true if the conservative default applies to the given element
   */
  public boolean applyConservativeDefaults(Element annotationScope) {
    if (annotationScope == null) {
      return false;
    }

    if (!useConservativeDefaultsBytecode && !useConservativeDefaultsSource) {
      // Every path below returns false when both flags are false, and the checks below are
      // expensive.
      return false;
    }

    if (uncheckedCodeDefaults.isEmpty()) {
      return false;
    }

    // TODO: I would expect this:
    //   atypeFactory.isFromByteCode(annotationScope)) {
    // to work instead of the
    // isElementFromByteCode/declarationFromElement/isFromStubFile calls,
    // but it doesn't work correctly and tests fail.

    boolean isFromStubFile = atypeFactory.isFromStubFile(annotationScope);
    boolean isBytecode =
        ElementUtils.isElementFromByteCode(annotationScope)
            && atypeFactory.declarationFromElement(annotationScope) == null
            && !isFromStubFile;
    if (isBytecode) {
      return useConservativeDefaultsBytecode && !isElementAnnotatedForThisChecker(annotationScope);
    } else if (isFromStubFile) {
      // TODO: Types in stub files not annotated for a particular checker should be
      // treated as unchecked bytecode.  For now, all types in stub files are treated as
      // checked code. Eventually, @AnnotateFor(checker) will be programmatically added
      // to methods in stub files supplied via the @Stubfile annotation.  Stub files will
      // be treated like unchecked code except for methods in the scope for an @AnnotatedFor.
      return false;
    } else if (useConservativeDefaultsSource) {
      return !isElementAnnotatedForThisChecker(annotationScope);
    }
    return false;
  }

  /**
   * Applies default annotations to a type. Conservative defaults are applied first as appropriate,
   * followed by source code defaults.
   *
   * <p>For a discussion on the rules for application of source code and conservative defaults,
   * please see the linked manual sections.
   *
   * @param annotationScope the element representing the nearest enclosing default annotation scope
   *     for the type
   * @param type the type to which defaults will be applied
   * @checker_framework.manual #effective-qualifier The effective qualifier on a type (defaults and
   *     inference)
   * @checker_framework.manual #annotating-libraries Annotating libraries
   */
  private void applyDefaultsElement(Element annotationScope, AnnotatedTypeMirror type) {
    PrecedenceList defaults = precedenceList(annotationScope);
    DefaultApplierElement applier =
        createDefaultApplierElement(atypeFactory, annotationScope, type, applyToTypeVar);
    applier.applyDefaults(defaults);
  }

  /**
   * Returns every default that applies to the given scope, in the order in which the defaults are
   * to be applied: first the defaults that {@link #addElementDefault} registered for the scope or
   * for one of its enclosing scopes, together with the {@code @DefaultQualifier} annotations on the
   * scope and on its enclosing scopes; then the conservative defaults if conservative defaults
   * apply to the scope; and last the checked code defaults.
   *
   * <p>In the first group, a registered default precedes a {@code @DefaultQualifier} annotation,
   * and a default of a nearer scope precedes a default of an enclosing scope, whatever the
   * locations of the two defaults. For example, a {@code @DefaultQualifier} for {@link
   * TypeUseLocation#ALL} on a method precedes a {@code @DefaultQualifier} for {@link
   * TypeUseLocation#RETURN} on the enclosing class. The defaults of any one scope are ordered by
   * {@link TypeUseLocation}. The exception is {@link TypeUseLocation#OTHERWISE}, which means "apply
   * if nothing more concrete is provided": every {@link TypeUseLocation#OTHERWISE} default comes
   * after every other default of the first group, whatever scope each comes from.
   *
   * <p>The result is memoized, and callers must not modify the result.
   *
   * @param annotationScope the element representing the nearest enclosing default annotation scope
   *     for the type, or null
   * @return the defaults that apply to {@code annotationScope}, in the order to apply them
   */
  protected PrecedenceList precedenceList(@Nullable Element annotationScope) {
    if (annotationScope == null) {
      return getCheckedCodeDefaultsArray();
    }

    PrecedenceList cached = precedenceListCache.get(annotationScope);
    if (cached != null) {
      return cached;
    }

    ScopeDefaults scopeDefaults = defaultsAt(annotationScope);
    boolean conservative = applyConservativeDefaults(annotationScope);

    if (scopeDefaults.isEmpty()) {
      // The common case.  Return a shared array, so that the overwhelming majority of scopes
      // need no per-scope storage at all.
      return conservative ? getUncheckedThenCheckedArray() : getCheckedCodeDefaultsArray();
    }

    List<Default> list = new ArrayList<>(scopeDefaults.elementDefaults);
    list.addAll(scopeDefaults.qualifierDefaults);
    // Move the OTHERWISE defaults to the end, so that any more concrete default takes precedence
    // even when the OTHERWISE default belongs to a nearer scope or was registered.  The sort is
    // stable, so otherwise the order above decides: nearer scopes first.
    list.sort(OTHERWISE_LAST);
    if (conservative) {
      list.addAll(uncheckedCodeDefaults);
    }
    list.addAll(checkedCodeDefaults);
    PrecedenceList result = makePrecedenceList(list);

    if (atypeFactory.shouldCache && !atypeFactory.isParsingAnnotationFiles()) {
      precedenceListCache.put(annotationScope, result);
    }
    return result;
  }

  /** Orders the defaults for {@link TypeUseLocation#OTHERWISE} after all other defaults. */
  private static final Comparator<Default> OTHERWISE_LAST =
      Comparator.comparing(def -> def.location == TypeUseLocation.OTHERWISE);

  /**
   * Returns the precedence list for a scope that has no element defaults and to which conservative
   * defaults do not apply.
   *
   * @return the precedence list consisting of just the checked code defaults
   */
  private PrecedenceList getCheckedCodeDefaultsArray() {
    if (checkedCodeDefaultsArray == null) {
      checkedCodeDefaultsArray = makePrecedenceList(new ArrayList<>(checkedCodeDefaults));
    }
    return checkedCodeDefaultsArray;
  }

  /**
   * Returns the precedence list for a scope that has no element defaults and to which conservative
   * defaults apply.
   *
   * @return the precedence list consisting of the unchecked code defaults then the checked code
   *     defaults
   */
  private PrecedenceList getUncheckedThenCheckedArray() {
    if (uncheckedThenCheckedArray == null) {
      List<Default> list = new ArrayList<>(uncheckedCodeDefaults);
      list.addAll(checkedCodeDefaults);
      uncheckedThenCheckedArray = makePrecedenceList(list);
    }
    return uncheckedThenCheckedArray;
  }

  /**
   * Creates a {@link PrecedenceList} for the defaults that apply to a scope.
   *
   * @param defaults every default that applies to the scope, in the order in which to apply them
   * @return a precedence list for {@code defaults}
   */
  private PrecedenceList makePrecedenceList(List<Default> defaults) {
    return new PrecedenceList(defaults, minimizeDefaults(defaults));
  }

  /**
   * The defaults that apply to a scope, other than the checked code defaults and the conservative
   * defaults: those that {@link QualifierDefaults#addElementDefault} registered and those written
   * as {@code @DefaultQualifier}.
   *
   * <p>Each is a list rather than a {@link DefaultSet}, and the two are kept apart, because both
   * distinctions decide precedence: a registered default takes precedence over a
   * {@code @DefaultQualifier} annotation, and a default of a scope takes precedence over a default
   * of an enclosing scope. Putting them all in one {@link DefaultSet} would instead let the
   * locations and then the annotations' names decide, since a {@link DefaultSet} is sorted by
   * location and then by annotation name.
   */
  private static class ScopeDefaults {

    /** No defaults at all. */
    public static final ScopeDefaults EMPTY =
        new ScopeDefaults(Collections.emptyList(), Collections.emptyList());

    /**
     * The defaults that {@link QualifierDefaults#addElementDefault} registered for the scope or for
     * one of its enclosing scopes, nearest scope first. Callers must not modify it.
     */
    public final List<Default> elementDefaults;

    /**
     * The defaults written as {@code @DefaultQualifier} on the scope or on one of its enclosing
     * scopes, nearest scope first. Callers must not modify it.
     */
    public final List<Default> qualifierDefaults;

    /**
     * Creates a ScopeDefaults.
     *
     * @param elementDefaults the registered defaults, nearest scope first
     * @param qualifierDefaults the {@code @DefaultQualifier} defaults, nearest scope first
     */
    private ScopeDefaults(List<Default> elementDefaults, List<Default> qualifierDefaults) {
      this.elementDefaults = elementDefaults;
      this.qualifierDefaults = qualifierDefaults;
    }

    /**
     * Returns a ScopeDefaults for the given defaults, or {@link #EMPTY} if there are none.
     *
     * @param elementDefaults the registered defaults, nearest scope first
     * @param qualifierDefaults the {@code @DefaultQualifier} defaults, nearest scope first
     * @return a ScopeDefaults for the given defaults
     */
    public static ScopeDefaults of(List<Default> elementDefaults, List<Default> qualifierDefaults) {
      if (elementDefaults.isEmpty() && qualifierDefaults.isEmpty()) {
        return EMPTY;
      }
      return new ScopeDefaults(elementDefaults, qualifierDefaults);
    }

    /**
     * Returns true if no default applies to the scope.
     *
     * @return true if no default applies to the scope
     */
    public boolean isEmpty() {
      return elementDefaults.isEmpty() && qualifierDefaults.isEmpty();
    }
  }

  /**
   * A precedence list: every {@link Default} that applies to some scope, in the order in which to
   * apply them, together with whether applying them all in one traversal of the type gives the same
   * result as traversing the type once per {@link Default}.
   *
   * <p>The two orders differ only when a {@link Default} that annotates a node below the top level
   * of the type precedes a {@link Default} that annotates, from the top-level node, some node other
   * than the top-level node. Within a single {@link DefaultSet} that cannot happen, because a
   * {@link DefaultSet} is sorted by {@link TypeUseLocation} and the cross-node locations all
   * precede the descendant locations. It can happen where two {@link DefaultSet}s are concatenated,
   * because the second one starts over at a low {@link TypeUseLocation}.
   */
  protected static class PrecedenceList {

    /**
     * The defaults, in the order in which to apply them, without the ones that {@link
     * QualifierDefaults#minimizeDefaults} removed. Callers must not modify this array.
     */
    public final Default[] defaults;

    /**
     * Every default that applies to the scope, in the order in which to apply them. This differs
     * from {@link #defaults} only if {@link QualifierDefaults#minimizeDefaults} removed something.
     * It is used for an applier that customizes how a default is applied, for which the assumptions
     * that justify removing a default do not hold. Callers must not modify this array.
     */
    public final Default[] allDefaults;

    /** True if one traversal of the type suffices for all of {@link #defaults}. */
    public final boolean singlePassSafe;

    /**
     * Creates a PrecedenceList.
     *
     * @param allDefaults every default that applies to the scope, in the order in which to apply
     *     them
     * @param defaults {@code allDefaults} without the entries that {@link
     *     QualifierDefaults#minimizeDefaults} removed; its elements must appear in {@code
     *     allDefaults}, in the same order
     */
    public PrecedenceList(List<Default> allDefaults, List<Default> defaults) {
      this.defaults = defaults.toArray(new Default[0]);
      // minimizeDefaults only removes entries, so equal sizes mean equal contents.
      this.allDefaults =
          allDefaults.size() == defaults.size()
              ? this.defaults
              : allDefaults.toArray(new Default[0]);
      boolean safe = true;
      boolean sawDescendantLocation = false;
      for (Default def : this.defaults) {
        if (sawDescendantLocation && CROSS_NODE_LOCATIONS.contains(def.location)) {
          safe = false;
          break;
        }
        if (DESCENDANT_LOCATIONS.contains(def.location)) {
          sawDescendantLocation = true;
        }
      }
      this.singlePassSafe = safe;
    }
  }

  /**
   * Removes from a precedence list every {@link Default} that a preceding {@link Default} makes
   * redundant, namely one whose location and qualifier hierarchy both already appeared.
   *
   * <p>Dropping such a {@link Default} does not change the result. Two {@link Default}s with the
   * same location reach {@link DefaultApplierElement#addAnnotation} at exactly the same nodes,
   * because which nodes a scan annotates depends on the location, the scope, and the structure of
   * the type, but never on the qualifier. {@code addAnnotation} fills a hierarchy only when that
   * hierarchy is empty, and the two {@link Default}s are in the same hierarchy, so the later one is
   * a no-op. Keeping the earlier one preserves which qualifier wins.
   *
   * <p>A {@link Default} whose qualifier is not a qualifier of this type system is never redundant,
   * because applying it does nothing at all.
   *
   * <p>This method removes nothing if the type system's canonicalization depends on the type being
   * annotated, because then which hierarchy a {@link Default} lands in is not known here. An
   * applier that overrides {@link DefaultApplierElement#addAnnotation} does not use this method's
   * result; see {@link DefaultApplierElement#applyDefaults}.
   *
   * @param defaults a precedence list
   * @return the precedence list, without the redundant entries
   */
  protected List<Default> minimizeDefaults(List<Default> defaults) {
    if (canonicalAnnotationIsTypeDependent()) {
      // Two defaults that are in the same hierarchy here might be in different hierarchies at
      // some node of the type, where each of them would be applied, so neither is redundant.
      return defaults;
    }
    QualifierHierarchy qualHierarchy = atypeFactory.getQualifierHierarchy();
    EnumMap<TypeUseLocation, AnnotationMirrorSet> seen = new EnumMap<>(TypeUseLocation.class);
    List<Default> result = new ArrayList<>(defaults.size());
    for (Default def : defaults) {
      AnnotationMirror anno = atypeFactory.canonicalAnnotation(def.anno);
      if (!atypeFactory.isSupportedQualifier(anno)) {
        // The qualifier is in no hierarchy of this type system, so it has no top and applying
        // it is a no-op.  Retain it rather than crashing in getTopAnnotation.
        result.add(def);
        continue;
      }
      AnnotationMirror top = qualHierarchy.getTopAnnotation(anno);
      AnnotationMirrorSet tops =
          seen.computeIfAbsent(def.location, __ -> new AnnotationMirrorSet());
      if (tops.add(top)) {
        result.add(def);
      }
    }
    return result;
  }

  /**
   * For each class of applier that has been used, whether it overrides {@link
   * DefaultApplierElement#applyDefault} or {@link DefaultApplierElement#addAnnotation}.
   */
  private static final Map<Class<?>, Boolean> applierIsCustomizedCache = new ConcurrentHashMap<>();

  /**
   * Returns true if the given class overrides {@link DefaultApplierElement#applyDefault}, {@link
   * DefaultApplierElement#addAnnotation}, or {@link DefaultApplierElement#shouldBeAnnotated}, any
   * of which may apply a default other than in the way that {@link #minimizeDefaults} and the
   * single-traversal optimization assume. For example, an override of {@code shouldBeAnnotated} may
   * read {@link DefaultApplierElement#location}, which holds a single location only when the type
   * is traversed once per default. Such an applier is given every default and one traversal of the
   * type per default.
   *
   * @param applierClass {@link DefaultApplierElement} or a subclass of it
   * @return true if {@code applierClass} overrides {@code applyDefault}, {@code addAnnotation}, or
   *     {@code shouldBeAnnotated}
   */
  private static boolean applierIsCustomized(Class<?> applierClass) {
    return applierIsCustomizedCache.computeIfAbsent(
        applierClass,
        c ->
            isOverridden(c, DefaultApplierElement.class, "applyDefault", Default.class)
                || isOverridden(
                    c,
                    DefaultApplierElement.class,
                    "addAnnotation",
                    AnnotatedTypeMirror.class,
                    AnnotationMirror.class)
                || isOverridden(
                    c,
                    DefaultApplierElement.class,
                    "shouldBeAnnotated",
                    AnnotatedTypeMirror.class,
                    boolean.class));
  }

  /**
   * For each type factory class that has been used, whether it overrides {@link
   * AnnotatedTypeFactory#canonicalAnnotation(AnnotationMirror, TypeMirror)}.
   */
  private static final Map<Class<?>, Boolean> canonicalAnnotationIsTypeDependentCache =
      new ConcurrentHashMap<>();

  /**
   * Returns true if this type system canonicalizes an annotation differently depending on the type
   * that the annotation is applied to; that is, if the type factory overrides {@link
   * AnnotatedTypeFactory#canonicalAnnotation(AnnotationMirror, TypeMirror)}. If it does, then the
   * qualifier hierarchy that a {@link Default} lands in cannot be determined from the {@link
   * Default} alone.
   *
   * @return true if canonicalization depends on the type being annotated
   */
  private boolean canonicalAnnotationIsTypeDependent() {
    return canonicalAnnotationIsTypeDependentCache.computeIfAbsent(
        atypeFactory.getClass(),
        c ->
            isOverridden(
                c,
                AnnotatedTypeFactory.class,
                "canonicalAnnotation",
                AnnotationMirror.class,
                TypeMirror.class));
  }

  /**
   * Returns true if the given method is overridden; that is, if some class that is {@code subclass}
   * or a proper subclass of {@code baseClass} declares the method.
   *
   * @param subclass {@code baseClass} or a subclass of it
   * @param baseClass the class that declares the method being overridden
   * @param methodName the name of the method
   * @param parameterTypes the erased parameter types of the method
   * @return true if {@code subclass} overrides the method
   */
  private static boolean isOverridden(
      Class<?> subclass, Class<?> baseClass, String methodName, Class<?>... parameterTypes) {
    for (Class<?> c = subclass; c != null && c != baseClass; c = c.getSuperclass()) {
      try {
        Method unused = c.getDeclaredMethod(methodName, parameterTypes);
        return true;
      } catch (NoSuchMethodException e) {
        // Class c does not declare the method; look in the superclass of c.
      }
    }
    return false;
  }

  /**
   * Returns a new {@link DefaultApplierElement}. A subclass can override this method to use a
   * subclass of {@link DefaultApplierElement}.
   *
   * @param atypeFactory the type factory
   * @param annotationScope the element whose defaults are being applied
   * @param type the type to which to apply defaults
   * @param applyToTypeVar true if the default should be applied to the primary annotation of a
   *     local variable whose type is a type variable
   * @return a new {@link DefaultApplierElement}
   */
  protected DefaultApplierElement createDefaultApplierElement(
      AnnotatedTypeFactory atypeFactory,
      Element annotationScope,
      AnnotatedTypeMirror type,
      boolean applyToTypeVar) {
    return new DefaultApplierElement(atypeFactory, annotationScope, type, applyToTypeVar);
  }

  /** A default applier element. */
  protected class DefaultApplierElement {

    /** The annotated type factory. */
    protected final AnnotatedTypeFactory atypeFactory;

    /** The scope of the default. */
    protected final Element scope;

    /** The type to which to apply the default. */
    protected final AnnotatedTypeMirror type;

    /**
     * Location of the default currently being applied. (Should only be set by {@link
     * #applyDefaults}.)
     */
    protected TypeUseLocation location;

    /** The defaults to apply at each node. (Should only be set by {@link #applyDefaults}.) */
    protected Default[] defaults = new Default[0];

    /** Reused by {@link #applyDefault}, so that one default costs no array allocation. */
    private final Default[] singletonDefaults = new Default[1];

    /** The default element applier implementation. */
    protected final DefaultApplierElementImpl impl;

    /*
      Local type variables are defaulted to top when flow is turned on
      We only want to default the top level type variable (and not type variables that are nested
      in its bounds). E.g.,
        <T extends List<E>, E extends Object> void method() {
           T t;
        }
      We would like t to have its primary annotation defaulted but NOT the E inside its upper bound.
      we use referential equality with the top level type var to determine which ones are definite
      type uses, i.e. uses which can be defaulted
    */
    private final AnnotatedTypeVariable defaultableTypeVar;

    public DefaultApplierElement(
        AnnotatedTypeFactory atypeFactory,
        Element scope,
        AnnotatedTypeMirror type,
        boolean applyToTypeVar) {
      this.atypeFactory = atypeFactory;
      this.scope = scope;
      this.type = type;
      this.impl = new DefaultApplierElementImpl();
      this.defaultableTypeVar = applyToTypeVar ? (AnnotatedTypeVariable) type : null;
    }

    /**
     * Apply default to the type.
     *
     * @param def default to apply
     */
    public void applyDefault(Default def) {
      // Set the location before the traversal, because shouldBeAnnotated may read it.
      this.location = def.location;
      singletonDefaults[0] = def;
      this.defaults = singletonDefaults;
      impl.visit(type, null);
    }

    /**
     * Apply every default in a precedence list to the type.
     *
     * <p>When the precedence list permits, this traverses the type once and applies every default
     * at each node, rather than traversing the type once per default.
     *
     * <p>It does not do so if this object's class overrides {@link #applyDefault}, {@link
     * #addAnnotation}, or {@link #shouldBeAnnotated}. Such an applier is instead given one
     * traversal of the type per default, so that the override runs for each default, and is given
     * even the defaults that {@link QualifierDefaults#minimizeDefaults} removed, because the
     * override may not have the semantics that removing them assumes.
     *
     * @param precedenceList the defaults to apply, in the order in which to apply them
     */
    public void applyDefaults(PrecedenceList precedenceList) {
      if (applierIsCustomized(getClass())) {
        for (Default def : precedenceList.allDefaults) {
          applyDefault(def);
        }
      } else if (precedenceList.singlePassSafe) {
        this.defaults = precedenceList.defaults;
        impl.visit(type, null);
      } else {
        for (Default def : precedenceList.defaults) {
          applyDefault(def);
        }
      }
    }

    /**
     * Returns true if the given qualifier should be applied to the given type. Currently we do not
     * apply defaults to void types, packages, wildcards, and type variables.
     *
     * @param type type to which qual would be applied
     * @return true if this application should proceed
     */
    protected boolean shouldBeAnnotated(AnnotatedTypeMirror type, boolean applyToTypeVar) {

      return !(type == null
          // TODO: executables themselves should not be annotated
          // For some reason h1h2checker-tests fails with this.
          // || type.getKind() == TypeKind.EXECUTABLE
          || type.getKind() == TypeKind.NONE
          || type.getKind() == TypeKind.WILDCARD
          || (type.getKind() == TypeKind.TYPEVAR && !applyToTypeVar)
          || type instanceof AnnotatedNoType);
    }

    /**
     * Add the qualifier to the type if it does not already have an annotation in the same hierarchy
     * as qual.
     *
     * @param type type to add qual
     * @param qual annotation to add
     */
    protected void addAnnotation(AnnotatedTypeMirror type, AnnotationMirror qual) {
      // Add the default annotation, but only if no other annotation is present.
      if (type.getKind() != TypeKind.EXECUTABLE) {
        type.addMissingAnnotation(qual);
      }
    }

    /**
     * Applies {@link DefaultApplierElement#defaults} at every node of a type, where each default
     * applies.
     */
    protected class DefaultApplierElementImpl extends AnnotatedTypeScanner<Void, Void> {

      /** Creates a {@code DefaultApplierElementImpl}. */
      protected DefaultApplierElementImpl() {}

      @Override
      public Void scan(@FindDistinct AnnotatedTypeMirror t, Void p) {
        if (!shouldBeAnnotated(t, t == defaultableTypeVar)) {
          return super.scan(t, p);
        }

        // Some defaults only apply to the top level type.
        boolean isTopLevelType = t == type;
        for (Default def : defaults) {
          location = def.location;
          applyDefaultAtNode(t, def.anno, isTopLevelType);
        }

        return super.scan(t, p);
      }

      /**
       * Apply one default at one node of the type, without traversing below the node.
       *
       * @param t the node
       * @param qual the default's qualifier
       * @param isTopLevelType true if {@code t} is the type that defaults are being applied to
       */
      protected void applyDefaultAtNode(
          @FindDistinct AnnotatedTypeMirror t, AnnotationMirror qual, boolean isTopLevelType) {
        switch (location) {
          case FIELD -> {
            if (scope != null && scope.getKind() == ElementKind.FIELD && isTopLevelType) {
              addAnnotation(t, qual);
            }
          }
          case LOCAL_VARIABLE -> {
            if (scope != null && scope.getKind() == ElementKind.LOCAL_VARIABLE && isTopLevelType) {
              // TODO: how do we determine that we are in a cast or instanceof type?
              addAnnotation(t, qual);
            }
          }
          case RESOURCE_VARIABLE -> {
            if (scope != null
                && scope.getKind() == ElementKind.RESOURCE_VARIABLE
                && isTopLevelType) {
              addAnnotation(t, qual);
            }
          }
          case EXCEPTION_PARAMETER -> {
            if (scope != null
                && scope.getKind() == ElementKind.EXCEPTION_PARAMETER
                && isTopLevelType) {
              addAnnotation(t, qual);
              if (t.getKind() == TypeKind.UNION) {
                AnnotatedUnionType aut = (AnnotatedUnionType) t;
                // Also apply the default to the alternative types
                for (AnnotatedDeclaredType anno : aut.getAlternatives()) {
                  addAnnotation(anno, qual);
                }
              }
            }
          }
          case PARAMETER -> {
            if (scope != null && scope.getKind() == ElementKind.PARAMETER && isTopLevelType) {
              addAnnotation(t, qual);
            } else if (scope != null
                && (scope.getKind() == ElementKind.METHOD
                    || scope.getKind() == ElementKind.CONSTRUCTOR)
                && t.getKind() == TypeKind.EXECUTABLE
                && isTopLevelType) {

              for (AnnotatedTypeMirror atm : ((AnnotatedExecutableType) t).getParameterTypes()) {
                if (shouldBeAnnotated(atm, false)) {
                  addAnnotation(atm, qual);
                }
              }
            }
          }
          case RECEIVER -> {
            if (scope != null
                && scope.getKind() == ElementKind.PARAMETER
                && isTopLevelType
                && scope.getSimpleName().contentEquals("this")) {
              // TODO: comparison against "this" is ugly, won't work
              // for all possible names for receiver parameter.
              // Comparison to Names._this might be a bit faster.
              addAnnotation(t, qual);
            } else if (scope != null
                && (scope.getKind() == ElementKind.METHOD)
                && t.getKind() == TypeKind.EXECUTABLE
                && isTopLevelType) {

              AnnotatedDeclaredType receiver = ((AnnotatedExecutableType) t).getReceiverType();
              if (shouldBeAnnotated(receiver, false)) {
                addAnnotation(receiver, qual);
              }
            }
          }
          case RETURN -> {
            if (scope != null
                && scope.getKind() == ElementKind.METHOD
                && t.getKind() == TypeKind.EXECUTABLE
                && isTopLevelType) {
              AnnotatedTypeMirror returnType = ((AnnotatedExecutableType) t).getReturnType();
              if (shouldBeAnnotated(returnType, false)) {
                addAnnotation(returnType, qual);
              }
            }
          }
          case CONSTRUCTOR_RESULT -> {
            if (scope != null
                && scope.getKind() == ElementKind.CONSTRUCTOR
                && t.getKind() == TypeKind.EXECUTABLE
                && isTopLevelType) {
              // This is the return type of a constructor declaration (not a
              // constructor invocation).
              AnnotatedTypeMirror returnType = ((AnnotatedExecutableType) t).getReturnType();
              if (shouldBeAnnotated(returnType, false)) {
                addAnnotation(returnType, qual);
              }
            }
          }
          case IMPLICIT_LOWER_BOUND -> {
            if (isLowerBound && boundType.isOneOf(BoundType.UNBOUNDED, BoundType.UPPER)) {
              addAnnotation(t, qual);
            }
          }
          case EXPLICIT_LOWER_BOUND -> {
            if (isLowerBound && boundType.isOneOf(BoundType.LOWER)) {
              addAnnotation(t, qual);
            }
          }
          case LOWER_BOUND -> {
            if (isLowerBound) {
              addAnnotation(t, qual);
            }
          }
          case IMPLICIT_UPPER_BOUND -> {
            if (isUpperBound && boundType.isOneOf(BoundType.UNBOUNDED, BoundType.LOWER)) {
              addAnnotation(t, qual);
            }
          }
          case EXPLICIT_UPPER_BOUND -> {
            if (isUpperBound && boundType.isOneOf(BoundType.UPPER)) {
              addAnnotation(t, qual);
            }
          }
          case UPPER_BOUND -> {
            if (this.isUpperBound) {
              addAnnotation(t, qual);
            }
          }
          case OTHERWISE, ALL ->
              // TODO: forbid ALL if anything else was given.
              addAnnotation(t, qual);
          default ->
              throw new BugInCF(
                  "QualifierDefaults.DefaultApplierElement: unhandled location: " + location);
        }
      }

      @Override
      public void reset() {
        super.reset();
        impl.isLowerBound = false;
        impl.isUpperBound = false;
        impl.boundType = BoundType.UNBOUNDED;
      }

      // are we currently defaulting the lower bound of a type variable or wildcard
      private boolean isLowerBound = false;

      // are we currently defaulting the upper bound of a type variable or wildcard
      private boolean isUpperBound = false;

      // the bound type of the current wildcard or type variable being defaulted
      private BoundType boundType = BoundType.UNBOUNDED;

      @Override
      public Void visitTypeVariable(AnnotatedTypeVariable type, Void p) {
        if (visitedNodes.containsKey(type)) {
          return visitedNodes.get(type);
        }

        visitBounds(type, type.getUpperBound(), type.getLowerBound(), p);
        return null;
      }

      @Override
      public Void visitWildcard(AnnotatedWildcardType type, Void p) {
        if (visitedNodes.containsKey(type)) {
          return visitedNodes.get(type);
        }

        visitBounds(type, type.getExtendsBound(), type.getSuperBound(), p);
        return null;
      }

      /**
       * Visit the bounds of a type variable or a wildcard and potentially apply qual to those
       * bounds. This method will also update the boundType, isLowerBound, and isUpperbound fields.
       */
      protected void visitBounds(
          AnnotatedTypeMirror boundedType,
          AnnotatedTypeMirror upperBound,
          AnnotatedTypeMirror lowerBound,
          Void p) {

        boolean prevIsUpperBound = isUpperBound;
        boolean prevIsLowerBound = isLowerBound;
        BoundType prevBoundType = boundType;

        boundType = getBoundType(boundedType);

        try {
          isLowerBound = true;
          isUpperBound = false;
          scanAndReduce(lowerBound, p, null);

          visitedNodes.put(type, null);

          isLowerBound = false;
          isUpperBound = true;
          scanAndReduce(upperBound, p, null);

          visitedNodes.put(type, null);

        } finally {
          isUpperBound = prevIsUpperBound;
          isLowerBound = prevIsLowerBound;
          boundType = prevBoundType;
        }
      }
    }
  }

  /**
   * Specifies whether the type variable or wildcard has an explicit upper bound (UPPER), an
   * explicit lower bound (LOWER), or no explicit bounds (UNBOUNDED).
   */
  protected enum BoundType {

    /** Indicates an upper-bounded type variable or wildcard. */
    UPPER,

    /** Indicates a lower-bounded type variable or wildcard. */
    LOWER,

    /**
     * Neither bound is specified, BOTH are implicit. (If a type variable is declared in bytecode
     * and the type of the upper bound is Object, then the checker assumes that the bound was not
     * explicitly written in source code.)
     */
    UNBOUNDED;

    public boolean isOneOf(BoundType... choices) {
      for (BoundType choice : choices) {
        if (this == choice) {
          return true;
        }
      }

      return false;
    }
  }

  /**
   * Returns the boundType for type.
   *
   * @param type the type whose boundType is returned. type must be an AnnotatedWildcardType or
   *     AnnotatedTypeVariable.
   * @return the boundType for type
   */
  private BoundType getBoundType(AnnotatedTypeMirror type) {
    if (type instanceof AnnotatedTypeVariable atv) {
      return getTypeVarBoundType(atv);
    }

    if (type instanceof AnnotatedWildcardType awt) {
      return getWildcardBoundType(awt);
    }

    throw new BugInCF("Unexpected type kind: type=" + type);
  }

  /**
   * Returns the bound type of the input typeVar.
   *
   * @param typeVar the type variable
   * @return the bound type of the input typeVar
   */
  private BoundType getTypeVarBoundType(AnnotatedTypeVariable typeVar) {
    return getTypeVarBoundType((TypeParameterElement) typeVar.getUnderlyingType().asElement());
  }

  /**
   * Returns the boundType (UPPER or UNBOUNDED) of the declaration of typeParamElem.
   *
   * @param typeParamElem the type parameter element
   * @return the boundType (UPPER or UNBOUNDED) of the declaration of typeParamElem
   */
  // Results are cached in {@link #elementToBoundType}.
  private BoundType getTypeVarBoundType(TypeParameterElement typeParamElem) {
    BoundType prev = elementToBoundType.get(typeParamElem);
    if (prev != null) {
      return prev;
    }

    TreePath declaredTypeVarEle = atypeFactory.getTreeUtils().getPath(typeParamElem);
    Tree typeParamDecl = declaredTypeVarEle == null ? null : declaredTypeVarEle.getLeaf();

    final BoundType boundType;
    if (typeParamDecl == null) {
      // This is not only for elements from binaries, but also
      // when the compilation unit is no-longer available.
      if (typeParamElem.getBounds().size() == 1
          && TypesUtils.isObject(typeParamElem.getBounds().get(0))) {
        // If the bound was Object, then it may or may not have been explicitly written.
        // Assume that it was not.
        boundType = BoundType.UNBOUNDED;
      } else {
        // The bound is not Object, so it must have been explicitly written and thus the
        // type variable has an upper bound.
        boundType = BoundType.UPPER;
      }

    } else {
      if (typeParamDecl instanceof TypeParameterTree tptree) {

        List<? extends Tree> bnds = tptree.getBounds();
        if (bnds != null && !bnds.isEmpty()) {
          boundType = BoundType.UPPER;
        } else {
          boundType = BoundType.UNBOUNDED;
        }
      } else {
        throw new BugInCF(
            StringsP.joinLines(
                "Unexpected tree type for typeVar Element:",
                "typeParamElem=" + typeParamElem,
                typeParamDecl));
      }
    }

    elementToBoundType.put(typeParamElem, boundType);
    return boundType;
  }

  /**
   * Returns the BoundType of annotatedWildcard. If it is unbounded, use the type parameter to which
   * its an argument.
   *
   * @param wildcardType the annotated wildcard type
   * @return the BoundType of annotatedWildcard. If it is unbounded, use the type parameter to which
   *     its an argument
   */
  public BoundType getWildcardBoundType(AnnotatedWildcardType wildcardType) {
    if (AnnotatedTypes.hasNoExplicitBound(wildcardType)) {
      TypeParameterElement e = TypesUtils.wildcardToTypeParam(wildcardType.getUnderlyingType());
      if (e != null) {
        return getTypeVarBoundType(e);
      } else {
        return BoundType.UNBOUNDED;
      }
    } else if (AnnotatedTypes.hasExplicitSuperBound(wildcardType)) {
      return BoundType.LOWER;
    } else {
      return BoundType.UPPER;
    }
  }
}
