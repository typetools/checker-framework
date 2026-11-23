// Test case for https://github.com/typetools/checker-framework/issues/3028

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue3028 {
  void cast(Object o) {
    Issue3028 cast = (Issue3028) o;
  }

  void initializedCast(Object o) {
    Issue3028 cast = (@Initialized Issue3028) o;
  }

  void nullableCast(@Nullable Object o) {
    // :: warning: [cast.unsafe]
    Issue3028 cast = (@NonNull Issue3028) o;
  }

  void arrayCast(Object[] o) {
    Issue3028[] cast = (Issue3028[]) o;
  }

  void unsafeParameterizedDowncast(Collection<@Nullable String> c) {
    // :: warning: [cast.unsafe]
    List<@NonNull String> list = (List<@NonNull String>) c;
  }

  void safeParameterizedDowncast(Collection<@Nullable String> c) {
    List<@Nullable String> list = (List<@Nullable String>) c;
  }

  void safeParameterizedDowncastToArrayList(Collection<@NonNull String> c) {
    ArrayList<@NonNull String> list = (ArrayList<@NonNull String>) c;
  }

  void unsafeParameterizedUpcast(ArrayList<@Nullable String> c) {
    // :: warning: [cast.unsafe]
    List<@NonNull String> list = (List<@NonNull String>) c;
  }

  void safeParameterizedUpcast(ArrayList<@NonNull String> c) {
    List<@NonNull String> list = (List<@NonNull String>) c;
  }

  void unrelatedInterfaces(Runnable r) {
    Serializable s = (Serializable) r;
  }

  void unrelatedInterfacesNullable(@Nullable Runnable r) {
    // :: warning: [cast.unsafe]
    Serializable s = (@NonNull Serializable) r;
  }

  <T extends Collection<@Nullable String>> void typeVariableDowncast(T t) {
    // :: warning: [cast.unsafe]
    List<@NonNull String> list = (List<@NonNull String>) t;
  }

  <T extends Collection<@Nullable String>> void safeTypeVariableDowncast(T t) {
    List<@Nullable String> list = (List<@Nullable String>) t;
  }

  <U extends Collection<@Nullable String>, T extends U> void nestedTypeVariableDowncast(T t) {
    // :: warning: [cast.unsafe]
    List<@NonNull String> list = (List<@NonNull String>) t;
  }

  <U extends Collection<@Nullable String>, T extends U> void safeNestedTypeVariableDowncast(T t) {
    List<@Nullable String> list = (List<@Nullable String>) t;
  }

  @SuppressWarnings("unchecked")
  <T extends Issue3028> T castToTypeVariable(Object o) {
    return (T) o;
  }

  void intersectionCast(Object o) {
    Object s = (Runnable & Serializable) o;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  void rawDowncast(Collection c) {
    // An unchecked cast does not verify the type arguments.
    // :: warning: [cast.unsafe]
    List<String> list = (List<String>) c;
  }

  @SuppressWarnings("unchecked")
  void uncheckedDowncast(Object o) {
    // :: warning: [cast.unsafe]
    List<@Nullable String> list = (List<@Nullable String>) o;
  }

  @SuppressWarnings("unchecked")
  <T> void uncheckedWildcardCast(Comparator<? super T> c) {
    // :: warning: [cast.unsafe]
    Comparator<T> c2 = (Comparator<T>) c;
  }

  static class StringList extends ArrayList<@NonNull String> {}

  void downcastToNonGenericSubclass(List<@NonNull String> list) {
    StringList s = (StringList) list;
  }

  void unsafeDowncastToNonGenericSubclass(List<@Nullable String> list) {
    // :: warning: [cast.unsafe]
    StringList s = (StringList) list;
  }

  <T extends Runnable & List<@Nullable String>>
      void unsafeDowncastToNonGenericSubclassFromIntersection(T t) {
    // As in unsafeDowncastToNonGenericSubclass, the value is a List<@Nullable String>.
    // :: warning: [cast.unsafe]
    StringList s = (StringList) t;
  }

  <T extends List<@Nullable String> & Runnable>
      void unsafeDowncastToNonGenericSubclassFromIntersectionListFirst(T t) {
    // :: warning: [cast.unsafe]
    StringList s = (StringList) t;
  }

  <T extends Runnable & List<@NonNull String>> void downcastToNonGenericSubclassFromIntersection(
      T t) {
    StringList s = (StringList) t;
  }

  <T extends Runnable & List<@NonNull String>> void downcastFromIntersection(T t) {
    ArrayList<@NonNull String> list = (ArrayList<@NonNull String>) t;
  }

  <T extends Runnable & List<@Nullable String>> void unsafeDowncastFromIntersection(T t) {
    // :: warning: [cast.unsafe]
    ArrayList<@NonNull String> list = (ArrayList<@NonNull String>) t;
  }

  void upcastToObject(List<@Nullable String> list) {
    Object o = (Object) list;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  void rawUpcast(ArrayList<String> c) {
    List list = (List) c;
  }

  @SuppressWarnings("rawtypes")
  void rawUpcastToWildcard(List rawList) {
    List<?> list = (List<?>) rawList;
    // A raw type determines no type arguments.
    // :: warning: [cast.unsafe]
    List<? extends @NonNull Object> list2 = (List<? extends @NonNull Object>) rawList;
  }

  @SuppressWarnings("rawtypes")
  void rawDowncastToWildcard(List rawList) {
    ArrayList<?> list = (ArrayList<?>) rawList;
    // :: warning: [cast.unsafe]
    ArrayList<? extends @NonNull Object> list2 = (ArrayList<? extends @NonNull Object>) rawList;
  }

  void primitiveCasts(long l, Integer i) {
    int x = (int) l;
    long y = (long) x;
    int z = (int) i;
  }

  void boxingCast(int i) {
    Comparable<Integer> c = (Comparable<Integer>) i;
    Object o = (Object) i;
  }

  void unboundedWildcardDowncast(Object o) {
    List<?> list = (List<?>) o;
    List<? extends @Nullable Object> list2 = (List<? extends @Nullable Object>) o;
  }

  void nonNullWildcardDowncast(Object o) {
    // The run-time check does not verify that the list has no null elements.
    // :: warning: [cast.unsafe]
    List<? extends @NonNull Object> list = (List<? extends @NonNull Object>) o;
  }

  static class Outer<T> {
    class Inner {}
  }

  // javac 17 treats a cast to `? extends Object` from a non-Object type as unchecked.
  @SuppressWarnings("unchecked")
  void nonNullWildcardEnclosingTypeDowncast(Object o) {
    Outer<?>.Inner inner = (Outer<?>.Inner) o;
    // The run-time check does not verify the type argument of the enclosing type.
    // :: warning: [cast.unsafe]
    Outer<? extends @NonNull Object>.Inner inner2 = (Outer<? extends @NonNull Object>.Inner) o;
  }

  void nonNullWildcardDowncastFromCollection(Collection<? extends @NonNull Object> c) {
    List<? extends @NonNull Object> list = (List<? extends @NonNull Object>) c;
  }

  void nullableWildcardDowncastFromCollection(Collection<? extends @Nullable Object> c) {
    // :: warning: [cast.unsafe]
    List<? extends @NonNull Object> list = (List<? extends @NonNull Object>) c;
  }

  void wildcardArrayDowncast(Object[] o) {
    List<?>[] lists = (List<?>[]) o;
    // :: warning: [cast.unsafe]
    List<? extends @NonNull Object>[] lists2 = (List<? extends @NonNull Object>[]) o;
  }

  void wildcardIntersectionDowncast(Object o) {
    Object x = (List<?> & Serializable) o;
    // :: warning: [cast.unsafe]
    Object y = (List<? extends @NonNull Object> & Serializable) o;
  }

  interface Extra<E> {}

  // javac 17 treats a cast to `? extends Object` from a non-Object type as unchecked.
  @SuppressWarnings("unchecked")
  void intersectionBoundUnrelatedToExpression(Collection<?> c) {
    Object x = (List<?> & Extra<?>) c;
    // :: warning: [cast.unsafe]
    Object y = (List<?> & Extra<? extends @NonNull Object>) c;
  }

  void fBoundedWildcardDowncast(Object o) {
    Enum<?> e = (Enum<?>) o;
    Comparable<?> c = (Comparable<?>) o;
  }

  // javac 17 treats a cast to `? extends Object` from a non-Object type as unchecked.
  @SuppressWarnings("unchecked")
  void unrelatedWildcardCast(Runnable r) {
    List<?> list = (List<?>) r;
    // :: warning: [cast.unsafe]
    List<? extends @NonNull Object> list2 = (List<? extends @NonNull Object>) r;
  }

  <T extends Runnable & List<@NonNull String>> void upcastFromIntersectionSecondBound(T t) {
    List<@NonNull String> list = (List<@NonNull String>) t;
  }

  // javac treats a cast from an intersection whose first bound is generic as unchecked.
  @SuppressWarnings("unchecked")
  <T extends List<@NonNull String> & Runnable> void upcastFromIntersectionFirstBound(T t) {
    List<@NonNull String> list = (List<@NonNull String>) t;
  }

  <T extends Runnable & Collection<@NonNull String>> void downcastFromIntersectionSecondBound(T t) {
    List<@NonNull String> list = (List<@NonNull String>) t;
  }

  <T extends Runnable & Collection<@Nullable String>>
      void unsafeDowncastFromIntersectionSecondBound(T t) {
    // :: warning: [cast.unsafe]
    List<@NonNull String> list = (List<@NonNull String>) t;
  }

  // javac treats a cast from an intersection whose first bound is generic as unchecked.
  @SuppressWarnings("unchecked")
  <T extends Collection<@NonNull String> & Runnable> void downcastFromIntersectionFirstBound(T t) {
    List<@NonNull String> list = (List<@NonNull String>) t;
  }

  // javac treats a cast from an intersection whose first bound is generic as unchecked.
  @SuppressWarnings("unchecked")
  <T extends Collection<@Nullable String> & Runnable> void unsafeDowncastFromIntersectionFirstBound(
      T t) {
    // :: warning: [cast.unsafe]
    List<@NonNull String> list = (List<@NonNull String>) t;
  }

  void intersectionCastGenericSecondBound(List<@Nullable String> l, List<@NonNull String> nn) {
    Object o = (Runnable & List<@NonNull String>) nn;
    // :: warning: [cast.unsafe]
    Object o2 = (Runnable & List<@NonNull String>) l;
  }

  void intersectionCastGenericFirstBound(List<@Nullable String> l, List<@NonNull String> nn) {
    Object o = (List<@NonNull String> & Runnable) nn;
    // :: warning: [cast.unsafe]
    Object o2 = (List<@NonNull String> & Runnable) l;
  }
}
