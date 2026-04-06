package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

public class NodeInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    MapRepresentation.MapAttribute statut;
    long lastUpdate;
    
    public NodeInfo(MapRepresentation.MapAttribute statut, long lastUpdate){
        this.statut = statut;
        this.lastUpdate = lastUpdate;
    }
}
