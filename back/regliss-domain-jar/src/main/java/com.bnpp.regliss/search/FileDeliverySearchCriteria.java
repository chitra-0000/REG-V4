package com.bnpp.regliss.search;

import com.bnpp.regliss.entity.FileDelivery.Status;
import com.bnpp.regliss.repository.pagination.PaginationOptions;

import java.util.Collections;
import java.util.List;

public class FileDeliverySearchCriteria {

    private String listName;
    private long versionNumber;
    private String fluxName;
    private String subscriberName;
    private String[] actualRunTime;
    private List<Status> status;
    private String listType;


    private PaginationOptions paginationOptions = new PaginationOptions(50);

    public String getListName() { return listName; }


    public void setListName(String listName) { this.listName = listName; }


    public long getVersionNumber() { return versionNumber; }


    public void setVersionNumber(long versionNumber) { this.versionNumber = versionNumber; }


    public String getFluxName() { return fluxName; }


    public void setFluxName(String fluxName) { this.fluxName = fluxName; }


    public String getSubscriberName() { return subscriberName; }


    public void setSubscriberName(String subscriberName) { this.subscriberName = subscriberName; }


    public String[] getActualRunTime() { return actualRunTime; }


    public void setActualRunTime(String[] actualRunTime) { this.actualRunTime = actualRunTime; }


    public List<Status> getStatus() { return Collections.unmodifiableList(status); }

    public void setStatus(List<Status> status) { this.status = status; }

    public String getListType() { return listType; }

    public void setListType(String listType) { this.listType = listType; }

    public PaginationOptions getPaginationOptions() { return paginationOptions; }

    public void setPaginationOptions(PaginationOptions paginationOptions) {
        this.paginationOptions = paginationOptions;
    }
}
