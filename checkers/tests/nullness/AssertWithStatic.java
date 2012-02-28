import checkers.nullness.quals.*;

public class AssertWithStatic {
    
    @AssertNonNullIfTrue("System.out")
    public boolean hasSysOut(){
        return System.out != null;
    }
    
    @AssertNonNullIfFalse("System.out")
    public boolean noSysOut(){
        return System.out == null;
    }
    
    @AssertNonNullAfter("System.out")
    //:: error: (assert.postcondition.not.satisfied)
    public void sysOutAfter(){
    }
}
