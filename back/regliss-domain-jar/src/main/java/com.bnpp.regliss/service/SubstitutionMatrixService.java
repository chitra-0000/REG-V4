package com.bnpp.regliss.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.bnpp.regliss.entity.SubstitutionMatrixElement;

public class SubstitutionMatrixService {

    private SubstitutionMatrixService(){
    }

    public static String applySubstitutionMatrix(String fieldValue, List<SubstitutionMatrixElement> originalSubstitutionMatrix) {
        List<SubstitutionMatrixElement> substitutionMatrix = new ArrayList<>(originalSubstitutionMatrix);
        while (true) {
            NavigableMap<Integer, SubstitutionMatrixElement> replacementsInCurrentIteration = new TreeMap<>();
            for (SubstitutionMatrixElement matrixElement : substitutionMatrix) {
                int indexOfElement = fieldValue.indexOf(matrixElement.getOriginalValue(), /* fromIndex: */ 0);
                while (indexOfElement != -1) {
                    replacementsInCurrentIteration.put(indexOfElement, matrixElement);
                    indexOfElement = fieldValue.indexOf(matrixElement.getOriginalValue(), /* fromIndex: */ indexOfElement + 1);
                }
            }

            for(Integer positionToReplace : replacementsInCurrentIteration.descendingKeySet()) {
                SubstitutionMatrixElement matrixElement = replacementsInCurrentIteration.get(positionToReplace);
                fieldValue = fieldValue.substring(0, positionToReplace) +  matrixElement.getSubstitutionValue() +
                        fieldValue.substring(positionToReplace + matrixElement.getOriginalValue().length(), fieldValue.length());
                substitutionMatrix.remove(matrixElement);
            }
            if (replacementsInCurrentIteration.isEmpty()) {
                return fieldValue;
            }
        }
    }
}
