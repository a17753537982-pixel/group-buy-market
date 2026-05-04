package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.infrastructure.dao.po.GroupBuyActivity;
import cn.bugstack.infrastructure.dcc.DCCService;
import cn.bugstack.infrastructure.redis.IRedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.function.Supplier;

public abstract class AbstractRepsitory {

    private Logger logger= LoggerFactory.getLogger(AbstractRepsitory.class);

    @Resource
    protected DCCService dccService;

    @Resource
    protected IRedisService redisService;


    protected <T> T getFromCacheOrDb(String cacheKey, Supplier<T> dbFallback)
    {
        if(dccService.isCaCheOpenSwitch())
        {
            T cacheResult  = redisService.getValue(cacheKey);
            if(null!=cacheResult)
            {
                return cacheResult;
            }

            T dbResult=dbFallback.get();
            if(null==dbResult)
            {
                return null;
            }
            redisService.setValue(cacheKey,dbResult);
            return dbResult;
        }else {
            return dbFallback.get();
        }
    }

}
