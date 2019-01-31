package gov.ca.cwds.idm.service.cognito.attribute.diff.builder;

import static gov.ca.cwds.idm.service.cognito.attribute.StandardUserAttribute.EMAIL;

import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.service.cognito.attribute.diff.EmailUserAttributeDiff;

/**
 * Created by Alexander Serbin on 1/15/2019
 */
public class EmailAttributeDiffBuilder extends StringAttributeDiffBuilder {

  public EmailAttributeDiffBuilder(String oldValue, String newValue) {
    super(EMAIL, oldValue, newValue);
  }

  @Override
  public EmailUserAttributeDiff buildDiff() {
    return new EmailUserAttributeDiff(getOldValue(), getNewValue());
  }
  
}
