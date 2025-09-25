package com.example.carsharingapp.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@NotBlank(message = "Password cannot be blank")
@Size(min = 8, max = 30, message = "Password must be between 8 and 30 characters long")
@Pattern(regexp =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,30}$",
        message = "Password must be 8-30 characters and include at least one uppercase, lowercase,"
                + " number, and a special symbol (e.g., !@#$).")
public @interface Password {
    String message() default "invalid password format or length";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
