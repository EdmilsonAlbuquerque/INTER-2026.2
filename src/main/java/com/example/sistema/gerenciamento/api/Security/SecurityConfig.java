package com.example.sistema.gerenciamento.api.Security;

import com.example.sistema.gerenciamento.api.Repository.UsuarioRepository;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    UserDetailsService userDetailsService(UsuarioRepository repository) {
        return email -> new UsuarioAutenticado(repository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas")));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioRepository repository,
            UserDetailsService users, PasswordEncoder encoder,
            @Value("${app.security.remember-key:}") String rememberKey) throws Exception {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        http.authenticationProvider(provider);
        http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                .requestMatchers("/", "/institucional", "/index.html", "/quem-somos", "/nossas-acoes", "/nossas-acoes.html", "/blog", "/institucional/**",
                        "/login", "/cadastro-usuario", "/recuperar-senha", "/redefinir-senha",
                        "/js/**", "/css/**", "/favicon.ico", "/error", "/acesso-negado").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/cadastro", "/auth/recuperar", "/auth/redefinir").permitAll()
                .requestMatchers("/usuarios", "/usuarios/**", "/admin", "/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/cadastro-pessoa").hasAnyRole("ADMINISTRADOR", "OPERADOR")
                .requestMatchers("/auth/me", "/auth/me/foto", "/gerenciamento", "/meu-perfil", "/servicos").authenticated()
                .requestMatchers("/financeiro", "/DASHFINACEIRO.html", "/api/financeiro/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/api/**", "/historicos-status", "/historicos-status/**")
                    .hasAnyRole("ADMINISTRADOR", "OPERADOR", "LEITOR")
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/**", "/historicos-status", "/historicos-status/**")
                    .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                .requestMatchers(HttpMethod.PUT, "/api/**").hasAnyRole("ADMINISTRADOR", "OPERADOR")
                .requestMatchers(HttpMethod.PATCH, "/api/**").hasAnyRole("ADMINISTRADOR", "OPERADOR")
                .anyRequest().denyAll());
        http.formLogin(form -> form.loginPage("/login").usernameParameter("email")
                .defaultSuccessUrl("/gerenciamento", true).failureUrl("/login?erro").permitAll());
        http.logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?saiu")
                .deleteCookies("JSESSIONID", "remember-me"));
        http.rememberMe(remember -> remember.key(rememberKey.isBlank() ? UUID.randomUUID().toString() : rememberKey)
                .userDetailsService(users).tokenValiditySeconds(7 * 24 * 3600));
        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> {
                    if (isApi(request)) jsonError(response, 401, "Entre no sistema para continuar.");
                    else new LoginUrlAuthenticationEntryPoint("/login").commence(request, response, exception);
                })
                .accessDeniedHandler((request, response, exception) -> {
                    if (isApi(request)) jsonError(response, 403, "Acesso negado ou sessão expirada. Atualize a página.");
                    else response.sendRedirect("/acesso-negado");
                }));
        // Revalida no banco a cada requisição: bloquear/trocar perfil/senha revoga sessões existentes.
        http.addFilterAfter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                    FilterChain chain) throws ServletException, IOException {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado principal) {
                    var current = repository.findById(principal.getId());
                    if (current.isEmpty() || !current.get().isAtivo()
                            || current.get().getVersaoAcesso() != principal.getVersao()) {
                        SecurityContextHolder.clearContext();
                        if (request.getSession(false) != null) request.getSession(false).invalidate();
                        var cookie = new jakarta.servlet.http.Cookie("remember-me", "");
                        cookie.setPath("/"); cookie.setMaxAge(0); cookie.setHttpOnly(true);
                        response.addCookie(cookie);
                    }
                }
                chain.doFilter(request, response);
            }
        }, RememberMeAuthenticationFilter.class);
        return http.build();
    }

    private static boolean isApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/") || request.getRequestURI().startsWith("/auth/")
                || request.getRequestURI().startsWith("/usuarios") || request.getRequestURI().startsWith("/historicos-status")
                || request.getRequestURI().startsWith("/admin/api/");
    }

    private static void jsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
