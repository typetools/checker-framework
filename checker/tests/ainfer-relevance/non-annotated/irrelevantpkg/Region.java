package irrelevantpkg;

// This class is package-private, so a compilation unit in another package cannot access it, and an
// import on demand imports only the types that are accessible where it appears.  This class is
// irrelevant, because it is not a subtype of any type in the checker's `@RelevantJavaTypes`.  It is
// used by `OnDemandImportPackagePrivateType.java`.
class Region {}
