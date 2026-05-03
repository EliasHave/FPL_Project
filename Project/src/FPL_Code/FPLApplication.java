package FPL_Code;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class FPLApplication {
    public static void main(String[] args) {
        SpringApplication.run(FPLApplication.class, args);
    }

    @Configuration
    public static class WebConfig implements WebMvcConfigurer {
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            // Staattinen sisältö (index.html, styles.css jne.)
            Path staticDir = Paths.get(System.getProperty("user.dir"), "Project", "resources", "static");
            registry.addResourceHandler("/static/**")
                    .addResourceLocations("file:" + staticDir.toAbsolutePath() + "/");

            // Dynaamiset suunnitelmat temp-kansiosta
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "fpl-suunnitelmat");
            registry.addResourceHandler("/suunnitelma_*")
                    .addResourceLocations("file:" + tempDir.toAbsolutePath() + "/")
                    .setCacheControl(CacheControl.noCache());
        }
    }
}