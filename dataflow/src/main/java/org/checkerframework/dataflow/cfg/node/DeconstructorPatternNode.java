package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

/** A node for a deconstrutor pattern. */
public class DeconstructorPatternNode extends Node {
  /**
   * The {@code DeconstructorPatternTree}, declared as {@link Tree} to permit this file to compile
   * under JDK 20 and earlier.
   */
  protected final Tree deconstructorPattern;

  /** A list of nested pattern nodes. */
  protected final List<Node> nestedPatterns;

  /**
   * Creates a {@code DeconstructorPatternNode}.
   *
   * @param type the type of the node
   * @param deconstructorPattern the {@code DeconstructorPatternTree}
   * @param nestedPatterns a list of nested pattern nodes
   */
  public DeconstructorPatternNode(
      TypeMirror type, Tree deconstructorPattern, List<Node> nestedPatterns) {
    super(type);
    this.deconstructorPattern = deconstructorPattern;
    this.nestedPatterns = nestedPatterns;
  }

  @Override
  @Pure
  public @Nullable Tree getTree() {
    return deconstructorPattern;
  }

  /**
   * Returns the nested patterns.
   *
   * @return the nested patterns
   */
  @Pure
  public List<Node> getNestedPatterns() {
    return nestedPatterns;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitDeconstructorPattern(this, p);
  }

  @Override
  @Pure
  public Collection<Node> getOperands() {
    return nestedPatterns;
  }

  /**
   * A list of nested binding variables. This is lazily initialized and should only be accessed by
   * {@link #getBindingVariables()}.
   */
  protected @MonotonicNonNull List<LocalVariableNode> bindingVariables = null;

  /**
   * Return all the binding variables in this pattern.
   *
   * @return all the binding variables in this pattern
   */
  public List<LocalVariableNode> getBindingVariables() {
    if (bindingVariables == null) {
      if (nestedPatterns.isEmpty()) {
        bindingVariables = Collections.emptyList();
      } else {
        bindingVariables = new ArrayList<>(nestedPatterns.size());
        for (Node patternNode : nestedPatterns) {
          if (patternNode instanceof LocalVariableNode) {
            bindingVariables.add((LocalVariableNode) patternNode);
          } else if (patternNode instanceof AnyPatternNode) {
            // Do nothing, as AnyPatternNode does not have binding variables.
          } else {
            bindingVariables.addAll(((DeconstructorPatternNode) patternNode).getBindingVariables());
          }
        }
        bindingVariables = Collections.unmodifiableList(bindingVariables);
      }
    }
    return bindingVariables;
  }

  @Override
  public String toString() {
    return deconstructorPattern.toString();
  }
}
