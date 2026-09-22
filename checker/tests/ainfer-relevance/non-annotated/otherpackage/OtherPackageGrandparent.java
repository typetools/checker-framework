package otherpackage;

// This class exists so that `OtherPackageSuperclass`'s package-private member type hides an
// inherited member type of the same name.  It is used by `PackagePrivateNotInherited.java`.
//
// This class's member class `List` is irrelevant, because it is not a subtype of any type in the
// checker's `@RelevantJavaTypes`.  By contrast, the top-level class `List` in the unnamed package
// is relevant.
public class OtherPackageGrandparent {

  // `OtherPackageSuperclass` declares a member type of the same name, so `OtherPackageSuperclass`
  // does not inherit this one, and therefore no subclass of `OtherPackageSuperclass` does either --
  // not even a subclass in this package, for which this member type is accessible.
  public static class List {}
}
