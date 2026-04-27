    package eu.su.mas.dedaleEtu.mas.behaviours;
    
    import dataStructures.serializableGraph.SerializableSimpleGraph;
    import dataStructures.tuple.Couple;
    import eu.su.mas.dedale.env.Location;
    import eu.su.mas.dedale.env.Observation;
    import eu.su.mas.dedale.env.gs.GsLocation;
    import eu.su.mas.dedale.mas.AbstractDedaleAgent;
    import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractCommunicationAgent;
    import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent2;
    import eu.su.mas.dedaleEtu.mas.behaviours.coalition_golem.leader.CoalitionLeaderGolemBehaviour;
    import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
    import eu.su.mas.dedaleEtu.mas.knowledge.NodeInfo;
    import eu.su.mas.dedaleEtu.mas.utils.MessageFactory;
    import jade.core.behaviours.SimpleBehaviour;
    import jade.lang.acl.ACLMessage;
    import jade.lang.acl.MessageTemplate;
    import jade.lang.acl.UnreadableException;
    import org.graphstream.graph.Node;

    import java.io.Serializable;
    import java.util.*;

    public class ExploreCoopBehaviour2 extends SimpleBehaviour{
        private static final long serialVersionUID = 8567774731496787661L;
    
        private boolean finished = false;
    
        private List<String> list_agentNames;
        private MapRepresentation myMap;
        private AbstractCommunicationAgent myagent;
        private int cpt;
        private long lastCoalitionTimestamp = 0;
        private String firstPosSmell = "";
        private String oldPos = "";
        
        
        public ExploreCoopBehaviour2(final AbstractCommunicationAgent myagent, List<String> agentNames){
            super(myagent);
            this.list_agentNames = agentNames;
            this.myagent = myagent;
            this.myMap = this.myagent.getMyMap();
            this.cpt = 0;
        }

        public void action() {
            if (this.myMap == null) {
                MapRepresentation m = new MapRepresentation(this.myAgent.getLocalName());
                this.myagent.setMap(m);
                this.myMap = this.myagent.getMyMap();
            }

            Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
            if (myPosition == null) return;
            if(myPosition.getLocationId().equals(this.myagent.getOrderTarget())){
                this.myagent.stopCurrentCoalition();
                this.myagent.setStatus(AbstractCommunicationAgent.Status.EXPLORING);
            }

            // 1. Observation
            List<Couple<Location, List<Couple<Observation, String>>>> lobs = ((AbstractDedaleAgent) this.myAgent).observe();

            try { this.myAgent.doWait(1000); } catch (Exception e) { e.printStackTrace(); }

            String golemNodeFound = null;
            List<String> smellNodes = new ArrayList<>();
            List<String> nearbyAgents = new ArrayList<>();
            List<String> neighbors = new ArrayList<>();
            HashMap<String, Integer> smellValueNodes = new HashMap<>();


            // 2. Traitement des observations et mise à jour de la carte
            for (Couple<Location, List<Couple<Observation, String>>> obs : lobs) {
                Location nodeLocation = obs.getLeft();
                String nodeId = nodeLocation.getLocationId();
                List<Couple<Observation, String>> nodeObservations = obs.getRight();
                
                NodeInfo.Entity type = NodeInfo.Entity.EMPTY;
                String entityName = "";
                int smellValue = 0;

                for (Couple<Observation, String> o : nodeObservations) {
                    switch (o.getLeft()) {
                        case STENCH:
                            smellValue += 1;
                            if(!smellNodes.contains(nodeId)) smellNodes.add(nodeId);
                            break;
                        case AGENTNAME:
                            entityName = o.getRight();
                            if (entityName.toLowerCase().contains("wumpus")) {
                                type = NodeInfo.Entity.GOLEM;
                                golemNodeFound = nodeId; // On a une cible visuelle !
                            } else {
                                type = NodeInfo.Entity.AGENT;
                                if (list_agentNames.contains(entityName) && !this.myagent.hasAlreadySentMap(entityName)) {
                                    nearbyAgents.add(entityName);
                                }
                            }
                            break;
                        default:break;    
                    }
                }
                if(smellValue != 0) smellValueNodes.put(nodeId, smellValue);

                if(!nodeId.equals(myPosition.getLocationId()) && (entityName.isEmpty())){
                    neighbors.add(nodeId);
                }

                // Mise à jour de la Map
                // On récupère le statut actuel dans la map AVANT de faire quoi que ce soit
                // 1. On cherche d'abord si le noeud existe dans le graphe
                org.graphstream.graph.Node n = this.myMap.getG().getNode(nodeId);
                MapRepresentation.MapAttribute currentAttr = null;

                if (n != null) {
                    Object classObj = n.getAttribute("ui.class");
                    if (classObj != null) {
                        currentAttr = MapRepresentation.MapAttribute.valueOf(classObj.toString());
                    }
                }

                // 2. 
                MapRepresentation.MapAttribute attrToSet = null;
                if (nodeId.equals(myPosition.getLocationId())) {
                    attrToSet = (currentAttr == null || currentAttr.equals(MapRepresentation.MapAttribute.open)) ?
                            MapRepresentation.MapAttribute.closed :
                            currentAttr;
                    this.myMap.addNode(nodeId, attrToSet, NodeInfo.Entity.AGENT, this.myagent.getLocalName(), smellValue);
                } else {
                    // Si currentAttr est null, c'est que le noeud est nouveau -> open
                    // Sinon, on garde ce qu'il y avait 
                    attrToSet = (currentAttr == null) ?
                            MapRepresentation.MapAttribute.open :
                            currentAttr;
                    if(type.equals(NodeInfo.Entity.AGENT) && !attrToSet.equals(MapRepresentation.MapAttribute.blocked)) attrToSet = MapRepresentation.MapAttribute.closed;
                    if(golemNodeFound != null && this.myagent.getStatus().equals(AbstractCommunicationAgent.Status.BLOCKING)){
                        this.myMap.addNode(nodeId, attrToSet, NodeInfo.Entity.EMPTY, "", smellValue);
                        this.myMap.addEdge(myPosition.getLocationId(), nodeId);
                    }
                    else{
                        this.myMap.addNode(nodeId, attrToSet, type, entityName, smellValue);
                        this.myMap.addEdge(myPosition.getLocationId(), nodeId);
                    }

                }
            }

            // 3. Logique de décision (Transitions de Statut)
            String nextNodeId = null;
            boolean followGolem = false;
            System.out.println(this.myagent.getLocalName() + ": estimatedSmell -> " + this.myagent.getGlobalEstimatedGolemSmell());
            
            if(!smellNodes.isEmpty()){
                smellNodes = this.myMap.filterExplainedSmells(smellValueNodes, this.myagent.getGlobalEstimatedGolemSmell());
                if(this.myagent.getStatus().equals(AbstractCommunicationAgent.Status.EXPLORING)){
                    this.firstPosSmell = myPosition.getLocationId();
                    System.out.println( this.myagent.getLocalName() + ": estimated smell -> "+ this.myagent.getGlobalEstimatedGolemSmell());

                }
            }
            
            if (golemNodeFound == null) {
                // Cas 1 : On est le Leader de la coalition
                // On arrête tout car on a perdu la cible de vue
                if (this.myagent.getLeaderId().equals(this.myagent.getLocalName())) {
                    System.out.println(this.myagent.getLocalName() + " - Golem perdu, dissolution de la coalition.");
                    this.myagent.stopCurrentCoalition(); // Remet le status en EXPLORING et vide les targets
                }

                // Cas 2 : On était en train de bloquer (BLOCKING)
                // Le Golem a dû s'enfuir ou se téléporter
                // plus censé arrivé maintenant mais je laisse au cas où
                else if (this.myagent.getStatus().equals(AbstractCommunicationAgent.Status.BLOCKING)) {
                    System.out.println(this.myagent.getLocalName() + " - Le Golem a forcé le passage, reprise de l'explo.");
                    this.myagent.setStatus(AbstractCommunicationAgent.Status.EXPLORING);
                    this.myagent.setOrderTarget("");
                    Node nodeOldPos = this.myagent.getMyMap().getG().getNode(this.oldPos);
                    if(nodeOldPos != null)nodeOldPos.setAttribute("ui.class","closed");
                    this.myagent.getMyMap().getG().getNode(myPosition.getLocationId()).setAttribute("ui.class","closed");
                }
            }
            else{
                int estimatedSmell = this.myMap.getShortestPath(firstPosSmell, golemNodeFound).size() - 1;
                this.myagent.setGlobalEstimatedGolemSmell(estimatedSmell);
            }
            // PRIORITÉ 1 : On voit un Golem -> On devient LEADER

            if (golemNodeFound != null && !this.myagent.getStatus().equals(AbstractCommunicationAgent.Status.BLOCKING)) {
                System.out.println(this.myAgent.getLocalName() + " - GOLEM DÉTECTÉ !");
                // --- DEBUG ZONE ---
                org.graphstream.graph.Node gNode = this.myMap.getG().getNode(golemNodeFound);
                if (gNode != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("\n=== [DEBUG SLOTS] Agent: ").append(this.myagent.getLocalName()).append(" ===\n");
                    sb.append("Golem en: ").append(golemNodeFound).append("\n");

                    gNode.neighborNodes().forEach(neighbor -> {
                        String nId = neighbor.getId();
                        // On récupère le nom de l'entité stocké dans l'attribut du noeud
                        Object entity = neighbor.getAttribute("entityId");
                        Object type = neighbor.getAttribute("ui.class"); // Pour voir si c'est open/closed

                        sb.append(" -> Slot ").append(nId)
                                .append(" [").append(type != null ? type : "unknown").append("]")
                                .append(" : ").append((entity != null && !entity.toString().isEmpty()) ? "AGENT: " + entity : "VIDE")
                                .append("\n");
                    });
                    sb.append("=========================================");
                    System.out.println(sb.toString());
                }
                // --- END DEBUG ---
                if(this.myagent.getMyMap().isGolemSurrounded(golemNodeFound)){
                    this.myagent.getMyMap().getG().getNode(golemNodeFound).setAttribute("ui.class","blocked");
                    this.myagent.getMyMap().getG().getNode(myPosition.getLocationId()).setAttribute("ui.class","blocked");
                    this.myagent.setStatus(AbstractCommunicationAgent.Status.BLOCKING);
                }
                else{
                    long elaspedTimeSinceCoalition = System.currentTimeMillis() - this.lastCoalitionTimestamp;
                    followGolem = true;

                    if((this.myagent.getLeaderId().isBlank() || this.myagent.getLeaderId().equals(this.myagent.getAID().getLocalName())) && (elaspedTimeSinceCoalition >= 5000)){
                        // On génère un ID de conversation unique
                        String cid = this.myagent.getLocalName() + " Coalition_" + System.currentTimeMillis();
                        // On lance le comportement de Leader
                        System.out.println(this.myAgent.getLocalName() + " - Lancement coalition...");
                        this.myagent.stopCurrentCoalition();
                        this.lastCoalitionTimestamp = System.currentTimeMillis();
                        this.myagent.startCoalition(
                                new CoalitionLeaderGolemBehaviour(this.myagent, this.myagent.agentList),
                                AbstractCommunicationAgent.Status.HUNTING,
                                this.myagent.getLocalName(),
                                golemNodeFound,
                                cid,
                                ""
                        );
                    }
                    else{
                        this.myagent.setStatus(AbstractCommunicationAgent.Status.HUNTING);
                    }
                }
                this.myagent.setGolemPos(golemNodeFound);
            }
            // PRIORITÉ 2 : On sent quelque chose (et on ne chasse pas déjà)
            else if (!smellNodes.isEmpty() && this.myagent.getStatus() != AbstractCommunicationAgent.Status.HUNTING && !this.myagent.getStatus().equals(AbstractCommunicationAgent.Status.BLOCKING)) {
                System.out.println(this.myAgent.getLocalName() + " - Odeur détectée.");
                if(!this.myagent.getStatus().equals(AbstractCommunicationAgent.Status.SMELLING)){
                    this.myagent.setStatus(AbstractCommunicationAgent.Status.SMELLING);
                    System.out.println(this.myAgent.getLocalName() + " - Passage en mode SMELLING.");
                }
                String smell_conv_id = "SMELL_" + this.myagent.getLocalName() + "_" + System.currentTimeMillis();
                this.myagent.addBehaviour(new SendMsgBehaviour(myagent, smell_conv_id, (Serializable) smellNodes, "SMELL", this.myagent.agentList));
            }

            // 4. Exécution du mouvement selon le Statut
            switch (this.myagent.getStatus()) {
                case HUNTING:
                    // En mode chasse, on suit l'ordre du Leader (ou notre propre cible si on est leader)
                    
                    nextNodeId = this.myagent.getOrderTarget();
                    System.out.println("Je suis " + this.myagent.getLocalName() + " et mon ordre est: " + nextNodeId + ", qui vient de: " + this.myagent.getLeaderId());
                    if (followGolem) {
                        nextNodeId = this.myagent.getGolemPos();
                    }
                    else if(nextNodeId == null || nextNodeId.isEmpty()){
                        this.myagent.stopCurrentCoalition();
                        nextNodeId = findExploNextNode(myPosition.getLocationId(), neighbors);
                    }
                    break;

                case SMELLING:
                    // On suit une odeur au hasard parmi celles détectées
                    if (!smellNodes.isEmpty()) {
                        nextNodeId = smellNodes.get((int) (Math.random() * smellNodes.size()));
                    } else {
                        // Si plus d'odeur, on repasse en exploration
                        this.myagent.setStatus(AbstractCommunicationAgent.Status.EXPLORING);
                    }
                    break;

                case BLOCKING:  System.out.println(this.myagent.getLocalName() + " est en train de bloquer :)"); break;
                default:
                    // Logique d'exploration classique (DFS/ShortestPath)
                    nextNodeId = findExploNextNode(myPosition.getLocationId(), neighbors);
                    break;
            }

            // 5. Déplacement physique
            oldPos = myPosition.getLocationId();
            if (nextNodeId != null) {
                // Si le nœud cible n'est pas adjacent, on calcule le chemin
                List<String> path = this.myMap.getShortestPath(myPosition.getLocationId(), nextNodeId);
                if (path != null && !path.isEmpty()) {
                    String step = path.getFirst();
                    boolean hasMoved = ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(step));
                    
                    if(!hasMoved)System.out.println(this.myagent.getLocalName() + " est en mode " + this.myagent.getStatus() + "et tente d'aller en: " + nextNodeId);
                    
                }
                else{
                    // permet de reset le comportement au prochain tick
                    this.myagent.setOrderTarget(myPosition.getLocationId());
                }
            }
            
            // Gestion des pings de proximité (si agents détectés)
            if (!nearbyAgents.isEmpty()) {
                this.myagent.addBehaviour(new PingBehaviour(this.myagent, nearbyAgents));
            }
            
            if(cpt >= 3){
                this.myagent.addBehaviour(new PingBehaviour(this.myagent, list_agentNames));
                // pour éviter de dépasser maxint si je fais % 3
                cpt = 0;
            } 
            cpt++;


            // Condition de fin d'exploration
            /*if (!this.myMap.hasOpenNode() && this.myagent.getStatus() == AbstractCommunicationAgent.Status.EXPLORING) {
                finished = true;
            }*/
        }

        // Helper pour l'exploration
        private String findExploNextNode(String currentPos, List<String> neighbors) {
            String nextNodeId = null;

            // 1. PRIORITÉ : Aller vers le nœud 'open' le plus proche
            if (this.myMap.hasOpenNode()) {
                List<String> path = this.myMap.getShortestPathToClosestOpenNode(currentPos);
                if (path != null && !path.isEmpty()) {
                    // getShortestPath retire déjà la position actuelle (index 0)
                    // Donc le premier élément de la liste est le voisin direct où aller
                    return path.getFirst();
                }
            }

            // 2. SECONDAIRE : Si tout est visité, aller vers le nœud "périmé" le plus proche
            // On définit un seuil
            long threshold = 20000;
            List<String> stalePath = this.myMap.getShortestPathToClosestStaleNode(currentPos, threshold);
            if (stalePath != null && !stalePath.isEmpty()) {
                System.out.println(this.myAgent.getLocalName() + " - Mode Patrouille : vers noeud périmé.");
                return stalePath.getFirst();
            }

            // 3. DERNIER RECOURS : Un voisin au hasard
            // Pour éviter que l'agent ne reste figé si tout est exploré et frais
            if (neighbors != null && !neighbors.isEmpty()) {
                return neighbors.get((int) (Math.random() * neighbors.size()));
            }


            return null;
        }
        
        @Override
        public boolean done(){
            return finished;
        }
    }
