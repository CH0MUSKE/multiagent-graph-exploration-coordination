package eu.su.mas.dedaleEtu.mas.hunt.knowledge;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.javafx.FxGraphRenderer;
import org.graphstream.ui.view.Viewer;

import dataStructures.serializableGraph.*;
import dataStructures.tuple.Couple;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * This simple topology representation only deals with the graph, not its content.</br>
 * The knowledge representation is not well written (at all), it is just given as a minimal example.</br>
 * The viewer methods are not independent of the data structure, and the dijkstra is recomputed every-time.
 * 
 * @author hc
 */
public class MapRepresentation implements Serializable {

	/**
	 * A node is open, closed, or agent
	 * @author hc
	 *
	 */

	public enum MapAttribute {	
		agent,open,closed
	}

	private static final long serialVersionUID = -1333959882640838272L;

	/*********************************
	 * Parameters for graph rendering
	 ********************************/

	private String defaultNodeStyle= "node {"+"fill-color: black;"+" size-mode:fit;text-alignment:under; text-size:14;text-color:white;text-background-mode:rounded-box;text-background-color:black;}";
	private String nodeStyle_open = "node.open {"+"fill-color: forestgreen;"+"}";
	private String nodeStyle_agent = "node.agent {"+"fill-color: blue;"+"}";
	private String nodeStyle=defaultNodeStyle+nodeStyle_agent+nodeStyle_open;

	private Graph g; //data structure non serializable
	private Viewer viewer; //ref to the display,  non-serializable
	private Integer nbEdges;//used to generate the edges ids
	private String agentName;//name of the agent the map belongs to
	public HashMap<String,String> entityPositions = new HashMap<>();

	private SerializableSimpleGraph<String, NodeInfo> sg;//used as a temporary dataStructure during migration
	
	/**
	 * @deprecated Prefer the use of MapRepresentation(String agentName)
	 */
	@Deprecated 
	public MapRepresentation() {
		

		//System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "javafx");
		this.g= new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet",nodeStyle);

		Platform.runLater(() -> {
			//openGui();
			openGui4();
		});
		//this.viewer = this.g.display();

		this.nbEdges=0;
	}


	/**
	 * @param agentName Name of the agent this representation belongs too
	 */
	public MapRepresentation(String agentName) {
		//System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "javafx");
		this.g= new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet",nodeStyle);
		this.agentName=agentName;

		Platform.runLater(() -> {
			//openGui();
			openGui4();
			
		});
		this.nbEdges=0;
	}
	
	/**
	 * Add or replace a node and its attribute 
	 * @param id unique identifier of the node
	 * @param mapAttribute attribute to process
	 */
	public synchronized void addNode(String id, MapAttribute mapAttribute, NodeInfo.Entity entityType, String entityId, int smell, long blockedLastUpdate, boolean blocked ){
		Node n;
		if (this.g.getNode(id)==null){
			n=this.g.addNode(id);
		}else{
			n=this.g.getNode(id);
		}

		long now = System.currentTimeMillis();
		boolean changeEntityLocation = false;
		if (entityId != null) {
			changeEntityLocation = updateEntityLocation(entityId, id, now);
		}
		if(changeEntityLocation){
			n.setAttribute("entityType", entityType.toString());
			n.setAttribute("entityId", entityId);
		}
		else{
			n.setAttribute("entityType", NodeInfo.Entity.EMPTY.toString());
			n.setAttribute("entityId", "");
		}
		n.setAttribute("ui.class", mapAttribute.toString());
		n.setAttribute("ui.label",id);
		n.setAttribute("lastUpdate", now);

		n.setAttribute("smell",smell);
		if(blockedLastUpdate < 0){
			Object oldTS = n.getAttribute("blockedLastUpdate");
			Object oldB = n.getAttribute("blocked");
			n.setAttribute("blockedLastUpdate", (oldTS != null) ? (long)oldTS : 0L);
			n.setAttribute("blocked", (oldB != null) && (boolean)oldB);
		}
		else{
			n.setAttribute("blockedLastUpdate", blockedLastUpdate);
			n.setAttribute("blocked", blocked);
		}
	}

