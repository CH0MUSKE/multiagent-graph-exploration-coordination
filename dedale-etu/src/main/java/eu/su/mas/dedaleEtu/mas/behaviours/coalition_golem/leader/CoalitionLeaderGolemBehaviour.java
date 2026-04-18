package eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.leader;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.SendMsgBehaviour;
import jade.core.behaviours.FSMBehaviour;

import java.io.Serializable;
import java.util.List;

public class CoalitionLeaderGolemBehaviour extends FSMBehaviour {

    public CoalitionLeaderGolemBehaviour(AbstractCommunicationAgent myagent, List<String> agentList) {
        super(myagent);

        String conversation_id = this.myAgent.getLocalName() + " " + System.currentTimeMillis();
        registerFirstState(new SendMsgBehaviour((AbstractDedaleAgent)myAgent, conversation_id, myagent.getGolemPos(), "ECHO_GOLEM", agentList),"DIFFUSION");
        
        registerState(new ReceiveEchoBehaviour(myagent, conversation_id), "ATTENTE_ECHO");
        registerState(new ComputeSlotsBehaviour(myagent), "CALCUL_SLOTS");

        registerLastState(new SendMsgBehaviour(myagent, conversation_id, (Serializable) this.getDataStore().get("PLAN_GOLEM"), "GOLEM_ORDER", myagent.getChildren()), "FIN_COALITION");

        // registerTransition(Source, Destination, CodeSortie)

        registerDefaultTransition("DIFFUSION", "ATTENTE_ECHO");

        
        registerTransition("ATTENTE_ECHO", "CALCUL_SLOTS", 1);

        registerTransition("ATTENTE_ECHO", "DIFFUSION", 0);

        registerDefaultTransition("CALCUL_SLOTS", "FIN_COALITION");
    }
}
