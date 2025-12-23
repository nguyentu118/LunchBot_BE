package vn.codegym.lunchbot_be.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "orders"})
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private String contactName;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String province;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String ward;

    @Column(nullable = false)
    private String street;

    private String building;

    @Column(nullable = false)
    private Boolean isDefault = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "shippingAddress")
    private List<Order> orders = new ArrayList<>();

    @Column(name = "province_id")
    private Integer provinceId; // ID Tỉnh/Thành từ GHN

    @Column(name = "district_id")
    private Integer districtId; // ID Quận/Huyện từ GHN

    @Column(name = "ward_code")
    private String wardCode;    // Mã Phường/Xã từ GHN
}
