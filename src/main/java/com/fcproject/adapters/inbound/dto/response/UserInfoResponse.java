package com.fcproject.adapters.inbound.dto.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserInfoResponse {
    private long id;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;
    private String email;
    private String phone;
    private String gender;

    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;


}
