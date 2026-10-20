// Test case for https://github.com/typetools/checker-framework/issues/5174 .
// Class implicitthis.ImplicitThis checks the type of each implicit receiver ("this") in the
// methods of this file.

class Issue5174Super<S> {
  S f;

  S methodInner(S in) {
    return in;
  }
}

class Issue5174Sub<T> extends Issue5174Super<T> {
  T g;

  // Every implicit receiver has type Issue5174Sub.
  void inSub(T in) {
    Object o = methodInner(in);
    o = f;
    o = g;
    o = toString();
  }

  class SubNested {
    // Every implicit receiver has type Issue5174Sub.
    void inNestedOuterMembers(T in) {
      Object o = methodInner(in);
      o = f;
      o = g;
    }

    // Every implicit receiver has type SubNested.
    void inNestedOwnMembers() {
      Object o = toString();
    }
  }
}
