public class Uninit4 {

  class Mam {
    Object a=new Object();
  }

  //:: error: (commitment.fields.uninitialized)
  class BadSon {
    Object b;
  }

  class GoodSon {
    Object b=new Object();
  }

  class WeirdSon {
    Object b;

    //:: error: (commitment.fields.uninitialized)
    WeirdSon() {
      super();
    }
  }

  class Daughter {
    Object b;

    //:: error: (commitment.fields.uninitialized)
    Daughter() {

    }
    Daughter(Object val) {
      this();
      b = val;
    }
  }
}
