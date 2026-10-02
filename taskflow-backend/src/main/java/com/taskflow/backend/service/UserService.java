package com.taskflow.backend.service;

import com.taskflow.backend.dto.UserResponse;
import com.taskflow.backend.dto.UpdateProfileRequest;
import com.taskflow.backend.exception.ResourceNotFoundException;
import com.taskflow.backend.model.Role;
import com.taskflow.backend.model.User;
import com.taskflow.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getUsersByRole(Role role) {
        return userRepository.findByRole(role).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToUserResponse(user);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public UserResponse getCurrentUserProfile(String email) {
        User user = getUserByEmail(email);
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateCurrentUserProfile(String email, UpdateProfileRequest request) {
        User user = getUserByEmail(email);
        String cleanName = request.getName() == null ? "" : request.getName().trim();
        user.setName(cleanName);
        return mapToUserResponse(userRepository.save(user));
    }

    public UserResponse mapToUserResponse(User user) {
        if (user == null) return null;
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getCreatedAt()
        );
    }
}
