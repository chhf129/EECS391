package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Peasant;
import edu.cwru.sepia.agent.planner.TownHallInfo;
import edu.cwru.sepia.environment.model.state.ResourceNode;

public class DepositWood implements StripsAction {

	public int unitID;
	Peasant peasant=null;
	TownHallInfo townhall;
	public DepositWood(int unitID){
		this.unitID=unitID;
	}
	@Override
	public boolean preconditionsMet(GameState state) {
		townhall=state.townHall;
		for (Peasant p: state.peasants){
			if (p.id == unitID){
				peasant=p;
			}
		}
		if (peasant.isCarrying && peasant.resourceAmount>0 && peasant.resourceType.equals(ResourceNode.Type.TREE) && peasant.location.isAdjacent(townhall.location)){
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
		townhall.wood+=100;
		peasant.isCarrying=false;
		peasant.resourceAmount=0;
		newState.cost += 1;
		newState.heuristic = newState.heuristic();
		newState.cause = this;
		newState.parent = state;
		return newState;
	}

	public String toString(){
		return "DepositWood: peasant " + unitID;
	}
}
