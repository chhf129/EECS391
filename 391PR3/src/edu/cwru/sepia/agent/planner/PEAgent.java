package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Template;
import edu.cwru.sepia.environment.model.state.Unit;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * This is an outline of the PEAgent. Implement the provided methods. You may add your own methods and members.
 */
public class PEAgent extends Agent {

    // The plan being executed
    private Stack<StripsAction> plan = null;

    // maps the real unit Ids to the plan's unit ids
    // when you're planning you won't know the true unit IDs that sepia assigns. So you'll use placeholders (1, 2, 3).
    // this maps those placeholders to the actual unit IDs.
    private Map<Integer, Integer> peasantIdMap;
    private int townhallId;
    private int peasantTemplateId;
    private boolean updateIDmap=true;

    public PEAgent(int playernum, Stack<StripsAction> plan) {
        super(playernum);
        peasantIdMap = new HashMap<Integer, Integer>();
        this.plan = plan;

    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {
        // gets the townhall ID and the peasant ID
    	int id=1;
        for(int unitId : stateView.getUnitIds(playernum)) {
            Unit.UnitView unit = stateView.getUnit(unitId);
            String unitType = unit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall")) {
                townhallId = unitId;
            } else if(unitType.equals("peasant")) {
                peasantIdMap.put(id, unitId);
                id++;
            }
        }

        // Gets the peasant template ID. This is used when building a new peasant with the townhall
        for(Template.TemplateView templateView : stateView.getTemplates(playernum)) {
            if(templateView.getName().toLowerCase().equals("peasant")) {
                peasantTemplateId = templateView.getID();
                break;
            }
        }

        return middleStep(stateView, historyView);
    }

    /**
     * This is where you will read the provided plan and execute it. If your plan is correct then when the plan is empty
     * the scenario should end with a victory. If the scenario keeps running after you run out of actions to execute
     * then either your plan is incorrect or your execution of the plan has a bug.
     *
     * You can create a SEPIA deposit action with the following method
     * Action.createPrimitiveDeposit(int peasantId, Direction townhallDirection)
     *
     * You can create a SEPIA harvest action with the following method
     * Action.createPrimitiveGather(int peasantId, Direction resourceDirection)
     *
     * You can create a SEPIA build action with the following method
     * Action.createPrimitiveProduction(int townhallId, int peasantTemplateId)
     *
     * You can create a SEPIA move action with the following method
     * Action.createCompoundMove(int peasantId, int x, int y)
     *
     * these actions are stored in a mapping between the peasant unit ID executing the action and the action you created.
     *
     * For the compound actions you will need to check their progress and wait until they are complete before issuing
     * another action for that unit. If you issue an action before the compound action is complete then the peasant
     * will stop what it was doing and begin executing the new action.
     *
     * To check an action's progress you can call getCurrentDurativeAction on each UnitView. If the Action is null nothing
     * is being executed. If the action is not null then you should also call getCurrentDurativeProgress. If the value is less than
     * 1 then the action is still in progress.
     *
     * Also remember to check your plan's preconditions before executing!
     */
    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
        //TODO parallelize actions for different units
    	Map<Integer,Action> actions=new HashMap<>();
    	if (plan.isEmpty()){
    		return actions;
    	}
    	if (updateIDmap){
        	int id=1;
            for(int unitId : stateView.getUnitIds(playernum)) {
                Unit.UnitView unit = stateView.getUnit(unitId);
                String unitType = unit.getTemplateView().getName().toLowerCase();
                if(unitType.equals("peasant")) {
                    peasantIdMap.put(id, unitId);
                    id++;
                }
            }
            updateIDmap=false;
    	}
    	
		Map<Integer,Action> tempActions=new HashMap<>();
		boolean canGoNext=true;
        StripsAction action=plan.peek();
    	if (action instanceof BuildPeasant){
    		System.out.println("build peasant!!");
    		actions.put(townhallId, createSepiaAction(action,stateView).get(0));
    		plan.pop();
    		updateIDmap=true;
    	}
    	else{
    		Map<Integer, ActionResult> actionResults=new HashMap<>();
			if (stateView.getTurnNumber() != 0) {
				actionResults= historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
				for (ActionResult result : actionResults.values()) {
    			    System.out.println(result.toString());
    			  }
			}
    		
    		List<Action> actionList=createSepiaAction(action,stateView);
    		for (Action tempAction:actionList){
    			int unitID=tempAction.getUnitId();
    			ActionResult result=actionResults.get(unitID);
    			if (result==null || result.getFeedback().equals(ActionFeedback.COMPLETED)){
    				tempActions.put(unitID, tempAction);
    				}
    			else if (result.getFeedback().equals(ActionFeedback.FAILED)){
    				canGoNext=true;
    			}
    			else{
    				canGoNext=false;
    				}
    		}
        	if (canGoNext){
        		actions.putAll(tempActions);
        		plan.pop();
        	}
    	}

