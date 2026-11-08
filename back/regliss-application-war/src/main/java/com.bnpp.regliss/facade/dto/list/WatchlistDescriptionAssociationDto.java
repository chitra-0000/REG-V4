package com.bnpp.regliss.facade.dto.list;

import com.bnpp.regliss.exporter.Description1FilterValues;

public class WatchlistDescriptionAssociationDto {


    public static class WatchlistDescriptionDto {
        public Long id;
        public WatchlistDescriptionDto(Long id) { this.id = id; }
    }

    public static class WatchlistDescription1Dto {
        public Description1FilterValues id;
        public boolean isPep;
        public WatchlistDescription1Dto(Description1FilterValues id, boolean isPep) {
            this.id = id;
            this.isPep = isPep;
        }
    }

    public WatchlistDescription1Dto description1;
    public WatchlistDescriptionDto description2;
    public WatchlistDescriptionDto description3;

    public WatchlistDescriptionAssociationDto(Description1FilterValues description1Id, boolean isPep,
            Long description2Id, Long description3Id) {
        this.description1 = new WatchlistDescription1Dto(description1Id, isPep);
        this.description2 = new WatchlistDescriptionDto(description2Id);
        this.description3 = new WatchlistDescriptionDto(description3Id);
    }



}
