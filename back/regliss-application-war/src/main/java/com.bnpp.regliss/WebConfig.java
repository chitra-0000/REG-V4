package com.bnpp.regliss;

import com.bnpp.regliss.web.boot.CustomJacksonObjectMapper;
import com.bnpp.regliss.web.security.RequestContextExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.WebContentInterceptor;

import java.util.List;

@Slf4j
@Configuration
//@EnableSwagger2
@Profile("server")
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addWebRequestInterceptor(requestContextExtractor()).addPathPatterns("/api/**");
        WebContentInterceptor cacheInterceptor = new WebContentInterceptor();
        cacheInterceptor.setCacheControl(CacheControl.noStore());
        cacheInterceptor.setCacheSeconds(0);
        registry.addInterceptor(cacheInterceptor);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/app").setViewName("index");
    }

    @Bean
    public RequestContextExtractor requestContextExtractor() {
        return new RequestContextExtractor();
    }



    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer asyncRequests) {
        asyncRequests.setDefaultTimeout(300_000);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(jsonConverter());
    }
    @Bean
    public MappingJackson2HttpMessageConverter jsonConverter() {
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
        jacksonConverter.setObjectMapper(new CustomJacksonObjectMapper());
        return jacksonConverter;
    }

    @Bean
    public HttpFirewall defaultHttpFirewall() {
        return new DefaultHttpFirewall();
    }

}

