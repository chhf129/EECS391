package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Peasant;
import edu.cwru.sepia.agent.planner.TownHallInfo;
import edu.cwru.sepia.environment.model.state.ResourceNode;

public class DepositRes implements StripsAction {

	public int unitID;
	Peasant peasant=null;
	TownHallInfo townhall;
	ResourceNode.Type resType=null;
	public DepositRes(int unitID){
		this.unitID=unitID;
	}
	public DepositRes(DepositRes d){
		unitID=d.unitID;
	}
	@Override
	public boolean preconditionsMet(GameState state) {
		townhall=state.townHall;
		for (Peasant p: state.peasants){
			if (p.id == unitID){
				peasant=p;
			}
		}
		resType=peasant.resourceType;
		if (peasant.isCarrying && peasant.resourceAmount>0 && peasant.location.isAdjacent(townhall.location)){
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
		townhall=newState.townHall;
		if (resType.equals(ResourceNode.Type.GOLD_MINE)){
			townhall.gold+=100;
		}
		else{
			townhall.wood+=100;
		}
		peasant.isCarrying=false;
		peasant.resourceAmount=0;
		newState.cost += 1;
		newState.heuristic = newState.heuristic();
		newState.cause = this;
		newState.parent = state;
		return newState;
	}

	public String toString(){
		if (resType.equals(ResourceNode.Type.GOLD_MINE)){
			return "DepositGold: peasant " + unitID;
		}
		else {
			return "DepositWood: peasant " + unitID;
		}
	}
}
