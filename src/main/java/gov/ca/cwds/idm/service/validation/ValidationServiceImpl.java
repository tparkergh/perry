package gov.ca.cwds.idm.service.validation;

import static gov.ca.cwds.idm.service.cognito.util.CognitoUsersSearchCriteriaUtil.composeToGetFirstPageByEmail;
import static gov.ca.cwds.idm.service.cognito.util.CognitoUsersSearchCriteriaUtil.composeToGetFirstPageByRacfId;
import static gov.ca.cwds.service.messages.MessageCode.ACTIVE_USER_WITH_RAFCID_EXISTS_IN_IDM;
import static gov.ca.cwds.service.messages.MessageCode.COUNTY_NAME_IS_NOT_PROVIDED;
import static gov.ca.cwds.service.messages.MessageCode.FIRST_NAME_IS_NOT_PROVIDED;
import static gov.ca.cwds.service.messages.MessageCode.LAST_NAME_IS_NOT_PROVIDED;
import static gov.ca.cwds.service.messages.MessageCode.NO_USER_WITH_RACFID_IN_CWSCMS;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_TO_REMOVE_ALL_ROLES;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_UPDATE_UNALLOWED_ROLES;
import static gov.ca.cwds.service.messages.MessageCode.USER_WITH_EMAIL_EXISTS_IN_IDM;
import static gov.ca.cwds.util.Utils.isRacfidUser;
import static gov.ca.cwds.util.Utils.toUpperCase;

