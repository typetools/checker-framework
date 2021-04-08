package org.checkerframework.dataflow.cfg.builder;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/** Storage cell for a single Label, with tracking whether it was accessed. */
class TryFinallyScopeCell {
  private @MonotonicNonNull Label label;
  private boolean accessed;

  protected TryFinallyScopeCell() {
    this.accessed = false;
  }

  protected TryFinallyScopeCell(Label label) {
    assert label != null;
    this.label = label;
    this.accessed = false;
  }

  public Label accessLabel() {
    if (label == null) {
      label = new Label();
    }
    accessed = true;
    return label;
  }

  public Label peekLabel() {
    if (label == null) {
      throw new Error("called peekLabel prematurely");
    }
    return label;
  }

  public boolean wasAccessed() {
    return accessed;
  }
}
