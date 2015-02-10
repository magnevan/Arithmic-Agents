package assignment;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

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

  public String toJson() {
    JSONObject obj = new JSONObject();
    obj.put("a", a);
    obj.put("b", b);
    obj.put("op", op.getText());
    obj.put("id", uniqueId);
    return obj.toJSONString();
  }

  public static ArithmeticTask fromJson(String json) {
    JSONObject obj = (JSONObject) JSONValue.parse(json);
    return new ArithmeticTask(
      Double.parseDouble((String) obj.get("a")),
      Double.parseDouble((String) obj.get("b")),
      Operator.fromString((String) obj.get("op")),
      Integer.parseInt((String) obj.get("id"))
    );
  }
}
