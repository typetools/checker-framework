import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.KeyFor;

class Issue414 {

  void simple(String s) {
    Map<String,Integer> mymap = new HashMap<String,Integer>();
    mymap.put(s, 1);
    @KeyFor("mymap") String s2 = s;
  }


  Map<String,Integer> someField = new HashMap<String,Integer>();

  void semiSimple(/*@KeyFor("this.someField")*/ String s) {
    Map<String,Integer> mymap = new HashMap<String,Integer>();
    mymap.put(s, 1);
    /*@KeyFor({"this.someField","mymap"})*/ String s2 = s;
  }


  void dominatorsNoGenerics(Map<String,Integer> preds) {

    Map<String,Integer> dom = new HashMap<String,Integer>();
    /*@KeyFor({"preds","dom"})*/ String root;

    List</*@KeyFor({"preds","dom"})*/ String> roots = new ArrayList<String>();


    for (String node : preds.keySet()) {
      dom.put(node, 1);
      root = node;
      roots.add(node);
    }
  }


  <T> void dominators(Map<T,List<T>> preds) {

    Map<T,Integer> dom = new HashMap<T,Integer>();

    /*@KeyFor({"preds","dom"})*/ T root;

    List</*@KeyFor({"preds","dom"})*/ T> roots = new ArrayList<T>();


    for (T node : preds.keySet()) {
      dom.put(node, 1);
      root = node;
      roots.add(node);
    }
  }
}
