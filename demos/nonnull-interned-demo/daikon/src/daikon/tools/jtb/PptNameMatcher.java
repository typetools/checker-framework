package daikon.tools.jtb;

import daikon.*;

import jtb.*;
import jtb.syntaxtree.*;
import jtb.visitor.*;

import java.util.*;

import utilMDE.*;


/**
 * Matches program point names with their corresponding MethodDeclaration's
 * (or ConstructorDeclaration's) in an AST.
 *
 * There are a number of issues in matching, for example, ASTs contain
 * generics, and program point names do not. This implementation
 * handles such issues.
 */
public class PptNameMatcher {

  // Output debugging information when matching a PptName to an AST.
  private static boolean debug_getMatches = false;

  /**
   * Create an AST matcher that will match program points against
   * AST elements rooted at `root'.
   */
  public PptNameMatcher(Node root) {
    root.accept(new ClassOrInterfaceTypeDecorateVisitor());
  }


  public String getUngenerifiedType(FormalParameter p) {

    Type type = p.f1;

    //  Grammar production for type:
    //  f0 -> ReferenceType()
    //        | PrimitiveType()

    if (type.f0.which == 0) {
      // It's a reference type.
      ReferenceType refType = (ReferenceType)type.f0.choice;
      //  Grammar production for ReferenceType:
      //  f0 -> PrimitiveType() ( "[" "]" )+
      //        | ( ClassOrInterfaceType() ) ( "[" "]" )*

      if (refType.f0.which == 0) {
        // It's a primitive array; no generics to handle.
        return Ast.getType(p);

      } else {

        // Make a copy of param (because we may modify it: we may
        // remove some generics stuff).
        //p.accept(new TreeFormatter());
        FormalParameter param = (FormalParameter)Ast.create("FormalParameter", Ast.format(p));



        Type type2 = param.f1;
        ReferenceType refType2 = (ReferenceType)type2.f0.choice;

        // Note the wrapping parentheses in
        //    ( ClassOrInterfaceType() ) ( "[" "]" )*
        NodeSequence intermediateSequence = (NodeSequence)refType2.f0.choice;
        NodeSequence intermediateSequenceOrig = (NodeSequence)refType.f0.choice;
        NodeSequence seq = (NodeSequence)intermediateSequence.elementAt(0);
        NodeSequence seqOrig = (NodeSequence)intermediateSequenceOrig.elementAt(0);


        Vector<Node> singleElementVector = seq.nodes;
        Vector<Node> singleElementVectorOrig = seqOrig.nodes;
        // Replace the ClassOrInterfaceType with its ungenerified version.

//     System.out.println("@0");
//     param.accept(new TreeDumper());


        // ClassOrInterfaceType t = (ClassOrInterfaceType)singleElementVector.get(0);
        ClassOrInterfaceType tOrig = (ClassOrInterfaceType)singleElementVectorOrig.get(0);
        Assert.assertTrue(tOrig.unGenerifiedVersionOfThis != null);
        singleElementVector.set(0, tOrig.unGenerifiedVersionOfThis);
        // Return getType of the ungenerified version of p.

        // tOrig.unGenerifiedVersionOfThis may have line/col numbering
        // that's inconsistent with param, so we call a formatter
        // here. param is only used for matching, and afterwards it's
        // discarded. So it's ok to reformat it.
        param.accept(new TreeFormatter());


//     System.out.println("@1");
//     param.accept(new TreeDumper());
//     System.out.println("@2");

        return Ast.getType(param);

      }

    } else {
      // It's a primitive; no generics to handle.
      return Ast.getType(p);
    }
  }

  /**
   * Iterates through program points and returns those that match the
   * given method declaration.
   */
  public List<PptTopLevel> getMatches(PptMap ppts, MethodDeclaration methdecl) {
    return getMatchesInternal(ppts, methdecl);
  }

  /**
   * Iterates through program points and returns those that match the
   * given constructor declaration.
   */
  public  List<PptTopLevel> getMatches(PptMap ppts, ConstructorDeclaration constrdecl) {
    return getMatchesInternal(ppts, constrdecl);
  }

  // Iterates through program points and returns those that match the
  // given method or constructor declaration.
  private List<PptTopLevel> getMatchesInternal(PptMap ppts, Node methodOrConstructorDeclaration) {

    List<PptTopLevel> result = new ArrayList<PptTopLevel>();

    for (Iterator<PptTopLevel> itor = ppts.pptIterator() ; itor.hasNext() ; ) {
      PptTopLevel ppt = itor.next();
      PptName ppt_name = ppt.ppt_name;

      if (matches(ppt_name, methodOrConstructorDeclaration)) {
        result.add(ppt);
      }
    }

    if (debug_getMatches) System.out.println("getMatch => " + result);
    return result;
  }

