package vn.codegym.lunchbot_be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
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