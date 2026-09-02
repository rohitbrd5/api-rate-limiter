package com.example.ratelimiter.integration;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Test utility to access Spring beans from test classes that are not managed by Spring DI.
 * <p>
 * Allows integration tests to retrieve beans from the application context when
 * direct injection is not convenient.
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * Get a bean from the application context by type.
     *
     * @param beanClass the class of the bean
     * @param <T> the type of the bean
     * @return the bean instance
     */
    public static <T> T getBean(Class<T> beanClass) {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext not initialized. Ensure @SpringBootTest is used.");
        }
        return applicationContext.getBean(beanClass);
    }
}
