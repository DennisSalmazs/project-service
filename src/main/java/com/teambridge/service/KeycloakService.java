package com.teambridge.service;

import java.util.List;

public interface KeycloakService {

    String getAccessToken();

    String getUsername();

    List<String> getUserClientRoles(String username);

    boolean hasClientRole(String username, String role);

}
