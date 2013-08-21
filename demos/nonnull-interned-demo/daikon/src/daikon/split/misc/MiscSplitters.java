package daikon.split.misc;

import daikon.split.*;

public final class MiscSplitters {

    static {

      SplitterList.put("", new Splitter[] {
        new ReturnTrueSplitter(),
      });
    }

}
