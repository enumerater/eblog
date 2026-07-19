package com.enumerate.eblog.service;

import com.enumerate.eblog.entity.Draft;
import com.enumerate.eblog.mapper.DraftMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DraftService {

    private final DraftMapper draftMapper;

    public DraftService(DraftMapper draftMapper) {
        this.draftMapper = draftMapper;
    }

    public List<Draft> findAll() {
        return draftMapper.findAll();
    }

    public Draft findById(Long id) {
        return draftMapper.findById(id);
    }

    public Draft save(Draft draft) {
        if (draft.getSummary() == null || draft.getSummary().isBlank()) {
            draft.setSummary(Draft.generateSummary(draft.getContent()));
        }
        draftMapper.insert(draft);
        return draftMapper.findById(draft.getId());
    }

    public Draft update(Long id, Draft partial) {
        Draft existing = draftMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("草稿不存在");
        }

        Draft toUpdate = new Draft();
        toUpdate.setId(id);
        if (partial.getTitle() != null) toUpdate.setTitle(partial.getTitle());
        if (partial.getContent() != null) toUpdate.setContent(partial.getContent());
        if (partial.getTagsJson() != null) toUpdate.setTagsJson(partial.getTagsJson());
        if (partial.getSummary() != null) toUpdate.setSummary(partial.getSummary());
        if (partial.getArticleId() != null) toUpdate.setArticleId(partial.getArticleId());

        draftMapper.update(toUpdate);
        return draftMapper.findById(id);
    }

    public void delete(Long id) {
        if (draftMapper.findById(id) == null) {
            throw new RuntimeException("草稿不存在");
        }
        draftMapper.deleteById(id);
    }
}