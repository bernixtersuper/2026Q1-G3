package com.menudigital.application.tenant;

import com.menudigital.application.tenant.dto.RegisterRestaurantCommand;
import com.menudigital.application.tenant.dto.RegisterRestaurantResponse;
import com.menudigital.domain.tenant.Restaurant;
import com.menudigital.domain.tenant.RestaurantRepository;
import com.menudigital.infrastructure.persistence.UserRepositoryImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RegisterRestaurantUseCase {

    @Inject
    RestaurantRepository restaurantRepository;

    @Inject
    UserRepositoryImpl userRepository;

    @Transactional
    public RegisterRestaurantResponse execute(RegisterRestaurantCommand command) {
        if (restaurantRepository.existsBySlug(command.slug())) {
            throw new SlugAlreadyExistsException("Slug '" + command.slug() + "' is already taken");
        }

        if (userRepository.existsByEmail(command.ownerEmail())) {
            throw new EmailAlreadyExistsException("Email '" + command.ownerEmail() + "' is already registered");
        }

        Restaurant restaurant = Restaurant.create(
            command.restaurantName(),
            command.slug(),
            command.ownerEmail()
        );
        restaurantRepository.save(restaurant);

        userRepository.createUser(command.ownerEmail(), command.cognitoSub(), restaurant.getId().value());

        return new RegisterRestaurantResponse(
            restaurant.getId().toString(),
            restaurant.getName()
        );
    }

    public static class SlugAlreadyExistsException extends RuntimeException {
        public SlugAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String message) {
            super(message);
        }
    }
}
