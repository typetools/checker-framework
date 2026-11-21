package ajavatestarchive;

import org.checkerframework.framework.testchecker.util.Odd;

// The ajava file for this class, which is read from an archive, writes `@Odd` on the return type
// and formal parameter of method `id` in an anonymous class.  Only tree-aware parsing of the ajava
// file for the compilation unit being checked reads annotations within an anonymous class, so the
// assignment in `check` type-checks only if that parsing reads the archive entry.

public class AjavaArchive {
  void use(@Odd String p) {
    Object o =
        new Object() {
          String id(String s) {
            return s;
          }

          void check() {
            @Odd String x = id(p);
          }
        };
  }
}
