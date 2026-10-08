package org.checkerframework.framework.util.typeinference8.util;

import com.sun.tools.javac.code.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.typeinference8.bound.BoundSet;
import org.checkerframework.framework.util.typeinference8.types.AbstractQualifier;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.Dependencies;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds.BoundKind;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.BugInCF;

/**
 * Resolution finds an instantiation for each variable in a given set of variables. It does this
 * using all the bounds on a variable. Because a bound on a variable may be another unresolved
 * variable, the order in which the variables are resolved must be computed before resolution. If
 * the set of variables contains any captured variables, then a different resolution algorithm is
 * used. If a set of variables does not contain a captured variable, but the resolution fails, then
 * the resolution algorithm for captured variables is used.
 *
 * <p>Resolution is discussed in <a
 * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-18.html#jls-18.4">JLS Section
 * 18.4</a>.
 *
 * <p>Entry point is two static methods, {@link #resolve(Collection, BoundSet,
 * Java8InferenceContext)} and {@link #resolve(Variable, BoundSet, Java8InferenceContext)}, which
 * create {@link Resolution} objects that actually perform the resolution.
 */
public final class Resolution {

  /**
   * Instantiates a set of variables, {@code as}.
   *
   * <p>This method removes from {@code as} every variable that already has an instantiation.
   *
   * @param as the set of variables to resolve; this method removes elements from it
   * @param boundSet the bound set that includes {@code as}
   * @param context Java8InferenceContext
   * @return bound set where {@code as} have instantiations
   */
  public static BoundSet resolve(
      Collection<Variable> as, BoundSet boundSet, Java8InferenceContext context) {
    List<Variable> requested = new ArrayList<>(as);

    // Remove any variables that already have instantiations
    List<Variable> resolvedVars = boundSet.getInstantiatedVariables();
    as.removeAll(resolvedVars);
    if (as.isEmpty()) {
      return resolveIgnoredAnnotations(requested, boundSet, context);
    }
    // Calculate the dependencies between variables. (A variable depends on another if it is
    // included in one of its bounds.)
    Dependencies dependencies = boundSet.getDependencies();
    Queue<Variable> unresolvedVars = new ArrayDeque<>(as);
    for (Variable var : as) {
      for (Variable dep : dependencies.get(var)) {
        if (!unresolvedVars.contains(dep)) {
          unresolvedVars.add(dep);
        }
      }
    }

    // Remove any variables that already have instantiations
    unresolvedVars.removeAll(resolvedVars);
    if (unresolvedVars.isEmpty()) {
      return resolveIgnoredAnnotations(requested, boundSet, context);
    }
    // `resolution.resolve` empties `unresolvedVars`, and resolving them can give any of them an
    // instantiation that ignores annotations.
    LinkedHashSet<Variable> resolved = new LinkedHashSet<>(requested);
    resolved.addAll(unresolvedVars);

    // Resolve the variables
    Resolution resolution = new Resolution(context, dependencies);
    boundSet = resolution.resolve(boundSet, unresolvedVars);
    checkNoFalse(boundSet, "after resolving", as);
    return resolveIgnoredAnnotations(new ArrayList<>(resolved), boundSet, context);
  }

  /**
   * Instantiates the variable {@code a}.
   *
   * @param a the variable to resolve
   * @param boundSet the bound set that includes {@code a}
   * @param context Java8InferenceContext
   * @return bound set where {@code a} is instantiated
   */
  public static BoundSet resolve(Variable a, BoundSet boundSet, Java8InferenceContext context) {
    if (a.getBounds().hasInstantiation()) {
      return resolveIgnoredAnnotations(Collections.singletonList(a), boundSet, context);
    }
    Dependencies dependencies = boundSet.getDependencies();

    LinkedHashSet<Variable> unresolvedVars = new LinkedHashSet<>();
    unresolvedVars.add(a);
    Resolution resolution = new Resolution(context, dependencies);
    boundSet = resolution.resolveSmallestSet(unresolvedVars, boundSet);
    checkNoFalse(boundSet, "after resolving", unresolvedVars);
    return resolveIgnoredAnnotations(Collections.singletonList(a), boundSet, context);
  }

