/*
 * This is the optimization problem to find the best location for the soil moisture sensors
 * to calibrate SMAP
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

//import com.sun.org.apache.xml.internal.resolver.helpers.Debug;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import plugins.Kriging;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.encodings.solutionType.BinaryRealSolutionType;
import jmetal.encodings.solutionType.RealSolutionType;
import static jmetal.problems.Water.LOWERLIMIT;
import static jmetal.problems.Water.UPPERLIMIT;
import jmetal.util.JMException;
import plugins.KrigingPoint;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.geospatialfiles.shapefile.attributes.DBFException;
import whitebox.structures.KdTree;


public class SensorVarianceOptimizerProblem extends Problem{
    // defining the lower and upper limits
  public static  WhiteboxRaster Image;
  public static final double [] LOWERLIMIT = {588907, 5475107};
  public static final double [] UPPERLIMIT = {600789, 5545485};           
  double difMin = 1000000000;
  Kriging k;
   /**
  * Constructor.
  * Creates a default instance of the Water problem.
  * @param solutionType The solution type must "Real" or "BinaryReal".
  */
  public SensorVarianceOptimizerProblem(String solutionType, int NV, WhiteboxRaster img) {
    
      
      Image = img;
    
    //reads all the target data
    double[][] targetData = new double[img.getNumberRows()][img.getNumberColumns()]; 
      for (int r = 0; r < img.getNumberRows(); r++) {
          targetData[r]= img.getRowValues(r);
      }
    /////////////////
      
      
    numberOfVariables_   = NV *2 ;
    numberOfObjectives_  = 2 ;
    numberOfConstraints_ = 1 ;
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
  @Override
  public void evaluate(Solution solution) throws JMException {  
      k = null;
      List<KrigingPoint> pnts = new ArrayList<>();
      
      for (int i = 0; i < solution.getDecisionVariables().length; i=i+2) {
          double z = Image.getValue(Image.getRowFromYCoordinate(solution.getDecisionVariables()[i+1].getValue()), 
                  Image.getColumnFromXCoordinate(solution.getDecisionVariables()[i].getValue()));
          KrigingPoint pp = new KrigingPoint(solution.getDecisionVariables()[i].getValue()
                  , solution.getDecisionVariables()[i+1].getValue(), z);
          pnts.add(pp);
      }
      
      k = new Kriging();
      
      
      k.Points = pnts;
      k.ConsiderNugget = false;
      k.LagSize = 2000;
      k.Anisotropic = false;
      Kriging.Variogram var = k.SemiVariogram(Kriging.SemiVariogramType.Spherical, 1, 35,false,true);
      
     
      
      k.resolution = 914;
      k.BMinX = 588450 + k.resolution/2;
      k.BMaxX = 601246 - k.resolution/2;
      k.BMinY = 5474650 + k.resolution/2;
      k.BMaxY = 5545942 - k.resolution/2;

      List<KrigingPoint> outPnts = k.calcInterpolationPoints();
      outPnts = k.InterpolatePoints(var, outPnts, 5);
      //k.BuildRaster("G:\\Optimized Sensory Network\\PALS\\20120607\\test"+".dep", outPnts);
      
//      double dif = 0;
//      for (int i = 0; i < outPnts.size(); i++) {
//          int r = ((i+1)-((i+1)%Image.getNumberColumns()))/ Image.getNumberColumns();
//          int c = (i%Image.getNumberColumns());
//          
//          dif += Math.abs(outPnts.get(i).z-Image.getValue(r, c));
//      }
//      
      
      double sDif = 0;
      double eDif = 0;
      for (int i = 0; i < outPnts.size(); i++) {
          sDif += Math.abs(outPnts.get(i).v);
          eDif += Math.abs(Image.grid[i]-outPnts.get(i).z);
      }
      
      //sDif = Math.abs(sDif);
    double [] f = new double[2] ; // 5 functions
    
    evaluateConstraints(solution);
      if (difMin>= sDif+eDif & solution.getOverallConstraintViolation() == 0 ) {
          difMin = sDif+eDif;
//          for (int i = 0; i < outPnts.size(); i++) {
//              System.out.print(i);
//                      
//          }
          k.BuildRaster("G:\\Optimized Sensory Network\\PALS\\20120607\\test"+".dep", outPnts,false);
          k.BuildRaster("G:\\Optimized Sensory Network\\PALS\\20120607\\testVar"+".dep", outPnts,true);
//          k.BuildRaster("G:\\Optimized Sensory Network\\PALS\\20120629\\test"+".dep", outPnts,false);
//          k.BuildRaster("G:\\Optimized Sensory Network\\PALS\\20120629\\testVar"+".dep", outPnts,true);

          //k.BuildRaster("G:\\Optimized Sensory Network\\PALS\\20120719\\test"+".dep", outPnts,false);
          //k.BuildRaster("G:\\Optimized Sensory Network\\PALS\\20120719\\testVar"+".dep", outPnts,true);

          
          try {
              k.DrawShapeFile("G:\\Optimized Sensory Network\\PALS\\20120607\\test.shp", pnts);
              //k.DrawShapeFile("G:\\Optimized Sensory Network\\PALS\\20120719\\test.shp", pnts);
          } catch (DBFException ex) {
              Logger.getLogger(SensorVarianceOptimizerProblem.class.getName()).log(Level.SEVERE, null, ex);
          } catch (IOException ex) {
              Logger.getLogger(SensorVarianceOptimizerProblem.class.getName()).log(Level.SEVERE, null, ex);
          }
      }
    
    // First function
    f[0] = sDif ;
    // Second function
    f[1] = eDif ;
             
    solution.setObjective(0,f[0]);    
    solution.setObjective(1,f[1]);
    System.out.println(sDif+" : "+ eDif + " : " + difMin);
  } // evaluate

  /** 
   * NOT USED Evaluates the constraint overhead of a solution 
   * @param solution The solution
   * @throws JMException 
   */  
  public void evaluateConstraints(Solution solution) throws JMException {
      
      
      
    double [] constraint = new double[1]; // 7 constraints
    
      for (int i = 0; i < k.Pairs.size(); i++) {
          if (k.Pairs.get(i).Distance<= 2 * k.resolution) {
              constraint[0]+=(2 * k.resolution - k.Pairs.get(i).Distance);
          }
      }
      List<KdTree.Entry<Double>> results;
      double[] pnt = new double[2];
      double dist = 0;
      for (int i = 0; i < k.Points.size(); i++) {
          pnt[1] = k.Points.get(i).x;
          pnt[0] = k.Points.get(i).y;
          
          results = k.pointsTree.nearestNeighbor(pnt, 2, false);
          if (results.get(0).distance != 0) {
              dist = Math.sqrt(results.get(0).distance);
          }
          else
          {
              dist = Math.sqrt(results.get(1).distance);
          }
          
          //dist = Math.sqrt(results.get(1).distance);
          if (dist > 5 * k.resolution) {
              constraint[0]+= dist - 5 * k.resolution;
          }
 
      }
 
    solution.setOverallConstraintViolation(-constraint[0]);    
    solution.setNumberOfViolatedConstraint(1);  
    //solution.setFitness(constraint[0]);
    System.out.println(-constraint[0]);
  } // evaluateConstraints   
    
}
