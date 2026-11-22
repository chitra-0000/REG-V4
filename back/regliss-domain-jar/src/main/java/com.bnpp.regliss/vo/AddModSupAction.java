package com.bnpp.regliss.vo;

import com.bnpp.regliss.repository.AddModSupRepository;

import java.util.stream.Stream;

public enum AddModSupAction {
    ADD("ADD") {
        @Override
        public Stream<AddModSupLine> queryAddModSupElementsDetails(AddModSupRepository repo, Long versionId) {
            return repo.getAddSupElementsByActionRaw(versionId, this.getActionName()).map(AddModSupLine::new);
        }
    },
    MOD("MOD") {
        @Override
        public Stream<AddModSupLine> queryAddModSupElementsDetails(AddModSupRepository repo, Long versionId) {
            return repo.getModElementsRaw(versionId).map(AddModSupLine::new);
        }
    },
    SUP("SUP") {
        @Override
        public Stream<AddModSupLine> queryAddModSupElementsDetails(AddModSupRepository repo, Long versionId) {
            return repo.getAddSupElementsByActionRaw(versionId, this.getActionName()).map(AddModSupLine::new);
        }
    };

    private final String action;

    AddModSupAction(String action) {
        this.action = action;
    }
    public abstract Stream<AddModSupLine> queryAddModSupElementsDetails(AddModSupRepository repo, Long versionId);
    public String getActionName() {
        return action;
    }

    public static AddModSupAction fromStringAction(String action) {
        for(AddModSupAction enumAction : AddModSupAction.values()) {
            if(enumAction.action.equalsIgnoreCase(action)) {
                return enumAction;
            }
        }

        throw new IllegalArgumentException("Not a valid action: " + action);
    }

}