
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
import nl.logius.digid.dc.domain.connection.Connection;
import nl.logius.digid.dc.Base;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "saml_metadata_process_results")
public class SamlMetadataProcessResult extends Base {

    public SamlMetadataProcessResult() {
    }

    public SamlMetadataProcessResult(Long connection){
        connectionId = connection;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "samlMetaDataProcessResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SamlMetadataProcessError> samlMetadataProcessErrors = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id", insertable = false, updatable = false)
    private Connection connection;

    @Column(name = "connection_id")
    private Long connectionId;

    @Column(name = "total_processed")
    private int totalProcessed;

    @Column(name = "total_created")
    private int totalCreated;

    @Column(name = "total_updated")
    private int totalUpdated;

    @Column(name = "total_errors")
    private int totalErrors;

    @Column(name = "metadata")
    private String metadata;

    @Column(name = "hash")
    private String hash;

    public Long getId() {
        return id;
    }
    public Long getConnectionId() {
        return connectionId;
    }
    public void setConnectionId(Long connectionId) {
        this.connectionId = connectionId;
    }
    public int getTotalProcessed() {
        return totalProcessed;
    }
    public void setTotalProcessed(int totalProcessed) {
        this.totalProcessed = totalProcessed;
    }
    public int getTotalCreated() {
        return totalCreated;
    }
    public void setTotalCreated(int totalCreated) {
        this.totalCreated = totalCreated;
    }
    public int getTotalUpdated() {
        return totalUpdated;
    }
    public void setTotalUpdated(int totalUpdated) {
        this.totalUpdated = totalUpdated;
    }
    public int getTotalErrors() {
        return totalErrors;
    }
    public void setTotalErrors(int totalErrors) {
        this.totalErrors = totalErrors;
    }
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    public void setHash(String hash) {
        this.hash = hash;
    }

    public void incrementErrors() {
        this.totalErrors += 1;
    }
    public void incrementProcessed() {
        this.totalProcessed += 1;
    }
    public void incrementCreated() {
        this.totalCreated += 1;
    }
    public void incrementUpdated() {
        this.totalUpdated += 1;
    }

    @JsonIgnore
    public List<SamlMetadataProcessError> getSamlMetadataProcessErrors() {
        return samlMetadataProcessErrors;
    }

    public void setSamlMetadataProcessErrors(List<SamlMetadataProcessError> samlMetadataProcessErrors) {
        this.samlMetadataProcessErrors = samlMetadataProcessErrors;
    }

    public void addProcessError(String reason, String data) {
        SamlMetadataProcessError error =  new SamlMetadataProcessError(reason, data);
        error.setSamlMetaDataProcessResult(this);
        samlMetadataProcessErrors.add(error);
        incrementErrors();
    }

    @JsonIgnore
    public String getMetadata() {
        return metadata;
    }

    public boolean allEntriesSuccessful(){
        return totalProcessed == ( totalCreated + totalUpdated );
    }
}
