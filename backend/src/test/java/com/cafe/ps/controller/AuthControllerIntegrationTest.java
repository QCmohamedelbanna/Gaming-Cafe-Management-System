package com.cafe.ps.controller;

import com.cafe.ps.AbstractMySQLIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import com.cafe.ps.repository.AppUserRepository;
import com.cafe.ps.entity.AccessRule;
import com.cafe.ps.entity.Permission;
import com.cafe.ps.entity.Role;
import com.cafe.ps.entity.RolePermission;
import com.cafe.ps.repository.AccessRuleRepository;
import com.cafe.ps.repository.RolePermissionRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.task.scheduling.enabled=false"
        }
)
@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends AbstractMySQLIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AccessRuleRepository accessRuleRepository;

    @Test
    void loginAcceptsTheRawXsrfCookieValueInTheHeader() throws Exception {
        MvcResult csrfResponse = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie csrfCookie = csrfResponse.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();

        mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"" + TEST_ADMIN_USERNAME + "\",\"password\":\"" + TEST_ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("PERMISSIONS_MANAGE")))
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("BILL_REFUND")));
    }

    @Test
    void adminCanReadAndUpdateRolePermissions() throws Exception {
        Cookie csrfCookie = csrfCookie();
        MvcResult loginResponse = login(csrfCookie);
        MockHttpSession session = (MockHttpSession) loginResponse.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/permissions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code").value(org.hamcrest.Matchers.hasItem("PERMISSIONS_MANAGE")));

        mockMvc.perform(put("/api/permissions/roles/MANAGER")
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(APPLICATION_JSON)
                        .content("{\"permissions\":[\"POS_USE\"]}"))
                .andExpect(status().isOk());

        assertThat(rolePermissionRepository.findByRoleOrderByPermissionAsc(Role.MANAGER))
                .extracting(RolePermission::getPermission)
                .containsExactly(Permission.POS_USE);
    }

    @Test
    void adminCanActivateDeactivateAndDeleteAUserWithoutShiftHistory() throws Exception {
        Cookie csrfCookie = csrfCookie();
        MvcResult loginResponse = login(csrfCookie);
        MockHttpSession session = (MockHttpSession) loginResponse.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(post("/api/users")
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"status-test\",\"displayName\":\"Status Test\",\"password\":\"password123\",\"role\":\"CASHIER\"}"))
                .andExpect(status().isOk());

        Long userId = appUserRepository.findByUsernameIgnoreCase("status-test")
                .orElseThrow()
                .getId();

        mockMvc.perform(patch("/api/users/{id}/active", userId)
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/users/{id}/active", userId)
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/users/{id}", userId)
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isOk());

        assertThat(appUserRepository.findByUsernameIgnoreCase("status-test")).isEmpty();
    }

    @Test
    void adminCanManageCustomRulesAndAssignThemToUsers() throws Exception {
        Cookie csrfCookie = csrfCookie();
        MvcResult loginResponse = login(csrfCookie);
        MockHttpSession adminSession = (MockHttpSession) loginResponse.getRequest().getSession(false);
        assertThat(adminSession).isNotNull();

        mockMvc.perform(post("/api/rules")
                        .session(adminSession)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Night Cashier\",\"description\":\"Limited evening access\",\"permissions\":[\"POS_USE\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemRule").value(false))
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.contains("POS_USE")));

        AccessRule rule = accessRuleRepository.findByNameIgnoreCase("Night Cashier").orElseThrow();
        String userPayload = "{\"username\":\"rule-test\",\"displayName\":\"Rule Test\",\"password\":\"password123\",\"role\":\"CASHIER\",\"ruleId\":" + rule.getId() + "}";
        mockMvc.perform(post("/api/users")
                        .session(adminSession)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(APPLICATION_JSON)
                        .content(userPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleName").value("Night Cashier"))
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.contains("POS_USE")));

        MvcResult customLogin = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"rule-test\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.contains("POS_USE")))
                .andReturn();
        MockHttpSession customSession = (MockHttpSession) customLogin.getRequest().getSession(false);
        assertThat(customSession).isNotNull();

        mockMvc.perform(get("/api/rules").session(customSession))
                .andExpect(status().isForbidden());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/rules/{id}", rule.getId())
                        .session(adminSession)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Reassign users before deleting this rule"));

        Long userId = appUserRepository.findByUsernameIgnoreCase("rule-test").orElseThrow().getId();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/users/{id}", userId)
                        .session(adminSession)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/rules/{id}", rule.getId())
                        .session(adminSession)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isOk());

        assertThat(accessRuleRepository.findByNameIgnoreCase("Night Cashier")).isEmpty();
    }

    @Test
    void logoutIsRejectedWhileTheUserHasAnOpenShift() throws Exception {
        Cookie csrfCookie = csrfCookie();
        MvcResult loginResponse = login(csrfCookie);
        MockHttpSession session = (MockHttpSession) loginResponse.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(post("/api/shifts")
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(APPLICATION_JSON)
                        .content("{\"openingCash\":0}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Close your open shift before signing out"));

        mockMvc.perform(post("/api/shifts/close")
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(APPLICATION_JSON)
                        .content("{\"actualCash\":0}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent());
    }

    private Cookie csrfCookie() throws Exception {
        MvcResult csrfResponse = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie csrfCookie = csrfResponse.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        return csrfCookie;
    }

    private MvcResult login(Cookie csrfCookie) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"" + TEST_ADMIN_USERNAME + "\",\"password\":\"" + TEST_ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
    }
}
