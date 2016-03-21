package painpoint.domain.painpoint;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import groovy.lang.Singleton;
import painpoint.Storage;
import painpoint.domain.painpoint.model.PainPoint;
import painpoint.domain.painpoint.model.PainPointFactory;
import painpoint.domain.util.DataModelUtil;

import java.net.ConnectException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Singleton
public class PainPointDomain {

    private static final String mTableName = "PainPoint";
    private static final String FIELDS = "ID, CLASSID, USERNAME, THUMBSDOWN";
    private final Storage mStorage = ServiceManager.getService(Storage.class);
    private boolean mNetworkError = false;
    private int mRetryCount = 0;
    private Map<Integer, PainPoint> mPainPointMapCache = null;

    public PainPointDomain() {
        createPainPointTable();
    }

    private Connection getConnection() {
        if(mNetworkError == false || mRetryCount > 10) {
            try {
                Class.forName("org.h2.Driver");
                mRetryCount = 0;
                return DriverManager.getConnection(mStorage.getH2Url(), "sa", "");
            } catch (SQLException sqlEx) {
                if (sqlEx.getErrorCode() == 1) {
                    mNetworkError = true;
                }
                PluginManager.getLogger().warn("SQLException " + sqlEx.getMessage());
            } catch (ClassNotFoundException cnfex) {
                PluginManager.getLogger().warn("ClassNotFoundException " + cnfex.getMessage());
            }
        }
        else {
            mRetryCount++;
        }
        return null;
    }

    private boolean hasPainPointForUser(Integer classId, String userName) {

        List<PainPoint> painPointsForClass = getPainPointsCacheForClassId(classId);
        for(PainPoint painPoint : painPointsForClass) {
            if(painPoint.getUserName().equalsIgnoreCase(userName)) {
                return true;
            }
        }

        return false;
    }

    // For testing purposes, early on.  Not "intended" for future use.
    private void deletePainPointTable() {
        try {
            Connection conn = getConnection();
            if (conn != null) {
                Statement stat = conn.createStatement();
                stat.execute("DROP TABLE " + mTableName);
                stat.close();
                conn.close();
            }
        }
        catch (SQLException ex) {
            PluginManager.getLogger().warn("deletePainPointTable SQLException " + ex.getMessage());
        }
    }

    private void createPainPointTable() {
        try {
            Connection conn = getConnection();
            if (conn != null) {
                Statement stat = conn.createStatement();
                stat.execute("CREATE TABLE " + mTableName + " (id INTEGER PRIMARY KEY, classid INT NOT NULL, username VARCHAR(256), thumbsdown BOOLEAN)");
                stat.close();
                conn.close();
            }
        }
        catch (SQLException ex) {
            PluginManager.getLogger().debug("SQLException " + ex.getMessage());
        }
    }

    public Map<Integer, PainPoint> getPainPointMap(boolean queryForData) throws SQLException {
        if(queryForData || mPainPointMapCache == null) {
            Map<Integer, PainPoint> painPointMap = null;
            Connection conn = getConnection();
            if (conn != null) {
                Statement stat = conn.createStatement();
                ResultSet resultSet = stat.executeQuery("SELECT * FROM " + mTableName);
                painPointMap = PainPointFactory.createPainPointMap(resultSet);
                if(painPointMap != null && !painPointMap.isEmpty()) {
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
                        "("+FIELDS+") " +
                        "VALUES(" + painPoint.toSQLValues() + ")";
                stat.execute(insertTableSQL);
                stat.close();
                conn.close();
                return true;
            }
        }
        catch (SQLException ex) {
            PluginManager.getLogger().warn("SQLException " + ex.getMessage());
        }
        return false;
    }

    public PainPoint getPainPointForId(boolean queryForData, Integer painPointId) {

        PainPoint painPoint = null;
        if(queryForData && mPainPointMapCache != null) {
            return mPainPointMapCache.get(painPointId);
        }
        else {
            if(queryForData) {
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
                    }
                    catch (SQLException ex) {
                        PluginManager.getLogger().warn("SQLException " + ex.getMessage());
                    }
                }
            }
        }
        return painPoint;
    }

    private List<PainPoint> getPainPointsCacheForClassId(Integer classId) {
        List<PainPoint> painPointList = new ArrayList<PainPoint>();
        if(mPainPointMapCache != null) {
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

        if(painPoint != null) {
            mPainPointMapCache.put(painPoint.getId(), painPoint);
        }

    }

    public List<PainPoint> getPainPointsForClassId(boolean queryForData, Integer classId) {

        List<PainPoint> painPointList = new ArrayList<PainPoint>();
        if(!queryForData && mPainPointMapCache != null) {
            painPointList = getPainPointsCacheForClassId(classId);
        }
        else {
            if(queryForData) {
                Map<Integer, PainPoint> painPointMap = null;
                Connection conn = getConnection();
                if (conn != null) {
                    try {
                        Statement stat = conn.createStatement();
                        ResultSet resultSet = stat.executeQuery("SELECT * FROM " + mTableName + " WHERE classid = " + classId);
                        painPointList = PainPointFactory.createPainPoints(resultSet);

                        PluginManager.getLogger().debug("getPainPointsForClassId size: " + painPointList.size());

                        stat.close();
                        conn.close();

                    }
                    catch (SQLException ex) {
                        PluginManager.getLogger().warn("SQLException " + ex.getMessage());
                    }
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
                String updateTableSQL = "UPDATE " + mTableName + " SET thumbsdown = " + painPoint.isThumbsDown() + " WHERE id = " +painPoint.getId();
                stat.execute(updateTableSQL);
                stat.close();
                conn.close();
                return true;
            }
        }
        catch (SQLException ex) {
            PluginManager.getLogger().warn("SQLException " + ex.getMessage());
        }
        return false;
    }


    public boolean addOrUpdateForClass(Integer classId, String userName, boolean painValue) {
        Integer painPointId = DataModelUtil.generatePainPointId(classId, userName);
        PainPoint painPoint = new PainPoint(painPointId, classId, userName, painValue);
        if(hasPainPointForUser(classId, userName)) {
            updatePainPoint(painPoint);
        }
        else {
            return insertPainPoint(painPoint);
        }
        return false;
    }
}
