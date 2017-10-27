package gov.ca.cwds.service.reissue;

import gov.ca.cwds.UniversalUserToken;
import gov.ca.cwds.config.Constants;
import gov.ca.cwds.rest.api.domain.PerryException;
import gov.ca.cwds.service.IdentityMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static gov.ca.cwds.config.Constants.IDENTITY;

/**
 * Created by TPT2 on 10/24/2017.
 */
@Service
@Profile("prod")
public class ReissueLoginServiceImpl implements ReissueLoginService {
  @Value("${security.oauth2.resource.revokeTokenUri}")
  private String revokeTokenUri;

  private OAuth2ProtectedResourceDetails resourceDetails;
  private ResourceServerProperties resourceServerProperties;
  private IdentityMappingService identityMappingService;
  private OAuth2RestTemplate clientRestTemplate;
  private ReissueTokenService tokenService;

  @Override
  public String issueAccessCode(String providerId, OAuth2ClientContext oauth2ClientContext) {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    OAuth2Authentication authentication = (OAuth2Authentication) securityContext.getAuthentication();
    UniversalUserToken userToken = (UniversalUserToken) authentication.getPrincipal();
    OAuth2AccessToken accessToken = oauth2ClientContext.getAccessToken();
    String identity = identityMappingService.map(userToken, providerId);
    accessToken.getAdditionalInformation().put(Constants.IDENTITY, identity);
    return tokenService.issueAccessCode(userToken.getUserId(), accessToken);
  }

  @Override
  public String issueToken(String accessCode) {
    return tokenService.getAccessTokenByAccessCode(accessCode);
  }

  @Override
  public String validate(String perryToken) {
    OAuth2AccessToken accessToken = tokenService.getAccessTokenByPerryToken(perryToken);
    OAuth2RestTemplate restTemplate = createRestTemplate(accessToken);
    restTemplate.postForObject(resourceServerProperties.getTokenInfoUri(), null, String.class);
    String identity = (String) accessToken.getAdditionalInformation().get(IDENTITY);
    OAuth2AccessToken reissuedAccessToken = restTemplate.getOAuth2ClientContext().getAccessToken();
    if (reissuedAccessToken != accessToken) {
      reissuedAccessToken.getAdditionalInformation().put(IDENTITY, identity);
      tokenService.updateAccessToken(perryToken, reissuedAccessToken);
    }
    return identity;
  }

  @Override
  public void invalidate(String perryToken) {
    OAuth2AccessToken accessToken = tokenService.deleteToken(perryToken);
    try {
      HttpHeaders headers = new HttpHeaders();
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("token", accessToken.getValue());
      params.add("token_type_hint", "access_token");
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
      clientRestTemplate.postForEntity(revokeTokenUri, request, String.class).getBody();
    } catch (Exception e) {
      throw new PerryException(
              "Token Revocation problem for revokeTokenUri = " + revokeTokenUri, e);
    }
  }

  private OAuth2RestTemplate createRestTemplate(OAuth2AccessToken accessToken) {
    return new OAuth2RestTemplate(resourceDetails, new DefaultOAuth2ClientContext(accessToken));
  }

  @Autowired
  public void setResourceDetails(OAuth2ProtectedResourceDetails resourceDetails) {
    this.resourceDetails = resourceDetails;
  }

  @Autowired
  public void setResourceServerProperties(ResourceServerProperties resourceServerProperties) {
    this.resourceServerProperties = resourceServerProperties;
  }

  @Autowired
  public void setIdentityMappingService(IdentityMappingService identityMappingService) {
    this.identityMappingService = identityMappingService;
  }

  @Autowired
  public void setClientRestTemplate(OAuth2RestTemplate clientRestTemplate) {
    this.clientRestTemplate = clientRestTemplate;
  }

  @Autowired
  public void setTokenService(ReissueTokenService tokenService) {
    this.tokenService = tokenService;
  }
}
