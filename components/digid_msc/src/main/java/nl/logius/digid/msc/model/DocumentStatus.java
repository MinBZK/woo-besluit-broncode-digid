
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

package nl.logius.digid.msc.model;

import java.sql.Timestamp;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import nl.logius.digid.msc.converter.db.DocTypeTypeConverter;
import nl.logius.digid.msc.converter.db.MUStatusTypeConverter;
import nl.logius.digid.msc.converter.db.StateSourceTypeConverter;
import nl.logius.digid.msc.converter.db.StatusTypeConverter;

import https.digid_nl.schema.mu_status_controller.DocTypeType;
import https.digid_nl.schema.mu_status_controller.MUStatusType;
import https.digid_nl.schema.mu_status_controller.StateSourceType;
import https.digid_nl.schema.mu_status_controller.StatusType;

@Entity
public class DocumentStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pseudonym;

    @Convert(converter = DocTypeTypeConverter.class)
    private DocTypeType docType;

    @Convert(converter = StateSourceTypeConverter.class)
    private StateSourceType stateSource;

    private String sequenceNo;

    @Convert(converter = StatusTypeConverter.class)
    private StatusType status;

    private Timestamp statusDatetime;

    @Convert(converter = MUStatusTypeConverter.class)
    private MUStatusType statusMu;

    private Timestamp statusMuDatetime;

    private String eptl;

    @Override
    public String toString() {
        return String.format("SCStatus[id=%d, status='%s']", id, status);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPseudonym() {
        return pseudonym;
    }

    public void setPseudonym(String pseudonym) {
        this.pseudonym = pseudonym;
    }

    public DocTypeType getDocType() {
        return docType;
    }

    public void setDocType(DocTypeType docType) {
        this.docType = docType;
    }

    public StateSourceType getStateSource() {
        return stateSource;
    }

    public void setStateSource(StateSourceType stateSource) {
        this.stateSource = stateSource;
    }

    public String getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(String sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }

    public Timestamp getStatusDatetime() {
        return statusDatetime;
    }

    public void setStatusDatetime(Timestamp statusDatetime) {
        this.statusDatetime = statusDatetime;
    }

    public MUStatusType getStatusMu() {
        return statusMu;
    }

    public void setStatusMu(MUStatusType statusMu) {
        this.statusMu = statusMu;
    }

    public Timestamp getStatusMuDatetime() {
        return statusMuDatetime;
    }

    public void setStatusMuDatetime(Timestamp statusMuDatetime) {
        this.statusMuDatetime = statusMuDatetime;
    }

    public String getEptl() {
        return eptl;
    }

    public void setEptl(String eptl) {
        this.eptl = eptl;
    }
}
