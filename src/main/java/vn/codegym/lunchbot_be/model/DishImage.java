package vn.codegym.lunchbot_be.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dish_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DishImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    @Column(nullable = false)
    private String imageUrl;

    private String publicId;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    private Boolean isPrimary = false;
}