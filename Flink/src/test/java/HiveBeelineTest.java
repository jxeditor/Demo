import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Before;

import org.junit.Test;

/**
 * @Author: xs
 * @Date: 2019-12-13 14:37
 * @Description:
 */
public class HiveBeelineTest {
    private Connection conn;

    @Before
    public void iniCoon() throws Exception {
        System.setProperty("HADOOP_USER_NAME", "hdfs");
        String driverName = "org.apache.hive.jdbc.HiveDriver";
        Class.forName(driverName);
        conn = DriverManager.getConnection(//
                "jdbc:hive2://hadoop01:10000/default",//
                "hdfs",
                "hdfs"//
        );
    }


    @Test
    public void insert() throws Exception {
        PreparedStatement ppst = conn.prepareStatement("create table default.users(id int,name string,age int)");
        ppst.execute();
        ppst.close();
        conn.close();
    }


}
