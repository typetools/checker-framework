package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A node for a deconstrutor pattern. */
public class DeconstructorPatternNode extends Node {
  /** The {@code DeconstructorPatternTree}. */
  private Tree deconstructorPattern;

  /** A list of nested pattern nodes. */
  private List<Node> nestedPatterns;

  /**
   * Creates a {@code DeconstructorPatternNode}
   *
   * @param type the type of the node
   * @param deconstructorPattern {@code DeconstructorPatternTree}
   * @param nestedPatterns a list of nested pattern nodes
   */
  public DeconstructorPatternNode(
      TypeMirror type, Tree deconstructorPattern, List<Node> nestedPatterns) {
    super(type);
    this.deconstructorPattern = deconstructorPattern;
    this.nestedPatterns = nestedPatterns;
  }

  @Override
  public @Nullable Tree getTree() {
    return deconstructorPattern;
  }

  /**
   * Returns the list of nested patterns.
   *
   * @return the list of nested patterns
   */
  public List<Node> getNestedPatterns() {
    return nestedPatterns;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitDeconstructorPattern(this, p);
  }

  @Override
  public Collection<Node> getOperands() {
    return nestedPatterns;
  }

  /**
   * A list of nested binding variables. This is lazily initialized and should only be accessed by
   * {@link #getBindingVariables()}.
   */
  private List<LocalVariableNode> bindingVariables = null;

  /**
   * Return a list of all the binding variables in this pattern.
   *
   * @return a list of all the binding variables
   */
  public List<LocalVariableNode> getBindingVariables() {
    if (bindingVariables == null && nestedPatterns.isEmpty()) {
      bindingVariables = Collections.emptyList();
      return bindingVariables;
    }
    if (bindingVariables == null) {
      List<LocalVariableNode> bindingVars = new ArrayList<>(nestedPatterns.size());
      for (Node patternNode : nestedPatterns) {
        if (patternNode instanceof LocalVariableNode) {
          bindingVars.add((LocalVariableNode) patternNode);
        } else {
          bindingVars.addAll(((DeconstructorPatternNode) patternNode).getBindingVariables());
        }
      }
      this.bindingVariables = bindingVars;
    }
    return bindingVariables;
  }
}
