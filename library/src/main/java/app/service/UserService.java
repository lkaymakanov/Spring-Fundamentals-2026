package app.service;

import app.model.entity.User;
import app.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepo;

    @Autowired
    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public User getById(UUID id) {
        User user = userRepo.findById(id)
                .orElseThrow(
                        () -> new RuntimeException("User with id [%s] does not exist.".formatted(id)));
        return  user; //UserMapper.toUserDto(user);
    }


    public User getUserById(UUID id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public User saveUser(User user) {
        return userRepo.save(user);
    }

    public void deleteUser(UUID id) {
        userRepo.deleteById(id);
    }
}