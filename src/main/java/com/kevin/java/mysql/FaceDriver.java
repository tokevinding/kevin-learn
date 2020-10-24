package com.kevin.java.mysql;

import com.mysql.jdbc.Driver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author dinghaifeng
 * @date 2020-10-12 10:21:58
 * @desc
 *  1.了解为什么使用Class.forName("com.mysql.jdbc.Driver");加载驱动
 *  2.默认驱动加载机制
 */
public class FaceDriver {

    public static void main(String[] args) throws Exception {

    }


    /**
     * 通过System.setProperty加载驱动（见{@link com.mysql.jdbc.Driver}源码）
     * 不推荐！！！！
     * static {
     *         try {
     *             //注册驱动
     *             DriverManager.registerDriver(new Driver());
     *         } catch (SQLException var1) {
     *             throw new RuntimeException("Can't register driver!");
     *         }
     *     }
     */
    public static void newDriverLoad() throws SQLException {
        new Driver();
        System.out.println(getConnection());
    }

    /**
     * 通过System.setProperty加载驱动（见{@link java.sql.DriverManager}源码）
     */
    public static void systemPropertiesLoad() throws ClassNotFoundException {
        System.setProperty("jdbc.drivers", "com.mysql.jdbc.Driver");
        System.out.println(getConnection());
    }

    /**
     * 使用Class.forName加载驱动
     * 说明：在Class.forName加载完驱动类，开始执行静态初始化代码时，会自动新建一个Driver的对象
     *      ，并调用DriverManager.registerDriver把自己注册到DriverManager中去。用Class.forName
     *      也是为了注册这个目的.(直接把Driver对象new出来，也是可以连接的，但是浪费空间没必要)
     *
     * 附加：
     *      ps1: Class.forName(String) 与ClassLoader.loadClass(String)的区别
     *           Class.forName(String): 加载类，并且执行类初始化；可以通过Class.forName(String, boolean, ClassLoader)第二个参数来仅仅加载类不执行初始化；
     *           ClassLoader.loadClass(String): 仅仅加载类，不执行类初始化；
     *
     *      ps2: 有时会看到这种用法：
     *           Class.forName(“com.mysql.jdbc.Driver”).newInstance();
     *           这是没有必要的，正如前述，静态初始化已经new了一个Driver的对象，注册到DriverManager中去，在此再建立一个Driver对象则是完全没有必要的，浪费空间。
     *
     *      ps3: 结合ps1，Class.forName(“com.mysql.jdbc.Driver”);
     *           相当于：
     *           ClassLoader loader = Thread.currentThread().getContextClassLoader();
     *           Class cls = loader.loadClass(“com.mysql.jdbc.Driver”);
     *           cls.newInstance();
     *           这种方法的问题同ps2, 浪费了一个Driver对象；
     *
     *      ps4: 在java 6中，引入了service provider的概念，即可以在配置文件中配置service（可能是一个interface或者abstract class）的provider（即service的实现类）。配置路径是：/META-INF/services/下面。详细信息见：http://docs.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Service%20Provider
     *           而java.sql.DriverManager也添加了对此的支持，因此，在JDK6中，DriverManager的查找Driver的范围为：
     *           1）system property “jdbc.drivers” 中配置的Driver值；
     *           2）用户调用Class.forName()注册的Driver
     *           3）service provider配置文件java.sql.Driver中配置的Driver值。
     *           因此，在jdk6中，其实是可以不用调用Class.forName来加载mysql驱动的，因为mysql的驱动程序jar包中已经包含了java.sql.Driver配置文件，并在文件中添加了com.mysql.jdbc.Driver.但在JDK6之前版本，还是要调用这个方法。
     */
    public static void forNameLoad() throws ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver");
        System.out.println(getConnection());
    }

    /**
     * 此处不使用Class.forName("com.mysql.jdbc.Driver"); 同样会默认加载驱动，获取到连接
     * 具体源码见：{@link java.sql.DriverManager.loadInitialDrivers} （静态代码块调用的(私有)方法）
     * 注：JDBC为我们提供了DriverManager类。如果其他加载方式，DriverManager初始化中会通过ServiceLoader类，
     *    在我们classpath中jar（数据库驱动包）中查找，如存在META-INF/services/java.sql.Driver文件，则加载该文件中的驱动类。
     */
    public static void defaultLoad() {
        System.out.println(getConnection());
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/learn", "root", "root");
        } catch (SQLException e) {
            throw new RuntimeException("获取连接失败", e);
        }
    }


}
