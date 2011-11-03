import checkers.nullness.quals.*;

class UnannoPrimitives {
  //:: error: (type.invalid)
  @Nullable int f;

  //:: error: (type.invalid)
  @NonNull int g;
}
