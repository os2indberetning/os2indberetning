package dk.digitalidentity.indberetning.controller.rest;

import dk.digitalidentity.indberetning.TestContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tests to ensure that our RestControllers are protected with the right annotations
@AutoConfigureMockMvc
@Import(TestContainerConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RestControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void failsWhenUnsecure() throws Exception {
        assertUrlProtected("/rest/admin");
        assertUrlProtected("/rest/plate");
        assertUrlProtected("/rest/appLogin");
        assertUrlProtected("/rest/report");
        assertUrlProtected("/reportCard");
        assertUrlProtected("/rest/personal");
        assertUrlProtected("/rest/mail");
        assertUrlProtected("/rest/personalAddress");
        assertUrlProtected("/rest/personalRoutes");
        assertUrlProtected("/rest/notification");
        assertUrlProtected("/rest/approve");
        assertUrlProtected("/rest/deadline");
        assertUrlProtected("/rest/rate");
        assertUrlProtected("/rest/rType");
    }

    @Test
    void isMissingAnnotation() {
        Reflections reflections = new Reflections("dk.digitalidentity");
        Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(Controller.class);
        List<String> classesMissingAnnotations = new ArrayList<>();
        for (Class<?> controller : controllers) {
            boolean match = false;
            boolean noRoleRequired = false;
            for (Annotation annotation : controller.getAnnotations()) {
                if (Objects.equals(annotation.annotationType().getSimpleName(), "NoRoleRequired")) {
                    noRoleRequired = true;
                    break;
                }

                if (annotation.annotationType().getSimpleName().startsWith("Require")) {
                    match = true;
                }
            }
            if (!noRoleRequired && !match) {
                classesMissingAnnotations.add(controller.getSimpleName());
            }
        }
        assertTrue(classesMissingAnnotations.isEmpty(), "The following classes are missing security annotations: " + classesMissingAnnotations);
    }

    // The expectation is that we should be redirected to login
    public void assertUrlProtected(final String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/saml2/authenticate/IdP"));
    }
}
