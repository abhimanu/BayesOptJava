/* BayesOpt Project.
 *
 */

package bayesopt.utility;

import org.jblas.*;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.*;


/**
 * Simple Class to load the example data from files straight into Matrices.
 */
public class LoadDataInMatrix {

	private final static Logger LOGGER = Logger.getLogger("BayesOptLogger");

    /**
     * Load data
     * @param filename  data file
     * @param sizeofInputs
     * @param sizeofOutputs
     * @return  [X, Y]
     */
    public static DoubleMatrix[] load(String filename,int sizeofInputs, int sizeofOutputs){

        ArrayList<double[]> inputsList = new ArrayList<double[]>();
        ArrayList<double[]> outputsList = new ArrayList<double[]>();
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(filename));
        } catch (FileNotFoundException e) {
            LOGGER.info("error: file " + filename + " not found.");
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        boolean eof;
        int datasize = 0;

        do {
            eof = true;

            String readLine = null;

            try {
                readLine = br.readLine();
            } catch (IOException e) {
                LOGGER.info("error: reading from " + filename + ".");
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            if (readLine != null && !readLine.equals("")) {
                eof = false;

                try {
                    double[] in = new double[sizeofInputs];
                    double[] out = new double[sizeofOutputs];
                    StringTokenizer st = new StringTokenizer(readLine, ", ");

                    // parse inputs
                    int index = 0;
                    int currentVariable = 0;
                    for (int i = 0; i < sizeofInputs; i++) {
                        in[index] = Double.parseDouble(st.nextToken());
                        index++;
                        currentVariable++;
                    }

                    // parse outputs
                    index = 0;
                    for (int i = 0; i < sizeofOutputs; i++) {
                        out[index] = Double.parseDouble(st.nextToken());
                        index++;
                        currentVariable++;
                    }

                    inputsList.add(in);
                    outputsList.add(out);
                }
                catch (Exception e) {
                    LOGGER.info(e + "\nerror: this line in the logfile does not agree with the configuration provided... it will be skipped");
                    datasize--;
                }
            }
            datasize++;
        } while (!eof);

        double[][] inmat = new double[inputsList.size()][sizeofInputs];
        double[][] outmat = new double[inputsList.size()][sizeofOutputs];
        inputsList.toArray(inmat);
        outputsList.toArray(outmat);

        return new DoubleMatrix[]{new DoubleMatrix(inmat), new DoubleMatrix(outmat)};
    }

    /**
     * Simple example of how to use this class.
     * @param args
     */
    public static void main(String[] args) {


       
     }


}
