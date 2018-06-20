import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestOverride {
    protected @PolyDet int mult(@PolyDet int a) {
        return a * a;
    }

    protected @PolyDet ArrayList<Integer> newList(@PolyDet int a) {
        return new ArrayList<Integer>(a);
    }

    protected @PolyDet int getList(@PolyDet ArrayList<Integer> a) {
        return a.get(0);
    }
}

class Child extends TestOverride {
    @Override
    protected @Det int mult(@PolyDet int a) {
        return 5;
    }

    @Override
    protected @Det ArrayList<@Det Integer> newList(@NonDet int a) {
        // :: error: (return.type.incompatible)
        return new ArrayList<Integer>(a);
    }

    @Override
    protected @PolyDet int getList(@Det ArrayList<Integer> a) {
        // :: error: (override.param.invalid)
        return a.get(0);
    }
}
