
package java.util;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class StringJoiner {
  
    public StringJoiner(CharSequence delimiter) {
        throw new RuntimeException("skeleton method");
    }

    public StringJoiner(CharSequence delimiter,
                        CharSequence prefix,
                        CharSequence suffix) {
        throw new RuntimeException("skeleton method");
    }

    public StringJoiner setEmptyValue(CharSequence emptyValue) {
       throw new RuntimeException("skeleton method");
    }

   
    @Override
    public String toString() {
      throw new RuntimeException("skeleton method");
    }

    
    public StringJoiner add(@Nullable CharSequence newElement) {
        throw new RuntimeException("skeleton method");
    }


    public StringJoiner merge(StringJoiner other) {
        throw new RuntimeException("skeleton method");
    }

    private StringBuilder prepareBuilder() {
        throw new RuntimeException("skeleton method");
    }

   
    public int length() {
       throw new RuntimeException("skeleton method");
    }
}
