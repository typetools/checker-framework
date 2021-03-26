package org.checkerframework.dataflow.cfg.builder;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.lang.model.element.Name;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A map that keeps track of new labels added within a try block. For names that are outside of the
 * try block, the finally label is returned. This ensures that a finally block is executed when
 * control flows outside of the try block.
 */
@SuppressWarnings("serial")
class TryFinallyScopeMap extends HashMap<Name, Label> {
  /** New labels within a try block that were added by this implementation. */
  private final Map<Name, Label> accessedNames;

  /** Create a new TryFinallyScopeMap. */
  protected TryFinallyScopeMap() {
    this.accessedNames = new LinkedHashMap<>();
  }

  @Override
  public Label get(@Nullable Object key) {
    if (key == null) {
      throw new IllegalArgumentException();
    }
    if (super.containsKey(key)) {
      return super.get(key);
    } else {
      if (accessedNames.containsKey(key)) {
        return accessedNames.get(key);
      }
      Label l = new Label();
      accessedNames.put((Name) key, l);
      return l;
    }
  }

  @Override
  @SuppressWarnings(
      "keyfor:contracts.conditional.postcondition.not.satisfied") // get adds everything
  public boolean containsKey(@Nullable Object key) {
    return true;
  }

  public Map<Name, Label> getAccessedNames() {
    return accessedNames;
  }
}
