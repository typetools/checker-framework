package daikon.tools.runtimechecker;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.List;
import java.util.*;

import jtb.syntaxtree.*;
import jtb.visitor.DepthFirstVisitor;
import jtb.visitor.TreeDumper;
import jtb.visitor.TreeFormatter;
import utilMDE.Assert;
import utilMDE.UtilMDE;
import daikon.PptMap;
import daikon.PptTopLevel;
import daikon.inv.Invariant;
import daikon.inv.OutputFormat;
import daikon.inv.ternary.threeScalar.FunctionBinary;
import daikon.tools.jtb.*;

import java.lang.reflect.*;

/**
 * Visitor that instruments a Java source file (i.e. adds code at
 * certain places) to check invariant violations at runtime.
 */
public class InstrumentVisitor extends DepthFirstVisitor {


    // If false, all invariants will be output. If true, only
    // invariants with high confidence will be output.
    public static boolean outputOnlyHighConfInvariants = false;

    // Properties under this threshold will be considered minor; at or
    // above threshold will be considered major.
    // See daikon.tools.runtimechecker.Property.calculateConfidence()
    public static double confidenceThreshold = 0.5;

    // If true, the instrumenter will make all fields of the class
    // visible. The reason for doing this is so that invariants over
    // potentially inaccessible object fields can be evaluated.
    public static boolean makeAllFieldsPublic = false;

    // The map containing the invariants.
    private final PptMap pptmap;

    // The AST of the class FooPropertyChecks, where Foo is the class
    // that we're instrumenting.
    public CheckerClasses checkerClasses;

    // The methods and constructors that were visited (in other words,
    // those explicitly declared in the source).
    public List<Method> visitedMethods = new ArrayList<Method>();
    public List<Constructor> visitedConstructors = new ArrayList<Constructor>();


    // [[ TODO: I'm using xmlString() because it will definitely give
    // different values for different Properties. But if Properties
    // are in fact unique and immutable, and if I ensure that hashcode
    // returns unique values for unique Properties, then I should be
    // able to use hashcode. So: make sure that the statements above
    // are all correct, and then use hashcode. ]]
    private Map<String, String> xmlStringToIndex = new HashMap<String,String>();

    private int varNumCounter = 0;

    private PptNameMatcher pptMatcher;

    /**
     * Create a visitor that will insert code to check the invariants
     * contained in pptmap.
     */
    public InstrumentVisitor(PptMap pptmap, TypeDeclaration root) {

        this.checkerClasses = new CheckerClasses();

        this.pptmap = pptmap;
        this.pptMatcher = new PptNameMatcher(root);

        for (Iterator<PptTopLevel> i = pptmap.pptIterator() ; i.hasNext() ; ) {
            PptTopLevel ppt = i.next();

            if (ppt.ppt_name.isExitPoint()
		&& !ppt.ppt_name.isCombinedExitPoint()) {
		continue;
	    }

            List<Invariant> invList = filterInvariants(daikon.tools.jtb.Ast.getInvariants(ppt, pptmap));
            for (Invariant inv : invList) {
		xmlStringToIndex.put(toProperty(inv).xmlString(), Integer.toString(varNumCounter));
		varNumCounter++;
	    }
	}
    }

    /**
     * If makeAllFieldsPublic == true, then it makes this field
     * declaration public.
     */
    public void visit(FieldDeclaration fd) {

        // Fix any line/col inconsistencies first
        fd.accept(new TreeFormatter());

        super.visit(fd);
        /**
         * Grammar production for ClassOrInterfaceBodyDeclaration:
         * f0 -> Initializer()
         *       | Modifiers() ( ClassOrInterfaceDeclaration(modifiers) | EnumDeclaration(modifiers)
         *                       | ConstructorDeclaration() | FieldDeclaration(modifiers)
         *                       | MethodDeclaration(modifiers) )
         *       | ";"
         */

        if (makeAllFieldsPublic) {
            NodeSequence seq = (NodeSequence)fd.getParent().getParent();
            Modifiers modifiers = (Modifiers)seq.elementAt(0);
            List modifierList = modifiers.f0.nodes;


            List<Node> newModifiers = new ArrayList<Node>();
            for (int i = 0 ; i < modifierList.size() ; i++) {
                NodeChoice nc = (NodeChoice)modifierList.get(i);
                NodeToken token = (NodeToken)nc.choice;
                if (!token.tokenImage.equals("public")
                    && !token.tokenImage.equals("protected")
                    && !token.tokenImage.equals("private")) {
                    newModifiers.add(token);
                }
            }

            newModifiers.add(new NodeToken("public"));
            modifiers.f0.nodes = new Vector<Node>(newModifiers);
        }
    }

