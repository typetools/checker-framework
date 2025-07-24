package org.checkerframework.afu.annotator.find;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.afu.scenelib.type.Type;

public class ConstructorInsertion extends TypedInsertion {
  private ReceiverInsertion receiverInsertion = null;
  private Set<Insertion> declarationInsertions = new LinkedHashSet<>();

  /**
   * Construct a ConstructorInsertion.
   *
   * <p>To insert the annotation and the constructor (for example, {@code @Anno Type this}) the name
   * should be set to the type to insert. This can either be done before calling this constructor,
   * or by modifying the return value of {@link #getType()}.
   *
   * @param type the type to use when inserting the constructor
   * @param criteria where to insert the text
   * @param innerTypeInsertions the inner types to go on this constructor
   */
  public ConstructorInsertion(Type type, Criteria criteria, List<Insertion> innerTypeInsertions) {
    super(type, criteria, true, innerTypeInsertions);
  }

  @Override
  protected String getText(boolean abbreviate) {
    StringBuilder b = new StringBuilder();
    if (annotationsOnly) {
      // List<String> annotations = type.getAnnotations();
      // if (annotations.isEmpty()) { return ""; }
      // for (String a : annotations) {
      //  b.append(a);
      //  b.append(' ');
      // }
      // return new AnnotationInsertion(b.toString(), getCriteria(),
      //    isSeparateLine()).getText(abbreviate);
      return "";
    } else {
      String typeString = typeToString(type, true);
      int dollarPos = typeString.lastIndexOf('$');
      if (dollarPos != -1) {
        typeString = typeString.substring(dollarPos + 1);
        if (typeString.startsWith("1")) {
          // HACK: A named class `Local` within a method is recorded in the classfile as
          // Outer$1Local rather than as Outer$Local.
          // This hack doesn't handle when there are multiple such named classes in different
          // methods, nor when a nested class's name really starts with "1".  (I could do the latter
          // by checking
          //   String outerName = typeString.substring(0, dollarPos);
          //   Class<?> outer = Class.forName(outerName);
          //   Class<?>[] nested = outer.getClasses();
          typeString = typeString.substring(1);
        }
      }

      for (Insertion i : declarationInsertions) {
        b.append(i.getText(abbreviate)).append(System.lineSeparator());
        if (abbreviate) {
          packageNames.addAll(i.getPackageNames());
        }
      }
      b.append("public ").append(typeString).append("(");
      if (receiverInsertion != null && !receiverInsertion.isInserted()) {
        b.append(receiverInsertion.getText(abbreviate));
      }
      b.append(") { super(); }");
      return b.toString();
    }
  }

  protected ReceiverInsertion getReceiverInsertion() {
    return receiverInsertion;
  }

  public void addReceiverInsertion(ReceiverInsertion recv) {
    if (receiverInsertion == null) {
      receiverInsertion = recv;
    } else {
      receiverInsertion.getInnerTypeInsertions().addAll(recv.getInnerTypeInsertions());
    }
  }

  public void addDeclarationInsertion(Insertion ins) {
    declarationInsertions.add(ins);
    ins.setInserted(true);
  }

  @Override
  protected boolean addLeadingSpace(boolean gotSeparateLine, int pos, char precedingChar) {
    return false;
  }

  @Override
  protected boolean addTrailingSpace(boolean gotSeparateLine) {
    return false;
  }

  @Override
  public Kind getKind() {
    return Kind.CONSTRUCTOR;
  }

  /**
   * Sets whether this insertion has already been inserted into source code.
   *
   * @param inserted {@code true} if this insertion has already been inserted, {@code false}
   *     otherwise.
   */
  @Override
  public void setInserted(boolean inserted) {
    super.setInserted(false);
    if (receiverInsertion != null) {
      receiverInsertion.setInserted(false);
    }
    for (Insertion insertion : declarationInsertions) {
      insertion.setInserted(false);
    }
  }

  @Override
  public String toString() {
    return "\"" + getText().replace(System.lineSeparator(), " ") + "\" " + super.toString();
  }
}
