public class Uninit4 {

  class Mam {
    Object a=new Object();
  }

  //:: error: (fields.uninitialized)
  class BadSon {
    Object b;
  }

  class GoodSon {
    Object b=new Object();
  }

  class WeirdSon {
    Object b;

    //:: error: (fields.uninitialized)
    WeirdSon() {
      super();
    }
  }

  class Daughter {
    Object b;

    //:: error: (fields.uninitialized)
    Daughter() {

    }
    Daughter(Object val) {
      this();
      b = val;
    }
  }
}
