// Test case for Issue 1797:
// https://github.com/typetools/checker-framework/issues/1797

import org.checkerframework.checker.nullness.qual.Nullable;

class Issue1797 {
    void fooReturn(@Nullable Object o) {
        try {
            return;
        } finally {
            if (o != null) {
                o.toString();
            }
        }
    }

    void fooReturnNested(@Nullable Object o) {
        while (this.hashCode() < 10) {
            while (this.hashCode() < 5) {
                try {
                    return;
                } finally {
                    if (o != null) {
                        o.toString();
                    }
                    // TODO: should go to exit, not either loop!
                }
            }
        }
    }

    void fooBreak(@Nullable Object o) {
        while (this.hashCode() < 5) {
            try {
                break;
            } finally {
                if (o != null) {
                    o.toString();
                }
            }
        }
    }

    void fooBreakLabel(@Nullable Object o) {
        outer:
        while (this.hashCode() < 10) {
            while (this.hashCode() < 5) {
                try {
                    break outer;
                } finally {
                    if (o != null) {
                        o.toString();
                    }
                    // TODO: this should continue after outer
                }
            }
        }
    }

    void fooBreakLabel2(@Nullable Object o) {
        outer:
        while (this.hashCode() < 10) {
            try {
                inner:
                while (this.hashCode() < 5) {
                    if (this.hashCode() < 2) {
                        break outer;
                        // TODO: should go to finally
                    } else {
                        break inner;
                        // TODO: should not go to finally
                    }
                }
            } finally {
                if (o != null) {
                    o.toString();
                }
                // TODO: this should continue either at outer or after outer
            }
        }
    }

    void fooBreakNoLabel(@Nullable Object o) {
        outer:
        while (this.hashCode() < 10) {
            inner:
            while (this.hashCode() < 5) {
                try {
                    break;
                } finally {
                    if (o != null) {
                        o.toString();
                    }
                    // TODO: this should continue at outer
                }
            }
        }
    }

    void fooContinue(@Nullable Object o) {
        while (this.hashCode() < 5) {
            try {
                continue;
            } finally {
                if (o != null) {
                    o.toString();
                }
            }
        }
    }

    void fooContinueLabel(@Nullable Object o) {
        outer:
        while (this.hashCode() < 10) {
            while (this.hashCode() < 5) {
                try {
                    continue outer;
                } finally {
                    if (o != null) {
                        o.toString();
                    }
                }
            }
        }
    }

    void fooSwitch(@Nullable Object o) {
        switch (this.hashCode()) {
            case 1:
                try {
                    break;
                } finally {
                    if (o != null) {
                        o.toString();
                    }
                }
            default:
        }
    }

    // A few tests to make sure also return with expression works.

    int barReturn(@Nullable Object o) {
        try {
            return 5;
        } finally {
            if (o != null) {
                o.toString();
            }
        }
    }

    int barReturnInFinally(@Nullable Object o) {
        try {
            return 5;
        } finally {
            if (o != null) {
                o.toString();
            }
            return 10;
        }
    }

    int barReturnNested(@Nullable Object o) {
        while (this.hashCode() < 10) {
            while (this.hashCode() < 5) {
                try {
                    return 5;
                } finally {
                    if (o != null) {
                        o.toString();
                    }
                    // TODO: should go to return 5, not either loop!
                }
            }
        }
        return 10;
    }
}
