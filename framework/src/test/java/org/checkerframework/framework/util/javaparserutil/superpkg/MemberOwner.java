package org.checkerframework.framework.util.javaparserutil.superpkg;

/**
 * Test data for {@code JavaParserUtilTest}: a class that declares a member type that inherits the
 * member types of {@link Base}. A name such as {@code Member.Visible} is therefore not canonical.
 */
public class MemberOwner {

  /** Creates a new MemberOwner. */
  public MemberOwner() {}

  /** A private member type, which is in scope in this class's body but in no other class. */
  private static class Member extends Base {}

  /** Uses {@code Member.Visible}, whose name is not canonical. */
  Member.Visible f;
}
