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
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/plugin/BeanContextAbstractPlugIn.java,v $
// $RCSfile: BeanContextAbstractPlugIn.java,v $
// $Revision: 1.1.1.1 $
// $Date: 2003/02/14 21:35:49 $
// $Author: dietrick $
// 
// **********************************************************************


package com.bbn.openmap.plugin;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.beans.*;
import java.beans.beancontext.*;
import java.util.Iterator;
import java.util.Properties;

import com.bbn.openmap.Layer;
import com.bbn.openmap.PropertyConsumer;
import com.bbn.openmap.event.MapMouseListener;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.util.Debug;
import com.bbn.openmap.util.PropUtils;

/**
 * This class is an abstract implementation of the PlugIn.  It does
 * all the things the AbstractPlugIn does, but also carries the
 * support baggage for PlugIns that want to play in the BeanContext.
 * @see com.bbn.openmap.plugin.PlugInLayer
 * @see com.bbn.openmap.plugin.PlugIn 
 * @see com.bbn.openmap.MapHandler
 * @see com.bbn.openmap.MapHandlerChild
 */
public abstract class BeanContextAbstractPlugIn extends AbstractPlugIn
    implements BeanContextChild, BeanContextMembershipListener {

    public BeanContextAbstractPlugIn() {
	super();
    }

    public BeanContextAbstractPlugIn(Component comp) {
	super(comp);
    }

    /**
     * BeanContextChildSupport object provides helper functions for
     * BeanContextChild interface.
     */
    protected BeanContextChildSupport beanContextChildSupport = new BeanContextChildSupport();
    
    /**
     * This is the method that your object can use to find other
     * objects within the MapHandler (BeanContext).  This method gets
     * called when the object gets added to the MapHandler, or when
     * another object gets added to the MapHandler after the object is
     * a member.  
     *
     * @param it Iterator to use to go through a list of objects.
     * Find the ones you need, and hook yourself up.
     */
    public void findAndInit(Iterator it) {
	while (it.hasNext()) {
	    findAndInit((Object)it.next());
	}
    }

    /**
     * The findAndInit method has been made non-abstract, because it
     * now calls this method for every object that is in the iterator
     * it receives.  This lets subclasses call a method on super
     * classes so they can handle their needs as well.
     */
    public void findAndInit(Object obj) { }

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
     * The childrenRemoved has been changed to go through its iterator
     * to call this method with every object.  This lets subclasses
     * call this method on their super class, so it can handle what it
     * needs to with objects it may be interested in.
     */
    public void findAndUndo(Object obj) {}

    /** Method for BeanContextChild interface. */
    public BeanContext getBeanContext() {
	return beanContextChildSupport.getBeanContext();
    }
  
    /**
     * Method for BeanContextChild interface. Adds this object as a
     * BeanContextMembership listener, set the BeanContext in this
     * objects BeanContextSupport, and receives the initial list of
     * objects currently contained in the BeanContext.  
     */
    public void setBeanContext(BeanContext in_bc) 
	throws PropertyVetoException {

	if(in_bc != null) {
	    in_bc.addBeanContextMembershipListener(this);
	    beanContextChildSupport.setBeanContext(in_bc);
	    findAndInit(in_bc.iterator());
	}
    }
  
    /**
     * Method for BeanContextChild interface.  Uses the
     * BeanContextChildSupport to add a listener to this object's
     * property.  You don't need this function for objects that extend
     * java.awt.Component.
     */
    public void addPropertyChangeListener(String propertyName,
					  PropertyChangeListener in_pcl) {
	beanContextChildSupport.addPropertyChangeListener(propertyName, in_pcl);
    }

    /**
     * Method for BeanContextChild interface.  Uses the
     * BeanContextChildSupport to remove a listener to this object's
     * property.  You don't need this function for objects that extend
     * java.awt.Component.
     */
    public void removePropertyChangeListener(String propertyName, 
					     PropertyChangeListener in_pcl) {
	beanContextChildSupport.removePropertyChangeListener(propertyName, in_pcl);
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
     * Method for BeanContextChild interface.  Uses the
     * BeanContextChildSupport to fire a property change.
     * You don't need this function for objects that extend
     * java.awt.Component.
     */
    public void firePropertyChange(String name, 
				   Object oldValue, 
				   Object newValue) {
	beanContextChildSupport.firePropertyChange(name, oldValue, newValue);
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
	beanContextChildSupport.fireVetoableChange(name, oldValue, newValue);
    }
}
