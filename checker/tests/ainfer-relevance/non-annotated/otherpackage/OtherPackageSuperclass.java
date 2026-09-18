package otherpackage;

// This class exists so that a test input in the unnamed package can extend a class that is in a
// different package and that declares a package-private member type.  It is used by
// `PackagePrivateNotInherited.java`.
//
// This class's member class `List` is irrelevant, because it is not a subtype of any type in the
// checker's `@RelevantJavaTypes`.  By contrast, the top-level class `List` in the unnamed package
// is relevant.
public class OtherPackageSuperclass extends OtherPackageGrandparent {

  // A subclass in another package does not inherit this member type, so the name "List" in such a
  // subclass's body does not refer to it.  This declaration also hides
  // `OtherPackageGrandparent.List`, which no subclass of this class inherits.
  static class List {}
}