    /**
     * Adds the following new methods:
     *
     * checkClassInvariantsInstrument(daikon.tools.runtimechecker.Violation.Time time)
     *   Checks the class invariants.
     * checkObjectInvariants_instrument(daikon.tools.runtimechecker.Violation.Time time)
     *   Check the object invariants
     * isDaikonInstrumented()
     *   returns true (you can imagine calling this method to see if the class has been
     *   instrumented).
     * getDaikonInvariants()
     *   Returns th array of properties being checked.
     *
     * Adds the following field:
     *
     * daikon.tools.runtimechecker.Property[] daikonProperties
     *   The properties being checked.
     *
     * Add code that initializes the properties array.
     */
    public void visit(ClassOrInterfaceBody clazz) {

        checkerClasses.addCheckerClass(clazz);

        // Fix any line/col inconsistencies first
        clazz.accept(new TreeFormatter());

        super.visit(clazz);

        if (Ast.isInterface(clazz)) {
            return;
        }

        // add method to check object and class invariants.
        ClassOrInterfaceDeclaration ucd = (ClassOrInterfaceDeclaration) clazz
                .getParent();
        String classname = Ast.getClassName(ucd);
        ClassOrInterfaceBodyDeclaration objInvDecl = checkObjectInvariants_instrumentDeclaration(classname);
        Ast.addDeclaration(clazz, objInvDecl);

        checkerClasses.addDeclaration(clazz, checkObjectInvariants_instrumentDeclaration_checker(classname,
                                                                                                 false /* check minor properties */));
        checkerClasses.addDeclaration(clazz, checkObjectInvariants_instrumentDeclaration_checker(classname,
                                                                                                 true /* check major properties */));

        boolean isNested = false;
        boolean isStatic = false;
        if (!Ast.isInner(ucd) || Ast.isStatic(ucd)) {
            ClassOrInterfaceBodyDeclaration classInvDecl = checkClassInvariantsInstrumentDeclaration(classname);
            checkerClasses.addDeclaration(clazz, checkClassInvariantsInstrumentDeclaration_checker(classname,
                                                                                                   false /* check minor properties */));
            checkerClasses.addDeclaration(clazz, checkClassInvariantsInstrumentDeclaration_checker(classname,
                                                                                                   true /* check minor properties */));
            Ast.addDeclaration(clazz, classInvDecl);
            Ast.addDeclaration(clazz, getInvariantsDecl());
            Ast.addDeclaration(clazz, isInstrumentedDecl());
	    Ast.addDeclaration(clazz, staticPropertyDecl());
	    Ast.addDeclaration(clazz, staticPropertyInit());
        }
    }

    /**
     * Adds code to check class invariants and preconditions on entry
     * (but not object invariants, because there's no object yet!).
     *
     * Adds code to check postcontiions, class and object invariants
     * on exit.
     */
    public void visit(ConstructorDeclaration ctor) {

        visitedConstructors.add(Ast.getConstructor(ctor));

        // Fix any line/col inconsistencies first
        ctor.accept(new TreeFormatter());

        super.visit(ctor);

        // Find declared throwables.
        List<String> declaredThrowables = getDeclaredThrowables(ctor.f3);

        List<PptTopLevel> matching_ppts = pptMatcher.getMatches(pptmap, ctor);

        StringBuffer code = new StringBuffer();

        NodeListOptional ctorBody = ctor.f6;

        // Create the code for a new BlockStatement that will be the new
        // body of the constructor.
        code.append("{");

        // Count this program point entry.
        code.append("daikon.tools.runtimechecker.Runtime.numPptEntries++;");

        // Check class invariants.
        code.append("checkClassInvariantsInstrument(daikon.tools.runtimechecker.Violation.Time.onEntry);");

        String name = Ast.getName(ctor);
        List<String> parameters = new ArrayList<String>();
        List<String> typesAndParameters = new ArrayList<String>();
        for (FormalParameter param : Ast.getParametersNoImplicit(ctor)) {
            parameters.add(Ast.getName(param));
            typesAndParameters.add(Ast.format(param));
        }

        checkerClasses
            .addDeclaration(ctor, checkPreconditions_checker_constructor(matching_ppts, pptmap, name, typesAndParameters,
                                                                         false /* check minor properties*/));
        checkerClasses
            .addDeclaration(ctor, checkPreconditions_checker_constructor(matching_ppts, pptmap, name, typesAndParameters,
                                                                         true /* check major properties*/));

        checkerClasses
            .addDeclaration(ctor, checkPostconditions_checker_constructor(matching_ppts, pptmap, name, typesAndParameters,
                                                                          false /* check minor properties*/));

        checkerClasses
            .addDeclaration(ctor, checkPostconditions_checker_constructor(matching_ppts, pptmap, name, typesAndParameters,
                                                                          true /* check major properties*/));


        // Check preconditions.
        checkPreconditions(code, matching_ppts, pptmap);

        // Call original constructor code, wrapped in a try clause so that
        // we can evaluate object/class invariants before exiting, even
        // if an exception is thrown.
        code.append("boolean methodThrewSomething_instrument = false;");

        //code.append("try {");

        // Insert original constructor code.
        // [[ TODO: should I use the daikon dumper that Mike found? ]]
        StringWriter stringWriter = new StringWriter();
        TreeDumper dumper = new TreeDumper(stringWriter);
        dumper.visit(ctorBody);
        code.append(stringWriter.toString());

        // Handle any exceptions, check postconditions, object and class
        // invariants.
        exitChecks(code, matching_ppts, pptmap, declaredThrowables, false);

        code.append("}");

        // Replace constructor body with instrumented code.
        BlockStatement newCtorBody = (BlockStatement) Ast.create(
                "BlockStatement", code.toString());
        //newCtorBody.accept(new TreeFormatter(2, 0));
        ctor.f6 = new NodeListOptional(newCtorBody);
    }

    // Methods that we have created
    private final Set<MethodDeclaration> generated_methods = new HashSet<MethodDeclaration>();

