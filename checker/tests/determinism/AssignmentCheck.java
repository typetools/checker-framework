import org.checkerframework.checker.determinism.qual.*;

public class AssignmentCheck {
    void TestAssignment(@NonDet int a) {
        @NonDet int b = a;
    }
}
