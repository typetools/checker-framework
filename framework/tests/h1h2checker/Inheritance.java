import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

// :: warning: (inconsistent.constructor.type) :: error: (super.invocation)
@H1S1 class Inheritance {
  void bar1(@H1Bot Inheritance param) {}

  void bar2(@H1S1 Inheritance param) {}
  // :: error: (annotations.on.use)
  void bar3(@H1Top Inheritance param) {}

  void foo1(@H1Bot Inheritance[] param) {}

  void foo2(@H1S1 Inheritance[] param) {}
  // :: error: (annotations.on.use)
  void foo3(@H1Top Inheritance[] param) {}
}
