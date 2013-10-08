/*
 * This is the optimization problem to find the best location for the soil moisture sensors
 * to calibrate SMAP
 */
package plugins;

/**
 *
 * @author Dr. Ehsan Roshani
 */
import plugins.Kriging;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.encodings.solutionType.BinaryRealSolutionType;
import jmetal.encodings.solutionType.RealSolutionType;
import static jmetal.problems.Water.LOWERLIMIT;
import static jmetal.problems.Water.UPPERLIMIT;
import jmetal.util.JMException;

public class SensorOptimizerProblem extends Problem{
    // defining the lower and upper limits
  public static final double [] LOWERLIMIT = {587993, 5474193};
  public static final double [] UPPERLIMIT = {601703, 5546399};           
    public void test(){
        Kriging k = new Kriging();
    }
   /**
  * Constructor.
  * Creates a default instance of the Water problem.
  * @param solutionType The solution type must "Real" or "BinaryReal".
  */
  public SensorOptimizerProblem(String solutionType, int NV) {
    numberOfVariables_   = NV *2 ;
    numberOfObjectives_  = 2 ;
    numberOfConstraints_ = 0 ;
    problemName_         = "SensorOptimizer";
	        
    upperLimit_ = new double[numberOfVariables_];
    lowerLimit_ = new double[numberOfVariables_];
    upperLimit_ = new double[numberOfVariables_];
    lowerLimit_ = new double[numberOfVariables_];
    for (int var = 0; var < numberOfVariables_; var=var+2){
      lowerLimit_[var] = LOWERLIMIT[0];
      upperLimit_[var] = UPPERLIMIT[0];

      lowerLimit_[var+1] = LOWERLIMIT[1];
      upperLimit_[var+1] = UPPERLIMIT[1];

    } // for
	        
    if (solutionType.compareTo("BinaryReal") == 0)
      solutionType_ = new BinaryRealSolutionType(this) ;
    else if (solutionType.compareTo("Real") == 0)
    	solutionType_ = new RealSolutionType(this) ;
    else {
    	System.out.println("Error: solution type " + solutionType + " invalid") ;
    	System.exit(-1) ;
    }  
 } // Roshani
	         /**
   * Evaluates a solution
   * @param solution The solution to evaluate
   * @throws JMException 
   */
  public void evaluate(Solution solution) throws JMException {         
    double [] x = new double[2] ; // 3 decision variables
    double [] f = new double[2] ; // 5 functions
    x[0] = solution.getDecisionVariables()[0].getValue();
    x[1] = solution.getDecisionVariables()[1].getValue();

    
    // First function
    f[0] = 106780.37 * (x[0] + x[1]) + 61704.67 ;
    // Second function
    f[1] = 3000 * x[0] ;
             
    solution.setObjective(0,f[0]);    
    solution.setObjective(1,f[1]);
  } // evaluate

  /** 
   * Evaluates the constraint overhead of a solution 
   * @param solution The solution
   * @throws JMException 
   */  
  public void evaluateConstraints(Solution solution) throws JMException {
    double [] constraint = new double[1]; // 7 constraints
    double [] x          = new double[2]; // 3 objectives
        
    x[0] = solution.getDecisionVariables()[0].getValue();
    x[1] = solution.getDecisionVariables()[1].getValue();
 
    constraint[0] = 1 - (0.00139/(x[0]*x[1])+4.94*x[1]-0.08)             ;
    
    double total = 0.0;
    int number = 0;
    for (int i = 0; i < numberOfConstraints_; i++) {
      if (constraint[i]<0.0){
        total+=constraint[i];
        number++;
      } // int
    } // for
        
    solution.setOverallConstraintViolation(total);    
    solution.setNumberOfViolatedConstraint(number);        
  } // evaluateConstraints   
    
}
