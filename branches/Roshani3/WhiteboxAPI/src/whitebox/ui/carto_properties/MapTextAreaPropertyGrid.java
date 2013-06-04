/*
 * Copyright (C) 2013 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package whitebox.ui.carto_properties;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import whitebox.cartographic.MapTextArea;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 *
 * @author johnlindsay
 */
public class MapTextAreaPropertyGrid extends JPanel implements PropertyChangeListener {
    
    private MapTextArea mapTextArea;
    private int rightMargin = 10;
    private int leftMargin = 10;
    private Color backColour = new Color(225, 245, 255);
    private WhiteboxPluginHost host = null;
    
    private StringProperty titleString;
    private BooleanProperty titleVisible;
    private ColourProperty fontColourBox;
    private BooleanProperty backgroundVisible;
    private ColourProperty backgroundColourBox;
    private BooleanProperty borderVisible;
    private ColourProperty borderColour;
    private NumericProperty marginSize;
    private NumericProperty interlineSpacing;
    private FontProperty fontProperty;
    
    public MapTextAreaPropertyGrid() {
        createUI();
    }
    
    public MapTextAreaPropertyGrid(MapTextArea mapTextArea, WhiteboxPluginHost host) {
        this.mapTextArea = mapTextArea;
        this.host = host;
        createUI();
    }

    public MapTextArea getMapTextArea() {
        return mapTextArea;
    }

    public void setMapTitle(MapTextArea mapTextArea) {
        this.mapTextArea = mapTextArea;
    }

    public int getLeftMargin() {
        return leftMargin;
    }

    public void setLeftMargin(int leftMargin) {
        this.leftMargin = leftMargin;
    }

    public int getRightMargin() {
        return rightMargin;
    }

    public void setRightMargin(int rightMargin) {
        this.rightMargin = rightMargin;
    }

    public Color getBackColour() {
        return backColour;
    }

    public void setBackColour(Color backColour) {
        this.backColour = backColour;
    }

    public WhiteboxPluginHost getHost() {
        return host;
    }

    public void setHost(WhiteboxPluginHost host) {
        this.host = host;
    }
    
