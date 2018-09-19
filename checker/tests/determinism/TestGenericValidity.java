import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestGenericValidity {
    <T extends @NonDet Object> void testValidity(T @Det [] arr) {}
}
