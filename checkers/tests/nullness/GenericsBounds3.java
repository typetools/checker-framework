import checkers.nullness.quals.*;

public class GenericsBounds3 {
  class Sup<X extends @NonNull Object> {}

  //:: (generic.argument.invalid)
  class Sub extends Sup<@Nullable Object> {}
  
  class SubGood extends Sup<@NonNull Object> {}
  
  interface ISup<X extends @NonNull Object> {}
  
  //:: (generic.argument.invalid)
  class ISub implements ISup<@Nullable Object> {}
  
  class ISubGood implements ISup<@NonNull Object> {}

  //:: (generic.argument.invalid)
  class ISub2 extends Sup<Object> implements java.io.Serializable, ISup<@Nullable Object> {}
}
