/**
 * 
 */
package it.geosolutions.utils.db;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.validation.InvalidArgumentException;
import org.apache.commons.cli2.validation.Validator;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
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
/**
 * @authors Fabiani, Ivano Picco
 * 
 */
public class Shp2Orcl extends BaseArgumentsManager {

    private static final int DEFAULT_ORACLE_PORT=1521;

   	private static final String VERSION = "0.3";
	private static final String NAME = "shp2Orcl";

  	private Option hostnameOpt;
	private Option portOpt;
	private Option databaseOpt;
	private Option schemaOpt;
	private Option userOpt;
	private Option passwordOpt;
    private Option shapefileOpt;

   	private static String hostname;
	private static Integer port;
	private static String database;
	private static String schema;
	private static String user;
	private static String password;
    private static String shapefile;


	/**
	 * Default constructor
	 */
	public Shp2Orcl()  {
		super(NAME, VERSION);

		// /////////////////////////////////////////////////////////////////////
		// Options for the command line
		// /////////////////////////////////////////////////////////////////////
		shapefileOpt = optionBuilder.withShortName("s").withLongName(
				"shapefile").withArgument(
				argumentBuilder.withName("filename").withMinimum(1)
						.withMaximum(1).create()).withDescription(
				"shapefile to import").withRequired(true)
				.create();
   		hostnameOpt = optionBuilder.withShortName("H").withLongName(
				"hostname").withArgument(
				argumentBuilder.withName("hostname").withMinimum(1)
						.withMaximum(1).create()).withDescription(
				"database host").withRequired(true)
				.create();
   		databaseOpt = optionBuilder.withShortName("d").withShortName("db").withLongName(
				"database").withArgument(
				argumentBuilder.withName("dbname").withMinimum(1)
						.withMaximum(1).create()).withDescription(
				"database name").withRequired(true)
				.create();
   		schemaOpt = optionBuilder.withShortName("S").withLongName(
				"schema").withArgument(
				argumentBuilder.withName("schema").withMinimum(1)
						.withMaximum(1).create()).withDescription(
				"database schema").withRequired(true)
				.create();
   		userOpt = optionBuilder.withShortName("u").withLongName(
				"user").withArgument(
				argumentBuilder.withName("username").withMinimum(1)
						.withMaximum(1).create()).withDescription(
				"username").withRequired(false)
				.create();
   		passwordOpt = optionBuilder.withShortName("p").withLongName(
				"password").withArgument(
				argumentBuilder.withName("password").withMinimum(1)
						.withMaximum(1).create()).withDescription(
				"password").withRequired(false)
				.create();

		portOpt = optionBuilder
				.withShortName("P")
				.withLongName("port")
				.withDescription("database port")
				.withArgument(
						argumentBuilder.withName("portnumber")
								.withMinimum(1).withMaximum(1).withValidator(
										new Validator() {

											public void validate(List args)
													throws InvalidArgumentException {
												final int size = args.size();
												if (size > 1)
													throw new InvalidArgumentException(
															"Only one port at a time can be defined");
												final String val = (String) args
														.get(0);

												final int value = Integer
														.parseInt(val);
												if (value <= 0 || value > 65536)
													throw new InvalidArgumentException(
															"Invalid port specification");

											}
										}).create()).withRequired(false)
				.create();

		addOption(shapefileOpt);
   		addOption(databaseOpt);
   		addOption(hostnameOpt);
   		addOption(portOpt);
   		addOption(schemaOpt);
   		addOption(userOpt);
   		addOption(passwordOpt);

		// /////////////////////////////////////////////////////////////////////
		//
		// Help Formatter
		//
		// /////////////////////////////////////////////////////////////////////
		finishInitialization();

	}

