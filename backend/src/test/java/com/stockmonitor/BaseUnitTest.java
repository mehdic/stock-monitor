package com.stockmonitor;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base class for unit tests using Mockito.
 *
 * <p>Usage: Extend this class for any unit test that requires mocking dependencies.
 *
 * <p>Features: - Mockito extension enabled - No Spring context loaded (faster tests) - Use @Mock
 * and @InjectMocks annotations
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {
  // Base class for unit tests with Mockito support
}
