package com.sinosafe.macaw.database.connector.redis;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO 通过spring上下文初始化redis连接配置
 * @author yusha
 *
 */
public class RedisConfigInit {
	
	private static final Logger logger = LoggerFactory.getLogger(RedisConfigInit.class);
	
	
	public RedisConfigInit(String redisHost,String passWord,String timeOut){
		if(StringUtils.isNotBlank(redisHost)){
			int timeOutInt = timeOut!=null&&!"".equals(timeOut)?Integer.parseInt(timeOut):10000;
			RedisPoolUtil.init(redisHost,passWord,timeOutInt);
		}else{
			logger.error("严重错误,serious redis setting error,取得Redis properties not exist配置值不存在.");
		}
	}
}
