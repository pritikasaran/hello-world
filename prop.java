/**
 * 
 */
package de.deltalloyd.partnerdb.webservices.impl;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.bind.annotation.XmlSeeAlso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.deltalloyd.foundation.auth.exception.AuthorisationException;
import de.deltalloyd.foundation.exception.AbstractCheckedSystemException;
import de.deltalloyd.partnerdb.core.monitor.appreg.MyAppPerformanceSensor;
import de.deltalloyd.partnerdb.core.type.NatuerlichePersonGrunddatenTO;
import de.deltalloyd.partnerdb.core.type.NatuerlichePersonTO;
import de.deltalloyd.partnerdb.core.type.NichtNatuerlichePersonGrunddatenTO;
import de.deltalloyd.partnerdb.core.type.NichtNatuerlichePersonTO;
import de.deltalloyd.partnerdb.core.type.PartnerGemeinschaftGrunddatenTO;
import de.deltalloyd.partnerdb.core.type.PartnerGemeinschaftTO;
import de.deltalloyd.partnerdb.orb.impl.OrbServiceEJB;
import de.deltalloyd.partnerdb.orb.service.type.FunktionServiceTO;
import de.deltalloyd.partnerdb.orb.service.type.OrdnungsbegriffServiceTO;
import de.deltalloyd.partnerdb.orb.type.FunktionTO;
import de.deltalloyd.partnerdb.orb.type.OrdnungsbegriffSuchTO;
import de.deltalloyd.partnerdb.orb.type.OrdnungsbegriffTO;
import de.deltalloyd.partnerdb.security.profiles.PdbBerechtigungen;
import de.deltalloyd.partnerdb.webservices.exception.PDBException;
import de.deltalloyd.partnerdb.webservices.exception.PartnerWebServiceException;
import de.deltalloyd.partnerdb.webservices.interfaces.IOrbWebService;
import de.deltalloyd.partnerdb.webservices.security.exception.STSException;
import de.deltalloyd.partnerdb.webservices.utils.PDBReleaseInfo;

/**
 * OrbWebServiceImpl stellt Webservices fuer den
 * Ordnungsbegriff-Geschaeftsvorfall zur Verfuegung.
 * 
 */
@Interceptors(MyAppPerformanceSensor.class)
@Stateless
@WebService(name = "OrbWebService", targetNamespace = "http://www.deltalloyd.de/pdb/partnerwebservice", serviceName = "OrbWebService")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
@XmlSeeAlso({ NatuerlichePersonTO.class, NichtNatuerlichePersonTO.class, PartnerGemeinschaftTO.class,
		PartnerGemeinschaftGrunddatenTO.class, NatuerlichePersonGrunddatenTO.class,
		NichtNatuerlichePersonGrunddatenTO.class })
public class OrbWebServiceImpl extends AbstractWebService implements IOrbWebService {
	/**
	 * Das Feld <tt>logger</tt> enthaelt ...
	 */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(de.deltalloyd.partnerdb.webservices.impl.OrbWebServiceImpl.class);

	@EJB
	private OrbServiceEJB orbServiceLocal;

