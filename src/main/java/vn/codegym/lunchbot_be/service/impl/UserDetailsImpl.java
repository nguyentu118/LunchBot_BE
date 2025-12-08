package vn.codegym.lunchbot_be.service.impl;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.codegym.lunchbot_be.model.User;

import java.util.Collection;
import java.util.Collections;

public class UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Boolean isActive;

    // Constructor để tạo UserDetailsImpl từ Entity User
    public UserDetailsImpl(Long id, String email, String password,
                           Collection<? extends GrantedAuthority> authorities, Boolean isActive) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.isActive = isActive;
    }

    // Phương thức tĩnh để xây dựng UserDetailsImpl từ Entity User
    public static UserDetailsImpl build(User user) {
        // Chuyển đổi Role (Ví dụ: UserRole.MERCHANT) thành GrantedAuthority
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        return new UserDetailsImpl(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(authority), // Chỉ dùng 1 role
                user.getIsActive() // Giả định có trường isActive
        );
    }

    // --- Các phương thức Getter tùy chỉnh ---

    public Long getId() {
        return id;
    }

    // --- Các phương thức bắt buộc của UserDetails ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email; // Sử dụng email làm username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive; // Dùng trường isActive để kiểm tra trạng thái
    }
}
