package org.checkerframework.afu.scenelib.io;

import com.sun.tools.javac.code.TargetType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.AnnotationBuilder;
import org.checkerframework.afu.scenelib.AnnotationFactory;
import org.checkerframework.afu.scenelib.Annotations;
import org.checkerframework.afu.scenelib.el.AClass;
import org.checkerframework.afu.scenelib.el.AElement;
import org.checkerframework.afu.scenelib.el.AMethod;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.afu.scenelib.el.LocalLocation;
import org.checkerframework.afu.scenelib.el.RelativeLocation;
import org.checkerframework.afu.scenelib.el.TypePathEntry;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.plumelib.util.FileIOException;

/**
 * <code>JavapParser</code> provides a static method that parses a class dump in the form produced
 * by <code>xjavap -s -verbose -annotations</code> and adds the annotations to an {@link AScene},
 * using the scene's {@link AnnotationFactory} to build individual annotations. If the scene's
 * {@link AnnotationFactory} announces that it does not want an annotation found in the javap
 * output, that annotation is skipped. Annotations from the javap output are merged into the scene;
 * it is an error if both the scene and the javap output contain annotations of the same type on the
 * same element.
 *
 * <p>THIS CLASS IS NOT FINISHED YET!
 *
 * <p>This class does not yet perform any error checking. Expect strange behavior and/or exceptions
 * if you give it bad input.
 */
public final class JavapParser {
  private static final String SECTION_TITLE_PREFIX = "  ";
  private static final String SECTION_DATA_PREFIX = "   ";
  private static final String CONST_POOL_DATA_PREFIX = "const #";

  private final AScene scene;

  private final BufferedReader bin;
  private String line; // null means end-of-file

  private int lineNo = 0; // TEMP

  private void nextLine() throws IOException {
    do {
      line = bin.readLine();
      lineNo++;
    } while (line != null && line.equals(""));
  }

  private void trim(String prefix) {
    if (line.startsWith(prefix)) {
      line = line.substring(prefix.length());
    }
  }

  private boolean inMember() {
    return line.startsWith(SECTION_TITLE_PREFIX);
  }

  private boolean inData() {
    return line.startsWith(SECTION_DATA_PREFIX) || line.startsWith(CONST_POOL_DATA_PREFIX);
  }

  private enum TargetMode {
    ORIGINAL,
    PARAMETER,
    EXTENDED
  }

  // This name comes from the section of the javap output that is being read.
  private enum AnnotationSection {
    RVA("RuntimeVisibleAnnotations", RetentionPolicy.RUNTIME, TargetMode.ORIGINAL),
    RIA("RuntimeInvisibleAnnotations", RetentionPolicy.CLASS, TargetMode.ORIGINAL),
    RVPA("RuntimeVisibleParameterAnnotations", RetentionPolicy.RUNTIME, TargetMode.PARAMETER),
    RIPA("RuntimeInvisibleParameterAnnotations", RetentionPolicy.CLASS, TargetMode.PARAMETER),
    RVEA("RuntimeVisibleTypeAnnotations", RetentionPolicy.RUNTIME, TargetMode.EXTENDED),
    RIEA("RuntimeInvisibleTypeAnnotations", RetentionPolicy.CLASS, TargetMode.EXTENDED),
    ;

    final String secTitle;
    final RetentionPolicy retention;
    final TargetMode locMode;

    AnnotationSection(String secTitle, RetentionPolicy retention, TargetMode locMode) {
      this.secTitle = secTitle;
      this.retention = retention;
      this.locMode = locMode;
    }
  }

  /**
   * Parse the head of an annotation definition.
   *
   * @return the binary name of the annotation
   * @throws IOException if there is trouble reading the index file
   * @throws ParseException if the file contents are not valid
   */
  private @BinaryName String parseAnnotationHead() throws IOException, ParseException {
    @SuppressWarnings("signature:assignment") // string manipulation
    @BinaryName String annoTypeName =
        line.substring(line.indexOf(annotationHead) + annotationHead.length(), line.length() - 1)
            .replace('/', '.');
    nextLine();
    return annoTypeName;
  }

  private static final String annotationHead = "//Annotation L"; // TEMP
  private static final String tagHead = "type = "; // TEMP

  private Annotation parseAnnotationBody(AnnotationBuilder ab, String indent)
      throws IOException, ParseException {
    // Grab the fields
    String fieldIndent = indent + " ";
    while (line.startsWith(fieldIndent)) {
      String line2 = line.substring(fieldIndent.length());
      // Let the caller deal with location information, if any
      if (line2.startsWith("target") || line2.startsWith("parameter")) {
        break;
      }
      // String fieldName = line2.substring(line2.indexOf("//") + "//".length());
      nextLine();
      char tag = line.charAt(line.indexOf(tagHead) + tagHead.length());
      switch (tag) {
        case '[':
          break;
        case '@':
          break;
        case 'c':
          break;
        case 'e':
          break;
      }
      // FINISH
    }
    return ab.finish();
  }

