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
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/layer/shape/NumAndBox.java,v $
// $RCSfile: NumAndBox.java,v $
// $Revision: 1.1.1.1 $
// $Date: 2003/02/14 21:35:48 $
// $Author: dietrick $
// 
// **********************************************************************


package com.bbn.openmap.layer.shape;

/**
 * A class used as a application object by OMGeometry objects, letting
 * them keep track of their record number (noted by their place in the
 * Shape file) and bounding box.  
 */
public class NumAndBox {
    protected int recNum;
    protected ESRIBoundingBox bbox;
    
    public NumAndBox(int recordNumber, ESRIBoundingBox box) {
	recNum = recordNumber;
	bbox = box;
    }
    
    public void setRecNum(int number) {
	recNum = number;
    }
    
    public int getRecNum() {
	return recNum;
    }
    
    public void setBoundingBox(ESRIBoundingBox box) {
	bbox = box;
    }
    
    public ESRIBoundingBox getBoundingBox() {
	return bbox;
    }
}
