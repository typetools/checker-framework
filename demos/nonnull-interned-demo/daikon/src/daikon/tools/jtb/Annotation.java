package daikon.tools.jtb;

import java.util.*;
import java.util.regex.*;

/**
 * Utility class to parse annotations generated with the Annotate program
 * using --wrap_xml flag.
 *
 * An example of the String representation of an annotation, as output
 * with the --wrap_xml flag, is:
 *
 *  <INVINFO>
 *  <INV> this.topOfStack <= this.theArray.length-1 </INV>
 *  <ENTER>
 *  <DAIKON>  this.topOfStack <= size(this.theArray[])-1  </DAIKON>
 *  <DAIKONCLASS>class daikon.inv.binary.twoScalar.IntLessEqual</DAIKONCLASS>
 *  <METHOD>  isEmpty()  </METHOD>
 *  </INVINFO>
 *
 * The above string should actually span only one line.
 *
 * To be well-formed, an annotation should be enclosed in <INVINFO>
 * tags, contain
 *
 *   <DAIKON> and
 *   <METHOD> tags,
 *
 * and exactly one of
 *
 *   <ENTER>,
 *   <EXIT>,
 *   <OBJECT>, or
 *   <CLASS>.
 *
 * Obviously, the tool Annotate outputs well-formed annotations, so
 * the user shouldn't have to worry too much about well-formedness.
 *
 * Two annotations are equal iff their fields "daikonRep", "method"
 * and "kind" are equal.
 *
 * The factory method get(String annoString) returns an annotation
 * that equals to the annotation represented by annoString. In
 * particular, if the same String is given twice to get(String
 * annoString), the method will return the same Annotation object.
 */
public class Annotation {

  /**
   * <p>Parse a String and return the annotation that it represents.
   *
   */
  // [[ Note: Using an XML parser seems like too strong a hammer here,
  // and the performance of string matching is not an obvious
  // bottleneck. ]]
  public static Annotation get(String annoString)
    throws Annotation.MalformedAnnotationException {

    // check well-formedness
    boolean wellformed = true;
    if (!(annoString.matches(".*<INVINFO>.*</INVINFO>.*")
          && annoString.matches(".*<DAIKON>(.*)</DAIKON>.*")
          && annoString.matches(".*<METHOD>(.*)</METHOD>.*"))) {
      throw new Annotation.MalformedAnnotationException(annoString);
    }

    // figure out what kind of annotation it is
    Kind k = null;
    if (annoString.matches(".*<ENTER>.*")) {
      k = Kind.enter;
    } else if (annoString.matches(".*<EXIT>.*")) {
      k = Kind.exit;
    } else if (
               annoString.matches(".*<OBJECT>.*")
               || annoString.matches(".*<CLASS>.*")) {
      k = Kind.objectInvariant;
    } else {
      throw new Annotation.MalformedAnnotationException(annoString);
    }

    String theDaikonRep =
      annoString.replaceFirst(".*<DAIKON>(.*)</DAIKON>.*", "$1").trim();
    String theMethod =
      annoString.replaceFirst(".*<METHOD>(.*)</METHOD>.*", "$1").trim();

    Annotation anno = Annotation.get(k, theDaikonRep, theMethod);

    if (annoString.matches(".*<INV>(.*)</INV>.*")) {
      anno.invRep = annoString.replaceFirst(".*<INV>(.*)</INV>.*", "$1").trim();
    }

    if (annoString.matches(".*<DAIKONCLASS>(.*)</DAIKONCLASS>.*")) {
      anno.daikonClass =
        annoString
        .replaceFirst(".*<DAIKONCLASS>(.*)</DAIKONCLASS>.*", "$1")
        .trim();
    }

    return anno;
  }

  /**
   * Thrown by method get(String annoString) if annoString doesn't
   * represent a well-formed annotation.
   */
  public static class MalformedAnnotationException extends Exception {
  static final long serialVersionUID = 20050923L;

    public MalformedAnnotationException(String s) {
      super(s);
    }
  }

  /**
   * XML representation. May be different from the String used to
   * generate the input; only those tags that were parsed by the
   * get() method will be output here.
   */
  public String xmlString() {
    return "<INVINFO> "
      + kind.xmlString()
      + "<DAIKON>"
      + daikonRep
      + " </DAIKON> "
      + "<METHOD> "
      + method
      + " </METHOD>"
      + "<INV>"
      + invRep
      + "</INV>"
      + " <DAIKONCLASS>"
      + daikonClass
      + " </DAIKONCLASS>"
      + "</INVINFO>";
  }

  /**
   * Find, parse and return all distinct annotations found in a
   * String.  The string <code>annoString</code> may contain none, one,
   * or several annotations.  Malformed annotations are ignored.
   */
  public static Annotation[] findAnnotations(String annoString) {
    List<String> l = new ArrayList<String>();
    l.add(annoString);
    return findAnnotations(l);
  }

