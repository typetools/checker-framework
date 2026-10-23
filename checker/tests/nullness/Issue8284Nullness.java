// Test case for https://github.com/typetools/checker-framework/issues/8284

import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

public class Issue8284Nullness {
  static class TypeDescriptor<T> {}

  static class Getter<K, V> {}

  static class Box<T> {
    T get() {
      throw new RuntimeException();
    }
  }

  static <T> List<Getter<@NonNull T, Object>> getGetters(TypeDescriptor<T> typeDescriptor) {
    throw new RuntimeException();
  }

  static <T> List<Getter<@NonNull T, Object>> caller(TypeDescriptor<T> targetTypeDescriptor) {
    return getGetters(targetTypeDescriptor);
  }

  static <T> void descriptorThenBox(TypeDescriptor<T> td, Box<@NonNull T> box) {}

  static <T> void boxThenDescriptor(Box<@NonNull T> box, TypeDescriptor<T> td) {}

  static <T> void argumentsOnly(TypeDescriptor<T> td, Box<@NonNull T> box) {
    descriptorThenBox(td, box);
    boxThenDescriptor(box, td);
  }

  static <T> List<Getter<@NonNull T, Object>> boxAndDescriptor(
      Box<@NonNull T> b, TypeDescriptor<T> td) {
    throw new RuntimeException();
  }

  static <S> Box<@NonNull S> makeBox() {
    throw new RuntimeException();
  }

  static <T> List<Getter<@NonNull T, Object>> nestedCall(TypeDescriptor<T> td) {
    return boxAndDescriptor(makeBox(), td);
  }

  static <T extends @Nullable Object> T descriptorThenBoxId(
      TypeDescriptor<T> td, Box<@NonNull T> box) {
    throw new RuntimeException();
  }

  static <T extends @Nullable Object> T boxThenDescriptorId(
      Box<@NonNull T> box, TypeDescriptor<T> td) {
    throw new RuntimeException();
  }

  static <S extends @Nullable Object> TypeDescriptor<S> wrap(TypeDescriptor<S> td) {
    throw new RuntimeException();
  }

  static <S extends @Nullable Object> Box<@NonNull S> wrapBox(Box<@NonNull S> box) {
    throw new RuntimeException();
  }

  // T is inferred to be U, which may be null, so each result may be null.
  static <U extends @Nullable Object> void nullableTypeVariable(
      TypeDescriptor<U> td, Box<@NonNull U> box) {
    // :: error: (dereference.of.nullable)
    descriptorThenBoxId(td, box).toString();
    // :: error: (dereference.of.nullable)
    boxThenDescriptorId(box, td).toString();
    // The bound T = U becomes proper only after wrap's type argument is inferred.
    // :: error: (dereference.of.nullable)
    boxThenDescriptorId(box, wrap(td)).toString();
    // :: error: (dereference.of.nullable)
    descriptorThenBoxId(wrap(td), box).toString();
    // :: error: (dereference.of.nullable)
    boxThenDescriptorId(wrapBox(box), wrap(td)).toString();
  }

  static <T extends @Nullable Object> T pick(Box<@NonNull T> box, T other) {
    throw new RuntimeException();
  }

  static <T extends @Nullable Object> T unbox(Box<@NonNull T> box) {
    throw new RuntimeException();
  }

  // The only EQUAL bound on T is T = @NonNull U, whose annotations are ignored.  It gives T's Java
  // type, but T's annotations come from the other argument.
  static <U extends @Nullable Object> void onlyIgnoredEqualBound(
      Box<@NonNull U> box, @Nullable U nble, @NonNull U nn) {
    // :: error: (dereference.of.nullable)
    pick(box, nble).toString();
    pick(box, nn).toString();
    unbox(box).toString();
    @Nullable U x = unbox(box);
  }

  static <X extends @Nullable Object> X nonNullThenExtends(Box<@NonNull X> a, Box<? extends X> b) {
    throw new RuntimeException();
  }

  static <X extends @Nullable Object> X extendsThenNonNull(Box<? extends X> b, Box<@NonNull X> a) {
    throw new RuntimeException();
  }

  // X = @NonNull U ignores annotations, so X's annotations come from Box<? extends U>.
  static <U extends @Nullable Object> void ignoredAndWildcard(
      Box<@NonNull U> box, Box<? extends U> ext) {
    nonNullThenExtends(box, ext);
    extendsThenNonNull(ext, box);
  }

  static <T extends @Nullable Object> Box<? extends T> wildcardBox(Box<@NonNull T> b) {
    throw new RuntimeException();
  }

  static <X extends @Nullable Object> X takeExtends(Box<? extends X> x) {
    throw new RuntimeException();
  }

  static <K extends @Nullable Object, V extends @Nullable Object> Map<K, ? extends V> wildcardMap(
      Box<@NonNull K> k, Box<@NonNull V> v) {
    throw new RuntimeException();
  }

