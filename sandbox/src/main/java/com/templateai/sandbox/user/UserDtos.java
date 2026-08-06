package com.templateai.sandbox.user;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserResponse(Long id, String name, String email) {

        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getName(), user.getEmail());
        }
    }
}
