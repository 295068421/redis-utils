package com.sinosafe.macaw.database.connector.redis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

import redis.clients.jedis.JedisCluster;

/**
 * TODO redis 访问客户端方法工具类 方法汇总
 * @author yusha
 *
 */
public class RedisClientUtil {

	private static final Logger logger = LoggerFactory.getLogger(RedisClientUtil.class);

	private volatile static RedisClientUtil instance;

	public static RedisClientUtil getInstance() {
		if (null == instance) {
			synchronized (RedisClientUtil.class) {
				if(null==instance){
					instance = new RedisClientUtil();
				}
			}
		}
		return instance;
	}

	private RedisClientUtil() {
	}

	/**
	 * 将需要存放到redis中。 时间复杂度：O(1)
	 * 
	 * @param key
	 *            存放到redis的Key值，唯一，不唯一会被覆盖
	 * @param seconds
	 *            超时时间，单位：秒，如果不需要设置超时时间直接给予默认值：0
	 * @param value
	 *            需要存放到redis的值
	 * @return 相应结果
	 */

	public boolean set(String key, int seconds, String value) {
		logger.debug("RedisClientUtil.set() key:" + key + ",seconds:" + seconds + ",value:" + value);
		long startTime = System.currentTimeMillis();
		String result = null;
		boolean isFlag = false;
		// 初始化redis对象
		JedisCluster jedis = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			result = jedis.set(key, value);
			// 如果超时时间大于零，则需要设置超时时长，否则永久存在
			if (seconds > 0) {
				jedis.expire(key, seconds);
			}
			if (!StringUtils.isEmpty(result) && "OK".equals(result.toUpperCase(Locale.getDefault()))) {
				logger.debug("RedisClientUtil.set() key:" + key + ",存放到redis的成功。");
				isFlag = true;
			} else {
				logger.warn("RedisClientUtil.set() key:" + key + ",存放到redis的失败。");
			}
		} catch (Exception ex) {
			logger.error("RedisClientUtil.set() is failed.Exception:", ex);

		}
		logger.debug("RedisClientUtil.set() key：" + key + ",value：" + value + "，所消耗的时间："
				+ (System.currentTimeMillis() - startTime) + "ms");
		return isFlag;
	}

	/**
	 * 通过唯一Key值从redis里面获取值。 时间复杂度：O(1)
	 * 
	 * @param key
	 *            redis唯一的key值
	 * @return 相应查询结果
	 */

	public String get(String key) {
		logger.debug("RedisClientUtil.get() key:" + key);
		long startTime = System.currentTimeMillis();
		// 初始化redis对象
		JedisCluster jedis = null;
		String redisValue = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			redisValue = jedis.get(key);
			logger.debug("RedisClientUtil.get() key:" + key + ",从redis获取的value：" + redisValue);
		} catch (Exception ex) {
			logger.error("RedisClientUtil.get() is failed. Exception:", ex);
			throw ex;
		}
		logger.debug("RedisClientUtil.get() key：" + key + "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
		return redisValue;
	}

	/**
	 * 将对象设置到redis服务器中，例如可以存入DTO或是Map对象。 时间复杂度：O(1)
	 * 
	 * @param key
	 *            存放到redis的Key值，唯一，不唯一会被覆盖
	 * @param objValue
	 *            需要缓存到redis的对象
	 * @param seconds
	 *            缓存的时间，单位秒,如果不需要设置超时时间直接给予默认值：0
	 */
	public boolean setMapOnRedis(String key, Map<String, String> redisMap, int seconds) {
		if (null == redisMap) {
			logger.warn(
					"RedisClientUtil.setMapOnRedis() key：" + key + "，redisMap is null" + "，需要缓存到redis的map为空，load到redis中。");
			return false;
		}
		String redisStr = redisMap.toString();
		if (redisStr.length() > 500) {
			redisStr = redisStr.substring(0, 500) + "....";
		}
		long startTime = System.currentTimeMillis();
		boolean returnReslut = false;
		String result = "";
		JedisCluster jedis = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			// 如果设置的超时时间为0时，说明是持久化到redis中，需要自己手动回收，如果非0则到达时间后就会销毁回收
			if (0 == seconds) {
				result = jedis.hmset(key, redisMap);
			} else {
				result = jedis.hmset(key, redisMap);
				// 设置超时时间
				jedis.expire(key, seconds);
			}
			// 设置到
			if (!StringUtils.isEmpty(result) && "OK".equals(result.toUpperCase(Locale.getDefault()))) {
				logger.debug("RedisClientUtil.setMapOnRedis() 将redisMap:" + JSONObject.toJSONString(redisMap) + "，key："
						+ key + ",设置到redis服务器中成功。");
				returnReslut = true;
			} else {
				logger.debug("RedisClientUtil.setMapOnRedis() 将objValue:" + JSONObject.toJSONString(redisMap) + "，key："
						+ key + "，设置到redis服务器中失败。");
			}
		} catch (Exception e) {
			logger.error("RedisClientUtil.setMapOnRedis() is failed. Exception:", e);
			throw e;
		}
		logger.debug("RedisClientUtil.setMapOnRedis() key：" + key + ",redisMap：" + JSONObject.toJSONString(redisMap)
				+ "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
		return returnReslut;
	}

	/**
	 * 将对象从redis服务器中获取； 时间复杂度：O(N)， N 为哈希表的大小。
	 * 
	 * 注：当存放map的size越大的时候，不建议使用该方法，会影响到性能，如果size比较小也就是小于10的话，可以使用该方法。
	 * 
	 * @param key
	 *            存放到redis中的唯一key值
	 * @return 获取的对象信息
	 * @throws PafaDAOException
	 */

	public Map<String, String> getRedisMap(String key) {
		long startTime = System.currentTimeMillis();
		logger.debug("RedisClientUtil.getRedisMap() key:" + key);
		Map<String, String> redisMap = null;
		JedisCluster jedis = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			redisMap = jedis.hgetAll(key);
			logger.debug("RedisClientUtil.getRedisMap() key:" + key + ",redis get value");
		} catch (Exception e) {
			logger.error("RedisClientUtil.getRedisMap() is failed.Exception:", e);
			throw e;
		}
		logger.debug(
				"RedisClientUtil.getRedisMap() key：" + key + "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
		return redisMap;
	}

	/**
	 * 将对象从redis取出自己想要的数据 时间复杂度：O(N)， N 为给定域的数量。
	 * 
	 * 注：当存放map的size越大的时候，不建议使用该方法，会影响到性能，如果size比较小也就是小于10的话，可以使用该方法。
	 * 
	 * 使用方法：
	 * 
	 * @param key
	 *            存放到redis中的唯一key值
	 * @param mapKeys
	 *            存放需要查询的Map对应的key值列表对应的值
	 * @return 获取的对象信息
	 */

	public Map<String, String> getRedisMap(String key, List<String> mapKeys) {
		long startTime = System.currentTimeMillis();
		logger.debug("RedisClientUtil.getRedisMap() key:" + key + ",mapKeys" + mapKeys);
		Map<String, String> redisMap = null;
		JedisCluster jedis = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			if (null != mapKeys && !mapKeys.isEmpty()) {
				int size = mapKeys.size();
				List<String> redisList = jedis.hmget(key, (String[]) mapKeys.toArray(new String[size]));
				if (null != redisList && !redisList.isEmpty()) {
					int redisSize = redisList.size();
					redisMap = new HashMap<String, String>();
					if (redisSize == size) {
						String redisValues = "";
						for (int i = 0; i < size; i++) {
							redisValues = redisList.get(i);
							if (StringUtils.isNotEmpty(redisValues)) {
								redisMap.put(mapKeys.get(i), redisList.get(i));
							}
						}
					}
				}
			} else {
				logger.warn("RedisClientUtil.getRedisMap() key:" + key + ",redis map value is null");
			}
		} catch (Exception e) {
			logger.error("RedisClientUtil.getRedisMap() is failed.Exception:", e);
			throw e;

		}
		logger.debug("RedisClientUtil.getRedisMap() key：" + key + ",mapKeys" + mapKeys + "，所消耗的时间："
				+ (System.currentTimeMillis() - startTime) + "ms");
		return redisMap;
	}

	/**
	 * 将对象从redis服务器中获取。 时间复杂度：O(1)
	 * 
	 * @param key
	 *            存放到redis中的唯一key值
	 * @param mapKey
	 *            存放到redis中的Map数据中的Key值
	 * @return 获取的对象信息
	 * @throws PafaDAOException
	 */

	public String getRedisMapValue(String key, String mapKey) {
		long startTime = System.currentTimeMillis();
		logger.debug("RedisClientUtil.getRedisMapValue() key:" + key + ",mapKey:" + mapKey);
		String mapValue = null;
		JedisCluster jedis = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			mapValue = jedis.hget(key, mapKey);
			logger.debug("RedisClientUtil.getRedisMapValue() key:" + key + ",redis get value");
		} catch (Exception e) {
			logger.error("RedisClientUtil.getRedisMapValue() is failed.Exception:", e);
			throw e;
		}
		logger.debug("RedisClientUtil.getRedisMapValue() key：" + key + "，所消耗的时间："
				+ (System.currentTimeMillis() - startTime) + "ms");
		return mapValue;
	}

	/**
	 * 更新过期时间。 时间复杂度：O(1)
	 * 
	 * @param key
	 *            需要更新存放到reids的key的值
	 * @param seconds
	 *            需要更新的缓存时间，单位：秒
	 * @return
	 */

	public void setExpire(String key, int seconds) {
		long startTime = System.currentTimeMillis();
		logger.debug("RedisClientUtil.setExpire() key:" + key + ",seconds:" + seconds);
		JedisCluster jedis = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			jedis.expire(key, seconds);
		} catch (Exception e) {
			logger.error("RedisClientUtil.setExpire() is failed.Exception:", e);
			throw e;
		}
		logger.debug(
				"RedisClientUtil.setExpire() key：" + key + "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
	}

	/**
	 * 指定key删除redis中环境的信息。 时间复杂度：O(1)
	 */

	public void del(String key) {
		long startTime = System.currentTimeMillis();
		logger.debug("RedisClientUtil.del() key:" + key);
		JedisCluster jedis = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			jedis.del(key);
		} catch (Exception e) {
			logger.error("RedisClientUtil.del() is failed.Exception:", e);
			throw e;
		}
		logger.debug("RedisClientUtil.del() key：" + key + "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
	}

	/**
	 * 计数器 将 key 中储存的数字值增一。 时间复杂度：O(1)
	 * 
	 * @param key
	 */

	public long incr(String key) {
		Long userNum = 0l;
		long startTime = System.currentTimeMillis();
		logger.debug("RedisClientUtil.incr() key:" + key);
		JedisCluster jedis = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			userNum = jedis.incr(key);
		} catch (Exception e) {
			logger.error("RedisClientUtil.incr() is failed.Exception:", e);
			throw e;
		}
		logger.debug("RedisClientUtil.incr() key：" + key + "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
		return userNum;
	}

	public long incr(String key, long num) {
		Long userNum = 0l;
		long startTime = System.currentTimeMillis();
		logger.debug("RedisClientUtil.incr() key:" + key);
		JedisCluster jedis = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			userNum = jedis.incrBy(key, num);
		} catch (Exception e) {
			logger.error("RedisClientUtil.incr() is failed.Exception:", e);
			throw e;
		}
		logger.debug("RedisClientUtil.incr() key：" + key + "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
		return userNum;
	}

	/**
	 * 获取卡方法，从右获取N张卡，然后将获取的卡再放到左边 时间复杂度：O(1)。
	 * 
	 * @param key
	 *            redis键
	 * @param num
	 *            按照该值进行排序
	 * @param cardNum
	 *            卡号
	 */
	public List<String> rpoplpush(String key, int num) {
		List<String> list = null;
		long startTime = System.currentTimeMillis();
		logger.debug("RedisClientUtil.rpoplpush() key:" + key + ",num:" + num);
		JedisCluster jedis = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			// 获取列表中的总数
			long llen = jedis.llen(key);
			if (num <= llen) {
				list = new ArrayList<String>(50);
				// 先从右边获取两张卡，获取并且移除掉这两张卡
				for (int i = 0; i < num; i++) {
					list.add(jedis.rpop(key));
				}
				if (!list.isEmpty()) {
					int size = list.size();
					// 将从右边获取的两张卡从新放到左边去
					jedis.lpush(key, (String[]) list.toArray(new String[size]));
				}
			}
		} catch (Exception e) {
			logger.error("RedisClientUtil.rpoplpush() is failed.Exception:", e);
			throw e;
		}
		logger.debug(
				"RedisClientUtil.rpoplpush() key：" + key + "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
		return list;
	}

	/**
	 * 查询redis中列表总数。 时间复杂度：O(1)
	 * 
	 * @param redisKey
	 * @return
	 */
	public long llen(String redisKey) {
		long llen = 0;
		long startTime = System.currentTimeMillis();
		logger.debug("RedisClientUtil.llen() redisKey:" + redisKey);
		JedisCluster jedis = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			// 获取列表中的总数
			llen = jedis.llen(redisKey);
		} catch (Exception e) {
			logger.error("RedisClientUtil.llen() is failed.Exception:", e);
			throw e;
		}
		logger.debug("RedisClientUtil.llen() redisKey：" + redisKey + "，所消耗的时间：" + (System.currentTimeMillis() - startTime)
				+ "ms");
		return llen;
	}

	/**
	 * 如果有多个 value 值，那么各个 value 值按从左到右的顺序依次插入到表头。 时间复杂度：O(1)
	 * 
	 * @param key
	 *            redis键
	 * @param strings
	 *            插入的列表
	 */
	public long lpush(String key, List<String> list) {
		Long result = 0l;
		long startTime = System.currentTimeMillis();
		logger.debug("RedisClientUtil.lpush() key:" + key);
		JedisCluster jedis = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			if (null != list && !list.isEmpty()) {
				int size = list.size();
				result = jedis.lpush(key, (String[]) list.toArray(new String[size]));
			}
		} catch (Exception e) {
			logger.error("RedisClientUtil.lpush() is failed.Exception:", e);
			throw e;
		}
		logger.debug("RedisClientUtil.lpush() key：" + key + "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
		return result;
	}

	/**
	 * 如果有多个 value 值，那么各个 value 值按从左到右的顺序依次插入到表尾。 时间复杂度：O(1)
	 * 
	 * @param key
	 *            redis键
	 */
	public long rpush(String key, List<String> list) {
		Long result = 0l;
		long startTime = System.currentTimeMillis();
		logger.debug("RedisClientUtil.rpush() key:" + key);
		JedisCluster jedis = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			if (null != list && !list.isEmpty()) {
				int size = list.size();
				result = jedis.lpush(key, (String[]) list.toArray(new String[size]));
			}
		} catch (Exception e) {
			logger.error("RedisClientUtil.rpush() is failed.Exception:", e);
			throw e;
		}
		logger.debug("RedisClientUtil.rpush() key：" + key + "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
		return result;
	}

	/**
	 * 移除到存放到redis列表的对应值的数据。 时间复杂度：O(1)
	 * 
	 * @param key
	 *            redis键
	 * @param value
	 *            需要移除的值
	 */
	public long lrem(String key, String value) {
		long result = 0l;
		long startTime = System.currentTimeMillis();
		logger.debug("RedisClientUtil.lrem() key:" + key + ",value:" + value);
		JedisCluster jedis = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			result = jedis.lrem(key, 0, value);
		} catch (Exception e) {
			logger.error("RedisClientUtil.lrem() is failed.Exception:", e);
			throw e;
		}
		logger.debug("RedisClientUtil.lrem() key：" + key + "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
		return result;
	}

	/**
	 * 从右边获取数据并且剔除掉该数据。 时间复杂度：O(1)
	 * 
	 * @param key
	 * @return
	 */
	public String rpop(String key) {
		String result = null;
		long startTime = System.currentTimeMillis();
		logger.debug("RedisClientUtil.rpop() key:" + key);
		JedisCluster jedis = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			result = jedis.rpop(key);
		} catch (Exception e) {
			logger.error("RedisClientUtil.rpop() is failed.Exception:", e);
			throw e;
		}
		logger.debug("RedisClientUtil.rpop() key：" + key + "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
		return result;
	}

	/**
	 * 从右边获取数据并且剔除掉该数据。 时间复杂度：O(1)
	 * 
	 * @param key
	 * @return
	 */
	public String brpop(String key) {
		List<String> result = null;
		long startTime = System.currentTimeMillis();
		logger.debug("RedisClientUtil.brpop() key:" + key);
		JedisCluster jedis = null;
		// 是否Redis连接异常
		try {
			jedis = RedisPoolUtil.getJedisPool();
			result = jedis.brpop(60, key);
		} catch (Exception e) {
			logger.error("RedisClientUtil.brpop() is failed.Exception:", e);
			throw e;
		}
		logger.debug("RedisClientUtil.rpop() key：" + key + "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
		return result.get(0);
	}

	/**
	 * 通过key获取存在该key中的所有列表数据。 时间复杂度：O(S+N)， S 为偏移量 start ， N 为指定区间内元素的数量。
	 * 
	 * @param key
	 * @param endNum
	 *            获取的个数
	 * @return
	 */
	public List<String> lrange(String key, int endNum) {
		List<String> list = null;
		long startTime = System.currentTimeMillis();
		logger.debug("RedisClientUtil.lrange() key:" + key);
		JedisCluster jedis = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			// 获取存放到redis的列表所有数据
			list = jedis.lrange(key, 0, endNum);
		} catch (Exception e) {
			logger.error("RedisClientUtil.lrange() is failed.Exception:", e);
			throw e;
		}
		logger.debug("RedisClientUtil.lrange() key：" + key + "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
		return list;
	}

	/**
	 * 根据运营商和卡号进行删除过期数据 时间复杂度：O(1)
	 * 
	 * @param appid
	 *            运营商ID
	 * @param cardNum
	 *            卡号
	 */
	public long hdel(String redisKey, String cardNum) {
		logger.debug("RedisClientUtil.hdel() redisKey:" + redisKey + ",cardNum:" + cardNum);
		JedisCluster jedis = null;
		// 是否Redis连接异常

		long startTime = System.currentTimeMillis();
		long result = 0;
		try {
			jedis = RedisPoolUtil.getJedisPool();
			// 获取存放到redis的列表所有数据
			result = jedis.hdel(redisKey, cardNum);
		} catch (Exception e) {
			logger.error("RedisClientUtil.hdel() is failed.Exception:", e);
			throw e;
		}
		logger.debug(
				"RedisClientUtil.hdel() key：" + redisKey + "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
		return result;
	}

	/**
	 * 返回该redisKey在redis中的存活时间，单位：秒。 时间复杂度：O(1)
	 * 
	 * @param redisKey
	 * @return
	 */

	public long ttl(String redisKey) {
		logger.debug("RedisClientUtil.ttl() redisKey:" + redisKey);
		JedisCluster jedis = null;
		// 是否Redis连接异常

		long startTime = System.currentTimeMillis();
		long result = 0;
		try {
			jedis = RedisPoolUtil.getJedisPool();
			// 获取存放到redis的列表所有数据
			result = jedis.ttl(redisKey);
		} catch (Exception e) {
			logger.error("RedisClientUtil.ttl() is failed.Exception:", e);
			throw e;
		}
		logger.debug(
				"RedisClientUtil.ttl() key：" + redisKey + "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
		return result;
	}

	/**
	 * 判断redis中Map是否存在该key值，存在则返回true，不存在则返回false。 时间复杂度：O(1)
	 * 
	 * @param redisKey
	 *            键值
	 * @param cardNum
	 *            查询的卡号
	 * @return 存在：true，不存在：fasle。
	 */
	public boolean hexists(String redisKey, String cardNum) {
		logger.debug("RedisClientUtil.hexists() redisKey:" + redisKey + ",cardNum:" + cardNum);
		JedisCluster jedis = null;
		// 是否Redis连接异常

		long startTime = System.currentTimeMillis();
		boolean result = false;
		try {
			jedis = RedisPoolUtil.getJedisPool();
			// 获取存放到redis的列表所有数据
			result = jedis.hexists(redisKey, cardNum);
		} catch (Exception e) {
			logger.error("RedisClientUtil.hexists() is failed.Exception:", e);
			throw e;
		}
		logger.debug("RedisClientUtil.hexists() key：" + redisKey + "，所消耗的时间：" + (System.currentTimeMillis() - startTime)
				+ "ms");
		return result;
	}

	public boolean updateUserdebug(String redisKey, Map<String, String> param) {
		logger.debug("RedisClientUtil.updateUserdebug() redisKey:" + redisKey + ",param:" + param);
		JedisCluster jedis = null;
		// 是否Redis连接异常

		long startTime = System.currentTimeMillis();
		boolean result = false;
		try {
			jedis = RedisPoolUtil.getJedisPool();
			// 更新redis的数据
			jedis.hmset(redisKey, param);
		} catch (Exception e) {
			logger.error("RedisClientUtil.updateUserdebug() is failed.Exception:", e);
			throw e;
		}
		logger.debug("RedisClientUtil.updateUserdebug() key：" + redisKey + "，所消耗的时间："
				+ (System.currentTimeMillis() - startTime) + "ms");
		return result;
	}

	public long hIncrCounter(String key, String field, long i) {
		long startTime = System.currentTimeMillis();
		long l = 0;
		logger.debug("RedisClientUtil.hIncrCounter key:" + key);
		JedisCluster jedis = null;
		// 是否Redis连接异常

		try {
			jedis = RedisPoolUtil.getJedisPool();
			boolean numStr = jedis.hexists(key, field);
			if (!numStr) {// 如果为空的话，初始化计数器
				jedis.hset(key, field, "0");
			}
			l = jedis.hincrBy(key, field, i);
		} catch (Exception e) {
			logger.error("RedisClientUtil.hIncrCounter is failed.Exception:", e);
			throw e;
		}
		logger.debug("RedisClientUtil.hIncrCounter key：" + key + "，所消耗的时间：" + (System.currentTimeMillis() - startTime)
				+ "ms");
		return l;
	}

	/**
	 * 检查给定 key 是否存在。
	 * 
	 * @param redisKey
	 * @return 存在：true，不存在：fasle。
	 */

	public boolean exists(String redisKey) {
		logger.debug("RedisClientUtil.exists() redisKey:" + redisKey);
		JedisCluster jedis = null;
		// 是否Redis连接异常
		long startTime = System.currentTimeMillis();
		boolean result = false;
		try {
			jedis = RedisPoolUtil.getJedisPool();
			// 获取存放到redis的列表所有数据
			result = jedis.exists(redisKey);
		} catch (Exception e) {
			logger.error("RedisClientUtil.exists() is failed.Exception:", e);
			throw e;
		}
		logger.debug(
				"RedisClientUtil.exists() key：" + redisKey + "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
		return result;
	}

	/**
	 * 计数器 将 key 中储存的数字值减一。 时间复杂度：O(1)
	 * 
	 * @param key
	 */

	public long decr(String key) {
		Long userNum = 0l;
		long startTime = System.currentTimeMillis();
		logger.debug("RedisClientUtil.decr() key:" + key);
		JedisCluster jedis = null;
		// 是否Redis连接异常
		try {
			jedis = RedisPoolUtil.getJedisPool();
			userNum = jedis.decr(key);
		} catch (Exception e) {
			logger.error("RedisClientUtil.decr() is failed.Exception:", e);
			throw e;
		}
		logger.debug("RedisClientUtil.decr() key：" + key + "，所消耗的时间：" + (System.currentTimeMillis() - startTime) + "ms");
		return userNum;
	}

}
