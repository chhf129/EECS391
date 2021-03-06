package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.history.DamageLog;
import edu.cwru.sepia.environment.model.history.DeathLog;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;

import java.io.*;
import java.util.*;

public class RLAgent extends Agent {

    /**
     * Set in the constructor. Defines how many learning episodes your agent should run for.
     * When starting an episode. If the count is greater than this value print a message
     * and call sys.exit(0)
     */
    public final int numEpisodes;

    /**
     * List of your footmen and your enemies footmen
     */
    private List<Integer> myFootmen;
    private List<Integer> enemyFootmen;
    //stores targets of each of player's footman (-1 means no target or footman is dead)
    private List<Integer> targets;
    
    private boolean evaluationMode=false;
    private int numEpisodesPlayed=0;
    //stores estimated Q value on assignment of an action
    private List<Double> oldQValues;
    /**
     * Convenience variable specifying enemy agent number. Use this whenever referring
     * to the enemy agent. We will make sure it is set to the proper number when testing your code.
     */
    public static final int ENEMY_PLAYERNUM = 1;
 
    /**
     * Set this to whatever size your feature vector is.
     */
    public static final int NUM_FEATURES = 4;

    /** Use this random number generator for your epsilon exploration. When you submit we will
     * change this seed so make sure that your agent works for more than the default seed.
     */
    public final Random random = new Random(12345);
    
    /**
     * Your Q-function weights.
     */
    public Double[] weights;

    /**
     * These variables are set for you according to the assignment definition. You can change them,
     * but it is not recommended. If you do change them please let us know and explain your reasoning for
     * changing them.
     */
    public final double gamma = 0.9;
    public final double learningRate = .0001;
    public final double epsilon = .02;
    private double currentEpsilon=epsilon;
    
    //total reward for current episode
    private double currentEpisodeReward=0.0;
    private List<Double> runningResults;
    private List<Double> evaluationResults;
    
    //Map<AttackerID,DefenderID> contains information about which unit is attacking which unit
    private Map<Integer,Integer> attackMap;

    public RLAgent(int playernum, String[] args) {
        super(playernum);

        if (args.length >= 1) {
            numEpisodes = Integer.parseInt(args[0]);
            System.out.println("Running " + numEpisodes + " episodes.");
        } else {
            numEpisodes = 10;
            System.out.println("Warning! Number of episodes not specified. Defaulting to 10 episodes.");
        }

        boolean loadWeights = false;
        if (args.length >= 2) {
            loadWeights = Boolean.parseBoolean(args[1]);
        } else {
            System.out.println("Warning! Load weights argument not specified. Defaulting to not loading.");
        }

        if (loadWeights) {
            weights = loadWeights();
        } else {
            // initialize weights to random values between -1 and 1
            weights = new Double[NUM_FEATURES];
            for (int i = 0; i < weights.length; i++) {
                weights[i] = random.nextDouble() * 2 - 1;
            }
        }
    }

    /**
     * We've implemented some setup code for your convenience. Change what you need to.
     */
    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {
    	
        // You will need to add code to check if you are in a testing or learning episode
    	
        // Find all of your units
        myFootmen = new LinkedList<>();
        for (Integer unitId : stateView.getUnitIds(playernum)) {
            Unit.UnitView unit = stateView.getUnit(unitId);

            String unitName = unit.getTemplateView().getName().toLowerCase();
            if (unitName.equals("footman")) {
                myFootmen.add(unitId);
            } else {
                System.err.println("Unknown unit type: " + unitName);
            }
        }

        // Find all of the enemy units
        enemyFootmen = new LinkedList<>();
        for (Integer unitId : stateView.getUnitIds(ENEMY_PLAYERNUM)) {
            Unit.UnitView unit = stateView.getUnit(unitId);

            String unitName = unit.getTemplateView().getName().toLowerCase();
            if (unitName.equals("footman")) {
                enemyFootmen.add(unitId);
            } else {
                System.err.println("Unknown unit type: " + unitName);
            }
        }
        targets = new LinkedList<>();
        oldQValues = new LinkedList<>();
        for (int i: myFootmen){
        	targets.add(-1);
        	oldQValues.add(-1.0);
        }
        currentEpisodeReward=0.0;
        evaluationMode=(numEpisodesPlayed % 15)>9;
        if (runningResults == null){
        	runningResults = new LinkedList<Double>();
        }
        if (evaluationResults == null){
        	evaluationResults = new LinkedList<Double>();
        }
        attackMap=new HashMap<>(myFootmen.size());
        
        if (numEpisodesPlayed>1){
        	weights= loadWeights();
        }
        
        return middleStep(stateView, historyView);
    }

