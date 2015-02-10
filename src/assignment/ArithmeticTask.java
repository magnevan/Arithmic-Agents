package assignment;

public class ArithmeticTask {
  final int a, b;
  final Operator op;

  public ArithmeticTask(int a, int b, Operator op) {
    this.a = a;
    this.b = b;
    this.op = op;
  }


  public static ArithmeticTask fromString(String content) {
    String[] parts = content.split(",");
    int a, b;
    Operator op;
    try {
      a = Integer.parseInt(parts[0]);
      b = Integer.parseInt(parts[1]);
      op = stringToOperator(parts[2]);
    } catch (Exception e) {
      return null;
    }

    return new ArithmeticTask(a, b, op);
  }

  private static Operator stringToOperator(String op) throws Exception {
    if      (op == "+") return Operator.ADD;
    else if (op == "-") return Operator.SUBTRACT;
    else if (op == "*") return Operator.MULTIPLY;
    else if (op == "/") return Operator.DIVIDE;
    else throw new Exception();
  }
}
