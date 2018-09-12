import org.checkerframework.checker.determinism.qual.*;

public class TestClassAnnos {
    void test1() {
        Node nd = new Node();
        System.out.println(nd.getData());

        NonDetNode nd1 = new NonDetNode();
        // :: error: (argument.type.incompatible)
        System.out.println(nd1.getVal());
    }
}

@Det class Node {
    int data;

    public Node() {
        data = 10;
    }

    int getData() {
        System.out.println(data);
        return data;
    }

    int getData1(int a) {
        // :: error: (argument.type.incompatible)
        System.out.println(a);
        return data;
    }
}

@NonDet class NonDetNode {
    String val;

    public NonDetNode() {
        val = "hi";
    }

    String getVal() {
        System.out.println(val);
        return val;
    }
}
