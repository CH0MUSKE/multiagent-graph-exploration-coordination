package eu.su.mas.dedaleEtu.mas.agents.dummies.explo;

import eu.su.mas.dedale.env.EntityCharacteristics;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.StartMyBehaviours;
import eu.su.mas.dedaleEtu.mas.agents.dummies.AbstractCommunicationAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploreCoopBehaviour2;
import eu.su.mas.dedaleEtu.mas.behaviours.ListenerBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.utils.MultiAgentRouterStream;
import jade.core.behaviours.Behaviour;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

public class ExploreCoopAgent2 extends AbstractCommunicationAgent {
    private static final long serialVersionUID = -7969469610241668130L;

    
    protected void setup(){

        super.setup();

        final Object[] args = getArguments();
        List<String> list_agentNames = new ArrayList<>();

        if (args != null && args.length > 0) {
            EntityCharacteristics ec = (EntityCharacteristics) args[0];

            List<String> userParams = ec.getUserParameters();

            if (userParams != null && !userParams.isEmpty()) {
                list_agentNames.addAll(userParams);
                System.out.println(this.getLocalName() + " contactera : " + list_agentNames);
            }
        }
        this.agentList = list_agentNames;


        List<Behaviour> lb=new ArrayList<Behaviour>();

        /************************************************
         *
         * ADD the behaviours of the Dummy Moving Agent
         *
         ************************************************/
        lb.add(new ExploreCoopBehaviour2(this, list_agentNames));
        lb.add(new ListenerBehaviour(this));




        /***
         * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
         */


        addBehaviour(new StartMyBehaviours(this,lb));

        System.out.println("the  agent "+this.getLocalName()+ " is started");

    }
    
    /**
     * This method is automatically called after doDelete()
     */
    protected void takeDown(){
        super.takeDown();
    }

    protected void beforeMove(){
        super.beforeMove();
        //System.out.println("I migrate");
    }

    protected void afterMove(){
        super.afterMove();
        //System.out.println("I migrated");
    }


}
