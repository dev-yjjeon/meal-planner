package com.mealplanner.repository;

import com.mealplanner.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    Optional<MenuItem> findByName(String name);
}
