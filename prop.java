/**
 * 
 */
package de.deltalloyd.partnerdb.webservices.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.xml.bind.annotation.XmlSeeAlso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.deltalloyd.foundation.auth.exception.AuthorisationException;
import de.deltalloyd.foundation.exception.AbstractBusinessException;
import de.deltalloyd.foundation.exception.AbstractCheckedSystemException;
import de.deltalloyd.partnerdb.access.exception.DataAccessBusinessException;
import de.deltalloyd.partnerdb.access.exception.PartnerVorgangExistsException;
import de.deltalloyd.partnerdb.access.interfaces.PartnerDataAccess;
import de.deltalloyd.partnerdb.access.interfaces.PartnerDataAccessFactory;
import de.deltalloyd.partnerdb.access.type.BenutzerInfo;
import de.deltalloyd.partnerdb.access.type.FachlicherVorgang;
import de.deltalloyd.partnerdb.access.type.JournalEintrag;
import de.deltalloyd.partnerdb.access.type.KistAbrechnungsverband;
import de.deltalloyd.partnerdb.access.type.NatuerlichePerson;
import de.deltalloyd.partnerdb.access.type.NichtNatuerlichePerson;
import de.deltalloyd.partnerdb.access.type.Partner;
import de.deltalloyd.partnerdb.access.type.PartnerGemeinschaft;
import de.deltalloyd.partnerdb.access.type.jpa.PartnerPK;
import de.deltalloyd.partnerdb.core.exception.CoreContractException;
import de.deltalloyd.partnerdb.core.exception.PartnerInvalidExcepion;
import de.deltalloyd.partnerdb.core.exception.PartnerModifiedException;
import de.deltalloyd.partnerdb.core.exception.PartnerNotFoundException;
import de.deltalloyd.partnerdb.core.impl.PartnerServiceEJB;
import de.deltalloyd.partnerdb.core.monitor.appreg.MyAppPerformanceSensor;
import de.deltalloyd.partnerdb.core.type.AdresseTO;
import de.deltalloyd.partnerdb.core.type.BankVerbindungTO;
import de.deltalloyd.partnerdb.core.type.EinzelVsnrTO;
import de.deltalloyd.partnerdb.core.type.JournalEintragTO;
import de.deltalloyd.partnerdb.core.type.NatuerlichePersonGrunddatenTO;
import de.deltalloyd.partnerdb.core.type.NatuerlichePersonTO;
import de.deltalloyd.partnerdb.core.type.NichtNatuerlichePersonGrunddatenTO;
import de.deltalloyd.partnerdb.core.type.NichtNatuerlichePersonTO;
import de.deltalloyd.partnerdb.core.type.PartnerGemeinschaftGrunddatenTO;
import de.deltalloyd.partnerdb.core.type.PartnerGemeinschaftTO;
import de.deltalloyd.partnerdb.core.type.PartnerGrunddatenTO;
import de.deltalloyd.partnerdb.core.type.PartnerTO;
import de.deltalloyd.partnerdb.core.type.PostAdresseTO;
import de.deltalloyd.partnerdb.core.type.PostPruefAntwortTO;
import de.deltalloyd.partnerdb.core.type.ValidationError;
import de.deltalloyd.partnerdb.core.type.ValidationResult;
import de.deltalloyd.partnerdb.core.type.util.PartnerBilanzdaten;
import de.deltalloyd.partnerdb.core.type.util.PartnerValidator;
import de.deltalloyd.partnerdb.orb.access.exception.OrbAccessNotFoundException;
//import de.deltalloyd.partnerdb.orchestration.datensammler.PdbDatensammlungService;
import de.deltalloyd.partnerdb.process.exception.PartnerManagerBusinessException;
import de.deltalloyd.partnerdb.security.profiles.PdbBerechtigungen;
import de.deltalloyd.partnerdb.security.services.SecurityTokenSevice;
import de.deltalloyd.partnerdb.webservices.exception.PDBException;
import de.deltalloyd.partnerdb.webservices.exception.PartnerWebServiceException;
import de.deltalloyd.partnerdb.webservices.interfaces.IPartnerWebService;
import de.deltalloyd.partnerdb.webservices.security.exception.STSException;
import de.deltalloyd.partnerdb.webservices.utils.PDBReleaseInfo;

/**
 * PartnerWebServiceImpl stellt Webservices fuer den Partner-Geschaeftsvorfall
 * zur Verfuegung.
 * 
 */
@Interceptors(MyAppPerformanceSensor.class)
@Stateless
@WebService(name = "PartnerWebService", targetNamespace = "http://www.deltalloyd.de/pdb/partnerwebservice", serviceName = "PartnerWebService")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
@XmlSeeAlso({ NatuerlichePersonTO.class, NichtNatuerlichePersonTO.class, PartnerGemeinschaftTO.class,
		PartnerGemeinschaftGrunddatenTO.class, NatuerlichePersonGrunddatenTO.class,
		NichtNatuerlichePersonGrunddatenTO.class })
public class PartnerWebServiceImpl extends AbstractWebService implements IPartnerWebService {

	private static final String PARTNER_NICHT_GEFUNDEN = "Partner wurde nicht gefunden.";
	private static final String LADE_PARTNER_BANKVERBINDUNG = "ladePartnerBankverbindung:";
	private static final String LADE_PARTNER_POST_ADRESSE = "ladePartnerPostAdresse:";
	/**
	 * Das Feld <tt>logger</tt> enthaelt ...
	 */
	private static final Logger LOG_THIS = LoggerFactory
			.getLogger(de.deltalloyd.partnerdb.webservices.impl.PartnerWebServiceImpl.class);
	@PersistenceContext(unitName = "PDBPOOL")
	private EntityManager em;

	@EJB
	private PartnerServiceEJB partnerServiceLocal;

	@EJB
	private SecurityTokenSevice sts;
	
//	@EJB
//	private PdbDatensammlungService pdbDatensammlungService;

	/**
	 * Das Feld <tt>partnerValidator</tt> enthaelt einen Validator zum pruefen von
	 * PartnerTOs.
	 */
	private PartnerValidator partnerValidator;

