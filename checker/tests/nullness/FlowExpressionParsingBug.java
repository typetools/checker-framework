import javax.swing.JMenuBar;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public abstract class FlowExpressionParsingBug {

    //// Check that JavaExpressions with explicit and implicit 'this' work

    protected @Nullable JMenuBar menuBar = null;

    @RequiresNonNull("menuBar")
    public void addFavorite() {}

    @RequiresNonNull("this.menuBar")
    public void addFavorite1() {}

    //// Check JavaExpressions for static fields with different ways to access the field

    static @Nullable String i = null;

    @RequiresNonNull("FlowExpressionParsingBug.i")
    public void a() {}

    @RequiresNonNull("i")
    public void b() {}

    @RequiresNonNull("this.i")
    public void c() {}

    void test1() {
        // :: error: (contracts.precondition.not.satisfied)
        a();
        FlowExpressionParsingBug.i = "";
        a();
    }

    void test1b() {
        // :: error: (contracts.precondition.not.satisfied)
        a();
        i = "";
        a();
    }

    void test1c() {
        // :: error: (contracts.precondition.not.satisfied)
        a();
        this.i = "";
        a();
    }

    void test2() {
        // :: error: (contracts.precondition.not.satisfied)
        b();
        FlowExpressionParsingBug.i = "";
        b();
    }

    void test2b() {
        // :: error: (contracts.precondition.not.satisfied)
        b();
        i = "";
        b();
    }

    void test2c() {
        // :: error: (contracts.precondition.not.satisfied)
        b();
        this.i = "";
        b();
    }

    void test3() {
        // :: error: (contracts.precondition.not.satisfied)
        c();
        FlowExpressionParsingBug.i = "";
        c();
    }

    void test3b() {
        // :: error: (contracts.precondition.not.satisfied)
        c();
        i = "";
        c();
    }

    void test3c() {
        // :: error: (contracts.precondition.not.satisfied)
        c();
        this.i = "";
        c();
    }
}
