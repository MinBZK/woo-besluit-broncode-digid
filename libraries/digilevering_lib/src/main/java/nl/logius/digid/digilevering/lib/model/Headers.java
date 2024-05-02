
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

package nl.logius.digid.digilevering.lib.model;

public interface Headers {

    String X_AUX_ACTION                 = "x_aux_action";
    String X_AUX_ACTIVITY               = "x_aux_activity";
    String X_AUX_PROCESS_TYPE           = "x_aux_process_type";
    String X_AUX_PROCESS_VERSION        = "x_aux_process_version";
    String X_AUX_PRODUCTION             = "x_aux_production";
    String X_AUX_PROTOCOL               = "x_aux_protocol";
    String X_AUX_PROTOCOL_VERSION       = "x_aux_protocol_version";
    String X_AUX_RECEIVER_ID            = "x_aux_receiver_id";
    String X_AUX_SENDER_ID              = "x_aux_sender_id";
    String X_AUX_SYSTEM_MSG_ID          = "x_aux_system_msg_id";
    String X_AUX_PROCESS_INSTANCE_ID    = "x_aux_process_instance_id";
    String X_AUX_SEQ_NUMBER             = "x_aux_seq_number";
    String X_AUX_MSG_ORDER              = "x_aux_msg_order";
}
