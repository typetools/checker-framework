import checkers.nullness.quals.*;

public class Uninit10 {

  @NonNull String[] strings;

  //:: warning: (fields.uninitialized)
  Uninit10() { }

  public class Inner {

    @NonNull String[] stringsInner;

    //:: warning: (fields.uninitialized)
    Inner() { }

  }

}

