package ir.mtn.nwg.controllers;

import ir.mtn.nwg.models.Role;
import ir.mtn.nwg.models.User;
import ir.mtn.nwg.payload.request.UserRequest;
import ir.mtn.nwg.payload.request.UserRequest2;
import ir.mtn.nwg.payload.response.MessageResponse;
import ir.mtn.nwg.repository.RoleRepository;
import ir.mtn.nwg.repository.UserRepository;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final RoleRepository roleRepository;

    public UserController(UserRepository userRepository, PasswordEncoder encoder, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.roleRepository = roleRepository;
    }

    @GetMapping("")
    public ResponseEntity<List<User>> allUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable long id) {
        return ResponseEntity.ok(userRepository.findById(id)
                .<RuntimeException>orElseThrow(() -> new RuntimeException("Error: User is not found.")));
    }

    @PostMapping("")
    @Secured({"ROLE_ADMIN"})
    public ResponseEntity<?> createUser(@Valid @RequestBody UserRequest userRequest) {
        if (userRepository.existsByUsername(userRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(userRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(userRequest.getUsername(),
                userRequest.getEmail(),
                encoder.encode(userRequest.getPassword()));

        if (userRequest.getRoles() != null && !userRequest.getRoles().isEmpty()) {
            Set<String> strRoles = new HashSet<>(Arrays.asList(userRequest.getRoles().split(",")));
            user.setRoles(getRolesByUser(strRoles));
        }

        userRepository.save(user);

        return ResponseEntity.ok(user);
    }

    private Set<Role> getRolesByUser(Set<String> strRoles) {
        Set<Role> roles = new HashSet<>();

        if (strRoles.isEmpty()) {
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                Role userRole = roleRepository.findByName(role)
                        .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                roles.add(userRole);
            });
        }
        return roles;
    }

    @DeleteMapping("/{id}")
    @Secured({"ROLE_ADMIN"})
    public ResponseEntity<?> deleteUser(@PathVariable long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (!optionalUser.isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: id does not exist!"));
        }

        User user = optionalUser.get();


        userRepository.delete(user);

        return ResponseEntity.ok(new MessageResponse("User deleted successfully!"));
    }

    @PutMapping("")
    @Secured({"ROLE_ADMIN"})
    public ResponseEntity<?> updateUser(@Valid @RequestBody UserRequest2 userRequest) {
        Optional<User> optionalUser = userRepository.findByUsername(userRequest.getUsername());
        if (!optionalUser.isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username does not exist!"));
        }

        User user = optionalUser.get();

        if (!user.getEmail().equals(userRequest.getEmail())) {
//            return ResponseEntity
//                    .badRequest()
//                    .body(new MessageResponse("Error: Email can't be changed!"));
            user.setEmail(userRequest.getEmail());
        }

        if (userRequest.getRoles() != null) {
            Set<String> strRoles = new HashSet<>(userRequest.getRoles());
            user.setRoles(getRolesByUser(strRoles));
        }

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User updated successfully!"));
    }

    @PostMapping("/password/{id}")
    @Secured({"ROLE_ADMIN"})
    public ResponseEntity<?> updateUserPassword(@PathVariable long id, @RequestBody String passwordJson) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (!optionalUser.isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Wrong Id"));
        }
        User user = optionalUser.get();

        JSONObject jsonObject = new JSONObject(passwordJson);
        String password = jsonObject.getString("password");
        if (password != null && password.length() > 5 && password.length() < 41) {
            user.setPassword(encoder.encode(password));
            userRepository.save(user);
            return ResponseEntity.ok(new MessageResponse("Password updated successfully!"));
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Password length must be between 6 and 40"));
        }
    }
}
