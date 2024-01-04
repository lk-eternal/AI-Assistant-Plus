package lk.eternal.ai.repository;

import jakarta.transaction.Transactional;
import lk.eternal.ai.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // 查询
    Optional<User> findByEmail(String email);

    @Transactional
    @Modifying
    @Query("DELETE FROM User u WHERE u.email IS NULL AND u.whenModified < :beforeWhenModified")
    void deleteAllByEmailIsNullAndWhenModifiedBefore(@Param("beforeWhenModified") LocalDateTime beforeWhenModified);
}
