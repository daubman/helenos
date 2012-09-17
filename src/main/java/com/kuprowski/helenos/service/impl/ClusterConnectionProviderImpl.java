package com.kuprowski.helenos.service.impl;

import com.kuprowski.helenos.ClusterConfiguration;
import com.kuprowski.helenos.context.PostConfiguringClusterListener;
import com.kuprowski.helenos.jdbc.core.support.ClusterConfigDao;
import com.kuprowski.helenos.service.ClusterConfigAware;
import com.kuprowski.helenos.service.ClusterConnectionProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.prettyprint.cassandra.connection.HConnectionManager;
import me.prettyprint.cassandra.service.CassandraHost;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * ********************************************************
 * Copyright: 2012 Tomek Kuprowski
 *
 * License: GPLv2: http://www.gnu.org/licences/gpl.html
 *
 * @author Tomek Kuprowski (tomekkuprowski at gmail dot com)
 * *******************************************************
 */
@Component("clusterConnectionProvider")
public class ClusterConnectionProviderImpl extends AbstractProvider implements ClusterConnectionProvider, ApplicationContextAware, ClusterConfigAware {

    @Autowired
    protected ClusterConfigDao clusterConfigDao;
    private ApplicationContext applicationContext;

    @Required
    public void setClusterConfigDao(ClusterConfigDao clusterConfigDao) {
        this.clusterConfigDao = clusterConfigDao;
    }

    @Override
    public List<ClusterConfiguration> loadAll() {
        return clusterConfigDao.loadAll();
    }

    @Override
    public void store(ClusterConfiguration configuration) {
        clusterConfigDao.store(configuration);
    }

    @Override
    public void activate(ClusterConfiguration configuration) {
        if (cluster != null) {
            cluster.getConnectionManager().shutdown();
            cluster = null;
        }
        if (!configuration.isActive()) {
            configuration.setActive(true);
            store(configuration);
        }
        PostConfiguringClusterListener.propagadeConfigChanges(applicationContext, configuration.createCluster());
    }

    @Override
    public void delete(String hosts) {
        clusterConfigDao.delete(hosts);
    }

    @Override
    public Map<String, Set<CassandraHost>> getConnectionStatus() {
        HConnectionManager connectionManager = cluster.getConnectionManager();

        Map<String, Set<CassandraHost>> ret = new HashMap<String, Set<CassandraHost>>(3);

        ret.put("LIVE", connectionManager.getHosts());
        ret.put("DOWN", connectionManager.getDownedHosts());
        ret.put("SUSPENDED", connectionManager.getSuspendedCassandraHosts());

        return ret;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}