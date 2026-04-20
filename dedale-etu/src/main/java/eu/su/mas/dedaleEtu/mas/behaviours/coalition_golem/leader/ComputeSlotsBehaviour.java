package eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.leader;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.OneShotBehaviour;

import java.util.HashMap;

public class ComputeSlotsBehaviour extends OneShotBehaviour {
    AbstractCommunicationAgent myagent;
    public ComputeSlotsBehaviour(AbstractCommunicationAgent myagent){
        super(myagent);
        this.myagent = myagent;
    }
    
    @Override
    public void action(){
        MapRepresentation map = this.myagent.getMyMap();
        String golemPos = this.myagent.getGolemPos();
        int coalitionSize = this.myagent.getCoalitionSize();
        HashMap<String,String> coalition = this.myagent.getCoalition();
        String bestChokePoint = map.selectBestChokePoint(golemPos, coalitionSize);
        HashMap<String,String> plan = map.chokePointAllocation(coalition,bestChokePoint);
        this.getDataStore().put("ORDER_GOLEM", plan);
    }
}
