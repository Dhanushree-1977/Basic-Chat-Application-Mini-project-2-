package com.realtimechat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone TestRunner that executes JUnit 5 test classes
 * without requiring an external build tool CLI or IDE.
 */
public class TestRunner {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   RUNNING AUTOMATED UNIT TEST SUITE");
        System.out.println("================================================================================");

        List<Class<?>> testClasses = List.of(
                com.realtimechat.model.MessageTest.class,
                com.realtimechat.service.AuthenticationServiceTest.class
        );

        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;

        for (Class<?> testClass : testClasses) {
            System.out.println("\n[SUITE] " + testClass.getSimpleName());

            Method beforeEach = null;
            for (Method m : testClass.getDeclaredMethods()) {
                if (m.isAnnotationPresent(BeforeEach.class)) {
                    beforeEach = m;
                    break;
                }
            }

            for (Method testMethod : testClass.getDeclaredMethods()) {
                if (testMethod.isAnnotationPresent(Test.class)) {
                    totalTests++;
                    String testName = testMethod.getName();
                    DisplayName displayName = testMethod.getAnnotation(DisplayName.class);
                    if (displayName != null) {
                        testName = displayName.value();
                    }

                    try {
                        Object instance = testClass.getDeclaredConstructor().newInstance();
                        if (beforeEach != null) {
                            beforeEach.invoke(instance);
                        }
                        testMethod.invoke(instance);
                        System.out.println("  PASS: " + testName);
                        passedTests++;
                    } catch (InvocationTargetException ite) {
                        Throwable cause = ite.getCause();
                        System.err.println("  FAIL: " + testName + " -> " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
                        cause.printStackTrace(System.err);
                        failedTests++;
                    } catch (Exception e) {
                        System.err.println("  ERROR: " + testName + " -> " + e.getMessage());
                        failedTests++;
                    }
                }
            }
        }

        System.out.println("\n================================================================================");
        System.out.printf("   TEST RESULTS: %d Passed | %d Failed | Total %d%n", passedTests, failedTests, totalTests);
        System.out.println("================================================================================");

        if (failedTests > 0) {
            System.exit(1);
        }
    }
}
