package assignment;

public enum Operator {
  ADD("+", "addition"),
  SUBTRACT("-", "subtraction"),
  MULTIPLY("*", "multiplication"),
  DIVIDE("/", "division");

  private final String symbol;
  private final String text;

  Operator(String symbol, String text) {
    this.symbol = symbol;
    this.text = text;
  }

  public String getSymbol() { return this.symbol; }
  public String getText() { return this.text; }

  /**
   * Turns a textual representation of an operator to the enum operator
   * @param symbol the symbol to convert, must be * - + or /
   * @return the Operator or null in case of incorrect input.
   */
  public static Operator fromString(String symbol) {
    if (symbol == null) return null;
    for (Operator op : Operator.values()) {
      if (op.symbol.equals(symbol)) {
        return op;
      }
    }
    return null;
  }
}
