package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.ResourceInfo;
import edu.cwru.sepia.environment.model.state.ResourceNode;

public class DoubleGather implements StripsAction {

	public int unit1, unit2, resource1, resource2;
	public StripsAction gather1, gather2;
	
	public DoubleGather(int u1, int u2, int r1, int r2){
		unit1 = u1;
		unit2 = u2;
		resource1 = r1;
		resource2 = r2;
	}
	
	@Override
	public boolean preconditionsMet(GameState state) {
		ResourceNode.Type t = findResourceType(resource1, state);
		gather1 = generateGather(unit1, resource1, t);
		t = findResourceType(resource2, state);
		gather2 = generateGather(unit2, resource2, t);
		return gather1.preconditionsMet(state) && gather2.preconditionsMet(state);
	}

	@Override
	public GameState apply(GameState state) {
		GameState newState = gather2.apply(gather1.apply(state));
		newState.cost = state.cost + 1;
		newState.heuristic = newState.heuristic();
		newState.parent = state;
		newState.cause = this;
		return null;
	}
	
	public ResourceNode.Type findResourceType(int id, GameState state){
		for (ResourceInfo ri: state.goldmines){
			if (ri.id == id){
				return ri.type;
			}
		}
		for (ResourceInfo ri: state.forests){
			if (ri.id == id){
				return ri.type;
			}
		}
		return null;
	}
	
	public StripsAction generateGather(int unitID, int resourceID, ResourceNode.Type type){
		if (type == ResourceNode.Type.GOLD_MINE){
			return new GatherGold(unitID, resourceID);
		} else {
			return new GatherWood(unitID, resourceID);
		}
	}

}
