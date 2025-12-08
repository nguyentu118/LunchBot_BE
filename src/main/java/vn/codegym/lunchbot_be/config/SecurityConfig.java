package vn.codegym.lunchbot_be.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.codegym.lunchbot_be.filter.JwtAuthenticationFilter;
import vn.codegym.lunchbot_be.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Tắt CSRF cho các API không cần bảo vệ chống CSRF (thường là POST/PUT/DELETE API công khai)
                .csrf(csrf -> csrf.disable())

                // 2. Cấu hình quyền truy cập (Authorization)
                .authorizeHttpRequests(authorize -> authorize
                        // Cho phép tất cả các request POST đến endpoint đăng ký Merchant mà không cần xác thực
                        .requestMatchers("/api/auth/register/merchant").permitAll()

                        // THÊM DÒNG NÀY: Cho phép truy cập admin API mà không cần xác thực (CHỈ DÙNG CHO TEST)
                        .requestMatchers("/api/admin/**").permitAll()  // <-- THÊM DÒNG NÀY

                        // Cho phép các request GET công khai khác (ví dụ: đăng nhập, swagger)
                        .requestMatchers("/api/auth/**", "/public/**").permitAll()

                        // Tất cả các request khác phải được xác thực
                        .anyRequest().authenticated()
                )

                // 3. Tắt HTTP Basic hoặc Form login mặc định nếu bạn dùng JWT
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable());

        return http.build();
    }

    // Đảm bảo bạn định nghĩa PasswordEncoder bean (ví dụ: BCryptPasswordEncoder)
    // ...
}