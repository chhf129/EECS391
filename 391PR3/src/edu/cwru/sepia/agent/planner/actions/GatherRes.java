package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Peasant;
import edu.cwru.sepia.agent.planner.ResourceInfo;
import edu.cwru.sepia.environment.model.state.ResourceNode;

public class GatherRes implements StripsAction {

	public int unitID,resID;
	ResourceInfo res=null;
	Peasant peasant=null; 
	ResourceNode.Type resType=null;
	public GatherRes(int unitID,int resID){
		this.unitID=unitID;
		this.resID=resID;
	}
	
	public GatherRes(GatherRes g){
		unitID=g.unitID;
		resID=g.resID;
		resType=g.resType;
	}
	
	@Override
	public boolean preconditionsMet(GameState state) {
		
		if (StripsAction.findResourceType(resID,state).equals(ResourceNode.Type.GOLD_MINE)){
			resType=ResourceNode.Type.GOLD_MINE;
			for (ResourceInfo ri: state.goldmines){
				if (ri.id==resID){
					res=ri;
				}
			}
		}
		else{
			resType=ResourceNode.Type.TREE;
			for (ResourceInfo ri: state.forests){
				if (ri.id==resID){
					res=ri;
				}
			}
		}
		
		for (Peasant p: state.peasants){
			if (p.id == unitID){
				peasant=p;
			}
		}
		
		if (peasant.location.isAdjacent(res.location) && res.amount>0 &&
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
		peasant.resourceType=resType;
		res.amount-=100;
		newState.cost += 1;
		newState.heuristic = newState.heuristic();
		newState.cause = this;
		newState.parent = state;
		
		return newState;
	}
	
	
	public String toString(){
		if (resType.equals(ResourceNode.Type.GOLD_MINE)){
			return "GatherGold: peasant " + unitID + " from mine " + resID;
		}
		else {
			return "GatherWood: peasant " + unitID + " from forest " + resID;
		}
	}
}
