package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.ElementQualifierHierarchy;
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.type.NoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypeSystemError;

/**
 * Represents the type qualifier hierarchy of a type system that supports multiple separate subtype
 * hierarchies.
 *
 * <p>This class is immutable and can be only created through {@link MultiGraphFactory}.
 *
 * @deprecated Use {@link ElementQualifierHierarchy}, {@link MostlyNoElementQualifierHierarchy}, or
 *     {@link NoElementQualifierHierarchy} instead. This class will be removed in a future release.
 *     <p>Here are instructions on how to convert from a subclass of MultiGraphQualifierHierarchy to
 *     the new implementations:
 *     <p>If the subclass implements isSubtype and calls super when annotations do not have
 *     elements, then use the following instructions to convert to {@link
 *     MostlyNoElementQualifierHierarchy}.
 *     <ol>
 *       <li>Change {@code extends MultiGraphQualifierHierarchy} to {@code extends
 *           MostlyNoElementQualifierHierarchy}.
 *       <li>Create a constructor matching super.
 *       <li>Implement isSubtypeWithElements, leastUpperBoundWithElements, and
 *           greatestLowerBoundWithElements. You may be able to reuse parts of your current
 *           implementation of isSubtype, leastUpperBound, and greatestLowerBound.
 *     </ol>
 *     <p>If the subclass implements isSubtype and does not call super in that implementation, then
 *     use the following instructions to convert to a subclass of {@link ElementQualifierHierarchy}.
 *     <ol>
 *       <li>Change {@code extends MultiGraphQualifierHierarchy} to {@code extends
 *           ElementQualifierHierarchy}.
 *       <li>Create a constructor matching super.
 *       <li>Implement {@link #leastUpperBound(AnnotationMirror, AnnotationMirror)} and {@link
 *           #greatestLowerBound(AnnotationMirror, AnnotationMirror)} if missing. (In the past, it
 *           was very easy to forget to implement these, now they are abstract methods.)
 *     </ol>
 *     If you wish to continue to use a subclass of {@link MultiGraphQualifierHierarchy} or {@link
 *     GraphQualifierHierarchy}, you may do so by adding the following to AnnotatedTypeFactory.
 *     (It's better to convert to one of the new classes because MultiGraphQualifierHierarchy and
 *     GraphQualifierHierarchy are buggy and no longer supported.)
 *     <p>If any qualifier has an annotation element without a default value, you will need to
 *     convert to one of the new subclasses. If you do not, then MultiGraphQualifierHierarchy will
 *     throw an exception with a message like "AnnotationBuilder.fromName: no value for element
 *     value() of checkers.inference.qual.VarAnnot".
 *     <pre>
 * {@code @Override}
 * {@code @SuppressWarnings("deprecation")}
 * <code> public QualifierHierarchy createQualifierHierarchy() {
 *      return org.checkerframework.framework.util.MultiGraphQualifierHierarchy
 *              .createMultiGraphQualifierHierarchy(this);
 *   }
 * </code>
 * {@code @Override}
 * {@code @SuppressWarnings("deprecation")}
 * <code> public QualifierHierarchy createQualifierHierarchyWithMultiGraphFactory(
 *          org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory
 *                  factory) {
 *      return new YourSubclassQualifierHierarchy(factory);
 *  }
 * </code></pre>
 */
@SuppressWarnings("interning") // Class is deprecated.
@Deprecated
public class MultiGraphQualifierHierarchy implements QualifierHierarchy {

