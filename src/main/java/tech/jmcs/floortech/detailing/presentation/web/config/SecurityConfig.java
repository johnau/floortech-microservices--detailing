package tech.jmcs.floortech.detailing.presentation.web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import tech.jmcs.floortech.common.auth.FloortechAuthenticationManager;
import tech.jmcs.floortech.common.auth.FloortechSecurityContextRepository;
import tech.jmcs.floortech.common.helper.JwtHelper;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {
    @Autowired
    private FloortechAuthenticationManager authenticationManager;
    @Autowired
    private FloortechSecurityContextRepository securityContextRepository;

    @Bean
    public SecurityWebFilterChain configure(ServerHttpSecurity http) {
//        http.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable);
        http.httpBasic().disable();
        http.formLogin().disable();
        http.logout().disable();
        http.csrf().disable();
//        http.csrf(ServerHttpSecurity.CsrfSpec::disable);
        http.authenticationManager(authenticationManager);
        http.securityContextRepository(securityContextRepository);
        http.authorizeExchange().anyExchange().authenticated();
        return http.build();

//        http.exceptionHandling()
//                .authenticationEntryPoint((swe, e) ->
//                        Mono.fromRunnable(() -> swe.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED))
//                ).accessDeniedHandler((swe, e) ->
//                        Mono.fromRunnable(() -> swe.getResponse().setStatusCode(HttpStatus.FORBIDDEN))
//                );

//        http.authenticationManager(reactiveAuthenticationManager);
//        http.securityContextRepository(NoOpServerSecurityContextRepository.getInstance());

//        http.headers()
//            .frameOptions().disable()
//            .cache().disable();
//        http.addFilterAt(authWebFilter(), SecurityWebFiltersOrder.HTTP_BASIC);

//        http.authorizeExchange(it -> it
//                .pathMatchers("/me").authenticated()
//                .pathMatchers("/users/{user}/**").access(this::currentUserMatchesPath)
//                .anyExchange().permitAll()
//        )
    }
}