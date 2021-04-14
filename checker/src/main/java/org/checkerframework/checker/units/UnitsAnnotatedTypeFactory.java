package org.checkerframework.checker.units;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiers;
import org.checkerframework.checker.units.qual.MixedUnits;
import org.checkerframework.checker.units.qual.Prefix;
import org.checkerframework.checker.units.qual.UnitsBottom;
import org.checkerframework.checker.units.qual.UnitsMultiple;
import org.checkerframework.checker.units.qual.UnknownUnits;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFormatter;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotationClassLoader;
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.DefaultQualifierKindHierarchy;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.framework.util.QualifierKindHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.UserError;
import org.plumelib.reflection.Signatures;

/**
 * Annotated type factory for the Units Checker.
 *
 * <p>Handles multiple names for the same unit, with different prefixes, e.g. @kg is the same
 * as @g(Prefix.kilo).
 *
 * <p>Supports relations between units, e.g. if "m" is a variable of type "@m" and "s" is a variable
 * of type "@s", the division "m/s" is automatically annotated as "mPERs", the correct unit for the
 * result.
 */
public class UnitsAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
  private static final Class<org.checkerframework.checker.units.qual.UnitsRelations>
      unitsRelationsAnnoClass = org.checkerframework.checker.units.qual.UnitsRelations.class;

  protected final AnnotationMirror mixedUnits =
      AnnotationBuilder.fromClass(elements, MixedUnits.class);
  protected final AnnotationMirror TOP = AnnotationBuilder.fromClass(elements, UnknownUnits.class);
  protected final AnnotationMirror BOTTOM =
      AnnotationBuilder.fromClass(elements, UnitsBottom.class);

  /** The UnitsMultiple.prefix argument/element. */
  private final ExecutableElement unitsMultiplePrefixElement =
      TreeUtils.getMethod(UnitsMultiple.class, "prefix", 0, processingEnv);
  /** The UnitsMultiple.quantity argument/element. */
  private final ExecutableElement unitsMultipleQuantityElement =
      TreeUtils.getMethod(UnitsMultiple.class, "quantity", 0, processingEnv);
  /** The UnitsRelations.value argument/element. */
  private final ExecutableElement unitsRelationsValueElement =
      TreeUtils.getMethod(
          org.checkerframework.checker.units.qual.UnitsRelations.class, "value", 0, processingEnv);

  /**
   * Map from canonical class name to the corresponding UnitsRelations instance. We use the string
   * to prevent instantiating the UnitsRelations multiple times.
   */
  private Map<@CanonicalName String, UnitsRelations> unitsRel;

  /** Map from canonical name of external qualifiers, to their Class. */
  private static final Map<@CanonicalName String, Class<? extends Annotation>> externalQualsMap =
      new HashMap<>();

  private static final Map<String, AnnotationMirror> aliasMap = new HashMap<>();

  public UnitsAnnotatedTypeFactory(BaseTypeChecker checker) {
    // use true to enable flow inference, false to disable it
    super(checker, false);

    this.postInit();
  }

  // In Units Checker, we always want to print out the Invisible Qualifiers (UnknownUnits), and to
  // format the print out of qualifiers by removing Prefix.one
  @Override
  protected AnnotatedTypeFormatter createAnnotatedTypeFormatter() {
    return new UnitsAnnotatedTypeFormatter(checker);
  }

  // Converts all metric-prefixed units' alias annotations (eg @kg) into base unit annotations
  // with prefix values (eg @g(Prefix.kilo))
  @Override
  public AnnotationMirror canonicalAnnotation(AnnotationMirror anno) {
    // Get the name of the aliased annotation
    String aname = AnnotationUtils.annotationName(anno);

    // See if we already have a map from this aliased annotation to its corresponding base unit
    // annotation
    if (aliasMap.containsKey(aname)) {
      // if so return it
      return aliasMap.get(aname);
    }

    boolean built = false;
    AnnotationMirror result = null;
    // if not, look for the UnitsMultiple meta annotations of this aliased annotation
    for (AnnotationMirror metaAnno : anno.getAnnotationType().asElement().getAnnotationMirrors()) {
      // see if the meta annotation is UnitsMultiple
      if (isUnitsMultiple(metaAnno)) {
        // retrieve the Class of the base unit annotation
        Name baseUnitAnnoClass =
            AnnotationUtils.getElementValueClassName(metaAnno, unitsMultipleQuantityElement);

        // retrieve the SI Prefix of the aliased annotation
        Prefix prefix =
            AnnotationUtils.getElementValueEnum(
                metaAnno, unitsMultiplePrefixElement, Prefix.class, Prefix.one);

        // Build a base unit annotation with the prefix applied
        result =
            UnitsRelationsTools.buildAnnoMirrorWithSpecificPrefix(
                processingEnv, baseUnitAnnoClass, prefix);

        // TODO: assert that this annotation is a prefix multiple of a Unit that's in the supported
        // type qualifiers list currently this breaks for externally loaded annotations if the order
        // was an alias before a base annotation.
        // assert isSupportedQualifier(result);

        built = true;
        break;
      }
    }

    if (built) {
      // aliases shouldn't have Prefix.one, but if it does then clean it up here
      if (UnitsRelationsTools.getPrefix(result) == Prefix.one) {
        result = removePrefix(result);
      }

      // add this to the alias map
      aliasMap.put(aname, result);
      return result;
    }

    return super.canonicalAnnotation(anno);
  }

  /**
   * Returns a map from canonical class name to the corresponding UnitsRelations instance.
   *
   * @return a map from canonical class name to the corresponding UnitsRelations instance
   */
  protected Map<@CanonicalName String, UnitsRelations> getUnitsRel() {
    if (unitsRel == null) {
      unitsRel = new HashMap<>();
      // Always add the default units relations, for the standard units.
      // Other code adds more relations.
      unitsRel.put(
          UnitsRelationsDefault.class.getCanonicalName(),
          new UnitsRelationsDefault().init(processingEnv));
    }
    return unitsRel;
  }

  @Override
  protected AnnotationClassLoader createAnnotationClassLoader() {
    // Use the UnitsAnnotationClassLoader instead of the default one
    return new UnitsAnnotationClassLoader(checker);
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    // get all the loaded annotations
    Set<Class<? extends Annotation>> qualSet = getBundledTypeQualifiers();

    // load all the external units
    loadAllExternalUnits();

    // copy all loaded external Units to qual set
    qualSet.addAll(externalQualsMap.values());

    return qualSet;
  }

  private void loadAllExternalUnits() {
    // load external individually named units
    String qualNames = checker.getOption("units");
    if (qualNames != null) {
      for (String qualName : qualNames.split(",")) {
        if (!Signatures.isBinaryName(qualName)) {
          throw new UserError("Malformed qualifier name \"%s\" in -Aunits=%s", qualName, qualNames);
        }
        loadExternalUnit(qualName);
      }
    }

    // load external directories of units
    String qualDirectories = checker.getOption("unitsDirs");
    if (qualDirectories != null) {
      for (String directoryName : qualDirectories.split(":")) {
        loadExternalDirectory(directoryName);
      }
    }
  }

  /**
   * Loads and processes a single external units qualifier.
   *
   * @param annoName the name of a units qualifier
   */
  private void loadExternalUnit(@BinaryName String annoName) {
    // loadExternalAnnotationClass() returns null for alias units
    Class<? extends Annotation> loadedClass = loader.loadExternalAnnotationClass(annoName);
    if (loadedClass != null) {
      addUnitToExternalQualMap(loadedClass);
    }
  }

  /** Loads and processes the units qualifiers from a single external directory. */
  private void loadExternalDirectory(String directoryName) {
    Set<Class<? extends Annotation>> annoClassSet =
        loader.loadExternalAnnotationClassesFromDirectory(directoryName);

    for (Class<? extends Annotation> annoClass : annoClassSet) {
      addUnitToExternalQualMap(annoClass);
    }
  }

  /** Adds the annotation class to the external qualifier map if it is not an alias annotation. */
  private void addUnitToExternalQualMap(final Class<? extends Annotation> annoClass) {
    AnnotationMirror mirror =
        UnitsRelationsTools.buildAnnoMirrorWithNoPrefix(
            processingEnv, annoClass.getCanonicalName());

    // if it is not an aliased annotation, add to external quals map if it isn't already in map
    if (!isAliasedAnnotation(mirror)) {
      String unitClassName = annoClass.getCanonicalName();
      if (!externalQualsMap.containsKey(unitClassName)) {
        externalQualsMap.put(unitClassName, annoClass);
      }
    }
    // if it is an aliased annotation
    else {
      // ensure it has a base unit
      @CanonicalName Name baseUnitClass = getBaseUnitAnno(mirror);
      if (baseUnitClass != null) {
        // if the base unit isn't already added, add that first
        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/658
        @DotSeparatedIdentifiers String baseUnitClassName = baseUnitClass.toString();
        if (!externalQualsMap.containsKey(baseUnitClassName)) {
          loadExternalUnit(baseUnitClassName);
        }

        // then add the aliased annotation to the alias map
        // TODO: refactor so we can directly add to alias map, skipping the assert check in
        // canonicalAnnotation.
        canonicalAnnotation(mirror);
      } else {
        // error: somehow the aliased annotation has @UnitsMultiple meta annotation, but no
        // base class defined in that meta annotation
        // TODO: error abort
      }
    }

    // process the units annotation and add its corresponding units relations class
    addUnitsRelations(annoClass);
  }

  private boolean isAliasedAnnotation(AnnotationMirror anno) {
    // loop through the meta annotations of the annotation, look for UnitsMultiple
    for (AnnotationMirror metaAnno : anno.getAnnotationType().asElement().getAnnotationMirrors()) {
      // see if the meta annotation is UnitsMultiple
      if (isUnitsMultiple(metaAnno)) {
        // TODO: does every alias have to have Prefix?
        return true;
      }
    }

    // if we are unable to find UnitsMultiple meta annotation, then this is not an Aliased
    // Annotation
    return false;
  }

  /**
   * Return the name of the given annotation, if it is meta-annotated with UnitsMultiple; otherwise
   * return null.
   *
   * @param anno the annotation to examine
   * @return the annotation's name, if it is meta-annotated with UnitsMultiple; otherwise null
   */
  private @Nullable @CanonicalName Name getBaseUnitAnno(AnnotationMirror anno) {
    // loop through the meta annotations of the annotation, look for UnitsMultiple
    for (AnnotationMirror metaAnno : anno.getAnnotationType().asElement().getAnnotationMirrors()) {
      // see if the meta annotation is UnitsMultiple
      if (isUnitsMultiple(metaAnno)) {
        // TODO: does every alias have to have Prefix?
        // Retrieve the base unit annotation.
        Name baseUnitAnnoClass =
            AnnotationUtils.getElementValueClassName(metaAnno, unitsMultipleQuantityElement);
        return baseUnitAnnoClass;
      }
    }
    return null;
  }

  /**
   * Returns true if {@code metaAnno} is {@link UnitsMultiple}.
   *
   * @param metaAnno an annotation mirror
   * @return true if {@code metaAnno} is {@link UnitsMultiple}
   */
  private boolean isUnitsMultiple(AnnotationMirror metaAnno) {
    return areSameByClass(metaAnno, UnitsMultiple.class);
  }

  /**
   * Look for an @UnitsRelations annotation on the qualifier and add it to the list of
   * UnitsRelations.
   *
   * @param qual the qualifier to investigate
   */
  private void addUnitsRelations(Class<? extends Annotation> qual) {
    AnnotationMirror am = AnnotationBuilder.fromClass(elements, qual);

    for (AnnotationMirror ama : am.getAnnotationType().asElement().getAnnotationMirrors()) {
      if (areSameByClass(ama, unitsRelationsAnnoClass)) {
        String theclassname =
            AnnotationUtils.getElementValueClassName(ama, unitsRelationsValueElement).toString();
        if (!Signatures.isClassGetName(theclassname)) {
          throw new UserError(
              "Malformed class name \"%s\" should be in ClassGetName format in annotation %s",
              theclassname, ama);
        }
        Class<?> valueElement;
        try {
          ClassLoader classLoader = InternalUtils.getClassLoaderForClass(AnnotationUtils.class);
          valueElement = Class.forName(theclassname, true, classLoader);
        } catch (ClassNotFoundException e) {
          String msg =
              String.format(
                  "Could not load class '%s' for field 'value' in annotation %s",
                  theclassname, ama);
          throw new UserError(msg, e);
        }
        Class<? extends UnitsRelations> unitsRelationsClass;
        try {
          unitsRelationsClass = valueElement.asSubclass(UnitsRelations.class);
        } catch (ClassCastException ex) {
          throw new UserError(
              "Invalid @UnitsRelations meta-annotation found in %s. "
                  + "@UnitsRelations value %s is not a subclass of "
                  + "org.checkerframework.checker.units.UnitsRelations.",
              qual, ama);
        }
        String classname = unitsRelationsClass.getCanonicalName();

        if (!getUnitsRel().containsKey(classname)) {
          try {
            unitsRel.put(
                classname,
                unitsRelationsClass.getDeclaredConstructor().newInstance().init(processingEnv));
          } catch (Throwable e) {
            throw new BugInCF("Throwable when instantiating UnitsRelations", e);
          }
        }
      }
    }
  }

  @Override
  public TreeAnnotator createTreeAnnotator() {
    // Don't call super.createTreeAnnotator because it includes PropagationTreeAnnotator which
    // is incorrect.
    return new ListTreeAnnotator(
        new UnitsPropagationTreeAnnotator(this),
        new LiteralTreeAnnotator(this).addStandardLiteralQualifiers(),
        new UnitsTreeAnnotator(this));
  }

  private static class UnitsPropagationTreeAnnotator extends PropagationTreeAnnotator {

    public UnitsPropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    // Handled completely by UnitsTreeAnnotator
    @Override
    public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
      return null;
    }

    // Handled completely by UnitsTreeAnnotator
    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
      return null;
    }
  }

  /** A class for adding annotations based on tree. */
  private class UnitsTreeAnnotator extends TreeAnnotator {

    UnitsTreeAnnotator(UnitsAnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
      AnnotatedTypeMirror lht = getAnnotatedType(node.getLeftOperand());
      AnnotatedTypeMirror rht = getAnnotatedType(node.getRightOperand());
      Tree.Kind kind = node.getKind();

      // Remove Prefix.one
      if (UnitsRelationsTools.getPrefix(lht) == Prefix.one) {
        lht = UnitsRelationsTools.removePrefix(elements, lht);
      }
      if (UnitsRelationsTools.getPrefix(rht) == Prefix.one) {
        rht = UnitsRelationsTools.removePrefix(elements, rht);
      }

      AnnotationMirror bestres = null;
      for (UnitsRelations ur : getUnitsRel().values()) {
        AnnotationMirror res = useUnitsRelation(kind, ur, lht, rht);

        if (bestres != null && res != null && !bestres.equals(res)) {
          checker.message(
              Kind.WARNING,
              "UnitsRelation mismatch, taking neither! Previous: "
                  + bestres
                  + " and current: "
                  + res);
          return null; // super.visitBinary(node, type);
        }

        if (res != null) {
          bestres = res;
        }
      }

      if (bestres != null) {
        type.replaceAnnotation(bestres);
      } else {
        // If none of the units relations classes could resolve the units, then apply default rules

        switch (kind) {
          case MINUS:
          case PLUS:
            if (lht.getAnnotations().equals(rht.getAnnotations())) {
              // The sum or difference has the same units as both operands.
              type.replaceAnnotations(lht.getAnnotations());
            } else {
              // otherwise it results in mixed
              type.replaceAnnotation(mixedUnits);
            }
            break;
          case DIVIDE:
            if (lht.getAnnotations().equals(rht.getAnnotations())) {
              // If the units of the division match, return TOP
              type.replaceAnnotation(TOP);
            } else if (UnitsRelationsTools.hasNoUnits(rht)) {
              // any unit divided by a scalar keeps that unit
              type.replaceAnnotations(lht.getAnnotations());
            } else if (UnitsRelationsTools.hasNoUnits(lht)) {
              // scalar divided by any unit returns mixed
              type.replaceAnnotation(mixedUnits);
            } else {
              // else it is a division of two units that have no defined relations
              // from a relations class
              // return mixed
              type.replaceAnnotation(mixedUnits);
            }
            break;
          case MULTIPLY:
            if (UnitsRelationsTools.hasNoUnits(lht)) {
              // any unit multiplied by a scalar keeps the unit
              type.replaceAnnotations(rht.getAnnotations());
            } else if (UnitsRelationsTools.hasNoUnits(rht)) {
              // any scalar multiplied by a unit becomes the unit
              type.replaceAnnotations(lht.getAnnotations());
            } else {
              // else it is a multiplication of two units that have no defined
              // relations from a relations class
              // return mixed
              type.replaceAnnotation(mixedUnits);
            }
            break;
          case REMAINDER:
            // in modulo operation, it always returns the left unit regardless of what
            // it is (unknown, or some unit)
            type.replaceAnnotations(lht.getAnnotations());
            break;
          default:
            // Placeholders for unhandled binary operations
            // Do nothing
        }
      }

      return null;
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
      ExpressionTree var = node.getVariable();
      AnnotatedTypeMirror varType = getAnnotatedType(var);

      type.replaceAnnotations(varType.getAnnotations());
      return null;
    }

    private AnnotationMirror useUnitsRelation(
        Tree.Kind kind, UnitsRelations ur, AnnotatedTypeMirror lht, AnnotatedTypeMirror rht) {

      AnnotationMirror res = null;
      if (ur != null) {
        switch (kind) {
          case DIVIDE:
            res = ur.division(lht, rht);
            break;
          case MULTIPLY:
            res = ur.multiplication(lht, rht);
            break;
          default:
            // Do nothing
        }
      }
      return res;
    }
  }

  /** Set the Bottom qualifier as the bottom of the hierarchy. */
  @Override
  public QualifierHierarchy createQualifierHierarchy() {
    return new UnitsQualifierHierarchy();
  }

  /** Qualifier Hierarchy for the Units Checker. */
  @AnnotatedFor("nullness")
  protected class UnitsQualifierHierarchy extends MostlyNoElementQualifierHierarchy {
    /** Constructor. */
    public UnitsQualifierHierarchy() {
      super(UnitsAnnotatedTypeFactory.this.getSupportedTypeQualifiers(), elements);
    }

    @Override
    protected QualifierKindHierarchy createQualifierKindHierarchy(
        @UnderInitialization UnitsQualifierHierarchy this,
        Collection<Class<? extends Annotation>> qualifierClasses) {
      return new UnitsQualifierKindHierarchy(qualifierClasses, elements);
    }

    @Override
    protected boolean isSubtypeWithElements(
        AnnotationMirror subAnno,
        QualifierKind subKind,
        AnnotationMirror superAnno,
        QualifierKind superKind) {
      return AnnotationUtils.areSame(subAnno, superAnno);
    }

    @Override
    protected AnnotationMirror leastUpperBoundWithElements(
        AnnotationMirror a1,
        QualifierKind qualifierKind1,
        AnnotationMirror a2,
        QualifierKind qualifierKind2,
        QualifierKind lubKind) {
      if (qualifierKind1.isBottom()) {
        return a2;
      } else if (qualifierKind2.isBottom()) {
        return a1;
      } else if (qualifierKind1 == qualifierKind2) {
        if (AnnotationUtils.areSame(a1, a2)) {
          return a1;
        } else {
          @SuppressWarnings({
            "nullness:assignment.type.incompatible" // Every qualifier kind is a
            // key in directSuperQualifierMap.
          })
          @NonNull AnnotationMirror lub =
              ((UnitsQualifierKindHierarchy) qualifierKindHierarchy)
                  .directSuperQualifierMap.get(qualifierKind1);
          return lub;
        }
      }
      throw new BugInCF("Unexpected QualifierKinds: %s %s", qualifierKind1, qualifierKind2);
    }

    @Override
    protected AnnotationMirror greatestLowerBoundWithElements(
        AnnotationMirror a1,
        QualifierKind qualifierKind1,
        AnnotationMirror a2,
        QualifierKind qualifierKind2,
        QualifierKind glbKind) {
      return UnitsAnnotatedTypeFactory.this.BOTTOM;
    }
  }

  /** UnitsQualifierKindHierarchy. */
  @AnnotatedFor("nullness")
  protected static class UnitsQualifierKindHierarchy extends DefaultQualifierKindHierarchy {

    /**
     * Mapping from QualifierKind to an AnnotationMirror that represents its direct super qualifier.
     * Every qualifier kind maps to a nonnull AnnotationMirror.
     */
    private final Map<QualifierKind, AnnotationMirror> directSuperQualifierMap;

    /**
     * Creates a UnitsQualifierKindHierarchy.
     *
     * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
     * @param elements element utils
     */
    public UnitsQualifierKindHierarchy(
        Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
      super(qualifierClasses, UnitsBottom.class);
      directSuperQualifierMap = createDirectSuperQualifierMap(elements);
    }

    /**
     * Creates the direct super qualifier map.
     *
     * @param elements element utils
     * @return the map
     */
    @RequiresNonNull("this.qualifierKinds")
    private Map<QualifierKind, AnnotationMirror> createDirectSuperQualifierMap(
        @UnderInitialization UnitsQualifierKindHierarchy this, Elements elements) {
      Map<QualifierKind, AnnotationMirror> directSuperType = new TreeMap<>(4);
      for (QualifierKind qualifierKind : qualifierKinds) {
        QualifierKind directSuperTypeKind = getDirectSuperQualifierKind(qualifierKind);
        AnnotationMirror directSuperTypeAnno;
        try {
          directSuperTypeAnno = AnnotationBuilder.fromName(elements, directSuperTypeKind.getName());
        } catch (BugInCF ex) {
          throw new TypeSystemError("Unit annotations must have a default for all elements.");
        }
        if (directSuperTypeAnno == null) {
          throw new TypeSystemError("Could not create AnnotationMirror: %s", directSuperTypeAnno);
        }
        directSuperType.put(qualifierKind, directSuperTypeAnno);
      }
      return directSuperType;
    }

    /**
     * Get the direct super qualifier for the given qualifier kind.
     *
     * @param qualifierKind qualifier kind
     * @return direct super qualifier kind
     */
    private QualifierKind getDirectSuperQualifierKind(
        @UnderInitialization UnitsQualifierKindHierarchy this, QualifierKind qualifierKind) {
      if (qualifierKind.isTop()) {
        return qualifierKind;
      }
      Set<QualifierKind> superQuals = new TreeSet<>(qualifierKind.getStrictSuperTypes());
      while (superQuals.size() > 0) {
        Set<QualifierKind> lowest = findLowestQualifiers(superQuals);
        if (lowest.size() == 1) {
          return lowest.iterator().next();
        }
        superQuals.removeAll(lowest);
      }
      throw new BugInCF("No direct super qualifier found for %s", qualifierKind);
    }
  }

  private AnnotationMirror removePrefix(AnnotationMirror anno) {
    return UnitsRelationsTools.removePrefix(elements, anno);
  }
}
