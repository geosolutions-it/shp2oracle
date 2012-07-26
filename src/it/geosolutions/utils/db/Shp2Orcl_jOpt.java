/**
 * 
 */
package it.geosolutions.utils.db;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.oracle.OracleNGDataStoreFactory;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.referencing.CRS;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import java.io.File;
import static java.io.File.*;
import static java.util.Arrays.*;
/**
 * @author Fabiani
 * 
 */
public class Shp2Orcl_jOpt {

    static int OraclePort=1521;
    static OptionSet options = null;

    /**
     * @param args
     */
    public static void main(String[] args) {
        Shp2Orcl_jOpt shp2orcl = new Shp2Orcl_jOpt();
        try {
            options = argsParser(args);

            if (options != null) {
                initOrclMap(options);
                try {
                    //shp2orcl.importShp(new File("C:/Work/data/FAO/FIGIS/shps/states.shp"));
                    //shp2orcl.importShp(new File("C:/Work/FIGIS/GIS_Data_Oracle_GS/FAO_AREAS/FAO_DIV_1.shp"));
                    shp2orcl.importShp((File) options.valueOf("shapefile"));
                } catch (IllegalAttributeException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (FactoryException e) {
                    e.printStackTrace();
                } catch (MismatchedDimensionException e) {
                    e.printStackTrace();
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                } catch (TransformException e) {
                    e.printStackTrace();
                }
            }
        } catch (OptionException ex) {
						/*TODO*/
        } catch (IOException ex) {
						/*TODO*/
        }


    }
    // ////////////////////////////////////////////////////////////////////////
    //
    // ////////////////////////////////////////////////////////////////////////

    static OracleNGDataStoreFactory factory = new OracleNGDataStoreFactory();

    private static Map<String, Comparable> orclMap = new HashMap<String, Comparable>();

    private static void initOrclMap (OptionSet options) {
        orclMap.put(JDBCDataStoreFactory.DBTYPE.key, "oracle");
        orclMap.put(JDBCDataStoreFactory.HOST.key, options.valueOf("hostname").toString());
        orclMap.put(JDBCDataStoreFactory.PORT.key, options.hasArgument("Port")?Integer.valueOf(options.valueOf("Port").toString()):OraclePort);
        orclMap.put(JDBCDataStoreFactory.DATABASE.key, options.valueOf("database").toString());
        orclMap.put(JDBCDataStoreFactory.SCHEMA.key, options.valueOf("schema").toString());
        orclMap.put(JDBCDataStoreFactory.USER.key, options.hasArgument("user")?options.valueOf("user").toString():null);
        orclMap.put(JDBCDataStoreFactory.PASSWD.key, options.hasArgument("password")?options.valueOf("password").toString():null);
        // orclMap.put(JDBCDataStoreFactory.NAMESPACE.key,
        // "http://www.fao.org/fi");
    }

    public static OptionSet argsParser(String[] args) throws OptionException, IOException {

        OptionParser parser = new OptionParser() {

            {
                acceptsAll(asList("v", "verbose"), "be more verbose");
                acceptsAll(asList("h", "?", "help"), "show help");
                acceptsAll(asList("s", "shapefile"), "shapefile").withRequiredArg().ofType(File.class).describedAs("shapefile");
                acceptsAll(asList("H", "hostname"), "Database server hostname or ip address").withRequiredArg().describedAs("hostname");
                acceptsAll(asList("p", "port"), "port number(default 1521)").withRequiredArg().ofType(Integer.class).describedAs("number");
                acceptsAll(asList("d", "db", "database"), "database name").withRequiredArg().describedAs("dbname");
                acceptsAll(asList("S", "schema"), "schema").withRequiredArg().describedAs("schema");
                acceptsAll(asList("u", "user"), "username").withRequiredArg().describedAs("username");
                acceptsAll(asList("p", "pwd", "password"), "password string").withRequiredArg().describedAs("password");
            }
        };
        parser.posixlyCorrect(true);
        if (args.length == 0) {
            System.err.println("Missing argument:");
            parser.printHelpOn(System.err);
            return null;
        }
        OptionSet options = null;
        try {
            options = parser.parse(args);
        } catch (OptionException e) {
            parser.printHelpOn(System.err);
            return null;
        }
        if (options.has("?")) {
            parser.printHelpOn(System.err);
            return null;
        }

        //controllo dei parametri necessari
        if (!options.has("shapefile") ||
            !options.has("hostname") ||
            !options.has("database") ||
            !options.has("schema"))
        {
            System.err.println("Missing argument:");
            parser.printHelpOn(System.err);
            return null;
        }

        //controllo dei parametri opzionali
//       if (!options.has("port") ||
//            !options.has("user") ||
//            !options.has("password"))
//        {
//            System.err.println("Missing argument:");
//            parser.printHelpOn(System.err);
//            return null;
//        }

        return options;
    }


    public void importShp(File shapeFile) throws IOException,
            IllegalAttributeException, FactoryException, MismatchedDimensionException, IndexOutOfBoundsException, TransformException {

        DataStore orclDataStore = factory.createDataStore(orclMap);
        log("importing shapefile " + shapeFile);
        long startwork = System.currentTimeMillis();

        DataStore shpDataStore = new ShapefileDataStore(shapeFile.toURI()
                .toURL());

        String ftName = shpDataStore.getTypeNames()[0]; // ShapefileDataStore
                                                        // will always return a
                                                        // single name.
        /** Importing SHP Data to DB **/

        // create the schema for the new shape file
        try {
            // FTWrapper pgft = new FTWrapper(shpDataStore.getSchema(ftName));
            // pgft.setReplaceTypeName(tablename);
            orclDataStore.createSchema(shpDataStore.getSchema(ftName));
        } catch (Exception e) {
            e.printStackTrace();
            // Most probably the schema already exists in the DB
            log("Error while creating schema '" + ftName + "': "
                    + e.getMessage());
            log("Will try to load featuretypes bypassing the error.");

            // orclDataStore.updateSchema(typeNames[t],
            // dataStore.getSchema(typeNames[t]));
        }

        // get a feature writer
        FeatureWriter<?, SimpleFeature> fw = orclDataStore.getFeatureWriter(ftName.toUpperCase(), Transaction.AUTO_COMMIT);

        // /////////////////////////////////////////////////////////////////////
        //
        // create the features
        //
        // /////////////////////////////////////////////////////////////////////
        SimpleFeature feature = null;

        final FeatureSource<?, ?> source = shpDataStore.getFeatureSource(ftName);
        final FeatureCollection<?, ?> fsShape = source.getFeatures();
        final FeatureIterator<?> fr = fsShape.features();

        final int size = fsShape.size();

        final CoordinateReferenceSystem sourceCRS = source.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem();
        final CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326", true);
        final MathTransform srcCRSToWGS84 = CRS.findMathTransform(sourceCRS, targetCRS, true);

        int cnt = 0;
        while (fr.hasNext()) {
            SimpleFeature srcFeature = (SimpleFeature) fr.next();
            
            // avoid illegal state
            fw.hasNext();
            feature = fw.next();

            if (cnt % 50 == 0)
                log("inserting ft #" + cnt + "/" + size + " in " + ftName);

            if (srcFeature != null) {
                int attr = srcFeature.getAttributeCount();
                for (int a = 0; a < attr; a++) {
                    Object attribute = srcFeature.getAttribute(a);
                    if (attribute instanceof Geometry) {
                    	  /** if we need to reproject the geometry before inserting into the DB ... **/
                        //Geometry defGeom = JTS.transform((Geometry) attribute, srcCRSToWGS84);
                        //defGeom.setSRID(4326);
                        
                        /** get the original geometry and put it as is into the DB ... **/
                        Geometry defGeom = (Geometry) attribute;
                        
                        feature.setAttribute(a, defGeom);
                    } else {
                        feature.setAttribute(a, attribute);
                    }
                }
                fw.write();
                cnt++;
            }
        }
        fr.close();

        try {
            fw.close();
        } catch (Exception whatever) {
            // amen
        }

        /** Importing SHP Data to DB - END **/
        long endwork = System.currentTimeMillis();

        log(" *** Inserted " + cnt + " features in " + ftName + " in " + (endwork - startwork) + "ms");
    }

    private void log(String msg) {
        System.out.println(getClass().getSimpleName() + ": " + msg);
    }
}
