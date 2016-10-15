package bayesopt.dl; 

// JEP imports
import jep.Jep;
import jep.JepException;

import java.util.*;
import javafx.collections.transformation.SortedList;

import org.jblas.*;

import org.rosuda.JRI.Rengine;

import bayesopt.utility.LoadDataInMatrix;
import bayesopt.gp.*; 

    
// For now this does mnist evaluation task
public class BayesOptDeepLearning {

	class Tuple<Integer, Double> { 
		final int x; 
		final double y; 
		public Tuple(int x, double y) { 
			this.x = x; 
			this.y = y; 
		} 
	} 


	double returnedObjective;
	int max_neurons = 500;
	int min_neurons = 50;
	int gridDiff = 20;
	int iterCounter = 0;
	
	List<Double> gridPoints;
	SortedMap<Integer, Double> sortedIterObjective;
	SortedMap<Integer, Integer> sortedIterNeuron;
	DoubleMatrix [] trainDataGP = null;
	Jep jep; 


	public BayesOptDeepLearning () throws JepException{
		
		jep = new Jep();
		gridPoints = new LinkedList<Double>();

		int currPoint = min_neurons;
		while (currPoint<=max_neurons){
			gridPoints.add((double)currPoint);
			currPoint += gridDiff;
		}
		
		sortedIterObjective = new TreeMap<Integer, Double>();
		sortedIterNeuron = new TreeMap<Integer, Integer>();
	

		// Insert the first and last point of the grid		
		getAndSetNextPrediction(min_neurons);
		getAndSetNextPrediction(max_neurons);
	}

	void getAndSetNextPrediction(int n_neurons) throws JepException {
		if (trainDataGP==null){
			trainDataGP = new DoubleMatrix[] {DoubleMatrix.zeros(1), DoubleMatrix.zeros(1)};
		}
		 
		double objVal =  getObjectiveForDataPoint(n_neurons);
		if (trainDataGP==null){
			trainDataGP = new DoubleMatrix[] {new DoubleMatrix(new double[] {iterCounter}), new DoubleMatrix(new double[] {objVal})};
		} else{
        	trainDataGP[0] = DoubleMatrix.concatVertically(trainDataGP[0], new DoubleMatrix(new double[] {iterCounter}));
        	trainDataGP[1] = DoubleMatrix.concatVertically(trainDataGP[1], new DoubleMatrix(new double[] {objVal}));
		}
		gridPoints.remove(Integer.valueOf(n_neurons));
		sortedIterObjective.put(iterCounter, objVal);
		sortedIterNeuron.put(iterCounter, n_neurons);
		System.out.println("n_neurons, objVal: "+n_neurons+","+objVal);
		iterCounter++;
	}
	
	public void setObjective(float newValue){
		returnedObjective = (double) newValue;
	}


	double getObjectiveForDataPoint(int n_neurons) throws JepException{
		jep.runScript("evaluate_objectives.py");
		jep.invoke("main", n_neurons, this);

		double currObj = this.returnedObjective;
		return currObj;

	}


	public static void main(String args[]) throws JepException{
		BayesOptDeepLearning bayesOpt = new BayesOptDeepLearning();
		GPRegression gpr = new GPRegression();
		DoubleMatrix [] testDataGP;

		gpr.setPlotPrediction(false);

		for (int i=0; i<15; i++){
			testDataGP = new DoubleMatrix [] {new DoubleMatrix(bayesOpt.gridPoints)};
			System.out.println("testDataGP===="+Arrays.toString(testDataGP[0].toArray()));
			System.out.println(testDataGP[0].getRows()+" "+testDataGP[0].getColumns());
        	DoubleMatrix F_bar = gpr.getGPpredictions(bayesOpt.trainDataGP, testDataGP);
			System.out.println("F_bar===="+Arrays.toString(F_bar.toArray()));
			int minPredIndex = F_bar.argmin();
			int minPredNeuron = (int) testDataGP[0].get(i);
		   	System.out.println("minPredIndex: "+minPredIndex+", minPredNeuron: "+
minPredNeuron); 
			bayesOpt.getAndSetNextPrediction(minPredNeuron);
		}
		System.out.println("Iteration vs Objective==="+Arrays.asList(bayesOpt.sortedIterObjective));
	}

}
