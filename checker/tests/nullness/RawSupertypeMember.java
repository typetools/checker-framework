// The supertypes of a raw type are erased (JLS 4.8), even when the supertype is written with type
// arguments in the source, as `Base<T> extends Super<@Nullable String>` is here.  javac agrees:
// for a receiver of the raw type `Base`, `get()` returns the raw type `List`, not
// `List<@Nullable String>`.
//
// SupertypeFinder records this by calling setIsUnderlyingTypeRaw() on the supertypes of a raw
// type; the underlying javac type still has its type arguments, so TypesUtils.isRaw returns false
// for it.  AnnotatedTypes.asMemberOf used to consult
// AnnotatedDeclaredType.isUnderlyingTypeRaw(); after switching to TypesUtils.isRaw(underlying
// type), members of such a supertype are no longer erased.

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings({"rawtypes", "unchecked"})
public class RawSupertypeMember {

  static class Super<K> {
    List<K> get() {
      throw new RuntimeException();
    }
  }

  static class Base<T> extends Super<@Nullable String> {}

  // Because Base is used raw, everything Sub inherits from Super is erased, so `get()` overrides
  // a method whose return type is the raw type `List`.
  static class Sub extends Base {
    // TODO: This is a false positive.  The overridden method's return type is erased, because
    // Super is a supertype of the raw type Base.
    @Override
    // :: error: [override.return]
    List<@NonNull String> get() {
      throw new RuntimeException();
    }
  }
}