    @Override
    public boolean parseArgs(String[] args) {
        if (!super.parseArgs(args)) {
            return false;
        }
        shapefile = (String) getOptionValue(shapefileOpt);
        database = (String) getOptionValue(databaseOpt);
        port = hasOption(portOpt)?Integer.valueOf((String) getOptionValue(portOpt)):DEFAULT_ORACLE_PORT;
        schema = (String) getOptionValue(schemaOpt);
        user = hasOption(userOpt)?(String) getOptionValue(userOpt): null;
        password = hasOption(passwordOpt)?(String) getOptionValue(passwordOpt): null;
        hostname = (String) getOptionValue(hostnameOpt);


        
        return true;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        Shp2Orcl shp2orcl = new Shp2Orcl();
        if (!shp2orcl.parseArgs(args)) {
            System.exit(1);
        }                 
        try {
            initOrclMap();
            shp2orcl.importShp(new File(shapefile));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FactoryException e) {
            e.printStackTrace();
        } catch (TransformException e) {
            e.printStackTrace();
        } catch (IllegalAttributeException e) {
            e.printStackTrace();
        } catch (MismatchedDimensionException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }
    // ////////////////////////////////////////////////////////////////////////
    //
    // ////////////////////////////////////////////////////////////////////////

    private static Map<String, Serializable> orclMap = new HashMap<String, Serializable>();

    private static void initOrclMap () {
        orclMap.put(JDBCDataStoreFactory.DBTYPE.key, "Oracle");
        orclMap.put(JDBCDataStoreFactory.HOST.key, hostname);
        orclMap.put(JDBCDataStoreFactory.PORT.key, port);
        orclMap.put(JDBCDataStoreFactory.DATABASE.key, database);
        orclMap.put(JDBCDataStoreFactory.SCHEMA.key, schema);
        orclMap.put(JDBCDataStoreFactory.USER.key, user);
        orclMap.put(JDBCDataStoreFactory.PASSWD.key, password);
        orclMap.put(JDBCDataStoreFactory.MINCONN.key, 1);
        orclMap.put(JDBCDataStoreFactory.MAXCONN.key, 10);
        // orclMap.put(JDBCDataStoreFactory.NAMESPACE.key,
        // "http://www.fao.org/fi");
    }

    public void importShp(File shapeFile) throws IOException,
            IllegalAttributeException, FactoryException, MismatchedDimensionException, IndexOutOfBoundsException, TransformException {

        DataStore orclDataStore = aquireFactory(orclMap).createDataStore(orclMap);
        log("importing shapefile " + shapeFile);
        long startwork = System.currentTimeMillis();

        DataStore shpDataStore = new ShapefileDataStore(shapeFile.toURI().toURL());

        String ftName = shpDataStore.getTypeNames()[0]; // ShapefileDataStore
                                                        // will always return a
                                                        // single name.
        
        //ftName = "FIGIS_GIS." + ftName;
        /** Importing SHP Data to DB **/

        // create the schema for the new shape file
        try {
            // FTWrapper pgft = new FTWrapper(shpDataStore.getSchema(ftName));
            // pgft.setReplaceTypeName(tablename);
            orclDataStore.createSchema(shpDataStore.getSchema(ftName));
        } catch (Exception e) {
            e.printStackTrace();
            // Most probably the schema already exists in the DB
            log("Error while creating schema '" + ftName + "': " + e.getMessage());
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
        final MathTransform srcCRSToWGS84 = CRS.findMathTransform(sourceCRS != null ? sourceCRS : targetCRS, targetCRS, true);

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
                        /** get the original geometry and put it as is into the DB ... **/
                        Geometry defGeom = (Geometry) attribute;

                        /** if we need to reproject the geometry before inserting into the DB ... **/
                        if (!srcCRSToWGS84.isIdentity())
                            defGeom = JTS.transform((Geometry) attribute, srcCRSToWGS84);
                        
                        defGeom.setSRID(999999);
                        
                        feature.setAttribute(a, defGeom.buffer(0));
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

    /**
     * When loading from DTO use the params to locate factory.
     *
     * <p>
     * bleck
     * </p>
     *
     * @param params
     *
     * @return
     */
    public static DataStoreFactorySpi aquireFactory(Map params) {
        for (Iterator i = DataStoreFinder.getAvailableDataStores(); i.hasNext();) {
            DataStoreFactorySpi factory = (DataStoreFactorySpi) i.next();
            
            if (factory.canProcess(params)) {
                return factory;
            }
        }

        return null;
    }
    
    private void log(String msg) {
        System.out.println(getClass().getSimpleName() + ": " + msg);
    }
  }
