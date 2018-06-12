import java.util.ArrayList;
import org.checkerframework.checker.determinism.qual.*;

public class TestOverride {
    protected @PolyDet int mult(@PolyDet int a) {
        return a * a;
    }

    protected @PolyDet ArrayList<Integer> multList(@PolyDet int a) {
        return new ArrayList<Integer>(a);
    }
}

class Child extends TestOverride {
    @Override
    protected @Det int mult(@PolyDet int a) {
        return 5;
    }

    @Override
    protected @Det ArrayList<@Det Integer> multList(@NonDet int a) {
        // :: error: (return.type.incompatible)
        return new ArrayList<Integer>(a);
    }
}
