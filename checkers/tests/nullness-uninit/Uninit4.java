public class Uninit4 {

  class Mam {
    Object a=new Object();
  }

  //:: warning: (fields.uninitialized)
  class BadSon {
    Object b;
  }

  class GoodSon {
    Object b=new Object();
  }

  class WeirdSon {
    Object b;

    //:: warning: (fields.uninitialized)
    WeirdSon() {
      super();
    }
  }

  class Daughter {
    Object b;

    //:: warning: (fields.uninitialized)
    Daughter() {

    }
    Daughter(Object val) {
      this();
      b = val;
    }
  }
}
