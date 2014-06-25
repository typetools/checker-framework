import org.checkerframework.common.value.qual.*;

class ExceptionTest{
    public @StaticallyExecutable static int div(int a)  {
        return 2 / a;
    }

    public @StaticallyExecutable static int add(int a) {
        return a + 2;
    }

    public void foo(){
        int a = 0;
        //:: warning: (method.evaluation.exception)
        div(a);
        
        @IntVal({2}) int c = add(a);

    }
    
}