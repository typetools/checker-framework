interface VarargsFunc {
  void take(String... in);
}

interface ArrayFunc {
  void take(String[] in);
}

class MemberRefVarArgsTest {

  static void myMethod(String... in) {}

  static void myMethodArray(String[] in) {}

  VarargsFunc v1 = MemberRefVarArgsTest::myMethod;
  VarargsFunc v2 = MemberRefVarArgsTest::myMethodArray;

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
