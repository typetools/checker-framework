// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that a case does not refine a selector expression that a guard of an earlier case, or a
// record accessor that a record pattern invokes, may have changed.  The case labels are tested
// against the value that the selector had before the switch.

import org.checkerframework.checker.nullness.qual.Nullable;

public class SwitchSelectorChangedByGuard {

  sealed interface Shape permits Circle, Square {}

  record Circle() implements Shape {}

  record Square() implements Shape {}

  @Nullable Shape shape;

  @Nullable Object g;

  boolean clearShape() {
    this.shape = null;
    return false;
  }

  boolean clearG() {
    g = null;
    return false;
  }

  void guardChangesField() {
    this.shape = new Circle();
    switch (this.shape) {
      case Circle c when clearShape() -> {}
      case Shape t -> {
        // :: error: [dereference.of.nullable]
        this.shape.toString();
      }
    }
  }

  void guardChangesFieldLastCase() {
    this.shape = new Circle();
    switch (this.shape) {
      case Circle c when clearShape() -> {}
      case Circle c -> {}
      case Square q -> {
        // :: error: [dereference.of.nullable]
        this.shape.toString();
      }
    }
  }

  int guardChangesFieldLaterCases() {
    if (shape == null) {
      return 0;
    }
    return switch (shape) {
      case Circle c when clearShape() -> 1;
      // :: error: [dereference.of.nullable]
      case Square s -> shape.hashCode();
      // :: error: [dereference.of.nullable]
      case Circle c -> shape.hashCode();
    };
  }

  // Without a guard, the selector field is refined.
  void noGuard() {
    this.shape = new Circle();
    switch (this.shape) {
      case Circle c -> {}
      case Shape t -> {
        this.shape.toString();
      }
    }
  }

  sealed interface Node permits Leaf, Pair {}

  record Leaf() implements Node {}

  record Pair(Node left) implements Node {
    // A record pattern invokes this accessor, even if the pattern does not match.
    @Override
    public Node left() {
      staticNode = null;
      return left;
    }
  }

  static @Nullable Node staticNode;

  // A record accessor that an earlier case's record pattern invokes may change the selector.
  String accessorChangesFieldLaterCase() {
    return switch (staticNode) {
      case null -> "";
      case Pair(Leaf l) -> "";
      // :: error: [dereference.of.nullable]
      case Node n -> staticNode.toString();
    };
  }

  // A record accessor that a case's own record pattern invokes may change the selector.
  String accessorChangesFieldSameCase() {
    return switch (staticNode) {
      case null -> "";
      // :: error: [dereference.of.nullable]
      case Pair(Node l) -> staticNode.toString();
      case Leaf l -> "";
    };
  }

  // Without a record pattern, the selector field is refined.
  String noRecordPattern() {
    return switch (staticNode) {
      case null -> "";
      case Leaf l -> "";
      case Node n -> staticNode.toString();
    };
  }

  sealed interface Tree permits Branch, Tip {}

  // The accessor is auto-generated, so it is side-effect-free.
  record Branch(Tree child) implements Tree {}

  record Tip() implements Tree {}

  static @Nullable Tree staticTree;

  // A side-effect-free record accessor does not change the selector.
  String pureAccessor() {
    return switch (staticTree) {
      case null -> "";
      case Branch(Tip t) -> staticTree.toString();
      case Tree t -> staticTree.toString();
    };
  }

  record Inner() {}

  record Outer(@Nullable Inner inner) {
    // A record pattern invokes this accessor, even if the pattern does not match.
    @Override
    public @Nullable Inner inner() {
      staticOuter = null;
      return inner;
    }
  }

  static @Nullable Outer staticOuter;

  // The sole case is exhaustive, but Outer(null) is a remainder that it does not match, so the
  // switch throws MatchException after the accessor has changed the selector.
  void accessorChangesFieldBeforeMatchException() {
    staticOuter = new Outer(null);
    try {
      switch (staticOuter) {
        case Outer(Inner()) -> {}
      }
    } catch (RuntimeException e) {
      // :: error: [dereference.of.nullable]
      staticOuter.hashCode();
    }
  }

  // A guard cannot change a local variable that is the selector expression.
  String localSelectorAfterGuard(@Nullable Shape s) {
    return switch (s) {
      case null -> "";
      case Circle c when clearG() -> "";
      case Circle c -> s.toString();
      case Square q -> s.toString();
    };
  }
}
