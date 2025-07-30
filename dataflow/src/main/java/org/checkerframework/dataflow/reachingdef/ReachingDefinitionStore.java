package org.checkerframework.dataflow.reachingdef;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.javacutil.BugInCF;

/**
 * A reaching definition store contains a set of reaching definitions represented by
 * ReachingDefinitionNode
 */
public class ReachingDefinitionStore implements Store<ReachingDefinitionStore> {

  /** The set of reaching definitions in this store. */
  private final Set<ReachingDefinitionNode> reachingDefSet;

  /** Create a new ReachDefinitionStore. */
  public ReachingDefinitionStore() {
    reachingDefSet = new LinkedHashSet<>();
  }

  /**
   * Create a new ReachDefinitionStore.
   *
   * @param reachingDefSet a set of reaching definition nodes. The parameter is captured and the
   *     caller should not retain an alias.
   */
  public ReachingDefinitionStore(Set<ReachingDefinitionNode> reachingDefSet) {
    this.reachingDefSet = reachingDefSet;
  }

  /**
   * Remove the information of a reaching definition from the reaching definition set.
   *
   * @param defTarget target of a reaching definition
   */
  public void killDef(Node defTarget) {
    Iterator<ReachingDefinitionNode> it = reachingDefSet.iterator();
    while (it.hasNext()) {
      // We use `.equals` instead of `==` here to compare value equality
      // rather than reference equality, because if two left-hand side node
      // have same values, we need to kill the old one and replace with the
      // new one.
      ReachingDefinitionNode generatedDefNode = it.next();
      if (generatedDefNode.def.getTarget().equals(defTarget)) {
        it.remove();
      }
    }
  }

  /**
   * Add a reaching definition to the reaching definition set.
   *
   * @param def a reaching definition
   */
  public void putDef(ReachingDefinitionNode def) {
    reachingDefSet.add(def);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof ReachingDefinitionStore)) {
      return false;
    }
    ReachingDefinitionStore other = (ReachingDefinitionStore) obj;
    return other.reachingDefSet.equals(this.reachingDefSet);
  }

  @Override
  public int hashCode() {
    return this.reachingDefSet.hashCode();
  }

  @Override
  public ReachingDefinitionStore copy() {
    return new ReachingDefinitionStore(new LinkedHashSet<>(reachingDefSet));
  }

  @Override
  public ReachingDefinitionStore leastUpperBound(ReachingDefinitionStore other) {
    LinkedHashSet<ReachingDefinitionNode> reachingDefSetLub =
        new LinkedHashSet<>(this.reachingDefSet.size() + other.reachingDefSet.size());
    reachingDefSetLub.addAll(this.reachingDefSet);
    reachingDefSetLub.addAll(other.reachingDefSet);
    return new ReachingDefinitionStore(reachingDefSetLub);
  }

  @Override
  public ReachingDefinitionStore widenedUpperBound(ReachingDefinitionStore previous) {
    throw new BugInCF("ReachingDefinitionStore.widenedUpperBound was called!");
  }

  @Override
  public boolean canAlias(JavaExpression a, JavaExpression b) {
    return true;
  }

  @Override
  public String visualize(CFGVisualizer<?, ReachingDefinitionStore, ?> viz) {
    String key = "reaching definitions";
    if (reachingDefSet.isEmpty()) {
      return viz.visualizeStoreKeyVal(key, "none");
    }
    StringJoiner sjStoreVal = new StringJoiner(", ", "{ ", " }");
    for (ReachingDefinitionNode reachDefNode : reachingDefSet) {
      sjStoreVal.add(reachDefNode.toString());
    }
    return viz.visualizeStoreKeyVal(key, sjStoreVal.toString());
  }

  @Override
  public String toString() {
    return "ReachingDefinitionStore: " + reachingDefSet.toString();
  }
}