  /**
   * Resolves the annotations of each variable in {@code vars} whose instantiation ignores
   * annotations (see {@link AbstractType#ignoreAnnotations}). Such an instantiation gives only the
   * variable's Java type: its annotations say nothing about the variable's, because they were
   * related to an explicit annotation on a use of the variable, so this method takes the
   * annotations from the variable's other bounds instead. As {@link #resolveWithLowerBounds} would,
   * it adds the bound {@code var = t}, where {@code t} is the least upper bound of the variable's
   * proper lower bounds, with its qualifier lower bounds applied, viewed as the variable's Java
   * type ({@link AbstractType#asSuper}). Every lower bound is a subtype of that Java type, so
   * {@code t} is too. If the variable has no proper lower bound, then {@code t} is the
   * instantiation with the qualifier lower bounds applied, and it still ignores annotations.
   *
   * <p>If {@code t} respects annotations, then it replaces the instantiation. A type that was made
   * by substituting the instantiation has recorded where (see {@link
   * ProperType#applyInstantiations}), so incorporating the new bound substitutes {@code t} there.
   *
   * @param vars variables, each of which has an instantiation
   * @param boundSet the bound set that includes {@code vars}
   * @param context the context
   * @return {@code boundSet}, with the new bounds incorporated
   */
  private static BoundSet resolveIgnoredAnnotations(
      List<Variable> vars, BoundSet boundSet, Java8InferenceContext context) {
    boolean added = false;
    for (Variable var : vars) {
      ProperType instantiation = var.getInstantiation();
      if (instantiation == null || !instantiation.ignoreAnnotations) {
        continue;
      }
      ProperType t = null;
      Set<ProperType> lowerBounds = var.getBounds().findProperLowerBounds();
      if (!lowerBounds.isEmpty()) {
        t =
            viewAs(
                lubOfLowerBounds(var, lowerBounds, false, context),
                instantiation.getJavaType(),
                context);
      }
      if (t == null) {
        t = lubOfLowerBounds(var, var.getBounds().findLowerBoundsForAnnotations(), true, context);
      }
      added |= var.getBounds().addBound(null, BoundKind.EQUAL, t);
    }
    if (added) {
      boundSet.incorporateToFixedPoint(new BoundSet(context));
    }
    return boundSet;
  }

  /**
   * Returns {@code type} viewed as {@code javaType}, a supertype of it, or null if that view cannot
   * be computed. A type variable, including a captured one, is viewed through its upper bounds,
   * whose annotations it has; another type is viewed through {@link AbstractType#asSuper}.
   *
   * @param type a proper type
   * @param javaType a supertype of the Java type of {@code type}
   * @param context the context
   * @return {@code type} viewed as {@code javaType}, or null
   */
  private static @Nullable ProperType viewAs(
      ProperType type, TypeMirror javaType, Java8InferenceContext context) {
    AbstractType current = type;
    while (current != null && current.isProper()) {
      if (context.types.isSameType((Type) current.getJavaType(), (Type) javaType)) {
        return (ProperType) current;
      }
      if (current.getTypeKind() == TypeKind.TYPEVAR) {
        current = current.getTypeVarUpperBound();
      } else {
        AbstractType asSuper = current.asSuper(javaType);
        return asSuper != null
                && asSuper.isProper()
                && context.types.isSameType((Type) asSuper.getJavaType(), (Type) javaType)
            ? (ProperType) asSuper
            : null;
      }
    }
    return null;
  }

