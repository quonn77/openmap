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
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/Layer.java,v $
// $RCSfile: Layer.java,v $
// $Revision: 1.1.1.1 $
// $Date: 2003/02/14 21:35:48 $
// $Author: dietrick $
// 
// **********************************************************************


package com.bbn.openmap;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.beans.beancontext.*;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;

import com.bbn.openmap.ProjectionPainter;
import com.bbn.openmap.event.*;
import com.bbn.openmap.layer.util.LayerUtils;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.util.Debug;
import com.bbn.openmap.util.PaletteHelper;
import com.bbn.openmap.util.PropUtils;
import com.bbn.openmap.util.propertyEditor.Inspector;

/**
 * Layer objects are components which can be added to the MapBean to
 * make a map.
 * <p>
 *
 * Layers implement the ProjectionListener interface to listen for
 * ProjectionEvents.  When the projection changes, they may need to
 * refetch, regenerate their graphics, and then repaint themselves
 * into the new view.
 * <p>
 *
 * When the Layer is added to the MapBean, it will start receiving
 * ProjectionEvents via the ProjectionListener.projectionChanged()
 * method it has to implement.  There is a
 * setProjection(ProjectionEvent) methods that should be called from
 * there if you want to save the projection for later use (handling
 * MouseEvents, etc).  If you call getProjection() before calling
 * setProjection(), getProjection() will return null, and your
 * OMGraphics will complain and probably freak out at some point.
 *
 *<pre>
 *  //// SAMPLE handling of the ProjectionListener interface.
 *
 *   public void projectionChanged(com.bbn.openmap.event.ProjectionEvent pe) {
 *      Projection proj = setProjection(pe);
 *      if (proj != null) {
 *	    // Use the projection to gather OMGraphics in the layer,
 *	    // and prepare the layer so that in the paint() method,
 *	    // the OMGraphics get rendered.  
 *
 *	    // Call any methods that kick off work to build graphics
 *	    // here...
 *
 *	    // You get the paint() methods called by calling
 *	    // repaint():
 *	    repaint();
 *	 } 
 *	
 *	 fireStatusUpdate(LayerStatusEvent.FINISH_WORKING);
 *    }
 * </pre>
 *
 * @see com.bbn.openmap.event.ProjectionListener
 * @see com.bbn.openmap.event.ProjectionEvent
 * @see com.bbn.openmap.PropertyConsumer
 */
