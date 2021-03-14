package org.checkerframework.framework.type.poly;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.visitor.EquivalentAtmComboScanner;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.AnnotationMirrorMap;
import org.checkerframework.framework.util.AnnotationMirrorSet;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Implements framework support for qualifier polymorphism.
 *
 * <p>{@link DefaultQualifierPolymorphism} implements the abstract methods in this class. Subclasses
 * can alter the way instantiations of polymorphic qualifiers are {@link #combine combined}.
 *
 * <p>An "instantiation" is a mapping from declaration type to use-site type &mdash; that is, a
 * mapping from {@code @Poly*} to concrete qualifiers.
 *
 * <p>The implementation performs these steps:
 *
 * <ul>
 *   <li>the PolyCollector creates an instantiation
 *   <li>if the instantiation is non-empty: the Replacer does resolution -- that is, it replaces
 *       each occurrence of {@code @Poly*} by the concrete qualifier it maps to in the instantiation
 *   <li>if the instantiation is empty, the Completer replaces each {@code @Poly*} by the top
 *       qualifier
 * </ul>
 */
public abstract class AbstractQualifierPolymorphism implements QualifierPolymorphism {

  /** Annotated type factory. */
  protected final AnnotatedTypeFactory atypeFactory;

  /** The qualifier hierarchy to use. */
  protected final QualifierHierarchy qualHierarchy;

  /**
   * The polymorphic qualifiers: mapping from a polymorphic qualifier of {@code qualHierarchy} to
   * the top qualifier of that hierarchy.
   */
  protected final AnnotationMirrorMap<AnnotationMirror> polyQuals = new AnnotationMirrorMap<>();

  /**
   * The qualifiers at the top of {@code qualHierarchy}. These are the values in {@code polyQuals}.
   */
  protected final AnnotationMirrorSet topQuals;

  /** Determines the instantiations for each polymorphic qualifier. */
  private PolyCollector collector = new PolyCollector();

  /** Resolves each polymorphic qualifier by replacing it with its instantiation. */
  private final SimpleAnnotatedTypeScanner<Void, AnnotationMirrorMap<AnnotationMirror>> replacer;

  /**
   * Completes a type by removing any unresolved polymorphic qualifiers, replacing them with the
   * bottom qualifiers.
   */
  private final SimpleAnnotatedTypeScanner<Void, Void> completer;

  /** Mapping from poly qualifier to its instantiation for types with a qualifier parameter. */
  protected final AnnotationMirrorMap<AnnotationMirror> polyInstantiationForQualifierParameter =
      new AnnotationMirrorMap<>();

  /**
   * Creates an {@link AbstractQualifierPolymorphism} instance that uses the given checker for
   * querying type qualifiers and the given factory for getting annotated types. Subclasses need to
   * add polymorphic qualifiers to {@code this.polyQuals}.
   *
   * @param env the processing environment
   * @param factory the factory for the current checker
   */
  protected AbstractQualifierPolymorphism(ProcessingEnvironment env, AnnotatedTypeFactory factory) {
    this.atypeFactory = factory;
    this.qualHierarchy = factory.getQualifierHierarchy();
    this.topQuals = new AnnotationMirrorSet(qualHierarchy.getTopAnnotations());

    this.completer =
        new SimpleAnnotatedTypeScanner<>(
            (type, p) -> {
              for (Map.Entry<AnnotationMirror, AnnotationMirror> entry : polyQuals.entrySet()) {
                AnnotationMirror poly = entry.getKey();
                AnnotationMirror top = entry.getValue();
                if (type.hasAnnotation(poly)) {
                  type.removeAnnotation(poly);
                  if (type.getKind() != TypeKind.TYPEVAR && type.getKind() != TypeKind.WILDCARD) {
                    // Do not add qualifiers to type variables and
                    // wildcards
                    type.addAnnotation(this.qualHierarchy.getBottomAnnotation(top));
                  }
                }
              }
              return null;
            });

    this.replacer =
        new SimpleAnnotatedTypeScanner<>(
            (type, map) -> {
              replace(type, map);
              return null;
            });
  }

  /**
   * Reset to allow reuse of the same instance. Subclasses should override this method. The
   * overriding implementation should clear its additional state and then call the super
   * implementation.
   */
  protected void reset() {
    collector.reset();
    replacer.reset();
    completer.reset();
    polyInstantiationForQualifierParameter.clear();
  }

  /**
   * Resolves polymorphism annotations for the given type.
   *
   * @param tree the tree associated with the type
   * @param type the type to annotate
   */
  @Override
  public void resolve(MethodInvocationTree tree, AnnotatedExecutableType type) {
    if (polyQuals.isEmpty()) {
      return;
    }

    // javac produces enum super calls with zero arguments even though the
    // method element requires two.
    // See also BaseTypeVisitor.visitMethodInvocation and
    // CFGBuilder.CFGTranslationPhaseOne.visitMethodInvocation.
    if (TreeUtils.isEnumSuper(tree)) {
      return;
    }
    List<AnnotatedTypeMirror> parameters =
        AnnotatedTypes.expandVarArgs(atypeFactory, type, tree.getArguments());
    List<AnnotatedTypeMirror> arguments =
        AnnotatedTypes.getAnnotatedTypes(atypeFactory, parameters, tree.getArguments());

    AnnotationMirrorMap<AnnotationMirror> instantiationMapping =
        collector.visit(arguments, parameters);

    // For super() and this() method calls, getReceiverType(tree) does not return the correct
    // type. So, just skip those.  This is consistent with skipping receivers of constructors below.
    if (type.getReceiverType() != null
        && !TreeUtils.isSuperConstructorCall(tree)
        && !TreeUtils.isThisConstructorCall(tree)) {
      instantiationMapping =
          collector.reduce(
              instantiationMapping,
              collector.visit(atypeFactory.getReceiverType(tree), type.getReceiverType()));
    }

    if (instantiationMapping != null && !instantiationMapping.isEmpty()) {
      replacer.visit(type, instantiationMapping);
    } else {
      completer.visit(type);
    }
    reset();
  }

  @Override
  public void resolve(NewClassTree tree, AnnotatedExecutableType type) {
    if (polyQuals.isEmpty()) {
      return;
    }
    List<AnnotatedTypeMirror> parameters =
        AnnotatedTypes.expandVarArgs(atypeFactory, type, tree.getArguments());
    List<AnnotatedTypeMirror> arguments =
        AnnotatedTypes.getAnnotatedTypes(atypeFactory, parameters, tree.getArguments());

    AnnotationMirrorMap<AnnotationMirror> instantiationMapping =
        collector.visit(arguments, parameters);
    // TODO: poly on receiver for constructors?
    // instantiationMapping = collector.reduce(instantiationMapping,
    //        collector.visit(factory.getReceiverType(tree), type.getReceiverType()));

    AnnotatedTypeMirror newClassType = atypeFactory.fromNewClass(tree);
    instantiationMapping =
        collector.reduce(
            instantiationMapping, mapQualifierToPoly(newClassType, type.getReturnType()));

    if (instantiationMapping != null && !instantiationMapping.isEmpty()) {
      replacer.visit(type, instantiationMapping);
    } else {
      completer.visit(type);
    }
    reset();
  }

  @Override
  public void resolve(VariableElement field, AnnotatedTypeMirror owner, AnnotatedTypeMirror type) {
    if (polyQuals.isEmpty()) {
      return;
    }
    AnnotationMirrorMap<AnnotationMirror> matchingMapping = new AnnotationMirrorMap<>();
    polyQuals.forEach(
        (polyAnnotation, topAnno) -> {
          AnnotationMirror annoOnOwner = owner.getAnnotationInHierarchy(topAnno);
          if (annoOnOwner != null) {
            matchingMapping.put(polyAnnotation, annoOnOwner);
          }
        });
    if (!matchingMapping.isEmpty()) {
      replacer.visit(type, matchingMapping);
    } else {
      completer.visit(type);
    }
    reset();
  }

  @Override
  public void resolve(
      AnnotatedExecutableType functionalInterface, AnnotatedExecutableType memberReference) {
    for (AnnotationMirror type : functionalInterface.getReturnType().getAnnotations()) {
      if (atypeFactory.getQualifierHierarchy().isPolymorphicQualifier(type)) {
        // functional interface has a polymorphic qualifier, so they should not be resolved
        // on memberReference.
        return;
      }
    }
    AnnotationMirrorMap<AnnotationMirror> instantiationMapping;

    List<AnnotatedTypeMirror> parameters = memberReference.getParameterTypes();
    List<AnnotatedTypeMirror> args = functionalInterface.getParameterTypes();
    if (args.size() == parameters.size() + 1) {
      // If the member reference is a reference to an instance method of an arbitrary
      // object, then first parameter of the functional interface corresponds to the
      // receiver of the member reference.
      List<AnnotatedTypeMirror> newParameters = new ArrayList<>(parameters.size() + 1);
      newParameters.add(memberReference.getReceiverType());
      newParameters.addAll(parameters);
      parameters = newParameters;
      instantiationMapping = new AnnotationMirrorMap<>();
    } else {
      if (memberReference.getReceiverType() != null
          && functionalInterface.getReceiverType() != null) {
        instantiationMapping =
            mapQualifierToPoly(
                functionalInterface.getReceiverType(), memberReference.getReceiverType());
      } else {
        instantiationMapping = new AnnotationMirrorMap<>();
      }
    }
    // Deal with varargs
    if (memberReference.isVarArgs() && !functionalInterface.isVarArgs()) {
      parameters = AnnotatedTypes.expandVarArgsFromTypes(memberReference, args);
    }

    instantiationMapping =
        collector.reduce(instantiationMapping, collector.visit(args, parameters));

    if (instantiationMapping != null && !instantiationMapping.isEmpty()) {
      replacer.visit(memberReference, instantiationMapping);
    } else {
      // TODO: Do we need this (return type?)
      completer.visit(memberReference);
    }
    reset();
  }

  /**
   * If the primary annotation of {@code polyType} is a polymorphic qualifier, then it is mapped to
   * the primary annotation of {@code type} and the map is returned. Otherwise, an empty map is
   * returned.
   *
   * @param type type with qualifier to us in the map
   * @param polyType type that may have polymorphic qualifiers
   * @return a mapping from the polymorphic qualifiers in {@code polyType} to the qualifiers in
   *     {@code type}
   */
  private AnnotationMirrorMap<AnnotationMirror> mapQualifierToPoly(
      AnnotatedTypeMirror type, AnnotatedTypeMirror polyType) {
    AnnotationMirrorMap<AnnotationMirror> result = new AnnotationMirrorMap<>();

    for (Map.Entry<AnnotationMirror, AnnotationMirror> kv : polyQuals.entrySet()) {
      AnnotationMirror top = kv.getValue();
      AnnotationMirror poly = kv.getKey();
      if (polyType.hasAnnotation(poly)) {
        AnnotationMirror typeQual = type.getAnnotationInHierarchy(top);
        if (typeQual != null) {
          if (atypeFactory.hasQualifierParameterInHierarchy(type, top)) {
            polyInstantiationForQualifierParameter.put(poly, typeQual);
          }
          result.put(poly, typeQual);
        }
      }
    }
    return result;
  }

  /**
   * Returns annotation that is the combination of the two annotations. The annotations are
   * instantiations for {@code polyQual}.
   *
   * <p>The combination is typically their least upper bound. (It could be the GLB in the case that
   * all arguments to a polymorphic method must have the same annotation.)
   *
   * @param polyQual polymorphic qualifier for which {@code a1} and {@code a2} are instantiations
   * @param a1 an annotation that is an instantiation of {@code polyQual}
   * @param a2 an annotation that is an instantiation of {@code polyQual}
   * @return an annotation that is the combination of the two annotations
   */
  protected abstract AnnotationMirror combine(
      AnnotationMirror polyQual, AnnotationMirror a1, AnnotationMirror a2);

  /**
   * Replaces the top-level polymorphic annotations in {@code type} with the instantiations in
   * {@code replacements}.
   *
   * <p>This method is called on all parts of a type.
   *
   * @param type AnnotatedTypeMirror whose poly annotations are replaced; it is side-effected by
   *     this method
   * @param replacements mapping from polymorphic annotation to instantiation
   */
  protected abstract void replace(
      AnnotatedTypeMirror type, AnnotationMirrorMap<AnnotationMirror> replacements);

  /**
   * A helper class that resolves the polymorphic qualifiers with the most restrictive qualifier. It
   * returns a mapping from the polymorphic qualifier to the substitution for that qualifier.
   */
  private class PolyCollector
      extends EquivalentAtmComboScanner<AnnotationMirrorMap<AnnotationMirror>, Void> {

    /**
     * Set of {@link AnnotatedTypeVariable} or {@link AnnotatedWildcardType} that have been visited.
     * Used to prevent infinite recursion on recursive types.
     *
     * <p>Uses reference equality rather than equals because the visitor may visit two types that
     * are structurally equal, but not actually the same. For example, the wildcards in {@code
     * Pair<?,?>} may be equal, but they both should be visited.
     */
    private final Set<AnnotatedTypeMirror> visitedTypes =
        Collections.newSetFromMap(new IdentityHashMap<AnnotatedTypeMirror, Boolean>());

    /**
     * Returns true if the {@link AnnotatedTypeMirror} has been visited. If it has not, then it is
     * added to the list of visited AnnotatedTypeMirrors.
     */
    private boolean visited(AnnotatedTypeMirror atm) {
      return !visitedTypes.add(atm);
    }

    @Override
    protected AnnotationMirrorMap<AnnotationMirror> scanWithNull(
        AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void aVoid) {
      return new AnnotationMirrorMap<>();
    }

    @Override
    public AnnotationMirrorMap<AnnotationMirror> reduce(
        AnnotationMirrorMap<AnnotationMirror> r1, AnnotationMirrorMap<AnnotationMirror> r2) {

      if (r1 == null || r1.isEmpty()) {
        return r2;
      }
      if (r2 == null || r2.isEmpty()) {
        return r1;
      }

      AnnotationMirrorMap<AnnotationMirror> res = new AnnotationMirrorMap<>();
      // Ensure that all qualifiers from r1 and r2 are visited.
      AnnotationMirrorSet r2remain = new AnnotationMirrorSet();
      r2remain.addAll(r2.keySet());
      for (Map.Entry<AnnotationMirror, AnnotationMirror> entry : r1.entrySet()) {
        AnnotationMirror polyQual = entry.getKey();
        AnnotationMirror a1Annos = entry.getValue();
        AnnotationMirror a2Annos = r2.get(polyQual);
        if (a2Annos == null) {
          res.put(polyQual, a1Annos);
        } else {
          res.put(polyQual, combine(polyQual, a1Annos, a2Annos));
        }
        r2remain.remove(polyQual);
      }
      for (AnnotationMirror key2 : r2remain) {
        res.put(key2, r2.get(key2));
      }
      return res;
    }

    /**
     * Calls {@link #visit(AnnotatedTypeMirror, AnnotatedTypeMirror)} for each type in {@code
     * types}.
     *
     * @param types AnnotateTypeMirrors used to find instantiations
     * @param polyTypes AnnotatedTypeMirrors that may have polymorphic qualifiers
     * @return a mapping of polymorphic qualifiers to their instantiations
     */
    private AnnotationMirrorMap<AnnotationMirror> visit(
        Iterable<? extends AnnotatedTypeMirror> types,
        Iterable<? extends AnnotatedTypeMirror> polyTypes) {
      AnnotationMirrorMap<AnnotationMirror> result = new AnnotationMirrorMap<>();

      Iterator<? extends AnnotatedTypeMirror> itert = types.iterator();
      Iterator<? extends AnnotatedTypeMirror> itera = polyTypes.iterator();

      while (itert.hasNext() && itera.hasNext()) {
        AnnotatedTypeMirror type = itert.next();
        AnnotatedTypeMirror actualType = itera.next();
        result = reduce(result, visit(type, actualType));
      }
      if (itert.hasNext()) {
        throw new BugInCF(
            "PolyCollector.visit: types is longer than polyTypes:%n"
                + "  types = %s%n  polyTypes = %s%n",
            types, polyTypes);
      }
      if (itera.hasNext()) {
        throw new BugInCF(
            "PolyCollector.visit: types is shorter than polyTypes:%n"
                + "  types = %s%n  polyTypes = %s%n",
            types, polyTypes);
      }
      return result;
    }

    /**
     * Creates a mapping of polymorphic qualifiers to their instantiations by visiting each
     * composite type in {@code type}.
     *
     * @param type AnnotateTypeMirror used to find instantiations
     * @param polyType AnnotatedTypeMirror that may have polymorphic qualifiers
     * @return a mapping of polymorphic qualifiers to their instantiations
     */
    private AnnotationMirrorMap<AnnotationMirror> visit(
        AnnotatedTypeMirror type, AnnotatedTypeMirror polyType) {
      if (type.getKind() == TypeKind.NULL) {
        return mapQualifierToPoly(type, polyType);
      }

      if (type.getKind() == TypeKind.WILDCARD) {
        AnnotatedWildcardType wildcardType = (AnnotatedWildcardType) type;
        if (wildcardType.getExtendsBound().getKind() == TypeKind.WILDCARD) {
          wildcardType = (AnnotatedWildcardType) wildcardType.getExtendsBound();
        }
        if (wildcardType.isUninferredTypeArgument()) {
          return mapQualifierToPoly(wildcardType.getExtendsBound(), polyType);
        }

        switch (polyType.getKind()) {
          case WILDCARD:
            AnnotatedTypeMirror asSuper =
                AnnotatedTypes.asSuper(atypeFactory, wildcardType, polyType);
            return visit(asSuper, polyType, null);
          case TYPEVAR:
            return mapQualifierToPoly(wildcardType.getExtendsBound(), polyType);
          default:
            return mapQualifierToPoly(wildcardType.getExtendsBound(), polyType);
        }
      }

      AnnotatedTypeMirror asSuper = AnnotatedTypes.asSuper(atypeFactory, type, polyType);

      return visit(asSuper, polyType, null);
    }

    @Override
    protected String defaultErrorMessage(
        AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void aVoid) {
      return String.format(
          "AbstractQualifierPolymorphism: Unexpected combination: type1: %s (%s) type2: %s (%s).",
          type1, type1.getKind(), type2, type2.getKind());
    }

    @Override
    public AnnotationMirrorMap<AnnotationMirror> visitArray_Array(
        AnnotatedArrayType type1, AnnotatedArrayType type2, Void aVoid) {
      AnnotationMirrorMap<AnnotationMirror> result = mapQualifierToPoly(type1, type2);
      return reduce(result, super.visitArray_Array(type1, type2, aVoid));
    }

    @Override
    public AnnotationMirrorMap<AnnotationMirror> visitDeclared_Declared(
        AnnotatedDeclaredType type1, AnnotatedDeclaredType type2, Void aVoid) {
      // Don't call super because asSuper has to be called on each type argument.
      if (visited(type2)) {
        return new AnnotationMirrorMap<>();
      }

      AnnotationMirrorMap<AnnotationMirror> result = mapQualifierToPoly(type1, type2);

      Iterator<AnnotatedTypeMirror> type2Args = type2.getTypeArguments().iterator();
      for (AnnotatedTypeMirror type1Arg : type1.getTypeArguments()) {
        AnnotatedTypeMirror type2Arg = type2Args.next();
        if (TypesUtils.isErasedSubtype(
            type1Arg.getUnderlyingType(),
            type2Arg.getUnderlyingType(),
            atypeFactory.getChecker().getTypeUtils())) {
          result = reduce(result, visit(type1Arg, type2Arg));
        } // else an unchecked warning was issued by Java, ignore this part of the type.
      }

      return result;
    }

    @Override
    public AnnotationMirrorMap<AnnotationMirror> visitIntersection_Intersection(
        AnnotatedIntersectionType type1, AnnotatedIntersectionType type2, Void aVoid) {
      AnnotationMirrorMap<AnnotationMirror> result = mapQualifierToPoly(type1, type2);
      return reduce(result, super.visitIntersection_Intersection(type1, type2, aVoid));
    }

    @Override
    public AnnotationMirrorMap<AnnotationMirror> visitNull_Null(
        AnnotatedNullType type1, AnnotatedNullType type2, Void aVoid) {
      return mapQualifierToPoly(type1, type2);
    }

    @Override
    public AnnotationMirrorMap<AnnotationMirror> visitPrimitive_Primitive(
        AnnotatedPrimitiveType type1, AnnotatedPrimitiveType type2, Void aVoid) {
      return mapQualifierToPoly(type1, type2);
    }

    @Override
    public AnnotationMirrorMap<AnnotationMirror> visitTypevar_Typevar(
        AnnotatedTypeVariable type1, AnnotatedTypeVariable type2, Void aVoid) {
      if (visited(type2)) {
        return new AnnotationMirrorMap<>();
      }
      AnnotationMirrorMap<AnnotationMirror> result = mapQualifierToPoly(type1, type2);
      return reduce(result, super.visitTypevar_Typevar(type1, type2, aVoid));
    }

    @Override
    public AnnotationMirrorMap<AnnotationMirror> visitUnion_Union(
        AnnotatedUnionType type1, AnnotatedUnionType type2, Void aVoid) {
      AnnotationMirrorMap<AnnotationMirror> result = mapQualifierToPoly(type1, type2);
      return reduce(result, super.visitUnion_Union(type1, type2, aVoid));
    }

    @Override
    public AnnotationMirrorMap<AnnotationMirror> visitWildcard_Wildcard(
        AnnotatedWildcardType type1, AnnotatedWildcardType type2, Void aVoid) {
      if (visited(type2)) {
        return new AnnotationMirrorMap<>();
      }
      AnnotationMirrorMap<AnnotationMirror> result = mapQualifierToPoly(type1, type2);
      return reduce(result, super.visitWildcard_Wildcard(type1, type2, aVoid));
    }

    /** Resets the state. */
    public void reset() {
      this.visitedTypes.clear();
      this.visited.clear();
    }
  }
}
