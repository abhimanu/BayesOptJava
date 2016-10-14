package bayesopt.gp; 

import java.util.*;

import org.jblas.*;
import bayesopt.utility.LoadDataInMatrix;

import org.rosuda.JRI.Rengine;

public class GPRegression{

	private DoubleMatrix getSquaredDistance (DoubleMatrix X1, DoubleMatrix X2){
		DoubleMatrix squared_distance = Geometry.pairwiseSquaredDistances(X1.transpose(), X2.transpose());
		return squared_distance;
	
	}

	private DoubleMatrix getCovarianceKernel (DoubleMatrix X1, DoubleMatrix X2, double log_sigma, double log_ell){	
		// For now we are coding Matern Kernel for p=1 or nu=3/2 
		// Get the Kernel
		int kernelSize1 = X1.getRows();
		int kernelSize2 = X2.getRows();
		double sigma2 = Math.exp(log_sigma * 2);
		double ell = Math.exp(log_ell);
		DoubleMatrix covariance = getSquaredDistance(X1, X2);
		if (kernelSize1 >= kernelSize2){
			for (int i=0; i<kernelSize1; i++){
				for (int j=1; j<=i && j<kernelSize2; j++){	// we start from j=1
					double d = Math.sqrt(3*covariance.get(i,j));		// Matern for p=1 and nu=3/2 
					System.out.println("d/ell:"+(1+d/ell)+"; Math.exp(-d/ell):"+Math.exp(-d/ell));
					covariance.put(i,j, sigma2*(1+d/ell)*Math.exp(-d/ell));
					if (i<kernelSize2){
						covariance.put(j,i, sigma2*(1+d/ell)*Math.exp(-d/ell));
					}
				}	
			}  
		} else {
			for (int i=0; i<kernelSize1; i++){
				for (int j=i; j<kernelSize2; j++){		// we start from j=i 
					double d = Math.sqrt(3*covariance.get(i,j));		// Matern for p=1 and nu=3/2 
					covariance.put(i,j, sigma2*(1+d/ell)*Math.exp(-d/ell));
					if (j<kernelSize1){
						covariance.put(j,i, sigma2*(1+d/ell)*Math.exp(-d/ell));
					}
				}	
			}  
		}
		return covariance;
	}


	public static void main(String args[]){
		// get the data
		// split the data into test and prediction 
		// train the GP; 
		// test on testset
		// Load train data
		DoubleMatrix[] dataTrain = LoadDataInMatrix.load("gpml1Ddemo1st.csv",1,1);
		DoubleMatrix[] dataTest = LoadDataInMatrix.load("gpml1Ddemo1stTestX.csv",1,0);
		GPRegression gpReg = new GPRegression();
		gpReg.getGPpredictions(dataTrain, dataTest);
	}

	public DoubleMatrix getGPpredictions(DoubleMatrix[] dataTrain, DoubleMatrix[] dataTest){
		GPRegression gpr = new GPRegression();
		double meanLin = 0.5, meanConst = 1;
		double log_sigma = Math.log(1), log_ell = Math.log(0.25), log_sigma_n = Math.log(0.1); 
		DoubleMatrix K = gpr.getCovarianceKernel(dataTrain[0], dataTrain[0], log_sigma, log_ell);
		int N = K.getRows();
		DoubleMatrix Ytrain = dataTrain[1];
		DoubleMatrix meanTrain = dataTrain[0].mul(meanLin);
		meanTrain.addi(meanConst);
		double lambda = Math.exp(2*log_sigma_n);
		K.addi(DoubleMatrix.eye(N).muli(lambda));
		DoubleMatrix alpha = Solve.solveSymmetric(K, Ytrain.sub(meanTrain));
		//System.out.println("print the test data");
		//System.out.println(dataTest[0]);
		DoubleMatrix K_star = gpr.getCovarianceKernel(dataTrain[0], dataTest[0], log_sigma, log_ell) ;		// K(X,X*) 
		DoubleMatrix F_bar = K_star.transpose().mmul(alpha);
		DoubleMatrix meanTest = dataTest[0].mul(meanLin);
		meanTest.addi(meanConst);
		F_bar.addi(meanTest);
        System.out.println(Arrays.toString(F_bar.toArray()));
		System.out.println(F_bar.getRows());
		// new R-engine
		Rengine engine = new Rengine (new String [] {"–vanilla"}, false, null);
		engine.assign("yTest", F_bar.toArray());
		engine.assign("xTest", dataTest[0].toArray());
		engine.assign("yTrain", dataTrain[1].toArray());
		engine.assign("xTrain", dataTrain[0].toArray());
		engine.eval("png(file='TestFile.png');");
		engine.eval("library(ggplot2);");
		engine.eval("temp <- plot(xTrain, yTrain, pch='+');");
		engine.eval("temp <- lines(xTest, yTest);");
		engine.eval("print temp;");
		engine.eval("dev.off();");
		engine.end();
		System.out.println("engine::: "+engine);
		//DoubleMatrix L = gpr.getCholesky(K);
		// Load test data
		//DoubleMatrix[] dataTest = LoadDataInMatrix.load("armdatastar.csv",6,1);
		
		//data[0].print();
		//System.out.println(data[0].rows+" "+data[0].columns);
		//System.out.println(data[0].getRow(0));
		//

		return F_bar;
	}

}