    /**
     *
     */
    public void visit(MethodDeclaration method) {

        // Fix any line/col inconsistencies first
        method.accept(new TreeFormatter());

        visitedMethods.add(Ast.getMethod(method));

        super.visit(method);

        ClassOrInterfaceDeclaration clsdecl =
            (ClassOrInterfaceDeclaration)Ast.getParent(ClassOrInterfaceDeclaration.class, method);

        Assert.assertTrue(clsdecl != null);

        if (Ast.isInterface(clsdecl)) {
            return;
        }

        method.accept(new TreeFormatter());

//         System.out.println("@@@0");
//         method.accept(new TreeDumper());
//         System.out.println("@@@1");

        if (generated_methods.contains(method)) {
            return;
        }

        // Determine if method is static.
        boolean isStatic = Ast.isStatic(method);

        // Find declared throwables.
        List<String> declaredThrowables = getDeclaredThrowables(method.f3);

        List<PptTopLevel> matching_ppts = pptMatcher.getMatches(pptmap, method);

        String name = Ast.getName(method);
        String returnType = Ast.getReturnType(method);
        String maybeReturn = (returnType.equals("void") ? "" : "return");
        List<String> typesAndParameters = new ArrayList<String>();
        List<String> parameters = new ArrayList<String>();
        for (FormalParameter param : Ast.getParameters(method)) {
            parameters.add(Ast.getName(param));
            typesAndParameters.add(Ast.format(param));
        }

        StringBuffer code = new StringBuffer();

        code.append("{");

        // Count this program point entry.
        code.append("daikon.tools.runtimechecker.Runtime.numPptEntries++;");

        // Check object invariants.
        if (!isStatic) {
            code.append("checkObjectInvariants_instrument(daikon.tools.runtimechecker.Violation.Time.onEntry);");
        }

        // Check class invariants.
        code.append("checkClassInvariantsInstrument(daikon.tools.runtimechecker.Violation.Time.onEntry);");

        checkerClasses
            .addDeclaration(method, checkPreconditions_checker_method(matching_ppts, pptmap, name, typesAndParameters,
                                                                      false /* check minor properties */));
        checkerClasses
            .addDeclaration(method, checkPreconditions_checker_method(matching_ppts, pptmap, name, typesAndParameters,
                                                                      true /* check major properties */));

        checkerClasses
            .addDeclaration(method, checkPostconditions_checker_method(matching_ppts, pptmap, name, returnType, typesAndParameters,
                                                                       false /* check minor properties */));

        checkerClasses
            .addDeclaration(method, checkPostconditions_checker_method(matching_ppts, pptmap, name, returnType, typesAndParameters,
                                                                       true /* check major properties */));

        checkPreconditions(code, matching_ppts, pptmap);

        // Call original method, wrapped in a try clause so that we
        // can evaluate object/class invariants before exiting.
        code.append("boolean methodThrewSomething_instrument = false;");
        if (!returnType.equals("void")) {
            code.append(returnType + " retval_instrument = ");

            // Assign some initial value to retval_instrument, otherwise
            // compiler
            // issues a "might not have been initialized" error.
            if (returnType.equals("boolean")) {
                code.append("false");
            } else if (returnType.equals("char")) {
                code.append("'a'");
            } else if (returnType.equals("byte") || returnType.equals("double")
                    || returnType.equals("float") || returnType.equals("int")
                    || returnType.equals("long") || returnType.equals("short")) {
                code.append("0");
            } else {
                code.append("null");
            }

            code.append(";");
        }

        //code.append("try {");

        if (!returnType.equals("void")) {
            code.append("retval_instrument = ");
        }

        code.append("internal$" + name + "(" + UtilMDE.join(parameters, ", ")
                + ");");

        exitChecks(code, matching_ppts, pptmap, declaredThrowables, isStatic);

        // Return value.
        if (!returnType.equals("void")) {
            code.append("return retval_instrument;");
        }

        code.append("}");

        // Add method to AST.
        String new_method = code.toString();

        MethodDeclaration wrapper = (MethodDeclaration) Ast.copy(
                "MethodDeclaration", method);

        Block block = (Block) Ast.create("Block", new_method);

        wrapper.f4.choice = block;
        wrapper.accept(new TreeFormatter());

        Modifiers modifiersOrig = Ast.getModifiers(method);
        Modifiers modifiers = (Modifiers)Ast.copy("Modifiers", modifiersOrig);
        modifiers.accept(new TreeFormatter());

        ClassOrInterfaceBody c =
            (ClassOrInterfaceBody) Ast.getParent(ClassOrInterfaceBody.class, method);

        StringBuffer modifiers_declaration_stringbuffer  = new StringBuffer();
        modifiers_declaration_stringbuffer.append(Ast.format(modifiers));
        modifiers_declaration_stringbuffer.append(" ");
        modifiers_declaration_stringbuffer.append(Ast.format(wrapper));

        ClassOrInterfaceBodyDeclaration d = (ClassOrInterfaceBodyDeclaration) Ast.create(
                "ClassOrInterfaceBodyDeclaration",
                new Class[] { Boolean.TYPE },
                new Object[] { Boolean.valueOf(false) },  // isInterface == false
                modifiers_declaration_stringbuffer.toString());
        Ast.addDeclaration(c, d);
        NodeSequence ns = (NodeSequence) d.f0.choice;
        NodeChoice nc = (NodeChoice) ns.elementAt(1);
        MethodDeclaration generated_method = (MethodDeclaration) nc.choice;
        generated_methods.add(generated_method);

        // Rename the original method, and make it private.
        Ast.setName(method, "internal$" + name);
        Ast.setAccess(method, "private");
    }