  /**
   * Find, parse and return all distinct annotations found in a list
   * of strings.  Each string in <code>annoString</code> may contain
   * none, one, or several annotations.  Malformed annotations are
   * ignored.
   */
  public static Annotation[] findAnnotations(List<String> annoStrings) {

    if (annoStrings == null) {
      return new Annotation[] {
      };
    }
    //Pattern p = Pattern.compile("(<INVINFO>.*</INVINFO>)");
    Set<Annotation> annos = new HashSet<Annotation>();
    for (String location : annoStrings) {
      if (location == null || location.equals("")) {
        continue;
      }
      String[] cutUp = location.split("<INVINFO>");
      //Matcher m = p.matcher(location);
      for (int splits = 0; splits < cutUp.length; splits++) {
        //while (m.find()) {
        try {
          //String s = m.group(1);
          String s = cutUp[splits];
          Annotation anno = Annotation.get("<INVINFO>" + s);
          // [[[ explain this! ]]]
          annos.add(anno);
        } catch (Exception e) {
          // malformed annotation; just go to next iteration
        }
      }

    }
    return annos.toArray(new Annotation[] {
    });
  }

  /**
   * Daikon representation (as output by Daikon's default output format).
   */
  private final String daikonRep;

  /**
   *  The way this annotation would be printed by Daikon.
   */
  public String daikonRep() {
    return daikonRep;
  }

  private final String method;

  /**
   *  The method that this annotation refers to.
   */
  public String method() {
    return method;
  }

  private final Kind kind;

  /**
   * The kind of this annotation.
   */
  public Kind kind() {
    return kind;
  }

  /**
   * <p> A class representing the kind of an annotation. An invariant
   * is either <code>Kind.enter</code>, <code>Kind.exit</code>, or
   * <code>Kind.objectInvariant</code>
   *
   * For well-formed Annotations, the following holds:
   *
   *    a.kind == Kind.enter
   * || a.kind == Kind.exit
   * || a.kind == Kind.objectInvariant
   *
   */
  public static class Kind {
    public final String name;
    public final String xmlname;
    private Kind(String name, String xmlname) {
      this.name = name;
      this.xmlname = xmlname;
    }
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }
      if (!(o instanceof Kind)) {
        return false;
      }
      return name.equals(((Kind) o).name);
    }
    public int hashCode() {
      return name.hashCode();
    }
    public String toString() {
      return name;
    }
    public String xmlString() {
      return xmlname;
    }
    public static final Kind enter = new Kind("precondition ", "<ENTER>");
    public static final Kind exit = new Kind("postcondition", "<EXIT>");
    public static final Kind objectInvariant = new Kind("obj invariant", "<OBJECT>");
  }

  private String invRep;

  /**
   * Representation of this annotation (the format depends on
   * which output format was used to create the annotation in Daikon; it's
   * one of JAVA, JML, ESC or DBC.
   *
   */
  public String invRep() {
    return invRep;
  }

  public String daikonClass;

  /**
   * The Daikon class name that this invariant represents an instance of.
   */
  public String daikonClass() {
    return daikonClass;
  }

  /**
   * Easy-on-the-eye format.
   */
  public String toString() {
    return kind.toString()
      + " : "
      + daikonRep();
  }

  /**
   * Two annotations are equal iff their fields "daikonRep", "method"
   * and "kind" are equal.
   */
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof Annotation)) {
      return false;
    }
    Annotation anno = (Annotation) o;
    return (
            this.daikonRep().equals(anno.daikonRep())
            && (this.method() == anno.method())
            && (this.kind() == anno.kind()));
  }

  public int hashCode() {
    return daikonRep.hashCode() + kind.hashCode() + method.hashCode();
  }


  // Maps into all the Annotation objects created.
  private static HashMap<Integer,Annotation>
    annotationsMap = new HashMap<Integer,Annotation>();

  private Annotation(Kind kind, String daikonRep, String method) {
    this.kind = kind;
    this.daikonRep = daikonRep;
    this.method = method;
  }

  /**
   * <p>Get the annotation with corresponding properties.
   *
   */
  public static Annotation get(Kind kind, String daikonRep, String method)
    throws Annotation.MalformedAnnotationException {

    Annotation anno = new Annotation(kind, daikonRep, method);
    if (annotationsMap.containsKey(new Integer(anno.hashCode()))) {
      return annotationsMap.get(new Integer(anno.hashCode()));
    } else {
      annotationsMap.put(new Integer(anno.hashCode()), anno);
      return anno;
    }
  }


  /**
   * The annotations in <code>annas</code> of kind <code>kind</code>.
   */
  public static Annotation[] getKind(Annotation[] annas, Kind kind) {
    List<Annotation> retval = new ArrayList<Annotation>();
    for (int i = 0; i < annas.length; i++) {
      if (kind == annas[i].kind) {
        retval.add(annas[i]);
      }
      break;
    }
    return retval.toArray(new Annotation[] {
    });
  }

}
