package it.zenflow.master.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class MasterSecurityConfig {

    private final PasswordEncoder passwordEncoder;

    public MasterSecurityConfig(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public UserDetailsService masterUserDetailsService() {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        manager.createUser(User.withUsername("admin")
                .password("$2a$10$CaRdS0qzQVo8Qe7CM.DZIeGUTlPHeYefedawhUjcre5fas7BLKpnS")
                .roles("MASTER_ADMIN")
                .build());
        return manager;
    }

    @Bean
    @Order(1) // Higher priority than the main security configuration
    public SecurityFilterChain masterSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .userDetailsService(masterUserDetailsService())
            .securityMatcher("/master/**") // Apply this configuration only to /master/** paths
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/master/login", "/master/css/**", "/master/js/**", "/master/images/**").permitAll()
                .anyRequest().hasRole("MASTER_ADMIN")
            )
            .formLogin(form -> form
                .loginPage("/master/login")
                .loginProcessingUrl("/master/login")
                .defaultSuccessUrl("/master/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/master/logout")
                .logoutSuccessUrl("/master/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            );

        return http.build();
    }
}