package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.ResourceInfo;
import edu.cwru.sepia.environment.model.state.ResourceNode;

/**
 * Implements two gather actions in parallel by performing them sequentially
 * and only increasing the cost by 1 action. The kind of gather actions to be
 * performed are determined by the resources indicated by the id's passed in.
 *
 */
public class DoubleGather implements StripsAction {

	public int unit1, unit2, resource1, resource2;
	public GatherRes gather1, gather2;
	
	public DoubleGather(int u1, int u2, int r1, int r2){
		unit1 = u1;
		unit2 = u2;
		resource1 = r1;
		resource2 = r2;
	}
	public DoubleGather(GatherRes g1, GatherRes g2){
		unit1=g1.unitID;
		resource1=g1.resID;
		unit2=g2.unitID;
		resource2=g2.resID;
		gather1=new GatherRes(g1);
		gather2=new GatherRes(g2);
	};
	
	@Override
	public boolean preconditionsMet(GameState state) {
	//	gather1 = generateGather(unit1, resource1, state);
	//	gather2 = generateGather(unit2, resource2, state);
		return gather1.preconditionsMet(state) && gather2.preconditionsMet(state);
	}

	@Override
	public GameState apply(GameState state) {
		GameState newState = gather2.apply(gather1.apply(state));
		newState.cost = state.cost + 1;
		newState.heuristic = newState.heuristic();
		newState.parent = state;
		newState.cause.add(this);
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
		return "DoubleGather:\n\t" + gather1.toString() + "\n\t" + gather2.toString();
	}
}
