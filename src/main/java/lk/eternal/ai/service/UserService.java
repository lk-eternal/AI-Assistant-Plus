package lk.eternal.ai.service;

import lk.eternal.ai.domain.User;
import lk.eternal.ai.repository.UserRepository;
import lk.eternal.ai.util.Assert;
import lk.eternal.ai.util.PasswordUtil;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getUser(UUID userId) {
        return Optional.ofNullable(userId)
                .flatMap(userRepository::findById);
    }

    public User getOrCreateUser(UUID userId) {
        return Optional.ofNullable(userId)
                .flatMap(userRepository::findById)
                .orElseGet(() -> {
                    final var newUser = new User();
                    userRepository.save(newUser);
                    return newUser;
                });
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    public Optional<User> getUserByEmail(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(user -> PasswordUtil.validatePassword(password, user.getPassword()));
    }

    public void createUser(User user, String email, String password) {
        Assert.isTrue(userRepository.findByEmail(email).isEmpty(), "该邮箱已注册");
        user.setEmail(email);
        user.setPassword(password);
        userRepository.save(user);
    }

    public void updateUser(User updatedUser) {
        userRepository.save(updatedUser);
    }


    public void deleteUser(UUID userId) {
        userRepository.deleteById(userId);
    }

}
