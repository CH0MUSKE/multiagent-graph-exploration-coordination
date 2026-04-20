package eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.SendMsgBehaviour;
import eu.su.mas.dedaleEtu.mas.utils.MessageFactory;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.List;

public class PropagationBehaviour extends OneShotBehaviour {
    AbstractCommunicationAgent agent;
    List<String> agentList;
    int depth;
    String conversation_id;
    private int exitValue = 1;
    private final long timeout;
    private String leader_id;

    public PropagationBehaviour(final AbstractCommunicationAgent agent, int depth, List<String> agentList, String conversation_id, long timeout, String leader_id){
        super(agent);
        this.agent = agent;
        this.agentList = agentList;
        this.depth = depth - 1;
        this.conversation_id = conversation_id;
        this.leader_id = leader_id;
        // hard codé, penser à changer celui de waitingorder si je modifie 
        long eps = 200;
        this.timeout = timeout - eps;
    }
    @Override
    public void action(){
        if(this.depth <= 0 || this.agentList == null || this.agentList.isEmpty() || this.timeout <= 0){
            System.out.println("Erreur: depth <= 1 ou agentList vide ou timeout <= 0");
            this.exitValue = 0;
            return;
        }
        String[] content = new String[4];
        content[0] = this.leader_id;
        content[1] = this.agent.getGolemPos();
        content[2] = String.valueOf(this.timeout);
        content[3] = String.valueOf((this.depth));
        ACLMessage msg = MessageFactory.buildMsg(this.agent.getAID(), this.agentList, "FLOOD_GOLEM", content, this.conversation_id);
        this.agent.sendMessage(msg);
    }
    
    @Override
    public int onEnd(){
        return this.exitValue;
    }
}
