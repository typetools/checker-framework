package org.checkerframework.afu.annotator.tests;

@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.TYPE_USE})
@interface AType {}

@interface ADecl {}

public class ConstructorReturn {
  public ConstructorReturn() {}
}

class ConstructorReturn_2_1 {
  ConstructorReturn_2_1() {}

  Object foo() {
    return null;
  }
}

class ConstructorReturn_2_2 {
  ConstructorReturn_2_2() {}

  Object foo() {
    return null;
  }
}

class ConstructorReturn_3_1 {
  ConstructorReturn_3_1() {}

  Object foo() {
    return null;
  }
}

class ConstructorReturn_3_2 {
  ConstructorReturn_3_2() {}

  Object foo() {
    return null;
  }
}
