// Test case for issue #293: https://github.com/typetools/checker-framework/issues/293
// Thanks to Ed Price for the test case.

// Design space:
//  try
//    ..
//  catch: 7 varieties
//    * absent
//    * catch Throwable
//    * catch Exception
//    for each of the "present" types of catch:
//     * no action
//     * call method
//     * assign string
//  finally: 2 varieties
//    * absent
//    * no action
// Naming indicates which one; overall 14 tests.

// Empty, to give this file its name.
public class TryFinally {}

class TestCabsentFabsent {
    static String getFoo() {
        return "foo";
    }

    private final String foo;

    public TestCabsentFabsent() {
        this.foo = getFoo();
    }
}

class TestCabsentFnoaction {
    static String getFoo() {
        return "foo";
    }

    private final String foo;

    public TestCabsentFnoaction() {
        try {
            this.foo = getFoo();
        } finally {
            // no action in finally clause
        }
    }
}

// Not legal in Java: error: variable foo might not have been initialized
// class TestCtnoactionFabsent {
//   static String getFoo() { return "foo"; }
//   private final String foo;
//   public TestCtnoactionFabsent() {
//     try {
//       this.foo = getFoo();
//     } catch (Throwable t) {
//       // no action on exception
//     }
//   }
// }

// Not legal in Java: error: variable foo might not have been initialized
// class TestCtnoactionFnoaction {
//   static String getFoo() { return "foo"; }
//   private final String foo;
//   public TestCtnoactionFnoaction() {
//     try {
//       this.foo = getFoo();
//     } catch (Throwable t) {
//       // no action on exception
//     } finally {
//       // no action in finally clause
//     }
//   }
// }

// Not legal in Java: error: variable foo might already have been assigned
// class TestCtmethodFabsent {
//   static String getFoo() { return "foo"; }
//   private final String foo;
//   public TestCtmethodFabsent() {
//     try {
//       this.foo = getFoo();
//     } catch (Throwable t) {
//       this.foo = getFoo();
//     }
//   }
// }

// Not legal in Java: error: variable foo might already have been assigned
// class TestCtmethodFnoaction {
//   static String getFoo() { return "foo"; }
//   private final String foo;
//   public TestCtmethodFnoaction() {
//     try {
//       this.foo = getFoo();
//     } catch (Throwable t) {
//       this.foo = getFoo();
//     } finally {
//       // no action in finally clause
//     }
//   }
// }

// Not legal in Java: error: variable foo might already have been assigned
// class TestCtstringFabsent {
//   static String getFoo() { return "foo"; }
//   private final String foo;
//   public TestCtstringFabsent() {
//     try {
//       this.foo = getFoo();
//     } catch (Throwable t) {
//       this.foo = "foo";
//     }
//   }
// }

// Not legal in Java: error: variable foo might already have been assigned
// class TestCtstringFnoaction {
//   static String getFoo() { return "foo"; }
//   private final String foo;
//   public TestCtstringFnoaction() {
//     try {
//       this.foo = getFoo();
//     } catch (Throwable t) {
//       this.foo = "foo";
//     } finally {
//       // no action in finally clause
//     }
//   }
// }

// Not legal in Java: error: variable foo might not have been initialized
// class TestCenoactionFabsent {
//   static String getFoo() { return "foo"; }
//   private final String foo;
//   public TestCenoactionFabsent() {
//     try {
//       this.foo = getFoo();
//     } catch (Exception t) {
//       // no action on exception
//     }
//   }
// }

// Not legal in Java: error: variable foo might not have been initialized
// class TestCenoactionFnoaction {
//   static String getFoo() { return "foo"; }
//   private final String foo;
//   public TestCenoactionFnoaction() {
//     try {
//       this.foo = getFoo();
//     } catch (Exception t) {
//       // no action on exception
//     } finally {
//       // no action in finally clause
//     }
//   }
// }

// Not legal in Java: error: variable foo might already have been assigned
// class TestCemethodFabsent {
//   static String getFoo() { return "foo"; }
//   private final String foo;
//   public TestCemethodFabsent() {
//     try {
//       this.foo = getFoo();
//     } catch (Exception t) {
//       this.foo = getFoo();
//     }
//   }
// }

// Not legal in Java: error: variable foo might already have been assigned
// class TestCemethodFnoaction {
//   static String getFoo() { return "foo"; }
//   private final String foo;
//   public TestCemethodFnoaction() {
//     try {
//       this.foo = getFoo();
//     } catch (Exception t) {
//       this.foo = getFoo();
//     } finally {
//       // no action in finally clause
//     }
//   }
// }

// Not legal in Java: error: variable foo might already have been assigned
// class TestCestringFabsent {
//   static String getFoo() { return "foo"; }
//   private final String foo;
//   public TestCestringFabsent() {
//     try {
//       this.foo = getFoo();
//     } catch (Exception t) {
//       this.foo = "foo";
//     }
//   }
// }

