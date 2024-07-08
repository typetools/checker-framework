interface VarArgsFunc {
  void take(String... in);
}

interface ArrayFunc {
  void take(String[] in);
}

// I do not understand why, but changing the name of this class to
// "MemberRefVarargsTest" (with a lowercase "a") leads to a test failure:
//   :0: warning: MemberRefVarargsTest-org.checkerframework.common.value.ValueChecker.astub:(line
//         5,col 1): Type not found: MemberRefVarargsTest
class MemberRefVarArgsTest {

  static void myMethod(String... in) {}

  static void myMethodArray(String[] in) {}

  VarArgsFunc v1 = MemberRefVarArgsTest::myMethod;
  VarArgsFunc v2 = MemberRefVarArgsTest::myMethodArray;

  ArrayFunc v3 = MemberRefVarArgsTest::myMethod;
  ArrayFunc v4 = MemberRefVarArgsTest::myMethodArray;
}

interface RegularFunc {
  void take(Object o);
}

interface RegularFunc2 {
  void take(Object o, String s);
}

interface RegularFunc3 {
  void take(Object o, String s, String s2);
}

class MoreVarAgrgsTest {
  static void myObjectArgArg(Object o, String... vararg) {}

  RegularFunc v1 = MoreVarAgrgsTest::myObjectArgArg;
  RegularFunc2 v2 = MoreVarAgrgsTest::myObjectArgArg;
  RegularFunc3 v4 = MoreVarAgrgsTest::myObjectArgArg;
}
