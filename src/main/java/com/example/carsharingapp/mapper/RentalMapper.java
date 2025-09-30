package com.example.carsharingapp.mapper;

import com.example.carsharingapp.config.MapperConfig;
import com.example.carsharingapp.dto.rental.AddRentalRequestDto;
import com.example.carsharingapp.dto.rental.RentalDto;
import com.example.carsharingapp.model.Rental;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface RentalMapper {
    RentalDto toDto(Rental rental);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "car", ignore = true)
    @Mapping(target = "rentalDate", ignore = true)
    @Mapping(target = "actualReturnDate", ignore = true)
    Rental toModel(AddRentalRequestDto requestDto);
}
