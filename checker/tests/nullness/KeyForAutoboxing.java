// Test case for issue #595:
// https://github.com/typetools/checker-framework/issues/595

// @skip-test until the issue is fixed

import java.util.Map;

public abstract class KeyForAutoboxing {

  public void working1(Object key, Map<Object, Object> m) {
    if (!m.containsKey(key)) {
      m.put(key, new Object());
    }
    m.get(key).toString();
  }

  public void working2(Integer key, Map<Integer, Object> m) {
    if (!m.containsKey(key)) {
      m.put(key, new Object());
    }
    m.get(key).toString();
  }

  public void working3(Double key, Map<Double, Object> m) {
    if (!m.containsKey(key)) {
      m.put(key, new Object());
    }
    m.get(key).toString();
  }

  public void notWorking1(int key, Map<Integer, Object> m) {
    if (!m.containsKey(key)) {
      m.put(key, new Object());
    }
    m.get(key).toString(); // Should not generate error but does
  }

  public void notWorking2(double key, Map<Double, Object> m) {
    if (!m.containsKey(key)) {
      m.put(key, new Object());
    }
    m.get(key).toString(); // Should not generate error but does
  }

  public void notWorking3(double key, Map<Double, Object> m) {
    if (m.containsKey(key)) {
      m.get(key).toString(); // Should not generate error but does
    }
  }

  public void notWorking4(double key, Map<Double, Object> m) {
    if (m.get(key) != null) {
      m.get(key).toString(); // Should not generate error but does
    }
  }

  public void notWorking5(double key, Map<Double, Object> m) {
    if (m.get(Double.valueOf(key)) != null) {
      m.get(Double.valueOf(key)).toString(); // Should not generate error but does
    }
  }
}
