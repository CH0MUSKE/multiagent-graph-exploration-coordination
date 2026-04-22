package eu.su.mas.dedaleEtu.mas.agents.dummies;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.Behaviour;
import net.sourceforge.plantuml.salt.element.ListWidth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractCommunicationAgent extends AbstractDedaleAgent{
    public enum Status{
        HUNTING, EXPLORING, SMELLING, BLOCKING
    }
    protected HashMap<String, Long> sentMapsRegistry = new HashMap<>();
    //Pour l'échange de map seulement. TODO: CHANGER LE NOM
    protected HashMap<String, Long> lastConvRegistry = new HashMap<>();
    //Pour l'échange de map seulement. TODO: CHANGER LE NOM
    protected HashMap<String, Long> activeConversations = new HashMap<>();
    protected HashMap<String, String> coalition = new HashMap<>();
    protected String coalitionId = "";
    //Pour l'échange de map seulement. TODO: CHANGER LE NOM
    protected static final long CONV_TIMEOUT = 5000;
    protected MapRepresentation myMap;
    protected String golemPos = "";
    protected List<String> children = new ArrayList<>();
    protected String leaderId = "";
    protected String orderTarget = "";
    protected Status status = Status.EXPLORING;
    public List<String> agentList = new ArrayList<>();
    private Behaviour currentCoalitionBehaviour = null;
    private String parent = "";
    

    
    public void markMapAsSent(String user_id, Long currTime) {
        this.sentMapsRegistry.put(user_id, currTime);
    }

    
    public boolean hasAlreadySentMap(String agentId) {
        if (!this.sentMapsRegistry.containsKey(agentId)) {
            return false;
        }

        long lastSentTime = this.sentMapsRegistry.get(agentId);
        long currentTime = System.currentTimeMillis();

        if ((currentTime - lastSentTime) > CONV_TIMEOUT) {
            this.sentMapsRegistry.remove(agentId);
            return false;
        }

        return true;
    }
    
    public Long getSentMapDate(String sender_id){
        return this.sentMapsRegistry.get(sender_id);
    }
    
    public void deleteEntrySentMapRegistry(String sender_id){
        this.sentMapsRegistry.remove(sender_id);
    }

    
    public void clearMapRegistry(){
        this.sentMapsRegistry.clear();
    }
    
    public void addConvId(String id){
        activeConversations.put(id,System.currentTimeMillis());
    }
    
    public boolean checkValidId(String id){
        if (id == null || !activeConversations.containsKey(id)) {
            return false;
        }

        long startTime = activeConversations.get(id);
        if ((System.currentTimeMillis() - startTime) > CONV_TIMEOUT) {
            activeConversations.remove(id);
            return false;
        }
        return true;
    }
    
    public boolean checkValidCoalition(String coalitionId){
        return this.coalitionId.equals(coalitionId);
    }

    
    public MapRepresentation getMyMap(){
        return myMap;
    }

    
    public Long getLastConvRegistry(String id) {
        return lastConvRegistry.get(id);
    }
    
    public void setLastConvRegistry(String id, Long last_update) {
        this.lastConvRegistry.put(id, last_update);
    }

    
    public void setMap(MapRepresentation map){
        this.myMap = map;
    }

    
    public String getCoalitionPos(String id) {
        return this.coalition.get(id);
    }
    
    
    public void addAgentToCoalition(String id, String pos) {
        this.coalition.put(id, pos);
    }
    
    public String getCoalitionId() {
        return coalitionId;
    }
    
    public void setCoalitionId(String coalitionId) {
        this.coalitionId = coalitionId;
    }
    
    
    public int getCoalitionSize(){
        return coalition.size();
    }
    
    
    public String getGolemPos(){
        return this.golemPos;
    }
    
    public void setGolemPos(String golemPos){
        this.golemPos = golemPos;
    }
    
    public HashMap<String,String> getCoalition(){
        return this.coalition;
    }

    
    public List<String> getChildren() {
        return this.children;
    }
    
    
    public void setLeaderId(String leaderId){
        this.leaderId = leaderId;
    }
    
    
    public String getLeaderId(){
        return this.leaderId;
    }
    
    public void setOrderTarget(String target){
        this.orderTarget = target;
    }
    
    public String getOrderTarget(){
        return orderTarget;
    }
    
    public void setStatus(Status status){
        this.status = status;
    }
    
    public Status getStatus(){
        return this.status;
    }
    public void setCurrentCoalitionBehaviour(Behaviour b) {
        this.currentCoalitionBehaviour = b;
    }

    public Behaviour getCurrentCoalitionBehaviour() {
        return this.currentCoalitionBehaviour;
    }
    
    public void startCoalition(Behaviour coalition, Status status, String leaderId, String golemPos, String coalitionId, String parent){
        this.setStatus(status);
        this.setLeaderId(leaderId);
        this.setGolemPos(golemPos);
        this.setCoalitionId(coalitionId);
        this.setCurrentCoalitionBehaviour(coalition);
        this.addBehaviour(coalition);
        this.setParent(parent);
    }
    
    public void resetCoalitionData() {
        this.currentCoalitionBehaviour = null;
        this.status = Status.EXPLORING;
        this.children.clear();
        this.coalition.clear();
        this.setParent("");
    }

    public void stopCurrentCoalition() {
        if (this.currentCoalitionBehaviour != null) {
            this.removeBehaviour(this.currentCoalitionBehaviour);
        }
        this.leaderId = "";
        this.orderTarget = "";
        resetCoalitionData();
    }
    
    public void setParent(String parent){
        this.parent = parent;
    }
    
    public String getParent(){
        return this.parent;
    }
}
