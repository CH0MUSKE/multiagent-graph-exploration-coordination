package eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.leader;

import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.SendMsgBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.PropagationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.ReceiveEchoBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.SendOrdersBehaviour;
import jade.core.behaviours.FSMBehaviour;

import java.io.Serializable;
import java.util.List;

public class CoalitionLeaderGolemBehaviour extends FSMBehaviour {
    final long timeout = 5000;
    final int depth = 3;
    private final AbstractCommunicationAgent agent;

    public CoalitionLeaderGolemBehaviour(final AbstractCommunicationAgent myagent, List<String> agentList) {
        super(myagent);
        this.agent = myagent;
        jade.core.behaviours.DataStore commonStore = new jade.core.behaviours.DataStore();
        this.setDataStore(commonStore);

        PropagationBehaviour prop = new PropagationBehaviour(myagent, depth+1, agentList, timeout, myagent.getAID().getLocalName());
        ReceiveEchoBehaviour echo = new ReceiveEchoBehaviour(myagent, timeout);
        ComputeSlotsBehaviour compute = new ComputeSlotsBehaviour(myagent);
        SendOrdersBehaviour send = new SendOrdersBehaviour(myagent);


        compute.setDataStore(commonStore);
        send.setDataStore(commonStore);

        registerFirstState(prop, "DIFFUSION");
        registerState(echo, "WAITING_ECHO");
        registerState(compute, "COMPUTING_SLOTS");
        registerLastState(send, "COALITION_END");
        

        // registerTransition(Source, Destination, CodeSortie)

        registerDefaultTransition("DIFFUSION", "WAITING_ECHO");
        
        registerTransition("WAITING_ECHO", "COMPUTING_SLOTS", 1);

        registerTransition("WAITING_ECHO", "COALITION_END", 0);

        registerDefaultTransition("COMPUTING_SLOTS", "COALITION_END");
    }
    
    @Override
    public int onEnd(){
        this.agent.resetCoalitionData();
        return 0;
    }
}
