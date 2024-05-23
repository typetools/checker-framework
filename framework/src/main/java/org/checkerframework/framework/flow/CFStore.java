package org.checkerframework.framework.flow;

import org.plumelib.util.SystemPlume;

/** The default store used in the Checker Framework. */
public class CFStore extends CFAbstractStore<CFValue, CFStore> {

  public CFStore(CFAbstractAnalysis<CFValue, CFStore, ?> analysis, boolean sequentialSemantics) {
    super(analysis, sequentialSemantics);
    SystemPlume.sleep(2);
    System.out.flush();
    new Error(getClassAndUid()).printStackTrace();
    SystemPlume.sleep(2);
    System.out.flush();
  }

  /**
   * Copy constructor.
   *
   * @param other the CFStore to copy
   */
  public CFStore(CFAbstractStore<CFValue, CFStore> other) {
    super(other);
    SystemPlume.sleep(2);
    System.out.flush();
    new Error(getClassAndUid()).printStackTrace();
    SystemPlume.sleep(2);
    System.out.flush();
  }
}
