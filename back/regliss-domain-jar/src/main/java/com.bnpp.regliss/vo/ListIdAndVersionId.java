package com.bnpp.regliss.vo;

import lombok.Getter;

@Getter
public class ListIdAndVersionId {
    private Long listId;
    private Long versionId;

    public ListIdAndVersionId(Long listId, Long versionId ){
        this.listId = listId;
        this.versionId = versionId;
    }
}