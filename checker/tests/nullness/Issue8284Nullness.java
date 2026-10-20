// Test case for https://github.com/typetools/checker-framework/issues/8284

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue8284Nullness {
  static class TypeDescriptor<T> {}

  static class Getter<K, V> {}

  static class Box<T> {}

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

  static void nullableTypeArgument(TypeDescriptor<@Nullable String> td, Box<@NonNull String> box) {
    // :: error: (dereference.of.nullable)
    descriptorThenBoxId(td, box).length();
  }

  static List<Getter<@Nullable String, Object>> nullableTarget(TypeDescriptor<String> td) {
    // :: error: (return)
    return getGetters(td);
  }
}
