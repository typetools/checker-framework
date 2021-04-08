/*
 * This class is solely to set up complicated recursive bounds in order to ensure that the
 * bounds initializer creates bounds with the right structure
 */

interface MyList<ZZ> {
  ZZ getZZ();

  void setZZ(ZZ ZZ);
}

interface MyMap<K, V> {
  K getK();

  V getV();

  void setK(K k);

  void setV(V v);
}

class MyRec<E extends MyList<E>> {}

class RecMyList extends MyRec<RecMyList> implements MyList<RecMyList> {
  @SuppressWarnings("return.type.incompatible")
  public RecMyList getZZ() {
    return null;
  }

  public void setZZ(RecMyList rl) {
    return;
  }
}

class Context2 {

  <T extends MyRec<? extends T> & MyList<T>> void main() {}

  void context() {
    this.<RecMyList>main();
  }
}

interface Rec<T extends Rec<T>> {}

class MyRec2<E extends Rec<? extends E>> {}

class RecImpl implements Rec<RecImpl> {}

class SubRec extends RecImpl {}

class CrazyGen2<TT extends MyList<EE>, EE extends MyMap<TT, TT>> {
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

class CrazyGen3<TTT extends MyList<TTT>, EEE extends MyMap<TTT, TTT>> {
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

  public <TV1 extends MyList<TV1>> String methodToPrint(TV1 tv1, int intParam) {
    return "";
  }
}
