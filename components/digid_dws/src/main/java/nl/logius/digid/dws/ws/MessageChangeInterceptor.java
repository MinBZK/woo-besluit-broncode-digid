
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

package nl.logius.digid.dws.ws;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.logius.digid.dws.exception.DwsRuntimeException;

/**
 * http://www.mastertheboss.com/jboss-web-services/apache-cxf-interceptors
 * http://stackoverflow.com/questions/6915428/how-to-modify-the-raw-xml-message-of-an-outbound-cxf-request
 *
 */
public abstract class MessageChangeInterceptor extends AbstractPhaseInterceptor<Message> {

    public MessageChangeInterceptor() {
        super(Phase.PRE_STREAM);
        addBefore(SoapPreProtocolOutInterceptor.class.getName());
    }

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected abstract String changeOutboundMessage(String currentEnvelope);

    protected abstract String changeInboundMessage(String currentEnvelope);

    public void handleMessage(Message message) {
        boolean isOutbound = false;
        isOutbound = message == message.getExchange().getOutMessage()
            || message == message.getExchange().getOutFaultMessage();

        if (isOutbound) {
            OutputStream os = message.getContent(OutputStream.class);

            CachedStream cs = new CachedStream();
            message.setContent(OutputStream.class, cs);

            message.getInterceptorChain().doIntercept(message);

            try {
                cs.flush();
                IOUtils.closeQuietly(cs);
                CachedOutputStream csnew = (CachedOutputStream) message.getContent(OutputStream.class);

                String currentEnvelopeMessage = IOUtils.toString(csnew.getInputStream(), "UTF-8");
                csnew.flush();
                IOUtils.closeQuietly(csnew);

                if (logger.isDebugEnabled()) {
                    logger.debug("Outbound message: " + currentEnvelopeMessage);
                }

                String res = changeOutboundMessage(currentEnvelopeMessage);
                if (res != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Outbound message has been changed: " + res);
                    }
                }
                res = res != null ? res : currentEnvelopeMessage;

                InputStream replaceInStream = IOUtils.toInputStream(res, "UTF-8");

                IOUtils.copy(replaceInStream, os);
                replaceInStream.close();
                IOUtils.closeQuietly(replaceInStream);

                os.flush();
                message.setContent(OutputStream.class, os);
                IOUtils.closeQuietly(os);

            } catch (IOException ioe) {
                logger.warn("Unable to perform change.", ioe);
                throw new DwsRuntimeException(ioe);
            }
        } else {
            try {
                InputStream is = message.getContent(InputStream.class);
                String currentEnvelopeMessage = IOUtils.toString(is, "UTF-8");
                IOUtils.closeQuietly(is);

                if (logger.isDebugEnabled()) {
                    logger.debug("Inbound message: " + currentEnvelopeMessage);
                }

                String res = changeInboundMessage(currentEnvelopeMessage);
                if (res != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Inbound message has been changed: " + res);
                    }
                }
                res = res != null ? res : currentEnvelopeMessage;

                is = IOUtils.toInputStream(res, "UTF-8");
                message.setContent(InputStream.class, is);
                IOUtils.closeQuietly(is);
            } catch (IOException ioe) {
                logger.warn("Unable to perform change.", ioe);

                throw new DwsRuntimeException(ioe);
            }
        }
    }

    public void handleFault(Message message) {
    }

    private class CachedStream extends CachedOutputStream {
        public CachedStream() {
            super();
        }

        protected void doFlush() throws IOException {
            currentStream.flush();
        }

        protected void doClose() throws IOException {
        }

        protected void onWrite() throws IOException {
        }
    }
}
