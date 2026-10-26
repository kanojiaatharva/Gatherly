package com.gatherly.auth;

import com.gatherly.common.BadRequestException;
import com.gatherly.common.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String cleanEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new BadRequestException("An account with this email already exists");
        }

        String displayName = request.displayName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = cleanEmail.split("@")[0];
        }

        User user = new User(cleanEmail, passwordEncoder.encode(request.password()), displayName.trim());
        User savedUser = userRepository.save(user);

        String token = jwtTokenService.generateToken(savedUser);
        return new AuthResponse(token, UserDto.fromEntity(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String cleanEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        String token = jwtTokenService.generateToken(user);
        return new AuthResponse(token, UserDto.fromEntity(user));
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UserPrincipal principal) {
        if (principal == null) {
            throw new com.gatherly.common.UnauthorizedException("No authenticated user in current session");
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserDto.fromEntity(user);
    }
}
