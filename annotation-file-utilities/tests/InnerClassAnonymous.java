package org.checkerframework.afu.annotator.tests;

import java.io.Serializable;

public class InnerClassAnonymous {
  public Object field;

  public class NamedInnerClass {
    public Object namedField;
  }

  public Serializable foo() {
    return new Serializable() {
      public final Object serialVersionUID = null;
    };
  }

  public Serializable bar() {
    return new Serializable() {
      private static final long serialVersionUID = 0;
    };
  }

  public Serializable baz() {
    return new Serializable() {
      private static final long serialVersionUID = 0;
    };
  }
}
