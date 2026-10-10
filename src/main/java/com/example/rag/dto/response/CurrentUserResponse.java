package com.example.rag.dto.response;

import com.example.rag.security.CurrentUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserResponse {

    private Long id;
    private String username;
    private String nickname;
    private String role;
    private String status;

    public static CurrentUserResponse from(CurrentUser user) {
        return new CurrentUserResponse(
                user.id(),
                user.username(),
                user.nickname(),
                user.role(),
                user.status()
        );
    }
}
