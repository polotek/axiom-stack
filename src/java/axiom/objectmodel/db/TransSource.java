package axiom.objectmodel.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.TransFSDirectory;

import axiom.extensions.trans.TransactionException;
import axiom.framework.ErrorReporter;
import axiom.framework.core.Application;
import axiom.objectmodel.DatabaseException;
import axiom.objectmodel.dom.LuceneManager;
import axiom.util.ResourceProperties;

public class TransSource {

	public final static String TRANS_SQL_LUCENE = 
		"CREATE TABLE Lucene (" +
			"timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
			"valid SMALLINT NOT NULL," +
			"db_home VARCHAR(255) NOT NULL," +
			"segments BLOB NOT NULL," + 
            "version INT NOT NULL)";
		
    public final static String TRANS_SQL_IDGEN = 
        "CREATE TABLE IdGen (" +
			"id INT NOT NULL," +
            "cluster_host VARCHAR(10) NOT NULL," +
    		"PRIMARY KEY (id, cluster_host))";
    
    public final static String TRANS_SQL_PATHINDICES =
		"CREATE TABLE PathIndices (" +
			"id VARCHAR(20) NOT NULL," +
			"layer INT NOT NULL," +
			"path VARCHAR(255) NOT NULL," +
			"PRIMARY KEY (id, layer))";
    
    public final static String TRANS_SQL_INDEX = 
        "CREATE INDEX Paths ON PathIndices (path)";
    
    public static final String DEFAULT_DRIVER = "org.h2.Driver";
    public static final String DEFAULT_URL = "jdbc:h2:file:";
    public static final String TRANSACTIONS_DB_DIR = "TransactionsDB";
    public static final String TRANSACTIONS_DB_NAME = "Transactions";
	
    private Properties conProps;
    private ResourceProperties props;
    protected String url;
    private String driver;
    private String user;
    private String password;
    private long lastRead = 0L;
    private Application app;
    private String hostId;
    private boolean isDefaultDb = false;
    private static boolean isDriverInitialized = false;

    public TransSource(Application app, ResourceProperties props) throws Exception {
        this.app = app;
        this.props = props;
        init();
    }

    /**
     * Get a JDBC connection to the db source.
     *
     * @return a JDBC connection
     *
     * @throws ClassNotFoundException if the JDBC driver couldn't be loaded
     * @throws SQLException if the connection couldn't be created
     */
    public Connection getConnection() throws Exception {
        return getConnection(false);
    }

    public Connection getConnection(boolean autocommit) throws Exception {
        if (props.lastModified() > lastRead) {
            init();
        }
        Connection con = DriverManager.getConnection(url, conProps);
        con.setAutoCommit(autocommit); // allowing for jdbc transactionality
        return con;
    }
    
    public synchronized void changeConnectionParams() {
        
    }

    /**
     * Initialize the db source from the properties
     *
     * @throws ClassNotFoundException if the JDBC driver couldn't be loaded
     */
    private synchronized void init() throws Exception {
        lastRead = props.lastModified();
        // get JDBC URL and driver class name
        url = props.getProperty("url");
        driver = props.getProperty("driver");
        // sanity checks
        if (DEFAULT_DRIVER.equals(driver)) {
            driver = DEFAULT_DRIVER;
            this.isDefaultDb = true;
        }
        
        // test if driver class is available
        try {
            synchronized (this.getClass()) {
                if (!this.isDefaultDb || !isDriverInitialized) {
                    Class.forName(driver).newInstance();   
                    isDriverInitialized = true;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println(driver + ":  Cannot locate JDBC Driver!");
            System.exit(-1);
        }

        // set up driver connection properties
        conProps = new Properties();
        this.user = props.getProperty("user");
        if (this.user != null) {
            conProps.put("user", this.user);
        } 
        
        this.password = props.getProperty("password");
        if (this.password != null) {
            conProps.put("password", this.password);
        } 
        
        this.hostId = props.getProperty("hostId");
        // read any remaining extra properties to be passed to the driver
        for (Enumeration e = props.keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            // filter out properties we alread have
            if ("url".equalsIgnoreCase(key) ||
                    "driver".equalsIgnoreCase(key) ||
                    "user".equalsIgnoreCase(key) ||
                    "password".equalsIgnoreCase(key) ||
                    "hostId".equalsIgnoreCase(key)) {
                continue;
            }
            conProps.setProperty(key, props.getProperty(key));
        }
        
        if (this.isDefaultDb) {
            PreparedStatement ps = null;
            Connection con = null;
            boolean exception = false;
            try{
                con = getConnection();
            	ps = con.prepareStatement(TRANS_SQL_LUCENE);
                ps.execute();
                ps.close();
                ps = con.prepareStatement(TRANS_SQL_IDGEN);
                ps.execute();
                ps.close();
                ps = con.prepareStatement(TRANS_SQL_PATHINDICES);
                ps.execute();
                ps.close();
                ps = con.prepareStatement(TRANS_SQL_INDEX);
                ps.execute();
                ps.close();
                ps = null;
                con.commit();
            } catch (SQLException ignore) {
                // ignore b/c it means we have already created the db  
                exception = true;
            } catch (Exception ex) {
                exception = true;
                throw ex;
            } finally {
                if (ps != null) {
                    try { 
                        ps.close(); 
                    } catch (SQLException e) { 
                    }
                    ps = null;
                }
                if (con != null) {
                    try { 
                        if (exception) {
                            con.rollback();
                        }
                        con.close();
                    } catch (SQLException e) { 
                    }
                    con = null;
                }
            }
        }
    }

    public Application getApplication() {
        return app;
    }

    public String getDriverClass() {
        return this.driver;
    }

    public String getUrl() {
        return this.url;
    }

    public String getUser() {
        return this.user;
    }

    public String getPassword() {
        return this.password;
    }

    public String getHostId() {
        return this.hostId;
    }

    public boolean checkConnection(Connection conn) {
        boolean validConn = false;
        try {
            if (conn == null || conn.isClosed()) {
                validConn = false;
            } else {
                Statement stmt = null;
                try {
                    stmt = conn.createStatement();
                    stmt.execute("SELECT 1");
                    stmt.close();
                    stmt = null;
                    validConn = true;
                } catch (Exception e) {
                    validConn = false;
                    try { 
                        conn.close(); 
                    } catch (SQLException sqle) { 
                        app.logError(ErrorReporter.errorMsg(this.getClass(), "checkConnection"), sqle);
                    }
                } finally {
                    if (stmt != null) {
                        try { 
                            stmt.close(); 
                        } catch (SQLException sqle) {
                            app.logError(ErrorReporter.errorMsg(this.getClass(), "checkConnection"), sqle); 
                        }
                        stmt = null;
                    }
                }
            }
        } catch (Exception ex) {
            validConn = false;
        }
        return validConn;
    }
    
}