	public synchronized boolean updateEntityLocation(String entityName, String newNodeId, long lastUpdate) {
		if (!entityName.isEmpty()){
			// 1. On cherche où était cette entité avant
			String oldNodeId = entityPositions.get(entityName);

			// 2. Si elle a bougé (old != new)
			if (oldNodeId != null && !oldNodeId.equals(newNodeId)) {
				// On récupère l'ancien nœud pour le vider
				org.graphstream.graph.Node oldNode = g.getNode(oldNodeId);
				if (oldNode != null) {
					if((long)oldNode.getAttribute("lastUpdate") <= lastUpdate){
						oldNode.setAttribute("entityType", NodeInfo.Entity.EMPTY.toString());
						oldNode.setAttribute("entityId", "");
					}
					else{
						return false;
					}
				}
			}
			// 3. On met à jour l'index avec la nouvelle position
			entityPositions.put(entityName, newNodeId);
			return true;
		}
		else {
			org.graphstream.graph.Node n = g.getNode(newNodeId);
			if (n != null) {
				// On regarde qui était enregistré sur ce nœud AVANT de le modifier
				String agentWhoWasHere = (String) n.getAttribute("entityId");
				if (agentWhoWasHere != null && !agentWhoWasHere.isEmpty()) {
					entityPositions.remove(agentWhoWasHere);
					//System.out.println("Nettoyage : " + agentWhoWasHere + " n'est plus sur " + newNodeId);
				}
			}
			return true;
		}
	}


	public synchronized List<String> getStaleNodes(long thresholdMs) {
		long now = System.currentTimeMillis();
		return g.nodes()
				.filter(n -> {
					// On ne s'intéresse qu'aux nœuds déjà visités (closed)
					String status = (String) n.getAttribute("ui.class");
					boolean blocked = (boolean)n.getAttribute("blocked"); 
					if (!MapAttribute.closed.toString().equals(status) || blocked) return false;

					// On vérifie l'âge de la dernière mise à jour
					Object lastUpdateObj = n.getAttribute("lastUpdate");
					long lastUpdate = (lastUpdateObj instanceof Long) ? (long) lastUpdateObj : 0L;

					return (now - lastUpdate) > thresholdMs;
				})
				.map(Node::getId)
				.collect(Collectors.toList());
	}

	public List<String> getShortestPathToClosestStaleNode(String myPosition, long threshold) {
		// 1. On récupère tous les nœuds qui n'ont pas été mis à jour depuis 'threshold'
		List<String> targets = getStaleNodes(threshold);

		if (targets.isEmpty()) return null;

		// 2. On cherche le plus proche en utilisant Dijkstra (celui qui a le path le plus court)
		Optional<Couple<String, Integer>> closest = targets.stream()
				.map(id -> {
					List<String> path = getShortestPath(myPosition, id);
					// Si le chemin est null (nœud inaccessible), on met une distance infinie
					int dist = (path != null) ? path.size() : Integer.MAX_VALUE;
					return new Couple<>(id, dist);
				})
				// On filtre les nœuds inaccessibles
				.filter(c -> c.getRight() < Integer.MAX_VALUE)
				.min(Comparator.comparing(Couple::getRight));

		// 3. On renvoie le chemin vers ce nœud
		if (closest.isPresent()) {
			return getShortestPath(myPosition, closest.get().getLeft());
		}

		return null;
	}

	/**
	 * Add a node to the graph. Do nothing if the node already exists.
	 * If new, it is labeled as open (non-visited), entity = EMPTY, id = "" and smell = 0
	 * @param id id of the node
	 * @return true if added
	 */
	public synchronized boolean addNewNode(String id) {
		if (this.g.getNode(id)==null){
			addNode(id,MapAttribute.open, NodeInfo.Entity.EMPTY, "",0,0,false);
			return true;
		}
		return false;
	}
	
