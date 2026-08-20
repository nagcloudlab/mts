package com.example.util;

public class CalculatorTest {

    // Junit
    // Arrange, Act, Assert

    Calculator calculator ;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        calculator = new Calculator();
    }

    @org.junit.jupiter.api.Test
    void testAdd() {
        // Arrange
        int a = 5;
        int b = 3;
        // Act
        int result = calculator.add(a, b);
        // Assert
        org.junit.jupiter.api.Assertions.assertEquals(8, result);
    }

    @org.junit.jupiter.api.Test
    void testSubtract() {
        // Arrange
        int a = 5;
        int b = 3;
        // Act
        int result = calculator.subtract(a, b);
        // Assert
        org.junit.jupiter.api.Assertions.assertEquals(2, result);

    }


    
}
