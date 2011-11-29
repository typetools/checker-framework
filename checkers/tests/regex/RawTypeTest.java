import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Stack;

import checkers.regex.quals.*;

class RawTypeTest {

    public void m1(Class<?> c) {
        new WeakReference<Class<? extends I2>>(c.asSubclass(I2.class));
    }
    
    public void m2(Class<Object> c) {}
    
    public void m3(Class c) {
        m2(c);
    }
  
    public void m4() {
        AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            return null;
        }});
    }
    
    public void m5(List list, C4 c) {
        list.add(c);
    }
    
    public void m6(List list, long l) {
        list.add(l);
    }
    
    class MyList<X extends @Regex String> {
        X f;
    }

    /* TODO: implement annotations on wildcard bounds
    interface I1 {
        public void m(MyList<? extends @Regex String> l);
    }

    class C1 implements I1 {
        public void m(MyList par) {
            @Regex String xxx = par.f;
        }
    }*/

    interface I2 {
        public void m(MyList<@Regex String> l);
    }

    class C2 implements I2 {
        public void m(MyList<@Regex String> l) {}
    }

    class C3 implements I2 {
        //:: error: (override.param.invalid) :: error: (generic.argument.invalid)
        public void m(MyList<String> l) {}
    }
    
    class C4 {}
}