	/**
	 * Add an undirect edge if not already existing.
	 * @param idNode1 unique identifier of node1
	 * @param idNode2 unique identifier of node2
	 */
	public synchronized void addEdge(String idNode1,String idNode2){
		this.nbEdges++;
		try {
			this.g.addEdge(this.nbEdges.toString(), idNode1, idNode2);
		}catch (IdAlreadyInUseException e1) {
			System.err.println("ID existing");
			System.exit(1);
		}catch (EdgeRejectedException e2) {
			this.nbEdges--;
		} catch(ElementNotFoundException e3){

		}
	}

	private double getNodeWeight(Node n, String myPositionId) {
		if (n.getId().equals(myPositionId)) {
			return 1.0;
		}
		long now = System.currentTimeMillis();
		long C = 5000; 

		// 1. Récupération des attributs de base
		boolean blocked = (boolean) n.getAttribute("blocked");
		Object entityTypeObj = n.getAttribute("entityType");
		String entityTypeStr = (entityTypeObj != null) ? entityTypeObj.toString() : "";
		Object lastUpdateObj = n.getAttribute("lastUpdate");
		long lastUpdate = (lastUpdateObj instanceof Long) ? (long) lastUpdateObj : 0L;

		// 2. CAS CRITIQUE : Mur de capture (Blocked)
		// Même si c'est vieux, on reste prudent, mais on pourrait aussi y mettre un timeout (?)
		if (blocked) {
			return 1000.0;
		}

		// 3. CAS SOCIAL : Occupation par un agent
		if (NodeInfo.Entity.AGENT.toString().equals(entityTypeStr)) {
			// Est-ce que l'info est "Fraîche" ?
			if ((now - lastUpdate) < C) {
				// L'info a moins de C : l'agent est probablement encore là
				return 10.0; // Coût élevé pour encourager le contournement
			} else {
				// L'info est vieille ( > C ) : on considère que l'agent a bougé
				// On traite la case comme une case vide classique
				return 1.0;
			}
		}

		// 4. Par défaut : case libre
		return 1.0;
	}

	/**
	 * Compute the shortest Path from idFrom to IdTo. The computation is currently not very efficient
	 * 
	 * 
	 * @param idFrom id of the origin node
	 * @param idTo id of the destination node
	 * @return the list of nodes to follow, null if the targeted node is not currently reachable
	 */


	public synchronized List<String> getShortestPath(String idFrom, String idTo) {
		List<String> shortestPath = new ArrayList<>();

		// 1. Calcul dynamique des poids avant chaque recherche
		g.edges().forEach(edge -> {
			Node n1 = edge.getNode0();
			Node n2 = edge.getNode1();

			// On récupère le coût du nœud de destination (le plus cher des deux)
			double weight = Math.max(getNodeWeight(n1, idFrom), getNodeWeight(n2, idFrom));
			edge.setAttribute("weight", weight);
		});

		Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "weight");
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();

		Node target = g.getNode(idTo);
		if (target != null && dijkstra.getPath(target) != null) {
			double totalWeight = dijkstra.getPathLength(target);

			// Si le poids est >= 1000, cela signifie qu'au moins une arête 
			// du chemin mène à un nœud "blocked"
			if (totalWeight >= 1000.0) {
				dijkstra.clear();
				return null; // On refuse le chemin car il est bloqué stratégiquement
			}
			dijkstra.getPath(target).nodes().forEach(n -> shortestPath.add(n.getId()));
		}
		dijkstra.clear();

