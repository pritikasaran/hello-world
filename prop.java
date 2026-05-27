/**
 * 
 */
package de.deltalloyd.partnerdb.webservices.impl;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.resource.ResourceException;
import jakarta.xml.bind.annotation.XmlSeeAlso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.deltalloyd.foundation.auth.exception.PolicyCheckException;
import de.deltalloyd.foundation.exception.AbstractCheckedSystemException;
import de.deltalloyd.partnerdb.core.exception.CoreSystemException;
import de.deltalloyd.partnerdb.core.monitor.appreg.MyAppPerformanceSensor;
import de.deltalloyd.partnerdb.core.type.kksn.KksnTO;
import de.deltalloyd.partnerdb.core.type.kksn.Kksnw2TO;
import de.deltalloyd.partnerdb.core.type.kksn.Kksnw3TO;
import de.deltalloyd.partnerdb.core.type.kksn.Kksnw4TO;
import de.deltalloyd.partnerdb.core.type.kksn.StKksnFunk2AktTO;
import de.deltalloyd.partnerdb.core.type.kksn.StKksnOrbAktTO;
import de.deltalloyd.partnerdb.core.type.kksn.StKksnOrbLesTO;
import de.deltalloyd.partnerdb.core.type.kksn.StKksnPersLesTO;
import de.deltalloyd.partnerdb.core.type.kksn.StKksnVorVersTO;
import de.deltalloyd.partnerdb.security.profiles.PdbBerechtigungen;
import de.deltalloyd.partnerdb.security.services.SecurityTokenSevice;
import de.deltalloyd.partnerdb.webservices.exception.KksnFehler;
import de.deltalloyd.partnerdb.webservices.utils.WSParams;
import de.deltalloyd.uniservra.UniservConnection;
import de.deltalloyd.uniservra.UniservConnectionFactory;
import de.deltalloyd.uniservra.UniservPostAllResultTO;
import de.deltalloyd.uniservra.UniservPostAllTO;

/**
 * KksnWSImpl - Webservice mit Kksn-Funktionen
 * 
 * @author bl10514
 * 
 */
@Interceptors(MyAppPerformanceSensor.class)
@Stateless
@WebService(name = "KksnWebService", targetNamespace = "http://www.deltalloyd.de/pdb/partnerwebservice", serviceName = "KksnWSService")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
@XmlSeeAlso({ KksnTO.class, Kksnw2TO.class, Kksnw3TO.class, Kksnw4TO.class, StKksnPersLesTO.class,
		StKksnVorVersTO.class, StKksnOrbLesTO.class, UniservPostAllTO.class, UniservPostAllResultTO.class })
public class KksnWSImpl extends KksnBasisWS {
	private static final Logger LOGGER = LoggerFactory.getLogger(KksnWSImpl.class);
	private static final Integer VIPMITARBEITER = 1;
	private final int defaultClient = 100;

	@EJB
	private SecurityTokenSevice sts;

	@Resource(name = "jca/partner/core/uniservpost")
	private UniservConnectionFactory uniservPostConnectionFactory;

	public KksnWSImpl() {

	}