public abstract class Layer extends JComponent
    implements ProjectionListener, ProjectionPainter, BeanContextChild, BeanContextMembershipListener, PropertyConsumer, ActionListener
{

    /**
     * Precaches the swing package.  Computed based on the package of
     * <code>JComponent</code>.
     */
    protected static final String SWING_PACKAGE = getPackage(JComponent.class);

    /**
     * The String to use for a key lookup in a Properties object to
     * find the name to use in a GUI relating to this layer.  
     */
    public static final String PrettyNameProperty = "prettyName";

    /**
     * The property to set to add the layer to the BeanContext
     * "addToBeanContext".  This needs be set by the layer itself,
     * because it knows whether it needs other components or not.
     * However, this property is defined in case an option can be
     * given to the user.  If a Layer doesn't want this option given,
     * it should reset the addToBeanContext variable after
     * setProperties() is called.  The Layer.setProperties() methods
     * maintain the current state of the variable if undefined, which
     * is false by default.
     */
    public static final String AddToBeanContextProperty = "addToBeanContext";

    public static final String AddAsBackgroundProperty = "background";

    /**
     * The property to show the palette when the layer is created -
     * or, more accurately, when the properties are set.
     */
    public static final String AutoPaletteProperty = "autoPalette";
    /** Layer-defined action event command to display the palette. */    
    public static final String DisplayPaletteCmd = "displayPaletteCmd";
    /** Layer-defined action event command to hide the palette. */    
    public static final String HidePaletteCmd = "hidePaletteCmd";
    /**
     * Layer-defined action event command to display the properties
     * using an Inspector. 
     */
    public static final String DisplayPropertiesCmd = "displayPropertiesCmd";
    /**
     * Layer-defined action event command to force a redraw on the
     * layer.  The Layer class does not respond to this command, it's
     * provided as a convenience. 
     */
    public static final String RedrawCmd = "redrawCmd";

    /**
     * The listeners to the Layer that respond to requests for
     * information displays, like messages, requests for URL displays,
     * etc.
     */
    protected Vector IDListeners = null;

    /**
     * List of LayerStatusListeners.
     */
    protected Vector lsListeners = null;

    /**
     * Arguments modified by the Layer, or set by the Bean, at
     * runtime.
     * @deprecated use setProperties
     */
    protected String dynamicArgs = null;

    /** 
     * Flag to indicate whether a AWTToolkit is available. Almost
     * always should be left alone, unless you are doing something
     * without a display available.  This flag, when false, redirects
     * the repaint() method to fire a LayerStatusEvent.FINISH_WORKING
     * instead.
     */
    protected static boolean AWTAvailable = true;
    
    /**
     * Token uniquely identifying this layer in the application
     * properties.
     */
    protected String propertyPrefix = null;

    /**
     * Used by the LayerHandler to check if the layer should be added
     * to the MapHandler BeanContext.  See the comments under the
     * AddToBeanContextProperty.  False by default.
     */
    protected boolean addToBeanContext = false;

    protected boolean addAsBackground = false;

    /**
     * A flag to have the layer display it's palette when the
     * properties are set.  If you are creating a layer manually, just
     * call showPalette() instead.
     */
    protected boolean autoPalette = false;

    /**
     * This is a convenience copy of the latest projection received
     * from the MapBean, when the Layer is added to the map.  If you
     * need it, use the accessor!.
     */
    private Projection projection = null;

    /**
     * The BeanContext allows Layers to find other components, and
     * other components to find the layer, if the layer is added to
     * it.
     */
    protected final BeanContextChildSupport beanContextChildSupport = new BeanContextChildSupport(this);

    protected transient Container palette = null;

    /**
     * Set AWTAvailable flag.
     * Your layer should not need to call this.
     * @param value boolean
     */
    public static void setAWTAvailable(boolean value) {
	AWTAvailable = value;
    }

    /**
     * Check AWTAvailable flag.
     * @return boolean
     */
    public static boolean isAWTAvailable() {
	return AWTAvailable;
    }

    /**
     * Returns the package of the given class as a string.
     *
     * @param c a class
     */
    protected static String getPackage(Class c) {
	String className = c.getName();
	int lastDot = className.lastIndexOf('.');
	return className.substring(0, lastDot);
    }

    /**
     * Override to only allow swing package listeners.  If Listeners
     * get added to the Layers, the mouse events don't make it to the
     * map.  Ever.
     * <p>
     * Swing popup menus, like <code>JPopupMenu</code> grab the
     * JComponent by adding themselves as <code>MouseListener</code>s.
     * So this method allows instances of classes in the xxx.swing
     * package to be added as <code>MouseListener</code>s, and no one
     * else.
     *
     * @param l a mouse listener.
     */
    public final void addMouseListener(MouseListener l) {
	String pkg = getPackage(l.getClass());
	if (java.beans.Beans.isDesignTime() ||
	    pkg.equals(SWING_PACKAGE) ||
	    pkg.startsWith(SWING_PACKAGE)) {

	    // Used to do nothing for the equals and startsWith
	    // comparison, but that breaks the menus from being
	    // recinded when something else is clicked on. Thanks to
	    // Tom Peel for pointing this out, 11/29/00.
	    super.addMouseListener(l);

	} else {
	    throw new IllegalArgumentException(
	            "This operation is disallowed because the package \""
		    + pkg + "\" is not in the swing package (\"" +
		    SWING_PACKAGE + "\").");
	}
    }

    /**
     * Interface Layer method to get the dynamic args.
     * @return String args
     * @deprecated use setProperties
     */
    public String getArgs() {
        return dynamicArgs;
    }

    /**
     * Interface Layer method to set the dynamic args.
     * @param args String
     * @deprecated use setProperties
     */
    public void setArgs(String args) {
	dynamicArgs = args;
    }

    /**
     * Interface Layer method to receive layer arguments.
     * @deprecated use setProperties
     * @param argv String[]
     */
    public void setArgs(String argv[]) {
    }

    /**
     * Accessor for the marker associated with this layer.  This is
     * the marker that uniquely identifies this layer in the
     * application properties.
     * @deprecated replaced by getPropertyPrefix() as of OpenMap 4.0.
     */
    public String getMarker() {
	return getPropertyPrefix();
    }

    /**
     * Sets the properties for the <code>Layer</code>.  This allows
     * <code>Layer</code>s to get a richer set of parameters than the
     * <code>setArgs</code> method.  This particular method assumes
     * that the marker name is not needed, because all of the contents
     * of this Properties object are to be used for this layer, and
     * scoping the properties with a prefix is unnecessary.
     * @param props the <code>Properties</code> object.
     * @see #setArgs 
     */
    public void setProperties(java.util.Properties props) {
	setProperties(getPropertyPrefix(), props);
    }

    /**
     * Sets the properties for the <code>Layer</code>.  This allows
     * <code>Layer</code>s to get a richer set of parameters than the
     * <code>setArgs</code> method. Part of the PropertyConsumer interface.
     * Layers which override this method should do something like:
     * <code><pre>
     * public void setProperties (String prefix, Properties props) {
     *     super.setProperties(prefix, props);
     *     // do local stuff
     * }
     * </pre></code>
     *
     * If the addToBeanContext property is not defined, it maintains
     * the same state.
     *
     * @param prefix the token to prefix the property names
     * @param props the <code>Properties</code> object
     * @see #setArgs
     */
    public void setProperties(String prefix, java.util.Properties props) {
	String prettyName = PrettyNameProperty;
	setPropertyPrefix(prefix);

	String realPrefix = PropUtils.getScopedPropertyPrefix(prefix);

	prettyName = realPrefix + PrettyNameProperty;
	
	String defaultName = getName(); 
	if (defaultName == null) {
	    defaultName = "Anonymous";
	}

	setName(props.getProperty(prettyName, defaultName));

	setAddToBeanContext(LayerUtils.booleanFromProperties(props, realPrefix + AddToBeanContextProperty, addToBeanContext));

	setAddAsBackground(LayerUtils.booleanFromProperties(props, realPrefix + AddAsBackgroundProperty, addAsBackground));

	autoPalette = LayerUtils.booleanFromProperties(props, realPrefix + AutoPaletteProperty, autoPalette);
    }

    /**
     * PropertyConsumer method, to fill in a Properties object,
     * reflecting the current values of the layer.  If the
     * layer has a propertyPrefix set, the property keys should
     * have that prefix plus a separating '.' prepended to each
     * propery key it uses for configuration.
     *
     * @param props a Properties object to load the PropertyConsumer
     * properties into.  If props equals null, then a new Properties
     * object should be created.
     * @return Properties object containing PropertyConsumer property
     * values.  If getList was not null, this should equal getList.
     * Otherwise, it should be the Properties object created by the
     * PropertyConsumer.
     */
    public Properties getProperties(Properties props) {
	if (props == null) {
	    props = new Properties();
	}

	String prefix = PropUtils.getScopedPropertyPrefix(propertyPrefix);
	props.put(prefix + "class", this.getClass().getName());

	String prettyName = getName();
	if (prettyName != null) {
	    props.put(prefix + PrettyNameProperty, prettyName);
	}

	props.put(prefix + AutoPaletteProperty, new Boolean(autoPalette).toString());
// 	props.put(prefix + AddAsBackgroundProperty, new Boolean(addAsBackground).toString());
	props.put(prefix + AddToBeanContextProperty, new Boolean(addToBeanContext).toString());

	return props;
    }

    /**
     * Method to fill in a Properties object with values reflecting
     * the properties able to be set on this PropertyConsumer.  The
     * key for each property should be the raw property name (without
     * a prefix) with a value that is a String that describes what the
     * property key represents, along with any other information about
     * the property that would be helpful (range, default value,
     * etc.).  For Layer, this method should at least return the
     * 'prettyName' property.
     *
     * @param list a Properties object to load the PropertyConsumer
     * properties into.  If getList equals null, then a new Properties
     * object should be created.
     * @return Properties object containing PropertyConsumer property
     * values.  If getList was not null, this should equal getList.
     * Otherwise, it should be the Properties object created by the
     * PropertyConsumer. 
     */
    public Properties getPropertyInfo(Properties list) {
	if (list == null) {
	    list = new Properties();
	}
	
	list.put("class", "Class Name used for Layer.");
	list.put(PrettyNameProperty, "Presentable name for Layer.");
	list.put("class.editor",  "com.bbn.openmap.util.propertyEditor.NonEditablePropertyEditor");
	list.put(PrettyNameProperty + ScopedEditorProperty, "com.bbn.openmap.util.propertyEditor.NonEditablePropertyEditor");
	list.put(AutoPaletteProperty, "Flag to automatically display palette when properties are set");
	list.put(AutoPaletteProperty + ScopedEditorProperty, "com.bbn.openmap.util.propertyEditor.YesNoPropertyEditor");

// 	list.put(AddAsBackgroundProperty, "Flag to use the layer as a background layer.");
// 	list.put(AddAsBackgroundProperty + ScopedEditorProperty, "com.bbn.openmap.util.propertyEditor.YesNoPropertyEditor");

	list.put(AddToBeanContextProperty, "Flag to give the layer access to all of the other application components.");
	list.put(AddToBeanContextProperty + ScopedEditorProperty, "com.bbn.openmap.util.propertyEditor.YesNoPropertyEditor");

	return list;
    }

    /**
     * Set the property key prefix that should be used by the
     * PropertyConsumer.  The prefix, along with a '.', should be
     * prepended to the property keys known by the PropertyConsumer.
     *
     * @param prefix the prefix String.  
     */
    public void setPropertyPrefix(String prefix) {
	propertyPrefix = prefix;
    }

    /**
     * Get the property key prefix that is being used to prepend to
     * the property keys for Properties lookups.
     *
     * @param String prefix String.  
     */
    public String getPropertyPrefix() {
	return propertyPrefix;
    }

    /**
     * Set the projection the layer should use for calculations.  You
     * probably don't need this if you are wondering if you do.  Call
     * setProjection(projEvent) instead.
     */
    public void setProjection(Projection proj) {
	projection = proj;
    }

    /**
     * This method lets you take the ProjectionEvent received from the
     * MapBean, and lets you know if you should do something with it.
     * MUST to be called in the projectionChanged() method of your
     * layer, if you want to refer to the projection later.  If this
     * methods returns null, you probably just want to call repaint()
     * if your layer.paint() method is ready to paint what it should.
     *
     * @param projEvent the ProjectionEvent from the ProjectionListener method.
     * @return The new Projection if it is different from the one we
     * already have, null if is the same as the current one.  
     */
    public Projection setProjection(ProjectionEvent projEvent) {
	Projection newProjection = projEvent.getProjection();
	
	if (!newProjection.equals(getProjection())) {
	    Projection clone = newProjection.makeClone();
	    setProjection(clone);
	    return clone;
	} else {
	    return null;
	}
    }

    /**
     * Get the latest projection.
     */
    public Projection getProjection() {
	return projection;
    }

    /**
     * Returns the MapMouseListener object that handles the mouse
     * events.  This method is IGNORED in this class: it returns null.
     * Derived Layers should return the appropriate object if they
     * desire to receive MouseEvents.  The easiest thing for a Layer
     * to do in order to receive MouseEvents is to implement the
     * MapMouseListener interface and return itself.  A code snippet:
     * <code><pre>
     *  public MapMouseListener getMapMouseListener() {
     *	    return this;
     *  }
     *  public String[] getMouseModeServiceList() {
     *	    return new String[] {
     *	        SelectMouseMode.modeID
     *	    };
     *  }
     * </pre></code>
     * @return null
     */
    public synchronized MapMouseListener getMapMouseListener() {
	return null;
    }
 
    /**
     * Gets the gui controls associated with the layer.
     * This default implementation returns null indicating
     * that the layer has no gui controls.
     *
     * @return java.awt.Component or null
     */
    public Component getGUI() {
	return null;
    }


    ///////////////////////////////////////////////////
    //  InfoDisplay Handling Setup and Firing

    /**
     * Adds a listener for <code>InfoDisplayEvent</code>s.
     *
     * @param aInfoDisplayListener the listener to add
     */
    public synchronized void addInfoDisplayListener(
	InfoDisplayListener aInfoDisplayListener) {
	if (IDListeners == null) {
	    IDListeners = new java.util.Vector();
	}
	IDListeners.addElement(aInfoDisplayListener);
    }

    /**
     * Removes an InfoDisplayListener from this Layer.
     *
     * @param aInfoDisplayListener the listener to remove
     */
    public synchronized void removeInfoDisplayListener(
	InfoDisplayListener aInfoDisplayListener) {
	if (IDListeners == null) {
	    return;
	}
	IDListeners.removeElement(aInfoDisplayListener);
    }

    /**
     * Sends a request to the InfoDisplayListener to show the information in
     * the InfoDisplay event on an single line display facility. 
     * @param evt the InfoDisplay event carrying the string.
     */
    public void fireRequestInfoLine(InfoDisplayEvent evt) {
	InfoDisplayListener temp[] = getSynchronizedListeners();
	if (temp != null) {
	    for (int i = 0; i < temp.length; i++) {
		temp[i].requestInfoLine(evt);
	    }
	} else if (Debug.debugging("layer")) { 
	    Debug.output(getName() +
			 "|Layer.fireRequestInfoLine(): no info request listener!");
	}
    }

    /**
     * Sends a request to the InfoDisplay listener to display the information
     * on an single line display facility.
     * The InfoDisplayEvent is created inside this function.
     * @param infoLine the string to put in the InfoDisplayEvent.  
     */
    public void fireRequestInfoLine(String infoLine) {
	fireRequestInfoLine(new InfoDisplayEvent(this, infoLine));
    }

    /**
     * Sends a request to the InfoDisplay listener to display the information
     * in the InfoDisplay event in a Browser.
     * @param evt the InfoDisplayEvent holding the contents to put in the
     * Browser.
     */
    public void fireRequestBrowserContent(InfoDisplayEvent evt) {
	InfoDisplayListener temp[] = getSynchronizedListeners();
	if (temp != null) {
	    for (int i = 0; i < temp.length; i++) {
		temp[i].requestBrowserContent(evt);
	    }
	} else if (Debug.debugging("layer")) { 
	    Debug.output(getName() +
			 "|Layer.fireRequestBrowserContent(): no info request listener!");
	}
    }

    /**
     * Sends a request to the InfoDisplayListener to display the information
     * in a Browser.
     * The InfoDisplayEvent is created here holding the browserContent
     * @param browserContent the contents to put in the Browser.
     */
    public void fireRequestBrowserContent(String browserContent) {
	fireRequestBrowserContent(new InfoDisplayEvent(this, browserContent));
    }

    /**
     * Sends a request to the InfoDisplayListener to display a URL given in
     * the InfoDisplay event in a Browser. 
     * @param evt the InfoDisplayEvent holding the url location to give to
     * the Browser.
     */
    public void fireRequestURL(InfoDisplayEvent evt) {
	InfoDisplayListener temp[] = getSynchronizedListeners();
	if (temp != null) {
	    for (int i = 0; i < temp.length; i++) {
		temp[i].requestURL(evt);
	    }
	} else if (Debug.debugging("layer")) { 
	    Debug.output(getName() +
			 "|Layer.fireRequestURL(): no info request listener!");
	}
    }

    /**
     * Sends a request to the InfoDisplayListener to display a URL in a
     * browser.
     * The InfoDisplayEvent is created here, and the URL location is put
     * inside it.
     * @param url the url location to give to the Browser.
     */
    public void fireRequestURL(String url) {
	fireRequestURL(new InfoDisplayEvent(this, url));
    }

    /**
     * Sends a request to the InfoDisplayListener to show a specific cursor
     * over its component area.
     * @param cursor the cursor to use.
     */
    public void fireRequestCursor(java.awt.Cursor cursor) {
	InfoDisplayListener temp[] = getSynchronizedListeners();
	if (temp != null) {
	    for (int i = 0; i < temp.length; i++) {
		temp[i].requestCursor(cursor);
	    }
	} else if (Debug.debugging("layer")) { 
	    Debug.output(getName() +
			 "|Layer.fireRequestCursor(): no info request listener!");
	}
    }

    /**
     * Sends a request to the InfoDisplayListener to put the information in
     * the InfoDisplay event in a dialog window. 
     * @param evt the InfoDisplayEvent holding the message to put into
     * the dialog window.
     */
    public void fireRequestMessage(InfoDisplayEvent evt) {
	InfoDisplayListener[] temp = getSynchronizedListeners();
	if (temp != null) {
	    for (int i = 0; i < temp.length; i++) {
		temp[i].requestMessage(evt);
	    }
	} else if (Debug.debugging("layer")) { 
	    Debug.output(getName() +
			 "|Layer.fireRequestMessage(): no info request listener!");
	}
    }

    /**
     * Sends a request to the InfoDisplayListener to display the information
     * in a dialog window.
     * The InfoDisplayEvent is created here, and the URL location is put
     * inside it.
     * @param message the message to put in the dialog window.
     */
    public void fireRequestMessage(String message) {
	fireRequestMessage(new InfoDisplayEvent(this, message));
    }

    /**
     * Request to show the tool tips on the map.
     *
     * @param me MouseEvent location for the tool tip.
     * @param tip string to display.
     */
    public void fireRequestToolTip(MouseEvent me, String tip) {
	fireRequestToolTip(me, new InfoDisplayEvent(this, tip));
    }

    /**
     * Request to hide the tool tips on the map.
     *
     * @param me MouseEvent location.
     */
    public void fireHideToolTip(MouseEvent me) {
	fireRequestToolTip(me, (InfoDisplayEvent) null);
    }

    /**
     * Fire off a Tool Tip request to the InfoDisplayListeners.  If
     * the InfoDisplayEvent is null, then a requestHideToolTip will be
     * fired. 
     */
    public void fireRequestToolTip(MouseEvent me, 
				   InfoDisplayEvent event) {
	InfoDisplayListener[] temp = getSynchronizedListeners();
	if (temp != null) {
	    for (int i = 0; i < temp.length; i++) {
		if (event != null) {
		    temp[i].requestShowToolTip(me, event);
		} else {
		    temp[i].requestHideToolTip(me);
		}
	    }
	} else if (Debug.debugging("layer")) { 
	    Debug.output(getName() +
			 "|Layer.fireRequestShowToolTip(): no info request listener!");
	}
    }

    /**
     * Get the InfoDisplayListeners.
     * Provides an internal InfoDisplayListener that is synchronized at the
     * time of the check for null, so that we won't attempt to use it
     * later where there might have been an opportunity for it to have
     * been deleted.  Huh?
     * @return a personal copy of the InfoDisplayListener
     */
    protected InfoDisplayListener[] getSynchronizedListeners() {
	// use this for freakin' thread safety
	InfoDisplayListener[] temp = null;
	synchronized (this) {
	    if (IDListeners == null) {
		return temp;
	    }
	    int numListeners = IDListeners.size();
	    temp =  new InfoDisplayListener[numListeners];
	    for (int i = 0; i < numListeners; i++) {
	       temp[i] = (InfoDisplayListener)IDListeners.elementAt(i);
	    }
	}
	return temp;
    }


    ///////////////////////////////////////////////////
    //  LayerStatus Handling Setup and Firing

    /**
     * Returns an array of all the LayerStatusListeners.
     * @return LayerStatusListener[]
     */
    protected LayerStatusListener[] getSynchronizedStatusListeners() {
	// use this for freakin' thread safety
	LayerStatusListener[] temp = null;
	synchronized (this) {
	    if (lsListeners == null) {
		return temp;
	    }
	    int numListeners = lsListeners.size();
	    temp =  new LayerStatusListener[numListeners];
	    for (int i = 0; i < numListeners; i++) {
	       temp[i] = (LayerStatusListener)lsListeners.elementAt(i);
	    }
	}
	return temp;
    }

    /**
     * Adds a listener for <code>LayerStatusEvent</code>s.
     *
     * @param aLayerStatusListener LayerStatusListener
     */
    public synchronized void addLayerStatusListener(LayerStatusListener aLayerStatusListener) {
	if (lsListeners == null) {
	    lsListeners = new java.util.Vector();
	}

	if (!lsListeners.contains(aLayerStatusListener)) {
	    lsListeners.addElement(aLayerStatusListener);
	}
    }

    /**
     * Removes a LayerStatusListene from this Layer.
     *
     * @param aLayerStatusListener the listener to remove
     */
    public synchronized void removeLayerStatusListener(
	LayerStatusListener aLayerStatusListener) {

	if (lsListeners == null) {
	    return;
	}
	lsListeners.removeElement(aLayerStatusListener);
    }

    /**
     * Sends a status update to the LayerStatusListener.
     * @param evt LayerStatusEvent
     */
    public void fireStatusUpdate(LayerStatusEvent evt) {
	if (AWTAvailable) {
	    LayerStatusListener[] temp = getSynchronizedStatusListeners();
	    if (temp != null) {
		for (int i = 0; i < temp.length; i++) {
		    temp[i].updateLayerStatus(evt);
		}
	    } else if (Debug.debugging("layer")) {
		Debug.output(getName() + 
			     "|Layer.fireStatusUpdate(): no LayerStatusListener!");
	    }
	}
    }

    /**
     * Sends a status update to the LayerStatusListener.
     * @param evt LayerStatusEvent
     */
    public void fireStatusUpdate(int status) {
	fireStatusUpdate(new LayerStatusEvent(this, status));
    }

    /**
     * Repaint the layer.
     * You should not need to override this.
     */
    public void repaint() {
	if (AWTAvailable) {
	    super.repaint();
	} else {
	    // This looks like a fireStatusUpdate, right?  But that is
	    // disabled if !AWTAvailable.  The only way to fire the
	    // status is finished is by calling a repaint.  Doing
	    // anything else confuses the GIFMapBean.  The firing of
	    // this status update may be redundant for layers that use
	    // the status updates already, but we have to play smart
	    // for all layers, especially for those who don't play
	    // nice.
	    LayerStatusEvent evt = new LayerStatusEvent(this, LayerStatusEvent.FINISH_WORKING);
	    LayerStatusListener[] temp = getSynchronizedStatusListeners();
	    if (temp != null) {
		for (int i = 0; i < temp.length; i++) {
		    temp[i].updateLayerStatus(evt);
		}
	    }
	}
    }

    /**
     * Repaint the layer.
     * If you are using BufferedMapBean for your application,
     * WE STRONGLY RECOMMEND THAT YOU DO NOT OVERRIDE THIS METHOD.
     * This method marks the layer buffer so that it will be refreshed.
     * If you override this method, and don't call super.repaint(),
     * the layers will not be repainted.
     */
    public void repaint(long tm, int x, int y, int width, int height) {
        Component p = getParent();
	if (p instanceof MapBean) {
	    ((MapBean)p).setBufferDirty(true);
	    if (Debug.debugging("basic")) {
		Debug.output(getName() +"|Layer: repaint(tm=" + tm + 
				   ", x=" + x + 
				   ", y=" + y + 
				   ", width=" + width + 
				   ", height=" + height + ")");
	    }

	    // How dangerous is this?  Let the MapBean manage the
	    // repaint call?  Seems to work OK, and lets the buffered
	    // MapBeans work better when they are embedded in other
	    // components.  It's this call here that makes the
	    // BufferedLayer work right.
	    p.repaint();
	} else {
	    super.repaint(tm, x, y, width, height);
	}
    }

    /**
     * This method is here to provide a default action for Layers as
     * they act as a ProjectionPainter.  Normally, ProjectionPainters
     * are expected to receive the projection, gather/create
     * OMGraphics that apply to the projection, and render them into
     * the Graphics provided.  This is supposed to be done in the
     * same thread that calls this function, so the caller knows that
     * when this method returns, everything that the
     * ProjectionPainter needed to do is complete.<P> If the layer
     * doesn't override this method, then the paint(Graphics) method
     * will be called.
     *
     * @param proj Projection of the map.
     * @param g java.awt.Graphics to draw into.  
     */
    public void renderDataForProjection(Projection proj, Graphics g) {
	paint(g);
    }


    /**
     * This method is called when the layer is added to the MapBean
     * @param cont Container
     */
    public void added(Container cont) {}

    /**
     * This method is called after the layer is removed from the
     * MapBean and when the projection changes.  We recommend that
     * Layers override this method and nullify memory-intensive
     * variables.
     * @param cont Container
     */
    public void removed(Container cont) {}

    /**
     * Part of a layer hack to notify the component listener when the
     * component is hidden.  These components don't receive the
     * ComponentHidden notification.  Remove when it works.
     */
    protected Vector localHackList;

    /**
     * Part of a layer hack to notify the component listener when the
     * component is hidden.  These components don't receive the
     * ComponentHidden notification.  Remove when it works. Set to
     * false to test.
     */
    protected boolean doHack = true;

    /**
     * Part of a layer hack to notify the component listener when the
     * component is hidden.  These components don't receive the
     * ComponentHidden notification.  Remove when it works.
     */
    public void setVisible(boolean show) {
	super.setVisible(show);
	if (doHack && !show) {
	    notifyHideHack();
	}
    }	

    /**
     * Part of a layer hack to notify the component listener when the
     * component is hidden.  These components don't receive the
     * ComponentHidden notification.  Remove when it works.
     */
    public void addComponentListener(ComponentListener cl) {
	super.addComponentListener(cl);
	if (localHackList == null) {
	    localHackList = new Vector();
	}
	if (!localHackList.contains(cl)) {
	    localHackList.add(cl);
	}
    }

    /**
     * Part of a layer hack to notify the component listener when the
     * component is hidden.  These components don't receive the
     * ComponentHidden notification.  Remove when it works.
     */
    public void removeComponentListener(ComponentListener cl) {
	super.removeComponentListener(cl);
	if (localHackList != null) {
	    localHackList.remove(cl);
	}
    }

    /**
     * Part of a layer hack to notify the component listener when the
     * component is hidden.  These components don't receive the
     * ComponentHidden notification.  Remove when it works.
     */
    public void notifyHideHack() {
	java.util.Vector targets;
	synchronized (this) {
	    if (localHackList == null) {
	    	return;
	    }
	    targets = (java.util.Vector) localHackList.clone();
	}
	ComponentEvent ce = new ComponentEvent(this, ComponentEvent.COMPONENT_HIDDEN);
	for (int i = 0; i < targets.size(); i++) {
	    ComponentListener target = (ComponentListener)targets.elementAt(i);
	    target.componentHidden(ce);
	}
    }

    /**
     * Set whether the Layer should be added to the BeanContext.
     */
    public void setAddToBeanContext(boolean set) {
	addToBeanContext = set;
    }

    /**
     * Set whether the Layer should be added to the BeanContext.
     */
    public boolean getAddToBeanContext() {
	return addToBeanContext;
    }

    /**
     * Mark the layer as one that should be considered a background
     * layer.  What that means is up to the MapBean or application.
     */
    public void setAddAsBackground(boolean set) {
	addAsBackground = set;
    } 

    /**
     * Check to see if the layer is marked as one that should be
     * considered a background layer. What that means is up to the
     * MapBean or application.
     * @return true if layer is a background layer.
     */
    public boolean getAddAsBackground() {
	return addAsBackground;
    }
    
    /**
     * This is the method that your layer can use to find other
     * objects within the MapHandler (BeanContext).  This method gets
     * called when the Layer gets added to the MapHandler, or when
     * another object gets added to the MapHandler after the Layer is
     * a member.  If the LayerHandler creates the Layer from
     * properties, the LayerHandler will add the Layer to the
     * BeanContext if Layer.addToBeanContext is true.  It is false by
     * default.
     *
     * For Layers, this method doesn't do anything by default.  If you
     * need your layer to get ahold of another object, then you can
     * use the Iterator to go through the objects to look for the one
     * you need.
     */
    public void findAndInit(Iterator it) {
	while (it.hasNext()) {
	    findAndInit(it.next());
	}
    }

    /**
     * This method is called by the findAndInit(Iterator) method, once
     * for every object inside the iterator.  It's here to allow
     * subclasses a way to receive objects and still let the super
     * classes have a shot at the object.  So, you can override this
     * method can call super.findAndInit(obj), or override the
     * findAndInit(Iterator) method and call super.findAndInit(obj).
     * Whatever.
     */
    public void findAndInit(Object obj) {}

    /**
     * BeanContextMembershipListener method.  Called when a new object
     * is added to the BeanContext of this object.  
     */
    public void childrenAdded(BeanContextMembershipEvent bcme) {
	findAndInit(bcme.iterator());      
    }
    
    /**
     * BeanContextMembershipListener method.  Called when a new object
     * is removed from the BeanContext of this object.  For the Layer,
     * this method doesn't do anything.  If your layer does something
     * with the childrenAdded method, or findAndInit, you should take
     * steps in this method to unhook the layer from the object used
     * in those methods.
     */
    public void childrenRemoved(BeanContextMembershipEvent bcme) {
	Iterator it = bcme.iterator();
	while (it.hasNext()) {
	    findAndUndo(it.next());
	}
    }

    /**
     * This is the method that does the opposite as the
     * findAndInit(Object).  Lets you call super classes with objects
     * that need to be removed.
     */
    public void findAndUndo(Object obj) {}

    /** Method for BeanContextChild interface. */
    public BeanContext getBeanContext() {
	return beanContextChildSupport.getBeanContext();
    }
  
    /** Method for BeanContextChild interface. */
    public void setBeanContext(BeanContext in_bc) 
	throws PropertyVetoException {

	if (in_bc != null) {
	    in_bc.addBeanContextMembershipListener(this);
	    beanContextChildSupport.setBeanContext(in_bc);
	    findAndInit(in_bc.iterator());
	}
    }
  
    /**
     * Method for BeanContextChild interface.  Uses the
     * BeanContextChildSupport to add a listener to this object's
     * property.  This listener wants to have the right to veto a
     * property change.
     */
    public void addVetoableChangeListener(String propertyName,
					  VetoableChangeListener in_vcl) {
	beanContextChildSupport.addVetoableChangeListener(propertyName, in_vcl);
    }
  
    /**
     * Method for BeanContextChild interface.  Uses the
     * BeanContextChildSupport to remove a listener to this object's
     * property.  The listener has the power to veto property changes.
     */
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

    public void clearListeners() {
	if (localHackList != null) {
	    localHackList.clear();
	}
	if (IDListeners != null) {
	    IDListeners.clear();
	}
	if (lsListeners != null) {
	    lsListeners.clear();
	}

	BeanContext bc = getBeanContext();
	if (bc != null) {
	    bc.removeBeanContextMembershipListener(this);
	}
    }

    public void finalize() {
	if (Debug.debugging("gc")) {
	    Debug.output("Layer |" + getName() + " |: getting GC'd");
	}
    }

    /**
     * Fire a component event to the Layer component listeners, with
     * the palette as the component, letting them know if it's visible
     * or not.
     */
    public void firePaletteEvent(boolean isVisible) {
	java.util.Vector targets;
	synchronized (this) {
	    if (localHackList == null) {
	    	return;
	    }
	    targets = (java.util.Vector) localHackList.clone();
	}
	

	int eventType;

	if (palette == null) return;

	if (!palette.isVisible()) {
	    eventType = ComponentEvent.COMPONENT_HIDDEN;
	} else {
	    eventType = ComponentEvent.COMPONENT_SHOWN;
	}

	ComponentEvent ce = new ComponentEvent(palette, eventType);

	for (int i = 0; i < targets.size(); i++) {
	    ComponentListener target = (ComponentListener)targets.elementAt(i);
	    if (eventType == ComponentEvent.COMPONENT_HIDDEN) {
		target.componentHidden(ce);
	    } else if (eventType == ComponentEvent.COMPONENT_SHOWN) {
		target.componentShown(ce);
	    }
	}
    }

    /**
     * Return the JFrame, or JInternalFrame, that serves as the
     * palette for the layer.
     */
    public Container getPalette() {
	return palette;
    }

    /**
     * Called when something about the layer has changed that would
     * require the palette to be reconfigured.  Will cause getGUI() to
     * be called again.  You should take steps before calling this
     * method to make sure that the getGUI() method is ready to
     * recreate the palette components from scratch if needed.
     */
    protected void resetPalette() {
	java.awt.Container pal = getPalette();
	boolean putUp = false;
	if (pal != null && pal.isVisible()) {
	    putUp = true;
	    setPaletteVisible(false);
	}

	if (putUp) {
	    setPaletteVisible(true);
	}
    }

    /**
     * Make the palette visible or not, destroy if invisible.
     */
    public void setPaletteVisible(boolean visible) {
	if (visible) {
	    showPalette();
	} else {
	    hidePalette();
	    palette = null;
	}
    }

    /**
     * Make the palette visible.  Will automatically determine if
     * we're running in an applet environment and will use a
     * JInternalFrame over a JFrame if necessary.
     */
    public void showPalette() {
	if (Environment.getBoolean(Environment.UseInternalFrames)) {

	    final JLayeredPane desktop = 
		Environment.getInternalFrameDesktop();

	    // get the window
	    palette = PaletteHelper.getPaletteInternalWindow(this,
		  new InternalFrameAdapter() {
		      public void internalFrameClosed(InternalFrameEvent e) {

			  if (desktop != null) {
			      desktop.remove((JInternalFrame)palette);
			      desktop.repaint();
			  }
			  palette = null;
			  firePaletteEvent(false);
		      };
		  });
	    // add the window to the desktop
	    if (desktop != null) desktop.add(palette);
	    firePaletteEvent(true);
	    palette.setVisible(true);
	} else {
	    if (palette == null) {
		palette = PaletteHelper.getPaletteWindow(
		    this, new ComponentAdapter(){  
			    public void componentHidden(ComponentEvent e){
				firePaletteEvent(false);
			    };
			} );
	    }
	    palette.setVisible(true);
	    ((JFrame)palette).setState(java.awt.Frame.NORMAL);
	    firePaletteEvent(true);
	}
    }
    
    /**
     * Hide the layer's palette.
     */
    public void hidePalette() {
	if (palette == null) {
	    return;
	}

	if (Environment.getBoolean(Environment.UseInternalFrames)){
	    // close the palette
	    try { 
		((JInternalFrame)palette).setClosed(true); 
	    } catch (java.beans.PropertyVetoException evt) {
		com.bbn.openmap.util.Assert.assertExp(
		    false, "Layer.hidePalette(): internal error!");
	    }
	    palette.setVisible(false);

	} else {
	    palette.setVisible(false);
	}
    }

    /**
     * The default actionPerformed method for Layer.  Make sure you
     * call super.actionPerformed if you care about receiving palette
     * show/hide commands.  This method is also set up to receive the
     * DisplayPropertiesCmd, and will bring up the Inspector for the
     * layer.
     */
    public void actionPerformed(ActionEvent ae) {
	String command = ae.getActionCommand();
	if (command == DisplayPaletteCmd) {
	    if (Debug.debugging("layer")) {
		Debug.output(getName() + " displaying palette");
	    }
	    showPalette();
	} else if (command == HidePaletteCmd) {
	    hidePalette();
	} else if (command == DisplayPropertiesCmd) {
	    Inspector inspector = new Inspector();
	    inspector.inspectPropertyConsumer(this);
	} 
    }
}
