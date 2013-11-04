/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins;

/**
 *
 * @author Ehsan.Roshani
 */
public class KrigingPoint {
    public double x;
    public double y;
    public double z;
    public double v;    //Kriging Variance Eq 12.20 P 290
            
    
    
    
    public KrigingPoint(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
