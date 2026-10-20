package publicpkg;

// This class exists so that a class in this package can inherit, rather than declare, a public
// member type named "CharBuffer".  It is used by `OnDemandImportAccessibility.java`.
public class PublicCharBufferHolder {

  // This type is relevant, because it is a subtype of `CharSequence`, which the checker's
  // `@RelevantJavaTypes` lists.
  public abstract static class CharBuffer implements CharSequence {}
}
