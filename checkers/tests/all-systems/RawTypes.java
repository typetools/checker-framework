import java.util.List;

class RawTypes {
    public void m(ClassLoader cl) throws ClassNotFoundException {
        Class clazz = cl.loadClass("java.lang.Object");
    }
}

interface I {
    public void m(List<? extends String> l);
}

class C implements I {
    public void m(List l) {}
}