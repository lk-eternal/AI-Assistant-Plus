package lk.eternal.ai.domain;

import jakarta.annotation.Resource;
import lk.eternal.ai.repository.UserRepository;
import lk.eternal.ai.util.Mapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

@SpringBootTest
@ActiveProfiles("local")
class UserTest {

    @Resource
    UserRepository userRepository;

    @Test
    public void test() {
        userRepository.findByEmail("123123@qwe.com").ifPresent(userRepository::delete);

        final var entity = new User();
        entity.setEmail("123123@qwe.com");
        userRepository.save(entity);
        System.out.printf(Mapper.writeAsStringNotError(userRepository.findByEmail("123123@qwe.com").orElse(null)));
        userRepository.delete(entity);
    }

    @Test
    public void testJob() throws InterruptedException {

        final var entity = new User();
        userRepository.save(entity);

        System.out.printf(Mapper.writeAsStringNotError(userRepository.findById(entity.getId()).orElse(null)));
        Thread.sleep(1000);
        userRepository.deleteAllByEmailIsNullAndWhenModifiedBefore(LocalDateTime.now());
        System.out.printf(Mapper.writeAsStringNotError(userRepository.findById(entity.getId()).orElse(null)));
    }
}