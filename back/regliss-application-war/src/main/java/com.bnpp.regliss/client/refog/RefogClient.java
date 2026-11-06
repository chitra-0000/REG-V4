package com.bnpp.regliss.client.refog;

// TODO: imports (lines 3-25) were folded in the IDE; reconstructed from usage - verify packages.
import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.client.refog.jaxb.Field;
import com.bnpp.regliss.client.refog.jaxb.RefogObject;
import com.bnpp.regliss.client.refog.jaxb.RefogSearch;
import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.profile.ReglissWebProfile;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.xml.bind.JAXBContext;          // jakarta.xml.bind.* on Spring Boot 3
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Service
@ReglissWebProfile
public class RefogClient {

    @Value("${refog.host}")
    private String refogHost;

    @Value("${refog.user}")
    private String refogUser;

    @Value("${refog.pass}")
    private String refogPass;

    @Value("${refog.url.searchText.length}")
    private int refogUrlSearchTextLength;

    private static final JAXBContext JAXB_CONTEXT;

    @Autowired
    private RefogUserMapper refogMapper;
    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(RefogSearch.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public User getExactlyOne(String username) {
        List<RefogUser> refogUsers = searchUsers(username);

        if (refogUsers == null || refogUsers.isEmpty()) {
            throw new ReglissException(ErrorCode.SEARCH_REFOG_USER_NOT_FOUND);
        }
        User userFromRefog = refogMapper.toUserEntity(refogUsers.get(0));
        if (!userFromRefog.getUsername().equalsIgnoreCase(username)) {
            throw new ReglissException("Username does not exactly match", ErrorCode.SEARCH_REFOG_USER_NOT_FOUND);
        }
        return userFromRefog;
    }

    public List<RefogUser> getUsersByUsernames(List<String> usernames){
        List<List<RefogUser>> refogUsers = new ArrayList<>();
        if (!usernames.isEmpty()) {
            String formattedUsernames = String.join(",", usernames);
            List<String> userNamesWithMaxLength = splitByMaxLength(formattedUsernames);
            for (String userName : userNamesWithMaxLength) {
                refogUsers.add(searchUsers(userName));
            }
            return refogUsers.stream().flatMap(List :: stream).collect(toList());
        } else {
            return new ArrayList<>();
        }
    }

    private List<RefogUser> searchUsers(String username) {
        RefogSearch result = doSearch(username);

        if (result == null || result.getData() == null) {
            return emptyList();
        }
        return result.getData().getRefogObject().stream()
                .map(this::extractRefogUser)
                .collect(toList());
    }

    private RefogUser extractRefogUser(RefogObject refogObject) {
        RefogUser user = new RefogUser();
        for (Field field : refogObject.getFields().getField()) {
            setField(user, field);
        }
        return user;
    }

    private void setField(RefogUser refogUser, Field field) {
        String fieldName = field.getName();
        String fieldValue = field.getValue();

        if (fieldName.equals("SIP_PRENOM_USUEL")) {
            refogUser.setFirstName(fieldValue);
        }
        if (fieldName.equals("SIP_NOM_USUEL")) {
            refogUser.setLastName(fieldValue);
        }
        if (fieldName.equals("SIP_I_UID")) {
            refogUser.setUsername(fieldValue);
        }
        if (fieldName.equals("SIP_EMAIL_GROUPE")) {
            refogUser.setEmail(fieldValue);
        }
        if (fieldName.equals("SIP_UO_POLE_EN")) {
            refogUser.setPole(fieldValue);
        }
        if (fieldName.equals("SIP_UO_PRINC_EN")) {
            refogUser.setEntity(fieldValue);
        }
        if (fieldName.equals("SIP_PAYS_EN")) {
            refogUser.setCountryName(fieldValue);
        }
        if (fieldName.equals("SIP_METIER_EN")) {
            refogUser.setBusinessLine(fieldValue);
        }
    }

    private RefogSearch doSearch(String username) {
        try {
            if (StringUtils.isNotBlank(username) && username.length() > 2) {
                String url = computeSearchUrl(username);
                Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
                return (RefogSearch) unmarshaller.unmarshal(new URL(UriComponentsBuilder.fromUriString(url).toUriString()));
            }
        } catch (MalformedURLException | JAXBException e) {
            throw new ReglissException(e, ErrorCode.CANNOT_CONNECT_TO_REFOG);
        }
        return null;
    }

    private String computeSearchUrl(String inputFields) {
        return this.refogHost + "/RefogWS?" +
                "applicationId=" + refogUser +
                "&applicationPwd=" + refogPass +
                "&searchType=RSI" + //
                "&searchText=" + inputFields +
                "&objectType=1" +
                "&outputFields=SIP_I_UID,SIP_NOM_USUEL,SIP_PRENOM_USUEL,SIP_EMAIL_GROUPE,SIP_UO_POLE_EN,SIP_UO_PRINC_EN,SIP_PAYS_EN," +
                "SIP_METIER_EN";

    }

    public List<String> splitByMaxLength(String input) {
        String[] tokens = input.split(",");

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];

            String candidate = (current.length() == 0)
                    ? token
                    : current + "," + token;

            if (candidate.length() <= refogUrlSearchTextLength) {
                if (current.length() > 0) {
                    current.append(',');
                }
                current.append(token);
            } else {
                result.add(current.toString());
                current = new StringBuilder(token);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }
}
