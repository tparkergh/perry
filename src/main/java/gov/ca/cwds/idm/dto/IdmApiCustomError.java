package gov.ca.cwds.idm.dto;

import static gov.ca.cwds.config.LoggingRequestIdFilter.REQUEST_ID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import gov.ca.cwds.service.messages.MessageCode;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@SuppressWarnings({"squid:S3437"})
public class IdmApiCustomError  implements Serializable {

  private static final long serialVersionUID = -7854227531434018227L;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
  private LocalDateTime timestamp;

  @JsonProperty
  private HttpStatus status;

  @JsonProperty
  private String technicalMessage;

  @JsonProperty
  private String userMessage;

  @JsonProperty
  private String errorCode;

  @JsonProperty
  private String incidentId;

  @JsonProperty
  private List<String> causes = new ArrayList<>();

  private IdmApiCustomError() {
    timestamp = LocalDateTime.now();
  }

  public IdmApiCustomError(HttpStatus status) {
    this();
    this.status = status;
    this.incidentId = MDC.get(REQUEST_ID);
  }

  public IdmApiCustomError(HttpStatus status, String technical_message) {
    this(status);
    this.technicalMessage = technical_message;
  }

  public IdmApiCustomError(HttpStatus status, MessageCode errorCode, String technical_message) {
    this(status, technical_message);
    this.errorCode = errorCode.getValue();
  }

  public IdmApiCustomError(HttpStatus status, MessageCode errorCode, String technical_message, List<String> causes) {
    this(status, errorCode, technical_message);
    this.causes = causes;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getTechnicalMessage() {
    return technicalMessage;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public String getUserMessage() {
    return userMessage;
  }

  public String getIncidentId() {
    return incidentId;
  }

  public List<String> getCauses() {
    return causes;
  }
}
