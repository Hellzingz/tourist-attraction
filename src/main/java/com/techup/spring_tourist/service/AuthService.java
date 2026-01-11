package com.techup.spring_tourist.service;

import com.techup.spring_tourist.dto.JwtResponse;
import com.techup.spring_tourist.entity.User;
import com.techup.spring_tourist.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import com.techup.spring_tourist.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  public void register(String displayName, String email, String password) {
    email = email.toLowerCase();
    if (userRepository.findByEmail(email).isPresent()) {
      throw new RuntimeException("Email already exists");
    }

    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setDisplayName(displayName);
    userRepository.save(user);
  }

  public JwtResponse login(String email, String password) {
    email = email.toLowerCase();
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found"));

    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new RuntimeException("Invalid credentials");
    }

    String token = jwtService.generateToken(user.getEmail());
    return new JwtResponse(token, user.getEmail(), user.getDisplayName());
  }
}