	/*
	 * (non-Javadoc)
	 * 
	 * @seede.deltalloyd.partnerdb.webservices.orb.interfaces.IOrbWebService#
	 * sucheOrdnungsbegriffService(java.lang.String, java.lang.String,
	 * java.lang.String, java.lang.Integer)
	 */
	@WebMethod
	public OrdnungsbegriffTO sucheOrdnungsbegriffService(String authToken, String orbArtSchluessel,
			String funkArtSchluessel, Integer aMandant, String aParameter) throws PartnerWebServiceException {
		try {
			LOGGER.debug("sucheOrdnungsbegriffService:Aufruf> orbArtSchluessel: " + orbArtSchluessel
					+ ", funkArtSchluessel: " + funkArtSchluessel + ", aMandant:" + aMandant);
			// Pruefung, ob allgemeine Leserechte vorliegen.
			getSSTSecurityToken().isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);
			// EJB-Aufruf
			OrdnungsbegriffSuchTO ordnungsbegriffSuchTO = new OrdnungsbegriffSuchTO();
			ordnungsbegriffSuchTO.setOrbartSchluessel(orbArtSchluessel);
			ordnungsbegriffSuchTO.setOrbMandant(aMandant);
			ordnungsbegriffSuchTO.setOrbSchluessel(funkArtSchluessel);

			OrdnungsbegriffTO ordnungsbegriffTO = orbServiceLocal.sucheOrdnungsbegriff(ordnungsbegriffSuchTO);

			if (ordnungsbegriffTO == null) {
				LOGGER.error("sucheOrdnungsbegriffService:" + orbArtSchluessel + "," + funkArtSchluessel + ","
						+ aMandant + ":ORB nicht gefunden");
				throw new PartnerWebServiceException(
						new PDBException(PDBException.DATENFEHLER, "ORB wurde nicht gefunden.", null));
			}

			boolean bVIPBerechtigung = false;
			try {
				bVIPBerechtigung = getSSTSecurityToken().isAllowed(authToken, PdbBerechtigungen.DLD_PDB_MITARB);
			} catch (Exception e) {

			}

			// bei VIP-Vertraegen keine Partnerdaten ausgeben
			if (bVIPBerechtigung == false) {
				final String VIPMITARBEITER = "1";
				for (FunktionTO funktionTO : ordnungsbegriffTO.getFunktionen()) {
					String vipKZ = funktionTO.getPartnerTO().getGrunddaten().getVipKennzeichen();
					if ((vipKZ != null) && (vipKZ.trim().equals(VIPMITARBEITER))) {
						long partnerNr = funktionTO.getPartnerTO().getGrunddaten().getPartnerNr();
						int klient = funktionTO.getPartnerTO().getGrunddaten().getKlient();
						if (funktionTO.getPartnerTO() instanceof NatuerlichePersonTO) {
							funktionTO.setPartnerTO(new NatuerlichePersonTO());
							funktionTO.getPartnerTO().setPartnerTyp("1");
							funktionTO.getPartnerTO().setGrunddaten(new NatuerlichePersonGrunddatenTO());
							funktionTO.getPartnerTO().getGrunddaten().setPartnerNr(partnerNr);
							funktionTO.getPartnerTO().getGrunddaten().setKlient(klient);
							funktionTO.getPartnerTO().getGrunddaten().setVipKennzeichen(VIPMITARBEITER);
						} else if (funktionTO.getPartnerTO() instanceof NichtNatuerlichePersonTO) {
							funktionTO.setPartnerTO(new NichtNatuerlichePersonTO());
							funktionTO.getPartnerTO().setPartnerTyp("2");
							funktionTO.getPartnerTO().setGrunddaten(new NichtNatuerlichePersonGrunddatenTO());
							funktionTO.getPartnerTO().getGrunddaten().setPartnerNr(partnerNr);
							funktionTO.getPartnerTO().getGrunddaten().setKlient(klient);
							funktionTO.getPartnerTO().getGrunddaten().setVipKennzeichen(VIPMITARBEITER);
						} else if (funktionTO.getPartnerTO() instanceof PartnerGemeinschaftTO) {
							funktionTO.setPartnerTO(new PartnerGemeinschaftTO());
							funktionTO.getPartnerTO().setPartnerTyp("3");
							funktionTO.getPartnerTO().setGrunddaten(new PartnerGemeinschaftGrunddatenTO());
							funktionTO.getPartnerTO().getGrunddaten().setPartnerNr(partnerNr);
							funktionTO.getPartnerTO().getGrunddaten().setKlient(klient);
							funktionTO.getPartnerTO().getGrunddaten().setVipKennzeichen(VIPMITARBEITER);
						}
					}
				}
			}

			return ordnungsbegriffTO;
		} catch (STSException e) {
			LOGGER.error(e.getMessage(), e);
			throw new PartnerWebServiceException(new PDBException(PDBException.KEINELESERECHTE, e.getUuid()));
		} catch (AuthorisationException e) {
			LOGGER.error(e.getMessage());
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AuthorisationException, e.getMessage(), e.getUuid()));
		} catch (AbstractCheckedSystemException e) {
			LOGGER.error(e.getMessage());
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new PartnerWebServiceException(new PDBException(PDBException.SONSTIGEFEHLER, e.getMessage(), null));
		}
	}

	/**
	 * 2016.06.14 im Release 13.0 kann sucheOrdnungsbegriffServiceFull-Methode
	 * geloescht werden und OrdnungsbegriffServiceTO ebenso!!!!.
	 * 
	 * 
	 */
	@WebMethod
	public OrdnungsbegriffServiceTO sucheOrdnungsbegriffServiceFull(String authToken, String orbArtSchluessel,
			String funkArtSchluessel, Integer aMandant, String aParameter) throws PartnerWebServiceException {
		try {
			LOGGER.debug("sucheOrdnungsbegriffService:Aufruf> orbArtSchluessel: " + orbArtSchluessel
					+ ", funkArtSchluessel: " + funkArtSchluessel + ", aMandant:" + aMandant);
			// Pruefung, ob allgemeine Leserechte vorliegen.
			getSSTSecurityToken().isAllowed(authToken, PdbBerechtigungen.PDB_LESEN);
			// EJB-Aufruf
			OrdnungsbegriffSuchTO ordnungsbegriffSuchTO = new OrdnungsbegriffSuchTO();
			ordnungsbegriffSuchTO.setOrbartSchluessel(orbArtSchluessel);
			ordnungsbegriffSuchTO.setOrbMandant(aMandant);
			ordnungsbegriffSuchTO.setOrbSchluessel(funkArtSchluessel);

			OrdnungsbegriffServiceTO ordnungsbegriffTO = orbServiceLocal
					.sucheOrdnungsbegriffFull(ordnungsbegriffSuchTO);

			if (ordnungsbegriffTO == null) {
				LOGGER.error("sucheOrdnungsbegriffService:" + orbArtSchluessel + "," + funkArtSchluessel + ","
						+ aMandant + ":ORB nicht gefunden");
				throw new PartnerWebServiceException(
						new PDBException(PDBException.DATENFEHLER, "ORB wurde nicht gefunden.", null));
			}

			boolean bVIPBerechtigung = false;
			try {
				bVIPBerechtigung = getSSTSecurityToken().isAllowed(authToken, PdbBerechtigungen.DLD_PDB_MITARB);
			} catch (Exception e) {

			}

			// bei VIP-Vertraegen keine Partnerdaten ausgeben
			if (bVIPBerechtigung == false) {
				final String VIPMITARBEITER = "1";
				for (FunktionServiceTO funktionTO : ordnungsbegriffTO.getFunktionen()) {
					String vipKZ = funktionTO.getPartnerTO().getGrunddaten().getVipKennzeichen();
					if ((vipKZ != null) && (vipKZ.trim().equals(VIPMITARBEITER))) {
						long partnerNr = funktionTO.getPartnerTO().getGrunddaten().getPartnerNr();
						int klient = funktionTO.getPartnerTO().getGrunddaten().getKlient();
						if (funktionTO.getPartnerTO() instanceof NatuerlichePersonTO) {
							funktionTO.setPartnerTO(new NatuerlichePersonTO());
							funktionTO.getPartnerTO().setPartnerTyp("1");
							funktionTO.getPartnerTO().setGrunddaten(new NatuerlichePersonGrunddatenTO());
							funktionTO.getPartnerTO().getGrunddaten().setPartnerNr(partnerNr);
							funktionTO.getPartnerTO().getGrunddaten().setKlient(klient);
							funktionTO.getPartnerTO().getGrunddaten().setVipKennzeichen(VIPMITARBEITER);
						} else if (funktionTO.getPartnerTO() instanceof NichtNatuerlichePersonTO) {
							funktionTO.setPartnerTO(new NichtNatuerlichePersonTO());
							funktionTO.getPartnerTO().setPartnerTyp("2");
							funktionTO.getPartnerTO().setGrunddaten(new NichtNatuerlichePersonGrunddatenTO());
							funktionTO.getPartnerTO().getGrunddaten().setPartnerNr(partnerNr);
							funktionTO.getPartnerTO().getGrunddaten().setKlient(klient);
							funktionTO.getPartnerTO().getGrunddaten().setVipKennzeichen(VIPMITARBEITER);
						} else if (funktionTO.getPartnerTO() instanceof PartnerGemeinschaftTO) {
							funktionTO.setPartnerTO(new PartnerGemeinschaftTO());
							funktionTO.getPartnerTO().setPartnerTyp("3");
							funktionTO.getPartnerTO().setGrunddaten(new PartnerGemeinschaftGrunddatenTO());
							funktionTO.getPartnerTO().getGrunddaten().setPartnerNr(partnerNr);
							funktionTO.getPartnerTO().getGrunddaten().setKlient(klient);
							funktionTO.getPartnerTO().getGrunddaten().setVipKennzeichen(VIPMITARBEITER);
						}
					}
				}
			}

			return ordnungsbegriffTO;
		} catch (STSException e) {
			LOGGER.error(e.getMessage(), e);
			throw new PartnerWebServiceException(new PDBException(PDBException.KEINELESERECHTE, e.getUuid()));
		} catch (AuthorisationException e) {
			LOGGER.error(e.getMessage());
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AuthorisationException, e.getMessage(), e.getUuid()));
		} catch (AbstractCheckedSystemException e) {
			LOGGER.error(e.getMessage());
			throw new PartnerWebServiceException(
					new PDBException(PDBException.AbstractCheckedSystemException, e.getMessage(), e.getUuid()));
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new PartnerWebServiceException(new PDBException(PDBException.SONSTIGEFEHLER, e.getMessage(), null));
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

}

