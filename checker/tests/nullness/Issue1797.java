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

    void fooWhileReturn(@Nullable Object o) {
        while (this.hashCode() < 5) {
            try {
                return;
            } finally {
                if (o != null) {
                    o.toString();
                }
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
                    // go to exit, not either loop
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
                    // continue after outer
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
                        // go to finally
                    } else {
                        break inner;
                        // do not go to finally
                    }
                }
            } finally {
                if (o != null) {
                    o.toString();
                }
                // continue either at outer or after outer
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
                    // continue at outer
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
                    // goes to return 5, not either loop!
                }
            }
        }
        return 10;
    }

    @FunctionalInterface
    interface NullableParamFunction {
        String takeVal(@Nullable Object x);
    }

    void testLambda() {
        NullableParamFunction n1 = (@Nullable Object x) -> (x == null) ? "null" : x.toString();
        try {
            NullableParamFunction n2 = (@Nullable Object x) -> (x == null) ? "null" : x.toString();
        } finally {
            NullableParamFunction n3 = (x) -> (x == null) ? "null" : x.toString();
        }
    }

    boolean nestedCFGConstructionTest(@Nullable Object o) {
        boolean result = true;
        java.io.BufferedWriter out = null;
        try {
            try {
            } finally {
                out = new java.io.BufferedWriter(new java.io.OutputStreamWriter(System.err));
            }
            if (o != null) {
                o.toString();
                out.write(' ');
            }
        } catch (Exception e) {
        } finally {
        }
        return result;
    }

    void nestedTryFinally() {
        try {
            try {
            } finally {
            }
        } finally {
        }
    }

    boolean nestedCFGConstructionTest2() throws java.io.IOException {
        java.io.BufferedWriter out =
                new java.io.BufferedWriter(new java.io.OutputStreamWriter(System.err));
        try {
            try {
                return true;
            } finally {
            }
        } finally {
            out.write(' ');
            out.close();
        }
    }

    void nestedTryFinally2(java.io.BufferedWriter out) throws java.io.IOException {
        try {
            try {
                return;
            } finally {
            }
        } finally {
            out.write(' ');
            out.close();
        }
    }
}
