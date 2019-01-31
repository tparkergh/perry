package gov.ca.cwds.idm.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import gov.ca.cwds.idm.dto.User;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class UserRegistrationResentEvent extends UserChangeLogEvent {

  private static final long serialVersionUID = -3283136972929466144L;

  public static final String EVENT_TYPE_REGISTRATION_RESENT = "Registration Resent";

  public UserRegistrationResentEvent(User user) {
    super(user);
    setEventType(EVENT_TYPE_REGISTRATION_RESENT);
  }

}
