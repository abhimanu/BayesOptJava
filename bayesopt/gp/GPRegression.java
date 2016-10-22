package bayesopt.gp; 

import java.util.*;
import java.util.logging.*;

import org.jblas.*;
import bayesopt.utility.LoadDataInMatrix;

import org.rosuda.JRI.Rengine;



public class GPRegression{

	private final static Logger LOGGER = Logger.getLogger("BayesOptLogger");

	private boolean plotPrediction = false;

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
				for (int j=0; j<=i && j<kernelSize2; j++){	// we start from j=1
					double d = Math.sqrt(3*covariance.get(i,j));		// Matern for p=1 and nu=3/2 
					//LOGGER.info("d/ell:"+(1+d/ell)+"; Math.exp(-d/ell):"+Math.exp(-d/ell));
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
		// Load train data
		DoubleMatrix[] dataTrain = LoadDataInMatrix.load("gpml1Ddemo1st.csv",1,1);
		DoubleMatrix[] dataTest = LoadDataInMatrix.load("gpml1Ddemo1stTestX.csv",1,0);
		// train the GP; 
		// test on testset
		GPRegression gpReg = new GPRegression();
		gpReg.plotPrediction = true;
		gpReg.getGPpredictions(dataTrain, dataTest);
	}

	public void setPlotPrediction(boolean flag){
		plotPrediction = flag;
	}

	public DoubleMatrix getGPpredictions(DoubleMatrix[] dataTrain, DoubleMatrix[] dataTest){
		GPRegression gpr = new GPRegression();
		// TODO: learn the parameters below
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
		//LOGGER.info("print the test data");
		//LOGGER.info(dataTest[0]);
		DoubleMatrix K_star = gpr.getCovarianceKernel(dataTrain[0], dataTest[0], log_sigma, log_ell) ;		// K(X,X*) 
		DoubleMatrix F_bar = K_star.transpose().mmul(alpha);
		DoubleMatrix meanTest = dataTest[0].mul(meanLin);
		meanTest.addi(meanConst);
		F_bar.addi(meanTest);
		DoubleMatrix var_F = new DoubleMatrix(dataTest[0].getRows(), dataTest[0].getColumns());	// This is variance  
		DoubleMatrix alpha_v = Solve.solveSymmetric(K, K_star);
		LOGGER.info("K_star.rows:="+K_star.getRows());
        for (int i=0; i<var_F.getRows(); i++){
			DoubleMatrix k_ss = gpr.getCovarianceKernel(dataTest[0].getRow(i), dataTest[0].getRow(i), log_sigma, log_ell);
			//LOGGER.info("k_ss:="+Arrays.toString(k_ss.toArray()));
			var_F.putRow(i, k_ss.sub(K_star.transpose().getRow(i).mmul(alpha_v.getColumn(i))).add(lambda));
		} 
		var_F = var_F.put(var_F.lt(0),0);
		if (plotPrediction){
			LOGGER.info(Arrays.toString(F_bar.toArray()));
			LOGGER.info("F_bar.rows:="+F_bar.getRows());
			LOGGER.info("var_F:="+Arrays.toString(var_F.toArray()));
			//LOGGER.info("var_F.lt(0):="+Arrays.toString(var_F.lt(0).toArray()));
			//LOGGER.info("var_F:="+Arrays.toString(var_F.put(var_F.lt(0), 0).toArray()));
			// new R-engine
			Rengine engine = new Rengine (new String [] {"–vanilla"}, false, null);
			engine.assign("yTest", F_bar.toArray());
			engine.assign("xTest", dataTest[0].toArray());
			engine.assign("yTrain", dataTrain[1].toArray());
			engine.assign("xTrain", dataTrain[0].toArray());
			engine.assign("varTest", var_F.toArray());
			engine.eval("png(file='TestFile.png');");
			engine.eval("library(ggplot2);");
			engine.eval("temp <- plot(xTrain, yTrain, pch='+');");
			engine.eval("temp <- lines(xTest, yTest);");
			engine.eval("temp <- lines(xTest, yTest+2*sqrt(varTest), lty=3, color='red');");
			engine.eval("temp <- lines(xTest, yTest-2*sqrt(varTest), lty=3, color='red');");
			engine.eval("temp <- polygon(c(xTest, rev(xTest)), c(yTest-2*sqrt(varTest), rev(yTest+2*sqrt(varTest))), col = rgb(0, 0, 255, max = 255, alpha = 125, names = 'blue50'), border = NA);");
			engine.eval("print temp;");
			engine.eval("dev.off();");
			engine.end();
		}

		return F_bar;
	}

}
