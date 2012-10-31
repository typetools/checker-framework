import javax.swing.JMenuBar;

import checkers.nonnull.quals.Nullable;
import checkers.nonnull.quals.RequiresNonNull;


public abstract class FlowExpressionParsingBug {
   
    protected @Nullable JMenuBar menuBar = null;

    @RequiresNonNull("menuBar")
    public void addFavorite() {
    }
    
    @RequiresNonNull("this.menuBar")
    public void addFavorite1() {
    }
    
    static @Nullable String i = null;

    @RequiresNonNull("FlowExpressionParsingBug.i")
    public void a() {
    }
    
    @RequiresNonNull("i")
    public void b() {
    }
    
    void test1() {
        //:: error: (contracts.precondition.not.satisfied)
        a();
        FlowExpressionParsingBug.i = "";
        a();
        
        //:: error: (contracts.precondition.not.satisfied)
        a();
        i = "";
        a();
    }
    
    void test2() {
        //:: error: (contracts.precondition.not.satisfied)
        b();
        FlowExpressionParsingBug.i = "";
        b();
        
        //:: error: (contracts.precondition.not.satisfied)
        b();
        i = "";
        b();
    }
}