    public final void createUI() {
        try {
            
            this.setBackground(Color.WHITE);
            
            Box mainBox = Box.createVerticalBox();
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            int preferredWidth = 470;
            this.add(mainBox);
            
//            Font labelFont = mapTextArea.getLabelFont();
            
            titleString = new StringProperty("Label text", 
                    mapTextArea.getLabel());
            titleString.setLeftMargin(leftMargin);
            titleString.setRightMargin(rightMargin);
            titleString.setBackColour(Color.WHITE);
            titleString.setTextboxWidth(8);
            titleString.setPreferredWidth(preferredWidth);
            titleString.addPropertyChangeListener("value", this);
            titleString.setShowTextArea(true);
            titleString.revalidate();
            mainBox.add(titleString);
            
            titleVisible = new BooleanProperty("Is the text area visible?", 
                    mapTextArea.isVisible());
            titleVisible.setLeftMargin(leftMargin);
            titleVisible.setRightMargin(rightMargin);
            titleVisible.setBackColour(backColour);
            titleVisible.setPreferredWidth(preferredWidth);
            titleVisible.addPropertyChangeListener("value", this);
            titleVisible.revalidate();
            mainBox.add(titleVisible);
            
            fontProperty = new FontProperty("Font:", mapTextArea.getLabelFont());
            fontProperty.setLeftMargin(leftMargin);
            fontProperty.setRightMargin(rightMargin);
            fontProperty.setBackColour(Color.WHITE);
            fontProperty.setTextboxWidth(15);
            fontProperty.setPreferredWidth(preferredWidth);
            fontProperty.addPropertyChangeListener("value", this);
            fontProperty.revalidate();
            mainBox.add(fontProperty);
            
            fontColourBox = new ColourProperty("Font colour", 
                    mapTextArea.getFontColour());
            fontColourBox.setLeftMargin(leftMargin);
            fontColourBox.setRightMargin(rightMargin);
            fontColourBox.setBackColour(backColour);
            fontColourBox.setPreferredWidth(preferredWidth);
            fontColourBox.revalidate();
            fontColourBox.addPropertyChangeListener("value", this);
            mainBox.add(fontColourBox);
            
            interlineSpacing = new NumericProperty("Interline spacing:", 
                    String.valueOf(mapTextArea.getInterlineSpace()));
            interlineSpacing.setLeftMargin(leftMargin);
            interlineSpacing.setRightMargin(rightMargin);
            interlineSpacing.setBackColour(Color.WHITE);
            interlineSpacing.setTextboxWidth(8);
            interlineSpacing.setPreferredWidth(preferredWidth);
            interlineSpacing.setParseIntegersOnly(false);
            interlineSpacing.setMinValue(0);
            interlineSpacing.setMaxValue(10);
            interlineSpacing.addPropertyChangeListener("value", this);
            interlineSpacing.revalidate();
            mainBox.add(interlineSpacing);
            
            backgroundVisible = new BooleanProperty("Is the background visible?", 
                    mapTextArea.isBackgroundVisible());
            backgroundVisible.setLeftMargin(leftMargin);
            backgroundVisible.setRightMargin(rightMargin);
            backgroundVisible.setBackColour(backColour);
            backgroundVisible.setPreferredWidth(preferredWidth);
            backgroundVisible.revalidate();
            backgroundVisible.addPropertyChangeListener("value", this);
            mainBox.add(backgroundVisible);
            
            backgroundColourBox = new ColourProperty("Background colour", 
                    mapTextArea.getBackColour());
            backgroundColourBox.setLeftMargin(leftMargin);
            backgroundColourBox.setRightMargin(rightMargin);
            backgroundColourBox.setBackColour(Color.WHITE);
            backgroundColourBox.setPreferredWidth(preferredWidth);
            backgroundColourBox.revalidate();
            backgroundColourBox.addPropertyChangeListener("value", this);
            mainBox.add(backgroundColourBox);
            
            borderVisible = new BooleanProperty("Is the border visible?", 
                    mapTextArea.isBorderVisible());
            borderVisible.setLeftMargin(leftMargin);
            borderVisible.setRightMargin(rightMargin);
            borderVisible.setBackColour(backColour);
            borderVisible.setPreferredWidth(preferredWidth);
            borderVisible.revalidate();
            borderVisible.addPropertyChangeListener("value", this);
            mainBox.add(borderVisible);
            
            borderColour = new ColourProperty("Border colour", 
                    mapTextArea.getBorderColour());
            borderColour.setLeftMargin(leftMargin);
            borderColour.setRightMargin(rightMargin);
            borderColour.setBackColour(Color.WHITE);
            borderColour.setPreferredWidth(preferredWidth);
            borderColour.revalidate();
            borderColour.addPropertyChangeListener("value", this);
            mainBox.add(borderColour);
            
            marginSize = new NumericProperty("Margin size (points)", 
                    String.valueOf(mapTextArea.getMargin()));
            marginSize.setLeftMargin(leftMargin);
            marginSize.setRightMargin(rightMargin);
            marginSize.setBackColour(backColour);
            marginSize.setTextboxWidth(5);
            marginSize.setParseIntegersOnly(true);
            marginSize.addPropertyChangeListener("value", this);
            marginSize.setPreferredWidth(preferredWidth);
            marginSize.revalidate();
            mainBox.add(marginSize);
            
            super.revalidate();
        } catch (Exception e) {
            //host.showFeedback(e.getMessage());
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Object source = evt.getSource();
        Boolean didSomething = false;
        if (!evt.getPropertyName().equals("value")) {
            return;
        }
        if (source == titleString) {
            mapTextArea.setLabel((String) evt.getNewValue());
            didSomething = true;
        } else if (source == titleVisible) {
            mapTextArea.setVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == fontColourBox) {
            mapTextArea.setFontColour(fontColourBox.getValue());
            didSomething = true;
        } else if (source == backgroundColourBox) {
            mapTextArea.setBackColour(backgroundColourBox.getValue());
            didSomething = true;
        } else if (source == backgroundVisible) {
            mapTextArea.setBackgroundVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderVisible) {
            mapTextArea.setBorderVisible((Boolean) evt.getNewValue());
            didSomething = true;
        } else if (source == borderColour) {
            mapTextArea.setBorderColour((Color) evt.getNewValue());
            didSomething = true;
        } else if (source == marginSize) {
            mapTextArea.setMargin(Integer.parseInt((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == interlineSpacing) {
            mapTextArea.setInterlineSpace(Float.parseFloat((String) evt.getNewValue()));
            didSomething = true;
        } else if (source == fontProperty) {
            mapTextArea.setLabelFont((Font)evt.getNewValue());
            didSomething = true;
        }

        if (didSomething && host != null) {
            host.refreshMap(false);
        }
    }
}
