package daikon.split;

import java.io.*;
import daikon.Daikon;

/**
 * Reads in and loads compiled Java source and returns a Java Object.
 **/
public class SplitterLoader extends ClassLoader {

  /**
   * Read in the bytes of the .class file.
   **/
  private byte[] read_Class_Data(String fileName) {

    try {
      FileInputStream fi = new FileInputStream(fileName);
      byte[] classBytes= new byte[fi.available()];
      fi.read(classBytes);
      fi.close();
      return classBytes;
    } catch (FileNotFoundException e) {
      if (! Daikon.dkconfig_suppressSplitterErrors) {
        System.out.println("File "
                           + fileName.substring(0, fileName.length()-6)
                           + ".java did not compile");
      }
      // do nothing. did not compile
    } catch (IOException ioe) {
      System.out.println("IO Error while reading class data " + fileName);
    }
    return null;
  }

  /**
   * @param full_pathname the pathname of a .class file
   * @return a Java Object corresponding to the .class file
   **/
  protected Class load_Class(String className, String full_pathname) {
    Class return_class;
    byte[] classData = read_Class_Data(full_pathname);
    if (classData == null) {
      return null;
    } else {
      return_class = defineClass(className, classData, 0, classData.length);
      resolveClass(return_class);
    }
    return return_class;
  }

}
