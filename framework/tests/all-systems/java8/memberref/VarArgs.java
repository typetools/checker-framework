
interface VarArgsFunc {
    void take(String ... in);
}

interface ArrayFunc {
    void take(String[] in);
}

class VarArgsTest {

    static void myMethod(String ... in){ }
    static void myMethodArray(String[] in){ }

    VarArgsFunc v1 = VarArgsTest::myMethod;
    VarArgsFunc v2 = VarArgsTest::myMethodArray;

    ArrayFunc v3 = VarArgsTest::myMethod;
    ArrayFunc v4 = VarArgsTest::myMethodArray;
}

