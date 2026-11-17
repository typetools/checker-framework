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

  void safeParameterizedDowncast2(Collection<@NonNull String> c) {
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

  void upcastToObject(List<@Nullable String> list) {
    Object o = (Object) list;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  void rawUpcast(ArrayList<String> c) {
    List list = (List) c;
  }

  void primitiveCasts(long l, Integer i) {
    int x = (int) l;
    long y = (long) x;
    int z = (int) i;
  }
}
