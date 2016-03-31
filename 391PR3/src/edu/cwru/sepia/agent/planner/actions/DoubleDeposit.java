package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Peasant;
import edu.cwru.sepia.environment.model.state.ResourceNode;

public class DoubleDeposit implements StripsAction {

	public int unit1, unit2;
	public StripsAction deposit1, deposit2;
	
	public DoubleDeposit(int u1, int u2){
		unit1 = u1;
		unit2 = u2;
	}
	
	@Override
	public boolean preconditionsMet(GameState state) {
		deposit1 = generateDeposit(unit1, state);
		deposit2 = generateDeposit(unit2, state);
		return deposit1.preconditionsMet(state) && deposit2.preconditionsMet(state);
	}

	@Override
	public GameState apply(GameState state) {
		GameState newState = deposit2.apply(deposit1.apply(state));
		newState.cost = state.cost+1;
		newState.heuristic = newState.heuristic();
		newState.parent = state;
		newState.cause = this;
		return null;
	}
	
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

}