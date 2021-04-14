package org.checkerframework.dataflow.constantpropagation;

import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.IntegerLiteralNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.javacutil.SystemUtil;

/** A store that records information about constant values. */
public class ConstantPropagationStore implements Store<ConstantPropagationStore> {

  /** Information about variables gathered so far. */
  Map<Node, Constant> contents;

  /** Creates a new ConstantPropagationStore. */
  public ConstantPropagationStore() {
    contents = new LinkedHashMap<>();
  }

  protected ConstantPropagationStore(Map<Node, Constant> contents) {
    this.contents = contents;
  }

  public Constant getInformation(Node n) {
    if (contents.containsKey(n)) {
      return contents.get(n);
    }
    return new Constant(Constant.Type.TOP);
  }

  public void mergeInformation(Node n, Constant val) {
    Constant value;
    if (contents.containsKey(n)) {
      value = val.leastUpperBound(contents.get(n));
    } else {
      value = val;
    }
    // TODO: remove (only two nodes supported atm)
    assert n instanceof IntegerLiteralNode || n instanceof LocalVariableNode;
    contents.put(n, value);
  }

  public void setInformation(Node n, Constant val) {
    // TODO: remove (only two nodes supported atm)
    assert n instanceof IntegerLiteralNode || n instanceof LocalVariableNode;
    contents.put(n, val);
  }

  @Override
  public ConstantPropagationStore copy() {
    return new ConstantPropagationStore(new LinkedHashMap<>(contents));
  }

  @Override
  public ConstantPropagationStore leastUpperBound(ConstantPropagationStore other) {
    Map<Node, Constant> newContents = new LinkedHashMap<>(contents.size() + other.contents.size());

    // go through all of the information of the other class
    for (Map.Entry<Node, Constant> e : other.contents.entrySet()) {
      Node n = e.getKey();
      Constant otherVal = e.getValue();
      if (contents.containsKey(n)) {
        // merge if both contain information about a variable
        newContents.put(n, otherVal.leastUpperBound(contents.get(n)));
      } else {
        // add new information
        newContents.put(n, otherVal);
      }
    }

    for (Map.Entry<Node, Constant> e : contents.entrySet()) {
      Node n = e.getKey();
      Constant thisVal = e.getValue();
      if (!other.contents.containsKey(n)) {
        // add new information
        newContents.put(n, thisVal);
      }
    }

    return new ConstantPropagationStore(newContents);
  }

  @Override
  public ConstantPropagationStore widenedUpperBound(ConstantPropagationStore previous) {
    return leastUpperBound(previous);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof ConstantPropagationStore)) {
      return false;
    }
    ConstantPropagationStore other = (ConstantPropagationStore) o;
    // go through all of the information of the other object
    for (Map.Entry<Node, Constant> e : other.contents.entrySet()) {
      Node n = e.getKey();
      Constant otherVal = e.getValue();
      if (otherVal.isBottom()) {
        continue; // no information
      }
      if (contents.containsKey(n)) {
        if (!otherVal.equals(contents.get(n))) {
          return false;
        }
      } else {
        return false;
      }
    }
    // go through all of the information of the this object
    for (Map.Entry<Node, Constant> e : contents.entrySet()) {
      Node n = e.getKey();
      Constant thisVal = e.getValue();
      if (thisVal.isBottom()) {
        continue; // no information
      }
      if (other.contents.containsKey(n)) {
        continue;
      } else {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int s = 0;
    for (Map.Entry<Node, Constant> e : contents.entrySet()) {
      if (!e.getValue().isBottom()) {
        s += e.hashCode();
      }
    }
    return s;
  }

  @Override
  public String toString() {
    // only output local variable information
    Map<Node, Constant> contentsWithoutLocalVars =
        new LinkedHashMap<>(SystemUtil.mapCapacity(contents));
    for (Map.Entry<Node, Constant> e : contents.entrySet()) {
      if (e.getKey() instanceof LocalVariableNode) {
        contentsWithoutLocalVars.put(e.getKey(), e.getValue());
      }
    }
    return contentsWithoutLocalVars.toString();
  }

  @Override
  public boolean canAlias(JavaExpression a, JavaExpression b) {
    return true;
  }

  /**
   * {@inheritDoc}
   *
   * <p>{@code value} is {@code null} because {@link ConstantPropagationStore} doesn't support
   * visualization.
   */
  @Override
  @SuppressWarnings("nullness")
  public String visualize(CFGVisualizer<?, ConstantPropagationStore, ?> viz) {
    return viz.visualizeStoreKeyVal("constant propagation", null);
  }
}
