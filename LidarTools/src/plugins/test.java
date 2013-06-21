/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


package plugins;
import java.util.List;
import whitebox.structures.KdTree;
/**
 *
 * @author eroshani
 */
public class test {
    
    public void r(){
        int numberOfTrees = 5;
        KdTree<Double> Tree = new KdTree.SqrEuclid<Double>(2, new Integer((int)numberOfTrees));
        double[] entry;
        entry = new double[]{1.0,1.0};
        Tree.addPoint(entry, 0.0);
        entry = new double[]{1.5,5.0};
        Tree.addPoint(entry, 0.0);
        entry = new double[]{3.0,2.0};
        Tree.addPoint(entry, 0.0);
        entry = new double[]{10.0,0.0};
        Tree.addPoint(entry, 0.0);
        entry = new double[]{10.0,7.0};
        Tree.addPoint(entry, 0.0);
        
        
        double[] p = new double[2];
        p[0]=4.0;p[1]=4.0;
        List<KdTree.Entry<Double>> results;
        //results=Tree.nearestNeighbor(p, 2, true);
        results = Tree.neighborsWithinRange(p, 3);
        
        int i = 0;
    }
    
    
public static void main(String[] args) {
    test t = new test();
    t.r();

}
}