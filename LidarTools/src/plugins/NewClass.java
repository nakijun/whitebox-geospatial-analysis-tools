/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins;

/**
 *
 * @author Ehsan.Roshani
 */
public class NewClass {
    class point{
        public double x;
        public double y;
    }
        public static void main(String[] args) {
            point[] c1 = new point[3];
            point[] c2 = new point[3];
            
            point p = new point();
            p.x = 8;
            p.y = 10;
            c1[1]=p;
            c2[1]=-4;
            c1[1]=c2[1];
            c2[1]=5;
        }

}
