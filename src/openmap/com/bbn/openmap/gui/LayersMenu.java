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
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/gui/LayersMenu.java,v $
// $RCSfile: LayersMenu.java,v $
// $Revision: 1.1.1.1 $
// $Date: 2003/02/14 21:35:48 $
// $Author: dietrick $
// 
// **********************************************************************


package com.bbn.openmap.gui;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.beans.beancontext.*;
import java.io.Serializable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;

import com.bbn.openmap.*;
import com.bbn.openmap.event.LayerEvent;
import com.bbn.openmap.event.LayerListener;
import com.bbn.openmap.event.LayerSupport;
import com.bbn.openmap.util.Assert;
import com.bbn.openmap.util.Debug;
import com.bbn.openmap.util.PaletteHelper;

/**
 * The LayersMenu is a JMenu which is a list of the layers of the map.
 * This LayersMenu expects to work with a LayerHandler containing all
 * the possible layers that may be added to the map.  This list
 * defaults to being checkbuttons which add and remove the named layer
 * to/from the Map.  You can also use an instance of this object to
 * show/hide layer palettes.<P> 
 * 
 * This object is interested in contacting a LayerHandler to find out
 * all the layers available, and optionally, a LayersPanel in order to
 * provide a menu item that will call up the GUI for the LayersPanel.
 * If this LayersMenu is a member of a BeanContext, it expects that
 * only one of each of these objects will be added, but if more than
 * one per type is added, the last one added to be BeanContext will be
 * the one hooked up to this LayersMenu.  
 */
