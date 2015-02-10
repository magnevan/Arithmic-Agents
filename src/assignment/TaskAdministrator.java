package assignment;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by Magne on 10-Feb-15.
 */
public class TaskAdministrator extends Agent {

  protected List<JobElement> job;

  protected Queue<ArithmeticTask> tasksAwaitingHandout;
  protected Map<ArithmeticTask, JobElement> taskToJobElement;

  @Override
  protected void setup() {
    System.out.println("Started up TaskAdministrator");

    job = new ArrayList<>();
    tasksAwaitingHandout = new LinkedList<>();
    taskToJobElement = new HashMap<>();

    // The input is a space separated list of numbers and operators
    String[] args = (String[])getArguments();
    for (String part : args) {
      // Control flow is a little strange here because default java doesn't
      // let you check if a string is a valid double without trying to parse
      // and catching the error.
      try {
        job.add(new JobElement(Double.parseDouble(part)));
        continue;
      } catch (NumberFormatException e) {}

      Operator op = Operator.fromString(part);
      if (op != null) {
        job.add(new JobElement(op));
      } else {
        System.err.printf("Failed to parse argument %s\n", part);
        doDelete();
      }
    }

    addBehaviour(new FindSubtasksBehavior());
  }

  /**
   * Strategy for finding subtasks is to look through the job list
   * until you find an operator (where started = false). Then checking
   * if the previous two elements in the list is numbers. If so, set these
   * as started, package up the problem and continue looking through the list.
   * This way we get the maximum parallelization possible (I think).
   */
  private class FindSubtasksBehavior extends OneShotBehaviour {

    @Override
    public void action() {
      ListIterator<JobElement> iter
        = ((TaskAdministrator)myAgent).job.listIterator();

      for (int i = 0; i < job.size(); i++) {
        JobElement el = job.get(i);
        if (el.isOperator() && !el.isStarted()) {
          // Check if the previous two are numbers
          if (i >= 2) {
            JobElement prev1 = job.get(i-1);
            JobElement prev2 = job.get(i-2);

            if (prev1.isNumber() && prev2.isNumber()) {
              prev1.start();
              prev2.start();
              el.start();
              ArithmeticTask task = new ArithmeticTask(
                prev2.number,
                prev1.number,
                el.op
              );
              tasksAwaitingHandout.add(task);
              taskToJobElement.put(task, el);
            }
          }
        }
      }
    }
  }

  /**
   * A job might look like "5 4 + 2 *", which is postfix and means
   * (5 + 4) * 2. This class represents one of the characters in this string.
   */
  private class JobElement {

    private boolean started = false;

    // One of these two will be set.
    public final Double number;
    public final Operator op;

    JobElement(double number) {
      this.number = number;
      op = null;
    }
    JobElement(Operator op) {
      this.number = null;
      this.op = op;
    }
    public boolean isOperator() { return this.op != null; }
    public boolean isNumber() { return !isOperator(); }

    public void start() { started = true; }
    public boolean isStarted() { return started; }
  }
}