        return actions;
    }

    /**
     * Returns a SEPIA version of the specified Strips Action.
     * @param action StripsAction
     * @return SEPIA representation of same action
     */
    private List<Action> createSepiaAction(StripsAction action, State.StateView stateView) {
    	List<Action> returnAction=new LinkedList<>();
    	Unit.UnitView townHall=stateView.getUnit(townhallId);
    	Position townHallPos=new Position(townHall.getXPosition(),townHall.getYPosition());
    	if (action instanceof BuildPeasant){
    		returnAction.add(Action.createPrimitiveProduction(townhallId,peasantTemplateId));
    	}
    	else{
    		Unit.UnitView peasant;
    		Position unitPos;
    		int unitID;
    		if (action instanceof DepositRes ){
    			unitID=peasantIdMap.get(((DepositRes) action).unitID);
    			peasant=stateView.getUnit(unitID);
    			unitPos=new Position(peasant.getXPosition(),peasant.getYPosition());
    			returnAction.add(Action.createPrimitiveDeposit(unitID,unitPos.getDirection(townHallPos)));
    		}
    		else if (action instanceof DoubleDeposit){
    			returnAction.addAll(createSepiaAction(((DoubleDeposit) action).deposit1,stateView));
    			returnAction.addAll(createSepiaAction(((DoubleDeposit) action).deposit2,stateView));
    		}
    		else if (action instanceof TripleDeposit){
    			returnAction.addAll(createSepiaAction(((TripleDeposit) action).deposit1,stateView));
    			returnAction.addAll(createSepiaAction(((TripleDeposit) action).deposit2,stateView));
    			returnAction.addAll(createSepiaAction(((TripleDeposit) action).deposit3,stateView));
    		}
    		else if (action instanceof GatherRes){
    			unitID=peasantIdMap.get(((GatherRes) action).unitID);
    			peasant=stateView.getUnit(unitID);
    			unitPos=new Position(peasant.getXPosition(),peasant.getYPosition());
    			
    			ResourceView res=stateView.getResourceNode(((GatherRes) action).resID);
    			Position resPos=new Position(res.getXPosition(),res.getYPosition());
    			returnAction.add(Action.createPrimitiveGather(unitID, unitPos.getDirection(resPos)));
    		}
    		else if (action instanceof DoubleGather){
    			returnAction.addAll(createSepiaAction(((DoubleGather) action).gather1,stateView));
    			returnAction.addAll(createSepiaAction(((DoubleGather) action).gather2,stateView));
    		}
    		else if (action instanceof TripleGather){
    			returnAction.addAll(createSepiaAction(((TripleGather) action).gather1,stateView));
    			returnAction.addAll(createSepiaAction(((TripleGather) action).gather2,stateView));
    			returnAction.addAll(createSepiaAction(((TripleGather) action).gather3,stateView));
    		}
    		else if (action instanceof StripsMove){
    			unitID=peasantIdMap.get(((StripsMove) action).unitID);
    			Position end=((StripsMove) action).end;
    			returnAction.add(Action.createCompoundMove(unitID, end.x, end.y));
    		}
    		else if (action instanceof DoubleMove){
    			returnAction.addAll(createSepiaAction(((DoubleMove) action).move1,stateView));
    			returnAction.addAll(createSepiaAction(((DoubleMove) action).move2,stateView));
    		}
    		else if (action instanceof TripleMove){
    			returnAction.addAll(createSepiaAction(((TripleMove) action).move1,stateView));
    			returnAction.addAll(createSepiaAction(((TripleMove) action).move2,stateView));
    			returnAction.addAll(createSepiaAction(((TripleMove) action).move3,stateView));
    		}
    	}
        return returnAction;
    }

    @Override
    public void terminalStep(State.StateView stateView, History.HistoryView historyView) {

    }

    @Override
    public void savePlayerData(OutputStream outputStream) {

    }

    @Override
    public void loadPlayerData(InputStream inputStream) {

    }
}
