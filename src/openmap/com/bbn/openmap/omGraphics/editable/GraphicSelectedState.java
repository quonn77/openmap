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
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/omGraphics/editable/GraphicSelectedState.java,v $
// $RCSfile: GraphicSelectedState.java,v $
// $Revision: 1.1.1.1 $
// $Date: 2003/02/14 21:35:49 $
// $Author: dietrick $
// 
// **********************************************************************


package com.bbn.openmap.omGraphics.editable;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import com.bbn.openmap.omGraphics.*;
import com.bbn.openmap.layer.util.stateMachine.*;
import com.bbn.openmap.util.Debug;

public class GraphicSelectedState extends State implements EOMGSelectedState {

    protected EditableOMGraphic graphic;

    public GraphicSelectedState(EditableOMGraphic eomg) {
	graphic = eomg;
    }

    /**
     * In this state, we need to change states only if the graphic, or
     * anyplace off the graphic is pressed down on.  If the end points
     * are clicked on, then we do nothing except set the moving point
     * and go to edit mode.
     */
    public boolean mousePressed(MouseEvent e) {
	Debug.message("eomg", "GraphicStateMachine|selected state|mousePressed");

	GrabPoint mp = graphic.getMovingPoint(e);

	// If the graphic itself was clicked on, then just go to selected
	// mode.
	if (mp == null) {
	    if (graphic.getGraphic().distance(e.getX(), e.getY()) > 2) {
		// if the graphic isn't picked, then unselect the graphic.
		graphic.getStateMachine().setUnselected();
//  		graphic.setMovingPoint(new GrabPoint(e.getX(), e.getY()));
//  		graphic.fireEvent(EOMGCursors.DEFAULT, "");
//  		graphic.setMovingPoint(null);
//  		graphic.redraw(e, true);
	    } else if (graphic.getCanGrabGraphic()) {
		// No point was selected, but the graphic was.  Get ready
		// to move the graphic.
		Debug.message("eomg", "GraphicStateMachine|selected state|mousePressed - graphic held");
		graphic.getStateMachine().setEdit();
		graphic.fireEvent(EOMGCursors.MOVE, "");
		graphic.move(e);
	    } else {
		Debug.message("eomg", "GraphicStateMachine|selected state|mousePressed - graphic can't be held");
		graphic.fireEvent(EOMGCursors.DEFAULT, "");
		graphic.redraw(e, true);
	    }
	} else {
	    // else, if the moving point is set, go to edit mode.  If
	    // the mouse is released, we'll consider ourselves
	    // unselected agin.
	    graphic.getStateMachine().setEdit();
	    graphic.fireEvent(EOMGCursors.EDIT, "");
	}
	return getMapMouseListenerResponse();
    }

    /**
     */
    public boolean mouseReleased(MouseEvent e) {
	Debug.message("eomg", "GraphicStateMachine|selected state|mouseReleased");

	GrabPoint mp = graphic.getMovingPoint(e);

	// If the graphic itself was clicked on, then just go to selected
	// mode.
	if (mp == null) {
	    if (graphic.getGraphic().distance(e.getX(), e.getY()) > 2) {
		// if the graphic isn't picked, then unselect the graphic.
		graphic.getStateMachine().setUnselected();
		graphic.fireEvent(EOMGCursors.DEFAULT, "");
		graphic.redraw(e, true);
	    } else if (graphic.getCanGrabGraphic()) {
		graphic.getStateMachine().setEdit();
		graphic.fireEvent(EOMGCursors.EDIT, "");
		graphic.redraw(e, true);
	    }
	} else {
	    // else, if the moving point is set, go to edit mode.  If
	    // the mouse is released, we'll consider ourselves
	    // unselected agin.
	    graphic.getStateMachine().setEdit();
	    graphic.fireEvent(EOMGCursors.EDIT, "");
	    graphic.redraw(e, true);
	}

	graphic.setMovingPoint(null);
	return getMapMouseListenerResponse();
    }

    public boolean mouseMoved(MouseEvent e) {
	Debug.message("eomgdetail", "GraphicStateMachine|selected state|mouseMoved");

	GrabPoint mp = graphic.getMovingPoint(e);

	// If the graphic itself was clicked on, then just go to selected
	// mode.
	if (mp == null) {
	    if (graphic.getGraphic().distance(e.getX(), e.getY()) < 2) {
		graphic.fireEvent(EOMGCursors.EDIT, "Click and Drag to move the graphic.");
	    } else {
		graphic.fireEvent(EOMGCursors.DEFAULT, "");
	    }
	} else {
	    graphic.fireEvent(EOMGCursors.EDIT, "Click and Drag to change the graphic.");
	}
	return false;
    }
}