    // vioTime can be the name of a variable (which should be in scope)
    // or a string, but if it's a string, then you should write it as
    // something like: \"<ENTER>\"
    private void appendInvariantChecks(List<Invariant> invs,
            StringBuffer code, String vioTime) {
        for (Invariant inv : invs) {

            String javarep = inv.format_using(OutputFormat.JAVA);

            Property property = toProperty(inv);

            String xmlString = property.xmlString();

            // [[ TODO : explain this. ]]
            // [[ TODO : move this to filterInvariants method. ]]
            if (xmlString.indexOf("orig(") != -1) {
                continue;
            }

            if (javarep.indexOf("unimplemented") != -1) {
                // [[ TODO: print a warning that some invariants were
                // unimplemented. ]]
                continue;
            }

            if (javarep.indexOf("\\result") != -1) {
                javarep = javarep.replaceAll("\\\\result", "retval_instrument");
            }

            String addViolationToListCode =
                "daikon.tools.runtimechecker.Runtime.violationsAdd" +
                "(daikon.tools.runtimechecker.Violation.get(daikonProperties[" +
                xmlStringToIndex.get(xmlString) +
                "], " + vioTime + "));";

            code.append("try {" + daikon.Global.lineSep + "");
            code.append("daikon.tools.runtimechecker.Runtime.numEvaluations++;");
            code.append("if (!(" + daikon.Global.lineSep + "");
            code.append(javarep);
            code.append(")) {");
            code.append(addViolationToListCode);
            code.append("}");
            code.append("} catch (ThreadDeath t_instrument) {" + daikon.Global.lineSep + "");
            code.append("throw t_instrument;");
            code.append("} catch (Throwable t_instrument) {" + daikon.Global.lineSep + "");
            // don't catch anything. The assumption is that invariant-checking code
            // never leads to an exception.
            code.append("}");
//             code.append(addViolationToListCode);
//             code.append("} catch (Error t_instrument) {" + daikon.Global.lineSep + "");
//             code.append(addViolationToListCode);
//             code.append("}" + daikon.Global.lineSep + "");
        }
    }

    private void appendInvariantChecks_checker(List<InvProp> ips, StringBuffer code) {

        for (InvProp ip : ips) {

            Invariant inv = ip.invariant;
            Property property = ip.property;

            Assert.assertTrue(property.xmlString().equals(toProperty(inv).xmlString()));

            String javarep = inv.format_using(OutputFormat.JAVA);

            String daikonrep = inv.format_using(OutputFormat.DAIKON);

            String xmlString = property.xmlString();

            // [[ TODO : explain this. ]]
            // [[ TODO : move this to filterInvariants method. ]]
            if (xmlString.indexOf("orig(") != -1) {
                continue;
            }

            if (javarep.indexOf("unimplemented") != -1) {
                // [[ TODO: print a warning that some invariants were
                // unimplemented. ]]
                continue;
            }

            if (javarep.indexOf("\\result") != -1) {
                javarep = javarep.replaceAll("\\\\result", "checker_returnval");
            }

            code.append("// Check: " + daikonrep + daikon.Global.lineSep);
            code.append("junit.framework.Assert.assertTrue(");
            code.append(fixForExternalUse(javarep));
            code.append(");");
        }
    }

    private static String fixForExternalUse(String inv) {
        inv = inv.replace("this", "thiz");
        return inv;
    }

    /**
     * @return
     */
    private ClassOrInterfaceBodyDeclaration isInstrumentedDecl() {
        StringBuffer code = new StringBuffer();
        code.append("public static boolean isDaikonInstrumented() { return true; }");
        return (ClassOrInterfaceBodyDeclaration) Ast.create("ClassOrInterfaceBodyDeclaration",
                                                            new Class[] { Boolean.TYPE },
                                                            new Object[] { Boolean.valueOf(false) },  // isInterface == false

                                                            code
                                                            .toString());
    }

    /**
     * @return
     */
    private ClassOrInterfaceBodyDeclaration getInvariantsDecl() {
        StringBuffer code = new StringBuffer();
        code.append("public static java.util.Set getDaikonInvariants() {");
        code.append("  return new java.util.HashSet(java.util.Arrays.asList(daikonProperties));");
        code.append("}");
        return (ClassOrInterfaceBodyDeclaration) Ast.create("ClassOrInterfaceBodyDeclaration",
                                                            new Class[] { Boolean.TYPE },
                                                            new Object[] { Boolean.valueOf(false) },  // isInterface == false

                                                            code
                                                            .toString());
    }

    /**
     * @return
     */
    private ClassOrInterfaceBodyDeclaration staticPropertyDecl() {
        StringBuffer code = new StringBuffer();

	code.append("private static daikon.tools.runtimechecker.Property[] daikonProperties;");
        return (ClassOrInterfaceBodyDeclaration) Ast.create("ClassOrInterfaceBodyDeclaration",
                                                            new Class[] { Boolean.TYPE },
                                                            new Object[] { Boolean.valueOf(false) },  // isInterface == false

                                                            code
                                                            .toString());
    }

