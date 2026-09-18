package otherpkg;

// This class exists so that a compilation unit in another package can import on demand, from a
// subclass, a public member type whose simple name is also the simple name of a member type of
// `ProtectedMemberType`.  This class is used by `StaticImportNotAccessible.java`.
public class PublicMemberType {

  // This type is relevant, because it is a subtype of `CharSequence`, which the checker's
  // `@RelevantJavaTypes` lists.
  public abstract static class MemberType implements CharSequence {}

  // `Subclass` inherits `MemberType` rather than declaring it, so `Elements` cannot look up the
  // name "otherpkg.PublicMemberType.Subclass.MemberType"; inference must search the supertypes of
  // `Subclass` to determine what an import on demand of `Subclass` imports.
  public static class Subclass extends PublicMemberType {}
}