  private static final String paramIdxHead = "parameter = ";
  private static final String offsetHead = "offset = ";
  private static final String typeIndexHead = "type_index = ";
  private static final Pattern localLocRegex =
      Pattern.compile("^\\s*start_pc = (\\d+), length = (\\d+), index = (\\d+)$");
  private static final String itlnHead = "location = ";

  private int parseOffset() throws IOException, ParseException {
    int offset = Integer.parseInt(line.substring(line.indexOf(offsetHead) + offsetHead.length()));
    nextLine();
    return offset;
  }

  private int parseTypeIndex() throws IOException, ParseException {
    int typeIndex =
        Integer.parseInt(line.substring(line.indexOf(typeIndexHead) + typeIndexHead.length()));
    nextLine();
    return typeIndex;
  }

  private List<Integer> parseInnerTypeLocationNums() throws IOException, ParseException {
    String numsStr = line.substring(line.indexOf(itlnHead) + itlnHead.length());
    List<Integer> nums = new ArrayList<>();
    for (; ; ) {
      int comma = numsStr.indexOf(',');
      if (comma == -1) {
        nums.add(Integer.parseInt(numsStr));
        break;
      }
      nums.add(Integer.parseInt(numsStr.substring(0, comma)));
      numsStr = numsStr.substring(comma + 2);
    }
    nextLine();
    return nums;
  }

  private AElement chooseSubElement(AElement member, AnnotationSection sec)
      throws IOException, ParseException {
    switch (sec.locMode) {
      case ORIGINAL:
        // There can be no location information.
        return member;
      case PARAMETER:
        {
          // should have a "parameter = "
          int paramIdx =
              Integer.parseInt(line.substring(line.indexOf(paramIdxHead) + paramIdxHead.length()));
          nextLine();
          return ((AMethod) member).parameters.getVivify(paramIdx);
        }
      case EXTENDED:
        // should have a "target = "
        String targetTypeName = line.substring(line.indexOf("//") + "//".length());
        TargetType targetType;
        TargetType tt = TargetType.valueOf(targetTypeName);
        if (tt != null) {
          targetType = tt;
        } else {
          throw new RuntimeException("null target type");
        }
        nextLine();
        ATypeElement subOuterType;
        AElement subElement;
        switch (targetType) {
          case FIELD:
          case METHOD_RETURN:
            subOuterType = (ATypeElement) member;
            break;
          case METHOD_RECEIVER:
            subOuterType = ((AMethod) member).receiver.type;
            break;
          case METHOD_FORMAL_PARAMETER:
            int paramIdx =
                Integer.parseInt(
                    line.substring(line.indexOf(paramIdxHead) + paramIdxHead.length()));
            nextLine();
            subOuterType = ((AMethod) member).parameters.getVivify(paramIdx).type;
            break;
          case LOCAL_VARIABLE:
          case RESOURCE_VARIABLE:
            int index, scopeStart, scopeLength;
            Matcher m = localLocRegex.matcher(line);
            m.matches();
            index = Integer.parseInt(m.group(1));
            scopeStart = Integer.parseInt(m.group(2));
            scopeLength = Integer.parseInt(m.group(3));
            LocalLocation ll = new LocalLocation(scopeStart, scopeLength, index);
            nextLine();
            subOuterType = ((AMethod) member).body.locals.getVivify(ll).type;
            break;
          case CAST:
            {
              int offset = parseOffset();
              int typeIndex = parseTypeIndex();
              subOuterType =
                  ((AMethod) member)
                      .body.typecasts.getVivify(RelativeLocation.createOffset(offset, typeIndex));
              break;
            }
          case INSTANCEOF:
            {
              int offset = parseOffset();
              subOuterType =
                  ((AMethod) member)
                      .body.instanceofs.getVivify(RelativeLocation.createOffset(offset, 0));
              break;
            }
          case NEW:
            {
              int offset = parseOffset();
              subOuterType =
                  ((AMethod) member).body.news.getVivify(RelativeLocation.createOffset(offset, 0));
              break;
            }
          default:
            throw new AssertionError();
        }
        // TODO: update location representation
        // if (targetType.) {
        List<Integer> location = parseInnerTypeLocationNums();
        List<TypePathEntry> typePath = TypePathEntry.getTypePathEntryListFromBinary(location);
        subElement = subOuterType.innerTypes.getVivify(typePath);
        // } else
        //    subElement = subOuterType;
        return subElement;
      default:
        throw new AssertionError();
    }
  }

