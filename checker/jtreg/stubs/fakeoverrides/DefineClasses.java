package fakeoverrides;

public class DefineClasses {}

interface SuperInterface {
    default int m() {
        return 0;
    }
}

class SuperClass implements SuperInterface {
    // fake override:
    // @Untainted int m();
}

interface SubInterface extends SuperInterface {
    // fake override:
    // int m();
}
