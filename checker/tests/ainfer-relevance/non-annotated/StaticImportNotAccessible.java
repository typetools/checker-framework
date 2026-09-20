import static otherpkg.ProtectedMemberType.Subclass.*;
import static otherpkg.PublicMemberType.Subclass.*;

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// An import on demand imports only the member types that are accessible where the import appears.
// `otherpkg.ProtectedMemberType.Subclass` inherits the protected member type
// `otherpkg.ProtectedMemberType.MemberType`, which is not accessible in this compilation unit:
// this compilation unit is in a different package than the type that declares it, and an import
// declaration is not within the body of a subclass of that type.  The first import therefore
// imports no type named "MemberType", and the name refers to the type that the second import
// imports.
//
// Inference must resolve the name "MemberType" to `otherpkg.PublicMemberType.MemberType`, which is
// relevant because it is a subtype of `CharSequence`, rather than to
// `otherpkg.ProtectedMemberType.MemberType`, which is inaccessible and is irrelevant.
//
// If inference resolves the name incorrectly and therefore discards the annotation, then the
// second (validation) pass of this test issues the warning that is written below.
public class StaticImportNotAccessible {

  MemberType memberTypeField;

  void assignField() {
    memberTypeField = getSibling1MemberType();
  }

  void useField() {
    // :: warning: [argument]
    expectsSibling1MemberType(memberTypeField);
  }

  void expectsSibling1MemberType(@AinferSibling1 MemberType m) {}

  @AinferSibling1 MemberType getSibling1MemberType() {
    return null;
  }
}
