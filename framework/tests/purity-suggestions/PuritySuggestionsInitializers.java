// A constructor is suggested to be side-effect-free only if the instance initializers that run as
// part of it are also side-effect-free.

public class PuritySuggestionsInitializers {

  static int counter;

  static class ImpureFieldInitializer {
    int f = counter++;

    ImpureFieldInitializer() {}
  }

  static class ImpureInitializerBlock {
    {
      counter++;
    }

    ImpureInitializerBlock() {}
  }

  static class PureInitializers {
    int f = 1;

    {
      f = 2;
    }

    // :: warning: [purity.more.sideeffectfree]
    PureInitializers() {}
  }

  static class Delegating {
    int f = counter++;

    Delegating() {}

    // The initializers run as part of `Delegating()`, which is not side-effect-free.
    Delegating(int x) {
      this();
    }
  }
}
