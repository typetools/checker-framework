import org.checkerframework.checker.igj.qual.*;

class Implicit<X> {}

class Explicit<X extends Object> {}

class DemoImplicit {
  Implicit<Object> ok1;
  Implicit<@Mutable Object> ok2;
  Implicit<@Immutable Object> ok3;
}

class DemoExplicit {
  Explicit<Object> ok1;
  Explicit<@Mutable Object> ok2;
  //:: error: (type.argument.type.incompatible)
  Explicit<@Immutable Object> err1;
}
