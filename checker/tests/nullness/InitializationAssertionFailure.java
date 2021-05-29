import java.io.*;

// An assertion failure occurred after CFAbstractTransfer.initialStore inserted
// a FieldAccess for serialVersionUID with the type InitializationAssertionFailure.

public class InitializationAssertionFailure implements Serializable {

  static final long serialVersionUID = 20030819L;

  private InitializationAssertionFailure() {}
}
