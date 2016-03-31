package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Template;
import edu.cwru.sepia.environment.model.state.Unit;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
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

    public PEAgent(int playernum, Stack<StripsAction> plan) {
        super(playernum);
        peasantIdMap = new HashMap<Integer, Integer>();
        this.plan = plan;

    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {
        // gets the townhall ID and the peasant ID
        for(int unitId : stateView.getUnitIds(playernum)) {
            Unit.UnitView unit = stateView.getUnit(unitId);
            String unitType = unit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall")) {
                townhallId = unitId;
            } else if(unitType.equals("peasant")) {
                peasantIdMap.put(unitId, unitId);
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
        StripsAction action=plan.peek();
    	if (action instanceof BuildPeasant){
    		
    		actions.put(townhallId, createSepiaAction(action,stateView));
    		plan.pop();
    	}
    	else{
    		Action tempAction=createSepiaAction(action,stateView);
    		int unitID=tempAction.getUnitId();
    		
    		if (stateView.getTurnNumber() != 0) {

    			  Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
    			  
    			  
    			  for (ActionResult result : actionResults.values()) {
    				  if (actionResults.size()!=1){
    					  System.err.println("more than 1 action assigned to unit");
    				  }
    			    System.out.println(result.toString());

    			  }
    			  
    			  ActionResult result=actionResults.get(unitID);
    			  if (result==null || result.getFeedback().equals(ActionFeedback.COMPLETED)){
    				  actions.put(unitID, tempAction);
    				  plan.pop();
   			  }
    			}
    		
    	}
        return actions;
    }

    /**
     * Returns a SEPIA version of the specified Strips Action.
     * @param action StripsAction
     * @return SEPIA representation of same action
     */
    private Action createSepiaAction(StripsAction action, State.StateView stateView) {
    	Action returnAction=null;
    	Unit.UnitView townHall=stateView.getUnit(townhallId);
    	Position townHallPos=new Position(townHall.getXPosition(),townHall.getYPosition());
    	if (action instanceof BuildPeasant){
    		returnAction=Action.createPrimitiveProduction(townhallId,peasantTemplateId);
    	}
    	else{
    		Unit.UnitView peasant;
    		Position unitPos;
    		int unitID;
    		/*
    		if (action instanceof DepositGold ){
    			unitID=((DepositGold) action).unitID;
    			peasant=stateView.getUnit(unitID);
    			unitPos=new Position(peasant.getXPosition(),peasant.getYPosition());
    			returnAction=Action.createPrimitiveDeposit(unitID,unitPos.getDirection(townHallPos));
    		}
    		else if (action instanceof DepositWood){
    			unitID=((DepositWood) action).unitID;
    			peasant=stateView.getUnit(unitID);
    			unitPos=new Position(peasant.getXPosition(),peasant.getYPosition());
    			returnAction=Action.createPrimitiveDeposit(unitID,unitPos.getDirection(townHallPos));
    		}
    		*/
    		if (action instanceof DepositRes ){
    			unitID=((DepositRes) action).unitID;
    			peasant=stateView.getUnit(unitID);
    			unitPos=new Position(peasant.getXPosition(),peasant.getYPosition());
    			returnAction=Action.createPrimitiveDeposit(unitID,unitPos.getDirection(townHallPos));
    		}
    		else if (action instanceof GatherRes){
    			unitID=((GatherRes) action).unitID;
    			peasant=stateView.getUnit(unitID);
    			unitPos=new Position(peasant.getXPosition(),peasant.getYPosition());
    			
    			ResourceView res=stateView.getResourceNode(((GatherRes) action).resID);
    			Position resPos=new Position(res.getXPosition(),res.getYPosition());
    			returnAction=Action.createPrimitiveGather(unitID, unitPos.getDirection(resPos));
    		}
    		/*
    		else if (action instanceof GatherGold){
    			unitID=((GatherGold) action).unitID;
    			peasant=stateView.getUnit(unitID);
    			unitPos=new Position(peasant.getXPosition(),peasant.getYPosition());
    			
    			ResourceView gold=stateView.getResourceNode(((GatherGold) action).goldID);
    			Position resPos=new Position(gold.getXPosition(),gold.getYPosition());
    			returnAction=Action.createPrimitiveGather(unitID, unitPos.getDirection(resPos));
    		}
    		else if (action instanceof GatherWood){
    			unitID=((GatherWood) action).unitID;
    			peasant=stateView.getUnit(unitID);
    			unitPos=new Position(peasant.getXPosition(),peasant.getYPosition());
    			
    			ResourceView wood=stateView.getResourceNode(((GatherWood) action).woodID);
    			Position resPos=new Position(wood.getXPosition(),wood.getYPosition());
    			returnAction=Action.createPrimitiveGather(unitID, unitPos.getDirection(resPos));
    		}
    		*/
    		
    		else if (action instanceof StripsMove){
    			unitID=((StripsMove) action).unitID;
    			Position end=((StripsMove) action).end;
    			returnAction=Action.createCompoundMove(unitID, end.x, end.y);
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
