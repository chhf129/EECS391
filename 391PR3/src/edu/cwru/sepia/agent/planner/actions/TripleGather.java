package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.ResourceInfo;
import edu.cwru.sepia.environment.model.state.ResourceNode;

/**
 * Implements three gather actions in parallel by performing them sequentially
 * and only increasing the cost by 1 action. The kind of gather actions to be
 * performed are determined by the resources indicated by the id's passed in.
 *
 */
public class TripleGather implements StripsAction {

	public int unit1, unit2, unit3, resource1, resource2, resource3;
	public StripsAction gather1, gather2, gather3;
	
	public TripleGather(int u1, int u2, int u3, int r1, int r2, int r3){
		unit1 = u1;
		unit2 = u2;
		unit3 = u3;
		resource1 = r1;
		resource2 = r2;
		resource3 = r3;
	}
	
	public TripleGather(){}
	
	@Override
	public boolean preconditionsMet(GameState state) {
		gather1 = generateGather(unit1, resource1, state);
		gather2 = generateGather(unit2, resource2, state);
		gather3 = generateGather(unit3, resource3, state);
		return gather1.preconditionsMet(state) && gather2.preconditionsMet(state) && gather3.preconditionsMet(state);
	}

	@Override
	public GameState apply(GameState state) {
		GameState newState = gather3.apply(gather2.apply(gather1.apply(state)));
		newState.cost = state.cost + 1;
		newState.heuristic = newState.heuristic();
		newState.parent = state;
		newState.cause = this;
		return newState;
	}
	
	/**
	 * Generates an appropriate Strips action given a kind of resource to gather
	 * @param unitID Id of unit to perform action
	 * @param resourceID Id of resource to gather from
	 * @param type Type of resource to gather
	 * @return
	 */
	public StripsAction generateGather(int unitID, int resourceID, GameState state){
		if (findResourceType(resourceID, state) == ResourceNode.Type.GOLD_MINE){
			return new GatherGold(unitID, resourceID);
		} else {
			return new GatherWood(unitID, resourceID);
		}
	}
	
	/**
	 * Given a resource id, searches for it in the state and returns its type
	 * @param id Id of the resource to find
	 * @param state State to search in
	 * @return the ResourceNode.Type corresponding to the resource
	 */
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
	
	public String toString(){
		return "TripleGather:\n\t" + gather1.toString() + "\n\t" + gather2.toString() + "\n\t" + gather3.toString();
	}
}
