package cn.bugstack.infrastructure.dcc;

import cn.bugstack.types.annotations.DCCValue;
import cn.bugstack.types.common.Constants;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class DCCService {

    @DCCValue("downgradeSwitch:0")
    private String downgradeSwitch;


    @DCCValue("cutRange:100")
    private String cutRange;

    @DCCValue("scBlacklist:s02c02")
    private String scBlacklist;

    @DCCValue("cacheOpenSwitch:0")
    private String cacheOpenSwitch;

    public boolean isDowngradeSwitch()
    {
        return "1".equals(downgradeSwitch);
    }

    public boolean isCutRange(String userId)
    {
        int hashCode = Math.abs(userId.hashCode());

        int lastTwoDigits=hashCode %100;

        if(lastTwoDigits <=Integer.parseInt(cutRange))
        {
            return true;
        }
        return false;
    }

    /**
     * 判断黑名单拦截渠道，true 拦截、false 放行
     */
    public boolean isSCBlackIntercept(String source, String channel) {
        List<String> list = Arrays.asList(scBlacklist.split(Constants.SPLIT));
        return list.contains(source + channel);
    }
    /**
     * 判断Redis拦截渠道，true 拦截、false 放行
     */
    public boolean isCaCheOpenSwitch()
    {
        return "0".equals(cacheOpenSwitch);
    }

}
