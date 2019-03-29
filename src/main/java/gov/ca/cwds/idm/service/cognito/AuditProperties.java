package gov.ca.cwds.idm.service.cognito;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;

@Profile("idm")
@ConfigurationProperties(prefix = "audit")
public class AuditProperties extends CommonDoraProperties{

}