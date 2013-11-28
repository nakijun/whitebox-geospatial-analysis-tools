/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins;

/**
 *
 * @author Ehsan Roshani, Ph.D.
    Department of Geography 
    University of Guelph
    Guelph, Ont. N1G 2W1 CANADA
    Phone: (519) 824-4120 x53527
    Email: eroshani@uoguelph.ca
 */

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.operators.crossover.CrossoverFactory;
import jmetal.operators.mutation.MutationFactory;
import jmetal.operators.selection.SelectionFactory;
import jmetal.problems.ProblemFactory;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.Configuration;
import jmetal.util.JMException;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import jmetal.metaheuristics.nsgaII.NSGAII;
import static jmetal.metaheuristics.nsgaII.NSGAII_main.fileHandler_;
import static jmetal.metaheuristics.nsgaII.NSGAII_main.logger_;
import jmetal.problems.Roshani;
import whitebox.geospatialfiles.WhiteboxRaster;



public class SensorOptimizer {
  
    public WhiteboxRaster OpenTargetRaster (String inputRaster){
        WhiteboxRaster image;
        image = new WhiteboxRaster(inputRaster, "r");
        
        int rows = image.getNumberRows();
        int cols = image.getNumberColumns();
        double rowSize = image.getCellSizeY();
        double colSize = image.getCellSizeX();
        double[][] value;
        value = new double[rows][cols];
        for (int r = 0; r < rows; r++) {
            value[r]=image.getRowValues(r);
        }
        return image;
    }

    
     public static void main(String [] args) throws 
                                  JMException, 
                                  SecurityException, 
                                  IOException, 
                                  ClassNotFoundException {
         
     SensorOptimizer so = new SensorOptimizer();
     WhiteboxRaster img = so.OpenTargetRaster("G:\\Optimized Sensory Network\\PALS\\20120607\\20120607flt.dep");
    Problem   problem   ; // The problem to solve
    Algorithm algorithm ; // The algorithm to use
    Operator  crossover ; // Crossover operator
    Operator  mutation  ; // Mutation operator
    Operator  selection ; // Selection operator
    
    HashMap  parameters ; // Operator parameters
    
    QualityIndicator indicators ; // Object to get quality indicators

    indicators = null ;
    if (args.length == 1) {
      Object [] params = {"Real"};
      problem = (new ProblemFactory()).getProblem(args[0],params);
    } // if
    else if (args.length == 2) {
      Object [] params = {"Real"};
      problem = (new ProblemFactory()).getProblem(args[0],params);
      indicators = new QualityIndicator(problem, args[1]) ;
    } // if
    else { // Default problem
        problem = new SensorOptimizerProblem("Real", 60, img);
    } // else
    
    algorithm = new NSGAII(problem);
    //algorithm = new ssNSGAII(problem);

    // Algorithm parameters
    algorithm.setInputParameter("populationSize",200);
    algorithm.setInputParameter("maxEvaluations",400000);

    // Mutation and Crossover for Real codification 
    parameters = new HashMap() ;
    parameters.put("probability", 0.9) ;
    parameters.put("distributionIndex", 20.0) ;
    crossover = CrossoverFactory.getCrossoverOperator("SBXCrossover", parameters);                   

    parameters = new HashMap() ;
    parameters.put("probability", 1.0/problem.getNumberOfVariables()) ;
    parameters.put("distributionIndex", 20.0) ;
    mutation = MutationFactory.getMutationOperator("PolynomialMutation", parameters);                    

    // Selection Operator 
    parameters = null ;
    selection = SelectionFactory.getSelectionOperator("BinaryTournament2", parameters) ;                           

    // Add the operators to the algorithm
    algorithm.addOperator("crossover",crossover);
    algorithm.addOperator("mutation",mutation);
    algorithm.addOperator("selection",selection);

    // Add the indicator object to the algorithm
    algorithm.setInputParameter("indicators", indicators) ;
    
    // Execute the Algorithm
    SolutionSet population = algorithm.execute();
    
  } //main
}
