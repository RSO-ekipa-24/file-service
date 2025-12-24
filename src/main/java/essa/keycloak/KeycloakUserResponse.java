package essa.keycloak;

import io.smallrye.common.constraint.NotNull;

public class KeycloakUserResponse {
    private final String username;
    private final String firstName;
    private final String lastName;
    private final String email;

    public KeycloakUserResponse(
            @NotNull String username,
            @NotNull String firstName,
            @NotNull String lastName,
            @NotNull String email
    ) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    @NotNull
    public String getUsername() {
        return username;
    }

    @NotNull
    public String getFirstName() {
        return firstName;
    }

    @NotNull
    public String getLastName() {
        return lastName;
    }

    @NotNull
    public String getEmail() {
        return email;
    }
}
