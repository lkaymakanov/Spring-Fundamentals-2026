package app.service;

import app.exception.InvalidCredentialsException;
import app.exception.InvalidPasswordException;
import app.exception.UserNotFoundException;
import app.exception.UsernameAlreadyExistsException;
import app.mapper.UserMapper;
import app.model.dto.ChangePasswordRequest;
import app.model.dto.UserDto;
import app.model.dto.UserLoginRequest;
import app.model.dto.UserRegisterRequest;
import app.model.entity.User;
import app.model.entity.UserRole;
import app.repository.UserRepository;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles user account operations: registration, authentication, password changes,
 * and CRUD on user entities.
 *
 * Security responsibilities:
 *  - Passwords are always hashed with BCrypt before persisting (never stored in plaintext).
 *  - Login compares submitted password against the stored hash using passwordEncoder.matches().
 *  - Password change requires verification of the current password.
 */
@Service
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Changes a user's password after validating all security rules.
     *
     * Enforces:
     *  1. User must exist.
     *  2. Submitted current password must match the stored hash.
     *  3. New password must differ from the current one.
     *  4. New password and confirmation must match.
     *
     * @throws UserNotFoundException      if the user ID doesn't exist
     * @throws InvalidCredentialsException if the current password is wrong
     * @throws InvalidPasswordException   if rule 3 or 4 is violated
     */
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Rule 2: verify the user actually knows the current password.
        // matches() safely compares plaintext against the stored hash — no leaks.
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        // Rule 3: reject "change to same password" — it's a useless operation that signals a UX confusion.
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new InvalidPasswordException("New password must be different from the current one");
        }

        // Rule 4: confirm field must match — catches typos before the user is locked out.
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidPasswordException("Passwords do not match");
        }

        // Encode and persist. BCrypt auto-generates a unique salt for each hash.
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepo.save(user);
    }

    /** Returns all users. Kept for admin/management use cases. */
    public List<User> findAll() {
        return userRepo.findAll().stream().collect(Collectors.toList());
    }

    /**
     * Looks up a user by ID and converts the entity to a DTO.
     * Use this when sending user data to the view layer (controllers).
     */
    public UserDto getById(UUID id) {
        User user = userRepo.findById(id)
                .orElseThrow(
                        () -> new RuntimeException("User with id [%s] does not exist.".formatted(id)));
        return UserMapper.toUserDto(user);
    }

    /**
     * Returns the raw User entity. Use this when other services need to set
     * relationships (e.g. BorrowRecord.user). For view-layer data, prefer getById().
     */
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

    /** Alias for findAll() — duplicate kept for naming clarity at call sites. */
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    /** Persists a User entity (used by admin endpoints for direct updates). */
    public User saveUser(User user) {
        return userRepo.save(user);
    }

    /** Deletes a user by ID. Caller is responsible for any cascading rules. */
    public void deleteUser(UUID id) {
        userRepo.deleteById(id);
    }

    /**
     * Authenticates a user with username + password.
     * On success returns a UserDto for the controller to put into the session.
     *
     * @throws InvalidCredentialsException if username doesn't exist OR password is wrong.
     *         Both cases share the same message to avoid username enumeration attacks.
     */
    public UserDto login(UserLoginRequest userLoginRequest) {
        Optional<User> optionalUser = userRepo.findByUsername(userLoginRequest.getUsername());

        // Same error message whether username or password is wrong — prevents attackers
        // from figuring out which usernames exist by probing the login endpoint.
        if (optionalUser.isEmpty() || !passwordEncoder.matches(userLoginRequest.getPassword(), optionalUser.get().getPassword())) {
            throw new InvalidCredentialsException("Username or password mismatch!");
        }

        return UserMapper.toUserDto(optionalUser.get());
    }

    /**
     * Creates a new user account.
     * Enforces:
     *  - Username uniqueness
     *  - Email uniqueness
     *  - Password is hashed before storage (never stored as plaintext)
     *  - Default role is USER (admins must be promoted manually)
     *
     * @throws UsernameAlreadyExistsException if the username or email is already taken
     */
    public void register(@Valid UserRegisterRequest userRegisterRequest) {
        if (userRepo.findByUsername(userRegisterRequest.getUsername()).isPresent()) {
            throw new UsernameAlreadyExistsException("Username already exists");
        }

        if (userRepo.findByEmail(userRegisterRequest.getEmail()).isPresent()) {
            throw new UsernameAlreadyExistsException("User with that email already exists");
        }

        User u = new User();
        u.setUsername(userRegisterRequest.getUsername());
        // Hash the password — never persist the raw value from the request.
        u.setPassword(passwordEncoder.encode(userRegisterRequest.getPassword()));
        u.setRole(UserRole.USER);
        u.setEmail(userRegisterRequest.getEmail());
        u.setCreatedAt(LocalDateTime.now());
        userRepo.save(u);
    }
}