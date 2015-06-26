/*
 * This class is solely to set up complicated recursive bounds in order to ensure that the
 * bounds initializer creates bounds with the right structure
 */

interface List<ZZ> {
    ZZ getZZ();
    void setZZ(ZZ ZZ);
}

interface Map<K,V> {
    K getK();
    V getV();

    void setK(K k);
    void setV(V v);
}

class MyRec<E extends List<E>> {

}

class RecList extends MyRec<RecList> implements List<RecList> {
    @SuppressWarnings("return.type.incompatible")
    public RecList getZZ() { return null; }
    public void setZZ(RecList rl) { return; }
}

class Context2 {

    <T extends MyRec<? extends T> & List<T>> void main() {}

    void context() {
        this.<RecList>main();
    }
}

interface Rec<T extends Rec<T>> {}
class MyRec2<E extends Rec<? extends E>> {}

class RecImpl implements Rec<RecImpl> {}
class SubRec extends RecImpl {}


class CrazyGen2<TT extends List<EE>, EE extends Map<TT, TT>> {
    TT t2;
    EE e2;

    public CrazyGen2(TT t2, EE e2) {
        this.t2 = t2;
        this.e2 = e2;
    }

    public void context() {
        t2.setZZ(e2);
        e2.setK(t2.getZZ().getK());
    }
}

class CrazyGen3<TTT extends List<TTT>, EEE extends Map<TTT, TTT>> {
    TTT t3;
    EEE e3;

    public CrazyGen3(TTT t3, EEE e3) {
        this.t3 = t3;
        this.e3 = e3;
    }

    public void context() {
        e3.setK(t3);
        e3.setK(t3.getZZ());
    }

}

class MyClass {

    public <TV1 extends List<TV1>> String methodToPrint(TV1 tv1, int intParam) {
        return "";
    }
}

