package com.bnpp.regliss.entity.six.entity;

import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;

public interface PersistableEntity {
    void setVersion(Version version);
    void setList(ReglissList list);
}
