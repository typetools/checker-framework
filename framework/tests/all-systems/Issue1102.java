// Test case for Issue 1102:
// https://github.com/typetools/checker-framework/issues/1102
// Additional test in checker/tests/nullness/Issue1102.java

interface Issue1102Itf {}

class Issue1102Base {}

class Issue1102Decl extends Issue1102Base {
  static <S extends Object, T extends Issue1102Base & Issue1102Itf> Issue1102Decl newInstance(T s) {
    return new Issue1102Decl();
  }
}

@SuppressWarnings("all") // Only interested in possible crash
class Issue1102Use<U extends Issue1102Base & Issue1102Itf> {
  U f;

  void bar() {
    Issue1102Decl d = Issue1102Decl.newInstance(f);
  }
}
