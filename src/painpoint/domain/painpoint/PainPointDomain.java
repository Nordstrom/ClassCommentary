package painpoint.domain.painpoint;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import groovy.lang.Singleton;
import painpoint.Storage;
import painpoint.domain.painpoint.model.PainPoint;
import painpoint.domain.painpoint.model.PainPointFactory;
import painpoint.domain.util.DataModelUtil;

import java.sql.*;
import java.util.*;

import org.h2.jdbcx.JdbcConnectionPool;

@Singleton
public class PainPointDomain {

    private static final String mTableName = "PainPoint";
    private static final String FIELDS = "ID, CLASSID, USERNAME, THUMBSDOWN";
    private final Storage mStorage = ServiceManager.getService(Storage.class);
    private boolean mNetworkError = false;
    private Map<Integer, PainPoint> mPainPointMapCache = new HashMap<Integer, PainPoint>();

    public PainPointDomain() {
        createPainPointTable();
    }

    private Connection getConnection() {
        try {
            JdbcConnectionPool cp = JdbcConnectionPool.
                    create(mStorage.getH2Url(), "sa", "");
            mNetworkError = false;
            return cp.getConnection();
        } catch (SQLException sqlEx) {
            if (sqlEx.getErrorCode() == 1) {
                mNetworkError = true;
            }
            PluginManager.getLogger().warn("SQLException " + sqlEx.getMessage());
        }
        return null;
    }

    private boolean hasPainPointForUser(Integer classId, String userName) {

        List<PainPoint> painPointsForClass = getPainPointsCacheForClassId(classId);
        for (PainPoint painPoint : painPointsForClass) {
            if (painPoint.getUserName().equalsIgnoreCase(userName)) {
                return true;
            }
        }

        return false;
    }