public class LayersMenu extends JMenu 
    implements Serializable, LayerListener, MenuBarMenu,
	BeanContextChild, BeanContextMembershipListener 
{

    /**
     * Static value to set this menu to control layer visibility.
     */
    public static final transient int LAYERS_ON_OFF = 1;
    /**
     * Static value to set this menu to control layer palette visibility.
     */
    public static final transient int PALETTES_ON_OFF = 2;
    /**
     * The flag setting the behavior of the menu, whether it controls
     * the layers or the layers palettes.
     */
    protected int menuType=-1;
    /**
     * The LayerHandler to listen to for layers available for the map.
     */
    protected transient LayerHandler layerHandler;
    /**
     * Used by the BeanContext methods to insure that the component we
     * disconnect the edit button from is the same one being removed
     * from the BeanContext.  
     */
    protected transient LayersPanel layersPanel;
    /** A button on the bottom of the menu to bring up the layersPanel. */
    protected transient JMenuItem edit = null;
    /** The default edit button title. */
    public final static String defaultEditLayersButtonTitle = "Edit Layers...";
    /** The actual edit button title. */
    protected transient String editLayersButtonTitle = defaultEditLayersButtonTitle;
    /** The menu item to add a layer to the map. */
    protected transient JMenuItem add = null;
	/** The default add button title. */
    protected transient String addLayersButtonTitle = "Add Layers...";
    

    /**
     * BeanContextChildSupport object provides helper functions for
     * BeanContextChild interface.
     */
    private BeanContextChildSupport beanContextChildSupport = new BeanContextChildSupport();

    /**
     * Construct LayersMenu.
     * @param inLayers the Layers
     */
    public LayersMenu() {
	this (null);
    }

    /**
     * Construct LayersMenu.
     * @param inLayers the Layers
     */
    public LayersMenu(LayerHandler lHandler) {
	this (lHandler, "Layers", LAYERS_ON_OFF);
    }

    /**
     * Construct LayersMenu.
     * @param inLayers the Layers
     * @param menuName the name of the menu
     * @param menuType either LAYERS_ON_OFF, or PALETTES_ON_OFF
     */
    public LayersMenu(LayerHandler lHandler, String menuName, int menuType) {
	super(menuName);
	this.menuType = menuType;
	setMnemonic('L');//HMMMM
	
	layerHandler = lHandler;

	// Layers will be turned on by something else initially.
	if (layerHandler != null) {
	    setLayers(layerHandler.getLayers());
	}

    }

    /** 
     * Set the LayerHandler that the LayersPanel listens to.  If the
     * LayerHandler is not null, the LayersMenu will be added to the
     * LayerHandler LayerListener list, and the LayersMenu will
     * receive a LayerEvent with the current layers. <P>
     *
     * If there is a LayerHandler that is already being listened to,
     * then the LayersPanel will remove itself from current LayerHandler
     * as a LayerListener, before adding itself to the new LayerHandler. <P>
     *
     * Lastly, if the LayerHandler passed in is null, the LayersPanel
     * will disconnect itself from any LayerHandler currently held,
     * and reset itself with no layers.
     *
     * @param lh the LayerHandler containing the layers.
     */
    public void setLayerHandler(LayerHandler lh) {
	if (layerHandler != null) {
	    layerHandler.removeLayerListener(this);
	}

	layerHandler = lh;
	if (layerHandler != null) {
	    layerHandler.addLayerListener(this);
	} else {
	    setLayers(new Layer[0]);
	}
    }

    /** 
     * Get the LayerHandler that the LayersPanel listens to.
     */
    public LayerHandler getLayerHandler() {
	return layerHandler;
    }

    /**
     * Set the LayersPanel privately to keep track of what object is
     * being used from the BeanContext.  
     */
    protected void setLayersPanel(LayersPanel lp) {
	layersPanel = lp;
    }

    /**
     * Get the LayersPanel, privately held to keep track of what object is
     * being used from the BeanContext.  
     */
    protected LayersPanel getLayersPanel() {
	return layersPanel;
    }

    /**
     * Return the title of the menu option to call up LayersPanel.
     */
    public void setEditLayersButtonTitle(String buttonTitle) {
	editLayersButtonTitle = buttonTitle;
    }

    /**
     * Return the title of the menu option to call up LayersPanel.
     */
    public String getEditLayersButtonTitle() {
	return editLayersButtonTitle;
    }

    /**
     * Set the edit menu item that tiggers the LayersPanel action
     * listener.  Assumes that it's already wired up.
     */
    public void setEdit(JMenuItem e) {
	edit = e;

	// This actually adds the edit button to the bottom of the
	// LayerMenu when the menu is reconstructed.
	if (getLayerHandler() != null) {
	    setLayers(getLayerHandler().getLayers());
	}
    }

    /**
     * Set the add menu item. 
     */
    public void setAdd(JMenuItem a) {
    	add = a;
	if (getLayerHandler() != null) {
	    setLayers(getLayerHandler().getLayers());
	}
    }
    
    /**
     * Get the edit menu item that tiggers the LayersPanel action
     * listener.
     */
    public JMenuItem getEdit() {
	return edit;
    }
    
    /**
     * LayerListener interface method.
     * A list of layers will be added, removed, or replaced based on
     * on the type of LayerEvent.
     * @param evt a LayerEvent
     */
    public void setLayers(LayerEvent evt) {
        Layer[] layers = evt.getLayers();
	int type = evt.getType();
	
	if (type==LayerEvent.ALL) {
	    setLayers(layers);
	}
    }
    
    /**
     * Set the layers that are on the menu.  Calls setLayers(layers, true);
     * @param inLayers the array of layers.
     */
    public void setLayers(Layer[] inLayers) {
	
	removeAll();

	// Set everything up for the new layers
	if (inLayers == null) {
	    if(Debug.debugging("layersmenu")) {
		Debug.error("LayersMenu.setLayers(): Layers are null.");
	    }
	} else {
	    for (int i=0; i<inLayers.length; i++) {
		LayerCheckBoxMenuItem cbs = 
		    new LayerCheckBoxMenuItem(inLayers[i]);
		add(cbs);
	    }
	}

	if (edit != null) {
	    add(new JSeparator());
	    add(edit);
	}
	if (add != null) {
	    add(add);
	}
    }

    /**
     * Remove all the components from the menu.  Also calls cleanup()
     * on all the LayerCheckBoxMenuItems, so they can remove
     * themselves from their layers.  
     */
    public void removeAll() {
	Component[] components = getMenuComponents();

	if (components.length > 0) {
	    Debug.message("layersmenu", "LayersMenu.removeAll(): purging menu");
	}

	for (int i = 0; i < components.length; i++) {
	    if (components[i] instanceof LayerCheckBoxMenuItem) {
		((LayerCheckBoxMenuItem)components[i]).cleanup();
	    }
	}
	super.removeAll();
    }

    /**
     * Update the layer names.
     */
    public synchronized void updateLayerLabels() {
	if (layerHandler != null) {
	    setLayers(layerHandler.getLayers());
	}
    }

    /**
     * CheckBoxMenuItem that encapsulates a Layer.
     */
    class LayerCheckBoxMenuItem
	extends JCheckBoxMenuItem
	implements ActionListener, ComponentListener
    {
	/** The layer that the button triggers. */
        Layer layer;
	/** The applet palette to bring up if this is a palette
	 *  menu. */
	JInternalFrame paletteWindow;
	/** The application palette to bring up if this is a palette
	 *  menu. */
	JFrame paletteWindow2;

	/**
	 * Construct the menu item, connected to the given layer.
	 */
        LayerCheckBoxMenuItem(Layer aLayer) {
           if (aLayer == null) {
               throw new IllegalArgumentException("null Layer");
           }
           layer = aLayer;
           this.setText(layer.getName());
	   setState(layer.isVisible());

           this.addActionListener(this);
	   layer.addComponentListener(this);
	}

	/** Get the layer for this checkbox. */      
        public Layer getLayer() {
	    return layer;
	}

	/** Disconnect all the listeners from the layer, clean up
	 *  other references. */
	public void cleanup() {
	    layer.removeComponentListener(this);
	    this.removeActionListener(this);
	    layer = null;
	}

	/** If this widget is being used for bringing up palettes,
	 *  bring up the layer's palette.*/
	protected void showPalette() {
	    layer.showPalette();
	}


	/** If this widget is being used for bringing up palettes,
	 *  hide the layer's palette.*/
	protected void hidePalette() {
	    layer.hidePalette();
	}

	/** This menu item listens to the status of its layer. */
	public void componentResized(ComponentEvent e) {}
	/** This menu item listens to the status of its layer. */
	public void componentMoved(ComponentEvent e) {}
	/** This menu item listens to the status of its layer.  If the
	 *  layer becomes visible, it makes the check box enabled. */
	public void componentShown(ComponentEvent e) {
	    if (e.getComponent() == layer) {
		if (getState() != true && menuType == LAYERS_ON_OFF) {
		    setState(true);
		    if (Debug.debugging("layersmenu")) {
			Debug.output("layersmenu.LCBMI: layer " + 
				     layer.getName() +
				     " is now visible.");
		    }
		}
	    } else if (e.getComponent() == layer.getPalette() &&
		       menuType == PALETTES_ON_OFF) {
		    setState(true);
	    }
	}
	/** This menu item listens to the status of its layer.  If the
	 *  layer becomes invisible, it disables the check box. */
	public void componentHidden(ComponentEvent e) {
	    if (e.getComponent() == layer) {
		if (getState() != false && menuType == LAYERS_ON_OFF) {
		    setState(false);
		    if (Debug.debugging("layersmenu")) {
			Debug.output("layersmenu.LCBMI: layer " + 
				     layer.getName() +
				     " is now hidden.");
		    }
		}
	    } else if (e.getComponent() == layer.getPalette() && 
		       menuType == PALETTES_ON_OFF) {
		setState(false);
	    }
	}

	/**
	 * Triggered by the menu item check button. 
	 * @param e ActionEvent fired by the button.
	 */
        public void actionPerformed(ActionEvent e) {

            if (!this.equals(e.getSource())) {
	        Debug.error("Wiring is hopelessly wrong in LayersMenu");
	    }
	    switch (menuType) {
		case LAYERS_ON_OFF:
		    layerHandler.turnLayerOn(getState(), layer);
		    break;
		case PALETTES_ON_OFF:
		    if (getState())
			showPalette();
		    else
			hidePalette();
		    break;
		default:
		    System.err.println("LayersMenu: unknown menuType!");
	    }
	}
    }

    /**
     * Given a LayersPanel, set up a JButton to add to the end of the
     * LayersMenu that will trigger the action listener of the
     * LayersPanel.
     *
     * @param lp the LayersPanel to ask for an ActionListener from.  
     */
    public void setupEditLayersButton(LayersPanel lp) {
	// initalize the Edit Layers... button.
	JMenuItem button = new JMenuItem(editLayersButtonTitle);
	button.setActionCommand("edit");
	button.addActionListener(lp.getActionListener());
	setEdit(button);
	setLayersPanel(lp);
    }

    /** 
     * Constructs the menu item that will bring up the LayerAddPanel.
     */
    public void setupLayerAddButton(final LayerAddPanel menu) {
	final LayerAddPanel lap = menu;
    	JMenuItem button = new JMenuItem(addLayersButtonTitle);
	button.setActionCommand("add");
	button.addActionListener(lap);
	setAdd(button);
	add(button);
    }

    /**
     * Called when the BeanContext membership changes.  interator from
     * the BeanContext.  This lets this object hook up with what it
     * needs.  It expects to find only one LayerHandler and
     * LayersPanel in the BeanContext.  If another
     * LayerHandler/LayersPanel is somehow added to the BeanContext,
     * however, it will drop the connection to the component it is set
     * up to listen to, and rewire itself to reflect the status of the
     * last version of the LayerHandler/LayersPanel found.
     * @param it Iterator received in the BeanContext or
     * BeanContextMembershioEvent.  
     */
    protected void findAndInit(Iterator it) {
	Object someObj;
	while (it.hasNext()) {
	    someObj = it.next();
	    if (someObj instanceof LayerHandler) {
		// do the initializing that need to be done here
		Debug.message("bc","LayersMenu found a LayerHandler");
		setLayerHandler((LayerHandler)someObj);
//  	    } else if (someObj instanceof PropertyHandler) {
//  	    	propertyHandler = (PropertyHandler)someObj;
	    } else if (someObj instanceof LayersPanel) {
		setupEditLayersButton((LayersPanel)someObj);
	    } else if (someObj instanceof LayerAddPanel) {
		// if a LayerAddPanel is present, do the necessary things
	    	setupLayerAddButton((LayerAddPanel)someObj);
	    }
	}
    }

//      private PropertyHandler propertyHandler = null;

    /**
     * BeanContextMembershipListener method, called when new objects
     * are added to the BeanContext.  This calls findAndInit in order
     * to this object to hook up with other objects it is interested
     * in.
     *
     * @param bcme BeanContextMembershipEvent, which also contains an
     * Iterator to use to go through the new objects.  
     */
    public void childrenAdded(BeanContextMembershipEvent bcme) {
	findAndInit(bcme.iterator());      
    }
    
    /**
     * Called when objects are removed from the BeanContext.  Should
     * be used to check for relevant objects that need to be
     * disconnected from this object.  This method does check to see
     * if a LayerHandler or LayersPanel is being removed, that it is
     * the same object currently being used by this LayersMenu.
     *
     * @param bcme BeanContextMembershipEvent, which also contains an
     * Iterator to use to go through the removed objects.  
     */
    public void childrenRemoved(BeanContextMembershipEvent bcme) {
	Iterator it = bcme.iterator();
	Object someObj;
	while (it.hasNext()) {
	    someObj = it.next();
	    if (someObj instanceof LayerHandler) {

		LayerHandler lh = (LayerHandler)someObj;
		// Need to check to see if this layerhandler is the
		// same as the one we have !!!!
		if (lh != getLayerHandler()) {
		    Debug.message("bc", "LayersMenu asked to remove LayerHandler that is not the same as what is currently held - ignoring request.");
		    return;
		}
		
		// do the initializing that need to be done here
		Debug.message("bc","LayersMenu.childrenRemoved: removing LayerHandler");
		setLayerHandler(null);
		setEdit(null);
	    }
	    if (someObj instanceof LayersPanel) {
		LayersPanel lp = (LayersPanel) someObj;
		// There's a problem here.  We can't tell if the
		// LayersPanel being removed is the owner of the
		// action listener used by the edit button.  If two
		// LayersPanels have been added to the BeanContext,
		// and we're now listening to the second one, then if
		// the first one is removed, we are forced here to
		// disconnect from the second and valid one.  Looks
		// like we need to maintain a handle on the
		// LayersPanel being triggered.
		if (lp != getLayersPanel()) {
		    Debug.message("bc", "LayersMenu asked to remove LayersPanel that is not the same as what is currently held - ignoring request.");
		    return;
		}
		// do the initializing that need to be done here
		Debug.message("bc","LayersMenu.childrenRemoved: removing LayersPanel");
		setLayersPanel(null);
		setEdit(null);
	    }
	}
    }

    /**
     * Get the BeanContext that this object is a member of.
     */
    public BeanContext getBeanContext() {
	return beanContextChildSupport.getBeanContext();
    }
  
    /** 
     * Called when this object joins the BeanContext. 
     * @param in_bc the BeanContext that is being joined.
     */
    public void setBeanContext(BeanContext in_bc) throws PropertyVetoException {
	if(in_bc != null) {
	    in_bc.addBeanContextMembershipListener(this);
	    beanContextChildSupport.setBeanContext(in_bc);
	    findAndInit(in_bc.iterator());
	}
    }
  
    public void addVetoableChangeListener(String propertyName,
					  VetoableChangeListener in_vcl) {
	beanContextChildSupport.addVetoableChangeListener(propertyName, in_vcl);
    }
  
    public void removeVetoableChangeListener(String propertyName, 
					     VetoableChangeListener in_vcl) {
	beanContextChildSupport.removeVetoableChangeListener(propertyName, in_vcl);
    }

    /**
     * Report a vetoable property update to any registered listeners. 
     * If anyone vetos the change, then fire a new event 
     * reverting everyone to the old value and then rethrow 
     * the PropertyVetoException. <P>
     *
     * No event is fired if old and new are equal and non-null.
     * <P>
     * @param name The programmatic name of the property that is about to
     * change
     * 
     * @param oldValue The old value of the property
     * @param newValue - The new value of the property
     * 
     * @throws PropertyVetoException if the recipient wishes the property
     * change to be rolled back.
     */
    public void fireVetoableChange(String name, 
				   Object oldValue, 
				   Object newValue) 
	throws PropertyVetoException {
	super.fireVetoableChange(name, oldValue, newValue);
	beanContextChildSupport.fireVetoableChange(name, oldValue, newValue);
    }

}