    private ClassOrInterfaceBodyDeclaration staticPropertyInit() {
        StringBuffer code = new StringBuffer();

	code.append("static {\n");
	code.append("try {\n");
	code.append("daikonProperties = new daikon.tools.runtimechecker.Property[" + varNumCounter  + "];\n");

        for (Map.Entry e : xmlStringToIndex.entrySet()) {
            code.append("daikonProperties[" + e.getValue() + "] = ");
            code.append("daikon.tools.runtimechecker.Property.get(");
            code.append("\"");
            code.append(e.getKey());
            code.append("\"");
            code.append(");\n");
	}

        code.append("} catch (Exception e) {");
        code.append(" System.err.println(\"malformed invariant. This is probably a bug in the daikon.tools.runtimechecker tool; please submit a bug report.\");");
        code.append(" e.printStackTrace();");
        code.append(" System.exit(1);\n");
        code.append("}");

        code.append("} // end static");

        return (ClassOrInterfaceBodyDeclaration) Ast.create("ClassOrInterfaceBodyDeclaration",
                                                            new Class[] { Boolean.TYPE },
                                                            new Object[] { Boolean.valueOf(false) },  // isInterface == false

                                                            code
                                                            .toString());
    }

    private ClassOrInterfaceBodyDeclaration checkObjectInvariants_instrumentDeclaration(String classname) {

        StringBuffer code = new StringBuffer();
        code
                .append("private void checkObjectInvariants_instrument(daikon.tools.runtimechecker.Violation.Time time) {");
        String objectPptname = classname + ":::OBJECT";
        PptTopLevel objectPpt = pptmap.get(objectPptname);
        if (objectPpt != null) {
            List<Invariant> objectInvariants = filterInvariants(Ast
                    .getInvariants(objectPpt, pptmap));
            appendInvariantChecks(objectInvariants, code, "time");
        }
        code.append("}" + daikon.Global.lineSep + "");
        return (ClassOrInterfaceBodyDeclaration) Ast.create("ClassOrInterfaceBodyDeclaration",
                                                            new Class[] { Boolean.TYPE },
                                                            new Object[] { Boolean.valueOf(false) },  // isInterface == false
                                                            code.toString());
    }

    private ClassOrInterfaceBodyDeclaration checkClassInvariantsInstrumentDeclaration(
            String classname) {
        StringBuffer code = new StringBuffer();
        code.append("private static void checkClassInvariantsInstrument(daikon.tools.runtimechecker.Violation.Time time) {");
        String classPptname = classname + ":::CLASS";
        PptTopLevel classPpt = pptmap.get(classPptname);
        if (classPpt != null) {
            List<Invariant> classInvariants = filterInvariants(Ast
                    .getInvariants(classPpt, pptmap));
            appendInvariantChecks(classInvariants, code, "time");
        }
        code.append("}" + daikon.Global.lineSep + "");
        return (ClassOrInterfaceBodyDeclaration) Ast.create("ClassOrInterfaceBodyDeclaration",
                                                            new Class[] { Boolean.TYPE },
                                                            new Object[] { Boolean.valueOf(false) },  // isInterface == false
                                                            code.toString());
    }

    private StringBuffer checkObjectInvariants_instrumentDeclaration_checker(
            String classname, boolean majorProperties) {
        StringBuffer code = new StringBuffer();
        code.append("public static void check" +
                    (majorProperties ? "Major" : "Minor") +
                    "ObjectInvariants(Object thiz) {");
        String objectPptname = classname + ":::OBJECT";
        PptTopLevel objectPpt = pptmap.get(objectPptname);
        if (objectPpt != null) {
            List<Invariant> objectInvariants = filterInvariants(Ast.getInvariants(objectPpt, pptmap));
            List<InvProp> finalList = null;
            if (majorProperties) {
                finalList = getMajor(objectInvariants);
            } else {
                finalList = getMinor(objectInvariants);
            }
            appendInvariantChecks_checker(finalList, code);
        }
        code.append("}" + daikon.Global.lineSep);
        return code;
    }

    private StringBuffer checkClassInvariantsInstrumentDeclaration_checker(
            String classname, boolean majorProperties) {
        StringBuffer code = new StringBuffer();
        code.append("public static void check" +
                    (majorProperties ? "Major" : "Minor") +
                    "ClassInvariants() {");
        String classPptname = classname + ":::CLASS";
        PptTopLevel classPpt = pptmap.get(classPptname);
        if (classPpt != null) {
            List<Invariant> classInvariants = filterInvariants(Ast.getInvariants(classPpt, pptmap));
            List<InvProp> finalList = null;
            if (majorProperties) {
                finalList = getMajor(classInvariants);
            } else {
                finalList = getMinor(classInvariants);
            }
            appendInvariantChecks_checker(finalList, code);
        }
        code.append("}" + daikon.Global.lineSep + "");
        return code;
    }

    /**
     * Return a subset of the argument list, removing invariants
     * that do not have a properly implemented Java format.
     **/
    private static List<Invariant> filterInvariants(
            List<Invariant> invariants) {
        List<Invariant> survivors = new ArrayList<Invariant>();
        for (Invariant inv : invariants) {

            if (! inv.isValidExpression(OutputFormat.JAVA)) {
                continue;
            }
            // Left and right shifts are formatted properly by Daikon,
            // but as of 2/6/2005, a JTB bug prevents them from being
            // inserted into code.
            if (inv instanceof FunctionBinary) {
                FunctionBinary fb = (FunctionBinary)inv;
                if (fb.isLshift() || fb.isRshiftSigned() || fb.isRshiftUnsigned()) {
                    // System.err.println("Warning: shift operation skipped: " + inv.format_using(OutputFormat.JAVA));
                    continue;
                }
            }

            if (outputOnlyHighConfInvariants) {
                if (toProperty(inv).calculateConfidence() < 0.5) {
                    continue;
                }
            }

            survivors.add(inv);
        }
        return survivors;
    }

