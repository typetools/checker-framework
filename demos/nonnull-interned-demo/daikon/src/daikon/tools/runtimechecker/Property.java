package daikon.tools.runtimechecker;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import utilMDE.Assert;

/**
 * A program property (currently, derived by Daikon).
 */
public class Property implements Serializable {

    private static final long serialVersionUID = 1L;


    // The name of the method that this property describes.
    private final String method;

    // The kind of proerty (enter, exit or objectInvariant).
    private final Kind kind;

    /**
     * The name of the method that this property describes. ("null" for object
     * invariants).
     */
    public String method() {
        return method;
    }

    /**
     * The kind of property (enter, exit or objectInvariant).
     */
    public Kind kind() {
        return kind;
    }

    /**
     * The set of property that this belongs to.
     */
    //public static PropertiesForClass properties = null;

    /**
     * Daikon representation (as output by Daikon's default output format).
     */
    private final String daikonRep;

    /**
     * Daikon representation (as output by Daikon's default output format).
     */
    public String daikonRep() {
        return daikonRep;
    }

    /**
     * <p>
     * A class representing the kind of an property. An invariant is either
     * <code>Kind.enter</code>,<code>Kind.exit</code>, or
     * <code>Kind.objectInvariant</code>
     */
    public static class Kind implements Serializable {
        private static final long serialVersionUID = 1L;

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

        public static final Kind objectInvariant = new Kind("obj invariant",
                                                            "<OBJECT>");

        public static final Kind classInvariant = new Kind("class invariant",
                                                            "<CLASS>");

        // See documentation for Serializable.
        private Object readResolve() throws ObjectStreamException {
            if (name.equals("precondition ")) {
                return enter;
            } else if (name.equals("postcondition")) {
                return exit;
            } else if (name.equals("class invariant")) {
                return classInvariant;
            } else {
                Assert.assertTrue(name.equals("obj invariant"));
                return objectInvariant;
            }
        }

    }

    /**
     * JML representation of this property.
     */
    public String invRep;

    /**
     * <p>
     * A measure of a property's universality: whether it captures the general
     * behavior of the program. The measure ranges from 0 (no confidence) to 1
     * (high confidence).
     */
    public double confidence;

    /**
     * The Daikon class name that this property represents.
     */
    public String daikonClass;

    /**
     * Easy-on-the-eye string representation.
     */
    public String toString() {
        return kind.toString() + " : " + daikonRep();
    }