  private void parseAnnotationSection(AElement member, AnnotationSection sec)
      throws IOException, ParseException {
    // FILL
    while (inData()) {
      String annoTypeName = parseAnnotationHead();
      RetentionPolicy retention = sec.retention;
      AnnotationBuilder ab =
          AnnotationFactory.saf.beginAnnotation(
              annoTypeName,
              Annotations.getRetentionPolicyMetaAnnotationSet(retention),
              "JavapParser");
      if (ab == null) {
        // don't care about the result
        // but need to skip over it anyway
        parseAnnotationBody(
            AnnotationFactory.saf.beginAnnotation(
                annoTypeName, Annotations.noAnnotations, "JavapParser"),
            SECTION_DATA_PREFIX);
      } else {
        // Wrap it in a TLA with the appropriate retention policy
        Annotation a = parseAnnotationBody(ab, SECTION_DATA_PREFIX);
        // Now we need to parse the location information to determine
        // which element gets the annotation.
        AElement annoMember = chooseSubElement(member, sec);
        annoMember.tlAnnotationsHere.add(a);
      }
    }
  }

  private void parseMember(AElement member) throws IOException, ParseException {
    while (inMember()) {
      // New section
      String secTitle = line.substring(2, line.indexOf(':'));
      AnnotationSection sec0 = null;
      for (AnnotationSection s : AnnotationSection.values()) {
        if (s.secTitle.equals(secTitle)) {
          sec0 = s;
        }
      }
      if (sec0 != null) {
        AnnotationSection sec = sec0;
        nextLine();
        System.out.println("Got section " + secTitle);
        parseAnnotationSection(member, sec);
      } else {
        System.out.println("Got unrecognized section " + secTitle);
        nextLine();
        // Skip the section
        while (inData()) {
          nextLine();
        }
      }
    }
  }

  private void parseMethodBody(AElement clazz, String methodName)
      throws IOException, ParseException {
    String sig = line.substring((SECTION_TITLE_PREFIX + "Signature: ").length());
    nextLine();
    String methodKey = methodName + sig;
    System.out.println("Got method " + methodKey); // TEMP
    parseMember(((AClass) clazz).methods.getVivify(methodKey));
  }

  // the "clazz" might actually be a package in case of "interface package-info"
  private void parseClass(AElement clazz) throws IOException, ParseException {
    parseMember(clazz);

    nextLine(); // {

    while (!line.equals("}")) {
      // new member
      if (line.indexOf("static {}") >= 0) {
        nextLine();
        parseMethodBody(clazz, "<clinit>");
      } else {
        int lparen = line.indexOf('(');
        if (lparen == -1) {
          // field
          int space = line.lastIndexOf(' ');
          String fieldName = line.substring(space + 1, line.length() - 1);
          nextLine();
          System.out.println("Got field " + fieldName); // TEMP
          parseMember(((AClass) clazz).fields.getVivify(fieldName));
        } else {
          // method
          int space = line.lastIndexOf(' ', lparen);
          String methodName = line.substring(space + 1, lparen);
          nextLine();
          parseMethodBody(clazz, methodName);
        }
      }
    }
    nextLine(); // }
  }

  private void parse() throws IOException, ParseException {
    try { // TEMP
      nextLine(); // get the first line

      while (line != null) {
        // new class
        nextLine();
        trim("public ");
        trim("protected ");
        trim("private ");
        trim("abstract ");
        trim("final ");
        trim("class ");
        trim("interface ");
        int nameEnd = line.indexOf(' ');
        String className = (nameEnd == -1) ? line : line.substring(0, line.indexOf(' '));
        String pp = IOUtils.packagePart(className);
        String bp = IOUtils.basenamePart(className);
        nextLine();
        if (bp.equals("package-info")) {
          parseClass(scene.packages.getVivify(pp));
        } else {
          parseClass(scene.classes.getVivify(className));
        }
      }
    } catch (RuntimeException e) {
      throw new RuntimeException("Line " + lineNo, e);
    }
  }

  private JavapParser(Reader in, AScene scene) {
    bin = new BufferedReader(in);
    this.scene = scene;
  }

  /** Transfers annotations from <code>in</code> to <code>scene</code>. */
  public static void parse(Reader in, AScene scene) throws IOException, ParseException {
    new JavapParser(in, scene).parse();
  }

  public static void parse(String filename, AScene scene) throws IOException, FileIOException {
    try (LineNumberReader lnr =
        new LineNumberReader(
            Files.newBufferedReader(Paths.get(filename), StandardCharsets.UTF_8))) {
      try {
        parse(lnr, scene);
      } catch (ParseException e) {
        throw new FileIOException(lnr, filename, e);
      }
    }
  }
}
