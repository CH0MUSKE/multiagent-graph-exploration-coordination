package eu.su.mas.dedaleEtu.mas.hunt.behaviours.coalition_golem.participant;

import eu.su.mas.dedaleEtu.mas.hunt.agents.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.hunt.behaviours.coalition_golem.PropagationBehaviour;
import eu.su.mas.dedaleEtu.mas.hunt.behaviours.coalition_golem.ReceiveEchoBehaviour;
import eu.su.mas.dedaleEtu.mas.hunt.behaviours.coalition_golem.SendOrdersBehaviour;
import jade.core.behaviours.FSMBehaviour;

import java.util.List;

public class CoalitionParticipantGolemBehaviour extends FSMBehaviour {
    private final AbstractCommunicationAgent agent;
    public CoalitionParticipantGolemBehaviour(final AbstractCommunicationAgent agent, long timeout, List<String> agentList, int depth, String leader_id){
        super(agent);
        this.agent = agent;
        jade.core.behaviours.DataStore commonStore = new jade.core.behaviours.DataStore();
        this.setDataStore(commonStore);

        PropagationBehaviour diffusion = new PropagationBehaviour(agent, depth, agentList, timeout, leader_id);
        ReceiveEchoBehaviour waitingEcho = new ReceiveEchoBehaviour(agent, timeout);
        SendEchoBehaviour sendingEcho = new SendEchoBehaviour(agent);
        WaitingOrderBehaviour waitingOrder = new WaitingOrderBehaviour(agent);
        SendOrdersBehaviour sendingOrderChildren = new SendOrdersBehaviour(agent);

        waitingOrder.setDataStore(commonStore);
        sendingOrderChildren.setDataStore(commonStore);

        registerFirstState(diffusion, "DIFFUSION");
        registerState(waitingEcho, "WAITING_ECHO");
        registerState(sendingEcho, "SENDING_ECHO");
        registerState(waitingOrder, "WAITING_ORDER");
        registerLastState(sendingOrderChildren, "SENDING_ORDER_CHILDREN");

        
        registerTransition("DIFFUSION", "WAITING_ECHO",1);
        registerTransition("DIFFUSION", "SENDING_ECHO",0);
        
        registerDefaultTransition("WAITING_ECHO", "SENDING_ECHO");

        registerDefaultTransition("SENDING_ECHO", "WAITING_ORDER");
        
        registerDefaultTransition("WAITING_ORDER", "SENDING_ORDER_CHILDREN");



    }
    
    @Override
    public int onEnd(){
        this.agent.resetCoalitionData();
        return 0;
    }
}
