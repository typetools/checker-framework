import checkers.nullness.quals.*;

// This is the example from manual section:
// "Generics (parametric polymorphism or type polymorphism)"
// whose source code is ../../manual/advanced-features.tex
class GenericsExample {

  class MyList1<@Nullable T> {
    T t;
    @Nullable T nble;
    @NonNull T nn;
    MyList1(T t, @Nullable T nble, @NonNull T nn) {
      this.t = t; this.nble = nble; this.nn = nn;
    }
    void add(T arg) { }
    T get(int i) { return t; }
    void m() {
      t = null;
      t = nble;
      nble = null;
      //:: error: (assignment.type.incompatible)
      nn = null;
      t = this.get(0);
      nble = this.get(0);
      //:: error: (assignment.type.incompatible)
      nn = this.get(0);
      this.add(t);
      this.add(nble);
      this.add(nn);
    }
  }

  class MyList2<@NonNull T> {
    T t;
    @Nullable T nble;
    @NonNull T nn;
    MyList2(T t, @Nullable T nble, @NonNull T nn) {
      this.t = t; this.nble = nble; this.nn = nn;
    }
    void add(T arg) { }
    T get(int i) { return t; }
    void m() {
      //:: error: (assignment.type.incompatible)
      t = null;
      //:: error: (assignment.type.incompatible)
      t = nble;
      nble = null;
      //:: error: (assignment.type.incompatible)
      nn = null;
      t = this.get(0);
      nble = this.get(0);
      nn = this.get(0);
      this.add(t);
      //:: error: (argument.type.incompatible)
      this.add(nble);
      this.add(nn);
    }
  }

  class MyList3<T extends @Nullable Object> {
    T t;
    @Nullable T nble;
    @NonNull T nn;
    MyList3(T t, @Nullable T nble, @NonNull T nn) {
      this.t = t; this.nble = nble; this.nn = nn;
    }
    void add(T arg) { }
    T get(int i) { return t; }
    void m() {
      //:: error: (assignment.type.incompatible)
      t = null;
      //:: error: (assignment.type.incompatible)
      t = nble;
      nble = null;
      //:: error: (assignment.type.incompatible)
      nn = null;
      t = this.get(0);
      nble = this.get(0);
      //:: error: (assignment.type.incompatible)
      nn = this.get(0);
      this.add(t);
      //:: error: (argument.type.incompatible)
      this.add(nble);
      this.add(nn);
    }
  }

  class MyList4<T extends @NonNull Object> { // same as MyList2
    T t;
    @Nullable T nble;
    @NonNull T nn;
    MyList4(T t, @Nullable T nble, @NonNull T nn) {
      this.t = t; this.nble = nble; this.nn = nn;
    }
    void add(T arg) { }
    T get(int i) { return t; }
    void m() {
      //:: error: (assignment.type.incompatible)
      t = null;
      //:: error: (assignment.type.incompatible)
      t = nble;
      nble = null;
      //:: error: (assignment.type.incompatible)
      nn = null;
      t = this.get(0);
      nble = this.get(0);
      nn = this.get(0);
      this.add(t);
      //:: error: (argument.type.incompatible)
      this.add(nble);
      this.add(nn);
    }
  }

}
