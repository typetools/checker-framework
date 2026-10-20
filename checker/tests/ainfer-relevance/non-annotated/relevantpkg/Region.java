package relevantpkg;

// This class has the same simple name as `irrelevantpkg.Region`, but it is public, so an import on
// demand does import it.  It is relevant, because it is a subtype of `CharSequence`, which the
// checker's `@RelevantJavaTypes` lists.  It is used by `OnDemandImportPackagePrivateType.java`.
public abstract class Region implements CharSequence {}
