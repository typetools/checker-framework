package utilMDE;

import java.io.*;
import java.util.jar.*;
import java.util.*;

/**
 * Given a list of .class files, print the class file version and also the
 * JDK/JRE version required to run it.
 * A .jar file can also be supplied, in which case each .class file within
 * it is procesed.
 * Example use:  java ClassFileVersion MyClass.class
 * <p>
 * Supplying the "-min JDKVER" argument suppresses output except for .class
 * files that require at least that JDK version.  For instance, to list all
 * the .class/.jar files that require JDK 6 or later, in this or any
 * subdirectory, run
 *   find . \( -name '*.class' -o -name '*.jar' \) -print | xargs java ClassFileVersion -min 6
 **/
public class ClassFileVersion {

  /** Only report versions that are at least this large. **/
  static double minversion = 0;

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println("Supplied no arguments.");
      System.out.println("Usage: java ClassFileVersion [-min JDKVER] <.class or .jar files>");
      System.exit(1);
    }

    if ((args.length >= 2)
        && (args[0].equals("-min"))) {
      minversion = Double.parseDouble(args[1]);
      if (minversion == 1.6)
        minversion = 6;
      else if (minversion == 1.7)
        minversion = 7;
      String[] newargs = new String[args.length - 2];
      System.arraycopy(args, 2, newargs, 0, args.length - 2);
      args = newargs;
    }

    // System.out.println("newargs: " + java.util.Arrays.toString(args));

    for (String filename : args) {
      if (! new File(filename).exists()) {
        System.out.println(filename + " does not exist!");
        continue;
      }

      if (filename.endsWith(".class")) {
        processClassFile(filename, new FileInputStream(filename));
      } else if (filename.endsWith(".jar")) {
        JarFile jarFile = new JarFile(filename);
        for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
          JarEntry entry = e.nextElement();
          String entryName = entry.getName();
          // Should really process recursively included jar files...
          if (entryName.endsWith(".class")) {
            InputStream is = jarFile.getInputStream(entry);
            processClassFile(filename + ":" + entryName, is);
          }
        }
      } else {
        System.out.println(filename + " is neither a .class nor a .jar file");
      }
    }
  }

  public static void processClassFile(String filename, InputStream is) {
    double[] versions = versionNumbers(is);
    if (versions == null) {
      System.out.println(filename + " is not a .class file (or IOException)");
    } else {
      double major = versions[0];
      double minor = versions[1];
      double jdkVersion = versions[2];

      if (jdkVersion >= minversion) {
        System.out.println(filename + " class file version is "
                           + (int)major + "." + (int)minor + ", requires JDK "
                           + ((jdkVersion == (int)jdkVersion)
                              ? Integer.toString((int)jdkVersion) : Double.toString(jdkVersion))
                           + " or later");
      }
    }
  }


  public static double[] versionNumbers(InputStream is) {
    try {
      DataInputStream dis = new DataInputStream(is);
      int magic = dis.readInt();
      if (magic != 0xcafebabe) {
        return null;
      }

      double minor = dis.readShort();
      double major = dis.readShort();
      double jdkVersion;

      if (major < 48) {
        jdkVersion = 1.3;          // really 1.3.1
      } else if (major == 48) {
        jdkVersion = 1.4;          // really 1.4.2
      } else if (major == 49) {
        jdkVersion = 1.5;
      } else if (major == 50) {
        jdkVersion = 6;
      } else {
        jdkVersion = 7;
      }

      return new double[] { major, minor, jdkVersion };
    } catch (IOException e) {
      return null;
    }

  }

}
