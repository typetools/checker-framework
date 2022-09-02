final class Issue5256 {
    char c;

    public int foo1() {
        int x = 1;
        x = x + (int) c;
        return x;
    }

    public int foo2() {
        char c = 65535;
        int x = 1;
        x = x + (int) c;
        return x;
    }
}
