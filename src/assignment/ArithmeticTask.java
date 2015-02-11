package assignment;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class ArithmeticTask {
  private static int nextID = 0;

  public final double a, b;
  public final Operator op;
  public double answer = Double.NaN;

  public final int uniqueId;

  public ArithmeticTask(double a, double b, Operator op) {
    this.a = a;
    this.b = b;
    this.op = op;
    this.uniqueId = nextID;
    nextID++;
  }

  private ArithmeticTask(
    double a, double b, Operator op, double answer, int id
  ) {
    this.a = a;
    this.b = b;
    this.op = op;
    this.answer = answer;
    this.uniqueId = id;
  }

  public String readableDescription() {
    return String.format("%.2f%s%.2f", a, op.getSymbol(), b);
  }

  public String toJson() {
    JSONObject obj = new JSONObject();
    obj.put("a", a);
    obj.put("b", b);
    obj.put("op", op.getSymbol());
    obj.put("id", uniqueId);
    obj.put("answer", answer);
    return obj.toJSONString();
  }

  public static ArithmeticTask fromJson(String json) {
    JSONObject obj = (JSONObject) JSONValue.parse(json);
    return new ArithmeticTask(
      Double.parseDouble((String) obj.get("a")),
      Double.parseDouble((String) obj.get("b")),
      Operator.fromString((String) obj.get("op")),
      Double.parseDouble((String) obj.get("answer")),
      Integer.parseInt((String) obj.get("id"))
    );
  }
}
