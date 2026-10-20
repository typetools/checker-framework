package publicpkg;

// This class exists so that a static import on demand in another package can name a class that
// inherits a public member type, rather than declaring it.  A member type that this class
// declared would have a canonical name, which name resolution looks up before it searches the
// types that an import on demand names.  It is used by `OnDemandImportAccessibility.java`.
public class PublicCharBufferSubclass extends PublicCharBufferHolder {}
