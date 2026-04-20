    package eu.su.mas.dedaleEtu.mas.behaviours;
    
    import dataStructures.serializableGraph.SerializableSimpleGraph;
    import dataStructures.tuple.Couple;
    import eu.su.mas.dedale.env.Location;
    import eu.su.mas.dedale.env.Observation;
    import eu.su.mas.dedale.env.gs.GsLocation;
    import eu.su.mas.dedale.mas.AbstractDedaleAgent;
    import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractCommunicationAgent;
    import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent2;
    import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
    import eu.su.mas.dedaleEtu.mas.knowledge.NodeInfo;
    import jade.core.behaviours.SimpleBehaviour;
    import jade.lang.acl.ACLMessage;
    import jade.lang.acl.MessageTemplate;
    import jade.lang.acl.UnreadableException;

    import java.util.ArrayList;
    import java.util.Iterator;
    import java.util.List;
    
    public class ExploreCoopBehaviour2 extends SimpleBehaviour{
        private static final long serialVersionUID = 8567774731496787661L;
    
        private boolean finished = false;
    
        private List<String> list_agentNames;
        private MapRepresentation myMap;
        private AbstractCommunicationAgent myagent;
        private int cpt;
        private Location prevLocation;
        
        public ExploreCoopBehaviour2(final AbstractCommunicationAgent myagent, List<String> agentNames){
            super(myagent);
            this.list_agentNames = agentNames;
            this.myagent = myagent;
            this.myMap = this.myagent.getMyMap();
            this.cpt = 0;
        }
        
        @Override 
        public void action(){
            if(this.myMap==null) {
                MapRepresentation m = new MapRepresentation(this.myAgent.getLocalName());
                this.myagent.setMap(m);
                this.myMap= this.myagent.getMyMap();
                
            }
            Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
            if (myPosition==null){
                return;
            }
            List<Couple<Location,List<Couple<Observation,String>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
            //Charlie -- list of observables: [<98, [<Stench, >]>, <97, []>, <99, [<Stench, >, <AgentName, Wumpus>]>]
            try {
                this.myAgent.doWait(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.myMap.addNode(myPosition.getLocationId(), MapRepresentation.MapAttribute.closed, NodeInfo.Entity.AGENT, this.myagent.getAID().getLocalName(), 0);
            
            String nextNodeId=null;
            List<String> nearbyAgents = new ArrayList<>();
            for (Couple<Location, List<Couple<Observation, String>>> obs : lobs) {
                Location nodeLocation = obs.getLeft();
                String nodeId = nodeLocation.getLocationId();
                List<Couple<Observation, String>> nodeObservations = obs.getRight();

                NodeInfo.Entity type = NodeInfo.Entity.EMPTY;
                String entityName = null;
                int smellValue = 0;

                // A. Extraction de l'odeur et des Entités
                for (Couple<Observation, String> o : nodeObservations) {
                    switch (o.getLeft()) {
                        case STENCH:
                            String val = o.getRight();
                            smellValue = (val == null || val.isEmpty()) ? 1 : Integer.parseInt(val);
                            break;
                        case AGENTNAME:
                            entityName = o.getRight();
                            // On identifie si c'est le Golem ou un Agent
                            if (entityName.toLowerCase().contains("golem")) {
                                type = NodeInfo.Entity.GOLEM;
                            } else {
                                type = NodeInfo.Entity.AGENT;
                                // Si c'est un allié, on l'ajoute pour le Ping
                                if (list_agentNames.contains(entityName) && !this.myagent.hasAlreadySentMap(entityName)) {
                                    nearbyAgents.add(entityName);
                                }
                            }
                            break;
                    }
                }

                // B. Cas particulier : Ma propre case
                if (nodeId.equals(myPosition.getLocationId())) {
                    type = NodeInfo.Entity.AGENT;
                    entityName = this.myagent.getLocalName();
                    this.myMap.addNode(nodeId, MapRepresentation.MapAttribute.closed, type, entityName, smellValue);
                } else {
                    // C. Topologie pour les voisins
                    boolean isNew = this.myMap.addNewNode(nodeId);
                    this.myMap.addEdge(myPosition.getLocationId(), nodeId);

                    // On met à jour les infos du voisin (Golem ou Vide)
                    this.myMap.addNode(nodeId, MapRepresentation.MapAttribute.open, type, entityName, smellValue);

                    if (nextNodeId == null && isNew) nextNodeId = nodeId;
                }
            }
            if (!this.myMap.hasOpenNode()){
                //Explo finished
                finished=true;
                System.out.println(this.myAgent.getLocalName()+" - Exploration successufully done, behaviour removed.");
            }else{
                //4) select next move.
                //4.1 If there exist one open node directly reachable, go for it,
                //	 otherwise choose one from the openNode list, compute the shortestPath and go for it
                if (nextNodeId==null){
                    //no directly accessible openNode
                    //chose one, compute the path and take the first step.
                    nextNodeId=this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId()).get(0);//getShortestPath(myPosition,this.openNodes.get(0)).get(0);
                    //System.out.println(this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"| nextNode: "+nextNodeId);
                }else {
                    //System.out.println("nextNode notNUll - "+this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"\n -- nextNode: "+nextNodeId);
                }
    
               boolean hasMoved = ((AbstractDedaleAgent)this.myAgent).moveTo(new GsLocation(nextNodeId));
                System.out.println(nextNodeId);

                if(hasMoved && cpt % 3 == 0){
                    this.myagent.addBehaviour(new PingBehaviour(this.myagent,list_agentNames));

                }
                if(!nearbyAgents.isEmpty()){
                    this.myagent.addBehaviour(new PingBehaviour(this.myagent,nearbyAgents));
                }
                cpt++;
            }
        }
        
        @Override
        public boolean done(){
            return finished;
        }
    }
