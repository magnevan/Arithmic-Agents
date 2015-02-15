package vikjord_wangsholm;

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

import java.util.*;

/**
 * Created by Magne on 10-Feb-15.
 */
public class TaskAdministrator extends Agent {

  protected List<JobElement> job;
  protected Map<Integer, JobElement> taskToJobElement;
  protected ACLMessage queryRef;

  @Override
  protected void setup() {
    System.out.println("Started up TaskAdministrator");

    job = new ArrayList<>();
    taskToJobElement = new HashMap<>();

    addBehaviour(new ReceiveSolutionsBehavior());
    addBehaviour(new AcceptTask());
  }

  private class AcceptTask extends OneShotBehaviour {

    @Override
    public void action() {
      job.clear();
      taskToJobElement.clear();

      queryRef = myAgent.blockingReceive(
        MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF)
      );

      String jobString = queryRef.getContent();
      for (String part : jobString.split(" ")) {
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
          System.err.printf("Failed to parse job %s\n", part);
        }
      }

      addBehaviour(new FindSubtasksAndAuctionBehavior());
    }
  }

  /**
   * Strategy for finding subtasks is to look through the job list
   * until you find an operator (where started = false). Then checking
   * if the previous two elements in the list is numbers. If so, set these
   * as started, package up the problem and continue looking through the list.
   * This gives you a good amount of parallelization if the problem is formed
   * properly: 5 2 + 1 7 + + is good 5 2 1 7 + + + is bad, even though
   * the problems are identical.
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
        System.out.printf("Handing out task '%s'\n", task.readableDescription());

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
        long endTimeMs = System.currentTimeMillis() + 5000;
        int proposalsRemaining = agents.length;
        ACLMessage bestProposal = null;
        int bestTime = Integer.MAX_VALUE;
        List<ACLMessage> allProposals = new ArrayList<>();

        while (
            proposalsRemaining > 0 &&
                endTimeMs > System.currentTimeMillis()
            ) {
          ACLMessage proposal =
              myAgent.blockingReceive(mt, endTimeMs - System.currentTimeMillis());

          if (proposal == null) continue;
          allProposals.add(proposal);
          proposalsRemaining--;
          int offer = Integer.parseInt(proposal.getContent());
          System.out.printf(
              "Received new proposal %d ms from %s\n",
              offer, proposal.getSender().getName()
          );
          if (offer < bestTime) {
            bestTime = offer;
            bestProposal = proposal;
          }
        }

        if (bestProposal != null) {
          // Send acceptance
          System.out.printf("Accepting proposal from %s for task %s\n",
              bestProposal.getSender().getLocalName(),
              task.readableDescription()
          );
          ACLMessage acceptance = bestProposal.createReply();
          acceptance.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
          acceptance.setContent(task.toJson());
          myAgent.send(acceptance);

          // Send rejections to the rest of the proposals
          for (ACLMessage proposal : allProposals) {
            if (proposal == bestProposal) continue;

            ACLMessage rejection = proposal.createReply();
            rejection.setPerformative(ACLMessage.REJECT_PROPOSAL);
            myAgent.send(rejection);
          }


        } else {
          System.out.printf("Did not find an agent to solve %s within the time limit!\n",
              task.readableDescription());
        }
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
          System.out.println("    " + sellerAgents[i].getName());
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

        System.out.printf(
          "Received answer to %s = %.2f from %s\n",
          task.readableDescription(),
          task.answer,
          msg.getSender().getName()
        );

        JobElement answer = new JobElement(task.answer);
        job.add(index-2, answer);
        receivedAnswers = true;
      }

      if (receivedAnswers) {
        System.out.printf("New problem is: %s\n", job.toString());
        if (job.size() != 1) {
          myAgent.addBehaviour(new FindSubtasksAndAuctionBehavior());
        } else {
          ACLMessage reply = queryRef.createReply();
          reply.setPerformative(ACLMessage.INFORM);
          reply.setContent(job.get(0).toString());
          myAgent.send(reply);
          addBehaviour(new AcceptTask());
        }
      }

      if (myAgent.getCurQueueSize() == 0) block();
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

    @Override
    public String toString() {
      if (op != null) {
        return op.getSymbol();
      } else {
        return number.toString();
      }
    }
  }
}
