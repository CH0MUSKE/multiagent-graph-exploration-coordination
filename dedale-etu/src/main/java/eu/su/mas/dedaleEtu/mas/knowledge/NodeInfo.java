package eu.su.mas.dedaleEtu.mas.knowledge;

import eu.su.mas.dedale.env.Observation;

import java.io.Serializable;

public class NodeInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    MapRepresentation.MapAttribute statut;
    long lastUpdate;
    public enum Entity{
        GOLEM, AGENT, EMPTY
    }
    public Entity entityType;
    public String entityId;
    public int smell = 0;
    public long blockedLastUpdate;
    public boolean blocked;
    
    
    public NodeInfo(MapRepresentation.MapAttribute statut, long lastUpdate, Entity entityType, String entityId, int smell, long blockedLastUpdate, boolean blocked){
        this.statut = statut;
        this.lastUpdate = lastUpdate;
        this.entityType = entityType;
        this.entityId = entityId;
        this.smell = smell;
        this.blockedLastUpdate = blockedLastUpdate;
        this.blocked = blocked;
    }
}
