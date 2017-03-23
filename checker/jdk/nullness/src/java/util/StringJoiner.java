
package java.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class StringJoiner {
    private final String prefix;
    private final String delimiter;
    private final String suffix;

  
    private StringBuilder value;

    private String emptyValue;

    public StringJoiner(@NonNull CharSequence delimiter) {
        throw new RuntimeException("skeleton method");
    }

    public StringJoiner(@NonNull CharSequence delimiter,
                        @NonNull CharSequence prefix,
                        @NonNull CharSequence suffix) {
        throw new RuntimeException("skeleton method");
    }

    public StringJoiner setEmptyValue(@NonNull CharSequence emptyValue) {
       throw new RuntimeException("skeleton method");
    }

   
    @Override
    public String toString() {
      throw new RuntimeException("skeleton method");
    }

    
    public StringJoiner add(@Nullable CharSequence newElement) {
        throw new RuntimeException("skeleton method");
    }


    public StringJoiner merge(@NonNull StringJoiner other) {
        throw new RuntimeException("skeleton method");
    }

    private StringBuilder prepareBuilder() {
        throw new RuntimeException("skeleton method");
    }

   
    public int length() {
       throw new RuntimeException("skeleton method");
    }
}
