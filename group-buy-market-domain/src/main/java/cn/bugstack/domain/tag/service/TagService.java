package cn.bugstack.domain.tag.service;


import cn.bugstack.domain.tag.adapter.repository.ITagRepository;
import cn.bugstack.domain.tag.model.entity.CrowdsTagJobEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TagService implements ITagService{

    @Resource
    private ITagRepository tagRepository;


    @Override
    public void execTagBatchJob(String tagId, String batchId) {
        CrowdsTagJobEntity crowdsTagJobEntity=tagRepository.queryCrowdsTagJobEntity(tagId,batchId);

        //采集用户数据
        List<String> userIdList=new ArrayList<String>(){{
            add("WangTian");
            add("liuJiLong");
        }};

        //加载进用户人群表
        for(String userId :userIdList)
        {
            tagRepository.addCrowdTagsUserId(userId,tagId);
        }

        //更新人群标签统计量
        tagRepository.addCrowdTagsStatistics(tagId,userIdList.size());
    }
}
