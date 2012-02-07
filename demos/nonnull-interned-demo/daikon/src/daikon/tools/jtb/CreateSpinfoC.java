package daikon.tools.jtb;

import jtb.cparser.*;
import jtb.cparser.syntaxtree.*;
import jtb.cparser.customvisitor.*;
import java.io.*;

public class CreateSpinfoC {

  public static void main(String[] args) {
    CParser parser;
    if ( args.length == 1 )
      {
        System.out.println("Create spinfo file from file " + args[0]+ " . . .");
      }
    else
      {
        System.out.println("C Parser Version 0.1Alpha:  Usage is one of:");
        System.out.println("         java CreateSpinfoC < inputfile");
        System.out.println("OR");
        System.out.println("         java CreateSpinfoC inputfile");
        return;
      }
    try
      {
        String fileName = args[0].substring(0,args[0].lastIndexOf("."));
        File temp = null;
        // filter out the '/f' characters in the file
        try {
          temp = new File(fileName + ".temp");
          FileReader reader = new FileReader(args[0]);
          FileWriter writer = new FileWriter(temp);
          int c;
          while ((c = reader.read()) != -1) {
            if (c != '\f') {
              writer.write(c);
            }
          }
          reader.close();
          writer.close();
        } catch (IOException e) {
          System.out.println(e.getMessage());
          temp.delete();
        }
        try {
          parser = new CParser(new FileInputStream(temp));
          TranslationUnit root = parser.TranslationUnit();
          StringFinder finder = new StringFinder();
          temp.delete();
          root.accept(finder);

          ConditionPrinter printer;
          try {
            printer = new ConditionPrinter(fileName + ".spinfo");
            printer.setActualStrings(finder.functionStringMapping);
            printer.setStringArrays(finder.stringMatrices);
            root.accept(printer);
            printer.close();
          } catch (IOException e) {
            System.out.println("File IO Error");
            System.out.println(e.getMessage());
          }
          System.out.println("CreateSpinfoC:  C program parsed successfully.");
        } catch (FileNotFoundException fe) {
          System.out.println(fileName + " was not found.");
        }
      }
    catch (ParseException e)
      {
        System.out.println("CreateSpinfoC encountered errors during parse.");
        System.out.println(e.getMessage());
      }
  }

}
