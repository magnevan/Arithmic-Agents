package assignment;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.tools.sniffer.Message;

import java.util.*;

/**
 * Created by Magne on 10-Feb-15.
 */
public class TaskAdministrator extends Agent {

  protected List<JobElement> job;

  protected Map<Integer, JobElement> taskToJobElement;

  @Override
  protected void setup() {
    System.out.println("Started up TaskAdministrator");

    job = new ArrayList<>();
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

    addBehaviour(new FindSubtasksAndAuctionBehavior());
    addBehaviour(new ReceiveSolutionsBehavior());
  }

  /**
   * Strategy for finding subtasks is to look through the job list
   * until you find an operator (where started = false). Then checking
   * if the previous two elements in the list is numbers. If so, set these
   * as started, package up the problem and continue looking through the list.
   * This way we get the maximum parallelization possible (I think).
   */
  private class FindSubtasksAndAuctionBehavior extends OneShotBehaviour {

    @Override
    public void action() {

      Queue<ArithmeticTask> tasksAwaitingHandout = new LinkedList<>();

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
              taskToJobElement.put(task.uniqueId, el);
            }
          }
        }
      }

      while (!tasksAwaitingHandout.isEmpty()) {
        ArithmeticTask task = tasksAwaitingHandout.poll();

        // Sending CFP
        String biddingRoundIdentifier = "CFP-task-" + task.uniqueId;
        AID[] agents = findAgentsRenderingService(task.op);
        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        for (AID agent : agents)
          cfp.addReceiver(agent);
        cfp.setContent(task.toJson());
        cfp.setConversationId(biddingRoundIdentifier);
        myAgent.send(cfp);

        // Receiving proposals
        MessageTemplate mt = MessageTemplate.and(
          MessageTemplate.MatchConversationId(biddingRoundIdentifier),
          MessageTemplate.MatchPerformative(ACLMessage.PROPOSE)
        );

        // only wait 1 second for all proposals to come in
        long endTimeMs = System.currentTimeMillis() + 1000;
        int proposalsRemaining = agents.length;
        AID bestAgent = null;
        int bestTime = Integer.MAX_VALUE;

        while (
          proposalsRemaining > 0 &&
          endTimeMs > System.currentTimeMillis()
        ) {
          ACLMessage proposal =
            myAgent.blockingReceive(mt, endTimeMs - System.currentTimeMillis());

          if (proposal == null) continue;
          proposalsRemaining--;
          int offer = Integer.parseInt(proposal.getContent());
          if (offer < bestTime) {
            bestTime = offer;
            bestAgent = proposal.getSender();
          }
        }

        // Send acceptance
        ACLMessage acceptance = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
        acceptance.addReceiver(bestAgent);
        acceptance.setContent(task.toJson());
        myAgent.send(acceptance);
      }
    }

    private AID[] findAgentsRenderingService(Operator op) {
      AID[] sellerAgents = null;
      DFAgentDescription template = new DFAgentDescription();
      ServiceDescription sd = new ServiceDescription();
      sd.setType(op.getText());
      template.addServices(sd);
      try {
        DFAgentDescription[] result = DFService.search(myAgent, template);
        System.out.printf(
          "Found the following agents doing %s:\n",
          op.getText()
        );
        sellerAgents = new AID[result.length];
        for (int i = 0; i < result.length; ++i) {
          sellerAgents[i] = result[i].getName();
          System.out.println(sellerAgents[i].getName());
        }
      }
      catch (FIPAException fe) {
        fe.printStackTrace();
      }

      return sellerAgents;
    }
  }

  private class ReceiveSolutionsBehavior extends CyclicBehaviour {

    @Override
    public void action() {
      boolean receivedAnswers = false;

      while (true) {
        ACLMessage msg = myAgent.receive(
          MessageTemplate.MatchPerformative(ACLMessage.INFORM)
        );
        if (msg == null) break;

        ArithmeticTask task = ArithmeticTask.fromJson(msg.getContent());
        // Remove the pieces of the job relating to this task and replace
        // with the answer. Example: job is 5 2 4 + 1 - + and we get back
        // the answer 6 for (2 4 +). New job is 5 6 1 - +.
        JobElement op = taskToJobElement.remove(task.uniqueId);
        int index = job.indexOf(op);
        job.remove(index);
        job.remove(index-1);
        job.remove(index-2);

        JobElement answer = new JobElement(task.answer);
        job.add(index-2, answer);
        receivedAnswers = true;
      }

      if (receivedAnswers) {
        myAgent.addBehaviour(new FindSubtasksAndAuctionBehavior());
      }

      block();
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
