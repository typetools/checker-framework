package org.checkerframework.afu.annotator.tests;

import java.io.Closeable;
import java.io.IOException;

public class Receivers {
  public void m() {}

  public void spaces() {}

  public void m(int i) {}

  public void spaces(int i) {}

  public void m(@Anno() String s) {}
}

class Receivers2 {
  public void m(Receivers2 this) {}

  public void spaces(Receivers2 this) {}

  public void m(Receivers2 this, int i) {}

  public void spaces(Receivers2 this, int i) {}
}

class Receivers3<K, V> {
  public void m() {}

  public void m(int i) {}
}

class Receivers4<K, V> {
  public void m(Receivers4<K, V> this) {}

  public void m(Receivers4<K, V> this, int i) {}
}

interface Receivers5 {
  public void m();
}

enum Receivers6 {
  TEST;

  public void m() {}
}

class Receivers7<K extends Object, V> {
  public void m() {}
}

class Receivers8<K extends Object> {
  public void m(Receivers8<K> this) {}
}

class Receivers9 {
  public void m() {}
}

class Receivers10<K, V> {
  public void m(Receivers10<K, V> this) {}

  public void m(Receivers10<K, V> this, Receivers10<K, V> other) {}
}

@interface Anno {}

// Test receiver insertion on inner class's default constructor.
final class ScriptBasedMapping {
  private final class RawScriptBasedMapping {}
}

// Test receiver insertion before first parameter annotation.
interface GenericInterface<T extends Object> {
  public T map(T toMap);
}

class GenericArray<Z extends Object> implements GenericInterface<String[]> {
  private Z z;

  public void setZ(Z z) {
    this.z = z;
  }

  public String[] map(String[] toMap) {
    return toMap;
  }
}

class GenericFields {
  private GenericArray<String> genArray;
}

// Test inner receiver insertion before first parameter annotation.
class Outer<T, S> {
  class Inner<T2 extends T> {
    private S s;
    private T t;

    protected void initialize(S s, T t) {
      this.s = s;
      this.t = t;
    }

    public Inner(S s, T t) {
      initialize(s, t);
    }
  }
}

// Test that parameters inside an anonymous class get annotated.
interface Interface {
  String get(String param);
}

// Test for infinite loop bug.
class Closer<T> implements Closeable {
  private final Closeable proxyProvider = System.out;

  @Override
  public void close() throws IOException {
    proxyProvider.close();
  }
}
