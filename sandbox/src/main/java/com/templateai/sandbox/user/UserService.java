package com.templateai.sandbox.user;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.templateai.sandbox.common.ApiException;
import com.templateai.sandbox.user.UserDtos.UserResponse;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    public List<UserResponse> list() {
        return users.findAllByOrderByNameAsc().stream().map(UserResponse::from).toList();
    }

    public UserResponse get(Long id) {
        return UserResponse.from(find(id));
    }

    /** Other features resolve the owning {@link User} entity (or confirm it exists) through this. */
    public User find(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User " + id + " not found"));
    }
}