  /**
   * Creates the QualifierHierarchy using {@link
   * org.checkerframework.framework.util.MultiGraphQualifierHierarchy}
   *
   * @param annotatedTypeFactory annotated type factory
   * @return qualifier hierarchy
   * @deprecated Use {@link ElementQualifierHierarchy} instead.
   */
  @Deprecated
  public static QualifierHierarchy createMultiGraphQualifierHierarchy(
      AnnotatedTypeFactory annotatedTypeFactory) {
    Set<Class<? extends Annotation>> supportedTypeQualifiers =
        annotatedTypeFactory.getSupportedTypeQualifiers();
    MultiGraphFactory factory = new MultiGraphFactory(annotatedTypeFactory);
    for (Class<? extends Annotation> typeQualifier : supportedTypeQualifiers) {
      AnnotationMirror typeQualifierAnno =
          AnnotationBuilder.fromClass(annotatedTypeFactory.getElementUtils(), typeQualifier);
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
                  + " is polymorphic and specifies super qualifiers."
                  + " Remove the @org.checkerframework.framework.qual.SubtypeOf or"
                  + " @org.checkerframework.framework.qual.PolymorphicQualifier annotation from"
                  + " it.");
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
        AnnotationMirror superAnno =
            AnnotationBuilder.fromClass(annotatedTypeFactory.getElementUtils(), superQualifier);
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
   * Factory used to create an instance of {@link GraphQualifierHierarchy}. A factory can be used to
   * create at most one {@link GraphQualifierHierarchy}.
   *
   * <p>To create a hierarchy, a client may do so in three steps:
   *
   * <ol>
   *   <li>add qualifiers using {@link #addQualifier(AnnotationMirror)};
   *   <li>add subtype relations using {@link #addSubtype(AnnotationMirror, AnnotationMirror)}
   *   <li>build the hierarchy and gets using {@link #build()}.
   * </ol>
   *
   * Notice that {@link #addSubtype(AnnotationMirror, AnnotationMirror)} adds the two qualifiers to
   * the hierarchy if they are not already in.
   *
   * <p>Also, once the client builds a hierarchy through {@link #build()}, no further modifications
   * are allowed nor can it making a new instance.
   *
   * <p>Clients build the hierarchy using {@link #addQualifier(AnnotationMirror)} and {@link
   * #addSubtype(AnnotationMirror, AnnotationMirror)}, then get the instance with calling {@link
   * #build()}
   *
   * @deprecated Use {@link ElementQualifierHierarchy} instead.
   */
  @Deprecated
  public static class MultiGraphFactory {
    /**
     * Map from qualifiers to the direct supertypes of the qualifier. Only the subtype relations
     * given by addSubtype are in this mapping, no transitive relationships. It is immutable once
     * GraphQualifierHierarchy is built. No polymorphic qualifiers are contained in this map.
     */
    protected final Map<AnnotationMirror, Set<AnnotationMirror>> supertypesDirect;

    /**
     * Map from qualifier hierarchy to the corresponding polymorphic qualifier. The key is:
     *
     * <ul>
     *   <li>the argument to @PolymorphicQualifier (typically the top qualifier in the hierarchy),
     *       or
     *   <li>"Annotation" if @PolymorphicQualifier is used without an argument, or
     * </ul>
     */
    protected final Map<AnnotationMirror, AnnotationMirror> polyQualifiers;

    /** The annotated type factory associated with this hierarchy. */
    protected final AnnotatedTypeFactory atypeFactory;

    /** Create a factory. */
    public MultiGraphFactory(AnnotatedTypeFactory atypeFactory) {
      this.supertypesDirect = AnnotationUtils.createAnnotationMap();
      this.polyQualifiers = AnnotationUtils.createAnnotationMap();
      this.atypeFactory = atypeFactory;
    }

    /**
     * Adds the passed qualifier to the hierarchy. Clients need to specify its super qualifiers in
     * subsequent calls to {@link #addSubtype(AnnotationMirror, AnnotationMirror)}.
     */
    public void addQualifier(AnnotationMirror qual) {
      assertNotBuilt();
      if (AnnotationUtils.containsSame(supertypesDirect.keySet(), qual)) {
        return;
      }

      @CanonicalName Name pqtopclass = getPolymorphicQualifierElement(qual);
      if (pqtopclass != null) {
        AnnotationMirror pqtop;
        if (pqtopclass.contentEquals(Annotation.class.getName())) {
          // A @PolymorphicQualifier with no value defaults to Annotation.class.
          // That means there is only one top in the hierarchy. The top qualifier
          // may not be known at this point, so use the qualifier itself.
          // This is changed to top in MultiGraphQualifierHierarchy.addPolyRelations
          pqtop = qual;
        } else {
          pqtop = AnnotationBuilder.fromName(atypeFactory.getElementUtils(), pqtopclass);
        }
        // use given top (which might be Annotation) as key
        this.polyQualifiers.put(pqtop, qual);
      } else {
        supertypesDirect.put(qual, AnnotationUtils.createAnnotationSet());
      }
    }

    /**
     * Returns the {@link PolymorphicQualifier} meta-annotation on {@code qual} if one exists;
     * otherwise return null.
     *
     * @param qual qualifier
     * @return the {@link PolymorphicQualifier} meta-annotation on {@code qual} if one exists;
     *     otherwise return null
     */
    private AnnotationMirror getPolymorphicQualifier(AnnotationMirror qual) {
      if (qual == null) {
        return null;
      }
      Element qualElt = qual.getAnnotationType().asElement();
      for (AnnotationMirror am : qualElt.getAnnotationMirrors()) {
        if (atypeFactory.areSameByClass(am, PolymorphicQualifier.class)) {
          return am;
        }
      }
      return null;
    }

    /**
     * If {@code qual} is a polymorphic qualifier, return the class specified by the {@link
     * PolymorphicQualifier} meta-annotation on the polymorphic qualifier is returned. Otherwise,
     * return null.
     *
     * <p>This value identifies the qualifier hierarchy to which this polymorphic qualifier belongs.
     * By convention, it is the top qualifier of the hierarchy. Use of {@code Annotation.class} is
     * discouraged, because it can lead to ambiguity if used for multiple type systems.
     *
     * @param qual an annotation
     * @return the name of the class specified by the {@link PolymorphicQualifier} meta-annotation
     *     on {@code qual}, if {@code qual} is a polymorphic qualifier; otherwise, null.
     * @see org.checkerframework.framework.qual.PolymorphicQualifier#value()
     */
    private @Nullable @CanonicalName Name getPolymorphicQualifierElement(AnnotationMirror qual) {
      AnnotationMirror poly = getPolymorphicQualifier(qual);

      // System.out.println("poly: " + poly + " pq: " +
      //     PolymorphicQualifier.class.getCanonicalName());
      if (poly == null) {
        return null;
      }
      // Default value for `PolymorphicQualifier.value` is Annotation.class
      Name ret = AnnotationUtils.getElementValueClassName(poly, "value", true);
      return ret;
    }

    /**
     * Adds a subtype relationship between the two type qualifiers. Assumes that both qualifiers are
     * part of the same qualifier hierarchy; callers should ensure this.
     *
     * @param sub the sub type qualifier
     * @param sup the super type qualifier
     */
    public void addSubtype(AnnotationMirror sub, AnnotationMirror sup) {
      assertNotBuilt();
      addQualifier(sub);
      addQualifier(sup);
      supertypesDirect.get(sub).add(sup);
    }

    /**
     * Returns an instance of {@link GraphQualifierHierarchy} that represents the hierarchy built so
     * far.
     */
    public QualifierHierarchy build() {
      assertNotBuilt();
      QualifierHierarchy result = createQualifierHierarchy();
      wasBuilt = true;
      return result;
    }

    /**
     * Create {@link QualifierHierarchy}
     *
     * @return QualifierHierarchy
     */
    protected QualifierHierarchy createQualifierHierarchy() {
      return atypeFactory.createQualifierHierarchyWithMultiGraphFactory(this);
    }

    /** True if the factory has already been built. */
    private boolean wasBuilt = false;

    /** Throw an exception if the factory was already built. */
    protected void assertNotBuilt() {
      if (wasBuilt) {
        throw new BugInCF("MultiGraphQualifierHierarchy.Factory was already built.");
      }
    }
  }

  /**
   * The declared, direct supertypes for each qualifier, without added transitive relations.
   * Immutable after construction finishes. No polymorphic qualifiers are contained in this map.
   *
   * @see MultiGraphQualifierHierarchy.MultiGraphFactory#supertypesDirect
   */
  protected final Map<AnnotationMirror, Set<AnnotationMirror>> supertypesDirect;

  /** The transitive closure of the supertypesDirect. Immutable after construction finishes. */
  protected final Map<AnnotationMirror, Set<AnnotationMirror>> supertypesTransitive;

  /** The top qualifiers of the individual type hierarchies. */
  protected final Set<AnnotationMirror> tops;

  /** The bottom qualifiers of the type hierarchies. TODO: clarify relation to tops. */
  protected final Set<AnnotationMirror> bottoms;

  /**
   * See {@link MultiGraphQualifierHierarchy.MultiGraphFactory#polyQualifiers}.
   *
   * @see MultiGraphQualifierHierarchy.MultiGraphFactory#polyQualifiers
   */
  protected final Map<AnnotationMirror, AnnotationMirror> polyQualifiers;

  /** All qualifiers, including polymorphic qualifiers. */
  private final Set<AnnotationMirror> typeQualifiers;

  public MultiGraphQualifierHierarchy(MultiGraphFactory f) {
    this(f, (Object[]) null);
  }

  // Allow a subclass to provide additional constructor parameters that
  // are simply passed back via a call to the "finish" method.
  public MultiGraphQualifierHierarchy(MultiGraphFactory f, Object... args) {
    super();
    // no need for copying as f.supertypes has no mutable references to it
    // TODO: also make the Set of supertypes immutable?
    this.supertypesDirect = Collections.unmodifiableMap(f.supertypesDirect);

    // Calculate the transitive closure
    Map<AnnotationMirror, Set<AnnotationMirror>> supertypesTransitive =
        transitiveClosure(f.supertypesDirect);

    Set<AnnotationMirror> newtops = findTops(supertypesTransitive);
    Set<AnnotationMirror> newbottoms = findBottoms(supertypesTransitive);

    this.polyQualifiers = f.polyQualifiers;

    addPolyRelations(this, supertypesTransitive, this.polyQualifiers, newtops, newbottoms);

    finish(this, supertypesTransitive, this.polyQualifiers, newtops, newbottoms, args);

    this.tops = Collections.unmodifiableSet(newtops);
    this.bottoms = Collections.unmodifiableSet(newbottoms);
    // TODO: make polyQualifiers immutable also?

    this.supertypesTransitive = Collections.unmodifiableMap(supertypesTransitive);
    Set<AnnotationMirror> typeQualifiers = AnnotationUtils.createAnnotationSet();
    typeQualifiers.addAll(supertypesTransitive.keySet());
    this.typeQualifiers = Collections.unmodifiableSet(typeQualifiers);
    // System.out.println("MGH: " + this);
  }

  @Override
  public boolean isValid() {
    return !typeQualifiers.isEmpty();
  }

  /**
   * Method to finalize the qualifier hierarchy before it becomes unmodifiable. The parameters pass
   * all fields and allow modification.
   */
  protected void finish(
      QualifierHierarchy qualHierarchy,
      Map<AnnotationMirror, Set<AnnotationMirror>> supertypesTransitive,
      Map<AnnotationMirror, AnnotationMirror> polyQualifiers,
      Set<AnnotationMirror> tops,
      Set<AnnotationMirror> bottoms,
      Object... args) {}

  @SideEffectFree
  @Override
  public String toString() {
    StringJoiner sj = new StringJoiner(System.lineSeparator());
    sj.add("Supertypes Graph: ");

    for (Map.Entry<AnnotationMirror, Set<AnnotationMirror>> qual : supertypesDirect.entrySet()) {
      sj.add("\t" + qual.getKey() + " = " + qual.getValue());
    }

    sj.add("Supertypes Map: ");

    for (Map.Entry<AnnotationMirror, Set<AnnotationMirror>> qual :
        supertypesTransitive.entrySet()) {
      String keyOpen = "\t" + qual.getKey() + " = [";

      Set<AnnotationMirror> supertypes = qual.getValue();

      if (supertypes.size() == 1) {
        // If there's only 1 supertype for this qual, then display that in the same row.
        sj.add(keyOpen + supertypes.iterator().next() + "]");
      } else {
        // otherwise, display each supertype in its own row
        sj.add(keyOpen);
        for (Iterator<AnnotationMirror> iterator = supertypes.iterator(); iterator.hasNext(); ) {
          sj.add("\t\t" + iterator.next() + (iterator.hasNext() ? ", " : ""));
        }
        sj.add("\t\t]");
      }
    }

    sj.add("Tops: " + tops);
    sj.add("Bottoms: " + bottoms);

    return sj.toString();
  }

  @Override
  public Set<? extends AnnotationMirror> getTopAnnotations() {
    return this.tops;
  }

  @Override
  public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
    for (AnnotationMirror top : tops) {
      if (AnnotationUtils.areSame(start, top) || isSubtype(start, top)) {
        return top;
      }
    }
    throw new BugInCF(
        "MultiGraphQualifierHierarchy: did not find the top corresponding to qualifier "
            + start
            + " all tops: "
            + tops);
  }

  @Override
  public Set<? extends AnnotationMirror> getBottomAnnotations() {
    return this.bottoms;
  }

  @Override
  public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
    for (AnnotationMirror bot : bottoms) {
      if (AnnotationUtils.areSame(start, bot) || isSubtype(bot, start)) {
        return bot;
      }
    }
    throw new BugInCF(
        "MultiGraphQualifierHierarchy: did not find the bottom corresponding to qualifier "
            + start
            + "; all bottoms: "
            + bottoms
            + "; this: "
            + this);
  }

  @Override
  public AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
    AnnotationMirror top = getTopAnnotation(start);
    for (AnnotationMirror key : polyQualifiers.keySet()) {
      if (key != null && AnnotationUtils.areSame(key, top)) {
        return polyQualifiers.get(key);
      }
    }
    // No polymorphic qualifier exists for that hierarchy.
    return null;
  }

  @Override
  public boolean isSubtype(
      Collection<? extends AnnotationMirror> rhs, Collection<? extends AnnotationMirror> lhs) {
    if (lhs.isEmpty() || rhs.isEmpty()) {
      throw new BugInCF(
          "MultiGraphQualifierHierarchy: empty annotations in lhs: " + lhs + " or rhs: " + rhs);
    }
    if (lhs.size() != rhs.size()) {
      throw new BugInCF(
          "MultiGraphQualifierHierarchy: mismatched number of annotations in lhs: "
              + lhs
              + " and rhs: "
              + rhs);
    }
    int valid = 0;
    for (AnnotationMirror lhsAnno : lhs) {
      for (AnnotationMirror rhsAnno : rhs) {
        if (AnnotationUtils.areSame(getTopAnnotation(lhsAnno), getTopAnnotation(rhsAnno))
            && isSubtype(rhsAnno, lhsAnno)) {
          ++valid;
        }
      }
    }
    return lhs.size() == valid;
  }

  /** For caching results of lubs * */
  private Map<AnnotationPair, AnnotationMirror> lubs = null;

  @Override
  public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
    if (!AnnotationUtils.areSameByName(getTopAnnotation(a1), getTopAnnotation(a2))) {
      return null;
    } else if (isSubtype(a1, a2)) {
      return a2;
    } else if (isSubtype(a2, a1)) {
      return a1;
    } else if (AnnotationUtils.areSameByName(a1, a2)) {
      return getTopAnnotation(a1);
    }
    if (lubs == null) {
      lubs = calculateLubs();
    }
    AnnotationPair pair = new AnnotationPair(a1, a2);
    return lubs.get(pair);
  }

  /** A cache of the results of glb computations. Maps from a pair of annotations to their glb. */
  private Map<AnnotationPair, AnnotationMirror> glbs = null;

  @Override
  public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
    if (AnnotationUtils.areSameByName(a1, a2)) {
      return AnnotationUtils.sameElementValues(a1, a2) ? a1 : getBottomAnnotation(a1);
    }
    if (glbs == null) {
      glbs = calculateGlbs();
    }
    AnnotationPair pair = new AnnotationPair(a1, a2);
    return glbs.get(pair);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Most qualifiers have no value fields. However, two annotations with values are subtype of
   * each other only if they have the same values. i.e. I(m) is a subtype of I(n) iff m = n.
   *
   * <p>When client specifies an annotation, a1, to be a subtype of annotation with values, a2, then
   * a1 is a subtype of all instances of a2 regardless of a2 values.
   *
   * @param subAnno the sub qualifier
   * @param superAnno the super qualifier
   */
  @Override
  public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
    checkAnnoInGraph(subAnno);
    checkAnnoInGraph(superAnno);

    /* TODO: this optimization leads to recursion
    for (AnnotationMirror top : tops) {
        System.out.println("Looking at top: " + tops + " and " + anno1);
        // We cannot use getRootAnnotation, as that would use subtyping and recurse
        if (isSubtype(anno1, top) && AnnotationUtils.areSame(top, anno2)) {
        return true;
        }
    }*/
    if (AnnotationUtils.areSameByName(subAnno, superAnno)) {
      return AnnotationUtils.sameElementValues(subAnno, superAnno);
    }
    Set<AnnotationMirror> supermap1 = this.supertypesTransitive.get(subAnno);
    return AnnotationUtils.containsSame(supermap1, superAnno);
  }

  /**
   * Throw a {@link BugInCF} if {@code a} is not in the {@link #supertypesTransitive} or {@link
   * #polyQualifiers}.
   *
   * @param a qualifier
   */
  private final void checkAnnoInGraph(AnnotationMirror a) {
    if (AnnotationUtils.containsSame(supertypesTransitive.keySet(), a)
        || AnnotationUtils.containsSame(polyQualifiers.values(), a)) {
      return;
    }

    if (a == null) {
      throw new BugInCF(
          "MultiGraphQualifierHierarchy found an unqualified type.  Please ensure that "
              + "your defaulting rules cover all cases and/or "
              + "use a @DefaultQualifierInHierarchy annotation.  "
              + "Also ensure that overrides of addComputedTypeAnnotations call super.");
    } else {
      // System.out.println("MultiGraphQH: " + this);
      throw new BugInCF(
          "MultiGraphQualifierHierarchy found the unrecognized qualifier: "
              + a
              + ". Please ensure that the qualifier is correctly included in the subtype"
              + " hierarchy.");
    }
  }

  /**
   * Infer the tops of the subtype hierarchy. Simply finds the qualifiers that have no supertypes.
   */
  // Not static to allow adaptation in subclasses. Only parameters should be modified.
  protected Set<AnnotationMirror> findTops(
      Map<AnnotationMirror, Set<AnnotationMirror>> supertypes) {
    Set<AnnotationMirror> possibleTops = AnnotationUtils.createAnnotationSet();
    for (AnnotationMirror anno : supertypes.keySet()) {
      if (supertypes.get(anno).isEmpty()) {
        possibleTops.add(anno);
      }
    }
    return possibleTops;
  }

  /**
   * Infer the bottoms of the subtype hierarchy. Simple finds the qualifiers that are not supertypes
   * of other qualifiers.
   */
  // Not static to allow adaptation in subclasses. Only parameters should be modified.
  protected Set<AnnotationMirror> findBottoms(
      Map<AnnotationMirror, Set<AnnotationMirror>> supertypes) {
    Set<AnnotationMirror> possibleBottoms = AnnotationUtils.createAnnotationSet();
    possibleBottoms.addAll(supertypes.keySet());
    for (Set<AnnotationMirror> supers : supertypes.values()) {
      possibleBottoms.removeAll(supers);
    }
    return possibleBottoms;
  }

  /** Computes the transitive closure of the given map and returns it. */
  /* The method gets all required parameters passed in and could be static. However,
   * we want to allow subclasses to adapt the behavior and therefore make it an instance method.
   */
  protected Map<AnnotationMirror, Set<AnnotationMirror>> transitiveClosure(
      Map<AnnotationMirror, Set<AnnotationMirror>> supertypes) {
    Map<AnnotationMirror, Set<AnnotationMirror>> result = AnnotationUtils.createAnnotationMap();
    for (AnnotationMirror anno : supertypes.keySet()) {
      // this method directly modifies result and is
      // ignoring the returned value
      findAllSupers(anno, supertypes, result);
    }
    return result;
  }

  /**
   * Add the relationships for polymorphic qualifiers.
   *
   * <p>A polymorphic qualifier, such as {@code PolyNull}, needs to be:
   *
   * <ol>
   *   <li>a subtype of the top qualifier (e.g. {@code Nullable})
   *   <li>a supertype of all the bottom qualifiers (e.g. {@code NonNull})
   * </ol>
   *
   * Field supertypesTransitive is not set yet when this method is called -- use parameter fullMap
   * instead.
   */
  // The method gets all required parameters passed in and could be static. However,
  // we want to allow subclasses to adapt the behavior and therefore make it an instance method.
  protected void addPolyRelations(
      QualifierHierarchy qualHierarchy,
      Map<AnnotationMirror, Set<AnnotationMirror>> fullMap,
      Map<AnnotationMirror, AnnotationMirror> polyQualifiers,
      Set<AnnotationMirror> tops,
      Set<AnnotationMirror> bottoms) {
    if (polyQualifiers.isEmpty()) {
      return;
    }

    // Handle the case where @PolymorphicQualifier uses the default value Annotation.class.
    if (polyQualifiers.size() == 1 && tops.size() == 1) {
      Map.Entry<AnnotationMirror, AnnotationMirror> entry =
          polyQualifiers.entrySet().iterator().next();
      AnnotationMirror poly = entry.getKey();
      AnnotationMirror maybeTop = entry.getValue();
      if (AnnotationUtils.areSameByName(poly, maybeTop)) {
        // If the value of @PolymorphicQualifier is the default value, Annotation.class,
        // then map is set to polyQual -> polyQual in
        // MultiGraphQualifierHierarchy.MultiGraphFactory.addQualifier,
        // because the top is unknown there.
        // Reset it to top here.
        polyQualifiers.put(tops.iterator().next(), poly);
        polyQualifiers.remove(poly);
      }
    }

    for (Map.Entry<AnnotationMirror, AnnotationMirror> kv : polyQualifiers.entrySet()) {
      AnnotationMirror declTop = kv.getKey();
      AnnotationMirror polyQualifier = kv.getValue();
      // Ensure that it's really the top of the hierarchy
      Set<AnnotationMirror> declSupers = fullMap.get(declTop);
      AnnotationMirror polyTop = null;
      if (declSupers.isEmpty()) {
        polyTop = declTop;
      } else {
        for (AnnotationMirror ds : declSupers) {
          if (AnnotationUtils.containsSameByName(tops, ds)) {
            polyTop = ds;
          }
        }
      }
      boolean found = (polyTop != null);
      if (found) {
        AnnotationUtils.updateMappingToImmutableSet(
            fullMap, polyQualifier, Collections.singleton(polyTop));
      } else if (AnnotationUtils.areSameByName(polyQualifier, declTop)) {
        throw new BugInCF(
            "MultiGraphQualifierHierarchy.addPolyRelations: "
                + "incorrect or missing top qualifier given in polymorphic qualifier "
                + polyQualifier
                + "; possible top qualifiers: "
                + tops);
      } else {
        throw new BugInCF(
            "MultiGraphQualifierHierarchy.addPolyRelations: "
                + "incorrect top qualifier given in polymorphic qualifier: "
                + polyQualifier
                + " could not find: "
                + polyTop);
      }

      found = false;
      AnnotationMirror bottom = null;
      outer:
      for (AnnotationMirror btm : bottoms) {
        for (AnnotationMirror btmsuper : fullMap.get(btm)) {
          if (AnnotationUtils.areSameByName(btmsuper, polyTop)) {
            found = true;
            bottom = btm;
            break outer;
          }
        }
      }
      if (found) {
        AnnotationUtils.updateMappingToImmutableSet(
            fullMap, bottom, Collections.singleton(polyQualifier));
      } else {
        // TODO: in a type system with a single qualifier this check will fail.
        // throw new BugInCF("MultiGraphQualifierHierarchy.addPolyRelations:
        // " +
        //        "incorrect top qualifier given in polymorphic qualifier: "
        //
        //        + polyQualifier + " could not find bottom for: " + polyTop);
      }
    }
  }

  private Map<AnnotationPair, AnnotationMirror> calculateLubs() {
    Map<AnnotationPair, AnnotationMirror> newlubs = new HashMap<>();
    for (AnnotationMirror a1 : typeQualifiers) {
      for (AnnotationMirror a2 : typeQualifiers) {
        if (AnnotationUtils.areSameByName(a1, a2)) {
          continue;
        }
        if (!AnnotationUtils.areSame(getTopAnnotation(a1), getTopAnnotation(a2))) {
          continue;
        }
        AnnotationPair pair = new AnnotationPair(a1, a2);
        if (newlubs.containsKey(pair)) {
          continue;
        }
        AnnotationMirror lub = findLub(a1, a2);
        newlubs.put(pair, lub);
      }
    }
    return newlubs;
  }

  /**
   * Finds and returns the Least Upper Bound (LUB) of two annotation mirrors a1 and a2 by
   * recursively climbing the qualifier hierarchy of a1 until one of them is a subtype of the other,
   * or returns null if no subtype relationships can be found.
   *
   * @param a1 first annotation mirror
   * @param a2 second annotation mirror
   * @return the LUB of a1 and a2, or null if none can be found
   */
  protected AnnotationMirror findLub(AnnotationMirror a1, AnnotationMirror a2) {
    if (isSubtype(a1, a2)) {
      return a2;
    }
    if (isSubtype(a2, a1)) {
      return a1;
    }

    assert getTopAnnotation(a1) == getTopAnnotation(a2)
        : "MultiGraphQualifierHierarchy.findLub: this method may only be called "
            + "with qualifiers from the same hierarchy. Found a1: "
            + a1
            + " [top: "
            + getTopAnnotation(a1)
            + "], a2: "
            + a2
            + " [top: "
            + getTopAnnotation(a2)
            + "]";

    if (isPolymorphicQualifier(a1)) {
      return findLubWithPoly(a1, a2);
    } else if (isPolymorphicQualifier(a2)) {
      return findLubWithPoly(a2, a1);
    }

    Set<AnnotationMirror> outset = AnnotationUtils.createAnnotationSet();
    for (AnnotationMirror a1Super : supertypesDirect.get(a1)) {
      // TODO: we take the first of the smallest supertypes, maybe we would
      // get a different LUB if we used a different one?
      AnnotationMirror a1Lub = findLub(a1Super, a2);
      if (a1Lub != null) {
        outset.add(a1Lub);
      } else {
        throw new BugInCF(
            "GraphQualifierHierarchy could not determine LUB for "
                + a1
                + " and "
                + a2
                + ". Please ensure that the checker knows about all type qualifiers.");
      }
    }
    return requireSingleton(outset, a1, a2, /*lub=*/ true);
  }

  private AnnotationMirror findLubWithPoly(AnnotationMirror poly, AnnotationMirror other) {
    AnnotationMirror bottom = getBottomAnnotation(other);
    if (AnnotationUtils.areSame(bottom, other)) {
      return poly;
    }

    return getTopAnnotation(poly);
  }

  @Override
  public boolean isPolymorphicQualifier(AnnotationMirror qual) {
    return AnnotationUtils.containsSame(polyQualifiers.values(), qual);
  }

  /** Remove all supertypes of elements contained in the set. */
  private Set<AnnotationMirror> findSmallestTypes(Set<AnnotationMirror> inset) {
    Set<AnnotationMirror> outset = AnnotationUtils.createAnnotationSet();
    outset.addAll(inset);

    for (AnnotationMirror a1 : inset) {
      outset.removeIf(a2 -> a1 != a2 && isSubtype(a1, a2));
    }
    return outset;
  }

  /** Finds all the super qualifiers for a qualifier. */
  private static Set<AnnotationMirror> findAllSupers(
      AnnotationMirror anno,
      Map<AnnotationMirror, Set<AnnotationMirror>> supertypes,
      Map<AnnotationMirror, Set<AnnotationMirror>> allSupersSoFar) {
    Set<AnnotationMirror> supers = AnnotationUtils.createAnnotationSet();
    for (AnnotationMirror superAnno : supertypes.get(anno)) {
      // add the current super to the superset
      supers.add(superAnno);
      // add all of current super's super into superset
      supers.addAll(findAllSupers(superAnno, supertypes, allSupersSoFar));
    }
    allSupersSoFar.put(anno, Collections.unmodifiableSet(supers));
    return supers;
  }

  /** Returns a map from each possible pair of annotations to their glb. */
  private Map<AnnotationPair, AnnotationMirror> calculateGlbs() {
    Map<AnnotationPair, AnnotationMirror> newglbs = new HashMap<>();
    for (AnnotationMirror a1 : typeQualifiers) {
      for (AnnotationMirror a2 : typeQualifiers) {
        if (AnnotationUtils.areSameByName(a1, a2)) {
          continue;
        }
        if (!AnnotationUtils.areSame(getTopAnnotation(a1), getTopAnnotation(a2))) {
          continue;
        }
        AnnotationPair pair = new AnnotationPair(a1, a2);
        if (newglbs.containsKey(pair)) {
          continue;
        }
        AnnotationMirror glb = findGlb(a1, a2);
        newglbs.put(pair, glb);
      }
    }
    return newglbs;
  }

  private AnnotationMirror findGlb(AnnotationMirror a1, AnnotationMirror a2) {
    if (isSubtype(a1, a2)) {
      return a1;
    }
    if (isSubtype(a2, a1)) {
      return a2;
    }

    assert getTopAnnotation(a1) == getTopAnnotation(a2)
        : "MultiGraphQualifierHierarchy.findGlb: this method may only be called "
            + "with qualifiers from the same hierarchy. Found a1: "
            + a1
            + " [top: "
            + getTopAnnotation(a1)
            + "], a2: "
            + a2
            + " [top: "
            + getTopAnnotation(a2)
            + "]";

    if (isPolymorphicQualifier(a1)) {
      return findGlbWithPoly(a1, a2);
    } else if (isPolymorphicQualifier(a2)) {
      return findGlbWithPoly(a2, a1);
    }

    Set<AnnotationMirror> outset = AnnotationUtils.createAnnotationSet();
    for (AnnotationMirror a1Sub : supertypesDirect.keySet()) {
      if (isSubtype(a1Sub, a1) && !a1Sub.equals(a1)) {
        AnnotationMirror a1lb = findGlb(a1Sub, a2);
        if (a1lb != null) {
          outset.add(a1lb);
        }
      }
    }
    return requireSingleton(outset, a1, a2, /*lub=*/ false);
  }

  private AnnotationMirror findGlbWithPoly(AnnotationMirror poly, AnnotationMirror other) {
    AnnotationMirror top = getTopAnnotation(other);
    if (AnnotationUtils.areSame(top, other)) {
      return poly;
    }

    return getBottomAnnotation(poly);
  }

  /** Remove all subtypes of elements contained in the set. */
  private Set<AnnotationMirror> findGreatestTypes(Set<AnnotationMirror> inset) {
    Set<AnnotationMirror> outset = AnnotationUtils.createAnnotationSet();
    outset.addAll(inset);

    for (AnnotationMirror a1 : inset) {
      Iterator<AnnotationMirror> outit = outset.iterator();
      while (outit.hasNext()) {
        AnnotationMirror a2 = outit.next();
        if (a1 != a2 && isSubtype(a2, a1)) {
          outit.remove();
        }
      }
    }
    return outset;
  }

  /**
   * Require that outset is a singleton set, after polymorphic qualifiers have been removed. If not,
   * report a bug: the type hierarchy is not a lattice.
   *
   * @param outset the set of upper or lower bounds of a1 and a2 (depending on whether lub==true)
   * @param a1 the first annotation being lubbed or glbed
   * @param a2 the second annotation being lubbed or glbed
   * @param lub true if computing lub(a1, a2), false if computing glb(a1, a2)
   * @return the unique element of outset; issues an error if outset.size() != 1
   */
  private AnnotationMirror requireSingleton(
      Set<AnnotationMirror> outset, AnnotationMirror a1, AnnotationMirror a2, boolean lub) {
    if (outset.size() == 0) {
      throw new BugInCF(
          "MultiGraphQualifierHierarchy could not determine "
              + (lub ? "LUB" : "GLB")
              + " for "
              + a1
              + " and "
              + a2
              + ". Please ensure that the checker knows about all type qualifiers.");
    } else if (outset.size() == 1) {
      return outset.iterator().next();
    } else {
      // outset.size() > 1

      outset = lub ? findSmallestTypes(outset) : findGreatestTypes(outset);

      AnnotationMirror result = null;
      for (AnnotationMirror anno : outset) {
        if (isPolymorphicQualifier(anno)) {
          continue;
        } else if (result == null) {
          result = anno;
        } else {
          throw new BugInCF(
              "Bug in checker implementation:  type hierarchy is not a lattice.%n"
                  + "There is no unique "
                  + (lub ? "lub" : "glb")
                  + "(%s, %s).%n"
                  + "Two incompatible candidates are: %s %s",
              a1,
              a2,
              result,
              anno);
        }
      }
      return result;
    }
  }

  /** Two annotations; used for caching the result of calls to lub and glb. */
  private static class AnnotationPair {
    /** The first annotation. */
    public final AnnotationMirror a1;
    /** The second annotation. */
    public final AnnotationMirror a2;
    /** The cached hashCode of this; -1 until computed. */
    private int hashCode = -1;

    /** Create a new AnnotationPair. */
    public AnnotationPair(AnnotationMirror a1, AnnotationMirror a2) {
      this.a1 = a1;
      this.a2 = a2;
    }

    @Pure
    @Override
    public int hashCode() {
      if (hashCode == -1) {
        hashCode =
            Objects.hash(AnnotationUtils.annotationName(a1), AnnotationUtils.annotationName(a2));
      }
      return hashCode;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (!(o instanceof AnnotationPair)) {
        return false;
      }
      AnnotationPair other = (AnnotationPair) o;
      if (AnnotationUtils.areSameByName(a1, other.a1)
          && AnnotationUtils.areSameByName(a2, other.a2)) {
        return true;
      }
      if (AnnotationUtils.areSameByName(a2, other.a1)
          && AnnotationUtils.areSameByName(a1, other.a2)) {
        return true;
      }
      return false;
    }

    @SideEffectFree
    @Override
    public String toString() {
      return "AnnotationPair(" + a1 + ", " + a2 + ")";
    }
  }
}
