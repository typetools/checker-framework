package org.checkerframework.afu.scenelib.el;

import java.util.Map;
import org.checkerframework.afu.scenelib.util.coll.VivifyingMap;

/**
 * ABlock has local variables in scope. We currently directly use them only for static initializer
 * blocks, which are not methods, but can declare local variables.
 */
public class ABlock extends AExpression {
  // Currently we don't validate the local locations (e.g., that no two
  // distinct ranges for the same index overlap).
  /** The method's annotated local variables; map key contains local variable location numbers. */
  public final VivifyingMap<LocalLocation, AField> locals =
      AField.<LocalLocation>newVivifyingLHMap_AF();

  /**
   * Creates a new ABlock.
   *
   * @param id the identifier
   */
  ABlock(String id) {
    super(id);
  }

  ABlock(ABlock block) {
    super(block);
    copyMapContents(block.locals, locals);
  }

  @Override
  public ABlock clone() {
    return new ABlock(this);
  }

  @Override
  public boolean equals(AElement o) {
    return o instanceof ABlock && ((ABlock) o).equalsBlock(this);
  }

  protected boolean equalsBlock(ABlock o) {
    return super.equalsExpression(o) && o.locals.equals(locals);
  }

  @Override
  public int hashCode() {
    return super.hashCode() + locals.hashCode();
  }

  @Override
  public boolean isEmpty() {
    return super.isEmpty() && locals.isEmpty();
  }

  @Override
  public void prune() {
    super.prune();
    locals.prune();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    // sb.append("ABlock ");
    // sb.append(id);
    for (Map.Entry<LocalLocation, AField> em : locals.entrySet()) {
      LocalLocation loc = em.getKey();
      sb.append(loc);
      sb.append(": ");
      AElement ae = em.getValue();
      sb.append(ae.toString());
      sb.append(' ');
    }
    sb.append(super.toString());
    return sb.toString();
  }

  @Override
  public <R, T> R accept(ElementVisitor<R, T> v, T t) {
    return v.visitBlock(this, t);
  }
}
