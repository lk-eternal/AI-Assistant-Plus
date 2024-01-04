package lk.eternal.ai.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
@EntityListeners(BaseEntity.EntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private final UUID id;

    @Column(name = "when_created", nullable = false)
    private LocalDateTime whenCreated;

    @Column(name = "when_modified")
    private LocalDateTime whenModified;

    public BaseEntity(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getWhenCreated() {
        return whenCreated;
    }

    public void setWhenCreated(LocalDateTime whenCreated) {
        this.whenCreated = whenCreated;
    }

    public LocalDateTime getWhenModified() {
        return whenModified;
    }

    public void setWhenModified(LocalDateTime whenModified) {
        this.whenModified = whenModified;
    }

    public static class EntityListener {

        @PrePersist
        public void prePersist(Object entity) {
            LocalDateTime now = LocalDateTime.now();
            if (entity instanceof BaseEntity baseEntity) {
                baseEntity.setWhenCreated(now);
                baseEntity.setWhenModified(now);
            }
        }

        @PreUpdate
        public void preUpdate(Object entity) {
            if (entity instanceof BaseEntity baseEntity) {
                baseEntity.setWhenModified(LocalDateTime.now());
            }
        }
    }
}