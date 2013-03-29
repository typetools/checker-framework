import checkers.nullness.quals.EnsuresNonNull;
import checkers.nullness.quals.EnsuresNonNullIf;
import checkers.nullness.quals.*;

public class AssertWithStatic {
    
    static @Nullable String f;

    @EnsuresNonNullIf(result=true, expression="AssertWithStatic.f")
    public boolean hasSysOut(){
        return AssertWithStatic.f != null;
    }

    @EnsuresNonNullIf(result=false, expression="AssertWithStatic.f")
    public boolean noSysOut(){
        return AssertWithStatic.f == null;
    }

    @EnsuresNonNull("AssertWithStatic.f")
    //:: error: (contracts.postcondition.not.satisfied)
    public void sysOutAfter(){
    }
}
