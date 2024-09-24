package com.encora.codesynthesistool.util;

import com.encora.codesynthesistool.model.User;
import java.util.List;

public class TestUtils {

    public static User getTestUser() {
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setRoles(List.of("ROLE_USER"));
        return testUser;
    }
}
