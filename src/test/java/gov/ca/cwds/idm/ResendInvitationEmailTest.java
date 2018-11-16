package gov.ca.cwds.idm;

import static gov.ca.cwds.config.TokenServiceConfiguration.TOKEN_TRANSACTION_MANAGER;
import static gov.ca.cwds.config.api.idm.Roles.OFFICE_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.STATE_ADMIN;
import static gov.ca.cwds.idm.persistence.ns.OperationType.UPDATE;
import static gov.ca.cwds.idm.util.TestCognitoServiceFacade.USER_WITH_RACFID_ID;
import static gov.ca.cwds.idm.util.TestUtils.dateTime;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.amazonaws.services.cognitoidp.model.AdminCreateUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminCreateUserResult;
import com.amazonaws.services.cognitoidp.model.UserType;
import gov.ca.cwds.idm.persistence.ns.entity.NsUser;
import gov.ca.cwds.idm.service.NsUserService;
import gov.ca.cwds.idm.util.TestCognitoServiceFacade;
import gov.ca.cwds.idm.util.WithMockCustomUser;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

@Transactional(value = TOKEN_TRANSACTION_MANAGER)
public class ResendInvitationEmailTest extends BaseIdmIntegrationWithUserLogTest {

  private static final String USER_WITH_RACFID_ID_EMAIL = "julio@gmail.com";

  @Autowired
  private NsUserService nsUserService;

  @Test
  @WithMockCustomUser(county = "OtherCounty")
  public void testResendInvitationEmailWithDifferentCounty() throws Exception {
    assertResendEmailUnauthorized(USER_WITH_RACFID_ID);
  }

  @Test
  @WithMockCustomUser(roles = {OFFICE_ADMIN}, adminOfficeIds = {"otherOfficeId"})
  public void testResendInvitationEmailWithOfficeRole() throws Exception {
    assertResendEmailUnauthorized(USER_WITH_RACFID_ID);
  }

  @Test
  @WithMockCustomUser(roles = {"OtherRole"})
  public void testResendInvitationEmailWithOtherRole() throws Exception {
    assertResendEmailUnauthorized(USER_WITH_RACFID_ID);
  }

  @Test
  @WithMockCustomUser(roles = {STATE_ADMIN}, county = "Madera")
  public void testResendInvitationEmailWithStateAdmin() throws Exception {
    assertResendEmailWorksFine();
  }

  @Test
  @WithMockCustomUser()
  public void testResendInvitationEmailWithCountyAdmin() throws Exception {
    assertResendEmailWorksFine();
  }

  public void testSaveLastRegistrationResubmitTime() {
    final String USER_NAME = "last-registration-resubmit-time-test-unique-username";

    final long FIRST_RESUBMIT_TIME_MILLIS = 1000000L;
    final long SECOND_RESUBMIT_TIME_MILLIS = FIRST_RESUBMIT_TIME_MILLIS + 1000L;

    final LocalDateTime FIRST_RESUBMIT_TIME = dateTime(FIRST_RESUBMIT_TIME_MILLIS);
    final LocalDateTime SECOND_RESUBMIT_TIME = dateTime(SECOND_RESUBMIT_TIME_MILLIS);

    userLogRepository.deleteAll();

    assertThat(getLastRegistrationResubmitTime(USER_NAME), nullValue());

    nsUserService.saveLastRegistrationResubmitTime(USER_NAME, FIRST_RESUBMIT_TIME);
    assertThat(getLastRegistrationResubmitTime(USER_NAME), is(FIRST_RESUBMIT_TIME));
    assertLastUserLog(dateTime(FIRST_RESUBMIT_TIME_MILLIS - 100), USER_NAME, UPDATE);

    nsUserService.saveLastRegistrationResubmitTime(USER_NAME, SECOND_RESUBMIT_TIME);
    assertThat(getLastRegistrationResubmitTime(USER_NAME), is(SECOND_RESUBMIT_TIME));
    assertLastUserLog(dateTime(SECOND_RESUBMIT_TIME_MILLIS - 100), USER_NAME, UPDATE);
  }

  private void assertResendEmailUnauthorized(String id) throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/idm/users/resend?id=" + id))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andReturn();
  }

  private void assertResendEmailWorksFine() throws Exception {
    AdminCreateUserRequest request =
        ((TestCognitoServiceFacade) cognitoServiceFacade)
            .createResendEmailRequest(USER_WITH_RACFID_ID_EMAIL);

    UserType user = new UserType();
    user.setUsername(USER_WITH_RACFID_ID_EMAIL);
    user.setEnabled(true);
    user.setUserStatus("FORCE_CHANGE_PASSWORD");

    AdminCreateUserResult result = new AdminCreateUserResult().withUser(user);
    when(cognito.adminCreateUser(request)).thenReturn(result);

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/idm/users/resend?id=" + USER_WITH_RACFID_ID))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();
  }

  private LocalDateTime getLastRegistrationResubmitTime(String username) {
    Optional<NsUser> nsUserOpt = nsUserService.findByUsername(username);
    return nsUserOpt.map(NsUser::getLastRegistrationResubmitTime).orElse(null);
  }
}