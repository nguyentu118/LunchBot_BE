package vn.codegym.lunchbot_be.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "dishes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"merchant", "categories", "orderItems", "cartItems", "favorites"})
@EqualsAndHashCode(exclude = {"merchant", "categories", "orderItems", "cartItems", "favorites"})
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    @JsonIgnoreProperties({"dishes", "handler", "hibernateLazyInitializer"})
    private Merchant merchant;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "images_urls",columnDefinition = "JSON")
    private String imagesUrls; // JSON array of URLs

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountPrice;

    @Column(precision = 10, scale = 2)
    @ColumnDefault("0.00")
    private BigDecimal serviceFee = BigDecimal.ZERO;

    private Integer preparationTime; // minutes

    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer viewCount = 0;

    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer orderCount = 0;

    @Column(nullable = false)
    private Boolean isRecommended = false;

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "dish", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<DishImage> images = new ArrayList<>();

    // Many-to-Many with Category
    @ManyToMany
    @JoinTable(
            name = "dish_category",
            joinColumns = @JoinColumn(name = "dish_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    @OneToMany(mappedBy = "dish", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "dish", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();

    @OneToMany(mappedBy = "dish", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorite> favorites = new ArrayList<>();

    // Helper method to increment view count
    public void incrementViewCount() {
        this.viewCount++;
    }

    // Helper method to increment order count
    public void incrementOrderCount() {
        this.orderCount++;
    }
}