    /**
     * You will need to calculate the reward at each step and update your totals. You will also need to
     * check if an event has occurred. If it has then you will need to update your weights and select a new action.
     *
     * If you are using the footmen vectors you will also need to remove killed units. To do so use the historyView
     * to get a DeathLog. Each DeathLog tells you which player's unit died and the unit ID of the dead unit. To get
     * the deaths from the last turn do something similar to the following snippet. Please be aware that on the first
     * turn you should not call this as you will get nothing back.
     *
     * for(DeathLog deathLog : historyView.getDeathLogs(stateView.getTurnNumber() -1)) {
     *     System.out.println("Player: " + deathLog.getController() + " unit: " + deathLog.getDeadUnitID());
     * }
     *
     * You should also check for completed actions using the history view. Obviously you never want a footman just
     * sitting around doing nothing (the enemy certainly isn't going to stop attacking). So at the minimum you will
     * have an even whenever one your footmen's targets is killed or an action fails. Actions may fail if the target
     * is surrounded or the unit cannot find a path to the unit. To get the action results from the previous turn
     * you can do something similar to the following. Please be aware that on the first turn you should not call this
     *
     * Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
     * for(ActionResult result : actionResults.values()) {
     *     System.out.println(result.toString());
     * }
     *
     * @return New actions to execute or nothing if an event has not occurred.
     */
    /*
     * 	if event point(at least one unit is damaged or dead), 
     *  	remove all dead unit from storage list
     *  	update weights
     *  
     *  add action to unit who has nothing to do
     *  
     */
    @Override
   public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
    	//action feedback from previous step
    	Map<Integer, ActionResult> actionResults=null;
    	
    	//action map contains action for current step
    	Map<Integer, Action> returnActions = new HashMap<Integer, Action>();
    	if(stateView.getTurnNumber() > 0){
    		
    		actionResults= historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);

    		//if not event point keep execute the same action
    		if (!ifEventPoint(stateView,historyView)){
    			return returnActions;
    		}
    		
    		//remove all dead units
    		for(DeathLog deathLog : historyView.getDeathLogs(stateView.getTurnNumber() - 1)){
    			if(deathLog.getController() == playernum){
    				myFootmen.remove(myFootmen.indexOf(deathLog.getDeadUnitID()));
    				attackMap.remove(deathLog.getDeadUnitID());
    			}
    			else if(deathLog.getController() == ENEMY_PLAYERNUM)
    			{
    				enemyFootmen.remove(enemyFootmen.indexOf(deathLog.getDeadUnitID()));
    				
    			}
    		}
    		
    		//update reward for each my footmen
    		//because dead units are removed beforewards, weights for dead units are not updated
    		
        	double oldReward;
        	int targetID;
        	double oldFeatureVector[];
        	
