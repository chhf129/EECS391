package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Peasant;
import edu.cwru.sepia.environment.model.state.ResourceNode;

/**
 * Implements three deposit actions in parallel by performing them sequentially
 * and only increasing the cost by 1 action. The kind of deposit actions to be
 * performed are determined by the resources held by the peasants indicated by
 * the IDs passed in
 */
public class TripleDeposit extends DoubleDeposit implements StripsAction  {

	public int unit1, unit2, unit3;
	public DepositRes deposit1, deposit2, deposit3;
	
	public TripleDeposit(int u1, int u2, int u3){
		super(u1,u2);
		unit3 = u3;
	}
	
	public TripleDeposit(DepositRes d1, DepositRes d2,DepositRes d3){
		super(d1,d2);
		unit3=d3.unitID;
		deposit3=new DepositRes(d3);
	}
	@Override
	public boolean preconditionsMet(GameState state) {
	//	deposit1 = generateDeposit(unit1, state);
	//	deposit2 = generateDeposit(unit2, state);
	//	deposit3 = generateDeposit(unit3, state);
		return deposit1.preconditionsMet(state) && deposit2.preconditionsMet(state) && deposit3.preconditionsMet(state);
	}

	@Override
	public GameState apply(GameState state) {
		GameState newState = deposit3.apply(deposit2.apply(deposit1.apply(state)));
		newState.cost = state.cost+1;
		newState.heuristic = newState.heuristic();
		newState.parent = state;
		newState.cause.add(this);
		return newState;
	}
	
	/**
	 * Generates an appropriate deposit action based on what the peasant is carrying
	 * @param unitID ID of the peasant
	 * @param state state to look in
	 * @return
	 */
	public StripsAction generateDeposit(int unitID, GameState state){
		for (Peasant p: state.peasants){
			if (p.id == unitID){
				if (p.resourceType == ResourceNode.Type.GOLD_MINE){
					return new DepositGold(unitID);
				} else {
					return new DepositWood(unitID);
				}
			}
		}
		return null;
	}

	public String toString(){
		return "TripleDeposit:\n\t" + deposit1.toString() + "\n\t" + deposit2.toString() + "\n\t" + deposit3.toString();
	}
}
