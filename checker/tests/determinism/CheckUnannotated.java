package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class CheckUnannotated {
    void unannotatedLib() {
        Random rnd = new Random();
        int ret = rnd.nextInt();
    }
}
