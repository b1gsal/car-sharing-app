package com.github.b1gsal.carsharingservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;
import org.springframework.beans.BeanWrapperImpl;

public class FieldMatchValidator implements ConstraintValidator<FieldMatch, Object> {
    private String firstFieldName;
    private String secondFieldName;

    @Override
    public void initialize(FieldMatch constraintAnnotation) {
        firstFieldName = constraintAnnotation.first();
        secondFieldName = constraintAnnotation.second();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            Object propertyValue1 = new BeanWrapperImpl(value).getPropertyValue(firstFieldName);
            Object propertyValue2 = new BeanWrapperImpl(value).getPropertyValue(secondFieldName);

            return Objects.equals(propertyValue1, propertyValue2);
        } catch (Exception e) {
            return false;
        }
    }
}
