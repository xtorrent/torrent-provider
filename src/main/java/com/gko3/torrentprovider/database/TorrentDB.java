package com.gko3.torrentprovider.database;

import com.gko3.torrentprovider.bean.InfohashTorrentMap;
import com.gko3.torrentprovider.common.OrderedProperties;
import com.gko3.torrentprovider.common.TorrentProviderConfig;
import com.gko3.torrentprovider.core.TorrentInfo;
import com.gko3.torrentprovider.thrift.GeneralResponse;
import com.gko3.torrentprovider.thrift.InfohashTorrent;
import com.gko3.torrentprovider.thrift.TorrentStatus;
import com.google.common.base.Objects;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.log4j.Logger;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Created by hechaobin01 on 2015/7/3.
 */
public class TorrentDB {
    private static final Logger LOG = Logger.getLogger(TorrentDB.class);

    private final QueryRunner queryRunner = new QueryRunner();

    private static TorrentDB ourInstance = new TorrentDB();

    public static TorrentDB getInstance() {
        return ourInstance;
    }

    private TorrentDB() {
    }

    private DataSource dataSource;

    public void init() throws  Exception {
        initDataSource();
        // add a thread for clean overdue torrent
        Thread cleanThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        cleanOverdueTorrent();
                        Thread.sleep(3600000); // one hour
                    } catch (Exception e) {
                        LOG.warn("infohash torrent clean thread has been interrupt:" + e);
                    }
                }
            }
        });
        cleanThread.start();
    }

    private void initDataSource()
            throws InstantiationException, IllegalAccessException, ClassNotFoundException,
            InvocationTargetException, SQLException {
        dataSource = (DataSource) Class.forName("com.mchange.v2.c3p0.ComboPooledDataSource").newInstance();

        if (Objects.equal(dataSource.getClass().getSimpleName(), "ComboPooledDataSource")) {
            System.setProperty("com.mchange.v2.c3p0.management.ManagementCoordinator",
                    "com.mchange.v2.c3p0.management.NullManagementCoordinator");
        }

        OrderedProperties properties = new OrderedProperties();
        properties.put("jdbcUrl", TorrentProviderConfig.databaseJdbc());
        properties.put("user", TorrentProviderConfig.databaseUser());
        properties.put("password", TorrentProviderConfig.databasePasswd());
        properties.put("initialPoolSize", TorrentProviderConfig.initialPoolSize());
        properties.put("minPoolSize", TorrentProviderConfig.minPoolSize());
        properties.put("maxPoolSize", TorrentProviderConfig.maxPoolSize());
        properties.put("acquireIncrement", TorrentProviderConfig.acquireIncrement());
        properties.put("maxIdleTime", TorrentProviderConfig.maxIdleTime());
        properties.put("checkoutTimeout", TorrentProviderConfig.checkoutTimeout());
        properties.put("idleConnectionTestPeriod", TorrentProviderConfig.idleConnectionTestPeriod());

        BeanUtils.populate(dataSource, properties);
        Connection connection = dataSource.getConnection();
        DatabaseMetaData metaData = connection.getMetaData();
        connection.close();
        LOG.info("connected to " + metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion());
    }

    /**
     * get torrent by infohash
     * @param infohash
     * @return
     */
    public TorrentInfo getTorrentByInfohash(String infohash) throws SQLException {
        if (infohash.isEmpty() || infohash.length() != 40) {
            TorrentInfo torrentInfo = new TorrentInfo();
            torrentInfo.setInfohash(infohash);
            torrentInfo.setTorrentStatus(TorrentStatus.STATUS_ERROR);
            torrentInfo.setMessage("infohash is invalid!");
            return torrentInfo;
        }

        StringBuffer sqlStringBuffer = new StringBuffer();
        sqlStringBuffer.append("SELECT * FROM torrent_info WHERE infohash=?");
        Connection connection = null;
        TorrentInfo torrentInfo = null;
        try {
            connection = dataSource.getConnection();
            torrentInfo = queryRunner.query(connection, sqlStringBuffer.toString(),
                    new BeanHandler<TorrentInfo>(TorrentInfo.class),
                    infohash);
            if (torrentInfo == null) {
                torrentInfo = new TorrentInfo();
                torrentInfo.setInfohash(infohash);
                torrentInfo.setTorrentStatus(TorrentStatus.STATUS_ERROR);
                torrentInfo.setMessage("infohash not exist in torrent-provider!");
                LOG.info("infohash:" + infohash + " not exist in server!");
                return torrentInfo;
            }
            torrentInfo.setTorrentCode(torrentInfo.getTorrentCode());
            torrentInfo.setUpdateTime(System.currentTimeMillis());
            torrentInfo.setTorrentStatus(TorrentStatus.STATUS_OK);
            torrentInfo.setMessage("OK");
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                LOG.warn("close connection error:" + e);
            }
        }
        return torrentInfo;
    }

    public GeneralResponse uploadTorrent(InfohashTorrent info) {
        GeneralResponse response = new GeneralResponse();
        response.setRetCode(1);
        response.setMessage("FAIL");
        if (info.getInfohash().isEmpty() || info.getInfohash().length() != 40) {
            response.setMessage("param error!");
            return response;
        }
        long currentTime = System.currentTimeMillis() / 1000;
        String sql = new String();

        Connection connection = null;
        try {
            // check if this torrent has exist, if so, just delete it
            sql = "SELECT * from torrent_info where infohash=?";
            Object[] params = {info.getInfohash()};
            connection = dataSource.getConnection();
            TorrentInfo torrentInfo = queryRunner.query(connection, sql,
                    new BeanHandler<TorrentInfo>(TorrentInfo.class),
                    params);
            if (torrentInfo != null) {
                boolean isHaveCode = false;
                sql = "DELETE FROM torrent_info where infohash=?";
                int number = queryRunner.update(connection, sql, info.getInfohash());
                // do not check delete number, because this may be 0 1 2 or something else
                // delete it from map
                InfohashTorrentMap.getInstance().removeTorrent(info.getInfohash());
                LOG.info("delete old torrent, infohash:" + torrentInfo.getInfohash()
                        + ", createTime:" + torrentInfo.getCreateTime()
                        + ", source:" + torrentInfo.getSource() + ", database number:" + number);
            }

            // insert new
            sql = "INSERT INTO torrent_info set infohash=?, source=?, torrentCode=?, createTime=?";
            Object[] paramsInsert = {info.getInfohash(), info.getSource(), info.getTorrentZipCode(), currentTime};
            int number = queryRunner.update(connection, sql, paramsInsert);
            if (number != 1) {
                response.setMessage("upload torrent failed for inert error");
                LOG.warn("insert torrent failed, number:" + number);
                return response;
            }
            // put it into map
            InfohashTorrentMap.getInstance().putTorrent(info.getInfohash(), new TorrentInfo(info));
        } catch (SQLException e) {
            LOG.warn("sql error:" + e);
            response.setMessage("sql error:" + e);
            return response;
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                LOG.warn("close connection error:" + e);
            }
        }

        response.setRetCode(0);
        response.setMessage("OK");
        return response;
    }

    private void cleanOverdueTorrent() {
        long cleanTime = System.currentTimeMillis() / 1000;
        cleanTime -= TorrentProviderConfig.maxInfohashTorrentKeepTime() * 86400;

        Connection connection = null;
        try {
            String sql = new String("DELETE FROM torrent_info where createTime<?");
            connection = dataSource.getConnection();
            int number = queryRunner.update(connection, sql, cleanTime);
            LOG.info("clean " + number + " torrents for upload before " + cleanTime);
        } catch (SQLException e) {
            LOG.warn("sql error:" + e);
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                LOG.warn("close connection error:" + e);
            }
        }
    }

}
