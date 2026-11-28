// Distilled from `StringsP.unescapeJava` in plume-util.
// The Upper Bound Checker represents `@LTLengthOf("orig")` canonically, without an explicit
// offset of 0.  If an explicit offset of 0 appeared in some values but not in others, then values
// with the same meaning would differ, and dataflow analysis of the outer loop would iterate enough
// to widen the type of `iii` to `@UpperBoundUnknown`.

import org.checkerframework.checker.index.qual.LTEqLengthOf;

public class LoopInSwitchInLoop {
  public static void unescapeJava(String orig) {
    @LTEqLengthOf("orig") int postEsc = 0;
    int thisEsc = orig.indexOf('\\');
    while (thisEsc != -1) {
      if (thisEsc == orig.length() - 1) {
        orig.substring(postEsc, thisEsc + 1);
        postEsc = thisEsc + 1;
        break;
      }
      switch (orig.charAt(thisEsc + 1)) {
        case 'b' -> {
          orig.substring(postEsc, thisEsc);
          postEsc = thisEsc + 2;
        }
        case 'f' -> {
          orig.substring(postEsc, thisEsc);
          postEsc = thisEsc + 2;
        }
        case 'n' -> {
          orig.substring(postEsc, thisEsc);
          postEsc = thisEsc + 2;
        }
        case 'r' -> {
          orig.substring(postEsc, thisEsc);
          postEsc = thisEsc + 2;
        }
        case 't' -> {
          orig.substring(postEsc, thisEsc);
          postEsc = thisEsc + 2;
        }
        case 'u' -> {
          orig.substring(postEsc, thisEsc);
          char octalChar = 0;
          int iii = thisEsc + 1;
          while (iii < Math.min(thisEsc + 4, orig.length())) {
            int thisDigit = Character.digit(orig.charAt(iii), 8);
            if (thisDigit == -1) {
              break;
            }
            int newValue = (octalChar * 8) + thisDigit;
            if (newValue > 0xFF) {
              break;
            }
            octalChar = (char) newValue;
            iii++;
          }
          postEsc = iii;
        }
        default -> {
          orig.substring(postEsc, thisEsc);
          postEsc = thisEsc + 1;
        }
      }
      thisEsc = orig.indexOf('\\', postEsc);
    }
  }
}
