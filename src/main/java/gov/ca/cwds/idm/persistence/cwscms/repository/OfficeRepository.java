package gov.ca.cwds.idm.persistence.cwscms.repository;

import gov.ca.cwds.data.auth.ReadOnlyRepository;
import gov.ca.cwds.data.persistence.auth.CwsOffice;
import gov.ca.cwds.idm.dto.Office;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Profile("idm")
@Repository
public interface OfficeRepository extends ReadOnlyRepository<CwsOffice, String> {

  String COUNTY_NAME = "countyName";

  @Query("select new gov.ca.cwds.idm.dto.Office(office.officeId, trim(office.cwsOfficeName),"
      + " office.governmentEntityType, trim(county.shortDescription)) from CwsOffice office,"
      + " County county where office.governmentEntityType = county.systemId ")
  List<Office> findOffices();

  @Query("select new gov.ca.cwds.idm.dto.Office(office.officeId, trim(office.cwsOfficeName),"
      + " office.governmentEntityType, trim(county.shortDescription)) from CwsOffice office,"
      + " County county where office.governmentEntityType = county.systemId "
      + "and county.shortDescription = :" + COUNTY_NAME)
  List<Office> findCountyOffices(@Param(COUNTY_NAME) String countyName);
}
