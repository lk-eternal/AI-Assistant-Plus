package lk.eternal.ai.job;

import jakarta.annotation.Resource;
import lk.eternal.ai.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AutoDeleteUserJob {

    @Resource
    private UserRepository userRepository;

    @Scheduled(cron = "0 0/1 * * * ?")
    public void run() {
        userRepository.deleteAllByEmailIsNullAndWhenModifiedBefore(LocalDateTime.now().minusMinutes(30));
    }
}
