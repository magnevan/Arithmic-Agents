package assignment;

public enum Operator {
  ADD("+"),
  SUBTRACT("-"),
  MULTIPLY("*"),
  DIVIDE("/");

  private String text;

  Operator(String text) {
    this.text = text;
  }

  public String getText() { return this.text; }

  /**
   * Turns a textual representation of an operator to the enum operator
   * @param text the text to convert, must be * - + or /
   * @return the Operator or null in case of incorrect input.
   */
  public static Operator fromString(String text) {
    if (text == null) return null;
    for (Operator op : Operator.values()) {
      if (op.text.equals(text)) {
        return op;
      }
    }
    return null;
  }
}
