package painpoint.domain.painpoint.model;

import com.intellij.ide.plugins.PluginManager;
import groovy.lang.Singleton;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Singleton
public class PainPointDomain {

    private static final String mTableName = "PainPoint";
    private static final String FIELDS = "ID, CLASSID, THUMBSUP, THUMBSDOWN";
    private Map<Integer, PainPoint> mPainPointMapCache = null;


    public PainPointDomain() {
        deletePainPointTable(); //TODO: don't do this dummy.
        createPainPointTable();
    }

    private Connection getConnection() {
        try {
            Class.forName("org.h2.Driver");
            return DriverManager.getConnection("jdbc:h2:tcp://localhost/~/test", "sa", "");
        }
        catch (SQLException sqlEx) {
            PluginManager.getLogger().warn("SQLException "+sqlEx.getMessage());
        }
        catch (ClassNotFoundException cnfex) {
            PluginManager.getLogger().warn("ClassNotFoundException " + cnfex.getMessage());
        }
        return null;
    }

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
                stat.execute("CREATE TABLE " + mTableName + " (id INT PRIMARY KEY AUTO_INCREMENT, classid INT, thumbsup BOOLEAN, thumbsdown BOOLEAN)");
                stat.close();
                conn.close();
            }
        }
        catch (SQLException ex) {
            PluginManager.getLogger().warn("SQLException " + ex.getMessage());
        }
    }

    public Map<Integer, PainPoint> getPainPointMap(boolean queryForData) throws SQLException {

        if(queryForData) {
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

    public void insertPainPoint(PainPoint painPoint) {
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
            }
        }
        catch (SQLException ex) {
            PluginManager.getLogger().warn("SQLException " + ex.getMessage());
        }
    }

    public PainPoint getPainPointForId(boolean queryForData, Integer painPointId) {

        PainPoint painPoint = null;
        if(queryForData && mPainPointMapCache != null) {
            return mPainPointMapCache.get(painPointId);
        }
        else {
            if(queryForData) {
                Map<Integer, PainPoint> painPointMap = null;
                Connection conn = getConnection();
                if (conn != null) {
                    try {
                        Statement stat = conn.createStatement();
                        ResultSet resultSet = stat.executeQuery("SELECT * FROM " + mTableName + " WHERE id = " + painPointId);
                        painPoint = PainPointFactory.createPainPoint(resultSet);
                        mPainPointMapCache.put(painPointId, painPoint);
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

        List<PainPoint> painPointList = new ArrayList<>();
        Iterator it = mPainPointMapCache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());

            PainPoint painPoint = (PainPoint)pair.getValue();
            if(painPoint.getClassId() == classId) {
                painPointList.add(painPoint);
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
        return painPointList;
    }

    public List<PainPoint> getPainPointsForClassId(boolean queryForData, Integer classId) {

        List<PainPoint> painPointList = new ArrayList<>();
        if(queryForData && mPainPointMapCache != null) {
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
}