    private static List<String> getDeclaredThrowables(NodeOptional nodeOpt) {
        List<String> declaredThrowables = new ArrayList<String>();
        if (nodeOpt.present()) {
            NodeSequence seq = (NodeSequence) nodeOpt.node;
            // There should only be two elements: "throws" and NameList
            Assert.assertTrue(seq.size() == 2);
            NameList nameList = (NameList) seq.elementAt(1);

            StringWriter stringWriter = new StringWriter();
            TreeDumper dumper = new TreeDumper(stringWriter);
            dumper.visit(nameList);

            String[] declaredThrowablesArray = stringWriter.toString().trim()
                    .split(",");
            for (int i = 0; i < declaredThrowablesArray.length; i++) {
                declaredThrowables.add(declaredThrowablesArray[i].trim());
            }
            //       System.out.println("@@@");
            //       for (int i = 0 ; i < declaredThrowables.size() ; i++) {
            //         System.out.println("xxx" + declaredThrowables.get(i) + "xxx");
            //       }
        }
        return declaredThrowables;
    }

    // [[ TODO: This method can return the wrong sequence of catch clauses,
    //    because it has no concept of throwable subclasses, and it just
    //    orders catch clauses in the order in which they appear in
    //    declaredThrowable. This can cause compilation to fail. ]]
    private void exitChecks(StringBuffer code,
            List<PptTopLevel> matching_ppts, PptMap pptmap,
	   List<String> declaredThrowables, boolean isStatic) {

// 	List<String> declaredThrowablesLocal = new ArrayList<String>(declaredThrowables);
// 	declaredThrowablesLocal.remove("java.lang.RuntimeException");
// 	declaredThrowablesLocal.remove("RuntimeException");
// 	declaredThrowablesLocal.remove("java.lang.Error");
// 	declaredThrowablesLocal.remove("Error");

        // Count this program point exit.
        code.append("daikon.tools.runtimechecker.Runtime.numNormalPptExits++;");

//         // [[ TODO: Figure out what could go wrong here (e.g. what if
//         // method declaration says "throws Throwable") and prepare for
//         // it. ]]
//         for (String declaredThrowable : declaredThrowablesLocal) {
//             code.append("} catch (" + declaredThrowable + " t_instrument) {");
//             // Count this program point exit.
//             code.append("daikon.tools.runtimechecker.Runtime.numExceptionalPptExits++;");
//             code.append("  methodThrewSomething_instrument = true;");
//             code.append("  throw t_instrument;");
//         }

// 	code.append("} catch (java.lang.RuntimeException t_instrument) {");
// 	code.append("  methodThrewSomething_instrument = true;");
//         // Count this program point exit.
//         code.append("daikon.tools.runtimechecker.Runtime.numExceptionalPptExits++;");
// 	code.append("  throw t_instrument;");
// 	code.append("} catch (java.lang.Error t_instrument) {");
//         // Count this program point exit.
//         code.append("daikon.tools.runtimechecker.Runtime.numExceptionalPptExits++;");
// 	code.append("  methodThrewSomething_instrument = true;");
// 	code.append("  throw t_instrument;");

//         code.append("} finally {");

        // If method didn't throw an exception, it completed
        // normally--check method postconditions (If the method didn't
        // complete normally, it makes no sense to check postconditions,
        // because Daikon only reports normal-exit postconditions.)

//         code.append(" if (!methodThrewSomething_instrument) {");

        // Check postconditions.
        for (PptTopLevel ppt : matching_ppts) {
            if (ppt.ppt_name.isExitPoint()
		&& ppt.ppt_name.isCombinedExitPoint()) {
                List<Invariant> postconditions = filterInvariants(Ast
								     .getInvariants(ppt, pptmap));
                appendInvariantChecks(postconditions, code, "daikon.tools.runtimechecker.Violation.Time.onExit");
            }
        }
//         code.append("}");

        // Check object invariants.
        if (!isStatic) {
            code.append("checkObjectInvariants_instrument(daikon.tools.runtimechecker.Violation.Time.onExit);");
        }
        // Check class invariants.
        code.append("checkClassInvariantsInstrument(daikon.tools.runtimechecker.Violation.Time.onExit);");

//         code.append("}"); // this closes the finally clause
    }

    private void checkPreconditions(StringBuffer code,
            List<PptTopLevel> matching_ppts, PptMap pptmap) {
        for (PptTopLevel ppt : matching_ppts) {
            if (ppt.ppt_name.isEnterPoint()) {
                List<Invariant> preconditions = filterInvariants(Ast
                        .getInvariants(ppt, pptmap));
                appendInvariantChecks(preconditions, code, "daikon.tools.runtimechecker.Violation.Time.onEntry");
            }
        }
    }