    // For testing purposes, early on.  Not "intended" for future use.
    private void deletePainPointTable() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    Connection conn = getConnection();
                    if (conn != null) {
                        Statement stat = conn.createStatement();
                        stat.execute("DROP TABLE " + mTableName);
                        stat.close();
                        conn.close();
                    }
                } catch (SQLException ex) {
                    PluginManager.getLogger().warn("deletePainPointTable SQLException " + ex.getMessage());
                }
            }
        });
    }

    private void createPainPointTable() {
        if (mNetworkError) {
            PluginManager.getLogger().debug("getPainPointMap  returning null.  Database may be down.");
            return;
        }
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    Connection conn = getConnection();
                    if (conn != null) {
                        Statement stat = conn.createStatement();
                        stat.execute("CREATE TABLE " + mTableName + " (id INTEGER PRIMARY KEY, classid INT NOT NULL, username VARCHAR(256), thumbsdown BOOLEAN)");
                        stat.close();
                        conn.close();
                    }
                } catch (SQLException ex) {
                    PluginManager.getLogger().debug("SQLException " + ex.getMessage());
                }
            }
        });
    }

    public Map<Integer, PainPoint> getPainPointMap(boolean queryForData) throws SQLException {

        if (mNetworkError) {
            PluginManager.getLogger().debug("getPainPointMap  returning null.  Database may be down.");
            return null;
        }

        if (queryForData || mPainPointMapCache == null || mPainPointMapCache.isEmpty()) {
            Map<Integer, PainPoint> painPointMap = null;
            Connection conn = getConnection();
            if (conn != null) {
                Statement stat = conn.createStatement();
                ResultSet resultSet = stat.executeQuery("SELECT * FROM " + mTableName);
                painPointMap = PainPointFactory.createPainPointMap(resultSet);
                if (painPointMap != null && !painPointMap.isEmpty()) {
                    mPainPointMapCache = painPointMap;
                }
                stat.close();
                conn.close();
            }
        }
        return mPainPointMapCache;
    }

    public boolean insertPainPoint(PainPoint painPoint) {
        try {
            Connection conn = getConnection();
            if (conn != null) {
                Statement stat = conn.createStatement();
                String insertTableSQL = "INSERT INTO " + mTableName +
                        "(" + FIELDS + ") " +
                        "VALUES(" + painPoint.toSQLValues() + ")";
                stat.execute(insertTableSQL);
                stat.close();
                conn.close();
                return true;
            }
        } catch (SQLException ex) {
            PluginManager.getLogger().warn("SQLException " + ex.getMessage());
        }
        return false;
    }

    public PainPoint getPainPointForId(boolean queryForData, Integer painPointId) {

        PainPoint painPoint = null;
        if (queryForData && !mPainPointMapCache.isEmpty()) {
            return mPainPointMapCache.get(painPointId);
        } else {
            if (queryForData) {
                Connection conn = getConnection();
                if (conn != null) {
                    try {
                        Statement stat = conn.createStatement();
                        ResultSet resultSet = stat.executeQuery("SELECT * FROM " + mTableName + " WHERE id = " + painPointId);
                        painPoint = PainPointFactory.createPainPoint(resultSet);
                        addPainPoint(painPoint);
                        PluginManager.getLogger().debug("getPainPointForId size: " + painPoint);

                        stat.close();
                        conn.close();
                    } catch (SQLException ex) {
                        PluginManager.getLogger().warn("SQLException " + ex.getMessage());
                    }
                }
            }
        }
        return painPoint;
    }

    private List<PainPoint> getPainPointsCacheForClassId(Integer classId) {
        List<PainPoint> painPointList = new ArrayList<PainPoint>();
        if (!mPainPointMapCache.isEmpty()) {
            Iterator it = mPainPointMapCache.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                System.out.println(pair.getKey() + " = " + pair.getValue());

                PainPoint painPoint = (PainPoint) pair.getValue();
                if (painPoint.getClassId().equals(classId)) {
                    painPointList.add(painPoint);
                }
                it.remove(); // avoids a ConcurrentModificationException
            }
        }
        return painPointList;
    }

    private void addPainPoint(PainPoint painPoint) {

        if (painPoint != null) {
            mPainPointMapCache.put(painPoint.getId(), painPoint);
        }

    }

    public List<PainPoint> getPainPointsForClassId(boolean queryForData, Integer classId) {

        List<PainPoint> painPointList = new ArrayList<PainPoint>();
        if (!queryForData && !mPainPointMapCache.isEmpty()) {
            painPointList = getPainPointsCacheForClassId(classId);
        } else {
            Connection conn = getConnection();
            if (conn != null) {
                try {
                    Statement stat = conn.createStatement();
                    ResultSet resultSet = stat.executeQuery("SELECT * FROM " + mTableName + " WHERE classid = " + classId);
                    painPointList = PainPointFactory.createPainPoints(resultSet);

                    PluginManager.getLogger().debug("getPainPointsForClassId size: " + painPointList.size());

                    stat.close();
                    conn.close();

                } catch (SQLException ex) {
                    PluginManager.getLogger().warn("SQLException " + ex.getMessage());
                }
            }
        }
        return painPointList;
    }

    private boolean updatePainPoint(PainPoint painPoint) {
        try {
            Connection conn = getConnection();
            if (conn != null) {
                Statement stat = conn.createStatement();
                String updateTableSQL = "UPDATE " + mTableName + " SET thumbsdown = " + painPoint.isThumbsDown() + " WHERE id = " + painPoint.getId();
                stat.execute(updateTableSQL);
                stat.close();
                conn.close();
                return true;
            }
        } catch (SQLException ex) {
            PluginManager.getLogger().warn("SQLException " + ex.getMessage());
        }
        return false;
    }


    public boolean addOrUpdateForClass(Integer classId, String userName, boolean painValue) {
        Integer painPointId = DataModelUtil.generatePainPointId(classId, userName);
        PainPoint painPoint = new PainPoint(painPointId, classId, userName, painValue);
        if (hasPainPointForUser(classId, userName)) {
            return updatePainPoint(painPoint);
        } else {
            return insertPainPoint(painPoint);
        }
    }
}
