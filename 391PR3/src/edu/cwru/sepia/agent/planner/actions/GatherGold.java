package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.agent.planner.GameState.Peasant;
import edu.cwru.sepia.agent.planner.GameState.ResourceInfo;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

public class GatherGold implements StripsAction {

	public int unitID,goldID;
	ResourceInfo gold=null;
	Peasant peasant=null; 
	GatherGold(int unitID,int goldID){
		this.unitID=unitID;
		this.goldID=goldID;
	}
	
	@Override
	public boolean preconditionsMet(GameState state) {
		

		for (ResourceInfo ri: state.goldmines){
			if (ri.id==goldID){
				gold=ri;
			}
		}
		for (Peasant p: state.peasants){
			if (p.id == unitID){
				peasant=p;
			}
		}
		
		if (peasant.location.isAdjacent(gold.location) && gold.amount>0 &&
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
		peasant.resourceType=ResourceNode.Type.GOLD_MINE;
		
		return newState;
	}
	
}