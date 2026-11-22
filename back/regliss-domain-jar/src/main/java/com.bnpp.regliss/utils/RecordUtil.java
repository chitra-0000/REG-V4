package com.bnpp.regliss.utils;

import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.record.Address;
import com.bnpp.regliss.entity.record.AddressContainer;
import com.bnpp.regliss.entity.record.Record;
import org.apache.commons.lang.StringUtils;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class RecordUtil {
    public static final String FIRST_LETTER_OF_ADDRESS_REFERENCE = "Z";
    public static final char MAXIMUM_LETTER_OF_ADDRESS_REFERENCE = 'Z';
    /**
     *
     * @return the next address reference suffix, or <code>null</code> the range of valid address suffixes was depleted
     */
    public static String computeNextAddressReference(Version version, String maxExistingAddressReferenceSuffix) {
        //if null or ZZZ..ZZ
        if(maxExistingAddressReferenceSuffix==null || maxExistingAddressReferenceSuffix.equals(buildMaxValueForLetterSection(maxExistingAddressReferenceSuffix.length()))){
            return null;
        }

        try {
            String nextAddressReference = String.format("%03d", Long.valueOf(version.getList().getReference())) + FIRST_LETTER_OF_ADDRESS_REFERENCE;
            BigInteger bI = new BigInteger(maxExistingAddressReferenceSuffix, 36);
            return nextAddressReference + addPadding((bI.add(BigInteger.ONE).toString(36).toUpperCase()),maxExistingAddressReferenceSuffix.length());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String addPadding(String incrementedSuffix, int length) {
        if(incrementedSuffix.length()<length){
            return padding( length-incrementedSuffix.length(), '0')+incrementedSuffix;
        }else {
            return incrementedSuffix;
        }
    }

    public static int getPosOfLastLetter(String maxExistingAddressReferenceSuffix) {
        for(int i=0;i<maxExistingAddressReferenceSuffix.length();i++){
            if(maxExistingAddressReferenceSuffix.charAt(i)<='9' && maxExistingAddressReferenceSuffix.charAt(i)>='0'){
                return i-1;
            }
        }
        return maxExistingAddressReferenceSuffix.length()-1;
    }

    private static String buildMaxValueForLetterSection(int numberOfLeters) {
        return padding( numberOfLeters, MAXIMUM_LETTER_OF_ADDRESS_REFERENCE);
    }

    private static String padding(int numberOfChars, char fillWithLetter) {
        StringBuilder maxValForLetterBuilder = new StringBuilder(numberOfChars);
        for (int i=0; i< numberOfChars;i++) {
            maxValForLetterBuilder.append(fillWithLetter);
        }
        return maxValForLetterBuilder.toString();
    }

    public static String getAddressReferenceSuffix(String addressReference) {
        return addressReference.substring(4);
    }

    public static Set<Address> getAddresses(Record record){
        if(record instanceof AddressContainer){
            AddressContainer thisRecord = (AddressContainer)record;
            return thisRecord.getAddresses();
        }
        return  new HashSet<>();
    }

    private RecordUtil(){}

    public static String getMinimumAddressReference() {
        return "N"+"00000";
    }

    public static String getMaximumAddressReference(Set<String> allPastAddressReglissReferences) {
        return allPastAddressReglissReferences.stream()
                .filter(StringUtils::isNotEmpty)
                .map(RecordUtil::getAddressReferenceSuffix)
                .max(Comparator.naturalOrder())
                .orElse(RecordUtil.getMinimumAddressReference());
    }
}