    /**
     * <p>
     * Two properties are equal if their fields <code>daikonRep</code>,
     * <code>method</code> and <code>kind</code> are equal.
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof Property)) {
            return false;
        }
        Property anno = (Property) o;
        return (this.daikonRep().equals(anno.daikonRep())
                && (this.method().equals(anno.method()))
                && (this.kind().equals(anno.kind())));
    }

    public int hashCode() {
        return daikonRep.hashCode() + kind.hashCode() + method.hashCode();
    }

    /**
     * <p>
     * Parse a String and return the property that it represents. An example of
     * the String representation of an property is:
     *
     * <pre>
     *
     *
     *
     *     &lt;INVINFO&gt;
     *     &lt;INV&gt; this.topOfStack &lt;= this.theArray.length-1 &lt;/INV&gt;
     *     &lt;ENTER&gt;
     *     &lt;DAIKON&gt;  this.topOfStack &lt;= size(this.theArray[])-1  &lt;/DAIKON&gt;
     *     &lt;DAIKONCLASS&gt;class daikon.inv.binary.twoScalar.IntLessEqual&lt;/DAIKONCLASS&gt;
     *     &lt;METHOD&gt;  isEmpty()  &lt;/METHOD&gt;
     *     &lt;/INVINFO&gt;
     *
     *
     *
     * </pre>
     *
     * <p>
     * The above string should actually span only one line.
     *
     * <p>
     * To be well-formed, an property should be enclosed in
     * <code><INVINFO</code> tags, contain <code><DAIKON></code> and
     * <code><METHOD></code> tags, and exactly one of <code><ENTER></code>,
     * <code><EXIT></code>,<code><OBJECT></code>, or <code><CLASS></code>.
     */
    // [[ Using an XML parser seems like too strong a hammer here.
    //    But should do some profiling to see if all the string
    //    matching slows things sufficiently for us to want to optimize. ]]
    public static Property get(String annoString)
            throws MalformedPropertyException {

        // check well-formedness
        boolean wellformed = true;
        if (!(annoString.matches(".*<INVINFO>.*</INVINFO>.*")
                && annoString.matches(".*<DAIKON>(.*)</DAIKON>.*") && annoString
                .matches(".*<METHOD>(.*)</METHOD>.*"))) {
            throw new MalformedPropertyException(annoString);
        }

        // figure out what kind of property it is
        Kind k = null;
        if (annoString.matches(".*<ENTER>.*")) {
            k = Kind.enter;
        } else if (annoString.matches(".*<EXIT>.*")) {
            k = Kind.exit;
        } else if (annoString.matches(".*<OBJECT>.*")) {
            k = Kind.objectInvariant;
        } else if (annoString.matches(".*<CLASS>.*")) {
            k = Kind.classInvariant;
        } else {
            throw new MalformedPropertyException(annoString);
        }

        String theDaikonRep = annoString.replaceFirst(
                ".*<DAIKON>(.*)</DAIKON>.*", "$1").trim();
        String theMethod = annoString.replaceFirst(".*<METHOD>(.*)</METHOD>.*",
                "$1").trim();

        Property anno = Property.get(k, theDaikonRep, theMethod);

        if (annoString.matches(".*<INV>(.*)</INV>.*")) {
            anno.invRep = annoString.replaceFirst(".*<INV>(.*)</INV>.*", "$1")
                    .trim();
        }

        if (annoString.matches(".*<DAIKONCLASS>(.*)</DAIKONCLASS>.*")) {
            anno.daikonClass = annoString.replaceFirst(
                    ".*<DAIKONCLASS>(.*)</DAIKONCLASS>.*", "$1").trim();
        }
        if (annoString.matches(".*<CONFIDENCE>(.*)</CONFIDENCE>.*")) {
            anno.confidence = Double.parseDouble(annoString.replaceFirst(
                    ".*<CONFIDENCE>(.*)</CONFIDENCE>.*", "$1").trim());
        } else {
            anno.confidence = anno.calculateConfidence();
        }
        return anno;
    }

    /**
     * XML representation. May be diferent from the String used to parse the
     * property; only those tags that were parsed by the get() method will be
     * output here.
     */
    public String xmlString() {
        return "<INVINFO> " + kind.xmlString() + "<DAIKON>" + daikonRep
                + " </DAIKON> " + "<METHOD> " + method + " </METHOD>" + "<INV>"
                + invRep + "</INV>" + " <CONFIDENCE>" + confidence
                + " </CONFIDENCE>" + " <DAIKONCLASS>" + daikonClass
                + " </DAIKONCLASS>" + "</INVINFO>";
    }

    /**
     * Similar to <code>xmlString()</code>, but without a
     * <code><INV>...</INV></code> tag (the JML representation).
     *
     * Invariant:
     * <p>
     * <code>this.equals(Property.get(this.xmlStringNoJml())</code>
     */
    public String xmlStringNoJml() {
        return "<INVINFO> " + kind.xmlString() + "<DAIKON>" + daikonRep
                + " </DAIKON> " + "<METHOD> " + method + " </METHOD>"
                + " <CONFIDENCE>" + confidence + " </CONFIDENCE>"
                + " <DAIKONCLASS>" + daikonClass + " </DAIKONCLASS>"
                + "</INVINFO>";
    }

    /**
     * Find, parse and return all well-formed XML String representing
     * properties. String. The string <code>annoString</code> may contain
     * none, one, or several properties. Ignores malformed properties.
     */
    public static Property[] findProperties(String annoString) {
        List<String> l = new ArrayList<String>();
        l.add(annoString);
        return findProperties(l);
    }

