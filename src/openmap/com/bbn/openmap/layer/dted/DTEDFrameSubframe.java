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
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/layer/dted/DTEDFrameSubframe.java,v $
// $RCSfile: DTEDFrameSubframe.java,v $
// $Revision: 1.1.1.1 $
// $Date: 2003/02/14 21:35:48 $
// $Author: dietrick $
// 
// **********************************************************************


package com.bbn.openmap.layer.dted;

import com.bbn.openmap.omGraphics.OMRaster;
import com.bbn.openmap.util.Debug;

public class DTEDFrameSubframe{
    // Types of slope shading
    /** Empty image. */
    public static final int NOSHADING = 0;
    /** Gray scale slope shading, sun from the Northwest. */
    public static final int SLOPESHADING = 1;
    /** Banded contour coloring, contour based on meters. */
    public static final int METERSHADING = 2;
    /** Banded contour coloring, contour based on feet. */
    public static final int FEETSHADING = 3;
    /** Test markings, for the boundary of the subframe. */
    public static final int BOUNDARYSHADING = 4;
    /** Colorized slope shading.  Color basnds are based on elevation,
     * and are accented by shaded indications. */
    public static final int COLOREDSHADING = 5;
    /** DTED LEVEL 0, 1km posts. */
    public static final int LEVEL_0 = 0;
    /** DTED LEVEL 1, 100m posts. */
    public static final int LEVEL_1 = 1;
    /** DTED LEVEL 2, 30m posts. */
    public static final int LEVEL_2 = 2;
    /** Default height between bands in band views. */
    public static final int DEFAULT_BANDHEIGHT = 25;
    /** Default contrast setting for slope shading. */
    public static final int DEFAULT_SLOPE_ADJUST = 3;
    
    public DTEDFrameSubframeInfo si; 
    public OMRaster image;

    public DTEDFrameSubframe(DTEDFrameSubframeInfo info){
	si = info.makeClone();
    }

//      public void finalize(){
//  	Debug.message("gc", "  DTEDFrameSubframe: getting GC'd");
//      }
}
