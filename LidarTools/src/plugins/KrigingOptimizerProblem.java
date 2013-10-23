/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins;

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
//import static jmetal.problems.Water.LOWERLIMIT;
//import static jmetal.problems.Water.UPPERLIMIT;
import jmetal.util.JMException;
import plugins.KrigingPoint;
import static plugins.SensorOptimizerProblem.Image;
import static plugins.SensorOptimizerProblem.LOWERLIMIT;
import static plugins.SensorOptimizerProblem.UPPERLIMIT;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.geospatialfiles.shapefile.attributes.DBFException;
/**
 *
 * @author Ehsan.Roshani
 */
public class KrigingOptimizerProblem extends Problem{
      // defining the lower and upper limits
  List<KrigingPoint> pnts = new ArrayList<>();                                     
                                           //{SV Type, Range, Sill, Nugget}   
  public static final double [] LOWERLIMIT = {0, 1    , 200 , 0 };
  public static final double [] UPPERLIMIT = {2, 70000, 1000, 200};           
  double difMin = 100000000;
   public class Variogram{
        public double Range;
        public double Sill;
        public double Nugget;
        public Kriging.SemiVariogramType Type;
    }
   /**
  * Constructor.
  * Creates a default instance of the Water problem.
  * @param solutionType The solution type must "Real" or "BinaryReal".
  */
  public KrigingOptimizerProblem(String solutionType, int NV, String PointShapeFile) {
    
    Kriging k = new Kriging();
    pnts = k.ReadPointFile(PointShapeFile, "Z");
//    pnts = k.RandomizePoints(pnts, 100);
//      try {
//          k.DrawShapeFile("G:\\Optimized Sensory Network\\PALS\\20120607\\Pnts100.shp", pnts);
//      } catch (DBFException ex) {
//          Logger.getLogger(KrigingOptimizerProblem.class.getName()).log(Level.SEVERE, null, ex);
//      } catch (IOException ex) {
//          Logger.getLogger(KrigingOptimizerProblem.class.getName()).log(Level.SEVERE, null, ex);
//      }
//    
    numberOfVariables_   = 4 ;
    numberOfObjectives_  = 2 ;
    numberOfConstraints_ = 0 ;
    problemName_         = "KrigingOptimizer";
	        
    upperLimit_ = new double[numberOfVariables_];
    lowerLimit_ = new double[numberOfVariables_];
    upperLimit_ = new double[numberOfVariables_];
    lowerLimit_ = new double[numberOfVariables_];
    for (int var = 0; var < numberOfVariables_; var++){
      lowerLimit_[var] = LOWERLIMIT[var];
      upperLimit_[var] = UPPERLIMIT[var];
    } 
	        
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
      
      
//      for (int i = 0; i < solution.getDecisionVariables().length; i=i+2) {
//          double z = Image.getValue(Image.getRowFromYCoordinate(solution.getDecisionVariables()[i+1].getValue()), 
//                  Image.getColumnFromXCoordinate(solution.getDecisionVariables()[i].getValue()));
//          KrigingPoint pp = new KrigingPoint(solution.getDecisionVariables()[i].getValue()
//                  , solution.getDecisionVariables()[i+1].getValue(), z);
//          pnts.add(pp);
//      }
      
      Kriging k = new Kriging();
      
      
      k.Points = pnts;
      k.ConsiderNugget = true;
      //k.LagSize = 2000;
      k.Anisotropic = false;
      Kriging.Variogram var = k.SemiVariogram(Kriging.SemiVariogramType.Spherical,
              solution.getDecisionVariables()[1].getValue(),solution.getDecisionVariables()[2].getValue(),
              solution.getDecisionVariables()[3].getValue(),false);
      
      
      
      k.resolution = 914;
      k.BMinX = 588907;
      k.BMaxX = 600789;
      k.BMinY = 5475107;
      k.BMaxY = 5545485;

      List<KrigingPoint> outPnts ;//= k.calcInterpolationPoints();
      
      
      k.BuildPointTree();
      outPnts = k.InterpolatePoints(var, pnts, 5);
      //k.BuildRaster("G:\\Optimized Sensory Network\\PALS\\20120607\\test"+".dep", outPnts);
      
      double dif = 0;
      for (int i = 0; i < outPnts.size(); i++) {
          int r = ((i+1)-((i+1)%Image.getNumberColumns()))/ Image.getNumberColumns();
          int c = (i%Image.getNumberColumns());
          
          dif += Math.abs(outPnts.get(i).z-Image.getValue(r, c));
      }
      
      
    double [] f = new double[2] ; // 5 functions
    
    
      if (difMin>= dif) {
          difMin = dif;
          k.BuildRaster("G:\\Optimized Sensory Network\\PALS\\20120607\\test"+".dep", outPnts);
          try {
              k.DrawShapeFile("G:\\Optimized Sensory Network\\PALS\\20120607\\test.shp", pnts);
          } catch (DBFException ex) {
              Logger.getLogger(SensorOptimizerProblem.class.getName()).log(Level.SEVERE, null, ex);
          } catch (IOException ex) {
              Logger.getLogger(SensorOptimizerProblem.class.getName()).log(Level.SEVERE, null, ex);
          }
      }
    
    // First function
    f[0] = dif ;
    // Second function
    f[1] = dif ;
             
    solution.setObjective(0,f[0]);    
    solution.setObjective(1,f[1]);
    System.out.println(dif);
  } // evaluate

}
