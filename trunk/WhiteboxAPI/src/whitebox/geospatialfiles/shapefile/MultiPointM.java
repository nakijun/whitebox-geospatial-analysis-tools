/*
 * Copyright (C) 2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package whitebox.geospatialfiles.shapefile;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import whitebox.structures.BoundingBox;
/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class MultiPointM implements Geometry {
   //private double[] box = new double[4];
    private BoundingBox bb;
    private int numPoints;
    private double[][] points;
    private double mMin;
    private double mMax;
    private double[] mArray;
    private boolean mIncluded = false;
    
    //constructors
    public MultiPointM(byte[] rawData) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(rawData);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();
            bb = new BoundingBox(buf.getDouble(0), buf.getDouble(8), 
                    buf.getDouble(16), buf.getDouble(24));
            numPoints = buf.getInt(32);
            points = new double[numPoints][2];
            for (int i = 0; i < numPoints; i++) {
                points[i][0] = buf.getDouble(36 + i * 16); // x value
                points[i][1] = buf.getDouble(36 + i * 16 + 8); // y value
            }
            
            int pos = 36 + numPoints * 16;
            // m data is optional.
            if (pos < buf.capacity()) {
                mMin = buf.getDouble(pos);
                mMax = buf.getDouble(pos + 8);

                mArray = new double[numPoints];
                pos += 16;
                for (int i = 0; i < numPoints; i++) {
                    mArray[i] = buf.getDouble(pos + i * 8); // m value
                }
                mIncluded = true;
            }
            
            buf.clear();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    
    // properties
    public BoundingBox getBox() {
        return bb;
    }
    
    public double getXMin() {
        return bb.getMinX();
    }
    
    public double getYMin() {
        return bb.getMinY();
    }
    
    public double getXMax() {
        return bb.getMaxX();
    }
    
    public double getYMax() {
        return bb.getMaxY();
    }
    
    public int getNumPoints() {
        return numPoints;
    }

    public double[][] getPoints() {
        return points;
    }

    public double[] getmArray() {
        return mArray;
    }

    public double getmMax() {
        return mMax;
    }

    public double getmMin() {
        return mMin;
    }
    
    public boolean isMDataIncluded() {
        return mIncluded;
    }
    
    @Override
    public int getLength() {
        return 32 + 4 + numPoints * 16 + 16 + numPoints * 8;
    }
    
    @Override
    public ByteBuffer toByteBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(getLength());
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.rewind();
        // put the bounding box data in.
        buf.putDouble(bb.getMinX());
        buf.putDouble(bb.getMinY());
        buf.putDouble(bb.getMaxX());
        buf.putDouble(bb.getMaxY());
        // put the numPoints in.
        buf.putInt(numPoints);
        // put the point data in.
        for (int i = 0; i < numPoints; i++) {
            buf.putDouble(points[i][0]);
            buf.putDouble(points[i][1]);
        }
        // put the min and max m values in
        buf.putDouble(mMin);
        buf.putDouble(mMax);
        // put the m values in
        for (int i = 0; i < numPoints; i++) {
            buf.putDouble(mArray[i]);
        }
        return buf;
    }

    @Override
    public ShapeType getShapeType() {
        return ShapeType.MULTIPOINTM;
    }
    
    @Override
    public boolean isMappable(BoundingBox box, double minSize) {
        if (box.doesIntersect(bb)) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public com.vividsolutions.jts.geom.Geometry[] getJTSGeometries() {
        GeometryFactory factory = new GeometryFactory();
        int a;
        CoordinateArraySequence coordArray;
        com.vividsolutions.jts.geom.Point[] pointArray = new com.vividsolutions.jts.geom.Point[numPoints];
        
        for (a = 0; a < numPoints; a++) {
            coordArray = new CoordinateArraySequence(1);
            coordArray.setOrdinate(0, 0, points[a][0]);
            coordArray.setOrdinate(0, 1, points[a][1]);
            coordArray.setOrdinate(0, 2, mArray[a]);
            pointArray[a] = factory.createPoint(coordArray);
        }
        
        return pointArray;
    }
}