  public boolean matches(PptName pptName, Node methodOrConstructorDeclaration) {

    // This method figures out three things and then calls another
    // method to do the match. The three things are:

    // 1. method name
    // 2. class name
    // 3. method parameters

    String classname = null;
    String methodname = null;
    List<FormalParameter> params = null;

    if (methodOrConstructorDeclaration instanceof MethodDeclaration) {
      classname = Ast.getClassName((MethodDeclaration)methodOrConstructorDeclaration);
      methodname = Ast.getName((MethodDeclaration)methodOrConstructorDeclaration);
      params = Ast.getParameters((MethodDeclaration)methodOrConstructorDeclaration);
    } else if (methodOrConstructorDeclaration instanceof ConstructorDeclaration) {
      classname = Ast.getClassName((ConstructorDeclaration)methodOrConstructorDeclaration);
      methodname = "<init>";
      params = Ast.getParameters((ConstructorDeclaration)methodOrConstructorDeclaration);
    } else {
      throw new Error("Bad type in Ast.getMatches: must be a MethodDeclaration or a ConstructorDeclaration:"
                      + methodOrConstructorDeclaration);
    }

    if (debug_getMatches) System.out.println("getMatches(" + classname + ", " + methodname + ", ...)");
    if (methodname.equals("<init>")) {
      int dotpos = classname.lastIndexOf('.');
      if (dotpos == -1) {
        methodname = classname;
      } else {
        methodname = classname.substring(dotpos + 1);
      }
      if (debug_getMatches) System.out.println("getMatches(" + classname + ", " + methodname + ", ...)");
    }

    if (debug_getMatches) System.out.println("getMatch goal = " + classname + " " + methodname);

    return matches(pptName, classname, methodname, params);

  }

  // True if pptName's name matches the method represented by the rest
  // of the parameters.
  private boolean matches(PptName pptName,
                                 String classname,
                                 String methodname,
                                 List<FormalParameter> method_params) {

      if (!(classname.equals(pptName.getFullClassName())
            && methodname.equals(pptName.getMethodName()))) {
        if (debug_getMatches) System.out.println("getMatch: class name and method name DO NOT match candidate.");
        return false;
      }

      List<String> pptTypeStrings = extractPptArgs(pptName);

      if (pptTypeStrings.size() != method_params.size()) {
        if (debug_getMatches) System.out.println("arg lengths mismatch: " + pptTypeStrings.size() + ", " + method_params.size());
        return false;
      }

      boolean unmatched = false;

      for (int i=0; i < pptTypeStrings.size(); i++) {
        String pptTypeString = pptTypeStrings.get(i);
        FormalParameter astType = method_params.get(i);

        if (debug_getMatches) {
          System.out.println("getMatch considering "
                             + pptTypeString
                           + " (" + pptName.getFullClassName() + ","
                             + pptName.getMethodName() + ")");
        }

        if (debug_getMatches) { System.out.println("Trying to match at arg position " + Integer.toString(i)); }

        if (!typeMatch(pptTypeString, astType)) {
          return false;
        } else {
          continue;
        }
      }

      return true;
  }

  public boolean typeMatch(String pptTypeString, FormalParameter astFormalParameter) {


    String astTypeString = getUngenerifiedType(astFormalParameter);

    if (debug_getMatches) System.out.println("Comparing " + pptTypeString + " to " + astTypeString + ":");

    if (Ast.typeMatch(pptTypeString, astTypeString)) {
      if (debug_getMatches) System.out.println("Match arg: " + pptTypeString + " " + astTypeString);
      return true;
    }

    if ((pptTypeString != null) && Ast.typeMatch(pptTypeString, astTypeString)) {
      if (debug_getMatches) System.out.println("Match arg: " + pptTypeString + " " + astTypeString);
      return true;
    }

    if (debug_getMatches) System.out.println("Mismatch arg: " + pptTypeString + " " + astTypeString);

    return false;

  }

  public List<String> extractPptArgs(PptName ppt_name) {

    String pptFullMethodName = ppt_name.getSignature();

    if (debug_getMatches) System.out.println("pptFullMethodName = " + pptFullMethodName);
    int lparen = pptFullMethodName.indexOf('(');
    int rparen = pptFullMethodName.indexOf(')');
    Assert.assertTrue(lparen > 0);
    Assert.assertTrue(rparen > lparen);
    String ppt_args_string = pptFullMethodName.substring(lparen+1, rparen);
    String[] ppt_args = utilMDE.UtilMDE.split(ppt_args_string, ", ");
    if ((ppt_args.length == 1)
        && (ppt_args[0].equals(""))) {
      ppt_args = new String[0];
    }

    return Arrays.asList(ppt_args);
  }




}
