package org.checkerframework.framework.util.typeinference8.types;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A data structure to hold the dependencies between variables. Dependencies are defined in <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.4">JLS section
 * 18.4</a> and impact the order in which variables are resolved.
 */
public class Dependencies {

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
          betas.addAll(map.get(gamma));
        }
        changed |= gammas.addAll(betas);
      }
    }
  }

  /**
   * Returns the set of dependencies of {@code alpha}.
   *
   * @param alpha a variable
   * @return the set of dependencies of {@code alpha}
   */
  public Set<Variable> get(Variable alpha) {
    return new LinkedHashSet<>(map.get(alpha));
  }

  /**
   * Returns the set of dependencies for all variables in {@code variables}.
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
