package it.zenflow.master.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MasterWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configure static resources specific to the master dashboard
        registry.addResourceHandler("/master/css/**")
                .addResourceLocations("classpath:/static/master/css/");
        registry.addResourceHandler("/master/js/**")
                .addResourceLocations("classpath:/static/master/js/");
        registry.addResourceHandler("/master/images/**")
                .addResourceLocations("classpath:/static/master/images/");
    }
}