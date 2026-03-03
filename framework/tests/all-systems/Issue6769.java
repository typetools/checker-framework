package typearginfer;

import java.util.*;
import java.util.stream.*;

abstract class Issue6769 {
  void test(Stream<? extends BasicBlock<?>> blocks) {
    blocks.map(BasicBlock::getStmts);
  }

  interface BasicBlock<V extends BasicBlock<V>> {
    List<String> getStmts();
  }
}
