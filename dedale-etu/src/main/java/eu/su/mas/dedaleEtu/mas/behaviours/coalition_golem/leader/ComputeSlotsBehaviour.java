package eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.leader;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.OneShotBehaviour;
import org.graphstream.graph.Node;

import java.util.HashMap;

public class ComputeSlotsBehaviour extends OneShotBehaviour {
    AbstractCommunicationAgent myagent;
    HashMap <String,String> plan;
    public ComputeSlotsBehaviour(AbstractCommunicationAgent myagent){
        super(myagent);
        this.myagent = myagent;
    }
    
    @Override
    public void action(){
        MapRepresentation map = this.myagent.getMyMap();
        String golemPos = this.myagent.getGolemPos();
        HashMap<String,String> coalition = this.myagent.getCoalition();

        // 1. VERIFICATION CRITIQUE : Est-ce que je connais la position du Golem ?
        if (map.getG().getNode(golemPos) == null) {
            System.err.println(myagent.getLocalName() + " - Planification impossible : Golem sur un noeud inconnu (" + golemPos + ")");
            // On met un plan vide .
            this.getDataStore().put("ORDER_GOLEM", new HashMap<String, String>());
            return;
        }
        // 2. FILTRAGE : Est-ce que je connais la position de tous mes alliés ?
        // On ne garde dans le calcul que les agents dont on connaît la position sur la carte
        HashMap<String, String> validCoalition = new HashMap<>();
        for (String agentId : coalition.keySet()) {
            String pos = coalition.get(agentId);
            Node agentNode = map.getG().getNode(pos);
            if (map.getG().getNode(pos) != null && !agentNode.hasEdgeBetween(golemPos)) {
                validCoalition.put(agentId, pos);
            } else {
                System.out.println("Avertissement : Agent " + agentId + " ignore car position inconnue (" + pos + ")");
            }
        }
        

        // 3. CALCUL : On utilise uniquement la coalition "valide"
        int validSize = validCoalition.size();
        HashMap<String,String> plan = new HashMap<>();
        if (validSize > 0) {
            System.out.println(this.myagent.getLocalName() +  " -> Calcul du plan en cours... ( " + this.myagent.getCoalitionId() + " )");
            String bestChokePoint = golemPos;
            if (bestChokePoint != null) {
                this.myagent.setOrderTarget(bestChokePoint);
                System.out.println(this.myagent.getLocalName() +  " -> Début de l'allocation ( " + this.myagent.getCoalitionId() + " )");
                plan = map.chokePointAllocation(validCoalition, bestChokePoint, this.myagent.getCurrentPosition().getLocationId());
                System.out.println(this.myagent.getLocalName() +  " -> Allocation terminée ( " + this.myagent.getCoalitionId()
                        + " )");
            }
            else{
                System.out.println("Aucun plan trouvé");
            }
        }
        else{
            System.err.println("Aucun membre de la coalition n'a une position connue, on attends la prochaine coalition ");
        }
        this.getDataStore().put("ORDER_GOLEM", plan);
        if(getDataStore().get("ORDER_GOLEM") == null) System.out.println("PLAN NULL");

    }
}
