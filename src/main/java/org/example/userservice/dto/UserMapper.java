package org.example.userservice.dto;

import org.example.userservice.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAge(),
                user.getCreatedAt()
        );
    }

    public static User toEntity(UserRequest request) {
        return new User(
                request.getName(),
                request.getEmail(),
                request.getAge()
        );
    }
}
