import i18next from '../utils/i18next';
import { Fragment, h } from '@stencil/core';
import { canRequestPinOnDrivingLicences, getGlobalApps, getAppIsLastLoginMethod } from './global-methods';

/**
 * Generate the links related to the document and its status
 * @document: the data of the document
 */
export function getDocumentLinks(document) {
  //Assign variables to fill in the URLs
  const linkVar1 = document.documentType === 'id' ? 'identiteitskaart' : 'rijbewijs';
  const linkVar2 = document.documentType === 'id' ? 'NI' : 'NL-Rijbewijs';

  //Link to activate the document
  const activateLink = (
    document.paid && document.status_mu === 'actief' ? (
      <dd-link
        link={`/${linkVar1}/activeren/${linkVar2}/${document.sequence_no}`}
        text={i18next.t(`id.login-status-link-activate-${document.documentType}`)}
      />
    ) : ('')
  );

  //Link to revoke the document
  const revokeLink = (
    document.paid && (document.status_mu === 'niet_actief' || document.status_mu === 'actief') ? (
      <dd-link
        class="dd-login-options-id-details__link"
        link={`/${linkVar1}/intrekken/bevestigen/${linkVar2}/${document.sequence_no}`}
        text={i18next.t(`id.login-status-link-revoke-${document.documentType}`)}
      />
    ) : ('')
  );

  //Link to unblock the document
  const unblockLink = (
    document.status_mu === 'actief' ? (
      <dd-link
        link={`/unblock_letter_aanvraag?card_type=${linkVar2}&sequence_no=${document.sequence_no}`}
        text={i18next.t(`id.login-status-link-unblock-${document.documentType}`)}
      />
    ) : ('')
  );

  //Link to fill in unblock code
  const fillInCodeLink = (
    <dd-link
      link={`/${linkVar1}/deblokkeren/code_invoeren/${linkVar2}/${document.sequence_no}`}
      text={i18next.t(`id.enter-code`)}
    />
  );

  //Link if no letter was received
  const noLetterLink = (
    <dd-link
      link={`/${linkVar1}/heraanvragen_deblokkeringscode/heraanvragen_deblokkeringscode?card_type=${linkVar2}&sequence_no=${document.sequence_no}`}
      text={i18next.t(`id.no-letter-received`)}
    />
  );
  //SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS

  const requestDrivingLicencePincodeLink =
    document.status_mu === 'actief' && document.documentType === 'licence' && !document.paid ? (
      <dd-link
        link={`/rijbewijs/aanvragen_pin/start_aanvragen_pincode?document_type=${linkVar2}&eidtoeslag_behandeld=${document.paid}&sequence_no=${document.sequence_no}`}
        text={i18next.t(`id.request-pin-link`)}
      />
    ) : (
      ''
    );

  const setDrivingLicencePincodeLink =
    canRequestPinOnDrivingLicences() && document.status_mu === 'actief' && document.documentType === 'licence' && document.allow_pin_reset && document.paid ? (
      <dd-link
        link={`/rijbewijs/instellen_pin/pincode_instellen_start?document_type=${linkVar2}&extend_with_activation=${document.status === 'niet_actief'
          }&sequence_no=${document.sequence_no}`}
        text={i18next.t(`id.set-pin-link`)}
      />
    ) : (
      ''
    );

  if (document.status === 'actief') {
    return (
      <Fragment>
        {revokeLink}
        {requestDrivingLicencePincodeLink}
        {setDrivingLicencePincodeLink}
      </Fragment>
    );
  } else if (document.status === 'niet_actief') {
    return (
      <Fragment>
        {activateLink}
        {revokeLink}
        {requestDrivingLicencePincodeLink}
        {setDrivingLicencePincodeLink}
      </Fragment>
    );
  } else if (document.status === 'geblokkeerd') {
    //Show additional links if an unblock code has been requested
    if (document.existing_unblock_request) {
      return (
        <Fragment>
          {revokeLink}
          {fillInCodeLink}
          {noLetterLink}
          {requestDrivingLicencePincodeLink}
          {setDrivingLicencePincodeLink}
        </Fragment>
      );
    } else {
      return (
        <Fragment>
          {revokeLink}
          {unblockLink}
          {requestDrivingLicencePincodeLink}
          {setDrivingLicencePincodeLink}
        </Fragment>
      );
    }
  }
}

/**
 * Generate the links related to the email and its status
 * @email: the email object
 */
export function getEmailLinks(email) {
  const addLink = <dd-link link="/email/nieuw" text={i18next.t(`contact.email-link-add`)} />;
  const changeLink = <dd-link link="/email/wijzigen" text={i18next.t(`contact.email-link-change`)} />;
  const removeLink = <dd-link link="/email/verwijderen" text={i18next.t(`contact.email-link-remove`)} />;
  const insertCodeLink = <dd-link link="/controleer_email" text={i18next.t(`contact.email-link-insert`)} />;
  const resendCodeLink = <dd-link link="/verstuur_email" text={i18next.t(`contact.email-link-resend`)} />;

  if (!email) {
    return <Fragment>{addLink}</Fragment>;
  } else if (email.status === 'not_verified' || email.status === 'blocked') {
    return (
      <Fragment>
        {insertCodeLink}
        {resendCodeLink}
        {changeLink}
        {removeLink}
      </Fragment>
    );
  } else {
    return (
      <Fragment>
        {changeLink}
        {removeLink}
      </Fragment>
    );
  }
}

/**
 * Generate the links related to the SMS verification
 * @sms: the sms status
 */
export function getSMSlinks(sms, zekerheidsniveau) {
  //Whether the user has at least one active DigiD app, show different links
  const atLeastOneActiveApp = getGlobalApps().some(app => app.status === 'active');
  if (sms === 'pending') {
    return (
      <Fragment>
        <dd-link link="/activeer_sms" text={i18next.t('username.sms-link-enter-code')} />
        <dd-link
          link={atLeastOneActiveApp ? '/existing_sms_request_url' : '/sms_uitbreiding'}
          text={i18next.t('username.sms-link-request-again')}
        />
      </Fragment>
    );
  } else if (sms === 'active') {
    return (
      <dd-link
        link={zekerheidsniveau === 20 ? '/controle_via_sms/waarschuwing_inlogniveau' : '/controle_via_sms/verwijderen'}
        text={i18next.t('username.sms-link-deactivate')}
      />
    );
  } else {
    // sms is inactive
    return (
      <dd-link
        link={atLeastOneActiveApp ? '/controle_via_sms/kies_app_of_brief' : '/sms_uitbreiding'}
        text={
          atLeastOneActiveApp ? i18next.t('username.sms-link-activate') : i18next.t('username.sms-link-request')
        }
      />
    );
  }
}

/**
 * Generate an app deactivation link, but only if this app is not the last active login method
 * @id: the app id
 */
//Don't show the deactivation link if this is the users last login method
export function getAppDeactivationLink(id) {
  return getAppIsLastLoginMethod() ? '' : <dd-link id='app-deactivation-link' link={`/apps/deactiveren/${id}/start`} text={i18next.t('app.deactivate')} />;
}
