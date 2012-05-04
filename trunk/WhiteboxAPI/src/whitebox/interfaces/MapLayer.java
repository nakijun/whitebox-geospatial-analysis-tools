/*
 * Copyright (C) 2011 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package whitebox.interfaces;

import whitebox.structures.DimensionBox;
/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public interface MapLayer {
    public String getLayerTitle();
    
    public void setLayerTitle(String title);
    
    public MapLayerType getLayerType();
    
    public DimensionBox getFullExtent();
    
    public boolean isVisible();
    
    public void setVisible(boolean value);
    
    public int getOverlayNumber();
    
    public void setOverlayNumber(int value);

    public enum MapLayerType {
        RASTER, VECTOR, MULTISPECTRAL
    }
}