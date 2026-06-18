package app.service;

import app.exception.InvalidCredentialsException;
import app.exception.UsernameAlreadyExistsException;
import app.mapper.UserMapper;
import app.model.dto.UserDto;
import app.model.dto.UserLoginRequest;
import app.model.dto.UserRegisterRequest;
import app.model.entity.User;
import app.model.entity.UserRole;
import app.repository.UserRepository;

import jakarta.validation.Valid;
import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepo,   PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }



    public List<User> findAll() {
        return userRepo.findAll().stream().collect(Collectors.toList());
    }


    public UserDto getById(UUID id) {
        User user = userRepo.findById(id)
                .orElseThrow(
                        () -> new RuntimeException("User with id [%s] does not exist.".formatted(id)));
        return  UserMapper.toUserDto(user);
    }


    public User getUserById(UUID id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /*public User registerUser(User user) {
        if (userRepo.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(UserRole.USER);

        return userRepo.save(user);
    }*/

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public User saveUser(User user) {
        return userRepo.save(user);
    }

    public void deleteUser(UUID id) {
        userRepo.deleteById(id);
    }

    public UserDto login(UserLoginRequest userLoginRequest) {
        Optional<User> optionalUser =  userRepo.findByUsername(userLoginRequest.getUsername());

        if (optionalUser.isEmpty() || !passwordEncoder.matches(userLoginRequest.getPassword(), optionalUser.get().getPassword()))
        {
            throw new InvalidCredentialsException("Username or password mismatch!");
        }

        return UserMapper.toUserDto(optionalUser.get());
    }

    public void register(@Valid UserRegisterRequest userRegisterRequest) {
        if (userRepo.findByUsername(userRegisterRequest.getUsername()).isPresent()) {
            throw new UsernameAlreadyExistsException("Username already exists");
        }
        User u = new User();
        u.setUsername(userRegisterRequest.getUsername());
        u.setPassword(passwordEncoder.encode(userRegisterRequest.getPassword()));
        u.setRole(UserRole.USER);
        u.setEmail(userRegisterRequest.getEmail());
        u.setCreatedAt(LocalDateTime.now());
        userRepo.save(u);
    }
}