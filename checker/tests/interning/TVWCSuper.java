public class TVWCSuper {
   class L<T> {}

   public static <T extends Comparable<? super T>> void sort(L<T> t) {
   }
}
