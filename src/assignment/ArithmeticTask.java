package assignment;

public class ArithmeticTask {
  private static int nextID = 0;

  final double a, b;
  final Operator op;

  public final int uniqueId;

  public ArithmeticTask(double a, double b, Operator op) {
    this.a = a;
    this.b = b;
    this.op = op;
    this.uniqueId = nextID;
    nextID++;
  }

  private ArithmeticTask(double a, double b, Operator op, int id) {
    this.a = a;
    this.b = b;
    this.op = op;
    this.uniqueId = id;
  }

  public static ArithmeticTask fromString(String content) {
    String[] parts = content.split(",");
    double a, b;
    Operator op;
    try {
      a = Double.parseDouble(parts[0]);
      b = Double.parseDouble(parts[1]);
      op = stringToOperator(parts[2]);
    } catch (Exception e) {
      return null;
    }

    return new ArithmeticTask(a, b, op, 0); // TODO fix parsing
  }

  private static Operator stringToOperator(String op) throws Exception {
    if      (op == "+") return Operator.ADD;
    else if (op == "-") return Operator.SUBTRACT;
    else if (op == "*") return Operator.MULTIPLY;
    else if (op == "/") return Operator.DIVIDE;
    else throw new Exception();
  }
}
