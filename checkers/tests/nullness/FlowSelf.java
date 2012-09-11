import checkers.nullness.quals.*;

class FlowSelf {

    void test() {

        String s = "foo";
        if (s == null)
            return;
        assert s != null;

        s = s.substring(1);

    }

}
