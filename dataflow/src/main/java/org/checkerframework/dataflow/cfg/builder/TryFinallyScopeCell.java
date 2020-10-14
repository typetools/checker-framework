package org.checkerframework.dataflow.cfg.builder;

/** Storage cell for a single Label, with tracking whether it was accessed. */
class TryFinallyScopeCell {
    private Label label;
    private boolean accessed;

    protected TryFinallyScopeCell() {
        this.label = null;
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
        assert label != null;
        return label;
    }

    public boolean wasAccessed() {
        return accessed;
    }
}
