package testpkg;

class Issue3597A {
    void f() {
        System.err.println(new Issue3597B().f().toString());
    }
}
