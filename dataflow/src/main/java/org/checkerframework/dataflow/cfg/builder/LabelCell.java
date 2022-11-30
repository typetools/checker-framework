package org.checkerframework.dataflow.cfg.builder;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.javacutil.BugInCF;

/** Storage cell for a single Label, with tracking whether it was accessed. */
class LabelCell {
  /** The label. If it is null, then it will be lazily set if {@link #accessLabel} is called. */
  private @MonotonicNonNull Label label;

  /** True if the label has been accessed. */
  private boolean accessed;

  /** Create a LabelCell with no label; the label will be lazily created if needed. */
  protected LabelCell() {
    this.accessed = false;
  }

  /**
   * Create a LabelCell with the given label.
   *
   * @param label the label
   */
  protected LabelCell(Label label) {
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
      throw new BugInCF("called peekLabel prematurely");
    }
    return label;
  }

  public boolean wasAccessed() {
    return accessed;
  }
}