  /**
   * Throws {@link BugInCF} if {@code boundSet} contains the false bound.
   *
   * @param boundSet a bound set that should not contain the false bound
   * @param where the location where the check is performed, for the error message
   * @param vars the variables being resolved, for the error message
   */
  private static void checkNoFalse(BoundSet boundSet, String where, Collection<Variable> vars) {
    if (boundSet.containsFalse()) {
      throw new BugInCF("Bound set contains false %s %s.", where, vars);
    }
  }

  /** The context. */
  private final Java8InferenceContext context;

  /** The set of dependencies between the variables. */
  private final Dependencies dependencies;

  /**
   * Creates a resolution.
   *
   * @param context the context
   * @param dependencies the dependencies
   */
  private Resolution(Java8InferenceContext context, Dependencies dependencies) {
    this.context = context;
    this.dependencies = dependencies;
  }

  /**
   * Resolve all the variables in {@code unresolvedVars}.
   *
   * @param boundSet current bound set
   * @param unresolvedVars a set of unresolved variables that includes all dependencies
   * @return the bounds set with the resolved bounds
   */
  private BoundSet resolve(BoundSet boundSet, Queue<Variable> unresolvedVars) {
    List<Variable> resolvedVars = boundSet.getInstantiatedVariables();

    while (!unresolvedVars.isEmpty()) {
      checkNoFalse(boundSet, "while resolving", unresolvedVars);

      Set<Variable> smallestDependencySet = getSmallestDependencySet(resolvedVars, unresolvedVars);

      // Resolve the smallest unresolved dependency set.
      boundSet = resolveSmallestSet(smallestDependencySet, boundSet);

      resolvedVars = boundSet.getInstantiatedVariables();
      unresolvedVars.removeAll(resolvedVars);
    }
    return boundSet;
  }

  /**
   * Returns the smallest set of unresolved variables that includes any variable on which a variable
   * in the set depends.
   *
   * @param resolvedVars variables that have been resolved
   * @param unresolvedVars variables that have not been resolved; must be non-empty
   * @return the smallest set of unresolved variable
   */
  private Set<Variable> getSmallestDependencySet(
      List<Variable> resolvedVars, Queue<Variable> unresolvedVars) {
    Set<Variable> smallestDependencySet = null;
    // This loop is looking for the smallest set of dependencies that have not been resolved.
    for (Variable alpha : unresolvedVars) {
      Set<Variable> alphasDependencySet = dependencies.get(alpha);
      alphasDependencySet.removeAll(resolvedVars);

      if (smallestDependencySet == null
          || alphasDependencySet.size() < smallestDependencySet.size()) {
        smallestDependencySet = alphasDependencySet;
      }

      if (smallestDependencySet.size() == 1) {
        // If the size is 1, then alpha has the smallest possible set of unresolved
        // dependencies.
        // (A variable is always dependent on itself.) So, stop looking for smaller ones.
        break;
      }
    }
    if (smallestDependencySet == null) {
      throw new BugInCF("getSmallestDependencySet: no unresolved variables");
    }
    return smallestDependencySet;
  }

  /**
   * Resolves {@code as}
   *
   * @param as the smallest set of unresolved variables that includes any variable on which a
   *     variable in the set depends
   * @param boundSet current bounds set
   * @return current bound set
   */
  private BoundSet resolveSmallestSet(Set<Variable> as, BoundSet boundSet) {
    checkNoFalse(boundSet, "on entry to resolveSmallestSet for", as);

    if (boundSet.containsCapture(as)) {
      // Wait to resolve variables that have an equal bound to a capture variable that has not been
      // resloved.
      Set<Variable> deferred = new LinkedHashSet<>();
      for (Variable v : as) {
        if (!v.isCaptureVariable() && hasUnresolvedEqualBoundToCaptureWithin(v, as)) {
          deferred.add(v);
        }
      }
      Set<Variable> toResolveNow = new LinkedHashSet<>(as);
      toResolveNow.removeAll(deferred);

      BoundSet resolvedBounds = resolveWithoutCapture(toResolveNow, boundSet);
      toResolveNow.removeAll(boundSet.getInstantiatedVariables());
      // Then resolve the capture variables (and any non-captures that depend on them directly).
      deferred.addAll(toResolveNow);
      return resolveWithCapture(deferred, resolvedBounds, context);
    } else {
      BoundSet copy = new BoundSet(boundSet);
      // Save the current bounds in case the first attempt at resolution fails.
      copy.saveBounds();
      try {
        BoundSet resolvedBounds = resolveWithoutCapture(as, boundSet);
        if (!resolvedBounds.containsFalse()) {
          return resolvedBounds;
        }
      } catch (FalseBoundException ex) {
        // Try with capture.
      }
      boundSet = copy;
      // If resolveWithoutCapture fails, then undo all resolved variables from the failed attempt.
      boundSet.restore();
      return resolveWithCapture(as, boundSet, context);
    }
  }