	/**
	 * Person lesen mit Anschrift und Bankverbindung
	 * 
	 * @param authToken
	 *            STS-Token
	 * @param par
	 *            Aufrufparameter: Klient und PersNr
	 * @return enthaelt das Kksn-Antwortobjekt
	 * @See KksnTo
	 */
	@WebMethod
	public KksnTO kksnPersLes(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "par") final StKksnPersLesTO pPar, @WebParam(name = "db") final String db) {
		StKksnPersLesTO par = pPar == null ? new StKksnPersLesTO() : pPar;
		KksnTO kksnTo = null;
		try {
			checkKlient(par);
			kksnTo = checkParams(new Object[] { authToken, par, par.getKlient(), par.getPersNr() }, WSParams.authToken,
					WSParams.stKksnPersLes, WSParams.klient, WSParams.partnerNr);
			if (kksnTo == null) {
				// Leseberechtigung pruefen
				kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
				if (kksnTo.getPrc() == KksnFehler.ENUL) {
					kksnTo = kksnWorker.kksnPersLes(par, db);
					// VIP-Mitarbeiter ?
					kksnTo = checkVipMitarbeiterPolicy(authToken, kksnTo);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.error("Fehler in kksnPersLes. ", e);
			kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
		}
		return kksnTo;
	}

	/**
	 * Kopfdublette ermitteln zu Nebendublette
	 * 
	 * @param authToken
	 *            STS-Token
	 * @param par
	 *            Aufrufparameter: Klient,PersNr,AnschrNr und BankverbindNr
	 * @return enthaelt das Kksn-Antwortobjekt
	 * @See KksnTo
	 */
	@WebMethod
	public KksnTO kksnDubrefK(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "par") final StKksnPersLesTO pPar, @WebParam(name = "db") final String db) {
		StKksnPersLesTO par = pPar == null ? new StKksnPersLesTO() : pPar;
		KksnTO kksnTo = null;
		try {
			if (par.getAnschrNr() == null) {
				par.setAnschrNr(0);
			}
			if (par.getBankverbindNr() == null) {
				par.setBankverbindNr(0);
			}
			checkKlient(par);
			kksnTo = checkParams(
					new Object[] { authToken, par.getKlient(), par.getPersNr(), par.getAnschrNr(),
							par.getBankverbindNr() },
					WSParams.authToken, WSParams.klient, WSParams.partnerNr, WSParams.anschriftnr,
					WSParams.bankverbindnr);
			if (kksnTo == null) {
				// Berechtigung pruefen
				kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
				if (kksnTo.getPrc() == KksnFehler.ENUL) {
					// Daten lesen
					kksnTo = kksnWorker.kksnDubrefK(par, db);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.error("Fehler in kksnDubrefK. ", e);
			kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
		}
		return kksnTo;
	}

	/**
	 * kksnPersnrPruef prueft Personennummer (fuer Eingaben)
	 * 
	 * @param authToken
	 *            STS-Token
	 * @param par
	 *            Aufrufparameter: Klient und PersNr
	 * @return enthaelt das Kksn-Antwortobjekt
	 * @See KksnTo
	 */
	@WebMethod
	public KksnTO kksnPersnrPruef(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "par") final StKksnPersLesTO pPar, @WebParam(name = "db") final String db) {
		StKksnPersLesTO par = pPar == null ? new StKksnPersLesTO() : pPar;
		KksnTO kksnTo = null;
		try {
			checkKlient(par);
			kksnTo = checkParams(new Object[] { authToken, par.getKlient(), par.getMandant(), par.getPersNr() },
					WSParams.authToken, WSParams.klient, WSParams.mandant, WSParams.partnerNr);
			if (kksnTo == null) {
				// Berechtigung pruefen
				kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
				if (kksnTo.getPrc() == KksnFehler.ENUL) {
					// Daten lesen
					kksnTo = kksnWorker.kksnPersnrPruef(par, db);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.error("Fehler in kksnPersnrPruef. ", e);
			kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
		}
		return kksnTo;
	}

	/**
	 * kksnAnLes liest Anschrift
	 * 
	 * @param authToken
	 *            STS-Token
	 * @param par
	 *            Aufrufparameter: Klient, PersNr und AnschrNr
	 * @return enthaelt das Kksn-Antwortobjekt
	 * @See KksnTo
	 * 
	 */
	@WebMethod
	public KksnTO kksnAnLes(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "par") final StKksnPersLesTO pPar, @WebParam(name = "db") final String db) {
		StKksnPersLesTO par = pPar == null ? new StKksnPersLesTO() : pPar;
		KksnTO kksnTo = null;
		checkKlient(par);
		try {
			kksnTo = checkParams(new Object[] { authToken, par.getKlient(), par.getPersNr(), par.getAnschrNr() },
					WSParams.authToken, WSParams.klient, WSParams.partnerNr, WSParams.anschriftnr_0);
			if (kksnTo == null) {
				// Leseberechtigung pruefen
				kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
				if (kksnTo.getPrc() == KksnFehler.ENUL) {
					kksnTo = kksnWorker.kksnAnLes(par, db);
					// VIP-Mitarbeiter ?
					kksnTo = checkVipMitarbeiterPolicy(authToken, kksnTo);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.error("Fehler in kksnAnLes. ", e);
			kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
		}
		return kksnTo;
	}

	/**
	 * kksnBaLes liest Bankverbindung einer Person
	 * 
	 * @param authToken
	 *            STS-Token
	 * @param par
	 *            Aufrufparameter: Klient, PersNr und BankverbindNr
	 * 
	 * @return enthaelt das Kksn-Antwortobjekt
	 * @See KksnTo
	 */
	@WebMethod
	public KksnTO kksnBaLes(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "par") final StKksnPersLesTO pPar, @WebParam(name = "db") final String db) {
		StKksnPersLesTO par = pPar == null ? new StKksnPersLesTO() : pPar;
		KksnTO kksnTo = null;
		try {
			checkKlient(par);
			kksnTo = checkParams(new Object[] { authToken, par.getKlient(), par.getPersNr(), par.getBankverbindNr() },
					WSParams.authToken, WSParams.klient, WSParams.partnerNr, WSParams.bankverbindnr_0);
			if (kksnTo == null) {
				// Leseberechtigung pruefen
				kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
				if (kksnTo.getPrc() == KksnFehler.ENUL) {
					kksnTo = kksnWorker.kksnBaLes(par, db);
					// VIP-Mitarbeiter ?
					kksnTo = checkVipMitarbeiterPolicy(authToken, kksnTo);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.error("Fehler in kksnBaLes. ", e);
			kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
		}
		return kksnTo;
	}

	/**
	 * ermitteln Vorversicherungen
	 * 
	 * @param authToken
	 *            STS-Token
	 * @param par
	 *            Aufrufparameter: Klient und PersNr.
	 * 
	 * @return enthaelt das Kksn-Antwortobjekt
	 * @See KksnTo
	 */
	@WebMethod
	public KksnTO kksnVorVers(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "par") final StKksnPersLesTO pPar, @WebParam(name = "db") final String db) {
		StKksnPersLesTO par = pPar == null ? new StKksnPersLesTO() : pPar;
		KksnTO kksnTo = null;
		try {
			checkKlient(par);
			kksnTo = checkParams(new Object[] { authToken, par.getKlient(), par.getPersNr() }, WSParams.authToken,
					WSParams.klient, WSParams.partnerNr);

			if (kksnTo == null) {
				// Berechtigung pruefen
				kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
				if (kksnTo.getPrc() == KksnFehler.ENUL) {
					// Daten lesen
					kksnTo = kksnWorker.kksnVorVers(par, db);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.error("Fehler in kksnVorVers. ", e);
			kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
		}
		return kksnTo;
	}

	/**
	 * ermitteln Vorversicherungen
	 * 
	 * @param authToken
	 *            STS-Token
	 * @param par
	 *            Aufrufparameter: Klient und PersNr.
	 * 
	 * @return enthaelt das Kksn-Antwortobjekt
	 * @See KksnTo
	 */
	@WebMethod
	public KksnTO kksnVorVersVN(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "par") final StKksnPersLesTO pPar, @WebParam(name = "db") final String db) {
		StKksnPersLesTO par = pPar == null ? new StKksnPersLesTO() : pPar;
		KksnTO kksnTo = null;
		try {
			checkKlient(par);
			kksnTo = checkParams(new Object[] { authToken, par.getKlient(), par.getPersNr() }, WSParams.authToken,
					WSParams.klient, WSParams.partnerNr);

			if (kksnTo == null) {
				// Berechtigung pruefen
				kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
				if (kksnTo.getPrc() == KksnFehler.ENUL) {
					// Daten lesen
					kksnTo = kksnWorker.kksnVorVersVN(par, db);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.error("Fehler in kksnVorVersVN. ", e);
			kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
		}
		return kksnTo;
	}

	/**
	 * ermitteln alle Partnernummern zu Bilanzmonat
	 * 
	 * @param authToken
	 *            STS-Token
	 * @param klient
	 *            Klient
	 * @param bilMonat
	 *            Bilanzmonat (1-12)
	 * @return enthaelt das Kksn-Antwortobjekt
	 * @See KksnTo
	 */
	@WebMethod
	public KksnTO pdsnPidZuBilmon(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "klient") final Integer klient, @WebParam(name = "bilMonat") final Integer bilMonat,
			@WebParam(name = "db") final String db) {
		KksnTO kksnTo = null;
		try {
			kksnTo = checkParams(new Object[] { authToken, klient, bilMonat }, WSParams.authToken, WSParams.klient,
					WSParams.bilMonat);

			if (kksnTo == null) {
				// Berechtigung pruefen
				kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
				if (kksnTo.getPrc() == KksnFehler.ENUL) {
					// Daten lesen
					kksnTo = kksnWorker.pdsnPidZuBilmon(klient, bilMonat, db);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.error("Fehler in pdsnPidZuBilmon. ", e);
			kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
		}
		return kksnTo;
	}

	/**
	 * 
	 * @param authToken
	 *            STS-Token
	 * @param userId
	 *            BLXXXXX-Benutzername des Aufrufes
	 * @param pfc
	 *            PFC-Aufrufcode zur Funktion
	 * @param pAlt
	 *            Alte Zustand der Funktion
	 * @param pNeu
	 *            Neue Zustand der Funktion
	 * @return
	 */
	@WebMethod
	public KksnTO kksnFunk2akt(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "userId") final String userId, @WebParam(name = "pfc") final Integer pfc,
			@WebParam(name = "funkAlt") final StKksnFunk2AktTO pAlt,
			@WebParam(name = "funkNeu") final StKksnFunk2AktTO pNeu, @WebParam(name = "db") final String db) {
		KksnTO kksnTo = null;
		try {
			// Schreibrechte pruefen
			kksnTo = checkPolicy(authToken, PdbBerechtigungen.DLD_PDB_SCHREIBEN);
			if (kksnTo.getPrc() == KksnFehler.ENUL) {
				kksnTo = orbWorker.kksnFunk2akt(userId, pfc, pAlt, pNeu, db);
			}
		} catch (RuntimeException e) {
			LOGGER.error("Fehler in kksnFunk2akt. ", e);
			kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
		}
		return kksnTo;
	}

	/**
	 * Ordnungsbegriff aktualisieren
	 * 
	 * @param authToken
	 *            STS-Token
	 * @param par
	 *            PFC-Aufrufcode und Informationen zum ORB
	 * @return enthaelt das Kksn-Antwortobjekt
	 * @See KksnTo
	 */
	@WebMethod
	public KksnTO kksnOrbAkt(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "par") final StKksnOrbAktTO pPar, @WebParam(name = "db") final String db) {
		StKksnOrbAktTO par = pPar == null ? new StKksnOrbAktTO() : pPar;
		KksnTO kksnTo = null;
		try {
			// Schreibrechte pruefen
			kksnTo = checkPolicy(authToken, PdbBerechtigungen.DLD_PDB_SCHREIBEN);
			if (kksnTo.getPrc() == KksnFehler.ENUL) {
				kksnTo = orbWorker.kksnOrbAkt(par, db);
			}
		} catch (RuntimeException e) {
			LOGGER.error("Fehler in kksnOrbAkt. ", e);
			kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
		}
		return kksnTo;
	}

	/**
	 * ermitteln alle Alias-Parnernummern zu einer Partnernummer - mit Hilfe der
	 * Dublettenreferenz
	 * 
	 * @param authToken
	 *            STS-Token
	 * @param par
	 *            Aufrufparameter: Klient und PersNr.
	 * 
	 * @return enthaelt das Kksn-Antwortobjekt
	 * @See KksnTo
	 */
	@WebMethod
	public KksnTO kksnDubletten(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "par") final StKksnPersLesTO pPar, @WebParam(name = "db") final String db) {

		StKksnPersLesTO par = pPar == null ? new StKksnPersLesTO() : pPar;
		KksnTO kksnTo = null;
		try {
			checkKlient(par);
			kksnTo = checkParams(new Object[] { authToken, par.getKlient(), par.getPersNr() }, WSParams.authToken,
					WSParams.klient, WSParams.partnerNr);

			if (kksnTo == null) {
				// Berechtigung pruefen
				kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
				if (kksnTo.getPrc() == KksnFehler.ENUL) {
					// Daten lesen
					kksnTo = kksnWorker.kksnDubletten(par, db);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.error("Fehler in kksnDubletten. ", e);
			kksnTo = new KksnTO(KksnFehler.EERR, getExceptionAsString(e));
		}
		return kksnTo;
	}

	/**
	 * Aufruf Uniserv-Unipost-Schnittstelle mit allen Parametern - fuer Test und
	 * Fehlersuche
	 * 
	 * @param authToken
	 *            STS-Token
	 * @param par
	 *            Aufrufparameter: UniservPostAllTO mit allen Parameter fuer
	 *            UniPost-DE
	 * 
	 * @return UniservPostAllResultTO
	 * @See KksnTo
	 */
	@WebMethod
	public UniservPostAllResultTO uniPostAll(@WebParam(name = "authToken") final String authToken,
			@WebParam(name = "par") final UniservPostAllTO pPar, @WebParam(name = "db") final String db) {

		UniservPostAllResultTO uniPostResult = new UniservPostAllResultTO();
		UniservConnection uniConn = null;
		try {
			uniConn = (UniservConnection) uniservPostConnectionFactory.getConnection();
		} catch (Exception e) {
			LOGGER.error("Exception in uniPostAll - getConnection(): ", e);
			uniPostResult.setMessage(e.getMessage());
		}
		if (uniConn == null) {
			LOGGER.error(" uniConn=Null in uniPostAll - nach getConnection(): ");
		} else {
			try {
				// ggf. check pPar
				// Berechtigung pruefen
				KksnTO kksnTo = checkPolicy(authToken, PdbBerechtigungen.PDB_LESEN);
				if (kksnTo.getPrc() == KksnFehler.ENUL) {
					uniPostResult = uniConn.pruefePostAdresse(pPar);
				}
			} catch (Exception e) {
				if (uniPostResult != null)
					uniPostResult.setMessage(e.getMessage());
				LOGGER.error("Exception in uniPostAll - checkPolicy / pruefePostAdresse: ", e);
				LOGGER.error("uniConn      : " + uniConn);
				LOGGER.error("pPar         : " + pPar);
				LOGGER.error("uniPostResult: " + uniPostResult);
			}
			try {
				uniConn.close();
			} catch (ResourceException e) {
				LOGGER.info("Exception in uniPostAll - close(): ", e);
				uniPostResult.setMessage(e.getMessage());
			}
		}
		return uniPostResult;
	}

	/**
	 * Hilfsfunktion fuer die Pruefung der Berechtigung des Aufrufers
	 * 
	 * @param authToken
	 *            STS-Token
	 * @param policyName
	 *            zur pruefende Policy
	 * @return
	 */
	private KksnTO checkPolicy(String authToken, PdbBerechtigungen securityProfile) throws CoreSystemException {
		KksnTO result = new KksnTO();
		try {

			if (!sts.isAllowed(authToken, securityProfile)) {
				result.setPrc(KksnFehler.EBRT, "Der Aufrufer darf die Daten nicht lesen");
			}
		} catch (PolicyCheckException e) {
			LOGGER.error("checkPolicy error", e);
			result.setPrc(KksnFehler.EBRT,
					"PolicyName '" + securityProfile.toString() + "' existiert nicht. " + e.getMessage());
		} catch (AbstractCheckedSystemException e) {
			LOGGER.error("checkPolicy error", e);
			result.setPrc(KksnFehler.ETOK, "Token ist ungueltig. " + e.getMessage());
		}
		return result;
	}

	/**
	 * Prueft das KksnTo bzgl. der VIP-Mitarbeiterberechetigung
	 * 
	 * @param authToken
	 *            STS-Token
	 * @param kksnTo
	 *            zu pruefende Daten
	 * @return Antwortobjekt in Abhaengigkeit der Berechtigung
	 */
	private KksnTO checkVipMitarbeiterPolicy(String authToken, KksnTO kksnTo) {
		KksnTO result = kksnTo;
		if (kksnTo == null) {
			return result;
		}
		// VIP-Mitarbeiter-Kennzeichen ?
		if ((kksnTo.getVip() != null) && (kksnTo.getVip() == VIPMITARBEITER)) {
			KksnTO check = checkPolicy(authToken, PdbBerechtigungen.DLD_PDB_MITARB);
			// Test
			// check.setPrc(KksnFehler.EBRT);
			// keine Daten ausgeben
			if (check.getPrc() != KksnFehler.ENUL) {
				if (check.getPrc() == KksnFehler.EBRT) {
					check.setPrc(KksnFehler.EVIP, "VIP-Berechtigung fehlt.");
				}
				result = check;
			}
		}

		return result;
	}

	/**
	 * Dynamische Methode fuer die Pruefung der Aufrufparameter
	 * 
	 * @param par
	 * @param params
	 * @return
	 */
	private KksnTO checkParams(Object[] par, WSParams... params) {
		KksnTO result = null;

		for (int i = 0; i < params.length; i++) {
			Object obj = par[i];
			if (WSParams.authToken == params[i]) {
				if ((obj == null) || (((String) obj).length() == 0)) {
					result = new KksnTO(KksnFehler.EPAR, WSParams.authToken, obj);
					break;
				}
			} else if (WSParams.klient == params[i]) {
				if ((obj == null) || (((Integer) obj) <= 0)) {
					result = new KksnTO(KksnFehler.EPAR, WSParams.klient, obj);
					break;
				}
			} else if (WSParams.partnerNr == params[i]) {
				if ((obj == null) || (((Long) obj) <= 0)) {
					result = new KksnTO(KksnFehler.EPAR, WSParams.partnerNr, obj);
					break;
				}
			} else if (WSParams.anschriftnr_0 == params[i]) {
				if ((obj == null) || (((Integer) obj) == 0)) {
					result = new KksnTO(KksnFehler.EPAR, WSParams.anschriftnr, obj);
					break;
				}
			} else if (WSParams.anschriftnr == params[i]) {
				if ((obj == null) || (((Integer) obj) < -1)) {
					result = new KksnTO(KksnFehler.EPAR, WSParams.anschriftnr, obj);
					break;
				}
			} else if (WSParams.stKksnPersLes == params[i]) {
				if (obj == null) {
					result = new KksnTO(KksnFehler.EPAR, WSParams.stKksnPersLes, obj);
				}
			} else if (WSParams.bankverbindnr_0 == params[i]) {
				if ((obj == null) || (((Integer) obj) == 0)) {
					result = new KksnTO(KksnFehler.EPAR, WSParams.bankverbindnr, obj);
					break;
				}
			} else if (WSParams.bankverbindnr == params[i]) {
				if ((obj == null) || (((Integer) obj) < 0)) {
					result = new KksnTO(KksnFehler.EPAR, WSParams.bankverbindnr, obj);
					break;
				}
			} else if (WSParams.mandant == params[i]) {
				if (obj == null) {
					result = new KksnTO(KksnFehler.EPAR, WSParams.mandant, obj);
					break;
				}
			} else if (WSParams.orbArt == params[i]) {
				if (obj == null) {
					result = new KksnTO(KksnFehler.EPAR, WSParams.orbArt, obj);
					break;
				}
			} else if (WSParams.schluessel == params[i]) {
				if (obj == null) {
					result = new KksnTO(KksnFehler.EPAR, WSParams.schluessel, obj);
					break;
				}
			} else if (WSParams.bilMonat == params[i]) {
				if (obj == null) {
					result = new KksnTO(KksnFehler.EPAR, WSParams.bilMonat, obj);
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Hilfsmethode fuer das Setzen des Default-Klients
	 * 
	 * @param par
	 *            zu pruefendes Objekt
	 */
	private void checkKlient(StKksnPersLesTO par) {
		if ((par != null) && ((par.getKlient() == null) || (par.getKlient().intValue() == 0))) {
			par.setKlient(defaultClient);
		}
	}
}
