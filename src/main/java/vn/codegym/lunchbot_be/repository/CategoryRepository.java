package vn.codegym.lunchbot_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.codegym.lunchbot_be.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