  /**
   * Returns true if {@code v} has an {@code EQUAL} bound that mentions a capture variable in {@code
   * as} that does not yet have an instantiation.
   *
   * @param v a variable
   * @param as a set of variables being resolved together
   * @return true if {@code v} has an unresolved {@code EQUAL} bound to a capture variable in {@code
   *     as}
   */
  private static boolean hasUnresolvedEqualBoundToCaptureWithin(Variable v, Set<Variable> as) {
    for (AbstractType t : v.getBounds().getBoundsOfKind(VariableBounds.BoundKind.EQUAL)) {
      for (Variable mentioned : t.getInferenceVariables()) {
        if (mentioned.isCaptureVariable()
            && as.contains(mentioned)
            && !mentioned.getBounds().hasInstantiation()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Apply the instantiated variables to the bounds of the variables in {@code variables}. This may
   * result in an instantiation being found of a variable in {@code variables}. All instantiated
   * variables are removed from {@code variables}.
   *
   * @param variables a list of variables; side-effected by this method
   */
  private static void applyAndRemoveInstantiations(List<Variable> variables) {
    boolean changed;
    do {
      changed = false;
      for (Variable v : variables) {
        if (v.getBounds().hasInstantiation()) {
          continue;
        }
        v.getBounds().applyInstantiationsToBounds();
        if (v.getBounds().hasInstantiation()) {
          // If v now has an instantiation, then loop through all the variables again to apply it to
          // all the bounds of the other variables.
          changed = true;
        }
      }
    } while (changed);
    variables.removeIf(v -> v.getBounds().hasInstantiation());
  }

  /**
   * Resolves all variables in {@code as} by instantiating each to the greatest lower bound of its
   * proper upper bounds. This may fail and resolveWithCapture will need to be used instead.
   *
   * <p>Resolves all non-captured variables in {@code as} by:
   *
   * <ul>
   *   <li>Resolving all variables with proper lower bounds by instantiating them to the least upper
   *       bound of their proper lower bounds. The instantiations are applies to the bounds of all
   *       variables in {@code as}. Then this step is repeated until no new instantiations are
   *       found.
   *   <li>Resolving all remaining variables using the greatest lower bound of their proper upper
   *       bounds.
   * </ul>
   *
   * Then all bounds are reduced and incorporated into {@code boundSet}.
   *
   * <p>Any of these steps may fail in which case the resulting bound set will contain false and
   * {@link #resolveWithCapture(Set, BoundSet, Java8InferenceContext)} should be used instead.
   *
   * @param as variables to resolve
   * @param boundSet the bound set to use
   * @return the resolved bound set
   */
  private BoundSet resolveWithoutCapture(Set<Variable> as, BoundSet boundSet) {
    BoundSet resolvedBoundSet = new BoundSet(context);
    List<Variable> varsToResolve = new ArrayList<>(as);
    varsToResolve.removeIf(Variable::isCaptureVariable);
    applyAndRemoveInstantiations(varsToResolve);

    // Resolve variables with proper lower bounds first.
    boolean changed = true;
    while (changed) {
      changed = false;
      for (Variable ai : varsToResolve) {
        Set<ProperType> lowerBounds = ai.getBounds().findProperLowerBounds();
        if (!lowerBounds.isEmpty()) {
          resolveWithLowerBounds(ai, lowerBounds, context);
          changed = true;
        }
      }
      applyAndRemoveInstantiations(varsToResolve);
    }

    // Resolve with upper bounds.
    for (Variable ai : varsToResolve) {
      Set<ProperType> upperBounds = ai.getBounds().findProperUpperBounds();
      if (!upperBounds.isEmpty()) {
        // Object is always an upper bound so this branch is always executed.
        resolveWithUpperBounds(ai, upperBounds);
      }
    }
    applyAndRemoveInstantiations(varsToResolve);

    if (!varsToResolve.isEmpty()) {
      resolvedBoundSet.addFalse();
    }
    boundSet.incorporateToFixedPoint(resolvedBoundSet);
    return boundSet;
  }

  /**
   * Resolves {@code ai} by instantiating it to the greatest lower bound of its proper upper bounds.
   *
   * @param ai a variable to resolve
   * @param upperBounds {@code ai}'s nonempty set of proper upper bounds
   */
  private void resolveWithUpperBounds(Variable ai, Set<ProperType> upperBounds) {
    ProperType ti = null;
    // Per JLS 18.4, use RuntimeException only if the bound set contains "throws ai" and *each*
    // proper upper bound of ai is a supertype of RuntimeException.
    boolean useRuntimeException = ai.getBounds().hasThrowsBound();
    for (ProperType liProperType : upperBounds) {
      TypeMirror li = liProperType.getJavaType();
      if (useRuntimeException) {
        useRuntimeException = context.env.getTypeUtils().isSubtype(context.runtimeException, li);
      }
      if (ti == null) {
        ti = liProperType;
      } else {
        ti = (ProperType) context.inferenceTypeFactory.glb(ti, liProperType);
      }
    }
    if (useRuntimeException) {
      ti = context.inferenceTypeFactory.getRuntimeException();
    }
    assert ti != null : "@AssumeAssertion(nullness): upperBounds is nonempty";
    ai.getBounds().addBound(null, BoundKind.EQUAL, ti);
  }

  /**
   * Resolve {@code ai} by instantiating it to the least upper bound of its proper lower bounds.
   *
   * @param ai a variable to resolve
   * @param lowerBounds {@code ai}'s nonempty set of proper lower bounds
   * @param context the context
   */
  private static void resolveWithLowerBounds(
      Variable ai, Set<ProperType> lowerBounds, Java8InferenceContext context) {
    ai.getBounds()
        .addBound(null, BoundKind.EQUAL, lubOfLowerBounds(ai, lowerBounds, false, context));
  }

  /**
   * Returns the least upper bound of {@code lowerBounds}, with the qualifier lower bounds of {@code
   * ai} applied.
   *
   * @param ai a variable
   * @param lowerBounds a nonempty set of proper types, each of which is a lower bound of {@code ai}
   *     or has the Java type of {@code ai}
   * @param includesEqualBounds true if {@code lowerBounds} may include {@code EQUAL} bounds that
   *     ignore annotations (see {@link AbstractType#ignoreAnnotations}), whose root annotations say
   *     nothing about {@code ai}'s in any hierarchy. A proper lower bound's root annotations say
   *     nothing about {@code ai}'s only in the hierarchies of the use's explicit primary
   *     annotation, where {@link
   *     org.checkerframework.framework.util.typeinference8.types.UseOfVariable#addBound} made them
   *     bottom, so lubbing them is already correct.
   * @param context the context
   * @return the least upper bound of {@code lowerBounds}, with the qualifier lower bounds of {@code
   *     ai} applied
   */
  private static ProperType lubOfLowerBounds(
      Variable ai,
      Set<ProperType> lowerBounds,
      boolean includesEqualBounds,
      Java8InferenceContext context) {
    ProperType lubProperType = context.inferenceTypeFactory.lub(lowerBounds);
    assert lubProperType != null : "@AssumeAssertion(nullness): lowerBounds is nonempty";
    Set<AbstractQualifier> qualifierLowerBounds =
        ai.getBounds().getQualifierBoundsOfKind(BoundKind.LOWER);
    if (!qualifierLowerBounds.isEmpty()) {
      // `lub` may return a type that shares its AnnotatedTypeMirror with one of `lowerBounds`,
      // which is still stored in a hash set of bounds.  Replacing annotations in place would
      // change that bound's hash code while it is in the set, so copy before mutating.  Only the
      // root annotations are changed below, so the copy keeps the origin.
      lubProperType = lubProperType.copy();
      QualifierHierarchy qh = context.typeFactory.getQualifierHierarchy();
      AnnotationMirrorSet lubAnnos = AbstractQualifier.lub(qualifierLowerBounds, context);
      if (lubProperType.getAnnotatedType().getKind() != TypeKind.TYPEVAR
          && includesEqualBounds
          && lubProperType.ignoreAnnotations) {
        // The root annotations of `lubProperType` say nothing about `ai`'s, so the qualifier lower
        // bounds replace them rather than being lubbed with them.  Replacing them when only some
        // hierarchies are ignored would discard the others, such as the H1 annotation of a lower
        // bound from a use whose primary annotation is in H2 only.  A type variable's are lubbed
        // into its lower bound, as below, because replacing its primary annotation would also fix
        // its upper bound.
        lubProperType.getAnnotatedType().replaceAnnotations(lubAnnos);
      } else if (lubProperType.getAnnotatedType().getKind() != TypeKind.TYPEVAR) {
        Set<? extends AnnotationMirror> newLubAnnos =
            qh.leastUpperBoundsQualifiersOnly(
                lubAnnos, lubProperType.getAnnotatedType().getPrimaryAnnotations());
        lubProperType.getAnnotatedType().replaceAnnotations(newLubAnnos);
      } else {

        AnnotatedTypeVariable lubTV = (AnnotatedTypeVariable) lubProperType.getAnnotatedType();
        Set<? extends AnnotationMirror> newLubAnnos =
            qh.leastUpperBoundsQualifiersOnly(
                lubAnnos, lubTV.getLowerBound().getPrimaryAnnotations());
        lubTV.getLowerBound().replaceAnnotations(newLubAnnos);
      }
    }
    return lubProperType;
  }

  /**
   * Instantiates the variables in {@code as} by creating fresh type variables using the bounds of
   * the variables.
   *
   * @param as a set of variables to resolve
   * @param boundSet the bounds set to use
   * @param context the context
   * @return the resolved bound set
   */
  private static BoundSet resolveWithCapture(
      Set<Variable> as, BoundSet boundSet, Java8InferenceContext context) {
    checkNoFalse(boundSet, "on entry to resolveWithCapture for", as);
    boundSet.removeCaptures(as);
    List<Variable> asList = new ArrayList<>();
    List<AbstractType> typeArg = new ArrayList<>();

    for (Variable ai : as) {
      ai.getBounds().applyInstantiationsToBounds();
      if (ai.getBounds().hasInstantiation()) {
        // If ai is equal to a variable that was resolved previously,
        // ai would now have an instantiation.
        continue;
      }
      asList.add(ai);
      Set<ProperType> lowerBounds = ai.getBounds().findProperLowerBounds();
      ProperType lowerBound = context.inferenceTypeFactory.lub(lowerBounds);
      if (lowerBound != null) {
        // The annotated type is mutated below and by `createFreshTypeVariable`, but `lub` may
        // return a type that shares its AnnotatedTypeMirror with one of `lowerBounds`, which is
        // still stored in a hash set of bounds.  Replacing annotations in place would change that
        // bound's hash code while it is in the set, so copy before mutating.
        lowerBound =
            (ProperType)
                lowerBound.create(
                    lowerBound.getAnnotatedType().deepCopy(), lowerBound.ignoreAnnotations);
      }

      Set<? extends AnnotationMirror> lowerBoundAnnos;
      Set<AbstractQualifier> qualifierLowerBounds =
          ai.getBounds().getQualifierBoundsOfKind(BoundKind.LOWER);
      if (!qualifierLowerBounds.isEmpty()) {
        QualifierHierarchy qh = context.typeFactory.getQualifierHierarchy();
        lowerBoundAnnos = AbstractQualifier.lub(qualifierLowerBounds, context);
        if (lowerBound != null) {
          if (lowerBound.getAnnotatedType().getKind() != TypeKind.TYPEVAR) {
            Set<? extends AnnotationMirror> newLubAnnos =
                qh.leastUpperBoundsQualifiersOnly(
                    lowerBoundAnnos, lowerBound.getAnnotatedType().getPrimaryAnnotations());
            lowerBound.getAnnotatedType().replaceAnnotations(newLubAnnos);
            lowerBoundAnnos = newLubAnnos;
          } else {
            AnnotatedTypeVariable lubTV = (AnnotatedTypeVariable) lowerBound.getAnnotatedType();
            Set<? extends AnnotationMirror> newLubAnnos =
                qh.leastUpperBoundsQualifiersOnly(
                    lowerBoundAnnos, lubTV.getLowerBound().getPrimaryAnnotations());
            lubTV.getLowerBound().replaceAnnotations(newLubAnnos);
            lowerBoundAnnos = newLubAnnos;
          }
        }
      } else {
        lowerBoundAnnos = Collections.emptySet();
      }

      Set<AbstractType> upperBounds = ai.getBounds().upperBounds();
      // Omit bounds that mention variables outside `as`, such as `alpha` in `ai <: alpha`.
      upperBounds.removeIf(u -> !as.containsAll(u.getInferenceVariables()));
      AbstractType upperBound = context.inferenceTypeFactory.glb(upperBounds);
      if (upperBound != null) {
        // `glb` returns its argument when `upperBounds` is a singleton, and that type is still
        // stored in `ai`'s set of upper bounds.  See the comment about `lowerBound` above.
        upperBound =
            upperBound.create(
                upperBound.getAnnotatedType().deepCopy(), upperBound.ignoreAnnotations);
      }
      Set<? extends AnnotationMirror> upperBoundAnnos;
      Set<AbstractQualifier> qualifierUpperBounds =
          ai.getBounds().getQualifierBoundsOfKind(BoundKind.UPPER);
      if (!qualifierUpperBounds.isEmpty()) {
        upperBoundAnnos = AbstractQualifier.glb(qualifierUpperBounds, context);
        if (upperBound != null) {
          upperBoundAnnos =
              context
                  .typeFactory
                  .getQualifierHierarchy()
                  .greatestLowerBoundsQualifiersOnly(
                      upperBoundAnnos, upperBound.getAnnotatedType().getPrimaryAnnotations());
          upperBound.getAnnotatedType().replaceAnnotations(upperBoundAnnos);
        }
      } else {
        upperBoundAnnos = Collections.emptySet();
      }

      AbstractType freshTypeVar =
          context.inferenceTypeFactory.createFreshTypeVariable(
              lowerBound, lowerBoundAnnos, upperBound, upperBoundAnnos);
      typeArg.add(freshTypeVar);
    }

    List<AbstractType> subsTypeArg = context.inferenceTypeFactory.getSubsTypeArgs(typeArg, asList);

    // Create the new bounds.
    for (int i = 0; i < asList.size(); i++) {
      Variable ai = asList.get(i);
      ai.getBounds().addBound(null, VariableBounds.BoundKind.EQUAL, subsTypeArg.get(i));
    }

    boundSet.reachFixedPoint();
    return boundSet;
  }
}
