package com.bnpp.regliss.utils;

import java.util.*;
import java.util.stream.*;

public class TextNormalizerQueryUtil {


    public static String createStringComparisonSql(String exprToTest, String paramValue) {
        String normalParam = "'"+normalize(paramValue)+"'";
        return createQueryCondition(paramValue, exprToTest, normalParam);
    }

    public static String createStringComparisonJpql(String exprToTest, String paramName, String paramValue) {
        String normalParam = ":"+paramName;
        return createQueryCondition(paramValue, exprToTest, normalParam);
    }

    private static String createQueryCondition(String paramValue, String normalExpr, String normalParam) {
        if (isLikeCriteria(paramValue)) {
            return normalExpr + " = " + normalParam;
        }
        return normalExpr + " LIKE " + normalParam;
    }

    public static String createStringListComparisonJpql(String exprToTest, String paramName) {
        return exprToTest + " IN " + ":" + paramName;
    }

    public static boolean isLikeCriteria(String paramValue) {
        return !paramValue.contains("%") && !paramValue.contains("?");
    }


    public static String normalize(String expr) {
        if(expr == null) {
            return null;
        }
        expr = expr.toLowerCase();
        Map<String, String> translations = new HashMap<>();
        translations.put("áÁâÂàÀåÅãÃäÄ", "a");
        translations.put("éÉêÊèÈëË", "e");
        translations.put("çÇ", "c");
        translations.put("íÍîÎìÌïÏ", "i");
        translations.put("ñÑ", "n");
        translations.put("óÓôÔòÒøØõÕöÖ", "o");
        translations.put("šŠ", "s");
        translations.put("úÚûÛùÙüÜ", "u");
        translations.put("ýÝÿŸ", "y");
        translations.put("?", "_");
        translations.put("æÆ", "ae");
        translations.put("œŒ", "oe");

        for (Map.Entry<String, String> pair : translations.entrySet()) {
            for (String oneChar : pair.getKey().split("")) {
                expr = expr.replace(oneChar, pair.getValue());
            }
        }

        return expr;
    }

    public static List<String> normalize(List<String> exprs) {
        return exprs.stream().map(TextNormalizerQueryUtil::normalize).collect(Collectors.toList());
    }

    public static String normalizePartialMatchCriteria(String searchCriteria){
        if(searchCriteria == null){
            return null;
        }
        if(searchCriteria.startsWith("\"") && searchCriteria.endsWith("\"") && searchCriteria.length()>=2){
            return searchCriteria.substring(1,searchCriteria.length()-1);
        }
        if(searchCriteria.indexOf('%') >= 0 || searchCriteria.indexOf('?') >= 0 || searchCriteria.indexOf('*') >= 0){
            if(searchCriteria.indexOf('*') >= 0){
                searchCriteria = searchCriteria.replace("*", "%");
            }
            return searchCriteria;
        }else{
            if(searchCriteria.length() > 0){
                return "%" + searchCriteria + "%";
            }else{
                return "";
            }
        }
    }

}
