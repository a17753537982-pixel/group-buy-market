package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.tag.adapter.repository.ITagRepository;
import cn.bugstack.domain.tag.model.entity.CrowdsTagJobEntity;
import cn.bugstack.infrastructure.dao.ICrowdTagsDao;
import cn.bugstack.infrastructure.dao.ICrowdTagsDetailDao;
import cn.bugstack.infrastructure.dao.ICrowdTagsJobDao;
import cn.bugstack.infrastructure.dao.po.CrowdTags;
import cn.bugstack.infrastructure.dao.po.CrowdTagsDetail;
import cn.bugstack.infrastructure.dao.po.CrowdTagsJob;
import cn.bugstack.infrastructure.redis.IRedisService;
import org.redisson.api.RBitSet;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

@Repository
public class TagRepository implements ITagRepository {


    @Resource
    private IRedisService redisService;

    @Resource
    private ICrowdTagsDao crowdTagsDao;

    @Resource
    private ICrowdTagsDetailDao crowdTagsDetailDao;

    @Resource
    private ICrowdTagsJobDao crowdTagsJobDao;

    @Override
    public void addCrowdTagsStatistics(String tagId, int size) {

        CrowdTags crowdTagsReq=new CrowdTags();
        crowdTagsReq.setTagId(tagId);
        crowdTagsReq.setStatistics(size);
        crowdTagsDao.updateCrowdTagsStatistics(crowdTagsReq);
    }

    @Override
    public void addCrowdTagsUserId(String userId, String tagId) {
        CrowdTagsDetail crowdTagsDetailReq=new CrowdTagsDetail();
        crowdTagsDetailReq.setUserId(userId);
        crowdTagsDetailReq.setTagId(tagId);

        try {
            crowdTagsDetailDao.addCrowdTagsUserId(crowdTagsDetailReq);
            //获取BitSet
            RBitSet bitSet = redisService.getBitSet(tagId);
            bitSet.set(redisService.getIndexFromUserId(userId));
        }catch (DuplicateKeyException ignore){}

    }

    @Override
    public CrowdsTagJobEntity queryCrowdsTagJobEntity(String tagId, String batchId) {
        CrowdTagsJob crowdTagsJobReq=new CrowdTagsJob();
        crowdTagsJobReq.setTagId(tagId);
        crowdTagsJobReq.setBatchId(batchId);
        CrowdTagsJob crowdTagsJobRes = crowdTagsJobDao.queryCrowdTagsJob(crowdTagsJobReq);

        if (null == crowdTagsJobRes) return null;

        return CrowdsTagJobEntity.builder()
                .tagType(crowdTagsJobRes.getTagType())
                .tagRule(crowdTagsJobRes.getTagRule())
                .statStartTime(crowdTagsJobRes.getStatStartTime())
                .statEndTime(crowdTagsJobRes.getStatEndTime())
                .build();
    }
}