  static <K extends @Nullable Object, V extends @Nullable Object> V getValue(
      Map<K, ? extends V> m) {
    throw new RuntimeException();
  }

  // T = @NonNull U, whose annotations are ignored, is T's instantiation as soon as it is found, so
  // the capture of wildcardBox's return type is bounded by U.
  static <U extends @Nullable Object> void captureOfReturnType(Box<@NonNull U> box) {
    takeExtends(wildcardBox(box));
    nonNullThenExtends(box, wildcardBox(box));
    extendsThenNonNull(wildcardBox(box), box);
    getValue(wildcardMap(box, box));
  }

  static <T extends @Nullable Object> @PolyNull T poly(@PolyNull T a) {
    throw new RuntimeException();
  }

  static <T extends @Nullable Object> @PolyNull T polyBox(@PolyNull T a, Box<T> b) {
    throw new RuntimeException();
  }

  static <T extends @Nullable Object> List<@PolyNull T> polyList(@PolyNull T a, Box<T> b) {
    throw new RuntimeException();
  }

  static <S extends @Nullable Object> S make() {
    throw new RuntimeException();
  }

  static <S extends @Nullable Object> S id(S s) {
    return s;
  }

  // A polymorphic qualifier on a use of an inference variable is not copied onto the variable's
  // instantiation when the instantiation is substituted for the use.
  static <U extends @Nullable Object> void polymorphicQualifierOnUse(
      Box<@Nullable String> bn, @Nullable String n, Box<U> bu, @NonNull U nnu) {
    String s1 = poly(make());
    @Nullable String s2 = poly(make());
    @Nullable String s3 = polyBox(id(n), bn);
    List<@Nullable String> l = polyList(id(n), bn);
    @NonNull U u = polyBox(id(nnu), bu);
  }

  static <T extends @Nullable Object> Box<T> nullableBox(Box<@Nullable T> box, T other) {
    throw new RuntimeException();
  }

  static <T extends @Nullable Object> Box<T> nonNullBox(Box<@NonNull T> box, T other) {
    throw new RuntimeException();
  }

  // T's Java-type-only instantiation, from the ignored bound on T, is substituted into S's lower
  // bound Box<T>
  // before T's annotations are known.  S's instantiation must still end up consistent with T's.
  static <U extends @Nullable Object> void substitutedIntoAnotherBound(
      Box<@Nullable U> nullableU, @NonNull U nn, Box<@NonNull U> nonNullU, @Nullable U nble) {
    id(nullableBox(nullableU, nn));
    Box<@NonNull U> b1 = id(nullableBox(nullableU, nn));
    id(nullableBox(nullableU, nn)).get().toString();
    id(nonNullBox(nonNullU, nble));
    Box<@Nullable U> b2 = id(nonNullBox(nonNullU, nble));
    // :: error: (dereference.of.nullable)
    id(nonNullBox(nonNullU, nble)).get().toString();
  }

  static <T extends @Nullable Object> Box<? extends T> extendsBox(Box<@NonNull T> box, T other) {
    throw new RuntimeException();
  }

  static <S extends @Nullable Object> @NonNull S castNonNull(@Nullable S s) {
    throw new RuntimeException();
  }

  static <T extends @Nullable Object> List<Box<T>> listOfBoxes(Box<@NonNull T> box, T other) {
    throw new RuntimeException();
  }

  static <S extends @Nullable Object> S pick2(S a, S b) {
    return a;
  }

  // A Java-type-only instantiation substituted inside a wildcard bound, under a use with a primary
  // annotation, and inside a type that is itself substituted into another type.
  static <U extends @Nullable Object> void ignoredSubstitutionsInOtherTypes(
      Box<@NonNull U> nonNullU,
      @Nullable U nble,
      Box<? extends @Nullable U> ext,
      List<Box<@Nullable U>> boxes) {
    Box<? extends @Nullable U> e1 = extendsBox(nonNullU, nble);
    Box<? extends @Nullable U> e2 = pick2(extendsBox(nonNullU, nble), ext);
    Box<@Nullable U> c1 = castNonNull(nonNullBox(nonNullU, nble));
    Box<@Nullable U> c2 = id(castNonNull(nonNullBox(nonNullU, nble)));
    List<Box<@Nullable U>> l1 = listOfBoxes(nonNullU, nble);
    List<Box<@Nullable U>> l2 = id(listOfBoxes(nonNullU, nble));
    List<Box<@Nullable U>> l3 = pick2(id(listOfBoxes(nonNullU, nble)), boxes);
    // :: error: (dereference.of.nullable)
    id(id(listOfBoxes(nonNullU, nble))).get(0).get().toString();
  }

  static void nullableTypeArgument(TypeDescriptor<@Nullable String> td, Box<@NonNull String> box) {
    // :: error: (dereference.of.nullable)
    descriptorThenBoxId(td, box).length();
  }

  static List<Getter<@Nullable String, Object>> nullableTarget(TypeDescriptor<String> td) {
    // :: error: (return)
    return getGetters(td);
  }
}
