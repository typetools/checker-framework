package org.checkerframework.framework.util.typeinference8.types;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A data structure to hold the dependencies between variables. Dependencies are defined in <a
 * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-18.html#jls-18.4">JLS section
 * 18.4</a> and impact the order in which variables are resolved.
 */
public class Dependencies {

  /** Creates Dependencies. */
  public Dependencies() {}

  /** A map from a variable to the variables, including itself, on which it depends. */
  private final Map<Variable, LinkedHashSet<Variable>> map = new LinkedHashMap<>();

  /**
   * Add {@code value} as a dependency of {@code key}.
   *
   * @param key a key to add
   * @param value a value to add
   */
  public void putOrAdd(Variable key, Variable value) {
    LinkedHashSet<Variable> set = map.computeIfAbsent(key, k -> new LinkedHashSet<>());
    set.add(value);
  }

  /**
   * Add {@code values} as dependencies of {@code key}.
   *
   * @param key a key to add
   * @param values values to add
   */
  public void putOrAddAll(Variable key, Collection<? extends Variable> values) {
    LinkedHashSet<Variable> set = map.computeIfAbsent(key, k -> new LinkedHashSet<>());
    set.addAll(values);
  }

  /**
   * Calculate and add transitive dependencies.
   *
   * <p>JLS 18.4 "An inference variable alpha depends on the resolution of an inference variable
   * beta if there exists an inference variable gamma such that alpha depends on the resolution of
   * gamma and gamma depends on the resolution of beta."
   */
  public void calculateTransitiveDependencies() {
    boolean changed = true;
    while (changed) {
      changed = false;
      for (Map.Entry<Variable, LinkedHashSet<Variable>> entry : map.entrySet()) {
        Variable alpha = entry.getKey();
        LinkedHashSet<Variable> gammas = entry.getValue();
        LinkedHashSet<Variable> betas = new LinkedHashSet<>();
        for (Variable gamma : gammas) {
          if (gamma == alpha) {
            continue;
          }
          LinkedHashSet<Variable> gammaDependencies = map.get(gamma);
          if (gammaDependencies != null) {
            betas.addAll(gammaDependencies);
          }
        }
        changed |= gammas.addAll(betas);
      }
    }
  }

  /**
   * Returns the set of dependencies of {@code alpha}, always including {@code alpha} itself,
   * because JLS 18.4 says that an inference variable depends on the resolution of itself.
   *
   * <p>If no dependency of {@code alpha} has been added to this object, the result is {@code
   * {alpha}}. That case arises only for a variable that is absent from the bound set that created
   * this object, because {@code BoundSet.getDependencies} records every variable of the bound set
   * as a dependency of itself. For such a variable, {@code {alpha}} is a lower bound on its true
   * dependencies: any dependency arising from its bounds is unknown to this object, so a caller may
   * treat {@code alpha} as resolvable earlier or more independently than JLS 18.4 permits.
   * Computing more than {@code {alpha}} here is not possible, because the transitive closure that
   * {@code BoundSet.getDependencies} computes needs the whole bound set. {@code
   * Resolution.getSmallestDependencySet} and {@code ConstraintSet.getClosedSubset} both query
   * variables that the bound set does not necessarily contain, and letting such a variable depend
   * on only itself lets inference proceed rather than crashing.
   *
   * <p>The result is a new, mutable set; the caller may modify it.
   *
   * @param alpha a variable
   * @return the set of dependencies of {@code alpha}
   */
  public Set<Variable> get(Variable alpha) {
    LinkedHashSet<Variable> alphaDependencies = map.get(alpha);
    LinkedHashSet<Variable> result =
        alphaDependencies == null ? new LinkedHashSet<>() : new LinkedHashSet<>(alphaDependencies);
    // JLS 18.4: "An inference variable alpha depends on the resolution of itself."
    result.add(alpha);
    return result;
  }

  /**
   * Returns the set of dependencies for all variables in {@code variables}. Every variable in
   * {@code variables} is in the result; a variable to which no dependency has been added
   * contributes only itself. See {@link #get(Variable)}.
   *
   * <p>The result is a new, mutable set; the caller may modify it.
   *
   * @param variables list of variables
   * @return the set of dependencies for all variables in {@code variables}
   */
  public Set<Variable> get(List<Variable> variables) {
    LinkedHashSet<Variable> set = new LinkedHashSet<>();
    for (Variable v : variables) {
      Set<Variable> get = get(v);
      set.addAll(get);
    }
    return set;
  }
}
