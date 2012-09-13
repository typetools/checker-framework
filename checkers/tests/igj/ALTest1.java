import checkers.igj.quals.*;
public final class ALTest1 {
   public static void main(String[] args) {
     //:: error: (constructor.invocation.invalid)
     final @Immutable String ref4 = new @Immutable String();
   }
}
