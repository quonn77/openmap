// **********************************************************************
// 
// <copyright>
// 
//  BBN Technologies, a Verizon Company
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
// 
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
// 
// </copyright>
// **********************************************************************
// 
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/plugin/graphicLoader/Attic/GraphicLoader.java,v $
// $RCSfile: GraphicLoader.java,v $
// $Revision: 1.1.1.1 $
// $Date: 2003/02/14 21:35:49 $
// $Author: dietrick $
// 
// **********************************************************************


package com.bbn.openmap.plugin.graphicLoader;

import java.awt.*;

import com.bbn.openmap.omGraphics.OMGraphicHandler;
import com.bbn.openmap.proj.Projection;

/**
 * The interface that describes an object that supplies updates to
 * OMGraphics presented by the GraphicLoaderPlugIn.
 */
public interface GraphicLoader {
    /**
     * The method that calls the GUI that controls the GraphicLoader.
     */
    public Component getGUI();
    /**
     * Let the GraphicLoader know what the projection is.
     */
    public void setProjection(Projection p);

    /**
     * Set the OMGraphicHandler that will receive OMGraphic updates
     * from the GraphicLoader.
     */
    public void setReceiver(OMGraphicHandler r);

    /**
     * Get the OMGraphicHandler that will receive OMGraphic updates
     * from the GraphicLoader.
     */
    public OMGraphicHandler getReceiver();

    /**
     * Get a pretty name for GUI representation that lets folks know
     * what the GraphicLoader does.
     */
    public String getName();
}
