package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtFilter;

    public SecurityConfig(JwtAuthFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth             		
                		.requestMatchers(
                			    "/swagger-ui/**",
                			    "/v3/api-docs/**",
                			    "/swagger-resources/**",
                			    "/swagger-ui.html",
                			    "/webjars/**",
                			    "/login/**",
                			    "/admintool/**"
                			).permitAll()

                			// Blogs
                			.requestMatchers(HttpMethod.POST, "/blogs/**").hasAnyRole("EDITOR", "ADMIN")
                			.requestMatchers(HttpMethod.PUT, "/blogs/**").hasAnyRole("EDITOR", "ADMIN")
                			.requestMatchers(HttpMethod.DELETE, "/blogs/**").hasAnyRole("EDITOR", "ADMIN") 
                			.requestMatchers(HttpMethod.GET, "/blogs/**").permitAll()

                			// Users
                			.requestMatchers(HttpMethod.PUT, "/users/**").hasAnyRole("EDITOR", "ADMIN") 
                			.requestMatchers(HttpMethod.GET, "/users/**").permitAll() 
                			.requestMatchers(HttpMethod.DELETE, "/users/**").hasAnyRole("EDITOR", "ADMIN")
                			.requestMatchers("/users/register").permitAll() 

                			// Categories
                			.requestMatchers(HttpMethod.POST, "/categories/**").hasAnyRole("EDITOR", "ADMIN") 
                			.requestMatchers(HttpMethod.DELETE, "/categories/**").hasRole( "ADMIN") 
                			.requestMatchers(HttpMethod.GET, "/categories/**").permitAll()

                			// Comments
                			.requestMatchers(HttpMethod.POST, "/comments/**").hasAnyRole("USER", "EDITOR", "ADMIN") 
                			.requestMatchers(HttpMethod.PUT, "/comments/**").hasAnyRole("USER", "EDITOR", "ADMIN") 
                			.requestMatchers(HttpMethod.DELETE, "/comments/**").hasAnyRole("EDITOR", "ADMIN","USER")
                			.requestMatchers(HttpMethod.GET, "/comments/**").permitAll()

                			// GraphQL
                			.requestMatchers("/graphql/**", "/graphiql/**").permitAll() 
                			
               

                			.anyRequest().authenticated()
                			)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

