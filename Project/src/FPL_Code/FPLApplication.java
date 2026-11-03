package FPL_Code;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
            // Luetaan static-tiedostot JAR-paketin sisältä (classpath), EI kovalevyltä (file:)
            registry.addResourceHandler("/**")
                    .addResourceLocations("classpath:/static/");
        }

        @Override
        public void addViewControllers(ViewControllerRegistry registry) {
            // Ohjaa juuriosoite (/) suoraan index.html-tiedostoon
            registry.addViewController("/").setViewName("forward:/index.html");
        }
    }
}