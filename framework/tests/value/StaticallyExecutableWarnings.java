import org.checkerframework.common.value.qual.*;
import org.checkerframework.dataflow.qual.Pure;

public class StaticallyExecutableWarnings {

  @StaticallyExecutable
  // :: warning: (statically.executable.not.pure)
  static int addNotPure(int a, int b) {
    return a + b;
  }

  @StaticallyExecutable
  @Pure
  static int add(Integer a, Integer b) {
    return a + b;
  }

  @StaticallyExecutable
  @Pure
  // :: error: (statically.executable.nonconstant.parameter.type)
  int receiverCannotBeConstant(int a, int b) {
    return a + b;
  }

  @StaticallyExecutable
  @Pure
  // :: error: (statically.executable.nonconstant.parameter.type)
  int explicitReceiverCannotBeConstant(StaticallyExecutableWarnings this, int a, int b) {
    return a + b;
  }

  @StaticallyExecutable
  @Pure
  // :: error: (statically.executable.nonconstant.return.type)
  static StaticallyExecutableWarnings returnTypeCannotBeConstant(int a, int b) {
    return new StaticallyExecutableWarnings();
  }

  @StaticallyExecutable
  @Pure
  // :: error: (statically.executable.nonconstant.parameter.type)
  static int parameterCannotBeConstant(int a, int b, Object o) {
    return a + b;
  }
}
