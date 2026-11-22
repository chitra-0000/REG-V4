package com.bnpp.regliss.service.permission;

import com.bnpp.regliss.entity.ListPermission;
import com.bnpp.regliss.entity.UserListPermission;

import lombok.Data;

@Data
public class ListPermissionVO {

    private Long reglissListId;
    private ListPermission permission;

    public ListPermissionVO(UserListPermission userListPermission) {
        this.reglissListId = userListPermission.getReglissListId();
        this.permission = userListPermission.getPermission();
    }

    public ListPermissionVO(Long listId, ListPermission permission) {
        this.reglissListId = listId;
        this.permission = permission;
    }

    public ListPermissionVO() {
        super();
    }

}
