import checkers.units.quals.g;
import checkers.units.quals.kg;
import checkers.units.quals.Mass;

import java.lang.reflect.Method;

import static checkers.units.UnitsTools.*;

public class MethodTest {

    public void pass1() {
        try {
            Class<?> c = Class.forName("MethodTest$SuperClass");
            Method m = c.getMethod("getA", new Class[] {});
            @g Object a = m.invoke(this, (Object[]) null);
        } catch (Exception ignore) {
        }
    }

    public void pass2() {
        String str = "get" + "A";
        try {
            Class<?> c = Class.forName("MethodTest$SuperClass");
            Method m = c.getMethod(str, new Class[] {});
            @g Object a = m.invoke(this, (Object[]) null);
        } catch (Exception ignore) {
        }
    }

    public void pass3() {
        String str = "get";
        str += "A";
        try {
            Class<?> c = Class.forName("MethodTest$SuperClass");
            Method m = c.getMethod(str, new Class[] {});
            //TODO: Should not fail -> enhance Value checker
            //and remove the expected error

            //:: error: (assignment.type.incompatible)
            @g Object a = m.invoke(this, (Object[]) null);
        } catch (Exception ignore) {
        }
    }

    public void pass4() {
        String str = "setA";
        @g int val1 = g;
        @g Integer val2 = val1;
        try {
            Class<?> c = Class.forName("MethodTest$SuperClass");
            Method m = c.getMethod(str, new Class[] { Integer.class });
            m.invoke(this, val1);
            m.invoke(this, val2);
        } catch (Exception ignore) {
        }
    }

    // Test resolution of methods declared in super class
    public void pass5() {
        try {
            Class<?> c = Class.forName("MethodTest$SubClass");
            Method m = c.getMethod("getB", new Class[0]);
            @kg Object o = m.invoke(this, (Object[]) null);
        } catch (Exception ignore) {
        }
    }

    // Test resolution of static methods
    public void pass6() {
        try {
            Class<?> c = MethodTest.class;
            Method m = c
                    .getMethod("convertKg2g", new Class[] { Integer.class });
            @g Object o = m.invoke(null, kg);
        } catch (Exception ignore) {
        }
    }

    // Test primitives
    public void pass7() {
        try {
            Class<?> c = MethodTest.class;
            Method m = c.getMethod("convertKg2g", new Class[] { int.class });
            @g Object o = m.invoke(null, kg);
        } catch (Exception ignore) {
        }
    }


    public void pass8() {
        String str = "setA";
        try {
            Class<?> c = Class.forName("MethodTest$SuperClass");
            Method m = c.getMethod(str, new Class[] { Integer.class });
            m.invoke(this, new Object[] { g });
        } catch (Exception ignore) {
        }
    }

    public void pass9() {
        String str = "getA";
        if (true) {
            str = "getB";
        }
        try {
            Class<?> c = Class.forName("MethodTest$SubClass");
            Method m = c.getMethod(str, new Class[0]);
            @Mass Object o = m.invoke(this, (Object[]) null);
        } catch (Exception ignore) {
        }
    }

    // Test getClass()
    public void pass10() {
        SuperClass inst = new SubClass();
        try {
            Class<?> c = inst.getClass();
            Method m = c.getMethod("getA", new Class[0]);
            @g Object o = m.invoke(inst, (Object[]) null);
        } catch (Exception ignore) {
        }
    }

    public void pass11() {
        try {
            Class<?> c = this.getClass();
            Method m = c
                    .getMethod("convertKg2g", new Class[] { Integer.class });
            @g Object o = m.invoke(null, kg);
        } catch (Exception ignore) {
        }
    }

    // Test .class on inner class
    public void pass12() {
        try {
            Class<?> c = SuperClass.class;
            Method m = c.getMethod("getA", new Class[0]);
            @g Object o = m.invoke(new SuperClass(), new Object[0]);
        } catch (Exception ignore) {
        }
    }

    public void fail1() {
        try {
            Class<?> c = MethodTest.class;
            Method m = c
                    .getMethod("convertKg2g", new Class[] { Integer.class });
            //:: error: (argument.type.incompatible)
            Object o = m.invoke(null, g);
        } catch (Exception ignore) {
        }
    }

    // Test unresolvable methods
    public void fail2(String str) {
        try {
            Class<?> c = Class.forName(str);
            Method m = c.getMethod("getA", new Class[] { Integer.class });
            //:: error: (assignment.type.incompatible)
            @g Object o = m.invoke(this, (Object[]) null);
        } catch (Exception ignore) {
        }
    }

    public void fail3() {
        String str = "setB";
        try {
            Class<?> c = Class.forName("MethodTest$SuperClass");
            Method m = c.getMethod(str, new Class[] { Integer.class });
            //:: error: (argument.type.incompatible)
            m.invoke(this, g);
        } catch (Exception ignore) {
        }
    }

    public void fail4() {
        String str = "setA";
        try {
            Class<?> c = Class.forName("MethodTest$SubClass");
            Method m = c.getMethod(str, new Class[] { Integer.class });
            //:: error: (argument.type.incompatible)
            m.invoke(this, new Object[] { kg });
        } catch (Exception ignore) {
        }
    }

    public void fail5() {
        String str = "setAB";
        try {
            Class<?> c = Class.forName("MethodTest$SubClass");
            Method m = c.getMethod(str, new Class[] { Integer.class,
                    Integer.class });
            //:: error: (argument.type.incompatible)
            m.invoke(this, new Object[] { g, kg });
        } catch (Exception ignore) {
        }
    }

    public void fail6() {
        String str = "setA";
        if (true) {
            str = "setB";
        }
        try {
            Class<?> c = Class.forName("MethodTest$SubClass");
            Method m = c.getMethod(str, new Class[] { Integer.class });
            //:: error: (argument.type.incompatible)
            m.invoke(this, new Object[] { g });
        } catch (Exception ignore) {
        }
    }

    public void fail7() {
        @kg MethodTest inst = new @kg MethodTest(); 
        try {
            Class<?> c = MethodTest.class;
            Method m = c.getMethod("convertKg2g", new Class[]{Integer.class});
            // TODO: The required bottom type for the receiver of a static
            // method might be overly conservative. 
            //:: error: (argument.type.incompatible)
            @g Object o = m.invoke(inst, kg);
        } catch (Exception ignore) {}
    }

    // Test method call that cannot be uniquely resolved
    public void fail8() {
        try {
            Class<?> c = SuperClass.class;
            Method m = c.getMethod("setC", new Class[] { Integer.class });
            //:: error: (argument.type.incompatible)
            Object o = m.invoke(new SuperClass(), new Object[] { kg });
        } catch (Exception ignore) {
        }
    }

    public static @g int convertKg2g(@kg int a) {
        return fromKiloGramToGram(a);
    }

    // TODO: Does the testing framework somehow support the compilation of 
    // multiple files at the same time?
    private class SubClass extends SuperClass {
    }

    private class SuperClass {
        private @g int a;
        private @kg int b;
        private @g Integer c;

        public SuperClass() {
            this.a = g;
            this.b = kg;
        }

        public @g int getA() {
            return a;
        }

        public void setA(@g int a) {
            this.a = a;
        }

        public @kg int getB() {
            return b;
        }

        public void setB(@kg int b) {
            this.b = b;
        }

        public void setAB(@g int a, @kg int b) {
            this.a = a;
            this.b = b;
        }

        public void setC(@g int c) {
            this.c = c;
        }

        public void setC(@g Integer c) {
            this.c = c;
        }
    }
}
