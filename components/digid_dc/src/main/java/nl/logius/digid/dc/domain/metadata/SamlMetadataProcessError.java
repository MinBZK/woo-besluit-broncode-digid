
/*
  Deze broncode is openbaar gemaakt vanwege een Woo-verzoek zodat deze
  gericht is op transparantie en niet op hergebruik. Hergebruik van 
  de broncode is toegestaan onder de EUPL licentie, met uitzondering 
  van broncode waarvoor een andere licentie is aangegeven.
  
  Het archief waar dit bestand deel van uitmaakt is te vinden op:
    https://github.com/MinBZK/woo-besluit-broncode-digid
  
  Eventuele kwetsbaarheden kunnen worden gemeld bij het NCSC via:
    https://www.ncsc.nl/contact/kwetsbaarheid-melden
  onder vermelding van "Logius, openbaar gemaakte broncode DigiD" 
  
  Voor overige vragen over dit Woo-besluit kunt u mailen met open@logius.nl
  
  This code has been disclosed in response to a request under the Dutch
  Open Government Act ("Wet open Overheid"). This implies that publication 
  is primarily driven by the need for transparence, not re-use.
  Re-use is permitted under the EUPL-license, with the exception 
  of source files that contain a different license.
  
  The archive that this file originates from can be found at:
    https://github.com/MinBZK/woo-besluit-broncode-digid
  
  Security vulnerabilities may be responsibly disclosed via the Dutch NCSC:
    https://www.ncsc.nl/contact/kwetsbaarheid-melden
  using the reference "Logius, publicly disclosed source code DigiD" 
  
  Other questions regarding this Open Goverment Act decision may be
  directed via email to open@logius.nl
*/

package nl.logius.digid.dc.domain.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.logius.digid.dc.Base;

import javax.persistence.*;

@Entity
@Table(name = "saml_metadata_process_errors")
public class SamlMetadataProcessError extends Base {

    public SamlMetadataProcessError(){

    }

    public SamlMetadataProcessError(String reason, String data) {
        errorReason = reason == null ? "unknown" : reason;
        service = data;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "saml_metadata_process_result_id")
    private SamlMetadataProcessResult samlMetaDataProcessResult;

    @Column(name = "saml_metadata_process_result_id", insertable=false, updatable=false)
    private Long samlMetadataProcessResultId;

    @Column(name = "service")
    private String service;

    @Column(name = "error_reason")
    private String errorReason;

    public Long getId() {
        return id;
    }

    @JsonIgnore
    public SamlMetadataProcessResult getSamlMetaDataProcessResult() {
        return samlMetaDataProcessResult;
    }

    public void setSamlMetaDataProcessResult(SamlMetadataProcessResult samlMetaDataProcessResult) {
        this.samlMetaDataProcessResult = samlMetaDataProcessResult;
    }

    public Long getSamlMetadataProcessResultId() {
        return samlMetadataProcessResultId;
    }

    public void setSamlMetadataProcessResultId(Long samlMetadataProcessResultId) {
        this.samlMetadataProcessResultId = samlMetadataProcessResultId;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }
}
