package classes;

import classes.Issue919B.InnerClass;
import java.util.Set;

public class Issue919 {
  private static void method(Set<InnerClass> innerClassSet2) throws Exception {
    Issue919B.otherMethod(innerClassSet2);
  }
}
