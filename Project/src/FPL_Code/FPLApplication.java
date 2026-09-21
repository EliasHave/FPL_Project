package FPL_Code;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableScheduling
public class FPLApplication {
    public static void main(String[] args) {
        SpringApplication.run(FPLApplication.class, args);
    }

    @Configuration
    public static class WebConfig implements WebMvcConfigurer {

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            // TÄMÄ ON SINUN ALKUPERÄINEN, TOIMIVAKSI TODETTU POLKUSI:
            Path staticDir = Paths.get(System.getProperty("user.dir"), "Project", "resources", "static");

            // Reititetään kaikki nettiselaimen haut suoraan tähän kansioon
            registry.addResourceHandler("/**")
                    .addResourceLocations("file:" + staticDir.toAbsolutePath() + "/");
        }

        @Override
        public void addViewControllers(ViewControllerRegistry registry) {
            // Ohjaa localhost:8080/ suoraan index.html tiedostoon
            registry.addViewController("/").setViewName("forward:/index.html");
        }
    }
}