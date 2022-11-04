//**********************************************************************
//
//<copyright>
//
//BBN Technologies
//10 Moulton Street
//Cambridge, MA 02138
//(617) 873-8000
//
//Copyright (C) BBNT Solutions LLC. All rights reserved.
//
//</copyright>
//**********************************************************************
//
//$Source:
///cvs/darwars/ambush/aar/src/com/bbn/ambush/mission/MissionHandler.java,v
//$
//$RCSfile: MissionHandler.java,v $
//$Revision: 1.10 $
//$Date: 2004/10/21 20:08:31 $
//$Author: dietrick $
//
//**********************************************************************

package com.bbn.openmap.dataAccess.mapTile;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import com.bbn.openmap.Environment;
import com.bbn.openmap.I18n;
import com.bbn.openmap.PropertyConsumer;
import com.bbn.openmap.image.BufferedImageHelper;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.util.PropUtils;
import com.bbn.openmap.util.cacheHandler.CacheObject;

/**
 * The TileMillMapTileFactory is an extension to the StandardMapTileFactory that can read image tiles stored in a
 * mbtiles file, which is the export format from the TileMill application. The mbtiles file is a sqlite database, so
 * this factory requires the sqlitejdbc package be used to read those data files. You can find that package at
 * http://www.zentus.com/sqlitejdbc.
 *
 * This component can be configured using properties: <p>
 *
 * <pre>
 * # Inherited from StandardMapTileFactory
 * rootDir=the jdbc driver/path string to use for the database file, "jdbc:sqlite:path to file"
 * cacheSize=the number of mapTiles the factory should hold on to. The default is 100.
 *
 * #optional:
 * # The class used to test for the existance of jdbc components.  Assumes sqlitejdbc, but the code fetching the tiles
 * # is pretty standard SQL - so you should be able to use any jdbc driver library.
 * testClass=org.sqlite.JDBC
 *
 * </pre>
 *
 * @author dietrick
 */
public class TileMillMapTileFactory
    extends StandardMapTileFactory
    implements MapTileFactory, PropertyConsumer {
    public final static String DEFAULT_TEST_CLASS = "org.sqlite.JDBC";
    public final static String TEST_CLASS_PROPERTY = "testClass";
    /**
     * Test class to use for existence of JDBC drivers.
     */
    protected String testClass = DEFAULT_TEST_CLASS;
    private boolean driverFound;
    private Connection conn;
    private Statement stat;

    public TileMillMapTileFactory() {
        this(null);
    }

    public TileMillMapTileFactory(String rootDir) {
        this.rootDir = rootDir;
        this.fileExt = ".png";
        verbose = logger.isLoggable(Level.FINE);
        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run(){
                if(conn!=null){
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        
        initDBConnection(rootDir);
    }

    /**
     * Fetches a new tile from the database.
     */
    public CacheObject load(Object key, int x, int y, int zoomLevel, Projection proj) {

        if(!driverFound){
            return null;
        }

        try {
         
            //"select zoom_level, tile_column, tile_row, tile_data from map, images where map.tile_id = images.tile_id";
            StringBuilder statement = new StringBuilder("select tile_data from map, images where");
            statement.append(" zoom_level = ").append(zoomLevel);
            statement.append(" and tile_column = ").append(x);
            statement.append(" and tile_row = ").append(Math.pow(2, zoomLevel) - y - 1);
            statement.append(" and map.tile_id = images.tile_id;");

            ResultSet rs = stat.executeQuery(statement.toString());
            while (rs.next()) {
                byte[] imageBytes = rs.getBytes("tile_data");

                boolean orig = ImageIO.getUseCache();
                ImageIO.setUseCache(false);
                ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
                BufferedImage bi = ImageIO.read(bis);
                ImageIO.setUseCache(orig);

                //BufferedImage bi = BufferedImageHelper.getBufferedImage(ii.getImage(), 0, 0, -1, -1);

                OMGraphic raster = createOMGraphicFromBufferedImage(bi, x, y, zoomLevel, proj);

                if (raster != null) {
                    return new CacheObject((String) key, raster);
                }

            }
            rs.close();
        } catch (Exception e) {
            logger.warning("something went wrong fetching image from database: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public Properties getProperties(Properties getList) {
        getList = super.getProperties(getList);
        if (testClass != null && !testClass.equals(DEFAULT_TEST_CLASS)) {
            getList.put(prefix + TEST_CLASS_PROPERTY, PropUtils.unnull(testClass));
        }
        return getList;
    }

    public Properties getPropertyInfo(Properties list) {
        list = super.getPropertyInfo(list);
        I18n i18n = Environment.getI18n();
        PropUtils.setI18NPropertyInfo(i18n, list, com.bbn.openmap.dataAccess.mapTile.StandardMapTileFactory.class,
            TEST_CLASS_PROPERTY, "JDBC Availability Test Class",
            "A class in the JDBC driver package to use to test for JDBC driver configuration (any class in package).",
            null);
        return list;
    }

    public void setProperties(String prefix, Properties setList) {
        super.setProperties(prefix, setList);
        prefix = PropUtils.getScopedPropertyPrefix(prefix);

        testClass = setList.getProperty(prefix + TEST_CLASS_PROPERTY, testClass);
        try {
            Class.forName(testClass);
            driverFound = true;
        } catch (Exception e) {
            logger.warning("can't locate sqlite JDBC components");
            driverFound = false;
        }
        
        if(!driverFound){
            return;
        }
        initDBConnection(rootDir);
    }
    
    private void initDBConnection(String root){
        if(root==null || root.length()==0){
            logger.warning("No rootDir has been defined");
            return;
        }
        try {
            conn = DriverManager.getConnection(root);
        } catch (SQLException e) {
            logger.warning("Unable to open connection with rootDir:"+root);
        }
        
        try{
            stat = conn.createStatement();
        }catch(SQLException e){
            logger.warning("Unable to create statemnt from connection");
        }
    }
}