package com.aurora.iotonenet.config;

import com.aurora.iotonenet.api.controller.*;
import com.aurora.iotonenet.application.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(ApiSecurityTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = {"app.login.username=maintenance-user", "app.login.password=test-only-password"})
class ApiSecurityTest {
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, LoginController.class, DeviceApiController.class, LoginApplicationService.class})
    static class Config {
        @Bean AppProperties appProperties() {
            AppProperties properties = new AppProperties();
            properties.getUserStore().setMode("memory");
            return properties;
        }
        @Bean DeviceStateService deviceStateService() { return mock(DeviceStateService.class); }
        @Bean LedOperationApplicationService ledOperationApplicationService() { return mock(LedOperationApplicationService.class); }
    }
    @Autowired WebApplicationContext context;
    @Autowired LedOperationApplicationService led;
    MockMvc mvc;
    ObjectMapper json = new ObjectMapper();
    record CsrfSession(MockHttpSession session, String header, String token) {}
    @BeforeEach void setUp() {
        reset(led);
        mvc = MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }
    CsrfSession csrf() throws Exception {
        MvcResult result = mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(result.getResponse().getContentAsString());
        return new CsrfSession((MockHttpSession) result.getRequest().getSession(false), token.get("headerName").asText(), token.get("token").asText());
    }
    String body(Object... pairs) throws Exception {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        for (int i=0; i<pairs.length; i+=2) values.put((String) pairs[i], pairs[i+1]);
        return json.writeValueAsString(values);
    }
    @Test void anonymousDeviceControlIsRejectedEvenWithCsrf() throws Exception {
        CsrfSession csrf = csrf();
        mvc.perform(post("/api/ops/led").session(csrf.session()).header(csrf.header(), csrf.token())
            .contentType(MediaType.APPLICATION_JSON).content(body("deviceId", "test-device", "led", true))).andExpect(status().isUnauthorized());
        verifyNoInteractions(led);
    }
    @Test void authenticatedControlRequiresCsrf() throws Exception {
        CsrfSession csrf = csrf();
        csrf.session().setAttribute("logged_in_user", "maintenance-user");
        mvc.perform(post("/api/ops/led").session(csrf.session()).contentType(MediaType.APPLICATION_JSON)
            .content(body("deviceId", "test-device", "led", true))).andExpect(status().isForbidden());
        verifyNoInteractions(led);
    }
    @Test void authenticatedSessionAndCsrfCanControlMockDevice() throws Exception {
        CsrfSession csrf = csrf();
        csrf.session().setAttribute("logged_in_user", "maintenance-user");
        mvc.perform(post("/api/ops/led").session(csrf.session()).header(csrf.header(), csrf.token())
            .contentType(MediaType.APPLICATION_JSON).content(body("deviceId", "test-device", "led", true))).andExpect(status().isAccepted());
        verify(led).setLed(any());
    }
    @Test void loginRotatesSessionAndLogoutInvalidatesIt() throws Exception {
        CsrfSession csrf = csrf();
        String originalId = csrf.session().getId();
        mvc.perform(post("/api/login").session(csrf.session()).header(csrf.header(), csrf.token()).contentType(MediaType.APPLICATION_JSON)
            .content(body("username", "maintenance-user", "password", "test-only-password")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        assertNotEquals(originalId, csrf.session().getId());
        assertEquals("maintenance-user", csrf.session().getAttribute("logged_in_user"));
        mvc.perform(post("/api/logout").session(csrf.session()).header(csrf.header(), csrf.token())).andExpect(status().isOk());
        assertTrue(csrf.session().isInvalid());
        mvc.perform(get("/api/status")).andExpect(status().isUnauthorized());
    }
    @Test void badPasswordDoesNotAuthenticate() throws Exception {
        CsrfSession csrf = csrf();
        mvc.perform(post("/api/login").session(csrf.session()).header(csrf.header(), csrf.token()).contentType(MediaType.APPLICATION_JSON)
            .content(body("username", "maintenance-user", "password", "incorrect")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false));
        assertNull(csrf.session().getAttribute("logged_in_user"));
    }
    @Test void healthIsPublicAndCrossOriginCredentialedLoginIsRejected() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(options("/api/login").header("Origin", "https://untrusted.invalid")
            .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }
}