import com.amazonaws.services.cognitoidp.model.UserType;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.service.cognito.CognitoServiceFacade;
import gov.ca.cwds.idm.service.cognito.util.CognitoUtils;
import gov.ca.cwds.idm.service.role.implementor.AdminRoleImplementorFactory;
import gov.ca.cwds.rest.api.domain.UserIdmValidationException;
import gov.ca.cwds.service.CwsUserInfoService;
import gov.ca.cwds.service.dto.CwsUserInfo;
import gov.ca.cwds.service.messages.MessageCode;
import gov.ca.cwds.service.messages.MessagesService;
import java.util.Collection;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Profile("idm")
public class ValidationServiceImpl implements ValidationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ValidationServiceImpl.class);

  private CwsUserInfoService cwsUserInfoService;

  private MessagesService messagesService;

  private CognitoServiceFacade cognitoServiceFacade;

  private AdminRoleImplementorFactory adminRoleImplementorFactory;

  @Override
  public void validateUserCreate(User enrichedUser, boolean activeUserExistsInCws) {

    validateFirstNameIsProvided(enrichedUser);
    validateLastNameIsProvided(enrichedUser);
    validateCountyNameIsProvided(enrichedUser);

    if (isRacfidUser(enrichedUser)) {
      String racfId = enrichedUser.getRacfid();
      validateActiveUserExistsInCws(activeUserExistsInCws, racfId);
      validateRacfidDoesNotExistInCognito(racfId);
    }
  }

  @Override
  public void validateVerifyIfUserCanBeCreated(User enrichedUser, boolean activeUserExistsInCws) {
    String racfId = enrichedUser.getRacfid();

    validateActiveUserExistsInCws(activeUserExistsInCws, racfId);
    validateEmailDoesNotExistInCognito(enrichedUser.getEmail());
    validateRacfidDoesNotExistInCognito(racfId);
  }

  @Override
  public void validateUpdateUser(UserType existedCognitoUser, UserUpdate updateUserDto) {
    validateUpdateByNewUserRoles(updateUserDto);
    validateActivateUser(existedCognitoUser, updateUserDto);
  }

  private void validateFirstNameIsProvided(User user) {
    validateIsNotBlank(user.getFirstName(), FIRST_NAME_IS_NOT_PROVIDED);
  }

  private void validateLastNameIsProvided(User user) {
    validateIsNotBlank(user.getLastName(), LAST_NAME_IS_NOT_PROVIDED);
  }

  private void validateCountyNameIsProvided(User user) {
    validateIsNotBlank(user.getCountyName(), COUNTY_NAME_IS_NOT_PROVIDED);
  }

  private void validateIsNotBlank(String value, MessageCode errorCode) {
    if (StringUtils.isBlank(value)) {
      throwValidationException(errorCode);
    }
  }

  private void validateActiveUserExistsInCws(boolean activeUserExistsInCws, String racfid) {
    if (!activeUserExistsInCws) {
      throwValidationException(NO_USER_WITH_RACFID_IN_CWSCMS, racfid);
    }
  }

  void validateRacfidDoesNotExistInCognito(String racfId) {
    if (isActiveRacfIdPresentInCognito(racfId)) {
      throwValidationException(ACTIVE_USER_WITH_RAFCID_EXISTS_IN_IDM, racfId);
    }
  }

  private void validateEmailDoesNotExistInCognito(String email) {
    if (userWithEmailExistsInCognito(email)) {
      throwValidationException(USER_WITH_EMAIL_EXISTS_IN_IDM, email);
    }
  }

  private void validateUpdateByNewUserRoles(UserUpdate updateUserDto) {
    Collection<String> newUserRoles = updateUserDto.getRoles();

    if (newUserRoles == null) {
      return;
    }

    if (newUserRoles.isEmpty()) {
      throwValidationException(UNABLE_TO_REMOVE_ALL_ROLES);
    }

    validateByAllowedRoles(newUserRoles);
  }

  private void validateByAllowedRoles(Collection<String> newUserRoles) {
    Collection<String> allowedRoles = adminRoleImplementorFactory.getPossibleUserRoles();
    if (!allowedRoles.containsAll(newUserRoles)) {
      throwValidationException(UNABLE_UPDATE_UNALLOWED_ROLES, newUserRoles, allowedRoles);
    }
  }

  private boolean isActiveRacfIdPresentInCognito(String racfId) {
    Collection<UserType> cognitoUsersByRacfId =
        cognitoServiceFacade.searchAllPages(composeToGetFirstPageByRacfId(toUpperCase(racfId)));
    return !CollectionUtils.isEmpty(cognitoUsersByRacfId)
        && isActiveUserPresent(cognitoUsersByRacfId);
  }

  private boolean userWithEmailExistsInCognito(String email) {
    Collection<UserType> cognitoUsers =
        cognitoServiceFacade.searchPage(composeToGetFirstPageByEmail(email)).getUsers();
    return !CollectionUtils.isEmpty(cognitoUsers);
  }

  private void throwValidationException(MessageCode messageCode, Object... args) {
    String msg = messagesService.getTechMessage(messageCode, args);
    String userMsg = messagesService.getUserMessage(messageCode, args);
    LOGGER.error(msg);
    throw new UserIdmValidationException(msg, userMsg, messageCode);
  }

  private void validateActivateUser(UserType existedCognitoUser, UserUpdate updateUserDto) {
    if (!canChangeToEnableActiveStatus(updateUserDto.getEnabled(),
        existedCognitoUser.getEnabled())) {
      return;
    }
    String racfId = CognitoUtils.getRACFId(existedCognitoUser);
    if (StringUtils.isNotBlank(racfId)) {
      validateActivateUser(racfId);
    }
  }

  private static boolean canChangeToEnableActiveStatus(Boolean newEnabled, Boolean currentEnabled) {
    return newEnabled != null && !newEnabled.equals(currentEnabled) && newEnabled;
  }

  void validateActivateUser(String racfId) {
    CwsUserInfo cwsUser = cwsUserInfoService.getCwsUserByRacfId(racfId);
    validateActiveUserExistsInCws(cwsUser != null, racfId);
    validateRacfidDoesNotExistInCognito(racfId);
  }

  private static boolean isActiveUserPresent(Collection<UserType> cognitoUsers) {
    return cognitoUsers
        .stream()
        .anyMatch(userType -> Objects.equals(userType.getEnabled(), Boolean.TRUE));
  }

  @Autowired
  public void setCwsUserInfoService(CwsUserInfoService cwsUserInfoService) {
    this.cwsUserInfoService = cwsUserInfoService;
  }

  @Autowired
  public void setMessagesService(MessagesService messagesService) {
    this.messagesService = messagesService;
  }

  @Autowired
  public void setCognitoServiceFacade(
      CognitoServiceFacade cognitoServiceFacade) {
    this.cognitoServiceFacade = cognitoServiceFacade;
  }

  @Autowired
  public void setAdminRoleImplementorFactory(
      AdminRoleImplementorFactory adminRoleImplementorFactory) {
    this.adminRoleImplementorFactory = adminRoleImplementorFactory;
  }
}