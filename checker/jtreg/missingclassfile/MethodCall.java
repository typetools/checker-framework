import lib.Factory;

/**
 * A method-call chain whose receiver type is a classpath class whose supertype has a type argument
 * whose class file is absent. This is the shape of Beam's {@code
 * DataStoreV1TableProvider#getTableStatistics}. See Issue8055.java.
 */
public class MethodCall {

  long callChain() {
    return Factory.make().self().size();
  }
}
