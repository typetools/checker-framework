package org.checkerframework.framework.flow;

/** The default transfer function used in the Checker Framework. */
public class CFTransfer extends CFAbstractTransfer<CFValue, CFStore, CFTransfer> {

  public CFTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    super(analysis);
  }
}
