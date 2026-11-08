package com.bnpp.regliss.facade.dto.list;

// [MISSING FROM SCREENSHOTS: lines 3-5 of the original file were not visible in the photos]

public class ReglissListAndFilterSubscriptionDto {

    public Long listId;
    public String listFilterName;
    public Long filterId;
    public Long concatenatedListId;

    public ReglissListAndFilterSubscriptionDto(ReglissList list, DJExportFilter watchlistFormat) {
        if (list != null) {
            listId = list.getId();
            listFilterName = list.getReference() + "-" + list.getShortName();
        } else {
            listFilterName = "Global plus OFAC";
        }
        if (watchlistFormat != null) {
            filterId = watchlistFormat.getId();
            listFilterName += " - " + watchlistFormat.getFilterName();
        }
    }

    public ReglissListAndFilterSubscriptionDto(ReglissList list, DJExportFilter watchlistFormat,
                                               ConcatenatedList concatenatedList) {
        if (list != null) {
            listId = list.getId();
            listFilterName = list.getReference() + "-" + list.getShortName();
        } else {
            concatenatedListId = concatenatedList.getId();
            listFilterName = concatenatedList.getLabel();
        }
        if (watchlistFormat != null) {
            filterId = watchlistFormat.getId();
            listFilterName += " - " + watchlistFormat.getFilterName();
        }
    }

    public ReglissListAndFilterSubscriptionDto(){

    }

}

