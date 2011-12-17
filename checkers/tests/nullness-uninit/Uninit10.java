import checkers.nullness.quals.*;

public class Uninit10 {

  @NonNull String[] strings;

  //:: error: (fields.uninitialized)
  Uninit10() { }

  public class Inner {

    @NonNull String[] stringsInner;

    //:: error: (fields.uninitialized)
    Inner() { }

  }

}

