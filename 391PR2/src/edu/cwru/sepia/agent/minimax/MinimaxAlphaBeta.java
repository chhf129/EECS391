package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MinimaxAlphaBeta extends Agent {

    private final int numPlys;

    public MinimaxAlphaBeta(int playernum, String[] args)
    {
        super(playernum);

        if(args.length < 1)
        {
            System.err.println("You must specify the number of plys");
            System.exit(1);
        }

        numPlys = Integer.parseInt(args[0]);
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate),
                numPlys,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);

        return bestChild.action;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {

    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this.
     *
     * This is the main entry point to the alpha beta search. Refer to the slides, assignment description
     * and book for more information.
     *
     * Try to keep the logic in this function as abstract as possible (i.e. move as much SEPIA specific
     * code into other functions and methods)
     *
     * @param node The action and state to search from
     * @param depth The remaining number of plys under this node
     * @param alpha The current best value for the maximizing node from this node to the root
     * @param beta The current best value for the minimizing node from this node to the root
     * @return The best child of this node with updated values
     */
    /*
     * Entry level method for alpha beta search starts the search on a player turn
     */
    public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta)
    {
    	return alphaBetaSearch(node, depth, alpha, beta, true);
    }
    
    /*
     * If only 1 ply left, it returns the child with the best heuristic value.
     * Otherwise, recursively performs alphaBetaSearch on each child. If pruning is
     * applicable, break and return that child, otherwise pick the best or worst child
     * depending on whose turn it is
     */
    public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta, boolean isMax)
    {
    	List<GameStateChild> children = orderChildrenWithHeuristics(node.state.getChildren(), isMax);
    	if (depth <= 1){
    		return children.get(0);
    	}
    	ArrayList<Double> weights = new ArrayList<Double>();
    	for (GameStateChild c: children){
    		GameStateChild d = alphaBetaSearch(c, depth-1, alpha, beta, !isMax);
    		double utility = d.state.getUtility();
    		weights.add(utility);
    		if (isMax){
    			//check for beta pruning
    			if (utility > beta){
    				return d;
    			} else {
    				alpha = Math.max(alpha, utility);
    			}
    		} else {
    			//check for alpha pruning
    			if (utility < alpha){
    				return d;
    			} else {
    				beta = Math.min(beta, utility);
    			}
    		}
    	}
    	if (isMax){
    		return children.get(weights.indexOf(Collections.max(weights)));
    	} else {
    		return children.get(weights.indexOf(Collections.min(weights)));
    	}
    }

    /**
     * You will implement this.
     *
     * Given a list of children you will order them according to heuristics you make up.
     * See the assignment description for suggestions on heuristics to use when sorting.
     *
     * Use this function inside of your alphaBetaSearch method.
     *
     * Include a good comment about what your heuristics are and why you chose them.
     *
     * @param children
     * @return The list of children sorted by your heuristic.
     */
    /*
     * Places most useful child at front of the list (max value for max nodes, min value for min nodes)
     * The getUtility function of an unfinished game uses heuristics to compute a value
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children, boolean isMax)
    {
    	Comparator<GameStateChild> sorter = new Comparator<GameStateChild>() {
    		public int compare(GameStateChild c1, GameStateChild c2){
    			int i = Double.compare(c1.state.getUtility(), c2.state.getUtility());
    			if (isMax){
    				i *= -1;
    			}
    			return i;
    		}
    	};
    	Collections.sort(children, sorter);
    	//DEBUG PRINTING*******************************
    	/*
    	for (GameStateChild c: children){
    		System.out.println("Child Summary:");
    		System.out.println("\tutility: " + c.state.getUtility());
    		for (Action a: c.action.values()){
    			System.out.println("\t"+a.toString());
    		}
    		System.out.print("\tfootman at: ");
    		for (GameUnit g: c.state.footmen){
    			System.out.print("(" + g.getXPosition() + ", " + g.getYPosition() + ")  ");
    		}
    		System.out.print("\n\tarchers at:" );
    		for (GameUnit g: c.state.archers){
    			System.out.print("(" + g.getXPosition() + ", " + g.getYPosition() + ")  ");
    		}
    		System.out.println("");
    	}
    	//***********************************************/
        return children;
    }
}
