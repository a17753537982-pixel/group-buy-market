package cn.bugstack.domain.tag.adapter.repository;

import cn.bugstack.domain.tag.model.entity.CrowdsTagJobEntity;

public interface ITagRepository {
    void addCrowdTagsStatistics(String tagId, int size);

    void addCrowdTagsUserId(String userId, String tagId);

    CrowdsTagJobEntity queryCrowdsTagJobEntity(String tagId, String batchId);
}