// Not legal in Java: error: variable foo might already have been assigned
// class TestCestringFnoaction {
//   static String getFoo() { return "foo"; }
//   private final String foo;
//   public TestCestringFnoaction() {
//     try {
//       this.foo = getFoo();
//     } catch (Exception t) {
//       this.foo = "foo";
//     } finally {
//       // no action in finally clause
//     }
//   }
// }

class TestCabsentFabsentNonfinal {
    static String getFoo() {
        return "foo";
    }

    private String foo;

    public TestCabsentFabsentNonfinal() {
        this.foo = getFoo();
    }
}

class TestCabsentFnoactionNonfinal {
    static String getFoo() {
        return "foo";
    }

    private String foo;

    public TestCabsentFnoactionNonfinal() {
        try {
            this.foo = getFoo();
        } finally {
            // no action in finally clause
        }
    }
}

class TestCtnoactionFabsentNonfinal {
    static String getFoo() {
        return "foo";
    }

    private String foo;
    // :: error: initialization.fields.uninitialized
    public TestCtnoactionFabsentNonfinal() {
        try {
            this.foo = getFoo();
        } catch (Throwable t) {
            // no action on exception
        }
    }
}

class TestCtnoactionFnoactionNonfinal {
    static String getFoo() {
        return "foo";
    }

    private String foo;
    // :: error: initialization.fields.uninitialized
    public TestCtnoactionFnoactionNonfinal() {
        try {
            this.foo = getFoo();
        } catch (Throwable t) {
            // no action on exception
        } finally {
            // no action in finally clause
        }
    }
}

class TestCtmethodFabsentNonfinal {
    static String getFoo() {
        return "foo";
    }

    private String foo;

    public TestCtmethodFabsentNonfinal() {
        try {
            this.foo = getFoo();
        } catch (Throwable t) {
            this.foo = getFoo();
        }
    }
}

class TestCtmethodFnoactionNonfinal {
    static String getFoo() {
        return "foo";
    }

    private String foo;

    public TestCtmethodFnoactionNonfinal() {
        try {
            this.foo = getFoo();
        } catch (Throwable t) {
            this.foo = getFoo();
        } finally {
            // no action in finally clause
        }
    }
}

class TestCtstringFabsentNonfinal {
    static String getFoo() {
        return "foo";
    }

    private String foo;

    public TestCtstringFabsentNonfinal() {
        try {
            this.foo = getFoo();
        } catch (Throwable t) {
            this.foo = "foo";
        }
    }
}

class TestCtstringFnoactionNonfinal {
    static String getFoo() {
        return "foo";
    }

    private String foo;

    public TestCtstringFnoactionNonfinal() {
        try {
            this.foo = getFoo();
        } catch (Throwable t) {
            this.foo = "foo";
        } finally {
            // no action in finally clause
        }
    }
}

class TestCenoactionFabsentNonfinal {
    static String getFoo() {
        return "foo";
    }

    private String foo;
    // :: error: initialization.fields.uninitialized
    public TestCenoactionFabsentNonfinal() {
        try {
            this.foo = getFoo();
        } catch (Exception t) {
            // no action on exception
        }
    }
}

class TestCenoactionFnoactionNonfinal {
    static String getFoo() {
        return "foo";
    }

    private String foo;
    // :: error: initialization.fields.uninitialized
    public TestCenoactionFnoactionNonfinal() {
        try {
            this.foo = getFoo();
        } catch (Exception t) {
            // no action on exception
        } finally {
            // no action in finally clause
        }
    }
}

class TestCemethodFabsentNonfinal {
    static String getFoo() {
        return "foo";
    }

    private String foo;

    public TestCemethodFabsentNonfinal() {
        try {
            this.foo = getFoo();
        } catch (Exception t) {
            this.foo = getFoo();
        }
    }
}

class TestCemethodFnoactionNonfinal {
    static String getFoo() {
        return "foo";
    }

    private String foo;

    public TestCemethodFnoactionNonfinal() {
        try {
            this.foo = getFoo();
        } catch (Exception t) {
            this.foo = getFoo();
        } finally {
            // no action in finally clause
        }
    }
}

class TestCestringFabsentNonfinal {
    static String getFoo() {
        return "foo";
    }

    private String foo;

    public TestCestringFabsentNonfinal() {
        try {
            this.foo = getFoo();
        } catch (Exception t) {
            this.foo = "foo";
        }
    }
}

class TestCestringFnoactionNonfinal {
    static String getFoo() {
        return "foo";
    }

    private String foo;

    public TestCestringFnoactionNonfinal() {
        try {
            this.foo = getFoo();
        } catch (Exception t) {
            this.foo = "foo";
        } finally {
            // no action in finally clause
        }
    }
}
