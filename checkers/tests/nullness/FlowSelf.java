import checkers.nullness.quals.*;

class FlowSelf {

    void test(@Nullable String s) {

        if (s == null)
            return;
        //:: warning: (known.nonnull)
        assert s != null;

        s = s.substring(1);

    }

}
