# Test Report

## Summary
- **Total Tests Run**: 6
- **Failures**: 3
- **Errors**: 0
- **Skipped**: 0

## Failures

### Test: `createLead_ShouldReturnCreatedLead`
- **Expected Status**: 201
- **Actual Status**: 500
- **Error**:
  ```
  java.lang.AssertionError: Status expected:<201> but was:<500>
      at org.springframework.test.util.AssertionErrors.fail(AssertionErrors.java:61)
      at org.springframework.test.util.AssertionErrors.assertEquals(AssertionErrors.java:128)
      at org.springframework.test.web.servlet.result.StatusResultMatchers.lambda$matcher$9(StatusResultMatchers.java:640)
      at org.springframework.test.web.servlet.MockMvc$1.andExpect(MockMvc.java:214)
      at com.leadflow.backend.controller.lead.LeadControllerTest.createLead_ShouldReturnCreatedLead(LeadControllerTest.java:97)
  ```

### Test: `updateLeadStatus_ShouldReturnUpdatedLead`
- **Expected Status**: 200
- **Actual Status**: 500
- **Error**:
  ```
  java.lang.AssertionError: Status expected:<200> but was:<500>
      at org.springframework.test.util.AssertionErrors.fail(AssertionErrors.java:61)
      at org.springframework.test.util.AssertionErrors.assertEquals(AssertionErrors.java:128)
      at org.springframework.test.web.servlet.result.StatusResultMatchers.lambda$matcher$9(StatusResultMatchers.java:640)
      at org.springframework.test.web.servlet.MockMvc$1.andExpect(MockMvc.java:214)
      at com.leadflow.backend.controller.lead.LeadControllerTest.updateLeadStatus_ShouldReturnUpdatedLead(LeadControllerTest.java:154)
  ```

### Test: `listActiveLeads_ShouldReturnLeadList`
- **Expected Status**: 200
- **Actual Status**: 500
- **Error**:
  ```
  java.lang.AssertionError: Status expected:<200> but was:<500>
      at org.springframework.test.util.AssertionErrors.fail(AssertionErrors.java:61)
      at org.springframework.test.util.AssertionErrors.assertEquals(AssertionErrors.java:128)
      at org.springframework.test.web.servlet.result.StatusResultMatchers.lambda$matcher$9(StatusResultMatchers.java:640)
      at org.springframework.test.web.servlet.MockMvc$1.andExpect(MockMvc.java:214)
      at com.leadflow.backend.controller.lead.LeadControllerTest.listActiveLeads_ShouldReturnLeadList(LeadControllerTest.java:135)
  ```

## Next Steps
- Investigate the root cause of the `500 Internal Server Error` in the failing tests.
- Ensure that all required beans and configurations are properly loaded in the test context.
- Validate the `LeadController` logic and dependencies for potential issues.