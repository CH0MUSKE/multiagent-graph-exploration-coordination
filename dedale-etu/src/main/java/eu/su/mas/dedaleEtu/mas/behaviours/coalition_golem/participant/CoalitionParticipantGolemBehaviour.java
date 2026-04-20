package eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.participant;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.SendMsgBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.PropagationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.ReceiveEchoBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.SendOrdersBehaviour;
import jade.core.behaviours.FSMBehaviour;

import java.util.List;

public class CoalitionParticipantGolemBehaviour extends FSMBehaviour {
    public CoalitionParticipantGolemBehaviour(final AbstractCommunicationAgent agent, long timeout, List<String> agentList, int depth, String conversation_id, String leader_id){
        super(agent);
        registerFirstState(new PropagationBehaviour(agent, depth, agentList, conversation_id,timeout, leader_id),"DIFFUSION");
        registerState(new ReceiveEchoBehaviour(agent, conversation_id, timeout),"WAITING_ECHO");
        registerState(new SendEchoBehaviour(agent), "SENDING_ECHO");
        registerState(new WaitingOrderBehaviour(agent), "WAITING_ORDER");
        registerState(new SendOrdersBehaviour(agent, conversation_id), "SENDING_ORDER_CHILDREN");

        
        registerTransition("DIFFUSION", "WAITING_ECHO",1);
        registerTransition("DIFFUSION", "SENDING_ECHO",0);
        
        registerDefaultTransition("WAITING_ECHO", "SENDING_ECHO");

        registerDefaultTransition("SENDING_ECHO", "WAITING_ORDER");
        
        registerDefaultTransition("WAITING_ORDER", "SENDING_ORDER_CHILDREN");



    }
}