    		for(int footmanID : myFootmen){
    			oldReward=calculateReward(stateView,historyView,footmanID);
    			currentEpisodeReward+=oldReward;
    			targetID=attackMap.get(footmanID);
    			//when target for this footman is dead, iterate to next footman
    			if (stateView.getUnit(targetID)==null){
    				continue;
    			}
    			oldFeatureVector=calculateFeatureVector(stateView, historyView, footmanID, targetID);
        		if (!evaluationMode){
        			weights=updateWeights(weights,oldFeatureVector,oldReward,stateView,historyView,footmanID);
        		}
    		}
    		
    	}
    	
		
		//add action to footman who has not action
    	int targetID;
    	for(int footmanID : myFootmen){
    		
    		//see if this footman can add action
    		if(actionResults == null || !actionResults.containsKey(footmanID) || 
    				actionResults.get(footmanID).getFeedback().equals(ActionFeedback.COMPLETED) ||
    				actionResults.get(footmanID).getFeedback().equals(ActionFeedback.FAILED)){
    			targetID = selectAction(stateView, historyView, footmanID);
    			
    			//add to attackMap so we know this footman is attacking which target
    			attackMap.put(footmanID, targetID);
    			returnActions.put(footmanID, Action.createCompoundAttack(footmanID, targetID));
    		}
		}
    	
    	return returnActions;
    }
    
    /*
     * event point is when at least one unit is dead or get hit
     */
    private boolean ifEventPoint(State.StateView stateView, History.HistoryView historyView){
    	return historyView.getDeathLogs(stateView.getTurnNumber() - 1).size()> 0 ||
    			historyView.getDamageLogs(stateView.getTurnNumber() - 1).size() > 0;
    }

    /**
     * Here you will calculate the cumulative average rewards for your testing episodes. If you have just
     * finished a set of test episodes you will call out testEpisode.
     *
     * It is also a good idea to save your weights with the saveWeights function.
     */
    @Override
    public void terminalStep(State.StateView stateView, History.HistoryView historyView) {   	
    	numEpisodesPlayed++; 
    	if (evaluationMode){
    		evaluationResults.add(currentEpisodeReward);
    		if (numEpisodesPlayed%5==0){
    			double total = 0;
    			for (Double d: evaluationResults){
    				total += d;
    			}
    			runningResults.add(total/5);
    			evaluationResults.clear();
    		}
    	}
        // MAKE SURE YOU CALL printTestData after you finish a test episode.
		if (numEpisodesPlayed >= numEpisodes*1.5){
			printTestData(runningResults);
			printData(runningResults);
		}
        // Save your weights
        saveWeights(weights);
        
        if (numEpisodesPlayed%15==0){
        	currentEpsilon-=0.002;
        	if (currentEpsilon<0){
        		currentEpsilon=0;
        	}
        }
        /*
		for (Unit.UnitView unit : stateView.getUnits(playernum)) {
			String unitTypeName = unit.getTemplateView().getName();
			if (unitTypeName.equals("Footman")) {
				System.out.println("WONNNNNN");
			}
		}
		*/

    }
    
    private void printData(List<Double> values){
    	for (int i=0; i<values.size(); i++){
    		System.out.println(values.get(i));
    	}
    }

    /**
     * Calculate the updated weights for this agent. 
     * @param oldWeights Weights prior to update
     * @param oldFeatures Features from (s,a)
     * @param totalReward Cumulative discounted reward for this footman.
     * @param stateView Current state of the game.
     * @param historyView History of the game up until this point
     * @param footmanId The footman we are updating the weights for
     * @return The updated weight vector.
     */
    public Double[] updateWeights(Double[] oldWeights, double[] oldFeatures, double totalReward, State.StateView stateView, History.HistoryView historyView, int footmanId) {
        Double[] newWeights = new Double[oldWeights.length];
        for (int i=0; i<newWeights.length; i++){
        	//calculate argmaxQ(s',a')
        	double bestQ = Double.NEGATIVE_INFINITY;
        	for (int n: enemyFootmen){
        		bestQ = Math.max(bestQ, calcQValue(stateView, historyView, footmanId, n));
        	}
        	//update weight according to wi <- wi + learningRate(reward + gamma(argmaxQ(s',a')) - Q(s,a))f(s,a)
        	newWeights[i] = oldWeights[i] + 
        			learningRate*oldFeatures[i]*(totalReward + gamma*bestQ - oldQValues.get(myFootmen.indexOf(footmanId)));
        }
    	return newWeights;
    }

    /**
     * Given a footman and the current state and history of the game select the enemy that this unit should
     * attack. This is where you would do the epsilon-greedy action selection.
     *
     * @param stateView Current state of the game
     * @param historyView The entire history of this episode
     * @param attackerId The footman that will be attacking
     * @return The enemy footman ID this unit should attack
     */
    //select action with greatest Q value
    public int selectAction(State.StateView stateView, History.HistoryView historyView, int attackerId) {
        int targetId=-1;
		if (!evaluationMode && (currentEpsilon > random.nextDouble())) {
			//choose random enemy
			Random ran = new Random();
			int i = ran.nextInt(enemyFootmen.size());
			targetId=enemyFootmen.get(i);
		}
		else{
			
        double targetQ = Double.NEGATIVE_INFINITY;
        for (int t: enemyFootmen){
        	double q = calcQValue(stateView, historyView, attackerId, t);
        	if (q > targetQ){
        		targetQ = q;
        		targetId = t;
        	}
        }
        oldQValues.set(myFootmen.indexOf(attackerId), targetQ);
		}
    	return targetId;
    }

    /**
     * Given the current state and the footman in question calculate the reward received on the last turn.
     * This is where you will check for things like Did this footman take or give damage? Did this footman die
     * or kill its enemy. Did this footman start an action on the last turn? See the assignment description
     * for the full list of rewards.
     *
     * Remember that you will need to discount this reward based on the timestep it is received on. See
     * the assignment description for more details.
     *
     * As part of the reward you will need to calculate if any of the units have taken damage. You can use
     * the history view to get a list of damages dealt in the previous turn. Use something like the following.
     *
     * for(DamageLog damageLogs : historyView.getDamageLogs(lastTurnNumber)) {
     *     System.out.println("Defending player: " + damageLog.getDefenderController() + " defending unit: " + \
     *     damageLog.getDefenderID() + " attacking player: " + damageLog.getAttackerController() + \
     *     "attacking unit: " + damageLog.getAttackerID());
     * }
     *
     * You will do something similar for the deaths. See the middle step documentation for a snippet
     * showing how to use the deathLogs.
     *
     * To see if a command was issued you can check the commands issued log.
     *
     * Map<Integer, Action> commandsIssued = historyView.getCommandsIssued(playernum, lastTurnNumber);
     * for (Map.Entry<Integer, Action> commandEntry : commandsIssued.entrySet()) {
     *     System.out.println("Unit " + commandEntry.getKey() + " was command to " + commandEntry.getValue().toString);
     * }
     *
     * @param stateView The current state of the game.
     * @param historyView History of the episode up until this turn.
     * @param footmanId The footman ID you are looking for the reward from.
     * @return The current reward
     */
    public double calculateReward(State.StateView stateView, History.HistoryView historyView, int footmanId) {
    	double reward = 0;
    	int targetId = -1;
    	//check if the unit participated in combat this turn
    	for (DamageLog dl: historyView.getDamageLogs(stateView.getTurnNumber()-1)){
    		//if attacking, add to reward and check for kill later
    		if (dl.getAttackerController() == 0 && dl.getAttackerID() == footmanId){
    			reward += dl.getDamage();
    			targetId = dl.getDefenderID();
    		//if attacked, subtract from reward
    		} else if (dl.getDefenderController() == 0 && dl.getDefenderID() == footmanId){
    			reward -= dl.getDamage();
    		}
    	}
    	//check for important deaths
    	for (DeathLog dl: historyView.getDeathLogs(stateView.getTurnNumber()-1)){
    		//if the unit died subtract from reward
    		if (dl.getController() == 0 && dl.getDeadUnitID() == footmanId){
    			reward -= 100;
    		//if unit killed its target add to reward
    		} else if (targetId > -1 && dl.getController() == 1 && dl.getDeadUnitID() == targetId){
    			reward += 100;
    		}
    	}
    	//find when the last action was issued to this unit and discount the reward
    	boolean foundAction = false;
    	int t = stateView.getTurnNumber();
    	while (!foundAction){
    		for (Map.Entry<Integer, Action> command: historyView.getCommandsIssued(0, t).entrySet()){
    			if(command.getKey() == footmanId){
    				foundAction = true;
    				break;
    			}
    		}
    		t--;
    	}
    	reward += (-0.1 * stateView.getTurnNumber() - t);
        return reward;
    }

    /**
     * Calculate the Q-Value for a given state action pair. The state in this scenario is the current
     * state view and the history of this episode. The action is the attacker and the enemy pair for the
     * SEPIA attack action.
     *
     * This returns the Q-value according to your feature approximation. This is where you will calculate
     * your features and multiply them by your current weights to get the approximate Q-value.
     *
     * @param stateView Current SEPIA state
     * @param historyView Episode history up to this point in the game
     * @param attackerId Your footman. The one doing the attacking.
     * @param defenderId An enemy footman that your footman would be attacking
     * @return The approximate Q-value
     */
    public double calcQValue(State.StateView stateView,
                             History.HistoryView historyView,
                             int attackerId,
                             int defenderId) {
        double[] features = calculateFeatureVector(stateView, historyView, attackerId, defenderId);
        int q =0;
        //simple linear model
        for (int i=0; i< weights.length; i++){
        	q += weights[i] * features[i];
        }
        
    	return q;
    }

    /**
     * Given a state and action calculate your features here. Please include a comment explaining what features
     * you chose and why you chose them.
     *
     * All of your feature functions should evaluate to a double. Collect all of these into an array. You will
     * take a dot product of this array with the weights array to get a Q-value for a given state action.
     *
     * It is a good idea to make the first value in your array a constant. This just helps remove any offset
     * from 0 in the Q-function. The other features are up to you. Many are suggested in the assignment
     * description.
     *
     * @param stateView Current state of the SEPIA game
     * @param historyView History of the game up until this turn
     * @param attackerId Your footman. The one doing the attacking.
     * @param defenderId An enemy footman. The one you are considering attacking.
     * @return The array of feature function outputs.
     */
    /*
     * Features list:
     * 		constant
     * 		distance between units
     * 		amount of target's hp left
     * 		number of other units attacking
     */
    public double[] calculateFeatureVector(State.StateView stateView,
                                           History.HistoryView historyView,
                                           int attackerId,
                                           int defenderId) {
        double[] features = new double[4];
        Unit.UnitView attacker = stateView.getUnit(attackerId);
        Unit.UnitView target = stateView.getUnit(defenderId);
        features[0] = 2;//arbitrary constant for dot product;
        features[1] = Math.sqrt(Math.pow(attacker.getXPosition() - target.getXPosition(),  2) + Math.pow(attacker.getYPosition() - target.getYPosition(),  2));//distance
        features[2] = target.getHP();
        features[3] = 0;//number attacking target
        for (int i: targets){
        	if (i == defenderId){
        		features[3]++;
        	}
        }
        
    	return features;
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * Prints the learning rate data described in the assignment. Do not modify this method.
     *
     * @param averageRewards List of cumulative average rewards from test episodes.
     */
    public void printTestData (List<Double> averageRewards) {
        System.out.println("");
        System.out.println("Games Played      Average Cumulative Reward");
        System.out.println("-------------     -------------------------");
        for (int i = 0; i < averageRewards.size(); i++) {
            String gamesPlayed = Integer.toString(10*i);
            String averageReward = String.format("%.2f", averageRewards.get(i));

            int numSpaces = "-------------     ".length() - gamesPlayed.length();
            StringBuffer spaceBuffer = new StringBuffer(numSpaces);
            for (int j = 0; j < numSpaces; j++) {
                spaceBuffer.append(" ");
            }
            System.out.println(gamesPlayed + spaceBuffer.toString() + averageReward);
        }
        System.out.println("");
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * This function will take your set of weights and save them to a file. Overwriting whatever file is
     * currently there. You will use this when training your agents. You will include th output of this function
     * from your trained agent with your submission.
     *
     * Look in the agent_weights folder for the output.
     *
     * @param weights Array of weights
     */
    public void saveWeights(Double[] weights) {
        File path = new File("agent_weights/weights.txt");
        // create the directories if they do not already exist
        path.getAbsoluteFile().getParentFile().mkdirs();

        try {
            // open a new file writer. Set append to false
            BufferedWriter writer = new BufferedWriter(new FileWriter(path, false));

            for (double weight : weights) {
                writer.write(String.format("%f\n", weight));
            }
            writer.flush();
            writer.close();
        } catch(IOException ex) {
            System.err.println("Failed to write weights to file. Reason: " + ex.getMessage());
        }
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * This function will load the weights stored at agent_weights/weights.txt. The contents of this file
     * can be created using the saveWeights function. You will use this function if the load weights argument
     * of the agent is set to 1.
     *
     * @return The array of weights
     */
    public Double[] loadWeights() {
        File path = new File("agent_weights/weights.txt");
        if (!path.exists()) {
            System.err.println("Failed to load weights. File does not exist");
            return null;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line;
            List<Double> weights = new LinkedList<>();
            while((line = reader.readLine()) != null) {
                weights.add(Double.parseDouble(line));
            }
            reader.close();

            return weights.toArray(new Double[weights.size()]);
        } catch(IOException ex) {
            System.err.println("Failed to load weights from file. Reason: " + ex.getMessage());
        }
        return null;
    }

    @Override
    public void savePlayerData(OutputStream outputStream) {

    }

    @Override
    public void loadPlayerData(InputStream inputStream) {

    }
}
