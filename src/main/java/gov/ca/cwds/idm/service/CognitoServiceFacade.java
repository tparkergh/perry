package gov.ca.cwds.idm.service;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult;
import gov.ca.cwds.idm.CognitoProperties;
import gov.ca.cwds.rest.api.domain.PerryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@Profile("idm")
public class CognitoServiceFacade {
  @Autowired private CognitoProperties properties;

  private AWSCognitoIdentityProvider identityProvider;

  @PostConstruct
  public void init() {
    AWSCredentialsProvider credentialsProvider =
        new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(properties.getIamAccessKeyId(), properties.getIamSecretKey()));
    identityProvider =
        AWSCognitoIdentityProviderClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(Regions.fromName(properties.getRegion()))
            .build();
  }

  public AdminGetUserResult getById(String id) {
    try {
      AdminGetUserRequest request =
          new AdminGetUserRequest().withUsername(id).withUserPoolId(properties.getUserpool());
      return identityProvider.adminGetUser(request);
    } catch (Exception e) {
      throw new PerryException("Exception while connecting to AWS Cognito", e);
    }
  }
}
