// Test case for issue 295:
// https://github.com/typetools/checker-framework/issues/295

import org.checkerframework.checker.nullness.qual.Nullable;

abstract class Issue295 {

  static class Box<T> {
    T value;

    Box(T value) {
      this.value = value;
    }
  }

  abstract <MTL> MTL load(Factory<MTL> p);

  abstract class Factory<TF> {
    abstract TF create();
  }

  <MT1> void f1(Factory<Box<@Nullable MT1>> f) {
    Box<@Nullable MT1> v = f.create();
    v = load(f);
  }

  <MT2> void f2(Factory<Box<@Nullable MT2>> f) {
    Box<? extends @Nullable MT2> v = load(f);
  }

  <MT3> void f3(Factory<Box<MT3>> f) {
    Box<MT3> v = load(f);
  }

  <MT4> void f4(Factory<Box<@Nullable MT4>> f) {
    Box<? extends @Nullable MT4> v = load(f);
  }

  <MT5 extends @Nullable Object> void f5(Factory<Box<MT5>> f) {
    Box<MT5> v = load(f);
  }

  <MT6 extends @Nullable Object> void f6(Factory<Box<MT6>> f) {
    Box<? extends MT6> v = load(f);
  }

  <MT7> void f1noquals(Factory<Box<String>> f) {
    Box<String> v = load(f);
  }
}
