package otherpkg;

// This class exists so that a compilation unit in another package can import on demand, from a
// subclass, a protected member type that this class declares.  A protected member type is
// inherited by a subclass in another package, but outside the package that declares it, the member
// type is accessible only within the body of such a subclass -- and an import declaration is not
// within the body of any class.  This class is used by `StaticImportNotAccessible.java`.
public class ProtectedMemberType {

  // This type is irrelevant, because it is not a subtype of any type in the checker's
  // `@RelevantJavaTypes`.
  protected static class MemberType {}

  // `Subclass` inherits `MemberType` rather than declaring it, so `Elements` cannot look up the
  // name "otherpkg.ProtectedMemberType.Subclass.MemberType"; inference must search the supertypes
  // of `Subclass` to determine what an import on demand of `Subclass` imports.
  public static class Subclass extends ProtectedMemberType {}
}
