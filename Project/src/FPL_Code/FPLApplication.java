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
            Path staticDir = Paths.get(System.getProperty("user.dir"), "Project", "resources", "static");
            registry.addResourceHandler("/**")
                    .addResourceLocations("file:" + staticDir.toAbsolutePath() + "/")
                    .setCacheControl(CacheControl.noCache());
        }
    }
}