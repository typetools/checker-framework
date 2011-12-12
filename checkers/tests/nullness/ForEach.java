import java.util.*;
import checkers.nullness.quals.*;

public class ForEach {
  void m1() {
    Set<? extends CharSequence> s = new HashSet<CharSequence>( );
    for( CharSequence cs : s ) {
      System.out.println( cs.length() );
    }
  }

  void m2() {
    Set<CharSequence> s = new HashSet<CharSequence>( );
    for( CharSequence cs : s ) {
      System.out.println( cs.length() );
    }
  }

  <T extends @NonNull Object> void m3(T p) {
    Set<T> s = new HashSet<T>( );
    for( T cs : s ) {
      System.out.println( cs.toString() );
    }
  }

  <T extends @NonNull Object> void m4(T p) {
    Set<T> s = new HashSet<T>( );
    for( Object cs : s ) {
      System.out.println( cs.toString() );
    }
  }
}
