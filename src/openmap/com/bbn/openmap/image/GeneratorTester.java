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
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/image/GeneratorTester.java,v $
// $RCSfile: GeneratorTester.java,v $
// $Revision: 1.1.1.1 $
// $Date: 2003/02/14 21:35:48 $
// $Author: dietrick $
// 
// **********************************************************************


package com.bbn.openmap.image;

import java.awt.*;
import java.util.*;
import java.io.*;
import java.net.*;

import javax.swing.JComponent;
import javax.swing.JFrame;

import com.bbn.openmap.*;
import com.bbn.openmap.proj.*;
import com.bbn.openmap.event.*;
import com.bbn.openmap.util.Debug;
import com.bbn.openmap.layer.shape.ShapeLayer;
import com.bbn.openmap.layer.GraticuleLayer;
import com.bbn.openmap.layer.dted.DTEDLayer;

/**
 * A test class to play around with the image server. 
 * @deprecated uses ImageServer methods no longer supported.
 */
public class GeneratorTester implements ImageReceiver {

    ImageServer server;
    String propertiesURLString = null;
    String fileName = null;
    Properties props = null;

    public GeneratorTester(String[] args) {

	props = System.getProperties();
	Debug.init(props);
	parseArgs(args);
	try {
	    URL propsURL = new URL(propertiesURLString);
	    Environment.init(props);
	    loadProperties(propsURL, props);
	} catch (MalformedURLException mul) {
	    System.out.println("GeneratorTester: Malformed URL");
	    return;
	}

	Projection proj = ProjectionFactory.makeProjection(CADRG.CADRGType,
							   42.0f, -72.0f,
							   5000000f, 
							   500, 500);

	server = new ImageServer(props);

//  	server.createImage(proj, this);
    }

    public void receiveImageData(byte[] bytes) {
	try {
	    File f = new File(fileName);
	    OutputStream writef = new FileOutputStream(f);
	    writef.write(bytes);
	    writef.close();
	    System.out.println(fileName + " JPEG written.");
	} catch (IOException ioe) {
	    System.out.println("No JPEG written due to IOExeception.");
	}
	System.exit(0);
    }

    /**
     * Test the image generator.
     *
     */
    public static void main(String[] args) {
	GeneratorTester gen = new GeneratorTester(args);
    }

    /**
     * Loads properties from a java resource.  This will load the
     * named resource identifier into the given properties instance.
     *
     * @param properties the Properties instance to receive the properties
     * @param resourceName the name of the resource to load
     * @param verbose indicates whether status messages should be printed
     */
    public boolean loadProperties(URL url, Properties props) {
	try {
	    InputStream propsIn = url.openStream();
	    props.load(propsIn);
	    return true;
	} catch (java.io.IOException e) {
	    return false;
	}		
    }

    /**
     */
    public void parseArgs(String[] args) {
	for (int i = 0; i < args.length; i++) {
	    if (args[i].equalsIgnoreCase("-url")) {
		propertiesURLString = args[++i];
	    } else if (args[i].equalsIgnoreCase("-outputFile")) {
		fileName = args[++i];
	    } else if (args[i].equalsIgnoreCase("-h")) {
		printHelp();
	    }
	}
	
	if (propertiesURLString == null || fileName == null) {
	    printHelp();
	}
    }

    /** 
     * <b>printHelp</b> should print a usage statement which reflects the
     * command line needs of the tester.
     */
    public void printHelp() {
	System.err.println("usage: java GeneratorTester -url <URL for properties file> -outputFile <path to output file>");
	System.exit(1);
    }


}