    private StringBuffer checkPreconditions_checker_method(List<PptTopLevel> matching_ppts, PptMap pptmap,
                                                           String methodName, List<String> parameters,
                                                           boolean majorProperties) {

        StringBuffer code = new StringBuffer();
        code.append("public static void check" +
                    (majorProperties ? "Major" : "Minor") +
                    "Preconditions_" +
                    methodName + "(" +
                    "Object thiz" +
                    (parameters.size() > 0 ? ", " : "") +
                    UtilMDE.join(parameters, ", ") +
                    ") {");

        for (PptTopLevel ppt : matching_ppts) {
            if (ppt.ppt_name.isEnterPoint()) {
                List<Invariant> preconditions = filterInvariants(Ast.getInvariants(ppt, pptmap));
                List<InvProp> finalList = null;
                if (majorProperties) {
                    finalList = getMajor(preconditions);
                } else {
                    finalList = getMinor(preconditions);
                }
                appendInvariantChecks_checker(finalList, code);
            }
        }

        code.append("}");
        return code;
    }



    private StringBuffer checkPostconditions_checker_method(List<PptTopLevel> matching_ppts, PptMap pptmap,
                                                            String methodName, String returnType,
                                                            List<String> parameters, boolean majorProperties) {

        StringBuffer code = new StringBuffer();
        code.append("public static void check" +
                    (majorProperties ? "Major" : "Minor") +
                    "Postconditions_" +
                    methodName + "(" +
                    "Object thiz " +
                    (returnType.equals("void") ? "" : ", " + returnType + " checker_returnval") +
                    (parameters.size() > 0 ? ", " : "") +
                    UtilMDE.join(parameters, ", ") +
                    ") {");

        for (PptTopLevel ppt : matching_ppts) {
            if (ppt.ppt_name.isExitPoint()
		&& ppt.ppt_name.isCombinedExitPoint()) {
                List<Invariant> postconditions = filterInvariants(Ast.getInvariants(ppt, pptmap));
                List<InvProp> finalList = null;
                if (majorProperties) {
                    finalList = getMajor(postconditions);
                } else {
                    finalList = getMinor(postconditions);
                }
                appendInvariantChecks_checker(finalList, code);
            }
        }

        code.append("}");
        return code;
    }




    private StringBuffer checkPreconditions_checker_constructor(List<PptTopLevel> matching_ppts, PptMap pptmap,
                                                                String methodName, List<String> parameters,
                                                                boolean majorProperties) {

        StringBuffer code = new StringBuffer();
        code.append("public static void check" +
                    (majorProperties ? "Major" : "Minor") +
                    "Preconditions_" +
                    methodName + "(" +
                    UtilMDE.join(parameters, ", ") +
                    ") {");

        for (PptTopLevel ppt : matching_ppts) {
            if (ppt.ppt_name.isEnterPoint()) {
                List<Invariant> preconditions = filterInvariants(Ast.getInvariants(ppt, pptmap));
                List<InvProp> finalList = null;
                if (majorProperties) {
                    finalList = getMajor(preconditions);
                } else {
                    finalList = getMinor(preconditions);
                }
                appendInvariantChecks_checker(finalList, code);
            }
        }

        code.append("}");
        return code;
    }



    private StringBuffer checkPostconditions_checker_constructor(List<PptTopLevel> matching_ppts, PptMap pptmap,
                                                                 String methodName, List<String> parameters,
                                                                 boolean majorProperties) {

        StringBuffer code = new StringBuffer();
        code.append("public static void check" +
                    (majorProperties ? "Major" : "Minor") +
                    "Postconditions_" +
                    methodName + "(" +
                    "Object thiz " +
                    (parameters.size() > 0 ? ", " : "") +
                    UtilMDE.join(parameters, ", ") +
                    ") {");

        for (PptTopLevel ppt : matching_ppts) {
            if (ppt.ppt_name.isExitPoint()
		&& ppt.ppt_name.isCombinedExitPoint()) {
                List<Invariant> postconditions = filterInvariants(Ast.getInvariants(ppt, pptmap));
                List<InvProp> finalList = null;
                if (majorProperties) {
                    finalList = getMajor(postconditions);
                } else {
                    finalList = getMinor(postconditions);
                }
                appendInvariantChecks_checker(finalList, code);
            }
        }

        code.append("}");
        return code;
    }

    private static Property toProperty(Invariant inv) {
        StringBuffer code = new StringBuffer();

        String daikonrep = inv.format_using(OutputFormat.DAIKON);

        String javarep = inv.format_using(OutputFormat.JAVA);

        if (daikonrep.indexOf("\"") != -1 || daikonrep.indexOf("\\") != -1) {
            // Now comes some real ugliness: [[ ... ]] It's easier to do
            // this transformation on a character list than by pattern
            // matching against a String.
            char[] chars = daikonrep.toCharArray();
            List<Character> charList = new ArrayList<Character>();
            for (int j = 0; j < chars.length; j++) {
                char c = chars[j];
                if ((c == '\"') || (c == '\\')) {
                    charList.add(new Character('\\'));
                }
                charList.add(new Character(c));
            }
            char[] backslashedChars = new char[charList.size()];
            for (int j = 0; j < charList.size(); j++) {
                backslashedChars[j] = charList.get(j).charValue();
            }
            daikonrep = new String(backslashedChars);
        }


        code.append("<INVINFO>");
        code.append("<" + inv.ppt.parent.ppt_name.getPoint() + ">");
        code.append("<DAIKON>" + daikonrep + "</DAIKON>");
        code.append("<INV>" + utilMDE.UtilMDE.escapeNonJava(javarep) + "</INV>");
        code.append("<DAIKONCLASS>" + inv.getClass().toString()
                    + "</DAIKONCLASS>");
        code.append("<METHOD>" + inv.ppt.parent.ppt_name.getSignature()
                    + "</METHOD>");
        code.append("</INVINFO>");

        try {
            return Property.get(code.toString());
        } catch (MalformedPropertyException e) {
            throw new Error(e);
        }
    }