    /**
     * Find, parse and return all distinct properties found in a list of
     * strings. Each string in <code>annoStrings</code> may contain none, one,
     * or several properties. Malformed properties are ignored.
     */
    public static Property[] findProperties(List<String> annoStrings) {

        if (annoStrings == null) {
            return new Property[] {};
        }
        //Pattern p = Pattern.compile("(<INVINFO>.*</INVINFO>)");
        Set<Property> annos = new HashSet<Property>();
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
                    Property anno = Property.get("<INVINFO>" + s);
                    // [[[ explain this! ]]]
                    annos.add(anno);
                } catch (Exception e) {
                    // malformed property; just go to next iteration
                }
            }

        }
        return annos.toArray(new Property[] {});
    }

    // Maps into all the Property objects created.
    private static HashMap<Integer, Property> propertiesMap = new HashMap<Integer, Property>();

    // Creates a new property with the given attributes.
    private Property(Kind kind, String daikonRep, String method) {
        this.kind = kind;
        this.daikonRep = daikonRep;
        this.method = method;
    }

    /**
     * <p>
     * Get the property with the given attributes.
     */
    private static Property get(Kind kind, String daikonRep, String method)
            throws MalformedPropertyException {

        Property anno = new Property(kind, daikonRep, method);
        if (propertiesMap.containsKey(new Integer(anno.hashCode()))) {
            return propertiesMap.get(new Integer(anno.hashCode()));
        } else {
            propertiesMap.put(new Integer(anno.hashCode()), anno);
            return anno;
        }
    }

    /**
     * The properties in <code>annas</code> with the given kind.
     */
    public static Property[] getKind(Property[] annas, Kind kind) {
        List<Property> retval = new ArrayList<Property>();
        for (int i = 0; i < annas.length; i++) {
            if (kind == annas[i].kind) {
                retval.add(annas[i]);
            }
            break;
        }
        return retval.toArray(new Property[] {});
    }

    /**
     * <p>
     * A heuristic technique that takes into account several factors when
     * calculating the confidence of an property, among them:
     *
     * <ul>
     * <li>The values of <code>property.kind()</code>.
     * <li>The "Daikon class" of the property.
     * <li>Whether the property relates old and new state.
     * <li>The total number of properties in the method containing this
     * property.
     * <ul>
     */
    // [[ A work in progress ]]
    public double calculateConfidence() {

        double ret = 0;

        // [[[ why the weird numbers like 0,89 and 0.91? Purely for
        // debugging purposes -- upon seeing a confidence number
        // assigned, I can tell why it was assigned. And as long as it's
        // above the confidence threshold (by default 0.5), 0.89 is treated
        // the same as 0.91. ]]]

        // Order is important in the following statements.

        // These types of invariants often seem to capture true properties
        if ((daikonClass != null)
                && ((this.daikonClass
                        .indexOf("daikon.inv.unary.scalar.NonZero") != -1) || (this.daikonClass
                        .indexOf("daikon.inv.unary.scalar.LowerBound") != -1))) {
            ret = 0.91;

            // These types of invariants often seem NOT to capture generally
            // true properties
        } else if ((daikonClass != null)
                && ((this.daikonClass.indexOf("SeqIndex") != -1)
                    || (this.daikonClass.indexOf("EltOneOf") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.unary.sequence.NoDuplicates") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.unary.scalar.OneOf") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.unary.sequence.OneOfFloatSequence") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.unary.sequence.OneOfSequence") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.unary.sequence.SeqIndexFloatNonEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoScalar.NumericFloat") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoScalar.NumericInt") != -1)

                    // Ignore invariants over two sequences.
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.PairwiseFloatEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.PairwiseFloatGreaterEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.PairwiseFloatGreaterThan") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.PairwiseFloatLessEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.PairwiseFloatLessThan") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.PairwiseIntEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.PairwiseIntGreaterEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.PairwiseIntGreaterThan") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.PairwiseIntLessEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.PairwiseIntLessThan") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.PairwiseLinearBinary") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.PairwiseLinearBinaryFloat") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.PairwiseNumericFloat") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.PairwiseNumericInt") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.Reverse") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.ReverseFloat") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SeqSeqFloatEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SeqSeqFloatGreaterEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SeqSeqFloatGreaterThan") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SeqSeqFloatLessEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SeqSeqFloatLessThan") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SeqSeqIntEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SeqSeqIntGreaterEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SeqSeqIntGreaterThan") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SeqSeqIntLessEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SeqSeqIntLessThan") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SeqSeqStringEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SeqSeqStringGreaterEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SeqSeqStringGreaterThan") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SeqSeqStringLessEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SeqSeqStringLessThan") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SubSequence") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SubSequenceFloat") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SubSet") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SubSetFloat") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SuperSet") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.twoSequence.SuperSetFloat") != -1)

                    // Ignore invariants having to do with a sequence and its indices
                    || (this.daikonClass.indexOf("daikon.inv.unary.sequence.SeqIndexFloatEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.unary.sequence.SeqIndexFloatGreaterEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.unary.sequence.SeqIndexFloatGreaterThan") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.unary.sequence.SeqIndexFloatLessEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.unary.sequence.SeqIndexFloatLessThan") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.unary.sequence.SeqIndexFloatNonEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.unary.sequence.SeqIndexIntEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.unary.sequence.SeqIndexIntGreaterEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.unary.sequence.SeqIndexIntGreaterThan") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.unary.sequence.SeqIndexIntLessEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.unary.sequence.SeqIndexIntLessThan") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.unary.sequence.SeqIndexIntNonEqual") != -1)

                    // These often lead to bogus linear equations.
                    || (this.daikonClass.indexOf("daikon.inv.ternary.threeScalar.FunctionBinary") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.ternary.threeScalar.FunctionBinaryFloat") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.ternary.threeScalar.LinearTernary") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.ternary.threeScalar.LinearTernaryFloat") != -1)

                    // Ignore invariants that compare all elements in a sequences against some value.
                    || (this.daikonClass.indexOf("daikon.inv.binary.sequenceScalar.SeqFloatEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.sequenceScalar.SeqFloatGreaterEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.sequenceScalar.SeqFloatGreaterThan") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.sequenceScalar.SeqFloatLessEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.sequenceScalar.SeqFloatLessThan") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.sequenceScalar.SeqIntEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.sequenceScalar.SeqIntGreaterEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.sequenceScalar.SeqIntGreaterThan") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.sequenceScalar.SeqIntLessEqual") != -1)
                    || (this.daikonClass.indexOf("daikon.inv.binary.sequenceScalar.SeqIntLessThan") != -1)
                    || (this.daikonClass.indexOf("(lexically)") != -1))) {
            ret = 0.19;

        } else if (kind == Kind.objectInvariant || kind == Kind.classInvariant) {
            ret = 1.0;
//         } else if (properties != null
//                 && (properties.methodAnnos(this.method()).length < ANNOS_PER_METHOD_FOR_GOOD_QUALITY)) {
//             ret = 0.9;
        } else {
            ret = 0.2;
        }

        return ret;
    }

    /**
     * The maximum confidence value calculated among all Properties given.
     */
    public static double maxConf(Property[] annos) {
        if (annos.length == 0) {
            return -1;
        }
        double max = 0.0;
        for (int i = 0; i < annos.length; i++) {
            if (annos[i].confidence > max) {
                max = annos[i].confidence;
            }
        }
        return max;
    }

    /**
     * The confidence of a set of properties. Currently it's just the maximum
     * confidence.
     */
    public static double confidence(Property[] annos) {
        return maxConf(annos);
    }

    // [[ TODO: first, you need to check that immutability and
    // uniqueness even hold. Then, you need to figoure out a better
    // way to do the stuff below (it seems to me like invRep,
    // daikonClass and confidence should be given to the constructor
    // of the object). ]]
    private Object readResolve() throws ObjectStreamException {
        try {

	    Property anno = get(kind(), daikonRep(), method());
	    Assert.assertTrue(anno.invRep == null || anno.invRep.equals(this.invRep),
			      "anno.invRep==" + anno.invRep + " this.invRep==" + this.invRep);
	    Assert.assertTrue(anno.daikonClass == null || anno.daikonClass.equals(this.daikonClass),
			      "anno.daikonClass==" + anno.daikonClass + " this.daikonClass==" + this.daikonClass);
	    Assert.assertTrue(anno.confidence == 0 || anno.confidence == this.confidence,
			      "anno.confidence==" + anno.confidence + " this.confidence==" + this.confidence);
	    if (anno.invRep == null) {
		anno.invRep = this.invRep;
		anno.daikonClass = this.daikonClass;
		anno.confidence = this.confidence;
	    }

	    return anno;

        } catch (MalformedPropertyException e) {
            throw new Error(e);
        }
    }

}