	/**
	 * Das Feld <tt>dataAccess</tt> enthaelt ...
	 */
	private PartnerDataAccess dataAccess;

	public PartnerWebServiceImpl() {
		partnerValidator = new PartnerValidator();
		// adressValidator = new AdressValidator();
		dataAccess = new PartnerDataAccessFactory().createPartnerDataAccess();
	}

	@PostConstruct
	@WebMethod(exclude = true)
	public void initEm() {
		dataAccess.registerEntityManager(em);
		partnerValidator.registerDataAccess(dataAccess);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.deltalloyd.partnerdb.webservices.partner.interfaces.IPartnerWebServices
	 * #ladePartner(java.lang.String, boolean, java.lang.Integer, java.lang.Long,
	 * java.lang.String)
	 */
	@WebMethod
	public PartnerTO ladePartner(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "nebendubletteaufloesen") final boolean nebendubletteaufloesen,
			@WebParam(name = "klient") final Integer aKlient, @WebParam(name = "partnernr") final Long aPartnerNr,
			@WebParam(name = "parameter") final String aParameter) throws PartnerWebServiceException {
		try {
			LOG_THIS.debug("ladePartner:Aufruf> klient: " + aKlient + ", partnernr: " + aPartnerNr);
			// Pruefung, ob allgemeine Leserechte vorliegen.
			sts.isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);
			// Partner suchen und zurueckgeben
			LOG_THIS.error("aPartnerNr---webserviceimpl-------------" + aPartnerNr);
			PartnerTO partnerTO = partnerServiceLocal.ladePartner(aKlient, aPartnerNr);
			if (partnerTO == null) {
				LOG_THIS.error("ladeKopfDublettenPartner:" + aKlient + "," + aPartnerNr + "," + aParameter + ":"
						+ PARTNER_NICHT_GEFUNDEN);
				throw new PartnerWebServiceException(
						new PDBException(PDBException.DATENFEHLER, PARTNER_NICHT_GEFUNDEN, null));
			}
			if (nebendubletteaufloesen && partnerTO.getGrunddaten().isNebenDublette()) {
				LOG_THIS.debug("nebendubletteaufloesen fuer PID:" + aPartnerNr);
				long lKopfDublettePartnerNr = partnerServiceLocal.getKopfDublettenPartnerNr(aKlient, aPartnerNr);
				partnerTO = partnerServiceLocal.ladePartner(aKlient, lKopfDublettePartnerNr);
			}

			// Wenn es sich um VIP-Partner handelt pruefe auf zusaetzliche
			// VIP-Rechte
			if ((partnerTO.getGrunddaten().getVipKennzeichen() != null)
										&& (partnerTO.getGrunddaten().getVipKennzeichen().trim().equals("1")))  {
				getSSTSecurityToken().isAllowed(authToken, PdbBerechtigungen.DLD_PDB_MITARB);

			}
			// alles ok
			return partnerTO;
		} catch (STSException e) {
			LOG_THIS.error(e.getMessage(), e);
			throw new PartnerWebServiceException(new PDBException(PDBException.KEINELESERECHTE, e.getUuid()));
		} catch (AuthorisationException e) {
			LOG_THIS.error(e.getMessage());
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AuthorisationException, e.getMessage(), e.getUuid()));
		} catch (AbstractCheckedSystemException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		} catch (PartnerWebServiceException e) {
			LOG_THIS.error(e.getMessage());
			throw e;
		} catch (Exception e) {
			LOG_THIS.error(e.getMessage());
			throw new PartnerWebServiceException(new PDBException(PDBException.SONSTIGEFEHLER, e.getMessage(), null));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seede.deltalloyd.partnerdb.webservices.interfaces.IPartnerWebService#
	 * ladePartnerPostAdresse(java.lang.String, java.lang.Integer, java.lang.Long,
	 * int, java.lang.String)
	 */
	@WebMethod
	public PostAdresseTO ladePartnerPostAdresse(final String authToken, final Integer aKlient, final Long aPartnerNr,
			int aAnschrnr, final String aParameter) throws PartnerWebServiceException {
		LOG_THIS.debug(LADE_PARTNER_POST_ADRESSE + aKlient + "," + aPartnerNr + "," + aAnschrnr + "," + aParameter);
		// Pruefung, ob allgemeine Leserechte vorliegen.
		try {
			sts.isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);
		} catch (AbstractCheckedSystemException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		}
		PartnerTO partnerTO;
		try {
			partnerTO = partnerServiceLocal.ladePartner(aKlient, aPartnerNr);
			if (partnerTO == null) {
				LOG_THIS.error(LADE_PARTNER_POST_ADRESSE + aKlient + "," + aPartnerNr + "," + aAnschrnr + ","
						+ aParameter + ":" + PARTNER_NICHT_GEFUNDEN);
				throw new PartnerWebServiceException(
						new PDBException(PDBException.DATENFEHLER, PARTNER_NICHT_GEFUNDEN, null));
			}
			// Wenn es sich um eine Nebendublette handelt lese die
			// KopfDublettenAdressNr aus
			if (partnerTO.getGrunddaten().isNebenDublette()) {
				aAnschrnr = partnerServiceLocal.getKopfDublettenAdressNr(aKlient, aPartnerNr, aAnschrnr);
				LOG_THIS.debug(LADE_PARTNER_POST_ADRESSE + aKlient + "," + aPartnerNr + "," + aAnschrnr + ","
						+ aParameter + ":Anschrnr aus Kopfdublette : " + aAnschrnr);
				long lKopfDublettePartnerNr = partnerServiceLocal.getKopfDublettenPartnerNr(aKlient, aPartnerNr);
				partnerTO = partnerServiceLocal.ladePartner(aKlient, lKopfDublettePartnerNr);
			}

			// Wenn es sich um VIP-Partner handelt pruefe auf zusaetzliche
			// VIP-Rechte
			if ((partnerTO.getGrunddaten().getVipKennzeichen() != null)
					&& (partnerTO.getGrunddaten().getVipKennzeichen().trim().equals("1"))) {
				sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_MITARB);
			}

			if (aAnschrnr == -1) { // wenn es sich um eine Primaere Postadresse
				// handelt
				return partnerTO.getPrimaerePostAdresse();
			} else {
				for (PostAdresseTO postAdresseTO : ((List<PostAdresseTO>) partnerTO.getPostAdressen())) {
					if (postAdresseTO.getNummer() == aAnschrnr) {
						return postAdresseTO;
					}
				}
			}
			LOG_THIS.error(LADE_PARTNER_POST_ADRESSE + aKlient + "," + aPartnerNr + "," + aAnschrnr + "," + aParameter
					+ ":PostAdresseTO nicht gefunden.");
			return null;
		} catch (STSException e) {
			LOG_THIS.error(e.getMessage(), e);
			throw new PartnerWebServiceException(new PDBException(PDBException.KEINELESERECHTE, e.getUuid()));
		} catch (AuthorisationException e) {
			LOG_THIS.error(e.getMessage());
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AuthorisationException, e.getMessage(), e.getUuid()));
		} catch (AbstractCheckedSystemException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seede.deltalloyd.partnerdb.webservices.interfaces.IPartnerWebService#
	 * ladePartnerBankverbindung(java.lang.Integer, java.lang.Long, int,
	 * java.lang.String)
	 */
	@WebMethod
	public BankVerbindungTO ladePartnerBankverbindung(final String authToken, final Integer aKlient,
			final Long aPartnerNr, int aBankVerbNr, final String aParameter) throws PartnerWebServiceException {
		LOG_THIS.debug(LADE_PARTNER_BANKVERBINDUNG + aKlient + "," + aPartnerNr + "," + aBankVerbNr + "," + aParameter);
		// Pruefung, ob allgemeine Leserechte vorliegen.
		try {
			sts.isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);
		} catch (AbstractCheckedSystemException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		}
		PartnerTO partnerTO;
		try {
			partnerTO = partnerServiceLocal.ladePartner(aKlient, aPartnerNr);
			if (partnerTO == null) {
				LOG_THIS.error(LADE_PARTNER_BANKVERBINDUNG + aKlient + "," + aPartnerNr + "," + aBankVerbNr + ","
						+ aParameter + ":" + PARTNER_NICHT_GEFUNDEN);
				throw new PartnerWebServiceException(
						new PDBException(PDBException.DATENFEHLER, PARTNER_NICHT_GEFUNDEN, null));
			}
			// Wenn es sich um eine Nebendublette handelt lese die
			// KopfDublettenAdressNr aus
			if (partnerTO.getGrunddaten().isNebenDublette()) {
				aBankVerbNr = partnerServiceLocal.getKopfDublettenBankVerbNr(aKlient, aPartnerNr, aBankVerbNr);
				LOG_THIS.debug(LADE_PARTNER_BANKVERBINDUNG + aKlient + "," + aPartnerNr + "," + aBankVerbNr + ","
						+ aParameter + ":AdressNr aus Kopfdublette : " + aBankVerbNr);
				long lKopfDublettePartnerNr = partnerServiceLocal.getKopfDublettenPartnerNr(aKlient, aPartnerNr);
				partnerTO = partnerServiceLocal.ladePartner(aKlient, lKopfDublettePartnerNr);
			}
			// Wenn es sich um VIP-Partner handelt, pruefe auf zusaetzliche
			// VIP-Rechte
			if ((partnerTO.getGrunddaten().getVipKennzeichen() != null)
					&& (partnerTO.getGrunddaten().getVipKennzeichen().trim().equals("1"))) {
				sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_MITARB);
			}
			if (aBankVerbNr == -1) { // wenn es sich um eine Primaere
				// Bankverbindung
				// handelt
				return partnerTO.getPrimaereBankVerbindung();
			} else {
				for (BankVerbindungTO bTO : ((List<BankVerbindungTO>) partnerTO.getBankVerbindungen())) {
					if (bTO.getNummer() == aBankVerbNr) {
						return bTO;
					}
				}
			}
			LOG_THIS.error(LADE_PARTNER_BANKVERBINDUNG + aKlient + "," + aPartnerNr + "," + aBankVerbNr + ","
					+ aParameter + ":BankVerbindungTO nicht gefunden.");
			return null;
		} catch (STSException e) {
			LOG_THIS.error(e.getMessage(), e);
			throw new PartnerWebServiceException(new PDBException(PDBException.KEINELESERECHTE, e.getUuid()));
		} catch (AuthorisationException e) {
			LOG_THIS.error(e.getMessage());
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AuthorisationException, e.getMessage(), e.getUuid()));
		} catch (AbstractCheckedSystemException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seede.deltalloyd.partnerdb.webservices.interfaces.IPartnerWebService#
	 * aktualisierePartnerOhneSperre (de.deltalloyd.partnerdb.core.type.PartnerTO)
	 */
	@WebMethod
	public void aktualisierePartner(final String authToken, final PartnerTO partnerTo, final String aParameter)
			throws PartnerWebServiceException {
		FachlicherVorgang vVorgang = null;
		LOG_THIS.info("PartnerWebServiceImpl:aktualisierePartner.....");
		boolean errorUpdate = true;
		try {
			// Pruefung, ob allgemeine Schreibrechte vorliegen.
			sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_SCHREIBEN);

			/* Contract */
			if (partnerTo == null || partnerTo.getGrunddaten() == null) {
				throw new CoreContractException("aPartnerTO und aPartnerTO.grunddaten duerfen nicht null sein");
			}
			LOG_THIS.info("PartnerWebServiceImpl:aktualisierePartner....." + partnerTo.getPartnerNr());

			// Wenn es sich um VIP-Partner handelt pruefe auf zusaetzliche
			// VIP-Rechte
			if ((partnerTo.getGrunddaten().getVipKennzeichen() != null)
					&& (partnerTo.getGrunddaten().getVipKennzeichen().trim().equals("1"))) {
				sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_MITARB);
			}

			// Wenn es sich um Partner mit VIPKZ 'Makler ARL' handelt pruefe auf
			// Schreibberechtigung

			/* Laden des zugehoerigen Partner-Objekts */
			Partner vPartner = dataAccess.findPartnerByID(partnerTo.getMandant(), partnerTo.getPartnerNr());

			/* Plausis durchfuehren */
			ValidationResult vValidationResult = partnerValidator.validiereGrunddaten(partnerTo.getGrunddaten());
			if (vValidationResult.isValidationOkay()) {
				vValidationResult = partnerValidator.validiereUebergreifend(partnerTo);
			}
			if (!vValidationResult.isValidationOkay()) {
				if (vValidationResult.getValidationErrors() != null) {
					for (ValidationError error : vValidationResult.getValidationErrors()) {
						LOG_THIS.error("aktualisierePartner Error-Key: {}, {}", error.getReasonKey(),
								Arrays.toString(error.getAttributeKeys()));
					}
				}
				throw new PartnerInvalidExcepion(vValidationResult);
			}

			String sUserID = getSSTSecurityToken().getUserId(authToken);

			/* Sperre setzen */
			vVorgang = new FachlicherVorgang(em);
			vVorgang.setOrb(partnerTo.getPartnerNr().longValue());
			vVorgang.setKlient(partnerTo.getMandant().intValue());
			vVorgang.setUser(sUserID);
			vVorgang.start();

			if (vPartner == null) {
				throw new PartnerNotFoundException(partnerTo.getMandant() + "," + partnerTo.getPartnerNr());
			}

			/* Laden der Benutzerinformationen */
			BenutzerInfo benutzerInfo = dataAccess.findBenutzerInfoByID(sUserID);

			/* Transferdaten uebertragen */
			JournalEintrag[] vJournalEintrags = vPartner.aktualisiereVonTO(partnerTo, sUserID,
					benutzerInfo.getBezeichnung(), 0);

			/* Search Index aktualisieren */
			dataAccess.updateSearchIndex(partnerTo);
			errorUpdate = false;
		} catch (STSException e) {
			LOG_THIS.error(e.getMessage(), e);
			PartnerWebServiceException vEx = new PartnerWebServiceException(
					new PDBException(PDBException.KEINESCHREIBRECHTE, e.getUuid()));
			LOG_THIS.error(vEx.getMessage(), vEx);
			throw vEx;
		} catch (PartnerVorgangExistsException e) {
			logException(e);
			PartnerWebServiceException vEx = new PartnerWebServiceException(
					new PDBException(PDBException.PartnerVorgangExistsException, e.getMessage(), e.getUuid()));
			LOG_THIS.error(vEx.getMessage(), vEx);
			throw vEx;
		} catch (CoreContractException e) {
			logException(e);
			PartnerWebServiceException vEx = new PartnerWebServiceException(
					new PDBException(PDBException.CoreContractException, e.getMessage(), e.getUuid()));
			LOG_THIS.error(vEx.getMessage(), vEx);
			throw vEx;
		} catch (PartnerInvalidExcepion e) {
			logException(e);
			String sErrors = "";
			if ((e.getValidationResult() != null) && (e.getValidationResult().getValidationErrors() != null)) {
				for (ValidationError vE : e.getValidationResult().getValidationErrors()) {
					String sAttributes = "";
					if (vE.getAttributeKeys() != null) {
						for (String s : vE.getAttributeKeys()) {
							sAttributes += s + ",";
						}
					}
					if (sAttributes.length() > 0) {
						sAttributes = sAttributes.substring(0, sAttributes.length() - 1);
					}
					sErrors += vE.getReasonKey() + " : " + sAttributes + ";";
				}
			}
			PartnerWebServiceException vEx = new PartnerWebServiceException(new PDBException(
					PDBException.PartnerInvalidExcepion, e.getMessage() + " (" + sErrors + ")", e.getUuid()));
			LOG_THIS.error(vEx.getMessage(), vEx);
			throw vEx;
		} catch (PartnerNotFoundException e) {
			logException(e);
			PartnerWebServiceException vEx = new PartnerWebServiceException(
					new PDBException(PDBException.DATENFEHLER, PARTNER_NICHT_GEFUNDEN, e.getUuid()));
			LOG_THIS.error(vEx.getMessage(), vEx);
			throw vEx;
		} catch (PartnerModifiedException e) {
			logException(e);
			PartnerWebServiceException vEx = new PartnerWebServiceException(
					new PDBException(PDBException.PartnerModifiedException, e.getMessage(), e.getUuid()));
			LOG_THIS.error(vEx.getMessage(), vEx);
			throw vEx;
		} catch (Exception e) {
			LOG_THIS.error("Exception vom Typ " + e.getClass());
			LOG_THIS.error(e.getMessage(), e);
			PartnerWebServiceException vEx = new PartnerWebServiceException(
					new PDBException(PDBException.SONSTIGEFEHLER, e.getMessage()));
			LOG_THIS.error(vEx.getMessage(), vEx);
			throw vEx;
		} finally {
			fachlicherVorgangSchliessen(vVorgang, errorUpdate);
		}
	}

	private void fachlicherVorgangSchliessen(FachlicherVorgang vVorgang, boolean errorUpdate)
			throws PartnerWebServiceException {
		if (vVorgang != null && vVorgang.isStarted()) {
			if (errorUpdate) {
				try {
					vVorgang.end();
				} catch (Exception e) {
					LOG_THIS.error(e.getMessage(), e);
				}
			} else {
				try {
					vVorgang.end();
				} catch (RuntimeException e) {
					LOG_THIS.error(e.getMessage(), e);
					PartnerWebServiceException vEx = new PartnerWebServiceException(
							new PDBException(PDBException.RuntimeException, e.getMessage(), null));
					LOG_THIS.error(vEx.getMessage(), vEx);
					throw vEx;
				} catch (DataAccessBusinessException e) {
					logException(e);
					PartnerWebServiceException vEx = new PartnerWebServiceException(
							new PDBException(PDBException.DataAccessBusinessException, e.getMessage(), e.getUuid()));
					LOG_THIS.error(vEx.getMessage(), vEx);
					throw vEx;
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.deltalloyd.partnerdb.webservices.impl.IWebServiceAbstract#getReleaseInfo
	 * ()
	 */
	@WebMethod
	public String getReleaseInfo() {
		return PDBReleaseInfo.getReleaseInfo();
	}

	/**
	 * Loggt eine AbstractBusinessException.
	 * 
	 * @param aEx
	 *            die zu loggende Exception.
	 */
	private void logException(final AbstractBusinessException aEx) {
		LOG_THIS.error("AbstractBusinessException");
		LOG_THIS.error(aEx.getMessage(), aEx);
	}

	/**
	 * Loggt eine AbstractCheckedSystemException.
	 * 
	 * @param aEx
	 *            die zu loggende Exception.
	 */
	private void logException(final AbstractCheckedSystemException aEx) {
		LOG_THIS.error("AbstractCheckedSystemException");
		LOG_THIS.error(aEx.getMessage(), aEx);
	}

	/**
	 * Prueft die Adresse
	 * 
	 * @param aPartnerTO
	 * @param aAdresseTO
	 * @return
	 * @throws PartnerWebServiceException
	 * 
	 * @throws AbstractCheckedSystemException
	 * @throws AuthorisationException
	 * 
	 */
	@WebMethod
	public ValidationResult validiereAdresse(PartnerTO aPartnerTO, AdresseTO aAdresseTO, String aParameter)
			throws PartnerWebServiceException {
		try {
			return partnerServiceLocal.validiereAdresse(aPartnerTO, aAdresseTO);
		} catch (AuthorisationException e) {
			LOG_THIS.error(e.getMessage());
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AuthorisationException, e.getMessage(), e.getUuid()));
		} catch (AbstractCheckedSystemException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		}
	}

	/**
	 * Prueft die Bankverbindung
	 * 
	 * @param aPartnerTO
	 * @param aBankverbindung
	 * @param aParameter
	 * @return
	 * @throws PartnerWebServiceException
	 * 
	 *             Test / 20120330 / ok.
	 */
	@WebMethod
	public ValidationResult validiereBankverbindung(PartnerTO aPartnerTO, BankVerbindungTO aBankverbindung,
			String aParameter) throws PartnerWebServiceException {
		try {
			return partnerServiceLocal.validiereBankverbindung(aPartnerTO, aBankverbindung);
		} catch (AuthorisationException e) {
			LOG_THIS.error(e.getMessage());
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AuthorisationException, e.getMessage(), e.getUuid()));
		} catch (AbstractCheckedSystemException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		}
	}

	/**
	 * Validiert die Grunddaten und gibt das ermittelte Ergebnis zurueck.
	 * 
	 * @param aTO
	 * @return
	 * @throws PartnerWebServiceException
	 * 
	 *             Test / 20120330 / ok.
	 */
	@WebMethod
	public ValidationResult validiereGrunddaten(PartnerGrunddatenTO aTO, String aParameter)
			throws PartnerWebServiceException {
		try {
			return partnerServiceLocal.validiereGrunddaten(aTO);
		} catch (AbstractCheckedSystemException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		} catch (CoreContractException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.CoreContractException, e.getMessage(), e.getUuid()));
		}
	}

	/**
	 * Validiert den gesamten TO-Baum.
	 * 
	 * @param aTO
	 * @param aParameter
	 * @return
	 * @throws PartnerWebServiceException
	 * 
	 *             Test / 20120330 / ok.
	 */
	@WebMethod
	public ValidationResult validiereUebergreifend(PartnerTO aTO, String aParameter) throws PartnerWebServiceException {
		try {
			return partnerValidator.validiereUebergreifend(aTO);
		} catch (CoreContractException e) {
			LOG_THIS.error(e.getMessage());
			throw new PartnerWebServiceException(
					new PDBException(PDBException.CoreContractException, e.getMessage(), e.getUuid()));
		}
	}

	/**
	 * Prueft die Postadresse
	 * 
	 * @param aPostadresse
	 * @param aParameter
	 * @return
	 * @throws PartnerWebServiceException
	 */
	@WebMethod
	public PostPruefAntwortTO pruefePostAdresse(final PostAdresseTO aPostadresse, final String aParameter)
			throws PartnerWebServiceException {
		try {
			return partnerServiceLocal.pruefePostAdresse(aPostadresse);
		} catch (PartnerManagerBusinessException e) {
			LOG_THIS.error(e.getMessage());
			throw new PartnerWebServiceException(
					new PDBException(PDBException.PartnerManagerBusinessException, e.getMessage(), e.getUuid()));
		} catch (AbstractCheckedSystemException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		}
	}

	@WebMethod
	public void vorgangEntsprerren(@WebParam(name = "token") final String authToken,
			@WebParam(name = "klient") final int klient, @WebParam(name = "vorgang") final long vorgang)
			throws PartnerWebServiceException {
		try {
			sts.isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);
			dataAccess.removeVorgang(klient, vorgang);
		} catch (STSException | AbstractCheckedSystemException e) {
			LOG_THIS.error(e.getMessage(), e);
			throw new PartnerWebServiceException(new PDBException(PDBException.KEINESCHREIBRECHTE, e.getUuid()));
		}
	}

	/**
	 * Ermittelt eine freie Partnernummer fuer den angegebenen Mandanten.
	 * 
	 * @param authToken
	 * @param aKlient
	 * @param aParameter
	 * @return
	 * @throws PartnerWebServiceException
	 */
	@WebMethod
	public long holeFreiePartnerNummer(final String authToken, final int aKlient, final String aParameter)
			throws PartnerWebServiceException {
		try {
			sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_SCHREIBEN);
			return partnerServiceLocal.holeFreiePartnerNummer(aKlient);
		} catch (STSException e) {
			LOG_THIS.error(e.getMessage(), e);
			throw new PartnerWebServiceException(new PDBException(PDBException.KEINESCHREIBRECHTE, e.getUuid()));
		} catch (AbstractCheckedSystemException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		}
	}

	/**
	 * Erzeugt einen Partner entsprechend zu dem Transferobjekt.
	 * 
	 * @param aPartnerTO
	 * @param aParameter
	 * @return
	 * @throws PartnerWebServiceException
	 */
	@WebMethod
	public ValidationResult erzeugePartner(final String authToken, final PartnerTO aPartnerTO, final String aParameter)
			throws PartnerWebServiceException {
		/* Plausis durchfuehren lassen */
		ValidationResult vValidationResult;
		try {
			// Pruefung, ob allgemeine Schreibrechte vorliegen.
			sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_SCHREIBEN);

			vValidationResult = partnerValidator.validiereUebergreifend(aPartnerTO);
			if (!vValidationResult.isValidationOkay()) {
				return vValidationResult;
			}

			/* Neuen Partner entsprechend Typ instanziieren */
			Partner vPartner = null;
			if (aPartnerTO instanceof NichtNatuerlichePersonTO) {
				vPartner = new NichtNatuerlichePerson();
			} else if (aPartnerTO instanceof NatuerlichePersonTO) {
				vPartner = new NatuerlichePerson();
			} else if (aPartnerTO instanceof PartnerGemeinschaftTO) {
				vPartner = new PartnerGemeinschaft();
			}

			/* Mandant und Partnernummer setzen */
			PartnerPK id = new PartnerPK(aPartnerTO.getMandant().intValue(), aPartnerTO.getPartnerNr().longValue());
			if (vPartner != null) {
				vPartner.setId(id);
			} else {
				LOG_THIS.error("vPartner ist unerwartet null");
				throw new NullPointerException();
			}

			String sUserID = getSSTSecurityToken().getUserId(authToken);
			/* Laden der Benutzerinformationen */
			BenutzerInfo benutzerInfo = dataAccess.findBenutzerInfoByID(sUserID);

			/* Transferdaten uebertragen */
			try {
				vPartner.aktualisiereVonTO(aPartnerTO, sUserID, benutzerInfo.getBezeichnung(),
						benutzerInfo.getKostenstelle());
			} catch (PartnerModifiedException e) {
				LOG_THIS.error("Kann nicht auftreten, da Neuanlage das Partners");
				logException(e);
			}

			/* Partner erzeugen */
			dataAccess.createPartner(vPartner);

			/* Suchindex versorgen */
			dataAccess.insertIntoSearchIndex(aPartnerTO);

			return vValidationResult;
		} catch (STSException e) {
			LOG_THIS.error(e.getMessage(), e);
			throw new PartnerWebServiceException(new PDBException(PDBException.KEINESCHREIBRECHTE, e.getUuid()));
		} catch (CoreContractException e) {
			logException(e);
			LOG_THIS.error(e.getMessage(), e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.CoreContractException, e.getMessage(), e.getUuid()));
		} catch (Exception e) {
			LOG_THIS.error(e.getMessage(), e);
			throw new PartnerWebServiceException(new PDBException(PDBException.SONSTIGEFEHLER, e.getMessage()));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seede.deltalloyd.partnerdb.webservices.interfaces.IPartnerWebService#
	 * getKopfDublettenAdressNr(int, long, int)
	 */
	@WebMethod
	public int getKopfDublettenAdressNr(int aKlient, long aPartnerNr, int aAdressNr) throws PartnerWebServiceException {
		try {
			return partnerServiceLocal.getKopfDublettenAdressNr(aKlient, aPartnerNr, aAdressNr);
		} catch (AbstractCheckedSystemException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seede.deltalloyd.partnerdb.webservices.interfaces.IPartnerWebService#
	 * getKopfDublettenBankVerbNr(int, long, int)
	 */
	@WebMethod
	public int getKopfDublettenBankVerbNr(int aKlient, long aPartnerNr, int aBankVerbNr)
			throws PartnerWebServiceException {
		try {
			return partnerServiceLocal.getKopfDublettenBankVerbNr(aKlient, aPartnerNr, aBankVerbNr);
		} catch (AbstractCheckedSystemException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seede.deltalloyd.partnerdb.webservices.interfaces.IPartnerWebService#
	 * getKopfDublettenPartnerNr(int, long)
	 */
	@WebMethod
	public long getKopfDublettenPartnerNr(int aKlient, long aPartnerNr) throws PartnerWebServiceException {
		try {
			return partnerServiceLocal.getKopfDublettenPartnerNr(aKlient, aPartnerNr);
		} catch (AbstractCheckedSystemException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seede.deltalloyd.partnerdb.webservices.interfaces.IPartnerWebService#
	 * ladeJournalFuerPartner(int, long)
	 */
	@WebMethod
	public List<JournalEintragTO> ladeJournalFuerPartner(int aKlient, long aPartnerNummer)
			throws PartnerWebServiceException {
		try {
			return partnerServiceLocal.ladeJournalFuerPartner(aKlient, aPartnerNummer);
		} catch (PartnerNotFoundException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.DATENFEHLER, PARTNER_NICHT_GEFUNDEN, e.getUuid()));
		} catch (AbstractCheckedSystemException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seede.deltalloyd.partnerdb.webservices.interfaces.IPartnerWebService#
	 * ladeSapPartnerInformation(java.lang.String)
	 */
	@WebMethod
	public List<String[]> ladeSapPartnerInformation(String aPID) throws PartnerWebServiceException {
		List<String[]> list = new ArrayList<>();
		Properties p = partnerServiceLocal.ladeSapPartnerInformation(aPID);

		for (Enumeration en = p.keys(); en.hasMoreElements();) {
			String sKey = (String) en.nextElement();
			String sValue = p.getProperty(sKey);
			String[] sValuePair = { sKey, sValue };
			list.add(sValuePair);
		}

		return list;
	}

	/**
	 * 
	 */
	@WebMethod
	public PartnerBilanzdaten getbilanzdaten(@WebParam(name = "klient") final Integer aKlient,
			@WebParam(name = "partnernr") final Long aPartnerNr) throws PartnerWebServiceException {
		PartnerBilanzdaten partnerbilanzdaten = new PartnerBilanzdaten();
		PartnerTO partnerto;
		try {
			partnerto = partnerServiceLocal.ladePartner(aKlient, aPartnerNr);
		} catch (AuthorisationException e) {
			LOG_THIS.error(e.getMessage());
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AuthorisationException, e.getMessage(), e.getUuid()));
		} catch (AbstractCheckedSystemException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		}
		if (partnerto == null) {
			LOG_THIS.error("Partner wurde nicht gefunden (" + aKlient + ")(" + aPartnerNr + ").");
			throw new PartnerWebServiceException(
					new PDBException(PDBException.DATENFEHLER, PARTNER_NICHT_GEFUNDEN, null));
		} else if (partnerto.getGrunddaten() instanceof NichtNatuerlichePersonGrunddatenTO) {
			partnerbilanzdaten
					.setBilanzMonat(((NichtNatuerlichePersonGrunddatenTO) partnerto.getGrunddaten()).getBilanzMonat());
			partnerbilanzdaten.setAbwVersandMonat(
					((NichtNatuerlichePersonGrunddatenTO) partnerto.getGrunddaten()).getAbwVersand());
			partnerbilanzdaten.setPartnerNr(aPartnerNr);
			partnerbilanzdaten.setMandant(partnerto.getMandant());
		} else if (partnerto.getGrunddaten() instanceof PartnerGemeinschaftGrunddatenTO) {
			partnerbilanzdaten
					.setBilanzMonat(((PartnerGemeinschaftGrunddatenTO) partnerto.getGrunddaten()).getBilanzMonat());
			partnerbilanzdaten
					.setAbwVersandMonat(((PartnerGemeinschaftGrunddatenTO) partnerto.getGrunddaten()).getAbwVersand());
			partnerbilanzdaten.setPartnerNr(aPartnerNr);
			partnerbilanzdaten.setMandant(partnerto.getMandant());
		} else {
			LOG_THIS.error("Partnertyp ungueltig. (" + aKlient + ")(" + aPartnerNr + ").");
			throw new PartnerWebServiceException(
					new PDBException(PDBException.DATENFEHLER, "Partnertyp ungueltig.", null));
		}
		return partnerbilanzdaten;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.deltalloyd.partnerdb.webservices.interfaces.IPartnerWebService#
	 * getNeusteEinzelVsnr(java.lang.String, java.lang.Integer, java.lang.String)
	 */
	@WebMethod
	public List<EinzelVsnrTO> getNeusteEinzelVsnr(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "mandant") final Integer mandant, @WebParam(name = "orbart") final String orbart,
			@WebParam(name = "orb") final String orb) throws PartnerWebServiceException {
		LOG_THIS.debug("getNeusteEinzelVsnr-Abfrage. token:" + authToken + " mandant:" + mandant + " orb:" + orb);
		try {
			// Pruefung, ob allgemeine Leserechte vorliegen.
			sts.isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);
			return partnerServiceLocal.getNeusteEinzelVsnr(mandant, orbart, orb);
		} catch (STSException e) {
			LOG_THIS.error(e.getMessage(), e);
			throw new PartnerWebServiceException(new PDBException(PDBException.KEINELESERECHTE, e.getUuid()));
		} catch (OrbAccessNotFoundException e) {
			LOG_THIS.error(e.getMessage(), e);
			throw new PartnerWebServiceException(new PDBException(PDBException.DATENFEHLER, e.getMessage(), ""));
		} catch (AbstractCheckedSystemException e) {
			logException(e);
			throw new PartnerWebServiceException(new PDBException(PDBException.AbstractCheckedSystemException,
					"ORB wurde nicht gefunden. " + e.getMessage(), ""));
		}

	}

	@WebMethod(exclude = true)
	public List<KistAbrechnungsverband> ladeKiStAbrechnungVerbaende() throws PartnerWebServiceException {
		try {
			return partnerServiceLocal.ladeKiStAbrechnungVerbaende();
		} catch (AbstractCheckedSystemException e) {
			logException(e);
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		}
	}

	@WebMethod
	public void updateUebertragungBRS(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "klient")  int klient, @WebParam(name = "partnerNr")  long partnerNr,
			@WebParam(name = "brsUebertragung")  String brsUebertragung) throws PartnerWebServiceException {

		try {
			// Pruefung, ob Schreibrechte vorliegen.
			sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_SCHREIBEN);

			String userId = getSSTSecurityToken().getUserId(authToken);

			partnerServiceLocal.updateBrsUebertragung(userId, klient, partnerNr, brsUebertragung);

		} catch (STSException e) {
			LOG_THIS.error(e.getMessage(), e);
			throw new PartnerWebServiceException(new PDBException(PDBException.KEINESCHREIBRECHTE, e.getUuid()));
		} catch (AbstractCheckedSystemException | PartnerManagerBusinessException e) {
			LOG_THIS.error(e.getMessage(), e);
			throw new PartnerWebServiceException(new PDBException(PDBException.PartnerManagerBusinessException,
					"Fehler beim Bearbeiten des Partners. " + e.getMessage(), ""));
		} catch (Exception e) {
			LOG_THIS.error("updateUebertragungBRS error", e);
			throw new PartnerWebServiceException(new PDBException(PDBException.PartnerManagerBusinessException,
					"Unerwartete Fehler: " + e.getMessage()));
		}
	}
	
	//create password for user 
	@WebMethod
	public List<Object> createPassword(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "klient") final Integer aKlient, @WebParam(name = "partnernr") final Long aPartnerNr,
			@WebParam(name = "mail") final String mail) throws PartnerWebServiceException {
		boolean errorUpdate = true;
		FachlicherVorgang vVorgang = null;
		List<Object> responseList;
		try {
			// Pruefung, ob allgemeine Schreibrechte vorliegen.
			sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_SCHREIBEN);
			PartnerTO partnerTo = partnerServiceLocal.ladePartner(aKlient, aPartnerNr);
			/* Contract */
			if (partnerTo == null || partnerTo.getGrunddaten() == null) {
				throw new CoreContractException("aPartnerTO und aPartnerTO.grunddaten duerfen nicht null sein");
			}
			LOG_THIS.info("PartnerWebServiceImpl:aktualisierePartner....." + partnerTo.getPartnerNr());

			// Wenn es sich um VIP-Partner handelt pruefe auf zusaetzliche
			// VIP-Rechte
			if ((partnerTo.getGrunddaten().getVipKennzeichen() != null)
					&& (partnerTo.getGrunddaten().getVipKennzeichen().trim().equals("1")))  {
				sts.isAllowed(authToken, PdbBerechtigungen.DLD_PDB_MITARB);
			}

			// Wenn es sich um Partner mit VIPKZ 'Makler ARL' handelt pruefe auf
			// Schreibberechtigung
			
			responseList = partnerServiceLocal.updatePaswordandBroker(partnerTo, mail);
			
			/* Laden des zugehoerigen Partner-Objekts */
			Partner vPartner = dataAccess.findPartnerByID(partnerTo.getMandant(), partnerTo.getPartnerNr());

			/* Plausis durchfuehren */
			ValidationResult vValidationResult = partnerValidator.validiereGrunddaten(partnerTo.getGrunddaten());
			if (vValidationResult.isValidationOkay()) {
				vValidationResult = partnerValidator.validiereUebergreifend(partnerTo);
			}
			if (!vValidationResult.isValidationOkay()) {
				if (vValidationResult.getValidationErrors() != null) {
					for (ValidationError error : vValidationResult.getValidationErrors()) {
						LOG_THIS.error("aktualisierePartner Error-Key: {}, {}", error.getReasonKey(),
								Arrays.toString(error.getAttributeKeys()));
					}
				}
				throw new PartnerInvalidExcepion(vValidationResult);
			}

			String sUserID = getSSTSecurityToken().getUserId(authToken);

			/* Sperre setzen */
			vVorgang = new FachlicherVorgang(em);
			vVorgang.setOrb(partnerTo.getPartnerNr().longValue());
			vVorgang.setKlient(partnerTo.getMandant().intValue());
			vVorgang.setUser(sUserID);
			vVorgang.start();

			if (vPartner == null) {
				throw new PartnerNotFoundException(partnerTo.getMandant() + "," + partnerTo.getPartnerNr());
			}

			/* Laden der Benutzerinformationen */
			BenutzerInfo benutzerInfo = dataAccess.findBenutzerInfoByID(sUserID);

			/* Transferdaten uebertragen */
			JournalEintrag[] vJournalEintrags = vPartner.aktualisiereVonTO(partnerTo, sUserID,
					benutzerInfo.getBezeichnung(), 0);

			/* Search Index aktualisieren */
			dataAccess.updateSearchIndex(partnerTo);
			errorUpdate = false;
		} catch (STSException e) {
			LOG_THIS.error(e.getMessage(), e);
			PartnerWebServiceException vEx = new PartnerWebServiceException(
					new PDBException(PDBException.KEINESCHREIBRECHTE, e.getUuid()));
			LOG_THIS.error(vEx.getMessage(), vEx);
			throw vEx;
		} catch (PartnerVorgangExistsException e) {
			logException(e);
			PartnerWebServiceException vEx = new PartnerWebServiceException(
					new PDBException(PDBException.PartnerVorgangExistsException, e.getMessage(), e.getUuid()));
			LOG_THIS.error(vEx.getMessage(), vEx);
			throw vEx;
		} catch (CoreContractException e) {
			logException(e);
			PartnerWebServiceException vEx = new PartnerWebServiceException(
					new PDBException(PDBException.CoreContractException, e.getMessage(), e.getUuid()));
			LOG_THIS.error(vEx.getMessage(), vEx);
			throw vEx;
		} catch (PartnerInvalidExcepion e) {
			logException(e);
			String sErrors = "";
			if ((e.getValidationResult() != null) && (e.getValidationResult().getValidationErrors() != null)) {
				for (ValidationError vE : e.getValidationResult().getValidationErrors()) {
					String sAttributes = "";
					if (vE.getAttributeKeys() != null) {
						for (String s : vE.getAttributeKeys()) {
							sAttributes += s + ",";
						}
					}
					if (sAttributes.length() > 0) {
						sAttributes = sAttributes.substring(0, sAttributes.length() - 1);
					}
					sErrors += vE.getReasonKey() + " : " + sAttributes + ";";
				}
			}
			PartnerWebServiceException vEx = new PartnerWebServiceException(new PDBException(
					PDBException.PartnerInvalidExcepion, e.getMessage() + " (" + sErrors + ")", e.getUuid()));
			LOG_THIS.error(vEx.getMessage(), vEx);
			throw vEx;
		} catch (PartnerNotFoundException e) {
			logException(e);
			PartnerWebServiceException vEx = new PartnerWebServiceException(
					new PDBException(PDBException.DATENFEHLER, PARTNER_NICHT_GEFUNDEN, e.getUuid()));
			LOG_THIS.error(vEx.getMessage(), vEx);
			throw vEx;
		} catch (PartnerModifiedException e) {
			logException(e);
			PartnerWebServiceException vEx = new PartnerWebServiceException(
					new PDBException(PDBException.PartnerModifiedException, e.getMessage(), e.getUuid()));
			LOG_THIS.error(vEx.getMessage(), vEx);
			throw vEx;
		} catch (Exception e) {
			LOG_THIS.error("Exception vom Typ " + e.getClass());
			LOG_THIS.error(e.getMessage(), e);
			PartnerWebServiceException vEx = new PartnerWebServiceException(
					new PDBException(PDBException.SONSTIGEFEHLER, e.getMessage()));
			LOG_THIS.error(vEx.getMessage(), vEx);
			throw vEx;
		} finally {
			fachlicherVorgangSchliessen(vVorgang, errorUpdate);
		}
		return responseList;
	}
	
//	@WebMethod
//	public void notifyCtv(@WebParam(name = "authToken") final String authToken, @WebParam(name = "relevanteVertraege") List<Vertragsdaten> relevanteVertraege
//			)
//			throws PartnerWebServiceException {
//		try {
//			pdbDatensammlungService.versendeBriefe(authToken, relevanteVertraege);
//		} catch (Exception e) {
//			throw new PartnerWebServiceException(new PDBException(PDBException.PartnerManagerBusinessException,
//					"Unerwartete Fehler: " + e.getMessage()));
//		}
//	}

}
