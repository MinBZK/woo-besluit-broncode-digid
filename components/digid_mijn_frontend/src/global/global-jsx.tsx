import i18next from '../utils/i18next';
import { Fragment, h } from '@stencil/core';

export function hiddenNewWindow(){
	return (
		<span aria-hidden='true'>[{i18next.t('general.new-window')}].</span>
	)
}

export function contactHelpdeskLink() {
  return (
  	<div>
      <a
        href="https://www.digid.nl/contact/"
        target="_blank"
        aria-label={i18next.t('general.contact1') + ', ' + i18next.t('general.new-window')}
      >
        {i18next.t('general.contact1')}
      </a>{' '}
			{hiddenNewWindow()}
		</div>
  );
}

export function getAccordionCloseButton(loginMethod, closingMethod, keydownMethod, ariaLabel) {
  return (
    <div class={`dd-login-options-${loginMethod}__accordion__content__footer`}>
      <div
        class={`dd-login-options-${loginMethod}__accordion__content__footer__content`}
        onClick={closingMethod}
        onKeyDown={keydownMethod}
        tabindex="0"
        aria-label={ariaLabel}
        role="button"
      >
        <dd-icon class={`dd-login-options-${loginMethod}__accordion__content__footer__content__icon`} name="cross" />
        <div class={`dd-login-options-${loginMethod}__accordion__content__footer__content__text`}>
          {i18next.t('general.close')}
        </div>
      </div>
    </div>
  );
}

export function getCardMoreDetailsButton(loginMethod, name: string = '') {
  return (
    <div class={`dd-login-options-${loginMethod}__card__more-details`}>
      <dd-button
        class={`dd-login-options-${loginMethod}__card__more-details__button`}
        theme="tertiary"
        arrow="after"
        text={i18next.t('button.more-details')}
        size-change="false"
        no-hover={true}
        aria-text={i18next.t(loginMethod + '.details-aria', { name })}
        role-override="link"
      />
    </div>
  );
}

export function loginMethodDetailsTable(loginMethod, index, names, content, indented, links, tooltips, tooltipcontent) {
  return (
    <Fragment>
      <th
        class={{
          [`dd-login-options-${loginMethod}-details__table-grid-name`]: true,
          'dd-login-options-app-details__ID-check': names[index] === 'ID-check',
        }}
      >
        {names[index]}
      </th>
      <td class={`dd-login-options-${loginMethod}-details__table-grid-content`}>{content[index]}</td>
      <td
        class={{
          [`dd-login-options-${loginMethod}-details__table-grid-link`]: true,
          [`dd-login-options-${loginMethod}-details__table-grid-link-indented`]: !indented[index],
        }}
      >
        {links[index]}
      </td>
      {tooltips[index] && (
        //Render tooltip content if the row has a tooltip
        <Fragment>
          <dd-tooltip
            id={`${loginMethod}-details-${names[index]}-button`}
            class={`dd-login-options-${loginMethod}-details__table-grid-tooltip`}
            contentId={`${loginMethod}-details-${names[index]}-content`}
            ariaText={names[index]}
          />
          <dd-tooltip-content
            id={`${loginMethod}-details-${names[index]}-content`}
            class={`dd-login-options-${loginMethod}-details__table-grid-tooltip-content`}
          >
            {tooltipcontent[index]}
          </dd-tooltip-content>
        </Fragment>
      )}
    </Fragment>
  );
}