    /**
     * A pair consisting of an Invariant and its corresponding Property.
     */
    private static class InvProp {
        public InvProp(Invariant inv, Property p) {
            this.invariant = inv;
            this.property = p;
        }
        public Invariant invariant;
        public Property property;
    }

    /**
     * Returns the invariants (and their properties) that have
     * confidence values at or above confidenceThreshold.
     */
    List<InvProp> getMajor(List<Invariant> invs) {

        List<InvProp> ret = new ArrayList<InvProp>();
        for (Invariant i : invs) {
            Property p = toProperty(i);
            if (p.confidence >= confidenceThreshold) {
                ret.add(new InvProp(i, p));
            }
        }
        return ret;
    }

    /**
     * Returns the invariants (and their properties) that have
     * confidence values below confidenceThreshold.
     */
    List<InvProp> getMinor(List<Invariant> invs) {

        List<InvProp> ret = new ArrayList<InvProp>();
        for (Invariant i : invs) {
            Property p = toProperty(i);
            if (p.confidence < confidenceThreshold) {
                ret.add(new InvProp(i, p));
            }
        }
        return ret;
    }

    /**
     * Add checker methods with empty bodies for all public
     * methods and constuctors not explicitly declared.
     */
    public void add_checkers_for_nondeclared_members() {

        for (CheckerClass cc : checkerClasses.classes) {

            Class c = Ast.getClass(cc.fclassbody);

            // Check that all declared methods were in fact visited.
            for (Method m : c.getDeclaredMethods()) {
                if (!visitedMethods.contains(m)) {
                    Assert.assertTrue(false,
                                      "m=" + m + ", visitedMethods=" + visitedMethods);
                }
            }

            // FIXME consider implicit constructors when doing check.
            // Check that all declared constructors were in fact visited.
//             for (Constructor cons : c.getDeclaredConstructors()) {
//                 if (!visitedConstructors.contains(cons)) {
//                     Assert.assertTrue(cons.equals(getDefaultConstructor(c)),
//                                       "cons=" + cons + ", visitedConstructors=" + visitedConstructors);
//                 }
//             }

            for (Method m : c.getMethods()) {
                if (!visitedMethods.contains(m)) {
                    cc.addDeclaration(createEmptyDeclaration(m));
                }
            }

            for (Constructor cons : c.getConstructors()) {
                if (!visitedConstructors.contains(cons)) {
                    cc.addDeclaration(createEmptyDeclaration(cons));
                }
            }
        }
    }

    private StringBuffer createEmptyDeclaration(Method m) {

        List<String> parameters = new ArrayList<String>();
        int paramCounter = 0;
        for (Class c : m.getParameterTypes()) {
            parameters.add(Ast.classnameForSourceOutput(c) + " param" + paramCounter++);
        }

        StringBuffer code = new StringBuffer();

        for (String s : new String[] { "Major", "Minor" }) {
            code.append("public static void check" + s + "Preconditions_" +
                        m.getName() + "(" +
                        "Object thiz" +
                        (parameters.size() > 0 ? ", " : "") +
                        UtilMDE.join(parameters, ", ") +
                        ") { /* no properties for this member */ }");

            code.append("public static void check" + s + "Postconditions_" +
                        m.getName() + "(" +
                        "Object thiz " +
                        (m.getReturnType().equals(Void.TYPE)
                         ? ""
                         : (", " + Ast.classnameForSourceOutput(m.getReturnType())
                            + " checker_returnval")) +
                        (parameters.size() > 0 ? ", " : "") +
                        UtilMDE.join(parameters, ", ") +
                        ") { /* no properties for this member */ }");
        }

        return code;
    }

    private StringBuffer createEmptyDeclaration(Constructor c) {

        List<String> parameters = new ArrayList<String>();
        int paramCounter = 0;
        for (Class cls : c.getParameterTypes()) {
            parameters.add(Ast.classnameForSourceOutput(cls) + " param" + paramCounter++);
        }

        StringBuffer code = new StringBuffer();

        Package pacg = c.getDeclaringClass().getPackage();
        String packageName = (pacg == null ? "" : pacg.getName());
        int packageNameLength = (packageName.equals("") ? 0 : packageName.length()+1 /* account for ending dot */);
        String className = c.getDeclaringClass().getName();
        Assert.assertTrue(className.startsWith(packageName));
        String baseClassName = className.substring(packageNameLength);

        for (String s : new String[] { "Major", "Minor" }) {

            code.append("public static void check" + s + "Preconditions_" +
                        baseClassName + "(" +
                        UtilMDE.join(parameters, ", ") +
                        ") { /* no properties for this member */ }");

            code.append("public static void check" + s + "Postconditions_" +
                        baseClassName + "(" +
                        "Object thiz " +
                        (parameters.size() > 0 ? ", " : "") +
                        UtilMDE.join(parameters, ", ") +
                        ") { /* no properties for this member */ }");

        }

        return code;
    }
}
