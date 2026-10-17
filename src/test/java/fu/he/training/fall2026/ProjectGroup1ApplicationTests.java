package fu.he.training.fall2026;

import static org.junit.jupiter.api.Assertions.assertFalse;

import fu.he.training.fall2026.controller.preview.UiPageRenderer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class ProjectGroup1ApplicationTests {

    @Autowired
    ApplicationContext context;

    @Test
    void defaultDevelopmentProfileEnablesPreview() {
        assertFalse(
                context.getBeansOfType(UiPageRenderer.class).isEmpty(),
                "The default dev profile must enable UI preview");
    }
}
