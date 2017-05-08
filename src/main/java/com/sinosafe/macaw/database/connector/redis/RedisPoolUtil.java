package com.sinosafe.macaw.database.connector.redis;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

/**
 * @TODO 完成redis cluster模式下 连接池的初始化，对连接池进行管理
 * @author yusha
 *
 */
public class RedisPoolUtil {

	private static final Logger logger = LoggerFactory.getLogger(RedisPoolUtil.class);
	

	private static JedisCluster  jedisPool;
	
	private static final int DEFAULT_REDIRECTIONS = 5;

	public synchronized static void init(String redisHost, String passWord, int timeOut) {
		// 关闭之前的连接池
		if (jedisPool != null) {
			try {
				jedisPool.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.error("init jedispool err cause by ->", e);
			}
		}
		Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
		// 使用多节点sharding初始化数据
		String[] hostArray = redisHost.split(",");
		for (String host : hostArray) {
			String[] tmpArray = host.split(":");
			if (tmpArray.length != 2) {
				logger.error("node address error !", host.length() - 1);
			}
			HostAndPort jedisShardInfo = new HostAndPort(tmpArray[0], Integer.parseInt(tmpArray[1]));
			jedisClusterNodes.add(jedisShardInfo);
		}
		try {
			jedisPool = new JedisCluster(jedisClusterNodes, timeOut, timeOut, DEFAULT_REDIRECTIONS, passWord,
					getJedisPoolConfig());
			logger.debug("init jedisPool is ->"+jedisPool);
			if(null==jedisPool)
				logger.error("严重错误，初始化redis pool失败! by redisHost->"+redisHost+";passWord->"+passWord);
		} catch (Exception e) {
			logger.error("系统级严重错误 init redis pool error cause by ->", e);
			throw new RuntimeException(e);
		}

	}
	
	/**
	 * 获取cluster 连接实例
	 * @return
	 */
	public static JedisCluster getJedisPool() {
		return jedisPool;
	}

	public static GenericObjectPoolConfig getJedisPoolConfig() {

		GenericObjectPoolConfig config = new GenericObjectPoolConfig();

		// 用尽时的策略
		// config.setWhenExhaustedAction();
		config.setBlockWhenExhausted(true);
		// 当池内没有返回对象时，最大等待时间 10000ms
		config.setMaxWaitMillis(-1);

		config.setTimeBetweenEvictionRunsMillis(-1);
		// 上面设置-1以后,下面的设置时间应该没有用.
		config.setMinEvictableIdleTimeMillis(1000L * 60 * 2);

		config.setTestWhileIdle(true);
		
		config.setTestOnBorrow(true);
		
		config.setTestOnReturn(true);

		config.setNumTestsPerEvictionRun(-1);

		// 最大分配的对象数
		 

		// 最大能够保持idel状态的对象数
		config.setMaxIdle(64);
		config.setMinIdle(50);
		

		return config;
	}
}
