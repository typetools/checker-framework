// Test case for issue 295:
// https://code.google.com/p/checker-framework/issues/detail?id=295

// Skipped until the issue is fixed.
// @skip-test

import checkers.nullness.quals.Nullable;

abstract class Issue295 {

    static class Box<T> {
        T value;
        Box(T value) { this.value = value; }
    }

    <T> void f1(Factory<Box</*@Nullable*/ T>> f) {
        Box</*@Nullable*/ T> v = load(f);
    }

    <T> void f2(Factory<Box</*@Nullable*/ T>> f) {
        Box<? extends /*@Nullable*/ T> v = load(f);
    }

    <T> void f3(Factory<Box<T>> f) {
        Box<T> v = load(f);
    }

    <T> void f4(Factory<Box</*@Nullable*/ T>> f) {
        Box<? extends /*@Nullable*/ T> v = load(f);
    }

    <T extends /*@Nullable*/ Object> void f5(Factory<Box<T>> f) {
        Box<T> v = load(f);
    }

    <T extends /*@Nullable*/ Object> void f6(Factory<Box<T>> f) {
        Box<? extends T> v = load(f);
    }

    <T> void f1noquals(Factory<Box<String>> f) {
        Box<String> v = load(f);
    }

    abstract <T> T load(Factory<T> p);
    abstract class Factory<T> { abstract T create(); }
}

