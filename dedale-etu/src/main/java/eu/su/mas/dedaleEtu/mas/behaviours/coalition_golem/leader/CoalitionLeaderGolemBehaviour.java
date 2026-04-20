package eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.leader;

import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.SendMsgBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.PropagationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.ReceiveEchoBehaviour;
import jade.core.behaviours.FSMBehaviour;

import java.io.Serializable;
import java.util.List;

public class CoalitionLeaderGolemBehaviour extends FSMBehaviour {
    final long timeout = 2000;
    final int depth = 3;

    public CoalitionLeaderGolemBehaviour(AbstractCommunicationAgent myagent, List<String> agentList) {
        super(myagent);

        String conversation_id = this.myAgent.getLocalName() + " " + System.currentTimeMillis();
        registerFirstState(new PropagationBehaviour(myagent, depth+1, agentList, conversation_id, timeout, myagent.getAID().getLocalName()),"DIFFUSION");
        
        registerState(new ReceiveEchoBehaviour(myagent, conversation_id, timeout), "WAITING_ECHO");
        registerState(new ComputeSlotsBehaviour(myagent), "COMPUTING_SLOTS");

        registerLastState(new SendMsgBehaviour(myagent, conversation_id, (Serializable) this.getDataStore().get("GOLEM_PLAN"), "GOLEM_ORDER", myagent.getChildren()), "COALITION_END");

        // registerTransition(Source, Destination, CodeSortie)

        registerDefaultTransition("DIFFUSION", "WAITING_ECHO");
        
        registerTransition("WAITING_ECHO", "COMPUTING_SLOTS", 1);

        registerTransition("WAITING_ECHO", "DIFFUSION", 0);

        registerDefaultTransition("COMPUTING_SLOTS", "COALITION_END");
    }
}
