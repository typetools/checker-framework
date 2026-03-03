import java.lang.annotation.*;
import java.util.*;

@Target(ElementType.TYPE_USE)
@interface X {}

@Target(ElementType.TYPE_USE)
@interface Y {}

public class GenericCellDoubled {
  List<Object> f;
  @X List<Object> g;
  @X List<@X ArrayList<Object>> h;
  @X List<ArrayList<@X Object>> i;

  Map<List<Object>, ArrayList<Integer>> j1;
  @X Map<List<@X Object>, @X ArrayList<Integer>> j2;
  @X Map<@X List<@X Object>, @X ArrayList<Integer>> j3;

  @X List k;
}