		if (shortestPath.size() > 0) shortestPath.remove(0); // Enlever ma position
		return shortestPath.isEmpty() ? null : shortestPath;
	}

	public List<String> getShortestPathToClosestOpenNode(String myPosition) {
		//1) Get all openNodes
		List<String> opennodes=getOpenNodes();

		//2) select the closest one
		List<Couple<String,Integer>> lc=
				opennodes.stream()
				.map(on -> (getShortestPath(myPosition,on)!=null)? new Couple<String, Integer>(on,getShortestPath(myPosition,on).size()): new Couple<String, Integer>(on,Integer.MAX_VALUE))//some nodes my be unreachable if the agents do not share at least one common node.
				.collect(Collectors.toList());

		Optional<Couple<String,Integer>> closest=lc.stream().min(Comparator.comparing(Couple::getRight));
		//3) Compute shorterPath

		return getShortestPath(myPosition,closest.get().getLeft());
	}



	public synchronized List<String> getOpenNodes(){
		return this.g.nodes()
				.filter(x -> MapAttribute.open.toString().equals(x.getAttribute("ui.class"))
						&& !Boolean.TRUE.equals(x.getAttribute("blocked")))
				.map(Node::getId)
				.collect(Collectors.toList());
	}


	/**
	 * Before the migration we kill all non serializable components and store their data in a serializable form
	 */
	public void prepareMigration(){
		serializeGraphTopology();

		closeGui();

		this.g=null;
	}

	/**
	 * Before sending the agent knowledge of the map it should be serialized.
	 */
	private void serializeGraphTopology() {
		this.sg= new SerializableSimpleGraph<String,NodeInfo>();
		Iterator<Node> iter=this.g.iterator();
		while(iter.hasNext()){
			Node n=iter.next();
			MapAttribute attr = MapAttribute.valueOf((String) n.getAttribute("ui.class"));
			long ts = (long) n.getAttribute("lastUpdate");
			NodeInfo.Entity entity = NodeInfo.Entity.valueOf((String)n.getAttribute("entityType"));
			String entityId = (String)n.getAttribute("entityId");
			int smell = (int)n.getAttribute("smell");
			long blockedLastUpdate = (long)n.getAttribute("blockedLastUpdate");
			boolean blocked = (boolean)n.getAttribute("blocked");
			sg.addNode(n.getId(), new NodeInfo(attr, ts, entity, entityId, smell, blockedLastUpdate, blocked));
		}
		Iterator<Edge> iterE=this.g.edges().iterator();
		while (iterE.hasNext()){
			Edge e=iterE.next();
			Node sn=e.getSourceNode();
			Node tn=e.getTargetNode();
			sg.addEdge(e.getId(), sn.getId(), tn.getId());
		}	
	}


	public synchronized SerializableSimpleGraph<String,NodeInfo> getSerializableGraph(){
		serializeGraphTopology();
		return this.sg;
	}

	/**
	 * After migration we load the serialized data and recreate the non serializable components (Gui,..)
	 */
	public synchronized void loadSavedData(){

		this.g= new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet",nodeStyle);

		openGui();

		Integer nbEd=0;
		for (SerializableNode<String, NodeInfo> n: this.sg.getAllNodes()){
			this.g.addNode(n.getNodeId()).setAttribute("ui.class", n.getNodeContent().statut.toString());
			this.g.addNode(n.getNodeId()).setAttribute("lastUpdate", n.getNodeContent().lastUpdate);
			this.g.addNode(n.getNodeId()).setAttribute("entityType", n.getNodeContent().entityType.toString());
			this.g.addNode(n.getNodeId()).setAttribute("entityId", n.getNodeContent().entityId);
			this.g.addNode(n.getNodeId()).setAttribute("smell", n.getNodeContent().smell);
			this.g.addNode(n.getNodeId()).setAttribute("blockedLastUpdate", n.getNodeContent().blockedLastUpdate);
			this.g.addNode(n.getNodeId()).setAttribute("blocked", n.getNodeContent().blocked);
			


			for(String s:this.sg.getEdges(n.getNodeId())){
				this.g.addEdge(nbEd.toString(),n.getNodeId(),s);
				nbEd++;
			}
		}
		System.out.println("Loading done");
	}

	/**
	 * Method called before migration to kill all non serializable graphStream components
	 */
	private synchronized void closeGui() {
		//once the graph is saved, clear non serializable components
		if (this.viewer!=null){
			//Platform.runLater(() -> {
			try{
				this.viewer.close();
			}catch(NullPointerException e){
				System.err.println("Bug graphstream viewer.close() work-around - https://github.com/graphstream/gs-core/issues/150");
			}
			//});
			this.viewer=null;
		}
	}

	/**
	 * Method called after a migration to reopen GUI components
	 */
	private synchronized void openGui() {
		this.viewer =new FxViewer(this.g, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);//GRAPH_IN_GUI_THREAD)
		viewer.enableAutoLayout();
		viewer.setCloseFramePolicy(FxViewer.CloseFramePolicy.CLOSE_VIEWER);
		viewer.addDefaultView(true);

		g.display();
	}

	/**
	 * Method called after a migration to reopen default GUI component
	 */
	private synchronized void openGui4() {

		Stage primaryStage = new Stage();
		StackPane newRoot = new StackPane();

		AnchorPane ap= new AnchorPane();

		FxViewer viewer = new FxViewer(g, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);

		g.setAttribute("ui.antialias");
		g.setAttribute("ui.quality");
		viewer.enableAutoLayout();
		viewer.setCloseFramePolicy(FxViewer.CloseFramePolicy.CLOSE_VIEWER);
		//viewer.addDefaultView(true);

		//g.display();

		FxViewPanel panel = (FxViewPanel)viewer.addDefaultView(false, new FxGraphRenderer());
		ap.getChildren().add(panel);
		newRoot.getChildren().add(ap);
		primaryStage.setTitle(this.agentName);

		Scene scene = new Scene(newRoot, 800, 800);
		primaryStage.setScene(scene);
		primaryStage.show();

	}
	
	public synchronized void mergeMap(String agent,SerializableSimpleGraph<String, NodeInfo> sgreceived) {
		//System.out.println("You should decide what you want to save and how");
		//System.out.println("We currently blindy add the topology");

		for (SerializableNode<String, NodeInfo> n: sgreceived.getAllNodes()){
			//System.out.println(n);
			boolean alreadyIn =false;
			//1 Add the node
			Node newnode=null;
			try {
				newnode=this.g.addNode(n.getNodeId());
			}	catch(IdAlreadyInUseException e) {
				alreadyIn=true;
				//System.out.println("Already in"+n.getNodeId());
			}
			if (!alreadyIn) {
				newnode.setAttribute("ui.label", newnode.getId());
				newnode.setAttribute("ui.class", n.getNodeContent().statut.toString());
				newnode.setAttribute("lastUpdate", n.getNodeContent().lastUpdate);
				newnode.setAttribute("entityType", n.getNodeContent().entityType.toString());
				newnode.setAttribute("entityId", n.getNodeContent().entityId);
				newnode.setAttribute("smell", n.getNodeContent().smell);
				newnode.setAttribute("blockedLastUpdate", n.getNodeContent().blockedLastUpdate);
				newnode.setAttribute("blocked", n.getNodeContent().blocked);

			}else{
				newnode=this.g.getNode(n.getNodeId());
				//3 check its attribute. If it is below the one received, update it.
				long curr_time = (long)newnode.getAttribute("lastUpdate");
				long curr_blockedTime = (long)newnode.getAttribute("blockedLastUpdate");
				NodeInfo infoRecue = n.getNodeContent();
				
				long new_time = infoRecue.lastUpdate;
				long new_blockedTime = infoRecue.blockedLastUpdate;
				String currentStatus = (String) newnode.getAttribute("ui.class");
				boolean tryingToReopen = (currentStatus.equals(MapAttribute.closed.toString())
						&& infoRecue.statut.equals(MapAttribute.open));
				
				//System.out.println("merge de " + agent + " en cours -> new_time = " + new_time + " | curr_time = " + curr_time + " | node concerné: " + n.getNodeId());
				if(entityPositions.get(infoRecue.entityId) != null && !entityPositions.get(infoRecue.entityId).isEmpty() ){
					//System.out.println(agent  + " old position of: " + infoRecue.entityId + " -> " + "( " + this.entityPositions.get(infoRecue.entityId) + ", " + this.g.getNode(entityPositions.get(infoRecue.entityId)).getAttribute("lastUpdate")+ " )");
				}
				

				if (new_time >= curr_time){
					boolean changeEntityLocation = false;
					if (infoRecue.entityId != null) {
						changeEntityLocation = updateEntityLocation(infoRecue.entityId, newnode.getId(), new_time);
					}
					newnode.setAttribute("lastUpdate", new_time);
					if(changeEntityLocation){
						newnode.setAttribute("entityType", infoRecue.entityType.toString());
						newnode.setAttribute("entityId", infoRecue.entityId);
					}
					else{
						newnode.setAttribute("entityType", NodeInfo.Entity.EMPTY.toString());
						newnode.setAttribute("entityId", "");
					}
					newnode.setAttribute("smell", infoRecue.smell);
				}
				//System.out.println(agent  + " new position of: " + infoRecue.entityId + " -> " + this.entityPositions.get(infoRecue.entityId));
				

				if(!tryingToReopen)	newnode.setAttribute("ui.class", infoRecue.statut.toString());
				if(new_blockedTime > curr_blockedTime){
					newnode.setAttribute("blocked", infoRecue.blocked);
					newnode.setAttribute("blockedLastUpdate", infoRecue.blockedLastUpdate);
				}
				
			}
		}

		//4 now that all nodes are added, we can add edges
		for (SerializableNode<String, NodeInfo> n: sgreceived.getAllNodes()){
			for(String s:sgreceived.getEdges(n.getNodeId())){
				addEdge(n.getNodeId(),s);
			}
		}
		//System.out.println("Merge done");
	}

	/**
	 * 
	 * @return true if there exist at least one openNode on the graph 
	 */
	public boolean hasOpenNode() {
		return (this.g.nodes()
				.filter(n -> n.getAttribute("ui.class").equals(MapAttribute.open.toString())
				&& !Boolean.TRUE.equals(n.getAttribute("blocked")))
				.findAny()).isPresent();
	}

	public synchronized SerializableSimpleGraph<String, NodeInfo> createSubGraph(Long lastConv) {
		// 1. Initialisation du graphe sérialisable vide
		SerializableSimpleGraph<String, NodeInfo> subGraph = new SerializableSimpleGraph<String, NodeInfo>();

		// Si lastConv est null (premier échange), on prend 0 pour tout envoyer
		long threshold = (lastConv == null) ? 0L : lastConv;

		// Liste temporaire pour savoir quels nœuds on a gardés (utile pour les arêtes)
		List<String> addedNodes = new ArrayList<>();

		// 2. Premier passage : On ajoute tous les nœuds mis à jour
		this.g.nodes().forEach(n -> {
			long nodeTs = (long) n.getAttribute("lastUpdate");
			NodeInfo.Entity entityType = NodeInfo.Entity.valueOf((String)n.getAttribute("entityType"));
			String entityId = (String)n.getAttribute("entityId");
			int smell = (int)n.getAttribute("smell");
			long blockedLastUpdate = (long)n.getAttribute("blockedLastUpdate");
			boolean blocked = (boolean)n.getAttribute("blocked");

			if (nodeTs >= threshold) {
				MapAttribute attr = MapAttribute.valueOf((String) n.getAttribute("ui.class"));
				subGraph.addNode(n.getId(), new NodeInfo(attr, nodeTs, entityType, entityId, smell, blockedLastUpdate, blocked));
				addedNodes.add(n.getId());
			}
		});

		// 3. Deuxième passage : On ajoute les arêtes
		// Stratégie : On ajoute une arête si AU MOINS UN des deux nœuds est nouveau
		// Cela permet de propager les nouvelles connexions vers d'anciens nœuds
		this.g.edges().forEach(e -> {
			String idSrc = e.getSourceNode().getId();
			String idDst = e.getTargetNode().getId();

			if (addedNodes.contains(idSrc) || addedNodes.contains(idDst)) {
				// On s'assure que les deux nœuds existent dans le sous-graphe 
				// (sinon le SerializableSimpleGraph risque de râler)
				if (subGraph.getNode(idSrc) == null) {
					Node n = e.getSourceNode();
					subGraph.addNode(idSrc, new NodeInfo(MapAttribute.valueOf((String)n.getAttribute("ui.class")), (long)n.getAttribute("lastUpdate"),NodeInfo.Entity.valueOf((String)n.getAttribute("entityType")),(String)n.getAttribute("entityId"), (int)n.getAttribute("smell"), (long)n.getAttribute("blockedLastUpdate"), (boolean)n.getAttribute("blocked") ));
				}
				if (subGraph.getNode(idDst) == null) {
					Node n = e.getTargetNode();
					subGraph.addNode(idDst, new NodeInfo(MapAttribute.valueOf((String)n.getAttribute("ui.class")), (long)n.getAttribute("lastUpdate"),NodeInfo.Entity.valueOf((String)n.getAttribute("entityType")), (String)n.getAttribute("entityId"), (int)n.getAttribute("smell"), (long)n.getAttribute("blockedLastUpdate"), (boolean)n.getAttribute("blocked") ));
				}

				subGraph.addEdge(e.getId(), idSrc, idDst);
			}
		});

		return subGraph;
	}

	public synchronized boolean isGolemSurrounded(String golemPos) {
		org.graphstream.graph.Node node = this.g.getNode(golemPos);
		if (node == null) return false;

		List<org.graphstream.graph.Node> neighbors = node.neighborNodes().collect(Collectors.toList());

		if (neighbors.isEmpty()) return false;

		for (org.graphstream.graph.Node neighbor : neighbors) {
			String entityType = (String) neighbor.getAttribute("entityType");
			boolean isBlocked = (boolean) neighbor.getAttribute("blocked");
			if (!(!NodeInfo.Entity.EMPTY.toString().equals(entityType) || isBlocked)) {
				return false;
			}
		}
		return true;
	}

	public synchronized List<String> getShortestPathIgnoringEntities(String idFrom,String idTo){
		List<String> shortestPath=new ArrayList<String>();

		Dijkstra dijkstra = new Dijkstra();//number of edge
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();//compute the distance to all nodes from idFrom
		List<Node> path=dijkstra.getPath(g.getNode(idTo)).getNodePath(); //the shortest path from idFrom to idTo
		Iterator<Node> iter=path.iterator();
		while (iter.hasNext()){
			shortestPath.add(iter.next().getId());
		}
		dijkstra.clear();
		if (shortestPath.isEmpty()) {//The openNode is not currently reachable
			return null;
		}else {
			shortestPath.remove(0);//remove the current position
		}
		return shortestPath;
	}



	public synchronized List<String> filterExplainedSmells(Map<String, Integer> observedStenches, int radius) {
		List<String> suspiciousSmells = new ArrayList<>();

		// 1. On récupère TOUS les Golems bloqués sur la carte une seule fois
		List<String> blockedGolems = this.g.nodes()
				.filter(n ->(boolean) n.getAttribute("blocked") && n.getAttribute("entityType").equals(NodeInfo.Entity.GOLEM.toString()))
				.map(Node::getId)
				.collect(Collectors.toList());

		// 2. Pour chaque nœud où on a détecté une odeur
		for (Map.Entry<String, Integer> entry : observedStenches.entrySet()) {
			String smellNodeId = entry.getKey();
			int nbStenchesObserved = entry.getValue();
			int explainedByBlocked = 0;

			// 3. On regarde combien de Golems bloqués sont dans le rayon de CE nœud précis
			for (String blockedPos : blockedGolems) {
				List<String> path = getShortestPathIgnoringEntities(smellNodeId, blockedPos);
				if (path != null && path.size() <= radius) {
					explainedByBlocked++;
				}
				//System.out.println("blocked golem pos: " + blockedPos);
			}

			// 4. Si le nombre d'odeurs senties est > au nombre de golems bloqués à proximité
			// Alors il y a au moins un Golem libre !
			if (nbStenchesObserved > explainedByBlocked) {
				suspiciousSmells.add(smellNodeId);
				//System.out.println("Node non ignoré: " + smellNodeId);
			}
		}

		return suspiciousSmells;
	}


	private List<String> getNodesWithAtMostNbNeighbours(int nbNeighbours) {
		List<String> filteredNodes = new ArrayList<>();

		this.g.nodes().forEach(n -> {
			// 1. On compte le nombre de voisins "utilisables" (ceux qui n'ont pas d'AGENT)
			long effectiveDegree = n.neighborNodes()
					.filter(neighbor -> {
						Object etAttr = neighbor.getAttribute("entityType");
						// Si l'attribut est null, on considère la case libre.
						// Sinon, on vérifie si ce n'est pas un AGENT.
						if (etAttr == null) return true;
						return !(NodeInfo.Entity.AGENT.toString().equals(etAttr.toString())
						&& ((long )n.getAttribute("lastUpdate") - System.currentTimeMillis()) <= 1);
					})
					.count();

			// 2. On vérifie aussi si le noeud lui-même n'est pas déjà occupé par un agent
			Object currentEntity = n.getAttribute("entityType");
			boolean isCurrentNodeFree = (currentEntity == null || !NodeInfo.Entity.AGENT.toString().equals(currentEntity.toString()));

			if (isCurrentNodeFree && effectiveDegree <= nbNeighbours) {
				filteredNodes.add(n.getId());
			}
		});

		return filteredNodes;
	}
	// getShortestPath sur énormément de noeuds, à voir si faut changer
	public String selectBestChokePoint(String golemPos, int nbAgents) {
		List<String> candidates = getNodesWithAtMostNbNeighbours(nbAgents);
		String bestNode = null;
		double maxScore = -1.0;

		for (String nodeId : candidates) {
			List<String> path = getShortestPath(golemPos, nodeId);
			double dist = (path != null) ? path.size() : 999; // Si inaccessible, on met une distance infinie
			double proximityScore = 1.0 / (dist + 1);

			double randFactor = Math.random();

			double finalScore = (proximityScore * 0.7) + (randFactor * 0.3);

			if (finalScore > maxScore) {
				maxScore = finalScore;
				bestNode = nodeId;
			} 
		}
		if(bestNode == null)System.out.println("VIDEEEEEEEEEEEEEEEE");
		else System.out.println("BEST NODE: " + bestNode);
		return bestNode;
	}
	
	// Extremement lourd en calcul, trouver autre chose si c'est trop lent
	public HashMap<String, String> chokePointAllocation(HashMap<String, String> agentPos, String selectedChokePoint, String currentPos) {
		//System.out.println("allocation en cours");
		Node n = this.g.getNode(selectedChokePoint);
		List<String> availableSlots = n.neighborNodes().map(Node::getId).collect(Collectors.toList());
		availableSlots.remove(currentPos);

		List<AssignmentCandidate> candidates = new ArrayList<>();

		for (String agentName : agentPos.keySet()) {
			String curPos = agentPos.get(agentName);
			for (String slot : availableSlots) {
				List<String> path = getShortestPath(curPos, slot);
				int dist = (path != null) ? path.size() : 999;
				candidates.add(new AssignmentCandidate(agentName, slot, dist));
			}
		}

		Collections.sort(candidates, Comparator.comparingInt(c -> c.distance));

		HashMap<String, String> assignments = new HashMap<>();
		Set<String> assignedAgents = new HashSet<>();
		Set<String> occupiedSlots = new HashSet<>();

		for (AssignmentCandidate c : candidates) {
			if (!assignedAgents.contains(c.agentName) && !occupiedSlots.contains(c.slotId)) {
				assignments.put(c.agentName, c.slotId);
				assignedAgents.add(c.agentName);
				occupiedSlots.add(c.slotId);
				//System.out.println("agent_name: " + c.agentName + " | slot: " + c.slotId);
			}
		}
		return assignments;
	}

	private static class AssignmentCandidate {
		String agentName;
		String slotId;
		int distance;
		AssignmentCandidate(String a, String s, int d) {
			this.agentName = a; this.slotId = s; this.distance = d;
		}
	}
	
	public Graph getG(){
		return this.g;
	}
	

}