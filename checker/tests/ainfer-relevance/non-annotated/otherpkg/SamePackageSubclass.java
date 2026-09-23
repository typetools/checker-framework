package otherpkg;

// This class exists so that a static import on demand in another package can name a class that
// inherits a package-private member type.  It is in the same package as
// `PackagePrivateMemberTypes`, so it inherits `PackagePrivateMemberTypes.CharBuffer`.  It is used
// by `OnDemandImportAccessibility.java`.
public class SamePackageSubclass extends PackagePrivateMemberTypes {}
