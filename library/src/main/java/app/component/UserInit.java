package app.component;

//import app.model.dto.user.UserDto;
//import app.model.dto.user.UserRegisterRequest;
//import app.model.entity.user.Country;
import app.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserInit implements CommandLineRunner {

    private final UserService userService;

    public UserInit(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Entered in run " + this.getClass());
        userService.findAll();
        /*List<UserDto> users = userService.findAll();
        if (!users.isEmpty()) {
            return;
        }

        UserRegisterRequest userRegisterRequest = UserRegisterRequest.builder()
                .username("defaultUser")
                .password("defaultPassword")
                .country(Country.BULGARIA)
                .build();

        //TODO: create registerAdmin method
        userService.register(userRegisterRequest);

        log.info("Default user created with username [%s] and password [%s].".formatted(
                userRegisterRequest.getUsername(), userRegisterRequest.getPassword()));*/
    }
}