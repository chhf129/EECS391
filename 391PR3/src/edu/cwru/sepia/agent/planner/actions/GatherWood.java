package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.agent.planner.ResourceInfo;
import edu.cwru.sepia.agent.planner.Peasant;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

public class GatherWood implements StripsAction {

	public int unitID,woodID;
	ResourceInfo wood=null;
	Peasant peasant=null; 
	public GatherWood(int unitID,int woodID){
		this.unitID=unitID;
		this.woodID=woodID;
	}
	
	@Override
	public boolean preconditionsMet(GameState state) {
		

		for (ResourceInfo ri: state.forests){
			if (ri.id==woodID){
				wood=ri;
			}
		}
		for (Peasant p: state.peasants){
			if (p.id == unitID){
				peasant=p;
			}
		}
		
		if (peasant.location.isAdjacent(wood.location) && wood.amount>0 &&
				!peasant.isCarrying){
			return true;
		}
		
		return false;
	}

	@Override
	public GameState apply(GameState state) {
		GameState newState=new GameState(state);
		if (!preconditionsMet(newState)){
			return null;
		}
		peasant.isCarrying=true;
		peasant.resourceAmount=100;
		peasant.resourceType=ResourceNode.Type.TREE;
		wood.amount-=100;
		newState.cost += 1;
		newState.heuristic = newState.heuristic();
		newState.cause = this;
		newState.parent = state;
		return newState;
	}
	
	public String toString(){
		return "GatherWood: peasant " + unitID + " from forest " + woodID;
	}
}
